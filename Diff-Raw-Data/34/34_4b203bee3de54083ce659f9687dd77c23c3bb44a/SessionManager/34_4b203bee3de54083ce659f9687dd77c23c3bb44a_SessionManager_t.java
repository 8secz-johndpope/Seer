 package com.novel.odisp;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 
 import com.novel.odisp.common.Message;
 import com.novel.odisp.common.MessageHandler;
 
 /**     Odisp  ("-").  ݣ 
  *   ,   ( )  ӣ
  *  Odisp . 
  * 
  * <h5> ODsessionManager':</h5>
  * 
  * <p>       ,   
  *  Odisp (   ).  
  *  : 
  * 
  * <pre>
  * #   : 
  * Message newMsg = dispatcher.getNewMessage(...);
  * ...
  * ODSessionManager sm = ODSessionManager.getSessionManager();
  * sm.add(newMsg.getId(), new MessageHandler() {
  *    public void messageReceived(Message msg) {
  *      System.out.println("Some reply-message received!");
  *    }
  * });
  * 
  * ...
  * 
  * handleMessage(Message msg) {
  *   ODSessionManager sm = ODSessionManager.getSessionManager();
  *   if (!sm.processMessage(msg)) {
  *     System.out.println("Message processed by SessionManager");
  *   } else if (ODObjectLoaded.equals(msg)) {
  *     ...
  *   }
  *   ...
  * }
  * </pre>
  * 
  * @author <a href="dron@novel-il.ru"> . </a>
  * @author (C) 2004  "-"
 * @version $Id: SessionManager.java,v 1.4 2004/07/12 12:31:00 valeks Exp $ 
  */
 public class SessionManager {
   /**   ODSessionManager */
   private static SessionManager sessionManager = new SessionManager();
   /**  msgId <=>  (MessageHandler) */
   private Map handlers = new HashMap();
   
   /** 
    */
   protected SessionManager() {
     super();
   }
   
   /**   
    * 
    * @return  .
    */
   public static SessionManager getSessionManager() {
     return sessionManager;
   }
   
   /**    .
    * 
    * @param messageId  ,   .
    * @see com.novel.odisp.common.Message#getId()
   * @param messageHandler   .
    */
   public void addMessageListener(int messageId,
                                  MessageHandler messageHandler) {
     handlers.put(new Integer(messageId), messageHandler);
   }
   
   /**    Odisp.   
    * messageRecevied  handleMessage. 
    * @param msg  Odisp
    * @return         
    * ,   true,     ,
    *        ( 
    * ).
    */
   public boolean processMessage(Message msg) {
     boolean matched = false;     
     Map toPerform;
     List toRemove = new ArrayList();
     synchronized(handlers) {
       toPerform = new HashMap(handlers);
     }
     Iterator it = toPerform.keySet().iterator();
     while (it.hasNext()) {
       Integer key = (Integer) it.next();
       if (key.equals(new Integer(msg.getReplyTo()))) {
         ((MessageHandler) toPerform.get(key)).messageReceived(msg);
         toRemove.add(key);
       }
     } // while
     synchronized(handlers) {
       Iterator ri = toRemove.iterator();
       while (it.hasNext()) {
         handlers.remove(it.next());
       }
     }
     return matched;
   }
 
 }
