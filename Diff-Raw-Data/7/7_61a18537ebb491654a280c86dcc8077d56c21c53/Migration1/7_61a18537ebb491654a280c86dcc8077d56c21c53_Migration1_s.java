 package de.deepamehta.plugins.workspaces.migrations;
 
 import de.deepamehta.core.model.DataField;
 import de.deepamehta.core.model.TopicType;
 import de.deepamehta.core.service.Migration;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 
 
 public class Migration1 extends Migration {
 
     @Override
     public void run() {
         addWorkspacesFieldToAllTypes();
         createWorkspaceTopicType();
     }
 
     // ---
 
     private void addWorkspacesFieldToAllTypes() {
         DataField workspacesField = new DataField("Workspaces", "reference");
         workspacesField.setUri("de/deepamehta/core/property/Workspaces");
         workspacesField.setRelatedTypeUri("de/deepamehta/core/topictype/Workspace");
         workspacesField.setEditor("checkboxes");
         //
         for (String typeUri : dms.getTopicTypeUris()) {
             dms.addDataField(typeUri, workspacesField);
         }
     }
 
     private void createWorkspaceTopicType() {
         DataField nameField = new DataField("Name", "text");
         nameField.setUri("de/deepamehta/core/property/Name");
        // nameField.setIndexingMode("FULLTEXT_KEY");
         nameField.setRendererClass("TitleRenderer");
         //
         DataField descriptionField = new DataField("Description", "html");
         descriptionField.setUri("de/deepamehta/core/property/Description");
        // descriptionField.setIndexingMode("FULLTEXT_KEY");
         descriptionField.setRendererClass("BodyTextRenderer");
         //
         List dataFields = new ArrayList();
         dataFields.add(nameField);
         dataFields.add(descriptionField);
         //
         Map properties = new HashMap();
         properties.put("de/deepamehta/core/property/TypeURI", "de/deepamehta/core/topictype/Workspace");
         properties.put("de/deepamehta/core/property/TypeLabel", "Workspace");
         properties.put("icon_src", "/de.deepamehta.3-workspaces/images/star.png");
         properties.put("js_renderer_class", "PlainDocument");
         //
         dms.createTopicType(properties, dataFields);
     }
 }
