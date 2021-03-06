 /*
  * Copyright 2006-2008 Web Cohesion
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *   http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package org.codehaus.enunciate.apt;
 
 import com.sun.mirror.apt.AnnotationProcessorEnvironment;
 import com.sun.mirror.apt.Messager;
 import com.sun.mirror.declaration.*;
 import com.sun.mirror.type.AnnotationType;
 import com.sun.mirror.type.ClassType;
 import freemarker.template.TemplateException;
 import freemarker.template.TemplateModelException;
 import net.sf.jelly.apt.Context;
 import net.sf.jelly.apt.decorations.TypeMirrorDecorator;
 import net.sf.jelly.apt.decorations.type.DecoratedTypeMirror;
 import net.sf.jelly.apt.freemarker.FreemarkerModel;
 import net.sf.jelly.apt.freemarker.FreemarkerProcessor;
 import net.sf.jelly.apt.freemarker.FreemarkerTransform;
 import org.codehaus.enunciate.EnunciateException;
 import org.codehaus.enunciate.XmlTransient;
 import org.codehaus.enunciate.config.EnunciateConfiguration;
 import org.codehaus.enunciate.contract.jaxb.Registry;
 import org.codehaus.enunciate.contract.jaxb.RootElementDeclaration;
 import org.codehaus.enunciate.contract.jaxb.TypeDefinition;
 import org.codehaus.enunciate.contract.jaxrs.RootResource;
 import org.codehaus.enunciate.contract.jaxws.EndpointImplementation;
 import org.codehaus.enunciate.contract.jaxws.EndpointInterface;
 import org.codehaus.enunciate.contract.json.JsonRootElementDeclaration;
 import org.codehaus.enunciate.contract.json.JsonTypeDefinition;
 import org.codehaus.enunciate.contract.rest.RESTEndpoint;
 import org.codehaus.enunciate.contract.validation.*;
 import org.codehaus.enunciate.json.JsonRootType;
 import org.codehaus.enunciate.json.JsonType;
 import org.codehaus.enunciate.main.Enunciate;
 import org.codehaus.enunciate.modules.DeploymentModule;
 import org.codehaus.enunciate.rest.annotations.ContentTypeHandler;
 import org.codehaus.enunciate.template.freemarker.*;
 import org.codehaus.enunciate.util.AntPatternMatcher;
 
 import javax.jws.WebService;
 import javax.ws.rs.HttpMethod;
 import javax.ws.rs.Path;
 import javax.ws.rs.ext.Provider;
 import javax.xml.bind.annotation.XmlRegistry;
 import javax.xml.bind.annotation.XmlRootElement;
 import java.io.IOException;
 import java.io.PrintWriter;
 import java.io.StringWriter;
 import java.util.*;
 
 /**
  * Root annotation processor for enunciate.  Initializes the model and signals the modules to generate.
  * <p/>
  * Even though it extends <code>FreemarkerProcessor</code>, it does not process any Freemarker templates directly.  It extends
  * <code>FreemarkerProcessor</code> only to inherit certain functionality.
  *
  * @author Ryan Heaton
  */
 public class EnunciateAnnotationProcessor extends FreemarkerProcessor {
 
   private EnunciateException ee = null;
   private IOException ioe = null;
   private RuntimeException re = null;
   private final Enunciate enunciate;
   private final String[] additionalApiClasses;
 
   /**
    * Package-private constructor for testing purposes.
    */
   EnunciateAnnotationProcessor() throws EnunciateException {
     this(new Enunciate(new String[0], new EnunciateConfiguration()));
   }
 
   public EnunciateAnnotationProcessor(Enunciate enunciate, String... additionalApiClasses) throws EnunciateException {
     super(null);
 
     if (enunciate == null) {
       throw new EnunciateException("An enunciate mechanism must be specified.");
     }
     else if (enunciate.getConfig() == null) {
       throw new EnunciateException("An enunciate mechanism must have a configuration (even if its the default configuration).");
     }
 
     this.enunciate = enunciate;
     this.additionalApiClasses = additionalApiClasses;
   }
 
   @Override
   public void process() {
     try {
       EnunciateFreemarkerModel model = getRootModel();
 
       EnunciateConfiguration config = this.enunciate.getConfig();
       for (DeploymentModule module : config.getAllModules()) {
         if (module instanceof EnunciateModelAware) {
           ((EnunciateModelAware) module).initModel(model);
         }
       }
 
       for (DeploymentModule module : config.getEnabledModules()) {
         debug("Invoking %s step for module %s", Enunciate.Target.GENERATE, module.getName());
         module.step(Enunciate.Target.GENERATE);
       }
     }
     catch (TemplateException e) {
       process(e);
     }
     catch (IOException e) {
       process(e);
     }
     catch (EnunciateException e) {
       process(e);
     }
     catch (RuntimeException re) {
       process(re);
     }
   }
 
   /**
    * Getting the root model pulls all endpoint interfaces and schema types out of the source
    * base, adds the classes specified to be included, and adds them to the model, then validates
    * the model.
    *
    * @return The root model.
    */
   @Override
   protected EnunciateFreemarkerModel getRootModel() throws TemplateModelException {
     EnunciateConfiguration config = this.enunciate.getConfig();
     EnunciateFreemarkerModel model = (EnunciateFreemarkerModel) super.getRootModel();
     model.setEnunciateConfig(config);
 
     //build up the list of all classes to which we are going to apply enunciate.
     TypeDeclaration[] additionalApiDefinitions = loadAdditionalApiDefinitions();
     AnnotationProcessorEnvironment env = Context.getCurrentEnvironment();
     Collection<TypeDeclaration> typeDeclarations = new ArrayList<TypeDeclaration>(env.getTypeDeclarations());
     typeDeclarations.addAll(Arrays.asList(additionalApiDefinitions));
 
     //trim the classes that are not "include"d.
     trimNotIncludedClasses(typeDeclarations);
 
     //remove any explicitly-excluded classes
     removeExcludedClasses(typeDeclarations);
 
     //override any namespace prefix mappings as specified in the config.
     for (String ns : config.getNamespacesToPrefixes().keySet()) {
       String prefix = config.getNamespacesToPrefixes().get(ns);
       model.getNamespacesToPrefixes().put(ns, prefix);
       debug("Assigning namespace prefix %s to namespace %s as specified in the config.", prefix, ns);
     }
 
     //override any content type ids as specified in the config.
     for (String ct : config.getContentTypesToIds().keySet()) {
       String id = config.getContentTypesToIds().get(ct);
       model.getContentTypesToIds().put(ct, id);
       debug("Assigning id '%s' to content type '%s' as specified in the config.", id, ct);
     }
 
     String baseURL = config.getDeploymentProtocol() + "://" + config.getDeploymentHost();
     if (config.getDeploymentContext() != null) {
       baseURL += config.getDeploymentContext();
     }
     else if ((config.getLabel() != null) && (!"".equals(config.getLabel()))) {
       //we don't have a default context set, so we'll just guess that it's the project label.
       baseURL += ("/" + config.getLabel());
     }
     model.setBaseDeploymentAddress(baseURL);
 
     List<EnunciateTypeDeclarationListener> typeDeclarationListeners = new ArrayList<EnunciateTypeDeclarationListener>();
     for (DeploymentModule module : config.getAllModules()) {
       if (module instanceof EnunciateTypeDeclarationListener) {
         typeDeclarationListeners.add((EnunciateTypeDeclarationListener) module);
       }
     }
 
     debug("Reading classes to enunciate...");
     for (TypeDeclaration declaration : typeDeclarations) {
       final boolean isEndpointInterface = isEndpointInterface(declaration);
       final boolean isRESTEndpoint = isRESTEndpoint(declaration);
       final boolean isContentTypeHandler = isRESTContentTypeHandler(declaration);
       final boolean isJAXRSRootResource = isJAXRSRootResource(declaration);
       final boolean isJAXRSSupport = isJAXRSSupport(declaration);
       if (isEndpointInterface || isRESTEndpoint || isContentTypeHandler || isJAXRSRootResource || isJAXRSSupport) {
         if (isEndpointInterface) {
           EndpointInterface endpointInterface = new EndpointInterface(declaration, additionalApiDefinitions);
           debug("%s to be considered as an endpoint interface.", declaration.getQualifiedName());
           for (EndpointImplementation implementation : endpointInterface.getEndpointImplementations()) {
             debug("%s is the implementation of endpoint interface %s.", implementation.getQualifiedName(), endpointInterface.getQualifiedName());
           }
           model.add(endpointInterface);
         }
 
         if (isRESTEndpoint) {
           RESTEndpoint restEndpoint = new RESTEndpoint((ClassDeclaration) declaration);
           debug("%s to be considered as a REST endpoint.", declaration.getQualifiedName());
           model.add(restEndpoint);
         }
 
         if (isContentTypeHandler) {
           debug("%s to be considered a content type handler.", declaration.getQualifiedName());
           model.addContentTypeHandler((ClassDeclaration) declaration);
         }
 
         if (isJAXRSRootResource) {
           RootResource rootResource = new RootResource(declaration);
           debug("%s to be considered as a JAX-RS root resource.", declaration.getQualifiedName());
           model.add(rootResource);
         }
 
         if (isJAXRSSupport) {
           if (declaration.getAnnotation(Provider.class) != null) {
             debug("%s to be considered as a JAX-RS provider.", declaration.getQualifiedName());
             model.addJAXRSProvider(declaration);
           }
           else {
             debug("%s to be considered a JAX-RS support class.", declaration.getQualifiedName());
           }
         }
       }
       else if (isRegistry(declaration)) {
         debug("%s to be considered as an XML registry.", declaration.getQualifiedName());
         Registry registry = new Registry((ClassDeclaration) declaration);
         model.add(registry);
       }
       else {
         boolean xmlType = isPotentialXmlSchemaType(declaration);
         if (xmlType) {
           TypeDefinition typeDef = createTypeDefinition((ClassDeclaration) declaration, model);
           loadTypeDef(typeDef, model);
         }
 
         boolean jsonType = isPotentialJsonSchemaType(declaration);
         if (jsonType) {
           JsonTypeDefinition typeDefinition = JsonTypeDefinition.createTypeDefinition((ClassDeclaration) declaration);
           loadJsonTypeDef(typeDefinition, model);
         }
 
         if (!xmlType && !jsonType) {
           onUnhandledDeclaration(model, declaration);
         }
       }
 
       for (EnunciateTypeDeclarationListener declarationListener : typeDeclarationListeners) {
         declarationListener.onTypeDeclarationInspected(declaration);
       }
     }
 
     validate(model);
 
     return model;
   }
 
   protected void onUnhandledDeclaration(EnunciateFreemarkerModel model, TypeDeclaration declaration) {
     debug("%s is neither an endpoint interface, rest endpoint, or a schema type, so it'll be ignored.", declaration.getQualifiedName());
   }
 
   /**
    * Remove any classes that are explicitly exluded from this (presumably modifiable) collection.
    *
    * @param typeDeclarations the declarations.
    */
   protected void removeExcludedClasses(Collection<TypeDeclaration> typeDeclarations) {
     EnunciateConfiguration config = this.enunciate.getConfig();
     AntPatternMatcher matcher = new AntPatternMatcher();
     matcher.setPathSeparator(".");
     if (!config.getApiExcludePatterns().isEmpty()) {
       Iterator<TypeDeclaration> typeDeclarationIt = typeDeclarations.iterator();
       while (typeDeclarationIt.hasNext()) {
         TypeDeclaration typeDeclaration = typeDeclarationIt.next();
         boolean exclude = false;
         if (config.getApiExcludePatterns().contains(typeDeclaration.getQualifiedName())) {
           exclude = true;
           debug("%s was explicitly excluded.", typeDeclaration.getQualifiedName());
         }
         else {
           for (String excludePattern : config.getApiExcludePatterns()) {
             if (matcher.match(excludePattern, typeDeclaration.getQualifiedName())) {
               exclude = true;
               debug("%s matches exclude pattern %s.", typeDeclaration.getQualifiedName(), excludePattern);
               break;
             }
           }
         }
 
         if (exclude) {
           typeDeclarationIt.remove();
         }
         else {
           debug("%s NOT to be excluded as an API class because it didn't match any exclude pattern.", typeDeclaration);
         }
       }
     }
   }
 
   /**
    * Trim any classes that are not explicitly included from this (presumably modifiable) collection.
    *
    * @param typeDeclarations The declarations.
    */
   protected void trimNotIncludedClasses(Collection<TypeDeclaration> typeDeclarations) {
     EnunciateConfiguration config = this.enunciate.getConfig();
     AntPatternMatcher matcher = new AntPatternMatcher();
     matcher.setPathSeparator(".");
     if (!config.getApiIncludePatterns().isEmpty()) {
       Iterator<TypeDeclaration> typeDeclarationIt = typeDeclarations.iterator();
       while (typeDeclarationIt.hasNext()) {
         TypeDeclaration typeDeclaration = typeDeclarationIt.next();
         boolean include = false;
         if (config.getApiIncludePatterns().contains(typeDeclaration.getQualifiedName())) {
           include = true;
           debug("%s was explicitly included.", typeDeclaration.getQualifiedName());
         }
         else {
           for (String includePattern : config.getApiIncludePatterns()) {
             if (matcher.match(includePattern, typeDeclaration.getQualifiedName())) {
               include = true;
               debug("%s matches include pattern %s.", typeDeclaration.getQualifiedName(), includePattern);
               break;
             }
           }
         }
 
         if (!include) {
           debug("%s NOT to be included as an API class because it didn't match any include pattern.", typeDeclaration);
           typeDeclarationIt.remove();
         }
       }
     }
   }
 
   /**
    * Loads the type declarations for the additional API definitions.
    *
    * @return The type declarations.
    */
   protected TypeDeclaration[] loadAdditionalApiDefinitions() {
     AnnotationProcessorEnvironment environment = Context.getCurrentEnvironment();
     Collection<TypeDeclaration> additionalApiDefinitions = new ArrayList<TypeDeclaration>();
     if (this.additionalApiClasses != null) {
       for (String additionalApiClass : this.additionalApiClasses) {
         TypeDeclaration declaration = environment.getTypeDeclaration(additionalApiClass);
         if (declaration != null) {
           additionalApiDefinitions.add(declaration);
         }
         else {
           this.enunciate.warn("Unable to load type definition for imported API class '%s'.", additionalApiClass);
         }
       }
     }
     return additionalApiDefinitions.toArray(new TypeDeclaration[additionalApiDefinitions.size()]);
   }
 
   /**
    * Loads the specified type definition into the specified model.
    *
    * @param typeDefinition The type definition to load.
    * @param model          The model into which to load the type definition.
    */
   protected void loadJsonTypeDef(JsonTypeDefinition typeDefinition, EnunciateFreemarkerModel model) {
     if (typeDefinition != null) {
       if (this.enunciate.getConfig() != null && !this.enunciate.getConfig().isExcludeUnreferencedClasses()) {
         debug("%s to be considered as a %s", typeDefinition.getTypeName(), typeDefinition.getClass().getSimpleName());
         model.addJsonType(typeDefinition);
 
         if(typeDefinition.getDelegate().getAnnotation(JsonRootType.class) != null)
         {
           debug("%s to be considered as a root element", typeDefinition.getTypeName());
           model.addJsonRootElement(new JsonRootElementDeclaration(typeDefinition));
         }
       }
       else {
         debug("%s is a potential schema type definition, but we're not going to add it directly to the model. (It could still be indirectly added, though.)", typeDefinition.getTypeName());
       }
     }
   }
 
   /**
    * Loads the specified type definition into the specified model.
    *
    * @param typeDef The type definition to load.
    * @param model   The model into which to load the type definition.
    */
   protected void loadTypeDef(TypeDefinition typeDef, EnunciateFreemarkerModel model) {
     if (typeDef != null) {
       if (this.enunciate.getConfig() != null && !this.enunciate.getConfig().isExcludeUnreferencedClasses()) {
         debug("%s to be considered as a %s (qname:{%s}%s).",
               typeDef.getQualifiedName(), typeDef.getClass().getSimpleName(),
               typeDef.getNamespace() == null ? "" : typeDef.getNamespace(),
               typeDef.getName() == null ? "(anonymous)" : typeDef.getName());
 
         model.add(typeDef);
       }
       else {
         debug("%s is a potential schema type definition, but we're not going to add it directly to the model. (It could still be indirectly added, though.)", typeDef.getQualifiedName());
       }
 
       RootElementDeclaration rootElement = createRootElementDeclaration((ClassDeclaration) typeDef.getDelegate(), typeDef);
       if (rootElement != null) {
         debug("%s to be considered as a root element", typeDef.getQualifiedName());
         model.add(rootElement);
       }
     }
   }
 
   /**
    * Validate the model. This action uses the validator specified in the config as well as any
    * module-specific validators.  Errors and warnings are printed using the APT messager.
    *
    * @param model The model to validate.
    * @throws ModelValidationException If any validation errors are encountered.
    */
   protected void validate(EnunciateFreemarkerModel model) throws ModelValidationException {
     debug("Validating the model...");
     Messager messager = getMessager();
     ValidatorChain validator = new ValidatorChain();
     EnunciateConfiguration config = this.enunciate.getConfig();
     Set<String> disabledRules = new TreeSet<String>(config.getDisabledRules());
     if (this.enunciate.isModuleEnabled("rest")) {
       //if the REST module is enabled, disable the validation rule that
       //fails if the module is not enabled.
       disabledRules.add("disabled.rest.module");
     }
 
     Validator coreValidator = config.getValidator();
     if (coreValidator instanceof ConfigurableRules) {
       ((ConfigurableRules) coreValidator).disableRules(disabledRules);
     }
     validator.addValidator("core", coreValidator);
     debug("Default validator added to the chain.");
     for (DeploymentModule module : config.getEnabledModules()) {
       Validator moduleValidator = module.getValidator();
       if (moduleValidator != null) {
         if (moduleValidator instanceof ConfigurableRules) {
           ((ConfigurableRules)moduleValidator).disableRules(disabledRules);
         }
         validator.addValidator(module.getName(), moduleValidator);
         debug("Validator for module %s added to the chain.", module.getName());
       }
     }
 
     if (!config.isAllowEmptyNamespace()) {
       validator.addValidator("emptyns", new EmptyNamespaceValidator());
     }
 
     ValidationResult validationResult = validate(model, validator);
 
     if (validationResult.hasWarnings()) {
       warn("Validation result has warnings.");
       for (ValidationMessage warning : validationResult.getWarnings()) {
         if (!disabledRules.contains("all.warnings") && !disabledRules.contains(String.valueOf(warning.getLabel()) + ".warnings")) {
           StringBuilder text = new StringBuilder();
           if (warning.getLabel() != null) {
             text.append('[').append(warning.getLabel()).append("] ");
           }
           text.append(warning.getText());
 
           if (warning.getPosition() != null) {
             messager.printWarning(warning.getPosition(), text.toString());
           }
           else {
             messager.printWarning(text.toString());
           }
         }
       }
     }
 
     if (validationResult.hasErrors()) {
       warn("Validation result has errors.");
       for (ValidationMessage error : validationResult.getErrors()) {
         StringBuilder text = new StringBuilder();
         if (error.getLabel() != null) {
           text.append('[').append(error.getLabel()).append("] ");
         }
         text.append(error.getText());
 
         if (error.getPosition() != null) {
           messager.printError(error.getPosition(), text.toString());
         }
         else {
           messager.printError(text.toString());
         }
       }
 
       throw new ModelValidationException();
     }
   }
 
   /**
    * Get the messager for the current environment.
    *
    * @return The messager.
    */
   protected Messager getMessager() {
     return Context.getCurrentEnvironment().getMessager();
   }
 
   //Inherited.
   @Override
   protected FreemarkerModel newRootModel() {
     return new EnunciateFreemarkerModel();
   }
 
   /**
    * Whether the specified declaration is a registry.
    *
    * @param declaration The declaration.
    * @return Whether the specified declaration is a registry.
    */
   protected boolean isRegistry(TypeDeclaration declaration) {
     return declaration.getAnnotation(XmlRegistry.class) != null;
   }
 
   /**
    * Whether the specified declaration is a potential schema type for JSON data.
    *
    * @param declaration The declaration to determine whether it's a potential schema type for JSON data.
    * @return Whether the specified declaration is a potential schema type for JSON data.
    */
   protected boolean isPotentialJsonSchemaType(TypeDeclaration declaration) {
     return declaration instanceof ClassDeclaration && (declaration.getAnnotation(JsonType.class) != null || declaration.getAnnotation(JsonRootType.class) != null);
   }
 
   /**
    * Whether the specified declaration is a potential schema type.
    *
    * @param declaration The declaration to determine whether it's a potential schema type.
    * @return Whether the specified declaration is a potential schema type.
    */
   protected boolean isPotentialXmlSchemaType(TypeDeclaration declaration) {
     if (!(declaration instanceof ClassDeclaration)) {
       debug("%s isn't a potential schema type because it's not a class.", declaration.getQualifiedName());
       return false;
     }
 
     if ((declaration.getPackage() != null) && (declaration.getPackage().getAnnotation(XmlTransient.class) != null)) {
       debug("%s isn't a potential schema type because its package is annotated as XmlTransient.", declaration.getQualifiedName());
       return false;
     }
 
     Collection<AnnotationMirror> annotationMirrors = declaration.getAnnotationMirrors();
     boolean explicitXMLTypeOrElement = false;
     for (AnnotationMirror mirror : annotationMirrors) {
       AnnotationTypeDeclaration annotationDeclaration = mirror.getAnnotationType().getDeclaration();
       if (annotationDeclaration != null) {
         String fqn = annotationDeclaration.getQualifiedName();
         //exclude all XmlTransient types and all jaxws types.
         if (XmlTransient.class.getName().equals(fqn)
           || "javax.xml.bind.annotation.XmlTransient".equals(fqn)
           || fqn.startsWith("javax.xml.ws")
           || fqn.startsWith("javax.ws.rs")
           || fqn.startsWith("javax.jws")) {
           debug("%s isn't a potential schema type because of annotation %s.", declaration.getQualifiedName(), fqn);
           return false;
         }
         else {
           explicitXMLTypeOrElement = ("javax.xml.bind.annotation.XmlType".equals(fqn))
             || ("javax.xml.bind.annotation.XmlRootElement".equals(fqn));
         }
       }
     }
 
     return explicitXMLTypeOrElement || !isThrowable(declaration);
   }
 
   /**
    * Whether the specified declaration is throwable.
    *
    * @param declaration The declaration to determine whether it is throwable.
    * @return Whether the specified declaration is throwable.
    */
   protected boolean isThrowable(TypeDeclaration declaration) {
     if (!(declaration instanceof ClassDeclaration)) {
       return false;
     }
     else if (Throwable.class.getName().equals(declaration.getQualifiedName())) {
       return false;
     }
     else {
       ClassType superClass = ((ClassDeclaration) declaration).getSuperclass();
       return ((DecoratedTypeMirror) TypeMirrorDecorator.decorate(superClass)).isInstanceOf(Throwable.class.getName());
     }
   }
 
   /**
    * Whether the specified declaration is a REST endpoint.
    *
    * @param declaration The declaration.
    * @return Whether the declaration is a REST endpoint.
    */
   public boolean isRESTEndpoint(TypeDeclaration declaration) {
     return ((declaration.getAnnotation(XmlTransient.class) == null)
       && (declaration instanceof ClassDeclaration) && (declaration.getAnnotation(org.codehaus.enunciate.rest.annotations.RESTEndpoint.class) != null));
   }
 
   /**
    * Whether the specified declaration is a data format handler.
    *
    * @param declaration The declaration.
    * @return Whether the specified declaration is a data format handler.
    */
   public boolean isRESTContentTypeHandler(TypeDeclaration declaration) {
     return (declaration instanceof ClassDeclaration) && (declaration.getAnnotation(ContentTypeHandler.class) != null);
   }
 
   /**
    * A quick check to see if a declaration is an endpoint interface.
    */
   public boolean isEndpointInterface(TypeDeclaration declaration) {
     WebService ws = declaration.getAnnotation(WebService.class);
     return (declaration.getAnnotation(XmlTransient.class) == null)
       && (ws != null) && ((declaration instanceof InterfaceDeclaration)
       //if this is a class declaration, then it has an implicit endpoint interface if it doesn't reference another.
       || (ws.endpointInterface() == null) || ("".equals(ws.endpointInterface())));
   }
 
   /**
    * Whether the specified type is a JAX-RS root resource.
    *
    * @param declaration The declaration.
    * @return Whether the specified type is a JAX-RS root resource.
    */
   public boolean isJAXRSRootResource(TypeDeclaration declaration) {
     return declaration.getAnnotation(XmlTransient.class) == null
       && declaration.getAnnotation(Path.class) != null;
   }
 
   /**
    * Whether the specified type is a JAX-RS support class (resource and/or provider).
    *
    * @param declaration The declaration.
    * @return Whether the specified type is a JAX-RS support class.
    */
   public boolean isJAXRSSupport(TypeDeclaration declaration) {
    if (declaration.getAnnotation(XmlTransient.class) != null) {
       return false;
     }
 
     //it's a JAX-RS resource if any method has either a @Path or a resource method designator.
     Collection<? extends MethodDeclaration> methods = declaration.getMethods();
     for (MethodDeclaration method : methods) {
       for (AnnotationMirror mirror : method.getAnnotationMirrors()) {
         AnnotationType type = mirror.getAnnotationType();
         if (type.getDeclaration() != null && type.getDeclaration().getAnnotation(HttpMethod.class) != null) {
           return true;
         }
       }
     }
 
     //otherwise it's a JAX-RS provider if it's annotated as such.
     return declaration.getAnnotation(Provider.class) != null;
   }
 
   /**
    * Find the type definition for a class given the class's declaration, or null if the class is xml transient.
    *
    * @param declaration The declaration.
    * @param model       The model to use to create the type declaration.
    * @return The type definition.
    */
   public TypeDefinition createTypeDefinition(ClassDeclaration declaration, EnunciateFreemarkerModel model) {
     return model.createTypeDefinition(declaration);
   }
 
   /**
    * Find or create the root element declaration for the specified type definition.
    *
    * @param declaration    The class declaration
    * @param typeDefinition The specified type definition.
    * @return The root element declaration.
    */
   public RootElementDeclaration createRootElementDeclaration(ClassDeclaration declaration, TypeDefinition typeDefinition) {
     if (!isRootSchemaElement(declaration)) {
       return null;
     }
     else {
       return new RootElementDeclaration(declaration, typeDefinition);
     }
   }
 
   /**
    * A quick check to see if a declaration defines a root schema element.
    */
   protected boolean isRootSchemaElement(TypeDeclaration declaration) {
     return declaration.getAnnotation(XmlRootElement.class) != null;
   }
 
   //Inherited.
   @Override
   public Collection<FreemarkerTransform> getTransforms() {
     String namespace = Context.getCurrentEnvironment().getOptions().get(EnunciateAnnotationProcessorFactory.FM_LIBRARY_NS_OPTION);
     Collection<FreemarkerTransform> transforms = super.getTransforms();
 
     //common transforms.
     transforms.add(new ForEachServiceEndpointTransform(namespace));
 
     //jaxws transforms.
     transforms.add(new ForEachBindingTypeTransform(namespace));
     transforms.add(new ForEachEndpointInterfaceTransform(namespace));
     transforms.add(new ForEachThrownWebFaultTransform(namespace));
     transforms.add(new ForEachWebFaultTransform(namespace));
     transforms.add(new ForEachWebMessageTransform(namespace));
     transforms.add(new ForEachWebMethodTransform(namespace));
     transforms.add(new ForEachWsdlTransform(namespace));
 
     //schema/data transforms.
     transforms.add(new ForEachSchemaTransform(namespace));
     transforms.add(new ForEachJsonSchemaTransform(namespace));
 
     //rest transforms.
     transforms.add(new ForEachRESTResourceListByPathTransform(namespace));
     transforms.add(new ForEachRESTEndpointTransform(namespace));
     transforms.add(new ForEachRESTNounTransform(namespace));
 
     //set up the enunciate file transform.
     EnunciateFileTransform fileTransform = new EnunciateFileTransform(namespace);
     transforms.add(fileTransform);
     return transforms;
   }
 
   /**
    * Validates the model given a validator.
    *
    * @param model     The model to validate.
    * @param validator The validator.
    * @return The results of the validation.
    */
   protected ValidationResult validate(EnunciateFreemarkerModel model, Validator validator) {
     ValidationResult validationResult = new ValidationResult();
 
     for (EndpointInterface ei : model.endpointInterfaces) {
       debug("Validating %s...", ei.getQualifiedName());
       validationResult.aggregate(validator.validateEndpointInterface(ei));
     }
 
     for (TypeDefinition typeDefinition : model.typeDefinitions) {
       debug("Validating %s...", typeDefinition.getQualifiedName());
       validationResult.aggregate(typeDefinition.accept(validator));
     }
 
     for (RootElementDeclaration rootElement : model.rootElements) {
       debug("Validating %s...", rootElement.getQualifiedName());
       validationResult.aggregate(validator.validateRootElement(rootElement));
     }
 
     // TODO Validate JSON root elements
 
     debug("Validating the REST API...");
     validationResult.aggregate(validator.validateRESTAPI(model.getNounsToRESTMethods()));
     validationResult.aggregate(validator.validateContentTypeHandlers(model.getContentTypeHandlers()));
     validationResult.aggregate(validator.validateRootResources(model.getRootResources()));
 
     //validate unique content type ids.
     Set<String> uniqueContentTypeIds = new TreeSet<String>();
     for (String contentType : model.getContentTypesToIds().keySet()) {
       String id = model.getContentTypesToIds().get(contentType);
       if (!uniqueContentTypeIds.add(id)) {
         StringBuilder builder = new StringBuilder("All content types must have unique ids.  The id '").
           append(id).append("' is assigned to the following content types: '").append(contentType).append("'");
         for (String ct : model.getContentTypesToIds().keySet()) {
           if (!contentType.equals(ct) && (id.equals(model.getContentTypesToIds().get(ct)))) {
             builder.append(", '").append(ct).append("'");
           }
         }
         builder.append(". Please use the Enunciate configuration to specify a unique id for each content type.");
         validationResult.addError((Declaration) null, builder.toString());
         break;
       }
     }
 
     return validationResult;
   }
 
   //Inherited.
   @Override
   protected void process(TemplateException e) {
     if (!(e instanceof ModelValidationException)) {
       Messager messager = getMessager();
       StringWriter stackTrace = new StringWriter();
       e.printStackTrace(new PrintWriter(stackTrace));
       messager.printError(stackTrace.toString());
     }
 
     this.ee = new EnunciateException(e);
   }
 
   protected void process(EnunciateException e) {
     this.ee = e;
   }
 
   protected void process(IOException e) {
     this.ioe = e;
   }
 
   protected void process(RuntimeException e) {
     this.re = e;
   }
 
   /**
    * Throws any errors that occurred during processing.
    */
   public void throwAnyErrors() throws EnunciateException, IOException {
     if (this.ee != null) {
       throw this.ee;
     }
     else if (this.ioe != null) {
       throw this.ioe;
     }
     else if (this.re != null) {
       throw re;
     }
   }
 
   /**
    * Handle an info-level message.
    *
    * @param message    The info message.
    * @param formatArgs The format args of the message.
    */
   public void info(String message, Object... formatArgs) {
     this.enunciate.info(message, formatArgs);
   }
 
   /**
    * Handle a debug-level message.
    *
    * @param message    The debug message.
    * @param formatArgs The format args of the message.
    */
   public void debug(String message, Object... formatArgs) {
     this.enunciate.debug(message, formatArgs);
   }
 
   /**
    * Handle a warn-level message.
    *
    * @param message    The warn message.
    * @param formatArgs The format args of the message.
    */
   public void warn(String message, Object... formatArgs) {
     this.enunciate.warn(message, formatArgs);
   }
 
 }
