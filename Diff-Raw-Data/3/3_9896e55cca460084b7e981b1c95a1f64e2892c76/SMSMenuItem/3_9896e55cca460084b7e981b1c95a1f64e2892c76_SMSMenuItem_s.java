 package me.desht.scrollingmenusign;
 
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.logging.Level;
 
 import me.desht.scrollingmenusign.enums.ReturnStatus;
 import me.desht.scrollingmenusign.parser.CommandParser;
 import me.desht.scrollingmenusign.parser.ParsedCommand;
 import me.desht.scrollingmenusign.util.MiscUtil;
 
 import org.bukkit.configuration.ConfigurationSection;
 import org.bukkit.entity.Player;
 
 public class SMSMenuItem implements Comparable<SMSMenuItem> {
 	private final String label;
 	private final String command;
 	private final String message;
 	private final SMSRemainingUses uses;
 	private final SMSMenu menu;
 
 	SMSMenuItem(SMSMenu menu, String l, String c, String m) {
 		if (l == null || c == null || m == null)
 			throw new NullPointerException();
 		this.menu = menu;
 		this.label = l;
 		this.command = c;
 		this.message = m;
 		this.uses = new SMSRemainingUses(this);
 	}
 
 	SMSMenuItem(SMSMenu menu, ConfigurationSection node) {
 		this.menu = menu;
 		this.label = MiscUtil.parseColourSpec(node.getString("label"));
 		this.command = node.getString("command");
 		this.message = MiscUtil.parseColourSpec(null, node.getString("message"));
 		this.uses = new SMSRemainingUses(this, node.getConfigurationSection("usesRemaining"));
 	}
 
 	/**
 	 * Get the label for this menu item
 	 * 
 	 * @return	The label
 	 */
 	public String getLabel() {
 		return label;
 	}
 
 	/**
 	 * Get the command for this menu item
 	 * 
 	 * @return	The command
 	 */
 	public String getCommand() {
 		return command;
 	}
 
 	/**
 	 * Get the feedback message for this menu item
 	 * 
 	 * @return	The feedback message
 	 */
 	public String getMessage() {
 		return message;
 	}
 
 	/**
 	 * Set the maximum number of uses for this menu, globally (i.e. for all users).
 	 * This clears any per-player use counts for the item.
 	 * 
 	 * @param nUses	maximum use count
 	 * @deprecated	Use getUseLimits().setGlobalUses() instead
 	 */
 	@Deprecated
 	public void setGlobalUses(int nUses) {
 		uses.clearUses();
 		uses.setGlobalUses(nUses);
 	}
 
 	/**
 	 * Set the maximum number of uses for this menu, per player.
 	 * This clears any global use count for the item.
 	 * 
 	 * @param nUses	maximum use count
 	 * @deprecated	Use getUseLimits().setUses() instead
 	 */
 	@Deprecated
 	public void setUses(int nUses) {
 		uses.setUses(nUses);
 	}
 
 	/**
 	 * Get the remaining number of uses of this menu item for the given player
 	 * 
 	 * @param player	Player to check for
 	 * @return			Number of uses remaining
 	 * @deprecated	Use getUseLimits().getRemainingUses() instead
 	 */
 	@Deprecated
 	public int getRemainingUses(Player player) {
 		return uses.getRemainingUses(player.getName());
 	}
 
 	/**
 	 * Clear (reset) the number of uses for the given player
 	 * 
 	 * @param player	Player to reset
 	 * @deprecated	Use getUseLimits().clearUses() instead
 	 */
 	@Deprecated
 	public void clearUses(Player player) {
 		uses.clearUses(player.getName());
 		if (menu != null)
 			menu.autosave();
 	}
 
 	/**
 	 * Clears all usage limits for this menu item
 	 * 
 	 * @deprecated	Use getUseLimits().clearUses() instead
 	 */
 	@Deprecated
 	public void clearUses() {
 		uses.clearUses();
 		if (menu != null)
 			menu.autosave();
 	}
 
 	/**
 	 * Executes the command for this item
 	 * 
 	 * @param player		Player to execute the command for
 	 * @throws SMSException	if the usage limit for this player is exhausted
 	 */
 	public void execute(Player player) throws SMSException {
 		if (player != null) {
 			checkRemainingUses(this.getUseLimits(), player);
 			checkRemainingUses(menu.getUseLimits(), player);
 		}
 
 		String cmd = getCommand();
 		if ((cmd == null || cmd.isEmpty()) && !menu.getDefaultCommand().isEmpty() ) {
 			cmd = menu.getDefaultCommand().replaceAll("<LABEL>", getLabel());
 		}
 
 		ParsedCommand pCmd = new CommandParser().runCommand(player, cmd);
		if (pCmd.getStatus() != ReturnStatus.CMD_OK) {
 			MiscUtil.errorMessage(player, pCmd.getLastError());
 		}
 	}
 
 	private void checkRemainingUses(SMSRemainingUses uses, Player player) throws SMSException {
 		String name = player.getName();
 		if (uses.hasLimitedUses(name)) {
 			String what = uses.getOwningObject().toString();
 			if (uses.getRemainingUses(name) == 0) {
 				throw new SMSException("You can't use that " + what + " anymore.");
 			}
 			uses.use(name);
 			if (menu != null)
 				menu.autosave();
 			MiscUtil.statusMessage(player, "&6[Uses remaining for this " + what + ": &e" + uses.getRemainingUses(name) + "&6]");
 		}
 	}
 
 	/**
 	 * Displays the feedback message for this menu item
 	 * 
 	 * @param player	Player to show the message to
 	 */
 	public void feedbackMessage(Player player) {
 		sendFeedback(player, getMessage());
 	}
 
 	private static void sendFeedback(Player player, String message) {
 		sendFeedback(player, message, new HashSet<String>());
 	}
 
 	private static void sendFeedback(Player player, String message, Set<String> history) {
 		if (message == null || message.length() == 0)
 			return;
 		if (message.startsWith("%")) {
 			// macro expansion
 			String macro = message.substring(1);
 			if (history.contains(macro)) {
 				MiscUtil.log(Level.WARNING, "sendFeedback [" + macro + "]: recursion detected");
 				MiscUtil.errorMessage(player, "Recursive loop detected in macro " + macro + "!");
 				return;
 			} else if (SMSMacro.hasMacro(macro)) {
 				history.add(macro);
 				sendFeedback(player, SMSMacro.getCommands(macro), history);
 			} else {
 				MiscUtil.errorMessage(player, "No such macro '" + macro + "'.");
 			}
 		} else {
 			MiscUtil.alertMessage(player, message);
 		}	
 	}
 
 	private static void sendFeedback(Player player, List<String> messages, Set<String> history) {
 		for (String m : messages) {
 			sendFeedback(player, m, history);
 		}
 	}
 
 	/* (non-Javadoc)
 	 * @see java.lang.Object#toString()
 	 */
 	@Override
 	public String toString() {
 		return "SMSMenuItem [label=" + label + ", command=" + command + ", message=" + message + "]";
 	}
 
 	/**
 	 * Get the remaining use details for this menu item
 	 *
 	 * @return	The remaining use details
 	 */
 	public SMSRemainingUses getUseLimits() {
 		return uses;
 	}
 
 	/**
 	 * Returns a printable representation of the number of uses remaining for this item.
 	 * 
 	 * @return	Formatted usage information
 	 */
 	String formatUses() {
 		return uses.toString();
 	}
 
 	/**
 	 * Returns a printable representation of the number of uses remaining for this item, for the given player.
 	 * 
 	 * @param player	Player to retrieve the usage information for
 	 * @return			Formatted usage information
 	 */
 	public String formatUses(Player player) {
 		if (player == null) {
 			return formatUses();
 		} else {
 			return uses.toString(player.getName());
 		}
 	}
 
 	/* (non-Javadoc)
 	 * @see java.lang.Object#hashCode()
 	 */
 	@Override
 	public int hashCode() {
 		final int prime = 31;
 		int result = 1;
 		result = prime * result + ((command == null) ? 0 : command.hashCode());
 		result = prime * result + ((label == null) ? 0 : label.hashCode());
 		result = prime * result + ((message == null) ? 0 : message.hashCode());
 		return result;
 	}
 
 	/* (non-Javadoc)
 	 * @see java.lang.Object#equals(java.lang.Object)
 	 */
 	@Override
 	public boolean equals(Object obj) {
 		if (this == obj)
 			return true;
 		if (obj == null)
 			return false;
 		if (getClass() != obj.getClass())
 			return false;
 		SMSMenuItem other = (SMSMenuItem) obj;
 		if (command == null) {
 			if (other.command != null)
 				return false;
 		} else if (!command.equals(other.command))
 			return false;
 		if (label == null) {
 			if (other.label != null)
 				return false;
 		} else if (!label.equals(other.label))
 			return false;
 		if (message == null) {
 			if (other.message != null)
 				return false;
 		} else if (!message.equals(other.message))
 			return false;
 		return true;
 	}
 
 	/* (non-Javadoc)
 	 * @see java.lang.Comparable#compareTo(java.lang.Object)
 	 */
 	@Override
 	public int compareTo(SMSMenuItem other) {
 		return MiscUtil.deColourise(label).compareToIgnoreCase(MiscUtil.deColourise(other.label));
 	}
 
 	Map<String, Object> freeze() {
 		Map<String, Object> map = new HashMap<String, Object>();
 
 		map.put("label", MiscUtil.unParseColourSpec(label));
 		map.put("command", command);
 		map.put("message", MiscUtil.unParseColourSpec(message));
 		map.put("usesRemaining", uses.freeze());
 
 		return map;
 	}
 
 	void autosave() {
 		if (menu != null)
 			menu.autosave();
 	}
 }
