 /*
  * Original Code Copyright Prime Technology.
  * Subsequent Code Modifications Copyright 2011-2012 ICEsoft Technologies Canada Corp. (c)
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  *
  * NOTE THIS CODE HAS BEEN MODIFIED FROM ORIGINAL FORM
  *
  * Subsequent Code Modifications have been made and contributed by ICEsoft Technologies Canada Corp. (c).
  *
  * Code Modification 1: Integrated with ICEfaces Advanced Component Environment.
  * Contributors: ICEsoft Technologies Canada Corp. (c)
  *
  * Code Modification 2: (ICE-6978) Used JSONBuilder to add the functionality of escaping JS output.
  * Contributors: ICEsoft Technologies Canada Corp. (c)
  * Contributors: ______________________
  */
 package org.icefaces.ace.component.datetimeentry;
 
 import java.io.IOException;
 import java.text.ParseException;
 import java.text.SimpleDateFormat;
 import java.util.Date;
 import java.util.Iterator;
 import java.util.Locale;
 import java.util.Map;
 
 import javax.faces.component.UIComponent;
 import javax.faces.context.FacesContext;
 import javax.faces.context.ResponseWriter;
 import javax.faces.convert.Converter;
 import javax.faces.convert.ConverterException;
 
 import org.icefaces.ace.renderkit.InputRenderer;
 import org.icefaces.ace.util.HTML;
 import org.icefaces.ace.util.JSONBuilder;
 import org.icefaces.render.MandatoryResourceComponent;
 
 @MandatoryResourceComponent(tagName="dateTimeEntry", value="org.icefaces.ace.component.datetimeentry.DateTimeEntry")
 public class DateTimeEntryRenderer extends InputRenderer {
 
     @Override
     public void decode(FacesContext context, UIComponent component) {
 //        printParams();
         DateTimeEntry dateTimeEntry = (DateTimeEntry) component;
 
         if(dateTimeEntry.isDisabled() || dateTimeEntry.isReadonly()) {
             return;
         }
 
         String clientId = dateTimeEntry.getClientId(context);
         Map<String, String> parameterMap = context.getExternalContext().getRequestParameterMap();
         String submittedValue = parameterMap.get(clientId + "_input");
         if (submittedValue == null && parameterMap.get(clientId + "_label") != null) {
             submittedValue = "";
         }
 
         if(submittedValue != null) {
             dateTimeEntry.setSubmittedValue(submittedValue);
         }
 
         decodeBehaviors(context, dateTimeEntry);
     }
 
     @Override
     public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
         DateTimeEntry dateTimeEntry = (DateTimeEntry) component;
         String value = DateTimeEntryUtils.getValueAsString(context, dateTimeEntry);
         Map<String, Object> labelAttributes = getLabelAttributes(component);
 
         encodeMarkup(context, dateTimeEntry, value, labelAttributes);
     }
 
     protected void encodeMarkup(FacesContext context, DateTimeEntry dateTimeEntry, String value, Map<String, Object> labelAttributes) throws IOException {
         ResponseWriter writer = context.getResponseWriter();
         String clientId = dateTimeEntry.getClientId(context);
         String inputId = clientId + "_input";
         boolean popup = dateTimeEntry.isPopup();
         Map paramMap = context.getExternalContext().getRequestParameterMap();
         String iceFocus = (String) paramMap.get("ice.focus");
 
         writer.startElement("span", dateTimeEntry);
         writer.writeAttribute("id", clientId, null);
         String style = dateTimeEntry.getStyle();
         if(style != null) writer.writeAttribute("style", style, null);
         String styleClass = dateTimeEntry.getStyleClass();
         if(styleClass != null) writer.writeAttribute("class", styleClass, null);
 
         //inline container
         if(!popup) {
             writer.startElement("div", null);
             writer.writeAttribute("id", clientId + "_inline", null);
             writer.endElement("div");
         }
 
         //input
         String type = popup ? "text" : "hidden";
 
         if (popup) {
             writeLabelAndIndicatorBefore(labelAttributes);
         }
         writer.startElement("input", null);
         writer.writeAttribute("id", inputId, null);
         writer.writeAttribute("name", inputId, null);
         writer.writeAttribute("type", type, null);
 		writer.writeAttribute("tabindex", dateTimeEntry.getTabindex(), null);
 
         String styleClasses = (themeForms() ? DateTimeEntry.INPUT_STYLE_CLASS : "") + getStateStyleClasses(dateTimeEntry);
         if(!isValueBlank(value)) {
             writer.writeAttribute("value", value, null);
         } else if (popup && !clientId.equals(iceFocus)) {
             String inFieldLabel = (String) labelAttributes.get("inFieldLabel");
             if (!isValueBlank(inFieldLabel)) {
                 writer.writeAttribute("name", clientId + "_label", null);
                 writer.writeAttribute("value", inFieldLabel, null);
                 styleClasses += " " + IN_FIELD_LABEL_STYLE_CLASS;
                 labelAttributes.put("labelIsInField", true);
             }
         }
 
         if(popup) {
             if(!isValueBlank(styleClasses)) writer.writeAttribute("class", styleClasses, null);
             if(dateTimeEntry.isReadOnlyInputText()) writer.writeAttribute("readonly", "readonly", null);
             if(dateTimeEntry.isDisabled()) writer.writeAttribute("disabled", "disabled", null);
             int size = dateTimeEntry.getSize();
             if (size <= 0) {
                 String formattedDate;
                 SimpleDateFormat dateFormat = new SimpleDateFormat(dateTimeEntry.getPattern(), dateTimeEntry.calculateLocale(context));
                 try {
                     formattedDate = dateFormat.format(new SimpleDateFormat("yyy-M-d H:m:s:S z").parse("2012-12-21 20:12:12:212 MST"));
                 } catch (ParseException e) {
                     formattedDate = dateFormat.format(new Date());
                 }
                 size = formattedDate.length();
             }
             writer.writeAttribute("size", size, null);
 
 //            renderPassThruAttributes(context, dateTimeEntry, HTML.INPUT_TEXT_ATTRS);
         }
 
         writer.endElement("input");
         if (popup) {
             writeLabelAndIndicatorAfter(labelAttributes);
         }
 		
 		encodeScript(context, dateTimeEntry, value, labelAttributes);
 
         writer.endElement("span");
     }
 
     protected void encodeScript(FacesContext context, DateTimeEntry dateTimeEntry, String value, Map<String, Object> labelAttributes) throws IOException {
         ResponseWriter writer = context.getResponseWriter();
         String clientId = dateTimeEntry.getClientId(context);
        
         writer.startElement("script", null);
         writer.writeAttribute("type", "text/javascript", null);
 
         String showOn = dateTimeEntry.getShowOn();
         boolean timeOnly = dateTimeEntry.isTimeOnly();
         StringBuilder script = new StringBuilder();
         JSONBuilder json = JSONBuilder.create();
 
         script.append("ice.ace.jq(function(){").append(resolveWidgetVar(dateTimeEntry)).append(" = new ");
         json.beginFunction("ice.ace.Calendar").
             item(clientId).
             beginMap().
                 entry("popup", dateTimeEntry.isPopup()).
                 entry("locale", dateTimeEntry.calculateLocale(context).toString());
                if(!isValueBlank(value) && !timeOnly) json.entry("defaultDate", value);
                 json.entryNonNullValue("pattern", DateTimeEntryUtils.convertPattern(dateTimeEntry.getPattern()));
                 if(dateTimeEntry.getPages() != 1) json.entry("numberOfMonths", dateTimeEntry.getPages());
                 json.entryNonNullValue("minDate", DateTimeEntryUtils.getDateAsString(dateTimeEntry, dateTimeEntry.getMindate())).
                 entryNonNullValue("maxDate", DateTimeEntryUtils.getDateAsString(dateTimeEntry, dateTimeEntry.getMaxdate()));
                 json.entryNonNullValue("showButtonPanel", dateTimeEntry.isShowButtonPanel());
                 if(dateTimeEntry.isShowWeek()) json.entry("showWeek", true);
                 if(dateTimeEntry.isDisabled()) json.entry("disabled", true);
                 json.entryNonNullValue("yearRange", dateTimeEntry.getYearRange());
                 if(dateTimeEntry.isNavigator()) {
                     json.entry("changeMonth", true).
                     entry("changeYear", true);
                 }
 
                 if(dateTimeEntry.getEffect() != null) {
                     json.entry("showAnim", dateTimeEntry.getEffect()).
                     entry("duration", dateTimeEntry.getEffectDuration());
                 }
                 if(!showOn.equalsIgnoreCase("focus")) {
                     String iconSrc = dateTimeEntry.getPopupIcon() != null ? getResourceURL(context, dateTimeEntry.getPopupIcon()) : getResourceRequestPath(context, DateTimeEntry.POPUP_ICON);
 
                     json.entry("showOn", showOn).
                     entry("buttonImage", iconSrc).
                     entry("buttonImageOnly", dateTimeEntry.isPopupIconOnly());
                 }
 
                 if(dateTimeEntry.isShowOtherMonths()) {
                     json.entry("showOtherMonths", true).
                     entry("selectOtherMonths", dateTimeEntry.isSelectOtherMonths());
                 }
 
                 //time
                 if(dateTimeEntry.hasTime()) {
                     json.entry("timeOnly", timeOnly).
 
                     //step
                     entry("stepHour", dateTimeEntry.getStepHour()).
                     entry("stepMinute", dateTimeEntry.getStepMinute()).
                     entry("stepSecond", dateTimeEntry.getStepSecond()).
                     
                     //minmax
                     entry("hourMin", dateTimeEntry.getMinHour()).
                     entry("hourMax", dateTimeEntry.getMaxHour()).
                     entry("minuteMin", dateTimeEntry.getMinMinute()).
                     entry("minuteMax", dateTimeEntry.getMaxMinute()).
                     entry("secondMin", dateTimeEntry.getMinSecond()).
                     entry("secondMax", dateTimeEntry.getMaxSecond());
                 }
 
                 encodeClientBehaviors(context, dateTimeEntry, json);
 
                 if(!themeForms()) {
                     json.entry("theme", false);
                 }
                 json.entry("disableHoverStyling", dateTimeEntry.isDisableHoverStyling());
                 json.entry("showCurrentAtPos", 0 - dateTimeEntry.getLeftMonthOffset());
                 json.entry("clientId", clientId);
                 json.entryNonNullValue("inFieldLabel", (String) labelAttributes.get("inFieldLabel"));
                 json.entry("inFieldLabelStyleClass", IN_FIELD_LABEL_STYLE_CLASS);
                 json.entry("labelIsInField", (Boolean) labelAttributes.get("labelIsInField"));
             json.endMap();
         json.endFunction();
 
         script.append(json.toString()).append("});");
 //        System.out.println(script);
         writer.write(script.toString());
 
         writer.endElement("script");
     }
 
     @Override
     public Object getConvertedValue(FacesContext context, UIComponent component, Object value) throws ConverterException {
         DateTimeEntry dateTimeEntry = (DateTimeEntry) component;
         String submittedValue = (String) value;
         Converter converter = dateTimeEntry.getConverter();
 
         if(isValueBlank(submittedValue)) {
             return null;
         }
 
         //Delegate to user supplied converter if defined
         if(converter != null) {
             return converter.getAsObject(context, dateTimeEntry, submittedValue);
         }
 
         //Use built-in converter
         try {
             Date convertedValue;
             Locale locale = dateTimeEntry.calculateLocale(context);
             SimpleDateFormat format = new SimpleDateFormat(dateTimeEntry.getPattern(), locale);
             format.setTimeZone(dateTimeEntry.calculateTimeZone());
             convertedValue = format.parse(submittedValue);
             
             return convertedValue;
 
         } catch (ParseException e) {
             throw new ConverterException(e);
         }
     }
 
     public static void printParams() {
         Map<String, String[]> paramValuesMap = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterValuesMap();
         String key;
         String[] values;
         for (Map.Entry<String, String[]> entry : paramValuesMap.entrySet()) {
             key = entry.getKey();
             values = entry.getValue();
             System.out.print(key);
             System.out.print(" = ");
             for (String value : values) {
                 System.out.print(value);
                 System.out.print(", ");
             }
             System.out.println();
         }
     }
 }
