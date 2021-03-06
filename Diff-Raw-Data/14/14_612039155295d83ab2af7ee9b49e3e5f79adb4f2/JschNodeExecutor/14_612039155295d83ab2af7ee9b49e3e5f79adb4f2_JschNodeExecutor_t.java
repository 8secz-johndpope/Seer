 /*
  * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
  *
  *  Licensed under the Apache License, Version 2.0 (the "License");
  *  you may not use this file except in compliance with the License.
  *  You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  *  Unless required by applicable law or agreed to in writing, software
  *  distributed under the License is distributed on an "AS IS" BASIS,
  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *  See the License for the specific language governing permissions and
  *  limitations under the License.
  */
 
 /*
 * JschNodeExecutor.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 3/21/11 4:46 PM
 * 
 */
 package com.dtolabs.rundeck.core.execution.impl.jsch;
 
 import com.dtolabs.rundeck.core.Constants;
 import com.dtolabs.rundeck.core.common.Framework;
 import com.dtolabs.rundeck.core.common.FrameworkProject;
 import com.dtolabs.rundeck.core.common.INodeEntry;
 import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
 import com.dtolabs.rundeck.core.execution.ExecutionContext;
 import com.dtolabs.rundeck.core.execution.ExecutionException;
 import com.dtolabs.rundeck.core.execution.ExecutionListener;
 import com.dtolabs.rundeck.core.execution.impl.common.AntSupport;
 import com.dtolabs.rundeck.core.execution.service.NodeExecutor;
 import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;
 import com.dtolabs.rundeck.core.execution.utils.Responder;
 import com.dtolabs.rundeck.core.execution.utils.ResponderThread;
 import com.dtolabs.rundeck.core.plugins.configuration.Describable;
 import com.dtolabs.rundeck.core.plugins.configuration.Description;
 import com.dtolabs.rundeck.core.plugins.configuration.Property;
 import com.dtolabs.rundeck.core.tasks.net.ExtSSHExec;
 import com.dtolabs.rundeck.core.tasks.net.SSHTaskBuilder;
 import com.jcraft.jsch.JSchException;
 import org.apache.log4j.Logger;
 import org.apache.tools.ant.BuildException;
 import org.apache.tools.ant.Project;
 
 import java.io.IOException;
 import java.io.PipedInputStream;
 import java.io.PipedOutputStream;
 import java.text.MessageFormat;
 import java.util.*;
 import java.util.regex.Pattern;
 
 /**
  * JschNodeExecutor is ...
  *
  * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
  */
 public class JschNodeExecutor implements NodeExecutor, Describable {
     public static final Logger logger = Logger.getLogger(JschNodeExecutor.class.getName());
     public static final String SERVICE_PROVIDER_TYPE = "jsch-ssh";
     public static final String FWK_PROP_AUTH_CANCEL_MSG = "framework.messages.error.ssh.authcancel";
     public static final String FWK_PROP_AUTH_CANCEL_MSG_DEFAULT =
         "Authentication failure connecting to node: \"{0}\". Make sure your resource definitions and credentials are up to date.";
    public static final String FWK_PROP_AUTH_FAIL_MSG = "framework.messages.error.ssh.authfail";
    public static final String FWK_PROP_AUTH_FAIL_MSG_DEFAULT =
        "Authentication failure connecting to node: \"{0}\". Password incorrect.";
     public static final String NODE_ATTR_SSH_KEYPATH = "ssh-keypath";
 
     public static final String PROJ_PROP_PREFIX = "project.";
     public static final String FWK_PROP_PREFIX = "framework.";
 
     public static final String FWK_PROP_SSH_KEYPATH = FWK_PROP_PREFIX + NODE_ATTR_SSH_KEYPATH;
     public static final String PROJ_PROP_SSH_KEYPATH = PROJ_PROP_PREFIX + NODE_ATTR_SSH_KEYPATH;
 
     public static final String NODE_ATTR_SSH_AUTHENTICATION = "ssh-authentication";
     public static final String NODE_ATTR_SSH_PASSWORD_OPTION = "ssh-password-option";
     public static final String DEFAULT_SSH_PASSWORD_OPTION = "option.sshPassword";
     public static final String NODE_ATTR_SUDO_PASSWORD_OPTION = "sudo-password-option";
     public static final String DEFAULT_SUDO_PASSWORD_OPTION = "option.sudoPassword";
 
 
     public static final String FWK_PROP_SSH_AUTHENTICATION = FWK_PROP_PREFIX + NODE_ATTR_SSH_AUTHENTICATION;
     public static final String PROJ_PROP_SSH_AUTHENTICATION = PROJ_PROP_PREFIX + NODE_ATTR_SSH_AUTHENTICATION;
 
     public static final String NODE_ATTR_SUDO_COMMAND_ENABLED = "sudo-command-enabled";
     public static final String NODE_ATTR_SUDO_PROMPT_PATTERN = "sudo-prompt-pattern";
     public static final String DEFAULT_SUDO_PROMPT_PATTERN = "^\\[sudo\\] password for .+: .*";
     public static final String NODE_ATTR_SUDO_FAILURE_PATTERN = "sudo-failure-pattern";
     public static final String DEFAULT_SUDO_FAILURE_PATTERN = "^.*try again.*";
     public static final String NODE_ATTR_SUDO_COMMAND_PATTERN = "sudo-command-pattern";
     public static final String DEFAULT_SUDO_COMMAND_PATTERN = "^sudo$";
     public static final String NODE_ATTR_SUDO_PROMPT_MAX_LINES = "sudo-prompt-max-lines";
     public static final int DEFAULT_SUDO_PROMPT_MAX_LINES = 12;
     public static final String NODE_ATTR_SUDO_RESPONSE_MAX_LINES = "sudo-response-max-lines";
     public static final int DEFAULT_SUDO_RESPONSE_MAX_LINES = 1;
     public static final String NODE_ATTR_SUDO_PROMPT_MAX_TIMEOUT = "sudo-prompt-max-timeout";
     public static final long DEFAULT_SUDO_PROMPT_MAX_TIMEOUT = 5000;
     public static final String NODE_ATTR_SUDO_RESPONSE_MAX_TIMEOUT = "sudo-response-max-timeout";
     public static final long DEFAULT_SUDO_RESPONSE_MAX_TIMEOUT = 5000;
     public static final String NODE_ATTR_SUDO_FAIL_ON_PROMPT_MAX_LINES = "sudo-fail-on-prompt-max-lines";
     public static final boolean DEFAULT_SUDO_FAIL_ON_PROMPT_MAX_LINES = false;
     public static final String NODE_ATTR_SUDO_FAIL_ON_PROMPT_TIMEOUT = "sudo-fail-on-prompt-timeout";
     public static final boolean DEFAULT_SUDO_FAIL_ON_PROMPT_TIMEOUT = true;
     public static final String NODE_ATTR_SUDO_FAIL_ON_RESPONSE_TIMEOUT = "sudo-fail-on-response-timeout";
     public static final boolean DEFAULT_SUDO_FAIL_ON_RESPONSE_TIMEOUT = false;
     public static final String NODE_ATTR_SUDO_SUCCESS_ON_PROMPT_THRESHOLD = "sudo-success-on-prompt-threshold";
     public static final boolean DEFAULT_SUDO_SUCCESS_ON_PROMPT_THRESHOLD = true;
 
     private Framework framework;
 
     public JschNodeExecutor(final Framework framework) {
         this.framework = framework;
     }
 
     static final List<Property> CONFIG_PROPERTIES = new ArrayList<Property>();
     static final Map<String, String> CONFIG_MAPPING;
 
     public static final String CONFIG_KEYPATH = "keypath";
     public static final String CONFIG_AUTHENTICATION = "authentication";
 
     static {
 //        CONFIG_PROPERTIES.add(PropertyUtil.string(CONFIG_KEYPATH, "SSH Keypath",
 //            "Path to a private SSH Key file, for use with SSH and SCP. Can be overridden by node attribute \""
 //            + NODE_ATTR_SSH_KEYPATH + "\".", true, null));
 
         final Map<String, String> mapping = new HashMap<String, String>();
         mapping.put(CONFIG_KEYPATH, PROJ_PROP_SSH_KEYPATH);
         mapping.put(CONFIG_AUTHENTICATION, PROJ_PROP_SSH_AUTHENTICATION);
         CONFIG_MAPPING = Collections.unmodifiableMap(mapping);
     }
 
 
     static final Description DESC = new Description() {
         public String getName() {
             return SERVICE_PROVIDER_TYPE;
         }
 
         public String getTitle() {
             return "SSH";
         }
 
         public String getDescription() {
             return "Executes a command on a remote node via SSH.";
         }
 
         public List<Property> getProperties() {
             return CONFIG_PROPERTIES;
         }
 
         public Map<String, String> getPropertiesMapping() {
             return CONFIG_MAPPING;
         }
     };
 
     public Description getDescription() {
         return DESC;
     }
 
     public NodeExecutorResult executeCommand(final ExecutionContext context, final String[] command,
                                              final INodeEntry node) throws
         ExecutionException {
         if (null == node.getHostname() || null == node.extractHostname()) {
             throw new ExecutionException(
                 "Hostname must be set to connect to remote node '" + node.getNodename() + "'");
         }
         if (null == node.extractUserName()) {
             throw new ExecutionException(
                 "Username must be set to connect to remote node '" + node.getNodename() + "'");
         }
 
 
         final ExecutionListener listener = context.getExecutionListener();
         final Project project = new Project();
         AntSupport.addAntBuildListener(listener, project);
 
         boolean success = false;
         final ExtSSHExec sshexec;
         //perform jsch sssh command
         final NodeSSHConnectionInfo nodeAuthentication = new NodeSSHConnectionInfo(node, framework,
             context);
         final int timeout = nodeAuthentication.getSSHTimeout();
         try {
 
             sshexec = SSHTaskBuilder.build(node, command, project, context.getDataContext(),
                 nodeAuthentication, context.getLoglevel());
         } catch (SSHTaskBuilder.BuilderException e) {
             throw new ExecutionException(e);
         }
 
         //Sudo support
         final ResponderThread thread;
         final SudoResponder sudoResponder = SudoResponder.create(node, framework, context);
         if (sudoResponder.isSudoEnabled() && sudoResponder.matchesCommandPattern(command[0])) {
             final PipedInputStream responderInput = new PipedInputStream();
             final PipedOutputStream responderOutput = new PipedOutputStream();
             final PipedInputStream jschInput = new PipedInputStream();
             final PipedOutputStream jschOutput = new PipedOutputStream();
             try {
                 responderInput.connect(jschOutput);
                 jschInput.connect(responderOutput);
             } catch (IOException e) {
                 throw new ExecutionException(e);
             }
             thread = new ResponderThread(sudoResponder, responderInput, responderOutput, sudoResponder);
             sshexec.setAllocatePty(true);
             sshexec.setInputStream(jschInput);
             sshexec.setSecondaryStream(jschOutput);
             sshexec.setDisconnectHolder(sudoResponder);
         } else {
             thread = null;
         }
 
         String errormsg = null;
         try {
             if (null != thread) {
                 thread.start();
             }
             sshexec.execute();
             success = true;
         } catch (BuildException e) {
             if (e.getMessage().contains("Timeout period exceeded, connection dropped")) {
                 errormsg =
                     "Failed execution for node: " + node.getNodename() + ": Execution Timeout period exceeded (after "
                     + timeout + "ms), connection dropped";
             } else if (null != e.getCause() && e.getCause() instanceof JSchException && (
                 e.getCause().getMessage().contains("timeout:") || e.getCause().getMessage().contains(
                     "SocketTimeoutException") || e.getCause().getMessage().contains(
                     "java.net.ConnectException: Operation timed out"))) {
                 errormsg = "Failed execution for node: " + node.getNodename() + ": Connection Timeout (after " + timeout
                            + "ms): " + e.getMessage();
             } else if (null != e.getCause() && e.getCause() instanceof JSchException && e.getCause().getMessage()
                 .contains("Auth cancel")) {
                 String msgformat = FWK_PROP_AUTH_CANCEL_MSG_DEFAULT;
                 if (framework.getPropertyLookup().hasProperty(FWK_PROP_AUTH_CANCEL_MSG)) {
                     msgformat = framework.getProperty(FWK_PROP_AUTH_CANCEL_MSG);
                 }
                errormsg = MessageFormat.format(msgformat, node.getNodename(), e.getMessage());
            }else if (null != e.getCause() && e.getCause() instanceof JSchException && e.getCause().getMessage()
                .contains("Auth fail")) {
                String msgformat = FWK_PROP_AUTH_FAIL_MSG_DEFAULT;
                if (framework.getPropertyLookup().hasProperty(FWK_PROP_AUTH_FAIL_MSG)) {
                    msgformat = framework.getProperty(FWK_PROP_AUTH_FAIL_MSG);
                }
                errormsg = MessageFormat.format(msgformat, node.getNodename(), e.getMessage());
             } else {
                 errormsg = e.getMessage();
             }
             context.getExecutionListener().log(0, errormsg);
         }
         if (null != thread) {
             if (thread.isAlive()) {
                 thread.stopResponder();
             }
             try {
                 thread.join(5000);
             } catch (InterruptedException e) {
             }
             if (!thread.isAlive() && thread.isFailed()) {
                 context.getExecutionListener().log(0,
                     sudoResponder.toString() + " failed: " + thread.getFailureReason());
             }
         }
         final int resultCode = sshexec.getExitStatus();
         final boolean status = success;
         final String resultmsg = null != errormsg ? errormsg : null;
 
         return new NodeExecutorResult() {
             public int getResultCode() {
                 return resultCode;
             }
 
             public boolean isSuccess() {
                 return status;
             }
 
             @Override
             public String toString() {
                 return "[jsch-ssh] result was " + (isSuccess() ? "success" : "failure") + ", resultcode: "
                        + getResultCode() + (null != resultmsg ? ": " + resultmsg : "");
 
             }
         };
     }
 
     final static class NodeSSHConnectionInfo implements SSHTaskBuilder.SSHConnectionInfo {
         final INodeEntry node;
         final Framework framework;
         final ExecutionContext context;
         FrameworkProject frameworkProject;
 
         NodeSSHConnectionInfo(final INodeEntry node, final Framework framework, final ExecutionContext context) {
 
             this.node = node;
             this.framework = framework;
             this.context = context;
             this.frameworkProject = framework.getFrameworkProjectMgr().getFrameworkProject(
                 context.getFrameworkProject());
         }
 
         public SSHTaskBuilder.AuthenticationType getAuthenticationType() {
             if (null != node.getAttributes().get(NODE_ATTR_SSH_AUTHENTICATION)) {
                 return SSHTaskBuilder.AuthenticationType.valueOf(node.getAttributes().get(
                     NODE_ATTR_SSH_AUTHENTICATION));
             }
 
             if (frameworkProject.hasProperty(PROJ_PROP_SSH_AUTHENTICATION)) {
                 return SSHTaskBuilder.AuthenticationType.valueOf(frameworkProject.getProperty(
                     PROJ_PROP_SSH_AUTHENTICATION));
             } else if (framework.hasProperty(FWK_PROP_SSH_AUTHENTICATION)) {
                 return SSHTaskBuilder.AuthenticationType.valueOf(framework.getProperty(FWK_PROP_SSH_AUTHENTICATION));
             } else {
                 return SSHTaskBuilder.AuthenticationType.privateKey;
             }
         }
 
         public String getPrivateKeyfilePath() {
             if (null != node.getAttributes().get(NODE_ATTR_SSH_KEYPATH)) {
                 return node.getAttributes().get(NODE_ATTR_SSH_KEYPATH);
             }
 
             if (frameworkProject.hasProperty(PROJ_PROP_SSH_KEYPATH)) {
                 return frameworkProject.getProperty(PROJ_PROP_SSH_KEYPATH);
             } else if (framework.hasProperty(FWK_PROP_SSH_KEYPATH)) {
                 return framework.getProperty(FWK_PROP_SSH_KEYPATH);
             } else {
                 //return default framework level
                 return framework.getProperty(Constants.SSH_KEYPATH_PROP);
             }
         }
 
         static String evaluateSecureOption(final String optionName, final ExecutionContext context) {
             if (null == optionName) {
                 logger.debug("option name was null");
                 return null;
             }
             if (null == context.getPrivateDataContext()) {
                 logger.debug("private context was null");
                 return null;
             }
             final String[] opts = optionName.split("\\.", 2);
             if (null != opts && 2 == opts.length) {
                 final Map<String, String> option = context.getPrivateDataContext().get(opts[0]);
                 if (null != option) {
                     final String value = option.get(opts[1]);
                     if (null == value) {
                         logger.debug("private context '" + optionName + "' was null");
                     }
                     return value;
                 } else {
                     logger.debug("private context '" + opts[0] + "' was null");
                 }
             }
             return null;
         }
 
 
         public String getPassword() {
             if (null != node.getAttributes().get(NODE_ATTR_SSH_PASSWORD_OPTION)) {
                 return evaluateSecureOption(node.getAttributes().get(NODE_ATTR_SSH_PASSWORD_OPTION), context);
             } else {
                 return evaluateSecureOption(DEFAULT_SSH_PASSWORD_OPTION, context);
             }
         }
 
         public int getSSHTimeout() {
             int timeout = 0;
             if (framework.getPropertyLookup().hasProperty(Constants.SSH_TIMEOUT_PROP)) {
                 final String val = framework.getProperty(Constants.SSH_TIMEOUT_PROP);
                 try {
                     timeout = Integer.parseInt(val);
                 } catch (NumberFormatException e) {
                 }
             }
             return timeout;
         }
 
         public String getUsername() {
             if (null != node.getUsername() || node.containsUserName()) {
                 final String user = node.extractUserName();
                 if (user.contains("${")) {
                     return DataContextUtils.replaceDataReferences(user, context.getDataContext());
                 }
                 return user;
             } else if (frameworkProject.hasProperty("project.ssh.user")
                        && !"".equals(frameworkProject.getProperty("project.ssh.user").trim())) {
                 return frameworkProject.getProperty("project.ssh.user").trim();
             } else {
                 return framework.getProperty(Constants.SSH_USER_PROP);
             }
         }
     }
 
 
     /**
      * Resolve a node/project/framework property by first checking node attributes named X, then project properties
      * named "project.X", then framework properties named "framework.X". If none of those exist, return the default
      * value
      */
     private static String resolveProperty(final String nodeAttribute, final String defaultValue, final INodeEntry node,
                                           final FrameworkProject frameworkProject, final Framework framework) {
 
         if (null != node.getAttributes().get(nodeAttribute)) {
             return node.getAttributes().get(nodeAttribute);
         } else if (frameworkProject.hasProperty(PROJ_PROP_PREFIX + nodeAttribute)
                    && !"".equals(frameworkProject.getProperty(PROJ_PROP_PREFIX + nodeAttribute))) {
             return frameworkProject.getProperty(PROJ_PROP_PREFIX + nodeAttribute);
         } else if (framework.hasProperty(FWK_PROP_PREFIX + nodeAttribute)) {
             return framework.getProperty(FWK_PROP_PREFIX + nodeAttribute);
         } else {
             return defaultValue;
         }
     }
 
     private static int resolveIntProperty(final String attribute, final int defaultValue, final INodeEntry iNodeEntry,
                                           final FrameworkProject frameworkProject, final Framework framework) {
         int value = defaultValue;
         final String string = resolveProperty(attribute, null, iNodeEntry, frameworkProject, framework);
         if (null != string) {
             try {
                 value = Integer.parseInt(string);
             } catch (NumberFormatException e) {
             }
         }
         return value;
     }
 
     private static long resolveLongProperty(final String attribute, final long defaultValue,
                                             final INodeEntry iNodeEntry,
                                             final FrameworkProject frameworkProject, final Framework framework) {
         long value = defaultValue;
         final String string = resolveProperty(attribute, null, iNodeEntry, frameworkProject, framework);
         if (null != string) {
             try {
                 value = Long.parseLong(string);
             } catch (NumberFormatException e) {
             }
         }
         return value;
     }
 
     private static boolean resolveBooleanProperty(final String attribute, final boolean defaultValue,
                                                   final INodeEntry iNodeEntry,
                                                   final FrameworkProject frameworkProject, final Framework framework) {
         boolean value = defaultValue;
         final String string = resolveProperty(attribute, null, iNodeEntry, frameworkProject, framework);
         if (null != string) {
             value = Boolean.parseBoolean(string);
         }
         return value;
     }
 
     /**
      * Sudo responder that determines response patterns from node attributes and project properties. Also handles
      * responder thread result by closing the SSH connection on failure. The mechanism for sudo response: <ol> <li>look
      * for "[sudo] password for &lt;username&gt;: " (inputSuccessPattern)</li> <li>if not seen in 12 lines, then assume
      * password is not needed (isSuccessOnInputThreshold=true)</li> <li>if fewer than 12 lines, and no new input in 5000
      * milliseconds, then fail (isFailOnInputTimeoutThreshold=true)</li> <li>if seen, output password and carriage
      * return (inputString)</li> <li>look for "Sorry, try again" (responseFailurePattern)</li> <li>if not seen in 3
      * lines, or 5000 milliseconds, then assume success (isFailOnResponseThreshold)</li> <li>if seen, then fail</li>
      * </ol>
      */
     private static class SudoResponder implements Responder, ResponderThread.ResultHandler,
         ExtSSHExec.DisconnectHolder {
         private ExtSSHExec.Disconnectable disconnectable;
         private String sudoCommandPattern;
         private String inputSuccessPattern;
         private String inputFailurePattern;
         private String responseSuccessPattern;
         private String responseFailurePattern;
         private int inputMaxLines = -1;
         private long inputMaxTimeout = -1;
         private boolean failOnInputLinesThreshold = false;
         private boolean failOnInputTimeoutThreshold = true;
         private boolean successOnInputThreshold = true;
         private int responseMaxLines = -1;
         private long responseMaxTimeout = -1;
         private boolean failOnResponseThreshold = false;
         private String inputString;
 
         public void setDisconnectable(final ExtSSHExec.Disconnectable disconnectable) {
             this.disconnectable = disconnectable;
         }
 
         public void handleResult(final boolean success, final String reason) {
             if (!success) {
                 if (null != disconnectable) {
                     disconnectable.disconnect();
                 }
             }
         }
 
         private SudoResponder() {
         }
 
         static SudoResponder create(final INodeEntry node, final Framework framework, final ExecutionContext context) {
             final SudoResponder sudoResponder = new SudoResponder();
             sudoResponder.init(node, framework.getFrameworkProjectMgr().getFrameworkProject(
                 context.getFrameworkProject()), framework, context);
             return sudoResponder;
         }
 
 
         public boolean matchesCommandPattern(final String command) {
             final String sudoCommandPattern1 = getSudoCommandPattern();
             if (null != sudoCommandPattern1) {
                 return Pattern.compile(sudoCommandPattern1).matcher(command).matches();
             } else {
                 return false;
             }
         }
 
         private void init(final INodeEntry node, final FrameworkProject frameworkProject,
                           final Framework framework, final ExecutionContext context) {
             sudoEnabled = determineSudoEnabled(node, context);
             if (sudoEnabled) {
                 final String sudoPassword = determineSudoPassword(node, context);
                 if (null != sudoPassword) {
                     inputString = sudoPassword + "\n";
                 }
 
                 sudoCommandPattern = resolveProperty(NODE_ATTR_SUDO_COMMAND_PATTERN, DEFAULT_SUDO_COMMAND_PATTERN, node,
                     frameworkProject, framework);
                 inputSuccessPattern = resolveProperty(NODE_ATTR_SUDO_PROMPT_PATTERN, DEFAULT_SUDO_PROMPT_PATTERN, node,
                     frameworkProject, framework);
                 inputFailurePattern = null;
                 responseFailurePattern = resolveProperty(NODE_ATTR_SUDO_FAILURE_PATTERN, DEFAULT_SUDO_FAILURE_PATTERN,
                     node,
                     frameworkProject, framework);
                 responseSuccessPattern = null;
                 inputMaxLines = resolveIntProperty(NODE_ATTR_SUDO_PROMPT_MAX_LINES, DEFAULT_SUDO_PROMPT_MAX_LINES,
                     node,
                     frameworkProject, framework);
                 inputMaxTimeout = resolveLongProperty(NODE_ATTR_SUDO_PROMPT_MAX_TIMEOUT,
                     DEFAULT_SUDO_PROMPT_MAX_TIMEOUT,
                     node, frameworkProject, framework);
                 responseMaxLines = resolveIntProperty(NODE_ATTR_SUDO_RESPONSE_MAX_LINES,
                     DEFAULT_SUDO_RESPONSE_MAX_LINES,
                     node, frameworkProject, framework);
                 responseMaxTimeout = resolveLongProperty(NODE_ATTR_SUDO_RESPONSE_MAX_TIMEOUT,
                     DEFAULT_SUDO_RESPONSE_MAX_TIMEOUT, node, frameworkProject, framework);
 
                 failOnInputLinesThreshold = resolveBooleanProperty(NODE_ATTR_SUDO_FAIL_ON_PROMPT_MAX_LINES,
                     DEFAULT_SUDO_FAIL_ON_PROMPT_MAX_LINES, node, frameworkProject, framework);
 
                 failOnInputTimeoutThreshold = resolveBooleanProperty(NODE_ATTR_SUDO_FAIL_ON_PROMPT_TIMEOUT,
                     DEFAULT_SUDO_FAIL_ON_PROMPT_TIMEOUT, node, frameworkProject, framework);
                 failOnResponseThreshold = resolveBooleanProperty(NODE_ATTR_SUDO_FAIL_ON_RESPONSE_TIMEOUT,
                     DEFAULT_SUDO_FAIL_ON_RESPONSE_TIMEOUT, node, frameworkProject, framework);
                 successOnInputThreshold = resolveBooleanProperty(NODE_ATTR_SUDO_SUCCESS_ON_PROMPT_THRESHOLD,
                     DEFAULT_SUDO_SUCCESS_ON_PROMPT_THRESHOLD, node, frameworkProject, framework);
             }
         }
 
         private boolean sudoEnabled = false;
 
         /**
          * Return true if sudo should be used for the command execution on this node
          */
         public boolean isSudoEnabled() {
             return sudoEnabled;
         }
 
         public String getInputString() {
             return inputString;
         }
 
         /**
          * Determine if sudo should be used: sudo-enabled property must be "true" and the sudo password must be set
          */
         private boolean determineSudoEnabled(final INodeEntry node, final ExecutionContext context) {
             if (null != node.getAttributes().get(NODE_ATTR_SUDO_COMMAND_ENABLED)) {
                 final boolean enabled = Boolean.parseBoolean(node.getAttributes().get(NODE_ATTR_SUDO_COMMAND_ENABLED));
                 if (enabled && null != determineSudoPassword(node, context)) {
                     return true;
                 }
             }
             return false;
         }
 
         /**
          * Determine the sudo password to use
          */
         private String determineSudoPassword(final INodeEntry node, final ExecutionContext context) {
             if (null != node.getAttributes().get(NODE_ATTR_SUDO_PASSWORD_OPTION)) {
                 return NodeSSHConnectionInfo.evaluateSecureOption(node.getAttributes().get(
                     NODE_ATTR_SUDO_PASSWORD_OPTION),
                     context);
             } else {
                 return NodeSSHConnectionInfo.evaluateSecureOption(DEFAULT_SUDO_PASSWORD_OPTION, context);
             }
         }
 
 
         public boolean isFailOnInputLinesThreshold() {
             return failOnInputLinesThreshold;
         }
 
         public boolean isFailOnInputTimeoutThreshold() {
             return failOnInputTimeoutThreshold;
         }
 
         public boolean isFailOnResponseThreshold() {
             return failOnResponseThreshold;
         }
 
         public boolean isSuccessOnInputThreshold() {
             return successOnInputThreshold;
         }
 
         /**
          * Return the regular expression to use to match the command to determine if Sudo should be used.
          */
         public String getSudoCommandPattern() {
             return sudoCommandPattern;
         }
 
         public String getInputFailurePattern() {
             return inputFailurePattern;
         }
 
         public String getInputSuccessPattern() {
 
             return inputSuccessPattern;
         }
 
         public String getResponseSuccessPattern() {
             return responseSuccessPattern;
         }
 
         public String getResponseFailurePattern() {
             return responseFailurePattern;
         }
 
         public int getInputMaxLines() {
             return inputMaxLines;
         }
 
         public long getInputMaxTimeout() {
             return inputMaxTimeout;
         }
 
 
         public int getResponseMaxLines() {
             return responseMaxLines;
         }
 
         public long getResponseMaxTimeout() {
             return responseMaxTimeout;
         }
 
         @Override
         public String toString() {
             return "Sudo execution password response";
         }
 
     }
 }
