 package uk.ac.ebi.ep.mm.app;
 
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileNotFoundException;
 import java.io.IOException;
 import java.io.InputStream;
 import java.sql.Connection;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Stack;
 
 import org.apache.commons.cli.CommandLine;
 import org.apache.log4j.Logger;
 import org.xml.sax.Attributes;
 import org.xml.sax.InputSource;
 import org.xml.sax.SAXException;
 import org.xml.sax.XMLReader;
 import org.xml.sax.helpers.DefaultHandler;
 import org.xml.sax.helpers.XMLReaderFactory;
 
 import uk.ac.ebi.biobabel.util.db.OracleDatabaseInstance;
 import uk.ac.ebi.ebisearchservice.ArrayOfString;
 import uk.ac.ebi.ebisearchservice.EBISearchService;
 import uk.ac.ebi.ebisearchservice.EBISearchService_Service;
 import uk.ac.ebi.ep.mm.Entry;
 import uk.ac.ebi.ep.mm.MegaJdbcMapper;
 import uk.ac.ebi.ep.mm.MegaLuceneMapper;
 import uk.ac.ebi.ep.mm.MegaMapper;
 import uk.ac.ebi.ep.mm.MmDatabase;
 import uk.ac.ebi.ep.mm.Relationship;
 import uk.ac.ebi.ep.mm.XRef;
 
 /**
  * UniProt XML parser which takes into account only primary accessions,
  * entry names (IDs), organisms, EC numbers and PDB codes, and indexes/stores
  * them in a mega-map.
  * <br>
  * Only enzymes - i.e. entries with an EC number assigned - are considered.
  * @author rafa
  *
  */
 public class UniprotSaxParser extends DefaultHandler implements MmParser {
 
 	private static final String UNIPROT_ENTRY =
 			"//uniprot/entry";
 
 	private static final String UNIPROT_ENTRY_NAME =
 			"//uniprot/entry/name";
 
 	private static final String UNIPROT_ENTRY_ACCESSION =
 			"//uniprot/entry/accession";
 
 	private static final String UNIPROT_ENTRY_DBREFERENCE =
 			"//uniprot/entry/dbReference";
 
 	private static final String UNIPROT_ENTRY_DBREFERENCE_PROPERTY =
 			"//uniprot/entry/dbReference/property";
 
 	private static final String UNIPROT_ENTRY_ORGANISM_NAME =
 			"//uniprot/entry/organism/name";
 
 	private static final String UNIPROT_ENTRY_PROTEIN_REC_NAME =
 			"//uniprot/entry/protein/recommendedName/fullName";
 	
 	private final Logger LOGGER = Logger.getLogger(UniprotSaxParser.class);
 
 	/**
 	 * The current element (tree path) being parsed.
 	 */
 	private Stack<String> currentContext = new Stack<String>();
 	
 	/**
 	 * The text value of the current element being parsed.
 	 */
 	protected StringBuilder currentChars = new StringBuilder();
 
 	protected boolean isEntry;
 
 	protected boolean isAccession;
 
 	protected boolean isEntryName;
 
 	protected boolean isOrgSciName;
 
 	protected boolean isOrgComName;
 	
 	protected boolean isDbRef;
 	
 	protected boolean isProperty;
 	
 	protected boolean isProtRecName;
 
 	private MegaMapper mm;
 
 	protected List<String> accessions = new ArrayList<String>();
 
 	protected List<String> entryNames = new ArrayList<String>();
 
 	protected String orgSciName;
 
 	protected String orgComName;
 	
 	protected List<String> ecs = new ArrayList<String>();
 	
 	protected List<String> pdbCodes = new ArrayList<String>();
 
 	protected String protRecName;
 	
 	private EBISearchService ebeyeService =
 			new EBISearchService_Service().getEBISearchServiceHttpPort();
 
 	/**
 	 * Parses a UniProt XML file and indexes/stores the UniProt accessions,
 	 * IDs and organisms into a mega-map.
 	 * @param args see {@link CliOptionsParser#getCommandLine(String...)}
 	 * @throws Exception in case of error while parsing.
 	 */
 	public static void main(String... args) throws Exception {
         CommandLine cl = CliOptionsParser.getCommandLine(args);
         if (cl != null){
     		MmParser parser = new UniprotSaxParser();
     		MegaMapper mm = null;
     		Connection con = null;
     		try {
         		if (cl.hasOption("indexDir")){
     				mm = new MegaLuceneMapper(cl.getOptionValue("indexDir"));
         		} else {
             		final String dbConfig = cl.getOptionValue("dbConfig");
 //        			writer = new MegaDbMapper(dbConfig, 1000);
             		con = OracleDatabaseInstance
     						.getInstance(dbConfig).getConnection();
             		con.setAutoCommit(false);
     				mm = new MegaJdbcMapper(con);
         		}
         		parser.setWriter(mm);
         		parser.parse(cl.getOptionValue("xmlFile"));
         		mm.commit();
     		} catch (Exception e){
     			mm.rollback();
     		} finally {
         		mm.closeMap();
         		if (con != null) con.close();
     		}
     		
         }
 	}
 	
 	public void setWriter(MegaMapper mmWriter){
 		this.mm = mmWriter;
 	}
 	
 	/**
 	 * Parses a UniProt XML file and indexes/stores the UniProt accessions,
 	 * IDs and organisms into a lucene index.<br>
 	 * This method is not thread safe.
 	 * @param uniprotXml the XML file to parse
 	 * @throws FileNotFoundException if the UniProt XML file is not found
 	 * 		or not readable.
 	 * @throws SAXException if no default XMLReader can be found or
 	 * 		instantiated, or exception during parsing.
 	 * @throws IOException if the lucene index cannot be opened/created,
 	 * 		or from the parser.
 	 */
 	public void parse(String uniprotXml)
 	throws Exception {
 		if (mm == null){
 			// Don't go ahead:
 			throw new NullPointerException("A MegaMapper must be configured");
 		}
 		File uniprotXmlFile = new File(uniprotXml);
 		LOGGER.info("Mega-map open to import UniProt entries");
 		try {
         	mm.openMap();
             XMLReader xr = XMLReaderFactory.createXMLReader();
             xr.setContentHandler(this);
             xr.setErrorHandler(this);
             InputStream is = new FileInputStream(uniprotXmlFile);
             InputSource source = new InputSource(is);
             LOGGER.info("Parsing start");
             xr.parse(source);
             LOGGER.info("Parsing end");
             mm.closeMap();
             LOGGER.info("Map closed");
         } catch (Exception e){
             LOGGER.error("During parsing", e);
             mm.handleError();
             throw e;
         }
 	}
 
 	@Override
 	public void startDocument() throws SAXException {
 		super.startDocument();
 	}
 
 	@Override
 	public void endDocument() throws SAXException {
 		super.endDocument();
 	}
 
 	@Override
 	public void startElement(String uri, String localName, String qName,
 			Attributes attributes) throws SAXException {
 		currentContext.push(localName);
 		// Update flags:
 		String currentXpath = getCurrentXpath();
 		isEntry = UNIPROT_ENTRY.equals(currentXpath);
 		isAccession = UNIPROT_ENTRY_ACCESSION.equals(currentXpath);
 		isEntryName = UNIPROT_ENTRY_NAME.equals(currentXpath);
 		final String typeAttr = attributes == null?
 				null : attributes.getValue("", "type");
 		final String valueAttr = attributes == null?
 				null : attributes.getValue("", "value");
 		isOrgSciName = UNIPROT_ENTRY_ORGANISM_NAME.equals(currentXpath)
 				&& "scientific".equals(typeAttr);
 		isOrgComName = UNIPROT_ENTRY_ORGANISM_NAME.equals(currentXpath)
 				&& "common".equals(typeAttr);
 		isDbRef = UNIPROT_ENTRY_DBREFERENCE.equals(currentXpath);
 		isProperty = UNIPROT_ENTRY_DBREFERENCE_PROPERTY.equals(currentXpath);
 		isProtRecName = UNIPROT_ENTRY_PROTEIN_REC_NAME.equals(currentXpath);
 		// Clear placeholder:
 		if (currentChars.length() > 0){
 			currentChars.delete(0, currentChars.length());
 		}
 		if (isDbRef){
 			if ("EC".equalsIgnoreCase(typeAttr)){
 				ecs.add(attributes.getValue("", "id"));
 			} else if ("PDB".equals(typeAttr)){
 				pdbCodes.add(attributes.getValue("", "id"));
 			}
 		} else if (isProperty){
 			if ("method".equalsIgnoreCase(typeAttr)
 					&& "Model".equalsIgnoreCase(valueAttr)){
 				// Ignore xrefs to PDB theoretical models (which are deprecated)
 				// Remove the last added xref to PDB:
 				String model = pdbCodes.remove(pdbCodes.size() - 1);
 				LOGGER.warn("Ignoring PDB theoretical model " + model);
 			}
 		}
 	}
 
 	@Override
 	public void characters(char[] ch, int start, int length)
 			throws SAXException {
 		// Check whether we need to do something:
 		if (isAccession || isEntryName || isOrgSciName || isOrgComName || isProtRecName){
 			currentChars.append(Arrays.copyOfRange(ch, start, start+length));
 		}
 	}
 
 	/**
 	 * Stores interesting data into the index.
 	 */
 	@Override
 	public void endElement(String uri, String localName, String qName)
 	throws SAXException {
 		if (isAccession){ // take only the primary one!
 			accessions.add(currentChars.toString());
 		} else if (isEntryName){
 			entryNames.add(currentChars.toString());
 		} else if (isOrgSciName){
 			orgSciName = currentChars.toString();
 		} else if (isOrgComName){
 			orgComName = currentChars.toString();
 		} else if (isProtRecName){
 			protRecName = currentChars.toString();
 		} else if (isEntry){
 			if (!ecs.isEmpty()){ // XXX here is the enzyme filter
 				try {
 					Collection<Entry> entries = new HashSet<Entry>();
 					Collection<XRef> xrefs = new HashSet<XRef>();
 
 					Entry uniprotEntry = new Entry();
 					uniprotEntry.setDbName(MmDatabase.UniProt.name());
 					uniprotEntry.setEntryAccessions(accessions);
 					uniprotEntry.setEntryId(entryNames.get(0)); // take first one
 					uniprotEntry.setEntryName(protRecName);
 					entries.add(uniprotEntry);
 					
 					Entry speciesEntry = new Entry();
 					speciesEntry.setDbName(MmDatabase.Linnean.name());
 					speciesEntry.setEntryId(orgSciName);
 					speciesEntry.setEntryName(orgComName);
 					entries.add(speciesEntry);
 					
 					XRef up2sp = new XRef();
 					up2sp.setFromEntry(uniprotEntry);
 					up2sp.setRelationship(Relationship.between(
 							MmDatabase.UniProt, MmDatabase.Linnean).name());
 					up2sp.setToEntry(speciesEntry);
 					xrefs.add(up2sp);
 					
 					for (String ec: ecs){
 						Entry ecEntry = new Entry();
 						ecEntry.setDbName(MmDatabase.EC.name());
 						ecEntry.setEntryId(ec);
 						entries.add(ecEntry);
 						
 						XRef up2ec = new XRef();
 						up2ec.setFromEntry(uniprotEntry);
 						up2ec.setRelationship(Relationship.between(
 								MmDatabase.UniProt, MmDatabase.EC).name());
 						up2ec.setToEntry(ecEntry);
 						xrefs.add(up2ec);
 					}
 					
 					for (String pdbCode : pdbCodes) {
 						Entry pdbEntry = new Entry();
 						pdbEntry.setDbName(MmDatabase.PDB.name());
 						pdbEntry.setEntryId(pdbCode);
 						// Add structure name:
 						try {
 							ArrayOfString fields = new ArrayOfString();
 							fields.getString().add("name");
 							String name = ebeyeService
 									.getEntry("pdbe", pdbCode, fields)
 									.getString().get(0);
 							pdbEntry.setEntryName(name);
							entries.add(pdbEntry);
 						} catch (Exception e){
 							LOGGER.error("Couldn't get name for " + pdbCode, e);
 						}
 						
 						XRef up2pdb = new XRef();
 						up2pdb.setFromEntry(uniprotEntry);
 						up2pdb.setRelationship(Relationship.between(
 								MmDatabase.UniProt, MmDatabase.PDB).name());
 						up2pdb.setToEntry(pdbEntry);
 						xrefs.add(up2pdb);
 					}
 					
 					mm.write(entries, xrefs);
 				} catch (Exception e) {
 					throw new RuntimeException("Adding entry to mega-map", e);
 				}
 			}
 			// Clean up:
 			accessions.clear();
 			entryNames.clear();
 			ecs.clear();
 			pdbCodes.clear();
 			orgSciName = null;
             orgComName = null;
 		}
 		currentContext.pop();
 		// Update flags:
 		String currentXpath = getCurrentXpath();
 		isEntry = UNIPROT_ENTRY.equals(currentXpath);
 		isAccession = false;
 		isEntryName = false;
 		isOrgSciName = false;
 	}
 
 	protected String getCurrentXpath() {
 		StringBuilder xpath = new StringBuilder("/");
 		for (String string : currentContext) {
 			xpath.append('/').append(string);
 		}
 		return xpath.toString();
 	}
 
 }
