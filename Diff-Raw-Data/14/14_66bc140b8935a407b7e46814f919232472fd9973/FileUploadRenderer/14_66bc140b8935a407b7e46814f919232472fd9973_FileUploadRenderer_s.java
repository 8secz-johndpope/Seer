 /*
  * OpenFaces - JSF Component Library 2.0
  * Copyright (C) 2007-2011, TeamDev Ltd.
  * licensing@openfaces.org
  * Unless agreed in writing the contents of this file are subject to
  * the GNU Lesser General Public License Version 2.1 (the "LGPL" License).
  * This library is distributed in the hope that it will be useful, but
  * WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  * Please visit http://openfaces.org/licensing/ for more details.
  */
 package org.openfaces.renderkit.input;
 
 import org.openfaces.component.input.FileUpload;
 import org.openfaces.component.output.ProgressBar;
 import org.openfaces.event.FileUploadItem;
 import org.openfaces.event.UploadCompletionEvent;
 import org.openfaces.org.json.JSONArray;
 import org.openfaces.org.json.JSONException;
 import org.openfaces.org.json.JSONObject;
 import org.openfaces.renderkit.AjaxPortionRenderer;
 import org.openfaces.renderkit.RendererBase;
 import org.openfaces.util.AjaxUtil;
 import org.openfaces.util.Rendering;
 import org.openfaces.util.Resources;
 import org.openfaces.util.Script;
 import org.openfaces.util.ScriptBuilder;
 import org.openfaces.util.StyleGroup;
 import org.openfaces.util.Styles;
 
 import javax.el.ELContext;
 import javax.el.MethodExpression;
 import javax.faces.component.UIComponent;
 import javax.faces.context.ExternalContext;
 import javax.faces.context.FacesContext;
 import javax.faces.context.ResponseWriter;
 import javax.servlet.http.HttpServletRequest;
 import java.io.IOException;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 
 public class FileUploadRenderer extends RendererBase implements AjaxPortionRenderer {
     private static final String DIV_FOR_INPUTS_ID = "::inputs";
     private static final String INPUT_OF_FILE_ID = "::fileInput";
     private static final String DIV_FOR_INFO_ID = "::infoDiv";
     public static final String DIV_HEADER_ID = "::header";
 
     private static final String ADD_BTN_ID = "::addButton";
     private static final String TITLE_ADD_BTN_DIV_ID = "::title";
     private static final String INPUT_DIV_OF_ADD_BTN_ID = "::forInput";
 
     /*facet names and components which is used in this component*/
     //private static final String F_ADD_BUTTON_TITLE = "addButtonTitle";
     private static final String F_UPLOAD_BUTTON = "uploadButton";
     private static final String F_CLEAR_ALL_BUTTON = "clearAllButton";
 
     private static final String F_CLEAR_BUTTON = "clearButton";
     private static final String F_STOP_BUTTON = "stopLoadButton";
     private static final String F_CANCEL_BUTTON = "cancelButton";
     private static final String F_PROGRESS_BAR = "progressBar";
     /*id of elements*/
     private static final String CANCEL_BTN_ID = "::cancelFacet";
     private static final String CLEAR_BTN_ID = "::clearFacet";
     private static final String STOP_BTN_ID = "::stopFacet";
 
     private static final String UPLOAD_BTN_ID = "::uploadBtn";
     private static final String CLEAR_ALL_BTN_ID = "::clearAllBtn";
 
     private static final String CLEAR_BUTTON_ID = "::clearButton";//if facet is using
     private static final String STOP_BUTTON_ID = "::stopButton";//if facet is using
     private static final String CANCEL_BUTTON_ID = "::cancelButton";//if facet is using
 
     private static final String FOOTER_DIV_ID = "::footer";
     private static final String HELP_ELEMENTS_ID = "::elements";
     private static final String HELPFUL_INPUT = "::helpfulInput";
     private static final String DEF_PROGRESS_ID = "progressBar";
 
 
     //private UIComponent addButtonTitle;
     private UIComponent uploadButton;
     private UIComponent clearAllButton;
     private UIComponent cancelButton;
     private UIComponent stopButton;
     private UIComponent clearButton;
     private ProgressBar progressBar;
 
     /*progress*/
     private static final String AJAX_PARAM_PROGRESS_REQUEST = "progressRequest";
     private static final String AJAX_PARAM_FILE_NAME = "fileName";
     private static final String PROGRESS_ID = "progress_";
 
     /*listOfFiles*/
     private static final String AJAX_FILES_REQUEST = "listOfFilesRequest";
     private static final String AJAX_PARAM_FILES_ID = "idOfFiles";
 
     @Override
     public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
         AjaxUtil.prepareComponentForAjax(context, component);
 
         FileUpload fileUpload = (FileUpload) component;
         ResponseWriter writer = context.getResponseWriter();
         if (((HttpServletRequest) context.getExternalContext().getRequest()).getAttribute("fileUploadRequest") != null) {
             uploadIfExistFiles(context, component);
             return;
         }
 
         setAllFacets(fileUpload);
         String clientId = component.getClientId(context);
         writer.startElement("div", component);
         Rendering.writeIdAttribute(context, fileUpload);
         Rendering.writeStyleAndClassAttributes(writer, fileUpload.getStyle(), fileUpload.getStyleClass(), "o_file_upload");
         Rendering.writeStandardEvents(writer, fileUpload);
 
         writeHeader(context, fileUpload, writer, clientId + DIV_HEADER_ID);
 
         writeMainDivForInfos(context, writer, fileUpload, clientId + DIV_FOR_INFO_ID);
         writeFooter(context, fileUpload, writer, clientId + FOOTER_DIV_ID);
         writeHelpfulElements(context, fileUpload, writer, clientId + HELP_ELEMENTS_ID);
 
         encodeScriptAndStyles(context, fileUpload, clientId);
         writer.endElement("div");
 
 
     }
 
     private void writeHelpfulInput(FacesContext context, FileUpload fileUpload, ResponseWriter writer, String elementId) throws IOException {
         writer.startElement("input", fileUpload);
         writer.writeAttribute("type", "text", null);
         writer.writeAttribute("id", elementId, null);
         writer.writeAttribute("style", "display:none", null);
         writer.writeAttribute("onchange", Rendering.getEventsParam(fileUpload, "onchange"), null);
 
         writer.endElement("input");
     }
 
     private void writeHelpfulElements(FacesContext context, FileUpload fileUpload, ResponseWriter writer, String elementId) throws IOException {
         writer.startElement("div", fileUpload);
         writer.writeAttribute("id", elementId, null);
         writer.writeAttribute("style", "display:none", null);
         writeHelpfulInput(context, fileUpload, writer, elementId + HELPFUL_INPUT);
         writeCancelButton(context, fileUpload, writer, elementId + CANCEL_BTN_ID);
         writeClearButton(context, fileUpload, writer, elementId + CLEAR_BTN_ID);
         writeStopButton(context, fileUpload, writer, elementId + STOP_BTN_ID);
         writeProgressBar(context);
         writer.endElement("div");
     }
 
     private void writeProgressBar(FacesContext context) throws IOException {
         if (progressBar == null) {
             progressBar = new ProgressBar();
            progressBar.setId(DEF_PROGRESS_ID);
         }
         progressBar.encodeAll(context);
 
     }
 
     private void writeClearButton(FacesContext context, FileUpload fileUpload, ResponseWriter writer, String elementId) throws IOException {
         writer.startElement("div", fileUpload);
         writer.writeAttribute("id", elementId, null);
         if (clearButton == null) {
             writer.startElement("input", fileUpload);
             writer.writeAttribute("type", "button", null);
             writer.writeAttribute("id", elementId + CLEAR_BUTTON_ID, null);
             writer.writeAttribute("class", "o_file_clear_btn", null);
             writer.writeAttribute("value", "Clear", null);
             writer.endElement("input");
         } else {
             clearButton.encodeAll(context);
         }
         writer.endElement("div");
     }
 
     private void writeStopButton(FacesContext context, FileUpload fileUpload, ResponseWriter writer, String elementId) throws IOException {
         writer.startElement("div", fileUpload);
         writer.writeAttribute("id", elementId, null);
         if (stopButton == null) {
             writer.startElement("input", fileUpload);
             writer.writeAttribute("type", "button", null);
             writer.writeAttribute("id", elementId + STOP_BUTTON_ID, null);
             writer.writeAttribute("class", "o_file_clear_btn", null);
             writer.writeAttribute("value", "Stop", null);
             writer.endElement("input");
         } else {
             stopButton.encodeAll(context);
         }
         writer.endElement("div");
     }
 
     private void writeCancelButton(FacesContext context, FileUpload fileUpload, ResponseWriter writer, String elementId) throws IOException {
         writer.startElement("div", fileUpload);
         writer.writeAttribute("id", elementId, null);
         if (cancelButton == null) {
             writer.startElement("input", fileUpload);
             writer.writeAttribute("type", "button", null);
             writer.writeAttribute("id", elementId + CANCEL_BUTTON_ID, null);
             writer.writeAttribute("class", "o_file_clear_btn", null);
             writer.writeAttribute("value", "Cancel", null);
             writer.endElement("input");
         } else {
             cancelButton.encodeAll(context);
         }
         writer.endElement("div");
     }
 
     private void setAllFacets(FileUpload fileUpload) {
 
         //addButtonTitle = fileUpload.getFacet(F_ADD_BUTTON_TITLE);
         uploadButton = fileUpload.getFacet(F_UPLOAD_BUTTON);
         clearAllButton = fileUpload.getFacet(F_CLEAR_ALL_BUTTON);
 
         cancelButton = fileUpload.getFacet(F_CANCEL_BUTTON);
         stopButton = fileUpload.getFacet(F_STOP_BUTTON);
         clearButton = fileUpload.getFacet(F_CLEAR_BUTTON);
         progressBar = (ProgressBar) fileUpload.getFacet(F_PROGRESS_BAR);
     }
 
     private void writeMainDivForInfos(FacesContext context, ResponseWriter writer, FileUpload fileUpload, String elementId) throws IOException {
         String styleClass = Styles.getCSSClass(context, fileUpload, fileUpload.getAllInfosStyle(), StyleGroup.regularStyleGroup(), fileUpload.getAllInfosClass(), "o_file_upload_infos");
 
         writer.startElement("table", fileUpload);
         writer.writeAttribute("id", elementId, null);
         writer.writeAttribute("class", styleClass, null);
         writer.endElement("table");
     }
 
     private void writeHeader(FacesContext context, FileUpload fileUpload, ResponseWriter writer, String elementId) throws IOException {
         String styleClass = Styles.getCSSClass(context, fileUpload, fileUpload.getHeaderStyle(), StyleGroup.regularStyleGroup(), fileUpload.getHeaderClass(), "o_file_upload_header");
         writer.startElement("table", fileUpload);
         writer.writeAttribute("id", elementId, null);
         writer.writeAttribute("class", styleClass, null);
         writer.startElement("tr", fileUpload);
         writer.startElement("td", fileUpload);
 
         writeAddButtonTable(context, fileUpload, writer, elementId + ADD_BTN_ID);
         writeUploadButton(context, fileUpload, writer, elementId + UPLOAD_BTN_ID);
         writer.endElement("td");
         writer.endElement("tr");
         writer.endElement("table");
     }
 
     private void writeUploadButton(FacesContext context, FileUpload fileUpload, ResponseWriter writer, String elementId) throws IOException {
         if (uploadButton == null) {
             writer.startElement("input", fileUpload);
             writer.writeAttribute("type", "button", null);
             writer.writeAttribute("id", elementId, null);
             writer.writeAttribute("class", "o_file_upload_btn", null);
             writer.writeAttribute("value", "Upload", null);
             writer.endElement("input");
         } else {
             uploadButton.encodeAll(context);
         }
     }
 
     private void writeClearAllButton(FacesContext context, FileUpload fileUpload, ResponseWriter writer, String elementId) throws IOException {
         if (clearAllButton == null) {
             writer.startElement("input", fileUpload);
             writer.writeAttribute("type", "button", null);
             writer.writeAttribute("id", elementId, null);
             writer.writeAttribute("value", "Clear all", null);
             writer.writeAttribute("class", "o_file_clear_all_btn", null);
             writer.endElement("input");
         } else {
             clearAllButton.encodeAll(context);
         }
     }
 
     private void writeAddButtonTable(FacesContext context, FileUpload fileUpload, ResponseWriter writer, String elementId) throws IOException {
         writer.startElement("table", fileUpload);
         writer.writeAttribute("style", "float:left;", null); //todo temporary
         writer.writeAttribute("id", elementId, null);
         writer.startElement("tr", fileUpload);
         writer.startElement("td", fileUpload);
 
         writer.startElement("div", fileUpload);
         writer.writeAttribute("style", "position:relative", null);
 
         writer.startElement("div", fileUpload);
         writer.writeAttribute("id", elementId + TITLE_ADD_BTN_DIV_ID, null);
 
         writer.startElement("input", fileUpload);
         writer.writeAttribute("type", "button", null);
         writer.writeAttribute("value", fileUpload.getAddButtonLabel(), null);
         writer.endElement("input");
 
         writer.endElement("div");
 
         writer.startElement("div", fileUpload);
         writer.writeAttribute("id", elementId + INPUT_DIV_OF_ADD_BTN_ID, null);
         writer.writeAttribute("style", "overflow:hidden;position:absolute;top:0;width:100%;height:100%", null); //todo temporary
 
         writer.endElement("div");
 
         writer.endElement("div");
 
         writer.endElement("td");
         writer.endElement("tr");
         writer.endElement("table");
     }
 
     private void writeFooter(FacesContext context, FileUpload fileUpload, ResponseWriter writer, String elementId) throws IOException {
         writer.startElement("div", fileUpload);
         writer.writeAttribute("id", elementId, null);
         writeClearAllButton(context, fileUpload, writer, elementId + CLEAR_ALL_BTN_ID);
         writer.endElement("div");
     }
 
     private void encodeScriptAndStyles(FacesContext context, FileUpload fileUpload, String clientId) throws IOException {
         String fileInfoClass = Styles.getCSSClass(context, fileUpload, fileUpload.getFileInfoStyle(), StyleGroup.regularStyleGroup(), fileUpload.getFileInfoClass(), "o_file_upload_info");
         String infoTitleClass = Styles.getCSSClass(context, fileUpload, fileUpload.getInfoTitleStyle(), StyleGroup.regularStyleGroup(), fileUpload.getInfoTitleClass(), "o_file_upload_info_title");
         String infoStatusClass = Styles.getCSSClass(context, fileUpload, fileUpload.getInfoStatusStyle(), StyleGroup.regularStyleGroup(), fileUpload.getInfoStatusClass(), "o_file_upload_info_status");
         String progressBarClass = Styles.getCSSClass(context, fileUpload, fileUpload.getProgressBarStyle(), StyleGroup.regularStyleGroup(), fileUpload.getProgressBarClass(), "o_file_upload_info_progress");
 
         String addButtonClass = Styles.getCSSClass(context, fileUpload, fileUpload.getAddButtonStyle(), StyleGroup.regularStyleGroup(), fileUpload.getAddButtonClass(), null);
         String addButtonOnMouseOverClass = Styles.getCSSClass(context, fileUpload, fileUpload.getAddButtonOnMouseOverStyle(), StyleGroup.regularStyleGroup(), fileUpload.getAddButtonOnMouseOverClass(), null);
         String addButtonOnMouseDownClass = Styles.getCSSClass(context, fileUpload, fileUpload.getAddButtonOnMouseDownStyle(), StyleGroup.regularStyleGroup(), fileUpload.getAddButtonOnMouseDownClass(), null);
         String addButtonOnFocusClass = Styles.getCSSClass(context, fileUpload, fileUpload.getAddButtonOnFocusStyle(), StyleGroup.regularStyleGroup(), fileUpload.getAddButtonOnFocusClass(), null);
         String addButtonDisabledClass = Styles.getCSSClass(context, fileUpload, fileUpload.getAddButtonDisabledStyle(), StyleGroup.regularStyleGroup(), fileUpload.getAddButtonDisabledClass(), "o_file_upload_addBtn_dis");
         Styles.renderStyleClasses(context, fileUpload);
 
         int uploadedSize = 0;
         String headerId = clientId + DIV_HEADER_ID;
         boolean duplicateAllowed = true;//fileUpload.isDuplicateAllowed();
 
         Script initScript = new ScriptBuilder().initScript(context, fileUpload, "O$.FileUpload._init",
                 fileUpload.getMinQuantity(),
                 fileUpload.getMaxQuantity(),
                 uploadedSize,
                 fileInfoClass,
                 infoTitleClass,
                 progressBarClass,
                 infoStatusClass,
                 fileUpload.getStatusNotUploadedText(),
                 fileUpload.getStatusInProgressText(),
                 fileUpload.getStatusUploadedText(),
                 fileUpload.getMaxFileSizeErrorText(),
                 fileUpload.getAcceptedFileTypes(),
                 duplicateAllowed,
                 headerId + ADD_BTN_ID,
                 addButtonClass,
                 addButtonOnMouseOverClass,
                 addButtonOnMouseDownClass,
                 addButtonOnFocusClass,
                 addButtonDisabledClass,
                 (uploadButton == null) ? headerId + UPLOAD_BTN_ID : uploadButton.getClientId(context),
                 (clearAllButton == null) ? clientId + FOOTER_DIV_ID + CLEAR_ALL_BTN_ID : clearAllButton.getClientId(context),
                 fileUpload.isDisabled(),
                 fileUpload.isAutoUpload(),
                 fileUpload.getTabindex(),
                 progressBar.getClientId(context),
                 fileUpload.getStatusStoppedText(),
                 fileUpload.isMultiUpload(),
                 generateUniqueId(clientId)
         );
 
         Rendering.renderInitScript(context, initScript,
                 Resources.utilJsURL(context),
                 Resources.jsonJsURL(context),
                 Resources.internalURL(context, "input/fileUpload.js")
         );
     }
 
     private String generateUniqueId(String clientId) {
         return clientId + System.currentTimeMillis();
     }
 
     private void uploadIfExistFiles(FacesContext context, UIComponent component) {
         FileUpload fileUpload = (FileUpload) component;
         ExternalContext extContext = context.getExternalContext();
         String clientId = fileUpload.getClientId(context);
         try {
             HttpServletRequest request = (HttpServletRequest) extContext.getRequest();
             FileUploadItem uploadedFile = (FileUploadItem) request.getAttribute(clientId + DIV_FOR_INPUTS_ID + INPUT_OF_FILE_ID);
             if (uploadedFile == null)
                 return;
             String id = (String) request.getAttribute("FILE_ID");
             request.getSession().setAttribute(id, uploadedFile);
 
             //file uploaded.
             //fileUpload.getFileUploadedListener().invoke(context.getELContext(), new Object[]{new FileUploadedEvent(fileUpload, uploadedFile)});
         } catch (Exception e) {
             throw new RuntimeException("Could not handle file upload - please ensure that you have correctly configured the filter.", e);
         }
     }
 
     @Override
     public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
     }
 
     @Override
     public void decode(FacesContext context, UIComponent component) {
         System.out.println("decode");
     }
 
     public JSONObject encodeAjaxPortion(FacesContext context, UIComponent component, String portionName, JSONObject jsonParam) throws IOException, JSONException {
         if (jsonParam.has(AJAX_PARAM_PROGRESS_REQUEST)) {
             JSONObject jsonObj = new JSONObject();
             String nameOfFile = (String) jsonParam.get(AJAX_PARAM_FILE_NAME);
             Map<String, Object> sessionMap = context.getExternalContext().getSessionMap();
             if (sessionMap.containsKey(PROGRESS_ID + nameOfFile)) {
                 Integer progress = (Integer) sessionMap.get(PROGRESS_ID + nameOfFile);
                 Rendering.addJsonParam(jsonObj, "progressInPercent", progress);
                 Rendering.addJsonParam(jsonObj, "status", "inProgress");
             } else {
                 Rendering.addJsonParam(jsonObj, "status", "error");
             }
             return jsonObj;
         } else if (jsonParam.has(AJAX_FILES_REQUEST)) {
             JSONObject jsonObj = new JSONObject();
             JSONArray ids = (JSONArray) jsonParam.get(AJAX_PARAM_FILES_ID);
             boolean allUploaded = true;
             Map<String, Object> sessionMap = context.getExternalContext().getSessionMap();
             for (int i = 0; i < ids.length(); i++) {
                 if (!sessionMap.containsKey(ids.getString(i))) {
                     allUploaded = false;
                     break;
                 }
             }
             if (allUploaded) {
                 if (ids.length() != 0) {//call listener
                     List<FileUploadItem> files = new LinkedList<FileUploadItem>();
                     for (int i = 0; i < ids.length(); i++) {
                         String id = ids.getString(i);
                         files.add((FileUploadItem) sessionMap.get(id));
                         sessionMap.remove(id);
                     }
                     FileUpload fileUpload = (FileUpload) component;
                     MethodExpression uploadCompletionListener = fileUpload.getUploadCompletionListener();
                     if (uploadCompletionListener != null) {
                         uploadCompletionListener.invoke(
                                 context.getELContext(), new Object[]{
                                 new UploadCompletionEvent(fileUpload, files)});
                     }
                 }
             }
             Rendering.addJsonParam(jsonObj, "allUploaded", allUploaded);
             return jsonObj;
         }
         return null;
     }
 }
