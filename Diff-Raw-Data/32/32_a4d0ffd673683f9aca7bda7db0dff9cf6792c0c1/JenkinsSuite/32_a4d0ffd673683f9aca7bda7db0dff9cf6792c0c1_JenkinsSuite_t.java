 package org.jboss.tools.hb.ui.bot.suite;
 
 import org.jboss.tools.hb.ui.bot.test.configuration.CreateConfigurationFileTest;
 import org.jboss.tools.hb.ui.bot.test.mappingfile.CreateMappingFileTest;
 import org.jboss.tools.hb.ui.bot.test.mappingfile.EditMappingFileTest;
 import org.jboss.tools.hb.ui.bot.test.perspective.PerspectiveTest;
 import org.jboss.tools.hb.ui.bot.test.reveng.CreateRevengFileTest;
 import org.jboss.tools.hb.ui.bot.test.view.JPADetailViewTest;
 import org.jboss.tools.hb.ui.bot.test.view.PackageInfoTest;
 import org.jboss.tools.ui.bot.ext.RequirementAwareSuite;
 import org.junit.runner.RunWith;
 import org.junit.runners.Suite.SuiteClasses;
 
 /**
  * Suite of tests for to be executed on jenkins slave
  * Same test from Hibernate suite are not here because of known issues
  * @author jpeterka
  *
  */
 @RunWith(RequirementAwareSuite.class)
 @SuiteClasses({
 	PerspectiveTest.class,
 	CreateConfigurationFileTest.class,
 	// EditConfigurationFileTest.class - Multipage editor must be fixed for Juno
 	CreateRevengFileTest.class,
 	CreateMappingFileTest.class,
 	EditMappingFileTest.class,
	//JPADetailViewTest.class,  // failing, investigate
	//PackageInfoTest.class		// failing, investigate
  	})
 public class JenkinsSuite {
 
 }
