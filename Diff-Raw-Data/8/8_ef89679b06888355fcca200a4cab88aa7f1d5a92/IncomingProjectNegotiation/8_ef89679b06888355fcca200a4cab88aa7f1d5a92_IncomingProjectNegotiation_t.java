 package de.fu_berlin.inf.dpp.invitation;
 
 import java.io.File;
 import java.io.FilterInputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.lang.reflect.InvocationTargetException;
 import java.text.MessageFormat;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
 import java.util.concurrent.Callable;
 import java.util.concurrent.atomic.AtomicBoolean;
 import java.util.zip.ZipEntry;
 import java.util.zip.ZipInputStream;
 
 import org.apache.log4j.Logger;
 import org.eclipse.core.resources.IContainer;
 import org.eclipse.core.resources.IFile;
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.resources.IWorkspace;
 import org.eclipse.core.resources.IWorkspaceDescription;
 import org.eclipse.core.resources.IWorkspaceRoot;
 import org.eclipse.core.resources.IWorkspaceRunnable;
 import org.eclipse.core.resources.ResourcesPlugin;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IPath;
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.core.runtime.IStatus;
 import org.eclipse.core.runtime.Path;
 import org.eclipse.core.runtime.Status;
 import org.eclipse.core.runtime.SubMonitor;
 import org.eclipse.jface.dialogs.ProgressMonitorDialog;
 import org.eclipse.jface.operation.IRunnableWithProgress;
 import org.eclipse.ui.actions.WorkspaceModifyOperation;
 import org.picocontainer.annotations.Inject;
 
 import de.fu_berlin.inf.dpp.FileList;
 import de.fu_berlin.inf.dpp.FileListDiff;
 import de.fu_berlin.inf.dpp.FileListFactory;
 import de.fu_berlin.inf.dpp.Saros;
 import de.fu_berlin.inf.dpp.SarosContext;
 import de.fu_berlin.inf.dpp.User;
 import de.fu_berlin.inf.dpp.activities.ProjectExchangeInfo;
 import de.fu_berlin.inf.dpp.editor.internal.EditorAPI;
 import de.fu_berlin.inf.dpp.exceptions.LocalCancellationException;
 import de.fu_berlin.inf.dpp.exceptions.RemoteCancellationException;
 import de.fu_berlin.inf.dpp.exceptions.SarosCancellationException;
 import de.fu_berlin.inf.dpp.invitation.ProcessTools.CancelLocation;
 import de.fu_berlin.inf.dpp.invitation.ProcessTools.CancelOption;
 import de.fu_berlin.inf.dpp.net.JID;
