 /******************************************************************************
 * Copyright (c) 2011 Institute for Software, HSR Hochschule fuer Technik 
 * Rapperswil, University of applied sciences and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html 
 *
 * Contributors:
 * 	Ueli Kunz <kunz@ideadapt.net>, Jules Weder <julesweder@gmail.com> - initial API and implementation
 ******************************************************************************/
 
 package ch.hsr.ifs.cdt.metriculator.model.test;
 
 import org.eclipse.core.runtime.CoreException;
 
 import ch.hsr.ifs.cdt.metriculator.MetriculatorPluginActivator;
 import ch.hsr.ifs.cdt.metriculator.checkers.LSLOCMetric;
 import ch.hsr.ifs.cdt.metriculator.checkers.LSLOCMetricChecker;
 import ch.hsr.ifs.cdt.metriculator.model.AbstractMetric;
 import ch.hsr.ifs.cdt.metriculator.model.AbstractMetricChecker;
 import ch.hsr.ifs.cdt.metriculator.model.TreePrinter;
 import ch.hsr.ifs.cdt.metriculator.model.nodes.AbstractNode;
 import ch.hsr.ifs.cdt.metriculator.model.nodes.WorkspaceNode;
 import ch.hsr.ifs.cdt.metriculator.test.MetriculatorCheckerTestCase;
 
 public class TreeBuilderIndexerTest extends MetriculatorCheckerTestCase {
 	AbstractNode root;
 	private AbstractMetricChecker checker;
 	private AbstractMetric metric;
 
 	@Override
 	public void setUp() throws Exception {
 		super.setUp();
 
 		System.out.println(getName());
 		root = new WorkspaceNode("rootnotmodified");
 
 		enableProblems(LSLOCMetricChecker.LSLOC_PROBLEM_ID);
 		MetriculatorPluginActivator.getDefault().resetTreeBuilders();
 
 		if (checker == null) {
 			checker = AbstractMetricChecker.getChecker(LSLOCMetricChecker.class);
 			metric = new LSLOCMetric(checker, "LSLOC", "lines of code");
 		}
 	}
 
 	@Override
 	public void tearDown() throws CoreException {
 		super.tearDown();
 
 		TreePrinter.printTree(root, metric);
 	}
 
 	
 	//	namespace {
 	//	}
 	//
 	//	namespace {
 	//	}
 	public void testCreateHybridWithAnonymousAstNodesOnSameLevel(){ 		
 		loadCodeAndRun(getAboveComment());
 		
 		root = MetriculatorPluginActivator.getDefault().getHybridTreeBuilder().root;
 		
 		AbstractNode file1 = root.getChildren().iterator().next().getChildren().iterator().next();
 		assertEquals(2, file1.getChildren().size());
 	}
 	
 	//	namespace N {
 	//	}
 	//
 	//	namespace N {
 	//	}
 	public void testCreateHybridWithAstNodesOfSameNameOnSameLevel(){		
 		loadCodeAndRun(getAboveComment());
 		
 		root = MetriculatorPluginActivator.getDefault().getHybridTreeBuilder().root;
 		
 		AbstractNode file1 = root.getChildren().iterator().next().getChildren().iterator().next();
 		assertEquals(2, file1.getChildren().size());
 	}	
 	
 	//	namespace N {
 	//	}
 	//
 	//	namespace N {
 	//	}
 	public void testCreateLogicAstNodesWithSameNameOnSameLevel(){ 		
 		loadCodeAndRun(getAboveComment());
 		
 		root = MetriculatorPluginActivator.getDefault().getLogicTreeBuilder().root;
 		
 		assertEquals(1, root.getChildren().size());
 	}	
 	
 	//	namespace {
 	//	}
 	//
 	//	namespace {
 	//	}
 	public void testCreateLogicWithAnonymousAstNodesOnSameLevel(){ 		
 		loadCodeAndRun(getAboveComment());
 		
 		root = MetriculatorPluginActivator.getDefault().getLogicTreeBuilder().root;
 					
 		assertEquals(2, root.getChildren().size());
 	}
 
 	//	class MyClass {
 	//	public:
 	//		MyClass();
 	//		virtual ~MyClass();
 	//		int JulesIndex();
 	//	};
 	//
 	//	int MyClass::JulesIndex(){
 	//		return 100;
 	//	}
 	public void testMergeOfMemberFunctions(){
 		loadCodeAndRun(getAboveComment());
 
 		root = MetriculatorPluginActivator.getDefault().getLogicTreeBuilder().root;
 
 		assertEquals(3, root.getChildren().iterator().next().getChildren().size());
 	}
 
 	//	class MyClass {			
 	//	public:
 	//		MyClass(){
 	//			int i = 1;
 	//		}
 	//		virtual ~MyClass();
 	//		int JulesIndex();
 	//	};
 	//	
 	//	MyClass::~MyClass() {
 	//	}
 	//
 	//	int MyClass::JulesIndex(){
 	//		return 100;
 	//	}
 	public void testMergeOfMemberFunctionsWithInternDefinition(){
 		loadCodeAndRun(getAboveComment());
		runOnProject();
 
 		root = MetriculatorPluginActivator.getDefault().getLogicTreeBuilder().root;
 
 		assertEquals(3, root.getChildren().iterator().next().getChildren().size());
 
 	}
 
 	//	class MyClass {			
 	//	public:
 	//		MyClass(){
 	//			int i = 1;
 	//		}
 	//		virtual ~MyClass();
 	//		int JulesIndex(int i);
 	//	};
 	//	
 	//	MyClass::~MyClass() {
 	//	}
 	//
 	//	int MyClass::JulesIndex(int i){
 	//		return 100;
 	//	}
 	public void testMergeOfMemberFunctionsWithInternDefinition2(){
 		loadCodeAndRun(getAboveComment());
 
 		root = MetriculatorPluginActivator.getDefault().getLogicTreeBuilder().root;
 
 		assertEquals(3, root.getChildren().iterator().next().getChildren().size());
 
 	}
 
 	//	class MyClass {			
 	//	public:
 	//		MyClass(){
 	//			int i = 1;
 	//		}
 	//		virtual ~MyClass();
 	//		int JulesIndex(int);
 	//	};
 	//	
 	//	MyClass::~MyClass() {
 	//	}
 	//
 	//	int MyClass::JulesIndex(int i){
 	//		return 100;
 	//	}
 	public void testMergeOfMemberFunctionsWithInternDefinition3(){
 		loadCodeAndRun(getAboveComment());
 
 		root = MetriculatorPluginActivator.getDefault().getLogicTreeBuilder().root;
 
 		assertEquals(3, root.getChildren().iterator().next().getChildren().size());
 
 	}
 
 	//	namespace testOwner{
 	//		class Laser {
 	//		public:
 	//			Laser();
 	//			virtual ~Laser();
 	//		};
 	//	}
 	//	
 	//	namespace testOwner{
 	//		Laser::Laser() {
 	//		}
 	//		Laser::~Laser() {
 	//		}
 	//	}	
 	public void testMergOfMemberFunctionsInNamespace(){
 		loadCodeAndRun(getAboveComment());
 
 		root = MetriculatorPluginActivator.getDefault().getLogicTreeBuilder().root;
 
 		assertEquals(2, root.getChildren().iterator().next().getChildren().iterator().next().getChildren().size());
 	}
 
 	//int forwardFunc(int);
 	//
 	//int forwardFunc(int i){
 	//	return 0;
 	//}
 	//
 	//int main(){
 	//	return forwardFunc(1);
 	//}
 	public void testMergOfFunctionDefinitionAndDeclarationInSameFile1(){
		loadCodeAndRun(getAboveComment());
 		runOnProject();
 		
 		root = MetriculatorPluginActivator.getDefault().getLogicTreeBuilder().root;
 		
 		assertEquals(2, root.getChildren().size());
 		assertTrue(root.getChildren().iterator().next().getNodeInfo().isFunctionDefinition());
 		assertEquals(0, root.getChildren().iterator().next().getChildren().size());
 	}
 
 	//int forwardFunc(int i);
 	//
 	//int forwardFunc(int i){
 	//	return 0;
 	//}
 	//
 	//int main(){
 	//	return forwardFunc(1);
 	//}
 	public void testMergOfFunctionDefinitionAndDeclarationInSameFile2(){
		loadCodeAndRun(getAboveComment());
 		runOnProject();
 		
 		root = MetriculatorPluginActivator.getDefault().getLogicTreeBuilder().root;
 		
 		assertEquals(2, root.getChildren().size());
 		assertTrue(root.getChildren().iterator().next().getNodeInfo().isFunctionDefinition());
 		assertEquals(0, root.getChildren().iterator().next().getChildren().size());
 	}
 
 	//	class MyClass {			
 	//	public:
 	//		MyClass(){
 	//			int i = 1;
 	//		}
 	//		virtual ~MyClass();
 	//		struct InnerStruct;
 	//	};
 	//	
 	//	MyClass::~MyClass() {
 	//	}
 	//	struct MyClass::InnerStruct{
 	//	};
 	public void testMergeOfNestedTypeDeclarationAndDefition(){
 		loadCodeAndRun(getAboveComment());
 
 		root = MetriculatorPluginActivator.getDefault().getLogicTreeBuilder().root;
 
 		assertEquals(3, root.getChildren().iterator().next().getChildren().size());
 	}
 
 	//	namespace N {
 	//		struct A {
 	//		    virtual void fx() { }
 	//		};
 	//	}
 	//
 	//	namespace N {
 	//		struct B {
 	//		    virtual void f() { }
 	//		    virtual void f1() { }
 	//		};
 	//	}
 	public void testMergeOfFunctionsInNamespaces(){
 		loadCodeAndRun(getAboveComment());
 
 		root = MetriculatorPluginActivator.getDefault().getLogicTreeBuilder().root;
 
 		assertEquals(1, root.getChildren().size());
 		assertEquals(2, root.getChildren().iterator().next().getChildren().size());
 		assertEquals(1, root.getChildren().iterator().next().getChildren().iterator().next().getChildren().size());
 	}
 	
 	//	namespace {
 	//		struct A {
 	//		    virtual void fx() { }
 	//		};
 	//	}
 	//
 	//	namespace {
 	//		struct B {
 	//		    virtual void f() { }
 	//		    virtual void f1() { }
 	//		};
 	//	}
 	public void testMergeOfFunctionsInAnonymousNamespaces(){
 		loadCodeAndRun(getAboveComment());
 
 		root = MetriculatorPluginActivator.getDefault().getLogicTreeBuilder().root;
 
 		assertEquals(2, root.getChildren().size());
 		assertEquals(1, root.getChildren().iterator().next().getChildren().size());
 		assertEquals(1, root.getChildren().iterator().next().getChildren().iterator().next().getChildren().size());
 		// TODO: test remove 2nd namespace
 	}
 	
 	// TODO: add test to test 2 ano namespaces result in 2 ano namespace nodes in logical view
 
 	// TODO: test merging in logical view of def und decl within ano namespaces.
 	//	namespace {
 	//		struct A {
 	//		    virtual void fx();
 	//		};
 	//	}
 	//
 	//	void A::fx(){}
 }
