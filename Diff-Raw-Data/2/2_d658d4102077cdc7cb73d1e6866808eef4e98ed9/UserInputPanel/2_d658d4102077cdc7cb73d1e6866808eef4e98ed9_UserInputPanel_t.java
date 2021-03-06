 /*
  * $Id$
  * Copyright (C) 2002 Elmar Grom
  *
  * File :               UserInputPanel.java
  * Description :        A panel to collect input form the end user.
  * Author's email :     elmar@grom.net
  * Author's Website :   http://www.izforge.com
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU General Public License
  * as published by the Free Software Foundation; either version 2
  * of the License, or any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
  */
 
 package   com.izforge.izpack.panels;
 
 import java.awt.Color;
 import java.awt.Font;
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.io.File;
 import java.io.InputStream;
 import java.text.MessageFormat;
 import java.util.HashMap;
 import java.util.Map;
 import java.util.Vector;
 
 import javax.swing.BorderFactory;
 import javax.swing.ButtonGroup;
 import javax.swing.JButton;
 import javax.swing.JCheckBox;
 import javax.swing.JComboBox;
 import javax.swing.JComponent;
 import javax.swing.JFileChooser;
 import javax.swing.JLabel;
 import javax.swing.JOptionPane;
 import javax.swing.JPanel;
 import javax.swing.JPasswordField;
 import javax.swing.JRadioButton;
 import javax.swing.JTextField;
 
 import net.n3.nanoxml.NonValidator;
 import net.n3.nanoxml.StdXMLBuilder;
 import net.n3.nanoxml.StdXMLParser;
 import net.n3.nanoxml.StdXMLReader;
 import net.n3.nanoxml.XMLElement;
 
 import com.izforge.izpack.LocaleDatabase;
 import com.izforge.izpack.Pack;
 import com.izforge.izpack.gui.ButtonFactory;
 import com.izforge.izpack.gui.TwoColumnConstraints;
 import com.izforge.izpack.gui.TwoColumnLayout;
 import com.izforge.izpack.installer.InstallData;
 import com.izforge.izpack.installer.InstallerFrame;
 import com.izforge.izpack.installer.IzPanel;
 import com.izforge.izpack.installer.ResourceManager;
 import com.izforge.izpack.util.MultiLineLabel;
 import com.izforge.izpack.util.OsConstraint;
 
 /*---------------------------------------------------------------------------*/
 /**
  * This panel is designed to collect user input during the installation
  * process. The panel is initially blank and is populated with input elements
  * based on the XML specification in a resource file.
  *
  *
  * @version  0.0.1 / 10/19/02
  * @author   getDirectoryCreated
  */
 /*---------------------------------------------------------------------------*/
 /*$
  * @design
  *
  * Each field is specified in its own node, containing attributes and data.
  * When this class is instantiated, the specification is read and analyzed.
  * Each field node is processed based on its type. An specialized member
  * function is called for each field type that creates the necessary UI
  * elements. All UI elements are stored in the uiElements vector. Elements
  * are packaged in an object array that must follow this pattern:
  *
  * index 0 - a String object, that specifies the field type. This is
  *           identical to the string used to identify the field type
  *           in the XML file.
  * index 1 - a String object that contains the variable name for substitution.
  * index 2 - the constraints object that should be used for positioning the
  *           UI element
  * index 3 - the UI element itself
  * index 4 - a Vector containg a list of pack for which the item should be
  *           created. This is used by buildUI() to decide if the item should
  *           be added to the UI.
  *
  * In some cases additional entries are used. The use depends on the specific
  * needs of the type of input field.
  *
  * When the panel is activated, the method buildUI() walks the list of UI
  * elements adds them to the panel together with the matching constraint.
  *
  * When an attempt is made to move on to another panel, the method readInput()
  * walks the list of UI elements again and calls specialized methods that
  * know how to read the user input from each of the UI elemnts and set the
  * associated varaible.
  *
  * The actual variable substitution is not performed by this panel but by
  * the variable substitutor.
  *
  * To Do:
  * ------
  * * make sure all header documentation is complete and correct
  *--------------------------------------------------------------------------*/
 public class UserInputPanel extends IzPanel
 {
   // ------------------------------------------------------------------------
   // Constant Definitions
   // ------------------------------------------------------------------------
 
   // The constants beginning with 'POS_' define locations in the object arrays
   // that used to hold all information for the individual fields. Some data is
   // not required for all field types. If this happens withing the array, that
   // location must be padded with 'null'. At the end of the array it can be
   // omitted. The data stored in this way is in most cases only known by
   // convention between the add and the associated read method. the following
   // positions are also used by other service methods in this class and must
   // not be used for other purposes:
   // - POS_DISPLAYED
   // - POS_TYPE
   // - POS_CONSTRAINTS
   // - POS_PACKS
 
   private static final int    POS_DISPLAYED                 = 0;
   private static final int    POS_TYPE                      = 1;
   private static final int    POS_VARIABLE                  = 2;
   private static final int    POS_CONSTRAINTS               = 3;
   private static final int    POS_FIELD                     = 4;
   private static final int    POS_PACKS                     = 5;
   private static final int    POS_TRUE                      = 6;
   private static final int    POS_FALSE                     = 7;
   private static final int    POS_MESSAGE                   = 8;
   private static final int    POS_GROUP                     = 9;
 
   /** The name of the XML file that specifies the panel layout */
   private static final String SPEC_FILE_NAME                = "userInputSpec.xml";
 
   /** how the spec node for a specific panel is identified */
   private static final String NODE_ID                       = "panel";
   private static final String FIELD_NODE_ID                 = "field";
   private static final String INSTANCE_IDENTIFIER           = "order";
   private static final String TYPE                          = "type";
   private static final String DESCRIPTION                   = "description";
   private static final String VARIABLE                      = "variable";
   private static final String TEXT                          = "txt";
   private static final String KEY                           = "id";
   private static final String SPEC                          = "spec";
   private static final String SET                           = "set";
   private static final String TRUE                          = "true";
   private static final String FALSE                         = "false";
   private static final String ALIGNMENT                     = "align";
   private static final String LEFT                          = "left";
   private static final String CENTER                        = "center";
   private static final String RIGHT                         = "right";
   private static final String TOP                           = "top";
   private static final String BOTTOM                        = "bottom";
   private static final String ITALICS                       = "italic";
   private static final String BOLD                          = "bold";
   private static final String SIZE                          = "size";
   private static final String VALIDATOR                     = "validator";
   private static final String PROCESSOR                     = "processor";
   private static final String CLASS                         = "class";
 
   private static final String FIELD_LABEL                   = "label";
 
   private static final String TITLE_FIELD                   = "title";
 
   private static final String TEXT_FIELD                    = "text";
   private static final String TEXT_SIZE                     = "size";
   private static final String STATIC_TEXT                   = "staticText";
 
   private static final String COMBO_FIELD                   = "combo";
   private static final String COMBO_CHOICE                  = "choice";
   private static final String COMBO_VALUE                   = "value";
 
   private static final String RADIO_FIELD                   = "radio";
   private static final String RADIO_CHOICE                  = "choice";
   private static final String RADIO_VALUE                   = "value";
 
   private static final String SPACE_FIELD                   = "space";
   private static final String DIVIDER_FIELD                 = "divider";
   private static final String CHECK_FIELD                   = "check";
 
   private static final String RULE_FIELD                    = "rule";
   private static final String RULE_LAYOUT                   = "layout";
   private static final String RULE_SEPARATOR                = "separator";
   private static final String RULE_RESULT_FORMAT            = "resultFormat";
   private static final String RULE_PLAIN_STRING             = "plainString";
   private static final String RULE_DISPLAY_FORMAT           = "displayFormat";
   private static final String RULE_SPECIAL_SEPARATOR        = "specialSeparator";
   private static final String RULE_ENCRYPTED                = "processed";
 
   private static final String PWD_FIELD                     = "password";
   private static final String PWD_INPUT                     = "pwd";
   private static final String PWD_SIZE                      = "size";
 
   private static final String SEARCH_FIELD                  = "search";
   // internal value for the button used to trigger autodetection
   private static final String SEARCH_BUTTON_FIELD           = "autodetect";
   private static final String SEARCH_CHOICE                 = "choice";
   private static final String SEARCH_FILENAME               = "filename";
   private static final String SEARCH_RESULT                 = "result";
   private static final String SEARCH_VALUE                  = "value";
   private static final String SEARCH_TYPE                   = "type";
   private static final String SEARCH_FILE                   = "file";
   private static final String SEARCH_DIRECTORY              = "directory";
   private static final String SEARCH_PARENTDIR              = "parentdir";
   private static final String SEARCH_CHECKFILENAME          = "checkfilename";
 
   private static final String PACKS                         = "createForPack";
   private static final String NAME                          = "name";
 
 
   // ------------------------------------------------------------------------
   // Variable Declarations
   // ------------------------------------------------------------------------
   private static int          instanceCount   = 0;
   private        int          instanceNumber  = 0;
   private        boolean      uiBuilt         = false;
 
   /** If there is a possibility that some UI elements will not get added we
       can not allow to go back to the PacksPanel, because the process of
       building the UI is not reversable. This variable keeps track if any
       packs have been defined and will be used to make a decision for
       locking the 'previous' button. */
   private         boolean     packsDefined    = false;
 
   private InstallerFrame      parent;
   /** The parsed result from reading the XML specification from the file */
   private XMLElement          spec;
   private boolean             haveSpec = false;
 
   /** Holds the references to all of the UI elements */
   private Vector              uiElements      = new Vector ();
   /** Holds the references to all radio button groups */
   private Vector              buttonGroups    = new Vector ();
   /** Holds the references to all password field groups */
   private Vector              passwordGroups  = new Vector ();
   /** used for temporary storage of references to password groups that
       have already been read in a given read cycle. */
   private Vector              passwordGroupsRead  = new Vector ();
   /** Used to track search fields. Contains SearchField references. */
   private Vector              searchFields = new Vector();
 
   /** Holds all user inputs for use in automated installation */
   private Vector              entries         = new Vector ();
 
   private TwoColumnLayout     layout;
   private LocaleDatabase      langpack        = null;
 
 
  /*--------------------------------------------------------------------------*/
  // This method can be used to search for layout problems. If this class is
  // compiled with this method uncommented, the layout guides will be shown
  // on the panel, making it possible to see if all components are placed
  // correctly.
  /*--------------------------------------------------------------------------*/
 //  public void paint (Graphics graphics)
 //  {
 //    super.paint (graphics);
 //    layout.showRules ((Graphics2D)graphics, Color.red);
 //  }
  /*--------------------------------------------------------------------------*/
  /**
   * Constructs a <code>UserInputPanel</code>.
   *
   * @param     parent       reference to the application frame
   * @param     installData  shared information about the installation
   */
  /*--------------------------------------------------------------------------*/
   public UserInputPanel (InstallerFrame parent,
                          InstallData    installData)
 
   {
     super (parent, installData);
 
     instanceNumber = instanceCount++;
     this.parent = parent;
 
     // ----------------------------------------------------
     // ----------------------------------------------------
     layout = new TwoColumnLayout (10, 5, 30, 25, TwoColumnLayout.LEFT);
     setLayout (layout);
 
     // ----------------------------------------------------
     // get a locale database
     // ----------------------------------------------------
     try
     {
       this.langpack = new LocaleDatabase (ResourceManager.getInstance().getInputStream (SPEC_FILE_NAME));
     }
     catch (Throwable exception)
     {}
 
     // ----------------------------------------------------
     // read the specifications
     // ----------------------------------------------------
     try
     {
       readSpec ();
     }
     catch (Throwable exception)
     {
       // log the problem
       exception.printStackTrace ();
     }
 
     if (!haveSpec)
     {
       // return if we could not read the spec. further
       // processing will only lead to problems. In this
       // case we must skip the panel when it gets activated.
       return;
     }
 
     // ----------------------------------------------------
     // process all field nodes. Each field node is analyzed
     // for its type, then an appropriate memeber function
     // is called that will create the correct UI elements.
     // ----------------------------------------------------
     Vector fields = spec.getChildrenNamed (FIELD_NODE_ID);
 
     for (int i = 0; i < fields.size (); i++)
     {
       XMLElement  field     = (XMLElement)fields.elementAt (i);
       String      attribute = field.getAttribute (TYPE);
 
       if (attribute != null)
       {
         if (attribute.equals (RULE_FIELD))
         {
           addRuleField (field);
         }
         else if (attribute.equals (TEXT_FIELD))
         {
           addTextField (field);
         }
         else if (attribute.equals (COMBO_FIELD))
         {
           addComboBox (field);
         }
         else if (attribute.equals (RADIO_FIELD))
         {
           addRadioButton (field);
         }
         else if (attribute.equals (PWD_FIELD))
         {
           addPasswordField (field);
         }
         else if (attribute.equals (SPACE_FIELD))
         {
           addSpace (field);
         }
         else if (attribute.equals (DIVIDER_FIELD))
         {
           addDivider (field);
         }
         else if (attribute.equals (CHECK_FIELD))
         {
           addCheckBox (field);
         }
         else if (attribute.equals (STATIC_TEXT))
         {
           addText (field);
         }
         else if (attribute.equals (TITLE_FIELD))
         {
           addTitle (field);
         }
         else if (attribute.equals (SEARCH_FIELD))
         {
           addSearch (field);
         }
       }
     }
   }
 
  /*--------------------------------------------------------------------------*/
  /**
   * Indicates wether the panel has been validated or not. The installer won't
   * let the user go further through the installation process until the panel
   * is validated. Default behavior is to return true.
   *
   * @return    A boolean stating wether the panel has been validated or not.
   */
  /*--------------------------------------------------------------------------*/
   public boolean isValidated ()
   {
     return (readInput ());
   }
  /*--------------------------------------------------------------------------*/
  /**
   * This method is called when the panel becomes active.
   */
  /*--------------------------------------------------------------------------*/
   public void panelActivate ()
   {
     if (spec == null)
     {
       // TODO: translate
       emitError("User input specification could not be found.", 
           "The specification for the user input panel could not be found. Please contact the packager.");
       parent.skipPanel();
     }
     
     Vector forPacks = spec.getChildrenNamed (PACKS);
     if (!itemRequiredFor (forPacks))
     {
       parent.skipPanel ();
       return;
     }
     if (!haveSpec)
     {
       parent.skipPanel ();
       return;
     }
     if (uiBuilt)
     {
       return;
     }
 
     buildUI ();
     uiBuilt = true;
 
     if (packsDefined)
     {
       parent.lockPrevButton ();
     }
   }
  /*--------------------------------------------------------------------------*/
  /**
   * Asks the panel to set its own XML data that can be brought back for an
   * automated installation process. Use it as a blackbox if your panel needs
   * to do something even in automated mode.
   *
   * @param     panelRoot    The XML root element of the panels blackbox tree.
   */
  /*--------------------------------------------------------------------------*/
   public void makeXMLData (XMLElement panelRoot)
   {
 		Map entryMap = new HashMap();
 
 		for (int i = 0; i < entries.size (); i++)
 		{
 			TextValuePair pair = (TextValuePair)entries.elementAt(i);
 			entryMap.put( pair.toString(), pair.getValue() );
 		}
 
 		new UserInputPanelAutomationHelper(entryMap).makeXMLData(idata, panelRoot);
   }
 
  /*--------------------------------------------------------------------------*/
  /**
   * Builds the UI and makes it ready for display
   */
  /*--------------------------------------------------------------------------*/
   private void buildUI ()
   {
     Object [] uiElement;
 
     for (int i = 0; i < uiElements.size (); i++)
     {
       uiElement = (Object [])uiElements.elementAt (i);
 
       if (itemRequiredFor ((Vector)uiElement [POS_PACKS]))
       {
         try
         {
           add ((JComponent)uiElement [POS_FIELD], uiElement [POS_CONSTRAINTS]);
           uiElement [POS_DISPLAYED] = new Boolean (true);
         }
         catch (Throwable exception)
         {
           System.out.println ("Internal format error in field: " + uiElement [POS_TYPE].toString ());  // !!! logging
         }
       }
       else
       {
         uiElement [POS_DISPLAYED] = new Boolean (false);
       }
     }
   }
  /*--------------------------------------------------------------------------*/
  /**
   * Reads the input data from all UI elements and sets the associated variables.
   *
   * @return    <code>true</code> if the operation is successdul, otherwise
   *            <code>false</code>.
   */
  /*--------------------------------------------------------------------------*/
   private boolean readInput ()
   {
     boolean     success;
     String      fieldType = null;
     Object []   field     = null;
 
     passwordGroupsRead.clear ();
     // ----------------------------------------------------
     // cycle through all but the password fields and read
     // their contents
     // ----------------------------------------------------
     for (int i = 0; i < uiElements.size (); i++)
     {
       field     = (Object [])uiElements.elementAt (i);
 
       if ((field != null) && (((Boolean)field [POS_DISPLAYED]).booleanValue ()))
       {
         fieldType = (String)(field [POS_TYPE]);
 
         // ------------------------------------------------
         if (fieldType.equals (RULE_FIELD))
         {
           success = readRuleField (field);
           if (!success)
           {
             return (false);
           }
         }
 
         // ------------------------------------------------
         if (fieldType.equals (PWD_FIELD))
         {
           success = readPasswordField (field);
           if (!success)
           {
             return (false);
           }
         }
 
         // ------------------------------------------------
         else if (fieldType.equals (TEXT_FIELD))
         {
           success = readTextField (field);
           if (!success)
           {
             return (false);
           }
         }
 
         // ------------------------------------------------
         else if (fieldType.equals (COMBO_FIELD))
         {
           success = readComboBox (field);
           if (!success)
           {
             return (false);
           }
         }
 
         // ------------------------------------------------
         else if (fieldType.equals (RADIO_FIELD))
         {
           success = readRadioButton (field);
           if (!success)
           {
             return (false);
           }
         }
 
         // ------------------------------------------------
         else if (fieldType.equals (CHECK_FIELD))
         {
           success = readCheckBox (field);
           if (!success)
           {
             return (false);
           }
         }
         else if (fieldType.equals (SEARCH_FIELD))
         {
           success = readSearch (field);
           if (!success)
           {
             return (false);
           }
         }
       }
     }
 
     return (true);
   }
  /*--------------------------------------------------------------------------*/
  /**
   * Reads the XML specification for the panel layout. The result is
   * stored in spec.
   *
   * @exception Exception for any problems in reading the specification
   */
  /*--------------------------------------------------------------------------*/
   private void readSpec () throws Exception
   {
     InputStream input = null;
     XMLElement  data;
     Vector      specElements;
     String      attribute;
     String      instance = Integer.toString (instanceNumber);
 
     try
     {
       input = parent.getResource (SPEC_FILE_NAME);
     }
     catch (Exception exception)
     {
       haveSpec = false;
       return;
     }
     if (input == null)
     {
       haveSpec = false;
       return;
     }
 
     // initialize the parser
     StdXMLParser parser = new StdXMLParser ();
     parser.setBuilder   (new StdXMLBuilder ());
     parser.setValidator (new NonValidator ());
     parser.setReader    (new StdXMLReader (input));
 
     // get the data
     data = (XMLElement) parser.parse ();
 
     // extract the spec to this specific panel instance
     if (data.hasChildren ())
     {
       specElements = data.getChildrenNamed (NODE_ID);
       for (int i = 0; i < specElements.size (); i++)
       {
         data      = (XMLElement)specElements.elementAt (i);
         attribute = data.getAttribute (INSTANCE_IDENTIFIER);
 
         if (instance.equals (attribute))
         {
           // use the current element as spec
           spec = data;
           // close the stream
           input.close ();
           haveSpec = true;
           return;
         }
       }
 
       haveSpec = false;
       return;
     }
 
     haveSpec = false;
   }
  /*--------------------------------------------------------------------------*/
  /**
   * Adds the title to the panel. There can only be one title, if mutiple
   * titles are defined, they keep overwriting what has already be defined,
   * so that the last definition is the one that prevails.
   *
   * @param     spec  a <code>XMLElement</code> containing the specification
   *                  for the title.
   */
  /*--------------------------------------------------------------------------*/
   private void addTitle (XMLElement spec)
   {
     String  title       = getText       (spec);
     boolean italic      = getBoolean    (spec, ITALICS, false);
     boolean bold        = getBoolean    (spec, BOLD, false);
     float   multiplier  = getFloat      (spec, SIZE, 2.0f);
     int     justify     = getAlignment  (spec);
 
     if (title != null)
     {
       JLabel label = new JLabel (title);
       Font   font  = label.getFont ();
       float  size  = font.getSize ();
       int    style = 0;
 
       if (bold)
       {
         style = style + Font.BOLD;
       }
       if (italic)
       {
         style = style + Font.ITALIC;
       }
 
       font = font.deriveFont (style, (size * multiplier));
       label.setFont (font);
       label.setAlignmentX (0);
 
       TwoColumnConstraints constraints = new TwoColumnConstraints ();
       constraints.align     = justify;
       constraints.position  = TwoColumnConstraints.NORTH;
 
       add (label, constraints);
     }
   }
  /*--------------------------------------------------------------------------*/
  /**
   * Adds a rule field to the list of UI elements.
   *
   * @param     spec  a <code>XMLElement</code> containing the specification
   *                  for the rule field.
   */
  /*--------------------------------------------------------------------------*/
   private void addRuleField (XMLElement spec)
   {
     Vector forPacks = spec.getChildrenNamed (PACKS);
     XMLElement      element       = spec.getFirstChildNamed (SPEC);
     String          variable      = spec.getAttribute (VARIABLE);
     RuleInputField  field         = null;
     JLabel          label;
     String          layout;
     String          set;
     String          separator;
     String          format;
     String          validator     = null;
     String          message       = null;
     String          processor     = null;
     int             resultFormat  = RuleInputField.DISPLAY_FORMAT;
 
     // ----------------------------------------------------
     // extract the specification details
     // ----------------------------------------------------
     if (element != null)
     {
       label     = new JLabel (getText (element));
       layout    = element.getAttribute (RULE_LAYOUT);
       set       = element.getAttribute (SET);
 
       // retrieve value of variable if not specified
       // (does not work here because of special format for set attribute)
       //if (set == null)
       //{
       //  set = idata.getVariable (variable);
       //}
 
       separator = element.getAttribute (RULE_SEPARATOR);
       format    = element.getAttribute (RULE_RESULT_FORMAT);
 
       if (format != null)
       {
         if (format.equals (RULE_PLAIN_STRING))
         {
           resultFormat = RuleInputField.PLAIN_STRING;
         }
         else if (format.equals (RULE_DISPLAY_FORMAT))
         {
           resultFormat = RuleInputField.DISPLAY_FORMAT;
         }
         else if (format.equals (RULE_SPECIAL_SEPARATOR))
         {
           resultFormat = RuleInputField.SPECIAL_SEPARATOR;
         }
         else if (format.equals (RULE_ENCRYPTED))
         {
           resultFormat = RuleInputField.ENCRYPTED;
         }
       }
     }
     // ----------------------------------------------------
     // if there is no specification element, return without
     // doing anything.
     // ----------------------------------------------------
     else
     {
       return;
     }
 
     // ----------------------------------------------------
     // get the description and add it to the list of UI
     // elements if it exists.
     // ----------------------------------------------------
     element = spec.getFirstChildNamed (DESCRIPTION);
     addDescription (element, forPacks);
 
     // ----------------------------------------------------
     // get the validator and processor if they are defined
     // ----------------------------------------------------
     element = spec.getFirstChildNamed (VALIDATOR);
     if (element != null)
     {
       validator = element.getAttribute (CLASS);
       message   = getText (element);
     }
 
     element = spec.getFirstChildNamed (PROCESSOR);
     if (element != null)
     {
       processor = element.getAttribute (CLASS);
     }
 
     // ----------------------------------------------------
     // create an instance of RuleInputField based on the
     // extracted specifications, then add it to the list
     // of UI elements.
     // ----------------------------------------------------
     field = new RuleInputField (layout,
                                 set,
                                 separator,
                                 validator,
                                 processor,
                                 resultFormat,
                                 getToolkit ());
 
     TwoColumnConstraints constraints = new TwoColumnConstraints ();
     constraints.position              = TwoColumnConstraints.WEST;
 
     uiElements.add (new Object [] {null, FIELD_LABEL, null, constraints, label, forPacks});
 
     TwoColumnConstraints constraints2 = new TwoColumnConstraints ();
     constraints2.position             = TwoColumnConstraints.EAST;
 
     uiElements.add (new Object [] {null, RULE_FIELD, variable, constraints2, field, forPacks, null, null, message});
   }
  /*--------------------------------------------------------------------------*/
  /**
   * Reads the data from the rule input field and sets the associated variable.
   *
   * @param     field  the object array that holds the details of the field.
   *
   * @return    <code>true</code> if there was no problem reading the data or
   *            if there was an irrecovarable problem. If there was a problem
   *            that can be corrected by the operator, an error dialog is
   *            popped up and <code>false</code> is returned.
   */
  /*--------------------------------------------------------------------------*/
   private boolean readRuleField (Object [] field)
   {
     RuleInputField  ruleField = null;
     String          variable  = null;
 
     try
     {
       ruleField = (RuleInputField)field [POS_FIELD];
       variable  = (String)field [POS_VARIABLE];
     }
     catch (Throwable exception)
     {
       return (true);
     }
     if ((variable == null) || (ruleField == null))
     {
       return (true);
     }
 
     boolean success = ruleField.validateContents ();
     if (!success)
     {
       JOptionPane.showMessageDialog (parent,
                                      (String)field [POS_MESSAGE],
                                      parent.langpack.getString ("UserInputPanel.error.caption"),
                                      JOptionPane.WARNING_MESSAGE);
       return (false);
     }
 
     idata.getVariableValueMap ().setVariable (variable, ruleField.getText ());
     entries.add (new TextValuePair (variable, ruleField.getText ()));
     return (true);
   }
  /*--------------------------------------------------------------------------*/
  /**
   * Adds a text field to the list of UI elements
   *
   * @param     spec  a <code>XMLElement</code> containing the specification
   *                  for the text field.
   */
  /*--------------------------------------------------------------------------*/
   private void addTextField (XMLElement spec)
   {
     Vector      forPacks = spec.getChildrenNamed (PACKS);
     XMLElement  element  = spec.getFirstChildNamed (SPEC);
     JLabel      label;
     String      set;
     int         size;
 
     String      variable = spec.getAttribute (VARIABLE);
     if ((variable == null) || (variable.length () == 0))
     {
       return;
     }
 
     // ----------------------------------------------------
     // extract the specification details
     // ----------------------------------------------------
     if (element != null)
     {
       label = new JLabel (getText (element));
       set   = element.getAttribute (SET);
       if (set == null)
       {
         set = idata.getVariable (variable);
         if (set == null)
         {
           set = "";
         }
       }
       try
       {
         size = Integer.parseInt (element.getAttribute (TEXT_SIZE));
       }
       catch (Throwable exception)
       {
         size = 1;
       }
     }
     // ----------------------------------------------------
     // if there is no specification element, return without
     // doing anything.
     // ----------------------------------------------------
     else
     {
       return;
     }
 
     // ----------------------------------------------------
     // get the description and add it to the list UI
     // elements if it exists.
     // ----------------------------------------------------
     element = spec.getFirstChildNamed (DESCRIPTION);
     addDescription (element, forPacks);
 
     // ----------------------------------------------------
     // construct the UI element and add it to the list
     // ----------------------------------------------------
     JTextField field = new JTextField (set, size);
     field.setCaretPosition (0);
 
     TwoColumnConstraints constraints = new TwoColumnConstraints ();
     constraints.position  = TwoColumnConstraints.WEST;
 
     uiElements.add (new Object [] {null, FIELD_LABEL, null, constraints, label, forPacks});
 
     TwoColumnConstraints constraints2 = new TwoColumnConstraints ();
     constraints2.position  = TwoColumnConstraints.EAST;
 
     uiElements.add (new Object [] {null, TEXT_FIELD, variable, constraints2, field, forPacks});
   }
  /*--------------------------------------------------------------------------*/
  /**
   * Reads data from the text field and sets the associated variable.
   *
   * @param     field  the object array that holds the details of the field.
   *
   * @return    <code>true</code> if there was no problem reading the data or
   *            if there was an irrecovarable problem. If there was a problem
   *            that can be corrected by the operator, an error dialog is
   *            popped up and <code>false</code> is returned.
   */
  /*--------------------------------------------------------------------------*/
   private boolean readTextField (Object [] field)
   {
     JTextField  textField = null;
     String      variable  = null;
     String      value     = null;
 
     try
     {
       textField = (JTextField)field [POS_FIELD];
       variable  = (String)field [POS_VARIABLE];
       value     = textField.getText ();
     }
     catch (Throwable exception)
     {
       return (true);
     }
     if ((variable == null) || (value == null))
     {
       return (true);
     }
 
     idata.getVariableValueMap ().setVariable (variable, value);
     entries.add (new TextValuePair (variable, value));
     return (true);
   }
  /*--------------------------------------------------------------------------*/
  /**
   * Adds a combo box to the list of UI elements.<br>
   * This is a complete example of a valid XML specification
   * <pre>
   * <field type="combo" variable="testVariable">
   *   <description text="Description for the combo box" id="a key for translated text"/>
   *   <spec text="label" id="key for the label"/>
   *     <choice text="choice 1" id="" value="combo box 1"/>
   *     <choice text="choice 2" id="" value="combo box 2" set="true"/>
   *     <choice text="choice 3" id="" value="combo box 3"/>
   *     <choice text="choice 4" id="" value="combo box 4"/>
   *   </spec>
   * </field>
   * </pre>
   * @param     spec  a <code>XMLElement</code> containing the specification
   *                  for the combo box.
   */
  /*--------------------------------------------------------------------------*/
   private void addComboBox (XMLElement spec)
   {
     Vector        forPacks  = spec.getChildrenNamed (PACKS);
     XMLElement    element   = spec.getFirstChildNamed (SPEC);
     String        variable  = spec.getAttribute (VARIABLE);
     TextValuePair listItem  = null;
     JComboBox     field     = new JComboBox ();
     JLabel        label;
 
     // ----------------------------------------------------
     // extract the specification details
     // ----------------------------------------------------
     if (element != null)
     {
       label = new JLabel (getText (element));
 
       Vector choices = element.getChildrenNamed (COMBO_CHOICE);
 
       if (choices == null)
       {
         return;
       }
 
       for (int i = 0; i < choices.size (); i++)
       {
         listItem = new TextValuePair (getText ((XMLElement)choices.elementAt (i)),
                                       ((XMLElement)choices.elementAt (i)).getAttribute (COMBO_VALUE));
 
         field.addItem (listItem);
 
         String set    = ((XMLElement)choices.elementAt (i)).getAttribute (SET);
         if (set != null)
         {
           if (set.equals (TRUE))
           {
             field.setSelectedIndex (i);
           }
         }
       }
     }
     // ----------------------------------------------------
     // if there is no specification element, return without
     // doing anything.
     // ----------------------------------------------------
     else
     {
       return;
     }
 
     // ----------------------------------------------------
     // get the description and add it to the list of UI
     // elements if it exists.
     // ----------------------------------------------------
     element = spec.getFirstChildNamed (DESCRIPTION);
     addDescription (element, forPacks);
 
     TwoColumnConstraints constraints = new TwoColumnConstraints ();
     constraints.position  = TwoColumnConstraints.WEST;
 
     uiElements.add (new Object [] {null, FIELD_LABEL, null, constraints, label, forPacks});
 
     TwoColumnConstraints constraints2 = new TwoColumnConstraints ();
     constraints2.position  = TwoColumnConstraints.EAST;
 
     uiElements.add (new Object [] {null, COMBO_FIELD, variable, constraints2, field, forPacks});
   }
  /*--------------------------------------------------------------------------*/
  /**
   * Reads the content of the combobox field and substitutes the associated
   * variable.
   *
   * @param     field  the object array that holds the details of the field.
   *
   * @return    <code>true</code> if there was no problem reading the data or
   *            if there was an irrecovarable problem. If there was a problem
   *            that can be corrected by the operator, an error dialog is
   *            popped up and <code>false</code> is returned.
   */
  /*--------------------------------------------------------------------------*/
   private boolean readComboBox (Object [] field)
   {
     String variable = null;
     String value    = null;
     JComboBox comboBox = null;
 
     try
     {
       variable  = (String)field [POS_VARIABLE];
       comboBox  = (JComboBox)field [POS_FIELD];
       value     = ((TextValuePair)comboBox.getSelectedItem ()).getValue ();
     }
     catch (Throwable exception)
     {
       return (true);
     }
     if ((variable == null) || (value == null))
     {
       return (true);
     }
 
     idata.getVariableValueMap ().setVariable (variable, value);
     entries.add (new TextValuePair (variable, value));
     return (true);
   }
  /*--------------------------------------------------------------------------*/
  /**
   * Adds a radio button set to the list of UI elements.<br>
   * This is a complete example of a valid XML specification
   * <pre>
   * <field type="radio" variable="testVariable">
   *   <description text="Description for the radio buttons" id="a key for translated text"/>
   *   <spec text="label" id="key for the label"/>
   *     <choice text="radio 1" id="" value=""/>
   *     <choice text="radio 2" id="" value="" set="true"/>
   *     <choice text="radio 3" id="" value=""/>
   *     <choice text="radio 4" id="" value=""/>
   *     <choice text="radio 5" id="" value=""/>
   *   </spec>
   * </field>
   * </pre>
   *
   * @param     spec  a <code>XMLElement</code> containing the specification
   *                  for the radio button set.
   */
  /*--------------------------------------------------------------------------*/
   private void addRadioButton (XMLElement spec)
   {
     Vector forPacks = spec.getChildrenNamed (PACKS);
     String                variable    = spec.getAttribute (VARIABLE);
     String                value       = null;
 
     XMLElement            element     = null;
     JLabel                label;
 
     ButtonGroup           group       = new ButtonGroup ();
 
     TwoColumnConstraints  constraints = new TwoColumnConstraints ();
     constraints.position              = TwoColumnConstraints.BOTH;
     constraints.indent                = true;
     constraints.stretch               = true;
 
     // ----------------------------------------------------
     // get the description and add it to the list of UI
     // elements if it exists.
     // ----------------------------------------------------
     element = spec.getFirstChildNamed (DESCRIPTION);
     addDescription (element, forPacks);
 
     // ----------------------------------------------------
     // extract the specification details
     // ----------------------------------------------------
     element = spec.getFirstChildNamed (SPEC);
 
     if (element != null)
     {
       // TODO: label is never added to the UI
       label = new JLabel (getText (element));
 
       Vector choices = element.getChildrenNamed (RADIO_CHOICE);
 
       if (choices == null)
       {
         return;
       }
 
       // --------------------------------------------------
       // process each choice element
       // --------------------------------------------------
       for (int i = 0; i < choices.size (); i++)
       {
         JRadioButton choice   = new JRadioButton ();
         choice.setText          (getText ((XMLElement)choices.elementAt (i)));
         value                 = (((XMLElement)choices.elementAt (i)).getAttribute (RADIO_VALUE));
 
         group.add (choice);
 
         String set    = ((XMLElement)choices.elementAt (i)).getAttribute (SET);
         if (set != null)
         {
           if (set.equals (TRUE))
           {
             choice.setSelected (true);
           }
         }
 
         buttonGroups.add (group);
         uiElements.add (new Object [] {null, RADIO_FIELD, variable, constraints, choice, forPacks, value, null, null, group});
       }
     }
   }
  /*--------------------------------------------------------------------------*/
  /**
   * Reads the content of the radio button field and substitutes the associated
   * variable.
   *
   * @param     field  the object array that holds the details of the field.
   *
   * @return    <code>true</code> if there was no problem reading the data or
   *            if there was an irrecovarable problem. If there was a problem
   *            that can be corrected by the operator, an error dialog is
   *            popped up and <code>false</code> is returned.
   */
  /*--------------------------------------------------------------------------*/
   private boolean readRadioButton (Object [] field)
   {
     String variable     = null;
     String value        = null;
     JRadioButton button = null;
 
     try
     {
       button   = (JRadioButton)field [POS_FIELD];
 
       if (!button.isSelected ())
       {
         return (true);
       }
 
       variable = (String)field [POS_VARIABLE];
       value    = (String)field [POS_TRUE];
     }
     catch (Throwable exception)
     {
       return (true);
     }
 
     idata.getVariableValueMap ().setVariable (variable, value);
     entries.add (new TextValuePair (variable, value));
     return (true);
   }
  /*--------------------------------------------------------------------------*/
  /**
   * Adds one or more password fields to the list of UI elements.<br>
   * This is a complete example of a valid XML specification
   * <pre>
   * <field type="password" variable="testVariable">
   *   <description align="left" txt="Please enter your password" id="a key for translated text"/>
   *   <spec>
   *     <pwd txt="Password" id="key for the label" size="10" set=""/>
   *     <pwd txt="Retype password" id="another key for the label" size="10" set=""/>
   *   </spec>
   *   <validator class="com.izforge.sample.PWDValidator" txt="Both versions of the password must match" id="key for the error text"/>
   *   <processor class="com.izforge.sample.PWDEncryptor"/>
   * </field>
   * </pre>
   *
   * @param     spec  a <code>XMLElement</code> containing the specification
   *                  for the set of password fields.
   */
  /*--------------------------------------------------------------------------*/
   private void addPasswordField (XMLElement spec)
   {
     Vector        forPacks  = spec.getChildrenNamed (PACKS);
     String        variable  = spec.getAttribute (VARIABLE);
     String        validator = null;
     String        message   = null;
     String        processor = null;
     XMLElement    element   = null;
     PasswordGroup group     = null;
     int           size      = 0;
 
     // ----------------------------------------------------
     // get the description and add it to the list of UI
     // elements if it exists.
     // ----------------------------------------------------
     element = spec.getFirstChildNamed (DESCRIPTION);
     addDescription (element, forPacks);
 
     // ----------------------------------------------------
     // get the validator and processor if they are defined
     // ----------------------------------------------------
     element = spec.getFirstChildNamed (VALIDATOR);
     if (element != null)
     {
       validator = element.getAttribute (CLASS);
       message   = getText (element);
     }
 
     element = spec.getFirstChildNamed (PROCESSOR);
     if (element != null)
     {
       processor = element.getAttribute (CLASS);
     }
 
     group = new PasswordGroup (validator,
                                processor);
 
     // ----------------------------------------------------
     // extract the specification details
     // ----------------------------------------------------
     element = spec.getFirstChildNamed (SPEC);
 
     if (element != null)
     {
       Vector inputs = element.getChildrenNamed (PWD_INPUT);
 
       if (inputs == null)
       {
         return;
       }
 
       // --------------------------------------------------
       // process each input field
       // --------------------------------------------------
       XMLElement fieldSpec;
       for (int i = 0; i < inputs.size (); i++)
       {
         fieldSpec     = (XMLElement)inputs.elementAt (i);
         String set    = fieldSpec.getAttribute (SET);
         JLabel label  = new JLabel (getText (fieldSpec));
         try
         {
           size = Integer.parseInt (fieldSpec.getAttribute (PWD_SIZE));
         }
         catch (Throwable exception)
         {
           size = 1;
         }
 
         // ----------------------------------------------------
         // construct the UI element and add it to the list
         // ----------------------------------------------------
         JPasswordField field = new JPasswordField (set, size);
         field.setCaretPosition (0);
 
         TwoColumnConstraints constraints = new TwoColumnConstraints ();
         constraints.position  = TwoColumnConstraints.WEST;
 
         uiElements.add (new Object [] {null, FIELD_LABEL, null, constraints, label, forPacks});
 
         TwoColumnConstraints constraints2 = new TwoColumnConstraints ();
         constraints2.position  = TwoColumnConstraints.EAST;
 
         uiElements.add (new Object [] {null, PWD_FIELD, variable, constraints2, field, forPacks, null, null, message, group});
         group.addField (field);
       }
     }
 
     passwordGroups.add (group);
   }
  /*--------------------------------------------------------------------------*/
  /**
   * Reads the content of the password field and substitutes the associated
   * variable.
   *
   * @param     field  a password group that manages one or more passord fields.
   *
   * @return    <code>true</code> if there was no problem reading the data or
   *            if there was an irrecovarable problem. If there was a problem
   *            that can be corrected by the operator, an error dialog is
   *            popped up and <code>false</code> is returned.
   */
  /*--------------------------------------------------------------------------*/
   private boolean readPasswordField (Object [] field)
   {
     PasswordGroup   group     = null;
     String          variable  = null;
     String          message   = null;
 
     try
     {
       group         = (PasswordGroup)field [POS_GROUP];
       variable      = (String)field [POS_VARIABLE];
       message       = (String)field [POS_MESSAGE];
     }
     catch (Throwable exception)
     {
       return (true);
     }
     if ((variable == null) || (passwordGroupsRead.contains (group)))
     {
       return (true);
     }
     passwordGroups.add (group);
 
 
     boolean success = group.validateContents ();
 
     if (!success)
     {
       JOptionPane.showMessageDialog (parent,
                                      message,
                                      parent.langpack.getString ("UserInputPanel.error.caption"),
                                      JOptionPane.WARNING_MESSAGE);
       return (false);
     }
 
     idata.getVariableValueMap ().setVariable (variable, group.getPassword ());
     entries.add (new TextValuePair (variable, group.getPassword ()));
     return (true);
   }
  /*--------------------------------------------------------------------------*/
  /**
   * Adds a chackbox to the list of UI elements.
   *
   * @param     spec  a <code>XMLElement</code> containing the specification
   *                  for the checkbox.
   */
  /*--------------------------------------------------------------------------*/
   private void addCheckBox (XMLElement spec)
   {
     Vector      forPacks    = spec.getChildrenNamed (PACKS);
     String      label       = "";
     String      set         = null;
     String      trueValue   = null;
     String      falseValue  = null;
     String      variable    = spec.getAttribute (VARIABLE);
     XMLElement  detail      = spec.getFirstChildNamed (SPEC);
 
     if (variable == null)
     {
       return;
     }
 
     if (detail != null)
     {
       label       = getText (detail);
       set         = detail.getAttribute (SET);
       trueValue   = detail.getAttribute (TRUE);
       falseValue  = detail.getAttribute (FALSE);
     }
 
     JCheckBox   checkbox  = new JCheckBox (label);
 
     if (set != null)
     {
       if (set.equals (FALSE))
       {
         checkbox.setSelected (false);
       }
       if (set.equals (TRUE))
       {
         checkbox.setSelected (true);
       }
     }
 
     // ----------------------------------------------------
     // get the description and add it to the list of UI
     // elements if it exists.
     // ----------------------------------------------------
     XMLElement element = spec.getFirstChildNamed (DESCRIPTION);
     addDescription (element, forPacks);
 
     TwoColumnConstraints constraints = new TwoColumnConstraints ();
     constraints.position  = TwoColumnConstraints.BOTH;
     constraints.stretch   = true;
     constraints.indent    = true;
 
     uiElements.add (new Object [] {null, CHECK_FIELD, variable, constraints, checkbox, forPacks, trueValue, falseValue});
   }
  /*--------------------------------------------------------------------------*/
  /**
   * Reads the content of the checkbox field and substitutes the associated
   * variable.
   *
   * @param     field  the object array that holds the details of the field.
   *
   * @return    <code>true</code> if there was no problem reading the data or
   *            if there was an irrecovarable problem. If there was a problem
   *            that can be corrected by the operator, an error dialog is
   *            popped up and <code>false</code> is returned.
   */
  /*--------------------------------------------------------------------------*/
   private boolean readCheckBox (Object [] field)
   {
     String    variable    = null;
     String    trueValue   = null;
     String    falseValue  = null;
     JCheckBox box         = null;
 
     try
     {
       box         = (JCheckBox)field [POS_FIELD];
       variable    = (String)field [POS_VARIABLE];
       trueValue   = (String)field [POS_TRUE];
       if (trueValue == null)
       {
         trueValue = "";
       }
 
       falseValue  = (String)field [POS_FALSE];
       if (falseValue == null)
       {
         falseValue = "";
       }
     }
     catch (Throwable exception)
     {
       return (true);
     }
 
     if (box.isSelected ())
     {
       idata.getVariableValueMap ().setVariable (variable, trueValue);
       entries.add (new TextValuePair (variable, trueValue));
     }
     else
     {
       idata.getVariableValueMap ().setVariable (variable, falseValue);
       entries.add (new TextValuePair (variable, falseValue));
     }
 
     return (true);
   }
  /*--------------------------------------------------------------------------*/
  /**
   * Adds a search field to the list of UI elements.<br>
   * This is a complete example of a valid XML specification
   * <pre>
   * <field type="search" variable="testVariable">
   *   <description text="Description for the search field" id="a key for translated text"/>
   *   <spec text="label" id="key for the label" filename="the_file_to_search" result="directory" /> <!-- values for result: directory, file -->
   *     <choice dir="directory1" set="true" /> <!-- default value -->
   *     <choice dir="dir2" />
   *   </spec>
   * </field>
   * </pre>
   * @param     spec  a <code>XMLElement</code> containing the specification
   *                  for the search field
   */
  /*--------------------------------------------------------------------------*/
   private void addSearch (XMLElement spec)
   {
     Vector        forPacks    = spec.getChildrenNamed (PACKS);
     XMLElement    element     = spec.getFirstChildNamed (SPEC);
     String        variable    = spec.getAttribute (VARIABLE);
     String        filename    = null;
     String        check_filename = null;
     int           search_type = 0;
     int           result_type = 0;
     JComboBox     combobox    = new JComboBox ();
     JLabel        label       = null;
 
     //System.out.println ("adding search combobox, variable "+variable);
 
     // allow the user to enter something
     combobox.setEditable (true);
 
     // ----------------------------------------------------
     // extract the specification details
     // ----------------------------------------------------
     if (element != null)
     {
       label = new JLabel (getText (element));
 
       // search type is optional (default: file)
       search_type = SearchField.TYPE_FILE;
 
       String search_type_str = element.getAttribute (SEARCH_TYPE);
 
       if (search_type_str != null)
       {
         if (search_type_str.equals (SEARCH_FILE))
         {
           search_type = SearchField.TYPE_FILE;
         }
         else if (search_type_str.equals (SEARCH_DIRECTORY))
         {
           search_type = SearchField.TYPE_DIRECTORY;
         }
       }
 
       // result type is mandatory too
       String result_type_str = element.getAttribute (SEARCH_RESULT);
 
       if (result_type_str == null)
       {
         return;
       }
       else if (result_type_str.equals (SEARCH_FILE))
       {
         result_type = SearchField.RESULT_FILE;
       }
       else if (result_type_str.equals (SEARCH_DIRECTORY))
       {
         result_type = SearchField.RESULT_DIRECTORY;
       }
       else if (result_type_str.equals (SEARCH_PARENTDIR))
       {
         result_type = SearchField.RESULT_PARENTDIR;
       }
       else
       {
         return;
       }
 
       // might be missing - null is okay
       filename = element.getAttribute (SEARCH_FILENAME);
 
       check_filename = element.getAttribute (SEARCH_CHECKFILENAME);
       
       Vector choices = element.getChildrenNamed (SEARCH_CHOICE);
 
       if (choices == null)
       {
         return;
       }
 
       for (int i = 0; i < choices.size (); i++)
       {
         XMLElement choice_el = (XMLElement)choices.elementAt (i);
         
         if (! OsConstraint.oneMatchesCurrentSystem(element))
           continue;
         
         String value = choice_el.getAttribute (SEARCH_VALUE);
 
         combobox.addItem (value);
 
         String set    = ((XMLElement)choices.elementAt (i)).getAttribute (SET);
         if (set != null)
         {
           if (set.equals (TRUE))
           {
             combobox.setSelectedIndex (i);
           }
         }
       }
     }
     // ----------------------------------------------------
     // if there is no specification element, return without
     // doing anything.
     // ----------------------------------------------------
     else
     {
       return;
     }
 
     // ----------------------------------------------------
     // get the description and add it to the list of UI
     // elements if it exists.
     // ----------------------------------------------------
     element = spec.getFirstChildNamed (DESCRIPTION);
     addDescription (element, forPacks);
 
     TwoColumnConstraints westconstraint1 = new TwoColumnConstraints ();
     westconstraint1.position  = TwoColumnConstraints.WEST;
 
     uiElements.add (new Object [] {null, FIELD_LABEL, null, westconstraint1, label, forPacks});
 
     TwoColumnConstraints eastconstraint1 = new TwoColumnConstraints ();
     eastconstraint1.position  = TwoColumnConstraints.EAST;
 
     StringBuffer tooltiptext = new StringBuffer ();
     
     if ((filename != null) && (filename.length() > 0))
     {
       tooltiptext.append (
         MessageFormat.format (parent.langpack.getString ("UserInputPanel.search.location"), 
             new String[] { filename} ));
     }
     
     if ((check_filename != null) && (check_filename.length() > 0))
     {
       tooltiptext.append (          
         MessageFormat.format (parent.langpack.getString ("UserInputPanel.search.location.checkedfile"), 
           new String[] { check_filename } ));
     }
     
     if (tooltiptext.length() > 0)
       combobox.setToolTipText (tooltiptext.toString());
 
     uiElements.add (new Object [] {null, SEARCH_FIELD, variable, eastconstraint1, combobox, forPacks});
 
     JPanel buttonPanel = new JPanel ();
     buttonPanel.setLayout (new com.izforge.izpack.gui.FlowLayout (com.izforge.izpack.gui.FlowLayout.LEADING));
 
     JButton autodetectButton = ButtonFactory.createButton (parent.langpack.getString ("UserInputPanel.search.autodetect"), idata.buttonsHColor);
 
     autodetectButton.setToolTipText (parent.langpack.getString ("UserInputPanel.search.autodetect.tooltip"));
 
     buttonPanel.add (autodetectButton);
 
     JButton browseButton = ButtonFactory.createButton (parent.langpack.getString ("UserInputPanel.search.browse"), idata.buttonsHColor);
 
     buttonPanel.add (browseButton);
 
     TwoColumnConstraints eastonlyconstraint = new TwoColumnConstraints ();
     eastonlyconstraint.position  = TwoColumnConstraints.EASTONLY;
 
     uiElements.add (new Object [] {null, SEARCH_BUTTON_FIELD, null, eastonlyconstraint, buttonPanel, forPacks});
 
     searchFields.add (new SearchField (filename, check_filename, parent, combobox, autodetectButton, browseButton, search_type, result_type));
   }
  /*--------------------------------------------------------------------------*/
  /**
   * Reads the content of the search field and substitutes the associated
   * variable.
   *
   * @param     field  the object array that holds the details of the field.
   *
   * @return    <code>true</code> if there was no problem reading the data or
   *            if there was an irrecovarable problem. If there was a problem
   *            that can be corrected by the operator, an error dialog is
   *            popped up and <code>false</code> is returned.
   */
  /*--------------------------------------------------------------------------*/
   private boolean readSearch (Object [] field)
   {
     String variable = null;
     String value    = null;
     JComboBox comboBox = null;
 
     try
     {
       variable  = (String)field [POS_VARIABLE];
       comboBox  = (JComboBox)field [POS_FIELD];
       for (int i = 0; i < this.searchFields.size(); ++i)
       {
         SearchField sf = (SearchField)this.searchFields.elementAt (i);
         if (sf.belongsTo (comboBox))
         {
           value = sf.getResult ();
           break;
         }
       }
     }
     catch (Throwable exception)
     {
       return (true);
     }
     if ((variable == null) || (value == null))
     {
       return (true);
     }
 
     idata.getVariableValueMap ().setVariable (variable, value);
     entries.add (new TextValuePair (variable, value));
     return (true);
   }
  /*--------------------------------------------------------------------------*/
  /**
   * Adds text to the list of UI elements
   *
   * @param     spec  a <code>XMLElement</code> containing the specification
   *                  for the text.
   */
  /*--------------------------------------------------------------------------*/
   private void addText (XMLElement spec)
   {
     Vector forPacks = spec.getChildrenNamed (PACKS);
 
     addDescription (spec, forPacks);
   }
  /*--------------------------------------------------------------------------*/
  /**
   * Adds a dummy field to the list of UI elements to act as spacer.
   *
   * @param     spec  a <code>XMLElement</code> containing other specifications.
   *                  At present this information is not used but might be in
   *                  future versions.
   */
  /*--------------------------------------------------------------------------*/
   private void addSpace (XMLElement spec)
   {
     Vector forPacks = spec.getChildrenNamed (PACKS);
     JPanel panel    = new JPanel ();
 
     TwoColumnConstraints constraints = new TwoColumnConstraints ();
     constraints.position  = TwoColumnConstraints.BOTH;
     constraints.stretch   = true;
 
     uiElements.add (new Object [] {null, SPACE_FIELD, null, constraints, panel, forPacks});
   }
  /*--------------------------------------------------------------------------*/
  /**
   * Adds a dividing line to the list of UI elements act as separator.
   *
   * @param     spec  a <code>XMLElement</code> containing additional
   *                  specifications.
   */
  /*--------------------------------------------------------------------------*/
   private void addDivider (XMLElement spec)
   {
     Vector forPacks   = spec.getChildrenNamed (PACKS);
     JPanel panel      = new JPanel ();
     String alignment  = spec.getAttribute (ALIGNMENT);
 
     if (alignment != null)
     {
       if (alignment.equals (TOP))
       {
         panel.setBorder (BorderFactory.createMatteBorder (1, 0, 0, 0, Color.gray));
       }
       else
       {
         panel.setBorder (BorderFactory.createMatteBorder (0, 0, 1, 0, Color.gray));
       }
     }
     else
     {
       panel.setBorder (BorderFactory.createMatteBorder (0, 0, 1, 0, Color.gray));
     }
 
     TwoColumnConstraints constraints = new TwoColumnConstraints ();
     constraints.position  = TwoColumnConstraints.BOTH;
     constraints.stretch   = true;
 
     uiElements.add (new Object [] {null, DIVIDER_FIELD, null, constraints, panel, forPacks});
   }
  /*--------------------------------------------------------------------------*/
  /**
   * Adds a description to the list of UI elements.
   *
   * @param     spec  a <code>XMLElement</code> containing the specification
   *                  for the description.
   */
  /*--------------------------------------------------------------------------*/
   private void addDescription (XMLElement spec,
                                Vector     forPacks)
   {
     String               description;
     TwoColumnConstraints constraints = new TwoColumnConstraints ();
     constraints.position  = TwoColumnConstraints.BOTH;
     constraints.stretch   = true;
 
     if (spec != null)
     {
       description = getText (spec);
 
       // if we have a description, add it to the UI elements
       if (description != null)
       {
         String  alignment = spec.getAttribute (ALIGNMENT);
         int     justify   = MultiLineLabel.LEFT;
 
         if (alignment != null)
         {
           if (alignment.equals (LEFT))
           {
             justify     = MultiLineLabel.LEFT;
           }
           else if (alignment.equals (CENTER))
           {
             justify     = MultiLineLabel.CENTER;
           }
           else if (alignment.equals (RIGHT))
           {
             justify     = MultiLineLabel.RIGHT;
           }
         }
 
         MultiLineLabel label = new MultiLineLabel (description, justify);
 
         uiElements.add (new Object [] {null, DESCRIPTION, null, constraints, label, forPacks});
       }
     }
   }
  /*--------------------------------------------------------------------------*/
  /**
   * Retrieves the value of a boolean attribute. If the attribute is found and
   * the values equals the value of the constant <code>TRUE</code> then true
   * is returned. If it equals <code>FALSE</code> the false is returned. In
   * all other cases, including when the attribute is not found, the default
   * value is returned.
   *
   * @param     element        the <code>XMLElement</code> to search for the
   *                           attribute.
   * @param     attribute      the attribute to search for
   * @param     defaultValue   the default value to use if the attribute does
   *                           not exist or a illegal value was discovered.
   *
   * @return    <code>true</code> if the attribute is found and the value
   *            equals the the constant <code>TRUE</code>. <<code> if the
   *            attribute is <code>FALSE</code>. In all other cases the
   *            default value is returned.
   */
  /*--------------------------------------------------------------------------*/
   private boolean getBoolean (XMLElement element,
                               String     attribute,
                               boolean    defaultValue)
   {
     boolean result = defaultValue;
 
     if ((attribute != null) && (attribute.length () > 0))
     {
       String value = element.getAttribute (attribute);
 
       if (value != null)
       {
         if (value.equals (TRUE))
         {
           result = true;
         }
         else if (value.equals (FALSE))
         {
           result = false;
         }
       }
     }
 
     return (result);
   }
  /*--------------------------------------------------------------------------*/
  /**
   * Retrieves the value of an integer attribute. If the attribute is not
   * found or the value is non-numeric then the default value is returned.
   *
   * @param     element      the <code>XMLElement</code> to search for the
   *                         attribute.
   * @param     attribute    the attribute to search for
   * @param     defaultValue the default value to use in case the attribute
   *                         does not exist.
   *
   * @return    the value of the attribute. If the attribute is not found or
   *            the content is not a legal integer, then the default value is
   *            returned.
   */
  /*--------------------------------------------------------------------------*/
   private int getInt (XMLElement  element,
                       String      attribute,
                       int         defaultValue)
   {
     int result = defaultValue;
 
     if ((attribute != null) && (attribute.length () > 0))
     {
       try
       {
         result = Integer.parseInt (element.getAttribute (attribute));
       }
       catch (Throwable exception)
       {}
     }
 
     return (result);
   }
  /*--------------------------------------------------------------------------*/
  /**
   * Retrieves the value of a floating point attribute. If the attribute is not
   * found or the value is non-numeric then the default value is returned.
   *
   * @param     element      the <code>XMLElement</code> to search for the
   *                         attribute.
   * @param     attribute    the attribute to search for
   * @param     defaultValue the default value to use in case the attribute
   *                         does not exist.
   *
   * @return    the value of the attribute. If the attribute is not found or
   *            the content is not a legal integer, then the default value is
   *            returned.
   */
  /*--------------------------------------------------------------------------*/
   private float getFloat (XMLElement  element,
                           String      attribute,
                           float       defaultValue)
   {
     float result = defaultValue;
 
     if ((attribute != null) && (attribute.length () > 0))
     {
       try
       {
         result = Float.parseFloat (element.getAttribute (attribute));
       }
       catch (Throwable exception)
       {}
     }
 
     return (result);
   }
  /*--------------------------------------------------------------------------*/
  /**
   * Extracts the text from an <code>XMLElement</code>. The text must be
   * defined in the resource file under the key defined in the <code>id</code>
   * attribute or as value of the attribute <code>text</code>.
   *
   * @param     element  the <code>XMLElement</code> from which to extract
   *                     the text.
   *
   * @return    The text defined in the <code>XMLElement</code>. If no text
   *            can be located, <code>null</code> is returned.
   */
  /*--------------------------------------------------------------------------*/
   private String getText (XMLElement element)
   {
     if (element == null)
     {
       return (null);
     }
 
     String key  = element.getAttribute (KEY);
     String text = null;
 
     if ((key != null) && (langpack != null))
     {
       try
       {
         text = langpack.getString (key);
       }
       catch (Throwable exception)
       {
         text = null;
       }
     }
 
     // if there is no text in the description, then
     // we were unable to retrieve it form the resource.
     // In this case try to get the text directly from
     // the XMLElement
     if (text == null)
     {
       text = element.getAttribute (TEXT);
     }
 
     return (text);
   }
  /*--------------------------------------------------------------------------*/
  /**
   * Retreives the alignment setting for the <code>XMLElement</code>. The
   * default value in case the <code>ALIGNMENT</code> attribute is not
   * found or the value is illegal is <code>TwoColumnConstraints.LEFT</code>.
   *
   * @param     element  the <code>XMLElement</code> from which to extract
   *                     the alignment setting.
   *
   * @return    the alignement setting for the <code>XMLElement</code>. The
   *            value is either <code>TwoColumnConstraints.LEFT</code>,
   *            <code>TwoColumnConstraints.CENTER</code> or
   *            <code>TwoColumnConstraints.RIGHT</code>.
   *
   * @see       com.izforge.izpack.gui.TwoColumnConstraints
   */
  /*--------------------------------------------------------------------------*/
   private int getAlignment (XMLElement element)
   {
     int result = TwoColumnConstraints.LEFT;
 
     String value = element.getAttribute (ALIGNMENT);
 
     if (value != null)
     {
       if (value.equals (LEFT))
       {
         result = TwoColumnConstraints.LEFT;
       }
       else if (value.equals (CENTER))
       {
         result = TwoColumnConstraints.CENTER;
       }
       else if (value.equals (RIGHT))
       {
         result = TwoColumnConstraints.RIGHT;
       }
     }
 
     return (result);
   }
  /*--------------------------------------------------------------------------*/
  /**
   * Verifies if an item is required for any of the packs listed. An item is
   * required for a pack in the list if that pack is actually selected for
   * installation.
   * <br><br>
   * <b>Note:</b><br>
   * If the list of selected packs is empty then <code>true</code> is always
   * returnd. The same is true if the <code>packs</code> list is empty.
   *
   * @param     packs  a <code>Vector</code> of <code>String</code>s. Each of
   *                   the strings denotes a pack for which an item
   *                   should be created if the pack is actually installed.
   *
   * @return    <code>true</code> if the item is required for at least
   *            one pack in the list, otherwise returns <code>false</code>.
   */
  /*--------------------------------------------------------------------------*/
  /*$
   * @design
   *
   * The information about the installed packs comes from
   * InstallData.selectedPacks. This assumes that this panel is presented to
   * the user AFTER the PacksPanel.
   *--------------------------------------------------------------------------*/
   private boolean itemRequiredFor (Vector packs)
   {
     String selected;
     String required;
 
     if (packs.size () == 0)
     {
       return (true);
     }
 
     // ----------------------------------------------------
     // We are getting to this point if any packs have been
     // specified. This means that there is a possibility
     // that some UI elements will not get added. This
     // means that we can not allow to go back to the
     // PacksPanel, because the process of building the
     // UI is not reversable.
     // ----------------------------------------------------
     packsDefined = true;
 
     // ----------------------------------------------------
     // analyze if the any of the packs for which the item
     // is required have been selected for installation.
     // ----------------------------------------------------
     for (int i = 0; i < idata.selectedPacks.size (); i++)
     {
       selected = ((Pack)idata.selectedPacks.get (i)).name;
 
       for (int k = 0; k < packs.size (); k++)
       {
         required = (String)((XMLElement)packs.elementAt (k)).getAttribute (NAME, "");
         if (selected.equals (required))
         {
           return (true);
         }
       }
     }
 
     return (false);
   }
 
 // --------------------------------------------------------------------------
 // Inner Classes
 // --------------------------------------------------------------------------
 
 /*---------------------------------------------------------------------------*/
 /**
  * This class can be used to associate a text string and a (text) value.
  */
 /*---------------------------------------------------------------------------*/
 private class TextValuePair
 {
   private String text   = "";
   private String value  = "";
 
  /*--------------------------------------------------------------------------*/
  /**
   * Constructs a new Text/Value pair, initialized with the text and a value.
   *
   * @param     text   the text that this object should represent
   * @param     value  the value that should be associated with this object
   */
  /*--------------------------------------------------------------------------*/
   public TextValuePair  (String text,
                          String value)
   {
     this.text   = text;
     this.value  = value;
   }
  /*--------------------------------------------------------------------------*/
  /**
   * Sets the text
   *
   * @param     text   the text for this object
   */
  /*--------------------------------------------------------------------------*/
   public void setText (String text)
   {
     this.text = text;
   }
  /*--------------------------------------------------------------------------*/
  /**
   * Sets the value of this object
   *
   * @param     value    the value for this object
   */
  /*--------------------------------------------------------------------------*/
   public void setValue (String value)
   {
     this.value = value;
   }
  /*--------------------------------------------------------------------------*/
  /**
   * This method returns the text that was set for the object
   *
   * @return    the object's text
   */
  /*--------------------------------------------------------------------------*/
   public String toString ()
   {
     return (text);
   }
  /*--------------------------------------------------------------------------*/
  /**
   * This method returns the value that was associated with this object
   *
   * @return    the object's value
   */
  /*--------------------------------------------------------------------------*/
   public String getValue ()
   {
     return (value);
   }
 }
 
 
 /*---------------------------------------------------------------------------*/
 /**
  * This class encapsulates a lot of search field functionality.
  *
  * A search field supports searching directories and files on the target 
  * system. This is a helper class to manage all data belonging to a
  * search field.
  */
 /*---------------------------------------------------------------------------*/
 
 private class SearchField implements ActionListener
 {
   /** used in constructor - we search for a directory. */
   public static final int       TYPE_DIRECTORY = 1;
   /** used in constructor - we search for a file. */
   public static final int       TYPE_FILE = 2;
 
   /** used in constructor - result of search is the directory. */
   public static final int       RESULT_DIRECTORY = 1;
   /** used in constructor - result of search is the whole file name. */
   public static final int       RESULT_FILE = 2;
   /** used in constructor - result of search is the parent directory. */
   public static final int       RESULT_PARENTDIR = 3;
 
   private String    filename = null;
   private String    checkFilename = null;
   private JButton   autodetectButton = null;
   private JButton   browseButton = null;
   private JComboBox pathComboBox = null;
   private int       searchType = TYPE_DIRECTORY;
   private int       resultType = RESULT_DIRECTORY;
 
   private InstallerFrame  parent = null;
 
   /*---------------------------------------------------------------------------*/
   /**
    * Constructor - initializes the object, adds it as action listener to
    * the "autodetect" button.
    *
    * @param filename    the name of the file to search for (might be null for
    *                    searching directories)
    * @param checkFilename the name of the file to check when searching for directories
    *                    (the checkFilename is appended to a found directory to figure out
    *                     whether it is the right directory)
    * @param combobox    the <code>JComboBox</code> holding the list of choices;
    *                    it should be editable and contain only Strings
    * @param autobutton  the autodetection button for triggering autodetection
    * @param browsebutton the browse button to look for the file
    * @param search_type what to search for - TYPE_FILE or TYPE_DIRECTORY
    * @param result_type what to return as the result - RESULT_FILE or 
    *                    RESULT_DIRECTORY or RESULT_PARENTDIR
    */
   /*---------------------------------------------------------------------------*/
   public SearchField (String filename, String checkFilename,
                       InstallerFrame parent, JComboBox combobox, 
                       JButton autobutton, JButton browsebutton, 
                       int search_type, int result_type)
   {
     this.filename = filename;
     this.checkFilename = checkFilename;
     this.parent = parent;
     this.autodetectButton = autobutton;
     this.browseButton = browsebutton;
     this.pathComboBox = combobox;
     this.searchType = search_type;
     this.resultType = result_type;
 
     this.autodetectButton.addActionListener (this);
     this.browseButton.addActionListener (this);
 
     autodetect ();
   }
 
   /** Check whether the given combobox belongs to this searchfield. 
    * This is used when reading the results.
    */
   public boolean belongsTo (JComboBox combobox)
   {
     return (this.pathComboBox == combobox);
   }
 
   /** check whether the given path matches */
   private boolean pathMatches (String path)
   {
     //System.out.println ("checking path " + path);
 
     File file = null;
     
     if ((this.filename == null) || (this.searchType == TYPE_DIRECTORY))
     {
       file = new File (path);
     }
     else
     {      
       file = new File (path, this.filename);
     }
 
     if (file.exists ())
     {
 
       if (((this.searchType == TYPE_DIRECTORY) && (file.isDirectory()))
           || ((this.searchType == TYPE_FILE) && (file.isFile ())))
       {
         // no file to check for
         if (this.checkFilename == null)
           return true;
         
         file = new File (file, this.checkFilename);
         
         return file.exists();
       }
       
     }
 
     //System.out.println (path + " did not match");
     return false;
   }
 
   /** perform autodetection */
   public boolean autodetect ()
   {
 
     // loop through all items
     for (int i = 0; i < this.pathComboBox.getItemCount(); ++i)
     {
       String path = (String)this.pathComboBox.getItemAt (i);
 
       if (this.pathMatches (path))
       {
         this.pathComboBox.setSelectedIndex (i);
         return true;
       }
 
     }
 
     // if the user entered something else, it's not listed as an item
     if (this.pathMatches ((String)this.pathComboBox.getSelectedItem()))
     {
       return true;
     }
 
     return false;
   }
 
  /*--------------------------------------------------------------------------*/
  /**
   * This is called if one of the buttons has bee pressed.
   *
   * It checks, which button caused the action and acts accordingly.
   */
  /*--------------------------------------------------------------------------*/
   public void actionPerformed (ActionEvent event)
   {
     //System.out.println ("autodetection button pressed.");
 
     if (event.getSource () == this.autodetectButton)
     {
       if (! autodetect ())
         JOptionPane.showMessageDialog (parent,
                                        parent.langpack.getString ("UserInputPanel.search.autodetect.failed.message"),
                                        parent.langpack.getString ("UserInputPanel.search.autodetect.failed.caption"),
                                        JOptionPane.WARNING_MESSAGE);
     }
     else if (event.getSource () == this.browseButton)
     {
       JFileChooser chooser = new JFileChooser ();
 
       if (this.searchType == TYPE_DIRECTORY)
         chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
 
       int result = chooser.showOpenDialog (this.parent);
 
       if (result == JFileChooser.APPROVE_OPTION)
       {
         File f = chooser.getSelectedFile();
 
         this.pathComboBox.setSelectedItem (f.getAbsolutePath());
         
         // use any given directory directly
         if (! this.pathMatches(f.getAbsolutePath()))
         {
           JOptionPane.showMessageDialog (parent,
                                          parent.langpack.getString ("UserInputPanel.search.wrongselection.message"),
                                          parent.langpack.getString ("UserInputPanel.search.wrongselection.caption"),
                                          JOptionPane.WARNING_MESSAGE);          
         }
       }
 
     }
 
     // we don't care for anything more here - getResult() does the rest
   }
 
  /*--------------------------------------------------------------------------*/
  /**
   * Return the result of the search according to result type.
   *
   * Sometimes, the whole path of the file is wanted, sometimes only the
   * directory where the file is in, sometimes the parent directory.
   *
   * @return null on error
   */
  /*--------------------------------------------------------------------------*/
   public String getResult ()
   {
     String item = (String)this.pathComboBox.getSelectedItem ();
     if (item != null) item.trim();
     String path = item;
     
     File f = new File (item);
 
     if (! f.isDirectory ())
     {
       path = f.getParent ();
     }
     
 
     // path now contains the final content of the combo box
     if (this.resultType == RESULT_DIRECTORY)
     {
       return path;
     }
     else if (this.resultType == RESULT_FILE)
     {
       if (this.filename != null)
       {
         return path + File.separatorChar + this.filename;
       }
       else
       {
        return item;
       }
     }
     else if (this.resultType == RESULT_PARENTDIR)
     {
       File dir = new File (path);
       return dir.getParent (); 
     }
 
     return null;
   }
 
 } // private class SearchFile
 
 } // public class UserInputPanel
 /*---------------------------------------------------------------------------*/
