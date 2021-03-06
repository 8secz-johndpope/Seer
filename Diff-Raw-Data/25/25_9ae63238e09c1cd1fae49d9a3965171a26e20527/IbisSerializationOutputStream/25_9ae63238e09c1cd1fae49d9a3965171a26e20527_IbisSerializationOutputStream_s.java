 package ibis.io;
 
 import ibis.util.TypedProperties;
 
 import java.io.IOException;
 import java.io.NotActiveException;
 import java.io.NotSerializableException;
 import java.io.ObjectOutput;
 import java.io.ObjectStreamClass;
 
 
 /**
  * This is the <code>SerializationOutputStream</code> version that is used
  * for Ibis serialization.
  * An effort has been made to make it look like and extend
  * <code>java.io.ObjectOutputStream</code>.
  * However, versioning is not supported, like it is in Sun serialization.
  */
 public class IbisSerializationOutputStream
 	extends DataSerializationOutputStream
 	implements IbisStreamFlags
 {
     /**
      * Record how many objects of any class are sent.
      */
     private static final boolean STATS_OBJECTS = TypedProperties.booleanProperty(IOProps.s_stats_written);
 
     // if STATS_OBJECTS
     private static java.util.Hashtable statSendObjects;
     private static final int[] statArrayCount;
     private static int statObjectHandle;
     private static final int[] statArrayHandle;
     private static final long[] statArrayLength;
 
 
     private static String primitiveName(int i) {
 	switch (i) {
 	case TYPE_BOOLEAN:
 	    return "boolean";
 	case TYPE_BYTE:
 	    return "byte";
 	case TYPE_CHAR:
 	    return "char";
 	case TYPE_SHORT:
 	    return "short";
 	case TYPE_INT:
 	    return "int";
 	case TYPE_LONG:
 	    return "long";
 	case TYPE_FLOAT:
 	    return "float";
 	case TYPE_DOUBLE:
 	    return "double";
 	}
 
 	return null;
     }
 
     private static int primitiveBytes(int i) {
 	switch (i) {
 	case TYPE_BOOLEAN:
 	    return SIZEOF_BOOLEAN;
 	case TYPE_BYTE:
 	    return SIZEOF_BYTE;
 	case TYPE_CHAR:
 	    return SIZEOF_CHAR;
 	case TYPE_SHORT:
 	    return SIZEOF_SHORT;
 	case TYPE_INT:
 	    return SIZEOF_INT;
 	case TYPE_LONG:
 	    return SIZEOF_LONG;
 	case TYPE_FLOAT:
 	    return SIZEOF_FLOAT;
 	case TYPE_DOUBLE:
 	    return SIZEOF_DOUBLE;
 	}
 
 	return 0;
     }
 
 
     private static int arrayClassType(Class arrayClass) {
 	if (false) {
 	} else if (arrayClass == classByteArray) {
 	    return TYPE_BYTE;
 	} else if (arrayClass == classIntArray) {
 	    return TYPE_INT;
 	} else if (arrayClass == classBooleanArray) {
 	    return TYPE_BOOLEAN;
 	} else if (arrayClass == classDoubleArray) {
 	    return TYPE_DOUBLE;
 	} else if (arrayClass == classCharArray) {
 	    return TYPE_CHAR;
 	} else if (arrayClass == classShortArray) {
 	    return TYPE_SHORT;
 	} else if (arrayClass == classLongArray) {
 	    return TYPE_LONG;
 	} else if (arrayClass == classFloatArray) {
 	    return TYPE_FLOAT;
 	}
 	return -1;
     }
 
 
     static {
 	if (STATS_OBJECTS) {
 	    Runtime.getRuntime().addShutdownHook(new Thread("IbisSerializationOutputStream ShutdownHook") {
 		public void run() {
 		    System.out.print("Serializable objects sent: ");
 		    System.out.println(statSendObjects);
 		    System.out.println("Non-array handles sent "
 		       	+ statObjectHandle);
 		    for (int i = BEGIN_TYPES; i < PRIMITIVE_TYPES; i++) {
 			if (statArrayCount[i] + statArrayHandle[i] > 0) {
 			    System.out.println("       "
 				+ primitiveName(i) + " arrays "
 				+ statArrayCount[i] + " total bytes "
 				+ (statArrayLength[i] * primitiveBytes(i))
 				+ " handles " + statArrayHandle[i]);
 			}
 		    }
 		}
 	    });
 	    System.out.println("IbisSerializationOutputStream.STATS_OBJECTS enabled");
 	    statSendObjects = new java.util.Hashtable();
 	    statArrayCount = new int[PRIMITIVE_TYPES];
 	    statArrayHandle = new int[PRIMITIVE_TYPES];
 	    statArrayLength = new long[PRIMITIVE_TYPES];
 	} else {
 	    statSendObjects = null;
 	    statArrayCount = null;
 	    statArrayLength = null;
 	    statArrayHandle = null;
 	}
     }
 
     /**
      * The first free object handle.
      */
     private int next_handle;
 
     /**
      * Hash table for keeping references to objects already written.
      */
     private IbisHash references  = new IbisHash(2048);
     // private IbisHash references  = new IbisHash(65536);
     // private IbisHash references  = new IbisHash(131200);
     // private IbisHash references  = new IbisHash(256);
 
     /**
      * Remember when a reset must be sent out.
      */
     private boolean resetPending = false;
 
     /**
      * The first free type index.
      */
     private int next_type;
 
     /**
      * Hashtable for types already put on the stream.
      */
     private IbisHash types = new IbisHash();
 
     /**
      * There is a notion of a "current" object. This is needed when a
      * user-defined <code>writeObject</code> refers to
      * <code>defaultWriteObject</code> or to <code>putFields</code>.
      */
     private Object current_object;
 
     /**
      * There also is a notion of a "current" level.
      * The "level" of a serializable class is computed as follows:<ul>
      * <li> if its superclass is serializable: the level of the superclass + 1.
      * <li> if its superclass is not serializable: 1.
      * </ul>
      * This level implies a level at which an object can be seen. The "current"
      * level is the level at which <code>current_object</code> is being
      * processed.
      */
     private int current_level;
 
     /**
      * There also is the notion of a "current" <code>PutField</code>, needed for
      * the <code>writeFields</code> method.
      */
     private ImplPutField current_putfield;
 
     /**
      * The <code>current_object</code>, <code>current_level</code>,
      * and <code>current_putfield</code> are maintained in stacks, so that
      * they can be managed by IOGenerator-generated code.
      */
     private Object[] object_stack;
     private int[] level_stack;
     private ImplPutField[] putfield_stack;
     private int max_stack_size = 0;
     private int stack_size = 0;
 
     /**
      * Structure summarizing an array write.
      */
     private static final class ArrayDescriptor {
 	int		type;
 	boolean[]	booleanArray;
 	byte[]		byteArray;
 	char[]		charArray;
 	short[]		shortArray;
 	int[]		intArray;
 	long[]		longArray;
 	float[]		floatArray;
 	double[]	doubleArray;
 	int		offset;
 	int		len;
     }
 
     /**
      * Where the arrays to be written are collected.
      */
     private ArrayDescriptor[] 	array;
 
     /**
      * Index in the <code>array</code> array.
      */
     private int			array_index;
 
     /**
      * Collects all indices of the <code>_buffer</code> arrays.
      */
     private short[]	indices_short;
 
     /**
      * For each 
      */
     private boolean[] touched = new boolean[PRIMITIVE_TYPES];
 
     /**
      * Constructor with an <code>Accumulator</code>.
      * @param out		the underlying <code>Accumulator</code>
      * @exception IOException	gets thrown when an IO error occurs.
      */
     public IbisSerializationOutputStream(Accumulator out)
 							 throws IOException {
 	super(out);
 
 	types_clear();
 
 	next_type = PRIMITIVE_TYPES;
 	references.clear();
 	next_handle = CONTROL_HANDLES;
     }
 
     /**
      * Constructor, may be used when this class is sub-classed.
      */
     protected IbisSerializationOutputStream() throws IOException {
 
 	super();
 
 	types_clear();
 
 	next_type = PRIMITIVE_TYPES;
 	references.clear();
 	next_handle = CONTROL_HANDLES;
     }
 
     /**
      * {@inheritDoc}
      */
     public String serializationImplName() {
 	return "ibis";
     }
 
     /**
      * {@inheritDoc}
      */
     public void statistics() {
 	if (false) {
 	    System.err.print("IbisOutput: references -> ");
 	    references.statistics();
 	    System.err.print("IbisOutput: types      -> ");
 	    types.statistics();
 	}
     }
 
     /**
      * Initializes the type hash by adding arrays of primitive types.
      */
     private void types_clear() {
 	types.clear();
 	types.put(classBooleanArray, TYPE_BOOLEAN | TYPE_BIT);
 	types.put(classByteArray,    TYPE_BYTE | TYPE_BIT);
 	types.put(classCharArray,    TYPE_CHAR | TYPE_BIT);
 	types.put(classShortArray,   TYPE_SHORT | TYPE_BIT);
 	types.put(classIntArray,     TYPE_INT | TYPE_BIT);
 	types.put(classLongArray,    TYPE_LONG | TYPE_BIT);
 	types.put(classFloatArray,   TYPE_FLOAT | TYPE_BIT);
 	types.put(classDoubleArray,  TYPE_DOUBLE | TYPE_BIT);
 	next_type = PRIMITIVE_TYPES;
     }
 
     /**
      * {@inheritDoc}
      */
     public void reset() throws IOException {
 	if (next_handle > CONTROL_HANDLES) {
 	    if(DEBUG) {
 		dbPrint("reset: next handle = " + next_handle + ".");
 	    }
 	    references.clear();
 	    /* We cannot send out the reset immediately, because the
 	     * reader side only accepts a reset when it is expecting a
 	     * handle. So, instead, we remember that we need to send
 	     * out a reset, and send before sending the next handle.
 	     */
 	    resetPending = true;
 	    next_handle = CONTROL_HANDLES;
 	}
 	// types_clear();
 	// There is no need to clear the type table.
 	// It can be reused after a reset.
     }
 
     /* This is the data output / object output part */
 
     /**
      * {@inheritDoc}
      */
     public void writeUTF(String str) throws IOException {
 	// dbPrint("writeUTF: " + str);
 	startTimer();
 	if(str == null) {
 	    writeInt(-1);
 	    stopTimer();
 	    return;
 	}
 
 	if(DEBUG) {
 	    dbPrint("write UTF " + str);
 	}
 	int len = str.length();
 
 	//	writeInt(len);
 	//	writeArray(str.toCharArray(), 0, len);
 
 	byte[] b = new byte[3 * len];
 	int bn = 0;
 
 	for (int i = 0; i < len; i++) {
 	    char c = str.charAt(i);
 	    if (c > 0x0000 && c <= 0x007f) {
 		b[bn++] = (byte)c;
 	    } else if (c <= 0x07ff) {
 		b[bn++] = (byte)(0xc0 | (0x1f & (c >> 6)));
 		b[bn++] = (byte)(0x80 | (0x3f & c));
 	    } else {
 		b[bn++] = (byte)(0xe0 | (0x0f & (c >> 12)));
 		b[bn++] = (byte)(0x80 | (0x3f & (c >>  6)));
 		b[bn++] = (byte)(0x80 | (0x3f & c));
 	    }
 	}
 
 	writeInt(bn);
 	writeArray(b, 0, bn);
 	stopTimer();
     }
 
     /**
      * Called by IOGenerator-generated code to write a Class object to this
      * stream. For a Class object, only its name is written.
      * @param ref		the <code>Class</code> to be written
      * @exception IOException	gets thrown when an IO error occurs.
      */
     public void writeClass(Class ref) throws IOException {
 	startTimer();
 	if (ref == null) {
 	    writeHandle(NUL_HANDLE);
 	    stopTimer();
 	    return;
 	}
 	int hashCode = references.getHashCode(ref);
 	int handle = references.find(ref, hashCode);
 	if (handle == 0) {
 	    handle = next_handle++;
 	    references.put(ref, handle, hashCode);
 	    writeType(java.lang.Class.class);
 	    writeUTF(ref.getName());
 	} else {
 	    writeHandle(handle);
 	}
 	stopTimer();
     }
 
     /**
      * Sends out handles as normal int's. Also checks if we
      * need to send out a reset first.
      * @param v		the handle to be written
      * @exception IOException	gets thrown when an IO error occurs.
      */
     private void writeHandle(int v) throws IOException {
         if (resetPending) {
                 writeInt(RESET_HANDLE);
 		if (DEBUG) {
 		    dbPrint("wrote a RESET");
 		}
                 resetPending = false;
         }
 
         // treating handles as normal int's --N
         writeInt(v);
 	if (DEBUG) {
 	    dbPrint("wrote handle " + v);
 	}
     }
 
     /**
      * {@inheritDoc}
      */
     public void writeBytes(String s) throws IOException {
 
 	startTimer();
 	if (s != null) {
 	    byte[] bytes = s.getBytes();
 	    int len = bytes.length;
 	    writeInt(len);
 	    for (int i = 0; i < len; i++) {
 		writeByte(bytes[i]);
 	    }
 	}
 	stopTimer();
     }
 
     /**
      * {@inheritDoc}
      */
     public void writeChars(String s) throws IOException {
 
 	startTimer();
 	if (s != null) {
 	    int len = s.length();
 	    writeInt(len);
 	    for (int i = 0; i < len; i++) {
 		writeChar(s.charAt(i));
 	    }
 	}
 	stopTimer();
     }
 
     /**
      * {@inheritDoc}
      */
     public void writeArray(boolean[] ref, int off, int len) throws IOException {
 	startTimer();
 	if(writeArrayHeader(ref, classBooleanArray, len, false)) {
 	    writeArrayBoolean(ref, off, len);
 	}
 	stopTimer();
     }
 
     /**
      * {@inheritDoc}
      */
     public void writeArray(byte[] ref, int off, int len) throws IOException {
 	startTimer();
 	if(writeArrayHeader(ref, classByteArray, len, false)) {
 	    writeArrayByte(ref, off, len);
 	}
 	stopTimer();
     }
 
     /**
      * {@inheritDoc}
      */
     public void writeArray(short[] ref, int off, int len) throws IOException {
 	startTimer();
 	if(writeArrayHeader(ref, classShortArray, len, false)) {
 	    writeArrayShort(ref, off, len);
 	}
 	stopTimer();
     }
 
     /**
      * {@inheritDoc}
      */
     public void writeArray(char[] ref, int off, int len) throws IOException {
 	startTimer();
 	if(writeArrayHeader(ref, classCharArray, len, false)) {
 	    writeArrayChar(ref, off, len);
 	}
 	stopTimer();
     }
 
     /**
      * {@inheritDoc}
      */
     public void writeArray(int[] ref, int off, int len) throws IOException {
 	startTimer();
 	if(writeArrayHeader(ref, classIntArray, len, false)) {
 	    writeArrayInt(ref, off, len);
 	}
 	stopTimer();
     }
 
     /**
      * {@inheritDoc}
      */
     public void writeArray(long[] ref, int off, int len) throws IOException {
 	startTimer();
 	if(writeArrayHeader(ref, classLongArray, len, false)) {
 	    writeArrayLong(ref, off, len);
 	}
 	stopTimer();
     }
 
     /**
      * {@inheritDoc}
      */
     public void writeArray(float[] ref, int off, int len) throws IOException {
 	startTimer();
 	if(writeArrayHeader(ref, classFloatArray, len, false)) {
 	    writeArrayFloat(ref, off, len);
 	}
 	stopTimer();
     }
 
     /**
      * {@inheritDoc}
      */
     public void writeArray(double[] ref, int off, int len) throws IOException {
 	startTimer();
 	if(writeArrayHeader(ref, classDoubleArray, len, false)) {
 	    writeArrayDouble(ref, off, len);
 	}
 	stopTimer();
     }
 
     /**
      * {@inheritDoc}
      */
     public void writeArray(Object[] ref, int off, int len) throws IOException {
 	startTimer();
 	Class clazz = ref.getClass();
 	if (writeArrayHeader(ref, clazz, len, false)) {
 	    for (int i = off; i < off + len; i++) {
 		writeObjectOverride(ref[i]);
 	    }
 	}
 	stopTimer();
     }
 
     /**
      * Writes a type or a handle.
      * If <code>ref</code> has been written before, this method writes its
      * handle and returns <code>true</code>. If not, its type is written,
      * a new handle is associated with it, and <code>false</code> is returned.
      *
      * @param ref	the object that is going to be put on the stream
      * @param clazz	the <code>Class</code> representing the type
      * 			of <code>ref</code>
      * @exception IOException	gets thrown when an IO error occurs.
      */
     private boolean writeTypeHandle(Object ref, Class clazz)
 	    throws IOException
     {
 	int handle = references.lazyPut(ref, next_handle);
 
 	if (handle != next_handle) {
 	    writeHandle(handle);
 	    return true;
 	}
 
 	writeType(clazz);
 	next_handle++;
 
 	if (DEBUG) {
 	    dbPrint("writeTypeHandle: references[" + handle + "] = " +
 			(ref == null ? "null" : ref));
 	}
 
 	return false;
     }
 
     /**
      * Writes a handle or an array header, depending on wether a cycle
      * should be and was detected. If a cycle was detected, it returns
      * <code>false</code>, otherwise <code>true</code>.
      * The array header consists of a type and a length.
      * @param ref	    the array to be written
      * @param clazz	    the <code>Class</code> representing the array type
      * @param len	    the number of elements to be written
      * @param doCycleCheck  set when cycles should be detected
      * @exception IOException	gets thrown when an IO error occurs.
      * @return <code>true</code> if no cycle was or should be detected
      *  (so that the array should be written).
      */
     private boolean writeArrayHeader(Object ref,
 				     Class clazz,
 				     int len,
 				     boolean doCycleCheck)
 	    throws IOException
     {
 	if (ref == null) {
 	    writeHandle(NUL_HANDLE);
 	    return false;
 	}
 
 	if (doCycleCheck) {
 	    /* A complete array. Do cycle/duplicate detection */
 	    if (writeTypeHandle(ref, clazz)) {
 		return false;
 	    }
 	} else {
 	    writeType(clazz);
 	}
 
 	writeInt(len);
 
 	addStatSendArrayHandle(ref, len);
 
 	if (DEBUG) {
 	    dbPrint("writeArrayHeader " + clazz.getName() + " length = " + len);
 	}
 	return true;
     }
 
     /**
      * Writes an array, but possibly only a handle.
      * @param ref	  the array to be written
      * @param arrayClass  the <code>Class</code> representing the array type
      * @param unshared	  set when no cycle detection check shoud be done
      * @exception IOException	gets thrown when an IO error occurs.
      */
     private void writeArray(Object ref, Class arrayClass, boolean unshared)
 	    throws IOException
     {
 	String s = arrayClass.getName();
 	switch (s.charAt(1)) {
 	    case 'B': {
 		byte[] a = (byte[])ref;
 		int len = a.length;
 		if(writeArrayHeader(a, arrayClass, len, ! unshared)) {
 		    writeArrayByte(a, 0, len);
 		}
 		break;
 	    }
 	    case 'I': {
 		int[] a = (int[])ref;
 		int len = a.length;
 		if(writeArrayHeader(a, arrayClass, len, !unshared)) {
 		    writeArrayInt(a, 0, len);
 		}
 		break;
 	    }
 	    case 'Z': {
 		boolean[] a = (boolean[])ref;
 		int len = a.length;
 		if(writeArrayHeader(a, arrayClass, len, ! unshared)) {
 		    writeArrayBoolean(a, 0, len);
 		}
 		break;
 	    }
 	    case 'D': {
 		double[] a = (double[])ref;
 		int len = a.length;
 		if(writeArrayHeader(a, arrayClass, len, ! unshared)) {
 		    writeArrayDouble(a, 0, len);
 		}
 		break;
 	    }
 	    case 'C': {
 		char[] a = (char[])ref;
 		int len = a.length;
 		if(writeArrayHeader(a, arrayClass, len, ! unshared)) {
 		    writeArrayChar(a, 0, len);
 		}
 		break;
 	    }
 	    case 'S': {
 		short[] a = (short[])ref;
 		int len = a.length;
 		if(writeArrayHeader(a, arrayClass, len, ! unshared)) {
 		    writeArrayShort(a, 0, len);
 		}
 		break;
 	    }
 	    case 'J': {
 		long[] a = (long[])ref;
 		int len = a.length;
 		if(writeArrayHeader(a, arrayClass, len, ! unshared)) {
 		    writeArrayLong(a, 0, len);
 		}
 		break;
 	    }
 	    case 'F': {
 		float[] a = (float[])ref;
 		int len = a.length;
 		if(writeArrayHeader(a, arrayClass, len, ! unshared)) {
 		    writeArrayFloat(a, 0, len);
 		}
 		break;
 	    }
 	    default: {
 		Object[] a = (Object[])ref;
 		int len = a.length;
 		if(writeArrayHeader(a, arrayClass, len, ! unshared)) {
 		    for (int i = 0; i < len; i++) {
 			writeObjectOverride(a[i]);
 		    }
 		}
 	    }
 	}
     }
 
     /**
      * Adds the type represented by <code>clazz</code> to the type
      * table and returns its number.
      * @param clazz	represents the type to be added
      * @return		the type number.
      */
     private int newType(Class clazz) {
 	int type_number = next_type++;
 
 	type_number = (type_number | TYPE_BIT);
 	types.put(clazz, type_number);
 
 	return type_number;
     }
 
     /**
      * Writes a type number, and, when new, a type name to the output stream.
      * @param clazz		the clazz to be written.
      * @exception IOException	gets thrown when an IO error occurs.
      */
     private void writeType(Class clazz) throws IOException {
 	int type_number = types.find(clazz);
 
 	if (type_number != 0) {
 	    writeHandle(type_number);	// TYPE_BIT is set, receiver sees it
 
 	    if(DEBUG) {
 		dbPrint("wrote type number 0x" +
 			    Integer.toHexString(type_number));
 	    }
 	    return;
 	}
 
 	type_number = newType(clazz);
 	writeHandle(type_number);	// TYPE_BIT is set, receiver sees it
 	if(DEBUG) {
 	    dbPrint("wrote NEW type number 0x"
 		    + Integer.toHexString(type_number) +
 		    " type " + clazz.getName());
 	}
 	writeUTF(clazz.getName());
     }
 
     /**
      * Writes a (new or old) handle for object <code>ref</code> to the output
      * stream. Returns 1 if the object is new, -1 if not.
      * @param ref		the object whose handle is to be written
      * @exception IOException	gets thrown when an IO error occurs.
      * @return			1 if it is a new object, -1 if it is not.
      */
     public int writeKnownObjectHeader(Object ref) throws IOException {
 
 	if (ref == null) {
 	    writeHandle(NUL_HANDLE);
 	    return 0;
 	}
 
 	int handle = references.lazyPut(ref, next_handle);
 	if (handle == next_handle) {
 // System.err.write("+");
 	    Class clazz = ref.getClass();
 	    next_handle++;
 	    if(DEBUG) {
 		dbPrint("writeKnownObjectHeader -> writing NEW object, class = "
 			    + clazz.getName());
 	    }
 	    writeType(clazz);
 	    return 1;
 	}
 
 	if(DEBUG) {
 	    dbPrint("writeKnownObjectHeader -> writing OLD HANDLE " + handle);
 	}
 	writeHandle(handle);
 
 	return -1;
     }
 
     /**
      * Writes a (new or old) handle for array of primitives <code>ref</code>
      * to the output stream. Returns 1 if the object is new, -1 if not.
      * @param ref		the object whose handle is to be written
      * @param typehandle	the type number
      * @exception IOException	gets thrown when an IO error occurs.
      * @return			1 if it is a new object, -1 if it is not.
      */
     public int writeKnownArrayHeader(Object ref, int typehandle)
 	    throws IOException
     {
 	if (ref == null) {
 	    writeHandle(NUL_HANDLE);
 	    return 0;
 	}
 
 	int handle = references.lazyPut(ref, next_handle);
 	if (handle == next_handle) {
 // System.err.write("+");
 	    next_handle++;
 	    writeInt(typehandle | TYPE_BIT);
 	    return 1;
 	}
 
 	if(DEBUG) {
 	    dbPrint("writeKnownObjectHeader -> writing OLD HANDLE " + handle);
 	}
 	writeHandle(handle);
 
 	return -1;
     }
 
     /**
      * Writes the serializable fields of an object <code>ref</code> using the
      * type information <code>t</code>.
      *
      * @param t		the type info for object <code>ref</code>
      * @param ref	the object of which the fields are to be written
      *
      * @exception IOException		 when an IO error occurs
      * @exception IllegalAccessException when access to a field is denied.
      */
     private void alternativeDefaultWriteObject(AlternativeTypeInfo t,
 					       Object ref)
 	    throws IOException, IllegalAccessException
     {
 	int temp = 0;
 	int i;
 
 	if (DEBUG) {
 	    dbPrint("alternativeDefaultWriteObject, class = " +
 			t.clazz.getName());
 	}
 	for (i=0;i<t.double_count;i++) {
 	    writeDouble(t.serializable_fields[temp++].getDouble(ref));
 	}
 	for (i=0;i<t.long_count;i++) {
 	    writeLong(t.serializable_fields[temp++].getLong(ref));
 	}
 	for (i=0;i<t.float_count;i++) {
 	    writeFloat(t.serializable_fields[temp++].getFloat(ref));
 	}
 	for (i=0;i<t.int_count;i++) {
 	    writeInt(t.serializable_fields[temp++].getInt(ref));
 	}
 	for (i=0;i<t.short_count;i++) {
 	    writeShort(t.serializable_fields[temp++].getShort(ref));
 	}
 	for (i=0;i<t.char_count;i++) {
 	    writeChar(t.serializable_fields[temp++].getChar(ref));
 	}
 	for (i=0;i<t.byte_count;i++) {
 	    writeByte(t.serializable_fields[temp++].getByte(ref));
 	}
 	for (i=0;i<t.boolean_count;i++) {
 	    writeBoolean(t.serializable_fields[temp++].getBoolean(ref));
 	}
 	for (i=0;i<t.reference_count;i++) {
 	    writeObjectOverride(t.serializable_fields[temp++].get(ref));
 	}
     }
 
 
     /**
      * Serializes an object <code>ref</code> using the type information
      * <code>t</code>.
      *
      * @param t		the type info for object <code>ref</code>
      * @param ref	the object of which the fields are to be written
      *
      * @exception IOException		 when an IO error occurs
      * @exception IllegalAccessException when access to a field or
      * 					 <code>writeObject</code> method is
      * 					 denied.
      */
     private void alternativeWriteObject(AlternativeTypeInfo t, Object ref) 
 	    throws IOException, IllegalAccessException
     {
 	if (t.superSerializable) {
 	    alternativeWriteObject(t.alternativeSuperInfo, ref);
 	}
 
 	if (t.hasWriteObject) {
 	    current_level = t.level;
 	    try {
 		if (DEBUG) {
 		    dbPrint("invoking writeObject() of class " +
 				t.clazz.getName());
 		}
 		t.invokeWriteObject(ref, this);
 		if (DEBUG) {
 		    dbPrint("done with writeObject() of class " +
 				t.clazz.getName());
 		}
 	    } catch (java.lang.reflect.InvocationTargetException e) {
 		if (DEBUG) {
 		    dbPrint("Caught exception: " + e);
 		    e.printStackTrace();
 		    dbPrint("now rethrow as IllegalAccessException ...");
 		}
 		throw new IllegalAccessException("writeObject method: " + e);
 	    }
 	    return;
 	}
 
 	alternativeDefaultWriteObject(t, ref);
     }
 
     /**
      * Push the notions of <code>current_object</code>,
      * <code>current_level</code>, and <code>current_putfield</code> on
      * their stacks, and set new ones.
      *
      * @param ref	the new <code>current_object</code> notion
      * @param level	the new <code>current_level</code> notion
      */
     public void push_current_object(Object ref, int level) {
 	if (stack_size >= max_stack_size) {
 	    max_stack_size = 2 * max_stack_size + 10;
 	    Object[] new_o_stack = new Object[max_stack_size];
 	    int[] new_l_stack = new int[max_stack_size];
 	    ImplPutField[] new_p_stack = new ImplPutField[max_stack_size];
 	    for (int i = 0; i < stack_size; i++) {
 		new_o_stack[i] = object_stack[i];
 		new_l_stack[i] = level_stack[i];
 		new_p_stack[i] = putfield_stack[i];
 	    }
 	    object_stack = new_o_stack;
 	    level_stack = new_l_stack;
 	    putfield_stack = new_p_stack;
 	}
 	object_stack[stack_size] = current_object;
 	level_stack[stack_size] = current_level;
 	putfield_stack[stack_size] = current_putfield;
 	stack_size++;
 	current_object = ref;
 	current_level = level;
 	current_putfield = null;
     }
 
     /**
      * Pop the notions of <code>current_object</code>,
      * <code>current_level</code>, and <code>current_putfield</code> from
      * their stacks.
      */
     public void pop_current_object() {
 	stack_size--;
 	current_object = object_stack[stack_size];
 	current_level = level_stack[stack_size];
 	current_putfield = putfield_stack[stack_size];
     }
 
     /**
      * This method takes care of writing the serializable fields of the
      * parent object, and also those of its parent objects.
      * It gets called by IOGenerator-generated code when an object
      * has a superclass that is serializable but not Ibis serializable.
      *
      * @param ref	the object with a non-Ibis-serializable parent object
      * @param classname	the name of the superclass
      * @exception IOException	gets thrown on IO error
      */
     public void writeSerializableObject(Object ref, String classname)
 	    throws IOException
     {
 	AlternativeTypeInfo t;
 	try {
 	    t = AlternativeTypeInfo.getAlternativeTypeInfo(classname);
 	} catch(ClassNotFoundException e) {
 	    throw new SerializationError("Internal error", e);
 	}
 	try {
 	    push_current_object(ref, 0);
 	    alternativeWriteObject(t, ref);
 	    pop_current_object();
 	} catch (IllegalAccessException e) {
 	    if (DEBUG) {
 		dbPrint("Caught exception: " + e);
 		e.printStackTrace();
 		dbPrint("now rethrow as java.io.NotSerializableException ...");
 	    }
 	    throw new NotSerializableException("Serializable failed for : " +
 						classname);
 	}
     }
 
     /**
      * This method takes care of writing the serializable fields of the
      * parent object, and also those of its parent objects.
      *
      * @param ref	the object with a non-Ibis-serializable parent object
      * @param clazz	the superclass
      * @exception IOException	gets thrown on IO error
      */
     private void writeSerializableObject(Object ref, Class clazz)
 	    throws IOException
     {
 	AlternativeTypeInfo t = AlternativeTypeInfo.
 				    getAlternativeTypeInfo(clazz);
 	try {
 	    push_current_object(ref, 0);
 	    alternativeWriteObject(t, ref);
 	    pop_current_object();
 	} catch (IllegalAccessException e) {
 		if (DEBUG) {
 		    dbPrint("Caught exception: " + e);
 		    e.printStackTrace();
 		    dbPrint("now rethrow as java.io.NotSerializableException ...");
 		}
 	    throw new NotSerializableException("Serializable failed for : " +
 						clazz.getName());
 	}
     }
 
     /**
      * Writes a <code>String</code> object.
      * This is a special case, because strings are written as an UTF.
      *
      * @param ref		the string to be written
      * @exception IOException	gets thrown on IO error
      */
     public void writeString(String ref) throws IOException {
 	startTimer();
 	if (ref == null) {
 	    if (DEBUG) {
 		dbPrint("writeString: --> null");
 	    }
 	    writeHandle(NUL_HANDLE);
 	    stopTimer();
 	    return;
 	}
 
 	int handle = references.lazyPut(ref, next_handle);
 	if (handle == next_handle) {
 	    next_handle++;
 	    writeType(java.lang.String.class);
 	    if (DEBUG) {
 		dbPrint("writeString: " + ref);
 	    }
 	    writeUTF(ref);
 	} else {
 	    if (DEBUG) {
 		dbPrint("writeString: duplicate handle " + handle +
 			    " string = " + ref);
 	    }
 	    writeHandle(handle);
 	}
 	stopTimer();
     }
 
 
     private static void addStatSendObject(Object ref) {
 	if (STATS_OBJECTS) {
 	    Class clazz = ref.getClass();
 	    Integer n = (Integer)statSendObjects.get(clazz);
 	    if (n == null) {
 		n = new Integer(1);
 	    } else {
 		n = new Integer(n.intValue() + 1);
 	    }
 	    statSendObjects.put(clazz, n);
 	}
     }
 
 
     private static void addStatSendObjectHandle(Object ref) {
 	if (STATS_OBJECTS) {
 	    statObjectHandle++;
 	}
     }
 
 
     void addStatSendArray(Object ref, int type, int len) {
 	if (STATS_OBJECTS) {
 	    addStatSendObject(ref);
 	    statArrayCount[type]++;
 	    statArrayLength[type] += len;
 	}
     }
 
 
     private static void addStatSendArrayHandle(Object ref, int len) {
 	if (STATS_OBJECTS) {
 	    Class arrayClass = ref.getClass();
 	    int type = arrayClassType(arrayClass);
 	    if (type == -1) {
 		statObjectHandle++;
 	    } else {
 		statArrayHandle[type]++;
 	    }
 	}
     }
 
 
     /**
      * Write objects and arrays.
      * Duplicates are deteced when this call is used.
      * The replacement mechanism is implemented here as well.
      * We cannot redefine <code>writeObject</code>, because it is final in
      * <code>ObjectOutputStream</code>. The trick for Ibis serialization is
      * to have the <code>ObjectOutputStream</code> be initialized with its
      * parameter-less constructor. This will cause its <code>writeObject</code>
      * to call <code>writeObjectOverride</code> instead of doing its own thing.
      *
      * @param ref the object to be written
      * @exception <code>IOException</code> is thrown when an IO error occurs.
     */
     public void writeObjectOverride(Object ref) throws IOException {
 	/*
 	 * ref < 0:	type
 	 * ref = 0:	null ptr
 	 * ref > 0:	handle
 	 */
 
 	startTimer();
 	if (ref == null) {
 	    writeHandle(NUL_HANDLE);
 	    stopTimer();
 	    return;
 	}
 	/* TODO: deal with writeReplace! This should be done before
 	   looking up the handle. If we don't want to do runtime
 	   inspection, this should probably be handled somehow in
 	   IOGenerator.
 	   Note that the needed info is available in AlternativeTypeInfo,
 	   but we don't want to use that when we have ibis.io.Serializable.
 	   */
 
 	if (replacer != null) {
 	    ref = replacer.replace(ref);
 	}
 	int hashCode = references.getHashCode(ref);
 	int handle = references.find(ref, hashCode);
 
 	if (handle == 0) {
 	    Class clazz = ref.getClass();
 	    if(DEBUG) {
 		dbPrint("start writeObject of class " + clazz.getName() +
 			    " handle = " + next_handle);
 	    }
 
 	    if (clazz.isArray()) {
 		writeArray(ref, clazz, false);
 	    } else {
 		handle = next_handle++;
 		references.put(ref, handle, hashCode);
 		writeType(clazz);
 		if (clazz == java.lang.String.class) {
 		    /* EEK this is not nice !! */
 		    writeUTF((String)ref);
 		} else if (clazz == java.lang.Class.class) {
 		    /* EEK this is not nice !! */
 		    writeUTF(((Class)ref).getName());
 		} else if (IbisSerializationInputStream.
 				    isIbisSerializable(clazz))
 		{
 		    ((ibis.io.Serializable)ref).generated_WriteObject(this);
 		} else if (ref instanceof java.io.Externalizable) {
 		    push_current_object(ref, 0);
 		    ((java.io.Externalizable) ref).writeExternal(this);
 		    pop_current_object();
 		} else if (ref instanceof java.io.Serializable) {
 		    writeSerializableObject(ref, clazz);
 		} else { 
 		    throw new NotSerializableException("Not Serializable : " +
 							clazz.getName());
 		}
 
 		addStatSendObject(ref);
 	    }
 	    if (DEBUG) {
 		dbPrint("finished writeObject of class " + clazz.getName());
 	    }
 	} else {
 	    if(DEBUG) {
 		dbPrint("writeObject: duplicate handle " + handle +
 			    " class = " + ref.getClass());
 	    }
 	    writeHandle(handle);
 
 	    addStatSendObjectHandle(ref);
 	}
 	stopTimer();
     }
 
     /**
      * {@inheritDoc}
      */
     public void writeUnshared(Object ref) throws IOException {
 	if (ref == null) {
 	    writeHandle(NUL_HANDLE);
 	    return;
 	}
 	/* TODO: deal with writeReplace! This should be done before
 	   looking up the handle. If we don't want to do runtime
 	   inspection, this should probably be handled somehow in
 	   IOGenerator.
 	   Note that the needed info is available in AlternativeTypeInfo,
 	   but we don't want to use that when we have ibis.io.Serializable.
 	*/
 	Class clazz = ref.getClass();
 	if(DEBUG) {
 	    dbPrint("start writeUnshared of class " + clazz.getName() +
 			" handle = " + next_handle);
 	}
 
 	if (clazz.isArray()) {
 	    writeArray(ref, clazz, true);
 	} else {
 	    writeType(clazz);
 	    if (clazz == java.lang.String.class) {
 		/* EEK this is not nice !! */
 		writeUTF((String)ref);
 	    } else if (clazz == java.lang.Class.class) {
 		/* EEK this is not nice !! */
 		writeUTF(((Class)ref).getName());
 	    } else if (ref instanceof java.io.Externalizable) {
 		push_current_object(ref, 0);
 		((java.io.Externalizable) ref).writeExternal(this);
 		pop_current_object();
 	    } else if (IbisSerializationInputStream.isIbisSerializable(clazz)) {
 		((ibis.io.Serializable)ref).generated_WriteObject(this);
 	    } else if (ref instanceof java.io.Serializable) {
 		writeSerializableObject(ref, clazz);
 	    } else {
 		throw new RuntimeException("Not Serializable : " +
 					    clazz.getName());
 	    }
 	}
 	if(DEBUG) {
 	    dbPrint("finished writeUnshared of class " + clazz.getName() +
 			" handle = " + next_handle);
 	}
     }
 
     /**
      * {@inheritDoc}
      */
     public void useProtocolVersion(int version) {
 	/* ignored. */
     }
 
     /**
      * {@inheritDoc}
      */
     protected void writeStreamHeader() {
 	/* ignored. */
     }
 
     /**
      * {@inheritDoc}
      */
     protected void writeClassDescriptor(ObjectStreamClass desc) {
 	/* ignored */
     }
 
     /* annotateClass does not have to be redefined: it is empty in the
        ObjectOutputStream implementation.
     */
 
     /**
      * {@inheritDoc}
      */
     public void writeFields() throws IOException {
 	if (current_putfield == null) {
 	    throw new NotActiveException("no PutField object");
 	}
 	current_putfield.writeFields();
     }
 
     /**
      * {@inheritDoc}
      */
     public PutField putFields() throws IOException {
 	if (current_putfield == null) {
 	    if (current_object == null) {
 		throw new NotActiveException("not in writeObject");
 	    }
 	    Class clazz = current_object.getClass();
 	    AlternativeTypeInfo t = AlternativeTypeInfo.
 					getAlternativeTypeInfo(clazz);
 	    current_putfield = new ImplPutField(t);
 	}
 	return current_putfield;
     }
 
     /**
      * The Ibis serialization implementation of <code>PutField</code>.
      */
     private class ImplPutField extends PutField {
 	private double[]  doubles;
 	private long[]	  longs;
 	private int[]	  ints;
 	private float[]   floats;
 	private short[]   shorts;
 	private char[]    chars;
 	private byte[]	  bytes;
 	private boolean[] booleans;
 	private Object[]  references;
 	private AlternativeTypeInfo t;
 
 	ImplPutField(AlternativeTypeInfo t) {
 	    doubles = new double[t.double_count];
 	    longs = new long[t.long_count];
 	    ints = new int[t.int_count];
 	    shorts = new short[t.short_count];
 	    floats = new float[t.float_count];
 	    chars = new char[t.char_count];
 	    bytes = new byte[t.byte_count];
 	    booleans = new boolean[t.boolean_count];
 	    references = new Object[t.reference_count];
 	    this.t = t;
 	}
 
 	public void put(String name, boolean value)
 	    throws IllegalArgumentException {
 	    booleans[t.getOffset(name, Boolean.TYPE)] = value;
 	}
 
 	public void put(String name, char value)
 	    throws IllegalArgumentException {
 	    chars[t.getOffset(name, Character.TYPE)] = value;
 	}
 
 	public void put(String name, byte value)
 	    throws IllegalArgumentException {
 	    bytes[t.getOffset(name, Byte.TYPE)] = value;
 	}
 
 	public void put(String name, short value)
 	    throws IllegalArgumentException {
 	    shorts[t.getOffset(name, Short.TYPE)] = value;
 	}
 
 	public void put(String name, int value)
 	    throws IllegalArgumentException {
 	    ints[t.getOffset(name, Integer.TYPE)] = value;
 	}
 
 	public void put(String name, long value)
 	    throws IllegalArgumentException {
 	    longs[t.getOffset(name, Long.TYPE)] = value;
 	}
 
 	public void put(String name, float value)
 	    throws IllegalArgumentException {
 	    floats[t.getOffset(name, Float.TYPE)] = value;
 	}
 
 	public void put(String name, double value)
 	    throws IllegalArgumentException {
 	    doubles[t.getOffset(name, Double.TYPE)] = value;
 	}
 
 	public void put(String name, Object value) {
 	    references[t.getOffset(name, Object.class)] = value;
 	}
 
 	public void write(ObjectOutput o) throws IOException {
 	    for (int i = 0; i < t.double_count; i++) {
 		o.writeDouble(doubles[i]);
 	    }
 	    for (int i = 0; i < t.float_count; i++) {
 		o.writeFloat(floats[i]);
 	    }
 	    for (int i = 0; i < t.long_count; i++) {
 		o.writeLong(longs[i]);
 	    }
 	    for (int i = 0; i < t.int_count; i++) {
 		o.writeInt(ints[i]);
 	    }
 	    for (int i = 0; i < t.short_count; i++) {
 		o.writeShort(shorts[i]);
 	    }
 	    for (int i = 0; i < t.char_count; i++) {
 		o.writeChar(chars[i]);
 	    }
 	    for (int i = 0; i < t.byte_count; i++) {
 		o.writeByte(bytes[i]);
 	    }
 	    for (int i = 0; i < t.boolean_count; i++) {
 		o.writeBoolean(booleans[i]);
 	    }
 	    for (int i = 0; i < t.reference_count; i++) {
 		o.writeObject(references[i]);
 	    }
 	}
 
 
 	void writeFields() throws IOException {
 	    for (int i = 0; i < t.double_count; i++) {
 		writeDouble(doubles[i]);
 	    }
 	    for (int i = 0; i < t.float_count; i++) {
 		writeFloat(floats[i]);
 	    }
 	    for (int i = 0; i < t.long_count; i++) {
 		writeLong(longs[i]);
 	    }
 	    for (int i = 0; i < t.int_count; i++) {
 		writeInt(ints[i]);
 	    }
 	    for (int i = 0; i < t.short_count; i++) {
 		writeShort(shorts[i]);
 	    }
 	    for (int i = 0; i < t.char_count; i++) {
 		writeChar(chars[i]);
 	    }
 	    for (int i = 0; i < t.byte_count; i++) {
 		writeByte(bytes[i]);
 	    }
 	    for (int i = 0; i < t.boolean_count; i++) {
 		writeBoolean(booleans[i]);
 	    }
 	    for (int i = 0; i < t.reference_count; i++) {
 		writeObjectOverride(references[i]);
 	    }
 	}
     }
 
     /**
      * This method writes the serializable fields of object <code>ref</code>
      * at the level indicated by <code>depth</code>.
      * (see the explanation at the declaration of the
      * <code>current_level</code> field).
      * It gets called from IOGenerator-generated code, when a parent object
      * is serializable but not Ibis serializable.
      *
      * @param ref	the object of which serializable fields must be written
      * @param depth	an indication of the current "view" of the object
      * @exception IOException	gets thrown when an IO error occurs.
      */
     public void defaultWriteSerializableObject(Object ref, int depth)
 	    throws IOException
     {
 	Class clazz = ref.getClass();
 	AlternativeTypeInfo t = AlternativeTypeInfo.
 				    getAlternativeTypeInfo(clazz);
 
 	/*  Find the type info corresponding to the current invocation.
 	    See the invokeWriteObject invocation in alternativeWriteObject.
 	    */
 	while (t.level > depth) {
 	    t = t.alternativeSuperInfo;
 	}
 	try {
 	    alternativeDefaultWriteObject(t, ref);
 	} catch(IllegalAccessException e) {
 		if (DEBUG) {
 		    dbPrint("Caught exception: " + e);
 		    e.printStackTrace();
 		    dbPrint("now rethrow as NotSerializableException ...");
 		}
 	    throw new NotSerializableException("illegal access" + e);
 	}
     }
 
     /**
      * {@inheritDoc}
      */
     public void defaultWriteObject() throws IOException, NotActiveException {
 	if (current_object == null) {
 	    throw new NotActiveException("defaultWriteObject: no object");
 	}
 
 	Object ref = current_object;
 	Class clazz = ref.getClass();
 
 	if (IbisSerializationInputStream.isIbisSerializable(clazz)) {
 	    /* Note that this will take the generated_DefaultWriteObject of the
 	       dynamic type of ref. The current_level variable actually
 	       indicates which instance of generated_DefaultWriteObject 
 	       should do some work.
 	    */
 	    if (DEBUG) {
 		dbPrint("generated_DefaultWriteObject, class = " +
 			    clazz.getName() + ", level = " + current_level);
 	    }
 	    ((ibis.io.Serializable)ref).
 		    generated_DefaultWriteObject(this, current_level);
 	} else if (ref instanceof java.io.Serializable) {
 	    AlternativeTypeInfo t = AlternativeTypeInfo.
 					getAlternativeTypeInfo(clazz);
 
 	    /*	Find the type info corresponding to the current invocation.
 		See the invokeWriteObject invocation in alternativeWriteObject.
 		*/
 	    while (t.level > current_level) {
 		t = t.alternativeSuperInfo;
 	    }
 	    try {
 		alternativeDefaultWriteObject(t, ref);
 	    } catch(IllegalAccessException e) {
 		if (DEBUG) {
 		    dbPrint("Caught exception: " + e);
 		    e.printStackTrace();
 		    dbPrint("now rethrow as NotSerializableException ...");
 		}
 		throw new NotSerializableException("illegal access" + e);
 	    }
 	} else { 
 	    throw new NotSerializableException("Not Serializable : " +
 						    clazz.getName());
 	}
     }
 }
