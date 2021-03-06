 package org.rhq.plugins.jslee;
 
 import java.util.ArrayList;
 import java.util.Set;
 
 import javax.management.MBeanServerConnection;
 import javax.management.MBeanServerInvocationHandler;
 import javax.management.ObjectName;
 import javax.slee.ServiceID;
 import javax.slee.management.ServiceManagementMBean;
 import javax.slee.management.ServiceState;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.mobicents.slee.container.management.jmx.SbbEntitiesMBeanImplMBean;
 import org.mobicents.slee.runtime.sbbentity.SbbEntity;
 import org.rhq.core.domain.configuration.Configuration;
 import org.rhq.core.domain.configuration.PropertyList;
 import org.rhq.core.domain.configuration.PropertyMap;
 import org.rhq.core.domain.configuration.PropertySimple;
 import org.rhq.core.domain.measurement.AvailabilityType;
 import org.rhq.core.domain.measurement.MeasurementDataNumeric;
 import org.rhq.core.domain.measurement.MeasurementDataTrait;
 import org.rhq.core.domain.measurement.MeasurementReport;
 import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
 import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
 import org.rhq.core.pluginapi.inventory.ResourceComponent;
 import org.rhq.core.pluginapi.inventory.ResourceContext;
 import org.rhq.core.pluginapi.measurement.MeasurementFacet;
 import org.rhq.core.pluginapi.operation.OperationFacet;
 import org.rhq.core.pluginapi.operation.OperationResult;
 import org.rhq.plugins.jslee.utils.MBeanServerUtils;
 
 public class ServiceComponent implements ResourceComponent<JainSleeServerComponent>, MeasurementFacet, OperationFacet {
   private final Log log = LogFactory.getLog(this.getClass());
 
   private ResourceContext<JainSleeServerComponent> resourceContext;
   private ServiceID serviceId = null;
   private MBeanServerUtils mbeanUtils = null;
 
   private ObjectName servicemanagement;
   private ServiceState serviceState = ServiceState.INACTIVE;
 
   public void start(ResourceContext<JainSleeServerComponent> context) throws InvalidPluginConfigurationException, Exception {
     log.info("start");
 
     this.resourceContext = context;
     this.servicemanagement = new ObjectName(ServiceManagementMBean.OBJECT_NAME);
 
     this.mbeanUtils = context.getParentResourceComponent().getMBeanServerUtils();
 
     String name = this.resourceContext.getPluginConfiguration().getSimple("name").getStringValue();
     String version = this.resourceContext.getPluginConfiguration().getSimple("version").getStringValue();
     String vendor = this.resourceContext.getPluginConfiguration().getSimple("vendor").getStringValue();
 
     serviceId = new ServiceID(name, vendor, version);
   }
 
   public void stop() {
     log.info("stop");
   }
 
   public AvailabilityType getAvailability() {
     log.info("getAvailability");
     try {
       MBeanServerConnection connection = this.mbeanUtils.getConnection();
       serviceState = (ServiceState) connection.invoke(this.servicemanagement, "getState",
           new Object[] { this.serviceId }, new String[] { ServiceID.class.getName() });
     } catch (Exception e) {
       log.error("getAvailability failed for ServiceID = " + this.serviceId);
       this.serviceState = ServiceState.INACTIVE;
       return AvailabilityType.DOWN;
     }
 
     return AvailabilityType.UP;
   }
 
   public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
     log.info("getValues");
     for (MeasurementScheduleRequest request : metrics) {
       if (request.getName().equals("state")) {
         report.addData(new MeasurementDataTrait(request, this.serviceState.toString()));
       }
       else if (request.getName().equals("SbbEntitiesCount")) {
         try {
           report.addData(new MeasurementDataNumeric(request, Double.valueOf(getServiceSbbEntities().size())));
         }
         catch (Exception e) {
           log.error("getAvailability failed for Service = " + this.serviceId);
           report.addData(new MeasurementDataNumeric(request, -1.0));
         }
       }
     }
 
   }
 
   public OperationResult invokeOperation(String name, Configuration parameters) throws InterruptedException, Exception {
     log.info("ServiceComponent.invokeOperation() with name = " + name);
 
     if ("changeServiceState".equals(name)) {
       return doChangeServiceState(parameters);
     }
     else if ("retrieveSbbEntities".equals(name)) {
       return doRetrieveSbbEntities();
     }
     else {
       throw new UnsupportedOperationException("Operation [" + name + "] is not supported.");
     }
   }
 
   private OperationResult doChangeServiceState(Configuration parameters) throws Exception {
     String message = null;
     String action = parameters.getSimple("action").getStringValue();
     MBeanServerConnection connection = this.mbeanUtils.getConnection();
     ServiceManagementMBean serviceManagementMBean = (ServiceManagementMBean) MBeanServerInvocationHandler.newProxyInstance(
         connection, this.servicemanagement, javax.slee.management.ServiceManagementMBean.class, false);
     if ("activate".equals(action)) {
       serviceManagementMBean.activate(this.serviceId);
       message = "Successfully Activated Service " + this.serviceId;
       this.serviceState = ServiceState.ACTIVE;
     }
     else if ("deactivate".equals(action)) {
       serviceManagementMBean.deactivate(this.serviceId);
       message = "Successfully Deactivated Service " + this.serviceId;
       this.serviceState = ServiceState.INACTIVE;
     }
 
     OperationResult result = new OperationResult();
     result.getComplexResults().put(new PropertySimple("result", message));
     return result;
   }
 
   private OperationResult doRetrieveSbbEntities() throws Exception {
 
     // The pretty table we are building as result
     PropertyList columnList = new PropertyList("result");
 
    for(SbbEntity sbbEntity : getServiceSbbEntities()) {
      if(sbbEntity.getServiceId().equals(this.serviceId)) {
        PropertyMap col = new PropertyMap("element");
 
        col.put(new PropertySimple("SBB Entity Id", sbbEntity.getSbbEntityId()));
        col.put(new PropertySimple("Parent SBB Entity Id", sbbEntity.getParentSbbEntityId()));
        col.put(new PropertySimple("Priority", sbbEntity.getPriority()));
        col.put(new PropertySimple("Attachment Count", sbbEntity.getAttachmentCount()));
 
        columnList.add(col);
      }
     }
 
     OperationResult result = new OperationResult();
     result.getComplexResults().put(columnList);
 
     return result;
   }
 
   /**
    * Obtain all SBB Entities and filter the ones which service id is the same as this.
    * 
    * @return an ArrayList of SbbEntity
    * @throws Exception
    */
  private ArrayList<SbbEntity> getServiceSbbEntities() throws Exception {
     MBeanServerConnection connection = this.mbeanUtils.getConnection();
     ObjectName sbbEntitiesMBeanObj = new ObjectName("org.mobicents.slee:name=SbbEntitiesMBean");
     SbbEntitiesMBeanImplMBean sbbEntititesMBean = (SbbEntitiesMBeanImplMBean) MBeanServerInvocationHandler.newProxyInstance(
         connection, sbbEntitiesMBeanObj, SbbEntitiesMBeanImplMBean.class, false);
     Object[] objs = sbbEntititesMBean.retrieveAllSbbEntities();
 
    ArrayList<SbbEntity> list = new ArrayList<SbbEntity>();
     for(Object obj : objs) {
      SbbEntity sbbEntity = (SbbEntity)obj; 
      if(sbbEntity.getServiceId().equals(this.serviceId)) {
         list.add(sbbEntity);
       }
     }
 
     return list;
   }
 }
