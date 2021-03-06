 package org.jboss.forge.resource.addon;
 
 import java.io.File;
 
 import org.jboss.forge.addon.resource.DirectoryResource;
 import org.jboss.forge.addon.resource.FileResource;
 import org.jboss.forge.addon.resource.Resource;
 import org.jboss.forge.addon.resource.ResourceFactory;
 import org.jboss.forge.addon.resource.ResourceGenerator;
 import org.jboss.forge.furnace.services.Exported;
 
 @Exported
 public class FileResourceGenerator implements ResourceGenerator<FileResource<?>, File>
 {
    @Override
    public boolean handles(Class<?> type, Object resource)
    {
       if (resource instanceof File)
       {
          return true;
       }
       return false;
    }
 
    @Override
    @SuppressWarnings("unchecked")
    public <T extends Resource<File>> T getResource(ResourceFactory factory, Class<FileResource<?>> type, File resource)
    {
       if (DirectoryResource.class.isAssignableFrom(type) || (resource.exists() && resource.isDirectory()))
          return (T) new DirectoryResourceImpl(factory, resource);
       return (T) new FileResourceImpl(factory, resource);
    }
 
    @Override
    public <T extends Resource<File>> Class<?> getResourceType(Class<FileResource<?>> type, File resource)
    {
       if (DirectoryResource.class.isAssignableFrom(type) || (resource.exists() && resource.isDirectory()))
          return DirectoryResource.class;
      return FileResourceImpl.class;
    }
 }
