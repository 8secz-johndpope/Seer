 package fi.aalto.lounaspaikka;
 
 import java.util.Calendar;
 
 import android.app.Activity;
 import android.os.Bundle;
 import android.text.method.ScrollingMovementMethod;
 import android.util.Log;
 import android.widget.TextView;
 import fi.aalto.lounaspaikka.objectfiles.ObjectsContainer;
 import fi.aalto.lounaspaikka.objectfiles.Restaurant;
 
 public class MenuActivity extends Activity{
 	@Override
 	public void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 		setContentView(R.layout.menu);
 		String todaysmenu=""; 
 		Calendar today = Calendar.getInstance();
 		 int day = today.get(Calendar.DAY_OF_WEEK);
 		//sunday is first day saturday 7th
 		 
 		int restaurantcount= ObjectsContainer.restaurants.size();
 		int rcounter=0;
 		while (restaurantcount>rcounter) {
		 todaysmenu =todaysmenu + ObjectsContainer.restaurants.get(rcounter).name + '\n';
 		
 		int mcounter=0;
		while (ObjectsContainer.restaurants.get(rcounter).weeksmenu.monday.daysmenu.size()>mcounter){
 		if (day==1)
 		{
 		todaysmenu = todaysmenu +ObjectsContainer.restaurants.get(rcounter).weeksmenu.sunday.daysmenu.get(mcounter).meal + '\n' ;
 		}
 		else if (day==2) {
 		todaysmenu = todaysmenu +ObjectsContainer.restaurants.get(rcounter).weeksmenu.monday.daysmenu.get(mcounter).meal + '\n' ;	
 		}
 		else if (day==3) {
 			todaysmenu = todaysmenu +ObjectsContainer.restaurants.get(rcounter).weeksmenu.tuesday.daysmenu.get(mcounter).meal + '\n' ;	
 		}
 		else if (day==4) {
 			todaysmenu = todaysmenu +ObjectsContainer.restaurants.get(rcounter).weeksmenu.wednesday.daysmenu.get(mcounter).meal + '\n' ;	
 		}
 		else if (day==5) {
 			todaysmenu = todaysmenu +ObjectsContainer.restaurants.get(rcounter).weeksmenu.thursday.daysmenu.get(mcounter).meal + '\n' ;	
 		}
 		else if (day==6) {
 			todaysmenu = todaysmenu +ObjectsContainer.restaurants.get(rcounter).weeksmenu.friday.daysmenu.get(mcounter).meal + '\n' ;	
 		}
 		else if (day==7) {
 			todaysmenu = todaysmenu +ObjectsContainer.restaurants.get(rcounter).weeksmenu.saturday.daysmenu.get(mcounter).meal + '\n' ;	
 		}
 		mcounter++;
 		}
 		rcounter++;
 		}
 		 TextView tv;
 		 tv = (TextView)findViewById(R.id.textView1);
 		    
 		    tv.setMovementMethod(new ScrollingMovementMethod());
 		    tv.setText(todaysmenu); 
 	}
 
 
 
 
 }
