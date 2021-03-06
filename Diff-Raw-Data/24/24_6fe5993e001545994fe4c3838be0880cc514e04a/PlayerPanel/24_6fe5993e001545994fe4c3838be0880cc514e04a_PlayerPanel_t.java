 package admin.panel.person.player;
 
 import java.awt.BorderLayout;
 import java.awt.Component;
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
 import java.awt.event.MouseEvent;
 import java.awt.event.MouseListener;
 import java.util.Arrays;
 import java.util.EnumSet;
 import java.util.List;
 import java.util.Observable;
 import java.util.Observer;
 
 import javax.swing.JButton;
 import javax.swing.JComboBox;
 import javax.swing.JLabel;
 import javax.swing.JPanel;
 import javax.swing.JTabbedPane;
 import javax.swing.JTextField;
 import javax.swing.ListSelectionModel;
 import javax.swing.SwingConstants;
 import javax.swing.event.ChangeEvent;
 import javax.swing.event.ChangeListener;
 
 import admin.MainFrame;
 import admin.Utils;
 import admin.panel.person.PersonPanel;
 import data.Contestant;
 import data.GameData;
 import data.GameData.UpdateTag;
 import data.InvalidFieldException;
 import data.Person;
 import data.User;
 
 /**
  * TODO: Doc
  * 
  * @author kevin
  * 
  */
 public class PlayerPanel extends PersonPanel<User> implements ChangeListener,
 		MouseListener, Observer {
 
 	private static final long serialVersionUID = 1L;
 
 	// input fields:
 	private JLabel labelName;
 	private JTextField tfFirstName;
 	private JTextField tfLastName;
 
 	private JLabel labelID;
 	private JTextField tfID;
 	private JButton btnGenID;
 
 	private JLabel labelWeekly;
 	private JComboBox<Contestant> cbWeeklyPick;
 	private JLabel labelUltimate;
 	private JComboBox<Contestant> cbUltPick;
 
 	/*
 	 * FIXME: Break into two labels one with "Points:" other with actual value
 	 * of pts
 	 */
 	private JLabel labelPts;
 	private JLabel labelPtsValue;
 	
 	// Constants:
 	protected static final String TOOL_NAME = "First and Last name must be alphabetic", 
 			TOOL_IDTXT = "ID must be 2-7 chars long and may end with numbers",
 			TOOL_IDBTN = "Click to auto-generate ID from first and last name",
 			TOOL_WEEKLY = "Select Weekly pick",
 			TOOL_ULT = "Select Ultimate Winner",
 			TOOL_SAVE = "",
 			TOOL_DELETE = "Remove selected User from system",
 			TOOL_NEW = "Add a new User to system",
 			TOOL_TABLE = "Click Heading to sort by column";
 
 	public PlayerPanel() {
 		super(new User());
 
 		setLayout(new BorderLayout(5, 5));
 
 		// ////////////////////////////
 		// Top Panel:
 		// ////////////////////////////
 		labelName = new JLabel("Name:");
 		tfFirstName = new JTextField();
 		tfLastName = new JTextField();
 
 		labelID = new JLabel("User ID:");
 		tfID = new JTextField();
 		btnGenID = new JButton("Generate ID");
 
 		labelWeekly = new JLabel("Weekly Pick:");
 		cbWeeklyPick = new JComboBox<Contestant>();
 
 		labelUltimate = new JLabel("Ultimate Pick:");
 		cbUltPick = new JComboBox<Contestant>();
 		
 		labelPts = new JLabel("Current Points:");
 		labelPtsValue = new JLabel("0");
 		labelPtsValue.setHorizontalTextPosition(SwingConstants.RIGHT);
 
 		personFields = new PlayerFieldsPanel(labelName, tfFirstName,
 				tfLastName, labelID, tfID, btnGenID, labelWeekly, cbWeeklyPick,
 				labelUltimate, cbUltPick, labelPts, labelPtsValue);
 		// add the mouse listener to all components.
 		for (Component c : ((JPanel)personFields).getComponents()) {
 			c.addMouseListener(this);
 		}
 
 		// right side!
 		
 
 		// ////////////////////////////
 		// Mid
 		// ////////////////////////////
 		// handled in super
 
 		// ////////////////////////////
 		// Bottom
 		// ////////////////////////////
 	
 
 		refreshContestantCBs();
 		assembleAll();
 	}
 
 	/**
 	 * Currently used to check if a tab is changed, and if its changed to the
 	 * PlayerPanel, it will modify ComboBoxes.
 	 */
 	@Override
 	public void stateChanged(ChangeEvent e) {
 
 		Object obj = e.getSource();
 		if (obj instanceof JTabbedPane) // tab clicked
 		{
 
 			JTabbedPane tab = (JTabbedPane) obj;
 
 			if (tab.getSelectedIndex() != 2)
 				return;
 
 			refreshContestantCBs();
 			return;
 		}
 	}
 
 	/**
 	 * Sets the user on the screen to the specified container. If newUser is
 	 * true, it will specify that when save is hit, then the GUI should add it
 	 * to the table rather than modify a pre-existing data.
 	 * 
 	 * @param u
 	 * @param newUser
 	 */
 	@Override
 	protected void setPanelPerson(User u, boolean newUser) {
 		super.setPanelPerson(u, newUser);
 		
 		btnSave.setEnabled(false);
 
 		if (newUser || u == null) {
 			// we don't want any rows selected
 			ListSelectionModel m = table.getSelectionModel();
 			m.clearSelection();
 			
 			return;
 		}
 		
 		tableModel.setRowSelect(u);
 	}
 
 	/**
 	 * Sets the error information based on an exception!
 	 * 
 	 * @param e
 	 *            Exception with the information necessary
 	 */
 	@Override
 	protected void setExceptionError(InvalidFieldException e) {
 		if (e.isHandled())
 			return;
 
 		MainFrame mf = MainFrame.getRunningFrame();
 
 		switch (e.getField()) {
 		case USER_ID:
 			mf.setStatusErrorMsg("Invalid ID (must be between 2 and 7 chars"
 					+ " long, followed by numbers)", tfID);
 			break;
 		case USER_ID_DUP:
 			mf.setStatusErrorMsg("Invalid ID (in use)", tfID);
 			break;
 		case USER_FIRST:
 			mf.setStatusErrorMsg("Invalid First Name (must be alphabetic"
 					+ ", 1-20 characters)", tfFirstName);
 			break;
 		case USER_LAST:
 			mf.setStatusErrorMsg("Invalid Last Name (must be alphabetic"
 					+ ", 1-20 characters)", tfLastName);
 			break;
 		case USER_ULT_PICK:
 			mf.setStatusErrorMsg("Invalid Ultimate Pick", cbUltPick);
 			break;
 		case USER_WEEKLY_PICK:
 			mf.setStatusErrorMsg("Invalid Weekly Pick", cbWeeklyPick);
 			break;
 		default:
 			mf.setStatusErrorMsg("Unknown problem with fields");
 		}
 
 		e.handle();
 	}
 
 	@Override
 	protected void buildActions() {
 		super.buildActions();
 		
 		btnAddNew.addActionListener(new ActionListener() {
 
 			@Override
 			public void actionPerformed(ActionEvent e) {
 				if (fieldsChanged) {
 					try {
 						savePerson();
 					} catch (InvalidFieldException ex) {
 						// can't add new.. :/
 						setExceptionError(ex);
 						return;
 					}
 				}
 
 				setPanelPerson(null, true);
 			}
 
 		});
 
 		btnGenID.addActionListener(new ActionListener() {
 
 			@Override
 			public void actionPerformed(ActionEvent arg0) {
 				User u = new User();
 				try {
 					u.setFirstName(tfFirstName.getText().trim());
 					u.setLastName(tfLastName.getText().trim());
 				} catch (InvalidFieldException ex) {
 					setExceptionError(ex);
 					return;
 				}
 				GameData g = GameData.getCurrentGame();
 				List<Person> userList = Utils.castListElem(g.getAllUsers(),
 						(Person) (new User())); // lol so ugly.
 
 				String id = Utils.generateID(u, userList);
 
 				tfID.setText(id);
 			}
 		});
		
		ItemListener cbListen = new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) 
					return; // we'll only look at selected
				
				Object src = e.getSource();
				
				// fake mouse event
				MouseEvent me = new MouseEvent((Component) e.getSource(), 
						e.getID(), System.currentTimeMillis(), 
						WHEN_FOCUSED, 0, 0, 0, false);
				
				mouseClicked(me);
			}
		};
		
		cbUltPick.addItemListener(cbListen);
		cbWeeklyPick.addItemListener(cbListen);
 
 		List<JTextField> tfArr = Arrays.asList(tfID, tfFirstName, tfLastName);
 		for (JTextField tf : tfArr) {
 			tf.addFocusListener(editAdapt);
 		}
 	}
 
 	/**
 	 * Loads the contestant data in the ComboBoxes from the GameData.
 	 */
 	private void refreshContestantCBs() {
 		GameData g = (GameData) GameData.getCurrentGame();
 
 		if (g == null) {
 			return;
 		}
 
 		List<Contestant> cons = g.getActiveContestants();
 
 		cbWeeklyPick.removeAllItems();
 		cbUltPick.removeAllItems();
 
 		boolean seasonStarted = g.isSeasonStarted();
 		cbUltPick.setEnabled(seasonStarted);
 		cbWeeklyPick.setEnabled(seasonStarted);
 
 		Contestant nullC = new Contestant();
 		nullC.setNull();
 
 		cbWeeklyPick.addItem(nullC);
 		cbUltPick.addItem(nullC);
 
 		if (seasonStarted) {
 			for (Contestant c : cons) {
 				cbWeeklyPick.addItem(c);
 				cbUltPick.addItem(c);
 			}
 		}
 
 	}
 
 	@Override
 	public void mouseClicked(MouseEvent me) {
 		Component c = me.getComponent();
 		
 		if (!c.isEnabled()) return;
 		
 		if (c == tfFirstName || c == tfLastName || c == tfID || c == btnGenID || 
 				c == cbUltPick || c == cbWeeklyPick) {
 			fieldsChanged = true;
 			btnSave.setEnabled(c.isEnabled());
 		}
 	}
 
 	/**
 	 * Changes all fields that have data changed. <br>
 	 * Currently calls: - Table update - Updates ComboBoxes
 	 */
 	@Override
 	public void update(Observable o, Object arg) {
 		@SuppressWarnings("unchecked")
 		EnumSet<UpdateTag> update = (EnumSet<UpdateTag>)arg;
 
 		if (update == null) 
 			return;
 		
 		GameData g = (GameData)o;
 
 		if (update.contains(UpdateTag.START_SEASON)) {
 			
 			btnAddNew.setEnabled(!g.isSeasonStarted());
 			btnDelete.setEnabled(!g.isSeasonStarted());
 			
 			btnGenID.setEnabled(!g.isSeasonStarted());
 			tfID.setEnabled(!g.isSeasonStarted());
 		}
 		
 		if (update.contains(UpdateTag.ADD_CONTESTANT) ||
 				update.contains(UpdateTag.REMOVE_CONTESTANT) ||
 				update.contains(UpdateTag.START_SEASON) ||
 				update.contains(UpdateTag.CONTESTANT_CAST_OFF)) {
 			refreshContestantCBs();
 				
 			tableModel.fireTableDataChanged();
 		}
 		
 		if (update.contains(UpdateTag.ADVANCE_WEEK)) {
 			tableModel.fireTableDataChanged();
 		}
 	}
 
 	@Override
 	protected void setToolTips() {
 		
 		labelName.setToolTipText(TOOL_NAME);
 		tfFirstName.setToolTipText(TOOL_NAME);
 		tfLastName.setToolTipText(TOOL_NAME);
 		
 		labelID.setToolTipText(TOOL_IDTXT);
 		tfID.setToolTipText(TOOL_IDTXT);
 		btnGenID.setToolTipText(TOOL_IDBTN);
 
 		labelWeekly.setToolTipText(TOOL_WEEKLY);
 		cbWeeklyPick.setToolTipText(TOOL_WEEKLY);
 	
 		labelUltimate.setToolTipText(TOOL_ULT);
 		cbUltPick.setToolTipText(TOOL_ULT);
 		
 		btnSave.setToolTipText(TOOL_SAVE);
 		
 		btnAddNew.setToolTipText(TOOL_NEW);
 		btnDelete.setToolTipText(TOOL_DELETE);
 		
 	}
 }
