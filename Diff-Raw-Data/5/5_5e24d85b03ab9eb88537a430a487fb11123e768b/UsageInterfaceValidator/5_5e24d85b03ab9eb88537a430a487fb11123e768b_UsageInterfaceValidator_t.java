 /**
  * Start time:19:32:15 2009-02-05<br>
  * Project: mobicents-jainslee-server-core<br>
  * 
  * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
  *         </a>
  * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
  */
 package org.mobicents.slee.container.component.validator;
 
 import java.lang.reflect.Method;
 import java.lang.reflect.Modifier;
 import java.util.Arrays;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.Map.Entry;
 
 import javax.slee.ComponentID;
 import javax.slee.usage.UnrecognizedUsageParameterSetNameException;
 
 import org.apache.log4j.Logger;
 import org.mobicents.slee.container.component.ProfileSpecificationComponent;
 import org.mobicents.slee.container.component.ResourceAdaptorComponent;
 import org.mobicents.slee.container.component.SbbComponent;
 import org.mobicents.slee.container.component.deployment.jaxb.descriptors.common.MUsageParameter;
 
 /**
  * Start time:19:32:15 2009-02-05<br>
  * Project: mobicents-jainslee-server-core<br>
  * 
  * This class is place where common elements like usage parameter interface is
  * validated. In 1.1 specs its widely usable - ra, profiles, sbbs. In 1.0 only
  * sbbs have those.
  * 
  * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
  *         </a>
  * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
  */
 public class UsageInterfaceValidator {
 
 	private final static String _INCREMENT_METHOD_PREFIX = "increment";
 	private final static String _GET_METHOD_PREFIX = "get";
 	private final static String _SAMPLE_METHOD_PREFIX = "sample";
 
 	private static transient Logger logger = Logger
 			.getLogger(UsageInterfaceValidator.class);
 
 	/**
 	 * This methods validate component which has usage parameter interface. Its
 	 * interface for methods. In case of 1.1 components parameters list must
 	 * match evey method defined. In case of 1.0 components parameters list MUST
 	 * be empty. It does not validate get usage method, those
 	 * 
 	 * @param isSlee11
 	 * 
 	 * @param usageInterface
 	 *            - interface class itself
 	 * @param parameters
 	 *            - list of parameters, in case of 1.0 sbb this MUST be null.
 	 * @return
 	 */
 	static boolean validateUsageParameterInterface(ComponentID id,
 			boolean isSlee11, Class usageInterface,
 			List<MUsageParameter> parameters) {
 
 		boolean passed = true;
 		String errorBuffer = new String("");
 		try {
 			if (!usageInterface.isInterface()) {
 				passed = false;
 				errorBuffer = appendToBuffer(id,
 						"Usage parameter interface class is not an interface",
 						"11.2", errorBuffer);
 				return passed;
 			}
 
 			// Interface constraints
 			if (isSlee11 && usageInterface.getPackage() == null) {
 				passed = false;
 				errorBuffer = appendToBuffer(
 						id,
 						"Usage parameter interface must be declared in pacakge name space.",
 						"11.2", errorBuffer);
 			}
 
 			if (!Modifier.isPublic(usageInterface.getModifiers())) {
 				passed = false;
 				errorBuffer = appendToBuffer(
 						id,
 						"Usage parameter interface must be declared as public.",
 						"11.2", errorBuffer);
 			}
 
 			// parameters check
 
 			HashMap<String, String> parameterNameToparameterType = new HashMap<String, String>();
 			Set<String> ignore = new HashSet<String>();
 			ignore.add("java.lang.Object");
 			Map<String, Method> interfaceMethods = ClassUtils
 					.getAllInterfacesMethods(usageInterface, ignore);
 			Map<String, MUsageParameter> localParametersMap = new HashMap<String, MUsageParameter>();
 
 			Set<String> identifiedIncrement = new HashSet<String>();
 			Set<String> identifiedGetIncrement = new HashSet<String>();
 			Set<String> identifiedSample = new HashSet<String>();
 			Set<String> identifiedGetSample = new HashSet<String>();
 			// this is for 1.1, get and increment methods must match with type
 
 			// validate parameter names if we are slee11
 			if (isSlee11)
 				for (MUsageParameter usage : parameters) {
 					char c = usage.getName().charAt(0);
 					if (!Character.isLowerCase(c)) {
 						passed = false;
 						errorBuffer = appendToBuffer(
 								id,
 								"Parameter name must start with lower case character and be start of valid jva identifier, parameter name from descriptor: "
 										+ usage.getName(), "11.2", errorBuffer);
 					}
 
 					localParametersMap.put(usage.getName(), usage);
 				}
 			// at the end we have to have empty list
 			for (Entry<String, Method> entry : interfaceMethods.entrySet()) {
 				// String declaredLongMethodName = entry.getKey();
 
 				Method m = entry.getValue();
 				String declaredMethodName = m.getName();
 				String declaredPrameterName = null;
 				Character c = null;
 				// he we just do checks, methods against constraints
 				// we remove them from parameters map, there is something left
 				// or not present in map in case of 1.1
 
 				// some variable that we need to store info about method
 				boolean isIncrement = false;
 				boolean isGetIncrement = false;
 				boolean isGetSample = false;
 				boolean isSample = false;
 
 				// 1.0 comp
 				if (declaredMethodName.startsWith(_SAMPLE_METHOD_PREFIX)) {
 					declaredPrameterName = declaredMethodName.replaceFirst(
 							_SAMPLE_METHOD_PREFIX, "");
 					c = declaredPrameterName.charAt(0);
 					isSample = true;
 
 					// 1.0 comp
 				} else if (declaredMethodName
 						.startsWith(_INCREMENT_METHOD_PREFIX)) {
 					declaredPrameterName = declaredMethodName.replaceFirst(
 							_INCREMENT_METHOD_PREFIX, "");
 					c = declaredPrameterName.charAt(0);
 					isIncrement = true;
 
 					// 1.1 only
 				} else if (declaredMethodName.startsWith(_GET_METHOD_PREFIX)) {
 					declaredPrameterName = declaredMethodName.replaceFirst(
 							_GET_METHOD_PREFIX, "");
 					c = declaredPrameterName.charAt(0);
 					if (!isSlee11) {
 						passed = false;
 						errorBuffer = appendToBuffer(
 								id,
 								"Wrong method declared in parameter usage interface. Get method for counter parameter types are allowed only in JSLEE 1.1, method: "
 										+ declaredMethodName, "11.2.X",
 								errorBuffer);
 					}
 					if (m.getReturnType().getName().compareTo(
 							"javax.slee.usage.SampleStatistics") == 0) {
 						isGetSample = true;
 					} else {
 						// we asume thats increment get
 						isGetIncrement = true;
 					}
 
 				} else {
 					passed = false;
 					errorBuffer = appendToBuffer(
 							id,
 							"Wrong method decalred in parameter usage interface. Methods must start with either \"get\", \"sample\" or \"increment\", method: "
 									+ declaredMethodName, "11.2.X", errorBuffer);
 					continue;
 				}
 
 				if (!Character.isUpperCase(c)) {
 					passed = false;
 					errorBuffer = appendToBuffer(
 							id,
 							"Method stripped of prefix, either \"get\", \"sample\" or \"increment\", must have following upper case character,method: "
 									+ declaredMethodName, "11.2", errorBuffer);
 
 				}
 
 				declaredPrameterName = Character.toLowerCase(c)
 						+ declaredPrameterName.substring(1);
 
 				// well we have indentified parameter, lets store;
 				if (isIncrement) {
 					if (identifiedIncrement.contains(declaredMethodName)) {
 						passed = false;
 						errorBuffer = appendToBuffer(
 								id,
 								"Duplicate declaration of usage parameter, possibly twe methods with the same name and different signature, method: "
 										+ declaredMethodName, "11.2",
 								errorBuffer);
 					} else {
 						identifiedIncrement.add(declaredPrameterName);
 						if (!validateParameterSetterSignatureMethod(id, m,
 								"11.2.3")) {
 							passed = false;
 
 						}
 					}
 
 				} else if (isGetIncrement) {
 					if (identifiedGetIncrement.contains(declaredMethodName)) {
 						passed = false;
 						errorBuffer = appendToBuffer(
 								id,
 								"Duplicate declaration of usage parameter, possibly twe methods with the same name and different signature, method: "
 										+ declaredMethodName, "11.2",
 								errorBuffer);
 					} else {
 						identifiedGetIncrement.add(declaredPrameterName);
 						if (!validateParameterGetterSignatureMethod(id, m,
 								"11.2.2", Long.TYPE)) {
 							passed = false;
 
 						}
 					}
 
 				} else if (isGetSample) {
 					if (identifiedGetSample.contains(declaredMethodName)) {
 						passed = false;
 						errorBuffer = appendToBuffer(
 								id,
 								"Duplicate declaration of usage parameter, possibly twe methods with the same name and different signature, method: "
 										+ declaredMethodName, "11.2",
 								errorBuffer);
 					} else {
 						identifiedGetSample.add(declaredPrameterName);
 						if (!validateParameterGetterSignatureMethod(id, m,
 								"11.2.4",
 								javax.slee.usage.SampleStatistics.class)) {
 							passed = false;
 
 						}
 					}
 
 				} else if (isSample) {
 					if (identifiedSample.contains(declaredMethodName)) {
 						passed = false;
 						errorBuffer = appendToBuffer(
 								id,
 								"Duplicate declaration of usage parameter, possibly twe methods with the same name and different signature, method: "
 										+ declaredMethodName, "11.2",
 								errorBuffer);
 					} else {
 						identifiedSample.add(declaredPrameterName);
 						if (!validateParameterSetterSignatureMethod(id, m,
 								"11.2.1")) {
 							passed = false;
 							errorBuffer = appendToBuffer(
 									id,
 									"Duplicate declaration of usage parameter, possibly twe methods with the same name and different signature, method: "
 											+ declaredMethodName, "11.2",
 									errorBuffer);
 						}
 					}
 
 				}
 
 				// UFFF, lets start the play
 				// /uh its a bit complicated
 			}
 			// we siganture here is ok, return types also, no duplicates, left:
 			// 1. cross check field types that we found - sample vs increments -
 			// there cant be doubles
 			// 2. remove all from list, if something is left, bam, we lack one
 			// method or have to many :)
 			Set<String> agregatedIncrement = new HashSet<String>();
 			Set<String> agregatedSample = new HashSet<String>();
 
 			agregatedIncrement.addAll(identifiedGetIncrement);
 			agregatedIncrement.addAll(identifiedIncrement);
 
 			agregatedSample.addAll(identifiedGetSample);
 			agregatedSample.addAll(identifiedSample);
 			Set<String> tmp = new HashSet<String>(agregatedSample);
 
 			tmp.retainAll(agregatedIncrement);
 			if (tmp.size() > 0) {
 				// ugh, its the end
 				passed = false;
 				errorBuffer = appendToBuffer(
 						id,
 						"Usage parameters can be associated only with single type - increment or sample, offending parameters: "
 								+ Arrays.toString(tmp.toArray()), "11.2",
 						errorBuffer);
 
 				return passed;
 			}
 
 			if (isSlee11) {
 				tmp.clear();
 				tmp.addAll(agregatedSample);
 				tmp.addAll(agregatedIncrement);
 
				//localParametersMap.size()!=0 - cause we can have zero of them - usage-parameter may not be present so its generation is turned off
				if (localParametersMap.size() != tmp.size() && localParametersMap.size()!=0) {
			
 					passed = false;
 
 					String errorPart = null;
 					if (localParametersMap.size() > tmp.size()) {
 						// is there any bettter way?
 						for (String s : localParametersMap.keySet())
 							tmp.remove(s);
 						errorPart = "More parameters are defined in descriptor, offending parameters: "
 								+ Arrays.toString(tmp.toArray());
 					} else {
 						for (String s : tmp)
 							localParametersMap.remove(s);
 						errorPart = "More parameters are defined in descriptor, offending parameters: "
 								+ Arrays.toString(localParametersMap.keySet()
 										.toArray());
 					}
 
 					errorBuffer = appendToBuffer(
 							id,
 							"Failed to map descriptor defined usage parameters against interface class methods. "
 									+ errorPart, "11.2", errorBuffer);
 
 				}
 			}
 		} finally {
 			if (!passed) {
 				logger.error(errorBuffer.toString());
 				//System.err.println(errorBuffer);
 			}
 		}
 
 		return passed;
 	}
 
 	static boolean validateParameterGetterSignatureMethod(ComponentID id,
 			Method m, String section, Class returnType) {
 		boolean passed = true;
 		String errorBuffer = new String("");
 		try {
 			// public, abstract, void, no throws, long parameter
 			int modifiers = m.getModifiers();
 			if (!Modifier.isPublic(modifiers)) {
 				passed = false;
 				errorBuffer = appendToBuffer(id,
 						"Usage paremter method must be declared public, method: "
 								+ m, section, errorBuffer);
 			}
 
 			if (!Modifier.isAbstract(modifiers)) {
 				passed = false;
 				errorBuffer = appendToBuffer(id,
 						"Usage paremter method must be declared abstract, method: "
 								+ m, section, errorBuffer);
 			}
 
 			if (m.getExceptionTypes().length > 0) {
 				passed = false;
 				errorBuffer = appendToBuffer(id,
 						"Usage paremter method must not declared throws clause, method: "
 								+ m, section, errorBuffer);
 			}
 
 			if (m.getReturnType().getName().compareTo(returnType.getName()) != 0) {
 				passed = false;
 				errorBuffer = appendToBuffer(id,
 						"Usage paremter method must not declare return type, method: "
 								+ m, section, errorBuffer);
 			}
 
 			Class[] params = new Class[] {};
 			if (!Arrays.equals(m.getParameterTypes(), params)) {
 				passed = false;
 				errorBuffer = appendToBuffer(id,
 						"Usage paremter method must not have parameters, method: "
 								+ m, section, errorBuffer);
 			}
 
 		} finally {
 			if (!passed) {
 				logger.error(errorBuffer.toString());
 				// ////System.err.println(errorBuffer);
 			}
 		}
 
 		return passed;
 	}
 
 	static boolean validateParameterSetterSignatureMethod(ComponentID id,
 			Method m, String section) {
 		boolean passed = true;
 		String errorBuffer = new String("");
 		try {
 			// public, abstract, void, no throws, long parameter
 			int modifiers = m.getModifiers();
 			if (!Modifier.isPublic(modifiers)) {
 				passed = false;
 				errorBuffer = appendToBuffer(id,
 						"Usage paremter method must be declared public, method: "
 								+ m, section, errorBuffer);
 			}
 
 			if (!Modifier.isAbstract(modifiers)) {
 				passed = false;
 				errorBuffer = appendToBuffer(id,
 						"Usage paremter method must be declared abstract, method: "
 								+ m, section, errorBuffer);
 			}
 
 			if (m.getExceptionTypes().length > 0) {
 				passed = false;
 				errorBuffer = appendToBuffer(id,
 						"Usage paremter method must not declared throws clause, method: "
 								+ m, section, errorBuffer);
 			}
 
 			if (m.getReturnType().getName().compareTo("void") != 0) {
 				passed = false;
 				errorBuffer = appendToBuffer(id,
 						"Usage paremter method must not declare return type, method: "
 								+ m, section, errorBuffer);
 			}
 
 			Class[] params = new Class[] { Long.TYPE };
 			if (!Arrays.equals(m.getParameterTypes(), params)) {
 				passed = false;
 				errorBuffer = appendToBuffer(id,
 						"Usage paremter method must have single parameter of type long, method: "
 								+ m, section, errorBuffer);
 			}
 
 		} finally {
 			if (!passed) {
 				logger.error(errorBuffer.toString());
 				// ////System.err.println(errorBuffer);
 			}
 		}
 
 		return passed;
 	}
 
 	static boolean validateSbbUsageParameterInterface(SbbComponent component,
 			Map<String, Method> sbbAbstractClassMethods,
 			Map<String, Method> sbbAbstractMethodsFromSuperClasses) {
 
 		String errorBuffer = new String("");
 		boolean passed = true;
 		boolean foundAtleastOne = false;
 		String methodName = "getDefaultSbbUsageParameterSet";
 		Class componentClass = component.getAbstractSbbClass();
 
 		Method m = null;
 
 		m = ClassUtils.getMethodFromMap(methodName, new Class[] {},
 				sbbAbstractClassMethods, sbbAbstractMethodsFromSuperClasses);
 
 		if (m != null) {
 			foundAtleastOne = true;
 			if (!validateGetUsageMethodSignature(component.getSbbID(), m, component
 					.getUsageParametersInterface(), new Class[] {}, "8.4.1")) {
 				passed = false;
 
 			}
 
 			sbbAbstractClassMethods.remove(ClassUtils.getMethodKey(m));
 			sbbAbstractMethodsFromSuperClasses.remove(ClassUtils
 					.getMethodKey(m));
 
 		}
 		methodName = "getSbbUsageParameterSet";
 
 		m = ClassUtils.getMethodFromMap(methodName,
 				new Class[] { String.class }, sbbAbstractClassMethods,
 				sbbAbstractMethodsFromSuperClasses);
 
 		if (m != null) {
 			foundAtleastOne = true;
 			if (!validateGetUsageMethodSignature(
 					component.getSbbID(),
 					m,
 					component.getUsageParametersInterface(),
 					new Class[] { UnrecognizedUsageParameterSetNameException.class },
 					"8.4.1")) {
 				passed = false;
 
 			}
 			sbbAbstractClassMethods.remove(ClassUtils.getMethodKey(m));
 			sbbAbstractMethodsFromSuperClasses.remove(ClassUtils
 					.getMethodKey(m));
 		}
 
 		if (!validateUsageParameterInterface(component.getSbbID(), component
 				.isSlee11(), component.getUsageParametersInterface(), component
 				.getDescriptor().getSbbClasses().getSbbUsageParametersInterface()
 				.getUsageParameter())) {
 			passed = false;
 		}
 
 		if (!foundAtleastOne) {
 			passed = false;
 			errorBuffer = appendToBuffer(
 					component.getSbbID(),
 					"Atleast one get usage interface method must be present(it must be public).",
 					"8.4.1", errorBuffer);
 		}
 
 		if (!passed) {
 			logger.error(errorBuffer.toString());
 			//System.err.println(errorBuffer);
 		}
 
 		return passed;
 
 	}
 
 	static boolean validateGetUsageMethodSignature(ComponentID id, Method m,
 			Class returnType, Class[] exceptions, String section) {
 
 		// must be public, abstract
 		boolean passed = true;
 		String errorBuffer = new String("");
 
 		int modifiers = m.getModifiers();
 		if (!Modifier.isAbstract(modifiers)) {
 			passed = false;
 			errorBuffer = appendToBuffer(id,
 					"Usage interface access method must be abstract, method name: "
 							+ m.getName(), section, errorBuffer);
 		}
 
 		if (!Modifier.isPublic(modifiers)) {
 			passed = false;
 			errorBuffer = appendToBuffer(id,
 					"Usage interface access method must be public, method name: "
 							+ m.getName(), section, errorBuffer);
 		}
 
 		// if (Modifier.isStatic(modifiers)) {
 		// passed = false;
 		// errorBuffer =
 		// appendToBuffer(id,"Usage interface access method must be abstract is static, method name: "
 		// + m.getName(),
 		// section, errorBuffer);
 		// }
 
 		// FIXME: native?
 		if (!Arrays.equals(m.getExceptionTypes(), exceptions)) {
 			passed = false;
 			errorBuffer = appendToBuffer(id,
 					"Usage interface access method has wrong exception types defined, method: "
 							+ m.getName() + ", allowed: "
 							+ Arrays.toString(exceptions) + ", present: "
 							+ Arrays.toString(m.getExceptionTypes()), section,
 					errorBuffer);
 
 		}
 
 		if (m.getReturnType().getName().compareTo(returnType.getName()) == 0 || ClassUtils.checkInterfaces(returnType, m.getReturnType().getName())!=null) {
 
 			//ok
 		
 
 		}else
 		{
 			passed = false;
 			errorBuffer = appendToBuffer(id,
 					"Usage interface access method has wrong return type defined, method: "
 							+ m.getName(), section, errorBuffer);
 		}
 		if (!passed) {
 			logger.error(errorBuffer.toString());
 			//System.err.println(errorBuffer);
 		}
 
 		return passed;
 	}
 
 	static boolean validateResourceAdaptorUsageParameterInterface(
 			ResourceAdaptorComponent component) {
 
 		boolean passed = true;
 		if (!validateUsageParameterInterface(component.getResourceAdaptorID(), component
 				.isSlee11(), component.getUsageParametersInterface(), component
 				.getDescriptor().getResourceAdaptorUsageParametersInterface().getUsageParameter()
 				)) {
 			passed = false;
 		}
 		
 		return passed;
 	}
 
 	static boolean validateProfileSpecificationUsageParameterInterface(
 			ProfileSpecificationComponent component,
 			Map<String, Method> abstractClassMethods,
 			Map<String, Method> abstractMethodsFromSuperClasses) {
 
 		
 		
 		String errorBuffer = new String("");
 		boolean passed = true;
 		boolean foundAtleastOne = false;
 		String methodName = "getDefaultUsageParameterSet";
 		Class componentClass = component.getProfileAbstractClass();
 
 		Method m = null;
 
 		m = ClassUtils.getMethodFromMap(methodName, new Class[] {},
 				abstractClassMethods, abstractMethodsFromSuperClasses);
 
 		if (m != null) {
 			foundAtleastOne = true;
 			if (!validateGetUsageMethodSignature(component.getProfileSpecificationID(), m, component
 					.getUsageParametersInterface(), new Class[] {}, "11.4.2")){
 				passed = false;
 
 			}
 
 			abstractClassMethods.remove(ClassUtils.getMethodKey(m));
 			abstractMethodsFromSuperClasses.remove(ClassUtils
 					.getMethodKey(m));
 
 		}
 		methodName = "getUsageParameterSet";
 
 		m = ClassUtils.getMethodFromMap(methodName,
 				new Class[] { String.class }, abstractClassMethods,
 				abstractMethodsFromSuperClasses);
 
 		if (m != null) {
 			foundAtleastOne = true;
 			if (!validateGetUsageMethodSignature(
 					component.getProfileSpecificationID(),
 					m,
 					component.getUsageParametersInterface(),
 					new Class[] { UnrecognizedUsageParameterSetNameException.class },
 					"11.4.2")) {
 				passed = false;
 
 			}
 			abstractClassMethods.remove(ClassUtils.getMethodKey(m));
 			abstractMethodsFromSuperClasses.remove(ClassUtils
 					.getMethodKey(m));
 		}
 
 		if (!validateUsageParameterInterface(component.getProfileSpecificationID(), component
 				.isSlee11(), component.getUsageParametersInterface(), component
 				.getDescriptor().getProfileClasses().getProfileUsageParameterInterface().getUsageParameter()
 				)) {
 			passed = false;
 		}
 
 		if (!foundAtleastOne) {
 			passed = false;
 			errorBuffer = appendToBuffer(
 					component.getProfileSpecificationID(),
 					"Atleast one get usage interface method must be present(it must be public).",
 					"11.4.2", errorBuffer);
 		}
 
 		if (!passed) {
 			logger.error(errorBuffer.toString());
 			//System.err.println(errorBuffer);
 		}
 
 		return passed;
 	}
 
 	protected static String appendToBuffer(ComponentID id, String message,
 			String section, String buffer) {
 		buffer += (id + " : violates section " + section
 				+ " of jSLEE 1.1 specification : " + message + "\n");
 		return buffer;
 	}
 
 	
 
 }
