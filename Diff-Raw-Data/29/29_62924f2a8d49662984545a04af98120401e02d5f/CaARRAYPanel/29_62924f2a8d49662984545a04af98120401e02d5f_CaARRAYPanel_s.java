 package org.geworkbench.builtin.projects.util;
 
 import java.awt.BorderLayout;
 import java.awt.Component;
 import java.awt.Dimension;
 import java.awt.GridLayout;
 import java.awt.SystemColor;
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.awt.event.MouseEvent;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Observer;
 import java.util.SortedMap;
 import java.util.TreeMap;
 import java.util.Vector;
 
 import javax.swing.BorderFactory;
 import javax.swing.Box;
 import javax.swing.BoxLayout;
 import javax.swing.JButton;
 import javax.swing.JLabel;
 import javax.swing.JMenuItem;
 import javax.swing.JOptionPane;
 import javax.swing.JPanel;
 import javax.swing.JPopupMenu;
 import javax.swing.JScrollPane;
 import javax.swing.JTextArea;
 import javax.swing.JTextField;
 import javax.swing.JTree;
 import javax.swing.SwingWorker;
 import javax.swing.border.Border;
 import javax.swing.event.TreeSelectionEvent;
 import javax.swing.event.TreeSelectionListener;
 import javax.swing.tree.DefaultMutableTreeNode;
 import javax.swing.tree.DefaultTreeModel;
 import javax.swing.tree.TreePath;
 import javax.swing.tree.TreeSelectionModel;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.geworkbench.builtin.projects.LoadDataDialog;
 import org.geworkbench.builtin.projects.remoteresources.carraydata.CaArray2Experiment;
 import org.geworkbench.engine.config.VisualPlugin;
 import org.geworkbench.engine.management.Publish;
 import org.geworkbench.engine.management.Subscribe;
 import org.geworkbench.events.CaArrayEvent;
 import org.geworkbench.events.CaArrayRequestEvent;
 import org.geworkbench.events.CaArrayRequestHybridizationListEvent;
 import org.geworkbench.events.CaArrayReturnHybridizationListEvent;
 import org.geworkbench.events.CaArraySuccessEvent;
 import org.geworkbench.util.ProgressBar;
 
 /**
  * @author xiaoqing
  * @version $Id$
  */
 public class CaARRAYPanel extends JPanel implements Observer, VisualPlugin {
 	private static final String LOADING_SELECTED_BIOASSAYS_ELAPSED_TIME = "Loading selected bioassays - elapsed time: ";
 
 	private static final long serialVersionUID = -4876378958265466224L;
 
 	private static final String CAARRAY_TITLE = "caARRAY";
 
 	private static Log log = LogFactory.getLog(CaARRAYPanel.class);
 
 	/**
 	 * Used to avoid querying the server for all experiments all the time.
 	 */
 	protected boolean experimentsLoaded = false;
 
 	private String currentResourceName = null;
 	private String previousResourceName = null;
 	private JLabel displayLabel = new JLabel();
 	private JPanel jPanel6 = new JPanel();
 	private GridLayout grid4 = new GridLayout();
 	private JPanel caArrayDetailPanel = new JPanel();
 	private JPanel caArrayTreePanel = new JPanel();
 	private BorderLayout borderLayout4 = new BorderLayout();
 	private BorderLayout borderLayout7 = new BorderLayout();
 	private Border border1 = null;
 	private Border border2 = null;
 	private JScrollPane jScrollPane1 = new JScrollPane();
 	private JPanel jPanel10 = new JPanel();
 	private JLabel jLabel4 = new JLabel();
 	private JTextArea experimentInfoArea = new JTextArea();
 	private JScrollPane jScrollPane2 = new JScrollPane(experimentInfoArea);
 	private JPanel jPanel14 = new JPanel();
 	private JPanel jPanel16 = new JPanel();
 	private JLabel derivedLabel = new JLabel("Number of Assays");
 	private JTextField measuredField = new JTextField();
 	private JTextField derivedField = new JTextField();
 	private JPanel jPanel13 = new JPanel();
 	private JButton extendButton = new JButton();
 	private JButton openButton = new JButton();
 	private JButton cancelButton = new JButton();
 	private LoadDataDialog parent = null;
 	private DefaultMutableTreeNode root = new DefaultMutableTreeNode(
 			"caARRAY experiments");
 	private DefaultTreeModel remoteTreeModel = new DefaultTreeModel(root);
 	private JTree remoteFileTree = new JTree(remoteTreeModel);
 	private JPopupMenu jRemoteDataPopup = new JPopupMenu();
 	private JMenuItem jGetRemoteDataMenu = new JMenuItem();
 	private boolean connectionSuccess = true;
 	private String user;
 	private String passwd;
 	private String url;
 	private int portnumber;
 
 	private ProgressBar progressBar = ProgressBar
 			.create(ProgressBar.INDETERMINATE_TYPE);
 	private LoadDataDialog parentPanel;
 
 	private volatile boolean stillWaitForConnecting = true;
 
 	private transient String lastChosenQuantitationType;
 	private static final int INTERNALTIMEOUTLIMIT = 600;
 	private static final int INCREASE_EACHTIME = 300;
 	private int internalTimeoutLimit = INTERNALTIMEOUTLIMIT;
 
 	public CaARRAYPanel() {
 		try {
 			jbInit();
 		} catch (Exception e) {
 			e.printStackTrace();
 		}
 	}
 
 	public void setParent(LoadDataDialog p) {
 		parent = p;
 	}
 
 	public boolean isStillWaitForConnecting() {
 		return stillWaitForConnecting;
 	}
 
 	public void setStillWaitForConnecting(boolean stillWaitForConnecting) {
 		this.stillWaitForConnecting = stillWaitForConnecting;
 	}
 
 	@Subscribe
 	public void receive(CaArrayEvent ce, Object source) {
 		if (!stillWaitForConnecting) {
 			return;
 		}
 		stillWaitForConnecting = false;
 		progressBar.stop();
 		// ready for the next request
 		setOpenButtonEnabled(true);
 
 		if (!ce.isPopulated()) {
 			String errorMessage = ce.getErrorMessage();
 			if (errorMessage == null) {
 				errorMessage = "Cannot connect with the server.";
 			}
 			if (!ce.isSucceed()) {
 				JOptionPane.showMessageDialog(null,errorMessage);
 				return;
 			} else {
 				JOptionPane.showMessageDialog(null, errorMessage);
 			}
 		}
 
 		if (ce.getInfoType().equalsIgnoreCase(CaArrayEvent.EXPERIMENT)) {
 			CaArray2Experiment[] currentLoadedExps = ce.getExperiments();
 
 			root = new DefaultMutableTreeNode("caARRAY experiments");
 			remoteTreeModel.setRoot(root);
 			if (currentLoadedExps == null) {
 				return;
 			}
 
 			for (int i = 0; i < currentLoadedExps.length; ++i) {
 				DefaultMutableTreeNode node = new DefaultMutableTreeNode(
 						currentLoadedExps[i]);
 				remoteTreeModel
 						.insertNodeInto(node, root, root.getChildCount());
 			}
 			remoteFileTree.expandRow(0);
 			experimentsLoaded = true;
 			previousResourceName = currentResourceName;
 			connectionSuccess = true;
 
 			displayLabel.setText("Total Experiments: "
 					+ currentLoadedExps.length);
 			caArrayTreePanel.add(displayLabel, BorderLayout.SOUTH);
 				
 			revalidate();
 		} else {
 			dispose();// make itself disappear.
 		}
 	}
 
 	@Publish
 	public CaArrayRequestEvent publishCaArrayRequestEvent(
 			CaArrayRequestEvent event) {
 		return event;
 	}
 
 	private void jbInit() throws Exception {
 		border1 = BorderFactory.createLineBorder(SystemColor.controlText, 1);
 		border2 = BorderFactory.createLineBorder(SystemColor.controlText, 1);
 		grid4.setColumns(2);
 		grid4.setHgap(10);
 		grid4.setRows(1);
 		grid4.setVgap(10);
 		jLabel4.setMaximumSize(new Dimension(200, 15));
 		jLabel4.setMinimumSize(new Dimension(200, 15));
 		jLabel4.setPreferredSize(new Dimension(200, 15));
 		jLabel4.setText("Experiment Information:");
 
 		jPanel13.setLayout(new BoxLayout(jPanel13, BoxLayout.X_AXIS));
 		jPanel13.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
 		caArrayTreePanel.setPreferredSize(new Dimension(197, 280));// change
 		// from 137
 		// to 167.
 		caArrayDetailPanel.setPreferredSize(new Dimension(380, 300));
 		jPanel10.setPreferredSize(new Dimension(370, 280));
 		this.setMinimumSize(new Dimension(510, 200));
 		this.setPreferredSize(new Dimension(510, 200));
 		jPanel13.add(extendButton);
 		// jPanel13.add(Box.createHorizontalGlue());
 		jPanel13.add(openButton);
 		// jPanel13.add(Box.createRigidArea(new Dimension(10, 0)));
 		jPanel13.add(cancelButton);
 
 		measuredField.setEditable(false);
 
 		extendButton.setPreferredSize(new Dimension(100, 25));
 		extendButton.setText("Show arrays");
 		extendButton.addActionListener(new ActionListener() {
 			public void actionPerformed(ActionEvent e) {
 				extendBioassays_action(e);
 			}
 		});
 		extendButton.setToolTipText("Display Bioassys");
 		openButton.setPreferredSize(new Dimension(60, 25));
 		openButton.setText("Open");
 		openButton.addActionListener(new ActionListener() {
 			public void actionPerformed(ActionEvent e) {
 				openRemoteFile_action(e);
 			}
 		});
 		openButton.setToolTipText("Load remote MicroarrayDataSet");
 		cancelButton.setMinimumSize(new Dimension(68, 25));
 		cancelButton.setPreferredSize(new Dimension(68, 25));
 		cancelButton.setText("Cancel");
 		cancelButton.setToolTipText("Close the Window");
 		cancelButton.addActionListener(new ActionListener() {
 			public void actionPerformed(ActionEvent e) {
 				jButton1_actionPerformed(e);
 			}
 		});
 
 		jPanel16.setLayout(new BoxLayout(jPanel16, BoxLayout.Y_AXIS));
 		jPanel16.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
 		jPanel16.add(Box.createVerticalGlue());
 		jPanel16.add(Box.createRigidArea(new Dimension(10, 0)));
 		jPanel16.add(derivedLabel);
 		jPanel16.add(derivedField);
 		jPanel16.add(Box.createRigidArea(new Dimension(10, 0)));
 		derivedField.setEditable(false);
 		jScrollPane2.setPreferredSize(new Dimension(300, 500));
 		jPanel14.setLayout(new BoxLayout(jPanel14, BoxLayout.X_AXIS));
 		jPanel14.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
 		jPanel14.add(Box.createHorizontalGlue());
 		jPanel14.add(Box.createRigidArea(new Dimension(10, 0)));
 		jPanel14.add(Box.createRigidArea(new Dimension(10, 0)));
 		experimentInfoArea.setText("");
 		experimentInfoArea.setEditable(false);
 		experimentInfoArea.setLineWrap(true);
 		experimentInfoArea.setWrapStyleWord(true);
 
 		jPanel6.setLayout(grid4);
 		caArrayTreePanel.setLayout(borderLayout4);
 		caArrayDetailPanel.setLayout(borderLayout7);
 		jPanel10.setLayout(new BoxLayout(jPanel10, BoxLayout.Y_AXIS));
 		jPanel10.add(jLabel4, null);
 		jPanel10.add(Box.createRigidArea(new Dimension(0, 10)));
 		jPanel10.add(jScrollPane2, null);
 		jPanel10.add(Box.createRigidArea(new Dimension(0, 10)));
 		jPanel10.add(jPanel14, null);
 		jPanel14.add(jPanel16);
 		jPanel10.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
 
 		caArrayDetailPanel.setBorder(border1);
 		caArrayTreePanel.setBorder(border2);
 		jScrollPane1.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
 		caArrayDetailPanel.add(jPanel10, BorderLayout.CENTER);
 		caArrayDetailPanel.add(jPanel13, BorderLayout.SOUTH);
 		jPanel6.add(caArrayTreePanel);
 		caArrayTreePanel.add(jScrollPane1, BorderLayout.CENTER);
 
 		jScrollPane1.getViewport().add(remoteFileTree, null);
 		jPanel6.add(caArrayDetailPanel);
 
 		remoteFileTree.getSelectionModel().setSelectionMode(
 				TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
 		remoteFileTree.addTreeSelectionListener(new TreeSelectionListener() {
 			public void valueChanged(TreeSelectionEvent tse) {
 				remoteFileSelection_action(tse);
 			}
 		});
 		remoteFileTree.addMouseListener(new java.awt.event.MouseAdapter() {
 			public void mouseReleased(MouseEvent e) {
 				jRemoteFileTree_mouseReleased(e);
 			}
 		});
 		jGetRemoteDataMenu.setText("Show arrays");
 		ActionListener listener = new ActionListener() {
 			public void actionPerformed(ActionEvent e) {
 				extendBioassays_action(e);
 			}
 		};
 		jGetRemoteDataMenu.addActionListener(listener);
 		jRemoteDataPopup.add(jGetRemoteDataMenu);
 		jPanel6.setMaximumSize(new Dimension(500, 300));
 		jPanel6.setMinimumSize(new Dimension(500, 285));
 		jPanel6.setPreferredSize(new Dimension(500, 285));
 		this.setLayout(new BorderLayout());
 		this.add(jPanel6, BorderLayout.CENTER);
 	}
 
 	/**
 	 * Update the progressBar to reflect the current time.
 	 *
 	 * @param text
 	 */
 	public void updateProgressBar(final String text) {
 		SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
 
 			@Override
 			protected Void doInBackground() throws Exception {
 				if (text.startsWith("Loading")) {
 					int i = 0;
 					internalTimeoutLimit = INTERNALTIMEOUTLIMIT;
 					do {
 						Thread.sleep(250);
 						i++;
 						String currentState = "";
 						if (numTotalArrays>1)
 							currentState = "Downloaded " + numCurrentArray + " of "+numTotalArrays + " arrays.";
 						if (i > 4) {
 							String htmltext = "<html>" + text + i / 4 + " seconds." + "<br>" + currentState +"</html>";
 							publish(htmltext);
 
 						}
 					} while (stillWaitForConnecting);
 				}
 				return null;
 			}
 
 			@Override
 			protected void process(List<String> list) {
 				  progressBar.setMessage(list.get(list.size() - 1));
 			}
 		};
 		worker.execute();
 	}
 
 	/**
 	 *
 	 * @param seconds
 	 */
 	private void increaseInternalTimeoutLimitBy(int seconds){
 		log.debug("Due time has been increased from "+internalTimeoutLimit+" seconds to " +(internalTimeoutLimit+seconds)+" seconds.");
 		internalTimeoutLimit += seconds;
 	}
 
 	/**
 	 *
 	 * @param ce
 	 * @param source
 	 */
 	@Subscribe
 	public void receive(CaArraySuccessEvent ce, Object source) {
 		this.numCurrentArray = numCurrentArray+1;
 		this.numTotalArrays = ce.getTotalArrays();
 		increaseInternalTimeoutLimitBy(INCREASE_EACHTIME);
 	}
 
 	private volatile int numTotalArrays = 0;
 	private volatile int numCurrentArray = 0;
 
 	/**
 	 * Action listener invoked when the user presses the "Open" button after
 	 * having selected a remote microarray. The listener will attempt to get the
 	 * microarray data from the remote server and load them in the application.
 	 *
 	 * @param e
 	 */
 	private void openRemoteFile_action(ActionEvent e) {
 		TreePath[] paths = remoteFileTree.getSelectionPaths();
 		if (paths.length <= 0 || paths[0].getPath().length <= 1) {
 			JOptionPane.showMessageDialog(null,
 					"Please select at least one Bioassay to retrieve.");
 			return;
 		}
 		
 		// If there are multiple parents in the paths, it means user tries
 		// to select arrays from different experiments, it's not allowed.
 		HashSet<TreePath> set = new HashSet<TreePath>();
 		for (TreePath treePath : paths) {
 			set.add(treePath.getParentPath());
 		}
 		if (set.size() > 1) {
 			JOptionPane
 					.showMessageDialog(
 							null,
 							"Only datasets from the same experiment can be merged.",
 							"Can't merge datasets",
 							JOptionPane.INFORMATION_MESSAGE);
 			return;
 		}
 		
 		// If there is only one parent, we continue.
 		CaArray2Experiment exp = (CaArray2Experiment) ((DefaultMutableTreeNode) paths[0]
 					.getPath()[1]).getUserObject();
 		final String currentSelectedExperimentName = exp.getName();
 		
 		Map<String, String> hyb = exp.getHybridizations();
 		if(hyb==null || paths[0].getPath().length<=2 ) { // hyb==null means 'before experiment expansion'; length<=2 means not 'a hybridization'
 			return;
 		}
 		final Map<String, String> currentSelectedBioAssay = new HashMap<String, String>();
 		for (int i = 0; i < paths.length; i++) {
 			DefaultMutableTreeNode node = (DefaultMutableTreeNode) paths[i]
 					.getLastPathComponent();
 			String hybName = (String) node.getUserObject();
 			currentSelectedBioAssay.put(hybName, hyb.get(hybName));
 		}
 		final String qType = checkQuantationTypeSelection(exp);
 
 		if (qType == null) {
 			return;
 		}
 
 		numCurrentArray = 0;
 		numTotalArrays = 0;
 		stillWaitForConnecting = true;
 		progressBar.setMessage(LOADING_SELECTED_BIOASSAYS_ELAPSED_TIME);
 		updateProgressBar(LOADING_SELECTED_BIOASSAYS_ELAPSED_TIME);
 		progressBar.addObserver(this);
 		progressBar.setTitle(CAARRAY_TITLE);
 		progressBar.start();
 
 		Runnable dataLoader = new Runnable() {
 			public void run() {
 				getBioAssay(currentSelectedExperimentName,
 						currentSelectedBioAssay, qType);
 			}
 		};
 		Thread t = new Thread(dataLoader);
 		t.setPriority(Thread.MAX_PRIORITY);
 		t.start();
 		// only one request at a time
 		setOpenButtonEnabled(false);
 
 	}
 
 	public void setOpenButtonEnabled(boolean b){
 		openButton.setEnabled(b);
 	}
 
 	public void startProgressBar() {
 		stillWaitForConnecting = true;
 		updateProgressBar("Loading the filtered experiments - elapsed time: ");
 		progressBar.setTitle(CAARRAY_TITLE);
 		progressBar.start();
 
 	}
 
 	/**
 	 * Action listener invoked when the user presses the "Show Bioassays" button
 	 * after having selected a remote microarray. If no node is selected, all
 	 * experiments will be extended with their associated bioassays. Otherwise,
 	 * only current selection will be extended.
 	 *
 	 * @param e
 	 */
 	private void extendBioassays_action(ActionEvent e) {
 
 		DefaultMutableTreeNode node = (DefaultMutableTreeNode) remoteFileTree
 				.getLastSelectedPathComponent();
 		if (node != null && node != root) {
 			if (node.getChildCount() == 0) {
 				CaArray2Experiment caArray2Experiment = (CaArray2Experiment)(node
 						.getUserObject());
 				if (caArray2Experiment.getHybridizations() == null)
 					publishCaArrayRequestHybridizationListEvent(new CaArrayRequestHybridizationListEvent(
 							url, portnumber, user, passwd, caArray2Experiment));
 			}
 		}
 
 	}
 
 	@Publish
 	public CaArrayRequestHybridizationListEvent publishCaArrayRequestHybridizationListEvent(CaArrayRequestHybridizationListEvent event) {
 		return event;
 	}
 
 	@Subscribe
 	public void receive(CaArrayReturnHybridizationListEvent event, Object source) {
 		CaArray2Experiment caArray2Experiment = event.getCaArray2Experiment();
 
 		Object root = remoteTreeModel.getRoot();
 		DefaultMutableTreeNode experimentNode = null;
 		for(int i=0; i<remoteTreeModel.getChildCount(root); i++) {
 			experimentNode = (DefaultMutableTreeNode)remoteTreeModel.getChild(root, i);
 			if( ((CaArray2Experiment)experimentNode.getUserObject()).equals(caArray2Experiment)) {
 				break; // found the right experiment node. done
 			}
 		}
 		// at this point, caArray2Experiment.getHybridizations() should never be null. It could be zero size though.
 		Vector<String> vector = new Vector<String>();
 		for (String hybName: caArray2Experiment.getHybridizations().keySet()) {
 			vector.add(hybName);
 		}
 		Collections.sort(vector);
 		for (String hybName: vector) {
 			DefaultMutableTreeNode assayNode = new DefaultMutableTreeNode(hybName);
 			remoteTreeModel.insertNodeInto(assayNode, experimentNode, experimentNode
 					.getChildCount());
 		}
 		remoteFileTree.expandPath(new TreePath(experimentNode.getPath()));
 		updateTextArea();
 	}
 
 	private String checkQuantationTypeSelection(CaArray2Experiment exp) {
 
 		String[] qTypes = exp.getQuantitationTypes();
 		if (qTypes == null || qTypes.length <= 0) {
 			JOptionPane.showMessageDialog(
 					null,
 					"There is no data associated with experiment: "
 							+ exp.getName());
 			return null;
 		}
 
 		String chosenType = (String) JOptionPane.showInputDialog(null,
 					"Please select the quantitation type to query:\n",
 					"Selection Dialog", JOptionPane.PLAIN_MESSAGE, null,
 					qTypes, lastChosenQuantitationType);
 		// If a string is returned, remember it.
 		if ((chosenType != null) && (chosenType.length() > 0)) {
 			lastChosenQuantitationType = chosenType;
 		}
 
 		// assume s is never empty String "".
 		return chosenType;
 
 	}
 
 	/**
 	 * This method is called if the cancel button is pressed.
 	 *
 	 * @param e -
 	 *            Event information.
 	 */
 	private void jButton1_actionPerformed(ActionEvent e) {
 		dispose();
 	}
 
 	public void getExperiments(ActionEvent e) {
 		displayLabel.setText("");
 		if (!experimentsLoaded
 				|| !currentResourceName.equals(previousResourceName)) {
 
 			getExperiments();
 
 			this.validate();
 			this.repaint();
 		} else if (currentResourceName.equals(previousResourceName)
 				&& experimentsLoaded) {
 
 			if (parentPanel != null) {
 				parentPanel.addRemotePanel();
 			}
 		}
 	}
 
 	/**
 	 * Menu action listener that brings up the popup menu that allows populating
 	 * the tree node for a remote file.
 	 *
 	 * @param e
 	 */
 	private void jRemoteFileTree_mouseReleased(MouseEvent e) {
 		TreePath path = remoteFileTree.getPathForLocation(e.getX(), e.getY());
 		if (path != null) {
 			DefaultMutableTreeNode selectedNode = ((DefaultMutableTreeNode) path
 					.getLastPathComponent());
 			Object nodeObj = selectedNode.getUserObject();
 			
 			if (e.isMetaDown() && nodeObj instanceof CaArray2Experiment) {
 				jRemoteDataPopup.show(remoteFileTree, e.getX(), e.getY());
 				if (selectedNode.getChildCount() > 0) {
 					jGetRemoteDataMenu.setEnabled(false);
 				} else {
 					jGetRemoteDataMenu.setEnabled(true);
 				}
 			}
 		}
 	}
 
 	/**
 	 * Action listener invoked when a remote file is selected in the remote file
 	 * tree. Updates the experiment information text area.
 	 *
 	 * @param
 	 */
 	private void remoteFileSelection_action(TreeSelectionEvent tse) {
 		if (tse == null) {
 			return;
 		}
 		updateTextArea(); // Update the contents of the text area.
 	}
 
 	/**
 	 * Properly update the experiment text area, based on the currently selected
 	 * node.
 	 */
 	private void updateTextArea() {
 		DefaultMutableTreeNode node = (DefaultMutableTreeNode) remoteFileTree
 				.getLastSelectedPathComponent();
 
 		if (node == null || node == root) {
 			experimentInfoArea.setText("");
 			measuredField.setText("");
 			derivedField.setText("");
 		} else {
 			// get the parent experiment.
 			CaArray2Experiment exp = (CaArray2Experiment) ((DefaultMutableTreeNode) node.getPath()[1])
 					.getUserObject();
 			experimentInfoArea.setText(exp.getDescription());
 			Map<String, String> hybridization = exp.getHybridizations();
 			if (hybridization != null) {
 				measuredField.setText(String.valueOf(hybridization.size()));
 				derivedField.setText(String.valueOf(hybridization.size()));
 			} else {
 				measuredField.setText("");
 				derivedField.setText("");
 			}
 		}
 		experimentInfoArea.setCaretPosition(0); // For long text.
 	}
 
 	private void getBioAssay(String currentSelectedExperimentName, Map<String, String> currentSelectedBioAssay,
 			String quantitationType) {
 		CaArrayRequestEvent event = new CaArrayRequestEvent(url, portnumber);
 		if (user == null || user.trim().length() == 0) {
 			event.setUsername(null);
 		} else {
 			event.setUsername(user);
 			event.setPassword(passwd);
 		}
 		event.setRequestItem(CaArrayRequestEvent.BIOASSAY);
 		Map<String, String> filterCrit = new HashMap<String, String>();
 		filterCrit.put(CaArrayRequestEvent.EXPERIMENT, currentSelectedExperimentName  );
 		SortedMap<String, String> assayNameFilter = new TreeMap<String, String>(currentSelectedBioAssay);
 		event.setFilterCrit(filterCrit);
 		event.setAssayNameFilter(assayNameFilter);
 		event.setQType(quantitationType);
 		log.info("publish CaArrayEvent at CaArrayPanel");
 		publishCaArrayRequestEvent(event);
 	}
 
 	/**
 	 * Gets a list of all experiments available on the remote server.
 	 */
 	private void getExperiments() {
 
 		stillWaitForConnecting = true;
 		progressBar
 				.setMessage("Loading experiments from the remote resource...");
 		updateProgressBar("Loading experiments from the remote resource for ");
 		progressBar.addObserver(this);
 		progressBar.setTitle(CAARRAY_TITLE);
 		progressBar.start();
 
 		try {
 
 			if (url == null) {
 				user = System.getProperty("caarray.mage.user");
 				passwd = System.getProperty("caarray.mage.password");
 				url = System.getProperty("SecureSessionManagerURL");
 			}
 
 			System.setProperty("SecureSessionManagerURL", url);
 
 			// update the progress message.
 			stillWaitForConnecting = true;
 			progressBar
 					.setMessage("Connecting with the server... The initial step may take a few minutes.");
 			CaArrayRequestEvent event = new CaArrayRequestEvent(url, portnumber);
 			if (user == null || user.trim().length() == 0) {
 				event.setUsername(null);
 			} else {
 				event.setUsername(user);
 				event.setPassword(passwd);
 
 			}
 			event.setRequestItem(CaArrayRequestEvent.EXPERIMENT);
 			Thread thread = new PubilshThread(event);
 			thread.start();
 		} catch (Exception e) {
 			e.printStackTrace();
 		}
 	}
 
 	private class PubilshThread extends Thread {
 		CaArrayRequestEvent event;
 
 		PubilshThread(CaArrayRequestEvent event) {
 			this.event = event;
 		}
 		public void run() {
 			publishCaArrayRequestEvent(event);
 		}
 	}
 
 	private void dispose() {
 		parent.dispose();
 	}
 
 	public void update(java.util.Observable ob, Object o) {
 		stillWaitForConnecting = false;
 		log.error("Get Cancelled");
 
 		progressBar.dispose();
 
 		CaARRAYPanel.cancelledConnectionInfo = CaARRAYPanel.createConnectonInfo( url, portnumber, user, passwd);
 		CaARRAYPanel.isCancelled = true;
 		// user can get next array now
 		setOpenButtonEnabled(true);
 
 	}
 
 	public boolean isConnectionSuccess() {
 		return connectionSuccess;
 	}
 
 	public int getPortnumber() {
 		return portnumber;
 	}
 
 	public void setPortnumber(int portnumber) {
 		this.portnumber = portnumber;
 	}
 
 	public String getUrl() {
 		return url;
 	}
 
 	public String getUser() {
 		return user;
 	}
 
 	public String getPasswd() {
 		return passwd;
 	}
 
 	public String getCurrentResourceName() {
 		return currentResourceName;
 	}
 
 	public LoadDataDialog getParentPanel() {
 		return parentPanel;
 	}
 
 	public boolean isExperimentsLoaded() {
 		return experimentsLoaded;
 	}
 
 	public void setConnectionSuccess(boolean isConnectionSuccess) {
 		this.connectionSuccess = isConnectionSuccess;
 	}
 
 	public void setUrl(String url) {
 		this.url = url;
 	}
 
 	public void setUser(String user) {
 		this.user = user;
 	}
 
 	public void setPasswd(String passwd) {
 		this.passwd = passwd;
 	}
 
 	public void setCurrentResourceName(String currentResourceName) {
 		this.currentResourceName = currentResourceName;
 	}
 
 	public void initializeExperimentTree() {
 		root = new DefaultMutableTreeNode("caARRAY experiments");
 		remoteTreeModel.setRoot(root);
 		repaint();
 	}
 
 	public void setParentPanel(LoadDataDialog parentPanel) {
 		this.parentPanel = parentPanel;
 	}
 
 	public void setExperimentsLoaded(boolean experimentsLoaded) {
 		this.experimentsLoaded = experimentsLoaded;
 	}
 
 	public Component getComponent() {
 		return this;
 	}
 
 	// refactored, static vars for now, as only one event could be canceled,
 	// eventually will be in some utility class
 	public static volatile String cancelledConnectionInfo = null;
 	public static volatile boolean isCancelled = false;
 
 	// refactored, not sure if needed at all
 	public static String createConnectonInfo(String url, int port, String username,
 			String password) {
 		String currentConnectionInfo = url + port;
 		if (username != null && username.length() > 0) {
 			currentConnectionInfo = currentConnectionInfo + username + password;
 		}
 		return currentConnectionInfo;
 	}
 }
