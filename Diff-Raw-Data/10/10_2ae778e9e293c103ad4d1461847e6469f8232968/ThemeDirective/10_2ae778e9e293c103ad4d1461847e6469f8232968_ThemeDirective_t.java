 /*
  * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
  *
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the GNU Lesser General Public License
  * (LGPL) version 2.1 which accompanies this distribution, and is available at
  * http://www.gnu.org/licenses/lgpl.html
  *
  * This library is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * Contributors:
  *     Jean-Marc Orliaguet, Chalmers
  *
  * $Id$
  */
 
 package org.nuxeo.theme.webengine.fm.extensions;
 
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.io.StringReader;
 import java.net.URL;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.Map;
 
 import javax.servlet.http.HttpServletRequest;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.rendering.fm.extensions.BlockWriter;
 import org.nuxeo.ecm.webengine.WebEngine;
 import org.nuxeo.ecm.webengine.model.WebContext;
 import org.nuxeo.theme.ApplicationType;
 import org.nuxeo.theme.Manager;
 import org.nuxeo.theme.NegotiationDef;
 import org.nuxeo.theme.negotiation.NegotiationException;
 import org.nuxeo.theme.themes.ThemeException;
 import org.nuxeo.theme.themes.ThemeManager;
 import org.nuxeo.theme.types.TypeFamily;
 import org.nuxeo.theme.webengine.negotiation.WebNegotiator;
 
 import freemarker.core.Environment;
 import freemarker.ext.beans.BeansWrapper;
 import freemarker.template.Template;
 import freemarker.template.TemplateDirectiveBody;
 import freemarker.template.TemplateDirectiveModel;
 import freemarker.template.TemplateException;
 import freemarker.template.TemplateModel;
 import freemarker.template.TemplateModelException;
 
 /**
  * @author <a href="mailto:jmo@chalmers.se">Jean-Marc Orliaguet</a>
  * 
  */
 public class ThemeDirective implements TemplateDirectiveModel {
 
     private static final Log log = LogFactory.getLog(ThemeDirective.class);
 
     private final Map<URL, String> cachedThemes = new HashMap<URL, String>();
 
     private Map<URL, Long> lastRefreshedMap = new HashMap<URL, Long>();
 
     @SuppressWarnings("unchecked")
     public void execute(Environment env, Map params, TemplateModel[] loopVars,
             TemplateDirectiveBody body) throws TemplateException, IOException {
 
         if (!params.isEmpty()) {
             throw new TemplateModelException(
                     "This directive doesn't allow parameters.");
         }
         if (loopVars.length != 0) {
             throw new TemplateModelException(
                     "This directive doesn't allow loop variables.");
         }
         if (body == null) {
             throw new TemplateModelException("Expecting a body");
         }
 
         WebContext ctx = WebEngine.getActiveContext();
         if (ctx == null) {
             throw new IllegalStateException("Not In a Web Context");
         }
 
         env.setGlobalVariable("nxthemesInfo",
                 BeansWrapper.getDefaultInstance().wrap(Manager.getInfoPool()));
 
         final URL themeUrl = getThemeUrlAndSetupRequest(ctx);
         if (themeUrl == null) {
             return;
         }
         String rendered;
         try {
             rendered = renderTheme(themeUrl);
         } catch (ThemeException e) {
             log.error("Theme rendering failed: " + e.getMessage());
             return;
         }

        // Render <@block> content
        BlockWriter writer = (BlockWriter) env.getOut();
        writer.setSuppressOutput(true);
        body.render(writer);
        writer.setSuppressOutput(false);
        
        // Apply the theme template
         BufferedReader reader = new BufferedReader(new StringReader(rendered));
         Template tpl = new Template(themeUrl.toString(), reader,
                 env.getConfiguration(), env.getTemplate().getEncoding());
         env.include(tpl);
     }
 
     public String renderTheme(URL themeUrl) throws ThemeException {
         if (!needsToBeRefreshed(themeUrl) && cachedThemes.containsKey(themeUrl)) {
             return cachedThemes.get(themeUrl);
         }
         String result = ThemeManager.renderElement(themeUrl);
         if (result != null) {
             cachedThemes.put(themeUrl, result);
             lastRefreshedMap.put(themeUrl, new Date().getTime());
         }
         return result;
     }
 
     protected boolean needsToBeRefreshed(URL themeUrl) {
         if (themeUrl.getProtocol().equals("nxtheme")) {
             Long lastRefreshed = lastRefreshedMap.get(themeUrl);
             if (lastRefreshed == null) {
                 lastRefreshed = 0L;
             }
             try {
                 if (themeUrl.openConnection().getLastModified() >= lastRefreshed) {
                     return true;
                 }
             } catch (IOException e) {
                 return false;
             }
         }
         return false;
     }
 
     private static URL getThemeUrlAndSetupRequest(WebContext context)
             throws IOException {
         HttpServletRequest request = context.getRequest();
         URL themeUrl = (URL) request.getAttribute("org.nuxeo.theme.url");
         if (themeUrl != null) {
             return themeUrl;
         }
 
         // Get the negotiation strategy
         final String root = context.getModulePath();
 
         final ApplicationType application = (ApplicationType) Manager.getTypeRegistry().lookup(
                 TypeFamily.APPLICATION, root);
 
         String strategy = null;
         if (application != null) {
             final NegotiationDef negotiation = application.getNegotiation();
             if (negotiation != null) {
                 request.setAttribute("org.nuxeo.theme.default.theme",
                         negotiation.getDefaultTheme());
                 request.setAttribute("org.nuxeo.theme.default.engine",
                         negotiation.getDefaultEngine());
                 request.setAttribute("org.nuxeo.theme.default.perspective",
                         negotiation.getDefaultPerspective());
                 strategy = negotiation.getStrategy();
             }
         }
 
         if (strategy == null) {
             log.error("Could not obtain the negotiation strategy for " + root);
         } else {
             try {
                 final String spec = new WebNegotiator(strategy, context).getSpec();
                 themeUrl = new URL(spec);
                 request.setAttribute("org.nuxeo.theme.url", themeUrl);
             } catch (NegotiationException e) {
                 log.error("Could not get default negotiation settings.", e);
             }
         }
 
         return themeUrl;
     }
 }
