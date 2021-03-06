 package it.unibz.krdb.obda.owlrefplatform.core.abox;
 
 import it.unibz.krdb.obda.model.Atom;
 import it.unibz.krdb.obda.model.CQIE;
 import it.unibz.krdb.obda.model.OBDADataFactory;
 import it.unibz.krdb.obda.model.OBDAMappingAxiom;
 import it.unibz.krdb.obda.model.OBDASQLQuery;
 import it.unibz.krdb.obda.model.Predicate;
 import it.unibz.krdb.obda.model.Predicate.COL_TYPE;
 import it.unibz.krdb.obda.model.Term;
 import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
 import it.unibz.krdb.obda.ontology.Assertion;
 import it.unibz.krdb.obda.ontology.ClassAssertion;
 import it.unibz.krdb.obda.ontology.ClassDescription;
 import it.unibz.krdb.obda.ontology.DataPropertyAssertion;
 import it.unibz.krdb.obda.ontology.DataType;
 import it.unibz.krdb.obda.ontology.Description;
 import it.unibz.krdb.obda.ontology.OClass;
 import it.unibz.krdb.obda.ontology.ObjectPropertyAssertion;
 import it.unibz.krdb.obda.ontology.Ontology;
 import it.unibz.krdb.obda.ontology.OntologyFactory;
 import it.unibz.krdb.obda.ontology.Property;
 import it.unibz.krdb.obda.ontology.PropertySomeRestriction;
 import it.unibz.krdb.obda.ontology.impl.OntologyFactoryImpl;
 import it.unibz.krdb.obda.ontology.impl.OntologyImpl;
 import it.unibz.krdb.obda.ontology.impl.PropertySomeRestrictionImpl;
 import it.unibz.krdb.obda.ontology.impl.PunningException;
 import it.unibz.krdb.obda.owlrefplatform.core.dag.DAG;
 import it.unibz.krdb.obda.owlrefplatform.core.dag.DAGConstructor;
 import it.unibz.krdb.obda.owlrefplatform.core.dag.DAGNode;
 import it.unibz.krdb.obda.owlrefplatform.core.dag.DAGOperations;
 import it.unibz.krdb.obda.owlrefplatform.core.dag.SemanticIndexRange;
 import it.unibz.krdb.obda.owlrefplatform.core.dag.SemanticIndexRange.Interval;
 
 import java.io.BufferedWriter;
 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.FileOutputStream;
 import java.io.FileReader;
 import java.io.IOException;
 import java.io.OutputStream;
 import java.io.OutputStreamWriter;
 import java.math.BigDecimal;
 import java.net.URI;
 import java.sql.Connection;
 import java.sql.PreparedStatement;
 import java.sql.ResultSet;
 import java.sql.SQLException;
 import java.sql.Statement;
 import java.sql.Timestamp;
 import java.text.ParseException;
 import java.text.SimpleDateFormat;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 import java.util.Properties;
 import java.util.Queue;
 import java.util.Set;
 
 import org.postgresql.copy.CopyManager;
 import org.postgresql.core.BaseConnection;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 
 /**
  * Store ABox assertions in the DB
  * 
  */
 public class RDBMSSIRepositoryManager implements RDBMSDataRepositoryManager {
 
 	private static final long serialVersionUID = -6494667662327970606L;
 
 	private final static Logger log = LoggerFactory.getLogger(RDBMSSIRepositoryManager.class);
 	
 	public final static String index_table = "IDX";
 
 	private final static String create_ddl = "CREATE TABLE " + index_table + " ( " + "URI VARCHAR(1000), " + "IDX INTEGER, " + "IDX_FROM INTEGER, " + "IDX_TO INTEGER, " + "ENTITY_TYPE INTEGER" + ")";
 
 	private final static String drop_dll = "DROP TABLE " + index_table + "";
 
 	private final static String insert_query = "INSERT INTO " + index_table + "(URI, IDX, IDX_FROM, IDX_TO, ENTITY_TYPE) VALUES(?, ?, ?, ?, ?)";
 
 	private final static String select_query = "SELECT * FROM " + index_table;
 
 	public static final String class_table = "quest_class_assertion";
 
 	public static final String role_table = "quest_object_property_assertion";
 
 	public static final String attribute_table_literal = "quest_data_property_literal_assertion";
 	public static final String attribute_table_string = "quest_data_property_string_assertion";
 	public static final String attribute_table_integer = "quest_data_property_integer_assertion";
 	public static final String attribute_table_decimal = "quest_data_property_decimal_assertion";
 	public static final String attribute_table_double = "quest_data_property_double_assertion";
 	public static final String attribute_table_datetime = "quest_data_property_datetime_assertion";
 	public static final String attribute_table_boolean = "quest_data_property_boolean_assertion";
 
 	public static final String class_table_create = "CREATE TABLE " + class_table + " ( " + "URI VARCHAR(1000), " + "IDX SMALLINT" + ")";
 
 	public static final String role_table_create = "CREATE TABLE " + role_table + " ( " + "URI1 VARCHAR(1000), " + "URI2 VARCHAR(1000), " + "IDX SMALLINT" + ")";
 
 	public static final String attribute_table_literal_create = "CREATE TABLE " + attribute_table_literal + " ( " + "URI VARCHAR(1000), " + "VALUE VARCHAR(1000), " + "LANG VARCHAR(20), " + "IDX SMALLINT" + ")";
 	public static final String attribute_table_string_create = "CREATE TABLE " + attribute_table_string + " ( " + "URI VARCHAR(1000), " + "VALUE VARCHAR(1000), " + "IDX SMALLINT" + ")";
 	public static final String attribute_table_integer_create = "CREATE TABLE " + attribute_table_integer + " ( " + "URI VARCHAR(1000), " + "VALUE INT, " + "IDX SMALLINT" + ")";
 	public static final String attribute_table_decimal_create = "CREATE TABLE " + attribute_table_decimal + " ( " + "URI VARCHAR(1000), " + "VALUE DECIMAL, " + "IDX SMALLINT" + ")";
 	public static final String attribute_table_double_create = "CREATE TABLE " + attribute_table_double + " ( " + "URI VARCHAR(1000), " + "VALUE DOUBLE PRECISION, " + "IDX SMALLINT" + ")";
 	public static final String attribute_table_datetime_create = "CREATE TABLE " + attribute_table_datetime + " ( " + "URI VARCHAR(1000), " + "VALUE TIMESTAMP, " + "IDX SMALLINT" + ")";
 	public static final String attribute_table_boolean_create = "CREATE TABLE " + attribute_table_boolean + " ( " + "URI VARCHAR(1000), " + "VALUE BOOLEAN, " + "IDX SMALLINT" + ")";
 
 	public static final String class_table_drop = "DROP TABLE " + class_table;
 
 	public static final String role_table_drop = "DROP TABLE " + role_table;
 
 	public static final String attribute_table_literal_drop = "DROP TABLE " + attribute_table_literal;
 	public static final String attribute_table_string_drop = "DROP TABLE " + attribute_table_string;
 	public static final String attribute_table_integer_drop = "DROP TABLE " + attribute_table_integer;
 	public static final String attribute_table_decimal_drop = "DROP TABLE " + attribute_table_decimal;
 	public static final String attribute_table_double_drop = "DROP TABLE " + attribute_table_double;
 	public static final String attribute_table_datetime_drop = "DROP TABLE " + attribute_table_datetime;
 	public static final String attribute_table_boolean_drop = "DROP TABLE " + attribute_table_boolean;
 
 	public static final String class_insert = "INSERT INTO " + class_table + " (URI, IDX) VALUES (?, ?)";
 
 	public static final String role_insert = "INSERT INTO " + role_table + " (URI1, URI2, IDX) VALUES (?, ?, ?)";
 
 	public static final String attribute_table_literal_insert = "INSERT INTO " + attribute_table_literal + " (URI, VALUE, LANG, IDX) VALUES (?, ?, ?, ?)";
 	public static final String attribute_table_string_insert = "INSERT INTO " + attribute_table_string + " (URI, VALUE, IDX) VALUES (?, ?, ?)";
 	public static final String attribute_table_integer_insert = "INSERT INTO " + attribute_table_integer + " (URI, VALUE, IDX) VALUES (?, ?, ?)";
 	public static final String attribute_table_decimal_insert = "INSERT INTO " + attribute_table_decimal + " (URI, VALUE, IDX) VALUES (?, ?, ?)";
 	public static final String attribute_table_double_insert = "INSERT INTO " + attribute_table_double + " (URI, VALUE, IDX) VALUES (?, ?, ?)";
 	public static final String attribute_table_datetime_insert = "INSERT INTO " + attribute_table_datetime + " (URI, VALUE, IDX) VALUES (?, ?, ?)";
 	public static final String attribute_table_boolean_insert = "INSERT INTO " + attribute_table_boolean + " (URI, VALUE, IDX) VALUES (?, ?, ?)";
 
 	public static final String indexclass1 = "CREATE INDEX idxclass1 ON " + class_table + " (URI)";
 	public static final String indexclass2 = "CREATE INDEX idxclass2 ON " + class_table + " (IDX)";
 
 	public static final String indexrole1 = "CREATE INDEX idxrole1 ON " + role_table + " (URI1)";
 	public static final String indexrole2 = "CREATE INDEX idxrole2 ON " + role_table + " (IDX)";
 	public static final String indexrole3 = "CREATE INDEX idxrole3 ON " + role_table + " (URI2)";
 
 	public static final String attribute_literal_index = "idx_literal_attribute";
 	public static final String attribute_string_index = "idx_string_attribute";
 	public static final String attribute_integer_index = "idx_integer_attribute";
 	public static final String attribute_decimal_index = "idx_decimal_attribute";
 	public static final String attribute_double_index = "idx_double_attribute";
 	public static final String attribute_datetime_index = "idx_datetime_attribute";
 	public static final String attribute_boolean_index = "idx_boolean_attribute";
 	
 	public static final String indexattribute_literal1 = "CREATE INDEX " + attribute_literal_index + "1" + " ON " + attribute_table_literal + " (URI)";
 	public static final String indexattribute_string1 = "CREATE INDEX " + attribute_string_index + "1" + " ON " + attribute_table_string + " (URI)";
 	public static final String indexattribute_integer1 = "CREATE INDEX " + attribute_integer_index + "1" + " ON " + attribute_table_integer + " (URI)";
 	public static final String indexattribute_decimal1 = "CREATE INDEX " + attribute_decimal_index + "1" + " ON " + attribute_table_decimal + " (URI)";
 	public static final String indexattribute_double1 = "CREATE INDEX " + attribute_double_index + "1" + " ON " + attribute_table_double + " (URI)";
 	public static final String indexattribute_datetime1 = "CREATE INDEX " + attribute_datetime_index + "1" + " ON " + attribute_table_datetime + " (URI)";
 	public static final String indexattribute_boolean1 = "CREATE INDEX " + attribute_boolean_index + "1" + " ON " + attribute_table_boolean + " (URI)";
 
 	public static final String indexattribute_literal2 = "CREATE INDEX " + attribute_literal_index + "2" + " ON " + attribute_table_literal + " (IDX)";
 	public static final String indexattribute_string2 = "CREATE INDEX " + attribute_string_index + "2" + " ON " + attribute_table_string + " (IDX)";
 	public static final String indexattribute_integer2 = "CREATE INDEX " + attribute_integer_index + "2" + " ON " + attribute_table_integer + " (IDX)";
 	public static final String indexattribute_decimal2 = "CREATE INDEX " + attribute_decimal_index + "2" + " ON " + attribute_table_decimal + " (IDX)";
 	public static final String indexattribute_double2 = "CREATE INDEX " + attribute_double_index + "2" + " ON " + attribute_table_double + " (IDX)";
 	public static final String indexattribute_datetime2 = "CREATE INDEX " + attribute_datetime_index + "2" + " ON " + attribute_table_datetime + " (IDX)";
 	public static final String indexattribute_boolean2 = "CREATE INDEX " + attribute_boolean_index + "2" + " ON " + attribute_table_boolean + " (IDX)";
 
 	public static final String indexattribute_literal3 = "CREATE INDEX " + attribute_literal_index + "3" + " ON " + attribute_table_literal + " (VALUE)";
 	public static final String indexattribute_string3 = "CREATE INDEX " + attribute_string_index + "3" + " ON " + attribute_table_string + " (VALUE)";
 	public static final String indexattribute_integer3 = "CREATE INDEX " + attribute_integer_index + "3" + " ON " + attribute_table_integer + " (VALUE)";
 	public static final String indexattribute_decimal3 = "CREATE INDEX " + attribute_decimal_index + "3" + " ON " + attribute_table_decimal + " (VALUE)";
 	public static final String indexattribute_double3 = "CREATE INDEX " + attribute_double_index + "3" + " ON " + attribute_table_double + " (VALUE)";
 	public static final String indexattribute_datetime3 = "CREATE INDEX " + attribute_datetime_index + "3" + " ON " + attribute_table_datetime + " (VALUE)";
 	public static final String indexattribute_boolean3 = "CREATE INDEX " + attribute_boolean_index + "3" + " ON " + attribute_table_boolean + " (VALUE)";
 
 	public static final String dropindexclass1 = "DROP INDEX idxclass1";
 	public static final String dropindexclass2 = "DROP INDEX idxclass2";
 
 	public static final String dropindexrole1 = "DROP INDEX idxrole1";
 	public static final String dropindexrole2 = "DROP INDEX idxrole2";
 	public static final String dropindexrole3 = "DROP INDEX idxrole3";
 
 	public static final String dropindexattribute_literal1 = "DROP INDEX " + attribute_literal_index + "1";
 	public static final String dropindexattribute_string1 = "DROP INDEX " + attribute_string_index + "1";
 	public static final String dropindexattribute_integer1 = "DROP INDEX " + attribute_integer_index + "1";
 	public static final String dropindexattribute_decimal1 = "DROP INDEX " + attribute_decimal_index + "1";
 	public static final String dropindexattribute_double1 = "DROP INDEX " + attribute_double_index + "1";
 	public static final String dropindexattribute_datetime1 = "DROP INDEX " + attribute_datetime_index + "1";
 	public static final String dropindexattribute_boolean1 = "DROP INDEX " + attribute_boolean_index + "1";
 
 	public static final String dropindexattribute_literal2 = "DROP INDEX " + attribute_literal_index + "2";
 	public static final String dropindexattribute_string2 = "DROP INDEX " + attribute_string_index + "2";
 	public static final String dropindexattribute_integer2 = "DROP INDEX " + attribute_integer_index + "2";
 	public static final String dropindexattribute_decimal2 = "DROP INDEX " + attribute_decimal_index + "2";
 	public static final String dropindexattribute_double2 = "DROP INDEX " + attribute_double_index + "2";
 	public static final String dropindexattribute_datetime2 = "DROP INDEX " + attribute_datetime_index + "2";
 	public static final String dropindexattribute_boolean2 = "DROP INDEX " + attribute_boolean_index + "2";
 
 	public static final String dropindexattribute_literal3 = "DROP INDEX " + attribute_literal_index + "3";
 	public static final String dropindexattribute_string3 = "DROP INDEX " + attribute_string_index + "3";
 	public static final String dropindexattribute_integer3 = "DROP INDEX " + attribute_integer_index + "3";
 	public static final String dropindexattribute_decimal3 = "DROP INDEX " + attribute_decimal_index + "3";
 	public static final String dropindexattribute_double3 = "DROP INDEX " + attribute_double_index + "3";
 	public static final String dropindexattribute_datetime3 = "DROP INDEX " + attribute_datetime_index + "3";
 	public static final String dropindexattribute_boolean3 = "DROP INDEX " + attribute_boolean_index + "3";
 
 	public static final String analyze = "ANALYZE";
 
 	public static final String select_mapping_class = "SELECT URI as X FROM " + class_table;
 
 	public static final String select_mapping_class_role_left = "SELECT URI1 as X FROM " + role_table;
 
 	public static final String select_mapping_class_role_right = "SELECT URI2 as X FROM " + role_table;
 
 	public static final String select_mapping_class_attribute_literal_left = "SELECT URI as X FROM " + attribute_table_literal;
 	public static final String select_mapping_class_attribute_string_left = "SELECT URI as X FROM " + attribute_table_string;
 	public static final String select_mapping_class_attribute_integer_left = "SELECT URI as X FROM " + attribute_table_integer;
 	public static final String select_mapping_class_attribute_decimal_left = "SELECT URI as X FROM " + attribute_table_decimal;
 	public static final String select_mapping_class_attribute_double_left = "SELECT URI as X FROM " + attribute_table_double;
 	public static final String select_mapping_class_attribute_datetime_left = "SELECT URI as X FROM " + attribute_table_datetime;
 	public static final String select_mapping_class_attribute_boolean_left = "SELECT URI as X FROM " + attribute_table_boolean;
 
 	public static final String select_mapping_role = "SELECT URI1 as X, URI2 as Y FROM " + role_table;
 
 	public static final String select_mapping_role_inverse = "SELECT URI2 as X, URI1 as Y FROM " + role_table;
 
 	public static final String select_mapping_attribute_literal = "SELECT URI as X, VALUE as Y, LANG as Z FROM " + attribute_table_literal;
 	public static final String select_mapping_attribute_string = "SELECT URI as X, VALUE as Y FROM " + attribute_table_string;
 	public static final String select_mapping_attribute_integer = "SELECT URI as X, VALUE as Y FROM " + attribute_table_integer;
 	public static final String select_mapping_attribute_decimal = "SELECT URI as X, VALUE as Y FROM " + attribute_table_decimal;
 	public static final String select_mapping_attribute_double = "SELECT URI as X, VALUE as Y FROM " + attribute_table_double;
 	public static final String select_mapping_attribute_datetime = "SELECT URI as X, VALUE as Y FROM " + attribute_table_datetime;
 	public static final String select_mapping_attribute_boolean = "SELECT URI as X, VALUE as Y FROM " + attribute_table_boolean;
 
 	public static final String whereSingleCondition = "IDX = %d";
 
 	public static final String whereIntervalCondition = "IDX >= %d AND IDX <= %d";
 
 	private static final OBDADataFactory dfac = OBDADataFactoryImpl.getInstance();
 
 	private static final OntologyFactory ofac = OntologyFactoryImpl.getInstance();
 
 	private Properties config;
 
 	private DAG dag;
 
 	private DAG pureIsa;
 
 	private Ontology aboxDependencies;
 
 	private Ontology ontology;
 
 	private boolean isIndexed;
 
 	final static int CLASS_TYPE = 1;
 
 	final static int ROLE_TYPE = 2;
 
 	private static final boolean mergeUniions = false;
 
 	public RDBMSSIRepositoryManager() throws PunningException {
 		this(null);
 	}
 
 	public RDBMSSIRepositoryManager(Set<Predicate> vocabulary) throws PunningException {
 
 		if (vocabulary != null) {
 			setVocabulary(vocabulary);
 		}
 	}
 
 	@Override
 	public void setConfig(Properties config) {
 		this.config = config;
 	}
 	
 	public DAG getDAG() {
 		return dag;
 	}
 
 	@Override
 	public void setTBox(Ontology ontology) {
 
 		this.ontology = ontology;
 
 		log.debug("Ontology: {}", ontology.toString());
 
 		dag = DAGConstructor.getISADAG(ontology);
 
 		// USE THE DAG GRAPHS FOR DEBUGGING
 		//
 		// try {
 		// GraphGenerator.dumpISA(dag, "given");
 		// } catch (IOException e1) {
 		// e1.printStackTrace();
 		// }
 
 		dag.clean();
 
 		pureIsa = DAGConstructor.filterPureISA(dag);
 		aboxDependencies = DAGConstructor.getSigmaOntology(dag);
 
 		pureIsa.clean();
 		pureIsa.index();
 
 		/***
 		 * Copying the equivalences that might bet lost from the translation
 		 */
 		for (Description d : dag.equi_mappings.keySet()) {
 			pureIsa.equi_mappings.put(d, dag.equi_mappings.get(d));
 		}
 
 		// try {
 		// GraphGenerator.dumpISA(dag, "no-cycles");
 		// GraphGenerator.dumpISA(pureIsa, "isa-indexed");
 		//
 		// } catch (IOException e) {
 		//
 		// }
 
 	}
 
 	@Override
 	public String getType() {
 		return TYPE_SI;
 	}
 
 	@Override
 	public void getTablesDDL(OutputStream outstream) throws IOException {
 		log.debug("Recreating ABox tables");
 
 		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(outstream));
 
 		out.append(create_ddl);
 		out.append(";\n");
 
 		out.append(class_table_create);
 		out.append(";\n");
 
 		out.append(role_table_create);
 		out.append(";\n");
 
 		out.append(attribute_table_literal_create);
 		out.append(";\n");
 		out.append(attribute_table_string_create);
 		out.append(";\n");
 		out.append(attribute_table_integer_create);
 		out.append(";\n");
 		out.append(attribute_table_decimal_create);
 		out.append(";\n");
 		out.append(attribute_table_double_create);
 		out.append(";\n");
 		out.append(attribute_table_datetime_create);
 		out.append(";\n");
 		out.append(attribute_table_boolean_create);
 		out.append(";\n");
 		
 		out.flush();
 	}
 
 	@Override
 	public void getIndexDDL(OutputStream outstream) throws IOException {
 
 		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(outstream));
 
 		out.append(indexclass1);
 		out.append(";\n");
 		out.append(indexclass2);
 		out.append(";\n");
 		out.append(indexrole1);
 		out.append(";\n");
 		out.append(indexrole2);
 		out.append(";\n");
 		out.append(indexrole3);
 		out.append(";\n");
 
 		out.append(indexattribute_literal1);
 		out.append(";\n");
 		out.append(indexattribute_string1);
 		out.append(";\n");
 		out.append(indexattribute_integer1);
 		out.append(";\n");
 		out.append(indexattribute_decimal1);
 		out.append(";\n");
 		out.append(indexattribute_double1);
 		out.append(";\n");
 		out.append(indexattribute_datetime1);
 		out.append(";\n");
 		out.append(indexattribute_boolean1);
 		out.append(";\n");
 
 		out.append(indexattribute_literal2);
 		out.append(";\n");
 		out.append(indexattribute_string2);
 		out.append(";\n");
 		out.append(indexattribute_integer2);
 		out.append(";\n");
 		out.append(indexattribute_decimal2);
 		out.append(";\n");
 		out.append(indexattribute_double2);
 		out.append(";\n");
 		out.append(indexattribute_datetime2);
 		out.append(";\n");
 		out.append(indexattribute_boolean2);
 		out.append(";\n");
 		
 		out.append(indexattribute_literal3);
 		out.append(";\n");
 		out.append(indexattribute_string3);
 		out.append(";\n");
 		out.append(indexattribute_integer3);
 		out.append(";\n");
 		out.append(indexattribute_decimal3);
 		out.append(";\n");
 		out.append(indexattribute_double3);
 		out.append(";\n");
 		out.append(indexattribute_datetime3);
 		out.append(";\n");
 		out.append(indexattribute_boolean3);
 		out.append(";\n");
 
 		out.flush();
 	}
 
 	@Override
 	public void getSQLInserts(Iterator<Assertion> data, OutputStream outstream) throws IOException {
 
 		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(outstream));
 
 		String role_insert_str = role_insert.replace("?", "%s");
 		
 		String attribute_insert_literal_str = attribute_table_literal_insert.replace("?", "%s");
 		String attribute_insert_string_str = attribute_table_string_insert.replace("?", "%s");
 		String attribute_insert_integer_str = attribute_table_integer_insert.replace("?", "%s");
 		String attribute_insert_decimal_str = attribute_table_decimal_insert.replace("?", "%s");
 		String attribute_insert_double_str = attribute_table_double_insert.replace("?", "%s");
 		String attribute_insert_date_str = attribute_table_datetime_insert.replace("?", "%s");
 		String attribute_insert_boolean_str = attribute_table_boolean_insert.replace("?", "%s");
 		
 		String cls_insert_str = class_insert.replace("?", "%s");
 
 		while (data.hasNext()) {
 			Assertion ax = data.next();
 			if (ax instanceof DataPropertyAssertion) {
 				DataPropertyAssertion attributeABoxAssertion = (DataPropertyAssertion) ax;
 				String prop = attributeABoxAssertion.getAttribute().getName().toString();
 				String uri = attributeABoxAssertion.getObject().getURI().toString();
 				String lit = attributeABoxAssertion.getValue().getValue();
 				String lang = attributeABoxAssertion.getValue().getLanguage();
 				Predicate.COL_TYPE attributeType = attributeABoxAssertion.getValue().getType();
 				
 				Predicate propPred = dfac.getDataPropertyPredicate(URI.create(prop));
 				Property propDesc = ofac.createProperty(propPred);
 				DAGNode node = pureIsa.getRoleNode(propDesc);
 				int idx = node.getIndex();
 
 				switch(attributeType) {
 					case LITERAL:
 						out.append(String.format(attribute_insert_literal_str, getQuotedString(uri), getQuotedString(lit), getQuotedString(lang), idx));
 						break;
 					case STRING:
 						out.append(String.format(attribute_insert_string_str, getQuotedString(uri), getQuotedString(lit), idx));
 						break;
 					case INTEGER:
 						out.append(String.format(attribute_insert_integer_str, getQuotedString(uri), Integer.parseInt(lit), idx));
 						break;
 					case DECIMAL:
 						out.append(String.format(attribute_insert_decimal_str, getQuotedString(uri), parseBigDecimal(lit), idx));
 						break;
 					case DOUBLE:
 						out.append(String.format(attribute_insert_double_str, getQuotedString(uri), Double.parseDouble(lit), idx));
 						break;
 					case DATETIME:
 						out.append(String.format(attribute_insert_date_str, getQuotedString(uri), parseTimestamp(lit), idx));
 						break;
 					case BOOLEAN:
 						out.append(String.format(attribute_insert_boolean_str, getQuotedString(uri), Boolean.parseBoolean(lit), idx));
 						break;
 				}
 				
 			} else if (ax instanceof ObjectPropertyAssertion) {
 				ObjectPropertyAssertion roleABoxAssertion = (ObjectPropertyAssertion) ax;
 				String prop = roleABoxAssertion.getRole().getName().toString();
 				String uri1 = roleABoxAssertion.getFirstObject().getURI().toString();
 				String uri2 = roleABoxAssertion.getSecondObject().getURI().toString();
 
 				Predicate propPred = dfac.getObjectPropertyPredicate(URI.create(prop));
 				Property propDesc = ofac.createProperty(propPred);
 
 				if (dag.equi_mappings.containsKey(propDesc)) {
 					Property desc = (Property) dag.equi_mappings.get(propDesc);
 					if (desc.isInverse()) {
 						String tmp = uri1;
 						uri1 = uri2;
 						uri2 = tmp;
 					}
 				}
 				DAGNode node = pureIsa.getRoleNode(propDesc);
 				int idx = node.getIndex();
 
 				out.append(String.format(role_insert_str, getQuotedString(uri1), getQuotedString(uri2), idx));
 
 			} else if (ax instanceof ClassAssertion) {
 				
 				String uri = ((ClassAssertion) ax).getObject().getURI().toString();
 				Predicate clsPred = ((ClassAssertion) ax).getConcept();
 				ClassDescription clsDesc = ofac.createClass(clsPred);
 				DAGNode node = pureIsa.getClassNode(clsDesc);
 				int idx = node.getIndex();
 
 				out.append(String.format(cls_insert_str, getQuotedString(uri), idx));
 			}
 			out.append(";\n");
 		}
 		out.flush();
 	}
 
 	@Override
 	public void createDBSchema(Connection conn, boolean dropExisting) throws SQLException {
 
 		if (isDBSchemaDefined(conn)) {
 			log.debug("Schema already exists. Skipping creation");
 			return;
 		}
 
 		log.debug("Recreating data tables");
 
 		Statement st = conn.createStatement();
 
 		if (dropExisting) {
 			try {
 				dropDBSchema(conn);
 			} catch (SQLException e) {
 				log.debug(e.getMessage(), e);
 			}
 		}
 
 		st.addBatch(create_ddl);
 		st.addBatch(class_table_create);
 		st.addBatch(role_table_create);		
 		
 		st.addBatch(attribute_table_literal_create);
 		st.addBatch(attribute_table_string_create);
 		st.addBatch(attribute_table_integer_create);
 		st.addBatch(attribute_table_decimal_create);
 		st.addBatch(attribute_table_double_create);
 		st.addBatch(attribute_table_datetime_create);
 		st.addBatch(attribute_table_boolean_create);
 		
 		st.executeBatch();
 		st.close();
 	}
 
 	@Override
 	public void createIndexes(Connection conn) throws SQLException {
 		log.debug("Creating indexes");
 
 		Statement st = conn.createStatement();
 
 		st.addBatch(indexclass1);
 		st.addBatch(indexclass2);
 		st.addBatch(indexrole1);
 		st.addBatch(indexrole2);
 		st.addBatch(indexrole3);
 
 		st.addBatch(indexattribute_literal1);
 		st.addBatch(indexattribute_string1);
 		st.addBatch(indexattribute_integer1);
 		st.addBatch(indexattribute_decimal1);
 		st.addBatch(indexattribute_double1);
 		st.addBatch(indexattribute_datetime1);
 		st.addBatch(indexattribute_boolean1);
 		
 		st.addBatch(indexattribute_literal2);
 		st.addBatch(indexattribute_string2);
 		st.addBatch(indexattribute_integer2);
 		st.addBatch(indexattribute_decimal2);
 		st.addBatch(indexattribute_double2);
 		st.addBatch(indexattribute_datetime2);
 		st.addBatch(indexattribute_boolean2);
 		
 		st.addBatch(indexattribute_literal3);
 		st.addBatch(indexattribute_string3);
 		st.addBatch(indexattribute_integer3);
 		st.addBatch(indexattribute_decimal3);
 		st.addBatch(indexattribute_double3);
 		st.addBatch(indexattribute_datetime3);
 		st.addBatch(indexattribute_boolean3);
 
 		st.executeBatch();
 		st.close();
 
 		isIndexed = true;
 	}
 
 	@Override
 	public void dropDBSchema(Connection conn) throws SQLException {
 
 		Statement st = conn.createStatement();
 
 		st.addBatch(drop_dll);
 
 		st.addBatch(class_table_drop);
 		st.addBatch(role_table_drop);
 
 		st.addBatch(attribute_table_literal_drop);
 		st.addBatch(attribute_table_string_drop);
 		st.addBatch(attribute_table_integer_drop);
 		st.addBatch(attribute_table_decimal_drop);
 		st.addBatch(attribute_table_double_drop);
 		st.addBatch(attribute_table_datetime_drop);
 		st.addBatch(attribute_table_boolean_drop);
 		
 		st.executeBatch();
 		st.close();
 	}
 
 	@Override
 	public int insertData(Connection conn, Iterator<Assertion> data, int commit, int batch) throws SQLException {
 		log.debug("Inserting data into DB");
 
 		if (commit < 1) {
 			commit = -1;
 		}
 		if (batch < 1) {
 			batch = -1;
 		}
 		
 		PreparedStatement classStm = conn.prepareStatement(class_insert);
 		PreparedStatement roleStm = conn.prepareStatement(role_insert);
 		
 		PreparedStatement attributeLiteralStm = conn.prepareStatement(attribute_table_literal_insert);
 		PreparedStatement attributeStringStm = conn.prepareStatement(attribute_table_string_insert);
 		PreparedStatement attributeIntegerStm = conn.prepareStatement(attribute_table_integer_insert);
 		PreparedStatement attributeDecimalStm = conn.prepareStatement(attribute_table_decimal_insert);
 		PreparedStatement attributeDoubleStm = conn.prepareStatement(attribute_table_double_insert);
 		PreparedStatement attributeDateStm = conn.prepareStatement(attribute_table_datetime_insert);
 		PreparedStatement attributeBooleanStm = conn.prepareStatement(attribute_table_boolean_insert);
 
 		HashMap<Predicate, Integer> indexes = new HashMap<Predicate, Integer>(this.ontology.getVocabulary().size() * 2);
 
 		int insertscount = 0;
 		int batchCount = 0;
 		int commitCount = 0;
 
 		while (data.hasNext()) {
 			Assertion ax = data.next();
 			batchCount += 1;
 			commitCount += 1;
 
 			if (ax instanceof DataPropertyAssertion) {
 				// Data property coming from the TBox
 				DataPropertyAssertion attributeABoxAssertion = (DataPropertyAssertion) ax;
 				Predicate attribute = attributeABoxAssertion.getAttribute();
 				Predicate.COL_TYPE attributeType = getAttributeType(attribute);
 				
 				// Data property coming from the ABox
 				String uri = attributeABoxAssertion.getObject().getURI().toString();
 				String value = attributeABoxAssertion.getValue().getValue();
 				String lang = attributeABoxAssertion.getValue().getLanguage();
 				Predicate.COL_TYPE assertionType = attributeABoxAssertion.getValue().getType(); 
 				
 				if (attributeType != Predicate.COL_TYPE.LITERAL) {
 					if (assertionType != null) {
 						if (assertionType != attributeType) {
 							continue; // skip it!
 						}
 					}
 				}
 								
 				Integer idxc = indexes.get(attribute);
 				int idx = -1;
 				if (idxc == null) {
 					Property propDesc = ofac.createProperty(attribute);
 					DAGNode node = pureIsa.getRoleNode(propDesc);
 					idx = node.getIndex();
 					indexes.put(attribute, idx);
 				} else {
 					idx = idxc;
 				}
 
 				insertscount += 1;
 
 				switch(attributeType) {
 				case LITERAL:
 					setInputStatement(attributeLiteralStm, uri, value, lang, idx);
 					attributeLiteralStm.addBatch();
 					break;
 				case STRING:
 					setInputStatement(attributeStringStm, uri, value, idx);
 					attributeStringStm.addBatch();
 					break;
 				case INTEGER:
 					setInputStatement(attributeIntegerStm, uri, Integer.parseInt(value), idx);
 					attributeIntegerStm.addBatch();
 					break;
 				case DECIMAL:
 					setInputStatement(attributeDecimalStm, uri, parseBigDecimal(value), idx);
 					attributeDecimalStm.addBatch();
 					break;
 				case DOUBLE:
 					setInputStatement(attributeDoubleStm, uri, Double.parseDouble(value), idx);
 					attributeDoubleStm.addBatch();
 					break;
 				case DATETIME:
 					setInputStatement(attributeDateStm, uri, parseTimestamp(value), idx);
 					attributeDateStm.addBatch();
 					break;
 				case BOOLEAN:
 					value = getBooleanString(value);  // PostgreSQL abbreviates the boolean value to 't' and 'f'
 					setInputStatement(attributeBooleanStm, uri, Boolean.parseBoolean(value), idx);
 					attributeBooleanStm.addBatch();
 					break;
 				}
 
 			} else if (ax instanceof ObjectPropertyAssertion) {
 
 				ObjectPropertyAssertion roleABoxAssertion = (ObjectPropertyAssertion) ax;
 				
 				String uri1 = roleABoxAssertion.getFirstObject().getURI().toString();
 				String uri2 = roleABoxAssertion.getSecondObject().getURI().toString();
 
 				Predicate propPred = roleABoxAssertion.getRole();
 				Property propDesc = ofac.createProperty(propPred);
 
 				if (dag.equi_mappings.containsKey(propDesc)) {
 					Property desc = (Property) dag.equi_mappings.get(propDesc);
 					if (desc.isInverse()) {
 						String tmp = uri1;
 						uri1 = uri2;
 						uri2 = tmp;
 					}
 				}
 
 				int idx = -1;
 				Integer idxc = indexes.get(propPred);
 				if (idxc == null) {
 
 					DAGNode node = pureIsa.getRoleNode(propDesc);
 					if (node == null) {
 						Property desc = (Property) dag.equi_mappings.get(propDesc);
 
 						if (desc == null) {
 							log.error("Property class without node: " + propDesc);
 						}
 						Property desinv = ofac.createProperty(desc.getPredicate(), !desc.isInverse());
 						DAGNode node2 = (pureIsa.getRoleNode(desinv));
 						idx = node2.getIndex();
 					} else {
 						idx = node.getIndex();
 					}
 					indexes.put(roleABoxAssertion.getRole(), idx);
 				} else {
 					idx = idxc;
 				}
 
 				insertscount += 1;
 
 				roleStm.setString(1, uri1);
 				roleStm.setString(2, uri2);
 				roleStm.setInt(3, idx);
 				roleStm.addBatch();
 
 			} else if (ax instanceof ClassAssertion) {
 				ClassAssertion cassertion = (ClassAssertion) ax;
 				Predicate pred = cassertion.getConcept();
 
 				int idx = -1;
 				Integer idxc = indexes.get(cassertion.getConcept());
 				if (idxc == null) {
 					Predicate clsPred = cassertion.getConcept();
 					ClassDescription clsDesc = ofac.createClass(clsPred);
 					DAGNode node = pureIsa.getClassNode(clsDesc);
 					if (node == null) {
 						String cls = cassertion.getConcept().getName().toString();
 						log.error("Found class without node: " + cls.toString());
 					}
 					idx = node.getIndex();
 					indexes.put(pred, idx);
 				} else {
 					idx = idxc;
 				}
 				String uri = cassertion.getObject().getURI().toString();
 
 				insertscount += 1;
 
 				classStm.setString(1, uri);
 				classStm.setInt(2, idx);
 				classStm.addBatch();
 				// }
 			}
 
 			if (batchCount == batch) {
 				batchCount = 0;
 				roleStm.executeBatch();
 				roleStm.clearBatch();
 
 				attributeLiteralStm.executeBatch();
 				attributeLiteralStm.clearBatch();
 				
 				attributeStringStm.executeBatch();
 				attributeStringStm.clearBatch();
 				
 				attributeIntegerStm.executeBatch();
 				attributeIntegerStm.clearBatch();
 				
 				attributeDecimalStm.executeBatch();
 				attributeDecimalStm.clearBatch();
 				
 				attributeDoubleStm.executeBatch();
 				attributeDoubleStm.clearBatch();
 				
 				attributeDateStm.executeBatch();
 				attributeDateStm.clearBatch();
 				
 				attributeBooleanStm.executeBatch();
 				attributeBooleanStm.clearBatch();
 
 				classStm.executeBatch();
 				classStm.clearBatch();
 			}
 			if (commitCount == commit) {
 				commitCount = 0;
 				conn.commit();
 			}
 		}
 
 		roleStm.executeBatch();
 		roleStm.clearBatch();
 		roleStm.close();
 
 		attributeLiteralStm.executeBatch();
 		attributeLiteralStm.clearBatch();
 		attributeLiteralStm.close();
 		
 		attributeStringStm.executeBatch();
 		attributeStringStm.clearBatch();
 		attributeStringStm.close();
 		
 		attributeIntegerStm.executeBatch();
 		attributeIntegerStm.clearBatch();
 		attributeIntegerStm.close();
 		
 		attributeDecimalStm.executeBatch();
 		attributeDecimalStm.clearBatch();
 		attributeDecimalStm.close();
 		
 		attributeDoubleStm.executeBatch();
 		attributeDoubleStm.clearBatch();
 		attributeDoubleStm.close();
 		
 		attributeDateStm.executeBatch();
 		attributeDateStm.clearBatch();
 		attributeDateStm.close();
 		
 		attributeBooleanStm.executeBatch();
 		attributeBooleanStm.clearBatch();
 		attributeBooleanStm.close();
 		
 		classStm.executeBatch();
 		classStm.clearBatch();
 		classStm.close();
 
 		if (commit != -1) {
 			conn.commit();
 		}
 		log.debug("Total tuples inserted: {}", insertscount);
 		return insertscount;
 
 	}
 
 	private String getBooleanString(String value) {
 		if (value.equalsIgnoreCase("t") || value.equals("1")) {
 			return "true";
 		} else if (value.equalsIgnoreCase("f") || value.equals("0")) {
 			return "false";
 		} else {
 			return value;  // nothing change
 		}
 	}
 
 	private void setInputStatement(PreparedStatement stm, String uri, String value, String lang, int idx) throws SQLException {
 		stm.setString(1, uri);
 		stm.setString(2, value);
 		stm.setString(3, lang);
 		stm.setInt(4, idx);
 	}
 	
 	private void setInputStatement(PreparedStatement stm, String uri, String value, int idx) throws SQLException {
 		stm.setString(1, uri);
 		stm.setString(2, value);
 		stm.setInt(3, idx);
 	}
 
 	private void setInputStatement(PreparedStatement stm, String uri, int value, int idx) throws SQLException {
 		stm.setString(1, uri);
 		stm.setInt(2, value);
 		stm.setInt(3, idx);
 	}
 	
 	private void setInputStatement(PreparedStatement stm, String uri, BigDecimal value, int idx) throws SQLException {
 		stm.setString(1, uri);
 		stm.setBigDecimal(2, value);
 		stm.setInt(3, idx);
 	}
 	
 	private void setInputStatement(PreparedStatement stm, String uri, double value, int idx) throws SQLException {
 		stm.setString(1, uri);
 		stm.setDouble(2, value);
 		stm.setInt(3, idx);
 	}
 	
 	private void setInputStatement(PreparedStatement stm, String uri, Timestamp value, int idx) throws SQLException {
 		stm.setString(1, uri);										
 		stm.setTimestamp(2, value);
 		stm.setInt(3, idx);
 	}
 	
 	private void setInputStatement(PreparedStatement stm, String uri, boolean value, int idx) throws SQLException {
 		stm.setString(1, uri);
 		stm.setBoolean(2, value);
 		stm.setInt(3, idx);
 	}
 
 	private BigDecimal parseBigDecimal(String value) {
 		return new BigDecimal(value);
 	}
 	
 	private Timestamp parseTimestamp(String lit) {
 		final String[] formatStrings = {
 				"yyyy-MM-dd HH:mm:ss.SS",
 				"yyyy-MM-dd HH:mm:ss.S",
 				"yyyy-MM-dd HH:mm:ss",
 				"yyyy-MM-dd",
 				"yyyy-MM-dd'T'HH:mm:ssz",
 		};
 		
 		for (String formatString : formatStrings) {
 	        try {
 	        	long time = new SimpleDateFormat(formatString).parse(lit).getTime();
 	        	Timestamp ts = new Timestamp(time);
 	        	return ts;
 	        } catch (ParseException e) { }
 	    }
     	return null; // the string can't be parsed to one of the datetime formats.
 	}
 	
 	private COL_TYPE getAttributeType(Predicate attribute) {		
 		PropertySomeRestriction role = ofac.getPropertySomeRestriction(attribute, true);
 		DAGNode roleNode = dag.get(role);
 		Set<DAGNode> ancestors = roleNode.getAncestors();
 		
 		for (DAGNode node : ancestors) {
 			Description desc = node.getDescription();
 			if (desc instanceof DataType) {
 				DataType datatype = (DataType) desc;
 				return datatype.getPredicate().getType(0);  // TODO Put some check for multiple types
 			}
 		}
 		return COL_TYPE.LITERAL;
 	}
 
 	@Override
 	public Ontology getABoxDependencies() {
 		return aboxDependencies;
 	}
 
 	@Override
 	public void loadMetadata(Connection conn) throws SQLException {
 		log.debug("Checking if SemanticIndex exists in DB");
 
 		Map<Description, DAGNode> res_classes = new HashMap<Description, DAGNode>();
 		Map<Description, DAGNode> res_roles = new HashMap<Description, DAGNode>();
 		Map<Description, DAGNode> res_allnodes = new HashMap<Description, DAGNode>();
 		Statement st = conn.createStatement();
 		ResultSet res_rows = st.executeQuery(select_query);
 		while (res_rows.next()) {
 			String uri = res_rows.getString(1);
 			int idx = res_rows.getInt(2);
 			int start_idx = res_rows.getInt(3);
 			int end_idx = res_rows.getInt(4);
 			int type = res_rows.getInt(5);
 
 			Predicate p;
 			if (type == CLASS_TYPE) {
 
 				boolean exists = false;
 				boolean inverse = false;
 
 				// ExistentialNode
 				if (uri.startsWith("E")) {
 					exists = true;
 					uri = uri.substring(1);
 				}
 				// Inverse
 				if (uri.endsWith("-")) {
 					uri = uri.substring(0, uri.length() - 2);
 					inverse = true;
 				}
 				ClassDescription description;
 
 				if (exists) {
 					p = dfac.getPredicate(URI.create(uri), 2);
 					description = ofac.getPropertySomeRestriction(p, inverse);
 				} else {
 					p = dfac.getClassPredicate(URI.create(uri));
 					description = ofac.createClass(p);
 				}
 
 				if (res_classes.containsKey(description)) {
 					res_classes.get(description).getRange().addInterval(start_idx, end_idx);
 				} else {
 					DAGNode node = new DAGNode(description);
 					node.setIndex(idx);
 					node.setRange(new SemanticIndexRange(start_idx, end_idx));
 					res_classes.put(description, node);
 					res_allnodes.put(description, node);
 				}
 
 			} else if (type == ROLE_TYPE) {
 
 				Property description;
 				boolean inverse = false;
 
 				// Inverse
 				if (uri.endsWith("-")) {
 					uri = uri.substring(0, uri.length() - 2);
 					inverse = true;
 				}
 				p = dfac.getObjectPropertyPredicate(URI.create(uri));
 				description = ofac.createProperty(p, inverse);
 
 				if (res_roles.containsKey(description)) {
 					res_roles.get(description).getRange().addInterval(start_idx, end_idx);
 				} else {
 					DAGNode node = new DAGNode(description);
 					node.setIndex(idx);
 					node.setRange(new SemanticIndexRange(start_idx, end_idx));
 					res_roles.put(description, node);
 					res_allnodes.put(description, node);
 				}
 			}
 		}
 		res_rows.close();
 		st.close();
 
 		dag = new DAG(res_classes, res_roles, new HashMap<Description, Description>(), res_allnodes);
 		pureIsa = DAGConstructor.filterPureISA(dag);
 	}
 
 	@Override
 	public boolean checkMetadata(Connection conn) throws SQLException {
 		return true;
 	}
 
 	@Override
 	public Collection<OBDAMappingAxiom> getMappings() {
 
 		/*
 		 * 
 		 * PART 1: Collecting relevant nodes for mappings
 		 */
 
 		/*
 		 * Collecting relevant nodes for each role. For a Role P, the relevant
 		 * nodes are, the DAGNode for P, and the top most inverse children of P
 		 */
 		DAGOperations.buildAncestors(dag);
 		DAGOperations.buildDescendants(dag);
 
 		// try {
 		// GraphGenerator.dumpISA(dag,"sidag");
 		// } catch (IOException e) {
 		// // e.printStackTrace();
 		// }
 
 		Set<DAGNode> roleNodes = new HashSet<DAGNode>();
 		Map<DAGNode, List<DAGNode>> roleInverseMaps = new HashMap<DAGNode, List<DAGNode>>();
 
 		Set<Predicate> roles = ontology.getRoles();
 		for (Predicate rolepred : roles) {
 
 			DAGNode node = dag.getRoleNode(ofac.createProperty(rolepred));
 			// We only map named roles
 			if (!(node.getDescription() instanceof Property) || ((Property) node.getDescription()).isInverse()) {
 				continue;
 			}
 			roleNodes.add(node);
 
 			List<DAGNode> roleInverseChildren = roleInverseMaps.get(node);
 			if (roleInverseChildren == null) {
 				roleInverseChildren = new LinkedList<DAGNode>();
 				roleInverseMaps.put(node, roleInverseChildren);
 			}
 
 			/*
 			 * collecting the top most inverse children, we do a bredth first
 			 * traversal, stopping a branch when we find an inverse child.
 			 * 
 			 * Collecting the top most allows us to avoid redundancy elimination
 			 */
 			Queue<DAGNode> childrenQueue = new LinkedList<DAGNode>();
 			childrenQueue.addAll(node.getChildren());
 			childrenQueue.addAll(node.getEquivalents());
 
 			while (!childrenQueue.isEmpty()) {
 				DAGNode child = childrenQueue.poll();
 				if ((child.getDescription() instanceof Property) && ((Property) child.getDescription()).isInverse()) {
 					roleInverseChildren.add(child);
 				} else {
 					childrenQueue.addAll(child.getChildren());
 				}
 			}
 
 			/* Removing redundant nodes */
 
 			HashSet<DAGNode> inverseRedundants = new HashSet<DAGNode>();
 			for (DAGNode inverseNode : roleInverseChildren) {
 				Property role = ((Property) inverseNode.getDescription());
 				for (DAGNode possibleRedundantNode : roleInverseChildren) {
 					Property possibleRedundantRole = ((Property) possibleRedundantNode.getDescription());
 					if (dag.getRoleNode(role).getDescendants().contains(possibleRedundantRole))
 						inverseRedundants.add(possibleRedundantNode);
 				}
 			}
 			roleInverseChildren.removeAll(inverseRedundants);
 
 		}
 
 		/*
 		 * Collecting relevant nodes for each class, that is, the Node itself,
 		 * and each exists R such that there is no other exists P, such that R
 		 * isa P
 		 * 
 		 * Here we cannot collect only the top most, so we do it in two passes.
 		 * First we callect all exsts R children, then we remove redundant ones.
 		 */
 
 		// TODO this part can be optimized if we know some existing dependencies
 		// (e.g., coming from given mappings)
 
 		Set<DAGNode> classNodesMaps = new HashSet<DAGNode>();
 		Map<DAGNode, Set<DAGNode>> classExistsMaps = new HashMap<DAGNode, Set<DAGNode>>();
 		for (DAGNode node : dag.getClasses()) {
 			// we only map named classes
 			if (!(node.getDescription() instanceof OClass)) {
 				continue;
 			}
 			classNodesMaps.add(node);
 
 			Set<DAGNode> existChildren = classExistsMaps.get(node);
 			if (existChildren == null) {
 				existChildren = new HashSet<DAGNode>();
 				classExistsMaps.put(node, existChildren);
 			}
 
 			/* Collecting Exists R children */
 			for (DAGNode child : node.getDescendants()) {
 				if (child.getDescription() instanceof PropertySomeRestrictionImpl) {
 					existChildren.add(child);
 				}
 			}
 
 			/*
 			 * Cleaning exists children (removing any exists R implied by the
 			 * role hierarchy )
 			 */
 			// Set<DAGNode> existChildren = classExistsMaps.get(node);
 			Set<DAGNode> redundantNodes = new HashSet<DAGNode>();
 			for (DAGNode existsnode : existChildren) {
 				/* Here we have ES */
 				PropertySomeRestriction existsDesc = (PropertySomeRestriction) existsnode.getDescription();
 				Property role = ofac.createProperty(existsDesc.getPredicate(), existsDesc.isInverse());
 				DAGNode roleNode = dag.getRoleNode(role);
 
 				for (DAGNode possiblyRedundantNode : existChildren) {
 					/* Here we have ER */
 					PropertySomeRestriction existsDesc2 = (PropertySomeRestriction) possiblyRedundantNode.getDescription();
 					Property role2 = ofac.createProperty(existsDesc2.getPredicate(), existsDesc2.isInverse());
 					DAGNode roleNode2 = dag.getRoleNode(role2);
 
 					if (roleNode.getDescendants().contains(roleNode2))
 						/*
 						 * The DAG implies that R ISA S, so we remove ER
 						 */
 						redundantNodes.add(possiblyRedundantNode);
 				}
 			}
 			existChildren.removeAll(redundantNodes);
 		}
 
 		/*
 		 * We collected all classes and properties that need mappings, and the
 		 * nodes that are relevant for each of their mappings
 		 */
 
 		/*
 		 * PART 2: Creating the mappings
 		 * 
 		 * Note, at every step we always use the pureIsa dag to get the indexes
 		 * and ranges for each class.
 		 */
 
 		// Creating the mappings for each role
 
 		Map<Predicate, List<OBDAMappingAxiom>> mappings = new HashMap<Predicate, List<OBDAMappingAxiom>>();
 
 		for (DAGNode roleNode : roleNodes) {
 
 			// Get the description of the role node, i.e., a Property object.
 			Property property = (Property) roleNode.getDescription();
 			
 			// Get the property predicate
 			Predicate role = property.getPredicate();
 			
 			// Get the indexed node (from the pureIsa dag)
 			DAGNode indexedNode = pureIsa.getRoleNode(property);
 			
 			// We need to make sure we make no mappings for Auxiliary roles 
 			// introduced by the Ontology translation process.
 			if (role.toString().contains(OntologyImpl.AUXROLEURI)) {
 				continue;
 			}
 
 			List<OBDAMappingAxiom> currentMappings = new LinkedList<OBDAMappingAxiom>();
 			mappings.put(role, currentMappings);
 			
 			// Mapping Target Query
 			CQIE targetQuery = constructTargetQuery(role);
 
 			// Mapping Source Query
 			String sourceQuery = constructSourceQuery(role, indexedNode);
 
 			// Construct the mapping axiom
 			OBDAMappingAxiom basicmapping = dfac.getRDBMSMappingAxiom(sourceQuery, targetQuery);
 			currentMappings.add(basicmapping);
 			
 			/* Rest mappings: computing mappings for inverses */
 
 			if (roleInverseMaps.get(roleNode).size() > 0) {
 				StringBuffer sql = new StringBuffer();
 				sql.append(select_mapping_role_inverse);
 				sql.append(" WHERE ");
 
 				boolean alreadyAppendedOne = false;
 
 				for (DAGNode inverseSubNodes : roleInverseMaps.get(roleNode)) {
 
 					/*
 					 * Getting the indexed node (from the pureIsa dag)
 					 */
 
 					Property inverseRole = (Property) inverseSubNodes.getDescription();
 					Property directRole = ofac.createProperty(inverseRole.getPredicate());
 
 					indexedNode = pureIsa.getRoleNode(directRole);
 
 					if (indexedNode != null) {
 						List<Interval> intervals = indexedNode.getRange().getIntervals();
 
 						for (int intervali = 0; intervali < intervals.size(); intervali++) {
 							if (alreadyAppendedOne)
 								sql.append(" OR ");
 							appendIntervalString(intervals.get(intervali), sql);
 							alreadyAppendedOne = true;
 						}
 					}
 				}
 				if (alreadyAppendedOne) {
 					OBDAMappingAxiom inverseMapping = dfac.getRDBMSMappingAxiom(sql.toString(), targetQuery);
 					currentMappings.add(inverseMapping);
 				}
 			}
 
 			/*
 			 * Generating mappings for the equivalent nodes
 			 */
 
 			for (DAGNode equivalent : roleNode.getEquivalents()) {
 
 				Property equiproperty = (Property) equivalent.getDescription();
 
 				if (equiproperty.isInverse()) {
 					Property directEquiProperty = ofac.createProperty(equiproperty.getPredicate(), false);
 					if ((pureIsa.getRoleNode(directEquiProperty) != null) && (pureIsa.getRoleNode(directEquiProperty).getIndex() != -1))
 						continue;
 				}
 
 				Atom headequi = dfac.getAtom(dfac.getPredicate(URI.create("m"), 2),
 						dfac.getVariable("X"), dfac.getVariable("Y"));
 
 				Atom bodyequi = null;
 				if (!equiproperty.isInverse()) {
 					bodyequi = dfac.getAtom(equiproperty.getPredicate(), dfac.getVariable("X"), dfac.getVariable("Y"));
 				} else {
 					bodyequi = dfac.getAtom(equiproperty.getPredicate(), dfac.getVariable("Y"), dfac.getVariable("X"));
 				}
 
 				CQIE targetQueryEqui = dfac.getCQIE(headequi, bodyequi);
 
 				List<OBDAMappingAxiom> equimappings = new LinkedList<OBDAMappingAxiom>();
 				mappings.put(equiproperty.getPredicate(), equimappings);
 
 				for (OBDAMappingAxiom mapping : currentMappings) {
 					equimappings.add(dfac.getRDBMSMappingAxiom(mapping.getSourceQuery().toString(), targetQueryEqui));
 				}
 			}
 		}
 
 		/*
 		 * Creating mappings for each concept
 		 */
 
 		for (DAGNode classNode : classNodesMaps) {
 
 			Predicate classuri = ((OClass) classNode.getDescription()).getPredicate();
 
 			List<OBDAMappingAxiom> currentMappings = new LinkedList<OBDAMappingAxiom>();
 
 			mappings.put(classuri, currentMappings);
 
 			// Mapping head
 
 			Atom head = dfac.getAtom(dfac.getPredicate(URI.create("m"), 1), dfac.getVariable("X"));
 			Atom body = dfac.getAtom(classuri, dfac.getVariable("X"));
 
 			/*
 			 * This target query is shared by all mappings for this class
 			 */
 
 			CQIE targetQuery = dfac.getCQIE(head, body);
 
 			/*
 			 * First mapping: Getting the SQL for the *BASIC* mapping using
 			 * ranges
 			 */
 
 			StringBuffer sql = new StringBuffer();
 			sql.append(select_mapping_class);
 			sql.append(" WHERE ");
 
 			/*
 			 * Getting the indexed node (from the pureIsa dag)
 			 */
 			DAGNode indexedNode = pureIsa.getClassNode((OClass) classNode.getDescription());
 			List<Interval> intervals = indexedNode.getRange().getIntervals();
 			appendIntervalString(intervals.get(0), sql);
 
 			for (int intervali = 1; intervali < intervals.size(); intervali++) {
 				sql.append(" OR ");
 				appendIntervalString(intervals.get(intervali), sql);
 			}
 
 			OBDAMappingAxiom basicmapping = dfac.getRDBMSMappingAxiom(sql.toString(), targetQuery);
 			currentMappings.add(basicmapping);
 
 			/*
 			 * Rest mappings 1: computing mappings for all exists R children
 			 * such that R is a role (Object Property)
 			 */
 
 			Set<DAGNode> nodeList = classExistsMaps.get(classNode);
 			
 			// Create the mapping for role (or object property) node(s).
 			StringBuffer sqlroledirect = new StringBuffer();
 			boolean hasNode = createMappingForRole(nodeList, sqlroledirect);
 			if (hasNode) {
 				OBDAMappingAxiom existsMapping = dfac.getRDBMSMappingAxiom(sqlroledirect.toString(), targetQuery);
 				currentMappings.add(existsMapping);
 			}
 			
 			// Create the mapping for inverse role (or inverse object property) node(s).
 			StringBuffer sqlroleinverse = new StringBuffer();
 			hasNode = createMappingForInverseRole(nodeList, sqlroleinverse);
 			if (hasNode) {
 				OBDAMappingAxiom existsMapping = dfac.getRDBMSMappingAxiom(sqlroleinverse.toString(), targetQuery);
 				currentMappings.add(existsMapping);
 			}
 
 			/*
 			 * Rest mappings 2: computing mappings for all exists R children
 			 * such that R is a attribute (Data Property)
 			 */
 			
 			// Create the mapping for data property node(s) with range rdfs:Literal data type
 			StringBuffer sqlattribute = new StringBuffer();
 			hasNode = createMappingForLiteralDataType(nodeList, sqlattribute);
 			if (hasNode) {
 				OBDAMappingAxiom existsMapping = dfac.getRDBMSMappingAxiom(sqlattribute.toString(), targetQuery);
 				currentMappings.add(existsMapping);
 			}
 			
 			// Create the mapping for data property node(s) with range xsd:string data type
 			sqlattribute = new StringBuffer();
 			hasNode = createMappingForStringDataType(nodeList, sqlattribute);
 			if (hasNode) {
 				OBDAMappingAxiom existsMapping = dfac.getRDBMSMappingAxiom(sqlattribute.toString(), targetQuery);
 				currentMappings.add(existsMapping);
 			}
 			
 			// Create the mapping for data property node(s) with range xsd:int data type
 			sqlattribute = new StringBuffer();
 			hasNode = createMappingForIntegerDataType(nodeList, sqlattribute);
 			if (hasNode) {
 				OBDAMappingAxiom existsMapping = dfac.getRDBMSMappingAxiom(sqlattribute.toString(), targetQuery);
 				currentMappings.add(existsMapping);
 			}
 			
 			// Create the mapping for data property node(s) with range xsd:decimal data type
 			sqlattribute = new StringBuffer();
 			hasNode = createMappingForDecimalDataType(nodeList, sqlattribute);
 			if (hasNode) {
 				OBDAMappingAxiom existsMapping = dfac.getRDBMSMappingAxiom(sqlattribute.toString(), targetQuery);
 				currentMappings.add(existsMapping);
 			}
 			
 			// Create the mapping for data property node(s) with range xsd:double data type
 			sqlattribute = new StringBuffer();
 			hasNode = createMappingForDoubleDataType(nodeList, sqlattribute);
 			if (hasNode) {
 				OBDAMappingAxiom existsMapping = dfac.getRDBMSMappingAxiom(sqlattribute.toString(), targetQuery);
 				currentMappings.add(existsMapping);
 			}
 			
 			// Create the mapping for data property node(s) with range xsd:date data type
 			sqlattribute = new StringBuffer();
 			hasNode = createMappingForDateDataType(nodeList, sqlattribute);
 			if (hasNode) {
 				OBDAMappingAxiom existsMapping = dfac.getRDBMSMappingAxiom(sqlattribute.toString(), targetQuery);
 				currentMappings.add(existsMapping);
 			}
 			
 			// Create the mapping for data property node(s) with range xsd:boolean data type
 			sqlattribute = new StringBuffer();
 			hasNode = createMappingForBooleanDataType(nodeList, sqlattribute);
 			if (hasNode) {
 				OBDAMappingAxiom existsMapping = dfac.getRDBMSMappingAxiom(sqlattribute.toString(), targetQuery);
 				currentMappings.add(existsMapping);
 			}
 
 			/*
 			 * Generating mappings for the equivalent nodes
 			 */
 
 			for (DAGNode equivalent : classNode.getEquivalents()) {
 				if (!(equivalent.getDescription() instanceof OClass)) {
 					continue;
 				}
 				OClass equiclass = (OClass) equivalent.getDescription();
 				Atom headequi = dfac.getAtom(dfac.getPredicate(URI.create("m"), 1),
 						dfac.getVariable("X"));
 				Atom bodyequi = dfac.getAtom(equiclass.getPredicate(), dfac.getVariable("X"));
 
 				CQIE targetQueryEqui = dfac.getCQIE(headequi, bodyequi);
 
 				List<OBDAMappingAxiom> equimappings = new LinkedList<OBDAMappingAxiom>();
 				mappings.put(equiclass.getPredicate(), equimappings);
 
 				for (OBDAMappingAxiom mapping : currentMappings) {
 					equimappings.add(dfac.getRDBMSMappingAxiom(mapping.getSourceQuery().toString(), targetQueryEqui));
 				}
 			}
 		}
 
 		/*
 		 * PART 4: Optimizing.
 		 */
 
 		// Merging multiple mappings into 1 with UNION ALL to minimize the
 		// number of the mappings.
 
 		if (mergeUniions) {
 			for (Predicate predicate : mappings.keySet()) {
 
 				List<OBDAMappingAxiom> currentMappings = mappings.get(predicate);
 
 				/* Getting the current head */
 				CQIE targetQuery = (CQIE) currentMappings.get(0).getTargetQuery();
 
 				/* Computing the merged SQL */
 				StringBuffer newSQL = new StringBuffer();
 				newSQL.append(((OBDASQLQuery) currentMappings.get(0).getSourceQuery()).toString());
 				for (int mapi = 1; mapi < currentMappings.size(); mapi++) {
 					newSQL.append(" UNION ALL ");
 					newSQL.append(((OBDASQLQuery) currentMappings.get(mapi).getSourceQuery()).toString());
 				}
 
 				/* Replacing the old mappings */
 				OBDAMappingAxiom mergedMapping = dfac.getRDBMSMappingAxiom(newSQL.toString(), targetQuery);
 				currentMappings.clear();
 				currentMappings.add(mergedMapping);
 			}
 		}
 
 		/*
 		 * Collecting the result
 		 */
 		Collection<OBDAMappingAxiom> result = new LinkedList<OBDAMappingAxiom>();
 		for (Predicate predicate : mappings.keySet()) {
 			log.debug("Predicate: {} Mappings: {}", predicate, mappings.get(predicate).size());
 			result.addAll(mappings.get(predicate));
 		}
 		log.debug("Total: {} mappings", result.size());
 		return result;
 	}
 	
 	private CQIE constructTargetQuery(Predicate predicate) {
 		// Initialize the predicate and term objects.
 		Predicate headPredicate, bodyPredicate = null;
 		List<Term> headTerms = new ArrayList<Term>();
 		List<Term> bodyTerms = new ArrayList<Term>();
 		if (isObjectProperty(predicate)) {
 			// If the predicate is a Object Property
			headPredicate = dfac.getPredicate(URI.create("m"), 2, new COL_TYPE[] { COL_TYPE.STRING, COL_TYPE.STRING });
 			headTerms.add(dfac.getVariable("X"));
 			headTerms.add(dfac.getVariable("Y"));				
 
 			bodyPredicate = predicate; // the body
 			bodyTerms.add(dfac.getVariable("X"));
 			bodyTerms.add(dfac.getVariable("Y"));
 		} else {
 			// If the predicate is a Data Property
 			if (isLiteralDataProperty(predicate)) {		
 				// if the property has Literal type
				headPredicate = dfac.getPredicate(URI.create("m"), 3, new COL_TYPE[] { COL_TYPE.STRING, COL_TYPE.STRING, COL_TYPE.STRING });
 				headTerms.add(dfac.getVariable("X"));
 				headTerms.add(dfac.getVariable("Y"));
 				headTerms.add(dfac.getVariable("Z"));
 	
 				bodyPredicate = predicate; // the body
 				bodyTerms.add(dfac.getVariable("X"));
 				bodyTerms.add(dfac.getFunctionalTerm(dfac.getDataTypePredicateLiteral(), dfac.getVariable("Y"), dfac.getVariable("Z")));
 			} else {
 				// If the property has other types.
 				headTerms.add(dfac.getVariable("X"));
 				headTerms.add(dfac.getVariable("Y"));
 				headPredicate = dfac.getPredicate(URI.create("m"), 2, new COL_TYPE[] { COL_TYPE.STRING, getAttributeType(predicate) });
 	
 				bodyPredicate = predicate; // the body
 				bodyTerms.add(dfac.getVariable("X"));
 				bodyTerms.add(dfac.getVariable("Y"));
 			}						
 		}
 		Atom head = dfac.getAtom(headPredicate, headTerms);
 		Atom body = dfac.getAtom(bodyPredicate, bodyTerms);
 		return dfac.getCQIE(head, body);
 	}
 
 	private String constructSourceQuery(Predicate predicate, DAGNode node) {
 		StringBuffer sql = new StringBuffer();
 		if (isObjectProperty(predicate)) {
 			sql.append(select_mapping_role);
 		} else {
 			COL_TYPE type = getAttributeType(predicate);
 			switch (type) {
 				case LITERAL: sql.append(select_mapping_attribute_literal); break;
 				case STRING: sql.append(select_mapping_attribute_string); break;
 				case INTEGER: sql.append(select_mapping_attribute_integer); break;
 				case DECIMAL: sql.append(select_mapping_attribute_decimal); break;
 				case DOUBLE: sql.append(select_mapping_attribute_double); break;
 				case DATETIME: sql.append(select_mapping_attribute_datetime); break;
 				case BOOLEAN: sql.append(select_mapping_attribute_boolean); break;
 			}
 		}
 		sql.append(" WHERE ");
 
 		List<Interval> intervals = node.getRange().getIntervals();
 		appendIntervalString(intervals.get(0), sql);
 
 		for (int intervali = 1; intervali < intervals.size(); intervali++) {
 			sql.append(" OR ");
 			appendIntervalString(intervals.get(intervali), sql);
 		}
 		return sql.toString();
 	}
 
 	private boolean isObjectProperty(Predicate role) {
 		return role.getType(1) == COL_TYPE.OBJECT;
 	}
 	
 	private boolean isLiteralDataProperty(Predicate role) {
 		return getAttributeType(role) == COL_TYPE.LITERAL;
 	}
 	
 	/**
 	 * Constructs the mappings for all roles (or object properties) in the DAG node list. The string 
 	 * buffer stores the mapping string, if any. The method returns true if it finds at least one role 
 	 * node.
 	 * 
 	 * @param nodeList
 	 * 			The list of existential class nodes.
 	 * @param buffer
 	 * 			The string buffer to stores the mapping string
 	 * @return Returns true if the method finds at least one role node, or false otherwise.
 	 */
 	private boolean createMappingForRole(Set<DAGNode> nodeList, StringBuffer buffer) {
 
 		boolean hasRoleNode = false;   // A flag if there is at least one role
 		
 		buffer.append(select_mapping_class_role_left);
 		buffer.append(" WHERE ");
 		
 		boolean multipleIntervals = false;  // A flag to tell there are more than one SI intervals.
 		for (DAGNode node : nodeList) {
 			PropertySomeRestriction property = (PropertySomeRestriction) node.getDescription();
 			boolean isObjectProperty = property.getPredicate().getType(1) == COL_TYPE.OBJECT;
 			if (isObjectProperty) {
 				if (!property.isInverse()) {
 					Property role = ofac.createProperty(property.getPredicate(), false);
 					DAGNode indexedNode = pureIsa.getRoleNode(role);
 					if (indexedNode != null) {
 						hasRoleNode = true;
 						List<Interval> intervals = indexedNode.getRange().getIntervals();
 						for (int i = 0; i < intervals.size(); i++) {
 							if (multipleIntervals) {
 								buffer.append(" OR ");
 							}
 							appendIntervalString(intervals.get(i), buffer);
 							multipleIntervals = true;
 						}
 					}
 				}
 			}
 		}
 		return hasRoleNode;
 	}
 
 	/**
 	 * Constructs the mappings for all inverse roles (or inverse object properties) in the DAG node list. 
 	 * The string buffer stores the mapping string, if any. The method returns true if it finds at least 
 	 * one inverse role node.
 	 * 
 	 * @param nodeList
 	 * 			The list of existential class nodes.
 	 * @param buffer
 	 * 			The string buffer to stores the mapping string
 	 * @return Returns true if the method finds at least one inverse role node, or false otherwise.
 	 */
 	private boolean createMappingForInverseRole(Set<DAGNode> nodeList, StringBuffer buffer) {
 		
 		boolean hasInverseRoleNode = false;  //  A flag if there is at least one inverse role.
 		
 		buffer.append(select_mapping_class_role_right);
 		buffer.append(" WHERE ");
 
 		boolean multipleIntervals = false;  // A flag to tell there are more than one SI intervals.
 		for (DAGNode node : nodeList) {
 			PropertySomeRestriction property = (PropertySomeRestriction) node.getDescription();
 			boolean isObjectProperty = property.getPredicate().getType(1) == COL_TYPE.OBJECT;
 			if (isObjectProperty) {
 				if (property.isInverse()) {
 					Property role = ofac.createProperty(property.getPredicate(), false);
 					DAGNode indexedNode = pureIsa.getRoleNode(role);
 					if (indexedNode != null) {
 						hasInverseRoleNode = true;
 						List<Interval> intervals = indexedNode.getRange().getIntervals();
 						for (int i = 0; i < intervals.size(); i++) {
 							if (multipleIntervals) {
 								buffer.append(" OR ");
 							}
 							appendIntervalString(intervals.get(i), buffer);
 							multipleIntervals = true;
 						}
 					}
 				}
 			}
 		}
 		return hasInverseRoleNode;
 	}
 
 	/**
 	 * Constructs the mappings for all data properties with range rdfs:Literal in the DAG node list. 
 	 * The string buffer stores the mapping string, if any. The method returns true if it finds at 
 	 * least one of the node.
 	 * 
 	 * @param nodeList
 	 * 			The list of existential class nodes.
 	 * @param buffer
 	 * 			The string buffer to stores the mapping string
 	 * @return Returns true if the method finds at least one data property node with rdfs:Literal as
 	 * the range, or false otherwise.
 	 */
 	private boolean createMappingForLiteralDataType(Set<DAGNode> nodeList, StringBuffer buffer) {
 		
 		boolean hasLiteralNode = false;  //  A flag if there is  at least one DP with range rdfs:Literal
 		
 		buffer.append(select_mapping_class_attribute_literal_left);
 		buffer.append(" WHERE ");
 
 		boolean multipleIntervals = false;  // A flag to tell there are more than one SI interval.
 		for (DAGNode node : nodeList) {
 			Predicate property = ((PropertySomeRestriction) node.getDescription()).getPredicate();
 			boolean isObjectProperty = (property.getType(1) == COL_TYPE.OBJECT);
 			if (!isObjectProperty) {
 				COL_TYPE dateType = getAttributeType(property);
 				if (dateType == COL_TYPE.LITERAL) {
 					PropertySomeRestriction existsDesc = (PropertySomeRestriction) node.getDescription();
 					Property role = ofac.createProperty(existsDesc.getPredicate(), false);
 					DAGNode indexedNode = pureIsa.getRoleNode(role);  // Get the indexed node.
 					if (indexedNode != null) {
 						hasLiteralNode = true;
 						List<Interval> intervals = indexedNode.getRange().getIntervals();	
 						for (int i = 0; i < intervals.size(); i++) {
 							if (multipleIntervals) {
 								buffer.append(" OR ");
 							}
 							appendIntervalString(intervals.get(i), buffer);
 							multipleIntervals = true;	
 						}
 					}
 				}
 			}
 		}
 		return hasLiteralNode;
 	}
 
 	/**
 	 * Constructs the mappings for all data properties with range xsd:string in the DAG node list. 
 	 * The string buffer stores the mapping string, if any. The method returns true if it finds at 
 	 * least one of the node.
 	 * 
 	 * @param nodeList
 	 * 			The list of existential class nodes.
 	 * @param buffer
 	 * 			The string buffer to stores the mapping string
 	 * @return Returns true if the method finds at least one data property node with xsd:string as
 	 * the range, or false otherwise.
 	 */
 	private boolean createMappingForStringDataType(Set<DAGNode> nodeList, StringBuffer buffer) {
 		
 		boolean hasStringNode = false;  // A flag if there is  at least one DP with range xsd:string
 		
 		buffer.append(select_mapping_class_attribute_string_left);
 		buffer.append(" WHERE ");
 
 		boolean multipleIntervals = false;  // A flag to tell there are more than one SI interval.
 		for (DAGNode node : nodeList) {
 			Predicate property = ((PropertySomeRestriction) node.getDescription()).getPredicate();
 			boolean isObjectProperty = (property.getType(1) == COL_TYPE.OBJECT);
 			if (!isObjectProperty) {
 				COL_TYPE dateType = getAttributeType(property);
 				if (dateType == COL_TYPE.STRING) {
 					PropertySomeRestriction existsDesc = (PropertySomeRestriction) node.getDescription();
 					Property role = ofac.createProperty(existsDesc.getPredicate(), false);
 					DAGNode indexedNode = pureIsa.getRoleNode(role);  // Get the indexed node.
 					if (indexedNode != null) {
 						hasStringNode = true;
 						List<Interval> intervals = indexedNode.getRange().getIntervals();	
 						for (int i = 0; i < intervals.size(); i++) {
 							if (multipleIntervals) {
 								buffer.append(" OR ");
 							}
 							appendIntervalString(intervals.get(i), buffer);
 							multipleIntervals = true;	
 						}
 					}
 				}
 			}
 		}
 		return hasStringNode;
 	}
 	
 	/**
 	 * Constructs the mappings for all data properties with range xsd:int in the DAG node list. 
 	 * The string buffer stores the mapping string, if any. The method returns true if it finds at 
 	 * least one of the node.
 	 * 
 	 * @param nodeList
 	 * 			The list of existential class nodes.
 	 * @param buffer
 	 * 			The string buffer to stores the mapping string
 	 * @return Returns true if the method finds at least one data property node with xsd:int as
 	 * the range, or false otherwise.
 	 */
 	private boolean createMappingForIntegerDataType(Set<DAGNode> nodeList, StringBuffer buffer) {
 		
 		boolean hasIntegerNode = false;  // A flag if there is  at least one DP with range xsd:int
 		
 		buffer.append(select_mapping_class_attribute_integer_left);
 		buffer.append(" WHERE ");
 
 		boolean multipleIntervals = false;  // A flag to tell there are more than one SI interval.
 		for (DAGNode node : nodeList) {
 			Predicate property = ((PropertySomeRestriction) node.getDescription()).getPredicate();
 			boolean isObjectProperty = (property.getType(1) == COL_TYPE.OBJECT);
 			if (!isObjectProperty) {
 				COL_TYPE dateType = getAttributeType(property);
 				if (dateType == COL_TYPE.INTEGER) {
 					PropertySomeRestriction existsDesc = (PropertySomeRestriction) node.getDescription();
 					Property role = ofac.createProperty(existsDesc.getPredicate(), false);
 					DAGNode indexedNode = pureIsa.getRoleNode(role);  // Get the indexed node.
 					if (indexedNode != null) {
 						hasIntegerNode = true;
 						List<Interval> intervals = indexedNode.getRange().getIntervals();	
 						for (int i = 0; i < intervals.size(); i++) {
 							if (multipleIntervals) {
 								buffer.append(" OR ");
 							}
 							appendIntervalString(intervals.get(i), buffer);
 							multipleIntervals = true;	
 						}
 					}
 				}
 			}
 		}
 		return hasIntegerNode;
 	}
 	
 	/**
 	 * Constructs the mappings for all data properties with range xsd:decimal in the DAG node list. 
 	 * The string buffer stores the mapping string, if any. The method returns true if it finds at 
 	 * least one of the node.
 	 * 
 	 * @param nodeList
 	 * 			The list of existential class nodes.
 	 * @param buffer
 	 * 			The string buffer to stores the mapping string
 	 * @return Returns true if the method finds at least one data property node with xsd:decimal as
 	 * the range, or false otherwise.
 	 */
 	private boolean createMappingForDecimalDataType(Set<DAGNode> nodeList, StringBuffer buffer) {
 		
 		boolean hasDecimalNode = false;  // A flag if there is  at least one DP with range xsd:decimal
 		
 		buffer.append(select_mapping_class_attribute_decimal_left);
 		buffer.append(" WHERE ");
 
 		boolean multipleIntervals = false;  // A flag to tell there are more than one SI interval.
 		for (DAGNode node : nodeList) {
 			Predicate property = ((PropertySomeRestriction) node.getDescription()).getPredicate();
 			boolean isObjectProperty = (property.getType(1) == COL_TYPE.OBJECT);
 			if (!isObjectProperty) {
 				COL_TYPE dateType = getAttributeType(property);
 				if (dateType == COL_TYPE.DECIMAL) {
 					PropertySomeRestriction existsDesc = (PropertySomeRestriction) node.getDescription();
 					Property role = ofac.createProperty(existsDesc.getPredicate(), false);
 					DAGNode indexedNode = pureIsa.getRoleNode(role);  // Get the indexed node.
 					if (indexedNode != null) {
 						hasDecimalNode = true;
 						List<Interval> intervals = indexedNode.getRange().getIntervals();	
 						for (int i = 0; i < intervals.size(); i++) {
 							if (multipleIntervals) {
 								buffer.append(" OR ");
 							}
 							appendIntervalString(intervals.get(i), buffer);
 							multipleIntervals = true;	
 						}
 					}
 				}
 			}
 		}
 		return hasDecimalNode;
 	}
 
 	/**
 	 * Constructs the mappings for all data properties with range xsd:double in the DAG node list. 
 	 * The string buffer stores the mapping string, if any. The method returns true if it finds at 
 	 * least one of the node.
 	 * 
 	 * @param nodeList
 	 * 			The list of existential class nodes.
 	 * @param buffer
 	 * 			The string buffer to stores the mapping string
 	 * @return Returns true if the method finds at least one data property node with xsd:double as
 	 * the range, or false otherwise.
 	 */
 	private boolean createMappingForDoubleDataType(Set<DAGNode> nodeList, StringBuffer buffer) {
 		
 		boolean hasDoubleNode = false;  // A flag if there is  at least one DP with range xsd:double
 		
 		buffer.append(select_mapping_class_attribute_double_left);
 		buffer.append(" WHERE ");
 
 		boolean multipleIntervals = false;  // A flag to tell there are more than one SI interval.
 		for (DAGNode node : nodeList) {
 			Predicate property = ((PropertySomeRestriction) node.getDescription()).getPredicate();
 			boolean isObjectProperty = (property.getType(1) == COL_TYPE.OBJECT);
 			if (!isObjectProperty) {
 				COL_TYPE dateType = getAttributeType(property);
 				if (dateType == COL_TYPE.DOUBLE) {
 					PropertySomeRestriction existsDesc = (PropertySomeRestriction) node.getDescription();
 					Property role = ofac.createProperty(existsDesc.getPredicate(), false);
 					DAGNode indexedNode = pureIsa.getRoleNode(role);  // Get the indexed node.
 					if (indexedNode != null) {
 						hasDoubleNode = true;
 						List<Interval> intervals = indexedNode.getRange().getIntervals();	
 						for (int i = 0; i < intervals.size(); i++) {
 							if (multipleIntervals) {
 								buffer.append(" OR ");
 							}
 							appendIntervalString(intervals.get(i), buffer);
 							multipleIntervals = true;	
 						}
 					}
 				}
 			}
 		}
 		return hasDoubleNode;
 	}
 
 	/**
 	 * Constructs the mappings for all data properties with range xsd:date in the DAG node list. 
 	 * The string buffer stores the mapping string, if any. The method returns true if it finds at 
 	 * least one of the node.
 	 * 
 	 * @param nodeList
 	 * 			The list of existential class nodes.
 	 * @param buffer
 	 * 			The string buffer to stores the mapping string
 	 * @return Returns true if the method finds at least one data property node with xsd:date as
 	 * the range, or false otherwise.
 	 */
 	private boolean createMappingForDateDataType(Set<DAGNode> nodeList, StringBuffer buffer) {
 		
 		boolean hasDateNode = false;  // A flag if there is  at least one DP with range xsd:date
 		
 		buffer.append(select_mapping_class_attribute_datetime_left);
 		buffer.append(" WHERE ");
 
 		boolean multipleIntervals = false;  // A flag to tell there are more than one SI interval.
 		for (DAGNode node : nodeList) {
 			Predicate property = ((PropertySomeRestriction) node.getDescription()).getPredicate();
 			boolean isObjectProperty = (property.getType(1) == COL_TYPE.OBJECT);
 			if (!isObjectProperty) {
 				COL_TYPE dateType = getAttributeType(property);
 				if (dateType == COL_TYPE.DATETIME) {
 					PropertySomeRestriction existsDesc = (PropertySomeRestriction) node.getDescription();
 					Property role = ofac.createProperty(existsDesc.getPredicate(), false);
 					DAGNode indexedNode = pureIsa.getRoleNode(role);  // Get the indexed node.
 					if (indexedNode != null) {
 						hasDateNode = true;
 						List<Interval> intervals = indexedNode.getRange().getIntervals();	
 						for (int i = 0; i < intervals.size(); i++) {
 							if (multipleIntervals) {
 								buffer.append(" OR ");
 							}
 							appendIntervalString(intervals.get(i), buffer);
 							multipleIntervals = true;	
 						}
 					}
 				}
 			}
 		}
 		return hasDateNode;
 	}
 
 	/**
 	 * Constructs the mappings for all data properties with range xsd:boolean in the DAG node list. 
 	 * The string buffer stores the mapping string, if any. The method returns true if it finds at 
 	 * least one of the node.
 	 * 
 	 * @param nodeList
 	 * 			The list of existential class nodes.
 	 * @param buffer
 	 * 			The string buffer to stores the mapping string
 	 * @return Returns true if the method finds at least one data property node with xsd:boolean as
 	 * the range, or false otherwise.
 	 */
 	private boolean createMappingForBooleanDataType(Set<DAGNode> nodeList, StringBuffer buffer) {
 		
 		boolean hasBooleanNode = false;  // A flag if there is  at least one DP with range xsd:boolean
 		
 		buffer.append(select_mapping_class_attribute_boolean_left);
 		buffer.append(" WHERE ");
 
 		boolean multipleIntervals = false;  // A flag to tell there are more than one SI interval.
 		for (DAGNode node : nodeList) {
 			Predicate property = ((PropertySomeRestriction) node.getDescription()).getPredicate();
 			boolean isObjectProperty = (property.getType(1) == COL_TYPE.OBJECT);
 			if (!isObjectProperty) {
 				COL_TYPE dateType = getAttributeType(property);
 				if (dateType == COL_TYPE.BOOLEAN) {
 					PropertySomeRestriction existsDesc = (PropertySomeRestriction) node.getDescription();
 					Property role = ofac.createProperty(existsDesc.getPredicate(), false);
 					DAGNode indexedNode = pureIsa.getRoleNode(role);  // Get the indexed node.
 					if (indexedNode != null) {
 						hasBooleanNode = true;
 						List<Interval> intervals = indexedNode.getRange().getIntervals();	
 						for (int i = 0; i < intervals.size(); i++) {
 							if (multipleIntervals) {
 								buffer.append(" OR ");
 							}
 							appendIntervalString(intervals.get(i), buffer);
 							multipleIntervals = true;	
 						}
 					}
 				}
 			}
 		}
 		return hasBooleanNode;
 	}
 	
 	private void appendIntervalString(Interval interval, StringBuffer out) {
 		if (interval.getStart() == interval.getEnd()) {
 			out.append(String.format(whereSingleCondition, interval.getStart()));
 		} else {
 			out.append(String.format(whereIntervalCondition, interval.getStart(), interval.getEnd()));
 		}
 	}
 
 	@Override
 	public void collectStatistics(Connection conn) throws SQLException {
 
 
 		Statement st = conn.createStatement();
 
 		st.addBatch(analyze);
 
 		st.executeBatch();
 		st.close();
 
 	}
 
 	@Override
 	public void getDropDDL(OutputStream out) throws IOException {
 		// TODO Auto-generated method stub
 
 	}
 
 	@Override
 	public void getMetadataSQLInserts(OutputStream outstream) throws IOException {
 
 		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(outstream));
 
 		String insert_query = this.insert_query.replace("?", "%s");
 
 		for (DAGNode node : dag.getClasses()) {
 
 			ClassDescription description = (ClassDescription) node.getDescription();
 
 			/*
 			 * we always prefer the pureISA node since it can have extra data
 			 * (indexes)
 			 */
 			DAGNode node2 = pureIsa.getClassNode(description);
 			if (node2 != null) {
 				node = node2;
 			}
 
 			String uri = description.toString();
 
 			for (Interval it : node.getRange().getIntervals()) {
 
 				out.append(String.format(insert_query, getQuotedString(uri), node.getIndex(), it.getStart(), it.getEnd(), CLASS_TYPE));
 				out.append(";\n");
 			}
 		}
 
 		for (DAGNode node : dag.getRoles()) {
 			Property description = (Property) node.getDescription();
 
 			/*
 			 * we always prefer the pureISA node since it can have extra data
 			 * (indexes)
 			 */
 			DAGNode node2 = pureIsa.getRoleNode(description);
 			if (node2 != null) {
 				node = node2;
 			}
 
 			String uri = description.toString();
 
 			for (Interval it : node.getRange().getIntervals()) {
 				out.append(String.format(insert_query, getQuotedString(uri), node.getIndex(), it.getStart(), it.getEnd(), ROLE_TYPE));
 				out.append(";\n");
 			}
 		}
 
 		out.flush();
 	}
 
 	@Override
 	public void insertMetadata(Connection conn) throws SQLException {
 
 
 		PreparedStatement stm = conn.prepareStatement(insert_query);
 		for (DAGNode node : dag.getClasses()) {
 
 			ClassDescription description = (ClassDescription) node.getDescription();
 
 			/*
 			 * we always prefer the pureISA node since it can have extra data
 			 * (indexes)
 			 */
 			DAGNode node2 = pureIsa.getClassNode(description);
 			if (node2 != null) {
 				node = node2;
 			}
 
 			String uri = description.toString();
 
 			for (Interval it : node.getRange().getIntervals()) {
 				stm.setString(1, uri);
 				stm.setInt(2, node.getIndex());
 				stm.setInt(3, it.getStart());
 				stm.setInt(4, it.getEnd());
 				stm.setInt(5, CLASS_TYPE);
 				stm.addBatch();
 			}
 		}
 		stm.executeBatch();
 
 		for (DAGNode node : dag.getRoles()) {
 			Property description = (Property) node.getDescription();
 
 			/*
 			 * we always prefer the pureISA node since it can have extra data
 			 * (indexes)
 			 */
 			DAGNode node2 = pureIsa.getRoleNode(description);
 			if (node2 != null) {
 				node = node2;
 			}
 
 			String uri = description.toString();
 
 			for (Interval it : node.getRange().getIntervals()) {
 				stm.setString(1, uri);
 				stm.setInt(2, node.getIndex());
 				stm.setInt(3, it.getStart());
 				stm.setInt(4, it.getEnd());
 				stm.setInt(5, ROLE_TYPE);
 				stm.addBatch();
 			}
 		}
 		stm.executeBatch();
 		stm.close();
 
 
 	}
 
 	@Override
 	public void setVocabulary(Set<Predicate> vocabulary) throws PunningException {
 		// TODO
 
 		/* This method should initialize the vocabulary of the DAG */
 
 	}
 
 	/*
 	 * Utilities
 	 */
 
 	private String getQuotedString(String str) {
 		StringBuffer bf = new StringBuffer();
 		bf.append("'");
 		bf.append(str);
 		bf.append("'");
 		return bf.toString();
 	}
 
 	private String getQuotedString(URI str) {
 		StringBuffer bf = new StringBuffer();
 		bf.append("'");
 		bf.append(str.toString());
 		bf.append("'");
 		return bf.toString();
 	}
 
 	@Override
 	public void dropIndexes(Connection conn) throws SQLException {
 		log.debug("Droping indexes");
 
 
 		Statement st = conn.createStatement();
 
 		st.addBatch(dropindexclass1);
 		st.addBatch(dropindexclass2);
 		// st.addBatch(dropindexclass3);
 		// st.addBatch(dropindexclass4);
 		st.addBatch(dropindexrole1);
 		st.addBatch(dropindexrole2);
 		st.addBatch(dropindexrole3);
 
 		st.addBatch(dropindexattribute_literal1);
 		st.addBatch(dropindexattribute_string1);
 		st.addBatch(dropindexattribute_integer1);
 		st.addBatch(dropindexattribute_decimal1);
 		st.addBatch(dropindexattribute_double1);
 		st.addBatch(dropindexattribute_datetime1);
 		st.addBatch(dropindexattribute_boolean1);
 		
 		st.addBatch(dropindexattribute_literal2);
 		st.addBatch(dropindexattribute_string2);
 		st.addBatch(dropindexattribute_integer2);
 		st.addBatch(dropindexattribute_decimal2);
 		st.addBatch(dropindexattribute_double2);
 		st.addBatch(dropindexattribute_datetime2);
 		st.addBatch(dropindexattribute_boolean2);
 		
 		st.addBatch(dropindexattribute_literal3);
 		st.addBatch(dropindexattribute_string3);
 		st.addBatch(dropindexattribute_integer3);
 		st.addBatch(dropindexattribute_decimal3);
 		st.addBatch(dropindexattribute_double3);
 		st.addBatch(dropindexattribute_datetime3);
 		st.addBatch(dropindexattribute_boolean3);
 
 		st.executeBatch();
 		st.close();
 
 		isIndexed = false;
 	}
 
 	@Override
 	public boolean isIndexed(Connection conn) {
 		return isIndexed;
 	}
 
 	@Override
 	public boolean isDBSchemaDefined(Connection conn) throws SQLException {
 		Statement st = conn.createStatement();
 		boolean exists = true;
 		try {
 			st.executeQuery(String.format("SELECT 1 FROM %s WHERE 1=0", class_table));
 			st.executeQuery(String.format("SELECT 1 FROM %s WHERE 1=0", role_table));
 			st.executeQuery(String.format("SELECT 1 FROM %s WHERE 1=0", attribute_table_literal));
 			st.executeQuery(String.format("SELECT 1 FROM %s WHERE 1=0", attribute_table_string));
 			st.executeQuery(String.format("SELECT 1 FROM %s WHERE 1=0", attribute_table_integer));
 			st.executeQuery(String.format("SELECT 1 FROM %s WHERE 1=0", attribute_table_decimal));
 			st.executeQuery(String.format("SELECT 1 FROM %s WHERE 1=0", attribute_table_double));
 			st.executeQuery(String.format("SELECT 1 FROM %s WHERE 1=0", attribute_table_datetime));
 			st.executeQuery(String.format("SELECT 1 FROM %s WHERE 1=0", attribute_table_boolean));			
 		} catch (SQLException e) {
 			exists = false;
 			log.debug(e.getMessage());
 		} finally {
 			try {
 				st.close();
 			} catch (SQLException e) {
 
 			}
 		}
 		return exists;
 	}
 
 	@Override
 	public long loadWithFile(Connection conn, final Iterator<Assertion> data) throws SQLException, IOException {
 
 		log.debug("Insert data into schemas using temporary files");
 
 		File tempFileDataPropertiesLiteral = File.createTempFile("quest-copy-dataprop-literal", ".tmp");
 		File tempFileDataPropertiesString = File.createTempFile("quest-copy-dataprop-string", ".tmp");
 		File tempFileDataPropertiesInteger = File.createTempFile("quest-copy-dataprop-integer", ".tmp");
 		File tempFileDataPropertiesDecimal = File.createTempFile("quest-copy-dataprop-decimal", ".tmp");
 		File tempFileDataPropertiesDouble = File.createTempFile("quest-copy-dataprop-double", ".tmp");
 		File tempFileDataPropertiesDate = File.createTempFile("quest-copy-dataprop-date", ".tmp");
 		File tempFileDataPropertiesBoolean = File.createTempFile("quest-copy-dataprop-boolean", ".tmp");
 		File tempFileObjectProperties = File.createTempFile("quest-copy-oprop", ".tmp");
 
 		BufferedWriter outObjectProperties = null;
 		BufferedWriter outDataPropertiesLiteral = null;
 		BufferedWriter outDataPropertiesString = null;
 		BufferedWriter outDataPropertiesInteger = null;
 		BufferedWriter outDataPropertiesDecimal = null;
 		BufferedWriter outDataPropertiesDouble = null;
 		BufferedWriter outDataPropertiesDate = null;
 		BufferedWriter outDataPropertiesBoolean = null;
 		try {
 			outObjectProperties = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFileObjectProperties)));
 			outDataPropertiesLiteral = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFileDataPropertiesLiteral)));
 			outDataPropertiesString = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFileDataPropertiesString)));
 			outDataPropertiesInteger = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFileDataPropertiesInteger)));
 			outDataPropertiesDecimal = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFileDataPropertiesDecimal)));
 			outDataPropertiesDouble = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFileDataPropertiesDouble)));
 			outDataPropertiesDate = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFileDataPropertiesDate)));
 			outDataPropertiesBoolean = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFileDataPropertiesBoolean)));
 		} catch (FileNotFoundException e) {
 			log.error(e.getMessage());
 			log.debug(e.getMessage(), e);
 			return -1;
 		}
 
 		File tempFileType = File.createTempFile("quest-copy-type", ".tmp");
 		BufferedWriter outType = null;
 		try {
 			outType = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFileType)));
 		} catch (FileNotFoundException e) {
 			log.error(e.getMessage());
 			log.debug(e.getMessage(), e);
 			return -1;
 		}
 
 		final long[] counts = new long[3];
 
 		final HashMap<Predicate, Integer> indexes = new HashMap<Predicate, Integer>(this.ontology.getVocabulary().size() * 2);
 
 		int insertscount = 0;
 
 		try {
 			while (data.hasNext()) {
 
 				Assertion ax = data.next();
 
 				insertscount += 1;
 
 				if (ax instanceof DataPropertyAssertion) {
 
 					DataPropertyAssertion attributeABoxAssertion = (DataPropertyAssertion) ax;
 					Predicate attribute = attributeABoxAssertion.getAttribute();
 					Predicate.COL_TYPE attributeType = getAttributeType(attribute);
 					
 					String uri = attributeABoxAssertion.getObject().getURI().toString();
 					String lit = attributeABoxAssertion.getValue().getValue();
 					String lang = attributeABoxAssertion.getValue().getLanguage();
 					
 					Integer idxc = indexes.get(attribute);
 					int idx = -1;
 					if (idxc == null) {
 						// Predicate propPred =
 						// attributeABoxAssertion.getAttribute();
 						Property propDesc = ofac.createProperty(attribute);
 						DAGNode node = pureIsa.getRoleNode(propDesc);
 						idx = node.getIndex();
 						indexes.put(attribute, idx);
 					} else {
 						idx = idxc;
 					}
 					
 					switch(attributeType) {
 						case LITERAL: appendStringToPropertyFile(outDataPropertiesLiteral, new String[] { uri, lit, lang, String.valueOf(idx) }); break;
 						case STRING: appendStringToPropertyFile(outDataPropertiesString, new String[] {uri, lit, String.valueOf(idx) }); break;
 						case INTEGER: appendStringToPropertyFile(outDataPropertiesString, new String[] {uri, lit, String.valueOf(idx) }); break;
 						case DECIMAL: appendStringToPropertyFile(outDataPropertiesString, new String[] {uri, lit, String.valueOf(idx) }); break;
 						case DOUBLE: appendStringToPropertyFile(outDataPropertiesString, new String[] {uri, lit, String.valueOf(idx) }); break;
 						case DATETIME: appendStringToPropertyFile(outDataPropertiesString, new String[] {uri, lit, String.valueOf(idx) }); break;
 						case BOOLEAN: appendStringToPropertyFile(outDataPropertiesString, new String[] {uri, lit, String.valueOf(idx) }); break;
 					}
 
 				} else if (ax instanceof ObjectPropertyAssertion) {
 
 					ObjectPropertyAssertion roleABoxAssertion = (ObjectPropertyAssertion) ax;
 					String uri1 = roleABoxAssertion.getFirstObject().getURI().toString();
 					String uri2 = roleABoxAssertion.getSecondObject().getURI().toString();
 
 					Predicate propPred = roleABoxAssertion.getRole();
 					Property propDesc = ofac.createProperty(propPred);
 
 					if (dag.equi_mappings.containsKey(propDesc)) {
 						Property desc = (Property) dag.equi_mappings.get(propDesc);
 						if (desc.isInverse()) {
 							String tmp = uri1;
 							uri1 = uri2;
 							uri2 = tmp;
 						}
 					}
 
 					int idx = -1;
 					Integer idxc = indexes.get(propPred);
 					if (idxc == null) {
 
 						DAGNode node = pureIsa.getRoleNode(propDesc);
 						if (node == null) {
 							Property desc = (Property) dag.equi_mappings.get(propDesc);
 
 							if (desc == null) {
 								log.error("Property class without node: " + propDesc);
 							}
 							Property desinv = ofac.createProperty(desc.getPredicate(), !desc.isInverse());
 							DAGNode node2 = (pureIsa.getRoleNode(desinv));
 							idx = node2.getIndex();
 						} else {
 							idx = node.getIndex();
 						}
 						indexes.put(roleABoxAssertion.getRole(), idx);
 					} else {
 						idx = idxc;
 					}
 
 					outObjectProperties.append(uri1);
 					outObjectProperties.append('\t');
 					outObjectProperties.append(uri2);
 					outObjectProperties.append('\t');
 					outObjectProperties.append(String.valueOf(idx));
 					outObjectProperties.append('\n');
 
 				} else if (ax instanceof ClassAssertion) {
 
 					ClassAssertion cassertion = (ClassAssertion) ax;
 					Predicate pred = cassertion.getConcept();
 
 					int idx = -1;
 					Integer idxc = indexes.get(cassertion.getConcept());
 					if (idxc == null) {
 						Predicate clsPred = cassertion.getConcept();
 						ClassDescription clsDesc = ofac.createClass(clsPred);
 						DAGNode node = pureIsa.getClassNode(clsDesc);
 						if (node == null) {
 							String cls = cassertion.getConcept().getName().toString();
 							log.error("Found class without node: " + cls.toString());
 						}
 						idx = node.getIndex();
 						indexes.put(pred, idx);
 					} else {
 						idx = idxc;
 					}
 					String uri = cassertion.getObject().getURI().toString();
 
 					outType.append(uri);
 					outType.append('\t');
 					outType.append(String.valueOf(idx));
 					outType.append('\n');
 				}
 			}
 			outType.flush();
 			outType.close();
 			
 			outObjectProperties.flush();
 			outObjectProperties.close();
 			
 			outDataPropertiesLiteral.flush();
 			outDataPropertiesLiteral.close();
 			
 			outDataPropertiesString.flush();
 			outDataPropertiesString.close();
 			
 			outDataPropertiesInteger.flush();
 			outDataPropertiesInteger.close();
 			
 			outDataPropertiesDecimal.flush();
 			outDataPropertiesDecimal.close();
 			
 			outDataPropertiesDouble.flush();
 			outDataPropertiesDouble.close();
 			
 			outDataPropertiesDate.flush();
 			outDataPropertiesDate.close();
 			
 			outDataPropertiesBoolean.flush();
 			outDataPropertiesBoolean.close();
 			log.debug("Finished reading input assertions.");
 		} catch (IOException e) {
 			log.error(e.getMessage());
 			log.debug(e.getMessage(), e);
 		} finally {
 			// NO-OP
 		}
 
 		/*
 		 * All data has been generated. Sending the data to the database.
 		 */
 
 		final CopyManager cm = new CopyManager((BaseConnection) conn);
 
 		try {
 			log.debug("Inserting object properties");
 			FileReader inprop = new FileReader(tempFileObjectProperties);
 			counts[0] = cm.copyIn("COPY " + role_table + " FROM STDIN", inprop);
 		} catch (Exception e) {
 			log.error(e.getMessage());
 		} finally {
 			try {
 				tempFileObjectProperties.delete();
 			} catch (Exception e) {
 				// NO-OP
 			}
 		}
 
 		try {
 			log.debug("Inserting data properties");
 			
 			counts[1] = 0; // init
 			FileReader inprop = new FileReader(tempFileDataPropertiesLiteral);			
 			counts[1] += cm.copyIn("COPY " + attribute_table_literal + " FROM STDIN", inprop);
 			inprop = new FileReader(tempFileDataPropertiesString);
 			counts[1] += cm.copyIn("COPY " + attribute_table_string + " FROM STDIN", inprop);
 			inprop = new FileReader(tempFileDataPropertiesInteger);
 			counts[1] += cm.copyIn("COPY " + attribute_table_integer + " FROM STDIN", inprop);
 			inprop = new FileReader(tempFileDataPropertiesDecimal);
 			counts[1] += cm.copyIn("COPY " + attribute_table_decimal + " FROM STDIN", inprop);
 			inprop = new FileReader(tempFileDataPropertiesDouble);
 			counts[1] += cm.copyIn("COPY " + attribute_table_double + " FROM STDIN", inprop);
 			inprop = new FileReader(tempFileDataPropertiesDate);
 			counts[1] += cm.copyIn("COPY " + attribute_table_datetime + " FROM STDIN", inprop);
 			inprop = new FileReader(tempFileDataPropertiesBoolean);
 			counts[1] += cm.copyIn("COPY " + attribute_table_boolean + " FROM STDIN", inprop);
 		} catch (Exception e) {
 			log.error(e.getMessage());
 		} finally {
 			try {
 				tempFileDataPropertiesLiteral.delete();
 				tempFileDataPropertiesString.delete();
 				tempFileDataPropertiesInteger.delete();
 				tempFileDataPropertiesDecimal.delete();
 				tempFileDataPropertiesDouble.delete();
 				tempFileDataPropertiesDate.delete();
 				tempFileDataPropertiesBoolean.delete();
 			} catch (Exception e) {
 				// NO-OP
 			}
 		}
 
 		try {
 			log.debug("Inserting type assertions");
 			FileReader intype = new FileReader(tempFileType);
 			counts[2] = cm.copyIn("COPY " + class_table + " FROM STDIN", intype);
 		} catch (Exception e) {
 			log.error(e.getMessage());
 		} finally {
 			try {
 				tempFileType.delete();
 			} catch (Exception e) {
 				// NO-OP
 			}
 		}
 
 		if (insertscount != (counts[0] + counts[1] + counts[2])) {
 			log.warn("Warning, effective inserts are different than the elements in the stream: in {}, effective: {}", insertscount,
 					counts[0] + counts[1] + counts[2]);
 		}
 		return counts[0] + counts[1] + counts[2];
 	}
 	
 	private void appendStringToPropertyFile(BufferedWriter writer, String[] input) throws IOException {
 		for (int i = 0; i < input.length; i++) {
 			writer.append(input[i]);			
 			if (i != input.length-1) {
 				writer.append('\t');
 			} else {
 				writer.append('\n');
 			}
 		}		
 	}
 }
