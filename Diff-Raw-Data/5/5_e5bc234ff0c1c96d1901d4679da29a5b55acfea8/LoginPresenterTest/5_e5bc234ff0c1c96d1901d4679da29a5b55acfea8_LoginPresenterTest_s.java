 package org.ourproject.kune.sitebar.client.ui;
 
 import org.easymock.EasyMock;
 import org.junit.Before;
 import org.junit.Test;
 import org.ourproject.kune.platf.client.dto.I18nLanguageDTO;
 import org.ourproject.kune.platf.client.state.SessionImpl;
 import org.ourproject.kune.workspace.client.sitebar.login.LoginListener;
 import org.ourproject.kune.workspace.client.sitebar.login.LoginPresenter;
 import org.ourproject.kune.workspace.client.sitebar.login.LoginView;
 
 public class LoginPresenterTest {
     private LoginPresenter presenter;
     private LoginView view;
     private LoginListener listener;
 
     @Before
     public void createObjects() {
         listener = EasyMock.createStrictMock(LoginListener.class);
         presenter = new LoginPresenter(new SessionImpl("foofoo", new I18nLanguageDTO()), listener);
         view = EasyMock.createStrictMock(LoginView.class);
     }
 
     @Test
     public void testViewInitialization() {
         // view.setEnabledLoginButton(false);
        view.reset();
         EasyMock.replay(view);
         presenter.init(view);
         EasyMock.verify(view);
     }
 
     @Test
     public void noUserInput() {
         // view.setEnabledLoginButton(false);
        view.reset();
         EasyMock.replay(view);
 
         presenter.init(view);
         // presenter.onDataChanged("", "");
         EasyMock.verify(view);
     }
 
     @Test
     public void nickPassTyped() {
         // view.setEnabledLoginButton(false);
        view.reset();
         // view.setEnabledLoginButton(true);
         EasyMock.replay(view);
 
         presenter.init(view);
         // presenter.onDataChanged("luther.b", "somepass");
         EasyMock.verify(view);
     }
 }
