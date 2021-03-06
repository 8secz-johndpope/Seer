 package eu.europeana.uim.enrichment;
 
 import java.io.IOException;
 import java.io.StringReader;
 import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Method;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.util.ArrayList;
 import java.util.Date;
 import java.util.List;
 import java.util.Map.Entry;
 import java.util.concurrent.CopyOnWriteArrayList;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 import java.util.regex.Pattern;
 
 import org.apache.commons.lang.StringUtils;
 import org.apache.solr.client.solrj.SolrServerException;
 import org.apache.solr.client.solrj.impl.HttpSolrServer;
 import org.apache.solr.client.solrj.util.ClientUtils;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.common.SolrInputDocument;
 import org.jibx.runtime.BindingDirectory;
 import org.jibx.runtime.IBindingFactory;
 import org.jibx.runtime.IUnmarshallingContext;
 import org.jibx.runtime.JiBXException;
 import org.theeuropeanlibrary.model.common.qualifier.Status;
 
 import com.google.code.morphia.Morphia;
 import com.google.code.morphia.query.Query;
 import com.google.code.morphia.query.UpdateOperations;
 import com.mongodb.BasicDBObject;
 import com.mongodb.DBCollection;
 import com.mongodb.DBObject;
 import com.mongodb.Mongo;
 import com.mongodb.WriteConcern;
 
 import eu.annocultor.converters.europeana.Entity;
 import eu.annocultor.converters.europeana.Field;
 import eu.annocultor.converters.europeana.RecordCompletenessRanking;
 import eu.europeana.corelib.definitions.jibx.AgentType;
 import eu.europeana.corelib.definitions.jibx.AggregatedCHO;
 import eu.europeana.corelib.definitions.jibx.Aggregation;
 import eu.europeana.corelib.definitions.jibx.Alt;
 import eu.europeana.corelib.definitions.jibx.Concept;
 import eu.europeana.corelib.definitions.jibx.Country;
 import eu.europeana.corelib.definitions.jibx.CountryCodes;
 import eu.europeana.corelib.definitions.jibx.Creator;
 import eu.europeana.corelib.definitions.jibx.EuropeanaAggregationType;
 import eu.europeana.corelib.definitions.jibx.EuropeanaProxy;
 import eu.europeana.corelib.definitions.jibx.HasView;
 import eu.europeana.corelib.definitions.jibx.LandingPage;
 import eu.europeana.corelib.definitions.jibx.Language1;
 import eu.europeana.corelib.definitions.jibx.LanguageCodes;
 import eu.europeana.corelib.definitions.jibx.Lat;
 import eu.europeana.corelib.definitions.jibx.LiteralType;
 import eu.europeana.corelib.definitions.jibx.PlaceType;
 import eu.europeana.corelib.definitions.jibx.ProvidedCHOType;
 import eu.europeana.corelib.definitions.jibx.ProxyFor;
 import eu.europeana.corelib.definitions.jibx.ProxyIn;
 import eu.europeana.corelib.definitions.jibx.ProxyType;
 import eu.europeana.corelib.definitions.jibx.RDF;
 import eu.europeana.corelib.definitions.jibx.ResourceOrLiteralType;
 import eu.europeana.corelib.definitions.jibx.ResourceOrLiteralType.Lang;
 import eu.europeana.corelib.definitions.jibx.ResourceOrLiteralType.Resource;
 import eu.europeana.corelib.definitions.jibx.ResourceType;
 import eu.europeana.corelib.definitions.jibx.Rights1;
 import eu.europeana.corelib.definitions.jibx.TimeSpanType;
 import eu.europeana.corelib.definitions.jibx.WebResourceType;
 import eu.europeana.corelib.definitions.jibx.Year;
 import eu.europeana.corelib.definitions.jibx._Long;
 import eu.europeana.corelib.definitions.model.EdmLabel;
 import eu.europeana.corelib.definitions.solr.DocType;
 import eu.europeana.corelib.definitions.solr.beans.FullBean;
 import eu.europeana.corelib.dereference.impl.RdfMethod;
 import eu.europeana.corelib.solr.bean.impl.FullBeanImpl;
 import eu.europeana.corelib.solr.entity.AgentImpl;
 import eu.europeana.corelib.solr.entity.ConceptImpl;
 import eu.europeana.corelib.solr.entity.PlaceImpl;
 import eu.europeana.corelib.solr.entity.TimespanImpl;
 import eu.europeana.corelib.solr.exceptions.MongoDBException;
 import eu.europeana.corelib.solr.server.EdmMongoServer;
 import eu.europeana.corelib.solr.utils.EseEdmMap;
 import eu.europeana.corelib.solr.utils.MongoConstructor;
 import eu.europeana.corelib.solr.utils.SolrConstructor;
 import eu.europeana.corelib.tools.lookuptable.EuropeanaId;
 import eu.europeana.corelib.tools.lookuptable.EuropeanaIdMongoServer;
 import eu.europeana.corelib.tools.utils.HashUtils;
 import eu.europeana.corelib.tools.utils.PreSipCreatorUtils;
 import eu.europeana.corelib.tools.utils.SipCreatorUtils;
 import eu.europeana.uim.common.BlockingInitializer;
 import eu.europeana.uim.common.TKey;
 import eu.europeana.uim.enrichment.enums.OriginalField;
 import eu.europeana.uim.enrichment.service.EnrichmentService;
 import eu.europeana.uim.enrichment.utils.EuropeanaDateUtils;
 import eu.europeana.uim.enrichment.utils.EuropeanaEnrichmentTagger;
 import eu.europeana.uim.enrichment.utils.OsgiEdmMongoServer;
 import eu.europeana.uim.enrichment.utils.PropertyReader;
 import eu.europeana.uim.enrichment.utils.SolrList;
 import eu.europeana.uim.enrichment.utils.UimConfigurationProperty;
 import eu.europeana.uim.model.europeana.EuropeanaModelRegistry;
 import eu.europeana.uim.model.europeanaspecific.fieldvalues.ControlledVocabularyProxy;
 import eu.europeana.uim.model.europeanaspecific.fieldvalues.EuropeanaRetrievableField;
 import eu.europeana.uim.orchestration.ExecutionContext;
 import eu.europeana.uim.plugin.ingestion.AbstractIngestionPlugin;
 import eu.europeana.uim.plugin.ingestion.CorruptedDatasetException;
 import eu.europeana.uim.plugin.ingestion.IngestionPluginFailedException;
 import eu.europeana.uim.store.Collection;
 import eu.europeana.uim.store.MetaDataRecord;
 import eu.europeana.uim.sugar.SugarCrmRecord;
 import eu.europeana.uim.sugar.SugarCrmService;
 
 /**
  * Enrichment plugin implementation
  * 
  * @author Yorgos.Mamakis@ kb.nl
  * 
  */
 public class EnrichmentPlugin<I> extends
 		AbstractIngestionPlugin<MetaDataRecord<I>, I> {
 
 	private static HttpSolrServer solrServer;
 	private static HttpSolrServer migrationSolrServer;
 	private static String mongoDB;
 	private static String mongoHost = PropertyReader
 			.getProperty(UimConfigurationProperty.MONGO_HOSTURL);
 	private static String mongoPort = PropertyReader
 			.getProperty(UimConfigurationProperty.MONGO_HOSTPORT);
 	private static String solrUrl;
 	private static String solrCore;
 	private static int recordNumber;
 	private static String europeanaID = PropertyReader
 			.getProperty(UimConfigurationProperty.MONGO_DB_EUROPEANA_ID);
 	private static final int RETRIES = 10;
 	private static String repository = PropertyReader
 			.getProperty(UimConfigurationProperty.UIM_REPOSITORY);
 	private static SugarCrmService sugarCrmService;
 	private static EnrichmentService enrichmentService;
 	private static String previewsOnlyInPortal;
 	private static String collections = PropertyReader
 			.getProperty(UimConfigurationProperty.MONGO_DB_COLLECTIONS);
 	private static Morphia morphia;
 	private static SolrList solrList;
 	private static List<SolrInputDocument> migrationSolrList;
 	private final static int SUBMIT_SIZE = 10000;
 	
 	private static Mongo mongo;
 	private final static String PORTALURL = "http://www.europeana.eu/portal/record";
 	private final static String SUFFIX = ".html";
 	private static String uname;
 	private static String pass;
 	private static IBindingFactory bfact;
 	private static OsgiEdmMongoServer mongoServer;
 	private static EuropeanaEnrichmentTagger tagger;
 	public EnrichmentPlugin(String name, String description) {
 		super(name, description);
 	}
 
 	public EnrichmentPlugin() {
 		super("", "");
 	}
 
 	static {
 		try {
 			// Should be placed in a static block for performance reasons
 			bfact = BindingDirectory.getFactory(RDF.class);
 
 		} catch (JiBXException e) {
 			e.printStackTrace();
 		}
 
 	}
 	private static final Logger log = Logger.getLogger(EnrichmentPlugin.class
 			.getName());
 	/**
 	 * The parameters used by this WorkflowStart
 	 */
 	private static final List<String> params = new ArrayList<String>() {
 		private static final long serialVersionUID = 1L;
 
 	};
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see eu.europeana.uim.plugin.ingestion.IngestionPlugin#getInputFields()
 	 */
 	@Override
 	public TKey<?, ?>[] getInputFields() {
 		return null;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see
 	 * eu.europeana.uim.plugin.ingestion.IngestionPlugin#getOptionalFields()
 	 */
 	@Override
 	public TKey<?, ?>[] getOptionalFields() {
 		return null;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see eu.europeana.uim.plugin.ingestion.IngestionPlugin#getOutputFields()
 	 */
 	@Override
 	public TKey<?, ?>[] getOutputFields() {
 		return null;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see eu.europeana.uim.plugin.Plugin#initialize()
 	 */
 	@Override
 	public void initialize() {
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see eu.europeana.uim.plugin.Plugin#shutdown()
 	 */
 	@Override
 	public void shutdown() {
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see eu.europeana.uim.plugin.Plugin#getParameters()
 	 */
 	@Override
 	public List<String> getParameters() {
 		return params;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see eu.europeana.uim.plugin.Plugin#getPreferredThreadCount()
 	 */
 	@Override
 	public int getPreferredThreadCount() {
 		return 12;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see eu.europeana.uim.plugin.Plugin#getMaximumThreadCount()
 	 */
 	@Override
 	public int getMaximumThreadCount() {
 		return 15;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see eu.europeana.uim.plugin.ExecutionPlugin#initialize(eu.europeana.uim.
 	 * orchestration.ExecutionContext)
 	 */
 	@Override
 	public void initialize(ExecutionContext<MetaDataRecord<I>, I> context)
 			throws IngestionPluginFailedException {
 
 		try {
 			tagger = new EuropeanaEnrichmentTagger();
 			tagger.init("Europeana");
 			solrList = SolrList.getInstance();
 			migrationSolrList = new ArrayList<SolrInputDocument>();
 			solrServer = enrichmentService.getSolrServer();
 			migrationSolrServer = enrichmentService.getMigrationServer();
 			mongo = new Mongo(mongoHost, Integer.parseInt(mongoPort));
 			mongoDB = enrichmentService.getMongoDB();
 			uname = PropertyReader
 					.getProperty(UimConfigurationProperty.MONGO_USERNAME) != null ? PropertyReader
 					.getProperty(UimConfigurationProperty.MONGO_USERNAME) : "";
 			pass = PropertyReader
 					.getProperty(UimConfigurationProperty.MONGO_PASSWORD) != null ? PropertyReader
 					.getProperty(UimConfigurationProperty.MONGO_PASSWORD) : "";
 			
 			@SuppressWarnings("rawtypes")
 			Collection collection = (Collection) context.getExecution()
 					.getDataSet();
 			String sugarCrmId = collection
 					.getValue(ControlledVocabularyProxy.SUGARCRMID);
 			SugarCrmRecord sugarCrmRecord = sugarCrmService
 					.retrieveRecord(sugarCrmId);
 			previewsOnlyInPortal = sugarCrmRecord
 					.getItemValue(EuropeanaRetrievableField.PREVIEWS_ONLY_IN_PORTAL);
 			
 			try {
 				mongoServer = new OsgiEdmMongoServer(mongo, mongoDB, uname, pass);
 				morphia = new Morphia();
 
 				mongoServer.createDatastore(morphia);
 				clearData(mongoServer,collection.getMnemonic());
				solrServer.deleteByQuery("europeana_collectionName:"+collection.getName().split("_")[0]+"*",5000);
 			} catch(Exception e){
 				e.printStackTrace();
 			}
 			
 		} catch (Exception e) {
 			e.printStackTrace();
 		}
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see eu.europeana.uim.plugin.ExecutionPlugin#completed(eu.europeana.uim.
 	 * orchestration.ExecutionContext)
 	 */
 	@Override
 	public void completed(ExecutionContext<MetaDataRecord<I>, I> context)
 			throws IngestionPluginFailedException {
 		try {
			solrServer.add(solrList.getQueue(),10000);
 			System.out.println("Adding " + recordNumber + " documents");
 
 			// solrServer.commit();
 			System.out.println("Committed in Solr Server");
 
 			//mongoServer.close();
 		} catch (IOException e) {
 			context.getLoggingEngine().logFailed(
 					Level.SEVERE,
 					this,
 					e,
 					"Input/Output exception occured in Solr with the following message: "
 							+ e.getMessage());
 		} catch (SolrServerException e) {
 			context.getLoggingEngine().logFailed(
 					Level.SEVERE,
 					this,
 					e,
 					"Solr server exception occured in Solr with the following message: "
 							+ e.getMessage());
 		}
 
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see
 	 * eu.europeana.uim.plugin.ingestion.IngestionPlugin#process(eu.europeana
 	 * .uim.store.UimDataSet, eu.europeana.uim.orchestration.ExecutionContext)
 	 */
 	@Override
 	public boolean process(MetaDataRecord<I> mdr,
 			ExecutionContext<MetaDataRecord<I>, I> context)
 			throws IngestionPluginFailedException, CorruptedDatasetException {
 		String value = null;
 		
 			//mongoServer = new OsgiEdmMongoServer(mongo, mongoDB, uname, pass);
 		
 		if (mdr.getValues(EuropeanaModelRegistry.EDMDEREFERENCEDRECORD) != null
 				&& mdr.getValues(EuropeanaModelRegistry.EDMDEREFERENCEDRECORD)
 						.size() > 0) {
 			value = mdr.getValues(EuropeanaModelRegistry.EDMDEREFERENCEDRECORD)
 					.get(0);
 		} else {
 			value = mdr.getValues(EuropeanaModelRegistry.EDMRECORD).get(0);
 		}
 		if (mdr.getValues(EuropeanaModelRegistry.STATUS).size() == 0
 				|| !mdr.getValues(EuropeanaModelRegistry.STATUS).get(0)
 						.equals(Status.DELETED)) {
 			MongoConstructor mongoConstructor = new MongoConstructor();
 			
 			//morphia = new Morphia();
 
 			//mongoServer.createDatastore(morphia);
 			try {
 				if (solrServer.ping() == null) {
 					log.log(Level.SEVERE,
 							"Solr server "
 									+ solrServer.getBaseURL()
 									+ " is not available. "
 									+ "\nChange solr.host and solr.port properties in uim.properties and restart UIM");
 					return false;
 				}
 
 				IUnmarshallingContext uctx = bfact.createUnmarshallingContext();
 				RDF rdf = (RDF) uctx.unmarshalDocument(new StringReader(value));
 				SolrInputDocument basicDocument = new SolrConstructor()
 						.constructSolrDocument(rdf);
 				// migrationSolrList.add(basicDocument);
 				
 				List<Entity> entities = tagger.tagDocument(basicDocument);
 				mergeEntities(rdf, entities);
 				RDF rdfFinal = cleanRDF(rdf);
 				boolean hasEuropeanaProxy = false;
 
 				for (ProxyType proxy : rdfFinal.getProxyList()) {
 					if (proxy.getEuropeanaProxy() != null
 							&& proxy.getEuropeanaProxy().isEuropeanaProxy()) {
 						hasEuropeanaProxy = true;
 					}
 				}
 				if (!hasEuropeanaProxy) {
 					ProxyType europeanaProxy = new ProxyType();
 					EuropeanaProxy prx = new EuropeanaProxy();
 					prx.setEuropeanaProxy(true);
 					europeanaProxy.setEuropeanaProxy(prx);
 					List<String> years = new ArrayList<String>();
 					for (ProxyType proxy : rdfFinal.getProxyList()) {
 						years.addAll(new EuropeanaDateUtils()
 								.createEuropeanaYears(proxy));
 						europeanaProxy.setType(proxy.getType());
 					}
 					List<Year> yearList = new ArrayList<Year>();
 					for (String year : years) {
 						Year yearObj = new Year();
 						LiteralType.Lang lang = new LiteralType.Lang();
 						lang.setLang("eur");
 						yearObj.setLang(lang);
 						yearObj.setString(year);
 						yearList.add(yearObj);
 					}
 
 					for (ProxyType proxy : rdfFinal.getProxyList()) {
 						if (proxy != null && proxy.getEuropeanaProxy() != null
 								&& proxy.getEuropeanaProxy().isEuropeanaProxy()) {
 							rdfFinal.getProxyList().remove(proxy);
 						}
 					}
 					rdfFinal.getProxyList().add(europeanaProxy);
 				}
 				SolrInputDocument solrInputDocument = new SolrConstructor()
 						.constructSolrDocument(rdfFinal);
 
 				FullBeanImpl fullBean = mongoConstructor.constructFullBean(
 						rdfFinal, mongoServer);
 				solrInputDocument.addField(
 						EdmLabel.PREVIEW_NO_DISTRIBUTE.toString(),
 						previewsOnlyInPortal);
 
 				fullBean.getAggregations()
 						.get(0)
 						.setEdmPreviewNoDistribute(
 								Boolean.parseBoolean(previewsOnlyInPortal));
 				int completeness = RecordCompletenessRanking
 						.rankRecordCompleteness(solrInputDocument);
 				fullBean.setEuropeanaCompleteness(completeness);
 				solrInputDocument.addField(
 						EdmLabel.EUROPEANA_COMPLETENESS.toString(),
 						completeness);
 				String collectionId = (String) mdr.getCollection()
 						.getMnemonic();
 				String fileName;
 				String oldCollectionId = enrichmentService
 						.getCollectionMongoServer().findOldCollectionId(
 								collectionId);
 				if (oldCollectionId != null) {
 					collectionId = oldCollectionId;
 					fileName = oldCollectionId;
 				} else {
 					fileName = (String) mdr.getCollection().getName();
 				}
 
 				
 
 				fullBean.setEuropeanaCollectionName(new String[] { mdr.getCollection().getName() });
 				solrInputDocument
 						.setField("europeana_collectionName", mdr.getCollection().getName());
 				if (mongoServer.getFullBean(fullBean.getAbout()) == null) {
 					mongoServer.getDatastore().save(fullBean);
 				} else {
 					updateFullBean(mongoServer,fullBean);
 
 				}
 				// FileUtils.write(new File("/home/gmamakis/"
 				// + fullBean.getAbout().replace("/", "_") + ".xml"),
 				// EDMUtils.toEDM(fullBean));
 				int retries = 0;
 				while (retries < RETRIES) {
 					try {
 						solrList.addToQueue(solrServer, solrInputDocument);
 						recordNumber++;
 						// Send records to SOLR by thousands
 
 						return true;
 					} catch (SolrException e) {
 						log.log(Level.WARNING,
 								"Solr Exception occured with error "
 										+ e.getMessage() + "\nRetrying");
 
 					}
 					retries++;
 				}
 				
 
 			} catch (JiBXException e) {
 				log.log(Level.WARNING,
 						"JibX Exception occured with error " + e.getMessage()
 								+ "\nRetrying");
 			} catch (MalformedURLException e) {
 				log.log(Level.WARNING,
 						"Malformed URL Exception occured with error "
 								+ e.getMessage() + "\nRetrying");
 			} catch (InstantiationException e) {
 				log.log(Level.WARNING,
 						"Instantiation Exception occured with error "
 								+ e.getMessage() + "\nRetrying");
 			} catch (IllegalAccessException e) {
 				log.log(Level.WARNING,
 						"Illegal Access Exception occured with error "
 								+ e.getMessage() + "\nRetrying");
 			} catch (IOException e) {
 				log.log(Level.WARNING,
 						"IO Exception occured with error " + e.getMessage()
 								+ "\nRetrying");
 			} catch (Exception e) {
 
 				log.log(Level.WARNING, "Generic Exception occured with error "
 						+ e.getMessage() + "\nRetrying");
 				e.printStackTrace();
 			}
 			return false;
 		} else {
 
 			IUnmarshallingContext uctx;
 			try {
 				uctx = bfact.createUnmarshallingContext();
 				RDF rdf = (RDF) uctx.unmarshalDocument(new StringReader(value));
 				FullBean fBean = mongoServer.getFullBean(rdf
 						.getProvidedCHOList().get(0).getAbout());
 				if (fBean != null) {
 					mongoServer.getDatastore().delete(fBean.getAggregations());
 					mongoServer.getDatastore().delete(fBean.getProvidedCHOs());
 					mongoServer.getDatastore().delete(fBean.getProxies());
 					mongoServer.getDatastore().delete(
 							fBean.getEuropeanaAggregation());
 					mongoServer.getDatastore().delete(fBean);
 					solrServer.deleteByQuery("europeana_id:"
							+ ClientUtils.escapeQueryChars(fBean.getAbout()),5000);
 				}
 			} catch (JiBXException e) {
 				// TODO Auto-generated catch block
 				e.printStackTrace();
 			} catch (SolrServerException e) {
 				// TODO Auto-generated catch block
 				e.printStackTrace();
 			} catch (IOException e) {
 				// TODO Auto-generated catch block
 				e.printStackTrace();
 			}
 
 		}
 		
 		return true;
 	}
 
 	
 	private void clearData(OsgiEdmMongoServer mongoServer, String collection) {
 		// TODO Auto-generated method stub
 		DBCollection records = mongoServer.getDatastore().getDB()
 				.getCollection("record");
 		DBCollection proxies = mongoServer.getDatastore().getDB()
 				.getCollection("Proxy");
 		DBCollection providedCHOs = mongoServer.getDatastore().getDB()
 				.getCollection("ProvidedCHO");
 		DBCollection aggregations = mongoServer.getDatastore().getDB()
 				.getCollection("Aggregation");
 		DBCollection europeanaAggregations = mongoServer.getDatastore().getDB()
 				.getCollection("EuropeanaAggregation");
 		
 
 		DBObject query = new BasicDBObject("about", Pattern.compile("^/"+collection+"/"));
 		DBObject proxyQuery = new BasicDBObject("about", "/proxy/provider"+
 				Pattern.compile("^/"+collection+"/"));
 		DBObject europeanaProxyQuery = new BasicDBObject("about",
 				"/proxy/europeana" + Pattern.compile("^/"+collection+"/"));
 
 		DBObject providedCHOQuery = new BasicDBObject("about", "/item" + Pattern.compile("^/"+collection+"/"));
 		DBObject aggregationQuery = new BasicDBObject("about",
 				"/aggregation/provider" + Pattern.compile("^/"+collection+"/"));
 		DBObject europeanaAggregationQuery = new BasicDBObject("about",
 				"/aggregation/europeana" + Pattern.compile("^/"+collection+"/"));
 
 		europeanaAggregations.remove(europeanaAggregationQuery,WriteConcern.JOURNAL_SAFE);
 		records.remove(query,WriteConcern.JOURNAL_SAFE);
 		proxies.remove(europeanaProxyQuery,WriteConcern.JOURNAL_SAFE);
 		proxies.remove(proxyQuery,WriteConcern.JOURNAL_SAFE);
 		providedCHOs.remove(providedCHOQuery,WriteConcern.JOURNAL_SAFE);
 		aggregations.remove(aggregationQuery,WriteConcern.JOURNAL_SAFE);
 	}
 
 	// update a FullBean
 	private void updateFullBean(OsgiEdmMongoServer mongoServer, FullBeanImpl fullBean) {
 		Query<FullBeanImpl> updateQuery = mongoServer.getDatastore()
 				.createQuery(FullBeanImpl.class).field("about")
 				.equal(fullBean.getAbout().replace("/item", ""));
 		UpdateOperations<FullBeanImpl> ops = mongoServer.getDatastore()
 				.createUpdateOperations(FullBeanImpl.class);
 		ops.set("title", fullBean.getTitle() != null ? fullBean.getTitle()
 				: new String[] {});
 		ops.set("year", fullBean.getYear() != null ? fullBean.getYear()
 				: new String[] {});
 		ops.set("provider",
 				fullBean.getProvider() != null ? fullBean.getProvider()
 						: new String[] {});
 		ops.set("language",
 				fullBean.getLanguage() != null ? fullBean.getLanguage()
 						: new String[] {});
 		ops.set("type", fullBean.getType() != null ? fullBean.getType()
 				: DocType.IMAGE);
 		ops.set("europeanaCompleteness", fullBean.getEuropeanaCompleteness());
 		ops.set("optOut", fullBean.isOptedOut());
 		ops.set("places", fullBean.getPlaces() != null ? fullBean.getPlaces()
 				: new ArrayList<PlaceImpl>());
 		ops.set("agents", fullBean.getAgents() != null ? fullBean.getAgents()
 				: new ArrayList<AgentImpl>());
 		ops.set("timespans",
 				fullBean.getTimespans() != null ? fullBean.getTimespans()
 						: new ArrayList<TimespanImpl>());
 		ops.set("concepts",
 				fullBean.getConcepts() != null ? fullBean.getConcepts()
 						: new ArrayList<ConceptImpl>());
 		ops.set("aggregations", fullBean.getAggregations());
 		ops.set("providedCHOs", fullBean.getProvidedCHOs());
 		ops.set("europeanaAggregation", fullBean.getEuropeanaAggregation());
 		ops.set("proxies", fullBean.getProxies());
 		ops.set("country",
 				fullBean.getCountry() != null ? fullBean.getCountry()
 						: new String[] {});
 		ops.set("europeanaCollectionName",
 				fullBean.getEuropeanaCollectionName());
 		mongoServer.getDatastore().update(updateQuery, ops);
 	}
 
 	/*
 	 * Merge Contextual Entities
 	 */
 	private void mergeEntities(RDF rdf, List<Entity> entities)
 			throws SecurityException, IllegalArgumentException,
 			NoSuchMethodException, IllegalAccessException,
 			InvocationTargetException {
 		ProxyType europeanaProxy = null;
 		ProvidedCHOType cho = null;
 
 		List<ProvidedCHOType> providedChoList = rdf.getProvidedCHOList();
 		cho = providedChoList.get(0);
 		List<ProxyType> proxyList = rdf.getProxyList();
 		for (ProxyType proxy : proxyList) {
 			if (proxy.getEuropeanaProxy() != null
 					&& proxy.getEuropeanaProxy().isEuropeanaProxy()) {
 				europeanaProxy = proxy;
 			}
 		}
 
 		if (europeanaProxy == null) {
 			europeanaProxy = createEuropeanaProxy(rdf);
 		}
 		europeanaProxy.setAbout("/proxy/europeana" + cho.getAbout());
 		ProxyFor pf = new ProxyFor();
 		pf.setResource("/item" + cho.getAbout());
 		europeanaProxy.setProxyFor(pf);
 		List<ProxyIn> pinList = new ArrayList<ProxyIn>();
 		ProxyIn pin = new ProxyIn();
 		pin.setResource("/aggregation/europeana" + cho.getAbout());
 		pinList.add(pin);
 		europeanaProxy.setProxyInList(pinList);
 		for (Entity entity : entities) {
 
 			if (StringUtils.equals(entity.getClassName(), "Concept")) {
 				Concept concept = new Concept();
 				List<Field> fields = entity.getFields();
 				if (fields != null && fields.size() > 0) {
 					for (Field field : fields) {
 						if (StringUtils.equalsIgnoreCase(field.getName(),
 								"skos_concept")) {
 							concept.setAbout(field
 									.getValues()
 									.get(field.getValues().keySet().iterator()
 											.next()).get(0));
 							// addToHasMetList(
 							// europeanaProxy,
 							// field.getValues()
 							// .get(field.getValues().keySet()
 							// .iterator().next()).get(0));
 						} else {
 							if (field.getValues() != null) {
 								for (Entry<String, List<String>> entry : field
 										.getValues().entrySet()) {
 									for (String str : entry.getValue()) {
 										appendConceptValue(concept,
 												field.getName(), str,
 												"_@xml:lang", entry.getKey());
 									}
 								}
 							}
 						}
 
 					}
 					List<Concept> conceptList = rdf.getConceptList() != null ? rdf
 							.getConceptList() : new ArrayList<Concept>();
 					conceptList.add(concept);
 					rdf.setConceptList(conceptList);
 					try {
 						europeanaProxy = OriginalField.getOriginalField(
 								entity.getOriginalField()).appendField(
 								europeanaProxy, concept.getAbout());
 					} catch (IllegalArgumentException e) {
 
 					}
 
 				}
 			} else if (StringUtils.equals(entity.getClassName(), "Timespan")) {
 
 				TimeSpanType ts = new TimeSpanType();
 				List<Field> fields = entity.getFields();
 				if (fields != null && fields.size() > 0) {
 					for (Field field : fields) {
 						if (StringUtils.equalsIgnoreCase(field.getName(),
 								"edm_timespan")) {
 							ts.setAbout(field
 									.getValues()
 									.get(field.getValues().keySet().iterator()
 											.next()).get(0));
 							// addToHasMetList(
 							// europeanaProxy,
 							// field.getValues()
 							// .get(field.getValues().keySet()
 							// .iterator().next()).get(0));
 						} else {
 							for (Entry<String, List<String>> entry : field
 									.getValues().entrySet()) {
 								for (String str : entry.getValue()) {
 									appendValue(TimeSpanType.class, ts,
 											field.getName(), str, "_@xml:lang",
 											entry.getKey());
 								}
 							}
 						}
 
 					}
 					List<TimeSpanType> timespans = rdf.getTimeSpanList() != null ? rdf
 							.getTimeSpanList() : new ArrayList<TimeSpanType>();
 					timespans.add(ts);
 					rdf.setTimeSpanList(timespans);
 					try {
 						europeanaProxy = OriginalField.getOriginalField(
 								entity.getOriginalField()).appendField(
 								europeanaProxy, ts.getAbout());
 					} catch (IllegalArgumentException e) {
 
 					}
 				}
 			} else if (StringUtils.equals(entity.getClassName(), "Agent")) {
 
 				AgentType ts = new AgentType();
 				List<Field> fields = entity.getFields();
 				if (fields != null && fields.size() > 0) {
 					for (Field field : fields) {
 						if (StringUtils.equalsIgnoreCase(field.getName(),
 								"edm_agent")) {
 							ts.setAbout(field
 									.getValues()
 									.get(field.getValues().keySet().iterator()
 											.next()).get(0));
 							// addToHasMetList(
 							// europeanaProxy,
 							// field.getValues()
 							// .get(field.getValues().keySet()
 							// .iterator().next()).get(0));
 						} else {
 							for (Entry<String, List<String>> entry : field
 									.getValues().entrySet()) {
 								for (String str : entry.getValue()) {
 									appendValue(AgentType.class, ts,
 											field.getName(), str, "_@xml:lang",
 											entry.getKey());
 								}
 							}
 						}
 
 					}
 					List<AgentType> agents = rdf.getAgentList() != null ? rdf
 							.getAgentList() : new ArrayList<AgentType>();
 					agents.add(ts);
 					rdf.setAgentList(agents);
 					try {
 						europeanaProxy = OriginalField.getOriginalField(
 								entity.getOriginalField()).appendField(
 								europeanaProxy, ts.getAbout());
 					} catch (IllegalArgumentException e) {
 
 					}
 				}
 			} else {
 				PlaceType ts = new PlaceType();
 				List<Field> fields = entity.getFields();
 				if (fields != null && fields.size() > 0) {
 					for (Field field : fields) {
 						if (StringUtils.equalsIgnoreCase(field.getName(),
 								"edm_place")) {
 							ts.setAbout(field
 									.getValues()
 									.get(field.getValues().keySet().iterator()
 											.next()).get(0));
 							// addToHasMetList(
 							// europeanaProxy,
 							// field.getValues()
 							// .get(field.getValues().keySet()
 							// .iterator().next()).get(0));
 						} else {
 							if (field.getValues() != null) {
 								for (Entry<String, List<String>> entry : field
 										.getValues().entrySet()) {
 									for (String str : entry.getValue()) {
 										appendValue(PlaceType.class, ts,
 												field.getName(), str,
 												"_@xml:lang", entry.getKey());
 									}
 								}
 							}
 						}
 
 					}
 					List<PlaceType> places = rdf.getPlaceList() != null ? rdf
 							.getPlaceList() : new ArrayList<PlaceType>();
 					places.add(ts);
 					rdf.setPlaceList(places);
 					try {
 						europeanaProxy = OriginalField.getOriginalField(
 								entity.getOriginalField()).appendField(
 								europeanaProxy, ts.getAbout());
 					} catch (IllegalArgumentException e) {
 
 					}
 				}
 			}
 		}
 	}
 
 	// private void addToHasMetList(ProxyType europeanaProxy, String value) {
 	// List<HasMet> hasMetList = europeanaProxy.getHasMetList() != null ?
 	// europeanaProxy
 	// .getHasMetList() : new ArrayList<HasMet>();
 	// HasMet hasMet = new HasMet();
 	// hasMet.setString(value);
 	// hasMetList.add(hasMet);
 	// europeanaProxy.setHasMetList(hasMetList);
 	// }
 
 	private ProxyType createEuropeanaProxy(RDF rdf) {
 		ProxyType europeanaProxy = new ProxyType();
 		EuropeanaProxy prx = new EuropeanaProxy();
 		prx.setEuropeanaProxy(true);
 		europeanaProxy.setEuropeanaProxy(prx);
 		List<String> years = new ArrayList<String>();
 		for (ProxyType proxy : rdf.getProxyList()) {
 			years.addAll(new EuropeanaDateUtils().createEuropeanaYears(proxy));
 			europeanaProxy.setType(proxy.getType());
 		}
 		List<Year> yearList = new ArrayList<Year>();
 		for (String year : years) {
 			Year yearObj = new Year();
 			LiteralType.Lang lang = new LiteralType.Lang();
 			lang.setLang("eur");
 			yearObj.setLang(lang);
 			yearObj.setString(year);
 			yearList.add(yearObj);
 		}
 		return europeanaProxy;
 	}
 
 	// Clean duplicate contextual entities
 	private RDF cleanRDF(RDF rdf) {
 		RDF rdfFinal = new RDF();
 		List<AgentType> agents = new CopyOnWriteArrayList<AgentType>();
 		List<TimeSpanType> timespans = new CopyOnWriteArrayList<TimeSpanType>();
 		List<PlaceType> places = new CopyOnWriteArrayList<PlaceType>();
 		List<Concept> concepts = new CopyOnWriteArrayList<Concept>();
 		if (rdf.getAgentList() != null) {
 			for (AgentType newAgent : rdf.getAgentList()) {
 				for (AgentType agent : agents) {
 					if (StringUtils.equals(agent.getAbout(),
 							newAgent.getAbout())) {
 						if (agent.getPrefLabelList() != null
 								&& newAgent.getPrefLabelList() != null) {
 							if (agent.getPrefLabelList().size() <= newAgent
 									.getPrefLabelList().size()) {
 								agents.remove(agent);
 							}
 						}
 					}
 				}
 				agents.add(newAgent);
 			}
 			rdfFinal.setAgentList(agents);
 		}
 		if (rdf.getConceptList() != null) {
 			for (Concept newConcept : rdf.getConceptList()) {
 				for (Concept concept : concepts) {
 					if (StringUtils.equals(concept.getAbout(),
 							newConcept.getAbout())) {
 						if (concept.getChoiceList() != null
 								&& newConcept.getChoiceList() != null) {
 							if (concept.getChoiceList().size() <= newConcept
 									.getChoiceList().size()) {
 								concepts.remove(concept);
 							}
 						}
 					}
 
 				}
 				concepts.add(newConcept);
 			}
 			rdfFinal.setConceptList(concepts);
 		}
 		if (rdf.getTimeSpanList() != null) {
 			for (TimeSpanType newTs : rdf.getTimeSpanList()) {
 				for (TimeSpanType ts : timespans) {
 					if (StringUtils.equals(ts.getAbout(), newTs.getAbout())) {
 						if (newTs.getIsPartOfList() != null
 								&& ts.getIsPartOfList() != null) {
 							if (ts.getIsPartOfList().size() <= newTs
 									.getIsPartOfList().size()) {
 								timespans.remove(ts);
 							}
 						}
 					}
 
 				}
 				timespans.add(newTs);
 			}
 			rdfFinal.setTimeSpanList(timespans);
 		}
 		if (rdf.getPlaceList() != null) {
 			for (PlaceType newPlace : rdf.getPlaceList()) {
 				for (PlaceType place : places) {
 					if (StringUtils.equals(place.getAbout(),
 							newPlace.getAbout())) {
 						if (place.getPrefLabelList() != null
 								&& newPlace.getPrefLabelList() != null) {
 							if (place.getPrefLabelList().size() <= newPlace
 									.getPrefLabelList().size()) {
 								places.remove(place);
 							}
 						}
 					}
 
 				}
 				places.add(newPlace);
 			}
 			rdfFinal.setPlaceList(places);
 		}
 		rdfFinal.setProxyList(rdf.getProxyList());
 		rdfFinal.setProvidedCHOList(rdf.getProvidedCHOList());
 		rdfFinal.setAggregationList(rdf.getAggregationList());
 
 		rdfFinal.setWebResourceList(rdf.getWebResourceList());
 		List<WebResourceType> webResources = new ArrayList<WebResourceType>();
 		for (Aggregation aggr : rdf.getAggregationList()) {
 			if (aggr.getIsShownAt() != null) {
 				WebResourceType wr = new WebResourceType();
 				wr.setAbout(aggr.getIsShownAt().getResource());
 				webResources.add(wr);
 			}
 			if (aggr.getIsShownBy() != null) {
 				WebResourceType wr = new WebResourceType();
 				wr.setAbout(aggr.getIsShownBy().getResource());
 				webResources.add(wr);
 			}
 			if (aggr.getObject() != null) {
 				WebResourceType wr = new WebResourceType();
 				wr.setAbout(aggr.getObject().getResource());
 				webResources.add(wr);
 			}
 			if (aggr.getHasViewList() != null) {
 				for (HasView hasView : aggr.getHasViewList()) {
 					WebResourceType wr = new WebResourceType();
 					wr.setAbout(hasView.getResource());
 					webResources.add(wr);
 				}
 			}
 		}
 		if (webResources.size() > 0) {
 			if (rdfFinal.getWebResourceList() != null) {
 				rdfFinal.getWebResourceList().addAll(webResources);
 			} else {
 				rdfFinal.setWebResourceList(webResources);
 			}
 		}
 
 		List<EuropeanaAggregationType> eTypeList = new ArrayList<EuropeanaAggregationType>();
 
 		eTypeList.add(createEuropeanaAggregation(rdf));
 
 		rdfFinal.setEuropeanaAggregationList(eTypeList);
 		return rdfFinal;
 	}
 
 	private EuropeanaAggregationType createEuropeanaAggregation(RDF rdf) {
 		EuropeanaAggregationType europeanaAggregation = null;
 		if (rdf.getEuropeanaAggregationList() != null
 				&& rdf.getEuropeanaAggregationList().size() > 0) {
 			europeanaAggregation = rdf.getEuropeanaAggregationList().get(0);
 		} else {
 			europeanaAggregation = new EuropeanaAggregationType();
 		}
 		ProvidedCHOType cho = rdf.getProvidedCHOList().get(0);
 		europeanaAggregation
 				.setAbout("/aggregation/europeana" + cho.getAbout());
 		LandingPage lp = new LandingPage();
 		lp.setResource(PORTALURL + cho.getAbout() + SUFFIX);
 		europeanaAggregation.setLandingPage(lp);
 		Country countryType = new Country();
 
 		countryType
 				.setCountry(europeanaAggregation.getCountry() != null ? europeanaAggregation
 						.getCountry().getCountry() : CountryCodes.EUROPE);
 
 		europeanaAggregation.setCountry(countryType);
 		Creator creatorType = new Creator();
 		creatorType.setString("Europeana");
 		europeanaAggregation.setCreator(creatorType);
 		Language1 languageType = new Language1();
 		languageType
 				.setLanguage(europeanaAggregation.getLanguage() != null ? europeanaAggregation
 						.getLanguage().getLanguage() : LanguageCodes.EN);
 
 		europeanaAggregation.setLanguage(languageType);
 		Rights1 rightsType = new Rights1();
 
 		if (europeanaAggregation.getRights() != null) {
 			rightsType.setResource(europeanaAggregation.getRights()
 					.getResource());
 		} else {
 			Resource res = new Resource();
 			res.setResource("http://creativecommons.org/licenses/by-sa/3.0/");
 			rightsType.setResource(res);
 		}
 
 		europeanaAggregation.setRights(rightsType);
 		AggregatedCHO aggrCHO = new AggregatedCHO();
 		aggrCHO.setResource(cho.getAbout());
 		europeanaAggregation.setAggregatedCHO(aggrCHO);
 		return europeanaAggregation;
 	}
 
 	public HttpSolrServer getSolrServer() {
 		return solrServer;
 	}
 
 	public void setSolrServer(HttpSolrServer solrServer) {
 		EnrichmentPlugin.solrServer = solrServer;
 	}
 
 	public void setSugarCrmService(SugarCrmService sugarCrmService) {
 		EnrichmentPlugin.sugarCrmService = sugarCrmService;
 	}
 
 	public String getEuropeanaID() {
 		return europeanaID;
 	}
 
 	public void setEuropeanaID(String europeanaID) {
 		EnrichmentPlugin.europeanaID = europeanaID;
 	}
 
 	public int getRecords() {
 		return recordNumber;
 	}
 
 	public String getRepository() {
 		return repository;
 	}
 
 	public void setRepository(String repository) {
 		EnrichmentPlugin.repository = repository;
 	}
 
 	public String getCollections() {
 		return collections;
 	}
 
 	public void setCollections(String collections) {
 		EnrichmentPlugin.collections = collections;
 	}
 
 	public SugarCrmService getSugarCrmService() {
 		return sugarCrmService;
 	}
 
 	public String getMongoDB() {
 		return mongoDB;
 	}
 
 	public void setMongoDB(String mongoDB) {
 		EnrichmentPlugin.mongoDB = mongoDB;
 	}
 
 	public String getMongoHost() {
 		return mongoHost;
 	}
 
 	public void setMongoHost(String mongoHost) {
 		EnrichmentPlugin.mongoHost = mongoHost;
 	}
 
 	public String getMongoPort() {
 		return mongoPort;
 	}
 
 	public void setMongoPort(String mongoPort) {
 		EnrichmentPlugin.mongoPort = mongoPort;
 	}
 
 	public String getSolrUrl() {
 		return solrUrl;
 	}
 
 	public void setSolrUrl(String solrUrl) {
 		EnrichmentPlugin.solrUrl = solrUrl;
 	}
 
 	public String getSolrCore() {
 		return solrCore;
 	}
 
 	public void setSolrCore(String solrCore) {
 		EnrichmentPlugin.solrCore = solrCore;
 	}
 
 	public EnrichmentService getEnrichmentService() {
 		return enrichmentService;
 	}
 
 	public void setEnrichmentService(EnrichmentService enrichmentService) {
 		EnrichmentPlugin.enrichmentService = enrichmentService;
 	}
 
 	// check if the hash of the record exists
 	
 
 	// append the rest of the contextual entities
 	@SuppressWarnings({ "unchecked", "rawtypes" })
 	private <T> T appendValue(Class<T> clazz, T obj, String edmLabel,
 			String val, String edmAttr, String valAttr)
 			throws SecurityException, NoSuchMethodException,
 			IllegalArgumentException, IllegalAccessException,
 			InvocationTargetException {
 		RdfMethod RDF = null;
 		for (RdfMethod rdfMethod : RdfMethod.values()) {
 			if (StringUtils.equals(rdfMethod.getSolrField(), edmLabel)) {
 				RDF = rdfMethod;
 			}
 		}
 
 		//
 		if (RDF != null) {
 			if (RDF.getMethodName().endsWith("List")) {
 
 				Method mthd = clazz.getMethod(RDF.getMethodName());
 
 				List lst = mthd.invoke(obj) != null ? (ArrayList) mthd
 						.invoke(obj) : new ArrayList();
 				if (RDF.getClazz().getSuperclass()
 						.isAssignableFrom(ResourceType.class)) {
 
 					ResourceType rs = new ResourceType();
 					rs.setResource(val);
 					lst.add(RDF.returnObject(RDF.getClazz(), rs));
 
 				} else if (RDF.getClazz().getSuperclass()
 						.isAssignableFrom(ResourceOrLiteralType.class)) {
 					ResourceOrLiteralType rs = new ResourceOrLiteralType();
 					if (isURI(val)) {
 
 						Resource res = new Resource();
 						res.setResource(val);
 						rs.setResource(res);
 
 					} else {
 						rs.setString(val);
 					}
 					if (edmAttr != null
 							&& StringUtils.equals(
 									StringUtils.split(edmAttr, "@")[1],
 									"xml:lang")) {
 						Lang lang = new Lang();
 						lang.setLang(valAttr);
 						rs.setLang(lang);
 					}
 					lst.add(RDF.returnObject(RDF.getClazz(), rs));
 				} else if (RDF.getClazz().getSuperclass()
 						.isAssignableFrom(LiteralType.class)) {
 					LiteralType rs = new LiteralType();
 					rs.setString(val);
 					LiteralType.Lang lang = new LiteralType.Lang();
 					if (edmAttr != null
 							&& StringUtils.equals(
 									StringUtils.split(edmAttr, "@")[1],
 									"xml:lang")) {
 
 						lang.setLang(valAttr);
 
 					} else {
 						lang.setLang("def");
 					}
 					rs.setLang(lang);
 					lst.add(RDF.returnObject(RDF.getClazz(), rs));
 				}
 
 				Class<?>[] cls = new Class<?>[1];
 				cls[0] = List.class;
 				Method method = obj.getClass().getMethod(
 						StringUtils.replace(RDF.getMethodName(), "get", "set"),
 						cls);
 				method.invoke(obj, lst);
 			} else {
 				if (RDF.getClazz().isAssignableFrom(ResourceType.class)) {
 					ResourceType rs = new ResourceType();
 					rs.setResource(val);
 					Class<?>[] cls = new Class<?>[1];
 					cls[0] = RDF.getClazz();
 					Method method = obj.getClass().getMethod(
 							StringUtils.replace(RDF.getMethodName(), "get",
 									"set"), cls);
 					method.invoke(obj, RDF.returnObject(RDF.getClazz(), rs));
 				} else if (RDF.getClazz().isAssignableFrom(LiteralType.class)) {
 					LiteralType rs = new LiteralType();
 					rs.setString(val);
 					LiteralType.Lang lang = new LiteralType.Lang();
 					if (edmAttr != null
 							&& StringUtils.equals(
 									StringUtils.split(edmAttr, "@")[1],
 									"xml:lang")) {
 
 						lang.setLang(valAttr);
 
 					} else {
 						lang.setLang("def");
 					}
 					rs.setLang(lang);
 					Class<?>[] cls = new Class<?>[1];
 					cls[0] = RDF.getClazz();
 					Method method = obj.getClass().getMethod(
 							StringUtils.replace(RDF.getMethodName(), "get",
 									"set"), cls);
 					method.invoke(obj, RDF.returnObject(RDF.getClazz(), rs));
 
 				} else if (RDF.getClazz().isAssignableFrom(
 						ResourceOrLiteralType.class)) {
 					ResourceOrLiteralType rs = new ResourceOrLiteralType();
 					if (isURI(val)) {
 						Resource res = new Resource();
 						res.setResource(val);
 						rs.setResource(res);
 					} else {
 						rs.setString(val);
 					}
 					Lang lang = new Lang();
 					if (edmAttr != null
 							&& StringUtils.equals(
 									StringUtils.split(edmAttr, "@")[1],
 									"xml:lang")) {
 
 						lang.setLang(valAttr);
 
 					} else {
 						lang.setLang("def");
 					}
 					rs.setLang(lang);
 					Class<?>[] cls = new Class<?>[1];
 					cls[0] = clazz;
 					Method method = obj.getClass().getMethod(
 							StringUtils.replace(RDF.getMethodName(), "get",
 									"set"), cls);
 					method.invoke(obj, RDF.returnObject(RDF.getClazz(), rs));
 				} else if (RDF.getClazz().isAssignableFrom(_Long.class)) {
 					Float rs = Float.parseFloat(val);
 					_Long lng = new _Long();
 					lng.setLong(rs);
 					((PlaceType) obj).setLong(lng);
 
 				} else if (RDF.getClazz().isAssignableFrom(Lat.class)) {
 					Float rs = Float.parseFloat(val);
 					Lat lng = new Lat();
 					lng.setLat(rs);
 					((PlaceType) obj).setLat(lng);
 
 				} else if (RDF.getClazz().isAssignableFrom(Alt.class)) {
 					Float rs = Float.parseFloat(val);
 					Alt lng = new Alt();
 					lng.setAlt(rs);
 					((PlaceType) obj).setAlt(lng);
 
 				}
 			}
 		}
 		//
 		return obj;
 	}
 
 	// Append concepts
 	private Concept appendConceptValue(Concept concept, String edmLabel,
 			String val, String edmAttr, String valAttr)
 			throws SecurityException, NoSuchMethodException,
 			IllegalArgumentException, IllegalAccessException,
 			InvocationTargetException {
 		RdfMethod RDF = null;
 		for (RdfMethod rdfMethod : RdfMethod.values()) {
 			if (StringUtils.equals(rdfMethod.getSolrField(), edmLabel)) {
 				RDF = rdfMethod;
 				break;
 			}
 		}
 		List<Concept.Choice> lst = concept.getChoiceList() != null ? concept
 				.getChoiceList() : new ArrayList<Concept.Choice>();
 		if (RDF.getClazz().getSuperclass().isAssignableFrom(ResourceType.class)) {
 
 			ResourceType obj = new ResourceType();
 			obj.setResource(val);
 			Class<?>[] cls = new Class<?>[1];
 			cls[0] = RDF.getClazz();
 			Concept.Choice choice = new Concept.Choice();
 			Method method = choice.getClass()
 					.getMethod(
 							StringUtils.replace(RDF.getMethodName(), "get",
 									"set"), cls);
 			method.invoke(choice, RDF.returnObject(RDF.getClazz(), obj));
 			lst.add(choice);
 
 		} else if (RDF.getClazz().getSuperclass()
 				.isAssignableFrom(ResourceOrLiteralType.class)) {
 
 			ResourceOrLiteralType obj = new ResourceOrLiteralType();
 
 			if (isURI(val)) {
 				Resource res = new Resource();
 				res.setResource(val);
 				obj.setResource(res);
 			} else {
 				obj.setString(val);
 			}
 			Lang lang = new Lang();
 			if (edmAttr != null
 					&& StringUtils.equals(StringUtils.split(edmAttr, "@")[1],
 							"xml:lang")) {
 
 				lang.setLang(valAttr);
 
 			} else {
 				lang.setLang("def");
 			}
 			obj.setLang(lang);
 			Class<?>[] cls = new Class<?>[1];
 			cls[0] = RDF.getClazz();
 			Concept.Choice choice = new Concept.Choice();
 			Method method = choice.getClass()
 					.getMethod(
 							StringUtils.replace(RDF.getMethodName(), "get",
 									"set"), cls);
 			method.invoke(choice, RDF.returnObject(RDF.getClazz(), obj));
 			lst.add(choice);
 
 		} else if (RDF.getClazz().getSuperclass()
 				.isAssignableFrom(LiteralType.class)) {
 			LiteralType obj = new LiteralType();
 			obj.setString(val);
 			LiteralType.Lang lang = new LiteralType.Lang();
 			if (edmAttr != null) {
 
 				lang.setLang(valAttr);
 
 			} else {
 				lang.setLang("def");
 			}
 			obj.setLang(lang);
 			Class<?>[] cls = new Class<?>[1];
 			cls[0] = RDF.getClazz();
 			Concept.Choice choice = new Concept.Choice();
 			Method method = choice.getClass()
 					.getMethod(
 							StringUtils.replace(RDF.getMethodName(), "get",
 									"set"), cls);
 			method.invoke(choice, RDF.returnObject(RDF.getClazz(), obj));
 			lst.add(choice);
 		}
 		concept.setChoiceList(lst);
 		return concept;
 	}
 
 	/**
 	 * Check if a String is a URI
 	 * 
 	 * @param uri
 	 * @return
 	 */
 	private static boolean isURI(String uri) {
 
 		try {
 			new URL(uri);
 			return true;
 		} catch (MalformedURLException e) {
 			return false;
 		}
 
 	}
 
 }
