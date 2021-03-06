 package com.dhemery.runtimesuite;
 
 import java.lang.annotation.Annotation;
 import java.lang.reflect.Field;
 import java.util.ArrayList;
 import java.util.List;
 
 import org.junit.runner.Description;
 import org.junit.runner.Runner;
 import org.junit.runner.notification.RunNotifier;
 import org.junit.runners.ParentRunner;
 import org.junit.runners.model.InitializationError;
 import org.junit.runners.model.RunnerBuilder;
 
 public class RuntimeSuite extends ParentRunner<Runner> {
 	private List<Class<?>> testClasses;
 	private List<Runner> runners;
 
 	public RuntimeSuite(Class<?> suiteClass, RunnerBuilder builder) throws InitializationError {
 		super(suiteClass);
 		List<Field> classFinderFields = getClassFinderFields(suiteClass);
 		List<Field> classFilterFields = getClassFilterFields(suiteClass);
 
 		Object suite = makeSuite(suiteClass);
 
 		List<Class<?>> candidateClasses = findTestClasses(suite, classFinderFields);
 		testClasses = filterTestClasses(suite, classFilterFields, candidateClasses);
 
 		runners = makeRunners(builder, testClasses);
 	}
 
 	protected Description describeChild(Runner child) {
 		return child.getDescription();
 	}
 
 	private List<Class<?>> filterTestClasses(Object suite, List<Field> filterFields, List<Class<?>> candidateClasses) throws InitializationError {
 		List<Class<?>> result = new ArrayList<Class<?>>();
 		result.addAll(candidateClasses);
 		for(Field filterField : filterFields) {
 			ClassFilter filter = (ClassFilter) getMember(suite, filterField);
 			result = filter.filter(result);
 		}
 		return result;
 	}
 
 	private List<Field> findMatchingFields(Class<?> suiteClass, Class<? extends Annotation> requiredAnnotation, Class<?> requiredType) {
 		Field[] fields = suiteClass.getFields();
 		List<Field> result = new ArrayList<Field>();
 		for(Field field : fields) {
 			if(hasAnnotation(field, requiredAnnotation) && hasType(field, requiredType)) {
 				result.add(field);
 			}
 		}
 		return result;
 	}
 
 	private List<Class<?>> findTestClasses(Object suite, List<Field> finderFields) throws InitializationError {
 		List<Class<?>> result = new ArrayList<Class<?>>();
 		for(Field finderField : finderFields) {
 			ClassFinder finder = (ClassFinder) getMember(suite, finderField);
 			result.addAll(finder.find());
 		}
 		return result;
 	}
 
 	protected List<Runner> getChildren() {	
 		return getRunners();
 	}
 
 	private List<Field> getClassFilterFields(Class<?> suiteClass) {
 		return findMatchingFields(suiteClass, Filter.class, ClassFilter.class);
 	}
 
 	private List<Field> getClassFinderFields(Class<?> suiteClass) {
 		return findMatchingFields(suiteClass, Finder.class, ClassFinder.class);
 	}
 
 	private Object getMember(Object suite, Field memberField) throws InitializationError {
 		try {
 			return memberField.get(suite);
 		} catch (Throwable cause) {
 			throw new InitializationError(cause);
 		}
 	}
 
 	public List<Runner> getRunners() {
 		return runners;
 	}
 
 	public List<Class<?>> getTestClasses() {
 		return testClasses;
 	}
 
 	private boolean hasAnnotation(Field field, Class<? extends Annotation> requiredAnnotation) {
 		return field.isAnnotationPresent(requiredAnnotation);
 	}
 
 	private boolean hasType(Field field, Class<?> requiredType) {
		return requiredType.isAssignableFrom(field.getType());
 	}
 
 	private Runner makeRunner(RunnerBuilder builder, Class<?> testClass) {
 		return builder.safeRunnerForClass(testClass);
 	}
 
 	private List<Runner> makeRunners(RunnerBuilder builder, List<Class<?>> testClasses) {
 		List<Runner> runners = new ArrayList<Runner>();
 		for(Class<?> testClass : testClasses) {
 			runners.add(makeRunner(builder, testClass));
 		}
 		return runners;
 	}
 
 	private Object makeSuite(Class<?> suiteClass) throws InitializationError {
 		try {
 			return suiteClass.newInstance();
 		} catch (Throwable cause) {
 			throw new InitializationError(cause);
 		}
 	}
 
 	protected void runChild(Runner child, RunNotifier notifier) {
 		child.run(notifier);
 	}
 }
