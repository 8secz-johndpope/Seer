 /**
  * Copyright (c) 2011 Metropolitan Transportation Authority
  *
  * Licensed under the Apache License, Version 2.0 (the "License"); you may not
  * use this file except in compliance with the License. You may obtain a copy of
  * the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  * License for the specific language governing permissions and limitations under
  * the License.
  */
 package org.onebusaway.nyc.vehicle_tracking.impl.inference;
 
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.Date;
 import java.util.List;
 import java.util.Map;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.ConcurrentMap;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Executors;
 
 import javax.annotation.PostConstruct;
 import javax.annotation.PreDestroy;
 
 import lrms_final_09_07.Angle;
 
 import org.joda.time.format.DateTimeFormatter;
 import org.joda.time.format.ISODateTimeFormat;
 import org.onebusaway.gtfs.model.AgencyAndId;
 import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
 import org.onebusaway.nyc.transit_data.model.NycVehicleManagementStatusBean;
 import org.onebusaway.nyc.transit_data_federation.services.bundle.BundleManagementService;
 import org.onebusaway.nyc.transit_data_federation.services.bundle.model.BundleItem;
 import org.onebusaway.nyc.transit_data_federation.services.tdm.VehicleAssignmentService;
 import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyPhaseSummary;
 import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
 import org.onebusaway.nyc.vehicle_tracking.model.NycTestInferredLocationRecord;
 import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;
 import org.onebusaway.nyc.vehicle_tracking.model.simulator.VehicleLocationDetails;
 import org.onebusaway.nyc.vehicle_tracking.services.inference.VehicleLocationInferenceService;
 import org.onebusaway.nyc.vehicle_tracking.services.queue.OutputQueueSenderService;
 import org.onebusaway.transit_data.model.blocks.BlockBean;
 import org.onebusaway.transit_data.model.trips.TripBean;
 import org.onebusaway.transit_data.services.TransitDataService;
 
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.context.ApplicationContext;
 import org.springframework.stereotype.Component;
 
 import com.jhlabs.map.proj.ProjectionException;
 
 import tcip_3_0_5_local.NMEA;
 import tcip_final_3_0_5_1.CcLocationReport;
 import tcip_final_3_0_5_1.CcLocationReport.EmergencyCodes;
 
 @Component
 public class VehicleLocationInferenceServiceImpl implements
     VehicleLocationInferenceService {
 
   private static Logger _log = LoggerFactory.getLogger(VehicleLocationInferenceServiceImpl.class);
 
   private static final DateTimeFormatter XML_DATE_TIME_FORMAT = ISODateTimeFormat.dateTimeParser();
 
   @Autowired
   private OutputQueueSenderService _outputQueueSenderService;
 
   @Autowired
   private VehicleAssignmentService _vehicleAssignmentService;
 
   @Autowired
   private BundleManagementService _bundleManagementService;
   
   @Autowired
   private TransitDataService _transitDataService;
   
   private BundleItem _lastBundle = null;
   
   private ExecutorService _executorService;
 
   private int _numberOfProcessingThreads = 10;
 
   private ConcurrentMap<AgencyAndId, VehicleInferenceInstance> _vehicleInstancesByVehicleId = new ConcurrentHashMap<AgencyAndId, VehicleInferenceInstance>();
 
   private ApplicationContext _applicationContext;
 
   /**
    * Usually, we shoudn't ever have a reference to ApplicationContext, but we
    * need it for the prototype
    * 
    * @param applicationContext
    */
   @Autowired
   public void setApplicationContext(ApplicationContext applicationContext) {
     _applicationContext = applicationContext;
   }
 
   public void setNumberOfProcessingThreads(int numberOfProcessingThreads) {
     _numberOfProcessingThreads = numberOfProcessingThreads;
   }
 
   /**
    * Has the bundle changed since the last time we returned a result?
    *
    * @return boolean: bundle changed or not
    */
   private boolean bundleHasChanged() {
     BundleItem currentBundle = _bundleManagementService.getCurrentBundleMetadata();
     
     boolean result = false;
 
     // active bundle was removed from BMS list of active bundles
     if(currentBundle == null)
       return true;
     
     if(_lastBundle != null) {
       result = !_lastBundle.getId().equals(currentBundle.getId());
     }
     
     _lastBundle = currentBundle;
     return result;
   }
   
   /**
    * If the bundle has changed, verify that all vehicle results are present in the current bundle.
    * If not, reset the inference to map them to the current reference data (bundle).
    * 
    */
   private synchronized void verifyVehicleResultMappingToCurrentBundle() {
     if(!bundleHasChanged())
       return;
     
     for(AgencyAndId vehicleId : _vehicleInstancesByVehicleId.keySet()) {
       VehicleInferenceInstance vehicleInstance = _vehicleInstancesByVehicleId.get(vehicleId);
       NycTestInferredLocationRecord state = vehicleInstance.getCurrentState();
 
       // no state
       if(state == null) {
         _log.info("Vehicle " + vehicleId + " reset on bundle change: no state available.");
         this.resetVehicleLocation(vehicleId);
         continue;
       }
       
       // no match to any trip
       if(state.getInferredBlockId() == null || state.getInferredTripId() == null) {
         _log.info("Vehicle " + vehicleId + " reset on bundle change: no matched trip/block.");
         this.resetVehicleLocation(vehicleId);
         continue;
       }
         
       // trip or block matched have disappeared!
       TripBean trip = _transitDataService.getTrip(state.getInferredTripId());
       BlockBean block = _transitDataService.getBlockForId(state.getInferredBlockId());
 
       if(trip == null || block == null) {
         _log.info("Vehicle " + vehicleId + " reset on bundle change: trip/block is no longer present.");
         this.resetVehicleLocation(vehicleId);
         continue;
       }
     }
   }
   
   @PostConstruct
   public void start() {
     if (_numberOfProcessingThreads <= 0)
       throw new IllegalArgumentException(
           "numberOfProcessingThreads must be positive");
     _executorService = Executors.newFixedThreadPool(_numberOfProcessingThreads);
   }
 
   @PreDestroy
   public void stop() {
     _executorService.shutdownNow();
   }
 
   @Override
   public void handleNycRawLocationRecord(NycRawLocationRecord record) {
     verifyVehicleResultMappingToCurrentBundle();
 
     _executorService.execute(new ProcessingTask(record));
   }
 
   @Override
   public void handleNycTestInferredLocationRecord(NycTestInferredLocationRecord record) {
     verifyVehicleResultMappingToCurrentBundle();
 
     _executorService.execute(new ProcessingTask(record));
   }
 
   public void handleCcLocationReportRecord(CcLocationReport message) {
     verifyVehicleResultMappingToCurrentBundle();
 
     NycRawLocationRecord r = new NycRawLocationRecord();
 
 	  r.setTime(XML_DATE_TIME_FORMAT.parseMillis(message.getTimeReported()));
 	  r.setTimeReceived(new Date().getTime());
 	
 	  r.setLatitude(message.getLatitude() / 1000000f);
 	  r.setLongitude(message.getLongitude() / 1000000f);
 
 	  Angle bearing = message.getDirection();
 	  if(bearing != null) {
 		  Integer degrees = bearing.getCdeg();		  
 		  if(degrees != null)
 			  r.setBearing(degrees);
 	  }
 	  
 	  r.setDestinationSignCode(message.getDestSignCode().toString());
 	  r.setDeviceId(message.getManufacturerData());
 	  
 	  AgencyAndId vehicleId = new AgencyAndId(
 			  message.getVehicle().getAgencydesignator(), 
 			  Long.toString(message.getVehicle().getVehicleId()));
 	  r.setVehicleId(vehicleId);
 
 	  r.setOperatorId(message.getOperatorID().getDesignator());
 	  r.setRunId(message.getRunID().getDesignator());
 	  r.setRunRouteId(message.getRouteID().getRouteDesignator());
 
 	  EmergencyCodes emergencyCodes = message.getEmergencyCodes();
 	  if(emergencyCodes != null)
 		  r.setEmergencyFlag(true);
 	  else
 		  r.setEmergencyFlag(false);
 	  
 	  tcip_3_0_5_local.CcLocationReport gpsData = message.getLocalCcLocationReport();
 	  if(gpsData != null) {
 		  NMEA nemaSentences = gpsData.getNMEA();
 		  List<String> sentenceStrings = nemaSentences.getSentence();
 		  if(sentenceStrings.size() == 2) {
 			  String RMC = sentenceStrings.get(0);
 			  String GGA = sentenceStrings.get(1);
 			  r.setRmc(RMC);
 			  r.setGga(GGA);
 		  }
 	  }
 	  
 	  _executorService.execute(new ProcessingTask(r));
   }
   
   @Override
   public void resetVehicleLocation(AgencyAndId vid) {
     _vehicleInstancesByVehicleId.remove(vid);
   }
 
   @Override
   public NycTestInferredLocationRecord getNycTestInferredLocationRecordForVehicle(AgencyAndId vid) {
     VehicleInferenceInstance instance = _vehicleInstancesByVehicleId.get(vid);
     if (instance == null)
       return null;
     NycTestInferredLocationRecord record = instance.getCurrentState();
     if(record != null)
     	record.setVehicleId(vid);
     return record;
   }
 
   @Override
   public List<NycTestInferredLocationRecord> getLatestProcessedVehicleLocationRecords() {
 
     List<NycTestInferredLocationRecord> records = new ArrayList<NycTestInferredLocationRecord>();
 
     for (Map.Entry<AgencyAndId, VehicleInferenceInstance> entry : _vehicleInstancesByVehicleId.entrySet()) {
       AgencyAndId vehicleId = entry.getKey();
       VehicleInferenceInstance instance = entry.getValue();
       if (instance != null) {
       	NycTestInferredLocationRecord record = instance.getCurrentState();
         if (record != null) {
           record.setVehicleId(vehicleId);
           records.add(record);
         }
       } else {
         _log.warn("No VehicleInferenceInstance found: vid=" + vehicleId);
       }
     }
 
     return records;
   }
 
   @Override
   public void setVehicleStatus(AgencyAndId vid, boolean enabled) {
     VehicleInferenceInstance instance = _vehicleInstancesByVehicleId.get(vid);
     if (instance == null)
       return;
     instance.setVehicleStatus(enabled);
   }
 
   @Override
   public List<Particle> getCurrentParticlesForVehicleId(AgencyAndId vehicleId) {
     VehicleInferenceInstance instance = _vehicleInstancesByVehicleId.get(vehicleId);
     if (instance == null)
       return null;
     return instance.getCurrentParticles();
   }
 
   @Override
   public List<Particle> getCurrentSampledParticlesForVehicleId(
       AgencyAndId vehicleId) {
     VehicleInferenceInstance instance = _vehicleInstancesByVehicleId.get(vehicleId);
     if (instance == null)
       return null;
     return instance.getCurrentSampledParticles();
   }
 
   @Override
   public List<JourneyPhaseSummary> getCurrentJourneySummariesForVehicleId(
       AgencyAndId vehicleId) {
     VehicleInferenceInstance instance = _vehicleInstancesByVehicleId.get(vehicleId);
     if (instance == null)
       return Collections.emptyList();
     return instance.getJourneySummaries();
   }
   
   @Override
   public VehicleLocationDetails getDetailsForVehicleId(AgencyAndId vehicleId) {
     VehicleInferenceInstance instance = _vehicleInstancesByVehicleId.get(vehicleId);
     if (instance == null)
       return null;
     VehicleLocationDetails details = instance.getDetails();
     details.setVehicleId(vehicleId);
     return details;
   }
   
   @Override
   public VehicleLocationDetails getBadDetailsForVehicleId(AgencyAndId vehicleId) {
     VehicleInferenceInstance instance = _vehicleInstancesByVehicleId.get(vehicleId);
     if (instance == null)
       return null;
     VehicleLocationDetails details = instance.getBadParticleDetails();
     details.setVehicleId(vehicleId);
     details.setParticleFilterFailureActive(true);
     return details;
   }
 
   @Override
   public VehicleLocationDetails getBadDetailsForVehicleId(AgencyAndId vehicleId,
       int particleId) {	
 	    VehicleLocationDetails details = getBadDetailsForVehicleId(vehicleId);
 	    if (details == null)
 	      return null;
 	    return findParticle(details, particleId);
   }
 
   @Override
   public VehicleLocationDetails getDetailsForVehicleId(AgencyAndId vehicleId, 
 	  int particleId) {
 	    VehicleLocationDetails details = getDetailsForVehicleId(vehicleId);
 	    if (details == null)
 	      return null;
 	    return findParticle(details, particleId);
   }
   
   /****
    * Private Methods
    ****/
   private VehicleLocationDetails findParticle(VehicleLocationDetails details, int particleId) {
 	  List<Particle> particles = details.getParticles();
 	  if (particles != null) {
 		  for (Particle p : particles) {
 			  if (p.getIndex() == particleId) {
 				  List<Particle> history = new ArrayList<Particle>();
 				  while (p != null) {
 					  history.add(p);
 					  p = p.getParent();
 				  }
 				  details.setParticles(history);
 				  details.setHistory(true);
 				  return details;
 			  }
 		  }
 	  }
 	  return null;
   }
 
   private VehicleInferenceInstance getInstanceForVehicle(AgencyAndId vehicleId) {
 
     VehicleInferenceInstance instance = _vehicleInstancesByVehicleId.get(vehicleId);
 
     if (instance == null) {
       VehicleInferenceInstance newInstance = _applicationContext.getBean(VehicleInferenceInstance.class);
       instance = _vehicleInstancesByVehicleId.putIfAbsent(vehicleId,
           newInstance);
       if (instance == null)
         instance = newInstance;
     }
 
     return instance;
   }
 
   public class ProcessingTask implements Runnable {
 
     private AgencyAndId _vehicleId;
 
     private NycRawLocationRecord _inferenceRecord;
 
     private NycTestInferredLocationRecord _nycTestInferredLocationRecord;
 
     public ProcessingTask(NycRawLocationRecord record) {
       _vehicleId = record.getVehicleId();
       _inferenceRecord = record;
     }
 
     public ProcessingTask(NycTestInferredLocationRecord record) {
       _vehicleId = record.getVehicleId();
       _nycTestInferredLocationRecord = record;
     }
 
     @Override
     public void run() {
 
       try {
         VehicleInferenceInstance existing = getInstanceForVehicle(_vehicleId);
 
         boolean passOnRecord = sendRecord(existing);
        passOnRecord = true;
 
         if (passOnRecord) {
         	NycVehicleManagementStatusBean managementRecord = existing.getCurrentManagementState();
         	managementRecord.setInferenceEngineIsPrimary(_outputQueueSenderService.getIsPrimaryInferenceInstance());
 
         	// don't let network IO problems stop inference completely.
 //        	try {
 //        	  managementRecord.setDepotId(_vehicleAssignmentService.getAssignedDepotForVehicle(_vehicleId));
 //        	} catch(Exception e) {
 //        	  managementRecord.setDepotId(null);
 //        	}
             
         	NycQueuedInferredLocationBean record = existing.getCurrentStateAsNycQueuedInferredLocationBean();
         	record.setVehicleId(_vehicleId.toString());
           record.setManagementRecord(managementRecord);
         	
         	_outputQueueSenderService.enqueue(record);
         }
       } catch (ProjectionException e) {
     	  // discard
       } catch (Throwable ex) {
         _log.error("Error processing new location record for inference: ", ex);
       }
     }
 
     private boolean sendRecord(VehicleInferenceInstance existing) {
       if (_inferenceRecord != null) {
         return existing.handleUpdate(_inferenceRecord);
       } else if (_nycTestInferredLocationRecord != null) {
         return existing.handleBypassUpdate(_nycTestInferredLocationRecord);
       }
       return false;
     }
   }
 }
