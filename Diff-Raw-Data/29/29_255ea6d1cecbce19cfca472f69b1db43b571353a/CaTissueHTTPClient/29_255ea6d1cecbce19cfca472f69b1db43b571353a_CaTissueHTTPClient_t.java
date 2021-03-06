 /**
  * <p>Title: CaTissueHTTPClient Class>
  * <p>Description:	This is the wrapper class over HTTP API that provides a 
  * functionality to APIs to connect& access the caTISSUE Core Application.</p>
  * Copyright:    Copyright (c) year
  * Company: Washington University, School of Medicine, St. Louis.
  * @author Aniruddha Phadnis
  * @version 1.00
  * Created on Dec 15, 2005
  */
 
 package edu.wustl.catissuecore.client;
 
 import java.io.ObjectInputStream;
 import java.io.ObjectOutputStream;
 import java.lang.reflect.Method;
 import java.net.URL;
 import java.net.URLConnection;
 import java.util.List;
 
 import edu.wustl.catissuecore.domain.User;
 import edu.wustl.catissuecore.util.global.Constants;
 import edu.wustl.common.util.global.ApplicationProperties;
 import edu.wustl.common.util.logger.Logger;
 
 public class CaTissueHTTPClient
 {
     private static CaTissueHTTPClient caTissueHTTPClient = new CaTissueHTTPClient();
     
     private static final String port = ApplicationProperties.getValue("server.port");
 
     private static final String servletURL="http://localhost:" + port + "/catissuecore/";
     
     private String httpSessionId;
     
     private String userName;
     
     public String getLoggedInUserName()
     {
         return userName;
     }
     
     private CaTissueHTTPClient()
     {
         
     }
     
     /**
      * This function returns instance of CaTissueHTTPClient
      * @return the CaTissueHTTPClient instance
      */
     public static CaTissueHTTPClient getInstance()
     {
         return caTissueHTTPClient;
     }
     
     /**
      * This function connects User to caTISSUE Core Application 
      * @param userName userName of the User to connect to caTISSUE Core Application
      * @param password password of the User to connect to caTISSUE Core Application
      * @return returns the status of login to caTISSUE Core Application
      */
 	public boolean connect(String userName, String password) throws Exception
 	{
 	    User user = new User();
 		user.setLoginName(userName);
 		user.setPassword(password);
 		
 		HTTPWrapperObject wrapperObject = new HTTPWrapperObject(user,Constants.LOGIN);
 	    
 		HTTPMessage httpMessage=(HTTPMessage)sendData(wrapperObject, servletURL+"LoginHTTP.do");
 		
 		if(Constants.SUCCESS.equals(httpMessage.getResponseStatus()))
 		{
 			httpSessionId = httpMessage.getSessionId();
 			this.userName = userName;
 			return true;
 		}
 		
 		return false;
 	}
 	
 	/**
 	 * This function disconnects User from caTISSUE Core Application
 	 * @return returns the status of logout to caTISSUE Core Application
 	 */
 	public boolean disConnect() throws Exception
 	{
 	    if(httpSessionId != null)
 	    {
 		    HTTPWrapperObject wrapperObject = new HTTPWrapperObject(null, Constants.LOGOUT);
 		    
 		    HTTPMessage httpMessage =(HTTPMessage)sendData(wrapperObject, servletURL+"LogoutHTTP.do;jsessionid="+httpSessionId);
 		    
 		    if(Constants.SUCCESS.equals(httpMessage.getResponseStatus()))
 			{
 				return true;
 			}
 	    }
 		
 		return false;
 	}
 	
 	/**
 	 * This function creates HTTPWrapperObject to send to caTISSUE Core Application for different operations and
 	 * returns updated Object of the caCORE List application or Exception Object containing exceptions while performing operation
 	 * @param domainObject the caCORE Like Object 
 	 * @param operation the operation to do on caCORE Like Object
 	 * @return the updated Object of the caCORE Like application or Exception Object containing exceptions while performing operation 
 	 */
 	private Object doOperation(Object domainObject,String operation) throws Exception
 	{
 	    HTTPWrapperObject wrapperObject = new HTTPWrapperObject(domainObject,operation);
 	    
 	    HTTPMessage httpMessage =(HTTPMessage)sendData(wrapperObject, servletURL+"OperationHTTP.do;jsessionid="+httpSessionId);
 	    
 	    //setting Id of the domainObject using Reflection if Add operatoin is Successfull
 	    if ( (operation.equals(Constants.ADD)) && (httpMessage.getResponseStatus().equals(Constants.SUCCESS))  && 
 	         (httpMessage.getDomainObjectId() != null) )
 	    {
 		    Class[] argClass=new Class[] {Long.class};
 		    
 		    Method setIdMethod=domainObject.getClass().getMethod("setId",argClass);
 		    
 		    Long[] arguments=new Long[]{httpMessage.getDomainObjectId()};
 		    
 		    setIdMethod.invoke(domainObject, arguments);
 	    }
 	    //creating Exception object containing response messages if operation fails
 	    else if(httpMessage.getResponseStatus().equals(Constants.FAILURE))
 	    {
 	        String exceptions=new String();
 	        
 	        List messageList = httpMessage.getMessageList();
 			
 			for(int i=0;i<messageList.size();i++)
 			{
 			    exceptions +=(String)messageList.get(i);
 			}
 	        
 	        throw new Exception(exceptions);
 	    }
 	    
 	    return domainObject;
 	}
 	
 	/**
 	 * This function opens a connection with caTISSUE Core Applicatoin and sends HTTPWrapperObject to do operation
 	 * and also returns response Object
 	 * @param wrapperObject the HTTPWrapperObject to send to caTISSUE Core Application
 	 * @param urlString the URL Connection string of caTISSUE Core Applicatoin
 	 * @return the response Object-HTTPMessage received from ResponseServlet 
 	 */
 	private Object sendData(HTTPWrapperObject wrapperObject, String urlString) throws Exception
 	{
 	    // Opens a connection to the caTissueCore Application URL
 		URL url = new URL(urlString);
 		URLConnection con = url.openConnection();
 		
 		con.setDoOutput(true);
 		con.setRequestProperty(Constants.CONTENT_TYPE,Constants.HTTP_API);
 		ObjectOutputStream objectOutStream = new ObjectOutputStream(con.getOutputStream());
 		
 				
 		objectOutStream.writeObject(wrapperObject);
 		objectOutStream.flush();
 		objectOutStream.close();
 		
 		ObjectInputStream objectInStream = new ObjectInputStream(con.getInputStream());
 		HTTPMessage httpMessage = (HTTPMessage)objectInStream.readObject();
 		
 		objectInStream.close();
 		
 		List messageList = httpMessage.getMessageList();
 		
 		for(int i=0;i<messageList.size();i++)
 		{
 		    Logger.out.debug("HTTPClient sendData-->" + messageList.get(i));
 		}
 		
 		return httpMessage;
 	}
 	
 	/**
 	 * Adds caCore Like domain object in the database.
 	 * @param domainObject the caCore Like object to add using HTTP API
 	 * @return returns the Added caCore Like object/Exception object if exception occurs performing Add operation
 	 */
 	public Object add(Object domainObject) throws Exception
 	{
 		return doOperation(domainObject,Constants.ADD);
 	}
 	
 	/**
 	 * Edits caCore Like domain object in the database.
 	 * @param domainObject the caCore Like object to edit using HTTP API
 	 * @return returns the Edited caCore Like object/Exception object if exception occurs performing Edit operation
 	 */
 	public Object edit(Object domainObject) throws Exception
 	{
 		return doOperation(domainObject,Constants.EDIT);
 	}
 	
	public String getHttpSessionId()
	{
		return httpSessionId;
	}
	public void setHttpSessionId(String httpSessionId)
	{
		this.httpSessionId = httpSessionId;
	}
 }
