 /**
  * Copyright 2010 Ralph Schaer <ralphschaer@gmail.com>
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *   http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package ch.ralscha.extdirectspring.api;
 
 import java.io.IOException;
 import java.lang.reflect.Method;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
 
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 
 import org.apache.commons.logging.LogFactory;
 import org.springframework.context.ApplicationContext;
 import org.springframework.context.ApplicationContextAware;
 import org.springframework.core.annotation.AnnotationUtils;
 import org.springframework.stereotype.Controller;
 import org.springframework.util.StringUtils;
 import org.springframework.web.bind.annotation.RequestMapping;
 import org.springframework.web.bind.annotation.RequestMethod;
 import org.springframework.web.bind.annotation.RequestParam;
 
 import ch.ralscha.extdirectspring.annotation.ExtDirectMethod;
 import ch.ralscha.extdirectspring.annotation.ExtDirectMethodType;
 import ch.ralscha.extdirectspring.util.ExtDirectSpringUtil;
 import ch.ralscha.extdirectspring.util.SupportedParameterTypes;
 
 /**
  * Spring managed controller that handles /api.jsp and /api-debug.js requests
  * 
  * @author Ralph Schaer
 * @author jeffreiffers
  */
 @Controller
 public class ApiController implements ApplicationContextAware {
 
 	private ApplicationContext context;
 
 	@Override
 	public void setApplicationContext(ApplicationContext context) {
 		this.context = context;
 	}
 
 	/**
 	 * Method that handles api.js calls. Generates a Javascript with the necessary
 	 * code for Ext.Direct
 	 * 
 	 * @param apiNs
 	 *          Name of the namespace the variable remotingApiVar will live in.
 	 *          Defaults to Ext.app
 	 * @param actionNs
 	 *          Name of the namespace the action will live in.
 	 * @param remotingApiVar
 	 *          Name of the remoting api variable. Defaults to REMOTING_API
 	 * @param pollingUrlsVar
 	 *          Name of the polling urls object. Defaults to POLLING_URLS
 	 * @param group
 	 *          Name of the api group
 	 * @param fullRouterUrl 
 	 *          If true the router property contains the full request URL with method, server and port. Defaults to false
 	 *          returns only the url without method, server and port           
 	 * @param format 
 	 *          Only valid value is "json2. Ext Designer sends this parameter and the response is a valid JSON.
 	 *          Defaults to null and response is standard Javascript.
 	 * @param request
 	 * @param response
 	 * @throws IOException
 	 */
 	@RequestMapping(value = { "/api.js", "/api-debug.js" }, method = RequestMethod.GET)
 	public void api(
 			@RequestParam(value = "apiNs", required = false, defaultValue = "Ext.app") final String apiNs,
 			@RequestParam(value = "actionNs", required = false) final String actionNs,
 			@RequestParam(value = "remotingApiVar", required = false, defaultValue = "REMOTING_API") final String remotingApiVar,
 			@RequestParam(value = "pollingUrlsVar", required = false, defaultValue = "POLLING_URLS") final String pollingUrlsVar,
 			@RequestParam(value = "group", required = false) final String group,
 			@RequestParam(value = "fullRouterUrl", required = false, defaultValue = "false") final boolean fullRouterUrl,
 			@RequestParam(value = "format", required = false) final String format, final HttpServletRequest request,
 			final HttpServletResponse response) throws IOException {
 
 		if (format == null) {
 			response.setContentType("application/x-javascript");
 
 			String requestUrlString;
 
 			if (fullRouterUrl) {
 				requestUrlString = request.getRequestURL().toString();
 			} else {
 				requestUrlString = request.getRequestURI();
 			}
 
 			boolean debug = requestUrlString.contains("api-debug.js");
 
 			ApiCacheKey apiKey = new ApiCacheKey(apiNs, actionNs, remotingApiVar, pollingUrlsVar, group, debug);
 			String apiString = ApiCache.INSTANCE.get(apiKey);
 			if (apiString == null) {
 
 				String routerUrl;
 				String basePollUrl;
 
 				if (!debug) {
 					routerUrl = requestUrlString.replace("api.js", "router");
 					basePollUrl = requestUrlString.replace("api.js", "poll");
 				} else {
 					routerUrl = requestUrlString.replace("api-debug.js", "router");
 					basePollUrl = requestUrlString.replace("api-debug.js", "poll");
 				}
 				apiString = buildApiString(apiNs, actionNs, remotingApiVar, pollingUrlsVar, routerUrl, basePollUrl,
 						group, debug);
 				ApiCache.INSTANCE.put(apiKey, apiString);
 			}
 
 			response.getOutputStream().write(apiString.getBytes());
 		} else {
 			response.setContentType("application/json");
 			String requestUrlString = request.getRequestURL().toString();
 
 			boolean debug = requestUrlString.contains("api-debug.js");
 
 			String routerUrl;
 			if (!debug) {
 				routerUrl = requestUrlString.replace("api.js", "router");
 			} else {
 				routerUrl = requestUrlString.replace("api-debug.js", "router");
 			}
 
 			String apiString = buildApiJson(apiNs, actionNs, remotingApiVar, routerUrl, group, debug);
 			response.getOutputStream().write(apiString.getBytes());
 		}
 	}
 
 	private String buildApiString(final String apiNs, final String actionNs, final String remotingApiVar,
 			final String pollingUrlsVar, final String routerUrl, final String basePollUrl, final String group,
 			final boolean debug) {
 
 		RemotingApi remotingApi = new RemotingApi(routerUrl, actionNs);
 		scanForExtDirectMethods(remotingApi, group);
 
 		StringBuilder sb = new StringBuilder();
 
 		if (StringUtils.hasText(apiNs)) {
 			sb.append("Ext.ns('");
 			sb.append(apiNs);
 			sb.append("');");
 		}
 
 		if (debug) {
 			sb.append("\n\n");
 		}
 
 		if (StringUtils.hasText(actionNs)) {
 			sb.append("Ext.ns('");
 			sb.append(actionNs);
 			sb.append("');");
 
 			if (debug) {
 				sb.append("\n\n");
 			}
 		}
 
 		String jsonConfig = ExtDirectSpringUtil.serializeObjectToJson(remotingApi, debug);
 
 		if (StringUtils.hasText(apiNs)) {
 			sb.append(apiNs).append(".");
 		}
 		sb.append(remotingApiVar).append(" = ");
 		sb.append(jsonConfig);
 		sb.append(";");
 
 		if (debug) {
 			sb.append("\n\n");
 		}
 
 		List<PollingProvider> pollingProviders = remotingApi.getPollingProviders();
 		if (!pollingProviders.isEmpty()) {
 
 			if (StringUtils.hasText(apiNs)) {
 				sb.append(apiNs).append(".");
 			}
 			sb.append(pollingUrlsVar).append(" = {");
 			if (debug) {
 				sb.append("\n");
 			}
 
 			for (int i = 0; i < pollingProviders.size(); i++) {
 				if (debug) {
 					sb.append("  ");
 				}
 
 				sb.append("\"");
 				sb.append(pollingProviders.get(i).getEvent());
 				sb.append("\"");
 				sb.append(" : \"").append(basePollUrl).append("/");
 				sb.append(pollingProviders.get(i).getBeanName());
 				sb.append("/");
 				sb.append(pollingProviders.get(i).getMethod());
 				sb.append("/");
 				sb.append(pollingProviders.get(i).getEvent());
 				sb.append("\"");
 				if (i < pollingProviders.size() - 1) {
 					sb.append(",");
 					if (debug) {
 						sb.append("\n");
 					}
 				}
 			}
 			if (debug) {
 				sb.append("\n");
 			}
 			sb.append("};");
 		}
 
 		return sb.toString();
 	}
 
 	private String buildApiJson(final String apiNs, final String actionNs, final String remotingApiVar,
 			final String routerUrl, final String group, final boolean debug) {
 
 		RemotingApi remotingApi = new RemotingApi(routerUrl, actionNs);
 
 		if (StringUtils.hasText(apiNs)) {
 			remotingApi.setDescriptor(apiNs + "." + remotingApiVar);
 		} else {
 			remotingApi.setDescriptor(remotingApiVar);
 		}
 
 		scanForExtDirectMethods(remotingApi, group);
 
 		return ExtDirectSpringUtil.serializeObjectToJson(remotingApi, debug);
 
 	}
 
 	private void scanForExtDirectMethods(final RemotingApi remotingApi, final String group) {
 		Map<String, Class<?>> beanDefinitions = getAllBeanClasses();
 
 		for (Entry<String, Class<?>> entry : beanDefinitions.entrySet()) {
 			Class<?> beanClass = entry.getValue();
 			String beanName = entry.getKey();
 
 			Method[] methods = beanClass.getMethods();
 
 			for (Method method : methods) {
 				ExtDirectMethod annotation = AnnotationUtils.findAnnotation(method, ExtDirectMethod.class);
 				if (annotation != null && isSameGroup(group, annotation.group())) {
 					ExtDirectMethodType type = annotation.value();
 
 					switch (type) {
 					case SIMPLE:
 						remotingApi.addAction(beanName, method.getName(), numberOfParameters(method));
 						break;
 					case FORM_LOAD:
 					case STORE_READ:
 					case STORE_MODIFY:
 						remotingApi.addAction(beanName, method.getName(), 1);
 						break;
 					case FORM_POST:
 						if (isValidFormPostMethod(method)) {
 							remotingApi.addAction(beanName, method.getName(), 0, true);
 						} else {
 							LogFactory
 									.getLog(getClass())
 									.warn("Method '"
 											+ beanName
 											+ "."
 											+ method.getName()
 											+ "' is annotated as a form post method but is not valid. "
 											+ "A form post method must be annotated with @RequestMapping and method=RequestMethod.POST. Method ignored.");
 						}
 						break;
 					case POLL:
 						remotingApi.addPollingProvider(beanName, method.getName(), annotation.event());
 						break;
 
 					}
 
 				}
 
 			}
 
 		}
 	}
 
 	private int numberOfParameters(final Method method) {
 		Class<?>[] parameterTypes = method.getParameterTypes();
 		int paramLength = 0;
 		for (Class<?> parameterType : parameterTypes) {
 			if (!SupportedParameterTypes.isSupported(parameterType)) {
 				paramLength++;
 			}
 		}
 		return paramLength;
 	}
 
 	private boolean isSameGroup(final String requestedGroup, final String annotationGroup) {
 		if (requestedGroup != null) {
 			return ExtDirectSpringUtil.equal(requestedGroup, annotationGroup);
 		}
 
 		return true;
 	}
 
 	private boolean isValidFormPostMethod(final Method method) {
 		if (AnnotationUtils.findAnnotation(method.getDeclaringClass(), Controller.class) == null) {
 			return false;
 		}
 
 		RequestMapping methodAnnotation = AnnotationUtils.findAnnotation(method, RequestMapping.class);
 
 		if (methodAnnotation == null) {
 			return false;
 		}
 
 		RequestMapping classAnnotation = AnnotationUtils.findAnnotation(method.getDeclaringClass(),
 				RequestMapping.class);
 
 		boolean hasValue = false;
 
 		if (classAnnotation != null) {
 			hasValue = (classAnnotation.value() != null && classAnnotation.value().length > 0);
 		}
 
 		if (!hasValue) {
 			hasValue = (methodAnnotation.value() != null && methodAnnotation.value().length > 0);
 		}
 
 		return hasValue && hasPostMethod(methodAnnotation.method());
 	}
 
 	private boolean hasPostMethod(RequestMethod[] methods) {
 		if (methods != null) {
 			for (RequestMethod method : methods) {
 				if (method.equals(RequestMethod.POST)) {
 					return true;
 				}
 			}
 		}
 
 		return false;
 	}
 
 	private Map<String, Class<?>> getAllBeanClasses() {
 		Map<String, Class<?>> beanClasses = new HashMap<String, Class<?>>();
 
 		ApplicationContext currentCtx = context;
 		do {
 			for (String beanName : currentCtx.getBeanDefinitionNames()) {
 				beanClasses.put(beanName, currentCtx.getType(beanName));
 			}
 			currentCtx = currentCtx.getParent();
 		} while (currentCtx != null);
 
 		return beanClasses;
 	}
 
 }
