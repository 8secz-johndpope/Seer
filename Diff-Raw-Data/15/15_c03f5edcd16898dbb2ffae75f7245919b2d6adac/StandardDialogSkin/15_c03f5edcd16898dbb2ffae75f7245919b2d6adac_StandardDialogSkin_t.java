 /*
  * Copyright (c) 2000-2004 Netspective Communications LLC. All rights reserved.
  *
  * Netspective Communications LLC ("Netspective") permits redistribution, modification and use of this file in source
  * and binary form ("The Software") under the Netspective Source License ("NSL" or "The License"). The following
  * conditions are provided as a summary of the NSL but the NSL remains the canonical license and must be accepted
  * before using The Software. Any use of The Software indicates agreement with the NSL.
  *
  * 1. Each copy or derived work of The Software must preserve the copyright notice and this notice unmodified.
  *
  * 2. Redistribution of The Software is allowed in object code form only (as Java .class files or a .jar file
  *    containing the .class files) and only as part of an application that uses The Software as part of its primary
  *    functionality. No distribution of the package is allowed as part of a software development kit, other library,
  *    or development tool without written consent of Netspective. Any modified form of The Software is bound by these
  *    same restrictions.
  *
  * 3. Redistributions of The Software in any form must include an unmodified copy of The License, normally in a plain
  *    ASCII text file unless otherwise agreed to, in writing, by Netspective.
  *
  * 4. The names "Netspective", "Axiom", "Commons", "Junxion", and "Sparx" are trademarks of Netspective and may not be
  *    used to endorse or appear in products derived from The Software without written consent of Netspective.
  *
  * THE SOFTWARE IS PROVIDED "AS IS" WITHOUT A WARRANTY OF ANY KIND. ALL EXPRESS OR IMPLIED REPRESENTATIONS AND
  * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT,
  * ARE HEREBY DISCLAIMED.
  *
  * NETSPECTIVE AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE OR ANY THIRD PARTY AS A
  * RESULT OF USING OR DISTRIBUTING THE SOFTWARE. IN NO EVENT WILL NETSPECTIVE OR ITS LICENSORS BE LIABLE FOR ANY LOST
  * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
  * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE THE SOFTWARE, EVEN
  * IF IT HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
  */
 package com.netspective.sparx.theme.basic;
 
 import java.io.IOException;
 import java.io.StringWriter;
 import java.io.Writer;
 import java.net.URLEncoder;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 
 import javax.servlet.http.HttpServletRequest;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 
 import com.netspective.commons.validate.ValidationContext;
 import com.netspective.commons.xdm.XmlDataModelSchema;
 import com.netspective.sparx.form.Dialog;
 import com.netspective.sparx.form.DialogContext;
 import com.netspective.sparx.form.DialogDirector;
 import com.netspective.sparx.form.DialogFlags;
 import com.netspective.sparx.form.DialogIncludeJavascriptFile;
 import com.netspective.sparx.form.DialogPerspectives;
 import com.netspective.sparx.form.DialogSkin;
 import com.netspective.sparx.form.field.DialogField;
 import com.netspective.sparx.form.field.DialogFieldFlags;
 import com.netspective.sparx.form.field.DialogFieldPopup;
 import com.netspective.sparx.form.field.DialogFields;
 import com.netspective.sparx.form.field.type.GridField;
 import com.netspective.sparx.form.field.type.SeparatorField;
 import com.netspective.sparx.panel.HtmlPanel;
 import com.netspective.sparx.theme.Theme;
 
 public class StandardDialogSkin extends BasicHtmlPanelSkin implements DialogSkin
 {
     public static final XmlDataModelSchema.Options XML_DATA_MODEL_SCHEMA_OPTIONS = new XmlDataModelSchema.Options().setIgnorePcData(true);
     private static final Log log = LogFactory.getLog(StandardDialogSkin.class);
     static public final String FIELDROW_PREFIX = "_dfr.";
     static public final String GRIDHEADROW_PREFIX = "_dghr.";
     static public final String GRIDFIELDROW_PREFIX = "_dgfr.";
     static public final String EMPTY = "";
 
     protected boolean summarizeErrors;
     protected String outerTableAttrs;
     protected String innerTableAttrs;
     protected String frameHdRowAlign;
     protected String frameHdRowAttrs;
     protected String frameHdFontAttrs;
     protected String errorMsgHdFontAttrs;
     protected String errorMsgHdText;
     protected String fieldRowAttrs;
     protected String fieldRowErrorAttrs;
     protected String gridCaptionFontAttrs;      // grid column font attributes
     protected String gridRowCaptionFontAttrs;   // grid row font attributes
     protected String gridCaptionCellAttrs;      // grid column display attributes
     protected String gridTableAttrs;            // grid table display attribute
     protected String gridBodyCellAttrs;
     protected String captionCellAttrs;
     protected String captionFontAttrs;
     protected String controlAreaFontAttrs;
     protected String controlAreaStyleAttrs;
     protected String controlAttrs;
     protected String controlAreaStyleClass;
     protected String controlAreaRequiredStyleClass;
     protected String controlAreaReadonlyStyleClass;
     protected String separatorFontAttrs;
     protected String separatorHtml;
     protected String separatorBannerTextFontAttrs;
     protected String hintFontAttrs;
     protected String errorMsgFontAttrs;
     protected String captionSuffix;
     protected String includePreScripts;
     protected String includePostScripts;
     protected String includePreStyleSheets;
     protected String includePostStyleSheets;
     protected String prependPreScript;
     protected String prependPostScript;
     protected String appendPreScript;
     protected String appendPostScript;
     protected Map includePostScriptsMap;
     protected Map includePreScriptsMap;
     private String controlAreaClass;
     private String captionClass;
 
     /**
      * Default constructor so that child classes can be instantiated through XDM.
      */
     public StandardDialogSkin()
     {
         init();
     }
 
     public StandardDialogSkin(Theme theme, String name, String panelClassNamePrefix, String panelResourcesPrefix, boolean fullWidth)
     {
         super(theme, name, panelClassNamePrefix, panelResourcesPrefix, fullWidth);
         init();
     }
 
     public void init()
     {
         summarizeErrors = true;
         outerTableAttrs = "border=\"0\" cellspacing=\"0\" cellpadding=\"0\" nowrap";
         innerTableAttrs = "width='100%' cellspacing='0' cellpadding='4' bgcolor='#EEEEEE' ";
         frameHdRowAlign = "LEFT";
         frameHdRowAttrs = "bgcolor='#6699CC' ";
         frameHdFontAttrs = "face='verdana,arial,helvetica' size=2 color='white' style='font-size:8pt' ";
         errorMsgHdFontAttrs = "face='verdana,arial,helvetica' size=2 color='darkred'";
         errorMsgHdText = "Please review the following:";
         fieldRowAttrs = "";
         fieldRowErrorAttrs = "bgcolor='#DDDDDD' ";
         captionCellAttrs = "align='right' ";
         captionFontAttrs = "size='2' face='tahoma,arial,helvetica' style='font-size:8pt' ";
         gridBodyCellAttrs = "";
         gridTableAttrs = "cellpadding='02' cellspacing='0' border='0' style='margin-top:0; margin-bottom:0'";
         gridCaptionFontAttrs = "size='2' face='tahoma,arial,helvetica' color='navy' style='font-size:8pt' ";
         gridRowCaptionFontAttrs = "size='2' face='tahoma,arial,helvetica' color='navy' style='font-size:8pt' ";
         gridCaptionCellAttrs = "align='center'";
         controlAreaFontAttrs = "size='2' face='tahoma,arial,helvetica' style='font-size:8pt' ";
 
         controlAreaStyleAttrs = "";
         controlAreaStyleClass = "dialog-input";
         controlAreaRequiredStyleClass = "dialog-input-required";
         controlAreaReadonlyStyleClass = "dialog-input-readonly";
 
         controlAttrs = " onfocus='return controlOnFocus(this, event)' onchange='controlOnChange(this, event)' " +
                       "onblur='controlOnBlur(this, event)' onkeypress='return controlOnKeypress(this, event)' onclick='controlOnClick(this, event) '";
         separatorFontAttrs = "face='verdana,arial' size=2 color=#555555";
         separatorBannerTextFontAttrs = "face='arial' size=2 color=#555555";
         separatorHtml = "";
         hintFontAttrs = " class=\"dialog-fields-hint\" ";
         errorMsgFontAttrs = "color='red'";
         captionSuffix = " ";
         includePreScripts = null;
         includePostScripts = null;
         includePreStyleSheets = null;
         includePostStyleSheets = null;
         prependPreScript = null;
         prependPostScript = null;
         appendPreScript = null;
         appendPostScript = null;
         includePostScriptsMap = new HashMap();
         includePreScriptsMap = new HashMap();
 
         controlAreaClass = " class=\"dialog-entry\" ";
         captionClass = " class=\"dialog-fields\"";
     }
 
     public final String getDefaultControlAttrs()
     {
         return controlAttrs;
     }
 
     /**
      * Gets the CSS style class for a dialog field control area
      *
      * @return String
      */
     public String getControlAreaStyleClass()
     {
         return controlAreaStyleClass;
     }
 
     /**
      * Gets the CSS style class for a required dialog field control area
      *
      * @return String
      */
     public String getControlAreaRequiredStyleClass()
     {
         return controlAreaRequiredStyleClass;
     }
 
     /**
      * Gets the CSS style class for a read only dialog field control area
      *
      * @return String
      */
     public String getControlAreaReadonlyStyleClass()
     {
         return controlAreaReadonlyStyleClass;
     }
 
     public void renderCompositeControlsHtml(Writer writer, DialogContext dc, DialogField parentField) throws IOException
     {
         DialogFields children = parentField.getChildren();
         writer.write("<span class='dialog-fields-no-arrow'>");
         for(int i = 0; i < children.size(); i++)
         {
             DialogField field = children.get(i);
             if(field.isAvailable(dc))
             {
                 if(field.isInputHidden(dc))
                     field.renderControlHtml(writer, dc);
                 else
                 {
                     DialogFieldFlags flags = field.getFlags();
                     if(flags.flagIsSet(DialogFieldFlags.COLUMN_BREAK_BEFORE))
                         writer.write("<br>");
                     boolean showCaption = field.showCaptionAsChild();
                     if(showCaption)
                     {
                         String caption = field.getCaption().getTextValue(dc);
                         if(caption != DialogField.CUSTOM_CAPTION && caption != null)
                         {
                             writer.write("<nobr>" + (field.isRequired(dc) ? "<b>" + caption + "</b>" : caption));
                             if(captionSuffix != null)
                                 writer.write(captionSuffix);
                         }
                     }
                     field.renderControlHtml(writer, dc);
                     writer.write("&nbsp;");
                     if(showCaption) writer.write("</nobr>");
                     if(flags.flagIsSet(DialogFieldFlags.COLUMN_BREAK_AFTER))
                         writer.write("<br>");
                 }
             }
         }
         writer.write("</span>");
     }
 
     public void appendGridControlBasics(DialogContext dc, DialogField field, StringBuffer html) throws IOException
     {
         StringWriter controlHtml = new StringWriter();
         field.renderControlHtml(controlHtml, dc);
         String popupHtml = getPopupHtml(dc, field);
         if(popupHtml != null)
             controlHtml.write(popupHtml);
 
         if(field.isHelpAvailable())
             controlHtml.write("&nbsp;" + field.getHelpPanel().getDialogFieldHelpHtml(dc, getTheme()));
 
         if(field.getFlags().flagIsSet(DialogFieldFlags.CREATE_ADJACENT_AREA))
         {
             String adjValue = dc.getFieldStates().getState(field).getAdjacentAreaValue();
             controlHtml.write("&nbsp;<span id='" + field.getQualifiedName() + "_adjacent'>" + (adjValue != null
                                                                                                ? adjValue : "") + "</span>");
         }
 
         StringBuffer messagesHtml = new StringBuffer();
         String hint = field.getHint().getTextValue(dc);
         if(hint != null)
         {
             messagesHtml.append("<br><font " + hintFontAttrs + ">");
             messagesHtml.append(hint);
             messagesHtml.append("</font>");
         }
 
         html.append("<font ");
         html.append(controlAreaFontAttrs);
         html.append(">");
         html.append(controlHtml);
         if(messagesHtml.length() > 0)
             html.append(messagesHtml);
         html.append("</font>");
     }
 
     public String getGridRowHtml(DialogContext dc, GridField gridField, DialogField compositeField, int row) throws IOException
     {
         String rowAttr = " id='" + GRIDFIELDROW_PREFIX + compositeField.getQualifiedName() + "' ";
         StringBuffer rowHtml = new StringBuffer("\n<tr valign='top' " + rowAttr + ">");
         DialogFields compChildren = compositeField.getChildren();
 
         // get the row's name
         String rowCaption = compositeField.getCaption().getTextValue(dc);
         if(rowCaption == null)
         {
             rowCaption = "";
         }
         if(row == 0)
         {
             String hRowAttr = " id='" + GRIDHEADROW_PREFIX + compositeField.getQualifiedName() + "' ";
             StringBuffer headerHtml = new StringBuffer("\n<tr " + hRowAttr + ">");
 
             int fieldNum = 0;
             String[] fieldCaptions = gridField.getCaptions(dc);
             // save space in the header for the row captions
             headerHtml.append("<td " + gridCaptionCellAttrs + ">&nbsp;</td> ");
             // append the row caption to the first row
             rowHtml.append("<td><font " + gridRowCaptionFontAttrs + ">");
             rowHtml.append(rowCaption);
             rowHtml.append("</font></td>");
             for(int i = 0; i < compChildren.size(); i++)
             {
                 DialogField field = compChildren.get(i);
                 if(field.isAvailable(dc))
                 {
                     String caption = fieldNum < fieldCaptions.length
                                      ? fieldCaptions[fieldNum] : field.getCaption().getTextValue(dc);
 
                     headerHtml.append("<td " + gridCaptionCellAttrs + "><font ");
                     headerHtml.append(gridCaptionFontAttrs);
                     headerHtml.append(">");
                     if(caption != null && caption != DialogField.CUSTOM_CAPTION)
                     {
                         headerHtml.append(field.isRequired(dc) ? "<b>" + caption + "</b>" : caption);
                     }
                     headerHtml.append("</font></td>");
 
 
                     rowHtml.append("<td " + gridBodyCellAttrs + ">");
                     appendGridControlBasics(dc, field, rowHtml);
                     rowHtml.append("</td>");
                 }
                 fieldNum++;
             }
 
             headerHtml.append("</tr>");
             headerHtml.append(rowHtml);
             headerHtml.append("</tr>");
 
             return headerHtml.toString();
         }
         else
         {
             // append the row caption to the first row
             rowHtml.append("<td><font " + gridRowCaptionFontAttrs + ">");
             rowHtml.append(rowCaption);
             rowHtml.append("</font></td>");
 
             for(int i = 0; i < compChildren.size(); i++)
             {
                 DialogField field = compChildren.get(i);
                 if(field.isAvailable(dc))
                 {
                     rowHtml.append("<td " + gridBodyCellAttrs + ">");
                     appendGridControlBasics(dc, field, rowHtml);
                     rowHtml.append("</td>");
                 }
             }
             rowHtml.append("</tr>");
             return rowHtml.toString();
         }
     }
 
     public void renderGridControlsHtml(Writer writer, DialogContext dc, GridField gridField) throws IOException
     {
         writer.write("\n<table " + gridTableAttrs + ">");
 
         DialogFields gridChildren = gridField.getChildren();
         int colsCount = 0;
 
         for(int row = 0; row < gridChildren.size(); row++)
         {
             DialogField rowField = gridChildren.get(row);
             if(colsCount == 0)
                 colsCount = rowField.getChildren().size();
 
             if(rowField.isAvailable(dc))
             {
                 StringBuffer messagesHtml = new StringBuffer();
                 boolean haveErrors = false;
                 boolean firstMsg = true;
                 List errorMessages = dc.getValidationContext().getValidationErrorsForScope(dc.getFieldStates().getState(rowField).getValidationContextScope());
                 if(errorMessages != null)
                 {
                     messagesHtml.append("<font " + errorMsgFontAttrs + ">");
                     Iterator emi = errorMessages.iterator();
                     while(emi.hasNext())
                     {
                         if(!firstMsg)
                             messagesHtml.append("<br>");
                         else
                             firstMsg = false;
                         messagesHtml.append((String) emi.next());
                     }
                     messagesHtml.append("</font>");
                     haveErrors = true;
                 }
 
                 writer.write(getGridRowHtml(dc, gridField, rowField, row));
                 if(haveErrors)
                 {
                     writer.write("<tr><td colspan='" + colsCount + "'>");
                     writer.write("<font " + controlAreaFontAttrs);
                     writer.write(messagesHtml.toString());
                     writer.write("</font></td></tr>");
                 }
             }
         }
 
         writer.write("\n</table>");
     }
 
     public String getPopupHtml(DialogContext dc, DialogField field)
     {
         DialogFieldPopup[] popup = field.getPopups();
 
         if(popup == null || popup.length == 0)
             return null;
 
         StringBuffer html = new StringBuffer();
         StringBuffer expression = null;
         for(int i = 0; i < popup.length; i++)
         {
             final DialogFieldPopup activePopup = popup[i];
 
             // if the field is read only we won't show it
             if(activePopup.isHideIfReadOnly() && dc.getFieldStates().getState(field).getStateFlags().flagIsSet(DialogFieldFlags.READ_ONLY))
                 continue;
 
             expression = new StringBuffer("new DialogFieldPopup('" + dc.getDialog().getHtmlFormName() + "', '" + field.getQualifiedName() +
                                           "', '" + activePopup.getAction().getTextValueOrBlank(dc) + "', '" + activePopup.getWindowClass() + "', " + activePopup.isCloseAfterSelect() +
                                           ", " + activePopup.isAllowMulti() + ", ");
 
             StringBuffer tmpBuffer = new StringBuffer();
             String[] fillFields = activePopup.getFill();
             if(fillFields != null && fillFields.length > 0)
             {
                 tmpBuffer.append("new Array(");
                 for(int k = 0; k < fillFields.length; k++)
                     tmpBuffer.append("'" + fillFields[k] + (k < fillFields.length - 1 ? "', " : "'"));
                 tmpBuffer.append(")");
             }
             else
                 tmpBuffer.append("null");
             expression.append(tmpBuffer.toString() + ", ");
 
             tmpBuffer = new StringBuffer();
             String[] extractFields = activePopup.getExtract();
             // append the list of extract fields if they exist
             if(extractFields != null && extractFields.length > 0)
             {
                 tmpBuffer.append("new Array(");
                 for(int k = 0; k < extractFields.length; k++)
                 {
                     tmpBuffer.append("'" + extractFields[k] + (k < extractFields.length - 1 ? "', " : "'"));
                 }
                 tmpBuffer.append(")");
             }
             else
                 tmpBuffer.append("null");
             expression.append(tmpBuffer.toString() + ", ");
 
             // append evaluation script
             if(activePopup.getPreActionScript() != null)
                 expression.append("'" + activePopup.getPreActionScript() + "'");
             else
                 expression.append("null");
             expression.append(")");
 
             if(activePopup.getStyle().getValueIndex() == DialogFieldPopup.Style.TEXT)
             {
                 html.append("&nbsp;<a href='' style='cursor:hand;' onclick=\"javascript:" + expression +
                             ";return false;\">" + activePopup.getStyleText().getTextValue(dc) + "</a>&nbsp;");
             }
             else
             {
                 String imageUrl = activePopup.getImageSrc().getTextValue(dc);
                 if(imageUrl == null)
                     imageUrl = getTheme().getResourceUrl("/images/panel/input/content-popup.gif");
                 html.append("&nbsp;<a href='' style='cursor:hand;' onclick=\"javascript:" + expression +
                             ";return false;\"><img border='0' src='" + imageUrl + "' alt='pop-up'></a>&nbsp;");
             }
         }
         return html.toString();
     }
 
     public void appendFieldHtml(DialogContext dc, DialogField field, StringBuffer fieldsHtml, StringBuffer fieldsJSDefn, List fieldErrorMsgs) throws IOException
     {
         if(field.isInputHidden(dc))
         {
             StringWriter writer = new StringWriter();
             field.renderControlHtml(writer, dc);
             fieldsHtml.append(writer);
             // even if the field is hidden, you still need to register it in JS
             if(field.getName() != null)
                 fieldsJSDefn.append(field.getJavaScriptDefn(dc));
             return;
         }
 
         String caption = field.getCaption().getTextValue(dc);
         DialogFields fieldChildren = field.getChildren();
         if(caption != null && fieldChildren != null && caption.equals(DialogField.GENERATE_CAPTION))
         {
             StringBuffer generated = new StringBuffer();
             for(int i = 0; i < fieldChildren.size(); i++)
             {
                 DialogField childField = fieldChildren.get(i);
                 String childCaption = childField.getCaption().getTextValue(dc);
                 if(childCaption != null && childCaption != DialogField.CUSTOM_CAPTION)
                 {
                     if(generated.length() > 0)
                         generated.append(" / ");
                     generated.append(childField.isRequired(dc) ? "<b>" + childCaption + "</b>" : childCaption);
                 }
             }
             caption = generated.toString();
         }
         else
         {
             if(caption != null && field.isRequired(dc))
                 caption = "<b>" + caption + "</b>";
         }
 
         if(captionSuffix != null && caption != null && caption.length() > 0) caption += captionSuffix;
 
         StringWriter controlHtml = new StringWriter();
         field.renderControlHtml(controlHtml, dc);
         String popupHtml = getPopupHtml(dc, field);
         if(popupHtml != null)
             controlHtml.write(popupHtml);
 
         if(field.isHelpAvailable())
             controlHtml.write("&nbsp;" + field.getHelpPanel().getDialogFieldHelpHtml(dc, getTheme()));
 
         DialogField.State state = dc.getFieldStates().getState(field);
         DialogFieldFlags stateFlags = state.getStateFlags();
 
         if(stateFlags.flagIsSet(DialogFieldFlags.CREATE_ADJACENT_AREA))
         {
             String adjValue = state.getAdjacentAreaValue();
             controlHtml.write("&nbsp;<span id='" + field.getQualifiedName() + "_adjacent'>" + (adjValue != null
                                                                                                ? adjValue : "") + "</span>");
         }
 
         boolean haveErrors = false;
         StringBuffer messagesHtml = null;
         List errorMessages = dc.getValidationContext().getValidationErrorsForScope(state.getValidationContextScope());
         if(errorMessages.size() > 0)
         {
             messagesHtml = new StringBuffer();
             messagesHtml.append("<font " + errorMsgFontAttrs + ">");
             for(int i = 0; i < errorMessages.size(); i++)
             {
                 String msgStr = (String) errorMessages.get(i);
                 fieldErrorMsgs.add(msgStr);
                 if(i > 0)
                     messagesHtml.append("<br>");
                 messagesHtml.append("<a name='dc_error_msg_" + i + "'>" + msgStr + "</a>");
             }
             messagesHtml.append("</font>");
             haveErrors = true;
         }
 
         String hintHtml = "";
         if(controlHtml.getBuffer().length() > 0)
         {
             // only show the hint when there is an input field!
             String hint = field.getHint().getTextValue(dc);
             if(hint != null)
             {
                 DialogFlags dialogFlags = dc.getDialog().getDialogFlags();
                 if((field.isReadOnly(dc) && dialogFlags.flagIsSet(DialogFlags.HIDE_READONLY_HINTS)))
                 {
                     hintHtml = "";
                 }
                 else if(dialogFlags.flagIsSet(DialogFlags.HIDE_HINTS_UNTIL_FOCUS))
                 {
                     // hide the hints until the field is being edited
                     hintHtml = "<br><span id=\"" + field.getQualifiedName() + "_hint\" class=\"dialog-fields-hint-hidden\">&nbsp;&nbsp;&nbsp;" + hint + "</span>";
                 }
                 else
                 {
                     hintHtml = "<br><span id=\"" + field.getQualifiedName() + "_hint\" class=\"dialog-fields-hint\">&nbsp;&nbsp;&nbsp;" + hint + "</span>";
                 }
             }
         }
 
         /*
 		 * each field row gets its own ID so DHTML can hide/show the row
 		 */
 
         String rowAttr = fieldRowAttrs + " id='" + FIELDROW_PREFIX + field.getQualifiedName() + "' ";
         if(haveErrors)
             rowAttr = rowAttr + fieldRowErrorAttrs;
 
         if(caption == null)
         {
             if(field instanceof SeparatorField)
                 fieldsHtml.append("<tr" + rowAttr + "><td class=\"dialog-fields-separator\" colspan='2'>" + controlHtml + "</td></tr>\n");
             else
                 fieldsHtml.append("<tr" + rowAttr + "><td colspan='2'>" + controlHtml + hintHtml +
                                   "</td></tr>\n");
 
             if(haveErrors)
                 fieldsHtml.append("<tr><td><span class=\"dialog-fields-errors\">&nbsp;&nbsp;&nbsp;" + messagesHtml + "</span></td></tr>\n");
         }
         else
         {
             String accessKey = field.getAccessKey();
             if(accessKey != null && accessKey.length() > 0)
             {
                 int accessKeyPos = caption.toLowerCase().indexOf(accessKey.toLowerCase());
                 if(accessKeyPos > 0 && accessKeyPos < caption.length() - 1)
                 {
                     fieldsHtml.append("<tr " + rowAttr + "><td " + getCaptionClass() + "><label for=\"" + field.getHtmlFormControlId() + "\" accesskey=\"" +
                                       field.getAccessKey() + "\">" + caption.substring(0, accessKeyPos) + "<span class=\"accesskey\">" +
                                       caption.substring(accessKeyPos, accessKeyPos + 1) + "</span>" + caption.substring(accessKeyPos + 1) + "</label></td>" +
                                       "<td " + getControlAreaClass() + ">" + controlHtml + hintHtml +
                                       "</td></tr>\n");
                 }
                 else if(accessKeyPos == caption.length() - 1)
                 {
                     fieldsHtml.append("<tr " + rowAttr + "><td " + getCaptionClass() + "><label for=\"" + field.getHtmlFormControlId() + "\" accesskey=\"" +
                                       field.getAccessKey() + "\">" + caption.substring(0, accessKeyPos) + "<span class=\"accesskey\">" +
                                       caption.substring(accessKeyPos) + "</span></label></td>" +
                                       "<td " + getControlAreaClass() + ">" + controlHtml + hintHtml +
                                       "</td></tr>\n");
                 }
                 else if(accessKeyPos == 0)
                 {
                     fieldsHtml.append("<tr " + rowAttr + "><td " + getCaptionClass() + "><label for=\"" + field.getHtmlFormControlId() + "\" accesskey=\"" +
                                       field.getAccessKey() + "\">" + "<span class=\"accesskey\">" +
                                       caption.substring(0, 1) + "</span>" + caption.substring(1) + "</label></td>" +
                                       "<td " + getControlAreaClass() + ">" + controlHtml + hintHtml +
                                       "</td></tr>\n");
                 }
                 else
                 {
                     fieldsHtml.append("<tr " + rowAttr + "><td " + getCaptionClass() + ">" + caption + "</td>" +
                                       "<td " + getControlAreaClass() + ">" + controlHtml + hintHtml +
                                       "</td></tr>\n");
                 }
             }
             else if(caption.length() > 0)
             {
                 fieldsHtml.append("<tr" + rowAttr + "><td " + getCaptionClass() + ">" +
                                   "<label for=\"" + field.getHtmlFormControlId() + "\">" +
                                   caption + "</label></td>" +
                                   "<td " + getControlAreaClass() + ">" + controlHtml + hintHtml +
                                   "</td></tr>\n");
             }
             else
             {
                 fieldsHtml.append("<tr" + rowAttr + "><td>&nbsp;</td>" +
                                   "<td " + getControlAreaClass() + ">" + controlHtml + hintHtml +
                                   "</td></tr>\n");
             }
 
             if(haveErrors)
                 fieldsHtml.append("<tr><td>&nbsp;</td><td>" +
                                   "<span class=\"dialog-fields-errors\">&nbsp;&nbsp;&nbsp;" + messagesHtml + "</span></td></tr>\n");
         }
 
         if(field.getName() != null)
             fieldsJSDefn.append(field.getJavaScriptDefn(dc));
     }
 
     public void renderHtml(Writer writer, DialogContext dc) throws IOException
     {
         renderPanelRegistration(writer, dc);
 
         if(dc.getDialog().hideHeading(dc))
             dc.setPanelRenderFlags(dc.getPanelRenderFlags() | HtmlPanel.RENDERFLAG_HIDE_FRAME_HEADING);
         int panelRenderFlags = dc.getPanelRenderFlags();
 
         if((panelRenderFlags & HtmlPanel.RENDERFLAG_NOFRAME) == 0)
         {
             renderFrameBegin(writer, dc);
             writer.write("    <table class=\"report\" width=\"100%\" border=\"0\" cellspacing=\"2\" cellpadding=\"0\">\n");
         }
         else
             writer.write("    <table id=\"" + dc.getPanel().getPanelIdentifier() + "_content\" class=\"report\" width=\"100%\" border=\"0\" cellspacing=\"2\" cellpadding=\"0\">\n");
 
         List fieldErrorMsgs = new ArrayList();
         List dlgErrorMsgs = dc.getValidationContext().getValidationErrorsForScope(ValidationContext.VALIDATIONSCOPE_ENTIRE_CONTEXT);
         if(dlgErrorMsgs != null)
             fieldErrorMsgs.addAll(dlgErrorMsgs);
 
         Dialog dialog = dc.getDialog();
 
         int layoutColumnsCount = dialog.getLayoutColumnsCount();
         int dlgTableColSpan = 2;
 
         StringBuffer fieldsHtml = new StringBuffer();
         StringBuffer fieldsJSDefn = new StringBuffer();
 
         DialogDirector director = dialog.getDirector();
         if(layoutColumnsCount == 1)
         {
             DialogFields fields = dc.getDialog().getFields();
             for(int i = 0; i < fields.size(); i++)
             {
                 DialogField field = fields.get(i);
                 if(!field.isAvailable(dc))
                     continue;
 
                 appendFieldHtml(dc, field, fieldsHtml, fieldsJSDefn, fieldErrorMsgs);
             }
 
             if(director != null && director.isAvailable(dc) && !dc.getDialogState().getPerspectives().flagIsSet(DialogPerspectives.PRINT))
             {
                 fieldsHtml.append("<tr><td class=\"dialog-button-table\" colspan='2'>");
                 StringWriter directorHtml = new StringWriter();
                 director.renderControlHtml(directorHtml, dc);
                 fieldsHtml.append(directorHtml);
                 fieldsHtml.append("</td></tr>");
             }
         }
         else
         {
             StringBuffer[] layoutColsFieldsHtml = new StringBuffer[layoutColumnsCount];
             for(int i = 0; i < layoutColumnsCount; i++)
                 layoutColsFieldsHtml[i] = new StringBuffer();
 
             int activeColumn = 0;
 
             DialogFields fields = dc.getDialog().getFields();
             for(int i = 0; i < fields.size(); i++)
             {
                 DialogField field = fields.get(i);
                 if(!field.isAvailable(dc))
                     continue;
 
                 DialogFieldFlags flags = field.getFlags();
                 if(flags.flagIsSet(DialogFieldFlags.COLUMN_BREAK_BEFORE))
                     activeColumn++;
                 appendFieldHtml(dc, field, layoutColsFieldsHtml[activeColumn], fieldsJSDefn, fieldErrorMsgs);
                 if(flags.flagIsSet(DialogFieldFlags.COLUMN_BREAK_AFTER))
                     activeColumn++;
             }
 
             int lastColumn = layoutColumnsCount - 1;
             int cellWidth = 100 / layoutColumnsCount;
             dlgTableColSpan = 0;
 
             fieldsHtml.append("<tr valign='top'>");
             for(int c = 0; c < layoutColumnsCount; c++)
             {
 
                 fieldsHtml.append("<td width='" + cellWidth + "%'><table width='100%'>");
                 fieldsHtml.append(layoutColsFieldsHtml[c]);
                 fieldsHtml.append("</table></td>");
                 dlgTableColSpan++;
 
                 if(c < lastColumn)
                 {
                     fieldsHtml.append("<td>&nbsp;&nbsp;</td>");
                     dlgTableColSpan++;
                 }
             }
             fieldsHtml.append("</tr>");
 
             if(director != null && director.isAvailable(dc) && !dc.getDialogState().getPerspectives().flagIsSet(DialogPerspectives.PRINT))
             {
                 fieldsHtml.append("<tr><td class=\"dialog-button-table\" colspan='" + dlgTableColSpan + "'>");
                 StringWriter directorHtml = new StringWriter();
                 director.renderControlHtml(directorHtml, dc);
                 fieldsHtml.append(directorHtml);
                 fieldsHtml.append("</td></tr>");
             }
         }
 
         StringBuffer errorMsgsHtml = new StringBuffer();
         if(fieldErrorMsgs.size() > 0)
         {
             errorMsgsHtml.append("<tr><td colspan='" + dlgTableColSpan + "'><ul type=square><font " + controlAreaFontAttrs + "><font " + errorMsgHdFontAttrs + "><b>" + errorMsgHdText + "</b></font>\n");
             for(int i = 0; i < fieldErrorMsgs.size(); i++)
             {
                 String errorMsg = (String) fieldErrorMsgs.get(i);
                 errorMsgsHtml.append("<li><a href='#dc_error_msg_" + i + "' style='text-decoration:none'><font " + errorMsgFontAttrs + ">" + errorMsg + "</font></a></li>\n");
             }
             errorMsgsHtml.append("</ul></td></tr>\n");
         }
         List fileList = dialog.getClientJs();
         String[] includeJSList = new String[fileList.size()];
         for(int i = 0; i < includeJSList.length; i++)
         {
             DialogIncludeJavascriptFile jsFileObj = (DialogIncludeJavascriptFile) fileList.get(i);
             includeJSList[i] = jsFileObj.getHref().getTextValue(dc);
         }
 
         if(includePreStyleSheets != null)
             writer.write(includePreStyleSheets);
         if(includePostStyleSheets != null)
             writer.write(includePostStyleSheets);
         if(prependPreScript != null)
             writer.write(prependPreScript);
         writer.write("<script language='JavaScript'>var _version = 1.0;</script>\n" +
                      "<script language='JavaScript1.1'>_version = 1.1;</script>\n" +
                      "<script language='JavaScript1.2'>_version = 1.2;</script>\n" +
                      "<script language='JavaScript1.3'>_version = 1.3;</script>\n" +
                      "<script language='JavaScript1.4'>_version = 1.4;</script>\n");
         if(includePreScripts != null)
             writer.write(includePreScripts);
 
         writer.write("<script language='JavaScript'>\n" +
                      "<!--\n" +
                      "	if(typeof dialogLibraryLoaded == 'undefined')\n" +
                      "	{\n" +
                      "		alert('ERROR: dialog.js was not loaded.');\n" +
                      "	}\n" +
                      "-->\n" +
                      "</script>\n");
 
         if(includeJSList.length > 0)
         {
             for(int i = 0; i < includeJSList.length; i++)
             {
                 writer.write("<script language='JavaScript' src='" + includeJSList[i] + "'></script>\n");
             }
         }
         if(includePostScripts != null)
             writer.write(includePostScripts);
         if(prependPostScript != null)
             writer.write(prependPostScript);
 
         DialogFlags dflags = dialog.getDialogFlags();
         if(dflags.flagIsSet(DialogFlags.DISABLE_CLIENT_VALIDATION))
             writer.write("<script>ALLOW_CLIENT_VALIDATION = false;</script>");
         if(dflags.flagIsSet(DialogFlags.TRANSLATE_ENTER_KEY_TO_TAB_KEY))
             writer.write("<script>TRANSLATE_ENTER_KEY_TO_TAB_KEY = true;</script>");
         if(dflags.flagIsSet(DialogFlags.SHOW_DATA_CHANGED_MESSAGE_ON_LEAVE))
             writer.write("<script>SHOW_DATA_CHANGED_MESSAGE_ON_LEAVE = true;</script>");
         if(dflags.flagIsSet(DialogFlags.DISABLE_CLIENT_KEYPRESS_FILTERS))
             writer.write("<script>ENABLE_KEYPRESS_FILTERS = flase;</script>");
         if(dflags.flagIsSet(DialogFlags.HIDE_HINTS_UNTIL_FOCUS))
             writer.write("<script>HIDE_HINTS_UNTIL_FOCUS = true;</script>");
 
         String dialogName = dialog.getHtmlFormName();
         String encType = dialog.getDialogFlags().flagIsSet(DialogFlags.ENCTYPE_MULTIPART_FORMDATA)
                          ? "enctype=\"multipart/form-data\"" : "";
 
         String actionURL = null;
         if(director != null)
             actionURL = director.getSubmitActionUrl() != null
                         ? director.getSubmitActionUrl().getValue(dc).getTextValue() : null;
 
         if(actionURL == null)
             actionURL = ((HttpServletRequest) dc.getRequest()).getRequestURI();
 
         renderContentsHtml(writer, dc, dialogName, actionURL, encType, dlgTableColSpan, errorMsgsHtml, fieldsHtml);
 
         if(appendPreScript != null)
             writer.write(appendPreScript);
 
         writer.write("<script language='JavaScript'>\n" +
                      "<!--\n" +
                      "       var " + dialogName + " = new Dialog(\"" + dialogName + "\");\n" +
                      "       var dialog = " + dialogName + "; setActiveDialog(dialog);\n" +
                      "       var field;\n" +
                      fieldsJSDefn +
                      "       dialog.finalizeContents();\n" +
                      "-->\n" +
                      "</script>\n");
 
         if(appendPostScript != null)
             writer.write(appendPostScript);
 
         // panel end
         writer.write("    </table>\n");
         if((panelRenderFlags & HtmlPanel.RENDERFLAG_NOFRAME) == 0)
             renderFrameEnd(writer, dc);
     }
 
     public void writeIncludeScripts(Writer writer, DialogContext dc, Map scriptsMap) throws IOException
     {
         Iterator i = scriptsMap.keySet().iterator();
         while(i.hasNext())
         {
             String src = (String) i.next();
             String adjustedSrc = src;
             if(src.startsWith("/"))
             {
                 String appRoot = ((HttpServletRequest) dc.getRequest()).getContextPath();
                 adjustedSrc = appRoot + src;
             }
             String lang = (String) scriptsMap.get(src);
             writer.write("<script src='" + adjustedSrc + "' language='" + lang + "'></script>\n");
         }
     }
 
     public void renderContentsHtml(Writer writer, DialogContext dc, String dialogName, String actionURL, String encType, int dlgTableColSpan, StringBuffer errorMsgsHtml, StringBuffer fieldsHtml) throws IOException
     {
         if(summarizeErrors)
             writer.write(errorMsgsHtml.toString());
 
         writer.write("<form id='" + dialogName + "' name='" + dialogName + "' action='" + actionURL + "' method='post' " +
                      encType + " onsubmit='return(activeDialog.isValid())'>\n" +
                      dc.getStateHiddens() + "\n" +
                      fieldsHtml +
                      "</form>\n");
     }
 
     public void renderSeparatorHtml(Writer writer, DialogContext dc, SeparatorField field) throws IOException
     {
         String heading = field.getHeading().getTextValue(dc);
         DialogFieldFlags flags = field.getFlags();
 
         if(heading != null)
         {
             String sep = "<a class=\"dialog-fields-separator-link\" name=\"" + URLEncoder.encode(heading) + "\">" + heading + "</a>";
             if(field.getBanner() != null)
             {
                 sep += "<br><font " + separatorBannerTextFontAttrs + ">";
                 sep += field.getBanner().getTextValue(dc);
                 sep += "</font>";
             }
             if(flags.flagIsSet(SeparatorField.Flags.RULE))
                 sep += separatorHtml;
 
             if(flags.flagIsSet(DialogFieldFlags.COLUMN_BREAK_BEFORE))
                 writer.write(sep);
             else
                 writer.write("<br>" + sep);
         }
         else
         {
             if(!flags.flagIsSet(DialogFieldFlags.COLUMN_BREAK_BEFORE))
                 writer.write(flags.flagIsSet(SeparatorField.Flags.RULE) ? "<br>" : "");
         }
     }
 
     public void renderRedirectHtml(Writer writer, DialogContext dc, String redirectUrl) throws IOException
     {
         writer.write("<script>window.location = \"");
         writer.write(redirectUrl);
         writer.write("\";</script>");
     }
 
     public boolean isSummarizeErrors()
     {
         return summarizeErrors;
     }
 
     public void setSummarizeErrors(boolean summarizeErrors)
     {
         this.summarizeErrors = summarizeErrors;
     }
 
     public String getOuterTableAttrs()
     {
         return outerTableAttrs;
     }
 
     public void setOuterTableAttrs(String value)
     {
         outerTableAttrs = value;
     }
 
     public String getInnerTableAttrs()
     {
         return innerTableAttrs;
     }
 
     public void setInnerTableAttrs(String value)
     {
         innerTableAttrs = value;
     }
 
     public String getFrameHdRowAlign()
     {
         return frameHdRowAlign;
     }
 
     public void setFrameHdRowAlign(String value)
     {
         frameHdRowAlign = value;
     }
 
     public String getFrameHdRowAttrs()
     {
         return frameHdRowAttrs;
     }
 
     public void setFrameHdRowAttrs(String value)
     {
         frameHdRowAttrs = value;
     }
 
     public String getFrameHdFontAttrs()
     {
         return frameHdFontAttrs;
     }
 
     public void setFrameHdFontAttrs(String value)
     {
         frameHdFontAttrs = value;
     }
 
     public String getFieldRowAttrs()
     {
         return fieldRowAttrs;
     }
 
     public void setFieldRowAttrs(String value)
     {
         fieldRowAttrs = value;
     }
 
     public String getFieldRowErrorAttrs()
     {
         return fieldRowErrorAttrs;
     }
 
     public void setFieldRowErrorAttrs(String value)
     {
         fieldRowErrorAttrs = value;
     }
 
     public String getGridCaptionFontAttrs()
     {
         return gridCaptionFontAttrs;
     }
 
     public void setGridCaptionFontAttrs(String value)
     {
         gridCaptionFontAttrs = value;
     }
 
     public String getGridRowCaptionFontAttrs()
     {
         return gridRowCaptionFontAttrs;
     }
 
     public void setGridRowCaptionFontAttrs(String value)
     {
         gridRowCaptionFontAttrs = value;
     }
 
     public String getCaptionCellAttrs()
     {
         return captionCellAttrs;
     }
 
     public void setCaptionCellAttrs(String value)
     {
         captionCellAttrs = value;
     }
 
     public String getCaptionFontAttrs()
     {
         return captionFontAttrs;
     }
 
     public void setCaptionFontAttrs(String value)
     {
         captionFontAttrs = value;
     }
 
     public String getControlAreaFontAttrs()
     {
         return controlAreaFontAttrs;
     }
 
     public void setControlAreaFontAttrs(String value)
     {
         controlAreaFontAttrs = value;
     }
 
     public String getControlAreaStyleAttrs()
     {
         return controlAreaStyleAttrs;
     }
 
     public void setControlAreaStyleAttrs(String value)
     {
         this.controlAreaStyleAttrs = value;
     }
 
     public String getControlAttrs()
     {
         return controlAttrs;
     }
 
     public void setControlAttrs(String value)
     {
         controlAttrs = value;
     }
 
     public String getSeparatorFontAttrs()
     {
         return separatorFontAttrs;
     }
 
     public void setSeparatorFontAttrs(String value)
     {
         separatorFontAttrs = value;
     }
 
     public String getSeparatorHtml()
     {
         return separatorHtml;
     }
 
     public void setSeparatorHtml(String value)
     {
         separatorHtml = value;
     }
 
     public String getHintFontAttrs()
     {
         return hintFontAttrs;
     }
 
     public void setHintFontAttrs(String value)
     {
         hintFontAttrs = value;
     }
 
     public String getErrorMsgFontAttrs()
     {
         return errorMsgFontAttrs;
     }
 
     public void setErrorMsgFontAttrs(String value)
     {
         errorMsgFontAttrs = value;
     }
 
     public String getCaptionSuffix()
     {
         return captionSuffix;
     }
 
     public void setCaptionSuffix(String value)
     {
         captionSuffix = value;
     }
 
     public String getIncludePreScripts()
     {
         return includePreScripts;
     }
 
     public void setIncludePreScripts(String value)
     {
         includePreScripts = value;
     }
 
     public String getIncludePostScripts()
     {
         return includePostScripts;
     }
 
     public void setIncludePostScripts(String value)
     {
         includePostScripts = value;
     }
 
     public String getIncludePreStyleSheets()
     {
         return includePreStyleSheets;
     }
 
     public void setIncludePreStyleSheets(String value)
     {
         includePreStyleSheets = value;
     }
 
     public String getIncludePostStyleSheets()
     {
         return includePostStyleSheets;
     }
 
     public void setIncludePostStyleSheets(String value)
     {
         includePostStyleSheets = value;
     }
 
     public String getPrependPreScript()
     {
         return prependPreScript;
     }
 
     public void setPrependPreScript(String value)
     {
         prependPreScript = value;
     }
 
     public String getPrependPostScript()
     {
         return prependPostScript;
     }
 
     public void setPrependPostScript(String value)
     {
         prependPostScript = value;
     }
 
     public String getAppendPreScript()
     {
         return appendPreScript;
     }
 
     public void setAppendPreScript(String value)
     {
         appendPreScript = value;
     }
 
     public String getAppendPostScript()
     {
         return appendPostScript;
     }
 
     public void setAppendPostScript(String value)
     {
         appendPostScript = value;
     }
 
     public String getCaptionClass()
     {
         return captionClass;
     }
 
     public void setCaptionClass(String captionClass)
     {
         this.captionClass = captionClass;
     }
 
     public String getControlAreaClass()
     {
         return controlAreaClass;
     }
 
     public void setControlAreaClass(String controlAreaClass)
     {
         this.controlAreaClass = controlAreaClass;
     }
 }
