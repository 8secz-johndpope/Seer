 /*
  *
  *
  * Copyright (C) 2005 SIPfoundry Inc.
  * Licensed by SIPfoundry under the LGPL license.
  * 
  * Copyright (C) 2005 snom technology AG
  * Licensed to SIPfoundry under a Contributor Agreement.
  * 
  * $
  */
 package org.sipfoundry.sipxconfig.phone.snom;
 
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Iterator;
 import java.util.List;
 
 import org.apache.commons.lang.StringUtils;
 import org.sipfoundry.sipxconfig.common.User;
 import org.sipfoundry.sipxconfig.device.DeviceDefaults;
 import org.sipfoundry.sipxconfig.device.DeviceTimeZone;
 import org.sipfoundry.sipxconfig.phone.Line;
 import org.sipfoundry.sipxconfig.phone.LineInfo;
 import org.sipfoundry.sipxconfig.phone.Phone;
 import org.sipfoundry.sipxconfig.setting.Setting;
 import org.sipfoundry.sipxconfig.setting.SettingEntry;
 
 public class SnomPhone extends Phone {
     public static final String BEAN_ID = "snom";
 
     private static final String USER_HOST = "line/user_host";
     // suspiciously, no registration server port?
 
     private static final String MAILBOX = "line/user_mailbox";
     private static final String OUTBOUND_PROXY = "sip/user_outbound";
     private static final String USER_NAME = "line/user_name";
     private static final String PASSWORD = "line/user_pass";
     private static final String AUTH_NAME = "line/user_pname";
     private static final String DISPLAY_NAME = "line/user_realname";
 
     private static final String TIMEZONE_SETTING = "network/utc_offset";
     private static final String CONFIG_URL = "update/setting_server";
     private static final String DST_SETTING = "network/dst";
     private static final String UDP_TRANSPORT_TAG = ";transport=udp";
 
     public SnomPhone() {
         super(BEAN_ID);
         init();
     }
 
     public SnomPhone(SnomModel model) {
         super(model);
         init();
     }
 
     private void init() {
         setPhoneTemplate("snom/snom.vm");
     }
 
     @Override
     public void initialize() {
         SnomDefaults defaults = new SnomDefaults(getPhoneContext().getPhoneDefaults(), this);
         addDefaultBeanSettingHandler(defaults);
     }
 
     @Override
     public void initializeLine(Line line) {
         SnomLineDefaults defaults = new SnomLineDefaults(getPhoneContext().getPhoneDefaults(),
                 line);
         line.addDefaultBeanSettingHandler(defaults);
     }
 
     @Override
     protected void setLineInfo(Line line, LineInfo externalLine) {
         line.setSettingValue(DISPLAY_NAME, externalLine.getDisplayName());
         line.setSettingValue(USER_NAME, externalLine.getUserId());
         line.setSettingValue(PASSWORD, externalLine.getPassword());
         line.setSettingValue(USER_HOST, externalLine.getRegistrationServer());
         line.setSettingValue(MAILBOX, externalLine.getVoiceMail());
     }
 
     @Override
     protected LineInfo getLineInfo(Line line) {
         LineInfo lineInfo = new LineInfo();
         lineInfo.setUserId(line.getSettingValue(USER_NAME));
         lineInfo.setDisplayName(line.getSettingValue(DISPLAY_NAME));
         lineInfo.setPassword(line.getSettingValue(PASSWORD));
         lineInfo.setRegistrationServer(line.getSettingValue(USER_HOST));
         lineInfo.setVoiceMail(line.getSettingValue(MAILBOX));
         return lineInfo;
     }
 
     public String getPhoneFilename() {
         return getWebDirectory() + "/" + getProfileName();
     }
 
     String getProfileName() {
         String serialNumber = getSerialNumber();
         StringBuffer buffer = new StringBuffer(serialNumber.toUpperCase());
         buffer.append(".htm");
         return buffer.toString();
     }
 
     public int getMaxLineCount() {
         return getModel().getMaxLineCount();
     }
 
     public Collection<Setting> getProfileLines() {
         int lineCount = getModel().getMaxLineCount();
         List<Setting> linesSettings = new ArrayList<Setting>(getMaxLineCount());
 
         Collection<Line> lines = getLines();
         int i = 0;
         Iterator<Line> ilines = lines.iterator();
         for (; ilines.hasNext() && (i < lineCount); i++) {
             linesSettings.add(ilines.next().getSettings());
         }
 
         for (; i < lineCount; i++) {
             Line line = createLine();
             line.setPhone(this);
             line.setPosition(i);
             linesSettings.add(line.getSettings());
         }
 
         return linesSettings;
     }
 
     public static class SnomDefaults {
         private DeviceDefaults m_defaults;
         private SnomPhone m_phone;
 
         SnomDefaults(DeviceDefaults defaults, SnomPhone phone) {
             m_defaults = defaults;
             m_phone = phone;
         }
 
         @SettingEntry(path = CONFIG_URL)
         public String getConfigUrl() {
             String configUrl = m_defaults.getProfileRootUrl() + '/' + m_phone.getProfileName();
             return configUrl;
         }
 
         @SettingEntry(path = TIMEZONE_SETTING)
         public String getTimeZoneOffset() {
             int tzsec = m_defaults.getTimeZone().getOffset();
 
             if (tzsec <= 0) {
                 return String.valueOf(tzsec);
             }
 
             return '+' + String.valueOf(tzsec);
         }
 
         @SettingEntry(path = DST_SETTING)
         public String getDstSetting() {
             DeviceTimeZone zone = m_defaults.getTimeZone();
             if (zone.getDstOffset() == 0) {
                 return null;
             }
 
             String dst = String.format("%d %02d.%02d.%02d %02d:00:00 %02d.%02d.%02d %02d:00:00",
                     zone.getDstOffset(), zone.getStartMonth(), Math.min(zone.getStartWeek(), 5),
                     (zone.getStartDayOfWeek() + 5) % 7 + 1, zone.getStartTime() / 3600, zone
                             .getStopMonth(), Math.min(zone.getStopWeek(), 5), (zone
                             .getStopDayOfWeek() + 5) % 7 + 1, zone.getStopTime() / 3600);
             return dst;
         }
     }
 
     public static class SnomLineDefaults {
         private Line m_line;
         private DeviceDefaults m_defaults;
 
         SnomLineDefaults(DeviceDefaults defaults, Line line) {
             m_line = line;
             m_defaults = defaults;
         }
 
         @SettingEntry(path = USER_HOST)
         public String getUserHost() {
             String registrationUri = StringUtils.EMPTY;
             User u = m_line.getUser();
             if (u != null) {
                 String domainName = m_defaults.getDomainName();
                 registrationUri = domainName + UDP_TRANSPORT_TAG;
             }
             return registrationUri;
         }
 
         @SettingEntry(path = OUTBOUND_PROXY)
         public String getOutboundProxy() {
             String outboundProxy = null;
             User user = m_line.getUser();
             if (user != null) {
                 // XPB-398 This forces TCP, this is defined in conjunction with "transport=udp"
                 // This is benign w/o SRV, but required w/SRV
                 outboundProxy = m_defaults.getFullyQualifiedDomainName();
             }
 
             return outboundProxy;
         }
 
         @SettingEntry(path = MAILBOX)
         public String getMailbox() {
             // XCF-722 Setting this to the mailbox (e.g. 101) would fix issue
             // where mailbox button on phone calls voicemail server, but would
             // break MWI subscription because SUBSCRIBE message fails
             // authentication unless this value is user's username
             return getUserName();
         }
 
        @SettingEntry(paths = { USER_NAME, AUTH_NAME })
         public String getUserName() {
             String username = null;
             User user = m_line.getUser();
             if (user != null) {
                 username = user.getUserName();
             }
 
             return username;
         }
 
         @SettingEntry(path = PASSWORD)
         public String getPassword() {
             String password = null;
             User user = m_line.getUser();
             if (user != null) {
                 password = user.getSipPassword();
             }
 
             return password;
         }
 
         @SettingEntry(path = DISPLAY_NAME)
         public String getDisplayName() {
             String displayName = null;
             User user = m_line.getUser();
             if (user != null) {
                 displayName = user.getDisplayName();
             }
 
             return displayName;
         }
 
         @SettingEntry(path = "sip/user_moh")
         public String getUserMoh() {
             return m_defaults.getSipxServer().getMusicOnHoldUri();
         }
     }
 
     public void restart() {
         sendCheckSyncToFirstLine();
     }
 }
