 package org.obda.reformulation.protege4.configpanel;
 
 import inf.unibz.it.obda.owlapi.ReformulationPlatformPreferences;
 
 import org.protege.editor.owl.ui.preferences.OWLPreferencesPanel;
 
 public class OBDAOWLReformulationPlatformConfigPanel extends OWLPreferencesPanel {
 
 	private ReformulationPlatformPreferences preference = null;
 	private ConfigPanel configPanel = null;
 	
 	@Override
 	public void applyChanges() {
 		// Do nothing.
 	}
 
 	@Override
 	public void initialise() throws Exception {
 		preference = (ReformulationPlatformPreferences)
 			getEditorKit().get(ReformulationPlatformPreferences.class.getName());
 		configPanel = new ConfigPanel(preference);
 		this.add(configPanel);
 	}
 
 	@Override
 	public void dispose() throws Exception {
 		// Do nothing.
 	}
 }
