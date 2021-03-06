 /*
  * Copyright 2011, 2012 Delving BV
  *
  * Licensed under the EUPL, Version 1.0 or as soon they
  * will be approved by the European Commission - subsequent
  * versions of the EUPL (the "Licence");
  * you may not use this work except in compliance with the
  * Licence.
  * You may obtain a copy of the Licence at:
  *
  * http://ec.europa.eu/idabc/eupl
  *
  * Unless required by applicable law or agreed to in
  * writing, software distributed under the Licence is
  * distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
  * express or implied.
  * See the Licence for the specific language governing
  * permissions and limitations under the Licence.
  */
 
 package eu.delving.sip.frames;
 
 import eu.delving.sip.base.FrameBase;
 import eu.delving.sip.base.Work;
 import eu.delving.sip.model.SipModel;
 import eu.delving.sip.model.WorkModel;
 
 import javax.swing.DefaultListCellRenderer;
 import javax.swing.JList;
 import javax.swing.ListSelectionModel;
 import javax.swing.event.ListSelectionEvent;
 import javax.swing.event.ListSelectionListener;
 import java.awt.BorderLayout;
 import java.awt.Component;
 import java.awt.Container;
 import java.awt.Font;
 import java.text.DateFormat;
 import java.text.SimpleDateFormat;
 
 import static eu.delving.sip.base.SwingHelper.*;
 
 /**
  * Show the live work model in detail, and pop up a cancellation dialog when one is selected.  The full list
  * is shown here and the mini list is made available for placement on a small section of the desktop.
  *
  * @author Gerald de Jong <gerald@delving.eu>
  */
 
 public class WorkFrame extends FrameBase {
     private static final DateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("hh:mm:ss");
     private static final Font MONOSPACED = new Font("Monospaced", Font.BOLD, 12);
     private JList fullList, miniList;
 
     public WorkFrame(final SipModel sipModel) {
         super(Which.WORK, sipModel, "Work");
         this.miniList = new JList(sipModel.getWorkModel().getListModel());
         this.miniList.setFont(MONOSPACED);
         this.miniList.setCellRenderer(new MiniCellRenderer());
         this.fullList = new JList(sipModel.getWorkModel().getListModel());
         this.fullList.setFont(MONOSPACED);
         this.fullList.setCellRenderer(new JobContextCellRenderer());
         this.fullList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
         this.fullList.addListSelectionListener(new ListSelectionListener() {
             @Override
             public void valueChanged(ListSelectionEvent e) {
                 if (e.getValueIsAdjusting()) return;
                 WorkModel.JobContext context = (WorkModel.JobContext) fullList.getSelectedValue();
                 if (context != null && context.getProgressIndicator() != null) {
                     String message = String.format(
                             "<html>Do you want to cancel this job?<br>%s",
                             toFullString(context)
                     );
                     if (sipModel.getFeedback().confirm("Cancel Job", message)) {
                         context.getProgressIndicator().cancel();
                     }
                 }
             }
         });
     }
 
     public JList getMiniList() {
         return miniList;
     }
 
     @Override
     protected void buildContent(Container content) {
         content.add(scrollV("Work happening in the background", fullList), BorderLayout.CENTER);
     }
 
     private class JobContextCellRenderer extends DefaultListCellRenderer {
         @Override
         public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
             WorkModel.JobContext jobContext = (WorkModel.JobContext) value;
             String show = toFullString(jobContext);
             return super.getListCellRendererComponent(list, show, index, isSelected, cellHasFocus);
         }
     }
 
     private String toFullString(WorkModel.JobContext jobContext) {
        if (jobContext.isEmpty()) return "empty";
         String date = TIMESTAMP_FORMAT.format(jobContext.getStart());
         String job = jobContext.getJob().toString();
         String dataSetSpec = jobContext.getDataSet();
         String show = String.format("%s: %s", job, date);
         if (dataSetSpec != null) {
             show += String.format(" (%s)", dataSetSpec);
         }
         WorkModel.ProgressIndicator progress = jobContext.getProgressIndicator();
         if (progress != null) {
             show += String.format(" - %s", progress.getString(true));
         }
         return show;
     }
 
     private class MiniCellRenderer extends DefaultListCellRenderer {
         @Override
         public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
             WorkModel.JobContext jobContext = (WorkModel.JobContext) value;
             WorkModel.ProgressIndicator progress = jobContext.getProgressIndicator();
            String jobName = jobContext.isDone() ? "done" : jobContext.toString();
             String show = progress == null ? jobName : String.format("%s: %s", jobName, progress.getString(false));
             Component component = super.getListCellRendererComponent(list, show, index, isSelected, cellHasFocus);
             if (jobContext.getWork() instanceof Work.LongTermWork) {
                 component.setBackground(LONG_TERM_JOB_COLOR);
             }
             else {
                 component.setBackground(NORMAL_JOB_COLOR);
             }
             return component;
         }
     }
 }
