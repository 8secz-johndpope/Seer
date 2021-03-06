 /*******************************************************************************
  * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
  * 
  * This program and the accompanying materials
  * are made available under the terms of the GNU Public License v3.0.
  * 
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  ******************************************************************************/
 package org.obiba.opal.web.gwt.app.client.presenter;
 
 import net.customware.gwt.presenter.client.EventBus;
 import net.customware.gwt.presenter.client.place.Place;
 import net.customware.gwt.presenter.client.place.PlaceRequest;
 import net.customware.gwt.presenter.client.widget.WidgetDisplay;
 import net.customware.gwt.presenter.client.widget.WidgetPresenter;
 
 import org.obiba.opal.web.gwt.app.client.event.WorkbenchChangeEvent;
 
 import com.google.gwt.user.client.Command;
 import com.google.gwt.user.client.ui.MenuItem;
 import com.google.gwt.user.client.ui.Widget;
 import com.google.inject.Inject;
 import com.google.inject.Provider;
 
 /**
  *
  */
 public class ApplicationPresenter extends WidgetPresenter<ApplicationPresenter.Display> {
 
   public interface Display extends WidgetDisplay {
 
     MenuItem getExploreVariables();
 
     void updateWorkbench(Widget workbench);
 
   }
 
   private final Provider<NavigatorPresenter> navigationPresenter;
 
   private WidgetPresenter<?> workbench;
 
   /**
    * @param display
    * @param eventBus
    */
   @Inject
   public ApplicationPresenter(final Display display, final EventBus eventBus, Provider<NavigatorPresenter> navigationPresenter) {
     super(display, eventBus);
     this.navigationPresenter = navigationPresenter;
   }
 
   @Override
   public Place getPlace() {
     return null;
   }
 
   @Override
   protected void onBind() {
 
     getDisplay().getExploreVariables().setCommand(new Command() {
 
       @Override
       public void execute() {
         eventBus.fireEvent(new WorkbenchChangeEvent(navigationPresenter.get()));
       }
     });
 
     super.registerHandler(eventBus.addHandler(WorkbenchChangeEvent.getType(), new WorkbenchChangeEvent.Handler() {
 
       @Override
       public void onWorkbenchChanged(WorkbenchChangeEvent event) {
         if(workbench != null) {
           workbench.unbind();
         }
         workbench = event.getWorkbench();
         WidgetDisplay wd = (WidgetDisplay) workbench.getDisplay();
         getDisplay().updateWorkbench(wd.asWidget());
         workbench.revealDisplay();
       }
     }));
   }
 
   @Override
   protected void onPlaceRequest(PlaceRequest request) {
   }
 
   @Override
   protected void onUnbind() {
   }
 
   @Override
   public void refreshDisplay() {
   }
 
   @Override
   public void revealDisplay() {
   }
 
 }
