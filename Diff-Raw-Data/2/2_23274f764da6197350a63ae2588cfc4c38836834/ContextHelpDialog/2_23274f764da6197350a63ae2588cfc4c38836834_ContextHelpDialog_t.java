 package org.eclipse.help.internal.ui;
 /*
  * (c) Copyright IBM Corp. 2000, 2001.
  * All Rights Reserved.
  */
 import java.util.*;
 import org.eclipse.help.*;
 import org.eclipse.help.internal.ui.util.*;
 import org.eclipse.help.internal.util.Logger;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.custom.StyledText;
 import org.eclipse.swt.events.*;
 import org.eclipse.swt.graphics.*;
 import org.eclipse.swt.layout.*;
 import org.eclipse.swt.widgets.*;
 import org.eclipse.ui.help.WorkbenchHelp;
 /**
  * ContextHelpDialog
  */
 public class ContextHelpDialog {
 	private Color backgroundColour = null;
 	private IContext context;
 	private Color foregroundColour = null;
 	private Color linkColour = null;
 	private static HyperlinkHandler linkManager = new HyperlinkHandler();
 	private Map menuItems;
 	private Shell shell;
 	private int x;
 	private int y;
 	/**
 	 * Constructor:
 	 * @param context an array of String or an array of IContext
 	 * @param x the x mouse location in the current display
 	 * @param y the y mouse location in the current display
 	 */
 	ContextHelpDialog(IContext context, int x, int y) {
 		this.context = context;
 		this.x = x;
 		this.y = y;
 		Display display = Display.getCurrent();
 		backgroundColour = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
 		foregroundColour = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);
 		linkColour = display.getSystemColor(SWT.COLOR_BLUE);
 		shell = new Shell(display.getActiveShell(), SWT.NONE);
 		if (Logger.DEBUG)
 			Logger.logDebugMessage(
 				"ContextHelpDialog",
 				" Constructor: Shell is:" + shell.toString());
 		WorkbenchHelp.setHelp(shell, new String[] { IHelpUIConstants.F1_SHELL });
 		shell.addListener(SWT.Deactivate, new Listener() {
 			public void handleEvent(Event e) {
 				if (Logger.DEBUG)
 					Logger.logDebugMessage(
 						"ContextHelpDialog",
 						"handleEvent: SWT.Deactivate called. ");
 				close();
 			};
 		});
 		shell.addControlListener(new ControlAdapter() {
 			public void controlMoved(ControlEvent e) {
 				if (Logger.DEBUG)
 					Logger.logDebugMessage("ContextHelpDialog", "controlMoved: called. ");
 				Rectangle clientArea = shell.getClientArea();
 				shell.redraw(
 					clientArea.x,
 					clientArea.y,
 					clientArea.width,
 					clientArea.height,
 					true);
 				shell.update();
 			}
 		});
 		if (Logger.DEBUG)
 			Logger.logDebugMessage(
 				"ContextHelpDialog",
 				"Constructor: Focus owner is: "
 					+ Display.getCurrent().getFocusControl().toString());
 		linkManager.setHyperlinkUnderlineMode(HyperlinkHandler.UNDERLINE_ROLLOVER);
 		createContents(shell);
 		shell.pack();
 		// Correct x and y of the shell if it not contained within the screen
 		int width = shell.getBounds().width;
 		int height = shell.getBounds().height;
 		// check lower boundaries
 		x = x >= 0 ? x : 0;
 		y = y >= 0 ? y : 0;
 		// check upper boundaries
 		int margin = 0;
 		if (System.getProperty("os.name").startsWith("Win"))
 			margin = 28; // for the Windows task bar in the ussual place;
 		Rectangle screen = display.getBounds();
 		x = x + width <= screen.width ? x : screen.width - width;
 		y = y + height <= screen.height - margin ? y : screen.height - margin - height;
 		shell.setLocation(x, y);
 	}
 	public synchronized void close() {
 		try {
 			if (Logger.DEBUG)
 				Logger.logDebugMessage("ContextHelpDialog", "close: called. ");
 			if (shell != null) {
 				shell.close();
 				if (!shell.isDisposed())
 					shell.dispose();
 				shell = null;
 			}
 		} catch (Throwable ex) {
 		}
 	}
 	/**
 	 */
 	protected Control createContents(Composite contents) {
 		contents.setBackground(backgroundColour);
 		GridLayout layout = new GridLayout();
 		layout.marginHeight = 5;
 		layout.marginWidth = 5;
 		contents.setLayout(layout);
 		contents.setLayoutData(new GridData(GridData.FILL_BOTH));
 		// create the dialog area and button bar
 		createInfoArea(contents);
 		createLinksArea(contents);
 		// if any errors or parsing errors have occurred, display them in a pop-up
 		ErrorUtil.displayStatus();
 		return contents;
 	}
 	private Control createInfoArea(Composite parent) {
 		// Create the text field.    
 		String styledText = context.getText();
 		if (styledText == null) // no description found in context objects.
 			styledText = WorkbenchResources.getString("WW002");
		StyledText text = new StyledText(parent, SWT.MULTI | SWT.READ_ONLY /* | SWT.WRAP*/);
 		text.getCaret().setVisible(false);
 		text.setBackground(backgroundColour);
 		text.setForeground(foregroundColour);
 		StyledLineWrapper content = new StyledLineWrapper(styledText);
 		text.setContent(content);
 		text.setStyleRanges(content.getStyles());
 		return text;
 	}
 	private Control createLink(Composite parent, IHelpResource topic) {
 		Label image = new Label(parent, SWT.NONE);
 		image.setImage(ElementLabelProvider.getDefault().getImage(topic));
 		image.setBackground(backgroundColour);
 		GridData data = new GridData();
 		data.horizontalAlignment = data.HORIZONTAL_ALIGN_BEGINNING;
 		data.verticalAlignment = data.VERTICAL_ALIGN_BEGINNING;
 		//data.horizontalIndent = 4;
 		image.setLayoutData(data);
 		Label link = new Label(parent, SWT.NONE);
 		link.setText(topic.getLabel());
 		link.setBackground(backgroundColour);
 		link.setForeground(linkColour);
 		data = new GridData();
 		data.horizontalAlignment = data.HORIZONTAL_ALIGN_BEGINNING;
 		data.verticalAlignment = data.VERTICAL_ALIGN_BEGINNING;
 		link.setLayoutData(data);
 		linkManager.registerHyperlink(link, new LinkListener(topic));
 		return link;
 	}
 	private Control createLinksArea(Composite parent) {
 		IHelpResource[] relatedTopics = context.getRelatedTopics();
 		relatedTopics = removeDuplicates(relatedTopics);
 		if (relatedTopics == null)
 			return null;
 		// Create control
 		Composite composite = new Composite(parent, SWT.NONE);
 		composite.setBackground(backgroundColour);
 		GridLayout layout = new GridLayout();
 		layout.marginHeight = 2;
 		layout.marginWidth = 0;
 		layout.verticalSpacing = 3;
 		layout.horizontalSpacing = 2;
 		layout.numColumns = 2;
 		composite.setLayout(layout);
 		GridData data =
 			new GridData(
 				GridData.FILL_BOTH
 					| GridData.HORIZONTAL_ALIGN_BEGINNING
 					| GridData.VERTICAL_ALIGN_CENTER);
 		composite.setLayoutData(data);
 		// Create separator.    
 		Label label = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
 		label.setBackground(backgroundColour);
 		label.setForeground(foregroundColour);
 		data =
 			new GridData(
 				GridData.HORIZONTAL_ALIGN_BEGINNING
 					| GridData.VERTICAL_ALIGN_BEGINNING
 					| GridData.FILL_HORIZONTAL);
 		data.horizontalSpan = 2;
 		label.setLayoutData(data);
 		// Create related links
 		for (int i = 0; i < relatedTopics.length; i++) {
 			createLink(composite, relatedTopics[i]);
 		}
 		return composite;
 	}
 	/**
 	 * Check if two context topic are the same.
 	 * They are considered the same if both labels and href are equal
 	 */
 	private boolean equal(IHelpResource topic1, IHelpResource topic2) {
 		return topic1.getHref().equals(topic2.getHref())
 			&& topic1.getLabel().equals(topic2.getLabel());
 	}
 	/**
 	 * Checks if topic labels and href are not null and not empty strings
 	 */
 	private boolean isValidTopic(IHelpResource topic) {
 		return topic != null
 			&& topic.getHref() != null
 			&& !"".equals(topic.getHref())
 			&& topic.getLabel() != null
 			&& !"".equals(topic.getLabel());
 	}
 	/**
 	 * Called when related link has been chosen
 	 * Opens view with list of all related topics
 	 */
 	private void launchFullViewHelp(IHelpResource selectedTopic) {
 		close();
 		if (Logger.DEBUG)
 			Logger.logDebugMessage("ContextHelpDialog", "launchFullViewHelp: closes shell");
 		// launch help view
 		DefaultHelp.getInstance().displayHelp(context, selectedTopic);
 	}
 	public synchronized void open() {
 		try {
 			shell.open();
 			if (Logger.DEBUG)
 				Logger.logDebugMessage(
 					"ContextHelpDialog",
 					"open: Focus owner after open is: "
 						+ Display.getCurrent().getFocusControl().toString());
 		} catch (Throwable e) {
 		}
 	}
 	/**
 	 * Filters out the duplicate topics from an array
 	 * TODO move this check to ContextManager
 	 */
 	private IHelpResource[] removeDuplicates(IHelpResource links[]) {
 		if (links == null || links.length <= 0)
 			return links;
 		ArrayList filtered = new ArrayList();
 		for (int i = 0; i < links.length; i++) {
 			IHelpResource topic1 = links[i];
 			if (!isValidTopic(topic1))
 				continue;
 			boolean dup = false;
 			for (int j = 0; j < filtered.size(); j++) {
 				IHelpResource topic2 = (IHelpResource) filtered.get(j);
 				if (!isValidTopic(topic2))
 					continue;
 				if (equal(topic1, topic2)) {
 					dup = true;
 					break;
 				}
 			}
 			if (!dup)
 				filtered.add(links[i]);
 		}
 		return (IHelpResource[]) filtered.toArray(new IHelpResource[filtered.size()]);
 	}
 	class LinkListener extends HyperlinkAdapter {
 		IHelpResource topic;
 		public LinkListener(IHelpResource topic) {
 			this.topic = topic;
 		}
 		public void linkActivated(Control c) {
 			launchFullViewHelp(topic);
 		}
 	}
 }
