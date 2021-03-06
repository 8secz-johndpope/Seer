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
 
 package com.bloatit.web.server;
 
 import java.math.BigDecimal;
 import java.util.ArrayDeque;
 import java.util.Deque;
 import java.util.List;
 
 import com.bloatit.framework.AuthToken;
 import com.bloatit.web.actions.Action;
 import com.bloatit.web.annotations.Message;
 import com.bloatit.web.utils.url.IndexPageUrl;
 import com.bloatit.web.utils.url.Parameters;
 import com.bloatit.web.utils.url.Url;
 import com.bloatit.web.utils.url.UrlParameter;
 
 /**
  * <p>
  * A class to handle the user session on the web server
  * </p>
  * <p>
  * A session starts when the user arrives on the server (first GET request). When the user
  * login, his sessions continues (he'll therefore keep all his session informations), but
  * he simply gets a new authtoken that says he's logged
  * </p>
  * <p>
  * Session is used for various purposes :
  * <li>Store some parameters {@link Session#addParameter(String, String)}</li>
  * <li>Perform localization</li>
  * <li>Store pages that the user wishes to consult, be he couldn't because he didn't meet
  * the requirements</li>
  * </p>
  */
 public class Session {
 
     public static final long LOGGED_SESSION_DURATION = 1296000; // 15 days in seconds
     public static final long DEFAULT_SESSION_DURATION = 86400; // 1 days in seconds
 
 
 	private final String key;
 	private final Deque<Action> actionList;
 	private final Deque<Notification> notificationList;
 	private AuthToken authToken;
 
 	private Url lastStablePage = null;
 	private Url targetPage = null;
 
 	private long expirationTime;
 
 	/**
 	 * The place to store session data
 	 */
 	private final Parameters sessionParams = new Parameters();
 
 	Session(final String key) {
	    this.key = key;
 
 		authToken = null;
 		actionList = new ArrayDeque<Action>();
 		notificationList = new ArrayDeque<Notification>();
 		resetExpirationTime();
 	}
 
 	public final void resetExpirationTime() {
 	    if(isLogged()) {
 	        expirationTime = Context.getTime() + LOGGED_SESSION_DURATION;
 	    } else {
 	        expirationTime = Context.getTime() + DEFAULT_SESSION_DURATION;
 	    }
 	}
 
 	public final void setAuthToken(final AuthToken token) {
 		authToken = token;
 		resetExpirationTime();
 	}
 
 	public final AuthToken getAuthToken() {
 		return authToken;
 	}
 
 	public final boolean isLogged() {
 		return authToken != null;
 	}
 
 	public final boolean isExpired() {
         return Context.getTime() > expirationTime;
     }
 
 	public final String getKey() {
 		return key;
 	}
 
 	public final Deque<Action> getActionList() {
 		return actionList;
 	}
 
 	public void setLastStablePage(final Url p) {
 		lastStablePage = p;
 	}
 
 	public Url getLastStablePage() {
 		return lastStablePage;
 	}
 
 	/**
 	 * You should use the pickPreferedPage instead.
 	 */
 	@Deprecated
 	public final Url getTargetPage() {
 		return targetPage;
 	}
 
 	public Url pickPreferredPage() {
 		if (targetPage != null) {
 			final Url tempStr = targetPage;
 			targetPage = null;
 			return tempStr;
 		} else if (lastStablePage != null) {
 			return lastStablePage;
 		} else {
 			return new IndexPageUrl();
 		}
 	}
 
 	public final void setTargetPage(final Url targetPage) {
 		this.targetPage = targetPage;
 	}
 
 	public final void notifyGood(final String message) {
 		notificationList.add(new Notification(message, Notification.Type.GOOD));
 	}
 
 	public final void notifyBad(final String message) {
 		notificationList.add(new Notification(message, Notification.Type.BAD));
 	}
 
 	public final void notifyError(final String message) {
 		notificationList.add(new Notification(message, Notification.Type.ERROR));
 	}
 
 	/**
 	 * Notifies all elements in a list as warnings
 	 */
 	public final void notifyList(final List<Message> errors) {
 		for (final Message error : errors) {
 			switch (error.getLevel()) {
 			case ERROR:
 				notifyError(error.getMessage());
 				break;
 			case WARNING:
 				notifyBad(error.getMessage());
 				break;
 			case INFO:
 				notifyGood(error.getMessage());
 				break;
 			default:
 				break;
 			}
 		}
 	}
 
 	public final void flushNotifications() {
 		notificationList.clear();
 	}
 
 	public final Deque<Notification> getNotifications() {
 		return notificationList;
 	}
 
 	/**
 	 * Finds all the session parameters
 	 * @return the parameter of the session
 	 * @deprecated use a RequestParam
 	 */
 	@Deprecated
 	public final Parameters getParams() {
 		return sessionParams;
 	}
 
 	/**
 	 * Finds a given parameter in the session
 	 * @param paramKey the key of the parameter
 	 * @return the value of the parameter
 	 * @deprecated use a RequestParam
 	 */
 	@Deprecated
 	public final String getParam(final String paramKey) {
 		return sessionParams.get(paramKey);
 	}
 
 	/**
 	 * <p>
 	 * Saves a new parameter in the session. The parameter will be saved only if <code>
 	 * paramValue</code> is <i>not null</i>. If you want to save a <code>null</code>
 	 * value, use {@link #addParamForced(String, String)}.
 	 * </p>
 	 * <p>
 	 * Session parameters are available until they are checked, or session ends
 	 * </p>
 	 * @param paramKey
 	 * @param paramValue
 	 */
 	public final void addParameter(final String paramKey, final String paramValue) {
 	    if( paramValue != null && paramKey != null) {
 	        sessionParams.put(paramKey, paramValue);
 	    }
 	}
 
 	public final void addParameter(UrlParameter<?> param){
 	    sessionParams.put(param.getName(), param.getStringValue());
 	}
 
 	/**
      * <p>
      * Saves a new parameter in the session. This method will save even <code>
      * null</code> parameters.
      * </p>
      * <p>
      * Session parameters are available until they are checked, or session ends
      * </p>
      * @param paramKey
      * @param paramValue
      */
 	public final void addParamForced(final String paramKey, final String paramValue) {
         sessionParams.put(paramKey, paramValue);
     }
 
 	/**
      * <p>
      * Saves a new <code>BigDecimal</code> in the session.
      * </p>
      * <p>
      * This method is null-safe : if <code>paramValue</code> is null, the method doesn't fail
      * but no parameter is added
      * </p>
      * <p>
      * Session parameters are available until they are checked, or session ends
      * </p>
      * @param paramKey
      * @param paramValue
      */
     public final void addParam(final String paramKey, final BigDecimal paramValue) {
         if(paramValue != null){
             sessionParams.put(paramKey, paramValue.toPlainString());
         }
     }
 }
