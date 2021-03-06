 package st.redline;
 
 import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Method;
 import java.lang.reflect.Modifier;
 import java.util.HashMap;
 import java.util.Map;
 
 public abstract class ProtoObject {
 
 	protected Map<String, Method> methodDictionary;
 	private static final Object[] NO_ARGUMENTS = {};
 
 	public abstract ProtoObject $class();
 
 	public ProtoObject $superclass() {
 		return null;
 	}
 
 	public ProtoObject() {
 		System.out.println("constructing ProtoObject");
 	}
 
 	public ProtoObject $send(java.lang.String selector) {
 		System.out.println("$send(" + selector + ")");
 		return tryInvokeMethod($findMethod(selector), selector, NO_ARGUMENTS);
 	}
 
 	private ProtoObject tryInvokeMethod(Method method, String selector, Object[] arguments) {
 		if (method != null)
 			try {
 				return (ProtoObject) method.invoke(this, arguments);
 			} catch (IllegalAccessException e) {
 				e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
 			} catch (InvocationTargetException e) {
 				e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
 			}
 		if (!selector.equals("doesNotUnderstand:"))
 			return sendDoesNotUnderstand(selector, arguments);
 		throw new IllegalStateException("Expected doesNotUnderstand: to be implemented.");
 	}
 
 	private ProtoObject sendDoesNotUnderstand(String selector, Object[] arguments) {
 		System.out.println("sendDoesNotUnderstand(" + selector + "," + arguments + ")");
 		throw new RuntimeException("TODO -  need to implement send of doesNotUnderstand");
 	}
 
 	private Method $findMethod(String selector) {
 		System.out.println("$findMethod(" + selector + ") " + this);
 		ProtoObject aClass = $class();
 		Method method = aClass.methodDictionary.get(selector);
 		while (method == null) {
 			ProtoObject aSuperclass = aClass.$superclass();
 			if (aSuperclass == null)
 				break;
 			method = aSuperclass.methodDictionary.get(selector);
 			if (method != null) {
 				break;
 			}
 			aClass = aSuperclass;
 		}
 		return method;
 	}
 
 	protected Map<String, Method> methodsFrom(java.lang.Class primitiveClass) {
 		System.out.println("methodsFrom(" + primitiveClass + ")");
 		Map<String, Method> methods = new HashMap<String, Method>();
		for (Method method : primitiveClass.getDeclaredMethods()) {
			if (Modifier.isPublic(method.getModifiers())
				&& !Modifier.isStatic(method.getModifiers())
				&& !method.getName().startsWith("$")) {
				System.out.println("Caching method: " + method);
				methods.put(method.getName(), method);
			}
		}
 		return methods;
 	}
 }
