 /*******************************************************************************
  * Copyright (c) 2005 The Regents of the University of California. 
  * This material was produced under U.S. Government contract W-7405-ENG-36 
  * for Los Alamos National Laboratory, which is operated by the University 
  * of California for the U.S. Department of Energy. The U.S. Government has 
  * rights to use, reproduce, and distribute this software. NEITHER THE 
  * GOVERNMENT NOR THE UNIVERSITY MAKES ANY WARRANTY, EXPRESS OR IMPLIED, OR 
  * ASSUMES ANY LIABILITY FOR THE USE OF THIS SOFTWARE. If software is modified 
  * to produce derivative works, such modified software should be clearly marked, 
  * so as not to confuse it with the version available from LANL.
  * 
  * Additionally, this program and the accompanying materials 
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  * 
  * LA-CC 04-115
  *******************************************************************************/
 package org.eclipse.ptp.rtsystem.simulation;
 
 import java.util.ArrayList;
 import java.util.Observable;
 
import org.eclipse.ptp.core.IPProcess;

 
 public class SimThread extends Observable {
 
 	final int RUNNING = 10;
 	final int SUSPENDED = 11;
 	final int TERMINATED = 12;
 	
 	int state;
 	
 	int curLine;
 	ArrayList breakLines;
 	SimStackFrame[] stackFrames;
 	int threadId;
 	int processId;
	SimProcess simProcess;
 	
	public SimThread(SimProcess proc, int tid, int pId) {
 		state = RUNNING;
 		curLine = 1;
		simProcess = proc;
 		breakLines = new ArrayList();
 		threadId = tid;
 		processId = pId;
 		int numStackFrames = 1;
 		
 		stackFrames = new SimStackFrame[numStackFrames];
 		for (int i = 0; i < numStackFrames; i++) {
 			stackFrames[i] = new SimStackFrame(i, "123456", "main", "main.c", 6);
 		}
 	}
 	
 	public int getThreadId() {
 		return threadId;
 	}
 	
 	public int getStackFrameCount() {
 		return stackFrames.length;
 	}
 	
 	public SimStackFrame[] getStackFrames() {
 		return stackFrames;
 	}
 
 	public void runCommand(SimInputStream in, String cmd, String arg) {
 		if (cmd.equals("print")) {
 			checkBreakpoint();
 			in.printString(curLine + " : " + arg + " from process (pid) " + processId + " & thread #" + threadId);
 			curLine++;
 /*		} else	if (cmd.equals("break")) {
 			breakLine = Integer.parseInt(arg);
 */		}
 	}
 	
 	public void addBreakpoint(int line) {
 		breakLines.add(new Integer(line));
 	}
 	
 	public void checkBreakpoint() {
 		Integer[] bps = (Integer []) breakLines.toArray(new Integer[0]);
 		for (int i = 0; i < bps.length; i++) {
 			if (curLine == bps[i].intValue()) {
 				state = SUSPENDED;
				simProcess.setStatus(IPProcess.STOPPED);

 				System.out.println("Process: " + processId + " Thread: " + 
 						threadId + " SUSPENDED at line " + curLine);
 				
 				setChanged();
 				ArrayList list = new ArrayList();
 				list.add(0, new Integer(processId));
 				list.add(1, new String("BREAKPOINTHIT"));
 				
 				/* Additional info */
 				list.add(2, new String("main.c"));
 				list.add(3, new Integer(curLine));
 				
 				for (int j = 0; j < stackFrames.length; j++) {
 					stackFrames[j].setLine(curLine);
 				}
 				
 				notifyObservers(list);
 				//BitList bitList = new BitList();
 				//bitList.set(processId);
 				//dSim.fireEvent(new EBreakpointHit(bitSet));
 				//dSim.fireEvent(new BreakpointHitEvent(dSim.getSession(), bitList));
 				// Do Something
 				return;
 			}
 		}
 
 	}
 	
 	public void terminate() {
 		state = TERMINATED;
		simProcess.setStatus(IPProcess.EXITED);
 		setChanged();
 		ArrayList list = new ArrayList();
 		list.add(0, new Integer(processId));
 		list.add(1, new String("TERMINATED"));
 		notifyObservers(list);
 	}
 	
 	public void resume() {
 		state = RUNNING;
		simProcess.setStatus(IPProcess.RUNNING);
 		setChanged();
 		ArrayList list = new ArrayList();
 		list.add(0, new Integer(processId));
 		list.add(1, new String("RESUMED"));
 		notifyObservers(list);
 	}
 }
