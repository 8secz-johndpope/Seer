 package org.openlmis.core.dto;
 
 import lombok.Data;
 import lombok.EqualsAndHashCode;
 import lombok.NoArgsConstructor;
 import org.codehaus.jackson.map.annotate.JsonSerialize;
 import org.openlmis.core.domain.Facility;
 import org.openlmis.core.domain.Vendor;
 
 import java.util.Date;
 
 @Data
 @NoArgsConstructor
 @EqualsAndHashCode(callSuper = false)
 public class FacilityFeedDTO extends BaseFeedDTO {
 
   private Long id;
 
   private String code;
 
   private String name;
 
   private Long typeId;
 
   private String description;
 
   private String GLN;
 
   private String mainPhone;
 
   private String fax;
 
   private String address1;
 
   private String address2;
 
   private Long geographicZoneID;
 
   private Long catchmentPopulation;
 
   private Double latitude;
 
   private Double longitude;
 
   private Double altitude;
 
   private String operatedBy;
 
   private Double coldStorageGrossCapacity;
 
   private Double coldStorageNetCapacity;
 
   private boolean suppliesOthers;
 
   private boolean isSDP;
 
   private boolean hasElectricity;
 
   private boolean isOnline;
 
   private boolean hasElectronicSCC;
 
   private boolean hasElectronicDAR;
 
   private boolean active;
 
   private Date goLiveDate;
 
   private Date goDownDate;
 
   private boolean satelliteFacility;
 
   private Integer satelliteParentID;
 
   private String comments;
 
   private boolean doNotDisplay;
 
   private Date modifiedDate;
 
   public FacilityFeedDTO(Facility facility) {
     this.id = facility.getId();
     this.code = facility.getCode();
     this.name = facility.getName();
     this.typeId = facility.getFacilityType().getId();
     this.description = facility.getDescription();
     this.GLN = facility.getGln();
     this.mainPhone = facility.getMainPhone();
     this.fax = facility.getFax();
     this.address1 = facility.getAddress1();
     this.address2 = facility.getAddress2();
     this.geographicZoneID = facility.getGeographicZone().getId();
     this.catchmentPopulation = facility.getCatchmentPopulation();
     this.latitude = facility.getLatitude();
     this.longitude = facility.getLongitude();
     this.altitude = facility.getAltitude();
    this.operatedBy = (facility.getOperatedBy() != null) ? facility.getOperatedBy().getText() : "";
     this.coldStorageGrossCapacity = facility.getColdStorageGrossCapacity();
     this.coldStorageNetCapacity = facility.getColdStorageNetCapacity();
     this.suppliesOthers = (facility.getSuppliesOthers() != null)?facility.getSuppliesOthers():false;
     this.isSDP = facility.getSdp();
     this.hasElectricity = (facility.getHasElectricity() != null)?facility.getHasElectricity():false;
     this.isOnline = (facility.getOnline() != null)?facility.getOnline():false;
     this.hasElectronicSCC = (facility.getHasElectronicScc() != null)?facility.getHasElectronicScc():false;
     this.hasElectronicDAR = (facility.getHasElectronicDar() != null)?facility.getHasElectronicDar():false;
     this.active = facility.getActive();
     this.goLiveDate = facility.getGoLiveDate();
     this.goDownDate = facility.getGoDownDate();
     this.satelliteFacility = (facility.getSatellite() != null)?facility.getSatellite():false;
     this.satelliteParentID = facility.getSatelliteParentId();
     this.comments = facility.getComment();
     this.doNotDisplay = false; // TODO : fill this
     this.modifiedDate = facility.getModifiedDate();
   }
 
   public String getVendorSystem() {
     return null;  //To change body of created methods use File | Settings | File Templates.
   }
 }
