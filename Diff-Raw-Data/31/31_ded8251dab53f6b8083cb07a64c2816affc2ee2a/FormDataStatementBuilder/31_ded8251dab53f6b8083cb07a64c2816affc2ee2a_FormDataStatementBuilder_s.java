 /*******************************************************************************
  * Copyright (c) 2010 BSI Business Systems Integration AG.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     BSI Business Systems Integration AG - initial API and implementation
  ******************************************************************************/
 package org.eclipse.scout.rt.server.services.common.jdbc.builder;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.TreeSet;
 import java.util.concurrent.atomic.AtomicInteger;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 import org.eclipse.scout.commons.ClassIdentifier;
 import org.eclipse.scout.commons.ListUtility;
 import org.eclipse.scout.commons.StringUtility;
 import org.eclipse.scout.commons.StringUtility.ITagProcessor;
 import org.eclipse.scout.commons.exception.ProcessingException;
 import org.eclipse.scout.commons.holders.NVPair;
 import org.eclipse.scout.commons.logger.IScoutLogger;
 import org.eclipse.scout.commons.logger.ScoutLogManager;
 import org.eclipse.scout.commons.parsers.BindModel;
 import org.eclipse.scout.commons.parsers.BindParser;
 import org.eclipse.scout.commons.parsers.token.IToken;
 import org.eclipse.scout.commons.parsers.token.ValueInputToken;
 import org.eclipse.scout.rt.server.services.common.jdbc.style.ISqlStyle;
 import org.eclipse.scout.rt.shared.data.form.AbstractFormData;
 import org.eclipse.scout.rt.shared.data.form.fields.AbstractFormFieldData;
 import org.eclipse.scout.rt.shared.data.form.fields.composer.ComposerAttributeNodeData;
 import org.eclipse.scout.rt.shared.data.form.fields.composer.ComposerEitherOrNodeData;
 import org.eclipse.scout.rt.shared.data.form.fields.composer.ComposerEntityNodeData;
 import org.eclipse.scout.rt.shared.data.form.fields.treefield.AbstractTreeFieldData;
 import org.eclipse.scout.rt.shared.data.form.fields.treefield.TreeNodeData;
 import org.eclipse.scout.rt.shared.data.model.DataModelConstants;
 import org.eclipse.scout.rt.shared.data.model.DataModelUtility;
 import org.eclipse.scout.rt.shared.data.model.IDataModel;
 import org.eclipse.scout.rt.shared.data.model.IDataModelAttribute;
 import org.eclipse.scout.rt.shared.data.model.IDataModelEntity;
 
 /**
  * <pre>
  * Usage:
  * <ul>
  * <li>call {@link #setDataModelEntityDefinition(Class, String, boolean)}, {@link #setDataModelAttributeDefinition(Class, String, boolean)} and {@link #addStatementMapping(Class, String, int, int, boolean)}
  * for all member classes in the FormData</li>
  * <li>call {@link #build(AbstractFormData)}</li>
  * <li>add {@link #getWhereConstraints()} to the base sql statement (starts with an AND)</li>
  * <li>add {@link #getBindMap()} to the sql bind bases</li>
  * </pre>
  * <p>
  * The method {@link #buildComposerEntityNode(ComposerEntityNodeData)} corrects composer trees for correct handling of
  * zero-traversing aggregation attributes and normal attributes using
  * {@link #isZeroTraversingAttribute(ComposerAttributeNodeData)}.<br>
  * An attribute is zero-traversing when it contains 0 and therefore null/non-existence together with the operator &lt;,
  * &gt;, &lt;=, &gt;=, =, !=, &lt;&gt;, between. Only numeric attributes can be zero-traversing. Dates never are.
  * <p>
  * Examples of zero-traversing:
  * <ul>
  * <li>Count(Person) &lt; 3</li>
  * <li>priority between -10 and 10</li>
  * <li>Sum(payment) &lt;= 1'000'000</li>
  * <li></li>
  * </ul>
  * <p>
  * Examples of <b>not</b> zero-traversing:
  * <ul>
  * <li>Count(Person) between 2 and 4</li>
  * <li>priority between 1 and 5</li>
  * <li>Sum(payment) &gt;= 1'000'000</li>
  * <li></li>
  * </ul>
  * <p>
  * When an entity e contains zero-traversing <b>aggregation</b> attributes (such as Count(.), Sum(.)) z1..zn and
  * non-zero-traversing attributes a1..an it is splittet into 2 entities as follows:<br>
  * <code>
  * <pre>either (
  *   e
  *     a1..an
  *     z1..zn
  * )
  * or NOT (
  *   e
  *     a1..an
  * )
  * </pre>
  * </code>
  * <p>
  * In sql this would be something like<br>
  * <code>
  * <pre>exists (select 1 from Person ... where a1 and z1 groupy by ... having a2 and z2)
  * </pre>
  * </code> will be transformed to <code>
  * <pre>
  * (
  *   exists (select 1 from Person ... where a1 and z1 groupy by ... having a2 and z2)
  *   OR NOT
  *   exists (select 1 from Person ... where a1 groupy by ... having a2)
  * )
  * </pre>
  * </code>
  * <p>
  * Zero-traversing non aggregation attributes are simply wrapped using NLV(attribute).
  * <p>
  * That way non-existent matches are added to the result, which matches the expected behaviour.
  * 
  * @author imo
  */
 @SuppressWarnings("deprecation")
 public class FormDataStatementBuilder implements DataModelConstants {
   private static final IScoutLogger LOG = ScoutLogManager.getLogger(FormDataStatementBuilder.class);
   private static final Pattern PLAIN_ATTRIBUTE_PATTERN = Pattern.compile("(<attribute>)([a-zA-Z_][a-zA-Z0-9_]*)(</attribute>)");
 
   public static enum AttributeStrategy {
     BuildConstraintOfAttribute,
     BuildConstraintOfContext,
     BuildConstraintOfAttributeWithContext,
     BuildQueryOfAttributeAndConstraintOfContext,
   }
 
   public static enum EntityStrategy {
     BuildConstraints,
     BuildQuery,
   }
 
   public static enum AttributeKind {
     /**
      * no attribute node
      */
     Undefined,
     NonAggregation,
     Aggregation,
     NonAggregationNonZeroTraversing,
     AggregationNonZeroTraversing,
   }
 
   private ISqlStyle m_sqlStyle;
   private IDataModel m_dataModel;
   private AliasMapper m_aliasMapper;
   private Map<Class<?>, DataModelAttributePartDefinition> m_dataModelAttMap;
   private Map<Class<?>, DataModelEntityPartDefinition> m_dataModelEntMap;
   private List<BasicPartDefinition> m_basicDefs;
   private Map<String, Object> m_bindMap;
   private AtomicInteger m_sequenceProvider;
   private StringBuffer m_where;
 
   /**
    * @param sqlStyle
    */
   public FormDataStatementBuilder(ISqlStyle sqlStyle) {
     m_sqlStyle = sqlStyle;
     m_aliasMapper = new AliasMapper();
     m_bindMap = new HashMap<String, Object>();
     m_dataModelAttMap = new HashMap<Class<?>, DataModelAttributePartDefinition>();
     m_dataModelEntMap = new HashMap<Class<?>, DataModelEntityPartDefinition>();
     m_basicDefs = new ArrayList<BasicPartDefinition>();
     setSequenceProvider(new AtomicInteger(0));
   }
 
   public IDataModel getDataModel() {
     return m_dataModel;
   }
 
   public void setDataModel(IDataModel dataModel) {
     m_dataModel = dataModel;
   }
 
   /**
    * @returns the reference to the sequence provider to be used outside for additional sequenced items or sub statemet
    *          builders
    */
   public AtomicInteger getSequenceProvider() {
     return m_sequenceProvider;
   }
 
   /**
    * use another sequence provider (counts 0,1,2... for aliases)
    */
   public void setSequenceProvider(AtomicInteger sequenceProvider) {
     m_sequenceProvider = sequenceProvider;
     m_aliasMapper.setSequenceProvider(m_sequenceProvider);
   }
 
   /**
    * Define the statement part for a sql part. For composer attributes and entites use
    * {@link #setDataModelAttributeDefinition(DataModelAttributePartDefinition)} and
    * {@link #setDataModelEntityDefinition(DataModelEntityPartDefinition)}
    * <p>
    * <b>Number, Date, String, Boolean field</b>:<br>
    * The sqlAttribute is something like <code>@PERSON@.LAST_NAME</code><br>
    * When multiple occurrences are simultaneously used, the sqlAttribute may be written as
    * <code>(&lt;attribute&gt;@PERSON@.ORDER_STATUS&lt;/attribute&gt; OR &lt;attribute&gt;@PERSON@.DELIVERY_STATUS&lt;/attribute&gt;)</code>
    * <p>
    * The operator and aggregationType are required, unless a {@link BasicPartDefinition} is used.
    */
   public void setBasicDefinition(Class<?> fieldType, String sqlAttribute, int operator) {
     setBasicDefinition(new BasicPartDefinition(fieldType, sqlAttribute, operator));
   }
 
   /**
    * see {@link #setBasicDefinition(Class, String, int)}
    */
   public void setBasicDefinition(ClassIdentifier fieldTypeIdentifier, String sqlAttribute, int operator) {
     setBasicDefinition(new BasicPartDefinition(fieldTypeIdentifier, sqlAttribute, operator));
   }
 
   /**
    * see {@link #setBasicDefinition(Class, String, int)}
    */
   public void setBasicDefinition(Class<?> fieldType, String sqlAttribute, int operator, boolean plainBind) {
     setBasicDefinition(new BasicPartDefinition(fieldType, sqlAttribute, operator, plainBind));
   }
 
   /**
    * see {@link #setBasicDefinition(Class, String, int)}
    */
   public void setBasicDefinition(ClassIdentifier fieldTypeIdentifier, String sqlAttribute, int operator, boolean plainBind) {
     setBasicDefinition(new BasicPartDefinition(fieldTypeIdentifier, sqlAttribute, operator, plainBind));
   }
 
   /**
    * see {@link #setBasicDefinition(Class, String, int)}
    */
   public void setBasicDefinition(Class<?>[] fieldTypes, String sqlAttribute, int operator) {
     setBasicDefinition(new BasicPartDefinition(fieldTypes, sqlAttribute, operator, false));
   }
 
   /**
    * see {@link #setBasicDefinition(Class, String, int)}
    */
   public void setBasicDefinition(ClassIdentifier[] fieldTypeIdentifiers, String sqlAttribute, int operator) {
     setBasicDefinition(new BasicPartDefinition(fieldTypeIdentifiers, sqlAttribute, operator, false));
   }
 
   /**
    * see {@link #setBasicDefinition(Class, String, int)}
    */
   public void setBasicDefinition(BasicPartDefinition def) {
     m_basicDefs.add(def);
   }
 
   /**
    * @deprecated use setBasicDefinition instead
    */
   @Deprecated
   public void setValueDefinition(Class<?> fieldType, String sqlAttribute, int operator) {
     setValueDefinition(new ValuePartDefinition(fieldType, sqlAttribute, operator));
   }
 
   /**
    * @deprecated use setBasicDefinition instead
    */
   @Deprecated
   public void setValueDefinition(ClassIdentifier fieldTypeIdentifier, String sqlAttribute, int operator) {
     setValueDefinition(new ValuePartDefinition(fieldTypeIdentifier, sqlAttribute, operator));
   }
 
   /**
    * @deprecated use setBasicDefinition instead
    */
   @Deprecated
   public void setValueDefinition(Class<?> fieldType, String sqlAttribute, int operator, boolean plainBind) {
     setValueDefinition(new ValuePartDefinition(fieldType, sqlAttribute, operator, plainBind));
   }
 
   /**
    * @deprecated use setBasicDefinition instead
    */
   @Deprecated
   public void setValueDefinition(ClassIdentifier fieldTypeIdentifier, String sqlAttribute, int operator, boolean plainBind) {
     setValueDefinition(new ValuePartDefinition(fieldTypeIdentifier, sqlAttribute, operator, plainBind));
   }
 
   /**
    * @deprecated use setBasicDefinition instead
    */
   @Deprecated
   public void setValueDefinition(Class<?>[] fieldTypes, String sqlAttribute, int operator) {
     setValueDefinition(new ValuePartDefinition(fieldTypes, sqlAttribute, operator, false));
   }
 
   /**
    * @deprecated use setBasicDefinition instead
    */
   @Deprecated
   public void setValueDefinition(ClassIdentifier[] fieldTypeIdentifiers, String sqlAttribute, int operator) {
     setValueDefinition(new ValuePartDefinition(fieldTypeIdentifiers, sqlAttribute, operator, false));
   }
 
   /**
    * @deprecated use setBasicDefinition instead
    */
   @Deprecated
   public void setValueDefinition(ValuePartDefinition def) {
     m_basicDefs.add(def);
   }
 
   /**
    * <b>Data model attribute</b>:<br>
    * The sqlAttribute is something like LAST_NAME, STATUS or @PERSON@.LAST_NAME, @PERSON@.STATUS.
    * 
    * @PERSON@ will be replaced by the parent entitie's generated alias.
    *          <p>
    *          The @PERSON@ prefix is added automatically if missing, but only if the entity where the attribute is
    *          contained has only <b>one</b> alias.<br>
    *          When multiple occurrences are simultaneously used, the sqlAttribute may be written as
    *          <code>(&lt;attribute&gt;ORDER_STATUS&lt;/attribute&gt; OR &lt;attribute&gt;DELIVERY_STATUS&lt;/attribute&gt;)</code>
    */
   public void setDataModelAttributeDefinition(Class<? extends IDataModelAttribute> attributeType, String sqlAttribute) {
     setDataModelAttributeDefinition(attributeType, sqlAttribute, false);
   }
 
   /**
    * see {@link #setDataModelAttributeDefinition(Class, String)}
    */
   public void setDataModelAttributeDefinition(Class<? extends IDataModelAttribute> attributeType, String sqlAttribute, boolean plainBind) {
     setDataModelAttributeDefinition(new DataModelAttributePartDefinition(attributeType, sqlAttribute, plainBind));
   }
 
   /**
    * see {@link #setDataModelAttributeDefinition(Class, String)}
    */
   public void setDataModelAttributeDefinition(Class<? extends IDataModelAttribute> attributeType, String whereClause, String selectClause, boolean plainBind) {
     setDataModelAttributeDefinition(new DataModelAttributePartDefinition(attributeType, whereClause, selectClause, plainBind));
   }
 
   /**
    * see {@link #setDataModelAttributeDefinition(Class, String)}
    */
   public void setDataModelAttributeDefinition(DataModelAttributePartDefinition def) {
     m_dataModelAttMap.put(def.getAttributeType(), def);
   }
 
   /**
    * see {@link #setDataModelEntityDefinition(Class, String, String)}
    */
   public void setDataModelEntityDefinition(Class<? extends IDataModelEntity> entityType, String whereClause) {
     setDataModelEntityDefinition(new DataModelEntityPartDefinition(entityType, whereClause));
   }
 
   /**
    * <b>Data model entity</b>:<br>
    * The whereClause is something like <code><pre>
    * EXISTS (
    * SELECT 1
    * FROM PERSON @PERSON@
    * WHERE @PERSON@.PERSON_ID=@parent.PERSON@.PERSON_ID
    * &lt;whereParts/&gt;
    * &lt;groupBy&gt;
    *  GROUP BY @PERSON@.PERSON_ID
    *  HAVING 1=1
    *  &lt;havingParts/&gt;
    * &lt;/groupBy&gt;
    * )
    * </pre></code> <br>
    * The selectClause is something like <code><pre>
    * ( SELECT &lt;column/&gt;
    * FROM PERSON @PERSON@
    * WHERE @PERSON@.PERSON_ID=@parent.PERSON@.PERSON_ID
    * &lt;whereParts/&gt;
    * )
    * </pre></code> <br>
    * The <i>whereParts</i> tag is replaced with all attributes contained in the entity that have no aggregation type.
    * Every attribute contributes a "AND <i>attribute</i> <i>op</i> <i>value</i>" line.<br>
    * The <i>groupBy</i> tag is only used when there are attributes in the entity that have an aggregation type.<br>
    * The <i>havingParts</i> tag is replaced with all attributes contained in the entity that have an aggregation type.
    * Every aggregation attribute contributes a "AND <i>fun</i>(<i>attribute</i>) <i>op</i> <i>value</i>" line.<br>
    */
   public void setDataModelEntityDefinition(Class<? extends IDataModelEntity> entityType, String whereClause, String selectClause) {
     setDataModelEntityDefinition(new DataModelEntityPartDefinition(entityType, whereClause, selectClause));
   }
 
   /**
    * see {@link #setDataModelEntityDefinition(Class, String)}
    */
   public void setDataModelEntityDefinition(DataModelEntityPartDefinition def) {
     m_dataModelEntMap.put(def.getEntityType(), def);
   }
 
   /**
    * Convenience for {@link #getAliasMapper()} and {@link AliasMapper#setRootAlias(String, String)}
    */
   public void setRootAlias(String entityName, String alias) {
     getAliasMapper().setRootAlias(entityName, alias);
   }
 
   protected FormDataStatementBuilderCheck createCheckInstance() {
     return new FormDataStatementBuilderCheck(this);
   }
 
   public void check(Object o) {
     FormDataStatementBuilderCheck c = createCheckInstance();
     c.checkRec(o);
     System.out.println(c.toString());
   }
 
   @SuppressWarnings("cast")
   public String build(AbstractFormData formData) throws ProcessingException {
     m_where = new StringBuffer();
     // get all formData fields and properties defined directly and indirectly by extending template fields, respectively
     //build constraints for fields
     for (BasicPartDefinition def : m_basicDefs) {
       if (def.accept(formData)) {
         Map<String, String> parentAliasMap = getAliasMapper().getRootAliases();
         EntityContribution contrib = def.createInstance(this, formData, parentAliasMap);
         // if there are no where parts, do nothing
         if (contrib != null && contrib.getWhereParts().size() != 0) {
           String wherePart = ListUtility.format(contrib.getWhereParts(), " AND ");
           if (contrib.getFromParts().size() > 0) {
             // there are from parts
             // create an EXISTS (SELECT 1 FROM ... WHERE ...)
             String fromPart = ListUtility.format(contrib.getFromParts(), ", ");
             addWhere(" AND EXISTS (SELECT 1 FROM " + fromPart + " WHERE " + wherePart + ")");
           }
           else {
             // no from parts, just use the where parts
             addWhere(" AND " + wherePart);
           }
         }
       }
     }
     //build constraints for composer trees
     Map<Integer, Map<String, AbstractFormFieldData>> fieldsBreathFirstMap = formData.getAllFieldsRec();
     for (Map<String, AbstractFormFieldData> map : fieldsBreathFirstMap.values()) {
       for (AbstractFormFieldData f : map.values()) {
         if (f.isValueSet()) {
           if (f instanceof AbstractTreeFieldData) {
             // composer tree with entity, attribute
             EntityContribution contrib = buildTreeNodes(((AbstractTreeFieldData) f).getRoots(), EntityStrategy.BuildConstraints, AttributeStrategy.BuildConstraintOfAttributeWithContext);
             // if there are no where parts, do nothing
             if (contrib.getWhereParts().size() != 0) {
               String wherePart = ListUtility.format(contrib.getWhereParts(), " AND ");
 
               if (contrib.getFromParts().size() > 0) {
                 // there are from parts
                 // create an EXISTS (SELECT 1 FROM ... WHERE ...)
                 String fromPart = ListUtility.format(contrib.getFromParts(), ", ");
                 addWhere(" AND EXISTS (SELECT 1 FROM " + fromPart + " WHERE " + wherePart + ")");
               }
               else {
                 // no from parts, just use the where parts
                 addWhere(" AND " + wherePart);
               }
             }
           }
         }
       }
     }
     return getWhereConstraints();
   }
 
   protected boolean isZeroTraversingAttribute(int operation, Object[] values) {
     Number value1 = values != null && values.length > 0 && values[0] instanceof Number ? (Number) values[0] : null;
     Number value2 = values != null && values.length > 1 && values[1] instanceof Number ? (Number) values[1] : null;
     switch (operation) {
       case OPERATOR_EQ: {
         if (value1 != null) {
           return value1.longValue() == 0;
         }
         break;
       }
       case OPERATOR_GE: {
         if (value1 != null) {
           return value1.doubleValue() <= 0;
         }
         break;
       }
       case OPERATOR_GT: {
         if (value1 != null) {
           return value1.doubleValue() < 0;
         }
         break;
       }
       case OPERATOR_LE: {
         if (value1 != null) {
           return value1.doubleValue() >= 0;
         }
         break;
       }
       case OPERATOR_LT: {
         if (value1 != null) {
           return value1.doubleValue() > 0;
         }
         break;
       }
       case OPERATOR_NEQ: {
         if (value1 != null) {
           return value1.longValue() != 0;
         }
         break;
       }
       case OPERATOR_BETWEEN: {
         if (value1 != null && value2 != null) {
           return value1.doubleValue() <= 0 && value2.doubleValue() >= 0;
         }
         else if (value1 != null) {
           return value1.doubleValue() <= 0;
         }
         else if (value2 != null) {
           return value2.doubleValue() >= 0;
         }
         break;
       }
     }
     return false;
   }
 
   public AliasMapper getAliasMapper() {
     return m_aliasMapper;
   }
 
   /**
    * @return the life bind map
    */
   public Map<String, Object> getBindMap() {
     return m_bindMap;
   }
 
   public ISqlStyle getSqlStyle() {
     return m_sqlStyle;
   }
 
   /**
    * Convenience for {@link #getBindMap()}.put(name,value)
    */
   public void addBinds(String[] names, Object[] values) {
     if (names != null) {
       for (int i = 0; i < names.length; i++) {
         addBind(names[i], values[i]);
       }
     }
   }
 
   /**
    * Convenience for {@link #getBindMap()}.put(name,value)
    */
   public void addBind(String name, Object value) {
     if (name != null && !name.startsWith(ISqlStyle.PLAIN_BIND_MARKER_PREFIX)) {
       getBindMap().put(name, value);
     }
   }
 
   /**
    * add sql part with custom binds the ADD keyword is NOT added (pre-pended)
    * automatically
    */
   public void addWhere(String sql, NVPair... customBinds) {
     if (sql != null) {
       m_where.append(" ");
       m_where.append(sql);
       for (NVPair p : customBinds) {
         addBind(p.getName(), p.getValue());
       }
     }
   }
 
   /**
    * @deprecated use {@link #getBasicPartDefinitions()} instead
    */
   @Deprecated
   public List<BasicPartDefinition> getValuePartDefinitions() {
     return Collections.unmodifiableList(m_basicDefs);
   }
 
   public List<BasicPartDefinition> getBasicPartDefinitions() {
     return Collections.unmodifiableList(m_basicDefs);
   }
 
   public Map<Class<?>, DataModelAttributePartDefinition> getDataModelAttributePartDefinitions() {
     return Collections.unmodifiableMap(m_dataModelAttMap);
   }
 
   public Map<Class<?>, DataModelEntityPartDefinition> getDataModelEntityPartDefinitions() {
     return Collections.unmodifiableMap(m_dataModelEntMap);
   }
 
   public String getWhereConstraints() {
     return m_where.toString();
   }
 
   /**
    * Replace bind name by unique bind name so that it is not
    * conflicting with other parts that use the same statement
    * part and bind name. For example S is replaces by __S123.
    */
   public String localizeBindName(String bindName, String prefix) {
     if (bindName != null) {
       String locName = prefix + bindName + getNextBindSeqNo();
       return locName;
     }
     return null;
   }
 
   /**
    * Replace bind name in statement
    */
   public String localizeStatement(String stm, String oldBindName, String newBindName) {
     stm = stm.replaceAll("#" + oldBindName + "#", "#" + newBindName + "#");
     stm = stm.replaceAll("\\&" + oldBindName + "\\&", "&" + newBindName + "&");
     stm = stm.replaceAll(":" + oldBindName + "([^A-Za-z0-9_])", ":" + newBindName + "$1");
     stm = stm.replaceAll(":" + oldBindName + "$", ":" + newBindName);
     return stm;
   }
 
   protected long getNextBindSeqNo() {
     return m_sequenceProvider.incrementAndGet();
   }
 
   @SuppressWarnings("unchecked")
   public static <T extends TreeNodeData> T getParentNodeOfType(TreeNodeData node, Class<T> type) {
     if (node == null) {
       return null;
     }
     while (node != null) {
       node = node.getParentNode();
       if (node != null && type.isAssignableFrom(node.getClass())) {
         return (T) node;
       }
     }
     return null;
   }
 
   public AttributeKind getAttributeKind(TreeNodeData node) {
     if (!(node instanceof ComposerAttributeNodeData)) {
       return AttributeKind.Undefined;
     }
     //
     ComposerAttributeNodeData attributeNode = (ComposerAttributeNodeData) node;
     Integer agg = attributeNode.getAggregationType();
     if (agg == null || agg == AGGREGATION_NONE) {
       if (!isZeroTraversingAttribute(attributeNode.getOperator(), attributeNode.getValues())) {
         return AttributeKind.NonAggregationNonZeroTraversing;
       }
       return AttributeKind.NonAggregation;
     }
     //
     if (!isZeroTraversingAttribute(attributeNode.getOperator(), attributeNode.getValues())) {
       return AttributeKind.AggregationNonZeroTraversing;
     }
     return AttributeKind.Aggregation;
   }
 
   /**
    * @param nodes
    * @return the complete string of all attribute contributions
    * @throws ProcessingException
    */
   public EntityContribution buildTreeNodes(List<TreeNodeData> nodes, EntityStrategy entityStrategy, AttributeStrategy attributeStrategy) throws ProcessingException {
     EntityContribution contrib = new EntityContribution();
     int i = 0;
     while (i < nodes.size()) {
       if (nodes.get(i) instanceof ComposerEntityNodeData) {
         String s = buildComposerEntityNode((ComposerEntityNodeData) nodes.get(i), entityStrategy);
         if (s != null) {
           contrib.getWhereParts().add(s);
         }
         i++;
       }
       else if (nodes.get(i) instanceof ComposerAttributeNodeData) {
         EntityContribution subContrib = buildComposerAttributeNode((ComposerAttributeNodeData) nodes.get(i), attributeStrategy);
         if (!subContrib.isEmpty()) {
           contrib.add(subContrib);
         }
         i++;
       }
       else if (nodes.get(i) instanceof ComposerEitherOrNodeData) {
         ArrayList<ComposerEitherOrNodeData> orNodes = new ArrayList<ComposerEitherOrNodeData>();
         orNodes.add((ComposerEitherOrNodeData) nodes.get(i));
         int k = i;
         while (k + 1 < nodes.size() && (nodes.get(k + 1) instanceof ComposerEitherOrNodeData) && !((ComposerEitherOrNodeData) nodes.get(k + 1)).isBeginOfEitherOr()) {
           orNodes.add((ComposerEitherOrNodeData) nodes.get(k + 1));
           k++;
         }
         EntityContribution subContrib = buildComposerOrNodes(orNodes, entityStrategy, attributeStrategy);
         if (!subContrib.isEmpty()) {
           contrib.add(subContrib);
         }
         i = k + 1;
       }
       else {
         EntityContribution subContrib = buildTreeNodes(nodes.get(i).getChildNodes(), entityStrategy, attributeStrategy);
         if (!subContrib.isEmpty()) {
           contrib.add(subContrib);
         }
       }
     }
     return contrib;
   }
 
   @SuppressWarnings("unchecked")
   protected EntityContribution buildComposerOrNodes(List<ComposerEitherOrNodeData> nodes, EntityStrategy entityStrategy, AttributeStrategy attributeStrategy) throws ProcessingException {
     EntityContribution contrib = new EntityContribution();
     // check if only one condition
     StringBuilder buf = new StringBuilder();
     int count = 0;
     for (ComposerEitherOrNodeData node : nodes) {
       EntityContribution subContrib = buildTreeNodes(node.getChildNodes(), entityStrategy, attributeStrategy);
       contrib.getFromParts().addAll(subContrib.getFromParts());
       if (subContrib.getWhereParts().size() + subContrib.getHavingParts().size() > 0) {
         if (count > 0) {
           buf.append(" OR ");
           if (node.isNegative()) {
             buf.append(" NOT ");
           }
         }
         buf.append("(");
         // remove possible outer join signs (+) in where / having constraint
         // this is necessary because outer joins are not allowed in OR clause
         // the removal of outer joins does not influence the result set
         buf.append(ListUtility.format(ListUtility.combine(subContrib.getWhereParts(), subContrib.getHavingParts()), " AND ").replaceAll("\\(\\+\\)", ""));
         buf.append(")");
         count++;
       }
     }
     if (count > 0) {
       if (count > 1) {
         buf.insert(0, "(");
         buf.append(")");
         contrib.getWhereParts().add(buf.toString());
       }
       else {
         String s = buf.toString();
         if (s.matches("\\(.*\\)")) {
           s = s.substring(1, s.length() - 1).trim();
         }
         contrib.getWhereParts().add(s);
       }
     }
     return contrib;
   }
 
   public String buildComposerEntityNode(ComposerEntityNodeData node, EntityStrategy entityStrategy) throws ProcessingException {
     if (getDataModel() == null) {
       throw new ProcessingException("there is no data model set, call FormDataStatementBuilder.setDataModel to set one");
     }
     IDataModelEntity entity = DataModelUtility.externalIdToEntity(getDataModel(), node.getEntityExternalId(), null);
     if (entity == null) {
       LOG.warn("no entity for external id: " + node.getEntityExternalId());
       return null;
     }
     DataModelEntityPartDefinition def = m_dataModelEntMap.get(entity.getClass());
     if (def == null) {
       LOG.warn("no PartDefinition for entity: " + entity);
       return null;
     }
     ComposerEntityNodeData parentEntityNode = getParentNodeOfType(node, ComposerEntityNodeData.class);
     Map<String, String> parentAliasMap = (parentEntityNode != null ? m_aliasMapper.getNodeAliases(parentEntityNode) : m_aliasMapper.getRootAliases());
     String baseStm;
     switch (entityStrategy) {
       case BuildQuery: {
         baseStm = def.getSelectClause();
         break;
       }
       case BuildConstraints: {
         baseStm = def.getWhereClause();
         break;
       }
       default: {
         baseStm = null;
       }
     }
     String stm = null;
     if (baseStm != null) {
       stm = def.createInstance(this, node, entityStrategy, baseStm, parentAliasMap);
     }
     if (stm == null) {
       return null;
     }
     m_aliasMapper.addAllNodeEntitiesFrom(node, stm);
     stm = m_aliasMapper.replaceMarkersByAliases(stm, m_aliasMapper.getNodeAliases(node), parentAliasMap);
     String s = buildComposerEntityEitherOrSplit(entityStrategy, stm, node.isNegative(), node.getChildNodes());
     return s;
   }
 
   protected String buildComposerEntityEitherOrSplit(EntityStrategy entityStrategy, String baseStm, boolean negative, List<TreeNodeData> childParts) throws ProcessingException {
     List<List<ComposerEitherOrNodeData>> orBlocks = new ArrayList<List<ComposerEitherOrNodeData>>();
     List<TreeNodeData> otherParts = new ArrayList<TreeNodeData>();
     List<ComposerEitherOrNodeData> currentOrBlock = new ArrayList<ComposerEitherOrNodeData>();
     for (TreeNodeData ch : childParts) {
       if (ch instanceof ComposerEitherOrNodeData) {
         ComposerEitherOrNodeData orData = (ComposerEitherOrNodeData) ch;
         if (orData.isBeginOfEitherOr()) {
           if (currentOrBlock.size() > 0) {
             orBlocks.add(new ArrayList<ComposerEitherOrNodeData>(currentOrBlock));
           }
           currentOrBlock.clear();
         }
         currentOrBlock.add(orData);
       }
       else {
         otherParts.add(ch);
       }
     }
     if (currentOrBlock.size() > 0) {
       orBlocks.add(new ArrayList<ComposerEitherOrNodeData>(currentOrBlock));
       currentOrBlock.clear();
     }
     //
     if (orBlocks.size() > 0) {
       StringBuilder blockBuf = new StringBuilder();
       int blockCount = 0;
       for (List<ComposerEitherOrNodeData> list : orBlocks) {
         int elemCount = 0;
         StringBuilder elemBuf = new StringBuilder();
         for (ComposerEitherOrNodeData orData : list) {
           ArrayList<TreeNodeData> subList = new ArrayList<TreeNodeData>();
           subList.addAll(otherParts);
           subList.addAll(orData.getChildNodes());
           String s = buildComposerEntityEitherOrSplit(entityStrategy, baseStm, negative ^ orData.isNegative(), subList);
           if (s != null) {
             if (elemCount > 0) {
               elemBuf.append(" OR ");
             }
             elemBuf.append(" ( ");
             elemBuf.append(s);
             elemBuf.append(" ) ");
             elemCount++;
           }
         }
         if (elemCount > 0) {
           if (blockCount > 0) {
             blockBuf.append(" AND ");
           }
           blockBuf.append(" ( ");
           blockBuf.append(elemBuf.toString());
           blockBuf.append(" ) ");
           blockCount++;
         }
       }
       if (blockCount > 0) {
         return blockBuf.toString();
       }
       return null;
     }
     return buildComposerEntityZeroTraversingSplit(entityStrategy, baseStm, negative, childParts);
   }
 
   protected String buildComposerEntityZeroTraversingSplit(EntityStrategy entityStrategy, String baseStm, boolean negative, List<TreeNodeData> childParts) throws ProcessingException {
     ArrayList<TreeNodeData> nonZeroChildren = new ArrayList<TreeNodeData>(2);
     for (TreeNodeData ch : childParts) {
       switch (getAttributeKind(ch)) {
         case Undefined:
         case NonAggregationNonZeroTraversing:
         case AggregationNonZeroTraversing: {
           nonZeroChildren.add(ch);
           break;
         }
       }
     }
     //
     //create entity part 1
     String entityPart1 = buildComposerEntityUnit(entityStrategy, baseStm, negative, childParts);
     //create negated entity part 2
     String entityPart2 = null;
     if (nonZeroChildren.size() < childParts.size()) {
       // negated negation
       entityPart2 = buildComposerEntityUnit(entityStrategy, baseStm, !negative, nonZeroChildren);
     }
     //combine parts
     if (entityPart2 != null) {
       return " ( " + entityPart1 + " OR " + entityPart2 + " ) ";
     }
     return entityPart1;
   }
 
   protected String buildComposerEntityUnit(EntityStrategy entityStrategy, String baseStm, boolean negative, List<TreeNodeData> childParts) throws ProcessingException {
     EntityContribution contrib = new EntityContribution();
     switch (entityStrategy) {
       case BuildConstraints: {
         ArrayList<TreeNodeData> nonAggregationParts = new ArrayList<TreeNodeData>(childParts.size());
         ArrayList<TreeNodeData> aggregationParts = new ArrayList<TreeNodeData>(2);
         for (TreeNodeData ch : childParts) {
           switch (getAttributeKind(ch)) {
             case Undefined:
             case NonAggregation:
             case NonAggregationNonZeroTraversing: {
               nonAggregationParts.add(ch);
               break;
             }
             case Aggregation:
             case AggregationNonZeroTraversing: {
               aggregationParts.add(ch);
               break;
             }
           }
         }
         //
         EntityContribution subContrib = buildTreeNodes(nonAggregationParts, entityStrategy, AttributeStrategy.BuildConstraintOfAttributeWithContext);
         contrib.add(subContrib);
         //
         subContrib = buildTreeNodes(aggregationParts, entityStrategy, AttributeStrategy.BuildConstraintOfContext);
         contrib.add(subContrib);
         //
         subContrib = buildTreeNodes(aggregationParts, entityStrategy, AttributeStrategy.BuildConstraintOfAttribute);
         contrib.add(subContrib);
         break;
       }
       case BuildQuery: {
         EntityContribution subContrib = buildTreeNodes(childParts, entityStrategy, AttributeStrategy.BuildQueryOfAttributeAndConstraintOfContext);
         contrib.add(subContrib);
         break;
       }
     }
     //
     String entityPart = createEntityPart(baseStm, negative, contrib);
     return entityPart;
   }
 
   @SuppressWarnings("cast")
   public EntityContribution buildComposerAttributeNode(final ComposerAttributeNodeData node, AttributeStrategy attributeStrategy) throws ProcessingException {
     if (getDataModel() == null) {
       throw new ProcessingException("there is no data model set, call FormDataStatementBuilder.setDataModel to set one");
     }
     IDataModelAttribute attribute = DataModelUtility.externalIdToAttribute(getDataModel(), node.getAttributeExternalId(), null);
     if (attribute == null) {
       LOG.warn("no attribute for external id: " + node.getAttributeExternalId());
       return new EntityContribution();
     }
     DataModelAttributePartDefinition def = m_dataModelAttMap.get(attribute.getClass());
     if (def == null) {
       Integer agg = node.getAggregationType();
       if (agg != null && agg == AGGREGATION_COUNT) {
         def = new DataModelAttributePartDefinition(null, "1", false);
       }
     }
     if (def == null) {
       LOG.warn("no PartDefinition for attribute: " + attribute);
       return new EntityContribution();
     }
     List<Object> bindValues = new ArrayList<Object>();
     if (node.getValues() != null) {
       bindValues.addAll(Arrays.asList(node.getValues()));
     }
     List<String> bindNames = new ArrayList<String>(bindValues.size());
     for (int i = 0; i < bindValues.size(); i++) {
       bindNames.add("" + (char) (((int) 'a') + i));
     }
     AliasMapper aliasMap = getAliasMapper();
     ComposerEntityNodeData parentEntityNode = FormDataStatementBuilder.getParentNodeOfType(node, ComposerEntityNodeData.class);
     Map<String, String> parentAliasMap = parentEntityNode != null ? aliasMap.getNodeAliases(parentEntityNode) : aliasMap.getRootAliases();
     String stm = null;
     switch (attributeStrategy) {
       case BuildConstraintOfAttribute:
       case BuildConstraintOfContext:
       case BuildConstraintOfAttributeWithContext: {
         stm = def.getWhereClause();
         break;
       }
       case BuildQueryOfAttributeAndConstraintOfContext: {
         stm = def.getSelectClause();
         break;
       }
     }
     EntityContribution contrib = null;
     if (stm != null) {
       contrib = def.createInstance(this, node, attributeStrategy, stm, bindNames, bindValues, parentAliasMap);
     }
     if (contrib == null) {
       contrib = new EntityContribution();
     }
     switch (attributeStrategy) {
       case BuildQueryOfAttributeAndConstraintOfContext: {
         if (contrib.getSelectParts().isEmpty()) {
           contrib.getSelectParts().add("NULL");
         }
         break;
       }
     }
     return contrib;
   }
 
   public String createEntityPart(String stm, boolean negative, EntityContribution contrib) throws ProcessingException {
     String entityPart = stm;
     // extend the select section
     if (contrib.getSelectParts().size() > 0) {
       final String s = ListUtility.format(contrib.getSelectParts(), ", ");
       if (StringUtility.getTag(entityPart, "selectParts") != null) {
         entityPart = StringUtility.replaceTags(entityPart, "selectParts", new ITagProcessor() {
           @Override
           public String processTag(String tagName, String tagContent) {
             if (tagContent.length() > 0) {
               return tagContent + ", " + s;
             }
             return s;
           }
         });
       }
       else {
         throw new IllegalArgumentException("missing <selectParts/> tag");
       }
     }
     entityPart = StringUtility.removeTagBounds(entityPart, "selectParts");
     // extend the from section
     TreeSet<String> fromParts = new TreeSet<String>(contrib.getFromParts());
     if (fromParts.size() > 0) {
       final String s = ListUtility.format(fromParts, ", ");
       if (StringUtility.getTag(entityPart, "fromParts") != null) {
         entityPart = StringUtility.replaceTags(entityPart, "fromParts", new ITagProcessor() {
           @Override
           public String processTag(String tagName, String tagContent) {
             return tagContent + ", " + s;//legacy: always prefix an additional ,
           }
         });
       }
       else {
         throw new IllegalArgumentException("missing <fromParts/> tag");
       }
     }
     entityPart = StringUtility.removeTagBounds(entityPart, "fromParts");
     // extend the where section
     if (contrib.getWhereParts().size() > 0) {
       final String s = ListUtility.format(contrib.getWhereParts(), " AND ");
       if (StringUtility.getTag(entityPart, "whereParts") != null) {
         entityPart = StringUtility.replaceTags(entityPart, "whereParts", new ITagProcessor() {
           @Override
           public String processTag(String tagName, String tagContent) {
            return " AND " + s;//legacy: always prefix an additional AND
           }
         });
       }
       else {
         entityPart = entityPart + " AND " + s;
       }
     }
     entityPart = StringUtility.removeTagBounds(entityPart, "whereParts");
     // extend the group by / having section
     int selectGroupByDelta = contrib.getSelectParts().size() - contrib.getGroupByParts().size();
     if ((selectGroupByDelta > 0 && contrib.getGroupByParts().size() > 0) || contrib.getHavingParts().size() > 0) {
       entityPart = StringUtility.removeTagBounds(entityPart, "groupBy");
       if (contrib.getGroupByParts().size() > 0) {
         final String s = ListUtility.format(contrib.getGroupByParts(), ", ");
         if (StringUtility.getTag(entityPart, "groupByParts") != null) {
           entityPart = StringUtility.replaceTags(entityPart, "groupByParts", new ITagProcessor() {
             @Override
             public String processTag(String tagName, String tagContent) {
               if (tagContent.length() > 0) {
                 return tagContent + ", " + s;
               }
               return s;
             }
           });
         }
         else {
           throw new IllegalArgumentException("missing <groupByParts/> tag");
         }
       }
       entityPart = StringUtility.removeTagBounds(entityPart, "groupByParts");
       //
       if (contrib.getHavingParts().size() > 0) {
         final String s = ListUtility.format(contrib.getHavingParts(), " AND ");
         if (StringUtility.getTag(entityPart, "havingParts") != null) {
           entityPart = StringUtility.replaceTags(entityPart, "havingParts", new ITagProcessor() {
             @Override
             public String processTag(String tagName, String tagContent) {
               return tagContent + " AND " + s;//legacy: always prefix an additional AND
             }
           });
         }
         else {
           throw new IllegalArgumentException("missing <havingParts/> tag");
         }
       }
       else {
         entityPart = StringUtility.removeTagBounds(entityPart, "havingParts");
       }
     }
     else {
       entityPart = StringUtility.removeTag(entityPart, "groupBy");
     }
     // negation
     if (negative) {
       entityPart = " NOT (" + entityPart + ") ";
     }
     return entityPart;
   }
 
   /**
    * adding an attribute as an entity contribution
    * <p>
    * 
    * @param stm
    *          may contain attribute, fromPart and wherePart tags
    */
   public EntityContribution createAttributePart(AttributeStrategy attributeStrategy, Integer aggregationType, String stm, int operation, List<String> bindNames, List<Object> bindValues, final boolean plainBind, Map<String, String> parentAliasMap) throws ProcessingException {
     if (stm == null) {
       return new EntityContribution();
     }
     //convenience: automatically wrap attribute in attribute tags
     if (stm.indexOf("<attribute>") < 0) {
       stm = "<attribute>" + stm + "</attribute>";
     }
     //convenience: automatically add missing alias on plain attributes, but only if the parent entity has at most 1 alias mapping
     Matcher m = PLAIN_ATTRIBUTE_PATTERN.matcher(stm);
     if (m.find()) {
       if (parentAliasMap.size() == 0) {
         //nop
       }
       else if (parentAliasMap.size() == 1) {
         stm = m.replaceAll("$1@parent." + parentAliasMap.keySet().iterator().next() + "@.$2$3");
       }
       else {
         throw new ProcessingException("composer attribute " + stm + " uses no @...@ alias prefix, but parent has more than 1 alias: " + parentAliasMap);
       }
     }
     boolean isAg = (aggregationType != null && aggregationType != AGGREGATION_NONE);
     EntityContribution contrib = new EntityContribution();
     //special handling of NOT: wrap NOT around complete constraint text and not only in attribute operator
     int positiveOperation;
     boolean negation;
     switch (operation) {
       case OPERATOR_DATE_IS_NOT_TODAY: {
         positiveOperation = OPERATOR_DATE_IS_TODAY;
         negation = true;
         break;
       }
       case OPERATOR_DATE_NEQ: {
         positiveOperation = OPERATOR_DATE_EQ;
         negation = true;
         break;
       }
       case OPERATOR_DATE_TIME_IS_NOT_NOW: {
         positiveOperation = OPERATOR_DATE_TIME_IS_NOW;
         negation = true;
         break;
       }
       case OPERATOR_DATE_TIME_NEQ: {
         positiveOperation = OPERATOR_DATE_TIME_EQ;
         negation = true;
         break;
       }
       case OPERATOR_NEQ: {
         positiveOperation = OPERATOR_EQ;
         negation = true;
         break;
       }
       case OPERATOR_NOT_CONTAINS: {
         positiveOperation = OPERATOR_CONTAINS;
         negation = true;
         break;
       }
       case OPERATOR_NOT_ENDS_WITH: {
         positiveOperation = OPERATOR_ENDS_WITH;
         negation = true;
         break;
       }
       case OPERATOR_NOT_IN: {
         positiveOperation = OPERATOR_IN;
         negation = true;
         break;
       }
       case OPERATOR_NOT_NULL: {
         positiveOperation = OPERATOR_NULL;
         negation = true;
         break;
       }
       case OPERATOR_NOT_STARTS_WITH: {
         positiveOperation = OPERATOR_STARTS_WITH;
         negation = true;
         break;
       }
       case OPERATOR_NUMBER_NOT_NULL: {
         positiveOperation = OPERATOR_NUMBER_NULL;
         negation = true;
         break;
       }
       case OPERATOR_TIME_IS_NOT_NOW: {
         positiveOperation = OPERATOR_TIME_IS_NOW;
         negation = true;
         break;
       }
       default: {
         positiveOperation = operation;
         negation = false;
       }
     }
     //
     String fromPart = StringUtility.getTag(stm, "fromPart");
     stm = StringUtility.removeTag(stm, "fromPart").trim();
     String wherePart = StringUtility.getTag(stm, "wherePart");
     if (wherePart == null) {
       String tmp = StringUtility.removeTag(stm, "attribute").trim();
       if (tmp.length() > 0) {
         wherePart = stm;
         stm = "";
       }
     }
     stm = StringUtility.removeTag(stm, "wherePart").trim();
     String attPart = StringUtility.getTag(stm, "attribute");
     stm = StringUtility.removeTag(stm, "attribute").trim();
     if (stm.length() > 0) {
       LOG.warn("attribute part is not well-formed; contains wherePart tag and also other sql text: " + stm);
     }
     //
     //from
     if (fromPart != null) {
       //resolve aliases in from
       // miss-using 'contrib' as a "node" because real node is not accessible
       m_aliasMapper.addMissingNodeEntitiesFrom(contrib, fromPart);
       Map<String, String> aliasMap = m_aliasMapper.getNodeAliases(contrib);
       parentAliasMap.putAll(aliasMap);
       fromPart = m_aliasMapper.replaceMarkersByAliases(fromPart, parentAliasMap, parentAliasMap);
       contrib.getFromParts().add(fromPart);
     }
     switch (attributeStrategy) {
       //select ... where
       case BuildQueryOfAttributeAndConstraintOfContext: {
         //select
         if (attPart != null) {
           String sql = createSqlPart(aggregationType, attPart, OPERATOR_NONE, bindNames, bindValues, plainBind, parentAliasMap);
           if (sql != null) {
             contrib.getSelectParts().add(sql);
             if (!isAg) {
               contrib.getGroupByParts().add(sql);
             }
           }
         }
         //where
         if (wherePart != null) {
           wherePart = StringUtility.replaceTags(wherePart, "attribute", "1=1").trim();
           String sql = createSqlPart(wherePart, bindNames, bindValues, plainBind, parentAliasMap);
           if (sql != null) {
             contrib.getWhereParts().add(sql);
           }
         }
         break;
       }
         //where / having
       case BuildConstraintOfAttribute: {
         if (attPart != null) {
           String sql = createSqlPart(aggregationType, attPart, positiveOperation, bindNames, bindValues, plainBind, parentAliasMap);
           if (sql != null) {
             if (negation) {
               sql = "NOT(" + sql + ")";
             }
             if (isAg) {
               contrib.getHavingParts().add(sql);
             }
             else {
               contrib.getWhereParts().add(sql);
             }
           }
         }
         break;
       }
       case BuildConstraintOfContext: {
         if (wherePart != null) {
           wherePart = StringUtility.replaceTags(wherePart, "attribute", "1=1").trim();
           String sql = createSqlPart(wherePart, bindNames, bindValues, plainBind, parentAliasMap);
           if (sql != null) {
             contrib.getWhereParts().add(sql);
           }
         }
         break;
       }
       case BuildConstraintOfAttributeWithContext: {
         String whereAndAttPart = (wherePart != null ? wherePart : "") + (wherePart != null && attPart != null ? " AND " : "") + (attPart != null ? "<attribute>" + attPart + "</attribute>" : "");
         if (whereAndAttPart.length() > 0) {
           String sql = createSqlPart(aggregationType, whereAndAttPart, positiveOperation, bindNames, bindValues, plainBind, parentAliasMap);
           if (sql != null) {
             if (negation) {
               sql = "NOT(" + sql + ")";
             }
             contrib.getWhereParts().add(sql);
           }
         }
         break;
       }
     }
     return contrib;
   }
 
   /**
    * adding an attribute as an entity contribution
    * <p>
    * 
    * @param stm
    *          may contain attribute, fromPart and wherePart tags
    */
   public String createAttributePartSimple(AttributeStrategy attributeStrategy, Integer aggregationType, String stm, int operation, List<String> bindNames, List<Object> bindValues, boolean plainBind, Map<String, String> parentAliasMap) throws ProcessingException {
     EntityContribution contrib = createAttributePart(attributeStrategy, aggregationType, stm, operation, bindNames, bindValues, plainBind, parentAliasMap);
     if (contrib.isEmpty()) {
       return null;
     }
     return ListUtility.format(contrib.getWhereParts(), " AND ");
   }
 
   /**
    * Create sql text, makes bind names unique, and adds all binds to the bind map
    * <p>
    * Convenience for {@link #createSqlPart(AGGREGATION_NONE, String, OPERATOR_NONE, List, List, boolean, Map)}
    */
   public String createSqlPart(String sql, List<String> bindNames, List<Object> bindValues, final boolean plainBind, Map<String, String> parentAliasMap) throws ProcessingException {
     return createSqlPart(AGGREGATION_NONE, sql, OPERATOR_NONE, bindNames, bindValues, plainBind, parentAliasMap);
   }
 
   /**
    * Create sql text, makes bind names unique, and adds all binds to the bind map
    * <p>
    * To use no operator use {@link DataModelConstants#OPERATOR_NONE} and null for binds and values, stm will be
    * decorated and is the result itself
    * <p>
    * To use no aggregation use {@link DataModelConstants#AGGREGATION_NONE}
    */
   public String createSqlPart(final Integer aggregationType, String sql, final int operation, List<String> bindNames, List<Object> bindValues, final boolean plainBind, Map<String, String> parentAliasMap) throws ProcessingException {
     if (sql == null) {
       sql = "";
     }
     if (bindNames == null) {
       bindNames = new ArrayList<String>(0);
     }
     if (bindValues == null) {
       bindValues = new ArrayList<Object>(0);
     }
     // the attribute was of the form: NAME or
     // <attribute>NAME</attribute>
     // make sure there is an attribute tag in the string, if none enclose all
     // by default
     if (sql.indexOf("<attribute>") < 0) {
       sql = "<attribute>" + sql + "</attribute>";
     }
     //convenience: automatically add missing alias on plain attributes, but only if the parent entity has at most 1 alias mapping
     Matcher m = PLAIN_ATTRIBUTE_PATTERN.matcher(sql);
     if (m.find()) {
       if (parentAliasMap.size() == 0) {
         //nop
       }
       else if (parentAliasMap.size() == 1) {
         sql = m.replaceAll("$1@parent." + parentAliasMap.keySet().iterator().next() + "@.$2$3");
       }
       else {
         throw new ProcessingException("root attribute with " + sql + " uses no @...@ alias prefix, but parent has more than 1 alias: " + parentAliasMap);
       }
     }
     //resolve aliases
     sql = m_aliasMapper.replaceMarkersByAliases(sql, parentAliasMap, parentAliasMap);
     // generate unique bind names
     final ArrayList<String> newBindNames = new ArrayList<String>(2);
     for (int i = 0; i < bindNames.size(); i++) {
       String o = bindNames.get(i);
       String n = localizeBindName(o, "__");
       newBindNames.add(n);
       sql = localizeStatement(sql, o, n);
     }
     // part decoration
     final List<Object> valuesFinal = bindValues;
     ITagProcessor processor = new ITagProcessor() {
       @Override
       public String processTag(String tagName, String a) {
         return createSqlOpValuePart(aggregationType, a, operation, newBindNames, valuesFinal, plainBind);
       }
     };
     return StringUtility.replaceTags(sql, "attribute", processor);
   }
 
   public String createSqlOpValuePart(Integer aggregationType, String sql, int operation, List<String> bindNames, List<Object> bindValues, boolean plainBind) {
     String[] names = (bindNames != null ? bindNames.toArray(new String[bindNames.size()]) : new String[0]);
     Object[] values = (bindValues != null ? bindValues.toArray(new Object[bindValues.size()]) : new Object[0]);
     if (plainBind && operation != OPERATOR_NONE) {
       //rewrite bindNames by plain values
       for (int i = 0; i < names.length; i++) {
         names[i] = ISqlStyle.PLAIN_BIND_MARKER_PREFIX + m_sqlStyle.toPlainText(values[i]);
       }
     }
     //
     if (aggregationType != null && aggregationType != AGGREGATION_NONE) {
       switch (aggregationType) {
         case AGGREGATION_COUNT: {
           sql = m_sqlStyle.toAggregationCount(sql);
           break;
         }
         case AGGREGATION_MIN: {
           sql = m_sqlStyle.toAggregationMin(sql);
           break;
         }
         case AGGREGATION_MAX: {
           sql = m_sqlStyle.toAggregationMax(sql);
           break;
         }
         case AGGREGATION_SUM: {
           sql = m_sqlStyle.toAggregationSum(sql);
           break;
         }
         case AGGREGATION_AVG: {
           sql = m_sqlStyle.toAggregationAvg(sql);
           break;
         }
         case AGGREGATION_MEDIAN: {
           sql = m_sqlStyle.toAggregationMedian(sql);
           break;
         }
       }
     }
     else if (isZeroTraversingAttribute(operation, values)) {
       sql = m_sqlStyle.getNvlToken() + "(" + sql + ",0)";
     }
     //
     switch (operation) {
       case OPERATOR_NONE: {
         if (plainBind) {
           if (names != null) {
             HashMap<String, String> tokenValue = new HashMap<String, String>();
             for (int i = 0; i < names.length; i++) {
               tokenValue.put(names[i], m_sqlStyle.toPlainText(values[i]));
             }
             BindModel m = new BindParser(sql).parse();
             IToken[] tokens = m.getIOTokens();
             if (tokens != null) {
               for (IToken iToken : tokens) {
                 if (iToken instanceof ValueInputToken) {
                   ValueInputToken t = (ValueInputToken) iToken;
                   t.setPlainValue(true);
                   t.setReplaceToken(tokenValue.get(t.getName()));
                 }
               }
             }
             sql = m.getFilteredStatement();
           }
         }
         else {
           addBinds(names, values);
         }
         return sql;
       }
       case OPERATOR_BETWEEN: {
         if (!plainBind) {
           addBinds(names, values);
         }
         if (values[0] == null) {
           return m_sqlStyle.createLE(sql, names[1]);
         }
         else if (values[1] == null) {
           return m_sqlStyle.createGE(sql, names[0]);
         }
         else {
           return m_sqlStyle.createBetween(sql, names[0], names[1]);
         }
       }
       case OPERATOR_DATE_BETWEEN: {
         if (!plainBind) {
           addBinds(names, values);
         }
         if (values[0] == null) {
           return m_sqlStyle.createDateLE(sql, names[1]);
         }
         else if (values[1] == null) {
           return m_sqlStyle.createDateGE(sql, names[0]);
         }
         else {
           return m_sqlStyle.createDateBetween(sql, names[0], names[1]);
         }
       }
       case OPERATOR_DATE_TIME_BETWEEN: {
         if (!plainBind) {
           addBinds(names, values);
         }
         if (values[0] == null) {
           return m_sqlStyle.createDateTimeLE(sql, names[1]);
         }
         else if (values[1] == null) {
           return m_sqlStyle.createDateTimeGE(sql, names[0]);
         }
         else {
           return m_sqlStyle.createDateTimeBetween(sql, names[0], names[1]);
         }
       }
       case OPERATOR_EQ: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createEQ(sql, names[0]);
       }
       case OPERATOR_DATE_EQ: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createDateEQ(sql, names[0]);
       }
       case OPERATOR_DATE_TIME_EQ: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createDateTimeEQ(sql, names[0]);
       }
       case OPERATOR_GE: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createGE(sql, names[0]);
       }
       case OPERATOR_DATE_GE: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createDateGE(sql, names[0]);
       }
       case OPERATOR_DATE_TIME_GE: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createDateTimeGE(sql, names[0]);
       }
       case OPERATOR_GT: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createGT(sql, names[0]);
       }
       case OPERATOR_DATE_GT: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createDateGT(sql, names[0]);
       }
       case OPERATOR_DATE_TIME_GT: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createDateTimeGT(sql, names[0]);
       }
       case OPERATOR_LE: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createLE(sql, names[0]);
       }
       case OPERATOR_DATE_LE: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createDateLE(sql, names[0]);
       }
       case OPERATOR_DATE_TIME_LE: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createDateTimeLE(sql, names[0]);
       }
       case OPERATOR_LT: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createLT(sql, names[0]);
       }
       case OPERATOR_DATE_LT: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createDateLT(sql, names[0]);
       }
       case OPERATOR_DATE_TIME_LT: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createDateTimeLT(sql, names[0]);
       }
       case OPERATOR_NEQ: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createNEQ(sql, names[0]);
       }
       case OPERATOR_DATE_NEQ: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createDateNEQ(sql, names[0]);
       }
       case OPERATOR_DATE_TIME_NEQ: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createDateTimeNEQ(sql, names[0]);
       }
       case OPERATOR_DATE_IS_IN_DAYS: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createDateIsInDays(sql, names[0]);
       }
       case OPERATOR_DATE_IS_IN_GE_DAYS: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createDateIsInGEDays(sql, names[0]);
       }
       case OPERATOR_DATE_IS_IN_GE_MONTHS: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createDateIsInGEMonths(sql, names[0]);
       }
       case OPERATOR_DATE_IS_IN_LE_DAYS: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createDateIsInLEDays(sql, names[0]);
       }
       case OPERATOR_DATE_IS_IN_LE_MONTHS: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createDateIsInLEMonths(sql, names[0]);
       }
       case OPERATOR_DATE_IS_IN_LAST_DAYS: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createDateIsInLastDays(sql, names[0]);
       }
       case OPERATOR_DATE_IS_IN_LAST_MONTHS: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createDateIsInLastMonths(sql, names[0]);
       }
       case OPERATOR_DATE_IS_IN_MONTHS: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createDateIsInMonths(sql, names[0]);
       }
       case OPERATOR_DATE_IS_IN_NEXT_DAYS: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createDateIsInNextDays(sql, names[0]);
       }
       case OPERATOR_DATE_IS_IN_NEXT_MONTHS: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createDateIsInNextMonths(sql, names[0]);
       }
       case OPERATOR_DATE_IS_NOT_TODAY: {
         return m_sqlStyle.createDateIsNotToday(sql);
       }
       case OPERATOR_DATE_IS_TODAY: {
         return m_sqlStyle.createDateIsToday(sql);
       }
       case OPERATOR_DATE_TIME_IS_IN_GE_HOURS: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createDateTimeIsInGEHours(sql, names[0]);
       }
       case OPERATOR_DATE_TIME_IS_IN_GE_MINUTES: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createDateTimeIsInGEMinutes(sql, names[0]);
       }
       case OPERATOR_DATE_TIME_IS_IN_LE_HOURS: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createDateTimeIsInLEHours(sql, names[0]);
       }
       case OPERATOR_DATE_TIME_IS_IN_LE_MINUTES: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createDateTimeIsInLEMinutes(sql, names[0]);
       }
       case OPERATOR_DATE_TIME_IS_NOT_NOW: {
         return m_sqlStyle.createDateTimeIsNotNow(sql);
       }
       case OPERATOR_DATE_TIME_IS_NOW: {
         return m_sqlStyle.createDateTimeIsNow(sql);
       }
       case OPERATOR_ENDS_WITH: {
         if (!plainBind) {
           addBind(names[0], m_sqlStyle.toLikePattern(values[0]));
         }
         return m_sqlStyle.createEndsWith(sql, names[0]);
       }
       case OPERATOR_NOT_ENDS_WITH: {
         if (!plainBind) {
           addBind(names[0], m_sqlStyle.toLikePattern(values[0]));
         }
         return m_sqlStyle.createNotEndsWith(sql, names[0]);
       }
       case OPERATOR_IN: {
         if (!plainBind) {
           addBinds(names, values);
           return m_sqlStyle.createIn(sql, names[0]);
         }
         return m_sqlStyle.createInList(sql, values[0]);
       }
       case OPERATOR_CONTAINS: {
         if (!plainBind) {
           addBind(names[0], m_sqlStyle.toLikePattern(values[0]));
         }
         return m_sqlStyle.createContains(sql, names[0]);
       }
       case OPERATOR_LIKE: {
         if (!plainBind) {
           addBind(names[0], m_sqlStyle.toLikePattern(values[0]));
         }
         return m_sqlStyle.createLike(sql, names[0]);
       }
       case OPERATOR_NOT_IN: {
         if (!plainBind) {
           addBinds(names, values);
           return m_sqlStyle.createNotIn(sql, names[0]);
         }
         return m_sqlStyle.createNotInList(sql, values[0]);
       }
       case OPERATOR_NOT_CONTAINS: {
         if (!plainBind) {
           addBind(names[0], m_sqlStyle.toLikePattern(values[0]));
         }
         return m_sqlStyle.createNotContains(sql, names[0]);
       }
       case OPERATOR_NOT_NULL: {
         return m_sqlStyle.createNotNull(sql);
       }
       case OPERATOR_NUMBER_NOT_NULL: {
         return m_sqlStyle.createNumberNotNull(sql);
       }
       case OPERATOR_NULL: {
         return m_sqlStyle.createNull(sql);
       }
       case OPERATOR_NUMBER_NULL: {
         return m_sqlStyle.createNumberNull(sql);
       }
       case OPERATOR_STARTS_WITH: {
         if (!plainBind) {
           addBind(names[0], m_sqlStyle.toLikePattern(values[0]));
         }
         return m_sqlStyle.createStartsWith(sql, names[0]);
       }
       case OPERATOR_NOT_STARTS_WITH: {
         if (!plainBind) {
           addBind(names[0], m_sqlStyle.toLikePattern(values[0]));
         }
         return m_sqlStyle.createNotStartsWith(sql, names[0]);
       }
       case OPERATOR_TIME_IS_IN_GE_HOURS: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createTimeIsInGEHours(sql, names[0]);
       }
       case OPERATOR_TIME_IS_IN_GE_MINUTES: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createTimeIsInGEMinutes(sql, names[0]);
       }
       case OPERATOR_TIME_IS_IN_HOURS: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createTimeIsInHours(sql, names[0]);
       }
       case OPERATOR_TIME_IS_IN_LE_HOURS: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createTimeIsInLEHours(sql, names[0]);
       }
       case OPERATOR_TIME_IS_IN_LE_MINUTES: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createTimeIsInLEMinutes(sql, names[0]);
       }
       case OPERATOR_TIME_IS_IN_MINUTES: {
         if (!plainBind) {
           addBinds(names, values);
         }
         return m_sqlStyle.createTimeIsInMinutes(sql, names[0]);
       }
       case OPERATOR_TIME_IS_NOW: {
         return m_sqlStyle.createTimeIsNow(sql);
       }
       case OPERATOR_TIME_IS_NOT_NOW: {
         return m_sqlStyle.createTimeIsNotNow(sql);
       }
       default: {
         throw new IllegalArgumentException("invalid operator: " + operation);
       }
     }
   }
 }
