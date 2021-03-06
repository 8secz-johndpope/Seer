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
 package de.fu_berlin.inf.dpp.invitation.internal;
 
 import org.apache.log4j.Logger;
 
 import de.fu_berlin.inf.dpp.invitation.IInvitationProcess;
 import de.fu_berlin.inf.dpp.net.ITransmitter;
 import de.fu_berlin.inf.dpp.net.JID;
import de.fu_berlin.inf.dpp.util.StackTrace;
 
 /**
  * @author rdjemili
  */
 public abstract class InvitationProcess implements IInvitationProcess {
 
     private static Logger logger = Logger.getLogger(InvitationProcess.class);
 
     protected final ITransmitter transmitter;
 
     protected State state;
 
     private Exception exception;
 
     protected JID peer;
 
     protected IInvitationUI invitationUI = null;
 
     protected String description;
 
     protected final int colorID;
 
     public InvitationProcess(ITransmitter transmitter, JID peer,
         String description, int colorID) {
         this.transmitter = transmitter;
         this.peer = peer;
         this.description = description;
         this.colorID = colorID;
 
         transmitter.addInvitationProcess(this);
     }
 
     /**
      * @see de.fu_berlin.inf.dpp.IInvitationProcess
      */
     public Exception getException() {
         return this.exception;
     }
 
     /**
      * @see de.fu_berlin.inf.dpp.IInvitationProcess
      */
     public State getState() {
         return this.state;
     }
 
     public void setState(State newstate) {
         this.state = newstate;
 
         if (this.invitationUI != null) {
             this.invitationUI.updateInvitationProgress(this.peer);
         }
     }
 
     /**
      * @see de.fu_berlin.inf.dpp.IInvitationProcess
      */
     public JID getPeer() {
         return this.peer;
     }
 
     /**
      * @see de.fu_berlin.inf.dpp.IInvitationProcess
      */
     public String getDescription() {
         return this.description;
     }
 
     /**
      * We are called because the invitation was canceled.
      * 
      * @param replicated
      *            true if the message originated remotely, false if locally.
      */
     public void cancel(String errorMsg, boolean replicated) {
         if (this.state == State.CANCELED) {
             return;
         }
 
         setState(State.CANCELED);
 
         if (errorMsg != null) {
            InvitationProcess.logger.error(
                "Invitation was canceled because of an error: " + errorMsg,
                new StackTrace());
         } else {
             InvitationProcess.logger.info("Invitation was canceled.");
         }
 
         if (!replicated) {
             this.transmitter.sendCancelInvitationMessage(this.peer, errorMsg);
         }
 
         this.invitationUI.cancel(this.peer, errorMsg, replicated);
 
         this.transmitter.removeInvitationProcess(this);
     }
 
     @Override
     public String toString() {
         return "InvitationProcess(peer:" + this.peer + ", state:" + this.state
             + ")";
     }
 
     /**
      * Should be called if an exception occurred. This saves the exception and
      * sets the invitation to canceled.
      */
     protected void failed(Exception e) {
         this.exception = e;
         logger.error("Invitation process failed", e);
         cancel(e.getMessage(), false);
     }
 
     /**
      * Assert that the process is in given state or throw an exception
      * otherwise.
      * 
      * @param expected
      *            the state that the process should currently have.
      */
     protected void assertState(State expected) {
         if (this.state != expected) {
             cancel("Unexpected state(" + this.state + ")", false);
         }
     }
 
     protected void failState() {
         throw new IllegalStateException("Bad input while in state "
             + this.state);
     }
 
     public void setInvitationUI(IInvitationUI inviteUI) {
         this.invitationUI = inviteUI;
     }
 
 }
