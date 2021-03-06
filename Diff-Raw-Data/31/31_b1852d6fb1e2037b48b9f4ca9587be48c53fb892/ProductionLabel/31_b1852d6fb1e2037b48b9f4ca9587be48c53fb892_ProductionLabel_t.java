 package net.sf.freecol.client.gui.panel;
 
 import java.awt.Color;
 import java.awt.Dimension;
 import java.awt.Font;
 import java.awt.Graphics;
 import java.awt.image.BufferedImage;
 import java.util.logging.Logger;
 
 import javax.swing.ImageIcon;
 import javax.swing.JComponent;
 
 import net.sf.freecol.client.ClientOptions;
 import net.sf.freecol.client.gui.Canvas;
 import net.sf.freecol.common.model.Goods;
 import net.sf.freecol.common.model.GoodsType;
 
 /**
  * The ProductionLabel represents Goods that are produced in a
  * WorkLocation or Settlement. It is similar to the GoodsLabel.
  */
 public final class ProductionLabel extends JComponent {
 
     public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
 
     public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";
 
     public static final String REVISION = "$Revision$";
 
     private static Logger logger = Logger.getLogger(ProductionLabel.class.getName());
 
     private final Canvas parent;
 
     /**
      * The maximum number of goodsIcons to display.
      */
     private int maxIcons = 7;
 
     /**
      * Whether to display positive integers with a "+" sign.
      */
     private boolean drawPlus = false;
 
     /**
      * Whether the ProductionLabel should be centered.
      */
     private boolean centered = true;
 
     /**
      * The compressed width of the ProductionLabel.
      */
     private int compressedWidth = -1;
 
     /**
      * The type of goods being produced.
      */
     private GoodsType goodsType;
 
     /**
      * The goodsIcon for this type of production.
      */
     private ImageIcon goodsIcon;
 
     /**
      * The amount of goods being produced.
      */
     private int production;
 
     /**
      * The amount of goods that could be produced.
      */
     private int maximumProduction = -1;
 
     /**
      * The smallest number to display above the goodsIcons.
      */
     private int displayNumber;
 
     /**
      * The smallest number to display above the goodsIcons.
      * used to Show stored items in ReportColonyPanel
      */
     private int stockNumber = -1;
 
     /**
      * Creates a new <code>ProductionLabel</code> instance.
      *
      * @param goods a <code>Goods</code> value
      * @param parent a <code>Canvas</code> value
      */
     public ProductionLabel(Goods goods, Canvas parent) {
         this(goods.getType(), goods.getAmount(), -1, parent);
     }
 
     /**
      * Creates a new <code>ProductionLabel</code> instance.
      *
      * @param goodsType an <code>int</code> value
      * @param amount an <code>int</code> value
      * @param parent a <code>Canvas</code> value
      */
     public ProductionLabel(GoodsType goodsType, int amount, Canvas parent) {
         this(goodsType, amount, -1, parent);
     }
 
     /**
      * Creates a new <code>ProductionLabel</code> instance.
      *
      * @param goodsType an <code>int</code> value
      * @param amount an <code>int</code> value
      * @param maximumProduction an <code>int</code> value
      * @param parent a <code>Canvas</code> value
      */
     public ProductionLabel(GoodsType goodsType, int amount, int maximumProduction, Canvas parent) {
         super();
         this.parent = parent;
         this.production = amount;
         this.goodsType = goodsType;
         this.maximumProduction = maximumProduction;
         ClientOptions options = parent.getClient().getClientOptions();
         maxIcons = options.getInteger(ClientOptions.MAX_NUMBER_OF_GOODS_IMAGES);
         displayNumber = options.getInteger(ClientOptions.MIN_NUMBER_FOR_DISPLAYING_GOODS_COUNT);
 
         
         setFont(new Font("Dialog", Font.BOLD, 12));
         if (amount < 0) {
             setForeground(Color.RED);
         } else {
             setForeground(Color.WHITE);
         }
         if (goodsType != null) {
             goodsIcon = parent.getImageProvider().getGoodsImageIcon(goodsType);
             compressedWidth = goodsIcon.getIconWidth()*2;
            setToolTipText(String.valueOf(amount) + " " + goodsType.getName());
         }
     }
     
 
     /**
      * Returns the parent Canvas object.
      * 
      * @return This ProductionLabel's Canvas.
      */
     public Canvas getCanvas() {
         return parent;
     }
 
     /**
      * Get the <code>DisplayNumber</code> value.
      *
      * @return an <code>int</code> value
      */
     public int getDisplayNumber() {
         return displayNumber;
     }
 
     /**
      * Set the <code>DisplayNumber</code> value.
      *
      * @param newDisplayNumber The new DisplayNumber value.
      */
     public void setDisplayNumber(final int newDisplayNumber) {
         this.displayNumber = newDisplayNumber;
     }
 
     /**
      * Get the <code>GoodsIcon</code> value.
      *
      * @return an <code>ImageIcon</code> value
      */
     public ImageIcon getGoodsIcon() {
         return goodsIcon;
     }
 
     /**
      * Set the <code>GoodsIcon</code> value.
      *
      * @param newGoodsIcon The new GoodsIcon value.
      */
     public void setGoodsIcon(final ImageIcon newGoodsIcon) {
         this.goodsIcon = newGoodsIcon;
     }
 
     /**
      * Get the <code>Production</code> value.
      *
      * @return an <code>int</code> value
      */
     public int getProduction() {
         return production;
     }
 
     /**
      * Set the <code>Production</code> value.
      *
      * @param newProduction The new Production value.
      */
     public void setProduction(final int newProduction) {
         this.production = newProduction;
         setToolTipText(String.valueOf(production) + " " + goodsType.getName());
     }
 
     /**
      * Get the <code>MaximumProduction</code> value.
      *
      * @return an <code>int</code> value
      */
     public int getMaximumProduction() {
         return maximumProduction;
     }
 
     /**
      * Set the <code>MaximumProduction</code> value.
      *
      * @param newMaximumProduction The new MaximumProduction value.
      */
     public void setMaximumProduction(final int newMaximumProduction) {
         this.maximumProduction = newMaximumProduction;
     }
 
     /**
      * Get the <code>MaxGoodsIcons</code> value.
      *
      * @return an <code>int</code> value
      */
     public int getMaxGoodsIcons() {
         return maxIcons;
     }
 
     /**
      * Set the <code>MaxGoodsIcons</code> value.
      *
      * @param newMaxGoodsIcons The new MaxGoodsIcons value.
      */
     public void setMaxGoodsIcons(final int newMaxGoodsIcons) {
         this.maxIcons = newMaxGoodsIcons;
     }
 
     /**
      * Get the <code>stockNumber</code> value.
      * used to Show stored items in ReportColonyPanel
      *
      * @return an <code>int</code> value
      */
     public int getStockNumber() {
         return stockNumber;
     }
 
     /**
      * Set the <code>stockNumber</code> value.
      * used to Show stored items in ReportColonyPanel
      *
      * @param newStockNumber The new StockNumber value.
      */
     public void setStockNumber(final int newStockNumber) {
         this.stockNumber = newStockNumber;
     }
 
     /**
      * Get the <code>DrawPlus</code> value.
      *
      * @return a <code>boolean</code> value
      */
     public boolean drawPlus() {
         return drawPlus;
     }
 
     /**
      * Set the <code>DrawPlus</code> value.
      *
      * @param newDrawPlus The new DrawPlus value.
      */
     public void setDrawPlus(final boolean newDrawPlus) {
         this.drawPlus = newDrawPlus;
     }
 
     /**
      * Get the <code>Centered</code> value.
      *
      * @return a <code>boolean</code> value
      */
     public boolean isCentered() {
         return centered;
     }
 
     /**
      * Set the <code>Centered</code> value.
      *
      * @param newCentered The new Centered value.
      */
     public void setCentered(final boolean newCentered) {
         this.centered = newCentered;
     }
 
     /**
      * Get the <code>CompressedWidth</code> value.
      *
      * @return an <code>int</code> value
      */
     public int getCompressedWidth() {
         return compressedWidth;
     }
 
     /**
      * Set the <code>CompressedWidth</code> value.
      *
      * @param newCompressedWidth The new CompressedWidth value.
      */
     public void setCompressedWidth(final int newCompressedWidth) {
         this.compressedWidth = newCompressedWidth;
     }
 
     /**
      * Overrides the <code>getPreferredSize</code> method.
      *
      * @return a <code>Dimension</code> value
      */
     public Dimension getPreferredSize() {
 
         if (goodsIcon == null || production == 0) {
             return new Dimension(0, 0);
         } else {
             return new Dimension(getPreferredWidth(), goodsIcon.getImage().getHeight(null));
         }
     }
 
 
     // TODO: get rid of the ugly code duplication
     /**
      * Returns only the width component of the preferred size.
      *
      * @return an <code>int</code> value
      */
     public int getPreferredWidth() {
 
         if (goodsIcon == null || production == 0) {
             return 0;
         }
 
         int drawImageCount = Math.min(Math.abs(production), maxIcons);
 
         int iconWidth = goodsIcon.getIconWidth();
         int pixelsPerIcon = iconWidth / 2;
         if (pixelsPerIcon - iconWidth < 0) {
             pixelsPerIcon = (compressedWidth - iconWidth) / drawImageCount;
         }
         int maxSpacing = iconWidth;
 
         /* TODO Tune this: all icons are the same width, but many
          * do not take up the whole width, eg. bells
          */
         boolean iconsTooFarApart = pixelsPerIcon > maxSpacing;
         if (iconsTooFarApart) {
             pixelsPerIcon = maxSpacing;
         }
 
         return pixelsPerIcon * (drawImageCount - 1) + iconWidth;
 
     }
 
     /**
      * Paints this ProductionLabel.
      * 
      * @param g The graphics context in which to do the painting.
      */
     public void paintComponent(Graphics g) {
 
         if (goodsIcon == null || (production == 0 && stockNumber<0) ) {
             return;
         }
 
         int drawImageCount = Math.min(Math.abs(production), maxIcons);
         if (drawImageCount==0) {
             drawImageCount=1;
         }
 
         int iconWidth = goodsIcon.getIconWidth();
         int pixelsPerIcon = iconWidth / 2;
         if (pixelsPerIcon - iconWidth < 0) {
             pixelsPerIcon = (compressedWidth - iconWidth) / drawImageCount;
         }
         int maxSpacing = iconWidth;
 
         /* TODO Tune this: all icons are the same width, but many
          * do not take up the whole width, eg. bells
          */
         boolean iconsTooFarApart = pixelsPerIcon > maxSpacing;
         if (iconsTooFarApart) {
             pixelsPerIcon = maxSpacing;
         }
         int coverage = pixelsPerIcon * (drawImageCount - 1) + iconWidth;
         int leftOffset = 0;
 
         boolean needToCenterImages = centered && coverage < getWidth();
         if (needToCenterImages) {
             leftOffset = (getWidth() - coverage)/2;
         }
 
         int width = Math.max(getWidth(), coverage);
         int height = Math.max(getHeight(), goodsIcon.getImage().getHeight(null));
         setSize(new Dimension(width, height));
 
 
         // Draw the icons onto the image:
         for (int i = 0; i < drawImageCount; i++) {
             goodsIcon.paintIcon(null, g, leftOffset + i*pixelsPerIcon, 0);
         }
 
         if (production >= displayNumber || production < 0 || maxIcons < production || stockNumber>0) {
             String number = "";
             if (stockNumber >= 0 ) {
                 number = Integer.toString(stockNumber);  // Show stored items in ReportColonyPanel
                 drawPlus = true;
             }
             if (production >=0 && drawPlus ) {
                 number = number + "+" + Integer.toString(production);
             } else {
                 number = number + Integer.toString(production);
             }
             if (maximumProduction > production && production > 0) {
                 number = number + "/" + String.valueOf(maximumProduction);
             }
             BufferedImage stringImage = parent.getGUI().createStringImage(this, number, getForeground(), width, 12);
             int textOffset = leftOffset + (coverage - stringImage.getWidth())/2;
             textOffset = (textOffset >= 0) ? textOffset : 0;
             g.drawImage(stringImage, textOffset,
                     goodsIcon.getIconHeight()/2 - stringImage.getHeight()/2, null);
         }
     }
 
 }
