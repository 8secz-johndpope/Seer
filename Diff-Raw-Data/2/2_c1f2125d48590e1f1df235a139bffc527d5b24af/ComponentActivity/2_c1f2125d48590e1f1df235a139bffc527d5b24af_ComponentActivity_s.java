 package org.objectweb.proactive.core.component.body;
 
 import org.apache.log4j.Logger;
 
 import org.objectweb.fractal.api.NoSuchInterfaceException;
 import org.objectweb.fractal.api.control.LifeCycleController;
 import org.objectweb.fractal.util.Fractal;
 
 import org.objectweb.proactive.Active;
 import org.objectweb.proactive.Body;
 import org.objectweb.proactive.EndActive;
 import org.objectweb.proactive.InitActive;
 import org.objectweb.proactive.RunActive;
 import org.objectweb.proactive.Service;
 import org.objectweb.proactive.core.body.ActiveBody;
 import org.objectweb.proactive.core.util.log.Loggers;
 import org.objectweb.proactive.core.util.log.ProActiveLogger;
 
 import java.io.Serializable;
 
 
 /**
  * @author Matthieu Morel
  *
  */
 public class ComponentActivity implements RunActive, InitActive, EndActive {
     private static Logger logger = ProActiveLogger.getLogger(Loggers.COMPONENTS_ACTIVITY);
     private transient InitActive componentInitActive; // used only once
     private RunActive componentRunActive;
     private EndActive componentEndActive;
     private ActiveBody activeBody;
     private InitActive functionalInitActive;
     private RunActive functionalRunActive;
     private EndActive functionalEndActive;
 
     public ComponentActivity() {
         // default component activity
         componentInitActive = null;
         componentRunActive = this;
         componentEndActive = null;
 
         functionalInitActive = new DefaultInitActive();
         functionalRunActive = new ComponentFIFORunActive();
         functionalEndActive = new DefaultEndActive();
     }
 
     public ComponentActivity(Active activity, Object reifiedObject) {
         // ComponentInitActive
         if ((activity != null) && activity instanceof ComponentInitActive) {
             componentInitActive = new ComponentInitActiveWrapper((ComponentInitActive) activity);
         } else if (reifiedObject instanceof ComponentInitActive) {
             componentInitActive = new ComponentInitActiveWrapper((ComponentInitActive) reifiedObject);
         } else {
             componentInitActive = null;
         }
 
         // ComponentRunActive
         if ((activity != null) && activity instanceof ComponentRunActive) {
             componentRunActive = new ComponentRunActiveWrapper((ComponentRunActive) activity);
         } else if (reifiedObject instanceof ComponentRunActive) {
             componentRunActive = new ComponentRunActiveWrapper((ComponentRunActive) reifiedObject);
         } else {
             componentRunActive = this;
         }
 
         // ComponentEndActive
         if ((activity != null) && activity instanceof ComponentEndActive) {
             componentEndActive = new ComponentEndActiveWrapper((ComponentEndActive) activity);
        } else if (reifiedObject instanceof EndActive) {
             componentEndActive = new ComponentEndActiveWrapper((ComponentEndActive) reifiedObject);
         } else {
             componentEndActive = null;
         }
 
         if ((activity != null) && activity instanceof InitActive) {
             functionalInitActive = (InitActive) activity;
         } else if (reifiedObject instanceof InitActive) {
             functionalInitActive = (InitActive) reifiedObject;
         } else {
             functionalInitActive = new DefaultInitActive();
         }
 
         // RunActive
         if ((activity != null) && activity instanceof RunActive) {
             functionalRunActive = (RunActive) activity;
         } else if (reifiedObject instanceof RunActive) {
             functionalRunActive = (RunActive) reifiedObject;
         } else {
             functionalRunActive = new ComponentFIFORunActive();
         }
 
         // EndActive
         if ((activity != null) && activity instanceof EndActive) {
             functionalEndActive = (EndActive) activity;
         } else if (reifiedObject instanceof EndActive) {
             functionalEndActive = (EndActive) reifiedObject;
         } else {
             functionalEndActive = new DefaultEndActive();
         }
     }
 
     public void runActivity(Body body) {
         if ((componentRunActive != null) && (componentRunActive != this)) {
             componentRunActive.runActivity(body);
         } else {
             // this is the default activity of the active object
             // the activity of the component has been initialized and started, now
             // what we have to do is to manage the life cycle, i.e. start and stop the
             // activity
             // that can be redefined on the reified object.
             try {
                 Service componentService = new Service(body);
                 NFRequestFilterImpl nfRequestFilter = new NFRequestFilterImpl();
                 while (body.isActive()) {
                     ComponentBody componentBody = (ComponentBody) body;
                     while (LifeCycleController.STOPPED.equals(
                                 Fractal.getLifeCycleController(
                                     componentBody.getProActiveComponent())
                                            .getFcState())) {
                         componentService.blockingServeOldest(nfRequestFilter);
                     }
 
                     // 3.1. init object Activity
                     // life cycle started : starting activity of the object
                     if (functionalInitActive != null) {
                         functionalInitActive.initActivity(activeBody);
                         //functionalInitActive = null; // we won't do it again
                     }
 
                     ((ComponentBody) body).startingFunctionalActivity();
                     // 3.2 while object activity
                     // componentServe (includes filter on priority)
                     functionalRunActive.runActivity(body);
                     ((ComponentBody) body).finishedFunctionalActivity();
                     if (functionalEndActive != null) {
                         functionalEndActive.endActivity(body);
                     }
                 }
             } catch (NoSuchInterfaceException e) {
                 logger.error(
                     "could not retreive an interface, probably the life cycle controller of this component; terminating the component. Error message is : " +
                     e.getMessage());
             }
         }
     }
 
     /*
      * @see org.objectweb.proactive.InitActive#initActivity(org.objectweb.proactive.Body)
      */
     public void initActivity(Body body) {
         if (componentInitActive != null) {
             componentInitActive.initActivity(body);
         } else {
             if (logger.isDebugEnabled()) {
                 logger.debug(
                     "initializing component activity ... (component will be active but not yet started)");
             }
         }
     }
 
     /*
      * @see org.objectweb.proactive.EndActive#endActivity(org.objectweb.proactive.Body)
      */
     public void endActivity(Body body) {
         if (componentEndActive != null) {
             componentEndActive.endActivity(body);
         } else {
             if (logger.isDebugEnabled()) {
                 logger.debug(
                     "ending component activity ... (object is still active)");
             }
         }
     }
 
     private class ComponentFIFORunActive implements RunActive, Serializable {
         public void runActivity(Body body) {
             new Service(body).fifoServing();
         }
     }
 
     private class DefaultInitActive implements InitActive, Serializable {
         public void initActivity(Body body) {
             if (logger.isDebugEnabled()) {
                 logger.debug(
                     "initializing default functional activity of the component");
             }
         }
     }
 
     private class DefaultEndActive implements EndActive, Serializable {
         public void endActivity(Body body) {
             if (logger.isDebugEnabled()) {
                 logger.debug(
                     "ending default functional activity of this component");
             }
         }
     }
 
     private class ComponentInitActiveWrapper implements InitActive,
         Serializable {
         private ComponentInitActive wrappedComponentInitActive;
 
         public ComponentInitActiveWrapper(
             ComponentInitActive componentInitActive) {
             wrappedComponentInitActive = componentInitActive;
         }
 
         public void initActivity(Body body) {
             wrappedComponentInitActive.initComponentActivity(body);
         }
     }
 
     private class ComponentRunActiveWrapper implements RunActive, Serializable {
         private ComponentRunActive wrappedComponentRunActive;
 
         public ComponentRunActiveWrapper(ComponentRunActive componentRunActive) {
             wrappedComponentRunActive = componentRunActive;
         }
 
         public void runActivity(Body body) {
             wrappedComponentRunActive.runComponentActivity(body);
         }
     }
 
     private class ComponentEndActiveWrapper implements EndActive, Serializable {
         private ComponentEndActive wrappedComponentEndActive;
 
         public ComponentEndActiveWrapper(ComponentEndActive componentEndActive) {
             wrappedComponentEndActive = componentEndActive;
         }
 
         public void endActivity(Body body) {
             wrappedComponentEndActive.endComponentActivity(body);
         }
     }
 }
