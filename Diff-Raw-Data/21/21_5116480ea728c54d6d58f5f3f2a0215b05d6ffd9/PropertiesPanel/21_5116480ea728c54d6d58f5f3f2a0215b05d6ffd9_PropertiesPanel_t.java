 package de.engineapp.containers;
 
 import java.awt.*;
 import java.awt.event.*;
 import java.util.Iterator;
 
 import javax.swing.*;
 import javax.swing.border.BevelBorder;
 import javax.swing.event.ChangeEvent;
 import javax.swing.event.ChangeListener;
 import javax.swing.event.DocumentEvent;
 import javax.swing.event.DocumentListener;
 
 import de.engine.environment.Scene;
 import de.engine.math.*;
 import de.engine.objects.Ground;
 import de.engine.objects.ObjectProperties;
 import de.engine.objects.Material;
 import de.engineapp.*;
 import de.engineapp.PresentationModel.*;
 import de.engineapp.controls.*;
 import de.engineapp.util.*;
 import de.engineapp.visual.*;
 import de.engineapp.windows.ColorPickerPopup;
 
 import static de.engineapp.Constants.*;
 
 
 /**
  * Panel to manipulate properties of selected objects and the environment.
  * 
  * @author Tim
  */
 public class PropertiesPanel extends VerticalBoxPanel implements SceneListener, ActionListener, ChangeListener, StorageListener, MouseListener, DocumentListener
 {
     private static final long serialVersionUID = 8656904964293251249L;
     
     private final static Localizer LOCALIZER = Localizer.getInstance();
     
     private PresentationModel pModel;
     
     //Label erstellen
     private JLabel nameLabel;
     private JLabel colorLabel;
     private JLabel materialLabel;
     private JLabel xCordinateLabel;
     private JLabel yCordinateLabel;
     private JLabel xSpeedLabel;
     private JLabel ySpeedLabel;
     private JLabel massLabel;
     private JLabel radiusLabel;
     private JLabel angleLabel;
     private JLabel emptyLabel;
     private JLabel gravityLabel;
     private JLabel selObjectsLabel;
     
     private JLabel LabelPotE; //Schriftzug
     private JLabel LabelKinE; //Schriftzug
     
     private JLabel potLabel;  //Werte
     private JLabel kinLabel;  //Werte
     
     //Buttons erstellen
     private JButton del;
     private JButton center;
     private JButton close;
     private QuickButton next;
     private QuickButton previous;
     private JButton delGround;
     
     //Namensfeld erstellen
     private JTextField name;
     private JPanel groupName;
     
     private ColorBox colorBox;
     private ColorPickerPopup colorPicker;
     private JPanel groupColorBox;
     
     //ComboBox und CheckBox erstellen
     private JCheckBox fix;
     private JCheckBox friction;
     private IconComboBox<Material> MaterialCombo;
     
     //Spinner erstellen
     private PropertySpinner massInput;
     private PropertySpinner radiusInput;
     private PropertySpinner xCord;
     private PropertySpinner yCord;
     private PropertySpinner vx;
     private PropertySpinner vy;
     private PropertySpinner angle;
     private PropertySpinner gravity;
     {
         
     }
     
     //Variablen
     private int avoidUpdate;
     
     public PropertiesPanel(PresentationModel model)
     {
         pModel = model;
         
         createControls();
         
          //+++++++++++++++++++++++++++++++++++++++++++++++++//
         //================= Konfiguration =================//
        //+++++++++++++++++++++++++++++++++++++++++++++++++//
         
         //Buttons
         del.addActionListener(this);
         del.setActionCommand(CMD_DELETE);
         center.addActionListener(this);
         center.setActionCommand(CMD_CENTER);
         close.addActionListener(this);
         close.setActionCommand(CMD_CLOSE);
         delGround.addActionListener(this);
         delGround.setActionCommand(CMD_DELETE_GROUND);
         
         //Namensfeld konfigurieren
         name.setEditable(isEnabled());
         name.addFocusListener(new FocusAdapter()
             {
                 @Override
                 public void focusGained(FocusEvent e)
                 {
                     name.selectAll();
                 }
             }
         );
         name.getDocument().addDocumentListener(this);
         
         colorBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
         colorBox.addMouseListener(new MouseAdapter()
         {
             @Override
             public void mouseClicked(MouseEvent e)
             {
                 if (SwingUtilities.isLeftMouseButton(e) && colorBox.isEnabled())
                 {
                     colorPicker.setVisible(true);
                 }
             }
         });
         
         
         this.setBorder( BorderFactory.createBevelBorder( BevelBorder.RAISED ) );
         
         
         pModel.addSceneListener(this);
         pModel.addStorageListener(this);
         pModel.addMouseListenerToCanvas(this);
     }
     
     private void createControls()
     {
         //Labels
         nameLabel       = new JLabel(LOCALIZER.getString(L_NAME_OF_OBJECT));
         colorLabel      = new JLabel(LOCALIZER.getString(L_COLOR_OF_OBJECT));
         materialLabel   = new JLabel(LOCALIZER.getString(L_MATERIAL));
         xCordinateLabel = new JLabel(LOCALIZER.getString(L_X_COORDINATE));
         yCordinateLabel = new JLabel(LOCALIZER.getString(L_Y_COORDINATE));
         xSpeedLabel     = new JLabel(LOCALIZER.getString(L_X_VELOCITY));
         ySpeedLabel     = new JLabel(LOCALIZER.getString(L_Y_VELOCITY));
         massLabel       = new JLabel(LOCALIZER.getString(L_MASS));
         radiusLabel     = new JLabel(LOCALIZER.getString(L_RADIUS));
         angleLabel      = new JLabel(LOCALIZER.getString(L_ANGLE));
         emptyLabel      = new JLabel();
         gravityLabel    = new JLabel(LOCALIZER.getString(L_GRAVITY));
         selObjectsLabel = new JLabel();
         
         LabelPotE       = new JLabel(LOCALIZER.getString(L_POT_ENERGY));
         LabelKinE       = new JLabel(LOCALIZER.getString(L_KIN_ENERGY));
 
         //Buttons
         del             = new JButton(LOCALIZER.getString(L_REMOVE));
         center          = new JButton(LOCALIZER.getString(L_CENTER));
         close           = new JButton("✗");//LOCALIZER.getString(L_CLOSE));
         close.setBorder(BorderFactory.createEmptyBorder());
         close.setFocusPainted(false);
         close.setPreferredSize(new Dimension(15, 15));
         
         delGround       = new JButton(LOCALIZER.getString(L_DELETE_GROUND));
         
         //Namensfeld
         name            = new JTextField();
         groupName       = new JPanel(new BorderLayout(3, 0));
         groupName.setOpaque(true);
         
         groupColorBox   = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
 
         //Combobox + CheckBox
         fix             = new JCheckBox(LOCALIZER.getString(L_PINNED));
         colorBox        = new ColorBox();
         
         friction        = new JCheckBox(LOCALIZER.getString(L_FRICTION));
     }
 
     @Override
     public void objectAdded(ObjectProperties object) {}
     
     @Override
     public void objectRemoved(ObjectProperties object) {}
 
     @Override
     public void groundAdded(Ground ground) 
     {
         showEnvironmentPanel();
     }
 
     @Override
     public void groundRemoved(Ground ground) 
     {
         
     }
 
 
     @Override
     public void objectSelected(ObjectProperties object)
     {
         if (!pModel.isState(STG_DBLCLICK_SHOW_PROPERTIES) || pModel.hasMultiSelectionObjects())
         {
             showPropertyPanel(object);
         }
     }
     
     
     private void showPropertyPanel(ObjectProperties object)
     {
         this.removeAll();
         //Instanzieren
         potLabel     = new JLabel(this.formatDoubleValue(object.getPotEnergy()));
         kinLabel     = new JLabel(this.formatDoubleValue(object.getKinEnergy()));
         
         massInput    = new PropertySpinner(object.getMass(),1,10000,10,this);
         radiusInput  = new PropertySpinner(object.getRadius(), 1, 1000000, 10, this);
         xCord        = new PropertySpinner(object.getPosition().getX(),-100000.0,100000,10,this);
         yCord        = new PropertySpinner(object.getPosition().getY(),-100000,100000,10,this);
         vx           = new PropertySpinner(object.velocity.getX(),-10000,10000,10,this);
         vy           = new PropertySpinner(object.velocity.getY(),-10000,10000,10,this);
         angle        = new PropertySpinner(Math.toDegrees(object.getRotationAngle()), 0, 359, 1, this, true);
         
         MaterialCombo = new IconComboBox<Material>(Material.values(), "materials");
         
         next         = new QuickButton(GuiUtil.getIcon(ICO_NEXT), CMD_NEXT, this);
         previous     = new QuickButton(GuiUtil.getIcon(ICO_PREVIOUS), CMD_PREVIOUS, this);
         groupName.add(previous, BorderLayout.LINE_START);
         groupName.add(next, BorderLayout.LINE_END);
         groupName.add(name);
         
         if (pModel.getMultipleSelectionObjects().size() == 2)
         {
             selObjectsLabel.setText(LOCALIZER.getString(L_SELECTED_OBJECT));
         }
         else if (pModel.hasMultiSelectionObjects())
         {
            selObjectsLabel.setText(String.format(LOCALIZER.getString(L_SELECTED_OBJECTS), 
                    pModel.getMultipleSelectionObjects().size() - 1));
         }
         
         colorPicker = new ColorPickerPopup(colorBox);
         
         //ToolTips hinzufügen
         setToolTips();
         
         //Konfigurieren
         name.setText(((ISelectable)pModel.getSelectedObject()).getName());
         
         MaterialCombo.setSelectedItem(pModel.getSelectedObject().surface);
         MaterialCombo.addActionListener(this);
         
         fix.addActionListener(this);
         fix.setSelected(pModel.getSelectedObject().isPinned);
         
         colorBox.setForeground(((IDrawable) object).getColor());
         colorBox.addChangeListener(this);
         colorBox.setPreferredSize(new Dimension(20, 20));
         
         groupColorBox.add(colorLabel);
         groupColorBox.add(colorBox);
         
         //Ausgrauen, Prüfungen in Methode enthalten
         updateControls();
         
         //Hinzufügen
         this.add(close, RIGHT_ALIGNMENT);
         this.addGap(6);
         this.add(nameLabel, CENTER_ALIGNMENT);
         this.addGap(5);
         this.addGroup(3, groupName);
         this.add(selObjectsLabel, RIGHT_ALIGNMENT);
         this.addGap(8);
 //        this.add(groupColorBox);
 //        this.addGroup(5, colorLabel, colorBox);
         this.addGroup(0, groupColorBox);
         this.addGap(10);
 //        this.add(del, RIGHT_ALIGNMENT);
         this.addGroup(35, center, del);
         this.addGap(10);
         this.addSeparator();
         this.addGap(15);
         this.addGroup(5, materialLabel);
         this.addGap(5);
         this.addGroup(5,MaterialCombo);
         this.addGap(10);
         this.addGroup(5, xCordinateLabel, yCordinateLabel);
         this.addGroup(5, xCord, yCord);
         this.addGap(8);
         this.addGroup(5, xSpeedLabel,ySpeedLabel);
         this.addGroup(5, vx, vy);
         this.addGap(8);
         this.addGroup(5,massLabel, radiusLabel);
         this.addGroup(5, massInput, radiusInput);
         this.addGap(8);
         this.add(angleLabel);
         this.addGroup(5, angle, emptyLabel);
 //        this.addGroup(5, angle, center);
         this.addGap(20);
         this.add(fix, LEFT_ALIGNMENT);
         this.addGap(20);
         this.addGroup(5,LabelPotE, potLabel);
         this.addGap(5);
         this.addGroup(5,LabelKinE, kinLabel);
         this.addGap(25);
 //        this.add(close, LEFT_ALIGNMENT);
         
         this.updateUI();
         this.setVisible(true);
     }
     
     public void setToolTips()
     {
         name.setToolTipText(         LOCALIZER.getString(TT_OBJECT_NAME));
         next.setToolTipText(         LOCALIZER.getString(TT_NEXT));
         previous.setToolTipText(     LOCALIZER.getString(TT_PREVIOUS));
         colorBox.setToolTipText(     LOCALIZER.getString(TT_COLOR));
         del.setToolTipText(          LOCALIZER.getString(TT_DEL));
         MaterialCombo.setToolTipText(LOCALIZER.getString(TT_MATERIAL));
         xCord.setToolTipText(        LOCALIZER.getString(TT_X_COORDINATE));
         yCord.setToolTipText(        LOCALIZER.getString(TT_Y_COORDINATE));
         vx.setToolTipText(           LOCALIZER.getString(TT_X_SPEED));
         vy.setToolTipText(           LOCALIZER.getString(TT_Y_SPEED));
         massInput.setToolTipText(    LOCALIZER.getString(TT_MASS));
         radiusInput.setToolTipText(  LOCALIZER.getString(TT_RADIUS));
         angle.setToolTipText(        LOCALIZER.getString(TT_ANGLE));
         fix.setToolTipText(          LOCALIZER.getString(TT_FIX));
         potLabel.setToolTipText(     LOCALIZER.getString(TT_POTE));
         LabelPotE.setToolTipText(    LOCALIZER.getString(TT_POTE));
         kinLabel.setToolTipText(     LOCALIZER.getString(TT_KINE));
         LabelKinE.setToolTipText(    LOCALIZER.getString(TT_KINE));
         close.setToolTipText(        LOCALIZER.getString(TT_CLOSE));
         center.setToolTipText(       LOCALIZER.getString(TT_JUMP_TO));
     }
     
     public void showEnvironmentPanel()
     {
         this.removeAll();
         
         this.add(close, RIGHT_ALIGNMENT);
         this.addGap(6);
         gravity = new PropertySpinner(-pModel.getScene().gravitational_acceleration, -100, 100, 0.2, this);
         
         friction.setSelected(pModel.getScene().enable_env_friction);
         
         delGround.addActionListener(this);
         colorBox.setPreferredSize(new Dimension(20, 20));
         colorBox.addChangeListener(this);
         colorPicker = new ColorPickerPopup(colorBox);
 
         if (pModel.getScene().existGround())
         {
             delGround.setEnabled(true);
             colorBox.setEnabled(true);
             colorBox.setForeground(((IDrawable) pModel.getScene().getGround()).getColor());
         }else 
         {
             delGround.setEnabled(false);
             colorBox.setEnabled(false);
         }
         
         
         this.add(gravityLabel, CENTER_ALIGNMENT);
         this.add(gravity, CENTER_ALIGNMENT);
         this.addGap(10);
         this.add(friction);
         this.addGap(10);
         this.add(delGround);
         this.addGap(5);
         this.add(colorBox);
 
         this.updateUI();
         this.setVisible(true);
     }
     
     
     @Override
     public void objectDeselected(ObjectProperties object)
     {
         this.setVisible(false);
     }
     
     
     @Override
     public void actionPerformed(ActionEvent e)
     {
         switch(e.getActionCommand())
         {
             case CMD_DELETE:
                 for (ObjectProperties object : pModel.getMultipleSelectionObjects())
                 {
                     pModel.removeObject(object);
                 }
                 pModel.fireRepaint();
                 break;
                 
             case CMD_CENTER:
                 pModel.navigateTo(pModel.getSelectedObject().getPosition());
                 pModel.fireRepaint();
                 break;
                 
             case CMD_CLOSE:
                 if (this.isAncestorOf(gravity))
                 {
                     this.setVisible(false);
                 }
                 else
                 {
                     pModel.setSelectedObject(null);
                     pModel.fireRepaint();
                 }
                 break;
                 
             case CMD_NEXT:
                 Iterator<ObjectProperties> it = pModel.getScene().getObjects().iterator();
                 while(it.next() != pModel.getSelectedObject());
                 if(it.hasNext())
                 {
                     pModel.setSelectedObject(it.next());
                 }
                 else
                 {
                     pModel.setSelectedObject(pModel.getScene().getObject(0));
                 }
                 showPropertyPanel(pModel.getSelectedObject());
                 pModel.fireRepaint();
                 break;
                 
             case CMD_PREVIOUS:
                 if(pModel.getSelectedObject() == pModel.getScene().getObject(0))
                 {
                     pModel.setSelectedObject(pModel.getScene().getObject(pModel.getScene().getCount()-1));
                 }
                 else
                 {
                     for(int i = pModel.getScene().getCount()-1; i >= 0; i--)
                     {
                         if(pModel.getSelectedObject() == pModel.getScene().getObject(i)) 
                         {
                             pModel.setSelectedObject(pModel.getScene().getObject(i-1));
                             break;
                         }
                     }
                 }
                 showPropertyPanel(pModel.getSelectedObject());
                 pModel.fireRepaint();
                 break;
                 
             case CMD_DELETE_GROUND:
                 pModel.getScene().removeGround();
                 pModel.fireRepaint();
                 showEnvironmentPanel();
                 break;
         }
 
         if(e.getSource() == MaterialCombo)
         {
             for (ObjectProperties object : pModel.getMultipleSelectionObjects())
             {
                 object.surface = MaterialCombo.getSelectedItem();
             }
         }
         if(e.getSource() == fix)
         {
             for (ObjectProperties object : pModel.getMultipleSelectionObjects())
             {
                 object.isPinned = fix.isSelected();
             }
         }
         
         
     }
 
     //Werte der Controls auf die Eigenschaften des Objekts übertragen
     @Override
     public void stateChanged(ChangeEvent e)
     {
         if(this.isAncestorOf(name) && avoidUpdate != 1)
         {
             if (!pModel.hasMultiSelectionObjects())
             {
                 ((ISelectable) pModel.getSelectedObject()).setName(name.getText());
             }
             
             double deltaX = xCord.getValue() - pModel.getSelectedObject().getX();
             double deltaY = yCord.getValue() - pModel.getSelectedObject().getY();
             double deltaVeloX = vx.getValue() - pModel.getSelectedObject().velocity.getX();
             double deltaVeloY = vy.getValue() - pModel.getSelectedObject().velocity.getY();
             
             Color newColor = colorBox.getForeground();
             double newMass = massInput.getValue();
             double newRadius = radiusInput.getValue();
             Material newSurface = MaterialCombo.getSelectedItem();
             double newAngle = Math.toRadians(angle.getValue());
             boolean isPinned = fix.isSelected();
             
             for (ObjectProperties object : pModel.getMultipleSelectionObjects())
             {
                 
                 ((IDrawable) object).setColor(newColor);
                 
                 object.world_position.translation = Util.add(object.getPosition(), new Vector(deltaX, deltaY));
                 object.velocity = Util.add(object.velocity, new Vector(deltaVeloX, deltaVeloY));
                 object.setMass(newMass);
                 object.setRadius(newRadius);
                 object.surface = newSurface;
                 object.setRotationAngle(newAngle);
                 object.isPinned = isPinned;
             }
         }
         
         if(this.isAncestorOf(gravity) && pModel.getScene().existGround())
         {
             pModel.getScene().gravitational_acceleration = -gravity.getValue();
             pModel.getScene().enable_env_friction = friction.isSelected();
             ((IDrawable) pModel.getScene().getGround()).setColor(colorBox.getForeground());
         }
         pModel.fireRepaint();
     }
     
     @Override
     public void objectUpdated(ObjectProperties object)
     {
         if(this.isVisible())
         {
             if(this.isAncestorOf(gravity))
             {
                 gravity.setValue(-pModel.getScene().gravitational_acceleration);
                 friction.setSelected(pModel.getScene().enable_env_friction);
             }
             else
             {
             avoidUpdate = 1;    //vermeidet ungewollten Aufruf des ChangeListeners (Endlosschleife)
             
             xCord.setValue(pModel.getSelectedObject().world_position.translation.getX());
             yCord.setValue(pModel.getSelectedObject().world_position.translation.getY());
             vx.setValue(pModel.getSelectedObject().velocity.getX());
             vy.setValue(pModel.getSelectedObject().velocity.getY());
             massInput.setValue(pModel.getSelectedObject().getMass());
             radiusInput.setValue(pModel.getSelectedObject().getRadius());
             MaterialCombo.setSelectedItem(pModel.getSelectedObject().surface);
             fix.setSelected(pModel.getSelectedObject().isPinned);
             angle.setValue(Math.toDegrees(pModel.getSelectedObject().getRotationAngle()));
             potLabel.setText(this.formatDoubleValue(object.getPotEnergy()));
             kinLabel.setText(this.formatDoubleValue(object.getKinEnergy()));
             name.setText(((ISelectable)pModel.getSelectedObject()).getName());
             
             avoidUpdate = 0;
             }
         }
     }
     
     
     //Werte der Controls zur Laufzeit der Szene anpassen
     @Override
     public void sceneUpdated(Scene scene)
     {
         if(this.isVisible())
         {
             if(this.isAncestorOf(gravity))
             {
                 gravity.setValue(-pModel.getScene().gravitational_acceleration);
                 friction.setSelected(pModel.getScene().enable_env_friction);
             }
             else
             {
             avoidUpdate = 1;    //vermeidet ungewollten Aufruf des ChangeListeners (Endlosschleife)
             massInput.setValue(pModel.getSelectedObject().getMass());
             radiusInput.setValue(pModel.getSelectedObject().getRadius());
             xCord.setValue(pModel.getSelectedObject().getPosition().getX()); 
             yCord.setValue(pModel.getSelectedObject().getPosition().getY());
             vx.setValue(pModel.getSelectedObject().velocity.getX());
             vy.setValue(pModel.getSelectedObject().velocity.getY());
             fix.setSelected(pModel.getSelectedObject().isPinned);
             angle.setValue(Math.toDegrees(pModel.getSelectedObject().getRotationAngle()));
             
             name.setText(((ISelectable)pModel.getSelectedObject()).getName());
             
             potLabel.setText(this.formatDoubleValue(pModel.getSelectedObject().getPotEnergy()));
             kinLabel.setText(this.formatDoubleValue(pModel.getSelectedObject().getKinEnergy()));
             
             avoidUpdate = 0;
             }
         }
     }
 
     //Formatvorlage für die Labels Epot & Ekin
     private String formatDoubleValue(double d)
     {
         String s;
         if(Math.abs(d) > 1000)
         {
             s = " kJ";
             d = d/1000;
             
             if (Math.abs(d) > 1000)
             {
                 s = " MJ";
                 d = d/1000;
                 
                 if (Math.abs(d) > 1000)
                 {
                     s = " GJ";
                     d = d/1000;
                 }
             }
         }
         else
         {
             s = " J";
         }
         s = String.format("%.2f", d) + s;
         return s;
     }
     
     @Override
     public void stateChanged(String id, boolean value) { }
     
     @Override
     public void propertyChanged(String id, String value)
     {
         if(id.equals(PRP_MODE))
         {
             if(this.isVisible())
                 updateControls();
         }
         
         if (id.equals(PRP_LANGUAGE_CODE))
         {
             createControls();
             
             if (this.isVisible())
             {
                 this.setVisible(false);
 //                this.removeAll();
                 showPropertyPanel(pModel.getSelectedObject());
             }
         }
     }
     
     //Ausgrauen im Wiedergabemodus, sonst aktivieren
     public void updateControls()
     {
         if(pModel.getProperty(PRP_MODE).equals(CMD_PLAYBACK_MODE))
         {
             massInput.setEnabled(false);
             radiusInput.setEnabled(false);
             xCord.setEnabled(false); 
             yCord.setEnabled(false);
             vx.setEnabled(false);
             vy.setEnabled(false);
             fix.setEnabled(false);
             angle.setEnabled(false);
             name.setEnabled(false);
             
             MaterialCombo.setEnabled(false);
             colorBox.setEnabled(false);
             
             del.setEnabled(false);
         }
         else
         {
             massInput.setEnabled(true);
             radiusInput.setEnabled(true);
             xCord.setEnabled(true); 
             yCord.setEnabled(true);
             vx.setEnabled(true);
             vy.setEnabled(true);
             fix.setEnabled(true);
             angle.setEnabled(true);
             if (!pModel.hasMultiSelectionObjects())
             {
                 name.setEnabled(true);
             }
             else
             {
                 name.setEnabled(false);
             }
             
             MaterialCombo.setEnabled(true);
             colorBox.setEnabled(true);
             
             del.setEnabled(true);
         }
         if (pModel.hasMultiSelectionObjects())
         {
             selObjectsLabel.setVisible(true);
         }
         else
         {
             selObjectsLabel.setVisible(false);
         }
     }
     
     @Override
     public void mouseClicked(MouseEvent e)
     {
         if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2 && 
                 pModel.isState(STG_DBLCLICK_SHOW_PROPERTIES) && pModel.hasSelectedObject())
         {
             showPropertyPanel(pModel.getSelectedObject());
         }
         if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2 && !pModel.hasSelectedObject())
         {
             showEnvironmentPanel();
         }
         if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() < 2 && this.isVisible() && !pModel.hasSelectedObject())
         {
             this.setVisible(false);
         }
     }
     
     @Override
     public void mouseEntered(MouseEvent e) { }
     
     @Override
     public void mouseExited(MouseEvent e) { }
     
     @Override
     public void mousePressed(MouseEvent e) { }
     
     @Override
     public void mouseReleased(MouseEvent e) { }
     
     
     @Override
    public void multipleObjectsSelected(ObjectProperties object)
    {
        if (!pModel.isState(STG_DBLCLICK_SHOW_PROPERTIES))
        {
            if (pModel.getMultipleSelectionObjects().size() == 2)
            {
                selObjectsLabel.setText(LOCALIZER.getString(L_SELECTED_OBJECT));
            }
            else if (pModel.hasMultiSelectionObjects())
            {
                selObjectsLabel.setText(String.format(LOCALIZER.getString(L_SELECTED_OBJECTS), 
                        pModel.getMultipleSelectionObjects().size() - 1));
            }
            selObjectsLabel.setVisible(true);
        }
    }
     
     @Override
     public void multipleObjectsDeselected(ObjectProperties object) { }
 
     @Override
     public void changedUpdate(DocumentEvent e)
     {
         ((ISelectable) pModel.getSelectedObject()).setName(name.getText());
     }
 
     @Override
     public void insertUpdate(DocumentEvent e)
     {
         ((ISelectable) pModel.getSelectedObject()).setName(name.getText());
     }
 
     @Override
     public void removeUpdate(DocumentEvent e)
     {
         ((ISelectable) pModel.getSelectedObject()).setName(name.getText());
     }
 }
