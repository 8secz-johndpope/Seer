 package org.jvnet.hudson.plugins.port_allocator;
 
 import hudson.Launcher;
 import hudson.model.Build;
 import hudson.model.BuildListener;
 import hudson.model.Computer;
 import hudson.model.Descriptor;
 import hudson.model.Executor;
 import hudson.tasks.BuildWrapper;
 import org.kohsuke.stapler.StaplerRequest;
 
 import java.io.IOException;
 import java.io.PrintStream;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Map;
 import java.util.Set;
 import java.util.StringTokenizer;
 
 import net.sf.json.JSONObject;
 
 /**
  * Allocates TCP Ports on a Computer for consumption and sets it as
  * envioronet variables, see configuration
  *
  * <p>
  * This just mediates between different Jobs running on the same Computer
  * by assigning free ports and its the jobs responsibility to open and close the ports.   
  *
  * <p>
  * TODO: implement ResourceActivity so that the queue performs intelligent job allocations
  * based on the port availability, instead of start executing something then block.
  *
  * @author Rama Pulavarthi
  */
 public class PortAllocator extends BuildWrapper /* implements ResourceActivity */
 {
     /**
      * ','-separated list of tokens that designates either port variable names or port numbers.
      */
     public final String portVariables;
 
     private PortAllocator(String portVariables){
        // to avoid platform difference issue in case sensitivity of environment variables,
        // always use uppser case.
        this.portVariables = portVariables.toUpperCase();
     }
 
     private Set<String> getVariables() {
         StringTokenizer stk = new StringTokenizer(portVariables,",");
         final Set<String> portVars = new HashSet<String>();
         while(stk.hasMoreTokens()) {
             portVars.add(stk.nextToken().trim());
         }
         return portVars;
     }
 
     public Environment setUp(Build build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
         PrintStream logger = listener.getLogger();
 
         final Set<String> portVars = getVariables();
         final Computer cur = Executor.currentExecutor().getOwner();
         Map<String,Integer> prefPortMap = new HashMap<String,Integer>();
         if (build.getPreviousBuild() != null) {
             AllocatedPortAction prevAlloc = build.getPreviousBuild().getAction(AllocatedPortAction.class);
             if (prevAlloc != null) {
                 // try to assign ports assigned in previous build
                 prefPortMap = prevAlloc.getPreviousAllocatedPorts();
             }
         }
         final PortAllocationManager pam = PortAllocationManager.getManager(cur);
         final Map<String, Integer> portMap = new HashMap<String, Integer>();
         for (String portVar : portVars) {
             Integer port;
             try {
                 //check if the users prefers port number
                 port = Integer.parseInt(portVar);
                 logger.println("Allocating TCP port "+port);
                 pam.allocate(build,port);
             } catch (NumberFormatException ex) {
                 int prefPort = prefPortMap.get(portVar)== null?0:prefPortMap.get(portVar);
                 port = pam.allocateRandom(build,prefPort);
                 logger.println("Allocating TCP port "+portVar+"="+port);
             }
             portMap.put(portVar, port);
         }
         // TODO: only log messages when we are blocking.
         logger.println("TCP port allocation complete");
         build.addAction(new AllocatedPortAction(portMap));
 
         return new Environment() {
 
             @Override
             public void buildEnvVars(Map<String, String> env) {
                 for (String portVar : portMap.keySet()) {
                     try {
                         //check if port variable is a port number, if so don't set env 
                         Integer.parseInt(portVar);
                     } catch (NumberFormatException ex) {
                         env.put(portVar, portMap.get(portVar).toString());
                     }
                 }
             }
 
             public boolean tearDown(Build build, BuildListener listener) throws IOException, InterruptedException {
                 for (Integer v : portMap.values())
                     pam.free(v);
                 return true;
             }
         };
     }
 
     public Descriptor<BuildWrapper> getDescriptor() {
         return DESCRIPTOR;
     }
 
 
     public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
 
     public static final class DescriptorImpl extends Descriptor<BuildWrapper> {
         DescriptorImpl() {
             super(PortAllocator.class);
             load();
         }
 
         public String getDisplayName() {
             return "Run Port Allocator during build";
         }
 
         public String getHelpFile() {
             return "/plugin/port-allocator/help.html";
         }
 
         @Override
         public BuildWrapper newInstance(StaplerRequest req, JSONObject formData) throws FormException {
             return new PortAllocator(req.getParameter("portallocator.portVariables"));
         }
     }
 
 }
