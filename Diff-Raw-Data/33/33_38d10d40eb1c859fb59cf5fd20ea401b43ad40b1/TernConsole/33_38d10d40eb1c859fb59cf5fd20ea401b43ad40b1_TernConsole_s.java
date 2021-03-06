 package tern.eclipse.ide.internal.ui.console;
 
 import org.eclipse.jface.resource.ImageDescriptor;
 import org.eclipse.swt.graphics.Color;
 import org.eclipse.swt.widgets.Display;
 import org.eclipse.ui.console.ConsolePlugin;
 import org.eclipse.ui.console.IConsole;
 import org.eclipse.ui.console.IConsoleManager;
 import org.eclipse.ui.console.MessageConsole;
 import org.eclipse.ui.console.MessageConsoleStream;
 
 import tern.eclipse.ide.ui.ImageResource;
 import tern.eclipse.ide.ui.console.ITernConsole;
 import tern.eclipse.ide.ui.console.LineType;
 
 public class TernConsole extends MessageConsole implements ITernConsole {
 
 	private boolean showOnMessage;
 
 	private IConsoleManager consoleManager;
 
 	private final ConsoleDocument document;
 
 	private boolean visible = false;
 
 	private MessageConsoleStream streams;
 
 	private boolean initialized;
 
 	public TernConsole() {
 		this("Tern", ImageResource.getImageDescriptor("icons/logo16x16.gif")); //$NON-NLS-1$
 	}
 
 	public TernConsole(String name, ImageDescriptor imageDescriptor) {
 		super(name, imageDescriptor);
 		consoleManager = ConsolePlugin.getDefault().getConsoleManager();
 		document = new ConsoleDocument();
 	}
 
 	protected void init() {
 		// Called when console is added to the console view
 		super.init();
 
 		// Ensure that initialization occurs in the ui thread
 		Display.getDefault().asyncExec(new Runnable() {
 			public void run() {
 				initializeStreams();
 				dump();
 			}
 		});
 	}
 
 	private void initializeStreams() {
 		synchronized (document) {
 			if (!initialized) {
 				// for (int i = 0; i < streams.length; i++) {
 				// streams[i] = newMessageStream();
 				// }
 				streams = newMessageStream();
 
 				// install colors
 				Color color;
 
 				/*
 				 * color = createColor(Display.getDefault(),
 				 * PREF_CONSOLE_DEBUG_COLOR);
 				 * streams[Message.MSG_DEBUG].setColor(color); color =
 				 * createColor(Display.getDefault(),
 				 * PREF_CONSOLE_VERBOSE_COLOR);
 				 * streams[Message.MSG_VERBOSE].setColor(color); color =
 				 * createColor(Display.getDefault(), PREF_CONSOLE_INFO_COLOR);
 				 * streams[Message.MSG_INFO].setColor(color); color =
 				 * createColor(Display.getDefault(), PREF_CONSOLE_WARN_COLOR);
 				 * streams[Message.MSG_WARN].setColor(color); color =
 				 * createColor(Display.getDefault(), PREF_CONSOLE_ERROR_COLOR);
 				 * streams[Message.MSG_ERR].setColor(color);
 				 */
 				initialized = true;
 			}
 		}
 	}
 
 	private void dump() {
 		synchronized (document) {
 			visible = true;
 			ConsoleDocument.ConsoleLine[] lines = document.getLines();
 			for (int i = 0; i < lines.length; i++) {
 				ConsoleDocument.ConsoleLine line = lines[i];
 				doAppendLine(line.getType(), line.getLine());
 			}
 			document.clear();
 		}
 	}
 
 	@Override
	public void doAppendLine(LineType lineType, String line) {
 		showConsole();
 		synchronized (document) {
 			if (visible) {
 				streams.println(line);
 			} else {
 				document.appendConsoleLine(lineType, line);
 			}
 		}
 	}
 
 	private void showConsole() {
 		show(false);
 	}
 
 	@Override
 	protected void dispose() {
 		// Here we can't call super.dispose() because we actually want the
 		// partitioner to remain
 		// connected, but we won't show lines until the console is added to the
 		// console manager
 		// again.
 
 		// Called when console is removed from the console view
 		synchronized (document) {
 			visible = false;
 		}
 	}
 
 	/**
 	 * Show the console.
 	 * 
 	 * @param showNoMatterWhat
 	 *            ignore preferences if <code>true</code>
 	 */
 	public void show(boolean showNoMatterWhat) {
 		//showOnMessage = true;
 		if (showNoMatterWhat || showOnMessage) {
 			if (!visible) {
 				TernConsoleFactory.showConsole();
 			} else {
 				consoleManager.showConsoleView(this);
 			}
 		}
 
 	}
 
 	/**
 	 * Used to notify this console of lifecycle methods <code>init()</code> and
 	 * <code>dispose()</code>.
 	 */
 	public class MyLifecycle implements org.eclipse.ui.console.IConsoleListener {
 		public void consolesAdded(IConsole[] consoles) {
 			for (int i = 0; i < consoles.length; i++) {
 				IConsole console = consoles[i];
 				if (console == TernConsole.this) {
 					init();
 				}
 			}
 		}
 
 		public void consolesRemoved(IConsole[] consoles) {
 			for (int i = 0; i < consoles.length; i++) {
 				IConsole console = consoles[i];
 				if (console == TernConsole.this) {
 					ConsolePlugin.getDefault().getConsoleManager()
 							.removeConsoleListener(this);
 					dispose();
 				}
 			}
 		}
 	}
 
 }
