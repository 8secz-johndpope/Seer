 /*
  * 
  * 
  * Copyright (C) 2005 SIPfoundry Inc.
  * Licensed by SIPfoundry under the LGPL license.
  * 
  * Copyright (C) 2005 Pingtel Corp.
  * Licensed to SIPfoundry under a Contributor Agreement.
  * 
  * $
  */
 package org.sipfoundry.sipxconfig.gateway.audiocodes;
 
import java.io.InputStream;

 import junit.framework.TestCase;
 
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
 import org.easymock.classextension.EasyMock;
 import org.easymock.classextension.IMocksControl;
 import org.sipfoundry.sipxconfig.TestHelper;
 import org.sipfoundry.sipxconfig.device.BeanFactoryModelSource;
 import org.sipfoundry.sipxconfig.device.DeviceDefaults;
 import org.sipfoundry.sipxconfig.device.MemoryProfileLocation;
 import org.sipfoundry.sipxconfig.gateway.GatewayModel;
 import org.sipfoundry.sipxconfig.setting.Setting;
 import org.sipfoundry.sipxconfig.setting.SettingSet;
 
 public class MediantGatewayTestDb extends TestCase {
     private AudioCodesModel m_model;
     private MediantGateway m_gateway;
 
     protected void setUp() throws Exception {
         BeanFactoryModelSource<GatewayModel> modelSource = (BeanFactoryModelSource<GatewayModel>) TestHelper
                 .getApplicationContext().getBean("nakedGatewayModelSource");
         m_model = (AudioCodesModel) modelSource.getModel("audiocodesMP1X4_4_FXO");
         m_gateway = (MediantGateway) TestHelper.getApplicationContext().getBean(m_model.getBeanId());
         m_gateway.setModelId(m_model.getModelId());
         m_gateway.setSerialNumber("FT0123456");
     }
 
    public void testModel() {
        assertSame(m_model, m_gateway.getModel());        
    }
    
     public void testGenerateProfiles() throws Exception {
         MemoryProfileLocation location = TestHelper.setVelocityProfileGenerator(m_gateway);
         m_gateway.generateProfiles();
 
        InputStream expectedProfile = getClass().getResourceAsStream("mp-gateway.ini");
        assertNotNull(expectedProfile);
        String expected = IOUtils.toString(expectedProfile);
        expectedProfile.close();
        
        String actual = location.toString();

        String expectedLines[] = StringUtils.split(expected, "\n");
        String actualLines[] = StringUtils.split(actual, "\n");

        int len = Math.min(actualLines.length, expectedLines.length);
        for (int i = 0; i < len; i++) {
            assertEquals(expectedLines[i], actualLines[i]);
        }
        assertEquals(expectedLines.length, actualLines.length);        
        assertEquals(expected, actual);
     }
 
     public void testPrepareSettings() throws Exception {
         assertSame(m_model, m_gateway.getModel());
 
         IMocksControl defaultsCtrl = EasyMock.createControl();
         DeviceDefaults defaults = defaultsCtrl.createMock(DeviceDefaults.class);
         defaults.getDomainName();
         defaultsCtrl.andReturn("mysipdomain.com").anyTimes();
         defaults.getProxyServerAddr();
         defaultsCtrl.andReturn("10.1.2.3").atLeastOnce();
 
         defaultsCtrl.replay();
 
         m_gateway.setDefaults(defaults);
 
         assertEquals("10.1.2.3", m_gateway.getSettingValue("SIP/ProxyIP"));
         assertEquals("mysipdomain.com", m_gateway.getSettingValue("SIP/ProxyName"));
 
         defaultsCtrl.verify();
     }
 
     public void testGetSettings() throws Exception {
         Setting settings = m_gateway.getSettings();
         assertEquals("13", settings.getSetting("Voice/MaxDigits").getValue());
         assertTrue(settings instanceof SettingSet);
         SettingSet root = (SettingSet) settings;
         SettingSet currentSettingSet = (SettingSet) root.getSetting("Voice");
         assertEquals("13", currentSettingSet.getSetting("MaxDigits").getValue());
     }
 }
