 /*
  * Copyright (C) 2010 BloatIt. This file is part of BloatIt. BloatIt is free software: you
  * can redistribute it and/or modify it under the terms of the GNU Affero General Public
  * License as published by the Free Software Foundation, either version 3 of the License,
  * or (at your option) any later version. BloatIt is distributed in the hope that it will
  * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
  * License for more details. You should have received a copy of the GNU Affero General
  * Public License along with BloatIt. If not, see <http://www.gnu.org/licenses/>.
  */
 package com.bloatit.web.pages;
 
 import java.util.Map.Entry;
 
 import com.bloatit.framework.utils.i18n.Localizator;
 import com.bloatit.framework.utils.i18n.Localizator.LanguageDescriptor;
 import com.bloatit.framework.webserver.Context;
 import com.bloatit.framework.webserver.annotations.ParamContainer;
 import com.bloatit.framework.webserver.annotations.RequestParam;
 import com.bloatit.framework.webserver.annotations.RequestParam.Role;
 import com.bloatit.framework.webserver.components.HtmlDiv;
 import com.bloatit.framework.webserver.components.HtmlTitleBlock;
 import com.bloatit.framework.webserver.components.form.HtmlForm;
 import com.bloatit.framework.webserver.components.form.HtmlFormBlock;
 import com.bloatit.framework.webserver.components.form.HtmlSimpleDropDown;
 import com.bloatit.framework.webserver.components.form.HtmlSubmit;
 import com.bloatit.framework.webserver.components.form.HtmlTextArea;
 import com.bloatit.framework.webserver.components.form.HtmlTextField;
 import com.bloatit.framework.webserver.components.meta.HtmlElement;
 import com.bloatit.model.demand.DemandManager;
 import com.bloatit.web.actions.CreateDemandAction;
 import com.bloatit.web.url.CreateDemandPageUrl;
import com.bloatit.web.url.CreateIdeaActionUrl;
 
 /**
  * Page that hosts the form to create a new Idea
  */
 @ParamContainer("demand/create")
 public final class CreateDemandPage extends LoggedPage {
 
     private static final int SPECIF_INPUT_NB_LINES = 10;
     private static final int SPECIF_INPUT_NB_COLUMNS = 80;
 
     @RequestParam(name = CreateDemandAction.DESCRIPTION_CODE, defaultValue = "", role = Role.SESSION)
     private final String description;
 
     @RequestParam(name = CreateDemandAction.SPECIFICATION_CODE, defaultValue = "", role = Role.SESSION)
     private final String specification;
 
     @RequestParam(name = CreateDemandAction.PROJECT_CODE, defaultValue = "", role = Role.SESSION)
     private final String project;
 
     @RequestParam(name = CreateDemandAction.CATEGORY_CODE, defaultValue = "", role = Role.SESSION)
     private final String category;
 
     @SuppressWarnings("unused")
     // Will be used when language can be changed on Idea creation
     @RequestParam(name = CreateDemandAction.LANGUAGE_CODE, defaultValue = "", role = Role.SESSION)
     private final String lang;
 
     public CreateDemandPage(final CreateDemandPageUrl createIdeaPageUrl) {
         super(createIdeaPageUrl);
         this.description = createIdeaPageUrl.getDescription();
         this.specification = createIdeaPageUrl.getSpecification();
         this.project = createIdeaPageUrl.getProject();
         this.category = createIdeaPageUrl.getCategory();
         this.lang = createIdeaPageUrl.getLang();
     }
 
     @Override
     protected String getPageTitle() {
         return "Create new idea";
     }
 
     @Override
     public boolean isStable() {
         return false;
     }
 
     @Override
     public HtmlElement createRestrictedContent() {
         if (DemandManager.canCreate(session.getAuthToken())) {
             return new HtmlDiv("padding_box").add(generateIdeaCreationForm());
         }
         return generateBadRightError();
     }
 
     private HtmlElement generateIdeaCreationForm() {
         final HtmlTitleBlock createIdeaTitle = new HtmlTitleBlock(Context.tr("Create a new idea"), 1);
        final CreateIdeaActionUrl doCreateUrl = new CreateIdeaActionUrl();
 
         // Create the form stub
         final HtmlForm createIdeaForm = new HtmlForm(doCreateUrl.urlString());
         final HtmlFormBlock specifBlock = new HtmlFormBlock(Context.tr("Specify the new idea"));
         final HtmlFormBlock paramBlock = new HtmlFormBlock(Context.tr("Parameters of the new idea"));
 
         createIdeaTitle.add(createIdeaForm);
         createIdeaForm.add(specifBlock);
         createIdeaForm.add(paramBlock);
         createIdeaForm.add(new HtmlSubmit(Context.tr("submit")));
 
         // Create the fields that will describe the description of the idea
         final HtmlTextField descriptionInput = new HtmlTextField(CreateDemandAction.DESCRIPTION_CODE, Context.tr("Title"));
         descriptionInput.setDefaultValue(description);
         descriptionInput.setComment(Context.tr("The title of the new idea must be permit to identify clearly the idea's specificity."));
 
         // Create the fields that will describe the specification of the idea
         final HtmlTextArea specificationInput = new HtmlTextArea(CreateDemandAction.SPECIFICATION_CODE, Context.tr("Describe the idea"),
                 SPECIF_INPUT_NB_LINES, SPECIF_INPUT_NB_COLUMNS);
         specificationInput.setDefaultValue(specification);
         specificationInput.setComment(Context.tr("Enter a long description of the idea : list all features, describe them all "
                 + "... Try to leave as little room for ambiguity as possible."));
         specifBlock.add(descriptionInput);
         specifBlock.add(specificationInput);
 
         // Create the fields that will be used to describe the parameters of the
         // idea (project ...)
         final HtmlSimpleDropDown languageInput = new HtmlSimpleDropDown(CreateDemandAction.LANGUAGE_CODE, Context.tr("Language"));
         for (final Entry<String, LanguageDescriptor> langEntry : Localizator.getAvailableLanguages().entrySet()) {
             languageInput.add(langEntry.getValue().name, langEntry.getValue().code);
         }
 
         final HtmlTextField categoryInput = new HtmlTextField(CreateDemandAction.CATEGORY_CODE, Context.tr("Category"));
         categoryInput.setDefaultValue(category);
         final HtmlTextField projectInput = new HtmlTextField(CreateDemandAction.PROJECT_CODE, Context.tr("Project"));
         projectInput.setDefaultValue(project);
         paramBlock.add(languageInput);
         paramBlock.add(categoryInput);
         paramBlock.add(projectInput);
 
         final HtmlDiv group = new HtmlDiv();
         group.add(createIdeaTitle);
         return group;
     }
 
     private HtmlElement generateBadRightError() {
         final HtmlDiv group = new HtmlDiv();
 
         return group;
     }
 
     @Override
     public String getRefusalReason() {
         return Context.tr("You must be logged to create a new idea.");
     }
 }
