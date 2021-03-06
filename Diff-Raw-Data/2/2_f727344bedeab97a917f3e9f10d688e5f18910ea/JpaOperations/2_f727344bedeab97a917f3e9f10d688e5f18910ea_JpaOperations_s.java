 package org.springframework.roo.addon.jpa;
 
 import java.io.IOException;
 import java.io.InputStream;
 import java.util.Date;
 import java.util.List;
 import java.util.Properties;
 import java.util.logging.Logger;
 
 import org.springframework.roo.metadata.MetadataService;
 import org.springframework.roo.process.manager.FileManager;
 import org.springframework.roo.process.manager.MutableFile;
 import org.springframework.roo.project.Dependency;
 import org.springframework.roo.project.Path;
 import org.springframework.roo.project.PathResolver;
 import org.springframework.roo.project.ProjectMetadata;
 import org.springframework.roo.project.ProjectOperations;
 import org.springframework.roo.project.Repository;
 import org.springframework.roo.support.lifecycle.ScopeDevelopment;
 import org.springframework.roo.support.logging.HandlerUtils;
 import org.springframework.roo.support.util.Assert;
 import org.springframework.roo.support.util.TemplateUtils;
 import org.springframework.roo.support.util.XmlElementBuilder;
 import org.springframework.roo.support.util.XmlUtils;
 import org.w3c.dom.Document;
 import org.w3c.dom.Element;
 
 /**
  * Provides JPA configuration operations.
  * 
  * @author Stefan Schmidt
  * @author Alan Stewart
  * @since 1.0
  */
 @ScopeDevelopment
 public class JpaOperations {
 	private static final Logger logger = HandlerUtils.getLogger(JpaOperations.class);
 	private FileManager fileManager;
 	private PathResolver pathResolver;
 	private MetadataService metadataService;
 	private ProjectOperations projectOperations;
 	
 	private static final String OPENJPA_VERSION = "2.0.0-beta2";
 
 	public JpaOperations(FileManager fileManager, PathResolver pathResolver, MetadataService metadataService, ProjectOperations projectOperations) {
 		Assert.notNull(fileManager, "File manager required");
 		Assert.notNull(pathResolver, "Path resolver required");
 		Assert.notNull(metadataService, "Metadata service required");
 		Assert.notNull(projectOperations, "Project operations required");
 		this.fileManager = fileManager;
 		this.pathResolver = pathResolver;
 		this.metadataService = metadataService;
 		this.projectOperations = projectOperations;
 	}
 
 	public boolean isJpaInstallationPossible() {
 		return metadataService.get(ProjectMetadata.getProjectIdentifier()) != null && !fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml"));
 	}
 
 	public boolean isJpaInstalled() {
 		return metadataService.get(ProjectMetadata.getProjectIdentifier()) != null && fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml"));
 	}
 
 	/**
 	 * This method is responsible for managing all JPA related artifacts (META-INF/persistence.xml, applicationContext.xml, database.properties and the project pom.xml)
 	 * 
 	 * @param ormProvider the ORM provider selected (Hibernate, OpenJpa, EclipseLink)
 	 * @param database the database (HSQL, H2, MySql, etc)
 	 */
 	public void configureJpa(OrmProvider ormProvider, JdbcDatabase database, String jndi) {
 		Assert.notNull(ormProvider, "ORM provider required");
 		Assert.notNull(database, "JDBC database required");
 
 		updatePersistenceXml(ormProvider, database);
 		if (jndi == null || jndi.length() == 0) {
 			updateDatabaseProperties(database);
 		}
 		updateApplicationContext(database, jndi);
 		updateDependencies(ormProvider, database);
 
 		if (ormProvider == OrmProvider.OPENJPA) {
 			installMavenPlugin(); // Install openjpa-maven-plugin for enhancement
 		}
 	}
 
 	private void updateApplicationContext(JdbcDatabase database, String jndi) {
 		String contextPath = pathResolver.getIdentifier(Path.SPRING_CONFIG_ROOT, "applicationContext.xml");
 		MutableFile contextMutableFile = null;
 
 		Document appCtx;
 		try {
 			if (fileManager.exists(contextPath)) {
 				contextMutableFile = fileManager.updateFile(contextPath);
 				appCtx = XmlUtils.getDocumentBuilder().parse(contextMutableFile.getInputStream());
 			} else {
 				throw new IllegalStateException("Could not acquire applicationContext.xml in " + contextPath);
 			}
 		} catch (Exception e) {
 			throw new IllegalStateException(e);
 		}
 
 		Element root = appCtx.getDocumentElement();
 
 		// Checking for existence of configurations, if found abort
 		Element dataSource = XmlUtils.findFirstElement("/beans/bean[@id='dataSource']", root);
 		Element dataSourceJndi = XmlUtils.findFirstElement("/beans/jndi-lookup[@id='dataSource']", root);
 
 		if ((jndi == null || jndi.length() == 0) && dataSource == null) {
 			dataSource = appCtx.createElement("bean");
 			dataSource.setAttribute("class", "org.apache.commons.dbcp.BasicDataSource");
 			dataSource.setAttribute("destroy-method", "close");
 			dataSource.setAttribute("id", "dataSource");
 			dataSource.appendChild(createPropertyElement("driverClassName", "${database.driverClassName}", appCtx));
 			dataSource.appendChild(createPropertyElement("url", "${database.url}", appCtx));
 			dataSource.appendChild(createPropertyElement("username", "${database.username}", appCtx));
 			dataSource.appendChild(createPropertyElement("password", "${database.password}", appCtx));
 			root.appendChild(dataSource);
 			if (dataSourceJndi != null) {
 				dataSourceJndi.getParentNode().removeChild(dataSourceJndi);
 			}
 		} else if (jndi != null && jndi.length() > 0) {
 			if (dataSourceJndi == null) {
 				dataSourceJndi = appCtx.createElement("jee:jndi-lookup");
 				dataSourceJndi.setAttribute("id", "dataSource");
 				root.appendChild(dataSourceJndi);
 			}
 			dataSourceJndi.setAttribute("jndi-name", jndi);
 			if (dataSource != null) {
 				dataSource.getParentNode().removeChild(dataSource);
 			}
 		}
 
 		Element transactionManager = XmlUtils.findFirstElement("/beans/bean[@id='transactionManager']", root);
 		if (transactionManager == null) {
 			transactionManager = appCtx.createElement("bean");
 			transactionManager.setAttribute("id", "transactionManager");
 			transactionManager.setAttribute("class", "org.springframework.orm.jpa.JpaTransactionManager");
 			transactionManager.appendChild(createRefElement("entityManagerFactory", "entityManagerFactory", appCtx));
 			root.appendChild(transactionManager);
 		}
 
 		Element aspectJTxManager = XmlUtils.findFirstElement("/beans/annotation-driven", root);
 		if (aspectJTxManager == null) {
 			aspectJTxManager = appCtx.createElement("tx:annotation-driven");
 			aspectJTxManager.setAttribute("mode", "aspectj");
 			aspectJTxManager.setAttribute("transaction-manager", "transactionManager");
 			root.appendChild(aspectJTxManager);
 		}
 
 		Element entityManagerFactory = XmlUtils.findFirstElement("/beans/bean[@id='entityManagerFactory']", root);
 		if (entityManagerFactory == null) {
 			entityManagerFactory = appCtx.createElement("bean");
 			entityManagerFactory.setAttribute("id", "entityManagerFactory");
 			entityManagerFactory.setAttribute("class", "org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean");
 			entityManagerFactory.appendChild(createRefElement("dataSource", "dataSource", appCtx));
 			root.appendChild(entityManagerFactory);
 		}
 
 		XmlUtils.writeXml(contextMutableFile.getOutputStream(), appCtx);
 	}
 
 	private void updateDatabaseProperties(JdbcDatabase database) {
 		String databasePath = pathResolver.getIdentifier(Path.SPRING_CONFIG_ROOT, "database.properties");
 		MutableFile databaseMutableFile = null;
 		Properties props = new Properties();
 
 		try {
 			if (fileManager.exists(databasePath)) {
 				databaseMutableFile = fileManager.updateFile(databasePath);
 				props.load(databaseMutableFile.getInputStream());
 			} else {
 				databaseMutableFile = fileManager.createFile(databasePath);
 				InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), "database-template.properties");
 				Assert.notNull(templateInputStream, "Could not acquire database properties template");
 				props.load(templateInputStream);
 			}
 		} catch (IOException ioe) {
 			throw new IllegalStateException(ioe);
 		}
 
 		props.put("database.driverClassName", database.getDriverClassName());
 
 		String connectionString = database.getConnectionString();
 		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
 		connectionString = connectionString.replace("TO_BE_CHANGED_BY_ADDON", projectMetadata.getProjectName());
 		props.put("database.url", connectionString);
 
 		if (database.equals(JdbcDatabase.HYPERSONIC_IN_MEMORY) || database.equals(JdbcDatabase.HYPERSONIC_PERSISTENT) || database.equals(JdbcDatabase.H2_IN_MEMORY)) {
 			props.put("database.username", "sa");
 		} else {
 			props.put("database.username", "");
 			logger.warning("Please enter your database details in src/main/resources/META-INF/spring/database.properties.");
 		}
 
 		props.put("database.password", "");
 
 		try {
 			props.store(databaseMutableFile.getOutputStream(), "Updated at " + new Date());
 		} catch (IOException ioe) {
 			throw new IllegalStateException(ioe);
 		}
 	}
 
 	private void updateDependencies(OrmProvider ormProvider, JdbcDatabase database) {
 		InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), "dependencies.xml");
 		Assert.notNull(templateInputStream, "Could not acquire dependencies.xml file");
 		Document dependencyDoc;
 		try {
 			dependencyDoc = XmlUtils.getDocumentBuilder().parse(templateInputStream);
 		} catch (Exception e) {
 			throw new IllegalStateException(e);
 		}
 
 		Element dependencies = (Element) dependencyDoc.getFirstChild();
 
 		List<Element> databaseDepenencies = XmlUtils.findElements("/dependencies/databases/database[@id='" + database.getKey() + "']/dependency", dependencies);
 		for (Element dependency : databaseDepenencies) {
 			projectOperations.dependencyUpdate(new Dependency(dependency));
 		}
 
 		List<Element> ormDepenencies = XmlUtils.findElements("/dependencies/ormProviders/provider[@id='" + ormProvider.getKey() + "']/dependency", dependencies);
 		for (Element dependency : ormDepenencies) {
 			projectOperations.dependencyUpdate(new Dependency(dependency));
 		}
 
 		// Hard coded to JPA & Hibernate Validator for now
 		List<Element> jpaDependencies = XmlUtils.findElements("/dependencies/persistence/provider[@id='JPA']/dependency", dependencies);
 		for (Element dependency : jpaDependencies) {
 			projectOperations.dependencyUpdate(new Dependency(dependency));
 		}
 
 		List<Element> springDependencies = XmlUtils.findElements("/dependencies/spring/dependency", dependencies);
 		for (Element dependency : springDependencies) {
 			projectOperations.dependencyUpdate(new Dependency(dependency));
 		}
 
 		List<Element> repositories = XmlUtils.findElements("/dependencies/ormProviders/provider[@id='" + ormProvider.getKey() + "']/repository", dependencies);
 		for (Element repository : repositories) {
 			Repository repo = new Repository(repository);
 			projectOperations.addRepository(repo.getId(), repo.getName(), repo.getUrl());
 		}
 	}
 
 	private void updatePersistenceXml(OrmProvider ormProvider, JdbcDatabase database) {
 		String persistencePath = pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml");
 		MutableFile persistenceMutableFile = null;
 
 		Document persistence;
 		try {
 			if (fileManager.exists(persistencePath)) {
 				persistenceMutableFile = fileManager.updateFile(persistencePath);
 				persistence = XmlUtils.getDocumentBuilder().parse(persistenceMutableFile.getInputStream());
 			} else {
 				persistenceMutableFile = fileManager.createFile(persistencePath);
 				InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), "persistence-template.xml");
 				Assert.notNull(templateInputStream, "Could not acquire peristence.xml template");
 				persistence = XmlUtils.getDocumentBuilder().parse(templateInputStream);
 			}
 		} catch (Exception e) {
 			throw new IllegalStateException(e);
 		}
 
 		Properties dialects = new Properties();
 		try {
 			InputStream dialectsInputStream = TemplateUtils.getTemplate(getClass(), "jpa-dialects.properties");
 			Assert.notNull(dialectsInputStream, "Could not acquire jpa-dialects.properties");
 			dialects.load(dialectsInputStream);
 		} catch (Exception e) {
 			throw new IllegalStateException(e);
 		}
 
 		Element rootElement = persistence.getDocumentElement();
 		Element persistenceUnit = XmlUtils.findFirstElement("/persistence/persistence-unit", rootElement);
 
 		while (persistenceUnit.getFirstChild() != null) {
 			persistenceUnit.removeChild(persistenceUnit.getFirstChild());
 		}
 
 		// Add provider element
 		Element provider = persistence.createElement("provider");
 		provider.setTextContent(ormProvider.getAdapter());
 		persistenceUnit.appendChild(provider);
 
 		// Add properties
 		Element properties = persistence.createElement("properties");
 		if (ormProvider.equals(OrmProvider.HIBERNATE)) {
 			properties.appendChild(createPropertyElement("hibernate.dialect", dialects.getProperty(ormProvider.getKey() + "." + database.getKey()), persistence));
 			properties.appendChild(persistence.createComment("value='create' to build a new database on each run; value='update' to modify an existing database; value='create-drop' means the same as 'create' but also drops tables when Hibernate closes; value='validate' makes no changes to the database")); // ROO-627
 			properties.appendChild(createPropertyElement("hibernate.hbm2ddl.auto", "create", persistence));
 			properties.appendChild(createPropertyElement("hibernate.ejb.naming_strategy", "org.hibernate.cfg.ImprovedNamingStrategy", persistence));
 		} else if (ormProvider.equals(OrmProvider.OPENJPA)) {
 			properties.appendChild(createPropertyElement("openjpa.jdbc.DBDictionary", dialects.getProperty(ormProvider.getKey() + "." + database.getKey()), persistence));
 			properties.appendChild(persistence.createComment("value='buildSchema' to runtime forward map the DDL SQL; value='validate' makes no changes to the database")); // ROO-627
 			properties.appendChild(createPropertyElement("openjpa.jdbc.SynchronizeMappings", "buildSchema", persistence));
 		} else if (ormProvider.equals(OrmProvider.ECLIPSELINK)) {
 			properties.appendChild(createPropertyElement("eclipselink.target-database", dialects.getProperty(ormProvider.getKey() + "." + database.getKey()), persistence));
 			properties.appendChild(persistence.createComment("value='drop-and-create-tables' to build a new database on each run; value='create-tables' creates new tables if needed; value='none' makes no changes to the database")); // ROO-627
 			properties.appendChild(createPropertyElement("eclipselink.ddl-generation", "drop-and-create-tables", persistence));
 			properties.appendChild(createPropertyElement("eclipselink.ddl-generation.output-mode", "database", persistence));
 			properties.appendChild(createPropertyElement("eclipselink.weaving", "static", persistence));
 		}
 		persistenceUnit.appendChild(properties);
 
 		XmlUtils.writeXml(persistenceMutableFile.getOutputStream(), persistence);
 	}
 
 	private Element createPropertyElement(String name, String value, Document doc) {
 		Element property = doc.createElement("property");
 		property.setAttribute("name", name);
 		property.setAttribute("value", value);
 		return property;
 	}
 
 	private Element createRefElement(String name, String value, Document doc) {
 		Element property = doc.createElement("property");
 		property.setAttribute("name", name);
 		property.setAttribute("ref", value);
 		return property;
 	}
 
 	private void installMavenPlugin() {
 		String pomFilePath = "pom.xml";
 		String pomPath = pathResolver.getIdentifier(Path.ROOT, pomFilePath);
 		MutableFile pomMutableFile = null;
 
 		Document pom;
 		try {
 			if (fileManager.exists(pomPath)) {
 				pomMutableFile = fileManager.updateFile(pomPath);
 				pom = XmlUtils.getDocumentBuilder().parse(pomMutableFile.getInputStream());
 			} else {
 				throw new IllegalStateException("This command cannot be run before a project has been created.");
 			}
 		} catch (Exception e) {
 			throw new IllegalStateException(e);
 		}
 
 		Element root = (Element) pom.getLastChild();
 
 		// Stop if the plugin is already installed
		if (XmlUtils.findFirstElement("/project/build/plugins/plugin[artifactId='maven-antrun-plugin']", root) != null) {
 			return;
 		}
 
 		Element dependencies = XmlUtils.findRequiredElement("/project/dependencies", root);
 		Assert.notNull(dependencies, "Could not find the first dependencies element in pom.xml");
 
 		// Now install the plugin itself
 		Element plugin = new XmlElementBuilder("plugin", pom)
 								.addChild(new XmlElementBuilder("groupId", pom).setText("org.codehaus.mojo").build())
 								.addChild(new XmlElementBuilder("artifactId", pom).setText("openjpa-maven-plugin").build())
 								.addChild(new XmlElementBuilder("version", pom).setText("1.0").build())
 								.addChild(new XmlElementBuilder("configuration", pom)
 											.addChild(new XmlElementBuilder("includes", pom).setText("**/*.class").build())
 											.addChild(new XmlElementBuilder("excludes", pom).setText("**/*_Roo_*.class").build())
 											.addChild(new XmlElementBuilder("addDefaultConstructor", pom).setText("true").build())
 											.addChild(new XmlElementBuilder("toolProperties", pom)
 														.addChild(new XmlElementBuilder("property", pom)
 																	.addChild(new XmlElementBuilder("name", pom).setText("directory").build())
 																	.addChild(new XmlElementBuilder("value", pom).setText("otherdirectoryvalue").build())
 																.build())
 													.build())
 										.build())
 								.addChild(new XmlElementBuilder("executions", pom)
 														.addChild(new XmlElementBuilder("execution", pom)
 																		.addChild(new XmlElementBuilder("id", pom).setText("enhancer").build())
 																		.addChild(new XmlElementBuilder("phase", pom).setText("compile").build())
 																		.addChild(new XmlElementBuilder("goals", pom)
 																						.addChild(new XmlElementBuilder("goal", pom).setText("enhance").build())
 																					.build())
 																	.build())
 													.build())
 								.addChild(new XmlElementBuilder("dependencies", pom)
 														.addChild(new XmlElementBuilder("dependency", pom)
 																		.addChild(new XmlElementBuilder("groupId", pom).setText("org.apache.openjpa").build())
 																		.addChild(new XmlElementBuilder("artifactId", pom).setText("openjpa").build())
 																		.addChild(new XmlElementBuilder("version", pom).setText(OPENJPA_VERSION).build())
 																		.addChild(new XmlElementBuilder("exclusions", pom)
 																						.addChild(new XmlElementBuilder("exclusion", pom)
 																										.addChild(new XmlElementBuilder("groupId", pom).setText("commons-logging").build())
 																										.addChild(new XmlElementBuilder("artifactId", pom).setText("commons-logging").build())
 																									.build())
 																						.addChild(new XmlElementBuilder("exclusion", pom)
 																										.addChild(new XmlElementBuilder("groupId", pom).setText("org.apache.geronimo.specs").build())
 																										.addChild(new XmlElementBuilder("artifactId", pom).setText("geronimo-jms_1.1_spec").build())
 																									.build())
 																					.build())
 																	.build())
 													.build())
 							.build();
 
 		XmlUtils.findRequiredElement("/project/build/plugins", root).appendChild(plugin);
 
 		XmlUtils.writeXml(pomMutableFile.getOutputStream(), pom);
 	}
 }
