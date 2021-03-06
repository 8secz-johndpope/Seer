 package edu.uci.ics.genomix.pregelix.operator.scaffolding2;
 
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collections;
 import java.util.Iterator;
 import java.util.SortedSet;
 
 import org.apache.hadoop.conf.Configuration;
 
 import edu.uci.ics.genomix.data.config.GenomixJobConf;
 import edu.uci.ics.genomix.data.types.DIR;
 import edu.uci.ics.genomix.data.types.EDGETYPE;
 import edu.uci.ics.genomix.data.types.Kmer;
 import edu.uci.ics.genomix.data.types.Node.NeighborInfo;
 import edu.uci.ics.genomix.data.types.ReadHeadInfo;
 import edu.uci.ics.genomix.data.types.VKmer;
 import edu.uci.ics.genomix.data.types.VKmerList;
 import edu.uci.ics.genomix.pregelix.base.DeBruijnGraphCleanVertex;
 import edu.uci.ics.genomix.pregelix.base.MessageWritable;
 import edu.uci.ics.genomix.pregelix.base.VertexValueWritable;
 import edu.uci.ics.genomix.pregelix.operator.scaffolding2.RayMessage.RayMessageType;
 import edu.uci.ics.pregelix.api.job.PregelixJob;
 
 public class RayVertex extends DeBruijnGraphCleanVertex<RayValue, RayMessage> {
     private static DIR INITIAL_DIRECTION;
     private static boolean HAS_PAIRED_END_READS;
     private static int MAX_READ_LENGTH;
     private static int MAX_OUTER_DISTANCE;
     private static int MIN_OUTER_DISTANCE;
     private static int MAX_DISTANCE; // the max(readlengths, outerdistances)
 
     public RayVertex() {
         outgoingMsg = new RayMessage();
     }
 
     @Override
     public void configure(Configuration conf) {
         super.configure(conf);
         initVertex();
         INITIAL_DIRECTION = DIR.FORWARD; // TODO set the INITIAL-DIRECTION appropriately
         HAS_PAIRED_END_READS = GenomixJobConf.outerDistanceMeans != null;
         MAX_READ_LENGTH = Integer.MIN_VALUE;
         MAX_OUTER_DISTANCE = Integer.MIN_VALUE;
         MIN_OUTER_DISTANCE = Integer.MAX_VALUE;
         for (byte libraryId = 0; libraryId < GenomixJobConf.readLengths.size(); libraryId++) {
             MAX_READ_LENGTH = Math.max(MAX_READ_LENGTH, GenomixJobConf.readLengths.get(libraryId));
             if (HAS_PAIRED_END_READS && GenomixJobConf.outerDistanceMeans.containsKey(libraryId)) {
                 MAX_OUTER_DISTANCE = Math.max(MAX_OUTER_DISTANCE, GenomixJobConf.outerDistanceMeans.get(libraryId)
                         + GenomixJobConf.outerDistanceStdDevs.get(libraryId));
                 MIN_OUTER_DISTANCE = Math.min(MIN_OUTER_DISTANCE, GenomixJobConf.outerDistanceMeans.get(libraryId)
                         - GenomixJobConf.outerDistanceStdDevs.get(libraryId));
             }
         }
         MAX_DISTANCE = Math.max(MAX_OUTER_DISTANCE, MAX_READ_LENGTH);
     }
 
     @Override
     public void compute(Iterator<RayMessage> msgIterator) throws Exception {
         if (getSuperstep() == 1) {
             initVertex();
             if (isStartSeed()) {
                 msgIterator = getStartMessage();
             }
         }
         scaffold(msgIterator);
         voteToHalt();
     }
 
     /**
      * @return whether or not this node meets the "seed" criteria
      */
     private boolean isStartSeed() {
         // TODO Auto-generated method stub
         return getVertexId().toString().equals("AGGTCC");
     }
 
     /**
      * @return an iterator with a single CONTINUE_WALK message including this node only.
      *         the edgeTypeBackToFrontier should be the flip of INITIAL_DIRECTION
      *         this allows us to call the `scaffold` function the same way in all iterations.
      */
     private Iterator<RayMessage> getStartMessage() {
         RayMessage initialMsg = new RayMessage();
         initialMsg.setMessageType(RayMessageType.CONTINUE_WALK);
         initialMsg.setWalkLength(0);
         if (INITIAL_DIRECTION == DIR.FORWARD) {
             initialMsg.setFrontierFlipped(false);
             initialMsg.setEdgeTypeBackToFrontier(EDGETYPE.RR);
         } else {
             initialMsg.setFrontierFlipped(true);
             initialMsg.setEdgeTypeBackToFrontier(EDGETYPE.FF);
         }
         return new ArrayList<RayMessage>(Collections.singletonList(initialMsg)).iterator();
     }
 
     // local variables for scaffold
     private ArrayList<RayMessage> requestScoreMsgs = new ArrayList<>();
     private ArrayList<RayMessage> aggregateScoreMsgs = new ArrayList<>();
 
     private void scaffold(Iterator<RayMessage> msgIterator) {
         // TODO since the messages aren't synchronized by iteration, we might want to  
         // manually order our messages to make the process a little more deterministic
         // for example, one node could receive both a "continue" message and a "request kmer"
         // message in the same iteration.  Who should win? the result will be an intersection 
         // in either case but should the "continue" msg always be allowed to be processed and
         // a "stop" always go back towards the "request kmer" walk?
         VKmer id = getVertexId();
         RayValue vertex = getVertexValue();
 
         requestScoreMsgs.clear();
         aggregateScoreMsgs.clear();
         while (msgIterator.hasNext()) {
             RayMessage msg = msgIterator.next();
             switch (msg.getMessageType()) {
                 case CONTINUE_WALK:
                     startBranchComparison(msg);
                     break;
                 case REQUEST_KMER:
                     sendKmerToWalkNodes(msg);
                     break;
                 case REQUEST_SCORE:
                     // batch-process these (have to truncate to min length)
                     RayMessage reqMsg = new RayMessage();
                     reqMsg.setAsCopy(msg);
                     requestScoreMsgs.add(reqMsg);
                     break;
                 case AGGREGATE_SCORE:
                     RayMessage aggMsg = new RayMessage();
                     aggMsg.setAsCopy(msg);
                     aggregateScoreMsgs.add(aggMsg);
                     break;
                 case PRUNE_EDGE:
                     // remove the given edge back towards the frontier node.  Also, clear the "visited" state of this node
                     // so that other walks are free to include me
                     vertex.getEdges(msg.getEdgeTypeBackToFrontier()).remove(msg.getSourceVertexId());
                     vertex.visited = false;
                     break;
                 case STOP:
                     // I am a frontier node but one of my neighbors was already included in a different walk.
                     // that neighbor is marked "intersection" and I need to remember the stopped state.  No path
                     // can continue out of me in the future, even when I receive aggregate scores
                     vertex.stopSearch = true;
                     // TODO obey this stop search in the CONTINUE branch
                     break;
             }
         }
         if (requestScoreMsgs.size() > 0) {
             sendScoresToFrontier(requestScoreMsgs);
         }
         if (aggregateScoreMsgs.size() > 0) {
             compareScoresAndPrune(aggregateScoreMsgs);
         }
     }
 
     private void startBranchComparison(RayMessage msg) {
         // I'm the new frontier node... time to do some pruning
         VKmer id = getVertexId();
         RayValue vertex = getVertexValue();
 
         // must explicitly check if this node was visited already (would have happened in 
         // this iteration as a previously processed msg!)
         if (vertex.visited) {
             vertex.intersection = true;
             vertex.stopSearch = true;
             return;
         }
         vertex.visited = true;
         // if the prev frontier was flipped and I came across a FR/RF, I'm back to unflipped
         vertex.flippedFromInitialDirection = msg.isCandidateFlipped();
         DIR nextDir = msg.getEdgeTypeBackToFrontier().mirror().neighborDir();
 
         msg.visitNode(id, vertex);
 
         if (vertex.degree(nextDir) == 0) {
             // this walk has reached a dead end!  nothing to do in this case.
             return;
         } else if (vertex.degree(nextDir) == 1) {
             // one neighbor -> just send him a continue msg w/ me added to the list
             NeighborInfo next = vertex.getSingleNeighbor(nextDir);
             msg.setEdgeTypeBackToFrontier(next.et.mirror());
             msg.setFrontierFlipped(vertex.flippedFromInitialDirection);
             sendMsg(next.kmer, msg);
         } else {
             // 2+ neighbors -> start evaluating candidates via a REQUEST_KMER msg
             msg.setMessageType(RayMessageType.REQUEST_KMER);
             msg.setFrontierFlipped(vertex.flippedFromInitialDirection);
             msg.setSourceVertexId(id);
             for (EDGETYPE et : nextDir.edgeTypes()) {
                 for (VKmer next : vertex.getEdges(et)) {
                     msg.setEdgeTypeBackToFrontier(et.mirror());
                     sendMsg(next, msg);
                 }
             }
         }
     }
 
     /**
      * I'm a candidate and need to send my kmer to all the nodes of the walk
      * if I've been visited already, I need to send a STOP msg back to the frontier node and
      * mark this node as an "intersection" between multiple seeds
      * 
      * @param msg
      */
     private void sendKmerToWalkNodes(RayMessage msg) {
         VKmer id = getVertexId();
         RayValue vertex = getVertexValue();
 
         // already visited -> the frontier must stop!
         if (vertex.visited) {
             vertex.intersection = true;
             outgoingMsg.reset();
             outgoingMsg.setMessageType(RayMessageType.STOP);
             sendMsg(msg.getSourceVertexId(), outgoingMsg);
             return;
         }
 
         outgoingMsg.reset();
         outgoingMsg.setMessageType(RayMessageType.REQUEST_SCORE);
         outgoingMsg.setSourceVertexId(msg.getSourceVertexId()); // frontier node
         outgoingMsg.setFrontierFlipped(msg.getFrontierFlipped());
         outgoingMsg.setEdgeTypeBackToFrontier(msg.getEdgeTypeBackToFrontier());
         outgoingMsg.setToScoreId(id); // candidate node (me)
         outgoingMsg.setToScoreKmer(vertex.getInternalKmer());
         outgoingMsg.setWalkLength(msg.getWalkLength());
         for (int i = 0; i < msg.getWalkIds().size(); i++) {
             outgoingMsg.getWalkOffsets().clear();
             outgoingMsg.getWalkOffsets().add(msg.getWalkOffsets().get(i)); // the offset of the destination node
             outgoingMsg.getWalkIds().clear();
             outgoingMsg.getWalkIds().append(msg.getWalkIds().getPosition(i));
             sendMsg(msg.getWalkIds().getPosition(i), outgoingMsg);
         }
     }
 
     /**
      * I'm a node along the walk and am receiving a kmer that might be in my reads
      * I should be receiving multiple kmers (from all branches adjacent to the frontier)
      * I need to score the kmers but should truncate the kmers so they're all the same length before scoring.
      * after scoring, I should send the scores to the frontier node.
      * if I don't have any reads that are relevant (the walk has progressed beyond me),
      * then I shouldn't send a msg to the frontier node. That way, I won't be included in future queries.
      * 
      * @param msg
      */
     private void sendScoresToFrontier(ArrayList<RayMessage> msgs) {
         VKmer id = getVertexId();
         RayValue vertex = getVertexValue();
 
         // all msgs should have the same total length and describe me as being at the same offset
         int myOffset = msgs.get(0).getWalkOffsets().get(0);
         int walkLength = msgs.get(0).getWalkLength();
         VKmer frontierNode = msgs.get(0).getSourceVertexId();
 
         // if the search has progressed beyond the reads I contain, don't do any scoring and don't report back
         // to the frontier node. This effectively prunes me from the walk (I won't be queried anymore)
         if (vertex.isOutOfRange(myOffset, walkLength, MAX_DISTANCE)) {
             if (id.equals(frontierNode)) {
                 // special case: I am the frontier node. Send an empty note just in case no 
                 // other nodes in the walk report back
                 outgoingMsg.reset();
                 outgoingMsg.setMessageType(RayMessageType.AGGREGATE_SCORE);
                 outgoingMsg.setWalkLength(walkLength);
                 outgoingMsg.getWalkOffsets().add(myOffset);
                 outgoingMsg.getWalkIds().append(id);
                 sendMsg(frontierNode, outgoingMsg);
             }
             return;
         }
 
         // get the smallest kmer in all the messages I've received
         int minLength = msgs.get(0).getWalkIds().getPosition(0).getKmerLetterLength();
         for (int i = 1; i < msgs.size(); i++) {
             minLength = Math.min(minLength, msgs.get(i).getWalkIds().getPosition(0).getKmerLetterLength());
         }
         minLength = minLength - Kmer.getKmerLength() + 1;
 
         // I'm now allowed to score the first minLength kmers according to my readids
         RayScores singleEndScores = voteFromReads(true, vertex, msgs, myOffset, walkLength, minLength);
         RayScores pairedEndScores = HAS_PAIRED_END_READS ? voteFromReads(false, vertex, msgs, myOffset, walkLength,
                 minLength) : null;
 
         outgoingMsg.reset();
         outgoingMsg.setMessageType(RayMessageType.AGGREGATE_SCORE);
         outgoingMsg.setWalkLength(walkLength);
         outgoingMsg.getWalkOffsets().add(myOffset);
         outgoingMsg.getWalkIds().append(id);
         outgoingMsg.setSingleEndScores(singleEndScores);
         outgoingMsg.setPairedEndScores(pairedEndScores);
         sendMsg(frontierNode, outgoingMsg);
     }
 
     /**
      * use the reads present in this vertex to score the kmers in this message list.
      * For each message,
      * | for each read in a subset of reads in this vertex
      * | | run a sliding window of length (original) Kmer.length
      * | | | see if all the letters in the sliding window match the read
      * So somehow, we've turned this into a n**4 operation :(
      * // TODO for single-end reads, we could alternatively count how many letters in the VKmers match
      * // or we could base the score on something like edit distance
      */
     private static RayScores voteFromReads(boolean singleEnd, RayValue vertex, ArrayList<RayMessage> msgs,
             int nodeOffset, int walkLength, int msgKmerLength) {
         SortedSet<ReadHeadInfo> readSubsetOrientedWithSearch = getReadSubsetOrientedWithSearch(singleEnd, vertex,
                 nodeOffset, walkLength);
 
         // nothing like nested for loops 4 levels deep (!)
         RayScores scores = new RayScores();
         for (RayMessage msg : msgs) {
             VKmer candidateKmer;
             if (singleEnd) {
                // for single-end reads, we need the candidate in the same orientation as the readheads
                boolean sameOrientationAsMe = vertex.flippedFromInitialDirection == msg.isCandidateFlipped();
                candidateKmer = sameOrientationAsMe ? msg.getToScoreKmer() : msg.getToScoreKmer().reverse();
             } else {
                // for paired-end reads, we place the candidate in the same orientation as the search started
                 candidateKmer = msg.isCandidateFlipped() ? msg.getToScoreKmer() : msg.getToScoreKmer().reverse();
             }
             int ruleATotal = 0, ruleBTotal = 0, ruleCTotal = 0;
             for (ReadHeadInfo read : readSubsetOrientedWithSearch) {
                 for (int kmerIndex = 0; kmerIndex < msgKmerLength; kmerIndex++) {
                     boolean match = false;
                     // TODO we currently keep the score separately for each kmer we're considering
                     // ruleC is about the minimum value in the comparison of the single kmers adjacent to the frontier
                     // but we're currently using it as the minimum across many kmers.  We'll have to think about this 
                     // rule some more and what it means in a merged graph
 
                     if (singleEnd) {
                         if (read.getThisReadSequence().matchesExactly(
                                 walkLength - nodeOffset - read.getOffset() + kmerIndex, candidateKmer, kmerIndex,
                                 Kmer.getKmerLength())) {
                             match = true;
                         }
                     } else {
                         int readLength = 100;
                         int outerDistanceMean = 500;
                         int outerDistanceStd = 30;
                         int mateStart = nodeOffset + read.getOffset() + outerDistanceMean - readLength;
                         int candidateInMate = walkLength - mateStart + kmerIndex;
                         // since read.thisSeq is in the same orientation as the search direction, 
                         // the mate sequence is flipped wrt search direction. we reverse it to be in 
                         // the same direction as the search direction.
                        if (read.getMateReadSequence()
                                .reverse()
                                .matchesInRange(candidateInMate - outerDistanceStd,
                                        candidateInMate + outerDistanceStd + Kmer.getKmerLength(), candidateKmer,
                                        kmerIndex, Kmer.getKmerLength())) {
                             match = true;
                         }
                     }
                     if (match) {
                         ruleATotal += walkLength - nodeOffset - read.getOffset();
                         ruleBTotal++;
                         ruleCTotal++;
                     }
                 }
             }
             scores.addRuleCounts(msg.getEdgeTypeBackToFrontier().mirror(), msg.getToScoreId(), ruleATotal, ruleBTotal,
                     ruleCTotal);
         }
         if (scores.size() > 0) {
             return scores;
         } else {
             return null;
         }
     }
 
     private static SortedSet<ReadHeadInfo> getReadSubsetOrientedWithSearch(boolean singleEnd, RayValue vertex,
             int nodeOffset, int walkLength) {
         // select out the readheads that might apply to this query
         // here's some ascii art trying to explain what the heck is going on
         //
         // the * indicates the walk length and is the start offset of the candidates c1 and c2
         // 
         //  |0    |10    |20    |30    |40   
         //                              /--c1
         //  ------->  ------->  ------>*
         //                              \--c2
         //              A1-------------------->
         //  B1------------------->
         //  C1--->                       <----C2
         //    D1--->       <---D2
         //                E1-->                      <---E2
         //
         // if our read length is 25, only A1 and C1 will apply to our c1/c2 decision.
         // the point here is to skip scoring of any single-end reads too far to the left 
         // and any paired-end reads that aren't in the right range.
         // TODO do we want to separate different libraries into different lists?  
         // That way, we would be able to skip checking D1 in the query
         if (!vertex.flippedFromInitialDirection) {
             // oriented with me. SE reads < read length away are removed
             if (singleEnd) {
                 int numBasesToSkipStart = Math.max(0, walkLength - nodeOffset - MAX_READ_LENGTH);
                 return vertex.getUnflippedReadIds().getOffSetRange(numBasesToSkipStart, ReadHeadInfo.MAX_OFFSET_VALUE);
             } else {
                 int numBasesToSkipStart = Math.max(0, walkLength - nodeOffset - MAX_OUTER_DISTANCE);
                 int numBasesToSkipEnd = walkLength - nodeOffset - MIN_OUTER_DISTANCE + MAX_READ_LENGTH;
                 return vertex.getUnflippedReadIds().getOffSetRange(numBasesToSkipStart, numBasesToSkipEnd);
             }
         } else {
             // oriented in the opposite way. SE reads > read length are removed
             // ----------->  <------------  --------------> *
             //                     A1------------------------->
             //                B1------------------------>
             // here, A1 applies and B1 does not but B1's offset is close to the END of the containing node 
             int myLength = vertex.getKmerLength() - Kmer.getKmerLength() + 1;
             if (singleEnd) {
                 int numBasesToSkipStart = Math.max(0, walkLength - nodeOffset - MAX_READ_LENGTH);
                return vertex.getFlippedReadIds().getOffSetRange(Integer.MIN_VALUE, myLength - numBasesToSkipStart);
             } else {
                int numBasesToSkipStart = walkLength - nodeOffset - MAX_OUTER_DISTANCE;
                 int numBasesToSkipEnd = walkLength - nodeOffset - MIN_OUTER_DISTANCE + MAX_READ_LENGTH;
                 return vertex.getFlippedReadIds().getOffSetRange(numBasesToSkipEnd, myLength - numBasesToSkipStart);
             }
         }
     }
 
     // local variables for compareScoresAndPrune
     private RayScores singleEndScores = new RayScores();
     private RayScores pairedEndScores = new RayScores();
     private VKmerList walkIds = new VKmerList();
     private ArrayList<Integer> walkOffsets = new ArrayList<>();
 
     /**
      * I'm the frontier node and am now receiving the total scores from each node in the walk.
      * Aggregate all the scores from all the nodes by id, then determine if one of the candidates
      * dominates all the others. If so, send a prune msg to all the others and a continue msg to the dominator.
      * the walk stops under two conditions: 1) no node dominates all others or 2) this node is marked
      * STOP (my neighbor intersected another walk)
      * in the special case where the selected edge is myself (I am one of my neighbors-- a tandem repeat),
      * no edges will be pruned since if the repeat is favored, I would end up going the opposite direction and
      * possibly pruning the walk I just came from!
      * // TODO it seems that tandem repeats are prunable but if they dominate, the walk should stop here completely.
      * // Need to think about this a bit more.
      */
     private void compareScoresAndPrune(ArrayList<RayMessage> msgs) {
         VKmer id = getVertexId();
         RayValue vertex = getVertexValue();
 
         // aggregate scores and walk info from all msgs
         singleEndScores.clear();
         pairedEndScores.clear();
         walkIds.clear();
         walkOffsets.clear();
         int walkLength = msgs.get(0).getWalkLength();
         for (RayMessage msg : msgs) {
             if (walkLength != msg.getWalkLength()) {
                 throw new IllegalStateException("One of the messages didn't agree about the walk length! Expected "
                         + walkLength + " but saw " + msg.getWalkLength());
             }
             singleEndScores.addAll(msg.getSingleEndScores());
             pairedEndScores.addAll(msg.getPairedEndScores());
             walkIds.append(msg.getWalkIds().getPosition(0));
             walkOffsets.add(msg.getWalkOffsets().get(0));
         }
 
         VKmer dominantKmer = null;
         EDGETYPE dominantEdgeType = null;
         // need to compare each edge in this dir with every other one.  Unfortunately, this is ugly to do, requiring 
         // us to iterate over edge types, then edges in those types, and keeping track of the indexes ourselves, etc :(
         // this 4 loops are really just two for loops that are tracking 1) the index, 2) the edge type, and 3) the kmer
         int queryIndex = 0, targetIndex = 0;
         boolean equalPairedEdgeFound = false;
         boolean equalSingleEdgeFound = false;
         boolean dominantEdgeFound = false;
        EDGETYPE[] searchETs = vertex.flippedFromInitialDirection ? INITIAL_DIRECTION.mirror().edgeTypes()
                : INITIAL_DIRECTION.edgeTypes();
         for (EDGETYPE queryET : searchETs) {
             for (VKmer queryKmer : vertex.getEdges(queryET)) {
                 queryIndex++;
                 targetIndex = 0;
                 equalPairedEdgeFound = false;
                 equalSingleEdgeFound = false;
                 for (EDGETYPE targetET : searchETs) {
                     for (VKmer targetKmer : vertex.getEdges(targetET)) {
                         targetIndex++;
                         if (queryIndex == targetIndex) {
                             // don't compare vs self
                             continue;
                         }
                         float coverage = vertex.getAverageCoverage();
                         if (!pairedEndScores.dominates(queryET, queryKmer, targetET, targetKmer, coverage)) {
                             equalPairedEdgeFound = true;
                         }
                         if (!singleEndScores.dominates(queryET, queryKmer, targetET, targetKmer, coverage)) {
                             equalSingleEdgeFound = true;
                         }
                     }
                     if (equalPairedEdgeFound && equalSingleEdgeFound) {
                         break;
                     }
                 }
                 if (!equalPairedEdgeFound) {
                     // this edge dominated all other edges.  Keep it as the winner
                     dominantKmer = queryKmer;
                     dominantEdgeType = queryET;
                     dominantEdgeFound = true;
                     break;
                 } else if (!equalSingleEdgeFound) {
                     dominantKmer = queryKmer;
                     dominantEdgeType = queryET;
                     dominantEdgeFound = true;
                     break;
                 }
             }
             if (dominantEdgeFound) {
                 break;
             }
         }
 
         if (dominantEdgeFound) {
             // if a dominant edge is found, all the others must be removed.
             for (EDGETYPE et : searchETs) {
                 for (VKmer kmer : vertex.getEdges(et)) {
                     if (et != dominantEdgeType || !kmer.equals(dominantKmer)) {
                         outgoingMsg.reset();
                         outgoingMsg.setMessageType(RayMessageType.PRUNE_EDGE);
                         outgoingMsg.setEdgeTypeBackToFrontier(et.mirror());
                         outgoingMsg.setSourceVertexId(id);
                         sendMsg(kmer, outgoingMsg);
                         vertex.getEdges(et).remove(kmer);
                     }
                 }
             }
             // the walk is then passed on to the single remaining node
             outgoingMsg.reset();
             outgoingMsg.setMessageType(RayMessageType.CONTINUE_WALK);
             outgoingMsg.setEdgeTypeBackToFrontier(dominantEdgeType.mirror());
             outgoingMsg.setWalkIds(walkIds);
             outgoingMsg.setWalkOffsets(walkOffsets);
             outgoingMsg.setWalkLength(walkLength);
             outgoingMsg.setFrontierFlipped(vertex.flippedFromInitialDirection); // TODO make sure this is correct
             sendMsg(dominantKmer, outgoingMsg);
         }
     }
 
     public static PregelixJob getConfiguredJob(
             GenomixJobConf conf,
             Class<? extends DeBruijnGraphCleanVertex<? extends VertexValueWritable, ? extends MessageWritable>> vertexClass)
             throws IOException {
         PregelixJob job = DeBruijnGraphCleanVertex.getConfiguredJob(conf, vertexClass);
         job.setVertexInputFormatClass(NodeToRayVertexInputFormat.class);
         job.setVertexOutputFormatClass(RayVertexToNodeOutputFormat.class);
         return job;
     }
 
 }
