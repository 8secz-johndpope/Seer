 package com.brainydroid.daydreaming.ui.Dashboard;
 
 import android.content.Context;
 import android.text.Html;
 import android.view.LayoutInflater;
 import android.view.View;
 import android.view.ViewGroup;
 import android.widget.ImageButton;
 import android.widget.LinearLayout;
 import android.widget.RelativeLayout;
 import android.widget.TextView;
 import com.brainydroid.daydreaming.R;
 import com.brainydroid.daydreaming.background.Logger;
 import com.brainydroid.daydreaming.ui.FirstLaunchSequence.FirstLaunch02TermsActivity;
 import roboguice.inject.ContentView;
 
 @ContentView(R.layout.activity_first_launch_terms)
 
 public class TermsActivity extends FirstLaunch02TermsActivity {
 
     private static String TAG = "TermsActivity";
 
     @Override
     public  void setButtonAndScrollViewListener() {
         agreeButton.setVisibility(View.GONE);
         disagreeButton.setVisibility(View.GONE);
 
         View buttons_layout =  findViewById(R.id.firstLaunchTerms_buttons);
         TextView valueTV = new TextView(this);
        valueTV.setText("If you no longer agree to these terms, " +
                "uninstall the app to quit the experiment.");
         valueTV.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        valueTV.setTextColor(R.color.ui_dark_blue_color);
         ((LinearLayout) buttons_layout).addView(valueTV);
 
         TextView text = (TextView) findViewById(R.id.firstLaunchTerms_please_scroll);
         text.setVisibility(View.GONE); // Clear TextView asking to scroll down
 
         setRobotoFont(this);
 
 
 
         ViewGroup parent = (ViewGroup)findViewById(R.id.firstLaunchTerms_main_layout);
         LayoutInflater.from(this).inflate(R.layout.return_to_dashboard_button, parent, true);
 
 
     }
 
     @Override
     public void addInfoButtonListener(){
         more_consent_button.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 more_consent_text.setText(Html.fromHtml(getString(R.string.more_terms_html)));  }
         });
     }
 
     @Override
     public void addAgreementButtonListener() { }
 
 
     @Override
     public void onBackPressed() {
         Logger.v(TAG, "Back pressed, setting slide transition");
         super.onBackPressed();
         overridePendingTransition(R.anim.push_bottom_in, R.anim.push_bottom_out);
     }
 
     @Override
     public void checkFirstLaunch() { }
 
     public void onClick_backtodashboard(View v) {
         onBackPressed();
     }
 
 
 }
