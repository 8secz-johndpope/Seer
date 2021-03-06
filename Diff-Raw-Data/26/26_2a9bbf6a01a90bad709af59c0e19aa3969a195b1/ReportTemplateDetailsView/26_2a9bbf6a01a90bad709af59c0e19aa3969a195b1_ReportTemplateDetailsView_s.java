 /*******************************************************************************
  * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
  * 
  * This program and the accompanying materials
  * are made available under the terms of the GNU Public License v3.0.
  * 
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  ******************************************************************************/
 package org.obiba.opal.web.gwt.app.client.report.view;
 
 import static org.obiba.opal.web.gwt.app.client.report.presenter.ReportTemplateDetailsPresenter.DELETE_ACTION;
 import static org.obiba.opal.web.gwt.app.client.report.presenter.ReportTemplateDetailsPresenter.DOWNLOAD_ACTION;
 
 import java.util.Date;
 
 import org.obiba.opal.web.gwt.app.client.js.JsArrays;
 import org.obiba.opal.web.gwt.app.client.report.presenter.ReportTemplateDetailsPresenter;
 import org.obiba.opal.web.gwt.app.client.report.presenter.ReportTemplateDetailsPresenter.ActionHandler;
 import org.obiba.opal.web.gwt.app.client.report.presenter.ReportTemplateDetailsPresenter.HasActionHandler;
import org.obiba.opal.web.gwt.user.cellview.client.DateTimeColumn;
 import org.obiba.opal.web.model.client.opal.FileDto;
 
 import com.google.gwt.core.client.GWT;
 import com.google.gwt.core.client.JsArray;
 import com.google.gwt.event.dom.client.ClickEvent;
 import com.google.gwt.event.dom.client.ClickHandler;
 import com.google.gwt.uibinder.client.UiBinder;
 import com.google.gwt.uibinder.client.UiField;
 import com.google.gwt.uibinder.client.UiTemplate;
 import com.google.gwt.user.cellview.client.CellTable;
 import com.google.gwt.user.client.ui.Anchor;
 import com.google.gwt.user.client.ui.Composite;
 import com.google.gwt.user.client.ui.FlowPanel;
 import com.google.gwt.user.client.ui.Grid;
 import com.google.gwt.user.client.ui.Widget;
 
 public class ReportTemplateDetailsView extends Composite implements ReportTemplateDetailsPresenter.Display {
 
   @UiTemplate("ReportTemplateDetailsView.ui.xml")
   interface ReportTemplateDetailsViewUiBinder extends UiBinder<Widget, ReportTemplateDetailsView> {
   }
 
   private static ReportTemplateDetailsViewUiBinder uiBinder = GWT.create(ReportTemplateDetailsViewUiBinder.class);
 
   @UiField
   CellTable<FileDto> producedReportsTable;
 
   @UiField
   FlowPanel reportTemplateDetails;
 
   Grid producedReports;
 
   private HasActionHandler actionsColumn;
 
   private ActionHandler actionHandler;
 
   public ReportTemplateDetailsView() {
     initWidget(uiBinder.createAndBindUi(this));
    initTable();
   }
 
   @Override
   public Widget asWidget() {
     return this;
   }
 
   @Override
   public void startProcessing() {
   }
 
   @Override
   public void stopProcessing() {
   }
 
   @Override
   public void setProducedReports(FileDto reportFolder) {
     JsArray<FileDto> reports = reportFolder.getChildrenArray();
     int reportCount = reports.length();
    producedReportsTable.setPageSize(reportCount);
    producedReportsTable.setDataSize(reportCount, true);
    producedReportsTable.setData(0, reportCount, JsArrays.toList(reports, 0, reportCount));

     reportTemplateDetails.clear();
     producedReports = new Grid(reportCount + 1, 2);
     producedReports.setTitle("Reports");
     producedReports.setHTML(0, 0, "<b>Produced Date</b>");
     producedReports.setHTML(0, 1, "<b>Action</b>");
 
     int i = 1;
     for(FileDto report : JsArrays.toIterable(reports)) {
       producedReports.setText(i, 0, new Date((long) report.getLastModifiedTime()).toString());
       producedReports.setWidget(i, 1, new ReportActionPanel(report));
       i++;
     }
     reportTemplateDetails.add(producedReports);
   }
 
  private void initTable() {
    producedReportsTable.setTitle("Reports");
    producedReportsTable.addColumn(new DateTimeColumn<FileDto>() {

      @Override
      public Date getValue(FileDto reportFile) {
        // return new Date((long) reportFile.getLastModifiedTime());
        return new Date();
      }

    });
  }

   private class ReportActionPanel extends FlowPanel {
 
     FileDto report;
 
     public ReportActionPanel(final FileDto report) {
       super();
       this.report = report;
 
       Anchor downloadLink = new Anchor("Download");
       Anchor deleteLink = new Anchor("Delete");
       downloadLink.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event) {
           actionHandler.doAction(report, DOWNLOAD_ACTION);
         }
       });
       deleteLink.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event) {
           actionHandler.doAction(report, DELETE_ACTION);
         }
       });
       add(downloadLink);
       add(deleteLink);
     }
   }
 
   @Override
   public void setActionHandler(ActionHandler handler) {
     this.actionHandler = handler;
   }
 
 }
