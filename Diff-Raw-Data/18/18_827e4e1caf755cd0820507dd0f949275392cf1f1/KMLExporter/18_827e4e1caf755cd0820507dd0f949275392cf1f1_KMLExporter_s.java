 /*
  * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
  * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
  * All rights reserved.
  * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
  *
  * This file is part of Neptus, Command and Control Framework.
  *
  * Commercial Licence Usage
  * Licencees holding valid commercial Neptus licences may use this file
  * in accordance with the commercial licence agreement provided with the
  * Software or, alternatively, in accordance with the terms contained in a
  * written agreement between you and Universidade do Porto. For licensing
  * terms, conditions, and further information contact lsts@fe.up.pt.
  *
  * European Union Public Licence - EUPL v.1.1 Usage
  * Alternatively, this file may be used under the terms of the EUPL,
  * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
  * included in the packaging of this file. You may not use this work
  * except in compliance with the Licence. Unless required by applicable
  * law or agreed to in writing, software distributed under the Licence is
  * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
  * ANY KIND, either express or implied. See the Licence for the specific
  * language governing permissions and limitations at
  * https://www.lsts.pt/neptus/licence.
  *
  * For more information please see <http://lsts.fe.up.pt/neptus>.
  *
  * Author: José Correia
  * Jan 8, 2013
  */
 package pt.lsts.neptus.mra.exporters;
 
 import java.awt.Color;
 import java.awt.Font;
 import java.awt.Graphics2D;
 import java.awt.RenderingHints;
 import java.awt.geom.Line2D;
 import java.awt.image.BufferedImage;
 import java.io.BufferedWriter;
 import java.io.File;
 import java.io.FileWriter;
 import java.util.ArrayList;
 import java.util.Date;
 import java.util.Vector;
 
 import javax.imageio.ImageIO;
 import javax.swing.JFrame;
 import javax.swing.JLabel;
 
 import pt.lsts.imc.EstimatedState;
 import pt.lsts.imc.IMCMessage;
 import pt.lsts.imc.lsf.LsfGenericIterator;
 import pt.lsts.neptus.NeptusLog;
 import pt.lsts.neptus.colormap.ColorBar;
 import pt.lsts.neptus.colormap.ColorMap;
 import pt.lsts.neptus.colormap.ColorMapFactory;
 import pt.lsts.neptus.colormap.ColormapOverlay;
 import pt.lsts.neptus.comm.IMCUtils;
 import pt.lsts.neptus.i18n.I18n;
 import pt.lsts.neptus.mra.NeptusMRA;
 import pt.lsts.neptus.mra.WorldImage;
 import pt.lsts.neptus.mra.api.BathymetryParser;
 import pt.lsts.neptus.mra.api.BathymetryParserFactory;
 import pt.lsts.neptus.mra.api.BathymetryPoint;
 import pt.lsts.neptus.mra.api.BathymetrySwath;
 import pt.lsts.neptus.mra.api.SidescanLine;
 import pt.lsts.neptus.mra.api.SidescanParameters;
 import pt.lsts.neptus.mra.api.SidescanParser;
 import pt.lsts.neptus.mra.api.SidescanParserFactory;
 import pt.lsts.neptus.mra.importers.IMraLogGroup;
 import pt.lsts.neptus.mra.importers.deltat.DeltaTParser;
 import pt.lsts.neptus.mra.importers.jsf.JsfSidescanParser;
 import pt.lsts.neptus.plugins.NeptusProperty;
 import pt.lsts.neptus.plugins.PluginDescription;
 import pt.lsts.neptus.plugins.PluginUtils;
 import pt.lsts.neptus.renderer2d.ImageLayer;
 import pt.lsts.neptus.types.coord.LocationType;
 import pt.lsts.neptus.types.mission.MissionType;
 import pt.lsts.neptus.types.mission.plan.PlanType;
 import pt.lsts.neptus.util.GuiUtils;
 import pt.lsts.neptus.util.bathymetry.TidePredictionFactory;
 import pt.lsts.neptus.util.bathymetry.TidePredictionFinder;
 import pt.lsts.neptus.util.llf.LogUtils;
 import pt.lsts.util.WGS84Utilities;
 
 /**
  * @author zp
  */
 @PluginDescription
 public class KMLExporter implements MRAExporter {
     public double minLat = 180;
     public double maxLat = -180;
     public double minLon = 360;
     public double maxLon = -360;
 
     public double minHeight = 1000;
     public double maxHeight = -1;
 
     LocationType topLeftLT;
     LocationType bottomRightLT;
 
     File f, output;
     IMraLogGroup source;
 
     @NeptusProperty
     public double timeVariableGain = 300;
 
     @NeptusProperty
     public double normalization = 0.1;
 
     @NeptusProperty
     public double swathLength = 1.0;
 
     @NeptusProperty
     public double swathTransparency = 0.25;
 
     @NeptusProperty
     public double layerTransparency = 0.5;
     
     @NeptusProperty
     public boolean separate_transducers = false;
     
     @NeptusProperty
     public boolean filterOutNadir = true;
     
     public KMLExporter(IMraLogGroup source) {
         this.source = source;        
 
     }
 
     @Override
     public String getName() {
         return I18n.text("Export to KML");
     }
 
     @Override
     public boolean canBeApplied(IMraLogGroup source) {
         return true;
     }
 
     public String kmlHeader(String title) {
         String ret = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<kml xmlns=\"http://earth.google.com/kml/2.1\">\n";
         ret += "\t<Document>\n";
         ret += "\t\t<name>" + title + "</name>\n";
 
         Date d = new Date((long) (1000 * source.getLsfIndex().getStartTime()));
         ret += "\t\t<description>Plan executed on " + d + "</description>";
 
         ret += "\t\t<Style id=\"estate\">\n";
         ret += "\t\t\t<LineStyle>\n";
         ret += "\t\t\t<color>99ff0000</color>\n";
         ret += "\t\t\t<width>4</width>\n";
         ret += "\t\t\t</LineStyle>\n";
         ret += "\t\t</Style>\n";
 
         ret += "\t\t<Style id=\"plan\">\n";
         ret += "\t\t\t<LineStyle>\n";
         ret += "\t\t\t<color>990000ff</color>\n";
         ret += "\t\t\t<width>4</width>\n";
         ret += "\t\t\t</LineStyle>\n";
         ret += "\t\t</Style>\n";
 
         return ret;
     }
 
     public String overlay(File imageFile, String title, LocationType sw, LocationType ne) {
         sw.convertToAbsoluteLatLonDepth();
         ne.convertToAbsoluteLatLonDepth();
         String ret = "\t\t<GroundOverlay>\n";
         try {
             ret += "\t\t\t<name>" + title + "</name>\n";
             ret += "\t\t\t<description></description>\n";
             ret += "\t\t\t<Icon>\n";
 
             ret += "\t\t\t\t<href>" + imageFile.getName() + "</href>\n";
             ret += "\t\t\t</Icon>\n";
             ret += "\t\t\t<LatLonBox>\n";
             ret += "\t\t\t\t<north>" + ne.getLatitudeAsDoubleValue() + "</north>\n";
             ret += "\t\t\t\t<south>" + sw.getLatitudeAsDoubleValue() + "</south>\n";
             ret += "\t\t\t\t<east>" + ne.getLongitudeAsDoubleValue() + "</east>\n";
             ret += "\t\t\t\t<west>" + sw.getLongitudeAsDoubleValue() + "</west>\n";
             ret += "\t\t\t\t<rotation>0</rotation>\n";
             ret += "\t\t\t</LatLonBox>\n";
             ret += "\t\t</GroundOverlay>\n";
         }
         catch (Exception e) {
             e.printStackTrace();
             return "";
 
         }
         return ret;
     }
 
     public String path(Vector<LocationType> coords, String name, String style) {
         String ret = "\t\t<Placemark>\n";
         ret += "\t\t\t<name>" + name + "</name>\n";
         ret += "\t\t\t<styleUrl>#" + style + "</styleUrl>\n";
         ret += "\t\t\t<LineString>\n";
         ret += "\t\t\t\t<altitudeMode>relative</altitudeMode>\n";
         ret += "\t\t\t\t<coordinates> ";
 
         for (LocationType l : coords) {
             l.convertToAbsoluteLatLonDepth();
             ret += l.getLongitudeAsDoubleValue() + "," + l.getLatitudeAsDoubleValue() + ",0\n";// -" + l.getDepth()+"\n";
         }
         ret += "\t\t\t\t</coordinates>\n";
         ret += "\t\t\t</LineString>\n";
         ret += "\t\t</Placemark>\n";
         return ret;
     }
 
     public String kmlFooter() {
         return "\t</Document>\n</kml>\n";
     }
 
     public String dvlOverlay(File dir, int resolution) {
         ColormapOverlay overlay = new ColormapOverlay("dvlBathymetry", 1, false, 0);
         TidePredictionFinder finder = TidePredictionFactory.create(source);
 
         for (EstimatedState state : source.getLsfIndex().getIterator(EstimatedState.class, 100)) {
             if (state.getAlt() < 0 || state.getDepth() < NeptusMRA.minDepthForBathymetry || Math.abs(state.getTheta()) > Math.toDegrees(10))
                 continue;
 
             LocationType loc = new LocationType(Math.toDegrees(state.getLat()), Math.toDegrees(state.getLon()));
             loc.translatePosition(state.getX(), state.getY(), 0);
 
             if (finder == null)
                 overlay.addSample(loc, state.getAlt() + state.getDepth());
             else {
                 try {
                     overlay.addSample(loc, state.getAlt() + state.getDepth() - finder.getTidePrediction(state.getDate(), false));
                 }
                 catch (Exception e) {
                     e.printStackTrace();
                 }
             }
         }
 
         ImageLayer il = overlay.getImageLayer();
         try {
             ImageIO.write(il.getImage(), "PNG", new File(dir, "dvl.png"));
 
             il.setTransparency(layerTransparency);
             il.saveToFile(new File(dir.getParentFile(), "sidescan.layer"));
             LocationType sw = new LocationType();
             LocationType ne = new LocationType();
             sw.setLatitude(il.getBottomRight().getLatitude());
             sw.setLongitude(il.getTopLeft().getLongitude());
 
             ne.setLatitude(il.getTopLeft().getLatitude());
             ne.setLongitude(il.getBottomRight().getLongitude());
 
             return overlay(new File(dir, "dvn.png"), "DVL Bathymetry mosaic",
                     sw, ne);
         }
         catch (Exception e) {
             e.printStackTrace();
             return "";
         }
 
     }
     
     enum Ducer {
         starboard,
         board,
         both
     }
 
     public String sidescanOverlay(File dir, double resolution, LocationType topLeft, LocationType bottomRight, Ducer ducer) {
         SidescanParser ssParser = SidescanParserFactory.build(source);
 
         //FIXME temporary fix
         boolean makeAbs = (ssParser instanceof JsfSidescanParser);
 
         // System.out.println("makeAbs: "+makeAbs);
 
         if (ssParser == null || ssParser.getSubsystemList().isEmpty())
             return "";
 
         double[] offsets = topLeft.getOffsetFrom(bottomRight);
         int width = (int) Math.abs(offsets[1]* resolution) ;
         int height = (int) Math.abs(offsets[0] * resolution);
 
         if (width <= 0 || height <= 0)
             return "";
 
         final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
 
         JLabel lbl = new JLabel() {
             private static final long serialVersionUID = 1L;
 
             @Override
             public void paint(java.awt.Graphics g) {
                 g.drawImage(img, 0, 0, getWidth(), getHeight(), 0, 0, img.getWidth(), img.getHeight(), null);                
             };
         };
 
         JFrame frm = GuiUtils.testFrame(lbl, "Creating sidescan mosaic...");
         frm.setSize(800, 600);
         GuiUtils.centerOnScreen(frm);
         Graphics2D g = (Graphics2D) img.getGraphics();
         long start = ssParser.firstPingTimestamp();
         long end = ssParser.lastPingTimestamp();
         int sys = ssParser.getSubsystemList().get(0);
         SidescanParameters params = new SidescanParameters(normalization, timeVariableGain);
         String filename = "sidescan";
         
         BufferedImage swath = null;
         ColorMap cmap = ColorMapFactory.createBronzeColormap();
         for (long time = start; time < end - 1000; time += 1000) {
             ArrayList<SidescanLine> lines;
             try {
                 lines = ssParser.getLinesBetween(time, time + 1000, sys, params);
             }
             catch (Exception e) {
                 e.printStackTrace();
                 continue;
             }
 
             BufferedImage previous = new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB);
             for (SidescanLine sl : lines) {
 
                 int widthPixels = (int)(sl.range * resolution * 2);
 
                 // Calculate nadir pixel range to be zero-alpha (transparent)
                 int nadirStartPixel = (int) ((widthPixels / 2) - (sl.state.getAltitude() * resolution * 1.25));
                 int nadirFinalPixel = (int) ((widthPixels / 2) + (sl.state.getAltitude() * resolution * 1.25));
 
                 if (swath == null || swath.getWidth() != widthPixels)
                     swath = new BufferedImage(widthPixels, 3, BufferedImage.TYPE_INT_ARGB);
 
                 if (previous != null)
                     swath.getGraphics().drawImage(previous, 0, 0, swath.getWidth(), 1, 1, 0, 2, previous.getWidth(), null);
 
                 int samplesPerPixel = sl.data.length / widthPixels;
                 if (samplesPerPixel == 0)
                     continue;
                 double sum = 0;
                 int count = 0;
 
                 int startPixel, endPixel;
                 
                 switch (ducer) {
                     case board:
                         startPixel = 0;
                         endPixel = sl.data.length/2;
                         filename = "sidescan_board";
                         break;
                     case starboard:
                         startPixel = sl.data.length/2;
                         endPixel = sl.data.length;
                         filename = "sidescan_starboard";
                         break;
                     default:
                         startPixel = 0;
                         endPixel = sl.data.length;
                         filename = "sidescan";
                         break;
                 }
                 
                 for (int i = startPixel; i < endPixel; i++) {
                     if (i != 0 && i % samplesPerPixel == 0) {
                         int alpha = (int)(swathTransparency * 255);
                         double val = sum / count;
                         
                         if(filterOutNadir && (double)i / samplesPerPixel >= nadirStartPixel && (double)i / samplesPerPixel <= nadirFinalPixel) {
                             alpha = (int)((1.0-val)*(1.0-val) *255);
                         }
 
                         if ((i/samplesPerPixel-1)<widthPixels)
                             swath.setRGB(i/samplesPerPixel-1, 0, cmap.getColor(val).getRGB() ^ ((alpha&0xFF)<<24));
                         sum = count = 0;
                     }
                     else {
                         count ++;
                         sum += sl.data[i];
                     }                 
                 }                
                 Graphics2D g2 = (Graphics2D)g.create();
                 g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                 g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                 double[] pos = sl.state.getPosition().getOffsetFrom(topLeft);
                 g2.translate(pos[1] * resolution, -pos[0] * resolution);
                 // System.out.print(Math.toDegrees(sl.state.getYaw())+" --> ");
                 if (makeAbs && sl.state.getYaw() < 0)
                     g2.rotate(Math.toRadians(300)+sl.state.getYaw());
                 else
                     g2.rotate(sl.state.getYaw());
                 // System.out.println(Math.toDegrees(Math.abs(sl.state.getYaw())));
                 //System.out.println(Math.toDegrees(sl.state.getYaw()));
                 g2.setColor(Color.black);
                 g2.scale(1, swathLength*resolution);
 
                 g2.drawImage(swath, -swath.getWidth()/2, 0, null);
                 g2.dispose();
                 previous = swath;
                 lbl.repaint();
             }
         }
 
         frm.setVisible(false);
         frm.dispose();
         
         try {
             ImageIO.write(img, "PNG", new File(dir, filename+".png"));
             ImageLayer il = new ImageLayer("Sidescan mosaic from "+source.name(), img, topLeft, bottomRight);
             il.setTransparency(layerTransparency);
             il.saveToFile(new File(dir.getParentFile(), filename+".layer"));
             return overlay(new File(dir, filename+".png"), "Sidescan mosaic", 
                     new LocationType(bottomRight.getLatitudeAsDoubleValue(), topLeft.getLongitudeAsDoubleValue()),
                     new LocationType(topLeft.getLatitudeAsDoubleValue(), bottomRight.getLongitudeAsDoubleValue()));
         }
         catch (Exception e) {
             e.printStackTrace();
             return "";
         }
     }
 
     public String multibeamLegend(File dir) {
         BufferedImage img = new BufferedImage(100, 170, BufferedImage.TYPE_INT_ARGB);
         Graphics2D g = (Graphics2D) img.getGraphics();
         g.setColor(new Color(255,255,255,100));
         g.fillRect(5, 30, 70, 110);
 
         ColorMap cmap = ColorMapFactory.createJetColorMap();
         ColorBar cb = new ColorBar(ColorBar.VERTICAL_ORIENTATION, cmap);
         cb.setSize(15, 80);
         g.setColor(Color.black);
         Font prev = g.getFont();
         g.setFont(new Font("Helvetica", Font.BOLD, 18));
         g.setFont(prev);
         g.translate(15, 45);
         cb.paint(g);
         g.translate(-10, -15);
 
         try {
             g.drawString(GuiUtils.getNeptusDecimalFormat(1).format(0), 28, 20);
             g.drawString(GuiUtils.getNeptusDecimalFormat(1).format(NeptusMRA.maxBathymDepth/2), 28, 60);
             g.drawString(GuiUtils.getNeptusDecimalFormat(1).format(NeptusMRA.maxBathymDepth), 28, 100);
         }
         catch (Exception e) {
             NeptusLog.pub().error(e);
             e.printStackTrace();
         }
 
         try {
             ImageIO.write(img, "PNG", new File(dir, "mb_legend.png"));
             String ret= "\t\t<ScreenOverlay>\n";
             ret += "\t\t\t<name>Multibeam layer legend</name>\n";
             ret += "\t\t\t<Icon>\n";
             ret += "\t\t\t\t<href>" + new File(dir, "mb_legend.png").toURI().toURL() + "</href>\n";
             ret += "\t\t\t</Icon>\n";
             ret += "\t\t\t<overlayXY x=\"0\" y=\"1\" xunits=\"fraction\" yunits=\"fraction\"/>\n";
             ret += "\t\t\t<screenXY x=\"0\" y=\"1\" xunits=\"fraction\" yunits=\"fraction\"/>\n";
             ret += "\t\t\t<rotationXY x=\"0\" y=\"0\" xunits=\"fraction\" yunits=\"fraction\"/>\n";
             ret += "\t\t\t<size x=\"0\" y=\"0\" xunits=\"fraction\" yunits=\"fraction\"/>\n";
             ret += "\t\t</ScreenOverlay>\n";
             return ret;
         }
         catch (Exception e) {
             e.printStackTrace();
             return "";
         }
     }
 
     public String multibeamOverlay(File dir) {
         BathymetryParser parser = BathymetryParserFactory.build(source);
         if (parser == null) {
             NeptusLog.pub().info(I18n.text("no multibeam data has been found."));
             return "";
         }
 
         String legend = multibeamLegend(dir);
 
         parser.rewind();
 
         LocationType topLeft = new LocationType(parser.getBathymetryInfo().topLeft);
         LocationType bottomRight = new LocationType(parser.getBathymetryInfo().bottomRight);
 
        topLeft.translatePosition(50, -50, 0);
        bottomRight.translatePosition(-50, 50, 0);
         topLeft.convertToAbsoluteLatLonDepth();
         bottomRight.convertToAbsoluteLatLonDepth();
 
 
         double[] offsets = topLeft.getOffsetFrom(bottomRight);
         double mult = 1.25;
         int width = (int) Math.abs(offsets[1]* mult) ;
         int height = (int) Math.abs(offsets[0] * mult);
 
         BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
         Graphics2D g = (Graphics2D)img.getGraphics();
         BathymetrySwath swath;
         long first = (long) (1000 * source.getLsfIndex().getStartTime());
         long time = (long) (1000 * source.getLsfIndex().getEndTime()) - first;
         long lastPercent = -1;
 
         ColorMap cmap = ColorMapFactory.createJetColorMap();
 
         while ((swath = parser.nextSwath(1)) != null) {
 
             LocationType loc = swath.getPose().getPosition();
 
             for (BathymetryPoint bp : swath.getData()) {
 
                 //if (Math.random() < 0.2)
                 //    continue;
                 LocationType loc2 = new LocationType(loc);
                 if (bp == null)
                     continue;
                 loc2.translatePosition(bp.north, bp.east, 0);
 
                 double[] pos = loc2.getOffsetFrom(topLeft);
                 Color c = cmap.getColor(1-(bp.depth/NeptusMRA.maxBathymDepth));
 
                 g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 64));
                 g.draw(new Line2D.Double(pos[1] * mult, -pos[0] * mult, pos[1] * mult, -pos[0] * mult));
             }
             long percent = ((swath.getTimestamp() - first) * 100) / time;
             if (percent != lastPercent)
                 NeptusLog.pub().info("MULTIBEAM: " + percent + "% done...");
             lastPercent = percent;
         }
 
         try {
             ImageIO.write(img, "PNG", new File(dir, "mb_bath2.png"));
             ImageLayer il = new ImageLayer("Bathymetry from "+source.name(), img, topLeft, bottomRight);
             il.saveToFile(new File(dir.getParentFile(), "multibeam.layer"));
 
 
             return legend+overlay(new File(dir, "mb_bath2.png"), "Multibeam Bathymetry", 
                     new LocationType(bottomRight.getLatitudeAsDoubleValue(), topLeft.getLongitudeAsDoubleValue()),
                     new LocationType(topLeft.getLatitudeAsDoubleValue(), bottomRight.getLongitudeAsDoubleValue()));
         }
         catch (Exception e) {
             e.printStackTrace();
             return "";
         }
 
     }
 
     @Deprecated
     public String multibeamOverlay_old(File dir) {
         if (source.getFile("multibeam.83P") == null) {
             NeptusLog.pub().info("no multibeam data hasn been found.");
             return "";
         }
 
         WorldImage imgMb = new WorldImage(3, ColorMapFactory.createJetColorMap());
         DeltaTParser parser = new DeltaTParser(source);
         parser.rewind();
         BathymetrySwath swath;
         long first = (long) (1000 * source.getLsfIndex().getStartTime());
         long time = (long) (1000 * source.getLsfIndex().getEndTime()) - first;
         long lastPercent = -1;
 
         while ((swath = parser.nextSwath(0.1)) != null) {
             //System.out.println("processing swath...");
             LocationType loc = swath.getPose().getPosition();
             for (BathymetryPoint bp : swath.getData()) {
 
                 if (Math.random() < 0.05)
                     continue;
                 LocationType loc2 = new LocationType(loc);
                 if (bp == null)
                     continue;
                 loc2.translatePosition(-bp.north, -bp.east, 0);
 
                 imgMb.addPoint(loc2, 1-(bp.depth/parser.getBathymetryInfo().maxDepth));
                 long percent = ((swath.getTimestamp() - first) * 100) / time;
                 if (percent != lastPercent)
                     NeptusLog.pub().info("MULTIBEAM: " + percent + "% done...");
                 lastPercent = percent;
             }
         }
 
         try {
             ImageIO.write(imgMb.processData(), "PNG", new File(dir, "mb_bath.png"));
             return overlay(new File(dir, "mb_bath.png"), "Multibeam Bathymetry", imgMb.getSouthWest(),
                     imgMb.getNorthEast());
         }
         catch (Exception e) {
             e.printStackTrace();
             return "";
         }
 
     }
 
     @Override
     public String process() {
 
         PluginUtils.editPluginProperties(this, true);
 
         try {
             File out = new File(source.getFile("mra"), "kml");
             out.mkdirs();
 
             out = new File(out, "data.kml");
             BufferedWriter bw = new BufferedWriter(new FileWriter(out));
             File f = source.getFile(".");
             String name = f.getCanonicalFile().getName();
             bw.write(kmlHeader(name));
 
             Vector<LocationType> states = new Vector<>();
 
             LocationType bottomRight = null, topLeft = null;
 
             // Path
             LsfGenericIterator it = source.getLsfIndex().getIterator("EstimatedState", 0, 3000);
             for (IMCMessage s : it) {
                 LocationType loc = IMCUtils.parseLocation(s);
                 loc.convertToAbsoluteLatLonDepth();
                 if (bottomRight == null) {
                     bottomRight = new LocationType(loc); 
                     topLeft = new LocationType(loc);
                 }
 
 
                 if (loc.getLatitudeAsDoubleValue() < bottomRight.getLatitudeAsDoubleValue())
                     bottomRight.setLatitude(loc.getLatitudeAsDoubleValue());
                 else if (loc.getLatitudeAsDoubleValue() > topLeft.getLatitudeAsDoubleValue())
                     topLeft.setLatitude(loc.getLatitudeAsDoubleValue());
                 if (loc.getLongitudeAsDoubleValue() < topLeft.getLongitudeAsDoubleValue())
                     topLeft.setLongitude(loc.getLongitudeAsDoubleValue());
                 else if (loc.getLongitudeAsDoubleValue() > bottomRight.getLongitudeAsDoubleValue())
                     bottomRight.setLongitude(loc.getLongitudeAsDoubleValue());
 
                 states.add(loc);
             }
 
             if (topLeft == null) {
                 bw.close();
                 throw new Exception("This log doesn't have required data (EstimatedState)");
             }
             bw.write(path(states, "Estimated State", "estate"));
 
             // plan
             PlanType plan = null;
             MissionType mt = LogUtils.generateMission(source);
             if (mt != null)
                 plan = LogUtils.generatePlan(mt, source);
             if (plan != null)
                 bw.write(path(plan.planPath(), "Planned waypoints", "plan"));
 
             topLeft.translatePosition(50, -50, 0);
             bottomRight.translatePosition(-50, 50, 0);
             topLeft.convertToAbsoluteLatLonDepth();
             bottomRight.convertToAbsoluteLatLonDepth();
 
             bw.write(sidescanOverlay(out.getParentFile(), 6, topLeft, bottomRight, Ducer.both));
             if (separate_transducers) {
                 bw.write(sidescanOverlay(out.getParentFile(), 6, topLeft, bottomRight, Ducer.board));
                 bw.write(sidescanOverlay(out.getParentFile(), 6, topLeft, bottomRight, Ducer.starboard));                
             }
             
 
             String mb = multibeamOverlay(out.getParentFile()); 
             if (!mb.isEmpty())
                 bw.write(mb);
             else {
                 WorldImage imgDvl = new WorldImage(1, ColorMapFactory.createJetColorMap());
                 imgDvl.setMaxVal(20d);
                 imgDvl.setMinVal(3d);
                 it = source.getLsfIndex().getIterator("EstimatedState", 0, 100);
                 for (IMCMessage s : it) {
                     LocationType loc = IMCUtils.parseState(s).getPosition();
                     double alt = s.getDouble("alt");
                     double depth = s.getDouble("depth");
                     if (alt == -1 || depth < NeptusMRA.minDepthForBathymetry)
                         continue;
                     else
                         imgDvl.addPoint(loc, s.getDouble("alt"));
                 }
                 ImageIO.write(imgDvl.processData(), "PNG", new File(out.getParent(), "dvl_bath.png"));
                 bw.write(overlay(new File(out.getParent(), "dvl_bath.png"), "DVL Bathymetry", imgDvl.getSouthWest(),
                         imgDvl.getNorthEast()));                
             }
 
             bw.write(kmlFooter());
 
             bw.close();
 
             return "Log exported to " + out.getAbsolutePath();
         }
         catch (Exception e) {
             GuiUtils.errorMessage("Error while exporting to KML", "Exception of type " + e.getClass().getSimpleName()
                     + " occurred: " + e.getMessage());
             e.printStackTrace();
             return null;
         }
     }
 
     public static void main(String[] args) {
         LocationType loc1 = new LocationType(41.08, -8.2343);
         LocationType loc2 = new LocationType(41.12, -8.2324);
         System.out.println(loc1.getDistanceInMeters(loc2));
         System.out.println(loc2.getDistanceInMeters(loc1));
         double[] res1 = loc2.getOffsetFrom(loc1);
 
         double[] res2 = WGS84Utilities.WGS84displacement(loc1.getLatitudeAsDoubleValue(), loc1.getLongitudeAsDoubleValue(), 0, loc2.getLatitudeAsDoubleValue(), loc2.getLongitudeAsDoubleValue(), 0);
         System.out.println(Math.sqrt(res2[0] * res2[0] + res2[1] * res2[1]));
         System.out.println(Math.sqrt(res1[0] * res1[0] + res1[1] * res1[1]));
     }
 
 }
