 /*
  * Copyright (C) 2011-12 asksven
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package com.asksven.betterbatterystats;
 
 import android.appwidget.AppWidgetManager;
 import android.content.ComponentName;
 import android.content.Context;
 import android.content.Intent;
 import android.util.Log;
 import com.asksven.android.common.utils.GenericLogger;
 import com.asksven.betterbatterystats.R;
 
 /**
  * @author sven
  *
  */
 public class SmallWidgetProvider extends BbsWidgetProvider
 {
 
 	private static final String TAG = "SmallWidgetProvider";
 
 	@Override
 	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
 	{
 
 		Log.w(TAG, "onUpdate method called");
 
 		// Update the widgets via the service
 		startService(context, this.getClass(), appWidgetManager, UpdateSmallWidgetService.class);
 		
 		setAlarm(context);
 		
 		super.onUpdate(context, appWidgetManager, appWidgetIds);
 	}
 
 	@Override
 	public void onReceive(Context context, Intent intent)
 	{
 		super.onReceive(context, intent);
 		if ( (WIDGET_UPDATE.equals(intent.getAction())) ||
 				(LargeWidgetProvider.WIDGET_PREFS_REFRESH.equals(intent.getAction())) ||
 					intent.getAction().equals("android.appwidget.action.APPWIDGET_UPDATE") )
 
 		{
 			if (WIDGET_UPDATE.equals(intent.getAction()))
 			{
 				Log.d(TAG, "Alarm called: updating");
 			}
 			else if (LargeWidgetProvider.WIDGET_PREFS_REFRESH.equals(intent.getAction()))
 			{
 				Log.d(TAG, "WIDGET_PREFS_REFRESH called: updating");
				GenericLogger.i(LargeWidgetProvider.WIDGET_LOG, TAG, "SmallWidgetProvider: Alarm to refresh widget was called");
 			}
 			else
 			{
 				Log.d(TAG, "APPWIDGET_UPDATE called: updating");
 			}
 				
 
 			AppWidgetManager appWidgetManager = AppWidgetManager
 					.getInstance(context);
 			ComponentName thisAppWidget = new ComponentName(
 					context.getPackageName(),
 					this.getClass().getName());
 			int[] appWidgetIds = appWidgetManager
 					.getAppWidgetIds(thisAppWidget);
 
 			if (appWidgetIds.length > 0)
 			{
 				onUpdate(context, appWidgetManager, appWidgetIds);
 			}
 		}
 	}
 
 	
 	
 }
