 package com.dougsko.diceware;
 
 
 import android.app.Activity;
 import android.app.AlertDialog;
 import android.app.Dialog;
 //import android.content.ClipData;
 import android.content.DialogInterface;
 import android.content.Intent;
 import android.database.Cursor;
 import android.net.Uri;
 import android.os.Bundle;
 import android.text.ClipboardManager;
 //import android.content.ClipboardManager;
 import android.view.Gravity;
 import android.view.Menu;
 import android.view.MenuInflater;
 import android.view.MenuItem;
 import android.view.View;
 import android.widget.AdapterView;
 import android.widget.AdapterView.OnItemSelectedListener;
 import android.widget.ArrayAdapter;
 import android.widget.Button;
 import android.widget.Spinner;
 import android.widget.TextView;
 import android.widget.Toast;
 
 public class Diceware extends Activity {
     /** Called when the activity is first created. */
 
 	private TextView mOutputText;
 	private DicewareDbAdapter mDbHelper;
 	private randomOrgHelper mRandomOrgHelper;
 	private int mode; 
 	private String roll;
 	private TextView rollsSoFar;
 	private TextView totalRolls;
 	
 	@Override
     public void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         setContentView(R.layout.main);
         
         roll = "";
         
         mOutputText = (TextView) findViewById(R.id.output);
         Button button_one = (Button) findViewById(R.id.one);
         Button button_two = (Button) findViewById(R.id.two);
         Button button_three = (Button) findViewById(R.id.three);
         Button button_four = (Button) findViewById(R.id.four);
         Button button_five = (Button) findViewById(R.id.five);
         Button button_six = (Button) findViewById(R.id.six);
         Button randomOrg = (Button) findViewById(R.id.randomOrg);
         Button copy_to_clipboard = (Button) findViewById(R.id.copy_to_clipboard);
         Button clear = (Button) findViewById(R.id.clear);
         rollsSoFar = (TextView) findViewById(R.id.rollsSoFar);
         totalRolls = (TextView) findViewById(R.id.totalRolls);
         
         final ClipboardManager clipBoard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
         
         mDbHelper = new DicewareDbAdapter(this);
         mDbHelper.open();
         
         mRandomOrgHelper = new randomOrgHelper(this);
 		
         
         // set up mode spinner
         Spinner spinner = (Spinner) findViewById(R.id.spinner);
         ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                 this, R.array.modes_array, android.R.layout.simple_spinner_item);
         adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
         spinner.setAdapter(adapter);
         spinner.setOnItemSelectedListener(new MyOnItemSelectedListener());
         
         
         
         
         // button 1 callback
         button_one.setOnClickListener(new View.OnClickListener() {
             public void onClick(View view) {
                 roll = roll.concat("1");
                 checkRoll();
             }            
         });
         
      // button 2 callback
         button_two.setOnClickListener(new View.OnClickListener() {
             public void onClick(View view) {
                 roll = roll.concat("2");
                 checkRoll();
             }            
         });
         
      // button 3 callback
         button_three.setOnClickListener(new View.OnClickListener() {
             public void onClick(View view) {
                 roll = roll.concat("3");
                 checkRoll();
             }            
         });
         
      // button 4 callback
         button_four.setOnClickListener(new View.OnClickListener() {
             public void onClick(View view) {
                 roll = roll.concat("4");
                 checkRoll();
             }            
         });
         
      // button 5 callback
         button_five.setOnClickListener(new View.OnClickListener() {
             public void onClick(View view) {
                 roll = roll.concat("5");
                 checkRoll();
             }            
         });
         
      // button 6 callback
         button_six.setOnClickListener(new View.OnClickListener() {
             public void onClick(View view) {
                 roll = roll.concat("6");
                 checkRoll();
             }            
         });
         
         /** randomOrg button callback
          * http get: http://www.random.org/integers/?num=6&min=1&max=6&col=1&base=10&format=plain&rnd=new
          * get 5 numbers, iterate through them and append each to roll and do checkRoll().
          */
         randomOrg.setOnClickListener(new View.OnClickListener() {
             public void onClick(View view) {
             	String numbers = mRandomOrgHelper.getNumbers(); //randomOrgHelper.getNumbers();
             	if(numbers != null) {
             		for(int i = 0; i <= numbers.length(); i++){
             			roll = numbers.substring(0,i);
             			checkRoll();
             			roll = "";
             		}
             	}
             	else {
             		Toast toast = Toast.makeText(Diceware.this, 
         	    			R.string.randomOrg_problem, 
         	    			Toast.LENGTH_LONG);
         	    	toast.setGravity(Gravity.BOTTOM|Gravity.CENTER, 0, 0);
         	    	toast.show();
             	}
             }            
         });
         
         // callback for clipboard button
         copy_to_clipboard.setOnClickListener(new View.OnClickListener() {
         	public void onClick(View view) {
         		CharSequence textPhrase = (CharSequence) mOutputText.getText();
         		if (textPhrase.length() > 1) {
         			textPhrase = textPhrase.subSequence(0, textPhrase.length()-1);
         			clipBoard.setText(textPhrase);
         			//clipBoard.setPrimaryClip(ClipData.newPlainText("pass", textPhrase));
         		}
         	}
         });
         
         // callback for clear button
         clear.setOnClickListener(new View.OnClickListener() {
         	public void onClick(View view) {
         		mOutputText.setText("");
         	}
         });
     
 	}
 		
 	// The callback for the mode selector spinner.
 	public class MyOnItemSelectedListener implements OnItemSelectedListener {
 		
 		String numberOfRolls;
 
 	    public void onItemSelected(AdapterView<?> parent,
 	        View view, int pos, long id) {
 	    	switch(pos){
 	    	case 0:
 	    		numberOfRolls = "5";
 	    		break;
 	    	case 1:
 	    		numberOfRolls = "3";
 	    		break;
 	    	case 2:
 	    		numberOfRolls = "2";
 	    		break;
 	    	case 3:
 	    		numberOfRolls = "2";
 	    		break;
 	    	}
 	    	Toast toast = Toast.makeText(parent.getContext(), 
 	    			String.format(getString(R.string.times_to_roll),numberOfRolls), 
 	    			Toast.LENGTH_LONG);
 	    	toast.setGravity(Gravity.BOTTOM|Gravity.CENTER, 0, 0);
 	    	toast.show();
 	      mode = pos;
 	      roll = "";
 	      totalRolls.setText(numberOfRolls);
 	    }
 
 	    public void onNothingSelected(AdapterView<?> parent) {
 	      // Do nothing.
 	    }
 	}
 	
 	// increment roll count
 	private void incrementRollCount(){
 		int rollsSoFarInt = Integer.parseInt((String) rollsSoFar.getText());
 		rollsSoFarInt += 1;
 		rollsSoFar.setText(Integer.toString(rollsSoFarInt));
 	}
 	
 	// reset roll count
 	private void resetRollCount(){
 		rollsSoFar.setText("0");
 	}
     
 	// the modes equate to the index of the modes_array in strings.xml
 	private void checkRoll(){
 		switch (mode) {
 		case 0:
 			incrementRollCount();
 			if( roll.length() == 5) {
 				getWord();
 				resetRollCount();
 			}
 			break;
 		case 1:
 			incrementRollCount();
 			if (roll.length() == 3) {
 				getAscii();
 				resetRollCount();
 			}
 			break;
 		case 2:
 			incrementRollCount();
 			if ( roll.length() == 2) {
 				getAlphaNumeric();
 				resetRollCount();
 			}
 			break;
 		case 3:
 			incrementRollCount();
 			if ( roll.substring(0) == "6"){
 				Toast toast = Toast.makeText(Diceware.this, 
 						getString(R.string.roll_again), 
 						Toast.LENGTH_LONG);
 				toast.setGravity(Gravity.BOTTOM|Gravity.CENTER, 0, 0);
 		    	toast.show();
 				roll = "";
 				resetRollCount();
 				break;
 			}
 			if(roll.length() == 2){
 				String first_digit_string = roll.substring(0, 1);
 				String second_digit_string = roll.substring(1);
 				
 				int first_digit_int = Integer.parseInt(first_digit_string);
 				int second_digit_int = Integer.parseInt(second_digit_string);
 				
 				// is the second roll even? if so, add 5 to first roll. 10 becomes 0.
 				if(second_digit_int % 2 == 0){
 					int output_int = first_digit_int + 5;
 					String output_string = Integer.toString(output_int);
 					mOutputText.append(output_string.substring(output_string.length() - 1) + " ");
 					roll = "";
 				}
 				else{
 					mOutputText.append(first_digit_string + " ");
 					roll = "";
 				}
 				resetRollCount();
 			}
 			break;
 		}
 		
 	}
 	
     private void getWord() {
     	Cursor dicewareCursor = mDbHelper.fetchWord(roll);
         startManagingCursor(dicewareCursor);
         
         mOutputText.append(dicewareCursor.getString(
         		dicewareCursor.getColumnIndexOrThrow(DicewareDbAdapter.KEY_WORD)) + " ");
         
         roll = "";
     }
     
     private void getAlphaNumeric() {
     	Cursor dicewareCursor = mDbHelper.fetchAlphaNumeric(roll);
     	startManagingCursor(dicewareCursor);
     	
     	mOutputText.append(dicewareCursor.getString(
     			dicewareCursor.getColumnIndexOrThrow(DicewareDbAdapter.KEY_CHAR)) + " ");
     	roll = "";
     }
     
     private void getAscii() {
     	Cursor dicewareCursor = mDbHelper.fetchAscii(roll);
     	startManagingCursor(dicewareCursor);
     	
     	String output = dicewareCursor.getString(
     			dicewareCursor.getColumnIndexOrThrow(DicewareDbAdapter.KEY_CHAR));
     	if(output.length() > 2) {
     		Toast toast = Toast.makeText(Diceware.this,
     				getString(R.string.please_roll_again), 
     				Toast.LENGTH_LONG);
     		toast.setGravity(Gravity.BOTTOM|Gravity.CENTER, 0, 0);
 	    	toast.show();    		
     	}
     	else {
     		mOutputText.append(output + " ");
     	}
     	roll = "";
     }
     
     @Override
     public boolean onCreateOptionsMenu(Menu menu) {
         MenuInflater inflater = getMenuInflater();
         inflater.inflate(R.menu.options, menu);
         return true;
     }
     
     @Override
     public boolean onOptionsItemSelected(MenuItem item) {
         // Handle item selection
         switch (item.getItemId()) {
         case R.id.faq:
         	showDialog(0);
             return true;
         case R.id.about:
         	showDialog(1);
         	return true;
         case R.id.help:
         	showDialog(2);
         	return true;
         default:
             return super.onOptionsItemSelected(item);
         }
     }
     
     protected Dialog onCreateDialog(int id) {
     	AlertDialog alert = null;
         switch(id) {
         case 0:
         	String url = getString(R.string.faq_url);        	
         	Intent i = new Intent(Intent.ACTION_VIEW);
         	i.setData(Uri.parse(url));
         	startActivity(i);
             break;
         case 1:
         	AlertDialog.Builder builder = new AlertDialog.Builder(this);
         	builder.setMessage(getString(R.string.about))
         	       .setCancelable(false)
         	       .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
         	           public void onClick(DialogInterface dialog, int id) {
         	        	   dialog.cancel();
         	           }
         	       });
         	alert = builder.create();
         	return alert;
         case 2:
         	AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
         	builder1.setMessage(getString(R.string.help))
         	       .setCancelable(false)
         	       .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
         	           public void onClick(DialogInterface dialog, int id) {
         	        	   dialog.cancel();
         	           }
         	       });
         	alert = builder1.create();
         	return alert;
 		default:
             alert = null;
         }
         return alert;
     }
     
     @Override
     public void onDestroy(){
     	super.onDestroy();
     	mDbHelper.close();
     } 
 }
