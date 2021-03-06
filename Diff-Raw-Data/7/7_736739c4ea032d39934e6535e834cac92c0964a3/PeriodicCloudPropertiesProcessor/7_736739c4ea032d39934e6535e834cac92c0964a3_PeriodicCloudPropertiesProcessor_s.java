 /*
  * Copyright (c) 2013 the original author or authors.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package org.openinfinity.cloud.application.batch.properties;
 
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.IOException;
 import java.util.Collection;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.Map;
 
 import org.openinfinity.cloud.domain.Deployment;
 import org.openinfinity.cloud.domain.SharedProperty;
 import org.openinfinity.cloud.util.filesystem.FileUtil;
 import org.openinfinity.core.annotation.Log;
 import org.openinfinity.core.util.ExceptionUtil;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.batch.item.ItemProcessor;
 import org.springframework.beans.factory.annotation.Value;
 import org.springframework.stereotype.Component;
 
 /**
  * Periodic cloud properties processor turns key value pairs to tmp-files (infrastructure.properties).
  * 
  * @author Ilkka Leinonen
  * @version 1.0.0
  * @since 1.2.0
  */
 @Component("periodicCloudPropertiesProcessor")
 public class PeriodicCloudPropertiesProcessor implements ItemProcessor<Collection<SharedProperty>, Map<File, Deployment>>{
 
 	private static final String EXCEPTION_MESSAGE_TEMPORARY_FILESYSTEM_DOES_NOT_EXIST = "Local temporary file system for storing key-value pairs is not existing. Please refine your setup: ";
 
 	private static final Logger LOGGER = LoggerFactory.getLogger(PeriodicCloudPropertiesProcessor.class);
 		
 	@Value("${localDiskTempFileSystem}")
 	private String localDiskTempFileSystem;
 	
 	@Log
 	@Override
 	public Map<File, Deployment> process(Collection<SharedProperty> sharedProperties) throws Exception {
 		File localDiskTempFileSystemDirectory = new File(localDiskTempFileSystem); 
 		if (! localDiskTempFileSystemDirectory.exists()) {
 			ExceptionUtil.throwSystemException(EXCEPTION_MESSAGE_TEMPORARY_FILESYSTEM_DOES_NOT_EXIST + localDiskTempFileSystem);
 		}
		//int availabilityZone = 0;
 		long organizationId = 0;
 		int instanceId = 0;
 		int clusterId = 0;
 		Date lastModifiedTimeStamp = new Date();
 		StringBuilder contentBuilder = new StringBuilder();
 		populateDeploymentMetadataAndTempFileContent(sharedProperties, organizationId, instanceId, clusterId, lastModifiedTimeStamp, contentBuilder);
 		File tmp = initializeTempFile(localDiskTempFileSystemDirectory, clusterId);
 		FileUtil.store(tmp.getAbsolutePath(), contentBuilder.toString());
 		Deployment deployment = populateDeployment(organizationId, instanceId, clusterId);
 		deployment.setInputStream(new FileInputStream(tmp));
 		Map<File, Deployment> fileAndDeployment = populateMapWithTempFileAndDeployment(tmp, deployment);
 		return fileAndDeployment;
 	}
 
 	private File initializeTempFile(File localDiskTempFileSystemDirectory, int clusterId) throws IOException {
 		StringBuilder tmpFilenameBuilder = new StringBuilder();
 		tmpFilenameBuilder.append(clusterId).append(".properties");
 		File tmp = File.createTempFile(tmpFilenameBuilder.toString(), ".tmp", localDiskTempFileSystemDirectory);
 		tmp.createNewFile();
 		return tmp;
 	}
 
 	private Map<File, Deployment> populateMapWithTempFileAndDeployment(File tmp, Deployment deployment) {
 		Map<File, Deployment> fileAndDeployment = new HashMap<File, Deployment>();
 		fileAndDeployment.put(tmp, deployment);
 		return fileAndDeployment;
 	}
 
 	private Deployment populateDeployment(long organizationId, int instanceId, int clusterId) {
 		Deployment deployment = new Deployment();
 		deployment.setOrganizationId(organizationId);
 		deployment.setInstanceId(instanceId);
 		deployment.setClusterId(clusterId);
 		deployment.setType("properties");
		deployment.setName("infrastructure");
 		return deployment;
 	}
 
 	private void populateDeploymentMetadataAndTempFileContent(Collection<SharedProperty> sharedProperties, long organizationId, int instanceId, int clusterId, Date lastModifiedTimeStamp, StringBuilder contentBuilder) {		
 		for (SharedProperty sharedProperty : sharedProperties) {
 			contentBuilder
 				.append(sharedProperty.getKey())
 				.append("=")
 				.append(sharedProperty.getValue())
 				.append("\n");
			clusterId = sharedProperty.getClusterId();
 			instanceId = sharedProperty.getInstanceId();
 			clusterId = sharedProperty.getClusterId();
 			lastModifiedTimeStamp = sharedProperty.getTimestamp().after(lastModifiedTimeStamp)?sharedProperty.getTimestamp():lastModifiedTimeStamp;
 		}
 	}
 	
 }
