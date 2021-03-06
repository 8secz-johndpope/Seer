 /*
  * Copyright (c) 2008-2013, Hazelcast, Inc. All Rights Reserved.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package com.hazelcast.map.operation;
 
 import com.hazelcast.config.MapConfig;
 import com.hazelcast.map.MapContainer;
 import com.hazelcast.map.MapService;
 import com.hazelcast.map.PartitionContainer;
 import com.hazelcast.map.RecordStore;
 import com.hazelcast.map.record.Record;
 import com.hazelcast.map.record.RecordReplicationInfo;
 import com.hazelcast.nio.ObjectDataInput;
 import com.hazelcast.nio.ObjectDataOutput;
 import com.hazelcast.nio.serialization.Data;
 import com.hazelcast.nio.serialization.SerializationService;
 import com.hazelcast.spi.AbstractOperation;
 import com.hazelcast.util.Clock;
 import com.hazelcast.util.scheduler.ScheduledEntry;
 
 import java.io.IOException;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Map;
 import java.util.Map.Entry;
 import java.util.Set;
 
 /**
  * @author mdogan 7/24/12
  */
 public class MapReplicationOperation extends AbstractOperation {
 
     private Map<String, Set<RecordReplicationInfo>> data;
     private Map<String, Boolean> mapInitialLoadInfo;
 
     public MapReplicationOperation() {
     }
 
     public MapReplicationOperation(PartitionContainer container, int partitionId, int replicaIndex) {
         this.setPartitionId(partitionId).setReplicaIndex(replicaIndex);
         SerializationService ss = container.getMapService().getSerializationService();
         data = new HashMap<String, Set<RecordReplicationInfo>>(container.getMaps().size());
         mapInitialLoadInfo = new HashMap<String, Boolean>(container.getMaps().size());
         for (Entry<String, RecordStore> entry : container.getMaps().entrySet()) {
             RecordStore recordStore = entry.getValue();
             MapContainer mapContainer = recordStore.getMapContainer();
             final MapConfig mapConfig = mapContainer.getMapConfig();
             if (mapConfig.getTotalBackupCount() < replicaIndex) {
                 continue;
             }
 
             String name = entry.getKey();
             // adding if initial data is loaded for the only maps that has mapstore behind
             if(mapContainer.getStore() != null) {
                mapInitialLoadInfo.put(name, recordStore.isLoaded());
             }
             // now prepare data to migrate records
            Set<RecordReplicationInfo> recordSet = new HashSet<RecordReplicationInfo>(recordStore.size());
             for (Entry<Data, Record> recordEntry : recordStore.getReadonlyRecordMap().entrySet()) {
                 Data key = recordEntry.getKey();
                 Record record = recordEntry.getValue();
                 RecordReplicationInfo recordReplicationInfo;
                 if (replicaIndex == 0) {
                     recordReplicationInfo = createScheduledRecordState(mapContainer, recordEntry, key);
                 } else {
                     recordReplicationInfo = new RecordReplicationInfo(record.getKey(), ss.toData(record.getValue()), record.getStatistics());
                 }
                 recordSet.add(recordReplicationInfo);
             }
             data.put(name, recordSet);
         }
 
     }
 
     private RecordReplicationInfo createScheduledRecordState(MapContainer mapContainer, Entry<Data, Record> recordEntry, Data key) {
         ScheduledEntry idleScheduledEntry = mapContainer.getIdleEvictionScheduler() == null ? null : mapContainer.getIdleEvictionScheduler().cancel(key);
         long idleDelay = idleScheduledEntry == null ? -1 : findDelayMillis(idleScheduledEntry);
 
         ScheduledEntry ttlScheduledEntry = mapContainer.getTtlEvictionScheduler() == null ? null : mapContainer.getTtlEvictionScheduler().cancel(key);
         long ttlDelay = ttlScheduledEntry == null ? -1 : findDelayMillis(ttlScheduledEntry);
 
         ScheduledEntry writeScheduledEntry = mapContainer.getMapStoreWriteScheduler() == null ? null : mapContainer.getMapStoreWriteScheduler().cancel(key);
         long writeDelay = writeScheduledEntry == null ? -1 : findDelayMillis(writeScheduledEntry);
 
         ScheduledEntry deleteScheduledEntry = mapContainer.getMapStoreDeleteScheduler() == null ? null : mapContainer.getMapStoreDeleteScheduler().cancel(key);
         long deleteDelay = deleteScheduledEntry == null ? -1 : findDelayMillis(deleteScheduledEntry);
 
         Record record = recordEntry.getValue();
         SerializationService ss = mapContainer.getMapService().getSerializationService();
         return new RecordReplicationInfo(record.getKey(), ss.toData(record.getValue()), record.getStatistics(),
                 idleDelay, ttlDelay, writeDelay, deleteDelay);
     }
 
     public void run() {
         MapService mapService = getService();
         if (data != null) {
             for (Entry<String, Set<RecordReplicationInfo>> dataEntry : data.entrySet()) {
                 Set<RecordReplicationInfo> recordReplicationInfos = dataEntry.getValue();
                 final String mapName = dataEntry.getKey();
                 RecordStore recordStore = mapService.getRecordStore(getPartitionId(), mapName);
                 for (RecordReplicationInfo recordReplicationInfo : recordReplicationInfos) {
                     Data key = recordReplicationInfo.getKey();
                     Record newRecord = mapService.createRecord(mapName, key, recordReplicationInfo.getValue(), -1, false);
                     newRecord.setStatistics(recordReplicationInfo.getStatistics());
                     recordStore.putRecord(key, newRecord);
                     if (recordReplicationInfo.getIdleDelayMillis() >= 0) {
                         mapService.scheduleIdleEviction(mapName, key, recordReplicationInfo.getIdleDelayMillis());
                     }
                     if (recordReplicationInfo.getTtlDelayMillis() >= 0) {
                         mapService.scheduleTtlEviction(mapName, newRecord, recordReplicationInfo.getTtlDelayMillis());
                     }
                     if (recordReplicationInfo.getMapStoreWriteDelayMillis() >= 0) {
                         mapService.scheduleMapStoreWrite(mapName, key, newRecord.getValue(), recordReplicationInfo.getMapStoreWriteDelayMillis());
                     }
                     if (recordReplicationInfo.getMapStoreDeleteDelayMillis() >= 0) {
                         mapService.scheduleMapStoreDelete(mapName, key, recordReplicationInfo.getMapStoreDeleteDelayMillis());
                     }
                 }
             }
         }
         if(mapInitialLoadInfo != null) {
             for (String mapName : mapInitialLoadInfo.keySet()) {
                 RecordStore recordStore = mapService.getRecordStore(getPartitionId(), mapName);
                 recordStore.setLoaded(mapInitialLoadInfo.get(mapName));
             }
         }
     }
 
     private long findDelayMillis(ScheduledEntry entry) {
         return Math.max(0, entry.getScheduledDelayMillis() - (Clock.currentTimeMillis() - entry.getScheduleTime()));
     }
 
     public String getServiceName() {
         return MapService.SERVICE_NAME;
     }
 
     protected void readInternal(final ObjectDataInput in) throws IOException {
         int size = in.readInt();
         data = new HashMap<String, Set<RecordReplicationInfo>>(size);
         for (int i = 0; i < size; i++) {
             String name = in.readUTF();
             int mapSize = in.readInt();
             Set<RecordReplicationInfo> recordReplicationInfos = new HashSet<RecordReplicationInfo>(mapSize);
             for (int j = 0; j < mapSize; j++) {
                 RecordReplicationInfo recordReplicationInfo = in.readObject();
                 recordReplicationInfos.add(recordReplicationInfo);
             }
             data.put(name, recordReplicationInfos);
         }
         size = in.readInt();
         mapInitialLoadInfo = new HashMap<String, Boolean>(size);
         for (int i = 0; i < size; i++) {
             String name = in.readUTF();
             boolean loaded = in.readBoolean();
             mapInitialLoadInfo.put(name, loaded);
         }
     }
 
     protected void writeInternal(final ObjectDataOutput out) throws IOException {
         out.writeInt(data.size());
         for (Entry<String, Set<RecordReplicationInfo>> mapEntry : data.entrySet()) {
             out.writeUTF(mapEntry.getKey());
             Set<RecordReplicationInfo> recordReplicationInfos = mapEntry.getValue();
             out.writeInt(recordReplicationInfos.size());
             for (RecordReplicationInfo recordReplicationInfo : recordReplicationInfos) {
                 out.writeObject(recordReplicationInfo);
             }
         }
         out.writeInt(mapInitialLoadInfo.size());
         for (Entry<String, Boolean> entry : mapInitialLoadInfo.entrySet()) {
             out.writeUTF(entry.getKey());
             out.writeBoolean(entry.getValue());
         }
     }
 
     public boolean isEmpty() {
         return data == null || data.isEmpty();
     }
 }
