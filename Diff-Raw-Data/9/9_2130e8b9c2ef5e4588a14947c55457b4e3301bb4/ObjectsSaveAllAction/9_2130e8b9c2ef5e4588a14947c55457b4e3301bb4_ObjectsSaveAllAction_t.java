 package gov.nasa.arc.mct.gui.actions;
 
 import gov.nasa.arc.mct.api.persistence.OptimisticLockException;
 import gov.nasa.arc.mct.components.AbstractComponent;
 import gov.nasa.arc.mct.gui.ActionContext;
 import gov.nasa.arc.mct.gui.ContextAwareAction;
 import gov.nasa.arc.mct.gui.impl.ActionContextImpl;
 import gov.nasa.arc.mct.platform.spi.PlatformAccess;
 import gov.nasa.arc.mct.services.internal.component.Updatable;
 
 import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.HashSet;
 import java.util.ResourceBundle;
 import java.util.Set;
 
 public class ObjectsSaveAllAction extends ContextAwareAction{
     private static final long serialVersionUID = 3940626077815919451L;
     private static final ResourceBundle BUNDLE = 
             ResourceBundle.getBundle(
                     ObjectsSaveAllAction.class.getName().substring(0, 
                             ObjectsSaveAllAction.class.getName().lastIndexOf("."))+".Bundle");
     private ActionContextImpl actionContext;
         
     public ObjectsSaveAllAction() {
         super(BUNDLE.getString("SaveAllAction.label"));
     }
     
     @Override
     public boolean canHandle(ActionContext context) {
         actionContext = (ActionContextImpl) context;
         return actionContext.getInspectorComponent() != null;
     }
 
 //    private boolean isComponentWriteableByUser(AbstractComponent component) {
 //        Platform p = PlatformAccess.getPlatform();
 //        PolicyContext policyContext = new PolicyContext();
 //        policyContext.setProperty(PolicyContext.PropertyName.TARGET_COMPONENT.getName(), component);
 //        policyContext.setProperty(PolicyContext.PropertyName.ACTION.getName(), 'w');
 //        String inspectionKey = PolicyInfo.CategoryType.OBJECT_INSPECTION_POLICY_CATEGORY.getKey();
 //        return p.getPolicyManager().execute(inspectionKey, policyContext).getStatus();
 //    }
     
     private AbstractComponent getInspectorComponent() {
         return actionContext.getInspectorComponent();
     }
     
     @Override
     public boolean isEnabled() {
         AbstractComponent ac = getInspectorComponent();
         return !ac.isStale() && ac.isDirty() || !ac.getAllModifiedObjects().isEmpty();
     }
 
     /**
      * This method is invoked when the client side object is stale. This can occur when another client 
      * even another window in the same application instance has saved the component after it has been loaded. 
      * This implementation will try again, which will overwrite the previous change; however, this is where 
      * configuration could be added to display a message instead. 
      */
     private void handleStaleObject(AbstractComponent ac) {
         overwritePreviousChanges(ac);
     }
     
     private void overwritePreviousChanges(AbstractComponent ac) {
         AbstractComponent updatedComp = PlatformAccess.getPlatform().getPersistenceProvider().getComponentFromStore(ac.getComponentId());
         ac.getCapability(Updatable.class).setVersion(updatedComp.getVersion());
         actionPerformed(null);
     }
     
     @Override
     public void actionPerformed(ActionEvent e) {
         AbstractComponent ac = getInspectorComponent();        
         Set<AbstractComponent> allModifiedObjects = ac.getAllModifiedObjects();
        if (ac.isDirty()) {
            if (allModifiedObjects == Collections.EMPTY_SET)
                allModifiedObjects = new HashSet<AbstractComponent>();
             allModifiedObjects.add(ac);
        }
         try {            
             PlatformAccess.getPlatform().getPersistenceProvider().persist(allModifiedObjects);
         } catch (OptimisticLockException ole) {
             handleStaleObject(ac);
         }
         ac.notifiedSaveAllSuccessful();
     }
 
 }
