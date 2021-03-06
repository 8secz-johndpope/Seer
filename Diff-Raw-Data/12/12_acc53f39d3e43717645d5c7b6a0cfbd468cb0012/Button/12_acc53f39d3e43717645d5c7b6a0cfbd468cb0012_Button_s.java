 /*
  * Button.java
  *
  * Copyright (c) 2006-2007 Operational Dynamics Consulting Pty Ltd, and Others
  * 
  * The code in this file, and the library it is a part of, are made available
  * to you by the authors under the terms of the "GNU General Public Licence,
  * version 2" plus the "Classpath Exception" (you may link to this code as a
  * library into other programs provided you don't make a derivation of it).
  * See the LICENCE file for the terms governing usage and redistribution.
  */
 package org.gnome.gtk;
 
 /**
  * A Widget that emits a signal when clicked on. Button can hold any just
  * about any other Widget as its child. The most commonly used child is a
  * Label, and there are convenience methods to help you just create a button
  * with the given text automatically, notably the
  * {@link Button#Button(String) Button(String)} constructor. <img
  * src="Button.png" class="snapshot">
  * 
  * <p>
  * Since Button is a Bin it strictly only has one child. Internally, however,
  * it may have both an icon image and some text (which is the look commonly
  * seen in the action buttons in Dialog boxes). You can add such an image to a
  * Button by calling {@link #setImage(Image) setImage()}; this works
  * alongside and with {@link #setLabel(String) setLabel()}. The machinery
  * within Button will manage creating the necessary internal structure
  * (HBoxes, Alignments, etc).
  * 
  * @author Andrew Cowie
  * @author Vreixo Formoso
  * @author Mario Torre
  * @since 4.0.0
  */
 public class Button extends Bin
 {
     protected Button(long pointer) {
         super(pointer);
     }
 
     /**
      * Create an "empty" button to use as a Container. You'll need to
      * {@link org.gnome.gtk.Container#add(Widget) add()} the Widget which will
      * be the Button's child.
      * 
      * <p>
      * For most uses {@link #setImage(Image) setImage()} and
      * {@link #setLabel(String) setLabel()} will more than take care of
      * things; they can be used together.
      * 
      * @since 4.0.0
      */
     public Button() {
         super(GtkButton.createButton());
     }
 
     /**
      * Create a button with a Label as its child. Simply specify the text you
      * want for the Label and a Button will be created accordingly. This is
      * quite a common case - in fact, we're generally more used to thinking of
      * Buttons as being Labels that you can press than as arbitrary Widget
      * Containers.
      * 
      * <p>
      * Note that you <i>can</i> use {@link #setImage(Image) setImage()} on a
      * Button created this way.
      * 
      * @param text
      *            the text you wish on the Label that will be created in the
      *            Button.
      * @since 4.0.0
      */
     public Button(String text) {
         // TODO replace with GtkButton.createButtonWithMnemonic(text) ?
         super(GtkButton.createButtonWithLabel(text));
     }
 
     /**
      * Create a new Button with a Label and Image from a StockItem. By using a
      * system StockItem, the newly created Button with use the same Label and
      * Image as other GNOME applications. To ensure consistent look-n-feel
      * between applications, it is highly recommend that you use provided
      * StockItems whenever possible.
      * 
      * @param stock
      *            The stock item that will determine the text and icon of the
      *            Button.
      * @since 4.0.4
      */
     public Button(Stock stock) {
         super(GtkButton.createButtonFromStock(stock.getStockId()));
     }
 
     /**
      * Set the text showing in the Button.
      * 
      * <p>
      * If you created an empty Button without a Label using
      * {@link #Button() Button()}, this will create a Label nested in an
      * Alignment for you. That <i>won't</i> work if you create an empty
      * Button then put a custom Widget in place with
      * {@link Container#add(Widget) add()} instead of employing this method
      * and/or {@link #setImage(Image) setImage()}).
      * 
      * @since 4.0.0
      */
     public void setLabel(String text) {
         GtkButton.setLabel(this, text);
     }
 
     /**
      * Get the text showing on the Button.
      * 
      * @return the text of the Label, or <code>null</code> if the no-arg
      *         constructor was used and you've just got an arbitrary
      *         Widget-containing-Button, not the more usual Button-with-Label.
      * @since 4.0.0
      */
     public String getLabel() {
         // return GtkButton.getLabel(this);
         return super.getPropertyString("label");
     }
 
     /**
      * Paint an arbitrary Image over this Button. If this is used on an empty
      * Button then the Button will be the size of the Image and will what is
      * activatable. On the other hand, you <i>can</i> use this in conjunction
      * with {@link #setLabel(String) setLabel()} in which case you will get an
      * icon on the left and the label text on the right.
      * 
      * @since 4.0.5
      */
     public void setImage(Image image) {
         GtkButton.setImage(this, image);
     }
 
     /**
      * Get the Image associated with this Button.
      * 
      * @return the Widget associated with this Button using the
      *         {@link #setImage(Image) setImage()} method, or
      *         <code>null</code> if the Button doesn't have one set.
      * 
      * @since 4.0.5
      */
     public Image getImage() {
         return (Image) GtkButton.getImage(this);
     }
 
     /**
      * Get the horizontal alignment of the child Widget within this Button.
      * The return will range from 0.0 (full left) to 1.0 (full right).
      */
     public float getAlignmentX() {
         float[] x = new float[1];
         float[] y = new float[1];
 
         GtkButton.getAlignment(this, x, y);
 
         return x[0];
     }
 
     /**
      * Get the vertical alignment of the child Widget within this Button. The
      * return will range from 0.0 (top) to 1.0 (bottom).
      */
     public float getAlignmentY() {
         float[] x = new float[1];
         float[] y = new float[1];
 
         GtkButton.getAlignment(this, x, y);
 
         return y[0];
     }
 
     /**
      * Set the alignment of the child Widget within the Button. This has no
      * impact unless the child of the Button is a Misc (which of course the
      * default child, a Label, is).
      * 
      * @param xalign
      *            from 0.0f representing fully left-aligned through 1.0f
      *            representing fully right-aligned.
      * @param yalign
      *            from 0.0f for fully top-aligned through 1.0f for fully
      *            bottom-aligned
      */
     public void setAlignment(float xalign, float yalign) {
         GtkButton.setAlignment(this, xalign, yalign);
     }
 
     /**
      * Set the "relief" style used to determine how the edges of this Button
      * will be decorated. The default is {@link ReliefStyle#NORMAL NORMAL}
      * which results in a Button just as you would expect, screaming out to be
      * pressed! There are two other variations, see {@link ReliefStyle} for
      * details.
      * 
      * @since 4.0.1
      */
     public void setRelief(ReliefStyle style) {
         GtkButton.setRelief(this, style);
     }
 
     /**
      * Get the relief style in use around this Button.
      * 
      * @since 4.0.1
      */
     public ReliefStyle getRelief() {
         // return GtkButton.getRelief(this);
         // TODO use real translation layer method
         return (ReliefStyle) getPropertyEnum("relief");
     }
 
     /**
      * Event generated when a user presses and releases a button, causing it
      * to activate.
      * 
      * <p>
      * <i>When the mouse is used to click on a Button this signal will be
      * emitted, but only if the cursor is still in the Button when the mouse
      * button is released. You're probably used to this behaviour without
      * realizing it.</i>
      */
     public interface CLICKED extends GtkButton.CLICKED
     {
         public void onClicked(Button source);
     }
 
     /**
      * Hook up a handler to receive "clicked" events on this Button. A typical
      * example of how this is used is as follows:
      * 
      * <pre>
      * Button b;
      *              
      * b.connect(new Button.CLICKED() {
      *     public void onClicked(Button source) {
      *         // do something!
      *     }
      * }
      * </pre>
      * 
      * <p>
      * You can of course create a subclass of Button.CLICKED and then use
      * instances of it if you have highly complicated algorithms to implement.
      * 
      * <p>
      * If you implement Button.CLICKED in the class you're currently working
      * on, then you use a technique called "self-delegation" which can
      * sometimes work well;
      * 
      * <pre>
      * b.connect(this);
      * </pre>
      */
     public void connect(CLICKED handler) {
         GtkButton.connect(this, handler);
     }
 
     /*
      * ACTIVATE: "Applications should never connect to this signal, but use
      * the 'clicked' signal."
      */
     /*
      * ENTERED, PRESSED, etc: "deprecated"
      */
 }
