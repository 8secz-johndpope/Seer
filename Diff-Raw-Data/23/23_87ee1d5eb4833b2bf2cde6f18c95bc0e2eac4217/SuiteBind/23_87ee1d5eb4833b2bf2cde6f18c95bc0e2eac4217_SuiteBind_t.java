 package de.jbee.inject.bind;
 
 import org.junit.runner.RunWith;
 import org.junit.runners.Suite;
 import org.junit.runners.Suite.SuiteClasses;
 
 @RunWith ( Suite.class )
 @SuiteClasses ( { TestAutobindBinds.class, TestElementBinds.class, TestInstanceBinds.class,
 		TestServiceMethodBinds.class, TestServiceBinds.class, TestSupplierBinds.class,
 		TestTypeBinds.class, TestBootstrapper.class, TestPackageLocalisedBinds.class,
 		TestEditionFeatureBinds.class, TestConstantModularBinds.class, TestTargetedBinds.class,
		TestLoggerBinds.class, TestRobotLegsProblemBinds.class, TestHintConstructorBinds.class } )
 public class SuiteBind {
 	// all tests in the bind package
 }
