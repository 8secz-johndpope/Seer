 /*
  * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
  *
  * This file is part of Resin(R) Open Source
  *
  * Each copy or derived work must preserve the copyright notice and this
  * notice unmodified.
  *
  * Resin Open Source is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.
  *
  * Resin Open Source is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
  * of NON-INFRINGEMENT.  See the GNU General Public License for more
  * details.
  *
  * You should have received a copy of the GNU General Public License
  * along with Resin Open Source; if not, write to the
  *
  *   Free Software Foundation, Inc.
  *   59 Temple Place, Suite 330
  *   Boston, MA 02111-1307  USA
  *
  * @author Scott Ferguson
  */
 
 package com.caucho.amber.field;
 
 import com.caucho.amber.expr.AmberExpr;
 import com.caucho.amber.expr.OneToManyExpr;
 import com.caucho.amber.expr.PathExpr;
 import com.caucho.amber.query.QueryParser;
 import com.caucho.amber.table.LinkColumns;
 import com.caucho.amber.table.Table;
 import com.caucho.amber.type.RelatedType;
 import com.caucho.amber.type.Type;
 import com.caucho.bytecode.JField;
 import com.caucho.bytecode.JType;
 import com.caucho.config.ConfigException;
 import com.caucho.java.JavaWriter;
 import com.caucho.log.Log;
 import com.caucho.util.CharBuffer;
 import com.caucho.util.L10N;
 
 import javax.persistence.CascadeType;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Map;
 import java.util.Set;
 import java.util.logging.Logger;
 
 /**
  * Represents a field to a collection of objects where the target
  * hold a back-link to the source entity.
  */
 public class EntityOneToManyField extends CollectionField {
   private static final L10N L = new L10N(EntityOneToManyField.class);
   protected static final Logger log = Log.open(EntityOneToManyField.class);
 
   private String _mapKey;
 
   private ArrayList<String> _orderByFields;
   private ArrayList<Boolean> _orderByAscending;
 
   private EntityManyToOneField _sourceField;
 
   public EntityOneToManyField(RelatedType entityType,
                               String name,
                               CascadeType[] cascadeTypes)
     throws ConfigException
   {
     super(entityType, name, cascadeTypes);
   }
 
   public EntityOneToManyField(RelatedType entityType,
                               String name)
     throws ConfigException
   {
     this(entityType, name, null);
   }
 
   public EntityOneToManyField(RelatedType entityType)
   {
     super(entityType);
   }
 
   /**
    * Sets the order by.
    */
   public void setOrderBy(ArrayList<String> orderByFields,
                          ArrayList<Boolean> orderByAscending)
   {
     _orderByFields = orderByFields;
     _orderByAscending = orderByAscending;
   }
 
   /**
    * Returns the source type as
    * entity or mapped-superclass.
    */
   public RelatedType getEntitySourceType()
   {
     return (RelatedType) getSourceType();
   }
 
   /**
    * Returns the target type as
    * entity or mapped-superclass.
    */
   public RelatedType getEntityTargetType()
   {
     return (RelatedType) getTargetType();
   }
 
   /**
    * Returns the target type as entity.
    */
   public Type getTargetType()
   {
     return _sourceField.getSourceType();
   }
 
   /**
    * Gets the source field.
    */
   public EntityManyToOneField getSourceField()
   {
     return _sourceField;
   }
 
   /**
    * Sets the source field.
    */
   public void setSourceField(EntityManyToOneField sourceField)
   {
     _sourceField = sourceField;
   }
 
   /**
    * Returns the link.
    */
   public LinkColumns getLinkColumns()
   {
     return _sourceField.getLinkColumns();
   }
 
   /**
    * Gets the map key.
    */
   public String getMapKey()
   {
     return _mapKey;
   }
 
   /**
    * Sets the map key.
    */
   public void setMapKey(String mapKey)
   {
     _mapKey = mapKey;
   }
 
   /**
    * Initialize.
    */
   public void init()
   {
     if (_sourceField == null || getLinkColumns() == null)
       throw new IllegalStateException();
   }
 
   /**
    * Creates the expression for the field.
    */
   public AmberExpr createExpr(QueryParser parser, PathExpr parent)
   {
     return new OneToManyExpr(parser, parent, getLinkColumns());
   }
   /**
    * Generates the (pre) cascade operation from
    * parent to this child. This field will only
    * be cascaded first if the operation can be
    * performed with no risk to break FK constraints.
    */
   public void generatePreCascade(JavaWriter out,
                                  String aConn,
                                  CascadeType cascadeType)
     throws IOException
   {
     if (cascadeType == CascadeType.PERSIST)
       return;
 
     generateInternalCascade(out, aConn, cascadeType);
   }
 
   /**
    * Generates the (post) cascade operation from
    * parent to this child. This field will only
    * be cascaded first if the operation can be
    * performed with no risk to break FK constraints.
    */
   public void generatePostCascade(JavaWriter out,
                                   String aConn,
                                   CascadeType cascadeType)
     throws IOException
   {
     if (cascadeType != CascadeType.PERSIST)
       return;
 
     generateInternalCascade(out, aConn, cascadeType);
   }
 
  protected void generateInternalCascade(JavaWriter out,
                                        String aConn,
                                        CascadeType cascadeType)
     throws IOException
   {
     if (isCascade(cascadeType)) {
 
       String getter = "_caucho_field_" + getGetterName(); // generateSuperGetter();
 
       out.println("if (" + getter + " == null && " + generateSuperGetter() + " != null)");
       out.pushDepth();
       out.println(getSetterName() + "(" + generateSuperGetter() + ");");
       out.popDepth();
 
       out.println();
       out.println("if (" + getter + " != null) {");
       out.pushDepth();
 
       out.print("for (Object o : " + getter);
 
       // jpa/0v04
       if (getJavaType().isAssignableTo(Map.class))
         out.print(".values()");
 
       out.println(") {");
       out.pushDepth();
 
       // XXX
       out.println("if (o == null)");
       out.println("  continue;");
 
       if (_sourceField != null) {
         String typeName = getEntityTargetType().getJavaTypeName();
         String setter = _sourceField.getSetterName();
         out.println("((" + typeName + ") o)." + setter + "(this);");
       }
 
       out.print(aConn + ".");
 
       switch (cascadeType) {
       case PERSIST:
         out.print("persistFromCascade");
         break;
 
       case MERGE:
         out.print("merge");
         break;
 
       case REMOVE:
         out.print("remove");
         break;
 
       case REFRESH:
         out.print("refresh");
         break;
       }
 
       out.println("(o);");
 
       out.popDepth();
       out.println("}");
 
       out.popDepth();
       out.println("}");
     }
   }
 
   /**
    * Generates the set clause.
    */
   public void generateSet(JavaWriter out, String pstmt,
                           String obj, String index)
     throws IOException
   {
   }
 
   /**
    * Generates the select clause.
    */
   public String generateLoadSelect(String id)
   {
     return null;
   }
 
   /**
    * Updates from the cached copy.
    */
   public void generateCopyLoadObject(JavaWriter out,
                                      String dst, String src,
                                      int loadIndex)
     throws IOException
   {
   }
 
   /**
    * Generates the target select.
    */
   public String generateTargetSelect(String id)
   {
     CharBuffer cb = CharBuffer.allocate();
 
     Id key = getEntityTargetType().getId();
 
     cb.append(key.generateSelect(id));
 
     String value = getEntityTargetType().generateLoadSelect(id);
 
     if (cb.length() > 0 && value.length() > 0)
       cb.append(", ");
 
     cb.append(value);
 
     return cb.close();
   }
 
   /**
    * Generates the set property.
    */
   public void generateGetProperty(JavaWriter out)
     throws IOException
   {
     String var = "_caucho_field_" + getGetterName();
 
     boolean isSet = getJavaType().isAssignableTo(Set.class);
     boolean isMap = false;
     if (!isSet) {
       isMap = getJavaType().isAssignableTo(Map.class);
     }
 
     JType type = getJavaType();
     JType []paramArgs = type.getActualTypeArguments();
     JType param = paramArgs.length > 0 ? paramArgs[0] : null;
     JType param2 = paramArgs.length > 1 ? paramArgs[1] : null;
 
     out.print("protected ");
 
     String collectionImpl;
 
     if (isSet)
       collectionImpl = "com.caucho.amber.collection.SetImpl";
     else if (isMap)
       collectionImpl = "com.caucho.amber.collection.MapImpl";
     else
       collectionImpl = "com.caucho.amber.collection.CollectionImpl";
 
     out.print(collectionImpl);
 
     if (param != null) {
       out.print("<");
       out.print(param.getPrintName());
       if (isMap) {
         if (param2 != null) {
           out.print(", ");
           out.print(param2.getPrintName());
         }
       }
       out.print(">");
     }
 
     out.println(" " + var + ";");
 
     out.println();
     out.println("public " + getJavaTypeName() + " " + getGetterName() + "()");
     out.println("{");
     out.pushDepth();
 
     out.println("if (" + var + " != null) {");
     out.pushDepth();
     out.println(var + ".setSession(__caucho_session);");
     out.println("return " + var + ";");
     out.popDepth();
     out.println("}");
 
     out.println();
     out.println("com.caucho.amber.AmberQuery query = null;");
 
     out.println();
     out.println("try {");
     out.pushDepth();
 
     out.println("if (__caucho_session == null) {");
     out.pushDepth();
 
     String newEmptyCollection = "new " + collectionImpl;
 
     if (param != null) {
       newEmptyCollection += "<" + param.getPrintName();
       if (isMap) {
         newEmptyCollection += ", ";
         newEmptyCollection += param2.getPrintName();
       }
       newEmptyCollection += ">";
     }
 
     newEmptyCollection += "(query";
     if (isMap) {
       // jpa/0v00
       newEmptyCollection += "," + getEntityTargetType().getBeanClass().getName();
       newEmptyCollection += ".class.getDeclaredMethod(\"get";
       String getterMapKey = getMapKey();
       getterMapKey = Character.toUpperCase(getterMapKey.charAt(0)) + getterMapKey.substring(1);
       newEmptyCollection += getterMapKey; // "getId");
       newEmptyCollection += "\", (Class []) null)";
     }
     newEmptyCollection += ")";
 
     out.println(var + " = " + newEmptyCollection + ";");
 
     // if (! isAbstract())
     out.println();
     out.println("return " + var + ";");
 
     out.popDepth();
     out.println("}");
 
     out.println();
     out.print("String sql=\"");
 
     out.print("SELECT c");
     out.print(" FROM " + getEntitySourceType().getName() + " o,");
     out.print("      o." + getName() + " c");
     out.print(" WHERE ");
     out.print(getEntitySourceType().getId().generateRawWhere("o"));
 
     if (_orderByFields != null) {
       out.print(" ORDER BY ");
 
       for (int i = 0; i < _orderByFields.size(); i++) {
         if (i != 0)
           out.print(", ");
 
         out.print("c." + _orderByFields.get(i));
         if (Boolean.FALSE.equals(_orderByAscending.get(i)))
           out.print(" DESC");
       }
     }
 
     out.println("\";");
     out.println("query = __caucho_session.prepareQuery(sql);");
 
     out.println("int index = 1;");
     getEntitySourceType().getId().generateSet(out, "query", "index", "this");
 
     // Ex: _caucho_getChildren = new com.caucho.amber.collection.CollectionImpl
     out.print(var);
     out.print(" = " + newEmptyCollection + ";");
 
     /*
       out.pushDepth();
 
       //generateAdd(out);
       //generateRemove(out);
       //generateClear(out);
       // generateSize(out);
 
       out.popDepth();
       out.println("};");
     */
 
     out.println();
     out.println("return " + var + ";");
 
     out.popDepth();
     out.println("} catch (Exception e) {");
     out.println("  throw com.caucho.amber.AmberRuntimeException.create(e);");
     out.println("}");
 
     out.popDepth();
     out.println("}");
   }
 
   /**
    * Generates the size method.
    */
   private void generateSize(JavaWriter out)
     throws IOException
   {
     out.println("public int size()");
     out.println("{");
     out.pushDepth();
 
     out.println("if (__caucho_session == null || isValid())");
     out.println("  return super.size();");
 
     out.println("try {");
     out.pushDepth();
 
     out.println("__caucho_session.flushNoChecks();");
 
     out.print("String sql=\"");
 
     out.print("SELECT count(*) FROM ");
     out.print(getEntitySourceType().getName());
     out.print(" AS o ");
 
     out.print(" WHERE ");
 
     // getKeyColumn().generateRawMatchArgWhere("o");
 
     ArrayList<IdField> keys = getEntitySourceType().getId().getKeys();
     for (int i = 0; i < keys.size(); i++) {
       if (i != 0)
         out.print(" AND ");
 
       out.print("o." + keys.get(i).getName());
       out.print("=?");
     }
 
     out.println("\";");
     out.println("com.caucho.amber.AmberQuery query;");
     out.println("query = __caucho_session.prepareQuery(sql);");
 
     out.println("int index = 1;");
 
     // ejb/06h0
     getEntitySourceType().getId().generateSet(out, "query", "index", getEntitySourceType().getInstanceClassName() + ".this"); // "__ResinExt.this");
 
     out.println("java.sql.ResultSet rs = query.executeQuery();");
 
     out.println("if (rs.next())");
     out.println("  return rs.getInt(1);");
     out.println("else");
     out.println("  return 0;");
 
     out.popDepth();
     out.println("} catch (java.sql.SQLException e) {");
     out.println("  throw com.caucho.amber.AmberRuntimeException.create(e);");
     out.println("}");
 
     out.popDepth();
     out.println("}");
   }
 
   /**
    * Generates the set property.
    */
   public void generateSetProperty(JavaWriter out)
     throws IOException
   {
     // commented out: jpa/0s2d
     // JMethod setter = getSetterMethod();
     //
     // if (setter == null)
     //   return;
     //
     // JClass []paramTypes = setter.getParameterTypes();
 
     JType type;
 
     if (! getEntitySourceType().isFieldAccess()) {
       type = getGetterMethod().getGenericReturnType();
     }
     else {
       JField field = RelatedType.getField(getBeanClass(), getName());
       type = field.getGenericType();
     }
 
     out.println();
     // commented out: jpa/0s2d
     // out.print("public void " + setter.getName() + "(");
     // out.print(getJavaTypeName() + " value)");
     out.print("public void " + getSetterName() + "(");
     out.print(type.getName() + " value)");
     out.println("{");
     out.pushDepth();
 
     // out.println("if (" + generateSuperGetter() + " == value)");
     // out.println("  return;");
     // out.println();
 
     //
     // jpa/0j57 needs to generate the following snippet:
     //
     // _caucho___caucho_get_xAnnualReviews
     //   = new com.caucho.amber.collection.CollectionImpl<qa.XAnnualReview>(__caucho_session, null);
     // _caucho___caucho_get_xAnnualReviews.addAll(0, value);
     //
     // jpa/0j57:
 
     out.println("try {");
     out.pushDepth();
 
     String var = "_caucho_field_" + getGetterName();
 
     out.print(var + " = new ");
 
     type = getJavaType();
 
     boolean isSet = type.isAssignableTo(Set.class);
     boolean isMap = false;
     if (!isSet) {
       isMap = type.isAssignableTo(Map.class);
     }
 
     JType []paramArgs = type.getActualTypeArguments();
     JType param = paramArgs.length > 0 ? paramArgs[0] : null;
     JType param2 = paramArgs.length > 1 ? paramArgs[1] : null;
 
     String collectionImpl;
 
     if (isSet)
       collectionImpl = "com.caucho.amber.collection.SetImpl";
     else if (isMap)
       collectionImpl = "com.caucho.amber.collection.MapImpl";
     else
       collectionImpl = "com.caucho.amber.collection.CollectionImpl";
 
     out.print(collectionImpl);
 
     if (param != null) {
       out.print("<");
       out.print(param.getPrintName());
       if (isMap) {
         if (param2 != null) {
           out.print(", ");
           out.print(param2.getPrintName());
         }
       }
       out.print(">");
     }
 
     out.print("(__caucho_session, null");
     if (isMap) {
       out.print(", ");
       out.print(getEntityTargetType().getBeanClass().getName());
       out.print(".class.getDeclaredMethod(\"get");
       String getterMapKey = getMapKey();
       getterMapKey = Character.toUpperCase(getterMapKey.charAt(0)) + getterMapKey.substring(1);
       out.print(getterMapKey); // "getId");
       out.print("\")");
     }
     out.println(");");
 
     out.print(var + ".");
 
     if (isMap)
       out.println("putAll(value);");
     else if (isSet)
       out.println("addAll(value);");
     else
       out.println("addAll(0, value);");
 
     out.popDepth();
     out.println("} catch(Exception e) {");
     out.println("  throw com.caucho.amber.AmberRuntimeException.create(e);");
     out.println("}");
 
     out.popDepth();
     out.println("}");
   }
 
   /**
    * Generates code for foreign entity create/delete
    */
   public void generateInvalidateForeign(JavaWriter out)
     throws IOException
   {
     Table table = getLinkColumns().getSourceTable();
 
     out.println("if (\"" + table.getName() + "\".equals(table)) {");
     out.pushDepth();
 
     String var = "_caucho_field_" + getGetterName();
 
     out.println("if (" + var + " != null)");
     out.println("  " + var + ".update();");
     out.popDepth();
     out.println("}");
   }
 
   /**
    * Generates the expire code
    *
    * ejb/06hi
    */
   public void generateExpire(JavaWriter out)
     throws IOException
   {
     String var = "_caucho_field_" + getGetterName();
 
     out.println(var + " = null;");
   }
 }
