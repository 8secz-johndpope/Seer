 /*
  * Copyright 2007-2012 The Europeana Foundation
  *
  *  Licenced under the EUPL, Version 1.1 (the "Licence") and subsequent versions as approved 
  *  by the European Commission;
  *  You may not use this work except in compliance with the Licence.
  *  
  *  You may obtain a copy of the Licence at:
  *  http://joinup.ec.europa.eu/software/page/eupl
  *
  *  Unless required by applicable law or agreed to in writing, software distributed under 
  *  the Licence is distributed on an "AS IS" basis, without warranties or conditions of 
  *  any kind, either express or implied.
  *  See the Licence for the specific language governing permissions and limitations under 
  *  the Licence.
  */
 
 package eu.europeana.portal2.web.util;
 
 import java.io.PrintWriter;
 import java.io.StringWriter;
 import java.sql.BatchUpdateException;
 import java.text.MessageFormat;
 import java.util.Iterator;
 import java.util.Locale;
 import java.util.Map;
 import java.util.logging.Logger;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 import javax.servlet.http.HttpServletRequest;
 
 import org.apache.commons.lang.StringUtils;
 import org.springframework.security.core.Authentication;
 import org.springframework.security.core.context.SecurityContextHolder;
 import org.springframework.web.servlet.LocaleResolver;
 import org.springframework.web.servlet.ModelAndView;
 import org.springframework.web.servlet.support.RequestContextUtils;
 
 import eu.europeana.corelib.db.service.UserService;
 import eu.europeana.corelib.definitions.db.entity.relational.User;
 import eu.europeana.corelib.web.model.PageData;
 import eu.europeana.corelib.web.model.PageInfo;
 import eu.europeana.portal2.web.presentation.ThemeChecker;
 import eu.europeana.portal2.web.presentation.model.PortalPageData;
 import eu.europeana.portal2.web.security.Portal2UserDetails;
 
 /**
  * Utility methods for controllers
  * 
  * @author Gerald de Jong <geralddejong@gmail.com>
  */
 
 public class ControllerUtil {
 
 	private static final String EMAIL_REGEXP = "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[_A-Za-z0-9-]+)";
 
 	private static final Pattern[] SEE_ALSO_PATTERNS = new Pattern[]{
		Pattern.compile("\\s*\\([^\\(\\)]*?\\)\\s*$"),
		Pattern.compile("\\s*\\[[^\\[\\]]*?\\]\\s*$"),
		Pattern.compile("\\s*<[^<>]*?>\\.?\\s*$"),
 		Pattern.compile("\\s*\\d+-\\d+\\s*$")
 	};
 	private static final String EMPTY_STRING = "";
 
 
 	public static boolean validEmailAddress(String emailAddress) {
 		return emailAddress.endsWith("@localhost") || emailAddress.matches(EMAIL_REGEXP);
 	}
 
 	// @Value("#{europeanaProperties['portal.theme']}")
 	// private static String defaultTheme;
 
 	private static Logger log = Logger.getLogger(ControllerUtil.class.getName());
 
 	public static Locale getLocale(HttpServletRequest request) {
 		LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
 		return localeResolver.resolveLocale(request);
 	}
 
 	/*
 	 * This creates the default ModelAndView for the portal applications. It
 	 * should be used in every Controller.
 	 */
 	public static ModelAndView createModelAndViewPage(PortalPageData model, Locale locale, PageInfo view) {
 		// adjust model
 		if (locale != null && model.getLocale() == null) {
 			model.setLocale(locale);
 		}
 		model.setPageInfo(view);
 		if (StringUtils.isBlank(model.getPageTitle())) {
 			model.setPageTitle(view.getPageTitle());
 		}
 
 		// create page
 		ModelAndView page = new ModelAndView(model.getTheme() + "/" + view.getTemplate());
 		page.addObject(PageData.PARAM_MODEL, model);
 		return page;
 	}
 
 	public static ModelAndView createModelAndViewPage(PortalPageData model, PageInfo view) {
 		return createModelAndViewPage(model, null, view);
 	}
 
 	/*
 	 * Format full requested uri from HttpServletRequest
 	 */
 	@SuppressWarnings("unchecked")
 	public static String formatFullRequestUrl(HttpServletRequest request) {
 		StringBuffer requestURL = request.getRequestURL();
 		if (request.getQueryString() != null) {
 			requestURL.append("?").append(request.getQueryString());
 		} else if (request.getParameterMap() != null) {
 			requestURL.append(formatParameterMapAsQueryString(request.getParameterMap()));
 		}
 		return requestURL.toString();
 	}
 
 	public static String formatParameterMapAsQueryString(
 			Map<String, String[]> parameterMap) {
 		StringBuilder output = new StringBuilder();
 		output.append("?");
 		Iterator<Map.Entry<String, String[]>> iterator1 = parameterMap.entrySet().iterator();
 		while (iterator1.hasNext()) {
 			Map.Entry<String, String[]> entry = iterator1.next();
 			if (entry.getValue().length > 0) {
 				output.append(MessageFormat.format("{0}={1}", entry.getKey(), entry.getValue()[0]));
 			} else {
 				output.append(MessageFormat.format("{0}={1}", entry.getKey(), ""));
 			}
 			if (iterator1.hasNext()) {
 				output.append("&");
 			}
 		}
 		return output.toString();
 	}
 
 	public static String getFullServletUrl(HttpServletRequest request) {
 		String url = request.getRequestURL().toString();
 		int index = url.indexOf(request.getServerName());
 		url = url.substring(0, index) + request.getServerName() + ":"
 				+ request.getServerPort() + request.getRequestURI();
 		return url;
 	}
 
 	/**
 	 * Returns the right theme variable.
 	 * 
 	 * It checks the request parameter "theme", and the default theme defined in the property file "portal.theme".
 	 * 
 	 * @param request
 	 *   Actual HTTP request object
 	 * @param defaultTheme
 	 *   The default theme comes from the property file
 	 *
 	 * @return
 	 *   A valid theme name
 	 */
 	public static String getSessionManagedTheme(HttpServletRequest request, String defaultTheme) {
 		// HttpSession session = request.getSession(true);
 		String theme = request.getParameter("theme");
 		if (!StringUtils.isBlank(theme)) {
 			theme = ThemeChecker.check(theme, defaultTheme);
 			// session.setAttribute("theme", theme);
 		}
 		/*
 		else {
 			String storedTheme = (String)session.getAttribute("theme");
 			if (!StringUtils.isBlank(storedTheme)) {
 				theme = ThemeChecker.check(storedTheme, defaultTheme);
 				log.info("theme2: " + theme);
 			}
 		}
 		*/
 		if (StringUtils.isBlank(theme)) {
 			// theme = ThemeChecker.check(defaultTheme);
 			theme = ThemeChecker.DEFAULT_THEME;
 		}
 		return theme;
 	}
 
 	public static User getUser(UserService userService) {
 		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
 		if (authentication == null) {
 			log.severe("Authentication is null");
 			return null;
 		}
 		User user = null;
 		Object principal = (Object)authentication.getPrincipal();
 		if (principal instanceof Portal2UserDetails) {
 			user = userService.findByEmail(((Portal2UserDetails)principal).getUsername());
 		} else {
 			// log.warning("Principal is not Portal2UserDetails: " + principal.toString());
 			// TODO: it is not a logged-in-user, do we need to get default information?
 			/*
 			try {
 				user = userService.findByID(principal.toString());
 				log.info("User: " + user.toString());
 			} catch (DatabaseException e) {
 				log.severe("DatabaseException while getting user: " + e.getLocalizedMessage());
 				e.printStackTrace();
 			}
 			*/
 		}
 		return user;
 	}
 
 	public static String clearSeeAlso(String value) {
 		value = value.replaceAll("\n", " ")
 				.replaceAll("\\s+", " ")
 		;
 
 		log.info(value);
 		Matcher m;
 		boolean doNext = false;
 		do {
 			doNext = false;
 			for (Pattern pattern : SEE_ALSO_PATTERNS) {
 				m = pattern.matcher(value);
 				while (m.find()) {
 					doNext = true;
 					value = m.replaceFirst(EMPTY_STRING);
 					log.info("'" + value + "'");
 					m = pattern.matcher(value);
 				}
 			}
 		} while (doNext == true);
 
 		value = value.replaceAll("\\/", "\\\\/");
 
 		return value;
 	}
 
 	public static String getStackTrace(Exception exception) {
 		StringWriter stringWriter = new StringWriter();
 		PrintWriter printWriter = new PrintWriter(stringWriter);
 		for (Throwable e = exception.getCause(); e != null; e = e.getCause()) {
 			if (e instanceof BatchUpdateException) {
 				BatchUpdateException bue = (BatchUpdateException) e;
 				Exception next = bue.getNextException();
 				log.warning("Next exception in batch: " + next);
 				next.printStackTrace(printWriter);
 			}
 		}
 		exception.printStackTrace(printWriter);
 		return stringWriter.toString();
 	}
 
 	public static void logTime(String type, long time) {
 		log.fine(String.format("elapsed time (%s): %d", type, time));
 	}
 }
