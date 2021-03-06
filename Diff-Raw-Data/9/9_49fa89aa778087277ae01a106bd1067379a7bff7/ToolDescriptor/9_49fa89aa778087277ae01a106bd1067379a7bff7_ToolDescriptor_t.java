 /*******************************************************************************
  *
  * Copyright (c) 2004-2009 Oracle Corporation.
  *
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors: 
 *
 *    Tom Huybrechts
  *     
  *
  *******************************************************************************/ 
 
 package hudson.tools;
 
 import hudson.model.Descriptor;
 import hudson.util.DescribableList;
 
 import java.util.Collections;
 import java.util.List;
 import java.io.IOException;
 import java.lang.reflect.Array;
 import net.sf.json.JSONObject;
 import org.kohsuke.stapler.StaplerRequest;
 
 /**
  * {@link Descriptor} for {@link ToolInstallation}.
  *
  * @author huybrechts
  * @since 1.286
  */
 public abstract class ToolDescriptor<T extends ToolInstallation> extends Descriptor<ToolInstallation> {
 
     private T[] installations;
 
     /**
      * Configured instances of {@link ToolInstallation}s.
      *
      * @return read-only list of installations;
      *      can be empty but never null.
      */
     public T[] getInstallations() {
        
        if (installations != null){
            installations.clone();
        }else{
           Class<?> arrayType = installations.getClass().getComponentType();
	   installations = (T[])Array.newInstance(arrayType, 0);
        }
        return  installations;
     }
 
     /**
      * Overwrites {@link ToolInstallation}s.
      *
      * @param installations list of installations;
      *      can be empty but never null.
      */
     public void setInstallations(T... installations) {
         this.installations = installations.clone();
     }
 
     /**
      * Lists up {@link ToolPropertyDescriptor}s that are applicable to this {@link ToolInstallation}.
      */
     public List<ToolPropertyDescriptor> getPropertyDescriptors() {
         return PropertyDescriptor.for_(ToolProperty.all(),clazz);
     }
 
     /**
      * Optional list of installers to be configured by default for new tools of this type.
      * If there are popular versions of the tool available using generic installation techniques,
      * they can be returned here for the user's convenience.
      * @since 1.305
      */
     public List<? extends ToolInstaller> getDefaultInstallers() {
         return Collections.emptyList();
     }
 
     /**
      * Default value for {@link ToolInstallation#getProperties()} used in the form binding.
      * @since 1.305
      */
     public DescribableList<ToolProperty<?>,ToolPropertyDescriptor> getDefaultProperties() throws IOException {
         DescribableList<ToolProperty<?>,ToolPropertyDescriptor> r
                 = new DescribableList<ToolProperty<?>, ToolPropertyDescriptor>(NOOP);
 
         List<? extends ToolInstaller> installers = getDefaultInstallers();
         if(!installers.isEmpty())
             r.add(new InstallSourceProperty(installers));
 
         return r;
     }
 
     @Override
     @SuppressWarnings("unchecked") // cast to T[]
     public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
         setInstallations(req.bindJSONToList(clazz, json.get("tool")).toArray((T[]) Array.newInstance(clazz, 0)));
         return true;
     }
 
 }
