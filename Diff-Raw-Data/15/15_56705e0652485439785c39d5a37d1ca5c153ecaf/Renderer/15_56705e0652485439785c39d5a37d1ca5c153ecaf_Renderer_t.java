 import java.awt.BorderLayout;
 import java.awt.CardLayout;
 import java.awt.Color;
 import java.awt.Container;
 import java.awt.Dimension;
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.awt.event.MouseAdapter;
 import java.awt.event.MouseEvent;
 import java.awt.event.MouseListener;
 import java.awt.*;
 import java.awt.font.*;
 import java.awt.image.BufferedImage;
 
 import java.io.File;
 import java.io.IOException;
 
 import javax.imageio.ImageIO;
 
 import javax.swing.*;
 import javax.swing.JTextField;
 import javax.swing.JButton;
 import javax.swing.JFrame;
 import javax.swing.JPanel;
 import javax.swing.text.DefaultCaret;
 import javax.swing.ImageIcon;
 import javax.swing.JLabel;
 import javax.swing.UIManager;
 import javax.swing.BorderFactory;
 
 import java.util.concurrent.locks.*;
 import java.util.ArrayList;
 
 public class Renderer {
 
     private JFrame frame;
     private boolean waiting;
     private String[] states; // keeps track of the state of the GUI
     private ReentrantLock lock;
     protected static final int WIDTH = 9;
     protected static final int HEIGHT = 5;
     protected static final int TILE_SIZE = 100;
     private JButton[] buttons = new JButton[WIDTH + HEIGHT];
     private Timer timer;
 
     public Renderer() {
 
         frame = new JFrame("M.U.L.E.");
         frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         frame.setResizable(false);
         frame.setMinimumSize(new Dimension(950, 770));
         frame.setPreferredSize(new Dimension(950, 770));
         frame.setVisible(true);
         waiting = true;
         lock = new ReentrantLock();
         frame.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
         timer = createTimer(50000);
     }
 
     public String[] drawIntroScreen() {
         
         // declare initial variables
         String action = "";
         states = new String[1];
         states[0] = "new";
 
         ImagePanel panel = new ImagePanel("/media/startscreen.png");
         panel.setPreferredSize(new Dimension(950, 700));
         panel.setLayout(null);
 
         ImagePanel menuPanel = new ImagePanel("/media/bss.png");
         menuPanel.setPreferredSize(new Dimension(950, 50));
         menuPanel.setLayout(null);
 
         ArrayList<JPanel> panels = new ArrayList<JPanel>();
         panels.add(panel);
         panels.add(menuPanel);
 
         changePanel(frame, panels);
 
         // add buttons
         JButton quitButton = addButtonToPanel(menuPanel, 11, 7, 171, 40, 0, "quit");
         JButton loadButton = addButtonToPanel(menuPanel, 590, 7, 171, 40, 0, "load");
         JButton newButton = addButtonToPanel(menuPanel, 771, 7, 171, 40, 0, "new");
 
         blockForInput();
         exitSafely();
         return states;
     }
 
     // States[0] - Action to perform: {"back", "okay"}
     // States[1] - Difficulty: {"1", "2", "3"}
     // States[2] - Number of Players: {"1", "2", "3", "4", "5"}
     public String[] drawSetupScreen() {
 
         // declare initial variables
         String action = "";
         states = new String[3];
         states[0] = "okay";
         states[1] = "2";
         states[2] = "1";
 
         ImagePanel panel = new ImagePanel("/media/gamesetup.png");
         panel.setPreferredSize(new Dimension(950, 525));
         panel.setLayout(null);
 
         JPanel playerPanel = new JPanel();
         playerPanel.setPreferredSize(new Dimension(950, 175));
         playerPanel.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
         for (int i = 0; i < 6; i++) {
             ImagePanel playerBox = new ImagePanel("/media/p" + i + "0.png");
             if (i == 0) {
                 playerBox.setPreferredSize(new Dimension(160, 175));
             }
             else {
                 playerBox.setPreferredSize(new Dimension(158, 175));
             }
             playerPanel.add(playerBox);
         }
 
         ImagePanel menuPanel = new ImagePanel("/media/bp0.png");
         menuPanel.setPreferredSize(new Dimension(950, 50));
         menuPanel.setLayout(null);
 
         //drawGameStatus(players, playerPanel, currPlayer);
 
         ArrayList<JPanel> panels = new ArrayList<JPanel>();
         panels.add(panel);
         panels.add(playerPanel);
         panels.add(menuPanel);
 
         changePanel(frame, panels);
 
         // add buttons
         JButton backButton = addButtonToPanel(menuPanel, 11, 7, 171, 40, 0, "back");
         JButton okayButton = addButtonToPanel(menuPanel, 771, 7, 171, 40, 0, "okay");
         JButton easyButton = addButtonToPanel(panel, 160, 164, 77, 40, 1, "1");
         JButton mediumButton = addButtonToPanel(panel, 407, 164, 137, 38, 1, "2");
         JButton hardButton = addButtonToPanel(panel, 715, 164, 78, 38, 1, "3");
         JButton onePlayer = addButtonToPanel(panel, 185, 404, 24, 40, 2, "1");
         JButton twoPlayer = addButtonToPanel(panel, 325, 404, 24, 40, 2, "2");
         JButton threePlayer = addButtonToPanel(panel, 465, 404, 24, 40, 2, "3");
         JButton fourPlayer = addButtonToPanel(panel, 605, 404, 24, 40, 2, "4");
         JButton fivePlayer = addButtonToPanel(panel, 745, 404, 24, 40, 2, "5");
 
         blockForInput();
         exitSafely();
         return states;
     }
 
     // States[0] - Action to perform: {"new", "load", "quit"}
     // States[1] - Map Number: {"1", "2", "3", "4", "5"}
     public String[] drawMapScreen() {
 
         // declare initial variables
         String action = "";
         states = new String[2];
         states[0] = "okay";
         states[1] = "1";
 
         ImagePanel panel = new ImagePanel("/media/mapselection.png");
         panel.setPreferredSize(new Dimension(950, 525));
         panel.setLayout(null); 
 
         JPanel playerPanel = new JPanel();
         playerPanel.setPreferredSize(new Dimension(950, 175));
         playerPanel.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
         for (int i = 0; i < 6; i++) {
             ImagePanel playerBox = new ImagePanel("/media/p" + i + "0.png");
             if (i == 0) {
                 playerBox.setPreferredSize(new Dimension(160, 175));
             }
             else {
                 playerBox.setPreferredSize(new Dimension(158, 175));
             }
             playerPanel.add(playerBox);
         }
 
         ImagePanel menuPanel = new ImagePanel("/media/bp0.png");
         menuPanel.setPreferredSize(new Dimension(950, 50));
         menuPanel.setLayout(null);
 
         ArrayList<JPanel> panels = new ArrayList<JPanel>();
         panels.add(panel);
         panels.add(playerPanel);
         panels.add(menuPanel);
         changePanel(frame, panels);
 
         // add buttons
         JButton backButton = addButtonToPanel(menuPanel, 11, 7, 170, 40, 0, "back");
         JButton okayButton = addButtonToPanel(menuPanel, 770, 7, 170, 40, 0, "okay");
         JButton map1Button = addButtonToPanel(panel, 110, 162, 224, 126, 1, "1");
         JButton map2Button = addButtonToPanel(panel, 365, 162, 224, 126, 1, "2");
         JButton map3Button = addButtonToPanel(panel, 617, 162, 224, 126, 1, "3");
         JButton map4Button = addButtonToPanel(panel, 235, 317, 224, 126, 1, "4");
         JButton map5Button = addButtonToPanel(panel, 490, 317, 224, 126, 1, "5");
 
         blockForInput();
         exitSafely();
         return states;
     }
 
     // REFACTOR THIS BIT TO MAKE PANELS MORE STATIC -ALex
 
     // States[0] - Action to perform: {"new", "load", "quit"}
     // States[1] - Race: {"human", "elephant", "squirrel", "frog", "cat"}
     // States[2] - Player Name
     // States[3] - Color: {"red", "blue", "pink", "green", "orange"}
     public String[] drawCharacterScreen() {
 
         // declare initial variables
         String action = "";
         states = new String[4];
         states[0] = "okay";
         states[1] = "human";
         states[2] = "default";
         states[3] = "red";
 
         ImagePanel panel = new ImagePanel("/media/playerselection.png");
         panel.setPreferredSize(new Dimension(950, 525));
         panel.setLayout(null);
 
 
         JPanel playerPanel = new JPanel();
         playerPanel.setPreferredSize(new Dimension(950, 175));
         playerPanel.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
         for (int i = 0; i < 6; i++) {
             ImagePanel playerBox = new ImagePanel("/media/p" + i + "0.png");
             if (i == 0) {
                 playerBox.setPreferredSize(new Dimension(160, 175));
             }
             else {
                 playerBox.setPreferredSize(new Dimension(158, 175));
             }
             playerPanel.add(playerBox);
         }
 
         ImagePanel menuPanel = new ImagePanel("/media/bp0.png");
         menuPanel.setPreferredSize(new Dimension(950, 50));
         menuPanel.setLayout(null);
 
         ArrayList<JPanel> panels = new ArrayList<JPanel>();
         panels.add(panel);
         panels.add(playerPanel);
         panels.add(menuPanel);
         changePanel(frame, panels);
 
         // add buttons
         JButton backButton = addButtonToPanel(menuPanel, 11, 7, 170, 40, 0, "back");
         JButton okayButton = addButtonToPanel(menuPanel, 771, 7, 170, 40, 0, "okay");
         JButton humanButton = addButtonToPanel(panel, 75, 78, 133, 115, 1, "human");
         JButton elephantButton = addButtonToPanel(panel, 232, 78, 133, 115, 1, "elephant");
         JButton squirrelButton = addButtonToPanel(panel, 413, 78, 123, 115, 1, "squirrel");
         JButton frogButton = addButtonToPanel(panel, 593, 78, 98, 115, 1, "frog");
         JButton catButton = addButtonToPanel(panel, 763, 78, 98, 115, 1, "cat");
         JButton redButton = addButtonToPanel(panel, 92, 250, 130, 200, 3, "red");
         JButton blueButton = addButtonToPanel(panel, 260, 250, 130, 200, 3, "blue");
         JButton pinkButton = addButtonToPanel(panel, 427, 250, 130, 200, 3, "pink");
         JButton greenButton = addButtonToPanel(panel, 587, 250, 130, 200, 3, "green");
         JButton orangeButton = addButtonToPanel(panel, 750, 250, 130, 200, 3, "orange");
 
         JTextField nameBox = addTextToPanel(panel, 420, 480, 225, 38);
 
         blockForInputCharacter(panel);
         exitSafely();
         states[2] = nameBox.getText();
         return states;
     }
 
     public String[] drawTownScreen(ArrayList<Player> players, int currPlayer) {
 
         states = new String[2];
     
         ImagePanel panel = new ImagePanel("/media/town.png");
         panel.setPreferredSize(new Dimension(950, 525));
         panel.setLayout(null);
 
         JPanel playerPanel = new JPanel();
         playerPanel.setPreferredSize(new Dimension(950, 175));
         playerPanel.setLayout(null);
 
         drawGameStatus(players, playerPanel, currPlayer);
 
         ImagePanel menuPanel = new ImagePanel("/media/bp1.png");
         menuPanel.setPreferredSize(new Dimension(950, 50));
         menuPanel.setLayout(null);
 
         ArrayList<JPanel> panels = new ArrayList<JPanel>();
         panels.add(panel);
         panels.add(playerPanel);
         panels.add(menuPanel);
         changePanel(frame, panels);
 
         // buttons
         addButtonToPanel(panel, 60, 60, 200, 400, 0, "assay");
         addButtonToPanel(panel, 260, 60, 250, 400, 0, "store");
         addButtonToPanel(panel, 510, 60, 210, 400, 0, "land office");
         addButtonToPanel(panel, 720, 60, 200, 400, 0, "pub"); 
 
         blockForInput();
         exitSafely();
         states[1] = "" + timer.getDelay();
         return states;
     }
 
     // State[0] = {"town", "time"}
     // State[1] = time left on timer
     public String[] drawMainGameScreen(Map map, ArrayList<Player> players, int currPlayer) {
 
         states = new String[2];
 
         ImagePanel panel = new ImagePanel("/media/map"+map.getMapNum()+".png");
         panel.setPreferredSize(new Dimension(950, 525));
         panel.setLayout(null);
 
         JPanel playerPanel = new JPanel();
         playerPanel.setPreferredSize(new Dimension(950, 175));
         playerPanel.setLayout(null);
 
         drawPlayerFlags(map, panel);
         drawGameStatus(players, playerPanel, currPlayer);
 
         ImagePanel menuPanel = new ImagePanel("/media/bp1.png");
         menuPanel.setPreferredSize(new Dimension(950, 50));
         menuPanel.setLayout(null);
 
         ArrayList<JPanel> panels = new ArrayList<JPanel>();
         panels.add(panel);
         panels.add(playerPanel);
         panels.add(menuPanel);
         changePanel(frame, panels);
 
         JButton[] buttons = new JButton[Map.WIDTH * Map.HEIGHT];
         for (int i = 0; i < Map.HEIGHT; i++) {
             for (int j = 0; j < Map.WIDTH; j++) {
                 buttons[i * Map.WIDTH + j] = addButtonToPanel(panel, 25 + j * 100, 25 + i * 100, 100, 100, 0, "" + (i * Map.WIDTH + j));
             }
         }
 
         blockForInput();
         exitSafely();
         states[1] = "" + timer.getDelay();
         return states;
     }
 
     public void drawLoadScreen(LoadScreenModel model) {
         return;
     }
     
     // helper methods
 
     private void changePanel(JFrame frame, ArrayList<JPanel> panels) {
         frame.getContentPane().removeAll();        
         for (JPanel panel : panels) {
             frame.add(panel);
         }
         frame.pack();
         frame.repaint();
         return;
     }
 
     private void blockForInput() {
         // wait for a button to be clicked
         boolean waitingSafe = true; // used to avoid race condition
         while (waitingSafe) {
             try {
                 lock.lock();
                 waitingSafe = waiting;
             }
             finally {
                 lock.unlock();
             }
         }
     }
 
     private JLabel blockForInputCharacter(JPanel panel) {
         // wait for a button to be clicked
         JLabel colors = addLabelToPanel(panel, 70, 250, 804, 200, "/media/" + states[1] + ".png");
         panel.repaint();
         String oldState = states[1];
         boolean waitingSafe = true; // used to avoid race condition
         while (waitingSafe) {
             if (!oldState.equals(states[1])) {
                 panel.remove(colors);
                 colors = addLabelToPanel(panel, 70, 250, 804, 200, "/media/" + states[1] + ".png");
                 panel.repaint();
                 oldState = states[1];
             }
 
             try {
                 lock.lock();
                 waitingSafe = waiting;
             }
             finally {
                 lock.unlock();
             }
         }
         return colors;
     }
 
     private void exitSafely() {
         try {
             lock.lock();
             waiting = true;
         }
         finally {
             lock.unlock();
         }
     }
 
     private JButton addButtonToPanel(JPanel panel, int x, int y, int width, int height, 
         final int stateNum, final String stateText) {
         final JButton button = new JButton();
         button.setBounds(x, y, width, height);
         panel.add(button);
         button.addMouseListener(new MouseAdapter() {
             public void mouseClicked(MouseEvent e) {
                 states[stateNum] = stateText; // set the new state
 
                 System.out.println(stateNum + " set to: " + stateText);
                 if (stateNum == 0) {
                     try {
                         lock.lock();
                         waiting = false;
                     }
                     finally {
                         lock.unlock();
                     }
                 }
             }
         });
 
         button.setOpaque(false);
         button.setContentAreaFilled(false);
         button.setBorderPainted(false);
         return button;
     }
 
     private Timer createTimer(int time) {
         ActionListener timerListener = new ActionListener() {
             public void actionPerformed(ActionEvent e) {
                 states[0] = "time";
                 try {
                     lock.lock();
                     waiting = false;
                 } 
                 finally {
                     lock.unlock();
                 }
             }
         };
         Timer timer = new Timer(time, timerListener);
         return timer;
     }
 
     public void stopTimer() {
         timer.stop();
     }
 
     public void startTimer() {
         timer.start();
     }
 
     public void restartTimer() {
         timer.restart();
     }
 
     private JTextField addTextToPanel(JPanel panel, int x, int y, int width, int height) {
         final JTextField text = new JTextField("Enter Name");
         text.addMouseListener(new MouseAdapter(){
             public void mouseClicked(MouseEvent e){
                 text.setText("");
             }
         });
         text.setBounds(x, y, width, height);
         DefaultCaret c = (DefaultCaret)text.getCaret();
         c.setVisible(true);
         c.setDot(0);
         text.setFont(new Font("Candara", Font.PLAIN, 30));
         text.setHorizontalAlignment(JTextField.LEFT);
         text.setForeground(Color.WHITE);
         text.setBackground(new Color(87, 51, 4));
         text.setOpaque(false);
         text.setCaretColor(Color.WHITE);
         text.setBorder(javax.swing.BorderFactory.createEmptyBorder());
         panel.add(text);
         return text;
     }
 
     private JLabel addLabelToPanel(JPanel panel, int x, int y, int width, int height, String image) {
         BufferedImage img;
         try {
             img = ImageIO.read(getClass().getResourceAsStream(image));
         }
         catch (Exception e) {
             System.out.println("Caught: " + e);
             return null;
         }
 
         ImageIcon icon = new ImageIcon(img);
         JLabel label = new JLabel();
         label.setIcon(icon);
         label.setBounds(x, y, width, height);
         panel.add(label);
         return label;
     }
 
     private void drawGameStatus(ArrayList<Player> players, JPanel panel, int currPlayer) {
         System.out.println("Size: " + players.size());
         for (int i = 0; i < players.size(); i++) {
             drawPlayerStatus(players.get(i), i, panel);
         }
 
         // current player color
         String colorPrefix = players.get(currPlayer).getColor().substring(0, 1);
         BufferedImage colorImg;
         try {
             colorImg = ImageIO.read(getClass().getResourceAsStream("/media/circ" + colorPrefix + ".png"));
         }
         catch (Exception e) {
             System.out.println("Caught: " + e);
             return;
         }
 
         JLabel colorLabel = new JLabel();
         ImageIcon colorIcon = new ImageIcon(colorImg);
         colorLabel.setIcon(colorIcon);
         colorLabel.setBounds(122, 128, 18, 18);
         panel.add(colorLabel);
 
         // create boxes for players
         for (int i = 0; i < 6; i++) {
             ImagePanel playerBox = new ImagePanel("/media/p" + i + "1.png");
             if (i == 0) {
                 playerBox.setBounds(0, 0, 160, 175);
             }
             else {
                 playerBox.setBounds(160 * i - (i - 1) * 2, 0, 158, 175);
             }
             panel.add(playerBox);
         }
     }
 
     private void drawPlayerStatus(Player player, int number, JPanel panel) {
         int xBase = 0;
         int yBase = 30;
 
         // read in all images
 
         // player name label
         JLabel playerLabel = new JLabel(player.getName());
         playerLabel.setBounds((xBase + 158 * (number + 1)) + 30, yBase, 100, 20);
         panel.add(playerLabel);
 
         // food label
         JLabel foodLabel = new JLabel("" + player.getFood());
         foodLabel.setBounds((xBase + 158 * (number + 1)) + 45, yBase + 23, 100, 20);
         panel.add(foodLabel);
 
         // energy label
         JLabel energyLabel = new JLabel("" + player.getEnergy());
         energyLabel.setBounds((xBase + 158 * (number + 1)) + 105, yBase + 23, 100, 20);
         panel.add(energyLabel);
 
         // smithore label
         JLabel smithoreLabel = new JLabel("" + player.getSmithore());
         smithoreLabel.setBounds((xBase + 158 * (number + 1)) + 45, yBase + 58, 100, 20);
         panel.add(smithoreLabel);
 
         // mule label
         JLabel muleLabel = new JLabel("" + player.getMules());
         muleLabel.setBounds((xBase + 158 * (number + 1)) + 107, yBase + 58, 100, 20);
         panel.add(muleLabel);
 
         // money label
         JLabel moneyLabel = new JLabel("" + player.getMoney());
         moneyLabel.setBounds((xBase + 158 * (number + 1) + 45), yBase + 95, 100, 20);
         panel.add(moneyLabel);
 
         // color label
         String colorPrefix = player.getColor().substring(0, 1);
         BufferedImage colorImg;
         try {
             colorImg = ImageIO.read(getClass().getResourceAsStream("/media/circ" + colorPrefix + ".png"));
         }
         catch (Exception e) {
             System.out.println("Caught: " + e);
             return;
         }
 
         JLabel colorLabel = new JLabel();
         ImageIcon colorIcon = new ImageIcon(colorImg);
         colorLabel.setIcon(colorIcon);
         colorLabel.setBounds((xBase + 158 * (number + 1) + 124), yBase + 98, 18, 18);
         panel.add(colorLabel);
     }
 
     private void drawPlayerFlags(Map map, JPanel panel) {
         for (int i = 0; i < Map.HEIGHT; i++) {
             for (int j = 0; j < Map.WIDTH; j++) {
                 Player owner = map.getOwnerOfTile(i * Map.WIDTH + j);
                 if (owner != null) {
                     drawPlayerFlag(i, j, owner, panel);
                 }
             }
         }
     }
 
     private void drawPlayerFlag(int row, int column, Player player, JPanel panel) {
         System.out.println("Drawing at location " + row + ", " + column);
         BufferedImage flagImg;
         String colorPrefix = player.getColor().substring(0, 1);
 
         try {
             flagImg = ImageIO.read(getClass().getResourceAsStream("/media/flag" + colorPrefix + ".png"));
         }
         catch (Exception e) {
             System.out.println("Caught: " + e);
             return;
         }
 
         JLabel flagLabel = new JLabel();
         ImageIcon flagIcon = new ImageIcon(flagImg); 
         flagLabel.setIcon(flagIcon);
         flagLabel.setBounds(25 + column * 100, 25 + row * 100, 100, 100);
         panel.add(flagLabel);
     }
 
 }
