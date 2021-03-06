 /**
  * Copyright (c) 2009-2010 MATSUFUJI Hideharu <matsufuji2008@gmail.com>,
  *               2010 KUBO Atsuhiro <kubo@iteman.jp>,
  * All rights reserved.
  *
  * This file is part of MakeGood.
  *
  * This program and the accompanying materials are made available under
  * the terms of the Eclipse Public License v1.0 which accompanies this
  * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
  */
 
 package com.piece_framework.makegood.ui.views;
 
 import java.io.File;
 import java.io.IOException;
 
 import javax.xml.parsers.ParserConfigurationException;
 
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.core.runtime.IStatus;
 import org.eclipse.core.runtime.Status;
 import org.eclipse.core.runtime.jobs.Job;
 import org.eclipse.debug.core.DebugEvent;
 import org.eclipse.debug.core.DebugException;
 import org.eclipse.debug.core.IDebugEventSetListener;
 import org.eclipse.debug.core.ILaunch;
 import org.eclipse.debug.core.model.IProcess;
 import org.eclipse.debug.internal.ui.views.console.ProcessConsole;
 import org.eclipse.debug.ui.IDebugUIConstants;
 import org.eclipse.php.internal.debug.core.model.IPHPDebugTarget;
 import org.eclipse.ui.console.ConsolePlugin;
 import org.eclipse.ui.console.IConsole;
 import org.eclipse.ui.console.IConsoleConstants;
 import org.eclipse.ui.console.IOConsoleOutputStream;
 import org.eclipse.ui.progress.UIJob;
 import org.xml.sax.SAXException;
 
 import com.piece_framework.makegood.core.result.JUnitXMLReader;
 import com.piece_framework.makegood.core.result.JUnitXMLReaderListener;
 import com.piece_framework.makegood.core.result.TestCaseResult;
 import com.piece_framework.makegood.core.result.TestSuiteResult;
 import com.piece_framework.makegood.launch.MakeGoodLaunch;
 import com.piece_framework.makegood.launch.MakeGoodLaunchConfigurationDelegate;
 import com.piece_framework.makegood.ui.Activator;
 import com.piece_framework.makegood.ui.actions.StopTestAction;
 import com.piece_framework.makegood.ui.ide.ViewShow;
 import com.piece_framework.makegood.ui.launch.TestRunner;
 
 public class ResultDebugEventSetListener implements IDebugEventSetListener {
     private Thread parserThread;
     private boolean hasErrors;
     private RunProgress progress;
     private JUnitXMLReader junitXMLReader;
     private TestCaseResult currentTestCase;
     private Failures failures;
 
     @Override
     public void handleDebugEvents(DebugEvent[] events) {
         if (events == null) return;
         int size = events.length;
         for (int i = 0; i < size; ++i) {
             final Object source = events[i].getSource();
             ILaunch launch = getLaunch(source);
             if (launch == null) continue;
             if (!(launch instanceof MakeGoodLaunch)) continue;
 
             if (events[i].getKind() == DebugEvent.CREATE) {
                 handleCreate(launch);
             } else if (events[i].getKind() == DebugEvent.TERMINATE) {
                 handleTerminate(launch);
             }
         }
     }
 
     private void handleCreate(final ILaunch launch) {
         // TODO This marker is to avoid calling create() twice by PDT.
         String createCalledMarker = getClass().getName() + ".createCalled"; //$NON-NLS-1$
         if (Boolean.TRUE.toString().equals(launch.getAttribute(createCalledMarker))) return;
         launch.setAttribute(createCalledMarker, Boolean.TRUE.toString());
 
         String junitXMLFile = null;
         try {
             junitXMLFile = MakeGoodLaunchConfigurationDelegate.getJUnitXMLFile(launch);
         } catch (CoreException e) {
             Activator.getDefault().getLog().log(new Status(Status.WARNING, Activator.PLUGIN_ID, e.getMessage(), e));
         }
         if (junitXMLFile == null) return;
 
         preventConsoleViewFocusing();
         progress = new RunProgress();
         progress.start();
         failures = new Failures();
         currentTestCase = null;
 
         junitXMLReader = new JUnitXMLReader(new File(junitXMLFile));
         junitXMLReader.addListener(new ResultJUnitXMLReaderListener());
         parserThread = new Thread() {
             @Override
             public void run() {
                 hasErrors = false;
                 try {
                     junitXMLReader.read();
                 } catch (ParserConfigurationException e) {
                     Activator.getDefault().getLog().log(new Status(IStatus.WARNING, Activator.PLUGIN_ID, e.getMessage(), e));
                 } catch (SAXException e) {
                     if (!StopTestAction.isStoppedByAction(launch)) {
                         hasErrors = true;
                     }
                 } catch (IOException e) {
                     Activator.getDefault().getLog().log(new Status(IStatus.WARNING, Activator.PLUGIN_ID, e.getMessage(), e));
                 }
             }
         };
         parserThread.start();
 
         Job job = new UIJob("MakeGood Test Start") { //$NON-NLS-1$
             @Override
             public IStatus runInUIThread(IProgressMonitor monitor) {
                 ResultView resultView = null;
                 resultView = (ResultView) ViewShow.show(ResultView.ID);
                 if (resultView == null) return Status.CANCEL_STATUS;
 
                 TestRunner.restoreFocusToLastActivePart();
 
                 resultView.reset();
                 resultView.startTest(progress, failures);
                 return Status.OK_STATUS;
             }
         };
         job.schedule();
     }
 
     private void handleTerminate(ILaunch launch) {
         // TODO This marker is to avoid calling terminate() twice by PDT.
         String terminateCalledMarker = getClass().getName() + ".terminateCalled"; //$NON-NLS-1$
         if (Boolean.TRUE.toString().equals(launch.getAttribute(terminateCalledMarker))) return;
         launch.setAttribute(terminateCalledMarker, Boolean.TRUE.toString());
 
         GET_EXIT_VALUE:
         for (IProcess process: launch.getProcesses()) {
             int exitValue = 0;
             do {
                 try {
                    if (process.isTerminated()) {
                        exitValue = process.getExitValue();
                    }

                     break;
                 } catch (DebugException e) {
                     Activator.getDefault().getLog().log(new Status(Status.WARNING, Activator.PLUGIN_ID, e.getMessage(), e));
                 }
 
                 try {
                     Thread.sleep(200);
                 } catch (InterruptedException e) {
                     break GET_EXIT_VALUE;
                 }
             } while (true);
 
             if (exitValue != 0) {
                 if (!StopTestAction.isStoppedByAction(launch)) {
                     hasErrors = true;
                 }
             }
 
             break;
         }
 
         junitXMLReader.stop();
         // TODO Since PDT 2.1 always returns 0 from IProcess.getExitValue(), We decided to use SAXException to check whether or not a PHP process exited with a fatal error.
         try {
             parserThread.join();
         } catch (InterruptedException e) {
             Activator.getDefault().getLog().log(new Status(Status.WARNING, Activator.PLUGIN_ID, e.getMessage(), e));
         }
 
         progress.end();
 
         Job job = new UIJob("MakeGood Test End") { //$NON-NLS-1$
             @Override
             public IStatus runInUIThread(IProgressMonitor monitor) {
                 ResultView resultView = (ResultView) ViewShow.find(ResultView.ID);
                 if (resultView == null) return Status.CANCEL_STATUS;
 
                 resultView.endTest();
 
                 if (hasErrors == true) {
                     ViewShow.show(IConsoleConstants.ID_CONSOLE_VIEW);
                 }
 
                 return Status.OK_STATUS;
             }
         };
         job.schedule();
     }
 
     private void preventConsoleViewFocusing() {
         for (IConsole console: ConsolePlugin.getDefault().getConsoleManager().getConsoles()) {
             if (!(console instanceof ProcessConsole)) continue;
             IOConsoleOutputStream stdoutStream = ((ProcessConsole) console).getStream(IDebugUIConstants.ID_STANDARD_OUTPUT_STREAM);
             if (stdoutStream == null) continue;
             stdoutStream.setActivateOnWrite(false);
             IOConsoleOutputStream stderrStream = ((ProcessConsole) console).getStream(IDebugUIConstants.ID_STANDARD_ERROR_STREAM);
             if (stderrStream == null) continue;
             stderrStream.setActivateOnWrite(false);
         }
     }
 
     private ILaunch getLaunch(Object eventSource) {
         if (eventSource instanceof IPHPDebugTarget) {
             return ((IPHPDebugTarget) eventSource).getLaunch();
         } else if (eventSource instanceof IProcess) {
             return ((IProcess) eventSource).getLaunch();
         } else {
             return null;
         }
     }
 
     public class ResultJUnitXMLReaderListener implements JUnitXMLReaderListener {
         @Override
         public void startTestSuite(TestSuiteResult testSuite) {
             failures.addResult(testSuite);
 
             if (progress.isInitialized()) {
                 return;
             }
 
             progress.initialize(testSuite);
 
             Job job = new UIJob("MakeGood Result Tree Set") { //$NON-NLS-1$
                 @Override
                 public IStatus runInUIThread(IProgressMonitor monitor) {
                     ResultView resultView = (ResultView) ViewShow.find(ResultView.ID);
                     if (resultView == null) return Status.CANCEL_STATUS;
                     resultView.setTreeInput(junitXMLReader.getResult());
                     return Status.OK_STATUS;
                 }
             };
             job.schedule();
         }
 
         @Override
         public void endTestSuite() {}
 
         @Override
         public void startTestCase(final TestCaseResult testCase) {
             failures.addResult(testCase);
             currentTestCase = testCase;
             progress.startTestCase();
 
             Job job = new UIJob("MakeGood Test Case Start") { //$NON-NLS-1$
                 @Override
                 public IStatus runInUIThread(IProgressMonitor monitor) {
                     ResultView resultView = (ResultView) ViewShow.find(ResultView.ID);
                     if (resultView == null) return Status.CANCEL_STATUS;
                     resultView.printCurrentlyRunningTestCase(currentTestCase);
                     resultView.updateOnStartTestCase(currentTestCase);
                     return Status.OK_STATUS;
                 }
             };
             job.schedule();
             try {
                 job.join();
             } catch (InterruptedException e) {
                 e.printStackTrace();
             }
         }
 
         @Override
         public void endTestCase() {
             if (!progress.isInitialized()) return;
 
             progress.endTestCase();
             currentTestCase.setTime(progress.getProcessTimeForTestCase());
 
             Job job = new UIJob("MakeGood Test Case End") { //$NON-NLS-1$
                 @Override
                 public IStatus runInUIThread(IProgressMonitor monitor) {
                     ResultView resultView = (ResultView) ViewShow.find(ResultView.ID);
                     if (resultView == null) return Status.CANCEL_STATUS;
                     resultView.updateOnEndTestCase(currentTestCase);
                     return Status.OK_STATUS;
                 }
             };
             job.schedule();
         }
 
         @Override
         public void startFailure(TestCaseResult failure) {
             failures.markCurrentResultAsFailure();
             currentTestCase = failure;
         }
 
         @Override
         public void endFailure() {}
     }
 }
