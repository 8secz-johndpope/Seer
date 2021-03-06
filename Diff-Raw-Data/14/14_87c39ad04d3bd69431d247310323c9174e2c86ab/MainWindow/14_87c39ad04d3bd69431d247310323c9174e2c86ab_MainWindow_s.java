 /*
  * Copyright (c) 2007-2011 by The Broad Institute, Inc. and the Massachusetts Institute of
  * Technology.  All Rights Reserved.
  *
  * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
  * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
  *
  * THE SOFTWARE IS PROVIDED "AS IS." THE BROAD AND MIT MAKE NO REPRESENTATIONS OR
  * WARRANTES OF ANY KIND CONCERNING THE SOFTWARE, EXPRESS OR IMPLIED, INCLUDING,
  * WITHOUT LIMITATION, WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
  * PURPOSE, NONINFRINGEMENT, OR THE ABSENCE OF LATENT OR OTHER DEFECTS, WHETHER
  * OR NOT DISCOVERABLE.  IN NO EVENT SHALL THE BROAD OR MIT, OR THEIR RESPECTIVE
  * TRUSTEES, DIRECTORS, OFFICERS, EMPLOYEES, AND AFFILIATES BE LIABLE FOR ANY DAMAGES
  * OF ANY KIND, INCLUDING, WITHOUT LIMITATION, INCIDENTAL OR CONSEQUENTIAL DAMAGES,
  * ECONOMIC DAMAGES OR INJURY TO PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER
  * THE BROAD OR MIT SHALL BE ADVISED, SHALL HAVE OTHER REASON TO KNOW, OR IN FACT
  * SHALL KNOW OF THE POSSIBILITY OF THE FOREGOING.
  */
 
 package org.broad.igv.graph;
 
import org.broad.igv.feature.BasicFeature;
import org.broad.igv.feature.Exon;
import org.broad.igv.feature.GeneManager;
 import org.broad.igv.feature.genome.Genome;
import org.broad.igv.tools.IgvTools;
import org.broad.igv.track.TestUtils;
 import org.broad.igv.ui.IGV;
 
 import javax.swing.*;
 import java.io.IOException;
import java.util.List;
 
 /**
  * @author jrobinso
  * @date Oct 12, 2010
  */
 public class MainWindow extends JFrame {
 
 
     // The constructor
 
     public MainWindow() throws IOException {
 
         Genome genome = IGV.getInstance().getGenomeManager().getCurrentGenome();
 
         // Create a test graph and graph panel
         //Graph graph =  GeneUtils.getGraphFor("ARF1"); // createGraph();
         Graph graph =  GeneUtils.getGraphFor("TCOF1", genome); // createGraph();
 
         GraphPanel2 graphPanel = new GraphPanel2();
         graphPanel.setGraph(graph);
 
         // The "add" method is a standard Swing method, inherite from JPanel
         add(graphPanel);
 
         // Set initial size
         graphPanel.setSize(800, 400);
 
 
     }
 
 
 
     // Create graph for a gene
 
 
     public static void main(String[] args) throws IOException {
 
         MainWindow window = new MainWindow();
         window.setSize(600, 400);
         window.setVisible(true);
 
     }
 
 }
