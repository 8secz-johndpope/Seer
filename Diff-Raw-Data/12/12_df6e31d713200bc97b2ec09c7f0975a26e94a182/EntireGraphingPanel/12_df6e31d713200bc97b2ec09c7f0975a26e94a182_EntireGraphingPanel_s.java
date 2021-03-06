 /* OpenLogViewer
  *
  * Copyright 2011
  *
  * This file is part of the OpenLogViewer project.
  *
  * OpenLogViewer software is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * OpenLogViewer software is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with any OpenLogViewer software.  If not, see http://www.gnu.org/licenses/
  *
  * I ask that if you make any changes to this file you fork the code on github.com!
  *
  */
 package org.diyefi.openlogviewer.graphing;
 
 import java.awt.Dimension;
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.awt.event.MouseEvent;
 import java.awt.event.MouseListener;
 import java.awt.event.MouseMotionListener;
 
 import javax.swing.JPanel;
 import javax.swing.Timer;
 
 import org.diyefi.openlogviewer.OpenLogViewerApp;
 import org.diyefi.openlogviewer.genericlog.GenericLog;
 import org.diyefi.openlogviewer.graphing.MultiGraphLayeredPane;
 
 /**
  *
  * @author Ben Fenner
  */
 public class EntireGraphingPanel extends JPanel implements ActionListener, MouseMotionListener, MouseListener {
 
 	public EntireGraphingPanel() {
         super();
         init();
     }
 
     private void init() {
     	this.setName("Graphing Panel");
         this.setLayout(new java.awt.BorderLayout());
         multiGraph = new MultiGraphLayeredPane();
         multiGraph.setPreferredSize(new Dimension(600, 400));
         this.add(multiGraph, java.awt.BorderLayout.CENTER);
         graphPositionPanel = new GraphPositionPanel();
         graphPositionPanel.setPreferredSize(new Dimension(600, 20));
         this.add(graphPositionPanel, java.awt.BorderLayout.SOUTH);
         resetGraphPosition();
         setGraphPositionMax();
         playing = false;
         timer = new Timer(10, this);
         timer.setInitialDelay(0);
         addMouseListener(this);
         addMouseMotionListener(this);
         addMouseListener(multiGraph.getInfoPanel());
         addMouseMotionListener(multiGraph.getInfoPanel());
         dragging = false;
         zoom = 1;
     }
     
     public void actionPerformed(ActionEvent e) {
         if (playing && graphPosition < graphPositionMax) {
         	moveGraphPosition(1);
         } else {
            stop();
         }
 
     }
     
     public MultiGraphLayeredPane getMultiGraphLayeredPane(){
     	return multiGraph;
     }
     
     public GraphPositionPanel getGraphPositionPanel(){
     	return graphPositionPanel;
     }
     
 
   public void zoomIn() {
       if (zoom < 50) {
           zoom++;
       }
       multiGraph.initGraphs();
       repaint();
   }
 
   public void zoomOut() {
       if (zoom > 1) {
           zoom--;
       }
       multiGraph.initGraphs();
       repaint();
   }
 
     public void play(){
     	if (playing) {
             pause();
         } else {
         	playing = true;
             timer.start();
         }
     }
     
     public void pause(){
     	playing = false;
     	timer.stop();
     }
     
     /**
      * increases speed of the graph by 1 ms until 0, at which speed cannot be advanced any further and will essentially update as fast as possible
      */
     public void fastForward() {
     	int currentDelay = timer.getDelay();
     	if(currentDelay > 0){
     		timer.setDelay(currentDelay - 1);
     	}
     }
     
     public void eject() {
         resetGraphPosition();
         multiGraph.initGraphs();
     }
     
     public void stop(){
     	playing = false;
     	timer.stop();
     	resetGraphPosition();
     }
     
     /**
      * Slows the speed of playback by 1 ms
      */
     public void slowDown() {
     	int currentDelay = timer.getDelay();
     	timer.setDelay(currentDelay + 1);
     }
     
     public int getGraphPosition(){
     	return graphPosition;
     }
     
     public int getGraphPositionMax(){
     	return graphPositionMax;
     }
     
     public int getZoom(){
     	return zoom;
     }
     
     public void moveGraphPosition(int amount){
     	int newPos = graphPosition + amount;
     	setGraphPosition(newPos);
     }
     
     public void setGraphPosition(int newPos){
     	graphPosition = newPos;
     	repaint();
     }
     
     public void setLog(GenericLog genLog) {
     	playing = false;
     	resetGraphPosition();
     	multiGraph.setLog(genLog);
     	graphPositionPanel.setLog(genLog);
     }
     
     public void setGraphPositionMax(){
     	boolean found = false;
         for (int i = 0; i < multiGraph.getComponentCount() && !found; i++) {
             if (multiGraph.getComponent(i) instanceof SingleGraphPanel) {
                 SingleGraphPanel gl = (SingleGraphPanel) multiGraph.getComponent(i);
                 graphPositionMax = gl.graphSize();
                 found = true;
             }
         }
     }
     
     public void resetGraphPosition(){
     	setGraphPosition(0);
     }
     
     public boolean isPlaying(){
     	return playing;
     }
     
   //MOUSE LISTENER FUNCTIONALITY
     @Override
     public void mouseClicked(MouseEvent e) {
     	mouseClicked(e.getX());
     }
     
     public void mouseClicked(int xPosition){
     	int graphPosition = OpenLogViewerApp.getInstance().getEntireGraphingPanel().getGraphPosition();
     	int graphPositionMax = OpenLogViewerApp.getInstance().getEntireGraphingPanel().getGraphPositionMax();
         if (!dragging) {
             int move = (xPosition / zoom) - ((this.getWidth() / 2) / zoom);
             if (move + graphPosition < graphPositionMax) {
                 if (move + graphPosition < 0) {
                 	OpenLogViewerApp.getInstance().getEntireGraphingPanel().resetGraphPosition();
                 } else {
                 	OpenLogViewerApp.getInstance().getEntireGraphingPanel().moveGraphPosition(move);
                 }
             } else {
             	OpenLogViewerApp.getInstance().getEntireGraphingPanel().setGraphPosition(graphPositionMax);
             }
             OpenLogViewerApp.getInstance().getMultiGraphLayeredPane().initGraphs();
         } else if (dragging) {
             dragging = false;
         }
     }
     
 
     @Override
     public void mouseDragged(MouseEvent e) {
         //mouseMoved(e);
     }
 
     @Override
     public void mouseMoved(MouseEvent e) {
         //repaint(); // call repaint because otherwise we are at the whim of the speed of playback to update mouse info
     }
 
     @Override
     public void mouseEntered(MouseEvent e) {
         //repaint();
     }
 
     @Override
     public void mouseExited(MouseEvent e) {
         //repaint();
     }
 
     @Override
     public void mousePressed(MouseEvent e) {
     
     }
 
     @Override
     public void mouseReleased(MouseEvent e) {
     	
     }
     
     private MultiGraphLayeredPane multiGraph;
     private GraphPositionPanel graphPositionPanel;
     private int graphPosition;
     private int graphPositionMax;
     private boolean playing;
     private Timer timer;
     private boolean dragging;
     private int zoom;
     private static final long serialVersionUID = 6880240079754110792L;
 
 }
