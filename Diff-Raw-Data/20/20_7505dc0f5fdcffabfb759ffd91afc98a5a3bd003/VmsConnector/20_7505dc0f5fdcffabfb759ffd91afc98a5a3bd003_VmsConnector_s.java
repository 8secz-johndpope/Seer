 /*
  * ====================
  * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  * 
  * Copyright 2007-2008 Sun Microsystems, Inc. All rights reserved.     
  * 
  * The contents of this file are subject to the terms of the Common Development 
  * and Distribution License("CDDL") (the "License").  You may not use this file 
  * except in compliance with the License.
  * 
  * You can obtain a copy of the License at 
  * http://IdentityConnectors.dev.java.net/legal/license.txt
  * See the License for the specific language governing permissions and limitations 
  * under the License. 
  * 
  * When distributing the Covered Code, include this CDDL Header Notice in each file
  * and include the License file at identityconnectors/legal/license.txt.
  * If applicable, add the following below this CDDL Header, with the fields 
  * enclosed by brackets [] replaced by your own identifying information: 
  * "Portions Copyrighted [year] [name of copyright owner]"
  * ====================
  */
 package org.identityconnectors.vms;
 
 import static org.identityconnectors.vms.VmsConstants.*;
 
 import java.io.IOException;
 import java.text.DateFormat;
 import java.text.MessageFormat;
 import java.text.ParseException;
 import java.text.SimpleDateFormat;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Locale;
 import java.util.Map;
 import java.util.Set;
 import java.util.StringTokenizer;
 import java.util.TimeZone;
 import java.util.UUID;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 import org.identityconnectors.common.CollectionUtil;
 import org.identityconnectors.common.StringUtil;
 import org.identityconnectors.common.logging.Log;
 import org.identityconnectors.common.script.ScriptExecutor;
 import org.identityconnectors.common.script.ScriptExecutorFactory;
 import org.identityconnectors.common.security.GuardedString;
 import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
 import org.identityconnectors.framework.common.exceptions.ConnectorException;
 import org.identityconnectors.framework.common.exceptions.UnknownUidException;
 import org.identityconnectors.framework.common.objects.Attribute;
 import org.identityconnectors.framework.common.objects.AttributeBuilder;
 import org.identityconnectors.framework.common.objects.AttributeInfo;
 import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
 import org.identityconnectors.framework.common.objects.AttributeInfoUtil;
 import org.identityconnectors.framework.common.objects.AttributeUtil;
 import org.identityconnectors.framework.common.objects.ConnectorObject;
 import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
 import org.identityconnectors.framework.common.objects.Name;
 import org.identityconnectors.framework.common.objects.ObjectClass;
 import org.identityconnectors.framework.common.objects.ObjectClassInfo;
 import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
 import org.identityconnectors.framework.common.objects.OperationOptions;
 import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
 import org.identityconnectors.framework.common.objects.OperationalAttributes;
 import org.identityconnectors.framework.common.objects.PredefinedAttributeInfos;
 import org.identityconnectors.framework.common.objects.PredefinedAttributes;
 import org.identityconnectors.framework.common.objects.ResultsHandler;
 import org.identityconnectors.framework.common.objects.Schema;
 import org.identityconnectors.framework.common.objects.SchemaBuilder;
 import org.identityconnectors.framework.common.objects.ScriptContext;
 import org.identityconnectors.framework.common.objects.Uid;
 import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
 import org.identityconnectors.framework.spi.AttributeNormalizer;
 import org.identityconnectors.framework.spi.Configuration;
 import org.identityconnectors.framework.spi.ConnectorClass;
 import org.identityconnectors.framework.spi.PoolableConnector;
 import org.identityconnectors.framework.spi.operations.AuthenticateOp;
 import org.identityconnectors.framework.spi.operations.CreateOp;
 import org.identityconnectors.framework.spi.operations.DeleteOp;
 import org.identityconnectors.framework.spi.operations.SchemaOp;
 import org.identityconnectors.framework.spi.operations.ScriptOnResourceOp;
 import org.identityconnectors.framework.spi.operations.SearchOp;
 import org.identityconnectors.framework.spi.operations.UpdateOp;
 import org.identityconnectors.patternparser.MapTransform;
 import org.identityconnectors.patternparser.Transform;
 
 @ConnectorClass(displayNameKey="VMSConnector", configurationClass= VmsConfiguration.class)
 public class VmsConnector implements PoolableConnector, AuthenticateOp, CreateOp,
 DeleteOp, SearchOp<String>, UpdateOp, SchemaOp, AttributeNormalizer, ScriptOnResourceOp {
     private Log                         _log = Log.getLog(VmsConnector.class);
     private DateFormat                  _vmsDateFormatWithSecs;
     private DateFormat                  _vmsDateFormatWithoutSecs;
     private VmsConfiguration            _configuration;
     private VmsConnection               _connection;
     private Schema                      _schema;
     private Map<String, AttributeInfo>  _attributeMap;
 
     private String                      _changeOwnPasswordCommandScript;
     private ScriptExecutor              _changeOwnPasswordCommandExecutor;
     private String                      _authorizeCommandScript;
     private ScriptExecutor              _authorizeCommandExecutor;
     private String                      _listCommandScript;
     private ScriptExecutor              _listCommandExecutor;
     private String                      _dateCommandScript;
     private ScriptExecutor              _dateCommandExecutor;
 
     private Pattern                     _errorPattern        = Pattern.compile("%\\w+-\\w-\\w+,[^\\n]*\\n");
 
     private final static String         VMS_DELTA_FORMAT    = "{0,number,##}-{1,number,##}:{2,number,##}:{3,number,##}.{4,number,00}";
 
     private static final String         SEPARATOR           = "Username: ";
     private static final String         UAF_PROMPT          = "UAF>";
     private static final String         UAF_PROMPT_CONTINUE = "_UAF>";
     private static final Transform      TRANSFORM           = new MapTransform(VmsAuthorizeInfo.getInfo());
 
     public static final int             LONG_WAIT           = 60000;
     public static final int             SHORT_WAIT          = 60000;
     private static final int            SEGMENT_MAX         = 500;    
 
     // VMS messages we search for
     //
     private static final String USER_ADDED       = "%UAF-I-ADDMSG,";     // user record successfully added
     private static final String USER_EXISTS      = "%UAF-E-UAEERR,";     // invalid user name, user name already exists
     private static final String USER_REMOVED     = "%UAF-I-REMMSG,";     // record removed from system authorization file
     private static final String USER_RENAMED     = "%UAF-I-RENMSG,";     // user record renamed
     private static final String USER_UPDATED     = "%UAF-I-MDFYMSG,";    // user record(s) updated
     private static final String BAD_USER         = "%UAF-W-BADUSR,";     // user name does not exist
     private static final String BAD_SPEC         = "%UAF-W-BADSPC,";     // no user matches specification
 
     public VmsConnector() {
         try {
             _changeOwnPasswordCommandScript = VmsUtilities.readFileFromClassPath("org/identityconnectors/vms/UserPasswordScript.txt");
             _authorizeCommandScript = VmsUtilities.readFileFromClassPath("org/identityconnectors/vms/AuthorizeCommandScript.txt");
             _listCommandScript = VmsUtilities.readFileFromClassPath("org/identityconnectors/vms/ListCommandScript.txt");
             _dateCommandScript = VmsUtilities.readFileFromClassPath("org/identityconnectors/vms/DateCommandScript.txt");
             if (StringUtil.isEmpty(_changeOwnPasswordCommandScript))
                 throw new ConnectorException("Internal error locating command scripts");
         } catch (IOException ioe) {
             throw ConnectorException.wrap(ioe);
         }
     }
 
     /**
      * Since the exclamation point is a comment delimiter in DCL (and
      * AUTHORIZE input), we must quote any strings in which it appears.
      * We also need to quote if any whitespace is contained
      * 
      * @param unquoted
      * @return
      */
     protected char[] quoteWhenNeeded(char[] unquoted) {
         return quoteWhenNeeded(unquoted, false);
     }
 
     protected char[] quoteWhenNeeded(char[] unquoted, boolean needsQuote) {
         boolean quote = needsQuote;
         for (char character : unquoted) {
             if (character == '!' | character == ' ' | character == '\t')
                 quote = true;
         }
         if (unquoted.length==0)
             quote=true;
         if (!quote) {
             char[] result = new char[unquoted.length];
             System.arraycopy(unquoted, 0, result, 0, result.length);
             return result;
         }
 
         return quoteString(new String(unquoted)).toCharArray();
     }
 
     /**
      * VMS quoting rules:
      * <ul>
      * <li>replace a single quote with the sequence "+"'"+" 
      * <br>- end the string, append a single quote, append the remainder
      * <li>replace a double quote with the sequence ""
      * </ul>
      * Then add double quotes around the entire string
      * 
      * @param string
      * @return
      */
     private String quoteString(String string) {
         return "\"" + string.replaceAll("\"", "\"\"").replaceAll("'", "\"+\"'\"+\"") + "\"";
     }
 
     protected char[] asCharArray(Object object) {
         if (object instanceof GuardedString) {
             GuardedString guarded = (GuardedString)object;
             GuardedStringAccessor accessor = new GuardedStringAccessor();
             guarded.access(accessor);
             char[] result = accessor.getArray();
             return result;
         } else if (object!=null) {
             return object.toString().toCharArray();
         } else {
             return null;
         }
     }
 
     protected char[] listToVmsValueList(List<Object> values) {
         if (values.size()==1) {
             if (values.get(0)==null) {
                 return null;
             } else {
                 char[] valueArray = asCharArray(values.get(0));
                 char [] result = quoteWhenNeeded(valueArray);
                 Arrays.fill(valueArray, 0, valueArray.length, ' ');
                 return result;
             }
         } else {
             CharArrayBuffer buffer = new CharArrayBuffer();
             char separator = '(';
             for (Object value : values) {
                 buffer.append(separator);
                 char[] valueArray = asCharArray(value);
                 char [] result = quoteWhenNeeded(valueArray);
                 Arrays.fill(valueArray, 0, valueArray.length, ' ');
                 buffer.append(result);
                 Arrays.fill(result, 0, result.length, ' ');
                 separator = ',';
             }
             buffer.append(")");
             char[] contents = buffer.getArray();
             buffer.clear();
             return contents;
         }
     }
 
     protected boolean isPresent(String base, String subString) {
         if (base==null)
             return false;
         else
             return base.contains(subString);
     }
 
     private List<CharArrayBuffer> appendAttributes(boolean modify, String prefix, Map<String, Attribute> attrMap) {
         List<CharArrayBuffer> commandList = new LinkedList<CharArrayBuffer>();
         CharArrayBuffer command = new CharArrayBuffer();
         if (modify)
             command.append("MODIFY ");
         else
             command.append("ADD ");
         command.append(prefix);
         commandList.add(command);
 
         // Enable/disable is handled by FLAGS=([NO]DISUSER)
         //
         Attribute enable = attrMap.remove(OperationalAttributes.ENABLE_NAME);
         if (enable!=null) {
             Attribute flags = attrMap.remove(ATTR_FLAGS);
             List<Object> flagsValue = new LinkedList<Object>();
             if (flags!= null)
                 flagsValue = new LinkedList<Object>(flags.getValue());
             if (AttributeUtil.getBooleanValue(enable))
                 flagsValue.add("NO"+FLAG_DISUSER);
             else
                 flagsValue.add(FLAG_DISUSER);
             attrMap.put(ATTR_FLAGS, AttributeBuilder.build(ATTR_FLAGS, flagsValue));
         }
 
         // Administrative user doesn't need current password
         //
         Attribute currentPassword = attrMap.remove(OperationalAttributes.CURRENT_PASSWORD_NAME);
 
         // Password expiration date is handled by the /PWDEXPIRED qualifier
         //
         Attribute expiration = attrMap.remove(OperationalAttributes.PASSWORD_EXPIRED_NAME);
         if (expiration!=null) {
             if (AttributeUtil.getBooleanValue(expiration)) {
                 String value = "/"+ATTR_PWDEXPIRED;
                 command = appendToCommand(commandList, command, value);
             } else {
                 String value = "/NO"+ATTR_PWDEXPIRED;
                 command = appendToCommand(commandList, command, value);
             }
         } else {
             Attribute password = attrMap.get(OperationalAttributes.PASSWORD_NAME);
             // If the password is being changed, VMS automatically pre-expires the password.
             // If it wasn't already expired, we don't want to change that.
             //
             if (password!=null && modify) {
                 // Get the user and find if currently pre-expired
                 //
                 List<CharArrayBuffer> addCommand = new LinkedList<CharArrayBuffer>();
                 CharArrayBuffer buffer = new CharArrayBuffer();
                 buffer.append("SHOW "+prefix);
                 addCommand.add(buffer);
 
                 Map<String, Object> variables = new HashMap<String, Object>();
                 fillInCommand(addCommand, variables);
 
                 String result = "";
                 try {
                     result = (String)_authorizeCommandExecutor.execute(variables);
                     if (!result.contains("(pre-expired)")) {
                         String value = "/"+"NO"+ATTR_PWDEXPIRED;
                         command = appendToCommand(commandList, command, value);
                     }
                     clearArrays(variables);
                 } catch (Exception e) {
                     clearArrays(variables);
                     _log.error(e, "error in create");
                     throw new ConnectorException(_configuration.getMessage(VmsMessages.ERROR_IN_CREATE), e);
                 }
             }
         }
 
         // Password change interval is handled by the /PWDLIFETIME qualifier
         //
         Attribute changeInterval = attrMap.remove(PredefinedAttributes.PASSWORD_CHANGE_INTERVAL_NAME);
         if (changeInterval!=null) {
             long expirationTime = AttributeUtil.getLongValue(changeInterval).longValue();
             if (expirationTime==0) {
                 String value = "/"+"NOPWDLIFETIME";
                 command = appendToCommand(commandList, command, value);
             } else {
                 String deltaValue = remapToDelta(expirationTime);
                 String value = "/PWDLIFETIME="+new String(quoteWhenNeeded(deltaValue.toCharArray(), true));
                 command = appendToCommand(commandList, command, value);
             }
         }
 
         for (Attribute attribute : attrMap.values()) {
             String name = attribute.getName();
             List<Object> values = new LinkedList<Object>();
             if (attribute.getValue()!=null)
                 values.addAll(attribute.getValue());
             // Need to update values for list-valued attributes to specify
             // negated values, as well as positive values
             if (ATTR_FLAGS.equalsIgnoreCase(name)) {
                 updateValues(values, VmsAttributeValidator.FLAGS_LIST);
             } else if (ATTR_PRIVILEGES.equalsIgnoreCase(name)) {
                 updateValues(values, VmsAttributeValidator.PRIVS_LIST);
             } else if (ATTR_DEFPRIVILEGES.equalsIgnoreCase(name)) {
                 updateValues(values, VmsAttributeValidator.PRIVS_LIST);
             } else if (ATTR_PRIMEDAYS.equalsIgnoreCase(name)) {
                 updateValues(values, VmsAttributeValidator.PRIMEDAYS_LIST);
             }
             // We treat empty lists as value needed to clear the attribute
             //  Dates are removed with NO prefix
             //  Deltas are removed with NO prefix
             //  Numbers get set to 0
             //  Strings get set to empty string
             //
             if (values.size()==0) {
                 schema();
                 AttributeInfo info = _attributeMap.get(name);
                 if (info!=null) {
                     if (ATTR_EXPIRATION.equals(name)
                             || ATTR_CPUTIME.equals(name)
                             || PredefinedAttributes.PASSWORD_CHANGE_INTERVAL_NAME.equals(name)) {
                         values.add(Boolean.FALSE);
                     } else if (info.getClass().isInstance(Number.class)
                             || info.getClass().isInstance(short.class)
                             || info.getClass().isInstance(int.class)
                             || info.getClass().isInstance(float.class)
                             || info.getClass().isInstance(double.class)
                             || info.getClass().isInstance(long.class)) {
                         values.add(0);
                     } else if (info.getClass().isInstance(Boolean.class)) {
                         values.add(Boolean.FALSE);
                     } else {
                         values.add("");
                     }
                 } else {
                     values.add("");
                 }
             }
             if (!name.equals(Name.NAME) && isNeedsValidation(attribute)) {
                 VmsAttributeValidator.validate(name, values, _configuration);
                 if (values.size()==1 && values.get(0) instanceof Boolean) {
                     // We use boolean value to indicate negatable, valueless
                     // attributes, and to clear time-based attributes
                     //
                     String value = null;
                     if (((Boolean)values.get(0)).booleanValue())
                         value = "/"+remapName(attribute);
                     else
                         value = "/NO"+remapName(attribute);
                     command = appendToCommand(commandList, command, value);
                 } else {
                     if (isDateTimeAttribute(name))
                         remapToDateTime(values);
                     if (isDeltaAttribute(name))
                         remapToDelta(values);
                     char[] value = listToVmsValueList(values);
                     String first = "/"+remapName(attribute)+"=";
                     if (command.length()+first.length()+value.length>SEGMENT_MAX) {
                         command = addNewCommandSegment(commandList, command);
                     }
                     command.append(first);
                     command.append(quoteWhenNeeded(value));
                     Arrays.fill(value, 0, value.length, ' ');
                 }
             }
         }
         return commandList;
     }
 
     private CharArrayBuffer appendToCommand(List<CharArrayBuffer> commandList,
             CharArrayBuffer command, String value) {
         if (command.length()+value.length()>SEGMENT_MAX) {
             command = addNewCommandSegment(commandList, command);                    
         }
         command.append(value);
         return command;
     }
 
     /** 
      * The values list is updated to hold negated values for every possibility that
      * is not in the list
      * @param values
      * @param possibilities
      */
     private void updateValues(List<Object> values, Collection<String> possibilities) {
         // Do case-insensitive value comparison
         //
         Collection<String> newValues = CollectionUtil.newCaseInsensitiveSet();
         for (Object value : values) 
             newValues.add(value.toString());
         for (String possibility : possibilities) {
             if (!newValues.contains(possibility))
                 values.add("NO"+possibility);
         }
     }
 
     private CharArrayBuffer addNewCommandSegment(
             List<CharArrayBuffer> commandList, CharArrayBuffer command) {
         command.append("-");
         command = new CharArrayBuffer();
         commandList.add(command);
         return command;
     }
 
     private boolean isDateTimeAttribute(String attributeName) {
         if (ATTR_EXPIRATION.equals(attributeName))
             return true;
         return false;
     }
 
     private boolean isDeltaAttribute(String attributeName) {
         if (PredefinedAttributes.PASSWORD_CHANGE_INTERVAL_NAME.equals(attributeName))
             return true;
         if (ATTR_CPUTIME.equals(attributeName))
             return true;
         return false;
     }
 
     private void remapToDateTime(List<Object> values) {
         for (int i=0; i<values.size(); i++) {
             Object value = values.get(i);
             values.set(i, _vmsDateFormatWithoutSecs.format(new Date((Long)value)));
         }
     }
 
     static void remapToDelta(List<Object> values) {
         for (int i=0; i<values.size(); i++) {
             Object value = values.get(i);
             value = remapToDelta((Long)value);
             values.set(i, value);
         }
     }
 
     Pattern _deltaPattern = Pattern.compile("(?:(\\d+)\\s)?(\\d+):(\\d+)(?::(\\d+))?(?:.(\\d+))?");
     private long remapFromDelta(String delta) {
         if (delta==null)
             return 0;
         Matcher matcher = _deltaPattern.matcher(delta);
         if (matcher.matches()) {
             String daysS         = matcher.group(1);
             String hoursS        = matcher.group(2);
             String minutesS      = matcher.group(3);
             String secondsS      = matcher.group(4);
             String centisecondsS = matcher.group(5);
 
             long days = daysS==null?0:Long.parseLong(daysS);
             long hours = Long.parseLong(hoursS);
             long minutes = Long.parseLong(minutesS);
             long seconds = secondsS==null?0:Long.parseLong(secondsS);
             long centiseconds = centisecondsS==null?0:Long.parseLong(centisecondsS);
             long result = ((((((((days*24)+hours)*60)+minutes)*60)+seconds)*100)+centiseconds)*10;
             return result;
         }
         return 0;
     }
 
     private static String remapToDelta(Long longValue) {
         // Convert to hundredths of a second
         longValue /= 10;
         long centiseconds = longValue % 100;
         // Convert to seconds
         longValue /= 100;
         long seconds = longValue % 60;
         // Convert to minutes
         longValue /= 60;
         long minutes = longValue % 60;
         // Convert to hours
         longValue /= 60;
         long hours = longValue % 24;
         // Convert to days
         longValue /= 24;
         long days = longValue;
         String value = MessageFormat.format(VMS_DELTA_FORMAT, new Object[] {days, hours, minutes, seconds, centiseconds});
         return value;
     }
 
     private String remapName(Attribute attribute) {
         if (attribute.is(OperationalAttributes.PASSWORD_NAME)) 
             return "PASSWORD";
         if (attribute.is(PredefinedAttributes.PASSWORD_CHANGE_INTERVAL_NAME)) 
             return "PWDLIFETIME";
         else
             return attribute.getName();
     }
 
     private boolean isNeedsValidation(Attribute attribute) {
         if (attribute.is(OperationalAttributes.CURRENT_PASSWORD_NAME)) 
             return false;
         if (attribute instanceof Uid)
             return false;
         return true;
     }
 
     /**
      * {@inheritDoc}
      */
     public Uid create(ObjectClass objectClass, final Set<Attribute> originalAttrs, final OperationOptions options) {
         if (!objectClass.is(ObjectClass.ACCOUNT_NAME))
             throw new IllegalArgumentException(_configuration.getMessage(VmsMessages.UNSUPPORTED_OBJECT_CLASS, objectClass.getObjectClassValue()));
 
         Map<String, Attribute> attrMap = new HashMap<String, Attribute>(AttributeUtil.toMap(originalAttrs));
 
         // Extract action attributes
         //
         String createDirCommand = getCreateDirCommand(attrMap);
         String copyLoginCommand = getCopyLoginCommand(attrMap);
         Attribute createDirectory = attrMap.remove(ATTR_CREATE_DIRECTORY);
         Attribute copyLoginScript = attrMap.remove(ATTR_COPY_LOGIN_SCRIPT);
         Attribute loginScriptSource = attrMap.remove(ATTR_LOGIN_SCRIPT_SOURCE);
 
         Name name = (Name)attrMap.get(Name.NAME);
 
         String accountId = name.getNameValue();
         _log.info("create(''{0}'')", accountId);
         List<CharArrayBuffer> addCommand = appendAttributes(false, accountId, attrMap);
 
         Map<String, Object> variables = new HashMap<String, Object>();
         fillInCommand(addCommand, variables);
 
         String result = "";
         try {
             result = (String)_authorizeCommandExecutor.execute(variables);
             clearArrays(variables);
         } catch (Exception e) {
             clearArrays(variables);
             _log.error(e, "error in create");
             throw new ConnectorException(_configuration.getMessage(VmsMessages.ERROR_IN_CREATE), e);
         }
 
         if (isPresent(result, USER_ADDED)) {
             _log.info("user created");
         } else if (isPresent(result, USER_EXISTS)) {
             throw new AlreadyExistsException();
         } else {
             // If we can locate an error message, we use it
             //
             Matcher matcher = _errorPattern.matcher(result);
             if (matcher.find())
                 result = matcher.group();
             throw new ConnectorException(_configuration.getMessage(VmsMessages.ERROR_IN_CREATE2, result));
         }
 
         // Process action attributes
         //
         if (createDirCommand!=null) {
             result = executeCommand(_connection, createDirCommand);
             Matcher matcher = _errorPattern.matcher(result);
             if (matcher.find()) {
                 result = matcher.group();
                 throw new ConnectorException(_configuration.getMessage(VmsMessages.ERROR_IN_CREATE_DIRECTORY, result));
             }
         }
         if (copyLoginCommand!=null) {
             result = executeCommand(_connection, copyLoginCommand);
             Matcher matcher = _errorPattern.matcher(result);
             if (matcher.find()) {
                 result = matcher.group();
                 throw new ConnectorException(_configuration.getMessage(VmsMessages.ERROR_IN_COPY_LOGIN, result));
             }
         }
 
         return new Uid(accountId);
     }
 
     /**
      * {@inheritDoc}
      */
     public void delete(ObjectClass objectClass, Uid uid, OperationOptions options) {
         if (!objectClass.is(ObjectClass.ACCOUNT_NAME))
             throw new IllegalArgumentException(_configuration.getMessage(VmsMessages.UNSUPPORTED_OBJECT_CLASS, objectClass.getObjectClassValue()));
         _log.info("delete(''{0}'')", uid.getUidValue());
         String removeCommand = "REMOVE "+uid.getUidValue();
         Map<String, Object> variables = new HashMap<String, Object>();
         variables.put("SHELL_PROMPT", _configuration.getLocalHostShellPrompt());
         variables.put("SHORT_WAIT", SHORT_WAIT);
         variables.put("UAF_PROMPT", UAF_PROMPT);
         variables.put("UAF_PROMPT_CONTINUE", UAF_PROMPT_CONTINUE);
         variables.put("COMMAND", removeCommand);
         variables.put("COMMANDS", new LinkedList<CharArrayBuffer>());
         variables.put("CONNECTION", _connection);
 
         String result = "";
         try {
             result = (String)_authorizeCommandExecutor.execute(variables);
         } catch (Exception e) {
             _log.error(e, "error in delete");
             throw new ConnectorException(_configuration.getMessage(VmsMessages.ERROR_IN_DELETE), e);
         }
 
         if (isPresent(result, USER_REMOVED)) {
             _log.info("user deleted");
             return;
         } else if (isPresent(result, BAD_USER)) {
             throw new UnknownUidException();
         } else {
             throw new ConnectorException(_configuration.getMessage(VmsMessages.ERROR_IN_DELETE2, result));
         }
     }
 
     VmsConnection getConnection() {
         return _connection;
     }
 
     /**
      * {@inheritDoc}
      */
     public FilterTranslator<String> createFilterTranslator(ObjectClass oclass, OperationOptions options) {
         return new VmsFilterTranslator();
     }
 
     /**
      * {@inheritDoc}
      */
     public void executeQuery(ObjectClass objectClass, String query, ResultsHandler handler, OperationOptions options) {
         if (!objectClass.is(ObjectClass.ACCOUNT_NAME))
             throw new IllegalArgumentException(_configuration.getMessage(VmsMessages.UNSUPPORTED_OBJECT_CLASS, objectClass.getObjectClassValue()));
         try {
             if ( query == null ) {
                 query = "*";
             }
             _log.info("executeQuery(''{0}'')", query);
             List<String> filterStrings = new ArrayList<String>();
             filterStrings.add(query);
             Set<String> attributesToGet = null;
             if (options!=null && options.getAttributesToGet()!=null)
                 attributesToGet = CollectionUtil.newReadOnlySet(options.getAttributesToGet());
             filterUsers(handler, filterStrings, attributesToGet==null?null:CollectionUtil.newReadOnlySet(attributesToGet));
         } catch (Exception e) {
             e.printStackTrace();
             _log.error(e, "error in executeQuery");
             throw new ConnectorException(_configuration.getMessage(VmsMessages.ERROR_IN_SEARCH), e);
         }
     }
 
     private void filterUsers(ResultsHandler handler, List<String> filters, Set<String> searchAttributesToGet) throws Exception {
         List<String> commands = new LinkedList<String>();
         for (String filter : filters)
             commands.add("SHOW "+filter);
         Map<String, Object> variables = new HashMap<String, Object>();
         variables.put("SHELL_PROMPT", _configuration.getLocalHostShellPrompt());
         variables.put("SHORT_WAIT", SHORT_WAIT);
         variables.put("LONG_WAIT", LONG_WAIT);
         variables.put("UAF_PROMPT", UAF_PROMPT);
         variables.put("COMMANDS", commands);
         variables.put("CONNECTION", _connection);
 
         String users = (String)_listCommandExecutor.execute(variables);
 
         String[] userArray = users.split(SEPARATOR);
         List<String> attributesToGet = null;
         if (searchAttributesToGet!=null)
             attributesToGet = CollectionUtil.newList(searchAttributesToGet);
         for (int i=1; i<userArray.length; i++) {
             String user = SEPARATOR+userArray[i].replaceAll("\r\n", "\n");
             // Now, we truncate from the UAF_PROMPT, if present
             //
             int index = user.indexOf(UAF_PROMPT);
             if (index>-1)
                 user = user.substring(0, index);
             user += "\n";
             try {
                 @SuppressWarnings("unchecked")
                 Map<String, Object> attributes = (Map<String, Object>)TRANSFORM.transform(user);
                 ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
                 builder.setUid((String)attributes.get(Name.NAME));
 
                 String lastLogin = (String)attributes.remove(PredefinedAttributes.LAST_LOGIN_DATE_NAME);
                 String cputime = (String)attributes.remove(ATTR_CPUTIME);
                 String lifetime = (String)attributes.remove(PredefinedAttributes.PASSWORD_CHANGE_INTERVAL_NAME);
 
                 // ENABLE is handled by checking to see if the DisUser Flag is
                 // present
                 //
                 @SuppressWarnings("unchecked")
                 List<String> flags = (List<String>)attributes.get(ATTR_FLAGS);
                 if (flags.size()>0) {
                     if (includeInAttributes(OperationalAttributes.ENABLE_NAME, attributesToGet)) {
                         if (flags.contains(FLAG_DISUSER))
                             builder.addAttribute(OperationalAttributes.ENABLE_NAME, Boolean.FALSE);
                         else
                             builder.addAttribute(OperationalAttributes.ENABLE_NAME, Boolean.TRUE);
                     }
                 } else {
                     if (includeInAttributes(OperationalAttributes.ENABLE_NAME, attributesToGet))
                         builder.addAttribute(OperationalAttributes.ENABLE_NAME, Boolean.TRUE);
                 }
 
                 // PASSWORD_EXPIRED is handled by seeing the last password change plus
                 // the password lifetime is before the current time.
                 // If the password is pre-expired, we always set this to TRUE 
                 //
                 if (includeInAttributes(OperationalAttributes.PASSWORD_EXPIRED_NAME, attributesToGet)) {
                     String lastChange = (String)attributes.get(PredefinedAttributes.LAST_PASSWORD_CHANGE_DATE_NAME);
                     if (lastChange.contains("(pre-expired)")) {
                         builder.addAttribute(OperationalAttributes.PASSWORD_EXPIRED_NAME, Boolean.TRUE);
                     } else {
                         Date expiredDate = getPasswordExpirationDate(lifetime, attributes);
                         Date vmsDate = getVmsDate();
                         builder.addAttribute(OperationalAttributes.PASSWORD_EXPIRED_NAME, expiredDate.before(vmsDate));
                     }
                 }
 
                 // ATTR_DEVICE is parsed out from ATTR_DEFAULT
                 //
                 if (includeInAttributes(ATTR_DEVICE, attributesToGet)) {
                     String defaultString = (String)attributes.get(ATTR_DEFAULT);
                     if (StringUtil.isNotBlank(defaultString)) {
                         String[] defaultInfo = defaultString.split(":");
                         if (defaultInfo.length>1)
                             builder.addAttribute(ATTR_DEVICE, defaultInfo[0]);
                         else
                             builder.addAttribute(ATTR_DEVICE);
                     } else {
                         builder.addAttribute(ATTR_DEVICE);
                     }
                 }
 
                 // ATTR_DIRECTORY is parsed out from ATTR_DEFAULT
                 //
                 if (includeInAttributes(ATTR_DIRECTORY, attributesToGet)) {
                     String defaultString = (String)attributes.get(ATTR_DEFAULT);
                     if (StringUtil.isNotBlank(defaultString)) {
                         String[] defaultInfo = defaultString.split(":");
                         if (defaultInfo.length>1)
                             builder.addAttribute(ATTR_DIRECTORY, defaultInfo[1]);
                         else
                             builder.addAttribute(ATTR_DIRECTORY, defaultInfo[0]);
                     } else {
                         builder.addAttribute(ATTR_DIRECTORY);
                     }
                 }
 
                 // LAST_LOGIN 
                 //
                 if (includeInAttributes(PredefinedAttributes.LAST_LOGIN_DATE_NAME, attributesToGet)) {
                     if (StringUtil.isNotBlank(lastLogin)) {
                         // Split it out into two separate dates
                         Pattern pattern = Pattern.compile("(.*)\\(interactive\\),(.*)\\(non-interactive\\)");
                         Matcher matcher = pattern.matcher(lastLogin);
                         Object value = null;
                         if (matcher.matches()) {
                             String interactive = matcher.group(1).trim();
                             String noninteractive = matcher.group(2).trim();
                             if (interactive.equals("(none)")) {
                                 if (noninteractive.equals("(none)")) {
                                     // no date, so keep null
                                 } else {
                                     Date date = _vmsDateFormatWithoutSecs.parse(noninteractive.trim());
                                     value = date.getTime();
                                 }
                             } else {
                                 if (noninteractive.equals("(none)")) {
                                     Date date = _vmsDateFormatWithoutSecs.parse(interactive.trim());
                                     value = date.getTime();
                                 } else {
                                     Date interactiveDate = _vmsDateFormatWithoutSecs.parse(interactive.trim());
                                     Date noninteractiveDate = _vmsDateFormatWithoutSecs.parse(noninteractive.trim());
                                     if (interactiveDate.after(noninteractiveDate)) {
                                         value = interactiveDate.getTime();
                                     } else {
                                         value = noninteractiveDate.getTime();
                                     }
                                 }
                             }
                             if (value!=null) {
                                 List<Object> values = new LinkedList<Object>();
                                 values.add(value);
                                 builder.addAttribute(PredefinedAttributes.LAST_LOGIN_DATE_NAME, values);
                             } else {
                                 builder.addAttribute(PredefinedAttributes.LAST_LOGIN_DATE_NAME);
                             }
                         }
 
                     }
                 }
 
                 // ATTR_EXPIRATION 
                 //
                 if (includeInAttributes(ATTR_EXPIRATION, attributesToGet)) {
                     String expiration = (String)attributes.remove(ATTR_EXPIRATION);
                     if (StringUtil.isNotBlank(expiration)) {
                         if (expiration.trim().equals("(none)")) {
                             builder.addAttribute(ATTR_EXPIRATION);
                         } else {
                             Date date = _vmsDateFormatWithoutSecs.parse(expiration.trim());
                             List<Object> value = new LinkedList<Object>();
                             value.add(date.getTime());
                             builder.addAttribute(ATTR_EXPIRATION, value);
                         }
                     } else {
                         builder.addAttribute(ATTR_EXPIRATION);
                     }
                 }
 
                 // CPU 
                 //
                 if (includeInAttributes(ATTR_CPUTIME, attributesToGet)) {
                     if (StringUtil.isNotBlank(cputime)) {
                         long cputimeLong = remapFromDelta(cputime);
                         builder.addAttribute(ATTR_CPUTIME, cputimeLong);
                     } else {
                         builder.addAttribute(ATTR_CPUTIME);
                     }
                 }
 
                 // PASSWORD_CHANGE_INTERVAL_NAME 
                 //
                 if (includeInAttributes(PredefinedAttributes.PASSWORD_CHANGE_INTERVAL_NAME, attributesToGet)) {
                     if (StringUtil.isNotBlank(lifetime)) {
                         long lifetimeLong = remapFromDelta(lifetime);
                         builder.addAttribute(PredefinedAttributes.PASSWORD_CHANGE_INTERVAL_NAME, lifetimeLong);
                     } else {
                         builder.addAttribute(PredefinedAttributes.PASSWORD_CHANGE_INTERVAL_NAME);
                     }
                 }
 
                 // PASSWORD_EXPIRATION_DATE is handled by seeing if there is an expiration
                 // date, and if so, converting it to milliseconds
                 //
                 //TODO: I believe we can't always compute this, since the case where the user has
                 //      never logged in, but there is a password lifetime is not computable.
                 //
                 if (includeInAttributes(OperationalAttributes.PASSWORD_EXPIRATION_DATE_NAME, attributesToGet)) {
                     String change = (String)attributes.get(PredefinedAttributes.LAST_PASSWORD_CHANGE_DATE_NAME);
                     if (StringUtil.isNotBlank(lifetime)) {
                         if (lifetime.equalsIgnoreCase("(none)")) {
                             builder.addAttribute(OperationalAttributes.PASSWORD_EXPIRATION_DATE_NAME);
                         } else if (change.contains("(pre-expired)")) {
                             builder.addAttribute(OperationalAttributes.PASSWORD_EXPIRATION_DATE_NAME, 0l);
                         } else {
                             Date expiredDate = getPasswordExpirationDate(lifetime, attributes); 
                             builder.addAttribute(OperationalAttributes.PASSWORD_EXPIRATION_DATE_NAME, expiredDate.getTime());
                         }
                     } else {
                         builder.addAttribute(OperationalAttributes.PASSWORD_EXPIRATION_DATE_NAME);
                     }
                 }
 
                 // All other attributes are handled normally
                 //
                 for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                     Object value = entry.getValue();
                     String key = entry.getKey();
                     if (includeInAttributes(key, attributesToGet)) {
                         if (value instanceof Collection) {
                             builder.addAttribute(key, (Collection<?>)value);
                         } else if (value!=null && StringUtil.isNotEmpty(value.toString())) {
                             builder.addAttribute(key, value);
                         } else {
                             builder.addAttribute(key);
                         }
                     }
                 }
                 ConnectorObject next = builder.build();
                 handler.handle(next);
             } catch (Exception e) {
                 _log.error(e, "error parsing users");
                 throw new ConnectorException(e);
             }
         }
     }
 
     private Date getVmsDate() {
         Map<String, Object> variables = new HashMap<String, Object>();
         variables.put("CONNECTION", _connection);
         variables.put("SHELL_PROMPT", _configuration.getLocalHostShellPrompt());
         variables.put("SHORT_WAIT", SHORT_WAIT);
 
         String result = "";
         try {
             result = (String)_dateCommandExecutor.execute(variables);
             result = result.replaceAll(_configuration.getLocalHostShellPrompt(), "").trim();
             Date date = _vmsDateFormatWithSecs.parse(result);
             return date;
         } catch (Exception e) {
             _log.error(e, "error in getVmsDate");
             throw new ConnectorException(_configuration.getMessage(VmsMessages.ERROR_IN_GETDATE), e);
         }
 
     }
 
     private Date getPasswordExpirationDate(String lifetime, Map<String, Object> attributes) throws ParseException {
         String lastChange = (String)attributes.get(PredefinedAttributes.LAST_PASSWORD_CHANGE_DATE_NAME);
         // If the password is pre-expired, we say it expired at time 0
         //
         if (lastChange.contains("(pre-expired)"))
             return new Date(0);
         
         // If there is no lifetime, there is no expiration date
         //
         if (lifetime==null)
             return null;
         
         // Otherwise, password expires 'lifetime' after 'lastChangeDate'
         //
         long lastChangeDate = _vmsDateFormatWithoutSecs.parse(lastChange).getTime();
         long lifetimeLong = remapFromDelta(lifetime);
         Date expiredDate = new Date(lastChangeDate+lifetimeLong);
         return expiredDate;
     }
 
     private boolean includeInAttributes(String attribute, List<String> attributesToGet) {
         if (attribute.equalsIgnoreCase(Name.NAME))
             return true;
         if (attributesToGet!=null) {
             return attributesToGet.contains(attribute);
         } else {
             schema();
             if (_attributeMap.containsKey(attribute))
                 return _attributeMap.get(attribute).isReturnedByDefault();
         }
         return false;
     }
 
     public Uid update(ObjectClass obj, Uid uid, Set<Attribute> attrs, OperationOptions options) {
         return update(obj, AttributeUtil.addUid(attrs, uid), options);
     }
 
     /**
      * {@inheritDoc}
      */
     Uid update(ObjectClass objectClass, Set<Attribute> attributes, OperationOptions options) {
         if (!objectClass.is(ObjectClass.ACCOUNT_NAME))
             throw new IllegalArgumentException(_configuration.getMessage(VmsMessages.UNSUPPORTED_OBJECT_CLASS, objectClass.getObjectClassValue()));
         Map<String, Attribute> attrMap = new HashMap<String, Attribute>(AttributeUtil.toMap(attributes));
 
         // Create-only attributes may not be specified
         //
         Attribute createDirectory = attrMap.remove(ATTR_CREATE_DIRECTORY);
         Attribute copyLogin = attrMap.remove(ATTR_COPY_LOGIN_SCRIPT);
         Attribute loginScriptSource = attrMap.remove(ATTR_LOGIN_SCRIPT_SOURCE);
 
         if (isAttributeTrue(createDirectory))
             throw new IllegalArgumentException(_configuration.getMessage(VmsMessages.UPDATE_ATTRIBUTE_VALUE, createDirectory.getName()));
         if (isAttributeTrue(copyLogin))
             throw new IllegalArgumentException(_configuration.getMessage(VmsMessages.UPDATE_ATTRIBUTE_VALUE, copyLogin.getName()));
 
         // Operational Attributes are handled specially
         //
         Uid uid = (Uid)attrMap.remove(Uid.NAME);
         Name name = (Name)attrMap.remove(Name.NAME);
         Attribute currentPassword = attrMap.remove(OperationalAttributes.CURRENT_PASSWORD_NAME);
         Attribute newPassword = attrMap.remove(OperationalAttributes.PASSWORD_NAME);
 
         // If name is different from Uid, we are performing a RENAME operation.
         // Do this first, followed by the MODIFY
         //
         if (name!=null && uid!=null && !uid.getUidValue().equals(name.getNameValue())) {
             CharArrayBuffer renameCommand = new CharArrayBuffer();
             renameCommand.append("RENAME "+uid.getUidValue()+" "+name.getNameValue());
 
             Map<String, Object> variables = new HashMap<String, Object>();
             variables.put("SHELL_PROMPT", _configuration.getLocalHostShellPrompt());
             variables.put("SHORT_WAIT", SHORT_WAIT);
             variables.put("UAF_PROMPT", UAF_PROMPT);
             variables.put("UAF_PROMPT_CONTINUE", UAF_PROMPT_CONTINUE);
             char[] commandContents = renameCommand.getArray();
             renameCommand.clear();
             variables.put("COMMAND", commandContents);
             variables.put("COMMANDS", new LinkedList<CharArrayBuffer>());
             variables.put("CONNECTION", _connection);
 
             String result = "";
             try {
                 result = (String)_authorizeCommandExecutor.execute(variables);
                 Arrays.fill(commandContents, 0, commandContents.length, ' ');
             } catch (Exception e) {
                 Arrays.fill(commandContents, 0, commandContents.length, ' ');
                 _log.error(e, "error in rename");
                 throw new ConnectorException(_configuration.getMessage(VmsMessages.ERROR_IN_MODIFY), e);
             }
 
             if (isPresent(result, USER_RENAMED)) {
                 uid = new Uid(name.getNameValue());
             } else if (isPresent(result, BAD_SPEC)) {
                 throw new UnknownUidException();
             } else {
                 throw new ConnectorException(_configuration.getMessage(VmsMessages.ERROR_IN_MODIFY2, result));
             }
         }
 
         // If Password and CurrentPassword
         // are specified, we change password via SET PASSWORD, rather than AUTHORIIZE
         //
         if (currentPassword!=null && newPassword!=null) {
             _log.info("update[changePassword](''{0}'')", uid.getUidValue());
             Map<String, Object> variables = new HashMap<String, Object>();
             variables.put("SHELL_PROMPT", _configuration.getLocalHostShellPrompt());
             variables.put("SHORT_WAIT", SHORT_WAIT);
             variables.put("USERNAME", uid.getUidValue());
             GuardedString currentGS = AttributeUtil.getGuardedStringValue(currentPassword);
             GuardedString newGS = AttributeUtil.getGuardedStringValue(newPassword);
             GuardedStringAccessor accessor = new GuardedStringAccessor();
             currentGS.access(accessor);
             char[] currentArray = accessor.getArray();
             newGS.access(accessor);
             char[] newArray = accessor.getArray();
             variables.put("CURRENT_PASSWORD", new String(currentArray));
             variables.put("NEW_PASSWORD", new String(newArray));
             variables.put("CONFIGURATION", _configuration);
 
             String result = "";
             try {
                 result = (String)_changeOwnPasswordCommandExecutor.execute(variables);
                 Arrays.fill(currentArray, 0, currentArray.length, ' ');
                 Arrays.fill(newArray, 0, newArray.length, ' ');
             } catch (Exception e) {
                 Arrays.fill(currentArray, 0, currentArray.length, ' ');
                 Arrays.fill(newArray, 0, newArray.length, ' ');
                 _log.error(e, "error in create");
                 throw new ConnectorException(_configuration.getMessage(VmsMessages.ERROR_IN_MODIFY), e);
             }
 
             if (result.indexOf("%SET-")>-1) {
                 String errortext = result.substring(result.indexOf("%SET-")).split("\n")[0];
                 throw new ConnectorException(_configuration.getMessage(VmsMessages.ERROR_IN_MODIFY2, errortext));
             }
         } else if (newPassword!=null) {
             // Put back the new password, so it can be changed administratively
             //
             attrMap.put(OperationalAttributes.PASSWORD_NAME, newPassword);
         }
 
         // If we have any remaining attributes, process them
         //
         if (attrMap.size()>0) {
             String accountId = uid.getUidValue();
             List<CharArrayBuffer> modifyCommand = appendAttributes(true, accountId, attrMap);
 
             Map<String, Object> variables = new HashMap<String, Object>();
             fillInCommand(modifyCommand, variables);
 
             String result = "";
             try {
                 result = (String)_authorizeCommandExecutor.execute(variables);
                 clearArrays(variables);
             } catch (Exception e) {
                 clearArrays(variables);
                 _log.error(e, "error in create");
                 throw new ConnectorException(_configuration.getMessage(VmsMessages.ERROR_IN_MODIFY), e);
             }
 
             if (isPresent(result, USER_UPDATED)) {
                 return uid;
             } else if (isPresent(result, BAD_SPEC)) {
                 throw new UnknownUidException();
             } else {
                 throw new ConnectorException(_configuration.getMessage(VmsMessages.ERROR_IN_MODIFY2, result));
             }
         }
         return uid;
     }
 
     private void fillInCommand(List<CharArrayBuffer> command, Map<String, Object> variables) {
         List<CharArrayBuffer> localCommand = new LinkedList<CharArrayBuffer>(command);
         CharArrayBuffer lastPart = localCommand.remove(command.size()-1);
         char[] commandContents = lastPart.getArray();
         lastPart.clear();
         variables.put("COMMAND", commandContents);
         List<char[]> firstContents = new LinkedList<char[]>();
         for (CharArrayBuffer part : localCommand) {
             commandContents = part.getArray();
             part.clear();
             firstContents.add(commandContents);
         }
         variables.put("COMMANDS", firstContents);
         variables.put("SHELL_PROMPT", _configuration.getLocalHostShellPrompt());
         variables.put("SHORT_WAIT", SHORT_WAIT);
         variables.put("UAF_PROMPT", UAF_PROMPT);
         variables.put("UAF_PROMPT_CONTINUE", UAF_PROMPT_CONTINUE);
         variables.put("CONNECTION", _connection);
     }
 
     private void clearArrays(Map<String, Object> variables) {
         char[] commandContents = (char[])variables.get("COMMAND");
         Arrays.fill(commandContents, 0, commandContents.length, ' ');
         @SuppressWarnings("unchecked")
         List<char[]> commandPrefixContents = (List<char[]>)variables.get("COMMANDS");
         if (commandPrefixContents!=null) {
             for (char[] commandPrefix : commandPrefixContents)
                 Arrays.fill(commandPrefix, 0, commandPrefix.length, ' ');
         }
     }
 
     /**
      * {@inheritDoc}
      */
     public Schema schema() {
         if (_schema!=null)
             return _schema;
 
         final SchemaBuilder schemaBuilder = new SchemaBuilder(getClass());
         Set<AttributeInfo> attributes = new HashSet<AttributeInfo>();
 
         attributes.add(buildRequiredAttribute(Name.NAME,            String.class));
 
         // Required Attributes
         //
         attributes.add(buildRequiredAttribute(ATTR_UIC,             String.class));
 
         // Optional Attributes (have VMS default values)
         //
         attributes.add(AttributeInfoBuilder.build(ATTR_DEVICE,      String.class));
         attributes.add(AttributeInfoBuilder.build(ATTR_DIRECTORY,   String.class));
         attributes.add(AttributeInfoBuilder.build(ATTR_OWNER,       String.class));
         attributes.add(AttributeInfoBuilder.build(ATTR_ACCOUNT,     String.class));
         attributes.add(AttributeInfoBuilder.build(ATTR_CLI,         String.class));
         attributes.add(AttributeInfoBuilder.build(ATTR_CLITABLES,   String.class));
         attributes.add(AttributeInfoBuilder.build(ATTR_LGICMD,      String.class));
         attributes.add(AttributeInfoBuilder.build(ATTR_PWDMINIMUM,  Integer.class));
         attributes.add(AttributeInfoBuilder.build(ATTR_MAXJOBS,     Integer.class));
         attributes.add(AttributeInfoBuilder.build(ATTR_MAXACCTJOBS, Integer.class));
         attributes.add(AttributeInfoBuilder.build(ATTR_SHRFILLM,    Integer.class));
         attributes.add(AttributeInfoBuilder.build(ATTR_PBYTLM,      Integer.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_MAXDETACH,   Integer.class));
         attributes.add(AttributeInfoBuilder.build(ATTR_BIOLM,       Integer.class));
         attributes.add(AttributeInfoBuilder.build(ATTR_JTQUOTA,     Integer.class));
         attributes.add(AttributeInfoBuilder.build(ATTR_DIOLM,       Integer.class));
         attributes.add(AttributeInfoBuilder.build(ATTR_WSDEFAULT,   Integer.class));
         attributes.add(AttributeInfoBuilder.build(ATTR_PRIORITY,    Integer.class));
         attributes.add(AttributeInfoBuilder.build(ATTR_WSQUOTA,     Integer.class));
         attributes.add(AttributeInfoBuilder.build(ATTR_QUEPRIO,     Integer.class));
         attributes.add(AttributeInfoBuilder.build(ATTR_WSEXTENT,    Integer.class));
         attributes.add(AttributeInfoBuilder.build(ATTR_CPUTIME,     Long.class));
         attributes.add(AttributeInfoBuilder.build(ATTR_ENQLM,       Integer.class));
         attributes.add(AttributeInfoBuilder.build(ATTR_PGFLQUOTA,   Integer.class));
         attributes.add(AttributeInfoBuilder.build(ATTR_TQELM,       Integer.class));
         attributes.add(AttributeInfoBuilder.build(ATTR_ASTLM,       Integer.class));
         attributes.add(AttributeInfoBuilder.build(ATTR_BYTLM,       Integer.class));
         attributes.add(AttributeInfoBuilder.build(ATTR_FILLM,       Integer.class));
         attributes.add(AttributeInfoBuilder.build(ATTR_PRCLM,       Integer.class));
 
         // Multi-valued attributes
         //
         attributes.add(buildMultivaluedAttribute(ATTR_FLAGS,        String.class, false));
         attributes.add(buildMultivaluedAttribute(ATTR_PRIMEDAYS,    String.class, false));
         attributes.add(buildMultivaluedAttribute(ATTR_PRIVILEGES,   String.class, false));
         attributes.add(buildMultivaluedAttribute(ATTR_DEFPRIVILEGES,String.class, false));
 
         // Write-only attributes
         //
         //attributes.add(buildWriteonlyAttribute(ATTR_ALGORITHM,      String.class, false, false));
         attributes.add(buildCreateonlyAttribute(ATTR_LOGIN_SCRIPT_SOURCE, String.class, false));
         attributes.add(buildCreateonlyAttribute(ATTR_COPY_LOGIN_SCRIPT, Boolean.class, false));
         attributes.add(buildCreateonlyAttribute(ATTR_CREATE_DIRECTORY, Boolean.class, false));
 
         // Read-only attributes
         //
         attributes.add(buildReadonlyAttribute(ATTR_LOGIN_FAILS, Integer.class, false));
         attributes.add(buildReadonlyAttribute(ATTR_EXPIRATION,  Long.class, false));
 
         // Operational Attributes
         //
         attributes.add(OperationalAttributeInfos.PASSWORD);
         boolean disableUserLogins = isDisableUserLogins(_configuration);
         if (!disableUserLogins)
             attributes.add(OperationalAttributeInfos.CURRENT_PASSWORD);
         attributes.add(OperationalAttributeInfos.ENABLE);
         attributes.add(OperationalAttributeInfos.PASSWORD_EXPIRED);
         //TODO: I believe we can't always compute this, since the case where the user has
         //      never logged in, but there is a password lifetime is not computable.
         //
         //attributes.add(buildReadonlyAttribute(OperationalAttributes.PASSWORD_EXPIRATION_DATE_NAME, Long.class, false));
 
         // Predefined Attributes
         //
         attributes.add(PredefinedAttributeInfos.LAST_LOGIN_DATE);
         attributes.add(PredefinedAttributeInfos.LAST_PASSWORD_CHANGE_DATE);
         attributes.add(PredefinedAttributeInfos.PASSWORD_CHANGE_INTERVAL);
 
         ObjectClassInfoBuilder ociBuilder = new ObjectClassInfoBuilder();
         ociBuilder.setType(ObjectClass.ACCOUNT_NAME);
         ociBuilder.addAllAttributeInfo(attributes);
         ObjectClassInfo objectClassInfo = ociBuilder.build();
         schemaBuilder.defineObjectClass(objectClassInfo);
 
         // Remove unsupported operations
         //
         if (disableUserLogins) {
             schemaBuilder.removeSupportedObjectClass(AuthenticateOp.class, objectClassInfo);
         }
 
         //TODO: can use VMS expiration attribute to do OperationalAttributes.DISABLE_DATE_NAME
 
         _schema = schemaBuilder.build();
         _attributeMap = AttributeInfoUtil.toMap(attributes);
         return _schema;
     }
 
     private AttributeInfo buildMultivaluedAttribute(String name, Class<?> clazz, boolean required) {
         AttributeInfoBuilder builder = new AttributeInfoBuilder();
         builder.setName(name);
         builder.setType(clazz);
         builder.setRequired(required);
         builder.setMultiValued(true);
         return builder.build();
     }
 
     private AttributeInfo buildRequiredAttribute(String name, Class<?> clazz) {
         AttributeInfoBuilder builder = new AttributeInfoBuilder();
         builder.setName(name);
         builder.setType(clazz);
         builder.setRequired(true);
         builder.setMultiValued(false);
         builder.setUpdateable(true);
         builder.setCreateable(true);
         return builder.build();
     }
 
     private AttributeInfo buildReadonlyAttribute(String name, Class<?> clazz, boolean required) {
         AttributeInfoBuilder builder = new AttributeInfoBuilder();
         builder.setName(name);
         builder.setType(clazz);
         builder.setRequired(required);
         builder.setMultiValued(false);
         builder.setCreateable(false);
         builder.setUpdateable(false);
         return builder.build();
     }
 
     private AttributeInfo buildCreateonlyAttribute(String name, Class<?> clazz, boolean required) {
         AttributeInfoBuilder builder = new AttributeInfoBuilder();
         builder.setName(name);
         builder.setType(clazz);
         builder.setRequired(required);
         builder.setMultiValued(false);
         builder.setUpdateable(false);
         builder.setCreateable(true);
         builder.setReadable(false);
         builder.setReturnedByDefault(false);
         return builder.build();
     }
 
     /**
      * {@inheritDoc}
      */
     public void dispose() {
         if (_connection != null) {
             _connection.dispose();
             _connection = null;
         }
     }
 
     /**
      * {@inheritDoc}
      */
     public void checkAlive() {
         _connection.test();
     }
 
     /**
      * {@inheritDoc}
      */
     public Configuration getConfiguration() {
         return _configuration;
     }
 
     /**
      * {@inheritDoc}
      */
     public void init(Configuration cfg) {
         _configuration = (VmsConfiguration)cfg;
         _vmsDateFormatWithSecs = new SimpleDateFormat(_configuration.getVmsDateFormatWithSecs(), new Locale(_configuration.getVmsLocale()));
         TimeZone timeZone = TimeZone.getTimeZone(_configuration.getVmsTimeZone());
         _vmsDateFormatWithSecs.setTimeZone(timeZone);
         _vmsDateFormatWithoutSecs = new SimpleDateFormat(_configuration.getVmsDateFormatWithoutSecs(), new Locale(_configuration.getVmsLocale()));
         _vmsDateFormatWithoutSecs.setTimeZone(timeZone);
 
         // Internal scripts are all in GROOVY for now
         //
         ScriptExecutorFactory scriptFactory = ScriptExecutorFactory.newInstance("GROOVY");
         _authorizeCommandExecutor = scriptFactory.newScriptExecutor(getClass().getClassLoader(), _authorizeCommandScript, true);
         _changeOwnPasswordCommandExecutor = scriptFactory.newScriptExecutor(getClass().getClassLoader(), _changeOwnPasswordCommandScript, true);
         _listCommandExecutor = scriptFactory.newScriptExecutor(getClass().getClassLoader(), _listCommandScript, true);
         _dateCommandExecutor = scriptFactory.newScriptExecutor(getClass().getClassLoader(), _dateCommandScript, true);
         try {
             _connection = new VmsConnection(_configuration, VmsConnector.SHORT_WAIT);
         } catch (Exception e) {
             throw ConnectorException.wrap(e);
         }
     }
 
     public Object runScriptOnResource(ScriptContext request, OperationOptions options) {
         String user = options.getRunAsUser();
         GuardedString password = options.getRunWithPassword();
         if (user!=null && password==null)
             throw new ConnectorException(_configuration.getMessage(VmsMessages.PASSWORD_REQUIRED_FOR_RUN_AS));
         if (user!=null && isDisableUserLogins(_configuration))
             throw new ConnectorException(_configuration.getMessage(VmsMessages.RUN_AS_WHEN_DISABLED));
 
         Map<String, Object> arguments = request.getScriptArguments();
         String language = request.getScriptLanguage();
         if (!"DCL".equalsIgnoreCase(language)) {
             throw new ConnectorException(_configuration.getMessage(VmsMessages.UNSUPPORTED_SCRIPTING_LANGUAGE, language));
         }
 
         String script = request.getScriptText();
         try {
             if (user!=null) {
                 VmsConfiguration configuration = new VmsConfiguration(_configuration);
                 configuration.setUserName(user);
                 configuration.setPassword(password);
                 VmsConnection connection = new VmsConnection(configuration,VmsConnector.SHORT_WAIT);
                 Object result = executeScript(connection, script, SHORT_WAIT, arguments);
                 connection.dispose();
                 return result;
             } else {
                 return executeScript(_connection, script, SHORT_WAIT, arguments);
             }
         } catch (Exception e) {
             throw ConnectorException.wrap(e);
         }
     }
 
     private boolean isDisableUserLogins(VmsConfiguration configuration) {
         boolean disableUserLogins = false;
         if (configuration.getDisableUserLogins()!=null)
             disableUserLogins = configuration.getDisableUserLogins();
         return disableUserLogins;
     }
 
     private boolean isSSH(VmsConfiguration configuration) {
         boolean isSSH = false;
         if (configuration.getSSH()!=null)
             isSSH = configuration.getSSH();
         return isSSH;
     }
 
     private boolean isAttributeTrue(Attribute attribute) {
         if (attribute==null)
             return false;
         return AttributeUtil.getBooleanValue(attribute);
     }
 
     protected String executeCommand(VmsConnection connection, String command) {
         connection.resetStandardOutput();
         try {
             connection.send(command);
             connection.waitFor(_configuration.getLocalHostShellPrompt(), SHORT_WAIT);
         } catch (Exception e) {
             throw ConnectorException.wrap(e);
         }
         String output = connection.getStandardOutput();
         int index = output.lastIndexOf(_configuration.getLocalHostShellPrompt());
         if (index!=-1)
             output = output.substring(0, index);
         String terminator = null;
         if (isSSH(_configuration))
             terminator = "\r\n";
         else
             terminator = "\n\r";
         // Strip trailing NULs (seen with SSH)
         //
         while (output.endsWith("\0"))
             output = output.substring(0, output.length()-1);
         // trim off starting or ending \n
         //
         if (output.startsWith(terminator))
             output = output.substring(terminator.length());
         if (output.endsWith(terminator))
             output = output.substring(0, output.length()-terminator.length());
         return output;
     }
 
     protected String[] executeScript(VmsConnection connection, String action, int timeout, Map<String, Object> args) {
         // create a temp file
         //
         String tmpfile = UUID.randomUUID().toString();
         
         //TODO: allow optional used of F$UNIQUE
 
         // Create an empty error file, so that we can send it back if there are
         // no errors
         //
         executeCommand(connection, "SET DEFAULT SYS$LOGIN");
         executeCommand(connection, "OPEN/WRITE OUTPUT_FILE " + tmpfile + ".ERROR");
         executeCommand(connection, "CLOSE OUTPUT_FILE");
 
         // create script and append actions
         //
         executeCommand(connection, "OPEN/WRITE OUTPUT_FILE " + tmpfile + ".COM");
         executeCommand(connection, "WRITE OUTPUT_FILE \"$ DEFINE SYS$ERROR " + tmpfile + ".ERROR");
         executeCommand(connection, "WRITE OUTPUT_FILE \"$ DEFINE SYS$OUTPUT " + tmpfile + ".OUTPUT");
 
         setEnvironmentVariables(connection, args);
 
         StringTokenizer st = new StringTokenizer(action, "\r\n\f");
         if (st.hasMoreTokens()) {
             do {
                 String token = st.nextToken();
                 if (!StringUtil.isBlank(token)) {
                     executeCommand(connection, "WRITE OUTPUT_FILE " + new String(quoteWhenNeeded(token.toCharArray(), true)));
                 }
             } while (st.hasMoreTokens());
         }
 
         executeCommand(connection, "CLOSE OUTPUT_FILE");
 
         executeCommand(connection, "@" + tmpfile);
 
         executeCommand(connection, "SET DEFAULT SYS$LOGIN");
         executeCommand(connection, "DEAS SYS$ERROR");
         executeCommand(connection, "DEAS SYS$OUTPUT");
 
         // Capture the output
         //
         String status = executeCommand(connection, "WRITE SYS$OUTPUT $STATUS");
         String output = executeCommand(connection, "TYPE " + tmpfile + ".OUTPUT");
         String error  = executeCommand(connection, "TYPE " + tmpfile + ".ERROR");
         executeCommand(connection, "DELETE " + tmpfile + ".OUTPUT;");
         executeCommand(connection, "DELETE " + tmpfile + ".ERROR;*");
         executeCommand(connection, "DELETE " + tmpfile + ".COM;");
 
         return new String[] { status, output, error };
     }
 
     private void setEnvironmentVariables(VmsConnection connection, Map<String, Object> args) {
         Set<Map.Entry<String, Object>> keyset = args.entrySet();
         for (Map.Entry<String, Object> entry : keyset) {
             String name = entry.getKey();
             Object value = entry.getValue();
             String dclAssignment = "$" + name + "=" + new String(quoteWhenNeeded(value.toString().toCharArray(), true));
             String line = "WRITE OUTPUT_FILE " + new String(quoteWhenNeeded(dclAssignment.toCharArray(), true));
             if (line.length() < 255) {
                 executeCommand(connection, line);
             }
         }
     }
 
     public Attribute normalizeAttribute(ObjectClass oclass, Attribute attribute) {
         // Because VMS reports NAME (and UID) in upper case, they will not match
         // against a lower case name unless the values are forced to upper case.
         //
         if (attribute instanceof Name) {
             Name name = (Name)attribute;
             return new Name(name.getNameValue().toUpperCase());
         }
         if (attribute instanceof Uid) {
             Uid uid = (Uid)attribute;
             return new Uid(uid.getUidValue().toUpperCase());
         }
         return attribute;
     }
 
     public Uid authenticate(ObjectClass objectClass, String username,
             GuardedString password, OperationOptions options) {
         VmsConfiguration configuration = new VmsConfiguration(_configuration);
         configuration.setUserName(username);
         configuration.setPassword(password);
         try {
             VmsConnection connection = new VmsConnection(configuration, VmsConnector.SHORT_WAIT);
             connection.dispose();
         } catch (Exception e) {
             throw ConnectorException.wrap(e);
         }
 
         return new Uid(username);
     }
 
     private String getCreateDirCommand(Map<String, Attribute> attrMap) {
         Attribute createDirectory = attrMap.get(ATTR_CREATE_DIRECTORY);
         if (createDirectory==null || !AttributeUtil.getBooleanValue(createDirectory))
             return null;
 
         Attribute uicAttr = attrMap.get(ATTR_UIC);
         Attribute deviceAttr = attrMap.get(ATTR_DEVICE);
         Attribute directoryAttr = attrMap.get(ATTR_DIRECTORY);
 
         if (uicAttr==null)
             throw new IllegalArgumentException(_configuration.getMessage(VmsMessages.MISSING_REQUIRED_ATTRIBUTE, ATTR_UIC));
         if (deviceAttr==null)
             throw new IllegalArgumentException(_configuration.getMessage(VmsMessages.MISSING_REQUIRED_ATTRIBUTE, ATTR_DEVICE));
         if (directoryAttr==null)
             throw new IllegalArgumentException(_configuration.getMessage(VmsMessages.MISSING_REQUIRED_ATTRIBUTE, ATTR_DIRECTORY));
 
         String uic = AttributeUtil.getStringValue(uicAttr);
         String device = AttributeUtil.getStringValue(deviceAttr);
         String directory = AttributeUtil.getStringValue(directoryAttr);
 
         if (StringUtil.isBlank(uic))
             throw new IllegalArgumentException(_configuration.getMessage(VmsMessages.MISSING_ATTRIBUTE_VALUE, ATTR_UIC));
         if (StringUtil.isBlank(device))
             throw new IllegalArgumentException(_configuration.getMessage(VmsMessages.MISSING_ATTRIBUTE_VALUE, ATTR_DEVICE));
         if (StringUtil.isBlank(directory))
             throw new IllegalArgumentException(_configuration.getMessage(VmsMessages.MISSING_ATTRIBUTE_VALUE, ATTR_DIRECTORY));
 
         StringBuffer cmd = new StringBuffer();
 
         cmd.append("CREATE/DIRECTORY/PROTECTION=SYSTEM:RWED/OWNER_UIC=");
         cmd.append(uic);
         cmd.append(" ");
         if (!device.endsWith(":"))
             device += ":";
         cmd.append(device);
         if (!directory.endsWith("]"))
             directory = "["+directory+"]";
         cmd.append(directory);
         return cmd.toString();
     }
 
     private String getCopyLoginCommand(Map<String, Attribute> attrMap) {
         Attribute copyLoginScript = attrMap.get(ATTR_COPY_LOGIN_SCRIPT);
         if (copyLoginScript==null || !AttributeUtil.getBooleanValue(copyLoginScript))
             return null;
 
         Attribute loginScriptSourceAttr = attrMap.get(ATTR_LOGIN_SCRIPT_SOURCE);
         Attribute uicAttr = attrMap.get(ATTR_UIC);
         Attribute deviceAttr = attrMap.get(ATTR_DEVICE);
         Attribute directoryAttr = attrMap.get(ATTR_DIRECTORY);
 
         if (uicAttr==null)
             throw new IllegalArgumentException(_configuration.getMessage(VmsMessages.MISSING_REQUIRED_ATTRIBUTE, ATTR_UIC));
         if (deviceAttr==null)
             throw new IllegalArgumentException(_configuration.getMessage(VmsMessages.MISSING_REQUIRED_ATTRIBUTE, ATTR_DEVICE));
         if (directoryAttr==null)
             throw new IllegalArgumentException(_configuration.getMessage(VmsMessages.MISSING_REQUIRED_ATTRIBUTE, ATTR_DIRECTORY));
         if (loginScriptSourceAttr==null)
             throw new IllegalArgumentException(_configuration.getMessage(VmsMessages.MISSING_REQUIRED_ATTRIBUTE, ATTR_LOGIN_SCRIPT_SOURCE));
 
         String uic = AttributeUtil.getStringValue(uicAttr);
         String device = AttributeUtil.getStringValue(deviceAttr);
         String directory = AttributeUtil.getStringValue(directoryAttr);
         String loginScriptSource = AttributeUtil.getStringValue(loginScriptSourceAttr);
 
         if (StringUtil.isBlank(uic))
             throw new IllegalArgumentException(_configuration.getMessage(VmsMessages.MISSING_ATTRIBUTE_VALUE, ATTR_UIC));
         if (StringUtil.isBlank(device))
             throw new IllegalArgumentException(_configuration.getMessage(VmsMessages.MISSING_ATTRIBUTE_VALUE, ATTR_DEVICE));
         if (StringUtil.isBlank(directory))
             throw new IllegalArgumentException(_configuration.getMessage(VmsMessages.MISSING_ATTRIBUTE_VALUE, ATTR_DIRECTORY));
         if (StringUtil.isBlank(loginScriptSource))
             throw new IllegalArgumentException(_configuration.getMessage(VmsMessages.MISSING_ATTRIBUTE_VALUE, ATTR_LOGIN_SCRIPT_SOURCE));
 
         StringBuffer cmd = new StringBuffer();
 
         cmd.append("COPY ");
         cmd.append(loginScriptSource);
         cmd.append(" ");
         if (!device.endsWith(":"))
             device += ":";
         cmd.append(device);
         if (!directory.endsWith("]"))
             directory = "["+directory+"]";
         cmd.append(directory);
         return cmd.toString();
     }
 
     private static class CharArrayBuffer {
         private char[]  _array;
         private int     _position;
 
         public CharArrayBuffer() {
             _array = new char[1024];
             _position = 0;
         }
 
         public void append(String string ) {
             append(string.toCharArray());
         }
 
         public void append(char character) {
             append(new char[] { character });
         }
 
         public void append(char[] string) {
             if (_position+string.length> _array.length) {
                 char[] oldArray = _array;
                 _array = new char[_array.length+1024];
                 System.arraycopy(oldArray, 0, _array, 0, _position);
                 Arrays.fill(oldArray, 0, oldArray.length, ' ');
             }
             System.arraycopy(string, 0, _array, _position, string.length);
             _position += string.length;
         }
 
         public void clear() {
             Arrays.fill(_array, 0, _array.length, ' ');
         }
 
         public char[] getArray() {
             char[] result = new char[_position];
             System.arraycopy(_array, 0, result, 0, _position);
             return result;
         }
 
         public int length() {
             return _position;
         }
     }
 
 }
 
