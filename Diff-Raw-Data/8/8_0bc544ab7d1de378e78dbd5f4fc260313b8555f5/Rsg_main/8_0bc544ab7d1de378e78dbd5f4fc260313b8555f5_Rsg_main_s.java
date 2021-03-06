 package com.werebug.randomsequencegenerator;
 
 import com.werebug.randomsequencegenerator.R;
 
 import android.os.Build;
 import android.os.Bundle;
 import android.view.View;
 import android.view.View.OnClickListener;
 import android.widget.Button;
 import android.widget.CheckBox;
 import android.widget.RadioGroup;
 import android.widget.RadioGroup.OnCheckedChangeListener;
 import android.widget.TextView;
 import android.app.Activity;
 import android.content.ClipData;
 import android.content.ClipboardManager;
 import android.content.Context;
 import android.content.Intent;
 
 public class Rsg_main extends Activity implements OnClickListener, OnCheckedChangeListener {
 	
 	View range_layout_1;
 	View range_layout_2;
 	View manual_layout;
 
     @Override
     public void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         setContentView(R.layout.activity_rsg_main);
     	this.range_layout_1 = (View)findViewById(R.id.dlu_range);
     	this.range_layout_2 = (View)findViewById(R.id.s_range);
     	this.manual_layout = (View)findViewById(R.id.manual_layout);
         Button create = (Button)findViewById(R.id.button_create);
         create.setOnClickListener(this);
         Button copy = (Button)findViewById(R.id.copy_button);
         copy.setOnClickListener(this);
         Button send_to = (Button)findViewById(R.id.send_button);
         send_to.setOnClickListener(this);
         RadioGroup rg = (RadioGroup)findViewById(R.id.radio_group);
         rg.setOnCheckedChangeListener(this);
     }
     
     public void onCheckedChanged (RadioGroup rg, int newchecked) {
     	switch (newchecked) {
     		case R.id.class_radio:
     			this.range_layout_1.setVisibility(View.VISIBLE);
     			this.range_layout_2.setVisibility(View.VISIBLE);		
     			this.manual_layout.setVisibility(View.GONE);
     			break;
     		case R.id.manual_radio:
     			this.manual_layout.setVisibility(View.VISIBLE);
     			this.range_layout_1.setVisibility(View.GONE);
     			this.range_layout_2.setVisibility(View.GONE);	
     			break;
     		default:
     			this.manual_layout.setVisibility(View.GONE);
     			this.range_layout_1.setVisibility(View.GONE);
     			this.range_layout_2.setVisibility(View.GONE);
     	}
     }
     
    @SuppressWarnings("deprecation")
 	public void onClick (View v) {
     	int clicked = v.getId();
     	switch (clicked) {
     		case R.id.button_create:
     	    	String chars = "";
     	    	String result = "";
     			
     			RadioGroup rg = (RadioGroup)findViewById(R.id.radio_group);
     	    	int selected = rg.getCheckedRadioButtonId();
     	    	
     	    	switch (selected) {
     	    		case R.id.binary_radio:
     	    			chars = chars.concat("01");
     	    			break;
     	    			
     	    		case R.id.hex_radio:
     	    			chars = chars.concat("0123456789ABCDEF");
     	    			break;
     	    			
     	    		case R.id.class_radio:
     	    			CheckBox digit = (CheckBox)findViewById(R.id.range_digit);
     	    			CheckBox lowercase = (CheckBox)findViewById(R.id.range_lowercase);
     	    			CheckBox uppercase = (CheckBox)findViewById(R.id.range_uppercase);
     	    			CheckBox special = (CheckBox)findViewById(R.id.range_special);
     	    			if (digit.isChecked()) {
     	    				chars = chars.concat("0123456789");
     	    			}
     	    			if (lowercase.isChecked()) {
     	    				chars = chars.concat("qwertyuiopasdfghjklzxcvbnm");
     	    			}
     	    			if (uppercase.isChecked()) {
     	    				chars = chars.concat("QWERTYUIOPASDFGHJKLZXCVBNM");
     	    			}
     	    			if (special.isChecked()) {
    	    				chars = chars.concat("$%&/()=?@#<>_-£[]*");
     	    			}
     	    			break;
     	    			
     	    		case R.id.manual_radio:
     	    			TextView manual = (TextView)findViewById(R.id.manual);
     	    			String chars_to_add = manual.getText().toString();
     	    			chars = chars.concat(chars_to_add);
     	    			break;
     	    		
     	    		default:
     	    			break;
     	    	}
     	    	
     	    	int chars_last_index = chars.length() - 1;
     	    	View copy_send = (View)findViewById(R.id.copy_send);
     	    	
     	    	if (chars_last_index >= 0) {
     	    		copy_send.setVisibility(View.VISIBLE);
 	    	    	TextView length_textview = (TextView)findViewById(R.id.string_length);
 	    	    	String length_as_string = length_textview.getText().toString();
 	    	    	int selected_length = Integer.parseInt(length_as_string, 10);
 	    	    	for (int i = selected_length; i > 0; i--) {
 	    	    		long random = Math.round(Math.random()*chars_last_index);
 	    	    		int index = (int) random;
 	    	    		String to_concat = String.valueOf(chars.charAt(index));
 	    	    		result = result.concat(to_concat);
 	    	    	}
     	    	}
     	    	else {
     	    		copy_send.setVisibility(View.GONE);
     	    	}
     	    	
     	    	TextView output = (TextView)findViewById(R.id.output_textview);
     	    	output.setText(result);
     	    	break;
     	    	
     		case R.id.copy_button:
     			TextView random_sequence = (TextView)findViewById(R.id.output_textview);
     			int sdk = Build.VERSION.SDK_INT;
     			if (sdk >= 11) {
 	    			ClipboardManager clipboard = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
 	    			ClipData clip = ClipData.newPlainText("rgs", random_sequence.getText());
 	    			clipboard.setPrimaryClip(clip);
     			}
     			else {
 					android.text.ClipboardManager old_cbm = (android.text.ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
     				old_cbm.setText(random_sequence.getText());    				
     			}
     			break;
     		
     		case R.id.send_button:
     			TextView sequence_to_send = (TextView)findViewById(R.id.output_textview);
     			Intent intent = new Intent(Intent.ACTION_SEND);
     			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
     			intent.setType("text/plain");
     			intent.putExtra(Intent.EXTRA_TEXT, sequence_to_send.getText());
     			startActivity(Intent.createChooser(intent, getResources().getString(R.string.send_with)));
     			break;
     	    
     	    default:
     	    	break;
     	}
     }
 }
