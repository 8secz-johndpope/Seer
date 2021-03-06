 package devopsdistilled.operp.client.items.panes.controllers.impl;
 
 import javax.inject.Inject;
 
 import devopsdistilled.operp.client.items.exceptions.ItemNameExistsException;
 import devopsdistilled.operp.client.items.exceptions.NullFieldException;
 import devopsdistilled.operp.client.items.exceptions.ProductBrandPairExistsException;
 import devopsdistilled.operp.client.items.models.BrandModel;
 import devopsdistilled.operp.client.items.models.ItemModel;
 import devopsdistilled.operp.client.items.models.ProductModel;
 import devopsdistilled.operp.client.items.panes.CreateItemPane;
 import devopsdistilled.operp.client.items.panes.controllers.CreateItemPaneController;
 import devopsdistilled.operp.client.items.panes.models.CreateItemPaneModel;
 import devopsdistilled.operp.server.data.entity.items.Item;
 
 public class CreateItemPaneControllerImpl implements CreateItemPaneController {
 
 	@Inject
 	private CreateItemPaneModel model;
 
 	@Inject
 	private CreateItemPane view;
 
 	@Inject
 	private ItemModel itemModel;
 
 	@Inject
 	private ProductModel productModel;
 
 	@Inject
 	private BrandModel brandModel;
 
 	@Override
 	public void init() {
 		view.init();
 		model.registerObserver(view);
 		productModel.registerObserver(view);
 		brandModel.registerObserver(view);
 	}
 
 	@Override
 	public void validate(Item item) throws ProductBrandPairExistsException,
 			ItemNameExistsException, NullFieldException {
 
		if (item.getItemName().equalsIgnoreCase("")
				|| item.getProduct() == null || item.getBrand() == null
				|| item.getPrice() == null) {
 
 			throw new NullFieldException();
 		}
 
 		if (itemModel.getService().isProductBrandPairExists(item.getProduct(),
 				item.getBrand())) {
 
 			throw new ProductBrandPairExistsException();
 		}
 
 		if (itemModel.getService().isItemNameExists(item.getItemName())) {
 
 			throw new ItemNameExistsException();
 		}
 	}
 
 	@Override
 	public Item save(Item item) {
 		Item savedItem = itemModel.saveAndUpdateModel(item);
 		return savedItem;
 	}
 }
