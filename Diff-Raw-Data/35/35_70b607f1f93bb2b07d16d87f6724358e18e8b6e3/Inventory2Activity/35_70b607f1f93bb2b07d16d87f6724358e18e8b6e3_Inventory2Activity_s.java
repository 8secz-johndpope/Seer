 package edu.upenn.cis350.project;
 
 import java.util.HashMap;
 
 import android.os.Bundle;
 import android.app.Activity;
 import android.content.Intent;
 import android.text.Editable;
 import android.view.Menu;
 import android.view.View;
 import android.widget.AdapterView;
 import android.widget.EditText;
 import android.widget.GridView;
 import android.widget.TextView;
 import android.widget.Toast;
 import android.widget.AdapterView.OnItemClickListener;
 
 public class Inventory2Activity extends Activity {
 
 	Bundle data;
 	int[] inventory; // pre-processing inventory
 	int[] fruitQtys; // post-processing changes 
 	/* table to store quantity of each item
 	 * 0 - apple
 	 * 1 - banana
 	 * 2 - grapes
 	 * 3 - kiwi
 	 * 4 - orange
 	 * 5 - pear
 	 * 6 - granola
 	 * 7 - frozen fruit
 	 * 8 - mixed bags
 	 * 9 - smoothie
 	 */
 	
 	@Override
 	protected void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 		setContentView(R.layout.activity_inventory2);
 		data = getIntent().getExtras();
 	}
 	
 	public void qtyClicked2(View view) {
 		switch(view.getId()) {
 		case R.id.apple2Plus:
			changeQty(true, 0, R.id.appleQty);
 			break;
 		case R.id.apple2Minus:
			changeQty(false, 0, R.id.appleQty);
 			break;
 		case R.id.banana2Plus:
			changeQty(true, 1, R.id.bananaQty);
 			break;
 		case R.id.banana2Minus:
			changeQty(false, 1, R.id.bananaQty);
 			break;
 		case R.id.grapes2Plus:
			changeQty(true, 2, R.id.grapesQty);
 			break;
 		case R.id.grapes2Minus:
			changeQty(false, 2, R.id.grapesQty);
 			break;
 		case R.id.kiwi2Plus:
			changeQty(true, 3, R.id.kiwiQty);
 			break;
 		case R.id.kiwi2Minus:
			changeQty(false, 3, R.id.kiwiQty);
 			break;
 		case R.id.orange2Plus:
			changeQty(true, 4, R.id.orangeQty);
 			break;
 		case R.id.orange2Minus:
			changeQty(false, 4, R.id.orangeQty);
 			break;
 		case R.id.pear2Plus:
			changeQty(true, 5, R.id.pearQty);
 			break;
 		case R.id.pear2Minus:
			changeQty(false, 5, R.id.pearQty);
 			break;
 		case R.id.mixedPlus:
 			changeQty(true, 8, R.id.mixedQty);
 			break;
 		case R.id.mixedMinus:
 			changeQty(false, 8, R.id.mixedQty);
 			break;
 		case R.id.smoothiePlus:
 			changeQty(true, 9, R.id.smoothieQty);
 			break;
 		case R.id.smoothieMinus:
 			changeQty(false, 9, R.id.smoothieQty);
 			break;
 		default:
 			throw new RuntimeException("Unknown Button!");
 		}
 	}
 	
 	// method that modifies fruit quantity depending on which button was pressed
 	private void changeQty(boolean pm, int fruit, int cid ) {
 		int qtyTemp = getQty(cid);
 		
 		if (pm) { // increment fruit qty
 			qtyTemp++;
 		} else if (qtyTemp > 0) { // decrement fruit qty
 			qtyTemp--;
 		}
 		fruitQtys[fruit] = qtyTemp;
 		EditText qtyEdit = (EditText) findViewById(cid);
 		qtyEdit.setText(""+qtyTemp);
 		
 	}
 	
 	private int getQty (int cid){
 		EditText qtyEdit = (EditText) findViewById(cid);
 		Editable qtyE = qtyEdit.getText();
 		
 		try {
 			return Integer.parseInt(qtyE.toString());
 		} catch (Exception e) {
 			return 0;
 		}
 	}
 
 	@Override
 	public boolean onCreateOptionsMenu(Menu menu) {
 		// Inflate the menu; this adds items to the action bar if it is present.
 		getMenuInflater().inflate(R.menu.activity_inventory2, menu);
 		return true;
 	}
 
     public void continueToTransactionBase(View v) {
     	
     	// calculate new inventory based on post-processing
     	inventory = (int[]) data.get("fruit_quantities");
     	
     	// subtract fruits used to make mixed bags
     	for (int i=0; i<6; i++) {
     		inventory[i] -= fruitQtys[i];
     	}
     	
     	// add on mixed fruit and smoothies that were made
     	inventory[8] = fruitQtys[8];
     	inventory[9] = fruitQtys[9];
     	
     	//Launch to transaction base
     	Intent i = new Intent(this, TransactionBaseActivity.class);
     	
     	//TODO Use savePostInventory to save info
     	HashMap<String, Integer> postinv = new HashMap<String, Integer>();
     	postinv.put("apples", inventory[0]);
     	postinv.put("bananas", inventory[1]);
     	postinv.put("grapes", inventory[2]);
     	postinv.put("kiwis", inventory[3]);
     	postinv.put("oranges", inventory[4]);
     	postinv.put("pears", inventory[5]);
     	postinv.put("granolas", inventory[6]);
     	postinv.put("mixed", inventory[8]);
     	postinv.put("smoothie", inventory[9]);
     	
     	DataBaser.getInstance().savePostInventory(postinv);
     	
     	i.putExtras(this.getIntent().getExtras());
     	
     	this.startActivity(i);
     }
 }
