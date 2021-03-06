 package plugins.data;
 
 import gcc.catalogue.ShoppingCart;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import org.apache.commons.lang.StringUtils;
 import org.molgenis.auth.MolgenisUser;
 import org.molgenis.framework.db.Database;
 import org.molgenis.framework.db.DatabaseException;
 import org.molgenis.framework.db.Query;
 import org.molgenis.framework.db.QueryRule;
 import org.molgenis.framework.db.QueryRule.Operator;
 import org.molgenis.framework.ui.PluginModel;
 import org.molgenis.framework.ui.ScreenController;
 import org.molgenis.framework.ui.ScreenMessage;
 import org.molgenis.util.Entity;
 import org.molgenis.util.HandleRequestDelegationException;
 import org.molgenis.util.SimpleEmailService.EmailException;
 import org.molgenis.util.Tuple;
 
 public class MeasurementsOrderForm extends PluginModel<Entity>{
 
 
 	private static final long serialVersionUID = -8140222842047905408L;
 	private ShoppingCart shoppingCart = null;
 	
 	public MeasurementsOrderForm(String name, ScreenController<?> parent)
 	{
 		super(name, parent);
 	}
 
 	@Override
 	public String getViewName()
 	{
 		return "plugins_data_MeasurementsOrderForm";
 	}
 
 	@Override
 	public String getViewTemplate()
 	{
 		return "plugins/data/MeasurementsOrderForm.ftl";
 	}
 	
 	public String getCustomHtmlHeaders()
     {
         return "<link rel=\"stylesheet\" style=\"text/css\" href=\"res/css/shopping_cart.css\">";
     }
 
 	
 	@Override
 	public void handleRequest(Database db, Tuple request) throws HandleRequestDelegationException, Exception {
 		
 		this.reload(db);
 		
 		
 		if ("EmptyShoppingCart".equals(request.getAction())) {
 			this.emptyShoppingCart(db);
 			this.reload(db);
 			
 		}else if ("checkoutOrder".equals(request.getAction())) {
 			if (shoppingCart == null){
 				this.getModel().getMessages().add(new ScreenMessage("Your shopping cart is empty. You cannot continue with the checkout! Please visit the catalogue tree.", true));
 				this.reload(db);
 			}
 			else {
 				//set field shoppingCart.setCheckedOut(true);
 				this.updateShoppingCartAsCheckedOut(db);
 	    		this.sendOrderEmail(db); 
 				//this.emptyShoppingCart(db);
 				this.getModel().getMessages().add(new ScreenMessage("Your orders request has been sent!", true));
 				this.reload(db);
 			}
 		}
 	}
 
 
 	public void updateShoppingCartAsCheckedOut(Database db) {
 		shoppingCart.setCheckedOut(true);
		System.out.println("shoppingCart>>>>>>>>>>>>>"+shoppingCart);
 		try {
 			db.update(shoppingCart);
 
 		} catch (DatabaseException e) {
 			this.getModel().getMessages().add(new ScreenMessage("A problem with update shopping cart has occured", true));
 			e.printStackTrace();
 		}
 	}
 	
 	public void sendOrderEmail(Database db) throws DatabaseException  {
 		MolgenisUser admin = db.query(MolgenisUser.class).eq(MolgenisUser.NAME, "admin").find().get(0);
 		if (StringUtils.isEmpty(admin.getEmail()))
 			throw new DatabaseException("Registration failed: the administrator has no email address set used to confirm your registration. Please contact your administrator about this.");
 		
 
 		String emailContents = "Dear admin, " + "\n\n"; 
 		emailContents += "The user : "+ this.getLogin().getUserName() +"\n";
 		emailContents += "has sent a request for the items/measurements below:" + "\n";
 		for (String name : shoppingCart.getMeasurements_Name()) {
 			emailContents += name + "\n";
 		}
 		emailContents += "\n\n" ;
 
 		System.out.println(emailContents);
 		try {
 			this.getEmailService().email("New items/measurements ordered", emailContents, admin.getEmail(), true);
 		} catch (EmailException e) {
 			// TODO Auto-generated catch block
 			e.printStackTrace();
 		}
 	}
 	
 	
 	public void emptyShoppingCart(Database db) {
 		//empty db table: actually delete the ones that have checkedOut='false' 
 		List<ShoppingCart> resshoppingCart  = new ArrayList<ShoppingCart>();
 		Query<ShoppingCart> q = db.query(ShoppingCart.class);
 		q.addRules(new QueryRule(ShoppingCart.CHECKEDOUT, Operator.EQUALS, false));
 		try {
 			resshoppingCart = q.find();
 			db.remove(resshoppingCart);
 		} catch (DatabaseException e) {
 			e.printStackTrace();
 		}
 		this.reload(db);
 		this.getModel().getMessages().add(new ScreenMessage("Your shopping cart is now empty, you can reload items from catalogue tree", true));
 	}
 	
 	@Override
 	public void reload(Database db) {
		
 
 		try {
 			Query<ShoppingCart> q = db.query(ShoppingCart.class);
 			q.addRules(new QueryRule(ShoppingCart.USERID, Operator.EQUALS, this.getLogin().getUserName()));
 			q.addRules(new QueryRule(ShoppingCart.CHECKEDOUT, Operator.EQUALS, false));
 
 			if (!q.find().isEmpty()) shoppingCart = q.find().get(0);
 		} catch (Exception e) {
 			this.getModel().getMessages().add(new ScreenMessage("No shopping cart available", false));
 			e.printStackTrace();
 		}
 
 	}
 
 	public ShoppingCart getshoppingCart() {
 		return shoppingCart;
 	}
 }
