 /* 
 @ITMillApache2LicenseForJavaFiles@
  */
 
 package com.itmill.toolkit.terminal.gwt.client.ui;
 
 import com.google.gwt.user.client.DOM;
 import com.google.gwt.user.client.Event;
 import com.google.gwt.user.client.ui.UIObject;
 import com.itmill.toolkit.terminal.gwt.client.ApplicationConnection;
 
 public class Icon extends UIObject {
     private final ApplicationConnection client;
     private String myUri;
 
     public Icon(ApplicationConnection client) {
         setElement(DOM.createImg());
         DOM.setElementProperty(getElement(), "alt", "");
         setStyleName("i-icon");
         this.client = client;
         client.addPngFix(getElement());
     }
 
     public Icon(ApplicationConnection client, String uidlUri) {
         this(client);
         setUri(uidlUri);
     }
 
     public void setUri(String uidlUri) {
         if (!uidlUri.equals(myUri)) {
             String uri = client.translateToolkitUri(uidlUri);
             DOM.setElementProperty(getElement(), "src", uri);
             myUri = uidlUri;
            // start sinkin onload events, widgets responsibility to react
            sinkEvents(Event.ONLOAD);
         }
     }
 
 }
