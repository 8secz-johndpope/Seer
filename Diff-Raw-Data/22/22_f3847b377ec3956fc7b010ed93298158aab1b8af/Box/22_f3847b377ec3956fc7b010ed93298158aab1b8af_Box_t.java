 package NothingHere;
 
 import java.awt.Color;
 import javax.swing.JLabel;
 
 /**
  *
 * @author C0ldF0x
 * All rights reserved.
 * 
  */
 public class Box extends JLabel{
     
     static Color colorList[] = {Color.red, Color.blue, Color.green, Color.pink, Color.orange, Color.black, Color.gray};
     Color color;
     boolean selected;
     
     public Box(){
         color=Color.white;
         selected=false;
         this.setBackground(color);
         setVisible(true);
         this.setOpaque(true);
     }
 
     void colorize() {
         color=colorList[(int)(Math.random()*colorList.length)];
         setBackground(color);
     }
     
 }
