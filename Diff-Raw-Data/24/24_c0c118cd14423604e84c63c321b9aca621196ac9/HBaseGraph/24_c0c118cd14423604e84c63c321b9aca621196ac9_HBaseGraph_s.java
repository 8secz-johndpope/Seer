 package nl.vu.jena.graph;
 
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import nl.vu.datalayer.hbase.HBaseClientSolution;
 import nl.vu.datalayer.hbase.id.Id;
 import nl.vu.datalayer.hbase.retrieve.IHBasePrefixMatchRetrieveOpsManager;
 import nl.vu.datalayer.hbase.retrieve.RowLimitPair;
 import nl.vu.jena.cache.JenaCache;
 
 import com.hp.hpl.jena.graph.Node;
 import com.hp.hpl.jena.graph.Node_Literal;
 import com.hp.hpl.jena.graph.Triple;
 import com.hp.hpl.jena.graph.TripleMatch;
 import com.hp.hpl.jena.graph.impl.GraphBase;
 import com.hp.hpl.jena.sparql.expr.Expr;
 import com.hp.hpl.jena.sparql.expr.ExprFunction2;
 import com.hp.hpl.jena.util.iterator.ExtendedIterator;
 import com.hp.hpl.jena.util.iterator.NullIterator;
 import com.hp.hpl.jena.util.iterator.WrappedIterator;
 
 public class HBaseGraph extends GraphBase {
 
 	private static final int CACHE_SIZE = 500;
 	private HBaseClientSolution hbase;
 	ExtendedIterator<Triple> it;
 	ValueIdMapper valIdMapper;
 	
 	private Map<TripleMatch, List<Triple>> cache = Collections.synchronizedMap(new JenaCache<TripleMatch, List<Triple>>(CACHE_SIZE));
 	
 	public HBaseGraph(HBaseClientSolution hbase) {
 		super();
 		this.hbase = hbase;
 		valIdMapper = new ValueIdMapper(hbase);
 	}
 
 	@Override
 	protected ExtendedIterator<Triple> graphBaseFind(TripleMatch m) {	
 		ExtendedIterator<Triple> ret;
 		List<Triple> tripleList;
 		if ((tripleList=cache.get(m))!=null){
 			return WrappedIterator.createNoRemove(tripleList.iterator());
 		}
 		
 		try {
 			Id[] quad = valIdMapper.getIdsFromTriple(m);
 
 			// retrieve results from HBase
 			ArrayList<ArrayList<Id>> results;
 
 			if (m instanceof FilteredTriple) {
 				ExprFunction2 simpleFilter = (ExprFunction2) (((FilteredTriple) m).getSimpleFilter());
 				results = getFilteredResults(quad, simpleFilter);
 			} else {
 				results = ((IHBasePrefixMatchRetrieveOpsManager) hbase.opsManager).getResults(quad);
 			}
 
 			ArrayList<Triple> convertedTriples = new ArrayList<Triple>(results.size());
 			for (ArrayList<Id> arrayList : results) {
 				Triple newTriple = new Triple(Node.createUncachedLiteral(
 						arrayList.get(0), null), Node.createUncachedLiteral(
 						arrayList.get(1), null), Node.createUncachedLiteral(
 						arrayList.get(2), null));
 
 				convertedTriples.add(newTriple);
 			}
 
 			ret = WrappedIterator.createNoRemove(convertedTriples.iterator());
 			cache.put(m, convertedTriples);
 
 		} catch (Exception e) {
 			e.printStackTrace();
 			return NullIterator.instance();
 		}
 		return ret;
 	}
 	
 	public void mapNodeIdsToMaterializedNodes(Map<Node_Literal, Node> tempIdMap) throws IOException{
 		valIdMapper.mapNodeIdsToMaterializedNodes(tempIdMap);
 	}
 	
 	public void mapMaterializedNodesToNodeIds(Map<Node, Node_Literal> node2nodeIdMap) throws IOException{
 		valIdMapper.mapMaterializedNodesToNodeIds(node2nodeIdMap);
 	}
 
 	private ArrayList<ArrayList<Id>> getFilteredResults(Id[] quad, ExprFunction2 simpleFilter)
 			throws Exception, IOException {
 		ArrayList<ArrayList<Id>> results;
 		
 		Expr arg1 = simpleFilter.getArg1();
 		Expr arg2 = simpleFilter.getArg2();
 		
 		if (arg1.isConstant() && arg1.getConstant().isNumber() ||
 				arg2.isConstant() && arg2.getConstant().isNumber()){
 			RowLimitPair limitPair = ExprToHBaseLimitsConverter.getRowLimitPair(simpleFilter);
 			results = ((IHBasePrefixMatchRetrieveOpsManager)hbase.opsManager).getResults(quad, limitPair);
 		}
 		/*TODO else if (simpleFilter instanceof E_Equals && 
 				((arg1.isConstant()&&arg1.getConstant().isIRI()) 
 				|| (arg2.isConstant()&&arg2.getConstant().isIRI()))){
 			E_Equals eq = (E_Equals) simpleFilter;
 			NodeValue constantNode = null;
 			if (eq.getArg1().isConstant()) {
 				constantNode = eq.getArg1().getConstant();
 			} else if (eq.getArg2().isConstant()) {
 				constantNode = eq.getArg2().getConstant();
 			}
 			
 			Value constValue = Convert.nodeToValue(valFactory, constantNode.asNode());
 			TODO have to convert equality Filters to Ids before getting here
 			quad[2] = 
 			results = hbase.opsManager.getResults(quad);
 		}*/
 		else 
 			throw new RuntimeException("Unsupported simple filter: "+simpleFilter.getOpName());//TODO
 		return results;
 	}
 
 	/* (non-Javadoc)
 	 * @see com.hp.hpl.jena.graph.impl.GraphBase#graphBaseFind(com.hp.hpl.jena.graph.Node, com.hp.hpl.jena.graph.Node, com.hp.hpl.jena.graph.Node)
 	 */
 	@Override
 	protected ExtendedIterator<Triple> graphBaseFind(Node s, Node p, Node o) {
 		//this function also does the materialization of Ids because it's called directly from the model
 		
 		Map<Node_Literal, Node> toResolveIdMap = new HashMap<Node_Literal, Node>();
 		
 		ExtendedIterator<Triple> idTripleIterator = graphBaseFind(Triple.createMatch(s, p, o));
 		List<Triple> tripleList = idTripleIterator.toList();
 		
 		for (Triple t : tripleList) {
 			addNodeToMap(toResolveIdMap, t.getSubject());
 			addNodeToMap(toResolveIdMap, t.getPredicate());
 			addNodeToMap(toResolveIdMap, t.getObject());
 		}
 		
 		try {
 			mapNodeIdsToMaterializedNodes(toResolveIdMap);
 		} catch (IOException e) {
 			e.printStackTrace();
 			return NullIterator.instance();
 		}
 		
 		ArrayList<Triple> materializedTriples = new ArrayList<Triple>();
 		for (Triple t : tripleList) {
 			Triple materializedTriple = new Triple(toResolveIdMap.get(t.getSubject()),
 									toResolveIdMap.get(t.getPredicate()),
 									toResolveIdMap.get(t.getObject()));
 			materializedTriples.add(materializedTriple);
 		}
 		
 		return WrappedIterator.createNoRemove(materializedTriples.iterator());
 	}
 
 
 	private void addNodeToMap(Map<Node_Literal, Node> toResolveIdMap, Node node) {
 		if (!toResolveIdMap.containsKey(node)){
 			toResolveIdMap.put((Node_Literal)node, null);
 		}
 	}
 
 }
