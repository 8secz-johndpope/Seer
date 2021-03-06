 package me.desht.scrollingmenusign;
 
 import java.lang.ref.WeakReference;
 import java.util.Arrays;
 import java.util.List;
 
 import me.desht.dhutils.MiscUtil;
 import me.desht.scrollingmenusign.views.PoppableView;
 import me.desht.scrollingmenusign.views.SMSView;
 
 import org.bukkit.Effect;
 import org.bukkit.Material;
 import org.bukkit.entity.Player;
 import org.bukkit.inventory.ItemStack;
 import org.bukkit.inventory.meta.BookMeta;
 
 
 /**
  * @author des
  *
  * Represents a written book that can be used to trigger poppable views.
  * 
  */
 public class PopupBook {
 	
 	private final WeakReference<SMSView> view;
 	private final WeakReference<Player> player;
 
 	/**
 	 * Private constructor (use PopupBook.get()).  Create a popup book object from a
 	 * written book item.
 	 * 
 	 * @param p	 the player
 	 * @param bi  the book item
 	 */
 	private PopupBook(Player p, ItemStack bi) {
 		BookMeta bm = (BookMeta)bi.getItemMeta();
 		
 		List<String> pages = bm.getPages();
 		String viewType = pages.get(1).split(" ")[1];
 		String viewName = pages.get(2);
 		String menuName = pages.get(3);
 		
 		if (!ScrollingMenuSign.getInstance().getHandler().checkMenu(menuName)) {
 			// the menu's been deleted? the book's of no use anymore
 			throw new SMSException("Missing menu " + menuName);
 		}
 		
 		SMSView wantedView = null;
 		if (!SMSView.checkForView(viewName)) {
 			// the view could have been deleted - see if the menu has any other views of the same type
 			for (SMSView view : SMSView.listViews()) {
 				if (view.getMenu().getName().equals(menuName) && view.getType().equals(viewType)) {
 					wantedView = view;
 					break;
 				}
 			}
 			if (wantedView != null) {
 				// update the book to refer to the new view we found
				pages.set(2, wantedView.getName());
				bm.setPages(pages);
 			}
 		} else {
 			wantedView = SMSView.getView(viewName);
 		}
 		
 		if (wantedView == null || !(wantedView instanceof PoppableView)) {
 			// couldn't get a suitable view for this book - destroy it
 			throw new SMSException("Invalid view: " + viewName);
 		}
 		
 		this.player = new WeakReference<Player>(p);
 		this.view = new WeakReference<SMSView>((SMSView) wantedView);
 	}
 	
 	/**
 	 * Create a popup book object for the given player and view.
 	 * 
 	 * @param p
 	 * @param view
 	 */
 	public PopupBook(Player p, SMSView view) {
 		this.player = new WeakReference<Player>(p);
 		if (!(view instanceof PoppableView)) {
 			throw new SMSException("Invalid view: " + view.getName());
 		}
 		this.view = new WeakReference<SMSView>(view);
 	}
 
 	public SMSView getView() {
 		return view.get();	
 	}
 	
 	/**
 	 * Toggle the popped state of the view this book refers to.
 	 */
 	public void toggle() {
 		Player p = this.player.get();
 		SMSView v = getView();
 		if (p != null && v != null) {
 			v.ensureAllowedToUse(p);
 			((PoppableView)v).toggleGUI(p);
 		}
 	}
 	
 	/**
 	 * Get the book item corresponding to this popup book.
 	 * 
 	 * @return	an ItemStack of 1 written book with the title and pages filled in
 	 */
 	public ItemStack toItemStack() {
 		Player p = this.player.get();
 		SMSView v = getView();
 		if (v == null || p == null) {
 			return null;
 		}
 		ItemStack item = new ItemStack(Material.WRITTEN_BOOK, 1);
 		
 		BookMeta bm = (BookMeta) item.getItemMeta();
 		
 		bm.setTitle(v.variableSubs(v.getMenu().getTitle()));
 		bm.setAuthor(p.getName());
 		String[] pages = new String[] {
 			"Left Click to Use!",
 			"sms " + v.getType() + " view",
 			v.getName(),
 			v.getMenu().getName(),
 		};
 		bm.setPages(Arrays.asList(pages));
 		item.setItemMeta(bm);
 		
 		return item;
 	}
 	
 	/**
 	 * Get the popup book that the player is holding, if any.
 	 * 
 	 * @param p the player
 	 * @return the book, or null if the player is not holding one
 	 * @throws SMSException if the player is holding a popup book, but it's not valid
 	 */
 	public static PopupBook get(Player p) {
 		if (!holding(p))
 			return null;
 		return new PopupBook(p, p.getItemInHand());
 	}
 	
 	/**
 	 * Check if the player is holding a popup book.
 	 * 
 	 * @param p the player
 	 * @return	true if the player is holding a popup book
 	 */
 	public static boolean holding(Player p) {
 		if (p.getItemInHand().getType() != Material.WRITTEN_BOOK) {
 			return false;
 		}
 		BookMeta bm = (BookMeta) p.getItemInHand().getItemMeta();
 		List<String> pages = bm.getPages();
 		return pages != null && pages.size() >= 4 && pages.get(1).matches("^sms \\w+ view$");
 	}
 
 	/**
 	 * Destroys the item in the player's hand.  Doesn't check first to see if it's a
 	 * popup book - would usually be called if PopupBook.get() throws a SMSException.
 	 * 
 	 * @param p
 	 */
 	public static void destroy(Player p) {
 		p.setItemInHand(new ItemStack(0));
 		MiscUtil.statusMessage(p, "Your book suddenly vanishes in a puff of smoke!");
 		p.playEffect(p.getLocation().add(p.getLocation().getDirection()), Effect.SMOKE, 4);
 	}
 }
