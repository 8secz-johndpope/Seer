 /*
  * Copyright 2008-2010 by Emeric Vernat
  *
  *     This file is part of Java Melody.
  *
  * Java Melody is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Lesser General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * Java Melody is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public License
  * along with Java Melody.  If not, see <http://www.gnu.org/licenses/>.
  */
 package net.bull.javamelody;
 
 import java.lang.reflect.Field;
 import java.security.AccessController;
 import java.security.PrivilegedAction;
 import java.util.Collections;
 import java.util.Hashtable;
 import java.util.LinkedHashMap;
 import java.util.Map;
 
 import javax.naming.Context;
 import javax.naming.InitialContext;
 import javax.naming.NameClassPair;
 import javax.naming.NameNotFoundException;
 import javax.naming.NamingException;
 import javax.naming.NoInitialContextException;
 import javax.servlet.ServletContext;
 import javax.sql.DataSource;
 
 /**
  * Classe utilitaire pour JdbcWrapper.
  * @author Emeric Vernat
  */
 final class JdbcWrapperHelper {
 	private static final Map<String, DataSource> SPRING_DATASOURCES = new LinkedHashMap<String, DataSource>();
 	private static final Map<String, DataSource> JNDI_DATASOURCES_BACKUP = new LinkedHashMap<String, DataSource>();
 
 	private JdbcWrapperHelper() {
 		super();
 	}
 
 	static void registerSpringDataSource(String beanName, DataSource dataSource) {
 		SPRING_DATASOURCES.put(beanName, dataSource);
 	}
 
 	static void rebindDataSource(ServletContext servletContext, String jndiName,
 			DataSource dataSource, DataSource dataSourceProxy) throws Throwable {
 		final Object securityToken = changeContextWritable(servletContext, null);
 		final InitialContext initialContext = new InitialContext();
 		initialContext.rebind(jndiName, dataSourceProxy);
 		JNDI_DATASOURCES_BACKUP.put(jndiName, dataSource);
 		changeContextWritable(servletContext, securityToken);
 		initialContext.close();
 	}
 
 	static void rebindInitialDataSources(ServletContext servletContext) throws Throwable {
 		try {
 			final InitialContext initialContext = new InitialContext();
 			for (final Map.Entry<String, DataSource> entry : JNDI_DATASOURCES_BACKUP.entrySet()) {
 				final String jndiName = entry.getKey();
 				final DataSource dataSource = entry.getValue();
 				final Object securityToken = changeContextWritable(servletContext, null);
 				initialContext.rebind(jndiName, dataSource);
 				changeContextWritable(servletContext, securityToken);
 			}
 			initialContext.close();
 		} finally {
 			JNDI_DATASOURCES_BACKUP.clear();
 		}
 	}
 
 	static Map<String, DataSource> getJndiAndSpringDataSources() throws NamingException {
 		Map<String, DataSource> dataSources;
 		try {
 			dataSources = new LinkedHashMap<String, DataSource>(getJndiDataSources());
 		} catch (final NoInitialContextException e) {
 			dataSources = new LinkedHashMap<String, DataSource>();
 		}
 		dataSources.putAll(SPRING_DATASOURCES);
 		return dataSources;
 	}
 
 	static Map<String, DataSource> getJndiDataSources() throws NamingException {
 		final Map<String, DataSource> dataSources = new LinkedHashMap<String, DataSource>(2);
 		final String datasourcesParameter = Parameters.getParameter(Parameter.DATASOURCES);
 		if (datasourcesParameter == null) {
 			dataSources.putAll(getJndiDataSourcesAt("java:comp/env/jdbc"));
 			// pour jboss sans jboss-env.xml ou sans resource-ref dans web.xml :
 			dataSources.putAll(getJndiDataSourcesAt("java:/jdbc"));
 			// pour JavaEE 6 :
 			// (voir par exemple http://smokeandice.blogspot.com/2009/12/datasourcedefinition-hidden-gem-from.html)
 			dataSources.putAll(getJndiDataSourcesAt("java:global/jdbc"));
 		} else if (datasourcesParameter.trim().length() != 0) { // NOPMD
 			final InitialContext initialContext = new InitialContext();
 			for (final String datasource : datasourcesParameter.split(",")) {
 				final String jndiName = datasource.trim();
 				// ici, on n'ajoute pas java:/comp/env
 				// et on suppose qu'il n'en faut pas ou que cela a été ajouté dans le paramétrage
 				final DataSource dataSource = (DataSource) initialContext.lookup(jndiName);
 				dataSources.put(jndiName, dataSource);
 			}
 			initialContext.close();
 		}
 		return Collections.unmodifiableMap(dataSources);
 	}
 
 	private static Map<String, DataSource> getJndiDataSourcesAt(String jndiPrefix)
 			throws NamingException {
 		final InitialContext initialContext = new InitialContext();
 		final Map<String, DataSource> dataSources = new LinkedHashMap<String, DataSource>(2);
 		try {
 			for (final NameClassPair nameClassPair : Collections.list(initialContext
 					.list(jndiPrefix))) {
 				// note: il ne suffit pas de tester
 				// (DataSource.class.isAssignableFrom(Class.forName(nameClassPair.getClassName())))
 				// car nameClassPair.getClassName() vaut "javax.naming.LinkRef" sous jboss 5.1.0.GA
 				// par exemple, donc on fait le lookup pour voir
 				final String jndiName;
 				if (nameClassPair.getName().startsWith("java:")) {
 					// pour glassfish v3
 					jndiName = nameClassPair.getName();
 				} else {
 					jndiName = jndiPrefix + '/' + nameClassPair.getName();
 				}
 				final Object value = initialContext.lookup(jndiName);
 				if (value instanceof DataSource) {
 					dataSources.put(jndiName, (DataSource) value);
 				}
 			}
 		} catch (final NameNotFoundException e) {
 			return dataSources;
 			// le préfixe "comp/env/jdbc" ou "/jdbc" n'existe pas dans jndi
 		}
 		initialContext.close();
 		return dataSources;
 	}
 
 	@SuppressWarnings("all")
 	// CHECKSTYLE:OFF
 	private static Object changeContextWritable(ServletContext servletContext, Object securityToken)
 			throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException,
 			NamingException {
 		// cette méthode ne peut pas être utilisée avec un simple JdbcDriver
 		assert servletContext != null;
 		if (servletContext.getServerInfo().contains("Tomcat")
 				&& System.getProperty("jonas.name") == null) {
 			// on n'exécute cela que si c'est tomcat
 			// et si ce n'est pas tomcat dans jonas
 			final Field field = Class.forName("org.apache.naming.ContextAccessController")
 					.getDeclaredField("readOnlyContexts");
 			setFieldAccessible(field);
 			@SuppressWarnings("unchecked")
 			final Hashtable<String, Object> readOnlyContexts = (Hashtable<String, Object>) field
 					.get(null);
 			// contextPath vaut /myapp par exemple
 			final String contextName = "/Catalina/localhost"
 					+ Parameters.getContextPath(servletContext);
 			if (securityToken == null) {
 				// on rend le contexte writable
 				return readOnlyContexts.remove(contextName);
 			}
 			// on remet le contexte not writable comme avant
 			readOnlyContexts.put(contextName, securityToken);
 
 			return null;
 		} else if (servletContext.getServerInfo().contains("jetty")) {
 			// on n'exécute cela que si c'est jetty
 			final Context jdbcContext = (Context) new InitialContext().lookup("java:comp");
 			final Field field = getAccessibleField(jdbcContext, "_env");
 			@SuppressWarnings("unchecked")
 			final Hashtable<Object, Object> env = (Hashtable<Object, Object>) field
 					.get(jdbcContext);
 			if (securityToken == null) {
 				// on rend le contexte writable
 				return env.remove("org.mortbay.jndi.lock");
 			}
 			// on remet le contexte not writable comme avant
 			env.put("org.mortbay.jndi.lock", securityToken);
 
 			return null;
 		}
 		return null;
 	}
 
 	static Object getFieldValue(Object object, String fieldName) throws IllegalAccessException {
 		return getAccessibleField(object, fieldName).get(object);
 	}
 
 	static void setFieldValue(Object object, String fieldName, Object value)
 			throws IllegalAccessException {
 		getAccessibleField(object, fieldName).set(object, value);
 	}
 
 	private static Field getAccessibleField(Object object, String fieldName) {
 		assert fieldName != null;
 		Class<?> classe = object.getClass();
 		Field result = null;
 		do {
 			for (final Field field : classe.getDeclaredFields()) {
 				if (fieldName.equals(field.getName())) {
 					result = field;
 					break;
 				}
 			}
 			classe = classe.getSuperclass();
 		} while (result == null && classe != null);
 
 		assert result != null;
 		setFieldAccessible(result);
 		return result;
 	}
 
 	private static void setFieldAccessible(final Field field) {
 		AccessController.doPrivileged(new PrivilegedAction<Object>() { // pour findbugs
 					/** {@inheritDoc} */
 					public Object run() {
 						field.setAccessible(true);
 						return null;
 					}
 				});
 	}
 	// CHECKSTYLE:ON
 }
