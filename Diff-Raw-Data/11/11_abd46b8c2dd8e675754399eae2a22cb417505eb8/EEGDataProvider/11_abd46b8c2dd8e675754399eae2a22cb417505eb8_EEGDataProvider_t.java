 package com.github.dreamrec;
 
 import com.webkitchen.eeg.acquisition.EEGAcquisitionController;
 import com.webkitchen.eeg.acquisition.IRawSampleListener;
 import com.webkitchen.eeg.acquisition.RawSample;
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 
 import java.io.IOException;
 import java.util.Date;
 
 /**
  *
  */
 public class EEGDataProvider implements IDataProvider, IRawSampleListener {
 
     private static final Log log = LogFactory.getLog(EEGDataProvider.class);
     private double dataFrequency;
     private static final int FREQUENCY_DIVIDER = 25;
     private static final int INCOMING_DATA_MAX_VALUE = 1024;
     private int packetNumber = -1;
     private final int chanel;
     private AveragingBuffer averagingBuffer = new AveragingBuffer(FREQUENCY_DIVIDER);
     private long startTime;
     private long stopTime;
 
     public EEGDataProvider(int chanel, double dataFrequency) {
         this.chanel = chanel;
         this.dataFrequency = dataFrequency;
         EEGAcquisitionController.getInstance().getChannelSampleGenerator().addSampleListener(this, new int[]{chanel + 1});
     }
 
     public void startRecording() throws ApplicationException {
         try {
             EEGAcquisitionController.getInstance().startReading(false);
         } catch (IOException e) {
             log.error(e);
             throw new ApplicationException("EEG machine reading failure ", e);
         }
         startTime = System.currentTimeMillis();
         log.info("StartTime: " + new Date(startTime));
     }
 
     public void stopRecording() {
         stopTime = System.currentTimeMillis();
         EEGAcquisitionController.getInstance().stopReading();
         int numberOfIncomingPackets = averagingBuffer.getIncomingCounter();
         log.info("StopTime: " + new Date(stopTime));
         log.info("Predefined data frequency = " + dataFrequency);
         log.info("Real incoming data frequency = " + numberOfIncomingPackets * 1000 / (stopTime - startTime));
     }
 
     public double getIncomingDataFrequency() {
         return dataFrequency;
     }
 
     public long getStartTime() {
         return startTime;
     }
 
     public int poll() {
         return averagingBuffer.poll();
     }
 
     public int available() {
         return averagingBuffer.available();
     }
 
     public void receiveSample(RawSample rawSample) {
         checkLostPackets(rawSample.getPacketNumber());
         int incomingValue = rawSample.getSamples()[0];
         checkIncomingValue(incomingValue);
         averagingBuffer.add(incomingValue);
     }
 
     private void checkIncomingValue(int incomingValue) {
         if (Math.abs(incomingValue) > INCOMING_DATA_MAX_VALUE) {
             log.warn("Received value exceeds maximum. Value = " + incomingValue + "    Max value = " + INCOMING_DATA_MAX_VALUE);
         }
     }
 
     private void checkLostPackets(int newPacketNumber) {
         if (packetNumber == -1) {
             packetNumber = newPacketNumber;
            return;
         }
        int lostPacketsNormal = newPacketNumber - packetNumber -1;
        int lostPacketsOnBound = (newPacketNumber + 256) - packetNumber - 1; // In the case when PacketNumber passes the bound 255 and return to 0
        int lostPackets = Math.min(lostPacketsNormal, lostPacketsOnBound);
        if (lostPackets != 0) {
            log.warn("Lost packet!!! Packet number = " + packetNumber + "; " + lostPackets + "packets were lost");
         }
        packetNumber = newPacketNumber;
     }
 }
