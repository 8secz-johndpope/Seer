 /*
  * Copyright 2012 Splunk, Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License"): you may
  * not use this file except in compliance with the License. You may obtain
  * a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  * License for the specific language governing permissions and limitations
  * under the License.
  */
 
 package com.splunk;
 
 import org.junit.After;
 import org.junit.Before;
 import org.junit.Test;
 
 import java.io.*;
 import java.net.Socket;
 
 public class IndexWithoutRestartTest extends SDKTestCase {
     private String indexName;
     private Index index;
 
     @Before
     @Override
     public void setUp() throws Exception {
         super.setUp();
         
         indexName = createTemporaryName();
         index = service.getIndexes().create(indexName);
         assertEventuallyTrue(new EventuallyTrueBehavior() {
             @Override
             public boolean predicate() {
                 return service.getIndexes().containsKey(indexName);
             }
         });
     }
 
     @After
     @Override
     public void tearDown() throws Exception {
         if (service.versionIsAtEarliest("5.0.0")) {
             if (service.getIndexes().containsKey(indexName)) {
                 index.remove();
             }
         } else {
             // Can't delete indexes via the REST API. Just let them build up.
         }
         
         super.tearDown();
     }
 
     @Test
     public void testDeletion() {
         if (service.versionIsEarlierThan("5.0.0")) {
             // Can't delete indexes via the REST API.
             return;
         }
         
         assertTrue(service.getIndexes().containsKey(indexName));
         
         index.remove();
         assertEventuallyTrue(new EventuallyTrueBehavior() {
             @Override
             public boolean predicate() {
                 return !service.getIndexes().containsKey(indexName);
             }
         });
     }
 
     @Test
     public void testAttachWith() throws IOException {
         final int originalEventCount = index.getTotalEventCount();
         
         index.attachWith(new ReceiverBehavior() {
             public void run(OutputStream stream) throws IOException {
                 String s = createTimestamp() + " Boris the mad baboon!\r\n";
                 stream.write(s.getBytes("UTF8"));
             }
         });
         
         assertEventuallyTrue(new EventuallyTrueBehavior() {
             @Override
             public boolean predicate() {
                 index.refresh();
                 return index.getTotalEventCount() == originalEventCount + 1;
             }
         });
     }
 
     @Test
     @SuppressWarnings("deprecation")
     public void testIndexGettersThrowNoErrors() {
         index.getAssureUTF8();
         index.getBlockSignatureDatabase();
         index.getBlockSignSize();
         index.getBloomfilterTotalSizeKB();
         index.getColdPath();
         index.getColdPathExpanded();
         index.getColdToFrozenDir();
         index.getColdToFrozenScript();
         index.getCompressRawdata();
         index.getCurrentDBSizeMB();
         index.getDefaultDatabase();
         index.getEnableRealtimeSearch();
         index.getFrozenTimePeriodInSecs();
         index.getHomePath();
         index.getHomePathExpanded();
         index.getIndexThreads();
         index.getLastInitTime();
         index.getMaxBloomBackfillBucketAge();
         index.getMaxConcurrentOptimizes();
         index.getMaxDataSize();
         index.getMaxHotBuckets();
         index.getMaxHotIdleSecs();
         index.getMaxHotSpanSecs();
         index.getMaxMemMB();
         index.getMaxMetaEntries();
         index.getMaxRunningProcessGroups();
         index.getMaxTime();
         index.getMaxTotalDataSizeMB();
         index.getMaxWarmDBCount();
         index.getMemPoolMB();
         index.getMinRawFileSyncSecs();
         index.getMinTime();
         index.getNumBloomfilters();
         index.getNumHotBuckets();
         index.getNumWarmBuckets();
         index.getPartialServiceMetaPeriod();
         index.getQuarantineFutureSecs();
         index.getQuarantinePastSecs();
         index.getRawChunkSizeBytes();
         index.getRotatePeriodInSecs();
         index.getServiceMetaPeriod();
         index.getSuppressBannerList();
         index.getSync();
         index.getSyncMeta();
         index.getThawedPath();
         index.getThawedPathExpanded();
         index.getThrottleCheckPeriod();
         index.getTotalEventCount();
         index.isDisabled();
         index.isInternal();
         
         // Fields only available from 5.0 on.
         if (service.versionIsAtEarliest("5.0.0")) {
             index.getBucketRebuildMemoryHint();
             index.getMaxTimeUnreplicatedNoAcks();
             index.getMaxTimeUnreplicatedWithAcks();
         }
     }
 
     @Test
     public void testSetters() {
         int newBlockSignSize = index.getBlockSignSize() + 1;
         index.setBlockSignSize(newBlockSignSize);
         int newFrozenTimePeriodInSecs = index.getFrozenTimePeriodInSecs()+1;
         index.setFrozenTimePeriodInSecs(newFrozenTimePeriodInSecs);
         int newMaxConcurrentOptimizes = index.getMaxConcurrentOptimizes()+1;
         index.setMaxConcurrentOptimizes(newMaxConcurrentOptimizes);
         String newMaxDataSize = "auto";
         index.setMaxDataSize(newMaxDataSize);
         int newMaxHotBuckets = index.getMaxHotBuckets()+1;
         index.setMaxHotBuckets(newMaxHotBuckets);
         int newMaxHotIdleSecs = index.getMaxHotIdleSecs()+1;
         index.setMaxHotIdleSecs(newMaxHotIdleSecs);
         int newMaxMemMB = index.getMaxMemMB()+1;
         index.setMaxMemMB(newMaxMemMB);
         int newMaxMetaEntries = index.getMaxMetaEntries()+1;
         index.setMaxMetaEntries(newMaxMetaEntries);
         int newMaxTotalDataSizeMB = index.getMaxTotalDataSizeMB()+1;
         index.setMaxTotalDataSizeMB(newMaxTotalDataSizeMB);
         int newMaxWarmDBCount = index.getMaxWarmDBCount()+1;
         index.setMaxWarmDBCount(newMaxWarmDBCount);
         String newMinRawFileSyncSecs = "disable";
         index.setMinRawFileSyncSecs(newMinRawFileSyncSecs);
         int newPartialServiceMetaPeriod = index.getPartialServiceMetaPeriod()+1;
         index.setPartialServiceMetaPeriod(newPartialServiceMetaPeriod);
         int newQuarantineFutureSecs = index.getQuarantineFutureSecs()+1;
         index.setQuarantineFutureSecs(newQuarantineFutureSecs);
         int newQuarantinePastSecs = index.getQuarantinePastSecs()+1;
         index.setQuarantinePastSecs(newQuarantinePastSecs);
         int newRawChunkSizeBytes = index.getRawChunkSizeBytes()+1;
         index.setRawChunkSizeBytes(newRawChunkSizeBytes);
         int newRotatePeriodInSecs = index.getRotatePeriodInSecs()+1;
         index.setRotatePeriodInSecs(newRotatePeriodInSecs);
         int newServiceMetaPeriod = index.getServiceMetaPeriod()+1;
         index.setServiceMetaPeriod(newServiceMetaPeriod);
         boolean newSyncMeta = !index.getSyncMeta();
         index.setSyncMeta(newSyncMeta);
         int newThrottleCheckPeriod = index.getThrottleCheckPeriod()+1;
         index.setThrottleCheckPeriod(newThrottleCheckPeriod);
         
         boolean newEnableOnlineBucketRepair = false;
         String newMaxBloomBackfillBucketAge = null;
         if (service.versionIsAtEarliest("4.3")) {
             newEnableOnlineBucketRepair = !index.getEnableOnlineBucketRepair();
             index.setEnableOnlineBucketRepair(newEnableOnlineBucketRepair);
             newMaxBloomBackfillBucketAge = "20d";
             index.setMaxBloomBackfillBucketAge(newMaxBloomBackfillBucketAge);
         }
         
         String newBucketRebuildMemoryHint = null;
         int newMaxTimeUnreplicatedNoAcks = -1;
         int newMaxTimeUnreplicatedWithAcks = -1;
         if (service.versionIsAtEarliest("5.0")) {
             newBucketRebuildMemoryHint = "auto";
             index.setBucketRebuildMemoryHint(newBucketRebuildMemoryHint);
             newMaxTimeUnreplicatedNoAcks = 300;
             index.setMaxTimeUnreplicatedNoAcks(newMaxTimeUnreplicatedNoAcks);
             newMaxTimeUnreplicatedWithAcks = 60;
             index.setMaxTimeUnreplicatedWithAcks(newMaxTimeUnreplicatedWithAcks);
         }
 
         index.update();
         index.refresh();
 
         assertEquals(newBlockSignSize, index.getBlockSignSize());
         assertEquals(newFrozenTimePeriodInSecs, index.getFrozenTimePeriodInSecs());
         assertEquals(newMaxConcurrentOptimizes, index.getMaxConcurrentOptimizes());
         assertEquals(newMaxDataSize, index.getMaxDataSize());
         assertEquals(newMaxHotBuckets, index.getMaxHotBuckets());
         assertEquals(newMaxHotIdleSecs, index.getMaxHotIdleSecs());
         assertEquals(newMaxMemMB, index.getMaxMemMB());
         assertEquals(newMaxMetaEntries, index.getMaxMetaEntries());
         assertEquals(newMaxTotalDataSizeMB, index.getMaxTotalDataSizeMB());
         assertEquals(newMaxWarmDBCount, index.getMaxWarmDBCount());
         assertEquals(newMinRawFileSyncSecs, index.getMinRawFileSyncSecs());
         assertEquals(newPartialServiceMetaPeriod, index.getPartialServiceMetaPeriod());
         assertEquals(newQuarantineFutureSecs, index.getQuarantineFutureSecs());
         assertEquals(newQuarantinePastSecs, index.getQuarantinePastSecs());
         assertEquals(newRawChunkSizeBytes, index.getRawChunkSizeBytes());
         assertEquals(newRotatePeriodInSecs, index.getRotatePeriodInSecs());
         assertEquals(newServiceMetaPeriod, index.getServiceMetaPeriod());
         assertEquals(newSyncMeta, index.getSyncMeta());
         assertEquals(newThrottleCheckPeriod, index.getThrottleCheckPeriod());
         if (service.versionIsAtEarliest("4.3")) {
             assertEquals(
                     newEnableOnlineBucketRepair,
                     index.getEnableOnlineBucketRepair()
             );
             assertEquals(
                     newMaxBloomBackfillBucketAge,
                     index.getMaxBloomBackfillBucketAge()
             );
         }
         if (service.versionIsAtEarliest("5.0")) {
             assertEquals(
                     newBucketRebuildMemoryHint,
                     index.getBucketRebuildMemoryHint()
             );
             assertEquals(
                     newMaxTimeUnreplicatedNoAcks,
                     index.getMaxTimeUnreplicatedNoAcks()
             );
             assertEquals(
                     newMaxTimeUnreplicatedWithAcks,
                     index.getMaxTimeUnreplicatedWithAcks()
             );
         }
         
         // TODO: Figure out which of the above setters is forcing
         //       Splunk to restart.
         clearRestartMessage();
     }
 
     @Test
     public void testEnable() {
         // Force the index to be disabled
         if (!index.isDisabled()) {
             index.disable();
             assertEventuallyTrue(new EventuallyTrueBehavior() {
                 @Override public boolean predicate() {
                     index.refresh();
                     return index.isDisabled();
                 }
             });
             clearRestartMessage();
         }
         
         index.enable();
         assertEventuallyTrue(new EventuallyTrueBehavior() {
             @Override public boolean predicate() {
                 index.refresh();
                 return !index.isDisabled();
             }
         });
     }
 
     @Test
     public void testSubmitOne() {
         assertTrue(getResultCountOfIndex() == 0);
         assertTrue(index.getTotalEventCount() == 0);
         
         index.submit(createTimestamp() + " This is a test of the emergency broadcasting system.");
         
         assertEventuallyTrue(new EventuallyTrueBehavior() {
             @Override
             public boolean predicate() {
                 index.refresh();
                 return index.getTotalEventCount() == 1;
             }
         });
     }
     
     @Test
     public void testSubmitOneInEachCall() {
         assertTrue(getResultCountOfIndex() == 0);
         assertTrue(index.getTotalEventCount() == 0);
         
         index.submit(createTimestamp() + " Hello world!\u0150");
         index.submit(createTimestamp() + " Goodbye world!\u0150");
         
         assertEventuallyTrue(new EventuallyTrueBehavior() {
             @Override
             public boolean predicate() {
                 return getResultCountOfIndex() == 2;
             }
         });
         assertEventuallyTrue(new EventuallyTrueBehavior() {
             @Override
             public boolean predicate() {
                 index.refresh();
                // Yeah 1, not 2. Weird isn't it?
                return index.getTotalEventCount() == 1;
             }
         });
     }
     
     @Test
     public void testSubmitMultipleInOneCall() {
         assertTrue(getResultCountOfIndex() == 0);
         assertTrue(index.getTotalEventCount() == 0);
         
         index.submit(
                 createTimestamp() + " Hello world!\u0150" + "\r\n" +
                 createTimestamp() + " Goodbye world!\u0150");
         
         assertEventuallyTrue(new EventuallyTrueBehavior() {
             @Override
             public boolean predicate() {
                 return getResultCountOfIndex() == 2;
             }
         });
         assertEventuallyTrue(new EventuallyTrueBehavior() {
             @Override
             public boolean predicate() {
                 index.refresh();
                // Yeah 1, not 2. Weird isn't it?
                return index.getTotalEventCount() == 1;
             }
         });
     }
 
     @Test
     public void testAttach() throws IOException {
         assertTrue(getResultCountOfIndex() == 0);
         assertTrue(index.getTotalEventCount() == 0);
         
         Socket socket = index.attach();
         OutputStream ostream = socket.getOutputStream();
         Writer out = new OutputStreamWriter(ostream, "UTF8");
         out.write(createTimestamp() + " Hello world!\u0150\r\n");
         out.write(createTimestamp() + " Goodbye world!\u0150\r\n");
         out.flush();
         socket.close();
         
         assertEventuallyTrue(new EventuallyTrueBehavior() {
             @Override
             public boolean predicate() {
                 return getResultCountOfIndex() == 2;
             }
         });
         assertEventuallyTrue(new EventuallyTrueBehavior() {
             @Override
             public boolean predicate() {
                 index.refresh();
                // Yeah 1, not 2. Weird isn't it?
                return index.getTotalEventCount() == 1;
             }
         });
     }
 
     @Test
     public void testUpload() throws Exception {
         installApplicationFromCollection("file_to_upload");
 
         final String splunkHome = service.getSettings().getSplunkHome();
         String[] pathComponents = {splunkHome, "etc", "apps", "file_to_upload", "log.txt"};
         File pathToLog = Util.joinPath(pathComponents);
 
         assertTrue("File to upload does not exist.", pathToLog.exists());
         final int eventCount = index.getTotalEventCount();
         index.upload(pathToLog.getAbsolutePath());
         assertEventuallyTrue(new EventuallyTrueBehavior() {
             @Override
             public boolean predicate() {
                 index.refresh();
                 return index.getTotalEventCount() == eventCount + 4;
             }
         });
     }
 
     @Test
     public void testSubmitAndClean() {
         assertTrue(index.getTotalEventCount() == 0);
         
         // Make sure the index is not empty.
         index.submit("Hello world");
         assertEventuallyTrue(new EventuallyTrueBehavior() {
             @Override
             public boolean predicate() {
                 index.refresh();
                 return index.getTotalEventCount() == 1;
             }
         });
         
         // Clean the index and make sure it's empty.
         index.clean(60);
         assertEventuallyTrue(new EventuallyTrueBehavior() {
             @Override
             public boolean predicate() {
                 index.refresh();
                 return index.getTotalEventCount() == 0;
             }
         });
     }
     
     // === Utility ===
 
     private int getResultCountOfIndex() {
         InputStream results = service.oneshot("search index=" + indexName);
         try {
             ResultsReaderXml resultsReader = new ResultsReaderXml(results);
             
             int numEvents = 0;
             while (resultsReader.getNextEvent() != null) {
                 numEvents++;
             }
             return numEvents;
         } catch (Exception e) {
             // TODO: Stop catching Exception once ResultsReader's interface
             //       has been cleaned up to not throw raw Exceptions.
             throw new RuntimeException(e);
         }
     }
 }
 
