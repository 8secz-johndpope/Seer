 package com.bettername.thepokemonone;
 
 import android.app.Activity;
 import android.content.Context;
 import android.graphics.LightingColorFilter;
 import android.os.Bundle;
 import android.view.View;
 import android.view.View.OnClickListener;
 import android.widget.Button;
 
 import com.bettername.thepokemoneone.data.CreateUser;
 import com.bettername.thepokemoneone.model.Player;
 
 public class CreaturePickActivity extends Activity {
 
 	Context appContext = this;
 
 	public void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 		setContentView(R.layout.activity_creaturepick);
 
 		final Button creature1 = (Button) findViewById(R.id.rhinodog_button);
 		final Button creature2 = (Button) findViewById(R.id.pigbat_button);
 		final Button creature3 = (Button) findViewById(R.id.birdhorse_button);
 
		final int mul = 0x8A8A8A8A;
 
 		creature1.setOnClickListener(new OnClickListener() {
 
 			@Override
 			public void onClick(View v) {
				creature1.getBackground().setColorFilter(
						new LightingColorFilter(mul, 0X00000000));
 				new CreateUser().createUser(Player.getCurrentUser());
 			}
 		});
 
 		creature2.setOnClickListener(new OnClickListener() {
 
 			@Override
 			public void onClick(View v) {
				creature2.getBackground().setColorFilter(
						new LightingColorFilter(mul, 0X00000000));
 				new CreateUser().createUser(Player.getCurrentUser());
 			}
 		});
 
 		creature3.setOnClickListener(new OnClickListener() {
 
 			@Override
 			public void onClick(View v) {
				creature3.getBackground().setColorFilter(
						new LightingColorFilter(mul, 0X00000000));
 				new CreateUser().createUser(Player.getCurrentUser());
 			}
 		});
 	}
 }
