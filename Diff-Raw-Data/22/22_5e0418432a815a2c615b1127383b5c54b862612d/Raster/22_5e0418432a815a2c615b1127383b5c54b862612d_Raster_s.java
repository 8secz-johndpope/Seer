 /*
 This program is free software: you can redistribute it and/or modify
     it under the terms of the GNU General Public License version 3 as published by
     the Free Software Foundation.
 
     This program is distributed in the hope that it will be useful,
     but WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
     GNU General Public License for more details.
 
     You should have received a copy of the GNU General Public License
     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
 
 package org.cirqwizard.render;
 
 import javafx.beans.property.DoubleProperty;
 import javafx.beans.property.SimpleDoubleProperty;
 import org.cirqwizard.appertures.CircularAperture;
 import org.cirqwizard.appertures.OctagonalAperture;
 import org.cirqwizard.appertures.OvalAperture;
 import org.cirqwizard.appertures.RectangularAperture;
 import org.cirqwizard.appertures.macro.*;
 import org.cirqwizard.geom.Point;
 import org.cirqwizard.gerber.Flash;
 import org.cirqwizard.gerber.GerberPrimitive;
 import org.cirqwizard.gerber.LinearShape;
 import org.cirqwizard.gerber.Region;
 import org.cirqwizard.math.RealNumber;
 import org.cirqwizard.toolpath.Toolpath;
 
 import javax.imageio.ImageIO;
 import java.awt.*;
 import java.awt.geom.*;
 import java.awt.image.BufferedImage;
 import java.awt.image.DataBuffer;
 import java.awt.image.IndexColorModel;
 import java.awt.image.WritableRaster;
 import java.io.File;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.LinkedList;
 
 
 public class Raster
 {
     private static final int PREVIEW_RESOLUTION_FACTOR = 10;
 
     private static final int MIN_PERIMETER = 40;    // Minimum feature to be considered should measure 0.1mm x 0.1mm or similar
 
     private BufferedImage preview;
     private int width;
     private int height;
     private int resolution;
     private int previewResolution;
     private double inflation;
     private ArrayList<GerberPrimitive> primitives = new ArrayList<GerberPrimitive>();
     private ArrayList<RealNumber> radii = new ArrayList<RealNumber>();
     private PointI lastCleared;
 
     private static final int WINDOWS_CACHE_SIZE = 1;
     private LinkedList<RasterWindow> windows = new LinkedList<RasterWindow>();
 
     private DoubleProperty generationProgress = new SimpleDoubleProperty();
     private DoubleProperty traceProgress = new SimpleDoubleProperty();
 
     private int[] dummyArray = new int[1];
 
     public Raster(double width, double height, int resolution, double inflation)
     {
         int[] cmap = {0x00000000, 0x00ffffff, 0x0000ff00, 0xffffffff};
         IndexColorModel icm = new IndexColorModel(2, 3, cmap, 0, false, 3, DataBuffer.TYPE_BYTE);
         previewResolution = resolution / PREVIEW_RESOLUTION_FACTOR;
         preview = new BufferedImage((int)(width * previewResolution), (int)(height * previewResolution), BufferedImage.TYPE_BYTE_BINARY, icm);
         this.width = (int)(width * resolution);
         this.height = (int)(height * resolution);
         this.inflation = inflation;
         this.resolution = resolution;
     }
 
     public void addPrimitive(GerberPrimitive primitive)
     {
         primitives.add(primitive);
         if (primitive.getAperture() instanceof CircularAperture)
         {
             CircularAperture aperture = (CircularAperture) primitive.getAperture();
             if (!radii.contains(aperture.getDiameter().divide(2).multiply(resolution)))
                 radii.add(aperture.getDiameter().divide(2).multiply(resolution));
         }
         else if (primitive.getAperture() instanceof ApertureMacro)
         {
             for (MacroPrimitive p : ((ApertureMacro) primitive.getAperture()).getPrimitives())
             {
                 if (p instanceof MacroCircle)
                     if (!radii.contains(((MacroCircle) p).getDiameter().divide(2).multiply(resolution)))
                         radii.add(((MacroCircle) p).getDiameter().divide(2).multiply(resolution));
             }
         }
     }
 
     public java.util.List<Toolpath> trace()
     {
         render();
         final ArrayList<Toolpath> segments = new ArrayList<Toolpath>();
         PointI[] p;
         lastCleared = new PointI(0, 0);
         generationProgress.setValue(0);
         while ((p = findNonEmptyPixel()) != null)
         {
             generationProgress.setValue((double) p[0].y / preview.getHeight());
             int perimeter = fill(p[0].x, p[0].y, 0);
             if (perimeter >= MIN_PERIMETER)
                 segments.addAll(new Tracer(Raster.this, p[1].x - 1, p[1].y, getPixel(p[1].x, p[1].y), traceProgress, perimeter * 10).trace(new RealNumber(inflation * 2)));
         }
         return segments;
     }
 
     public DoubleProperty generationProgressProperty()
     {
         return generationProgress;
     }
 
     public DoubleProperty traceProgressProperty()
     {
         return traceProgress;
     }
 
     public int getPixel(int x, int y)
     {
         return getPixel(x, y, RenderingHint.SQUARE);
     }
 
     public int getPixel(int x, int y, RenderingHint hint)
     {
         PointI p = new PointI(x, y);
         RasterWindow window = null;
         for (RasterWindow w : windows)
         {
             if (w.contains(p))
             {
                 window = w;
                 break;
             }
         }
         if (window != null)
         {
             windows.remove(window);
             windows.addFirst(window);
             return window.getPixel(p);
         }
 
         int windowWidth = 1000;
         int windowHeight = 1000;
         switch (hint)
         {
             case COLUMN:
                 windowWidth = 100;
                 windowHeight = 20000;
             break;
             case ROW:
                 windowWidth = 20000;
                 windowHeight = 100;
             break;
         }
         PointI windowCorner = new PointI(x - windowWidth / 2, y - windowHeight / 2);
         if (windowCorner.x + windowWidth > width)
             windowCorner.x = width - windowWidth + 1;
         if (windowCorner.y + windowHeight > height)
             windowCorner.y = height - windowHeight + 1;
         windowCorner.x = Math.max(0, windowCorner.x);
         windowCorner.y = Math.max(0, windowCorner.y);
         RasterWindow w = renderWindow(windowCorner, windowWidth, windowHeight);
         windows.addFirst(w);
         while (windows.size() > WINDOWS_CACHE_SIZE)
             windows.removeLast();
 
         return w.getPixel(p);
     }
 
     public void setPixel(int x, int y, int value)
     {
         dummyArray[0] = value;
         preview.getRaster().setPixel(x, y, dummyArray);
     }
 
     public java.util.List<RealNumber> getRadii()
     {
         return radii;
     }
 
     public int getResolution()
     {
         return resolution;
     }
 
     private RasterWindow renderWindow(PointI lowerLeftCorner, int width, int height)
     {
         int[] cmap = {0x00000000, 0x00ffffff, 0x0000ff00, 0xffffffff};
         IndexColorModel icm = new IndexColorModel(2, 3, cmap, 0, false, 3, DataBuffer.TYPE_BYTE);
         BufferedImage window = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY, icm);
 
         Graphics2D g = window.createGraphics();
         g.setBackground(Color.GREEN);
         g.clearRect(0, 0, width, height);
         g = window.createGraphics();
         g.transform(AffineTransform.getTranslateInstance(-lowerLeftCorner.x, -lowerLeftCorner.y));
         g.transform(AffineTransform.getScaleInstance(resolution, resolution));
         for (GerberPrimitive primitive : primitives)
             renderPrimitive(g, primitive, inflation);
 
         return new RasterWindow(window, lowerLeftCorner);
     }
 
     private void render()
     {
         Graphics2D g = preview.createGraphics();
         g.setBackground(Color.GREEN);
         g.clearRect(0, 0, preview.getWidth(), preview.getHeight());
         g = preview.createGraphics();
         g.transform(AffineTransform.getScaleInstance(previewResolution, previewResolution));
         for (GerberPrimitive primitive : primitives)
             renderPrimitive(g, primitive, inflation);
         fill(0, 0, 0);
     }
 
     private void renderPrimitive(Graphics2D g, GerberPrimitive primitive, double inflation)
     {
         if (!(primitive instanceof Region) && !primitive.getAperture().isVisible())
             return;
 
         g.setColor(Color.WHITE);
         if (primitive instanceof LinearShape)
         {
             LinearShape linearShape = (LinearShape) primitive;
             int cap = linearShape.getAperture() instanceof CircularAperture ? BasicStroke.CAP_ROUND : BasicStroke.CAP_SQUARE;
             g.setStroke(new BasicStroke((float) ((linearShape.getAperture().getWidth(new RealNumber(0)).doubleValue() + inflation * 2)), cap, BasicStroke.JOIN_ROUND));
             g.draw(new Line2D.Double(linearShape.getFrom().getX().doubleValue(), linearShape.getFrom().getY().doubleValue(),
                     linearShape.getTo().getX().doubleValue(), linearShape.getTo().getY().doubleValue()));
         }
         else if (primitive instanceof Region)
         {
             Region region = (Region) primitive;
 
             Path2D polygon = new GeneralPath();
 
             g.setStroke(new BasicStroke((float) inflation * 2, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
             Point p = region.getSegments().get(0).getFrom();
             polygon.moveTo(p.getX().doubleValue(), p.getY().doubleValue());
             for (LinearShape segment : region.getSegments())
                 polygon.lineTo(segment.getTo().getX().doubleValue(), segment.getTo().getY().doubleValue());
 
             g.draw(polygon);
             g.fill(polygon);
         }
         else if (primitive instanceof Flash)
         {
             Flash flash = (Flash) primitive;
             if (flash.getAperture() instanceof CircularAperture)
             {
                 double d = ((CircularAperture)flash.getAperture()).getDiameter().doubleValue() + inflation * 2;
                 double r = d / 2;
                 g.fill(new Ellipse2D.Double(flash.getX().doubleValue() - r, flash.getY().doubleValue() - r, d, d));
             }
             else if (flash.getAperture() instanceof RectangularAperture)
             {
                 RectangularAperture aperture = (RectangularAperture)flash.getAperture();
                 g.fill(new Rectangle2D.Double(flash.getX().doubleValue() - aperture.getDimensions()[0].doubleValue() / 2 - inflation,
                         flash.getY().doubleValue() - aperture.getDimensions()[1].doubleValue() / 2 - inflation,
                         aperture.getDimensions()[0].doubleValue() + inflation * 2,
                         aperture.getDimensions()[1].doubleValue() + inflation * 2));
             }
             else if (flash.getAperture() instanceof OctagonalAperture)
             {
                 double edgeOffset = (((OctagonalAperture)flash.getAperture()).getDiameter().doubleValue() + inflation * 2) * (Math.pow(2, 0.5) - 1) / 2;
                 double centerOffset = (((OctagonalAperture)flash.getAperture()).getDiameter().doubleValue() + inflation * 2) * 0.5;
                 double flashX = flash.getX().doubleValue();
                 double flashY = flash.getY().doubleValue();
 
                 Path2D polygon = new GeneralPath();
                 polygon.moveTo(centerOffset + flashX, edgeOffset + flashY);
                 polygon.lineTo(edgeOffset + flashX, centerOffset + flashY);
                 polygon.lineTo(-edgeOffset + flashX, centerOffset + flashY);
                 polygon.lineTo(-centerOffset + flashX, edgeOffset + flashY);
                 polygon.lineTo(-centerOffset + flashX, -edgeOffset + flashY);
                 polygon.lineTo(-edgeOffset + flashX, -centerOffset + flashY);
                 polygon.lineTo(edgeOffset + flashX, -centerOffset + flashY);
                 polygon.lineTo(centerOffset + flashX, -edgeOffset + flashY);
                 g.fill(polygon);
             }
             else if (flash.getAperture() instanceof OvalAperture)
             {
                 OvalAperture aperture = (OvalAperture)flash.getAperture();
                 double flashX = flash.getX().doubleValue();
                 double flashY = flash.getY().doubleValue();
                 double width = aperture.getWidth().doubleValue() + inflation * 2;
                 double height = aperture.getHeight().doubleValue() + inflation * 2;
                 double d = Math.min(width, height);
                 double l = aperture.isHorizontal() ? width - height : height - width;
                 double xOffset = aperture.isHorizontal() ? l / 2 : 0;
                 double yOffset = aperture.isHorizontal() ? 0 : l / 2;
                 double rectX = aperture.isHorizontal() ? flashX - l / 2 : flashX - width / 2;
                 double rectY = aperture.isHorizontal() ? flashY - height / 2 : flashY - l / 2;
                 double rectWidth =  aperture.isHorizontal() ? l : width;
                 double rectHeight =  aperture.isHorizontal() ? height : l;
 
                 g.fill(new Ellipse2D.Double(flashX - xOffset - d / 2, flashY + yOffset - d / 2, d, d));
                 g.fill(new Ellipse2D.Double(flashX + xOffset - d / 2, flashY - yOffset - d / 2, d, d));
                 g.fill(new Rectangle2D.Double(rectX, rectY, rectWidth, rectHeight));
             }
             else if (flash.getAperture() instanceof ApertureMacro)
             {
                 ApertureMacro macro = (ApertureMacro) flash.getAperture();
                 for (MacroPrimitive p : macro.getPrimitives())
                 {
                     if (p instanceof MacroCenterLine)
                     {
                         MacroCenterLine centerLine = (MacroCenterLine) p;
                         Point from = centerLine.getFrom().add(flash.getPoint());
                         Point to = centerLine.getTo().add(flash.getPoint());
                         g.setStroke(new BasicStroke((float)centerLine.getHeight().doubleValue(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
                         g.draw(new Line2D.Float((float)from.getX().doubleValue(), (float) from.getY().doubleValue(), (float) to.getX().doubleValue(), (float) to.getY().doubleValue()));
                     }
                     else if (p instanceof MacroVectorLine)
                     {
                         MacroVectorLine vectorLine = (MacroVectorLine) p;
                         Point from = vectorLine.getTranslatedStart().add(flash.getPoint());
                         Point to = vectorLine.getTranslatedEnd().add(flash.getPoint());
                         g.setStroke(new BasicStroke((float) vectorLine.getWidth().doubleValue(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
                         g.draw(new Line2D.Float((float) from.getX().doubleValue(), (float) from.getY().doubleValue(), (float) to.getX().doubleValue(), (float) to.getY().doubleValue()));
                     }
                     else if (p instanceof MacroCircle)
                     {
                         MacroCircle circle = (MacroCircle) p;
                         double d = circle.getDiameter().doubleValue();
                         double r = d / 2;
                         Point point = circle.getCenter().add(flash.getPoint());
                         g.fill(new Ellipse2D.Double(point.getX().doubleValue() - r, point.getY().doubleValue() - r, d, d));
                     }
                     else if (p instanceof MacroOutline)
                     {
                         MacroOutline outline = (MacroOutline) p;
                         double x = flash.getX().doubleValue();
                         double y = flash.getY().doubleValue();
 
                         Path2D polygon = new GeneralPath();
                        Point point = outline.getPoints().get(0);
                         polygon.moveTo(point.getX().doubleValue() + x, point.getY().doubleValue() + y);
                        for (int i = 1; i < outline.getPoints().size(); i++)
                         {
                            point = outline.getPoints().get(i);
                             polygon.lineTo(point.getX().doubleValue() + x, point.getY().doubleValue() + y);
                         }
                         g.fill(polygon);
                     }
                 }
             }
         }
     }
 
     private int fill(int x, int y, int fillColor)
     {
         WritableRaster r = preview.getRaster();
         int sampleColor = r.getPixel(x, y, (int[])null)[0];
         if (sampleColor == fillColor)
             return 0;
         LinkedList<PointI> queue = new LinkedList<PointI>();
         queue.add(new PointI(x, y));
 
         int y1;
         boolean spanLeft, spanRight;
         int perimeter = 0;
         while (!queue.isEmpty())
         {
             PointI p = queue.removeFirst();
             y1 = p.y;
             while (y1 >= 0 && r.getPixel(p.x, y1, dummyArray)[0] == sampleColor)
                 y1--;
             y1++;
             spanLeft = false;
             spanRight = false;
             while (y1 < preview.getHeight() && r.getPixel(p.x, y1, dummyArray)[0] == sampleColor)
             {
                 dummyArray[0] = fillColor;
                 r.setPixel(p.x, y1, dummyArray);
                 if (!spanLeft && p.x > 0 && r.getPixel(p.x - 1, y1, dummyArray)[0] == sampleColor)
                 {
                     queue.addFirst(new PointI(p.x - 1, y1));
                     spanLeft = true;
                 }
                 else if (spanLeft && p.x > 0 && r.getPixel(p.x - 1, y1, dummyArray)[0] != sampleColor)
                 {
                     spanLeft = false;
                     perimeter++;
                 }
 
                 if (!spanRight && p.x < preview.getWidth() - 1 && r.getPixel(p.x + 1, y1, dummyArray)[0] == sampleColor)
                 {
                     queue.addFirst(new PointI(p.x + 1, y1));
                     spanRight = true;
                 }
                 else if (spanRight && p.x < preview.getWidth() - 1 && r.getPixel(p.x + 1, y1, dummyArray)[0] != sampleColor)
                 {
                     spanRight = false;
                     perimeter++;
                 }
 
                 y1++;
             }
             perimeter += 2;
         }
 
         return perimeter;
     }
 
     private PointI[] findNonEmptyPixel()
     {
         WritableRaster r = preview.getRaster();
         for (; lastCleared.y < preview.getHeight(); lastCleared.y++)
             for (lastCleared.x = 0; lastCleared.x < preview.getWidth(); lastCleared.x++)
                 if (r.getPixel(lastCleared.x, lastCleared.y, (int[])null)[0] != 0)
                 {
                     int color = r.getPixel(lastCleared.x, lastCleared.y, (int[])null)[0];
                     int y0 = lastCleared.y * PREVIEW_RESOLUTION_FACTOR - PREVIEW_RESOLUTION_FACTOR * 3;
                     y0 = Math.max(0, y0);
                     int x0 = lastCleared.x * PREVIEW_RESOLUTION_FACTOR - PREVIEW_RESOLUTION_FACTOR * 3;
                     x0 = Math.max(0, x0);
                     int yMax = Math.min(height, y0 + PREVIEW_RESOLUTION_FACTOR * 6);
                     int xMax = Math.min(width, x0 + PREVIEW_RESOLUTION_FACTOR * 6);
                     for (int y1 = y0; y1 < yMax; y1++)
                     {
                         boolean isOutside = false;
                         for (int x1 = x0; x1 < xMax; x1++)
                         {
                             if (!isOutside)
                                 isOutside = getPixel(x1, y1) != color;
                             else if (getPixel(x1, y1) == color)
                                 return new PointI[] {new PointI(lastCleared.x, lastCleared.y), new PointI(x1, y1)};
                         }
                     }
                 }
         return null;
     }
 
     public void saveTo(String file)
     {
         try
         {
             ImageIO.write(preview, "png", new File(file));
         }
         catch (IOException e)
         {
             e.printStackTrace();
         }
     }
 
 //    public void saveWindow(String file)
 //    {
 //        System.out.println("windowLeftCorner: " + windowLowerLeftCorner);
 //        try
 //        {
 //            ImageIO.write(window, "png", new File(file));
 //        }
 //        catch (IOException e)
 //        {
 //            e.printStackTrace();
 //        }
 //    }
 
     public static enum RenderingHint {ROW, COLUMN, SQUARE}
 
 }
