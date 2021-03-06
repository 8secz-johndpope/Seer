 package com.nickotter.randomname;
  
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 
 import com.navdrawer.SimpleSideDrawer;
 
 import android.app.ExpandableListActivity;
 import android.os.Bundle;
 import android.util.Log;
 import android.view.View;
 import android.widget.ExpandableListView;
 import android.widget.SimpleExpandableListAdapter;
 import android.app.Activity;
 import android.view.Menu;
 import android.view.View.OnClickListener;
  
 public class DrawerTestMainActivity extends ExpandableListActivity
 {
 	//removed activity extension
 
 	final String LOGTAG = "DrawerTestMainActivity";
 	
     private SimpleSideDrawer mNav;
     
     CRUD databaseCRUD = null;
     
     
     @Override
     public void onCreate(Bundle savedInstanceState) 
     {
     	
     	
     	//load activity
         super.onCreate(savedInstanceState);
         setContentView(R.layout.drawer_example_activity_main);
         
         
         //Setup Database
         //Log.v(LOGTAG, "Deleting DATABASE_NAME="+ Sqlite.DATABASE_NAME);
 		this.deleteDatabase(Sqlite.DATABASE_NAME);
 		Log.v(LOGTAG, "Initializing CRUD object");
 		databaseCRUD = new CRUD(this);
 		Log.v(LOGTAG, "opening database connection in CRUD object");
 		databaseCRUD.open();
 		
 		//Setup Sidedrawer
 		Log.v(LOGTAG, "Setting up side drawer");
         mNav = new SimpleSideDrawer(this);
         mNav.setBehindContentView(R.layout.drawer_example_activity_behind);
         
         //Open drawer key listener
         Log.v(LOGTAG, "Sidedrawer button init");
         findViewById(R.id.btn).setOnClickListener
         (new OnClickListener() 
         	{
             	@Override 
             	public void onClick(View v) 
             	{
             		mNav.toggleDrawer();
             	};
         	}   
         );
         
         //Exclusion toggle listener]
         Log.v(LOGTAG, "Intitializing the toggle listeners");
         findViewById(R.id.toggleExclusion).setOnClickListener
         (new OnClickListener() 
         	{
             	@Override 
             	public void onClick(View v) 
             	{
             		//databaseCRUD.exclusionShift();
             	};
         	}   
         );
         
         //Verbalizer toggle listener
         findViewById(R.id.toggleVerbalize).setOnClickListener
         (new OnClickListener() 
         	{
             	@Override 
             	public void onClick(View v) 
             	{
             		//databaseCRUD.verbalShift();
             	};
         	}   
         );
         
         //Shake randomizer toggle listener
         findViewById(R.id.toggleShaker).setOnClickListener
         (new OnClickListener() 
         	{
             	@Override 
             	public void onClick(View v) 
             	{
             		//databaseCRUD.shakeShift();
             	};
         	}   
         );
         
         
        //Create fake database
         Log.v(LOGTAG, "Initializing fake database");
         Group g1 = new Group("CS 480");
         Group g2 = new Group("CS 481");
         Group g3 = new Group("CS 482");
   	
         databaseCRUD.add_group(g1);
         databaseCRUD.add_group(g2);
         databaseCRUD.add_group(g3);
         Log.v(LOGTAG, "Groups initialized");
   	
 		MyList l1 = new MyList(1, g1.getID(), "List 1");
 		MyList l2 = new MyList(2, g1.getID(), "List 2");
 		MyList l3 = new MyList(3, g2.getID(), "List 1");
 		MyList l4 = new MyList(4, g2.getID(), "List 2");
 		MyList l5 = new MyList(5, g3.getID(), "List 1");
 		MyList l6 = new MyList(6, g3.getID(), "List 2");
 		  
 		databaseCRUD.add_list(g1, l1);
 		databaseCRUD.add_list(g1, l2);
 		databaseCRUD.add_list(g2, l3);
 		databaseCRUD.add_list(g2, l4);
 		databaseCRUD.add_list(g3, l5);
 		databaseCRUD.add_list(g3, l6);
         Log.v(LOGTAG, "Lists added");
         
 		//SimpleExpandableListAdapter egl = new SimpleExpandableListAdapter()
         //Prototype for the expanded list
         SimpleExpandableListAdapter expandGroupList = new SimpleExpandableListAdapter(
         		this, //associated context
           		createGroupList(),				//hash map for groups
           		R.layout.group_row,				//Group XML 
           		new String[] {"Group Item"},		//Get group string id
           		new int[] {R.id.row_name}, 	//get groups integer id's
           		createChildList(),				//hash map for lists(children) 
           		R.layout.child_row,				//List(child) XML 
           		new String[] {"Sub Item"},	//String id's of lists 
           		new int[] {R.id.grp_child}	//integer id's of lists 
         );
         
         setListAdapter(expandGroupList);
 		//databaseCRUD.query_group();
         
         //Instead of grouplist() and childlist() we could use a database function that returns arraylists similar 
         //to what is already implemented querygroups() querylists() 
 }
     
    /* public void onCreate(Bundle savedInstanceState) {
         try{
              super.onCreate(savedInstanceState);
              setContentView(R.layout.drawer_example_activity_main);
  
         SimpleExpandableListAdapter expListAdapter =
             new SimpleExpandableListAdapter(
                     this,
                     createGroupList(),              // Creating group List.
                     R.layout.group_row,             // Group item layout XML.
                     new String[] { "Group Item" },  // the key of group item.
                     new int[] { R.id.row_name },    // ID of each group item.-Data under the key goes into this TextView.
                     createChildList(),              // childData describes second-level entries.
                     R.layout.child_row,             // Layout for sub-level entries(second level).
                     new String[] {"Sub Item"},      // Keys in childData maps to display.
                     new int[] { R.id.grp_child }     // Data under the keys above go into these TextViews.
                 );
             setListAdapter( expListAdapter );       // setting the adapter in the list.
  
         }catch(Exception e){
             System.out.println("Errrr +++ " + e.getMessage());
         }
     }
  */
     private ArrayList<HashMap<String, String>> createGroupList()
     {    	
 		  ArrayList<HashMap<String, String>> groups = new ArrayList<HashMap<String, String>>();
 		  
 		  Log.v(LOGTAG, "Group fetch");
 		  List<Group> gList = databaseCRUD.query_group();
 		  Log.v(LOGTAG, "\t There are " + gList.size() + " groups fetched");
 		  
 		  Log.v(LOGTAG, "Adding Group hashmap");
 		  for(Group t : gList)
 		  {
 			  Log.v(LOGTAG, "Group name: " + t.getName() + "\n\t Contains ID: " + t.getID());
 			  HashMap<String, String> m = new HashMap<String, String>();
 			  m.put(t.getName(), Integer.toString(t.getID()));
 			  groups.add(m);		  
 		  }
 		  Log.v(LOGTAG, "Groups added");
 		  
 		  return groups;    	
     	/*
 		List<MyList> tempList = databaseCRUD.query_list(g1);
 		
 		Log.v(LOGTAG, "\t There are "+ tempList.size() + " items in temp");
 		
 		for(MyList t : tempList) {
 			Log.v(LOGTAG, t.getName() + " is a part of group " + t.getGroupID());
 			
 			Log.v(LOGTAG, "\t\t querying for list items");
 			List<Item> tempItem = databaseCRUD.query_item(t);
 			
 			Log.v(LOGTAG, "\t\t There are " + tempItem.size() + " items in the list");
 			for(Item tI : tempItem) {
 				Log.v(LOGTAG, "\t\t\t" + "item name="+ tI.getName());
 			}
 		}*/
     }
     
     
     
     private ArrayList<ArrayList<HashMap<String, String>>> createChildList()
     {
     	ArrayList<ArrayList<HashMap<String, String>>> lists = new ArrayList<ArrayList<HashMap<String, String>>>();
     	
     	Log.v(LOGTAG, "re-Querying groups to fetch lists");
     	List<Group> gList = databaseCRUD.query_group();
     	
     	Log.v(LOGTAG, "Querying lists based on groups");
     	for(Group g: gList)
     	{
     		ArrayList<HashMap<String, String>> secList = new ArrayList<HashMap<String, String>>();
     		List<MyList> lList = databaseCRUD.query_list(g);
     		
     		for(MyList l: lList)
     		{
         			Log.v(LOGTAG, "Current List being added to Hashmap" + l.getName());
         		    HashMap<String, String> child = new HashMap<String, String>();
         		    child.put(l.getName(), Integer.toString(l.getID()));
         		    secList.add(child);
     		}
     		lists.add(secList);	
     	}
     	Log.v(LOGTAG, "Lists added underneath the Groups");
 
     	/* Prototype
 		  ArrayList<ArrayList<HashMap<String, String>>> lists = new ArrayList<ArrayList<HashMap<String, String>>>();
 		  for(int i=0; i<3; ++i)
 		  {
 			  ArrayList<HashMap<String, String>> secList = new ArrayList<HashMap<String, String>>();
 			  for(int j=0; j<2; j++)
 			  {
 				  HashMap<String, String> child = new HashMap<String, String>();
 				  child.put("List" + j, "List: " + j);
 				  secList.add(child);	  
 			  }
 			  lists.add(secList);
 		  } end prototype */	
 		  		  
 		return lists;
     	
     	
     }
     //Creating rows (groups)
     @SuppressWarnings("unchecked")
     //    private List<HashMap<String, String>> createGroupList()
     
     /*
     private void createGroupList() 
     {  		  
   		  //databaseCRUD.query_group()
   		  
   		  ArrayList<HashMap<String, String>> groups = new ArrayList<HashMap<String, String>>();
   		  for(int i=0; i<3; ++i)
   		  {
   	  		  HashMap<String, String> m = new HashMap<String, String>();
   	  		  m.put("Group", "Group: " + i);
   	  		  groups.add(m);  			  
   		  }
   		  
   		  ArrayList<ArrayList<HashMap<String, String>>> lists = new ArrayList<ArrayList<HashMap<String, String>>>();
   		  for(int i=0; i<3; ++i)
   		  {
   			  ArrayList<HashMap<String, String>> secList = new ArrayList<HashMap<String, String>>();
   			  for(int j=0; j<2; j++)
   			  {
   				  HashMap<String, String> child = new HashMap<String, String>();
   				  child.put("List" + j, "List: " + j);
   				  secList.add(child);	  
   			  }
   			  lists.add(secList);
   		  }
 
   		  
   		  
     	
     	/*ArrayList<HashMap<String, String>> result = new ArrayList<HashMap<String, String>>();
           for( int i = 0 ; i < 15 ; ++i ) 
           { // 15 groups........
         	  HashMap<String, String> m = new HashMap<String, String>();
         	  m.put( "Group Item","Group Item " + i ); // the key and it's value.
         	  result.add( m );
           }
          
     }
     
     */
     
     /*
     //creating children (lists)
     @SuppressWarnings("unchecked")
     private void createChildList() 
     {
 
 		
         ArrayList<ArrayList<HashMap<String, String>>> result = new ArrayList<ArrayList<HashMap<String, String>>>();
         for( int i = 0 ; i < 15 ; ++i ) { // this -15 is the number of groups(Here it's fifteen)
           // each group need each HashMap-Here for each group we have 3 subgroups 
           ArrayList<HashMap<String, String>> secList = new ArrayList<HashMap<String, String>>();
           for( int n = 0 ; n < 3 ; n++ ) {
             HashMap<String, String> child = new HashMap<String, String>();
             child.put( "Sub Item", "Sub Item " + n );
             secList.add( child );
           }
          result.add( secList );
         }
     }
     
     
     public void  onContentChanged  () {
         System.out.println("onContentChanged");
         super.onContentChanged();
     }
     */
     
     // This function is called on each child click 
     public boolean onChildClick( ExpandableListView parent, View v, int groupPosition,int childPosition,long id) 
     {
         System.out.println("Inside onChildClick at groupPosition = " + groupPosition +" Child clicked at position " + childPosition);
         return true;
     }
     /*
  
     // This function is called on expansion of the group
     public void  onGroupExpand  (int groupPosition) {
         try{
              System.out.println("Group expanding Listener => groupPosition = " + groupPosition);
         }catch(Exception e){
             System.out.println(" groupPosition Errrr +++ " + e.getMessage());
         }
     }
     */
 }
