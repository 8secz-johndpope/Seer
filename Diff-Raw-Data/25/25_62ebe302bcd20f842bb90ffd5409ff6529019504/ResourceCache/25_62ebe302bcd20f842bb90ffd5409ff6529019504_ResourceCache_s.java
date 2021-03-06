 package com.mangofactory.bakehouse.core;
 
 import java.util.List;
 import java.util.Map;
 
 import lombok.Data;
 import lombok.SneakyThrows;
 import lombok.extern.slf4j.Slf4j;
 
 import org.apache.commons.vfs2.FileChangeEvent;
 import org.apache.commons.vfs2.FileListener;
 import org.apache.commons.vfs2.FileObject;
 import org.apache.commons.vfs2.FileSystemException;
 import org.apache.commons.vfs2.FileSystemManager;
 import org.apache.commons.vfs2.VFS;
 import org.apache.commons.vfs2.impl.DefaultFileMonitor;
 
 import com.google.common.collect.Lists;
 import com.google.common.collect.Maps;
 import com.mangofactory.bakehouse.config.BakehouseConfig;
 import com.mangofactory.bakehouse.core.io.FileManager;
 import com.mangofactory.bakehouse.core.io.FilePath;
 
 @Slf4j
 public class ResourceCache {
 
 	private Map<String, List<ResourceProcessor>> configurationMap = Maps.newHashMap();
 	private Map<String, CachedResource> cache = Maps.newHashMap();
 	private CacheInvalidatingFileListener fileListener;
 	private DefaultFileMonitor fileMonitor;
 	private FileSystemManager fileSystemManager;
 	private final FileManager fileManager;
 	public ResourceCache(FileManager fileManager) throws FileSystemException {
 		this(VFS.getManager(), fileManager);
 	}
 	
 	// TODO: Issue #3 - Consolidate FileManager and FileSystemManager 
 	public ResourceCache(FileSystemManager fileSystemManager, FileManager fileManager)
 	{
 		this.fileManager = fileManager;
 		fileListener = new CacheInvalidatingFileListener(this);
 		fileMonitor = new DefaultFileMonitor(fileListener);
 		fileMonitor.start();
 		this.fileSystemManager = fileSystemManager;
 	}
 	public void setConfiguration(String configurationName, List<ResourceProcessor> processors)
 	{
 		if (configurationMap.containsKey(configurationName))
 			configurationMap.remove(configurationName);
 		addConfiguration(configurationName, processors);
 	}
 	public void setConfiguration(String configurationName, ResourceProcessor... processors)
 	{
 		List<ResourceProcessor> processorList = Lists.newArrayList(processors);
 		setConfiguration(configurationName, processorList);
 	}
 	public void addConfiguration(String configurationName, ResourceProcessor... processors)
 	{
 		List<ResourceProcessor> processorList = Lists.newArrayList(processors);
 		addConfiguration(configurationName, processorList);
 	}
 	public void addConfiguration(String configurationName,
 			List<ResourceProcessor> processorList) {
 		if (configurationMap.containsKey(configurationName))
 		{
 			configurationMap.get(configurationName).addAll(processorList);
 		} else {
 			configurationMap.put(configurationName, processorList);
 		}
 	}
 	public Resource getResourceGroup(String configuration, String type, List<FilePath> resourcePaths)
 	{
 		if (cache.containsKey(configuration))
 		{
 			log.info("Serving resource '{}' from cache",configuration);
 			return cache.get(configuration).getResource();
 		} else {
 			BuildResourceRequest request = new BuildResourceRequest(configuration, type, resourcePaths);
 			return buildResource(request);
 		}
 	}
 	private Resource buildResource(BuildResourceRequest request) {
 		String configuration = request.getConfiguration();
 		log.info("Building resource '{}'", configuration);
 		List<FilePath> filePathsToMonitor = Lists.newArrayList(request.getResourcePaths());
 		Resource resource = getDefaultResource(request.getResourcePaths(),request.getType());
 		if (configurationMap.containsKey(configuration))
 		{
 			List<ResourceProcessor> processors = configurationMap.get(configuration);
 			for (ResourceProcessor resourceProcessor : processors) {
 				resource = resourceProcessor.process(resource);
 				filePathsToMonitor.addAll(resourceProcessor.getAdditionalFilesToMonitor());
 			}
 		} else {
 			log.warn("No matching configuration defined for '{}'. Using default resource",configuration);
 		}
 		
 		List<FilePath> servletRelativePaths = fileManager.makeServletRelative(resource.getResourcePaths());
 		resource = resource.setResourcePaths(servletRelativePaths);
 		
 		log.info("Caching resource '{}'", configuration);
 		cache(request,resource);
 		watchPaths(filePathsToMonitor,configuration);
 		return resource;
 	}
 	@SneakyThrows
 	private void watchPaths(List<FilePath> resourcePaths, String configuration) {
 		fileListener.addFiles(configuration, resourcePaths);
 		for (FilePath resourcePath : resourcePaths) {
 			FileObject fileObject = fileSystemManager.resolveFile(resourcePath.getPath());
 			fileMonitor.addFile(fileObject);
 			log.info("Now watching {} for changes",resourcePath.getPath());
 		}
 	}
 	private void cache(BuildResourceRequest request, Resource resource) {
 		CachedResource cachedResource = new CachedResource(resource.getResourceType(), resource, request);
 		cache.put(request.getConfiguration(), cachedResource);
 	}
 	private Resource getDefaultResource(List<FilePath> resourcePaths, String resourceType) {
 		DefaultResource resource = DefaultResource.fromPaths(resourcePaths,resourceType);
 		return resource;
 	}
 	
 	public void invalidate(String configuration) {
 		CachedResource cachedResource = cache.remove(configuration);
 		if (cachedResource != null)
 		{
 			log.info("Cache of '{}' invalidated - rebuilding",configuration);
 			buildResource(cachedResource.request);
 		} else {
 			log.error("Cannot invalidate configuration '{}' as was not found in the cache",configuration);
 		}
 	}
 	
 	@Data
 	class BuildResourceRequest {
 		private final String configuration;
 		private final String type;
 		private final List<FilePath> resourcePaths;
 	}
 	@Data
 	class CachedResource {
 		private final String type;
 		private final Resource resource;
 		private final BuildResourceRequest request;
 	}
 	
 	class CacheInvalidatingFileListener implements FileListener
 	{
 		private final ResourceCache cache;
 		private final Map<FilePath, String> filesToConfiguration;
 		
 		public CacheInvalidatingFileListener(ResourceCache cache)
 		{
 			this.cache = cache;
 			filesToConfiguration = Maps.newHashMap();
 		}
 		public void addFiles(String configuration, List<FilePath> files)
 		{
 			for (FilePath filePath : files) {
 				filesToConfiguration.put(filePath, configuration);
 			}
 		}
 		public void fileCreated(FileChangeEvent event) throws Exception {
 			handleEvent(event); 
 		}
 
 		public void fileDeleted(FileChangeEvent event) throws Exception {
 			handleEvent(event); 			
 		}
 
 		public void fileChanged(FileChangeEvent event) throws Exception {
 			handleEvent(event); 			
 		}
 		private void handleEvent(FileChangeEvent event) {
 			FilePath filePath = FilePath.forAbsolutePath(event.getFile().getName().getPath());
 			if (filesToConfiguration.containsKey(filePath))
 			{
 				String configuration = filesToConfiguration.get(filePath);
 				log.info("File '{}' changed - invalidating cache for configuration '{}'",event.getFile().getName().getBaseName(),configuration);
 				cache.invalidate(configuration);
 			} else {
 				log.error("File '{}' changed - but is not associated with any configuration - ignoring", event.getFile().getName().getBaseName());
 			}
 		}
 	}
 
 	public void configureProcessors(BakehouseConfig bakehouseConfig) {
 		for (List<ResourceProcessor> processorList : configurationMap.values())
 		{
 			for (ResourceProcessor resourceProcessor : processorList) {
 				resourceProcessor.configure(bakehouseConfig);
 			}
 		}
 	}
 }
 
 
