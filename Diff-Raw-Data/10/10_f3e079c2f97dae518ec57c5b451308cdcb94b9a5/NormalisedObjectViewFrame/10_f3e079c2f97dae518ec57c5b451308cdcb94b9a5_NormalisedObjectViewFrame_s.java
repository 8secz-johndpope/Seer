 /*
  * Created on 29/11/2005
  * justinw5
  * 
  */
 package au.gov.naa.digipres.xena.viewer;
 
 import java.awt.BorderLayout;
 import java.awt.FlowLayout;
 import java.awt.event.ItemEvent;
 import java.awt.event.ItemListener;
 import java.awt.event.WindowAdapter;
 import java.awt.event.WindowEvent;
 import java.io.File;
 import java.util.Iterator;
 import java.util.List;
 
 import javax.swing.DefaultComboBoxModel;
 import javax.swing.JComboBox;
 import javax.swing.JFrame;
 import javax.swing.JOptionPane;
 import javax.swing.JPanel;
 import javax.swing.JToolBar;
 import javax.swing.border.EtchedBorder;
 
 import au.gov.naa.digipres.xena.core.NormalisedObjectViewFactory;
 import au.gov.naa.digipres.xena.core.Xena;
 import au.gov.naa.digipres.xena.kernel.IconFactory;
 import au.gov.naa.digipres.xena.kernel.XenaException;
 import au.gov.naa.digipres.xena.kernel.view.ViewManager;
 import au.gov.naa.digipres.xena.kernel.view.XenaView;
 
 /**
  * Simple frame to display a Normalised Object View, a JPanel
  * which has been returned by NormalisedOjectViewFactory.
  * 
  * The frame's toolbar will contain a select box which will 
  * enable the user to change the type of view, e.g. Package, 
  * raw XML, tree XML etc.
  * 
  * @author justinw5
  * created 29/11/2005
  * xena
  * Short desc of class: frame to display Normalised Object Views
  */
 public class NormalisedObjectViewFrame extends JFrame
 {
 	public static final int DEFAULT_WIDTH = 800;
 	public static final int DEFAULT_HEIGHT = 600;
 	
 	private ViewManager viewManager;
 	JComboBox viewTypeCombo;
 	DefaultComboBoxModel viewTypeModel = null;
 	private XenaView currentDisplayView;
 	private File xenaFile;
 	NormalisedObjectViewFactory novFactory;
 	JPanel xenaViewPanel;
 	
 	
 	/**
 	 * Create a new NormalisedObjectViewFrame
 	 * 
 	 * @param xenaView view to display in the frame
 	 * @param xena Xena interface object
 	 * @param xenaFile original xena File
 	 */
 	public NormalisedObjectViewFrame(XenaView xenaView,
 			ViewManager viewManager,
 			File xenaFile) 
 	{
 		super();
 
 		this.xenaFile = xenaFile;
 		this.viewManager = viewManager;
 		novFactory = new NormalisedObjectViewFactory(viewManager);
 		try
 		{
 			initFrame();
 			setupTypeComboBox(xenaView);
 			displayXenaView(xenaView);
 			currentDisplayView = xenaView;			
 		}
 		catch (XenaException e)
 		{
 			handleXenaException(e);
 		}
 	}
 	
 	/**
 	 * Create a new NormalisedObjectViewFrame
 	 * 
 	 * @param xenaView view to display in the frame
 	 * @param xena Xena interface object
 	 * @param xenaFile original xena File
 	 */
 	public NormalisedObjectViewFrame(XenaView xenaView,
 			Xena xena,
 			File xenaFile) 
 	{
 		this(xenaView, xena.getPluginManager().getViewManager(), xenaFile);
 	}
 	
 	/**
 	 * One-time initialisation of frame GUI - menu, toolbar and
 	 * event listeners.
 	 * 
 	 * @throws XenaException
 	 */
 	private void initFrame() throws XenaException
 	{
 		this.setIconImage(IconFactory.getIconByName("images/xena-splash.png").getImage());
 		
 		// Setup toolbar
 		JToolBar toolBar = new JToolBar();
 		toolBar.setLayout(new FlowLayout(FlowLayout.LEFT));
 		JPanel toolBarPanel = new JPanel(new BorderLayout());
 		toolBarPanel.setBorder(new EtchedBorder());
 		
 		viewTypeCombo = new JComboBox();
 		
 		viewTypeCombo.addItemListener(new ItemListener(){
 
 			public void itemStateChanged(ItemEvent e)
 			{
 				if (e.getStateChange() == ItemEvent.SELECTED)
 				{
 					// A new view type has been selected
 					try
 					{
 						displayXenaView(currentDisplayView, 
 						                (XenaView)viewTypeModel.getSelectedItem());
 					}
 					catch (XenaException e1)
 					{
 						handleXenaException(e1);
 					}
 				}
 			}
 			
 		});		
 		
 		toolBar.add(viewTypeCombo);
 		toolBarPanel.add(toolBar, BorderLayout.NORTH);
 		this.getContentPane().add(toolBarPanel, BorderLayout.NORTH);	
 				
 		// Panel in which the XenaView will be displayed
 		xenaViewPanel = new JPanel(new BorderLayout());
 		this.getContentPane().add(xenaViewPanel, BorderLayout.CENTER);
 		this.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
 		
 		// Ensure resources are surrendered when window closes
 		this.addWindowListener(new WindowAdapter(){
 
 			@Override
 			public void windowClosing(WindowEvent e)
 			{
 				doCloseWindow();
 			}
 			
 		});
 	}
 	
 	/**
 	 * Displays the given XenaView, by adding the view to the display panel.
 	 * 
 	 * @param concreteView
 	 * @throws XenaException
 	 */
 	private void displayXenaView(XenaView concreteView)
 		throws XenaException
 	{	
 		xenaViewPanel.removeAll();
 		xenaViewPanel.add(concreteView, BorderLayout.CENTER);
 		this.validate();
 		this.setTitle("XenaViewer - " + 
 		              xenaFile.getName() + 
 		              " (" + viewTypeModel.getSelectedItem().toString() + ")");
 		System.gc();
 	}
 
 	/**
 	 * Displays the given view using the new view type. A new XenaView,
 	 * using the new view type, is retrieved from the 
 	 * NormalisedObjectViewFactory.
 	 * 
 	 * @param concreteView
 	 * @param viewType
 	 * @throws XenaException
 	 */
 	private void displayXenaView(XenaView concreteView, XenaView viewType)
 		throws XenaException
 	{	
 				
 		// Need to clone the template view
 		viewType = viewManager.lookup(viewType.getClass(),
 		                              concreteView.getLevel(),
 		                              concreteView.getTopTag());
 		
 		XenaView displayView = novFactory.getView(xenaFile, viewType);
 		displayXenaView(displayView);
 		currentDisplayView = displayView;
 	}
 
 	/**
 	 * Retrieves the list of view types applicable to the given XenaView,
 	 * and displays these options in the combox box.
 	 * 
 	 * @param xenaView
 	 * @throws XenaException
 	 */
 	private void setupTypeComboBox(XenaView xenaView) 
 		throws XenaException
 	{
 		viewTypeModel = new DefaultComboBoxModel();
 	
 		// Get all applicable view types
 		List<XenaView> viewTypes =
 			viewManager.lookup(xenaView.getTopTag(), 0);
 		
 		// Add options to combo box model
 		Iterator<XenaView> iter = viewTypes.iterator();		
 		while (iter.hasNext())
 		{
 			viewTypeModel.addElement(iter.next());
 		}
 		
 		viewTypeCombo.setModel(viewTypeModel);	
 	}
 	
 	/**
 	 * Surrender this window's resources, and close
 	 *
 	 */
 	private void doCloseWindow()
 	{
 		this.setVisible(false);
 		this.dispose();
 		System.gc();		
 	}
 	
 	/**
 	 * Display error messages
 	 * @param xex
 	 */
 	private void handleXenaException (XenaException xex)
 	{
 		xex.printStackTrace();
 		JOptionPane.showMessageDialog(this, 
 		                              xex.getMessage(),
 		                              "Xena Viewer",
 		                              JOptionPane.ERROR_MESSAGE);
 	}
 
 }
