 /*
     Copyright 2007-2012 QSpin - www.qspin.be
 
     This file is part of QTaste framework.
 
     QTaste is free software: you can redistribute it and/or modify
     it under the terms of the GNU Lesser General Public License as published by
     the Free Software Foundation, either version 3 of the License, or
     (at your option) any later version.
 
     QTaste is distributed in the hope that it will be useful,
     but WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
     GNU Lesser General Public License for more details.
 
     You should have received a copy of the GNU Lesser General Public License
     along with QTaste. If not, see <http://www.gnu.org/licenses/>.
 */
 
 package com.qspin.qtaste.javagui.server;
 
 import java.awt.Component;
 import java.awt.Container;
 import java.util.List;
 
 import javax.swing.JDialog;
 import javax.swing.JOptionPane;
 import javax.swing.JTextField;
 import javax.swing.SwingUtilities;
 
 import com.qspin.qtaste.testsuite.QTasteException;
 import com.qspin.qtaste.testsuite.QTasteTestFailException;
 
 /**
  * Commander which sets a value in the input field of a popup.
  * @see JOptionPane#showInputDialog(Object)
  */
 public class PopupTextSetter extends UpdateComponentCommander {
 
 	/**
 	 * Commander which sets a value in the input field of a popup.
 	 * @param INTEGER - the timeout value; OBJECT - with the value to insert. The toString method will be used on the object.
 	 * @return true if the command is successfully performed.
 	 * @throws QTasteException
 	 */
 	@Override
 	public Boolean executeCommand(Object... data) throws QTasteException {
 		setData(data);
 		int timeout = Integer.parseInt(mData[0].toString());
 		long maxTime = System.currentTimeMillis() + 1000 * timeout;
 		
 		while ( System.currentTimeMillis() < maxTime )
 		{
 			List<JDialog> popups = findPopups();
			JDialog targetPopup = null;
 			//If there is only one popup, use this popup and do not check the popup state.
 			if ( popups.size() == 1 )
				targetPopup = popups.get(0);
 			else
 			{
 				for (JDialog dialog : popups )
 				{
 					setComponentFrameVisible(dialog);
 					if ( !dialog.isVisible() || !dialog.isEnabled() || !dialog.isActive() )
 					{
 						String msg = "Ignore the dialog '" + dialog.getTitle() + "' cause:\n ";
 						if (!dialog.isVisible())
 							msg += "\t is not visible";
 						if (!dialog.isEnabled())
 							msg += "\t is not enabled";
 						if (!dialog.isActive())
 							msg += "\t is not active";
 						LOGGER.info(msg);
 						continue;
 					}
 					else
 					{
						targetPopup = dialog;
 					}
 				}
 			}
			component = findTextComponent(targetPopup);
 			
 			if ( component != null && component.isEnabled() && checkComponentIsVisible(component) )
 				break;
 			if ( component != null && component.isEnabled() && checkComponentIsVisible(component) )
 				break;
 			
 			try {
 				Thread.sleep(1000);
 			} catch (InterruptedException e) {
 				LOGGER.warn("Exception during the component search sleep...");
 			}
 		}	
 			
 		if (component == null )
 		{
 			throw new QTasteTestFailException("The text field component is not found.");
 		}
 		if (!component.isEnabled()) {
 			throw new QTasteTestFailException("The text field component is not enabled.");
 		}
 		if (! checkComponentIsVisible(component))
 			throw new QTasteTestFailException("The text field component is not visible!");
 		
 		
 		prepareActions();
 		SwingUtilities.invokeLater(this);
 		synchronizeThreads();
 		
 		return true;
 	}
 	
 	private JTextField findTextComponent(Component c)
 	{
 		if ( c instanceof JTextField )
 			return (JTextField)c;
 		else if ( c instanceof Container )
 		{
 			for (Component comp : ((Container)c).getComponents() )
 			{
 				JTextField jtf = findTextComponent(comp);
 				if ( jtf != null )
 					return jtf;
 			}
 		}
 		return null;
 	}
 	
 	@Override
 	protected void prepareActions() throws QTasteTestFailException {
 		//Do nothing
 	}
 
 	@Override
 	protected void doActionsInSwingThread() throws QTasteTestFailException {
 		((JTextField)component).setText(mData[1].toString());
 	}
 
 }