import de.fu_berlin.inf.dpp.observables.FileReplacementInProgressObservable;
 import de.fu_berlin.inf.dpp.observables.SarosSessionObservable;
 import de.fu_berlin.inf.dpp.preferences.PreferenceUtils;
 import de.fu_berlin.inf.dpp.project.IChecksumCache;
 import de.fu_berlin.inf.dpp.project.ISarosSession;
 import de.fu_berlin.inf.dpp.ui.RemoteProgressManager;
 import de.fu_berlin.inf.dpp.ui.wizards.AddProjectToSessionWizard;
 import de.fu_berlin.inf.dpp.ui.wizards.pages.EnterProjectNamePage;
 import de.fu_berlin.inf.dpp.util.ArrayUtils;
 import de.fu_berlin.inf.dpp.util.FileUtils;
 import de.fu_berlin.inf.dpp.util.Utils;
 import de.fu_berlin.inf.dpp.vcs.VCSAdapter;
 import de.fu_berlin.inf.dpp.vcs.VCSResourceInfo;
 
 public class IncomingProjectNegotiation extends ProjectNegotiation {
 
     private static Logger log = Logger
         .getLogger(IncomingProjectNegotiation.class);
 
     protected SubMonitor monitor;
     protected AtomicBoolean cancelled = new AtomicBoolean(false);
     protected SarosCancellationException cancellationCause;
     protected AddProjectToSessionWizard addIncomingProjectUI;
 
     protected List<ProjectExchangeInfo> projectInfos;
 
     @Inject
     protected PreferenceUtils preferenceUtils;
     @Inject
     protected SarosSessionObservable sarosSessionObservable;
 
     @Inject
     protected RemoteProgressManager rpm;
 
     @Inject
     protected IChecksumCache checksumCache;
 
    @Inject
    protected FileReplacementInProgressObservable fileReplacementInProgressObservable;
     /**
      * maps the projectID to the project in workspace
      */
     Map<String, IProject> localProjects;
 
     protected JID jid;
 
     public IncomingProjectNegotiation(JID peer, String processID,
         List<ProjectExchangeInfo> projectInfos, SarosContext sarosContext) {
         super(peer, sarosContext);
 
         this.processID = processID;
         this.projectInfos = projectInfos;
         this.localProjects = new HashMap<String, IProject>();
         this.jid = peer;
 
     }
 
     @Override
     public Map<String, String> getProjectNames() {
         Map<String, String> result = new HashMap<String, String>();
         for (ProjectExchangeInfo info : this.projectInfos) {
             result.put(info.getProjectID(), info.getProjectName());
         }
         return result;
     }
 
     /**
      * 
      * @param projectID
      * @return The {@link FileList fileList} which belongs to the project with
      *         the ID <code>projectID</code> from inviter <br />
      *         <code><b>null<b></code> if there isn't such a {@link FileList
      *         fileList}
      */
     public FileList getRemoteFileList(String projectID) {
         for (ProjectExchangeInfo info : this.projectInfos) {
             if (info.getProjectID().equals(projectID))
                 return info.getFileList();
         }
         return null;
     }
 
     public void setProjectInvitationUI(
         AddProjectToSessionWizard addIncomingProjectUI) {
         this.addIncomingProjectUI = addIncomingProjectUI;
     }
 
     /**
      * 
      * @param projectNames
      *            In this parameter the names of the projects are stored. They
      *            key is the session wide <code><b>projectID</b></code> and the
      *            value is the name of the project in the workspace of the
      *            invited buddy (given from the {@link EnterProjectNamePage})
      */
     public void accept(Map<String, String> projectNames, SubMonitor subMonitor,
         Map<String, Boolean> skipSyncs, boolean useVersionControl)
         throws SarosCancellationException {
 
         this.monitor = subMonitor;
         IWorkspace ws = ResourcesPlugin.getWorkspace();
         IWorkspaceDescription desc = ws.getDescription();
         boolean wasAutobuilding = desc.isAutoBuilding();
 
         subMonitor.beginTask("Initializing shared project", 100);
        fileReplacementInProgressObservable.startReplacement();
         try {
             if (wasAutobuilding) {
                 desc.setAutoBuilding(false);
                 ws.setDescription(desc);
             }
 
             List<FileList> missingFiles = calculateMissingFiles(projectNames,
                 skipSyncs, useVersionControl, subMonitor.newChild(10));
 
             // the user who sends this ProjectNegotiation is now responsible for
             // all resources from that project
             for (Entry<String, IProject> entry : localProjects.entrySet()) {
                 sessionManager.getSarosSession().addProjectOwnership(
                     entry.getKey(), entry.getValue(), jid);
             }
 
             transmitter.sendFileLists(peer, processID, missingFiles,
                 subMonitor.newChild(10));
             checkCancellation();
 
             boolean filesMissing = false;
 
             for (FileList list : missingFiles)
                 filesMissing |= list.getPaths().size() > 0;
 
             // Host/Inviter decided to transmit files with one big archive
             if (filesMissing)
                 acceptArchive(localProjects.size(), subMonitor.newChild(80));
 
             // We are finished with the exchanging process. Add all projects
             // resources to the session.
             for (String projectID : localProjects.keySet()) {
                 IProject iProject = localProjects.get(projectID);
                 if (isPartialRemoteProject(projectID)) {
                     List<IPath> paths = getRemoteFileList(projectID).getPaths();
                     List<IResource> dependentResources = new ArrayList<IResource>();
                     for (IPath iPath : paths) {
                         dependentResources.add(iProject.findMember(iPath));
                     }
                     sessionManager.getSarosSession().addSharedResources(
                         iProject, projectID, dependentResources);
                 } else {
                     sessionManager.getSarosSession().addSharedResources(
                         iProject, projectID, null);
                 }
 
                 sessionManager.notifyProjectAdded(iProject);
             }
         } catch (Exception e) {
             processException(e);
         } finally {
            fileReplacementInProgressObservable.replacementDone();
             subMonitor.done();
 
             // Re-enable auto-building...
             if (wasAutobuilding) {
                 desc.setAutoBuilding(true);
                 try {
                     ws.setDescription(desc);
                 } catch (CoreException e) {
                     localCancel(
                         "An error occurred while synchronising the project",
                         CancelOption.NOTIFY_PEER);
                 }
             }
             if (this.projectExchangeProcesses.getProcesses()
                 .containsValue(this))
                 this.projectExchangeProcesses
                     .removeProjectExchangeProcess(this);
         }
     }
 
     public boolean isPartialRemoteProject(String projectID) {
         for (ProjectExchangeInfo info : this.projectInfos) {
             if (info.getProjectID().equals(projectID))
                 return info.isPartial();
         }
         return false;
     }
 
     /**
      * The archive with all missing files sorted by project will be received and
      * unpacked project by project.
      * 
      * @param numberOfLoops
      *            how many projects will be in the big archive
      */
     private void acceptArchive(int numberOfLoops, SubMonitor submonitor)
         throws IOException, SarosCancellationException, CoreException {
 
         // waiting for the big archive to come in
 
         submonitor.beginTask(null, 100);
         submonitor.subTask("Receiving archive");
 
         InputStream archiveStream = transmitter.receiveArchive(processID,
             getPeer(), submonitor.newChild(70), false);
         checkCancellation();
 
         ZipInputStream zipStream = new ZipInputStream(archiveStream);
         ZipEntry zipEntry;
         // unpacking the big archive
         SubMonitor zipStreamLoopMonitor = submonitor.newChild(30);
         while ((zipEntry = zipStream.getNextEntry()) != null) {
             // Every zipEntry is (again) a ZipArchive, which contains all
             // missing files for one project.
             SubMonitor lMonitor = zipStreamLoopMonitor
                 .newChild(100 / numberOfLoops);
             /*
              * For every entry (which is a zipArchive for a single project) we
              * have to find out which project it is meant for. So we need the
              * projectID.
              * 
              * The archive name contains the projectID.
              * 
              * archiveName = projectID + this.projectIDDelimiter + randomNumber
              * + '.zip'
              */
             String projectID = zipEntry.getName().substring(0,
                 zipEntry.getName().indexOf(this.projectIDDelimiter));
             IProject project = localProjects.get(projectID);
             IPath path = Path.fromPortableString(zipEntry.getName());
             // store the archive temporary
             IFile file = project.getFile(path);
             // Archive needs to be stored temporarily to be accessed
             FileUtils.writeFile(new FilterInputStream(zipStream) {
                 @Override
                 public void close() throws IOException {
                     // prevent the ZipInputStream from being closed
                 }
             }, file, lMonitor.newChild(10));
 
             InputStream inputStream = file.getContents();
             log.debug(file.getProjectRelativePath().toString());
             // write all files in archive to project
             writeArchive(inputStream, project, lMonitor.newChild(80));
             zipStream.closeEntry();
             file.delete(true, false, lMonitor.newChild(10));
         }
         submonitor.done();
     }
 
     /**
      * calculates all the files the host/inviter has to send for synchronization
      * 
      * @param projectNames
      *            projectID => projectName (in local workspace)
      */
     private List<FileList> calculateMissingFiles(
         Map<String, String> projectNames, Map<String, Boolean> skipSyncs,
         boolean useVersionControl, SubMonitor subMonitor)
         throws SarosCancellationException, IOException {
 
         subMonitor.beginTask(null, 100);
         int numberOfLoops = projectNames.size();
         List<FileList> missingFiles = new ArrayList<FileList>();
 
         /*
          * this for loop sets up all the projects needed for the session and
          * computes the missing files.
          */
         for (Entry<String, String> entry : projectNames.entrySet()) {
             SubMonitor lMonitor = subMonitor.newChild(100 / numberOfLoops);
             String projectID = entry.getKey();
             String projectName = entry.getValue();
             checkCancellation();
             ProjectExchangeInfo projectInfo = null;
             for (ProjectExchangeInfo pInfo : this.projectInfos) {
                 if (pInfo.getProjectID().equals(projectID))
                     projectInfo = pInfo;
             }
             if (projectInfo == null) {
                 log.error("tried to add a project that wasn't shared");
                 // this should never happen
                 continue;
             }
 
             VCSAdapter vcs = null;
             if (preferenceUtils.useVersionControl() && useVersionControl) {
                 vcs = VCSAdapter.getAdapter(projectInfo.getFileList()
                     .getVcsProviderID());
             }
 
             IProject iProject = ResourcesPlugin.getWorkspace().getRoot()
                 .getProject(projectName);
             if (iProject.exists()) {
                 /*
                  * Saving unsaved files is supposed to be done in
                  * JoinSessionWizard#performFinish().
                  */
                 if (EditorAPI.existUnsavedFiles(iProject)) {
                     log.error("Unsaved files detected.");
                 }
             } else {
                 iProject = null;
             }
             IProject localProject = assignLocalProject(iProject, projectName,
                 projectID, vcs, lMonitor.newChild(30), projectInfo);
             localProjects.put(projectID, localProject);
 
             checkCancellation();
             if (vcs != null && !isPartialRemoteProject(projectID)) {
                 log.debug("initVcState");
                 initVcState(localProject, vcs, lMonitor.newChild(40),
                     projectInfo.getFileList());
             }
             checkCancellation();
 
             log.debug("compute required Files for project " + projectName
                 + " with ID: " + projectID);
             FileList requiredFiles = computeRequiredFiles(localProject,
                 projectInfo.getFileList(), projectID, skipSyncs.get(projectID)
                     .booleanValue(), vcs, lMonitor.newChild(30));
             requiredFiles.setProjectID(projectID);
             checkCancellation();
             missingFiles.add(requiredFiles);
             lMonitor.done();
         }
         return missingFiles;
     }
 
     /**
      * In this method the project we want to have in the session is initialized.
      * If the baseProject is not null we use it as the base to create the
      * 
      * @param projectID
      * 
      * @param projectInfo
      * 
      * @throws LocalCancellationException
      */
     private IProject assignLocalProject(final IProject baseProject,
         final String newProjectName, String projectID, final VCSAdapter vcs,
         final SubMonitor monitor, ProjectExchangeInfo projectInfo)
         throws LocalCancellationException {
         IProject newLocalProject = baseProject;
         FileList remoteFileList = projectInfo.getFileList();
         // if the baseProject already exists
         if (newLocalProject != null) {
             if (newLocalProject.getName().equals(newProjectName)) {
                 // TODO the project could be managed by a different Team
                 // provider
                 if (vcs != null && !vcs.isManaged(newLocalProject)
                     && !projectInfo.isPartial()) {
                     String repositoryRoot = remoteFileList.getRepositoryRoot();
                     final String url = remoteFileList.getProjectInfo().url;
                     String directory = url.substring(repositoryRoot.length());
                     vcs.connect(newLocalProject, repositoryRoot, directory,
                         monitor);
                 }
             }
             return newLocalProject;
         }
 
         if (vcs != null) {
             if (!isPartialRemoteProject(projectID)) {
                 try {
                     /*
                      * Inform the host of the session that the current (local)
                      * user has started the possibly time consuming SVN checkout
                      * via a remoteProgressMonitor
                      */
                     ISarosSession sarosSession = sarosSessionObservable
                         .getValue();
                     List<User> notifiedUsers = new ArrayList<User>();
                     if (sarosSession != null) {
                         notifiedUsers.add(sarosSession.getHost());
                         /*
                          * The monitor that is created here is shown both
                          * locally and remote and is handled like a regular
                          * progress monitor.
                          */
                         IProgressMonitor remoteMonitor = rpm
                             .mirrorLocalProgressMonitorToRemote(sarosSession,
                                 notifiedUsers, monitor);
                         remoteMonitor
                             .setTaskName("Project checkout via subversion");
                         newLocalProject = vcs.checkoutProject(newProjectName,
                             remoteFileList, remoteMonitor);
                     } else {
                         log.error("No Saros session!");
                     }
                 } catch (org.eclipse.core.runtime.OperationCanceledException e) {
                     /*
                      * The exception is thrown if the user canceled the svn
                      * checkout process. We send the remote user a sophisticated
                      * message here.
                      */
                     throw new LocalCancellationException(
                         "The CVS checkout process was canceled",
                         CancelOption.NOTIFY_PEER);
                 }
             }
 
             /*
              * HACK: After checking out a project, give Eclipse/the Team
              * provider time to realize that the project is now managed. The
              * problem was that when checking later to see if we have to
              * switch/update individual resources in initVcState, the project
              * appeared as unmanaged. It might work to wrap initVcState in a
              * job, such that it is scheduled after the project is marked as
              * managed.
              */
             try {
                 Thread.sleep(1000);
             } catch (InterruptedException e) {
                 // do nothing
             }
             if (newLocalProject != null)
                 return newLocalProject;
         }
 
         try {
             newLocalProject = Utils.runSWTSync(new Callable<IProject>() {
                 public IProject call() throws CoreException,
                     InterruptedException {
                     try {
                         return createNewProject(newProjectName, baseProject);
                     } catch (Exception e) {
                         throw new RuntimeException(e.getMessage());
                     }
                 }
             });
         } catch (Exception e) {
             throw new LocalCancellationException(e.getMessage(),
                 CancelOption.NOTIFY_PEER);
         }
         return newLocalProject;
     }
 
     /**
      * Checks whether the invitation process or the monitor has been cancelled.
      * If the monitor has been cancelled but the invitation process has not yet,
      * it cancels the invitation process.
      * 
      * @throws SarosCancellationException
      *             if the invitation process or the monitor has already been
      *             cancelled.
      */
     protected void checkCancellation() throws SarosCancellationException {
         if (cancelled.get()) {
             log.debug("Inv" + Utils.prefix(peer) + ": Cancellation checkpoint");
             throw new SarosCancellationException();
         }
 
         if (monitor == null)
             return;
 
         if (monitor.isCanceled()) {
             log.debug("Inv" + Utils.prefix(peer) + ": Cancellation checkpoint");
             localCancel(null, CancelOption.NOTIFY_PEER);
             throw new SarosCancellationException();
         }
 
         return;
     }
 
     protected void processException(Exception e)
         throws SarosCancellationException {
         if (e instanceof LocalCancellationException) {
             localCancel(e.getMessage(), CancelOption.NOTIFY_PEER);
         } else if (e instanceof RemoteCancellationException) {
             remoteCancel(e.getMessage());
         } else if (e instanceof SarosCancellationException) {
             /**
              * If this exception is thrown because of a local cancellation, we
              * initiate a localCancel here.
              * 
              * If this exception is thrown because of a remote cancellation, the
              * call of localCancel will be ignored.
              */
             localCancel(e.getMessage(), CancelOption.NOTIFY_PEER);
         } else if (e instanceof IOException) {
             String errorMsg = MessageFormat.format("Unknown error: {0}", e);
             if (e.getMessage() != null)
                 errorMsg = e.getMessage();
             localCancel(errorMsg, CancelOption.NOTIFY_PEER);
         } else {
             log.warn("Inv" + Utils.prefix(peer)
                 + ": This type of Exception is not expected: ", e);
             String errorMsg = MessageFormat.format("Unknown error: {0}", e);
             if (e.getMessage() != null)
                 errorMsg = e.getMessage();
             localCancel(errorMsg, CancelOption.NOTIFY_PEER);
         }
         executeCancellation();
     }
 
     protected void executeCancellation() throws SarosCancellationException {
 
         log.debug("Inv" + Utils.prefix(peer) + ": executeCancellation");
         if (!cancelled.get())
             throw new IllegalStateException(
                 "executeCancellation should only be called after localCancel or remoteCancel!");
 
         /*
          * Remove the entries from the mapping in the SarosSession.
          */
         for (Entry<String, IProject> entry : localProjects.entrySet()) {
             sessionManager.getSarosSession().removeProjectOwnership(
                 entry.getKey(), entry.getValue(), jid);
         }
 
         String errorMsg;
         String cancelMessage;
         if (cancellationCause instanceof LocalCancellationException) {
             LocalCancellationException e = (LocalCancellationException) cancellationCause;
             errorMsg = e.getMessage();
 
             switch (e.getCancelOption()) {
             case NOTIFY_PEER:
                 transmitter.sendCancelSharingProjectMessage(peer, errorMsg);
                 break;
             case DO_NOT_NOTIFY_PEER:
                 break;
             default:
                 log.warn("Inv" + Utils.prefix(peer)
                     + ": This case is not expected here.");
             }
 
             if (errorMsg != null) {
                 cancelMessage = "Sharing project was cancelled locally"
                     + " because of an error: " + errorMsg;
                 log.error("Inv" + Utils.prefix(peer) + ": " + cancelMessage);
             } else {
                 cancelMessage = "Sharing project was cancelled by local user.";
                 log.debug("Inv" + Utils.prefix(peer) + ": " + cancelMessage);
             }
 
         } else if (cancellationCause instanceof RemoteCancellationException) {
             RemoteCancellationException e = (RemoteCancellationException) cancellationCause;
             errorMsg = e.getMessage();
             if (errorMsg != null) {
                 cancelMessage = MessageFormat
                     .format(
                         "Sharing project was cancelled by the remote user because of an error on his/her side: {0}",
                         errorMsg);
                 log.error("Inv" + Utils.prefix(peer) + ": " + cancelMessage);
             } else {
                 cancelMessage = "Sharing project was cancelled by the remote user.";
                 log.debug("Inv" + Utils.prefix(peer) + ": " + cancelMessage);
             }
         } else {
             log.error("This type of exception is not expected here: ",
                 cancellationCause);
         }
 
         // The session might have been stopped already, if not we will stop it.
         ISarosSession session = sessionManager.getSarosSession();
         if (session != null) {
             if (session.getProjectResourcesMapping().keySet().isEmpty()
                 || session.getRemoteUsers().isEmpty())
                 sessionManager.stopSarosSession();
         }
 
         projectExchangeProcesses.removeProjectExchangeProcess(this);
         throw cancellationCause;
     }
 
     /**
      * This method does <strong>not</strong> execute the cancellation but only
      * sets the {@link #cancellationCause}. It should be called if the
      * cancellation was initated by the <strong>local</strong> user. The
      * cancellation will be ignored if the invitation has already been cancelled
      * before. <br>
      * In order to cancel the invitation process {@link #executeCancellation()}
      * should be called.
      * 
      * @param errorMsg
      *            the error that caused the cancellation. This should be some
      *            user-friendly text as it might be presented to the user.
      *            <code>null</code> if the cancellation was caused by the user's
      *            request and not by some error.
      * 
      * @param cancelOption
      *            If <code>NOTIFY_PEER</code> we send a cancellation message to
      *            our peer.
      */
     public void localCancel(String errorMsg, CancelOption cancelOption) {
         if (!cancelled.compareAndSet(false, true))
             return;
         log.debug("Inv" + Utils.prefix(peer) + ": localCancel: " + errorMsg);
         if (monitor != null)
             monitor.setCanceled(true);
         cancellationCause = new LocalCancellationException(errorMsg,
             cancelOption);
         if (monitor == null) {
             log.debug("Inv" + Utils.prefix(peer)
                 + ": Closing JoinSessionWizard manually.");
             try {
                 executeCancellation();
             } catch (SarosCancellationException e) {
                 /**
                  * This happens if the JoinSessionWizard is currently waiting
                  * for user input.
                  */
                 if (addIncomingProjectUI != null)
                     addIncomingProjectUI.cancelWizard(peer, e.getMessage(),
                         CancelLocation.LOCAL);
                 else
                     log.error("The inInvitationUI is null, could not"
                         + " close the JoinSessionWizard.");
             }
         }
     }
 
     @Override
     public void remoteCancel(String errorMsg) {
         if (!cancelled.compareAndSet(false, true))
             return;
         log.debug("Inv"
             + Utils.prefix(peer)
             + ": remoteCancel "
             + (errorMsg == null ? " by user" : " because of error: " + errorMsg));
         if (monitor != null)
             monitor.setCanceled(true);
         cancellationCause = new RemoteCancellationException(errorMsg);
         if (monitor == null) {
             log.debug("Inv" + Utils.prefix(peer)
                 + ": Closing JoinSessionWizard manually.");
             try {
                 executeCancellation();
             } catch (SarosCancellationException e) {
                 /**
                  * This happens if the JoinSessionWizard is currently waiting
                  * for user input.
                  */
                 if (addIncomingProjectUI != null)
                     addIncomingProjectUI.cancelWizard(peer, e.getMessage(),
                         CancelLocation.REMOTE);
                 else
                     log.error("The inInvitationUI is null, could not"
                         + " close the JoinSessionWizard.");
             }
         }
     }
 
     /**
      * Creates a new project.
      * 
      * @param newProjectName
      *            the project name of the new project.
      * @param baseProject
      *            if not <code>null</code> all files of the baseProject will be
      *            copied into the new project after having created it.
      * @return the new project.
      * @throws Exception
      * 
      * @swt Needs to be run from the SWT UI Thread
      */
     protected IProject createNewProject(String newProjectName,
         final IProject baseProject) throws Exception {
 
         log.debug("Inv" + Utils.prefix(peer) + ": Creating new project...");
         IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
         final IProject project = workspaceRoot.getProject(newProjectName);
 
         final File projectDir = new File(
             workspaceRoot.getLocation().toString(), newProjectName);
 
         if (projectDir.exists()) {
             throw new CoreException(new Status(IStatus.ERROR, Saros.SAROS,
                 MessageFormat.format("Project {0} already exists!",
                     newProjectName)));
         }
 
         ProgressMonitorDialog dialog = new ProgressMonitorDialog(EditorAPI
             .getAWorkbenchWindow().getShell());
 
         dialog.run(true, true, new IRunnableWithProgress() {
             public void run(IProgressMonitor monitor)
                 throws InvocationTargetException {
                 try {
                     SubMonitor subMonitor = SubMonitor.convert(monitor,
                         "Copy local resources... ", 300);
 
                     subMonitor.subTask("Clearing History...");
                     project.clearHistory(subMonitor.newChild(100));
 
                     subMonitor.subTask("Refreshing Project");
                     project.refreshLocal(IResource.DEPTH_INFINITE,
                         subMonitor.newChild(100));
 
                     if (baseProject == null) {
                         subMonitor.subTask("Creating Project...");
                         project.create(subMonitor.newChild(50));
 
                         subMonitor.subTask("Opening Project...");
                         project.open(subMonitor.newChild(50));
                     } else {
                         subMonitor.subTask("Copying Project...");
                         baseProject.copy(project.getFullPath(), true,
                             subMonitor.newChild(100));
                     }
                 } catch (Exception e) {
                     throw new RuntimeException(e.getCause());
                 }
             }
         });
         return project;
     }
 
     /**
      * Computes the list of files that we're going to request from the host.<br>
      * If a VCS is used, update files if needed, and remove them from the list
      * of requested files if that's possible.
      * 
      * @param currentLocalProject
      * @param remoteFileList
      * @param skipSync
      *            Skip the initial synchronization.
      * @param vcs
      *            The VCS adapter of the local project.
      * @param monitor
      *            The SubMonitor of the dialog.
      * 
      * @return The list of files that we need from the host.
      * @throws LocalCancellationException
      *             If the user requested a cancel.
      * @throws IOException
      */
     private FileList computeRequiredFiles(IProject currentLocalProject,
         FileList remoteFileList, String projectID, boolean skipSync,
         VCSAdapter vcs, SubMonitor monitor) throws LocalCancellationException,
         IOException {
         monitor.beginTask("Compute required Files...", 100);
 
         if (skipSync)
             return FileListFactory.createEmptyFileList();
 
         FileListDiff filesToSynchronize = null;
         FileList localFileList = null;
         try {
             localFileList = FileListFactory.createFileList(currentLocalProject,
                 null, checksumCache, vcs != null, monitor.newChild(1));
         } catch (CoreException e) {
             e.printStackTrace();
             return FileListFactory.createEmptyFileList();
         }
         SubMonitor childMonitor = monitor.newChild(5,
             SubMonitor.SUPPRESS_ALL_LABELS);
         filesToSynchronize = computeDiff(localFileList, remoteFileList,
             currentLocalProject, projectID, childMonitor);
 
         List<IPath> missingFiles = filesToSynchronize.getAddedPaths();
         missingFiles.addAll(filesToSynchronize.getAlteredPaths());
         if (missingFiles.isEmpty()) {
             log.debug("Inv" + Utils.prefix(peer)
                 + ": There are no files to synchronize.");
             /**
              * We send an empty file list to the host as a notification that we
              * do not need any files.
              */
             return FileListFactory.createEmptyFileList();
         }
 
         return FileListFactory.createPathFileList(missingFiles);
     }
 
     /**
      * Determines the missing resources.
      * 
      * @param localFileList
      *            The file list of the local project.
      * @param remoteFileList
      *            The file list of the remote project.
      * @param currentLocalProject
      *            The project in workspace. Every file we need to add/replace is
      *            added to the {@link FileListDiff}
      * @param projectID
      * @param monitor
      *            The progress monitor of the dialog.
      * @return A modified FileListDiff which doesn't contain any directories or
      *         files to remove, but just added and altered files.
      * @throws LocalCancellationException
      *             If the process is canceled by the user.
      */
     protected FileListDiff computeDiff(FileList localFileList,
         FileList remoteFileList, IProject currentLocalProject,
         String projectID, SubMonitor monitor) throws LocalCancellationException {
         log.debug("Inv" + Utils.prefix(peer) + ": Computing file list diff...");
 
         monitor.beginTask(null, 100);
 
         try {
             monitor.subTask("Calculating Diff");
             FileListDiff diff = FileListDiff
                 .diff(localFileList, remoteFileList);
             monitor.worked(20);
 
             monitor.subTask("Removing unneeded resources");
             if (!isPartialRemoteProject(projectID))
                 diff = diff.removeUnneededResources(currentLocalProject,
                     monitor.newChild(40, SubMonitor.SUPPRESS_ALL_LABELS));
 
             monitor.subTask("Adding Folders");
             diff = diff.addAllFolders(currentLocalProject,
                 monitor.newChild(40, SubMonitor.SUPPRESS_ALL_LABELS));
 
             return diff;
         } catch (CoreException e) {
             throw new LocalCancellationException(MessageFormat.format(
                 "Could not create diff file list: {0}", e.getMessage()),
                 CancelOption.NOTIFY_PEER);
         } finally {
             monitor.done();
         }
     }
 
     /**
      * Have a look at the description of {@link WorkspaceModifyOperation}!
      * 
      * @throws LocalCancellationException
      * 
      * @see WorkspaceModifyOperation
      */
     private void writeArchive(final InputStream archiveStream,
         final IProject project, final SubMonitor subMonitor)
         throws LocalCancellationException {
 
         log.debug("Inv" + Utils.prefix(peer) + ": Writing archive to disk...");
         try {
             ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
                 public void run(IProgressMonitor monitor) throws CoreException {
                     try {
                         FileUtils.writeArchive(archiveStream, project,
                             subMonitor, sessionManager.getSarosSession());
                     } catch (LocalCancellationException e) {
                         throw new CoreException(new Status(IStatus.CANCEL,
                             Saros.SAROS, null, e));
                     }
                 }
             }, subMonitor);
         } catch (CoreException e) {
             try {
                 throw e.getCause();
             } catch (LocalCancellationException lc) {
                 throw lc;
             } catch (Throwable t) {
                 throw new LocalCancellationException(
                     "An error occurred while writing the archive: "
                         + t.getMessage(), CancelOption.NOTIFY_PEER);
             }
         }
     }
 
     @Override
     public String getProcessID() {
         return processID;
     }
 
     /**
      * Recursively synchronizes the version control state (URL and revision) of
      * each resource in the project with the host by switching or updating when
      * necessary.<br>
      * <br>
      * It's very hard to predict how many resources have to be changed. In the
      * worst case, every resource has to be changed as many times as the number
      * of segments in its path. Due to these complications, the monitor is only
      * used for cancellation and the label, but not for the progress bar.
      * 
      * @param remoteFileList
      * 
      * @throws SarosCancellationException
      */
     private void initVcState(IResource resource, VCSAdapter vcs,
         SubMonitor monitor, FileList remoteFileList)
         throws SarosCancellationException {
         if (monitor.isCanceled())
             return;
 
         if (!vcs.isManaged(resource))
             return;
 
         final VCSResourceInfo info = vcs.getCurrentResourceInfo(resource);
         final IPath path = resource.getProjectRelativePath();
         if (resource instanceof IProject) {
             /*
              * We have to revert the project first because the invitee could
              * have deleted a managed resource. Also, we don't want an update or
              * switch to cause an unresolved conflict here. The revert might
              * leave some unmanaged files, but these will get cleaned up later;
              * we're only concerned with managed files here.
              */
             vcs.revert(resource, monitor);
         }
 
         String url = remoteFileList.getVCSUrl(path);
         String revision = remoteFileList.getVCSRevision(path);
         List<IPath> paths = remoteFileList.getPaths();
         if (url == null || revision == null) {
             // The resource might have been deleted.
             return;
         }
         if (!info.url.equals(url)) {
             log.trace("Switching " + resource.getName() + " from " + info.url
                 + " to " + url);
             vcs.switch_(resource, url, revision, monitor);
         } else if (!info.revision.equals(revision) && paths.contains(path)) {
             log.trace("Updating " + resource.getName() + " from "
                 + info.revision + " to " + revision);
             vcs.update(resource, revision, monitor);
         }
         if (monitor.isCanceled())
             return;
 
         if (resource instanceof IContainer) {
             // Recurse.
             try {
                 List<IResource> children = ArrayUtils.getAdaptableObjects(
                     ((IContainer) resource).members(), IResource.class);
                 for (IResource child : children) {
                     if (remoteFileList.getPaths().contains(child.getFullPath()))
                         initVcState(child, vcs, monitor, remoteFileList);
                     if (monitor.isCanceled())
                         break;
                 }
             } catch (CoreException e) {
                 /*
                  * We shouldn't ever get here. CoreExceptions are thrown e.g. if
                  * the project is closed or the resource doesn't exist, both of
                  * which are impossible at this point.
                  */
                 log.error("Unknown error while trying to initialize the "
                     + "children of " + resource.toString() + ".", e);
                 localCancel(
                     "Could not initialize the project's version control state, "
                         + "please try again without VCS support.",
                     CancelOption.NOTIFY_PEER);
                 executeCancellation();
             }
         }
     }
 
     public List<ProjectExchangeInfo> getProjectInfos() {
         return projectInfos;
     }
 }
