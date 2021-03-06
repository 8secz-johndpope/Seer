 package com.uservoice.helpcenter;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import android.graphics.Color;
 import android.view.*;
 import android.widget.AbsListView;
 import org.json.JSONArray;
 import org.json.JSONException;
 import org.json.JSONObject;
 
 import com.uservoice.uservoicesdk.Config;
 import com.uservoice.uservoicesdk.Session;
 import com.uservoice.uservoicesdk.UserVoice;
 import com.uservoice.uservoicesdk.model.ClientConfig;
 import com.uservoice.uservoicesdk.ui.DefaultCallback;
 
 import android.content.Context;
 import android.content.SharedPreferences;
 import android.content.SharedPreferences.Editor;
 import android.support.v4.app.FragmentActivity;
 import android.util.Log;
 import android.widget.AdapterView;
 import android.widget.BaseAdapter;
 import android.widget.TextView;
 
 public class MainAdapter extends BaseAdapter implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, ActionMode.Callback {
 
 	private static int HEADER = 0;
 	private static int ACCOUNT = 1;
 	private static int ADD = 2;
     private final AbsListView listView;
     private FragmentActivity context;
 	private LayoutInflater inflater;
 	private List<Map<String, String>> accounts;
 	private Map<String,String> activeAccount;
     private List<Map<String,String>> selectedAccounts = new ArrayList<Map<String, String>>();
     private ActionMode actionMode;
     private List<View> selectedViews = new ArrayList<View>();
 
     public MainAdapter(FragmentActivity context, AbsListView listView) {
 		this.context = context;
         this.listView = listView;
 		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
 		loadAccounts();
 	}
 	
 	private void loadAccounts() {
 		accounts = new ArrayList<Map<String,String>>();
 		SharedPreferences prefs = context.getSharedPreferences("uv_help_center", 0);
 		if (prefs.contains("accounts")) {
 			try {
 				JSONArray array = new JSONArray(prefs.getString("accounts", null));
 				for (int i=0; i<array.length(); i++) {
 					JSONObject obj = array.getJSONObject(i);
 					Map<String,String> map = new HashMap<String,String>();
 					map.put("name", obj.getString("name"));
 					map.put("subdomain", obj.getString("subdomain"));
 					accounts.add(map);
 				}
 			} catch (JSONException e) {
 			}
 		} else {
 			Map<String,String> map = new HashMap<String,String>();
 			map.put("name", "UserVoice");
 			map.put("subdomain", "feedback.uservoice.com");
 			accounts.add(map);
 			saveAccounts();
 		}
 	}
 	
 	private void saveAccounts() {
 		JSONArray array = new JSONArray();
 		for (Map<String,String> map : accounts) {
 			JSONObject obj = new JSONObject(map);
 			array.put(obj);
 		}
 		SharedPreferences prefs = context.getSharedPreferences("uv_help_center", 0);
 		Editor edit = prefs.edit();
 		edit.putString("accounts", array.toString());
 		edit.commit();
 	}
 
 	@Override
 	public int getCount() {
 		return 2 + accounts.size();
 	}
 
 	@Override
 	public Object getItem(int position) {
 		return position > 0 && position < accounts.size() + 1 ? accounts.get(position - 1) : null;
 	}
 
 	@Override
 	public long getItemId(int position) {
 		return position;
 	}
 
 	@Override
 	public int getItemViewType(int position) {
 		if (position == 0)
 			return HEADER;
 		if (position < accounts.size() + 1)
 			return ACCOUNT;
         if (position == accounts.size() + 1)
 	    	return ADD;
         return HEADER;
 	}
 
 	@Override
 	public int getViewTypeCount() {
 		return 3;
 	}
 
 	@Override
 	public View getView(int position, View convertView, ViewGroup parent) {
 		View view = convertView;
 		int type = getItemViewType(position);
 		if (view == null) {
 			if (type == HEADER)
 				view = inflater.inflate(com.uservoice.uservoicesdk.R.layout.header_item_light, null);
 			else if (type == ACCOUNT)
 				view = inflater.inflate(com.uservoice.uservoicesdk.R.layout.text_item, null);
 			else if (type == ADD)
 				view = inflater.inflate(com.uservoice.uservoicesdk.R.layout.text_item, null);
 		}
 
 		if (type == HEADER) {
 			TextView text = (TextView) view.findViewById(com.uservoice.uservoicesdk.R.id.header_text);
 			text.setText(R.string.select_an_account);
 		} else if (type == ACCOUNT) {
 			@SuppressWarnings("unchecked")
 			Map<String,String> account = (Map<String,String>) getItem(position);
 			TextView name = (TextView) view.findViewById(com.uservoice.uservoicesdk.R.id.text);
 			TextView subdomain = (TextView) view.findViewById(com.uservoice.uservoicesdk.R.id.text2);
 			name.setText(account.get("name"));
 			subdomain.setText(account.get("subdomain"));
 		} else if (type == ADD) {
 			TextView text = (TextView) view.findViewById(com.uservoice.uservoicesdk.R.id.text);
 			text.setText(R.string.add_account);
 			view.findViewById(com.uservoice.uservoicesdk.R.id.text2).setVisibility(View.GONE);
 			view.findViewById(com.uservoice.uservoicesdk.R.id.divider).setVisibility(View.GONE);
 		}
 
 		return view;
 	}
 	
 	@Override
 	public boolean isEnabled(int position) {
 		return position > 0 && position < getCount();
 	}
 	
 	public void addAccount(final String subdomain) {
 		// TODO maybe add a loading cell
 		activeAccount = null;
 		Config config = new Config(subdomain);
 		UserVoice.init(config, context);
 		ClientConfig.loadClientConfig(new DefaultCallback<ClientConfig>(context) {
 			@Override
 			public void onModel(ClientConfig model) {
 				Session.getInstance().setClientConfig(model);
 				Map<String,String> account = new HashMap<String,String>();
 				account.put("subdomain", subdomain);
 				account.put("name", model.getAccountName());
 				accounts.add(account);
 				saveAccounts();
 				activeAccount = account;
                 notifyDataSetChanged();
 			}
 		});
 	}
 
 	@Override
 	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
 		int type = getItemViewType(position);
 		if (type == ADD) {
 			AccountDialogFragment dialog = new AccountDialogFragment(this);
 			dialog.show(context.getSupportFragmentManager(), "AccountDialogFragment");
 		} else if (type == ACCOUNT) {
 			@SuppressWarnings("unchecked")
 			Map<String,String> account = (Map<String, String>) getItem(position);
 			if (activeAccount != account) {
 				Config config = new Config(account.get("subdomain"));
 				UserVoice.init(config, context);
 				activeAccount = account;
 			}
 			UserVoice.launchUserVoice(context);
 		}
 	}
 
     @Override
     @SuppressWarnings("unchecked")
     public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
         int type = getItemViewType(position);
         if (type == ACCOUNT) {
             if (actionMode == null)
                 actionMode = context.startActionMode(this);
            if (listView.isItemChecked(position)) {
                selectedAccounts.remove((Map<String, String>) getItem(position));
                 selectedViews.remove(view);
                 view.setBackgroundColor(Color.TRANSPARENT);
                listView.setItemChecked(position, false);
             } else {
                selectedAccounts.add((Map<String, String>) getItem(position));
                 selectedViews.add(view);
                 view.setBackgroundColor(Color.rgb(51, 181, 229));
                listView.setItemChecked(position, true);
             }
             return true;
         }
         return false;
     }
 
     @Override
     public boolean onCreateActionMode(ActionMode mode, Menu menu) {
         MenuInflater inflater = mode.getMenuInflater();
         inflater.inflate(R.menu.context_menu, menu);
         return true;
     }
 
     @Override
     public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
         return false;
     }
 
     @Override
     public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
         switch (item.getItemId()) {
             case R.id.delete:
                 accounts.removeAll(selectedAccounts);
                 saveAccounts();
                 notifyDataSetChanged();
                 mode.finish();
                 return true;
             default:
                 return false;
         }
     }
 
     @Override
     public void onDestroyActionMode(ActionMode mode) {
         for (View view : selectedViews) {
             view.setBackgroundColor(Color.TRANSPARENT);
         }
         selectedViews.clear();
         selectedAccounts.clear();
         actionMode = null;
     }
 }
