 /*
  * To change this template, choose Tools | Templates
  * and open the template in the editor.
  */
 package gui;
 
 
 import org.apache.log4j.*;
 
 import data.Task;
 import logic.JIDLogic;
 
 //import com.seaglasslookandfeel.*;
 
 import javax.swing.ActionMap;
 import javax.swing.InputMap;
 import javax.swing.JButton;
 import javax.swing.JComponent;
 import javax.swing.JLabel;
 import javax.swing.JLayeredPane;
 import javax.swing.SwingConstants;
 import javax.swing.SwingUtilities;
 import javax.swing.Timer;
 import javax.swing.UIManager;
 
 import java.awt.BorderLayout;
 import java.awt.Color;
 import java.awt.Component;
 import java.awt.Container;
 import java.awt.Dimension;
 import java.awt.Graphics;
 import java.awt.Point;
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.awt.event.KeyAdapter;
 import java.awt.event.KeyEvent;
 import java.awt.event.MouseAdapter;
 import java.awt.event.MouseEvent;
 import java.awt.event.MouseListener;
 import java.awt.event.MouseMotionAdapter;
 
 import javax.swing.JTextField;
 
 import constant.OperationFeedback;
 
 /**
  * 
  * @author Ramon
  */
 public class MainJFrame extends javax.swing.JFrame {
 
 	private static Logger logger=Logger.getLogger(MainJFrame.class);
 	
 	enum STATE {
 		ADD, DELETE, EDIT, SEARCH, COMPLETED, ARCHIVE
 		, OVERDUE, NULL, LIST, UNDO, EXIT, HELP, REDO
 	};
 	
 	boolean edit = false;
 	STATE curState;
 	STATE prevState = STATE.NULL;
 	Task[] prevTasks;
 	Task[] tasks;
 	String prevText;
 	String id;
 	int prevIndex;
 	static boolean expand = false;
 	
 	// Variables declaration - do not modify
     private javax.swing.JLabel bgLabel;
     private javax.swing.JLabel button1;
     private javax.swing.JLabel button2;
     private javax.swing.JLabel button3;
     private javax.swing.JLabel downButton;
     private javax.swing.JComboBox jComboBox1;
     private javax.swing.JLayeredPane jLayeredPane1;
 	private javax.swing.JLabel logo;
 	private JLabel bg;
 	private MouseListener curdownButton;
 	private javax.swing.JPanel jPanel1;
 	private	JLayeredPane lp;
 	// End of variables declaration
 
 	private static Point point = new Point();
 	private static Point currentLocation = new Point(0,0);
 	private final boolean TEST = true;
 
 	// End of variables declaration
 
 	/**
 	 * Creates new form MainJFrame
 	 */
 	public MainJFrame() {
 		try {
 			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
 			/*
 			 * for (javax.swing.UIManager.LookAndFeelInfo info :
 			 * javax.swing.UIManager.getInstalledLookAndFeels()) { if
 			 * ("Nimbus".equals(info.getName())) {
 			 * javax.swing.UIManager.setLookAndFeel(info.getClassName());
 			 * UIManager. break; } }
 			 */
 		} catch (ClassNotFoundException ex) {
 			java.util.logging.Logger.getLogger(MainJFrame.class.getName()).log(
 					java.util.logging.Level.SEVERE, null, ex);
 		} catch (InstantiationException ex) {
 			java.util.logging.Logger.getLogger(MainJFrame.class.getName()).log(
 					java.util.logging.Level.SEVERE, null, ex);
 		} catch (IllegalAccessException ex) {
 			java.util.logging.Logger.getLogger(MainJFrame.class.getName()).log(
 					java.util.logging.Level.SEVERE, null, ex);
 		} catch (javax.swing.UnsupportedLookAndFeelException ex) {
 			java.util.logging.Logger.getLogger(MainJFrame.class.getName()).log(
 					java.util.logging.Level.SEVERE, null, ex);
 		}
 		// </editor-fold>
 
 		initComponents();
 		setAction();
 		this.setFocusable(true);
 		this.setDefaultCloseOperation(this.DO_NOTHING_ON_CLOSE);
 	
 		/*
 		 * Create and display the form
 		 */
 		java.awt.EventQueue.invokeLater(new Runnable() {
 
 			public void run() {
 				showFrame();
 			}
 		});
 		
 		addBindings();
 	}
 
 	/**
 	 * This method is called from within the constructor to initialize the form.
 	 * WARNING: Do NOT modify this code. The content of this method is always
 	 * regenerated by the Form Editor.
 	 */
 	@SuppressWarnings("unchecked")
 	// <editor-fold defaultstate="collapsed" desc="Generated Code">
 	private void initComponents() {
 		jComboBox1 = new javax.swing.JComboBox();
 		jComboBox1.setEditable(true);
 		jComboBox1.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
 		
         jLayeredPane1 = new javax.swing.JLayeredPane();
         button1 = new javax.swing.JLabel();
         button2 = new javax.swing.JLabel();
         button3 = new javax.swing.JLabel();
         logo = new javax.swing.JLabel();
         downButton = new javax.swing.JLabel();
         bgLabel = new javax.swing.JLabel();
 
         jComboBox1.setBounds(90, 40, 260, 30);
         jLayeredPane1.add(jComboBox1, 2);
 
         button1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
         button1.setIcon(Resource.helpImg);
         button1.setBounds(300, 0, 30, 30);
         jLayeredPane1.add(button1, javax.swing.JLayeredPane.DEFAULT_LAYER);
 
         button2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
         button2.setIcon(Resource.minimizeImg);
         button2.setBounds(330, 0, 30, 30);
         jLayeredPane1.add(button2, javax.swing.JLayeredPane.DEFAULT_LAYER);
 
         button3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
         button3.setIcon(Resource.exitImg); 
         button3.setBounds(360, 0, 30, 30);
         jLayeredPane1.add(button3, javax.swing.JLayeredPane.DEFAULT_LAYER);
 
         logo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
         logo.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
         logo.setIcon(Resource.bigLogo);
         logo.setBounds(10, 0, 70, 80);
         jLayeredPane1.add(logo, javax.swing.JLayeredPane.DEFAULT_LAYER);
 
         downButton.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
         downButton.setIcon(Resource.down);
         downButton.setBounds(360, 40, 30, 30);
         jLayeredPane1.add(downButton, javax.swing.JLayeredPane.DEFAULT_LAYER);
 
         jLayeredPane1.add(ExpandComponent.getJScrollPane(), JLayeredPane.DEFAULT_LAYER);
         
         bgLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);
         bgLabel.setIcon(Resource.smallBG);
         bgLabel.setBounds(0, 0, 400, 400);
         jLayeredPane1.add(bgLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
 
         
         javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
         getContentPane().setLayout(layout);
         layout.setHorizontalGroup(
             layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addComponent(jLayeredPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
         );
         layout.setVerticalGroup(
             layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addComponent(jLayeredPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 360, Short.MAX_VALUE)
         );
 
 		this.setIconImage((Resource.bigLogo).getImage());
 		this.setUndecorated(true);
 		this.setBackground(new Color(0,0,0,0));
 		this.pack();
 		this.setSize(400, 100);
 	}
 	
 	private void setAction() {
 		setJFrameAction();
 		setJComboBox1Action();
 		setlogoAction();
 		setbutton1Action();
 		setbutton2Action();
 		setbutton3Action();
 		setdownButtonActionExpand();
 	}
 	
 	private void setbutton2Action() {
 		button2.addMouseListener(new MouseAdapter() {
 
 			@Override
 			public void mouseClicked(MouseEvent arg0) {
 				MainJFrame.this.hideFrame();
 			}
 
 			@Override
 			public void mouseEntered(MouseEvent arg0) {
 				button2.setIcon(Resource.minimizeImgOn);
 			}
 
 			@Override
 			public void mouseExited(MouseEvent arg0) {
 				button2.setIcon(Resource.minimizeImg);
 			}
 
 		});
 		
 	}
 
 	private void setbutton1Action() {
 		//call help.
 	}
 
 	public void setdownButtonActionContract() {
 		downButton.setToolTipText("Contract");
 
 		downButton.setIcon(Resource.up);
 		
 		downButton.removeMouseListener(curdownButton);
 		downButton.addMouseListener(curdownButton = new MouseListener() {
 
 			@Override
 			public void mouseClicked(MouseEvent arg0) {
 				downButton.setIcon(Resource.upPress);
 				Timer timer = new Timer(100, new ActionListener() {
 
 					@Override
 					public void actionPerformed(ActionEvent arg0) {
 						downButton.setIcon(Resource.down);
 						if (MainJFrame.this.getSize().equals(
 								new Dimension(400, 400))) {
 							contractFrame();
 						} else {
 							expandFrame();
 						}
 					}
 
 				});
 				timer.setRepeats(false);
 				timer.start();
 			}
 
 			@Override
 			public void mouseEntered(MouseEvent arg0) {
 				downButton.setIcon(Resource.upOn);
 			}
 
 			@Override
 			public void mouseExited(MouseEvent arg0) {
 				downButton.setIcon(Resource.up);
 			}
 
 			@Override
 			public void mousePressed(MouseEvent arg0) {
 				downButton.setIcon(Resource.upPress);
 			}
 
 			@Override
 			public void mouseReleased(MouseEvent arg0) {
 				downButton.setIcon(Resource.down);
 			}
 		});
 	}
 
 	public void setdownButtonActionExpand() {
 		downButton.setToolTipText("Expand");
 
 		downButton.setIcon(Resource.down);
 		downButton.removeMouseListener(curdownButton);
 		
 		downButton.addMouseListener(curdownButton = new MouseListener() {
 
 			@Override
 			public void mouseClicked(MouseEvent arg0) {
 				downButton.setIcon(Resource.downPress);
 				
 				Timer timer = new Timer(100, new ActionListener() {
 
 					@Override
 					public void actionPerformed(ActionEvent arg0) {
 						downButton.setIcon(Resource.up);
 						if (MainJFrame.this.getSize().equals(
 								new Dimension(400, 400))) {
 							contractFrame();
 						} else {
 							expandFrame();
 						}
 					}
 
 				});
 				timer.setRepeats(false);
 				timer.start();
 				
 			}
 
 			@Override
 			public void mouseEntered(MouseEvent arg0) {
 				downButton.setIcon(Resource.downOn);
 			}
 
 			@Override
 			public void mouseExited(MouseEvent arg0) {
 				downButton.setIcon(Resource.down);
 			}
 
 			@Override
 			public void mousePressed(MouseEvent arg0) {
 				downButton.setIcon(Resource.downPress);
 			}
 
 			@Override
 			public void mouseReleased(MouseEvent arg0) {
 				downButton.setIcon(Resource.up);
 			}
 		});
 	}
 
 	private void setbutton3Action() {
 		button3.setToolTipText("Close");
 
 		button3.addMouseListener(new MouseAdapter() {
 
 			@Override
 			public void mouseClicked(MouseEvent arg0) {
 				if (TopPopUp.isShow())
 					TopPopUp.hideBox();
 				MainJFrame.this.setVisible(false);
 				JIDLogic.JIDLogic_close();
 				System.exit(0);
 			}
 
 			@Override
 			public void mouseEntered(MouseEvent arg0) {
 				button3.setIcon(Resource.exitOn);
 			}
 
 			@Override
 			public void mouseExited(MouseEvent arg0) {
 				button3.setIcon(Resource.exitImg);
 			}
 
 		});
 	}
 	
 	private void setlogoAction() {
 	}
 
 	private void setJComboBox1Action() {
 		
 		int index;
 		this.getButtonSubComponent(jComboBox1).setVisible(false);
 		final AutoCompletion jBoxCompletion = new AutoCompletion(jComboBox1);
 
 		final JTextField editorcomp = (JTextField) jComboBox1.getEditor()
 				.getEditorComponent();
 		editorcomp.setText("");
 		
 		editorcomp.addKeyListener(new KeyAdapter() {
 
 			@Override
 			public void keyPressed(KeyEvent arg0) {
 				final KeyEvent e = arg0;
 				 SwingUtilities.invokeLater(
 				      new Runnable() {
 					    	int curIndex;
 					    	String command;
 					    	String curText;
 							@Override
 							public void run() {
 
 						    	curText = editorcomp.getText();
 								jBoxCompletion.stopWorking();
 								//curText= editorcomp.getText();
 								curState= checkCommand(curText);curIndex= getIndex();
 								logger.debug("curText:" + curText);
 								logger.debug("prevText: " + prevText);
 								//logger.debug("cmd: " + command);
 								logger.debug("state: " +curState);
 								logger.debug("prev: " +prevState);
 								logger.debug("index: "+ curIndex);
 
 								if(prevState == STATE.NULL && curState!=prevState) {
 									command = new String(curText);
 								}
 								
 								if(curState == STATE.NULL && curState!=prevState) {
 									jBoxCompletion.setStandardModel();
 									//jBoxCompletion.startWorking();
 									jComboBox1.setSelectedIndex(-1);
 								}
 								
 								if(((curState == STATE.EDIT && !edit)
 									|| curState == STATE.DELETE
 									|| curState == STATE.SEARCH
 									|| curState == STATE.COMPLETED)
 									&& curText.length() > curState.toString().length() +1) {
 									if((e.getKeyCode() == KeyEvent.VK_BACK_SPACE || !e.isActionKey())
 									&& e.getKeyCode() != KeyEvent.VK_ENTER
 									&& e.getKeyCode() != KeyEvent.VK_UP
 									&& e.getKeyCode() != KeyEvent.VK_DOWN){
 										
 									 SwingUtilities.invokeLater(
 									new Runnable() {
 	
 										@Override
 										public void run() {
 											System.out
 													.println("***enter interstate: ");
 	
 											JIDLogic.setCommand(curState.toString().toLowerCase());
 	
 											logger.debug("********exeCmd: "
 													+ curText);
 											tasks = JIDLogic
 													.executeCommand(curText);
 	
 											jBoxCompletion.stopWorking();
 											jBoxCompletion
 													.setNewModel(taskArrayToString(tasks));
 											
 											jComboBox1.setPopupVisible(true);
 	
 											jComboBox1.setSelectedIndex(-1);
 											editorcomp.setText(curText);
 											//editorcomp.setText(curState.toString() + tasks[0]);
 	
 											if (tasks != null)
 												id = tasks[0].getTaskId();
 											else
 												id = "dummyString!@#$";
 										}
 	
 									});
 								}
 
 								if(curState != STATE.NULL &&
 										(e.getKeyCode()==KeyEvent.VK_UP || e.getKeyCode()==KeyEvent.VK_DOWN)) {
 										curText = prevText;
 										tasks = JIDLogic.executeCommand(curText);
 										id = tasks[jComboBox1.getSelectedIndex()].getTaskId();
 										editorcomp.setText(curText);
 										return;
 									}
 								}
 									
 								if(e.getKeyCode() == KeyEvent.VK_ENTER && curState!=STATE.NULL) {
 									String exeCmd = new String();
 									
 									logger.debug("*********************enter");
 									
 									if(curState != STATE.EDIT)
 										edit = false;
 									
 									switch (curState ){
 									case DELETE:
 									case COMPLETED:
 										
 										exeCmd = curState.toString().toLowerCase() + " "
 												+ id + " ";
 										break;
 									case EDIT:
 										if(!edit) {
 											exeCmd = curState.toString().toLowerCase() + " "
 													+ id;
 											editorcomp.setText(
 													curState.toString().toLowerCase() + " " 
 													+ storagecontroller.StorageManager.getTaskById(id));
 											edit = true;
 										}
 										else {
 											exeCmd = curText;
 											edit = false;
 										}
 										break;
 									case ADD:
 										exeCmd = curText;
 										break;
 									case SEARCH:
 										exeCmd = curText;
 										break;
 									case LIST:
 									case UNDO:
 										exeCmd = curText;									
 									case OVERDUE:
 										exeCmd = curText;
 									}
 									
 									logger.debug("********exeCmd: " + exeCmd);
 									JIDLogic.setCommand(curState.toString());
 									tasks = JIDLogic.executeCommand(exeCmd);
 									
 									switch(curState) {
 									case DELETE:
 									case COMPLETED:				
 									case ADD:
 									case EDIT:
 										if(!edit) {
 											if(tasks!=null) {
 												showPopup( curState.toString()+ " " 
 														+ tasks[0]);
 												ExpandComponent.updateJTable();
 											}
 										}
 									break;									
 									case SEARCH:
 										ExpandComponent.updateJTable(tasks);
 										expandFrame();
 									break;
 									case LIST:
 										ExpandComponent.updateJTable();
 										expandFrame();
 									break;
									case UNDO:
										break;
 									case EXIT:
 										JIDLogic.JIDLogic_close();
 										System.exit(0);
 										break;
 									case OVERDUE:
 										new Action.OverdueAction().actionPerformed(null);
 									break;
									case REDO:
										new Action.RedoAction().actionPerformed(null);
									break;
 									}
 									
 									if(UIController.getOperationFeedback() == OperationFeedback.VALID && !edit) {
 										
 										jBoxCompletion.setStandardModel();
 										editorcomp.setText("");
 										curState = STATE.NULL;
 										UIController.refresh();
 									}
 									else {
 										UIController.showInvalidDisplay();
 										UIController.sendOperationFeedback(OperationFeedback.VALID);
 									}
 								}
 								
 								prevState = curState;
 								prevIndex = curIndex;
 								prevText = curText;
 								prevTasks = tasks;
 							}
 							
 
 
 							private int getIndex() {
 								int idx = jComboBox1.getSelectedIndex();
 								
 								if(idx <0 ) return idx;
 								
 								String selected = (String)jComboBox1.getItemAt(idx);
 								
 							//	if(curText.length() <= selected.length() 
 							//		&& selected.substring(0, curText.length()).equalsIgnoreCase(curText))
 									return idx;
 								
 							//	return -1;
 							}
 
 							private String[] taskArrayToString (Task[] tasks) {
 								if(tasks!=null) {
 									String[] strings = new String[tasks.length];
 									for(int i=0; i<tasks.length; i++)
 										strings[i]= curState.toString() + " " 
 												+ tasks[i];
 									
 									
 									logger.debug("str[0]: "+strings[0]);
 									return strings;
 								}
 								else {
 									return null;
 								}
 							}
 							
 							private STATE checkCommand(String curText) {
 								String delims = "[ ]+";
 								String firstWord = curText.split(delims)[0];
 								if(firstWord.equalsIgnoreCase("add") 
 										|| firstWord.equalsIgnoreCase("insert"))
 									return STATE.ADD;
 								if(firstWord.equalsIgnoreCase("delete")
 										|| firstWord.equalsIgnoreCase("remove"))
 									return STATE.DELETE;
 								if(firstWord.equalsIgnoreCase("modify")
 										|| firstWord.equalsIgnoreCase("edit")
 										|| firstWord.equalsIgnoreCase("update"))
 									return STATE.EDIT;
 								if(firstWord.equalsIgnoreCase("search")
 										|| firstWord.equalsIgnoreCase("find"))
 									return STATE.SEARCH;
 								if(firstWord.equalsIgnoreCase("completed")
 										|| firstWord.equalsIgnoreCase("done"))
 									return STATE.COMPLETED;
 								if(firstWord.equalsIgnoreCase("archive"))
 									return STATE.ARCHIVE;
 								if(firstWord.equalsIgnoreCase("overdue"))
 									return STATE.OVERDUE;
 								if(firstWord.equalsIgnoreCase("list"))
 									return STATE.LIST;
 								if(firstWord.equalsIgnoreCase("undo"))
 									return STATE.UNDO;
 								if(firstWord.equalsIgnoreCase("exit"))
 									return STATE.EXIT;
 								if(firstWord.equalsIgnoreCase("help"))
 									return STATE.HELP;
								if(firstWord.equalsIgnoreCase("redu"))
 									return STATE.REDO;
 								return STATE.NULL;
 							} 
 
 				      } );
 				}
 
 
 		});
 
 		editorcomp.addMouseListener(new MouseAdapter() {
 
 			@Override
 			public void mouseClicked(MouseEvent arg0) {
 				editorcomp.selectAll();
 			}
 
 		});
 
 	}
 
 	private void setJFrameAction() {
 		addMouseListener(new MouseAdapter() {
 			public void mousePressed(MouseEvent e) {
 				point.x = e.getX();
 				point.y = e.getY();
 
 			}
 
 			@Override
 			public void mouseReleased(MouseEvent e) {
 				currentLocation = MainJFrame.this.getLocation();
 			}
 		});
 
 		addMouseMotionListener(new MouseMotionAdapter() {
 			public void mouseDragged(MouseEvent e) {
 				Point p = getLocation();
 				setLocation(p.x + e.getX() - point.x, p.y + e.getY() - point.y);
 				Point popupP = TopPopUp.jFrame.getLocation();
 				TopPopUp.setPosition(popupP.x + e.getX() - point.x, popupP.y
 							+ e.getY() - point.y);
 				
 			}
 		});
 
 	}
 
 	public static void showPopup(String str) {
 		logger.debug("-----------------POPUP-----------------------");
 		TopPopUp.setText(str);
 		TopPopUp.setPosition(currentLocation.x + 15, currentLocation.y - 30);
 		TopPopUp.showBox();
 		TopPopUp.jFrame.setFocusable(true);
 	}
 
 	public void showFrame() {
 		SwingUtilities.invokeLater(new Runnable() {
 
 			@Override
 			public void run() {
 				// TODO Auto-generated method stub
 				
 				MainJFrame.this.setVisible(true);
 				MainJFrame.this.toFront();
 				
 				jComboBox1.requestFocus();
 			}
 
 		});
 	}
 
 	public void hideFrame() {
 		SwingUtilities.invokeLater(new Runnable() {
 
 			@Override
 			public void run() {
 				TopPopUp.hideBox();
 				MainJFrame.this.setVisible(false);
 				UIController.showTrayMsg("Jot It Down!", "is hiding!");
 			}
 
 		});
 	}
 
 	public void setInputText(final String string) {
 		SwingUtilities.invokeLater(new Runnable() {
 
 			@Override
 			public void run() {
 				jComboBox1.setSelectedItem(string);
 				jComboBox1.getEditor().getEditorComponent()
 						.requestFocusInWindow();
 			}
 
 		});
 	}
 
 	private static JButton getButtonSubComponent(Container container) {
 		if (container instanceof JButton) {
 			return (JButton) container;
 		} else {
 			Component[] components = container.getComponents();
 			for (Component component : components) {
 				if (component instanceof Container) {
 					return getButtonSubComponent((Container) component);
 				}
 			}
 		}
 		return null;
 	}
 	
 	public void expandFrame() {
 		if(!expand) {
 			MainJFrame.this.setSize(400,400);
 			jLayeredPane1.setSize(400,400);
 			bgLabel.setIcon(Resource.largeBG);
 			expand = true;
 			setdownButtonActionContract();
 		}
 	}
 	
 	public void contractFrame() {
 		if (expand) {
 			MainJFrame.this.setSize(400, 100);
 			expand = false;
 			setdownButtonActionExpand();
 			bgLabel.setIcon(Resource.smallBG);
 		}
 	}
 	
 	public boolean isExpand() {
 		return expand;
 	}
 	
     protected void addBindings() {
         InputMap inputMap = this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
         ActionMap actionMap = this.getRootPane().getActionMap();
         
         new Binding(this, inputMap, actionMap);
     }
     
 }
