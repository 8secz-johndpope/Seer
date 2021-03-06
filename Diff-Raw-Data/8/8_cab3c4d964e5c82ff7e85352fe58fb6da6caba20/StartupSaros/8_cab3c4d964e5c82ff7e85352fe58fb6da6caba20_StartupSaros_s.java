 package de.fu_berlin.inf.dpp;
 
 import org.apache.log4j.Logger;
 import org.eclipse.ui.IStartup;
 import org.eclipse.ui.PlatformUI;
 import org.eclipse.ui.intro.IIntroManager;
 import org.eclipse.ui.intro.IIntroPart;
 import org.picocontainer.annotations.Inject;
 
 import de.fu_berlin.inf.dpp.accountManagement.XMPPAccountStore;
 import de.fu_berlin.inf.dpp.annotations.Component;
 import de.fu_berlin.inf.dpp.stf.server.STFController;
 import de.fu_berlin.inf.dpp.ui.SarosUI;
import de.fu_berlin.inf.dpp.ui.util.WizardUtils;
 import de.fu_berlin.inf.dpp.util.Utils;
 
 /**
  * An instance of this class is instantiated when Eclipse starts, after the
  * Saros plugin has been started.
  * 
  * {@link #earlyStartup()} is called after the workbench is initialized. <br>
  * <br>
  * Checks whether the release number changed.
  * 
  * @author Lisa Dohrmann, Sandor Szücs
  */
 @Component(module = "integration")
 public class StartupSaros implements IStartup {
 
     private static final Logger log = Logger.getLogger(StartupSaros.class);
 
     @Inject
     private Saros saros;
 
     @Inject
     private SarosUI sarosUI;
 
     @Inject
     private XMPPAccountStore xmppAccountStore;
 
     public StartupSaros() {
         SarosPluginContext.reinject(this);
     }
 
     /*
      * Once the workbench is started, the method earlyStartup() will be called
      * from a separate thread
      */
 
     public void earlyStartup() {
 
        if (xmppAccountStore.isEmpty()) {
             showSarosView();
        }
 
         Integer port = Integer.getInteger("de.fu_berlin.inf.dpp.testmode");
 
         if (port != null && port > 0 && port <= 65535) {
             log.info("starting  RMI bot listen on port " + port);
             startRmiBot(port);
 
         } else if (port != null) {
             log.error("could not start RMI bot, port " + port
                 + " is not a valid port number");
         } else {
             /*
              * Only show configuration wizard if no accounts are configured. If
              * Saros is already configured, do not show the tutorial because the
              * user is probably already experienced.
              */
 
             handleStartup(xmppAccountStore.isEmpty());
         }
     }
 
     private void handleStartup(boolean showConfigurationWizard) {
         if (showConfigurationWizard) {
             Utils.runSafeSWTAsync(log, new Runnable() {
                 @Override
                 public void run() {
                     Utils.openInternalBrowser(Messages.Saros_tutorial_url,
                         Messages.Saros_tutorial_title);
                 }
             });
            WizardUtils.openSarosConfigurationWizard();
         }
     }
 
     private void startRmiBot(final int port) {
 
         Utils.runSafeAsync("RmiSWTWorkbenchBot-", log, new Runnable() {
             public void run() {
                 try {
                     STFController.start(port, saros);
                 } catch (Exception e) {
                     log.error("starting RMI bot failed", e);
                 }
             }
         });
     }
 
     private void showSarosView() {
         Utils.runSafeSWTAsync(log, new Runnable() {
             public void run() {
                 IIntroManager m = PlatformUI.getWorkbench().getIntroManager();
                 IIntroPart i = m.getIntro();
                 /*
                  * if there is a welcome screen, don't activate the SarosView
                  * because it would be maximized and hiding the workbench window
                  */
                 if (i == null)
                     sarosUI.openSarosView();
             }
         });
     }
 }
