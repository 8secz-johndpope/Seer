 /*
  * Copyright 2009 the original author or authors.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package org.spockframework.mock;
 
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;

 /**
  *
  * @author Peter Niederwieser
  */
 public class ConstantResultGenerator implements IResultGenerator {
   private final Object constant;
 
   public ConstantResultGenerator(Object constant) {
     this.constant = constant;
   }
 
   public Object generate(IMockInvocation invocation) {
    return DefaultTypeTransformation.castToType(constant, invocation.getMethod().getReturnType());
   }
 }
