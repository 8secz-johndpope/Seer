 package gov.nist.javax.sip.stack;
 
 import gov.nist.javax.sip.parser.*;
 import gov.nist.javax.sip.header.*;
 import gov.nist.javax.sip.address.*;
 import gov.nist.javax.sip.message.*;
 import gov.nist.javax.sip.*;
 import gov.nist.core.*;
 
 import java.io.IOException;
 import java.util.*;
 import java.net.InetAddress;
 import java.text.ParseException;
 
 import javax.sip.*;
 import javax.sip.message.*;
 
 
 /**
  *	Abstract class to support both client and server transactions.  
  * Provides an encapsulation of a message channel, handles timer events, 
  * and creation of the Via header for a message.
  *
  *	@author	Jeff Keyser 
  *      @author M. Ranganathan 
  * (modified Jeff's original source and aligned   with JAIN-SIP 1.1)
  *      @version JAIN-SIP-1.1
  *
  */
 public abstract class SIPTransaction
 	extends MessageChannel implements javax.sip.Transaction {
 
 	
             
             protected static final int BASE_TIMER_INTERVAL = 
                         SIPTransactionStack.BASE_TIMER_INTERVAL;
             
             
             /** One timer tick. */
             protected static final int T1 = 1;
             
              /** 5 sec Maximum duration a message will remain in the network  */         
 	    protected static final int T4 =  5000/BASE_TIMER_INTERVAL;
             
             
             /** The maximum retransmit interval for non-INVITE
             requests and INVITE responses */
             
 	    protected static final int T2 = 4000/BASE_TIMER_INTERVAL;
             
             
             /** INVITE request retransmit interval, for UDP only */
 	    protected static final int TIMER_A = 1;
 
             /** INVITE transaction  timeout timer */
 	    protected static final int TIMER_B = 64;
 	    
 	    protected static final int TIMER_J = 64;
 
 	    protected static final int TIMER_F = 64;
 
 	    protected static final int TIMER_H = 64;
 
 	    protected static final int TIMER_I = T4;
 
 	    protected static final int TIMER_K = T4;
 	
 	    protected static final int TIMER_D = 32000/BASE_TIMER_INTERVAL ;
 
 	    protected static final int  TIMER_C = 3*60*1000/BASE_TIMER_INTERVAL;
             
             protected SIPResponse lastResponse;
             
             protected  DialogImpl  dialog;
 
 	    protected boolean isMapped;
             
             
         /** Initialized but no state assigned.
          */
          public static final int INITIAL_STATE = -1;
 
 	/**
 	 *	Trying state.
 	 */
 	public static final int	TRYING_STATE = TransactionState._TRYING;
  	
 	/** CALLING State.
 	*/
 	public static final int	CALLING_STATE = TransactionState._CALLING;
 	/** 	Proceeding state.
 	 */
 	public static final int	PROCEEDING_STATE = TransactionState._PROCEEDING;
 	/**	Completed state.
 	 */
 	public static final int	COMPLETED_STATE = TransactionState._COMPLETED;
 	/**	Confirmed state.
 	 */
 	public static final int	CONFIRMED_STATE = TransactionState._CONFIRMED;
 	/** 	Terminated state.  
 	*/
 	public static final int	TERMINATED_STATE = TransactionState._TERMINATED;
 	/**
 	 *	Maximum number of ticks between retransmissions.
 	 */
 	protected static final int	MAXIMUM_RETRANSMISSION_TICK_COUNT = 8;
 
 	// Parent stack for this transaction
 	protected SIPTransactionStack	parentStack;
 	// Original request that is being handled by this transaction
 	protected  SIPRequest			originalRequest;
 	// Underlying channel being used to send messages for this transaction
 	protected MessageChannel   encapsulatedChannel;
 	// Transaction branch ID
 	private String			branch;
 
 	// Method of the Request used to create the transaction.
 	private String 			method;
 	// Sequence number of request used to create the transaction
 	private int			cSeq;
 
 	// Current transaction state
 	private int			currentState;
 	// Number of ticks the retransmission timer was set to last
 	private int		retransmissionTimerLastTickCount;
 	// Number of ticks before the message is retransmitted
 	private int		retransmissionTimerTicksLeft;
 	// Number of ticks before the transaction times out
 	private int		timeoutTimerTicksLeft;
 	// List of event listeners for this transaction
 	private Set		eventListeners;
      
 	// Flag to indcate that this has been cancelled.
 	protected boolean	isCancelled;
 
 	// Back ptr to the JAIN layer.
 	private Object		wrapper;
 
 
 
 	public String getBranchId() { return this.branch; }
 
        
 
 
 	/**
 	 *	Transaction constructor.
 	 *
 	 *	@param newParentStack Parent stack for this transaction.
 	 *	@param newEncapsulatedChannel 
 	 * 		Underlying channel for this transaction.
 	 */
 	protected SIPTransaction(
 		SIPTransactionStack	newParentStack,
 		MessageChannel		newEncapsulatedChannel
 	) {
 
 		parentStack = newParentStack;
 		encapsulatedChannel = newEncapsulatedChannel;
                 
                 
                 this.currentState = INITIAL_STATE;
 		
 		disableRetransmissionTimer( );
 		disableTimeoutTimer( );
 		eventListeners = Collections.synchronizedSet( new HashSet( ) );
 
 		// Always add the parent stack as a listener 
 		// of this transaction
 		addEventListener( newParentStack );
 
 	}
 
 
 	/**
 	 *	Sets the request message that this transaction handles.
 	 *
 	 *	@param newOriginalRequest Request being handled.
 	 */
 	public void setOriginalRequest(
 		SIPRequest	newOriginalRequest
 	) {
 
 		// Branch value of topmost Via header
 		String	newBranch;
 
 
 		this.originalRequest = newOriginalRequest;
 
 		this.method = newOriginalRequest.getMethod();
 		
 		this.cSeq = 
 			newOriginalRequest.getCSeq().getSequenceNumber();
 
 		originalRequest.setTransaction(this);
 
 		// If the message has an explicit branch value set,
 		newBranch = ( (Via)newOriginalRequest.getViaHeaders( ).
 				getFirst( ) ).getBranch( );
 		if( newBranch != null ) {
 			if (parentStack.logWriter.needsLogging) 
 			   parentStack.logWriter.logMessage("Setting Branch id : " 
 				+ newBranch);
 
 			// Override the default branch with the one 
 			// set by the message
 			setBranch( newBranch );
 
 		} else {
 			if (parentStack.logWriter.needsLogging)
 			    parentStack.logWriter.logMessage
 				("Branch id is null - compute TID!"
 				+ newOriginalRequest.encode());
 			setBranch(newOriginalRequest.getTransactionId());
 		}
 
 	}
 
 
 	/**
 	 *	Gets the request being handled by this transaction.
 	 *
 	 *	@return Request being handled.
 	 */
 	public  SIPRequest getOriginalRequest(
 	) {
 
 		return originalRequest;
 
 	}
 
 	/** Get the original request but cast to a Request structure.
 	*
 	* @return the request that generated this transaction.
 	*/
 	public Request getRequest() {
 		return (Request) originalRequest;
 	}
 
 
 	/**
 	 *  Returns a flag stating whether this transaction is for an 
 	 *	INVITE request or not.
 	 *
 	 *	@return True if this is an INVITE request, false if not.
 	 */
 	protected final boolean isInviteTransaction(
 	) {
 
 		return getMethod( ).equals ( Request.INVITE );
 
 	}
 
 	/** Return true if the transaction corresponds to a CANCEL message.
 	*
 	*@return true if the transaciton is a CANCEL transaction.
 	*/
 	protected final boolean isCancelTransaction(
 	) {
 
 		return getMethod( ).equals
 				( Request.CANCEL );
 
 	}
 
 	/** Return a flag that states if this is a BYE transaction.
 	*
 	*@return true if the transaciton is a BYE transaction.
 	*/
 	protected final boolean isByeTransaction(
 	) {
 		return getMethod( ).equals( Request.BYE );
 	}
 		
 
 
 	/**
 	 *  Returns the message channel used for 
 	 * 		transmitting/receiving messages
 	 * for this transaction. Made public in support of JAIN dual 
 	 * transaction model.
 	 *
 	 *	@return Encapsulated MessageChannel.
 	 *
 	 */
 	public MessageChannel getMessageChannel(
 	) {
 
 		return encapsulatedChannel;
 
 	}
 
 
 	/**
 	 *	Sets the Via header branch parameter used to identify 
 	 * this transaction.
 	 *
 	 *@param newBranch New string used as the branch 
 	 * 	for this transaction.
 	 */
 	public final void setBranch(
 		String	newBranch
 	) {
 
 		branch = newBranch;
 
 	}
 
 
 	/**
 	 *Gets the current setting for the branch parameter of this transaction.
 	 *
 	 *@return Branch parameter for this transaction.
 	 */
 	public final String getBranch(
 	) {
 		if (this.branch == null) {
 		    this.branch = 
 		    getOriginalRequest().getTopmostVia().getBranch();
 		}
 		return branch;
 
 	}
 
 	/** Get the method of the request used to create this transaction.
 	*
 	*@return the method of the request for the transaction.
 	*/
 	public final String getMethod() { return this.method; }
 
 
 	/** Get the Sequence number of the request used to create the 
 	* transaction.
 	*
 	*@return the cseq of the request used to create the transaction.
 	*/
 	public final int getCSeq() {
 		return this.cSeq;
 	}
 
 
 	/**
 	 *Changes the state of this transaction.
 	 *
 	 *@param newState New state of this transaction.
 	 */
 	public void setState(
 		int		newState
 	) {
 
 		currentState = newState;
 		if (parentStack.logWriter.needsLogging)  {
 		    parentStack.logWriter.logMessage
 		    ("Transaction:setState " + newState +  " " + this );
 		    parentStack.logWriter.logStackTrace();
 		}
 
 
 	}
 
 	
 
 
 	/**
 	 *	Gets the current state of this transaction.
 	 *
 	 *	@return Current state of this transaction.
 	 */
 	public final TransactionState getState() {
 	    if (this.currentState ==  INITIAL_STATE ) return null;
 	    else return TransactionState.getObject(this.currentState);
                    
 
 	}
 
 
 	/**
 	 *Enables retransmission timer events for this transaction to begin in
 	 * one tick.
 	 */
 	protected final void enableRetransmissionTimer(
 	) {
 
 		// Changed this to 2 on request from Joseph Cheung 
 		enableRetransmissionTimer( 1 );
 
 	}
 
 
 	/**
 	 *Enables retransmission timer events for this 
 	 * transaction to begin after the number of ticks passed to 
 	 * this routine.
 	 *
 	 *@param tickCount Number of ticks before the 
 	 * 	next retransmission timer
 	 *	event occurs.
 	 */
 	protected final void enableRetransmissionTimer(
 		int		tickCount
 	) {
 
 		retransmissionTimerTicksLeft = 
 		Math.min( tickCount, MAXIMUM_RETRANSMISSION_TICK_COUNT );
 		retransmissionTimerLastTickCount = 
 		retransmissionTimerTicksLeft;
 
 	}
 
 
 	/**
 	 *	Turns off retransmission events for this transaction.
 	 */
 	protected final void disableRetransmissionTimer(
 	) {
 
 		retransmissionTimerTicksLeft = -1;
 
 	}
 
 
 	/**
 	 *Enables a timeout event to occur for this transaction after the number
 	 * of ticks passed to this method.
 	 *
 	 *@param tickCount Number of ticks before this transaction times out.
 	 */
 	protected final void enableTimeoutTimer(
 		int		tickCount
 	) {
 		if (parentStack.logWriter.needsLogging) 
 		    parentStack.logWriter.logMessage("enableTimeoutTimer " +
 			this + " tickCount " +
 			   tickCount  + 
 			" currentTickCount = " + timeoutTimerTicksLeft);
 
 
 		timeoutTimerTicksLeft = tickCount;
 
 	}
 
 
 	/**
 	 *	Disabled the timeout timer.
 	 */
 	protected final void disableTimeoutTimer(
 	) {
 
 		timeoutTimerTicksLeft = -1;
 
 	}
 
 
 	/**
 	 *	Fired after each timer tick.  
 	 * Checks the retransmission and timeout
 	 * timers of this transaction, and fired these events 
 	 * if necessary.
 	 */
	final void fireTimer(
 	) {
 		// If the timeout timer is enabled,
 		if( timeoutTimerTicksLeft != -1 ) {
 
 			// Count down the timer, and if it has run out,
 			if( --timeoutTimerTicksLeft == 0 ) {
 
 				// Fire the timeout timer
 				fireTimeoutTimer( );
 
 			}
 
 		}
 
 		// If the retransmission timer is enabled,
 		if( retransmissionTimerTicksLeft != -1 ) {
 
 			// Count down the timer, and if it has run out,
 			if( --retransmissionTimerTicksLeft == 0 ) {
 
 				// Enable this timer to fire again after 
 				// twice the original time
 				enableRetransmissionTimer
 				( retransmissionTimerLastTickCount * 2 );
 
 				// Fire the timeout timer
 				fireRetransmissionTimer( );
 
 			}
 
 		}
 
 	}
 
 
 	/**
 	 *	Tests a message to see if it is part of this transaction.
 	 *
 	 *	@return True if the message is part of this 
 	 * 		transaction, false if not.
 	 */
 	public abstract boolean isMessagePartOfTransaction(
 		SIPMessage	messageToTest
 	);
 
 
 	/**
 	 *	This method is called when this transaction's 
 	 * retransmission timer has fired.
 	 */
 	protected abstract void fireRetransmissionTimer(
 	);
 
 
 	/**
 	 *	This method is called when this transaction's 
 	 * timeout timer has fired.
 	 */
 	protected abstract void fireTimeoutTimer(
 	);
 
 
 	/**
 	 *	Tests if this transaction has terminated.
 	 *
 	 *	@return Trus if this transaction is terminated, false if not.
 	 */
 	protected final boolean isTerminated(
 	) {
 
 		return getState() != null &&
 		      getState( ).getValue() == TERMINATED_STATE;
 
 	}
 
 
 	public String getHost(
 	) {
 
 		return encapsulatedChannel.getHost( );
 
 	}
 
 
 	public String getKey(
 	) {
 
 		return encapsulatedChannel.getKey( );
 
 	}
 
 
 	public int getPort(
 	) {
 
 		return encapsulatedChannel.getPort( );
 
 	}
 
 
 	public SIPStack getSIPStack(
 	) {
 
 		return parentStack;
 
 	}
 
 
 	public String getPeerAddress(
 	) {
 
 		return encapsulatedChannel.getPeerAddress( );
 
 	}
 
 	public int getPeerPort (
 	)  {
 		return encapsulatedChannel.getPeerPort();
 	}
 		
 
 
 	public String getPeerName(
 	) {
 
 		return encapsulatedChannel.getPeerName( );
 
 	}
 
 
 	public String getTransport(
 	) {
 
 		return encapsulatedChannel.getTransport( );
 
 	}
 
 
 	public boolean isReliable(
 	) {
 
 		return encapsulatedChannel.isReliable( );
 
 	}
 
 	/** Clear the original request (to reduce the stack size) 
         protected void clearOriginalRequest() {
 		this.originalRequest = null;
 	}
 	*/
 
 
 	/**
 	 *Returns the Via header for this channel.  Gets the Via header of the
 	 *underlying message channel, and adds a branch parameter to it for this
 	 *transaction.
 	 */
 	public Via getViaHeader(
 	) {
 
 		// Via header of the encapulated channel
 		Via	channelViaHeader;
 
 
 		// Add the branch parameter to the underlying 
 		// channel's Via header
 		channelViaHeader = super.getViaHeader( );
 		try {
 		channelViaHeader.setBranch( branch );
 		} catch (java.text.ParseException ex) {}
 		return channelViaHeader;
 
 	}
 
 
 
 
 	public void handleException(
 		SIPServerException	ex
 	) {
 
 		encapsulatedChannel.handleException( ex );
 
 	}
 
 
 	/**
 	 *	Process the message through the transaction and sends it to the SIP
 	 * peer.
 	 *
 	 *	@param messageToSend Message to send to the SIP peer.
 	 */
 	abstract public void sendMessage(
 		SIPMessage	messageToSend
 	) throws IOException;
 
 
 
 
 	/**
 	 *	Parse the byte array as a message, process it through the 
 	 * transaction, and send it to the SIP peer.
 	 *
 	 *	@param messageBytes Bytes of the message to send.
 	 *	@param receiverAddress Address of the target peer.
 	 *	@param receiverPort Network port of the target peer.
 	 *
 	 *	@throws IOException If there is an error parsing 
 	 * the byte array into an object.
 	 */
 	protected void sendMessage(
 		byte[]		messageBytes,
 		InetAddress	receiverAddress,
 		int			receiverPort
 	) throws IOException {
 
 		// Object representation of the SIP message
 		SIPMessage		messageToSend;
 
 		try {
 			StringMsgParser messageParser = new StringMsgParser();
 			messageToSend = 
 			messageParser.parseSIPMessage( messageBytes );
 			sendMessage( messageToSend );
 		} catch( ParseException e ) {
 			throw new IOException( e.getMessage( ) );
 		}
 
 	}
 
 
 	/**
 	 *	Adds a new event listener to this transaction.
 	 *
 	 *	@param newListener Listener to add.
 	 */
 	public void addEventListener(
 		SIPTransactionEventListener	newListener
 	) {
 
 		eventListeners.add( newListener );
 
 	}
 
 
 	/**
 	 *	Removed an event listener from this transaction.
 	 *
 	 *	@param oldListener Listener to remove.
 	 */
 	public void removeEventListener(
 		SIPTransactionEventListener	oldListener
 	) {
 
 		eventListeners.remove( oldListener );
 
 	}
 
 
 	/**
 	 * Creates a SIPTransactionErrorEvent and sends it 
 	 * to all of the listeners of this transaction.  
          * This method also flags the transaction as
 	 * terminated.
 	 *
 	 *	@param errorEventID ID of the error to raise.
 	 */
 	protected void raiseErrorEvent(
 		int	errorEventID
 	) {
 
 		// Error event to send to all listeners
 		SIPTransactionErrorEvent	newErrorEvent;
 		// Iterator through the list of listeners
 		Iterator			listenerIterator;
 		// Next listener in the list
 		SIPTransactionEventListener	nextListener;
 
 		// Create the error event
 		newErrorEvent = new SIPTransactionErrorEvent( this, 
 					errorEventID );
 
 		// Loop through all listeners of this transaction
 		synchronized( eventListeners ) {
 			listenerIterator = eventListeners.iterator( );
 			while( listenerIterator.hasNext( ) ) {
 				// Send the event to the next listener
 				nextListener = (SIPTransactionEventListener)
 						listenerIterator.next( );
 				nextListener.transactionErrorEvent
 						( newErrorEvent );
 
 			}
 
 		}
 		// Clear the event listeners after propagating the error.
 		// Retransmit notifications are just an alert to the 
 		// application (they are not an error).
 		if ( errorEventID !=   
 			SIPTransactionErrorEvent.TIMEOUT_RETRANSMIT) {
 	            eventListeners.clear();
 
 		   // Errors always terminate a transaction
 		   setState( TERMINATED_STATE );
 		
 		   if (this instanceof SIPServerTransaction &&
 		    this.isByeTransaction() && this.dialog != null )
 	 	    this.dialog.setState(DialogImpl.TERMINATED_STATE);
 		}
 		
 
 	}
 
 
 	/** Sets the canceled flag. This indicates that the transaction
 	* has been successfully cancelled.
 	*/
 	protected void checkCancel(SIPResponse transactionResponse) {
 	        if (transactionResponse.getCSeq().
 		   getMethod().equalsIgnoreCase("CANCEL") && 
 		   (transactionResponse.getStatusCode() == 200 ||
 	           transactionResponse.getStatusCode() == 487))  {
 	           this.isCancelled = true;
 	       }
 	}
         
         
         /** A shortcut way of telling if we are a server transaction.
          */
         protected boolean IsServerTransaction() {
                 return this instanceof SIPServerTransaction;
         }
 
         /** Gets the dialog object of this Transaction object. This object
          * returns null if no dialog exists. A dialog only exists for a
          * transaction when a session is setup between a User Agent Client and a
          * User Agent Server, either by a 1xx Provisional Response for an early
          * dialog or a 200OK Response for a committed dialog.
          *
          * @return the Dialog Object of this Transaction object.
          * @see Dialog
          */
         public Dialog getDialog() {
             return (Dialog) this.dialog;
         }
         
         /** set the dialog object.
          *@param dialog -- the dialog to set.
          */
         public void setDialog(DialogImpl dialog) {
             this.dialog = dialog;
         }
         
         /** Returns the current value of the retransmit timer in 
 	 * milliseconds used to retransmit messages over unreliable transports.
          *
          * @return the integer value of the retransmit timer in milliseconds.
          */
         public int getRetransmitTimer() {
             return SIPTransactionStack.BASE_TIMER_INTERVAL;
         }
         
         /** Get the host to assign for an outgoing Request via header.
          */
         public String getViaHost() {
             return this.getViaHeader().getHost();
             
         }
         
         /** Get the user from the original request.
         public String getUser() {
             return 
                 (this.getOriginalRequest().getRequestURI()).getUser();
        }
          */
         
         /** Get the last response.
          */
         public SIPResponse getLastResponse() { return this.lastResponse; }
 
 	
 	/** Get the JAIN interface response 
 	*/
 	public Response getResponse() { return (Response) this.lastResponse; }
 
 	/** Get the transaction Id.
 	*/
 	public String getTransactionId() {
 		return this.getOriginalRequest().getTransactionId();
 	}
         
         
         
         /** Get the port to assign for the via header of an outgoing message.
          */
         public int getViaPort() {
             return this.getViaHeader().getPort();
         }
         /** A method that can be used to test if an incoming request
         * belongs to this transction. This does not take the transaction
         * state into account when doing the check otherwise it is identical
         * to isMessagePartOfTransaction. This is useful for checking if
         * a CANCEL belongs to this transaction.
         *
         * @param requestToTest is the request to test.
         * @return true if the the request belongs to the transaction.
         *
         */
     public boolean doesCancelMatchTransaction(
     SIPRequest	requestToTest
     ) {
         
         // List of Via headers in the message to test
         ViaList	viaHeaders;
         // Topmost Via header in the list
         Via		topViaHeader;
         // Branch code in the topmost Via header
         String	messageBranch;
         // Flags whether the select message is part of this transaction
         boolean transactionMatches;
         
         
         transactionMatches = false;
         
         if (this.getOriginalRequest() == null ||
             this.getOriginalRequest().getMethod().equalsIgnoreCase
             (Request.CANCEL))
             return false;
         // Get the topmost Via header and its branch parameter
         viaHeaders = requestToTest.getViaHeaders( );
         if( viaHeaders != null ) {
             
             topViaHeader = (Via)viaHeaders.getFirst( );
             messageBranch = topViaHeader.getBranch( );
             if( messageBranch != null ) {
                 
                 // If the branch parameter exists but
                 // does not start with the magic cookie,
                 if( !messageBranch.startsWith
                 ( SIPConstants.BRANCH_MAGIC_COOKIE ) ) {
                     
                     // Flags this as old
                     // (RFC2543-compatible) client
                     // version
                     messageBranch = null;
                     
                 }
                 
             }
             
             // If a new branch parameter exists,
             if( messageBranch != null  &&
             this.getBranch() != null ) {
                 
                 // If the branch equals the branch in
                 // this message,
                 if( getBranch().equals(messageBranch)
                 && topViaHeader.getSentBy( ).
                 equals(((Via)getOriginalRequest().
                 getViaHeaders().getFirst()).
                 getSentBy()) 
 		
                 ) {
                     transactionMatches = true;
                     if (parentStack.logWriter.needsLogging)
                         parentStack.logWriter.logMessage
                         ("returning  true");
                 }
                 
             } else {
                 // If this is an RFC2543-compliant message,
                 // If RequestURI, To tag, From tag,
                 // CallID, CSeq number, and top Via
                 // headers are the same,
 		if (parentStack.logWriter.needsLogging)
 			parentStack.logWriter.logMessage("testing against " +
 				getOriginalRequest());
                 
                 
                 if (
 
 		getOriginalRequest().getRequestURI().equals
 		(requestToTest.getRequestURI()) &&
 
 		getOriginalRequest().getTo().equals
 		(requestToTest.getTo()) &&
 		
 		 getOriginalRequest().getFrom().equals
 		(requestToTest.getFrom()) &&
 
                 getOriginalRequest().getCallId( ).
 		getCallId( ).equals
 		( requestToTest.getCallId( ) .getCallId( )) &&
 
                 getOriginalRequest( ).
                 getCSeq( ).getSequenceNumber( ) ==
                 requestToTest.getCSeq().
                 getSequenceNumber() &&
 
                 topViaHeader.equals
 		( getOriginalRequest().
                   getViaHeaders().getFirst())
 
                 
 		) {
                     
                     transactionMatches = true;
                 }
                 
             }
             
         }
         
         return transactionMatches;
     }
         
         /** Sets the value of the retransmit timer to the 
 	 * newly supplied timer value.
 	 * The retransmit timer is expressed in milliseconds and its 
 	 * default value  is 500ms. This method allows the application
 	 * to change the transaction  retransmit behavior for different
 	 * networks. Take the gateway proxy as  an example. The internal
 	 * intranet is likely to be reatively uncongested  and the
 	 * endpoints will be relatively close. The external network is
 	 * the  general Internet. This functionality allows different
 	 * retransmit times  for either side.  
 	 *
          * @param retransmitTimer - the new integer value of the 
 	 * retransmit timer in milliseconds.
          */
         public void setRetransmitTimer(int retransmitTimer) 
 	{
 		throw new UnsupportedOperationException
 			("Feature not supported");
         }
         
      /** Close the encapsulated channel.
       */
      public void close() { this.encapsulatedChannel.close(); }
 
      public boolean isSecure() { 
 	  return encapsulatedChannel.isSecure();
      }
 
      public MessageProcessor getMessageProcessor() {
 		return this.encapsulatedChannel.getMessageProcessor();
      }
 
 }
