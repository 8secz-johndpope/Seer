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
 package com.bloatit.web.actions;
 
 import java.util.Locale;
 
 import com.bloatit.framework.exceptions.RedirectException;
 import com.bloatit.framework.webserver.Context;
 import com.bloatit.framework.webserver.annotations.ParamConstraint;
 import com.bloatit.framework.webserver.annotations.ParamContainer;
 import com.bloatit.framework.webserver.annotations.RequestParam;
 import com.bloatit.framework.webserver.annotations.RequestParam.Role;
 import com.bloatit.framework.webserver.annotations.tr;
 import com.bloatit.framework.webserver.masters.Action;
 import com.bloatit.framework.webserver.url.Url;
 import com.bloatit.model.demand.Demand;
 import com.bloatit.model.demand.DemandManager;
import com.bloatit.web.url.CreateIdeaActionUrl;
 import com.bloatit.web.url.CreateDemandPageUrl;
 import com.bloatit.web.url.DemandPageUrl;
 import com.bloatit.web.url.LoginPageUrl;
 
 /**
  * A response to a form used to create a new idea
  */
 @ParamContainer("idea/docreate")
 public final class CreateDemandAction extends Action {
 
     public static final String DESCRIPTION_CODE = "bloatit_idea_description";
     public static final String SPECIFICATION_CODE = "bloatit_idea_specification";
     public static final String PROJECT_CODE = "bloatit_idea_project";
     public static final String CATEGORY_CODE = "bloatit_idea_category";
     public static final String LANGUAGE_CODE = "bloatit_idea_lang";
 
     @RequestParam(name = DESCRIPTION_CODE, role = Role.POST)
     @ParamConstraint(max = "80",
                      maxErrorMsg = @tr("The title must be 80 chars length max."), //
                      min = "10", minErrorMsg = @tr("The title must have at least 10 chars."),
                      optionalErrorMsg = @tr("Error you forgot to write a title"))
     private final String description;
 
     @RequestParam(name = SPECIFICATION_CODE, role = Role.POST)
     private final String specification;
 
     @RequestParam(name = PROJECT_CODE, defaultValue = "", role = Role.POST)
     private final String project;
 
     @RequestParam(name = CATEGORY_CODE, defaultValue = "", role = Role.POST)
     private final String category;
 
     @RequestParam(name = LANGUAGE_CODE, role = Role.POST)
     private final String lang;
    private final CreateIdeaActionUrl url;
 
    public CreateDemandAction(final CreateIdeaActionUrl url) {
         super(url);
         this.url = url;
 
         this.description = url.getDescription();
         this.specification = url.getSpecification();
         this.project = url.getProject();
         this.category = url.getCategory();
         this.lang = url.getLang();
 
     }
 
     @Override
     protected Url doProcess() throws RedirectException {
         session.notifyList(url.getMessages());
         if (!DemandManager.canCreate(session.getAuthToken())) {
             session.notifyError(Context.tr("You must be logged in to create an idea."));
             return new LoginPageUrl();
         }
         final Locale langLocale = new Locale(lang);
         // TODO make it work
         final Demand d = new Demand(session.getAuthToken().getMember(), langLocale, description, specification, null);
 
         d.authenticate(session.getAuthToken());
 
         final DemandPageUrl to = new DemandPageUrl(d);
 
         return to;
     }
 
     @Override
     protected Url doProcessErrors() throws RedirectException {
         session.notifyList(url.getMessages());
 
         session.addParameter(DESCRIPTION_CODE, description);
         session.addParameter(SPECIFICATION_CODE, specification);
         session.addParameter(PROJECT_CODE, project);
         session.addParameter(CATEGORY_CODE, category);
         session.addParameter(url.getLangParameter());
 
         return new CreateDemandPageUrl();
     }
 }
