 /*
  * Copyright (C) 2012 Jamie Nicol <jamie@thenicols.net>
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 
 package org.jamienicol.resistors;
 
 import android.app.Activity;
 import android.os.Bundle;
 import android.view.View;
 import android.widget.TextView;
 
 public class ResistorsActivity extends Activity
 {
 	private View mainLayout;
 	private Resistor resistor;
 	private TextView resistanceView;
 
 	/** Called when the activity is first created. */
 	@Override
 	public void onCreate(Bundle savedInstanceState)
 	{
 		super.onCreate(savedInstanceState);
 		setContentView(R.layout.main);
 
 		mainLayout = findViewById (R.id.mainLayout);
 		resistor = (Resistor)findViewById (R.id.resistor);
 		resistanceView = (TextView)findViewById (R.id.resistanceView);
 
 		mainLayout.setBackgroundDrawable (resistor.getBackground ());
 		resistanceView.setBackgroundDrawable (resistor.getBackground ());
 		resistanceView.setText (resistor.getResistance ().toString ());

		/* update text view whenever the resistance changes */
		Resistor.OnResistanceChangeListener listener = new Resistor.OnResistanceChangeListener () {
			public void onResistanceChange (Resistance resistance) {
				resistanceView.setText (resistance.toString ());
			}
		};
		resistor.setOnResistanceChangeListener (listener);
 	}
 }
