 package airlift.util;
 
 import java.lang.annotation.Annotation;
 import java.lang.reflect.Type;
 import java.util.logging.Logger;
 
 import org.apache.commons.lang.StringUtils;
 
 import airlift.rest.Route;
 
 import com.google.gson.GsonBuilder;
 
 
 public class AirliftUtil
 {
 	private static Logger log = Logger.getLogger(AirliftUtil.class.getName());
 
 	public static org.apache.commons.beanutils.BeanUtilsBean createBeanUtilsBean(String[] _allowedDateTimePatterns, java.util.TimeZone _timeZone)
 	{
 		org.apache.commons.beanutils.converters.SqlDateConverter sqlDateConverter = new org.apache.commons.beanutils.converters.SqlDateConverter();
 		sqlDateConverter.setPatterns(_allowedDateTimePatterns);
 		sqlDateConverter.setTimeZone(_timeZone);
 
 		org.apache.commons.beanutils.converters.DateConverter dateConverter = new org.apache.commons.beanutils.converters.DateConverter();
 		dateConverter.setPatterns(_allowedDateTimePatterns);
 		dateConverter.setTimeZone(_timeZone);
 
 		org.apache.commons.beanutils.converters.SqlTimestampConverter sqlTimestampConverter = new org.apache.commons.beanutils.converters.SqlTimestampConverter();
 		sqlTimestampConverter.setPatterns(_allowedDateTimePatterns);
 		sqlTimestampConverter.setTimeZone(_timeZone);
 
 		//registering "" (empty string) as a true value to support checkboxes with
 		//the value attribute not being set.  Setting the value
 		//atrribute wil make the value visible on the form.  This may
 		//not be desired for a simple yes-no option hence the need to
 		//register "" as true.
 		String[] trueStrings = {"yes", "y", "true", "on", "1", ""};
 		String[] falseStrings = {"no", "n", "false", "off", "0"};
 		org.apache.commons.beanutils.converters.BooleanConverter booleanConverter = new org.apache.commons.beanutils.converters.BooleanConverter(trueStrings, falseStrings, Boolean.FALSE);
 
 		org.apache.commons.beanutils.ConvertUtilsBean convertUtilsBean = new org.apache.commons.beanutils.ConvertUtilsBean();
 		convertUtilsBean.register(sqlDateConverter, java.sql.Date.class);
 		convertUtilsBean.register(dateConverter, java.util.Date.class);
 		convertUtilsBean.register(sqlTimestampConverter, java.sql.Timestamp.class);
 		convertUtilsBean.register(booleanConverter, Boolean.class);
 		convertUtilsBean.register(booleanConverter, Boolean.TYPE);
 
 		return new org.apache.commons.beanutils.BeanUtilsBean(convertUtilsBean);
 	}
 
 	public static String serializeStackTrace(Throwable _t)
 	{				
 		java.io.ByteArrayOutputStream byteArrayOutputStream = new java.io.ByteArrayOutputStream();
 		java.io.PrintWriter printWriter = null;
 		String errorString = null;
 
 		try
 		{
 			printWriter = new java.io.PrintWriter(byteArrayOutputStream, true);
 			_t.printStackTrace(printWriter);
 			errorString = byteArrayOutputStream.toString();
 		}
 		catch (Throwable u)
 		{
 			if (printWriter != null) { try { printWriter.close(); } catch (Throwable v) {} }
 		}
 
 		return errorString;
 	}
 
 	public static String toJson(java.util.List _list)
 	{
 		return new GsonBuilder().create().toJson(_list);
 	}
 
 	public static String toJson(Object _object)
 	{
 		return new GsonBuilder().create().toJson(_object);
 	}
 
 	public static Object fromJson(String _json, Class _class)
 	{
 		return new GsonBuilder().create().fromJson(_json, _class);
 	}
 
 	public static String createAirliftType(String _javaType)
 	{
 		String airliftType = "airlift:string";
 		String[] tokenArray = _javaType.split("\\.");
 
 		String type = tokenArray[tokenArray.length - 1].toLowerCase();
 
 		if (type.startsWith("int") == true)
 		{
 			airliftType = "airlift:int";
 		}
 		else if (type.startsWith("char") == true)
 		{
 			airliftType = "airlift:char";
 		}
 		else
 		{
 			airliftType = "airlift:" + type;
 		}
 
 		return airliftType;
 	}
 
 	public static boolean isWhitespace(String _string)
 	{
 		return StringUtils.isWhitespace(_string);
 	}
 
 	public static String upperTheFirstCharacter(String _string)
 	{
  		return StringUtils.capitalize(_string);
 	}
 
 	public static String lowerTheFirstCharacter(String _string)
 	{
 		return StringUtils.uncapitalize(_string);
 	}
 
 	public static <T extends Annotation> T getMethodAnnotation(Class _class, String _attributeName, Class<T> _annotationClass)
 	{
 		try
 		{
 			String getter = "get" + upperTheFirstCharacter(_attributeName);
 			java.lang.reflect.Method method = _class.getMethod(getter);
 			
 			return method.getAnnotation(_annotationClass);
 		}
 		catch(Throwable t)
 		{
 			throw new RuntimeException(t);
 		}
 	}
 
 	public static String getAttributeType(Object _object, String _attributeName)
 	{
 		try
 		{
 			String getter = "get" + upperTheFirstCharacter(_attributeName);
 			java.lang.reflect.Method method = _object.getClass().getMethod(getter);
 			
 			return method.getReturnType().getName();
 		}
 		catch(Throwable t)
 		{
 
 			throw new RuntimeException(t);
 		}
 	}
 
 	public static boolean isDomainName(String _domainName, String _rootPackageName)
 	{
 		boolean isDomainName = false;
 
 		try
 		{
 			airlift.AppProfile appProfile = (airlift.AppProfile) Class.forName(_rootPackageName + ".AppProfile").newInstance();
 
 			isDomainName = appProfile.isValidDomain(_domainName);
 		}
 		catch(Throwable t)
 		{
 			throw new RuntimeException("Cannot load Airlift generated class: " + _rootPackageName + ".AppProfile");
 		}
 
 		return isDomainName;
 	}
 	
 	public static boolean isNewDomainName(String _domainName, String _rootPackageName)
 	{
 		boolean isNewDomainName = false;
 
 		if (_domainName.toLowerCase().startsWith("new_") == true && _domainName.length() > 3)
 		{
 			String domainName = _domainName.substring(4, _domainName.length());
 			isNewDomainName = isDomainName(domainName, _rootPackageName);
 		}
 
 		return isNewDomainName;
 	}
 
 	public static boolean isUriACollection(String _uri, String _rootPackageName)
 	{
 		boolean isUriACollection = false;
 
 		String[] tokenArray = _uri.split("\\/");
 
 		if (tokenArray.length > 1)
 		{
 			String last = tokenArray[tokenArray.length - 1];
 
 			if (last.contains(".") == true)
 			{
 				String[] lastTokenArray = last.split("\\.");
 				last = lastTokenArray[0];
 			}
 			
 			isUriACollection = isDomainName(last, _rootPackageName);
 		}
 
 		return isUriACollection;
 	}
 
 	public static boolean isUriANewDomain(String _uri, String _rootPackageName)
 	{
 		boolean isUriANewDomain = false;
 
 		String[] tokenArray = _uri.split("\\/");
 
 		if (tokenArray.length > 1)
 		{
 			String last = tokenArray[tokenArray.length - 1];
 
 			if (last.contains(".") == true)
 			{
 				String[] lastTokenArray = last.split("\\.");
 				last = lastTokenArray[0];
 			}
 
 			isUriANewDomain = isNewDomainName(last, _rootPackageName);
 		}
 
 		return isUriANewDomain;
 	}
 
 	public static String determinePrimaryKeyName(String _domainName, String _rootPackageName)
 	{
 		return "id";
 	}
 
 	public static void populateDomainInformation(String _uri, java.util.Map _uriParameterMap, String _rootPackageName)
 	{
 		String[] tokenArray = _uri.split("\\/");
 
 		String parentDomain = null;
 
 		for (String token: tokenArray)
 		{
 			String candidateToken = token;
 
 			java.util.List<String> tokenList = hasSuffix(token);
 
 			if (tokenList.isEmpty() == false)
 			{
 				candidateToken = tokenList.get(0);
 				Route.addSuffix(_uriParameterMap, (String) tokenList.get(1));
 			}
 
 			if (isDomainName(candidateToken, _rootPackageName) == true || isNewDomainName(candidateToken, _rootPackageName) == true)
 			{				
 				Route.addDomainName(_uriParameterMap, candidateToken);
 				parentDomain = candidateToken;
 			}
 			else if (parentDomain != null)
 			{
 				String primaryKeyName = airlift.util.AirliftUtil.determinePrimaryKeyName(parentDomain, _rootPackageName);
 				String primaryKey = candidateToken;
 				
 				Route.addBindings(_uriParameterMap, parentDomain, primaryKeyName, primaryKey);
 				parentDomain = null;
 			}
 		}
 
 		log.info("uri parameter map is: " + _uriParameterMap);
 	}
 
 	public static java.util.List<String> hasSuffix(String _token)
 	{
 		java.util.List<String> list = new java.util.ArrayList<String>();
 		
 		if (_token.contains(".") == true)
 		{
 			String[] tokenArray = _token.split("\\.");
 			
 			list.add(tokenArray[0]);
 			list.add(tokenArray[1]);
 		}
 
 		return list;
 	}
 	
 	public static String generateStringFromArray(Object[] _object)
 	{
 		StringBuffer stringBuffer = new StringBuffer();
 
 		stringBuffer.append("[");
 
 		if (_object != null)
 		{
 			for (Object object: _object)
 			{
 				if (object != null)
 				{
 					stringBuffer.append(object.toString()).append("'");
 				}
 				else
 				{
 					stringBuffer.append("'");
 				}
 			}
 		}
 
 		return stringBuffer.toString().replaceAll(",$", "") + "]";
 	}
 
 	public static byte[] doCipher(byte[] _message, String _password, String _initialVector,
 								String _provider, String _name, String _mode, String _padding,
 								int _revolutions, String _cipherMode)
 	{
 		byte[] initialBytes = (_message != null) ? _message : new byte[0];
 		
 		try
 		{
 			byte[] key = _password.getBytes();
 			byte[] initialVectorSpec = _initialVector.getBytes();
 
 			String provider = (_provider != null) ? _provider : "SunJCE"; 
 			String name = (_name != null) ? _name : "AES";
 			String mode = (_mode != null) ? _mode : "PCBC";
 			String padding = (_padding != null) ? _padding : "PKCS5PADDING";
 			int revolutions = _revolutions;
 
 			int cipherMode = ("encrypt".equalsIgnoreCase(_cipherMode) == true) ? javax.crypto.Cipher.ENCRYPT_MODE : javax.crypto.Cipher.DECRYPT_MODE;
 
 			String algorithmString = name + "/" + mode + "/" + padding;
 
 			javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(algorithmString, provider);
 
 			cipher.init(cipherMode,
 						new javax.crypto.spec.SecretKeySpec(key, name),
 						new javax.crypto.spec.IvParameterSpec(initialVectorSpec)); 
 
 			for (int i = 0; i < revolutions; i++)
 			{
 				initialBytes = cipher.doFinal(initialBytes); 
 			}
 		}
 		catch(Throwable t)
 		{
 			throw new RuntimeException(t);
 		}
 
 		return initialBytes;
 	}
 
 	public static byte[] encrypt(byte[] _initialBytes, String _password, String _initialVector, String _provider,
 					  String _name, String _mode, String _padding, int _revolutions)
 	{
 		byte[] initialBytes = _initialBytes;
 		
 		if (initialBytes != null)
 		{
 			initialBytes = doCipher(initialBytes, _password, _initialVector, _provider, _name, _mode, _padding, _revolutions, "encrypt");
 		}
 		else
 		{
 			initialBytes = new byte[0];
 		}
 
 		return initialBytes;
 	}
 
 	public static byte[] decrypt(byte[] _initialBytes, String _password, String _initialVector, String _provider,
 						  String _name, String _mode, String _padding, int _revolutions)
 	{
 		byte[] initialBytes = _initialBytes;
 
 		if (initialBytes != null)
 		{
 			initialBytes = doCipher(initialBytes, _password, _initialVector, _provider, _name, _mode, _padding, _revolutions, "decrypt");
 		}
 		else
 		{
 			initialBytes = new byte[0];
 		}
 
 		return initialBytes;
 	}
 
 	public static byte[] convert(byte[] _byteArray)
 	{
 		return _byteArray;
 	}
 	
 	public static byte[] convert(String _string)
 	{
 		return (_string == null) ? null : convert(_string.getBytes());
 	}
 
 	public static byte[] convert(java.lang.Long _number)
 	{
 		return (_number == null) ? null : convert(_number.toString());
 	}
 
 	public static byte[] convert(java.lang.Short _number)
 	{
 		return (_number == null) ? null : convert(_number.toString());
 	}
 
 	public static byte[] convert(java.lang.Integer _number)
 	{
 		return (_number == null) ? null : convert(_number.toString());
 	}
 
 	public static byte[] convert(java.lang.Double _number)
 	{
 		return (_number == null) ? null : convert(_number.toString());
 	}
 
 	public static byte[] convert(java.lang.Float _number)
 	{
 		return (_number == null) ? null : convert(_number.toString());
 	}
 
 	public static byte[] convert(java.util.Date _date)
 	{
 		return (_date == null) ? null : convert(_date.getTime());
 	}
 
 	public static byte[] convertToByteArray(byte[] _byteArray)
 	{
 		return _byteArray;
 	}
 
 	public static String convertToString(byte[] _byteArray)
 	{
 		return (_byteArray == null) ? null : new String(convertToByteArray(_byteArray));
 	}
 
 	public static Short convertToShort(byte[] _byteArray)
 	{
		return (_byteArray == null) ? null : Short.parseShort(convertToString(_byteArray));
 	}
 
 	public static Long convertToLong(byte[] _byteArray)
 	{
		return (_byteArray == null) ? null : Long.parseLong(convertToString(_byteArray));
 	}
 
 	public static Integer convertToInteger(byte[] _byteArray)
 	{
		return (_byteArray == null) ? null : Integer.parseInt(convertToString(_byteArray));
 	}
 
 	public static Double convertToDouble(byte[] _byteArray)
 	{
		return (_byteArray == null) ? null : Double.parseDouble(convertToString(_byteArray));
 	}
 
 	public static Float convertToFloat(byte[] _byteArray)
 	{
		return (_byteArray == null) ? null : Float.parseFloat(convertToString(_byteArray));
 	}
 
 	public static java.util.Date convertToDate(byte[] _byteArray)
 	{
		return (_byteArray == null) ? null : new java.util.Date(convertToLong(_byteArray));
 	}
 	
 	public static java.util.Map<String, Object> describe(Object _do, Class _interfaceClass)
 	{
 		java.util.Map<String, Object> descriptionMap = new java.util.HashMap<String, Object>();
 
 		try
 		{
 			org.apache.commons.beanutils.PropertyUtilsBean propertyUtilsBean = new org.apache.commons.beanutils.PropertyUtilsBean();
 
 			java.beans.PropertyDescriptor[] descriptorArray = propertyUtilsBean.getPropertyDescriptors(_do);
 
 			for (java.beans.PropertyDescriptor propertyDescriptor: descriptorArray)
 			{
 				if ("class".equalsIgnoreCase(propertyDescriptor.getName()) == false)
 				{
 					java.lang.reflect.Method getter = propertyDescriptor.getReadMethod();
 
 					Object rawValue = getter.invoke(_do, new Object[0]);
 					Object value = null;
 
 					if (java.sql.Date.class.equals(propertyDescriptor.getPropertyType()) == true)
 					{
 						airlift.generator.Datable datable = airlift.util.AirliftUtil.getMethodAnnotation(_interfaceClass, propertyDescriptor.getName(), airlift.generator.Datable.class);
 
 						String mask = "MM-dd-yyyy";
 
 						if (datable != null)
 						{
 							String[] datePatternArray = datable.dateTimePatterns();
 
 							if (datePatternArray != null && datePatternArray.length > 0)
 							{
 								mask = datePatternArray[0];
 							}
 						}
 
 						value = airlift.util.FormatUtil.format((java.util.Date)rawValue, mask);
 					}
 					else if (java.util.Date.class.equals(propertyDescriptor.getPropertyType()) == true)
 					{
 						airlift.generator.Presentable presentable = airlift.util.AirliftUtil.getMethodAnnotation(_interfaceClass, propertyDescriptor.getName(), airlift.generator.Presentable.class);
 
 						String mask = "MM-dd-yyyy";
 
 						if (presentable != null)
 						{
 							String pattern = presentable.dateTimePattern();
 
 							if (pattern != null)
 							{
 								mask = pattern;
 							}
 						}
 
 						value = airlift.util.FormatUtil.format((java.util.Date) rawValue, mask);
 
 					}
 					else if (java.sql.Timestamp.class.equals(propertyDescriptor.getPropertyType()) == true)
 					{
 						airlift.generator.Datable datable = airlift.util.AirliftUtil.getMethodAnnotation(_interfaceClass, propertyDescriptor.getName(), airlift.generator.Datable.class);
 
 						String mask = "MM-dd-yyyy HH:mm:ss";
 
 						if (datable != null)
 						{
 							String[] patternArray = datable.dateTimePatterns();
 
 							if (patternArray != null && patternArray.length > 0)
 							{
 								mask = patternArray[0];
 							}
 						}
 
 						value = airlift.util.FormatUtil.format((java.sql.Timestamp) rawValue, mask);
 					}
 					else if (java.util.ArrayList.class.equals(propertyDescriptor.getPropertyType()) == true ||
 							java.util.HashSet.class.equals(propertyDescriptor.getPropertyType()) == true)
 					{
 						value = (rawValue == null) ? null : rawValue;
 					}
 					else
 					{
 						value = (rawValue == null) ? null : rawValue.toString();
 					}
 
 					descriptionMap.put(propertyDescriptor.getName(), value);
 				}
 			}
 
 			return descriptionMap;
 		}		
 		catch(Throwable t)
 		{
 			throw  new RuntimeException(t);
 		}
 	}
 
 	protected static class SqlDateInstanceCreator
 			implements com.google.gson.InstanceCreator<java.sql.Date> {
 		public java.sql.Date createInstance(Type type) {
 			return new java.sql.Date(1L);
 		}
 	}
 
 	protected static class SqlTimestampInstanceCreator
 			implements com.google.gson.InstanceCreator<java.sql.Timestamp> {
 		public java.sql.Timestamp createInstance(Type type) {
 			return new java.sql.Timestamp(1L);
 		}
 	}
 }
