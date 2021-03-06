 package org.collectionspace.chain.csp.schema;
 
 import org.collectionspace.chain.csp.config.ReadOnlySection;
 
 public class EmailData {
 
 	String baseurl,fromaddress,toaddress ;
 	String smtphost,smtpport,smtppass,smtpuser;
 	Boolean smtpdebug,smtpauth;
 	String pswdmsg, pswdsubj;
 	
 /* 	<email>
 		<baseurl>hendecasyllabic.local:8180</baseurl>
 		<from>csm22@caret.cam.ac.uk</from>
 		<to></to><!-- if specified then all emails will send to this address - used for debugging -->
 		<smtp>
 			<host>localhost</host>
 			<port>25</port>
 			<debug>false</debug>
 			<auth enabled="false"> <!-- set to true if wish to use auth -->
 				<username></username>
 				<password></password>
 			</auth>
 		</smtp>
 		<passwordreset>
 			<subject>CollectionSpace Password reset request</subject>
 			<message>A password reset has been requested from this email. If you wish to reset your password please click on this link {{link}}.</message>
 		</passwordreset>
 	</email> 
 
 */
 	
 	public EmailData(Spec spec, ReadOnlySection section) {
 		baseurl=(String)section.getValue("/baseurl");
 		fromaddress=(String)section.getValue("/from");
 		toaddress=(String)section.getValue("/to");
 		smtphost = (String)section.getValue("/smtp/host");
 		smtpport = (String)section.getValue("/smtp/port");
		smtpdebug = (Boolean)section.getValue("/smtp/debug");
		smtpauth = (Boolean)section.getValue("smtp/auth/@enabled");
		smtppass = (String)section.getValue("smtp/auth/password");
		smtpuser = (String)section.getValue("smtp/auth/username");
 		pswdmsg = (String)section.getValue("/passwordreset/message");
 		pswdsubj = (String)section.getValue("/passwordreset/subject");
 	}
 	
 
 	public String getBaseURL() { return baseurl; }
 	public String getFromAddress() { return fromaddress; }
 	public String getToAddress() { return toaddress; }
 
 	public String getSMTPPort() { return smtpport; }
 	public String getSMTPHost() { return smtphost; }
 	public Boolean doSMTPDebug() { return smtpdebug; }
 
 	public String getPasswordResetMessage() { return pswdmsg; }
 	public String getPasswordResetSubject() { return pswdsubj; }
 	
 	public Boolean doSMTPAuth() { return smtpauth; }
 	public String getSMTPAuthPassword() { if(smtpauth){ return smtppass;} else {return null;} }
 	public String getSMTPAuthUsername() { if(smtpauth){ return smtpuser;} else {return null;} }
 	
 	public EmailData getEmailData() { return this; }
 	
 }
