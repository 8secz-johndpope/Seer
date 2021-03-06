 /*
  * DPP - Serious Distributed Pair Programming
  * (c) Freie Universitaet Berlin - Fachbereich Mathematik und Informatik - 2006
  * (c) Riad Djemili - 2006
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 1, or (at your option)
  * any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
  */
 package de.fu_berlin.inf.dpp.project;
 
 import java.util.List;
 import java.util.Map;
 import java.util.Random;
 import java.util.concurrent.CopyOnWriteArrayList;
 
 import org.apache.log4j.Logger;
 import org.eclipse.core.resources.IProject;
 import org.jivesoftware.smack.XMPPConnection;
 import org.jivesoftware.smack.XMPPException;
 import org.picocontainer.annotations.Inject;
 
 import de.fu_berlin.inf.dpp.PreferenceUtils;
 import de.fu_berlin.inf.dpp.Saros;
 import de.fu_berlin.inf.dpp.Saros.ConnectionState;
 import de.fu_berlin.inf.dpp.invitation.IIncomingInvitationProcess;
 import de.fu_berlin.inf.dpp.invitation.internal.IncomingInvitationProcess;
 import de.fu_berlin.inf.dpp.net.IConnectionListener;
 import de.fu_berlin.inf.dpp.net.JID;
 import de.fu_berlin.inf.dpp.net.internal.DataTransferManager;
 import de.fu_berlin.inf.dpp.net.internal.XMPPChatReceiver;
 import de.fu_berlin.inf.dpp.net.internal.XMPPChatTransmitter;
 import de.fu_berlin.inf.dpp.observables.SessionIDObservable;
 import de.fu_berlin.inf.dpp.observables.SharedProjectObservable;
 import de.fu_berlin.inf.dpp.project.internal.SharedProject;
 import de.fu_berlin.inf.dpp.util.Util;
 
 /**
  * The SessionManager is responsible for initiating new Saros sessions and for
  * reacting to invitations. The user can be only part of one session at most.
  * 
  * @author rdjemili
  * 
  * @component The single instance of this class per application is created by
  *            PicoContainer in the central plug-in class {@link Saros}
  */
 public class SessionManager implements IConnectionListener, ISessionManager {
 
     private static Logger log = Logger
         .getLogger(SessionManager.class.getName());
 
     @Inject
     protected SharedProjectObservable currentlySharedProject;
 
     @Inject
     protected XMPPChatReceiver xmppReceiver;
 
     @Inject
     protected XMPPChatTransmitter transmitter;
 
     @Inject
     protected DataTransferManager transferManager;
 
     @Inject
     protected SessionIDObservable sessionID;
 
     @Inject
     protected PreferenceUtils preferenceUtils;
 
     private final List<ISessionListener> listeners = new CopyOnWriteArrayList<ISessionListener>();
 
     protected Saros saros;
 
     public SessionManager(Saros saros) {
         this.saros = saros;
         saros.addListener(this);
     }
 
     protected static final Random sessionRandom = new Random(System
         .currentTimeMillis());
 
     public void startSession(IProject project) throws XMPPException {
         if (!saros.isConnected()) {
             throw new XMPPException("No connection");
         }
 
         JID myJID = saros.getMyJID();
         this.sessionID.setValue(String.valueOf(sessionRandom.nextInt()));
 
         SharedProject sharedProject = new SharedProject(saros,
             this.transmitter, this.transferManager, project, myJID);
 
         this.currentlySharedProject.setValue(sharedProject);
 
         sharedProject.start();
 
         for (ISessionListener listener : this.listeners) {
             listener.sessionStarted(sharedProject);
         }
 
         sharedProject.startInvitation(preferenceUtils.getAutoInviteUsers());
 
         SessionManager.log.info("Session started");
     }
 
     /**
      * {@inheritDoc}
      */
     public ISharedProject joinSession(IProject project, JID host, int colorID) {
 
         SharedProject sharedProject = new SharedProject(saros,
             this.transmitter, this.transferManager, project, saros.getMyJID(),
             host, colorID);
         this.currentlySharedProject.setValue(sharedProject);
 
         for (ISessionListener listener : this.listeners) {
             listener.sessionStarted(sharedProject);
         }
 
         log.info("Shared project joined");
 
         return sharedProject;
     }
 
     /**
      * @nonSWT
      */
     public void stopSharedProject() {
 
         assert !Util.isSWT();
 
         SharedProject project = currentlySharedProject.getValue();
 
         if (project == null) {
             return;
         }
 
         this.transmitter.sendLeaveMessage(project);
 
        try {
            // set resources writable again
            project.setProjectReadonly(false);
            project.stop();
            project.dispose();
        } catch (RuntimeException e) {
            log.error("Error stopping project: ", e);
        }
 
         this.currentlySharedProject.setValue(null);
 
         for (ISessionListener listener : this.listeners) {
             try {
                 listener.sessionEnded(project);
             } catch (RuntimeException e) {
                 log.error("Internal error in notifying listener"
                     + " of SharedProject end: ", e);
             }
         }
 
         sessionID.setValue(SessionIDObservable.NOT_IN_SESSION);
 
         SessionManager.log.info("Session left");
     }
 
     public ISharedProject getSharedProject() {
         return this.currentlySharedProject.getValue();
     }
 
     public void addSessionListener(ISessionListener listener) {
         if (!this.listeners.contains(listener)) {
             this.listeners.add(listener);
         }
     }
 
     public void removeSessionListener(ISessionListener listener) {
         this.listeners.remove(listener);
     }
 
     public IIncomingInvitationProcess invitationReceived(JID from,
         String sessionID, String projectName, String description, int colorID) {
 
         this.sessionID.setValue(sessionID);
 
         IIncomingInvitationProcess process = new IncomingInvitationProcess(
             this, this.transmitter, transferManager, from, projectName,
             description, colorID);
 
         for (ISessionListener listener : this.listeners) {
             listener.invitationReceived(process);
         }
 
         SessionManager.log.info("Received invitation from [" + from.getBase()
             + "]");
 
         return process;
     }
 
     public void connectionStateChanged(XMPPConnection connection,
         ConnectionState newState) {
 
         if (newState == ConnectionState.DISCONNECTING) {
             stopSharedProject();
         }
     }
 
     public void onReconnect(Map<JID, Integer> expectedSequenceNumbers) {
 
         SharedProject project = currentlySharedProject.getValue();
 
         if (project == null) {
             return;
         }
 
         this.transmitter.sendRemainingFiles();
         this.transmitter.sendRemainingMessages();
 
         // ask for next expected activities (in case I missed something while
         // being not available)
 
         // TODO this is currently disabled
         this.transmitter.sendRequestForActivity(project,
             expectedSequenceNumbers, true);
     }
 
     public void cancelIncomingInvitation() {
         /**
          * We never started a session, but still had set a session ID because we
          * were in an InvitationProcess.
          */
         sessionID.setValue(SessionIDObservable.NOT_IN_SESSION);
     }
 }
