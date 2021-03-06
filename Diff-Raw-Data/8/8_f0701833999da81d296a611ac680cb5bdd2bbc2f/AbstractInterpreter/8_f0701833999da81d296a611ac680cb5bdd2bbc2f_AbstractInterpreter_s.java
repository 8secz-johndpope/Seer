 /*
  * Copyright (C) 2009 Google Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License"); you may not
  * use this file except in compliance with the License. You may obtain a copy of
  * the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  * License for the specific language governing permissions and limitations under
  * the License.
  */
 
 package com.google.ase.interpreter;
 
 import com.google.ase.Constants;
 
 public abstract class AbstractInterpreter implements InterpreterInterface {
 
   @Override
   public boolean isInstalled() {
     return InterpreterManager.checkInstalled(getName());
   }
 
   @Override
   public String getContentTemplate() {
     return "";
   }
 
   @Override
   public String getInterpreterArchiveName() {
    return String.format("%s-r%s.zip", getName(), getVersion());
   }
 
   @Override
   public String getInterpreterExtrasArchiveName() {
    return String.format("%s-r%s_extras.zip", getName(), getVersion());
   }
 
   @Override
   public String getScriptsArchiveName() {
    return String.format("%s-r%s_scripts.zip", getName(), getVersion());
   }
 
   @Override
   public String getInterpreterArchiveUrl() {
     return Constants.BASE_INSTALL_URL + getInterpreterArchiveName();
   }
 
   @Override
   public String getScriptsArchiveUrl() {
     return Constants.BASE_INSTALL_URL + getScriptsArchiveName();
   }
 
   @Override
   public String getInterpreterExtrasArchiveUrl() {
     return Constants.BASE_INSTALL_URL + getInterpreterExtrasArchiveName();
   }
 
 }
