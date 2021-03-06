 package scene;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import model.Item;
 import model.Party;
 import model.Person;
 
 import org.newdawn.slick.Color;
 import org.newdawn.slick.Font;
 import org.newdawn.slick.GameContainer;
 import org.newdawn.slick.SlickException;
 import org.newdawn.slick.UnicodeFont;
 import org.newdawn.slick.state.StateBasedGame;
 
 import component.Button;
 import component.Component;
 import component.ConditionBar;
 import component.Label;
 import component.Panel;
 import component.Positionable;
 import component.Positionable.ReferencePoint;
 
 import core.FontManager;
 import core.GameDirector;
 
 public class PartyInventoryScene extends Scene {
 	public static final SceneID ID = SceneID.PartyInventory;
 	private static final int PADDING = 20;
 	private static final int ITEM_BUTTON_WIDTH = 40;
 	
 	private Party party;
 	
 	public PartyInventoryScene(Party party) {
 		this.party = party;
 	}
 	
 	@Override
 	public void init(GameContainer container, StateBasedGame game) throws SlickException {
 		super.init(container, game);
 
 		Font fieldFont = GameDirector.sharedSceneDelegate().getFontManager().getFont(FontManager.FontID.FIELD);
 		
 		ArrayList<Person> members = party.getPartyMembers();
 		
 		ArrayList<Panel> personPanels = new ArrayList<Panel>();
 		for (Person person : members) {
 			List<Item> inventory = person.getInventory().getItems();
 			
 			Panel panel = new Panel(container, 200, 100);
 			
 			Label nameLabel = new Label(container, fieldFont, Color.white, person.getName());
 			panel.add(nameLabel, panel.getPosition(ReferencePoint.CenterLeft), ReferencePoint.CenterLeft, 0, 0);
 			
 			Positionable lastPositionReference = nameLabel;
 			for (Item item : inventory) {
 				Button button = new Button(container, ITEM_BUTTON_WIDTH, 20, new Label(container, fieldFont, Color.white, item.getName()));
 				ConditionBar conditionBar = new ConditionBar(container, ITEM_BUTTON_WIDTH, 5, item);
 				
 				panel.add(button, lastPositionReference.getPosition(ReferencePoint.CenterLeft), ReferencePoint.CenterLeft, 0, 0);
 				panel.add(conditionBar, conditionBar.getPosition(ReferencePoint.BottomCenter), ReferencePoint.TopCenter, 0, 0);
 				
 				lastPositionReference = button;
 			}
 		}
		mainLayer.addAsGrid((Component[])personPanels.toArray(), mainLayer.getPosition(ReferencePoint.TopCenter), (int)(members.size() / 2), 2, PADDING, PADDING, PADDING, PADDING);
 		
 		UnicodeFont h1 = GameDirector.sharedSceneDelegate().getFontManager().getFont(FontManager.FontID.H1);
 		UnicodeFont h2 = GameDirector.sharedSceneDelegate().getFontManager().getFont(FontManager.FontID.H2);
 		
 		Label titleLabel = new Label(container, h1, Color.white, "Party Inventory Scene");
 		Label subtitleLabel = new Label(container, h2, Color.white, "Press escape to go back");
 		
 		mainLayer.add(titleLabel, mainLayer.getPosition(Positionable.ReferencePoint.CenterCenter), Positionable.ReferencePoint.BottomCenter, 0, -5);
 		mainLayer.add(subtitleLabel, titleLabel.getPosition(Positionable.ReferencePoint.BottomCenter), Positionable.ReferencePoint.TopCenter, 0, 5);
 		
 		backgroundLayer.add(new Panel(container, new Color(0xc4aeca)));
 	}
 	
 	@Override
 	public void update(GameContainer container, StateBasedGame game, int delta) throws SlickException {
 		return;
 	}
 
 	@Override
 	public int getID() {
 		return ID.ordinal();
 	}
 }
