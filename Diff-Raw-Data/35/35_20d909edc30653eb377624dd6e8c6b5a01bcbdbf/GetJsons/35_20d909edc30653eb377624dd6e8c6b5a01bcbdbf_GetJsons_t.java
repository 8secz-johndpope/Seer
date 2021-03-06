 package opentree.plugins;
 
 import jade.tree.JadeTree;
 import jade.tree.TreePrinter;
 
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.LinkedList;
 
 import opentree.GraphDatabaseAgent;
 import opentree.Taxon;
 import opentree.TaxonSet;
 import opentree.Taxonomy;
 import opentree.tnrs.TNRSMatch;
 import opentree.tnrs.TNRSNameResult;
 import opentree.tnrs.TNRSQuery;
 
 import org.neo4j.graphdb.GraphDatabaseService;
 import org.neo4j.graphdb.Node;
 import org.neo4j.graphdb.Relationship;
 import org.neo4j.server.plugins.Description;
 import org.neo4j.server.plugins.Parameter;
 import org.neo4j.server.plugins.PluginTarget;
 import org.neo4j.server.plugins.ServerPlugin;
 import org.neo4j.server.plugins.Source;
 import org.neo4j.server.rest.repr.OpentreeRepresentationConverter;
 import org.neo4j.server.rest.repr.Representation;
 import org.z3950.zing.cql.CQLBooleanNode;
 import org.z3950.zing.cql.CQLNode;
 import org.z3950.zing.cql.CQLParseException;
 import org.z3950.zing.cql.CQLParser;
 import org.z3950.zing.cql.CQLTermNode;
 
 public class GetJsons extends ServerPlugin {
 
     @Description("Return a JSON with alternative TAXONOMIC relationships noted and returned")
     @PluginTarget(Node.class)
     public String getConflictTaxJsonAltRel(@Source Node target,
             @Parameter(name = "domsource", optional = true) @Description("The dominant source.") String domsource,
             @Parameter(name = "altrels", optional = true) @Description("The list of alternative relationships to prefer.") Long[] altrels,
             @Parameter(name = "nubrel", optional = true) @Description("A new relationship nub.") Long nubRelId) {
 
         String retst = "";
         Taxon taxon;
         Taxonomy t = new Taxonomy(new GraphDatabaseAgent(target.getGraphDatabase()));
 
         if (nubRelId != null) {
 
             Relationship rel = target.getGraphDatabase().getRelationshipById(nubRelId);
             ArrayList<Long> rels = new ArrayList<Long>();
             if (altrels != null)
                 for (int i = 0; i < altrels.length; i++) {
                     rels.add(altrels[i]);
                 }
             taxon = t.getTaxon(rel.getEndNode());
             retst = taxon.constructJSONAltRels((String) rel.getProperty("source"), rels);
 
         } else {
 
             taxon = t.getTaxon(target);
             ArrayList<Long> rels = new ArrayList<Long>();
             if (altrels != null)
                 for (int i = 0; i < altrels.length; i++) {
                     rels.add(altrels[i]);
                 }
             retst = taxon.constructJSONAltRels(domsource, rels);
         }
 
         return retst;
 
     }
 
     @Description("Return a taxonomy subtree for a set of taxon names")
     @PluginTarget(GraphDatabaseService.class)
     public Representation subtree(@Source GraphDatabaseService graphDb,
             @Parameter(name = "query", optional = true) @Description("A CQL query string") String query) {
 
         /*
          * It would be good to turn this into an unmanaged extension so we can use phylows urls /phylows/tree/subtree
          */
 
         Taxonomy taxonomy = new Taxonomy(new GraphDatabaseAgent(graphDb));
 
         // parse the cql
         CQLParser cp = new CQLParser();
         CQLNode queryNode = null;
         try {
             queryNode = cp.parse(query);
         } catch (CQLParseException e) {
             // TODO Auto-generated catch block
             e.printStackTrace();
         } catch (IOException e) {
             // TODO Auto-generated catch block
             e.printStackTrace();
         }
 
         // extract the names of the taxa from the CQL
         // currently, for simplicity, we are using taxon names, but we should be using taxon UIDS.
         String taxaForSubtree = "";
         if (queryNode instanceof CQLTermNode) {
             if ((((CQLTermNode) queryNode).getQualifier()).equals("pt.taxaForSubtree")) {
                 taxaForSubtree = ((CQLTermNode) queryNode).getTerm();
             }
         }
         
         String[] inputNames = taxaForSubtree.split(",");
         LinkedList<Node> tNodes = new LinkedList<Node>();
 
         // get the nodes for the names
        if (inputNames.length > 0) {
            for (String taxName : inputNames) {
                for (Node n : taxonomy.ALLTAXA.findTaxNodesByName(taxName.trim())) {
                    tNodes.add(n);
                }
             }
 
            // create a taxon set for the found nodes and get the subtree
            TaxonSet taxa = new TaxonSet(tNodes, taxonomy);
            JadeTree subTree = taxa.getPrefTaxSubtree();
    
            // return a newick tree
            TreePrinter tp = new TreePrinter();
            return OpentreeRepresentationConverter.convert(tp.printNH(subTree));
        
        } else {
            return OpentreeRepresentationConverter.convert("");
        }
 
     }
 
     /**
      * Recursively traverses a CQL parse tree (presumably starting from its root node). Currently not used, but here
     * for reference; we will presumably want to support more complex queries at some point that will require recursions.
      * 
      * @param node
      * @return
      */
     protected CQLNode parseCQL(CQLNode node, String taxaForSubtree) {
         if (node instanceof CQLBooleanNode) {
             ((CQLBooleanNode) node).left = parseCQL(((CQLBooleanNode) node).left, taxaForSubtree);
             ((CQLBooleanNode) node).right = parseCQL(((CQLBooleanNode) node).right, taxaForSubtree);
             return node;
 
         } else if (node instanceof CQLTermNode) {
             String index = ((CQLTermNode) node).getQualifier();
             String term = ((CQLTermNode) node).getTerm();
 
 /*            CQLRelation relation = ((CQLTermNode) node).getRelation();
             Map<String, String> mapping = getPredicateMapping();
 
             for (String key : mapping.keySet()) {
                 index = index.replaceAll(key, mapping.get(key));
             }
 
             return new CQLTermNode(index, relation, term); */
         }
 
 //        logger.debug(node);
         return node;
     }
 
 /*    @Deprecated
     @Description("Return a taxonomy subtree for a set of taxon names")
     @PluginTarget(GraphDatabaseService.class)
     public Representation subtreeForNames(@Source GraphDatabaseService graphDb,
             @Parameter(name = "queryString", optional = true) @Description("A comma-delimited set of taxon names") String queryString) {
 
         Taxonomy taxonomy = new Taxonomy(new GraphDatabaseAgent(graphDb));
 
         String[] inputNames = queryString.split(",");
         LinkedList<Node> tNodes = new LinkedList<Node>();
 
         // currently, for simplicity, we are using taxon names, but we should be using taxon UIDS.
         for (String taxName : inputNames) {
             System.out.println("Searching for " + taxName);
             for (Node n : taxonomy.ALLTAXA.findTaxNodesByName(taxName.trim())) {
                 tNodes.add(n);
             }
         }
 
         // create a taxon set for the found nodes and get the subtree
         TaxonSet taxa = new TaxonSet(tNodes, taxonomy);
         JadeTree subTree = taxa.getPrefTaxSubtree();
 
         TreePrinter tp = new TreePrinter();
 
         return OpentreeRepresentationConverter.convert(tp.printNH(subTree));
 
     } */
 
     /*
      * @Description("Return a JSON with alternative TAXONOMIC relationships noted and returned")
      * 
      * @PluginTarget(Node.class) public String getConflictTaxJsonAltRel(@Source Node source,
      * 
      * @Description("The dominant source.") @Parameter(name = "domsource", optional = true) String domsource,
      * 
      * @Description("The list of alternative relationships to prefer.") @Parameter(name = "altrels", optional = true) Long[] altrels,
      * 
      * @Description("A new relationship nub.") @Parameter(name = "nubrel", optional = true) Long nubrel,
      * 
      * @Description("A new relationship nub.") @Parameter(name = "nodeid", optional = true) Long nodeId) { String retst = ""; Taxonomy taxonomy = new
      * Taxonomy(new GraphDatabaseAgent(source.getGraphDatabase())); Taxon taxon;
      * 
      * if (nubrel != null) { Relationship rel = source.getGraphDatabase().getRelationshipById(nubrel); ArrayList<Long> rels = new ArrayList<Long>(); if (altrels
      * != null) for (int i = 0; i < altrels.length; i++) { rels.add(altrels[i]); } taxon = taxonomy.getTaxon(rel.getEndNode()); retst =
      * taxon.constructJSONAltRels((String) rel.getProperty("source"), rels);
      * 
      * } else { /* ArrayList<Long> rels = new ArrayList<Long>(); if (altrels != null) for (int i = 0; i < altrels.length; i++) { rels.add(altrels[i]); } taxon =
      * new Taxon(source); retst = taxon.constructJSONAltRels(domsource, rels); *
      * 
      * taxon = taxonomy.getTaxon(taxonomy.getNodeById(nodeId)); ArrayList<Long> rels = new ArrayList<Long>(); if (altrels != null) for (int i = 0; i <
      * altrels.length; i++) { rels.add(altrels[i]); } retst = taxon.constructJSONAltRels(domsource, rels);
      * 
      * } return retst; }
      */
 
     @SuppressWarnings("unchecked")
     @Description("Return a JSON with node ids for nodes matching a name")
     @PluginTarget(GraphDatabaseService.class)
     public Representation getNodeIDJSONFromName(@Source GraphDatabaseService graphDb,
             @Description("Name of node to find.") @Parameter(name = "nodename", optional = true) String nodename) {
 
         HashMap<String, Object> results = new HashMap<String, Object>();
         results.put("nodeid", new ArrayList<Long>());
 
         GraphDatabaseAgent gdb = new GraphDatabaseAgent(graphDb);
         TNRSQuery tnrs = new TNRSQuery(new Taxonomy(gdb));
 
         TNRSNameResult matches = tnrs.matchExact(nodename).iterator().next();
 
         for (TNRSMatch m : matches)
             ((ArrayList<Long>) results.get("nodeid")).add(m.getMatchedNode().getId());
 
         if (results.size() < 1)
             results.put("error", "Could not find any taxon by that name");
 
         return OpentreeRepresentationConverter.convert(results);
     }
 }
