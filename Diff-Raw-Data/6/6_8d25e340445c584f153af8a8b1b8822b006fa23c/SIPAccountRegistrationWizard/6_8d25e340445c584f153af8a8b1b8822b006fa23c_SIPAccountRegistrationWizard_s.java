 /*
  * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
  *
  * Distributable under LGPL license.
  * See terms of license at gnu.org.
  */
 package net.java.sip.communicator.plugin.sipaccregwizz;
 
 import java.awt.*;
 import java.util.*;
 
 import net.java.sip.communicator.service.gui.*;
 import net.java.sip.communicator.service.protocol.*;
 import net.java.sip.communicator.util.*;
 
 import org.osgi.framework.*;
 
 /**
  * The <tt>IPPIAccountRegistrationWizard</tt> is an implementation of the
  * <tt>AccountRegistrationWizard</tt> for the SIP protocol. It should allow
  * the user to create and configure a new SIP account.
  *
  * @author Yana Stamcheva
  */
 public class SIPAccountRegistrationWizard
     implements AccountRegistrationWizard
 {
     private FirstWizardPage firstWizardPage;
 
     private SIPAccountRegistration registration
         = new SIPAccountRegistration();
 
     private WizardContainer wizardContainer;
 
     private ProtocolProviderService protocolProvider;
 
     private String propertiesPackage
         = "net.java.sip.communicator.plugin.sipaccregwizz";
 
     private boolean isModification;
 
     private static final Logger logger
         = Logger.getLogger(SIPAccountRegistrationWizard.class);
 
     /**
      * Creates an instance of <tt>IPPIAccountRegistrationWizard</tt>.
      * @param wizardContainer the wizard container, where this wizard
      * is added
      */
     public SIPAccountRegistrationWizard(WizardContainer wizardContainer) {
         this.wizardContainer = wizardContainer;
     }
 
     /**
      * Implements the <code>AccountRegistrationWizard.getIcon</code> method.
      * Returns the icon to be used for this wizard.
      * @return byte[]
      */
     public byte[] getIcon() {
         return Resources.getImage(Resources.SIP_LOGO);
     }
 
     /**
      * Implements the <code>AccountRegistrationWizard.getPageImage</code> method.
      * Returns the image used to decorate the wizard page
      *
      * @return byte[] the image used to decorate the wizard page
      */
     public byte[] getPageImage()
     {
         return Resources.getImage(Resources.PAGE_IMAGE);
     }
 
 
     /**
      * Implements the <code>AccountRegistrationWizard.getProtocolName</code>
      * method. Returns the protocol name for this wizard.
      * @return String
      */
     public String getProtocolName() {
         return Resources.getString("protocolName");
     }
 
     /**
      * Implements the <code>AccountRegistrationWizard.getProtocolDescription
      * </code> method. Returns the description of the protocol for this wizard.
      * @return String
      */
     public String getProtocolDescription() {
         return Resources.getString("protocolDescription");
     }
 
     /**
      * Returns the set of pages contained in this wizard.
      * @return Iterator
      */
     public Iterator getPages() {
         ArrayList pages = new ArrayList();
         firstWizardPage = new FirstWizardPage(this);
 
         pages.add(firstWizardPage);
 
         return pages.iterator();
     }
 
     /**
      * Returns the set of data that user has entered through this wizard.
      * @return Iterator
      */
     public Iterator getSummary() {
         Hashtable summaryTable = new Hashtable();
 
         boolean rememberPswd = new Boolean(registration.isRememberPassword())
             .booleanValue();
 
         String rememberPswdString;
         if(rememberPswd)
             rememberPswdString = Resources.getString("yes");
         else
             rememberPswdString = Resources.getString("no");
 
         summaryTable.put(Resources.getString("uin"),
                 registration.getUin());
         summaryTable.put(Resources.getString("rememberPassword"),
                 rememberPswdString);
         summaryTable.put(Resources.getString("registrar"),
                 registration.getServerAddress());
         summaryTable.put(Resources.getString("serverPort"),
                 registration.getServerPort());
         summaryTable.put(Resources.getString("proxy"),
                 registration.getProxy());
         summaryTable.put(Resources.getString("proxyPort"),
                 registration.getProxyPort());
         summaryTable.put(Resources.getString("preferredTransport"),
                 registration.getPreferredTransport());
         
         if (registration.isEnablePresence()) {
             summaryTable.put(Resources.getString("enablePresence"),
                 Resources.getString("yes"));
         } else {
             summaryTable.put(Resources.getString("enablePresence"),
                     Resources.getString("no"));
         }
         if (registration.isForceP2PMode()) {
             summaryTable.put(Resources.getString("forceP2PPresence"),
                     Resources.getString("yes"));
         } else {
             summaryTable.put(Resources.getString("forceP2PPresence"),
                     Resources.getString("no"));
         }
         summaryTable.put(Resources.getString("offlineContactPollingPeriod"),
                 registration.getPollingPeriod());
         summaryTable.put(Resources.getString("subscriptionExpiration"),
                 registration.getSubscriptionExpiration());
 
         return summaryTable.entrySet().iterator();
     }
 
     /**
      * Installs the account created through this wizard.
      * @return ProtocolProviderService
      */
     public ProtocolProviderService finish() {
         //firstWizardPage = null;
         ProtocolProviderFactory factory
             = SIPAccRegWizzActivator.getSIPProtocolProviderFactory();
 
         ProtocolProviderService pps = null;
         if (factory != null)
             pps = this.installAccount(factory,
                 registration.getUin(), registration.getPassword());
 
         return pps;
     }
 
     /**
      * Creates an account for the given user and password.
      * @param providerFactory the ProtocolProviderFactory which will create
      * the account
      * @param user the user identifier
      * @param passwd the password
      * @return the <tt>ProtocolProviderService</tt> for the new account.
      */
     public ProtocolProviderService installAccount(
             ProtocolProviderFactory providerFactory,
             String user,
             String passwd)
     {
         Hashtable accountProperties = new Hashtable();
 
         if(registration.isRememberPassword()) {
             accountProperties.put(ProtocolProviderFactory.PASSWORD, passwd);
         }
 
         accountProperties.put(ProtocolProviderFactory.SERVER_ADDRESS,
                                   registration.getServerAddress());
 
         accountProperties.put(ProtocolProviderFactory.SERVER_PORT,
                 registration.getServerPort());
 
         accountProperties.put(ProtocolProviderFactory.PROXY_ADDRESS,
                 registration.getProxy());
 
         accountProperties.put(ProtocolProviderFactory.PROXY_PORT,
                 registration.getProxyPort());
 
         accountProperties.put(ProtocolProviderFactory.PREFERRED_TRANSPORT,
                 registration.getPreferredTransport());
 
         accountProperties.put(ProtocolProviderFactory.IS_PRESENCE_ENABLED,
                 Boolean.toString(registration.isEnablePresence()));
 
         accountProperties.put(ProtocolProviderFactory.FORCE_P2P_MODE,
                 Boolean.toString(registration.isForceP2PMode()));
 
         accountProperties.put(ProtocolProviderFactory.POLLING_PERIOD,
                 registration.getPollingPeriod());
 
         accountProperties.put(ProtocolProviderFactory.SUBSCRIPTION_EXPIRATION,
                 registration.getSubscriptionExpiration());
 
        accountProperties.put(ProtocolProviderFactory.PROTOCOL,
            "YOHOHOOHOHOH");

        accountProperties.put(ProtocolProviderFactory.PROTOCOL_ICON_PATH,
            "resources/images/protocol/sip/yohoho");

         if(isModification)
         {
             providerFactory.uninstallAccount(protocolProvider.getAccountID());
             this.protocolProvider = null;
             this.isModification  = false;
         }
 
         try {
             AccountID accountID = providerFactory.installAccount(
                     user, accountProperties);
 
             ServiceReference serRef = providerFactory
                 .getProviderForAccount(accountID);
 
             protocolProvider
                 = (ProtocolProviderService) SIPAccRegWizzActivator.bundleContext
                     .getService(serRef);
 
         }
         catch (IllegalArgumentException exc)
         {
             SIPAccRegWizzActivator.getUIService().getPopupDialog()
                 .showMessagePopupDialog(exc.getMessage(),
                     Resources.getString("error"),
                     PopupDialog.ERROR_MESSAGE);
         }
         catch (IllegalStateException exc)
         {
             SIPAccRegWizzActivator.getUIService().getPopupDialog()
                 .showMessagePopupDialog(exc.getMessage(),
                     Resources.getString("error"),
                     PopupDialog.ERROR_MESSAGE);
         }
 
 
         return protocolProvider;
     }
 
     /**
      * Fills the UIN and Password fields in this panel with the data coming
      * from the given protocolProvider.
      * @param protocolProvider The <tt>ProtocolProviderService</tt> to load the
      * data from.
      */
     public void loadAccount(ProtocolProviderService protocolProvider)
     {
         this.isModification = true;
 
         this.protocolProvider = protocolProvider;
 
         this.registration = new SIPAccountRegistration();
 
         this.firstWizardPage.loadAccount(protocolProvider);
     }
 
     /**
      * Indicates if this wizard is opened for modification or for creating a
      * new account.
      * 
      * @return <code>true</code> if this wizard is opened for modification and
      * <code>false</code> otherwise.
      */
     public boolean isModification()
     {
         return isModification;
     }
 
     /**
      * Returns the wizard container, where all pages are added.
      * 
      * @return the wizard container, where all pages are added
      */
     public WizardContainer getWizardContainer()
     {
         return wizardContainer;
     }
 
     /**
      * Returns the registration object, which will store all the data through
      * the wizard.
      * 
      * @return the registration object, which will store all the data through
      * the wizard
      */
     public SIPAccountRegistration getRegistration()
     {
         return registration;
     }
     
     /**
      * Returns the size of this wizard.
      * @return the size of this wizard
      */
     public Dimension getSize()
     {
         return new Dimension(600, 500);
     }
     
     /**
      * Returns the identifier of the page to show first in the wizard.
      * @return the identifier of the page to show first in the wizard.
      */
     public Object getFirstPageIdentifier()
     {
         return firstWizardPage.getIdentifier();
     }
 
     /**
      * Returns the identifier of the page to show last in the wizard.
      * @return the identifier of the page to show last in the wizard.
      */
     public Object getLastPageIdentifier()
     {
         return firstWizardPage.getIdentifier();
     }
 
     /**
      * Sets the modification property to indicate if this wizard is opened for
      * a modification.
      * 
      * @param isModification indicates if this wizard is opened for modification
      * or for creating a new account. 
      */
     public void setModification(boolean isModification)
     {
         this.isModification = isModification;
     }
 }
