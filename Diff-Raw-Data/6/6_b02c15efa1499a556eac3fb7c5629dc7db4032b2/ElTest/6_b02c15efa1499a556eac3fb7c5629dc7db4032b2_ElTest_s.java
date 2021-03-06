 /*
  * JBoss, Home of Professional Open Source
  * Copyright 2010, Red Hat, Inc., and individual contributors
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
 package org.jboss.weld.extensions.test.el;
 
 import javax.inject.Inject;
 
 import org.jboss.arquillian.api.Deployment;
 import org.jboss.arquillian.junit.Arquillian;
 import org.jboss.shrinkwrap.api.Archive;
 import org.jboss.shrinkwrap.api.ShrinkWrap;
 import org.jboss.shrinkwrap.api.spec.JavaArchive;
 import org.jboss.weld.extensions.el.Expressions;
 import org.junit.Assert;
 import org.junit.Test;
 import org.junit.runner.RunWith;
 
//this test has been disabled due to deployment problems
//as some classes are being picked up in scanning when they shouldn't be
 @RunWith(Arquillian.class)
@org.junit.Ignore
 public class ElTest
 {
    @Inject
    Expressions extressions;
 
    @Deployment
    public static Archive<?> deploy()
    {
       JavaArchive a = ShrinkWrap.create(JavaArchive.class, "test.jar");
       a.addPackage(ElTest.class.getPackage());
      a.addPackage(Expressions.class.getPackage());
       return a;
    }
 
    @Test
    public void testElResolver()
    {
       Assert.assertTrue(extressions.evaluateValueExpression("#{ute.speed}").equals("fast"));
       Assert.assertTrue(extressions.invokeMethodExpression("#{ute.go}").equals(Ute.GO_STRING));
 
    }
 }
