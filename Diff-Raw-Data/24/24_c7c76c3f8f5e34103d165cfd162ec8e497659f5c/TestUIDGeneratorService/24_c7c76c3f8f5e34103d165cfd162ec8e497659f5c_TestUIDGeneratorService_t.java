 /*
  * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
  * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
  */
 
 package org.nuxeo.ecm.platform.uidgen;
 
 import java.util.Calendar;
 import java.util.GregorianCalendar;
 import java.util.HashMap;
 import java.util.Map;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.nuxeo.ecm.core.api.DocumentModel;
 import org.nuxeo.ecm.core.api.impl.DataModelImpl;
 import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
 import org.nuxeo.ecm.core.listener.CoreEventListenerService;
 import org.nuxeo.ecm.core.listener.EventListener;
 import org.nuxeo.ecm.core.schema.SchemaManager;
 import org.nuxeo.ecm.core.schema.SchemaNames;
 import org.nuxeo.ecm.core.schema.TypeRef;
 import org.nuxeo.ecm.core.schema.types.QName;
 import org.nuxeo.ecm.core.schema.types.SchemaImpl;
 import org.nuxeo.ecm.core.schema.types.Type;
 import org.nuxeo.ecm.core.schema.types.primitives.StringType;
 import org.nuxeo.ecm.platform.uidgen.service.ServiceHelper;
 import org.nuxeo.ecm.platform.uidgen.service.UIDGeneratorService;
 import org.nuxeo.runtime.api.Framework;
 import org.nuxeo.runtime.test.NXRuntimeTestCase;
 
 public class TestUIDGeneratorService extends NXRuntimeTestCase {
 
     final Log log = LogFactory.getLog(TestUIDGeneratorService.class);
 
     @Override
     protected void setUp() throws Exception {
         super.setUp();
 
         deployBundle("org.nuxeo.ecm.core.schema");
         deployBundle("org.nuxeo.ecm.core"); // for dublincore
         // define geide schema
         SchemaImpl sch = new SchemaImpl("geide");
         sch.addField(QName.valueOf("application_emetteur"), new TypeRef<Type>(SchemaNames.BUILTIN, StringType.ID));
         sch.addField(QName.valueOf("atelier_emetteur"), new TypeRef<Type>(SchemaNames.BUILTIN, StringType.ID));
         Framework.getLocalService(SchemaManager.class).registerSchema(sch);
 
         deployContrib("org.nuxeo.ecm.platform.uidgen.core.tests",
                 "nxuidgenerator-bundle.xml");
         deployContrib("org.nuxeo.ecm.platform.uidgen.core.tests",
                 "nxuidgenerator-bundle-contrib.xml");
 
     }
 
     private static CoreEventListenerService getListenerService() {
        return Framework.getLocalService(CoreEventListenerService.class);
     }
 
     public void testServiceRegistration() {
         CoreEventListenerService listenerService = getListenerService();
         assertNotNull(listenerService);
         EventListener dcListener = listenerService.getEventListenerByName("uidlistener");
         assertNotNull(dcListener);
         log.info("UIDGenerator listener registered");
     }
 
     public void testStorageService() {
         UIDGeneratorService service = ServiceHelper.getUIDGeneratorService();
         assertNotNull(service);
     }
 
     private static DocumentModel createDocumentModel(String type) throws Exception {
         DocumentModelImpl docModel = new DocumentModelImpl(type);
         Map<String, Object> dcMap = new HashMap<String, Object>();
         dcMap.put("title", null);
         dcMap.put("description", null);
         docModel.addDataModel(new DataModelImpl("dublincore", dcMap));
         Map<String, Object> geideMap = new HashMap<String, Object>();
         geideMap.put("application_emetteur", null);
         docModel.addDataModel(new DataModelImpl("geide", geideMap));
         return docModel;
     }
 
     public void testUIDGenerator() throws Exception {
         String docTypeName = "GeideDoc";
         // create Geide doc
         DocumentModel gdoc = createDocumentModel(docTypeName);
         gdoc.setProperty("dublincore", "title", "testGdoc_Title");
         gdoc.setProperty("dublincore", "description", "testGdoc_description");
         gdoc.setProperty("geide", "application_emetteur", "T4");
 
         UIDGeneratorService service = ServiceHelper.getUIDGeneratorService();
         String uid = service.createUID(gdoc);
 
         final int year = new GregorianCalendar().get(Calendar.YEAR);
         final String expected = "T4" + year + "00001";
         assertEquals(expected, uid);
     }
 
 }
