 /*
  * (C) Copyright 2002-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
  *
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the GNU Lesser General Public License
  * (LGPL) version 2.1 which accompanies this distribution, and is available at
  * http://www.gnu.org/licenses/lgpl.html
  *
  * This library is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * Contributors:
  *     Nuxeo - initial API and implementation
  *
  */
 
 package org.nuxeo.ecm.core.convert.tests;
 
 import java.io.File;
 
 import org.nuxeo.common.utils.FileUtils;
 import org.nuxeo.ecm.core.api.Blob;
 import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
 import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
 import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
 import org.nuxeo.ecm.core.convert.api.ConversionService;
 import org.nuxeo.ecm.core.convert.cache.ConversionCacheGCManager;
 import org.nuxeo.ecm.core.convert.cache.ConversionCacheHolder;
 import org.nuxeo.ecm.core.convert.extension.Converter;
 import org.nuxeo.ecm.core.convert.service.ConversionServiceImpl;
 import org.nuxeo.runtime.api.Framework;
 import org.nuxeo.runtime.test.NXRuntimeTestCase;
 
 public class TestCGCache extends NXRuntimeTestCase {
     
     ConversionService cs; 
 
     @Override
     public void setUp() throws Exception {
         super.setUp();
         deployBundle("org.nuxeo.ecm.core.api");
         deployBundle("org.nuxeo.ecm.core.convert.api");
         deployBundle("org.nuxeo.ecm.core.convert");
         deployContrib("org.nuxeo.ecm.core.convert.tests", "OSGI-INF/convert-service-config-enabled-gc.xml");
         cs = Framework.getLocalService(ConversionService.class);
     }
 
     
     public void testCGTask() throws Exception {
         int noRuns = ConversionCacheGCManager.getGCRuns();
         Converter cv = deployConverter();
         assertNotNull(cv);
 
         int cacheSize1 = ConversionCacheHolder.getNbCacheEntries();
         BlobHolder bh = getBlobHolder();
         BlobHolder result = cs.convert("identity", bh, null);
         assertNotNull(result);
         
         int cacheSize2 = ConversionCacheHolder.getNbCacheEntries();
         // check new cache entry was created
         assertEquals(1, cacheSize2 - cacheSize1);
 
         //  fire the frameworkStarted event and make sure that the GC
         // has run and that the cache is empty
         fireFrameworkStarted();
         // wait for the CHTread to run
        while ( ConversionCacheGCManager.getGCRuns() == noRuns) {
             Thread.sleep(1000);
         }
 
         int cacheSize3 = ConversionCacheHolder.getNbCacheEntries();
         assertEquals(0, cacheSize3);
     }
     
     private Converter deployConverter() throws Exception{
         deployContrib("org.nuxeo.ecm.core.convert.tests", "OSGI-INF/converters-test-contrib3.xml");
         return ConversionServiceImpl.getConverter("identity");
     }
     
     private BlobHolder getBlobHolder(){
         File file = FileUtils.getResourceFileFromContext("test-data/hello.doc");
         assertNotNull(file);
         assertTrue(file.length() > 0);
         Blob blob = new FileBlob(file);
         blob.setFilename("hello.doc");
         blob.setMimeType("application/msword");
         return new SimpleBlobHolder(blob);
 
     }
   
 }
