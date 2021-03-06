 package org.marketcetera.oms;
 
 import org.marketcetera.core.*;
 import org.marketcetera.quickfix.*;
 import quickfix.FieldNotFound;
 import quickfix.Message;
 import quickfix.SessionID;
 import quickfix.field.*;
 import quickfix.fix42.ExecutionReport;
 import quickfix.fix42.OrderCancelReject;
 
 import java.math.BigDecimal;
 import java.util.LinkedList;
 import java.util.List;
 
 public class OutgoingMessageHandler {
 
 	private List<OrderModifier> orderModifiers;
     private OrderRouteManager routeMgr;
     private SessionID defaultSessionID;         // used to store the SessionID so that FIX sender can find it
     private IQuickFIXSender quickFIXSender = new QuickFIXSender();
     
 	public OutgoingMessageHandler() {
         setOrderModifiers(new LinkedList<OrderModifier>());
         setOrderRouteManager(new OrderRouteManager());
     }
 
     public void setOrderRouteManager(OrderRouteManager inMgr)
     {
         routeMgr = inMgr;
     }
 
     public void setOrderModifiers(List<OrderModifier> mods){
 		orderModifiers = new LinkedList<OrderModifier>();
 		for (OrderModifier mod : mods) {
 			orderModifiers.add(mod);
 		}
 		orderModifiers.add(new TransactionTimeInsertOrderModifier());
 	}
 	
 	public Message handleMessage(Message message) {
         Message returnVal = null;
         try {
             modifyOrder(message);
             // if single, pre-create an executionReport and send it back
             if (FIXMessageUtil.isOrderSingle(message))
             {
                 Message outReport = executionReportFromNewOrder(message);
                 if(LoggerAdapter.isDebugEnabled(this)) {
                     LoggerAdapter.debug("Sending immediate execReport:  "+outReport, this);
                 }
 				returnVal = outReport;
             }
             routeMgr.modifyOrder(message);
             if (defaultSessionID != null)
             	quickFIXSender.sendToTarget(message, defaultSessionID);
             else 
             	quickFIXSender.sendToTarget(message);
             	
         } catch (FieldNotFound fnfEx) {
             MarketceteraFIXException mfix = MarketceteraFIXException.createFieldNotFoundException(fnfEx);
             returnVal = createRejectionMessage(mfix, message);
         } catch (MarketceteraException e) {
         	returnVal = createRejectionMessage(e, message);
         } catch(Exception ex) {
         	returnVal = createRejectionMessage(ex, message);
         }
         return returnVal;
 	}
 	
 
 
     /** Creates a rejection message based on the message that causes the rejection
      * Currently, if it's an orderCancel then we send back an OrderCancelReject,
      * otherwise we always send back the ExecutionReport.
      * @param existingOrder
      * @return Corresponding rejection Message
      */
     protected Message createRejectionMessage(Exception causeEx, Message existingOrder)
     {
         Message rejection = null;
         if(FIXMessageUtil.isCancelReplaceRequest(existingOrder) ||
            FIXMessageUtil.isCancelRequest(existingOrder) )
         {
             rejection = new OrderCancelReject();
         } else {
             rejection = new ExecutionReport();
         }
 
         rejection.setField(new OrdStatus(OrdStatus.REJECTED));
         rejection.setField(new ExecType(ExecType.REJECTED));
         FIXMessageUtil.fillFieldsFromExistingMessage(rejection,  existingOrder);
         
         
         String msg = (causeEx.getMessage() == null) ? causeEx.toString() : causeEx.getMessage();
         LoggerAdapter.error(OMSMessageKey.MESSAGE_EXCEPTION.getLocalizedMessage(msg, existingOrder), causeEx, this);
         rejection.setString(Text.FIELD, msg);
         FIXMessageUtil.fillFieldsFromExistingMessage(rejection,  existingOrder);
         // manually set the ClOrdID since it's not required in the dictionary but is for electronic orders
         try {
             rejection.setField(new ClOrdID(existingOrder.getString(ClOrdID.FIELD)));
         } catch(FieldNotFound ignored) {
             // don't set it if it's not there
         }
 
         return rejection;
     }
 
     public Message executionReportFromNewOrder(Message newOrder) throws FieldNotFound {
         if (FIXMessageUtil.isOrderSingle(newOrder)){
             String clOrdId = newOrder.getString(ClOrdID.FIELD);
             char side = newOrder.getChar(Side.FIELD);
             String symbol = newOrder.getString(Symbol.FIELD);
             BigDecimal orderQty = new BigDecimal(newOrder.getString(OrderQty.FIELD));
             BigDecimal orderPrice = null;
             try {
                 String strPrice = newOrder.getString(Price.FIELD);
                 orderPrice =  new BigDecimal(strPrice);
             } catch(FieldNotFound ex) {
                 // leave as null
             }
 
             AccountID inAccountID = null;
             try {
                 inAccountID = new AccountID(newOrder.getString(Account.FIELD));
             } catch (FieldNotFound ex) {
                 // only set the Account field if it's there
             }
 
             return FIXMessageUtil.newExecutionReport(
                     null,
                     new InternalID(clOrdId),
                     "ZZ-INTERNAL",
                     '\0',
                     ExecType.NEW,
                     OrdStatus.NEW,
                     side,
                     orderQty,
                     orderPrice,
                     BigDecimal.ZERO,
                     BigDecimal.ZERO,
                     orderQty,
                     BigDecimal.ZERO,
                     BigDecimal.ZERO,
                     new MSymbol(symbol),
                     inAccountID);
         } else {
             return null;
         }
     }
 
     /** Apply all the order modifiers to this message */
     protected void modifyOrder(Message inOrder) throws MarketceteraException
     {
         for (OrderModifier oneModifier : orderModifiers) {
             oneModifier.modifyOrder(inOrder);
         }
     }
     
     /** Sets the default session that's actually created in {@link QuickFIXInitiator} */
     public void setDefaultSessionID(SessionID inSessionID)
     {
         defaultSessionID = inSessionID;
     }
 
     public SessionID getDefaultSessionID() {
         return defaultSessionID;
     }
 
 	public IQuickFIXSender getQuickFIXSender() {
 		return quickFIXSender;
 	}
 
 	public void setQuickFIXSender(IQuickFIXSender quickFIXSender) {
 		this.quickFIXSender = quickFIXSender;
 	}
 
 
 
 }
