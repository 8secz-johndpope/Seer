 /*
  * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
  * Author: 
  * May 25, 2005
  */
 package pt.up.fe.dceg.neptus.gui;
 
 import java.awt.Color;
 import java.awt.Component;
 import java.awt.Graphics;
 import java.awt.Graphics2D;
 import java.awt.Image;
 import java.util.HashMap;
 
 import javax.swing.ImageIcon;
 import javax.swing.JTree;
 import javax.swing.tree.DefaultMutableTreeNode;
 import javax.swing.tree.DefaultTreeCellRenderer;
 
 import pt.up.fe.dceg.neptus.NeptusLog;
 import pt.up.fe.dceg.neptus.gui.MissionBrowser.State;
 import pt.up.fe.dceg.neptus.gui.tree.ExtendedTreeNode;
 import pt.up.fe.dceg.neptus.plugins.planning.plandb.PlanDBInfo;
 import pt.up.fe.dceg.neptus.types.checklist.ChecklistType;
 import pt.up.fe.dceg.neptus.types.coord.LocationType;
 import pt.up.fe.dceg.neptus.types.map.MapType;
 import pt.up.fe.dceg.neptus.types.map.MarkElement;
 import pt.up.fe.dceg.neptus.types.map.TransponderElement;
 import pt.up.fe.dceg.neptus.types.misc.LBLRangesTimer;
 import pt.up.fe.dceg.neptus.types.mission.ChecklistMission;
 import pt.up.fe.dceg.neptus.types.mission.HomeReference;
 import pt.up.fe.dceg.neptus.types.mission.MapMission;
 import pt.up.fe.dceg.neptus.types.mission.VehicleMission;
 import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
 import pt.up.fe.dceg.neptus.util.ColorUtils;
 import pt.up.fe.dceg.neptus.util.ImageUtils;
 import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystem;
 import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystemsHolder;
 
 /**
  * @author zp
  * @author pdias
  */
 public class MissionTreeCellRenderer extends DefaultTreeCellRenderer {
     
     
     
     private enum Icons{
         PATH_SOURCE("images/"),
         PLAN("plan"),
         PLAN_PATH("plans/"),
         BEACON("beacon"),
         BEACONS_PATH("beacons/"),
         ACOUSTIC("Acoustic"),
         MULTIPLE_VEHICLES("Multp"),
         EXTENSION(".png");
         private final String name;
         private Icons(String name) {
             this.name = name;
         }
 
         /**
          * @return the name
          */
         public String getName() {
             return name;
         }
 
 
     }
 
     private final int MAX_ACCEPTABLE_ELAPSED_TIME = 120;
 
     private static final long serialVersionUID = -2666337254439313801L;
 
     private static final ImageIcon MAP_ICON = ImageUtils.createImageIcon("images/menus/mapeditor.png");
     // private static final ImageIcon PLAN_EXT_ICON = new ExtendedIcon(ImageUtils.getImage("images/menus/plan.png"));
     // private static final ImageIcon ACOUSTIC_PLAN_ICON = GuiUtils.getLetterIcon('A', Color.BLACK,
     // ColorUtils.setTransparencyToColor(Color.GREEN, 150), 16);
     // private static final ImageIcon PLAN_AC_EXT_ICON = new ExtendedIcon(ImageUtils.getImage("images/menus/plan.png"),
     // ACOUSTIC_PLAN_ICON.getImage());
     // private static final ImageIcon MULTI_PLAN_EXT_ICON = ImageUtils.getScaledIcon("images/buttons/plan_plus.png", 16,
     // 16);
     private static final ImageIcon SETTINGS_ICON = ImageUtils.createImageIcon("images/menus/settings.png");
     private static final ImageIcon CHECKLIST_ICON = ImageUtils.createImageIcon("images/buttons/checklist.png");
     private static final ImageIcon DIR_ICON = ImageUtils.createImageIcon("images/menus/open.png");
     private static final ImageIcon DIR_CLOSED_ICON = ImageUtils.createImageIcon("images/menus/folder_closed.png");
     private static final ImageIcon HOMEREF_ICON = ImageUtils.getScaledIcon("images/buttons/home.png", 16, 16);
     private static final ImageIcon TRANSPONDER_ICON = new ExtendedIcon(ImageUtils.getScaledImage(
             "images/transponder.png", 16, 16));
     private static final ImageIcon START_ICON = ImageUtils.getScaledIcon("images/flag2_green32.png", 16, 16);
 
     private static HashMap<String, ImageIcon> VEHICLES_ICONS = new HashMap<String, ImageIcon>();
     // private static HashMap<String, ImageIcon> PLAN_ICONS = new HashMap<String, ImageIcon>();
 
     // static {
     // for (VehicleType ve : VehiclesHolder.getVehiclesList().values()) {
     // Image vehicleImage;
     // if (!ve.getPresentationImageHref().equalsIgnoreCase(""))
     // vehicleImage = ImageUtils.getImage(ve.getPresentationImageHref());
     // else
     // vehicleImage = ImageUtils.getImage(ve.getSideImageHref());
     // if (vehicleImage == null) {
     // VEHICLES_ICONS.put(ve.getId(), new ImageIcon(vehicleImage));
     // break;
     // }
     // int desiredWidth = 16, desiredHeight = 16;
     //
     // int height = vehicleImage.getHeight(null);
     // int width = vehicleImage.getWidth(null);
     //
     // if (height > width) {
     // desiredWidth = (int) (16.0 * ((double) width / (double) height));
     // }
     // else {
     // desiredHeight = (int) (16.0 * ((double) height / (double) width));
     // }
     //
     // Image sVehicleImage = ImageUtils.getFasterScaledInstance(vehicleImage, desiredWidth, desiredHeight);
     // VEHICLES_ICONS.put(ve.getId(), new ImageIcon(sVehicleImage));
     //
     // PLAN_ICONS.put(ve.getId(), new PlanIcon(sVehicleImage));
     // }
     // }
 
     public boolean debugOn = false;
 
     @Override
     public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
             boolean leaf, int row, boolean hasFocus) {
         
 
         super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
 
         setToolTipText(null); // no tool tip
 
         if (leaf) {
             DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
 
             if ((node.getUserObject() instanceof MapType) || (node.getUserObject() instanceof MapMission)) {
                 setIcon(MAP_ICON);
                 setToolTipText("Edit the map");
             }
 
             else if (node.getUserObject() instanceof PlanType) {
                 State state = State.LOCAL;
                 try {
                     if (node instanceof ExtendedTreeNode) {
                         ExtendedTreeNode ptn = (ExtendedTreeNode) node;
                         State sync = (State) ptn.getUserInfo().get("sync");
                         if (sync != null)
                             state = sync;
                     }
                 }
                 catch (Exception e) {
                     e.printStackTrace();
                 }
 
                 PlanType plan = ((PlanType) node.getUserObject());
                 setPlanIcon(plan.getId(), state, plan.hasMultipleVehiclesAssociated());
                 setToolTipText(plan.toStringWithVehicles());
             }
 
             else if (node.getUserObject() instanceof PlanDBInfo) {
                 PlanDBInfo plan = ((PlanDBInfo) node.getUserObject());
                 setPlanIcon(plan.getPlanId(), State.REMOTE, false);
             }
 
             else if (node.getUserObject() instanceof HomeReference) {
                 setIcon(HOMEREF_ICON);
                 setText(new LocationType(((HomeReference) node.getUserObject())).toString());
                 setToolTipText("View/Edit home reference");
             }
 
             else if (node.getUserObject() instanceof MarkElement) {
                 setIcon(START_ICON);
                 setText(((MarkElement) node.getUserObject()).getPosition().toString());
                 setToolTipText("View/Edit navigation startup position");
             }
 
             else if (node.getUserObject() instanceof VehicleMission) {
                 ImageIcon vehicleIcon = null;
                 try {
                     vehicleIcon = VEHICLES_ICONS.get(((VehicleMission) node.getUserObject()).getVehicle().getId());
                     if (vehicleIcon == null) {
                         Image vehicleImage;
                         if (!((VehicleMission) node.getUserObject()).getVehicle().getPresentationImageHref()
                                 .equalsIgnoreCase(""))
                             vehicleImage = ImageUtils.getImage(((VehicleMission) node.getUserObject()).getVehicle()
                                     .getPresentationImageHref());
                         else
                             vehicleImage = ImageUtils.getImage(((VehicleMission) node.getUserObject()).getVehicle()
                                     .getSideImageHref());
 
                         int desiredWidth = 16, desiredHeight = 16;
 
                         int height = vehicleImage.getHeight(null);
                         int width = vehicleImage.getWidth(null);
 
                         if (height > width) {
                             desiredWidth = (int) (16.0 * ((double) width / (double) height));
                         }
                         else {
                             desiredHeight = (int) (16.0 * ((double) height / (double) width));
                         }
 
                         ImageIcon vIcon = new ImageIcon(vehicleImage.getScaledInstance(desiredWidth, desiredHeight,
                                 Image.SCALE_DEFAULT));
                         setIcon(vIcon);
                         VEHICLES_ICONS.put(((VehicleMission) node.getUserObject()).getVehicle().getId(), vIcon);
                     }
                     else
                         setIcon(vehicleIcon);
                 }
                 catch (Exception e) {
                     NeptusLog.pub().info(((VehicleMission) node.getUserObject()).getId() + " vehicle not found!");
                 }
 
                 setToolTipText("View/Edit the vehicle information");
             }
 
             else if ((node.getUserObject() == "Mission Information") || (node.getUserObject() == "Info")) {
                 // setIcon(new ImageIcon(GuiUtils.getImage("images/menus/settings.png")));
                 setIcon(SETTINGS_ICON);
             }
 
             // else if (node.getUserObject() == "Home Reference") {
             // //setIcon(new ImageIcon(GuiUtils.getImage("images/menus/settings.png")));
             // setIcon(SETTINGS_ICON);
             // }
 
             else if ((node.getUserObject() instanceof ChecklistType)
                     || (node.getUserObject() instanceof ChecklistMission)) {
                 // setIcon(new ImageIcon(GuiUtils.getImage("images/buttons/checklist.png")));
                 setIcon(CHECKLIST_ICON);
                 setToolTipText("Edit the checklist");
             }
 
             else if (node.getUserObject() instanceof TransponderElement) {
                 State state = State.LOCAL;
                 ExtendedTreeNode ptn = (ExtendedTreeNode) node;
                 TransponderElement nodeObj = (TransponderElement) node.getUserObject();
                 HashMap<String, Object> info = ptn.getUserInfo();
                 ImcSystem imcSystem = ImcSystemsHolder.lookupSystemByName((String) info.get("vehicle"));
                 setBeaconLabel(nodeObj, imcSystem);
                 setBeaconIcon(state, ptn);
 
             }
             else if (node.getUserObject() == "Settings") {
                 // setIcon(new ImageIcon(GuiUtils.getImage("images/menus/settings.png")));
                 setIcon(SETTINGS_ICON);
             }
 
             else {
                 // return mapCellRenderer.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                 // setIcon(new ImageIcon(GuiUtils.getImage("images/menus/open.png")));
                 setIcon(expanded ? DIR_ICON : DIR_CLOSED_ICON);
             }
         }
         else {
             // setIcon(new ImageIcon(GuiUtils.getImage("images/menus/open.png")));
             setIcon(expanded ? DIR_ICON : DIR_CLOSED_ICON);
             // return mapCellRenderer.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
         }
 
         return this;
     }
 
     private void setBeaconIcon(State state, ExtendedTreeNode ptn) {
         State sync = (State) ptn.getUserInfo().get("sync");
         if (sync != null)
             state = sync;
         StringBuilder fileName = new StringBuilder(Icons.PATH_SOURCE.getName());
         fileName.append(Icons.BEACONS_PATH.getName());
         fileName.append(Icons.BEACON.getName());
         fileName.append(state.getFileName());
         fileName.append(Icons.EXTENSION.getName());
         setIcon(ImageUtils.getIcon(fileName.toString()));
     }
 
     private void setBeaconLabel(TransponderElement nodeObj, ImcSystem imcSystem) {
         if (imcSystem != null) {
             LBLRangesTimer timer = (LBLRangesTimer) imcSystem.retrieveData(nodeObj.getName());
             if (timer != null) {
                 String color;
                 int time = timer.getTime();
                 if (time == -1) {
                     setText(nodeObj.getName());
                 }
                 else {
                     if (time <= MAX_ACCEPTABLE_ELAPSED_TIME) {
                         color = "green";
                     }
                     else if (time <= LBLRangesTimer.maxTime) {
                         color = "red";
                     }
                     else {
                         color = "black";
                     }
                     int minutes = time / 60;
                     int seconds = time % 60;
                     String formatedTime = (minutes > 0) ? (minutes + "min " + seconds + "s") : (seconds + "s");
                     setText("<html>" + nodeObj.getName() + " (<span color='" + color + "'>&#916;t "
                             + formatedTime + "</span>)");
                 }
             }
         }
     }
 
     private void setPlanIcon(String planId, State state, boolean hasMultpVehicles) {
         StringBuilder fileName = new StringBuilder(Icons.PATH_SOURCE.getName());
         fileName.append(Icons.PLAN_PATH.getName());
         fileName.append(Icons.PLAN.getName());
         fileName.append(state.getFileName());
         if (planId.length() == 1) {
             fileName.append(Icons.ACOUSTIC.getName());
         }
 
         if (hasMultpVehicles) {
             fileName.append(Icons.MULTIPLE_VEHICLES.getName());
         }
 
         fileName.append(Icons.EXTENSION.getName());
         // setIcon(ImageUtils.getIcon(fileName.toString()));
          setIcon(ImageUtils.getScaledIcon(fileName.toString(), 16, 16));
     }
 }
 
 class ExtendedIcon extends ImageIcon {
     private static final long serialVersionUID = 5952273156817637800L;
 
     private static Color BLUE = ColorUtils.setTransparencyToColor(Color.BLUE, 90);
     private static Color GREEN = ColorUtils.setTransparencyToColor(Color.GREEN, 90);
     private static Color RED = ColorUtils.setTransparencyToColor(Color.RED, 90);
 
     // protected static final ImageIcon PLAN_ICON = new ImageIcon(ImageUtils.getImage("images/menus/plan.png"));
 
     protected State state = State.LOCAL;
 
     protected Image overImage = null;
     
     public ExtendedIcon(Image vImage) {
         super(vImage);
     }
 
     public ExtendedIcon(Image vImage, Image topImage) {
         super(vImage);
         this.overImage = topImage;
     }
 
     /**
      * @return the state
      */
     public State getState() {
         return state;
     }
 
     /**
      * @param state the state to set
      */
     public void setState(State state) {
         this.state = state;
     }
 
     @Override
     public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
         // g.drawImage(PLAN_ICON.getImage(), 0, 0, null);
         g.drawImage(getImage(), 0, 0, null);
 
         super.paintIcon(c, g, x, y);
 
         if(overImage != null)
             g.drawImage(overImage, 0, 0, null);
 
         if (state != State.LOCAL) {
             // Graphics2D g2 = (Graphics2D) g.create(0, 0, PLAN_ICON.getImage().getWidth(null),
             // PLAN_ICON.getImage().getHeight(null));
             Graphics2D g2 = (Graphics2D) g.create(0, 0, getImage().getWidth(null), getImage().getHeight(null));
             Color color = (state == State.SYNC ? GREEN : (state == State.REMOTE ? BLUE : RED));
             g2.setColor(color);
             g2.fill(g2.getClipBounds());
             g2.dispose();
         }
     }
 }
