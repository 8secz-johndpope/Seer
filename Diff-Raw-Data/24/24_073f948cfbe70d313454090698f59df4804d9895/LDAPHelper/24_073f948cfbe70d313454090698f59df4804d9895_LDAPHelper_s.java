 /*
  * Created on Nov 11, 2004
  *
  * TODO To change the template for this generated file go to
  * Window - Preferences - Java - Code Style - Code Templates
  */
 package gov.nih.nci.security.authentication.helper;
 
 /**
  *
  *<!-- LICENSE_TEXT_START -->
  *
  *The NCICB Common Security Module (CSM) Software License, Version 3.0 Copyright
  *2004-2005 Ekagra Software Technologies Limited ('Ekagra')
  *
  *Copyright Notice.  The software subject to this notice and license includes both
  *human readable source code form and machine readable, binary, object code form
  *(the 'CSM Software').  The CSM Software was developed in conjunction with the
  *National Cancer Institute ('NCI') by NCI employees and employees of Ekagra.  To
  *the extent government employees are authors, any rights in such works shall be
  *subject to Title 17 of the United States Code, section 105.    
  *
  *This CSM Software License (the 'License') is between NCI and You.  'You (or
  *'Your') shall mean a person or an entity, and all other entities that control,
  *are controlled by, or are under common control with the entity.  'Control' for
  *purposes of this definition means (i) the direct or indirect power to cause the
  *direction or management of such entity, whether by contract or otherwise, or
  *(ii) ownership of fifty percent (50%) or more of the outstanding shares, or
  *(iii) beneficial ownership of such entity.  
  *
  *This License is granted provided that You agree to the conditions described
  *below.  NCI grants You a non-exclusive, worldwide, perpetual, fully-paid-up,
  *no-charge, irrevocable, transferable and royalty-free right and license in its
  *rights in the CSM Software to (i) use, install, access, operate, execute, copy,
  *modify, translate, market, publicly display, publicly perform, and prepare
  *derivative works of the CSM Software; (ii) distribute and have distributed to
  *and by third parties the CSM Software and any modifications and derivative works
  *thereof; and (iii) sublicense the foregoing rights set out in (i) and (ii) to
  *third parties, including the right to license such rights to further third
  *parties.  For sake of clarity, and not by way of limitation, NCI shall have no
  *right of accounting or right of payment from You or Your sublicensees for the
  *rights granted under this License.  This License is granted at no charge to You.
  *
  *1.	Your redistributions of the source code for the Software must retain the
  *above copyright notice, this list of conditions and the disclaimer and
  *limitation of liability of Article 6 below.  Your redistributions in object code
  *form must reproduce the above copyright notice, this list of conditions and the
  *disclaimer of Article 6 in the documentation and/or other materials provided
  *with the distribution, if any.
  *2.	Your end-user documentation included with the redistribution, if any, must
  *include the following acknowledgment: 'This product includes software developed
  *by Ekagra and the National Cancer Institute.'  If You do not include such
  *end-user documentation, You shall include this acknowledgment in the Software
  *itself, wherever such third-party acknowledgments normally appear.
  *
  *3.	You may not use the names 'The National Cancer Institute', 'NCI' 'Ekagra
  *Software Technologies Limited' and 'Ekagra' to endorse or promote products
  *derived from this Software.  This License does not authorize You to use any
  *trademarks, service marks, trade names, logos or product names of either NCI or
  *Ekagra, except as required to comply with the terms of this License.
  *
  *4.	For sake of clarity, and not by way of limitation, You may incorporate this
  *Software into Your proprietary programs and into any third party proprietary
  *programs.  However, if You incorporate the Software into third party proprietary
  *programs, You agree that You are solely responsible for obtaining any permission
  *from such third parties required to incorporate the Software into such third
  *party proprietary programs and for informing Your sublicensees, including
  *without limitation Your end-users, of their obligation to secure any required
  *permissions from such third parties before incorporating the Software into such
  *third party proprietary software programs.  In the event that You fail to obtain
  *such permissions, You agree to indemnify NCI for any claims against NCI by such
  *third parties, except to the extent prohibited by law, resulting from Your
  *failure to obtain such permissions.
  *
  *5.	For sake of clarity, and not by way of limitation, You may add Your own
  *copyright statement to Your modifications and to the derivative works, and You
  *may provide additional or different license terms and conditions in Your
  *sublicenses of modifications of the Software, or any derivative works of the
  *Software as a whole, provided Your use, reproduction, and distribution of the
  *Work otherwise complies with the conditions stated in this License.
  *
  *6.	THIS SOFTWARE IS PROVIDED 'AS IS,' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
  *(INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY,
  *NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE) ARE DISCLAIMED.  IN NO
  *EVENT SHALL THE NATIONAL CANCER INSTITUTE, EKAGRA, OR THEIR AFFILIATES BE LIABLE
  *FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
  *DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
  *SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
  *CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
  *TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
  *THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  *
  *<!-- LICENSE_TEXT_END -->
  *
  */
 
 
 import gov.nih.nci.security.authentication.principal.EmailIdPrincipal;
 import gov.nih.nci.security.authentication.principal.FirstNamePrincipal;
 import gov.nih.nci.security.authentication.principal.LastNamePrincipal;
 import gov.nih.nci.security.authentication.principal.LoginIdPrincipal;
 import gov.nih.nci.security.constants.Constants;
 import gov.nih.nci.security.exceptions.internal.CSInternalConfigurationException;
 import gov.nih.nci.security.exceptions.internal.CSInternalInsufficientAttributesException;
 import gov.nih.nci.security.exceptions.internal.CSInternalLoginException;
 
 import java.security.Security;
 import java.util.Hashtable;
 
 import javax.naming.Context;
 import javax.naming.NamingEnumeration;
 import javax.naming.NamingException;
 import javax.naming.directory.Attribute;
 import javax.naming.directory.Attributes;
 import javax.naming.directory.DirContext;
 import javax.naming.directory.InitialDirContext;
 import javax.naming.directory.SearchControls;
 import javax.naming.directory.SearchResult;
 import javax.security.auth.Subject;
 
 import org.apache.log4j.Logger;
 
 /**
  * This is a helper class which is used to perform all LDAP operations like
  * connecting to the LDAP server, executing the LDAP queries etc. This is a static class and
  * provides a single helper method.
  * 
  * @author Kunal Modi (Ekagra Software Technologies Ltd.)
  */
 public class LDAPHelper {
 
 	private static final Logger log = Logger.getLogger(LDAPHelper.class);
 	
 	/**
 	 * Default Private Class Constructor
 	 *  
 	 */
 	private LDAPHelper() {
 	}
 
 	/**
 	 * Accepts the connection properties as well as the user id and password. It
 	 * opens a connection to the database and fires a the query to find. If the
 	 * query was successful then it returns TRUE else it returns FALSE
 	 * 
 	 * @param connectionProperties table containing details for establishing connection like the 
 	 * the url of the ldap server and the seach base which is to be used as the starting 
 	 * point
 	 * @param userID the user entered user name provided by the calling application
 	 * @param password the user entered password provided by the calling application
 	 * @param subject it is the JAAS Subject which is used for 
 	 * @return TRUE if the authentication was sucessful using the provided user
 	 * credentials and FALSE if the authentication fails
 	 * @throws CSInternalConfigurationException 
 	 * @throws CSInternalLoginException 
 	 * @throws CSInternalInsufficientAttributesException 
 	 */
 	public static boolean authenticate(Hashtable connectionProperties, String userID, char[] password, Subject subject) throws CSInternalConfigurationException, CSInternalLoginException, CSInternalInsufficientAttributesException {
 		Hashtable environment = new Hashtable();
 		setLDAPEnvironment(environment, connectionProperties);
 		return ldapAuthenticateUser(environment, connectionProperties, userID, new String(password), subject);
 	}
 
 	/**
 	 * This methods clears and reloads the enviroment variables from the connection properties
 	 * supplied. It loads the Initial context, provider URL and the connection details.
 	 * @param environment This
 	 * @param connectionProperties
 	 */
 	private static void setLDAPEnvironment(Hashtable environment, Hashtable connectionProperties) {
 		Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
 
 		environment.clear();
 		environment.put(Context.INITIAL_CONTEXT_FACTORY, Constants.INITIAL_CONTEXT);
 		environment.put(Context.PROVIDER_URL, connectionProperties.get(Constants.LDAP_HOST));
 		environment.put(Context.SECURITY_AUTHENTICATION, "simple");
 		//if (((String)connectionProperties.get(Constants.LDAP_HOST)).contains("ldaps")) // removed to make it JDK 1.4 compatible
 		if (((String)connectionProperties.get(Constants.LDAP_HOST)).regionMatches(true, 0, "ldaps", 0, "ldaps".length()))
 			environment.put(Context.SECURITY_PROTOCOL, "ssl");
 		if (connectionProperties.get(Constants.LDAP_ADMIN_USER_NAME)!= null && ((String)connectionProperties.get(Constants.LDAP_ADMIN_USER_NAME)).length() != 0)
 			environment.put(Context.SECURITY_PRINCIPAL, connectionProperties.get(Constants.LDAP_ADMIN_USER_NAME));
 		if (connectionProperties.get(Constants.LDAP_ADMIN_PASSWORD)!= null && ((String)connectionProperties.get(Constants.LDAP_ADMIN_PASSWORD)).length() != 0)
 			environment.put(Context.SECURITY_CREDENTIALS, connectionProperties.get(Constants.LDAP_ADMIN_PASSWORD));
 		
 		if (log.isDebugEnabled())
 			log.debug("Authentication|||setLDAPEnvironment|Success| Set the LDAP Environment Properties |" + connectionProperties.get(Constants.LDAP_HOST));			
 	}
 
 	/**
 	 * This method returns the the Fully Distinguished Name obtained from the
 	 * Directory Server for the given user id. It accepts the evironment variables
 	 * and connection properties to connect to the LDAP server. It then obtains the 
 	 * Fully Distinguished User Name for the user id provided from the LDAP server
 	 * 
 	 * @param environment The environment variables which are used to connect to LDAP
 	 * @param connectionProperties The LDAP url and search base used to point to LDAP
 	 * @param userName The user name which is to be authenticated
 	 * @return The Fully Distinguished User Name obtained from the LDAP for the passed user name
 	 * @throws CSInternalLoginException 
 	 * @throws CSInternalConfigurationException 
 	 */
 	private static String getFullyDistinguishedName(Hashtable environment, Hashtable connectionProperties, String userName) throws CSInternalLoginException, CSInternalConfigurationException {
 		String[] attributeIDs = { (String) connectionProperties.get(Constants.LDAP_USER_ID_LABEL) }; //{"dn"} ;
 		String searchFilter = "(" + (String) connectionProperties.get(Constants.LDAP_USER_ID_LABEL) + "=" + userName + ")";
 
 		DirContext dirContext = null;
 		try
 		{
 			dirContext = new InitialDirContext(environment);
 		}
 		catch (NamingException e1)
 		{
 			if (log.isDebugEnabled())
 			log.debug("Authentication||"+userName+"|getFullyDistinguishedName|Failure| Error connecting to the Directory Server for " + userName +"|"+ e1.getMessage());
 			throw new CSInternalConfigurationException("Error occured in connecting to the directory server");
 		}
 
 		SearchControls searchControls = new SearchControls();
 		searchControls.setReturningAttributes(attributeIDs);
 		searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
 
 		String fullyDistinguishedName = null;
 		NamingEnumeration searchEnum = null;
 		try
 		{
 			searchEnum = dirContext.search((String) connectionProperties.get(Constants.LDAP_SEARCHABLE_BASE), searchFilter, searchControls);
 		}
 		catch (NamingException e)
 		{
 			if (log.isDebugEnabled())
 				log.debug("Authentication||"+userName+"|getFullyDistinguishedName|Failure| Error obtaining the Distinguished name for " + userName +"|"+ e.getMessage());			
 			throw new CSInternalLoginException( "User Name doesnot exists");
 		}
 		try
 		{
 			dirContext.close();
 		}
 		catch (NamingException e)
 		{
 			if (log.isDebugEnabled())
 				log.debug("Authentication||"+userName+"|getFullyDistinguishedName|Failure| Error in closing the directory context for " + userName +"|"+ e.getMessage());			
 		}
 
 		try
 		{
 			while (searchEnum.hasMore()) {
 				SearchResult searchResult = (SearchResult) searchEnum.next();
 				fullyDistinguishedName = searchResult.getName()	+ "," + (String) connectionProperties.get(Constants.LDAP_SEARCHABLE_BASE);
 				if (log.isDebugEnabled())
 					log.debug("Authentication||"+userName+"|getFullyDistinguishedName|Success| Obtained the Distinguished Name |" + fullyDistinguishedName);			
 				return fullyDistinguishedName;
 				}
 		}
 		catch (NamingException e)
 		{
 			if (log.isDebugEnabled())
 				log.debug("Authentication||"+userName+"|getFullyDistinguishedName|Failure| Error obtaining the Distinguished name for " + userName +"|"+ e.getMessage());			
 			throw new CSInternalLoginException( "User Name doesnot exists");
 		}
 		return null;
 	}
 
 	/**
 	 * Return the result of user authentication with LDAP server
 	 * 
 	 * @param loginName the login name of the user
 	 * @param passwd the password of the user
 	 * @return true for successful authentication <br>
 	 *         false for failed authentication
 	 * @throws CSInternalConfigurationException 
 	 * @throws CSInternalLoginException 
 	 * @throws CSInternalInsufficientAttributesException 
 	 * @throws CSInternalConfigurationException 
 	 */
 	private static boolean ldapAuthenticateUser(Hashtable environment, Hashtable connectionProperties, String userName, String password, Subject subject) throws CSInternalLoginException, CSInternalInsufficientAttributesException, CSInternalConfigurationException
 	{
 		String fullyDistinguishedName = getFullyDistinguishedName(environment, connectionProperties, userName);//connectionProperties.get(Constants.LDAP_USER_ID_LABEL) + "=" + userName + "," + connectionProperties.get(Constants.LDAP_SEARCHABLE_BASE);

 		if (null == fullyDistinguishedName) {
 			if (log.isDebugEnabled())
 				log.debug("Authentication||"+userName+"|ldapAuthenticateUser|Failure| Error obtaining the Distinguished Name|");
 			return false;
 		}

 		try 
 		{
 			environment.put(Context.SECURITY_PRINCIPAL, fullyDistinguishedName);
 			environment.put(Context.SECURITY_CREDENTIALS, password);
 			DirContext initialDircontext = new InitialDirContext(environment);			
 			if (   ((String)connectionProperties.get(Constants.USER_FIRST_NAME) != null && !((String)connectionProperties.get(Constants.USER_FIRST_NAME)).trim().equals(""))
 				&& ((String)connectionProperties.get(Constants.USER_LAST_NAME) != null 	&& !((String)connectionProperties.get(Constants.USER_LAST_NAME)).trim().equals(""))
 				&& ((String)connectionProperties.get(Constants.USER_EMAIL_ID) != null 	&& !((String)connectionProperties.get(Constants.USER_EMAIL_ID)).trim().equals("")))
 			{
 				Attributes attributes = initialDircontext.getAttributes(fullyDistinguishedName);
 				
 				Attribute firstName = attributes.get((String)connectionProperties.get(Constants.USER_FIRST_NAME));
 				if (null != firstName)
 					subject.getPrincipals().add(new FirstNamePrincipal((String)firstName.get()));
 				else
 					throw new CSInternalInsufficientAttributesException("User Attribute First Name not found");
 				
 				Attribute lastName = attributes.get((String)connectionProperties.get(Constants.USER_LAST_NAME));
 				if (null != lastName)
 					subject.getPrincipals().add(new LastNamePrincipal((String)lastName.get()));
 				else
 					throw new CSInternalInsufficientAttributesException("User Attribute Last Name not found");
 				
 				Attribute emailId = attributes.get((String)connectionProperties.get(Constants.USER_EMAIL_ID));
 				if (null != emailId)
 					subject.getPrincipals().add(new EmailIdPrincipal((String)emailId.get()));
 				else
 					throw new CSInternalInsufficientAttributesException("User Attribute Email Id not found");
 				
 				subject.getPrincipals().add(new LoginIdPrincipal(userName));
 
			}
			else if (  ((String)connectionProperties.get(Constants.USER_FIRST_NAME) == null || ((String)connectionProperties.get(Constants.USER_FIRST_NAME)).trim().equals(""))
					&& ((String)connectionProperties.get(Constants.USER_LAST_NAME) == null 	|| ((String)connectionProperties.get(Constants.USER_LAST_NAME)).trim().equals(""))
					&& ((String)connectionProperties.get(Constants.USER_EMAIL_ID) == null 	|| ((String)connectionProperties.get(Constants.USER_EMAIL_ID)).trim().equals("")))
			{
				// do nothing;
			}
			else
			{
 				throw new CSInternalConfigurationException("Login Failed : Improper Configuration, Unable to Retrieve User Attributes");
 			}
 			initialDircontext.close();
 			setLDAPEnvironment(environment, connectionProperties);
 			if (log.isDebugEnabled())
 				log.debug("Authentication||"+userName+"|ldapAuthenticateUser|Success| Login Successful for User " + userName + "|");
 			return true;
 		} catch (NamingException ne) {
 			if (log.isDebugEnabled())
 				log.debug("Authentication||"+userName+"|ldapAuthenticateUser|Failure| Login Failed for User " + userName + "|" + ne.getMessage());
 			throw new CSInternalLoginException("Login Failed : User Credentials Incorrect");
 		}
 	}
 }
