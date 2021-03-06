 package devopsdistilled.operp.client.party;
 
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 
 import javax.inject.Inject;
 import javax.swing.JButton;
 import javax.swing.JComponent;
 import javax.swing.JPanel;
 
 import net.miginfocom.swing.MigLayout;
 import devopsdistilled.operp.client.abstracts.TaskPane;
 import devopsdistilled.operp.client.party.controllers.VendorController;
 
 public class PartyMgmtPane extends TaskPane {
 
 	@Inject
 	private VendorController vendorController;
 
 	@Override
 	public String toString() {
 		return new String("Parties");
 	}
 
 	/**
 	 * @wbp.parser.entryPoint
 	 */
 	@Override
 	public JComponent getPane() {
 		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("", "[][][]", "[]"));
 
 		JButton btnCreateVendor = new JButton("Create Vendor");
 		btnCreateVendor.addActionListener(new ActionListener() {
 			@Override
 			public void actionPerformed(ActionEvent e) {
 				vendorController.create();
 			}
 		});
 		panel.add(btnCreateVendor, "cell 0 0");

		JButton btnListVendors = new JButton("List Vendors");
		btnListVendors.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				vendorController.list();
			}
		});
		panel.add(btnListVendors, "cell 2 0");
 		return panel;
 	}
 
 }
