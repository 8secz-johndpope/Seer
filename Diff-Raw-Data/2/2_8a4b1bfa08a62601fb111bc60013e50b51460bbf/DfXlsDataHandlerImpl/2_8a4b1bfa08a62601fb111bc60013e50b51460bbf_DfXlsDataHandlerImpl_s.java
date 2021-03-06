 /*
  * Copyright 2004-2011 the Seasar Foundation and the Others.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
  * either express or implied. See the License for the specific language
  * governing permissions and limitations under the License.
  */
 package org.seasar.dbflute.logic.replaceschema.loaddata.impl;
 
 import java.io.File;
 import java.io.FilenameFilter;
 import java.sql.Connection;
 import java.sql.PreparedStatement;
 import java.sql.SQLException;
 import java.sql.Timestamp;
 import java.util.ArrayList;
 import java.util.Comparator;
 import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
 import java.util.Set;
 import java.util.SortedSet;
 import java.util.TreeSet;
 import java.util.regex.Pattern;
 import java.util.regex.PatternSyntaxException;
 
 import javax.sql.DataSource;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.seasar.dbflute.exception.DfXlsDataColumnDefFailureException;
 import org.seasar.dbflute.exception.DfXlsDataRegistrationFailureException;
 import org.seasar.dbflute.exception.DfXlsDataTableNotFoundException;
 import org.seasar.dbflute.exception.factory.ExceptionMessageBuilder;
 import org.seasar.dbflute.helper.StringKeyMap;
 import org.seasar.dbflute.helper.dataset.DfDataColumn;
 import org.seasar.dbflute.helper.dataset.DfDataRow;
 import org.seasar.dbflute.helper.dataset.DfDataSet;
 import org.seasar.dbflute.helper.dataset.DfDataTable;
 import org.seasar.dbflute.helper.dataset.states.DfDtsCreatedState;
 import org.seasar.dbflute.helper.dataset.states.DfDtsSqlContext;
 import org.seasar.dbflute.helper.dataset.types.DfDtsColumnType;
 import org.seasar.dbflute.helper.dataset.types.DfDtsColumnTypes;
 import org.seasar.dbflute.helper.io.xls.DfXlsReader;
 import org.seasar.dbflute.logic.jdbc.metadata.info.DfColumnMeta;
 import org.seasar.dbflute.logic.replaceschema.loaddata.DfLoadedDataInfo;
 import org.seasar.dbflute.logic.replaceschema.loaddata.DfXlsDataHandler;
 import org.seasar.dbflute.logic.replaceschema.loaddata.DfXlsDataResource;
 import org.seasar.dbflute.properties.filereader.DfMapStringFileReader;
 
 /**
  * The implementation of xls data handler. And also of writer.
  * @author jflute
  */
 public class DfXlsDataHandlerImpl extends DfAbsractDataWriter implements DfXlsDataHandler {
 
     // ===================================================================================
     //                                                                          Definition
     //                                                                          ==========
     /** Log instance. */
     private static final Log _log = LogFactory.getLog(DfXlsDataHandlerImpl.class);
 
     // ===================================================================================
     //                                                                           Attribute
     //                                                                           =========
     /** The pattern of skip sheet. (NullAllowed) */
     protected Pattern _skipSheetPattern;
 
     // ===================================================================================
     //                                                                         Constructor
     //                                                                         ===========
     public DfXlsDataHandlerImpl(DataSource dataSource) {
         super(dataSource); // use database only when writing
     }
 
     // ===================================================================================
     //                                                                                Read
     //                                                                                ====
     public List<DfDataSet> readSeveralData(DfXlsDataResource resource) {
         final String dataDirectory = resource.getDataDirectory();
         final List<File> xlsList = getXlsList(resource);
         final List<DfDataSet> ls = new ArrayList<DfDataSet>();
         for (File file : xlsList) {
             final DfXlsReader xlsReader = createXlsReader(dataDirectory, file);
             ls.add(xlsReader.read());
         }
         return ls;
     }
 
     // ===================================================================================
     //                                                                               Write
     //                                                                               =====
     public void writeSeveralData(DfXlsDataResource resource, DfLoadedDataInfo loadedDataInfo) {
         final String dataDirectory = resource.getDataDirectory();
         final List<File> xlsList = getXlsList(resource);
         for (File file : xlsList) {
             _log.info("/= = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = ");
             _log.info("writeData(" + file + ")");
             _log.info("= = = = = = =/");
             final DfXlsReader xlsReader = createXlsReader(dataDirectory, file);
             final DfDataSet dataSet = xlsReader.read();
 
             filterValidColumn(dataSet);
             setupDefaultValue(dataDirectory, dataSet);
 
             doWriteDataSet(file, dataSet);
             final boolean warned = false; // this has no warning fixedly
             loadedDataInfo.addLoadedFile(resource.getEnvType(), "xls", null, file.getName(), warned);
         }
     }
 
     protected void doWriteDataSet(File file, DfDataSet dataSet) {
         for (int i = 0; i < dataSet.getTableSize(); i++) {
             final DfDataTable dataTable = dataSet.getTable(i);
             doWriteDataTable(file, dataTable);
         }
     }
 
     protected void doWriteDataTable(File file, DfDataTable dataTable) {
         final String tableDbName = dataTable.getTableDbName();
         if (dataTable.getRowSize() == 0) {
             _log.info("*Not found row at the table: " + tableDbName);
             return;
         }
 
         // set up columnMetaInfo
         final Map<String, DfColumnMeta> columnInfoMap = getColumnInfoMap(tableDbName);
         if (columnInfoMap.isEmpty()) {
             throwTableNotFoundException(file, tableDbName);
         }
 
         // process before handling table
         beforeHandlingTable(tableDbName, columnInfoMap);
 
         // set up columnNameList
         final List<String> columnNameList = new ArrayList<String>();
         for (int j = 0; j < dataTable.getColumnSize(); j++) {
             final DfDataColumn dataColumn = dataTable.getColumn(j);
             final String columnName = dataColumn.getColumnDbName();
             columnNameList.add(columnName);
         }
 
         Connection conn = null;
         PreparedStatement ps = null;
         try {
             conn = _dataSource.getConnection();
             for (int j = 0; j < dataTable.getRowSize(); j++) {
                 final DfDataRow dataRow = dataTable.getRow(j);
                 if (ps == null) {
                     final MyCreatedState myCreatedState = new MyCreatedState();
                     final String preparedSql = myCreatedState.buildPreparedSql(dataRow);
                     ps = conn.prepareStatement(preparedSql);
                 }
                 doWriteDataRow(file, dataTable, dataRow, columnInfoMap, columnNameList, conn, ps);
             }
             if (!_suppressBatchUpdate) {
                 ps.executeBatch();
             }
         } catch (SQLException e) {
             final SQLException nextEx = e.getNextException();
             if (nextEx != null && !e.equals(nextEx)) { // focus on next exception
                 _log.warn("*Failed to register the xls data: " + e.getMessage());
                 String msg = buildRegistrationExceptionMessage(file, tableDbName, nextEx);
                 throw new DfXlsDataRegistrationFailureException(msg, nextEx); // switch!
             }
             String msg = buildRegistrationExceptionMessage(file, tableDbName, e);
             throw new DfXlsDataRegistrationFailureException(msg, e);
         } finally {
             if (ps != null) {
                 try {
                     ps.close();
                 } catch (SQLException ignored) {
                     _log.info("Statement#close() threw the exception!", ignored);
                 }
             }
             if (conn != null) {
                 try {
                     conn.close();
                 } catch (SQLException ignored) {
                     _log.info("Connection#close() threw the exception!", ignored);
                 }
             }
             // process after (finally) handling table
             finallyHandlingTable(tableDbName, columnInfoMap);
         }
     }
 
     protected void throwTableNotFoundException(File file, String tableDbName) {
         final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
         br.addNotice("The table specified on the xls file was not found in the schema.");
         br.addItem("Advice");
         br.addElement("Please confirm the name about its spelling.");
         br.addElement("And confirm that whether the DLL executions have errors.");
         br.addItem("Xls File");
         br.addElement(file);
         br.addItem("Table");
         br.addElement(tableDbName);
         final String msg = br.buildExceptionMessage();
         throw new DfXlsDataTableNotFoundException(msg);
     }
 
     protected String buildRegistrationExceptionMessage(File file, String tableDbName, Exception e) {
         final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
         br.addNotice("Failed to register the table data.");
         br.addItem("Xls File");
         br.addElement(file);
         br.addItem("Table");
         br.addElement(tableDbName);
         br.addItem("Message");
         br.addElement(e.getMessage());
         final Map<String, Class<?>> bindTypeCacheMap = _bindTypeCacheMap.get(tableDbName);
         if (bindTypeCacheMap != null) {
             br.addItem("Bind Type");
             final Set<Entry<String, Class<?>>> entrySet = bindTypeCacheMap.entrySet();
             for (Entry<String, Class<?>> entry : entrySet) {
                 br.addElement(entry.getKey() + " = " + entry.getValue());
             }
         }
         final Map<String, StringProcessor> stringProcessorCacheMap = _stringProcessorCacheMap.get(tableDbName);
        if (bindTypeCacheMap != null) {
             br.addItem("String Processor");
             final Set<Entry<String, StringProcessor>> entrySet = stringProcessorCacheMap.entrySet();
             for (Entry<String, StringProcessor> entry : entrySet) {
                 br.addElement(entry.getKey() + " = " + entry.getValue());
             }
         }
         return br.buildExceptionMessage();
     }
 
     protected void beforeHandlingTable(String tableDbName, Map<String, DfColumnMeta> columnInfoMap) {
         if (_dataWritingInterceptor != null) {
             _dataWritingInterceptor.processBeforeHandlingTable(tableDbName, columnInfoMap);
         }
     }
 
     protected void finallyHandlingTable(String tableDbName, Map<String, DfColumnMeta> columnInfoMap) {
         if (_dataWritingInterceptor != null) {
             _dataWritingInterceptor.processFinallyHandlingTable(tableDbName, columnInfoMap);
         }
     }
 
     protected void doWriteDataRow(File file, DfDataTable dataTable, DfDataRow dataRow,
             Map<String, DfColumnMeta> columnInfoMap, List<String> columnNameList, Connection conn, PreparedStatement ps)
             throws SQLException {
         final String tableDbName = dataTable.getTableDbName();
         // ColumnValue and ColumnObject
         final ColumnContainer columnContainer = createColumnContainer(dataTable, dataRow);
         final Map<String, Object> columnValueMap = columnContainer.getColumnValueMap();
         if (columnValueMap.isEmpty()) {
             throwXlsDataColumnDefFailureException(file, dataTable);
         }
         if (_loggingInsertSql) {
             final List<Object> valueList = new ArrayList<Object>(columnValueMap.values());
             _log.info(getSql4Log(tableDbName, columnNameList, valueList));
         }
         int bindCount = 1;
         final Set<Entry<String, Object>> entrySet = columnValueMap.entrySet();
         for (Entry<String, Object> entry : entrySet) {
             final String columnName = entry.getKey();
             final Object obj = entry.getValue();
 
             // - - - - - - - - - - - - - - - - - - -
             // Process Null (against Null Headache)
             // - - - - - - - - - - - - - - - - - - -
             if (processNull(tableDbName, columnName, obj, ps, bindCount, columnInfoMap)) {
                 bindCount++;
                 continue;
             }
 
             // - - - - - - - - - - - - - - -
             // Process NotNull and NotString
             // - - - - - - - - - - - - - - -
             // If the value is not null and the value has the own type except string,
             // It registers the value to statement by the type.
             if (processNotNullNotString(tableDbName, columnName, obj, conn, ps, bindCount, columnInfoMap)) {
                 bindCount++;
                 continue;
             }
 
             // - - - - - - - - - - - - - - - - - - -
             // Process NotNull and StringExpression
             // - - - - - - - - - - - - - - - - - - -
             final String value = (String) obj;
             processNotNullString(file, tableDbName, columnName, value, conn, ps, bindCount, columnInfoMap);
             bindCount++;
         }
         if (_suppressBatchUpdate) {
             ps.execute();
         } else {
             ps.addBatch();
         }
     }
 
     protected void throwXlsDataColumnDefFailureException(File file, DfDataTable dataTable) {
         final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
         br.addNotice("The table specified on the xls file does not have (writable) columns.");
         br.addItem("Advice");
         br.addElement("Please confirm the column names about their spellings.");
         br.addElement("And confirm the column definition of the table.");
         br.addItem("Xls File");
         br.addElement(file);
         br.addItem("Table");
         br.addElement(dataTable.getTableDbName());
         br.addItem("Defined Column");
         final int columnSize = dataTable.getColumnSize();
         if (columnSize > 0) {
             for (int i = 0; i < dataTable.getColumnSize(); i++) {
                 final DfDataColumn dataColumn = dataTable.getColumn(i);
                 br.addElement(dataColumn.getColumnDbName());
             }
         } else {
             br.addElement("(no column)");
         }
         final String msg = br.buildExceptionMessage();
         throw new DfXlsDataColumnDefFailureException(msg);
     }
 
     // ===================================================================================
     //                                                                        Xls Handling
     //                                                                        ============
     protected DfXlsReader createXlsReader(String dataDirectory, File file) {
         final Map<String, String> tableNameMap = getTableNameMap(dataDirectory);
         final Map<String, List<String>> notTrimTableColumnMap = getNotTrimTableColumnMap(dataDirectory);
         final Map<String, List<String>> emptyStringTableColumnMap = getEmptyStringTableColumnMap(dataDirectory);
         final DfXlsReader xlsReader = new DfXlsReader(file, tableNameMap, notTrimTableColumnMap,
                 emptyStringTableColumnMap, _skipSheetPattern);
         if (tableNameMap != null && !tableNameMap.isEmpty()) {
             _log.info("/- - - - - - - - - - - - - - - - - - - - - - - - - - - - ");
             _log.info("tableNameMap = " + tableNameMap);
             _log.info("- - - - - - - - - -/");
         }
         return xlsReader;
     }
 
     public List<File> getXlsList(DfXlsDataResource resource) {
         final Comparator<File> fileNameAscComparator = new Comparator<File>() {
             public int compare(File o1, File o2) {
                 return o1.getName().compareTo(o2.getName());
             }
         };
         final SortedSet<File> sortedFileSet = new TreeSet<File>(fileNameAscComparator);
 
         final String dataDirectory = resource.getDataDirectory();
         final File dir = new File(dataDirectory);
         final FilenameFilter filter = new FilenameFilter() {
             public boolean accept(File dir, String name) {
                 return name.endsWith(".xls");
             }
         };
         final File[] listFiles = dir.listFiles(filter);
         if (listFiles == null) {
             return new ArrayList<File>();
         }
         for (File file : listFiles) {
             sortedFileSet.add(file);
         }
         return new ArrayList<File>(sortedFileSet);
     }
 
     protected void filterValidColumn(final DfDataSet dataSet) {
         for (int i = 0; i < dataSet.getTableSize(); i++) {
             final DfDataTable table = dataSet.getTable(i);
             final String tableName = table.getTableDbName();
 
             final Map<String, DfColumnMeta> metaInfoMap = getColumnInfoMap(tableName);
             for (int j = 0; j < table.getColumnSize(); j++) {
                 final DfDataColumn dataColumn = table.getColumn(j);
                 if (!metaInfoMap.containsKey(dataColumn.getColumnDbName())) {
                     dataColumn.setWritable(false);
                 }
             }
         }
     }
 
     // ===================================================================================
     //                                                                              Option
     //                                                                              ======
     protected void setupDefaultValue(String dataDirectory, final DfDataSet dataSet) {
         final Map<String, String> defaultValueMap = getDefaultValueMap(dataDirectory);
         for (int i = 0; i < dataSet.getTableSize(); i++) {
             final DfDataTable table = dataSet.getTable(i);
             final Set<String> defaultValueMapKeySet = defaultValueMap.keySet();
             final String tableName = table.getTableDbName();
 
             final Map<String, DfColumnMeta> metaInfoMap = getColumnInfoMap(tableName);
             for (String defaultTargetColumnName : defaultValueMapKeySet) {
                 final String defaultValue = defaultValueMap.get(defaultTargetColumnName);
 
                 if (metaInfoMap.containsKey(defaultTargetColumnName) && !table.hasColumn(defaultTargetColumnName)) {
                     final DfDtsColumnType columnType;
                     final Object value;
                     if (defaultValue.equalsIgnoreCase("sysdate")) {
                         columnType = DfDtsColumnTypes.TIMESTAMP;
                         value = new Timestamp(System.currentTimeMillis());
                     } else {
                         columnType = DfDtsColumnTypes.STRING;
                         value = defaultValue;
                     }
                     table.addColumn(defaultTargetColumnName, columnType);
 
                     int rowSize = table.getRowSize();
                     for (int j = 0; j < table.getRowSize(); j++) {
                         final DfDataRow row = table.getRow(j);
                         row.addValue(defaultTargetColumnName, value);
                         ++rowSize;
                     }
                 }
             }
         }
     }
 
     private Map<String, String> getDefaultValueMap(String dataDirectory) {
         final DfMapStringFileReader reader = new DfMapStringFileReader();
         String path = dataDirectory + "/defaultValueMap.dataprop";
         Map<String, String> resultMap = reader.readMapAsStringValue(path);
         if (resultMap != null && !resultMap.isEmpty()) {
             return resultMap;
         }
         path = dataDirectory + "/default-value.txt";
         resultMap = reader.readMapAsStringValue(path);
         return resultMap;
     }
 
     private Map<String, String> getTableNameMap(String dataDirectory) {
         final DfMapStringFileReader reader = new DfMapStringFileReader();
         String path = dataDirectory + "/tableNameMap.dataprop";
         Map<String, String> resultMap = reader.readMapAsStringValue(path);
         if (resultMap == null || resultMap.isEmpty()) {
             path = dataDirectory + "/table-name.txt";
             resultMap = reader.readMapAsStringValue(path);
         }
         final StringKeyMap<String> flmap = StringKeyMap.createAsFlexible();
         flmap.putAll(resultMap);
         return flmap;
     }
 
     private Map<String, List<String>> getNotTrimTableColumnMap(String dataDirectory) {
         final DfMapStringFileReader reader = new DfMapStringFileReader();
         String path = dataDirectory + "/notTrimColumnMap.dataprop";
         Map<String, List<String>> resultMap = reader.readMapAsStringListValue(path);
         if (resultMap == null || resultMap.isEmpty()) {
             path = dataDirectory + "/not-trim-column.txt";
             resultMap = reader.readMapAsStringListValue(path);
         }
         final Set<Entry<String, List<String>>> entrySet = resultMap.entrySet();
         final StringKeyMap<List<String>> stringKeyMap = StringKeyMap.createAsFlexible();
         for (Entry<String, List<String>> entry : entrySet) {
             stringKeyMap.put(entry.getKey(), entry.getValue());
         }
         return stringKeyMap;
     }
 
     private Map<String, List<String>> getEmptyStringTableColumnMap(String dataDirectory) {
         final DfMapStringFileReader reader = new DfMapStringFileReader();
         String path = dataDirectory + "/emptyStringColumnMap.dataprop";
         Map<String, List<String>> resultMap = reader.readMapAsStringListValue(path);
         if (resultMap == null || resultMap.isEmpty()) {
             path = dataDirectory + "/empty-string-column.txt";
             resultMap = reader.readMapAsStringListValue(path);
         }
         final Set<Entry<String, List<String>>> entrySet = resultMap.entrySet();
         final StringKeyMap<List<String>> stringKeyMap = StringKeyMap.createAsFlexible();
         for (Entry<String, List<String>> entry : entrySet) {
             stringKeyMap.put(entry.getKey(), entry.getValue());
         }
         return stringKeyMap;
     }
 
     protected String getSql4Log(String tableName, List<String> columnNameList,
             final List<? extends Object> bindParameters) {
         String bindParameterString = bindParameters.toString();
         bindParameterString = bindParameterString.substring(1, bindParameterString.length() - 1);
         return tableName + ":{" + bindParameterString + "}";
     }
 
     protected ColumnContainer createColumnContainer(final DfDataTable dataTable, final DfDataRow dataRow) {
         final ColumnContainer container = new ColumnContainer();
         for (int i = 0; i < dataTable.getColumnSize(); i++) {
             final DfDataColumn dataColumn = dataTable.getColumn(i);
             if (!dataColumn.isWritable()) {
                 continue;
             }
             final Object value = dataRow.getValue(i);
             final String columnName = dataColumn.getColumnDbName();
             container.addColumnValue(columnName, value);
             container.addColumnObject(columnName, dataColumn);
         }
         return container;
     }
 
     // ===================================================================================
     //                                                                        Helper Class
     //                                                                        ============
     protected static class ColumnContainer {
         protected Map<String, Object> columnValueMap = new LinkedHashMap<String, Object>();
         protected Map<String, DfDataColumn> columnObjectMap = new LinkedHashMap<String, DfDataColumn>();
 
         public Map<String, Object> getColumnValueMap() {
             return columnValueMap;
         }
 
         public void addColumnValue(String columnName, Object columnValue) {
             this.columnValueMap.put(columnName, columnValue);
         }
 
         public Map<String, DfDataColumn> getColumnObjectMap() {
             return columnObjectMap;
         }
 
         public void addColumnObject(String columnName, DfDataColumn columnObject) {
             this.columnObjectMap.put(columnName, columnObject);
         }
     }
 
     protected static class MyCreatedState {
         public String buildPreparedSql(final DfDataRow row) {
             final DfDtsCreatedState createdState = new DfDtsCreatedState() {
                 public String toString() {
                     final DfDtsSqlContext sqlContext = getSqlContext(row);
                     return sqlContext.getSql();
                 }
             };
             return createdState.toString();
         }
     }
 
     // ===================================================================================
     //                                                                            Accessor
     //                                                                            ========
     public void setSkipSheet(String skipSheet) {
         if (skipSheet == null || skipSheet.trim().length() == 0) {
             return;
         }
         try {
             _skipSheetPattern = Pattern.compile(skipSheet);
         } catch (PatternSyntaxException e) {
             String msg = "The pattern syntax for skip-sheet was wrong: " + skipSheet;
             throw new IllegalStateException(msg, e);
         }
     }
 }
