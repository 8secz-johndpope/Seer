 package cz.quinix.condroid.ui;
 
 
 import cz.quinix.condroid.R;
 import android.app.AlertDialog;
 import android.content.Context;
 import android.content.DialogInterface;
 import android.content.Intent;
 import android.content.SharedPreferences;
 import android.net.Uri;
 import android.view.LayoutInflater;
 import android.view.View;
 import android.widget.ImageView;
 
 public class AboutDialog extends AlertDialog {
 	
 
 	protected AboutDialog(Context context) {
 		super(context);
 		
 		View v = LayoutInflater.from(context).inflate(R.layout.about_dialog, null);
		this.setTitle(R.string.appName);
 		this.setView(v);
 		this.setButton(BUTTON1, "OK", new OkListener());
 		this.setButton(BUTTON2, "Přispět", new DonateListener());
 		this.setButton(BUTTON3, "Feedback", new FeedbackListener());
 		ImageView follow = ((ImageView) v.findViewById(R.id.iFollow));
 		follow.setOnClickListener(new FollowListener());
 		this.setIcon(R.drawable.icon);
 	}
 	
 	class OkListener implements OnClickListener {
 
 		public void onClick(DialogInterface dialog, int which) {
 			SharedPreferences pr = getContext().getSharedPreferences(WelcomeActivity.TAG, 0);
 			SharedPreferences.Editor e = pr.edit();
 			e.putBoolean("aboutShown", true);
 			e.commit();
 		}
 		
 	}
 	
 	class FeedbackListener implements OnClickListener {
 
 		public void onClick(DialogInterface dialog, int which) {
 			/* Create the Intent */
 			final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
 
 			/* Fill it with Data */
 			emailIntent.setType("plain/text");
 			emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{getContext().getString(R.string.tAboutMail)});
 			emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "[Condroid] - Feedback");
 			//emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "");
 
 			/* Send it off to the Activity-Chooser */
 			getContext().startActivity(Intent.createChooser(emailIntent, "Poslat e-mail"));
 		}
 		
 	}
 	
 	class DonateListener implements OnClickListener {
 
 		public void onClick(DialogInterface dialog, int which) {
 			Intent intent = new Intent(Intent.ACTION_VIEW);
 			intent.setData(Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_donations" +
 					"&business=2Z394R5DLKGU4&lc=CZ&item_name=Condroid&currency_code=CZK" +
 					"&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHostedGuest"));
 			getContext().startActivity(intent);
 			
 		}
 		
 	}
 	class FollowListener implements android.view.View.OnClickListener {
 
 		public void onClick(View v) {
 			Intent intent = new Intent(Intent.ACTION_VIEW);
 			intent.setData(Uri.parse("http://mobile.twitter.com/Condroid_CZ"));
 			getContext().startActivity(intent);
 		}
 		
 	}
 
 }
