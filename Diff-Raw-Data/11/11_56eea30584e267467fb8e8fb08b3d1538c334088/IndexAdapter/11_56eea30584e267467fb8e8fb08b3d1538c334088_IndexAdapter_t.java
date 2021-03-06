 package hk.reality.stock.view;
 
 import hk.reality.stock.R;
 import hk.reality.stock.model.Index;
 import android.content.Context;
 import android.graphics.Color;
 import android.text.format.DateFormat;
 import android.util.Log;
 import android.view.LayoutInflater;
 import android.view.View;
 import android.view.ViewGroup;
 import android.widget.ArrayAdapter;
 import android.widget.TextView;
 
 public class IndexAdapter extends ArrayAdapter<Index> {
     private static final String TAG = "IndexAdapter";
     private java.text.DateFormat formatter;
 
     public IndexAdapter(Context context) {
         super(context, 0);
         formatter = DateFormat.getTimeFormat(context);
     }
 
 
     @Override
     public View getView(int position, View view, ViewGroup parent) {
         View v = view;
         if (v == null) {
             LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = (View) vi.inflate(R.layout.index_item, null);
         }
 
         Log.d(TAG, "prepare view for indexes");
         // prepare views
         TextView name = (TextView) v.findViewById(R.id.name);       
         TextView price = (TextView) v.findViewById(R.id.price);
         TextView change = (TextView) v.findViewById(R.id.change);
         TextView volume = (TextView) v.findViewById(R.id.volume);
         TextView time = (TextView) v.findViewById(R.id.time);
         
         // set data
         Index index = getItem(position);
         if (index != null && index.getUpdatedAt() != null) {
             time.setText(formatter.format(index.getUpdatedAt().getTime()));
             volume.setText("");
             name.setText(index.getName());
             price.setText(String.format("%.02f", index.getValue().doubleValue()));
             change.setText(String.format("%+.02f (%.02f%%)", 
                     index.getChange().doubleValue(), 
                     index.getChangePercent().doubleValue()));
             
             if (index.getChange().floatValue() > 0) {
                 price.setTextColor(Color.rgb(0, 213, 65));
                 change.setTextColor(Color.rgb(0, 213, 65));
             } else if (index.getChange().floatValue() < 0) {
                 price.setTextColor(Color.rgb(238, 30, 0));
                 change.setTextColor(Color.rgb(238, 30, 0));
             } else {
                 price.setTextColor(Color.WHITE);
                 change.setTextColor(Color.WHITE);
             }
 
         } else {
             time.setText("");
             volume.setText("---");
             name.setText(index.getName());
             price.setText("----");
             change.setText("---- (---)");
         }
         return v;
     }
 }
