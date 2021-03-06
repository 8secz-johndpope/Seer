 /**
  * Hubroid - A GitHub app for Android
  * 
  * Copyright (c) 2010 Eddie Ringle.
  * 
  * Licensed under the New BSD License.
  */
 
 package org.idlesoft.android.hubroid;
 
 import java.io.File;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.net.URLEncoder;
 
 import org.json.JSONArray;
 import org.json.JSONException;
import org.json.JSONObject;
 
 import android.app.Activity;
 import android.app.ProgressDialog;
 import android.content.Intent;
 import android.content.SharedPreferences;
 import android.os.Bundle;
 import android.os.Environment;
 import android.view.Menu;
 import android.view.MenuItem;
 import android.view.View;
 import android.view.View.OnClickListener;
 import android.widget.AdapterView;
 import android.widget.Button;
 import android.widget.EditText;
 import android.widget.ListView;
 import android.widget.TextView;
 import android.widget.Toast;
 import android.widget.AdapterView.OnItemClickListener;
 
 public class Search extends Activity {
 	private RepositoriesListAdapter m_repositories_adapter;
 	private SearchUsersListAdapter m_users_adapter;
 	public ProgressDialog m_progressDialog;
 	private SharedPreferences m_prefs;
 	private SharedPreferences.Editor m_editor;
 	public String m_username;
 	public String m_type;
 	public JSONArray m_repositoriesData;
 	public JSONArray m_usersData;
 	public Intent m_intent;
 	public int m_position;
 
 	public void initializeList() {
 		try {
 			if (m_type == "repositories") {
 				URL query = new URL(
 						"http://github.com/api/v2/json/repos/search/"
 								+ URLEncoder
 										.encode(((EditText) findViewById(R.id.et_search_search_box))
 												.getText().toString()));
 
				JSONObject response = Hubroid.make_api_request(query);
 
				if (response == null) {
 					runOnUiThread(new Runnable() {
 						public void run() {
 							Toast
 									.makeText(
 											Search.this,
 											"Error gathering repository data, please try again.",
 											Toast.LENGTH_SHORT).show();
 						}
 					});
 				} else {
					m_repositoriesData = response.getJSONArray("repositories");
 					m_repositories_adapter = new RepositoriesListAdapter(
 							getApplicationContext(), m_repositoriesData);
 				}
 			} else if (m_type == "users") {
 				URL query = new URL(
 						"http://github.com/api/v2/json/user/search/"
 								+ URLEncoder
 										.encode(((EditText) findViewById(R.id.et_search_search_box))
 												.getText().toString()));
 
				JSONObject response = Hubroid.make_api_request(query);
 
				if (response == null) {
 					runOnUiThread(new Runnable() {
 						public void run() {
 							Toast
 									.makeText(
 											Search.this,
 											"Error gathering user data, please try again.",
 											Toast.LENGTH_SHORT).show();
 						}
 					});
 				} else {
					m_usersData = response.getJSONArray("users");
 					m_users_adapter = new SearchUsersListAdapter(
 							getApplicationContext(), m_usersData);
 				}
 			}
 
 		} catch (MalformedURLException e) {
 			e.printStackTrace();
 		} catch (JSONException e) {
 			e.printStackTrace();
 		}
 	}
 
 	private Runnable threadProc_itemClick = new Runnable() {
 		public void run() {
 			try {
 				if (m_type == "repositories") {
 					m_intent = new Intent(Search.this, RepositoryInfo.class);
 					m_intent.putExtra("repo_name", m_repositoriesData
 							.getJSONObject(m_position).getString("name"));
 					m_intent.putExtra("username", m_repositoriesData
 							.getJSONObject(m_position).getString("username"));
 				} else if (m_type == "users") {
 					m_intent = new Intent(Search.this, UserInfo.class);
 					m_intent.putExtra("username", m_usersData.getJSONObject(
 							m_position).getString("username"));
 				}
 			} catch (JSONException e) {
 				e.printStackTrace();
 			}
 
 			runOnUiThread(new Runnable() {
 				public void run() {
 					Search.this.startActivityForResult(m_intent, 5005);
 				}
 			});
 		}
 	};
 
 	protected void onActivityResult(int requestCode, int resultCode, Intent data)
 	{
 		if (resultCode == 5005) {
 			Toast.makeText(Search.this, "That user has recently been deleted.", Toast.LENGTH_SHORT).show();
 		}
 	}
 
 	public void toggleList(String type) {
 		ListView repositoriesList = (ListView) findViewById(R.id.lv_search_repositories_list);
 		ListView usersList = (ListView) findViewById(R.id.lv_search_users_list);
 		TextView title = (TextView) findViewById(R.id.tv_top_bar_title);
 
 		if (type == "" || type == null) {
 			type = (m_type == "repositories") ? "users" : "repositories";
 		}
 		m_type = type;
 
 		if (m_type == "repositories") {
 			repositoriesList.setVisibility(View.VISIBLE);
 			usersList.setVisibility(View.GONE);
 			title.setText("Search Repositories");
 		} else if (m_type == "users") {
 			usersList.setVisibility(View.VISIBLE);
 			repositoriesList.setVisibility(View.GONE);
 			title.setText("Search Users");
 		}
 	}
 
 	private OnClickListener m_btnSearchListener = new OnClickListener() {
 		public void onClick(View v) {
 			EditText search_box = (EditText) findViewById(R.id.et_search_search_box);
 			if (search_box.getText().toString() != "") {
 				if (m_type == "repositories") {
 					m_progressDialog = ProgressDialog
 							.show(Search.this, "Please wait...",
 									"Searching Repositories...", true);
 					Thread thread = new Thread(new Runnable() {
 						public void run() {
 							initializeList();
 							runOnUiThread(new Runnable() {
 								public void run() {
 									((ListView)findViewById(R.id.lv_search_repositories_list)).setAdapter(m_repositories_adapter);
 									m_progressDialog.dismiss();
 								}
 							});
 						}
 					});
 					thread.start();
 				} else if (m_type == "users") {
 					m_progressDialog = ProgressDialog.show(Search.this,
 							"Please wait...", "Searching Users...", true);
 					Thread thread = new Thread(new Runnable() {
 						public void run() {
 							initializeList();
 							runOnUiThread(new Runnable() {
 								public void run() {
 									((ListView)findViewById(R.id.lv_search_users_list)).setAdapter(m_users_adapter);
 									m_progressDialog.dismiss();
 								}
 							});
 						}
 					});
 					thread.start();
 				}
 			}
 		}
 	};
 
 	private OnClickListener onButtonToggleClickListener = new OnClickListener() {
 		public void onClick(View v) {
 			if (v.getId() == R.id.btn_search_repositories) {
 				toggleList("repositories");
 				m_type = "repositories";
 			} else if (v.getId() == R.id.btn_search_users) {
 				toggleList("users");
 				m_type = "users";
 			}
 		}
 	};
 
 	private OnItemClickListener m_MessageClickedHandler = new OnItemClickListener() {
 		public void onItemClick(AdapterView<?> parent, View v, int position,
 				long id) {
 			m_position = position;
 			Thread thread = new Thread(null, threadProc_itemClick);
 			thread.start();
 		}
 	};
 
 	public boolean onPrepareOptionsMenu(Menu menu) {
 		if (!menu.hasVisibleItems()) {
 			menu.add(0, 0, 0, "Back to Main").setIcon(android.R.drawable.ic_menu_revert);
 			menu.add(0, 1, 0, "Clear Preferences");
 			menu.add(0, 2, 0, "Clear Cache");
 		}
 		return true;
 	}
 
 	public boolean onOptionsItemSelected(MenuItem item) {
 		switch (item.getItemId()) {
 		case 0:
 			Intent i1 = new Intent(this, Hubroid.class);
 			startActivity(i1);
 			return true;
 		case 1:
 			m_editor.clear().commit();
 			Intent intent = new Intent(this, Hubroid.class);
 			startActivity(intent);
         	return true;
 		case 2:
 			File root = Environment.getExternalStorageDirectory();
 			if (root.canWrite()) {
 				File hubroid = new File(root, "hubroid");
 				if (!hubroid.exists() && !hubroid.isDirectory()) {
 					return true;
 				} else {
 					hubroid.delete();
 					return true;
 				}
 			}
 		}
 		return false;
 	}
 
 	@Override
 	public void onCreate(Bundle icicle) {
 		super.onCreate(icicle);
 		setContentView(R.layout.search);
 
 		m_prefs = getSharedPreferences(Hubroid.PREFS_NAME, 0);
 		m_editor = m_prefs.edit();
 		m_type = "repositories";
 
 		Bundle extras = getIntent().getExtras();
 		if (extras != null) {
 			if (extras.containsKey("username")) {
 				m_username = icicle.getString("username");
 			} else {
 				m_username = m_prefs.getString("login", "");
 			}
 		} else {
 			m_username = m_prefs.getString("login", "");
 		}
 	}
 
 	@Override
 	public void onStart() {
 		super.onStart();
 
 		((Button) findViewById(R.id.btn_search_repositories))
 				.setOnClickListener(onButtonToggleClickListener);
 		((Button) findViewById(R.id.btn_search_users))
 				.setOnClickListener(onButtonToggleClickListener);
 		((Button) findViewById(R.id.btn_search_go))
 				.setOnClickListener(m_btnSearchListener);
 
 		((ListView) findViewById(R.id.lv_search_repositories_list))
 				.setOnItemClickListener(m_MessageClickedHandler);
 		((ListView) findViewById(R.id.lv_search_users_list))
 				.setOnItemClickListener(m_MessageClickedHandler);
 	}
 
 	@Override
 	public void onSaveInstanceState(Bundle savedInstanceState) {
 		savedInstanceState.putString("type", m_type);
 		if (m_repositoriesData != null) {
 			savedInstanceState.putString("repositories_json",
 					m_repositoriesData.toString());
 		}
 		if (m_usersData != null) {
 			savedInstanceState.putString("users_json", m_usersData.toString());
 		}
 		super.onSaveInstanceState(savedInstanceState);
 	}
 
 	@Override
 	public void onRestoreInstanceState(Bundle savedInstanceState) {
 		super.onRestoreInstanceState(savedInstanceState);
 		m_type = savedInstanceState.getString("type");
 		try {
 			if (savedInstanceState.containsKey("repositories_json")) {
 				m_repositoriesData = new JSONArray(savedInstanceState
 						.getString("repositories_json"));
 			} else {
 				m_repositoriesData = new JSONArray();
 			}
 		} catch (JSONException e) {
 			m_repositoriesData = new JSONArray();
 		}
 		try {
 			if (savedInstanceState.containsKey("users_json")) {
 				m_usersData = new JSONArray(savedInstanceState
 						.getString("users_json"));
 			} else {
 				m_usersData = new JSONArray();
 			}
 		} catch (JSONException e) {
 			m_usersData = new JSONArray();
 		}
  		if (m_repositoriesData.length() > 0) {
 			m_repositories_adapter = new RepositoriesListAdapter(
 					Search.this, m_repositoriesData);
 		} else {
 			m_repositories_adapter = null;
 		}
  		if (m_usersData.length() > 0) {
  			m_users_adapter = new SearchUsersListAdapter(Search.this, m_usersData);
  		} else {
  			m_users_adapter = null;
  		}
 	}
 
 	@Override
 	public void onResume() {
 		super.onResume();
 		ListView repositories = (ListView) findViewById(R.id.lv_search_repositories_list);
 		ListView users = (ListView) findViewById(R.id.lv_search_users_list);
 
 		repositories.setAdapter(m_repositories_adapter);
 		users.setAdapter(m_users_adapter);
 		toggleList(m_type);
 	}
 }
