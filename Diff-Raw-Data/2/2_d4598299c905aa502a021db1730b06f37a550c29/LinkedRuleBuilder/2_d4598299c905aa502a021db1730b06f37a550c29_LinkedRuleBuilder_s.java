 /* $Id$
  *
  * Licensed to the Apache Software Foundation (ASF) under one or more
  * contributor license agreements.  See the NOTICE file distributed with
  * this work for additional information regarding copyright ownership.
  * The ASF licenses this file to You under the Apache License, Version 2.0
  * (the "License"); you may not use this file except in compliance with
  * the License.  You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package org.apache.commons.digester3.binder;
 
 import org.apache.commons.digester3.Rule;
 
 /**
  * Builder invoked to bind one or more rules to a pattern.
  *
  * @since 3.0
  */
 public final class LinkedRuleBuilder
 {
 
     private final RulesBinder mainBinder;
 
     private final FromBinderRuleSet fromBinderRuleSet;
 
     private final ClassLoader classLoader;
 
     private final String keyPattern;
 
     private String namespaceURI;
 
     LinkedRuleBuilder( final RulesBinder mainBinder, final FromBinderRuleSet fromBinderRuleSet,
                        final ClassLoader classLoader, final String keyPattern )
     {
         this.mainBinder = mainBinder;
         this.fromBinderRuleSet = fromBinderRuleSet;
         this.classLoader = classLoader;
         this.keyPattern = keyPattern;
     }
 
     /**
      * Construct rule that automatically sets a property from the body text, taking the property
      * name the same as the current element.
      */
     public BeanPropertySetterBuilder setBeanProperty()
     {
         return addProvider( new BeanPropertySetterBuilder( this.keyPattern, this.namespaceURI, this.mainBinder, this ) );
     }
 
     /**
      * Calls a method on an object on the stack (normally the top/parent object), passing arguments collected from
      * subsequent {@link #callParam(int)} rule or from the body of this element.
      *
      * @param methodName Method name of the parent object to call
      */
     public CallMethodBuilder callMethod( String methodName )
     {
         if ( methodName == null || methodName.length() == 0 )
         {
             mainBinder.addError( "{ forPattern( \"%s\" ).callMethod( String ) } empty 'methodName' not allowed",
                                  keyPattern );
         }
 
         return this.addProvider( new CallMethodBuilder( keyPattern, namespaceURI, mainBinder, this, methodName,
                                                         classLoader ) );
     }
 
     /**
      * Saves a parameter for use by a surrounding {@link #callMethod(String)}.
      */
     public CallParamBuilder callParam()
     {
         return this.addProvider( new CallParamBuilder( keyPattern, namespaceURI, mainBinder, this ) );
     }
 
     /**
      * Construct a "call parameter" rule that will save the body text of this element as the parameter value.
      */
     public PathCallParamBuilder callParamPath()
     {
         return addProvider( new PathCallParamBuilder( keyPattern, namespaceURI, mainBinder, this ) );
     }
 
     /**
      * Uses an {@link ObjectCreationFactory} to create a new object which it pushes onto the object stack.
      *
      * When the element is complete, the object will be popped.
      */
     public FactoryCreateBuilder factoryCreate()
     {
         return addProvider( new FactoryCreateBuilder( keyPattern, namespaceURI, mainBinder, this, classLoader ) );
     }
 
     /**
      * Construct an object.
      */
     public ObjectCreateBuilder createObject()
     {
         return addProvider( new ObjectCreateBuilder( keyPattern, namespaceURI, mainBinder, this, classLoader ) );
     }
 
     /**
      * Saves a parameter for use by a surrounding {@link #callMethod(String)}.
      *
      * @param <T> The parameter type to pass along
      * @param paramObj The parameter to pass along
      */
     public <T> ObjectParamBuilder<T> objectParam( /* @Nullable */T paramObj )
     {
         return addProvider( new ObjectParamBuilder<T>( keyPattern, namespaceURI, mainBinder, this, paramObj ) );
     }
 
     /**
      * Sets properties on the object at the top of the stack,
      * based on child elements with names matching properties on that  object.
      */
     public NestedPropertiesBuilder setNestedProperties()
     {
         // that would be useful when adding rules via automatically generated rules binding (such annotations)
         NestedPropertiesBuilder nestedPropertiesBuilder =
             fromBinderRuleSet.getProvider( keyPattern, namespaceURI, NestedPropertiesBuilder.class );
         if ( nestedPropertiesBuilder != null )
         {
             return nestedPropertiesBuilder;
         }
 
         return addProvider( new NestedPropertiesBuilder( keyPattern, namespaceURI, mainBinder, this ) );
     }
 
     /**
      * Calls a method on the (top-1) (parent) object, passing the top object (child) as an argument,
      * commonly used to establish parent-child relationships.
      *
      * @param methodName Method name of the parent method to call
      */
     public SetNextBuilder setNext( String methodName )
     {
         if ( methodName == null || methodName.length() == 0 )
         {
             mainBinder.addError( "{ forPattern( \"%s\" ).setNext( String ) } empty 'methodName' not allowed",
                                  keyPattern );
         }
         return this.addProvider( new SetNextBuilder( keyPattern, namespaceURI, mainBinder, this, methodName,
                                                      classLoader ) );
     }
 
     /**
     * {@inheritDoc}
      */
     public SetPropertiesBuilder setProperties()
     {
         // that would be useful when adding rules via automatically generated rules binding (such annotations)
         SetPropertiesBuilder setPropertiesBuilder =
             fromBinderRuleSet.getProvider( keyPattern, namespaceURI, SetPropertiesBuilder.class );
         if ( setPropertiesBuilder != null )
         {
             return setPropertiesBuilder;
         }
 
         return addProvider( new SetPropertiesBuilder( keyPattern, namespaceURI, mainBinder, this ) );
     }
 
     /**
      * Sets an individual property on the object at the top of the stack, based on attributes with specified names.
      *
      * @param attributePropertyName Name of the attribute that will contain the name of the property to be set
      */
     public SetPropertyBuilder setProperty( String attributePropertyName )
     {
         if ( attributePropertyName == null || attributePropertyName.length() == 0 )
         {
             mainBinder.addError( "{ forPattern( \"%s\" ).setProperty( String )} empty 'attributePropertyName' not allowed",
                                  keyPattern );
         }
 
         return addProvider( new SetPropertyBuilder( keyPattern, namespaceURI, mainBinder, this, attributePropertyName ) );
     }
 
     /**
      * Add a custom user rule in the specified pattern built by the given provider.
      *
      * @param <R>
      * @param provider
      * @return
      */
     public <R extends Rule> ByRuleProviderBuilder<R> addRuleCreatedBy(RuleProvider<R> provider)
     {
         if ( provider == null )
         {
             mainBinder.addError( "{ forPattern( \"%s\" ).addRuleCreatedBy() } null rule provider not valid", keyPattern );
         }
 
         return addProvider( new ByRuleProviderBuilder<R>( keyPattern, namespaceURI, mainBinder, this, provider ) );
     }
 
     /**
      * Sets the namespace URI for the current rule pattern.
      *
      * @param namespaceURI the namespace URI associated to the rule pattern.
      * @return this {@link LinkedRuleBuilder} instance
      */
     public LinkedRuleBuilder withNamespaceURI( /* @Nullable */String namespaceURI )
     {
         if ( namespaceURI == null || namespaceURI.length() > 0 )
         {
             this.namespaceURI = namespaceURI;
         }
         else
         {
             // ignore empty namespaces, null is better
             this.namespaceURI = null;
         }
 
         return this;
     }
 
     /**
      * Add a provider in the data structure where storing the providers binding.
      *
      * @param <R> The rule will be created by the given provider
      * @param provider The provider has to be stored in the data structure
      * @return The provider itself has to be stored in the data structure
      */
     private <R extends Rule, RB extends AbstractBackToLinkedRuleBuilder<R>> RB addProvider( RB provider )
     {
         fromBinderRuleSet.registerProvider( provider );
         return provider;
     }
 
 }
