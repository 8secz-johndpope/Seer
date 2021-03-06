 /*******************************************************************************
  * Copyright (c) 2004 - 2005 University Of British Columbia and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     University Of British Columbia - initial API and implementation
  *******************************************************************************/
 
 package org.eclipse.mylar.monitor.tests;
 
import org.eclipse.mylar.core.util.ITimerThreadListener;
import org.eclipse.mylar.core.util.TimerThread;
 
 import junit.framework.TestCase;
 
 /**
  * @author Shawn Minto
  * @author Mik Kersten
  */
 public class ActiveTimerTest extends TestCase {
 
 	private boolean gotTimeOut = false;
	private TimerThread thread;
	private ITimerThreadListener listener = new ITimerThreadListener(){
 
 		public void fireTimedOut() {
 			gotTimeOut = true;
 			thread.killThread();
 		}
 		
 	};
 	
	private ITimerThreadListener listener2 = new ITimerThreadListener(){
 
 		public void fireTimedOut() {
 			gotTimeOut = true;
 		}
 		
 	};
 	
 	public void testActiveTimer(){
		thread = new TimerThread(600, 100);
 		thread.addListener(listener);
 		int i = 0;
 		gotTimeOut = false;
 		thread.start();
 		while(!gotTimeOut){
 			i++;
 			try{
 				Thread.sleep(100);
 			} catch(InterruptedException e){}
 		}
 		assertFalse("Too long of a wait", i > 8);
 		assertFalse("Too short of a wait", i < 6);
 
 		
		thread = new TimerThread(1000, 100);
 		thread.addListener(listener2);
 		i = 0;
 		gotTimeOut = false;
 		thread.start();
 		for(int j = 0; j < 10; j++){
 			try{
 				Thread.sleep(100);
 			} catch(InterruptedException e){}
 			thread.resetTimer();
 		}
 		while(!gotTimeOut){
 			i++;
 			try{
 				Thread.sleep(100);
 			} catch(InterruptedException e){}
 		}
 		thread.killThread();
 		assertFalse("Too long of a wait", i > 12);
 		assertFalse("Too short of a wait", i < 10);	
 	}
 }
