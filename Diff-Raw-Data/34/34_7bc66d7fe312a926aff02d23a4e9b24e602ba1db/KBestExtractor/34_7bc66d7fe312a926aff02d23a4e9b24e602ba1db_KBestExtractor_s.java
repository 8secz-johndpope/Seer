 /* This file is part of the Joshua Machine Translation System.
  * 
  * Joshua is free software; you can redistribute it and/or modify
  * it under the terms of the GNU Lesser General Public License as
  * published by the Free Software Foundation; either version 2.1
  * of the License, or (at your option) any later version.
  *
  * This library is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this library; if not, write to the Free
  * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
  * MA 02111-1307 USA
  */
 package joshua.decoder.hypergraph;
 
 import joshua.decoder.ff.FFTransitionResult;
 import joshua.decoder.ff.FeatureFunction;
 import joshua.decoder.ff.tm.Rule;
 import joshua.decoder.Support;
 import joshua.corpus.SymbolTable;
 import joshua.util.Regex;
 
 import java.io.BufferedWriter;
 import java.io.OutputStreamWriter;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.PriorityQueue;
 import java.util.logging.Logger;
 
 /**
  * this class implement:  lazy k-best extraction on a hyper-graph
  *to seed the kbest extraction, it only needs that each hyperedge should have the best_cost properly set, and it does not require any list being sorted
  *instead, the priority queue heap_cands will do internal sorting
  *In fact, the real crucial cost is the transition-cost at each hyperedge. We store the best-cost instead of the transition cost since it is easy to do pruning and
  *find one-best. Moreover, the transition cost can be recovered by get_transition_cost(), though somewhat expensive
  *
  * to recover the model cost for each individual model, we should either have access to the model, or store the model cost in the hyperedge 
  * (for example, in the case of disk-hypergraph, we need to store all these model cost at each hyperedge)
  *
  * @author Zhifei Li, <zhifei.work@gmail.com>
  * @version $LastChangedDate$
  */
 public class KBestExtractor {
 	
 	@SuppressWarnings("unused")
 	private static final Logger logger = Logger.getLogger(KBestExtractor.class.getName());
 	
 	private final HashMap<HGNode,VirtualItem> tbl_virtual_items = new HashMap<HGNode,VirtualItem>();
 	
 	/**
 	 * Shared symbol table for source language terminals,
 	 * target language terminals, and shared nonterminals.
 	 * <p>
 	 * It may be that separate tables should be maintained
 	 * for the source and target languages.
 	 * <p>
 	 * TODO It seems likely that only a target side symbol table is required
 	 *      for this class.
 	 */
 	private final SymbolTable p_symbolTable;
 	
 	
 	static String root_sym = "ROOT";
 	static int root_id;//TODO: bug
 	
 	//configuratoin option
 	private boolean extract_unique_nbest  = true;
 	private boolean extract_nbest_tree = false;
 	private boolean include_align=false;
 	private boolean add_combined_score=true;
 	private boolean isMonolingual = false;
 	private boolean performSanityCheck = true;
 	
 	public KBestExtractor(SymbolTable symbolTable) {
 		this.p_symbolTable = symbolTable;
 	}
 	
 	public KBestExtractor(SymbolTable symbolTable, boolean extract_unique_nbest_, boolean 	extract_nbest_tree_,  boolean include_align_,
 			boolean add_combined_score_,  boolean isMonolingual_,  boolean performSanityCheck_){
 		this.p_symbolTable = symbolTable;
 		root_id = p_symbolTable.addNonterminal(root_sym);
 		
 		this.extract_unique_nbest = extract_unique_nbest_;
 		this.extract_nbest_tree = extract_nbest_tree_;
 		this.include_align = include_align_;
 		this.add_combined_score = add_combined_score_;
 		this.isMonolingual = isMonolingual_;
 		this.performSanityCheck = performSanityCheck_;		
 	}
 	
 	
 //	########################################## kbest extraction algorithm ##########################	
 	public void lazy_k_best_extract_hg(
 		HyperGraph hg, ArrayList<FeatureFunction> l_models, int global_n,
 		int sent_id, BufferedWriter out		
 	) throws IOException {
 		//long start = System.currentTimeMillis();
 		reset_state();
 		if (null == hg.goal_item) return;
 		
 		BufferedWriter out2 = null;
 		if (null == out) {
 			out2 = new BufferedWriter(new OutputStreamWriter(System.out));
 		} else {
 			out2 = out;
 		}
 		
 		
 		//VirtualItem virtual_goal_item = add_virtual_item( hg.goal_item);
 		int next_n = 0;
 		while (true) {
 			/*
 			DerivationState cur = virtual_goal_item.lazy_k_best_extract_item(this ,++next_n,extract_unique_nbest,extract_nbest_tree,include_align);//global_n is not used at all
 			if( cur==null || virtual_goal_item.l_nbest.size()<next_n //do not have more hypthesis
 				|| virtual_goal_item.l_nbest.size()>global_n)//get enough hyps
 						break;
 			String hyp_str = get_kth_hyp(cur, sent_id, l_models, extract_nbest_tree, include_align, add_combined_score);
 			*/
 			String hyp_str = get_kth_hyp(hg.goal_item, ++next_n, sent_id, l_models);
 			if(hyp_str==null || next_n > global_n) break;
 
 			//write to files
 			out2.write(hyp_str);
 			out2.write("\n");
 		}
 		//g_time_kbest_extract += System.currentTimeMillis()-start;
 		//Support.write_log_line("time_kbest_extract: "+ Chart.g_time_kbest_extract, Support.INFO);
 	}
 		
 	//the only difference from the above function is: we store the nbest into an arraylist, instead of a file
 	public void lazy_k_best_extract_hg(HyperGraph hg, ArrayList<FeatureFunction> l_models, int global_n, int sent_id, ArrayList<String> out){
 		//long start = System.currentTimeMillis();
 		reset_state();
 		if(hg.goal_item==null)return;		
 		//VirtualItem virtual_goal_item = add_virtual_item( hg.goal_item);
 		int next_n=0;
 		while(true){
 			/*
 			DerivationState cur = virtual_goal_item.lazy_k_best_extract_item(this ,++next_n,extract_unique_nbest,extract_nbest_tree,include_align);//global_n is not used at all
 			if( cur==null || virtual_goal_item.l_nbest.size()<next_n //do not have more hypthesis
 				|| virtual_goal_item.l_nbest.size()>global_n)//get enough hyps
 						break;
 			String hyp_str = get_kth_hyp(cur, sent_id, l_models, extract_nbest_tree, include_align, add_combined_score);
 			*/
 			String hyp_str = get_kth_hyp(hg.goal_item, ++next_n, sent_id, l_models);
 			if(hyp_str==null || next_n > global_n) break;
 
 			//write to files
 			out.add(hyp_str);			
 		}
 		//g_time_kbest_extract += System.currentTimeMillis()-start;
 		//Support.write_log_line("time_kbest_extract: "+ Chart.g_time_kbest_extract, Support.INFO);
 	}
 	
 	
 	public void reset_state(){
 		tbl_virtual_items.clear();
 	}
 	
 	/*get the k-th hyp at the item it
 	 * format: sent_id ||| hyp ||| individual model cost ||| combined cost
 	 * sent_id<0: do not add sent_id
 	 * l_models==null: do not add model cost
 	 * add_combined_score==f: do not add combined model cost
 	 * */
 	//***************** you may need to reset_state() before you call this function for the first time
 	public String get_kth_hyp(HGNode it, int k,  int sent_id, ArrayList<FeatureFunction> l_models){
 		VirtualItem virtual_item = add_virtual_item(it);
 		DerivationState cur = virtual_item.lazy_k_best_extract_item(p_symbolTable, this, k);
 		if( cur==null) return null;
 		return get_kth_hyp(cur, sent_id, l_models);
 	}
 	
 	private String get_kth_hyp(DerivationState cur, int sent_id, ArrayList<FeatureFunction> l_models){
 		double[] model_cost = null;
 		if(l_models!=null) model_cost = new double[l_models.size()];		
		String str_hyp_numeric = cur.get_hypothesis(p_symbolTable, this, model_cost,l_models);	
 		//for(int k=0; k<model_cost.length; k++) System.out.println(model_cost[k]);
 		String str_hyp_str = convert_hyp_2_string(sent_id, cur, l_models, str_hyp_numeric, model_cost);
 		return str_hyp_str;
 	}
 	
 	/* non-recursive function
 	 * format: sent_id ||| hyp ||| individual model cost ||| combined cost
 	 * sent_id<0: do not add sent_id
 	 * l_models==null: do not add model cost
 	 * add_combined_score==f: do not add combined model cost
 	 * */
 	private String convert_hyp_2_string(int sent_id, DerivationState cur, ArrayList<FeatureFunction> l_models, String str_hyp_numeric, double[] model_cost){
 		String[] tem = Regex.spaces.split(str_hyp_numeric);
 		StringBuffer str_hyp = new StringBuffer();
 		
 		//####sent id
 		if (sent_id >= 0) { // valid sent id must be >=0
 			str_hyp.append(sent_id);
 			str_hyp.append(" ||| ");
 		}
 		
 		//TODO: consider_start_sym
 		//####hyp words
 		for (int t = 0; t < tem.length; t++) {
 			tem[t] = tem[t].trim();
 			if (extract_nbest_tree && ( tem[t].startsWith("(") || tem[t].endsWith(")"))) { // tree tag
 				if (tem[t].startsWith("(")) {
 					if (include_align) {
 						// we must account for the {i-j} substring
 						int ijStrIndex = tem[t].indexOf('{');
 						String tag = this.p_symbolTable.getWord(Integer.parseInt(tem[t].substring(1,ijStrIndex)));
 						str_hyp.append("(");
 						str_hyp.append(tag);
 						str_hyp.append(tem[t].substring(ijStrIndex)); // append {i-j}
 					} else {
 						String tag = this.p_symbolTable.getWord(Integer.parseInt(tem[t].substring(1)));
 						str_hyp.append("(");
 						str_hyp.append(tag);
 					}
 				} else {
 					//note: it may have more than two ")", e.g., "3499))"
 					int first_bracket_pos = tem[t].indexOf(")");//TODO: assume the tag/terminal does not have ")"
 					String tag = this.p_symbolTable.getWord(Integer.parseInt(tem[t].substring(0,first_bracket_pos)));
 					str_hyp.append(tag);
 					str_hyp.append(tem[t].substring(first_bracket_pos));
 				}
 			} else { // terminal symbol
 				str_hyp.append(this.p_symbolTable.getWord(Integer.parseInt(tem[t])));
 			}
 			if (t < tem.length-1) {
 				str_hyp.append(" ");
 			}
 		}
 		
 		//####individual model cost, and final transition cost
 		if (null != model_cost) {
 			str_hyp.append(" |||");
 			double tem_sum = 0.0;
 			for (int k = 0; k < model_cost.length; k++) { /* note that all the transition cost (including finaltransition cost) is already stored in the hyperedge */
 				str_hyp.append(String.format(" %.3f", -model_cost[k]));
 				tem_sum += model_cost[k]*l_models.get(k).getWeight();
 			}
 			//sanity check
 			if (performSanityCheck) {
 				if (Math.abs(cur.cost - tem_sum) > 1e-2) {
 					System.out.println("In nbest extraction, Cost does not match; cur.cost: " + cur.cost + "; temsum: " +tem_sum);
 					for (int k = 0; k < model_cost.length; k++) {
 						System.out.println("model weight: " + l_models.get(k).getWeight() + "; cost: " +model_cost[k]);
 					}
 					System.exit(1);
 				}
 			}
 		}
 		
 		//####combined model cost
 		if (add_combined_score) {
 			str_hyp.append(String.format(" ||| %.3f",-cur.cost));
 		}
 		return str_hyp.toString();
 	}
 		
 	
 //add the virtualitem is necessary
 	private VirtualItem add_virtual_item(HGNode it) {
 		VirtualItem res = tbl_virtual_items.get(it);
 		if (null == res) {
 			res = new VirtualItem(it);
 			tbl_virtual_items.put(it, res);
 		}
 		return res;
 	}
 
 	
 //=========================== class VirtualItem ===========================
 	/*to seed the kbest extraction, it only needs that each hyperedge should have the best_cost properly set, and it does not require any list being sorted
 	  *instead, the priority queue heap_cands will do internal sorting*/
 
 	private  class VirtualItem {
 		public ArrayList<DerivationState> l_nbest = new ArrayList<DerivationState>();//sorted ArrayList of DerivationState, in the paper is: D(^) [v]
 		private PriorityQueue<DerivationState> heap_cands = null; // remember frontier states, best-first;  in the paper, it is called cand[v]
 		private HashMap<String, Integer>  derivation_tbl = null; // rememeber which DerivationState has been explored; why duplicate, e.g., 1 2 + 1 0 == 2 1 + 0 1 
 		private HashMap<String,Integer> nbest_str_tbl = null; //reember unique *string* at each item, used for unique-nbest-string extraction 
 		HGNode p_item = null;
 		
 		public VirtualItem(HGNode it) {
 			this.p_item = it;
 		}
 		
 		//return: the k-th hyp or null; k is started from one
 		private DerivationState lazy_k_best_extract_item(SymbolTable p_symbol,
 			KBestExtractor kbest_extator,
 			int             k
 		) {
 			if (l_nbest.size() >= k) { // no need to continue
 				return l_nbest.get(k-1);
 			}
 			
 			//### we need to fill in the l_nest in order to get k-th hyp
 			DerivationState res = null;
 			if (null == heap_cands) {
 				get_candidates(p_symbol, kbest_extator);
 			}
 			int t_added = 0; //sanity check
 			while (l_nbest.size() < k) {
 				if (heap_cands.size() > 0) {
 					res = heap_cands.poll();
 					//derivation_tbl.remove(res.get_signature());//TODO: should remove? note that two state may be tied because the cost is the same
 					if (extract_unique_nbest) {
						String res_str = res.get_hypothesis(p_symbol,kbest_extator, null,null);
 						// We pass false for extract_nbest_tree because we want; 
 						// to check that the hypothesis *strings* are unique,
 						// not the trees.
 						//@todo zhifei: this causes trouble to monolingual grammar as there is only one *string*, need to fix it
 						if (! nbest_str_tbl.containsKey(res_str)) {
 							l_nbest.add(res);
 							nbest_str_tbl.put(res_str,1);
 						}
 					} else {
 						l_nbest.add(res);
 					}
 					lazy_next(p_symbol, kbest_extator, res);//always extend the last, add all new hyp into heap_cands
 					
 					//debug: sanity check
 					t_added++;
 					if ( ! extract_unique_nbest && t_added > 1){//this is possible only when extracting unique nbest
 						Support.write_log_line("In lazy_k_best_extract, add more than one time, k is " + k, Support.ERROR);
 						System.exit(1);
 					}
 				} else {
 					break;
 				}
 			}
 			if (l_nbest.size() < k) {
 				res = null;//in case we do not get to the depth of k
 			}
 			//debug: sanity check
 			//if(l_nbest.size()>=k && l_nbest.get(k-1)!=res){System.out.println("In lazy_k_best_extract, ranking is not correct ");System.exit(1);}
 			
 			return res;
 		}
 		
 		//last: the last item that has been selected, we need to extend it
 		//get the next hyp at the "last" hyperedge
 		private void lazy_next(SymbolTable p_symbol, KBestExtractor kbest_extator, DerivationState last){
 			if(last.p_edge.get_ant_items()==null)
 				return;
 			for(int i=0; i < last.p_edge.get_ant_items().size();i++){//slide the ant item
 				HGNode it = (HGNode) last.p_edge.get_ant_items().get(i);
 				VirtualItem virtual_it = kbest_extator.add_virtual_item(it);
 				int[] new_ranks = new int[last.ranks.length];
 				for(int c=0; c<new_ranks.length;c++)
 					new_ranks[c]=last.ranks[c];				
 				
 				new_ranks[i]=last.ranks[i]+1;
 				String new_sig = getDerivationStateSignature(last.p_edge, new_ranks, last.hyperedge_pos);
 				
 				//why duplicate, e.g., 1 2 + 1 0 == 2 1 + 0 1 
 				if(derivation_tbl.containsKey(new_sig)==true){
 					continue;
 				}
 				virtual_it.lazy_k_best_extract_item(p_symbol, kbest_extator, new_ranks[i]);
 				if(new_ranks[i]<=virtual_it.l_nbest.size()//exist the new_ranks[i] derivation
 				  /*&& "t" is not in heap_cands*/ ){//already checked before, check this condition
 					double cost= last.cost - ((DerivationState)virtual_it.l_nbest.get(last.ranks[i]-1)).cost + ((DerivationState)virtual_it.l_nbest.get(new_ranks[i]-1)).cost;
 					DerivationState t = new DerivationState(last.p_parent_node, last.p_edge, new_ranks, cost, last.hyperedge_pos);
 					heap_cands.add(t);
 					derivation_tbl.put(new_sig,1);
 				}				
 			}		
 		}
 
 		//this is the seeding function, for example, it will get down to the leaf, and sort the terminals
 		//get a 1best from each hyperedge, and add them into the heap_cands
 		private void get_candidates(SymbolTable p_symbol, KBestExtractor kbest_extator){
 			heap_cands=new PriorityQueue<DerivationState>();
 			derivation_tbl = new HashMap<String, Integer> ();
 			if(extract_unique_nbest==true)
 				nbest_str_tbl=new HashMap<String,Integer> ();
 			//sanity check
 			if (null == p_item.l_hyperedges) {
 				System.out.println("Error, l_hyperedges is null in get_candidates, must be wrong");
 				System.exit(1);
 			}
 			int pos=0;
 			for(HyperEdge hyper_edge : p_item.l_hyperedges){				
 				DerivationState t = get_best_derivation(p_symbol,kbest_extator, p_item, hyper_edge,pos);
 //				why duplicate, e.g., 1 2 + 1 0 == 2 1 + 0 1 , but here we should not get duplicate				
 				if(derivation_tbl.containsKey(t.get_signature())==false){
 					heap_cands.add(t);
 					derivation_tbl.put(t.get_signature(),1);
 				}else{//sanity check
 					System.out.println("Error: get duplicate derivation in get_candidates, this should not happen");
 					System.out.println("signature is " + t.get_signature());
 					System.out.println("l_hyperedge size is " + p_item.l_hyperedges.size());
 					System.exit(1);
 				}
 				pos++;
 			}	
 			
 //			TODO: if tem.size is too large, this may cause unnecessary computation, we comment the segment to accormodate the unique nbest extraction			
 			/*if(tem.size()>global_n){
 				heap_cands=new PriorityQueue<DerivationState>();
 				for(int i=1; i<=global_n; i++)
 					heap_cands.add(tem.poll());
 			}else
 				heap_cands=tem;
 			*/	
 		}
 		
 		//get my best derivation, and recursively add 1best for all my children, used by get_candidates only
 		private DerivationState get_best_derivation(SymbolTable p_symbol, KBestExtractor kbest_extator, HGNode parent_item, HyperEdge hyper_edge, int deduct_pos){
 			int[] ranks;
 			double cost=0;
 			if(hyper_edge.get_ant_items()==null){//axiom
 				ranks=null;
 				cost=hyper_edge.best_cost;//seeding: this hyperedge only have one single translation for the terminal symbol
 			}else{//best combination					
 				ranks = new int[hyper_edge.get_ant_items().size()];					
 				for(int i=0; i < hyper_edge.get_ant_items().size();i++){//make sure the 1best at my children is ready
 					ranks[i]=1;//rank start from one									
 					HGNode child_it = (HGNode) hyper_edge.get_ant_items().get(i);//add the 1best for my children
 					VirtualItem virtual_child_it = kbest_extator.add_virtual_item(child_it);
 					virtual_child_it.lazy_k_best_extract_item(p_symbol, kbest_extator,  ranks[i]);
 				}
 				cost = hyper_edge.best_cost;//seeding
 			}				
 			DerivationState t = new DerivationState(parent_item, hyper_edge, ranks, cost, deduct_pos );
 			return t;
 		}
 	};
 	
 
 //	################# class DerivationState #######################
 	/*each Item will maintain a list of this, each of which corresponds to a hyperedge and its children's ranks
 	 * remember the ranks of a hyperedge node
 	 * used for kbest extraction*/
 	
 	//each DerivationState rougly correponds a hypothesis 
 	private class DerivationState implements Comparable<DerivationState> 
 	{
 		HGNode p_parent_node;
 		HyperEdge p_edge;//in the paper, it is "e"		
 		//**lesson: once we define this as a static variable, which cause big trouble
 		int hyperedge_pos; //this is my position in my parent's Item.l_hyperedges, used for signature calculation
 		int[] ranks;//in the paper, it is "j", which is a ArrayList of size |e|
 		double cost;//the cost of this hypthesis
 		
 		public DerivationState(HGNode pa, HyperEdge e, int[] r, double c ,int pos){
 			p_parent_node = pa;
 			p_edge =e ;
 			ranks = r;
 			cost=c;
 			hyperedge_pos=pos;
 		}
 		
 		private String get_signature(){
 			StringBuffer res = new StringBuffer();
 			//res.apend(p_edge2.toString());//Wrong: this may not be unique to identify a hyperedge (as it represent the class name and hashcode which my be equal for different objects)
 			res.append(hyperedge_pos);
 			if(ranks!=null)
 				for(int i=0; i<ranks.length;i++){
 					res.append(" ");
 					res.append(ranks[i]);			
 				}
 			return res.toString();
 		}
 		
			
 		//get the numeric sequence of the particular hypothesis
 		//if want to get model cost, then have to set model_cost and l_models
		private String get_hypothesis(SymbolTable p_symbol, KBestExtractor kbest_extator, double[] model_cost, ArrayList<FeatureFunction> l_models){
 			//### accumulate cost of p_edge into model_cost if necessary
 			if(model_cost!=null) compute_cost(p_parent_node, p_edge, model_cost, l_models);
 			
 			//### get hyp string recursively
 			StringBuffer res = new StringBuffer();			
 			Rule rl = p_edge.get_rule();
 			if(rl==null){//hyperedges under "goal item" does not have rule
				if(extract_nbest_tree==true){
 					//res.append("(ROOT ");
 					res.append("(");
 					res.append(root_id);
 					if (include_align == true) {
 						// append "{i-j}"
 						res.append("{"); res.append(p_parent_node.i); res.append("-"); res.append(p_parent_node.j); res.append("}");
 					}
 					res.append(" ");
 				}
 				for(int id=0; id < p_edge.get_ant_items().size();id++){
 					HGNode child = (HGNode)p_edge.get_ant_items().get(id);
 					VirtualItem virtual_child = kbest_extator.add_virtual_item(child);
					res.append(((DerivationState)virtual_child.l_nbest.get(ranks[id]-1)).get_hypothesis(p_symbol,kbest_extator, model_cost,l_models));
 	    			if(id<p_edge.get_ant_items().size()-1) res.append(" ");		
     			}
				if(extract_nbest_tree==true) res.append(")");		
 			}else{			
				if(extract_nbest_tree==true){
 					res.append("(");
 					res.append(rl.getLHS());
 					if (include_align == true) {
 						// append "{i-j}"
 						res.append("{"); res.append(p_parent_node.i); res.append("-"); res.append(p_parent_node.j); res.append("}");
 					}
 					res.append(" ");
 				}
 				if(isMonolingual==false){//bilingual
 					int[] english = rl.getEnglish();
 					for(int c=0; c<english.length; c++){
 			    		if(p_symbol.isNonterminal(english[c])==true){
 			    			int id=p_symbol.getTargetNonterminalIndex(english[c]);
 			    			HGNode child = (HGNode)p_edge.get_ant_items().get(id);
 			    			VirtualItem virtual_child =kbest_extator.add_virtual_item(child);
			    			res.append(((DerivationState)virtual_child.l_nbest.get(ranks[id]-1)).get_hypothesis(p_symbol,kbest_extator,  model_cost, l_models));
 			    		}else{
 			    			res.append(english[c]);
 			    		}
 			    		if(c<english.length-1) res.append(" ");
 					}
 				}else{//monolingual
 					int[] french = rl.getFrench();
 					int nonTerminalID =0;//the position of the non-terminal in the rule
 					for(int c=0; c<french.length; c++){
 			    		if(p_symbol.isNonterminal(french[c])==true){
 			    			HGNode child = (HGNode)p_edge.get_ant_items().get(nonTerminalID);
 			    			VirtualItem virtual_child =kbest_extator.add_virtual_item(child);
			    			res.append(((DerivationState)virtual_child.l_nbest.get(ranks[nonTerminalID]-1)).get_hypothesis(p_symbol,kbest_extator, model_cost, l_models));
 			    			nonTerminalID++;
 			    		}else{
 			    			res.append(french[c]);
 			    		}
 			    		if(c<french.length-1) res.append(" ");
 					}
 				}
				if(extract_nbest_tree==true) res.append(")");
 			}
 			
 			return res.toString();
 		}
 		
 		/*
 		//TODO: we assume at most one lm, and the LM is the only non-stateles model
 		//another potential difficulty in handling multiple LMs: symbol synchronization among the LMs
 		//accumulate hyperedge cost into model_cost[], used by get_hyp()
 		private void compute_cost_not_used(HyperEdge dt, double[] model_cost, ArrayList l_models){
 			if(model_cost==null) return;
 			
 			//System.out.println("Rule is: " + dt.rule.toString());
 			double stateless_transition_cost =0;
 			FeatureFunction lm_model =null;
 			int lm_model_index = -1;
 			for(int k=0; k< l_models.size(); k++){
 				FeatureFunction m = (FeatureFunction) l_models.get(k);	
 				double t_res =0;
 				if(m.isStateful() == false){//stateless feature
 					if(dt.get_rule()!=null){//hyperedges under goal item do not have rules
 						FFTransitionResult tem_tbl =  m.transition(dt.get_rule(), null, -1, -1);
 						t_res = tem_tbl.getTransitionCost();
 					}else{//final transtion
 						t_res = m.finalTransition(null);
 					}
 					model_cost[k] += t_res;
 					stateless_transition_cost += t_res*m.getWeight();
 				}else{
 					lm_model = m;
 					lm_model_index = k;
 				}
 			}
 			if(lm_model_index!=-1)//have lm model
 				model_cost[lm_model_index] += (dt.get_transition_cost(false)-stateless_transition_cost)/lm_model.getWeight();
 		}
 		*/
 		
 		
 		private void compute_cost(HGNode parent_item, HyperEdge dt, double[] model_cost, ArrayList<FeatureFunction> l_models){
 			if(model_cost==null) return;
 			//System.out.println("Rule is: " + dt.rule.toString());
 			
 			for(int k=0; k< l_models.size(); k++){
 				FeatureFunction m = (FeatureFunction) l_models.get(k);	
 				double t_res =0;
 							
 				if(dt.get_rule()!=null){//hyperedges under goal item do not have rules
 					FFTransitionResult tem_tbl =  HyperGraph.computeTransition(dt, m, parent_item.i, parent_item.j);
 					t_res = tem_tbl.getTransitionCost();
 				}else{//final transtion
 					t_res = HyperGraph.computeFinalTransition(dt, m);
 				}
 				model_cost[k] += t_res;
 			}			
 		}
 		
 		
 		//natual order by cost
 		public int compareTo(DerivationState another) throws ClassCastException {
 		    if (!(another instanceof DerivationState))
 		      throw new ClassCastException("An Derivation object expected.");
 		    if(this.cost < ((DerivationState)another).cost)
 		    	return -1;
 		    else if(this.cost == ((DerivationState)another).cost)
 		    	return 0;
 		    else
 		    	return 1; 
 		}
 	}//end of Class DerivationState
 
 	private String getDerivationStateSignature(HyperEdge p_edge2, int[] ranks2, int pos){
 		StringBuffer res = new StringBuffer();
 		//res.apend(p_edge2.toString());//Wrong: this may not be unique to identify a hyperedge (as it represent the class name and hashcode which my be equal for different objects)
 		res.append(pos);
 		if(ranks2!=null)
 			for(int i=0; i<ranks2.length;i++){
 				res.append(" ");
 				res.append(ranks2[i]);			
 			}
 		return res.toString();
 	}
 }
