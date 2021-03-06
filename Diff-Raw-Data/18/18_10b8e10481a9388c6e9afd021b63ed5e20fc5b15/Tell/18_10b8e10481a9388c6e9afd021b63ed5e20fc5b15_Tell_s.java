 import uk.co.uwcs.choob.*;
 import uk.co.uwcs.choob.modules.*;
 import uk.co.uwcs.choob.support.*;
 import uk.co.uwcs.choob.support.events.*;
 import java.util.*;
 import java.util.regex.*;
 
 public class TellObject
 {
 	public int id;
 	public String type;
 	public long date;
 	public String from;
 	public String message;
 	public String target;
 	public boolean nickServ;
 }
 
 class TellData
 {
 	boolean valid;
 	int error;
 	String from;
 	String[] targets;
 	String message;
 	long date;
 	String type;
 }
 
 public class Tell
 {
 	private static int MAXTARGETS = 7;
 	private static long CACHEEXPIRE = 60 * 60 * 1000; // 5 mins
 
 	public String[] info()
 	{
 		return new String[] {
 			"A plugin to allow users to leave each other messages.",
 			"The Choob Team",
 			"choob@uwcs.co.uk",
 			"$Rev$$Date$"
 		};
 	}
 
 	private Modules mods;
 	private IRCInterface irc;
 
 	private HashMap<String,Long> tellCache;
 
 	public Tell (Modules mods, IRCInterface irc)
 	{
 		this.mods = mods;
 		this.irc = irc;
 		this.tellCache = new HashMap<String,Long>();
 	}
 
 	public String[] helpTopics = { "Using", "Security", "Cache" };
 
 	public String[] helpUsing = {
 		  "Tell is a plugin that allows you to send messages to people who"
 		+ " aren't around at the moment. When they next speak, the bot will"
 		+ " let them know."
 	};
 
 	public String[] helpSecurity = {
 		  "Tells currently use NickServ in the following way:",
 		  "If the nick you send to's base nickname exists in NickServ (eg."
 		+ " you said to 'bob|sleep' and 'bob' is registered), the tell is"
 		+ " marked secure. This means that bob can only pick up the message if"
 		+ " he is identified with NickServ, on a name that the bot both"
 		+ " considers equivalent normally to 'bob' (like, say, 'bob|awake')"
 		+ " AND considers securely equivalent, too (ie. is linked to bob).",
 		  "You should note that this means, in particular, that people who's"
 		+ " root username is not equal to their base nickname can't receive"
 		+ " tells at all! That is, if 'bob|bot' is bob's root username, he"
 		+ " will never receive tells."
 	};
 
 	public String[] helpCache = {
 		  "Whether or not you have a tell is cached for " + (CACHEEXPIRE / 1000)
 		+ " seconds. This is to stop tell continually telling you you have"
 		+ " tells but aren't identified. If you DO identify with NickServ"
 		+ " therefore, you need to use the Tell.Get command to reset this and"
 		+ " try again. Note that NickServ status is also cached."
 	};
 
 	public synchronized int apiInject(String from, String[] targets, String message, String type)
 	{
 		TellData tell = validateTell(from, targets, message, (new Date()).getTime(), type);
 		if (!tell.valid)
 			return tell.error;
 		return 16 + doTell(tell);
 	}
 
 	public String[] helpCommandSend = {
 		"Send a tell to the given nickname.",
 		"<Nick>[,<Nick>...] <Message>",
 		"<Nick> is the target of the tell",
 		"<Message> is the content"
 	};
 
 	synchronized TellData validateTell(final String from, final String[] targets, final String message, final long date, final String type)
 	{
 		TellData rv = new TellData();
 		rv.valid = true;
 		rv.error = 0;
 		rv.from = from;
 		rv.targets = null;
 		rv.message = message;
 		rv.date = date;
 		rv.type = type;
 
 		// Only include unique targets.
 		final List<String> validTargets = new ArrayList<String>(MAXTARGETS);
 		for (int i = 0; i < targets.length; i++)
 		{
 			String targetNick = mods.nick.getBestPrimaryNick(targets[i]);
 			String rootTargetNick = mods.security.getRootUser(targetNick);
 
 			String target = rootTargetNick != null ? rootTargetNick : targetNick;
 
 			// Make sure we don't dup targets.
 			if (validTargets.contains(target))
 				continue;
 
 			validTargets.add(target);
 		}
 		rv.targets = validTargets.toArray(new String[0]);
 
 		// Check we're not going to too many people.
 		if (rv.targets.length > MAXTARGETS)
 			rv.error = 1;
 
 		if (rv.error != 0)
 			rv.valid = false;
 
 		return rv;
 	}
 
 	synchronized int doTell(TellData tell)
 	{
 		if (!tell.valid)
 			return 0;
 
 		final TellObject tellObj = new TellObject();
 
 		// Note: This is intentionally not translated to a primary nick.
 		tellObj.from = tell.from;
 		tellObj.message = tell.message;
 		tellObj.date = tell.date;
 		tellObj.type = tell.type;
 
 		final String[] targets = tell.targets;
 
 		mods.odb.runTransaction(
 			new ObjectDBTransaction()
 			{
 				public void run()
 				{
 					for(int i=0; i<targets.length; i++)
 					{
 						tellObj.id = 0;
 						tellObj.target = targets[i];
 						tellObj.nickServ = nsStatus(tellObj.target) > 0;
 
 						clearCache(tellObj.target);
 						save(tellObj);
 					}
 				}
 			}
 		);
 		
 		// Send e-mails to people who've asked for them.
 		try
 		{
 			for(int i = 0; i < targets.length; i++)
 			{
 				String email = (String)mods.plugin.callAPI("Options", "GetUserOption", targets[i], "Email", "");
 				if (email.length() > 0)
 				{
 					mods.plugin.callAPI("Mail", "SendMail", email,
 					                    "Tell from " + tellObj.from + " via " + irc.getNickname(),
 					                    "At " + new Date(tellObj.date) + ", " + tellObj.from + " told me to " + tellObj.type + " you: " + tellObj.message);
 				}
 			}
 			
 		}
 		catch (ChoobNoSuchCallException e)
 		{
 			// Oh dear, no Options. :(
 		}
 
 		return targets.length;
 
 	}
 
 	public synchronized void commandSend( Message mes )
 	{
 		String[] params = mods.util.getParamArray(mes, 2);
 		if (params.length <= 2)
 		{
 			irc.sendContextReply(mes, "Syntax: 'Tell.Send " + helpCommandSend[1] + "'");
 			return;
 		}
 
		final boolean question=params[2].indexOf('?', params[2].length()-5)!=-1; // It's a question if it has a "?" within 5 characters of it's end. (ie. "? :)").
 
		final String type=question ? "ask" : "tell";
 		final String[] targets = params[1].split(", *");
 		final long time = mes.getMillis();
 
 		final TellData tell = validateTell(mes.getNick(), targets, params[2], time, type);
 
 		if (!tell.valid)
 		{
 			switch (tell.error)
 			{
 				case 1:
 					irc.sendContextReply(mes, "Sorry, you're only allowed " + MAXTARGETS + " targets for a given tell.");
 					return;
 				default:
 					irc.sendContextReply(mes, "Unknown tell error code: " + tell.error);
 					return;
 			}
 		}
 
 		int count = tell.targets.length;
		Map<String,String> mesFlags = ((IRCEvent)mes).getFlags();
 		if (!mesFlags.containsKey("timedevents.delayed"))
 		{
 			irc.sendContextReply(mes, "Okay, will " + type + " upon next speaking. (Sent to " + count + " " + (count == 1 ? "person" : "people") + ".)");
 		}
 
 		doTell(tell);
 	}
 
 	public String[] helpCommandGet = {
 		"Get any tells that have been sent to you. See Cache."
 	};
 	public void commandGet( Message mes )
 	{
 		clearCache(mes.getNick());
 		loudspew( mes.getNick() );
 		irc.sendContextReply(mes, "OK, if you had any tells, I just sent 'em. :)");
 	}
 
 	private void clearCache( String nick )
 	{
 		String pnick = mods.nick.getBestPrimaryNick(nick);
 		String rnick = mods.security.getRootUser(pnick);
 		if (rnick == null)
 			rnick = pnick;
 
 		synchronized(tellCache)
 		{
 			Iterator<String> iter = tellCache.keySet().iterator();
 			while(iter.hasNext())
 			{
 				String item = iter.next();
 				String pitem = mods.nick.getBestPrimaryNick(item);
 				String ritem = mods.security.getRootUser(pitem);
 				if (ritem == null)
 					ritem = pitem;
 
 				if (ritem.equalsIgnoreCase(rnick))
 					iter.remove();
 			}
 		}
 	}
 
 	public String[] optionsUser = { "Secure", "NickChange", "Email" };
 	public String[] optionsUserDefaults = { "1", "1", "0" };
 
 	public boolean optionCheckUserSecure( String value, String userName ) { return value.equals("0") || value.equals("1") || value.equals("2"); }
 	public String[] helpOptionSecure = {
 		"Choose the security level of your tells.",
 		"Set this to \"0\" to not have secure tells (no NickServ required), \"1\" to make them require NickServ, or \"2\" to make them require both NickServ and that your nicknames are linked in the bot."
 	};
 
 	public boolean optionCheckUserNickChange( String value, String userName ) { return value.equals("0") || value.equals("1"); }
 	public String[] helpOptionNickChange = {
 		"Choose to have tells delivered on nick change.",
 		"Set this to \"0\" to not have tells delivered on nick change, default, \"1\", will deliver on nick change."
 	};
 
 	public boolean optionCheckUserEmail( String value, String userName ) { return true; }
 	public String[] helpOptionEmail = {
 		"Choose to have a copy of all tells e-mailed to you.",
 		"Set this to an e-mail address to have copies of all tells sent to you mailed. Set to '' to stop."
 	};
 
 	private synchronized void spew (String nick)
 	{
 		try
 		{
 			loudspew(nick);
 		}
 		catch (Exception e)
 		{
 			System.err.println("Tell.spew suppressed error:");
 			e.printStackTrace();
 		}
 	}
 
 	private synchronized void loudspew( String nick )
 	{
 		// Use the cache
 		boolean willSkip = false;
 		synchronized(tellCache)
 		{
 			Long cache = tellCache.get(nick);
 			if (cache != null && cache > System.currentTimeMillis())
 				willSkip = true;
 			tellCache.put(nick, System.currentTimeMillis() + CACHEEXPIRE);
 		}
 		if (willSkip)
 			return;
 
 		// getBestPrimaryNick should be safe from injection
 		String testNick = mods.nick.getBestPrimaryNick( nick );
 		// rootNick won't, necessarily
 		String rootNick = mods.security.getRootUser( testNick );
 
 		List<TellObject> results;
 		if (rootNick != null && !rootNick.equals(testNick))
 			results = mods.odb.retrieve (TellObject.class, "WHERE target = '" + mods.odb.escapeString(testNick) + "' OR target = '" + mods.odb.escapeString(rootNick) + "'");
 		else
 			results = mods.odb.retrieve (TellObject.class, "WHERE target = '" + mods.odb.escapeString(testNick) + "'");
 
 		if (results.size() != 0)
 		{
 			int nsStatus = -1;
 			for (int i=0; i < results.size(); i++ )
 			{
 				TellObject tellObj = (TellObject)results.get(i);
 				if (tellObj.nickServ)
 				{
 					if (nsStatus == -1)
 					{
 						// NickServ not yet checked...
 
 						// First pick up the setting of Secure.
 						int secureOption;
 						try
 						{
 							String val = (String)mods.plugin.callAPI("Options", "GetUserOption", nick, "Secure", "1" );
 							secureOption = Integer.parseInt(val);
 						}
 						catch (Throwable e)
 						{
 							// No such call(default) or number format issue(!)
 							secureOption = 1;
 						}
 						if (secureOption > 2 || secureOption < 0)
 							secureOption = 1;
 
 						// This is a secure tell. One of several things can happen.
 						if ( secureOption == 2 )
 						{
 							// If secure tell is set, we require the
 							// actual nickname to be explicitly linked
 							// to the root.
 							String secureRootNick = mods.security.getRootUser( nick );
 							if (rootNick != null)
 							{
 								// rootNick is set and we're directed at it.
 								// Hence must check root of real nick is
 								// equal to rootNick.
 								if ( !rootNick.equalsIgnoreCase(secureRootNick) )
 									nsStatus = -2;
 							}
 							else
 							{
 								// rootNick is NOT set. Since Secure
 								// operates on bot users, and the user
 								// hasn't registered his, we tell him to
 								// bugger off.
 								nsStatus = -3;
 							}
 						}
 						else
 						{
 							// If not, just the primary will do.
 						}
 
 						if (nsStatus == -1)
 						{
 							// No errors from the above...
 							if (secureOption != 0)
 							{
 								// We require NS auth.
 								nsStatus = nsStatus( nick );
 							}
 							else
 							{
 								// We don't require NS auth.
 								nsStatus = 3;
 							}
 						}
 					}
 					// If all the above ran and we're allowed to send, nsStatus
 					// is 3. Otherwise it's >= -1, <= 2.
 					if (nsStatus != 3)
 						continue;
 				}
 				irc.sendMessage(nick, "At " + new Date(tellObj.date) + ", " + tellObj.from + " told me to " + tellObj.type + " you: " + tellObj.message);
 				mods.odb.delete(results.get(i));
 			}
 			if (nsStatus == -2)
 				irc.sendMessage(nick, "Hi! I think you have tells, and you have set Secure=2, but your nickname isn't linked to " + rootNick + ". See Help.Help Security.UsingLink to do this, then do Tell.Get.");
 			else if (nsStatus == -3)
 				irc.sendMessage(nick, "Hi! I think you have tells, and you have set Secure=2, but you haven't actually registered " + testNick + " with the bot. Since this defeats the point of secure tells, I suggest you register it (Security.AddUser), then link this nickname to it (See Help.Help Security.UsingLink).");
 			else if (nsStatus == 0)
 				irc.sendMessage(nick, "Hi! I think you (" + testNick + ") have tells, but you haven't actually registered your nickname with NickServ. Since " + testNick + " was registered and you haven't set Secure=0, you need to register with this nickname or change to " + testNick + " to pick up your tells.");
 			else if (nsStatus > 0 && nsStatus < 3)
 				irc.sendMessage(nick, "Hi! You have tells, but you're not identified with NickServ! Once you've done so, use the Tell.Get command.");
 		}
 	}
 
 	public void onAction( ChannelAction ev )
 	{
 		if (ev.getSynthLevel() > 0)
 			return;
 		spew(ev.getNick());
 	}
 
 	public void onMessage( ChannelMessage ev )
 	{
 		if (ev.getSynthLevel() > 0)
 			return;
 		spew(ev.getNick());
 	}
 
 	public void onPrivateMessage( PrivateMessage ev )
 	{
 		if (ev.getSynthLevel() > 0)
 			return;
 		spew(ev.getNick());
 	}
 
 	public void onPrivateAction( PrivateAction ev )
 	{
 		if (ev.getSynthLevel() > 0)
 			return;
 		spew(ev.getNick());
 	}
 
 	public void onJoin( ChannelJoin ev )
 	{
 		spew(ev.getNick());
 	}
 
 	public void onNickChange( NickChange ev )
 	{
 		try
 		{
 			if (((String)mods.plugin.callAPI("Options", "GetUserOption", ev.getNewNick(), "NickChange", "1" )).equals("0"))
 				return;
 		}
 		catch (ChoobNoSuchCallException e)
 		{} // Non-issue.
 
 		try
 		{
 			Thread.sleep(1000);
 		}
 		catch (InterruptedException e)
 		{
 			// Bah, who cares? :)
 		}
 		spew(ev.getNewNick());
 	}
 
 	private int nsStatus( String nick )
 	{
 		try
 		{
 			return (Integer)mods.plugin.callAPI("NickServ", "Status", nick);
 		}
 		catch (ChoobNoSuchCallException e)
 		{
 			return 0;
 		}
 	}
 }
 
