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
 package org.xtreemfs.osd.operations;
 
 import java.util.List;
 
 import org.xtreemfs.common.Capability;
 import org.xtreemfs.common.buffer.ReusableBuffer;
 import org.xtreemfs.common.uuids.ServiceUUID;
 import org.xtreemfs.common.uuids.UnknownUUIDException;
 import org.xtreemfs.common.xloc.StripingPolicyImpl;
 import org.xtreemfs.common.xloc.XLocations;
 import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
 import org.xtreemfs.interfaces.Constants;
 import org.xtreemfs.interfaces.InternalGmax;
 import org.xtreemfs.interfaces.ObjectData;
 import org.xtreemfs.interfaces.OSDInterface.OSDException;
 import org.xtreemfs.interfaces.OSDInterface.readRequest;
 import org.xtreemfs.interfaces.OSDInterface.readResponse;
 import org.xtreemfs.interfaces.utils.ONCRPCException;
 import org.xtreemfs.interfaces.utils.Serializable;
 import org.xtreemfs.osd.ErrorCodes;
 import org.xtreemfs.osd.OSDRequest;
 import org.xtreemfs.osd.OSDRequestDispatcher;
 import org.xtreemfs.osd.stages.ReplicationStage.FetchObjectCallback;
 import org.xtreemfs.osd.stages.StorageStage.ReadObjectCallback;
 import org.xtreemfs.osd.storage.ObjectInformation;
 import org.xtreemfs.osd.storage.ObjectInformation.ObjectStatus;
 
 public final class ReadOperation extends OSDOperation {
 
     final int procId;
 
     final String sharedSecret;
 
     final ServiceUUID localUUID;
 
     public ReadOperation(OSDRequestDispatcher master) {
         super(master);
         readRequest rq = new readRequest();
         procId = rq.getOperationNumber();
         sharedSecret = master.getConfig().getCapabilitySecret();
         localUUID = master.getConfig().getUUID();
     }
 
     @Override
     public int getProcedureId() {
         return procId;
     }
 
     @Override
     public void startRequest(final OSDRequest rq) {
         final readRequest args = (readRequest) rq.getRequestArgs();
 
         if (args.getObject_number() < 0) {
             rq.sendException(new OSDException(ErrorCodes.INVALID_PARAMS, "object number must be >= 0", ""));
             return;
         }
 
         if (args.getOffset() < 0) {
             rq.sendException(new OSDException(ErrorCodes.INVALID_PARAMS, "offset must be >= 0", ""));
             return;
         }
 
         if (args.getLength() < 0) {
             rq.sendException(new OSDException(ErrorCodes.INVALID_PARAMS, "length must be >= 0", ""));
             return;
         }
 
         final StripingPolicyImpl sp = rq.getLocationList().getLocalReplica().getStripingPolicy();
 
         if (args.getLength()+args.getOffset() > sp.getStripeSizeForObject(0)) {
             rq.sendException(new OSDException(ErrorCodes.INVALID_PARAMS, "length + ofset must be <= "+sp.getStripeSizeForObject(0)+" (stripe size)", ""));
             return;
         }
 
         master.getStorageStage().readObject(args.getFile_id(), args.getObject_number(), sp, rq, new ReadObjectCallback() {
 
             @Override
             public void readComplete(ObjectInformation result, Exception error) {
                 postRead(rq, args, result, error);
             }
         });
     }
 
     public void postRead(final OSDRequest rq, readRequest args, ObjectInformation result, Exception error) {
         if (error != null) {
             if (error instanceof ONCRPCException) {
                 rq.sendException((ONCRPCException) error);
             } else {
                 rq.sendInternalServerError(error);
             }
         } else {
             if (result.getStatus() == ObjectInformation.ObjectStatus.DOES_NOT_EXIST
                     && rq.getLocationList().getReplicaUpdatePolicy().equals(Constants.REPL_UPDATE_PC_RONLY)) {
                 // read only replication!
                 readReplica(rq, args);
             } else {
                 if (rq.getLocationList().getLocalReplica().isStriped()) {
                     // striped read
                     stripedRead(rq, args, result);
                } else {
                    // non-striped case
                    nonStripedRead(rq, args, result);
                 }
             }
         }
 
     }
 
     private void nonStripedRead(OSDRequest rq, readRequest args, ObjectInformation result) {
 
         final boolean isLastObjectOrEOF = result.getLastLocalObjectNo() <= args.getObject_number();
         readFinish(rq, args, result, isLastObjectOrEOF);
     }
 
     private void stripedRead(final OSDRequest rq, final readRequest args, final ObjectInformation result) {
         ObjectData data;
         final long objNo = args.getObject_number();
         final long lastKnownObject = Math.max(result.getLastLocalObjectNo(), result.getGlobalLastObjectNo());
         final boolean isLastObjectLocallyKnown = lastKnownObject <= objNo;
         //check if GMAX must be fetched to determin EOF
         if ((objNo > lastKnownObject) ||
                 (objNo == lastKnownObject) && (result.getData() != null) && (result.getData().remaining() < result.getStripeSize())) {
             try {
                 final List<ServiceUUID> osds = rq.getLocationList().getLocalReplica().getOSDs();
                 final RPCResponse[] gmaxRPCs = new RPCResponse[osds.size() - 1];
                 int cnt = 0;
                 for (ServiceUUID osd : osds) {
                     if (!osd.equals(localUUID)) {
                         gmaxRPCs[cnt++] = master.getOSDClient().internal_get_gmax(osd.getAddress(), args.getFile_id(), args.getFile_credentials());
                     }
                 }
                 this.waitForResponses(gmaxRPCs, new ResponsesListener() {
 
                     @Override
                     public void responsesAvailable() {
                         stripedReadAnalyzeGmax(rq, args, result, gmaxRPCs);
                     }
                 });
             } catch (UnknownUUIDException ex) {
                 rq.sendInternalServerError(ex);
                 return;
             }
         } else {
             readFinish(rq, args, result, isLastObjectLocallyKnown);
         }
     }
 
     private void stripedReadAnalyzeGmax(final OSDRequest rq, final readRequest args,
             final ObjectInformation result, RPCResponse[] gmaxRPCs) {
         long maxObjNo = -1;
         long maxTruncate = -1;
 
         try {
             for (int i = 0; i < gmaxRPCs.length; i++) {
                 InternalGmax gmax = (InternalGmax) gmaxRPCs[i].get();
                 if ((gmax.getLast_object_id() > maxObjNo) && (gmax.getEpoch() >= maxTruncate)) {
                     //found new max
                     maxObjNo = gmax.getLast_object_id();
                     maxTruncate = gmax.getEpoch();
                 }
             }
             final boolean isLastObjectLocallyKnown = maxObjNo <= args.getObject_number();
             readFinish(rq, args, result, isLastObjectLocallyKnown);
             //and update gmax locally
             master.getStorageStage().receivedGMAX_ASYNC(args.getFile_id(), maxTruncate, maxObjNo);
         } catch (Exception ex) {
             rq.sendInternalServerError(ex);
         } finally {
             for (RPCResponse r : gmaxRPCs)
                 r.freeBuffers();
         }
 
     }
 
     private void readFinish(OSDRequest rq, readRequest args, ObjectInformation result, boolean isLastObjectOrEOF) {
         final boolean isRangeRequested = (args.getOffset() > 0) || (args.getLength() < result.getStripeSize());
         ObjectData data;
         if (isRangeRequested) {
             data = result.getObjectData(isLastObjectOrEOF, args.getOffset(), args.getLength());
         } else {
             data = result.getObjectData(isLastObjectOrEOF);
         }
         master.objectSent();
         if (data.getData() != null)
             master.dataSent(data.getData().capacity());
 
         sendResponse(rq, data);
     }
     
     private void readReplica(final OSDRequest rq, final readRequest args) {
         XLocations xLoc = rq.getLocationList();
         StripingPolicyImpl sp = xLoc.getLocalReplica().getStripingPolicy();
 
         // check if it is a EOF
         if (args.getObject_number() > sp.getObjectNoForOffset(xLoc.getXLocSet().getRead_only_file_size() - 1)) {
             ObjectInformation objectInfo = new ObjectInformation(ObjectStatus.DOES_NOT_EXIST, null, sp
                     .getStripeSizeForObject(args.getObject_number()));
             objectInfo.setGlobalLastObjectNo(xLoc.getXLocSet().getRead_only_file_size());
 
             readFinish(rq, args, objectInfo, true);
         } else {
             master.getReplicationStage().fetchObject(args.getFile_id(), args.getObject_number(), xLoc,
                     rq.getCapability(), rq.getCowPolicy(), rq, new FetchObjectCallback() {
                         @Override
                         public void fetchComplete(ObjectInformation objectInfo, Exception error) {
                             postReadReplica(rq, args, objectInfo, error);
                         }
                     });
         }
     }
 
     public void postReadReplica(final OSDRequest rq, readRequest args, ObjectInformation result, Exception error) {
         XLocations xLoc = rq.getLocationList();
         StripingPolicyImpl sp = xLoc.getLocalReplica().getStripingPolicy();
 
         if (error != null) {
             if (error instanceof ONCRPCException) {
                 rq.sendException((ONCRPCException) error);
             } else {
                 rq.sendInternalServerError(error);
             }
         } else {
             // TODO: check implementation!
             if (args.getObject_number() == sp.getObjectNoForOffset(xLoc.getXLocSet().getRead_only_file_size() - 1))
                 // last object
                 readFinish(rq, args, result, true);
             else
                 readFinish(rq, args, result, false);
         }
     }
 
     public void sendResponse(OSDRequest rq, ObjectData result) {
         readResponse response = new readResponse(result);
         rq.sendSuccess(response);
     }
 
     @Override
     public Serializable parseRPCMessage(ReusableBuffer data, OSDRequest rq) throws Exception {
         readRequest rpcrq = new readRequest();
         rpcrq.deserialize(data);
 
         rq.setFileId(rpcrq.getFile_id());
         rq.setCapability(new Capability(rpcrq.getFile_credentials().getXcap(), sharedSecret));
         rq.setLocationList(new XLocations(rpcrq.getFile_credentials().getXlocs(), localUUID));
 
         return rpcrq;
     }
 
     @Override
     public boolean requiresCapability() {
         return true;
     }
 
     @Override
     public void startInternalEvent(Object[] args) {
         throw new UnsupportedOperationException("Not supported yet.");
     }
 }
