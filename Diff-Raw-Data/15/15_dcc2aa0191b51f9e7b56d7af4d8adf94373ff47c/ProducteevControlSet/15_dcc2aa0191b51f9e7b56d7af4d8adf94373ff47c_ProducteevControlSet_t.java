 package com.todoroo.astrid.producteev;
 
 import java.util.ArrayList;
 
 import android.app.Activity;
 import android.view.LayoutInflater;
import android.view.View;
 import android.view.ViewGroup;
 import android.widget.ArrayAdapter;
 import android.widget.Spinner;
 import android.widget.TextView;
 
 import com.timsu.astrid.R;
 import com.todoroo.andlib.service.Autowired;
 import com.todoroo.andlib.service.DependencyInjectionService;
 import com.todoroo.andlib.utility.DateUtilities;
 import com.todoroo.astrid.activity.TaskEditActivity.TaskEditControlSet;
 import com.todoroo.astrid.model.Metadata;
 import com.todoroo.astrid.model.StoreObject;
 import com.todoroo.astrid.model.Task;
 import com.todoroo.astrid.producteev.sync.ProducteevDashboard;
 import com.todoroo.astrid.producteev.sync.ProducteevDataService;
 import com.todoroo.astrid.producteev.sync.ProducteevTask;
 import com.todoroo.astrid.producteev.sync.ProducteevUser;
 import com.todoroo.astrid.service.MetadataService;
 
 /**
  * Control Set for managing task/dashboard assignments in Producteev
  *
  * @author Arne Jans <arne.jans@gmail.com>
  *
  */
 public class ProducteevControlSet implements TaskEditControlSet {
 
     // --- instance variables
 
     private final Activity activity;
 
    private final View view;
     private Task myTask;
     private final Spinner responsibleSelector;
     private final Spinner dashboardSelector;
 
     private ArrayList<ProducteevUser> users = null;
     private ArrayList<ProducteevDashboard> dashboards = null;
 
     @Autowired
     MetadataService metadataService;
 
     public ProducteevControlSet(final Activity activity, ViewGroup parent) {
         DependencyInjectionService.getInstance().inject(this);
 
         this.activity = activity;
        view = LayoutInflater.from(activity).inflate(R.layout.producteev_control, parent, true);
 
         this.responsibleSelector = (Spinner) activity.findViewById(R.id.producteev_TEA_task_assign);
         this.dashboardSelector = (Spinner) activity.findViewById(R.id.producteev_TEA_dashboard_assign);
     }
 
     @Override
     public void readFromTask(Task task) {
         this.myTask = task;
         Metadata metadata = ProducteevDataService.getInstance().getTaskMetadata(myTask.getId());
         if (metadata != null) {
             // Fill the dashboard-spinner and set the current dashboard
             long dashboardId = metadata.getValue(ProducteevTask.DASHBOARD_ID);
 
             StoreObject[] dashboardsData = ProducteevDataService.getInstance().getDashboards();
             dashboards = new ArrayList<ProducteevDashboard>(dashboardsData.length);
             ProducteevDashboard ownerDashboard = null;
             int dashboardSpinnerIndex = 0;
             //dashboard to not sync as first spinner-entry
             dashboards.add(new ProducteevDashboard(ProducteevUtilities.DASHBOARD_NO_SYNC, activity.getString(R.string.producteev_no_dashboard),null));
             for (int i=1;i<dashboardsData.length+1;i++) {
                 ProducteevDashboard dashboard = new ProducteevDashboard(dashboardsData[i-1]);
                 dashboards.add(dashboard);
                 if(dashboard.getId() == dashboardId) {
                     ownerDashboard = dashboard;
                     dashboardSpinnerIndex = i;
                 }
             }
 
             ArrayAdapter<ProducteevDashboard> dashAdapter = new ArrayAdapter<ProducteevDashboard>(activity,
                     android.R.layout.simple_spinner_item, dashboards);
             dashAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
             dashboardSelector.setAdapter(dashAdapter);
             dashboardSelector.setSelection(dashboardSpinnerIndex);
 
             if (ownerDashboard == null || ownerDashboard.getId() == ProducteevUtilities.DASHBOARD_NO_SYNC) {
                 responsibleSelector.setEnabled(false);
                 TextView emptyView = new TextView(activity);
                 emptyView.setText(activity.getText(R.string.producteev_no_dashboard));
                 responsibleSelector.setEmptyView(emptyView);
                view.findViewById(R.id.producteev_TEA_task_assign_label).setVisibility(View.GONE);
                 return;
             }
 
             // Fill the responsible-spinner and set the current responsible
             users = ownerDashboard.getUsers();
             long responsibleId = metadata.getValue(ProducteevTask.RESPONSIBLE_ID);
             int userSpinnerIndex = 0;
 
             for (ProducteevUser user : users) {
                 if (user.getId() == responsibleId) {
                     break;
                 }
                 userSpinnerIndex++;
             }
             ArrayAdapter<ProducteevUser> usersAdapter = new ArrayAdapter<ProducteevUser>(activity,
                     android.R.layout.simple_spinner_item, users);
             usersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
             responsibleSelector.setAdapter(usersAdapter);
             responsibleSelector.setSelection(userSpinnerIndex);
         }
     }
 
     @Override
     public void writeToModel(Task task) {
         Metadata metadata = ProducteevDataService.getInstance().getTaskMetadata(task.getId());
         if (metadata == null) {
             metadata = new Metadata();
             metadata.setValue(Metadata.KEY, ProducteevTask.METADATA_KEY);
             metadata.setValue(Metadata.TASK, task.getId());
             metadata.setValue(ProducteevTask.ID, 0L);
         }
 
         ProducteevDashboard dashboard = (ProducteevDashboard) dashboardSelector.getSelectedItem();
         metadata.setValue(ProducteevTask.DASHBOARD_ID, dashboard.getId());
 
         ProducteevUser responsibleUser = (ProducteevUser) responsibleSelector.getSelectedItem();

        if(responsibleUser == null)
            metadata.setValue(ProducteevTask.RESPONSIBLE_ID, 0L);
        else
            metadata.setValue(ProducteevTask.RESPONSIBLE_ID, responsibleUser.getId());
 
         if(metadata.getSetValues().size() > 0 ) {
             metadataService.save(metadata);
             task.setValue(Task.MODIFICATION_DATE, DateUtilities.now());
         }
     }
 }
