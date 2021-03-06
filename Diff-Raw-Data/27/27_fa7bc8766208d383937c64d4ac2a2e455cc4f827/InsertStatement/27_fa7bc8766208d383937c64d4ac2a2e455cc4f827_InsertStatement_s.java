 package liquibase.database.sql;
 
 import liquibase.database.Database;
 import liquibase.exception.StatementNotSupportedOnDatabaseException;
 
 import java.util.Date;
 import java.util.HashMap;
 import java.util.Map;
 
 public class InsertStatement implements SqlStatement {
     private String schemaName;
     private String tableName;
     private Map<String, Object> newColumnValues = new HashMap<String, Object>();
 
     public InsertStatement(String schemaName, String tableName) {
         this.schemaName = schemaName;
         this.tableName = tableName;
     }
 
     public String getSchemaName() {
         return schemaName;
     }
 
     public String getTableName() {
         return tableName;
     }
 
     public InsertStatement addColumnValue(String columnName, Object newValue) {
         newColumnValues.put(columnName, newValue);
 
         return this;
     }
 
     public Object getColumnValue(String columnName) {
         return newColumnValues.get(columnName);
     }
 
     public String getSqlStatement(Database database) throws StatementNotSupportedOnDatabaseException {
         if (getSchemaName() != null && !database.supportsSchemas()) {
             throw new StatementNotSupportedOnDatabaseException("Database does not support schemas", this, database);
         }
         StringBuffer sql = new StringBuffer("INSERT INTO " + database.escapeTableName(getSchemaName(), getTableName()) + " (");
         for (String column : newColumnValues.keySet()) {
             sql.append(column).append(", ");
         }
         sql.deleteCharAt(sql.lastIndexOf(" "));
         sql.deleteCharAt(sql.lastIndexOf(","));
 
         sql.append(") VALUES (");
 
         for (String column : newColumnValues.keySet()) {
             Object newValue = newColumnValues.get(column);
             if (newValue == null) {
                 sql.append("NULL");
             } else if (newValue instanceof String && database.shouldQuoteValue(((String) newValue))) {
                 sql.append("'").append(newValue).append("'");
             } else if (newValue instanceof Date) {
                 sql.append(database.getDateLiteral(((Date) newValue)));
             } else {
                 sql.append(newValue);
             }
             sql.append(", ");
         }
 
         sql.deleteCharAt(sql.lastIndexOf(" "));
         sql.deleteCharAt(sql.lastIndexOf(","));
 
         sql.append(")");
         return sql.toString();
     }
 
     public String getEndDelimiter(Database database) {
         return ";";
     }
 
     public boolean supportsDatabase(Database database) {
         return true;
     }
 }
