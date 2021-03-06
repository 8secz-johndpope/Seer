 /*
  * Copyright 2013 Red Hat, Inc. and/or its affiliates.
  *
  * Licensed under the Eclipse Public License version 1.0, available at
  * http://www.eclipse.org/legal/epl-v10.html
  */
 package org.jboss.forge.container.impl;
 
 import java.util.concurrent.Callable;
 import java.util.concurrent.Future;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 import javax.enterprise.inject.spi.BeanManager;
 
 import org.jboss.forge.container.Forge;
 import org.jboss.forge.container.addons.Addon;
 import org.jboss.forge.container.addons.Status;
 import org.jboss.forge.container.event.PostStartup;
 import org.jboss.forge.container.event.PreShutdown;
 import org.jboss.forge.container.lock.LockMode;
 import org.jboss.forge.container.modules.AddonResourceLoader;
 import org.jboss.forge.container.modules.ModularURLScanner;
 import org.jboss.forge.container.modules.ModularWeld;
 import org.jboss.forge.container.modules.ModuleScanResult;
 import org.jboss.forge.container.services.ServiceRegistry;
 import org.jboss.forge.container.util.Assert;
 import org.jboss.forge.container.util.BeanManagerUtils;
 import org.jboss.forge.container.util.ClassLoaders;
 import org.jboss.forge.container.util.Threads;
 import org.jboss.weld.environment.se.Weld;
 import org.jboss.weld.environment.se.WeldContainer;
 import org.jboss.weld.resources.spi.ResourceLoader;
 
 /**
  * Loads an {@link Addon}
  */
 public final class AddonRunnable implements Runnable
 {
    private static final Logger logger = Logger.getLogger(AddonRunnable.class.getName());
 
    private Forge forge;
    private AddonImpl addon;
    private AddonContainerStartup container;
 
    private Callable<Object> shutdownCallable = new Callable<Object>()
    {
       @Override
       public Object call() throws Exception
       {
          addon.setStatus(Status.LOADED);
          return null;
       }
    };
 
    public AddonRunnable(Forge forge, AddonImpl addon)
    {
       this.forge = forge;
       this.addon = addon;
    }
 
    public void shutdown()
    {
       try
       {
          forge.getLockManager().performLocked(LockMode.READ, new Callable<Void>()
          {
             @Override
             public Void call() throws Exception
             {
                logger.info("< Stopping container [" + addon.getId() + "]");
                long start = System.currentTimeMillis();
                ClassLoaders.executeIn(addon.getClassLoader(), shutdownCallable);
                logger.info("<< Stopped container [" + addon.getId() + "] - "
                         + (System.currentTimeMillis() - start) + "ms");
                return null;
             }
          });
       }
       catch (RuntimeException e)
       {
          logger.log(Level.SEVERE, "Failed to shut down addon " + addon.getId(), e);
          throw e;
       }
    }
 
    @Override
    public void run()
    {
       Thread currentThread = Thread.currentThread();
       String name = currentThread.getName();
       currentThread.setName(addon.getId().toCoordinates());
       try
       {
          forge.getLockManager().performLocked(LockMode.READ, new Callable<Void>()
          {
             @Override
             public Void call() throws Exception
             {
                logger.info("> Starting container [" + addon.getId() + "]");
                long start = System.currentTimeMillis();
                container = new AddonContainerStartup();
                shutdownCallable = ClassLoaders.executeIn(addon.getClassLoader(), container);
                logger.info(">> Started container [" + addon.getId() + "] - "
                         + (System.currentTimeMillis() - start) + "ms");
                return null;
             }
          });
       }
       catch (RuntimeException e)
       {
          logger.log(Level.SEVERE, "Failed to start addon " + addon.getId(), e);
          throw e;
       }
       finally
       {
          currentThread.setName(name);
       }
    }
 
    public AddonImpl getAddon()
    {
       return addon;
    }
 
    public class AddonContainerStartup implements Callable<Callable<Object>>
    {
       private Future<Object> operation;
 
       @Override
       public Callable<Object> call() throws Exception
       {
          try
          {
             ResourceLoader resourceLoader = new AddonResourceLoader(addon);
             ModularURLScanner scanner = new ModularURLScanner(resourceLoader, "META-INF/beans.xml");
             ModuleScanResult scanResult = scanner.scan();
 
             Callable<Object> shutdownCallback = null;
 
             if (scanResult.getDiscoveredResourceUrls().isEmpty())
             {
                /*
                 * This is an import-only addon and does not require weld, nor provide remote services.
                 */
                addon.setServiceRegistry(new NullServiceRegistry());
                addon.setStatus(Status.STARTED);
 
                shutdownCallback = new Callable<Object>()
                {
                   @Override
                   public Object call() throws Exception
                   {
                      addon.setStatus(Status.LOADED);
                      return null;
                   }
                };
             }
             else
             {
                final Weld weld = new ModularWeld(scanResult);
                WeldContainer container;
                container = weld.initialize();
 
                final BeanManager manager = container.getBeanManager();
                Assert.notNull(manager, "BeanManager was null");
 
                AddonRepositoryProducer repositoryProducer = BeanManagerUtils.getContextualInstance(manager,
                         AddonRepositoryProducer.class);
               repositoryProducer.setRespository(addon.getRepository());
 
                ForgeProducer forgeProducer = BeanManagerUtils.getContextualInstance(manager, ForgeProducer.class);
                forgeProducer.setForge(forge);
 
                AddonProducer addonProducer = BeanManagerUtils.getContextualInstance(manager, AddonProducer.class);
                addonProducer.setAddon(addon);
 
                AddonRegistryProducer addonRegistryProducer = BeanManagerUtils.getContextualInstance(manager,
                         AddonRegistryProducer.class);
                addonRegistryProducer.setRegistry(forge.getAddonRegistry());
 
                ContainerServiceExtension extension = BeanManagerUtils.getContextualInstance(manager,
                         ContainerServiceExtension.class);
                ServiceRegistryProducer serviceRegistryProducer = BeanManagerUtils.getContextualInstance(manager,
                         ServiceRegistryProducer.class);
                serviceRegistryProducer.setServiceRegistry(new ServiceRegistryImpl(addon, manager, extension));
 
                ServiceRegistry registry = BeanManagerUtils.getContextualInstance(manager, ServiceRegistry.class);
                Assert.notNull(registry, "Service registry was null.");
                addon.setServiceRegistry(registry);
 
                logger.info("Services loaded from addon [" + addon.getId() + "] -  " + registry.getServices());
 
                shutdownCallback = new Callable<Object>()
                {
                   @Override
                   public Object call() throws Exception
                   {
                      try
                      {
                         manager.fireEvent(new PreShutdown());
                      }
                      catch (Exception e)
                      {
                         logger.log(Level.SEVERE, "Failed to execute pre-Shutdown event.", e);
                      }
                      finally
                      {
                         addon.setStatus(Status.LOADED);
                         if (operation != null)
                            operation.cancel(true);
                      }
 
                      weld.shutdown();
                      return null;
                   }
                };
 
                operation = Threads.runAsync(new Callable<Object>()
                {
                   @Override
                   public Object call() throws Exception
                   {
                      return forge.getLockManager().performLocked(LockMode.READ, new Callable<Object>()
                      {
                         @Override
                         public Object call() throws Exception
                         {
                            manager.fireEvent(new PostStartup());
                            return null;
                         }
                      });
                   }
                });
 
                addon.setStatus(Status.STARTED);
             }
 
             return shutdownCallback;
          }
          catch (Exception e)
          {
             addon.setStatus(Status.FAILED);
             throw e;
          }
       }
    }
 
    @Override
    public int hashCode()
    {
       final int prime = 31;
       int result = 1;
       result = prime * result + ((addon == null) ? 0 : addon.hashCode());
       return result;
    }
 
    @Override
    public boolean equals(Object obj)
    {
       if (this == obj)
          return true;
       if (obj == null)
          return false;
       if (getClass() != obj.getClass())
          return false;
       AddonRunnable other = (AddonRunnable) obj;
       if (addon == null)
       {
          if (other.addon != null)
             return false;
       }
       else if (!addon.equals(other.addon))
          return false;
       return true;
    }
 }
