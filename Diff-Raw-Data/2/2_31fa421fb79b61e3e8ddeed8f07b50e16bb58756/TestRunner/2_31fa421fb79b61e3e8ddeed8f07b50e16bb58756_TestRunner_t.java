 /*
  * JBoss, Home of Professional Open Source
  * Copyright 2009, Red Hat Middleware LLC, and individual contributors
  * by the @authors tag. See the copyright.txt in the distribution for a
  * full listing of individual contributors.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  * http://www.apache.org/licenses/LICENSE-2.0
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package org.jboss.arquillian.spi;
 
 /**
  * TestRunner
  * 
 * A Generic way to start the test framework.
  *
  * @author <a href="mailto:aslak@conduct.no">Aslak Knutsen</a>
  * @version $Revision: $
  */
 public interface TestRunner
 {
    public enum ExecutionMode 
    {
       /**
        * When running deployed inside the container  
        */
       CONTAINER,
       
       /**
        * When running outside the container
        */
       STANDALONE
    }
    
    /**
     * Run a single test method in a test class.
     * 
     * @param testClass The test case class to execute
     * @param methodName The method to execute
     * @return The result of the test
     */
    TestResult execute(Class<?> testClass, String methodName);
    
    /**
     * Instruct the TestRunner which mode to run in.
     * 
     * @param executionMode 
     */
    void setExecutionMode(ExecutionMode executionMode);
 }
