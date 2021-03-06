 package net.sf.drftpd.master.command.plugins;
 
 import java.rmi.RemoteException;
 import java.util.Collection;
 import java.util.Date;
 import java.util.Iterator;
 import java.util.List;
 import java.util.NoSuchElementException;
 import java.util.StringTokenizer;
 
 import net.sf.drftpd.Bytes;
 import net.sf.drftpd.DuplicateElementException;
 import net.sf.drftpd.ObjectExistsException;
 import net.sf.drftpd.master.BaseFtpConnection;
 import net.sf.drftpd.master.FtpReply;
 import net.sf.drftpd.master.FtpRequest;
 import net.sf.drftpd.master.command.CommandHandler;
 import net.sf.drftpd.master.command.CommandManager;
 import net.sf.drftpd.master.command.CommandManagerFactory;
 import net.sf.drftpd.master.command.UnhandledCommandException;
 import net.sf.drftpd.master.config.FtpConfig;
 import net.sf.drftpd.master.config.Permission;
 import net.sf.drftpd.master.usermanager.NoSuchUserException;
 import net.sf.drftpd.master.usermanager.User;
 import net.sf.drftpd.master.usermanager.UserFileException;
 import net.sf.drftpd.slave.Transfer;
 import net.sf.drftpd.util.ReplacerUtils;
 
 import org.apache.log4j.Level;
 import org.apache.log4j.Logger;
 import org.tanesha.replacer.FormatterException;
 import org.tanesha.replacer.ReplacerEnvironment;
 import org.tanesha.replacer.ReplacerFormat;
 import org.tanesha.replacer.SimplePrintf;
 
 /**
  * @author mog
 * @version $Id: UserManagment.java,v 1.22 2004/01/20 22:12:02 zubov Exp $
  */
 public class UserManagment implements CommandHandler {
 	private static final Logger logger = Logger.getLogger(UserManagment.class);
 
 	private FtpReply doSITE_ADDIP(BaseFtpConnection conn) {
 		FtpRequest request = conn.getRequest();
 		conn.resetState();
 
 		if (!conn.getUserNull().isAdmin()
 			&& !conn.getUserNull().isGroupAdmin()) {
 			return FtpReply.RESPONSE_530_ACCESS_DENIED;
 		}
 
 		if (!request.hasArgument()) {
 			return FtpReply.RESPONSE_501_SYNTAX_ERROR;
 		}
 
 		String args[] = request.getArgument().split(" ");
 		if (args.length < 2) {
 			return new FtpReply(
 				200,
 				conn.jprintf(UserManagment.class.getName(), "addip.usage"));
 		}
 		FtpReply response = new FtpReply(200);
 		User myUser;
 		try {
 			myUser = conn.getUserManager().getUserByName(args[0]);
 			if (conn.getUserNull().isGroupAdmin()
 				&& !conn.getUserNull().getGroupName().equals(
 					myUser.getGroupName())) {
 				return FtpReply.RESPONSE_530_ACCESS_DENIED;
 			}
 			ReplacerEnvironment env = new ReplacerEnvironment();
 			env.add("targetuser", myUser.getUsername());
 			for (int i = 1; i < args.length; i++) {
 				String string = args[i];
 				env.add("mask", string);
 				try {
 					myUser.addIPMask(string);
 					response.addComment(
 						conn.jprintf(
 							UserManagment.class.getName(),
 							"addip.success",
 							env));
 				} catch (DuplicateElementException e) {
 					response.addComment(
 						conn.jprintf(
 							UserManagment.class.getName(),
 							"addip.dupe",
 							env));
 				}
 			}
 			myUser.commit(); // throws UserFileException
 			//userManager.save(user2);
 		} catch (NoSuchUserException ex) {
 			return new FtpReply(200, "No such user: " + args[0]);
 		} catch (UserFileException ex) {
 			response.addComment(ex.getMessage());
 			return response;
 		}
 		return response;
 	}
 
 	/**
 	 * USAGE: site adduser <user> <password> [<ident@ip#1> ... <ident@ip#5>]
 	 *	Adds a user. You can have wild cards for users that have dynamic ips
 	 *	Examples: *@192.168.1.* , frank@192.168.*.* , bob@192.*.*.*
 	 *	(*@192.168.1.1[5-9] will allow only 192.168.1.15-19 to connect but no one else)
 	 *
 	 *	If a user is added by a groupadmin, that user will have the GLOCK
 	 *	flag enabled and will inherit the groupadmin's home directory.
 	 *	
 	 *	All default values for the user are read from file default.user in
 	 *	/glftpd/ftp-data/users. Comments inside describe what is what.
 	 *	Gadmins can be assigned their own default.<group> userfiles
 	 *	as templates to be used when they add a user, if one is not found,
 	 *	default.user will be used.
 	 *	default.groupname files will also be used for "site gadduser".
 	 *
 	 *	ex. site ADDUSER Archimede mypassword 
 	 *
 	 *	This would add the user 'Archimede' with the password 'mypassword'.
 	 *
 	 *	ex. site ADDUSER Archimede mypassword *@127.0.0.1
 	 *	
 	 *	This would do the same as above + add the ip '*@127.0.0.1' at the
 	 *	same time.
 	 *
 	 *	HOMEDIRS:
 	 *	After login, the user will automatically be transferred into his/her
 	 *	homedir. As of 1.16.x this dir is now "kinda" chroot'ed and they are
 	 *	now unable to "cd ..".
 	 *
 	 *
 	 *
 	 * USAGE: site gadduser <group> <user> <password> [<ident@ip#1 .. ident@ip#5>]
 	 * Adds a user and changes his/her group to <group>.  If default.group
 	 * exists, it will be used as a base instead of default.user.
 	 *
 	 * Only public groups can be used as <group>.
 	 */
 	private FtpReply doSITE_ADDUSER(BaseFtpConnection conn) {
 		FtpRequest request = conn.getRequest();
		boolean isGAdduser = request.getCommand().equals("SITE GADDUSER");
 		conn.resetState();
 
 		if (!conn.getUserNull().isAdmin()
 			&& !conn.getUserNull().isGroupAdmin()) {
 			return FtpReply.RESPONSE_530_ACCESS_DENIED;
 		}
 
 		if (!request.hasArgument()) {
 			String key;
 			if (isGAdduser) {
 				key = "gadduser.usage";
 			} else { //request.getCommand().equals("SITE ADDUSER");
 				key = "adduser.usage";
 			}
 			return new FtpReply(
 				501,
 				conn.jprintf(UserManagment.class.getName(), key));
 		}
 
 		if (conn.getUserNull().isGroupAdmin()) {
 			if (isGAdduser) {
 				return FtpReply.RESPONSE_530_ACCESS_DENIED;
 			}
 
 			int users;
 			try {
 				users =
 					conn
 						.getUserManager()
 						.getAllUsersByGroup(conn.getUserNull().getGroupName())
 						.size();
 				if (users >= conn.getUserNull().getGroupSlots()) {
 					return new FtpReply(
 						200,
 						conn.jprintf(
 							UserManagment.class.getName(),
 							"adduser.noslots"));
 				}
 			} catch (UserFileException e1) {
 				logger.warn("", e1);
 				return new FtpReply(200, e1.getMessage());
 			}
 		}
 		StringTokenizer st = new StringTokenizer(request.getArgument());
 		User newUser;
 		FtpReply response = new FtpReply(200);
 		ReplacerEnvironment env = new ReplacerEnvironment();
 		try {
 			String group = null;
 			if (isGAdduser) {
 				group = st.nextToken();
 			}
 			String newUsername = st.nextToken();
 			env.add("targetuser", newUsername);
 
 			String pass = st.nextToken();
 			response.addComment(
 				conn.jprintf(
 					UserManagment.class.getName(),
 					"adduser.success",
 					env));
 			//action, no more NoSuchElementException below here
 			newUser = conn.getUserManager().create(newUsername);
 			newUser.setPassword(pass);
 			newUser.setComment("Added by " + conn.getUserNull().getUsername());
 			if (conn.getUserNull().isGroupAdmin()) {
 				newUser.setGroup(conn.getUserNull().getGroupName());
 			}
 			if (isGAdduser) {
 				newUser.setGroup(group);
 				response.addComment(
 					"Primary group set to " + newUser.getGroupName());
 			}
 		} catch (NoSuchElementException ex) {
 			return FtpReply.RESPONSE_501_SYNTAX_ERROR;
 		} catch (UserFileException ex) {
 			return new FtpReply(200, ex.getMessage());
 		}
 		try {
 			while (st.hasMoreTokens()) {
 				String string = st.nextToken();
 				try {
 					newUser.addIPMask(string);
 					response.addComment(
 						conn.jprintf(
 							UserManagment.class.getName(),
 							"addip.success",
 							env));
 				} catch (DuplicateElementException e1) {
 					response.addComment(
 						conn.jprintf(
 							UserManagment.class.getName(),
 							"addip.dupe",
 							env));
 				}
 			}
 			newUser.commit();
 		} catch (UserFileException ex) {
 			return new FtpReply(200, ex.getMessage());
 		}
 		return response;
 	}
 
 	/**
 	 * USAGE: site change <user> <field> <value> - change a field for a user
 	   site change =<group> <field> <value> - change a field for each member of group <group>
 	   site change { <user1> <user2> .. } <field> <value> - change a field for each user in the list
 	   site change * <field> <value>     - change a field for everyone
 	
 	Type "site change user help" in glftpd for syntax.
 	
 	Fields available:
 	
 	Field			Description
 	-------------------------------------------------------------
 	ratio		Upload/Download ratio. 0 = Unlimited (Leech)
 	wkly_allotment 	The number of kilobytes that this user will be given once a week
 			(you need the reset binary enabled in your crontab).
 			Syntax: site change user wkly_allotment "#,###"
 			The first number is the section number (0=default section),
 			the second is the number of kilobytes to give.
 			(user's credits are replaced, not added to, with this value)
 			Only one section at a time is supported,
 	homedir		This will change the user's homedir.
 			NOTE: This command is disabled by default.  To enable it, add
 			"min_homedir /site" to your config file, where "/site" is the
 			minimum directory that users can have, i.e. you can't change
 			a user's home directory to /ftp-data or anything that doesn't
 			have "/site" at the beginning.
 			Important: don't use a trailing slash for homedir!
 			Users CAN NOT cd, list, upload/download, etc, outside of their
 						home dir. It acts similarly to chroot() (try man chroot).
 	startup_dir	The directory to start in. ex: /incoming will start the user
 			in /glftpd/site/incoming if rootpath is /glftpd and homedir is /site.
 			Users CAN cd, list, upload/download, etc, outside of startup_dir.
 	idle_time	Sets the default and maximum idle time for this user (overrides
 			the -t and -T settings on glftpd command line). If -1, it is disabled;
 			if 0, it is the same as the idler flag.
 	credits		Credits left to download.
 	flags		+1ABC or +H or -3, type "site flags" for a list of flags.
 	num_logins	# # : number of simultaneous logins allowed. The second
 			number is number of sim. logins from the same IP.
 	timeframe	# # : the hour from which to allow logins and the hour when logins from
 				this user will start being rejected. This is set in a 24 hour format.
 			If a user is online past his timeframe, he'll be disconnected the
 			next time he does a 'CWD'.
 	time_limit	Time limits, per LOGIN SESSION. (set in minutes. 0 = Unlimited)
 	tagline		User's tagline.
 	group_slots	Number of users a GADMIN is allowed to add.
 			If you specify a second argument, it will be the
 			number of leech accounts the gadmin can give (done by
 			"site change user ratio 0") (2nd arg = leech slots)
 	comment		Changes the user's comment (max 50 characters).
 			Comments are displayed by the comment cookie (see below).
 	max_dlspeed	Downstream bandwidth control (KBytes/sec) (0 = Unlimited)
 	max_ulspeed	Same but for uploads
 	max_sim_down	Maximum number of simultaneous downloads for this user
 			(-1 = unlimited, 0 = zero [user can't download])
 	max_sim_up	Maximum number of simultaneous uploads for this user
 			(-1 = unlimited, 0 = zero [user can't upload])
 	sratio		<SECTIONNAME> <#>
 			This is to change the ratio of a section (other than default).
 	
 	Flags available:
 	
 	Flagname       	Flag	Description
 	-------------------------------------------------------------
 	SITEOP		1	User is siteop.
 	GADMIN		2	User is Groupadmin of his/her first public
 				group (doesn't work for private groups).
 	GLOCK		3	User cannot change group.
 	EXEMPT		4	Allows to log in when site is full. Also allows
 				user to do "site idle 0", which is the same as
 				having the idler flag. Also exempts the user
 				from the sim_xfers limit in config file.
 	COLOR		5	Enable/Disable the use of color (toggle with "site color").
 	DELETED		6	User is deleted.
 	USEREDIT	7	"Co-Siteop"
 	ANON		8	User is anonymous (per-session like login).
 	
 	*NOTE* The 1 flag is not GOD mode, you must have the correct flags for the actions you wish to perform.
 	*NOTE* If you have flag 1 then you DO NOT WANT flag 2
 	
 	Restrictions placed on users flagged ANONYMOUS.
 		1.  '!' on login is ignored.
 		2.  They cannot DELETE, RMDIR, or RENAME. 
 		3.  Userfiles do not update like usual, meaning no stats will
 				be kept for these users.  The userfile only serves as a template for the starting 
 			environment of the logged in user. 
 				Use external scripts if you must keep records of their transfer stats.
 	
 	NUKE		A	User is allowed to use site NUKE.
 	UNNUKE		B	User is allowed to use site UNNUKE.
 	UNDUPE		C	User is allowed to use site UNDUPE.
 	KICK		D	User is allowed to use site KICK.
 	KILL		E	User is allowed to use site KILL/SWHO.
 	TAKE		F	User is allowed to use site TAKE.
 	GIVE		G	User is allowed to use site GIVE.
 	USERS/USER	H	This allows you to view users ( site USER/USERS )
 	IDLER		I	User is allowed to idle forever.
 	CUSTOM1		J	Custom flag 1
 	CUSTOM2		K	Custom flag 2
 	CUSTOM3		L	Custom flag 3
 	CUSTOM4		M	Custom flag 4
 	CUSTOM5		N	Custom flag 5
 	
 	You can use custom flags in the config file to give some users access
 	to certain things without having to use private groups.  These flags
 	will only show up in "site flags" if they're turned on.
 	
 	ex. site change Archimede ratio 5
 	
 	This would set the ratio to 1:5 for the user 'Archimede'.
 	
 	ex. site change Archimede flags +2-AG
 	
 	This would make the user 'Archimede' groupadmin and remove his ability
 	to use the commands site nuke and site give.
 	
 	NOTE: The flag DELETED can not be changed with site change, it
 		  will change when someone does a site deluser/readd.
 	 */
 	private FtpReply doSITE_CHANGE(BaseFtpConnection conn) {
 		FtpRequest request = conn.getRequest();
 
 		if (!conn.getUserNull().isAdmin()
 			&& !conn.getUserNull().isGroupAdmin()) {
 			return FtpReply.RESPONSE_530_ACCESS_DENIED;
 		}
 		if (!request.hasArgument()) {
 			return new FtpReply(
 				501,
 				conn.jprintf(UserManagment.class.getName(), "change.usage"));
 		}
 
 		String command, commandArgument;
 		User myUser;
 		{
 			String argument = request.getArgument();
 			int pos1 = argument.indexOf(' ');
 			if (pos1 == -1) {
 				return FtpReply.RESPONSE_501_SYNTAX_ERROR;
 			}
 			String username = argument.substring(0, pos1);
 			try {
 				myUser =
 					conn.getUserManager().getUserByName(
 						argument.substring(0, pos1));
 			} catch (NoSuchUserException e) {
 				return new FtpReply(
 					550,
 					"User " + username + " not found: " + e.getMessage());
 			} catch (UserFileException e) {
 				logger.log(Level.FATAL, "Error loading user", e);
 				return new FtpReply(
 					550,
 					"Error loading user: " + e.getMessage());
 			}
 
 			int pos2 = argument.indexOf(' ', pos1 + 1);
 			if (pos2 == -1) {
 				return FtpReply.RESPONSE_501_SYNTAX_ERROR;
 			}
 			command = argument.substring(pos1 + 1, pos2);
 			commandArgument = argument.substring(pos2 + 1);
 			if (conn.getUserNull().isGroupAdmin()) {
 				////// ratio //////
 				if (!command.equalsIgnoreCase("ratio")
 					|| !conn.getUserNull().getGroupName().equals(
 						myUser.getGroupName())) {
 					return FtpReply.RESPONSE_530_ACCESS_DENIED;
 				}
 				float ratio = Float.parseFloat(commandArgument);
 				if (ratio == 0F) {
 					int usedleechslots = 0;
 					try {
 						for (Iterator iter =
 							conn
 								.getUserManager()
 								.getAllUsersByGroup(
 									conn.getUserNull().getGroupName())
 								.iterator();
 							iter.hasNext();
 							) {
 							if (((User) iter.next()).getRatio() == 0F)
 								usedleechslots++;
 						}
 					} catch (UserFileException e1) {
 						return new FtpReply(
 							200,
 							"IO error reading userfiles: " + e1.getMessage());
 					}
 					if (usedleechslots
 						>= conn.getUserNull().getGroupLeechSlots() + 1) {
 						return new FtpReply(
 							200,
 							conn.jprintf(
 								UserManagment.class.getName(),
 								"changeratio.nomoreslots"));
 					}
 				} else if (ratio != 0F) {
 					return new FtpReply(
 						200,
 						conn.jprintf(
 							UserManagment.class.getName(),
 							"changeratio.invalidratio"));
 				}
 				myUser.setRatio(ratio);
 			}
 		}
 
 		//		String args[] = request.getArgument().split(" ");
 		//		String command = args[1].toLowerCase();
 		// 0 = user
 		// 1 = command
 		// 2- = argument
 		FtpReply response = (FtpReply) FtpReply.RESPONSE_200_COMMAND_OK.clone();
 		if ("credits".equalsIgnoreCase(command)) {
 			myUser.setCredits(Bytes.parseBytes(commandArgument));
 
 		} else if ("ratio".equalsIgnoreCase(command)) {
 			myUser.setRatio(Float.parseFloat(commandArgument));
 
 		} else if ("comment".equalsIgnoreCase(command)) {
 			myUser.setComment(commandArgument);
 
 		} else if ("idle_time".equalsIgnoreCase(command)) {
 			myUser.setIdleTime(Integer.parseInt(commandArgument));
 
 		} else if ("num_logins".equalsIgnoreCase(command)) {
 			try {
 				String args[] = commandArgument.split(" ");
 				if (args.length < 1 || args.length > 2) {
 					return FtpReply.RESPONSE_501_SYNTAX_ERROR;
 				}
 				myUser.setMaxLogins(Integer.parseInt(args[0]));
 				if (args.length == 2) {
 					myUser.setMaxLoginsPerIP(Integer.parseInt(args[1]));
 				}
 			} catch (NumberFormatException ex) {
 				return FtpReply.RESPONSE_501_SYNTAX_ERROR;
 			}
 		} else if ("num_logins".equalsIgnoreCase(command)) {
 			myUser.setMaxLoginsPerIP(Integer.parseInt(commandArgument));
 
 			//} else if ("max_dlspeed".equalsIgnoreCase(command)) {
 			//	myUser.setMaxDownloadRate(Integer.parseInt(commandArgument));
 
 			//} else if ("max_ulspeed".equals(command)) {
 			//	myUser.setMaxUploadRate(Integer.parseInt(commandArgument));
 		} else if ("group".equals(command)) {
 			myUser.setGroup(commandArgument);
 
 			//			group_slots	Number of users a GADMIN is allowed to add.
 			//					If you specify a second argument, it will be the
 			//					number of leech accounts the gadmin can give (done by
 			//					"site change user ratio 0") (2nd arg = leech slots)
 		} else if ("group_slots".equals(command)) {
 			try {
 				String args[] = commandArgument.split(" ");
 				if (args.length < 1 || args.length > 2) {
 					return FtpReply.RESPONSE_501_SYNTAX_ERROR;
 				}
 				myUser.setGroupSlots(Short.parseShort(args[0]));
 				if (args.length >= 2) {
 					myUser.setGroupLeechSlots(Short.parseShort(args[1]));
 				}
 			} catch (NumberFormatException ex) {
 				return FtpReply.RESPONSE_501_SYNTAX_ERROR;
 			}
 		} else {
 			return FtpReply.RESPONSE_501_SYNTAX_ERROR;
 		}
 		try {
 			myUser.commit();
 		} catch (UserFileException e) {
 			response.addComment(e.getMessage());
 		}
 		return response;
 	}
 
 	/**
 	 * USAGE: site chgrp <user> <group> [<group>]
 	 *		Adds/removes a user from group(s).
 	 *	
 	 *		ex. site chgrp archimede ftp
 	 *		This would change the group to 'ftp' for the user 'archimede'.
 	 *	
 	 *		ex1. site chgrp archimede ftp
 	 *		This would remove the group ftp from the user 'archimede'.
 	 *	
 	 *		ex2. site chgrp archimede ftp eleet
 	 *		This moves archimede from ftp group to eleet group.
 	 */
 	private FtpReply doSITE_CHGRP(BaseFtpConnection conn) {
 		FtpRequest request = conn.getRequest();
 		conn.resetState();
 
 		if (!conn.getUserNull().isAdmin()) {
 			return FtpReply.RESPONSE_530_ACCESS_DENIED;
 		}
 
 		if (!request.hasArgument()) {
 			return new FtpReply(
 				501,
 				conn.jprintf(UserManagment.class.getName(), "chgrp.usage"));
 		}
 
 		String args[] = request.getArgument().split("[ ,]");
 		if (args.length < 2) {
 			return FtpReply.RESPONSE_501_SYNTAX_ERROR;
 		}
 
 		User myUser;
 		try {
 			myUser = conn.getUserManager().getUserByName(args[0]);
 		} catch (NoSuchUserException e) {
 			return new FtpReply(200, "User not found: " + e.getMessage());
 		} catch (UserFileException e) {
 			logger.log(Level.FATAL, "IO error reading user", e);
 			return new FtpReply(
 				200,
 				"IO error reading user: " + e.getMessage());
 		}
 
 		FtpReply response = new FtpReply(200);
 		for (int i = 1; i < args.length; i++) {
 			String string = args[i];
 			try {
 				myUser.removeGroup(string);
 				response.addComment(
 					myUser.getUsername() + " removed from group " + string);
 			} catch (NoSuchFieldException e1) {
 				try {
 					myUser.addGroup(string);
 				} catch (DuplicateElementException e2) {
 					logger.log(
 						Level.FATAL,
 						"Error, user was not a member before",
 						e2);
 				}
 				response.addComment(
 					myUser.getUsername() + " added to group " + string);
 			}
 		}
 		return response;
 	}
 
 	/**
 	 * USAGE: site chpass <user> <password>
 	 * 	Change users password.
 	 * 
 	 * 	ex. site chpass Archimede newpassword
 	 * 	This would change the password to 'newpassword' for the 
 	 * 	user 'Archimede'.
 	 * 
 	 * 	See "site passwd" for more info if you get a "Password is not secure
 	 * 	enough" error.
 	 * 
 	 * 	* Denotes any password, ex. site chpass arch *
 	 * 	This will allow arch to login with any password
 	 * 
 	 * 	@ Denotes any email-like password, ex. site chpass arch @
 	 * 	This will allow arch to login with a@b.com but not ab.com
 	 */
 	private FtpReply doSITE_CHPASS(BaseFtpConnection conn) {
 		FtpRequest request = conn.getRequest();
 		conn.resetState();
 
 		if (!conn.getUserNull().isAdmin()) {
 			return FtpReply.RESPONSE_530_ACCESS_DENIED;
 		}
 
 		if (!request.hasArgument()) {
 			return new FtpReply(
 				501,
 				conn.jprintf(UserManagment.class.getName(), "chpass.usage"));
 		}
 
 		String args[] = request.getArgument().split(" ");
 		if (args.length != 2) {
 			return FtpReply.RESPONSE_501_SYNTAX_ERROR;
 		}
 
 		try {
 			User user = conn.getUserManager().getUserByName(args[0]);
 			user.setPassword(args[1]);
 
 			return FtpReply.RESPONSE_200_COMMAND_OK;
 		} catch (NoSuchUserException e) {
 			return new FtpReply(200, "User not found: " + e.getMessage());
 		} catch (UserFileException e) {
 			logger.log(Level.FATAL, "Error reading userfile", e);
 			return new FtpReply(
 				200,
 				"Error reading userfile: " + e.getMessage());
 		}
 
 	}
 	/**
 	 * USAGE: site delip <user> <ident@ip> ...
 	 * @param request
 	 * @param out
 	 */
 	private FtpReply doSITE_DELIP(BaseFtpConnection conn) {
 		FtpRequest request = conn.getRequest();
 		conn.resetState();
 		if (!conn.getUserNull().isAdmin()
 			&& !conn.getUserNull().isGroupAdmin()) {
 			return FtpReply.RESPONSE_530_ACCESS_DENIED;
 		}
 
 		if (!request.hasArgument()) {
 			return new FtpReply(
 				501,
 				conn.jprintf(UserManagment.class.getName(), "delip.usage"));
 		}
 
 		String args[] = request.getArgument().split(" ");
 
 		System.out.println(args.length);
 		if (args.length < 2) {
 			return FtpReply.RESPONSE_501_SYNTAX_ERROR;
 		}
 
 		User myUser;
 		try {
 			myUser = conn.getUserManager().getUserByName(args[0]);
 		} catch (NoSuchUserException e) {
 			return new FtpReply(200, e.getMessage());
 		} catch (UserFileException e) {
 			logger.log(Level.FATAL, "IO error", e);
 			return new FtpReply(200, "IO error: " + e.getMessage());
 		}
 		if (conn.getUserNull().isGroupAdmin()
 			&& !conn.getUserNull().getGroupName().equals(myUser.getGroupName())) {
 			return FtpReply.RESPONSE_530_ACCESS_DENIED;
 		}
 
 		FtpReply response = new FtpReply(200);
 		for (int i = 1; i < args.length; i++) {
 			String string = args[i];
 			try {
 				myUser.removeIpMask(string);
 				response.addComment("Removed " + string);
 			} catch (NoSuchFieldException e1) {
 				response.addComment(
 					"Mask " + string + " not found: " + e1.getMessage());
 				continue;
 			}
 		}
 		return response;
 	}
 
 	private FtpReply doSITE_DELUSER(BaseFtpConnection conn) {
 		FtpRequest request = conn.getRequest();
 		conn.resetState();
 		if (!request.hasArgument()) {
 			return new FtpReply(
 				501,
 				conn.jprintf(UserManagment.class.getName(), "deluser.usage"));
 		}
 
 		if (!conn.getUserNull().isAdmin()
 			&& !conn.getUserNull().isGroupAdmin()) {
 			return FtpReply.RESPONSE_530_ACCESS_DENIED;
 		}
 
 		String delUsername = request.getArgument();
 		User delUser;
 		try {
 			delUser = conn.getUserManager().getUserByName(delUsername);
 		} catch (NoSuchUserException e) {
 			return new FtpReply(200, e.getMessage());
 		} catch (UserFileException e) {
 			return new FtpReply(200, "Couldn't getUser: " + e.getMessage());
 		}
 
 		if (conn.getUserNull().isGroupAdmin()
 			&& !conn.getUserNull().getGroupName().equals(
 				delUser.getGroupName())) {
 			return FtpReply.RESPONSE_530_ACCESS_DENIED;
 		}
 
 		delUser.setDeleted(true);
 		return FtpReply.RESPONSE_200_COMMAND_OK;
 	}
 
 	private FtpReply doSITE_GINFO(BaseFtpConnection conn) {
 		FtpRequest request = conn.getRequest();
 
 		conn.resetState();
 		//security
 		if (!conn.getUserNull().isAdmin()
 			&& !conn.getUserNull().isGroupAdmin()) {
 			return FtpReply.RESPONSE_530_ACCESS_DENIED;
 		}
 
 		//syntax
 		if (!request.hasArgument()) {
 			return new FtpReply(
 				501,
 				conn.jprintf(UserManagment.class.getName(), "ginfo.usage"));
 		}
 
 		//gadmin
 		String group = request.getArgument();
 		if (conn.getUserNull().isGroupAdmin()
 			&& !conn.getUserNull().getGroupName().equals(group)) {
 			return FtpReply.RESPONSE_530_ACCESS_DENIED;
 		}
 
 		Collection users;
 		try {
 			users = conn.getUserManager().getAllUsersByGroup(group);
 		} catch (UserFileException e) {
 			return new FtpReply(200, "IO error: " + e.getMessage());
 		}
 
 		FtpReply response = new FtpReply(200);
 		for (Iterator iter = users.iterator(); iter.hasNext();) {
 			User user = (User) iter.next();
 			char status = ' ';
 			if (user.isGroupAdmin()) {
 				status = '+';
 			} else if (user.isAdmin()) {
 				status = '*';
 			}
 			response.addComment(status + user.getUsername());
 		}
 		response.addComment(" * = siteop   + = gadmin");
 		return response;
 	}
 
 	private FtpReply doSITE_GIVE(BaseFtpConnection conn) {
 		FtpRequest request = conn.getRequest();
 
 		conn.resetState();
 
 		if (!request.hasArgument()) {
 			return new FtpReply(
 				501,
 				conn.jprintf(UserManagment.class.getName(), "give.usage"));
 		}
 
 		StringTokenizer st = new StringTokenizer(request.getArgument());
 		if (!st.hasMoreTokens()) {
 			return FtpReply.RESPONSE_501_SYNTAX_ERROR;
 		}
 		User user2;
 		try {
 			user2 = conn.getUserManager().getUserByName(st.nextToken());
 		} catch (Exception e) {
 			logger.warn("", e);
 			return new FtpReply(200, e.getMessage());
 		}
 
 		if (!st.hasMoreTokens()) {
 			return FtpReply.RESPONSE_501_SYNTAX_ERROR;
 		}
 
 		long credits = Bytes.parseBytes(st.nextToken());
 		if (0 > credits) {
 			return new FtpReply(200, "Credits must be a positive number.");
 		}
 
 		conn.getUserNull().updateCredits(-credits);
 		user2.updateCredits(credits);
 		return new FtpReply(
 			200,
 			"OK, gave "
 				+ Bytes.formatBytes(credits)
 				+ " of your credits to "
 				+ user2.getUsername());
 	}
 	private FtpReply doSITE_GROUPS(BaseFtpConnection conn) {
 		conn.resetState();
 
 		Collection groups;
 		try {
 			groups = conn.getUserManager().getAllGroups();
 		} catch (UserFileException e) {
 			logger.log(Level.FATAL, "IO error from getAllGroups()", e);
 			return new FtpReply(200, "IO error: " + e.getMessage());
 		}
 		FtpReply response = new FtpReply(200);
 		response.addComment("All groups:");
 		for (Iterator iter = groups.iterator(); iter.hasNext();) {
 			String element = (String) iter.next();
 			response.addComment(element);
 		}
 
 		return response;
 	}
 	private FtpReply doSITE_KICK(BaseFtpConnection conn) {
 		FtpRequest request = conn.getRequest();
 		if (!conn.getUserNull().isAdmin()) {
 			return FtpReply.RESPONSE_530_ACCESS_DENIED;
 		}
 
 		if (!request.hasArgument()) {
 			return new FtpReply(
 				501,
 				conn.jprintf(UserManagment.class.getName(), "kick.usage"));
 		}
 		String arg = request.getArgument();
 		int pos = arg.indexOf(' ');
 
 		String username;
 		String message = "Kicked by " + conn.getUserNull().getUsername();
 		if (pos == -1) {
 			username = arg;
 		} else {
 			username = arg.substring(0, pos);
 			message = arg.substring(pos + 1);
 		}
 
 		FtpReply response = (FtpReply) FtpReply.RESPONSE_200_COMMAND_OK.clone();
 
 		Collection conns = conn.getConnectionManager().getConnections();
 		synchronized (conns) {
 			for (Iterator iter = conns.iterator(); iter.hasNext();) {
 				BaseFtpConnection conn2 = (BaseFtpConnection) iter.next();
 				try {
 					if (conn2.getUser().getUsername().equals(username)) {
 						conn2.stop(message);
 					}
 				} catch (NoSuchUserException e) {
 				}
 			}
 		}
 		return response;
 	}
 
 	private FtpReply doSITE_PASSWD(BaseFtpConnection conn) {
 		FtpRequest request = conn.getRequest();
 		conn.resetState();
 		if (!request.hasArgument()) {
 			return new FtpReply(
 				501,
 				conn.jprintf(UserManagment.class.getName(), "passwd.usage"));
 		}
 		conn.getUserNull().setPassword(request.getArgument());
 		return FtpReply.RESPONSE_200_COMMAND_OK;
 	}
 
 	private FtpReply doSITE_PURGE(BaseFtpConnection conn) {
 		FtpRequest request = conn.getRequest();
 		conn.resetState();
 
 		if (!conn.getUserNull().isAdmin()
 			&& !conn.getUserNull().isGroupAdmin()) {
 			return FtpReply.RESPONSE_530_ACCESS_DENIED;
 		}
 
 		if (!request.hasArgument()) {
 			return new FtpReply(
 				501,
 				conn.jprintf(UserManagment.class.getName(), "purge.usage"));
 		}
 
 		String delUsername = request.getArgument();
 		User delUser;
 		try {
 			delUser = conn.getUserManager().getUserByNameUnchecked(delUsername);
 		} catch (NoSuchUserException e) {
 			return new FtpReply(200, e.getMessage());
 		} catch (UserFileException e) {
 			return new FtpReply(200, "Couldn't getUser: " + e.getMessage());
 		}
 		if (!delUser.isDeleted()) {
 			return new FtpReply(200, "User isn't deleted");
 		}
 
 		if (conn.getUserNull().isGroupAdmin()
 			&& !conn.getUserNull().getGroupName().equals(
 				delUser.getGroupName())) {
 			return FtpReply.RESPONSE_530_ACCESS_DENIED;
 		}
 		delUser.purge();
 		return FtpReply.RESPONSE_200_COMMAND_OK;
 	}
 
 	private FtpReply doSITE_READD(BaseFtpConnection conn) {
 		FtpRequest request = conn.getRequest();
 		conn.resetState();
 		if (!conn.getUserNull().isAdmin()
 			&& !conn.getUserNull().isGroupAdmin()) {
 			return FtpReply.RESPONSE_530_ACCESS_DENIED;
 		}
 
 		if (!request.hasArgument()) {
 			return new FtpReply(
 				501,
 				conn.jprintf(UserManagment.class.getName(), "readd.usage"));
 		}
 
 		User myUser;
 		try {
 			myUser =
 				conn.getUserManager().getUserByNameUnchecked(
 					request.getArgument());
 		} catch (NoSuchUserException e) {
 			return new FtpReply(200, e.getMessage());
 		} catch (UserFileException e) {
 			return new FtpReply(200, "IO error: " + e.getMessage());
 		}
 
 		if (conn.getUserNull().isGroupAdmin()
 			&& !conn.getUserNull().getGroupName().equals(myUser.getGroupName())) {
 			return FtpReply.RESPONSE_530_ACCESS_DENIED;
 		}
 
 		if (!myUser.isDeleted()) {
 			return new FtpReply(200, "User wasn't deleted");
 		}
 		myUser.setDeleted(false);
 		return FtpReply.RESPONSE_200_COMMAND_OK;
 	}
 
 	private FtpReply doSITE_RENUSER(BaseFtpConnection conn) {
 		FtpRequest request = conn.getRequest();
 		conn.resetState();
 		if (!conn.getUserNull().isAdmin()) {
 			return FtpReply.RESPONSE_530_ACCESS_DENIED;
 		}
 
 		if (!request.hasArgument()) {
 			return new FtpReply(
 				501,
 				conn.jprintf(UserManagment.class.getName(), "renuser.usage"));
 		}
 
 		String args[] = request.getArgument().split(" ");
 		if (args.length != 2) {
 			return FtpReply.RESPONSE_501_SYNTAX_ERROR;
 		}
 
 		try {
 			conn.getUserManager().getUserByName(args[0]).rename(args[1]);
 		} catch (NoSuchUserException e) {
 			return new FtpReply(200, "No such user: " + e.getMessage());
 		} catch (ObjectExistsException e) {
 			return new FtpReply(200, "Target username is already taken");
 		} catch (UserFileException e) {
 			return new FtpReply(200, e.getMessage());
 		}
 		return FtpReply.RESPONSE_200_COMMAND_OK;
 	}
 
 	private FtpReply doSITE_SEEN(BaseFtpConnection conn) {
 		FtpRequest request = conn.getRequest();
 		if (!request.hasArgument()) {
 			return new FtpReply(
 				501,
 				conn.jprintf(UserManagment.class.getName(), "seen.usage"));
 		}
 
 		User user;
 		try {
 			user = conn.getUserManager().getUserByName(request.getArgument());
 		} catch (NoSuchUserException e) {
 			return new FtpReply(200, e.getMessage());
 		} catch (UserFileException e) {
 			logger.log(Level.FATAL, "", e);
 			return new FtpReply(
 				200,
 				"Error reading userfile: " + e.getMessage());
 		}
 
 		return new FtpReply(
 			200,
 			"User was last seen: " + new Date(user.getLastAccessTime()));
 	}
 	private FtpReply doSITE_TAGLINE(BaseFtpConnection conn) {
 		FtpRequest request = conn.getRequest();
 		conn.resetState();
 
 		if (!request.hasArgument()) {
 			return new FtpReply(
 				501,
 				conn.jprintf(UserManagment.class.getName(), "tagline.usage"));
 		}
 
 		conn.getUserNull().setTagline(request.getArgument());
 		return FtpReply.RESPONSE_200_COMMAND_OK;
 	}
 	/**
 	 * USAGE: site take <user> <kbytes> [<message>]
 	 *        Removes credit from user
 	 *
 	 *        ex. site take Archimede 100000 haha
 	 *
 	 *        This will remove 100mb of credits from the user 'Archimede' and 
 	 *        send the message haha to him.
 	 */
 	private FtpReply doSITE_TAKE(BaseFtpConnection conn) {
 		FtpRequest request = conn.getRequest();
 		if (!conn.getUserNull().isAdmin()) {
 			return FtpReply.RESPONSE_530_ACCESS_DENIED;
 		}
 		if (!request.hasArgument()) {
 			return new FtpReply(
 				501,
 				conn.jprintf(UserManagment.class.getName(), "take.usage"));
 		}
 		StringTokenizer st = new StringTokenizer(request.getArgument());
 		if (!st.hasMoreTokens()) {
 			return FtpReply.RESPONSE_501_SYNTAX_ERROR;
 		}
 		//String args[] = request.getArgument().split(" ");
 		User user2;
 		long credits;
 
 		try {
 			user2 = conn.getUserManager().getUserByName(st.nextToken());
 			if (!st.hasMoreTokens()) {
 				return FtpReply.RESPONSE_501_SYNTAX_ERROR;
 			}
 			credits = Bytes.parseBytes(st.nextToken()); // B, not KiB
 			if (0 > credits) {
 				return new FtpReply(200, "Credits must be a positive number.");
 			}
 			user2.updateCredits(-credits);
 		} catch (Exception ex) {
 			return new FtpReply(200, ex.getMessage());
 		}
 		return new FtpReply(
 			200,
 			"OK, removed " + credits + "b from " + user2.getUsername() + ".");
 	}
 
 	/**
 	 * USAGE: site user [<user>]
 	 * 	Lists users / Shows detailed info about a user.
 	 * 
 	 * 	ex. site user
 	 * 
 	 * 	This will display a list of all users currently on site.
 	 * 
 	 * 	ex. site user Archimede
 	 * 
 	 * 	This will show detailed information about user 'Archimede'.
 	 */
 	private FtpReply doSITE_USER(BaseFtpConnection conn) {
 		FtpRequest request = conn.getRequest();
 		conn.resetState();
 		if (!conn.getUserNull().isAdmin()
 			&& !conn.getUserNull().isGroupAdmin()) {
 			return FtpReply.RESPONSE_530_ACCESS_DENIED;
 		}
 		if (!request.hasArgument()) {
 			return new FtpReply(
 				501,
 				conn.jprintf(UserManagment.class.getName(), "user.usage"));
 		}
 		FtpReply response = (FtpReply) FtpReply.RESPONSE_200_COMMAND_OK.clone();
 
 		User myUser;
 		try {
 			myUser =
 				conn.getUserManager().getUserByNameUnchecked(
 					request.getArgument());
 		} catch (NoSuchUserException ex) {
 			response.setMessage("User " + request.getArgument() + " not found");
 			return response;
 			//return FtpResponse.RESPONSE_200_COMMAND_OK);
 		} catch (UserFileException ex) {
 			return new FtpReply(200, ex.getMessage());
 		}
 
 		if (conn.getUserNull().isGroupAdmin()
 			&& !conn.getUserNull().getGroupName().equals(myUser.getGroupName())) {
 			return FtpReply.RESPONSE_501_SYNTAX_ERROR;
 		}
 		response.addComment("comment: " + myUser.getComment());
 		response.addComment("username: " + myUser.getUsername());
 		//int i = (int) (myUser.getTimeToday() / 1000);
 		//int hours = i / 60;
 		//int minutes = i - hours * 60;
 		response.addComment("created: " + new Date(myUser.getCreated()));
 		response.addComment(
 			"last seen: " + new Date(myUser.getLastAccessTime()));
 		//response.addComment("time on today: " + hours + ":" + minutes);
 		response.addComment("ratio: " + myUser.getRatio());
 		response.addComment(
 			"credits: " + Bytes.formatBytes(myUser.getCredits()));
 		response.addComment(
 			"group slots: "
 				+ myUser.getGroupSlots()
 				+ " "
 				+ myUser.getGroupLeechSlots());
 		response.addComment("primary group: " + myUser.getGroupName());
 		response.addComment("extra groups: " + myUser.getGroups());
 		response.addComment("ip masks: " + myUser.getIpMasks());
 		response.addComment(
 			"total bytes up: " + Bytes.formatBytes(myUser.getUploadedBytes()));
 		response.addComment(
 			"total bytes dn: "
 				+ Bytes.formatBytes(myUser.getDownloadedBytes()));
 		return response;
 	}
 
 	private FtpReply doSITE_USERS(BaseFtpConnection conn) {
 		FtpRequest request = conn.getRequest();
 		conn.resetState();
 		if (!conn.getUserNull().isAdmin()) {
 			return FtpReply.RESPONSE_530_ACCESS_DENIED;
 		}
 
 		FtpReply response = new FtpReply(200);
 		Collection myUsers;
 		try {
 			myUsers = conn.getUserManager().getAllUsers();
 		} catch (UserFileException e) {
 			logger.log(Level.FATAL, "IO error reading all users", e);
 			return new FtpReply(200, "IO error: " + e.getMessage());
 		}
 
 		if (request.hasArgument()) {
 			Permission perm =
 				new Permission(
 					FtpConfig.makeUsers(
 						new StringTokenizer(request.getArgument())));
 			for (Iterator iter = myUsers.iterator(); iter.hasNext();) {
 				User element = (User) iter.next();
 				if (!perm.check(element))
 					iter.remove();
 			}
 		}
 
 		for (Iterator iter = myUsers.iterator(); iter.hasNext();) {
 			User myUser = (User) iter.next();
 			response.addComment(myUser.getUsername());
 		}
 		return response;
 	}
 	/**
 	 * Lists currently connected users.
 	 */
 	private FtpReply doSITE_WHO(BaseFtpConnection conn) {
 		conn.resetState();
 
 		FtpReply response = (FtpReply) FtpReply.RESPONSE_200_COMMAND_OK.clone();
 
 		try {
 			ReplacerFormat formatup =
 				ReplacerUtils.finalFormat(UserManagment.class, "who.up");
 			ReplacerFormat formatdown =
 				ReplacerUtils.finalFormat(UserManagment.class, "who.down");
 			ReplacerFormat formatidle =
 				ReplacerUtils.finalFormat(UserManagment.class, "who.idle");
 			ReplacerFormat formatcommand =
 				ReplacerUtils.finalFormat(UserManagment.class, "who.command");
 
 			ReplacerEnvironment env = new ReplacerEnvironment();
 
 			List conns = conn.getConnectionManager().getConnections();
 			synchronized (conns) {
 				for (Iterator iter = conns.iterator(); iter.hasNext();) {
 					BaseFtpConnection conn2 = (BaseFtpConnection) iter.next();
 					if (conn2.isAuthenticated()) {
 						User user;
 						try {
 							user = conn2.getUser();
 						} catch (NoSuchUserException e) {
 							continue;
 						}
 						if (conn
 							.getConfig()
 							.checkHideInWho(user, conn2.getCurrentDirectory()))
 							continue;
 						//StringBuffer status = new StringBuffer();
 						env.add(
 							"idle",
 							(System.currentTimeMillis()
 								- conn2.getLastActive())
 								/ 1000
 								+ "s");
 						env.add("targetuser", user.getUsername());
 
 						if (!conn2.isExecuting()) {
 							response.addComment(
 								SimplePrintf.jprintf(formatidle, env));
 
 						} else if (
 							conn2.getDataConnectionHandler().isTransfering()) {
 							if (conn2
 								.getDataConnectionHandler()
 								.isTransfering()) {
 								try {
 									env.add(
 										"speed",
 										Bytes.formatBytes(
 											conn2
 												.getDataConnectionHandler()
 												.getTransfer()
 												.getXferSpeed())
 											+ "/s");
 								} catch (RemoteException e2) {
 									logger.warn("", e2);
 								}
 								env.add(
 									"file",
 									conn2
 										.getDataConnectionHandler()
 										.getTransferFile()
 										.getName());
 								env.add(
 									"slave",
 									conn2
 										.getDataConnectionHandler()
 										.getTranferSlave()
 										.getName());
 							}
 
 							if (conn2.getTransferDirection()
 								== Transfer.TRANSFER_RECEIVING_UPLOAD) {
 								response.addComment(
 									SimplePrintf.jprintf(formatup, env));
 
 							} else if (
 								conn2.getTransferDirection()
 									== Transfer.TRANSFER_SENDING_DOWNLOAD) {
 								response.addComment(
 									SimplePrintf.jprintf(formatdown, env));
 							}
 						} else {
 							env.add("command", conn2.getRequest().getCommand());
 							response.addComment(
 								SimplePrintf.jprintf(formatcommand, env));
 						}
 					}
 				}
 			}
 			return response;
 		} catch (FormatterException e) {
 			return new FtpReply(200, e.getMessage());
 		}
 	}
 
 	/* (non-Javadoc)
 	 * @see net.sf.drftpd.master.command.CommandHandler#execute(net.sf.drftpd.master.BaseFtpConnection)
 	 */
 	public FtpReply execute(BaseFtpConnection conn)
 		throws UnhandledCommandException {
 		String cmd = conn.getRequest().getCommand();
 		if ("SITE ADDIP".equals(cmd))
 			return doSITE_ADDIP(conn);
 		if ("SITE CHANGE".equals(cmd))
 			return doSITE_CHANGE(conn);
 		if ("SITE CHGRP".equals(cmd))
 			return doSITE_CHGRP(conn);
 		if ("SITE CHPASS".equals(cmd))
 			return doSITE_CHPASS(conn);
 		if ("SITE DELIP".equals(cmd))
 			return doSITE_DELIP(conn);
 		if ("SITE DELUSER".equals(cmd))
 			return doSITE_DELUSER(conn);
 		if ("SITE ADDUSER".equals(cmd) || "SITE GADDUSER".equals(cmd))
 			return doSITE_ADDUSER(conn);
 		if ("SITE GINFO".equals(cmd))
 			return doSITE_GINFO(conn);
 		if ("SITE GIVE".equals(cmd))
 			return doSITE_GIVE(conn);
 		if ("SITE GROUPS".equals(cmd))
 			return doSITE_GROUPS(conn);
 		if ("SITE KICK".equals(cmd))
 			return doSITE_KICK(conn);
 		if ("SITE PASSWD".equals(cmd))
 			return doSITE_PASSWD(conn);
 		if ("SITE PURGE".equals(cmd))
 			return doSITE_PURGE(conn);
 		if ("SITE READD".equals(cmd))
 			return doSITE_READD(conn);
 		if ("SITE RENUSER".equals(cmd))
 			return doSITE_RENUSER(conn);
 		if ("SITE TAGLINE".equals(cmd))
 			return doSITE_TAGLINE(conn);
 		if ("SITE TAKE".equals(cmd))
 			return doSITE_TAKE(conn);
 		if ("SITE USER".equals(cmd))
 			return doSITE_USER(conn);
 		if ("SITE USERS".equals(cmd))
 			return doSITE_USERS(conn);
 		if ("SITE WHO".equals(cmd))
 			return doSITE_WHO(conn);
 		throw UnhandledCommandException.create(
 			UserManagment.class,
 			conn.getRequest());
 	}
 
 	public String[] getFeatReplies() {
 		return null;
 	}
 
 	public CommandHandler initialize(
 		BaseFtpConnection conn,
 		CommandManager initializer) {
 		return this;
 	}
 	public void load(CommandManagerFactory initializer) {
 	}
 
 	public void unload() {
 	}
 
 }
