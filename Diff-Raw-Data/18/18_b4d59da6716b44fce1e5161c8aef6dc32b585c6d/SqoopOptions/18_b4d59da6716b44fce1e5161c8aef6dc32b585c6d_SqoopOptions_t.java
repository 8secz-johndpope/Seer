 /**
  * Licensed to Cloudera, Inc. under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  Cloudera, Inc. licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 
 package org.apache.hadoop.sqoop;
 
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.util.Arrays;
 import java.util.ArrayList;
 import java.util.Properties;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.sqoop.lib.LargeObjectLoader;
 import org.apache.hadoop.util.ToolRunner;
 import org.apache.log4j.Category;
 import org.apache.log4j.Level;
 import org.apache.log4j.Logger;
 
 /**
  * Command-line arguments used by Sqoop
  */
 public class SqoopOptions {
 
   public static final Log LOG = LogFactory.getLog(SqoopOptions.class.getName());
 
   /**
    * Thrown when invalid cmdline options are given
    */
   @SuppressWarnings("serial")
   public static class InvalidOptionsException extends Exception {
 
     private String message;
 
     public InvalidOptionsException(final String msg) {
       this.message = msg;
     }
 
     public String getMessage() {
       return message;
     }
 
     public String toString() {
       return getMessage();
     }
   }
 
   // selects in-HDFS destination file format
   public enum FileLayout {
     TextFile,
     SequenceFile
   }
 
 
   // TODO(aaron): Adding something here? Add a setter and a getter.
   // Add a default value in initDefaults() if you need one.
   // If you want to load from a properties file, add an entry in the
   // loadFromProperties() method.
   // Then add command-line arguments in the appropriate tools. The
   // names of all command-line args are stored as constants in BaseSqoopTool.
   private String connectString;
   private String tableName;
   private String [] columns;
   private String username;
   private String password;
   private String codeOutputDir;
   private String jarOutputDir;
   private String hadoopHome;
   private String splitByCol;
   private String whereClause;
   private String debugSqlCmd;
   private String driverClassName;
   private String warehouseDir;
   private FileLayout layout;
   private boolean direct; // if true and conn is mysql, use mysqldump.
   private String tmpDir; // where temp data goes; usually /tmp
   private String hiveHome;
   private boolean hiveImport;
   private boolean overwriteHiveTable;
   private String hiveTableName;
   private String packageName; // package to prepend to auto-named classes.
   private String className; // package+class to apply to individual table import.
                             // also used as an *input* class with existingJarFile.
   private String existingJarFile; // Name of a jar containing existing table definition
                                   // class to use.
   private int numMappers;
   private boolean useCompression;
   private long directSplitSize; // In direct mode, open a new stream every X bytes.
 
   private long maxInlineLobSize; // Max size of an inline LOB; larger LOBs are written
                                  // to external files on disk.
 
   private String exportDir; // HDFS path to read from when performing an export
 
   private char inputFieldDelim;
   private char inputRecordDelim;
   private char inputEnclosedBy;
   private char inputEscapedBy;
   private boolean inputMustBeEnclosed;
 
   private char outputFieldDelim;
   private char outputRecordDelim;
   private char outputEnclosedBy;
   private char outputEscapedBy;
   private boolean outputMustBeEnclosed;
 
   private boolean areDelimsManuallySet;
 
   private Configuration conf;
 
   public static final int DEFAULT_NUM_MAPPERS = 4;
 
   private static final String DEFAULT_CONFIG_FILE = "sqoop.properties";
 
   private String [] extraArgs;
 
   public SqoopOptions() {
     initDefaults(null);
   }
 
   public SqoopOptions(Configuration conf) {
     initDefaults(conf);
   }
 
   /**
    * Alternate SqoopOptions interface used mostly for unit testing
    * @param connect JDBC connect string to use
    * @param table Table to read
    */
   public SqoopOptions(final String connect, final String table) {
     initDefaults(null);
 
     this.connectString = connect;
     this.tableName = table;
   }
 
   private boolean getBooleanProperty(Properties props, String propName, boolean defaultValue) {
     String str = props.getProperty(propName,
         Boolean.toString(defaultValue)).toLowerCase();
     return "true".equals(str) || "yes".equals(str) || "1".equals(str);
   }
 
   private long getLongProperty(Properties props, String propName, long defaultValue) {
     String str = props.getProperty(propName,
         Long.toString(defaultValue)).toLowerCase();
     try {
       return Long.parseLong(str);
     } catch (NumberFormatException nfe) {
       LOG.warn("Could not parse integer value for config parameter " + propName);
       return defaultValue;
     }
   }
 
   private void loadFromProperties() {
     File configFile = new File(DEFAULT_CONFIG_FILE);
     if (!configFile.canRead()) {
       return; //can't do this.
     }
 
     Properties props = new Properties();
     InputStream istream = null;
     try {
       LOG.info("Loading properties from " + configFile.getAbsolutePath());
       istream = new FileInputStream(configFile);
       props.load(istream);
 
       this.hadoopHome = props.getProperty("hadoop.home", this.hadoopHome);
       this.codeOutputDir = props.getProperty("out.dir", this.codeOutputDir);
       this.jarOutputDir = props.getProperty("bin.dir", this.jarOutputDir);
       this.username = props.getProperty("db.username", this.username);
       this.password = props.getProperty("db.password", this.password);
       this.tableName = props.getProperty("db.table", this.tableName);
       this.connectString = props.getProperty("db.connect.url", this.connectString);
       this.splitByCol = props.getProperty("db.split.column", this.splitByCol);
       this.whereClause = props.getProperty("db.where.clause", this.whereClause);
       this.driverClassName = props.getProperty("jdbc.driver", this.driverClassName);
       this.warehouseDir = props.getProperty("hdfs.warehouse.dir", this.warehouseDir);
       this.hiveHome = props.getProperty("hive.home", this.hiveHome);
       this.className = props.getProperty("java.classname", this.className);
       this.packageName = props.getProperty("java.packagename", this.packageName);
       this.existingJarFile = props.getProperty("java.jar.file", this.existingJarFile);
       this.exportDir = props.getProperty("export.dir", this.exportDir);
 
       this.direct = getBooleanProperty(props, "direct.import", this.direct);
       this.hiveImport = getBooleanProperty(props, "hive.import", this.hiveImport);
       this.overwriteHiveTable = getBooleanProperty(props, "hive.overwrite.table", this.overwriteHiveTable);
       this.useCompression = getBooleanProperty(props, "compression", this.useCompression);
       this.directSplitSize = getLongProperty(props, "direct.split.size",
           this.directSplitSize);
     } catch (IOException ioe) {
       LOG.error("Could not read properties file " + DEFAULT_CONFIG_FILE + ": " + ioe.toString());
     } finally {
       if (null != istream) {
         try {
           istream.close();
         } catch (IOException ioe) {
           // ignore this; we're closing.
         }
       }
     }
   }
 
   /**
    * @return the temp directory to use; this is guaranteed to end with
    * the file separator character (e.g., '/')
    */
   public String getTempDir() {
     return this.tmpDir;
   }
 
   private void initDefaults(Configuration baseConfiguration) {
     // first, set the true defaults if nothing else happens.
     // default action is to run the full pipeline.
     this.hadoopHome = System.getenv("HADOOP_HOME");
 
     // Set this with $HIVE_HOME, but -Dhive.home can override.
     this.hiveHome = System.getenv("HIVE_HOME");
     this.hiveHome = System.getProperty("hive.home", this.hiveHome);
 
     // Set this to cwd, but -Dsqoop.src.dir can override.
     this.codeOutputDir = System.getProperty("sqoop.src.dir", ".");
 
     String myTmpDir = System.getProperty("test.build.data", "/tmp/");
     if (!myTmpDir.endsWith(File.separator)) {
       myTmpDir = myTmpDir + File.separator;
     }
 
     this.tmpDir = myTmpDir;
     this.jarOutputDir = tmpDir + "sqoop/compile";
     this.layout = FileLayout.TextFile;
 
     this.inputFieldDelim = '\000';
     this.inputRecordDelim = '\000';
     this.inputEnclosedBy = '\000';
     this.inputEscapedBy = '\000';
     this.inputMustBeEnclosed = false;
 
     this.outputFieldDelim = ',';
     this.outputRecordDelim = '\n';
     this.outputEnclosedBy = '\000';
     this.outputEscapedBy = '\000';
     this.outputMustBeEnclosed = false;
 
     this.areDelimsManuallySet = false;
 
     this.numMappers = DEFAULT_NUM_MAPPERS;
     this.useCompression = false;
     this.directSplitSize = 0;
 
     this.maxInlineLobSize = LargeObjectLoader.DEFAULT_MAX_LOB_LENGTH;
 
     if (null == baseConfiguration) {
       this.conf = new Configuration();
     } else {
       this.conf = baseConfiguration;
     }
 
     this.extraArgs = null;
 
     loadFromProperties();
   }
 
   /**
    * Given a string containing a single character or an escape sequence representing
    * a char, return that char itself.
    *
    * Normal literal characters return themselves: "x" -&gt; 'x', etc.
    * Strings containing a '\' followed by one of t, r, n, or b escape to the usual
    * character as seen in Java: "\n" -&gt; (newline), etc.
    *
    * Strings like "\0ooo" return the character specified by the octal sequence 'ooo'
    * Strings like "\0xhhh" or "\0Xhhh" return the character specified by the hex sequence 'hhh'
    *
    * If the input string contains leading or trailing spaces, these are ignored.
    */
   public static char toChar(String charish) throws InvalidOptionsException {
     if (null == charish || charish.length() == 0) {
       throw new InvalidOptionsException("Character argument expected." 
           + "\nTry --help for usage instructions.");
     }
 
     if (charish.startsWith("\\0x") || charish.startsWith("\\0X")) {
       if (charish.length() == 3) {
         throw new InvalidOptionsException("Base-16 value expected for character argument."
           + "\nTry --help for usage instructions.");
       } else {
         String valStr = charish.substring(3);
         int val = Integer.parseInt(valStr, 16);
         return (char) val;
       }
     } else if (charish.startsWith("\\0")) {
       if (charish.equals("\\0")) {
         // it's just '\0', which we can take as shorthand for nul.
         return '\000';
       } else {
         // it's an octal value.
         String valStr = charish.substring(2);
         int val = Integer.parseInt(valStr, 8);
         return (char) val;
       }
     } else if (charish.startsWith("\\")) {
       if (charish.length() == 1) {
         // it's just a '\'. Keep it literal.
         return '\\';
       } else if (charish.length() > 2) {
         // we don't have any 3+ char escape strings. 
         throw new InvalidOptionsException("Cannot understand character argument: " + charish
             + "\nTry --help for usage instructions.");
       } else {
         // this is some sort of normal 1-character escape sequence.
         char escapeWhat = charish.charAt(1);
         switch(escapeWhat) {
         case 'b':
           return '\b';
         case 'n':
           return '\n';
         case 'r':
           return '\r';
         case 't':
           return '\t';
         case '\"':
           return '\"';
         case '\'':
           return '\'';
         case '\\':
           return '\\';
         default:
           throw new InvalidOptionsException("Cannot understand character argument: " + charish
               + "\nTry --help for usage instructions.");
         }
       }
     } else {
       // it's a normal character.
       if (charish.length() > 1) {
         LOG.warn("Character argument " + charish + " has multiple characters; "
             + "only the first will be used.");
       }
 
       return charish.charAt(0);
     }
   }
 
   /** get the temporary directory; guaranteed to end in File.separator
    * (e.g., '/')
    */
   public String getTmpDir() {
     return tmpDir;
   }
 
   public void setTmpDir(String tmp) {
    this.tmpDir = tmp;
   }
 
   public String getConnectString() {
     return connectString;
   }
 
   public void setConnectString(String connectStr) {
     this.connectString = connectStr;
   }
 
   public String getTableName() {
     return tableName;
   }
 
   public void setTableName(String tableName) {
     this.tableName = tableName;
   }
 
   public String getExportDir() {
     return exportDir;
   }
 
   public void setExportDir(String exportDir) {
     this.exportDir = exportDir;
   }
 
   public String getExistingJarName() {
     return existingJarFile;
   }
 
   public void setExistingJarName(String jarFile) {
     this.existingJarFile = jarFile;
   }
 
   public String[] getColumns() {
     if (null == columns) {
       return null;
     } else {
       return Arrays.copyOf(columns, columns.length);
     }
   }
 
   public void setColumns(String [] cols) {
     if (null == cols) {
       this.columns = null;
     } else {
       this.columns = Arrays.copyOf(cols, cols.length);
     }
   }
 
   public String getSplitByCol() {
     return splitByCol;
   }
 
   public void setSplitByCol(String splitBy) {
     this.splitByCol = splitBy;
   }
   
   public String getWhereClause() {
     return whereClause;
   }
 
   public void setWhereClause(String where) {
     this.whereClause = where;
   }
 
   public String getUsername() {
     return username;
   }
 
   public void setUsername(String user) {
     this.username = user;
   }
 
   public String getPassword() {
     return password;
   }
 
   /**
    * Allow the user to enter his password on the console without printing characters.
    * @return the password as a string
    */
   private String securePasswordEntry() {
     return new String(System.console().readPassword("Enter password: "));
   }
 
   /**
    * Set the password in this SqoopOptions from the console without printing
    * characters.
    */
   public void setPasswordFromConsole() {
     this.password = securePasswordEntry();
   }
 
   public void setPassword(String pass) {
     this.password = pass;
   }
 
   public boolean isDirect() {
     return direct;
   }
 
   public void setDirectMode(boolean isDirect) {
     this.direct = isDirect;
   }
 
   /**
    * @return the number of map tasks to use for import
    */
   public int getNumMappers() {
     return this.numMappers;
   }
 
   public void setNumMappers(int numMappers) {
     this.numMappers = numMappers;
   }
 
   /**
    * @return the user-specified absolute class name for the table
    */
   public String getClassName() {
     return className;
   }
 
   public void setClassName(String className) {
     this.className = className;
   }
 
   /**
    * @return the user-specified package to prepend to table names via --package-name.
    */
   public String getPackageName() {
     return packageName;
   }
 
   public void setPackageName(String packageName) {
     this.packageName = packageName;
   }
 
   public String getHiveHome() {
     return hiveHome;
   }
   
   public void setHiveHome(String hiveHome) {
     this.hiveHome = hiveHome;
   }
 
   /** @return true if we should import the table into Hive */
   public boolean doHiveImport() {
     return hiveImport;
   }
 
   public void setHiveImport(boolean hiveImport) {
     this.hiveImport = hiveImport;
   }
 
   /**
    * @return the user-specified option to overwrite existing table in hive
    */
   public boolean doOverwriteHiveTable() {
     return overwriteHiveTable;
   }
 
   public void setOverwriteHiveTable(boolean overwrite) {
     this.overwriteHiveTable = overwrite;
   }
 
   /**
    * @return location where .java files go; guaranteed to end with '/'
    */
   public String getCodeOutputDir() {
     if (codeOutputDir.endsWith(File.separator)) {
       return codeOutputDir;
     } else {
       return codeOutputDir + File.separator;
     }
   }
 
   public void setCodeOutputDir(String outputDir) {
     this.codeOutputDir = outputDir;
   }
 
   /**
    * @return location where .jar and .class files go; guaranteed to end with '/'
    */
   public String getJarOutputDir() {
     if (jarOutputDir.endsWith(File.separator)) {
       return jarOutputDir;
     } else {
       return jarOutputDir + File.separator;
     }
   }
 
   public void setJarOutputDir(String outDir) {
     this.jarOutputDir = outDir;
   }
 
   /**
    * Return the value of $HADOOP_HOME
    * @return $HADOOP_HOME, or null if it's not set.
    */
   public String getHadoopHome() {
     return hadoopHome;
   }
 
   public void setHadoopHome(String hadoopHome) {
     this.hadoopHome = hadoopHome;
   }
 
   /**
    * @return a sql command to execute and exit with.
    */
   public String getDebugSqlCmd() {
     return debugSqlCmd;
   }
 
   public void setDebugSqlCmd(String sqlStatement) {
     this.debugSqlCmd = sqlStatement;
   }
 
   /**
    * @return The JDBC driver class name specified with --driver
    */
   public String getDriverClassName() {
     return driverClassName;
   }
 
   public void setDriverClassName(String driverClass) {
     this.driverClassName = driverClass;
   }
 
   /**
    * @return the base destination path for table uploads.
    */
   public String getWarehouseDir() {
     return warehouseDir;
   }
 
   public void setWarehouseDir(String warehouse) {
     this.warehouseDir = warehouse;
   }
 
   /**
    * @return the destination file format
    */
   public FileLayout getFileLayout() {
     return this.layout;
   }
 
   public void setFileLayout(FileLayout layout) {
     this.layout = layout;
   }
 
   /**
    * @return the field delimiter to use when parsing lines. Defaults to the field delim
    * to use when printing lines
    */
   public char getInputFieldDelim() {
     if (inputFieldDelim == '\000') {
       return this.outputFieldDelim;
     } else {
       return this.inputFieldDelim;
     }
   }
 
   public void setInputFieldsTerminatedBy(char c) {
     this.inputFieldDelim = c;
   }
 
   /**
    * @return the record delimiter to use when parsing lines. Defaults to the record delim
    * to use when printing lines.
    */
   public char getInputRecordDelim() {
     if (inputRecordDelim == '\000') {
       return this.outputRecordDelim;
     } else {
       return this.inputRecordDelim;
     }
   }
 
   public void setInputLinesTerminatedBy(char c) {
     this.inputRecordDelim = c;
   }
 
   /**
    * @return the character that may enclose fields when parsing lines. Defaults to the
    * enclosing-char to use when printing lines.
    */
   public char getInputEnclosedBy() {
     if (inputEnclosedBy == '\000') {
       return this.outputEnclosedBy;
     } else {
       return this.inputEnclosedBy;
     }
   }
 
   public void setInputEnclosedBy(char c) {
     this.inputEnclosedBy = c;
   }
 
   /**
    * @return the escape character to use when parsing lines. Defaults to the escape
    * character used when printing lines.
    */
   public char getInputEscapedBy() {
     if (inputEscapedBy == '\000') {
       return this.outputEscapedBy;
     } else {
       return this.inputEscapedBy;
     }
   }
 
   public void setInputEscapedBy(char c) {
     this.inputEscapedBy = c;
   }
 
   /**
    * @return true if fields must be enclosed by the --enclosed-by character when parsing.
    * Defaults to false. Set true when --input-enclosed-by is used.
    */
   public boolean isInputEncloseRequired() {
     if (inputEnclosedBy == '\000') {
       return this.outputMustBeEnclosed;
     } else {
       return this.inputMustBeEnclosed;
     }
   }
 
   public void setInputEncloseRequired(boolean required) {
     this.inputMustBeEnclosed = required;
   }
 
   /**
    * @return the character to print between fields when importing them to text.
    */
   public char getOutputFieldDelim() {
     return this.outputFieldDelim;
   }
 
   public void setFieldsTerminatedBy(char c) {
     this.outputFieldDelim = c;
   }
 
 
   /**
    * @return the character to print between records when importing them to text.
    */
   public char getOutputRecordDelim() {
     return this.outputRecordDelim;
   }
 
   public void setLinesTerminatedBy(char c) {
     this.outputRecordDelim = c;
   }
 
   /**
    * @return a character which may enclose the contents of fields when imported to text.
    */
   public char getOutputEnclosedBy() {
     return this.outputEnclosedBy;
   }
 
   public void setEnclosedBy(char c) {
     this.outputEnclosedBy = c;
   }
 
   /**
    * @return a character which signifies an escape sequence when importing to text.
    */
   public char getOutputEscapedBy() {
     return this.outputEscapedBy;
   }
 
   public void setEscapedBy(char c) {
     this.outputEscapedBy = c;
   }
 
   /**
    * @return true if fields imported to text must be enclosed by the EnclosedBy char.
    * default is false; set to true if --enclosed-by is used instead of --optionally-enclosed-by.
    */
   public boolean isOutputEncloseRequired() {
     return this.outputMustBeEnclosed;
   }
 
   public void setOutputEncloseRequired(boolean required) {
     this.outputMustBeEnclosed = required; 
   }
 
   /**
    * @return true if the user wants imported results to be compressed.
    */
   public boolean shouldUseCompression() {
     return this.useCompression;
   }
 
   public void setUseCompression(boolean useCompression) {
     this.useCompression = useCompression;
   }
 
   /**
    * @return the name of the destination table when importing to Hive
    */
   public String getHiveTableName() {
     if (null != this.hiveTableName) {
       return this.hiveTableName;
     } else {
       return this.tableName;
     }
   }
 
   public void setHiveTableName(String tableName) {
     this.hiveTableName = tableName;
   }
 
   /**
    * @return the file size to split by when using --direct mode.
    */
   public long getDirectSplitSize() {
     return this.directSplitSize;
   }
 
   public void setDirectSplitSize(long splitSize) {
     this.directSplitSize = splitSize;
   }
 
   /**
    * @return the max size of a LOB before we spill to a separate file.
    */
   public long getInlineLobLimit() {
     return this.maxInlineLobSize;
   }
 
   public void setInlineLobLimit(long limit) {
     this.maxInlineLobSize = limit;
   }
 
   /**
    * @return true if the delimiters have been explicitly set by the user.
    */
   public boolean explicitDelims() {
     return areDelimsManuallySet;
   }
 
   /**
    * Flag the delimiter settings as explicit user settings, or implicit.
    */
   public void setExplicitDelims(boolean explicit) {
     this.areDelimsManuallySet = explicit;
   }
 
   public Configuration getConf() {
     return conf;
   }
 
   public void setConf(Configuration config) {
     this.conf = config;
   }
 
   /**
    * @return command-line arguments after a '-'
    */
   public String [] getExtraArgs() {
     if (extraArgs == null) {
       return null;
     }
 
     String [] out = new String[extraArgs.length];
     for (int i = 0; i < extraArgs.length; i++) {
       out[i] = extraArgs[i];
     }
     return out;
   }
 
   public void setExtraArgs(String [] args) {
     if (null == args) {
       this.extraArgs = null;
       return;
     }
 
     this.extraArgs = new String[args.length];
     for (int i = 0; i < args.length; i++) {
       this.extraArgs[i] = args[i];
     }
   }
 }
