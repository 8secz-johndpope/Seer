 /************************************************************************
  * This file is part of AdminCmd.
  *
  * AdminCmd is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * AdminCmd is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with AdminCmd.  If not, see <http://www.gnu.org/licenses/>.
  ************************************************************************/
 package be.Balor.Manager.Permissions.Plugins;
 
 import in.mDev.MiracleM4n.mChatSuite.mChatSuite;
 import in.mDev.MiracleM4n.mChatSuite.types.InfoType;
 
import java.util.Collection;
import java.util.Map;
 import java.util.Set;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 import org.bukkit.command.CommandSender;
 import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;
 import org.bukkit.permissions.Permission;
 import org.bukkit.permissions.PermissionAttachmentInfo;
 
 import be.Balor.Manager.Exceptions.NoPermissionsPlugin;
import be.Balor.Tools.ClassUtils;
 import be.Balor.Tools.Utils;
import be.Balor.Tools.Debug.ACLogger;
 
 import com.miraclem4n.mchat.api.Reader;
 
 /**
  * @author Lathanael (aka Philippe Leipold)
  * 
  */
 public abstract class SuperPermissions implements IPermissionPlugin {
 	private static boolean mChat = false;
 
 	/**
 	 *
 	 */
 	public SuperPermissions() {
 	}
 
 	/**
 	 * @param mChatSuite
 	 *            the mChatAPI to set
 	 */
 	public static void setmChatapi(final mChatSuite mChatSuite) {
 		if (!SuperPermissions.mChat && mChatSuite != null) {
 			SuperPermissions.mChat = true;
 		}
 	}
 
 	/**
 	 * @return the mChatAPI
 	 */
 	public static boolean isApiSet() {
 		return mChat;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see
 	 * be.Balor.Manager.Permissions.AbstractPermission#hasPerm(org.bukkit.command
 	 * .CommandSender, java.lang.String, boolean)
 	 */
 	@Override
 	public boolean hasPerm(final CommandSender player, final String perm, final boolean errorMsg) {
 		if (!(player instanceof Player)) {
 			return true;
 		}
 		if (player.hasPermission(perm)) {
 			return true;
 		} else {
 			if (errorMsg) {
 				Utils.sI18n(player, "errorNotPerm", "p", perm);
 			}
 
 			return false;
 		}
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see
 	 * be.Balor.Manager.Permissions.AbstractPermission#hasPerm(org.bukkit.command
 	 * .CommandSender, org.bukkit.permissions.Permission, boolean)
 	 */
 	@Override
 	public boolean hasPerm(final CommandSender player, final Permission perm, final boolean errorMsg) {
 		if (!(player instanceof Player)) {
 			return true;
 		}
 		if (player.hasPermission(perm)) {
 			return true;
 		} else {
 			if (errorMsg) {
 				Utils.sI18n(player, "errorNotPerm", "p", perm.getName());
 			}
 			return false;
 		}
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see
 	 * be.Balor.Manager.Permissions.AbstractPermission#isInGroup(org.java.lang
 	 * .String, org.bukkit.entity.Player)
 	 */
 	@Override
 	public boolean isInGroup(final String group, final Player player) throws NoPermissionsPlugin {
 		throw new NoPermissionsPlugin("To use this functionality you need a Permission Plugin");
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see
 	 * be.Balor.Manager.Permissions.AbstractPermission#getUsers(org.java.lang
 	 * .String)
 	 */
 	@Override
 	public Set<Player> getUsers(final String groupName) throws NoPermissionsPlugin {
 		throw new NoPermissionsPlugin("To use this functionality you need a Permission Plugin");
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see
 	 * be.Balor.Manager.Permissions.AbstractPermission#getPermissionLimit(org
 	 * .bukkit.entity.Player, java.lang.String)
 	 */
 	@Override
 	public String getPermissionLimit(final Player p, final String limit) {
 		String result = null;
 		if (mChat) {
 			result = Reader.getInfo(p.getName(), InfoType.USER, p.getWorld().getName(), "admincmd."
 					+ limit);
 		}
 		if (result == null || (result != null && result.isEmpty())) {
 			final Pattern regex = Pattern
 					.compile("admincmd\\." + limit.toLowerCase() + "\\.[0-9]+");
			Map<String, PermissionAttachmentInfo> permissions = null;
			try {
				final PermissibleBase perm = ClassUtils.getPrivateField(p, "perm");
				permissions = ClassUtils.getPrivateField(perm, "permissions");
			} catch (final SecurityException e) {
				ACLogger.severe("Can't get the limit " + limit + " from player " + p.getName(), e);
			} catch (final IllegalArgumentException e) {
				ACLogger.severe("Can't get the limit " + limit + " from player " + p.getName(), e);
			} catch (final IllegalAccessException e) {
				ACLogger.severe("Can't get the limit " + limit + " from player " + p.getName(), e);
			} catch (final NoSuchFieldException e) {
				ACLogger.severe("Can't get the limit " + limit + " from player " + p.getName(), e);
			}
			final Collection<PermissionAttachmentInfo> perms = permissions.values();
 			int max = Integer.MIN_VALUE;
			synchronized (permissions) {
 				for (final PermissionAttachmentInfo info : perms) {
 					final Matcher regexMatcher = regex.matcher(info.getPermission().toLowerCase());
 					if (!regexMatcher.find()) {
 						continue;
 					}
 					final int current = Integer.parseInt(info.getPermission().split("\\.")[2]);
 					if (current < max) {
 						continue;
 					}
 					max = current;
 
 				}
 			}
 			if (max != Integer.MIN_VALUE) {
 				return String.valueOf(max);
 			}
 		} else {
 			return result;
 		}
 		return null;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see
 	 * be.Balor.Manager.Permissions.AbstractPermission#getPrefix(java.lang.String
 	 * , java.lang.String)
 	 */
 	@Override
 	public String getPrefix(final Player player) {
 		if (mChat) {
 			return Reader.getPrefix(player.getName(), InfoType.USER, player.getWorld().getName());
 		} else {
 			return "";
 		}
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see
 	 * be.Balor.Manager.Permissions.IPermissionPlugin#getSuffix(org.bukkit.entity
 	 * .Player)
 	 */
 	@Override
 	public String getSuffix(final Player player) {
 		if (mChat) {
 			return Reader.getSuffix(player.getName(), InfoType.USER, player.getWorld().getName());
 		} else {
 			return "";
 		}
 	}
 
 }
