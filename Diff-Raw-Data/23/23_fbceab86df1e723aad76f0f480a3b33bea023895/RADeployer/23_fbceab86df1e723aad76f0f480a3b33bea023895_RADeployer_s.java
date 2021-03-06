 /*
  * JBoss, Home of Professional Open Source.
  * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
  * as indicated by the @author tags. See the copyright.txt file in the
  * distribution for a full listing of individual contributors.
  *
  * This is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as
  * published by the Free Software Foundation; either version 2.1 of
  * the License, or (at your option) any later version.
  *
  * This software is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this software; if not, write to the Free
  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
  */
 
 package org.jboss.jca.deployers.fungal;
 
 import org.jboss.jca.fungal.deployers.CloneableDeployer;
 import org.jboss.jca.fungal.deployers.DeployException;
 import org.jboss.jca.fungal.deployers.Deployer;
 import org.jboss.jca.fungal.deployers.Deployment;
 import org.jboss.jca.fungal.util.FileUtil;
 import org.jboss.jca.fungal.util.Injection;
 import org.jboss.jca.fungal.util.JarFilter;
 
 import java.io.File;
 import java.io.IOException;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.net.URLClassLoader;
 import java.util.ArrayList;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.concurrent.atomic.AtomicBoolean;
 
 import org.jboss.logging.Logger;
 
 import org.jboss.metadata.rar.jboss.BvGroupMetaData;
 import org.jboss.metadata.rar.jboss.JBossRA20Base;
 import org.jboss.metadata.rar.jboss.JBossRAMetaData;
 import org.jboss.metadata.rar.spec.AdminObjectMetaData;
 import org.jboss.metadata.rar.spec.ConfigPropertyMetaData;
 import org.jboss.metadata.rar.spec.ConnectionDefinitionMetaData;
 import org.jboss.metadata.rar.spec.ConnectorMetaData;
 import org.jboss.metadata.rar.spec.JCA16DTDMetaData;
 import org.jboss.metadata.rar.spec.JCA16DefaultNSMetaData;
 import org.jboss.metadata.rar.spec.JCA16MetaData;
 import org.jboss.metadata.rar.spec.MessageListenerMetaData;
 import org.jboss.papaki.AnnotationRepository;
 import org.jboss.papaki.AnnotationScanner;
 import org.jboss.papaki.AnnotationScannerFactory;
 
 /**
  * The RA deployer for JCA/SJC
  * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
  * @author <a href="mailto:jeff.zhang@jboss.org">Jeff Zhang</a>
  */
