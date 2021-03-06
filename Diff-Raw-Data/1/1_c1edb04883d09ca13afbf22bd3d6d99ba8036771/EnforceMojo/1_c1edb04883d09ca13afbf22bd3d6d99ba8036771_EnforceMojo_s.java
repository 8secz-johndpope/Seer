 package org.apache.maven.plugins.enforcer;
 
 /*
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  *  http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing,
  * software distributed under the License is distributed on an
  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  * KIND, either express or implied.  See the License for the
  * specific language governing permissions and limitations
  * under the License.
  */
 
 import java.util.ArrayList;
 import java.util.Hashtable;
 import java.util.Iterator;
 
 import org.apache.maven.enforcer.rule.api.EnforcerRule;
 import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
 import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
 import org.apache.maven.execution.MavenSession;
 import org.apache.maven.plugin.AbstractMojo;
 import org.apache.maven.plugin.MojoExecutionException;
 import org.apache.maven.plugin.logging.Log;
 import org.apache.maven.project.MavenProject;
 import org.apache.maven.project.path.PathTranslator;
 import org.codehaus.plexus.PlexusConstants;
 import org.codehaus.plexus.PlexusContainer;
 import org.codehaus.plexus.context.Context;
 import org.codehaus.plexus.context.ContextException;
 import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
 
 /**
  * This goal executes the defined enforcer-rules once per
  * module.
  * 
  * @requiresDependencyResolution test
  * @goal enforce
  * @phase validate
  * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
  * @version $Id$
  */
 public class EnforceMojo
     extends AbstractMojo
     implements Contextualizable
 {
 
     /**
      * Path Translator needed by the ExpressionEvaluator
      * 
      * @component role="org.apache.maven.project.path.PathTranslator"
      */
     protected PathTranslator translator;
 
     /**
      * The MavenSession
      * 
      * @parameter expression="${session}"
      */
     protected MavenSession session;
 
     /**
      * POM
      * 
      * @parameter expression="${project}"
      * @readonly
      * @required
      */
     protected MavenProject project;
 
     /**
      * Flag to fail the build if a version check fails.
      * 
      * @parameter expression="${enforcer.fail}"
      *            default-value="true"
      */
     protected boolean fail = true;
 
     /**
      * Flag to easily skip all checks
      * 
      * @parameter expression="${enforcer.skip}"
      *            default-value="false"
      */
     protected boolean skip = false;
 
     /**
      * Fail on the first rule that doesn't pass
      * 
      * @parameter expression="${enforcer.failFast}"
      *            default-value="false"
      */
     protected boolean failFast = false;
 
     /**
      * Array of objects that implement the EnforcerRule
      * interface to execute.
      * 
      * @parameter
      * @required
      */
     private EnforcerRule[] rules;
     
     /**
      * Use this flag to disable rule result caching. This will cause
      * all rules to execute on each project even if the rule indicates it can
      * safely be cached.
      * @parameter expression="${enforcer.ignoreCache}"
      *  default-value="false"
      */
      protected boolean ignoreCache = false;
     
     /**
      * This is a static variable used to persist the cached results across plugin invocations.
      */
      protected static Hashtable cache = new Hashtable();
 
     
     // set by the contextualize method. Only way to get the
     // plugin's container in 2.0.x
     protected PlexusContainer container;
 
     public void contextualize ( Context context )
         throws ContextException
     {
         container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
     }
     
     /**
      * Entry point to the mojo
      */
     public void execute ()
         throws MojoExecutionException
     {
         Log log = this.getLog();
 
         EnforcerExpressionEvaluator evaluator = new EnforcerExpressionEvaluator( session, translator, project );
 
         // the entire execution can be easily skipped
         if ( !skip )
         {
             // list to store exceptions
             ArrayList list = new ArrayList();
 
             // make sure the rules exist
             if ( rules != null && rules.length > 0 )
             {
                 String currentRule = "Unknown";
 
                 // create my helper
                 EnforcerRuleHelper helper = new DefaultEnforcementRuleHelper( session, evaluator, log, container );
 
                 // if we are only warning, then disable
                 // failFast
                 if ( !fail )
                 {
                     failFast = false;
                 }
 
                 // go through each rule
                 for ( int i = 0; i < rules.length; i++ )
                 {
 
                     // prevent against empty rules
                     EnforcerRule rule = rules[i];
                     if ( rule != null )
                     {
                         // store the current rule for
                         // logging purposes
                         currentRule = rule.getClass().getName();
                         log.debug( "Executing rule: " + currentRule );
                         try
                         {
                             if ( ignoreCache || shouldExecute( rule ) )
                             {
                                 // execute the rule
                                 rules[i].execute( helper );
                             }
                         }
                         catch ( EnforcerRuleException e )
                         {
                             // i can throw an exception
                             // because failfast will be
                             // false if fail is false.
                             if ( failFast )
                             {
                                 throw new MojoExecutionException( currentRule + " failed with message:\n"
                                     + e.getMessage(), e );
                             }
                             else
                             {
                                 list.add( "Rule " + i + ": " + currentRule + " failed with message:\n" + e.getMessage() );
                                 log.debug( "Adding failure due to exception", e );
                             }
                         }
                     }
                 }
 
                 // if we found anything
                 if ( !list.isEmpty() )
                 {
                     Iterator iter = list.iterator();
                     while ( iter.hasNext() )
                     {
                         String failure = (String) iter.next();
                         log.warn( failure );
                     }
                     if ( fail )
                     {
                         throw new MojoExecutionException(
                                                           "Some Enforcer rules have failed. Look above for specific messages explaining why the rule failed." );
                     }
                 }
             }
             else
             {
                 throw new MojoExecutionException(
                                                   "No rules are configured. Use the skip flag if you want to disable execution." );
             }
         }
         else
         {
             log.info( "Skipping Rule Enforcement." );
         }
     }
 
     /**
      * This method determines if a rule should execute based
      * on the cache
      * 
      * @param rule
      * @return
      */
     protected boolean shouldExecute ( EnforcerRule rule )
     {
         if ( rule.isCacheable() )
         {
             Log log = this.getLog();
             log.debug( "Rule " + rule.getClass().getName() + " is cacheable." );
             String key = rule.getClass().getName() + " " + rule.getCacheId();
             if ( EnforceMojo.cache.containsKey( key ) )
             {
                 log.debug( "Key " + key + " was found in the cache" );
                 if ( rule.isResultValid( (EnforcerRule) cache.get( key ) ) )
                 {
                     log.debug( "The cached results are still valid. Skipping the rule: "+rule.getClass().getName() );
                     return false;
                 }
             }
             
             //add it to the cache of executed rules
             EnforceMojo.cache.put( key, rule );
         }
         return true;
     }
 
     /**
      * @return the fail
      */
     public boolean isFail ()
     {
         return this.fail;
     }
 
     /**
      * @param theFail the fail to set
      */
     public void setFail ( boolean theFail )
     {
         this.fail = theFail;
     }
 
     /**
      * @return the rules
      */
     public EnforcerRule[] getRules ()
     {
         return this.rules;
     }
 
     /**
      * @param theRules the rules to set
      */
     public void setRules ( EnforcerRule[] theRules )
     {
         this.rules = theRules;
     }
 
     /**
      * @return the skip
      */
     public boolean isSkip ()
     {
         return this.skip;
     }
 
     /**
      * @param theSkip the skip to set
      */
     public void setSkip ( boolean theSkip )
     {
         this.skip = theSkip;
     }
 
     /**
      * @return the failFast
      */
     public boolean isFailFast ()
     {
         return this.failFast;
     }
 
     /**
      * @param theFailFast the failFast to set
      */
     public void setFailFast ( boolean theFailFast )
     {
         this.failFast = theFailFast;
     }
 
     /**
      * @return the project
      */
     public MavenProject getProject ()
     {
         return this.project;
     }
 
     /**
      * @param theProject the project to set
      */
     public void setProject ( MavenProject theProject )
     {
         this.project = theProject;
     }
 
     /**
      * @return the session
      */
     public MavenSession getSession ()
     {
         return this.session;
     }
 
     /**
      * @param theSession the session to set
      */
     public void setSession ( MavenSession theSession )
     {
         this.session = theSession;
     }
 
     /**
      * @return the translator
      */
     public PathTranslator getTranslator ()
     {
         return this.translator;
     }
 
     /**
      * @param theTranslator the translator to set
      */
     public void setTranslator ( PathTranslator theTranslator )
     {
         this.translator = theTranslator;
     }
 }
