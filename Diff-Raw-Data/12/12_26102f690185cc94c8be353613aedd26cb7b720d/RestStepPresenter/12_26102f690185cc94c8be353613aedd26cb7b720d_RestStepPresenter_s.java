 /*******************************************************************************
  * Copyright (c) 2012 OBiBa. All rights reserved.
  *
  * This program and the accompanying materials
  * are made available under the terms of the GNU Public License v3.0.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  ******************************************************************************/
 package org.obiba.opal.web.gwt.app.client.wizard.importdata.presenter;
 
 import org.obiba.opal.web.gwt.app.client.event.NotificationEvent;
 import org.obiba.opal.web.gwt.app.client.wizard.WizardStepDisplay;
 import org.obiba.opal.web.gwt.app.client.wizard.importdata.ImportData;
 import org.obiba.opal.web.gwt.app.client.wizard.importdata.ImportFormat;
 
 import com.google.gwt.event.shared.EventBus;
 import com.google.inject.Inject;
 import com.gwtplatform.mvp.client.PresenterWidget;
 import com.gwtplatform.mvp.client.View;
 
 public class RestStepPresenter extends PresenterWidget<RestStepPresenter.Display> implements DataImportPresenter.DataImportFormatStepPresenter {
 
   @Inject
   public RestStepPresenter(EventBus eventBus, RestStepPresenter.Display view) {
     super(eventBus, view);
   }
 
   @Override
   protected void onBind() {
     super.onBind();
   }
 
   @Override
   public ImportData getImportData() {
     ImportData importData = new ImportData();
     importData.setImportFormat(ImportFormat.REST);
     importData.put("url", getView().getUrl()) //
     .put("username", getView().getUsername())//
     .put("password", getView().getPassword())//
     .put("remoteDatasource", getView().getRemoteDatasource());
     return importData;
   }
 
   @Override
   public boolean validate() {
     if(getView().getUrl().isEmpty()) {
       getEventBus().fireEvent(NotificationEvent.newBuilder().error("OpalURLIsRequired").build());
       return false;
     }
     if(getView().getUsername().isEmpty()) {
       getEventBus().fireEvent(NotificationEvent.newBuilder().error("UsernameIsRequired").build());
       return false;
     }
     if(getView().getRemoteDatasource().isEmpty()) {
       getEventBus().fireEvent(NotificationEvent.newBuilder().error("RemoteDatasourceIsRequired").build());
       return false;
     }
     return true;
   }
 
   public interface Display extends View, WizardStepDisplay {
 
     String getRemoteDatasource();
 
     String getPassword();
 
     String getUsername();
 
     String getUrl();
   }
 }
