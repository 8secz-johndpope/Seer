 package uni.projecte.dataLayer.RemoteDBManager.objects;
 
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.Set;
 
 import uni.projecte.R;
 import uni.projecte.Activities.Citations.CitationEditor;
 import uni.projecte.Activities.Thesaurus.ThesaurusTaxonChecker;
 import uni.projecte.controler.ProjectControler;
 import uni.projecte.dataTypes.LocalTaxon;
 import uni.projecte.dataTypes.RemoteTaxonSet;
 import android.app.Activity;
 import android.content.Context;
 import android.content.Intent;
 import android.graphics.Color;
 import android.os.Bundle;
 import android.text.Html;
 import android.util.Log;
 import android.view.LayoutInflater;
 import android.view.View;
 import android.view.ViewGroup;
 import android.view.View.OnClickListener;
 import android.widget.BaseAdapter;
 import android.widget.Filter;
 import android.widget.Filterable;
 import android.widget.ImageButton;
 import android.widget.ImageView;
 import android.widget.SectionIndexer;
 import android.widget.TextView;
 
 
 public class LocalTaxonNoThListAdapter extends BaseAdapter implements SectionIndexer,Filterable{
   	 
 		private LayoutInflater mInflater;
 		private ArrayList<LocalTaxon> elements;
 	    HashMap<String, Integer> alphaIndexer;
 	    String[] sections;
 	    RemoteTaxonSet remoteTaxonList;
 	    private long projId;
 	    private OnClickListener addThListener;
 	    
 		 public LocalTaxonNoThListAdapter(Context context,ArrayList<LocalTaxon> localTaxon, long projId, OnClickListener addThListener) {
 		 
 			 mInflater = LayoutInflater.from(context);
 		 
 			 this.elements=localTaxon;
 	         alphaIndexer = new HashMap<String, Integer>(); 
 	         this.projId=projId;
 	         this.addThListener=addThListener;
 			 
 			  int size = elements.size();
 	          for (int i = size - 1; i >= 0; i--) {
 	                  String element = elements.get(i).getTaxon();
	                  if(element!=null && !element.equals("")) alphaIndexer.put(element.substring(0, 1), i);
 	          //We store the first letter of the word, and its index.
 	          //The Hashmap will replace the value for identical keys are putted in
 	          } 
 	          
 	          Set<String> keys = alphaIndexer.keySet(); // set of letters ...sets
 	          // cannot be sorted...
 	
 	          Iterator<String> it = keys.iterator();
 	          ArrayList<String> keyList = new ArrayList<String>(); // list can be
 	          // sorted
 	
 	          while (it.hasNext()) {
 	                  String key = it.next();
 	                  keyList.add(key);
 	          }
 	
 	          Collections.sort(keyList);
 	
 	          sections = new String[keyList.size()]; // simple conversion to an
 	          // array of object
 	          keyList.toArray(sections);
 	
 		 }
 	
 	
 	
 		public int getCount() {
 			 return elements.size();
 		 }
 	
 		 public Object getItem(int position) {
 		 return position;
 		 }
 	
 		 public long getItemId(int position) {
 		 return position;
 		 }
 	
 		 public View getView(int position, View convertView, ViewGroup parent) {
 		 ViewHolder holder;
 		 if (convertView == null) {
 	    	 
 			 convertView = mInflater.inflate(R.layout.taxonnothrow, null);
 			 
 	    	 holder = new ViewHolder();
 	    	 holder.text = (TextView) convertView.findViewById(R.id.tvTaxonList);
 	    	 holder.image= (ImageButton) convertView.findViewById(R.id.ibLocalThNoThEdit);
 	    	 holder.ibAddTh= (ImageButton) convertView.findViewById(R.id.ibAddNewThElem);
 
 	    	 convertView.setTag(holder);
 	    	 
 	    	 
 		 } 
 		 else {
 			 
 			 holder = (ViewHolder) convertView.getTag();
 			 
 		 }
 	
 		 
 	    	 holder.text.setText(Html.fromHtml(elements.get(position).getTaxon()));
 	    	 holder.text.setOnClickListener(addThListener);
 
 	   	    		 
 	    	// holder.image.setVisibility(View.GONE);
 	    //holder.image.setVisibility(View.VISIBLE);
 	    	 holder.text.setTextColor(Color.WHITE);
     		 
 	    	 holder.ibAddTh.setOnClickListener(addThListener);
 	    	 holder.ibAddTh.setId(position);
 	    	 
 	    	 holder.image.setId((int) elements.get(position).getCitationId());
 	    	 holder.image.setOnClickListener(new OnClickListener() {  
 	          	
 	          	public void onClick(View v) { 
 	          	
 	          		ProjectControler rc= new ProjectControler(v.getContext());
 	      			rc.loadProjectInfoById(projId);
 	    	 
 	    	 		Intent intent = new Intent(v.getContext(), CitationEditor.class);
 	   
 		 			Bundle b = new Bundle();
 		 			b.putString("rsName", rc.getName());
 		 			intent.putExtras(b);
 		 			b = new Bundle();
 		 			b.putLong("id", rc.getProjectId());
 		 			intent.putExtras(b);
 		 			b = new Bundle();
 		 			b.putString("rsDescription", rc.getThName());
 		 			intent.putExtras(b);
 		 			b = new Bundle();
 		 			b.putLong("idSample", v.getId());
 		 			intent.putExtras(b);
 		 		
 		 			
 		 		   ((Activity) v.getContext()).startActivityForResult(intent, ThesaurusTaxonChecker.EDIT_CITATION);   
 	          		
 	          		
 	          	}
 	          	
 	     	 }
 	          
 	          );
 	     	 
 	
 	    	 return convertView;
 		 
 		 }
 	
 		 static class ViewHolder {
 			 
 	    	 TextView text;
 	    	 ImageButton image;
 	    	 ImageButton ibAddTh;	    	 
 	
 		 }
 		 
 		  public int getPositionForSection(int section) {
 	          // Log.v("getPositionForSection", ""+section);
 	          String letter = sections[section];
 	
 	          return alphaIndexer.get(letter);
 	  }
 	
 	  public int getSectionForPosition(int position) {
 	
 	          // you will notice it will be never called (right?)
 	          Log.v("getSectionForPosition", "called");
 	          return 0;
 	  }
 	
 	  public Object[] getSections() {
 	
 	          return sections; // to string will be called each object, to display
 	          // the letter
 	  }
 	
 	
 	
 	public Filter getFilter() {
 		
 		return null;
 		
 	}
 	 
 }    
