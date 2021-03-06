 /**
  * Redistribution and use of this software and associated documentation
  * ("Software"), with or without modification, are permitted provided
  * that the following conditions are met:
  *
  * 1. Redistributions of source code must retain copyright
  *    statements and notices.  Redistributions must also contain a
  *    copy of this document.
  *
  * 2. Redistributions in binary form must reproduce the
  *    above copyright notice, this list of conditions and the
  *    following disclaimer in the documentation and/or other
  *    materials provided with the distribution.
  *
  * 3. The name "Exolab" must not be used to endorse or promote
  *    products derived from this Software without prior written
  *    permission of Exoffice Technologies.  For written permission,
  *    please contact info@exolab.org.
  *
  * 4. Products derived from this Software may not be called "Exolab"
  *    nor may "Exolab" appear in their names without prior written
  *    permission of Exoffice Technologies. Exolab is a registered
  *    trademark of Exoffice Technologies.
  *
  * 5. Due credit should be given to the Exolab Project
  *    (http://www.exolab.org/).
  *
  * THIS SOFTWARE IS PROVIDED BY EXOFFICE TECHNOLOGIES AND CONTRIBUTORS
  * ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT
  * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
  * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
  * EXOFFICE TECHNOLOGIES OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
  * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
  * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
  * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
  * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
  * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
  * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
  * OF THE POSSIBILITY OF SUCH DAMAGE.
  *
  * Copyright 1999 (C) Exoffice Technologies Inc. All Rights Reserved.
  *
  * $Id$
  */
 
 
 package org.openejb.core.stateless;
 
 
 import java.io.IOException;
 import java.io.Serializable;
 import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Method;
 import java.rmi.RemoteException;
 import java.security.Principal;
 import java.util.HashMap;
 import java.util.Properties;
 import javax.ejb.EJBException;
 import javax.ejb.EJBHome;
 import javax.ejb.EJBMetaData;
 import javax.ejb.EJBObject;
 import javax.ejb.EnterpriseBean;
 import javax.ejb.Handle;
 import javax.ejb.HomeHandle;
 import javax.ejb.SessionBean;
 import javax.transaction.Status;
 import javax.transaction.Status;
 import javax.transaction.Transaction;
 import javax.transaction.Transaction;
 import javax.transaction.TransactionManager;
 import javax.transaction.TransactionRequiredException;
 import javax.transaction.UserTransaction;
 import org.openejb.Container;
 import org.openejb.DeploymentInfo;
 import org.openejb.InvalidateReferenceException;
 import org.openejb.OpenEJB;
 import org.openejb.OpenEJBException;
 import org.openejb.ProxyInfo;
 import org.openejb.SystemException;
 import org.openejb.core.EnvProps;
 import org.openejb.core.Operations;
 import org.openejb.core.ThreadContext;
 import org.openejb.core.transaction.TransactionPolicy;
 import org.openejb.core.transaction.TransactionContext;
 import org.openejb.core.transaction.TransactionContainer;
 import org.openejb.util.SafeProperties;
 import org.openejb.util.SafeToolkit;
 
 /**
  * Stateless SessionBean container
  * 
  * @author <a href="mailto:Richard@Monson-Haefel.com">Richard Monson-Haefel</a>
  * @author <a href="mailto:david.blevins@visi.com">David Blevins</a>
  * @version $Revision$ $Date$
  */
 public class StatelessContainer implements org.openejb.RpcContainer, TransactionContainer{
 
     /** managed bean instances; passivation and cache */
     StatelessInstanceManager instanceManager;
 
     /** contains deployment information for each by deployed to this container */
     HashMap deploymentRegistry;
 
     /** the server unique id for this container */
     Object containerID = null;
 
 
     /** manages the transactional scope according to the bean's transaction attributes */
     //StatelessTransactionScopeHandler txScopeHandle;
 
 
     /**
      * Construct this container with the specified container id, deployments, container manager and properties.
      * The properties can include the class name of the preferred InstanceManager, org.openejb.core.entity.EntityInstanceManager
      * is the default. The properties should also include the properties for the instance manager.
      *
      * @param id the unique id to identify this container in the ContainerSystem
      * @param registry a hashMap of bean delpoyments that this container will be responsible for
      * @param mngr the ContainerManager for this container
      * @param properties the properties this container needs to initialize and run
      * @throws OpenEJBException if there is a problem constructing the container
      * @see org.openejb.Container
      */
     public void init(Object id, HashMap registry, Properties properties)
     throws org.openejb.OpenEJBException{
         containerID = id;
         deploymentRegistry = registry;
 
         if(properties == null)properties = new Properties();
 
 
         SafeToolkit toolkit = SafeToolkit.getToolkit("StatelessContainer");
         SafeProperties safeProps = toolkit.getSafeProperties(properties);
         try{
         String className = safeProps.getProperty(EnvProps.IM_CLASS_NAME, "org.openejb.core.stateless.StatelessInstanceManager");
         instanceManager =(StatelessInstanceManager)Class.forName(className).newInstance();
         }catch(Exception e){
         throw new org.openejb.SystemException("Initialization of InstanceManager for the \""+containerID+"\" stateful container failed",e);
         }
         instanceManager.init(properties);
 
 
 //        txScopeHandle = new StatelessTransactionScopeHandler(this,instanceManager);
 
         /*
         * This block of code is necessary to avoid a chicken and egg problem. The DeploymentInfo
         * objects must have a reference to their container during this assembly process, but the
         * container is created after the DeploymentInfo necessitating this loop to assign all
         * deployment info object's their containers.
         */
         org.openejb.DeploymentInfo [] deploys = this.deployments();
         for(int x = 0; x < deploys.length; x++){
             org.openejb.core.DeploymentInfo di = (org.openejb.core.DeploymentInfo)deploys[x];
             di.setContainer(this);
         }
 
     }
 
     //===============================
     // begin Container Implementation
     //
 
     /**
      * Gets the <code>DeploymentInfo</code> objects for all the beans deployed in this container.
      *
      * @return an array of DeploymentInfo objects
      * @see org.openejb.DeploymentInfo
      * @see org.openejb.ContainerSystem#deployments() ContainerSystem.deployments()
      */
     public DeploymentInfo [] deployments(){
         return (DeploymentInfo [])deploymentRegistry.values().toArray(new DeploymentInfo[deploymentRegistry.size()]);
     }
 
     /**
      * Gets the <code>DeploymentInfo</code> object for the bean with the specified deployment id.
      *
      * @param id the deployment id of the deployed bean.
      * @return the DeploymentInfo object associated with the bean.
      * @see org.openejb.DeploymentInfo
      * @see org.openejb.ContainerSystem#getDeploymentInfo(Object) ContainerSystem.getDeploymentInfo
      * @see org.openejb.DeploymentInfo#getDeploymentID()
      */
     public DeploymentInfo getDeploymentInfo(Object deploymentID){
         return (DeploymentInfo)deploymentRegistry.get(deploymentID);
     }
     /**
      * Gets the type of container (STATELESS, STATEFUL, ENTITY, or MESSAGE_DRIVEN
      *
      * @return id type bean container
      */
     public int getContainerType( ){
         return Container.STATELESS;
     }
 
     /**
      * Gets the id of this container.
      *
      * @return the id of this container.
      * @see org.openejb.DeploymentInfo#getContainerID() DeploymentInfo.getContainerID()
      */
     public Object getContainerID(){
         return containerID;
     }
 
     /**
      * Adds a bean to this container.
      * @param deploymentId the deployment id of the bean to deploy.
      * @param info the DeploymentInfo object associated with the bean.
      * @throws org.openejb.OpenEJBException
      *      Occurs when the container is not able to deploy the bean for some
      *      reason.
      */
     public void deploy(Object deploymentID, DeploymentInfo info) throws OpenEJBException {
         HashMap registry = (HashMap)deploymentRegistry.clone();
         registry.put(deploymentID, info);
         deploymentRegistry = registry;
     }
 
     /**
      * Invokes a method on an instance of the specified bean deployment.
      *
      * @param deployID the dployment id of the bean deployment
      * @param callMethod the method to be called on the bean instance
      * @param args the arguments to use when invoking the specified method
      * @param primKey the primary key class of the bean or null if the bean does not need a primary key
      * @param prncpl
      * @return the result of invoking the specified method on the bean instance
      * @throws org.openejb.OpenEJBException
      * @see org.openejb.Container#invoke Container.invoke
      * @see org.openejb.core.stateful.StatefulContainer#invoke StatefulContainer.invoke
      */
     public Object invoke(Object deployID, Method callMethod,Object [] args,Object primKey, Object securityIdentity)    throws org.openejb.OpenEJBException{
         try{
 
         org.openejb.core.DeploymentInfo deployInfo = (org.openejb.core.DeploymentInfo)this.getDeploymentInfo(deployID);
 
         ThreadContext callContext = ThreadContext.getThreadContext();
         callContext.set(deployInfo, primKey, securityIdentity);
 
         // check authorization to invoke
 
         boolean authorized = OpenEJB.getSecurityService().isCallerAuthorized(securityIdentity, deployInfo.getAuthorizedRoles(callMethod));
         if(!authorized)
             throw new org.openejb.ApplicationException(new RemoteException("Unauthorized Access by Principal Denied"));
 
         // use special methods for remove and create requests
         if(EJBHome.class.isAssignableFrom(callMethod.getDeclaringClass())){
             if(callMethod.getName().equals("create")){
                 return createEJBObject(deployInfo);
             }else
                 return null;// EJBHome.remove( ) and other EJBHome methods are not process by the container
         }else if(EJBObject.class == callMethod.getDeclaringClass() ){
             return null;// EJBObject.remove( ) and other EJBObject methods are not process by the container
         }
 
 
         SessionBean bean = null;
 
         // retreive instance from instance manager
         bean = (SessionBean)instanceManager.getInstance(callContext);
 
         // allows the container to know the context of the call to check allowed operations
         callContext.setCurrentOperation(Operations.OP_BUSINESS);
 
         // converts a remote interface Method object into its corresponding bean class Method.
         Method runMethod = deployInfo.getMatchingBeanMethod(callMethod);
 
         Object retValue = invoke(callMethod, runMethod, args, bean, callContext) ;
         instanceManager.poolInstance(callContext,bean);
 
         // see comments in org.openejb.core.DeploymentInfo.
         return deployInfo.convertIfLocalReference(callMethod, retValue);
 
         }finally{
             /*
                 The thread context must be stripped from the thread before returning or throwing an exception
                 so that an object outside the container does not have access to a
                 bean's JNDI ENC.  In addition, its important for the
                 org.openejb.core.ivm.java.javaURLContextFactory, which determines the context
                 of a JNDI lookup based on the presence of a ThreadContext object.  If no ThreadContext
                 object is available, then the request is assumed to be made from outside the container
                 system and is given the global OpenEJB JNDI name space instead.  If there is a thread context,
                 then the request is assumed to be made from within the container system and so the
                 javaContextFactory must return the JNDI ENC of the current enterprise bean which it
                 obtains from the DeploymentInfo object associated with the current thread context.
             */
             ThreadContext.setThreadContext(null);
         }
     }
     //
     // end ContainerManager Implementation
     //====================================
 
 
     //============================================
     // begin methods unique to this implementation
     //
 
     public StatelessInstanceManager getInstanceManager( ){
         return instanceManager;
     }
 
     protected Object invoke(Method callMethod, Method runMethod, Object [] args, EnterpriseBean bean, ThreadContext callContext)
     throws org.openejb.OpenEJBException{
 
         TransactionPolicy txPolicy   = callContext.getDeploymentInfo().getTransactionPolicy( callMethod );
         TransactionContext txContext = new TransactionContext();
         txContext.callContext = callContext;
 
         //OLD:Transaction originalTx = txScopeHandle.beforeInvoke(callMethod, bean, callContext);
         txPolicy.beforeInvoke( bean, txContext );
 
         // txScopeHandler.afterInvoke( ) peformed in the finally clause
 
         Object returnValue = null;
         try{
             // using Java reflection, invoke the method on the bean instance.
             returnValue = runMethod.invoke(bean, args);
         }catch(java.lang.reflect.InvocationTargetException ite){// handle exceptions thrown by enterprise bean
             if ( ite.getTargetException() instanceof RuntimeException ) {
                 /* System Exception ****************************/
                 //OLD:txScopeHandle.handleSystemException(callMethod,bean,callContext, originalTx,ite.getTargetException());
                 txPolicy.handleSystemException( ite.getTargetException(), bean, txContext );
             } else {
                 /* Application Exception ***********************/
                 instanceManager.poolInstance(callContext,bean);
                 //OLD:txScopeHandle.handleApplicationException(callMethod,bean,callContext, originalTx,ite.getTargetException());
                 txPolicy.handleApplicationException( ite.getTargetException(), txContext );
             }
         }catch(Throwable re){// handle reflection exception
             /*
               Any exception thrown by reflection; not by the enterprise bean. Possible
               Exceptions are:
                 IllegalAccessException - if the underlying method is inaccessible.
                 IllegalArgumentException - if the number of actual and formal parameters differ, or if an unwrapping conversion fails.
                 NullPointerException - if the specified object is null and the method is an instance method.
                 ExceptionInInitializerError - if the initialization provoked by this method fails.
             */
             txPolicy.handleSystemException( re, bean, txContext );
         }finally{
             //OLD:txScopeHandle.afterInvoke(callMethod, bean,callContext, originalTx);
             txPolicy.afterInvoke( bean, txContext );
         }
 
         return returnValue;
     }
 
     /**
     * This method creates a ProxyInfo object for a specific deployment.  When a stateless
     * session bean is created, the container does not need to create an bean instance
     * because instances are shared and pooled and only delegated to service a request when a call is
     * received from the client.  The ProxyInfo object will allow the server to construct an
     * appropriate remote reference that the client can use to make calls.
     */
     protected ProxyInfo createEJBObject(org.openejb.core.DeploymentInfo deploymentInfo) {
         return new ProxyInfo(deploymentInfo, null, deploymentInfo.getRemoteInterface(), this);
     }
 
     //
     // end methods unique to this implementation
     //================================================
 
     public void discardInstance(EnterpriseBean instance, ThreadContext context) {
        instanceManager.freeInstance( context, instance );    
     }
 
 }