public class RADeployer implements CloneableDeployer
 {
    private static Logger log = Logger.getLogger(RADeployer.class);
    private static boolean trace = log.isTraceEnabled();
    
    /** Preform bean validation */
    private static AtomicBoolean beanValidation = new AtomicBoolean(true);
 
    /**
     * Constructor
     */
    public RADeployer()
    {
    }
 
    /**
     * Set if bean validation should be performed
     * @param value The value
     */
    public void setBeanValidation(boolean value)
    {
       beanValidation.set(value);
    }
    
    /**
     * Should bean validation be performed
     * @return True if validation; otherwise false
     */
    public boolean getBeanValidation()
    {
       return beanValidation.get();
    }
    
    /**
     * Deploy
     * @param url The url
     * @param parent The parent classloader
     * @return The deployment
     * @exception DeployException Thrown if an error occurs during deployment
     */
    public Deployment deploy(URL url, ClassLoader parent) throws DeployException
    {
       if (url == null || !(url.toExternalForm().endsWith(".rar") || url.toExternalForm().endsWith(".rar/")))
          return null;
 
       log.debug("Deploying: " + url.toExternalForm());
 
       ClassLoader oldTCCL = SecurityActions.getThreadContextClassLoader();
       try
       {
          File f = new File(url.toURI());
       
          if (!f.exists())
             throw new IOException("Archive " + url.toExternalForm() + " doesnt exists");
 
          File root = null;
 
          if (f.isFile())
          {
             FileUtil fileUtil = new FileUtil();
             File destination = new File(SecurityActions.getSystemProperty("jboss.jca.home"), "/tmp/");
             root = fileUtil.extract(f, destination);
          }
          else
          {
             root = f;
          }
       
          // Create classloader
          URL[] urls = getUrls(root);
          URLClassLoader cl = SecurityActions.createURLCLassLoader(urls, parent);
          SecurityActions.setThreadContextClassLoader(cl);
 
          // Parse metadata
          Metadata metadataHandler = new Metadata();
          ConnectorMetaData cmd = metadataHandler.getStandardMetaData(root);
          JBossRAMetaData jrmd = metadataHandler.getJBossMetaData(root);
          boolean isMetadataComplete = true;
 
          // Process annotations
          if (cmd == null || cmd.is16())
          {
             AnnotationScanner annotationScanner = 
                AnnotationScannerFactory.getStrategy(AnnotationScannerFactory.JAVASSIST_INPUT_STREAM);
             AnnotationRepository annotationRepository = annotationScanner.scan(cl.getURLs(), cl);
 
             isMetadataComplete = false;
             if (cmd != null)
             {
                if (cmd instanceof JCA16MetaData)
                {
                   JCA16MetaData jmd = (JCA16MetaData)cmd;
                   isMetadataComplete = jmd.isMetadataComplete();
                }
                else if (cmd instanceof JCA16DefaultNSMetaData)
                {
                   JCA16DefaultNSMetaData jmd = (JCA16DefaultNSMetaData)cmd;
                   isMetadataComplete = jmd.isMetadataComplete();
                }
                else if (cmd instanceof JCA16DTDMetaData)
                {
                   JCA16DTDMetaData jmd = (JCA16DTDMetaData)cmd;
                   isMetadataComplete = jmd.isMetadataComplete();
                }
             }
             
             if (cmd == null || !isMetadataComplete)
             {
                Annotations annotator = new Annotations();
                cmd = annotator.process(cmd, annotationRepository);
             }
          }
          
          // Validate metadata
          metadataHandler.validate(cmd);
          
          // Merge metadata
          cmd = metadataHandler.merge(cmd, jrmd);
 
          // Create objects
          // And
          // Inject values
          List<Object> objects = new ArrayList<Object>();
          if (cmd != null)
          {
             // ResourceAdapter
             if (cmd.getRa() != null && cmd.getRa().getRaClass() != null)
             {
                initAndInject(cmd.getRa().getRaClass(), 
                      cmd.getRa().getConfigProperty(), objects, cl);
             }
             
             // ManagedConnectionFactory
             if (cmd.getRa() != null &&
                cmd.getRa().getOutboundRa() != null && 
                cmd.getRa().getOutboundRa().getConDefs() != null)
             {
                List<ConnectionDefinitionMetaData> cdMetas = cmd.getRa().getOutboundRa().getConDefs();
                if (cdMetas.size() > 0)
                {
                   //mcfs = new ArrayList<Object>();
                   for (ConnectionDefinitionMetaData cdMeta : cdMetas)
                   {
                      if (cdMeta.getManagedConnectionFactoryClass() != null)
                      {
                         initAndInject(cdMeta.getManagedConnectionFactoryClass(), 
                            cdMeta.getConfigProps(), objects, cl);
                      }
                   }
                }
             }
             // activationspec
             if (cmd.getRa() != null &&
                cmd.getRa().getInboundRa() != null &&
                cmd.getRa().getInboundRa().getMessageAdapter() != null &&
                cmd.getRa().getInboundRa().getMessageAdapter().getMessageListeners() != null)
             {
                List<MessageListenerMetaData> mlMetas = cmd.getRa().getInboundRa().
                   getMessageAdapter().getMessageListeners();
                if (mlMetas.size() > 0)
                {
                   for (MessageListenerMetaData mlMeta : mlMetas)
                   {
                      if (mlMeta.getActivationSpecType() != null && mlMeta.getActivationSpecType().getAsClass() != null)
                      {
                         initAndInject(mlMeta.getActivationSpecType().getAsClass(), 
                            mlMeta.getActivationSpecType().getConfigProps(), objects, cl);
                      }
                   }
                }
             }
 
             //adminobject
             if (cmd.getRa() != null &&
                cmd.getRa().getAdminObjects() != null)
             {
                List<AdminObjectMetaData> aoMetas = cmd.getRa().getAdminObjects();
                if (aoMetas.size() > 0)
                {
                   for (AdminObjectMetaData aoMeta : aoMetas)
                   {
                      if (aoMeta.getAdminObjectImplementationClass() != null)
                      {
                         initAndInject(aoMeta.getAdminObjectImplementationClass(), 
                               aoMeta.getConfigProps(), objects, cl);
                      }
                   }
                }
             }
          }
 
 
          // Bean validation
          if (getBeanValidation())
          {
             JBossRA20Base jrmd20 = null;
             List<Class> groupsClasses = null;
             if (jrmd instanceof JBossRA20Base)
             {
                jrmd20 = (JBossRA20Base)jrmd;
             }
             if (jrmd20 != null && jrmd20.getBvGroupsList() != null && jrmd20.getBvGroupsList().size() > 0)
             {
                BvGroupMetaData bvGroups = jrmd20.getBvGroupsList().get(0);
                groupsClasses = new ArrayList<Class>();
                for (String group : bvGroups.getBvGroups())
                {
                   groupsClasses.add(Class.forName(group, true, cl));
                }
             }
             
             if (objects != null && objects.size() > 0)
             {
                BeanValidation beanValidator = new BeanValidation();
                for (Object mcf : objects)
                {
                   beanValidator.validate(mcf, groupsClasses);
                }
             }
          }
          
          // Activate deployment
 
          log.info("Deployed: " + url.toExternalForm());
 
          return new RADeployment(url, cl);
       }
       catch (DeployException de)
       {
          // Just rethrow
          throw de;
       }
       catch (Throwable t)
       {
          throw new DeployException("Deployment " + url.toExternalForm() + " failed", t);
       }
       finally
       {
          SecurityActions.setThreadContextClassLoader(oldTCCL);
       }
    }
 
    /**
     * initAndInject
     * @param mlMeta
     * @param mcfs
     * @param cl
     * @throws DeployException
     */
    private void initAndInject(String className, List<ConfigPropertyMetaData> cpMetas,
                               List<Object> mcfs, URLClassLoader cl) throws DeployException
    {
       Object mcf = null;
       try 
       {
          Class mcfClass = Class.forName(className, true, cl);
          mcf = mcfClass.newInstance();
          mcfs.add(mcf);
          
          if (mcf != null)
          {
             if (cpMetas != null)
             {
                Injection injector = new Injection();
                for (ConfigPropertyMetaData cpmd : cpMetas)
                {
                   injector.inject(cpmd.getType(), cpmd.getName(), cpmd.getValue(), mcf);
                }
             }
          }
       } 
       catch (ClassNotFoundException e)
       {
          log.trace("can't constractor " + className + " class");
       }
       catch (Throwable t)
       {
          throw new DeployException("Deployment " + className + " failed", t);
       }
 
    }
 
    /**
     * Get the URLs for the directory and all libraries located in the directory
     * @param directrory The directory
     * @return The URLs
     * @exception MalformedURLException MalformedURLException
     * @exception IOException IOException
     */
    private URL[] getUrls(File directory) throws MalformedURLException, IOException
    {
       List<URL> list = new LinkedList<URL>();
 
       if (directory.exists() && directory.isDirectory())
       {
          // Add directory
          list.add(directory.toURI().toURL());
 
          // Add the contents of the directory too
          File[] jars = directory.listFiles(new JarFilter());
 
          if (jars != null)
          {
             for (int j = 0; j < jars.length; j++)
             {
                list.add(jars[j].getCanonicalFile().toURI().toURL());
             }
          }
       }
       return list.toArray(new URL[list.size()]);      
    }
 
    /**
     * Clone
     * @return The copy of the object
     * @exception CloneNotSupportedException Thrown if a copy can't be created
     */
    public Deployer clone() throws CloneNotSupportedException
    {
       return new RADeployer();
    }
 }
