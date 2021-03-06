 package com.jakeapp.gui.swing.panels;
 
 import com.jakeapp.core.domain.ProtocolType;
 import com.jakeapp.core.domain.ServiceCredentials;
 import com.jakeapp.core.domain.exceptions.FrontendNotLoggedInException;
 import com.jakeapp.core.domain.exceptions.InvalidCredentialsException;
 import com.jakeapp.core.services.MsgService;
 import com.jakeapp.core.services.exceptions.ProtocolNotSupportedException;
 import com.jakeapp.core.util.availablelater.AvailableLaterObject;
 import com.jakeapp.gui.swing.JakeMainApp;
 import com.jakeapp.gui.swing.JakeMainView;
 import com.jakeapp.gui.swing.callbacks.ConnectionStatus;
 import com.jakeapp.gui.swing.callbacks.RegistrationStatus;
 import com.jakeapp.gui.swing.controls.JAsynchronousProgressIndicator;
 import com.jakeapp.gui.swing.dialogs.AdvancedAccountSettingsDialog;
 import com.jakeapp.gui.swing.helpers.*;
 import com.jakeapp.gui.swing.helpers.dragdrop.ProjectDropHandler;
 import com.jakeapp.gui.swing.renderer.IconComboBoxRenderer;
 import com.jakeapp.gui.swing.worker.LoginAccountWorker;
 import com.jakeapp.gui.swing.worker.SwingWorkerWithAvailableLaterObject;
 import com.jakeapp.jake.ics.exceptions.NetworkException;
 import net.miginfocom.swing.MigLayout;
 import org.apache.log4j.Logger;
 import org.jdesktop.application.ResourceMap;
 import org.jdesktop.jxlayer.JXLayer;
 import org.jdesktop.swingx.JXHyperlink;
 import org.jdesktop.swingx.JXPanel;
 import org.jdesktop.swingx.hyperlink.LinkAction;
 
 import javax.swing.*;
 import javax.swing.event.DocumentEvent;
 import javax.swing.event.DocumentListener;
 import java.awt.*;
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.io.IOException;
 import java.net.URI;
 import java.net.URISyntaxException;
 import java.util.List;
 
 /**
  * The Userpanel creates accouts for Jake.
  *
  * @author: studpete
  */
 public class UserPanel extends JXPanel implements RegistrationStatus, ConnectionStatus {
 	private ResourceMap resourceMap;
 	private static final Logger log = Logger.getLogger(UserPanel.class);
 
 	private javax.swing.JRadioButton registerRadioButton;
 	private javax.swing.JRadioButton loginRadioButton;
 	private javax.swing.ButtonGroup loginRegisterButtonGroup;
 
 	private JPanel loginSuccessPanel;
 	private javax.swing.JPanel registrationInfoPanel;
 	private JButton signInRegisterButton;
 	private UserDataPanel loginUserDataPanel;
 	private UserDataPanel registerUserDataPanel;
 	private JComboBox loginServiceCheckBox;
 	private JAsynchronousProgressIndicator workingAnimation;
 	private ServiceCredentials cred;
 	private JPanel addUserPanel;
 	private JPanel loginUserPanel;
 
 	private ImageIcon jakeWelcomeIcon = new ImageIcon(JakeMainView.getMainView().getLargeAppImage().getScaledInstance(90, 90, Image.SCALE_SMOOTH));
 	private JLabel userLabelLoginSuccess;
 	private JPanel userListPanel;
 
 	/**
 	 * SupportedServices; we add some default support for common services.
 	 */
 	private enum SupportedServices {
 		Google, Jabber, UnitedInternet
 	}
 
 	/**
 	 * The three user panels supported.
 	 */
 	private enum UserPanels {
 		AddUser, ManageUsers, LoggedIn
 	}
 
 	/**
 	 * Create the User Panel.
 	 */
 	public UserPanel() {
 
 		setResourceMap(org.jdesktop.application.Application.getInstance(
 				  com.jakeapp.gui.swing.JakeMainApp.class).getContext().getResourceMap(UserPanel.class));
 
 		// only support xmpp - for the moment
 		cred = new ServiceCredentials();
 		cred.setProtocol(ProtocolType.XMPP);
 
 		initComponents();
 
 		// register the connection & reg status callback!
 		JakeMainApp.getApp().getCore().addConnectionStatusCallbackListener(this);
 		JakeMainApp.getApp().getCore().addRegistrationStatusCallbackListener(this);
 
 		// device which panel to show!
 		updateView();
 	}
 
 	public ResourceMap getResourceMap() {
 		return resourceMap;
 	}
 
 	public void setResourceMap(ResourceMap resourceMap) {
 		this.resourceMap = resourceMap;
 	}
 
 
 	private void initComponents() {
 		this.setLayout(new MigLayout("wrap 1, fill, center, ins 15"));
 
 		// initialize various panels
 		addUserPanel = createAddUserPanel();
 		loginUserPanel = createChooseUserPanel();
 		loginSuccessPanel = createSignInSuccessPanel();
 
 		// set the background painter
 		this.setBackgroundPainter(Platform.getStyler().getContentPanelBackgroundPainter());
 	}
 
 	/**
 	 * Creates the Login User Panel
 	 *
 	 * @return
 	 */
 	private JPanel createChooseUserPanel() {
 		// create the user login panel
 		JPanel loginUserPanel = new JPanel(new MigLayout("wrap 1, fill, center, ins 0"));
 		loginUserPanel.setOpaque(false);
 
 		// the say hello heading
 		JPanel titlePanel = new JPanel(new MigLayout("nogrid, fillx, top, ins 0"));
 		titlePanel.setOpaque(false);
 		JLabel selectUserLabel = new JLabel(getResourceMap().getString("selectUserLabel"));
 		selectUserLabel.setIcon(jakeWelcomeIcon);
 		selectUserLabel.setVerticalTextPosition(JLabel.TOP);
 		titlePanel.add(selectUserLabel, "top, center, h 80!");
 		loginUserPanel.add(titlePanel, "wrap, gapbottom 20, top, growx, h 80!");
 
 		// add link to create new user
 		JButton createAccountBtn = new JButton(getResourceMap().getString("addUserBtn"));
 		createAccountBtn.putClientProperty("JButton.buttonType", "textured");
 		createAccountBtn.addActionListener(new ActionListener() {
 			@Override
 			public void actionPerformed(ActionEvent e) {
 				showPanel(UserPanels.AddUser);
 			}
 		});
 
 		// TODO: show how many projects a user has
 		// create the user list
 		userListPanel = new JPanel(new MigLayout("wrap 1, filly, center, ins 0"));
 		userListPanel.setOpaque(false);
 		JScrollPane usersScrollPanel = new JScrollPane(userListPanel);
 		usersScrollPanel.setOpaque(false);
 		usersScrollPanel.getViewport().setOpaque(false);
 		usersScrollPanel.setBorder(null);
 
 		updateChooseUserPanel();
 
 		loginUserPanel.add(usersScrollPanel, "grow");
 
 		loginUserPanel.add(createAccountBtn, "wrap, center");
 
 		return loginUserPanel;
 	}
 
 
 	private void updateChooseUserPanel() {
 		userListPanel.removeAll();
 
 		try {
 			List<MsgService> msgs = JakeMainApp.getCore().getMsgServics();
 
 			if (msgs != null) {
 				for (MsgService msg : msgs) {
 					UserControlPanel userPanel = new UserControlPanel(msg);
 					JXLayer userLayer = new JXLayer(userPanel);
 					//userLayer.setBackground(Color.WHITE);
 					//userLayer.set(new Color(0, 128, 0, 128));
 					//userLayer.setOpaque(true);
 					userListPanel.add(userLayer);
 				}
 			}
 		} catch (FrontendNotLoggedInException e) {
 			ExceptionUtilities.showError(e);
 		}
 	}
 
 
 	/**
 	 * Creates the Add User Panel
 	 *
 	 * @return
 	 */
 	private JPanel createAddUserPanel() {
 		log.debug("creting add user panel...");
 
 		// create the add user panel
 		JPanel addUserPanel = new JPanel(new MigLayout("wrap 1, filly, center, ins 0"));
 		addUserPanel.setOpaque(false);
 
 		// the say hello heading
 		JPanel titlePanel = new JPanel(new MigLayout("nogrid, fill, top, ins 0"));
 		titlePanel.setOpaque(false);
 		JLabel createAccountLabel = new JLabel(getResourceMap().getString("loginMessageLabel"));
 		createAccountLabel.setIcon(jakeWelcomeIcon);
 		createAccountLabel.setVerticalTextPosition(JLabel.TOP);
 		titlePanel.add(createAccountLabel, "top, center, h 80!");
 		addUserPanel.add(titlePanel, "wrap, gapbottom 20, top, grow, h 80:300");
 
 		// login existing with service
 		loginRadioButton = new JRadioButton(getResourceMap().getString("loginRadioButton"));
 		loginRadioButton.setOpaque(false);
 		loginRadioButton.addActionListener(new ActionListener() {
 			@Override
 			public void actionPerformed(ActionEvent e) {
 				updateSignInRegisterMode();
 			}
 		});
 		loginRadioButton.setSelected(true);
 
 		// register new
 		registerRadioButton = new JRadioButton(getResourceMap().getString("registerRadioButton"));
 		registerRadioButton.setOpaque(false);
 		registerRadioButton.addActionListener(new ActionListener() {
 			@Override
 			public void actionPerformed(ActionEvent e) {
 				updateSignInRegisterMode();
 			}
 		});
 
 		loginRegisterButtonGroup = new ButtonGroup();
 		loginRegisterButtonGroup.add(registerRadioButton);
 		loginRegisterButtonGroup.add(loginRadioButton);
 
 		// login service
 		String[] loginServices = new String[]{"Google Talk", "Jabber", "United Internet (GMX, Web.de)"};
 		Integer[] indexes = new Integer[]{0, 1, 2};
 		ImageIcon[] images = new ImageIcon[3];
 		images[0] = new ImageIcon(Toolkit.getDefaultToolkit().getImage(
 				  getClass().getResource("/icons/service-google.png")).getScaledInstance(16, 16, Image.SCALE_SMOOTH));
 		images[1] = new ImageIcon(Toolkit.getDefaultToolkit().getImage(
 				  getClass().getResource("/icons/service-jabber.png")).getScaledInstance(16, 16, Image.SCALE_SMOOTH));
 		images[2] = new ImageIcon(Toolkit.getDefaultToolkit().getImage(
 				  getClass().getResource("/icons/service-unitedinternet.png")).getScaledInstance(16, 16, Image.SCALE_SMOOTH));
 		loginServiceCheckBox = new JComboBox();
 		loginServiceCheckBox.setModel(new DefaultComboBoxModel(indexes));
 		IconComboBoxRenderer renderer = new IconComboBoxRenderer(images, loginServices);
 		loginServiceCheckBox.setRenderer(renderer);
 		loginServiceCheckBox.setEditable(false);
 		loginServiceCheckBox.addActionListener(new ActionListener() {
 			@Override
 			public void actionPerformed(ActionEvent e) {
 				updateLoginUsernameLabel();
 			}
 		});
 
 		// add to user panel
 		addUserPanel.add(loginRadioButton, "split");
 		addUserPanel.add(loginServiceCheckBox, "wrap");
 		loginUserDataPanel = new UserDataPanel(false);
 		updateLoginUsernameLabel();
 		addUserPanel.add(loginUserDataPanel, "hidemode 1");
 
 		// add the register radio button
 		registerUserDataPanel = new UserDataPanel(true);
 		addUserPanel.add(registerRadioButton, "");
 
 		addUserPanel.add(registerUserDataPanel, "hidemode 1");
 
 		JPanel buttonPanel = new JPanel(new MigLayout("wrap 2, fill, ins 0"));
 		buttonPanel.setOpaque(false);
 
 		workingAnimation = new JAsynchronousProgressIndicator();
 		buttonPanel.add(workingAnimation, "hidemode 1, left");
 
 		signInRegisterButton = new JButton();
 		signInRegisterButton.putClientProperty("JButton.buttonType", "textured");
 		signInRegisterButton.addActionListener(new ActionListener() {
 			@Override
 			public void actionPerformed(ActionEvent e) {
 				signInRegisterButtonPressed();
 			}
 		});
 		updateSignInRegisterMode();
 
 		// add back button if there are users
 		try {
 			if (JakeMainApp.getCore().getMsgServics().size() > 0) {
 				log.debug("adding back button: getMsgServics().size()=" + JakeMainApp.getCore().getMsgServics().size());
 				JButton backBtn = new JButton(getResourceMap().getString("backBtn"));
 				backBtn.putClientProperty("JButton.buttonType", "textured");
 				backBtn.addActionListener(new ActionListener() {
 					@Override
 					public void actionPerformed(ActionEvent e) {
 						showPanel(UserPanels.ManageUsers);
 					}
 				});
 				buttonPanel.add(backBtn, "left, bottom, split");
 			}
 		} catch (FrontendNotLoggedInException e) {
 			ExceptionUtilities.showError(e);
 		}
 
 		buttonPanel.add(signInRegisterButton, "right, bottom, wrap");
 		addUserPanel.add(buttonPanel, "width 370!");
 		return addUserPanel;
 	}
 
 	/**
 	 * Updates internal saved ServiceCredentials and return object.
 	 *
 	 * @return
 	 */
 	private ServiceCredentials getCredientals() {
 		if (isModeSignIn()) {
 			cred.setUserId(loginUserDataPanel.getUserName());
 			cred.setPlainTextPassword(loginUserDataPanel.getPassword());


 		} else {
 			cred.setUserId(loginUserDataPanel.getUserName());
 			cred.setPlainTextPassword(loginUserDataPanel.getPassword());
 		}
 
 		return cred;
 	}
 
 	/**
 	 * Action that is called when Button Sign In / Register is pressed.
 	 */
 	public void signInRegisterButtonPressed() {
 		log.info("Sign In / Registering (isSignIn=" + isModeSignIn());
 
 		if (isSignInRegisterButtonEnabled()) {
 
 			if (isModeSignIn()) {
 				try {
 					// sync call
 					MsgService msg = JakeMainApp.getCore().addAccount(getCredientals());
 					JakeMainApp.setMsgService(msg);
					JakeExecutor.exec(new LoginAccountWorker(msg));
 
 				} catch (Exception e) {
 					log.warn(e);
 					ExceptionUtilities.showError(e);
 				} finally {
 					updateView();
 				}
 			} else {
 				JakeExecutor.exec(new RegisterAccountWorker(getCredientals()));
 				// TODO
 			}
 		}
 	}
 
 
 	/**
 	 * Private inner worker for account registration.
 	 */
 	private class RegisterAccountWorker extends SwingWorkerWithAvailableLaterObject<Void> {
 		private ServiceCredentials cred;
 
 		private RegisterAccountWorker(ServiceCredentials cred) {
 			this.cred = cred;
 		}
 
 		@Override
 		protected AvailableLaterObject<Void> calculateFunction() {
 			workingAnimation.startAnimation();
 
 			try {
 				return JakeMainApp.getCore().createAccount(cred, this);
 			} catch (FrontendNotLoggedInException e) {
 				log.warn(e);
 				ExceptionUtilities.showError(e);
 			} catch (InvalidCredentialsException e) {
 				log.warn(e);
 				ExceptionUtilities.showError(e);
 			} catch (ProtocolNotSupportedException e) {
 				log.warn(e);
 				ExceptionUtilities.showError(e);
 			} catch (NetworkException e) {
 				log.warn(e);
 				ExceptionUtilities.showError(e);
 			}
 
 			workingAnimation.stopAnimation();
 			return null;
 		}
 
 		@Override
 		protected void done() {
 			workingAnimation.stopAnimation();
 
 			updateView();
 		}
 	}
 
 
 	/**
 	 * Updates the login username in representation to selected service.
 	 */
 	private void updateLoginUsernameLabel() {
 		if (loginServiceCheckBox.getSelectedIndex() ==
 				  SupportedServices.Google.ordinal()) {
 			loginUserDataPanel.setUserLabel("usernameGoogle");
 		} else if (loginServiceCheckBox.getSelectedIndex() ==
 				  SupportedServices.Jabber.ordinal()) {
 			loginUserDataPanel.setUserLabel("usernameJabber");
 		} else {
 			loginUserDataPanel.setUserLabel("usernameUInternet");
 		}
 	}
 
 
 	/**
 	 * Creates User/Password Field for entering credientals
 	 *
 	 * @return
 	 */
 	private class UserDataPanel extends JPanel {
 		private JTextField userName;
 		private JTextField passName;
 		private JComboBox serverComboBox;
 		private JCheckBox rememberPassCheckBox;
 		private JLabel userLabel;
 
 		public UserDataPanel(boolean addServer) {
 			this.setLayout(new MigLayout("wrap 1, fill"));
 			this.setOpaque(false);
 
 			// add server
 			if (addServer) {
 				// fill the registraton info panel
 				registrationInfoPanel = new JPanel(new MigLayout("wrap 2, ins 0"));
 				JLabel registrationLabel1 = new JLabel(getResourceMap().getString("registrationLabel1"));
 				registrationLabel1.setForeground(Color.DARK_GRAY);
 				registrationInfoPanel.add(registrationLabel1, "span 2, wrap");
 				JLabel registrationLabel2 = new JLabel(getResourceMap().getString("registrationLabel2"));
 				registrationLabel2.setForeground(Color.DARK_GRAY);
 				registrationInfoPanel.add(registrationLabel2);
 				LinkAction linkAction = new LinkAction(getResourceMap().getString("registrationLabel3")) {
 					public void actionPerformed(ActionEvent e) {
 						try {
 							Desktop.getDesktop().browse(new URI(getResourceMap().getString("registrationLabelHyperlink")));
 						} catch (IOException e1) {
 							e1.printStackTrace();
 						} catch (URISyntaxException e1) {
 							e1.printStackTrace();
 						}
 						setVisited(true);
 					}
 				};
 				registrationInfoPanel.add(new JXHyperlink(linkAction), "gapbottom 10");
 				registrationInfoPanel.setOpaque(false);
 				this.add(registrationInfoPanel);
 
 				JLabel serverLabel = new JLabel(getResourceMap().getString("serverLabel"));
 				serverLabel.setForeground(Color.DARK_GRAY);
 				serverComboBox = new JComboBox();
 				serverComboBox.setModel(new DefaultComboBoxModel(new String[]{"jabber.fsinf.at", "jabber.org",
 						  "jabber.ccc.de", "macjabber.de", "swissjabber.ch", "binaryfreedom.info"}));
 				serverComboBox.setEditable(true);
 
 				this.add(serverLabel, "");
 				this.add(serverComboBox, "width 350!");
 			}
 			userLabel = new JLabel(getResourceMap().getString("usernameLabel"));
 			userLabel.setForeground(Color.DARK_GRAY);
 			this.add(userLabel);
 
 			userName = new JTextField();
 			this.add(userName, "width 350!");
 
 			JLabel passLabel = new JLabel(getResourceMap().getString("passwordLabel"));
 			passLabel.setForeground(Color.DARK_GRAY);
 			this.add(passLabel);
 
 			passName = new JPasswordField();
 			this.add(passName, "width 350!");
 
 			rememberPassCheckBox = new JCheckBox(getResourceMap().getString("rememberPasswordCheckBox"));
 			rememberPassCheckBox.setSelected(true);
 			rememberPassCheckBox.setOpaque(false);
 			this.add(rememberPassCheckBox, addServer ? "" : "split");
 
 			DocumentListener dl = new DocumentListener() {
 				public void insertUpdate(DocumentEvent documentEvent) {
 					updateSignInRegisterMode();
 				}
 
 				public void removeUpdate(DocumentEvent documentEvent) {
 					updateSignInRegisterMode();
 				}
 
 				public void changedUpdate(DocumentEvent documentEvent) {
 					updateSignInRegisterMode();
 				}
 			};
 
 			// instlal event listener for password text field
 			userName.getDocument().addDocumentListener(dl);
 			passName.getDocument().addDocumentListener(dl);
 
 			if (!addServer) {
 				// Advanced Settings
 				JButton loginAdvancedBtn = new JButton(getResourceMap().getString("advancedServerButton"));
 				loginAdvancedBtn.putClientProperty("JButton.buttonType", "textured");
 				loginAdvancedBtn.addActionListener(new ActionListener() {
 					@Override
 					public void actionPerformed(ActionEvent e) {
 						AdvancedAccountSettingsDialog.showDialog(getCredientals());
 					}
 				});
 				this.add(loginAdvancedBtn, "wrap");
 			}
 		}
 
 		/**
 		 * Get Username from internal TextField.
 		 *
 		 * @return
 		 */
 		public String getUserName() {
 			return userName.getText();
 		}
 
 		/**
 		 * Get Password from internal TextField.
 		 *
 		 * @return
 		 */
 		public String getPassword() {
 			return passName.getText();
 		}
 
 		/**
 		 * Get selected server string from JComboBox.
 		 *
 		 * @return
 		 */
 		public String getServer() {
 			return serverComboBox != null ? serverComboBox.getSelectedItem().toString() : null;
 		}
 
 		/**
 		 * Save password?
 		 *
 		 * @return
 		 */
		public boolean isRememberPassword() {
 			return rememberPassCheckBox.isSelected();
 		}
 
 		/**
 		 * Set the user label text.
 		 * translates the string.
 		 *
 		 * @param str
 		 */
 		public void setUserLabel(String str) {
 			userLabel.setText(getResourceMap().getString(str));
 		}
 	}
 
 	/**
 	 * Create the success panel for correct user adding.
 	 * This Panel has Drag&Drop-Abilities.
 	 *
 	 * @return
 	 */
 	private JPanel createSignInSuccessPanel() {
 
 		// create the drag & drop hint
 		JPanel loginSuccessPanel = new JPanel();
 		loginSuccessPanel.setTransferHandler(new ProjectDropHandler());
 		loginSuccessPanel.setOpaque(false);
 		loginSuccessPanel.setLayout(new MigLayout("nogrid, al center, fill"));
 
 		userLabelLoginSuccess = new JLabel();
 		loginSuccessPanel.add(userLabelLoginSuccess, "wrap");
 
 		// the sign out button
 		JButton signOutButton = new JButton(getResourceMap().getString("signInSuccessSignOut"));
 		signOutButton.putClientProperty("JButton.buttonType", "textured");
 		signOutButton.addActionListener(new ActionListener() {
 			public void actionPerformed(ActionEvent actionEvent) {
 
 				// TODO: more control over login state
 				//if (MsgServiceHelper.isUserLoggedIn()) {
 				try {
 					JakeMainApp.getMsgService().logout();
 					JakeMainApp.setMsgService(null);
 					updateView();
 				} catch (Exception e) {
 					log.warn(e);
 					ExceptionUtilities.showError(e);
 				}
 			}
 			//else {
 			//ExceptionUtilities.showError("No user is logged in!");
 			//}
 		});
 
 		loginSuccessPanel.add(signOutButton, "wrap, al center");
 
 		JLabel iconSuccess = new JLabel();
 		iconSuccess.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(
 				  getClass().getResource("/icons/dropfolder.png"))));
 
 		loginSuccessPanel.add(iconSuccess, "wrap, al center");
 
 		JLabel messageSuccess1 = new JLabel(getResourceMap().getString("dragDropHint1"));
 		messageSuccess1.setFont(Platform.getStyler().getH1Font());
 		messageSuccess1.setForeground(Color.DARK_GRAY);
 		loginSuccessPanel.add(messageSuccess1, "wrap, al center");
 
 		JLabel messageSuccess2 = new JLabel(getResourceMap().getString("dragDropHint2"));
 		messageSuccess2.setFont(Platform.getStyler().getH1Font());
 		messageSuccess2.setForeground(Color.DARK_GRAY);
 		loginSuccessPanel.add(messageSuccess2, "al center");
 
 		updateSignInSuccessPanel();
 
 		return loginSuccessPanel;
 	}
 
 	private void updateSignInSuccessPanel() {
 		if (JakeMainApp.getMsgService() != null) {
 			userLabelLoginSuccess.setText(JakeMainApp.getMsgService().getUserId().toString());
 		}
 	}
 
 	private boolean isModeSignIn() {
 		return loginRadioButton.isSelected();
 	}
 
 	private boolean isSignInRegisterButtonEnabled() {
 		if (isModeSignIn()) {
 			return (loginUserDataPanel.getUserName().length() > 0 &&
 					  loginUserDataPanel.getPassword().length() > 0);
 		} else {
 			return (registerUserDataPanel.getUserName().length() > 0 &&
 					  registerUserDataPanel.getPassword().length() > 0);
 
 		}
 	}
 
 	private void updateSignInRegisterMode() {
		log.info("updating signin/register mode.");
 
 		loginUserDataPanel.setVisible(isModeSignIn());
 		registerUserDataPanel.setVisible(!isModeSignIn());
 		loginServiceCheckBox.setEnabled(isModeSignIn());
 
 
 		if (isModeSignIn()) {
 			signInRegisterButton.setText(getResourceMap().getString("loginSignIn"));
 		} else {
 			signInRegisterButton.setText(getResourceMap().getString("loginRegister"));
 		}
 
 		// disable the button as long as no credidentals are entered
 		signInRegisterButton.setEnabled(isSignInRegisterButtonEnabled());
 	}
 
 	/**
 	 * Updates the main view.
 	 * If there is a user registered, show dragdrop screen.
 	 * If not, show the add user screen.
 	 */
 	private void updateView() {
 		log.info("updating login view. selected user: " + JakeMainApp.getMsgService() +
 				  ", user count: " + JakeMainApp.getCore().getMsgServics().size());
 
 		// always update everything
 		updateSignInSuccessPanel();
 		updateChooseUserPanel();
 
 		// update the view (maybe already logged in)
 		if (JakeMainApp.getMsgService() != null) {
 			showPanel(UserPanels.LoggedIn);
 		} else {
 			if (JakeMainApp.getCore().getMsgServics().size() > 0) {
 				showPanel(UserPanels.ManageUsers);
 			} else {
 				showPanel(UserPanels.AddUser);
 			}
 		}
 	}
 
 	/**
 	 * Set which panel will be shown
 	 *
 	 * @param panel
 	 */
 	private void showPanel(UserPanels panel) {
 		log.info("show panel: " + panel);
 		showContentPanel(addUserPanel, panel == UserPanels.AddUser);
 		showContentPanel(loginUserPanel, panel == UserPanels.ManageUsers);
 		showContentPanel(loginSuccessPanel, panel == UserPanels.LoggedIn);
 	}
 
 
 	/**
 	 * Helper for showPanel; add or remove certain panel at runtime.
 	 */
 	private void showContentPanel(JPanel panel, boolean show) {
 		if (show) {
 			this.add(panel, "grow");
 		} else {
 			this.remove(panel);
 		}
 		this.updateUI();
 	}
 
 
 	public void setRegistrationStatus(final RegisterStati status, final String msg) {
 		log.info("got registration status update: " + status);
 
 		Runnable runner = new Runnable() {
 			public void run() {
 
 				updateView();
 
 				// animation is controlled via swingworker
 
 				if (status == RegisterStati.RegistrationActive) {
 					signInRegisterButton.setText(getResourceMap().getString("loginRegisterProceed"));
 					signInRegisterButton.setEnabled(false);
 				}
 			}
 		};
 
 		SwingUtilities.invokeLater(runner);
 	}
 
 
 	public void setConnectionStatus(final ConnectionStati status, final String msg) {
 		log.info("got connection status update: " + status);
 
 		Runnable runner = new Runnable() {
 			public void run() {
 
 				// always update view
 				updateView();
 
 				if (status == ConnectionStati.SigningIn) {
 					signInRegisterButton.setText(getResourceMap().getString("loginSignInProceed"));
 					signInRegisterButton.setEnabled(false);
 				}
 			}
 		};
 
 		SwingUtilities.invokeLater(runner);
 	}
 
 	/**
 	 * Create User Panel
 	 */
 	private class UserControlPanel extends JXPanel {
 		private final MsgService msg;
 		private JPasswordField passField;
 		private JCheckBox rememberPassCheckBox;
 		private final static String MagicPassToken = "%MAGIC%";
 
 		public UserControlPanel(final MsgService msg) {
 			log.info("creating UserControlPanel with " + msg);
 			this.msg = msg;
 
 			this.setLayout(new MigLayout("wrap 2, fill"));
 
 			this.setBorder(BorderFactory.createLineBorder(Colors.LightBlue.color(), 1));
 			this.setOpaque(false);
 
 			JLabel userLabel = new JLabel(StringUtilities.htmlize("<b>" + msg.getUserId().getUserId() + "</b>"));
 			this.add(userLabel, "span 2, gapbottom 8");
 
 			JLabel passLabel = new JLabel(getResourceMap().getString("passwordLabel") + ":");
 			this.add(passLabel, "left");
 			passField = new JPasswordField();
 			this.add(passField, "w 200!");
 
 			rememberPassCheckBox = new JCheckBox(getResourceMap().getString("rememberPasswordCheckBox"));
 			rememberPassCheckBox.setSelected(true);
 			rememberPassCheckBox.setOpaque(false);
 			rememberPassCheckBox.addActionListener(new ActionListener() {
 				@Override
 				public void actionPerformed(ActionEvent e) {
 					updateUserPanel();
 				}
 			});
 			this.add(rememberPassCheckBox, "span 2");
 
 			JButton deleteUserBtn = new JButton(getResourceMap().getString("deleteUser"));
 			deleteUserBtn.putClientProperty("JButton.buttonType", "textured");
 			deleteUserBtn.addActionListener(new ActionListener() {
 				@Override
 				public void actionPerformed(ActionEvent e) {
 					try {
 						JakeMainApp.getCore().removeAccount(msg);
 						updateView();
 					} catch (Exception e1) {
 						ExceptionUtilities.showError(e1);
 					}
 				}
 			});
 			this.add(deleteUserBtn, "left, bottom");
 
 			JButton signInBtn = new JButton(getResourceMap().getString("loginSignInOnly"));
 			signInBtn.putClientProperty("JButton.buttonType", "textured");
 			signInBtn.addActionListener(new ActionListener() {
 				@Override
 				public void actionPerformed(ActionEvent e) {
 					try {
 						JakeMainApp.setMsgService(msg);
 
 						if (isMagicToken()) {
 							JakeExecutor.exec(new LoginAccountWorker(msg));
 						} else {
 							JakeExecutor.exec(new LoginAccountWorker(msg, getPassword(), isRememberPassword()));
 						}
 
 						updateView();
 					} catch (Exception e1) {
 						ExceptionUtilities.showError(e1);
 					}
 				}
 			});
 			this.add(signInBtn, "right, bottom");
 
 			// if a password is set, write a magic token into password field
 			// to represent the "not changed" state
 			if (msg.isPasswordSaved()) {
 				passField.setText(MagicPassToken);
 			}
 			rememberPassCheckBox.setSelected(msg.isPasswordSaved());
 		}
 
 
 		/**
 		 * Disables the Password Field if Password is saved.
 		 */
 		private void updateUserPanel() {
 			if (isMagicToken() && isRememberPassword()) {
 				passField.setEditable(false);
 			} else {
 				passField.setEditable(true);
 			}
 
 			// remove magic token if checkbox is removed
 			if (isMagicToken() && !isRememberPassword()) {
 				passField.setText("");
 			}
 		}
 
 		private boolean isMagicToken() {
 			return getPassword().compareTo(MagicPassToken) == 0;
 		}
 
 		/**
 		 * En/Disables the controls.
 		 * (Should be disabled, when logging in)
 		 *
 		 * @param enable
 		 */
 		public void enableControls(boolean enable) {
 			passField.setEnabled(enable);
 			updateUserPanel();
 		}
 
 		public boolean isRememberPassword() {
 			return rememberPassCheckBox.isSelected();
 		}
 
 		public String getPassword() {
 			return passField.getText();
 		}
 	}
 }
