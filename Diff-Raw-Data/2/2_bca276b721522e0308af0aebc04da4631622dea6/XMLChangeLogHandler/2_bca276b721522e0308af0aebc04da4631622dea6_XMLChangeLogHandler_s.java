 package liquibase.parser.xml;
 
 import liquibase.ChangeSet;
 import liquibase.DatabaseChangeLog;
 import liquibase.FileOpener;
 import liquibase.change.*;
 import liquibase.change.custom.CustomChangeWrapper;
 import liquibase.exception.CustomChangeException;
 import liquibase.exception.LiquibaseException;
 import liquibase.exception.MigrationFailedException;
 import liquibase.log.LogFactory;
 import liquibase.parser.ChangeLogParser;
 import liquibase.preconditions.*;
 import liquibase.util.ObjectUtil;
 import liquibase.util.StringUtils;
 import org.xml.sax.Attributes;
 import org.xml.sax.SAXException;
 import org.xml.sax.helpers.DefaultHandler;
 
 import java.lang.reflect.InvocationTargetException;
 import java.util.*;
 import java.util.regex.Pattern;
 import java.util.regex.Matcher;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 import java.io.InputStream;
 
 class XMLChangeLogHandler extends DefaultHandler {
 
     protected Logger log;
 
     private DatabaseChangeLog databaseChangeLog;
     private Change change;
     private StringBuffer text;
     private AndPrecondition rootPrecondition;
     private Stack<PreconditionLogic> preconditionLogicStack = new Stack<PreconditionLogic>();
     private ChangeSet changeSet;
     private FileOpener fileOpener;
     private Precondition currentPrecondition;
 
     private Map<String, Object> changeLogParameters = new HashMap<String, Object>();
     private boolean inRollback = false;
 
 
     protected XMLChangeLogHandler(String physicalChangeLogLocation, FileOpener fileOpener, Map<String, Object> properties) {
         log = LogFactory.getLogger();
         this.fileOpener = fileOpener;
 
         databaseChangeLog = new DatabaseChangeLog(physicalChangeLogLocation);
         databaseChangeLog.setPhysicalFilePath(physicalChangeLogLocation);
 
         for (Map.Entry entry : System.getProperties().entrySet()) {
             changeLogParameters.put(entry.getKey().toString(), entry.getValue());
         }
 
         for (Map.Entry entry : properties.entrySet()) {
             changeLogParameters.put(entry.getKey().toString(), entry.getValue());
         }
     }
 
     public DatabaseChangeLog getDatabaseChangeLog() {
         return databaseChangeLog;
     }
 
     public void startElement(String uri, String localName, String qName, Attributes baseAttributes) throws SAXException {
         Attributes atts = new ExpandingAttributes(baseAttributes);
         try {
             if ("comment".equals(qName)) {
                 text = new StringBuffer();
             } else if ("validCheckSum".equals(qName)) {
                 text = new StringBuffer();                
             } else if ("databaseChangeLog".equals(qName)) {
                 String version = uri.substring(uri.lastIndexOf("/")+1);
                 if (!version.equals(XMLChangeLogParser.getSchemaVersion())) {
                     log.warning(databaseChangeLog.getPhysicalFilePath()+" is using schema version "+version+" rather than version "+XMLChangeLogParser.getSchemaVersion());
                 }
                 databaseChangeLog.setLogicalFilePath(atts.getValue("logicalFilePath"));
             } else if ("include".equals(qName)) {
                 String fileName = atts.getValue("file");
                 handleIncludedChangeLog(fileName);
             } else if (changeSet == null && "changeSet".equals(qName)) {
                 boolean alwaysRun = false;
                 boolean runOnChange = false;
                 if ("true".equalsIgnoreCase(atts.getValue("runAlways"))) {
                     alwaysRun = true;
                 }
                 if ("true".equalsIgnoreCase(atts.getValue("runOnChange"))) {
                     runOnChange = true;
                 }
                 changeSet = new ChangeSet(atts.getValue("id"),
                         atts.getValue("author"),
                         alwaysRun,
                         runOnChange,
                         databaseChangeLog.getFilePath(),
                         databaseChangeLog.getPhysicalFilePath(),
                         atts.getValue("context"),
                         atts.getValue("dbms"));
                 if (StringUtils.trimToNull(atts.getValue("failOnError")) != null) {
                     changeSet.setFailOnError(Boolean.parseBoolean(atts.getValue("failOnError")));
                 }
             } else if (changeSet != null && "rollback".equals(qName)) {
                 text = new StringBuffer();
                 String id = atts.getValue("changeSetId");
                 if (id != null) {
                     String path = atts.getValue("changeSetPath");
                     if (path == null) {
                         path = databaseChangeLog.getFilePath();
                     }
                     String author = atts.getValue("changeSetAuthor");
                     ChangeSet changeSet = databaseChangeLog.getChangeSet(path, author, id);
                     if (changeSet == null) {
                         throw new SAXException("Could not find changeSet to use for rollback: "+path+":"+author+":"+id);
                     } else {
                         for (Change change : changeSet.getChanges()) {
                             this.changeSet.addRollbackChange(change);
                         }
                     }
                 }
                 inRollback = true;
             } else if ("preConditions".equals(qName)) {
                 rootPrecondition = new AndPrecondition();
                rootPrecondition.setSkipOnFail(Boolean.parseBoolean(atts.getValue("skipOnFail")));
                 preconditionLogicStack.push(rootPrecondition);
             } else if (rootPrecondition != null) {
                 currentPrecondition = PreconditionFactory.getInstance().create(qName);
 
                 for (int i = 0; i < atts.getLength(); i++) {
                     String attributeName = atts.getQName(i);
                     String attributeValue = atts.getValue(i);
                     setProperty(currentPrecondition, attributeName, attributeValue);
                 }
                 preconditionLogicStack.peek().addNestedPrecondition(currentPrecondition);
 
                 if (currentPrecondition instanceof PreconditionLogic) {
                     preconditionLogicStack.push(((PreconditionLogic) currentPrecondition));
                 }
 
                 if ("sqlCheck".equals(qName)) {
                     text = new StringBuffer();
                 }
             } else if (changeSet != null && change == null) {
                 change = ChangeFactory.getInstance().create(qName);
                 change.setChangeSet(changeSet);
                 text = new StringBuffer();
                 if (change == null) {
                     throw new MigrationFailedException(changeSet, "Unknown change: " + qName);
                 }
                 change.setFileOpener(fileOpener);
                 if (change instanceof CustomChangeWrapper) {
                     ((CustomChangeWrapper) change).setClassLoader(fileOpener.toClassLoader());
                 }
                 for (int i = 0; i < atts.getLength(); i++) {
                     String attributeName = atts.getQName(i);
                     String attributeValue = atts.getValue(i);
                     setProperty(change, attributeName, attributeValue);
                 }
                 change.setUp();
             } else if (change != null && "column".equals(qName)) {
                 ColumnConfig column;
                 if (change instanceof LoadDataChange) {
                     column = new LoadDataColumnConfig();
                 } else {
                     column = new ColumnConfig();
                 }
                 for (int i = 0; i < atts.getLength(); i++) {
                     String attributeName = atts.getQName(i);
                     String attributeValue = atts.getValue(i);
                     setProperty(column, attributeName, attributeValue);
                 }
                 if (change instanceof ChangeWithColumns) {
                     ((ChangeWithColumns) change).addColumn(column);
                  } else {
                     throw new RuntimeException("Unexpected column tag for " + change.getClass().getName());
                 }
             } else if (change != null && "constraints".equals(qName)) {
                 ConstraintsConfig constraints = new ConstraintsConfig();
                 for (int i = 0; i < atts.getLength(); i++) {
                     String attributeName = atts.getQName(i);
                     String attributeValue = atts.getValue(i);
                     setProperty(constraints, attributeName, attributeValue);
                 }
                 ColumnConfig lastColumn;
                 if (change instanceof AddColumnChange) {
                     lastColumn = ((AddColumnChange) change).getLastColumn();
                 } else if (change instanceof CreateTableChange) {
                     lastColumn = ((CreateTableChange) change).getColumns().get(((CreateTableChange) change).getColumns().size() - 1);
                 } else if (change instanceof ModifyColumnChange) {
                     lastColumn = ((ModifyColumnChange) change).getColumns().get(((ModifyColumnChange) change).getColumns().size() - 1);
                 } else {
                     throw new RuntimeException("Unexpected change: " + change.getClass().getName());
                 }
                 lastColumn.setConstraints(constraints);
             } else if ("param".equals(qName)) {
                 if (change instanceof CustomChangeWrapper) {
                     ((CustomChangeWrapper) change).setParam(atts.getValue("name"), atts.getValue("value"));
                 } else {
                     throw new MigrationFailedException(changeSet, "'param' unexpected in " + qName);
                 }
             } else if ("where".equals(qName)) {
                 text = new StringBuffer();
             } else if ("property".equals(qName)) {
                 if (StringUtils.trimToNull(atts.getValue("file")) == null) {
                     this.setParameterValue(atts.getValue("name"), atts.getValue("value"));
                 } else {
                     Properties props = new Properties();
                     InputStream propertiesStream = fileOpener.getResourceAsStream(atts.getValue("file"));
                     if (propertiesStream == null) {
                         log.info("Could not open properties file "+atts.getValue("file"));
                     } else {
                         props.load(propertiesStream);
 
                         for (Map.Entry entry : props.entrySet()) {
                             this.setParameterValue(entry.getKey().toString(), entry.getValue().toString());
                         }
                     }
                 }
             } else if (change instanceof ExecuteShellCommandChange && "arg".equals(qName)) {
                 ((ExecuteShellCommandChange) change).addArg(atts.getValue("value"));
             } else {
                 throw new MigrationFailedException(changeSet, "Unexpected tag: " + qName);
             }
         } catch (Exception e) {
             log.log(Level.SEVERE, "Error thrown as a SAXException: " + e.getMessage(), e);
             e.printStackTrace();
             throw new SAXException(e);
         }
     }
 
     protected void handleIncludedChangeLog(String fileName) throws LiquibaseException {
         for (ChangeSet changeSet : new ChangeLogParser(changeLogParameters).parse(fileName, fileOpener).getChangeSets()) {
             databaseChangeLog.addChangeSet(changeSet);
         }
     }
 
     private void setProperty(Object object, String attributeName, String attributeValue) throws IllegalAccessException, InvocationTargetException, CustomChangeException {
         if (object instanceof CustomChangeWrapper) {
             if (attributeName.equals("class")) {
                 ((CustomChangeWrapper) object).setClass(expandExpressions(attributeValue));
             } else {
                 ((CustomChangeWrapper) object).setParam(attributeName, expandExpressions(attributeValue));
             }
         } else {
             ObjectUtil.setProperty(object, attributeName, expandExpressions(attributeValue));
         }
     }
 
     public void endElement(String uri, String localName, String qName) throws SAXException {
         String textString = null;
         if (text != null && text.length() > 0) {
             textString = expandExpressions(StringUtils.trimToNull(text.toString()));
         }
 
         try {
             if (rootPrecondition != null) {
                 if ("preConditions".equals(qName)) {
                     if (changeSet == null) {
                         databaseChangeLog.setPreconditions(rootPrecondition);
                         handlePreCondition(rootPrecondition);
                     } else {
                         changeSet.setPreconditions(rootPrecondition);
                     }
                     rootPrecondition = null;
                 } else if ("and".equals(qName)) {
                     preconditionLogicStack.pop();
                     currentPrecondition = null;
                 } else if ("or".equals(qName)) {
                     preconditionLogicStack.pop();
                     currentPrecondition = null;
                 } else if ("not".equals(qName)) {
                     preconditionLogicStack.pop();
                     currentPrecondition = null;
                 } else if (qName.equals("sqlCheck")) {
                     ((SqlPrecondition) currentPrecondition).setSql(textString);
                     currentPrecondition = null;
                 } else if (qName.equals("customPrecondition")) {
                     ((CustomPreconditionWrapper) currentPrecondition).setClassLoader(fileOpener.toClassLoader());
                 }
 
             } else if (changeSet != null && "rollback".equals(qName)) {
                 changeSet.addRollBackSQL(textString);
                 inRollback = false;
             } else if (change != null && change instanceof RawSQLChange && "comment".equals(qName)) {
                 ((RawSQLChange) change).setComments(textString);
                 text = new StringBuffer();
             } else if (change != null && "where".equals(qName)) {
                 if (change instanceof UpdateDataChange) {
                     ((UpdateDataChange) change).setWhereClause(textString);
                 } else if (change instanceof DeleteDataChange) {
                     ((DeleteDataChange) change).setWhereClause(textString);
                 } else {
                     throw new RuntimeException("Unexpected change type: "+change.getClass().getName());
                 }
                 text = new StringBuffer();
             } else if (change != null && change instanceof CreateProcedureChange && "comment".equals(qName)) {
                 ((CreateProcedureChange) change).setComments(textString);
                 text = new StringBuffer();
             } else if (changeSet != null && "comment".equals(qName)) {
                 changeSet.setComments(textString);
                 text = new StringBuffer();
             } else if (changeSet != null && "changeSet".equals(qName)) {
                 handleChangeSet(changeSet);
                 changeSet = null;
             } else if (change != null && qName.equals(change.getTagName())) {
                 if (textString != null) {
                     if (change instanceof RawSQLChange) {
                         ((RawSQLChange) change).setSql(textString);
                     } else if (change instanceof CreateProcedureChange) {
                         ((CreateProcedureChange) change).setProcedureBody(textString);
                     } else if (change instanceof CreateViewChange) {
                         ((CreateViewChange) change).setSelectQuery(textString);
                     } else if (change instanceof InsertDataChange) {
                         List<ColumnConfig> columns = ((InsertDataChange) change).getColumns();
                         columns.get(columns.size() - 1).setValue(textString);
                     } else if (change instanceof UpdateDataChange) {
                         List<ColumnConfig> columns = ((UpdateDataChange) change).getColumns();
                         columns.get(columns.size() - 1).setValue(textString);
                     } else {
                         throw new RuntimeException("Unexpected text in " + change.getTagName());
                     }
                 }
                 text = null;
                 if (inRollback) {
                     changeSet.addRollbackChange(change);
                 } else {
                     changeSet.addChange(change);
                 }
                 change = null;
             } else if (changeSet != null && "validCheckSum".equals(qName)) {
                 changeSet.addValidCheckSum(text.toString());
                 text = null;
             }
         } catch (Exception e) {
             log.log(Level.SEVERE, "Error thrown as a SAXException: " + e.getMessage(), e);
             throw new SAXException(databaseChangeLog.getPhysicalFilePath() + ": " + e.getMessage(), e);
         }
     }
 
     protected String expandExpressions(String text) {
         if (text == null) {
             return null;
         }
         Pattern expressionPattern = Pattern.compile("(\\$\\{[^\\}]+\\})");
         Matcher matcher = expressionPattern.matcher(text);
         String originalText = text;
         while (matcher.find()) {
             String expressionString = originalText.substring(matcher.start(), matcher.end());
             String valueTolookup = expressionString.replaceFirst("\\$\\{","").replaceFirst("\\}$", "");
 
             int dotIndex = valueTolookup.indexOf('.');
             Object value = getParameterValue(valueTolookup);
 
             if (value != null) {
                 text = text.replace(expressionString, value.toString());
             }
         }
         return text;
     }
 
     protected void handlePreCondition(@SuppressWarnings("unused")Precondition precondition) {
         databaseChangeLog.setPreconditions(rootPrecondition);
     }
 
     protected void handleChangeSet(ChangeSet changeSet) {
         databaseChangeLog.addChangeSet(changeSet);
     }
 
     public void characters(char ch[], int start, int length) throws SAXException {
         if (text != null) {
             text.append(new String(ch, start, length));
         }
     }
 
     public Object getParameterValue(String paramter) {
         return changeLogParameters.get(paramter);
     }
 
     public void setParameterValue(String paramter, Object value) {
         if (!changeLogParameters.containsKey(paramter)) {
             changeLogParameters.put(paramter, value);
         }
     }
 
     /**
      * Wrapper for Attributes that expands the value as needed
      */
     private  class ExpandingAttributes implements Attributes {
         private Attributes attributes;
 
         private ExpandingAttributes(Attributes attributes) {
             this.attributes = attributes;
         }
 
         public int getLength() {
             return attributes.getLength();
         }
 
         public String getURI(int index) {
             return attributes.getURI(index);
         }
 
         public String getLocalName(int index) {
             return attributes.getLocalName(index);
         }
 
         public String getQName(int index) {
             return attributes.getQName(index);
         }
 
         public String getType(int index) {
             return attributes.getType(index);
         }
 
         public String getValue(int index) {
             return attributes.getValue(index);
         }
 
         public int getIndex(String uri, String localName) {
             return attributes.getIndex(uri, localName);
         }
 
         public int getIndex(String qName) {
             return attributes.getIndex(qName);
         }
 
         public String getType(String uri, String localName) {
             return attributes.getType(uri, localName);
         }
 
         public String getType(String qName) {
             return attributes.getType(qName);
         }
 
         public String getValue(String uri, String localName) {
             return expandExpressions(attributes.getValue(uri, localName));
         }
 
         public String getValue(String qName) {
             return expandExpressions(attributes.getValue(qName));
         }
     }
 }
