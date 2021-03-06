 package org.obiba.opal.web.gwt.app.client;
 
 import org.obiba.opal.web.gwt.app.client.event.SessionCreatedEvent;
 import org.obiba.opal.web.gwt.app.client.event.SessionEndedEvent;
 import org.obiba.opal.web.gwt.app.client.place.Places;
 import org.obiba.opal.web.gwt.app.client.presenter.ConfirmationPresenter;
 import org.obiba.opal.web.gwt.app.client.presenter.UnhandledResponseNotificationPresenter;
 import org.obiba.opal.web.gwt.rest.client.DefaultResourceAuthorizationRequestBuilder;
 import org.obiba.opal.web.gwt.rest.client.DefaultResourceRequestBuilder;
 import org.obiba.opal.web.gwt.rest.client.RequestCredentials;
 import org.obiba.opal.web.gwt.rest.client.ResourceAuthorizationCache;
 import org.obiba.opal.web.gwt.rest.client.ResourceCallback;
 import org.obiba.opal.web.gwt.rest.client.ResourceRequestBuilderFactory;
 import org.obiba.opal.web.gwt.rest.client.UriBuilder;
 import org.obiba.opal.web.gwt.rest.client.event.RequestCredentialsExpiredEvent;
 import org.obiba.opal.web.gwt.rest.client.event.RequestErrorEvent;
 import org.obiba.opal.web.gwt.rest.client.event.RequestEventBus;
 import org.obiba.opal.web.gwt.rest.client.event.UnhandledResponseEvent;
 import org.obiba.opal.web.model.client.database.DatabasesStatusDto;
 import org.obiba.opal.web.model.client.opal.Subject;
 
 import com.google.common.base.Strings;
 import com.google.gwt.core.client.GWT;
 import com.google.gwt.event.shared.GwtEvent;
 import com.google.gwt.http.client.Response;
 import com.google.gwt.user.client.Window;
 import com.google.gwt.user.client.Window.ClosingEvent;
 import com.google.gwt.user.client.Window.ClosingHandler;
 import com.google.inject.Inject;
 import com.google.web.bindery.event.shared.EventBus;
 import com.gwtplatform.mvp.client.Bootstrapper;
 import com.gwtplatform.mvp.client.proxy.PlaceManager;
 import com.gwtplatform.mvp.client.proxy.PlaceRequest;
 import com.gwtplatform.mvp.client.proxy.RevealRootPopupContentEvent;
 
 public class OpalBootstrapperImpl implements Bootstrapper {
   private final PlaceManager placeManager;
 
   @Inject
   EventBus eventBus;
 
   @Inject
   ResourceAuthorizationCache authorizationCache;
 
   @Inject
   RequestCredentials requestCredentials;
 
   @Inject
   ConfirmationPresenter confirmationPresenter;
 
   @Inject
   public OpalBootstrapperImpl(PlaceManager placeManager) {
     this.placeManager = placeManager;
   }
 
   @Override
   public void onBootstrap() {
     initialize();
   }
 
   private void initialize() {
     // TODO: is there a better way to provide the dependencies to instances created with GWT.create()?
     DefaultResourceRequestBuilder.setup(new RequestEventBus() {
 
       @Override
       public void fireEvent(GwtEvent<?> event) {
         eventBus.fireEvent(event);
       }
     }, requestCredentials, authorizationCache);
 
     DefaultResourceAuthorizationRequestBuilder.setup(authorizationCache);
 
     initConfirmationPresenter();
 
     initUserSession();
 
     registerHandlers();
   }
 
   private void initUserSession() {
     String username = requestCredentials.getUsername();
    if(!Strings.isNullOrEmpty(username) && !"undefined".equals(username)) {
      UriBuilder builder = UriBuilder.create().segment("auth", "session", username, "username");
       ResourceRequestBuilderFactory.<Subject>newBuilder().forResource(builder.build()).get()
           .withCallback(new SubjectResourceCallback()).send();
     } else {
      placeManager.revealUnauthorizedPlace(Places.LOGIN);
     }
   }
 
   private void initConfirmationPresenter() {
     confirmationPresenter.bind();
   }
 
   private void registerHandlers() {
     eventBus.addHandler(RequestErrorEvent.getType(), new RequestErrorEvent.RequestErrorHandler() {
       @Override
       public void onRequestError(RequestErrorEvent e) {
         GWT.log("Request error: ", e.getException());
       }
     });
     eventBus.addHandler(RequestCredentialsExpiredEvent.getType(), new RequestCredentialsExpiredEvent.Handler() {
       @Override
       public void onCredentialsExpired(RequestCredentialsExpiredEvent e) {
         requestCredentials.invalidate();
         placeManager.revealUnauthorizedPlace(Places.LOGIN);
       }
     });
     eventBus.addHandler(SessionCreatedEvent.getType(), new SessionCreatedEvent.Handler() {
       @Override
       public void onSessionCreated(SessionCreatedEvent event) {
         ResourceRequestBuilderFactory.<DatabasesStatusDto>newBuilder()
             .forResource(UriBuilder.create().segment("system", "status", "databases").build()).get()
             .withCallback(new DatabasesStatusResourceCallback()).send();
       }
     });
 
     eventBus.addHandler(SessionEndedEvent.getType(), new SessionEndedEvent.Handler() {
 
       @Override
       public void onSessionEnded(SessionEndedEvent event) {
         if(requestCredentials.hasCredentials()) {
           ResourceRequestBuilderFactory.newBuilder().forResource(
               UriBuilder.create().segment("auth", "session", requestCredentials.extractCredentials()).build()).delete()
               .send();
           requestCredentials.invalidate();
         }
         // Reload application and reset history
         Window.Location.replace("/");
       }
     });
 
     // Kills the session it the browser is closed or when navigating to another page.
     Window.addWindowClosingHandler(new ClosingHandler() {
 
       @Override
       public void onWindowClosing(ClosingEvent arg0) {
         //eventBus.fireEvent(new SessionEndedEvent());
       }
 
     });
 
   }
 
   private class SubjectResourceCallback implements ResourceCallback<Subject> {
     @Override
     public void onResource(Response response, Subject subject) {
       if(response.getStatusCode() == Response.SC_OK) {
         requestCredentials.setUsername(subject.getPrincipal());
         placeManager.revealCurrentPlace();
       } else {
         eventBus.fireEvent(new SessionEndedEvent());
       }
     }
   }
 
   private class DatabasesStatusResourceCallback implements ResourceCallback<DatabasesStatusDto> {
     @Override
     public void onResource(Response response, DatabasesStatusDto resource) {
      if (!resource.getHasIdentifiers() || !resource.getHasStorage()) {
         placeManager.revealPlace(new PlaceRequest.Builder().nameToken(Places.INSTALL).build());
       } else {
         placeManager.revealCurrentPlace();
       }
     }
   }
 }
