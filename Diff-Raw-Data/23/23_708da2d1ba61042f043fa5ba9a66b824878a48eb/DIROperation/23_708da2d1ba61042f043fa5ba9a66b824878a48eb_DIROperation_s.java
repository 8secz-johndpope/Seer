 /*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.
 
     This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
     Grid Operating System, see <http://www.xtreemos.eu> for more details.
     The XtreemOS project has been developed with the financial support of the
     European Commission's IST program under contract #FP6-033576.
 
     XtreemFS is free software: you can redistribute it and/or modify it under
     the terms of the GNU General Public License as published by the Free
     Software Foundation, either version 2 of the License, or (at your option)
     any later version.
 
     XtreemFS is distributed in the hope that it will be useful,
     but WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
     GNU General Public License for more details.
 
     You should have received a copy of the GNU General Public License
     along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
 /*
  * AUTHORS: Björn Kolbeck (ZIB)
  */
 
 package org.xtreemfs.dir.operations;
 
 import static org.xtreemfs.babudb.BabuDBException.ErrorCode.DB_EXISTS;
 import static org.xtreemfs.babudb.BabuDBException.ErrorCode.NO_ACCESS;
 import static org.xtreemfs.babudb.BabuDBException.ErrorCode.NO_SUCH_DB;
 import static org.xtreemfs.babudb.BabuDBException.ErrorCode.NO_SUCH_INDEX;
 import static org.xtreemfs.babudb.BabuDBException.ErrorCode.NO_SUCH_SNAPSHOT;
 import static org.xtreemfs.babudb.BabuDBException.ErrorCode.SNAP_EXISTS;
 
 import java.net.InetSocketAddress;
 
 import org.xtreemfs.babudb.BabuDBException;
 import org.xtreemfs.babudb.BabuDBRequestListener;
 import org.xtreemfs.babudb.replication.ReplicationManager;
 import org.xtreemfs.dir.DIRRequest;
 import org.xtreemfs.dir.DIRRequestDispatcher;
 import org.xtreemfs.foundation.logging.Logging;
 import org.xtreemfs.foundation.oncrpc.utils.ONCRPCException;
 import org.xtreemfs.interfaces.DIRInterface.InvalidArgumentException;
 import org.xtreemfs.interfaces.DIRInterface.RedirectException;
 
 /**
  * 
  * @author bjko
  */
 public abstract class DIROperation {
     
     protected final DIRRequestDispatcher master;
     
     protected final ReplicationManager dbsReplicationManager;
     
     public DIROperation(DIRRequestDispatcher master) {
         this.master = master;
         dbsReplicationManager = master.getDBSReplicationService();
     }
 
     public abstract int getProcedureId();
     
     /**
      * called after request was parsed and operation assigned.
      * 
      * @param rq
      *            the new request
      */
     public abstract void startRequest(DIRRequest rq);
     
     
     /**
      * Method to check if operation needs user authentication.
      * 
      * @return true, if the user needs to be authenticated
      */
     public abstract boolean isAuthRequired();
     
 
     /**
      * parses the RPC request message. Can throw any exception which
      * will result in an error message telling the client that the
      * request message data is garbage.
      * @param rq
      * @throws java.lang.Exception
      */
     public abstract void parseRPCMessage(DIRRequest rq) throws Exception;
    
     
     /**
      * Operation to give a failure back to the client.
      * Will decide, if a {@link RedirectException} should be returned.
      * 
      * @param error - Exception thrown.
      * @param rq - original {@link DIRRequest}.
      */
     void requestFailed(Exception error, DIRRequest rq) {
        if (error != null && 
            error instanceof BabuDBException && 
            ((BabuDBException) error).getErrorCode().equals(NO_ACCESS) && 
            dbsReplicationManager != null) {
             
             InetSocketAddress altMaster = dbsReplicationManager.getMaster();
             if (altMaster != null) {
                 // retrieve the correct port for the DIR mirror
                 String host = altMaster.getAddress().getHostAddress();
                 Integer port = this.master.getConfig().getMirrors().get(host);
                 if (port == null){ 
                     Logging.logMessage(Logging.LEVEL_ERROR, this,  "The port for " +
                     		"the mirror DIR '%s' could not be retrieved.",
                     		host);
                     
                     rq.sendInternalServerError(error);
                 } else {
                     rq.sendRedirectException(host,port);
                 }
             } else {
                 // if there is a handover in progress, redirect to the local
                 // server to notify the client about this process
                 rq.sendRedirectException(
                         this.master.getConfig().getAddress().getHostAddress(), 
                         this.master.getConfig().getPort());
             }
         } else if (error != null && error instanceof ONCRPCException) {
             Logging.logError(Logging.LEVEL_ERROR, this, error);
             rq.sendException((ONCRPCException) error);
         } else if (error != null && 
                    error instanceof BabuDBException &&
                   (((BabuDBException)error).getErrorCode().equals(NO_SUCH_DB) ||
                    ((BabuDBException)error).getErrorCode().equals(DB_EXISTS) ||
                    ((BabuDBException)error).getErrorCode().equals(NO_SUCH_INDEX) ||
                    ((BabuDBException)error).getErrorCode().equals(NO_SUCH_SNAPSHOT) ||
                    ((BabuDBException)error).getErrorCode().equals(SNAP_EXISTS)                   
                   )) 
         { // blame the client
             Logging.logError(Logging.LEVEL_ERROR, this, error);
             rq.sendException(new InvalidArgumentException(error.getMessage()));
         } else {
             if (error != null && !(error instanceof BabuDBException))
                 Logging.logError(Logging.LEVEL_ERROR, this, error);
             
             if (error != null)
                 Logging.logError(Logging.LEVEL_ERROR, this, error);
             
             rq.sendInternalServerError(error);
         }
     }
     
     /**
      * Method-interface for sending a response 
      * 
      * @param result - can be null, if not necessary.
      * @param rq - original {@link DIRRequest}.
      */
     abstract void requestFinished(Object result, DIRRequest rq);
     
     /**
      * Listener implementation for non-blocking BabuDB requests.
      * 
      * @author flangner
      * @since 11/16/2009
      * @param <I> - input type.
      * @param <O> - output type.
      */
     abstract class DBRequestListener<I,O> implements BabuDBRequestListener<I> {
 
         private final boolean finishRequest;
         
         DBRequestListener(boolean finishRequest) {
             this.finishRequest = finishRequest;
         }
         
         abstract O execute(I result, DIRRequest rq) throws Exception;
         
         /*
          * (non-Javadoc)
          * @see org.xtreemfs.babudb.BabuDBRequestListener#failed(org.xtreemfs.babudb.BabuDBException, java.lang.Object)
          */
         @Override
         public void failed(BabuDBException error, Object request) {
             requestFailed(error, (DIRRequest) request);
         }
 
         /*
          * (non-Javadoc)
          * @see org.xtreemfs.babudb.BabuDBRequestListener#finished(java.lang.Object, java.lang.Object)
          */
         @Override
         public void finished(I data, Object context) {
             try {
                 O result = execute(data, (DIRRequest) context);
                 if (finishRequest)
                     requestFinished(result, (DIRRequest) context);
                 
             } catch (Exception e) {
                 requestFailed(e, (DIRRequest) context);
             }
         }
         
     }
 }
