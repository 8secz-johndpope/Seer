 package org.eclipse.jdt.internal.debug.ui.launcher;
 
 /*
  * (c) Copyright IBM Corp. 2000, 2001.
  * All Rights Reserved.
  */
 
 import java.lang.reflect.InvocationTargetException;
 
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.resources.IncrementalProjectBuilder;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IAdaptable;
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.core.runtime.SubProgressMonitor;
 import org.eclipse.debug.core.DebugPlugin;
 import org.eclipse.debug.core.ILaunch;
 import org.eclipse.debug.core.ILauncher;
 import org.eclipse.debug.core.Launch;
 import org.eclipse.debug.core.model.ILauncherDelegate;
 import org.eclipse.debug.core.model.ISourceLocator;
 import org.eclipse.debug.internal.ui.LaunchWizard;
 import org.eclipse.debug.internal.ui.LaunchWizardDialog;
 import org.eclipse.jdt.core.IJavaElement;
 import org.eclipse.jdt.core.IJavaProject;
 import org.eclipse.jdt.core.IType;
 import org.eclipse.jdt.core.JavaCore;
 import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
 import org.eclipse.jdt.internal.debug.ui.JavaApplicationWizard;
 import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
 import org.eclipse.jdt.internal.ui.util.SWTUtil;
 import org.eclipse.jdt.launching.ExecutionArguments;
 import org.eclipse.jdt.launching.IVMInstall;
 import org.eclipse.jdt.launching.IVMRunner;
 import org.eclipse.jdt.launching.JavaRuntime;
 import org.eclipse.jdt.launching.VMRunnerConfiguration;
 import org.eclipse.jdt.launching.VMRunnerResult;
 import org.eclipse.jface.dialogs.ProgressMonitorDialog;
 import org.eclipse.jface.operation.IRunnableWithProgress;
 import org.eclipse.jface.viewers.IStructuredSelection;
 import org.eclipse.jface.viewers.StructuredSelection;
 import org.eclipse.jface.wizard.WizardDialog;
 import org.eclipse.swt.widgets.Shell;
 
 /**
  * A launcher for running Java main classes. 
  */
 public class JavaApplicationLauncherDelegate implements ILauncherDelegate {
 				
 	/**
 	 * Creates a new launcher
 	 */
 	public JavaApplicationLauncherDelegate() {
 	}
 		
 	protected boolean launchElement(Object runnable, String mode, ILauncher launcher) {
 		
 		if (!(runnable instanceof IType)) {
 			return useLaunchWizard(mode, launcher, new Object[] {runnable});
 		}
 		
 		IType mainType= (IType) runnable;
 
 		IJavaProject javaProject= mainType.getJavaProject();
 		ExecutionArguments args= null;
 		String[] classPath= null;
 		try {
 			classPath= JavaRuntime.computeDefaultRuntimeClassPath(javaProject);
 			args= ExecutionArguments.getArguments(mainType);
 		} catch (CoreException e) {
 			JDIDebugUIPlugin.log(e.getStatus());
 			return false;
 		}
 		return doLaunch(javaProject, mode, mainType, args, classPath, launcher);
 	}
 	
 	/**
 	 *	@see ILauncherDelegate#launch(Object[], String, ILauncher)
 	 */
 	public boolean launch(Object[] objects, String mode, ILauncher launcher) {
 		IStructuredSelection selection= new StructuredSelection(objects);
 		Object[] elements= getLaunchableElements(selection, mode);
 		if (elements.length == 0) {
 			return useLaunchWizard(mode, launcher, objects);
 		} else if (elements.length == 1) {
 			return launchElement(elements[0], mode, launcher);
 		} else {
 			return useWizard(elements, selection, mode, launcher);
 		}
 	}	
 	
 	/**
 	 * Use the wizard to do the launch.
 	 */
 	private boolean useWizard(Object[] elements, IStructuredSelection selection, String mode, ILauncher launcher) {
 		JavaApplicationWizard w= new JavaApplicationWizard(elements);
 		w.init(launcher, mode, selection);
 		WizardDialog dialog= new WizardDialog(JDIDebugUIPlugin.getActiveWorkbenchShell(), w);
 		dialog.open();
 		return true;
 	}
 	
 	private boolean useLaunchWizard(String mode, ILauncher launcher, Object[] objects) {
 		IProject project= findProject(objects);
 		LaunchWizard lw= new LaunchWizard(new Object[] { launcher }, null, mode, project, launcher);
 		Shell shell = JDIDebugUIPlugin.getActiveWorkbenchShell();
 		LaunchWizardDialog dialog= new LaunchWizardDialog(shell, lw);
 		dialog.open();
 		return true;
 	}
 	
 	private IProject findProject(Object[] objects) {
 		for (int i = 0; i < objects.length; i++) {
 			Object object = objects[i];
 			if (object instanceof IAdaptable) {
 				IResource resource = (IResource) ((IAdaptable)object).getAdapter(IResource.class);
 				if (resource != null) {
 					return resource.getProject();				
 				}
 			}
 		}
 		return null;
 	}	
 	
 	private boolean doLaunch(final IJavaProject jproject, final String mode, final IType mainType, ExecutionArguments args, String[] classPath, final ILauncher launcherProxy) {
 		try {
 			final IVMRunner runner= getVMRunner(jproject, mode);
 			if (runner == null) {
 				return false;
 			}
 			
 			final VMRunnerConfiguration config= new VMRunnerConfiguration(JavaModelUtil.getFullyQualifiedName(mainType), classPath);
 
 			if (args != null) {
 				String[] vmArgs= args.getVMArgumentsArray();
 				String[] programArgs= args.getProgramArgumentsArray();
 				config.setVMArguments(vmArgs);
 				config.setProgramArguments(programArgs);				
 			} else {
 				config.setVMArguments(new String[0]);
 				config.setProgramArguments(new String[0]);
 			}
 			
 			final VMRunnerResult[] result= new VMRunnerResult[1];
 			
 			IRunnableWithProgress r= new IRunnableWithProgress() {
 				public void run(IProgressMonitor pm) throws InvocationTargetException {
 					pm.beginTask(LauncherMessages.getString("javaAppLauncher.progress.startVM"), 4); //$NON-NLS-1$
 					try {
 						IProject proj= jproject.getProject();
 						if (!proj.getWorkspace().isAutoBuilding()) {
 							try {
 								proj.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, new SubProgressMonitor(pm, 3));
 							} catch (CoreException e) {
 								throw new InvocationTargetException(e);
 							}
 						} else {
 							pm.worked(3);
 						}
 						SubProgressMonitor newMonitor= new SubProgressMonitor(pm, 1);
 						newMonitor.beginTask(LauncherMessages.getString("javaAppLauncher.progress.startVM"), IProgressMonitor.UNKNOWN); //$NON-NLS-1$
 						
 						result[0]= runner.run(config);
 						newMonitor.done();
 					} catch (CoreException e) {
 						throw new InvocationTargetException(e);
 					} finally {
 						pm.done();
 					}
 				}
 			};
 			
 			try {
 				new ProgressMonitorDialog(JDIDebugUIPlugin.getActiveWorkbenchShell()).run(true, false, r);		
 			} catch (InterruptedException e) {
 				return true;
 			} catch (InvocationTargetException e) {
				ExceptionHandler.handle(e, JDIDebugUIPlugin.getActiveWorkbenchShell(), LauncherMessages.getString("JavaApplicationLauncherDelegate.Java_Application_Launcher_1"), LauncherMessages.getString("JavaApplicationLauncherDelegate.Launching_failed._2")); //$NON-NLS-2$ //$NON-NLS-1$
 				return false;
 			}
 			if (result[0] != null) {
 				ISourceLocator sourceLocator= new JavaUISourceLocator(jproject);
 				Launch newLaunch= new Launch(launcherProxy, mode, mainType, sourceLocator, result[0].getProcesses(), result[0].getDebugTarget());
 				registerLaunch(newLaunch);
 			}
 			return true;
 		} catch (CoreException e) {
 			JDIDebugUIPlugin.log(e);
 		}
 		return false;
 	}
 	
 	private void registerLaunch(final ILaunch launch) {
 		SWTUtil.getStandardDisplay().syncExec(new Runnable() {
 			public void run() {
 				DebugPlugin.getDefault().getLaunchManager().registerLaunch(launch);
 			}
 		});
 	}
 	
 	protected IVMRunner getVMRunner(IJavaProject jproject, String mode) throws CoreException {
 		if (jproject != null) {
 			IVMInstall vm= JavaRuntime.getVMInstall(jproject);
 			if (vm == null)
 				vm= JavaRuntime.getDefaultVMInstall();
 			if (vm != null)
 				return vm.getVMRunner(mode);
 		}
 		return null;
 	}
 	
 	/**
 	 * Returns a collection of elements this launcher is capable of launching
 	 * in the specified mode based on the given selection. If this launcher cannot launch any
 	 * elements in the selection, an empty array is returned.
 	 * Also used by the JavaApplicationWizard.
 	 */
 	public Object[] getLaunchableElements(IStructuredSelection selection, String mode) {
 		try {
 			ProgressMonitorDialog dialog= new ProgressMonitorDialog(JDIDebugUIPlugin.getActiveWorkbenchShell());
 			return MainMethodFinder.findTargets(dialog, selection.toArray());
 		} catch (InvocationTargetException e) {
 			// ignore, return no targets
 			JDIDebugUIPlugin.log(e);
 		} catch (InterruptedException e) {
 			// user pressed cancel
 		}
 		return new Object[0];
 	}
 
 	/**
 	 * @see ILauncherDelegate#getLaunchMemento(Object)
 	 */
 	public String getLaunchMemento(Object element) {
 		if (element instanceof IJavaElement) {
 			return ((IJavaElement)element).getHandleIdentifier();
 		}
 		return null;
 	}
 
 	/**
 	 * @see ILauncherDelegate#getLaunchObject(String)
 	 */	
 	public Object getLaunchObject(String memento) {
 		IJavaElement e = JavaCore.create(memento);
 		if (e != null && e.exists()) {
 			return e;
 		} else {
 			return null;
 		}
 	}
 }
