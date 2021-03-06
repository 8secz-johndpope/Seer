 package org.ovirt.engine.core.common.businessentities.gluster;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.LinkedHashMap;
 import java.util.LinkedHashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
 import java.util.Set;
 
 import javax.validation.Valid;
 import javax.validation.constraints.NotNull;
 
 import org.ovirt.engine.core.common.businessentities.BusinessEntity;
 import org.ovirt.engine.core.common.businessentities.IVdcQueryable;
 import org.ovirt.engine.core.common.constants.gluster.GlusterConstants;
 import org.ovirt.engine.core.common.utils.gluster.GlusterCoreUtil;
 import org.ovirt.engine.core.common.validation.group.CreateEntity;
 import org.ovirt.engine.core.common.validation.group.RemoveEntity;
 import org.ovirt.engine.core.common.validation.group.gluster.CreateReplicatedVolume;
 import org.ovirt.engine.core.common.validation.group.gluster.CreateStripedVolume;
 import org.ovirt.engine.core.compat.Guid;
 import org.ovirt.engine.core.compat.StringHelper;
 
 /**
  * The gluster volume entity. This is a logical partition within the virtual storage space provided by a Gluster
  * Cluster. It is made up of multiple bricks (server:brickDirectory) across multiple servers of the cluster.
  *
  * @see GlusterVolumeType
  * @see TransportType
  * @see GlusterVolumeStatus
  * @see GlusterBrickEntity
  * @see GlusterBrickStatus
  * @see GlusterVolumeOptionEntity
  * @see AccessProtocol
  */
 public class GlusterVolumeEntity extends IVdcQueryable implements BusinessEntity<Guid> {
     private static final long serialVersionUID = 2355384696827317277L;
 
     @NotNull(message = "VALIDATION.GLUSTER.VOLUME.ID.NOT_NULL", groups = { RemoveEntity.class })
     private Guid id;
 
     @NotNull(message = "VALIDATION.GLUSTER.VOLUME.CLUSTER_ID.NOT_NULL", groups = {CreateEntity.class, CreateReplicatedVolume.class, CreateStripedVolume.class})
     private Guid clusterId;
 
     @NotNull(message = "VALIDATION.GLUSTER.VOLUME.NAME.NOT_NULL", groups = {CreateEntity.class, CreateReplicatedVolume.class, CreateStripedVolume.class})
     private String name;
 
     @NotNull(message = "VALIDATION.GLUSTER.VOLUME.TYPE.NOT_NULL", groups = {CreateEntity.class, CreateReplicatedVolume.class, CreateStripedVolume.class})
     private GlusterVolumeType volumeType = GlusterVolumeType.DISTRIBUTE;
 
     @NotNull(message = "VALIDATION.GLUSTER.VOLUME.REPLICA_COUNT.NOT_NULL", groups = { CreateReplicatedVolume.class })
     private Integer replicaCount;
 
     @NotNull(message = "VALIDATION.GLUSTER.VOLUME.STRIPE_COUNT.NOT_NULL", groups = { CreateStripedVolume.class })
     private Integer stripeCount;
 
     @Valid
     private Map<String, GlusterVolumeOptionEntity> options = new LinkedHashMap<String, GlusterVolumeOptionEntity>();
 
     @NotNull(message = "VALIDATION.GLUSTER.VOLUME.BRICKS.NOT_NULL", groups = {CreateEntity.class, CreateReplicatedVolume.class, CreateStripedVolume.class})
     @Valid
     private List<GlusterBrickEntity> bricks = new ArrayList<GlusterBrickEntity>();
 
     private GlusterVolumeStatus status = GlusterVolumeStatus.DOWN;
 
     // Gluster and NFS are enabled by default
     private Set<AccessProtocol> accessProtocols = new LinkedHashSet<AccessProtocol>(Arrays.asList(new AccessProtocol[] {
             AccessProtocol.GLUSTER, AccessProtocol.NFS }));
 
     // Transport type TCP is enabled by default
     private Set<TransportType> transportTypes = new LinkedHashSet<TransportType>(Arrays.asList(new TransportType[] {
             TransportType.TCP }));
 
     public GlusterVolumeEntity() {
     }
 
     @Override
     public Guid getId() {
         return id;
     }
 
     @Override
     public void setId(Guid id) {
         this.id = id;
     }
 
     public Guid getClusterId() {
         return clusterId;
     }
 
     public void setClusterId(Guid clusterId) {
         this.clusterId = clusterId;
     }
 
     public String getName() {
         return name;
     }
 
     public void setName(String name) {
         this.name = name;
     }
 
     public GlusterVolumeType getVolumeType() {
         return volumeType;
     }
 
     public void setVolumeType(GlusterVolumeType volumeType) {
         this.volumeType = volumeType;
 
         switch (volumeType) {
         case DISTRIBUTE:
             setReplicaCount(0);
             setStripeCount(0);
             break;
         case REPLICATE:
         case DISTRIBUTED_REPLICATE:
             setStripeCount(0);
             break;
         case STRIPE:
         case DISTRIBUTED_STRIPE:
             setReplicaCount(0);
             break;
         }
     }
 
     public void setVolumeType(String volumeType) {
         setVolumeType(GlusterVolumeType.valueOf(volumeType));
     }
 
     public GlusterVolumeStatus getStatus() {
         return status;
     }
 
     public void setStatus(GlusterVolumeStatus status) {
         this.status = status;
     }
 
     public boolean isOnline() {
         return this.status == GlusterVolumeStatus.UP;
     }
 
     public Integer getReplicaCount() {
         return replicaCount;
     }
 
     public void setReplicaCount(Integer replicaCount) {
         this.replicaCount = replicaCount;
     }
 
     public Integer getStripeCount() {
         return stripeCount;
     }
 
     public void setStripeCount(Integer stripeCount) {
         this.stripeCount = stripeCount;
     }
 
     public Set<AccessProtocol> getAccessProtocols() {
         return accessProtocols;
     }
 
     public void setAccessProtocols(Set<AccessProtocol> accessProtocols) {
         this.accessProtocols = accessProtocols;
     }
 
     public void addAccessProtocol(AccessProtocol protocol) {
         accessProtocols.add(protocol);
     }
 
     public void removeAccessProtocol(AccessProtocol protocol) {
         accessProtocols.remove(protocol);
     }
 
     public Set<TransportType> getTransportTypes() {
         return transportTypes;
     }
 
     public void setTransportTypes(Set<TransportType> transportTypes) {
         this.transportTypes = transportTypes;
     }
 
     public void addTransportType(TransportType transportType) {
         transportTypes.add(transportType);
     }
 
     public void removeTransportType(TransportType transportType) {
         transportTypes.remove(transportType);
     }
 
     public String getAccessControlList() {
         return getOptionValue(GlusterConstants.OPTION_AUTH_ALLOW);
     }
 
     public void setAccessControlList(String accessControlList) {
         if (!StringHelper.isNullOrEmpty(accessControlList)) {
             setOption(GlusterConstants.OPTION_AUTH_ALLOW, accessControlList);
         }
     }
 
     public Collection<GlusterVolumeOptionEntity> getOptions() {
         return options.values();
     }
 
     public GlusterVolumeOptionEntity getOption(String optionKey) {
         return options.get(optionKey);
     }
 
     /**
      * Returns value of given option key as set on the volume. <br>
      * In case the option is not set, <code>null</code> will be returned.
      */
     public String getOptionValue(String optionKey) {
         GlusterVolumeOptionEntity option = options.get(optionKey);
         if (option == null) {
             return null;
         }
         return option.getValue();
     }
 
     public void setOption(GlusterVolumeOptionEntity option) {
         options.put(option.getKey(), option);
     }
 
     public void setOption(String key, String value) {
         if (options.containsKey(key)) {
             options.get(key).setValue(value);
         } else {
             options.put(key, new GlusterVolumeOptionEntity(id, key, value));
         }
     }
 
     /**
      * Sets options from a comma separated list of key value pairs separated by = <br>
      * e.g. key=val1,key2=val2,...,keyn=valn
      *
      * @param options
      */
     public void setOptions(String options) {
         this.options.clear();
         if (options == null || options.trim().isEmpty()) {
             return;
         }
 
         String[] optionArr = options.split(",", -1);
         for (String option : optionArr) {
             String[] optionInfo = option.split("=", -1);
             if (optionInfo.length == 2) {
                 setOption(optionInfo[0], optionInfo[1]);
             }
         }
     }
 
     public void setOptions(Collection<GlusterVolumeOptionEntity> options) {
         this.options.clear();
         for (GlusterVolumeOptionEntity option : options) {
             setOption(option);
         }
     }
 
     public void setOptions(Map<String, String> options) {
         this.options.clear();
         for (Entry<String, String> entry : options.entrySet()) {
             setOption(entry.getKey(), entry.getValue());
         }
     }
 
     public void removeOption(String optionKey) {
         options.remove(optionKey);
     }
 
     public void addBrick(GlusterBrickEntity GlusterBrick) {
         bricks.add(GlusterBrick);
     }
 
     public void addBricks(Collection<GlusterBrickEntity> bricks) {
         this.bricks.addAll(bricks);
     }
 
     public void setBricks(List<GlusterBrickEntity> bricks) {
         this.bricks = bricks;
     }
 
     public void removeBrick(GlusterBrickEntity GlusterBrick) {
         bricks.remove(GlusterBrick);
     }
 
     /**
      * Replaces an existing brick in the volume with the given new brick. The new brick will have same index as the
      * existing one.
      *
      * @param existingBrick
      * @param newBrick
      * @return Index of the brick that was replaced. Returns -1 if the {@code existingBrick} is not found in the volume, leaving the volume unchanged.
      */
     public int replaceBrick(GlusterBrickEntity existingBrick, GlusterBrickEntity newBrick) {
         int index = bricks.indexOf(existingBrick);
         if (index != -1) {
             GlusterBrickEntity brick = bricks.get(index);
             brick.copyFrom(newBrick);
         }
         return index;
     }
 
     public List<GlusterBrickEntity> getBricks() {
         return bricks;
     }
 
     public void enableNFS() {
         accessProtocols.add(AccessProtocol.NFS);
         setOption(GlusterConstants.OPTION_NFS_DISABLE, GlusterConstants.OFF);
     }
 
     public void disableNFS() {
         accessProtocols.remove(AccessProtocol.NFS);
         setOption(GlusterConstants.OPTION_NFS_DISABLE, GlusterConstants.ON);
     }
 
     public boolean isNfsEnabled() {
         String nfsDisabled = getOptionValue(GlusterConstants.OPTION_NFS_DISABLE);
         return (nfsDisabled == null || nfsDisabled.equalsIgnoreCase(GlusterConstants.OFF));
     }
 
     public void enableCifs() {
         accessProtocols.add(AccessProtocol.CIFS);
     }
 
     public void disableCifs() {
         accessProtocols.remove(AccessProtocol.CIFS);
     }
 
     public boolean isCifsEnabled() {
         return accessProtocols.contains(AccessProtocol.CIFS);
     }
 
     public List<String> getBrickDirectories() {
         List<String> brickDirectories = new ArrayList<String>();
         for (GlusterBrickEntity brick : getBricks()) {
             brickDirectories.add(brick.getQualifiedName());
         }
         return brickDirectories;
     }
 
     @Override
     public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result + ((name == null) ? 0 : name.hashCode());
         result = prime * result + ((clusterId == null) ? 0 : clusterId.hashCode());
         result = prime * result + ((volumeType == null) ? 0 : volumeType.hashCode());
         result = prime * result + ((transportTypes == null) ? 0 : transportTypes.hashCode());
         result = prime * result + ((status == null) ? 0 : status.hashCode());
        result = prime * result + ((replicaCount == null) ? 0 : replicaCount.hashCode());
        result = prime * result + ((stripeCount == null) ? 0 : stripeCount.hashCode());
         result = prime * result + ((options == null) ? 0 : options.hashCode());
         result = prime * result + ((accessProtocols == null) ? 0 : accessProtocols.hashCode());
         result = prime * result + ((bricks == null) ? 0 : bricks.hashCode());
         return result;
     }
 
     @Override
     public boolean equals(Object obj) {
         if (!(obj instanceof GlusterVolumeEntity)) {
             return false;
         }
 
         GlusterVolumeEntity volume = (GlusterVolumeEntity) obj;
 
         if (!(clusterId.equals(volume.getClusterId()))) {
             return false;
         }
 
         if (!(name.equals(volume.getName())
                 && volumeType == volume.getVolumeType()
                 && status == volume.getStatus()
                && equalIntegers(replicaCount, volume.getReplicaCount())
                && equalIntegers(stripeCount, volume.getStripeCount()))) {
             return false;
         }
 
         if (!GlusterCoreUtil.listsEqual(getOptions(), volume.getOptions())) {
             return false;
         }
 
         if (!GlusterCoreUtil.listsEqual(accessProtocols, volume.getAccessProtocols())) {
             return false;
         }
 
         if (!GlusterCoreUtil.listsEqual(transportTypes, volume.getTransportTypes())) {
             return false;
         }
 
         if (!GlusterCoreUtil.listsEqual(bricks, volume.getBricks())) {
             return false;
         }
 
         return true;
     }
 
    private boolean equalIntegers(Integer int1, Integer int2) {
        if(int1 == null) {
            return int1 == int2;
        } else {
            return int1.equals(int2);
        }
    }

     @Override
     public Object getQueryableId() {
         return getId();
     }
 }
