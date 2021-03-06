 package gui;
 
 import java.awt.*;
 import java.awt.event.*;
 import java.util.HashMap;
 
 import javax.swing.*;
 import javax.swing.border.EmptyBorder;
 import javax.swing.border.EtchedBorder;
 import javax.swing.border.TitledBorder;
 
 import visualization.MapComponent;
 
 import controller.Controller;
 
 /**
  * 
  * @author Anders
  * @author Pacmans
  * @version 10. April 2012
  * 
  */
 public class GUI {
 
 	// This field contains the current version of the program.
 	private static final String VERSION = "Version 1.0";
 	// The main frame of our program.
 	private JFrame frame;
 	private Controller controller;
 	private JPanel contentPane, mapPanel, loadingPanel, optionPanel,
 			roadtypeBoxes;
 	// The map from the controller
 	private MapComponent map;
 	// A ButtonGroup with car, bike, and walk.
 	private ButtonGroup group;
 	private HashMap<String, JCheckBox> boxes = new HashMap<String, JCheckBox>();
 	// selected JToggleButton - 0 if car, 1 if bike, 2 if walk.
 	private int selectedTransport = 0, number;
 	private JLabel statusbar = new JLabel(" ");
	private boolean manuelControl = false;
 
 	public GUI() {
 		makeFrame();
 		makeMenuBar();
 		makeRightPanel();
 		setupFrame();
 		controller = Controller.getInstance();
 	}
 
 	private void setupFrame() {
 		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
 		frame.pack();
 		frame.setSize(800, 600);
 		// place the frame at the center of the screen and show.
 		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
 		frame.setLocation(d.width / 2 - frame.getWidth() / 2, d.height / 2
 				- frame.getHeight() / 2);
 		contentPane.setEnabled(false);
 		frame.setBackground(Color.darkGray);
 		frame.setState(Frame.NORMAL);
 		frame.setVisible(true);
 		updateGUI();
 	}
 
 	private void updateGUI() {
		if (manuelControl) {
 			roadtypeBoxes.setVisible(true);
 		} else {
 			roadtypeBoxes.setVisible(false);
 		}
 	}
 
 	public void setupMap() {
 		map = controller.getMap();
 		map.addMouseWheelListener(new MouseWheelListener() {
 			public void mouseWheelMoved(MouseWheelEvent e) {
				if (!manuelControl) { // manuel control is not selected
 					int wheelDirection = e.getWheelRotation();
 					int zoom = map.getZoomNiveau();
 
 					if (wheelDirection < 0) { // zooms in
 						switch (zoom) {
 						case 25:
 							controller.updateMap(4, true);
 							boxes.get("Secondary roads").setSelected(true);
 							break;
 						case 17:
 							controller.updateMap(5, true);
 							boxes.get("Normal roads").setSelected(true);
 							break;
 						case 14:
 							controller.updateMap(6, true);
 							boxes.get("Trails & streets").setSelected(true);
 							break;
 						case 11:
 							controller.updateMap(7, true);
 							boxes.get("Paths").setSelected(true);
 							break;
 						}
 					} else { // scrolls down
 						switch (zoom) {
 						case 26:
 							controller.updateMap(4, false);
 							boxes.get("Secondary roads").setSelected(false);
 							break;
 						case 18:
 							controller.updateMap(5, false);
 							boxes.get("Normal roads").setSelected(false);
 							break;
 						case 15:
 							controller.updateMap(6, false);
 							boxes.get("Trails & streets").setSelected(false);
 							break;
 						case 12:
 							controller.updateMap(7, false);
 							boxes.get("Paths").setSelected(false);
 							break;
 						}
 					}
 				}
 			}
 		});
 		frame.setVisible(false);
 		contentPane.remove(loadingPanel);
 		mapPanel = new JPanel(new GridLayout(1, 1));
 		mapPanel.add(map);
 		contentPane.add(mapPanel, "Center");
 		contentPane.add(statusbar, "South");
 		frame.addComponentListener(new ComponentAdapter() {
 			public void componentResized(ComponentEvent e) {
 				// Controller.scaleMap(mapPanel.getWidth(),mapPanel.getHeight());
 				mapPanel.updateUI();
 			}
 		});
 		frame.pack();
 		contentPane.setEnabled(true);
 		updateGUI();
 		frame.setBackground(Color.lightGray);
 		frame.setSize(800, 600);
 		frame.setVisible(true);
 	}
 
 	private void makeFrame() {
 		// create the frame set the layout and border.
 		frame = new JFrame("Map Of Denmark");
 		contentPane = (JPanel) frame.getContentPane();
 		contentPane.setBorder(new EmptyBorder(4, 4, 4, 4));
 		contentPane.setLayout(new BorderLayout(5, 5));
 		loadingPanel = new JPanel(new FlowLayout(1));
 		loadingPanel.setBorder(new EmptyBorder(150, 6, 6, 6));
 		JLabel loadingLabel = new JLabel("Loading map...");
 		loadingLabel.setForeground(Color.white);
 		loadingLabel.setFont(new Font("Verdana", Font.BOLD, 40));
 		loadingPanel.add(loadingLabel);
 		contentPane.add(loadingPanel, "Center");
 	}
 
 	private void makeMenuBar() {
 		// Create key stroke shortcuts for the menu.
 		final int SHORTCUT_MASK = Toolkit.getDefaultToolkit()
 				.getMenuShortcutKeyMask();
 
 		// make the JMenuBar
 		JMenuBar menubar = new JMenuBar();
 		frame.setJMenuBar(menubar);
 
 		// create the JMenu fields.
 		JMenu menu;
 		JMenuItem item;
 
 		// create File menu
 		menu = new JMenu("File");
 		menubar.add(menu);
 
 		// make a new menu item and add a setAccelerator to use of shortcuts.
 		item = new JMenuItem("Quit");
 		item.setAccelerator(KeyStroke
 				.getKeyStroke(KeyEvent.VK_Q, SHORTCUT_MASK));
 		// create an actionlistener and call the method quit() when chosen.
 		item.addActionListener(new ActionListener() {
 			public void actionPerformed(ActionEvent e) {
 				quit();
 			}
 		});
 		menu.add(item);
 
 		// create Help menu
 		menu = new JMenu("Help");
 		menubar.add(menu);
 
 		item = new JMenuItem("About Map Of Denmark");
 		item.setAccelerator(KeyStroke
 				.getKeyStroke(KeyEvent.VK_A, SHORTCUT_MASK));
 		item.addActionListener(new ActionListener() {
 			public void actionPerformed(ActionEvent e) {
 				showAbout();
 			}
 		});
 		menu.add(item);
 	}
 
 	private void makeRightPanel() {
 		// initialize a new JPanel.
 		optionPanel = new JPanel();
 		// create a vertical BoxLayout on the optionPanel.
 		optionPanel.setLayout(new BoxLayout(optionPanel, BoxLayout.Y_AXIS));
 
 		// add the checkbox, and the other GUI to the right panel.
 		optionPanel.add(createRouteplanningBox());
 		optionPanel.add(createCheckbox());
 		optionPanel.add(createZoomOutButton());
 		// add the optionPanel to the contentPanes borderlayout.
 		contentPane.add(optionPanel, "East");
 	}
 
 	private JPanel createRouteplanningBox() {
 		JPanel routePlanning = new JPanel();
 		TitledBorder border = new TitledBorder(new EtchedBorder(),
 				"Route planning");
 		border = setHeadlineFont(border);
 		routePlanning.setBorder(border);
 		routePlanning.setLayout(new BoxLayout(routePlanning, BoxLayout.Y_AXIS));
 
 		// from row
 		JLabel label = new JLabel("From");
 		label = setLabelFont(label);
 		JTextField text = new JTextField(10);
 		text = addLiveSearch(text);
 		text.setBackground(Color.lightGray);
 		JPanel fromPanel = new JPanel(new FlowLayout(2));
 		fromPanel.add(label);
 		fromPanel.add(text);
 
 		// to row
 		label = new JLabel("To");
 		label = setLabelFont(label);
 		text = new JTextField(10);
 		text = addLiveSearch(text);
 		text.setBackground(Color.lightGray);
 		JPanel toPanel = new JPanel(new FlowLayout(2));
 		toPanel.add(label);
 		toPanel.add(text);
 
 		// go button
 		JButton go = new JButton("Go");
 		go = setButtonText(go);
 		go.setPreferredSize(new Dimension(55, 33));
 		go.addActionListener(new ActionListener() {
 			public void actionPerformed(ActionEvent e) {
 				// get the selected transportation type and DO SOMETHING
 				// getSelectedTransportation()
 			}
 		});
 		JPanel goPanel = new JPanel(new FlowLayout(1));
 		goPanel.add(go);
 
 		routePlanning.add(fromPanel);
 		routePlanning.add(toPanel);
 		routePlanning.add(createTogglePanel());
 		routePlanning.add(goPanel);
 
 		return routePlanning;
 	}
 
 	private JTextField addLiveSearch(JTextField text) {
 		text.addKeyListener(new KeyAdapter() {
 			public void keyReleased(KeyEvent e) {
 				JTextField textField = (JTextField) e.getSource();
 				String string = textField.getText();
 				setStatus(string);
 			}
 		});
 		return text;
 	}
 
 	private <T extends JComponent> T setLabelFont(T label) {
 		label.setFont(new Font("Verdana", Font.PLAIN, 14));
 		return label;
 	}
 
 	private TitledBorder setHeadlineFont(TitledBorder label) {
 		label.setTitleFont(new Font("Verdana", Font.BOLD, 15));
 		return label;
 	}
 
 	private <T extends JComponent> T setButtonText(T label) {
 		label.setFont(new Font("Verdana", Font.PLAIN, 14));
 		return label;
 	}
 
 	// toggleButtons in a ButtonGroup
 	private JPanel createTogglePanel() {
 		JPanel togglePanel = new JPanel(new FlowLayout(1));
 		group = new ButtonGroup();
 		ImageIcon icon = getScaledIcon(new ImageIcon("./src/icons/car.png"));
 		togglePanel.add(createJToggleButton(icon, true, 0));
 
 		icon = getScaledIcon(new ImageIcon("./src/icons/bike.png"));
 		togglePanel.add(createJToggleButton(icon, false, 1));
 
 		icon = getScaledIcon(new ImageIcon("./src/icons/walk.png"));
 		togglePanel.add(createJToggleButton(icon, false, 2));
 		return togglePanel;
 	}
 
 	private ImageIcon getScaledIcon(ImageIcon icon) {
 		Image img = icon.getImage();
 		Image newimg = img.getScaledInstance(30, 30,
 				java.awt.Image.SCALE_SMOOTH);
 		return new ImageIcon(newimg);
 	}
 
 	private JToggleButton createJToggleButton(ImageIcon ico, boolean selected,
 			int number) {
 		JToggleButton button = new JToggleButton();
 		final int _number = number;
 		if (selected == true)
 			button.setSelected(true);
 		button.setIcon(ico);
 		button.addItemListener(new ItemListener() {
 			public void itemStateChanged(ItemEvent e) {
 				if (e.getStateChange() == ItemEvent.SELECTED) {
 					setSelectedTransportation(_number);
 					System.out.println(getSelectedTransportation());
 				}
 			}
 		});
 		group.add(button);
 		return button;
 	}
 
 	// return 0 if car, 1 if bike, 2 if walk.
 	private int getSelectedTransportation() {
 		return selectedTransport;
 	}
 
 	// return 0 if car, 1 if bike, 2 if walk.
 	private void setSelectedTransportation(int number) {
 		selectedTransport = number;
 	}
 
 	private JPanel createZoomOutButton() {
 		JPanel zoomPanel = new JPanel(new FlowLayout(1));
 		JButton zoomOut = new JButton("Zoom out");
 		zoomOut.setPreferredSize(new Dimension(90, 35));
 		zoomOut = setButtonText(zoomOut);
 		zoomOut.addActionListener(new ActionListener() {
 			public void actionPerformed(ActionEvent e) {
				if (!manuelControl) {
 					controller.updateMap(1, true);
 					controller.updateMap(2, true);
 					controller.updateMap(3, true);
 					controller.updateMap(4, true);
 					controller.updateMap(5, false);
 					controller.updateMap(6, false);
 					controller.updateMap(7, false);
 				}
 				controller.showAll();
 			}
 		});
 		zoomPanel.add(zoomOut);
 		return zoomPanel;
 	}
 
 	private JPanel createCheckbox() {
 		// initialize checkboxPanel
 		JPanel checkboxPanel = new JPanel();
 		checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.Y_AXIS));
 		TitledBorder border = new TitledBorder(new EtchedBorder(), "Road types");
 		border = setHeadlineFont(border);
 		checkboxPanel.setBorder(border);
 		// fill the checkboxPanel
		JPanel manuelPanel = new JPanel(new FlowLayout(0));
		JCheckBox manuelControlBox = new JCheckBox("Manuel Control");
		manuelControlBox.setSelected(false);
		manuelControlBox = setLabelFont(manuelControlBox);
		manuelControlBox.addItemListener(new ItemListener() {
 			public void itemStateChanged(ItemEvent e) {
 				if (e.getStateChange() == 1) { // selected
					manuelControl = true;
 				} else {
					manuelControl = false;
 				}
 				updateGUI();
 			}
 		});
		manuelPanel.add(manuelControlBox);
 
 		roadtypeBoxes = new JPanel(new GridLayout(7, 1));
 		roadtypeBoxes.add(createRoadtypeBox("Highways", true)); // Priority 1
 																// roads
 		roadtypeBoxes.add(createRoadtypeBox("Expressways", true)); // Priority 2
 		roadtypeBoxes.add(createRoadtypeBox("Primary roads", true)); // and so
 																		// on..
 		roadtypeBoxes.add(createRoadtypeBox("Secondary roads", true));
 		roadtypeBoxes.add(createRoadtypeBox("Normal roads", false));
 		roadtypeBoxes.add(createRoadtypeBox("Trails & streets", false));
 		roadtypeBoxes.add(createRoadtypeBox("Paths", false));
		checkboxPanel.add(manuelPanel);
 		checkboxPanel.add(roadtypeBoxes);
 
 		return checkboxPanel;
 	}
 
 	private JPanel createRoadtypeBox(String string, boolean selected) {
 		JPanel fl = new JPanel(new FlowLayout(0));
 		JCheckBox box = new JCheckBox(string);
 		box = setLabelFont(box);
 		box.setSelected(selected);
 		box.addItemListener(new ItemListener() {
 			public void itemStateChanged(ItemEvent e) {
 				JCheckBox box = (JCheckBox) e.getSource();
 				if (box.getText().equals("Highways"))
 					number = 1;
 				if (box.getText().equals("Expressways"))
 					number = 2;
 				if (box.getText().equals("Primary roads"))
 					number = 3;
 				if (box.getText().equals("Secondary roads"))
 					number = 4;
 				if (box.getText().equals("Normal roads"))
 					number = 5;
 				if (box.getText().equals("Trails & streets"))
 					number = 6;
 				if (box.getText().equals("Paths"))
 					number = 7;
 				if (e.getStateChange() == 1)
 					controller.updateMap(number, true);
 				else
 					controller.updateMap(number, false);
 			}
 		});
 		boxes.put(string, box);
 		fl.add(box);
 		return fl;
 	}
 
 	public void setStatus(String text) {
 		statusbar.setText(text);
 	}
 
 	public void quit() {
 		// Exits the application.
 		System.exit(0);
 	}
 
 	/**
 	 * Creates a message dialog which shows the current version of the
 	 * application.
 	 */
 	private void showAbout() {
 		JOptionPane.showMessageDialog(frame, "Map Of Denmark - " + VERSION
 				+ "\nMade by Claus, BjÃ¸rn, Phillip, Morten & Anders.",
 				"About Map Of Denmark", JOptionPane.INFORMATION_MESSAGE);
 	}
 }
