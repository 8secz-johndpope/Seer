 package devopsdistilled.operp.client.context.stock;
 
 import org.springframework.context.annotation.Bean;
 import org.springframework.context.annotation.Configuration;
 
 import devopsdistilled.operp.client.stock.panes.CreateWarehousePane;
 import devopsdistilled.operp.client.stock.panes.EditWarehousePane;
 import devopsdistilled.operp.client.stock.panes.ListStockPane;
 import devopsdistilled.operp.client.stock.panes.ListWarehousePane;
import devopsdistilled.operp.client.stock.panes.TransferStockPane;
 import devopsdistilled.operp.client.stock.panes.UpdateStockPane;
 
 @Configuration
 public class MvcViewContext {
 
 	@Bean
 	public CreateWarehousePane createWarehousePane() {
 		return new CreateWarehousePane();
 	}
 
 	@Bean
 	public ListWarehousePane listWarehousePane() {
 		return new ListWarehousePane();
 	}
 
 	@Bean
 	public EditWarehousePane editWarehousePane() {
 		return new EditWarehousePane();
 	}
 
 	@Bean
 	public ListStockPane listStockPane() {
 		return new ListStockPane();
 	}
 
 	@Bean
 	public UpdateStockPane updateStockPane() {
 		return new UpdateStockPane();
 	}
 
	@Bean
	public TransferStockPane transferStockPane() {
		return new TransferStockPane();
	}

 }
