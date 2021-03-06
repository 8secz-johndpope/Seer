 package org.geworkbench.builtin.projects;
 
 import java.awt.BorderLayout;
 import java.awt.Color;
 import java.awt.Dimension;
 import java.awt.FlowLayout;
 import java.awt.GridBagConstraints;
 import java.awt.GridBagLayout;
 import java.awt.GridLayout;
 import java.awt.Insets;
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.io.BufferedReader;
 import java.io.BufferedWriter;
 import java.io.File;
 import java.io.FileReader;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.util.Arrays;
 import java.util.Comparator;
 
 import javax.swing.Box;
 import javax.swing.BoxLayout;
 import javax.swing.ButtonGroup;
 import javax.swing.DefaultComboBoxModel;
 import javax.swing.JButton;
 import javax.swing.JCheckBox;
 import javax.swing.JComboBox;
 import javax.swing.JDialog;
 import javax.swing.JFileChooser;
 import javax.swing.JLabel;
 import javax.swing.JOptionPane;
 import javax.swing.JPanel;
 import javax.swing.JRadioButton;
 import javax.swing.JTabbedPane;
 import javax.swing.JTextArea;
 import javax.swing.JTextField;
 import javax.swing.filechooser.FileFilter;
 
 import org.geworkbench.builtin.projects.remoteresources.RemoteResourceDialog;
 import org.geworkbench.builtin.projects.remoteresources.query.CaARRAYQueryPanel;
 import org.geworkbench.builtin.projects.util.CaARRAYPanel;
 import org.geworkbench.components.parsers.FileFormat;
 import org.geworkbench.engine.management.ComponentRegistry;
 import org.geworkbench.events.CaArrayEvent;
 import org.geworkbench.events.CaArrayQueryEvent;
 import org.geworkbench.events.CaArrayQueryResultEvent;
 import org.geworkbench.events.CaArrayRequestEvent;
