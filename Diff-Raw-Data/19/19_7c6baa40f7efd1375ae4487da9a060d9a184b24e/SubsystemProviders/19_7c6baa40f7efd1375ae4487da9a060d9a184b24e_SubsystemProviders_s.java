 /*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
 package com.mycompany.subsystem.extension;
 
 import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
 import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
 import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
 import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
 
 import java.util.Locale;
 
 import org.jboss.as.controller.descriptions.DescriptionProvider;
 import org.jboss.dmr.ModelNode;
 
 /**
  * Contains the description providers. The description providers are what print out the
  * information when you execute the {@code read-resource-description} operation.
  *
  * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
  */
 public class SubsystemProviders {
 
     /**
      * Used to create the description of the subsystem
      */
     public static DescriptionProvider SUBSYSTEM = new DescriptionProvider() {
         public ModelNode getModelDescription(Locale locale) {
             //The locale is passed in so you can internationalize the strings used in the descriptions
 
             final ModelNode subsystem = new ModelNode();
             subsystem.get(DESCRIPTION).set("This is my subsystem");
             subsystem.get(HEAD_COMMENT_ALLOWED).set(true);
             subsystem.get(TAIL_COMMENT_ALLOWED).set(true);
             subsystem.get(NAMESPACE).set(SubsystemExtension.NAMESPACE);
 
             return subsystem;
         }
     };
 
     /**
      * Used to create the description of the subsystem add method
      */
     public static DescriptionProvider SUBSYSTEM_ADD = new DescriptionProvider() {
         public ModelNode getModelDescription(Locale locale) {
             //The locale is passed in so you can internationalize the strings used in the descriptions
 
             final ModelNode subsystem = new ModelNode();
             subsystem.get(DESCRIPTION).set("Adds my subsystem");
 
             return subsystem;
         }
     };
 
 }
