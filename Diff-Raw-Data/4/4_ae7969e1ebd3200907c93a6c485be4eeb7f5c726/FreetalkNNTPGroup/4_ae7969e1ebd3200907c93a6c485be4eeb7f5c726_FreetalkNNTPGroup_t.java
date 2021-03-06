 /* This code is part of Freenet. It is distributed under the GNU General
  * Public License, version 2 (or at your option any later version). See
  * http://www.gnu.org/ for further details of the GPL. */
 package plugins.Freetalk.ui.NNTP;
 
 import plugins.Freetalk.Board;
 import plugins.Freetalk.Message;
 import plugins.Freetalk.exceptions.NoSuchMessageException;
 
 import java.util.Iterator;
 import java.util.NoSuchElementException;
 
 /**
  * Object representing a newsgroup, as seen from the NNTP client's
  * point of view.
  *
  * @author Benjamin Moody
  */
 public class FreetalkNNTPGroup {
 	private final Board board;
 
 	public FreetalkNNTPGroup(Board board) {
 		this.board = board;
 	}
 
 	/**
 	 * Get the FTBoard object associated with this group.
 	 */
 	public Board getBoard() {
 		return board;
 	}
 
 	/**
 	 * Estimate number of messages that have been posted.
 	 */
 	public long messageCount() {
 		return board.getAllMessages().size();
 	}
 
 	/**
 	 * Get the first valid message number.
 	 */
 	public int firstMessage() {
 		return 1;
 	}
 
 	/**
 	 * Get the last valid message number.
 	 */
 	public int lastMessage() {
 		return board.getLastMessageIndex();
 	}
 
 	/**
 	 * Get an iterator for articles in the given range.
 	 */
 	public Iterator<FreetalkNNTPArticle> getMessageIterator(int start, int end) throws NoSuchMessageException {
 		synchronized (board) {
 			if (start < firstMessage())
 				start = firstMessage();
 
 			if (end == -1 || end > lastMessage())
 				end = lastMessage();
 
 			Iterator<FreetalkNNTPArticle> iter;
 
 			final int startIndex = start;
 			final int endIndex = end;
 
 			iter = new Iterator<FreetalkNNTPArticle>() {
 				private int currentIndex = startIndex;
 				private Message currentMessage = null;
 
 				public boolean hasNext() {
 					if (currentMessage != null)
 						return true;
 
 					while (currentIndex <= endIndex) {
 						try {
 							currentMessage = board.getMessageByIndex(currentIndex);
 							return true;
 						}
 						catch (NoSuchMessageException e) {
 							// ignore
 						}
 						currentIndex++;
 					}
 					return false;
 				}
 
 				public FreetalkNNTPArticle next() {
 					if (!hasNext())
 						throw new NoSuchElementException();
 					else {
						Message msg = currentMessage;
						currentMessage = null;
						return new FreetalkNNTPArticle(msg, currentIndex++);
 					}
 				}
 
 				public void remove() {
 					throw new UnsupportedOperationException();
 				}
 			};
 
 			if (!iter.hasNext())
 				throw new NoSuchMessageException();
 
 			return iter;
 		}
 	}
 
 	/**
 	 * Get the board posting status.  This is normally either "y"
 	 * (posting is allowed), "n" (posting is not allowed), or "m"
 	 * (group is moderated.)  It is a hint to the reader and doesn't
 	 * necessarily indicate whether the client will be allowed to
 	 * post, or whether any given message will be accepted.
 	 */
 	public String postingStatus() {
 		return "y";
 	}
 }
