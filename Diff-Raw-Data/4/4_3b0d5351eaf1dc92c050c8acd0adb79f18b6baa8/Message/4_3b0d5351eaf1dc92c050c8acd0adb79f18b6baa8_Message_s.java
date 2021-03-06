 /* This code is part of Freenet. It is distributed under the GNU General
  * Public License, version 2 (or at your option any later version). See
  * http://www.gnu.org/ for further details of the GPL. */
 package plugins.Freetalk;
 
 import java.io.UnsupportedEncodingException;
 import java.util.Arrays;
 import java.util.Date;
 import java.util.Iterator;
 import java.util.List;
 import java.util.NoSuchElementException;
 import java.util.Set;
 
 import plugins.Freetalk.exceptions.InvalidParameterException;
 import plugins.Freetalk.exceptions.NoSuchMessageException;
 
 import com.db4o.ObjectContainer;
 import com.db4o.query.Query;
 
 import freenet.crypt.SHA256;
 import freenet.keys.FreenetURI;
 import freenet.support.HexUtil;
 import freenet.support.Logger;
 
 /**
  * @author saces, xor
  *
  */
 public class Message {
 	
 	/* Attributes, stored in the database */
 	
 	/**
 	 * The URI of this message.
 	 */
 	protected final FreenetURI mURI;
 	
 	/**
 	 * The ID of the message, a (hash) function of the URI, lowercase characters of [0-9a-z] only.
 	 */
 	protected final String mID;
 	
 	/**
 	 * The URI of the thread this message belongs to.
 	 * We do not need it to construct the thread-tree from messages, but it boosts performance of thread-tree-construction:
 	 * Thread-size (amount of replies) is usually infinitesimal compared to the size of a FTBoard (amount of threads).
 	 * We receive messages in random order, therefore we will usually have orphan messages of which we need to find the parents.
 	 * If we receive the parent messages of those messages, we will be able to find their orphan children faster if we only need to search in
 	 * the thread they belong to and not in the whole FTBoard - which may contain many thousands of messages.
 	 */
 	protected final FreenetURI mThreadURI;
 	
 	/**
 	 * The URI of the message to which this message is a reply. Null if it is a thread.
 	 */
 	protected final FreenetURI mParentURI;
 	
 	/**
 	 * The boards to which this message was posted, in alphabetical order.
 	 */
 	protected final Board[] mBoards; 
 	
 	protected final Board mReplyToBoard;
 	
 	protected final FTIdentity mAuthor;
 
 	protected final String mTitle;
 	
 	/**
 	 * The date when the message was written in <strong>UTC time</strong>.
 	 */
 	protected final Date mDate;
 	
 	/**
 	 * The index of the message on the inserter's message USK/SSK.
 	 */
 	protected int mIndex;
 	
 	protected final String mText;
 	
 	/**
 	 * The attachments of this message, in the order in which they were received in the original message.
 	 */
 	protected final Attachment[] mAttachments;
 	
 	public static class Attachment {
 		private final FreenetURI mURI;
 		private final int mSize; /* Size in bytes */
 		
 		public Attachment(FreenetURI myURI, int mySize) {
 			mURI = myURI;
 			mSize = mySize;
 		}
 		
 		public FreenetURI getURI() {
 			return mURI;
 		}
 		
 		public int getSize() {
 			return mSize;
 		}
 	}
 	
 	/**
 	 * The thread to which this message is a reply.
 	 */
 	private Message mThread = null;
 	
 	/**
 	 * The message to which this message is a reply.
 	 */
 	private Message mParent = null;
 	
 	
 	/* References to objects of the plugin, not stored in the database. */
 	
 	protected transient ObjectContainer db;
 	
 	protected transient MessageManager mMessageManager;
 	
 	
 	/**
 	 * Get a list of fields which the database should create an index on.
 	 */
 	public static String[] getIndexedFields() {
 		return new String[] { "mURI", "mID", "mThreadURI", };
 	}
 	
 	public Message(FreenetURI newURI, FreenetURI newThreadURI, FreenetURI newParentURI, Set<Board> newBoards, Board newReplyToBoard, FTIdentity newAuthor, String newTitle, Date newDate, String newText, List<Attachment> newAttachments) throws InvalidParameterException {
 		if (newURI == null || newBoards == null || newAuthor == null)
 			throw new IllegalArgumentException();
 		
 		if(newParentURI != null && newThreadURI == null) 
 			Logger.error(this, "Message with parent URI but without thread URI created: " + newURI);
 		
 		if (newBoards.isEmpty())
 			throw new InvalidParameterException("No boards in message " + newURI);
 		
 		if (newReplyToBoard != null && !newBoards.contains(newReplyToBoard)) {
 			Logger.error(this, "Message created with replyToBoard not being in newBoards: " + newURI);
 			newBoards.add(newReplyToBoard);
 		}
 
 		if (!isTitleValid(newTitle))
 			throw new InvalidParameterException("Invalid message title in message " + newURI);
 		
 		if (!isTextValid(newText))
 			throw new InvalidParameterException("Invalid message text in message " + newURI);
 	
 		mURI = newURI;
 		mID = generateID(mURI);
 		mThreadURI = newThreadURI;
 		mParentURI = newParentURI;
 		mBoards = newBoards.toArray(new Board[newBoards.size()]);
 		Arrays.sort(mBoards);		
 		mReplyToBoard = newReplyToBoard;
 		mAuthor = newAuthor;
 		mTitle = newTitle;
 		mDate = newDate; // TODO: Check out whether Date provides a function for getting the timezone and throw an Exception if not UTC.
 		mIndex = getIndexFromURI(mURI);
 		mText = newText;
 		mAttachments = newAttachments == null ? null : newAttachments.toArray(new Attachment[newAttachments.size()]);
 	}
 	
 	/**
 	 * Has to be used after loading a FTBoard object from the database to initialize the transient fields.
 	 */
 	public void initializeTransient(ObjectContainer myDB, MessageManager myMessageManager) {
 		assert(myDB != null);
 		assert(myMessageManager != null);
 		db = myDB;
 		mMessageManager = myMessageManager;
 	}
 	
 	public static String generateID(FreenetURI uri) {
 		/* FIXME: Maybe find an easier way for message ID generation before release */
 		try {
 			return HexUtil.bytesToHex(SHA256.digest(uri.toACIIString().getBytes("US-ASCII")));
 		} catch (UnsupportedEncodingException e) {
 			throw new RuntimeException(e);
 		}
 	}
 	
 	protected static FreenetURI generateURI(FreenetURI baseURI, FTIdentity author, int index) {
 		baseURI = baseURI.setKeyType("SSK");
 		baseURI = baseURI.setDocName(Freetalk.PLUGIN_TITLE + "|" + "Message" + "-" + index + ".xml");
 		return baseURI.setMetaString(null);
 	}
 	
 	public static int getIndexFromURI(FreenetURI uri) {
 		return Integer.parseInt(uri.getDocName().split("[|]")[1].split("[-]")[1].replace(".xml", ""));
 	}
 	
 	public static FreenetURI generateRequestURI(FTIdentity author, int index) {
 		return generateURI(author.getRequestURI(), author, index);
 	}
 	
 	/**
 	 * Get the URI of the message.
 	 */
 	public FreenetURI getURI() {
 		return mURI;
 	}
 	
 	public String getID() {
 		return mID;
 	}
 	
 	/**
 	 * Get the FreenetURI of the thread this message belongs to.
 	 * @throws NoSuchMessageException 
 	 */
 	public FreenetURI getParentThreadURI() throws NoSuchMessageException {
 		if(mThreadURI == null)
 			throw new NoSuchMessageException();
 		
 		return mThreadURI;
 	}
 	
 	public synchronized String getParentThreadID() throws NoSuchMessageException {
 		/* TODO: Which requires more CPU, to synchronize the function so that we can check for mThread != null and use its cached ID or to
 		 * just generate the ID by SHA256 hashing the parent URI and bytesToHex ?
 		 * I suppose the synchronization is faster. Anyone else? */
 		return mThread != null ? mThread.getID() : generateID(getParentThreadURI());
 	}
 	
 	/**
 	 * Get the FreenetURI to which this message is a reply. Null if the message is a thread.
 	 */
 	public FreenetURI getParentURI() throws NoSuchMessageException {
 		if(mParentURI == null)
 			throw new NoSuchMessageException();
 		
 		return mParentURI;
 	}
 	
 	public synchronized String getParentID() throws NoSuchMessageException {
 		return mParent != null ? mParent.getID() : generateID(getParentURI());
 	}
 	
 	public boolean isThread() {
 		return mParentURI == null;
 	}
 	
 	/**
 	 * Get the boards to which this message was posted. The boards are returned in alphabetical order.
 	 * The transient fields of the returned boards are initialized already.
 	 */
 	public Board[] getBoards() {
 		for(Board b : mBoards)
 			b.initializeTransient(db, mMessageManager);
 		return mBoards;
 	}
 	
 	public Board getReplyToBoard() {
 		return mReplyToBoard;
 	}
 
 	/**
 	 * Get the author of the message.
 	 */
 	public FTIdentity getAuthor() {
 		db.activate(this, 3);
 		if(mAuthor == null)
 			throw new RuntimeException("mAuthor == null");
 		mAuthor.initializeTransient(db, mMessageManager.getIdentityManager());
 		return mAuthor;
 	}
 
 	/**
 	 * Get the title of the message.
 	 */
 	public String getTitle() {
 		return mTitle;
 	}
 	
 	/**
 	 * Get the date when the message was written in <strong>UTC time</strong>.
 	 */
 	public Date getDate() {
 		return mDate;
 	}
 	
 	/**
 	 * Get the index of the message on it's date.
 	 */
 	public int getIndex() {
 		return mIndex;
 	}
 	
 	/**
 	 * Get the text of the message.
 	 */
 	public String getText() {
 		return mText;
 	}
 	
 	/**
 	 * Get the attachments of the message, in the order in which they were received.
 	 */
 	public Attachment[] getAttachments() {
 		return mAttachments;
 	}
 	
 	/**
 	 * Get the thread to which this message belongs. The transient fields of the returned message will be initialized already.
 	 */
 	public synchronized Message getThread() throws NoSuchMessageException {
 		if(mThread == null)
 			throw new NoSuchMessageException();
 		mThread.initializeTransient(db, mMessageManager);
 		return mThread;
 	}
 	
 	public synchronized void setThread(Message newParentThread) {
 		assert(mThread == null);
 		assert(mThreadURI != null);
 		assert(newParentThread.getURI().equals(mThreadURI));
 		mThread = newParentThread;
 		store();
 	}
 
 	/**
 	 * Get the message to which this message is a reply. The transient fields of the returned message will be initialized already.
 	 */
 	public synchronized Message getParent() throws NoSuchMessageException {
 		if(mParent == null)
 			throw new NoSuchMessageException();
 		mParent.initializeTransient(db, mMessageManager);
 		return mParent;
 	}
 
 	public synchronized void setParent(Message newParent)  {
 		assert(newParent.getURI().equals(mParentURI));
 		/* TODO: assert(newParent contains at least one board which mBoards contains) */
 		mParent = newParent;
 		store();
 	}
 	
 	/**
 	 * Returns an iterator over the children of the message, sorted descending by date.
 	 * The transient fields of the children will be initialized already.
 	 */
 	public synchronized Iterator<Message> childrenIterator(final Board targetBoard) {
 		return new Iterator<Message>() {
 			private Iterator<Message> iter;
 			private Board board;
 			private Message next;
 			
 			{
 				board = targetBoard;
 				
 				/* TODO: Accelerate this query: configure db4o to keep a per-message date-sorted index of children.
 				 * - Not very important for now since threads are usually small. */
 				Query q = db.query();
 				q.constrain(Message.class);
 				q.descend("mParent").constrain(this);
 				q.descend("mDate").orderDescending();
 				
 				iter = q.execute().iterator();
 				next = iter.hasNext() ? iter.next() : null;
 			}
 
 			public boolean hasNext() {
 				while(next != null) {
 					if(Arrays.binarySearch(next.getBoards(), board) >= 0)
 						return true;
 				}
 				
 				return false;
 			}
 
 			public Message next() {
 				if(!hasNext()) { /* We have to call hasNext() to ignore messages which do not belong to the selected board */
 					assert(false); /* However, the users of the function should do this for us */
 					throw new NoSuchElementException();
 				}
 				
 				Message child = next;
 				next = iter.hasNext() ? iter.next() : null;
 				child.initializeTransient(db, mMessageManager);
 				return child;
 			}
 
 			public void remove() {
 				throw new UnsupportedOperationException("Use child.setParent(null) instead.");
 			}
 		};
 	}
 	
 	/**
 	 * Checks whether the title of the message is valid. Validity conditions:
 	 * - ...
 	 */
 	static public boolean isTitleValid(String title) {
 		// FIXME: Implement.
 		return true;
 	}
 	
 	/**
 	 * Checks whether the text of the message is valid. Validity conditions:
 	 * - ...
 	 */
 	static public boolean isTextValid(String text) {
 		// FIXME: Implement.
 		return true;
 	}
 	
 	/**
 	 * Makes the passed title valid in means of <code>isTitleValid()</code>
 	 * @see isTitleValid
 	 */
 	static public String makeTitleValid(String title) {
 		// FIXME: Implement.
 		return title;
 	}
 
 	/**
 	 * Makes the passed text valid in means of <code>isTextValid()</code>
 	 * @see isTextValid
 	 */
 	static public String makeTextValid(String text) {
 		// FIXME: Implement.
 		return text;
 	}
 	
 	public void store() {
 		/* FIXME: Check for duplicates */
 		if(db.ext().isStored(this) && !db.ext().isActive(this))
 			throw new RuntimeException("Trying to store a non-active Message object");
 		
 		if(mAuthor == null)
 			throw new RuntimeException("Trying to store a message with mAuthor == null");
 		
 		db.store(mURI);
 		if(mThreadURI != null)
 			db.store(mThreadURI);
 		if(mParentURI != null)
 			db.store(mParentURI);
		db.store(mDate);
		db.store(mAttachments);
 		db.store(this);
 		db.commit();
 	}
 	
 	public void objectOnUpdate(ObjectContainer db) {
 		if(mAuthor == null) throw new RuntimeException("mAuthor == null");
 		Logger.debug(this, "object on update: mAuthor == " + mAuthor);
 	}
 
 	public void objectOnNew(ObjectContainer db) {
 		objectOnUpdate(db);
 	}
 }
