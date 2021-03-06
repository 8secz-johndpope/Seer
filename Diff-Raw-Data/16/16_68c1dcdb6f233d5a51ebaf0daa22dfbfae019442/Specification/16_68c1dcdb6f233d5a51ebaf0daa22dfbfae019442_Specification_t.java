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
 
 package spock.lang;
 
 import org.junit.runner.RunWith;
 
 /**
  * Convenience base class for specifications. Avoids the need to annotate
  * a specification with @Speck and @RunWith, and makes spock.lang.Predef
  * members automatically known to the IDE.
  * <p><em>Note:</em> This class is experimental and might be removed
  * in the future.
  *
  * @author Peter Niederwieser
  */
 @RunWith(Sputnik.class)
public abstract class Specification extends Predef {
   public Object setup() { return null; }
 
   public Object cleanup() { return null; }
 
   public Object setupSpeck() { return null; }
 
   public Object cleanupSpeck() { return null; }
 }
