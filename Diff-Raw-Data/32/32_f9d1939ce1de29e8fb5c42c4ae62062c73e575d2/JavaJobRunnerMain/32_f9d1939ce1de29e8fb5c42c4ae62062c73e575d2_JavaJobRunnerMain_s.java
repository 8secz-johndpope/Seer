 /*
  * Copyright 2010 LinkedIn, Inc
  * 
  * Licensed under the Apache License, Version 2.0 (the "License"); you may not
  * use this file except in compliance with the License. You may obtain a copy of
  * the License at
  * 
  * http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  * License for the specific language governing permissions and limitations under
  * the License.
  */
 package azkaban.jobExecutor;
 
 import azkaban.utils.Props;
 import azkaban.utils.SecurityUtils;
 
 import org.apache.hadoop.conf.Configuration;
 import org.apache.log4j.ConsoleAppender;
 import org.apache.log4j.Layout;
 import org.apache.log4j.Logger;
 import org.apache.log4j.PatternLayout;
 
 import java.io.BufferedOutputStream;
 import java.io.BufferedReader;
 import java.io.FileOutputStream;
 import java.io.FileReader;
 import java.io.IOException;
 import java.io.OutputStream;
 import java.lang.reflect.Constructor;
 import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Method;
 import java.security.PrivilegedExceptionAction;
 import java.util.HashMap;
 import java.util.LinkedHashMap;
 import java.util.Map;
 import java.util.Properties;
 
 public class JavaJobRunnerMain {
 
 	public static final String JOB_CLASS = "job.class";
 	public static final String DEFAULT_RUN_METHOD = "run";
 	public static final String DEFAULT_CANCEL_METHOD = "cancel";
 
 	// This is the Job interface method to get the properties generated by the
 	// job.
 	public static final String GET_GENERATED_PROPERTIES_METHOD = "getJobGeneratedProperties";
 
 	public static final String CANCEL_METHOD_PARAM = "method.cancel";
 	public static final String RUN_METHOD_PARAM = "method.run";
 	public static final String[] PROPS_CLASSES = new String[] { "azkaban.utils.Props", "azkaban.common.utils.Props" };
 
 	private static final Layout DEFAULT_LAYOUT = new PatternLayout("%p %m\n");
 
 	public final Logger _logger;
 
 	public String _cancelMethod;
 	public String _jobName;
 	public Object _javaObject;
 	private boolean _isFinished = false;
 
 	public static void main(String[] args) throws Exception {
 		@SuppressWarnings("unused")
 		JavaJobRunnerMain wrapper = new JavaJobRunnerMain();
 	}
 
 	public JavaJobRunnerMain() throws Exception {
 		Runtime.getRuntime().addShutdownHook(new Thread() {
 			public void run() {
 				cancelJob();
 			}
 		});
 
 		try {
 			_jobName = System.getenv(ProcessJob.JOB_NAME_ENV);
 			String propsFile = System.getenv(ProcessJob.JOB_PROP_ENV);
 
 			_logger = Logger.getRootLogger();
 			_logger.removeAllAppenders();
 			ConsoleAppender appender = new ConsoleAppender(DEFAULT_LAYOUT);
 			appender.activateOptions();
 			_logger.addAppender(appender);
 
 			Properties prop = new Properties();
 			prop.load(new BufferedReader(new FileReader(propsFile)));
 
 			_logger.info("Running job " + _jobName);
 			String className = prop.getProperty(JOB_CLASS);
 			if (className == null) {
 				throw new Exception("Class name is not set.");
 			}
 			_logger.info("Class name " + className);
 
 			// Create the object using proxy
 			if (SecurityUtils.shouldProxy(prop)) {
 				_javaObject = getObjectAsProxyUser(prop, _logger, _jobName, className);
 			} else {
 				_javaObject = getObject(_jobName, className, prop, _logger);
 			}
 			if (_javaObject == null) {
 				_logger.info("Could not create java object to run job: " + className);
 				throw new Exception("Could not create running object");
 			}
 
 			_cancelMethod = prop.getProperty(CANCEL_METHOD_PARAM, DEFAULT_CANCEL_METHOD);
 
 			final String runMethod = prop.getProperty(RUN_METHOD_PARAM, DEFAULT_RUN_METHOD);
 			_logger.info("Invoking method " + runMethod);
 
 			if (SecurityUtils.shouldProxy(prop)) {
 				_logger.info("Proxying enabled.");
 				runMethodAsProxyUser(prop, _javaObject, runMethod);
 			} else {
 				_logger.info("Proxy check failed, not proxying run.");
 				runMethod(_javaObject, runMethod);
 			}
 			_isFinished = true;
 
 			// Get the generated properties and store them to disk, to be read
 			// by ProcessJob.
 			try {
 				final Method generatedPropertiesMethod = _javaObject.getClass().getMethod(
 						GET_GENERATED_PROPERTIES_METHOD, new Class<?>[] {});
				Props outputGendProps = (Props) generatedPropertiesMethod.invoke(_javaObject, new Object[] {});
				outputGeneratedProperties(outputGendProps);
 			} catch (NoSuchMethodException e) {
 				_logger.info(String.format(
 						"Apparently there isn't a method[%s] on object[%s], using empty Props object instead.",
 						GET_GENERATED_PROPERTIES_METHOD, _javaObject));
 				outputGeneratedProperties(new Props());
 			}
 		} catch (Exception e) {
 			_isFinished = true;
 			throw e;
 		}
 	}
 
 	private void runMethodAsProxyUser(Properties prop, final Object obj, final String runMethod) throws IOException,
 			InterruptedException {
 		SecurityUtils.getProxiedUser(prop, _logger, new Configuration()).doAs(new PrivilegedExceptionAction<Void>() {
 			@Override
 			public Void run() throws Exception {
 				runMethod(obj, runMethod);
 				return null;
 			}
 		});
 	}
 
 	private void runMethod(Object obj, String runMethod) throws IllegalAccessException, InvocationTargetException,
 			NoSuchMethodException {
 		obj.getClass().getMethod(runMethod, new Class<?>[] {}).invoke(obj);
 	}
 
 	private void outputGeneratedProperties(Props outputProperties) {
 		_logger.info("Outputting generated properties to " + ProcessJob.JOB_OUTPUT_PROP_FILE);
 
 		if (outputProperties == null) {
 			_logger.info("  no gend props");
 			return;
 		}
 		for (String key : outputProperties.getKeySet()) {
 			_logger.info("  gend prop " + key + " value:" + outputProperties.get(key));
 		}
 
 		String outputFileStr = System.getenv(ProcessJob.JOB_OUTPUT_PROP_FILE);
 		if (outputFileStr == null) {
 			return;
 		}
 
 		Map<String, String> properties = new LinkedHashMap<String, String>();
 		for (String key : outputProperties.getKeySet()) {
 			properties.put(key, outputProperties.get(key));
 		}
 
 		OutputStream writer = null;
 		try {
 			writer = new BufferedOutputStream(new FileOutputStream(outputFileStr));
 			// Manually serialize into JSON instead of adding org.json to
 			// external classpath
 			writer.write("{\n".getBytes());
 			for (Map.Entry<String, String> entry : properties.entrySet()) {
 				writer.write(String.format("  '%s':'%s',\n", entry.getKey().replace("'", "\\'"),
 						entry.getValue().replace("'", "\\'")).getBytes());
 			}
 			writer.write("}".getBytes());
 		} catch (Exception e) {
 			new RuntimeException("Unable to store output properties to: " + outputFileStr);
 		} finally {
 			try {
 				writer.close();
 			} catch (IOException e) {
 			}
 		}
 	}
 
 	public void cancelJob() {
 		if (_isFinished) {
 			return;
 		}
 		_logger.info("Attempting to call cancel on this job");
 		if (_javaObject != null) {
 			Method method = null;
 
 			try {
 				method = _javaObject.getClass().getMethod(_cancelMethod);
 			} catch (SecurityException e) {
 			} catch (NoSuchMethodException e) {
 			}
 
 			if (method != null)
 				try {
 					method.invoke(_javaObject);
 				} catch (Exception e) {
 					if (_logger != null) {
 						_logger.error("Cancel method failed! ", e);
 					}
 				}
 			else {
 				throw new RuntimeException("Job " + _jobName + " does not have cancel method " + _cancelMethod);
 			}
 		}
 	}
 
 	private static Object getObjectAsProxyUser(final Properties prop, final Logger logger, final String jobName,
 			final String className) throws Exception {
 
 		Object obj = SecurityUtils.getProxiedUser(prop, logger, new Configuration()).doAs(
 				new PrivilegedExceptionAction<Object>() {
 					@Override
 					public Object run() throws Exception {
 						return getObject(jobName, className, prop, logger);
 					}
 				});
 
 		return obj;
 	}
 
 	private static Object getObject(String jobName, String className, Properties properties, Logger logger)
 			throws Exception {
 
 		Class<?> runningClass = JavaJobRunnerMain.class.getClassLoader().loadClass(className);
 
 		if (runningClass == null) {
 			throw new Exception("Class " + className + " was not found. Cannot run job.");
 		}
 
 		Class<?> propsClass = null;
 		for (String propClassName : PROPS_CLASSES) {
 			propsClass = JavaJobRunnerMain.class.getClassLoader().loadClass(propClassName);
			if (propsClass != null) {
 				break;
 			}
 		}
 
 		Object obj = null;
 		if (propsClass != null && getConstructor(runningClass, String.class, propsClass) != null) {
 			// Create props class
 			Constructor<?> propsCon = getConstructor(propsClass, propsClass, Properties[].class);
 			Object props = propsCon.newInstance(null, new Properties[] { properties });
 
 			Constructor<?> con = getConstructor(runningClass, String.class, propsClass);
 			logger.info("Constructor found " + con.toGenericString());
 			obj = con.newInstance(jobName, props);
 		} else if (getConstructor(runningClass, String.class, Properties.class) != null) {
 			
 			Constructor<?> con = getConstructor(runningClass, String.class, Properties.class);
 			logger.info("Constructor found " + con.toGenericString());
 			obj = con.newInstance(jobName, properties);
 		} else if (getConstructor(runningClass, String.class, Map.class) != null) {
 			Constructor<?> con = getConstructor(runningClass, String.class, Map.class);
 			logger.info("Constructor found " + con.toGenericString());
 
 			@SuppressWarnings("rawtypes")
 			HashMap map = new HashMap();
 			for (Map.Entry<Object, Object> entry : properties.entrySet()) {
 				map.put(entry.getKey(), entry.getValue());
 			}
 			obj = con.newInstance(jobName, map);
 		} else if (getConstructor(runningClass, String.class) != null) {
 			Constructor<?> con = getConstructor(runningClass, String.class);
 			logger.info("Constructor found " + con.toGenericString());
 			obj = con.newInstance(jobName);
 		} else if (getConstructor(runningClass) != null) {
 			Constructor<?> con = getConstructor(runningClass);
 			logger.info("Constructor found " + con.toGenericString());
 			obj = con.newInstance();
 		} else {
 			logger.error("Constructor not found. Listing available Constructors.");
 			for (Constructor<?> c : runningClass.getConstructors()) {
 				logger.info(c.toGenericString());
 			}
 		}
 		return obj;
 	}
 
 	private static Constructor<?> getConstructor(Class<?> c, Class<?>... args) {
 		try {
 			Constructor<?> cons = c.getConstructor(args);
 			return cons;
 		} catch (NoSuchMethodException e) {
 			return null;
 		}
 	}
 
 }
