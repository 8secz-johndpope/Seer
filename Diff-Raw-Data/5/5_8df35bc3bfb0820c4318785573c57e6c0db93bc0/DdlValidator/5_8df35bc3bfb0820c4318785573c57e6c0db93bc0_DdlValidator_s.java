 /*
 // $Id$
 // Farrago is an extensible data management system.
 // Copyright (C) 2005-2005 The Eigenbase Project
 // Copyright (C) 2003-2005 Disruptive Tech
 // Copyright (C) 2005-2005 LucidEra, Inc.
 // Portions Copyright (C) 2003-2005 John V. Sichi
 //
 // This program is free software; you can redistribute it and/or modify it
 // under the terms of the GNU General Public License as published by the Free
 // Software Foundation; either version 2 of the License, or (at your option)
 // any later version approved by The Eigenbase Project.
 //
 // This program is distributed in the hope that it will be useful,
 // but WITHOUT ANY WARRANTY; without even the implied warranty of
 // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 // GNU General Public License for more details.
 //
 // You should have received a copy of the GNU General Public License
 // along with this program; if not, write to the Free Software
 // Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
 package net.sf.farrago.ddl;
 
 import java.sql.*;
 import java.util.*;
 import java.util.logging.*;
 
 import javax.jmi.reflect.*;
 
 import net.sf.farrago.catalog.*;
 import net.sf.farrago.cwm.core.*;
 import net.sf.farrago.cwm.datatypes.*;
 import net.sf.farrago.cwm.keysindexes.*;
 import net.sf.farrago.cwm.relational.*;
 import net.sf.farrago.cwm.relational.enumerations.*;
 import net.sf.farrago.fem.med.*;
 import net.sf.farrago.fem.sql2003.*;
 import net.sf.farrago.fem.security.*;
 import net.sf.farrago.fennel.*;
 import net.sf.farrago.namespace.*;
 import net.sf.farrago.namespace.util.*;
 import net.sf.farrago.query.*;
 import net.sf.farrago.resource.*;
 import net.sf.farrago.session.*;
 import net.sf.farrago.trace.*;
 import net.sf.farrago.type.*;
 import net.sf.farrago.util.*;
 
 import org.eigenbase.sql.*;
 import org.eigenbase.sql.validate.SqlValidatorException;
 import org.eigenbase.sql.parser.*;
 import org.eigenbase.util.*;
 import org.netbeans.api.mdr.events.*;
 
 
 /**
  * DdlValidator validates the process of applying a DDL statement to the
  * catalog.  By implementing MDRPreChangeListener, it is able to
  * automatically collect references to all objects modified by the statement.
  * (MDRChangeListener isn't suitable since it's asynchronous.)
  *
  * <p>
  * Generic validation support is implemented in this class, but
 * object-specific rules should be implemented in the Impl classes for the
  * appropriate catalog objects.
  * </p>
  *
  * NOTE:  all validation activity must take place in the same thread in which
  * DdlValidator's constructor is called.
  *
  * @author John V. Sichi
  * @version $Id$
  */
 public class DdlValidator extends FarragoCompoundAllocation
     implements FarragoSessionDdlValidator,
         MDRPreChangeListener
 {
     //~ Static fields/initializers --------------------------------------------
 
     private static final Logger tracer = FarragoTrace.getDdlValidatorTracer();
 
     /** Symbolic constant used to mark an element being created */
     private static final Integer VALIDATE_CREATION = new Integer(1);
 
     /** Symbolic constant used to mark an element being updated */
     private static final Integer VALIDATE_MODIFICATION = new Integer(2);
 
     /** Symbolic constant used to mark an element being deleted */
     private static final Integer VALIDATE_DELETION = new Integer(3);
 
     /** Symbolic constant used to mark an element being truncated */
     private static final Integer VALIDATE_TRUNCATION = new Integer(4);
 
     //~ Instance fields -------------------------------------------------------
 
     private final FarragoSessionStmtValidator stmtValidator;
 
     /** Queue of excns detected during plannedChange. */
     private DeferredException enqueuedValidationExcn;
 
     /**
      * Map (from RefAssociation.Class to FarragoSessionDdlDropRule) of
      * associations for which special handling is required during DROP.
      */
     private MultiMap dropRules;
 
     /** Map from catalog object to SqlParserPos for beginning of definition. */
     private final Map parserContextMap;
 
     /** Map from catalog object to SqlParserPos for offset of body. */
     private final Map parserOffsetMap;
 
     /**
      * Map from catalog object to associated SQL definition
      * (not all objects have these).
      */
     private final Map sqlMap;
 
     /**
      * Map containing scheduled validation actions.  The key is the MofId of
      * the object scheduled for validation; the value is the action type (one
      * of the symbols {@link #VALIDATE_CREATION}, {@link #VALIDATE_DELETION},
      * {@link #VALIDATE_MODIFICATION}, {@link #VALIDATE_TRUNCATION}).
      */
     private Map schedulingMap;
 
     /**
      * Map of objects in transition between schedulingMap and validatedMap.
      * Content format is same as for schedulingMap.
      */
     private Map transitMap;
 
     /**
      * Map of object validations which have already taken place.
      * The key is the RefObject itself; the value is the action type.
      */
     private Map validatedMap;
 
     /**
      * Set of objects which a DROP CASCADE has encountered but not yet
      * processed.
      */
     private Set deleteQueue;
 
     /**
      * Thread binding to prevent cross-talk.
      */
     private Thread activeThread;
 
     /**
      * DDL statement being validated.
      */
     private FarragoSessionDdlStmt ddlStmt;
 
     /**
      * List of handlers to be invoked.
      */
     protected List actionHandlers;
 
     //~ Constructors ----------------------------------------------------------
 
     /**
      * Creates a new validator.  The validator will listen for repository
      * change events and schedule appropriate validation actions on the
      * affected objects.  Validation is deferred until validate() is called.
      *
      * @param stmtValidator generic stmt validator
      */
     public DdlValidator(FarragoSessionStmtValidator stmtValidator)
     {
         this.stmtValidator = stmtValidator;
         stmtValidator.addAllocation(this);
 
         // NOTE jvs 25-Jan-2004:  Use LinkedHashXXX, since order
         // matters for these.
         schedulingMap = new LinkedHashMap();
         validatedMap = new LinkedHashMap();
         deleteQueue = new LinkedHashSet();
 
         parserContextMap = new HashMap();
         parserOffsetMap = new HashMap();
         sqlMap = new HashMap();
 
         // NOTE:  dropRules are populated implicitly as action handlers
         // are set up below.
         dropRules = new MultiMap();
 
         // Build up list of action handlers.
         actionHandlers = new ArrayList();
 
         // First, install action handlers for all installed model
         // extensions.
         Iterator extIter =
             stmtValidator.getSession().getModelExtensions().iterator();
         while (extIter.hasNext()) {
             FarragoSessionModelExtension ext = (FarragoSessionModelExtension)
                 extIter.next();
             ext.defineDdlHandlers(
                 this,
                 actionHandlers);
         }
 
         // Then, install action handlers specific to this personality.
         stmtValidator.getSession().getPersonality().defineDdlHandlers(
             this,
             actionHandlers);
 
         // MDR pre-change instance creation events are useless, since they
         // don't refer to the new instance.  Instead, we rely on the
         // fact that it's pretty much guaranteed that the new object will have
         // attributes or associations set (though someone will probably come up
         // with a pathological case eventually).
         activeThread = Thread.currentThread();
         getRepos().getMdrRepos().addListener(this,
             InstanceEvent.EVENT_INSTANCE_DELETE
                 | AttributeEvent.EVENTMASK_ATTRIBUTE
                 | AssociationEvent.EVENTMASK_ASSOCIATION);
     }
 
     //~ Methods ---------------------------------------------------------------
 
     // implement FarragoSessionDdlValidator
     public FarragoSessionStmtValidator getStmtValidator()
     {
         return stmtValidator;
     }
 
     // implement FarragoSessionDdlValidator
     public FarragoRepos getRepos()
     {
         return stmtValidator.getRepos();
     }
 
     // implement FarragoSessionDdlValidator
     public FennelDbHandle getFennelDbHandle()
     {
         return stmtValidator.getFennelDbHandle();
     }
 
     // implement FarragoSessionDdlValidator
     public FarragoTypeFactory getTypeFactory()
     {
         return stmtValidator.getTypeFactory();
     }
 
     // implement FarragoSessionDdlValidator
     public FarragoSessionIndexMap getIndexMap()
     {
         return stmtValidator.getIndexMap();
     }
 
     // implement FarragoSessionDdlValidator
     public FarragoDataWrapperCache getDataWrapperCache()
     {
         return stmtValidator.getDataWrapperCache();
     }
 
     // implement FarragoSessionDdlValidator
     public FarragoSession newReentrantSession()
     {
         return getInvokingSession().cloneSession(
             stmtValidator.getSessionVariables());
     }
 
     // implement FarragoSessionDdlValidator
     public FarragoSession getInvokingSession()
     {
         return stmtValidator.getSession();
     }
 
     // implement FarragoSessionDdlValidator
     public void releaseReentrantSession(FarragoSession session)
     {
         session.closeAllocation();
     }
 
     // implement FarragoSessionDdlValidator
     public FarragoSessionParser getParser()
     {
         return stmtValidator.getParser();
     }
 
     // implement FarragoSessionDdlValidator
     public boolean isDeletedObject(RefObject refObject)
     {
         return testObjectStatus(refObject, VALIDATE_DELETION);
     }
 
     // implement FarragoSessionDdlValidator
     public boolean isCreatedObject(RefObject refObject)
     {
         return testObjectStatus(refObject, VALIDATE_CREATION);
     }
 
     private boolean testObjectStatus(
         RefObject refObject,
         Integer status)
     {
         return (schedulingMap.get(refObject.refMofId()) == status)
             || (validatedMap.get(refObject) == status)
             || ((transitMap != null)
             && (transitMap.get(refObject.refMofId()) == status));
     }
 
     // implement FarragoSessionDdlValidator
     public boolean isDropRestrict()
     {
         return ddlStmt.isDropRestricted();
     }
 
     /**
      * Determines whether a catalog object has had its visibility set yet.
      * NOTE: this should remain private and only be called prior to
      * executeStorage().
      *
      * @param modelElement object in question
      *
      * @return true if refObject isn't visible yet
      */
     private boolean isNewObject(CwmModelElement modelElement)
     {
         return modelElement.getVisibility() != VisibilityKindEnum.VK_PUBLIC;
     }
 
     // implement FarragoSessionDdlValidator
     public String getParserPosString(RefObject obj)
     {
         SqlParserPos parserContext = getParserPos(obj);
         if (parserContext == null) {
             return null;
         }
         return parserContext.toString();
     }
 
     private SqlParserPos getParserPos(RefObject obj)
     {
         return (SqlParserPos) parserContextMap.get(obj);
     }
 
     // implement FarragoSessionDdlValidator
     public void setParserOffset(RefObject obj, SqlParserPos pos)
     {
         parserOffsetMap.put(obj, pos);
     }
 
     // implement FarragoSessionDdlValidator
     public SqlParserPos getParserOffset(RefObject obj)
     {
         return (SqlParserPos) parserOffsetMap.get(obj);
     }
 
     // implement FarragoSessionDdlValidator
     public void setSqlDefinition(RefObject obj, SqlNode sqlNode)
     {
         sqlMap.put(obj, sqlNode);
     }
 
     // implement FarragoSessionDdlValidator
     public SqlNode getSqlDefinition(RefObject obj)
     {
         SqlNode sqlNode = (SqlNode) sqlMap.get(obj);
         SqlParserPos parserContext = getParserPos(obj);
         if (parserContext != null) {
             stmtValidator.setParserPosition(parserContext);
         }
         return sqlNode;
     }
 
     // implement FarragoSessionDdlValidator
     public void setSchemaObjectName(
         CwmModelElement schemaElement,
         SqlIdentifier qualifiedName)
     {
         SqlIdentifier schemaName = null;
         assert (qualifiedName.names.length > 0);
         assert (qualifiedName.names.length < 4);
 
         if (qualifiedName.names.length == 3) {
             schemaElement.setName(qualifiedName.names[2]);
             schemaName = new SqlIdentifier(
                 new String[] {
                     qualifiedName.names[0],
                     qualifiedName.names[1]
                 },
                 SqlParserPos.ZERO);
         } else if (qualifiedName.names.length == 2) {
             schemaElement.setName(qualifiedName.names[1]);
             schemaName = new SqlIdentifier(
                 qualifiedName.names[0],
                 SqlParserPos.ZERO);
         } else {
             schemaElement.setName(qualifiedName.names[0]);
             if (stmtValidator.getSessionVariables().schemaName == null) {
                 throw FarragoResource.instance().ValidatorNoDefaultSchema.ex();
             }
             schemaName = new SqlIdentifier(
                 stmtValidator.getSessionVariables().schemaName,
                 SqlParserPos.ZERO);
         }
         CwmSchema schema = stmtValidator.findSchema(schemaName);
         schema.getOwnedElement().add(schemaElement);
     }
 
     // implement MDRChangeListener
     public void change(MDRChangeEvent event)
     {
         // don't care
     }
 
     // implement MDRPreChangeListener
     public void changeCancelled(MDRChangeEvent event)
     {
         // don't care
     }
 
     // override FarragoCompoundAllocation
     public void closeAllocation()
     {
         stopListening();
         super.closeAllocation();
     }
 
     // implement FarragoSessionDdlValidator
     public void executeStorage()
     {
         // catalog updates which occur during execution should not result in
         // further validation
         stopListening();
 
         List deletionList = new ArrayList();
         Iterator mapIter = validatedMap.entrySet().iterator();
         while (mapIter.hasNext()) {
             Map.Entry mapEntry = (Map.Entry) mapIter.next();
             RefObject obj = (RefObject) mapEntry.getKey();
             Object action = mapEntry.getValue();
 
             if (obj instanceof CwmModelElement) {
                 CwmModelElement modelElement = (CwmModelElement) obj;
 
                 if (action != VALIDATE_DELETION) {
                     updateObjectTimestamp(modelElement);
                 }
             }
 
             if (obj instanceof CwmStructuralFeature) {
                 // Set some mandatory but irrelevant attributes.
                 CwmStructuralFeature feature = (CwmStructuralFeature) obj;
 
                 feature.setChangeability(ChangeableKindEnum.CK_CHANGEABLE);
             }
 
             if (action == VALIDATE_DELETION) {
                 clearDependencySuppliers(obj);
                 RefFeatured container = obj.refImmediateComposite();
                 if (container != null) {
                     Object containerAction = validatedMap.get(container);
                     if (containerAction == VALIDATE_DELETION) {
                         // container is also being deleted; don't try
                         // deleting this contained object--depending on order,
                         // the attempt could cause an excn
                     } else {
                         // container is not being deleted
                         deletionList.add(obj);
                     }
                 } else {
                     // top-level object
                     deletionList.add(obj);
                 }
             } else {
                 JmiUtil.assertConstraints(obj);
             }
             if (!(obj instanceof CwmModelElement)) {
                 continue;
             }
             CwmModelElement element = (CwmModelElement) obj;
             if (action == VALIDATE_CREATION) {
                 invokeHandler(element, "executeCreation");
             } else if (action == VALIDATE_DELETION) {
                 invokeHandler(element, "executeDrop");
             } else if (action == VALIDATE_TRUNCATION) {
                 invokeHandler(element, "executeTruncation");
             } else if (action == VALIDATE_MODIFICATION) {
                 invokeHandler(element, "executeModification");
             } else {
                 assert (false);
             }
         }
 
         // now we can finally consummate any requested deletions, since we're
         // all done referencing the objects
         Collections.reverse(deletionList);
         Iterator deletionIter = deletionList.iterator();
         while (deletionIter.hasNext()) {
             RefObject refObj = (RefObject) deletionIter.next();
             if (tracer.isLoggable(Level.FINE)) {
                 tracer.fine("really deleting " + refObj);
             }
             refObj.refDelete();
         }
     }
 
     private void updateObjectTimestamp(CwmModelElement element)
     {
         FemCreationGrant creationGrant = null;
         if (isNewObject(element)) {
             // Define a pseudo-grant representing the element's relationship to
             // its creator.  As a bonus, this will initialize element creation
             // time.
             creationGrant = FarragoCatalogUtil.newCreationGrant(
                 getRepos(),
                 FarragoCatalogInit.SYSTEM_USER_NAME,
                 getInvokingSession().getSessionVariables().currentUserName,
                 element);
         } else {
             // Find the original creation grant for this existing element.
             PrivilegeIsGrantedOnElement privAssoc =
                 getRepos().getSecurityPackage().
                 getPrivilegeIsGrantedOnElement();
             Iterator iter = privAssoc.getPrivilege(element).iterator();
             while (iter.hasNext()) {
                 Object obj = iter.next();
                 if (obj instanceof FemCreationGrant) {
                     creationGrant = (FemCreationGrant) obj;
                     break;
                 }
             }
         }
         assert(creationGrant != null);
 
         // Update element modification time.
         Timestamp ts = new Timestamp(System.currentTimeMillis());
         creationGrant.setModificationTimestamp(ts.toString());
         
         // We use the visibility attribute to distinguish new objects
         // from existing objects.  From here on, no longer treat
         // this element as new.
         element.setVisibility(VisibilityKindEnum.VK_PUBLIC);
     }
 
     // implement MDRPreChangeListener
     public void plannedChange(MDRChangeEvent event)
     {
         if (tracer.isLoggable(Level.FINE)) {
             tracer.fine(event.toString());
         }
         if (activeThread != Thread.currentThread()) {
             // REVIEW:  This isn't going to be good enough if we have
             // reentrant DDL.
             // ignore events from other threads
             return;
         }
 
         // NOTE:  do not throw exceptions from this method, because MDR will
         // swallow them!  Instead, use enqueueValidationExcn(), and the
         // exception will be thrown when validate() is called.
         if (event.getType() == InstanceEvent.EVENT_INSTANCE_DELETE) {
             RefObject obj = (RefObject) (event.getSource());
             clearDependencySuppliers(obj);
             scheduleDeletion(obj);
         } else if (event instanceof AttributeEvent) {
             RefObject obj = (RefObject) (event.getSource());
             scheduleModification(obj);
         } else if (event instanceof AssociationEvent) {
             AssociationEvent associationEvent = (AssociationEvent) event;
             if (tracer.isLoggable(Level.FINE)) {
                 tracer.fine("end name = " + associationEvent.getEndName());
             }
             scheduleModification(associationEvent.getFixedElement());
             if (associationEvent.getOldElement() != null) {
                 scheduleModification(associationEvent.getNewElement());
             }
             if (associationEvent.getNewElement() != null) {
                 scheduleModification(associationEvent.getNewElement());
             }
             if (event.getType() == AssociationEvent.EVENT_ASSOCIATION_REMOVE) {
                 RefAssociation refAssoc =
                     (RefAssociation) associationEvent.getSource();
                 List rules = dropRules.getMulti(refAssoc.getClass());
                 Iterator ruleIter = rules.iterator();
                 while (ruleIter.hasNext()) {
                     FarragoSessionDdlDropRule rule =
                         (FarragoSessionDdlDropRule) ruleIter.next();
                     if ((rule != null)
                             && rule.getEndName().equals(
                                 associationEvent.getEndName())) {
                         fireDropRule(
                             rule,
                             associationEvent.getFixedElement(),
                             associationEvent.getOldElement());
                     }
                 }
             }
         } else {
             // REVIEW:  when I turn this on, I see notifications for
             // CreateInstanceEvent, which is supposed to be masked out.
             // Probably an MDR bug.
 
             /*
                tracer.warning(
                    "Unexpected event "+event.getClass().getName()
                    + ", type="+event.getType());
                assert(false);
             */
         }
     }
 
     // implement FarragoSessionDdlValidator
     public void scheduleTruncation(CwmModelElement modelElement)
     {
         assert (!validatedMap.containsKey(modelElement));
         assert (!schedulingMap.containsKey(modelElement.refMofId()));
         schedulingMap.put(
             modelElement.refMofId(),
             VALIDATE_TRUNCATION);
         mapParserPosition(modelElement);
     }
 
     // implement FarragoSessionDdlValidator
     public void validate(FarragoSessionDdlStmt ddlStmt)
     {
         this.ddlStmt = ddlStmt;
         ddlStmt.preValidate(this);
         checkValidationExcnQueue();
 
         if (ddlStmt instanceof DdlDropStmt) {
             // Process deletions until a fixpoint is reached, using MDR events
             // to implement RESTRICT/CASCADE.
             while (!deleteQueue.isEmpty()) {
                 RefObject refObj = (RefObject) deleteQueue.iterator().next();
                 deleteQueue.remove(refObj);
                 if (!schedulingMap.containsKey(refObj.refMofId())) {
                     if (tracer.isLoggable(Level.FINE)) {
                         tracer.fine("probe deleting " + refObj);
                     }
                     refObj.refDelete();
                 }
             }
 
             // In order to validate deletion, we need the objects to exist, but
             // they've already been deleted.  So rollback the deletion now, but
             // we still remember the objects encountered above.
             rollbackDeletions();
 
             // REVIEW:  This may need to get more complicated in the future.
             // Like, if deletions triggered modifications to some referencing
             // object which needs to have its update validation run AFTER the
             // deletion takes place, not before.
         }
 
         while (!schedulingMap.isEmpty()) {
             checkValidationExcnQueue();
 
             // Swap in a new map so new scheduling calls aren't handled until
             // the next round.
             transitMap = schedulingMap;
             schedulingMap = new LinkedHashMap();
 
             boolean progress = false;
             Iterator mapIter = transitMap.entrySet().iterator();
             while (mapIter.hasNext()) {
                 Map.Entry mapEntry = (Map.Entry) mapIter.next();
                 RefObject obj =
                     (RefObject) getRepos().getMdrRepos().getByMofId(
                         (String) mapEntry.getKey());
                 if (obj == null) {
                     continue;
                 }
                 Object action = mapEntry.getValue();
 
                 // mark this object as already validated so it doesn't slip
                 // back in by updating itself
                 validatedMap.put(obj, action);
 
                 if (!(obj instanceof CwmModelElement)) {
                     progress = true;
                     continue;
                 }
                 CwmModelElement element = (CwmModelElement) obj;
                 try {
                     validateAction(element, action);
                     if (element.getVisibility() == null) {
                         // mark this new element as validated
                         element.setVisibility(VisibilityKindEnum.VK_PRIVATE);
                     }
                     progress = true;
                 } catch (FarragoUnvalidatedDependencyException ex) {
                     // Something hit an unvalidated dependency; we'll have
                     // to retry this object later.
                     validatedMap.remove(obj);
                     schedulingMap.put(
                         obj.refMofId(),
                         action);
                 }
             }
             transitMap = null;
             if (!progress) {
                 // Every single object hit a
                 // FarragoUnvalidatedDependencyException.  This implies a
                 // cycle.  TODO:  identify the cycle in the exception.
                 throw FarragoResource.instance().ValidatorSchemaDependencyCycle.ex();
             }
         }
 
         // one last time
         checkValidationExcnQueue();
 
         // finally, process any deferred privilege checks
         stmtValidator.getPrivilegeChecker().checkAccess();
     }
 
     private void clearDependencySuppliers(RefObject refObj)
     {
         // REVIEW: We have to break dependencies explicitly
         // before object deletion, or all kinds of nasty internal
         // MDR errors result.  Should find out if that's expected
         // behavior.
         if (refObj instanceof CwmDependency) {
             CwmDependency dependency = (CwmDependency) refObj;
             dependency.getSupplier().clear();
             dependency.getClient().clear();
         }
     }
 
     // implement FarragoSessionDdlValidator
     public void validateUniqueNames(
         CwmModelElement container,
         Collection collection,
         boolean includeType)
     {
         Map nameMap = new LinkedHashMap();
         Iterator iter = collection.iterator();
         while (iter.hasNext()) {
             CwmModelElement element = (CwmModelElement) iter.next();
             Object nameKey = getNameKey(element, includeType);
             if (nameKey == null) {
                 continue;
             }
 
             CwmModelElement other = (CwmModelElement) nameMap.get(nameKey);
             if (other != null) {
                 if (isNewObject(other) && isNewObject(element)) {
                     // clash between two new objects being defined
                     // simultaneously
                     // REVIEW: error message assumes collection is sorted by
                     // definition order, which may not be guaranteed?
                     throw newPositionalError(
                         element,
                         FarragoResource.instance().ValidatorDuplicateNames.ex(
                             getRepos().getLocalizedObjectName(
                                 null,
                                 element.getName(),
                                 element.refClass()),
                             getRepos().getLocalizedObjectName(container),
                             getParserPosString(other)));
                 } else {
                     CwmModelElement newElement;
                     CwmModelElement oldElement;
                     if (isNewObject(other)) {
                         newElement = other;
                         oldElement = element;
                     } else {
                         // TODO:  remove this later when RENAME is supported
                         assert (isNewObject(element));
                         newElement = element;
                         oldElement = other;
                     }
 
                     // new object clashes with existing object
                     throw newPositionalError(
                         newElement,
                         FarragoResource.instance().ValidatorNameInUse.ex(
                             getRepos().getLocalizedObjectName(
                                 null,
                                 oldElement.getName(),
                                 oldElement.refClass()),
                             getRepos().getLocalizedObjectName(container)));
                 }
             }
             nameMap.put(nameKey, element);
         }
     }
 
     private Object getNameKey(
         CwmModelElement element,
         boolean includeType)
     {
         String name = element.getName();
         if (name == null) {
             return null;
         }
         if (!includeType) {
             return name;
         }
         Class typeClass;
         // TODO jvs 25-Feb-2005:  provide a mechanism for generalizing these
        // special case
         if (element instanceof CwmNamedColumnSet) {
             typeClass = CwmNamedColumnSet.class;
         } else if (element instanceof CwmCatalog) {
             typeClass = CwmCatalog.class;
         } else {
             typeClass = element.getClass();
         }
         return typeClass.toString() + ":" + name;
     }
 
     // implement FarragoSessionDdlValidator
     public CwmDependency createDependency(
         CwmNamespace client,
         Collection suppliers)
     {
         String depName = client.getName() + "$DEP";
         CwmDependency dependency = (CwmDependency)
             FarragoCatalogUtil.getModelElementByNameAndType(
                 client.getOwnedElement(),
                 depName,
                 getRepos().getCorePackage().getCwmDependency());
 
         if (dependency == null) {
             dependency = getRepos().newCwmDependency();
             dependency.setName(depName);
             dependency.setKind("GenericDependency");
 
             // NOTE: The client owns the dependency, so their lifetimes are
             // coeval.  That's why we restrict clients to being namespaces.
             client.getOwnedElement().add(dependency);
             dependency.getClient().add(client);
         }
 
         Iterator iter = suppliers.iterator();
         while (iter.hasNext()) {
             Object supplier = iter.next();
             dependency.getSupplier().add(supplier);
         }
 
         return dependency;
     }
 
     // implement FarragoSessionDdlValidator
     public void discardDataWrapper(CwmModelElement wrapper)
     {
         stmtValidator.getSharedDataWrapperCache().discard(wrapper.refMofId());
     }
 
     // implement FarragoSessionDdlValidator
     public void setCreatedSchemaContext(FemLocalSchema schema)
     {
         FarragoSessionVariables sessionVariables =
             getStmtValidator().getSessionVariables();
 
         sessionVariables.catalogName =
             schema.getNamespace().getName();
         sessionVariables.schemaName =
             schema.getName();
 
         // convert from List<FemSqlpathElement> to List<SqlIdentifier>
         List list = new ArrayList();
         Iterator iter = schema.getPathElement().iterator();
         while (iter.hasNext()) {
             FemSqlpathElement element = (FemSqlpathElement) iter.next();
             SqlIdentifier id = new SqlIdentifier(
                 new String [] {
                     element.getSearchedSchemaCatalogName(),
                     element.getSearchedSchemaName()
                 },
                 SqlParserPos.ZERO);
             list.add(id);
         }
         sessionVariables.schemaSearchPath =
             Collections.unmodifiableList(list);
     }
 
     // implement FarragoSessionDdlValidator
     public EigenbaseException newPositionalError(
         RefObject refObj,
         SqlValidatorException ex)
     {
         SqlParserPos parserContext = getParserPos(refObj);
         if (parserContext == null) {
             return new EigenbaseException(ex.getMessage(), ex.getCause());
         }
         String msg = parserContext.toString();
         EigenbaseContextException contextExcn =
             FarragoResource.instance().ValidatorPositionContext.ex(msg, ex);
         contextExcn.setPosition(
             parserContext.getLineNum(),
             parserContext.getColumnNum(),
             parserContext.getEndLineNum(),
             parserContext.getEndColumnNum());
         return contextExcn;
     }
 
     // implement FarragoSessionDdlValidator
     public void defineDropRule(
         RefAssociation refAssoc,
         FarragoSessionDdlDropRule dropRule)
     {
         // NOTE:  use class object because in some circumstances MDR makes
         // up multiple instances of the same association, but doesn't implement
         // equals/hashCode correctly.
         dropRules.putMulti(
             refAssoc.getClass(),
             dropRule);
     }
 
     private void enqueueValidationExcn(DeferredException excn)
     {
         if (enqueuedValidationExcn != null) {
             // for now, only deal with one at a time
             return;
         }
         enqueuedValidationExcn = excn;
     }
 
     private void checkValidationExcnQueue()
     {
         if (enqueuedValidationExcn != null) {
             // rollback any deletions so that exception construction can
             // rely on pre-deletion state (e.g. for object identification)
             rollbackDeletions();
             throw enqueuedValidationExcn.getException();
         }
     }
 
     private void fireDropRule(
         FarragoSessionDdlDropRule rule,
         RefObject droppedEnd,
         RefObject otherEnd)
     {
         if ((rule.getSuperInterface() != null)
                 && !(rule.getSuperInterface().isInstance(droppedEnd))) {
             return;
         }
         ReferentialRuleTypeEnum action = rule.getAction();
         if (action == ReferentialRuleTypeEnum.IMPORTED_KEY_CASCADE) {
             deleteQueue.add(otherEnd);
             return;
         }
         if (!isDropRestrict()) {
             deleteQueue.add(otherEnd);
             return;
         }
 
         // NOTE: We can't construct the exception now since the object is
         // deleted.  Instead, defer until after rollback.
         final String mofId = droppedEnd.refMofId();
         enqueueValidationExcn(
             new DeferredException() {
                 EigenbaseException getException()
                 {
                     CwmModelElement droppedElement =
                         (CwmModelElement) getRepos().getMdrRepos().getByMofId(mofId);
                     return FarragoResource.instance().ValidatorDropRestrict.ex(
                         getRepos().getLocalizedObjectName(
                             droppedElement,
                             droppedElement.refClass()));
                 }
             });
     }
 
     private void mapParserPosition(Object obj)
     {
         SqlParserPos context =
             getParser().getCurrentPosition();
         if (context == null) {
             return;
         }
         parserContextMap.put(obj, context);
     }
 
     private void rollbackDeletions()
     {
         // A savepoint or chained transaction would be smoother, but MDR
         // doesn't support those.  Instead, have to restart the txn
         // altogether.  Take advantage of the fact that MDR allows us to retain
         // references across txns.
         getRepos().endReposTxn(true);
 
         // TODO:  really need a lock to protect us against someone else's DDL
         // here, which could invalidate our schedulingMap.
         getRepos().beginReposTxn(true);
     }
 
     private void scheduleDeletion(RefObject obj)
     {
         assert (!validatedMap.containsKey(obj));
 
         // delete overrides anything else
         schedulingMap.put(
             obj.refMofId(),
             VALIDATE_DELETION);
         mapParserPosition(obj);
     }
 
     private void scheduleModification(RefObject obj)
     {
         if (obj == null) {
             return;
         }
         if (validatedMap.containsKey(obj)) {
             return;
         }
         if (schedulingMap.containsKey(obj.refMofId())) {
             return;
         }
         if (!(obj instanceof CwmModelElement)) {
             // NOTE jvs 12-June-2005:  record it so that we can run
             // integrity verification on it during executeStorage()
             schedulingMap.put(obj.refMofId(), VALIDATE_MODIFICATION);
             return;
         }
         CwmModelElement element = (CwmModelElement) obj;
         if (isNewObject(element)) {
             schedulingMap.put(
                 obj.refMofId(),
                 VALIDATE_CREATION);
         } else {
             schedulingMap.put(
                 obj.refMofId(),
                 VALIDATE_MODIFICATION);
         }
         mapParserPosition(obj);
     }
 
     private void stopListening()
     {
         if (activeThread != null) {
             getRepos().getMdrRepos().removeListener(this);
             activeThread = null;
         }
     }
 
     private boolean validateAction(
         CwmModelElement modelElement,
         Object action)
     {
         stmtValidator.setParserPosition(null);
         if (action == VALIDATE_CREATION) {
             return invokeHandler(
                 modelElement,
                 "validateDefinition");
         } else if (action == VALIDATE_MODIFICATION) {
             return invokeHandler(
                 modelElement,
                 "validateModification");
         } else if (action == VALIDATE_DELETION) {
             return invokeHandler(
                 modelElement,
                 "validateDrop");
         } else if (action == VALIDATE_TRUNCATION) {
             return invokeHandler(
                 modelElement,
                 "validateTruncation");
         } else {
             throw new AssertionError();
         }
     }
 
     private boolean invokeHandler(
         CwmModelElement modelElement,
         String action)
     {
         for (int i = 0; i < actionHandlers.size(); ++i) {
             Object handler = actionHandlers.get(i);
             boolean handled = ReflectUtil.invokeVisitor(
                 handler,
                 modelElement,
                 CwmModelElement.class,
                 action);
             if (handled) {
                 return true;
             }
         }
         return false;
     }
 
     //~ Inner Classes ---------------------------------------------------------
 
     /**
      * DeferredException allows an exception's creation to be deferred.
      * This is needed since it is not possible to correctly construct
      * an exception in certain validation contexts.
      */
     private static abstract class DeferredException
     {
         abstract EigenbaseException getException();
     }
 }
 
 
 // End DdlValidator.java
