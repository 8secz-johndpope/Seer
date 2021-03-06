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
 package org.gitools.ui.app.heatmap.panel.details;
 
 import org.apache.commons.lang.StringUtils;
 import org.gitools.heatmap.Heatmap;
 import org.gitools.heatmap.HeatmapDimension;
 import org.gitools.heatmap.HeatmapLayer;
 import org.gitools.heatmap.decorator.Decoration;
 import org.gitools.heatmap.decorator.Decorator;
 import org.gitools.heatmap.decorator.DetailsDecoration;
 import org.gitools.heatmap.header.HeatmapHeader;
 import org.gitools.ui.app.actions.edit.EditHeaderAction;
 import org.gitools.ui.app.actions.edit.EditLayerAction;
 import org.gitools.ui.app.heatmap.panel.details.boxes.DetailsBox;
 import org.gitools.ui.app.heatmap.popupmenus.PopupMenuActions;
 import org.jdesktop.swingx.JXTaskPaneContainer;
 import org.jdesktop.swingx.plaf.LookAndFeelAddons;
 import org.jdesktop.swingx.plaf.metal.MetalLookAndFeelAddons;
 
 import javax.swing.*;
 import java.awt.*;
 import java.beans.PropertyChangeEvent;
 import java.beans.PropertyChangeListener;
 import java.util.ArrayList;
 import java.util.List;
 
 /**
  * A details panel with three collapsible panels
  */
 public class DetailsPanel extends JXTaskPaneContainer {
 
     private DetailsBox columnsBox;
     private DetailsBox rowsBox;
     private DetailsBox layersBox;
     private JLabel hintLabel;
 
 
     /**
      * Instantiates a new details panel.
      *
      * @param heatmap the heatmap
      */
     public DetailsPanel(final Heatmap heatmap) {
         super();
 
         try {
             LookAndFeelAddons.setAddon(MetalLookAndFeelAddons.class);
         } catch (InstantiationException | IllegalAccessException e) {
             e.printStackTrace();
         }
 
         setBackground(Color.WHITE);
 
         // Changes to track
         heatmap.getRows().addPropertyChangeListener(new PropertyChangeListener() {
             @Override
             public void propertyChange(PropertyChangeEvent evt) {
                 update(heatmap.getRows(), rowsBox);
                 updateLayers(heatmap, layersBox);
             }
         });
 
         heatmap.getColumns().addPropertyChangeListener(new PropertyChangeListener() {
             @Override
             public void propertyChange(PropertyChangeEvent evt) {
                 update(heatmap.getColumns(), columnsBox);
                 updateLayers(heatmap, layersBox);
             }
         });
 
         PropertyChangeListener updateLayers = new PropertyChangeListener() {
             @Override
             public void propertyChange(PropertyChangeEvent evt) {
                 updateLayers(heatmap, layersBox);
             }
         };
         heatmap.getLayers().addPropertyChangeListener(updateLayers);
         heatmap.getLayers().getTopLayer().addPropertyChangeListener(updateLayers);
 
         add(columnsBox = new DetailsBox("Column", PopupMenuActions.DETAILS_COLUMNS) {
             @Override
             protected void onMouseDblClick(DetailsDecoration detail) {
                 Object reference = detail.getReference();
 
                 if (reference instanceof HeatmapHeader) {
                     new EditHeaderAction((HeatmapHeader) reference).actionPerformed(null);
                 }
             }
         });
         columnsBox.setCollapsed(true);
 
         add(rowsBox = new DetailsBox("Row", PopupMenuActions.DETAILS_ROWS) {
             @Override
             protected void onMouseDblClick(DetailsDecoration detail) {
                 Object reference = detail.getReference();
 
                 if (reference instanceof HeatmapHeader) {
                     new EditHeaderAction((HeatmapHeader) reference).actionPerformed(null);
                 }
             }
         });
         rowsBox.setCollapsed(true);
 
         add(layersBox = new DetailsBox("Values", PopupMenuActions.DETAILS_LAYERS) {
             @Override
             protected void onMouseClick(DetailsDecoration detail) {
                 heatmap.getLayers().setTopLayerIndex(detail.getIndex());
             }
 
             @Override
             protected void onMouseDblClick(DetailsDecoration detail) {
                 Object reference = detail.getReference();
 
                 if (reference instanceof HeatmapLayer) {
                     new EditLayerAction((HeatmapLayer) reference).actionPerformed(null);
                 }
             }
         });
 
         hintLabel = new JLabel();
        hintLabel.setText("<html><body><i>Right click</i> on any layer or header id to " +
                "<b>adjust visualization and other settings</b>.</body></html>");
         add(hintLabel);
 
         update(heatmap.getRows(), rowsBox);
         update(heatmap.getColumns(), columnsBox);
         updateLayers(heatmap, layersBox);
     }
 
     private static void update(HeatmapDimension rows, DetailsBox rowsBox) {
 
         String lead = rows.getFocus();
         String label = StringUtils.capitalize(rows.getId().getLabel());
 
         if (lead != null) {
             rowsBox.setTitle(label + ": " + lead + " [" + (rows.indexOf(lead) + 1) + "]");
         } else {
             rowsBox.setTitle(label);
         }
         List<DetailsDecoration> details = new ArrayList<>();
         rows.populateDetails(details);
         rowsBox.draw(details);
     }
 
     private static void updateLayers(Heatmap heatmap, DetailsBox layersBox) {
         String col = heatmap.getColumns().getFocus();
         String row = heatmap.getRows().getFocus();
 
         if (col != null && row != null) {
 
             Decorator decorator = heatmap.getLayers().getTopLayer().getDecorator();
             Decoration decoration = new Decoration();
             boolean showValue = decorator.isShowValue();
             decorator.setShowValue(true);
             decoration.reset();
             HeatmapLayer layer = heatmap.getLayers().getTopLayer();
             decorator.decorate(decoration, layer.getLongFormatter(), heatmap, layer, row, col);
             decorator.setShowValue(showValue);
 
             layersBox.setTitle("Values: " + decoration.getFormatedValue());
         } else {
             layersBox.setTitle("Values");
         }
 
         List<DetailsDecoration> layersDetails = new ArrayList<>();
         heatmap.getLayers().populateDetails(layersDetails, heatmap, heatmap.getRows().getFocus(), heatmap.getColumns().getFocus());
         layersBox.draw(layersDetails);
     }
 
 
 }
