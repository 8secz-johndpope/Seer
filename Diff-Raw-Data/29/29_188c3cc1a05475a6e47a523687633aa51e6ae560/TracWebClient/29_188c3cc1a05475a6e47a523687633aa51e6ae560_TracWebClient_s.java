 /*******************************************************************************
  * Copyright (c) 2006, 2008 Steffen Pingel and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     Tasktop Technologies - initial API and implementation
  *******************************************************************************/
 
 package org.eclipse.mylyn.internal.trac.core.client;
 
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.io.StreamTokenizer;
 import java.io.StringReader;
 import java.net.HttpURLConnection;
 import java.text.ParseException;
 import java.util.ArrayList;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.StringTokenizer;
 
 import javax.swing.text.html.HTML.Tag;
 
 import org.apache.commons.httpclient.HostConfiguration;
 import org.apache.commons.httpclient.HttpClient;
 import org.apache.commons.httpclient.HttpStatus;
 import org.apache.commons.httpclient.auth.AuthScope;
 import org.apache.commons.httpclient.methods.GetMethod;
 import org.apache.commons.lang.StringEscapeUtils;
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.core.runtime.IStatus;
 import org.eclipse.core.runtime.OperationCanceledException;
 import org.eclipse.core.runtime.Status;
 import org.eclipse.mylyn.commons.core.StatusHandler;
 import org.eclipse.mylyn.commons.net.AbstractWebLocation;
 import org.eclipse.mylyn.commons.net.AuthenticationCredentials;
 import org.eclipse.mylyn.commons.net.AuthenticationType;
 import org.eclipse.mylyn.commons.net.HtmlStreamTokenizer;
 import org.eclipse.mylyn.commons.net.HtmlTag;
 import org.eclipse.mylyn.commons.net.Policy;
 import org.eclipse.mylyn.commons.net.UnsupportedRequestException;
 import org.eclipse.mylyn.commons.net.WebUtil;
 import org.eclipse.mylyn.commons.net.HtmlStreamTokenizer.Token;
 import org.eclipse.mylyn.internal.trac.core.TracCorePlugin;
 import org.eclipse.mylyn.internal.trac.core.model.TracComponent;
 import org.eclipse.mylyn.internal.trac.core.model.TracMilestone;
 import org.eclipse.mylyn.internal.trac.core.model.TracPriority;
 import org.eclipse.mylyn.internal.trac.core.model.TracRepositoryInfo;
 import org.eclipse.mylyn.internal.trac.core.model.TracSearch;
 import org.eclipse.mylyn.internal.trac.core.model.TracSearchFilter;
 import org.eclipse.mylyn.internal.trac.core.model.TracSeverity;
 import org.eclipse.mylyn.internal.trac.core.model.TracTicket;
 import org.eclipse.mylyn.internal.trac.core.model.TracTicketResolution;
 import org.eclipse.mylyn.internal.trac.core.model.TracTicketStatus;
 import org.eclipse.mylyn.internal.trac.core.model.TracTicketType;
 import org.eclipse.mylyn.internal.trac.core.model.TracVersion;
 import org.eclipse.mylyn.internal.trac.core.model.TracSearchFilter.CompareOperator;
 import org.eclipse.mylyn.internal.trac.core.model.TracTicket.Key;
 import org.eclipse.mylyn.internal.trac.core.util.TracUtil;
 import org.eclipse.mylyn.internal.trac.core.util.TracHttpClientTransportFactory.TracHttpException;
 
 /**
  * Represents a Trac repository that is accessed through the Trac's query script and web interface.
  * 
  * @author Steffen Pingel
  */
 public class TracWebClient extends AbstractTracClient {
 
 	private class Request {
 
 		private final String url;
 
 		private HostConfiguration hostConfiguration;
 
 		public Request(String url) {
 			this.url = url;
 		}
 
 		public GetMethod execute(IProgressMonitor monitor) throws TracLoginException, IOException, TracHttpException {
 			hostConfiguration = WebUtil.createHostConfiguration(httpClient, location, monitor);
 
 			for (int attempt = 0; attempt < 2; attempt++) {
 				// force authentication
 				if (!authenticated) {
 					AuthenticationCredentials credentials = location.getCredentials(AuthenticationType.REPOSITORY);
 					if (credentialsValid(credentials)) {
 						authenticate(monitor);
 					}
 				}
 
 				GetMethod method = new GetMethod(WebUtil.getRequestPath(url));
 				int code;
 				try {
 					code = WebUtil.execute(httpClient, hostConfiguration, method, monitor);
 				} catch (IOException e) {
 					method.releaseConnection();
 					throw e;
 				}
 
 				if (code == HttpURLConnection.HTTP_OK) {
 					return method;
 				} else if (code == HttpURLConnection.HTTP_UNAUTHORIZED || code == HttpURLConnection.HTTP_FORBIDDEN) {
 					// login or re-authenticate due to an expired session
 					method.releaseConnection();
 					authenticated = false;
 					authenticate(monitor);
 				} else {
 					throw new TracHttpException(code);
 				}
 			}
 
 			throw new TracLoginException();
 		}
 
 		private void authenticate(IProgressMonitor monitor) throws TracLoginException, IOException {
 			while (true) {
 				AuthenticationCredentials credentials = location.getCredentials(AuthenticationType.REPOSITORY);
 				if (!credentialsValid(credentials)) {
 					throw new TracLoginException();
 				}
 
 				// try standard basic/digest/ntlm authentication first
 				AuthScope authScope = new AuthScope(WebUtil.getHost(repositoryUrl), WebUtil.getPort(repositoryUrl),
 						null, AuthScope.ANY_SCHEME);
 				httpClient.getState().setCredentials(authScope,
 						WebUtil.getHttpClientCredentials(credentials, WebUtil.getHost(repositoryUrl)));
 
 				GetMethod method = new GetMethod(WebUtil.getRequestPath(repositoryUrl + LOGIN_URL));
 				method.setFollowRedirects(false);
 				int code;
 				try {
 					code = WebUtil.execute(httpClient, hostConfiguration, method, monitor);
 					if (needsReauthentication(code, monitor)) {
 						continue;
 					}
 				} finally {
 					method.releaseConnection();
 				}
 
 				// the expected return code is a redirect, anything else is suspicious
 				if (code == HttpURLConnection.HTTP_OK) {
 					// try form-based authentication via AccountManagerPlugin as a
 					// fall-back
 					authenticateAccountManager(httpClient, hostConfiguration, credentials, monitor);
 				}
 
 				validateAuthenticationState(httpClient);
 
 				// success since no exception was thrown
 				authenticated = true;
 				break;
 			}
 		}
 
 		private boolean needsReauthentication(int code, IProgressMonitor monitor) throws IOException,
 				TracLoginException {
 			final AuthenticationType authenticationType;
 			if (code == HttpStatus.SC_UNAUTHORIZED || code == HttpStatus.SC_FORBIDDEN) {
 				authenticationType = AuthenticationType.REPOSITORY;
 			} else if (code == HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED) {
 				authenticationType = AuthenticationType.PROXY;
 			} else {
 				return false;
 			}
 
 			try {
 				location.requestCredentials(authenticationType, null, monitor);
 			} catch (UnsupportedRequestException e) {
 				throw new TracLoginException();
 			}
 
 			hostConfiguration = WebUtil.createHostConfiguration(httpClient, location, monitor);
 			return true;
 		}
 
 	}
 
 	private final HttpClient httpClient;
 
 	private boolean authenticated;
 
 	public TracWebClient(AbstractWebLocation location, Version version) {
 		super(location, version);
 		this.httpClient = createHttpClient();
 	}
 
 	private synchronized GetMethod connect(String requestUrl, IProgressMonitor monitor) throws TracException {
 		monitor = Policy.monitorFor(monitor);
 		try {
 			Request request = new Request(requestUrl);
 			return request.execute(monitor);
 		} catch (TracException e) {
 			throw e;
 		} catch (Exception e) {
 			throw new TracException(e);
 		}
 	}
 
 	/**
 	 * Fetches the web site of a single ticket and returns the Trac ticket.
 	 * 
 	 * @param id
 	 *            Trac id of ticket
 	 */
 	public TracTicket getTicket(int id, IProgressMonitor monitor) throws TracException {
 		GetMethod method = connect(repositoryUrl + ITracClient.TICKET_URL + id, monitor);
 		try {
 			TracTicket ticket = new TracTicket(id);
 
 			InputStream in = WebUtil.getResponseBodyAsStream(method, monitor);
 			try {
 				BufferedReader reader = new BufferedReader(new InputStreamReader(in, method.getResponseCharSet()));
 				HtmlStreamTokenizer tokenizer = new HtmlStreamTokenizer(reader, null);
 				for (Token token = tokenizer.nextToken(); token.getType() != Token.EOF; token = tokenizer.nextToken()) {
 					if (token.getType() == Token.TAG) {
 						HtmlTag tag = (HtmlTag) token.getValue();
 						if (tag.getTagType() == Tag.TD) {
 							String headers = tag.getAttribute("headers"); //$NON-NLS-1$
 							if ("h_component".equals(headers)) { //$NON-NLS-1$
 								ticket.putBuiltinValue(Key.COMPONENT, getText(tokenizer));
 							} else if ("h_milestone".equals(headers)) { //$NON-NLS-1$
 								ticket.putBuiltinValue(Key.MILESTONE, getText(tokenizer));
 							} else if ("h_priority".equals(headers)) { //$NON-NLS-1$
 								ticket.putBuiltinValue(Key.PRIORITY, getText(tokenizer));
 							} else if ("h_severity".equals(headers)) { //$NON-NLS-1$
 								ticket.putBuiltinValue(Key.SEVERITY, getText(tokenizer));
 							} else if ("h_version".equals(headers)) { //$NON-NLS-1$
 								ticket.putBuiltinValue(Key.VERSION, getText(tokenizer));
 							} else if ("h_keywords".equals(headers)) { //$NON-NLS-1$
 								ticket.putBuiltinValue(Key.KEYWORDS, getText(tokenizer));
 							} else if ("h_cc".equals(headers)) { //$NON-NLS-1$
 								ticket.putBuiltinValue(Key.CC, getText(tokenizer));
 							} else if ("h_owner".equals(headers)) { //$NON-NLS-1$
 								ticket.putBuiltinValue(Key.OWNER, getText(tokenizer));
 							} else if ("h_reporter".equals(headers)) { //$NON-NLS-1$
 								ticket.putBuiltinValue(Key.REPORTER, getText(tokenizer));
 							}
 							// TODO handle custom fields
 						} else if (tag.getTagType() == Tag.H2
 								&& ("summary".equals(tag.getAttribute("class")) || "summary searchable".equals(tag.getAttribute("class")))) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
 							ticket.putBuiltinValue(Key.SUMMARY, getText(tokenizer));
 						} else if (tag.getTagType() == Tag.H3 && "status".equals(tag.getAttribute("class"))) { //$NON-NLS-1$ //$NON-NLS-2$
 							String text = getStrongText(tokenizer);
 							if (text.length() > 0) {
 								// Trac 0.9 format: status / status (resolution)
 								int i = text.indexOf(" ("); //$NON-NLS-1$
 								if (i != -1) {
 									ticket.putBuiltinValue(Key.STATUS, text.substring(0, i));
 									ticket.putBuiltinValue(Key.RESOLUTION, text.substring(i + 2, text.length() - 1));
 								} else {
 									ticket.putBuiltinValue(Key.STATUS, text);
 								}
 							}
 						} else if (tag.getTagType() == Tag.SPAN) {
 							String clazz = tag.getAttribute("class"); //$NON-NLS-1$
 							if ("status".equals(clazz)) { //$NON-NLS-1$
 								// Trac 0.10 format: (status type) / (status type: resolution)
 								String text = getText(tokenizer);
 								if (text.startsWith("(") && text.endsWith(")")) { //$NON-NLS-1$ //$NON-NLS-2$
 									StringTokenizer t = new StringTokenizer(text.substring(1, text.length() - 1), " :"); //$NON-NLS-1$
 									if (t.hasMoreTokens()) {
 										ticket.putBuiltinValue(Key.STATUS, t.nextToken());
 									}
 									if (t.hasMoreTokens()) {
 										ticket.putBuiltinValue(Key.TYPE, t.nextToken());
 									}
 									if (t.hasMoreTokens()) {
 										ticket.putBuiltinValue(Key.RESOLUTION, t.nextToken());
 									}
 								}
 							}
 						}
 						// TODO parse description
 					}
 				}
 			} finally {
 				in.close();
 			}
 
 			if (ticket.isValid() && ticket.getValue(Key.SUMMARY) != null) {
 				return ticket;
 			}
 
 			throw new InvalidTicketException();
 		} catch (IOException e) {
 			throw new TracException(e);
 		} catch (ParseException e) {
 			throw new TracException(e);
 		} finally {
 			method.releaseConnection();
 		}
 	}
 
 	public void searchForTicketIds(TracSearch query, List<Integer> result, IProgressMonitor monitor)
 			throws TracException {
 		List<TracTicket> ticketResult = new ArrayList<TracTicket>();
 		search(query, ticketResult, monitor);
 		for (TracTicket tracTicket : ticketResult) {
 			result.add(tracTicket.getId());
 		}
 	}
 
 	public void search(TracSearch query, List<TracTicket> tickets, IProgressMonitor monitor) throws TracException {
 		GetMethod method = connect(repositoryUrl + ITracClient.QUERY_URL + query.toUrl(), monitor);
 		try {
 			InputStream in = WebUtil.getResponseBodyAsStream(method, monitor);
 			try {
 				BufferedReader reader = new BufferedReader(new InputStreamReader(in, method.getResponseCharSet()));
 				String line;
 
 				Map<String, String> constantValues = getExactMatchValues(query);
 
 				// first line contains names of returned ticket fields
 				line = reader.readLine();
 				if (line == null) {
 					throw new InvalidTicketException();
 				}
 				StringTokenizer t = new StringTokenizer(line, "\t"); //$NON-NLS-1$
 				Key[] fields = new Key[t.countTokens()];
 				for (int i = 0; i < fields.length; i++) {
 					fields[i] = Key.fromKey(t.nextToken());
 				}
 
 				// create a ticket for each following line of output
 				while ((line = reader.readLine()) != null) {
 					t = new StringTokenizer(line, "\t"); //$NON-NLS-1$
 					TracTicket ticket = new TracTicket();
 					for (int i = 0; i < fields.length && t.hasMoreTokens(); i++) {
 						if (fields[i] != null) {
 							try {
 								if (fields[i] == Key.ID) {
 									ticket.setId(Integer.parseInt(t.nextToken()));
 								} else if (fields[i] == Key.TIME) {
 									ticket.setCreated(TracUtil.parseDate(Integer.parseInt(t.nextToken())));
 								} else if (fields[i] == Key.CHANGE_TIME) {
 									ticket.setLastChanged(TracUtil.parseDate(Integer.parseInt(t.nextToken())));
 								} else {
 									ticket.putBuiltinValue(fields[i], parseTicketValue(t.nextToken()));
 								}
 							} catch (NumberFormatException e) {
 								StatusHandler.log(new Status(IStatus.WARNING, TracCorePlugin.ID_PLUGIN,
 										"Error parsing response: '" + line + "'", e)); //$NON-NLS-1$ //$NON-NLS-2$
 							}
 						}
 					}
 
 					if (ticket.isValid()) {
 						for (String key : constantValues.keySet()) {
 							ticket.putValue(key, parseTicketValue(constantValues.get(key)));
 						}
 
 						tickets.add(ticket);
 					}
 				}
 			} finally {
 				in.close();
 			}
 		} catch (IOException e) {
 			throw new TracException(e);
 		} finally {
 			method.releaseConnection();
 		}
 	}
 
 	/**
 	 * Trac has sepcial encoding rules for the returned output: None is represented by "--".
 	 */
 	private String parseTicketValue(String value) {
 		if ("--".equals(value)) { //$NON-NLS-1$
 			return ""; //$NON-NLS-1$
 		}
 		return value;
 	}
 
 	/**
 	 * Extracts constant values from <code>query</code>. The Trac query script does not return fields that matched
 	 * exactly againt a single value.
 	 */
 	private Map<String, String> getExactMatchValues(TracSearch query) {
 		Map<String, String> values = new HashMap<String, String>();
 		List<TracSearchFilter> filters = query.getFilters();
 		for (TracSearchFilter filter : filters) {
 			if (filter.getOperator() == CompareOperator.IS && filter.getValues().size() == 1) {
 				values.put(filter.getFieldName(), filter.getValues().get(0));
 			}
 		}
 		return values;
 	}
 
 	public TracRepositoryInfo validate(IProgressMonitor monitor) throws TracException {
 		GetMethod method = connect(repositoryUrl + "/", monitor); //$NON-NLS-1$
 		try {
 			InputStream in = WebUtil.getResponseBodyAsStream(method, monitor);
 			try {
 				BufferedReader reader = new BufferedReader(new InputStreamReader(in, method.getResponseCharSet()));
 
 				boolean inFooter = false;
 				boolean valid = false;
 				String version = null;
 
 				HtmlStreamTokenizer tokenizer = new HtmlStreamTokenizer(reader, null);
 				for (Token token = tokenizer.nextToken(); token.getType() != Token.EOF; token = tokenizer.nextToken()) {
 					if (token.getType() == Token.TAG) {
 						HtmlTag tag = (HtmlTag) token.getValue();
 						if (tag.getTagType() == Tag.DIV) {
 							String id = tag.getAttribute("id"); //$NON-NLS-1$
 							inFooter = !tag.isEndTag() && "footer".equals(id); //$NON-NLS-1$
 						} else if (tag.getTagType() == Tag.STRONG && inFooter) {
 							version = getText(tokenizer);
 						} else if (tag.getTagType() == Tag.A) {
 							String id = tag.getAttribute("id"); //$NON-NLS-1$
 							if ("tracpowered".equals(id)) { //$NON-NLS-1$
 								valid = true;
 							}
 						}
 					}
 				}
 
 				if (version != null) {
 					if (version.startsWith("Trac 0.9")) { //$NON-NLS-1$
 						return new TracRepositoryInfo(0, 0, 0, version);
 					} else if (version.startsWith("Trac 0.10")) { //$NON-NLS-1$
 						return new TracRepositoryInfo(0, 0, 1, version);
 					} else if (version.startsWith("Trac 0.11")) { //$NON-NLS-1$
 						return new TracRepositoryInfo(1, 0, 0, version);
 					} else {
						throw new TracException("The Trac version " + version //$NON-NLS-1$
								+ " is unsupported. Please use version 0.9, 0.10. or 0.11"); //$NON-NLS-1$
 					}
 				}
 
 				if (!valid) {
 					throw new TracException("Not a valid Trac repository"); //$NON-NLS-1$
 				}
 
 				return new TracRepositoryInfo(version);
 			} finally {
 				in.close();
 			}
 		} catch (IOException e) {
 			throw new TracException(e);
 		} catch (ParseException e) {
 			throw new TracException(e);
 		} finally {
 			method.releaseConnection();
 		}
 	}
 
 	@Override
 	public void updateAttributes(IProgressMonitor monitor) throws TracException {
 		monitor.beginTask(Messages.TracWebClient_Updating_attributes, IProgressMonitor.UNKNOWN);
 
 		GetMethod method = connect(repositoryUrl + ITracClient.CUSTOM_QUERY_URL, monitor);
 		try {
 			InputStream in = WebUtil.getResponseBodyAsStream(method, monitor);
 			try {
 				BufferedReader reader = new BufferedReader(new InputStreamReader(in, method.getResponseCharSet()));
 				HtmlStreamTokenizer tokenizer = new HtmlStreamTokenizer(reader, null);
 				for (Token token = tokenizer.nextToken(); token.getType() != Token.EOF; token = tokenizer.nextToken()) {
 					if (monitor.isCanceled()) {
 						throw new OperationCanceledException();
 					}
 
 					if (token.getType() == Token.TAG) {
 						HtmlTag tag = (HtmlTag) token.getValue();
 						if (tag.getTagType() == Tag.SCRIPT) {
 							String text = getText(tokenizer).trim();
 							if (text.startsWith("var properties=")) { //$NON-NLS-1$
 								parseAttributes(text);
 							}
 						}
 					}
 				}
 
 				addResolutionAndStatus();
 			} finally {
 				in.close();
 			}
 		} catch (IOException e) {
 			throw new TracException(e);
 		} catch (ParseException e) {
 			throw new TracException(e);
 		} finally {
 			method.releaseConnection();
 		}
 	}
 
 	enum AttributeState {
 		INIT, IN_LIST, IN_ATTRIBUTE_KEY, IN_ATTRIBUTE_VALUE, IN_ATTRIBUTE_VALUE_LIST
 	};
 
 	/**
 	 * Parses the JavaScript code from the query page to extract repository configuration.
 	 */
 	private void parseAttributes(String text) throws IOException {
 		StreamTokenizer t = new StreamTokenizer(new StringReader(text));
 		t.quoteChar('"');
 
 		AttributeFactory attributeFactory = null;
 		String attributeType = null;
 
 		AttributeState state = AttributeState.INIT;
 		int tokenType;
 		while ((tokenType = t.nextToken()) != StreamTokenizer.TT_EOF) {
 			switch (tokenType) {
 			case StreamTokenizer.TT_WORD:
 				if (state == AttributeState.IN_LIST) {
 					if ("component".equals(t.sval)) { //$NON-NLS-1$
 						data.components = new ArrayList<TracComponent>();
 						attributeFactory = new AttributeFactory() {
 							public void addAttribute(String value) {
 								data.components.add(new TracComponent(value));
 							}
 						};
 					} else if ("milestone".equals(t.sval)) { //$NON-NLS-1$
 						data.milestones = new ArrayList<TracMilestone>();
 						attributeFactory = new AttributeFactory() {
 							public void addAttribute(String value) {
 								data.milestones.add(new TracMilestone(value));
 							}
 						};
 					} else if ("priority".equals(t.sval)) { //$NON-NLS-1$
 						data.priorities = new ArrayList<TracPriority>();
 						attributeFactory = new AttributeFactory() {
 							public void addAttribute(String value) {
 								data.priorities.add(new TracPriority(value, data.priorities.size() + 1));
 							}
 						};
 					} else if ("resolution".equals(t.sval)) { //$NON-NLS-1$
 						data.ticketResolutions = new ArrayList<TracTicketResolution>();
 						attributeFactory = new AttributeFactory() {
 							public void addAttribute(String value) {
 								data.ticketResolutions.add(new TracTicketResolution(value,
 										data.ticketResolutions.size() + 1));
 							}
 						};
 					} else if ("severity".equals(t.sval)) { //$NON-NLS-1$
 						data.severities = new ArrayList<TracSeverity>();
 						attributeFactory = new AttributeFactory() {
 							public void addAttribute(String value) {
 								data.severities.add(new TracSeverity(value, data.severities.size() + 1));
 							}
 						};
 					} else if ("status".equals(t.sval)) { //$NON-NLS-1$
 						data.ticketStatus = new ArrayList<TracTicketStatus>();
 						attributeFactory = new AttributeFactory() {
 							public void addAttribute(String value) {
 								data.ticketStatus.add(new TracTicketStatus(value, data.ticketStatus.size() + 1));
 							}
 						};
 					} else if ("type".equals(t.sval)) { //$NON-NLS-1$
 						data.ticketTypes = new ArrayList<TracTicketType>();
 						attributeFactory = new AttributeFactory() {
 							public void addAttribute(String value) {
 								data.ticketTypes.add(new TracTicketType(value, data.ticketTypes.size() + 1));
 							}
 						};
 					} else if ("version".equals(t.sval)) { //$NON-NLS-1$
 						data.versions = new ArrayList<TracVersion>();
 						attributeFactory = new AttributeFactory() {
 							public void addAttribute(String value) {
 								data.versions.add(new TracVersion(value));
 							}
 						};
 					} else {
 						attributeFactory = null;
 					}
 				} else if (state == AttributeState.IN_ATTRIBUTE_KEY) {
 					attributeType = t.sval;
 				}
 				break;
 			case '"':
 				if (state == AttributeState.IN_ATTRIBUTE_VALUE_LIST && "options".equals(attributeType)) { //$NON-NLS-1$
 					if (attributeFactory != null) {
 						attributeFactory.addAttribute(t.sval);
 					}
 				}
 				break;
 			case ':':
 				if (state == AttributeState.IN_ATTRIBUTE_KEY) {
 					state = AttributeState.IN_ATTRIBUTE_VALUE;
 				}
 				break;
 			case ',':
 				if (state == AttributeState.IN_ATTRIBUTE_VALUE) {
 					state = AttributeState.IN_ATTRIBUTE_KEY;
 				}
 				break;
 			case '[':
 				if (state == AttributeState.IN_ATTRIBUTE_VALUE) {
 					state = AttributeState.IN_ATTRIBUTE_VALUE_LIST;
 				}
 				break;
 			case ']':
 				if (state == AttributeState.IN_ATTRIBUTE_VALUE_LIST) {
 					state = AttributeState.IN_ATTRIBUTE_VALUE;
 				}
 				break;
 			case '{':
 				if (state == AttributeState.INIT) {
 					state = AttributeState.IN_LIST;
 				} else if (state == AttributeState.IN_LIST) {
 					state = AttributeState.IN_ATTRIBUTE_KEY;
 				} else {
 					throw new IOException("Error parsing attributes: unexpected token '{'"); //$NON-NLS-1$
 				}
 				break;
 			case '}':
 				if (state == AttributeState.IN_ATTRIBUTE_KEY || state == AttributeState.IN_ATTRIBUTE_VALUE) {
 					state = AttributeState.IN_LIST;
 				} else if (state == AttributeState.IN_LIST) {
 					state = AttributeState.INIT;
 				} else {
 					throw new IOException("Error parsing attributes: unexpected token '}'"); //$NON-NLS-1$
 				}
 				break;
 			}
 		}
 	}
 
 	public void updateAttributesNewTicketPage(IProgressMonitor monitor) throws TracException {
 		monitor.beginTask(Messages.TracWebClient_Updating_attributes, IProgressMonitor.UNKNOWN);
 
 		GetMethod method = connect(repositoryUrl + ITracClient.NEW_TICKET_URL, monitor);
 		try {
 			InputStream in = WebUtil.getResponseBodyAsStream(method, monitor);
 			try {
 				BufferedReader reader = new BufferedReader(new InputStreamReader(in, method.getResponseCharSet()));
 				HtmlStreamTokenizer tokenizer = new HtmlStreamTokenizer(reader, null);
 				for (Token token = tokenizer.nextToken(); token.getType() != Token.EOF; token = tokenizer.nextToken()) {
 					if (monitor.isCanceled()) {
 						throw new OperationCanceledException();
 					}
 
 					if (token.getType() == Token.TAG) {
 						HtmlTag tag = (HtmlTag) token.getValue();
 						if (tag.getTagType() == Tag.SELECT) {
 							String name = tag.getAttribute("id"); //$NON-NLS-1$
 							if ("component".equals(name)) { //$NON-NLS-1$
 								List<String> values = getOptionValues(tokenizer);
 								data.components = new ArrayList<TracComponent>(values.size());
 								for (String value : values) {
 									data.components.add(new TracComponent(value));
 								}
 							} else if ("milestone".equals(name)) { //$NON-NLS-1$
 								List<String> values = getOptionValues(tokenizer);
 								data.milestones = new ArrayList<TracMilestone>(values.size());
 								for (String value : values) {
 									data.milestones.add(new TracMilestone(value));
 								}
 							} else if ("priority".equals(name)) { //$NON-NLS-1$
 								List<String> values = getOptionValues(tokenizer);
 								data.priorities = new ArrayList<TracPriority>(values.size());
 								for (int i = 0; i < values.size(); i++) {
 									data.priorities.add(new TracPriority(values.get(i), i + 1));
 								}
 							} else if ("severity".equals(name)) { //$NON-NLS-1$
 								List<String> values = getOptionValues(tokenizer);
 								data.severities = new ArrayList<TracSeverity>(values.size());
 								for (int i = 0; i < values.size(); i++) {
 									data.severities.add(new TracSeverity(values.get(i), i + 1));
 								}
 							} else if ("type".equals(name)) { //$NON-NLS-1$
 								List<String> values = getOptionValues(tokenizer);
 								data.ticketTypes = new ArrayList<TracTicketType>(values.size());
 								for (int i = 0; i < values.size(); i++) {
 									data.ticketTypes.add(new TracTicketType(values.get(i), i + 1));
 								}
 							} else if ("version".equals(name)) { //$NON-NLS-1$
 								List<String> values = getOptionValues(tokenizer);
 								data.versions = new ArrayList<TracVersion>(values.size());
 								for (String value : values) {
 									data.versions.add(new TracVersion(value));
 								}
 							}
 						}
 					}
 				}
 
 				addResolutionAndStatus();
 			} finally {
 				in.close();
 			}
 		} catch (IOException e) {
 			throw new TracException(e);
 		} catch (ParseException e) {
 			throw new TracException(e);
 		} finally {
 			method.releaseConnection();
 		}
 	}
 
 	private void addResolutionAndStatus() {
 		if (data.ticketResolutions == null || data.ticketResolutions.isEmpty()) {
 			data.ticketResolutions = new ArrayList<TracTicketResolution>(5);
 			data.ticketResolutions.add(new TracTicketResolution("fixed", 1)); //$NON-NLS-1$
 			data.ticketResolutions.add(new TracTicketResolution("invalid", 2)); //$NON-NLS-1$
 			data.ticketResolutions.add(new TracTicketResolution("wontfix", 3)); //$NON-NLS-1$
 			data.ticketResolutions.add(new TracTicketResolution("duplicate", 4)); //$NON-NLS-1$
 			data.ticketResolutions.add(new TracTicketResolution("worksforme", 5)); //$NON-NLS-1$
 		}
 
 		if (data.ticketStatus == null || data.ticketStatus.isEmpty()) {
 			data.ticketStatus = new ArrayList<TracTicketStatus>(4);
 			data.ticketStatus.add(new TracTicketStatus("new", 1)); //$NON-NLS-1$
 			data.ticketStatus.add(new TracTicketStatus("assigned", 2)); //$NON-NLS-1$
 			data.ticketStatus.add(new TracTicketStatus("reopened", 3)); //$NON-NLS-1$
 			data.ticketStatus.add(new TracTicketStatus("closed", 4)); //$NON-NLS-1$
 		}
 	}
 
 	private List<String> getOptionValues(HtmlStreamTokenizer tokenizer) throws IOException, ParseException {
 		List<String> values = new ArrayList<String>();
 		for (Token token = tokenizer.nextToken(); token.getType() != Token.EOF; token = tokenizer.nextToken()) {
 			if (token.getType() == Token.TAG) {
 				HtmlTag tag = (HtmlTag) token.getValue();
 				if (tag.getTagType() == Tag.OPTION && !tag.isEndTag()) {
 					String value = getText(tokenizer).trim();
 					if (value.length() > 0) {
 						values.add(value);
 					}
 				} else {
 					return values;
 				}
 			}
 		}
 		return values;
 	}
 
 	private String getText(HtmlStreamTokenizer tokenizer) throws IOException, ParseException {
 		StringBuilder sb = new StringBuilder();
 		for (Token token = tokenizer.nextToken(); token.getType() != Token.EOF; token = tokenizer.nextToken()) {
 			if (token.getType() == Token.TEXT) {
 				sb.append(token.toString().trim());
 				sb.append(" "); //$NON-NLS-1$
 			} else if (token.getType() == Token.COMMENT) {
 				// ignore
 			} else {
 				break;
 			}
 		}
 		return StringEscapeUtils.unescapeHtml(sb.toString().trim());
 	}
 
 	/**
 	 * Looks for a <code>strong</code> tag and returns the text enclosed by the tag.
 	 */
 	private String getStrongText(HtmlStreamTokenizer tokenizer) throws IOException, ParseException {
 		for (Token token = tokenizer.nextToken(); token.getType() != Token.EOF; token = tokenizer.nextToken()) {
 			if (token.getType() == Token.TAG && ((HtmlTag) token.getValue()).getTagType() == Tag.STRONG) {
 				return getText(tokenizer);
 			} else if (token.getType() == Token.COMMENT) {
 				// ignore
 			} else if (token.getType() == Token.TEXT) {
 				// ignore
 			} else {
 				break;
 			}
 		}
 		return ""; //$NON-NLS-1$
 	}
 
 	public InputStream getAttachmentData(int id, String filename, IProgressMonitor monitor) throws TracException {
 		GetMethod method = connect(repositoryUrl + ITracClient.ATTACHMENT_URL + id + "/" + filename + "?format=raw", //$NON-NLS-1$ //$NON-NLS-2$
 				monitor);
 		try {
 			// the receiver is responsible for closing the stream which will
 			// release the connection
 			return method.getResponseBodyAsStream();
 		} catch (IOException e) {
 			method.releaseConnection();
 			throw new TracException(e);
 		}
 	}
 
 	public void putAttachmentData(int id, String name, String description, InputStream in, IProgressMonitor monitor)
 			throws TracException {
 		throw new TracException("Unsupported operation"); //$NON-NLS-1$
 	}
 
 	public void deleteAttachment(int ticketId, String filename, IProgressMonitor monitor) throws TracException {
 		throw new TracException("Unsupported operation"); //$NON-NLS-1$
 	}
 
 	public int createTicket(TracTicket ticket, IProgressMonitor monitor) throws TracException {
 		throw new TracException("Unsupported operation"); //$NON-NLS-1$
 	}
 
 	public void updateTicket(TracTicket ticket, String comment, IProgressMonitor monitor) throws TracException {
 		throw new TracException("Unsupported operation"); //$NON-NLS-1$
 	}
 
 	public Set<Integer> getChangedTickets(Date since, IProgressMonitor monitor) throws TracException {
 		return null;
 	}
 
 	private interface AttributeFactory {
 
 		void addAttribute(String value);
 
 	}
 
 	public Date getTicketLastChanged(Integer id, IProgressMonitor monitor) {
 		throw new UnsupportedOperationException();
 	}
 
 	public void deleteTicket(int ticketId, IProgressMonitor monitor) throws TracException {
 		throw new UnsupportedOperationException();
 	}
 
 }
