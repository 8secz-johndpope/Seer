 /*******************************************************************************
  * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
  * 
  * This program and the accompanying materials
  * are made available under the terms of the GNU Public License v3.0.
  * 
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  ******************************************************************************/
 package org.obiba.opal.web.gwt.app.client.workbench.view;
 
 import java.util.Iterator;
 
 import com.google.gwt.core.client.GWT;
 import com.google.gwt.uibinder.client.UiBinder;
 import com.google.gwt.uibinder.client.UiField;
 import com.google.gwt.uibinder.client.UiTemplate;
 import com.google.gwt.user.client.ui.Composite;
 import com.google.gwt.user.client.ui.DockLayoutPanel;
 import com.google.gwt.user.client.ui.FlowPanel;
 import com.google.gwt.user.client.ui.HasWidgets;
 import com.google.gwt.user.client.ui.ScrollPanel;
 import com.google.gwt.user.client.ui.SimplePanel;
 import com.google.gwt.user.client.ui.SplitLayoutPanel;
 import com.google.gwt.user.client.ui.Widget;
 import com.google.gwt.user.client.ui.WidgetCollection;
 
 /**
  *
  */
 public class WorkbenchLayout extends Composite implements HasWidgets {
 
   @UiTemplate("WorkbenchLayout.ui.xml")
   interface WorkbenchLayoutUiBinder extends UiBinder<Widget, WorkbenchLayout> {
   }
 
   private static WorkbenchLayoutUiBinder uiBinder = GWT.create(WorkbenchLayoutUiBinder.class);
 
   //
   // Instance Variables
   //
 
   @UiField
   DockLayoutPanel workbench;
 
   @UiField
   FlowPanel topHeader;
 
   @UiField
   SimplePanel controlContent;
 
   @UiField
   SplitLayoutPanel content;
 
   @UiField
   ScrollPanel mainContent;
 
   private WidgetCollection children = new WidgetCollection(this);
 
   private Widget titleWidget;
 
   private Widget summaryWidget;
 
   private Widget controlWidget;
 
   private Widget mainWidget;
 
  private Widget informationWidget;

   //
   // Constructors
   //
 
   public WorkbenchLayout() {
     super();
     initWidget(uiBinder.createAndBindUi(this));
     addHandlers();
   }
 
   private void addHandlers() {
   }
 
   public Widget getTitleWidget() {
     return titleWidget;
   }
 
   public Widget getSummaryWidget() {
     return summaryWidget;
   }
 
   public Widget getControlWidget() {
     return controlWidget;
   }
 
   public Widget getMainWidget() {
     return mainWidget;
   }
 
  public Widget getInformationWidget() {
    return informationWidget;
  }

   public void setTitleWidget(Widget w) {
     children.add(w);
     titleWidget = w;
     titleWidget.setStyleName("title", true);
     topHeader.add(titleWidget);
   }
 
   public void setSummaryWidget(Widget w) {
     children.add(w);
     summaryWidget = w;
     summaryWidget.removeFromParent();
     // topHeader.add(summaryWidget);
   }
 
   public void setControlWidget(Widget w) {
     children.add(w);
     controlWidget = w;
     controlContent.setWidget(controlWidget);
   }
 
   public void setMainWidget(Widget w) {
     children.add(w);
     mainWidget = w;
     if(mainWidget instanceof DockLayoutPanel) {
       mainWidget.removeFromParent();
       content.remove(mainContent);
       mainWidget.setStyleName("main", true);
       content.add(mainWidget);
     } else {
       mainContent.setWidget(mainWidget);
     }
   }
 
   //
   // HasWidgets methods
   //
 
   @Override
   public void add(Widget w) {
     if(w == null) return;
 
     if(titleWidget == null) {
       setTitleWidget(w);
     } else if(summaryWidget == null) {
       setSummaryWidget(w);
     } else if(controlWidget == null) {
       setControlWidget(w);
     } else if(mainWidget == null) {
       setMainWidget(w);
     } else {
       throw new IllegalArgumentException("Unexpected widget added.");
     }
   }
 
   @Override
   public void clear() {
     for(int i = 0; i < children.size(); i++) {
       children.get(i).removeFromParent();
       children.remove(i);
     }
   }
 
   @Override
   public Iterator<Widget> iterator() {
     return children.iterator();
   }
 
   @Override
   public boolean remove(Widget w) {
     if(w.getParent() != this) {
       return false;
     }
     if(children.contains(w)) {
       int index = children.indexOf(w);
       children.get(index).removeFromParent();
       children.remove(w);
       return true;
     }
     return false;
   }
 }
