 /*******************************************************************************
 * Copyright (c) 2009 Centrum Wiskunde en Informatica (CWI)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Arnold Lankamp - interfaces and implementation
 *******************************************************************************/
 package org.eclipse.imp.pdb.facts.io.binary;
 
 import java.io.IOException;
 import java.io.InputStream;
 import java.math.BigDecimal;
 import java.math.BigInteger;
 import java.net.URI;
 import java.net.URISyntaxException;
 
 import org.eclipse.imp.pdb.facts.IBool;
 import org.eclipse.imp.pdb.facts.IConstructor;
 import org.eclipse.imp.pdb.facts.IDateTime;
 import org.eclipse.imp.pdb.facts.IInteger;
 import org.eclipse.imp.pdb.facts.IList;
 import org.eclipse.imp.pdb.facts.IListWriter;
 import org.eclipse.imp.pdb.facts.IMap;
 import org.eclipse.imp.pdb.facts.IMapWriter;
 import org.eclipse.imp.pdb.facts.INode;
 import org.eclipse.imp.pdb.facts.IReal;
 import org.eclipse.imp.pdb.facts.IRelation;
 import org.eclipse.imp.pdb.facts.IRelationWriter;
 import org.eclipse.imp.pdb.facts.ISet;
 import org.eclipse.imp.pdb.facts.ISetWriter;
 import org.eclipse.imp.pdb.facts.ISourceLocation;
 import org.eclipse.imp.pdb.facts.IString;
 import org.eclipse.imp.pdb.facts.ITuple;
 import org.eclipse.imp.pdb.facts.IValue;
 import org.eclipse.imp.pdb.facts.IValueFactory;
 import org.eclipse.imp.pdb.facts.exceptions.FactParseError;
 import org.eclipse.imp.pdb.facts.type.Type;
 import org.eclipse.imp.pdb.facts.type.TypeFactory;
 import org.eclipse.imp.pdb.facts.type.TypeStore;
 import org.eclipse.imp.pdb.facts.util.ResizingArray;
 import org.eclipse.imp.pdb.facts.util.ShareableHashMap;
 
 // TODO Change this thing so it doesn't use recursion.
 /**
  * @author Arnold Lankamp
  */
 public class BinaryReader{
 	private final static int DEFAULT_SHARED_VALUES_STORE_SIZE = 1024;
 	private final static int DEFAULT_SHARED_TYPES_STORE_SIZE = 128;
 	private final static int DEFAULT_SHARED_PATHS_STORE_SIZE = 128;
 	private final static int DEFAULT_SHARED_NAMES_STORE_SIZE = 128;
 
 	private final static int BOOL_HEADER = 0x01;
 	private final static int INTEGER_HEADER = 0x02;
 	private final static int BIG_INTEGER_HEADER = 0x03; // Special case of INTEGER_HEADER (flags for alternate encoding).
 	private final static int DOUBLE_HEADER = 0x04;
 	private final static int IEEE754_ENCODED_DOUBLE_HEADER = 0x14;
 	private final static int STRING_HEADER = 0x05;
 	private final static int SOURCE_LOCATION_HEADER = 0x06;
 	private final static int DATE_TIME_HEADER = 0x10;
 	private final static int TUPLE_HEADER = 0x07;
 	private final static int NODE_HEADER = 0x08;
 	private final static int ANNOTATED_NODE_HEADER = 0x09;
 	private final static int CONSTRUCTOR_HEADER = 0x0a;
 	private final static int ANNOTATED_CONSTRUCTOR_HEADER = 0x0b;
 	private final static int LIST_HEADER = 0x0c;
 	private final static int SET_HEADER = 0x0d;
 	private final static int RELATION_HEADER = 0x0e;
 	private final static int MAP_HEADER = 0x0f;
 	
 	private final static int VALUE_TYPE_HEADER = 0x01;
 	private final static int VOID_TYPE_HEADER = 0x02;
 	private final static int BOOL_TYPE_HEADER = 0x03;
 	private final static int INTEGER_TYPE_HEADER = 0x04;
 	private final static int DOUBLE_TYPE_HEADER = 0x05;
 	private final static int STRING_TYPE_HEADER = 0x06;
 	private final static int SOURCE_LOCATION_TYPE_HEADER = 0x07;
 	private final static int DATE_TIME_TYPE_HEADER = 0x14;
 	private final static int NODE_TYPE_HEADER = 0x08;
 	private final static int TUPLE_TYPE_HEADER = 0x09;
 	private final static int LIST_TYPE_HEADER = 0x0a;
 	private final static int SET_TYPE_HEADER = 0x0b;
 	private final static int RELATION_TYPE_HEADER = 0x0c;
 	private final static int MAP_TYPE_HEADER = 0x0d;
 	private final static int PARAMETER_TYPE_HEADER = 0x0e;
 	private final static int ADT_TYPE_HEADER = 0x0f;
 	private final static int CONSTRUCTOR_TYPE_HEADER = 0x10;
 	private final static int ALIAS_TYPE_HEADER = 0x11;
 	private final static int ANNOTATED_NODE_TYPE_HEADER = 0x12;
 	private final static int ANNOTATED_CONSTRUCTOR_TYPE_HEADER = 0x13;
 	
 	private final static int TYPE_MASK = 0x1f;
 	
 	private final static int SHARED_FLAG = 0x80;
 	private final static int TYPE_SHARED_FLAG = 0x40;
 	private final static int URL_SHARED_FLAG = 0x20;
 	private final static int NAME_SHARED_FLAG = 0x20;
 	
 	private final static int HAS_FIELD_NAMES = 0x20;
 	
 	private final static int DATE_TIME_INDICATOR = 0x01;
 	private final static int DATE_INDICATOR = 0x02;
 	
 	private final static TypeFactory tf = TypeFactory.getInstance();
 	
 	private final ResizingArray<IValue> sharedValues;
 	private int currentSharedValueId;
 	private final ResizingArray<Type> sharedTypes;
 	private int currentSharedTypeId;
 	private final ResizingArray<String> sharedPaths;
 	private int currentSharedPathId;
 	private final ResizingArray<String> sharedNames;
 	private int currentSharedNamesId;
 	
 	private final IValueFactory valueFactory;
 	private final TypeStore typeStore;
 	private final InputStream in;
 	
 	public BinaryReader(IValueFactory valueFactory, TypeStore typeStore, InputStream inputStream){
 		super();
 		
 		this.valueFactory = valueFactory;
 		this.typeStore = typeStore;
 		this.in = new InputStreamCheckerWrapper(inputStream);
 
 		sharedValues = new ResizingArray<IValue>(DEFAULT_SHARED_VALUES_STORE_SIZE);
 		currentSharedValueId = 0;
 		sharedTypes = new ResizingArray<Type>(DEFAULT_SHARED_TYPES_STORE_SIZE);
 		currentSharedTypeId = 0;
 		sharedPaths = new ResizingArray<String>(DEFAULT_SHARED_PATHS_STORE_SIZE);
 		currentSharedPathId = 0;
 		sharedNames = new ResizingArray<String>(DEFAULT_SHARED_NAMES_STORE_SIZE);
 		currentSharedNamesId = 0;
 	}
 	
 	public IValue deserialize() throws IOException{
 		int header = in.read();
 		if((header & SHARED_FLAG) == SHARED_FLAG){
 			return sharedValues.get(parseInteger());
 		}
 		
 		IValue value;
 		
 		int valueType = header & TYPE_MASK;
 		switch(valueType){
 			case BOOL_HEADER:
 				value = readBool();
 				break;
 			case INTEGER_HEADER:
 				value = readInteger();
 				break;
 			case BIG_INTEGER_HEADER:
 				value = readBigInteger();
 				break;
 			case DOUBLE_HEADER:
 				value = readDouble();
 				break;
 			case IEEE754_ENCODED_DOUBLE_HEADER:
 				value = readIEEE754EncodedDouble();
 				break;
 			case STRING_HEADER:
 				value = readString();
 				break;
 			case SOURCE_LOCATION_HEADER:
 				value = readSourceLocation(header);
 				break;
 			case DATE_TIME_HEADER:
 				value = readDateTime();
 				break;
 			case TUPLE_HEADER:
 				value = readTuple();
 				break;
 			case NODE_HEADER:
 				value = readNode(header);
 				break;
 			case ANNOTATED_NODE_HEADER:
 				value = readAnnotatedNode(header);
 				break;
 			case CONSTRUCTOR_HEADER:
 				value = readConstructor(header);
 				break;
 			case ANNOTATED_CONSTRUCTOR_HEADER:
 				value = readAnnotatedConstructor(header);
 				break;
 			case LIST_HEADER:
 				value = readList(header);
 				break;
 			case SET_HEADER:
 				value = readSet(header);
 				break;
 			case RELATION_HEADER:
 				value = readRelation(header);
 				break;
 			case MAP_HEADER:
 				value = readMap(header);
 				break;
 			default:
 				throw new RuntimeException("Unknow value type: "+valueType);
 		}
 		
 		sharedValues.set(value, currentSharedValueId++);
 		
 		return value;
 	}
 	
 	// Called by value stuff.
 	private Type readType(int header) throws IOException{
 		if((header & TYPE_SHARED_FLAG) == TYPE_SHARED_FLAG){
 			return sharedTypes.get(parseInteger());
 		}
 		
 		return doReadType(in.read());
 	}
 	
 	// Called by type stuff.
 	private Type doReadType() throws IOException{
 		return doReadType(in.read());
 	}
 	
 	private Type doReadType(int typeHeader) throws IOException{
 		if((typeHeader & SHARED_FLAG) == SHARED_FLAG){
 			return sharedTypes.get(parseInteger());
 		}
 		
 		Type type;
 		
 		int typeType = typeHeader & TYPE_MASK;
 		switch(typeType){
 			case VALUE_TYPE_HEADER:
 				type = readValueType();
 				break;
 			case VOID_TYPE_HEADER:
 				type = readVoidType();
 				break;
 			case BOOL_TYPE_HEADER:
 				type = readBoolType();
 				break;
 			case INTEGER_TYPE_HEADER:
 				type = readIntegerType();
 				break;
 			case DOUBLE_TYPE_HEADER:
 				type = readDoubleType();
 				break;
 			case STRING_TYPE_HEADER:
 				type = readStringType();
 				break;
 			case SOURCE_LOCATION_TYPE_HEADER:
 				type = readSourceLocationType();
 				break;
 			case DATE_TIME_TYPE_HEADER:
 				type = readDateTimeType();
 				break;
 			case NODE_TYPE_HEADER:
 				type = readNodeType();
 				break;
 			case TUPLE_TYPE_HEADER:
 				type = readTupleType(typeHeader);
 				break;
 			case LIST_TYPE_HEADER:
 				type = readListType();
 				break;
 			case SET_TYPE_HEADER:
 				type = readSetType();
 				break;
 			case RELATION_TYPE_HEADER:
 				type = readRelationType();
 				break;
 			case MAP_TYPE_HEADER:
 				type = readMapType();
 				break;
 			case PARAMETER_TYPE_HEADER:
 				type = readParameterType();
 				break;
 			case ADT_TYPE_HEADER:
 				type = readADTType();
 				break;
 			case CONSTRUCTOR_TYPE_HEADER:
 				type = readConstructorType();
 				break;
 			case ALIAS_TYPE_HEADER:
 				type = readAliasType();
 				break;
 			case ANNOTATED_NODE_TYPE_HEADER:
 				type = readAnnotatedNodeType();
 				break;
 			case ANNOTATED_CONSTRUCTOR_TYPE_HEADER:
 				type = readAnnotatedConstructorType();
 				break;
 			default:
 				throw new RuntimeException("Unkown type type: "+typeType);
 		}
 		
 		sharedTypes.set(type, currentSharedTypeId++);
 		
 		return type;
 	}
 	
 	private IBool readBool() throws IOException{
 		int bool = in.read();
 		
 		return valueFactory.bool(bool == 0 ? false : true);
 	}
 	
 	private IInteger readInteger() throws IOException{
 		int integerValue = parseInteger();
 		
 		return valueFactory.integer(integerValue);
 	}
 	
 	private IInteger readBigInteger() throws IOException{
 		int length = parseInteger();
 		byte[] integerData = new byte[length];
 		in.read(integerData, 0, length);
 		
 		return valueFactory.integer(integerData);
 	}
 	
 	private IReal readDouble() throws IOException{
 		int length = parseInteger();
 		byte[] unscaledValueData = new byte[length];
 		in.read(unscaledValueData, 0, length);
 		int scale = parseInteger();
 		
 		return valueFactory.real(new BigDecimal(new BigInteger(unscaledValueData), scale).toString()); // The toString call kind of stinks.
 	}
 	
 	private IReal readIEEE754EncodedDouble() throws IOException{
 		double theDouble = parseDouble();
 		
 		return valueFactory.real(theDouble); // The toString call kind of stinks.
 	}
 	
 	private IString readString() throws IOException{
 		int size = parseInteger();
 		
 		byte[] data = new byte[size];
 		for(int i = 0; i< size; i++){
 			data[i] = (byte) in.read();
 		}
 		
 		return valueFactory.string(new String(data));
 	}
 	
 	private ISourceLocation readSourceLocation(int header) throws IOException{
 		String path;
 		if((header & URL_SHARED_FLAG) == URL_SHARED_FLAG){
 			int path_id = parseInteger();
 			path = sharedPaths.get(path_id);
 		}else{
 			int pathSize = parseInteger();
 			
 			byte[] data = new byte[pathSize];
 			for(int i = 0; i< pathSize; i++){
 				data[i] = (byte) in.read();
 			}
 			
 			path = new String(data);
 			sharedPaths.set(path, currentSharedPathId++);
 		}
 		
 		URI uri;
 		try{
 			uri = new URI(path);
 		}catch(URISyntaxException e){
 			throw new FactParseError("Illegal URI", e); // Can't happen.
 		}
 		
 		int offset = parseInteger();
 		int length = parseInteger();
 		int beginLine = parseInteger();
 		int endLine = parseInteger();
 		int beginCol = parseInteger();
 		int endCol = parseInteger();
 		
 		return valueFactory.sourceLocation(uri, offset, length, beginLine, endLine, beginCol, endCol);
 	}
 	
 	private IDateTime readDateTime() throws IOException{
 		int typeIndicator = in.read();
 		
 		if(typeIndicator == DATE_TIME_INDICATOR){
 			int year = parseInteger();
 			int month = parseInteger();
 			int day = parseInteger();
 			
 			int hour = parseInteger();
 			int minute = parseInteger();
 			int second = parseInteger();
 			int millisecond = parseInteger();
 			
 			int timeZoneHourOffset = parseInteger();
 			int timeZoneMinuteOffset = parseInteger();
 			
 			return valueFactory.datetime(year, month, day, hour, minute, second, millisecond, timeZoneHourOffset, timeZoneMinuteOffset);
 		}else if(typeIndicator == DATE_INDICATOR){
 			int year = parseInteger();
 			int month = parseInteger();
 			int day = parseInteger();
 			
 			return valueFactory.date(year, month, day);
 		}else{
 			int hour = parseInteger();
 			int minute = parseInteger();
 			int second = parseInteger();
 			int millisecond = parseInteger();
 			
 			int timeZoneHourOffset = parseInteger();
 			int timeZoneMinuteOffset = parseInteger();
 			
 			return valueFactory.time(hour, minute, second, millisecond, timeZoneHourOffset, timeZoneMinuteOffset);
 		}
 	}
 	
 	private ITuple readTuple() throws IOException{
 		int arity = parseInteger();
 		
 		IValue[] content = new IValue[arity];
 		for(int i = 0; i < arity; i++){
 			content[i] = deserialize();
 		}
 		
 		return valueFactory.tuple(content);
 	}
 	
 	private INode readNode(int header) throws IOException{
 		String nodeName;
 		if((header & NAME_SHARED_FLAG) == NAME_SHARED_FLAG){
 			nodeName = sharedNames.get(parseInteger());
 		}else{
 			int nodeNameLength = parseInteger();
 			
 			byte[] data = new byte[nodeNameLength];
 			for(int i = 0; i < nodeNameLength; i++){
 				data[i] = (byte) in.read();
 			}
 			nodeName = new String(data);
 			
 			sharedNames.set(nodeName, currentSharedNamesId++);
 		}
 		
 		int arity = parseInteger();
 		
 		IValue[] content = new IValue[arity];
 		for(int i = 0; i < arity; i++){
 			content[i] = deserialize();
 		}
 		
 		return valueFactory.node(nodeName, content);
 	}
 	
 	private INode readAnnotatedNode(int header) throws IOException{
 		String nodeName;
 		if((header & NAME_SHARED_FLAG) == NAME_SHARED_FLAG){
 			nodeName = sharedNames.get(parseInteger());
 		}else{
 			int nodeNameLength = parseInteger();
 			
 			byte[] data = new byte[nodeNameLength];
 			for(int i = 0; i < nodeNameLength; i++){
 				data[i] = (byte) in.read();
 			}
 			nodeName = new String(data);
 
 			sharedNames.set(nodeName, currentSharedNamesId++);
 		}
 		
 		int arity = parseInteger();
 		
 		IValue[] content = new IValue[arity];
 		for(int i = 0; i < arity; i++){
 			content[i] = deserialize();
 		}
 		
 		int numberOfAnnotations = parseInteger();
 		
 		ShareableHashMap<String, IValue> annotations = new ShareableHashMap<String, IValue>();
 		for(int i = numberOfAnnotations - 1; i >= 0; i--){
 			int labelLength = parseInteger();
 			byte[] labelData = new byte[labelLength];
 			in.read(labelData);
 			String label = new String(labelData);
 			
 			IValue value = deserialize();
 			
 			annotations.put(label, value);
 		}
 		
 		INode node = valueFactory.node(nodeName, content);
 		return node.setAnnotations(annotations);
 	}
 	
 	private IConstructor readConstructor(int header) throws IOException{
 		Type constructorType = readType(header);
 		
 		int arity = parseInteger();
 		
 		IValue[] content = new IValue[arity];
 		for(int i = 0; i < arity; i++){
 			content[i] = deserialize();
 		}
 		
 		return valueFactory.constructor(constructorType, content);
 	}
 	
 	private IConstructor readAnnotatedConstructor(int header) throws IOException{
 		Type constructorType = readType(header);
 		
 		int arity = parseInteger();
 		
 		IValue[] content = new IValue[arity];
 		for(int i = 0; i < arity; i++){
 			content[i] = deserialize();
 		}
 		
 		int numberOfAnnotations = parseInteger();
 		
 		ShareableHashMap<String, IValue> annotations = new ShareableHashMap<String, IValue>();
 		for(int i = numberOfAnnotations - 1; i >= 0; i--){
 			int labelLength = parseInteger();
 			byte[] labelData = new byte[labelLength];
 			in.read(labelData);
 			String label = new String(labelData);
 			
 			IValue value = deserialize();
 			
 			annotations.put(label, value);
 		}
 		
 		IConstructor constructor = valueFactory.constructor(constructorType, content);
 		return constructor.setAnnotations(annotations);
 	}
 	
 	private IList readList(int header) throws IOException{
 		Type elementType = readType(header);
 		
 		int length = parseInteger();
 		
 		IListWriter listWriter = valueFactory.listWriter(elementType);
 		for(int i = 0; i < length; i++){
 			listWriter.append(deserialize());
 		}
 		
 		return listWriter.done();
 	}
 	
 	private ISet readSet(int header) throws IOException{
 		Type elementType = readType(header);
 		
 		int length = parseInteger();
 		
 		ISetWriter setWriter = valueFactory.setWriter(elementType);
 		for(int i = 0; i < length; i++){
 			setWriter.insert(deserialize());
 		}
 		
 		return setWriter.done();
 	}
 	
 	private IRelation readRelation(int header) throws IOException{
 		Type elementType = readType(header);
 		
 		int length = parseInteger();
 		
 		IRelationWriter relationWriter = valueFactory.relationWriter(elementType);
 		for(int i = 0; i < length; i++){
 			relationWriter.insert(deserialize());
 		}
 		
 		return relationWriter.done();
 	}
 	
 	private IMap readMap(int header) throws IOException{
 		Type mapType = readType(header);
 
 		Type keyType = mapType.getKeyType();
 		Type valueType = mapType.getValueType();
 		
 		int length = parseInteger();
 		
 		IMapWriter mapWriter = valueFactory.mapWriter(keyType, valueType);
 		for(int i = 0; i < length; i++){
 			IValue key = deserialize();
 			IValue value = deserialize();
 			
 			mapWriter.put(key, value);
 		}
 		
 		return mapWriter.done();
 	}
 	
 	private Type readValueType(){
 		return tf.valueType();
 	}
 	
 	private Type readVoidType(){
 		return tf.voidType();
 	}
 	
 	private Type readBoolType(){
 		return tf.boolType();
 	}
 	
 	private Type readIntegerType(){
 		return tf.integerType();
 	}
 	
 	private Type readDoubleType(){
 		return tf.realType();
 	}
 	
 	private Type readStringType(){
 		return tf.stringType();
 	}
 	
 	private Type readSourceLocationType(){
 		return tf.sourceLocationType();
 	}
 	
 	private Type readDateTimeType(){
 		return tf.dateTimeType();
 	}
 	
 	private Type readNodeType(){
 		return tf.nodeType();
 	}
 	
 	private Type readAnnotatedNodeType() throws IOException{
 		Type nodeType = tf.nodeType();
 		
 		int nrOfAnnotations = parseInteger();
 		for(--nrOfAnnotations; nrOfAnnotations >= 0; nrOfAnnotations--){
 			int nrOfLabelBytes = parseInteger();
 			byte[] labelBytes = new byte[nrOfLabelBytes];
 			in.read(labelBytes);
 			String label = new String(labelBytes);
 			
 			Type valueType = doReadType();
 			
 			typeStore.declareAnnotation(nodeType, label, valueType);
 		}
 		
 		return nodeType;
 	}
 	
 	private Type readTupleType(int header) throws IOException{
 		boolean hasFieldNames = ((header & HAS_FIELD_NAMES) == HAS_FIELD_NAMES);
 		
 		if(hasFieldNames){
 			int arity = parseInteger();
 			
 			Type[] fields = new Type[arity];
 			String[] fieldNames = new String[arity];
 			for(int i = 0; i < arity; i++){
 				fields[i] = doReadType();
 				
 				int fieldNameLength = parseInteger();
 				byte[] fieldNameData = new byte[fieldNameLength];
 				in.read(fieldNameData);
 				fieldNames[i] = new String(fieldNameData);
 			}
 			
 			return tf.tupleType(fields, fieldNames);
 		}
 		
 		int arity = parseInteger();
 		
 		Type[] fields = new Type[arity];
 		for(int i = 0; i < arity; i++){
 			fields[i] = doReadType();
 		}
 		
 		return tf.tupleType(fields);
 	}
 	
 	private Type readListType() throws IOException{
 		Type elementType = doReadType();
 		
 		return tf.listType(elementType);
 	}
 	
 	private Type readSetType() throws IOException{
 		Type elementType = doReadType();
 		
 		return tf.setType(elementType);
 	}
 	
 	private Type readRelationType() throws IOException{
 		Type elementType = doReadType();
 		
 		return tf.relTypeFromTuple(elementType);
 	}
 	
 	private Type readMapType() throws IOException{
 		Type keyType = doReadType();
 		Type valueType = doReadType();
 		
 		return tf.mapType(keyType, valueType);
 	}
 	
 	private Type readParameterType() throws IOException{
 		int nameLength = parseInteger();
 		byte[] nameData = new byte[nameLength];
 		in.read(nameData);
 		String name = new String(nameData);
 		
 		Type bound = doReadType();
 		
 		return tf.parameterType(name, bound);
 	}
 	
 	private Type readADTType() throws IOException{
 		int nameLength = parseInteger();
 		byte[] nameData = new byte[nameLength];
 		in.read(nameData);
 		String name = new String(nameData);
 		
 		Type parameters = doReadType();
 		
 		return tf.abstractDataTypeFromTuple(typeStore, name, parameters);
 	}
 	
 	private Type readConstructorType() throws IOException{
 		int nameLength = parseInteger();
 		byte[] nameData = new byte[nameLength];
 		in.read(nameData);
 		String name = new String(nameData);
 		
 		Type fieldTypes = doReadType();
 		
 		Type adtType = doReadType();
 		
 		return tf.constructorFromTuple(typeStore, adtType, name, fieldTypes);
 	}
 	
 	private Type readAnnotatedConstructorType() throws IOException{
 		int nameLength = parseInteger();
 		byte[] nameData = new byte[nameLength];
 		in.read(nameData);
 		String name = new String(nameData);
 		
 		Type fieldTypes = doReadType();
 		
 		Type adtType = doReadType();
 		
 		Type constructorType = tf.constructorFromTuple(typeStore, adtType, name, fieldTypes);
 		
 		int nrOfAnnotations = parseInteger();
 		for(--nrOfAnnotations; nrOfAnnotations >= 0; nrOfAnnotations--){
 			int nrOfLabelBytes = parseInteger();
 			byte[] labelBytes = new byte[nrOfLabelBytes];
 			in.read(labelBytes);
 			String label = new String(labelBytes);
 			
 			Type valueType = doReadType();
 			
 			typeStore.declareAnnotation(constructorType, label, valueType);
 		}
 		
 		return constructorType;
 	}
 	
 	private Type readAliasType() throws IOException{
 		int nameLength = parseInteger();
 		byte[] nameData = new byte[nameLength];
 		in.read(nameData);
 		String name = new String(nameData);
 		
 		Type aliasedType = doReadType();
 		
 		Type parameters = doReadType();
 		
		return tf.aliasType(typeStore, name, aliasedType, parameters);
 	}
 	
 	private final static int SEVENBITS = 0x0000007f;
 	private final static int SIGNBIT = 0x00000080;
 	
 	private int parseInteger() throws IOException{
 		int part = in.read();
 		int result = (part & SEVENBITS);
 		
 		if((part & SIGNBIT) == 0) return result;
 			
 		part = in.read();
 		result |= ((part & SEVENBITS) << 7);
 		if((part & SIGNBIT) == 0) return result;
 			
 		part = in.read();
 		result |= ((part & SEVENBITS) << 14);
 		if((part & SIGNBIT) == 0) return result;
 			
 		part = in.read();
 		result |= ((part & SEVENBITS) << 21);
 		if((part & SIGNBIT) == 0) return result;
 			
 		part = in.read();
 		result |= ((part & SEVENBITS) << 28);
 		return result;
 	}
 
 	private final static int BYTEMASK = 0x000000ff;
 	private final static int BYTEBITS = 8;
 	private final static int LONGBITS = 8;
 	
 	private double parseDouble() throws IOException{
 		long result = 0;
 		for(int i = 0; i < LONGBITS; i++){
 			result |= ((((long) in.read()) & BYTEMASK) << (i * BYTEBITS));
 		}
 		return Double.longBitsToDouble(result);	
 	}
 	
 	private static class InputStreamCheckerWrapper extends InputStream{
 		private final InputStream backingStream;
 		
 		public InputStreamCheckerWrapper(InputStream backingStream){
 			super();
 			
 			this.backingStream = backingStream;
 		}
 		
 		public int read() throws IOException{
 			int b = backingStream.read();
 			if(b == -1) throw new IOException("Encountered premature EOF.");
 			
 			return b;
 		}
 	}
 }
