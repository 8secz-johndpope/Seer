 /*
  * Copyright 2012 ios-driver committers.
  * 
  * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
  * in compliance with the License. You may obtain a copy of the License at
  * 
  * http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software distributed under the License
  * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
  * or implied. See the License for the specific language governing permissions and limitations under
  * the License.
  */
 package org.uiautomation.ios.utils;
 
 import com.dd.plist.BinaryPropertyListParser;
 import com.dd.plist.NSObject;

import org.apache.commons.io.IOUtils;
 import org.json.JSONObject;
 import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.BeanToJsonConverter;
import org.uiautomation.ios.server.application.LanguageDictionary;
 import org.uiautomation.iosdriver.ApplicationInfo;
 
 import java.io.File;
import java.io.FileInputStream;
import java.io.StringWriter;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
 import java.util.Map;
 
 public class PlistFileUtils {
 
   private final File source;
   private final Map<String, Object> plistContent;
 
   public PlistFileUtils(File source) {
     this.source = source;
     this.plistContent = read(source);
   }
 
 
   private Map<String, Object> read(File bplist) {
     NSObject object = null;
     try {
       object = BinaryPropertyListParser.parse(bplist);
     } catch (Exception e) {
      throw new WebDriverException("Cannot parse info.plist : " + e.getMessage(), e);
     }
     ApplicationInfo info = new ApplicationInfo(object);
     return info.getProperties();
   }
 
   public JSONObject toJSON() throws Exception {
     JSONObject res = new JSONObject(plistContent);
     return res;
   }
 }
