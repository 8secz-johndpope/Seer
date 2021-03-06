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
 
 /**
  * A support class for RulesModule which reduces repetition and results in a more readable configuration.
  *
  * @since 3.0
  */
 public abstract class AbstractRulesModule
     implements RulesModule
 {
 
     private RulesBinder rulesBinder;
 
     /**
      * {@inheritDoc}
      */
     public final void configure( RulesBinder rulesBinder )
     {
        if ( this.rulesBinder != null )
         {
             throw new IllegalStateException( "Re-entry is not allowed." );
         }
 
         this.rulesBinder = rulesBinder;
         try
         {
             this.configure();
         }
         finally
         {
             this.rulesBinder = null;
         }
     }
 
     /**
      * Configures a {@link Binder} via the exposed methods.
      */
     protected abstract void configure();
 
     /**
      * @see RulesBinder#addError(String, Object...)
      */
     protected void addError( String messagePattern, Object... arguments )
     {
         this.rulesBinder.addError( messagePattern, arguments );
     }
 
     /**
      * @see RulesBinder#addError(Throwable)
      */
     protected void addError( Throwable t )
     {
         this.rulesBinder.addError( t );
     }
 
     /**
      * @see RulesBinder#install(RulesModule)
      */
     protected void install( RulesModule rulesModule )
     {
         this.rulesBinder.install( rulesModule );
     }
 
     /**
      * @see RulesBinder#forPattern(String)
      */
     protected LinkedRuleBuilder forPattern( String pattern )
     {
         return this.rulesBinder.forPattern( pattern );
     }
 
     /**
      * Return the wrapped {@link RulesBinder}.
      *
      * @return The wrapped {@link RulesBinder}
      */
     protected RulesBinder rulesBinder()
     {
         return this.rulesBinder;
     }
 
 }
