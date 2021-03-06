 /* ***** BEGIN LICENSE BLOCK *****
  * Version: MPL 1.1
  * The contents of this file are subject to the Mozilla Public License Version
  * 1.1 (the "License"); you may not use this file except in compliance with
  * the License. You may obtain a copy of the License at
  * http://www.mozilla.org/MPL/
  *
  * Software distributed under the License is distributed on an "AS IS" basis,
  * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
  * for the specific language governing rights and limitations under the
  * License.
  *
  * The Original Code is Riot.
  *
  * The Initial Developer of the Original Code is
  * Neteye GmbH.
  * Portions created by the Initial Developer are Copyright (C) 2006
  * the Initial Developer. All Rights Reserved.
  *
  * Contributor(s):
  *   Felix Gnass [fgnass at neteye dot de]
  *   Alf Werder <alf.werder@glonz.com>
  *
  * ***** END LICENSE BLOCK ***** */
 package org.riotfamily.website.template;
 
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.Map;
 
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 
 import org.riotfamily.common.web.util.ServletUtils;
 import org.springframework.beans.factory.InitializingBean;
 import org.springframework.web.servlet.DispatcherServlet;
 import org.springframework.web.servlet.ModelAndView;
 import org.springframework.web.servlet.mvc.AbstractController;
 
 /**
  * Controller that passes a map of URLs to it's view (the template). The view
  * is responsible for including the URLs (using a RequestDispatcher) at the
  * right place.
  * <p>
  * The most simple way to achieve this is to use a JSTL view that contains
  * a <code>&lt;c:import value="${<i>slotname</i>}" /&gt;</code> tag for each
  * slot, where <code><i>slotname</i></code> has to be one of the keys present
  * in the controllers configuration map.
  * </p>
  * <p>
  * You may extend existing template configurations by setting the
  * {@link #setParent(TemplateController) parent} property to another
  * TemplateController. The local configuration will then be merged with the
  * one of the parent, overriding previously defined URLs.
  * </p>
  * <p>
  * Additionally the controller supports nested templates, i.e. the URL of a
  * slot may in turn map to another TemplateController. These nested structures
  * are taken into account when configurations are merged. When extending a
  * parent you may also override URLs defined in nested templates.
  * </p>
  * <p>
  * Let's say Template A has two slots, <i>left</i> and <i>right</i>. The right
  * slot includes another Template B which also has two slots <i>top</i> and
  * <i>bottom</i>, where <i>top</i> contains the URL <code>/foo.html</code>.
  * </p>
  * We can now define a third TemplateController A2 which extends A:
  * <pre>
  * &lt;template:definition name="A2" parent="A"&gt;
  *     &lt;template:insert slot="right.top" url="/bar.html" /&gt;
  * &lt;/template:definition&gt;
  * </pre>
  * <p>
  * The syntax above makes use of the Spring 2.0 namespace support. See
  * {@link org.riotfamily.website.template.config.TemplateNamespaceHandler}
  * for more information.
  * </p>
  *
  * @author Alf Werder
  * @author Felix Gnass
  */
 public class TemplateController extends AbstractController
 		implements InitializingBean {
 
 	/** NOTE: The DispatcherServlet class name prefix forces an attribute
 	 * cleanup to be performed after an include, regardless of the servlet's
 	 * cleanupAfterIncludes setting.
 	 */
 	protected static final String SLOTS_CONFIGURATION_ATTRIBUTE =
 			DispatcherServlet.class.getName() + "#" +
 			TemplateController.class.getName() + ".slots";
 
 	protected static final String SLOT_PATH_ATTRIBUTE =
 		DispatcherServlet.class.getName() + "#" +
 		TemplateController.class.getName() + ".slotPath";
 	
 	protected static final String SLOT_PARAMETER =
 			TemplateController.class.getName() + ".SLOT";
 
 	private TemplateController parent;
 
 	private String viewName;
 
 	private Map configuration;
 
 	private Map mergedConfiguration;
 
 	private boolean session;
 
 	public TemplateController getParent() {
 		return parent;
 	}
 
 	public void setParent(TemplateController parent) {
 		this.parent = parent;
 	}
 	
 	public String getViewName() {
 		return viewName;
 	}
 
 	public void setViewName(String view) {
 		this.viewName = view;
 	}
 
 	public Map getConfiguration() {
 		return configuration;
 	}
 
 	public void setConfiguration(Map configuration) {
 		this.configuration = configuration;
 	}
 
 	public void setSession(boolean session) {
 		this.session = session;
 	}
 
 	/**
 	 * Initializes the controller after all properties have been set. If a
 	 * parent controller is set the view will be inherited (if not set locally).
 	 */
 	public final void afterPropertiesSet() throws Exception {
 		inheritView();
 		inheritConfiguration();
 		initController();
 	}
 	
 	/**
 	 * Subclasses may overwrite this method to perform initialization tasks 
 	 * after all properties have been set. The default implementation 
 	 * does nothing.
 	 */
 	protected void initController() {
 	}
 		
 	/**
 	 * Sets the view to the parent view if it has not been set locally.
 	 */
 	private void inheritView() {
 		if (viewName == null && getParent() != null) {
 			viewName = getParent().getViewName();
 		}
 	}
 	
 	/**
 	 * Merges the configuration map with the ones defined by ancestors.
 	 */
 	protected void inheritConfiguration() {
 		mergedConfiguration = new HashMap();
 		if (parent != null) {
 			mergedConfiguration.putAll(parent.getMergedConfiguration());
 		}
 		if (configuration != null) {
 			mergedConfiguration.putAll(configuration);
 		}
 	}
 	
 	protected Map getMergedConfiguration() {
 		return this.mergedConfiguration;
 	}
 
 	/**
 	 * Gets the effective configuration in case the template is nested within
 	 * another template. The surrounding template(s) may override the local 
 	 * slot configuration. 
 	 */
 	private Map getEffectiveConfiguration(HttpServletRequest request) {
 		Map effectiveConfiguration = new HashMap(getMergedConfiguration());
 		applyOverrides(effectiveConfiguration, request);
 		return effectiveConfiguration;
 	}
 
 	/**
 	 * Applies the overrides defined by surrounding templates.
 	 */
 	protected void applyOverrides(Map config, HttpServletRequest request) {
 		Map slotsConfiguration = (Map) request.getAttribute(
 				SLOTS_CONFIGURATION_ATTRIBUTE);
 
 		if (slotsConfiguration != null) {
 			String slot = request.getParameter(SLOT_PARAMETER);
 			if (slot != null) {
 				String prefix = slot + '.';
 				config.putAll(selectEntries(slotsConfiguration, prefix));
 			}
 			else {
 				config.putAll(slotsConfiguration);
 			}
 		}
 	}
 	
 	/**
 	 * Creates a new map containing all entries starting with the given prefix.
 	 * The prefix is stripped from the keys of the new map.
 	 */
 	private static Map selectEntries(Map map, String prefix) {
 		Map result = new HashMap();
 		int prefixLength = prefix.length();
 		Iterator i = map.entrySet().iterator();
 		while (i.hasNext()) {
 			Map.Entry entry = (Map.Entry) i.next();
 			String key = (String) entry.getKey();
 			if (key.startsWith(prefix)) {
 				result.put(key.substring(prefixLength), entry.getValue());
 			}
 		}
 		return result;
 	}
 
 	/**
 	 * Builds a map of URLs that is used as model for the template view.
 	 */
 	protected Map buildUrlMap(Map config) {
 		Map model = new HashMap();
 		Iterator i = config.entrySet().iterator();
 		while (i.hasNext()) {
 			Map.Entry entry = (Map.Entry) i.next();
 			String slot = (String) entry.getKey();
 			if (slot.indexOf('.') == -1) {
 				if (entry.getValue() != null) {
 					model.put(slot, getUrlMapValue(entry.getValue(), slot));
 				}
 				else {
 					model.remove(slot);
 				}
 			}
 		}
 		return model;
 	}
 
 	/**
 	 * Returns either a single String or a list of URLs. 
 	 */
 	protected Object getUrlMapValue(Object value, String slot) {
 		if (value instanceof Collection) {
 			ArrayList urls = new ArrayList();
 			Iterator it = ((Collection) value).iterator();
 			while (it.hasNext()) {
 				String location = (String) it.next();
 				urls.add(getSlotUrl(location, slot));
 			}
 			return urls;
 		}
 		else {
 			return getSlotUrl((String) value, slot);
 		}
 	}
 	
 	/**
 	 * Returns the include URL for the given location and slot. By default
 	 * <code>SLOT_REQUEST_PARAMETER_NAME</code> is appended, containing the
 	 * given slot name.
 	 */
 	protected String getSlotUrl(String location, String slot) {
 		StringBuffer url = new StringBuffer();
 		url.append(location);
 		url.append((url.indexOf("?") != -1) ? '&' : '?');
 		url.append(SLOT_PARAMETER);
 		url.append('=');
 		url.append(slot);
 		return url.toString();
 	}
 
 	protected ModelAndView handleRequestInternal(HttpServletRequest request,
 			HttpServletResponse response) throws Exception {
 
 		if (session) {
 			request.getSession();
 		}
 		Map config = getEffectiveConfiguration(request);
 		request.setAttribute(SLOTS_CONFIGURATION_ATTRIBUTE, config);
 		request.setAttribute(SLOT_PATH_ATTRIBUTE, getSlotPath(request));
 		return new ModelAndView(getViewName(), buildUrlMap(config));
 	}
 
 	/**
 	 * Returns the fully qualified slot-path for the given request.
 	 */
 	public static String getSlotPath(HttpServletRequest request) {
 		String slotPath = (String) request.getAttribute(SLOT_PATH_ATTRIBUTE);
 		String slot = request.getParameter(SLOT_PARAMETER);
 		if (slot != null) {
 			if (slotPath != null) {
 				slotPath = slotPath + '.' + slot;
 			}
 			else {
 				slotPath = slot;
 			}
 		}
 		return slotPath;
 	}
 
 	/**
 	 * Returns whether the given request is handled by a TemplateController and
 	 * no further includes have been performed.
 	 */
 	public static boolean isInTemplate(HttpServletRequest request) {
		Map slots = (Map) request.getAttribute(SLOTS_CONFIGURATION_ATTRIBUTE);
		String slotName = request.getParameter(SLOT_PARAMETER);
		if (slots == null || slotName == null) {
 			return false;
 		}
		Object content = slots.get(slotName);
		String uri = ServletUtils.getPathWithinApplication(request);
		if (content instanceof Collection) {
			Collection c = (Collection) content;
			return c.contains(uri);
		}
		else {
			return uri.equals(content);
		}
 	}
 }
