 /*
  * Copyright 2010 FatWire Corporation. All Rights Reserved.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *    http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package com.fatwire.gst.foundation.taglib;
 
 import java.io.IOException;

 import javax.servlet.jsp.JspException;
 import javax.servlet.jsp.tagext.SimpleTagSupport;
 
import org.apache.commons.lang.StringUtils;

 import COM.FutureTense.Interfaces.ICS;
 import COM.FutureTense.JspTags.Root;
 
 import com.fatwire.assetapi.data.AssetId;
 import com.fatwire.gst.foundation.facade.assetapi.asset.ScatteredAssetAccessTemplate;
 
 public class AssetLoad extends SimpleTagSupport {
 
     private String c;
     private String cid;
     private String attributes;
     private String name;
 
     /* (non-Javadoc)
      * @see javax.servlet.jsp.tagext.SimpleTagSupport#doTag()
      */
     @Override
     public void doTag() throws JspException, IOException {
 
         ICS ics = (ICS) this.getJspContext().getAttribute(Root.sICS);
        ScatteredAssetAccessTemplate t = new ScatteredAssetAccessTemplate(ics);
         if (StringUtils.isBlank(c) || StringUtils.isBlank(cid)) {
             if (StringUtils.isBlank(attributes)) {
                 getJspContext().setAttribute(name, t.readCurrent());
             } else {
                 getJspContext().setAttribute(name, t.readCurrent(attributes.split(",")));
             }
 
         } else {
             AssetId id = t.createAssetId(c, cid);
             if (StringUtils.isBlank(attributes)) {
                 getJspContext().setAttribute(name, t.read(id));
             } else {
                 getJspContext().setAttribute(name, t.read(id, attributes.split(",")));
             }
 
         }
 
         super.doTag();
     }
 
     /**
      * @param c the c to set
      */
     public void setC(String c) {
         this.c = c;
     }
 
     /**
      * @param cid the cid to set
      */
     public void setCid(String cid) {
         this.cid = cid;
     }
 
     /**
      * @param attributes the attributes to set
      */
     public void setAttributes(String attributes) {
         this.attributes = attributes;
     }
 
     /**
      * @param name the name to set
      */
     public void setName(String name) {
         this.name = name;
     }
 
 }
