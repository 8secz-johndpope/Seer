 package devopsdistilled.operp.server.data.service.items.impl;
 
 import javax.inject.Inject;
 
 import org.springframework.stereotype.Service;
 
 import devopsdistilled.operp.server.data.entity.items.Brand;
 import devopsdistilled.operp.server.data.entity.items.Item;
 import devopsdistilled.operp.server.data.entity.items.Product;
 import devopsdistilled.operp.server.data.repo.items.ItemRepository;
 import devopsdistilled.operp.server.data.service.impl.AbstractEntityService;
import devopsdistilled.operp.server.data.service.items.BrandService;
 import devopsdistilled.operp.server.data.service.items.ItemService;
import devopsdistilled.operp.server.data.service.items.ProductService;
 
 @Service
 public class ItemServiceImpl extends
 		AbstractEntityService<Item, Long, ItemRepository> implements
 		ItemService {
 
 	private static final long serialVersionUID = 7578267584162733059L;
 
 	@Inject
 	private ItemRepository itemRepository;
 
	@Inject
	private ProductService productService;

	@Inject
	private BrandService brandService;

 	@Override
 	protected ItemRepository getRepo() {
 		return itemRepository;
 	}
 
 	@Override
 	public boolean isProductBrandPairExists(Product product, Brand brand) {
 		Item item = itemRepository.findByProductAndBrand(product, brand);
 		if (item != null)
 			return true;
 		else
 			return false;
 	}
 
 	@Override
 	public boolean isItemNameExists(String itemName) {
 		Item item = itemRepository.findByItemName(itemName);
 
 		if (item != null)
 			return true;
 		else
 			return false;
 	}
 
 	/**
 	 * Returns false if Product and Brand Pair exists and the pair doesn't
 	 * belong to given itemId. Else returns true
 	 */
 	@Override
 	public boolean isProductBrandPairValidForItem(Long itemId, Product product,
 			Brand brand) {
 
 		Item item = itemRepository.findByProductAndBrand(product, brand);
 
 		if (item == null)
 			return true;
 
 		if (item.getItemId().equals(itemId))
 			return false;
 
 		return true;
 	}
 
 	/**
 	 * Returns false if ItemName exists for Item other than Item with given
 	 * itemId. Else return true
 	 */
 	@Override
 	public boolean isItemNameValidForItem(Long itemId, String itemName) {
 
 		Item item = itemRepository.findByItemName(itemName);
 
 		if (item == null)
 			return true;
 
 		if (item.getItemId().equals(itemId))
 			return true;
 
 		return false;
 	}
 
 	@Override
 	protected Item findByEntityName(String entityName) {
 		return itemRepository.findByItemName(entityName);
 	}
 
	@Override
	public <S extends Item> S save(S item) {
		if (item.getProduct() != null) {
			Product product = productService.findOne(item.getProduct()
					.getProductId());
			item.setProduct(product);
		}

		if (item.getBrand() != null) {
			Brand brand = brandService.findOne(item.getBrand().getBrandID());
			item.setBrand(brand);
		}

		return super.save(item);
	}
 }
