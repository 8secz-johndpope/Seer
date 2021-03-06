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
 package org.gitools.ui.commands;
 
 import org.apache.commons.lang.ArrayUtils;
 import org.gitools.core.clustering.method.annotations.AnnPatClusteringMethod;
 import org.gitools.core.heatmap.header.ColoredLabel;
 import org.gitools.core.heatmap.header.HeatmapColoredLabelsHeader;
 import org.gitools.ui.platform.AppFrame;
 import org.gitools.utils.progressmonitor.IProgressMonitor;
 import org.jetbrains.annotations.NotNull;
 
 import java.awt.*;
 import java.util.List;
 
 public class CommandAddHeaderColoredLabels extends CommandAddHeader {
 
     private List<String> colors;
     private List<String> ids;
     private String pattern;
     private boolean autoGenerateColors;
 
     public CommandAddHeaderColoredLabels(String heatmap, String side, String pattern, List<String> colors, List<String> ids, boolean autoGenerateColors) {
         super(heatmap, side);
         this.colors = colors;
         this.ids = ids;
         this.pattern = pattern;
         this.autoGenerateColors = autoGenerateColors;
     }
 
     @Override
     public void execute(@NotNull IProgressMonitor monitor) throws CommandException {
 
 
         super.execute(monitor);
         if (getExitStatus() > 0) {
             return;
         }
 
 
         ColoredLabel[] cls = new ColoredLabel[0];
         HeatmapColoredLabelsHeader header = new HeatmapColoredLabelsHeader(hdim);
 
         header.setAnnotationPattern(pattern);
         header.setHeatmapDimension(hdim);
         header.setTitle(header.deriveTitleFromPattern());
 
         if (autoGenerateColors) {
             cls =  header.getClusters();
             header.autoGenerateColoredLabels(new AnnPatClusteringMethod());
         }
 
 
 
         if (!(colors == null || ids == null)) {
 
            if (colors.size() != ids.size()) {
                setExitStatus(1);
                throw new CommandException("The number of specified colors and values must match");
            }

             if (cls.length < 1) {
                 cls = new ColoredLabel[0];
             }
 
             for (int i = 0; i < colors.size(); i++) {
                 ColoredLabel cl = new ColoredLabel(ids.get(i), Color.decode(colors.get(i)));
                 cls = addCluster(cls, cl);
             }

            if (cls.length == 0)  {
                throw new CommandException("No color labels have been created.");
            }

             header.setClusters(cls);
 
         }
 
         hdim.addHeader(header);
 
         AppFrame.get().refresh();
 
         // Force a GC to release free memory
         System.gc();
 
         monitor.end();
 
 
         setExitStatus(0);
         return;
     }
 
     private ColoredLabel[] addCluster(ColoredLabel[] coloredLabels, ColoredLabel cl) {
         String key = cl.getValue();
         for (int i = 0; i < coloredLabels.length; i++) {
             if (coloredLabels[i].getValue().equals(key)) {
                 coloredLabels[i] = cl;
                 break;
             }
         }
         return (ColoredLabel[]) ArrayUtils.add(coloredLabels, cl);
     }
 }
