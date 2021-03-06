 /* ====================================================================
  * Redistribution and use of this software and associated documentation
  * ("Software"), with or without modification, are permitted provided
  * that the following conditions are met:
  *
  * 1. Redistributions of source code must retain copyright
  *    statements and notices.  Redistributions must also contain a
  *    copy of this document.
  *
  * 2. Redistributions in binary form must reproduce this list of
  *    conditions and the following disclaimer in the documentation
  *    and/or other materials provided with the distribution.
  *
  * 3. The name "OpenEJB" must not be used to endorse or promote
  *    products derived from this Software without prior written
  *    permission of The OpenEJB Group.  For written permission,
  *    please contact openejb-group@openejb.sf.net.
  *
  * 4. Products derived from this Software may not be called "OpenEJB"
  *    nor may "OpenEJB" appear in their names without prior written
  *    permission of The OpenEJB Group. OpenEJB is a registered
  *    trademark of The OpenEJB Group.
  *
  * 5. Due credit should be given to the OpenEJB Project
  *    (http://openejb.org/).
  *
  * THIS SOFTWARE IS PROVIDED BY THE OPENEJB GROUP AND CONTRIBUTORS
  * ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT
  * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
  * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
  * THE OPENEJB GROUP OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
  * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
  * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
  * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
  * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
  * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
  * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
  * OF THE POSSIBILITY OF SUCH DAMAGE.
  *
  * ====================================================================
  *
  * This software consists of voluntary contributions made by many
  * individuals on behalf of the OpenEJB Project.  For more information
  * please see <http://openejb.org/>.
  *
  * ====================================================================
  */
 
 package org.openejb.deployment;
 
 import java.io.File;
 import java.io.IOException;
 import java.net.URI;
 import java.net.URISyntaxException;
 import java.net.URL;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.Map;
 import java.util.Properties;
 import java.util.jar.JarFile;
 import javax.management.MalformedObjectNameException;
 import javax.management.ObjectName;
 import javax.naming.Reference;
 
 import org.apache.geronimo.deployment.DeploymentException;
 import org.apache.geronimo.deployment.service.GBeanHelper;
 import org.apache.geronimo.deployment.util.IOUtil;
 import org.apache.geronimo.deployment.util.JarUtil;
 import org.apache.geronimo.gbean.GBeanInfo;
 import org.apache.geronimo.gbean.GBeanInfoFactory;
 import org.apache.geronimo.gbean.jmx.GBeanMBean;
 import org.apache.geronimo.j2ee.deployment.EARContext;
 import org.apache.geronimo.j2ee.deployment.EJBModule;
 import org.apache.geronimo.j2ee.deployment.EJBReferenceBuilder;
 import org.apache.geronimo.j2ee.deployment.Module;
 import org.apache.geronimo.j2ee.deployment.ModuleBuilder;
 import org.apache.geronimo.kernel.Kernel;
 import org.apache.geronimo.naming.jmx.JMXReferenceFactory;
 import org.apache.geronimo.schema.SchemaConversionUtils;
 import org.apache.geronimo.security.deploy.Security;
 import org.apache.geronimo.xbeans.j2ee.EjbJarDocument;
 import org.apache.geronimo.xbeans.j2ee.EjbJarType;
 import org.apache.geronimo.xbeans.j2ee.EnterpriseBeansType;
 import org.apache.xmlbeans.XmlException;
 import org.apache.xmlbeans.XmlObject;
 import org.openejb.EJBModuleImpl;
 import org.openejb.proxy.EJBProxyFactory;
 import org.openejb.proxy.ProxyObjectFactory;
 import org.openejb.proxy.ProxyRefAddr;
 import org.openejb.xbeans.ejbjar.OpenejbDependencyType;
 import org.openejb.xbeans.ejbjar.OpenejbEntityBeanType;
 import org.openejb.xbeans.ejbjar.OpenejbGbeanType;
 import org.openejb.xbeans.ejbjar.OpenejbMessageDrivenBeanType;
 import org.openejb.xbeans.ejbjar.OpenejbOpenejbJarDocument;
 import org.openejb.xbeans.ejbjar.OpenejbOpenejbJarType;
 import org.openejb.xbeans.ejbjar.OpenejbSessionBeanType;
 import org.tranql.ejb.EJBSchema;
 import org.tranql.sql.DataSourceDelegate;
 import org.tranql.sql.sql92.SQL92Schema;
 
 
 /**
  * @version $Revision$ $Date$
  */
 public class OpenEJBModuleBuilder implements ModuleBuilder, EJBReferenceBuilder {
     final Kernel kernel;
     private final CMPEntityBuilder cmpEntityBuilder;
     private final SessionBuilder sessionBuilder;
     private final EntityBuilder entityBuilder;
     private final MdbBuilder mdbBuilder;
     private final SecurityBuilder securityBuilder;
 
     public OpenEJBModuleBuilder(Kernel kernel) {
         this.kernel = kernel;
         this.securityBuilder = new SecurityBuilder(this);
         this.cmpEntityBuilder = new CMPEntityBuilder(this);
         this.sessionBuilder = new SessionBuilder(this);
         this.entityBuilder = new EntityBuilder(this);
         this.mdbBuilder = new MdbBuilder(kernel, this);
     }
 
     public SecurityBuilder getSecurityBuilder() {
         return securityBuilder;
     }
 
     public Module createModule(File plan, JarFile moduleFile) throws DeploymentException {
         return createModule(plan, moduleFile, "war", null, true);
     }
 
     public Module createModule(Object plan, JarFile moduleFile, String targetPath, URL specDDUrl, URI earConfigId) throws DeploymentException {
         return createModule(plan, moduleFile, targetPath, specDDUrl, false);
     }
 
     private Module createModule(Object plan, JarFile moduleFile, String targetPath, URL specDDUrl, boolean standAlone) throws DeploymentException {
         assert moduleFile != null: "moduleFile is null";
         assert targetPath != null: "targetPath is null";
         assert !targetPath.endsWith("/"): "targetPath must not end with a '/'";
 
         String specDD;
         EjbJarType ejbJar;
         try {
             if (specDDUrl == null) {
                 specDDUrl = JarUtil.createJarURL(moduleFile, "META-INF/ejb-jar.xml");
             }
 
             // read in the entire specDD as a string, we need this for getDeploymentDescriptor
             // on the J2ee management object
             specDD = IOUtil.readAll(specDDUrl);
 
             // parse it
             EjbJarDocument ejbJarDoc = SchemaConversionUtils.convertToEJBSchema(SchemaConversionUtils.parse(specDD));
             ejbJar = ejbJarDoc.getEjbJar();
         } catch (Exception e) {
             return null;
         }
 
         OpenejbOpenejbJarType openejbJar = getOpenejbJar(plan, moduleFile, standAlone, targetPath, ejbJar);
 
         // get the ids from either the application plan or for a stand alone module from the specific deployer
         URI configId = null;
         try {
             configId = new URI(openejbJar.getConfigId());
         } catch (URISyntaxException e) {
             throw new DeploymentException("Invalid configId " + openejbJar.getConfigId(), e);
         }
 
         URI parentId = null;
         if (openejbJar.isSetParentId()) {
             try {
                 parentId = new URI(openejbJar.getParentId());
             } catch (URISyntaxException e) {
                 throw new DeploymentException("Invalid parentId " + openejbJar.getParentId(), e);
             }
         }
 
         return new EJBModule(standAlone, configId, parentId, moduleFile, targetPath, ejbJar, openejbJar, specDD);
     }
 
     OpenejbOpenejbJarType getOpenejbJar(Object plan, JarFile moduleFile, boolean standAlone, String targetPath, EjbJarType ejbJar) throws DeploymentException {
         OpenejbOpenejbJarType openejbJar = null;
         try {
             // load the openejb-jar.xml from either the supplied plan or from the earFile
             try {
                 if (plan instanceof XmlObject) {
                     openejbJar = (OpenejbOpenejbJarType) SchemaConversionUtils.getNestedObjectAsType(
                             (XmlObject) plan,
                             "openejb-jar",
                             OpenejbOpenejbJarType.type);
                 } else {
                     OpenejbOpenejbJarDocument openejbJarDoc = null;
                     if (plan != null) {
                         openejbJarDoc = OpenejbOpenejbJarDocument.Factory.parse((File)plan);
                     } else {
                         URL path = JarUtil.createJarURL(moduleFile, "META-INF/openejb-jar.xml");
                         openejbJarDoc = OpenejbOpenejbJarDocument.Factory.parse(path);
                     }
                     if (openejbJarDoc != null) {
                         openejbJar = openejbJarDoc.getOpenejbJar();
                     }
                 }
             } catch (IOException e) {
             }
 
             // if we got one extract the validate it otherwise create a default one
             if (openejbJar != null) {
                 openejbJar = (OpenejbOpenejbJarType) SchemaConversionUtils.convertToGeronimoNamingSchema(openejbJar);
                 SchemaConversionUtils.validateDD(openejbJar);
             } else {
                 String path;
                 if (standAlone) {
                     // default configId is based on the moduleFile name
                     path = new File(moduleFile.getName()).getName();
                 } else {
                     // default configId is based on the module uri from the application.xml
                     path = targetPath;
                 }
                 openejbJar = createDefaultPlan(path, ejbJar);
             }
         } catch (XmlException e) {
             throw new DeploymentException(e);
         }
         return openejbJar;
     }
 
     private OpenejbOpenejbJarType createDefaultPlan(String name, EjbJarType ejbJar) {
         String id = ejbJar.getId();
         if (id == null) {
             id = name;
             if (id.endsWith(".jar")) {
                 id = id.substring(0, id.length() - 4);
             }
             if (id.endsWith("/")) {
                 id = id.substring(0, id.length() - 1);
             }
         }
 
         OpenejbOpenejbJarType openejbEjbJar = OpenejbOpenejbJarType.Factory.newInstance();
         openejbEjbJar.setParentId("org/apache/geronimo/Server");
         if (null != ejbJar.getId()) {
             openejbEjbJar.setConfigId(ejbJar.getId());
         } else {
             openejbEjbJar.setConfigId(id);
         }
         openejbEjbJar.addNewEnterpriseBeans();
         return openejbEjbJar;
     }
 
     public void installModule(JarFile earFile, EARContext earContext, Module module) throws DeploymentException {
         try {
             // extract the ejbJar file into a standalone packed jar file and add the contents to the output
             File ejbJarFile = JarUtil.extractToPackedJar(module.getModuleFile());
             earContext.addInclude(URI.create(module.getTargetPath()), ejbJarFile.toURL());
 
             // add the dependencies declared in the openejb-jar.xml file
             OpenejbOpenejbJarType openEjbJar = (OpenejbOpenejbJarType) module.getVendorDD();
             OpenejbDependencyType[] dependencies = openEjbJar.getDependencyArray();
             for (int i = 0; i < dependencies.length; i++) {
                 earContext.addDependency(getDependencyURI(dependencies[i]));
             }
         } catch (IOException e) {
             throw new DeploymentException("Unable to deploy ejb module [" + module.getName() + "]", e);
         }
     }
 
     public void initContext(EARContext earContext, Module module, ClassLoader cl) throws DeploymentException {
         EJBModule ejbModule = (EJBModule) module;
         EjbJarType ejbJar = (EjbJarType) ejbModule.getSpecDD();
         EnterpriseBeansType enterpriseBeans = ejbJar.getEnterpriseBeans();
 
         sessionBuilder.initContext(earContext, module, cl, enterpriseBeans);
         entityBuilder.initContext(earContext, module, cl, enterpriseBeans);
         mdbBuilder.initContext(cl, enterpriseBeans);
     }
 
    public Reference createEJBLocalReference(String objectName, boolean session, String local, String localHome) {
         ProxyRefAddr address = ProxyRefAddr.createLocal(objectName, session, local, localHome);
         Reference reference = new Reference(null, address, ProxyObjectFactory.class.getName(), null);
         return reference;
     }
 
    public Reference createEJBRemoteReference(String objectName, boolean session, String remote, String home) {
         ProxyRefAddr address = ProxyRefAddr.createRemote(objectName, session, remote, home);
         Reference reference = new Reference(null, address, ProxyObjectFactory.class.getName(), null);
         return reference;
     }
 
     ObjectName createResourceAdapterQueryName(EARContext earContext, String resourceAdapterName) throws DeploymentException {
         return mdbBuilder.createResourceAdapterQueryName(earContext, resourceAdapterName);
     }
 
     public CMPEntityBuilder getCmpEntityBuilder() {
         return cmpEntityBuilder;
     }
 
     public EntityBuilder getBmpEntityBuilder() {
         return entityBuilder;
     }
 
     public MdbBuilder getMdbBuilder() {
         return mdbBuilder;
     }
 
     public SessionBuilder getSessionBuilder() {
         return sessionBuilder;
     }
 
     public String addGBeans(EARContext earContext, Module module, ClassLoader cl) throws DeploymentException {
         EJBModule ejbModule = (EJBModule) module;
         OpenejbOpenejbJarType openejbEjbJar = (OpenejbOpenejbJarType) module.getVendorDD();
         EjbJarType ejbJar = (EjbJarType) module.getSpecDD();
         OpenejbGbeanType[] gbeans = openejbEjbJar.getGbeanArray();
         for (int i = 0; i < gbeans.length; i++) {
             GBeanHelper.addGbean(new OpenEJBGBeanAdapter(gbeans[i]), cl, earContext);
         }
 
         Properties nameProps = new Properties();
         nameProps.put("j2eeType", "EJBModule");
         nameProps.put("name", module.getName());
         nameProps.put("J2EEServer", earContext.getJ2EEServerName());
         nameProps.put("J2EEApplication", earContext.getJ2EEApplicationName());
 
         ObjectName ejbModuleObjectName;
         try {
             ejbModuleObjectName = new ObjectName(earContext.getJ2EEDomainName(), nameProps);
         } catch (MalformedObjectNameException e) {
             throw new DeploymentException("Unable to construct ObjectName", e);
         }
 
         // EJBModule GBean
         String connectionFactoryName = openejbEjbJar.getCmpConnectionFactory();
         EJBSchema ejbSchema = new EJBSchema(module.getName());
         DataSourceDelegate delegate = new DataSourceDelegate();
         SQL92Schema sqlSchema = new SQL92Schema(module.getName(), delegate);
         GBeanMBean ejbModuleGBean = new GBeanMBean(EJBModuleImpl.GBEAN_INFO, cl);
         try {
             ejbModuleGBean.setReferencePatterns("J2EEServer", Collections.singleton(earContext.getServerObjectName()));
             if (!earContext.getJ2EEApplicationName().equals("null")) {
                 ejbModuleGBean.setReferencePatterns("J2EEApplication", Collections.singleton(earContext.getApplicationObjectName()));
             }
 
             ejbModuleGBean.setAttribute("deploymentDescriptor", module.getOriginalSpecDD());
 
             if (connectionFactoryName != null) {
                 JMXReferenceFactory refFactory = new JMXReferenceFactory("geronimo.server", "geronimo");
                 ObjectName connectionFactoryObjectName = refFactory.createManagedConnectionFactoryObjectName(connectionFactoryName);
                 ejbModuleGBean.setReferencePattern("ConnectionFactory", connectionFactoryObjectName);
                 ejbModuleGBean.setAttribute("Delegate", delegate);
             }
         } catch (Exception e) {
             throw new DeploymentException("Unable to initialize EJBModule GBean", e);
         }
         earContext.addGBean(ejbModuleObjectName, ejbModuleGBean);
 
         // @todo need a better schema name
         cmpEntityBuilder.buildCMPSchema(earContext, module.getName(), ejbJar, openejbEjbJar, cl, ejbSchema, sqlSchema);
 
         // create an index of the openejb ejb configurations by ejb-name
         Map openejbBeans = new HashMap();
         //TODO NPE if enterprise-beans or session is missing
         OpenejbSessionBeanType[] openejbSessionBeans = openejbEjbJar.getEnterpriseBeans().getSessionArray();
         for (int i = 0; i < openejbSessionBeans.length; i++) {
             OpenejbSessionBeanType sessionBean = openejbSessionBeans[i];
             openejbBeans.put(sessionBean.getEjbName(), sessionBean);
         }
         //TODO NPE as above
         OpenejbEntityBeanType[] openejbEntityBeans = openejbEjbJar.getEnterpriseBeans().getEntityArray();
         for (int i = 0; i < openejbEntityBeans.length; i++) {
             OpenejbEntityBeanType entityBean = openejbEntityBeans[i];
             openejbBeans.put(entityBean.getEjbName(), entityBean);
         }
         //TODO NPE as above
         OpenejbMessageDrivenBeanType[] openejbMessageDrivenBeans = openejbEjbJar.getEnterpriseBeans().getMessageDrivenArray();
         for (int i = 0; i < openejbMessageDrivenBeans.length; i++) {
             OpenejbMessageDrivenBeanType messageDrivenBean = openejbMessageDrivenBeans[i];
             openejbBeans.put(messageDrivenBean.getEjbName(), messageDrivenBean);
         }
 
         TransactionPolicyHelper transactionPolicyHelper;
         if (ejbJar.isSetAssemblyDescriptor()) {
             transactionPolicyHelper = new TransactionPolicyHelper(ejbJar.getAssemblyDescriptor().getContainerTransactionArray());
         } else {
             transactionPolicyHelper = new TransactionPolicyHelper();
         }
 
         Security security = securityBuilder.buildSecurityConfig(openejbEjbJar);
 
         EnterpriseBeansType enterpriseBeans = ejbJar.getEnterpriseBeans();
 
         sessionBuilder.buildBeans(earContext, module, cl, ejbModule, openejbBeans, transactionPolicyHelper, security, enterpriseBeans);
 
         entityBuilder.buildBeans(earContext, module, cl, ejbModule, openejbBeans, transactionPolicyHelper, security, enterpriseBeans);
 
         cmpEntityBuilder.buildBeans(earContext, module, cl, ejbModule, connectionFactoryName, ejbSchema, sqlSchema, openejbBeans, transactionPolicyHelper, security, enterpriseBeans);
 
         mdbBuilder.buildBeans(earContext, module, cl, ejbModule, openejbBeans, transactionPolicyHelper, security, enterpriseBeans);
 
         return null;
     }
 
     public Object createEJBProxyFactory(String containerId, boolean isSessionBean, String remoteInterfaceName, String homeInterfaceName, String localInterfaceName, String localHomeInterfaceName, ClassLoader cl) throws DeploymentException {
         Class remoteInterface = loadClass(cl, remoteInterfaceName);
         Class homeInterface = loadClass(cl, homeInterfaceName);
         Class localInterface = loadClass(cl, localInterfaceName);
         Class localHomeInterface = loadClass(cl, localHomeInterfaceName);
         return new EJBProxyFactory(containerId, isSessionBean, remoteInterface, homeInterface, localInterface, localHomeInterface);
     }
 
     private Class loadClass(ClassLoader cl, String name) throws DeploymentException {
         if (name == null) {
             return null;
         }
         try {
             return cl.loadClass(name);
         } catch (ClassNotFoundException e) {
             throw new DeploymentException("Unable to load Class: " + name);
         }
     }
 
     private URI getDependencyURI(OpenejbDependencyType dep) throws DeploymentException {
         URI uri;
         if (dep.isSetUri()) {
             try {
                 uri = new URI(dep.getUri());
             } catch (URISyntaxException e) {
                 throw new DeploymentException("Invalid dependency URI " + dep.getUri(), e);
             }
         } else {
             // @todo support more than just jars
             String id = dep.getGroupId() + "/jars/" + dep.getArtifactId() + '-' + dep.getVersion() + ".jar";
             try {
                 uri = new URI(id);
             } catch (URISyntaxException e) {
                 throw new DeploymentException("Unable to construct URI for groupId=" + dep.getGroupId() + ", artifactId=" + dep.getArtifactId() + ", version=" + dep.getVersion(), e);
             }
         }
         return uri;
     }
 
 
     protected static String getJ2eeStringValue(org.apache.geronimo.xbeans.j2ee.String string) {
         if (string == null) {
             return null;
         }
         return string.getStringValue();
     }
 
     public static final GBeanInfo GBEAN_INFO;
 
     static {
         GBeanInfoFactory infoFactory = new GBeanInfoFactory(OpenEJBModuleBuilder.class);
         infoFactory.addAttribute("kernel", Kernel.class, false);
         infoFactory.setConstructor(new String[]{"kernel"});
         infoFactory.addInterface(ModuleBuilder.class);
         infoFactory.addInterface(EJBReferenceBuilder.class);
 
         GBEAN_INFO = infoFactory.getBeanInfo();
     }
 
     public static GBeanInfo getGBeanInfo() {
         return GBEAN_INFO;
     }
 }
