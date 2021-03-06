 /**
  * Copyright 2014 Red Hat, Inc. and/or its affiliates.
  *
  * Licensed under the Eclipse Public License version 1.0, available at
  * http://www.eclipse.org/legal/epl-v10.html
  */
 
 package org.jboss.forge.addon.javaee.cdi.ui;
 
 import org.jboss.forge.addon.javaee.cdi.CDIFacet;
import org.jboss.forge.addon.projects.ui.RequiresProjectPredicate;
 import org.jboss.forge.addon.ui.context.UIContext;
 
 /**
  * Requires a project, to be executed in a non-GUI environment and have the {@link CDIFacet} installed to be enabled
  * 
  * @author <a href="ggastald@redhat.com">George Gastaldi</a>
  */
public class RequiresCDIFacetPredicate extends RequiresProjectPredicate
 {
    @Override
    public boolean accept(UIContext context)
    {
      boolean enabled = super.accept(context);
      if (enabled)
       {
         enabled = !context.getProvider().isGUI() && getProject(context).hasFacet(CDIFacet.class);
       }
       return enabled;
    }
 }
