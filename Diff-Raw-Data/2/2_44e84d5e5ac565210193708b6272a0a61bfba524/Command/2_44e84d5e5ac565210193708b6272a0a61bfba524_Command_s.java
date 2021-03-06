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
 
 package org.apache.maven.shell.command;
 
 import org.apache.maven.shell.i18n.MessageSource;
 import jline.Completor;
 
 import java.util.Collection;
 
 /**
  * Provides the user-action for a command.
  *
  * @version $Rev$ $Date$
  */
 public interface Command
 {
     String getName();
 
     MessageSource getMessages();
 
     Completor[] getCompleters();
 
     /*
     String getShortcut()
 
     Completor getCompletor()
 
     String getDescription()
 
     String getUsage()
 
     String getHelp()
 
     List<CommandAlias> getAliases()
     
     boolean getHidden()
     */
 
     /**
      * Execute the command action.
      *
      * @param context   The execution context of the command.
      * @return          The result of the command execution.
      *
     * @throws Notifiaction     Inform the shell of some non-exception exit state.
      * @throws Exception        Command execution failed.
      */
     Object execute(CommandContext context) throws Exception;
 
     /**
      * Enumeration for the basic return types of a command execution.
      */
     enum Result
     {
         /**
          * The command execution was successful.
          */
         SUCCESS, // 0
 
         /**
          * The command exectuion failed.
          */
         FAILURE // 1
     }
 }
