 package com.github.mahmoudhossam.height;
 
 import java.text.NumberFormat;
 
 import android.app.Activity;
 import android.app.AlertDialog;
 import android.app.AlertDialog.Builder;
 import android.content.DialogInterface;
 import android.content.DialogInterface.OnClickListener;
 import android.os.Bundle;
 import android.view.KeyEvent;
 import android.view.Menu;
 import android.view.MenuItem;
 import android.view.View;
 import android.view.View.OnKeyListener;
 import android.widget.EditText;
 import com.github.mahmoudhossam.height.R;
 
 public class HeightActivity extends Activity {
 
 	private static enum Mode {
 		IMPERIAL, METRIC
 	};
 
 	private static final int SWITCH_ID = Menu.FIRST;
 
 	private static Mode current;
 
 	private EditText cm;
 	private EditText feet;
 	private EditText inches;
 
 	/** Called when the activity is first created. */
 	@Override
 	public void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 		setContentView(R.layout.main);
 		init();
 	}
 
 	public void init() {
 		current = Mode.METRIC;
 		cm = (EditText) findViewById(R.id.editText1);
 		feet = (EditText) findViewById(R.id.editText2);
 		inches = (EditText) findViewById(R.id.editText3);
 		setListeners(cm);
 		setListeners(feet);
 		setListeners(inches);
 	}
 
 	public void setListeners(View v) {
 		v.setOnKeyListener(new OnKeyListener() {
 
 			@Override
 			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_ENTER
 						&& event.getAction() == KeyEvent.ACTION_DOWN) {
					if ((v == cm || v == inches)) {
						onConvertClick(null);
					} else if (v == feet) {
						inches.requestFocus();
					}
 				}
 				return false;
 			}
 		});
 	}
 
 	public void onConvertClick(View view) {
 		createResultDialog(getResult()).show();
 	}
 
 	public String getResult() {
 		if (current == Mode.IMPERIAL) {
 			NumberFormat nf = NumberFormat.getInstance();
 			nf.setMaximumFractionDigits(1);
 			double output = Backend.getCentimeters(parseInput(feet),
 					parseInput(inches));
 			return nf.format(output) + " centimeters";
 		} else {
 			int[] result = Backend.getFeetAndInches(parseInput(cm));
 			return "" + result[0] + " feet, " + result[1] + " inches.";
 		}
 	}
 
 	private AlertDialog createResultDialog(String result) {
 		Builder builder = new AlertDialog.Builder(this);
 		builder.setCancelable(false)
 				.setTitle(getResources().getString(R.string.result))
 				.setMessage(result)
 				.setPositiveButton("OK", new OnClickListener() {
 
 					@Override
 					public void onClick(DialogInterface dialog, int which) {
 						dialog.cancel();
 						if (current == Mode.IMPERIAL)
 							feet.requestFocus();
 						else
 							cm.requestFocus();
 					}
 				});
 		return builder.create();
 	}
 
 	public double parseInput(EditText input) {
 		if (input.getText().length() > 0) {
 			String text = input.getText().toString();
 			double content = Double.parseDouble(text);
 			return content;
 		} else {
 			return 0;
 		}
 	}
 
 	@Override
 	public boolean onCreateOptionsMenu(Menu menu) {
 		menu.add(0, SWITCH_ID, 0, R.string.change);
 		return super.onCreateOptionsMenu(menu);
 	}
 
 	@Override
 	public boolean onOptionsItemSelected(MenuItem item) {
 		if (item.getItemId() == SWITCH_ID) {
 			doSwitch();
 		}
 		return super.onOptionsItemSelected(item);
 	}
 
 	private void doSwitch() {
 		if (current == Mode.METRIC) {
 			toImperial();
 		} else {
 			toMetric();
 		}
 		emptyBoxes();
 	}
 
 	private void toImperial() {
 		toggleFeetAndInches(true);
 		toggleCentimeters(false);
 		current = Mode.IMPERIAL;
 		feet.requestFocus();
 	}
 
 	private void toMetric() {
 		toggleFeetAndInches(false);
 		toggleCentimeters(true);
 		current = Mode.METRIC;
 		cm.requestFocus();
 	}
 
 	private void toggleCentimeters(boolean on) {
 		cm.setEnabled(on);
 
 	}
 
 	private void toggleFeetAndInches(boolean on) {
 		feet.setEnabled(on);
 		inches.setEnabled(on);
 	}
 
 	private void emptyBoxes() {
 		cm.setText("");
 		feet.setText("");
 		inches.setText("");
 	}
 
 	@Override
 	protected void onRestoreInstanceState(Bundle savedInstanceState) {
 		cm.setText(savedInstanceState.getString("cm"));
 		feet.setText(savedInstanceState.getString("feet"));
 		inches.setText(savedInstanceState.getString("inches"));
 		super.onRestoreInstanceState(savedInstanceState);
 	}
 
 	@Override
 	protected void onSaveInstanceState(Bundle outState) {
 		outState.putString("cm", cm.getText().toString());
 		outState.putString("inches", inches.getText().toString());
 		outState.putString("feet", feet.getText().toString());
 		super.onSaveInstanceState(outState);
 	}
 }
