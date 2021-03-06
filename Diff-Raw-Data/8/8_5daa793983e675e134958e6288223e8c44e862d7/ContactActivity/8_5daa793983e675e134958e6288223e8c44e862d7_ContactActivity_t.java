 package com.anibug.smsmanager;
 
 import java.util.List;
 
 import android.app.ListActivity;
 import android.content.Intent;
 import android.os.Bundle;
 import android.util.Log;
 import android.view.ContextMenu;
 import android.view.ContextMenu.ContextMenuInfo;
 import android.view.Menu;
 import android.view.MenuItem;
 import android.view.View;
 import android.widget.AdapterView;
 
 import com.anibug.smsmanager.adapter.ContactArrayAdapter;
 import com.anibug.smsmanager.model.Contact;
 import com.anibug.smsmanager.model.ContactManager;
 
 public class ContactActivity extends ListActivity {
 	ContactManager contactManager;
 	
 	@Override
 	protected void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 		setTitle("Contacts");
 
 		contactManager = new ContactManager(this);
 
 		updateList();
 	}
 
 	private void updateList() {
 		List<Contact> contacts = contactManager.fetchAll();
 		setListAdapter(new ContactArrayAdapter(this, contacts));
 		getListView().setOnCreateContextMenuListener(this);
 	}
 
 	@Override
 	public boolean onCreateOptionsMenu(Menu menu) {
 		getMenuInflater().inflate(R.menu.contact_list_options, menu);
 		return super.onCreateOptionsMenu(menu);
 	}
 
 	@Override
 	public boolean onOptionsItemSelected(MenuItem item) {
 		switch (item.getItemId()) {
 		case R.id.item_new_contact:
 			showEditingDialog();
 			break;
 		default:
 			assert false;
 			break;
 		}
 
 		return true;
 	}
 
 	private void showEditingDialog() {
 		  Intent intent = new Intent(this, ContactEditActivity.class);
 		  startActivityForResult(intent, ContactEditActivity.NEW_CONTACT);
 	}
 	
 	@Override
 	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
 		if (resultCode != RESULT_OK)
 			return;
 
 		switch (requestCode) {
 		case ContactEditActivity.NEW_CONTACT:
 			updateList();
 			break;
 		default:
 			assert false;
 			break;
 		}
 	}
 
 	
 	private int positionClicked = -1;
 
	private final int MENU_ITEM_REMOVE = 1;
 	// TODO: make contact editable. 
 	// private final int MENU_ITEM_EDIT = 2;
 
 	@Override
 	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
 		super.onCreateContextMenu(menu, v, menuInfo);
 
 		// TODO: Put the name in title.
 		menu.setHeaderTitle("Contact");  
 
 		try {
 			// Save the position and recall it when item clicked.
 			AdapterView.AdapterContextMenuInfo info;
 		    info = (AdapterView.AdapterContextMenuInfo) menuInfo;
 		    positionClicked = info.position;
 		} catch (ClassCastException e) {
 		    Log.e(getClass().getName(), "bad menuInfo", e);
 		    return;
 		}
 
		menu.add(Menu.NONE, MENU_ITEM_REMOVE, Menu.NONE, "Remove");
 	}
 
     @Override
     public boolean onContextItemSelected(MenuItem item) {
     	switch (item.getItemId()) {
		case MENU_ITEM_REMOVE:
 			Contact selected = (Contact) getListAdapter().getItem(positionClicked);
 			contactManager.delete(selected);
 			updateList();
 			return false;
 		default:
 			assert false;
 			return true;
 		}
     }
 }
