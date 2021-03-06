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
 
 package org.apache.directory.studio.dsmlv2.reponse;
 
 
 import org.apache.directory.shared.ldap.codec.search.SearchResultDone;
 import org.apache.directory.studio.dsmlv2.DsmlDecorator;
 import org.dom4j.Element;
 
 
 /**
  * DSML Decorator for SearchResultDone
  *
  * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
  * @version $Rev$, $Date$
  */
 public class SearchResultDoneDsml extends LdapResponseDecorator implements DsmlDecorator
 {
     /**
      * Creates a new instance of SearchResultDoneDsml.
      */
     public SearchResultDoneDsml()
     {
         super( new SearchResultDone() );
     }
 
 
     /**
      * Creates a new instance of SearchResultDoneDsml.
      *
      * @param ldapMessage
      *      the message to decorate
      */
     public SearchResultDoneDsml( SearchResultDone ldapMessage )
     {
         super( ldapMessage );
     }
 
 
     /* (non-Javadoc)
      * @see org.apache.directory.studio.dsmlv2.reponse.LdapMessageDecorator#getMessageType()
      */
     public int getMessageType()
     {
         return instance.getMessageType();
     }
 
 
     /* (non-Javadoc)
      * @see org.apache.directory.studio.dsmlv2.reponse.DsmlDecorator#toDsml(org.dom4j.Element)
      */
     public Element toDsml( Element root )
     {
         Element element = root.addElement( "searchResultDone" );
 
         LdapResultDsml ldapResultDsml = new LdapResultDsml( ( ( SearchResultDone ) instance ).getLdapResult(), instance );
        ldapResultDsml.toDsml( element );
         return element;
     }
 }
