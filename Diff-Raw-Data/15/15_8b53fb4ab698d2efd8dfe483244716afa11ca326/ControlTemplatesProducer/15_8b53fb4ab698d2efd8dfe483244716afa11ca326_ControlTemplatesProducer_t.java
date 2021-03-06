 /******************************************************************************
  * ControlTemplatesProducer.java - created by aaronz@vt.edu on Mar 19, 2007
  * 
  * Copyright (c) 2007 Virginia Polytechnic Institute and State University
  * Licensed under the Educational Community License version 1.0
  * 
  * A copy of the Educational Community License has been included in this 
  * distribution and is available at: http://www.opensource.org/licenses/ecl1.php
  * 
  * Contributors:
  * Aaron Zeckoski (aaronz@vt.edu) - primary
  * 
  *****************************************************************************/
 
 package org.sakaiproject.evaluation.tool.producers;
 
 import java.text.DateFormat;
 import java.util.List;
 import java.util.Locale;
 
 import org.sakaiproject.evaluation.logic.EvalEvaluationsLogic;
 import org.sakaiproject.evaluation.logic.EvalExternalLogic;
 import org.sakaiproject.evaluation.logic.EvalTemplatesLogic;
 import org.sakaiproject.evaluation.model.EvalTemplate;
 import org.sakaiproject.evaluation.tool.viewparams.TemplateViewParameters;
 
 import uk.org.ponder.rsf.components.UIBranchContainer;
 import uk.org.ponder.rsf.components.UIContainer;
 import uk.org.ponder.rsf.components.UIInternalLink;
 import uk.org.ponder.rsf.components.UILink;
 import uk.org.ponder.rsf.components.UIMessage;
 import uk.org.ponder.rsf.components.UIOutput;
 import uk.org.ponder.rsf.view.ComponentChecker;
 import uk.org.ponder.rsf.view.ViewComponentProducer;
 import uk.org.ponder.rsf.viewstate.SimpleViewParameters;
 import uk.org.ponder.rsf.viewstate.ViewParameters;
 
 /**
  * This lists templates for users so they can add, modify, remove them
  *
  * @author Aaron Zeckoski (aaronz@vt.edu)
  */
 public class ControlTemplatesProducer implements ViewComponentProducer {
 
 	/* (non-Javadoc)
 	 * @see uk.org.ponder.rsf.view.ViewComponentProducer#getViewID()
 	 */
 	public static String VIEW_ID = "control_templates";
 	public String getViewID() {
 		return VIEW_ID;
 	}
 
 
 	private EvalExternalLogic external;
 	public void setExternal(EvalExternalLogic external) {
 		this.external = external;
 	}
 
 	private EvalTemplatesLogic templatesLogic;
 	public void setTemplatesLogic(EvalTemplatesLogic templatesLogic) {
 		this.templatesLogic = templatesLogic;
 	}
 	
 	private EvalEvaluationsLogic evaluationsLogic;
 	public void setEvaluationsLogic(EvalEvaluationsLogic evaluationsLogic) {
 		this.evaluationsLogic = evaluationsLogic;
 	}
 
 	private Locale locale;
 	public void setLocale(Locale locale) {
 		this.locale = locale;
 	}
 
 
 	/* (non-Javadoc)
 	 * @see uk.org.ponder.rsf.view.ComponentProducer#fillComponents(uk.org.ponder.rsf.components.UIContainer, uk.org.ponder.rsf.viewstate.ViewParameters, uk.org.ponder.rsf.view.ComponentChecker)
 	 */
 	public void fillComponents(UIContainer tofill, ViewParameters viewparams, ComponentChecker checker) {
 
 		// local variables used in the render logic
 		String currentUserId = external.getCurrentUserId();
 		boolean userAdmin = external.isUserAdmin(currentUserId);
 		boolean createTemplate = templatesLogic.canCreateTemplate(currentUserId);
 		boolean beginEvaluation = evaluationsLogic.canBeginEvaluation(currentUserId);
 		// use a date which is related to the current users locale
 		DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM, locale);
 
 		// page title
 		UIMessage.make(tofill, "page-title", "controltemplates.page.title");
 
 		/*
 		 * top links here
 		 */
 		UIInternalLink.make(tofill, "summary-link", 
 				UIMessage.make("summary.page.title"), 
 			new SimpleViewParameters(SummaryProducer.VIEW_ID));
 
 		if (userAdmin) {
 			UIInternalLink.make(tofill, "administrate-link", 
 					UIMessage.make("administrate.page.title"),
 				new SimpleViewParameters(AdministrateProducer.VIEW_ID));
 		}
 
 		if (createTemplate) {
 			UIInternalLink.make(tofill, "control-templates-link", 
 					UIMessage.make("controltemplates.page.title"), 
 				new SimpleViewParameters(ControlTemplatesProducer.VIEW_ID));
 		} else {
 			throw new SecurityException("User attempted to access " + 
 					ControlTemplatesProducer.VIEW_ID + " when they are not allowed");
 		}
 
 		if (beginEvaluation) {
 			UIInternalLink.make(tofill, "control-evaluations-link", 
 					UIMessage.make("controlevaluations.page.title"), 
 				new SimpleViewParameters(ControlEvaluationsProducer.VIEW_ID));
 			UIInternalLink.make(tofill, "begin-evaluation-link", 
 					UIMessage.make("starteval.page.title"), 
 				new TemplateViewParameters(EvaluationStartProducer.VIEW_ID, null));
 		}
 
 		// create template header and link
 		UIMessage.make(tofill, "templates-header", "controltemplates.templates.header"); 
 		UIMessage.make(tofill, "templates-description","controltemplates.templates.description"); 
 		UIInternalLink.make(tofill, "create-template-link", 
 					UIMessage.make("createtemplate.page.title"), 
 				new TemplateViewParameters(ModifyTemplateProducer.VIEW_ID, null));
 
 		// get templates for the current user
 		List templates = templatesLogic.getTemplatesForUser(currentUserId, null, true);
 		if (templates.size() > 0) {
 			UIBranchContainer templateListing = UIBranchContainer.make(tofill, "template-listing:");
 
 			UIMessage.make(templateListing, "template-title-header", "controltemplates.template.title.header");	
 			UIMessage.make(templateListing, "template-owner-header", "controltemplates.template.owner.header");
 			UIMessage.make(templateListing, "template-lastupdate-header", "controltemplates.template.lastupdate.header");
 
 			for (int i = 0; i < templates.size(); i++) {
 				EvalTemplate template = (EvalTemplate) (templates.get(i));
 
 				UIBranchContainer templateRow = UIBranchContainer.make(templateListing, "template-row:", template.getId().toString());
 
 				// local locked check is more efficient so do that first
 				if ( ! template.getLocked().booleanValue() &&
 						templatesLogic.canModifyTemplate(currentUserId, template.getId()) ) {
                 	// template controllable
 					UIInternalLink.make(templateRow, "template-modify-link", template.getTitle(), 
 							new TemplateViewParameters(ModifyTemplateItemsProducer.VIEW_ID, template.getId()));
 				} else {
                 	// template not controllable
 					UIOutput.make(templateRow, "template-title", template.getTitle());
 				}
 
 				// local locked check is more efficient so do that first
 				if ( ! template.getLocked().booleanValue() &&
 						templatesLogic.canRemoveTemplate(currentUserId, template.getId()) ) {
 					UIInternalLink.make(templateRow, "delete-template-link", 
 							new TemplateViewParameters( RemoveTemplateProducer.VIEW_ID, template.getId() ));
 				} else {
 					UIMessage.make(templateRow, "template-used", "controltemplates.template.inuse");
 				}
 
 				// direct link to the template preview
				UILink.make(templateRow, "preview-template-direct-link", UIMessage.make("general.direct.link"), 
 						external.getEntityURL(template) );
 
 				UIOutput.make(templateRow, "template-owner", external.getUserDisplayName( template.getOwner() ));
 				UIOutput.make(templateRow, "template-last-update", df.format( template.getLastModified() ));
 
 			}
 		} else {
 			UIMessage.make(tofill, "no-templates", "controltemplates.templates.none");
 		}
 	}
 
 }
