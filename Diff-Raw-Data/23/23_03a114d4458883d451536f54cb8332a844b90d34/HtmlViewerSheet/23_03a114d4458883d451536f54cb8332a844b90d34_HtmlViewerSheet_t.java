 package net.sourceforge.squirrel_sql.client.gui;
 /*
  * Copyright (C) 2002 Colin Bell
  * colbell@users.sourceforge.net
  *
  * This library is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public
  * License as published by the Free Software Foundation; either
  * version 2.1 of the License, or (at your option) any later version.
  *
  * This library is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this library; if not, write to the Free Software
  * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
  */
 import java.awt.BorderLayout;
 import java.awt.Container;
 import java.awt.event.ActionEvent;
 import java.awt.event.MouseAdapter;
 import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
 import java.io.IOException;
 import java.net.URL;
 import java.util.ArrayList;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.ListIterator;
 
 import javax.swing.BorderFactory;
 import javax.swing.JButton;
 import javax.swing.JEditorPane;
 import javax.swing.JPanel;
 import javax.swing.JScrollPane;
 import javax.swing.event.HyperlinkEvent;
 import javax.swing.event.HyperlinkListener;
 import javax.swing.text.html.HTMLDocument;
 import javax.swing.text.html.HTMLFrameHyperlinkEvent;
 
 import net.sourceforge.squirrel_sql.fw.gui.CursorChanger;
 import net.sourceforge.squirrel_sql.fw.gui.GUIUtils;
 import net.sourceforge.squirrel_sql.fw.gui.TextPopupMenu;
 import net.sourceforge.squirrel_sql.fw.gui.ToolBar;
 import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
 import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;
 
 import net.sourceforge.squirrel_sql.client.IApplication;
 import net.sourceforge.squirrel_sql.client.action.SquirrelAction;
 /**
  * This sheet shows the contents of a HTML file.
  *
  * @author  <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
  */
 public class HtmlViewerSheet extends BaseSheet
 {
 	/** Logger for this class. */
 	private final static ILogger s_log =
 		LoggerController.createLogger(HtmlViewerSheet.class);
 
 	/** Application API. */
 	private final IApplication _app;
 
 	/** Toolbar for window. */
 	private ToolBar _toolBar;
 
 	/** URL being displayed. */
 	private URL _documentURL;
 	
 	/** Text area containing the HTML. */
 	private JEditorPane _contentsTxt = new JEditorPane();
 
 	/** History of links. */
 	private List _history = new LinkedList();
 
 	/** Current index into <TT>_history</TT>. */
 	private int _historyIndex = -1;
 
   	public HtmlViewerSheet(IApplication app, String title) throws IOException
 	{
 		this(app, title, null);
 	}
 
   	public HtmlViewerSheet(IApplication app, String title, URL url)
 		throws IOException
 	{
 		super(title, true, true, true, true);
 		if (app == null)
 		{
 			throw new IllegalArgumentException("IApplication == null");
 		}
 		_app = app;
 		createUserInterface();
 		if (url != null)
 		{
 			read(url);
 		}
 	}
 
 	public synchronized void read(URL url) throws IOException
 	{
 		if (url == null)
 		{
 			throw new IllegalArgumentException("URL == null");
 		}
 
 		_documentURL = url;
 
 		CursorChanger cursorChg = new CursorChanger(this);
 		cursorChg.show();
 		try
 		{
// Causes NPE in JDK 1.3.1
//			_contentsTxt.setText("");
 			try
 			{
 				_contentsTxt.setPage(url);
 				_history.add(url);
 				_historyIndex = 0;
 			}
 			catch (IOException ex)
 			{
				final String msg = "Error occured reading from URL";
 				s_log.error(msg, ex);
 				throw(ex);
 			}
		} finally
		{
 			cursorChg.restore();
 		}
 	}
 
 	/**
 	 * Return the URL being displayed.
 	 * 
 	 * @return	URL being displayed.
 	 */
 	public URL getURL()
 	{
 		return _documentURL;
 	}
 
 	private void goBack()
 	{
 		if (_historyIndex > 0 && _historyIndex < _history.size())
 		{
 			try
 			{
 				_contentsTxt.setPage((URL)_history.get(--_historyIndex));
 			}
 			catch (IOException ex)
 			{
 				s_log.error(ex);
 			}
 		}
 	}
 
 	private void goForward()
 	{
 		if (_historyIndex > -1 && _historyIndex < _history.size() - 1)
 		{
 			try
 			{
 				_contentsTxt.setPage((URL)_history.get(++_historyIndex));
 			}
 			catch (IOException ex)
 			{
 				s_log.error(ex);
 			}
 		}
 	}
 
 	/**
 	 * Create user interface.
 	 */
 	private void createUserInterface()
 	{
 		GUIUtils.makeToolWindow(this, true);
 		Container contentPane = getContentPane();
 		contentPane.setLayout(new BorderLayout());
 		contentPane.add(createMainPanel(), BorderLayout.CENTER);
 		contentPane.add(createToolBar(), BorderLayout.NORTH);
 		pack();
 	}
 
 	private ToolBar createToolBar()
 	{
 		_toolBar = new ToolBar();
 		_toolBar.setBorder(BorderFactory.createEtchedBorder());
 		_toolBar.setUseRolloverButtons(true);
 		_toolBar.setFloatable(false);
 		_toolBar.add(new BackAction(_app));
 		_toolBar.add(new ForwardAction(_app));
 		JButton btn = _toolBar.add(new CloseAction(_app));
 		btn.setAlignmentX(btn.RIGHT_ALIGNMENT);
 		return _toolBar;
 	}
 
 	/**
 	 * Create the main panel.
 	 */
 	private JPanel createMainPanel()
 	{
 		_contentsTxt.setEditable(false);
 		_contentsTxt.setContentType("text/html");
 		final TextPopupMenu pop = new TextPopupMenu();
 		pop.setTextComponent(_contentsTxt);
 		_contentsTxt.addMouseListener(new MouseAdapter() {
 			public void mousePressed(MouseEvent evt) {
 				if (evt.isPopupTrigger()) {
 					pop.show(evt.getComponent(), evt.getX(), evt.getY());
 				}
 			}
 			public void mouseReleased(MouseEvent evt) {
 				if (evt.isPopupTrigger()) {
 					pop.show(evt.getComponent(), evt.getX(), evt.getY());
 				}
 			}
 		});
 
 		final JPanel pnl = new JPanel(new BorderLayout());
 		_contentsTxt.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 0));
 		_contentsTxt.addHyperlinkListener(createHyperLinkListener());
 		pnl.add(new JScrollPane(_contentsTxt), BorderLayout.CENTER);
 
 		return pnl;
 	}
 
 	private HyperlinkListener createHyperLinkListener()
 	{
 		return new HyperlinkListener()
 		{
 			public void hyperlinkUpdate(HyperlinkEvent e)
 			{
 				if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
 				{
 					if (e instanceof HTMLFrameHyperlinkEvent)
 					{
 						((HTMLDocument)_contentsTxt.getDocument()).processHTMLFrameHyperlinkEvent((HTMLFrameHyperlinkEvent)e);
 					}
 					else
 					{
 						try
 						{
 							final URL url = e.getURL();
 
 							ListIterator it = _history.listIterator(_historyIndex + 1);
 							while (it.hasNext())
 							{
 								it.next();
 								it.remove();
 							}
 							_history.add(url);
 							_historyIndex = _history.size() - 1;
 							_contentsTxt.setPage(url);
 						}
 						catch (IOException ioe)
 						{
 							s_log.error(ioe);
 						}
 					}
 				}
 			}
 		};
 	}
 
 	private final class CloseAction extends SquirrelAction
 	{
 		public CloseAction(IApplication app)
 		{
 			super(app);
 			if (app == null)
 			{
 				throw new IllegalArgumentException("Null IApplication passed");
 			}
 		}
 
 		public void actionPerformed(ActionEvent evt)
 		{
 			dispose();
 		}
 	}
 
 	private final class BackAction extends SquirrelAction
 	{
 		public BackAction(IApplication app)
 		{
 			super(app);
 			if (app == null)
 			{
 				throw new IllegalArgumentException("Null IApplication passed");
 			}
 		}
 
 		public void actionPerformed(ActionEvent evt)
 		{
 			goBack();
 		}
 	}
 
 	private final class ForwardAction extends SquirrelAction
 	{
 		public ForwardAction(IApplication app)
 		{
 			super(app);
 			if (app == null)
 			{
 				throw new IllegalArgumentException("Null IApplication passed");
 			}
 		}
 
 		public void actionPerformed(ActionEvent evt)
 		{
 			goForward();
 		}
 	}
 
 }
 	
