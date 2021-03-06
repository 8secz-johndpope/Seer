 package com.ks.stockquote;
 
 import java.io.BufferedReader;
 import java.io.DataOutputStream;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.net.HttpURLConnection;
 import java.net.URL;
 
 import android.content.Context;
 import android.net.ConnectivityManager;
 import android.net.NetworkInfo;
 
 import com.google.gson.Gson;
 
 public class Util {
 
 	public static Gson gson = new Gson();
 
 	public enum RequestMethod {
 		GET, POST
 	}
 
 	public static String executeRequest(RequestMethod method, String targetURL, String urlParameters) {
 		URL url;
 		HttpURLConnection connection = null;
 		try {
 			// Create connection
 			url = new URL(targetURL);
 			connection = (HttpURLConnection) url.openConnection();
 			connection.setRequestMethod(method.toString());
 			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
 
 			// connection.setRequestProperty("Content-Length", "" +
 			// Integer.toString(urlParameters.getBytes().length));
 			connection.setRequestProperty("Content-Language", "en-US");
 
 			connection.setUseCaches(false);
 			connection.setDoInput(true);
 			connection.setDoOutput(true);
 
 			// Send request
 			DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
 			wr.writeBytes(urlParameters);
 			wr.flush();
 			wr.close();
 
 			// Get Response
 			InputStream is = connection.getInputStream();
 			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
 			String line;
 			StringBuffer response = new StringBuffer();
 			while ((line = rd.readLine()) != null) {
 				response.append(line);
 				response.append('\r');
 			}
 			rd.close();
 			return response.toString();
 
 		} catch (Exception e) {
 
 			e.printStackTrace();
 			return null;
 
 		} finally {
 
 			if (connection != null) {
 				connection.disconnect();
 			}
 		}
 	}
 
 	public static boolean hasInternet(Context context) {
 		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
 		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
 
 		boolean connected = false;
 		if (activeNetwork == null) {
 			connected = false;
 		} else {
 			connected = activeNetwork.isConnectedOrConnecting();
 		}
 
 		return connected;
 	}
 }
