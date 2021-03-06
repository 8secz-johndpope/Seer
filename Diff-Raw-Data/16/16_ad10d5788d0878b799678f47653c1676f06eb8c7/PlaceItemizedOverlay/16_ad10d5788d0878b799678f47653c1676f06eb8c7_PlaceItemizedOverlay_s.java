 package se.tidensavtryck;
 
 import java.util.ArrayList;
 
 import android.content.Context;
 import android.content.Intent;
 import android.graphics.drawable.Drawable;
 import android.widget.Toast;
 
 import com.google.android.maps.MapView;
 import com.google.android.maps.OverlayItem;
 import com.readystatesoftware.mapviewballoons.BalloonItemizedOverlay;
 
public class PlaceItemizedOverlay  extends BalloonItemizedOverlay<OverlayItem> {
 
     private ArrayList<OverlayItem> m_overlays = new ArrayList<OverlayItem>();
     private Context c;
 	private final Context context;
     
     public PlaceItemizedOverlay(Context context, MapView mapView) {
         super(boundCenter(context.getResources().getDrawable(R.drawable.marker)), mapView);
 		this.context = context;
         c = mapView.getContext();
     }
 
    public void addOverlay(OverlayItem overlay, Drawable drawable) {
     	boundCenter(drawable);
     	overlay.setMarker(drawable);
         m_overlays.add(overlay);
         populate();
     }
     
     @Override
     protected OverlayItem createItem(int i) {
         return m_overlays.get(i);
     }
 
     @Override
     public int size() {
         return m_overlays.size();
     }
 
     @Override
     protected boolean onBalloonTap(int index) {
         Toast.makeText(c, "onPlaceTap for overlay index " + index,
                 Toast.LENGTH_LONG).show();
         Intent i = new Intent(context, RecordActivity.class);
         context.startActivity(i);
         return true;
     }
     
 }
