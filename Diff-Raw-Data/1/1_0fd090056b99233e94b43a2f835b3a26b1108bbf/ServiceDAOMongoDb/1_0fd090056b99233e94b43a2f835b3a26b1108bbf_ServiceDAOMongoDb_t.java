 package net.es.lookup.database;
 
 
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.HashMap;
 import java.util.Set;
 
 import com.mongodb.*;
 import java.net.UnknownHostException;
 import java.util.regex.Pattern;
 
 import net.es.lookup.common.Service;
 import net.es.lookup.common.Message;
 import net.es.lookup.resources.ServicesResource;
 import net.es.lookup.common.ReservedKeywords;
 import net.es.lookup.common.exception.internal.DatabaseException;
 import net.es.lookup.common.exception.internal.DuplicateEntryException;
 
 public class ServiceDAOMongoDb {
 	private String dburl="127.0.0.1";
 	private int dbport=27017;
 	private String dbname="LookupService";
 	private String collname="services";
 
 	private Mongo mongo;
 	private DB db;
 	private DBCollection coll;
     private static ServiceDAOMongoDb instance = null;
     
 	private static Map<String, String> operatorMapping = new HashMap();
 	private static Map<String, String> listOperatorMapping = new HashMap();
 	
 
     public static ServiceDAOMongoDb getInstance() {
         return ServiceDAOMongoDb.instance;
     }
 
 	// retrieves default - mongodb running on localhost and default port - 27017 and dbname- "lookupservice", collection name - "services" 
 	//creates a new one if it cannot find one 
 	public ServiceDAOMongoDb() throws DatabaseException{
 		init();
 	}
 	
 	//uses default url and port - mongodb running on localhost and default port - 27017
 	//creates a new one if it cannot find one
 	public ServiceDAOMongoDb(String dbname, String collname) throws DatabaseException{
 		this.dbname = dbname;
 		this.collname = collname;
 		init();
 	}
 	
 	//retrieves the db and collection(table); creates a new one if it cannot find one
 	public ServiceDAOMongoDb (String dburl, int dbport, String dbname, String collname) throws DatabaseException{
 		this.dburl = dburl;
 		this.dbport = dbport;
 		this.dbname = dbname;
 		this.collname = collname;
 		init();
 	}
 	
 	private void init() throws DatabaseException {
         if (ServiceDAOMongoDb.instance != null) {
             // An instance has been already created.
             throw new DatabaseException("Attempt to create a second instance of ServiceDAOMongoDb");
         }
         ServiceDAOMongoDb.instance = this;
         try{
         	mongo = new Mongo(dburl,dbport);
         	System.out.println(mongo.getAddress().toString());
 		
         	db = mongo.getDB(dbname);
         	System.out.println(db.getName());
         	coll = db.getCollection(collname);
         	System.out.println(coll.getName());
         }catch(UnknownHostException e){
         	throw new DatabaseException(e.getMessage());
         }catch(Exception e){
         	throw new DatabaseException(e.getMessage());
         }
 		
         operatorMapping.put(ReservedKeywords.RECORD_OPERATOR_ALL, "$and");
         operatorMapping.put(ReservedKeywords.RECORD_OPERATOR_ANY, "$or");
 		
         listOperatorMapping.put(ReservedKeywords.RECORD_OPERATOR_ANY, "$in");
        listOperatorMapping.put(ReservedKeywords.RECORD_OPERATOR_ALL, "$all");
 	}
 	
 	//should use json specific register request and response.
 	public Message queryAndPublishService(Message message, Message queryRequest, Message operators) throws DatabaseException{
 		int errorcode;
 		String errormsg;
 		Message response;
 		
 		//check for duplicates
 		try{
 			List<Service> dupEntries = this.query(message,queryRequest,operators);
 			System.out.println("Duplicate Entries: "+dupEntries.size());
 			if(dupEntries.size()>0){
 				throw new DuplicateEntryException("Record already exists");		
 			}
 		}catch(DatabaseException e){
 			throw new DatabaseException("Error inserting record");
 		}
 		
 		Map<String, Object> services = message.getMap();
 		BasicDBObject doc = new BasicDBObject();
 		doc.putAll(services);
 		
 		WriteResult wrt = coll.insert(doc);
 		
 		CommandResult cmdres = wrt.getLastError();
 		if(cmdres.ok()){
 			errorcode = 200;
 			errormsg = "SUCCESS";
 		}else{
 			throw new DatabaseException("Error inserting record");
 		}
 		
 		response = new Message(services);
 		response.setError(errorcode);
 		response.setErrorMessage(errormsg);
 		return response;
 	}
 	
 	
 	public Message deleteService(Message message) throws DatabaseException{
 		
 		int errorcode;
 		String errormsg;
 	
 		BasicDBObject query = new BasicDBObject();
 		String uri = message.getURI();
 		//TODO: add check to see if only one elem is returned
 		query.put(ReservedKeywords.RECORD_URI, uri);
 		
 		try{
 			WriteResult wrt = coll.remove(query);
 		
 			CommandResult cmdres = wrt.getLastError();
 		
 			if(cmdres.ok()){
 				errorcode = 200;
 				errormsg = "SUCCESS";
 			}else{
 				throw new DatabaseException(cmdres.getErrorMessage());
 			}
 		}catch(MongoException e){
 			throw new DatabaseException(e.getMessage());
 		}
 		
 		Message response = new Message();
 		response.setError(errorcode);
 		response.setErrorMessage(errormsg);
 		return response;
 		
 	}
 	
 	public Message updateService(String serviceid, Message updateRequest) throws DatabaseException{
 		
 		int errorcode;
 		String errormsg;
         
         
         if(serviceid != null && !serviceid.isEmpty()){
         	
         	BasicDBObject query = new BasicDBObject();
         	query.put(ReservedKeywords.RECORD_URI, serviceid);
         	
         	System.out.println(query);
         	
         	BasicDBObject updateObject = new BasicDBObject();
         	updateObject.putAll(updateRequest.getMap());
         	
         	System.out.println(updateObject);
         	
         	try{
         		WriteResult wrt = coll.update(query, updateObject);
         		CommandResult cmdres = wrt.getLastError();
         		System.out.println(cmdres.ok());
         	
         		if(cmdres.ok()){
         			errorcode=200;
         			errormsg="SUCCESS";
         		}else{
         			throw new DatabaseException(cmdres.getErrorMessage());
         		}
         	}catch(MongoException e){
         		throw new DatabaseException(e.getMessage());
         	}
     		
         }else{
         	throw new DatabaseException("Record URI not specified!!!");
         }
 		
         Message response = new Message();
         response.setError(errorcode);
         response.setErrorMessage(errormsg);
 		return response;
 	}
 
     public List<Service> query(Message message, Message queryRequest, Message operators) throws DatabaseException{
         return this.query (message, queryRequest, operators, 0, 0);
     }
 	
 	public List<Service> query(Message message, Message queryRequest, Message operators, int maxResults, int skip) throws DatabaseException{
 		
 		BasicDBObject query = buildQuery(queryRequest, operators);
 		
 		ArrayList <Service> result = new ArrayList<Service>();
 		
 		try{
 			DBCursor cur = coll.find(query);	
 			
 			while (cur.hasNext()){
 				Service tmpserv = new Service();
 				DBObject tmp = cur.next();
 				Set<String> keys = tmp.keySet();
 				if (!keys.isEmpty()){
 					Iterator<String> it = keys.iterator();
 					while(it.hasNext()){	
 						String tmpKey = it.next();
 						tmpserv.add (tmpKey,tmp.get(tmpKey));  
 					}
 				}
 				result.add(tmpserv);
 			}
 		}catch(MongoException e){
 			throw new DatabaseException("Error retrieving results");
 		}
 
 		return result;
 	}
 	
 	
 	
 	//Builds the query from the given map
 	private BasicDBObject buildQuery(Message queryRequest, Message operators){
 		Map<String, Object> serv =  queryRequest.getMap();
 		
 		Map<String, String> ops = operators.getMap();
 	
 		List <HashMap<String,Object>> keyValueList = new ArrayList<HashMap<String,Object>>();
 		
         for (Map.Entry<String,Object> entry : serv.entrySet()) {
             String newKey = entry.getKey();
             HashMap<String, Object> tmpHash = new HashMap<String, Object>();
             Object obj = serv.get(newKey);
             if (obj instanceof String) {
                  
             	String val = (String) obj;
             	//deal with metacharacter
             	 if(val.endsWith("*")){
             		 val = val.substring(0, val.length()-1);
             		 System.out.println(val);
             		 Pattern newVal = Pattern.compile("^"+val);
             		 tmpHash.put(newKey, newVal);
             	 }else if(val.startsWith("*")){
             		 val = val.substring(1, val.length());
             		 System.out.println(val);
             		 Pattern newVal = Pattern.compile(val+"$");
             		 tmpHash.put(newKey, newVal);
             	 }else{
             		 tmpHash.put(newKey, (String) obj);
             	 }
             } else if (obj instanceof List) {
                  List <Object> values = (List<Object>) obj;
                  ArrayList newValues = new ArrayList();
                  if(values.size()>1){
                 	 for(int i=0; i<values.size();i++){
                 		 String val = (String)values.get(i);
                 		 if(val.endsWith("*")){
                     		 val = val.substring(0, val.length()-1);
                     		 System.out.println(val);
                     		 Pattern newVal = Pattern.compile("^"+val);
                     		 newValues.add(newVal);
                     	 }else if(val.startsWith("*")){
                     		 val = val.substring(1, val.length());
                     		 System.out.println(val);
                     		 Pattern newVal = Pattern.compile(val+"$");
                     		 newValues.add(newVal);
                     	 }else{
                     		 newValues.add(val);
                     	 }
                 		 
                 	 }
                 	 HashMap<String, Object> listvalues = new HashMap<String, Object>();
                 	 if(ops.containsKey(newKey) && this.listOperatorMapping.containsKey(ops.get(newKey))){
                 		 
                 		 //get the operator
                 		 String curop = this.listOperatorMapping.get(ops.get(newKey));
                 		 
                 		 listvalues.put(curop, newValues);
                 		 tmpHash.put(newKey, listvalues);
                 	 }else{
                 		 tmpHash.put(newKey, newValues);
                 	 }  
                 	 
                 	
                  }else if(values.size()==1){
                 	 String val = (String)values.get(0);
                 	 if(val.endsWith("*")){
                 		 val = val.substring(0, val.length()-1);
                 		 System.out.println(val);
                 		 Pattern newVal = Pattern.compile("^"+val);
                 		 tmpHash.put(newKey, newVal);
                 	 }else if(val.startsWith("*")){
                 		 val = val.substring(1, val.length());
                 		 System.out.println(val);
                 		 Pattern newVal = Pattern.compile(val+"$");
                 		 newValues.add(newVal);
                 	 }else{
                 		 tmpHash.put(newKey, values.get(0));
                 	 }
                         
                  }
                     
              }
             
             if(!tmpHash.isEmpty()){
             	keyValueList.add(tmpHash);
             }
              
             
            
         }
 		
 		BasicDBObject query = new BasicDBObject();
 		ArrayList queryOp = (ArrayList)operators.getOperator();
 		
 		String op=null;
 		if( queryOp != null && !queryOp.isEmpty()){
 			op = (String)queryOp.get(0);
 		}else{
 			op = ReservedKeywords.RECORD_OPERATOR_DEFAULT;
 		}
 		
 		String mongoOp = "$and";
 		
 		if(op != null && !op.isEmpty()){
 			if(op.equalsIgnoreCase(ReservedKeywords.RECORD_OPERATOR_ANY)){
 				mongoOp = "$or";
 			}else if(op.equalsIgnoreCase(ReservedKeywords.RECORD_OPERATOR_ALL)){
 				mongoOp = "$and";
 			}
 		}
 		
 		if(!keyValueList.isEmpty()){
 			query.put(mongoOp, keyValueList);
 		}
 		
 		System.out.println(query);
 		return query;
 	}
 	
 	public Service getServiceByURI(String URI) throws DatabaseException{
 		int errorcode;
 		String errormsg;
 		
 		BasicDBObject query = new BasicDBObject();
 		query.put(ReservedKeywords.RECORD_URI, URI);
 		Service result=null;
 		
 		try{
 			DBCursor cur = coll.find(query);
 		
 			System.out.println("Came inside getServiceByURI");
 			
 			if (cur.size() == 1){
 				DBObject tmp = cur.next();
 				result = new Service(tmp.toMap());
 			}
 		}catch(MongoException e){
 			throw new DatabaseException(e.getMessage());
 		}
 			return result;
 	}
 	
 }
