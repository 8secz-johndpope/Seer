 /*
  * #%L
  * gitools-ui-app
  * %%
  * Copyright (C) 2013 Universitat Pompeu Fabra - Biomedical Genomics group
  * %%
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as
  * published by the Free Software Foundation, either version 3 of the 
  * License, or (at your option) any later version.
  * 
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  * 
  * You should have received a copy of the GNU General Public 
  * License along with this program.  If not, see
  * <http://www.gnu.org/licenses/gpl-3.0.html>.
  * #L%
  */
 package org.gitools.ui.heatmap.editor;
 
 import com.alee.extended.panel.GroupPanel;
 import com.alee.extended.panel.GroupingType;
 import com.alee.extended.panel.WebAccordion;
 import com.alee.extended.panel.WebAccordionStyle;
 import com.alee.laf.panel.WebPanel;
 import com.alee.laf.splitpane.WebSplitPane;
 import org.gitools.core.heatmap.Heatmap;
 import org.gitools.core.heatmap.HeatmapDimension;
 import org.gitools.core.persistence.IResourceLocator;
 import org.gitools.core.persistence.PersistenceException;
 import org.gitools.core.persistence.PersistenceManager;
 import org.gitools.core.persistence.ResourceReference;
 import org.gitools.core.persistence.formats.FileFormat;
 import org.gitools.core.persistence.formats.analysis.HeatmapFormat;
 import org.gitools.core.persistence.locators.UrlResourceLocator;
 import org.gitools.ui.IconNames;
 import org.gitools.ui.heatmap.panel.ColorScalePanel;
 import org.gitools.ui.heatmap.panel.HeatmapMouseListener;
 import org.gitools.ui.heatmap.panel.HeatmapPanel;
 import org.gitools.ui.heatmap.panel.details.DetailsPanel;
 import org.gitools.ui.heatmap.panel.search.HeatmapSearchPanel;
 import org.gitools.ui.heatmap.panel.settings.SettingsPanel;
 import org.gitools.ui.platform.AppFrame;
 import org.gitools.ui.platform.IconUtils;
 import org.gitools.ui.platform.editor.AbstractEditor;
 import org.gitools.ui.platform.progress.JobRunnable;
 import org.gitools.ui.platform.progress.JobThread;
 import org.gitools.ui.platform.wizard.WizardDialog;
 import org.gitools.ui.settings.Settings;
 import org.gitools.ui.wizard.common.SaveFileWizard;
 import org.gitools.utils.progressmonitor.IProgressMonitor;
 import org.jetbrains.annotations.NotNull;
 
 import javax.swing.*;
 import javax.swing.border.CompoundBorder;
 import java.awt.*;
 import java.awt.event.MouseEvent;
 import java.beans.PropertyChangeEvent;
 import java.beans.PropertyChangeListener;
 import java.io.File;
 import java.net.URISyntaxException;
 
 public class HeatmapEditor extends AbstractEditor {
     private static final int DEFAULT_ACCORDION_WIDTH = 320;
     protected final Heatmap heatmap;
     private HeatmapPanel heatmapPanel;
     private ColorScalePanel colorScalePanel;
     private HeatmapSearchPanel searchPanel;
     private DetailsPanel detailsPanel;
     private int lastMouseRow = -1;
     private int lastMouseCol = -1;
 
     public HeatmapEditor(@NotNull Heatmap heatmap) {
 
         IResourceLocator locator = heatmap.getLocator();
         if (locator != null && locator.getURL().getProtocol().equals("file")) {
             try {
                 File file = new File(locator.getURL().toURI());
                 setFile(file);
             } catch (URISyntaxException e) {
             }
         }
 
 
         // Initialize and create heatmap model
         heatmap.init();
         this.heatmap = heatmap;
 
         setIcon(IconUtils.getIconResource(IconNames.heatmap16));
 
         createComponents(this);
 
         setSaveAllowed(true);
         setSaveAsAllowed(true);
         setBackground(Color.WHITE);
 
 
         // Add change listeners
         PropertyChangeListener dirtyListener = new PropertyChangeListener() {
             @Override
             public void propertyChange(PropertyChangeEvent evt) {
                 HeatmapEditor.this.setDirty(true);
             }
         };
         heatmap.addPropertyChangeListener(dirtyListener);
         heatmap.getRows().addPropertyChangeListener(dirtyListener);
         heatmap.getColumns().addPropertyChangeListener(dirtyListener);
         heatmap.getLayers().addPropertyChangeListener(dirtyListener);
         heatmap.getLayers().getTopLayer().getDecorator().addPropertyChangeListener(dirtyListener);
 
     }
 
 
     private void createComponents(@NotNull JComponent container) {
 
         WebAccordion leftPanel = new WebAccordion(WebAccordionStyle.accordionStyle);
         leftPanel.setMultiplySelectionAllowed(false);
         leftPanel.setAnimate(true);
         leftPanel.setUndecorated(true);
 
         detailsPanel = new DetailsPanel(heatmap);
         colorScalePanel = new ColorScalePanel(heatmap);
 
         WebPanel emptyPanel = new WebPanel();
         emptyPanel.setBackground(Color.WHITE);
         GroupPanel details = new GroupPanel(GroupingType.fillMiddle, false, detailsPanel, emptyPanel, colorScalePanel);
         details.setUndecorated(true);
         details.setBackground(Color.WHITE);
         leftPanel.addPane("Details", details);
        leftPanel.addPane("Settings", new JScrollPane(new SettingsPanel(heatmap).getRootPanel(), JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
 
         heatmapPanel = new HeatmapPanel(heatmap);
         heatmapPanel.requestFocusInWindow();
         heatmapPanel.addHeatmapMouseListener(new HeatmapMouseListener() {
             @Override
             public void mouseMoved(int row, int col, MouseEvent e) {
                 HeatmapEditor.this.mouseMoved(row, col, e);
             }
 
             @Override
             public void mouseClicked(int row, int col, MouseEvent e) {
                 HeatmapEditor.this.mouseClicked(row, col, e);
             }
         });
 
         searchPanel = new HeatmapSearchPanel(heatmap);
         searchPanel.setVisible(false);
 
         WebSplitPane splitPane = new WebSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, heatmapPanel);
 
         heatmapPanel.setBorder(new CompoundBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20), BorderFactory.createMatteBorder(0, 1, 1, 0, Color.GRAY)));
         splitPane.setOneTouchExpandable(true);
         splitPane.setDividerLocation(DEFAULT_ACCORDION_WIDTH);
         splitPane.setContinuousLayout(false);
         splitPane.setDividerSize(4);
         splitPane.setBackground(Color.WHITE);
         splitPane.setForeground(Color.WHITE);
 
         container.setLayout(new BorderLayout());
         container.add(searchPanel, BorderLayout.NORTH);
         container.add(splitPane, BorderLayout.CENTER);
 
     }
 
     @Override
     public Heatmap getModel() {
         return heatmap;
     }
 
     @Override
     public void refresh() {
     }
 
     @Override
     public void doVisible() {
         heatmapPanel.requestFocusInWindow();
     }
 
     @Override
     public void doSaveAs(IProgressMonitor monitor) {
 
         File file = getFile();
         if (file != null) {
             Settings.getDefault().setLastPath(file.getParent());
         }
 
         String name = getName();
         int heatmapExt = name.indexOf(".heatmap");
         if (heatmapExt != -1) {
             name = name.substring(0, heatmapExt);
         }
         name = name.replaceAll("\\.", "_");
 
         SaveFileWizard wiz = SaveFileWizard.createSimple(
                 "Save heatmap",
                 name,
                 Settings.getDefault().getLastPath(),
                 new FileFormat[]{
                         new FileFormat("Multiple files heatmap (*.heatmap)", HeatmapFormat.EXTENSION, false, false),
                         new FileFormat("Single file heatmap (*.heatmap.zip)", HeatmapFormat.EXTENSION + ".zip", false, false)
                 }
         );
 
         WizardDialog dlg = new WizardDialog(AppFrame.get(), wiz);
         dlg.setVisible(true);
         if (dlg.isCancelled()) {
             return;
         }
 
         Settings.getDefault().setLastPath(wiz.getFolder());
         file = wiz.getPathAsFile();
         setFile(file);
 
         heatmap.setLocator(new UrlResourceLocator(file));
         heatmap.setData(new ResourceReference<>("data", heatmap.getData().get()));
         HeatmapDimension rows = heatmap.getRows();
         HeatmapDimension columns = heatmap.getColumns();
         rows.setAnnotationsReference(new ResourceReference<>(rows.getId() + "-annotations", rows.getAnnotations()));
         columns.setAnnotationsReference(new ResourceReference<>(columns.getId() + "-annotations", columns.getAnnotations()));
 
         doSave(monitor);
 
     }
 
     @Override
     public void doSave(@NotNull IProgressMonitor monitor) {
 
         File file = getFile();
         if (file == null) {
             doSaveAs(monitor);
             return;
         }
 
         try {
             PersistenceManager.get().store(heatmap.getLocator(), heatmap, monitor);
         } catch (PersistenceException ex) {
             monitor.exception(ex);
         }
 
         setDirty(false);
     }
 
     @Override
     public boolean doClose() {
         if (isDirty()) {
             int res = JOptionPane.showOptionDialog(AppFrame.get(), "File " + getName() + " is modified.\n" +
                     "Save changes ?", "Close", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[]{"Cancel", "Discard", "Save"}, "Save");
 
             if (res == -1 || res == 0) {
                 return false;
             } else if (res == 2) {
                 SaveFileWizard wiz = SaveFileWizard.createSimple("Save heatmap", getName(), Settings.getDefault().getLastPath(), new FileFormat[]{new FileFormat("Heatmap", HeatmapFormat.EXTENSION)});
 
                 WizardDialog dlg = new WizardDialog(AppFrame.get(), wiz);
                 dlg.setVisible(true);
                 if (dlg.isCancelled()) {
                     return false;
                 }
 
                 Settings.getDefault().setLastPath(wiz.getFolder());
 
                 setFile(wiz.getPathAsFile());
 
                 JobThread.execute(AppFrame.get(), new JobRunnable() {
                     @Override
                     public void run(@NotNull IProgressMonitor monitor) {
                         doSave(monitor);
                     }
                 });
             }
         }
 
         // Force GC
         System.gc();
 
         return true;
     }
 
     public void showSearch(boolean searchColumns) {
         searchPanel.searchOnColumns(searchColumns);
         searchPanel.setVisible(true);
     }
 
     void mouseMoved(int row, int col, MouseEvent e) {
         /*
         if (lastMouseRow == row && lastMouseCol == col) {
             return;
         }
 
         lastMouseRow = row;
         lastMouseCol = col;
 
         IMatrixView mv = heatmap;
 
         StringBuilder sb = new StringBuilder();
 
         if (row != -1 && col == -1) { // Row
             String label = mv.getRows().getLabel(row);
             sb.append(label);
             HeatmapDimension rowDim = heatmap.getRows();
             IAnnotations am = rowDim.getAnnotations();
             if (am != null) {
                 if (am.hasIdentifier(label)) {
                     boolean first = true;
                     for (HeatmapHeader header : rowDim.getHeaders()) {
                         if (header instanceof HeatmapTextLabelsHeader) {
                             String annLabel = ((HeatmapTextLabelsHeader) header).getLabelAnnotation();
                             if (annLabel == null || annLabel.isEmpty()) {
                                 continue;
                             }
                             sb.append(first ? ": " : ", ").append(annLabel).append(" = ").append(am.getAnnotation(label, annLabel));
                             first = false;
                         }
                     }
                 }
             }
         } else if (row == -1 && col != -1) { // Column
             String label = mv.getColumns().getLabel(col);
             sb.append(label);
             HeatmapDimension colDim = heatmap.getColumns();
             IAnnotations am = colDim.getAnnotations();
             if (am != null) {
                 if (am.hasIdentifier(label)) {
                     boolean first = true;
                     for (HeatmapHeader header : colDim.getHeaders()) {
                         if (header instanceof HeatmapTextLabelsHeader) {
                             String annLabel = ((HeatmapTextLabelsHeader) header).getLabelAnnotation();
                             if (annLabel == null || annLabel.isEmpty()) {
                                 continue;
                             }
                             sb.append(first ? ": " : ", ").append(annLabel).append(" = ").append(am.getAnnotation(label, annLabel));
                             first = false;
                         }
                     }
                 }
             }
         } else if (row != -1 && col != -1) { // Cell
             String rowLabel = mv.getRows().getLabel(row);
             String colLabel = mv.getColumns().getLabel(col);
             sb.append(colLabel).append(", ").append(rowLabel);
             IMatrixLayers attrs = mv.getLayers();
             if (attrs.size() > 0) {
                 sb.append(": ").append(attrs.get(0).getName()).append(" = ").append(mv.getValue(row, col, 0));
                 for (int i = 1; i < attrs.size(); i++)
                     sb.append(", ").append(attrs.get(i).getName()).append(" = ").append(mv.getValue(row, col, i));
             }
         }
 
         if (sb.length() > 0) {
             AppFrame.get().setStatusText(sb.toString());
         }
         */
     }
 
     void mouseClicked(int row, int col, MouseEvent e) {
     }
 
     @Override
     public void detach() {
         this.heatmap.detach();
     }
 }
