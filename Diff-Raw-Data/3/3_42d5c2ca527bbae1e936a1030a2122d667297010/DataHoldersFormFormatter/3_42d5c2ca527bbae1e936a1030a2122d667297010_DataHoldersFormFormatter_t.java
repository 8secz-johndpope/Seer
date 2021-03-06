 /**
  * Copyright (C) 2012 JBoss Inc
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *       http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package org.jbpm.formModeler.components.editor;
 
 import org.apache.commons.logging.Log;
 import org.jbpm.formModeler.api.config.FieldTypeManager;
 import org.jbpm.formModeler.api.model.*;
 import org.jbpm.formModeler.api.processing.BindingManager;
 import org.jbpm.formModeler.service.bb.mvc.taglib.formatter.Formatter;
 import org.jbpm.formModeler.service.bb.mvc.taglib.formatter.FormatterException;
 
 import javax.inject.Inject;
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 import java.util.Set;
 
 public class DataHoldersFormFormatter extends Formatter {
 
     @Inject
     private Log log;
 
     public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws FormatterException {
         try {
             WysiwygFormEditor wysiwygFormEditor = WysiwygFormEditor.lookup();
             if (WysiwygFormEditor.EDITION_OPTION_BINDINGS_FIELDS.equals(wysiwygFormEditor.getCurrentEditionOption())){
                 renderPendingFields();
             } else {
                 renderDataHolders();
             }
         }catch (Exception e){
             log.error(" DataHoldersFormFormatter rendering error");
         }
     }
 
     public void renderDataHolders(){
         WysiwygFormEditor wysiwygFormEditor = WysiwygFormEditor.lookup();
         try {
             renderFragment("outputStart");
 
             renderFragment("outputNameInput");
 
             renderFragment("outputStartBindings");
 
             Form form = wysiwygFormEditor.getCurrentForm();
 
             Set<DataHolder> holders =form.getHolders();
             for (DataHolder holder : holders) {
                 setAttribute("id",holder.getId() );
                 setAttribute("type", holder.getTypeCode());
                 setAttribute("renderColor", holder.getRenderColor());
                 setAttribute("value",holder.getInfo());
                 renderFragment("outputBindings");
             }
             renderFragment("outputEndBindings");
 
             renderFragment("outputEnd");
         } catch (Exception e) {
             log.error("Error:", e);
         }
     }
 
     public void renderPendingFields() throws Exception {
         WysiwygFormEditor wysiwygFormEditor = WysiwygFormEditor.lookup();
         Form form = wysiwygFormEditor.getCurrentForm();
         Set<DataHolder> holders=form.getHolders();
         BindingManager bindingManager = wysiwygFormEditor.getBindingManager();
         FieldTypeManager fieldTypeManager = wysiwygFormEditor.getFieldTypesManager();
 
         renderFragment("outputStart");
 
         for (DataHolder dataHolder : holders) {
 
             Set <DataFieldHolder> dataFieldHolders = dataHolder.getFieldHolders();
 
             String fieldName = "";
             int i=0;
            if(dataFieldHolders!=null){
             for (DataFieldHolder dataFieldHolder : dataFieldHolders) {
                 fieldName =dataFieldHolder.getId();
                 if(fieldName!=null && !form.existBinding(dataHolder,fieldName)){
                     if(i==0){//first field
                         setAttribute("id",dataHolder.getId() );
                         setAttribute("type",dataHolder.getTypeCode() );
                         setAttribute("renderColor", dataHolder.getRenderColor());
 
                         if (dataHolder.getId()!=null && dataHolder.getId().equals(wysiwygFormEditor.getLastDataHolderUsedId())){
                             setAttribute("open",Boolean.TRUE);
                         } else {
                             setAttribute("open",Boolean.FALSE);
                         }
                         setAttribute("showHolderName", ((dataHolder.getId()!=null && dataHolder.getId().length()<20)? dataHolder.getId(): dataHolder.getId().substring(0,19) +"..."));
 
                         renderFragment("outputBinding");
 
                     }
                     i++;
                     renderAddField( fieldName, fieldTypeManager.getTypeByCode(dataFieldHolder.getType()), dataHolder.getId());
                 }
             }
             if(i!=0){//last field of list
                 renderFragment("outputEndBinding");
             }
            }
 
         }
 
         renderFragment("outputEnd");
     }
     public void renderAddField(String fieldName, FieldType type,String bindingId){
         WysiwygFormEditor wysiwygFormEditor = WysiwygFormEditor.lookup();
 
         setAttribute("typeName", type.getCode());
         setAttribute("bindingId", bindingId);
         setAttribute("showFieldName", ((fieldName!=null && fieldName.length()<17) ? fieldName: fieldName.substring(0,13) +"..."));
 
         setAttribute("iconUri", wysiwygFormEditor.getFieldTypesManager().getIconPathForCode(type.getCode()));
         setAttribute("fieldName", fieldName);
         renderFragment("outputField");
     }
 }
