 /*
  * Copyright 2013 Red Hat, Inc. and/or its affiliates.
  *
  * Licensed under the Eclipse Public License version 1.0, available at
  * http://www.eclipse.org/legal/epl-v10.html
  */
 package org.jboss.forge.furnace.impl.addons;
 
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 import java.util.ServiceLoader;
 import java.util.Set;
 import java.util.concurrent.Callable;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 import org.jboss.forge.furnace.Furnace;
 import org.jboss.forge.furnace.addons.Addon;
 import org.jboss.forge.furnace.addons.AddonDependency;
 import org.jboss.forge.furnace.exception.ContainerException;
 import org.jboss.forge.furnace.impl.util.Iterators;
 import org.jboss.forge.furnace.lifecycle.AddonLifecycleProvider;
 import org.jboss.forge.furnace.lifecycle.ControlType;
 import org.jboss.forge.furnace.repositories.AddonRepository;
 import org.jboss.forge.furnace.util.Addons;
 import org.jboss.forge.furnace.util.ClassLoaders;
 
 /**
  * Loads an {@link Addon}
  */
 public final class AddonRunnable implements Runnable
 {
 
    private static final Logger logger = Logger.getLogger(AddonRunnable.class.getName());
 
    private boolean shutdownRequested = false;
    private Furnace furnace;
    private Addon addon;
 
    private AddonLifecycleManager lifecycleManager;
    private AddonStateManager stateManager;
 
    private AddonLifecycleProviderEntry lifecycleProviderEntry;
 
    public AddonRunnable(Furnace furnace, AddonLifecycleManager lifecycleManager, AddonStateManager stateManager,
             Addon addon)
    {
       this.lifecycleManager = lifecycleManager;
       this.stateManager = stateManager;
       this.furnace = furnace;
       this.addon = addon;
    }
 
    @Override
    public void run()
    {
       Thread currentThread = Thread.currentThread();
       String name = currentThread.getName();
       currentThread.setName(addon.getId().toCoordinates());
       try
       {
          logger.info("> Starting container [" + addon.getId() + "] [" + addon.getRepository().getRootDirectory() + "]");
          long start = System.currentTimeMillis();
 
          lifecycleProviderEntry = detectLifecycleProvider();
          if (lifecycleProviderEntry != null)
          {
             final AddonLifecycleProvider lifecycleProvider = lifecycleProviderEntry.getProvider();
             ClassLoaders.executeIn(addon.getClassLoader(), new Callable<Void>()
             {
                @Override
                public Void call() throws Exception
                {
                   lifecycleProvider.initialize(furnace, furnace.getAddonRegistry(getRepositories()),
                            lifecycleProviderEntry.getAddon());
                   lifecycleProvider.start(addon);
                   stateManager.setServiceRegistry(addon, lifecycleProvider.getServiceRegistry(addon));
 
                   for (AddonDependency dependency : addon.getDependencies())
                   {
                      if (dependency.getDependency().getStatus().isLoaded())
                         Addons.waitUntilStarted(dependency.getDependency());
                   }
 
                   lifecycleProvider.postStartup(addon);
                   return null;
                }
             });
          }
 
          logger.info(">> Started container [" + addon.getId() + "] - " + (System.currentTimeMillis() - start) + "ms");
 
       }
       catch (Throwable e)
       {
          addon.getFuture().cancel(false);
 
          Level level = Level.FINEST;
          if (!shutdownRequested)
             level = Level.SEVERE;
 
          logger.log(level, "Failed to start addon [" + addon.getId() + "] with classloader ["
                   + stateManager.getClassLoaderOf(addon)
                   + "]", e);
       }
       finally
       {
          lifecycleManager.finishedStarting(addon);
          currentThread.setName(name);
          currentThread.setContextClassLoader(null);
       }
    }
 
    protected AddonRepository[] getRepositories()
    {
       Set<AddonRepository> repositories = stateManager.getViewsOf(addon).iterator().next().getRepositories();
       return repositories.toArray(new AddonRepository[] {});
    }
 
    public void shutdown()
    {
       shutdownRequested = true;
       try
       {
          logger.info("< Stopping container [" + addon.getId() + "] [" + addon.getRepository().getRootDirectory() + "]");
          long start = System.currentTimeMillis();
 
          if (lifecycleProviderEntry != null)
          {
             final AddonLifecycleProvider lifecycleProvider = lifecycleProviderEntry.getProvider();
             ClassLoaders.executeIn(addon.getClassLoader(), new Callable<Void>()
             {
                @Override
                public Void call() throws Exception
                {
                   try
                   {
                      lifecycleProvider.postStartup(addon);
                   }
                   catch (Throwable e)
                   {
                     logger.log(Level.SEVERE, "Failed to execute pre-shutdown task for [" + addon + "]", e);
                   }
                   lifecycleProvider.stop(addon);
                   return null;
                }
             });
          }
 
          logger.info("<< Stopped container [" + addon.getId() + "] - " + (System.currentTimeMillis() - start) + "ms");
       }
       catch (Throwable e)
       {
          logger.log(Level.FINE, "Failed to shut down addon " + addon.getId(), e);
       }
       finally
       {
          Thread.currentThread().setContextClassLoader(null);
       }
    }
 
    private AddonLifecycleProviderEntry detectLifecycleProvider()
    {
       AddonLifecycleProviderEntry result = null;
       result = detectLifecycleProviderLocal();
       if (result == null)
          result = detectLifecycleProviderDependencies();
       return result;
    }
 
    private AddonLifecycleProviderEntry detectLifecycleProviderLocal()
    {
       AddonLifecycleProviderEntry result = null;
       final ClassLoader classLoader = addon.getClassLoader();
       try
       {
          result = ClassLoaders.executeIn(classLoader, new Callable<AddonLifecycleProviderEntry>()
          {
             @Override
             public AddonLifecycleProviderEntry call() throws Exception
             {
                AddonLifecycleProviderEntry result = null;
 
                ServiceLoader<AddonLifecycleProvider> serviceLoader = ServiceLoader.load(
                         AddonLifecycleProvider.class, classLoader);
 
                Iterator<AddonLifecycleProvider> iterator = serviceLoader.iterator();
                if (serviceLoader != null && iterator.hasNext())
                {
                   AddonLifecycleProvider provider = iterator.next();
 
                   if (ClassLoaders.ownsClass(classLoader, provider.getClass()))
                   {
                      if (ControlType.ALL.equals(provider.getControlType()))
                      {
                         result = new AddonLifecycleProviderEntry(addon, provider);
                      }
                      if (ControlType.SELF.equals(provider.getControlType()))
                      {
                         result = new AddonLifecycleProviderEntry(addon, provider);
                      }
 
                      if (result != null && iterator.hasNext())
                      {
                         throw new ContainerException("Expected only one [" + AddonLifecycleProvider.class.getName()
                                  + "] but found multiple. Remove all but one redundant container implementations: " +
                                  Iterators.asList(serviceLoader));
                      }
                   }
                }
                return result;
             }
          });
       }
       catch (Throwable e)
       {
          // FIXME Figure out why ServiceLoader is trying to load things from the wrong ClassLoader
          logger.log(Level.FINEST, "ServiceLoader misbehaved when loading AddonLifecycleProvider instances.", e);
       }
 
       return result;
    }
 
    private AddonLifecycleProviderEntry detectLifecycleProviderDependencies()
    {
       List<AddonLifecycleProviderEntry> results = new ArrayList<AddonRunnable.AddonLifecycleProviderEntry>();
 
       for (AddonDependency addonDependency : addon.getDependencies())
       {
          final Addon dependency = addonDependency.getDependency();
          final ClassLoader classLoader = dependency.getClassLoader();
          try
          {
             AddonLifecycleProviderEntry result = null;
 
             ServiceLoader<AddonLifecycleProvider> serviceLoader = ServiceLoader.load(
                      AddonLifecycleProvider.class, classLoader);
 
             Iterator<AddonLifecycleProvider> iterator = serviceLoader.iterator();
             if (serviceLoader != null && iterator.hasNext())
             {
                AddonLifecycleProvider provider = iterator.next();
                if (ClassLoaders.ownsClass(classLoader, provider.getClass()))
                {
                   if (ControlType.ALL.equals(provider.getControlType()))
                   {
                      result = new AddonLifecycleProviderEntry(dependency, provider);
                   }
                   if (ControlType.DEPENDENTS.equals(provider.getControlType()))
                   {
                      result = new AddonLifecycleProviderEntry(dependency, provider);
                   }
 
                   if (result != null && iterator.hasNext())
                   {
                      throw new ContainerException(
                               "Expected only one ["
                                        + AddonLifecycleProvider.class.getName()
                                        + "] but found multiple. Remove all but one redundant container implementations: "
                                        +
                                        Iterators.asList(serviceLoader));
                   }
                }
             }
 
             if (result != null)
                results.add(result);
          }
          catch (Throwable e)
          {
             // FIXME Figure out why ServiceLoader is trying to load things from the wrong ClassLoader
             logger.log(Level.FINEST, "ServiceLoader misbehaved when loading AddonLifecycleProvider instances.", e);
          }
       }
 
       if (results.size() > 1)
       {
          throw new ContainerException("Expected only one [" + AddonLifecycleProvider.class.getName()
                   + "] but found multiple. Remove all but one redundant container implementations: " +
                   results);
       }
 
       return results.isEmpty() ? null : results.get(0);
    }
 
    @Override
    public String toString()
    {
       return addon.toString();
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
 
    private class AddonLifecycleProviderEntry
    {
       private AddonLifecycleProvider provider;
       private Addon addon;
 
       public AddonLifecycleProviderEntry(Addon addon, AddonLifecycleProvider value)
       {
          this.provider = value;
          this.addon = addon;
       }
 
       public AddonLifecycleProvider getProvider()
       {
          return provider;
       }
 
       public Addon getAddon()
       {
          return addon;
       }
 
       @Override
       public String toString()
       {
          return "[" + addon + " -> " + provider + "]";
       }
    }
 }
