 /*
  *  MicroEmulator
  *  Copyright (C) 2001 Bartek Teodorczyk <barteo@barteo.net>
  *
  *  It is licensed under the following two licenses as alternatives:
  *    1. GNU Lesser General Public License (the "LGPL") version 2.1 or any newer version
  *    2. Apache License (the "AL") Version 2.0
  *
  *  You may not use this file except in compliance with at least one of
  *  the above two licenses.
  *
  *  You may obtain a copy of the LGPL at
  *      http://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
  *
  *  You may obtain a copy of the AL at
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  *  Unless required by applicable law or agreed to in writing, software
  *  distributed under the License is distributed on an "AS IS" BASIS,
  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *  See the LGPL or the AL for the specific language governing permissions and
  *  limitations.
  */
  
 package javax.microedition.lcdui;
 
 import java.util.Vector;
 
 import org.microemu.device.Device;
 import org.microemu.device.DeviceFactory;
 import org.microemu.device.ui.DisplayableUI;
 
 
 
 public abstract class Displayable
 {
 	Device device;
 	
 	boolean sizeChangedDeferredRequest;
 	
 	Display currentDisplay = null;
     
 	boolean fullScreenMode;
 
     Ticker ticker;
     
     // TODO make private
     int viewPortY;
     // TODO make private
     int viewPortHeight;
     
    DisplayableUI ui;
     
     private String title;
     
     /**
      * @associates Command 
      */
 	private Vector commands = new Vector();
 	
 	private CommandListener listener = null;
 
     
     Displayable(String title) 
     {
         this.device = DeviceFactory.getDevice();
         this.sizeChangedDeferredRequest = false;        
         this.fullScreenMode = false;
         this.title = title;
     }
     
     
     void setUI(DisplayableUI ui) {
     	this.ui = ui;
     }
   
 
 	public void addCommand(Command cmd) {
 		// Check that its not the same command
 		for (int i = 0; i < commands.size(); i++) {
 			if (cmd == (Command) commands.elementAt(i)) {
 				// Its the same just return
 				return;
 			}
 		}
 
 		// Now insert it in order
 		boolean inserted = false;
 		for (int i = 0; i < commands.size(); i++) {
 			if (cmd.getPriority() < ((Command) commands.elementAt(i)).getPriority()) {
 				commands.insertElementAt(cmd, i);
 				inserted = true;
 				break;
 			}
 		}
 		if (inserted == false) {
 			// Not inserted just place it at the end
 			commands.addElement(cmd);
 		}
 		
 		ui.addCommand(cmd);
 
 		if (isShown()) {
 			currentDisplay.updateCommands();
 		}
 	}
 
 
 	public void removeCommand(Command cmd)
 	{
 		commands.removeElement(cmd);
 		
 		ui.removeCommand(cmd);
 
 		if (isShown()) {
 			currentDisplay.updateCommands();
 		}
 	}
     
     
     public int getWidth()
     {
     	if (fullScreenMode) {
     		return device.getDeviceDisplay().getFullWidth();
     	} else {
     		return device.getDeviceDisplay().getWidth();
     	}
     }
 
 
     public int getHeight()
     {
     	if (fullScreenMode) {
     		return device.getDeviceDisplay().getFullHeight();
     	} else {
     		return device.getDeviceDisplay().getHeight();
     	}
     }
 
 
 	public boolean isShown()
 	{
 		if (currentDisplay == null) {
 			return false;
 		}
 		return currentDisplay.isShown(this);
 	}
 
     
     public Ticker getTicker() 
     {
         return ticker;
     }
 
     
     public void setTicker(Ticker ticker) 
     {
         this.ticker = ticker;
 
         repaint();
     }
 
     
     public String getTitle() 
     {
         return title;
     }
 
     
     public void setTitle(String s) 
     {
         this.title = s;
         
         // TODO move to the native UI component
         ui.invalidate();
     }        
     
 
 	public void setCommandListener(CommandListener l)
 	{
 		listener = l;
 		
 		ui.setCommandListener(l);
 	}
 	
 	
 	protected void sizeChanged(int w, int h)
 	{		
 	}
 
 
 	CommandListener getCommandListener()
 	{
 		return listener;
 	}
 
 
 	Vector getCommands()
 	{
 		// in Form this is overriden to allow for the inclusion
 		// of item contained commands 
 		// Andres Navarro
 		return commands;
 	}
 
 
 	void hideNotify()
 	{
 	}
 
 
 	final void hideNotify(Display d)
 	{		
 		ui.hideNotify();
 
 		hideNotify();
 	}
 
 
 	void keyPressed(int keyCode)
 	{
 	}
 
 
 	void keyRepeated(int keyCode)
 	{
 	}
 
 
 	void keyReleased(int keyCode)
 	{
 	}
 
 
 	void pointerPressed(int x, int y) 
 	{
 	}
 
 	
 	void pointerReleased(int x, int y) 
 	{
 	}
 
 	
 	void pointerDragged(int x, int y) 
 	{
 	}
 
 	
 	abstract void paint(Graphics g);
 
 
 	void repaint()
 	{
 		if (currentDisplay != null) {
 			repaint(0, 0, getWidth(), getHeight());
 		}
 	}
 
 
 	void repaint(int x, int y, int width, int height)
     {
 		if (currentDisplay != null) {
 			currentDisplay.repaint(this, x, y, width, height);
 		}
     }
 	
 	
 	void showNotify()
 	{        
 	}
 
 
 	final void showNotify(Display d)
 	{
 		currentDisplay = d;
         viewPortY = 0;
         // TODO remove this StringComponent object when native UI is completed
         StringComponent title = new StringComponent(getTitle());
         viewPortHeight = getHeight() - title.getHeight() - 1;
         if (ticker != null) {
         		viewPortHeight -= this.ticker.getHeight();
         }
         
         if (sizeChangedDeferredRequest) {
         	sizeChanged(getWidth(), getHeight());
         	sizeChangedDeferredRequest = false;
         }
 		
 		showNotify();
 
 		ui.showNotify();		
 	}
 
 }
