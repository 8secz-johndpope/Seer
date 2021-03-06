 /*
  * Orika - simpler, better and faster Java bean mapping
  * 
  * Copyright (C) 2011 Orika authors
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
 
 package ma.glasnost.orika.metadata;
 
 import ma.glasnost.orika.DefaultFieldMapper;
 import ma.glasnost.orika.property.PropertyResolverStrategy;
 
 /**
  * ClassMapBuilderFactory should be used to construct the new instances
  * of ClassMapBuilder needed to register a mapping from one class/type to
  * another class/type.
  * 
  * @author matt.deboer@gmail.com
  *
  */
 public class ClassMapBuilderFactory {
 
 	private PropertyResolverStrategy propertyResolver;
 	private DefaultFieldMapper[] defaults;
 	
 	/**
 	 * @param propertyResolver the PropertyResolverStrategy instance to use when resolving properties
      * of the mapped types
 	 */
 	public synchronized void setPropertyResolver(PropertyResolverStrategy propertyResolver) {
 	    this.propertyResolver = propertyResolver;
 	}
 	
 	/**
 	 * @param defaults zero or more DefaultFieldMapper instances that should be applied when the 
      * <code>byDefault</code> method of the ClassMapBuilder is called.
 	 */
 	public synchronized void setDefaultFieldMappers(DefaultFieldMapper... defaults) {
 	    this.defaults = defaults;
 	}
 	
 	/**
 	 * Verifies whether the factory has been properly initialized
 	 * @return
 	 */
 	public synchronized boolean isInitialized() {
 	    return propertyResolver != null && defaults != null;
 	}
 	/**
      * @param aType
      * @param bType
      * @return
      */
     public <A, B> ClassMapBuilder<A, B> map(Type<A> aType, Type<B> bType) {
        return new ClassMapBuilder<A, B>(aType, bType, propertyResolver, defaults);
     }
     
     /**
      * @param aType
      * @param bType
      * @return
      */
     public <A, B> ClassMapBuilder<A, B> map(Class<A> aType, Type<B> bType) {
        return new ClassMapBuilder<A, B>(TypeFactory.<A> valueOf(aType), bType, propertyResolver, defaults);
     }
     
     /**
      * @param aType
      * @param bType
      * @return
      */
     public <A, B> ClassMapBuilder<A, B> map(Type<A> aType, Class<B> bType) {
        return new ClassMapBuilder<A, B>(aType, TypeFactory.<B> valueOf(bType), propertyResolver, defaults);
     }
 	
     /**
      * @param aType
      * @param bType
      * @return
      */
     public <A, B> ClassMapBuilder<A, B> map(Class<A> aType, Class<B> bType) {
        return new ClassMapBuilder<A, B>(TypeFactory.<A> valueOf(aType), TypeFactory.<B> valueOf(bType), propertyResolver, defaults);
     }
     
 }
