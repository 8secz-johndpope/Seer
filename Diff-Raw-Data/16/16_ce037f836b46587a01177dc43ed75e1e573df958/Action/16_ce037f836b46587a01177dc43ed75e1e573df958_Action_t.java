 /*
  * Copyright (c) 2006-2007 Chris Smith, Shane Mc Cormack, Gregory Holmes
  *
  * Permission is hereby granted, free of charge, to any person obtaining a copy
  * of this software and associated documentation files (the "Software"), to deal
  * in the Software without restriction, including without limitation the rights
  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  * copies of the Software, and to permit persons to whom the Software is
  * furnished to do so, subject to the following conditions:
  *
  * The above copyright notice and this permission notice shall be included in
  * all copies or substantial portions of the Software.
  *
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  * SOFTWARE.
  */
 
 package com.dmdirc.actions;
 
 import com.dmdirc.actions.interfaces.ActionType;
 import com.dmdirc.actions.interfaces.ActionComponent;
 import com.dmdirc.actions.interfaces.ActionComparison;
 import com.dmdirc.logger.ErrorLevel;
 import com.dmdirc.logger.Logger;
 import com.dmdirc.util.ConfigFile;
 import com.dmdirc.util.InvalidConfigFileException;
 
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.IOException;
 import java.io.Serializable;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Properties;
 
 /**
  * Describes a single action.
  *
  * @author chris
  */
 public class Action extends ActionModel implements Serializable {
 
     /**
      * A version number for this class. It should be changed whenever the class
      * structure is changed (or anything else that would prevent serialized
      * objects being unserialized with the new class).
      */
     private static final long serialVersionUID = 1;
 
     private static final String DOMAIN_CONDITIONTREE = "conditiontree".intern();
     private static final String DOMAIN_FORMAT = "format".intern();
     private static final String DOMAIN_METADATA = "metadata".intern();
     private static final String DOMAIN_RESPONSE = "response".intern();
     private static final String DOMAIN_TRIGGERS = "triggers".intern();
 
     /** The file containing this action. */
     private File file;
 
     /** The location of the file we're reading/saving. */
     private String location;
 
     /** The config file we're using. */
     private ConfigFile config;
 
     /** The properties read for this action. */
     private Properties properties;
 
     /**
      * Creates a new instance of Action. The group and name specified must
      * be the group and name of a valid action already saved to disk.
      *
      * @param group The group the action belongs to
      * @param name The name of the action
      */
     public Action(final String group, final String name) {
         super(group, name);
 
         location = ActionManager.getDirectory() + group + File.separator + name;
 
         file = new File(location);
 
         try {
             config = new ConfigFile(location);
             config.read();
             loadActionFromConfig();
         } catch (InvalidConfigFileException ex) {
             // This isn't a valid config file. Maybe it's a properties file?
 
             loadProperties();
         } catch (IOException ex) {
             Logger.userError(ErrorLevel.HIGH, "I/O error when loading action: "
                     + group + "/" + name + ": " + ex.getMessage());
         }
     }
 
     /**
      * Loads this action from a properties file.
      */
     private void loadProperties() {
         try {
             final FileInputStream inputStream = new FileInputStream(file);
 
             properties = new Properties();
             properties.load(inputStream);
             loadActionFromProperties();
 
             inputStream.close();
         } catch (IOException ex) {
             Logger.userError(ErrorLevel.HIGH, "I/O error when loading action: "
                     + group + "/" + name + ": " + ex.getMessage());
         }
     }
 
     /**
      * Creates a new instance of Action with the specified properties and saves
      * it to disk.
      *
      * @param group The group the action belongs to
      * @param name The name of the action
      * @param triggers The triggers to use
      * @param response The response to use
      * @param conditions The conditions to use
      * @param newFormat The new formatter to use
      */
     public Action(final String group, final String name,
             final ActionType[] triggers, final String[] response,
             final List<ActionCondition> conditions, final String newFormat) {
         this(group, name, triggers, response, conditions,
                 ConditionTree.createConjunction(conditions.size()), newFormat);
     }
 
     /**
      * Creates a new instance of Action with the specified properties and saves
      * it to disk.
      *
      * @param group The group the action belongs to
      * @param name The name of the action
      * @param triggers The triggers to use
      * @param response The response to use
      * @param conditions The conditions to use
      * @param conditionTree The condition tree to use
      * @param newFormat The new formatter to use
      */
     public Action(final String group, final String name,
             final ActionType[] triggers, final String[] response,
             final List<ActionCondition> conditions,
             final ConditionTree conditionTree, final String newFormat) {
         super(group, name, triggers, response, conditions, conditionTree, newFormat);
 
         final String dir = ActionManager.getDirectory() + group + File.separator;
         location = dir + name;
 
         new File(dir).mkdirs();
 
         file = new File(location);
 
         ActionManager.registerAction(this);
     }
 
     /**
      * Loads this action from the config instance.
      */
     private void loadActionFromConfig() {
         if (config.isFlatDomain(DOMAIN_TRIGGERS)) {
             if (!loadTriggers(config.getFlatDomain(DOMAIN_TRIGGERS))) {
                 return;
             }
         } else {
             error("No trigger specified");
             return;
         }
 
         if (config.isFlatDomain(DOMAIN_RESPONSE)) {
             response = new String[config.getFlatDomain(DOMAIN_RESPONSE).size()];
 
             int i = 0;
             for (String line: config.getFlatDomain(DOMAIN_RESPONSE)) {
                 response[i++] = line;
             }
         } else {
             error("No response specified");
             return;
         }
 
         if (config.isFlatDomain(DOMAIN_FORMAT)) {
             newFormat = config.getFlatDomain(DOMAIN_FORMAT).size() == 0 ? "" :
                 config.getFlatDomain(DOMAIN_FORMAT).get(0);
         }
 
         for (int cond = 0; config.isKeyDomain("condition " + cond); cond++) {
             if (!readCondition(config.getKeyDomain("condition " + cond))) {
                 return;
             }
         }
 
         if (config.isFlatDomain(DOMAIN_CONDITIONTREE)) {
             conditionTree = ConditionTree.parseString(
                     config.getFlatDomain(DOMAIN_CONDITIONTREE).get(0));
 
             if (conditionTree == null) {
                 error("Unable to parse condition tree");
                 return;
             }
 
             if (conditionTree.getMaximumArgument() >= conditions.size()) {
                 error("Condition tree references condition "
                         + conditionTree.getMaximumArgument() + " but there are"
                         + " only " + conditions.size() + " conditions");
                 return;
             }
         }
 
         ActionManager.registerAction(this);
 
         checkMetaData();
     }
 
     /**
      * Checks to see if this action contains group meta-data, and adds it to
      * the group as appropriate.
      */
     private void checkMetaData() {
         if (config.isKeyDomain(DOMAIN_METADATA)) {
             final ActionGroup myGroup = ActionManager.getGroup(group);
             final Map<String, String> data = config.getKeyDomain(DOMAIN_METADATA);
 
             if (data.containsKey("description")) {
                 myGroup.setDescription(data.get("description"));
             }
 
             if (data.containsKey("author")) {
                 myGroup.setAuthor(data.get("author"));
             }
             
             if (data.containsKey("version")) {
                 try {
                     myGroup.setVersion(Integer.parseInt(data.get("version")));
                 } catch (NumberFormatException ex) {
                     // Do nothing
                 }
             }
             
             if (data.containsKey("component")) {
                 myGroup.setComponent(data.get("component"));
             }
         }
 
         for (int i = 0; config.isKeyDomain("setting " + i); i++) {
             final ActionGroup myGroup = ActionManager.getGroup(group);
             final Map<String, String> data = config.getKeyDomain("setting " + i);
             
             if (data.containsKey("type") && data.containsKey("setting")
                     && data.containsKey("title") && data.containsKey("default")
                     && data.containsKey("tooltip")) {
                 myGroup.getSettings().add(new ActionSetting(
                         ActionSetting.TYPE.valueOf(data.get("type")),
                         data.get("setting"), data.get("title"), 
                         data.get("tooltip"), data.get("default")));
                 ActionManager.registerDefault(data.get("setting"), data.get("default"));
             }
         }
     }
 
     private boolean loadTriggers(final List<String> newTriggers) {
         triggers = new ActionType[newTriggers.size()];
 
         for (int i = 0; i < triggers.length; i++) {
             triggers[i] = ActionManager.getActionType(newTriggers.get(i));
 
             if (triggers[i] == null) {
                 error("Invalid trigger specified: " + newTriggers.get(i));
                 return false;
             } else if (i != 0 && !triggers[i].getType().equals(triggers[0].getType())) {
                 error("Triggers are not compatible");
                 return false;
             }
         }
 
         return true;
     }
 
     /**
      * Loads the various attributes of this action from the properties instance.
      */
     private void loadActionFromProperties() {
         // Read the triggers
         if (properties.containsKey("trigger")) {
             final String[] triggerStrings = properties.getProperty("trigger").split("\\|");
 
             loadTriggers(Arrays.asList(triggerStrings));
         } else {
             error("No trigger specified");
             return;
         }
 
         // Read the response
         if (properties.containsKey(DOMAIN_RESPONSE)) {
             response = properties.getProperty(DOMAIN_RESPONSE).split("\n");
         } else {
             error("No response specified");
             return;
         }
 
         // Read the format change
         if (properties.containsKey(DOMAIN_FORMAT)) {
             newFormat = properties.getProperty(DOMAIN_FORMAT);
         }
 
         // Read the conditions
         int numConditions = 0;
 
         if (properties.containsKey("conditions")) {
             try {
                 numConditions = Integer.parseInt(properties.getProperty("conditions"));
             } catch (NumberFormatException ex) {
                 error("Invalid number of conditions specified");
                 return;
             }
         }
 
         boolean valid = true;
 
         for (int i = 0; i < numConditions; i++) {
             valid = valid & readCondition(i);
         }
 
         if (!valid) {
             return;
         }
 
         if (properties.containsKey(DOMAIN_CONDITIONTREE)) {
             conditionTree = ConditionTree.parseString(properties.getProperty(DOMAIN_CONDITIONTREE));
 
             if (conditionTree == null) {
                 error("Unable to parse condition tree");
                 return;
             }
 
             if (conditionTree.getMaximumArgument() >= conditions.size()) {
                 error("Condition tree references condition "
                         + conditionTree.getMaximumArgument() + " but there are"
                         + " only " + conditions.size() + " conditions");
                 return;
             }
         }
 
         ActionManager.registerAction(this);
         save();
     }
 
     /**
      * Called to save the action.
      */
     public void save() {
         final ConfigFile newConfig = new ConfigFile(location);
 
         final List<String> triggerNames = new ArrayList<String>();
         final List<String> responseLines = new ArrayList<String>();
 
         for (ActionType trigger : triggers) {
             if (trigger == null) {
                 Logger.appError(ErrorLevel.LOW, "ActionType was null",
                         new IllegalArgumentException("Triggers: "
                         + Arrays.toString(triggers)));
                 continue;
             }
 
             triggerNames.add(trigger.toString());
         }
 
         for (String line : response) {
             responseLines.add(line);
         }
 
         newConfig.addDomain(DOMAIN_TRIGGERS, triggerNames);
         newConfig.addDomain(DOMAIN_RESPONSE, responseLines);
 
         if (conditionTree != null) {
             newConfig.addDomain(DOMAIN_CONDITIONTREE, new ArrayList<String>());
             newConfig.getFlatDomain(DOMAIN_CONDITIONTREE).add(conditionTree.toString());
         }
 
         if (newFormat != null) {
             newConfig.addDomain(DOMAIN_FORMAT, new ArrayList<String>());
             newConfig.getFlatDomain(DOMAIN_FORMAT).add(newFormat.toString());
         }
 
         int i = 0;
         for (ActionCondition condition : conditions) {
             final Map<String, String> data = new HashMap<String, String>();
 
             data.put("argument", String.valueOf(condition.getArg()));
             data.put("component", condition.getComponent().toString());
             data.put("comparison", condition.getComparison().toString());
             data.put("target", condition.getTarget());
 
             newConfig.addDomain("condition " + i, data);
             i++;
         }
 
        if (config != null) {
            // Preserve any meta-data
            if (config.isKeyDomain(DOMAIN_METADATA)) {
                newConfig.addDomain(DOMAIN_METADATA, config.getKeyDomain(DOMAIN_METADATA));
            }

            for (i = 0; config.isKeyDomain("setting " + i); i++) {
                newConfig.addDomain("setting " + i, config.getKeyDomain("setting " + i));
            }
         }
 
         try {
             newConfig.write();
 
             resetModified();
         } catch (IOException ex) {
             Logger.userError(ErrorLevel.HIGH, "I/O error when saving action: "
                     + group + "/" + name + ": " + ex.getMessage());
         }
     }
 
     private boolean readCondition(final Map<String,String> data) {
         int arg = 0;
         ActionComponent component = null;
         ActionComparison comparison = null;
         String target = "";
 
         // ------ Read the argument
 
         try {
             arg = Integer.parseInt(data.get("argument"));
         } catch (NumberFormatException ex) {
             error("Invalid argument number specified: " + data.get("argument"));
             return false;
         }
 
         if (arg < 0 || arg >= triggers[0].getType().getArity()) {
             error("Invalid argument number specified: " + arg);
             return false;
         }
 
         // ------ Read the component
 
         component = readComponent(data, arg);
         if (component == null) {
             return false;
         }
 
         // ------ Read the comparison
 
         comparison = ActionManager.getActionComparison(data.get("comparison"));
         if (comparison == null) {
             error("Invalid comparison specified: " + data.get("comparison"));
             return false;
         }
 
         if (!comparison.appliesTo().equals(component.getType())) {
             error("Comparison cannot be applied to specified component: " + data.get("comparison"));
             return false;
         }
 
         // ------ Read the target
 
         target = data.get("target");
 
         if (target == null) {
             error("No target specified for condition");
             return false;
         }
 
         conditions.add(new ActionCondition(arg, component, comparison, target));
         return true;
     }
 
     private ActionComponent readComponent(final Map<String, String> data, final int arg) {
         final String componentName = data.get("component");
         ActionComponent component;
 
         if (componentName.indexOf('.') == -1) {
             component = ActionManager.getActionComponent(componentName);
         } else {
             try {
                 component = new ActionComponentChain(triggers[0].getType().getArgTypes()[arg],
                         componentName);
             } catch (IllegalArgumentException iae) {
                 error(iae.getMessage());
                 return null;
             }
         }
 
         if (component == null) {
             error("Unknown component: " + componentName);
             return null;
         }
 
         if (!component.appliesTo().equals(triggers[0].getType().getArgTypes()[arg])) {
             error("Component cannot be applied to specified arg in condition: " + componentName);
             return null;
         }
 
         return component;
     }
 
     /**
      * Reads the specified condition.
      *
      * @param condition Condition number to read
      * @return True if the condition was read successfully.
      */
     private boolean readCondition(final int condition) {
         // It may help to close your eyes while reading this method.
 
         int arg = -1;
         ActionComponent component = null;
         ActionComparison comparison = null;
         String target = "";
 
         if (properties.containsKey("condition" + condition + "-arg")) {
             try {
                 arg = Integer.parseInt(properties.getProperty("condition" + condition + "-arg"));
             } catch (NumberFormatException ex) {
                 error("Invalid argument number for condition " + condition);
                 return false;
             }
         }
 
         if (arg < 0 || arg >= triggers[0].getType().getArity()) {
             error("Invalid argument number for condition " + condition);
             return false;
         }
 
         if (properties.containsKey("condition" + condition + "-component")) {
             final String componentName = properties.getProperty("condition"
                     + condition + "-component");
 
             if (componentName.indexOf('.') == -1) {
                 component = ActionManager.getActionComponent(componentName);
             } else {
                 try {
                     component = new ActionComponentChain(triggers[0].getType().getArgTypes()[arg],
                             componentName);
                 } catch (IllegalArgumentException iae) {
                     error(iae.getMessage());
                     return false;
                 }
             }
 
             if (component == null) {
                 error("Invalid component for condition " + condition);
                 return false;
             }
 
             if (!component.appliesTo().equals(triggers[0].getType().getArgTypes()[arg])) {
                 error("Component cannot be applied to specified arg in condition " + condition);
                 return false;
             }
         } else {
             error("No component specified for condition " + condition);
             return false;
         }
 
         if (properties.containsKey("condition" + condition + "-comparison")) {
             comparison = ActionManager.getActionComparison(
                     properties.getProperty("condition" + condition + "-comparison"));
             if (comparison == null) {
                 error("Invalid comparison for condition " + condition);
                 return false;
             }
 
             if (!comparison.appliesTo().equals(component.getType())) {
                 error("Comparison cannot be applied to specified component in condition "
                         + condition);
                 return false;
             }
         } else {
             error("No comparison specified for condition " + condition);
             return false;
         }
 
         if (properties.containsKey("condition" + condition + "-target")) {
             target = properties.getProperty("condition" + condition + "-target");
         } else {
             error("No target specified for condition " + condition);
             return false;
         }
 
         conditions.add(new ActionCondition(arg, component, comparison, target));
         return true;
     }
 
     /**
      * Raises a trivial error, informing the user of the problem.
      *
      * @param message The message to be raised
      */
     private void error(final String message) {
         Logger.userError(ErrorLevel.LOW, "Error when parsing action: "
                 + group + "/" + name + ": " + message);
     }
 
     /** {@inheritDoc} */
     @Override
     public void setName(final String newName) {
         super.setName(newName);
 
         file.delete();
 
         file = new File(file.getParent() + System.getProperty("file.separator") + newName);
 
         save();
     }
 
     /** {@inheritDoc} */
     @Override
     public void setGroup(final String newGroup) {
         super.setGroup(newGroup);
 
         location = ActionManager.getDirectory() + group + File.separator + name;
 
         file.delete();
         file = new File(location);
 
         save();
     }
 
     /**
      * Deletes this action.
      */
     public void delete() {
         file.delete();
     }
 
     /** {@inheritDoc} */
     @Override
     public String toString() {
         final String parent = super.toString();
 
         return parent.substring(0, parent.length() - 1)
                 + ",file=" + file + "]";
     }
 
 }
