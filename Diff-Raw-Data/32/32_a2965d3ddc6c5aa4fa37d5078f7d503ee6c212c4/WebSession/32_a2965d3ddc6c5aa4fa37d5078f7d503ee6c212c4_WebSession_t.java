 /*
  * Copyright (C) 2009 Emweb bvba, Leuven, Belgium.
  *
  * See the LICENSE file for terms of use.
  */
 package eu.webtoolkit.jwt;
 
 import java.util.*;
 import java.util.regex.*;
 import java.io.*;
 import java.lang.ref.*;
 import java.util.concurrent.locks.ReentrantLock;
 import javax.servlet.http.*;
 import javax.servlet.*;
 import eu.webtoolkit.jwt.*;
 import eu.webtoolkit.jwt.chart.*;
 import eu.webtoolkit.jwt.utils.*;
 import eu.webtoolkit.jwt.servlet.*;
 
 class WebSession {
 	enum State {
 		JustCreated, Loaded, Dead;
 
 		/**
 		 * Returns the numerical representation of this enum.
 		 */
 		public int getValue() {
 			return ordinal();
 		}
 	}
 
 	public WebSession(WtServlet controller, String sessionId,
 			EntryPointType type, String favicon, WebRequest request,
 			WEnvironment env) {
 		this.mutex_ = new ReentrantLock();
 		this.type_ = type;
 		this.favicon_ = favicon;
 		this.state_ = WebSession.State.JustCreated;
 		this.sessionId_ = sessionId;
 		this.sessionIdCookie_ = MathUtils.randomId();
 		this.sessionIdChanged_ = false;
 		this.sessionIdCookieChanged_ = true;
 		this.sessionIdInUrl_ = false;
 		this.controller_ = controller;
 		this.renderer_ = new WebRenderer(this);
 		this.applicationName_ = "";
 		this.bookmarkUrl_ = "";
 		this.basePath_ = "";
 		this.absoluteBaseUrl_ = "";
 		this.applicationUrl_ = "";
 		this.deploymentPath_ = "";
 		this.redirect_ = "";
 		this.pagePathInfo_ = "";
 		this.asyncResponse_ = null;
 		this.bootStyleResponse_ = null;
 		this.canWriteAsyncResponse_ = false;
 		this.noBootStyleResponse_ = false;
 		this.progressiveBoot_ = false;
 		this.recursiveEvent_ = this.mutex_.newCondition();
 		this.newRecursiveEvent_ = false;
 		this.updatesPendingEvent_ = this.mutex_.newCondition();
 		this.updatesPending_ = false;
 		this.embeddedEnv_ = new WEnvironment(this);
 		this.app_ = null;
 		this.debug_ = this.controller_.getConfiguration().getErrorReporting() != Configuration.ErrorReporting.ErrorMessage;
 		this.handlers_ = new ArrayList<WebSession.Handler>();
 		this.emitStack_ = new ArrayList<WObject>();
 		this.recursiveEventLoop_ = null;
 		this.env_ = env != null ? env : this.embeddedEnv_;
 		if (request != null) {
 			this.applicationUrl_ = request.getScriptName();
 		} else {
 			this.applicationUrl_ = "/";
 		}
 		this.deploymentPath_ = this.applicationUrl_;
 		int slashpos = this.deploymentPath_.lastIndexOf('/');
 		if (slashpos != -1) {
 			this.basePath_ = this.deploymentPath_
 					.substring(0, 0 + slashpos + 1);
 			this.applicationName_ = this.deploymentPath_
 					.substring(slashpos + 1);
 		} else {
 			this.basePath_ = "";
 			this.applicationName_ = this.applicationUrl_;
 		}
 		this.log("notice").append("Session created");
 		this.getRenderer().setCookie("Wt" + this.sessionId_,
 				this.sessionIdCookie_, -1, "", "");
 	}
 
 	public WebSession(WtServlet controller, String sessionId,
 			EntryPointType type, String favicon, WebRequest request) {
 		this(controller, sessionId, type, favicon, request, (WEnvironment) null);
 	}
 
 	public static WebSession getInstance() {
 		WebSession.Handler handler = WebSession.Handler.getInstance();
 		return handler != null ? handler.getSession() : null;
 	}
 
 	public EntryPointType getType() {
 		return this.type_;
 	}
 
 	public String getFavicon() {
 		return this.favicon_;
 	}
 
 	public String getDocType() {
 		final boolean xhtml = this.env_.getContentType() == WEnvironment.ContentType.XHTML1;
 		if (xhtml) {
 			return "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">";
 		} else {
 			return "<!doctype html>";
 		}
 	}
 
 	public String getSessionId() {
 		return this.sessionId_;
 	}
 
 	public WtServlet getController() {
 		return this.controller_;
 	}
 
 	public WEnvironment getEnv() {
 		return this.env_;
 	}
 
 	public WApplication getApp() {
 		return this.app_;
 	}
 
 	public WebRenderer getRenderer() {
 		return this.renderer_;
 	}
 
 	public boolean isDebug() {
 		return this.debug_;
 	}
 
 	public void redirect(String url) {
 		this.redirect_ = url;
 		if (this.redirect_.length() == 0) {
 			this.redirect_ = "?";
 		}
 	}
 
 	public String getRedirect() {
 		String result = this.redirect_;
 		this.redirect_ = "";
 		return result;
 	}
 
 	public void setApplication(WApplication app) {
 		this.app_ = app;
 	}
 
 	public WLogEntry log(String type) {
 		Configuration conf = this.controller_.getConfiguration();
 		WLogEntry e = conf.getLogger().getEntry();
 		return e;
 	}
 
 	public void notify(WEvent event) throws IOException {
 		WebSession.Handler handler = event.impl_.handler;
 		WebRequest request = handler.getRequest();
 		WebResponse response = handler.getResponse();
 		if (WebSession.Handler.getInstance() != handler) {
 			WebSession.Handler.getInstance().setRequest(request, response);
 		}
 		if (event.impl_.renderOnly) {
 			this.render(handler);
 			return;
 		}
 		String requestE = request.getParameter("request");
 		String pageIdE = request.getParameter("pageId");
 		if (pageIdE != null
 				&& !pageIdE.equals(String.valueOf(this.renderer_.getPageId()))) {
 			handler.getResponse().setContentType(
 					"text/javascript; charset=UTF-8");
 			handler.getResponse().out().append("{}");
 			handler.getResponse().flush();
 			handler.setRequest((WebRequest) null, (WebResponse) null);
 			return;
 		}
 		if (!this.app_.initialized_) {
 			this.app_.initialize();
 			this.app_.initialized_ = true;
 		}
 		switch (this.state_) {
 		case JustCreated:
 			this.render(handler);
 			break;
 		case Loaded:
 			if (handler.getResponse().getResponseType() == WebRequest.ResponseType.Page) {
 				if (!handler.getRequest().getHeaderValue("User-Agent").equals(
 						this.env_.getUserAgent())) {
 					this.log("secure").append(
 							"Change of user-agent not allowed.");
 					this.serveError(403, handler, "Forbidden");
 					return;
 				}
 				String ca = WEnvironment.getClientAddress(handler.getRequest(),
 						this.controller_.getConfiguration());
 				if (!ca.equals(this.env_.getClientAddress())) {
 					boolean isInvalid = this.sessionIdCookie_.length() == 0;
 					if (!isInvalid) {
 						String cookie = request.getHeaderValue("Cookie");
 						if (cookie.indexOf(this.sessionIdCookie_) == -1) {
 							isInvalid = true;
 						}
 					}
 					if (isInvalid) {
 						this.log("secure").append("Change of IP address (")
 								.append(this.env_.getClientAddress()).append(
 										" -> ").append(ca).append(
 										") not allowed.");
 						this.serveError(403, handler, "Forbidden");
 						return;
 					}
 				}
 			}
 			if (this.sessionIdCookieChanged_) {
 				String cookie = request.getHeaderValue("Cookie");
 				if (cookie.indexOf(this.sessionIdCookie_) == -1) {
 					this.sessionIdCookie_ = "";
 					this.log("info").append("Session id cookie not working");
 				}
 				this.sessionIdCookieChanged_ = false;
 			}
 			if (handler.getResponse().getResponseType() == WebRequest.ResponseType.Script) {
 				String sidE = request.getParameter("sid");
 				if (!(sidE != null)
 						|| !sidE.equals(String.valueOf(this.renderer_
 								.getScriptId()))) {
 					throw new WtException("Script id mismatch");
 				}
 				if (!(request.getParameter("skeleton") != null)) {
 					if (!this.env_.hasAjax()) {
 						this.env_.enableAjax(request);
 						this.app_.enableAjax();
 						if (this.env_.getInternalPath().length() > 1) {
 							this.app_.changedInternalPath(this.env_
 									.getInternalPath());
 						}
 					} else {
 						String hashE = request.getParameter("_");
 						if (hashE != null) {
 							this.app_.changedInternalPath(hashE);
 						}
 					}
 				}
 				this.render(handler);
 			} else {
 				try {
 					if (0 != 0) {
 						this.app_.requestTooLarge().trigger(0);
 					}
 				} catch (RuntimeException e) {
 					this.log("error").append(
 							"Exception in WApplication::requestTooLarge")
 							.append(e.toString());
 					throw e;
 				}
 				WResource resource = null;
 				if (!(requestE != null) && request.getPathInfo().length() != 0) {
 					resource = this.app_.decodeExposedResource("/path/"
 							+ request.getPathInfo());
 				}
 				String resourceE = request.getParameter("resource");
 				String signalE = this.getSignal(request, "");
 				if (signalE != null) {
 					this.progressiveBoot_ = false;
 				}
 				if (resource != null || requestE != null
 						&& requestE.equals("resource") && resourceE != null) {
 					if (resourceE != null && resourceE.equals("blank")) {
 						handler.getResponse().setContentType("text/html");
 						handler
 								.getResponse()
 								.out()
 								.append(
 										"<html><head><title>bhm</title></head><body>&#160;</body></html>");
 						handler.getResponse().flush();
 						handler.setRequest((WebRequest) null,
 								(WebResponse) null);
 					} else {
 						if (!(resource != null)) {
 							resource = this.app_
 									.decodeExposedResource(resourceE);
 						}
 						if (resource != null) {
 							try {
 								resource.handle(request, response);
 								handler.setRequest((WebRequest) null,
 										(WebResponse) null);
 							} catch (RuntimeException e) {
 								this.log("error").append(
 										"Exception while streaming resource")
 										.append(e.toString());
 								throw e;
 							}
 						} else {
 							this.log("error").append(
 									"decodeResource(): resource '").append(
 									resourceE).append("' not exposed");
 							handler.getResponse().setContentType("text/html");
 							handler
 									.getResponse()
 									.out()
 									.append(
 											"<html><body><h1>Nothing to say about that.</h1></body></html>");
 							handler.getResponse().flush();
 							handler.setRequest((WebRequest) null,
 									(WebResponse) null);
 						}
 					}
 				} else {
 					this.env_.urlScheme_ = request.getScheme();
 					if (signalE != null) {
 						String ackIdE = request.getParameter("ackId");
 						boolean invalidAckId = this.env_.hasAjax()
 								&& !request.isWebSocketMessage();
 						if (ackIdE != null) {
 							try {
 								if (this.renderer_.ackUpdate(Integer
 										.parseInt(ackIdE))) {
 									invalidAckId = false;
 								}
 							} catch (NumberFormatException e) {
 							}
 						}
 						if (invalidAckId) {
							if (!(ackIdE != null)) {
								this.log("secure").append("Missing ackId");
							} else {
								this.log("secure").append("Invalid ackId");
							}
 							this.serveError(403, handler, "Forbidden");
 							return;
 						}
 						if (this.asyncResponse_ != null
 								&& !this.asyncResponse_.isWebSocketRequest()) {
 							this.asyncResponse_ = null;
 							this.canWriteAsyncResponse_ = false;
 						}
 						if (signalE.equals("poll")) {
 							if (!WtServlet.isAsyncSupported()) {
 								this.updatesPendingEvent_.signal();
 								if (!this.updatesPending_) {
 									try {
 										this.updatesPendingEvent_
 												.await(
 														this.controller_
 																.getConfiguration()
 																.getServerPushTimeout() * 2,
 														java.util.concurrent.TimeUnit.SECONDS);
 									} catch (InterruptedException e) {
 									}
 								}
 								if (!this.updatesPending_) {
 									return;
 								}
 							}
 							if (!this.updatesPending_
 									&& !(this.asyncResponse_ != null)) {
 								this.asyncResponse_ = handler.getResponse();
 								this.canWriteAsyncResponse_ = true;
 								handler.setRequest((WebRequest) null,
 										(WebResponse) null);
 							}
 						}
 						if (handler.getRequest() != null) {
 							try {
 								handler.nextSignal = -1;
 								this.notifySignal(event);
 							} catch (RuntimeException e) {
 								this.log("error").append(
 										"Error during event handling: ")
 										.append(e.toString());
 								throw e;
 							}
 						}
 					}
 					if (handler.getResponse() != null
 							&& handler.getResponse().getResponseType() == WebRequest.ResponseType.Page
 							&& !this.env_.hasAjax()) {
 						String hashE = request.getParameter("_");
 						if (hashE != null) {
 							this.app_.changedInternalPath(hashE);
 						} else {
 							if (request.getPathInfo().length() != 0) {
 								this.app_.changedInternalPath(request
 										.getPathInfo());
 							} else {
 								this.app_.changedInternalPath("");
 							}
 						}
 					}
 					if (!(signalE != null)) {
 						this.log("notice").append("Refreshing session");
 						this.flushBootStyleResponse();
 						this.env_.parameters_ = handler.getRequest()
 								.getParameterMap();
 						this.app_.refresh();
 					}
 					if (handler.getResponse() != null
 							&& !(this.recursiveEventLoop_ != null)) {
 						this.render(handler);
 					}
 				}
 			}
 		case Dead:
 			break;
 		}
 	}
 
 	public void pushUpdates() {
 		try {
 			if (!this.renderer_.isDirty()
 					|| this.state_ == WebSession.State.Dead) {
 				return;
 			}
 			this.updatesPending_ = true;
 			if (this.canWriteAsyncResponse_) {
 				if (this.asyncResponse_.isWebSocketRequest()
 						&& this.asyncResponse_.isWebSocketMessagePending()) {
 					return;
 				}
 				if (this.asyncResponse_.isWebSocketRequest()) {
 				} else {
 					this.asyncResponse_
 							.setResponseType(WebRequest.ResponseType.Update);
 					this.renderer_.serveResponse(this.asyncResponse_);
 				}
 				this.updatesPending_ = false;
 				if (!this.asyncResponse_.isWebSocketRequest()) {
 					this.asyncResponse_.flush();
 					this.asyncResponse_ = null;
 					this.canWriteAsyncResponse_ = false;
 				}
 			} else {
 				this.updatesPendingEvent_.signal();
 			}
 		} catch (IOException ioe) {
 			ioe.printStackTrace();
 		}
 	}
 
 	public void doRecursiveEventLoop() {
 		try {
 			WebSession.Handler handler = WebSession.Handler.getInstance();
 			if (handler.getRequest() != null && !WtServlet.isAsyncSupported()) {
 				throw new WtException(
 						"Recursive eventloop requires a Servlet 3.0 enabled servlet container and an application with async-supported enabled.");
 			}
 			if (handler.getRequest() != null) {
 				handler.getSession().notifySignal(
 						new WEvent(new WEvent.Impl(handler)));
 			} else {
 				this.app_.triggerUpdate();
 			}
 			if (handler.getResponse() != null) {
 				handler.getSession().render(handler);
 			}
 			if (this.state_ == WebSession.State.Dead) {
 				this.recursiveEventLoop_ = null;
 				throw new WtException(
 						"doRecursiveEventLoop(): session was killed");
 			}
 			this.recursiveEventLoop_ = handler;
 			this.newRecursiveEvent_ = false;
 			while (!this.newRecursiveEvent_) {
 				this.recursiveEvent_.awaitUninterruptibly();
 			}
 			if (this.state_ == WebSession.State.Dead) {
 				this.recursiveEventLoop_ = null;
 				throw new WtException(
 						"doRecursiveEventLoop(): session was killed");
 			}
 			this.app_.notify(new WEvent(new WEvent.Impl(handler)));
 			this.recursiveEventLoop_ = null;
 		} catch (IOException ioe) {
 			ioe.printStackTrace();
 		}
 	}
 
 	public boolean isBootStyleResponse() {
 		return !this.noBootStyleResponse_;
 	}
 
 	public void expire() {
 		this.kill();
 	}
 
 	public boolean isUnlockRecursiveEventLoop() {
 		if (!(this.recursiveEventLoop_ != null)) {
 			return false;
 		}
 		WebSession.Handler handler = WebSession.Handler.getInstance();
 		this.recursiveEventLoop_.setRequest(handler.getRequest(), handler
 				.getResponse());
 		handler.setRequest((WebRequest) null, (WebResponse) null);
 		this.newRecursiveEvent_ = true;
 		this.recursiveEvent_.signal();
 		return true;
 	}
 
 	public void pushEmitStack(WObject o) {
 		this.emitStack_.add(o);
 	}
 
 	public void popEmitStack() {
 		this.emitStack_.remove(this.emitStack_.size() - 1);
 	}
 
 	public WObject getEmitStackTop() {
 		if (!this.emitStack_.isEmpty()) {
 			return this.emitStack_.get(this.emitStack_.size() - 1);
 		} else {
 			return null;
 		}
 	}
 
 	public boolean isDead() {
 		return this.state_ == WebSession.State.Dead;
 	}
 
 	public WebSession.State getState() {
 		return this.state_;
 	}
 
 	public void kill() {
 		this.state_ = WebSession.State.Dead;
 		this.isUnlockRecursiveEventLoop();
 	}
 
 	public boolean isProgressiveBoot() {
 		return this.progressiveBoot_;
 	}
 
 	public String getApplicationName() {
 		return this.applicationName_;
 	}
 
 	public String getApplicationUrl() {
 		return this.applicationUrl_ + this.getSessionQuery();
 	}
 
 	public String getDeploymentPath() {
 		return this.deploymentPath_;
 	}
 
 	public boolean hasSessionIdInUrl() {
 		return this.sessionIdInUrl_;
 	}
 
 	public boolean isUseUglyInternalPaths() {
 		return false;
 	}
 
 	public void setPagePathInfo(String path) {
 		this.pagePathInfo_ = path;
 	}
 
 	public String getPagePathInfo() {
 		return this.pagePathInfo_;
 	}
 
 	public String getMostRelativeUrl(String internalPath) {
 		return this.appendSessionQuery(this.getBookmarkUrl(internalPath));
 	}
 
 	public final String getMostRelativeUrl() {
 		return getMostRelativeUrl("");
 	}
 
 	public String appendInternalPath(String baseUrl, String internalPath) {
 		if (internalPath.length() == 0 || internalPath.equals("/")) {
 			if (baseUrl.length() == 0) {
 				return "?";
 			} else {
 				return baseUrl;
 			}
 		} else {
 			if (this.isUseUglyInternalPaths()) {
 				return baseUrl + "?_=" + DomElement.urlEncodeS(internalPath);
 			} else {
 				if (this.applicationName_.length() == 0) {
 					return baseUrl
 							+ DomElement.urlEncodeS(internalPath.substring(1));
 				} else {
 					return baseUrl + DomElement.urlEncodeS(internalPath);
 				}
 			}
 		}
 	}
 
 	public String appendSessionQuery(String url) {
 		String result = url;
 		if (this.env_.agentIsSpiderBot()) {
 			return result;
 		}
 		int questionPos = result.indexOf('?');
 		if (questionPos == -1) {
 			result += this.getSessionQuery();
 		} else {
 			if (questionPos == result.length() - 1) {
 				result += this.getSessionQuery().substring(1);
 			} else {
 				result += '&' + this.getSessionQuery().substring(1);
 			}
 		}
 		if (result.startsWith("?")) {
 			result = this.applicationUrl_ + result;
 		}
 		if (WebSession.Handler.getInstance().getResponse() != null) {
 			return WebSession.Handler.getInstance().getResponse().encodeURL(
 					result);
 		}
 		return url;
 	}
 
 	public String ajaxCanonicalUrl(WebResponse request) {
 		String hashE = null;
 		if (this.applicationName_.length() == 0) {
 			hashE = request.getParameter("_");
 		}
 		if (this.pagePathInfo_.length() != 0 || hashE != null
 				&& hashE.length() > 1) {
 			String url = "";
 			if (this.applicationName_.length() == 0) {
 				url = this.fixRelativeUrl(".");
 				url = url.substring(0, 0 + url.length() - 1);
 			} else {
 				url = this.fixRelativeUrl(this.applicationName_);
 			}
 			boolean firstParameter = true;
 			for (Iterator<Map.Entry<String, String[]>> i_it = request
 					.getParameterMap().entrySet().iterator(); i_it.hasNext();) {
 				Map.Entry<String, String[]> i = i_it.next();
 				if (!i.getKey().equals("_")) {
 					url += (firstParameter ? '?' : '&')
 							+ DomElement.urlEncodeS(i.getKey()) + '='
 							+ DomElement.urlEncodeS(i.getValue()[0]);
 					firstParameter = false;
 				}
 			}
 			url += '#' + (this.app_ != null ? this.app_.getInternalPath()
 					: this.env_.getInternalPath());
 			return url;
 		} else {
 			return "";
 		}
 	}
 
 	enum BootstrapOption {
 		ClearInternalPath, KeepInternalPath;
 
 		/**
 		 * Returns the numerical representation of this enum.
 		 */
 		public int getValue() {
 			return ordinal();
 		}
 	}
 
 	public String getBootstrapUrl(WebResponse response,
 			WebSession.BootstrapOption option) {
 		switch (option) {
 		case KeepInternalPath: {
 			String url = "";
 			String internalPath = this.app_ != null ? this.app_
 					.getInternalPath() : this.env_.getInternalPath();
 			if (this.isUseUglyInternalPaths()) {
 				if (internalPath.length() > 1) {
 					url = "?_=" + DomElement.urlEncodeS(internalPath);
 				}
 				if (isAbsoluteUrl(this.applicationUrl_)) {
 					url = this.applicationUrl_ + url;
 				}
 			} else {
 				if (!isAbsoluteUrl(this.applicationUrl_)) {
 					if (internalPath.length() > 1) {
 						String lastPart = internalPath.substring(internalPath
 								.lastIndexOf('/') + 1);
 						url = "";
 					} else {
 						url = this.applicationName_;
 					}
 				} else {
 					if (this.applicationName_.length() == 0
 							&& internalPath.length() > 1) {
 						internalPath = internalPath.substring(1);
 					}
 					url = this.applicationUrl_ + internalPath;
 				}
 			}
 			return this.appendSessionQuery(url);
 		}
 		case ClearInternalPath: {
 			String url = "";
 			if (this.applicationName_.length() == 0) {
 				url = this.fixRelativeUrl(".");
 				url = url.substring(0, 0 + url.length() - 1);
 			} else {
 				url = this.fixRelativeUrl(this.applicationName_);
 			}
 			return this.appendSessionQuery(url);
 		}
 		default:
 			assert false;
 		}
 		return "";
 	}
 
 	public String fixRelativeUrl(String url) {
 		if (isAbsoluteUrl(url)) {
 			return url;
 		}
 		if (url.length() > 0 && url.charAt(0) == '#') {
 			return url;
 		}
 		if (!isAbsoluteUrl(this.applicationUrl_)) {
 			if (url.length() == 0 || url.charAt(0) == '/') {
 				return url;
 			} else {
 				if (this.env_.hashInternalPaths()) {
 					return url;
 				} else {
 					String rel = "";
 					String pi = this.pagePathInfo_;
 					for (int i = 0; i < pi.length(); ++i) {
 						if (pi.charAt(i) == '/') {
 							rel += "../";
 						}
 					}
 					return rel + url;
 				}
 			}
 		} else {
 			return this.makeAbsoluteUrl(url);
 		}
 	}
 
 	public String makeAbsoluteUrl(String url) {
 		if (isAbsoluteUrl(url)) {
 			return url;
 		} else {
 			if (url.length() == 0 || url.charAt(0) != '/') {
 				return this.absoluteBaseUrl_ + url;
 			} else {
 				return host(this.absoluteBaseUrl_) + url;
 			}
 		}
 	}
 
 	public String getBookmarkUrl(String internalPath) {
 		String result = this.bookmarkUrl_;
 		return this.appendInternalPath(result, internalPath);
 	}
 
 	public String getBookmarkUrl() {
 		if (this.app_ != null) {
 			return this.getBookmarkUrl(this.app_.getInternalPath());
 		} else {
 			return this.getBookmarkUrl(this.env_.getInternalPath());
 		}
 	}
 
 	public String getCgiValue(String varName) {
 		WebRequest request = WebSession.Handler.getInstance().getRequest();
 		if (request != null) {
 			return "";
 		} else {
 			return "";
 		}
 	}
 
 	public String getCgiHeader(String headerName) {
 		WebRequest request = WebSession.Handler.getInstance().getRequest();
 		if (request != null) {
 			return request.getHeaderValue(headerName);
 		} else {
 			return "";
 		}
 	}
 
 	public EventType getEventType(WEvent event) {
 		if (event.impl_.handler == null) {
 			return EventType.OtherEvent;
 		}
 		WebSession.Handler handler = event.impl_.handler;
 		WebRequest request = handler.getRequest();
 		if (event.impl_.renderOnly) {
 			return EventType.OtherEvent;
 		}
 		String requestE = request.getParameter("request");
 		String pageIdE = handler.getRequest().getParameter("pageId");
 		if (pageIdE != null
 				&& !pageIdE.equals(String.valueOf(this.renderer_.getPageId()))) {
 			return EventType.OtherEvent;
 		}
 		switch (this.state_) {
 		case Loaded:
 			if (handler.getResponse().getResponseType() == WebRequest.ResponseType.Script) {
 				return EventType.OtherEvent;
 			} else {
 				WResource resource = null;
 				if (!(requestE != null) && request.getPathInfo().length() != 0) {
 					resource = this.app_.decodeExposedResource("/path/"
 							+ request.getPathInfo());
 				}
 				String resourceE = request.getParameter("resource");
 				String signalE = this.getSignal(request, "");
 				if (resource != null || requestE != null
 						&& requestE.equals("resource") && resourceE != null) {
 					return EventType.OtherEvent;
 				} else {
 					if (signalE != null) {
 						if (signalE.equals("none") || signalE.equals("load")
 								|| signalE.equals("hash")
 								|| signalE.equals("poll")) {
 							return EventType.OtherEvent;
 						} else {
 							List<Integer> signalOrder = this
 									.getSignalProcessingOrder(event);
 							int timerSignals = 0;
 							for (int i = 0; i < signalOrder.size(); ++i) {
 								int signalI = signalOrder.get(i);
 								String se = signalI > 0 ? 'e' + String
 										.valueOf(signalI) : "";
 								String s = this.getSignal(request, se);
 								if (!(s != null)) {
 									break;
 								} else {
 									if (signalE.equals("user")) {
 										return EventType.UserEvent;
 									} else {
 										AbstractEventSignal esb = this
 												.decodeSignal(s);
 										WTimerWidget t = ((esb.getSender()) instanceof WTimerWidget ? (WTimerWidget) (esb
 												.getSender())
 												: null);
 										if (t != null) {
 											++timerSignals;
 										} else {
 											return EventType.UserEvent;
 										}
 									}
 								}
 							}
 							if (timerSignals != 0) {
 								return EventType.TimerEvent;
 							}
 						}
 					} else {
 						return EventType.OtherEvent;
 					}
 				}
 			}
 		default:
 			return EventType.OtherEvent;
 		}
 	}
 
 	public void setState(WebSession.State state, int timeout) {
 		if (this.state_ != WebSession.State.Dead) {
 			this.state_ = state;
 		}
 	}
 
 	static class Handler {
 		public Handler() {
 			this.nextSignal = -1;
 			this.signalOrder = new ArrayList<Integer>();
 			this.prevHandler_ = null;
 			this.session_ = null;
 			this.request_ = null;
 			this.response_ = null;
 			this.init();
 		}
 
 		public Handler(WebSession session, WebRequest request,
 				WebResponse response) {
 			this.nextSignal = -1;
 			this.signalOrder = new ArrayList<Integer>();
 			this.prevHandler_ = null;
 			this.session_ = session;
 			this.request_ = request;
 			this.response_ = response;
 			session.getMutex().lock();
 			this.init();
 		}
 
 		public Handler(WebSession session, boolean takeLock) {
 			this.nextSignal = -1;
 			this.signalOrder = new ArrayList<Integer>();
 			this.prevHandler_ = null;
 			this.session_ = session;
 			this.request_ = null;
 			this.response_ = null;
 			if (takeLock) {
 				session.getMutex().lock();
 			}
 			this.init();
 		}
 
 		public Handler(WebSession session) {
 			this.nextSignal = -1;
 			this.signalOrder = new ArrayList<Integer>();
 			this.prevHandler_ = null;
 			this.session_ = session;
 			this.request_ = null;
 			this.response_ = null;
 			this.init();
 		}
 
 		public void release() {
 			if (this.session_.getMutex().isHeldByCurrentThread()) {
 				this.session_.getMutex().unlock();
 			}
 			attachThreadToHandler(this.prevHandler_);
 		}
 
 		public static WebSession.Handler getInstance() {
 			return threadHandler_.get();
 		}
 
 		public boolean isHaveLock() {
 			return this.session_.getMutex().isHeldByCurrentThread();
 		}
 
 		public WebResponse getResponse() {
 			return this.response_;
 		}
 
 		public WebRequest getRequest() {
 			return this.request_;
 		}
 
 		public WebSession getSession() {
 			return this.session_;
 		}
 
 		public void setRequest(WebRequest request, WebResponse response) {
 			this.request_ = request;
 			this.response_ = response;
 		}
 
 		public int nextSignal;
 		public List<Integer> signalOrder;
 
 		static void attachThreadToSession(WebSession session) {
 			attachThreadToHandler((WebSession.Handler) null);
 			if (!(session != null)) {
 				return;
 			}
 			if (session.state_ == WebSession.State.Dead) {
 				session.log("warn").append("Attaching to dead session?");
 				attachThreadToHandler(new WebSession.Handler(session, false));
 				return;
 			}
 			attachThreadToHandler(new WebSession.Handler(session, false));
 		}
 
 		public static WebSession.Handler attachThreadToHandler(
 				WebSession.Handler handler) {
 			WebSession.Handler result;
 			result = threadHandler_.get();
 			threadHandler_.set(handler);
 			return result;
 		}
 
 		private void init() {
 			this.prevHandler_ = attachThreadToHandler(this);
 		}
 
 		private WebSession.Handler prevHandler_;
 		private WebSession session_;
 		private WebRequest request_;
 		private WebResponse response_;
 		private boolean killed_;
 	}
 
 	public void handleRequest(WebSession.Handler handler) throws IOException {
 		try {
 			WebRequest request = handler.getRequest();
 			String wtdE = request.getParameter("wtd");
 			String origin = request.getHeaderValue("Origin");
 			if (origin.length() != 0) {
 				if (wtdE != null && wtdE.equals(this.sessionId_)
 						|| this.state_ == WebSession.State.JustCreated) {
 					if (origin.equals("null")) {
 						origin = "*";
 					}
 					handler.getResponse().addHeader(
 							"Access-Control-Allow-Origin", origin);
 					handler.getResponse().addHeader(
 							"Access-Control-Allow-Credentials", "true");
 					if (request.getRequestMethod().equals("OPTIONS")) {
 						WebResponse response = handler.getResponse();
 						response.setStatus(200);
 						response.addHeader("Access-Control-Allow-Methods",
 								"POST, OPTIONS");
 						response.addHeader("Access-Control-Max-Age", "1728000");
 						response.flush();
 						return;
 					}
 				} else {
 					if (request.isWebSocketRequest()) {
 						handler.getResponse().flush();
 						return;
 					}
 				}
 			}
 			if (request.isWebSocketRequest()) {
 				if (this.state_ != WebSession.State.JustCreated) {
 					this.handleWebSocketRequest(handler);
 					return;
 				} else {
 					handler.getResponse().flush();
 					this.kill();
 					return;
 				}
 			}
 			Configuration conf = this.controller_.getConfiguration();
 			String requestE = request.getParameter("request");
 			handler.getResponse().setResponseType(WebRequest.ResponseType.Page);
 			if (!(requestE != null && requestE.equals("resource")
 					|| request.getRequestMethod().equals("POST") || request
 					.getRequestMethod().equals("GET"))) {
 				handler.getResponse().setStatus(400);
 				handler.getResponse().flush();
 				return;
 			}
 			if ((!(wtdE != null) || !wtdE.equals(this.sessionId_))
 					&& this.state_ != WebSession.State.JustCreated
 					&& (requestE != null && (requestE.equals("jsupdate") || requestE
 							.equals("resource")))) {
 				this.log("secure").append("CSRF prevention kicked in.");
 				this.serveError(403, handler, "Forbidden");
 			} else {
 				try {
 					switch (this.state_) {
 					case JustCreated: {
 						switch (this.type_) {
 						case Application: {
 							this.init(request);
 							if (requestE != null) {
 								if (requestE.equals("jsupdate")
 										|| requestE.equals("script")) {
 									handler.getResponse().setResponseType(
 											WebRequest.ResponseType.Update);
 									this
 											.log("notice")
 											.append(
 													"Signal from dead session, sending reload.");
 									this.renderer_.letReloadJS(handler
 											.getResponse(), true);
 									this.kill();
 									break;
 								} else {
 									if (!requestE.equals("page")) {
 										this.log("notice").append(
 												"Not serving this.");
 										handler.getResponse().setContentType(
 												"text/html");
 										handler
 												.getResponse()
 												.out()
 												.append(
 														"<html><head></head><body></body></html>");
 										this.kill();
 										break;
 									}
 								}
 							}
 							boolean forcePlain = this.env_.agentIsSpiderBot()
 									|| !this.env_.agentSupportsAjax();
 							this.progressiveBoot_ = !forcePlain
 									&& conf.progressiveBootstrap();
 							if (forcePlain || this.progressiveBoot_) {
 								try {
 									String internalPath = this.env_
 											.getCookie("WtInternalPath");
 									this.env_.setInternalPath(internalPath);
 								} catch (RuntimeException e) {
 								}
 								if (!this.start()) {
 									throw new WtException(
 											"Could not start application.");
 								}
 								this.app_.notify(new WEvent(new WEvent.Impl(
 										handler)));
 								this.setLoaded();
 								if (this.env_.agentIsSpiderBot()) {
 									this.kill();
 								}
 							} else {
 								this.serveResponse(handler);
 								this.setState(WebSession.State.Loaded, 10);
 							}
 							break;
 						}
 						case WidgetSet:
 							if (requestE != null && requestE.equals("resource")) {
 								String resourceE = request
 										.getParameter("resource");
 								if (resourceE != null
 										&& resourceE.equals("blank")) {
 									handler.getResponse().setContentType(
 											"text/html");
 									handler
 											.getResponse()
 											.out()
 											.append(
 													"<html><head><title>bhm</title></head><body>&#160;</body></html>");
 								} else {
 									this
 											.log("notice")
 											.append(
 													"Not starting session for resource.");
 									handler.getResponse().setContentType(
 											"text/html");
 									handler
 											.getResponse()
 											.out()
 											.append(
 													"<html><head></head><body></body></html>");
 								}
 								this.kill();
 							} else {
 								handler.getResponse().setResponseType(
 										WebRequest.ResponseType.Script);
 								this.init(request);
 								this.env_.enableAjax(request);
 								if (!this.start()) {
 									throw new WtException(
 											"Could not start application.");
 								}
 								this.app_.notify(new WEvent(new WEvent.Impl(
 										handler)));
 								this.setLoaded();
 							}
 							break;
 						default:
 							assert false;
 						}
 						break;
 					}
 					case Loaded: {
 						if (requestE != null) {
 							if (requestE.equals("jsupdate")) {
 								handler.getResponse().setResponseType(
 										WebRequest.ResponseType.Update);
 							} else {
 								if (requestE.equals("script")) {
 									handler.getResponse().setResponseType(
 											WebRequest.ResponseType.Script);
 								} else {
 									if (requestE.equals("style")) {
 										this.flushBootStyleResponse();
 										String jsE = request.getParameter("js");
 										boolean nojs = jsE != null
 												&& jsE.equals("no");
 										final boolean xhtml = this.env_
 												.getContentType() == WEnvironment.ContentType.XHTML1;
 										this.noBootStyleResponse_ = this.noBootStyleResponse_
 												|| !(this.app_ != null)
 												&& (xhtml || nojs);
 										if (nojs || this.noBootStyleResponse_) {
 											handler.getResponse()
 													.setContentType("text/css");
 											handler.getResponse().flush();
 											handler.setRequest(
 													(WebRequest) null,
 													(WebResponse) null);
 										} else {
 											int i = 0;
 											final int MAX_TRIES = 1000;
 											while (!(this.app_ != null)
 													&& i < MAX_TRIES) {
 												this.mutex_.unlock();
 												Thread.sleep(5);
 												this.mutex_.lock();
 												++i;
 											}
 											if (i < MAX_TRIES) {
 												this.renderer_
 														.serveLinkedCss(handler
 																.getResponse());
 											}
 											handler.getResponse().flush();
 											handler.setRequest(
 													(WebRequest) null,
 													(WebResponse) null);
 										}
 										break;
 									}
 								}
 							}
 						}
 						boolean requestForResource = requestE != null
 								&& requestE.equals("resource");
 						if (!(this.app_ != null)) {
 							String resourceE = request.getParameter("resource");
 							if (handler.getResponse().getResponseType() == WebRequest.ResponseType.Script) {
 								if (!(request.getParameter("skeleton") != null)) {
 									this.env_.enableAjax(request);
 									if (!this.start()) {
 										throw new WtException(
 												"Could not start application.");
 									}
 								} else {
 									this.serveResponse(handler);
 									return;
 								}
 							} else {
 								if (requestForResource && resourceE != null
 										&& resourceE.equals("blank")) {
 									handler.getResponse().setContentType(
 											"text/html");
 									handler
 											.getResponse()
 											.out()
 											.append(
 													"<html><head><title>bhm</title></head><body>&#160;</body></html>");
 									break;
 								} else {
 									String jsE = request.getParameter("js");
 									if (jsE != null && jsE.equals("no")) {
 										if (!this.start()) {
 											throw new WtException(
 													"Could not start application.");
 										}
 									} else {
 										if (!conf.isReloadIsNewSession()
 												&& wtdE != null
 												&& wtdE.equals(this.sessionId_)) {
 											this.serveResponse(handler);
 											this
 													.setState(
 															WebSession.State.Loaded,
 															10);
 										} else {
 											handler
 													.getResponse()
 													.setContentType("text/html");
 											handler
 													.getResponse()
 													.out()
 													.append(
 															"<html><body><h1>Refusing to respond.</h1></body></html>");
 										}
 										break;
 									}
 								}
 							}
 						}
 						if (!(handler.getRequest() != null)) {
 							break;
 						}
 						String signalE = handler.getRequest().getParameter(
 								"signal");
 						boolean isPoll = signalE != null
 								&& signalE.equals("poll");
 						if (requestForResource || isPoll
 								|| !this.isUnlockRecursiveEventLoop()) {
 							this.setLoaded();
 							this.app_.notify(new WEvent(
 									new WEvent.Impl(handler)));
 							if (handler.getResponse() != null
 									&& !requestForResource) {
 								this.app_.notify(new WEvent(new WEvent.Impl(
 										handler, true)));
 							}
 						}
 						break;
 					}
 					case Dead:
 						throw new WtException(
 								"Internal error: WebSession is dead?");
 					}
 				} catch (WtException e) {
 					this.log("fatal").append(e.toString());
 					e.printStackTrace();
 					this.kill();
 					if (handler.getResponse() != null) {
 						this.serveError(500, handler, e.toString());
 					}
 				} catch (RuntimeException e) {
 					this.log("fatal").append(e.toString());
 					e.printStackTrace();
 					this.kill();
 					if (handler.getResponse() != null) {
 						this.serveError(500, handler, e.toString());
 					}
 				}
 			}
 			if (handler.getResponse() != null) {
 				handler.getResponse().flush();
 			}
 		} catch (InterruptedException ie) {
 			ie.printStackTrace();
 		}
 	}
 
 	public ReentrantLock getMutex() {
 		return this.mutex_;
 	}
 
 	public void setLoaded() {
 		this.setState(WebSession.State.Loaded, this.controller_
 				.getConfiguration().getSessionTimeout());
 	}
 
 	// public void generateNewSessionId() ;
 	private void handleWebSocketRequest(WebSession.Handler handler) {
 	}
 
 	private static void handleWebSocketMessage(WebSession session) {
 	}
 
 	private static void webSocketReady(WebSession session) {
 	}
 
 	private void checkTimers() {
 		WContainerWidget timers = this.app_.getTimerRoot();
 		List<WWidget> timerWidgets = timers.getChildren();
 		List<WTimerWidget> expired = new ArrayList<WTimerWidget>();
 		for (int i = 0; i < timerWidgets.size(); ++i) {
 			WTimerWidget wti = ((timerWidgets.get(i)) instanceof WTimerWidget ? (WTimerWidget) (timerWidgets
 					.get(i))
 					: null);
 			if (wti.isTimerExpired()) {
 				expired.add(wti);
 			}
 		}
 		WMouseEvent dummy = new WMouseEvent();
 		for (int i = 0; i < expired.size(); ++i) {
 			expired.get(i).clicked().trigger(dummy);
 		}
 	}
 
 	private void hibernate() {
 		if (this.app_ != null && this.app_.localizedStrings_ != null) {
 			this.app_.localizedStrings_.hibernate();
 		}
 	}
 
 	private ReentrantLock mutex_;
 	private EntryPointType type_;
 	private String favicon_;
 	private WebSession.State state_;
 	private String sessionId_;
 	private String sessionIdCookie_;
 	boolean sessionIdChanged_;
 	private boolean sessionIdCookieChanged_;
 	private boolean sessionIdInUrl_;
 	private WtServlet controller_;
 	private WebRenderer renderer_;
 	private String applicationName_;
 	private String bookmarkUrl_;
 	private String basePath_;
 	private String absoluteBaseUrl_;
 	private String applicationUrl_;
 	private String deploymentPath_;
 	private String redirect_;
 	String pagePathInfo_;
 	private WebResponse asyncResponse_;
 	private WebResponse bootStyleResponse_;
 	private boolean canWriteAsyncResponse_;
 	private boolean noBootStyleResponse_;
 	private boolean progressiveBoot_;
 	private java.util.concurrent.locks.Condition recursiveEvent_;
 	private boolean newRecursiveEvent_;
 	private java.util.concurrent.locks.Condition updatesPendingEvent_;
 	private boolean updatesPending_;
 	private WEnvironment embeddedEnv_;
 	private WEnvironment env_;
 	private WApplication app_;
 	private boolean debug_;
 	private List<WebSession.Handler> handlers_;
 	private List<WObject> emitStack_;
 	private WebSession.Handler recursiveEventLoop_;
 
 	// private WResource decodeResource(String resourceId) ;
 	private AbstractEventSignal decodeSignal(String signalId) {
 		AbstractEventSignal result = this.app_.decodeExposedSignal(signalId);
 		if (result != null) {
 			return result;
 		} else {
 			this.log("error").append("decodeSignal(): signal '").append(
 					signalId).append("' not exposed");
 			return null;
 		}
 	}
 
 	private AbstractEventSignal decodeSignal(String objectId, String name) {
 		AbstractEventSignal result = this.app_.decodeExposedSignal(objectId,
 				name);
 		if (result != null) {
 			return result;
 		} else {
 			this.log("error").append("decodeSignal(): signal '").append(
 					objectId).append('.').append(name).append("' not exposed");
 			return null;
 		}
 	}
 
 	private static WObject.FormData getFormData(WebRequest request, String name) {
 		List<UploadedFile> files = new ArrayList<UploadedFile>();
 		CollectionUtils.findInMultimap(request.getUploadedFiles(), name, files);
 		return new WObject.FormData(request.getParameterValues(name), files);
 	}
 
 	private void render(WebSession.Handler handler) throws IOException {
 		try {
 			if (!this.env_.hasAjax()) {
 				try {
 					this.checkTimers();
 				} catch (RuntimeException e) {
 					this.log("error").append(
 							"Exception while triggering timers").append(
 							e.toString());
 					throw e;
 				}
 			}
 			if (this.app_.isQuited()) {
 				this.kill();
 			}
 			this.serveResponse(handler);
 		} catch (RuntimeException e) {
 			handler.getResponse().flush();
 			handler.setRequest((WebRequest) null, (WebResponse) null);
 			throw e;
 		}
 		this.updatesPending_ = false;
 	}
 
 	private void serveError(int status, WebSession.Handler handler, String e)
 			throws IOException {
 		this.renderer_.serveError(status, handler.getResponse(), e);
 		handler.getResponse().flush();
 		handler.setRequest((WebRequest) null, (WebResponse) null);
 	}
 
 	private void serveResponse(WebSession.Handler handler) throws IOException {
 		if (handler.getResponse().getResponseType() == WebRequest.ResponseType.Page) {
 			this.pagePathInfo_ = handler.getRequest().getPathInfo();
 			String wtdE = handler.getRequest().getParameter("wtd");
 			if (wtdE != null && wtdE.equals(this.sessionId_)) {
 				this.sessionIdInUrl_ = true;
 			} else {
 				this.sessionIdInUrl_ = false;
 			}
 		}
 		if (!handler.getRequest().isWebSocketMessage()) {
 			if (this.bootStyleResponse_ != null) {
 				if (handler.getResponse().getResponseType() == WebRequest.ResponseType.Script
 						&& !(handler.getRequest().getParameter("skeleton") != null)) {
 					this.renderer_.serveLinkedCss(this.bootStyleResponse_);
 				}
 				this.flushBootStyleResponse();
 			}
 			this.renderer_.serveResponse(handler.getResponse());
 		}
 		handler.getResponse().flush();
 		handler.setRequest((WebRequest) null, (WebResponse) null);
 	}
 
 	enum SignalKind {
 		LearnedStateless(0), AutoLearnStateless(1), Dynamic(2);
 
 		private int value;
 
 		SignalKind(int value) {
 			this.value = value;
 		}
 
 		/**
 		 * Returns the numerical representation of this enum.
 		 */
 		public int getValue() {
 			return value;
 		}
 	}
 
 	private void processSignal(AbstractEventSignal s, String se,
 			WebRequest request, WebSession.SignalKind kind) {
 		if (!(s != null)) {
 			return;
 		}
 		switch (kind) {
 		case LearnedStateless:
 			s.processLearnedStateless();
 			break;
 		case AutoLearnStateless:
 			s.processAutoLearnStateless(this.renderer_);
 			break;
 		case Dynamic:
 			JavaScriptEvent jsEvent = new JavaScriptEvent();
 			jsEvent.get(request, se);
 			s.processDynamic(jsEvent);
 		}
 	}
 
 	private List<Integer> getSignalProcessingOrder(WEvent e) {
 		WebSession.Handler handler = e.impl_.handler;
 		List<Integer> highPriority = new ArrayList<Integer>();
 		List<Integer> normalPriority = new ArrayList<Integer>();
 		for (int i = 0;; ++i) {
 			WebRequest request = handler.getRequest();
 			String se = i > 0 ? 'e' + String.valueOf(i) : "";
 			String signalE = this.getSignal(request, se);
 			if (!(signalE != null)) {
 				break;
 			}
 			if (!signalE.equals("user") && !signalE.equals("hash")
 					&& !signalE.equals("none") && !signalE.equals("poll")
 					&& !signalE.equals("load")) {
 				AbstractEventSignal signal = this.decodeSignal(signalE);
 				if (!(signal != null)) {
 				} else {
 					if (signal.getName() == WFormWidget.CHANGE_SIGNAL) {
 						highPriority.add(i);
 					} else {
 						normalPriority.add(i);
 					}
 				}
 			} else {
 				normalPriority.add(i);
 			}
 		}
 		highPriority.addAll(normalPriority);
 		return highPriority;
 	}
 
 	private void notifySignal(WEvent e) throws IOException {
 		WebSession.Handler handler = e.impl_.handler;
 		if (handler.nextSignal == -1) {
 			handler.signalOrder = this.getSignalProcessingOrder(e);
 			handler.nextSignal = 0;
 		}
 		for (int i = handler.nextSignal; i < handler.signalOrder.size(); ++i) {
 			if (!(handler.getRequest() != null)) {
 				return;
 			}
 			WebRequest request = handler.getRequest();
 			int signalI = handler.signalOrder.get(i);
 			String se = signalI > 0 ? 'e' + String.valueOf(signalI) : "";
 			String signalE = this.getSignal(request, se);
 			if (!(signalE != null)) {
 				return;
 			}
 			this.renderer_.setRendered(true);
 			if (signalE.equals("none") || signalE.equals("load")) {
 				if (signalE.equals("load")
 						&& this.getType() == EntryPointType.WidgetSet) {
 					this.renderer_.setRendered(false);
 				}
 				this.renderer_.setVisibleOnly(false);
 			} else {
 				if (!signalE.equals("poll")) {
 					this.propagateFormValues(e, se);
 					if (i == 0) {
 						this.renderer_.saveChanges();
 					}
 					handler.nextSignal = i + 1;
 					if (signalE.equals("hash")) {
 						String hashE = request.getParameter(se + "_");
 						if (hashE != null) {
 							this.app_.changedInternalPath(hashE);
 							this.app_.doJavaScript("Wt3_1_11.scrollIntoView("
 									+ WWebWidget.jsStringLiteral(hashE) + ");");
 						} else {
 							this.app_.changedInternalPath("");
 						}
 					} else {
 						for (int k = 0; k < 3; ++k) {
 							WebSession.SignalKind kind = WebSession.SignalKind
 									.values()[k];
 							if (kind == WebSession.SignalKind.AutoLearnStateless
 									&& 0 != 0) {
 								break;
 							}
 							if (signalE.equals("user")) {
 								String idE = request.getParameter(se + "id");
 								String nameE = request
 										.getParameter(se + "name");
 								if (!(idE != null) || !(nameE != null)) {
 									break;
 								}
 								this.processSignal(this
 										.decodeSignal(idE, nameE), se, request,
 										kind);
 							} else {
 								this.processSignal(this.decodeSignal(signalE),
 										se, request, kind);
 							}
 							if (kind == WebSession.SignalKind.LearnedStateless
 									&& i == 0) {
 								this.renderer_.discardChanges();
 							}
 						}
 					}
 				}
 			}
 		}
 	}
 
 	private void propagateFormValues(WEvent e, String se) {
 		WebRequest request = e.impl_.handler.getRequest();
 		this.renderer_.updateFormObjectsList(this.app_);
 		Map<String, WObject> formObjects = this.renderer_.getFormObjects();
 		String focus = request.getParameter(se + "focus");
 		if (focus != null) {
 			int selectionStart = -1;
 			int selectionEnd = -1;
 			try {
 				String selStart = request.getParameter(se + "selstart");
 				if (selStart != null) {
 					selectionStart = Integer.parseInt(selStart);
 				}
 				String selEnd = request.getParameter(se + "selend");
 				if (selEnd != null) {
 					selectionEnd = Integer.parseInt(selEnd);
 				}
 			} catch (NumberFormatException ee) {
 				this.log("error").append(
 						"Could not lexical cast selection range");
 			}
 			this.app_.setFocus(focus, selectionStart, selectionEnd);
 		} else {
 			this.app_.setFocus("", -1, -1);
 		}
 		for (Iterator<Map.Entry<String, WObject>> i_it = formObjects.entrySet()
 				.iterator(); i_it.hasNext();) {
 			Map.Entry<String, WObject> i = i_it.next();
 			String formName = i.getKey();
 			WObject obj = i.getValue();
 			if (!(0 != 0)) {
 				obj.setFormData(getFormData(request, se + formName));
 			} else {
 				obj.setRequestTooLarge(0);
 			}
 		}
 	}
 
 	private String getSignal(WebRequest request, String se) {
 		String signalE = request.getParameter(se + "signal");
 		if (!(signalE != null)) {
 			final int signalLength = 7 + se.length();
 			Map<String, String[]> entries = request.getParameterMap();
 			for (Iterator<Map.Entry<String, String[]>> i_it = entries
 					.entrySet().iterator(); i_it.hasNext();) {
 				Map.Entry<String, String[]> i = i_it.next();
 				if (i.getKey().length() > (int) signalLength
 						&& i.getKey().substring(0, 0 + signalLength).equals(
 								se + "signal=")) {
 					signalE = i.getValue()[0];
 					String v = i.getKey().substring(signalLength);
 					if (v.length() >= 2) {
 						String e = v.substring(v.length() - 2);
 						if (e.equals(".x") || e.equals(".y")) {
 							v = v.substring(0, 0 + v.length() - 2);
 						}
 					}
 					(signalE) = v;
 					break;
 				}
 			}
 		}
 		return signalE;
 	}
 
 	private void init(WebRequest request) {
 		this.env_.init(request);
 		String hashE = request.getParameter("_");
 		this.absoluteBaseUrl_ = this.env_.getUrlScheme() + "://"
 				+ this.env_.getHostName() + this.basePath_;
 		boolean useAbsoluteUrls;
 		String absoluteBaseUrl = this.app_.readConfigurationProperty("baseURL",
 				this.absoluteBaseUrl_);
 		if (absoluteBaseUrl != this.absoluteBaseUrl_) {
 			this.absoluteBaseUrl_ = absoluteBaseUrl;
 			useAbsoluteUrls = true;
 		} else {
 			useAbsoluteUrls = false;
 		}
 		if (useAbsoluteUrls) {
 			int slashpos = this.absoluteBaseUrl_.lastIndexOf('/');
 			if (slashpos != -1
 					&& slashpos != this.absoluteBaseUrl_.length() - 1) {
 				this.absoluteBaseUrl_ = this.absoluteBaseUrl_.substring(0,
 						0 + slashpos + 1);
 			}
 		}
 		this.bookmarkUrl_ = this.applicationName_;
 		if (this.applicationName_.length() == 0) {
 			this.bookmarkUrl_ = this.applicationUrl_;
 		}
 		if (this.getType() == EntryPointType.WidgetSet || useAbsoluteUrls) {
 			this.applicationUrl_ = this.absoluteBaseUrl_
 					+ this.applicationName_;
 			this.bookmarkUrl_ = this.applicationUrl_;
 		}
 		String path = request.getPathInfo();
 		if (path.length() == 0 && hashE != null) {
 			path = hashE;
 		}
 		this.env_.setInternalPath(path);
 		this.pagePathInfo_ = request.getPathInfo();
 	}
 
 	private boolean start() {
 		try {
 			this.app_ = this.controller_.doCreateApplication(this);
 		} catch (RuntimeException e) {
 			this.app_ = null;
 			this.kill();
 			throw e;
 		}
 		return this.app_ != null;
 	}
 
 	private String getSessionQuery() {
 		return "?wtd=" + this.sessionId_;
 	}
 
 	private void flushBootStyleResponse() {
 		if (this.bootStyleResponse_ != null) {
 			this.bootStyleResponse_.flush();
 			this.bootStyleResponse_ = null;
 		}
 	}
 
 	static UploadedFile uf;
 
 	static boolean isAbsoluteUrl(String url) {
 		return url.indexOf("://") != -1;
 	}
 
 	static String host(String url) {
 		int pos = 0;
 		for (int i = 0; i < 3; ++i) {
 			pos = url.indexOf('/', pos);
 			if (pos == -1) {
 				return url;
 			} else {
 				++pos;
 			}
 		}
 		return url.substring(0, 0 + pos - 1);
 	}
 
 	private static ThreadLocal<WebSession.Handler> threadHandler_ = new ThreadLocal<WebSession.Handler>();
 }
