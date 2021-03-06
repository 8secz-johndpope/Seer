 /**
  * Copyright 2009, Frederic Bregier, and individual contributors
  * by the @author tags. See the COPYRIGHT.txt in the distribution for a
  * full listing of individual contributors.
  *
  * This is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as
  * published by the Free Software Foundation; either version 3.0 of
  * the License, or (at your option) any later version.
  *
  * This software is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this software; if not, write to the Free
  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
  */
 package openr66.client;
 
 import goldengate.common.logging.GgInternalLoggerFactory;
 
 import openr66.commander.ClientRunner;
 import openr66.context.ErrorCode;
 import openr66.context.R66Result;
 import openr66.context.task.exception.OpenR66RunnerErrorException;
 import openr66.database.DbConstant;
 import openr66.database.data.DbRule;
 import openr66.database.data.DbTaskRunner;
 import openr66.database.exception.OpenR66DatabaseException;
 import openr66.protocol.exception.OpenR66ProtocolNoConnectionException;
 import openr66.protocol.exception.OpenR66ProtocolPacketException;
 import openr66.protocol.localhandler.LocalChannelReference;
 import openr66.protocol.localhandler.packet.RequestPacket;
 import openr66.protocol.networkhandler.NetworkTransaction;
import openr66.protocol.test.TestRecvThroughClient;
 import openr66.protocol.utils.R66Future;
 
 /**
  * Class for Recv Through client
  *
  * This class does not included the real file transfer since it is up to the business project
  * to implement how to write new data received from the remote host. If an error occurs,
  * no transfer log is kept.
  *
  * 1) Configuration must have been loaded<br>
  * <br>
  * 2) Pipeline and NetworkTransaction must have been initiated:<br>
  * <tt>     Configuration.configuration.pipelineInit();</tt><br>
  * <tt>     NetworkTransaction networkTransaction = new NetworkTransaction();</tt><br>
  * <br>
  * 3) Prepare the request of transfer:<br>
  * <tt>     R66Future futureReq = new R66Future(true);</tt><br>
  * <tt>     RecvThroughHandler rth = new RecvThroughHandler(...);</tt><br>
  * <tt>     RecvThroughClient transaction = new RecvThroughClient(futureReq, rth, ...);</tt><br>
  * <tt>     transaction.run();</tt><br>
  * <br>
  * 4) If everything is in success, wait for the transfer to finish:<br>
  * <tt>     futureReq.awaitUninterruptibly();</tt><br>
  * <tt>     R66Result result = futureReq.getResult();</tt><br>
  * <br>
  * 5) If there is the need to re-do, just re-execute the steps from 3 to 4.<br>
  * Don't forget at the very end to finish the global structure (steps 3 to 4 no more executed):<br>
  * <tt>     networkTransaction.closeAll();</tt><br>
  * <br>
  * <br>
 * @see TestRecvThroughClient {@link TestRecvThroughClient} Class as example of usage
  *
  * @author Frederic Bregier
  *
  */
 public class RecvThroughClient extends AbstractTransfer {
     protected final NetworkTransaction networkTransaction;
     protected LocalChannelReference localChannelReference;
     protected final RecvThroughHandler handler;
     /**
      * @param future
      * @param remoteHost
      * @param filename
      * @param rulename
      * @param fileinfo
      * @param isMD5
      * @param blocksize
      * @param networkTransaction
      */
     public RecvThroughClient(R66Future future, RecvThroughHandler handler, String remoteHost,
             String filename, String rulename, String fileinfo, boolean isMD5,
             int blocksize, NetworkTransaction networkTransaction) {
         super(RecvThroughClient.class,
                 future, filename, rulename, fileinfo, isMD5, remoteHost, blocksize);
         this.networkTransaction = networkTransaction;
         this.handler = handler;
     }
     /**
      * Prior to call this method, the pipeline and NetworkTransaction must have been initialized.
      * It is the responsibility of the caller to finish all network resources.
      */
     public void run() {
         if (logger == null) {
             logger = GgInternalLoggerFactory.getLogger(RecvThroughClient.class);
         }
         DbRule rule;
         try {
             rule = new DbRule(DbConstant.admin.session, rulename);
         } catch (OpenR66DatabaseException e) {
             logger.error("Cannot get Rule: "+rulename, e);
             future.setResult(new R66Result(e, null, true,
                     ErrorCode.Internal));
             future.setFailure(e);
             return;
         }
         int mode = rule.mode;
         if (isMD5) {
             mode = RequestPacket.getModeMD5(mode);
         }
         RequestPacket request = new RequestPacket(rulename,
                 mode, filename, blocksize, 0,
                 DbConstant.ILLEGALVALUE, fileinfo);
         // isRecv is True since it is the requester, so recv => isSender is false
         boolean isSender = false;
         DbTaskRunner taskRunner = null;
         try {
             try {
                 taskRunner =
                     new DbTaskRunner(DbConstant.admin.session,rule,isSender,request,remoteHost);
             } catch (OpenR66DatabaseException e) {
                 logger.error("Cannot get task", e);
                 future.setResult(new R66Result(e, null, true,
                         ErrorCode.Internal));
                 future.setFailure(e);
                 return;
             }
             ClientRunner runner = new ClientRunner(networkTransaction, taskRunner, future);
             runner.setRecvThroughHandler(handler);
             try {
                 runner.runTransfer();
             } catch (OpenR66RunnerErrorException e) {
                 logger.error("Cannot Transfer", e);
                 future.setResult(new R66Result(e, null, true,
                         ErrorCode.Internal));
                 future.setFailure(e);
                 return;
             } catch (OpenR66ProtocolNoConnectionException e) {
                 logger.error("Cannot Connect", e);
                 future.setResult(new R66Result(e, null, true,
                         ErrorCode.ConnectionImpossible));
                 future.setFailure(e);
                 return;
             } catch (OpenR66ProtocolPacketException e) {
                 logger.error("Bad Protocol", e);
                 future.setResult(new R66Result(e, null, true,
                         ErrorCode.TransferError));
                 future.setFailure(e);
                 return;
             }
         } finally {
             if (taskRunner != null) {
                 if (future.isCancelled() || nolog) {
                     try {
                         taskRunner.delete();
                     } catch (OpenR66DatabaseException e) {
                     }
                 }
             }
         }
     }
 
 }
