 /*
  * Helma License Notice
  *
  * The contents of this file are subject to the Helma License
  * Version 2.0 (the "License"). You may not use this file except in
  * compliance with the License. A copy of the License is available at
  * http://adele.helma.org/download/helma/license.txt
  *
  * Copyright 1998-2003 Helma Software. All Rights Reserved.
  *
  * $RCSfile: DbMapping.java,v $
  * $Author: hannes $
  * $Revision: 1.60 $
  * $Date: 2005/11/10 17:04:12 $
  */
 
 /* 
  * Modified by:
  * 
  * Axiom Software Inc., 11480 Commerce Park Drive, Third Floor, Reston, VA 20191 USA
  * email: info@axiomsoftwareinc.com
  */
 package axiom.objectmodel.db;
 
 
 import java.sql.*;
 import java.util.*;
 
 import axiom.framework.ErrorReporter;
 import axiom.framework.core.Application;
 import axiom.framework.core.Prototype;
 import axiom.util.ResourceProperties;
 
 /**
  * A DbMapping describes how a certain type of  Nodes is to mapped to a
  * relational database table. Basically it consists of a set of JavaScript property-to-
  * Database row bindings which are represented by instances of the Relation class.
  */
 public final class DbMapping {
     // DbMappings belong to an application
     protected Application app;
 
     // prototype name of this mapping
     private String typename;
 
     // properties from where the mapping is read
     private ResourceProperties props;
 
     // name of data dbSource to which this mapping writes
     private DbSource dbSource;
 
     // name of datasource
     private String dbSourceName;
 
     // name of db table
     private String tableName;
     private String[] tableNames;
     private String[] localKeys;
     private String[] foreignKeys;
     
     // list of properties to try for parent
     private ParentInfo[] parentInfo;
 
     // Relations describing subnodes and properties.
     protected Relation subRelation;
     protected Relation propRelation;
 
     // if this defines a subnode mapping with groupby layer,
     // we need a DbMapping for those groupby nodes
     private DbMapping groupbyMapping;
 
     // Map of property names to Relations objects
     private HashMap prop2db;
 
     // Map of db columns to Relations objects.
     // Case insensitive, keys are stored in upper case so
     // lookups must do a toUpperCase().
     private HashMap db2prop;
 
     // list of columns to fetch from db
     private DbColumn[] columns = null;
 
     // list of columns by table number
     private DbColumn[][] columnsByTable = null;
     
     // Map of db columns by name
     private HashMap columnMap;
 
     // Array of aggressively loaded references
     private Relation[] joins;
 
     // pre-rendered select statement
     private String selectString = null;
     private String insertString = null;
     private String updateString = null;
 
     // db field used as primary key
     private String idField;
 
     // db field used as object name
     private String nameField;
 
     // db field used to identify name of prototype to use for object instantiation
     private String protoField;
 
     // name of parent prototype, if any
     private String extendsProto;
 
     // dbmapping of parent prototype, if any
     private DbMapping parentMapping;
 
     // descriptor for key generation method
     private String idgen;
 
     // remember last key generated for this table
     private long lastID;
 
     // timestamp of last modification of the mapping (prototype.properties)
     // init value is -1 so we know we have to run update once even if
     // the underlying properties file is non-existent
     long lastTypeChange = -1;
 
     // timestamp of last modification of an object of this type
     long lastDataChange;
 
     // evict objects of this type when received via replication
     private boolean evictOnReplication;
 
     // Set of mappings that depend on us and should be forwarded last data change events
     HashSet dependentMappings = new HashSet();
     
     // set an (optional) timeout on the cache nodes
     protected long timeout;
     
     // the axiom db class that manages objects of this db mapping
     protected String dbClass;
         
     /**
      * Create an empty DbMapping
      */
     public DbMapping(Application app) {
         this.app = app;
         this.typename = null;
 
         prop2db = new HashMap();
         db2prop = new HashMap();
 
         parentInfo = null;
 
         idField = null;
     }
 
     /**
      * Create a DbMapping from a prototype.properties property file
      */
     public DbMapping(Application app, String typename, ResourceProperties props) {
         this.app = app;
         // create a unique instance of the string. This is useful so
         // we can compare types just by using == instead of equals.
         this.typename = typename == null ? null : typename.intern();
 
         prop2db = new HashMap();
         db2prop = new HashMap();
 
         columnMap = new HashMap();
         parentInfo = null;
         idField = null;
         this.props = props;
         readBasicProperties();
     }
 
     /**
      * Tell the type manager whether we need update() to be called
      */
     public boolean needsUpdate() {
         return props.lastModified() != lastTypeChange;
     }
 
     private void setupJoin() {
         int count = 0;
         ArrayList tables = new ArrayList();
         while (true) {
             String tbl = props.getProperty("_table."+count);
             String lcl = props.getProperty("_table."+count+".local");
             String fgn = props.getProperty("_table."+count+".foreign");
             if (tbl != null && lcl != null && fgn != null) {
                 tables.add(new String[] { tbl, lcl, fgn });
             } else {
                 break;
             }
             count++;
         }
         if (count > 0) {
             final int size = tables.size() + 1;
             tableNames = new String[size];
             localKeys = new String[size];
             foreignKeys = new String[size];
             tableNames[0] = tableName;
             localKeys[0] = idField;
             foreignKeys[0] = idField;
             StringBuffer sb = new StringBuffer(tableName);
             for (int i = 1; i < size; i++) {
                 String[] values = (String[]) tables.get(i-1);
                 tableNames[i] = values[0];
                 sb.append(",").append(values[0]);
                 localKeys[i] = values[1];
                 foreignKeys[i] = values[2];
             }
             tableName = sb.toString();
         }
     }
     
     /**
      * Read in basic properties and register dbmapping with the
      * dbsource.
      */
     private void readBasicProperties() {
         tableName = props.getProperty("_table");
         dbSourceName = props.getProperty("_db");
 		if (dbSourceName != null) {
             dbSource = app.getDbSource(dbSourceName);
 
             if (dbSource != null) {
             	dbClass = dbSource.getProperty("class", null);
             }
             if (dbSource == null) {
                 app.logEvent("*** Data Source for prototype " + typename +
                              " does not exist: " + dbSourceName);
                 app.logEvent("*** accessing or storing a " + typename +
                              " object will cause an error.");
             } else if (tableName == null && dbSource.getType().equalsIgnoreCase("relational")) {
                 app.logEvent("*** No table name specified for prototype " + typename);
                 app.logEvent("*** accessing or storing a " + typename +
                         " object will cause an error.");
 
                 // mark mapping as invalid by nulling the dbSource field
                 dbSource = null;
             } else {
                 // dbSource and tableName not null - register this instance
                 dbSource.registerDbMapping(this);
             }
         }
     }
     
     /**
      * Read the mapping from the Properties. Return true if the properties were changed.
      * The read is split in two, this method and the rewire method. The reason is that in order
      * for rewire to work, all other db mappings must have been initialized and registered.
      */
     public String getDbField(String f) {
     	if (f.indexOf(".") > -1) {
     		String[] a = f.split("\\.");
     		return a[1];
     	}
     	return f;
     }
     
     public synchronized void update() {
         // read in properties
         readBasicProperties();
         idgen = props.getProperty("_idgen");
         // if id field is null, we assume "ID" as default. We don't set it
         // however, so that if null we check the parent prototype first.
         idField = props.getProperty("_id");
         if (idField == null) { idField = "id"; }
         nameField = props.getProperty("_name");
         protoField = props.getProperty("_prototype");
         
         setupJoin(); 
         
         evictOnReplication = "true".equals(props.getProperty("_evictOnReplication"));
 
         String parentSpec = props.getProperty("_parent");
 
         if (parentSpec != null) {
             // comma-separated list of properties to be used as parent
             StringTokenizer st = new StringTokenizer(parentSpec, ",;");
 
             parentInfo = new ParentInfo[st.countTokens()];
 
             for (int i = 0; i < parentInfo.length; i++)
                 parentInfo[i] = new ParentInfo(st.nextToken().trim());
         } else {
             parentInfo = null;
         }
 
         lastTypeChange = props.lastModified();
 
         // see if this prototype extends (inherits from) any other prototype
         extendsProto = props.getProperty("_extends");
         // always inherit from AxiomObject 
         if (extendsProto == null && !this.isRelational() 
                 && !"AxiomObject".equals(this.typename)) {
             extendsProto = "AxiomObject"; 
         }
 
         if (extendsProto != null) {
             parentMapping = app.getDbMapping(extendsProto);
             if (parentMapping != null) {
                 if (parentMapping.needsUpdate()) {
                     parentMapping.update();
                 }
                 // if tableName or DbSource are inherited from the parent mapping
                 // set them to null so we are aware of the fact.
                 if (tableName != null &&
                         tableName.equals(parentMapping.getTableName())) {
                     tableName = null;
                 }
                 if (dbSourceName != null &&
                         dbSourceName.equals(parentMapping.getDbSourceName())) {
                     dbSourceName = null;
                     dbSource = null;
                 }
             }
         } else {
             parentMapping = null;
         }
 
         // set the parent prototype in the corresponding Prototype object!
         // this was previously done by TypeManager, but we need to do it
         // ourself because DbMapping.update() may be called by other code than
         // the TypeManager.
         if (typename != null &&
                 !"global".equalsIgnoreCase(typename) &&
                 !"axiomobject".equalsIgnoreCase(typename)) {
             Prototype proto = app.getPrototypeByName(typename);
             if (proto != null) {
                 if (extendsProto != null) {
                     proto.setParentPrototype(app.getPrototypeByName(extendsProto));
                 } else if (!app.isJavaPrototype(typename)) {
                     proto.setParentPrototype(app.getPrototypeByName("axiomobject"));
                 }
             }
         }
 
         // null the cached columns and select string
         columns = null;
         columnsByTable = null; 
         columnMap.clear();
         selectString = insertString = updateString = null;
 
         HashMap p2d = new HashMap();
         HashMap d2p = new HashMap();
         ArrayList joinList = new ArrayList();
 
         for (Enumeration e = props.keys(); e.hasMoreElements();) {
             String propName = (String) e.nextElement();
             try {
                 // ignore internal properties (starting with "_") and sub-options (containing a ".")
                 if (!propName.startsWith("_") && (propName.indexOf(".") < 0)) {
                     String dbField = props.getProperty(propName);
                     //dbField = getDbField(dbField);
                     // check if a relation for this propery already exists. If so, reuse it
                     Relation rel = (Relation) prop2db.get(propName.toLowerCase());
 
                     if (rel == null) {
                         rel = new Relation(propName, this);
                     }
 
 //                  rel.update(dbField, props);
                     rel.update(props);
 
                     // key enumerations from SystemProperties are all lower case, which is why
                     // even though we don't do a toLowerCase() here,
                     // we have to when we lookup things in p2d later.
                     p2d.put(propName, rel);
 
                     if ((rel.columnName != null) &&
                             ((rel.reftype == Relation.PRIMITIVE) ||
                             (rel.reftype == Relation.REFERENCE))) {
                         Relation old = (Relation) d2p.put(rel.columnName.toUpperCase(), rel);
                         // check if we're overwriting another relation
                         // if so, primitive relations get precendence to references
                         if (old != null) {
                             app.logEvent("*** Duplicate mapping for "+typename+"."+rel.columnName);
                             if (old.reftype == Relation.PRIMITIVE) {
                                 d2p.put(old.columnName.toUpperCase(), old);
                             }
                         }
                     }
 
                     // check if a reference is aggressively fetched
                     if ((rel.reftype == Relation.REFERENCE ||
                              rel.reftype == Relation.COMPLEX_REFERENCE) &&
                              rel.aggressiveLoading) {
                         joinList.add(rel);
                     }
                 }
             } catch (Exception x) {
                 app.logError(ErrorReporter.errorMsg(this.getClass(), "update"), x);
             }
         }
 
         prop2db = p2d;
         db2prop = d2p;
 
         joins = new Relation[joinList.size()];
         joins = (Relation[]) joinList.toArray(joins);
 
         String subnodeMapping = props.getProperty("_children");
 
         if (subnodeMapping != null) {
             try {
                 // check if subnode relation already exists. If so, reuse it
                 if (subRelation == null) {
                     subRelation = new Relation("_children", this);
                 }
 
 //              subRelation.update(subnodeMapping, props);
                 subRelation.update(props);
 
                 // if subnodes are accessed via access name or group name,
                 // the subnode relation is also the property relation.
                 if ((subRelation.accessName != null) || (subRelation.groupby != null)) {
                     propRelation = subRelation;
                 } else {
                     propRelation = null;
                 }
             } catch (Exception x) {
                 app.logError(ErrorReporter.errorMsg(this.getClass(), "update") 
                 		+ "Error reading _subnodes relation for " + typename, x);
 
                 // subRelation = null;
             }
         } else {
             subRelation = propRelation = null;
         }
 
         if (groupbyMapping != null) {
             initGroupbyMapping();
             groupbyMapping.lastTypeChange = this.lastTypeChange;
         }
         
         try {
             String v = props.getProperty("_cache.timeout");
             if (v != null) {
                 timeout = Long.parseLong(v);
                 timeout *= 1000L;
             } else {
                 timeout = -1L;
             }
         } catch (Exception ex) {
             timeout = -1L;
         }
     }
 
     /**
      * Method in interface Updatable.
      */
     public void remove() {
         // do nothing, removing of type properties is not implemented.
     }
 
     /**
      * Get a JDBC connection for this DbMapping.
      */
     public Connection getConnection() throws ClassNotFoundException, SQLException {
         if (dbSourceName == null) {
             if (parentMapping != null) {
                 return parentMapping.getConnection();
             } else {
                 throw new SQLException("Tried to get Connection from non-relational embedded data source.");
             }
         }
 
         if (tableName == null) {
             throw new SQLException("Invalid DbMapping, _table not specified: " + this);
         }
         
         // if dbSource was previously not available, check again
         if (dbSource == null) {
             dbSource = app.getDbSource(dbSourceName);
         }
 
         if (dbSource == null) {
             throw new SQLException("Datasource not defined or unable to load driver: " + dbSourceName + ".");
         }
 
         return dbSource.getConnection();
     }
 
     /**
      * Get the DbSource object for this DbMapping. The DbSource describes a JDBC
      * data source including URL, JDBC driver, username and password.
      */
     public DbSource getDbSource() {
         if (dbSource == null) {
             if ((tableName != null) && (dbSourceName != null)) {
                 dbSource = app.getDbSource(dbSourceName);
             } else if (parentMapping != null) {
                 return parentMapping.getDbSource();
             }
         }
 
         return dbSource;
     }
 
     /**
      * Get the dbsource name used for this type mapping.
      */
     public String getDbSourceName() {
         if ((dbSourceName == null) && (parentMapping != null)) {
             return parentMapping.getDbSourceName();
         }
 
         return dbSourceName;
     }
     
     public String getTableJoinClause(int primary) {
         /** TODO: This is invalid for now, multi-table needs to be fixed anyway */
     	if (getTableCount() > 1) {
             StringBuffer sb = new StringBuffer();
     		
     		if (primary == 1) { 
                 sb.append(" WHERE "); 
             } else {
                 sb.append(" AND ");
             }
             
     		for (int i = 1; i < tableNames.length; i++) {
     			if (i > 1) { 
                     sb.append(" AND "); 
                 }
         		sb.append(idField);
                 sb.append(" = ");
                 sb.append(idField);
     		}
             
     		return sb.toString();
     	}
         
     	return "";
     }
     
     public int getTableCount() {
     	if (tableNames != null) { 
             return tableNames.length; 
         }
         
     	return 1;    	
     }
 
     public String getTableDeleteProperties() {
     	if (getTableCount() > 1) {
     		StringBuffer v = new StringBuffer();
     		for (int i = 0; i < tableNames.length; i++) {
     			if (i > 0) { v.append(","); }
         		v.append(tableNames[i]).append(".*");    			
     		}
     		v.append(" ");
     		return v.toString();
     	}
         
     	return "";
     }
     
     /**
      * Get the table name used for this type mapping.
      */
     public String getTableName() {
         if ((tableName == null) && (parentMapping != null)) {
             return parentMapping.getTableName();
         }
 
         return tableName;
     }
 
     public String getTableName(int index) {
         if ((tableNames == null) && (parentMapping != null)) {
             return parentMapping.getTableName(index);
         } else if (tableNames == null) { return getTableName(); }
 
         return tableNames[index];
     }
     
     /**
      * Get the application this DbMapping belongs to.
      */
     public Application getApplication() {
         return app;
     }
 
     /**
      * Get the name of this mapping's application
      */
     public String getAppName() {
         return app.getName();
     }
 
     /**
      * Get the name of the object type this DbMapping belongs to.
      */
     public String getTypeName() {
         return typename;
     }
 
     /**
      * Get the name of this type's parent type, if any.
      */
     public String getExtends() {
         return extendsProto;
     }
 
     /**
      * Get the primary key column name for objects using this mapping.
      */
     public String getIDField() {        
         if ((idField == null) && (parentMapping != null)) {
             return parentMapping.getIDField();
         }
 
         return (idField == null) ? "ID" : idField;
     }
 
     /**
      * Get the column used for (internal) names of objects of this type.
      */
     public String getNameField() {
         if ((nameField == null) && (parentMapping != null)) {
             return parentMapping.getNameField();
         }
 
         return nameField;
     }
 
     /**
      * Get the column used for names of prototype.
      */
     public String getPrototypeField() {
         if ((protoField == null) && (parentMapping != null)) {
             return parentMapping.getPrototypeField();
         }
 
         return protoField;
     }
 
     /**
      * Should objects of this type be evicted/discarded/reloaded when received via
      * cache replication?
      */
     public boolean evictOnReplication() {
         return evictOnReplication;
     }
 
     /**
      * Translate a database column name to an object property name according to this mapping.
      */
     public String columnNameToProperty(String columnName) {
         if (columnName == null) {
             return null;
         }
 
         // SEMIHACK: If columnName is a function call, try to extract actual
         // column name from it
         int open = columnName.indexOf('(');
         int close = columnName.indexOf(')');
         if (open > -1 && close > open) {
             columnName = columnName.substring(open + 1, close);
         }
 
         return _columnNameToProperty(columnName.toUpperCase());
     }
 
     private String _columnNameToProperty(final String columnName) {
         Relation rel = (Relation) db2prop.get(columnName);
         
         if ((rel == null) && (parentMapping != null)) {
             return parentMapping._columnNameToProperty(columnName);
         }
 
         if ((rel != null) &&
                 ((rel.reftype == Relation.PRIMITIVE) ||
                 (rel.reftype == Relation.REFERENCE))) {
             return rel.propName;
         }
         
         return null;
     }
 
     /**
      * Translate an object property name to a database column name according to this mapping.
      */
     public String propertyToColumnName(String propName) {
         if (propName == null) {
             return null;
         }
 
         // FIXME: prop2db stores keys in lower case, because it gets them
         // from a SystemProperties object which converts keys to lower case.
         return _propertyToColumnName(propName.toLowerCase());
     }
 
     private String _propertyToColumnName(final String propName) {
         Relation rel = (Relation) prop2db.get(propName);
 
         if ((rel == null) && (parentMapping != null)) {
             return parentMapping._propertyToColumnName(propName);
         }
 
         if ((rel != null) &&
                 ((rel.reftype == Relation.PRIMITIVE) ||
                 (rel.reftype == Relation.REFERENCE))) {
             return rel.columnName;
         }
 
         return null;
     }
 
     /**
      * Translate a database column name to an object property name according to this mapping.
      */
     public Relation columnNameToRelation(String columnName) {
         if (columnName == null) {
             return null;
         }
 
         return _columnNameToRelation(columnName.toUpperCase());
     }
 
     private Relation _columnNameToRelation(final String columnName) {
         Relation rel = (Relation) db2prop.get(columnName);
 
         if ((rel == null) && (parentMapping != null)) {
             return parentMapping._columnNameToRelation(columnName);
         }
 
         return rel;
     }
 
     /**
      * Translate an object property name to a database column name according to this mapping.
      */
     public Relation propertyToRelation(String propName) {
         if (propName == null) {
             return null;
         }
 
         // FIXME: prop2db stores keys in lower case, because it gets them
         // from a SystemProperties object which converts keys to lower case.
         return _propertyToRelation(propName.toLowerCase());
     }
 
     private Relation _propertyToRelation(String propName) {
         Relation rel = (Relation) prop2db.get(propName);
 
         if ((rel == null) && (parentMapping != null)) {
             return parentMapping._propertyToRelation(propName);
         }
 
         return rel;
     }
 
     /**
      * This returns the parent info array, which tells an object of this type how to
      * determine its parent object.
      */
     public synchronized ParentInfo[] getParentInfo() {
         if ((parentInfo == null) && (parentMapping != null)) {
             return parentMapping.getParentInfo();
         }
 
         return parentInfo;
     }
 
     /**
      *
      *
      * @return ...
      */
     public DbMapping getSubnodeMapping() {
         if (subRelation != null) {
             return subRelation.otherType;
         }
 
         if (parentMapping != null) {
             return parentMapping.getSubnodeMapping();
         }
 
         return null;
     }
 
     /**
      *
      *
      * @param propname ...
      *
      * @return ...
      */
     public DbMapping getExactPropertyMapping(String propname) {
         Relation rel = getExactPropertyRelation(propname);
 
         if (rel != null) {
             // if this is a virtual node, it doesn't have a dbmapping
             if (rel.virtual && (rel.prototype == null)) {
                 return null;
             } else {
                 return rel.otherType;
             }
         }
 
         return null;
     }
 
     /**
      *
      *
      * @param propname ...
      *
      * @return ...
      */
     public DbMapping getPropertyMapping(String propname) {
         Relation rel = getPropertyRelation(propname);
 
         if (rel != null) {
             // if this is a virtual node, it doesn't have a dbmapping
             if (rel.virtual && (rel.prototype == null)) {
                 return null;
             } else {
                 return rel.otherType;
             }
         }
 
         return null;
     }
 
     /**
      * If subnodes are grouped by one of their properties, return the
      * db-mapping with the right relations to create the group-by nodes
      */
     public synchronized DbMapping getGroupbyMapping() {
         if ((subRelation == null) || (subRelation.groupby == null)) {
             return null;
         }
 
         if (groupbyMapping == null) {
             initGroupbyMapping();
         }
 
         return groupbyMapping;
     }
 
     /**
      * Initialize the dbmapping used for group-by nodes.
      */
     private void initGroupbyMapping() {
         // if a prototype is defined for groupby nodes, use that
         // if mapping doesn' exist or isn't defined, create a new (anonymous internal) one
         groupbyMapping = new DbMapping(app);
 
         // If a mapping is defined, make the internal mapping inherit from
         // the defined named prototype.
         if (subRelation.groupbyPrototype != null) {
             groupbyMapping.parentMapping = app.getDbMapping(subRelation.groupbyPrototype);
         }
 
         groupbyMapping.subRelation = subRelation.getGroupbySubnodeRelation();
 
         if (propRelation != null) {
             groupbyMapping.propRelation = propRelation.getGroupbyPropertyRelation();
         } else {
             groupbyMapping.propRelation = subRelation.getGroupbyPropertyRelation();
         }
 
         groupbyMapping.typename = subRelation.groupbyPrototype;
     }
 
     /**
      *
      *
      * @param rel ...
      */
     public void setPropertyRelation(Relation rel) {
         propRelation = rel;
     }
 
     /**
      *
      *
      * @return ...
      */
     public Relation getSubnodeRelation() {
         if ((subRelation == null) && (parentMapping != null)) {
             return parentMapping.getSubnodeRelation();
         }
 
         return subRelation;
     }
 
     /**
      * Return the list of defined property names as String array.
      */
     public String[] getPropertyNames() {
         return (String[]) prop2db.keySet().toArray(new String[prop2db.size()]);
     }
 
     public Set getPropertyColumns() {    	
     	Set pr = props.keySet();
     	Object[] p = pr.toArray();
     	for (int i=0;i<p.length;i++) {
             String prop = p[i].toString();
             if (((prop.indexOf("_")) == 0 && !(prop.equals("_id"))) || (prop.indexOf(".") > -1)) {
     			pr.remove(p[i]);
     		} else {
     		    String v = props.getProperty(prop);
                 if (v == null || v.trim().equals("")) {
                     pr.remove(p[i]);
                 }
             }
     	}    	
     	return pr;
     }
     
     /**
      *
      *
      * @return ...
      */
     private Relation getPropertyRelation() {
         if ((propRelation == null) && (parentMapping != null)) {
             return parentMapping.getPropertyRelation();
         }
 
         return propRelation;
     }
 
     /**
      *
      *
      * @param propname ...
      *
      * @return ...
      */
     public Relation getPropertyRelation(String propname) {
         if (propname == null) {
             return getPropertyRelation();
         }
 
         // first try finding an exact match for the property name
         Relation rel = getExactPropertyRelation(propname);
 
         // if not defined, return the generic property mapping
         if (rel == null) {
             rel = getPropertyRelation();
         }
 
         return rel;
     }
 
     /**
      *
      *
      * @param propname ...
      *
      * @return ...
      */
     public Relation getExactPropertyRelation(String propname) {
         if (propname == null) {
             return null;
         }
         propname = app.isPropertyFilesIgnoreCase() ? propname.toLowerCase() : propname; 
 
         Relation rel = (Relation) prop2db.get(propname);
 
         if ((rel == null) && (parentMapping != null)) {
             rel = parentMapping.getExactPropertyRelation(propname);
         }
 
         return rel;
     }
 
     /**
      *
      *
      * @return ...
      */
     public String getSubnodeGroupby() {
         if ((subRelation == null) && (parentMapping != null)) {
             return parentMapping.getSubnodeGroupby();
         }
 
         return (subRelation == null) ? null : subRelation.groupby;
     }
 
     /**
      *
      *
      * @return ...
      */
     public String getIDgen() {
         if ((idgen == null) && (parentMapping != null)) {
             return parentMapping.getIDgen();
         }
 
         return idgen;
     }
 
     /**
      *
      *
      * @return ...
      */
     public WrappedNodeManager getWrappedNodeManager() {
         if (app == null) {
             throw new RuntimeException("Can't get node manager from internal db mapping");
         }
 
         return app.getWrappedNodeManager();
     }
 
     /**
      *  Tell whether this data mapping maps to a relational database table. This returns true
      *  if a datasource is specified, even if it is not a valid one. Otherwise, objects with invalid
      *  mappings would be stored in the embedded db instead of an error being thrown, which is
      *  not what we want.
      */
     public boolean isRelational() {
         if (dbSourceName != null && this.getType().toUpperCase().equals("RELATIONAL")) {
             return true;
         }
 
         if (parentMapping != null) {
             return parentMapping.isRelational();
         }
 
         return false;
     }
         
     public synchronized DbColumn[] getColumns()
     throws ClassNotFoundException, SQLException {
     	return getColumns(-1);
     }
     /**
      * Return an array of DbColumns for the relational table mapped by this DbMapping.
     * Some reworking done to accommodate for the columnsByTable field
      */
     public synchronized DbColumn[] getColumns(int tableNumber)
                                        throws ClassNotFoundException, SQLException {
         if (!isRelational()) {
             throw new SQLException("Can't get columns for non-relational data mapping " +
                                    this);
         }
         
         if (tableNumber == -1 && this.columns != null) {
             return this.columns;
         } else if (tableNumber > -1 && this.columnsByTable != null 
                 && tableNumber < this.columnsByTable.length 
                 && this.columnsByTable[tableNumber] != null) {
             return this.columnsByTable[tableNumber];
         }
         
         DbColumn[] columns = null;
         // Use local variable cols to avoid synchronization (schema may be nulled elsewhere)
         // we do two things here: set the SQL type on the Relation mappings
         // and build a string of column names.
         Statement stmt = null;
         try {
             Connection con = getConnection();
             stmt = con.createStatement();
             String table = null;
             if (tableNumber == -1) {
                 table = getTableName();
             } else {
                 table = getTableName(tableNumber);
             }
             
             if (table == null) {
                 throw new SQLException("Table name is null in getColumns() for " + this);
             }
             String scs = "*";
             if (tableNumber == -1) { scs = getSelectColumnsString(); } 
             
             ResultSet rs = stmt.executeQuery(new StringBuffer("SELECT ").append(scs).append(" FROM ").append(table)
                     .append(" WHERE 1 = 0")
                     .toString());
             if (rs == null) {
                 throw new SQLException("Error retrieving columns for " + this);
             }
             
             ResultSetMetaData meta = rs.getMetaData();
             
             // ok, we have the meta data, now loop through mapping...
             int ncols = meta.getColumnCount();            
             ArrayList list = new ArrayList(ncols);
             for (int i = 0; i < ncols; i++) {
                 String colName = meta.getColumnName(i + 1);
                 Relation rel = columnNameToRelation(colName);
                 DbColumn col = new DbColumn(colName, meta.getColumnType(i + 1), rel, this, tableNumber); 
                 list.add(col);
             }
             columns = new DbColumn[list.size()];
             columns = (DbColumn[]) list.toArray(columns);
             
             if (tableNumber == -1) {
                 this.columns = columns;
             } else {
                 if (this.columnsByTable == null && this.tableNames != null) {
                     this.columnsByTable = new DbColumn[this.tableNames.length][];
                 }
                 if (this.columnsByTable != null && tableNumber < this.columnsByTable.length) {
                     this.columnsByTable[tableNumber] = columns;
                 }
             }
         } finally {
             if (stmt != null) { 
                 try { stmt.close(); } catch (SQLException ignore) { }
                 stmt = null;
             }
         }
         
         return columns;
     }
 
     /**
      *  Return the array of relations that are fetched with objects of this type.
      */
     public Relation[] getJoins() {
         return joins;
     }
 
     /**
      *
      *
      * @param columnName ...
      *
      * @return ...
      *
      * @throws ClassNotFoundException ...
      * @throws SQLException ...
      */
     public DbColumn getColumn(String columnName)
                        throws ClassNotFoundException, SQLException {
 
         DbColumn col = (DbColumn) columnMap.get(columnName);
 
         if (col == null) {
             DbColumn[] cols = columns;
 
             if (cols == null) {
                 cols = getColumns();
             }
 
             for (int i = 0; i < cols.length; i++) {
                 if (columnName.equalsIgnoreCase(cols[i].getName())) {
                     col = cols[i];
 
                     break;
                 }
             }
 
             columnMap.put(columnName, col);
         }
 
         return col;
     }
 
     public String getSelectColumnsString() {
     	Object[] cols = getPropertyColumns().toArray();
     	StringBuffer sb = new StringBuffer();
     	String c = "";
     	for (int i = 0; i < cols.length; i++) {
     		c = props.getProperty(cols[i].toString());
     		if (c.matches("[\\w.]*")) {
     			if (sb.length() > 0) { sb.append(", "); }
     			sb.append(c);
     		}
     	}
     	return sb.toString();
     }
   
     /**
      *  Get a StringBuffer initialized to the first part of the select statement
      *  for objects defined by this DbMapping
      *
      * @param rel the Relation we use to select. Currently only used for optimizer hints.
      *            Is null if selecting by primary key.
      * @return the StringBuffer containing the first part of the select query
      */
     public StringBuffer getSelect(Relation rel) {
         // assign to local variable first so we are thread safe
         // (selectString may be reset by other threads)
         String sel = selectString;
         boolean isOracle = isOracle();
 
         if (rel == null && sel != null) {
             return new StringBuffer(sel);
         }
 
         StringBuffer s = new StringBuffer("SELECT ");
 
         if (rel != null && rel.queryHints != null) {
             s.append(rel.queryHints).append(" ");
         }
 
         String table = getTableName();
 
         // all columns from the main table
         //s.append(table);
         //s.append(".*");
         //s.append("*");
         s.append(getSelectColumnsString());
 
         for (int i = 0; i < joins.length; i++) {
             if (!joins[i].otherType.isRelational()) {
                 continue;
             }
             s.append(", ");
             s.append(Relation.JOIN_PREFIX);
             s.append(joins[i].propName);
             s.append(".*");
         }
 
         s.append(" FROM ");
 
         s.append(table);
 
         if (rel != null) {
             rel.appendAdditionalTables(s);
         }
 
         s.append(" ");
 
         for (int i = 0; i < joins.length; i++) {
             if (!joins[i].otherType.isRelational()) {
                 continue;
             }
             if (isOracle) {
                 // generate an old-style oracle left join - see
                 // http://www.praetoriate.com/oracle_tips_outer_joins.htm
                 s.append(", ");
                 s.append(joins[i].otherType.getTableName());
                 s.append(" ");
                 s.append(Relation.JOIN_PREFIX);
                 s.append(joins[i].propName);
                 s.append(" ");
             } else {
                 s.append("LEFT OUTER JOIN ");
                 s.append(joins[i].otherType.getTableName());
                 s.append(" ");
                 s.append(Relation.JOIN_PREFIX);
                 s.append(joins[i].propName);
                 s.append(" ON ");
                 joins[i].renderJoinConstraints(s, isOracle);
             }
         }
 
         // cache rendered string for later calls, but only if it wasn't
         // built for a particular Relation
         if (rel == null) {
             selectString = s.toString();
         }
 
         return s;
     }
     
     public StringBuffer getSelectCount(Relation rel) {
         boolean isOracle = isOracle();
 
         StringBuffer s = new StringBuffer("SELECT COUNT(*) ");
 
         if (rel != null && rel.queryHints != null) {
             s.append(rel.queryHints).append(" ");
         }
 
         String table = getTableName();
 
         // all columns from the main table
         //s.append(table);
         //s.append(".*");
         //s.append("*");
         s.append(getSelectColumnsString());
 
         for (int i = 0; i < joins.length; i++) {
             if (!joins[i].otherType.isRelational()) {
                 continue;
             }
             s.append(", ");
             s.append(Relation.JOIN_PREFIX);
             s.append(joins[i].propName);
             s.append(".*");
         }
 
         s.append(" FROM ");
 
         s.append(table);
 
         if (rel != null) {
             rel.appendAdditionalTables(s);
         }
 
         s.append(" ");
 
         for (int i = 0; i < joins.length; i++) {
             if (!joins[i].otherType.isRelational()) {
                 continue;
             }
             if (isOracle) {
                 // generate an old-style oracle left join - see
                 // http://www.praetoriate.com/oracle_tips_outer_joins.htm
                 s.append(", ");
                 s.append(joins[i].otherType.getTableName());
                 s.append(" ");
                 s.append(Relation.JOIN_PREFIX);
                 s.append(joins[i].propName);
                 s.append(" ");
             } else {
                 s.append("LEFT OUTER JOIN ");
                 s.append(joins[i].otherType.getTableName());
                 s.append(" ");
                 s.append(Relation.JOIN_PREFIX);
                 s.append(joins[i].propName);
                 s.append(" ON ");
                 joins[i].renderJoinConstraints(s, isOracle);
             }
         }
 
         return s;
     }
 
     /**
      *
      *
      * @return ...
      */
     public String getInsert(int tableNumber) throws ClassNotFoundException, SQLException {
         String ins = insertString;
 
         // we do NOT want to cache the insert string because it depends on table number
         /*if (ins != null) {
             return ins;
         }*/
 
         StringBuffer b1 = new StringBuffer("INSERT INTO ");
         // commented out invalid for now, multi-table needs to be fixed
         //if (tableNumber == -1) { b1.append(getTableName()); } 
         //else { b1.append(getTableName(tableNumber)); }    
         b1.append(getTableName());
         b1.append(" ( ");
         String idfield = getTableKey(tableNumber);
         int idx;
         if ((idx = idfield.indexOf(".")) > -1) {
             idfield = idfield.substring(idx+1);
         }
         b1.append(idfield); 
 
         StringBuffer b2 = new StringBuffer(" ) VALUES ( ?");
 
         DbColumn[] cols = getColumns(tableNumber);
 
         for (int i = 0; i < cols.length; i++) {
             Relation rel = cols[i].getRelation();
             String name = cols[i].getName();
             if (name.equals(idField)) { continue; } // We don't need this twice!
             if (((rel != null) && (rel.isPrimitive() ||
                     rel.isReference())) ||
                     name.equalsIgnoreCase(getNameField()) ||
                     name.equalsIgnoreCase(getPrototypeField())) {
                 b1.append(", ").append(cols[i].getName());
                 b2.append(", ?");
             }
         }
 
         b1.append(b2.toString());
         b1.append(" )");
 
         // cache rendered string for later calls.
         ins = insertString = b1.toString();
 
         return ins;
     }
 
 
     /**
      *
      *
      * @return ...
      */
     public StringBuffer getUpdate() {
         String upd = updateString;
 
         if (upd != null) {
             return new StringBuffer(upd);
         }
 
         StringBuffer s = new StringBuffer("UPDATE ");
 
         s.append(getTableName());
         s.append(" SET ");
 
         // cache rendered string for later calls.
         updateString = s.toString();
 
         return s;
     }
 
     /**
      *  Return true if values for the column identified by the parameter need
      *  to be quoted in SQL queries.
      */
     public boolean needsQuotes(String columnName) throws SQLException {
         if ((tableName == null) && (parentMapping != null)) {
             return parentMapping.needsQuotes(columnName);
         }
 
         try {
             DbColumn col = getColumn(columnName);
 
             // This is not a mapped column. In case of doubt, add quotes.
             if (col == null) {
                 return true;
             }
 
             switch (col.getType()) {
                 case Types.CHAR:
                 case Types.VARCHAR:
                 case Types.LONGVARCHAR:
                 case Types.BINARY:
                 case Types.VARBINARY:
                 case Types.LONGVARBINARY:
                 case Types.DATE:
                 case Types.TIME:
                 case Types.TIMESTAMP:
                     return true;
 
                 default:
                     return false;
             }
         } catch (Exception x) {
             throw new SQLException(x.getMessage());
         }
     }
 
     /**
      * Add constraints to select query string to join object references
      */
     public void addJoinConstraints(StringBuffer s, String pre) {
         boolean isOracle = isOracle();
         String prefix = pre;
 
         if (!isOracle) {
             // constraints have already been rendered by getSelect()
             return;
         }
 
         for (int i = 0; i < joins.length; i++) {
             if (!joins[i].otherType.isRelational()) {
                 continue;
             }
             s.append(prefix);
             joins[i].renderJoinConstraints(s, isOracle);
             prefix = " AND ";
         }
     }
 
     /**
      * Is the database behind this an Oracle db?
      *
      * @return true if the dbsource is using an oracle JDBC driver
      */
     public boolean isOracle() {
         if (dbSource != null) {
             return dbSource.isOracle();
         }
         if (parentMapping != null) {
             return parentMapping.isOracle();
         }
         return false;
     }
 
     /**
      * Return a string representation for this DbMapping
      *
      * @return a string representation
      */
     public String toString() {
         if (typename == null) {
             return "[unspecified internal DbMapping]";
         } else {
             return ("[" + app.getName() + "." + typename + "]");
         }
     }
 
     /**
      * Get the last time something changed in the Mapping
      *
      * @return time of last mapping change
      */
     public long getLastTypeChange() {
         return lastTypeChange;
     }
 
     /**
      * Get the last time something changed in our data
      *
      * @return time of last data change
      */
     public long getLastDataChange() {
         // refer to parent mapping if it uses the same db/table
         if (inheritsStorage()) {
             return parentMapping.getLastDataChange();
         } else {
             return lastDataChange;
         }
     }
 
     /**
      * Set the last time something changed in the data, propagating the event
      * to mappings that depend on us through an additionalTables switch.
      */
     public void setLastDataChange(long t) {
         // forward data change timestamp to storage-compatible parent mapping
         if (inheritsStorage()) {
             parentMapping.setLastDataChange(t);
         } else {
             lastDataChange = t;
             // propagate data change timestamp to mappings that depend on us
             if (!dependentMappings.isEmpty()) {
                 Iterator it = dependentMappings.iterator();
                 while(it.hasNext()) {
                     DbMapping dbmap = (DbMapping) it.next();
                     dbmap.setIndirectDataChange(t);
                 }
             }
         }
     }
 
     /**
      * Set the last time something changed in the data. This is already an indirect
      * data change triggered by a mapping we depend on, so we don't propagate it to
      * mappings that depend on us through an additionalTables switch.
      */
     protected void setIndirectDataChange(long t) {
         // forward data change timestamp to storage-compatible parent mapping
         if (inheritsStorage()) {
             parentMapping.setIndirectDataChange(t);
         } else {
             lastDataChange = t;
         }
     }
 
     /**
      * Helper method to generate a new ID. This is only used in the special case
      * when using the select(max) method and the underlying table is still empty.
      *
      * @param dbmax the maximum value already stored in db
      * @return a new and hopefully unique id
      */
     protected synchronized long getNewID(long dbmax) {
         // refer to parent mapping if it uses the same db/table
         if (inheritsStorage()) {
             return parentMapping.getNewID(dbmax);
         } else {
             lastID = Math.max(dbmax + 1, lastID + 1);
             return lastID;
         }
     }
 
     /**
      * Return an enumeration of all properties defined by this db mapping.
      *
      * @return the property enumeration
      */
     public Enumeration getPropertyEnumeration() {
         HashSet set = new HashSet();
 
         collectPropertyNames(set);
 
         final Iterator it = set.iterator();
 
         return new Enumeration() {
                 public boolean hasMoreElements() {
                     return it.hasNext();
                 }
 
                 public Object nextElement() {
                     return it.next();
                 }
             };
     }
 
     /**
      * Collect a set of all properties defined by this db mapping
      *
      * @param basket the set to put properties into
      */
     private void collectPropertyNames(HashSet basket) {
         // fetch propnames from parent mapping first, than add our own.
         if (parentMapping != null) {
             parentMapping.collectPropertyNames(basket);
         }
 
         if (!prop2db.isEmpty()) {
             basket.addAll(prop2db.keySet());
         }
     }
 
     /**
      * Return the name of the prototype which specifies the storage location
      * (dbsource + tablename) for this type, or null if it is stored in the embedded
      * db.
      */
     public String getStorageTypeName() {
         if ((tableName == null) && (dbSourceName == null) && (parentMapping != null)) {
             return parentMapping.getStorageTypeName();
         }
 
         return (dbSourceName == null) ? null : typename;
     }
 
     /**
      * Check whether this DbMapping inherits its storage location from its
      * parent mapping. The raison d'etre for this is that we need to detect
      * inherited storage even if the dbsource and table are explicitly set
      * in the extended mapping.
      *
      * @return true if this mapping shares its parent mapping storage
      */
     protected boolean inheritsStorage() {
         // note: tableName and dbSourceName are nulled out in update() if they
         // are inherited from the parent mapping. This way we know that
         // storage is not inherited if either of them is not null.
         if (parentMapping == null || tableName != null || dbSourceName != null) {
             return false;
         }
         return true;
     }
 
     /**
      * Static utility method to check whether two DbMappings use the same storage.
      *
      * @return true if both use the embedded database or the same relational table.
      */
     public static boolean areStorageCompatible(DbMapping dbm1, DbMapping dbm2) {
         if (dbm1 == null)
             return dbm2 == null || !dbm2.isRelational();
         return dbm1.isStorageCompatible(dbm2);        
     }
 
     /**
      * Tell if this DbMapping uses the same storage as the given DbMapping.
      *
      * @return true if both use the embedded database or the same relational table.
      */
     public boolean isStorageCompatible(DbMapping other) {
         if (other == null) {
             return !isRelational();
         } else if (other == this) {
             return true;
         } else if (isRelational()) {
             return getTableName().equals(other.getTableName()) &&
                    getDbSource().equals(other.getDbSource());
         }
 
         return !other.isRelational();
     }
 
     /**
      *  Return true if this db mapping represents the prototype indicated
      *  by the string argument, either itself or via one of its parent prototypes.
      */
     public boolean isInstanceOf(String other) {
         if ((typename != null) && typename.equals(other)) {
             return true;
         }
 
         DbMapping p = parentMapping;
 
         while (p != null) {
             if ((p.typename != null) && p.typename.equals(other)) {
                 return true;
             }
 
             p = p.parentMapping;
         }
 
         return false;
     }
 
     /**
      * Get the mapping we inherit from, or null
      *
      * @return the parent DbMapping, or null
      */
     public DbMapping getParentMapping() {
         return parentMapping;
     }
 
     /**
      * Get our ResourceProperties
      *
      * @return our properties
      */
     public ResourceProperties getProperties() {
         return props;
     }
 
     /**
      * Register a DbMapping that depends on this DbMapping, so that collections of other mapping
      * should be reloaded if data on this mapping is updated.
      *
      * @param dbmap the DbMapping that depends on us
      */
     protected void addDependency(DbMapping dbmap) {
         this.dependentMappings.add(dbmap);
     }
     
     /*
      * Return whether the key mapp
      */
     public boolean hasTableKeys() {
         return ((localKeys != null) && (localKeys.length > 0) 
                 && (foreignKeys != null) && (foreignKeys.length > 0));
     }
     
     /*
      * Return the key mapping for the given table number
      */
     public String getTableKey(int tableNumber) {
         if ((foreignKeys != null) && (foreignKeys.length > 0) 
                 && (tableNumber > -1) && (tableNumber < foreignKeys.length)) {
             return foreignKeys[tableNumber];
         }
 
         return idField;
     }
     
     /*
      * Return primary key for a table
      */
     public String getPrimaryKeyValue(Node n, int tableNumber) {
         if (tableNumber > -1 && localKeys != null && tableNumber < localKeys.length) {
             if (idField.equalsIgnoreCase(localKeys[tableNumber])) {
                 return n.getID();
             }
             String propName = _columnNameToProperty(localKeys[tableNumber]);
             Property prop;
             if (propName != null && (prop = n.getProperty(propName)) != null) {
                 return prop.getValue().toString();
             }
         }
         return n.getID();
     }
     
     /*
      * Return the cache timeout value for a node of this mapping
      */
     public long getCacheTimeout() {
         long timeout = this.timeout;
         DbMapping mapping = this.parentMapping;
         while (timeout == -1 && mapping != null) {
             timeout = mapping.timeout;
             mapping = mapping.parentMapping;
         }
         return timeout;
     }
     
     public String getType() {
      	return app.getDbProperties().getProperty(dbSourceName + ".type", "LUCENE");
     }
     
     public String getClassName() {
     	return this.dbClass;
     }
     
 }
