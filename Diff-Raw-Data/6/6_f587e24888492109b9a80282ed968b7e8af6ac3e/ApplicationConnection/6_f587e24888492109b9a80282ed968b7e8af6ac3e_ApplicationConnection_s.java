 package com.itmill.toolkit.terminal.gwt.client;
 
 import java.util.Date;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.Vector;
 
 import com.google.gwt.http.client.Request;
 import com.google.gwt.http.client.RequestBuilder;
 import com.google.gwt.http.client.RequestCallback;
 import com.google.gwt.http.client.RequestException;
 import com.google.gwt.http.client.Response;
 import com.google.gwt.json.client.JSONArray;
 import com.google.gwt.json.client.JSONObject;
 import com.google.gwt.json.client.JSONParser;
 import com.google.gwt.json.client.JSONString;
 import com.google.gwt.json.client.JSONValue;
 import com.google.gwt.user.client.ui.FocusListener;
 import com.google.gwt.user.client.ui.FocusWidget;
 import com.google.gwt.user.client.ui.HasFocus;
 import com.google.gwt.user.client.ui.HasWidgets;
 import com.google.gwt.user.client.ui.Widget;
 import com.itmill.toolkit.terminal.gwt.client.ui.ContextMenu;
 import com.itmill.toolkit.terminal.gwt.client.ui.IView;
 
 /**
  * Entry point classes define <code>onModuleLoad()</code>.
  */
 public class ApplicationConnection implements FocusListener {
 
 	private String appUri;
 
 	private HashMap resourcesMap = new HashMap();
 
 	private static Console console;
 
 	private Vector pendingVariables = new Vector();
 
 	private HashMap idToPaintable = new HashMap();
 
 	private HashMap paintableToId = new HashMap();
 
 	private final WidgetSet widgetSet;
 
 	private ContextMenu contextMenu = null;
 
 	private IView view;
 
 	public ApplicationConnection(WidgetSet widgetSet) {
 		this.widgetSet = widgetSet;
 		appUri = getAppUri();
 
 		if (isDebugMode()) {
 			console = new DebugConsole();
 		} else {
 			console = new NullConsole();
 		}
 
 		makeUidlRequest("repaintAll=1");
 
 		// TODO remove hardcoded id name
 		view = new IView("itmtk-ajax-window");
 
 	}
 
 	public static Console getConsole() {
 		return console;
 	}
 
 	private native static boolean isDebugMode()
 	/*-{
 		var uri = $wnd.location;
 		var re = /debug[^\/]*$/;
 		return re.test(uri);
 	}-*/;
 
 	public native String getAppUri()
 	/*-{
 		var u = $wnd.itmtk.appUri;
 		if (u.indexOf("/") != 0 && u.indexOf("http") != 0) u = "../../../" +u;
 		 return u;
 	}-*/;
 
 	private native String getPathInfo()
 	/*-{
 		 return $wnd.itmtk.pathInfo;
 	 }-*/;
 
 	private void makeUidlRequest(String requestData) {
 		console.log("Making UIDL Request with params: " + requestData);
 		RequestBuilder rb = new RequestBuilder(RequestBuilder.POST, appUri
 				+ "/UIDL" + getPathInfo());
 		rb.setHeader("Content-Type",
 				"application/x-www-form-urlencoded; charset=utf-8");
 		try {
 			rb.sendRequest(requestData, new RequestCallback() {
 				public void onError(Request request, Throwable exception) {
 					// TODO Better reporting to user
 					console.error("Got error");
 				}
 
 				public void onResponseReceived(Request request,
 						Response response) {
 					handleReceivedJSONMessage(response);
 				}
 
 			});
 
 		} catch (RequestException e) {
 			// TODO Better reporting to user
 			console.error(e.getMessage());
 		}
 	}
 
 	private void handleReceivedJSONMessage(Response response) {
 		Date start = new Date();
 		String jsonText = response.getText().substring(3) + "}";
 		JSONValue json;
 		try {
 			json = JSONParser.parse(jsonText);
 		} catch (com.google.gwt.json.client.JSONException e) {
 			console.log(e.getMessage() + " - Original JSON-text:");
 			console.log(jsonText);
 			return;
 		}
 		// Handle redirect
 		JSONObject redirect = (JSONObject) ((JSONObject) json).get("redirect");
 		if (redirect != null) {
 			JSONString url = (JSONString) redirect.get("url");
 			if (url != null) {
 				console.log("redirecting to " + url.stringValue());
 				redirect(url.stringValue());
 				return;
 			}
 		}
 
 		// Store resources
 		JSONObject resources = (JSONObject) ((JSONObject) json)
 				.get("resources");
 		for (Iterator i = resources.keySet().iterator(); i.hasNext();) {
 			String key = (String) i.next();
 			resourcesMap.put(key, ((JSONString) resources.get(key))
 					.stringValue());
 		}
 
 		// Store locale data
 		if (((JSONObject) json).containsKey("locales")) {
 			JSONArray l = (JSONArray) ((JSONObject) json).get("locales");
 			for (int i = 0; i < l.size(); i++)
 				LocaleService.addLocale((JSONObject) l.get(i));
 		}
 
 		// Process changes
 		JSONArray changes = (JSONArray) ((JSONObject) json).get("changes");
 		for (int i = 0; i < changes.size(); i++) {
 			try {
 				UIDL change = new UIDL((JSONArray) changes.get(i));
 				try {
 					console.dirUIDL(change);
 				} catch (Exception e) {
 					console.log(e.getMessage());
 					// TODO: dir doesn't work in any browser although it should
 					// work (works in hosted mode)
 					// it partially did at some part but now broken.
 				}
 				UIDL uidl = change.getChildUIDL(0);
 				Paintable paintable = getPaintable(uidl.getId());
 				if (paintable != null)
 					paintable.updateFromUIDL(uidl, this);
 				else {
 					if (!uidl.getTag().equals("window"))
 						throw new IllegalStateException("Received update for "
 								+ uidl.getTag()
 								+ ", but there is no such paintable ("
 								+ uidl.getId() + ") registered yet.");
 					view.updateFromUIDL(uidl, this);
 				}
 			} catch (Throwable e) {
 				e.printStackTrace();
 			}
 		}
 
 		if (((JSONObject) json).containsKey("meta")) {
 			JSONObject meta = ((JSONObject) json).get("meta").isObject();
 			if (meta.containsKey("focus")) {
 				String focusPid = meta.get("focus").isString().stringValue();
 				Paintable toBeFocused = this.getPaintable(focusPid);
 				if (toBeFocused instanceof HasFocus) {
 					HasFocus toBeFocusedWidget = (HasFocus) toBeFocused;
 					toBeFocusedWidget.setFocus(true);
 				}
 			}
 		}
 
 		long prosessingTime = (new Date().getTime()) - start.getTime();
 		console.log(" Processing time was " + String.valueOf(prosessingTime)
 				+ "ms for " + jsonText.length() + " characters of JSON");
 		console.log("Referenced paintables: " + idToPaintable.size());
 
 	}
 
 	// Redirect browser
 	private static native void redirect(String url)
 	/*-{
 		 $wnd.location = url;
 	 }-*/;
 
 	public void registerPaintable(String id, Paintable paintable) {
 		idToPaintable.put(id, paintable);
 		paintableToId.put(paintable, id);
 	}
 
 	public void unregisterPaintable(Paintable p) {
 		idToPaintable.remove(paintableToId.get(p));
 		paintableToId.remove(p);
 
 		if (p instanceof HasWidgets) {
 			unregisterChildPaintables((HasWidgets) p);
 		}
 	}
 
 	public void unregisterChildPaintables(HasWidgets container) {
 		Iterator it = container.iterator();
 		while (it.hasNext()) {
 			Widget w = (Widget) it.next();
 			if (w instanceof Paintable) {
 				this.unregisterPaintable((Paintable) w);
 			}
 			if (w instanceof HasWidgets) {
 				unregisterChildPaintables((HasWidgets) w);
 			}
 		}
 	}
 
 	/**
 	 * Returns Paintable element by its id
 	 * 
 	 * @param id
 	 *            Paintable ID
 	 */
 	public Paintable getPaintable(String id) {
 		return (Paintable) idToPaintable.get(id);
 	}
 
 	private void addVariableToQueue(String paintableId, String variableName,
 			String encodedValue, boolean immediate, char type) {
 		String id = paintableId + "_" + variableName + "_" + type;
 		for (int i = 0; i < pendingVariables.size(); i += 2)
 			if ((pendingVariables.get(i)).equals(id)) {
 				pendingVariables.remove(i);
 				pendingVariables.remove(i);
 				break;
 			}
 		pendingVariables.add(id);
 		pendingVariables.add(encodedValue);
 		if (immediate)
 			sendPendingVariableChanges();
 	}
 
 	public void sendPendingVariableChanges() {
 		StringBuffer req = new StringBuffer();
 
 		req.append("changes=");
 		for (int i = 0; i < pendingVariables.size(); i++) {
 			if (i > 0)
 				req.append("\u0001");
 			req.append(pendingVariables.get(i));
 		}
 
 		pendingVariables.clear();
 		makeUidlRequest(req.toString());
 	}
 
 	private static native String escapeString(String value) 
 	/*-{
 		return encodeURIComponent(value);
 	}-*/;
 
 	public void updateVariable(String paintableId, String variableName,
 			String newValue, boolean immediate) {
 		addVariableToQueue(paintableId, variableName, escapeString(newValue),
 				immediate, 's');
 	}
 
 	public void updateVariable(String paintableId, String variableName,
 			int newValue, boolean immediate) {
 		addVariableToQueue(paintableId, variableName, "" + newValue, immediate,
 				'i');
 	}
 
 	public void updateVariable(String paintableId, String variableName,
 			long newValue, boolean immediate) {
 		addVariableToQueue(paintableId, variableName, "" + newValue, immediate,
 				'l');
 	}
 
 	public void updateVariable(String paintableId, String variableName,
 			float newValue, boolean immediate) {
 		addVariableToQueue(paintableId, variableName, "" + newValue, immediate,
 				'f');
 	}
 
 	public void updateVariable(String paintableId, String variableName,
 			double newValue, boolean immediate) {
 		addVariableToQueue(paintableId, variableName, "" + newValue, immediate,
 				'd');
 	}
 
 	public void updateVariable(String paintableId, String variableName,
 			boolean newValue, boolean immediate) {
 		addVariableToQueue(paintableId, variableName, newValue ? "true"
 				: "false", immediate, 'b');
 	}
 
 	public void updateVariable(String paintableId, String variableName,
 			Object[] values, boolean immediate) {
 		StringBuffer buf = new StringBuffer();
 		for (int i = 0; i < values.length; i++) {
 			if (i > 0)
 				buf.append(",");
 			buf.append(escapeString(values[i].toString()));
 		}
 		addVariableToQueue(paintableId, variableName, buf.toString(),
 				immediate, 'a');
 	}
 
 	public static Container getParentLayout(Widget component) {
 		Widget parent = component.getParent();
 		while (parent != null && !(parent instanceof Container))
 			parent = parent.getParent();
 		if (parent != null && ((Container) parent).hasChildComponent(component))
 			return (Container) parent;
 		return null;
 	}
 
 	/**
 	 * Update generic component features.
 	 * 
 	 * <h2>Selecting correct implementation</h2>
 	 * 
 	 * <p>
 	 * The implementation of a component depends on many properties, including
 	 * styles, component features, etc. Sometimes the user changes those
 	 * properties after the component has been created. Calling this method in
 	 * the beginning of your updateFromUIDL -method automatically replaces your
 	 * component with more appropriate if the requested implementation changes.
 	 * </p>
 	 * 
 	 * <h2>Caption, icon, error messages and description</h2>
 	 * 
 	 * <p>
 	 * Component can delegate management of caption, icon, error messages and
 	 * description to parent layout. This is optional an should be decided by
 	 * component author
 	 * </p>
 	 * 
 	 * <h2>Component visibility and disabling</h2>
 	 * 
 	 * This method will manage component visibility automatically and if
 	 * component is an instanceof FocusWidget, also handle component disabling
 	 * when needed.
 	 * 
 	 * @param currentWidget
 	 *            Current widget that might need replacement
 	 * @param uidl
 	 *            UIDL to be painted
 	 * @param manageCaption
 	 *            True if you want to delegate caption, icon, description and
 	 *            error message management to parent.
 	 * 
 	 * @return Returns true iff no further painting is needed by caller
 	 */
 	public boolean updateComponent(Widget component, UIDL uidl,
 			boolean manageCaption) {
 
 		// If the server request that a cached instance should be used, do
 		// nothing
 		if (uidl.getBooleanAttribute("cached"))
 			return true;
 
 		// Switch to correct implementation if needed
 		if (!widgetSet.isCorrectImplementation(component, uidl)) {
 			Container parent = getParentLayout(component);
 			if (parent != null) {
 				Widget w = widgetSet.createWidget(uidl);
 				parent.replaceChildComponent(component, w);
 				registerPaintable(uidl.getId(), (Paintable) w);
 				((Paintable) w).updateFromUIDL(uidl, this);
 				return true;
 			}
 		}
 
 		// Set captions
 		// TODO Manage Error messages
 		if (manageCaption) {
 			Container parent = getParentLayout(component);
 			if (parent != null)
 				parent.updateCaption((Paintable) component, uidl);
 		}
 
 		// Visibility, Disabling and read-only status
 		if (component instanceof FocusWidget) {
 			boolean enabled = true;
 			if (uidl.hasAttribute("disabled"))
 				enabled = !uidl.getBooleanAttribute("disabled");
 			else if (uidl.hasAttribute("readonly"))
 				enabled = !uidl.getBooleanAttribute("readonly");
 			((FocusWidget) component).setEnabled(enabled);
 		} else {
 			boolean enabled = true;
 			if (uidl.hasAttribute("disabled"))
 				enabled = !uidl.getBooleanAttribute("disabled");
 			if (!enabled)
 				component.addStyleName("i-disabled");
 			else
 				component.removeStyleName("i-disabled");
 		}
 		boolean visible = !uidl.getBooleanAttribute("invisible");
 		component.setVisible(visible);
 		if (!visible)
 			return true;
 
 		// add additional styles as css classes
		if (uidl.hasAttribute("style"))
 			component.addStyleName(uidl.getStringAttribute("style"));
 
 		return false;
 	}
 
 	/**
 	 * Get either existing or new widget for given UIDL.
 	 * 
 	 * If corresponding paintable has been previously painted, return it.
 	 * Otherwise create and register a new widget from UIDL. Caller must update
 	 * the returned widget from UIDL after it has been connected to parent.
 	 * 
 	 * @param uidl
 	 *            UIDL to create widget from.
 	 * @return Either existing or new widget corresponding to UIDL.
 	 */
 	public Widget getWidget(UIDL uidl) {
 		String id = uidl.getId();
 		Widget w = (Widget) getPaintable(id);
 		if (w != null)
 			return w;
 		w = widgetSet.createWidget(uidl);
 		registerPaintable(id, (Paintable) w);
 		return w;
 	}
 
 	public String getResource(String name) {
 		return (String) resourcesMap.get(name);
 	}
 
 	/**
 	 * Singleton method to get instance of app's context menu.
 	 * 
 	 * @return IContextMenu object
 	 */
 	public ContextMenu getContextMenu() {
 		if (contextMenu == null) {
 			contextMenu = new ContextMenu();
 		}
 		return contextMenu;
 	}
 
 	public void onFocus(Widget sender) {
 		// TODO Auto-generated method stub
 
 	}
 
 	public void onLostFocus(Widget sender) {
 		// TODO Auto-generated method stub
 
 	}
 }
