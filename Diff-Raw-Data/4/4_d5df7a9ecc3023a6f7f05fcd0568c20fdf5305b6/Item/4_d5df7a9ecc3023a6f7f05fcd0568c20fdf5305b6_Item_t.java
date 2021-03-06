 package devopsdistilled.operp.server.data.entity.items;
 
 import java.io.Serializable;
 
 import javax.persistence.Entity;
 import javax.persistence.GeneratedValue;
 import javax.persistence.GenerationType;
 import javax.persistence.Id;
 import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
 
 import devopsdistilled.operp.server.data.entity.Entiti;
 
 @Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "product", "brand" }))
 public class Item extends Entiti implements Serializable {
 
 	private static final long serialVersionUID = 1137602696634935018L;
 
 	@Id
 	@GeneratedValue(strategy = GenerationType.AUTO)
 	private Long itemId;
 
 	private String itemName;
 
 	private Double price;
 
 	@ManyToOne
 	private Product product;
 
 	@ManyToOne
 	private Brand brand;
 
 	public Long getItemId() {
 		return itemId;
 	}
 
 	public void setItemId(Long itemId) {
 		this.itemId = itemId;
 	}
 
 	public String getItemName() {
 		return itemName;
 	}
 
 	public void setItemName(String itemName) {
 		this.itemName = itemName;
 	}
 
 	public Double getPrice() {
 		return price;
 	}
 
 	public void setPrice(Double price) {
 		this.price = price;
 	}
 
 	public Product getProduct() {
 		return product;
 	}
 
 	public void setProduct(Product product) {
 		this.product = product;
 	}
 
 	public Brand getBrand() {
 		return brand;
 	}
 
 	public void setBrand(Brand brand) {
 		this.brand = brand;
 	}
 
 }
