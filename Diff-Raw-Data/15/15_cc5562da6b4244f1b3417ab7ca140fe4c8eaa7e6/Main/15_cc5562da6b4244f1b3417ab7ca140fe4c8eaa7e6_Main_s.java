 /*
  * Main.java
  *
  * Created on May 20, 2008, 6:30 PM
  *
  * To change this template, choose Tools | Template Manager
  * and open the template in the editor.
  */
 
 package slickbrickles.ui;
 
 import slickbrickles.*;
 
 /**
  *
  * @author henry
  */
 public class Main {
     public static MainGame theGame;
    public static StateHandeler state;
     public static void main(String[] args) {
        state = new StateHandeler();
         state.start();
     //    theGame.start();
     }
     
 }
