 package com.wordpress.mteixeira.saldoticket;
 
 import android.app.AlertDialog;
 import android.content.DialogInterface;
 import android.content.Intent;
 import android.content.SharedPreferences;
 import android.os.AsyncTask;
 import android.os.Bundle;
 import android.app.Activity;
 import android.preference.PreferenceManager;
 import android.text.Editable;
 import android.text.InputFilter;
 import android.text.TextWatcher;
 import android.util.Log;
 import android.view.KeyEvent;
 import android.view.Menu;
 import android.view.MenuItem;
 import android.view.View;
 import android.view.inputmethod.EditorInfo;
 import android.view.inputmethod.InputMethodManager;
 import android.widget.ArrayAdapter;
 import android.widget.AutoCompleteTextView;
 import android.widget.Button;
 import android.widget.ProgressBar;
 import android.widget.TextView;
 
 import com.google.gson.Gson;
 
 import java.io.IOException;
 import java.io.BufferedReader;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.net.UnknownHostException;
 import java.text.ParseException;
 import java.text.SimpleDateFormat;
 import java.util.ArrayList;
 import java.util.Calendar;
 import java.util.Date;
 import java.util.List;
 import java.util.Locale;
 
 import org.apache.http.HttpEntity;
 import org.apache.http.HttpResponse;
 import org.apache.http.client.ClientProtocolException;
 import org.apache.http.client.methods.HttpGet;
 import org.apache.http.impl.client.DefaultHttpClient;
 import org.apache.http.params.BasicHttpParams;
 import org.apache.http.params.HttpConnectionParams;
 import org.apache.http.params.HttpParams;
 
 // auto complete reference
 // http://android-er.blogspot.com.br/2012/10/autocompletetextview-with-dynamic.html
 
 public class MainActivity extends Activity {
 
     private SharedPreferences settings;
     private SharedPreferences.Editor settingsEditor;
 
     AutoCompleteTextView etNumber;
     ArrayAdapter<String> ticketNumberHistoryAdapter;
 
     String someMessage;
     Button btCheck;
     TextView txLast;
     String strLast;
     TextView txBalance;
     String strBalance;
     TextView txNextDate;
     String strNextDate;
     TextView txNextIncrement;
     String strNextIncrement;
     Button btDelete;
     Button btTransactions;
     ProgressBar progress;
     Boolean fetchError = false;
 
     private TicketRestaurante ticket = null;
     public final static String CARD_NUMBER = "com.wordpress.mteixeira.saldoticket.CARD_NUMBER";
     private String TICKETS_HISTORY = "tickets_history";
 
     @Override
     protected void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         setContentView(R.layout.activity_main);
 
         settings = PreferenceManager.getDefaultSharedPreferences(this);
 
         btCheck = (Button)findViewById(R.id.checkButton);
         btCheck.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 hideSoftKeyboard(MainActivity.this);
                 new CheckBalance().execute();
             }
         });
 
         progress = (ProgressBar)findViewById(R.id.mainProgressBar);
         progress.setVisibility(View.GONE);
 
         etNumber = (AutoCompleteTextView)findViewById(R.id.ticketNumber);
         etNumber.setText(settings.getString("ticketNumber", ""));
         try {
             if(etNumber.getText().length() == 16) {
                 btCheck.setEnabled(true);
             } else {
                 btCheck.setEnabled(false);
             }
         } catch (NullPointerException e) {
             showError("Falha interna [c1]");
             Log.e(Constants.TAG, String.valueOf(e));
         }
         InputFilter[] filters = new InputFilter[1];
         filters[0] = new InputFilter.LengthFilter(16);
         etNumber.setFilters(filters);
 
         etNumber.setOnEditorActionListener(new TextView.OnEditorActionListener() {
             @Override
             public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                 boolean handled = false;
                 if(i == EditorInfo.IME_ACTION_GO && etNumber.getText().length() == 16) {
                     hideSoftKeyboard(MainActivity.this);
                     new CheckBalance().execute();
                     handled = true;
                 }
                 return handled;
             }
         });
 
         btDelete = (Button)findViewById(R.id.delete);
         btDelete.setVisibility(etNumber.getText().length() > 0 ? View.VISIBLE : View.GONE);
 
         etNumber.addTextChangedListener(new TextWatcher() {
             @Override
             public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
 
             }
 
             @Override
             public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
 
             }
 
             @Override
             public void afterTextChanged(Editable editable) {
                 btDelete.setVisibility(etNumber.getText().length() > 0 ? View.VISIBLE : View.GONE);
                 if (etNumber.getText().length() == 16) {
                     btCheck.setEnabled(true);
                 } else {
                     btCheck.setEnabled(false);
                 }
             }
         });
 
         txLast = (TextView)findViewById(R.id.textLastCheck);
         txLast.setText(settings.getString("lastCheck", ""));
 
         txBalance = (TextView)findViewById(R.id.textBalance);
         txBalance.setText(settings.getString("lastBalance", ""));
 
         txNextDate = (TextView)findViewById(R.id.textNextDate);
         txNextDate.setText(settings.getString("nextDate", ""));
 
         txNextIncrement = (TextView)findViewById(R.id.textNextIncrement);
         txNextIncrement.setText(settings.getString("nextIncrement", ""));
 
         btTransactions = (Button)findViewById(R.id.buttonTransactions);
         btTransactions.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 hideSoftKeyboard(MainActivity.this);
                 showTransactions(view);
             }
         });
         btTransactions.setVisibility(View.GONE);
 
         updateForecast();
     }
 
     public interface Constants {
         String TAG = "com.wordpress.mteixeira.saldoticket";
     }
 
     public static String formatMoney(Double value) {
         return "R$ " + String.format(Locale.GERMAN, "%1$,.2f", value);
     }
 
     @Override
     public boolean onCreateOptionsMenu(Menu menu) {
         getMenuInflater().inflate(R.menu.main, menu);
         return true;
     }
 
     public boolean onOptionsItemSelected (MenuItem item){
         switch (item.getItemId()){
             case R.id.action_about:
                 AboutDialog about = new AboutDialog(this);
                 about.setTitle(getString(R.string.action_about));
                 about.show();
                 break;
             case R.id.action_settings:
                 startActivity(new Intent(this, SettingsActivity.class));
                 break;
         }
         return true;
     }
 
     @Override
     protected void onPause() {
         onStop();
     }
 
     @Override
     protected void onStop(){
         super.onStop();
         settingsEditor = settings.edit();
         settingsEditor.putString("ticketNumber", etNumber.getText().toString());
         settingsEditor.putString("lastCheck", txLast.getText().toString());
         settingsEditor.putString("lastBalance", txBalance.getText().toString());
         settingsEditor.putString("nextDate", txNextDate.getText().toString());
         settingsEditor.putString("nextIncrement", txNextIncrement.getText().toString());
         settingsEditor.commit();
     }
 
     @Override
     protected void onResume() {
         super.onResume();
         refreshTicketListAdapter();
     }
 
     public void resetCheckBalance(View v) {
         if (etNumber != null) {
             etNumber.setText("");
             txLast.setText("");
             txBalance.setText("");
             txNextDate.setText("");
             txNextIncrement.setText("");
             findViewById(R.id.textUseForecastTotal).setVisibility(TextView.GONE);
             findViewById(R.id.textUseForecastBusiness).setVisibility(TextView.GONE);
             findViewById(R.id.labelUseForecast).setVisibility(TextView.GONE);
         }
     }
 
     private void showError(String message) {
         someMessage = message;
         runOnUiThread(new Runnable() {
             @Override
             public void run() {
                 AlertDialog error = new AlertDialog.Builder(MainActivity.this).create();
                 error.setTitle("Erro");
                 error.setMessage(someMessage);
                 //error.setIcon(R.drawable.error);
                 error.setButton("OK", new DialogInterface.OnClickListener() {
                     @Override
                     public void onClick(DialogInterface dialogInterface, int i) {
                         etNumber.requestFocus();
                     }
                 });
                 error.show();
             }
         });
         try {
             getMainLooper().loop();
         } catch (RuntimeException e) {
             Log.e(MainActivity.Constants.TAG, String.valueOf(e));
         }
     }
 
     private void showTransactions(View view) {
         Intent intent = new Intent(this, TransactionsActivity.class);
         intent.putExtra(CARD_NUMBER, etNumber.getText().toString());
         startActivity(intent);
     }
 
     public static void hideSoftKeyboard(Activity activity) {
         InputMethodManager inputMethodManager = (InputMethodManager)  activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
         inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
     }
 
     private void updateForecast() {
         TextView txUseForecastTotal = (TextView)findViewById(R.id.textUseForecastTotal);
         TextView txUseForecastBusiness = (TextView)findViewById(R.id.textUseForecastBusiness);
         TextView lbUseForecast = (TextView)findViewById(R.id.labelUseForecast);
         Double[] forecast = calculateUseForecast();
         if(forecast[0] > 0.0) {
             lbUseForecast.setVisibility(TextView.VISIBLE);
             txUseForecastTotal.setVisibility(TextView.VISIBLE);
             txUseForecastTotal.setText(formatMoney(forecast[0]) + "/dia (total)");
             txUseForecastBusiness.setVisibility(TextView.VISIBLE);
             txUseForecastBusiness.setText(formatMoney(forecast[1]) + "/dia (útil)");
         } else {
             lbUseForecast.setVisibility(TextView.GONE);
             txUseForecastTotal.setVisibility(TextView.GONE);
             txUseForecastBusiness.setVisibility(TextView.GONE);
         }
     }
 
     private Double[] calculateUseForecast() {
         // forecast[0] = running days
         // forecast[1] = business days
         Double[] forecast = new Double[2];
         forecast[0] = 0.0;
         forecast[1] = 0.0;
         String nextDate = txNextDate.getText().toString();
         if(nextDate != null && nextDate.matches("\\d{2}/\\d{2}/\\d{4}")) {
             try {
                 Double balance = null;
                 if(ticket != null && ticket.getSchedulings().size() > 0) {
                     balance = ticket.getSeeBalance();
                 } else {
                     balance = Double.valueOf(txBalance.getText().toString().replace(",", ".").substring(3)).doubleValue();
                 }
                 Date last = new SimpleDateFormat("d/M/y").parse(txLast.getText().toString());
                 Date next = new SimpleDateFormat("d/M/y").parse(txNextDate.getText().toString());
 
                 // calculate based on running days
                 Double timeDiff = Double.valueOf(((next.getTime() - last.getTime())) / (1000*60*60*24));
                 forecast[0] = Math.round((balance / timeDiff) * 100.0) / 100.0;
 
                 // calculate based on business days
                 Calendar startCal = Calendar.getInstance();
                 Calendar endCal = Calendar.getInstance();
                 startCal.setTime(last);
                 endCal.setTime(next);
                 int workDays = 0;
                 if(startCal == endCal) {
                     forecast[1] = balance;
                 } else {
                     do {
                         if (startCal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY && startCal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                             workDays++;
                         }
                         startCal.add(Calendar.DAY_OF_MONTH, 1);
                     } while (startCal.getTimeInMillis() < endCal.getTimeInMillis());
                     forecast[1] = Math.round((balance / workDays) * 100.0) / 100.0;
                 }
             } catch (ParseException e) {
                 showError("Falha interna [1]");
                 Log.e(Constants.TAG, String.valueOf(e));
             } catch (NullPointerException e) {
                 showError("Falha interna [2]");
                 Log.e(Constants.TAG, String.valueOf(e));
             }
         }
         return forecast;
     }
 
     private class CheckBalance extends AsyncTask<Void, Void, Void> {
         @Override
         protected void onPreExecute() {
             btCheck.setEnabled(false);
             btCheck.setVisibility(View.GONE);
             btTransactions.setVisibility(View.GONE);
             etNumber.setFocusable(false);
             progress.setVisibility(View.VISIBLE);
             btDelete.setClickable(false);
         }
 
         @Override
         protected Void doInBackground(Void... arg0) {
             fetchBalance();
             return null;
         }
 
         @Override
         protected void onPostExecute(Void result) {
             btCheck.setEnabled(true);
             if(!fetchError) {
                 btTransactions.setVisibility(View.VISIBLE);
             }
             progress.setVisibility(View.GONE);
             btCheck.setVisibility(View.VISIBLE);
             etNumber.setFocusableInTouchMode(true);
             btDelete.setClickable(true);
         }
     }
 
     private void fetchBalance() {
         fetchError = false;
         strLast = "";
         strBalance = "";
         strNextDate = "";
         strNextIncrement = "";
         try {
             String ticketNumber = etNumber.getText().toString();
             HttpParams httpParameters = new BasicHttpParams();
             // Set the timeout in milliseconds until a connection is established.
             // The default value is zero, that means the timeout is not used.
             int timeoutConnection = 3000;
             HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
             // Set the default socket timeout (SO_TIMEOUT)
             // in milliseconds which is the timeout for waiting for data.
             int timeoutSocket = 5000;
             HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
             DefaultHttpClient httpclient = new DefaultHttpClient(httpParameters);
             HttpGet request = new HttpGet("http://www.ticket.com.br/ticket-corporativo-web/ticket-consultcard?chkProduto=Ticket+Restaurante&txtNumeroCartao=" + ticketNumber + "&txtOperacao=saldo_agendamentos&cardNumber=");
 
             InputStream inputStream = null;
             HttpResponse response = httpclient.execute(request);
             HttpEntity entity = response.getEntity();
 
             inputStream = entity.getContent();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
             StringBuilder sb = new StringBuilder();
 
             String line = null;
             while ((line = reader.readLine()) != null)
             {
                 sb.append(line + "\n");
             }
             String json = sb.toString();
 
             if(json.equals("")) {
                 showError("Falha na conexão [1]");
                 Log.e(Constants.TAG, "Result string is null.");
             } else {
                 Gson gson = JsonUtil.getGson();
                 try {
                     ticket = gson.fromJson(json, TicketRestaurante.class);
                 } catch (IllegalStateException e) {
                     showError("Falha na consulta (serviço remoto enviou dados inválidos) [1]");
                     Log.e(Constants.TAG, String.valueOf(e));
                 }
                 if(ticket.getMessageError() == null) {
                     try {
                         strLast = new SimpleDateFormat("dd/MM/yyyy").format(ticket.getConsultDate());
                     } catch (NullPointerException e) {
                         showError("Falha na consulta (serviço remoto dados inválidos) [2]");
                         Log.e(Constants.TAG, String.valueOf(e));
                     }
                     strBalance = formatMoney(ticket.getSeeBalance());
                    strNextDate = getResources().getString(R.string.info_unavailable);
                    strNextIncrement = "";
                    try {
                        List<TicketRestauranteSchedulings> schedulings;
                        schedulings = ticket.getSchedulings();
                        if(schedulings.size() > 0) {
                            strNextDate = new SimpleDateFormat("dd/MM/yyyy").format(schedulings.get(0).getDate());
                            strNextIncrement = formatMoney(schedulings.get(0).getValue());
                        }
                    } catch (NullPointerException e) {
                        // values have been set before
                     }
                 } else {
                     showError(ticket.getMessageError());
                     fetchError = true;
                 }
 
                 if(!fetchError && !settings.contains(ticketNumber) && settings.getBoolean("pref_keepHistory", false)) {
                     // save current ticket number with a generic fake name
                     settingsEditor = settings.edit();
                     settingsEditor.putString(ticketNumber, "saved");
                     settingsEditor.commit();
                 }
 
                 runOnUiThread(new Runnable() {
                     @Override
                     public void run() {
                         txLast.setText(strLast);
                         txBalance.setText(strBalance);
                         txNextDate.setText(strNextDate);
                         txNextIncrement.setText(strNextIncrement);
                         updateForecast();
                         refreshTicketListAdapter();
                     }
                 });
             }
 
             runOnUiThread(new Runnable() {
                 @Override
                 public void run() {
 
                 }
             });
         } catch (ClientProtocolException e) {
             showError("Falha na conexão [2]");
             Log.e(Constants.TAG, String.valueOf(e));
         } catch (UnknownHostException e) {
             showError("Falha na conexão [3]");
             Log.e(Constants.TAG, String.valueOf(e));
         } catch (IOException e) {
             showError("Falha na conexão [4]");
             Log.e(Constants.TAG, String.valueOf(e));
         }
     }
 
     private void refreshTicketListAdapter() {
         if(settings.getBoolean("pref_keepHistory", false)) {
             // assume this will be disabled by default
             ticketNumberHistoryAdapter = new ArrayAdapter<String>(
                     MainActivity.this,
                     android.R.layout.simple_dropdown_item_1line,
                     getTicketNumberList());
             etNumber.setAdapter(ticketNumberHistoryAdapter);
             etNumber.setThreshold(0);
         } else {
             etNumber.setAdapter(null);
         }
     }
 
     private List<String> getTicketNumberList() {
         List<String> ticketNumberList = new ArrayList<String>();
         String[] confKeys = settings.getAll().keySet().toArray(new String[0]);
         for(int i = 0; i < confKeys.length; i++) {
             try {
                 if(confKeys[i].matches("\\d{16}")) {
                     ticketNumberList.add(confKeys[i]);
                 }
             } catch (ClassCastException e) {
                 // some keys are not really strings, skip them
                 continue;
             }
         }
         return ticketNumberList;
     }
 }
