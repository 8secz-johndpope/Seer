 package com.ehb.questiontime;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 
 import javax.xml.parsers.ParserConfigurationException;
 import javax.xml.parsers.SAXParser;
 import javax.xml.parsers.SAXParserFactory;
 
 import org.json.JSONObject;
 import org.xml.sax.SAXException;
 import org.xml.sax.XMLReader;
 
 import android.app.Activity;
 import android.app.AlertDialog;
 import android.app.ProgressDialog;
 import android.content.DialogInterface;
 import android.content.DialogInterface.OnCancelListener;
 import android.content.Intent;
 import android.os.Bundle;
 import android.util.Log;
 import android.util.Xml;
 import android.view.ContextMenu;
 import android.view.ContextMenu.ContextMenuInfo;
 import android.view.Menu;
 import android.view.MenuItem;
 import android.view.View;
 import android.view.View.OnCreateContextMenuListener;
 import android.widget.AdapterView;
 import android.widget.AdapterView.OnItemClickListener;
 import android.widget.ListView;
 import android.widget.SimpleAdapter;
 import android.widget.Toast;
 
 import com.loopj.android.http.AsyncHttpResponseHandler;
 import com.loopj.android.http.JsonHttpResponseHandler;
 import com.loopj.android.http.RequestParams;
 
 public class QuestionTab extends Activity {
 	/** XML node keys **/
 	static final String KEY_ROW = "row"; // parent node
 	static final String KEY_DATA = "data";
 	static final String KEY_ID = "ID";
 	static final String KEY_QUESTION = "QUESTION";
 	static final String KEY_ISOPEN = "ISOPEN";
 	static final String KEY_TEACHERID = "TEACHERID";
 	static final String KEY_ISPUSHED = "ISPUSHED";
 
 	private ListView myListview;
	// private SearchView mySearchView;
 
 	private static final int DELETE_ID = Menu.FIRST;
 	private static final int PUSH_ID = Menu.FIRST + 1;
 
 	public Question aQuestion;
 	public ArrayList<Question> questions = new ArrayList<Question>();
 
 	@Override
 	public void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 		setContentView(R.layout.question_tab);
 
 		questionListing();
 
 	}
 
 	/** method to refresh the question list via the menu **/
 
 	public void refreshQuestionList() {
 
 		questionListing();
 
 	}
 
 	/** method to refresh the question list via the button **/
 
 	public void refreshQuestionList(View v) {
 
 		questionListing();
 
 	}
 
 	public void questionListing() {
 		RestClient.get("questions.xml", null, new AsyncHttpResponseHandler() {
 
 			private ProgressDialog dialog;
 
 			@Override
 			public void onStart() {
 				dialog = ProgressDialog.show(getParent(), "Loading",
 						"Data loading", true, true, new OnCancelListener() {
 							public void onCancel(DialogInterface dialog) {
 								dialog.dismiss();
 							}
 						});
 			}
 
 			@Override
 			public void onSuccess(String response) {
 				if (this.dialog.isShowing())
 					this.dialog.dismiss();
 
 				ArrayList<HashMap<String, Object>> listItem = new ArrayList<HashMap<String, Object>>();
 
 				/** create an instance of our parser */
 
 				QuestionParser parser = new QuestionParser();
 
 				SAXParserFactory factory = SAXParserFactory.newInstance();
 				SAXParser sp = null;
 				try {
 					sp = factory.newSAXParser();
 				} catch (ParserConfigurationException e1) {
 					e1.printStackTrace();
 				} catch (SAXException e1) {
 					e1.printStackTrace();
 				}
 
 				XMLReader reader = null;
 				try {
 					reader = sp.getXMLReader();
 				} catch (SAXException e1) {
 					e1.printStackTrace();
 				}
 
 				// add our own parser to the xml reader and start parsing
 				// the
 				// file
 				reader.setContentHandler(parser);
 				try {
 					Xml.parse(response, parser);
 				} catch (SAXException e) {
 					e.printStackTrace();
 				}
 				questions = parser.questions;
 				// Log.d("demo", "Questions in the getQuestions()= " +
 				// questions);
 
 				for (int i = 0; i < questions.size(); i++) {
 					Question temp = questions.get(i);
 
 					HashMap<String, Object> map = new HashMap<String, Object>();
 
 					map.put(KEY_QUESTION, temp.questionText);
 
 					listItem.add(map);
 				}
 
 				myListview = (ListView) findViewById(R.id.listViewTabVragen);
 
 				SimpleAdapter adapter = new SimpleAdapter(QuestionTab.this,
 						listItem, R.layout.list_item_question,
 						new String[] { KEY_QUESTION },
 						new int[] { R.id.vragenTextTextView });
 
 				myListview.setAdapter(adapter);
 
 				/** to create a Context Menu for the "long press" **/
 				myListview
 						.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
 
 							/** long press to delete ? .... **/
 							public void onCreateContextMenu(ContextMenu menu,
 									View v, ContextMenuInfo menuInfo) {
 								// super.onCreateContextMenu(menu, v, menuInfo);
 								menu.setHeaderTitle("CONTEXT MENU");
 								menu.add(0, DELETE_ID, 0, R.string.menu_delete);
 								menu.add(0, PUSH_ID, 0, R.string.push);
 							}
 						});
 				myListview.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
 				/** click on an item in the listView **/
 
 				myListview.setOnItemClickListener(new OnItemClickListener() {
 
 					/** click on a row **/
 					public void onItemClick(AdapterView<?> arg0, View view,
 							int position, long arg3) {
 
 						// Log.w("TAG", "onItemClick clicked position :"+
 						// position);
 
 						Intent intent = new Intent(getParent(),
 								QuestionDetails.class);
 
 						intent.putExtra(KEY_QUESTION,
 								questions.get(position).questionText);
 						// Log.d("demo","question text="+
 						// questions.get(position).questionText);
 
 						/* to retrieve the id of the question */
 
 						intent.putExtra(KEY_ID,
 								questions.get(position).ID.toString());
 
 						TabGroupActivity parentactivity = (TabGroupActivity) getParent();
 						parentactivity.startChildActivity("VragenDetails",
 								intent);
 
 					}
 
 				});
 
 				System.out.println(response);
 			}
 		});
 
 	}
 
 	public void onRefresh(View v) {
 		questionListing();
 	}
 
 	/** one of the Context Item is selected **/
 	@Override
 	public boolean onContextItemSelected(MenuItem item) {
 
 		// to find the item of the list view that was clicked
 		// AdapterView.AdapterContextMenuInfo info =
 		// (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
 		// int index=info.position;
 
 		switch (item.getItemId()) {
 		case DELETE_ID:
 
 			AlertDialog alertDialog = new AlertDialog.Builder(getParent())
 					.create();
 			alertDialog.setTitle("Verwijderen...");
 			alertDialog.setMessage("Bent U zeker ?");
 			alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
 				public void onClick(DialogInterface dialog, int which) {
 					// here you can add functions
 				}
 			});
 			alertDialog.setButton2("Cancel",
 					new DialogInterface.OnClickListener() {
 						public void onClick(DialogInterface dialog, int which) {
 							return;
 						}
 					});
 
 			alertDialog.setIcon(R.drawable.logo_test2);
 			alertDialog.show();
 			// Toast.makeText(getApplicationContext(), "Delete the question",
 			// Toast.LENGTH_LONG).show();
 			break;
 
 		case PUSH_ID:
 
 			Push(item);
 		}
 
 		return super.onContextItemSelected(item);
 	}
 
 	/** onClick - button "Add Question" **/
	public void onAddClick(View v) {
 		// Intent i = new Intent(VragenTab.this, VragenDetails.class);
 		// startActivityForResult(i, ACTIVITY_DETAIL);
 		Intent intent = new Intent(getParent(), QuestionDetails.class);
 		TabGroupActivity parentactivity = (TabGroupActivity) getParent();
 		parentactivity.startChildActivity("QuestionDetails", intent);
 
 	}
 
 	// Called with the result of the other activity
 	// requestCode was the origin request code send to the activity
 	// resultCode is the return code, 0 is everything is ok
 	// intend can be used to get data
 	@Override
 	protected void onActivityResult(int requestCode, int resultCode,
 			Intent intent) {
 		super.onActivityResult(requestCode, resultCode, intent);
 	}
 
 	/** the push button clicker **/
 
 	public void onPushButtonClick(View v) {
 		Toast.makeText(getApplicationContext(), "Push the question",
 				Toast.LENGTH_LONG).show();
 		// Push(MenuItem item);
 
 	}
 
 	public void Push(MenuItem item) {
 
 		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
 				.getMenuInfo();
 		int index = info.position;
 
 		Log.d("demo", "index= " + index);
 		Log.d("demo", "getItemId()= " + item.getItemId());
 
 		RequestParams params = new RequestParams();
 
 		// String id = "" + item.getItemId();// Als we weten welke vraag het is
 		String id = "" + index;
 
 		params.put("question[ispushed]", "1");
 
 		RestClient.put("questions/" + id, params,
 				new AsyncHttpResponseHandler() {
 					private ProgressDialog dialog;
 
 					@Override
 					public void onStart() {
 						dialog = ProgressDialog.show(getParent(), "Loading",
 								"Data loading", true, true,
 								new OnCancelListener() {
 									public void onCancel(DialogInterface dialog) {
 										dialog.dismiss();
 									}
 								});
 						
 					}
 
 					@Override
 					public void onSuccess(String response) {
 						
 						Log.d("success", "pushed");
 					}
 				});
 
 	}
 
 }
