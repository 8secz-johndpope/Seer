 /******************************************************************************
  * Product of NIST/ITL Advanced Networking Technologies Division (ANTD).      *
  ******************************************************************************/
 package gov.nist.javax.sip;
 
 import javax.sip.*;
 import javax.sip.message.*;
 import gov.nist.javax.sip.stack.*;
 import gov.nist.javax.sip.message.*;
 import gov.nist.javax.sip.header.*;
 import gov.nist.core.*;
 
 /**
  * An adapter class from the JAIN implementation objects to the NIST-SIP stack.
  * This is the class that is instantiated by the NistSipMessageFactory to
  * create a new SIPServerRequest or SIPServerResponse.
  * Note that this is not part of the JAIN-SIP spec (it does not implement
  * a JAIN-SIP interface). This is part of the glue that ties together the
  * NIST-SIP stack and event model with the JAIN-SIP stack. Implementors
  * of JAIN services need not concern themselves with this class.
  *
 * @version JAIN-SIP-1.1 $Revision: 1.24 $ $Date: 2004-03-31 20:30:46 $
  *
  * @author M. Ranganathan <mranga@nist.gov>  <br/>
  * Bug fix Contributions by Lamine Brahimi and  Andreas Bystrom. <br/>
  * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
  */
 public class NistSipMessageHandlerImpl
 	implements SIPServerRequestInterface, SIPServerResponseInterface {
 
 	protected SIPTransaction transactionChannel;
 	protected MessageChannel rawMessageChannel;
 	// protected SIPRequest sipRequest;
 	// protected SIPResponse sipResponse;
 	protected ListeningPointImpl listeningPoint;
 	protected SIPTransactionStack sipStack;
 	protected SipStackImpl sipStackImpl;
 	/**
 	 * Process a request.
 	 *@exception SIPServerException is thrown when there is an error
 	 * processing the request.
 	 */
 	public void processRequest(
 		SIPRequest sipRequest,
 		MessageChannel incomingMessageChannel)
 		throws SIPServerException {
 		// Generate the wrapper JAIN-SIP object.
 		if (LogWriter.needsLogging)
 			sipStackImpl.logMessage(
 				"PROCESSING INCOMING REQUEST " + sipRequest.getFirstLine());
 		if (listeningPoint == null) {
 			if (LogWriter.needsLogging)
 				sipStackImpl.logMessage(
 				"Dropping message: No listening point registered!");
 			return;
 		}
 		this.rawMessageChannel = incomingMessageChannel;
 
 		SIPTransactionStack sipStack =
 			(SIPTransactionStack) transactionChannel.getSIPStack();
 		gov.nist.javax.sip.SipStackImpl sipStackImpl = (SipStackImpl) sipStack;
 		SipProviderImpl sipProvider = listeningPoint.getProvider();
 		if (sipProvider == null) {
 			if (LogWriter.needsLogging)
 				sipStackImpl.logMessage("No provider - dropping !!");
 			return;
 		}
 		SipListener sipListener = sipProvider.sipListener;
 
 		SIPTransaction transaction = transactionChannel;
 		// Look for the registered SIPListener for the message channel.
 //ifndef SIMULATION
 //
 		synchronized (sipProvider) {
 //endif
 //
 			if (sipRequest.getMethod().equalsIgnoreCase(Request.ACK)) {
 				// Could not find transaction. Generate an event
 				// with a null transaction identifier.
 				String dialogId = sipRequest.getDialogId(true);
 				DialogImpl dialog = sipStackImpl.getDialog(dialogId);
 				if (LogWriter.needsLogging)
 					sipStackImpl.logMessage(
 						"Processing ACK for dialog " + dialog);
 
 				if (dialog == null) {
 					if (LogWriter.needsLogging) {
 						sipStackImpl.logMessage(
 							"Dialog does not exist "
 								+ sipRequest.getFirstLine()
 								+ " isServerTransaction = "
 								+ true);
 
 					}
 					// Bug reported by Antonis Karydas
 					transaction =
 						sipStackImpl.findTransaction(sipRequest, true);
 				} else if (dialog.getLastAck()!= null &&
 					   dialog.getLastAck().equals(sipRequest) ){
 					if (sipStackImpl.isRetransmissionFilterActive() ) {
 							dialog.ackReceived(sipRequest);
 							transaction.setDialog(dialog);
 						if (LogWriter.needsLogging) {
 							sipStackImpl.logMessage
 							("Retransmission Filter enabled - dropping Ack"+
 							 " retransmission");
 						}
 						return;
 					}
 					if (LogWriter.needsLogging)
 						sipStackImpl.logMessage(
 							"ACK retransmission for 2XX response "
 							+ "Sending ACK to the TU");
 				} else {
 
 					// This could be a re-invite processing.
 					// check to see if the ack matches with the last
 					// transaction.
 
 					SIPTransaction tr = dialog.getLastTransaction();
 					SIPResponse sipResponse = tr.getLastResponse();
 
 					// Idiot check for sending ACK from the wrong side!
 					if (tr instanceof SIPServerTransaction 
 						&& sipResponse != null
 						&& sipResponse.getStatusCode() / 100 == 2
 						&& sipResponse.getCSeq().getSequenceNumber()
 							== sipRequest.getCSeq().getSequenceNumber()) {
 
 						transaction.setDialog(dialog);
 						dialog.ackReceived(sipRequest);
 						if (sipStackImpl.isRetransmissionFilterActive() &&
 							tr.ackSeen()) {
 							if (LogWriter.needsLogging)
 								sipStackImpl.logMessage(
 								"ACK retransmission for 2XX response --- "
 									+ "Dropping ");
 							return;
 						} else {
 							// record that we already saw an ACK for
 							// this transaction.
 							tr.setAckSeen();
 							sipStackImpl.logMessage(
 								"ACK retransmission for 2XX response --- "
 									+ "sending to TU ");
 						}
 							
 							
 					} else {
 						if (LogWriter.needsLogging)
 							sipStackImpl.logMessage(
 								"ACK retransmission for non 2XX response "
 									+ "Discarding ACK");
 						// Could not find a transaction.
 						if (tr == null) {
 							if (LogWriter.needsLogging)
 								sipStackImpl.logMessage(
 									"Could not find transaction ACK dropped");
 							return;
 						}
 						transaction = tr;
 						if (transaction instanceof SIPClientTransaction) {
 							if (LogWriter.needsLogging)
 								sipStackImpl.logMessage("Dropping late ACK");
 							return;
 						}
 					}
 				}
 			} else if (sipRequest.getMethod().equals(Request.BYE) ) {
 				transaction = this.transactionChannel;
 				// If the stack has not mapped this transaction because
 				// of sequence number problem then just drop the BYE
 				if (transaction != null &&
 				     ((SIPServerTransaction)transaction).isTransactionMapped()) {
 				    // Get the dialog identifier for the bye request.
 					String dialogId = sipRequest.getDialogId(true);
 					if (LogWriter.needsLogging)
 						sipStackImpl.logMessage("dialogId = " + dialogId);
 					DialogImpl dialog = sipStackImpl.getDialog(dialogId);
 					if (dialog != null) {
 						dialog.addTransaction(transaction);
 					} else {
 						dialogId = sipRequest.getDialogId(false);
 						if (LogWriter.needsLogging)
 							sipStackImpl.getLogWriter().logMessage(
 							"dialogId = " + dialogId);
 						dialog = sipStackImpl.getDialog(dialogId);
 						if (dialog != null) {
 							dialog.addTransaction(transaction);
 						} else {
 							transaction = null; // pass up to provider for
 							// stateless handling.
 						}
 					}
 			
 				} else if (transaction != null)  {
 					// This is an out of sequence BYE
 					// transaction was allocated but
 					// not mapped to the stack so
 					// just discard it.
					String dialogId = sipRequest.getDialogId(true);
					DialogImpl dialog = sipStack.getDialog(dialogId);
					if (dialog != null) {
						if (LogWriter.needsLogging)
						sipStackImpl.logMessage
 						("Dropping out of sequence BYE");
						return;
					} else transaction = null;
					
 				}
 				// note that the transaction may be null (which 
 				// happens when no dialog for the bye was fund.
 			} else if (
 				sipRequest.getRequestLine().getMethod().equals(
 					Request.CANCEL)) {
 
 				// The ID refers to a previously sent
 				// INVITE therefore it refers to the
 				// server transaction table.
				// Bug reported by Andreas Bystr�m
 				// Find the transaction to cancel.
 				// Send a 487 for the cancel to inform the
 				// other side that we've seen it but do not send the
 				// request up to the application layer.
 
 				// Get rid of the CANCEL transaction -- we pass the
 				// transaciton we are trying to cancel up to the TU.
 
 				// Antonis Karydas: Suggestion
 				// 'transaction' here refers to the transaction to 
 				// be cancelled. Do not change
 				// it's state because of the CANCEL. 
 				// Wait, instead for the 487 from TU.
 				// transaction.setState(SIPTransaction.TERMINATED_STATE);
 
 				SIPServerTransaction serverTransaction =
 					(SIPServerTransaction) sipStack.findCancelTransaction(
 						sipRequest,
 						true);
 
 				// Generate an event
 				// with a null transaction identifier.
 				if (serverTransaction == null) {
 					// Could not find the invite transaction.
 					if (LogWriter.needsLogging) {
 						sipStackImpl.logMessage(
 							"transaction "
 								+ " does not exist "
 								+ sipRequest.getFirstLine()
 								+ "isServerTransaction = "
 								+ true);
 					}
 					transaction = null;
 				} else {
 					transaction = serverTransaction;
 				}
 			}
 
 			if (LogWriter.needsLogging) {
 				sipStackImpl.logMessage("-----------------");
 				sipStackImpl.logMessage(sipRequest.encodeMessage());
 			}
 			// If the transaction is found then it is already managed so
 			// dont call the listener.
 			if (sipStack.isDialogCreated(sipRequest.getMethod())) {
 				if ((SIPServerTransaction) sipStack
 					.findTransaction(sipRequest, true) 
 				!= null) {
 					return;
 				} 
 			}
 			String dialogId = sipRequest.getDialogId(true);
 			DialogImpl dialog = sipStackImpl.getDialog(dialogId);
 
 			// Sequence numbers are supposed to be incremented
 			// monotonically (actually sequentially) for RFC 3261
 
 			if (dialog != null && 
 				transaction != null && 
 				! sipRequest.getMethod().equals(Request.BYE) &&
 				! sipRequest.getMethod().equals(Request.CANCEL) &&
 				! sipRequest.getMethod().equals(Request.ACK)) {
 				if (  dialog.getRemoteSequenceNumber() >= 
 				      sipRequest.getCSeq().getSequenceNumber() ) {
 				      if (LogWriter.needsLogging) {
 					sipStackImpl.logMessage
 					("Dropping out of sequence message " +
 				          dialog.getRemoteSequenceNumber() + 
 					  " "  + sipRequest.getCSeq());
 				     }
 				      
 				      return;
 				}
 				dialog.addTransaction(transaction);
 				dialog.addRoute(sipRequest);
 			}
 
 			RequestEvent sipEvent;
 
 			if (dialog == null
 				&& sipRequest.getMethod().equals(Request.NOTIFY)) {
 				SIPClientTransaction ct =
 					sipStack.findSubscribeTransaction(sipRequest);
 				// From RFC 3265
 				// If the server transaction cannot be found or if it
 				// aleady has a dialog attached to it then just assign the
 				// notify to this dialog and pass it up. 
 				if (ct != null) {
 					transaction.setDialog((DialogImpl) ct.getDialog());
 					if (ct.getDialog().getState() == null) {
 						sipEvent =
 							new RequestEvent(
 								sipProvider,
 								null,
 								(Request) sipRequest);
 					} else {
 						sipEvent =
 							new RequestEvent(
 								(SipProvider) sipProvider,
 								(ServerTransaction) transaction,
 								(Request) sipRequest);
 					}
 				} else {
 					// Got a notify out of the blue - just pass it up 
 					// for stateless handling by the application.
 					sipEvent =
 						new RequestEvent(
 							sipProvider,
 							null,
 							(Request) sipRequest);
 				}
 
 			} else {
 				// For a dialog creating event - set the transaction to null.
 				// The listener can create the dialog if needed.
 				if (transaction != null
 					&& ((SIPServerTransaction) transaction).isTransactionMapped())
 					sipEvent =
 						new RequestEvent(
 							sipProvider,
 							(ServerTransaction) transaction,
 							(Request) sipRequest);
 				else
 					sipEvent =
 						new RequestEvent(
 							sipProvider,
 							null,
 							(Request) sipRequest);
 			}
 			sipProvider.handleEvent(sipEvent, transaction);
 //ifndef SIMULATION
 //
 		}
 //endif
 //
 
 	}
 
 	/**
 	 *Process the response.
 	 *@exception SIPServerException is thrown when there is an error
 	 * processing the response
 	 *@param incomingMessageChannel -- message channel on which the
 	 * response is received.
 	 */
 	public void processResponse(
 		SIPResponse sipResponse,
 		MessageChannel incomingMessageChannel)
 		throws SIPServerException {
 		if (LogWriter.needsLogging) {
 			sipStackImpl.logMessage(
 				"PROCESSING INCOMING RESPONSE" + sipResponse.encodeMessage());
 		}
 		if (listeningPoint == null) {
 			if (LogWriter.needsLogging)
 				sipStackImpl.logMessage(
 					"Dropping message: No listening point" + " registered!");
 			return;
 		}
 
 		SIPTransaction transaction = (SIPTransaction) this.transactionChannel;
 		SipProviderImpl sipProvider = listeningPoint.getProvider();
 		if (sipProvider == null) {
 			if (LogWriter.needsLogging) {
 				sipStackImpl.logMessage("Dropping message:  no provider");
 			}
 			return;
 		}
 		this.rawMessageChannel = incomingMessageChannel;
 
 		SIPTransactionStack sipStack =
 			(SIPTransactionStack) sipProvider.sipStackImpl;
 		SipStackImpl sipStackImpl = (SipStackImpl) sipStack;
 
 		if (LogWriter.needsLogging)
 			sipStackImpl.logMessage("Transaction = " + transaction);
 
 		if (this.transactionChannel == null) {
 			String dialogId = sipResponse.getDialogId(false);
 			DialogImpl dialog = sipStack.getDialog(dialogId);
 			//  Have a dialog but could not find transaction.
 			if (sipProvider.sipListener == null) {
 				return;
 			} else if (dialog != null) {
 				// Bug report by Emil Ivov
 				if (sipResponse.getStatusCode() != Response.OK) {
 					return;
 				} else if (sipStackImpl.isRetransmissionFilterActive()) {
 					// 200  retransmission for the final response.
 					if (sipResponse
 						.getCSeq()
 						.equals(
 							dialog
 								.getFirstTransaction()
 								.getRequest()
 								.getHeader(
 								SIPHeaderNames.CSEQ))) {
 						try {
 							// Found the dialog - resend the ACK and
 							// dont pass up the null transaction
 							// bug noticed by Joe Provino.
 							dialog.resendAck();
 							return;
 						} catch (SipException ex) {
 							// What to do here ?? kill the dialog?
 						}
 					}
 				}
 			}
 			// Pass the response up to the application layer to handle
 			// statelessly.
 			// Dialog is null so this is handled statelessly
 
 			ResponseEvent sipEvent =
 				new ResponseEvent(sipProvider, null, (Response) sipResponse);
 			sipProvider.handleEvent(sipEvent, transaction);
 			return;
 		}
 
 
 
 		SipListener sipListener = sipProvider.sipListener;
 
 		ResponseEvent responseEvent =
 			new javax.sip.ResponseEvent(
 				sipProvider,
 				(ClientTransaction) transaction,
 				(Response) sipResponse);
 		sipProvider.handleEvent(responseEvent, transaction);
 
 	}
 	/** Get the sender channel.
 	 */
 	public MessageChannel getRequestChannel() {
 		return this.transactionChannel;
 	}
 
 	/** Get the channel if we want to initiate a new transaction to
 	 * the sender of  a response.
 	 *@return a message channel that points to the place from where we got
 	 * the response.
 	 */
 	public MessageChannel getResponseChannel() {
 		if (this.transactionChannel != null)
 			return this.transactionChannel;
 		else
 			return this.rawMessageChannel;
 	}
 
 	/** Just a placeholder. This is called from the stack
 	 * for message logging. Auxiliary processing information can
 	 * be passed back to be  written into the log file.
 	 *@return auxiliary information that we may have generated during the
 	 * message processing which is retrieved by the message logger.
 	 */
 	public String getProcessingInfo() {
 		return null;
 	}
 }
 /*
  * $Log: not supported by cvs2svn $
 * Revision 1.23  2004/03/25 15:15:03  mranga
 * Reviewed by:   mranga
 * option to log message content added.
 *
  * Revision 1.22  2004/03/05 20:36:55  mranga
  * Reviewed by:   mranga
  * put in some debug printfs and cleaned some things up.
  *
  * Revision 1.21  2004/02/26 14:28:50  mranga
  * Reviewed by:   mranga
  * Moved some code around (no functional change) so that dialog state is set
  * when the transaction is added to the dialog.
  * Cleaned up the Shootist example a bit.
  *
  * Revision 1.20  2004/02/25 23:02:13  mranga
  * Submitted by:  jeand
  * Reviewed by:   mranga
  * Remove pointless redundant code
  *
  * Revision 1.19  2004/02/25 22:15:43  mranga
  * Submitted by:  jeand
  * Reviewed by:   mranga
  * Dialog state should be set to completed state and not confirmed state on bye.
  *
  * Revision 1.18  2004/02/20 20:22:55  mranga
  * Reviewed by:   mranga
  * More hacks to supress OK retransmission on re-invite when retransmission
  * filter  is enabled.
  *
  * Revision 1.17  2004/02/19 16:21:16  mranga
  * Reviewed by:   mranga
  * added idiot check to guard against servers who like to send acks from the
  * wrong side.
  *
  * Revision 1.16  2004/02/19 16:01:40  mranga
  * Reviewed by:   mranga
  * tighten up retransmission filter to deal with ack retransmissions.
  *
  * Revision 1.15  2004/02/19 15:20:27  mranga
  * Reviewed by:   mranga
  * allow ack to go through for re-invite processing
  *
  * Revision 1.14  2004/02/13 13:55:31  mranga
  * Reviewed by:   mranga
  * per the spec, Transactions must always have a valid dialog pointer. Assigned a dummy dialog for transactions that are not assigned to any dialog (such as Message).
  *
  * Revision 1.13  2004/02/11 20:22:30  mranga
  * Reviewed by:   mranga
  * tighten up the sequence number checks for BYE processing.
  *
  * Revision 1.12  2004/02/05 10:58:12  mranga
  * Reviewed by:   mranga
  * Fixed a another bug caused by previous fix that restricted request
  * consumption in a dialog based on increasing sequence
  * numbers.
  *
  * Revision 1.11  2004/02/04 22:07:24  mranga
  * Reviewed by:   mranga
  * Fix for handling of out of order sequence numbers in the dialog layer.
  *
  * Revision 1.10  2004/02/04 18:44:18  mranga
  * Reviewed by:   mranga
  * check sequence number before delivering event to application.
  *
  * Revision 1.9  2004/01/27 15:11:06  mranga
  * Submitted by:  jeand
  * Reviewed by:   mranga
  * If retrans filter enabled then ack should be seen only once by
  * application. Else each retransmitted ack is seen by application.
  *
  * Revision 1.8  2004/01/26 19:12:49  mranga
  * Reviewed by:   mranga
  * moved SIMULATION tag to first columnm
  *
  * Revision 1.7  2004/01/22 13:26:28  sverker
  * Issue number:
  * Obtained from:
  * Submitted by:  sverker
  * Reviewed by:   mranga
  *
  * Major reformat of code to conform with style guide. Resolved compiler and javadoc warnings. Added CVS tags.
  *
  * CVS: ----------------------------------------------------------------------
  * CVS: Issue number:
  * CVS:   If this change addresses one or more issues,
  * CVS:   then enter the issue number(s) here.
  * CVS: Obtained from:
  * CVS:   If this change has been taken from another system,
  * CVS:   then name the system in this line, otherwise delete it.
  * CVS: Submitted by:
  * CVS:   If this code has been contributed to the project by someone else; i.e.,
  * CVS:   they sent us a patch or a set of diffs, then include their name/email
  * CVS:   address here. If this is your work then delete this line.
  * CVS: Reviewed by:
  * CVS:   If we are doing pre-commit code reviews and someone else has
  * CVS:   reviewed your changes, include their name(s) here.
  * CVS:   If you have not had it reviewed then delete this line.
  *
  */
