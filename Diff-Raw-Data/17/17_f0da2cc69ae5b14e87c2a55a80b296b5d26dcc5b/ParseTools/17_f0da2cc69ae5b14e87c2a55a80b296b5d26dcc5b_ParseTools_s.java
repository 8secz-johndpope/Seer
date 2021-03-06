 package org.mvel.util;
 
 import org.mvel.*;
 import static org.mvel.AbstractParser.getCurrentThreadParserContext;
 import static org.mvel.DataConversion.canConvert;
 import org.mvel.integration.ResolverTools;
 import org.mvel.integration.VariableResolverFactory;
 import org.mvel.integration.impl.ClassImportResolverFactory;
 import org.mvel.integration.impl.LocalVariableResolverFactory;
 import org.mvel.integration.impl.StaticMethodImportResolverFactory;
 import org.mvel.math.MathProcessor;
 
 import java.io.File;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.io.Serializable;
 import static java.lang.Character.isWhitespace;
 import static java.lang.Class.forName;
 import static java.lang.Double.parseDouble;
 import static java.lang.String.valueOf;
 import static java.lang.System.arraycopy;
 import java.lang.reflect.Constructor;
 import java.lang.reflect.Method;
 import java.math.BigDecimal;
 import java.math.BigInteger;
 import java.util.*;
 
 
 /**
  * This class contains much of the actual parsing code used by the core parser.  
  */
 public class ParseTools {
     public static final Object[] EMPTY_OBJ_ARR = new Object[0];
     public static final MathProcessor MATH_PROCESSOR;
     public static final boolean JDK_14_COMPATIBILITY;
 
     static {
         try {
             double version = parseDouble(System.getProperty("java.version").substring(0, 3));
             if (version == 1.4) {
                 MATH_PROCESSOR = (MathProcessor) forName("org.mvel.math.JDK14CompatabilityMath").newInstance();
                 JDK_14_COMPATIBILITY = true;
             }
             else if (version > 1.4) {
                 MATH_PROCESSOR = (MathProcessor) forName("org.mvel.math.IEEEFloatingPointMath").newInstance();
                 JDK_14_COMPATIBILITY = false;
             }
             else {
                 throw new RuntimeException("unsupported java version: " + version);
             }
         }
         catch (RuntimeException e) {
             throw e;
         }
         catch (Exception e) {
             throw new RuntimeException("unable to initialize math processor", e);
         }
 
     }
 
     public static String[] parseMethodOrConstructor(char[] parm) {
         int start = -1;
         for (int i = 0; i < parm.length; i++) {
             if (parm[i] == '(') {
                 start = ++i;
                 break;
             }
         }
         if (start != -1) {
             start--;
             return parseParameterList(parm, start + 1, balancedCapture(parm, start, '(') - start - 1);
         }
 
         return null;
     }
 
     public static String[] parseParameterList(char[] parm, int offset, int length) {
         List<String> list = new LinkedList<String>();
 
         if (length == -1)
             length = parm.length;
 
         int adepth = 0;
 
         int start = offset;
         int i = offset;
         int end = i + length;
 
         for (; i < end; i++) {
             switch (parm[i]) {
                 case'(':
                     int depth = 1;
                     while (i++ < length - 1 && depth != 0) {
                         switch (parm[i]) {
                             case'(':
                                 depth++;
                                 continue;
                             case')':
                                 depth--;
                                 continue;
                             case'\'':
                                 i = captureStringLiteral('\'', parm, i, parm.length);
                                 continue;
                             case'"':
                                 i = captureStringLiteral('"', parm, i, parm.length);
 
                         }
                     }
                     i--;
                     continue;
 
                 case'[':
                 case'{':
                     adepth++;
 //                    if (adepth++ == 0)
 //                        start = i;
                     
                     continue;
 
                 case']':
                 case'}':
                     if (--adepth == 0) {
                         list.add(new String(parm, start, i - start + 1));
 
                         while (isWhitespace(parm[i]))
                             i++;
 
                         start = i + 1;
                     }
                     continue;
 
                 case'\'':
                     i = captureStringLiteral('\'', parm, i, parm.length);
                     continue;
 
 
                 case'"':
                     i = captureStringLiteral('"', parm, i, parm.length);
                     continue;
 
                 case',':
                     if (adepth != 0)
                         continue;
 
                     if (i > start) {
                         while (isWhitespace(parm[start]))
                             start++;
 
                         list.add(new String(parm, start, i - start));
                     }
 
                     while (isWhitespace(parm[i]))
                         i++;
 
                     start = i + 1;
             }
         }
 
         if (start < (length + offset) && i > start) {
             String s = new String(parm, start, i - start).trim();
             if (s.length() > 0)
                 list.add(s);
         }
         else if (list.size() == 0) {
             String s = new String(parm, start, length).trim();
             if (s.length() > 0)
                 list.add(s);
         }
 
         return list.toArray(new String[list.size()]);
     }
 
     private static Map<String, Map<Integer, Method>> RESOLVED_METH_CACHE = new WeakHashMap<String, Map<Integer, Method>>(10);
 

     public static Method getBestCandidate(Object[] arguments, String method, Method[] methods) {
         Class[] targetParms = new Class[arguments.length];
         for (int i = 0; i < arguments.length; i++) {
             targetParms[i] = arguments[i] != null ? arguments[i].getClass() : Object.class;
         }
         return getBestCandidate(targetParms, method, methods);
     }
 
 
     public static Method getBestCandidate(Class[] arguments, String method, Method[] methods) {
         if (methods.length == 0) {
             return null;
         }
         Class[] parmTypes;
         Method bestCandidate = null;
         int bestScore = 0;
         int score = 0;
 
         Integer hash = createClassSignatureHash(methods[0].getDeclaringClass(), arguments);
 
         if (RESOLVED_METH_CACHE.containsKey(method) && RESOLVED_METH_CACHE.get(method).containsKey(hash)) {
             return RESOLVED_METH_CACHE.get(method).get(hash);
         }
 
         for (Method meth : methods) {
             if (method.equals(meth.getName())) {
                 if ((parmTypes = meth.getParameterTypes()).length != arguments.length)
                     continue;
                 else if (arguments.length == 0 && parmTypes.length == 0) {
                     bestCandidate = meth;
                     break;
                 }
 
                 for (int i = 0; i < arguments.length; i++) {
                     if (parmTypes[i] == arguments[i]) {
                         score += 5;
                     }
                     else if (parmTypes[i].isPrimitive() && boxPrimitive(parmTypes[i]) == arguments[i]) {
                         score += 4;
                     }
                     else if (arguments[i].isPrimitive() && unboxPrimitive(arguments[i]) == parmTypes[i]) {
                         score += 4;
                     }
                     else if (isNumericallyCoercible(arguments[i], parmTypes[i])) {
                         score += 3;
                     }
                     else if (parmTypes[i].isAssignableFrom(arguments[i])) {
                         score += 2;
                     }
                     else if (canConvert(parmTypes[i], arguments[i]) || arguments[i] == Object.class) {
                         score += 1;
                     }
                     else {
                         score = 0;
                         break;
                     }
                 }
 
                 if (score != 0 && score > bestScore) {
                     bestCandidate = meth;
                     bestScore = score;
                 }
                 score = 0;
             }
         }
 
         if (bestCandidate != null) {
             if (!RESOLVED_METH_CACHE.containsKey(method))
                 RESOLVED_METH_CACHE.put(method, new WeakHashMap<Integer, Method>());
 
             RESOLVED_METH_CACHE.get(method).put(hash, bestCandidate);
         }
 
         return bestCandidate;
     }
 
     public static Method getWidenedTarget(Method method) {
         Class cls = method.getDeclaringClass();
         Method m = method;
         Class[] args = method.getParameterTypes();
         String name = method.getName();
 
         do {
             for (Class iface : cls.getInterfaces()) {
                 m = getBestCandidate(args, name, iface.getMethods());
                 if (m != null && m.getDeclaringClass().getSuperclass() != null) {
                     cls = m.getDeclaringClass();
                 }
             }
         }
         while ((cls = cls.getSuperclass()) != null);
 
         return m != null ? m : method;
     }
 
     private static Map<Class, Map<Integer, Constructor>> RESOLVED_CONST_CACHE = new WeakHashMap<Class, Map<Integer, Constructor>>(10);
 
     private static Map<Constructor, Class[]> CONSTRUCTOR_PARMS_CACHE = new WeakHashMap<Constructor, Class[]>(10);
 
     private static Class[] getConstructors(Constructor cns) {
         if (CONSTRUCTOR_PARMS_CACHE.containsKey(cns))
             return CONSTRUCTOR_PARMS_CACHE.get(cns);
         else {
             Class[] c = cns.getParameterTypes();
             CONSTRUCTOR_PARMS_CACHE.put(cns, c);
             return c;
         }
     }
 
     public static Constructor getBestConstructorCanadidate(Object[] arguments, Class cls) {
         Class[] parmTypes;
         Constructor bestCandidate = null;
         int bestScore = 0;
         int score = 0;
 
         Class[] targetParms = new Class[arguments.length];
 
         for (int i = 0; i < arguments.length; i++)
             targetParms[i] = arguments[i] != null ? arguments[i].getClass() : Object.class;
 
         Integer hash = createClassSignatureHash(cls, targetParms);
 
         if (RESOLVED_CONST_CACHE.containsKey(cls) && RESOLVED_CONST_CACHE.get(cls).containsKey(hash))
             return RESOLVED_CONST_CACHE.get(cls).get(hash);
 
         for (Constructor construct : getConstructors(cls)) {
             if ((parmTypes = getConstructors(construct)).length != arguments.length)
                 continue;
             else if (arguments.length == 0 && parmTypes.length == 0)
                 return construct;
 
             for (int i = 0; i < arguments.length; i++) {
                 if (parmTypes[i] == targetParms[i]) {
                     score += 5;
                 }
                 else if (parmTypes[i].isPrimitive() && boxPrimitive(parmTypes[i]) == targetParms[i]) {
                     score += 4;
                 }
                 else if (targetParms[i].isPrimitive() && unboxPrimitive(targetParms[i]) == parmTypes[i]) {
                     score += 4;
                 }
                 else if (isNumericallyCoercible(targetParms[i], parmTypes[i])) {
                     score += 3;
                 }
                 else if (parmTypes[i].isAssignableFrom(targetParms[i])) {
                     score += 2;
                 }
                 else if (canConvert(parmTypes[i], targetParms[i])) {
                     score += 1;
                 }
                 else {
                     score = 0;
                     break;
                 }
             }
 
             if (score != 0 && score > bestScore) {
                 bestCandidate = construct;
                 bestScore = score;
             }
             score = 0;
 
         }
 
         if (bestCandidate != null) {
             if (!RESOLVED_CONST_CACHE.containsKey(cls))
                 RESOLVED_CONST_CACHE.put(cls, new WeakHashMap<Integer, Constructor>());
 
             RESOLVED_CONST_CACHE.get(cls).put(hash, bestCandidate);
         }
 
         return bestCandidate;
     }
 
     private static Map<String, Class> CLASS_RESOLVER_CACHE = new WeakHashMap<String, Class>(10);
 
     private static Map<Class, Constructor[]> CLASS_CONSTRUCTOR_CACHE = new WeakHashMap<Class, Constructor[]>(10);
 
     public static Class createClassSafe(String className) {
         try {
             return createClass(className);
         }
         catch (ClassNotFoundException e) {
             return null;
         }
     }
 
     public static Class createClass(String className) throws ClassNotFoundException {
         if (CLASS_RESOLVER_CACHE.containsKey(className))
             return CLASS_RESOLVER_CACHE.get(className);
         else {
             Class cls = Class.forName(className);
             CLASS_RESOLVER_CACHE.put(className, cls);
             return cls;
         }
     }
 
     public static Constructor[] getConstructors(Class cls) {
         if (CLASS_CONSTRUCTOR_CACHE.containsKey(cls))
             return CLASS_CONSTRUCTOR_CACHE.get(cls);
         else {
             Constructor[] cns = cls.getConstructors();
             CLASS_CONSTRUCTOR_CACHE.put(cls, cns);
             return cns;
         }
     }
 
 
     public static String[] captureContructorAndResidual(String token) {
         char[] cs = token.toCharArray();
 
         int depth = 0;
 
         for (int i = 0; i < cs.length; i++) {
             switch (cs[i]) {
                 case'(':
                     depth++;
                     continue;
                 case')':
                     if (1 == depth--) {
                         return new String[]{new String(cs, 0, ++i), new String(cs, i, cs.length - i)};
                     }
             }
         }
         return new String[]{token};
     }
 
     public static String[] captureContructorAndResidual(char[] cs) {
         int depth = 0;
         for (int i = 0; i < cs.length; i++) {
             switch (cs[i]) {
                 case'(':
                     depth++;
                     continue;
                 case')':
                     if (1 == depth--) {
                         return new String[]{new String(cs, 0, ++i), new String(cs, i, cs.length - i)};
                     }
             }
         }
         return new String[]{new String(cs)};
     }
 
     public static Class boxPrimitive(Class cls) {
         if (cls == int.class || cls == Integer.class) {
             return Integer.class;
         }
         else if (cls == int[].class || cls == Integer[].class) {
             return Integer[].class;
         }
         else if (cls == long.class || cls == Long.class) {
             return Long.class;
         }
         else if (cls == long[].class || cls == Long[].class) {
             return Long[].class;
         }
         else if (cls == short.class || cls == Short.class) {
             return Short.class;
         }
         else if (cls == short[].class || cls == Short[].class) {
             return Short[].class;
         }
         else if (cls == double.class || cls == Double.class) {
             return Double.class;
         }
         else if (cls == double[].class || cls == Double[].class) {
             return Double[].class;
         }
         else if (cls == float.class || cls == Float.class) {
             return Float.class;
         }
         else if (cls == float[].class || cls == Float[].class) {
             return Float[].class;
         }
         else if (cls == boolean.class || cls == Boolean.class) {
             return Boolean.class;
         }
         else if (cls == boolean[].class || cls == Boolean[].class) {
             return Boolean[].class;
         }
         else if (cls == byte.class || cls == Byte.class) {
             return Byte.class;
         }
         else if (cls == byte[].class || cls == Byte[].class) {
             return Byte[].class;
         }
 
         return null;
     }
 
     public static Class unboxPrimitive(Class cls) {
         if (cls == Integer.class || cls == int.class) {
             return int.class;
         }
         else if (cls == Integer[].class || cls == int[].class) {
             return int[].class;
         }
         else if (cls == Long.class || cls == long.class) {
             return long.class;
         }
         else if (cls == Long[].class || cls == long[].class) {
             return long[].class;
         }
         else if (cls == Short.class || cls == short.class) {
             return short.class;
         }
         else if (cls == Short[].class || cls == short[].class) {
             return short[].class;
         }
         else if (cls == Double.class || cls == double.class) {
             return double.class;
         }
         else if (cls == Double[].class || cls == double[].class) {
             return double[].class;
         }
         else if (cls == Float.class || cls == float.class) {
             return float.class;
         }
         else if (cls == Float[].class || cls == float[].class) {
             return float[].class;
         }
         else if (cls == Boolean.class || cls == boolean.class) {
             return boolean.class;
         }
         else if (cls == Boolean[].class || cls == boolean[].class) {
             return boolean[].class;
         }
         else if (cls == Byte.class || cls == byte.class) {
             return byte.class;
         }
         else if (cls == Byte[].class || cls == byte[].class) {
             return byte[].class;
         }
 
         return null;
     }
 
     public static boolean containsCheck(Object compareTo, Object compareTest) {
         if (compareTo == null)
             return false;
         else if (compareTo instanceof String)
             // @todo use String.contains once we move to jdk1.5
             return ((String) compareTo).indexOf(valueOf(compareTest)) > -1;
         else if (compareTo instanceof Collection)
             return ((Collection) compareTo).contains(compareTest);
         else if (compareTo instanceof Map)
             return ((Map) compareTo).containsKey(compareTest);
         else if (compareTo.getClass().isArray()) {
             for (Object o : ((Object[]) compareTo)) {
                 if (compareTest == null && o == null)
                     return true;
                 else if (o != null && o.equals(compareTest))
                     return true;
             }
         }
         return false;
     }
 
     public static int createClassSignatureHash(Class declaring, Class[] sig) {
         int hash = 0;
         for (Class cls : sig) {
             if (cls != null)
                 hash += cls.hashCode();
         }
 
         return hash + sig.length + declaring.hashCode();
     }
 
     public static char handleEscapeSequence(char escapedChar) {
         switch (escapedChar) {
             case'\\':
                 return '\\';
             case't':
                 return '\t';
             case'r':
                 return '\r';
             case'n':
                 return '\n';
             case'\'':
                 return '\'';
             case'"':
                 return '"';
             default:
                 throw new ParseException("illegal escape sequence: " + escapedChar);
         }
     }
 
     public static char[] createShortFormOperativeAssignment(String name, char[] statement, int operation) {
         if (operation == -1) {
             return statement;
         }
 
         char[] stmt;
         char op = 0;
         switch (operation) {
             case Operator.ADD:
                 op = '+';
                 break;
             case Operator.SUB:
                 op = '-';
                 break;
             case Operator.MULT:
                 op = '*';
                 break;
             case Operator.DIV:
                 op = '/';
                 break;
 
             case Operator.BW_AND:
                 op = '&';
                 break;
             case Operator.BW_OR:
                 op = '|';
                 break;
         }
 
         arraycopy(name.toCharArray(), 0, (stmt = new char[name.length() + statement.length + 1]), 0, name.length());
         stmt[name.length()] = op;
         arraycopy(statement, 0, stmt, name.length() + 1, statement.length);
 
         return stmt;
     }
 
     public static VariableResolverFactory finalLocalVariableFactory(VariableResolverFactory factory) {
         VariableResolverFactory v = factory;
         while (v != null) {
             if (v instanceof LocalVariableResolverFactory) {
                 return v;
             }
 
             v = v.getNextFactory();
         }
 
         if (factory == null) {
             throw new OptimizationFailure("unable to assign variables.  no variable resolver factory available.");
         }
         else {
             return new LocalVariableResolverFactory(new HashMap<String, Object>()).setNextFactory(factory);
         }
     }
 
     public static ClassImportResolverFactory findClassImportResolverFactory(VariableResolverFactory factory) {
         VariableResolverFactory v = factory;
         while (v != null) {
             if (v instanceof ClassImportResolverFactory) {
                 return (ClassImportResolverFactory) v;
             }
             v = v.getNextFactory();
         }
 
         if (factory == null) {
             throw new OptimizationFailure("unable to import classes.  no variable resolver factory available.");
         }
         else {
             return ResolverTools.insertFactory(factory, new ClassImportResolverFactory());
         }
     }
 
     public static StaticMethodImportResolverFactory findStaticMethodImportResolverFactory(VariableResolverFactory factory) {
         VariableResolverFactory v = factory;
         while (v != null) {
             if (v instanceof StaticMethodImportResolverFactory) {
                 return (StaticMethodImportResolverFactory) v;
             }
             v = v.getNextFactory();
         }
 
         if (factory == null) {
             throw new OptimizationFailure("unable to import classes.  no variable resolver factory available.");
         }
         else {
             return ResolverTools.insertFactory(factory, new StaticMethodImportResolverFactory());
         }
     }
 
     public static Class findClass(VariableResolverFactory factory, String name) throws ClassNotFoundException {
         try {
             if (AbstractParser.LITERALS.containsKey(name)) {
                 return (Class) AbstractParser.LITERALS.get(name);
             }
             else if (factory != null && factory.isResolveable(name)) {
                 return (Class) factory.getVariableResolver(name).getValue();
             }
             else if (getCurrentThreadParserContext() != null && getCurrentThreadParserContext().hasImport(name)) {
                 return getCurrentThreadParserContext().getImport(name);
             }
             else {
                 return createClass(name);
             }
         }
         catch (ClassNotFoundException e) {
             throw e;
         }
         catch (Exception e) {
             throw new CompileException("class not found: " + name, e);
         }
     }
 
     public static boolean debug(String str) {
         return true;
     }
 
     public static boolean debug(Throwable t) {
         t.printStackTrace();
         return true;
     }
 
     public static char[] subset(char[] array, int start, int length) {
         char[] newArray = new char[length];
         arraycopy(array, start, newArray, 0, length);
         return newArray;
     }
 
     public static char[] subset(char[] array, int start) {
         char[] newArray = new char[array.length - start];
         arraycopy(array, start, newArray, 0, newArray.length);
         return newArray;
     }
 
     public static int resolveType(Class cls) {
         if (cls == null)
             return 0;
         if (BigDecimal.class == cls)
             return DataTypes.BIG_DECIMAL;
 
         if (BigInteger.class == cls)
             return DataTypes.BIG_INTEGER;
 
         if (String.class == cls)
             return DataTypes.STRING;
 
         if (int.class == cls)
             return DataTypes.INTEGER;
         if (short.class == cls)
             return DataTypes.SHORT;
         if (float.class == cls)
             return DataTypes.FLOAT;
         if (double.class == cls)
             return DataTypes.DOUBLE;
         if (long.class == cls)
             return DataTypes.LONG;
         if (boolean.class == cls)
             return DataTypes.BOOLEAN;
         if (byte.class == cls)
             return DataTypes.BYTE;
         if (char.class == cls)
             return DataTypes.CHAR;
 
 
         if (Integer.class == cls)
             return DataTypes.W_INTEGER;
         if (Short.class == cls)
             return DataTypes.W_SHORT;
         if (Float.class == cls)
             return DataTypes.W_FLOAT;
         if (Double.class == cls)
             return DataTypes.W_DOUBLE;
         if (Long.class == cls)
             return DataTypes.W_LONG;
         if (Boolean.class == cls)
             return DataTypes.W_BOOLEAN;
         if (Byte.class == cls)
             return DataTypes.W_BYTE;
         if (Character.class == cls)
             return DataTypes.W_CHAR;
 
 
         return DataTypes.OBJECT;
     }
 
     public static Object valueOnly(Object o) {
         return (o instanceof ASTNode) ? ((ASTNode) o).getLiteralValue() : o;
     }
 
     public static boolean isNumericallyCoercible(Class target, Class parm) {
         Class boxedTarget = target.isPrimitive() ? boxPrimitive(target) : target;
 
         if (boxedTarget != null && Number.class.isAssignableFrom(target)) {
             Class boxedParm = parm.isPrimitive() ? boxPrimitive(parm) : parm;
 
             if (boxedParm != null) {
                 return Number.class.isAssignableFrom(boxedParm);
             }
         }
         return false;
     }
 
     public static Object handleParserEgress(Object result, boolean returnBigDecimal) {
         if (result instanceof BigDecimal) {
             if (returnBigDecimal) return result;
             else if (((BigDecimal) result).scale() > 14) {
                 return ((BigDecimal) result).doubleValue();
             }
             else if (((BigDecimal) result).scale() > 0) {
                 return ((BigDecimal) result).floatValue();
             }
             else if (((BigDecimal) result).longValue() > Integer.MAX_VALUE) {
                 return ((BigDecimal) result).longValue();
             }
             else {
                 return ((BigDecimal) result).intValue();
             }
         }
         else
             return result;
 
     }
 
     public static Method determineActualTargetMethod(Method method) {
         String name = method.getName();
 
         /**
          * Follow our way up the class heirarchy until we find the physical target method.
          */
         for (Class cls : method.getDeclaringClass().getInterfaces()) {
             for (Method meth : cls.getMethods()) {
                 if (meth.getParameterTypes().length == 0 && name.equals(meth.getName())) {
                     return meth;
                 }
             }
         }
 
         return null;
     }
 
     public static Object doOperations(Object val1, int operation, Object val2) {
         return MATH_PROCESSOR.doOperation(val1, operation, val2);
     }
 
 
     public static Object increment(Object o) {
         if (o instanceof Integer) {
             return (Integer) o + 1;
         }
         else if (o instanceof Double) {
             return (Double) o + 1;
         }
         else if (o instanceof Float) {
             return (Float) o + 1;
         }
         else if (o instanceof Short) {
             return (Short) o + 1;
         }
         else if (o instanceof Character) {
             return (Character) o + 1;
         }
         else {
             throw new CompileException("unable to increment type: " + (o != null ? o.getClass().getName() : "null"));
         }
     }
 
 
     public static Map<String, String> parseParameters(char[] parms) {
         Map<String, String> allParms = new HashMap<String, String>();
 
         boolean capture = false;
         int start = 0;
 
         String parmName = null;
         int i = 0;
         for (; i < parms.length; i++) {
             switch (parms[i]) {
                 case'=':
                     i++;
                     parmName = new String(parms, start, i - start - 1).trim();
                     capture = true;
                     start = i;
                     break;
 
                 case',':
                     if (capture) {
                         allParms.put(parmName, new String(parms, start, i - start).trim());
                         start = ++i;
                         capture = false;
                         break;
                     }
             }
         }
 
         if (capture) {
             allParms.put(parmName, new String(parms, start, i - start).trim());
         }
 
         return allParms;
     }
 
 
     /**
      * This is an important aspect of the core parser tools.  This method is used throughout the core parser
      * and sub-lexical parsers to capture a balanced capture between opening and terminating tokens such as:
      * <em>( [ { ' " </em>
      * <br>
      * <br>
      * For example: ((foo + bar + (bar - foo)) * 20;<br>
      * <br>
      *
      * If a balanced capture is performed from position 2, we get "(foo + bar + (bar - foo))" back.<br>
      * If a balanced capture is performed from position 15, we get "(bar - foo)" back.<br>
      * Etc.
      *
      * @param chars
      * @param start
      * @param type
      * @return
      */
     public static int balancedCapture(char[] chars, int start, char type) {
         int depth = 1;
         char term = type;
         switch (type) {
             case'[':
                 term = ']';
                 break;
             case'{':
                 term = '}';
                 break;
             case'(':
                 term = ')';
                 break;
         }
 
         if (type == term) {
             for (start++; start < chars.length; start++) {
                 if (chars[start] == type) {
                     return start;
                 }
             }
         }
         else {
             for (start++; start < chars.length; start++) {
                 if (chars[start] == '\'' || chars[start] == '"') {
                     start = captureStringLiteral(chars[start], chars, start, chars.length);
                 }
                 else if (chars[start] == type) {
                     depth++;
                 }
                 else if (chars[start] == term && --depth == 0) {
                     return start;
                 }
             }
         }
 
         return -1;
     }
 
     public static String handleStringEscapes(char[] input) {
         int escapes = 0;
         for (int i = 0; i < input.length; i++) {
             if (input[i] == '\\') {
                 input[i++] = 0;
                 input[i] = handleEscapeSequence(input[i]);
                 escapes++;
             }
         }
 
         char[] processedEscapeString = new char[input.length - escapes];
         int cursor = 0;
         for (char aName : input) {
             if (aName == 0) {
                 continue;
             }
             processedEscapeString[cursor++] = aName;
         }
 
         return new String(processedEscapeString);
     }
 
     public static int captureStringLiteral(final char type, final char[] expr, int cursor, int length) {
         while (++cursor < length && expr[cursor] != type) {
             if (expr[cursor] == '\\') handleEscapeSequence(expr[++cursor]);
         }
 
         if (cursor == length || expr[cursor] != type) {
             throw new CompileException("unterminated literal", expr, cursor);
         }
 
         return cursor;
     }
 
 
     /**
      * REMOVE THIS WITH JDK1.4 COMPATIBILITY!  COMPENSATES FOR LACK OF getSimpleName IN java.lang.Class -- DIE 1.4!
      *
      * @param cls -- class reference
      * @return Simple name of class
      */
     public static String getSimpleClassName(Class cls) {
         if (JDK_14_COMPATIBILITY) {
             int lastIndex = cls.getName().lastIndexOf('$');
             if ( lastIndex < 0 ) {
                 lastIndex = cls.getName().lastIndexOf('.');    
             }
             if ( cls.isArray() ) {
                 return cls.getName().substring(lastIndex + 1) + "[]";
             } else {
                 return cls.getName().substring(lastIndex + 1);
             }
         }
         else {
             return cls.getSimpleName();
         }
     }
 
 
     public static void checkNameSafety(String name) {
         if (AbstractParser.isReservedWord(name)) {
             throw new CompileException("reserved word in assignment: " + name);
         }
     }
 
     public static FileWriter getDebugFileWriter() throws IOException {
         return new FileWriter(new File(MVEL.getDebuggingOutputFileName()), true);
     }
 
     public static boolean isPrimitiveWrapper(Class clazz) {
         return clazz == Integer.class || clazz == Boolean.class || clazz == Long.class || clazz == Double.class
                 || clazz == Float.class || clazz == Short.class || clazz == Byte.class || clazz == Character.class;
     }
 
     public static Serializable subCompileExpression(String expression) {
         return optimizeTree(new ExpressionCompiler(expression)._compile());
     }
 
 
     public static Serializable subCompileExpression(char[] expression) {
         return optimizeTree(new ExpressionCompiler(expression)._compile());
     }
 
     public static Serializable optimizeTree(final CompiledExpression compiled) {
         ASTIterator nodes = compiled.getTokens();
 
         /**
          * If there is only one token, and it's an identifier, we can optimize this as an accessor expression.
          */
         if (MVEL.isOptimizationEnabled() && nodes.size() == 1) {
             ASTNode tk = nodes.firstNode();
 
             if (tk.isLiteral() && !tk.isThisVal()) {
                 if ((tk.getFields() & ASTNode.INTEGER32) != 0) {
                     return new ExecutableLiteral(tk.getIntRegister());
                 }
                 else {
                     return new ExecutableLiteral(tk.getLiteralValue());
                 }
             }
             if (tk.isIdentifier()) {
                 return new ExecutableAccessor(tk, false);
             }
         }
 
         return compiled;
     }
 }
