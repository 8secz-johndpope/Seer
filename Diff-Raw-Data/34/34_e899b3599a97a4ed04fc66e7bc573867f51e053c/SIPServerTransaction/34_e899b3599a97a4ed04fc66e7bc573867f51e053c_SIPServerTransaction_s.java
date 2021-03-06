 /*
  * Conditions Of Use 
  * 
  * This software was developed by employees of the National Institute of
  * Standards and Technology (NIST), an agency of the Federal Government.
  * Pursuant to title 15 Untied States Code Section 105, works of NIST
  * employees are not subject to copyright protection in the United States
  * and are considered to be in the public domain.  As a result, a formal
  * license is not needed to use the software.
  * 
  * This software is provided by NIST as a service and is expressly
  * provided "AS IS."  NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED
  * OR STATUTORY, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF
  * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT
  * AND DATA ACCURACY.  NIST does not warrant or make any representations
  * regarding the use of the software or the results thereof, including but
  * not limited to the correctness, accuracy, reliability or usefulness of
  * the software.
  * 
  * Permission to use this software is contingent upon your acceptance
  * of the terms of this agreement
  *  
  * .
  * 
  */
 package gov.nist.javax.sip.stack;
 
 import gov.nist.core.*;
 import gov.nist.javax.sip.message.*;
 import gov.nist.javax.sip.header.*;
 import gov.nist.javax.sip.SIPConstants;
 import gov.nist.javax.sip.SipProviderImpl;
 
 import javax.sip.address.Hop;
 import javax.sip.header.*;
 import javax.sip.message.*;
 import javax.sip.*;
 
 import java.text.ParseException;
 import java.io.IOException;
 
 import java.util.TimerTask;
 
 /*
  * Bug fixes / enhancements:Emil Ivov, Antonis Karydas, Daniel J. Martinez
  * Manzano, Daniel, Hagai Sela, Vazques-Illa, Bill Roome, Thomas Froment and
  * Pierre De Rop, Christophe Anzille and Jeroen van Bemmel, Frank Reif.
  * 
  */
 
 /**
  * Represents a server transaction. Implements the following state machines.
  * 
  * <pre>
  *                                
  *                                 
  *                                  
  *                                                                 |INVITE
  *                                                                 |pass INV to TU
  *                                              INVITE             V send 100 if TU won't in 200ms
  *                                              send response+-----------+
  *                                                  +--------|           |--------+101-199 from TU
  *                                                  |        | Proceeding|        |send response
  *                                                  +-------&gt;|           |&lt;-------+
  *                                                           |           |          Transport Err.
  *                                                           |           |          Inform TU
  *                                                           |           |---------------&gt;+
  *                                                           +-----------+                |
  *                                              300-699 from TU |     |2xx from TU        |
  *                                              send response   |     |send response      |
  *                                                              |     +------------------&gt;+
  *                                                              |                         |
  *                                              INVITE          V          Timer G fires  |
  *                                              send response+-----------+ send response  |
  *                                                  +--------|           |--------+       |
  *                                                  |        | Completed |        |       |
  *                                                  +-------&gt;|           |&lt;-------+       |
  *                                                           +-----------+                |
  *                                                              |     |                   |
  *                                                          ACK |     |                   |
  *                                                          -   |     +------------------&gt;+
  *                                                              |        Timer H fires    |
  *                                                              V        or Transport Err.|
  *                                                           +-----------+  Inform TU     |
  *                                                           |           |                |
  *                                                           | Confirmed |                |
  *                                                           |           |                |
  *                                                           +-----------+                |
  *                                                                 |                      |
  *                                                                 |Timer I fires         |
  *                                                                 |-                     |
  *                                                                 |                      |
  *                                                                 V                      |
  *                                                           +-----------+                |
  *                                                           |           |                |
  *                                                           | Terminated|&lt;---------------+
  *                                                           |           |
  *                                                           +-----------+
  *                                  
  *                                                Figure 7: INVITE server transaction
  *                                  
  *                                  
  *                                     		Request received
  *                                                                    |pass to TU
  *                                  
  *                                                                    V
  *                                                              +-----------+
  *                                                              |           |
  *                                                              | Trying    |-------------+
  *                                                              |           |             |
  *                                                              +-----------+             |200-699 from TU
  *                                                                    |                   |send response
  *                                                                    |1xx from TU        |
  *                                                                    |send response      |
  *                                                                    |                   |
  *                                                 Request            V      1xx from TU  |
  *                                                 send response+-----------+send response|
  *                                                     +--------|           |--------+    |
  *                                                     |        | Proceeding|        |    |
  *                                                     +-------&gt;|           |&lt;-------+    |
  *                                              +&lt;--------------|           |             |
  *                                              |Trnsprt Err    +-----------+             |
  *                                              |Inform TU            |                   |
  *                                              |                     |                   |
  *                                              |                     |200-699 from TU    |
  *                                              |                     |send response      |
  *                                              |  Request            V                   |
  *                                              |  send response+-----------+             |
  *                                              |      +--------|           |             |
  *                                              |      |        | Completed |&lt;------------+
  *                                              |      +-------&gt;|           |
  *                                              +&lt;--------------|           |
  *                                              |Trnsprt Err    +-----------+
  *                                              |Inform TU            |
  *                                              |                     |Timer J fires
  *                                              |                     |-
  *                                              |                     |
  *                                              |                     V
  *                                              |               +-----------+
  *                                              |               |           |
  *                                              +--------------&gt;| Terminated|
  *                                                              |           |
  *                                                              +-----------+
  *                                  
  *                                  
  *                                  
  *                                  
  *                                 
  * </pre>
  * 
 * @version 1.2 $Revision: 1.67 $ $Date: 2006-07-20 14:58:33 $
  * @author M. Ranganathan <br/><a href=" {@docRoot}/uncopyright.html">This
  *         code is in the public domain. </a>
  * 
  * 
  */
 public class SIPServerTransaction extends SIPTransaction implements
 		ServerRequestInterface, javax.sip.ServerTransaction {
 
 	// force the listener to see transaction
 
 	private int rseqNumber;
 
 	// private LinkedList pendingRequests;
 
 	// Real RequestInterface to pass messages to
 	private ServerRequestInterface requestOf;
 
 	private SIPDialog dialog;
 
 	// the unacknowledged SIPResponse
 
 	private SIPResponse pendingReliableResponse;
 
 	// The pending reliable Response Timer
 	private ProvisionalResponseTask provisionalResponseTask;
 
 	private boolean retransmissionAlertEnabled;
 
 	private RetransmissionAlertTimerTask retransmissionAlertTimerTask;
 
 	protected boolean isAckSeen;
 
 	private SIPClientTransaction pendingSubscribeTransaction;
 
 	private SIPServerTransaction inviteTransaction;
 
 	/**
 	 * This timer task is used for alerting the application to send
 	 * retransmission alerts.
 	 * 
 	 * 
 	 */
 	class RetransmissionAlertTimerTask extends TimerTask {
 
 		String dialogId;
 
 		int ticks;
 
 		int ticksLeft;
 
 		public RetransmissionAlertTimerTask(String dialogId) {
 
 			this.ticks = SIPTransaction.T1;
 			this.ticksLeft = this.ticks;
 		}
 
 		public void run() {
 			SIPServerTransaction serverTransaction = SIPServerTransaction.this;
 			ticksLeft--;
 			if (ticksLeft == -1) {
 				serverTransaction.fireRetransmissionTimer();
 				this.ticksLeft = 2 * ticks;
 			}
 
 		}
 
 	}
 
 	class ProvisionalResponseTask extends TimerTask {
 
 		int ticks;
 
 		int ticksLeft;
 
 		public ProvisionalResponseTask() {
 			this.ticks = SIPTransaction.T1;
 			this.ticksLeft = this.ticks;
 		}
 
 		public void run() {
 			SIPServerTransaction serverTransaction = SIPServerTransaction.this;
 			/*
 			 * The reliable provisional response is passed to the transaction
 			 * layer periodically with an interval that starts at T1 seconds and
 			 * doubles for each retransmission (T1 is defined in Section 17 of
 			 * RFC 3261). Once passed to the server transaction, it is added to
 			 * an internal list of unacknowledged reliable provisional
 			 * responses. The transaction layer will forward each retransmission
 			 * passed from the UAS core.
 			 */
 			// If the transaction has terminated,
 			if (serverTransaction.isTerminated()) {
 
 				this.cancel();
 
 			} else {
 				ticksLeft--;
 				if (ticksLeft == -1) {
 					serverTransaction.fireReliableResponseRetransmissionTimer();
 					this.ticksLeft = 2 * ticks;
 				}
 
 			}
 
 		}
 
 	}
 
 	class SendTrying extends TimerTask {
 
 		protected SendTrying() {
 			if (sipStack.isLoggingEnabled())
 				sipStack.logWriter.logDebug("scheduled timer for "
 						+ SIPServerTransaction.this);
 
 		}
 
 		public void run() {
 			SIPServerTransaction serverTransaction = SIPServerTransaction.this;
 
 			if (serverTransaction.getRealState() == null
 					|| TransactionState.TRYING == serverTransaction
 							.getRealState()) {
 				if (sipStack.isLoggingEnabled())
 					sipStack.logWriter
 							.logDebug(" sending Trying current state = "
 									+ serverTransaction.getRealState());
 				try {
 					serverTransaction
 							.sendMessage(serverTransaction.getOriginalRequest()
 									.createResponse(100, "Trying"));
 					if (serverTransaction.sipStack.isLoggingEnabled())
 						sipStack.logWriter.logDebug(" trying sent "
 								+ serverTransaction.getRealState());
 				} catch (IOException ex) {
 					if (serverTransaction.sipStack.isLoggingEnabled())
 						sipStack.logWriter.logError("IO error sending  TRYING");
 				}
 			}
 		}
 	}
 
 	class TransactionTimer extends TimerTask {
 
 		public TransactionTimer() {
 			if (sipStack.logWriter.isLoggingEnabled()) {
 				sipStack.logWriter.logDebug("TransactionTimer() : "
 						+ getTransactionId());
 			}
 
 		}
 
 		public void run() {
 			// If the transaction has terminated,
 			if (isTerminated()) {
 				// Keep the transaction hanging around in the transaction table
 				// to catch the incoming ACK -- this is needed for tcp only.
 				// Note that the transaction record is actually removed in
 				// the connection linger timer.
 				try {
 					this.cancel();
 				} catch (IllegalStateException ex) {
 					if (!sipStack.isAlive())
 						return;
 				}
 
 				// Oneshot timer that garbage collects the SeverTransaction
 				// after a scheduled amount of time. The linger timer allows
 				// the client side of the tx to use the same connection to
 				// send an ACK and prevents a race condition for creation
 				// of new server tx
 				TimerTask myTimer = new LingerTimer();
 
 				sipStack.timer.schedule(myTimer,
 						SIPTransactionStack.CONNECTION_LINGER_TIME * 1000);
 
 			} else {
 				// Add to the fire list -- needs to be moved
 				// outside the synchronized block to prevent
 				// deadlock.
 				fireTimer();
 
 			}
 		}
 
 	}
 
 	/**
 	 * Send a response.
 	 * 
 	 * @param transactionResponse --
 	 *            the response to send
 	 * 
 	 */
 
 	private void sendResponse(SIPResponse transactionResponse)
 			throws IOException {
 
 		// RFC18.2.2. Sending Responses
 		// The server transport uses the value of the top Via header field in
 		// order
 		// to determine where to send a response.
 		// It MUST follow the following process:
 		// If the "sent-protocol" is a reliable transport
 		// protocol such as TCP or SCTP,
 		// or TLS over those, the response MUST be
 		// sent using the existing connection
 		// to the source of the original request
 		// that created the transaction, if that connection is still open.
 		if (isReliable()) {
 
 			getMessageChannel().sendMessage(transactionResponse);
 
 			// TODO If that connection attempt fails, the server SHOULD
 			// use SRV 3263 procedures
 			// for servers in order to determine the IP address
 			// and port to open the connection and send the response to.
 
 		} else {
 			Via via = transactionResponse.getTopmostVia();
 			String transport = via.getTransport();
 			if (transport == null)
 				throw new IOException("missing transport!");
 			// @@@ hagai Symmetric NAT support
 			int port = via.getRPort();
 			if (port == -1)
 				port = via.getPort();
 			if (port == -1) {
 				if (transport.equalsIgnoreCase("TLS"))
 					port = 5061;
 				else
 					port = 5060;
 			}
 
 			// Otherwise, if the Via header field value contains a
 			// "maddr" parameter, the response MUST be forwarded to
 			// the address listed there, using the port indicated in "sent-by",
 			// or port 5060 if none is present. If the address is a multicast
 			// address, the response SHOULD be sent using
 			// the TTL indicated in the "ttl" parameter, or with a
 			// TTL of 1 if that parameter is not present.
 			Host maddr = via.getMaddr();
 			String host = null;
 			if (maddr != null) {
 				host = maddr.getHostname();
 			} else {
 				// Otherwise (for unreliable unicast transports),
 				// if the top Via has a "received" parameter, the response MUST
 				// be sent to the
 				// address in the "received" parameter, using the port indicated
 				// in the
 				// "sent-by" value, or using port 5060 if none is specified
 				// explicitly.
 				host = via.getParameter(Via.RECEIVED);
 				if (host == null) {
 					// Otherwise, if it is not receiver-tagged, the response
 					// MUST be
 					// sent to the address indicated by the "sent-by" value,
 					// using the procedures in Section 5
 					// RFC 3263 PROCEDURE TO BE DONE HERE
 					host = via.getHost();
 				}
 			}
 
 			Hop hop = sipStack.addressResolver.resolveAddress(new HopImpl(host,
 					port, transport));
 
 			MessageChannel messageChannel = ((SIPTransactionStack) getSIPStack())
 					.createRawMessageChannel(this.getPort(), hop);
 			if (messageChannel != null)
 				messageChannel.sendMessage(transactionResponse);
 			else
 				throw new IOException("Could not create a message channel for "
 						+ hop);
 		}
 	}
 
 	/**
 	 * Creates a new server transaction.
 	 * 
 	 * @param newSIPStack
 	 *            Transaction stack this transaction belongs to.
 	 * @param newChannelToUse
 	 *            Channel to encapsulate.
 	 */
 	protected SIPServerTransaction(SIPTransactionStack newSIPStack,
 			MessageChannel newChannelToUse) {
 
 		super(newSIPStack, newChannelToUse);
 		this.rseqNumber = (int) (Math.random()) * 1000;
 		// Only one outstanding request for a given server tx.
 
 		if (sipStack.isLoggingEnabled()) {
 			sipStack.logWriter.logDebug("Creating Server Transaction"
 					+ this.getBranchId());
 			sipStack.logWriter.logStackTrace();
 		}
 
 	}
 
 	/**
 	 * Sets the real RequestInterface this transaction encapsulates.
 	 * 
 	 * @param newRequestOf
 	 *            RequestInterface to send messages to.
 	 */
 	public void setRequestInterface(ServerRequestInterface newRequestOf) {
 
 		requestOf = newRequestOf;
 
 	}
 
 	/**
 	 * Returns this transaction.
 	 */
 	public MessageChannel getResponseChannel() {
 
 		return this;
 
 	}
 
 	/**
 	 * Deterines if the message is a part of this transaction.
 	 * 
 	 * @param messageToTest
 	 *            Message to check if it is part of this transaction.
 	 * 
 	 * @return True if the message is part of this transaction, false if not.
 	 */
 	public boolean isMessagePartOfTransaction(SIPMessage messageToTest) {
 
 		// List of Via headers in the message to test
 		ViaList viaHeaders;
 		// Topmost Via header in the list
 		Via topViaHeader;
 		// Branch code in the topmost Via header
 		String messageBranch;
 		// Flags whether the select message is part of this transaction
 		boolean transactionMatches;
 
 		transactionMatches = false;
 
 		String method = messageToTest.getCSeq().getMethod();
 		// Invite Server transactions linger in the terminated state in the
 		// transaction
 		// table and are matched to compensate for
 		// http://bugs.sipit.net/show_bug.cgi?id=769
 		if ((method.equals(Request.INVITE) || !isTerminated())) {
 
 			// Get the topmost Via header and its branch parameter
 			viaHeaders = messageToTest.getViaHeaders();
 			if (viaHeaders != null) {
 
 				topViaHeader = (Via) viaHeaders.getFirst();
 				messageBranch = topViaHeader.getBranch();
 				if (messageBranch != null) {
 
 					// If the branch parameter exists but
 					// does not start with the magic cookie,
 					if (!messageBranch
 							.startsWith(SIPConstants.BRANCH_MAGIC_COOKIE)) {
 
 						// Flags this as old
 						// (RFC2543-compatible) client
 						// version
 						messageBranch = null;
 
 					}
 
 				}
 
 				// If a new branch parameter exists,
 				if (messageBranch != null && this.getBranch() != null) {
 					if (method.equals(Request.CANCEL)) {
 						// Cancel is handled as a special case because it
 						// shares the same same branch id of the invite
 						// that it is trying to cancel.
 						transactionMatches = this.getMethod().equals(
 								Request.CANCEL)
 								&& getBranch().equalsIgnoreCase(messageBranch)
 								&& topViaHeader.getSentBy().equals(
 										((Via) getOriginalRequest()
 												.getViaHeaders().getFirst())
 												.getSentBy());
 
 					} else {
 						// Matching server side transaction with only the
 						// branch parameter.
 						transactionMatches = getBranch().equalsIgnoreCase(
 								messageBranch)
 								&& topViaHeader.getSentBy().equals(
 										((Via) getOriginalRequest()
 												.getViaHeaders().getFirst())
 												.getSentBy());
 
 					}
 
 				} else {
 					// If this is an RFC2543-compliant message,
 					// This code is really here for backwards compatibility. It
 					// is a weak check.
 					// If RequestURI, To tag, From tag,
 					// CallID, CSeq number, and top Via
 					// headers are the same,
 					String originalFromTag = super.fromTag;
 
 					String thisFromTag = messageToTest.getFrom().getTag();
 
 					boolean skipFrom = (originalFromTag == null || thisFromTag == null);
 
 					String originalToTag = super.toTag;
 
 					String thisToTag = messageToTest.getTo().getTag();
 
 					boolean skipTo = (originalToTag == null || thisToTag == null);
 					boolean isResponse = (messageToTest instanceof SIPResponse);
 					if ((isResponse || getOriginalRequest().getRequestURI()
 							.equals(
 									((SIPRequest) messageToTest)
 											.getRequestURI()))
 							&& (skipFrom || originalFromTag
 									.equalsIgnoreCase(thisFromTag))
 							&& (skipTo || originalToTag
 									.equalsIgnoreCase(thisToTag))
 							&& getOriginalRequest().getCallId().getCallId()
 									.equalsIgnoreCase(
 											messageToTest.getCallId()
 													.getCallId())
 							&& getOriginalRequest().getCSeq()
 									.getSequenceNumberLong() == messageToTest
 									.getCSeq().getSequenceNumberLong()
 							&& topViaHeader.equals(getOriginalRequest()
 									.getViaHeaders().getFirst())) {
 
 						transactionMatches = true;
 					}
 
 				}
 
 			}
 
 		}
 		return transactionMatches;
 
 	}
 
 	/**
 	 * Send out a trying response (only happens when the transaction is mapped).
 	 * Otherwise the transaction is not known to the stack.
 	 */
 	protected void map() {
 		// note that TRYING is a pseudo-state for invite transactions
 
 		if (getRealState() == null || getRealState() == TransactionState.TRYING) {
 			// JvB: Removed the condition 'dialog!=null'. Trying should also be
 			// sent by intermediate proxies. This fixes some TCK tests
 			// null check added as the stack may be stopped.
 			if (isInviteTransaction() && !this.isMapped
 					&& sipStack.timer != null) {
 				this.isMapped = true;
 				// Schedule a timer to fire in 200 ms if the
 				// TU did not send a trying in that time.
 				sipStack.timer.schedule(new SendTrying(), 200);
 
 			} else {
 				isMapped = true;
 			}
 		}
 		// Pull it out of the pending transactions list.
 		sipStack.removePendingTransaction(this);
 	}
 
 	/**
 	 * Return true if the transaction is known to stack.
 	 */
 	public boolean isTransactionMapped() {
 		return this.isMapped;
 	}
 
 	/**
 	 * Process a new request message through this transaction. If necessary,
 	 * this message will also be passed onto the TU.
 	 * 
 	 * @param transactionRequest
 	 *            Request to process.
 	 * @param sourceChannel
 	 *            Channel that received this message.
 	 */
 	public void processRequest(SIPRequest transactionRequest,
 			MessageChannel sourceChannel) {
 		boolean toTu = false;
 
 		// Can only process a single request directed to the
 		// transaction at a time. For a given server transaction
 		// the listener sees only one event at a time.
 
 		if (sipStack.logWriter.isLoggingEnabled()) {
 			sipStack.logWriter.logDebug("processRequest: "
 					+ transactionRequest.getFirstLine());
 			sipStack.logWriter.logDebug("tx state = " + this.getRealState());
 		}
 
 		try {
 
 			// If this is the first request for this transaction,
 			if (getRealState() == null) {
 				// Save this request as the one this
 				// transaction is handling
 				setOriginalRequest(transactionRequest);
 				this.setState(TransactionState.TRYING);
 				toTu = true;
 				this.setPassToListener();
 				// Automatic dialog support implies that this is an endpoint.
 				if (isInviteTransaction() && this.isMapped) { // JvB: also
 					// proxies need
 					// to do this
 
 					// Has side-effect of setting
 					// state to "Proceeding"
 					sendMessage(transactionRequest
 							.createResponse(100, "Trying"));
 
 				}
 				// If an invite transaction is ACK'ed while in
 				// the completed state,
 			} else if (isInviteTransaction()
 					&& TransactionState.COMPLETED == getRealState()
 					&& transactionRequest.getMethod().equals(Request.ACK)) {
 
 				// @jvB bug fix
 				this.setState(TransactionState.CONFIRMED);
 				disableRetransmissionTimer();
 				if (!isReliable()) {
 					enableTimeoutTimer(TIMER_I);
 
 				} else {
 
 					this.setState(TransactionState.TERMINATED);
 
 				}
 
 				// JvB: For the purpose of testing a TI, added a property to
 				// pass it anyway
 				if (sipStack.non2XXAckPassedToListener) {
 					// This is useful for test applications that want to see
 					// all messages.
 					requestOf.processRequest(transactionRequest, this);
 				} else {
 					// According to RFC3261 Application should not Ack in
 					// CONFIRMED state
 					if (sipStack.logWriter.isLoggingEnabled()) {
 						sipStack.logWriter
 								.logDebug("ACK received for server Tx "
 										+ this.getTransactionId()
 										+ " not delivering to application!");
 
 					}
 
 					this.semaphore.release();
 				}
 				return;
 
 				// If we receive a retransmission of the original
 				// request,
 			} else if (transactionRequest.getMethod().equals(
 					getOriginalRequest().getMethod())) {
 
 				if (TransactionState.PROCEEDING == getRealState()
 						|| TransactionState.COMPLETED == getRealState()) {
 					this.semaphore.release();
 					// Resend the last response to
 					// the client
 					if (lastResponse != null) {
 
 						// Send the message to the client
 						super.sendMessage(lastResponse);
 
 					}
 				} else if (transactionRequest.getMethod().equals(Request.ACK)) {
 					// This is passed up to the TU to suppress
 					// retransmission of OK
 					if (requestOf != null)
 						requestOf.processRequest(transactionRequest, this);
 					else
 						this.semaphore.release();
 				}
 				sipStack.logWriter
 						.logDebug("completed processing retransmitted request : "
 								+ transactionRequest.getFirstLine()
 								+ this
 								+ " txState = "
 								+ this.getState()
 								+ " lastResponse = " + this.getLastResponse());
 				return;
 
 			}
 
 			// Pass message to the TU
 			if (TransactionState.COMPLETED != getRealState()
 					&& TransactionState.TERMINATED != getRealState()
 					&& requestOf != null) {
 				if (getOriginalRequest().getMethod().equals(
 						transactionRequest.getMethod())) {
 					// Only send original request to TU once!
 					if (toTu) {
 						requestOf.processRequest(transactionRequest, this);
 					} else
 						this.semaphore.release();
 				} else {
 					if (requestOf != null)
 						requestOf.processRequest(transactionRequest, this);
 					else
 						this.semaphore.release();
 				}
 			} else {
 				// This seems like a common bug so I am allowing it through!
 				if (((SIPTransactionStack) getSIPStack())
 						.isDialogCreated(getOriginalRequest().getMethod())
 						&& getRealState() == TransactionState.TERMINATED
 						&& transactionRequest.getMethod().equals(Request.ACK)
 						&& requestOf != null) {
 					SIPDialog thisDialog = (SIPDialog) this.dialog;
 
 					if (thisDialog == null || !thisDialog.ackProcessed) {
 						// Filter out duplicate acks
 						if (thisDialog != null) {
 							thisDialog.ackReceived(transactionRequest);
 							thisDialog.ackProcessed = true;
 						}
 						requestOf.processRequest(transactionRequest, this);
 					} else {
 						this.semaphore.release();
 					}
 
 				} else if (transactionRequest.getMethod()
 						.equals(Request.CANCEL)) {
 					if (sipStack.isLoggingEnabled())
 						sipStack.logWriter
 								.logDebug("Too late to cancel Transaction");
 					this.semaphore.release();
 					// send OK and just ignore the CANCEL.
 					try {
 						this.sendMessage(transactionRequest
 								.createResponse(Response.OK));
 					} catch (IOException ex) {
 						// Transaction is already terminated
 						// just ignore the IOException.
 					}
 				}
 				sipStack.logWriter.logDebug("Dropping request "
 						+ getRealState());
 			}
 
 		} catch (IOException e) {
 			this.semaphore.release();
 			this.raiseIOExceptionEvent();
 		}
 
 	}
 
 	/**
 	 * Send a response message through this transactionand onto the client. The
 	 * response drives the state machine.
 	 * 
 	 * @param messageToSend
 	 *            Response to process and send.
 	 */
 	public void sendMessage(SIPMessage messageToSend) throws IOException {
 
 		// Message typecast as a response
 		SIPResponse transactionResponse;
 		// Status code of the response being sent to the client
 		int statusCode;
 
 		// Get the status code from the response
 		transactionResponse = (SIPResponse) messageToSend;
 		statusCode = transactionResponse.getStatusCode();
 
 		try {
 			// Provided we have set the banch id for this we set the BID for the
 			// outgoing via.
 			if (this.getOriginalRequest().getTopmostVia().getBranch() != null)
 				transactionResponse.getTopmostVia().setBranch(this.getBranch());
 			else
 				transactionResponse.getTopmostVia().removeParameter(
 						ParameterNames.BRANCH);
 
 			// Make the topmost via headers match identically for the
 			// transaction
 			// rsponse.
 			if (!this.getOriginalRequest().getTopmostVia().hasPort())
 				transactionResponse.getTopmostVia().removePort();
 		} catch (ParseException ex) {
 			ex.printStackTrace();
 		}
 
 		// Method of the response does not match the request used to
 		// create the transaction - transaction state does not change.
 		if (!transactionResponse.getCSeq().getMethod().equals(
 				getOriginalRequest().getMethod())) {
 			sendResponse(transactionResponse);
 			return;
 		}
 
 		// If the TU sends a provisional response while in the
 		// trying state,
 		if (getRealState() == TransactionState.TRYING) {
 			if (statusCode / 100 == 1) {
 				this.setState(TransactionState.PROCEEDING);
 			} else if (200 <= statusCode && statusCode <= 699) {
 				
 				// JvB: INVITE ST can never be TRYING, so this is always true
 				// if (!isInviteTransaction()) {
 					this.setState(TransactionState.COMPLETED);
 				/*} else {
 					if (statusCode / 100 == 2) {
 						this.setState(TransactionState.TERMINATED);
 					} else
 						this.setState(TransactionState.COMPLETED);
 				}*/
 				if (!isReliable()) {
 					enableRetransmissionTimer();
 				}
 				enableTimeoutTimer(TIMER_J);
 			}
 
 			// If the transaction is in the proceeding state,
 		} else if (getRealState() == TransactionState.PROCEEDING) {
 
 			if (isInviteTransaction()) {
 
 				// If the response is a failure message,
 				if (statusCode / 100 == 2) {
 					// Set up to catch returning ACKs
 					// Do NOT change the
 					// transaction state if this
 					// is a response for a CANCEL.
 					// Wait, instead for the 487 from TU.
 					if (!transactionResponse.getCSeq().getMethod().equals(
 							Request.CANCEL)) {
 
 						this.collectionTime = TIMER_J;
 						this.setState(TransactionState.TERMINATED);
 						if (!isReliable()) {
 							// test for dialog existance
 							if (this.dialog != null)
 								this.dialog.setRetransmissionTicks();
 							enableRetransmissionTimer();
 
 						}
 						enableTimeoutTimer(TIMER_J);
 					}
 				} else if (300 <= statusCode && statusCode <= 699) {
 
 					// Set up to catch returning ACKs
 					this.setState(TransactionState.COMPLETED);
 					if (!isReliable()) {
 						/*
 						 * While in the "Proceeding" state, if the TU passes a
 						 * response with status code from 300 to 699 to the
 						 * server transaction, the response MUST be passed to
 						 * the transport layer for transmission, and the state
 						 * machine MUST enter the "Completed" state. For
 						 * unreliable transports, timer G is set to fire in T1
 						 * seconds, and is not set to fire for reliable
 						 * transports.
 						 */
 
 						enableRetransmissionTimer();
 
 					}
 					enableTimeoutTimer(TIMER_H);
 
 				} else if (statusCode / 100 == 2) {
 					// If the response is a success message,
 					// Terminate the transaction
 					this.setState(TransactionState.TERMINATED);
 					disableRetransmissionTimer();
 					disableTimeoutTimer();
 
 				}
 
 				// If the transaction is not an invite transaction
 				// and this is a final response,
 			} else if (200 <= statusCode && statusCode <= 699) {
 
 				// Set up to retransmit this response,
 				// or terminate the transaction
 				this.setState(TransactionState.COMPLETED);
 				if (!isReliable()) {
 
 					disableRetransmissionTimer();
 					enableTimeoutTimer(TIMER_J);
 
 				} else {
 
 					this.setState(TransactionState.TERMINATED);
 
 				}
 
 			}
 
 			// If the transaction has already completed,
 		} else if (TransactionState.COMPLETED == this.getRealState()) {
 
 			return;
 		}
 		try {
 			// Send the message to the client.
 			// Record the last message sent out.
 			if (sipStack.getLogWriter().isLoggingEnabled()) {
 				sipStack.getLogWriter().logDebug(
 						"sendMessage : tx = " + this + " getState = "
 								+ this.getState());
 			}
 			lastResponse = transactionResponse;
 			this.sendResponse(transactionResponse);
 
 		} catch (IOException e) {
 
 			this.setState(TransactionState.TERMINATED);
 			this.collectionTime = 0;
 			throw e;
 
 		}
 
 	}
 
 	public String getViaHost() {
 
 		return getMessageChannel().getViaHost();
 
 	}
 
 	public int getViaPort() {
 
 		return getMessageChannel().getViaPort();
 
 	}
 
 	/**
 	 * Called by the transaction stack when a retransmission timer fires. This
 	 * retransmits the last response when the retransmission filter is enabled.
 	 */
 	protected void fireRetransmissionTimer() {
 
 		try {
 			if (sipStack.getLogWriter().isLoggingEnabled()) {
 				sipStack.getLogWriter().logDebug(
 						"fireRetransmissionTimer() -- ");
 			}
 			// Resend the last response sent by this transaction
 			if (isInviteTransaction() && lastResponse != null) {
 				// null can happen if this is terminating when the timer fires.
 				if (!this.retransmissionAlertEnabled) {
 					super.sendMessage(lastResponse);
 				} else {
 					// alert the application to retransmit the last response
 					SipProviderImpl sipProvider = (SipProviderImpl) this
 							.getSipProvider();
 					TimeoutEvent txTimeout = new TimeoutEvent(sipProvider,
 							this, Timeout.RETRANSMIT);
 					sipProvider.handleEvent(txTimeout, this);
 				}
 
 			}
 		} catch (IOException e) {
 			if (sipStack.isLoggingEnabled())
 				sipStack.logWriter.logException(e);
 			raiseErrorEvent(SIPTransactionErrorEvent.TRANSPORT_ERROR);
 
 		}
 
 	}
 
 	private void fireReliableResponseRetransmissionTimer() {
 		try {
 
 			super.sendMessage(this.pendingReliableResponse);
 
 		} catch (IOException e) {
 			if (sipStack.isLoggingEnabled())
 				sipStack.logWriter.logException(e);
 			this.setState(TransactionState.TERMINATED);
 			raiseErrorEvent(SIPTransactionErrorEvent.TRANSPORT_ERROR);
 
 		}
 	}
 
 	/**
 	 * Called by the transaction stack when a timeout timer fires.
 	 */
 	protected void fireTimeoutTimer() {
 
 		if (sipStack.isLoggingEnabled())
 			sipStack.logWriter
 					.logDebug("SIPServerTransaction.fireTimeoutTimer this = "
 							+ this + " current state = " + this.getRealState()
 							+ " method = "
 							+ this.getOriginalRequest().getMethod());
 
 		SIPDialog dialog = (SIPDialog) this.dialog;
 		if (((SIPTransactionStack) getSIPStack()).isDialogCreated(this
 				.getOriginalRequest().getMethod())
 				&& (TransactionState.CALLING == this.getRealState() || TransactionState.TRYING == this
 						.getRealState())) {
 			dialog.setState(SIPDialog.TERMINATED_STATE);
 		} else if (getOriginalRequest().getMethod().equals(Request.BYE)) {
 			if (dialog != null && dialog.isTerminatedOnBye())
 				dialog.setState(SIPDialog.TERMINATED_STATE);
 		}
 
 		if (TransactionState.COMPLETED == this.getRealState()
 				&& isInviteTransaction()) {
 			raiseErrorEvent(SIPTransactionErrorEvent.TIMEOUT_ERROR);
 			this.setState(TransactionState.TERMINATED);
 			sipStack.removeTransaction(this);
 
 		} else if (TransactionState.COMPLETED == this.getRealState()
 				&& !isInviteTransaction()) {
 			this.setState(TransactionState.TERMINATED);
 			sipStack.removeTransaction(this);
 
 		} else if (TransactionState.CONFIRMED == this.getRealState()
 				&& isInviteTransaction()) {
 			// TIMER_I should not generate a timeout
 			// exception to the application when the
 			// Invite transaction is in Confirmed state.
 			// Just transition to Terminated state.
 			this.setState(TransactionState.TERMINATED);
 			sipStack.removeTransaction(this);
 		} else if (!isInviteTransaction()
 				&& (TransactionState.COMPLETED == this.getRealState() || TransactionState.CONFIRMED == this
 						.getRealState())) {
 			this.setState(TransactionState.TERMINATED);
 		} else if (isInviteTransaction()
 				&& TransactionState.TERMINATED == this.getRealState()) {
 			// This state could be reached when retransmitting
 
 			raiseErrorEvent(SIPTransactionErrorEvent.TIMEOUT_ERROR);
 			if (dialog != null)
 				dialog.setState(SIPDialog.TERMINATED_STATE);
 		}
 
 	}
 
 	/**
 	 * Get the last response.
 	 */
 	public SIPResponse getLastResponse() {
 		return this.lastResponse;
 	}
 
 	/**
 	 * Set the original request.
 	 */
 	public void setOriginalRequest(SIPRequest originalRequest) {
 		super.setOriginalRequest(originalRequest);
 
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see javax.sip.ServerTransaction#sendResponse(javax.sip.message.Response)
 	 */
 	public void sendResponse(Response response) throws SipException {
 		SIPResponse sipResponse = (SIPResponse) response;
 
 		SIPDialog dialog = this.dialog;
 		if (response == null)
 			throw new NullPointerException("null response");
 
 		try {
 			sipResponse.checkHeaders();
 		} catch (ParseException ex) {
 			throw new SipException(ex.getMessage());
 		}
 
 		// check for meaningful response.
 		if (!sipResponse.getCSeq().getMethod().equals(this.getMethod())) {
 			throw new SipException(
 					"CSeq method does not match Request method of request that created the tx.");
 		}
 
 		/*
 		 * 200-class responses to SUBSCRIBE requests also MUST contain an
 		 * "Expires" header. The period of time in the response MAY be shorter
 		 * but MUST NOT be longer than specified in the request.
 		 */
 		if (this.getMethod().equals(Request.SUBSCRIBE)
 				&& response.getStatusCode() / 100 == 2) {
 
 			if (response.getHeader(ExpiresHeader.NAME) == null) {
 				throw new SipException(
 						"Expires header is mandatory in 2xx response of SUBSCRIBE");
 			} else {
 				Expires requestExpires = (Expires) this.getOriginalRequest()
 						.getExpires();
 				Expires responseExpires = (Expires) response.getExpires();
 				/*
 				 * If no "Expires" header is present in a SUBSCRIBE request, the
 				 * implied default is defined by the event package being used.
 				 */
 				if (requestExpires != null
 						&& responseExpires.getExpires() > requestExpires
 								.getExpires()) {
 					throw new SipException(
 							"Response Expires time exceeds request Expires time : See RFC 3265 3.1.1");
 				}
 			}
 
 		}
 
 		// Check for mandatory header.
 		if (sipResponse.getStatusCode() == 200
 				&& sipResponse.getCSeq().getMethod().equals(Request.INVITE)
 				&& sipResponse.getHeader(ContactHeader.NAME) == null)
 			throw new SipException(
 					"Contact Header is mandatory for the OK to the INVITE");
 
 		if (!this.isMessagePartOfTransaction((SIPMessage) response)) {
 			throw new SipException(
 					"Response does not belong to this transaction.");
 		}
 
 		// Fix up the response if the dialog has already been established.
 		try {
 			/*
 			 * TODO Check this. The UAS MAY send a final response to the initial
 			 * request before having received PRACKs for all unacknowledged
 			 * reliable provisional responses, unless the final response is 2xx
 			 * and any of the unacknowledged reliable provisional responses
 			 * contained a session description. In that case, it MUST NOT send a
 			 * final response until those provisional responses are
 			 * acknowledged.
 			 */
 			if (this.pendingReliableResponse != null
 					&& response.getStatusCode() / 100 == 2
 					&& this.pendingReliableResponse.getContentTypeHeader()
 							.getContentType().equals("application")
 					&& this.pendingReliableResponse.getContentTypeHeader()
 							.getContentSubType().equals("sdp")) {
 				throw new SipException(
 						"cannot send response -- unacked povisional");
 			} else {
 				// Sending the final response cancels the
 				// pending response task.
 				if (this.pendingReliableResponse != null
 						&& sipResponse.isFinalResponse()) {
 					this.provisionalResponseTask.cancel();
 					this.provisionalResponseTask = null;
 				}
 			}
 
 			// Dialog checks. These make sure that the response
 			// being sent makes sense.
 			if (dialog != null) {
 				if (sipResponse.getStatusCode() / 100 == 2
 						&& sipStack.isDialogCreated(sipResponse.getCSeq()
 								.getMethod())) {
 					if (dialog.getLocalTag() == null
 							&& sipResponse.getTo().getTag() == null) {
 						throw new SipException("To tag must be set for OK");
 					} else if (dialog.getLocalTag() != null
 							&& sipResponse.getToTag() == null) {
 						sipResponse.setToTag(dialog.getLocalTag());
 					} else if (dialog.getLocalTag() != null
 							&& sipResponse.getToTag() != null
 							&& !dialog.getLocalTag().equals(
 									sipResponse.getToTag())) {
 						throw new SipException("Tag mismatch dialogTag is "
 								+ dialog.getLocalTag() + " responseTag is "
 								+ sipResponse.getToTag());
 					}
 				}
 
 				if (!sipResponse.getCallId().getCallId().equals(
 						dialog.getCallId().getCallId())) {
 					throw new SipException("Dialog mismatch!");
 				}
 			}
 
 			// If sending the response within an established dialog, then
 			// set up the tags appropriately. Should probably throw exception
 			// here if tags do not match instead of setting the tags??
 			// lets be nice and set the tag if the user forgot to do it.
 
 			if (dialog != null && dialog.getLocalTag() != null
 					&& sipResponse.getTo().getTag() == null
 					&& sipResponse.getStatusCode() != 100)
 				sipResponse.getTo().setTag(dialog.getLocalTag());
 
 			// Backward compatibility slippery slope....
 			// Only set the from tag in the response when the
 			// incoming request has a from tag.
 			String fromTag = ((SIPRequest) this.getRequest()).getFrom()
 					.getTag();
 			if (fromTag != null)
 				sipResponse.getFrom().setTag(fromTag);
 			else {
 				if (sipStack.isLoggingEnabled())
 					sipStack.logWriter
 							.logDebug("WARNING -- Null From tag  Dialog layer in jeopardy!!");
 			}
 
 			// See if the dialog needs to be inserted into the dialog table
 			// or if the state of the dialog needs to be changed.
 			if (dialog != null && response.getStatusCode() != 100) {
 				if (!dialog.checkResponseTags(sipResponse))
 					throw new SipException(
 							"Response tags dont match with Dialog tags");
 
 				DialogState oldState = dialog.getState();
 				dialog.setLastResponse(this, (SIPResponse) response);
 				if (oldState == null
 						&& dialog.getState() == DialogState.TERMINATED) {
 					DialogTerminatedEvent event = new DialogTerminatedEvent(
 							dialog.getSipProvider(), dialog);
 
 					// Provide notification to the listener that the dialog has
 					// ended.
 					dialog.getSipProvider().handleEvent(event, this);
 
 				}
 
 			} else if (dialog == null
 					&& this.getMethod().equals(Request.INVITE)
 					&& this.retransmissionAlertEnabled
 					&& this.retransmissionAlertTimerTask == null
 					&& response.getStatusCode() / 100 == 2) {
 				String dialogId = ((SIPResponse) response).getDialogId(true);
 
 				this.retransmissionAlertTimerTask = new RetransmissionAlertTimerTask(
 						dialogId);
 				sipStack.retransmissionAlertTransactions.put(dialogId, this);
 				sipStack.timer.schedule(this.retransmissionAlertTimerTask, 0,
 						SIPTransactionStack.BASE_TIMER_INTERVAL);
 
 			}
 
 			// Send message after possibly inserting the Dialog
 			// into the dialog table to avoid a possible race condition.
 
 			this.sendMessage((SIPResponse) response);
 
 		} catch (IOException ex) {
 			throw new SipException(ex.getMessage());
 		} catch (java.text.ParseException ex1) {
 			throw new SipException(ex1.getMessage());
 		}
 	}
 
 	/**
 	 * Return the book-keeping information that we actually use.
 	 */
 	private TransactionState getRealState() {
 		return super.getState();
 	}
 
 	/**
 	 * Return the current transaction state according to the RFC 3261
 	 * transaction state machine. Invite transactions do not have a trying
 	 * state. We just use this as a pseudo state for processing requests.
 	 * 
 	 * @return the state of the transaction.
 	 */
 	public TransactionState getState() {
 		// Trying is a pseudo state for INVITE transactions.
 		if (this.isInviteTransaction()
 				&& TransactionState.TRYING == super.getState())
 			return TransactionState.PROCEEDING;
 		else
 			return super.getState();
 	}
 
 	/**
 	 * Sets a timeout after which the connection is closed (provided the server
 	 * does not use the connection for outgoing requests in this time period)
 	 * and calls the superclass to set state.
 	 */
 	public void setState(TransactionState newState) {
 		// Set this timer for connection caching
 		// of incoming connections.
 		if (newState == TransactionState.TERMINATED && this.isReliable()
 				&& (!getSIPStack().cacheServerConnections)) {
 			// Set a time after which the connection
 			// is closed.
 			this.collectionTime = TIMER_J;
 		}
 
 		super.setState(newState);
 
 	}
 
 	/**
 	 * Start the timer task.
 	 */
 	protected void startTransactionTimer() {
 		if (sipStack.timer != null) {
 			// The timer is set to null when the Stack is
 			// shutting down.
 			TimerTask myTimer = new TransactionTimer();
 			sipStack.timer.schedule(myTimer, 0,
 					SIPTransactionStack.BASE_TIMER_INTERVAL);
 		}
 	}
 
 	public boolean equals(Object other) {
 		if (!other.getClass().equals(this.getClass())) {
 			return false;
 		}
 		SIPServerTransaction sst = (SIPServerTransaction) other;
 		return this.getBranch().equalsIgnoreCase(sst.getBranch());
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see gov.nist.javax.sip.stack.SIPTransaction#getDialog()
 	 */
 	public Dialog getDialog() {
 
 		return this.dialog;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see gov.nist.javax.sip.stack.SIPTransaction#setDialog(gov.nist.javax.sip.stack.SIPDialog,
 	 *      gov.nist.javax.sip.message.SIPMessage)
 	 */
 	public void setDialog(SIPDialog sipDialog, String dialogId) {
 		if (sipStack.logWriter.isLoggingEnabled())
 			this.sipStack.logWriter.logDebug("setDialog " + this + " dialog = "
 					+ sipDialog);
 		this.dialog = sipDialog;
		this.dialog.setAssigned();
 		if (this.retransmissionAlertEnabled
 				&& this.retransmissionAlertTimerTask != null) {
 			this.retransmissionAlertTimerTask.cancel();
 			this.retransmissionAlertTimerTask = null;
 			sipStack.retransmissionAlertTransactions
 					.remove(this.retransmissionAlertTimerTask.dialogId);
 
 		}
 		this.retransmissionAlertEnabled = false;
 
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see javax.sip.Transaction#terminate()
 	 */
 	public void terminate() throws ObjectInUseException {
 		this.setState(TransactionState.TERMINATED);
 		if (this.retransmissionAlertTimerTask != null) {
 			this.retransmissionAlertTimerTask.cancel();
 			this.retransmissionAlertTimerTask = null;
 			this.sipStack.retransmissionAlertTransactions
 					.remove(retransmissionAlertTimerTask.dialogId);
 
 		}
 
 	}
 
 	protected void sendReliableProvisionalResponse(Response relResponse)
 			throws SipException {
 
 		/*
 		 * After the first reliable provisional response for a request has been
 		 * acknowledged, the UAS MAY send additional reliable provisional
 		 * responses. The UAS MUST NOT send a second reliable provisional
 		 * response until the first is acknowledged.
 		 */
 		if (this.pendingReliableResponse != null) {
 			throw new SipException("Unacknowledged response");
 
 		} else
 			this.pendingReliableResponse = (SIPResponse) relResponse;
 		/*
 		 * In addition, it MUST contain a Require header field containing the
 		 * option tag 100rel, and MUST include an RSeq header field.
 		 */
 		RSeq rseq = (RSeq) relResponse.getHeader(RSeqHeader.NAME);
 		if (relResponse.getHeader(RSeqHeader.NAME) == null) {
 			rseq = new RSeq();
 			relResponse.setHeader(rseq);
 		}
 
 		try {
 			this.rseqNumber++;
 			rseq.setSequenceNumber(this.rseqNumber);
 
 			// start the timer task which will retransmit the reliable response
 			// until the PRACK is received
 			this.lastResponse = (SIPResponse) relResponse;
 			this.sendMessage((SIPMessage) relResponse);
 			this.provisionalResponseTask = new ProvisionalResponseTask();
 			this.sipStack.timer.schedule(provisionalResponseTask, 0,
 					SIPTransactionStack.BASE_TIMER_INTERVAL);
 
 		} catch (Exception ex) {
 			InternalErrorHandler.handleException(ex);
 		}
 
 	}
 
 	public SIPResponse getReliableProvisionalResponse() {
 
 		return this.pendingReliableResponse;
 	}
 
 	/**
 	 * Cancel the retransmit timer for the provisional response task.
 	 * 
 	 * @return true if the tx has seen the prack for the first time and false
 	 *         otherwise.
 	 * 
 	 */
 	public boolean prackRecieved() {
 
 		if (this.pendingReliableResponse == null)
 			return false;
 		this.provisionalResponseTask.cancel();
 		this.pendingReliableResponse = null;
 		return true;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see javax.sip.ServerTransaction#enableRetransmissionAlerts()
 	 */
 
 	public void enableRetransmissionAlerts() throws SipException {
 		if (this.getDialog() != null)
 			throw new SipException("Dialog associated with tx");
 
 		else if (!this.getMethod().equals(Request.INVITE))
 			throw new SipException("Request Method must be INVITE");
 
 		this.retransmissionAlertEnabled = true;
 
 	}
 
 	public boolean isRetransmissionAlertEnabled() {
 		return this.retransmissionAlertEnabled;
 	}
 
 	/**
 	 * Disable retransmission Alerts and cancel associated timers.
 	 * 
 	 */
 	public void disableRetransmissionAlerts() {
 		if (this.retransmissionAlertTimerTask != null
 				&& this.retransmissionAlertEnabled) {
 			this.retransmissionAlertTimerTask.cancel();
 			this.retransmissionAlertEnabled = false;
 			this.retransmissionAlertTimerTask = null;
 			String dialogId = this.retransmissionAlertTimerTask.dialogId;
 			sipStack.retransmissionAlertTransactions.remove(dialogId);
 		}
 	}
 
 	/**
 	 * This is book-keeping for retransmission filter management.
 	 */
 	public void setAckSeen() {
 		this.isAckSeen = true;
 	}
 
 	/**
 	 * This is book-keeping for retransmission filter management.
 	 */
 	public boolean ackSeen() {
 		return this.isAckSeen;
 	}
 
 	public void setMapped(boolean b) {
 		this.isMapped = true;
 
 	}
 
 	public void setPendingSubscribe(
 			SIPClientTransaction pendingSubscribeClientTx) {
 		this.pendingSubscribeTransaction = pendingSubscribeClientTx;
 
 	}
 
 	public void releaseSem() {
 		if (this.pendingSubscribeTransaction != null) {
 			/*
 			 * When a notify is being processed we take a lock on the subscribe
 			 * to avoid racing with the OK of the subscribe.
 			 */
 			pendingSubscribeTransaction.releaseSem();
 		} else if (this.inviteTransaction != null && this.getMethod().equals(Request.CANCEL)) {
 			/*
 			 * When a CANCEL is being processed we take a nested lock on the
 			 * associated INVITE server tx.
 			 */
 			this.inviteTransaction.releaseSem();
 		}
 		super.releaseSem();
 	}
 
 	/**
 	 * The INVITE Server Transaction corresponding to a CANCEL Server
 	 * Transaction.
 	 * 
 	 * @param st --
 	 *            the invite server tx corresponding to the cancel server
 	 *            transaction.
 	 */
 	public void setInviteTransaction(SIPServerTransaction st) {
 		this.inviteTransaction = st;
 
 	}
 
 }
