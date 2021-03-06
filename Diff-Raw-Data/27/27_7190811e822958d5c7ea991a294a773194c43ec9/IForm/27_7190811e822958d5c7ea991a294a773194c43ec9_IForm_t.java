 package com.itmill.toolkit.terminal.gwt.client.ui;
 
 import com.google.gwt.user.client.ui.SimplePanel;
 import com.google.gwt.user.client.ui.Widget;
 import com.itmill.toolkit.terminal.gwt.client.ApplicationConnection;
 import com.itmill.toolkit.terminal.gwt.client.Container;
 import com.itmill.toolkit.terminal.gwt.client.Paintable;
 import com.itmill.toolkit.terminal.gwt.client.UIDL;
 
 public class IForm extends SimplePanel implements Paintable {
 
 	public static final String CLASSNAME = "i-form";
 
 	private Container lo;
 
 	private ApplicationConnection client;
 
 	public IForm() {
 		super();
 		setStyleName(CLASSNAME);
 	}
 
 	public void updateFromUIDL(UIDL uidl, ApplicationConnection client) {
 		this.client = client;
		
		if(client.updateComponent(this, uidl, true))
			return;
		
 		UIDL layoutUidl = uidl.getChildUIDL(0);
 		if (lo == null) {
 			lo = (Container) client.getWidget(layoutUidl);
 			setWidget((Widget) lo);
 		}
 		lo.updateFromUIDL(layoutUidl, client);
 	}
 }