import org.geworkbench.events.CaArraySuccessEvent;
 
 /**
  *  Popup to select a file (local or remote) to open.
  *  
  * @author First Genetic Trust Inc.
 * @version $Id: LoadData.java,v 1.40 2009-05-15 20:11:51 chiangy Exp $
  */
 public class LoadData extends JDialog {
 	private static final long serialVersionUID = -1983039293757013174L;
 	
 	private BorderLayout borderLayout1 = new BorderLayout();
 	private JPanel jPanel1 = new JPanel();
 	private GridLayout gridLayout1 = new GridLayout();
 	private JPanel jPanel2 = new JPanel();
 	private JPanel jPanel3 = new JPanel();
 	private FlowLayout flowLayout1 = new FlowLayout();
 	private FlowLayout flowLayout2 = new FlowLayout();
 	private JRadioButton jRadioButton1 = new JRadioButton();
 	private JRadioButton jRadioButton2 = new JRadioButton();
 	private JPanel jPanel4 = new JPanel();
 	private BorderLayout borderLayout2 = new BorderLayout();
 	private JFileChooser jFileChooser1 = new JFileChooser();
 	private JPanel jPanel5 = new JPanel();
 	private ButtonGroup buttonGroup1 = new ButtonGroup();
 	private ButtonGroup buttonGroup2 = new ButtonGroup();
 	private ButtonGroup buttonGroup3 = new ButtonGroup();
 	private JLabel jLabel3 = new JLabel();
 	private JPanel jPanel9 = new JPanel();
 	private JPanel jPanel11 = new JPanel();
 	private JPanel jPanel12 = new JPanel();
 	private JRadioButton jRadioButton6 = new JRadioButton();
 	private JLabel jLabel2 = new JLabel();
 	private JRadioButton jRadioButton5 = new JRadioButton();
 	private GridLayout gridLayout3 = new GridLayout();
 	private JRadioButton jRadioButton4 = new JRadioButton();
 	private JRadioButton jRadioButton3 = new JRadioButton();
 	private JLabel jLabel1 = new JLabel();
 	private GridLayout gridLayout4 = new GridLayout();
 	private GridBagLayout gridBagLayout1 = new GridBagLayout();
 	private JLabel jLabel4 = new JLabel();
 	private JTextArea experimentInfoArea = new JTextArea();
 	private GridLayout gridLayout2 = new GridLayout();
 	private GridLayout grid4 = new GridLayout();
 	private String format = null;
 
 	private CaARRAYPanel caArrayDisplayPanel = new CaARRAYPanel(this);
 	private JCheckBox mergeCheckBox;
 	private JPanel lowerPanel;
 	private JPanel mergePanel;
 	private RemoteResourceDialog remoteResourceDialog;
 
 	private JTabbedPane lowTabPane;
 
 	private String indexURL = "";
 	private JTextField indexField;
 	private JButton updateIndexButton;
 	private String currentRemoteResourceName;
 	private String currentDetailedResourceName; // current resource name which
 												// shows detail at the top
 												// panel.
 	private CaARRAYQueryPanel caARRAYQueryPanel;
 
 	/**
 	 * The project panel that manages the dialog box.
 	 */
 	public ProjectPanel parentProjectPanel = null;
 
 	/**
 	 * Stores the <code>FileFormat</code> objects for the supported input
 	 * formats.
 	 */
 	private FileFormat[] supportedInputFormats = null;
 	BorderLayout borderLayout7 = new BorderLayout();
 	JPanel jPanel7 = new JPanel();
 	JPanel gridButtonPanel = new JPanel();
 	JRadioButton jRadioButton7 = new JRadioButton();
 	JRadioButton jRadioButton8 = new JRadioButton();
 	JRadioButton gridButton = new JRadioButton();
 	JPanel jPanel10 = new JPanel();
 
 	String[] DEFAULTRESOUCES = new String[] { "caARRAY", "GEDP" };
 	DefaultComboBoxModel resourceModel = new DefaultComboBoxModel(
 			DEFAULTRESOUCES);
 	JComboBox jComboBox1 = new JComboBox(resourceModel);
 
 	JButton addButton = new JButton();
 	JButton queryButton = new JButton();
 	JButton editButton = new JButton();
 	JButton deleteButton = new JButton();
 	BoxLayout boxLayout21;
 	JButton openRemoteResourceButton = new JButton();
 	private boolean isSynchronized = false;
 	private boolean merge;
 	private static final String QUERYTITLE = "Query the caARRAY Server.";
 
 	public LoadData(ProjectPanel parent) {
 		parentProjectPanel = parent;
 		indexURL = System.getProperties().getProperty("globus.url")
 				+ System.getProperties().getProperty("caarray.indexservice");
 
 		try {
 			jbInit();
 		} catch (Exception e) {
 			e.printStackTrace();
 		}
 	}
 
 	public LoadData() {
 		try {
 			jbInit();
 		} catch (Exception e) {
 			e.printStackTrace();
 		}
 	}
 
 	public void receive(CaArrayEvent ce) {
 		caArrayDisplayPanel.receive(ce);
 	}
 
	public void receive(CaArraySuccessEvent ce) {
		caArrayDisplayPanel.receive(ce);
	}

 	public void receive(CaArrayQueryResultEvent ce) {
 		caARRAYQueryPanel.receiveCaAraryQueryResultEvent(ce);
 	}
 
 	public void setCaARRAYServer(String url, int portnumber) {
 		caArrayDisplayPanel.setUrl(url + ":" + portnumber);
 	}
 
 	public void setDirectory(String directory) {
 		jFileChooser1.setCurrentDirectory(new File(directory));
 	}
 
 	public void setFormat(String format) {
 		this.format = format;
 	}
 
 	public void setMerge(boolean merge) {
 		this.merge = merge;
 	}
 
 	public CaARRAYPanel getCaArrayDisplayPanel() {
 		return caArrayDisplayPanel;
 	}
 
 	private void jbInit() throws Exception {
 		this.setModal(false);
 		this.getContentPane().setLayout(borderLayout1);
 
 		jPanel1.setLayout(gridLayout1);
 		jPanel2.setLayout(flowLayout1);
 		jPanel3.setLayout(flowLayout2);
 		jRadioButton1.setText("Local File");
 		jRadioButton2.setText("GEDP");
 		jPanel4.setLayout(borderLayout2);
 		jLabel3.setFont(new java.awt.Font("Dialog", 1, 11));
 		jLabel3.setText("Select column to use");
 		grid4.setColumns(2);
 		grid4.setHgap(10);
 		grid4.setRows(1);
 		grid4.setVgap(10);
 		jPanel5.setLayout(gridBagLayout1);
 		jRadioButton6.setDebugGraphicsOptions(0);
 		jRadioButton6.setText("Mean");
 		jRadioButton6.setSelected(true);
 		jLabel2.setText("GenePix");
 		jRadioButton5.setText("Median");
 		jPanel12.setLayout(gridLayout3);
 		gridLayout3.setColumns(1);
 		gridLayout3.setRows(3);
 		jRadioButton4.setText("Signal");
 		jRadioButton4.setSelected(true);
 		jRadioButton3.setText("Log Average");
 		jLabel1.setText("Affymetrix");
 		jPanel11.setLayout(gridLayout4);
 		gridLayout4.setColumns(1);
 		gridLayout4.setRows(3);
 		jPanel9.setLayout(gridLayout2);
 		jLabel4.setMaximumSize(new Dimension(200, 15));
 		jLabel4.setMinimumSize(new Dimension(200, 15));
 		jLabel4.setPreferredSize(new Dimension(200, 15));
 		jLabel4.setText("Experiment Information:");
 		experimentInfoArea.setPreferredSize(new Dimension(300, 300));
 		experimentInfoArea.setText("");
 		experimentInfoArea.setEditable(false);
 		experimentInfoArea.setLineWrap(true);
 		experimentInfoArea.setWrapStyleWord(true);
 		gridLayout2.setColumns(1);
 		this.setResizable(false);
 		jRadioButton7.setText("caARRAY");
 		lowerPanel = new JPanel();
 		lowerPanel.setLayout(new BoxLayout(lowerPanel, BoxLayout.Y_AXIS));
 		remoteResourceDialog = new RemoteResourceDialog();
 		addButton.setToolTipText("Add a new resource");
 		addButton.setText("Add A New Resource");
 		addButton.addActionListener(new ActionListener() {
 			public void actionPerformed(ActionEvent e) {
 				displayRemoteResourceDialog(RemoteResourceDialog.ADD);
 
 			}
 
 		});
 
 		caARRAYQueryPanel = new CaARRAYQueryPanel(JOptionPane
 				.getFrameForComponent(this), QUERYTITLE);
 		queryButton.setToolTipText("Filtering the selections.");
 		queryButton.setText("Filtering");
 		queryButton.addActionListener(new ActionListener() {
 			public void actionPerformed(ActionEvent e) {
 				displayQueryDialog(RemoteResourceDialog.ADD);
 
 			}
 
 		});
 
 		editButton.setToolTipText("Edit an existed source");
 		editButton.setText("Edit");
 		editButton.addActionListener(new ActionListener() {
 			public void actionPerformed(ActionEvent e) {
 				currentRemoteResourceName = jComboBox1.getSelectedItem()
 						.toString();
 				displayRemoteResourceDialog(currentRemoteResourceName,
 						RemoteResourceDialog.EDIT);
 
 			}
 		});
 
 		jComboBox1.addActionListener(new ActionListener() {
 			public void actionPerformed(ActionEvent e) {
 				updateCurrentView();
 
 			}
 		});
 
 		deleteButton.setToolTipText("Delete an existed resource");
 		deleteButton.setText("Delete");
 		deleteButton.addActionListener(new ActionListener() {
 			public void actionPerformed(ActionEvent e) {
 				deleteButton_actionPerformed(e);
 			}
 
 		});
 		boxLayout21 = new BoxLayout(jPanel10, BoxLayout.X_AXIS);
 		jPanel10.setLayout(boxLayout21);
 		openRemoteResourceButton.setText("Go");
 		openRemoteResourceButton.addActionListener(new ActionListener() {
 			public void actionPerformed(ActionEvent e) {
 				openRemoteResourceButton_actionPerformed(e);
 			}
 		});
 		jComboBox1.setPreferredSize(new Dimension(50, 19));
 
 		mergePanel = new JPanel();
 		mergePanel.setLayout(new BoxLayout(mergePanel, BoxLayout.X_AXIS));
 		mergeCheckBox = new JCheckBox("Merge Files", false);
 		mergeCheckBox.addActionListener(new ActionListener() {
 			public void actionPerformed(ActionEvent e) {
 				merge = mergeCheckBox.isSelected();
 			}
 		});
 
 		jPanel10.add(jComboBox1);
 		jPanel10.add(openRemoteResourceButton);
 		jPanel10.add(queryButton);
 		jPanel10.add(addButton);
 		jPanel10.add(editButton);
 		jPanel10.add(deleteButton);
 
 		mergePanel.add(mergeCheckBox);
 		mergePanel.add(Box.createGlue());
 		lowerPanel.add(jPanel1);
 
 		indexField = new JTextField(indexURL);
 		updateIndexButton = new JButton("Update");
 
 		updateIndexButton.addActionListener(new ActionListener() {
 			public void actionPerformed(ActionEvent e) {
 
 				String previousIndexURL = indexURL;
 				if (remoteResourceDialog.updateResource(indexField.getText())) {
 					lowTabPane.setSelectedIndex(0);
 					jRadioButton8.setSelected(true);
 					updateExistedResourcesGUI();
 					addRemotePanel_actionPerformed(e);
 
 				} else {
 					indexField.setText(previousIndexURL);
 				}
 			}
 		});
 
 		this.getContentPane().add(lowerPanel, BorderLayout.SOUTH);
 
 		jPanel1.add(mergePanel);
 		jPanel1.add(jPanel2, null);
 		jPanel2.add(jRadioButton1, null);
 
 		jPanel1.add(jPanel7, null);
 		// remove grid radio button for geWorkbench 1.6 release
 		//jPanel1.add(gridButtonPanel, null);
 		jRadioButton8.setText("Remote");
 		gridButton.setText("Grid");
 		jPanel7.add(jRadioButton8, null);
 		gridButtonPanel.add(gridButton);
 
 		// XQ modification ends here.
 		this.getContentPane().add(jPanel4, BorderLayout.CENTER);
 		jPanel4.add(jFileChooser1, BorderLayout.CENTER);
 		jPanel5.add(jLabel3, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
 				GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,
 						0, 0, 0), 0, 17));
 		jPanel5.add(jPanel12, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,
 				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
 				new Insets(0, 0, 23, 0), 27, 30));
 		jPanel12.add(jLabel2, null);
 		jPanel12.add(jRadioButton6, null);
 		jPanel12.add(jRadioButton5, null);
 		jPanel5.add(jPanel11, new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0,
 				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
 				new Insets(0, 0, 86, 0), 27, 30));
 		jPanel11.add(jLabel1, null);
 		jPanel11.add(jRadioButton4, null);
 		jPanel11.add(jRadioButton3, null);
 		buttonGroup1.add(jRadioButton1);
 		buttonGroup1.add(jRadioButton2);
 		buttonGroup1.add(jRadioButton7);
 		buttonGroup1.add(jRadioButton8);
 		buttonGroup1.add(gridButton);
 		buttonGroup2.add(jRadioButton5);
 		buttonGroup2.add(jRadioButton6);
 		buttonGroup3.add(jRadioButton3);
 		buttonGroup3.add(jRadioButton4);
 		jPanel9.setPreferredSize(new Dimension(200, 40));
 		jRadioButton1.setSelected(true);
 		jRadioButton1.addActionListener(new ActionListener() {
 			public void actionPerformed(ActionEvent e) {
 				jRadioButton2_actionPerformed(e);
 			}
 		});
 		jRadioButton2.addActionListener(new ActionListener() {
 			public void actionPerformed(ActionEvent e) {
 				remoteButtonSelection_actionPerformed(e);
 			}
 		});
 		jRadioButton7.addActionListener(new ActionListener() {
 			public void actionPerformed(ActionEvent e) {
 				mageButtonSelection_actionPerformed(e);
 			}
 		});
 		jRadioButton8.addActionListener(new ActionListener() {
 			public void actionPerformed(ActionEvent e) {
 				addRemotePanel_actionPerformed(e);
 			}
 		});
 		gridButton.addActionListener(new ActionListener() {
 			public void actionPerformed(ActionEvent e) {
 				addGridPanel_actionPerformed(e);
 			}
 		});
 		jFileChooser1.addActionListener(new ActionListener() {
 			public void actionPerformed(ActionEvent e) {
 				jFileChooser1_actionPerformed(e);
 			}
 		});
 		this.getContentPane().setSize(new Dimension(683, 450));
 		this.setTitle("Open File");
 		this.updateExistedResourcesGUI();
 		pack();
 	}
 
 	/**
 	 * Queries the <code>PluginRegistry</code> for all supported file formats
 	 * and sets the file chooser options accordingly.
 	 */
 	public void setupInputFormats() {
 		int i;
 		// Get the supported formats from the registry.
 		/**
 		 * PluginDescriptor[] inputFormats =
 		 * PluginRegistry.getPluginsAtExtension("input-format"); if
 		 * (inputFormats != null) { supportedInputFormats = new
 		 * FileFormat[inputFormats.length]; for (i = 0; i < inputFormats.length;
 		 * ++i) { supportedInputFormats[i] = (FileFormat)
 		 * inputFormats[i].getPlugin(); } }
 		 */
 		supportedInputFormats = ComponentRegistry.getRegistry().getModules(
 				org.geworkbench.components.parsers.FileFormat.class);
 		Arrays.sort(supportedInputFormats, new FileFormatComparator());
 
 		// Setup the file chooser options.
 		jFileChooser1.resetChoosableFileFilters();
 		jFileChooser1.setAcceptAllFileFilterUsed(false);
 		jFileChooser1.setMultiSelectionEnabled(true);
 		for (i = 0; i < supportedInputFormats.length; ++i) {
 			jFileChooser1.addChoosableFileFilter(supportedInputFormats[i]
 					.getFileFilter());
 		}
 
 		int idx = 0;
 		if ((format != null)
 				&& (!format.equals(""))
 				&& ((idx = Integer.parseInt(format)) < supportedInputFormats.length)) {
 			jFileChooser1.setFileFilter(supportedInputFormats[idx]
 					.getFileFilter());
 		}
 	}
 
 	/**
 	 * Comparator for sorting file format based on their names.
 	 * @author zji
 	 *
 	 */
 	private static class FileFormatComparator implements Comparator<FileFormat> {
 
 		public int compare(FileFormat o1, FileFormat o2) {
 			return (o1.getFormatName().compareToIgnoreCase(o2.getFormatName()));
 		}
 		
 	}
 	
 	/**
 	 * displayRemoteResourceDiolog for add a new resouce.
 	 * 
 	 * @param option
 	 *            int
 	 */
 	void displayRemoteResourceDialog(int option) {
 		RemoteResourceDialog.setPreviousResourceName((String) jComboBox1
 				.getSelectedItem());
 		RemoteResourceDialog.showDialog(this, null, option, null);
 		updateExistedResourcesGUI();
 	}
 
 	/**
 	 * Show filtering options.
 	 * 
 	 * @param option
 	 *            int
 	 */
 	void displayQueryDialog(int option) {
 
 		String remoteSourceName = (String) jComboBox1.getSelectedItem();
 		RemoteResourceDialog.setPreviousResourceName(remoteSourceName);
 		if (caARRAYQueryPanel == null) {
 			caARRAYQueryPanel = new CaARRAYQueryPanel(JOptionPane
 					.getFrameForComponent(this), QUERYTITLE);
 		}
 		caARRAYQueryPanel.display(this, remoteSourceName);
 	}
 
 	/**
 	 * displayRemoteResourceDiolog for edit/delete an existed resource.
 	 * 
 	 * @param shortname
 	 *            Object
 	 * @param option
 	 *            int
 	 */
 	void displayRemoteResourceDialog(Object shortname, int option) {
 		if (shortname != null) {
			if (RemoteResourceDialog.showDialog(this, null, option, shortname
					.toString()))
				updateExistedResourcesGUI();
 		} else {
			if (RemoteResourceDialog.showDialog(this, null, option, null))
				updateExistedResourcesGUI();
 		}
 	}
 
 	/**
 	 * updateExistedResources, should be called after any button related to
 	 * remoteresource is clicked.
 	 */
 	private void updateExistedResourcesGUI() {
 		String[] resources = RemoteResourceDialog.getResourceNames();
 		if (resources != null) {
 			resourceModel.removeAllElements();
 			for (String s : resources) {
 				if (resourceModel.getIndexOf(s) == -1) {
 					resourceModel.addElement(s);
 				}
 			}
 			if (remoteResourceDialog.getCurrentResourceName() != null
 					&& !remoteResourceDialog.getCurrentResourceName()
 							.equals("")) {
 				jComboBox1.setSelectedItem(remoteResourceDialog
 						.getCurrentResourceName());
 				updateCurrentView();
 			}
 		}
 
 	}
 
 	public void publishProjectNodeAddedEvent(
 			org.geworkbench.events.ProjectNodeAddedEvent event) {
 		parentProjectPanel.publishProjectNodeAddedEvent(event);
 	}
 
 	public void publishCaArrayRequestEvent(CaArrayRequestEvent event) {
 		parentProjectPanel.publishCaArrayRequestEvent(event);
 	}
 
 	public void publishCaArrayQueryEvent(CaArrayQueryEvent event) {
 		parentProjectPanel.publishCaArrayQueryEvent(event);
 	}
 
 	/**
 	 * deleteButton_actionPerformed, remove the selected resource after
 	 * confirmation.
 	 * 
 	 * @param e
 	 *            ActionEvent
 	 */
 	public void deleteButton_actionPerformed(ActionEvent e) {
 		String deleteResourceStr = (String) jComboBox1.getSelectedItem();
 		if (deleteResourceStr != null) {
 			int choice = JOptionPane.showConfirmDialog(null,
 					"Do you really want to remove the remote source: "
 							+ deleteResourceStr + "?", "Warning",
 					JOptionPane.OK_CANCEL_OPTION);
 			if (choice != 2) {
 				resourceModel.removeElement(deleteResourceStr);
 				remoteResourceDialog.removeResourceByName(deleteResourceStr);
 				repaint();
 			}
 
 		}
 		updateExistedResourcesGUI();
 
 	}
 
 	/**
 	 * Responds to the selection of an input file by the user
 	 * 
 	 * @param e
 	 */
 	private void jFileChooser1_actionPerformed(ActionEvent e) {
 		int i;
 		if (e.getActionCommand() == JFileChooser.APPROVE_SELECTION) {
 			// Get the format that the user designated for the selected file.
 			FileFilter selectedFilter = jFileChooser1.getFileFilter();
 			if (selectedFilter == null) {
 				return; // Just to cover the case where no plugins are defined.
 			}
 			File[] files = jFileChooser1.getSelectedFiles();
 			for (i = 0; i < supportedInputFormats.length; ++i) {
 				if (selectedFilter == supportedInputFormats[i].getFileFilter()) {
 					try {
 						String format = i + "\n";
 						String filepath = null;
 						filepath = jFileChooser1.getCurrentDirectory()
 								.getCanonicalPath();
 						setLastDataInfo(filepath, format);
 						// Delegates the actual file loading to the project
 						// panel
 						parentProjectPanel.fileOpenAction(files,
 								supportedInputFormats[i], mergeCheckBox
 										.isSelected());
 						dispose();
 						return;
 					} catch (org.geworkbench.components.parsers.InputFileFormatException iffe) {
 						// Let the user know that there was a problem parsing
 						// the file.
 						JOptionPane
 								.showMessageDialog(
 										null,
 										"The input file does not comply with the designated format.",
 										"Parsing Error",
 										JOptionPane.ERROR_MESSAGE);
 					} catch (IOException ex) {
 					}
 				}
 			}
 		} else if (e.getActionCommand() == JFileChooser.CANCEL_SELECTION) {
 			dispose();
 		}
 	}
 
 	/**
 	 * Responds to the user selection to see the remote files.
 	 * 
 	 * @param e
 	 */
 	private void remoteButtonSelection_actionPerformed(ActionEvent e) {
 		// jPanel6.getExperiments(e);
 		this.getContentPane().remove(caArrayDisplayPanel);
 		this.getContentPane().remove(jPanel4);
 		// this.getContentPane().add(jPanel6, BorderLayout.CENTER);
 		this.validate();
 		this.repaint();
 	}
 
 	private void updateCurrentView() {
 		if (currentRemoteResourceName != null
 				&& jComboBox1.getSelectedItem() != null
 				&& currentRemoteResourceName != jComboBox1.getSelectedItem()
 						.toString()) {
 
 			currentRemoteResourceName = jComboBox1.getSelectedItem().toString();
 
 		} else {
 			if (jComboBox1.getSelectedItem() != null)
 				currentRemoteResourceName = jComboBox1.getSelectedItem()
 						.toString();
 		}
 		remoteResourceDialog
 				.setupSystemPropertyForCurrentResource(currentRemoteResourceName);
 		isSynchronized = !remoteResourceDialog.isSourceDirty()
 				&& (currentDetailedResourceName
 						.equalsIgnoreCase(currentRemoteResourceName));
 		if (!isSynchronized) {
 			openRemoteResourceButton.setBackground(Color.RED);
 			openRemoteResourceButton
 					.setToolTipText("Click to get the display synchronized with Remote source.");
 			//Added by XQ to fix bug 1288
 			caArrayDisplayPanel.setExperiments(null);
 			caArrayDisplayPanel.setExperimentsLoaded(false);
 		} else {
 			openRemoteResourceButton.setBackground(null);
 			openRemoteResourceButton
 					.setToolTipText("The top display is synchronized with the Remote source.");
 		}
 
 	}
 
 	private void mageButtonSelection_actionPerformed(ActionEvent e) {
 		String currentResourceName = resourceModel.getSelectedItem().toString()
 				.trim();
 		currentDetailedResourceName = currentResourceName;
 		remoteResourceDialog
 				.setupSystemPropertyForCurrentResource(currentResourceName);
 		remoteResourceDialog.updateCurrentResourceStatus(currentResourceName,
 				false);
 		caArrayDisplayPanel.setUrl(remoteResourceDialog.getCurrentURL());
 		caArrayDisplayPanel.setUser(remoteResourceDialog.getCurrentUser());
 		caArrayDisplayPanel
 				.setPasswd(remoteResourceDialog.getCurrentPassword());
 		caArrayDisplayPanel.setPortnumber(remoteResourceDialog
 				.getCurrentPortnumber());
 		caArrayDisplayPanel.setParentPanel(this);
 		caArrayDisplayPanel.setCurrentResourceName(currentResourceName);
 		// XZ, 4/4. The user name/password can be null now for caArray2.0.1.
 
 		if (!isSynchronized) {
 			caArrayDisplayPanel.setExperiments(null);
 			caArrayDisplayPanel.setExperimentsLoaded(false);
 			caArrayDisplayPanel.getExperiments(e);
 		} else {
 			 
 			int choice = JOptionPane.showConfirmDialog(null, "You just connected to the server. Connect again?");
 			if(JOptionPane.YES_OPTION==choice){
 				caArrayDisplayPanel.setExperiments(null);
 				caArrayDisplayPanel.setExperimentsLoaded(false);
 				caArrayDisplayPanel.getExperiments(e);	
 			} 
 		}
 		// Reset resource dirty to false after the connection to the server.
 		remoteResourceDialog.setSourceDirty(false);
 		updateCurrentView();
 	
 	}
 
 	public void addRemotePanel() {
 		// if (caArrayDisplayPanel.isConnectionSuccess()) {
 		updateCurrentView();
 		// this.getContentPane().remove(jPanel6);
 		this.getContentPane().remove(jPanel4);
 		this.getContentPane().add(caArrayDisplayPanel, BorderLayout.CENTER);
 		this.validate();
 		this.repaint();
 		// }
 
 	}
 
 	private void addRemotePanel_actionPerformed(ActionEvent e) {
 		addRemotePanel();
 		lowerPanel.add(jPanel10);
 		this.validate();
 		this.repaint();
 	}
 
 	private void addGridPanel_actionPerformed(ActionEvent e) {
 		addRemotePanel();
 		lowerPanel.add(jPanel10);
 		this.validate();
 		this.repaint();
 	}
 
 	private void jRadioButton2_actionPerformed(ActionEvent e) {
 		this.getContentPane().remove(caArrayDisplayPanel);
 		// this.getContentPane().remove(jPanel6);
 		this.getContentPane().add(jPanel4, BorderLayout.CENTER);
 		lowerPanel.remove(jPanel10);
 		this.validate();
 		this.repaint();
 	}
 
 	/**
 	 * Returns the experiment information for the most recently selected remote
 	 * file.
 	 * 
 	 * @return
 	 */
 	public String getExperimentInformation() {
 		String expInfo = experimentInfoArea.getText();
 		return (expInfo == null ? "" : expInfo);
 	}
 
 	static public String getLastDataDirectory() {
 		String dir = System.getProperty("data.files.dir");
 		// This is where we store user data information
 		String filename = System.getProperty("userSettings");
 		try {
 			File file = new File(filename);
 			if (file.exists()) {
 				BufferedReader br = new BufferedReader(new FileReader(file));
 				br.readLine(); // skip the format information
 				dir = br.readLine();
 				br.close();
 			}
 		} catch (IOException ex) {
 			ex.printStackTrace();
 		}
 		if (dir.equals(".")) {
 			dir = System.getProperty("user.dir");
 		}
 		return dir;
 	}
 
 	static public String getLastDataFormat() {
 		String format = "";
 		// This is where we store user data information
 		String filename = System.getProperty("userSettings");
 		try {
 			File file = new File(filename);
 			if (file.exists()) {
 				BufferedReader br = new BufferedReader(new FileReader(file));
 				format = br.readLine();
 				br.close();
 			}
 		} catch (IOException ex) {
 			ex.printStackTrace();
 		}
 		return format;
 	}
 
 	static public void setLastDataInfo(String dir, String format) {
 		try { // save current settings.
 			BufferedWriter br = new BufferedWriter(new FileWriter(System
 					.getProperty("userSettings")));
 			br.write(format);
 			br.write(dir);
 			br.close();
 		} catch (IOException ex) {
 			ex.printStackTrace();
 		}
 	}
 
 	public void openRemoteResourceButton_actionPerformed(ActionEvent e) {
 		mageButtonSelection_actionPerformed(e);
 	}
 
 	public boolean isMerge() {
 		return merge;
 	}
 }
