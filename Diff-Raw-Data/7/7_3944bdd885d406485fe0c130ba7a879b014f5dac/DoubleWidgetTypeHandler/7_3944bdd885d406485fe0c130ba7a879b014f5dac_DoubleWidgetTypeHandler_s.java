 /*
  * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
  *     Anahide Tchertchian
  */
 package org.nuxeo.ecm.platform.forms.layout.facelets.plugins;
 
 import javax.faces.component.html.HtmlInputText;
 import javax.faces.component.html.HtmlOutputText;
 import javax.faces.convert.DoubleConverter;
 
 import org.nuxeo.ecm.platform.forms.layout.api.BuiltinWidgetModes;
 import org.nuxeo.ecm.platform.forms.layout.api.Widget;
 import org.nuxeo.ecm.platform.forms.layout.api.exceptions.WidgetException;
 import org.nuxeo.ecm.platform.forms.layout.facelets.FaceletHandlerHelper;
 import org.nuxeo.ecm.platform.forms.layout.facelets.LeafFaceletHandler;
 import org.nuxeo.ecm.platform.forms.layout.facelets.TagConfigFactory;
 
 import com.sun.facelets.FaceletContext;
 import com.sun.facelets.FaceletHandler;
 import com.sun.facelets.tag.CompositeFaceletHandler;
 import com.sun.facelets.tag.TagAttribute;
 import com.sun.facelets.tag.TagAttributes;
 import com.sun.facelets.tag.TagConfig;
 import com.sun.facelets.tag.jsf.ComponentHandler;
 import com.sun.facelets.tag.jsf.ConvertHandler;
 import com.sun.facelets.tag.jsf.ConverterConfig;
import com.sun.facelets.tag.jsf.core.ConvertNumberHandler;
 
 /**
  * Double widget.
  *
  * @author Anahide Tchertchian
  * @since 5.4.1
  */
 public class DoubleWidgetTypeHandler extends AbstractWidgetTypeHandler {
 
     private static final long serialVersionUID = 1L;
 
     @Override
     public FaceletHandler getFaceletHandler(FaceletContext ctx,
             TagConfig tagConfig, Widget widget, FaceletHandler[] subHandlers)
             throws WidgetException {
         FaceletHandlerHelper helper = new FaceletHandlerHelper(ctx, tagConfig);
         String mode = widget.getMode();
         String widgetId = widget.getId();
         String widgetName = widget.getName();
         TagAttributes attributes = helper.getTagAttributes(widgetId, widget);
         FaceletHandler leaf = new LeafFaceletHandler();
         if (BuiltinWidgetModes.EDIT.equals(mode)) {
             ConverterConfig convertConfig = TagConfigFactory.createConverterConfig(
                     tagConfig, new TagAttributes(new TagAttribute[0]), leaf,
                     DoubleConverter.CONVERTER_ID);
            ConvertHandler convert = new ConvertNumberHandler(convertConfig);
             ComponentHandler input = helper.getHtmlComponentHandler(attributes,
                     convert, HtmlInputText.COMPONENT_TYPE, null);
             String msgId = helper.generateMessageId(widgetName);
             ComponentHandler message = helper.getMessageComponentHandler(msgId,
                     widgetId, null);
             FaceletHandler[] handlers = { input, message };
             return new CompositeFaceletHandler(handlers);
         } else {
             // default on text with int converter for other modes
             ConverterConfig convertConfig = TagConfigFactory.createConverterConfig(
                     tagConfig, new TagAttributes(new TagAttribute[0]), leaf,
                     DoubleConverter.CONVERTER_ID);
            ConvertHandler convert = new ConvertNumberHandler(convertConfig);
             ComponentHandler output = helper.getHtmlComponentHandler(
                     attributes, convert, HtmlOutputText.COMPONENT_TYPE, null);
             return output;
         }
     }
 
 }
