 /*
  * Haxy plugin loader that compiles with the Sun javac API and stuff.
  * @author bucko
  */
 
 package uk.co.uwcs.choob.plugins;
 
 import java.io.*;
 import java.lang.reflect.*;
 import java.net.URL;
 import java.net.URLConnection;
 import java.security.*;
 import java.util.*;
 import java.util.regex.*;
 
 import javax.tools.*;
 
 import uk.co.uwcs.choob.ChoobPluginManager;
 import uk.co.uwcs.choob.ChoobTask;
 import uk.co.uwcs.choob.modules.Modules;
 import uk.co.uwcs.choob.support.*;
 import uk.co.uwcs.choob.support.events.Event;
 import uk.co.uwcs.choob.support.events.Message;
 
 public final class HaxSunPluginManager extends ChoobPluginManager
 {
 	private final Modules mods;
 	private final IRCInterface irc;
 
 	private final JavaCompiler compiler;
 
 	private final static String prefix = "pluginData";
 	private final ChoobPluginMap allPlugins;
 
 	public HaxSunPluginManager(Modules mods, IRCInterface irc)
 			throws ChoobException {
 		this.mods = mods;
 		this.irc = irc;
 		this.allPlugins = new ChoobPluginMap();
 		this.compiler = ToolProvider.getSystemJavaCompiler();
 		if (compiler == null) {
 			throw new ChoobException("Compiler class not found.");
 		}
 	}
 
 	private String compile(final String[] fileNames, final String classPath) throws ChoobException
 	{
 		ByteArrayOutputStream baos = new ByteArrayOutputStream();
 		final PrintWriter output = new PrintWriter(baos);
 
 		final StandardJavaFileManager fileManager = compiler
 				.getStandardFileManager(null, null, null);
 		
 		final File outputLocation = new File(classPath);
 		final List<File> outputLocationList = new ArrayList<File>();
 		outputLocationList.add(outputLocation);
 		
 		try {
 			fileManager.setLocation(StandardLocation.CLASS_OUTPUT, outputLocationList);
 		} catch (IOException e) {
 			throw (ChoobException) (e.getCause());
 		}
 		final Iterable<? extends JavaFileObject> compilationUnit = fileManager
 			.getJavaFileObjectsFromStrings(Arrays.asList(fileNames));
 		final boolean success;
 
 		try {
 			success = AccessController
 					.doPrivileged(new PrivilegedExceptionAction<Boolean>() {
 						public Boolean run() {
 							return compiler.getTask(output, fileManager, null,
 									null, null, compilationUnit).call();
 						}
 					});
 		} catch (PrivilegedActionException e) {
 			throw (ChoobException) (e.getCause());
 		}
 
 		try {
 			fileManager.close();
 		} catch (IOException e) {
 			throw (ChoobException) (e.getCause());
 		}
 
 		String baosts = baos.toString();
 
 		if (success)
 			return baosts; // success
 		else
 		{
 			System.out.println(baosts);
 
 			String excep="Compile failed.";
 
 			try
 			{
 				String url=(String)mods.plugin.callAPI("Http", "StoreString", baosts);
 				excep="Compile failed, see: " + url + " for details.";
 			}
 			catch (Exception e) {}
 			throw new ChoobException(excep);
 		}
 	}
 
 	private String[] makeJavaFiles(String pluginName, String outDir, InputStream in) throws IOException
 	{
 		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
 		String line;
 		StringBuffer imps = new StringBuffer();
		StringBuffer ants = new StringBuffer();
 		PrintStream classOut = null;
 		List<String> fileNames = new ArrayList<String>();
 		int skipLines = 0;
 		while((line = reader.readLine()) != null)
 		{
 			if (line.startsWith("class "))
 				line = "public " + line;
 
 			if (line.startsWith("import "))
 			{
 				skipLines--; // imports get added anyway...
 				imps.append(line + "\n");
 			}
			else if (line.startsWith("@"))
			{
				ants.append(line + "\n");
				continue;
			}
 			else if (line.startsWith("package "))
 			{
 				// Squelch
 			}
 			else if (line.startsWith("public class "))
 			{
 				String[] bits = line.split(" ");
 				String className = bits[2];
 				String fileName = outDir + className + ".java";
 				fileNames.add(fileName);
 				File javaFile = new File(fileName);
 				if (classOut != null)
 				{
 					classOut.flush();
 					classOut.close();
 				}
 				classOut = new PrintStream(new FileOutputStream(javaFile));
 				classOut.print("package plugins." + pluginName + ";");
 				classOut.print(imps);
 				for(int i=0; i<skipLines; i++)
 					classOut.print("\n");
				classOut.print(ants);
				ants = new StringBuffer();
 			}
 			skipLines++;
 			if (classOut != null)
 				classOut.println(line);
 		}
 		if (classOut != null)
 		{
 			classOut.flush();
 			classOut.close();
 		}
 		return fileNames.toArray(new String[fileNames.size()]);
 	}
 
 	protected Object createPlugin(String pluginName, URL source) throws ChoobException
 	{
 		String classPath = prefix + File.separator + pluginName + File.separator;
 		if (source != null)
 		{
 			File javaDir = new File(classPath);
 			String javaFileName = classPath + pluginName + ".java";
 			File javaFile = new File(javaFileName);
 			URLConnection sourceConn;
 			try
 			{
 				sourceConn = source.openConnection();
 			}
 			catch (IOException e)
 			{
 				throw new ChoobException("Problem opening connection: " + e);
 			}
 			if (sourceConn.getLastModified() > javaFile.lastModified() || sourceConn.getLastModified() == 0)
 			{
 				// needs updating
 				InputStream in = null;
 				boolean success = false;
 				try
 				{
 					javaDir.mkdirs();
 					in = sourceConn.getInputStream();
 					String[] names = makeJavaFiles(pluginName, classPath, in);
 					compile(names, classPath);
 					// This should help to aleviate timezone differences.
 					javaFile.setLastModified( sourceConn.getLastModified() );
 					success = true;
 				}
 				catch (IOException e)
 				{
 					throw new ChoobException("Failed to set up for compiler: " + e);
 				}
 				finally
 				{
 					try {
 						if (in != null)
 							in.close();
 					} catch (IOException e) {}
 					
 					if (!success)
 						javaFile.delete();
 				}
 			}
 		}
 		ClassLoader loader = new HaxSunPluginClassLoader(pluginName, classPath, getProtectionDomain(pluginName));
 		try
 		{
 			return instantiatePlugin(loader.loadClass("plugins." + pluginName + "." + pluginName), pluginName);
 		}
 		catch (ClassNotFoundException e)
 		{
 			throw new ChoobException("Could not find plugin class for " + pluginName + ": " + e);
 		}
 	}
 
 	protected Object instantiatePlugin(Class<?> newClass, String pluginName) throws ChoobException
 	{
 		Object pluginObj=null;
 
 		// Squiggly brackets are for the weak.
 
 		Constructor<?> c[] = newClass.getConstructors();
 
 		for (int i = 0; i < c.length; i++)
 			try {
 				Class<?>[] t = c[i].getParameterTypes();
 				Object[] arg = new Object[t.length];
 
 				for (int j=0; j<t.length; j++)
 					if (t[j] == IRCInterface.class)
 						arg[j]=irc;
 					else
 						if (t[j] == Modules.class)
 							arg[j]=mods;
 						else
 							throw new ChoobException("Unknown parameter in constructor.");
 				pluginObj = c[i].newInstance(arg);
 				break;
 			}
 			catch (IllegalAccessException e)
 			{
 				throw new ChoobException("Plugin " + pluginName + " had no constructor (this error shouldn't occour, something serious is wrong): " + e);
 			}
 			catch (InvocationTargetException e)
 			{
 				throw new ChoobException("Plugin " + pluginName + "'s constructor threw an exception: " + e.getCause(), e.getCause());
 			}
 			catch (InstantiationException e)
 			{
 				throw new ChoobException("Plugin " + pluginName + "'s constructor threw an exception: " + e.getCause(), e.getCause());
 			}
 
 		try
 		{
 			// XXX Isn't this a bit pointless? newInstance() calls the
 			// (redefinable) constructor anyway...
 			Method meth = newClass.getMethod("create");
 			meth.invoke(pluginObj);
 
 		}
 		catch (NoSuchMethodException e)
 		{
 			// This is nonfatal and in fact to be expected!
 		}
 		catch (IllegalAccessException e)
 		{
 			// So is this.
 		}
 		catch (InvocationTargetException e)
 		{
 			// This isn't.
 			throw new ChoobException("Plugin " + pluginName + "'s create() threw an exception: " + e.getCause(), e.getCause());
 		}
 
 		String[] newCommands = new String[0];
 		String[] oldCommands = new String[0];
 		synchronized(allPlugins)
 		{
 			List<String> coms = allPlugins.getCommands(pluginName);
 			if (coms != null)
 				oldCommands = coms.toArray(oldCommands);
 
 			allPlugins.resetPlugin(pluginName, pluginObj);
 
 			coms = allPlugins.getCommands(pluginName);
 			if (coms != null)
 				newCommands = coms.toArray(newCommands);
 		}
 
 		for(int i=0; i<oldCommands.length; i++)
 			removeCommand(oldCommands[i]);
 		for(int i=0; i<newCommands.length; i++)
 			addCommand(newCommands[i]);
 
 		return pluginObj;
 	}
 
 	protected void destroyPlugin(String pluginName)
 	{
 		String[] oldCommands = new String[0];
 		synchronized(allPlugins)
 		{
 			List<String> coms = allPlugins.getCommands(pluginName);
 			if (coms != null)
 				oldCommands = coms.toArray(oldCommands);
 		}
 
 		// Cleanup is actually pretty easy.
 		synchronized(allPlugins)
 		{
 			allPlugins.resetPlugin(pluginName, null);
 		}
 
 		for(int i=0; i<oldCommands.length; i++)
 			removeCommand(oldCommands[i]);
 	}
 
 	/**
 	 * Go through the horror of method resolution. Fields are regarded as a
 	 * method that takes no parameters.
 	 *
 	 * @param methods The list of things to search.
 	 * @param args The arguments of the method we hope to resolve.
 	 */
 	private Member javaHorrorMethodResolve(List<Member> methods, Object[] args)
 	{
 		// Do any of them have the right signature?
 		List<Member> filtered = new LinkedList<Member>();
 		Iterator<Member> it = methods.iterator();
 		while(it.hasNext())
 		{
 			Member thing = it.next();
 			if (thing instanceof Field)
 			{
 				if (args.length == 0)
 					filtered.add(thing);
 			}
 			else
 			{
 				// Is a method
 				Method method = (Method) thing;
 				Class<?>[] types = method.getParameterTypes();
 				int paramlength = types.length;
 				if (paramlength != args.length)
 					continue;
 
 				boolean okness = true;
 				for(int j=0; j<paramlength; j++)
 				{
 					if (!types[j].isInstance(args[j]))
 					{
 						okness = false;
 						break;
 					}
 				}
 				if (okness)
 					filtered.add(method);
 			}
 		}
 
 		// Right, have a bunch of applicable methods.
 		// TODO: Technically should pick most specific.
 
 		if (filtered.size() == 0)
 			return null;
 
 		return filtered.get(0);
 	}
 
 	private ChoobTask callCommand(final Method meth, final Object param)
 	{
 		final String pluginName = meth.getDeclaringClass().getSimpleName();
 		final Object plugin = allPlugins.getPluginObj(pluginName);
 		final Object[] params;
 		if (meth.getParameterTypes().length == 1)
 			params = new Object[] { param };
 		else
 			params = new Object[] { param, mods, irc };
 
 		return new ChoobTask(pluginName) {
 			public void run() {
 				try
 				{
 					meth.invoke(plugin, params);
 				}
 				catch (InvocationTargetException e)
 				{
 					// Let the user know what happened.
 					if (param instanceof Message)
 					{
 						Throwable cause = e.getCause();
 
 						mods.plugin.exceptionReply((Message)param, cause, pluginName);
 					}
 					System.err.println("Exception invoking method " + meth);
 					e.getCause().printStackTrace();
 				}
 				catch (IllegalAccessException e)
 				{
 					System.err.println("Could not access method " + meth);
 					e.printStackTrace();
 				}
 			}
 		};
 	}
 
 	public ChoobTask commandTask(String pluginName, String command, Message ev)
 	{
 		Method meth = allPlugins.getCommand(pluginName + "." + command);
 		if(meth != null)
 			return callCommand(meth, ev);
 		return null;
 	}
 
 	public List<ChoobTask> eventTasks(Event ev)
 	{
 		List<ChoobTask> tasks = new LinkedList<ChoobTask>();
 		List<Method> meths = allPlugins.getEvent(ev.getMethodName());
 		if(meths != null)
 			for(Method meth: meths)
 			{
 				tasks.add(callCommand(meth, ev));
 			}
 		return tasks;
 	}
 
 	public List<ChoobTask> filterTasks(Message ev)
 	{
 		List<ChoobTask> tasks = new LinkedList<ChoobTask>();
 		List<Method> meths = allPlugins.getFilter(ev.getMessage());
 		if(meths != null)
 			for(Method meth: meths)
 			{
 				tasks.add(callCommand(meth, ev));
 			}
 		return tasks;
 	}
 
 	public ChoobTask intervalTask(String pluginName, Object param)
 	{
 		Method meth = allPlugins.getInterval(pluginName);
 		if(meth != null)
 			return callCommand(meth, param);
 		return null;
 	}
 
 	public Object doGeneric(String pluginName, String prefix, String genName, final Object... params) throws ChoobNoSuchCallException
 	{
 		final Object plugin = allPlugins.getPluginObj(pluginName);
 		String fullName = pluginName + "." + prefix + ":" + genName;
 		String sig = getAPISignature(fullName, params);
 		if (plugin == null)
 			throw new ChoobNoSuchPluginException(pluginName, sig);
 
 		Member meth = allPlugins.getGeneric(sig);
 		if (meth == null)
 		{
 			// OK, not cached. But maybe it's still there...
 			List<Member> meths = allPlugins.getAllGeneric(fullName);
 
 			if (meths != null)
 				meth = javaHorrorMethodResolve(meths, params);
 
 			if (meth != null)
 				allPlugins.setGeneric(sig, meth);
 			else
 				throw new ChoobNoSuchCallException(pluginName, sig);
 		}
 		final Member meth2 = meth;
 		try
 		{
 			return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
 				public Object run() throws InvocationTargetException, IllegalAccessException {
 					if (meth2 instanceof Method)
 						return ((Method)meth2).invoke(plugin, params);
 					else
 						return ((Field)meth2).get(plugin);
 				}
 			}, mods.security.getPluginContext() );
 		}
 		catch (PrivilegedActionException pe)
 		{
 			Throwable e = pe.getCause();
 			if (e instanceof InvocationTargetException)
 			{
 				if (e.getCause() instanceof ChoobError)
 					// Doesn't need wrapping...
 					throw (ChoobError)e.getCause();
 				else
 					throw new ChoobInvocationError(pluginName, fullName, e.getCause());
 			}
 			else if (e instanceof IllegalAccessException)
 				throw new ChoobError("Could not access method " + fullName + ": " + e);
 			else
 				throw new ChoobError("Unknown error accessing method " + fullName + ": " + e);
 		}
 	}
 
 	public Object doAPI(String pluginName, String APIName, final Object... params) throws ChoobNoSuchCallException
 	{
 		return doGeneric(pluginName, "api", APIName, params);
 	}
 
 	// Helper methods for the class below.
 	static boolean checkCommandSignature(Method meth)
 	{
 		Class<?>[] params = meth.getParameterTypes();
 		if (params.length == 1)
 		{
 			if (!Message.class.isAssignableFrom(params[0]))
 				return false;
 		}
 		else if (params.length == 3)
 		{
 			if (!Message.class.isAssignableFrom(params[0]))
 				return false;
 			if (!params[1].isAssignableFrom(Modules.class))
 				return false;
 			if (!params[2].isAssignableFrom(IRCInterface.class))
 				return false;
 		}
 		else
 			return false;
 
 		return true;
 	}
 
 	static boolean checkEventSignature(Method meth)
 	{
 		Class<?>[] params = meth.getParameterTypes();
 		if (params.length == 1)
 		{
 			// XXX could be better
 			if (!Event.class.isAssignableFrom(params[0]))
 				return false;
 		}
 		else if (params.length == 3)
 		{
 			if (!Event.class.isAssignableFrom(params[0]))
 				return false;
 			if (!params[1].isAssignableFrom(Modules.class))
 				return false;
 			if (!params[2].isAssignableFrom(IRCInterface.class))
 				return false;
 		}
 		else
 			return false;
 
 		return true;
 	}
 
 	static boolean checkIntervalSignature(Method meth)
 	{
 		Class<?>[] params = meth.getParameterTypes();
 		if (params.length == 1)
 		{
 			// OK
 		}
 		else if (params.length == 3)
 		{
 			if (!params[1].isAssignableFrom(Modules.class))
 				return false;
 			if (!params[2].isAssignableFrom(IRCInterface.class))
 				return false;
 		}
 		else
 			return false;
 
 		return true;
 	}
 
 	static boolean checkAPISignature(Method meth)
 	{
 		return true;
 	}
 
 	static String getAPISignature(String APIName, Object... args)
 	{
 		StringBuffer buf = new StringBuffer(APIName + "(");
 		for(int i=0; i<args.length; i++)
 		{
 			if (args[i] != null)
 				buf.append(args[i].getClass().getSimpleName());
 			else
 				buf.append("null");
 
 			if (i < args.length - 1)
 				buf.append(",");
 		}
 		buf.append(")");
 
 		return buf.toString();
 	}
 }
 
 /**
  * Caches all sorts of plugin info
  */
 final class ChoobPluginMap
 {
 	// Name -> Plugin object
 	private final Map<String,Object> plugins;
 
 	// These map a plugin name to the list of things in the later lists.
 	private final Map<String,List<String>> pluginCommands;
 //	private final Map<String,List<String>> pluginApiCallSigs;
 //	private final Map<String,List<String>> pluginApiCalls;
 	private final Map<String,List<String>> pluginGenCallSigs;
 	private final Map<String,List<String>> pluginGenCalls;
 	private final Map<String,List<Pattern>> pluginFilters;
 
 	// This gives a method to search for inside the events list.
 	private final Map<String,List<Method>> pluginEvents;
 
 	// Only one interval per plugin.
 	private final Map<String,Method> pluginInterval;
 
 	private final Map<String,Method> commands; // plugin.commandname -> method
 //	private final Map<String,Method> apiCallSigs;
 //	private final Map<String,List<Method>> apiCalls;
 	private final Map<String,Member> genCallSigs; // plugin.prefix:genericname(params) -> method
 	private final Map<String,List<Member>> genCalls; // plugin.prefix:genericname -> list of possible methods
 	private final Map<Pattern,List<Method>> filters; // pattern object -> method to call on match
 	private final Map<String,List<Method>> events; // event name -> method list
 
 	// Create an empty plugin map.
 	ChoobPluginMap()
 	{
 		plugins = new HashMap<String,Object>();
 		pluginCommands = new HashMap<String,List<String>>();
 		//pluginApiCalls = new HashMap<String,List<String>>();
 		//pluginApiCallSigs = new HashMap<String,List<String>>();
 		pluginGenCalls = new HashMap<String,List<String>>();
 		pluginGenCallSigs = new HashMap<String,List<String>>();
 		pluginFilters = new HashMap<String,List<Pattern>>();
 		pluginEvents = new HashMap<String,List<Method>>();
 		pluginInterval = new HashMap<String,Method>();
 		commands = new HashMap<String,Method>();
 		//apiCallSigs = new HashMap<String,Method>();
 		//apiCalls = new HashMap<String,List<Method>>();
 		genCallSigs = new HashMap<String,Member>();
 		genCalls = new HashMap<String,List<Member>>();
 		filters = new HashMap<Pattern,List<Method>>();
 		events = new HashMap<String,List<Method>>();
 	}
 
 	// Wipe out details for plugin <name>, and if pluginObj is not null, add new ones.
 	synchronized void resetPlugin(String pluginName, Object pluginObj)
 	{
 		String lname = pluginName.toLowerCase();
 		if (plugins.get(lname) != null)
 		{
 			// Must clear out old values
 			Iterator<String> it;
 			it = pluginCommands.get(lname).iterator();
 			while (it.hasNext())
 				commands.remove(it.next());
 			/*it = pluginApiCalls.get(lname).iterator();
 			while (it.hasNext())
 				apiCalls.remove(it.next());
 			it = pluginApiCallSigs.get(lname).iterator();
 			while (it.hasNext())
 				apiCallSigs.remove(it.next());*/
 			it = pluginGenCalls.get(lname).iterator();
 			while (it.hasNext())
 				genCalls.remove(it.next());
 			it = pluginGenCallSigs.get(lname).iterator();
 			while (it.hasNext())
 				genCallSigs.remove(it.next());
 			Iterator<Pattern> it3 = pluginFilters.get(lname).iterator();
 			while (it3.hasNext())
 			{
 				Iterator<Method> it2 = filters.get(it3.next()).iterator();
 				while(it2.hasNext())
 				{
 					if (it2.next().getDeclaringClass().getSimpleName().compareToIgnoreCase(pluginName) == 0)
 						it2.remove();
 				}
 			}
 			Iterator<Method> it2 = pluginEvents.get(lname).iterator();
 			while (it2.hasNext())
 			{
 				Method m = it2.next();
 				events.get(m.getName()).remove(m);
 			}
 		}
 
 		if (pluginObj == null)
 		{
 			plugins.remove(lname);
 			pluginCommands.remove(lname);
 			//pluginApiCalls.remove(lname);
 			//pluginApiCallSigs.remove(lname);
 			pluginGenCalls.remove(lname);
 			pluginGenCallSigs.remove(lname);
 			pluginEvents.remove(lname);
 			pluginInterval.remove(lname);
 			return;
 		}
 
 		plugins.put(lname, pluginObj);
 		// OK, now load in new values...
 		List<String> coms = new LinkedList<String>();
 		pluginCommands.put(lname, coms);
 		//List<String> apis = new LinkedList<String>();
 		//pluginApiCalls.put(lname, apis);
 		//List<String> apiss = new LinkedList<String>();
 		//pluginApiCallSigs.put(lname, apiss);
 		List<String> gens = new LinkedList<String>();
 		pluginGenCalls.put(lname, gens);
 		List<String> genss = new LinkedList<String>();
 		pluginGenCallSigs.put(lname, genss);
 		List<Pattern> fils = new LinkedList<Pattern>();
 		pluginFilters.put(lname, fils);
 		List<Method> evs = new LinkedList<Method>();
 		pluginEvents.put(lname, evs);
 
 		Class<?> pluginClass = pluginObj.getClass();
 		Method[] meths = pluginClass.getMethods();
 		for(Method meth: meths)
 		{
 			// We don't want these. :)
 			if (meth.getDeclaringClass() != pluginClass)
 				continue;
 
 			String name = meth.getName();
 			if (name.startsWith("command"))
 			{
 				String commandName = lname + "." + name.substring(7).toLowerCase();
 				// Command
 				if (HaxSunPluginManager.checkCommandSignature(meth))
 				{
 					coms.add(commandName);
 					commands.put(commandName, meth);
 				}
 				else
 				{
 					System.err.println("Command " + commandName + " had invalid signature.");
 				}
 			}
 /*			else if (name.startsWith("api"))
 			{
 				String apiName = lname + "." + name.substring(3).toLowerCase();
 				if (HaxSunPluginManager.checkAPISignature(meth))
 				{
 					apis.add(apiName);
 					if (apiCalls.get(apiName) == null)
 						apiCalls.put(apiName, new LinkedList<Method>());
 					apiCalls.get(apiName).add(meth);
 				}
 				else
 				{
 					System.err.println("API call " + apiName + " had invalid signature.");
 				}
 			} API == generic */
 			else if (name.startsWith("filter"))
 			{
 				String filter;
 				try
 				{
 					filter = (String)pluginClass.getField(name + "Regex").get(pluginObj);
 				}
 				catch (NoSuchFieldException e)
 				{
 					System.err.println("Plugin " + pluginName + " had filter " + name + " with no regex.");
 					continue;
 				}
 				catch (ClassCastException e)
 				{
 					System.err.println("Plugin " + pluginName + " had filter " + name + " with no non-String regex.");
 					continue;
 				}
 				catch (IllegalAccessException e)
 				{
 					System.err.println("Plugin " + pluginName + " had non-public filter " + name + ".");
 					continue;
 				}
 				Pattern pattern;
 				try
 				{
 					pattern = Pattern.compile(filter, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
 				}
 				catch (PatternSyntaxException e)
 				{
 					System.err.println("Plugin " + pluginName + " had invalid filter " + filter + ": " + e);
 					continue;
 				}
 				if (HaxSunPluginManager.checkCommandSignature(meth))
 				{
 					fils.add(pattern);
 					if (filters.get(pattern) == null)
 						filters.put(pattern, new LinkedList<Method>());
 					filters.get(pattern).add(meth);
 				}
 				else
 				{
 					System.err.println("Filter " + lname + "." + name + " had invalid signature.");
 				}
 			}
 			else if (name.startsWith("on"))
 			{
 				if (HaxSunPluginManager.checkEventSignature(meth))
 				{
 					evs.add(meth);
 					if (events.get(name) == null)
 						events.put(name, new LinkedList<Method>());
 					events.get(name).add(meth);
 				}
 				else
 				{
 					System.err.println("Event " + lname + "." + name + " had invalid signature.");
 				}
 			}
 			else if (name.startsWith("interval"))
 			{
 				if (HaxSunPluginManager.checkIntervalSignature(meth))
 				{
 					pluginInterval.put(lname, meth);
 				}
 				else
 				{
 					System.err.println("Interval " + lname + "." + name + " had invalid signature.");
 				}
 			}
 			else
 			{
 				// File it as a generic.
 				Matcher matcher = Pattern.compile("([a-z]+)([A-Z].+)?").matcher(name);
 				if (matcher.matches())
 				{
 					// Is a real generic.
 					String prefix = matcher.group(1);
 					String gName = name.substring(prefix.length()).toLowerCase();
 					String fullName = lname + "." + prefix + ":" + gName;
 					if (HaxSunPluginManager.checkAPISignature(meth))
 					{
 						gens.add(fullName);
 						if (genCalls.get(fullName) == null)
 							genCalls.put(fullName, new LinkedList<Member>());
 						genCalls.get(fullName).add(meth);
 					}
 					else
 					{
 						System.err.println("Generic call " + fullName + " had invalid signature.");
 					}
 				}
 				else
 				{
 					System.err.println("Ignoring method " + name + ".");
 				}
 			}
 		}
 		Field[] fields = pluginClass.getFields();
 		for(Field field: fields)
 		{
 			// We don't want these. :)
 			if (field.getDeclaringClass() != pluginClass)
 				continue;
 
 			String name = field.getName();
 			if (name.startsWith("command"))
 			{
 			}
 /*			else if (name.startsWith("api"))
 			{
 			} API == generic */
 			else if (name.startsWith("filter"))
 			{
 			}
 			else if (name.startsWith("on"))
 			{
 			}
 			else if (name.startsWith("interval"))
 			{
 			}
 			else
 			{
 				// File it as a generic.
 				Matcher matcher = Pattern.compile("([a-z]+)([A-Z].+)?").matcher(name);
 				if (matcher.matches())
 				{
 					// Is a real generic.
 					String prefix = matcher.group(1);
 					String gName = name.substring(prefix.length()).toLowerCase();
 					String fullName = lname + "." + prefix + ":" + gName;
 
 					gens.add(fullName);
 					if (genCalls.get(fullName) == null)
 						genCalls.put(fullName, new LinkedList<Member>());
 					genCalls.get(fullName).add(field);
 				}
 				else
 				{
 					System.err.println("Ignoring field " + name + ".");
 				}
 			}
 		}
 	}
 
 	synchronized Object getPluginObj(String pluginName)
 	{
 		return plugins.get(pluginName.toLowerCase());
 	}
 
 	synchronized Object getPluginObj(Member meth)
 	{
 		return getPluginObj(meth.getDeclaringClass().getSimpleName().toLowerCase());
 	}
 
 	synchronized Method getCommand(String commandName)
 	{
 		return commands.get(commandName.toLowerCase());
 	}
 
 	synchronized List<String> getCommands(String pluginName)
 	{
 		return pluginCommands.get(pluginName.toLowerCase());
 	}
 
 	/*synchronized Method getAPI(String apiName)
 	{
 		return apiCallSigs.get(apiName.toLowerCase());
 	}
 
 	synchronized void setAPI(String apiName, Method meth)
 	{
 		pluginApiCallSigs.get(meth.getDeclaringClass().getSimpleName().toLowerCase()).add(apiName.toLowerCase());
 		apiCallSigs.put(apiName.toLowerCase(), meth);
 	}
 
 	synchronized List<Method> getAllAPI(String apiName)
 	{
 		return apiCalls.get(apiName.toLowerCase());
 	}*/
 
 	synchronized Member getGeneric(String genName)
 	{
 		return genCallSigs.get(genName.toLowerCase());
 	}
 
 	synchronized void setGeneric(String genName, Member obj)
 	{
 		pluginGenCallSigs.get(obj.getDeclaringClass().getSimpleName().toLowerCase()).add(genName.toLowerCase());
 		genCallSigs.put(genName.toLowerCase(), obj);
 	}
 
 	synchronized List<Member> getAllGeneric(String genName)
 	{
 		return genCalls.get(genName.toLowerCase());
 	}
 
 	synchronized List<Method> getFilter(String text)
 	{
 		List<Method> ret = new LinkedList<Method>();
 		Iterator<Pattern> pats = filters.keySet().iterator();
 		while(pats.hasNext())
 		{
 			Pattern pat = pats.next();
 			if (pat.matcher(text).find())
 				ret.addAll(filters.get(pat));
 		}
 		return ret;
 	}
 
 	synchronized List<Method> getEvent(String eventName)
 	{
 		List<Method> handlers = events.get(eventName);
 		if (handlers != null)
 			return new ArrayList<Method>(handlers);
 		return null;
 	}
 
 	synchronized Method getInterval(String pluginName)
 	{
 		return pluginInterval.get(pluginName.toLowerCase());
 	}
 }
