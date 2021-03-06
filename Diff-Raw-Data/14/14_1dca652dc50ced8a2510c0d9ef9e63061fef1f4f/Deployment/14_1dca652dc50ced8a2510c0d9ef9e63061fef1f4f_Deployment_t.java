 /* 
  * JBoss, Home of Professional Open Source 
  * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
  * as indicated by the @author tags. All rights reserved. 
  * See the copyright.txt in the distribution for a 
  * full listing of individual contributors.
  *
  * This copyrighted material is made available to anyone wishing to use, 
  * modify, copy, or redistribute it subject to the terms and conditions 
  * of the GNU Lesser General Public License, v. 2.1. 
  * This program is distributed in the hope that it will be useful, but WITHOUT A 
  * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
  * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details. 
  * You should have received a copy of the GNU Lesser General Public License, 
  * v.2.1 along with this distribution; if not, write to the Free Software 
  * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, 
  * MA  02110-1301, USA.
  */
 
 package org.switchyard.deploy.internal;
 
 import java.io.IOException;
 import java.io.InputStream;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 import java.util.ServiceLoader;
 
 import javax.xml.namespace.QName;
 
 import org.apache.log4j.Logger;
 import org.switchyard.ExchangeHandler;
 import org.switchyard.ServiceDomain;
 import org.switchyard.ServiceReference;
 import org.switchyard.common.type.Classes;
 import org.switchyard.config.model.ModelPuller;
 import org.switchyard.config.model.composite.BindingModel;
 import org.switchyard.config.model.composite.ComponentModel;
 import org.switchyard.config.model.composite.ComponentReferenceModel;
 import org.switchyard.config.model.composite.ComponentServiceModel;
 import org.switchyard.config.model.composite.CompositeReferenceModel;
 import org.switchyard.config.model.composite.CompositeServiceModel;
 import org.switchyard.config.model.composite.InterfaceModel;
 import org.switchyard.config.model.switchyard.SwitchYardModel;
 import org.switchyard.config.model.transform.TransformsModel;
 import org.switchyard.deploy.Activator;
 import org.switchyard.exception.SwitchYardException;
 import org.switchyard.extensions.wsdl.WSDLReaderException;
 import org.switchyard.extensions.wsdl.WSDLService;
 import org.switchyard.metadata.ServiceInterface;
 import org.switchyard.metadata.java.JavaService;
 
 /**
  * Deployment is a framework-independent representation of a deployed SwitchYard 
  * application.  At this point, a deployment is 1:1 with a SwitchYard domain, but
  * this will change to allow multiple deployments to participate in a single, 
  * shared domain.
  * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
  */
 public class Deployment extends AbstractDeployment {
     /**
      * Interface type used by a Java interface, e.g. "interface.java"
      */
     private static final String JAVA_INTERFACE = "java";
 
     /**
      * Interface type used by a WSDL interface, e.g. "interface.wsdl"
      */
     private static final String WSDL_INTERFACE = "wsdl";
 
     private static Logger _log = Logger.getLogger(Deployment.class);
 
     private Map<String, Activator> _activators = new HashMap<String, Activator>();
     private List<Activation> _services = new LinkedList<Activation>();
     private List<Activation> _serviceBindings = new LinkedList<Activation>();
     private List<Activation> _references = new LinkedList<Activation>();
     private List<Activation> _referenceBindings = new LinkedList<Activation>();
 
     /**
      * Create a new instance of Deployer from a configuration stream.
      * @param configStream stream containing switchyard config
      * @throws IOException Error reading configuration model.
      */
     public Deployment(InputStream configStream) throws IOException {
         super(new ModelPuller<SwitchYardModel>().pull(configStream));
     }
     
     /**
      * Create a new instance of Deployer from a configuration model.
      * @param configModel switchyard config model
      */
     public Deployment(SwitchYardModel configModel) {
         super(configModel);
     }
     
     /**
      * Initialize the deployment.
      * @param appServiceDomain The ServiceDomain for the application.
      */
     public void init(ServiceDomain appServiceDomain) {
         super.init(appServiceDomain);
         _log.debug("Initializing deployment for application " + getConfig().getName());
         // create a new domain and load transformer and activator instances for lifecycle
         registerTransformers();
         createActivators();
     }
     
     /**
      * Starts the deployment.  All services are registered and the appropriate 
      * activators are triggered.
      */
     public void start() {
         _log.debug("Starting deployment " + getConfig().getName());
         // ordered startup lifecycle
         try {
             deployReferenceBindings();
             deployServices();
             deployReferences();
             deployServiceBindings();
             deployAutoRegisteredTransformers();
         } catch (RuntimeException e1) {
             // Undo partial deployment...
             _log.debug("Undeploying partially deployed artifacts of failed deployment " + getConfig().getName());
             try {
                 stop();
             } catch (RuntimeException e2) {
                 // Nothing we can do...
                 _log.debug("Failed to properly undeploy a partial/failed deployment " +  getConfig().getName(), e2);
             }
             // Rethrow the exception...
             throw e1;
         }
     }
 
     /**
      * Stops the deployment.  All services are unregistered and the appropriate
      * activators are triggered.
      */
     public void stop() {
         _log.debug("Stopping deployment " + getConfig().getName());
         undeployServiceBindings();
         undeployServices();
         undeployReferences();
         undeployReferenceBindings();
         undeployAutoRegisteredTransformers();
     }
 
     /**
      * Tear everything down.
      */
     public void destroy() {
         _log.debug("Destroying deployment " + getConfig().getName());
         
         destroyDomain();
         
         // Clean up our list of activations, just in case something's left
         _services.clear();
         _serviceBindings.clear();
         _references.clear();
         _referenceBindings.clear();
 
         getTransformerRegistryLoader().unregisterTransformers();
     }
     
     /**
      * Find the deployment activator for the specified type.
      * 
      * @param type The activator component type.
      * @return Activator the Activator instance, or null if no activator exists for the specified type.
      * @throws SwitchYardException if the component definition does not contain
      * an implementation type
      */
     public Activator findActivator(String type) throws SwitchYardException {
         if (_activators.containsKey(type)) {
             return _activators.get(type);
         } else {
             throw new SwitchYardException("Activator not found for type: " + type);
         }
     }
     
     /**
      * Finds an activator that handles the component implementation type.
      * @throws SwitchYardException if the component definition does not contain
      * an implementation type
      */
     private Activator findActivator(ComponentModel component) throws SwitchYardException {
         if (component.getImplementation() == null) {
            throw new SwitchYardException("Component defintion " + component.getName() 
                    + " does not included an implementation definition.");
         }
         return findActivator(component.getImplementation().getType());
     }
     
     private void createActivators() {
         ServiceDomain serviceDomain = getDomain();
         ServiceLoader<Activator> activatorLoader = ServiceLoader.load(Activator.class);
         for (Activator activator : activatorLoader) {
             activator.setServiceDomain(serviceDomain);
             Collection<String> activationTypes = activator.getActivationTypes();
             if (activationTypes != null) {
                 for (String type : activationTypes) {
                    _log.debug("Registered activation type " + type
                            + " for activator " + activator.getClass());
                     _activators.put(type, activator);
                 }
             }
         }
     }
 
     private void registerTransformers() {
         _log.debug("Registering configured Transformers ...");
         TransformsModel transforms = getConfig().getTransforms();
         getTransformerRegistryLoader().registerTransformers(transforms);
     }
 
     private void deployReferenceBindings() {
         _log.debug("Deploying reference bindings ...");
         // activate bindings for each service
         for (CompositeReferenceModel reference : getConfig().getComposite().getReferences()) {
             for (BindingModel binding : reference.getBindings()) {
                 QName refQName = reference.getQName();
                 _log.debug("Deploying binding " + binding.getType() + " for reference " + reference.getName());
                 
                 Activator activator = findActivator(binding.getType());
                 ExchangeHandler handler = activator.init(refQName, reference);
                 
                 ServiceInterface si = getComponentReferenceInterface(reference.getComponentReference());
                 ServiceReference serviceRef = si != null
                         ? getDomain().registerService(refQName, handler, si)
                         : getDomain().registerService(refQName, handler);
                         
                 Activation activation = new Activation(serviceRef, activator);
                 activation.start();
                 
                 _referenceBindings.add(activation);
             }
         }
     }
     
     private ServiceInterface getComponentReferenceInterface(ComponentReferenceModel reference) {
         ServiceInterface serviceInterface = null;
         
         if (reference != null && reference.getInterface() != null) {
             serviceInterface = loadServiceInterface(reference.getInterface());
         }
         return serviceInterface;
     }
     
     private ServiceInterface getComponentServiceInterface(ComponentServiceModel service) {
         ServiceInterface serviceInterface = null;
         
         if (service != null && service.getInterface() != null) {
             serviceInterface = loadServiceInterface(service.getInterface());
         }
         return serviceInterface;
     }
     
     private ServiceInterface loadServiceInterface(InterfaceModel intfModel) {
         ServiceInterface serviceInterface = null;
         
         if (intfModel != null) {
             if (isJavaInterface(intfModel.getType())) {
                 serviceInterface = JavaService.fromClass(
                         loadClass(intfModel.getInterface()));
             } else if (intfModel.getType().equals(WSDL_INTERFACE)) {
                 try {
                     serviceInterface = WSDLService.fromWSDL(intfModel.getInterface());
                 } catch (WSDLReaderException wsdlre) {
                     throw new SwitchYardException(wsdlre);
                 }
             }
         }
         
         return serviceInterface;
     }
     
     private boolean isJavaInterface(final String type) {
         return type.equals(JAVA_INTERFACE);
     }
 
     private void deployServices() {
         _log.debug("Deploying services ...");
         // deploy services to each implementation found in the application
         for (ComponentModel component : getConfig().getComposite().getComponents()) {
             Activator activator = findActivator(component);
             if (activator == null) {
                throw new SwitchYardException("Activator not found for "
                        + component.getImplementation().getType());
             }
             // register a service for each one declared in the component
             for (ComponentServiceModel service : component.getServices()) {
                 _log.debug("Registering service " + service.getName()  
                        + " for component " + component.getImplementation().getType());
                 ExchangeHandler handler = activator.init(service.getQName(), service);
                 ServiceInterface serviceIntf = getComponentServiceInterface(service);
                 ServiceReference serviceRef = serviceIntf != null
                         ? getDomain().registerService(service.getQName(), handler, serviceIntf)
                         : getDomain().registerService(service.getQName(), handler);
                         
                 Activation activation = new Activation(serviceRef, activator);
                 activation.start();
                 _services.add(activation);
             }
         }
         
     }
     
     private void deployReferences() {
         _log.debug("Deploying references ...");
         for (ComponentModel component : getConfig().getComposite().getComponents()) {
             Activator activator = findActivator(component);
             // register a service for each one declared in the component
             for (ComponentReferenceModel reference : component.getReferences()) {
                 _log.debug("Registering reference " + reference.getName()  
                        + " for component " + component.getImplementation().getType());
                 ServiceReference service = getDomain().getService(reference.getQName());
                 if (service == null) {
                     throw new SwitchYardException("Unable to activate reference, service not found: " 
                             + reference.getQName());
                 }
                 activator.init(reference.getQName(), reference);
                 Activation activation = new Activation(service, activator);
                 activation.start();
                 _references.add(activation);
             }
         }
     }
 
     private void deployServiceBindings() {
         _log.debug("Deploying service bindings ...");
         // activate bindings for each service
         for (CompositeServiceModel service : getConfig().getComposite().getServices()) {
             for (BindingModel binding : service.getBindings()) {
                 _log.debug("Deploying binding " + binding.getType() + " for service " + service.getName());
                 Activator activator = findActivator(binding.getType());
                 ServiceReference serviceRef = getDomain().getService(service.getQName());
                 if (serviceRef == null) {
                     throw new SwitchYardException("Unable to activate binding, service not found: " 
                             + service.getQName());
                 }
                 activator.init(serviceRef.getName(), service);
                 Activation activation = new Activation(serviceRef, activator);
                 activation.start();
                 _serviceBindings.add(activation);
             }
         }
     }
 
     private void undeployServiceBindings() {
         _log.debug("Undeploying reference bindings ...");
        for (Activation activation : _serviceBindings) {
            activation.stop();
            activation.destroy();
        }
     }
 
     private void undeployServices() {
         _log.debug("Undeploying services ...");
         for (Activation activation : _services) {
             activation.stop();
             activation.destroy();
         }
     }
 
     private void undeployReferences() {
         _log.debug("Undeploying references ...");
         for (Activation activation : _references) {
             activation.stop();
             activation.destroy();
         }
     }
 
     private void undeployReferenceBindings() {
         _log.debug("Undeploying reference bindings ...");
         for (Activation activation : _referenceBindings) {
             activation.stop();
             activation.destroy();
         }
     }
 
     private void deployAutoRegisteredTransformers() {
         deployAutoRegisteredTransformers(_services);
         deployAutoRegisteredTransformers(_references);
     }
 
     private void deployAutoRegisteredTransformers(List<Activation> activationList) {
         for (Activation activation : activationList) {
             ServiceInterface serviceInterface = activation.getService().getInterface();
             deployAutoRegisteredTransformers(serviceInterface);
         }
     }
 
     private void destroyDomain() {
 
     }
 
     private Class<?> loadClass(String className) {
         return Classes.forName(className, getClass());
     }
 
 }
 
 class Activation {
     private ServiceReference _service;
     private Activator _activator;
     
     Activation(ServiceReference service, Activator activator) {
         _service = service;
         _activator = activator;
     }
     
     void start() {
         _activator.start(_service);
     }
     
     void stop() {
         _activator.stop(_service);
     }
     
     void destroy() {
         _activator.destroy(_service);
     }
     
     ServiceReference getService() {
         return _service;
     }
     
     Activator getActivator() {
         return _activator;
     }
 }
