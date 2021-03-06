 package com.floreantpos.bo.actions;
 
 import java.awt.event.ActionEvent;
 
 import javax.swing.AbstractAction;
 import javax.swing.Icon;
 import javax.swing.JTabbedPane;
 
 import com.floreantpos.bo.ui.BackOfficeWindow;
 import com.floreantpos.bo.ui.explorer.ModifierGroupExplorer;
 import com.floreantpos.main.Application;
 
 public class ModifierGroupExplorerAction extends AbstractAction {
 
 	public ModifierGroupExplorerAction() {
 		super("Menu Modifier Groups");
 	}
 
 	public ModifierGroupExplorerAction(String name) {
 		super(name);
 	}
 
 	public ModifierGroupExplorerAction(String name, Icon icon) {
 		super(name, icon);
 	}
 
 	public void actionPerformed(ActionEvent e) {
		//BackOfficeWindow backOfficeWindow = Application.getInstance().getBackOfficeWindow();
		//backOfficeWindow.getTabbedPane().addTab("Modifier Group exploere", new ModifierGroupExplorer());

 		BackOfficeWindow backOfficeWindow = Application.getInstance().getBackOfficeWindow();
 		JTabbedPane tabbedPane;
 		ModifierGroupExplorer mGroup;
 		tabbedPane = backOfficeWindow.getTabbedPane();
		int index = tabbedPane.indexOfTab("Modifier Group exploere");
 		if (index == -1) {
 			mGroup = new ModifierGroupExplorer();
			tabbedPane.addTab("Modifier Group exploere", mGroup);
 		}
 		else {
 			mGroup = (ModifierGroupExplorer) tabbedPane.getComponentAt(index);
 		}
 		tabbedPane.setSelectedComponent(mGroup);
 	}
 
 }
