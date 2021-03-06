 /*
  *  Freeplane - mind map editor
  *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
  *
  *  This file is created by Dimitry Polivaev in 2008.
  *
  *  This program is free software: you can redistribute it and/or modify
  *  it under the terms of the GNU General Public License as published by
  *  the Free Software Foundation, either version 2 of the License, or
  *  (at your option) any later version.
  *
  *  This program is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *  GNU General Public License for more details.
  *
  *  You should have received a copy of the GNU General Public License
  *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 package org.freeplane.features.mindmapmode.text;
 
 import java.awt.Component;
 import java.awt.KeyEventDispatcher;
 import java.awt.KeyboardFocusManager;
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.awt.event.InputEvent;
 import java.awt.event.KeyEvent;
 import java.io.File;
 import java.io.IOException;
 import java.io.StringReader;
 import java.io.StringWriter;
 import java.net.URI;
 import java.util.Collection;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 import javax.swing.JEditorPane;
 import javax.swing.JFileChooser;
 import javax.swing.JFrame;
 import javax.swing.JOptionPane;
 import javax.swing.text.BadLocationException;
 import javax.swing.text.html.HTMLDocument;
 import javax.swing.text.html.HTMLEditorKit;
 
 import org.freeplane.core.controller.Controller;
 import org.freeplane.core.controller.INodeSelectionListener;
 import org.freeplane.core.frame.ViewController;
 import org.freeplane.core.resources.ResourceController;
 import org.freeplane.core.ui.IEditHandler.FirstAction;
 import org.freeplane.core.ui.components.BitmapImagePreview;
 import org.freeplane.core.ui.components.OptionalDontShowMeAgainDialog;
 import org.freeplane.core.ui.components.UITools;
 import org.freeplane.core.undo.IActor;
 import org.freeplane.core.util.FixedHTMLWriter;
 import org.freeplane.core.util.HtmlUtils;
 import org.freeplane.core.util.LogUtils;
 import org.freeplane.core.util.TextUtils;
 import org.freeplane.features.common.format.ScannerController;
 import org.freeplane.features.common.link.LinkController;
 import org.freeplane.features.common.link.NodeLinks;
 import org.freeplane.features.common.map.INodeChangeListener;
 import org.freeplane.features.common.map.MapController;
 import org.freeplane.features.common.map.MapModel;
 import org.freeplane.features.common.map.ModeController;
 import org.freeplane.features.common.map.NodeChangeEvent;
 import org.freeplane.features.common.map.NodeModel;
 import org.freeplane.features.common.nodestyle.NodeStyleController;
 import org.freeplane.features.common.text.DetailTextModel;
 import org.freeplane.features.common.text.IContentTransformer;
 import org.freeplane.features.common.text.ShortenedTextModel;
 import org.freeplane.features.common.text.TextController;
 import org.freeplane.features.common.url.UrlManager;
 import org.freeplane.features.mindmapmode.MModeController;
 import org.freeplane.features.mindmapmode.link.MLinkController;
 import org.freeplane.features.mindmapmode.map.MMapController;
 import org.freeplane.features.mindmapmode.nodestyle.MNodeStyleController;
 import org.freeplane.features.mindmapmode.text.IEditBaseCreator.EditedComponent;
 
 import com.lightdev.app.shtm.SHTMLPanel;
 import com.lightdev.app.shtm.TextResources;
 
 
 /**
  * @author Dimitry Polivaev
  */
 public class MTextController extends TextController {
 	
 	public static final String NODE_TEXT = "NodeText";
 	private static Pattern FORMATTING_PATTERN = null;
 	private static final boolean parseData = ResourceController.getResourceController().getBooleanProperty(
 	"parse_data");
 	private EditNodeBase mCurrentEditDialog = null;
 	private final Collection<IEditorPaneListener> editorPaneListeners;
 
 	public static MTextController getController() {
 		return (MTextController) TextController.getController();
 	}
 
 	public MTextController(ModeController modeController) {
 		super(modeController);
 		editorPaneListeners = new LinkedList<IEditorPaneListener>();
 		createActions();
 	}
 
 	private void createActions() {
 		ModeController modeController = Controller.getCurrentModeController();
 		modeController.addAction(new EditAction());
 		modeController.addAction(new UseRichFormattingAction());
 		modeController.addAction(new UsePlainTextAction());
 		modeController.addAction(new JoinNodesAction());
 		modeController.addAction(new EditLongAction());
 		modeController.addAction(new SetImageByFileChooserAction());
         modeController.addAction(new EditDetailsAction(false));
         modeController.addAction(new EditDetailsAction(true));
 		modeController.addAction(new DeleteDetailsAction());
 	}
 
 	private String[] getContent(final String text, final int pos) {
 		if (pos <= 0) {
 			return null;
 		}
 		final String[] strings = new String[2];
 		if (text.startsWith("<html>")) {
 			final HTMLEditorKit kit = new HTMLEditorKit();
 			final HTMLDocument doc = new HTMLDocument();
 			final StringReader buf = new StringReader(text);
 			try {
 				kit.read(buf, doc, 0);
 				final char[] firstText = doc.getText(0, pos).toCharArray();
 				int firstStart = 0;
 				int firstLen = pos;
 				while ((firstStart < firstLen) && (firstText[firstStart] <= ' ')) {
 					firstStart++;
 				}
 				while ((firstStart < firstLen) && (firstText[firstLen - 1] <= ' ')) {
 					firstLen--;
 				}
 				int secondStart = 0;
 				int secondLen = doc.getLength() - pos;
 				final char[] secondText = doc.getText(pos, secondLen).toCharArray();
 				while ((secondStart < secondLen) && (secondText[secondStart] <= ' ')) {
 					secondStart++;
 				}
 				while ((secondStart < secondLen) && (secondText[secondLen - 1] <= ' ')) {
 					secondLen--;
 				}
 				if (firstStart == firstLen || secondStart == secondLen) {
 					return null;
 				}
 				StringWriter out = new StringWriter();
 				new FixedHTMLWriter(out, doc, firstStart, firstLen - firstStart).write();
 				strings[0] = out.toString();
 				out = new StringWriter();
 				new FixedHTMLWriter(out, doc, pos + secondStart, secondLen - secondStart).write();
 				strings[1] = out.toString();
 				return strings;
 			}
 			catch (final IOException e) {
 				LogUtils.severe(e);
 			}
 			catch (final BadLocationException e) {
 				LogUtils.severe(e);
 			}
 		}
 		else {
 			if (pos >= text.length()) {
 				return null;
 			}
 			strings[0] = text.substring(0, pos);
 			strings[1] = text.substring(pos);
 		}
 		return strings;
 	}
 
 	public void joinNodes(final NodeModel selectedNode, final List<NodeModel> selectedNodes) {
 		((JoinNodesAction) Controller.getCurrentModeController().getAction("JoinNodesAction")).joinNodes(selectedNode, selectedNodes);
 	}
 
 	public void setImageByFileChooser() {
 		boolean picturesAmongSelecteds = false;
 		final ModeController modeController = Controller.getCurrentModeController();
 		for (final NodeModel node : modeController.getMapController().getSelectedNodes()) {
 			final URI link = NodeLinks.getLink(node);
 			if (link != null) {
 				final String linkString = link.toString();
 				final String lowerCase = linkString.toLowerCase();
 				if (lowerCase.endsWith(".png") || lowerCase.endsWith(".jpg") || lowerCase.endsWith(".jpeg")
 				        || lowerCase.endsWith(".gif")) {
 					picturesAmongSelecteds = true;
 					final String encodedLinkString = HtmlUtils.unicodeToHTMLUnicodeEntity(linkString);
 					final String strText = "<html><img src=\"" + encodedLinkString + "\">";
 					((MLinkController) LinkController.getController()).setLink(node, (URI) null, false);
 					setNodeText(node, strText);
 				}
 			}
 		}
 		if (picturesAmongSelecteds) {
 			return;
 		}
 		final Controller controller = modeController.getController();
 		final ViewController viewController = controller.getViewController();
 		final NodeModel selectedNode = modeController.getMapController().getSelectedNode();
 		final MapModel map = selectedNode.getMap();
 		final File file = map.getFile();
 		final boolean useRelativeUri = ResourceController.getResourceController().getProperty("links").equals(
 		    "relative");
 		if (file == null && useRelativeUri) {
 			JOptionPane.showMessageDialog(viewController.getContentPane(), TextUtils
 			    .getText("not_saved_for_image_error"), "Freeplane", JOptionPane.WARNING_MESSAGE);
 			return;
 		}
 		final ExampleFileFilter filter = new ExampleFileFilter();
 		filter.addExtension("jpg");
 		filter.addExtension("jpeg");
 		filter.addExtension("png");
 		filter.addExtension("gif");
 		filter.setDescription(TextUtils.getText("bitmaps"));
 		final UrlManager urlManager = (UrlManager) modeController.getExtension(UrlManager.class);
 		final JFileChooser chooser = urlManager.getFileChooser(null, false);
 		chooser.setFileFilter(filter);
 		chooser.setAcceptAllFileFilterUsed(false);
 		chooser.setAccessory(new BitmapImagePreview(chooser));
 		final int returnVal = chooser.showOpenDialog(viewController.getContentPane());
 		if (returnVal != JFileChooser.APPROVE_OPTION) {
 			return;
 		}
 		final File input = chooser.getSelectedFile();
 		URI uri = input.toURI();
 		if (uri == null) {
 			return;
 		}
 		// bad hack: try to interpret file as http link
 		if(! input.exists()){
 			uri = LinkController.toRelativeURI(map.getFile(), input);
 			if(uri == null || ! "http".equals(uri.getScheme())){
 				UITools.errorMessage(TextUtils.format("file_not_found", input.toString()));
 				return;
 			}
 		}
 		else if (useRelativeUri) {
 			uri = LinkController.toRelativeURI(map.getFile(), input);
 		}
 		String uriString = uri.toString();
 		if(uriString.startsWith("http:/")){
 			uriString = "http://" + uriString.substring("http:/".length());
 		}
 		final String strText = "<html><img src=\"" + uriString + "\">";
 		setNodeText(selectedNode, strText);
 	}
 
 	private static final Pattern HTML_HEAD = Pattern.compile("\\s*<head>.*</head>", Pattern.DOTALL);
 
 	public void setGuessedNodeObject(final NodeModel node, final String newText) {
 		if (HtmlUtils.isHtmlNode(newText))
 			setNodeObject(node, newText);
 		else
 			setNodeObject(node, guessObject(newText));
 	}
 
 	/** converts strings to date or number if possible. All other data types are left unchanged. */
 	public Object guessObject(final Object text) {
 		if (parseData && text instanceof String)
			return ScannerController.getController(modeController).parse((String) text);
 		return text;
 	}
 	
 	public void setNodeText(final NodeModel node, final String newText) {
 		setNodeObject(node, newText);
 	}
 
 	public void setNodeObject(final NodeModel node, final Object newObject) {
 		final Object oldText = node.getUserObject();
 		if (oldText.equals(newObject)) {
 			return;
 		}
 		
 		final IActor actor = new IActor() {
 			public void act() {
 				if (!oldText.equals(newObject)) {
 					node.setUserObject(newObject);
 					Controller.getCurrentModeController().getMapController().nodeChanged(node, NodeModel.NODE_TEXT, oldText, newObject);
 				}
 			}
 
 			public String getDescription() {
 				return "setNodeText";
 			}
 
 			public void undo() {
 				if (!oldText.equals(newObject)) {
 					node.setUserObject(oldText);
 					Controller.getCurrentModeController().getMapController().nodeChanged(node, NodeModel.NODE_TEXT, newObject, oldText);
 				}
 			}
 		};
 		Controller.getCurrentModeController().execute(actor, node.getMap());
 	}
 
 	public void splitNode(final NodeModel node, final int caretPosition, final String newText) {
 		if (node.isRoot()) {
 			return;
 		}
 		final String futureText = newText != null ? newText : node.getText();
 		final String[] strings = getContent(futureText, caretPosition);
 		if (strings == null) {
 			return;
 		}
 		final String newUpperContent = strings[0];
 		final String newLowerContent = strings[1];
 		setGuessedNodeObject(node, newUpperContent);
 		final NodeModel parent = node.getParentNode();
 		final ModeController modeController = Controller.getCurrentModeController();
 		final NodeModel lowerNode = ((MMapController) modeController.getMapController()).addNewNode(parent, parent
 		    .getChildPosition(node) + 1, node.isLeft());
 		final MNodeStyleController nodeStyleController = (MNodeStyleController) NodeStyleController
 		    .getController();
 		nodeStyleController.copyStyle(node, lowerNode);
 		setGuessedNodeObject(lowerNode, newLowerContent);
 	}
 
 	public boolean useRichTextInEditor(String key) {
 		final int showResult = OptionalDontShowMeAgainDialog.show(
 			"OptionPanel." + key, "edit.decision", key,
 		    OptionalDontShowMeAgainDialog.BOTH_OK_AND_CANCEL_OPTIONS_ARE_STORED);
 		return showResult == JOptionPane.OK_OPTION;
 	}
 
 	public void editDetails(final NodeModel nodeModel, InputEvent e, final boolean editLong) {
 		final Controller controller = Controller.getCurrentController();
 	    stopEditing();
 		Controller.getCurrentModeController().setBlocked(true);
 		String text = DetailTextModel.getDetailTextText(nodeModel);
 		final boolean isNewNode = text ==  null;
 		if(isNewNode){
 			final MTextController textController = (MTextController) MTextController.getController();
 	        textController.setDetails(nodeModel, "<html>");
 	        text = "";
 		}
 		final EditNodeBase.IEditControl editControl = new EditNodeBase.IEditControl() {
 			public void cancel() {
 				if (isNewNode) {
 					((MModeController) Controller.getCurrentModeController()).undo();
 				}
 				stop();
 			}
 
 			public void ok(final String newText) {
 				setDetailsHtmlText(nodeModel, newText);
 				stop();
 			}
 
 			public void split(final String newText, final int position) {
 			}
 			private void stop() {
 				Controller.getCurrentModeController().setBlocked(false);
 				mCurrentEditDialog = null;
 			}
 		};
 		mCurrentEditDialog = createEditor(nodeModel, IEditBaseCreator.EditedComponent.DETAIL, editControl, text, e, false, editLong, true);
 		final JFrame frame = controller.getViewController().getJFrame();
 		mCurrentEditDialog.show(frame);
     }
 
 
 	private void setDetailsHtmlText(final NodeModel node, final String newText) {
 		final String body = HTML_HEAD.matcher(newText).replaceFirst("");
 		final MTextController textController = (MTextController) MTextController.getController();
         textController.setDetails(node, body.replaceFirst("\\s+$", ""));
 	}
 
 	public void setDetails(final NodeModel node, final String newText) {
 		final String oldText = DetailTextModel.getDetailTextText(node);
 		if (oldText == newText || null != oldText && oldText.equals(newText)) {
 			return;
 		}
 		final IActor actor = new IActor() {
 			public void act() {
 				setText(newText);
 			}
 
 			public String getDescription() {
 				return "setDetailText";
 			}
 
 			private void setText(final String text) {
 				final boolean containsDetails = !(text == null || text.equals(""));
 				if (containsDetails) {
 					final DetailTextModel details = DetailTextModel.createDetailText(node);
 					details.setHtml(text);
 					node.addExtension(details);
 				}
 				else {
 					final DetailTextModel details = (DetailTextModel) node.getExtension(DetailTextModel.class);
 					if (null != details ) {
 						if(details.isHidden()){
 							details.setHtml(null);
 						}
 						else{
 							node.removeExtension(DetailTextModel.class);
 						}
 					}
 				}
 				setDetailsTooltip(node);
 				Controller.getCurrentModeController().getMapController().nodeChanged(node, DetailTextModel.class, oldText, text);
 			}
 
 			public void undo() {
 				setText(oldText);
 			}
 		};
 		Controller.getCurrentModeController().execute(actor, node.getMap());
 	}
 
 	public void setDetailsHidden(final NodeModel node, final boolean isHidden) {
 		stopEditing();
 		DetailTextModel details = (DetailTextModel) node.getExtension(DetailTextModel.class);
 		if (details == null || details.isHidden() == isHidden) {
 			return;
 		}
 		final IActor actor = new IActor() {
 			public void act() {
 				setHidden(isHidden);
 			}
 
 			public String getDescription() {
 				return "setDetailsHidden";
 			}
 
 			private void setHidden(final boolean isHidden) {
 				final DetailTextModel details = DetailTextModel.createDetailText(node);
 				details.setHidden(isHidden);
 				node.addExtension(details);
 				Controller.getCurrentModeController().getMapController().nodeChanged(node, "DETAILS_HIDDEN", ! isHidden, isHidden);
 			}
 
 			public void undo() {
 				setHidden(! isHidden);
 			}
 		};
 		setDetailsTooltip(node);
 		Controller.getCurrentModeController().execute(actor, node.getMap());
 	}
 
 	public void setIsShortened(final NodeModel node, final boolean state) {
 		ShortenedTextModel details = (ShortenedTextModel) node.getExtension(ShortenedTextModel.class);
 		if (details == null && state == false || details != null && state == true) {
 			return;
 		}
 		final IActor actor = new IActor() {
 			public void act() {
 				setShortener(state);
 			}
 
 			public String getDescription() {
 				return "setShortener";
 			}
 
 			private void setShortener(final boolean state) {
 				if(state){
 					final ShortenedTextModel details = ShortenedTextModel.createShortenedTextModel(node);
 					node.addExtension(details);
 					setNodeTextTooltip(node);
 				}
 				else{
 					node.removeExtension(ShortenedTextModel.class);
 				}
 				Controller.getCurrentModeController().getMapController().nodeChanged(node, "SHORTENER", ! state, state);
 			}
 
 			public void undo() {
 				setShortener(! state);
 			}
 		};
 		Controller.getCurrentModeController().execute(actor, node.getMap());
 	}
 
 	public void edit(final InputEvent e, final FirstAction action, final boolean editLong) {
 		final Controller controller = Controller.getCurrentController();
 		final NodeModel selectedNode = controller.getSelection().getSelected();
 		if (selectedNode != null) {
 			if (e == null || FirstAction.EDIT_CURRENT.equals(action)) {
 				edit(selectedNode, selectedNode, e, false, false, editLong);
 			}
 			else if (!Controller.getCurrentModeController().isBlocked()) {
 				final KeyEvent firstKeyEvent; 
 				if(e instanceof KeyEvent)
 					firstKeyEvent = (KeyEvent) e;
 				else
 					firstKeyEvent = null;
 				final int mode = FirstAction.ADD_CHILD.equals(action) ? MMapController.NEW_CHILD : MMapController.NEW_SIBLING_BEHIND;
 				((MMapController) Controller.getCurrentModeController().getMapController()).addNewNode(mode,firstKeyEvent);
 			}
 			if (e != null) {
 				e.consume();
 			}
 		}
 	}
 	
 	public boolean containsFormatting(final String text){
 		if(FORMATTING_PATTERN == null){
 			FORMATTING_PATTERN = Pattern.compile("<(?!/|html>|head|body|p/?>|!--|style type=\"text/css\">)", Pattern.CASE_INSENSITIVE);
 		}
 		final Matcher matcher = FORMATTING_PATTERN.matcher(text);
 		return matcher.find();
 	}
 
 	private class EditEventDispatcher implements KeyEventDispatcher, INodeChangeListener, INodeSelectionListener{
 		private final boolean editLong;
 	    private final boolean parentFolded;
 	    private final boolean isNewNode;
 	    private final NodeModel prevSelectedModel;
 	    private final NodeModel nodeModel;
 		private final ModeController modeController;
 
 	    private EditEventDispatcher(ModeController modeController, NodeModel nodeModel, NodeModel prevSelectedModel, boolean isNewNode,
 	                                boolean parentFolded, boolean editLong) {
 	    	this.modeController = modeController;
 		    this.editLong = editLong;
 		    this.parentFolded = parentFolded;
 		    this.isNewNode = isNewNode;
 		    this.prevSelectedModel = prevSelectedModel;
 		    this.nodeModel = nodeModel;
 	    }
 
 	    public boolean dispatchKeyEvent(KeyEvent e) {
 	    	if(e.getID() == KeyEvent.KEY_RELEASED)
 	    		return false;
 	    	switch(e.getKeyCode()){
 	    		case KeyEvent.VK_SHIFT:
 	    		case KeyEvent.VK_CONTROL:
 	    		case KeyEvent.VK_CAPS_LOCK:
 	    		case KeyEvent.VK_ALT:
 	    		case KeyEvent.VK_ALT_GRAPH:
 	    			return false;
 	    	}
 	    	uninstall();
 	    	edit(nodeModel, prevSelectedModel, e, isNewNode, parentFolded, editLong);
 	    	return true;
 	    }
 
 		public void uninstall() {
 	        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
 			MapController mapController = modeController.getMapController();
 			mapController.removeNodeChangeListener(this);
 			mapController.removeNodeSelectionListener(this);
         }
 
 		public void install() {
 			KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
 			MapController mapController = modeController.getMapController();
 			mapController.addNodeChangeListener(this);
 			mapController.addNodeSelectionListener(this);
         }
 
 		public void onDeselect(NodeModel node) {
 			uninstall();
         }
 
 		public void onSelect(NodeModel node) {
 			uninstall();
         }
 
 		public void nodeChanged(NodeChangeEvent event) {
 			uninstall();
         }
     }
 
 	public void edit(final NodeModel nodeModel, final NodeModel prevSelectedModel, final InputEvent firstEvent,
 	          final boolean isNewNode, final boolean parentFolded, final boolean editLong) {
 		if (nodeModel == null || mCurrentEditDialog != null) {
 			return;
 		}
 		final Controller controller = Controller.getCurrentController();
 		if (controller.getMap() != nodeModel.getMap()) {
 			return;
 		}
 		final ViewController viewController = controller.getViewController();
 		final Component map = viewController.getMapView();
 		map.validate();
 		map.invalidate();
 		final Component node = viewController.getComponent(nodeModel);
 		if (node == null) {
 			return;
 		}
 		node.requestFocus();
 		stopEditing();
 		if(isNewNode && firstEvent == null 
 				&& ! ResourceController.getResourceController().getBooleanProperty("display_inline_editor_for_all_new_nodes")){
 			final EditEventDispatcher dispatcher = new EditEventDispatcher(Controller.getCurrentModeController(), nodeModel, prevSelectedModel, isNewNode, parentFolded, editLong);
 			dispatcher.install();
 			return;
 		};
 		final EditNodeBase.IEditControl editControl = new EditNodeBase.IEditControl() {
 			public void cancel() {
 				if (isNewNode && nodeModel.getMap().equals(controller.getMap())) {
 					controller.getSelection().selectAsTheOnlyOneSelected(nodeModel);
 					((MModeController) Controller.getCurrentModeController()).undo();
 					final MapController mapController = Controller.getCurrentModeController().getMapController();
 					mapController.select(prevSelectedModel);
 					if (parentFolded) {
 						mapController.setFolded(prevSelectedModel, true);
 					}
 				}
 				stop();
 			}
 
 			private void stop() {
 				Controller.getCurrentModeController().setBlocked(false);
 				viewController.obtainFocusForSelected();
 				mCurrentEditDialog = null;
 			}
 
 			public void ok(final String text) {
 				String processedText = text;
 				if(HtmlUtils.isHtmlNode(processedText)){
 					processedText = HTML_HEAD.matcher(processedText).replaceFirst("");
 					if(! containsFormatting(processedText)){
 						processedText = HtmlUtils.htmlToPlain(processedText);
 					}
 				}
 				processedText = processedText.replaceFirst("\\s+$", "");
 				setGuessedNodeObject(nodeModel, processedText);
 				stop();
 			}
 
 			public void split(final String newText, final int position) {
 				splitNode(nodeModel, position, newText);
 				viewController.obtainFocusForSelected();
 				stop();
 			}
 		};
 		mCurrentEditDialog = createEditor(nodeModel, IEditBaseCreator.EditedComponent.TEXT, editControl, nodeModel.getText(), firstEvent, isNewNode, editLong, true);
 		final JFrame frame = controller.getViewController().getJFrame();
 		mCurrentEditDialog.show(frame);
 	}
 
 	private EditNodeBase createEditor(final NodeModel nodeModel, final EditedComponent editedComponent,
                                       final EditNodeBase.IEditControl editControl, String text, final InputEvent firstEvent,
                                       final boolean isNewNode, final boolean editLong, boolean internal) {
 	    Controller.getCurrentModeController().setBlocked(true);
 		final KeyEvent firstKeyEvent; 
 		if(firstEvent instanceof KeyEvent)
 			firstKeyEvent = (KeyEvent) firstEvent;
 		else
 			firstKeyEvent = null;
 		EditNodeBase base = getEditNodeBase(nodeModel, text, editedComponent, editControl, firstKeyEvent, editLong);
 		if(base != null || ! internal){
 			return base;
 		}
 		final IEditBaseCreator textFieldCreator = (IEditBaseCreator) Controller.getCurrentController().getMapViewManager();
 		return textFieldCreator.createEditor(nodeModel, editedComponent, editControl, text, firstEvent, editLong);
     }
 
 
 	public EditNodeBase getEditNodeBase(final NodeModel nodeModel, final String text, EditedComponent editedComponent, final EditNodeBase.IEditControl editControl,
                                 final KeyEvent firstEvent, final boolean editLong) {
 	    final List<IContentTransformer> textTransformers = getTextTransformers();
 		for(IContentTransformer t : textTransformers){
 			if(t instanceof IEditBaseCreator){
 				final EditNodeBase base = ((IEditBaseCreator) t).createEditor(nodeModel, editedComponent, editControl, text, firstEvent, editLong);
 				if(base != null){
 					return base;
 				}
 			}
 		}
 		return null;
     }
 
 
 	public void stopEditing() {
 		if (mCurrentEditDialog != null) {
 			mCurrentEditDialog.closeEdit();
 			mCurrentEditDialog = null;
 		}
 	}
 	public void addEditorPaneListener(IEditorPaneListener l){
 		editorPaneListeners.add(l);
 	}
 	
 	public void removeEditorPaneListener(IEditorPaneListener l){
 		editorPaneListeners.remove(l);
 	}
 	
 	private void fireEditorPaneCreated(JEditorPane editor, Object purpose){
 		for(IEditorPaneListener l :editorPaneListeners){
 			l.editorPaneCreated(editor, purpose);
 		}
 	}
 
 	public SHTMLPanel createSHTMLPanel(String purpose) {
     	SHTMLPanel.setResources(new TextResources() {
     		public String getString(String pKey) {
     			pKey = "simplyhtml." + pKey;
     			String resourceString = ResourceController.getResourceController().getText(pKey, null);
     			if (resourceString == null) {
     				resourceString = ResourceController.getResourceController().getProperty(pKey);
     			}
     			return resourceString;
     		}
     	});
     	final SHTMLPanel shtmlPanel = SHTMLPanel.createSHTMLPanel();
     	shtmlPanel.setOpenHyperlinkHandler(new ActionListener(){
 
 			public void actionPerformed(ActionEvent pE) {
 				try {
 					UrlManager.getController().loadURL(new URI(pE.getActionCommand()));
 				} catch (Exception e) {
 					LogUtils.warn(e);
 				}
 			}});
 
     	final JEditorPane editorPane = shtmlPanel.getEditorPane();
     	fireEditorPaneCreated(editorPane, purpose);
 		return shtmlPanel;
     }
 
 	public JEditorPane createEditorPane(Object purpose) {
      	final JEditorPane editorPane = new JEditorPane();
     	fireEditorPaneCreated(editorPane, purpose);
 		return editorPane;
     }
 
 }
 
