 /*
  * Copyright 2004-2005 the original author or authors.
  * 
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  * 
  *      http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */ 
 package org.codehaus.groovy.grails.commons;
 
 import groovy.lang.GroovyClassLoader;
 import junit.framework.TestCase;
 
 /**
  * 
  * 
  * @author Steven Devijver
  * @since Jul 2, 2005
  */
 public class AbstractInjectableGrailsClassTests extends TestCase {
 
 	public AbstractInjectableGrailsClassTests() {
 		super();
 	}
 
 	public AbstractInjectableGrailsClassTests(String arg0) {
 		super(arg0);
 	}
 
 	public void testAbstractInjectableGrailsClassDefault() throws Exception {
 		GroovyClassLoader cl = new GroovyClassLoader();
 		Class clazz = cl.parseClass("class TestService { }");
 		InjectableGrailsClass grailsClass = new AbstractInjectableGrailsClass(clazz, "Service") {};
 		assertTrue(grailsClass.byType());
 		assertFalse(grailsClass.byName());
 	}
 	
 	public void testAbstractInjectableGrailsClassNullProperty() throws Exception {
 		GroovyClassLoader cl = new GroovyClassLoader();
		Class clazz = cl.parseClass("class TestService {  boolean byName;  boolean available = true }");
 		InjectableGrailsClass grailsClass = new AbstractInjectableGrailsClass(clazz, "Service") {};
 		assertTrue(grailsClass.byType());
 		assertFalse(grailsClass.byName());
 		assertTrue(grailsClass.getAvailable());
 	}
 
 	public void testAbstractInjectableGrailsClassNullPropertyFalse() throws Exception {
 		GroovyClassLoader cl = new GroovyClassLoader();
		Class clazz = cl.parseClass("class TestService {  boolean byName = false; boolean available = true }");
 		InjectableGrailsClass grailsClass = new AbstractInjectableGrailsClass(clazz, "Service") {};
 		assertTrue(grailsClass.byType());
 		assertFalse(grailsClass.byName());
 		assertTrue(grailsClass.getAvailable());
 	}
 
 	public void testAbstractInjectableGrailsClassNullPropertyTrue() throws Exception {
 		GroovyClassLoader cl = new GroovyClassLoader();
		Class clazz = cl.parseClass("class TestService {  boolean byName = true; public static boolean available = false }");
 		InjectableGrailsClass grailsClass = new AbstractInjectableGrailsClass(clazz, "Service") {};
 		assertFalse(grailsClass.byType());
 		assertTrue(grailsClass.byName());
 		assertFalse(grailsClass.getAvailable());
 	}
 
 }
