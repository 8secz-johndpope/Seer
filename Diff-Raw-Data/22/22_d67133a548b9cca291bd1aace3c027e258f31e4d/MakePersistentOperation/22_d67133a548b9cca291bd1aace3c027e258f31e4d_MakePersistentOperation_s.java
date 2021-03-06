 /*
  * Copyright (c) 2009 - 2011, Jan Stender, Bjoern Kolbeck, Mikael Hoegqvist,
  *                     Felix Hupfeld, Felix Langner, Zuse Institute Berlin
  * 
  * Licensed under the BSD License, see LICENSE file for details.
  * 
  */
 package org.xtreemfs.babudb.replication.proxy.operations;
 
 import org.xtreemfs.babudb.api.database.DatabaseRequestListener;
 import org.xtreemfs.babudb.api.database.DatabaseRequestResult;
 import org.xtreemfs.babudb.api.exception.BabuDBException;
 import org.xtreemfs.babudb.pbrpc.GlobalTypes.ErrorCodeResponse;
 import org.xtreemfs.babudb.pbrpc.GlobalTypes.Type;
 import org.xtreemfs.babudb.pbrpc.RemoteAccessServiceConstants;
 import org.xtreemfs.babudb.replication.BabuDBInterface;
 import org.xtreemfs.babudb.replication.transmission.ErrorCode;
 import org.xtreemfs.babudb.replication.transmission.dispatcher.Operation;
 import org.xtreemfs.babudb.replication.transmission.dispatcher.Request;
 import org.xtreemfs.foundation.logging.Logging;
 
 import com.google.protobuf.Message;
 
 /**
  * Operation to execute database modifying methods remotely at the server with
  * master privilege. 
  *
  * @author flangner
  * @since 01/19/2011
  */
 public class MakePersistentOperation extends Operation {
 
     private final BabuDBInterface dbs;
     
     public MakePersistentOperation(BabuDBInterface dbs) {
         this.dbs = dbs;
     }
     
     /* (non-Javadoc)
      * @see org.xtreemfs.babudb.replication.transmission.dispatcher.Operation#getProcedureId()
      */
     @Override
     public int getProcedureId() {
         return RemoteAccessServiceConstants.PROC_ID_MAKEPERSISTENT;
     }
 
     /* (non-Javadoc)
      * @see org.xtreemfs.babudb.replication.transmission.dispatcher.Operation#getDefaultRequest()
      */
     @Override
     public Message getDefaultRequest() {
         return Type.getDefaultInstance();
     }
 
     /* (non-Javadoc)
      * @see org.xtreemfs.babudb.replication.transmission.dispatcher.Operation#
      *          processRequest(org.xtreemfs.babudb.replication.transmission.dispatcher.Request)
      */
     @Override
     public void processRequest(final Request rq) {
         Type type = (Type) rq.getRequestMessage();
         
         Logging.logMessage(Logging.LEVEL_DEBUG, this, "MakePersistentOperation:" +
                 " type %d.", type.getValue());
         
         try {
             DatabaseRequestResult<Object> result = 
                 dbs.getPersistanceManager().makePersistent((byte) type.getValue(), 
                         rq.getData().createViewBuffer());
             
             result.registerListener(new DatabaseRequestListener<Object>() {
                 
                 @Override
                 public void finished(Object result, Object context) {
                     rq.sendSuccess(ErrorCodeResponse.getDefaultInstance());
                 }
                 
                 @Override
                 public void failed(BabuDBException error, Object context) {
                     rq.sendSuccess(ErrorCodeResponse.newBuilder().setErrorCode(
                             ErrorCode.SERVICE_UNAVAILABLE).build());
                 }
             });
         } catch (BabuDBException be) {
             rq.sendSuccess(ErrorCodeResponse.newBuilder().setErrorCode(
                     ErrorCode.SERVICE_UNAVAILABLE).build());
         } 
     }
 }
