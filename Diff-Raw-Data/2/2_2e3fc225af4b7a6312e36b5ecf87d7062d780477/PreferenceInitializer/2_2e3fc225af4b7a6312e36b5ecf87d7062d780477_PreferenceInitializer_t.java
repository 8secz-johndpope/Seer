 /* License (BSD Style License):
  * Copyright (c) 2011
  * Department of Computer Science
  * Technische Universität Darmstadt
  * All rights reserved.
  *
  *  Redistribution and use in source and binary forms, with or without
  *  modification, are permitted provided that the following conditions are met:
  *
  *  - Redistributions of source code must retain the above copyright notice,
  *    this list of conditions and the following disclaimer.
  *  - Redistributions in binary form must reproduce the above copyright notice,
  *    this list of conditions and the following disclaimer in the documentation
  *    and/or other materials provided with the distribution.
  *  - Neither the name of the Software Technology Group or Technische 
  *    Universität Darmstadt nor the names of its contributors may be used to 
  *    endorse or promote products derived from this software without specific 
  *    prior written permission.
  *
  *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
  *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
  *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
  *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
  *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
  *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
  *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
  *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
  *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
  *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
  *  POSSIBILITY OF SUCH DAMAGE.
  */
 package de.tud.cs.st.Lyrebird.recorder.preferences;
 
 import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
 import org.eclipse.jface.preference.IPreferenceStore;
 
 import de.tud.cs.st.Lyrebird.recorder.Activator;
 
 /**
  * Class used to initialize default preference values.
  * @author Malte V
  */
 public class PreferenceInitializer extends AbstractPreferenceInitializer {
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
 	 */
 	public void initializeDefaultPreferences() {
 		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
 		store.setDefault(PreferenceConstants.P_SAVE_PER_PROJECT, true);
 		store.setDefault(PreferenceConstants.P_PROJECT_RELATIV_PATH, "Outputdir_" + Activator.PLUGIN_ID);
 		store.setDefault(PreferenceConstants.P_ABSOLUTE_PATH, "");
 	}
 
 }
