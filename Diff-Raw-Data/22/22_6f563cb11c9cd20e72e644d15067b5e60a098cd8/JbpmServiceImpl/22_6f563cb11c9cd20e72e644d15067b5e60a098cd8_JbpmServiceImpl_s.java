 /*
  * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
  *
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the GNU Lesser General Public License
  * (LGPL) version 2.1 which accompanies this distribution, and is available at
  * http://www.gnu.org/licenses/lgpl.html
  *
  * This library is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * Contributors:
  *     arussel
  */
 package org.nuxeo.ecm.platform.jbpm.core.service;
 
 import java.io.Serializable;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 import org.hibernate.Session;
 import org.jbpm.JbpmConfiguration;
 import org.jbpm.JbpmContext;
 import org.jbpm.context.exe.ContextInstance;
 import org.jbpm.db.GraphSession;
 import org.jbpm.graph.def.ProcessDefinition;
 import org.jbpm.graph.def.Transition;
 import org.jbpm.graph.exe.ProcessInstance;
 import org.jbpm.taskmgmt.exe.TaskInstance;
 import org.nuxeo.ecm.core.api.ClientException;
 import org.nuxeo.ecm.core.api.CoreInstance;
 import org.nuxeo.ecm.core.api.CoreSession;
 import org.nuxeo.ecm.core.api.DocumentModel;
 import org.nuxeo.ecm.core.api.IdRef;
 import org.nuxeo.ecm.core.api.NuxeoGroup;
 import org.nuxeo.ecm.core.api.NuxeoPrincipal;
 import org.nuxeo.ecm.platform.jbpm.JbpmListFilter;
 import org.nuxeo.ecm.platform.jbpm.JbpmOperation;
 import org.nuxeo.ecm.platform.jbpm.JbpmSecurityPolicy;
 import org.nuxeo.ecm.platform.jbpm.JbpmService;
 import org.nuxeo.ecm.platform.jbpm.NuxeoJbpmException;
 import org.nuxeo.ecm.platform.jbpm.NuxeoJbpmRuntimeException;
 import org.nuxeo.ecm.platform.jbpm.NuxeoJbpmSecurityException;
 import org.nuxeo.ecm.platform.usermanager.UserManager;
 import org.nuxeo.runtime.api.Framework;
 
 /**
  * @author arussel
  *
  */
 public class JbpmServiceImpl implements JbpmService {
 
     private static final String FROM_ORG_JBPM_TASKMGMT_EXE_TASK_INSTANCE_TI_WHERE_TI_END_IS_NULL = "from org.jbpm.taskmgmt.exe.TaskInstance ti where ti.end is null";
 
     private Map<String, String[]> typeFilters;
 
     private JbpmConfiguration configuration;
 
     private UserManager userManager;
 
     public static final ThreadLocal<JbpmContext> contexts = new ThreadLocal<JbpmContext>();
 
     private final List<JbpmSecurityPolicy> securityPolicies = new ArrayList<JbpmSecurityPolicy>();
 
     public Serializable executeJbpmOperation(JbpmOperation operation)
             throws NuxeoJbpmException {
         JbpmContext context = getContext();
         return operation.run(context);
     }
 
     // we open the first call in the thread
     // and close it on the session complete of hibernate.
     protected JbpmContext getContext() {
         JbpmContext context = contexts.get();
         if (context == null) {
             context = configuration.createJbpmContext();
             contexts.set(context);
             context.getSession().getTransaction().registerSynchronization(
                     new JbpmSynchronization(context));
         }
         return context;
     }
 
     public JbpmConfiguration getConfiguration() {
         return configuration;
     }
 
     protected void setConfiguration(JbpmConfiguration configuration) {
         this.configuration = configuration;
     }
 
     @SuppressWarnings("unchecked")
     public List<TaskInstance> getCurrentTaskInstances(
             final NuxeoPrincipal currentUser, final JbpmListFilter filter)
             throws NuxeoJbpmException {
         try {
             return (List<TaskInstance>) executeJbpmOperation(new JbpmOperation() {
                 public ArrayList<TaskInstance> run(JbpmContext context)
                         throws NuxeoJbpmException {
                     context.setActorId(NuxeoPrincipal.PREFIX
                             + currentUser.getName());
                     List<String> groups = currentUser.getAllGroups();
                     List<String> prefixedGroup = new ArrayList<String>();
                     for (String s : groups) {
                         prefixedGroup.add(NuxeoGroup.PREFIX + s);
                     }
                     prefixedGroup.add(NuxeoPrincipal.PREFIX
                             + currentUser.getName());
                     ArrayList<TaskInstance> tis = new ArrayList<TaskInstance>();
                     tis.addAll(context.getTaskMgmtSession().findTaskInstances(
                             prefixedGroup));
                     tis.addAll(context.getTaskMgmtSession().findPooledTaskInstances(
                             prefixedGroup));
 
                     // filter
                     if (filter != null) {
                         tis = filter.filter(context, null, tis, currentUser);
                     }
 
                     // remove duplicates
                     HashSet<TaskInstance> setTis = new HashSet<TaskInstance>();
                     setTis.addAll(tis);
 
                     return toArrayList(setTis);
                 }
 
             });
         } catch (Exception e) {
             throw new NuxeoJbpmException(e);
         }
     }
 
     private <T> ArrayList<T> toArrayList(Collection<T> list) {
         ArrayList<T> arrayList = new ArrayList<T>();
         for (T t : list) {
             arrayList.add(t);
         }
         return arrayList;
     }
 
     public ProcessInstance createProcessInstance(final NuxeoPrincipal user,
             final String processInstanceName, final DocumentModel dm,
             final Map<String, Serializable> variables,
             final Map<String, Serializable> transientVariables)
             throws NuxeoJbpmException {
         ProcessInstance pi = (ProcessInstance) executeJbpmOperation(new JbpmOperation() {
             public ProcessInstance run(JbpmContext context)
                     throws NuxeoJbpmException {
                 context.setActorId(NuxeoPrincipal.PREFIX + user.getName());
                 ProcessDefinition pd = context.getGraphSession().findLatestProcessDefinition(
                         processInstanceName);
                 if (dm != null) {
                     boolean permission = getPermission(pd,
                             JbpmSecurityPolicy.Action.execute, dm) == Boolean.FALSE ? false
                             : true;
                     if (!permission) {
                         throw new NuxeoJbpmSecurityException();
                     }
                 }
                 ProcessInstance pi = context.newProcessInstance(processInstanceName.toString());
                 if (variables != null) {
                     pi.getContextInstance().addVariables(variables);
                 }
                 if (transientVariables != null) {
                     pi.getContextInstance().setTransientVariables(
                             transientVariables);
                 }
                 if (dm != null) {
                     pi.getContextInstance().setVariable(
                             JbpmService.VariableName.documentId.name(),
                             dm.getId());
                     pi.getContextInstance().setVariable(
                             JbpmService.VariableName.documentRepositoryName.name(),
                             dm.getRepositoryName());
                 }
                 TaskInstance ti = pi.getTaskMgmtInstance().createStartTaskInstance();
                 if (ti == null) {
                     pi.signal();
                 } else {
                     ti.end();
                 }
                 return pi;
             }
         });
         return pi;
     }
 
     @SuppressWarnings("unchecked")
     public List<ProcessInstance> getCurrentProcessInstances(
             final NuxeoPrincipal principal, final JbpmListFilter filter)
             throws NuxeoJbpmException {
         List<ProcessInstance> res = (List<ProcessInstance>) executeJbpmOperation(new JbpmOperation() {
             public ArrayList<ProcessInstance> run(JbpmContext context)
                     throws NuxeoJbpmException {
 
                 ArrayList<ProcessInstance> initiatorPD = new ArrayList<ProcessInstance>();
                 List<ProcessDefinition> pds = context.getGraphSession().findAllProcessDefinitions();
                 for (ProcessDefinition pd : pds) {
                     List<ProcessInstance> pis = context.getGraphSession().findProcessInstances(
                             pd.getId());
                     for (ProcessInstance pi : pis) {
                         String actorName = NuxeoPrincipal.PREFIX
                                 + principal.getName();
                         if (actorName.equals(pi.getContextInstance().getVariable(
                                 JbpmService.VariableName.initiator.name()))) {
                             initiatorPD.add(pi);
                         }
                     }
                 }
 
                 if (filter != null) {
                     initiatorPD = filter.filter(context, null, initiatorPD,
                             principal);
                 }
 
                 return toArrayList(initiatorPD);
             }
         });
 
         return res;
     }
 
     @SuppressWarnings("unchecked")
     protected List<ProcessDefinition> getProcessDefinitions(NuxeoPrincipal user)
             throws NuxeoJbpmException {
         return (List<ProcessDefinition>) executeJbpmOperation(new JbpmOperation() {
             public ArrayList<ProcessDefinition> run(JbpmContext context) {
                 GraphSession session = context.getGraphSession();
                 return toArrayList((List<ProcessDefinition>) session.findLatestProcessDefinitions());
             }
         });
     }
 
     public DocumentModel getDocumentModel(final TaskInstance ti,
             final NuxeoPrincipal user) throws NuxeoJbpmException {
         return (DocumentModel) executeJbpmOperation(new JbpmOperation() {
             public Serializable run(JbpmContext context)
                     throws NuxeoJbpmException {
                 context.setActorId(NuxeoPrincipal.PREFIX + user.getName());
                 TaskInstance sessionedTi = context.getTaskInstance(ti.getId());
                 String docId = (String) sessionedTi.getVariable(JbpmService.VariableName.documentId.name());
                 String repoId = (String) sessionedTi.getVariable(JbpmService.VariableName.documentRepositoryName.name());
                 return getDocumentModel(user, docId, repoId);
             }
         });
 
     }
 
     protected DocumentModel getDocumentModel(NuxeoPrincipal user, String docId,
             String repoId) throws NuxeoJbpmException {
         CoreSession session = getCoreSession(repoId, user);
         DocumentModel result;
         try {
             result = session.getDocument(new IdRef(docId));
         } catch (ClientException e) {
             throw new NuxeoJbpmException(e);
         } finally {
             closeCoreSession(session);
         }
         return result;
     }
 
     public DocumentModel getDocumentModel(final ProcessInstance pi,
             final NuxeoPrincipal user) throws NuxeoJbpmException {
         return (DocumentModel) executeJbpmOperation(new JbpmOperation() {
             public Serializable run(JbpmContext context)
                     throws NuxeoJbpmException {
                 ProcessInstance sessionedPi = context.getProcessInstance(pi.getId());
                 ContextInstance ci = sessionedPi.getContextInstance();
                 String docId = (String) ci.getVariable(JbpmService.VariableName.documentId.name());
                 String repoId = (String) ci.getVariable(JbpmService.VariableName.documentRepositoryName.name());
                 return getDocumentModel(user, docId, repoId);
             }
         });
     }
 
     @SuppressWarnings("unchecked")
     public List<ProcessInstance> getProcessInstances(final DocumentModel dm,
             final NuxeoPrincipal user, final JbpmListFilter jbpmListFilter)
             throws NuxeoJbpmException {
         return (List<ProcessInstance>) executeJbpmOperation(new JbpmOperation() {
             public ArrayList<ProcessInstance> run(JbpmContext context)
                     throws NuxeoJbpmException {
                 if (user != null) {
                     context.setActorId(NuxeoPrincipal.PREFIX + user.getName());
                 }
                 ArrayList<ProcessInstance> result = new ArrayList<ProcessInstance>();
                 Session session = context.getSession();
                 List<ProcessInstance> list = session.getNamedQuery(
                         JbpmService.HibernateQueries.NuxeoHibernateQueries_getProcessInstancesForDoc.name()).setParameter(
                         "docId", dm.getId()).setParameter("repoId",
                         dm.getRepositoryName()).list();
                 for (ProcessInstance pi : list) {
                     result.add(pi);
                 }
                 if (jbpmListFilter != null) {
                     result = jbpmListFilter.filter(context, dm, result, user);
                 }
                 return result;
             }
         });
     }
 
     @SuppressWarnings("unchecked")
     public List<TaskInstance> getTaskInstances(final DocumentModel dm,
             final NuxeoPrincipal user, final JbpmListFilter filter)
             throws NuxeoJbpmException {
         assert dm != null;
         return (List<TaskInstance>) executeJbpmOperation(new JbpmOperation() {
             public ArrayList<TaskInstance> run(JbpmContext context)
                     throws NuxeoJbpmException {
                 Set<TaskInstance> tisSet = new HashSet<TaskInstance>();
                 if (user != null) {
                     context.setActorId(NuxeoPrincipal.PREFIX + user.getName());
                     List<String> groups = user.getAllGroups();
                     List<String> prefixedActorsId = new ArrayList<String>();
                     for (String s : groups) {
                         prefixedActorsId.add(NuxeoGroup.PREFIX + s);
                     }
                     prefixedActorsId.add(NuxeoPrincipal.PREFIX + user.getName());
                     List<TaskInstance> tis = context.getTaskMgmtSession().findTaskInstances(
                             prefixedActorsId);
                     tis.addAll(context.getTaskMgmtSession().findPooledTaskInstances(
                             prefixedActorsId));
                     tisSet.addAll(tis);
                 } else {
                     List<TaskInstance> tis = context.getSession().createQuery(
                             FROM_ORG_JBPM_TASKMGMT_EXE_TASK_INSTANCE_TI_WHERE_TI_END_IS_NULL).list();
                     tisSet.addAll(tis);
                 }
                 List<Long> donePi = new ArrayList<Long>();
                 List<Long> useDocument = new ArrayList<Long>();
                 ArrayList<TaskInstance> result = new ArrayList<TaskInstance>();
                 // we need to look at the variables of the process instance of
                 // the task.
                 for (TaskInstance ti : tisSet) {
                     ProcessInstance pi = ti.getProcessInstance();
                     if (pi == null) {// task created outside a process
                         String docId = (String) ti.getVariable(JbpmService.VariableName.documentId.name());
                         String repoId = (String) ti.getVariable(JbpmService.VariableName.documentRepositoryName.name());
                         if (docId.equals(dm.getId())
                                 && repoId.equals(dm.getRepositoryName())) {
                             result.add(ti);
                         }
                     } else if (!donePi.contains(pi.getId())) {
                         String docId = (String) pi.getContextInstance().getVariable(
                                 JbpmService.VariableName.documentId.name());
                         String repoId = (String) pi.getContextInstance().getVariable(
                                 JbpmService.VariableName.documentRepositoryName.name());
                         donePi.add(pi.getId());
                         if (docId.equals(dm.getId())
                                 && repoId.equals(dm.getRepositoryName())) {
                             useDocument.add(pi.getId());
                         }
                         if (useDocument.contains(pi.getId())) {
                             result.add(ti);
                         }
                     }
                 }
                 if (filter != null) {
                     result = filter.filter(context, dm, result, user);
                 }
                 return result;
             }
         });
     }
 
     protected CoreSession getCoreSession(String repositoryName,
             NuxeoPrincipal principal) throws NuxeoJbpmException {
         Map<String, Serializable> context = new HashMap<String, Serializable>();
         context.put("principal", principal);
         try {
             return CoreInstance.getInstance().open(repositoryName, context);
         } catch (ClientException e) {
             throw new NuxeoJbpmException(e);
         }
     }
 
     protected void closeCoreSession(CoreSession session) {
         CoreInstance.getInstance().close(session);
     }
 
     public void endProcessInstance(final Long processId)
             throws NuxeoJbpmException {
         executeJbpmOperation(new JbpmOperation() {
             public Serializable run(JbpmContext context)
                     throws NuxeoJbpmException {
                 context.getProcessInstance(processId).end();
                 return null;
             }
         });
     }
 
     public void endTask(final Long taskInstanceId, final String transition,
             final Map<String, Serializable> variables,
             final Map<String, Serializable> transientVariables)
             throws NuxeoJbpmException {
         executeJbpmOperation(new JbpmOperation() {
             public Serializable run(JbpmContext context)
                     throws NuxeoJbpmException {
                 TaskInstance ti = context.getTaskInstance(taskInstanceId);
                 if (variables != null) {
                     ti.getProcessInstance().getContextInstance().addVariables(
                             variables);
                 }
                 if (transientVariables != null) {
                     ti.getProcessInstance().getContextInstance().setTransientVariables(
                             transientVariables);
                 }
                 String docId = (String) ti.getVariable(JbpmService.VariableName.documentId.name());
                 String repoId = (String) ti.getVariable(JbpmService.VariableName.documentRepositoryName.name());
                 DocumentModel dm = getDocumentModel(
                         getPrincipal(ti.getActorId()), docId, repoId);
                 if (dm != null) {
                     Transition t = ti.getToken().getNode().getLeavingTransition(
                             transition);
                     Boolean permission = getPermission(t,
                             JbpmSecurityPolicy.Action.execute, dm);
                     if (!permission) {
                         throw new NuxeoJbpmSecurityException();
                     }
                 }
                 if (transition == null || transition.equals("")) {
                     ti.end();
                 } else {
                     ti.end(transition);
                 }
                 return null;
             }
         });
     }
 
     protected NuxeoPrincipal getPrincipal(String user)
             throws NuxeoJbpmException {
         try {
             return getUserManager().getPrincipal(user);
         } catch (ClientException e) {
             throw new NuxeoJbpmException(e);
         }
     }
 
     protected UserManager getUserManager() {
         if (userManager == null) {
             try {
                 userManager = Framework.getService(UserManager.class);
             } catch (Exception e) {
                 throw new NuxeoJbpmRuntimeException(e);
             }
         }
         return userManager;
     }
 
     @SuppressWarnings("unchecked")
     public List<String> getAvailableTransitions(final Long taskInstanceId,
             final NuxeoPrincipal principal) throws NuxeoJbpmException {
         return (List<String>) executeJbpmOperation(new JbpmOperation() {
             public Serializable run(JbpmContext context)
                     throws NuxeoJbpmException {
                 context.setActorId(NuxeoPrincipal.PREFIX + principal.getName());
                 // jbpm code returns an array list.
                 return (Serializable) context.getTaskInstance(taskInstanceId).getAvailableTransitions();
             }
 
         });
     }
 
     public ProcessInstance getProcessInstance(final Long processInstanceId)
             throws NuxeoJbpmException {
         return (ProcessInstance) executeJbpmOperation(new JbpmOperation() {
             public Serializable run(JbpmContext context)
                     throws NuxeoJbpmException {
                 return context.getProcessInstance(processInstanceId);
             }
         });
     }
 
     @SuppressWarnings("unchecked")
     public List<TaskInstance> getTaskInstances(final Long processInstanceId,
             final NuxeoPrincipal principal) throws NuxeoJbpmException {
         return (List<TaskInstance>) executeJbpmOperation(new JbpmOperation() {
             public Serializable run(JbpmContext context)
                     throws NuxeoJbpmException {
                 context.setActorId(NuxeoPrincipal.PREFIX + principal.getName());
                 Collection<TaskInstance> tis = context.getProcessInstance(
                         processInstanceId).getTaskMgmtInstance().getTaskInstances();
                 ArrayList<TaskInstance> result = new ArrayList<TaskInstance>();
                 for (TaskInstance ti : tis) {
                     result.add(ti);
                 }
                 return result;
             }
         });
     }
 
     public void saveTaskInstances(final List<TaskInstance> taskInstances)
             throws NuxeoJbpmException {
         executeJbpmOperation(new JbpmOperation() {
             public Serializable run(JbpmContext context)
                     throws NuxeoJbpmException {
                 Session session = context.getSession();
                 for (TaskInstance ti : taskInstances) {
                     session.saveOrUpdate(ti);
                 }
                 return null;
             }
 
         });
     }
 
     protected void addSecurityPolicy(JbpmSecurityPolicy securityPolicy) {
         this.securityPolicies.add(securityPolicy);
     }
 
     @SuppressWarnings("unchecked")
     public List<ProcessDefinition> getProcessDefinitions(
             final NuxeoPrincipal user, final DocumentModel dm,
             final JbpmListFilter filter) throws NuxeoJbpmException {
         return (List<ProcessDefinition>) executeJbpmOperation(new JbpmOperation() {
             public ArrayList<ProcessDefinition> run(JbpmContext context)
                     throws NuxeoJbpmException {
                 context.setActorId(NuxeoPrincipal.PREFIX + user.getName());
                 List<ProcessDefinition> pds = context.getGraphSession().findLatestProcessDefinitions();
                 ArrayList<ProcessDefinition> result = new ArrayList<ProcessDefinition>();
                 for (ProcessDefinition pd : pds) {
                     Boolean perm = getPermission(pd,
                             JbpmSecurityPolicy.Action.read, dm);
                     if (perm == null || perm) {
                         result.add(pd);
                     }
                 }
                 if (filter != null) {
                     result = filter.filter(context, dm, result, user);
                 }
                 return result;
             }
         });
     }
 
     protected Boolean getPermission(ProcessDefinition pd,
             JbpmSecurityPolicy.Action action, DocumentModel dm) {
         for (JbpmSecurityPolicy pm : securityPolicies) {
             Boolean perm = pm.checkPermission(pd, action, dm);
             if (perm != null) {
                 return perm;
             }
 
         }
         return null;
     }
 
     protected Boolean getPermission(Transition t,
             JbpmSecurityPolicy.Action action, DocumentModel dm) {
         for (JbpmSecurityPolicy pm : securityPolicies) {
             Boolean perm = pm.checkPermission(t, action, dm);
             if (perm != null) {
                 return perm;
             }
         }
         return Boolean.TRUE;
     }
 
     public void persistProcessInstance(final ProcessInstance pi)
             throws NuxeoJbpmException {
         executeJbpmOperation(new JbpmOperation() {
             public Serializable run(JbpmContext context)
                     throws NuxeoJbpmException {
                 Session session = context.getSession();
                 session.update(pi);
                 return null;
             }
 
         });
     }
 
     public Map<String, String[]> getTypeFilterConfiguration() {
         return typeFilters;
     }
 
     protected void setTypeFilters(Map<String, String[]> typeFilters) {
         this.typeFilters = typeFilters;
     }
 
     public void abandonProcessInstance(final NuxeoPrincipal principal,
             final Long processId) throws NuxeoJbpmException {
         executeJbpmOperation(new JbpmOperation() {
             @SuppressWarnings("unchecked")
             public Serializable run(JbpmContext context)
                     throws NuxeoJbpmException {
                 ProcessInstance pi = context.getProcessInstance(processId);
                List<TaskInstance> tis = (List<TaskInstance>) pi.getTaskMgmtInstance().getTaskInstances();
                 for (TaskInstance ti : tis) {
                     if (!ti.hasEnded()) {
                        ti.cancel();
                     }
                 }
                 pi.end();
                 return null;
             }
         });
     }
 }
