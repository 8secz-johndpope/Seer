 /*
  * $Id$
  *
  * Copyright  1997-2004 Unidata Program Center/University Corporation for
  * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
  * support@unidata.ucar.edu.
  *
  * This library is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as published by
  * the Free Software Foundation; either version 2.1 of the License, or (at
  * your option) any later version.
  *
  * This library is distributed in the hope that it will be useful, but
  * WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
  * General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public License
  * along with this library; if not, write to the Free Software Foundation,
  * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
  */
 
 package edu.wisc.ssec.mcidasv.chooser.adde;
 
 import java.awt.Dimension;
 import java.util.ArrayList;
 import java.util.Hashtable;
 import java.util.List;
 import java.util.Vector;
 
 import javax.swing.JComponent;
 import javax.swing.JRadioButton;
 import javax.swing.ListSelectionModel;
 
 import org.w3c.dom.Element;
 
 import ucar.unidata.data.DataSource;
 import ucar.unidata.idv.chooser.IdvChooserManager;
 import ucar.unidata.ui.ChooserList;
 import ucar.unidata.util.GuiUtils;
 
 /**
  * A chooser for adde front products
  *
  *
  *
  * @author IDV development team
 * @version $Revision$Date: 2008/09/16 20:49:50 $
  */
 public class AddeFrontChooser extends AddeChooser {
     /** for gui */
     ChooserList timesList;
 
     /** for gui */
     JRadioButton forecastBtn;
 
     /** for gui */
     JRadioButton observedBtn;
 
     /**
      * Make a new one
      *
      * @param mgr The manager
      * @param root The xml element that defined this object
      *
      */
     public AddeFrontChooser(IdvChooserManager mgr, Element root) {
         super(mgr, root);
     }
 
     /**
      * update the buttons
      */
     protected void updateStatus() {
         super.updateStatus();
         setHaveData(true);
     }
 
     /**
      * Update the widget with the latest data.
      *
      * @throws Exception On badness
      */
     @Override public void handleUpdate() throws Exception {
        updateServerList();
         readTimes();
         updateStatus();
         showNormalCursor();
         saveServerState();
     }
 
     /**
      * Make the GUI
      *
      * @return The GUI
      */
     protected JComponent doMakeContents() {
         forecastBtn = new JRadioButton("Forecast Fronts", false);
         observedBtn = new JRadioButton("Analysis Fronts", true);
         GuiUtils.buttonGroup(observedBtn, forecastBtn);
 
         List comps = processServerComponents();
         GuiUtils.tmpInsets = GuiUtils.INSETS_5;
 
         comps.add(GuiUtils.rLabel("Latest:"));
         comps.add(GuiUtils.left(GuiUtils.hbox(observedBtn, forecastBtn)));
 
         timesList = new ChooserList();
         timesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
         Vector items = new Vector();
         for (int i = 0; i < 10; i++) {
             if (i == 0) {
                 items.add("Most recent day");
             } else {
                 items.add((i + 1) + " most recent days");
             }
         }
         timesList.setListData(items);
         timesList.setSelectedIndex(0);
         timesList.getScroller().setPreferredSize(new Dimension(200, 100));
         //        comps.add(GuiUtils.rLabel("Days:"));
         //        comps.add(GuiUtils.left(timesList.getScroller()));
 
         JComponent buttons = getDefaultButtons();
         setHaveData(true);
         return GuiUtils.topLeft(GuiUtils.vbox(GuiUtils.doLayout(comps, 2, GuiUtils.WT_NY,
                 GuiUtils.WT_N), buttons));
     }
 
 
     /**
      * User said go, we go. Simply get the list of images
      * from the imageChooser and create the ADDE.IMAGE
      * DataSource
      *
      */
     public void doLoadInThread() {
         List   urls   = new ArrayList();
         int    index  = timesList.getSelectedIndex();
         String server = getAddeServer("AddeFrontChooser.doLoadInThread").getName();
         String type   = (forecastBtn.isSelected()
                          ? "SRP"
                          : "SUS");
         for (int i = 0; i <= index; i++) {
             String url = "adde://" + server
                          + "/wxtext?group=RTWXTEXT&apro=COD&astn=" + type
                          + "&day=%DAY-" + i + "%";
             urls.add(url);
         }
         Hashtable ht = new Hashtable();
         //TODO: Change the name, maybe add the date
         ht.put(DataSource.PROP_TITLE, (forecastBtn.isSelected()
                                        ? "Forecast Front"
                                        : "Analysis Fronts"));
         makeDataSource(urls, "TEXT.FRONT", ht);
         saveServerState();
     }
 
     /**
      * get the adde server grup type to use
      *
      * @return group type
      */
     @Override protected String getGroupType() {
         return "text";
     }
 
     public String getDataType() {
         return "TEXT";
     }
 }
 
