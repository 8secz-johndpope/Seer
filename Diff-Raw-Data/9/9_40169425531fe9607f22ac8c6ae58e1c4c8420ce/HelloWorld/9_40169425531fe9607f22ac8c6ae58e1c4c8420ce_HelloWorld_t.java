 package org.errai.samples.helloworld.client;
 
 import com.google.gwt.core.client.EntryPoint;
 import com.google.gwt.event.dom.client.ClickEvent;
 import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.RootPanel;
 import org.jboss.errai.bus.client.CommandMessage;
import org.jboss.errai.bus.client.ErraiBus;
 import org.jboss.errai.bus.client.MessageBus;
 
 public class HelloWorld implements EntryPoint {
    private MessageBus bus = ErraiBus.get();
 
     public void onModuleLoad() {
         Button clickMe = new Button("Click Me!");
 
         clickMe.addClickHandler(new ClickHandler() {
             public void onClick(ClickEvent event) {
                 CommandMessage.create()
                         .toSubject("HelloWorld")
                         .sendNowWith(bus);
            }
         });
 
         RootPanel.get().add(clickMe);
     }
 }
