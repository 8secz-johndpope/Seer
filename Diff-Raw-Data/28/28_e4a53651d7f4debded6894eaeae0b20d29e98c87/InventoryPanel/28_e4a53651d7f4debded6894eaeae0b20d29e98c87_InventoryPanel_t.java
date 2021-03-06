 package GUI;
 
 import java.awt.BorderLayout;
 import java.awt.Dimension;
 import java.awt.FlowLayout;
 import java.awt.Graphics;
 import java.awt.Image;
 import java.util.ArrayList;
 import java.util.Observable;
 import java.util.Observer;
 
 import javax.swing.Box;
 import javax.swing.BoxLayout;
 import javax.swing.ImageIcon;
 import javax.swing.JPanel;
 
 import Character.PlayerCharacter;
 import Item.Item;
 
 /**
  * Show the players inventory
  * @author kristoffer
  */
 @SuppressWarnings("serial")
 public class InventoryPanel extends JPanel implements Observer{
 	
 	// fields:
 	private JPanel slotPanel;
 	private JPanel equippedPanel;
 	private ArrayList<ItemIcon> slots;
 	private ItemIcon weaponSlot;
 	private ItemIcon armorSlot;
 	private PlayerCharacter player;
 	
 	// consants:
 	private static final String PANEL_BACKGROUND = "images/gui/inventory.png";
 	
 	/**
 	 * Constructor
 	 */
 	public InventoryPanel(PlayerCharacter player){
 		this.player = player;
 		slots = new ArrayList<ItemIcon>();
 		
 		setPanelDetails();
 		createTopPanel();
 		createBottomPanel();
 	}
 	
 	/**
 	 * Sets this panels default visuals
 	 */
 	private void setPanelDetails(){
 		setOpaque(true);
 		setDoubleBuffered(true);
 		setPreferredSize(new Dimension(200, 640));
 		setVisible(true);
 		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
 	}
 	
 	/**
 	 * Paints a background image
 	 */
 	public void paintComponent(Graphics g) {
 		Image img = new ImageIcon(PANEL_BACKGROUND).getImage();	
 		g.drawImage(img, 0, 0, null);
 	}
 	
 	/**
 	 * Creates the topPanel which handles the inventory slots
 	 */
 	private void createTopPanel(){
 		JPanel topPanel = new JPanel();
 		topPanel.setOpaque(false);
 		topPanel.setPreferredSize(new Dimension(180, 420));
 		
 		// fill out inventory label
 		add(Box.createVerticalStrut(50));
 		
 		slotPanel = new JPanel();
 		slotPanel.setOpaque(false);
 		slotPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 10));
 		slotPanel.setPreferredSize(new Dimension(180, 420));
 		
 		// create slots
 		for(int i=0; i<player.getMaxInventorySize(); i++){
 			ItemIcon itemIcon = new ItemIcon(null, player);
 			slotPanel.add(itemIcon);
 			slots.add(itemIcon);
 		}
 
 		topPanel.add(slotPanel, BorderLayout.SOUTH);
 		add(topPanel);
 	}
 	
 	/**
 	 * Creates the bottomPanel which shows which items is active
 	 */
 	private void createBottomPanel(){
 		// fill out equipped label
 		add(Box.createVerticalStrut(50));
 
 		JPanel bottomPanel = new JPanel();
 		bottomPanel.setOpaque(false);
 		bottomPanel.setPreferredSize(new Dimension(180, 80));
 		
 		equippedPanel = new JPanel();
 		equippedPanel.setOpaque(false);
 		equippedPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 10));
 		equippedPanel.setPreferredSize(new Dimension(180, 100));
 		
 		// create weapon slot
 		ItemIcon weaponIcon = new ItemIcon(null, player);
 		equippedPanel.add(weaponIcon);
 		weaponSlot = weaponIcon;
 		
 		// create armor slot
 		ItemIcon armorIcon = new ItemIcon(null, player);
 		equippedPanel.add(armorIcon);
 		armorSlot = armorIcon;
 		
 		bottomPanel.add(equippedPanel);
 		add(bottomPanel);
 	}
 
 	/**
 	 * Handles the updates in the inventory
 	 * @param player
 	 * @param items
 	 */
 	private void updateInventory(){
 		// update panels
 		updateSlots();
 		updateEquip();
 		
 		slotPanel.revalidate();
 		equippedPanel.revalidate();
 	}
 	
 	/**
 	 * Updates the inventory slots
 	 */
 	private void updateSlots(){
 		for(int i=0; i<player.getInventory().size(); i++){
 			ItemIcon icon = slots.get(i);
 			Item item = player.getInventory().get(i);
 			
 			if(item != null){
 				icon.setItem(item);
 			}
 		}
 	}
 	
 	/**
 	 * Updates the equip slots
 	 */
 	private void updateEquip(){
 		if(player.getWeapon() != null){
 			weaponSlot.setItem(player.getWeapon());
		} else {
			weaponSlot.setItem(null);
 		}
 		
 		if(player.getArmor() != null){
 			armorSlot.setItem(player.getArmor());
		} else {
			armorSlot.setItem(null);
		}
 	}
 	
 	/**
 	 * Reset the inventory when game loads etc...
 	 * @param player
 	 */
 	public void reset(PlayerCharacter player){
 		updateInventory();
 	}
 	
 	/**
 	 * When somthing has changed in the inventory, update is called
 	 */
 	@Override
 	public void update(Observable o, Object arg) {
 		if(o instanceof PlayerCharacter && arg instanceof ArrayList<?>){	
 			updateInventory();
 		}	
 	}
 }
