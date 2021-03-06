 /*
  * Copyright 2009 by OpenGamma Inc and other contributors.
  * 
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *    http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 */
 
 package org.fudgemsg.proto.java;
 
 import java.io.File;
 import java.io.IOException;
 import java.util.Iterator;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 
 import org.fudgemsg.FudgeTypeDictionary;
 import org.fudgemsg.proto.CodeGeneratorUtil;
 import org.fudgemsg.proto.Compiler;
 import org.fudgemsg.proto.Definition;
 import org.fudgemsg.proto.EnumDefinition;
 import org.fudgemsg.proto.FieldDefinition;
 import org.fudgemsg.proto.FieldType;
 import org.fudgemsg.proto.IndentWriter;
 import org.fudgemsg.proto.MessageDefinition;
 import org.fudgemsg.proto.Binding;
 import org.fudgemsg.proto.TaxonomyDefinition;
 import org.fudgemsg.proto.c.CBlockCode;
 import org.fudgemsg.proto.proto.DocumentedClassCode;
 import org.fudgemsg.proto.proto.HeaderlessClassCode;
 
 /**
  * Code generator for the Java Fudge implementation
  * 
  * @author Andrew
  */
 /* package */ class JavaClassCode extends HeaderlessClassCode {
   
   // TODO 2010-01-07 Andrew -- we don't need the JavaWriter class; temporary local variable names can be tracked using the call stack, and the other methods can be brought back in here
   
   /* package */ static final JavaClassCode INSTANCE = new JavaClassCode ();
   
   private static final String CLASS_COLLECTIONS = java.util.Collections.class.getName ();
   private static final String CLASS_COLLECTION = java.util.Collection.class.getName ();
   private static final String CLASS_FUDGEMSG = org.fudgemsg.FudgeMsg.class.getName ();
   private static final String CLASS_FUDGEFIELDCONTAINER = org.fudgemsg.FudgeFieldContainer.class.getName ();
   private static final String CLASS_MUTABLEFUDGEFIELDCONTAINER = org.fudgemsg.MutableFudgeFieldContainer.class.getName ();
   private static final String CLASS_MAPFUDGETAXONOMY = org.fudgemsg.taxon.MapFudgeTaxonomy.class.getName ();
   private static final String CLASS_ARRAYLIST = java.util.ArrayList.class.getName ();
   private static final String CLASS_FUDGECONTEXT = org.fudgemsg.FudgeContext.class.getName ();
   private static final String CLASS_FUDGETAXONOMY = org.fudgemsg.taxon.FudgeTaxonomy.class.getName ();
   private static final String CLASS_LIST = java.util.List.class.getName ();
   private static final String CLASS_ARRAYS = java.util.Arrays.class.getName ();
   private static final String CLASS_LISTITERATOR = java.util.ListIterator.class.getName ();
   private static final String CLASS_FUDGEFIELD = org.fudgemsg.FudgeField.class.getName ();
   private static final String CLASS_INDICATOR = org.fudgemsg.types.IndicatorType.class.getName ();
   
   private static final String VALUE_INDICATOR = CLASS_INDICATOR + ".INSTANCE";
 
   private JavaClassCode () {
     super (new DocumentedClassCode (blockCodeDelegate (new CBlockCode (literalCodeDelegate (JavaLiteralCode.INSTANCE)))));
   }
   
   private String getBinding (final Definition definition, final String key) {
     final Binding.Data data = definition.getLanguageBinding ("Java").getData (key);
     return (data != null) ? data.getValue () : null;
   }
   
   private JavaWriter beginClass (JavaWriter writer, final Definition definition, final String extendsClass, String interfaceClass) throws IOException {
     if (definition.getOuterDefinition () == null) {
       final String namespace = definition.getNamespace ();
       if (namespace != null) {
         writer.packageDef (namespace);
         endStmt (writer);
       }
     }
     final String imports = getBinding (definition, "imports");
     if (imports != null) {
       for (String library : imports.split (",")) {
         writer.importLib (library.trim ());
         endStmt (writer);
       }
     }
     // TODO 2010-01-13 Andrew -- need to support the Javadoc-style annotations in the proto files and write out javadoc for Java classes 
     final String extraImplements = getBinding (definition, "implements");
     if (extraImplements != null) {
       if (interfaceClass == null) {
         interfaceClass = extraImplements;
       } else {
         interfaceClass = interfaceClass + ", " + extraImplements;
       }
     }
     writer.classDef (definition.getOuterDefinition () != null, definition.getName (), extendsClass, interfaceClass);
     writer = beginBlock (writer); // class definition
     final String bodyCode = getBinding (definition, "body");
     if (bodyCode != null) {
       writer.getWriter ().write (bodyCode);
       writer.getWriter ().newLine ();
     }
     return writer;
   }
 
   @Override
   public void beginClassImplementationDeclaration(final Compiler.Context context, MessageDefinition message, IndentWriter iWriter) throws IOException {
     super.beginClassImplementationDeclaration (context, message, iWriter);
     final MessageDefinition ext = message.getExtends ();
     beginClass (new JavaWriter (iWriter), message, (ext != null) ? ext.getIdentifier () : null, "Cloneable");
   }
 
   @Override
   public void endClassImplementationDeclaration(final Compiler.Context context, MessageDefinition message, IndentWriter iWriter) throws IOException {
     final JavaWriter writer = new JavaWriter (iWriter);
     writeEquals (writer, message);
     writeHashCode (writer, message);
     writeClone (writer, message);
     endBlock (iWriter); // class definition
   }
 
   @Override
   public File getImplementationFile(final Compiler.Context context, Definition definition, File targetPath) throws IOException {
     File implementation = CodeGeneratorUtil.applyNamespace(context, targetPath, definition.getIdentifierArray());
     if (implementation == null) return null;
     return new File(implementation, definition.getName() + ".java");
   }
   
   private String fieldMethodName (final FieldDefinition field, final String prefix, final String suffix) {
     final StringBuilder sb = new StringBuilder ();
     if (prefix != null) {
       sb.append (prefix).append (camelCaseFieldName (field));
     } else {
       sb.append (localFieldName (field));
     }
     if (suffix != null) sb.append (suffix);
     return sb.toString ();
   }
   
   @Override
   public void writeClassImplementationAccessor(final Compiler.Context context, final FieldDefinition field, final IndentWriter writer) throws IOException {
     JavaWriter jWriter = new JavaWriter (writer);
     final String attribute = privateFieldName (field);
     jWriter.method (false, typeString (field.getType (), field.isRepeated ()), fieldMethodName (field, "get", null), null);
     jWriter = beginBlock (jWriter); // accessor
     if (field.isRepeated ()) {
       // repeated fields, return the first
       jWriter.returnInvoke (fieldMethodName (field, "get", null), "0", null);
     } else {
       // non-repeated fields, return attribute directly
       jWriter.returnVariable (attribute);
     }
     endStmt (jWriter); // return
     jWriter = endBlock (jWriter); // accessor
     if (field.isRepeated ()) {
       jWriter.method (false, "int", fieldMethodName (field, "get", "Count"), null);
       jWriter = beginBlock (jWriter); // getXCount
       jWriter.returnIfNull (attribute, attribute + ".size ()", "0");
       endStmt (jWriter); // return
       jWriter = endBlock (jWriter); // getXCount
       jWriter.method (false, typeString (field.getType (), true), fieldMethodName (field, "get", null), "final int n");
       jWriter = beginBlock (jWriter); // getX(n)
       jWriter.ifNull (attribute);
       jWriter = beginBlock (jWriter); // if
       jWriter.returnNullIfZero ("n");
       endStmt (jWriter);
       jWriter.throwIndexOutOfBoundsException ("n");
       endStmt (jWriter);
       jWriter = endBlock (jWriter); // if
       jWriter.returnInvoke (attribute + ".get", "n", null);
       endStmt (jWriter);
       jWriter = endBlock (jWriter); // getX(n)
       jWriter.method (false,  realTypeString (field, false), fieldMethodName (field, "get", "List"), null);
       jWriter = beginBlock (jWriter); // getXList
       jWriter.returnInvoke (CLASS_COLLECTIONS + ".unmodifiableList", attribute, null);
       endStmt (jWriter);
       jWriter = endBlock (jWriter);
     }
     if (field.isMutable ()) {
       writeMutatorMethod (writer, false, field);
     }
   }
   
   private String genericTypeString (final String javaClass, final FieldType type, final boolean concrete) {
     final StringBuilder sb = new StringBuilder (javaClass);
     sb.append ('<');
     if (!concrete) sb.append ("? extends ");
     sb.append (typeString (type, true)).append ('>');
     return sb.toString ();
   }
   
   private String listTypeString (final FieldType type, final boolean concrete) {
     return genericTypeString (concrete ? CLASS_ARRAYLIST : CLASS_LIST, type, true);
   }
   
   private String realTypeString (final FieldDefinition field, final boolean generic) {
     if (field.isRepeated ()) {
       return generic ? genericTypeString (CLASS_COLLECTION, field.getType (), false) : listTypeString (field.getType (), false);
     } else {
       return typeString (field.getType (), false);
     }
   }
   
   @Override
   public void writeClassImplementationAttribute(final Compiler.Context context, final FieldDefinition field, final IndentWriter iWriter) throws IOException {
     final JavaWriter writer = new JavaWriter (iWriter);
     writer.attribute (!field.isMutable (), realTypeString (field, false), privateFieldName (field));
     endStmt (writer); // attribute decl
   }
   
   private void writeBuilderClassFields (final JavaWriter writer, MessageDefinition message) throws IOException {
     for (FieldDefinition field : message.getFieldDefinitions ()) {
       writer.attribute (field.isRequired () && (field.getDefaultValue () == null), realTypeString (field, false), privateFieldName (field));
       endStmt (writer); // builder field decl
     }
   }
   
   private void getSuperConstructorParams (final MessageDefinition message, final StringBuilder typeList, final StringBuilder invokeList) {
     if (message.getExtends () != null) getSuperConstructorParams (message.getExtends (), typeList, invokeList);
     for (FieldDefinition field : message.getFieldDefinitions ()) {
       if (field.isRequired ()) {
         if (typeList.length () != 0) typeList.append (", ");
         typeList.append (realTypeString (field, true)).append (' ').append (localFieldName (field));
         if (invokeList.length () != 0) invokeList.append (", ");
         invokeList.append (localFieldName (field));
       }
     }
   }
   
   private void writePublicConstructorBody (final IndentWriter writer, final List<FieldDefinition> required, final List<FieldDefinition> defaultValues, final String methodPrefix) throws IOException {
     for (FieldDefinition field : required) {
       writeMutatorAssignment (writer, field);
     }
     for (FieldDefinition field : defaultValues) {
       writer.write (fieldMethodName (field, methodPrefix, null) + " (" + getLiteral (field.getDefaultValue ()) + ")");
       endStmt (writer);
     }
   }
   
   /**
    * Writes out the constructor for either Builder or the main message if the builder pattern is not being used.
    */
   private void writePublicConstructor (final IndentWriter writer, final boolean builder, final MessageDefinition message) throws IOException {
     final List<FieldDefinition> defaultValues = new LinkedList<FieldDefinition> ();
     final List<FieldDefinition> required = new LinkedList<FieldDefinition> ();
     final MessageDefinition superMessage = message.getExtends ();
     final StringBuilder sbSuperParamTypeList = new StringBuilder ();
     final StringBuilder sbSuperParamInvokeList = new StringBuilder ();
     if (superMessage != null) {
       getSuperConstructorParams (message.getExtends (), sbSuperParamTypeList, sbSuperParamInvokeList);
     }
     final StringBuilder sbParamTypeList = new StringBuilder ();
     for (FieldDefinition field : message.getFieldDefinitions ()) {
       if (field.getDefaultValue () != null) {
         defaultValues.add (field);
       } else if (field.isRequired ()) {
         required.add (field);
         if (sbParamTypeList.length () > 0) sbParamTypeList.append (", ");
         sbParamTypeList.append (realTypeString (field, true)).append (' ').append (localFieldName (field));
       }
     }
     writer.write ("public " + (builder ? "Builder" : message.getName ()) + " (");
     if (superMessage != null) {
       writer.write (sbSuperParamTypeList.toString ());
       if (sbParamTypeList.length () > 0) writer.write (", ");
     }
     writer.write (sbParamTypeList.toString () + ")");
     beginBlock (writer); // constructor
     if (superMessage != null) {
       writer.write ("super (" + sbSuperParamInvokeList.toString () + ")");
       endStmt (writer);
     }
     writePublicConstructorBody (writer, required, defaultValues, builder ? null : "set");
     endBlock (writer); // constructor
     if (builder) {
       if (superMessage != null) {
         writer.write ("protected Builder (final " + superMessage.getIdentifier () + ".Builder fudgeParent");
         if (sbParamTypeList.length () > 0) writer.write (", " + sbParamTypeList.toString ());
         writer.write (')');
         beginBlock (writer); // constructor
         writer.write ("super (fudgeParent)");
         endStmt (writer);
         writePublicConstructorBody (writer, required, defaultValues, null);
         endBlock (writer); // constructor
       }
       writer.write ("protected Builder (final Builder delegate)");
       beginBlock (writer); // constructor
       if (superMessage != null) {
         writer.write ("super (delegate)");
         endStmt (writer);
       }
       for (FieldDefinition field : message.getFieldDefinitions ()) {
         final String pfn = privateFieldName (field);
         writer.write (pfn + " = delegate." + pfn);
         endStmt (writer);
       }
       endBlock (writer); // constructor
     }
   }
   
   private void writeCheckArrayLength (final IndentWriter writer, final String variable, final String displayVariable, final FieldType.ArrayType array, final int lvCount) throws IOException {
     if (array.isFixedLength ()) {
       writer.write ("if (" + variable + ".length != " + array.getFixedLength () + ") throw new IllegalArgumentException (\"'" + displayVariable + "' is not the expected length (" + array.getFixedLength () + ")\")");
       endStmt (writer);
     }
     if (array.isDeepFixedLength ()) {
       final String lv = "fudge" + lvCount;
       writer.write ("for (" + typeString (array.getBaseType (), false) + " " + lv + " : " + variable + ")");
       beginBlock (writer);
       writeCheckArrayLength (writer, lv, displayVariable + "[]", (FieldType.ArrayType)array.getBaseType (), lvCount + 1);
       endBlock (writer);
     }
   }
   
   private String writeDefensiveCopy (final IndentWriter writer, final FieldType type, final String source, final String displayName, int lvCount) throws IOException {
     if (type instanceof FieldType.ArrayType) {
       final FieldType.ArrayType array = (FieldType.ArrayType)type;
       final FieldType baseType = array.getBaseType ();
       writer.write (source + " = " + CLASS_ARRAYS + ".copyOf (" + source + ", " + source + ".length)");
       endStmt (writer);
       if (!(baseType instanceof FieldType.EnumType) && (baseType != FieldType.STRING_TYPE) && isObject (baseType)) {
         final String lv = "fudge" + lvCount;
         writer.write ("for (int " + lv + " = 0; " + lv + " < " + source + ".length; " + lv + "++)");
         beginBlock (writer); // for
         writer.write (source + "[" + lv + "] = " + writeDefensiveCopy (writer, baseType, source + "[" + lv + "]", displayName + "[]", lvCount + 1));
         endStmt (writer);
         endBlock (writer); // for
       }
       writeCheckArrayLength (writer, source, displayName, array, lvCount);
       return source;
     } else if (type instanceof FieldType.MessageType) {
       return source + ".clone ()";
     } else {
       return source;
     }
   }
   
   private void writeMutatorAssignment (final IndentWriter writer, final FieldDefinition field) throws IOException {
     final String value = localFieldName (field);
     if (field.isRepeated ()) {
       writer.write ("if (" + value + " == null) ");
       if (field.isRequired ()) {
         writer.write ("throw new NullPointerException (\"'" + value + "' cannot be null\")");
       } else {
         writer.write (privateFieldName (field) + " = null");
       }
       endStmt (writer);
       writer.write ("else");
       beginBlock (writer); // elseif
       writer.write ("final " + listTypeString (field.getType (), false) + " fudge0 = new " + listTypeString (field.getType (), true) + " (" + value + ")");
       endStmt (writer);
       if (field.isRequired ()) {
         writer.write ("if (" + value + ".size () == 0) throw new IllegalArgumentException (\"'" + value + "' cannot be an empty list\")");
         endStmt (writer);
       }
       final String type = typeString (field.getType (), true);
       writer.write ("for (" + CLASS_LISTITERATOR + "<" + type + "> fudge1 = fudge0.listIterator (); fudge1.hasNext (); )");
       beginBlock (writer); // for
       writer.write (type + " fudge2 = fudge1.next ()");
       endStmt (writer);
       writer.write ("if (fudge2 == null) throw new NullPointerException (\"List element of '" + value + "' cannot be null\")");
       endStmt (writer);
       if (isObject (field.getType ())) {
         writer.write ("fudge1.set (" + writeDefensiveCopy (writer, field.getType (), "fudge2", value + "[]", 3) + ")");
         endStmt (writer);
       }
       endBlock (writer); // for
       writer.write (privateFieldName (field) + " = fudge0");
       endStmt (writer);
       endBlock (writer); // elseif
     } else {
       if (isObject (field.getType ()) && field.isRequired ()) {
         writer.write ("if (" + value + " == null) throw new NullPointerException (\"'" + value + "' cannot be null\")");
         endStmt (writer);
       }
       writer.write (privateFieldName (field) + " = " + writeDefensiveCopy (writer, field.getType (), value, value, 0));
       endStmt (writer);
     }
   }
   
   private void writeMutatorMethod (final IndentWriter writer, final boolean builderReturn, final FieldDefinition field) throws IOException {
     // standard method (or singleton list on repeated fields)
     final String lfn = localFieldName (field);
     final String pfn = privateFieldName (field);
     final String returnType = "public " + (builderReturn ? "Builder" : "void") + " ";
     writer.write (returnType + fieldMethodName (field, builderReturn ? null : "set", null) + " (" + typeString (field.getType (), field.isRepeated ()) + " " + lfn + ")");
     beginBlock (writer); // method
     if (field.isRepeated ()) {
       writer.write ("if (" + lfn + " == null) ");
       if (field.isRequired ()) {
         writer.write ("throw new NullPointerException (\"'" + lfn + "' cannot be null\")"); 
       } else {
         writer.write (pfn + " = null");
       }
       endStmt (writer); // null assignment or exception
       writer.write ("else");
       beginBlock (writer); // else
       writer.write (pfn + " = new " + listTypeString (field.getType (), true) + " (1)");
       endStmt (writer); // reset list
       writer.write (fieldMethodName (field, "add", null) + " (" + lfn + ")");
       endStmt (writer); // invoke add
       endBlock (writer);
     } else {
       writeMutatorAssignment (writer, field);
     }
     if (builderReturn) {
       writer.write ("return this");
       endStmt (writer); // return this
     }
     endBlock (writer); // method
     if (field.isRepeated ()) {
       // standard method to assign a whole list on repeated fields
       writer.write (returnType + fieldMethodName (field, builderReturn ? null : "set", null) + " (" + genericTypeString (CLASS_COLLECTION, field.getType (), false) + " " + lfn + ")");
       beginBlock (writer); // method
       writeMutatorAssignment (writer, field);
       if (builderReturn) {
         writer.write ("return this");
         endStmt (writer); // return this
       }
       endBlock (writer); // method
       // standard method to append an item to a repeated field list
       writer.write (returnType + fieldMethodName (field, "add", null) + " (" + typeString (field.getType (), true) + " " + lfn + ")");
       beginBlock (writer); // method
       writer.write ("if (" + lfn + " == null) throw new NullPointerException (\"'" + lfn + "' cannot be null\")");
       endStmt (writer); // check for null
       writer.write ("if (" + pfn + " == null) " + pfn + " = new " + listTypeString (field.getType (), true) + " ()");
       endStmt (writer); // assign empty list if none already
       writer.write (pfn + ".add (" + writeDefensiveCopy (writer, field.getType (), lfn, lfn, 0) + ")");
       endStmt (writer); // append
       if (builderReturn) {
         writer.write ("return this");
         endStmt (writer); // return this
       }
       endBlock (writer); // method
     }
   }
   
   private void writeBuilderClassMethods (final IndentWriter writer, final MessageDefinition message) throws IOException {
     for (FieldDefinition field : message.getFieldDefinitions ()) {
       if (!field.isRequired () || (field.getDefaultValue () != null)) {
         writeMutatorMethod (writer, true, field);
       }
     }
   }
   
   private void writeBuilderClassBuildMethod (JavaWriter writer, MessageDefinition message) throws IOException {
     writer.method (false, message.getName (), "build", null);
     writer = beginBlock (writer);
     writer.returnConstruct (message.getName (), "this");
     endStmt (writer);
     writer = endBlock (writer);
   }
   
   private void writeBuilderClass (final IndentWriter writer, MessageDefinition message) throws IOException {
     final MessageDefinition ext = message.getExtends ();
     JavaWriter jWriter = new JavaWriter (writer);
     jWriter.classDef (true, "Builder", (ext != null) ? ext.getIdentifier () + ".Builder" : null, null);
     jWriter = beginBlock (jWriter); // builder class
     writeBuilderClassFields (jWriter, message);
     writePublicConstructor (writer, true, message);
     writeBuilderClassMethods (writer, message);
     writeBuilderClassBuildMethod (jWriter, message);
     jWriter = endBlock (jWriter); // builder class
   }
   
   private void writeProtectedBuilderConstructor (JavaWriter writer, final MessageDefinition message) throws IOException {
     writer.constructor ("protected", message.getName (), "final Builder builder");
     writer = beginBlock (writer); // constructor
     if (message.getExtends () != null) {
       writer.invoke ("super", "builder");
       endStmt (writer);
     }
     for (FieldDefinition field : message.getFieldDefinitions ()) {
       writer.assignment ("this." + privateFieldName (field), "builder." + privateFieldName (field));
       endStmt (writer); // assignment
     }
     writer = endBlock (writer); // constructor
   }
   
   private JavaWriter beginBlock (final JavaWriter writer) throws IOException {
     beginBlock (writer.getWriter ());
     return new JavaWriter (writer);
   }
   
   private JavaWriter endBlock (final JavaWriter writer) throws IOException {
     final JavaWriter parent = writer.getParent ();
     endBlock (parent.getWriter ());
     return parent;
   }
   
   private void endStmt (final JavaWriter writer) throws IOException {
     endStmt (writer.getWriter ());
   }
 
   private void writeAddToFudgeMsg (JavaWriter writer, final String msg, final String name, final String ordinal, String value, final FieldType type) throws IOException {
     // special value substitutions for some types
     switch (type.getFudgeFieldType ()) {
     case FudgeTypeDictionary.INT_ARRAY_TYPE_ID :
       if (((FieldType.ArrayType)type).getBaseType () instanceof FieldType.EnumType) {
         final String temp1 = writer.localVariable ("int[]", true, "new int[" + value + ".length]");
         endStmt (writer);
         final String temp2 = writer.forEachIndex (temp1, "length");
         writer = beginBlock (writer);
         writer.assignment (temp1 + "[" + temp2 + "]", value + "[" + temp2 + "].getFudgeEncoding ()");
         endStmt (writer);
         writer = endBlock (writer);
         value = temp1;
       }
       break;
     case FudgeTypeDictionary.INDICATOR_TYPE_ID :
       writer.ifBool (value); // indicators are present if the bool we are using is set
       value = VALUE_INDICATOR;
       break;
     case FudgeTypeDictionary.INT_TYPE_ID :
       if (type instanceof FieldType.EnumType) {
         value = value + ".getFudgeEncoding ()";
       }
       break;
     case FudgeTypeDictionary.FUDGE_MSG_TYPE_ID :
       if (type instanceof FieldType.ArrayType) {
         final String temp1 = writer.localVariable (CLASS_FUDGEMSG, true, "new " + CLASS_FUDGEMSG + " (context)");
         endStmt (writer);
         final FieldType baseType = ((FieldType.ArrayType)type).getBaseType ();
         final String temp2 = writer.forEach (typeString (baseType, false), value);
         writer = beginBlock (writer);
         writeAddToFudgeMsg (writer, temp1, "null", "null", temp2, baseType);
         writer = endBlock (writer);
         value = temp1;
       } else if (type instanceof FieldType.MessageType) {
         value = value + ".toFudgeMsg (context)";
       } else {
         throw new IllegalStateException ("type '" + type + "' is not an expected submessage type");
       }
       break;
     }
     writer.invoke (msg, "add", name + ", " + ordinal + ", " + value);
     endStmt (writer);
   }
   
   private void writeToFudgeMsg (JavaWriter writer, final MessageDefinition message) throws IOException {
     writer.method (false, CLASS_FUDGEMSG, "toFudgeMsg", "final " + CLASS_FUDGECONTEXT + " context");
     writer = beginBlock (writer); // toFudgeMsg
     writer.ifNull ("context");
     writer.throwNullParameterException ("context");
     endStmt (writer);
     writer.namedLocalVariable (CLASS_FUDGEMSG, "msg", "context.newMessage ()");
     endStmt (writer);
     writer.invoke ("toFudgeMsg", "context, msg");
     endStmt (writer);
     writer.returnVariable ("msg");
     endStmt (writer);
     writer = endBlock (writer); // toFudgeMsg
     writer.method (false, "void", "toFudgeMsg", "final " + CLASS_FUDGECONTEXT + " context, final " + CLASS_MUTABLEFUDGEFIELDCONTAINER + " msg");
     writer = beginBlock (writer); // toFudgeMsg
     if (message.getExtends () != null) {
       writer.invoke ("super", "toFudgeMsg", "context, msg");
       endStmt (writer);
     }
     for (FieldDefinition field : message.getFieldDefinitions ()) {
       final FieldType type = field.getType ();
       String value = privateFieldName (field);
       if (field.isRepeated ()) {
         writer.ifNotNull (value);
         writer = beginBlock (writer); // if not null
         value = writer.forEach (typeString (type, true), value);
         writer = beginBlock (writer); // foreach
       } else {
         if (isObject (type)) {
           writer.ifNotNull (value);
           writer = beginBlock (writer); // if not null
         }
       }
       writeAddToFudgeMsg (writer, "msg", (field.getName () != null) ? "\"" + field.getName () + "\"" : "null", (field.getOrdinal () != null) ? field.getOrdinal ().toString () : "null", value, type);
       if (field.isRepeated ()) {
         writer = endBlock (writer); // foreach
         writer = endBlock (writer); // if not null
       } else {
         if (isObject (type)) {
           writer = endBlock (writer); // if not null
         }
       }
     }
     writer = endBlock (writer); // toFudgeMsg
   }
   
   /**
    * Returns true if the type is an array of Java objects rather than a Java primitive types. I.e. will generic collection methods work
    */
   private boolean isObjectArray (final FieldType.ArrayType type) {
     final FieldType base = type.getBaseType ();
     return isObject (base);
   }
   
   /**
    * Returns true if the type is not a Java primitive.
    */
   private boolean isObject (final FieldType type) {
     if (type instanceof FieldType.ArrayType) {
       return true;
     } else if (type instanceof FieldType.EnumType) {
       return true;
     } else if (type instanceof FieldType.MessageType) {
       return true;
     } else {
       switch (type.getFudgeFieldType ()) {
       case FudgeTypeDictionary.INDICATOR_TYPE_ID :
       case FudgeTypeDictionary.BOOLEAN_TYPE_ID :
       case FudgeTypeDictionary.BYTE_TYPE_ID :
       case FudgeTypeDictionary.SHORT_TYPE_ID :
       case FudgeTypeDictionary.INT_TYPE_ID :
       case FudgeTypeDictionary.LONG_TYPE_ID :
       case FudgeTypeDictionary.FLOAT_TYPE_ID :
       case FudgeTypeDictionary.DOUBLE_TYPE_ID :
         return false;
       case FudgeTypeDictionary.STRING_TYPE_ID :
         return true;
       default :
         throw new IllegalStateException ("type '" + type + "' is not an expected type (fudge field type " + type.getFudgeFieldType () + ")");
       }
     }
   }
   
   private String toArray (JavaWriter writer, final String source, final FieldType.ArrayType type) throws IOException {
     final StringBuilder sbNewArray = new StringBuilder ("new ");
     int dims = 0;
     FieldType base = type.getBaseType ();
     while (base instanceof FieldType.ArrayType) {
       base = ((FieldType.ArrayType)base).getBaseType ();
       dims++;
     }
     sbNewArray.append (typeString (base, false)).append ('[').append (source).append (".size ()]");
     for (int i = 0; i < dims; i++) {
       sbNewArray.append ("[]");
     }
     if (isObjectArray (type)) {
       return source + ".toArray (" + sbNewArray + ")";
     } else {
       final String newArray = writer.localVariable (typeString (type, false), true, sbNewArray.toString ());
       endStmt (writer);
       final String index = writer.localVariable ("int", false, "0");
       endStmt (writer);
       final String element = writer.forEach (typeString (type.getBaseType (), false), source);
       writer = beginBlock (writer);
       writer.assignment (newArray + "[" + index + "++]", element);
       endStmt (writer);
       writer = endBlock (writer);
       return newArray;
     }
   }
   
   private String writeDecodeSimpleFudgeField (final JavaWriter writer, final String displayType, final String javaType, final MessageDefinition message, final String fieldData, final String fieldRef, final String assignTo, final String appendTo) throws IOException {
     writer.anonGetValue (fieldData);
     endStmt (writer);
     writer.anonIfNotInstanceOf (javaType);
     writer.throwInvalidFudgeFieldException (message, fieldRef, displayType, null);
     endStmt (writer);
     if (appendTo != null) {
       return "(" + javaType + ")fudge0";
     } else {
       writer.anonAssignment (assignTo, javaType);
       endStmt (writer);
       return assignTo;
     }
   }
   
   private void writeDecodeFudgeField (JavaWriter writer, final FieldType type, final MessageDefinition message, final String fieldData, final String fieldRef, String assignTo, final String appendTo) throws IOException {
     // TODO 2010-01-04 Andrew -- should we support intrinsic conversion from shorter to longer types, e.g. short[] to int[]? like we do for the single values
     if (type instanceof FieldType.ArrayType) {
       final FieldType.ArrayType arrayType = (FieldType.ArrayType)type;
       final FieldType baseType = arrayType.getBaseType ();
       boolean checkLength = arrayType.isFixedLength ();
       switch (type.getFudgeFieldType ()) {
       case FudgeTypeDictionary.BYTE_ARRAY_TYPE_ID :
       case FudgeTypeDictionary.BYTE_ARR_4_TYPE_ID :
       case FudgeTypeDictionary.BYTE_ARR_8_TYPE_ID :
       case FudgeTypeDictionary.BYTE_ARR_16_TYPE_ID :
       case FudgeTypeDictionary.BYTE_ARR_20_TYPE_ID :
       case FudgeTypeDictionary.BYTE_ARR_32_TYPE_ID :
       case FudgeTypeDictionary.BYTE_ARR_64_TYPE_ID :
       case FudgeTypeDictionary.BYTE_ARR_128_TYPE_ID :
       case FudgeTypeDictionary.BYTE_ARR_256_TYPE_ID :
       case FudgeTypeDictionary.BYTE_ARR_512_TYPE_ID :
         if (appendTo != null) {
           assignTo = writer.localVariable ("byte[]", true);
           endStmt (writer);
         }
         writer.anonGetValue (fieldData);
         endStmt (writer);
         writer.anonIfInstanceOf ("byte[]");
         writer.anonAssignment (assignTo, "byte[]");
         endStmt (writer);
         writer.elseThrowInvalidFudgeFieldException (message, fieldRef, "byte[]", null);
         endStmt (writer);
         break;
       case FudgeTypeDictionary.SHORT_ARRAY_TYPE_ID :
         assignTo = writeDecodeSimpleFudgeField (writer, "short[]", "short[]", message, fieldData, fieldRef, assignTo, appendTo);
         break;
       case FudgeTypeDictionary.INT_ARRAY_TYPE_ID : {
         if (baseType instanceof FieldType.EnumType) {
           final EnumDefinition enumDefinition = ((FieldType.EnumType)baseType).getEnumDefinition ();
           writer.anonGetValue (fieldData);
           endStmt (writer);
           writer.anonIfNotInstanceOf ("int[]");
           writer.throwInvalidFudgeFieldException (message, fieldRef, type.toString (), null);
           endStmt (writer);
           if (appendTo != null) {
             assignTo = writer.localVariable (typeString (type, false), true);
             endStmt (writer);
           }
           writer.guard ();
           writer = beginBlock (writer); // try
           final String intArray = writer.localVariable ("int[]", true, "(int[])fudge0");
           endStmt (writer);
           writer.assignment (assignTo, "new " + enumDefinition.getIdentifier () + "[" + intArray + ".length]");
           endStmt (writer);
           final String index = writer.forEachIndex (intArray, "length");
           writer = beginBlock (writer); // for
           writer.assignment (assignTo + "[" + index + "]", enumDefinition.getIdentifier () + ".fromFudgeEncoding (" + intArray + "[" + index + "])");
           endStmt (writer);
           writer = endBlock (writer); // for
           writer = endBlock (writer); // try
           writer.catchIllegalArgumentException ();
           writer = beginBlock (writer); // catch
           writer.throwInvalidFudgeFieldException (message, fieldRef, type.toString (), "e");
           endStmt (writer);
           writer = endBlock (writer); // catch
         } else {
           assignTo = writeDecodeSimpleFudgeField (writer, "int[]", "int[]", message, fieldData, fieldRef, assignTo, appendTo);
         }
         break;
       }
       case FudgeTypeDictionary.LONG_ARRAY_TYPE_ID :
         assignTo = writeDecodeSimpleFudgeField (writer, "long[]", "long[]", message, fieldData, fieldRef, assignTo, appendTo);
         break;
       case FudgeTypeDictionary.FLOAT_ARRAY_TYPE_ID :
         assignTo = writeDecodeSimpleFudgeField (writer, "float[]", "float[]", message, fieldData, fieldRef, assignTo, appendTo);
         break;
       case FudgeTypeDictionary.DOUBLE_ARRAY_TYPE_ID :
         assignTo = writeDecodeSimpleFudgeField (writer, "double[]", "double[]", message, fieldData, fieldRef, assignTo, appendTo);
         break;
       case FudgeTypeDictionary.FUDGE_MSG_TYPE_ID :
         // arbitrary array
         writer.anonGetValue (fieldData);
         endStmt (writer);
         writer.anonIfNotInstanceOf (CLASS_FUDGEMSG);
         writer.throwInvalidFudgeFieldException (message, fieldRef, "sub message (array)", null);
         endStmt (writer);
         final String subMessage = writer.localVariable (CLASS_FUDGEFIELDCONTAINER, true, "(" + CLASS_FUDGEFIELDCONTAINER + ")fudge0");
         endStmt (writer);
         if (appendTo != null) {
           // TODO 2010-01-06 Andrew -- we could call getNumFields on the subMessage and allocate a proper array once, but might that be slower if we have a FudgeMsg implementation that makes data available as soon as its received & decoded - i.e. a big array submessage would have to be decoded in its entirety to get the length
           assignTo = writer.localVariable (listTypeString (baseType, false), true, "new " + listTypeString (baseType, true) + " ()");
           endStmt (writer);
         }
         writer.getWriter ().write ("try");
         writer = beginBlock (writer); // try
         final String msgElement = writer.forEach (CLASS_FUDGEFIELD, subMessage);
         writer = beginBlock (writer); // iteration
         writeDecodeFudgeField (writer, baseType, message, msgElement, fieldRef + "[]", null, assignTo + ".add");
         writer = endBlock (writer); // iteration
         writer = endBlock (writer); // try
         writer.getWriter ().write ("catch (IllegalArgumentException e)");
         writer = beginBlock (writer); // catch
         writer.throwInvalidFudgeFieldException (message, fieldRef, type.toString (), "e");
         endStmt (writer);
         writer = endBlock (writer); // catch
         if (appendTo != null) {
           if (checkLength) {
             writer.ifSizeNot (assignTo, "size ()", arrayType.getFixedLength ());
             writer.throwInvalidFudgeFieldException (message, fieldRef, type.toString (), null);
             endStmt (writer);
             checkLength = false;
           }
           assignTo = toArray (writer, assignTo, (FieldType.ArrayType)type);
         }
         break;
       default :
         throw new IllegalStateException ("type '" + type + "' is not an expected type (fudge field type " + type.getFudgeFieldType () + ")");
       }
       // check the array lengths if the spec required a specific length
       if (checkLength) {
         writer.ifSizeNot (assignTo, "length", arrayType.getFixedLength ());
         writer.throwInvalidFudgeFieldException (message, fieldRef, type.toString (), null);
         endStmt (writer);
       }
     } else if (type instanceof FieldType.MessageType) {
       final MessageDefinition msg = ((FieldType.MessageType)type).getMessageDefinition ();
       if (appendTo != null) {
         assignTo = writer.localVariable (msg.getIdentifier (), true);
         endStmt (writer);
       }
       writer.anonGetValue (fieldData);
       endStmt (writer);
       writer.anonIfNotInstanceOf (CLASS_FUDGEFIELDCONTAINER);
       writer.throwInvalidFudgeFieldException (message, fieldRef, msg.getName (), null);
       endStmt (writer);
       writer.guard ();
       writer = beginBlock (writer); // try
       writer.assignment (assignTo, msg.getIdentifier () + ".fromFudgeMsg ((" + CLASS_FUDGEFIELDCONTAINER + ")fudge0)");
       endStmt (writer);
       writer = endBlock (writer); // try
       writer.catchIllegalArgumentException ();
       writer = beginBlock (writer); // catch
       writer.throwInvalidFudgeFieldException (message, fieldRef, msg.getName (), "e");
       endStmt (writer);
       writer = endBlock (writer); // catch
     } else if (type instanceof FieldType.EnumType) {
       final EnumDefinition enumDefinition = ((FieldType.EnumType)type).getEnumDefinition ();
       final String intValue = writer.localVariable ("int", true);
       endStmt (writer);
       if (appendTo != null) {
         assignTo = writer.localVariable (enumDefinition.getIdentifier (), true);
         endStmt (writer);
       }
       writer.anonGetValue (fieldData);
       endStmt (writer);
       writer.anonIfInstanceOf ("Integer");
       writer.anonAssignment (intValue, "Integer");
       endStmt (writer);
       writer.anonElseIfInstanceOf ("Short");
       writer.anonAssignment (intValue, "Short");
       endStmt (writer);
       writer.anonElseIfInstanceOf ("Byte");
       writer.anonAssignment (intValue, "Byte");
       endStmt (writer);
       writer.elseThrowInvalidFudgeFieldException (message, fieldRef, enumDefinition.getName (), null);
       endStmt (writer);
       writer.guard ();
       writer = beginBlock (writer); // try
       writer.assignment (assignTo, enumDefinition.getIdentifier () + ".fromFudgeEncoding (" + intValue + ")");
       endStmt (writer);
       writer = endBlock (writer); // try
       writer.catchIllegalArgumentException ();
       writer = beginBlock (writer); // catch
       writer.throwInvalidFudgeFieldException (message, fieldRef, enumDefinition.getName (), "e");
       endStmt (writer);
       writer = endBlock (writer); // catch
     } else {
       switch (type.getFudgeFieldType ()) {
       case FudgeTypeDictionary.INDICATOR_TYPE_ID :
         writer.anonGetValue (fieldData);
         endStmt (writer);
         writer.anonIfNotInstanceOf (CLASS_INDICATOR);
         writer.throwInvalidFudgeFieldException (message, fieldRef, "indicator", null);
         endStmt (writer);
         // using a boolean internally, so just set to true to indicate this is in the message
         if (appendTo != null) {
           assignTo = "true";
         } else {
           writer.assignment (assignTo, "true");
           endStmt (writer);
         }
         break;
       case FudgeTypeDictionary.BOOLEAN_TYPE_ID : {
         assignTo = writeDecodeSimpleFudgeField (writer, "boolean", "Boolean", message, fieldData, fieldRef, assignTo, appendTo);
         break;
       }
       case FudgeTypeDictionary.BYTE_TYPE_ID : {
         assignTo = writeDecodeSimpleFudgeField (writer, "byte", "Byte", message, fieldData, fieldRef, assignTo, appendTo);
         break;
       }
       case FudgeTypeDictionary.SHORT_TYPE_ID :
         if (appendTo != null) {
           assignTo = writer.localVariable ("short", true);
           endStmt (writer);
         }
         writer.anonGetValue (fieldData);
         endStmt (writer);
         writer.anonIfInstanceOf ("Short");
         writer.anonAssignment (assignTo, "Short");
         endStmt (writer);
         writer.anonElseIfInstanceOf ("Byte");
         writer.anonAssignment (assignTo, "Byte");
         endStmt (writer);
         writer.elseThrowInvalidFudgeFieldException (message, fieldRef, "short", null);
         endStmt (writer);
         break;
       case FudgeTypeDictionary.INT_TYPE_ID :
         if (appendTo != null) {
           assignTo = writer.localVariable ("int", true);
           endStmt (writer);
         }
         writer.anonGetValue (fieldData);
         endStmt (writer);
         writer.anonIfInstanceOf ("Integer");
         writer.anonAssignment (assignTo, "Integer");
         endStmt (writer);
         writer.anonElseIfInstanceOf ("Short");
         writer.anonAssignment (assignTo, "Short");
         endStmt (writer);
         writer.anonElseIfInstanceOf ("Byte");
         writer.anonAssignment (assignTo, "Byte");
         endStmt (writer);
         writer.elseThrowInvalidFudgeFieldException (message, fieldRef, "int", null);
         endStmt (writer);
         break;
       case FudgeTypeDictionary.LONG_TYPE_ID :
         if (appendTo != null) {
           assignTo = writer.localVariable ("long", true);
           endStmt (writer);
         }
         writer.anonGetValue (fieldData);
         endStmt (writer);
         writer.anonIfInstanceOf ("Long");
         writer.anonAssignment (assignTo, "Long");
         endStmt (writer);
         writer.anonElseIfInstanceOf ("Integer");
         writer.anonAssignment (assignTo, "Integer");
         endStmt (writer);
         writer.anonElseIfInstanceOf ("Short");
         writer.anonAssignment (assignTo, "Short");
         endStmt (writer);
         writer.anonElseIfInstanceOf ("Byte");
         writer.anonAssignment (assignTo, "Byte");
         endStmt (writer);
         writer.elseThrowInvalidFudgeFieldException (message, fieldRef, "long", null);
         endStmt (writer);
         break;
       case FudgeTypeDictionary.FLOAT_TYPE_ID : {
         assignTo = writeDecodeSimpleFudgeField (writer, "float", "Float", message, fieldData, fieldRef, assignTo, appendTo);
         break;
       }
       case FudgeTypeDictionary.DOUBLE_TYPE_ID :
         if (appendTo != null) {
           assignTo = writer.localVariable ("double", true);
           endStmt (writer);
         }
         writer.anonGetValue (fieldData);
         endStmt (writer);
         writer.anonIfInstanceOf ("Double");
         writer.anonAssignment (assignTo, "Double");
         endStmt (writer);
         writer.anonElseIfInstanceOf ("Float");
         writer.anonAssignment (assignTo, "Float");
         endStmt (writer);
         writer.elseThrowInvalidFudgeFieldException (message, fieldRef, "double", null);
         endStmt (writer);
         break;
       case FudgeTypeDictionary.STRING_TYPE_ID : {
         final String value = fieldData + ".getValue ().toString ()";
         if (appendTo != null) {
           assignTo = value;
         } else {
           writer.assignment (assignTo, value);
           endStmt (writer);
         }
         break;
       }
       default :
         throw new IllegalStateException ("type '" + type + "' is not an expected type (fudge field type " + type.getFudgeFieldType () + ")");
       }
     }
     if (appendTo != null) {
       writer.invoke (appendTo, assignTo);
       endStmt (writer);
     }
   }
   
   private void writeDecodeFudgeFieldsToList (JavaWriter writer, final FieldDefinition field, final String localName) throws IOException {
     writer.assignmentConstruct (localName, listTypeString (field.getType (), true), "fudgeFields.size ()");
     endStmt (writer); // list construction
     final String fieldData = writer.forEach (CLASS_FUDGEFIELD, "fudgeFields");
     beginBlock (writer.getWriter ()); // iteration
     writeDecodeFudgeField (writer, field.getType (), field.getOuterMessage (), fieldData, field.getName (), null, localName + ".add");
     endBlock (writer.getWriter ()); // iteration
   }
   
   private void writeGetFudgeFields (final IndentWriter writer, final List<FieldDefinition> fields, final boolean useAssignment, final boolean useBuilder) throws IOException {
     JavaWriter jWriter = new JavaWriter (writer);
     for (FieldDefinition field : fields) {
       final StringBuilder sbGetField = new StringBuilder ("fudgeMsg.get");
       if (field.isRepeated ()) sbGetField.append ("All");
       sbGetField.append ("By");
       final Integer ordinal = field.getOrdinal ();
       if (ordinal != null) {
         sbGetField.append ("Ordinal (").append (ordinal.toString ()).append (')');
       } else {
         sbGetField.append ("Name (\"").append (field.getName ()).append ("\")");
       }
       jWriter.assignment (field.isRepeated () ? "fudgeFields" : "fudgeField", sbGetField.toString ());
       endStmt (jWriter); // field(s) assignment
       if (useAssignment) {
         if (field.isRepeated ()) {
           jWriter.ifZero ("fudgeFields.size ()");
         } else {
           jWriter.ifNull ("fudgeField");
         }
         jWriter.throwInvalidFudgeFieldException (field.getOuterMessage (), field.getName (), "present", null);
         endStmt (jWriter); // if & throw
         final String dest;
         if (useBuilder) {
           writer.write ("final " + realTypeString (field, false) + " " + (dest = localFieldName (field)));
         } else {
           dest = privateFieldName (field);
         }
         endStmt (writer);
         if (field.isRepeated ()) {
           writeDecodeFudgeFieldsToList (jWriter, field, dest);
         } else {
           writeDecodeFudgeField (jWriter, field.getType (), field.getOuterMessage (), "fudgeField", field.getName (), dest, null);
         }
       } else {
         final String dest;
         if (useBuilder) {
           dest = "fudgeBuilder." + fieldMethodName (field, null, null);
         } else {
           dest = fieldMethodName (field, "set", null);
         }
         if (field.isRepeated ()) {
           jWriter.ifGtZero ("fudgeFields.size ()");
           jWriter = beginBlock (jWriter); // if guard
           final String tempList = jWriter.localVariable (listTypeString (field.getType (), false), true);
           endStmt (jWriter); // temp variable
           writeDecodeFudgeFieldsToList (jWriter, field, tempList);
           writer.write (dest + " (" + tempList + ")");
           endStmt (writer); // add to builder or object
         } else {
           jWriter.ifNotNull ("fudgeField");
           jWriter = beginBlock (jWriter); // if guard
           writeDecodeFudgeField (jWriter, field.getType (), field.getOuterMessage (), "fudgeField", field.getName (), null, dest);
         }
         jWriter = endBlock (jWriter); // if guard
       }
     }
   }
   
   private void writeProtectedFudgeMsgConstructor (final IndentWriter writer, final MessageDefinition message) throws IOException {
     writer.write ("protected " + message.getName () + " (final " + CLASS_FUDGEFIELDCONTAINER + " fudgeMsg)");
     beginBlock (writer); // constructor
     if (message.getExtends () != null) {
       writer.write ("super (fudgeMsg)");
       endStmt (writer);
     }
     writeDecodeFudgeMsg (writer, message, false);
     endBlock (writer); // constructor
   }
   
   private void writeBuilderFromFudgeMsg (final IndentWriter writer, final MessageDefinition message) throws IOException {
     writer.write ("protected static Builder builderFromFudgeMsg (final " + CLASS_FUDGEFIELDCONTAINER + " fudgeMsg)");
     beginBlock (writer); // builderFromFudgeMsg
     if (message.getExtends () != null) {
       final String superClass = message.getExtends ().getIdentifier ();
       writer.write ("final " + superClass + ".Builder fudgeSuperBuilder = " + superClass + ".builderFromFudgeMsg (fudgeMsg)");
       endStmt (writer);
     }
     writeDecodeFudgeMsg (writer, message, true);
     writer.write ("return fudgeBuilder");
     endStmt (writer);
     endBlock (writer); // builderFromFudgeMsg
   }
   
   private void writeDecodeFudgeMsg (final IndentWriter writer, final MessageDefinition message, final boolean useBuilder) throws IOException {
     final List<FieldDefinition> required = new LinkedList<FieldDefinition> ();
     final List<FieldDefinition> optional = new LinkedList<FieldDefinition> ();
    boolean fieldDeclared = false, fieldsDeclared = false;
     for (FieldDefinition field : message.getFieldDefinitions ()) {
       if (field.isRequired () && (field.getDefaultValue () == null)) {
         required.add (field);
       } else {
         optional.add (field);
       }
       if (field.isRepeated ()) {
         if (!fieldsDeclared) {
           writer.write (CLASS_LIST + "<" + CLASS_FUDGEFIELD + ">" + " fudgeFields");
           endStmt (writer);
           fieldsDeclared = true;
         }
       } else {
         if (!fieldDeclared) {
           writer.write (CLASS_FUDGEFIELD + " fudgeField");
           endStmt (writer);
           fieldDeclared = true;
         }
       }
     }
     writeGetFudgeFields (writer, required, true, useBuilder);
     if (useBuilder) {
       writer.write ("final Builder fudgeBuilder = new Builder (");
       boolean first = true;
       if (message.getExtends () != null) {
         writer.write ("fudgeSuperBuilder");
         first = false;
       }
       for (FieldDefinition field : required) {
         if (first) first = false; else writer.write (", ");
         writer.write (localFieldName (field));
       }
       writer.write (")");
       endStmt (writer);
       writeGetFudgeFields (writer, optional, false, true);
     } else {
       writeGetFudgeFields (writer, optional, false, false);
     }
   }
   
   private void writeFromFudgeMsg (final IndentWriter writer, final MessageDefinition message, final boolean useBuilder) throws IOException {
     writer.write ("public static " + message.getName () + " fromFudgeMsg (final " + CLASS_FUDGEFIELDCONTAINER + " fudgeMsg)");
     beginBlock (writer);
     if (useBuilder) {
       writer.write ("return builderFromFudgeMsg (fudgeMsg).build ()");
     } else {
       writer.write ("return new " + message.getName () + " (fudgeMsg)");
     }
     endStmt (writer);
     endBlock (writer);
   }
   
   private void writeEquals (JavaWriter writer, final MessageDefinition message) throws IOException {
     writer.method (false, "boolean", "equals", "final Object o");
     writer = beginBlock (writer);
     writer.ifNull ("o");
     writer.returnFalse ();
     endStmt (writer);
     writer.ifNotInstanceOf ("o", message.getName ());
     writer.returnFalse ();
     endStmt (writer);
     writer.namedLocalVariable (message.getName (), "msg", "(" + message.getName () + ")o");
     endStmt (writer);
     for (FieldDefinition field : message.getFieldDefinitions ()) {
       final String a = privateFieldName (field);
       final String b = "msg." + a;
       final FieldType type = field.getType ();
       if (type instanceof FieldType.ArrayType) {
         if (field.isRepeated ()) {
           writer.ifNotNull (a);
           writer.ifNotNull (b);
           writer = beginBlock (writer);
           writer.ifBool (a + ".size () != " + b + ".size ()");
           writer.returnFalse (); // lists are different lengths
           endStmt (writer);
           final String i = writer.forEachIndex (a, "size ()");
           writer = beginBlock (writer); // for
           writer.ifNotBool (CLASS_ARRAYS + "." + (isObjectArray ((FieldType.ArrayType)type) ? "deepEquals" : "equals") + " (" + a + ".get (" + i + "), " + b + ".get (" + i + "))");
           writer.returnFalse ();
           endStmt (writer);
           writer = endBlock (writer); // for
           writer = endBlock (writer); // if
           writer.elseReturnFalse (); // a is not null, but b is null
           endStmt (writer);
           writer.elseIfNotNull (b);
           writer.returnFalse (); // a is null, b is not null
         } else {
           writer.ifNotBool (CLASS_ARRAYS + "." + (isObjectArray ((FieldType.ArrayType)type) ? "deepEquals" : "equals") + " (" + a + ", " + b + ")");
           writer.returnFalse ();
         }
       } else {
         if (isObject (type) || field.isRepeated ()) {
           writer.ifNotNull (a);
           writer.ifNotNull (b);
           writer = beginBlock (writer);
           writer.ifNotBool (a + ".equals (" + b + ")");
           writer.returnFalse (); // a is not equal to b
           endStmt (writer);
           writer = endBlock (writer);
           writer.elseReturnFalse (); // a is not null, but b is null
           endStmt (writer);
           writer.elseIfNotNull (b);
           writer.returnFalse (); // a is null, b is not null
         } else {
           writer.ifBool (a + " != " + b);
           writer.returnFalse ();
         }
       }
       endStmt (writer);
     }
     if (message.getExtends () != null) {
       writer.returnInvoke ("super.equals", "msg", null);
     } else {
       writer.returnTrue ();
     }
     endStmt (writer);
     writer = endBlock (writer);
   }
   
   private void writeHashCode (JavaWriter writer, final MessageDefinition message) throws IOException {
     writer.method (false, "int", "hashCode", null);
     writer = beginBlock (writer);
     writer.namedLocalVariable ("int", "hc", null);
     endStmt (writer);
     writer.assignment ("hc", (message.getExtends () != null) ? "super.hashCode ()" : "1");
     endStmt (writer);
     for (FieldDefinition field : message.getFieldDefinitions ()) {
       final String name = privateFieldName (field);
       final FieldType type = field.getType ();
       if (type instanceof FieldType.ArrayType) {
         writer.assignment ("hc", "hc * 31");
         endStmt (writer);
         writer.ifNotNull (name);
         if (field.isRepeated ()) {
           final String repeated = writer.forEach (typeString (type, true), name);
           writer = beginBlock (writer);
           writer.assignment ("hc", "hc * 31");
           endStmt (writer);
           writer.assignment ("hc", "hc + " + CLASS_ARRAYS + "." + (isObjectArray ((FieldType.ArrayType)type) ? "deepHashCode" : "hashCode") + " (" + repeated + ")");
           endStmt (writer);
           writer = endBlock (writer);
         } else {
           writer.assignment ("hc", "hc + " + CLASS_ARRAYS + "." + (isObjectArray ((FieldType.ArrayType)type) ? "deepHashCode" : "hashCode") + " (" + name + ")");
           endStmt (writer);
         }
       } else {
         writer.assignment ("hc", "hc * 31");
         endStmt (writer);
         if (isObject (type) || field.isRepeated ()) {
           writer.ifNotNull (name);
           writer.assignment ("hc", "hc + " + name + ".hashCode ()");
         } else {
           switch (type.getFudgeFieldType ()) {
           case FudgeTypeDictionary.BOOLEAN_TYPE_ID :
           case FudgeTypeDictionary.INDICATOR_TYPE_ID :
             writer.ifBool (name);
             writer.assignment ("hc", "hc + 1");
             break;
           default :
             writer.assignment ("hc", "hc + (int)" + name);
             break;
           }
         }
         endStmt (writer);
       }
     }
     writer.returnVariable ("hc");
     endStmt (writer);
     writer = endBlock (writer);
   }
   
   private void writeClone (JavaWriter writer, final MessageDefinition message) throws IOException {
     writer.method (false, message.getName (), "clone", null);
     writer = beginBlock (writer); // method
     if (message.getExtends () == null) {
       writer.guard ();
       writer = beginBlock (writer); // try
     }
     writer.returnInvoke ("super.clone", null, message.getName ());
     endStmt (writer);
     if (message.getExtends () == null) {
       writer = endBlock (writer); // try
       writer.catchCloneNotSupportedException ();
       writer = beginBlock (writer); // catch
       writer.throwAssertionError ("Cloning is definately supported");
       endStmt (writer);
       writer = endBlock (writer); // catch
     }
     writer = endBlock (writer); // method
   }
 
   /**
    * We must use a builder if:
    *   a) there are optional immutable fields; or
    *   b) there are immutable fields which have a default value; or
    *   c) the parent message(s) use a builder.
    * 
    * I.e. we omit the builder if it would just have a construct and no mutator methods
    */
   private boolean useBuilderPattern (final MessageDefinition message) {
     for (FieldDefinition field : message.getFieldDefinitions ()) {
       if (!field.isMutable ()) {
         if (!field.isRequired ()) return true; // optional fields - must use a Builder 
         if (field.getDefaultValue () != null) return true; // required field with default value - must use a Builder
       }
     }
     if (message.getExtends () != null) {
       // use a builder if the parent does
       return useBuilderPattern (message.getExtends ());
     } else {
       // we don't need a builder 
       return false;
     }
   }
   
   @Override
   public void writeClassImplementationConstructor(final Compiler.Context context, final MessageDefinition message, final IndentWriter writer) throws IOException {
     final JavaWriter jWriter = new JavaWriter (writer);
     final boolean useBuilder = useBuilderPattern (message);
     if (useBuilder) {
       writeBuilderClass (writer, message);
       writeProtectedBuilderConstructor (jWriter, message);
       writeBuilderFromFudgeMsg (writer, message);
     } else {
       writePublicConstructor (writer, false, message);
       writeProtectedFudgeMsgConstructor (writer, message);
     }
     writeToFudgeMsg (jWriter, message);
     writeFromFudgeMsg (writer, message, useBuilder);
   }
 
   @Override
   public void writeEnumImplementationDeclaration(final Compiler.Context context, EnumDefinition enumDefinition, IndentWriter iWriter) throws IOException {
     super.writeEnumImplementationDeclaration (context, enumDefinition, iWriter);
     JavaWriter writer = new JavaWriter (iWriter);
     if (enumDefinition.getOuterDefinition () == null) {
       final String namespace = enumDefinition.getNamespace ();
       if (namespace != null) {
         writer.packageDef (namespace);
         endStmt (writer);
       }
     }
     writer.enumDef (enumDefinition.getName ());
     writer = beginBlock (writer); // enum
     Iterator<Map.Entry<String,Integer>> elements = enumDefinition.getElements ();
     boolean first = true;
     while (elements.hasNext ()) {
       final Map.Entry<String,Integer> element = elements.next ();
       if (first) {
         first = false;
       } else {
         writer.enumElementSeparator ();
       }
       writer.enumElement (element.getKey (), element.getValue ().toString ());
     }
     endStmt (writer); // initial enumset
     writer.attribute (true, "int", "_fudgeEncoding");
     endStmt (writer); // ordinal def
     writer.constructor ("private", enumDefinition.getName (), "final int fudgeEncoding");
     writer = beginBlock (writer); // constructor
     writer.assignment ("_fudgeEncoding", "fudgeEncoding");
     endStmt (writer); // assignment
     writer = endBlock (writer); // constructor
     writer.method (false, "int", "getFudgeEncoding", null);
     writer = beginBlock (writer); // getFudgeEncoding
     writer.returnVariable ("_fudgeEncoding");
     endStmt (writer); // return
     writer = endBlock (writer); // getFudgeEncoding
     writer.method (true, enumDefinition.getName (), "fromFudgeEncoding", "final int fudgeEncoding");
     writer = beginBlock (writer); // fromFudgeEncoding
     writer.select ("fudgeEncoding");
     beginBlock (writer); // switch
     elements = enumDefinition.getElements ();
     while (elements.hasNext ()) {
       final Map.Entry<String,Integer> element = elements.next ();
       writer.selectCaseReturn (element.getValue ().toString (), element.getKey ());
       endStmt (writer);
     }
     writer.defaultThrowInvalidFudgeEnumException (enumDefinition, "fudgeEncoding");
     endStmt (writer); // default
     endBlock (writer); // switch
     endBlock (writer); // fromFudgeEncoding
     endBlock (writer); // enum
   }
   
   @Override
   public void writeTaxonomyImplementationDeclaration (final Compiler.Context context, final TaxonomyDefinition taxonomy, final IndentWriter iWriter) throws IOException {
     super.writeTaxonomyImplementationDeclaration (context, taxonomy, iWriter);
     JavaWriter writer = beginClass (new JavaWriter (iWriter), taxonomy, CLASS_MAPFUDGETAXONOMY, null);
     writer.publicStaticFinal (CLASS_FUDGETAXONOMY, "INSTANCE", "new " + taxonomy.getName () + " ()");
     endStmt (writer); // instance
     final StringBuilder sbOrdinals = new StringBuilder ();
     final StringBuilder sbStrings = new StringBuilder ();
     final Iterator<Map.Entry<String,Integer>> elements = taxonomy.getElements ();
     while (elements.hasNext ()) {
       final Map.Entry<String,Integer> element = elements.next ();
       final String name = element.getKey ();
       writer.publicStaticFinal ("String", "STR_" + name, "\"" + name + "\"");
       endStmt (writer); // STR_ decl
       if (sbStrings.length () > 0) sbStrings.append (", ");
       sbStrings.append ("STR_").append (name);
       writer.publicStaticFinal ("short", "VAL_" + name, element.getValue ().toString ());
       endStmt (writer); // VAL_ decl
       if (sbOrdinals.length () > 0) sbOrdinals.append (", ");
       sbOrdinals.append ("VAL_").append (name);
     }
     writer.constructor ("private", taxonomy.getName (), null);
     writer = beginBlock (writer); // constructor
     writer.invoke ("super", "new int[] { " + sbOrdinals.toString () + " }, new String[] { " + sbStrings.toString () + " }");
     endStmt (writer); // super
     endBlock (writer); // constructor
     endBlock (writer); // class
   }
 
   private String typeString (final FieldType type, final boolean asObject) {
     if (type instanceof FieldType.ArrayType) {
       final FieldType.ArrayType array = (FieldType.ArrayType)type;
       final StringBuilder sb = new StringBuilder ();
       sb.append (typeString (array.getBaseType (), false));
       sb.append ("[]");
       return sb.toString ();
     } else if (type instanceof FieldType.EnumType) {
       return ((FieldType.EnumType)type).getEnumDefinition ().getIdentifier ();
     } else if (type instanceof FieldType.MessageType) {
       return ((FieldType.MessageType)type).getMessageDefinition ().getIdentifier ();
     } else {
       switch (type.getFudgeFieldType ()) {
       case FudgeTypeDictionary.INDICATOR_TYPE_ID :
         // We'll handle indicators as a boolean - was it in the Fudge message or not
         return asObject ? "Boolean" : "boolean";
       case FudgeTypeDictionary.BOOLEAN_TYPE_ID :
         return asObject ? "Boolean" : "boolean";
       case FudgeTypeDictionary.BYTE_TYPE_ID :
         return asObject ? "Byte" : "byte";
       case FudgeTypeDictionary.SHORT_TYPE_ID :
         return asObject ? "Short" : "short";
       case FudgeTypeDictionary.INT_TYPE_ID :
         return asObject ? "Integer" : "int";
       case FudgeTypeDictionary.LONG_TYPE_ID :
         return asObject ? "Long" : "long";
       case FudgeTypeDictionary.FLOAT_TYPE_ID :
         return asObject ? "Float" : "float";
       case FudgeTypeDictionary.DOUBLE_TYPE_ID :
         return asObject ? "Double" : "double";
       case FudgeTypeDictionary.STRING_TYPE_ID :
         return "String";
       default :
         throw new IllegalStateException ("type '" + type + "' is not an expected type (fudge field type " + type.getFudgeFieldType () + ")");
       }
     }
   }
   
 }
