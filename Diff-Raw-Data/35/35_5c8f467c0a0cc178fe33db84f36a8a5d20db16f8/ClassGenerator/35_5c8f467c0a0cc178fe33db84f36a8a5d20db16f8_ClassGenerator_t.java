 package org.kered.dko.ant;
 
 import java.io.BufferedReader;
 import java.io.BufferedWriter;
 import java.io.File;
 import java.io.FileReader;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.sql.Blob;
 import java.sql.Date;
 import java.sql.Timestamp;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
 import java.util.Set;
 import java.util.TreeSet;
 import java.util.regex.Pattern;
 
 import org.kered.dko.Constants;
 import org.kered.dko.Field;
 import org.kered.dko.json.JSONArray;
 import org.kered.dko.json.JSONException;
 import org.kered.dko.json.JSONObject;
 
 class ClassGenerator {
 
 	private final String pkg;
 	private final String dir;
 	private final String pkgDir;
 
 	private final String[] stripPrefixes;
 
 	private final String[] stripSuffixes;
 
 	private Map<String, String> tableToClassName;
 	private JSONObject classTypeMappings = new JSONObject();
 	private JSONObject typeMappingFunctions = new JSONObject();
 	private Map<Pattern, String> schemaTypeMappings;
 	private final Map<String, String> schemaAliases;
 
 
 	public ClassGenerator(final String dir, final String pkg, final String[] stripPrefixes, final String[] stripSuffixes, final Map<String, String> schemaAliases) {
 		this.dir = dir;
 		this.pkg = pkg;
 		this.schemaAliases = schemaAliases;
 		pkgDir = Util.join("/", pkg.split("[.]"));
 		this.stripPrefixes = stripPrefixes.clone();
 		this.stripSuffixes = stripSuffixes.clone();
 	}
 
 	private static class FK {
 	    String name = null;
 	    String[] reffing = null;
 	    String[] reffed = null;
 	    Map<String,String> columns = new LinkedHashMap<String,String>();
 	}
 
 	public static void go(final String dir, final String pkg, final String[] stripPrefixes,
 		final String[] stripSuffixes, final String metadataFile, final Map<String, String> schemaAliases, final File fakeFKsFile,
 		final String typeMappingsFile, final String dataSource, final String callbackPackage, final JSONObject enums, final boolean useDetailedToString)
 				throws IOException, JSONException {
 
 		BufferedReader br = new BufferedReader(new FileReader(metadataFile));
 		StringBuffer sb = new StringBuffer();
 		String s = null;
 		while ((s=br.readLine())!=null) sb.append(s).append('\n');
 		final JSONObject metadata = new JSONObject(sb.toString());
 
 		JSONObject fakeFKs = new JSONObject();
 		if (fakeFKsFile!=null && fakeFKsFile.exists()) {
 			br = new BufferedReader(new FileReader(fakeFKsFile));
 			sb = new StringBuffer();
 			s = null;
 			while ((s=br.readLine())!=null) sb.append(s).append('\n');
 			fakeFKs = new JSONObject(sb.toString());
 		}
 
 		JSONObject classTypeMappings = new JSONObject();
 		JSONObject schemaTypeMappings = new JSONObject();
 		JSONObject typeMappingFunctions = new JSONObject();
 		final File mappingsFile = typeMappingsFile==null ? null : new File(typeMappingsFile);
 		if (mappingsFile!=null && mappingsFile.exists()) {
 			br = new BufferedReader(new FileReader(mappingsFile));
 			sb = new StringBuffer();
 			s = null;
 			while ((s=br.readLine())!=null) sb.append(s).append('\n');
 			final JSONObject allMappings = new JSONObject(sb.toString());
 			classTypeMappings = allMappings.getJSONObject("class_mappings");
 			schemaTypeMappings = allMappings.getJSONObject("schema_mappings");
 			typeMappingFunctions = allMappings.getJSONObject("functions");
 		}
 
 		final ClassGenerator generator = new ClassGenerator(dir, pkg, stripPrefixes, stripSuffixes, schemaAliases);
 		generator.classTypeMappings = classTypeMappings;
 		generator.typeMappingFunctions = typeMappingFunctions;
 		generator.schemaTypeMappings = new LinkedHashMap<Pattern,String>();
 		for (final String key : schemaTypeMappings.keySet()) {
 			final String value = schemaTypeMappings.optString(key);
 			generator.schemaTypeMappings.put(Pattern.compile(key), value);
 		}
 
 		final JSONObject schemas = metadata.getJSONObject("schemas");
 		final JSONObject foreignKeys = metadata.getJSONObject("foreign_keys");
 
 		final String dataSourceName = DataSourceGenerator.getDataSourceName(dataSource);
 
 		for (final String schema : schemas.keySet()) {
 			final JSONObject tables = schemas.getJSONObject(schema);
 
 			String pkgName = sanitizeJavaKeywords(schema);
 			if (schemaAliases.containsKey(schema)) {
 				pkgName = sanitizeJavaKeywords(schemaAliases.get(schema));
 			}
 
 			generator.tableToClassName = new LinkedHashMap<String,String>();
 
 			for (final String table : new TreeSet<String>(tables.keySet())) {
 				// we process these in order to avoid naming conflicts when you have
 				// both plural and singular tables of the same root word
 				generator.genTableClassName(table);
 			}
 			generator.genTableToClassMap(pkgName);
 
 			for (final String table : tables.keySet()) {
 			    // skip these junk mssql tables
 			    if(table.startsWith("syncobj_")) continue;
 
 				final JSONObject columns = tables.getJSONObject(table);
 				final JSONArray pks = getJSONArray(metadata, "primary_keys", schema, table);
 				final List<FK> fks = new ArrayList<FK>();
 				final List<FK> fksIn = new ArrayList<FK>();
 
 				for (final String constraint_name : foreignKeys.keySet()) {
 				    final JSONObject fkmd = foreignKeys.getJSONObject(constraint_name);
 				    splitFK(schema, table, fks, fksIn, constraint_name, fkmd);
 				}
 				for (final String constraint_name : fakeFKs.keySet()) {
 				    final JSONObject fkmd = fakeFKs.getJSONObject(constraint_name);
 				    splitFK(schema, table, fks, fksIn, constraint_name, fkmd);
 				}
 
 				generator.generate(schema, pkgName, table, columns, pks, fks, fksIn, dataSourceName,
 						callbackPackage, enums, useDetailedToString);
 			}
 
 		}
 
 	}
 
 	static String sanitizeJavaKeywords(String s) {
 		s = s.toLowerCase();
 		if (Constants.KEYWORDS_JAVA.contains(s)) return s+"_";
 		return s;
 
 	}
 
 	private void genTableToClassMap(final String schema) throws IOException {
 		final File targetDir = new File(Util.join("/", dir, pkgDir, schema));
 		if (!targetDir.isDirectory()) targetDir.mkdirs();
 		final File file = new File(Util.join("/", dir, pkgDir, schema, "_TableToClassMap.java"));
 		System.out.println("writing: "+ file.getPath());
 		final BufferedWriter br = new BufferedWriter(new FileWriter(file));
 		br.write("package "+ pkg +"."+ schema +";\n\n");
 		br.write("import java.util.Collections;\n");
 		br.write("import java.util.Map;\n");
 		br.write("import java.util.HashMap;\n");
 		br.write("import org.kered.dko.Table;\n\n");
 		br.write("public class _TableToClassMap {\n");
 		br.write("\tpublic static final Map<String, Class<? extends Table>> IT;\n");
 		br.write("\tstatic {\n");
 		br.write("\t\tMap<String, Class<? extends Table>> it = new HashMap<String, Class<? extends Table>>();\n");
 		for (final Entry<String, String> e : this.tableToClassName.entrySet()) {
 			br.write("\t\tit.put(\""+ e.getKey() +"\", "+ e.getValue() +".class);\n");
 		}
 		br.write("\t\tIT = Collections.unmodifiableMap(it);\n");
 		br.write("\t}\n");
 		// end class
 		br.write("}\n");
 		br.close();
 	}
 
 	private static void splitFK(final String schema, final String table, final List<FK> fks, final List<FK> fksIn,
 		final String constraint_name, final JSONObject fkmd) throws JSONException {
 	    final FK fk = new FK();
 	    fk.name = constraint_name;
 	    final String[] reffing = {fkmd.getJSONArray("reffing").getString(0),
 	        fkmd.getJSONArray("reffing").getString(1)};
 	    fk.reffing = reffing;
 	    final String[] reffed = {fkmd.getJSONArray("reffed").getString(0),
 	        fkmd.getJSONArray("reffed").getString(1)};
 	    fk.reffed = reffed;
 	    final JSONObject cols = fkmd.getJSONObject("columns");
 	    for (final String key : cols.keySet()) {
 	    fk.columns.put(key, cols.getString(key));
 	    }
 	    if (schema.equals(fk.reffing[0]) && table.equals(fk.reffing[1])) {
 	    fks.add(fk);
 	    }
 	    if (schema.equals(fk.reffed[0]) && table.equals(fk.reffed[1])) {
 	    fksIn.add(fk);
 	    }
 	}
 
 	private static JSONObject getJSONObject(JSONObject o, final String... path) throws JSONException {
 		for (final String s : path) {
 			if (o==null) return null;
 			if (o.has(s)) o = o.optJSONObject(s);
 			else return null;
 		}
 		return o;
 	}
 
 	private static JSONArray getJSONArray(Object o, final String... path) throws JSONException {
 		for (final String s : path) {
 			if (o==null) return null;
 			if (o instanceof JSONObject && ((JSONObject)o).has(s)) {
 				o = ((JSONObject)o).opt(s);
 			} else return null;
 		}
 		if (o instanceof JSONArray) return (JSONArray) o;
 		else return null;
 	}
 
 
 	private static void recursiveDelete(final File file) {
 		if (file==null) return;
 		for (final File f : file.listFiles()) {
 			if (f.isDirectory()) recursiveDelete(f);
 			else f.delete();
 		}
 		file.delete();
 	}
 
 
 	private static String getFieldName(final String column) {
 		return column.replace(' ', '_')
 			.replace("%", "_PERCENT")
 			.replace("-", "_DASH_")
 			.toUpperCase();
 	}
 
 	private static String getFieldName(final Collection<String> columns) {
 	    final StringBuffer sb = new StringBuffer();
 	    int i = 1;
 	    for (final String column : columns) {
 		sb.append(column.replace(' ', '_').toUpperCase());
 		if (++i < columns.size()) sb.append("__");
 	    }
 		return sb.toString();
 	}
 
 	private void generate(final String schema, final String pkgName, final String table, final JSONObject columns, JSONArray pks,
 			final List<FK> fks, final List<FK> fksIn, final String dataSourceName, final String callbackPackage,
 			final JSONObject enums, final boolean useDetailedToString)
 	throws IOException, JSONException {
 		final String className = genTableClassName(table);
 		final Set<String> pkSet = new HashSet<String>();
 		if (pks == null) pks = new JSONArray();
 		for (int i=0; i<pks.length(); ++i) {
 			pkSet.add(pks.getString(i));
 		}
 		final int fieldCount = columns.keySet().size();
 
 		new File(Util.join("/", dir, pkgDir, pkgName)).mkdirs();
 		final File file = new File(Util.join("/", dir, pkgDir, pkgName, className+".java"));
 		System.out.println("writing: "+ file.getAbsolutePath());
 		final BufferedWriter br = new BufferedWriter(new FileWriter(file));
 		br.write("package "+ pkg +"."+ pkgName +";\n\n");
 		br.write("import java.lang.reflect.Method;\n");
 		br.write("import java.lang.reflect.InvocationTargetException;\n");
 		br.write("import java.sql.SQLException;\n");
 		br.write("import javax.sql.DataSource;\n");
 		br.write("import java.util.Map;\n\n");
 		br.write("import java.util.HashMap;\n\n");
 		br.write("import org.kered.dko.Field;\n");
 		br.write("import org.kered.dko.Query;\n");
 		br.write("import org.kered.dko.QueryFactory;\n");
 		br.write("import org.kered.dko.Condition;\n");
 		br.write("import org.kered.dko.Table;\n");
 		br.write("\n");
 		br.write("/**\n");
 		br.write(" * This class represents the database table: "+ schema +"."+ table +"\n");
 		br.write(" * Static elements represent the table as a whole.  Instances represent rows in the database.\n");
 		br.write(" */\n");
 		br.write("public class "+ className +" extends Table implements Comparable<"+ className +">, java.io.Serializable {\n\n");
 
 		// write field constants
 		int index = 0;
 		for (final String column : columns.keySet()) {
 			br.write("\t/**\n");
 			br.write("\t * Represents the database field: "+ table +"."+ column +"\n");
 			br.write("\t */\n");
 			br.write("\tpublic static final Field<");
 			final String sqlType = columns.getString(column);
 			br.write(getFieldType(pkgName, table, column, sqlType));
 			final String fieldName = getFieldName(column);
 			br.write("> "+ fieldName);
 			br.write(" = new Field<"+ getFieldType(pkgName, table, column, sqlType));
 			br.write(">("+ index +", "+ className +".class, \""+ column +"\", \""+ fieldName);
 			br.write("\", "+ getFieldType(pkgName, table, column, sqlType) +".class");
 			br.write(", \""+ sqlType +"\");\n");
 			++index;
 		}
 		br.write("\n");
 
 		// write primary keys
 		br.write("\t/**\n");
 		br.write("\t * A special object defining what fields make up the primary key of this table\n");
 		br.write("\t */\n");
 		br.write("\tpublic static Field.PK<"+ className +"> PK = new Field.PK<"+ className +">(");
 		for (int i=0; i<pks.length(); ++i) {
 			br.write(pks.getString(i).toUpperCase());
 			if (i<pks.length()-1) br.write(", ");
 		}
 		br.write(");\n");
 
 		// write foreign keys
 		for (final FK fk : fks) {
 			final String referencedSchema = fk.reffed[0];
 			final String referencedTable = fk.reffed[1];
 			String referencedTableClassName = genTableClassName(referencedTable);
 			if (!schema.equals(referencedSchema)) {
 				String relatedSchemaAlias = schemaAliases.get(referencedSchema);
 				if (relatedSchemaAlias == null) relatedSchemaAlias = sanitizeJavaKeywords(referencedSchema);
 				referencedTableClassName = pkg +"."+ sanitizeJavaKeywords(fk.reffed[0]) +"."+ referencedTableClassName;
 			}
 			final String fkName = genFKName(fk.columns.keySet(), referencedTable);
 			br.write("\t/**\n");
 			br.write("\t * A special object defining the foreign key relationship between this table and:\n");
 			br.write("\t * "+ referencedSchema +"."+ referencedTable +"\n");
 			br.write("\t */\n");
 			br.write("\tpublic static final Field.FK<"+ referencedTableClassName +"> "+ fkName);
 			br.write(" = new Field.FK<"+ referencedTableClassName +">(\""+ fkName +"\", "+ index +", "+ className +".class, ");
 			br.write(referencedTableClassName +".class");
 			for (final Entry<String, String> e : fk.columns.entrySet()) {
 				br.write(", "+ e.getKey().toUpperCase());
 			}
 			for (final Entry<String, String> e : fk.columns.entrySet()) {
 				br.write(", "+ referencedTableClassName +".");
 				br.write(e.getValue().toUpperCase());
 			}
 			br.write(");\n");
 			++index;
 		}
 		br.write("\n");
 
 		// write pk enums
 		for (final String column : columns.keySet()) {
 			final String enumKey = pkgName +"."+ table +"."+ column;
 			if (!enums.has(enumKey)) continue;
 			final JSONObject instances = enums.optJSONObject(enumKey);
 			final boolean simple = pkSet.size() == 1;
 			if (simple) {
 				final String pk = pkSet.iterator().next();
 				final String pkType = getFieldType(pkgName, table, pk, columns.getString(pk));
 				br.write("\tpublic enum PKS implements Table.__SimplePrimaryKey<"+ className +", "+ pkType +"> {\n\n");
 				int count = 0;
				Set<String> usedNames = new HashSet<String>();
 				for (final String name : instances.keySet()) {
 					++ count;
 					String value = instances.optJSONArray(name).getString(0);
					String javaName = name.toUpperCase().replaceAll("\\W", "_");
					if (Character.isDigit(javaName.charAt(0))) javaName = "_"+javaName;
					if (usedNames.contains(javaName)) {
						javaName = javaName +"_"+ value.toUpperCase().replaceAll("\\W", "_");
					}
					usedNames.add(javaName);
 					if ("java.lang.String".equals(pkType)) value = "\""+ value +"\"";
					br.write("\t\t"+ javaName);
 					br.write("(\""+ name.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\"") +"\", "+ value +")");
 					if (count < instances.keySet().size()) br.write(",\n");
 				}
 				br.write(";\n\n");
 				br.write("\t\tprivate final String _NAME;\n");
 				br.write("\t\tpublic final "+ pkType +" "+ getFieldName(pk) +";\n");
 				br.write("\t\tPKS(String _name, "+ pkType +" v) {\n");
 				br.write("\t\t\t_NAME = _name;\n");
 				br.write("\t\t\t"+ getFieldName(pk) +" = v;\n");
 				br.write("\t\t}\n");
 				br.write("\t\tpublic "+ pkType +" get"+ getInstanceMethodName(pk) +"() {\n");
 				br.write("\t\t\treturn "+ getFieldName(pk) +";\n");
 				br.write("\t\t}\n");
				br.write("\t\t@java.lang.Override\n");
 				br.write("\t\tpublic java.util.List<Field<?>> FIELDS() {\n");
 				br.write("\t\t\tjava.util.List<Field<?>> ret = new java.util.ArrayList<Field<?>>();\n");
 				br.write("\t\t\tret.add("+ className +"."+ getFieldName(pk) +");\n");
 				br.write("\t\t\treturn ret;\n");
 				br.write("\t\t}\n");
 				br.write("\t\t@SuppressWarnings(\"unchecked\")\n");
				br.write("\t\t@java.lang.Override\n");
 				br.write("\t\tpublic <R> R get(Field<R> field) {\n");
 				br.write("\t\t\tif (field=="+ className +"."+ getFieldName(pk)
 						+") return (R) Integer.valueOf("+ getFieldName(pk) +"); \n");
 				br.write("\t\t\tif ("+ className +"."+ getFieldName(pk)
 						+".sameField(field)) return (R) Integer.valueOf("
 						+ getFieldName(pk) +");\n");
 				br.write("\t\t\tthrow new RuntimeException(\"field \"+ field +\" is not part of this primary key\");\n");
 				br.write("\t\t}\n");
				br.write("\t\t@java.lang.Override\n");
 				br.write("\t\tpublic "+ pkType +" value() {\n");
 				br.write("\t\t\treturn "+ getFieldName(pk)+ ";\n");
 				br.write("\t\t}\n\n");
				br.write("\t\t@java.lang.Override\n");
 				br.write("\t\tpublic String toString() {\n");
 				br.write("\t\t\treturn _NAME;\n");
 				br.write("\t\t}\n\n");
 				br.write("\t\tpublic static PKS lookup("+ pkType +" v) {\n");
 				br.write("\t\t\tfor (final PKS x : PKS.values()) {\n");
 				br.write("\t\t\t\tif (x."+ getFieldName(pk) +" == null ? v == null : x."+ getFieldName(pk) +".equals(v)) {\n");
 				br.write("\t\t\t\t\treturn x;\n");
 				br.write("\t\t\t\t}\n");
 				br.write("\t\t\t}\n");
 				br.write("\t\t\treturn null;\n");
 				br.write("\t\t}\n\n");
 				br.write("\t\tpublic static PKS lookup(String _name) {\n");
 				br.write("\t\t\tfor (final PKS x : PKS.values()) {\n");
 				br.write("\t\t\t\tif (x._NAME == null ? _name == null : x._NAME.equals(_name)) {\n");
 				br.write("\t\t\t\t\treturn x;\n");
 				br.write("\t\t\t\t}\n");
 				br.write("\t\t\t}\n");
 				br.write("\t\t\treturn null;\n");
 				br.write("\t\t}\n\n");
 				br.write("\t}\n");
 			}
 		}
 
 		// write field value references
 		for (final String column : pkSet) {
 			br.write("\tprivate "+ getFieldType(pkgName, table, column, columns.getString(column)));
 			br.write(" "+ getInstanceFieldName(column) + " = null;\n");
 		}
 		for (final String column : columns.keySet()) {
 			if (pkSet.contains(column)) continue;
 			br.write("\tprivate "+ getFieldType(pkgName, table, column, columns.getString(column)));
 			br.write(" "+ getInstanceFieldName(column) + " = null;\n");
 		}
 		br.write("\n");
 
 		// write constructors
 		br.write("\t/**\n");
 		br.write("\t * Creates a new (empty) instance of "+ className +".  Call .insert() to insert it into the database.\n");
 		br.write("\t */\n");
 		br.write("\tpublic "+ className +"() {}\n\n");
 		br.write("\tpublic "+ className +"("+ className +" o) {\n");
 		for (final String column : columns.keySet()) {
 			br.write("\t\tif (o.__NOSCO_FETCHED_VALUES.get("+ getFieldName(column) +".INDEX)) {\n");
 			br.write("\t\t\t"+ getInstanceFieldName(column) +" = o.get"+ getInstanceMethodName(column) +"();\n");
 			br.write("\t\t\t__NOSCO_FETCHED_VALUES.set("+ getFieldName(column) +".INDEX);\n");
 			br.write("\t\t}\n");
 		}
 		br.write("\t\tif(o.__NOSCO_UPDATED_VALUES != null) __NOSCO_UPDATED_VALUES = (java.util.BitSet) o.__NOSCO_UPDATED_VALUES.clone();\n\n");
 		br.write("\t}\n\n");
 		br.write("\t@SuppressWarnings(\"rawtypes\")\n");
 		br.write("\tprotected "+ className +"(final Field[] _fields, final Object[] _objects, final int _start, final int _end) {\n");
 		br.write("\t\tif (_fields.length != _objects.length)\n\t\t\tthrow new IllegalArgumentException(");
 		br.write("\"fields.length != objects.length => \"+ _fields.length +\" != \"+ _objects.length");
 		br.write(" +\"\");\n");
 		br.write("\t\tfor (int _i=_start; _i<_end; ++_i) {\n");
 		for (final String column : columns.keySet()) {
 			br.write("\t\t\tif (_fields[_i]=="+ getFieldName(column) +") {\n");
 			br.write("\t\t\t\t"+ getInstanceFieldName(column) +" = ");
 			final String assignment = convertToActualType(schema, table, column,
 					columns.getString(column),
 					"("+ getFieldClassType(columns.getString(column)).getName()+ ") _objects[_i]");
 			br.write(assignment);
 			br.write(";\n");
 			br.write("\t\t\t\t__NOSCO_FETCHED_VALUES.set("+ getFieldName(column) +".INDEX);\n");
 			br.write("\t\t\t\tcontinue;\n");
 			br.write("\t\t\t}\n");
 		}
 		br.write("\t\t}\n\t}\n\n");
 
 		// write abstract method impls
 		br.write("\tpublic String SCHEMA_NAME() {\n\t\treturn \""+ schema +"\";\n\t}\n\n");
 
 		br.write("\tpublic String TABLE_NAME() {\n\t\treturn \""+ table +"\";\n\t}\n\n");
 
 		br.write("\tprivate static java.util.List<Field<?>> __NOSCO_FIELDS;\n");
 		br.write("\tstatic {\n");
 		br.write("\t\tjava.util.List<Field<?>> fields = new java.util.ArrayList<Field<?>>();\n");
 		for (final String column : columns.keySet()) {
 			br.write("\t\tfields.add("+getFieldName(column)+");\n");
 		}
 		br.write("\t\t__NOSCO_FIELDS = java.util.Collections.unmodifiableList(fields);\n");
 		br.write("\t}\n");
 		br.write("\t@SuppressWarnings(\"rawtypes\")\n");
 		br.write("\tpublic java.util.List<Field<?>> FIELDS() {\n");
 		br.write("\t\treturn __NOSCO_FIELDS;\n\t}\n\n");
 
 		br.write("\t@SuppressWarnings(\"rawtypes\")\n");
 		br.write("\tpublic Field.FK[] FKS() {\n\t\tfinal Field.FK[] fields = {");
 		for (final FK fk : fks) {
 			final String referencedTable = fk.reffed[1];
 //			if (!schema.equals(fk.reffed[0])) {
 //				referencedTable = fk.reffed[0] +"_"+ referencedTable;
 //			}
 			br.write(genFKName(fk.columns.keySet(), referencedTable) + ",");
 		}
 		br.write("};\n\t\treturn fields;\n\t}\n\n");
 
 		// write the generic get(field) method
 		br.write("\t@SuppressWarnings(\"unchecked\")\n");
 		br.write("\tpublic <S> S get(final Field<S> _field) {\n");
 		for (final String column : columns.keySet()) {
 			br.write("\t\tif (_field=="+ getFieldName(column) +") ");
 			br.write("return (S) get"+ getInstanceMethodName(column) +"();\n");
 		}
 		br.write("\t\tthrow new IllegalArgumentException(\"unknown field \"+ _field);\n");
 		br.write("\t}\n\n");
 
 		// write the generic set(field, value) method
 		br.write("\tpublic <S> void set(final Field<S> _field, final S _value) {\n");
 		for (final String column : columns.keySet()) {
 			br.write("\t\tif (_field=="+ getFieldName(column) +") ");
 			br.write(getInstanceFieldName(column) +" = ("+ getFieldType(pkgName, table, column, columns.getString(column)) +") _value; else\n");
 		}
 		br.write("\t\tthrow new IllegalArgumentException(\"unknown field \"+ _field);\n");
 		br.write("\t}\n\n");
 
 		//br.write("\tpublic Field[] GET_PRIMARY_KEY_FIELDS() {\n\t\tField[] fields = {");
 		//for (int i=0; i<pks.length(); ++i) {
 		//	br.write(pks.getString(i).toUpperCase()+",");
 		//}
 		//br.write("};\n\t\treturn fields;\n\t}\n\n");
 
 		br.write("\t/**\n");
 		br.write("\t * Represents all rows in the database.  This is the object you will likely\n");
 		br.write("\t * use the most.  Iterating over this object will stream to you all rows in\n");
 		br.write("\t * this table.  Filtering it with .where() calls constrains your query.\n");
 		br.write("\t * Please see the DKO documentation <a href='http://nosco.googlecode.com/hg/doc/api/org/nosco/Query.html'>here</a>\n");
 		br.write("\t * for more usage information.  (Or the 'introduction to DKOs' document \n");
 		br.write("\t * <a href='http://nosco.googlecode.com/hg/doc/introduction.html'>here</a>)\n");
 		br.write("\t */\n");
 		br.write("\tpublic static final Query<"+ className +"> ALL = QueryFactory.IT.getQuery("
 		+ className +".class)");
 		//if (dataSourceName != null) {
 		//	br.write(".use("+ pkg +"."+ dataSourceName +"."+ pkgName.toUpperCase() +")");
 		//}
 		br.write(";\n\n");
 		if (dataSourceName != null) {
 			br.write("\tstatic DataSource __DEFAULT_DATASOURCE = "+ pkg +"."
 					+ dataSourceName +".INSTANCE;\n\n");
 		}
 
 		// write toString
 		br.write("\t public String toString() {\n");
 		br.write("\t\tif (__NOSCO_CALLBACK_TOSTRING!=null) {\n");
 		br.write("\t\t\ttry { return (String) __NOSCO_CALLBACK_TOSTRING.invoke(null, this); }\n");
 		br.write("\t\t\tcatch (Throwable e) { __NOSCO_LOGGER.warning(e.toString()); }\n");
 		br.write("\t\t}\n");
 		if (useDetailedToString) {
 			br.write("\t\treturn this.toStringDetailed();\n");
 		} else {
 			br.write("\t\treturn this.toStringSimple();\n");
 		}
 		br.write("\t}\n");
 
 		// write toStringShort
 		br.write("\t public String toStringSimple() {\n");
 		br.write("\t\tStringBuffer _sb = new StringBuffer();\n");
 		br.write("\t\t_sb.append(\"["+ className +"\");");
 		for (final String column : pkSet) {
 			writeToStringPart(br, column);
 		}
 		boolean foundNameColumn = false;
 		for (final String column : columns.keySet()) {
 			if (pkSet.contains(column)) continue;
 			if (!"name".equalsIgnoreCase(column)) continue;
 			foundNameColumn = true;
 			writeToStringPart(br, column);
 		}
 		if (!foundNameColumn) {
 			for (final String column : columns.keySet()) {
 				if (pkSet.contains(column)) continue;
 				if (!column.toLowerCase().contains("name")) continue;
 				writeToStringPart(br, column);
 			}
 		}
 		br.write("\t\t_sb.append(\"]\");\n");
 		br.write("\t\treturn _sb.toString();\n");
 		br.write("\t}\n\n");
 
 		// write toStringDetailed
 		br.write("\t public String toStringDetailed() {\n");
 		br.write("\t\tStringBuffer _sb = new StringBuffer();\n");
 		br.write("\t\t_sb.append(\"["+ className +"\");");
 		for (final String column : pkSet) {
 			writeToStringPart(br, column);
 		}
 		for (final String column : columns.keySet()) {
 			if (pkSet.contains(column)) continue;
 			writeToStringPart(br, column);
 		}
 		br.write("\t\t_sb.append(\"]\");\n");
 		br.write("\t\treturn _sb.toString();\n");
 		br.write("\t}\n\n");
 
 		// write getters and setters
 		for (final String column : columns.keySet()) {
 			final String cls = getFieldType(pkgName, table, column, columns.getString(column));
 			br.write("\t/**\n");
 			br.write("\t * Gets the value of column "+ column +".\n");
 			br.write("\t */\n");
 			br.write("\tpublic "+ cls +" get"+ getInstanceMethodName(column) +"() {\n");
 			br.write("\t\t\t__NOSCO_PRIVATE_accessedColumnCallback(this, "+ getFieldName(column) +");\n");
 			br.write("\t\tif (!__NOSCO_FETCHED_VALUES.get("+ getFieldName(column) +".INDEX) && __NOSCO_ORIGINAL_DATA_SOURCE!=null) {\n");
 			br.write("\t\t\tfinal "+ className +" _tmp = ALL.use(__NOSCO_ORIGINAL_DATA_SOURCE).onlyFields(");
 			br.write(getFieldName(column)+")");
 			for (final String pk : pkSet) {
 				br.write(".where("+ getFieldName(pk) +".eq("+ getInstanceFieldName(pk) +"))");
 			}
 			br.write(".getTheOnly();\n");
 			br.write("\t\t\t"+ getInstanceFieldName(column) +" = _tmp == null ? null : _tmp."
 					+ getInstanceFieldName(column) +";\n");
 			br.write("\t\t\t__NOSCO_FETCHED_VALUES.set("+ getFieldName(column) +".INDEX);\n");
 			br.write("\t\t}\n");
 			br.write("\t\treturn "+ getInstanceFieldName(column) +";\n\t}\n\n");
 
 			br.write("\t/**\n");
 			br.write("\t * Sets the value of column "+ column +".\n");
 			br.write("\t */\n");
 			br.write("\tpublic "+ className +" set"+ getInstanceMethodName(column));
 			br.write("(final "+ cls +" v) {\n");
 			br.write("\t\t"+ getInstanceFieldName(column) +" = v;\n");
 			br.write("\t\tif (__NOSCO_UPDATED_VALUES == null) __NOSCO_UPDATED_VALUES = new java.util.BitSet();\n");
 			br.write("\t\t__NOSCO_UPDATED_VALUES.set("+ getFieldName(column) +".INDEX);\n");
 			br.write("\t\t__NOSCO_FETCHED_VALUES.set("+ getFieldName(column) +".INDEX);\n");
 			br.write("\t\treturn this;\n");
 			br.write("\t}\n\n");
 
 			if (!cls.equals(getFieldClassType(columns.getString(column)).getName())) {
 				br.write("\tpublic "+ className +" set"+ getInstanceMethodName(column));
 				br.write("("+ getFieldClassType(columns.getString(column)).getName() +" v) {\n");
 				br.write("\t\treturn set"+ getInstanceMethodName(column) +"("+
 						this.convertToActualType(pkgName, table, column,
 								columns.getString(column), "v") +");\n");
 				br.write("\t}\n\n");
 			}
 		}
 
 		// write getters and setters for FKs
 		for (final FK fk : fks) {
 			final String referencedSchema = fk.reffed[0];
 			final String referencedTable = fk.reffed[1];
 			//String referencedColumn = referenced.getString(2);
 			String referencedTableClassName = genTableClassName(referencedTable);
 			if (!schema.equals(referencedSchema)) {
 				String relatedSchemaAlias = schemaAliases.get(referencedSchema);
 				if (relatedSchemaAlias == null) relatedSchemaAlias = sanitizeJavaKeywords(referencedSchema);
 				referencedTableClassName = pkg +"."+ relatedSchemaAlias +"."+ referencedTableClassName;
 			}
 			final String methodName = genFKMethodName(fk.columns.keySet(), referencedTable);
 			final String cachedObjectName = "_NOSCO_FK_"+ underscoreToCamelCase(fk.columns.keySet(), false);
 
 			br.write("\tprivate "+ referencedTableClassName +" "+ cachedObjectName +" = null;\n\n");
 
 			br.write("\t/**\n");
 			br.write("\t * Gets the object representing the row this FK is referencing.\n");
 			br.write("\t * If it was not pre-loaded with the .with(FK), object is lazy-loaded.\n");
 			br.write("\t */\n");
 			br.write("\tpublic "+ referencedTableClassName +" get"+ methodName +"() {\n");
 			final String fkName = genFKName(fk.columns.keySet(), referencedTable);
 			br.write("\t\tif (!__NOSCO_FETCHED_VALUES.get("+ fkName +".INDEX)) {\n");
 			br.write("\t\t\t"+ cachedObjectName +" = "+ referencedTableClassName +".ALL");
 			br.write(".where("+ referencedTableClassName +"."+ getFieldName(fk.columns.values()) +".eq("+ underscoreToCamelCase(fk.columns.keySet(), false) +"))");
 			br.write(".getTheOnly();\n");
 			br.write("\t\t\t__NOSCO_FETCHED_VALUES.set("+ fkName +".INDEX);\n");
 			br.write("\t\t\t__NOSCO_PRIVATE_accessedFkCallback(this, "+ fkName +");\n");
 			br.write("\t\t}\n");
 			br.write("\t\treturn "+ cachedObjectName +";\n\t}\n\n");
 
 			br.write("\t/**\n");
 			br.write("\t * Sets the row this FK is referencing.\n");
 			br.write("\t */\n");
 			br.write("\tpublic "+ className +" set"+ methodName +"(final "+ referencedTableClassName +" v) {\n");
 			//br.write(" v) {\n\t\tPUT_VALUE(");
 
 			br.write("\t\t"+ underscoreToCamelCase(fk.columns.keySet(), false) +" = v.get"+ underscoreToCamelCase(fk.columns.values(), true) +"();\n");
 			br.write("\t\tif (__NOSCO_UPDATED_VALUES == null) __NOSCO_UPDATED_VALUES = new java.util.BitSet();\n");
 			br.write("\t\t__NOSCO_UPDATED_VALUES.set("+ getFieldName(fk.columns.keySet()) +".INDEX);\n");
 			br.write("\t\t"+ cachedObjectName +" = v;\n");
 			br.write("\t\t__NOSCO_UPDATED_VALUES.set("+ fkName +".INDEX);\n");
 			br.write("\t\t__NOSCO_FETCHED_VALUES.set("+ fkName +".INDEX);\n");
 			br.write("\t\treturn this;\n");
 
 			br.write("\n\t}\n\n");
 		}
 
 		// write SET_FK
 		br.write("\tprotected void SET_FK(final Field.FK<?> field, final Object v) {\n");
 		br.write("\t\tif (false);\n");
 		for (final FK fk : fks) {
 			final String cachedObjectName = "_NOSCO_FK_"+ underscoreToCamelCase(fk.columns.keySet(), false);
 			final String referencedTable = fk.reffed[1];
 			String relatedTableClassName = genTableClassName(referencedTable);
 			final String referencedSchema = fk.reffed[0];
 			if (!schema.equals(referencedSchema)) {
 				String relatedSchemaAlias = schemaAliases.get(referencedSchema);
 				if (relatedSchemaAlias == null) relatedSchemaAlias = sanitizeJavaKeywords(referencedSchema);
 				relatedTableClassName = pkg +"."+ relatedSchemaAlias +"."+ relatedTableClassName;
 		}
 			br.write("\t\telse if (field == "+ genFKName(fk.columns.keySet(), referencedTable) +") {\n");
 			br.write("\t\t\t"+ cachedObjectName +" = ("+ relatedTableClassName +") v;\n");
 			br.write("\t\t\t__NOSCO_FETCHED_VALUES.set("+ genFKName(fk.columns.keySet(), referencedTable) +".INDEX);\n");
 			br.write("\t\t}\n");
 
 		}
 		br.write("\t\telse {throw new RuntimeException(\"unknown FK\");}\n");
 		br.write("\t}\n\n");
 
 		// write SET_FK_SET methods
 		br.write("\t@SuppressWarnings(\"unchecked\")\n");
 		br.write("\tprotected void SET_FK_SET(final Field.FK<?> fk, final Query<?> v) {\n");
 		br.write("\t\tif (false);\n");
 		for (final FK fk : fksIn) {
 		    final String relatedSchema = fk.reffing[0];
 		    final String relatedTable = fk.reffing[1];
 		    String relatedTableClassName = this.genTableClassName(relatedTable);
 			if (!schema.equals(relatedSchema)) {
 				String relatedSchemaAlias = schemaAliases.get(relatedSchema);
 				if (relatedSchemaAlias == null) relatedSchemaAlias = sanitizeJavaKeywords(relatedSchema);
 				relatedTableClassName = pkg +"."+ relatedSchemaAlias +"."+ relatedTableClassName;
 			}
 			final String fkName = genFKName(fk.columns.keySet(), fk.reffed[1]);
 		    final String localVar = "__NOSCO_CACHED_FK_SET___"+ relatedTable + "___"
 		    		+ Util.join("__", fk.columns.keySet());
 		    br.write("\t\telse if ("+ relatedTableClassName +"."+ fkName +".equals(fk)) {\n");
 		    br.write("\t\t\t"+ localVar +" = (Query<"+ relatedTableClassName +">) v;\n");
 		    br.write("\t\t}\n");
 		}
 		br.write("\t\telse {throw new RuntimeException(\"unknown FK\");}\n");
 		br.write("\t}\n\n");
 
 		// write the getTableFKSet() functions
 		final Map<String, Integer> reffingCounts = new HashMap<String,Integer>();
 		for (final FK fk : fksIn) {
 		    final String relatedTable = fk.reffing[1];
 		    Integer c = reffingCounts.get(relatedTable);
 		    if (c == null) c = 0;
 		    reffingCounts.put(relatedTable, c+1);
 		}
 		for (final FK fk : fksIn) {
 		    final String relatedSchema = fk.reffing[0];
 		    final String relatedTable = fk.reffing[1];
 		    final String reffedTable = fk.reffed[1];
 		    String relatedTableClassName = this.genTableClassName(relatedTable);
 			if (!schema.equals(relatedSchema)) {
 				String relatedSchemaAlias = schemaAliases.get(relatedSchema);
 				if (relatedSchemaAlias == null) relatedSchemaAlias = sanitizeJavaKeywords(relatedSchema);
 				relatedTableClassName = pkg +"."+ relatedSchemaAlias +"."+ relatedTableClassName;
 			}
 		    String method = getInstanceMethodName(relatedTable);
 		    if (reffingCounts.get(relatedTable) > 1) {
 			    final String tmp = Util.join("_", fk.columns.keySet());
 				method = method + "_" + getInstanceMethodName(tmp);
 		    }
 		    final String localVar = "__NOSCO_CACHED_FK_SET___"+ relatedTable + "___"
 		    		+ Util.join("__", fk.columns.keySet());
 		    br.write("\tprivate Query<"+ relatedTableClassName +"> "+ localVar +" = null;\n");
 			br.write("\t/**\n");
 			br.write("\t * Returns a query representing the collection of rows this FK is referencing.\n");
 			br.write("\t * If it pre-loaded with the .with(FK), collection is in memory.\n");
 			br.write("\t * Else, iterations over this query will query the database.\n");
 			br.write("\t */\n");
 		    br.write("\tpublic Query<"+ relatedTableClassName +"> get"+ method +"Set() {\n");
 		    br.write("\t\tif ("+ localVar +" != null) return "+ localVar + ";\n");
 		    //br.write("\t\tif (__NOSCO_SELECT != null) return __NOSCO_PRIVATE_getSelectCachedQuery("+ relatedTableClassName + ".class, condition);\n");
 			final String fkName = genFKName(fk.columns.keySet(), reffedTable);
 			//br.write("\t\t\t__NOSCO_PRIVATE_accessedFkCallback(this, "+ relatedTableClassName +"."+ fkName +");\n");
 		    br.write("\t\tCondition condition = Condition.TRUE");
 		    for (final Entry<String, String> e : fk.columns.entrySet()) {
 				final String relatedColumn = e.getKey();
 				final String column = e.getValue();
 				br.write(".and("+ relatedTableClassName +"."+ getFieldName(relatedColumn) +".eq(get"+ getInstanceMethodName(column) +"()))");
 		    }
 		    br.write(";\n");
 		    br.write("\t\treturn "+ relatedTableClassName +".ALL.where(condition);\n");
 		    br.write("\t}\n\n");
 		}
 
 		// write save function
 		br.write("\t/**\n");
 		br.write("\t * Saves the object by either an insert or an update operation to the default DataSource.\n");
 		br.write("\t */\n");
 		br.write("\tpublic boolean save() throws SQLException {\n");
 		br.write("\t\t return save(ALL.getDataSource());\n");
 		br.write("\t}\n");
 		br.write("\t/**\n");
 		br.write("\t * Saves the object by either an insert or an update operation to the specified DataSource.\n");
 		br.write("\t */\n");
 		br.write("\t@SuppressWarnings(\"rawtypes\")\n");
 		br.write("\tpublic boolean save(final DataSource _ds) throws SQLException {\n");
 		br.write("\t\tif (!dirty()) return false;\n");
 		br.write("\t\tfinal Query<"+ className +"> query = ALL.use(_ds)");
 		for (final String pk : pkSet) {
 			br.write(".where("+ getFieldName(pk) +".eq("+ getInstanceFieldName(pk) +"))");
 		}
 		br.write(";\n");
 		if (pkSet == null || pkSet.isEmpty()) {
 			br.write("\t\tthrow new RuntimeException(\"save() is ambiguous on objects without PKs - use insert() or update()\");\n");
 		} else {
 			br.write("\t\tfinal long size = query.size();\n");
 			br.write("\t\tif (size == 0) return this.insert(_ds);\n");
 			br.write("\t\telse if (size == 1) return this.update(_ds);\n");
 			br.write("\t\telse throw new RuntimeException(\"more than one result was returned " +
 					"for a query that specified all the PKs.  this is bad.\");\n");
 			br.write("\t\t\n");
 		}
 		br.write("\t}\n");
 
 		// write update function
 		br.write("\t/**\n");
 		br.write("\t * Updates this row in the database using the default DataSource.\n");
 		br.write("\t */\n");
 		br.write("\tpublic boolean update() throws SQLException {\n");
 		br.write("\t\t return update(ALL.getDataSource());\n");
 		br.write("\t}\n");
 		br.write("\t/**\n");
 		br.write("\t * Updates this row in the database using the specified DataSource.\n");
 		br.write("\t */\n");
 		br.write("\t@SuppressWarnings(\"rawtypes\")\n");
 		br.write("\tpublic boolean update(final DataSource _ds) throws SQLException {\n");
 		br.write("\t\tif (!dirty()) return false;\n");
 		br.write("\t\tQuery<"+ className +"> query = ALL.use(_ds)");
 		for (final String pk : pkSet) {
 			br.write(".where("+ getFieldName(pk) +".eq("+ getInstanceFieldName(pk) +"))");
 		}
 		if (pkSet == null || pkSet.size() == 0) {
 			for (final String column : columns.keySet()) {
 				br.write(".where("+ getFieldName(column) +".eq("+ getInstanceFieldName(column) +"))");
 			}
 		}
 		br.write(";\n");
 		br.write("\t\tif (__NOSCO_CALLBACK_UPDATE_PRE!=null) "
 				+ "try {\n\t\t\tfinal "+ className +"[] __NOSCO_CALLBACKS = {this};\n"
 				+ "\t\t\t__NOSCO_CALLBACK_UPDATE_PRE.invoke(null, (Object)__NOSCO_CALLBACKS, _ds); }"
 				+ "catch (IllegalAccessException e) { e.printStackTrace(); } "
 				+ "catch (InvocationTargetException e) { e.printStackTrace(); }\n");
 		br.write("\t\tif (__NOSCO_CALLBACK_UPDATE_PRE==null && __NOSCO_CALLBACK_UPDATE_PRE_OLD!=null) "
 				+ "try { __NOSCO_CALLBACK_UPDATE_PRE_OLD.invoke(null, this); }"
 				+ "catch (IllegalAccessException e) { e.printStackTrace(); } "
 				+ "catch (InvocationTargetException e) { e.printStackTrace(); }\n");
 		br.write("\t\tfinal Map<Field<?>,Object> updates = new HashMap<Field<?>,Object>();\n");
 		for (final String column : columns.keySet()) {
 			br.write("\t\tif (__NOSCO_UPDATED_VALUES.get("+ getFieldName(column) +".INDEX)) {\n");
 			br.write("\t\t\tupdates.put("+ getFieldName(column) +", "
 			+ convertToOriginalType(pkgName, table, column, columns.getString(column), getInstanceFieldName(column)) +");\n");
 			br.write("\t\t}\n");
 		}
 		br.write("\t\tquery = query.set(updates);\n");
 		br.write("\t\tfinal int count = query.update();\n");
 		br.write("\t\tif (__NOSCO_CALLBACK_UPDATE_POST!=null) "
 				+ "try {\n\t\t\tfinal "+ className +"[] __NOSCO_CALLBACKS = {this};\n"
 				+ "\t\t\t__NOSCO_CALLBACK_UPDATE_POST.invoke(null, (Object)__NOSCO_CALLBACKS, _ds); }"
 				+ "catch (IllegalAccessException e) { e.printStackTrace(); } "
 				+ "catch (InvocationTargetException e) { e.printStackTrace(); }\n");
 		br.write("\t\tif (__NOSCO_CALLBACK_UPDATE_POST==null && __NOSCO_CALLBACK_UPDATE_POST_OLD!=null) "
 				+ "try { __NOSCO_CALLBACK_UPDATE_POST_OLD.invoke(null, this, _ds); }"
 				+ "catch (IllegalAccessException e) { e.printStackTrace(); } "
 				+ "catch (InvocationTargetException e) { e.printStackTrace(); }\n");
 		br.write("\t\treturn count==1;\n");
 		br.write("\t}\n");
 
 		// write delete function
 		br.write("\t/**\n");
 		br.write("\t * Deletes this row in the database using the default DataSource.\n");
 		br.write("\t */\n");
 		br.write("\tpublic boolean delete() throws SQLException {\n");
 		br.write("\t\t return delete(ALL.getDataSource());\n");
 		br.write("\t}\n");
 		br.write("\t/**\n");
 		br.write("\t * Deletes this row in the database using the specified DataSource.\n");
 		br.write("\t */\n");
 		br.write("\t@SuppressWarnings(\"rawtypes\")\n");
 		br.write("\tpublic boolean delete(final DataSource _ds) throws SQLException {\n");
 		br.write("\t\tfinal Query<"+ className +"> query = ALL.use(_ds)");
 		for (final String pk : pkSet) {
 			br.write(".where("+ getFieldName(pk) +".eq("+ getInstanceFieldName(pk) +"))");
 		}
 		if (pkSet == null || pkSet.size() == 0) {
 			for (final String column : columns.keySet()) {
 				br.write(".where("+ getFieldName(column) +".eq("+ getInstanceFieldName(column) +"))");
 			}
 		}
 		br.write(";\n");
 		br.write("\t\tif (__NOSCO_CALLBACK_DELETE_PRE!=null) "
 				+ "try {\n\t\t\tfinal "+ className +"[] __NOSCO_CALLBACKS = {this};\n"
 				+ "\t\t\t__NOSCO_CALLBACK_DELETE_PRE.invoke(null, (Object)__NOSCO_CALLBACKS, _ds); }"
 				+ "catch (IllegalAccessException e) { e.printStackTrace(); } "
 				+ "catch (InvocationTargetException e) { e.printStackTrace(); }\n");
 		br.write("\t\tint count = query.delete();\n");
 		br.write("\t\tif (__NOSCO_CALLBACK_DELETE_POST!=null) "
 				+ "try {\n\t\t\tfinal "+ className +"[] __NOSCO_CALLBACKS = {this};\n"
 				+ "\t\t\t__NOSCO_CALLBACK_DELETE_POST.invoke(null, (Object)__NOSCO_CALLBACKS); }"
 				+ "catch (IllegalAccessException e) { e.printStackTrace(); } "
 				+ "catch (InvocationTargetException e) { e.printStackTrace(); }\n");
 		br.write("\t\treturn count==1;\n");
 		br.write("\t}\n");
 
 		// write insert function
 		br.write("\t/**\n");
 		br.write("\t * Inserts this row in the database using the default DataSource.\n");
 		br.write("\t */\n");
 		br.write("\tpublic boolean insert() throws SQLException {\n");
 		br.write("\t\t return insert(ALL.getDataSource());\n");
 		br.write("\t}\n");
 		br.write("\t/**\n");
 		br.write("\t * Inserts this row in the database using the specified DataSource.\n");
 		br.write("\t */\n");
 		br.write("\t@SuppressWarnings(\"rawtypes\")\n");
 		br.write("\tpublic boolean insert(final DataSource _ds) throws SQLException {\n");
 		//br.write("\t\tif (!dirty()) return false;\n");
 		br.write("\t\tQuery<"+ className +"> query = ALL.use(_ds)");
 		for (final String pk : pkSet) {
 			br.write(".where("+ getFieldName(pk) +".eq("+ getInstanceFieldName(pk) +"))");
 		}
 		br.write(";\n");
 		if (!pkSet.isEmpty()) {
 			br.write("\t\tif (__NOSCO_CALLBACK_INSERT_PRE!=null) "
 					+ "try {\n\t\t\tfinal "+ className +"[] __NOSCO_CALLBACKS = {this};\n"
 					+ "\t\t\t__NOSCO_CALLBACK_INSERT_PRE.invoke(null, (Object)__NOSCO_CALLBACKS, _ds); }"
 					+ "catch (IllegalAccessException e) { e.printStackTrace(); } "
 					+ "catch (InvocationTargetException e) { e.printStackTrace(); }\n");
 			br.write("\t\tif (__NOSCO_CALLBACK_INSERT_PRE==null && __NOSCO_CALLBACK_INSERT_PRE_OLD!=null) "
 					+ "try { __NOSCO_CALLBACK_INSERT_PRE_OLD.invoke(null, this); }"
 					+ "catch (IllegalAccessException e) { e.printStackTrace(); } "
 					+ "catch (InvocationTargetException e) { e.printStackTrace(); }\n");
 		}
 		br.write("\t\tMap<Field<?>,Object> updates = new HashMap<Field<?>,Object>();\n");
 		for (final String column : columns.keySet()) {
 			br.write("\t\tupdates.put("+ getFieldName(column) +", "
 		+ convertToOriginalType(pkgName, table, column, columns.getString(column), getInstanceFieldName(column)) +");\n");
 		}
 		br.write("\t\tquery = query.set(updates);\n");
 		br.write("\t\t\tquery.insert();\n");
 		br.write("\t\t\tif (__NOSCO_CALLBACK_INSERT_POST!=null) "
 				+ "try {\n\t\t\tfinal "+ className +"[] __NOSCO_CALLBACKS = {this};\n"
 					+ "\t\t\t__NOSCO_CALLBACK_INSERT_POST.invoke(null, (Object)__NOSCO_CALLBACKS, _ds); }"
 				+ "catch (IllegalAccessException e) { e.printStackTrace(); } "
 				+ "catch (InvocationTargetException e) { e.printStackTrace(); }\n");
 		br.write("\t\t\tif (__NOSCO_CALLBACK_INSERT_POST==null && __NOSCO_CALLBACK_INSERT_POST_OLD!=null) "
 				+ "try { __NOSCO_CALLBACK_INSERT_POST_OLD.invoke(null, this, _ds); }"
 				+ "catch (IllegalAccessException e) { e.printStackTrace(); } "
 				+ "catch (InvocationTargetException e) { e.printStackTrace(); }\n");
 		br.write("\t\t\treturn true;\n");
 		br.write("\t}\n");
 
 		// write exists function
 		br.write("\t/**\n");
 		br.write("\t * Tests if this row exists in the database using the default DataSource.\n");
 		br.write("\t */\n");
 		br.write("\tpublic boolean exists() throws SQLException {\n");
 		br.write("\t\t return exists(ALL.getDataSource());\n");
 		br.write("\t}\n");
 		br.write("\t/**\n");
 		br.write("\t * Tests if this row exists in the database using the specified DataSource.\n");
 		br.write("\t */\n");
 		br.write("\t@SuppressWarnings(\"rawtypes\")\n");
 		br.write("\tpublic boolean exists(final DataSource _ds) throws SQLException {\n");
 		br.write("\t\tfinal Query<"+ className +"> query = ALL.use(_ds)");
 		for (final String column : pkSet == null || pkSet.size() == 0 ? columns.keySet() : pkSet) {
 			br.write(".where("+ getFieldName(column) +".eq("+ getInstanceFieldName(column) +"))");
 		}
 		br.write(";\n");
 		br.write("\t\tfinal long size = query.size();\n");
 		br.write("\t\treturn size > 0;\n");
 		br.write("\t}\n");
 
 
 		// write the logger
 		br.write("\tprivate static final java.util.logging.Logger __NOSCO_LOGGER = " +
 				"java.util.logging.Logger.getLogger(\""+ pkgName +"."+ className +"\");\n");
 
 
 		// write callbacks
 		br.write("\tprivate static Method __NOSCO_CALLBACK_TOSTRING = null;\n");
 		br.write("\tprivate static Method __NOSCO_CALLBACK_INSERT_PRE = null;\n");
 		br.write("\tprivate static Method __NOSCO_CALLBACK_INSERT_POST = null;\n");
 		br.write("\tprivate static Method __NOSCO_CALLBACK_UPDATE_PRE = null;\n");
 		br.write("\tprivate static Method __NOSCO_CALLBACK_UPDATE_POST = null;\n");
 		br.write("\tprivate static Method __NOSCO_CALLBACK_DELETE_PRE = null;\n");
 		br.write("\tprivate static Method __NOSCO_CALLBACK_DELETE_POST = null;\n");
 		br.write("\tprivate static Method __NOSCO_CALLBACK_HASH_CODE = null;\n");
 		br.write("\tprivate static Method __NOSCO_CALLBACK_EQUALS = null;\n");
 		br.write("\tprivate static Method __NOSCO_CALLBACK_COMPARE_TO = null;\n");
 		br.write("\tprivate static Method __NOSCO_CALLBACK_INSERT_PRE_OLD = null;\n");
 		br.write("\tprivate static Method __NOSCO_CALLBACK_INSERT_POST_OLD = null;\n");
 		br.write("\tprivate static Method __NOSCO_CALLBACK_UPDATE_PRE_OLD = null;\n");
 		br.write("\tprivate static Method __NOSCO_CALLBACK_UPDATE_POST_OLD = null;\n");
 		if (callbackPackage != null) {
 			br.write("\tstatic {\n");
 			br.write("\t\ttry {\n");
 			br.write("\t\t\t __NOSCO_CALLBACK_TOSTRING = Class.forName(\""+ callbackPackage
 					+"."+ pkgName +"."+ className +"CB\").getMethod(\"toString\", "
 					+ className +".class);\n");
 			br.write("\t\t\t__NOSCO_LOGGER.fine(\"found toString callback \"+ __NOSCO_CALLBACK_TOSTRING);\n");
 			br.write("\t\t} catch (final Exception e) { /* ignore */ }\n");
 			br.write("\t\ttry {\n");
 			br.write("\t\t\t __NOSCO_CALLBACK_INSERT_PRE = Class.forName(\""+ callbackPackage
 					+"."+ pkgName +"."+ className +"CB\").getMethod(\"preInsert\", "
 					+ className +"[].class, DataSource.class);\n");
 			br.write("\t\t\t__NOSCO_LOGGER.fine(\"found preInsert callback \"+ __NOSCO_CALLBACK_INSERT_PRE);\n");
 			br.write("\t\t} catch (final Exception e) { /* ignore */ }\n");
 			br.write("\t\ttry {\n");
 			br.write("\t\t\t __NOSCO_CALLBACK_INSERT_POST = Class.forName(\""+ callbackPackage
 					+"."+ pkgName +"."+ className +"CB\").getMethod(\"postInsert\", "
 					+ className +"[].class, DataSource.class);\n");
 			br.write("\t\t\t__NOSCO_LOGGER.fine(\"found postInsert callback \"+ __NOSCO_CALLBACK_INSERT_POST);\n");
 			br.write("\t\t} catch (final Exception e) { /* ignore */ }\n");
 			br.write("\t\ttry {\n");
 			br.write("\t\t\t __NOSCO_CALLBACK_UPDATE_PRE = Class.forName(\""+ callbackPackage
 					+"."+ pkgName +"."+ className +"CB\").getMethod(\"preUpdate\", "
 					+ className +"[].class, DataSource.class);\n");
 			br.write("\t\t\t__NOSCO_LOGGER.fine(\"found preUpdate callback \"+ __NOSCO_CALLBACK_UPDATE_PRE);\n");
 			br.write("\t\t} catch (final Exception e) { /* ignore */ }\n");
 			br.write("\t\ttry {\n");
 			br.write("\t\t\t __NOSCO_CALLBACK_UPDATE_POST = Class.forName(\""+ callbackPackage
 					+"."+ pkgName +"."+ className +"CB\").getMethod(\"postUpdate\", "
 					+ className +"[].class, DataSource.class);\n");
 			br.write("\t\t\t__NOSCO_LOGGER.fine(\"found postUpdate callback \"+ __NOSCO_CALLBACK_UPDATE_POST);\n");
 			br.write("\t\t} catch (final Exception e) { /* ignore */ }\n");
 			br.write("\t\ttry {\n");
 			br.write("\t\t\t __NOSCO_CALLBACK_DELETE_PRE = Class.forName(\""+ callbackPackage
 					+"."+ pkgName +"."+ className +"CB\").getMethod(\"preDelete\", "
 					+ className +"[].class, DataSource.class);\n");
 			br.write("\t\t\t__NOSCO_LOGGER.fine(\"found preDelete callback \"+ __NOSCO_CALLBACK_DELETE_PRE);\n");
 			br.write("\t\t} catch (final Exception e) { /* ignore */ }\n");
 			br.write("\t\ttry {\n");
 			br.write("\t\t\t __NOSCO_CALLBACK_DELETE_POST = Class.forName(\""+ callbackPackage
 					+"."+ pkgName +"."+ className +"CB\").getMethod(\"postDelete\", "
 					+ className +"[].class, DataSource.class);\n");
 			br.write("\t\t\t__NOSCO_LOGGER.fine(\"found postDelete callback \"+ __NOSCO_CALLBACK_DELETE_POST);\n");
 			br.write("\t\t} catch (final Exception e) { /* ignore */ }\n");
 			br.write("\t\ttry {\n");
 			br.write("\t\t\t __NOSCO_CALLBACK_INSERT_PRE_OLD = Class.forName(\""+ callbackPackage
 					+"."+ pkgName +"."+ className +"CB\").getMethod(\"preInsert\", "
 					+ className +".class, DataSource.class);\n");
 			br.write("\t\t} catch (final Exception e) { /* ignore */ }\n");
 			br.write("\t\ttry {\n");
 			br.write("\t\t\t __NOSCO_CALLBACK_INSERT_POST_OLD = Class.forName(\""+ callbackPackage
 					+"."+ pkgName +"."+ className +"CB\").getMethod(\"postInsert\", "
 					+ className +".class, DataSource.class);\n");
 			br.write("\t\t} catch (final Exception e) { /* ignore */ }\n");
 			br.write("\t\ttry {\n");
 			br.write("\t\t\t __NOSCO_CALLBACK_UPDATE_PRE_OLD = Class.forName(\""+ callbackPackage
 					+"."+ pkgName +"."+ className +"CB\").getMethod(\"preUpdate\", "
 					+ className +".class, DataSource.class);\n");
 			br.write("\t\t} catch (final Exception e) { /* ignore */ }\n");
 			br.write("\t\ttry {\n");
 			br.write("\t\t\t __NOSCO_CALLBACK_UPDATE_POST_OLD = Class.forName(\""+ callbackPackage
 					+"."+ pkgName +"."+ className +"CB\").getMethod(\"postUpdate\", "
 					+ className +".class, DataSource.class);\n");
 			br.write("\t\t} catch (final Exception e) { /* ignore */ }\n");
 			br.write("\t\ttry {\n");
 			br.write("\t\t\t __NOSCO_CALLBACK_HASH_CODE = Class.forName(\""+ callbackPackage
 					+"."+ pkgName +"."+ className +"CB\").getMethod(\"hashCode\", "
 					+ className +".class);\n");
 			br.write("\t\t\t__NOSCO_LOGGER.fine(\"found hashCode callback \"+ __NOSCO_CALLBACK_HASH_CODE);\n");
 			br.write("\t\t} catch (final Exception e) { /* ignore */ }\n");
 			br.write("\t\ttry {\n");
 			br.write("\t\t\t __NOSCO_CALLBACK_EQUALS = Class.forName(\""+ callbackPackage
 					+"."+ pkgName +"."+ className +"CB\").getMethod(\"equals\", "
 					+ className +".class, Object.class);\n");
 			br.write("\t\t\t__NOSCO_LOGGER.fine(\"found equals callback \"+ __NOSCO_CALLBACK_EQUALS);\n");
 			br.write("\t\t} catch (final Exception e) { /* ignore */ }\n");
 			br.write("\t\ttry {\n");
 			br.write("\t\t\t __NOSCO_CALLBACK_COMPARE_TO = Class.forName(\""+ callbackPackage
 					+"."+ pkgName +"."+ className +"CB\").getMethod(\"compareTo\", "
 					+ className +".class, " + className +".class);\n");
 			br.write("\t\t\t__NOSCO_LOGGER.fine(\"found compareTo callback \"+ __NOSCO_CALLBACK_COMPARE_TO);\n");
 			br.write("\t\t} catch (final Exception e) { /* ignore */ }\n");
 			br.write("\t}\n");
 		}
 
 		// write the alias function
 		br.write("\t/**\n");
 		br.write("\t * Returns a table alias.  This is used when specifying manual joins\n");
 		br.write("\t * to reference later using Field.from(alias) in where() conditions.\n");
 		br.write("\t */\n");
 		br.write("\tpublic static Table.__Alias<"+ className +"> as(final String alias) {\n");
 		br.write("\t\treturn new Table.__Alias<"+ className +">("+ className +".class, alias);\n");
 		br.write("\t}\n\n");
 
 		// write the hashcode function
		br.write("\t@java.lang.Override\n");
 		br.write("\tpublic int hashCode() {\n");
 		br.write("\t\tif (__NOSCO_CALLBACK_HASH_CODE!=null)\n"
 				+ "\t\t\ttry { return (Integer) __NOSCO_CALLBACK_HASH_CODE.invoke(null, this); }\n"
 				+ "\t\t\tcatch (final IllegalAccessException e) { e.printStackTrace(); }\n"
 				+ "\t\t\tcatch (final InvocationTargetException e) { e.printStackTrace(); }\n");
 		br.write("\t\tfinal int prime = 31;\n");
 		br.write("\t\tint result = 1;\n");
 		for (final String column : pkSet == null || pkSet.size() == 0 ? columns.keySet() : pkSet) {
 			br.write("\t\tresult = prime * result + (("+ getInstanceFieldName(column)
 					+" == null) ? 0 : "+ getInstanceFieldName(column) +".hashCode());\n");
 		}
 		br.write("\t\treturn result;\n");
 		br.write("\t}\n\n");
 
 		// write the equals function
		br.write("\t@java.lang.Override\n");
 		br.write("\tpublic boolean equals(final Object other) {\n");
 		br.write("\t\tif (__NOSCO_CALLBACK_EQUALS!=null)\n"
 				+ "\t\t\ttry { return (Boolean) __NOSCO_CALLBACK_EQUALS.invoke(null, this, other); }\n"
 				+ "\t\t\tcatch (final IllegalAccessException e) { e.printStackTrace(); }\n"
 				+ "\t\t\tcatch (final InvocationTargetException e) { e.printStackTrace(); }\n");
 		br.write("\t\treturn (other == this) || ((other != null) \n");
 		br.write("\t\t\t&& (other instanceof "+ className +")\n");
 		br.write("\t\t\n");
 		for (final String column : pkSet == null || pkSet.size() == 0 ? columns.keySet() : pkSet) {
 			br.write("\t\t\t&& (("+ getInstanceFieldName(column) +" == null) ? ((("
 					+ className +")other)."+ getInstanceFieldName(column) +" == null) : ("
 					+ getInstanceFieldName(column) +".equals((("+ className +")other)."
 					+ getInstanceFieldName(column) +")))\n");
 		}
 		br.write("\t\t);\n");
 		br.write("\t}\n\n");
 
 		// write the compare function
		br.write("\t@java.lang.Override\n");
 		br.write("\t@SuppressWarnings({\"rawtypes\",\"unchecked\"})\n");
 		br.write("\tpublic int compareTo(final "+ className +" o) {\n");
 		br.write("\t\tif (__NOSCO_CALLBACK_COMPARE_TO!=null) "
 				+ "try { return (Integer) __NOSCO_CALLBACK_COMPARE_TO.invoke(null, this, o); }"
 				+ "catch (IllegalAccessException e) { e.printStackTrace(); } "
 				+ "catch (InvocationTargetException e) { e.printStackTrace(); }\n");
 		br.write("\t\tint v = 0;\n");
 		for (final String column : pkSet == null || pkSet.size() == 0 ? columns.keySet() : pkSet) {
 			final String fieldName = getInstanceFieldName(column);
 			br.write("\t\tif ("+ fieldName +" instanceof Comparable) {\n");
 			br.write("\t\t\tv = "+ fieldName +"==null ? (o."+ fieldName
 					+ "==null ? 0 : -1) : ((Comparable) "
 					+ fieldName +").compareTo(o."+ fieldName +");\n");
 			br.write("\t\t\tif (v != 0) return v;\n");
 			br.write("\t\t}\n");
 		}
 		br.write("\t\treturn 0;\n");
 		br.write("\t}\n\n");
 
 		// write the map type function
		br.write("\t@java.lang.Override\n");
 		br.write("\t/**\n");
 		br.write("\t * Internal method - please do not use.\n");
 		br.write("\t */\n");
 		br.write("\tpublic java.lang.Object __NOSCO_PRIVATE_mapType(final java.lang.Object o) {\n");
 		final Set<String> coveredTypes = new HashSet<String>();
 		for (final String origType : classTypeMappings.keySet()) {
 			final String actualType = classTypeMappings.optString(origType);
 			br.write("\t\tif (o instanceof "+ actualType +") return ");
 			br.write(typeMappingFunctions.optString(actualType +" "+ origType)
 					.replaceAll("[%]s", "(("+ actualType +")o)"));
 			br.write(";\n");
 			coveredTypes.add(actualType);
 		}
 		for (final String actualType : this.schemaTypeMappings.values()) {
 			if (coveredTypes.contains(actualType)) continue;
 			String origType = null;
 			for (final String column : columns.keySet()) {
 				final String type = columns.getString(column);
 				if (actualType.equals(this.getFieldType(pkgName, table, column, type))) {
 					origType = this.getFieldClassType(type).getName();
 					break;
 				}
 			}
 			if (origType == null) continue;
 			br.write("\t\tif (o instanceof "+ actualType +") return ");
 			br.write(typeMappingFunctions.optString(actualType +" "+ origType)
 					.replaceAll("[%]s", "(("+ actualType +")o)"));
 			br.write(";\n");
 			coveredTypes.add(actualType);
 		}
 		for (final String actualType : this.schemaTypeMappings.values()) {
 			for (final String x : typeMappingFunctions.keySet()) {
 				if (coveredTypes.contains(actualType)) continue;
 				final String[] y = x.split(" ");
 				if (y==null || y.length!=2) {
 					throw new RuntimeException("bad key in type_mappings: '"+ x +"'. must be of " +
 							"the form 'com.something.ClassA com.else.ClassB'");
 				}
 				if (actualType.equals(y[0])) {
 					br.write("\t\tif (o instanceof "+ actualType +") return ");
 					br.write(typeMappingFunctions.optString(actualType +" "+ y[1])
 							.replaceAll("[%]s", "(("+ actualType +")o)"));
 					br.write(";\n");
 					coveredTypes.add(actualType);
 				}
 			}
 		}
 		br.write("\t\treturn o;\n");
 		br.write("\t}\n\n");
 
 		// end class
 		br.write("}\n");
 		br.close();
 	}
 
 	private void writeToStringPart(final BufferedWriter br, final String column)
 			throws IOException {
 		br.write("\t\tif (__NOSCO_FETCHED_VALUES.get("+ getFieldName(column) +".INDEX)) {\n");
 		br.write("\t\t\t_sb.append(\" "+ column +":\");\n");
 		br.write("\t\t\tString _tmp = "+ getInstanceFieldName(column) +"==null ? \"null\" : "+ getInstanceFieldName(column) +".toString();\n");
 		br.write("\t\t\tboolean _quote = _tmp.contains(\" \");\n");
 		br.write("\t\t\tif (_quote) _sb.append('\"');\n");
 		br.write("\t\t\t_sb.append(_tmp.replace(\"\\\"\", \"\\\\\\\"\"));\n");
 		br.write("\t\t\tif (_quote) _sb.append('\"');\n");
 		br.write("\t\t}\n");
 	}
 
 	private static String getInstanceFieldName(String column) {
 		column = column.toLowerCase();
 		if (Constants.KEYWORDS_JAVA.contains(column)) column = "NOSCO_JAVA_KEYWORD_PROTECTION" + column;
 	    column = column.replace("-", "_DASH_");
 	    return underscoreToCamelCase(column, false);
 	}
 
 	private static String getInstanceMethodName(String column) {
 	    if ("class".equals(column)) column = "JAVA_KEYWORD_class";
 	    column = column.replace("-", "_DASH_");
 	    return underscoreToCamelCase(column, true);
 	}
 
 	private String genFKName(final Set<String> columns, String referencedTable) {
 		for(String column : columns) {
 			column = column.toUpperCase();
 			referencedTable = referencedTable.toUpperCase();
 			if (column.endsWith("_ID")) column = column.substring(0,column.length()-3);
 			if (referencedTable.startsWith(column)) {
 				return "FK_"+ dePlural(referencedTable).toUpperCase();
 			} else {
 				return "FK_"+ dePlural(column +"_"+ referencedTable).toUpperCase();
 			}
 		}
 		return null;
 	}
 
 
 	private String genFKMethodName(final Set<String> columns, final String referencedTable) {
 		return this.underscoreToCamelCase(columns, true)+"FK";
 		//return genTableClassName(genFKName(columns, referencedTable));
 	}
 
 
 	private String genTableClassName(final String table) {
 		if (tableToClassName.containsKey(table)) return tableToClassName.get(table);
 		String proposed = table;
 		for (final String prefix : stripPrefixes) {
 			if (proposed.startsWith(prefix)) {
 				proposed = proposed.substring(prefix.length());
 				break;
 			}
 		}
 		/*for (String suffix : stripSuffixes) {
 			if (table.endsWith(suffix)) {
 				table = table.substring(0,table.length()-suffix.length());
 				break;
 			}
 		} //*/
 		String proposed2 = underscoreToCamelCase(dePlural(proposed), true);
 		if (tableToClassName.containsValue(proposed2)) {
 			proposed2 = underscoreToCamelCase(proposed, true);
 		}
 		tableToClassName.put(table, proposed2);
 		return proposed2;
 	}
 
 	private String dePlural(String s) {
 		s = s.toLowerCase();
 //		if (s.endsWith("series"));
 //		else if (s.endsWith("us"));
 //		else if (s.endsWith("is"));
 //		else if (s.endsWith("as"));
 //		else if (s.endsWith("ss"));
 //		else if (s.endsWith("xes")) s = s.substring(0,s.length()-2);
 //		else if (s.endsWith("ies")) s = s.substring(0,s.length()-3)+"y";
 //		else if (s.endsWith("s")) s = s.substring(0,s.length()-1);
 		return s;
 	}
 
 	private String convertToActualType(final String schema, final String table, final String column,
 			final String type, final String var) {
 		final String origType = getFieldClassType(type).getName();
 		final String actualType = getFieldType(schema, table, column, type);
 		if (origType.equals(actualType)) {
 			return var;
 		}
 		return convertType(var, origType, actualType);
 	}
 
 	private String convertType(final String var, final String fromType, final String toType) {
 		final String key = fromType +" "+ toType;
 		if (!typeMappingFunctions.has(key)) {
 			throw new RuntimeException("a mapping function of '" + key +"' was not " +
 					"found");
 		}
 		try {
 			String ret = typeMappingFunctions.getString(key);
 			ret = "("+ var +"== null) ? null : " + ret.replaceAll("[%]s", var);
 			return ret;
 		} catch (final JSONException e) {
 			e.printStackTrace();
 			throw new RuntimeException("the mapping function of '" + key +"' needs " +
 					"to be a JSON string");
 		}
 	}
 
 	private String convertToOriginalType(final String schema, final String table, final String column,
 			final String type, final String var) {
 		final String origType = getFieldClassType(type).getName();
 		final String actualType = getFieldType(schema, table, column, type);
 		if (origType.equals(actualType)) {
 			return var;
 		}
 		return convertType(var, actualType, origType);
 	}
 
 	private String getFieldType(final String schema, final String table, final String column, final String type) {
 		final String s = getFieldClassType(type).getName();
 		final String v = schema +"."+ table +"."+ column;
 		for (final Entry<Pattern, String> e : schemaTypeMappings.entrySet()) {
 			if (e.getKey().matcher(v).matches()) {
 				return e.getValue();
 			}
 		}
 		try {
 			return classTypeMappings.has(s) ? classTypeMappings.getString(s) : s;
 		} catch (final JSONException e) {
 			e.printStackTrace();
 			try {
 				throw new RuntimeException("a String was expected for the type mapping of "
 						+ s +" but a "+ classTypeMappings.get(s) +" was found");
 			} catch (final JSONException e1) {
 				e1.printStackTrace();
 				throw new RuntimeException("this should never happen");
 			}
 		}
 	}
 
 	private Class<? extends Object> getFieldClassType(final String type) {
 		if ("varchar".equals(type)) return String.class;
 		if ("char".equals(type)) return Character.class;
 		if ("nvarchar".equals(type)) return String.class;
 		if ("nchar".equals(type)) return String.class;
 		if ("longtext".equals(type)) return String.class;
 		if ("text".equals(type)) return String.class;
 		if ("tinytext".equals(type)) return String.class;
 		if ("mediumtext".equals(type)) return String.class;
 		if ("ntext".equals(type)) return String.class;
 		if ("xml".equals(type)) return String.class;
 		if ("character varying".equals(type)) return String.class;
 		if ("int".equals(type)) return Integer.class;
 		if ("mediumint".equals(type)) return Integer.class;
 		if ("smallint".equals(type)) return Integer.class;
 		if ("tinyint".equals(type)) return Integer.class;
 		if ("bigint".equals(type)) return Long.class;
 		if ("integer".equals(type)) return Integer.class;
 		if ("decimal".equals(type)) return Double.class;
 		if ("money".equals(type)) return Double.class;
 		if ("numeric".equals(type)) return Double.class;
 		if ("float".equals(type)) return Double.class;
 		if ("real".equals(type)) return Float.class;
 		if ("blob".equals(type)) return Blob.class;
 		if ("longblob".equals(type)) return Blob.class;
 		if ("datetime".equals(type)) return Timestamp.class;
 		if ("date".equals(type)) return Date.class;
 		if ("timestamp".equals(type)) return Timestamp.class;
 		if ("year".equals(type)) return Integer.class;
 		if ("enum".equals(type)) return Integer.class;
 		if ("set".equals(type)) return String.class;
 		if ("bit".equals(type)) return Boolean.class;
 		if ("binary".equals(type)) return java.sql.Blob.class;
 		if ("varbinary".equals(type)) return java.sql.Blob.class;
 		if ("sql_variant".equals(type)) return Object.class;
 		if ("smalldatetime".equals(type)) return Timestamp.class;
 		if ("uniqueidentifier".equals(type)) return String.class;
 		if ("image".equals(type)) return java.sql.Blob.class;
 		System.err.println("unknown field type: "+ type);
 		return null;
 	}
 
 
 	private static String underscoreToCamelCase(String s, final boolean capitalizeFirstChar) {
 		if (s==null) return null;
 		if (s.length()==0) return s;
 		s = s.toLowerCase();
 		s = s.replace(' ', '_').replace("%", "_PERCENT");
 		final char[] c = s.toCharArray();
 		if (capitalizeFirstChar) {
 			c[0] = Character.toUpperCase(c[0]);
 		}
 		for (int i=1; i<c.length; ++i) {
 			if (c[i-1]=='_') {
 				c[i] = Character.toUpperCase(c[i]);
 			}
 		}
 		return new String(c).replaceAll("_", "");
 	}
 
 	private static String underscoreToCamelCase(final Collection<String> strings, boolean capitalizeFirstChar) {
 	    final StringBuffer sb = new StringBuffer();
 	    for (final String s : strings) {
 		sb.append(underscoreToCamelCase(s, capitalizeFirstChar));
 		capitalizeFirstChar = true;
 	    }
 	    return sb.toString();
 	}
 
 }
