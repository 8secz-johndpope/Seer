 package com.vaadin.demo.sampler.features.layouts;
 
 import com.vaadin.ui.Component;
 import com.vaadin.ui.CssLayout;
 import com.vaadin.ui.Label;
 import com.vaadin.ui.Panel;
 import com.vaadin.ui.VerticalLayout;
 
 @SuppressWarnings("serial")
 public class CssLayoutsExample extends VerticalLayout {
 
     public CssLayoutsExample() {
         setMargin(true);
 
         /*
         * Note that adding inline style like this is a very, VERY bad
         * programming habit. The correct place for CSS is in the theme. We do
         * it here to keep the CSS in the java code for demonstration purposes.
          */
         Label demostyle = new Label(
                 "<style>"
                         + ".floatedpanel {float:right;} "
                         + ".footer {width: 3in; padding: 3px;margin-left:auto;margin-right:auto;"
                         + "clear:both; height:40px; background-color: grey; white-space:normal;} "
                         + ".brick {text-align:center;width: 100px; height: 100px; margin: 10px;padding:10px;"
                         + "background-color:#1E2123; color:white;float:left; border: 3px solid black;} "
                         + "</style>", Label.CONTENT_XHTML);
 
         addComponent(demostyle);
 
         final Panel panel = new Panel("Panel");
         panel.setStyleName("floatedpanel");
         panel.setWidth("30%");
         panel.setHeight("370px");
        panel.addComponent(new Label("This panel is 30% wide"
                + "and 370px high (defined on the server side) "
                 + "and floated right (with custom css). "
                + "Try resizing the browser window to see "
                + "how the black boxes (floated left) "
                + "behave. Every third of them has colored text "
                + "to demonstrate the dynamic css injection."));
 
         final Label bottomCenter = new Label(
                "I'm a 3 inches wide footer at the bottom "
                         + "of the layout (and centered unless I'm in IE6)");
         bottomCenter.setSizeUndefined(); // disable 100% default width
         bottomCenter.setStyleName("footer");
         // bottomCenter.setWidth("50px");
 
         CssLayout cssLayout = new CssLayout() {
             int brickCounter = 0;
 
             @Override
             protected String getCss(Component c) {
                 // colorize every third rendered brick
                 if (c instanceof Brick) {
                     brickCounter++;
                     if (brickCounter % 3 == 0) {
                        // make every third brick colored and bold
                         return "color: #ff6611; font-weight:bold;";
                     }
                 }
                 return null;
             }
         };
 
         cssLayout.setWidth("100%");
 
         cssLayout.addComponent(panel);
         for (int i = 0; i < 15; i++) {
             // add black labels that float left
             cssLayout.addComponent(new Brick());
         }
         cssLayout.addComponent(bottomCenter);
 
         addComponent(cssLayout);
     }
 
     /**
      * A simple label containing text "Brick" and themed black square.
      */
     static class Brick extends Label {
         public Brick() {
             super("Brick");
             // disable 100% that label has by default
             setSizeUndefined();
             setStyleName("brick");
         }
     }
 
 }
