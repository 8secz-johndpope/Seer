 package edu.cornell.mannlib.vitro.webapp.controller.edit;
 
 import java.io.IOException;
 import java.io.StringReader;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Set;
 
 import javax.servlet.ServletException;
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 import javax.servlet.http.HttpSession;
 
 import org.apache.commons.httpclient.HttpStatus;
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 
 import com.hp.hpl.jena.ontology.OntModel;
 import com.hp.hpl.jena.rdf.model.Model;
 import com.hp.hpl.jena.shared.Lock;
 
 import edu.cornell.mannlib.vedit.beans.LoginFormBean;
 import edu.cornell.mannlib.vitro.webapp.controller.freemarker.FreeMarkerHttpServlet;
 import edu.cornell.mannlib.vitro.webapp.dao.jena.DependentResourceDeleteJena;
 import edu.cornell.mannlib.vitro.webapp.dao.jena.event.EditEvent;
 import edu.cornell.mannlib.vitro.webapp.edit.n3editing.EditN3Utils;
 
 public class PrimitiveRdfEdit extends FreeMarkerHttpServlet{
 
 	private static final long serialVersionUID = 1L;
 
 	@Override
 	protected String getBody() {
 //		boolean loggedIn = checkLoginStatus(request, response);
 //		if( !loggedIn){
 //			doError(response,"You must be logged in to use this servlet.",HttpStatus.SC_UNAUTHORIZED);
 //			return;
 //		}
		return mergeBodyToTemplate("primitiveRdfEdit.ftl",new HashMap());
 	}
 
 	@Override
 	protected String getTitle() {
 		return "RDF edit";
 	}
 
 	@Override
 	public void doPost(HttpServletRequest request,
 			HttpServletResponse response) throws ServletException, IOException {
 		
 		boolean loggedIn = checkLoginStatus(request, response);
 		if( !loggedIn){
 			doError(response,"You must be logged in to use this servlet.",HttpStatus.SC_UNAUTHORIZED);
 			return;
 		}
 		
 //        PolicyIface policy = RequestPolicyList.getPolicies( request );
 //        
 //        if( policy == null || ( policy instanceof PolicyList && ((PolicyList)policy).size() == 0 )){
 //            policy = ServletPolicyList.getPolicies( getServletContext() );
 //            if( policy == null || ( policy instanceof PolicyList && ((PolicyList)policy).size() == 0 )){            
 //                log.debug("No policy found in request at " + RequestPolicyList.POLICY_LIST);
 //                doError(response, "no policy found.",500);
 //                return;                
 //            }
 //        }              
 //        
 //        IdentifierBundle ids = (IdentifierBundle)ServletIdentifierBundleFactory
 //            .getIdBundleForRequest(request,request.getSession(false),getServletContext());
 //        
 //        if( ids == null ){
 //            log.error("No IdentifierBundle objects for request");
 //            doError(response,"no identifiers found",500);
 //            return;
 //        }
         
         /* Predefined values for RdfFormat are "RDF/XML", 
          * "N-TRIPLE", "TURTLE" (or "TTL") and "N3". null represents 
          * the default language, "RDF/XML". "RDF/XML-ABBREV" is a synonym for "RDF/XML" */
 		String format = request.getParameter("RdfFormat");		
 		if( format == null )		    
			format = "N3";		
 		if ( ! ("N-TRIPLE".equals(format) || "TURTLE".equals(format) || "TTL".equals(format)
 			    || "N3".equals(format)|| "RDF/XML-ABBREV".equals(format) || "RDF/XML".equals(format) )){
 			doError(response,"RdfFormat was not recoganized.",500);
 			return;
 		}
 		
 		//parse RDF	
 		Set<Model> additions= null;
 		try {
 			additions = parseRdfParam(request.getParameterValues("additions"),format);
 		} catch (Exception e) {
 			doError(response,"Error reading RDF, set log level to debug for this class to get error messages in the server logs.",HttpStatus.SC_BAD_REQUEST);
 			return;
 		}
 						
 		Set<Model> retractions = null;
 		try {
 			retractions = parseRdfParam(request.getParameterValues("retractions"),format);
 		} catch (Exception e) {
 			doError(response,"Error reading RDF, set log level to debug for this class to get error messages in the server logs.",HttpStatus.SC_BAD_REQUEST);
 			return;
 		}
 		
 		//check permissions		
 		//TODO: (bdc34)This is not yet implemented, must check the IDs against the policies for permissons before doing an edit!
 		boolean hasPermission = true;
 		
 		if( !hasPermission ){
 			//if not okay, send error message
 			doError(response,"Insufficent permissions.",HttpStatus.SC_UNAUTHORIZED);
 		}
 		
 		if( hasPermission ){
 			String editorUri = EditN3Utils.getEditorUri(vreq,request.getSession(false),getServletContext());			
 			try {
 				processChanges(request, additions, retractions, getWriteModel(request),getQueryModel(request), editorUri);
 			} catch (Exception e) {
 				doError(response,e.getMessage(),HttpStatus.SC_INTERNAL_SERVER_ERROR);
 			}			
 		}
 	}
 	
 	protected void processChanges( HttpServletRequest request, Set<Model> additions, Set<Model> retractions, OntModel writeModel, OntModel queryModel, String editorURI ) throws Exception{
 		Model a = com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel();
 		for(Model m : additions)
 			a.add(m);
 		
 		Model r = com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel();
 		for(Model m : retractions)
 			r.add(m);
 		
 		procesChanges(request,a,r,writeModel,queryModel,editorURI);		
 	}
 	
 	protected void procesChanges(HttpServletRequest request, Model additions, Model retractions, OntModel writeModel, OntModel queryModel, String editorURI ) throws Exception{		
     /*
      * Do a diff on the additions and retractions and then only add the delta to the jenaOntModel.
      */            
     Model assertionsAdded = additions.difference( retractions );    
     Model assertionsRetracted = retractions.difference( additions );
         
   	Model depResRetractions = 
    		DependentResourceDeleteJena
    		  .getDependentResourceDeleteForChange(assertionsAdded, assertionsRetracted, queryModel);
   	assertionsRetracted.add( depResRetractions );                     
      
     Lock lock = null;
     try{
         lock =  writeModel.getLock();
         lock.enterCriticalSection(Lock.WRITE);
         writeModel.getBaseModel().notifyEvent(new EditEvent(editorURI,true));   
         writeModel.add( assertionsAdded );
         writeModel.remove( assertionsRetracted );
     }catch(Throwable t){
     	throw new Exception("Error while modifying model \n" + t.getMessage());    	
     }finally{
         writeModel.getBaseModel().notifyEvent(new EditEvent(editorURI,false));
         lock.leaveCriticalSection();
     }
     
 }
 
 	/**
 	 * Convert the values from a parameters into RDF models.
 	 * @param parameters - the result of request.getParameters(String)
 	 * @param format - a valid format string for Jena's Model.read()
 	 * @return 
 	 * @throws Exception
 	 */
 	protected Set<Model> parseRdfParam(String[] parameters, String format) throws Exception{
 		Set<Model> models = new HashSet<Model>();				
 		for( String param : parameters){
 			try{
 				StringReader reader = new StringReader(param);
 				Model model = com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel();			
 				model.read(reader, null, format);
 			}catch(Error ex){
 				log.error("Error reading RDF as " + format + " in " + param);
 				throw new Exception("Error reading RDF, set log level to debug for this class to get error messages in the sever logs.");
 			}
 		}
 		return models;
 	}
 	
 	private void doError(HttpServletResponse response, String errorMsg,int httpstatus){
 		response.setStatus(httpstatus);
 		try {
 			response.getWriter().write(errorMsg);
 		} catch (IOException e) {
 			log.debug("IO exception during output",e );
 		}
 	}
 	
 	protected OntModel getWriteModel(HttpServletRequest request){
 		HttpSession session = request.getSession(false);
 		if( session == null || session.getAttribute("jenaOntModel") == null )
 			return (OntModel)getServletContext().getAttribute("jenaOntModel");
 		else
			return (OntModel)session.getAttribute("jenaOntModel");
 	}
 	
 	protected OntModel getQueryModel(HttpServletRequest request){
 		return getWriteModel(request);
 	}
 	
 	Log log = LogFactory.getLog(PrimitiveRdfEdit.class.getName());
 
 
 	static public boolean checkLoginStatus(HttpServletRequest request, HttpServletResponse response){
         LoginFormBean loginBean = (LoginFormBean) request.getSession().getAttribute("loginHandler");        
         if (loginBean == null){            
         	return false;            
         } else {                    	
         	return true;
         }
 	}
 
 
 }
