 /* Date:        April 4, 2011
  * Template:	PluginScreenJavaTemplateGen.java.ftl
  * generator:   org.molgenis.generators.ui.PluginScreenJavaTemplateGen 3.3.3
  * 
  * THIS FILE IS A TEMPLATE. PLEASE EDIT :-)
  */
 
 package org.molgenis.chd7.ui;
 
 import org.molgenis.framework.db.Database;
 import org.molgenis.framework.ui.EasyPluginController;
 import org.molgenis.framework.ui.FreemarkerView;
 import org.molgenis.framework.ui.ScreenController;
 import org.molgenis.mutation.service.MutationService;
 import org.molgenis.mutation.service.PatientService;
 import org.molgenis.chd7.ui.BackgroundModel;
 
 public class Background extends EasyPluginController<BackgroundModel>
 {
 	private static final long serialVersionUID = 1L;
 
 	public Background(String name, ScreenController<?> parent)
 	{
 		super(name, null, parent);
 		this.setModel(new BackgroundModel(this));
 		this.setView(new FreemarkerView("Background.ftl", getModel()));
 	}
 	
 	@Override
 	public void reload(Database db)
 	{
 		try
 		{
 			MutationService mutationService = MutationService.getInstance(db);
 			PatientService patientService   = PatientService.getInstance(db);
 			
 			this.getModel().setNumPathogenicMutations(mutationService.getNumMutationsByPathogenicity("pathogenic"));
 			this.getModel().setNumPathogenicPatients(patientService.getNumPatientsByPathogenicity("pathogenic"));
 			this.getModel().setNumUnclassifiedMutations(mutationService.getNumMutationsByPathogenicity("unclassified variant"));
 			this.getModel().setNumUnclassifiedPatients(patientService.getNumPatientsByPathogenicity("unclassified variant"));
 			this.getModel().setNumBenignMutations(mutationService.getNumMutationsByPathogenicity("benign"));
			this.getModel().setNumPatientsUnpub(patientService.getNumUnpublishedPatients());
 		}
 		catch (Exception e)
 		{
 			//...
 		}
 	}
 }
