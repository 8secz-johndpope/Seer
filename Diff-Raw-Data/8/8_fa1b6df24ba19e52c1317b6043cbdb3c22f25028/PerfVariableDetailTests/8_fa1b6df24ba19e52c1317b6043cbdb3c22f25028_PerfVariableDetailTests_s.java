 /*******************************************************************************
  * Copyright (c) 2000, 2005 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  * 
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.jdt.debug.tests.performance;
 
 import org.eclipse.debug.core.model.IValue;
 import org.eclipse.debug.ui.DebugUITools;
 import org.eclipse.debug.ui.IDebugModelPresentation;
 import org.eclipse.debug.ui.IValueDetailListener;
 import org.eclipse.jdt.debug.core.IJavaDebugTarget;
 import org.eclipse.jdt.debug.core.IJavaLineBreakpoint;
 import org.eclipse.jdt.debug.core.IJavaStackFrame;
 import org.eclipse.jdt.debug.core.IJavaThread;
 import org.eclipse.jdt.debug.core.IJavaVariable;
 import org.eclipse.jdt.debug.tests.AbstractDebugPerformanceTest;
 import org.eclipse.test.performance.Dimension;
 
 /**
  * Tests performance of conditional breakpoints.
  */
 public class PerfVariableDetailTests extends AbstractDebugPerformanceTest implements IValueDetailListener {
 
     private IJavaDebugTarget fTarget;
     private Object fLock = new Object();
     
     public PerfVariableDetailTests(String name) {
         super(name);
     }
 
     public void detailComputed(IValue value, String result) {
     	synchronized (fLock) {
     		fLock.notifyAll();
 		}
 	}
     
     public void testToStringDetails() throws Exception {
         // just in case
     	tagAsSummary("Computing variable toString() details iteratively", Dimension.ELAPSED_PROCESS);
         removeAllBreakpoints();
         String typeName = "VariableDetails";
         IJavaLineBreakpoint breakpoint = createLineBreakpoint(24, typeName);
         IJavaThread thread = null;
         try {
        	thread = launchToBreakpoint(typeName);
         	IJavaStackFrame frame = (IJavaStackFrame) thread.getTopStackFrame();
         	assertNotNull("Missing top stack frame", frame);
         	IJavaVariable variable = frame.findVariable("v");
         	assertNotNull("Missing variable 'v'", variable);
         	IDebugModelPresentation presentation = DebugUITools.newDebugModelPresentation("org.eclipse.jdt.debug");
         	IValue value = variable.getValue();
         	
         	// warm up
         	for (int i = 0; i < 100; i++) {
         		synchronized (fLock) {
         			presentation.computeDetail(value, this);
         			fLock.wait(30000);
         		}
         	}
         	
         	// test
        	for (int i = 0; i < 10; i++) {
         		startMeasuring();
         		for (int j = 0; j < 150; j++) {
 	        		synchronized (fLock) {
 	        			presentation.computeDetail(value, this);
 	        			fLock.wait(30000);
 	        		}
         		}
         		stopMeasuring();
         	}
         	
         	commitMeasurements();
         	assertPerformance();
         	
         } finally {
         	removeAllBreakpoints();
         	terminateAndRemove(thread);
         }
     }
 
 	
 
 }
