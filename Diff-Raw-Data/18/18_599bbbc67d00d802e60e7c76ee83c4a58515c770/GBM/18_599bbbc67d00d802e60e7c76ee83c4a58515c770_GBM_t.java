 package hex.gbm;
 
 import static water.util.Utils.div;
 import hex.gbm.DTree.DecidedNode;
 import hex.gbm.DTree.LeafNode;
 import hex.gbm.DTree.TreeModel.TreeStats;
 import hex.gbm.DTree.UndecidedNode;
 import water.*;
 import water.api.DocGen;
 import water.api.GBMProgressPage;
 import water.fvec.Chunk;
 import water.fvec.Frame;
 import water.util.*;
 import water.util.Log.Tag.Sys;
 
 // Gradient Boosted Trees
 //
 // Based on "Elements of Statistical Learning, Second Edition, page 387"
 public class GBM extends SharedTreeModelBuilder {
   static final int API_WEAVER = 1; // This file has auto-gen'd doc & json fields
   static public DocGen.FieldDoc[] DOC_FIELDS; // Initialized from Auto-Gen code.
 
   @API(help = "Learning rate, from 0. to 1.0", filter = Default.class, dmin=0, dmax=1)
   public double learn_rate = 0.1;
 
   @API(help = "Grid search parallelism", filter = Default.class, lmax = 4, gridable=false)
   public int grid_parallelism = 1;
 
   public static class GBMModel extends DTree.TreeModel {
     static final int API_WEAVER = 1; // This file has auto-gen'd doc & json fields
     static public DocGen.FieldDoc[] DOC_FIELDS; // Initialized from Auto-Gen code.
     final double _learn_rate;
     public GBMModel(Key key, Key dataKey, Key testKey, String names[], String domains[][], int ntrees, int max_depth, int min_rows, int nbins, double learn_rate) {
       super(key,dataKey,testKey,names,domains,ntrees,max_depth,min_rows,nbins);
       _learn_rate = learn_rate;
     }
     public GBMModel(GBMModel prior, DTree[] trees, double err, long [][] cm, TreeStats tstats) {
       super(prior, trees, err, cm, tstats);
       _learn_rate = prior._learn_rate;
     }
 
     @Override protected float[] score0(double[] data, float[] preds) {
       float sum = 0;
       float[] p = super.score0(data, preds);
       if (nclasses()>1) { // classification
         for(int k=0; k<p.length;k++)
           sum+=(p[k]=(float)Math.exp(p[k]));
         div(p,sum);
       } else { // regression
         // do nothing for regression
       }
       return p;
     }
 
     @Override protected void generateModelDescription(StringBuilder sb) {
       DocGen.HTML.paragraph(sb,"Learn rate: "+_learn_rate);
     }
 
     @Override protected void toJavaUnifyPreds(SB bodyCtxSB) {
       if (isClassifier()) {
         bodyCtxSB.i().p("// Compute Probabilities").nl();
         bodyCtxSB.i().p("float sum = 0;").nl();
         bodyCtxSB.i().p("for(int i=1;i<preds.length; i++) sum += (preds[i]=(float) Math.exp(preds[i]));").nl();
         bodyCtxSB.i().p("for(int i=1; i<preds.length; i++) preds[i] = (float) preds[i] / sum;").nl();
       }
     }
   }
   public Frame score( Frame fr ) { return ((GBMModel)UKV.get(dest())).score(fr);  }
 
   @Override protected Log.Tag.Sys logTag() { return Sys.GBM__; }
   public GBM() { description = "Distributed GBM"; }
 
   /** Return the query link to this page */
   public static String link(Key k, String content) {
     RString rs = new RString("<a href='GBM.query?source=%$key'>%content</a>");
     rs.replace("key", k.toString());
     rs.replace("content", content);
     return rs.toString();
   }
 
   @Override protected void logStart() {
     Log.info("Starting GBM model build...");
     super.logStart();
     Log.info("    learn_rate: " + learn_rate);
   }
 
   @Override protected Status exec() {
     logStart();
     buildModel();
     return Status.Done;
   }
 
   @Override public int gridParallelism() {
     return grid_parallelism;
   }
 
   @Override protected Response redirect() {
     return GBMProgressPage.redirect(this, self(), dest());
   }
 
   // ==========================================================================
   // Compute a GBM tree.
 
   // Start by splitting all the data according to some criteria (minimize
   // variance at the leaves).  Record on each row which split it goes to, and
   // assign a split number to it (for next pass).  On *this* pass, use the
   // split-number to build a per-split histogram, with a per-histogram-bucket
   // variance.
   @Override protected void buildModel( final Frame fr, String names[], String domains[][], final Key outputKey, final Key dataKey, final Key testKey, final Timer t_build ) {
     GBMModel model = new GBMModel(outputKey, dataKey, testKey, names, domains, ntrees, max_depth, min_rows, nbins, learn_rate);
     DKV.put(outputKey, model);
     // Build trees until we hit the limit
     int tid;
     DTree[] ktrees = null;              // Trees
     TreeStats tstats = new TreeStats(); // Tree stats
     for( tid=0; tid<ntrees; tid++) {
       // ESL2, page 387
       // Step 2a: Compute prediction (prob distribution) from prior tree results:
       //   Work <== f(Tree)
       new ComputeProb().doAll(fr);
 
       // ESL2, page 387
       // Step 2b i: Compute residuals from the prediction (probability distribution)
       //   Work <== f(Work)
       new ComputeRes().doAll(fr);
 
       // ESL2, page 387, Step 2b ii, iii, iv
       ktrees = buildNextKTrees(fr);
       if( cancelled() ) break; // If canceled during building, do not bulkscore
 
       // Check latest predictions
       tstats.updateBy(ktrees);
       model = doScoring(model, outputKey, fr, ktrees, tid, tstats, false);
     }
     // Final scoring
     model = doScoring(model, outputKey, fr, ktrees, tid, tstats, true);
     cleanUp(fr,t_build); // Shared cleanup
   }
 
   private GBMModel doScoring(GBMModel model, Key outputKey, Frame fr, DTree[] ktrees, int tid, TreeStats tstats, boolean finalScoring ) {
     Score sc = new Score().doIt(model,fr,validation).report(Sys.GBM__,tid,ktrees);
     model = new GBMModel(model, finalScoring?null:ktrees, (float)sc._sum/_nrows, sc._cm, tstats);
     DKV.put(outputKey, model);
     return model;
   }
 
   // --------------------------------------------------------------------------
   // Compute Prediction from prior tree results.
   // Classification: Probability Distribution of loglikelyhoods
   //   Prob_k = exp(Work_k)/sum_all_K exp(Work_k)
   // Regression: Just prior tree results
   // Work <== f(Tree)
   class ComputeProb extends MRTask2<ComputeProb> {
     @Override public void map( Chunk chks[] ) {
       Chunk ys = chk_resp(chks);
       if( _nclass > 1 ) {       // Classification
         double ds[] = new double[_nclass];
         for( int row=0; row<ys._len; row++ ) {
           double sum = score0(chks,ds,row);
           if( Double.isInfinite(sum) ) // Overflow (happens for constant responses)
             for( int k=0; k<_nclass; k++ )
               chk_work(chks,k).set0(row,Double.isInfinite(ds[k])?1.0f:0.0f);
           else
             for( int k=0; k<_nclass; k++ ) // Save as a probability distribution
               chk_work(chks,k).set0(row,(float)(ds[k]/sum));
         }
       } else {                  // Regression
         Chunk tr = chk_tree(chks,0); // Prior tree sums
         Chunk wk = chk_work(chks,0); // Predictions
         for( int row=0; row<ys._len; row++ )
           wk.set0(row,(float)tr.at0(row));
       }
     }
   }
 
   // Read the 'tree' columns, do model-specific math and put the results in the
   // ds[] array, and return the sum.  Dividing any ds[] element by the sum
   // turns the results into a probability distribution.
   @Override protected double score0( Chunk chks[], double ds[/*nclass*/], int row ) {
     if( _nclass == 1 )                                       // Classification?
       return chk_tree(chks,0).at0(row);
     double sum=0;
     for( int k=0; k<_nclass; k++ ) // Sum across of likelyhoods
       sum+=(ds[k]=Math.exp(chk_tree(chks,k).at0(row)));
     return sum;
   }
 
   // --------------------------------------------------------------------------
   // Compute Residuals from Actuals
   // Work <== f(Work)
   class ComputeRes extends MRTask2<ComputeRes> {
     @Override public void map( Chunk chks[] ) {
       Chunk ys = chk_resp(chks);
       if( _nclass > 1 ) {       // Classification
 
         for( int row=0; row<ys._len; row++ ) {
           if( ys.isNA0(row) ) throw H2O.unimpl(); // Set NANs in all works
           int y = (int)ys.at80(row); // zero-based response variable
           // Actual is '1' for class 'y' and '0' for all other classes
           for( int k=0; k<_nclass; k++ ) {
             if( _distribution[k] != 0 ) {
               Chunk wk = chk_work(chks,k);
               wk.set0(row, (y==k?1f:0f)-(float)wk.at0(row) );
             }
           }
         }
 
       } else {                  // Regression
         Chunk wk = chk_work(chks,0); // Prediction==>Residuals
         for( int row=0; row<ys._len; row++ )
           wk.set0(row, (float)(ys.at0(row)-wk.at0(row)) );
       }
     }
   }
 
   // --------------------------------------------------------------------------
   // Build the next k-trees, which is trying to correct the residual error from
   // the prior trees.  From LSE2, page 387.  Step 2b ii, iii.
   private DTree[] buildNextKTrees(Frame fr) {
     // We're going to build K (nclass) trees - each focused on correcting
     // errors for a single class.
     final DTree[] ktrees = new DTree[_nclass];
     for( int k=0; k<_nclass; k++ ) {
       // Initially setup as-if an empty-split had just happened
       if( _distribution == null || _distribution[k] != 0 ) {
         ktrees[k] = new DTree(fr._names,_ncols,(char)nbins,(char)_nclass,min_rows);
         new GBMUndecidedNode(ktrees[k],-1,DBinHistogram.initialHist(fr,_ncols,(char)nbins)); // The "root" node
       }
     }
     int[] leafs = new int[_nclass]; // Define a "working set" of leaf splits, from here to tree._len
 
     // ----
     // ESL2, page 387.  Step 2b ii.
     // One Big Loop till the ktrees are of proper depth.
     // Adds a layer to the trees each pass.
     int depth=0;
     for( ; depth<max_depth; depth++ ) {
       if( cancelled() ) return null;
 
       // Build K trees, one per class.
       // Fuse 2 conceptual passes into one:
       // Pass 1: Score a prior DHistogram, and make new DTree.Node assignments
       // to every row.  This involves pulling out the current assigned Node,
       // "scoring" the row against that Node's decision criteria, and assigning
       // the row to a new child Node (and giving it an improved prediction).
       // Pass 2: Build new summary DHistograms on the new child Nodes every row
       // got assigned into.  Collect counts, mean, variance, min, max per bin,
       // per column.
       ScoreBuildHistogram sbh = new ScoreBuildHistogram(ktrees,leafs).doAll(fr);
       //System.out.println(sbh.profString());
 
       // Build up the next-generation tree splits from the current histograms.
       // Nearly all leaves will split one more level.  This loop nest is
       //           O( #active_splits * #bins * #ncols )
       // but is NOT over all the data.
       boolean did_split=false;
       for( int k=0; k<_nclass; k++ ) {
         DTree tree = ktrees[k]; // Tree for class K
         if( tree == null ) continue;
         int tmax = tree._len;   // Number of total splits in tree K
         for( int leaf=leafs[k]; leaf<tmax; leaf++ ) { // Visit all the new splits (leaves)
           UndecidedNode udn = tree.undecided(leaf);
           udn._hs = sbh.getFinalHisto(k,leaf);
           //System.out.println(udn);
           // Replace the Undecided with the Split decision
           GBMDecidedNode dn = new GBMDecidedNode((GBMUndecidedNode)udn);
           if( dn._split._col == -1 ) udn.do_not_split();
           else did_split = true;
         }
         tree.depth++;
         leafs[k]=tmax;          // Setup leafs for next tree level
       }
 
       // If we did not make any new splits, then the tree is split-to-death
       if( !did_split ) break;
     }
 
     // Each tree bottomed-out in a DecidedNode; go 1 more level and insert
     // LeafNodes to hold predictions.
     for( int k=0; k<_nclass; k++ ) {
       DTree tree = ktrees[k];
       if( tree == null ) continue;
       int leaf = leafs[k] = tree._len;
       for( int nid=0; nid<leaf; nid++ ) {
         if( tree.node(nid) instanceof DecidedNode ) {
           DecidedNode dn = tree.decided(nid);
           for( int i=0; i<dn._nids.length; i++ ) {
             int cnid = dn._nids[i];
             if( cnid == -1 || // Bottomed out (predictors or responses known constant)
                 tree.node(cnid) instanceof UndecidedNode || // Or chopped off for depth
                 (tree.node(cnid) instanceof DecidedNode &&  // Or not possible to split
                  ((DecidedNode)tree.node(cnid))._split._col==-1) )
               dn._nids[i] = new GBMLeafNode(tree,nid)._nid; // Mark a leaf here
           }
           // Handle the trivial non-splitting tree
           if( nid==0 && dn._split._col == -1 )
             new GBMLeafNode(tree,-1,0);
         }
       }
     }
 
     // ----
     // ESL2, page 387.  Step 2b iii.  Compute the gammas, and store them back
     // into the tree leaves.  Includes learn_rate.
     //    gamma_i_k = (nclass-1)/nclass * (sum res_i / sum (|res_i|*(1-|res_i|)))
     // For regression:
     //    gamma_i_k = sum res_i / count(res_i)
     GammaPass gp = new GammaPass(ktrees,leafs).doAll(fr);
     double m1class = _nclass > 1 ? (double)(_nclass-1)/_nclass : 1.0; // K-1/K
     for( int k=0; k<_nclass; k++ ) {
       final DTree tree = ktrees[k];
       if( tree == null ) continue;
       for( int i=0; i<tree._len-leafs[k]; i++ ) {
         double g = gp._gss[k][i] == 0 // Constant response?
           ? 1000                      // Cap (exponential) learn, instead of dealing with Inf
           : learn_rate*m1class*gp._rss[k][i]/gp._gss[k][i];
         assert !Double.isNaN(g);
         ((LeafNode)tree.node(leafs[k]+i))._pred = g;
       }
     }
 
     // ----
     // ESL2, page 387.  Step 2b iv.  Cache the sum of all the trees, plus the
     // new tree, in the 'tree' columns.  Also, zap the NIDs for next pass.
     // Tree <== f(Tree)
     // Nids <== 0
     new MRTask2() {
       @Override public void map( Chunk chks[] ) {
         // For all tree/klasses
         for( int k=0; k<_nclass; k++ ) {
           final DTree tree = ktrees[k];
           if( tree == null ) continue;
           final Chunk nids = chk_nids(chks,k);
           final Chunk ct   = chk_tree(chks,k);
           for( int row=0; row<nids._len; row++ ) {
             int nid = (int)nids.at80(row);
             ct.set0(row, (float)(ct.at0(row) + ((LeafNode)tree.node(nid))._pred));
             nids.set0(row,0);
           }
         }
       }
     }.doAll(fr);
 
     // Collect leaves stats
     for (int i=0; i<ktrees.length; i++) ktrees[i].leaves = ktrees[i].len() - leafs[i];
     // DEBUG: Print the generated K trees
    printGenerateTrees(ktrees);
 
     return ktrees;
   }
 
 
   // ---
   // ESL2, page 387.  Step 2b iii.
   // Nids <== f(Nids)
   private class GammaPass extends MRTask2<GammaPass> {
     final DTree _trees[]; // Read-only, shared (except at the histograms in the Nodes)
     final int   _leafs[]; // Number of active leaves (per tree)
     // Per leaf: sum(res);
     double _rss[/*tree/klass*/][/*tree-relative node-id*/];
     // Per leaf:  sum(|res|*1-|res|)
     double _gss[/*tree/klass*/][/*tree-relative node-id*/];
     GammaPass(DTree trees[], int leafs[]) { _leafs=leafs; _trees=trees; }
     @Override public void map( Chunk[] chks ) {
       _gss = new double[_nclass][];
       _rss = new double[_nclass][];
       // For all tree/klasses
       for( int k=0; k<_nclass; k++ ) {
         final DTree tree = _trees[k];
         final int   leaf = _leafs[k];
         if( tree == null ) continue; // Empty class is ignored
         // A leaf-biased array of all active Tree leaves.
         final double gs[] = _gss[k] = new double[tree._len-leaf];
         final double rs[] = _rss[k] = new double[tree._len-leaf];
         final Chunk nids = chk_nids(chks,k); // Node-ids  for this tree/class
         final Chunk ress = chk_work(chks,k); // Residuals for this tree/class
         // If we have all constant responses, then we do not split even the
         // root and the residuals should be zero.
         if( tree.root() instanceof LeafNode ) continue;
         for( int row=0; row<nids._len; row++ ) { // For all rows
           int nid = (int)nids.at80(row);         // Get Node to decide from
           int oldnid = nid;
           if( tree.node(nid) instanceof UndecidedNode ) // If we bottomed out the tree
             nid = tree.node(nid)._pid;                  // Then take parent's decision
           DecidedNode dn = tree.decided(nid);           // Must have a decision point
           if( dn._split._col == -1 )                    // Unable to decide?
             dn = tree.decided(nid = dn._pid); // Then take parent's decision
           int leafnid = dn.ns(chks,row); // Decide down to a leafnode
           assert leaf <= leafnid && leafnid < tree._len;
           assert tree.node(leafnid) instanceof LeafNode;
           // Note: I can which leaf/region I end up in, but I do not care for
           // the prediction presented by the tree.  For GBM, we compute the
           // sum-of-residuals (and sum/abs/mult residuals) for all rows in the
           // leaf, and get our prediction from that.
           nids.set0(row,leafnid);
           if( ress.isNA0(row) ) continue;
           double res = ress.at0(row);
           double ares = Math.abs(res);
           gs[leafnid-leaf] += _nclass > 1 ? ares*(1-ares) : 1;
           rs[leafnid-leaf] += res;
         }
       }
     }
     @Override public void reduce( GammaPass gp ) {
       Utils.add(_gss,gp._gss);
       Utils.add(_rss,gp._rss);
     }
   }
 
   // ---
   // GBM DTree decision node: same as the normal DecidedNode, but
   // specifies a decision algorithm given complete histograms on all
   // columns.  GBM algo: find the lowest error amongst *all* columns.
   static class GBMDecidedNode extends DecidedNode<GBMUndecidedNode> {
     GBMDecidedNode( GBMUndecidedNode n ) { super(n); }
     @Override public GBMUndecidedNode makeUndecidedNode(DBinHistogram[] nhists ) {
       return new GBMUndecidedNode(_tree,_nid,nhists);
     }
 
     // Find the column with the best split (lowest score).  Unlike RF, GBM
     // scores on all columns and selects splits on all columns.
     @Override public DTree.Split bestCol( GBMUndecidedNode u ) {
       DTree.Split best = new DTree.Split(-1,-1,false,Double.MAX_VALUE,Double.MAX_VALUE,0L,0L);
       DHistogram hs[] = u._hs;
       if( hs == null ) return best;
       for( int i=0; i<hs.length; i++ ) {
         if( hs[i]==null || hs[i].nbins() <= 1 ) continue;
         DTree.Split s = hs[i].scoreMSE(i);
         if( s == null ) continue;
         if( best == null || s.se() < best.se() ) best = s;
         if( s.se() <= 0 ) break; // No point in looking further!
       }
       return best;
     }
   }
 
   // ---
   // GBM DTree undecided node: same as the normal UndecidedNode, but specifies
   // a list of columns to score on now, and then decide over later.
   // GBM algo: use all columns
   static class GBMUndecidedNode extends UndecidedNode {
     GBMUndecidedNode( DTree tree, int pid, DBinHistogram hs[] ) { super(tree,pid,hs); }
     // Randomly select mtry columns to 'score' in following pass over the data.
     // In GBM, we use all columns (as opposed to RF, which uses a random subset).
     @Override public int[] scoreCols( DHistogram[] hs ) { return null; }
   }
 
   // ---
   static class GBMLeafNode extends LeafNode {
     GBMLeafNode( DTree tree, int pid ) { super(tree,pid); }
     GBMLeafNode( DTree tree, int pid, int nid ) { super(tree,pid,nid); }
     // Insert just the predictions: a single byte/short if we are predicting a
     // single class, or else the full distribution.
     @Override protected AutoBuffer compress(AutoBuffer ab) { assert !Double.isNaN(_pred); return ab.put4f((float)_pred); }
     @Override protected int size() { return 4; }
   }
 }
