 package joshua.lattice;
 
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.logging.Logger;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 import joshua.corpus.Vocabulary;
 
 /**
  * A lattice representation of a directed graph.
  * 
  * @author Lane Schwartz
  * @author Matt Post <post@cs.jhu.edu>
  * @since 2008-07-08
  * 
  * @param Label Type of label associated with an arc.
  */
 public class Lattice<Value> implements Iterable<Node<Value>> {
 
   /**
    * True if there is more than one path through the lattice.
    */
   private final boolean latticeHasAmbiguity;
 
   /**
    * Costs of the best path between each pair of nodes in the lattice.
    */
   private final float[][] costs;
   
   /**
    * List of all nodes in the lattice. Nodes are assumed to be in topological order.
    */
   private final List<Node<Value>> nodes;
 
   /**
    * Records the shortest distance through the lattice.
    */
   private int shortestDistance = Integer.MAX_VALUE;
 
   /** Logger for this class. */
   private static final Logger logger = Logger.getLogger(Lattice.class.getName());
 
   /**
    * Constructs a new lattice from an existing list of (connected) nodes.
    * <p>
    * The list of nodes must already be in topological order. If the list is not in topological
    * order, the behavior of the lattice is not defined.
    * 
    * @param nodes A list of nodes which must be in topological order.
    */
   public Lattice(List<Node<Value>> nodes) {
     this.nodes = nodes;
     this.costs = calculateAllPairsShortestPath(nodes);
     this.latticeHasAmbiguity = true;
   }
 
   public Lattice(List<Node<Value>> nodes, boolean isAmbiguous) {
     // Node<Value> sink = new Node<Value>(nodes.size());
     // nodes.add(sink);
     this.nodes = nodes;
     this.costs = calculateAllPairsShortestPath(nodes);
     this.latticeHasAmbiguity = isAmbiguous;
   }
 
   public Lattice(Value[] linearChain) {
     this.latticeHasAmbiguity = false;
     this.nodes = new ArrayList<Node<Value>>();
 
     Node<Value> previous = new Node<Value>(0);
     nodes.add(previous);
 
     int i = 1;
 
     for (Value value : linearChain) {
 
       Node<Value> current = new Node<Value>(i);
       float cost = 0.0f;
       // if (i > 4) cost = (float)i/1.53432f;
       previous.addArc(current, cost, value);
 
       nodes.add(current);
 
       previous = current;
       i++;
     }
 
     this.costs = calculateAllPairsShortestPath(nodes);
   }
 
   public final boolean hasMoreThanOnePath() {
     return latticeHasAmbiguity;
   }
 
   /**
    * Computes the shortest distance between two nodes, which is used (perhaps among other
    * places) in computing which rules can apply over which spans of the input
    * 
    * @param tail
    * @param head
    * @return
    */
  public int distance(Arc<Value> arc) {
     return (int) this.getShortestPath(arc.getTail().getNumber(), arc.getHead().getNumber());
   }
   
   public int distance(int i, int j) {
     return (int) this.getShortestPath(i, j);
   }
     
 
   /**
    * Convenience method to get a lattice from an int[].
    * 
    * This method is useful because Java's generics won't allow a primitive array to be passed as a
    * generic array.
    * 
    * @param linearChain
    * @return Lattice representation of the linear chain.
    */
   public static Lattice<Integer> createIntLattice(int[] linearChain) {
     Integer[] integerSentence = new Integer[linearChain.length];
     for (int i = 0; i < linearChain.length; i++) {
       integerSentence[i] = linearChain[i];
     }
 
     return new Lattice<Integer>(integerSentence);
   }
 
   public static Lattice<Integer> createIntLatticeFromString(String data) {
     Map<Integer, Node<Integer>> nodes = new HashMap<Integer, Node<Integer>>();
 
     // this matches a sequence of tuples, which describe arcs
     // leaving this node
     Pattern nodePattern = Pattern.compile("(.+?)\\(\\s*(\\(.+?\\),\\s*)\\s*\\)(.*)");
 
     // this matches a comma-delimited, parenthesized tuple of a
     // (a) single-quoted word (b) a number (c) an offset (how many
     // states to jump ahead)
     Pattern arcPattern = Pattern
         .compile("\\s*\\('(.+?)',\\s*(-?\\d+\\.?\\d*?),\\s*(\\d+)\\),\\s*(.*)");
 
     Matcher nodeMatcher = nodePattern.matcher(data);
 
     boolean latticeIsAmbiguous = false;
 
     int nodeID = 0;
     Node<Integer> startNode = new Node<Integer>(nodeID);
     nodes.put(nodeID, startNode);
 
     while (nodeMatcher.matches()) {
 
       String nodeData = nodeMatcher.group(2);
       String remainingData = nodeMatcher.group(3);
 
       nodeID++;
 
       Node<Integer> currentNode = null;
       if (nodes.containsKey(nodeID)) {
         currentNode = nodes.get(nodeID);
       } else {
         currentNode = new Node<Integer>(nodeID);
         nodes.put(nodeID, currentNode);
       }
 
       logger.fine("Node " + nodeID + ":");
 
       Matcher arcMatcher = arcPattern.matcher(nodeData);
       int numArcs = 0;
       if (!arcMatcher.matches()) {
         throw new RuntimeException("Parse error!");
       }
       while (arcMatcher.matches()) {
         numArcs++;
         String arcLabel = arcMatcher.group(1);
         float arcWeight = Float.valueOf(arcMatcher.group(2));
         int destinationNodeID = nodeID + Integer.valueOf(arcMatcher.group(3));
 
         Node<Integer> destinationNode;
         if (nodes.containsKey(destinationNodeID)) {
           destinationNode = nodes.get(destinationNodeID);
         } else {
           destinationNode = new Node<Integer>(destinationNodeID);
           nodes.put(destinationNodeID, destinationNode);
         }
 
         String remainingArcs = arcMatcher.group(4);
 
         logger.fine("\t" + arcLabel + " " + arcWeight + " " + destinationNodeID);
         Integer intArcLabel = Vocabulary.id(arcLabel);
         currentNode.addArc(destinationNode, arcWeight, intArcLabel);
 
         arcMatcher = arcPattern.matcher(remainingArcs);
       }
       if (numArcs > 1)
         latticeIsAmbiguous = true;
 
       nodeMatcher = nodePattern.matcher(remainingData);
     }
 
     /* Add <s> to the start of the lattice. */
     if (nodes.containsKey(1)) {
       Node<Integer> firstNode = nodes.get(1);
       startNode.addArc(firstNode, 0.0f, Vocabulary.id(Vocabulary.START_SYM));
     }
 
     /* Add </s> as a final state, and connect it to all end-state nodes. */
     Node<Integer> endNode = new Node<Integer>(++nodeID);
     for (Node<Integer> node : nodes.values()) {
       if (node.getOutgoingArcs().size() == 0)
         node.addArc(endNode, 0.0f, Vocabulary.id(Vocabulary.STOP_SYM));
     }
     // Add the endnode after the above loop so as to avoid a self-loop.
     nodes.put(nodeID, endNode);
 
     List<Node<Integer>> nodeList = new ArrayList<Node<Integer>>(nodes.values());
     Collections.sort(nodeList, new NodeIdentifierComparator());
 
     logger.fine(nodeList.toString());
 
     return new Lattice<Integer>(nodeList, latticeIsAmbiguous);
   }
 
   /**
    * Constructs a lattice from a given string representation.
    * 
    * @param data String representation of a lattice.
    * @return A lattice that corresponds to the given string.
    */
   public static Lattice<String> createStringLatticeFromString(String data) {
 
     Map<Integer, Node<String>> nodes = new HashMap<Integer, Node<String>>();
 
     Pattern nodePattern = Pattern.compile("(.+?)\\((\\(.+?\\),)\\)(.*)");
     Pattern arcPattern = Pattern.compile("\\('(.+?)',(\\d+.\\d+),(\\d+)\\),(.*)");
 
     Matcher nodeMatcher = nodePattern.matcher(data);
 
     int nodeID = -1;
 
     while (nodeMatcher.matches()) {
 
       String nodeData = nodeMatcher.group(2);
       String remainingData = nodeMatcher.group(3);
 
       nodeID++;
 
       Node<String> currentNode;
       if (nodes.containsKey(nodeID)) {
         currentNode = nodes.get(nodeID);
       } else {
         currentNode = new Node<String>(nodeID);
         nodes.put(nodeID, currentNode);
       }
 
       logger.fine("Node " + nodeID + ":");
 
       Matcher arcMatcher = arcPattern.matcher(nodeData);
 
       while (arcMatcher.matches()) {
         String arcLabel = arcMatcher.group(1);
         float arcWeight = Float.valueOf(arcMatcher.group(2));
         int destinationNodeID = nodeID + Integer.valueOf(arcMatcher.group(3));
 
         Node<String> destinationNode;
         if (nodes.containsKey(destinationNodeID)) {
           destinationNode = nodes.get(destinationNodeID);
         } else {
           destinationNode = new Node<String>(destinationNodeID);
           nodes.put(destinationNodeID, destinationNode);
         }
 
         String remainingArcs = arcMatcher.group(4);
 
         logger.fine("\t" + arcLabel + " " + arcWeight + " " + destinationNodeID);
 
         currentNode.addArc(destinationNode, arcWeight, arcLabel);
 
         arcMatcher = arcPattern.matcher(remainingArcs);
       }
 
       nodeMatcher = nodePattern.matcher(remainingData);
     }
 
     List<Node<String>> nodeList = new ArrayList<Node<String>>(nodes.values());
     Collections.sort(nodeList, new NodeIdentifierComparator());
 
     logger.fine(nodeList.toString());
 
     return new Lattice<String>(nodeList);
   }
 
   /**
    * Gets the cost of the shortest path between two nodes.
    * 
    * @param from ID of the starting node.
    * @param to ID of the ending node.
    * @return The cost of the shortest path between the two nodes.
    */
   public float getShortestPath(int from, int to) {
 //    System.err.println(String.format("DISTANCE(%d,%d) = %f", from, to, costs[from][to]));
     return costs[from][to];
   }
 
   /**
    * Gets the shortest distance through the lattice.
    * 
    */
   public int getShortestDistance() {
     return shortestDistance;
   }
 
   /**
    * Gets the node with a specified integer identifier.
    * 
    * @param index Integer identifier for a node.
    * @return The node with the specified integer identifier
    */
   public Node<Value> getNode(int index) {
     return nodes.get(index);
   }
 
   /**
    * Returns an iterator over the nodes in this lattice.
    * 
    * @return An iterator over the nodes in this lattice.
    */
   public Iterator<Node<Value>> iterator() {
     return nodes.iterator();
   }
 
   /**
    * Returns the number of nodes in this lattice.
    * 
    * @return The number of nodes in this lattice.
    */
   public int size() {
     return nodes.size();
   }
 
   /**
    * Calculate the all-pairs shortest path for all pairs of nodes.
    * <p>
    * Note: This method assumes no backward arcs. If there are backward arcs, the returned shortest
    * path costs for that node may not be accurate.
    * 
    * @param nodes A list of nodes which must be in topological order.
    * @return The all-pairs shortest path for all pairs of nodes.
    */
   private float[][] calculateAllPairsShortestPath(List<Node<Value>> nodes) {
 
     int size = nodes.size();
     float[][] costs = new float[size][size];
 
     // Initialize pairwise costs. Costs from a node to itself are 0, and are infinite between
     // different nodes.
     for (int from = 0; from < size; from++) {
       for (int to = 0; to < size; to++) {
         costs[from][to] = (from == to) ? 0.0f : Float.POSITIVE_INFINITY;
       }
     }
 
     // Loop over all pairs of immediate neighbors and
     // record the actual costs.
     for (Node<Value> tail : nodes) {
       for (Arc<Value> arc : tail.getOutgoingArcs()) {
         Node<Value> head = arc.getHead();
 
         int from = tail.id();
         int to = head.id();
         // this is slightly different
         // than it was defined in Dyer et al 2008
         float cost = arc.getCost();
         // minimally, cost should be weighted by
         // the feature weight assigned, so we just
         // set this to 1.0 for now
         cost = 1.0f;
 
         if (cost < costs[from][to]) {
           costs[from][to] = cost;
         }
       }
     }
 
     // Loop over every possible starting node (the last node is assumed to not be a starting node)
     for (int i = 0; i < size - 2; i++) {
 
       // Loop over every possible ending node, starting two nodes past the starting node (this
       // assumes no backward arcs)
       for (int j = i + 2; j < size; j++) {
 
         // Loop over every possible middle node, starting one node past the starting node (this
         // assumes no backward arcs)
         for (int k = i + 1; k < j; k++) {
 
           // The best cost is the minimum of the previously recorded cost and the sum of costs in
           // the currently considered path
           costs[i][j] = Math.min(costs[i][j], costs[i][k] + costs[k][j]);
 
           if (i == 0 && j == size - 1 && costs[i][j] < shortestDistance)
             shortestDistance = (int)costs[i][j];
         }
       }
     }
 
     return costs;
   }
 
   @Override
   public String toString() {
     StringBuilder s = new StringBuilder();
 
     for (Node<Value> start : this) {
       for (Arc<Value> arc : start.getOutgoingArcs()) {
         s.append(arc.toString());
         s.append('\n');
       }
     }
 
     return s.toString();
   }
 
   public static void main(String[] args) {
 
     List<Node<String>> nodes = new ArrayList<Node<String>>();
     for (int i = 0; i < 4; i++) {
       nodes.add(new Node<String>(i));
     }
 
     nodes.get(0).addArc(nodes.get(1), 1.0f, "x");
     nodes.get(1).addArc(nodes.get(2), 1.0f, "y");
     nodes.get(0).addArc(nodes.get(2), 1.5f, "a");
     nodes.get(2).addArc(nodes.get(3), 3.0f, "b");
     nodes.get(2).addArc(nodes.get(3), 5.0f, "c");
 
     Lattice<String> graph = new Lattice<String>(nodes);
 
     System.out.println("Shortest path from 0 to 3: " + graph.getShortestPath(0, 3));
   }
 }
