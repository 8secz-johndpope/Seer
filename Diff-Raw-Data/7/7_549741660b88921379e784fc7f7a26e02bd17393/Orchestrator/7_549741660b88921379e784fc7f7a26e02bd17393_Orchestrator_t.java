 package com.vmware.vhadoop.model;
 
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.concurrent.ExecutionException;
 import java.util.concurrent.Future;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.TimeoutException;
 
 
 /**
  * Top level container for various other entities, such as hosts, vms, resource pools, et al.
  * @author ghicken
  *
  */
 public class Orchestrator extends ResourceContainer
 {
    /** Nominal CPU speed is 2GHz */
    long cpuSpeed = 2000;
 
    /** List of containers with configuration updates since last cleared */
    Set<ResourceContainer> configUpdated;
 
    /** List of containers with usage updates since last cleared */
    Set<Usage> usageUpdated;
 
    /** List of VMs updated since this list was last accessed. Convenience */
    Set<VM> updatedVMs;
 
    public Orchestrator(String id) {
       super(id);
       configUpdated = new HashSet<ResourceContainer>();
       usageUpdated = new HashSet<Usage>();
       updatedVMs = new HashSet<VM>();
    }
 
    AllocationPolicy allocationPolicy;
 
    /**
     * This returns the allocation policy used to portion out scarce resources.
     * This accessor is provided so that subtrees of containers can use it when they determine that there's
     * a need rather than processing the entire hierarchy every time.
     * @return
     */
    public AllocationPolicy getAllocationPolicy() {
       return allocationPolicy;
    }
 
    /**
     * This causes a global re-evaluation of resource usage patterns against limits.
     */
    public synchronized void configurationUpdated(ResourceContainer container) {
       configUpdated.add(container);
 
       if (container instanceof VM) {
          updatedVMs.add((VM)container);
          synchronized (updatedVMs) {
             updatedVMs.notifyAll();
          }
       }
 
       /* notify both usage and config waiters */
       /* TODO: move this to AFTER any config change has completed */
       synchronized (configUpdated) {
          configUpdated.notifyAll();
       }
       synchronized (usageUpdated) {
          usageUpdated.notifyAll();
       }
    }
 
    /**
     * This causes a global re-evaluation of resource usage patterns against limits.
     */
    public synchronized void usageUpdated(Usage usage) {
       usageUpdated.add(usage);
       /* TODO: move this to AFTER any usage update has completed */
       synchronized (usageUpdated) {
          usageUpdated.notifyAll();
       }
    }
 
    private Map<String, Future<Boolean>> setPower(Set<String> ids, boolean power) {
       Map<String, Future<Boolean>> map = new HashMap<String, Future<Boolean>>();
 
       for (String id : ids) {
          VM vm = (VM)get(id);
          /* TODO: alter this if we ever model power on to take time */
          final boolean result = power ? vm.powerOn() : vm.powerOff();
          map.put(id, new Future<Boolean>() {
             @Override
             public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
             }
 
             @Override
             public boolean isCancelled() {
                return false;
             }
 
             @Override
             public boolean isDone() {
                return true;
             }
 
             @Override
             public Boolean get() throws InterruptedException, ExecutionException {
                return result;
             }
 
             @Override
             public Boolean get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return result;
             }});
       }
 
       return map;
    }
 
    public Map<String, Future<Boolean>> powerOnVMs(Set<String> ids) {
       return setPower(ids, true);
    }
 
    public Map<String, Future<Boolean>> powerOffVMs(Set<String> ids) {
       return setPower(ids, false);
    }
 
    /**
     * This method returns updated VMs since the method was last called.
     * This uses the concurrent modification behaviour of iterators to
     * @return
     */
    public List<VM> getUpdatedVMs() {
      List<VM> vms = null;
       synchronized (updatedVMs) {
          try {
             while (updatedVMs.isEmpty()) {
                   updatedVMs.wait();
             }
            vms = new LinkedList<VM>(updatedVMs);
            updatedVMs.clear();
          } catch (InterruptedException e) {}
       }
 
       return vms;
    }
 
    /**
     * This allows a caller to efficiently wait for the state of the world to change.
     * This will wake waiters when there is configuration change, but not on usage
     * updates.
     */
    public void waitForConfigurationUpdate(long timeout) {
       /* TODO: add in an update counter so that callers can qualify the observed state they were
        * basing their decision to wait upon. Return immediately if there have been subsequent
        * updates
        */
       if (timeout > 0) {
          synchronized (configUpdated) {
             try {
                configUpdated.wait(timeout);
             } catch (InterruptedException e) {}
          }
       }
    }
 
    /**
     * This allows a caller to efficiently wait for the state of the world to change.
     * This will wake waiters when for both configuration and usage updated
     */
    public void waitForUsageUpdate(long timeout) {
       /* TODO: add in an update counter so that callers can qualify the observed state they were
        * basing their decision to wait upon. Return immediately if there have been subsequent
        * updates
        */
       if (timeout > 0) {
          synchronized (usageUpdated) {
             try {
                usageUpdated.wait(timeout);
             } catch (InterruptedException e) {}
          }
       }
    }
 
 
    /**
     * Sets the nominal speed of CPUs in this orchestration. Limits are specified in Mhz, so
     * this is a mechanism of converting from Mhz allocation to vCPU count.
     * @param Mhz
     */
    public void setCpuSpeed(long Mhz) {
       this.cpuSpeed = Mhz;
    }
 
    /**
     * Gets the nominal speed of CPUs in this orchestration. Limits are specified in Mhz, so
     * this is a mechanism of converting from Mhz allocation to vCPU count.
     * @return Mhz
     */
    public long getCpuSpeed() {
       return this.cpuSpeed;
 
    }
 
    /**
     * This returns an array of metric values in the order determined by the vcStatsList in VCStatsProducer
     * @param id the id of the VM to provide metrics for
     * @return the metric values
     */
    public long[] getMetrics(String id) {
       VM vm = (VM)get(id);
       return vm.getMetrics();
    }
 }
