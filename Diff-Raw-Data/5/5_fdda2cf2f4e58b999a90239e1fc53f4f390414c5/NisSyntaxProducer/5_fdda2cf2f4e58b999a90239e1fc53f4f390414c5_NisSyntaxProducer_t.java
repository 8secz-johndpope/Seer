 /*
  *  Licensed to the Apache Software Foundation (ASF) under one
  *  or more contributor license agreements.  See the NOTICE file
  *  distributed with this work for additional information
  *  regarding copyright ownership.  The ASF licenses this file
  *  to you under the Apache License, Version 2.0 (the
  *  "License"); you may not use this file except in compliance
  *  with the License.  You may obtain a copy of the License at
  *  
  *    http://www.apache.org/licenses/LICENSE-2.0
  *  
  *  Unless required by applicable law or agreed to in writing,
  *  software distributed under the License is distributed on an
  *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  *  KIND, either express or implied.  See the License for the
  *  specific language governing permissions and limitations
  *  under the License. 
  *  
  */
 package org.apache.directory.server.schema.bootstrap;
 
 
 import javax.naming.NamingException;
 
 import org.apache.directory.server.schema.bootstrap.AbstractBootstrapProducer;
 import org.apache.directory.server.schema.bootstrap.ProducerCallback;
 import org.apache.directory.server.schema.bootstrap.ProducerTypeEnum;
 import org.apache.directory.server.schema.registries.Registries;
 import org.apache.directory.server.schema.registries.SyntaxCheckerRegistry;
 
 
 /**
  * A producer of Syntax objects for the nis schema.
  * Modified by hand from generated code
  *
  * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
  * @version $Rev$
  */
 public class NisSyntaxProducer extends AbstractBootstrapProducer
 {
     public NisSyntaxProducer()
     {
         super( ProducerTypeEnum.SYNTAX_PRODUCER );
     }
 
 
     // ------------------------------------------------------------------------
     // BootstrapProducer Methods
     // ------------------------------------------------------------------------
 
     /**
      * @see org.apache.directory.server.schema.bootstrap.BootstrapProducer#produce(org.apache.directory.server.schema.registries.Registries, org.apache.directory.server.schema.bootstrap.ProducerCallback)
      */
     public void produce( Registries registries, ProducerCallback cb ) throws NamingException
     {
         BootstrapSyntax syntax;
         SyntaxCheckerRegistry syntaxCheckerRegistry = registries.getSyntaxCheckerRegistry();
 
         // 1.3.6.1.1.1.0.0 - RFC2307 NIS Netgroup Triple
         syntax = new BootstrapSyntax( "1.3.6.1.1.1.0.0", syntaxCheckerRegistry );
         syntax.setDescription( "RFC2307 NIS Netgroup Triple" );
         syntax.setNames( new String[]
             { "NIS Netgroup Triple" } );
        syntax.setHumanReadable( true );
         cb.schemaObjectProduced( this, syntax.getOid(), syntax );
 
         // 1.3.6.1.1.1.0.1 - RFC2307 Boot Parameter Syntax
         syntax = new BootstrapSyntax( "1.3.6.1.1.1.0.1", syntaxCheckerRegistry );
         syntax.setNames( new String[]
             { "NIS Boot Parameter" } );
        syntax.setHumanReadable( true );
         cb.schemaObjectProduced( this, syntax.getOid(), syntax );
 
     }
 }
