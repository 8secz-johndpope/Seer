 /*******************************************************************************
  * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
  * 
  * This program and the accompanying materials
  * are made available under the terms of the GNU Public License v3.0.
  * 
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  ******************************************************************************/
 package org.obiba.opal.web.gwt.app.client.fs.presenter;
 
 import org.obiba.opal.web.gwt.app.client.event.NotificationEvent;
 import org.obiba.opal.web.gwt.app.client.fs.event.FileDeletedEvent;
 import org.obiba.opal.web.gwt.app.client.fs.event.FileDownloadEvent;
 import org.obiba.opal.web.gwt.app.client.fs.event.FileSelectionChangeEvent;
 import org.obiba.opal.web.gwt.app.client.fs.event.FileSystemTreeFolderSelectionChangeEvent;
 import org.obiba.opal.web.gwt.app.client.fs.event.FolderRefreshedEvent;
 import org.obiba.opal.web.gwt.app.client.place.Places;
 import org.obiba.opal.web.gwt.app.client.presenter.ApplicationPresenter;
 import org.obiba.opal.web.gwt.app.client.widgets.event.ConfirmationEvent;
 import org.obiba.opal.web.gwt.app.client.widgets.event.ConfirmationRequiredEvent;
 import org.obiba.opal.web.gwt.rest.client.ResourceAuthorizationRequestBuilderFactory;
 import org.obiba.opal.web.gwt.rest.client.ResourceRequestBuilderFactory;
 import org.obiba.opal.web.gwt.rest.client.ResponseCodeCallback;
 import org.obiba.opal.web.gwt.rest.client.authorization.CompositeAuthorizer;
 import org.obiba.opal.web.gwt.rest.client.authorization.HasAuthorization;
 import org.obiba.opal.web.model.client.opal.FileDto;
 
 import com.google.gwt.event.dom.client.ClickEvent;
 import com.google.gwt.event.dom.client.ClickHandler;
 import com.google.gwt.event.dom.client.HasClickHandlers;
 import com.google.gwt.event.shared.EventBus;
 import com.google.gwt.http.client.Request;
 import com.google.gwt.http.client.Response;
 import com.google.gwt.user.client.ui.HasWidgets;
 import com.google.inject.Inject;
 import com.gwtplatform.mvp.client.Presenter;
 import com.gwtplatform.mvp.client.View;
 import com.gwtplatform.mvp.client.annotations.NameToken;
 import com.gwtplatform.mvp.client.annotations.ProxyStandard;
 import com.gwtplatform.mvp.client.proxy.ProxyPlace;
 import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
 
 public class FileExplorerPresenter extends Presenter<FileExplorerPresenter.Display, FileExplorerPresenter.Proxy> {
 
   public interface Display extends View {
     public HasWidgets getFileSystemTree();
 
     public HasWidgets getFolderDetailsPanel();
 
     public HasClickHandlers getFileUploadButton();
 
     public HasClickHandlers getFileDeleteButton();
 
     public HasClickHandlers getFileDownloadButton();
 
     public HasClickHandlers getCreateFolderButton();
 
     public void setEnabledFileDeleteButton(boolean enabled);
 
     public HasAuthorization getCreateFolderAuthorizer();
 
     public HasAuthorization getFileUploadAuthorizer();
 
     public HasAuthorization getFileDownloadAuthorizer();
 
     public HasAuthorization getFileDeleteAuthorizer();
 
   }
 
   @ProxyStandard
   @NameToken(Places.files)
   public interface Proxy extends ProxyPlace<FileExplorerPresenter> {
   }
 
   FileSystemTreePresenter fileSystemTreePresenter;
 
   FolderDetailsPresenter folderDetailsPresenter;
 
   FileUploadDialogPresenter fileUploadDialogPresenter;
 
   CreateFolderDialogPresenter createFolderDialogPresenter;
 
   private Runnable actionRequiringConfirmation;
 
   @Inject
  @SuppressWarnings("PMD.ExcessiveParameterList")
   public FileExplorerPresenter(Display display, EventBus eventBus, Proxy proxy, FileSystemTreePresenter fileSystemTreePresenter, FolderDetailsPresenter folderDetailsPresenter, FileUploadDialogPresenter fileUploadDialogPresenter, CreateFolderDialogPresenter createFolderDialogPresenter) {
     super(eventBus, display, proxy);
     this.fileSystemTreePresenter = fileSystemTreePresenter;
     this.folderDetailsPresenter = folderDetailsPresenter;
     this.fileUploadDialogPresenter = fileUploadDialogPresenter;
     this.createFolderDialogPresenter = createFolderDialogPresenter;
   }
 
   @Override
   protected void revealInParent() {
     RevealContentEvent.fire(this, ApplicationPresenter.WORKBENCH, this);
   }
 
   @Override
   protected void onBind() {
     initDisplayComponents();
     addEventHandlers();
   }
 
   @Override
   protected void onUnbind() {
     getView().getFileSystemTree().remove(fileSystemTreePresenter.getDisplay().asWidget());
     getView().getFolderDetailsPanel().remove(folderDetailsPresenter.getDisplay().asWidget());
     folderDetailsPresenter.unbind();
     fileSystemTreePresenter.unbind();
     createFolderDialogPresenter.unbind();
     fileUploadDialogPresenter.unbind();
   }
 
   @Override
   public void onReset() {
     folderDetailsPresenter.refreshDisplay();
   }
 
   @Override
   public void onReveal() {
     fileSystemTreePresenter.revealDisplay();
     folderDetailsPresenter.revealDisplay();
   }
 
   private void authorizeFile(FileDto dto) {
     // download
     ResourceAuthorizationRequestBuilderFactory.newBuilder().forResource("/files" + dto.getPath()).get().authorize(getView().getFileDownloadAuthorizer()).send();
     // delete
     ResourceAuthorizationRequestBuilderFactory.newBuilder().forResource("/files" + dto.getPath()).delete().authorize(getView().getFileDeleteAuthorizer()).send();
   }
 
   private void authorizeFolder(FileDto dto) {
     // create folder and upload
     ResourceAuthorizationRequestBuilderFactory.newBuilder().forResource("/files" + dto.getPath()).post()//
     .authorize(new CompositeAuthorizer(getView().getCreateFolderAuthorizer(), getView().getFileUploadAuthorizer())).send();
 
     if(!folderDetailsPresenter.hasSelection()) {
       // download
       ResourceAuthorizationRequestBuilderFactory.newBuilder().forResource("/files" + dto.getPath()).get().authorize(getView().getFileDownloadAuthorizer()).send();
       // delete
       setEnableFileDeleteButton();
     }
   }
 
   private void setEnableFileDeleteButton() {
     FileDto folder = folderDetailsPresenter.getCurrentFolder();
     if(folder.getPath().equals("/") || folder.getChildrenCount() > 0) {
       getView().setEnabledFileDeleteButton(false);
     } else {
       ResourceAuthorizationRequestBuilderFactory.newBuilder().forResource("/files" + folder.getPath()).delete().authorize(getView().getFileDeleteAuthorizer()).send();
     }
   }
 
   protected void initDisplayComponents() {
 
     folderDetailsPresenter.getDisplay().setSelectionEnabled(true);
 
     getView().getFileSystemTree().add(fileSystemTreePresenter.getDisplay().asWidget());
     getView().getFolderDetailsPanel().add(folderDetailsPresenter.getDisplay().asWidget());
 
     fileSystemTreePresenter.bind();
     folderDetailsPresenter.bind();
     createFolderDialogPresenter.bind();
     fileUploadDialogPresenter.bind();
   }
 
   /**
    * Returns the file currently selected or the current folder if no file is selected.
    * @return
    */
   private FileDto getCurrentSelectionOrFolder() {
     return folderDetailsPresenter.hasSelection() ? folderDetailsPresenter.getSelectedFile() : folderDetailsPresenter.getCurrentFolder();
   }
 
   private void addEventHandlers() {
 
     super.registerHandler(getView().getFileDeleteButton().addClickHandler(new ClickHandler() {
       @Override
       public void onClick(ClickEvent event) {
         // We are either deleting a file or a folder
         final FileDto fileToDelete = getCurrentSelectionOrFolder();
         actionRequiringConfirmation = new Runnable() {
           public void run() {
             deleteFile(fileToDelete);
           }
         };
 
         getEventBus().fireEvent(new ConfirmationRequiredEvent(actionRequiringConfirmation, "deleteFile", "confirmDeleteFile"));
       }
     }));
 
     super.registerHandler(getView().getFileDownloadButton().addClickHandler(new ClickHandler() {
       @Override
       public void onClick(ClickEvent event) {
         downloadFile(getCurrentSelectionOrFolder());
       }
     }));
 
     super.registerHandler(getView().getCreateFolderButton().addClickHandler(new ClickHandler() {
       @Override
       public void onClick(ClickEvent event) {
         FileDto currentFolder = folderDetailsPresenter.getCurrentFolder();
         createFolderDialogPresenter.setCurrentFolder(currentFolder);
         createFolderDialogPresenter.revealDisplay();
       }
     }));
 
     super.registerHandler(getView().getFileUploadButton().addClickHandler(new ClickHandler() {
 
       @Override
       public void onClick(ClickEvent event) {
         FileDto currentFolder = folderDetailsPresenter.getCurrentFolder();
         fileUploadDialogPresenter.setCurrentFolder(currentFolder);
         fileUploadDialogPresenter.revealDisplay();
       }
     }));
 
     super.registerHandler(getEventBus().addHandler(FileSelectionChangeEvent.getType(), new FileSelectionChangeEvent.Handler() {
 
       @Override
       public void onFileSelectionChange(FileSelectionChangeEvent event) {
         getView().setEnabledFileDeleteButton(folderDetailsPresenter.hasSelection());
         if(folderDetailsPresenter.hasSelection()) {
           authorizeFile(event.getFile());
         }
       }
     }));
 
     super.registerHandler(getEventBus().addHandler(FileSystemTreeFolderSelectionChangeEvent.getType(), new FileSystemTreeFolderSelectionChangeEvent.Handler() {
 
       @Override
       public void onFolderSelectionChange(FileSystemTreeFolderSelectionChangeEvent event) {
         setEnableFileDeleteButton();
       }
 
     }));
 
     super.registerHandler(getEventBus().addHandler(FolderRefreshedEvent.getType(), new FolderRefreshedEvent.Handler() {
 
       @Override
       public void onFolderRefreshed(FolderRefreshedEvent event) {
         authorizeFolder(event.getFolder());
       }
     }));
 
     super.registerHandler(getEventBus().addHandler(ConfirmationEvent.getType(), new ConfirmationEventHandler()));
 
   }
 
   class ConfirmationEventHandler implements ConfirmationEvent.Handler {
 
     public void onConfirmation(ConfirmationEvent event) {
       if(actionRequiringConfirmation != null && event.getSource().equals(actionRequiringConfirmation) && event.isConfirmed()) {
         actionRequiringConfirmation.run();
         actionRequiringConfirmation = null;
       }
     }
   }
 
   private void deleteFile(final FileDto file) {
     ResponseCodeCallback callbackHandler = new ResponseCodeCallback() {
 
       @Override
       public void onResponseCode(Request request, Response response) {
         if(response.getStatusCode() != Response.SC_OK) {
           getEventBus().fireEvent(NotificationEvent.newBuilder().error(response.getText()).build());
         } else {
           getEventBus().fireEvent(new FileDeletedEvent(file));
         }
       }
     };
 
     ResourceRequestBuilderFactory.newBuilder().forResource("/files" + file.getPath()).delete().withCallback(Response.SC_OK, callbackHandler).withCallback(Response.SC_FORBIDDEN, callbackHandler).withCallback(Response.SC_INTERNAL_SERVER_ERROR, callbackHandler).withCallback(Response.SC_NOT_FOUND, callbackHandler).send();
   }
 
   private void downloadFile(final FileDto file) {
     String url = new StringBuilder("/files").append(file.getPath()).toString();
     getEventBus().fireEvent(new FileDownloadEvent(url));
   }
 }
