 package devopsdistilled.operp.client.items.views;
 
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.util.List;
 
 import javax.inject.Inject;
 import javax.swing.JButton;
 import javax.swing.JComboBox;
 import javax.swing.JComponent;
 import javax.swing.JLabel;
 import javax.swing.JPanel;
 import javax.swing.JTextField;
 
 import net.miginfocom.swing.MigLayout;
 import devopsdistilled.operp.client.abstracts.SubTaskPane;
 import devopsdistilled.operp.client.items.controllers.CreateItemPaneController;
 import devopsdistilled.operp.client.items.models.observers.BrandModelObserver;
 import devopsdistilled.operp.client.items.models.observers.CreateItemPaneModelObserver;
 import devopsdistilled.operp.client.items.models.observers.ProductModelObserver;
 import devopsdistilled.operp.server.data.entity.items.Brand;
 import devopsdistilled.operp.server.data.entity.items.Item;
 import devopsdistilled.operp.server.data.entity.items.Product;
 
 public class CreateItemPane extends SubTaskPane implements
 		CreateItemPaneModelObserver, ProductModelObserver, BrandModelObserver {
 
 	@Inject
 	private CreateItemPaneController controller;
 
 	private final JPanel pane;
 	private final JTextField itemNameField;
 	private final JTextField priceField;
 	private final JComboBox<Brand> comboBrands;
 	private final JComboBox<Product> comboProducts;
 
 	@Override
 	public void init() {
 		super.init();
 	}
 
 	public CreateItemPane() {
 		pane = new JPanel();
 		pane.setLayout(new MigLayout("", "[][][grow][]", "[][][][][]"));
 
 		JLabel lblProductName = new JLabel("Product Name");
 		pane.add(lblProductName, "cell 0 0,alignx trailing");
 
 		comboProducts = new JComboBox<Product>();
 		pane.add(comboProducts, "flowx,cell 2 0,growx");
 
 		JLabel lblBrandName = new JLabel("Brand Name");
 		pane.add(lblBrandName, "cell 0 1,alignx trailing");
 
 		comboBrands = new JComboBox<Brand>();
 		pane.add(comboBrands, "flowx,cell 2 1,growx");
 
 		JLabel lblItemId = new JLabel("Item Name");
 		pane.add(lblItemId, "cell 0 2,alignx trailing");
 
 		itemNameField = new JTextField();
 
 		pane.add(itemNameField, "cell 2 2,growx");
 		itemNameField.setColumns(10);
 
 		JLabel lblPrice = new JLabel("Price");
 		pane.add(lblPrice, "cell 0 3,alignx trailing");
 
 		priceField = new JTextField();
 		pane.add(priceField, "cell 2 3,growx");
 		priceField.setColumns(10);
 
 		JButton btnCancel = new JButton("Cancel");
 		btnCancel.addActionListener(new ActionListener() {
 			@Override
 			public void actionPerformed(ActionEvent e) {
 				getDialog().dispose();
 			}
 		});
 		pane.add(btnCancel, "flowx,cell 2 4");
 		JButton btnSave = new JButton("Create");
 		btnSave.addActionListener(new ActionListener() {
 			@Override
 			public void actionPerformed(ActionEvent e) {
 				Item item = new Item();
 				Brand brand = (Brand) comboBrands.getSelectedItem();
 				Product product = (Product) comboProducts.getSelectedItem();
 				item.setBrand(brand);
 				item.setProduct(product);
 
 			}
 		});
 		pane.add(btnSave, "cell 2 4");
 
 		JButton btnNewProduct = new JButton("New Product");
 		pane.add(btnNewProduct, "cell 2 0");
 
 		JButton btnNewBrand = new JButton("New Brand");
 		btnNewBrand.addActionListener(new ActionListener() {
 			@Override
 			public void actionPerformed(ActionEvent e) {
 			}
 		});
 		pane.add(btnNewBrand, "cell 2 1");
 
 	}
 
 	@Override
 	public JComponent getPane() {
 		return pane;
 	}
 
 	@Override
 	public void updateBrands(List<Brand> brands) {
 		comboBrands.removeAllItems();
 		for (Brand brand : brands) {
 			comboBrands.addItem(brand);
 		}
 
 	}

	@Override
	public void updateProducts(List<Product> products) {
		comboProducts.removeAllItems();
		for (Product product : products) {
			comboProducts.addItem(product);
		}
	}

 }
