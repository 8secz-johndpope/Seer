 /*
  * Copyright (C) 2007 IsmAvatar <cmagicj@nni.com>
  * Copyright (C) 2007 Clam <ebordin@aapt.net.au>
  * Copyright (C) 2007 Quadduc <quadduc@gmail.com>
  * 
  * This file is part of Lateral GM.
  * Lateral GM is free software and comes with ABSOLUTELY NO WARRANTY.
  * See LICENSE for details.
  */
 
 package org.lateralgm.subframes;
 
 import static org.lateralgm.main.Util.deRef;
 
 import java.awt.BorderLayout;
 import java.awt.Color;
 import java.awt.Component;
 import java.awt.Dimension;
 import java.awt.FlowLayout;
 import java.awt.GridLayout;
 import java.awt.Point;
 import java.awt.datatransfer.DataFlavor;
 import java.awt.datatransfer.Transferable;
 import java.awt.datatransfer.UnsupportedFlavorException;
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.awt.event.KeyAdapter;
 import java.awt.event.KeyEvent;
 import java.awt.event.MouseAdapter;
 import java.awt.event.MouseEvent;
 import java.beans.PropertyVetoException;
 import java.io.IOException;
 import java.lang.ref.WeakReference;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.EmptyStackException;
 import java.util.Enumeration;
 import java.util.Hashtable;
 import java.util.List;
 import java.util.Stack;
 
 import javax.swing.AbstractListModel;
 import javax.swing.BorderFactory;
 import javax.swing.BoxLayout;
 import javax.swing.DropMode;
 import javax.swing.ImageIcon;
 import javax.swing.JButton;
 import javax.swing.JCheckBox;
 import javax.swing.JComboBox;
 import javax.swing.JComponent;
 import javax.swing.JLabel;
 import javax.swing.JList;
 import javax.swing.JOptionPane;
 import javax.swing.JPanel;
 import javax.swing.JScrollPane;
 import javax.swing.JTabbedPane;
 import javax.swing.JTree;
 import javax.swing.ListCellRenderer;
 import javax.swing.ListModel;
 import javax.swing.TransferHandler;
 import javax.swing.border.Border;
 import javax.swing.border.EmptyBorder;
 import javax.swing.event.TreeSelectionEvent;
 import javax.swing.event.TreeSelectionListener;
 import javax.swing.tree.DefaultMutableTreeNode;
 import javax.swing.tree.DefaultTreeCellRenderer;
 import javax.swing.tree.TreeNode;
 import javax.swing.tree.TreePath;
 import javax.swing.tree.TreeSelectionModel;
 
 import org.lateralgm.compare.ResourceComparator;
 import org.lateralgm.components.GMLTextArea;
 import org.lateralgm.components.GmTreeGraphics;
 import org.lateralgm.components.IntegerField;
 import org.lateralgm.components.ResourceMenu;
 import org.lateralgm.components.impl.EventNode;
 import org.lateralgm.components.impl.ResNode;
 import org.lateralgm.components.mdi.MDIFrame;
 import org.lateralgm.components.visual.VTextIcon;
 import org.lateralgm.main.LGM;
 import org.lateralgm.main.Listener;
 import org.lateralgm.main.Util;
 import org.lateralgm.messages.Messages;
 import org.lateralgm.resources.GmObject;
 import org.lateralgm.resources.Resource;
 import org.lateralgm.resources.Sprite;
 import org.lateralgm.resources.library.LibAction;
 import org.lateralgm.resources.library.LibManager;
 import org.lateralgm.resources.library.Library;
 import org.lateralgm.resources.sub.Action;
 import org.lateralgm.resources.sub.ActionContainer;
 import org.lateralgm.resources.sub.Argument;
 import org.lateralgm.resources.sub.Event;
 import org.lateralgm.resources.sub.MainEvent;
 
 public class GmObjectFrame extends ResourceFrame<GmObject> implements ActionListener,
 		TreeSelectionListener
 	{
 	private static final long serialVersionUID = 1L;
 	private static final ImageIcon INFO_ICON = LGM.getIconForKey("GmObjectFrame.INFO"); //$NON-NLS-1$
 
 	//TODO: update this when sprite changes, not just when ResourceMenu selection changes
 	public JLabel preview;
 	public ResourceMenu<Sprite> sprite;
 	public JComboBox sp2;
 	public JButton newSprite;
 	public JButton editSprite;
 	public JCheckBox visible;
 	public JCheckBox solid;
 	public IntegerField depth;
 	public JCheckBox persistent;
 	public ResourceMenu<GmObject> parent;
 	public ResourceMenu<Sprite> mask;
 	public JButton information;
 
 	public EventTree events;
 	public JButton deleteEvent;
 	public EventGroupNode rootEvent;
 	public ActionList actions;
 	public GMLTextArea code;
 
 	private DefaultMutableTreeNode lastValidEventSelection;
 
 	public GmObjectFrame(GmObject res, ResNode node)
 		{
 		super(res,node);
 
 		setSize(560,400);
 		setMinimumSize(new Dimension(560,400));
 		setLayout(new BoxLayout(getContentPane(),BoxLayout.X_AXIS));
 
 		JPanel side1 = new JPanel(new FlowLayout());
 		side1.setPreferredSize(new Dimension(180,280));
 
 		JLabel lab = new JLabel(Messages.getString("GmObjectFrame.NAME")); //$NON-NLS-1$
 		lab.setPreferredSize(new Dimension(50,14));
 		side1.add(lab);
 		name.setPreferredSize(new Dimension(110,20));
 		side1.add(name);
 
 		JPanel origin = Util.makeTitledPanel(Messages.getString("GmObjectFrame.SPRITE"),180,80); //$NON-NLS-1$
 		preview = new JLabel(GmTreeGraphics.getSpriteIcon(res.sprite == null ? null : res.sprite));
 		preview.setPreferredSize(new Dimension(16,16));
 		origin.add(preview);
 
 		String t = Messages.getString("GmObjectFrame.NO_SPRITE"); //$NON-NLS-1$
 		sprite = new ResourceMenu<Sprite>(Resource.SPRITE,t,144);
 		sprite.setSelected(res.sprite);
 		sprite.addActionListener(this);
 		origin.add(sprite);
 		newSprite = new JButton(Messages.getString("GmObjectFrame.NEW")); //$NON-NLS-1$
 		newSprite.setPreferredSize(new Dimension(80,20));
 		newSprite.addActionListener(this);
 		origin.add(newSprite);
 		editSprite = new JButton(Messages.getString("GmObjectFrame.EDIT")); //$NON-NLS-1$
 		editSprite.setPreferredSize(new Dimension(80,20));
 		editSprite.addActionListener(this);
 		origin.add(editSprite);
 		side1.add(origin);
 
 		visible = new JCheckBox(Messages.getString("GmObjectFrame.VISIBLE"),res.visible); //$NON-NLS-1$
 		visible.setPreferredSize(new Dimension(80,20));
 		side1.add(visible);
 		solid = new JCheckBox(Messages.getString("GmObjectFrame.SOLID"),res.solid); //$NON-NLS-1$
 		solid.setPreferredSize(new Dimension(80,20));
 		side1.add(solid);
 
 		lab = new JLabel(Messages.getString("GmObjectFrame.DEPTH")); //$NON-NLS-1$
 		lab.setPreferredSize(new Dimension(50,14));
 		side1.add(lab);
 		depth = new IntegerField(Integer.MIN_VALUE,Integer.MAX_VALUE,res.depth);
 		depth.setPreferredSize(new Dimension(110,20));
 		side1.add(depth);
 
 		addGap(side1,30,1);
 		persistent = new JCheckBox(Messages.getString("GmObjectFrame.PERSISTENT")); //$NON-NLS-1$
 		persistent.setSelected(res.persistent);
 		persistent.setPreferredSize(new Dimension(100,20));
 		side1.add(persistent);
 		addGap(side1,30,1);
 
 		lab = new JLabel(Messages.getString("GmObjectFrame.PARENT")); //$NON-NLS-1$
 		lab.setPreferredSize(new Dimension(50,14));
 		side1.add(lab);
 		t = Messages.getString("GmObjectFrame.NO_PARENT"); //$NON-NLS-1$
 		parent = new ResourceMenu<GmObject>(Resource.GMOBJECT,t,110);
 		parent.setSelected(res.parent);
 		parent.addActionListener(this);
 		side1.add(parent);
 
 		lab = new JLabel(Messages.getString("GmObjectFrame.MASK")); //$NON-NLS-1$
 		lab.setPreferredSize(new Dimension(50,14));
 		side1.add(lab);
 		t = Messages.getString("GmObjectFrame.SAME_AS_SPRITE"); //$NON-NLS-1$
 		mask = new ResourceMenu<Sprite>(Resource.SPRITE,t,110);
 		mask.setSelected(res.mask);
 		side1.add(mask);
 
 		addGap(side1,160,4);
 
 		information = new JButton(Messages.getString("GmObjectFrame.INFO"),INFO_ICON); //$NON-NLS-1$
 		information.setPreferredSize(new Dimension(160,20));
 		information.addActionListener(this);
 		side1.add(information);
 
 		addGap(side1,160,16);
 
 		save.setPreferredSize(new Dimension(130,24));
 		save.setText(Messages.getString("GmObjectFrame.SAVE")); //$NON-NLS-1$
 		side1.add(save);
 
 		JPanel side2 = new JPanel(new BorderLayout());
 		side2.setMaximumSize(new Dimension(90,Integer.MAX_VALUE));
 		lab = new JLabel(Messages.getString("GmObjectFrame.EVENTS")); //$NON-NLS-1$
 		side2.add(lab,"North"); //$NON-NLS-1$
 		makeEventTree(res);
 		JScrollPane scroll = new JScrollPane(events);
 		scroll.setPreferredSize(new Dimension(140,260));
 		side2.add(scroll,"Center"); //$NON-NLS-1$
 		deleteEvent = new JButton(Messages.getString("GmObjectFrame.DELETE")); //$NON-NLS-1$
 		deleteEvent.addActionListener(this);
 		side2.add(deleteEvent,"South"); //$NON-NLS-1$
 
 		add(side1);
 		add(side2);
 
 		if (false)
 			{
 			code = new GMLTextArea(""); //$NON-NLS-1$
 			JScrollPane codePane = new JScrollPane(code);
 			add(codePane);
 			}
 		else
 			{
 			actions = addActionPane(this);
 			}
 
 		// Select first event
 		TreeNode event = (TreeNode) events.getModel().getRoot();
 		while (event.getChildCount() > 0)
 			event = event.getChildAt(0);
 		if (event != events.getModel().getRoot())
 			events.setSelectionPath(new TreePath(((DefaultMutableTreeNode) event).getPath()));
 		}
 
 	public static class EventTree extends JTree
 		{
 		private static final long serialVersionUID = 1L;
 
 		public EventTree(TreeNode n)
 			{
 			super(n);
 			//otherwise, getToolTipText won't be called
 			setToolTipText(""); //$NON-NLS-1$
 			}
 
 		public String getToolTipText(MouseEvent e)
 			{
 			Point p = e.getPoint();
 			TreePath path = getPathForLocation(p.x,p.y);
 			if (path == null) return null;
 			Object c = path.getLastPathComponent();
 			if (c instanceof EventInstanceNode)
 				{
 				EventInstanceNode node = (EventInstanceNode) c;
 				Event ev = node.getUserObject();
 				return String.format(Messages.getString("MainEvent.EVENT_HINT" + ev.mainId),ev.toString()); //$NON-NLS-1$
 				}
 			else
 				return String.format(Messages.getString("MainEvent.EVENTS"),c.toString()); //$NON-NLS-1$
 			}
 		}
 
 	private class EventNodeTransferHandler extends TransferHandler
 		{
 		private static final long serialVersionUID = 1L;
 
 		protected Transferable createTransferable(JComponent c)
 			{
 			return null;
 			}
 
 		public int getSourceActions(JComponent c)
 			{
 			return NONE;
 			}
 
 		public boolean canImport(TransferHandler.TransferSupport support)
 			{
 			if (!support.isDataFlavorSupported(EventNode.EVENTNODE_FLAVOR)) return false;
 			EventNode t = (EventNode) LGM.eventSelect.events.getLastSelectedPathComponent();
 			if (t == null || !t.isValid()) return false;
 			if (rootEvent.contains(new Event(t.mainId,t.eventId,t.other))) return false;
 			for (DataFlavor f : support.getDataFlavors())
 				if (f == EventNode.EVENTNODE_FLAVOR) return true;
 			return false;
 			}
 
 		public boolean importData(TransferHandler.TransferSupport support)
 			{
 			if (!canImport(support)) return false;
 			try
 				{
 				EventNode t = (EventNode) support.getTransferable().getTransferData(
 						EventNode.EVENTNODE_FLAVOR);
 				Point p = support.getDropLocation().getDropPoint();
 				TreePath path = events.getPathForLocation(p.x,p.y);
 				int func = path == null ? EventFrame.FUNCTION_ADD : LGM.eventSelect.function.getValue();
 
 				switch (func)
 					{
 					case EventFrame.FUNCTION_ADD:
 						if (!t.isValid()) return false;
 						addEvent(new Event(t.mainId,t.eventId,t.other));
 						return true;
 					case EventFrame.FUNCTION_REPLACE:
 						DefaultMutableTreeNode dropNode = (DefaultMutableTreeNode) path.getLastPathComponent();
 						if (!(dropNode instanceof EventInstanceNode) || !t.isValid()) return false;
 						EventInstanceNode drop = (EventInstanceNode) dropNode;
 						Event ev = drop.getUserObject();
 						removeEvent(ev);
 						ev.mainId = t.mainId;
 						ev.id = t.eventId;
 						ev.other = t.other;
 						addEvent(ev);
 						return true;
 					case EventFrame.FUNCTION_DUPLICATE:
 						dropNode = (DefaultMutableTreeNode) path.getLastPathComponent();
 						if (!(dropNode instanceof EventInstanceNode) || !t.isValid()) return false;
 						drop = (EventInstanceNode) dropNode;
 						ev = drop.getUserObject();
 						actions.save();
 						Event ev2 = ev.copy();
 						ev2.mainId = t.mainId;
 						ev2.id = t.eventId;
 						ev2.other = t.other;
 						addEvent(ev2);
 						return true;
 					}
 				}
 			catch (Throwable e)
 				{
 				// This is just to stop the dnd system from silencing exceptions
 				e.printStackTrace();
 				}
 			return false;
 			}
 		}
 
 	private static class EventInstanceNode extends DefaultMutableTreeNode implements
 			Comparable<EventInstanceNode>
 		{
 		private static final long serialVersionUID = 1L;
 
 		public EventInstanceNode(Event e)
 			{
 			super(e);
 			}
 
 		public Event getUserObject()
 			{
 			return (Event) super.getUserObject();
 			}
 
 		public int compareTo(EventInstanceNode n)
 			{
 			return getUserObject().compareTo(n.getUserObject());
 			}
 		}
 
 	private class EventGroupNode extends DefaultMutableTreeNode
 		{
 		private static final long serialVersionUID = 1L;
 		public int mainId;
 
 		public EventGroupNode(int mainId)
 			{
 			super(Messages.getString("MainEvent.EVENT" + mainId)); //$NON-NLS-1$
 			this.mainId = mainId;
 			}
 
 		public boolean contains(Event e)
 			{
 			for (int i = 0; i < getChildCount(); i++)
 				{
 				if (getChildAt(i) instanceof EventInstanceNode)
 					{
 					if (((EventInstanceNode) getChildAt(i)).getUserObject().matchesType(e)) return true;
 					}
 				else if (((EventGroupNode) getChildAt(i)).contains(e)) return true;
 				}
 			return false;
 			}
 
 		public boolean checkAndRemove(Event e)
 			{
 			for (int i = 0; i < getChildCount(); i++)
 				{
 				if (((EventInstanceNode) getChildAt(i)).getUserObject().matchesType(e))
 					{
 					remove(i);
 					events.setSelectionRow(0);
 					events.updateUI();
 					return true;
 					}
 				}
 			return false;
 			}
 
 		@SuppressWarnings("unchecked")
 		public void sortChildren()
 			{
 			// This doesn't seem to have any unwanted effects (directly sorting the protected field)
 			Collections.sort((java.util.Vector<EventInstanceNode>) children);
 			}
 
 		public TreePath childPath(Event e)
 			{
 			for (int i = 0; i < getChildCount(); i++)
 				if (getChildAt(i) instanceof EventInstanceNode
 						&& ((EventInstanceNode) getChildAt(i)).getUserObject().matchesType(e))
 					return new TreePath(((EventInstanceNode) getChildAt(i)).getPath());
 			return null;
 			}
 
 		public void select(Event e)
 			{
 			TreePath p = childPath(e);
 			events.setSelectionPath(p);
 			events.updateUI();
 			events.scrollPathToVisible(p);
 			events.updateUI();
 			}
 		}
 
 	private static class ActionListModel extends AbstractListModel
 		{
 		private static final long serialVersionUID = 1L;
 		private ArrayList<Action> list;
 		private ArrayList<Integer> indents;
 
 		public ActionListModel()
 			{
 			list = new ArrayList<Action>();
 			indents = new ArrayList<Integer>();
 			}
 
 		public void add(Action a)
 			{
 			add(getSize(),a);
 			}
 
 		public void add(int index, Action a)
 			{
 			list.add(index,a);
 			updateIndentation();
 			fireIntervalAdded(this,index,index);
 			}
 
 		public void addAll(int index, Collection<? extends Action> c)
 			{
 			int s = c.size();
 			if (s <= 0) return;
 			list.addAll(index,c);
 			updateIndentation();
 			fireIntervalAdded(this,index,index + s - 1);
 			}
 
 		public void remove(int index)
 			{
 			list.remove(index);
 			updateIndentation();
 			fireIntervalRemoved(this,index,index);
 			}
 
 		public Object getElementAt(int index)
 			{
 			return list.get(index);
 			}
 
 		public int getSize()
 			{
 			return list.size();
 			}
 
 		private void updateIndentation()
 			{
 			int lms = list.size();
 			indents.clear();
 			indents.ensureCapacity(lms);
 			Stack<Integer> levelIndents = new Stack<Integer>();
 			Stack<Stack<Integer>> questions = new Stack<Stack<Integer>>();
 			levelIndents.push(0);
 			questions.push(new Stack<Integer>());
 			int nextIndent = 0;
 			for (int i = 0; i < lms; i++)
 				{
 				Action a = list.get(i);
 				int indent = nextIndent;
 				switch (a.libAction.actionKind)
 					{
 					case Action.ACT_BEGIN:
 						levelIndents.push(indent);
 						questions.push(new Stack<Integer>());
 						break;
 					case Action.ACT_END:
 						indent = levelIndents.peek();
 						if (levelIndents.size() > 1)
 							{
 							levelIndents.pop();
 							questions.pop();
 							}
 						nextIndent = levelIndents.peek();
 						break;
 					case Action.ACT_ELSE:
 						try
 							{
 							int j = questions.peek().pop();
 							if (j >= 0) indent = indents.get(j);
 							}
 						catch (EmptyStackException e)
 							{
 							}
 						nextIndent = indent + 1;
 						break;
 					case Action.ACT_REPEAT:
 						nextIndent++;
 						break;
 					case Action.ACT_EXIT:
 						nextIndent = levelIndents.peek();
 						break;
 					default:
 						if (a.libAction.question)
 							{
 							questions.peek().push(i);
 							nextIndent++;
 							}
 						else if (a.libAction.execType != Action.EXEC_NONE) nextIndent = levelIndents.peek();
 					}
 				indents.add(indent);
 				}
 			}
 		}
 
 	public static final DataFlavor ACTION_FLAVOR = new DataFlavor(Action.class,"Action"); //$NON-NLS-1$
 	public static final DataFlavor ACTION_ARRAY_FLAVOR = new DataFlavor(List.class,"Action array"); //$NON-NLS-1$
 	public static final DataFlavor LIB_ACTION_FLAVOR = new DataFlavor(LibAction.class,
 			"Library action"); //$NON-NLS-1$
 
 	public static class LibActionTransferable implements Transferable
 		{
 		private static final DataFlavor[] FLAVORS = { LIB_ACTION_FLAVOR };
 		private final LibAction libAction;
 
 		public LibActionTransferable(LibAction la)
 			{
 			libAction = la;
 			}
 
 		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException,IOException
 			{
 			if (flavor == LIB_ACTION_FLAVOR)
 				{
 				return libAction;
 				}
 			throw new UnsupportedFlavorException(flavor);
 			}
 
 		public DataFlavor[] getTransferDataFlavors()
 			{
 			return FLAVORS;
 			}
 
 		public boolean isDataFlavorSupported(DataFlavor flavor)
 			{
 			return flavor == LIB_ACTION_FLAVOR;
 			}
 		}
 
 	public static class LibActionTransferHandler extends TransferHandler
 		{
 		private static final long serialVersionUID = 1L;
 
 		public boolean canImport(TransferHandler.TransferSupport info)
 			{
 			return false;
 			}
 
 		public boolean importData(TransferHandler.TransferSupport info)
 			{
 			return false;
 			}
 
 		public int getSourceActions(JComponent c)
 			{
 			return COPY;
 			}
 
 		protected Transferable createTransferable(JComponent c)
 			{
 			LibActionButton lab = (LibActionButton) c;
 			LibAction la = lab.getLibAction();
 			return new LibActionTransferable(la);
 			}
 		}
 
 	public static class ActionTransferable implements Transferable
 		{
 		private final Action[] actions;
 		private final DataFlavor[] flavors;
 
 		public ActionTransferable(Action[] a)
 			{
 			actions = a;
 			ArrayList<DataFlavor> fl = new ArrayList<DataFlavor>(2);
 			fl.add(ACTION_ARRAY_FLAVOR);
 			if (a.length == 1) fl.add(ACTION_FLAVOR);
 			flavors = fl.toArray(new DataFlavor[2]);
 			}
 
 		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException,IOException
 			{
 			if (flavor == ACTION_FLAVOR && actions.length == 1)
 				{
 				return actions[0];
 				}
 			if (flavor == ACTION_ARRAY_FLAVOR)
 				{
 				List<Action> l = Arrays.asList(actions);
 				return l;
 				}
 			throw new UnsupportedFlavorException(flavor);
 			}
 
 		public DataFlavor[] getTransferDataFlavors()
 			{
 			return flavors;
 			}
 
 		public boolean isDataFlavorSupported(DataFlavor flavor)
 			{
 			for (DataFlavor f : flavors)
 				{
 				if (f == flavor) return true;
 				}
 			return false;
 			}
 		}
 
 	public static class ActionTransferHandler extends TransferHandler
 		{
 		private static final long serialVersionUID = 1L;
 		private int[] indices = null;
 		private int addIndex = -1; //Location where items were added
 		private int addCount = 0; //Number of items added.
 
 		@Override
 		protected void exportDone(JComponent source, Transferable data, int action)
 			{
 			if (action == MOVE && indices != null)
 				{
 				JList ls = (JList) source;
 				ActionListModel model = (ActionListModel) ls.getModel();
 				if (addCount > 0)
 					{
 					for (int i = 0; i < indices.length; i++)
 						{
 						if (indices[i] > addIndex)
 							{
 							indices[i] += addCount;
 							}
 						}
 					}
 				for (int i = indices.length - 1; i >= 0; i--)
 					{
 					model.remove(indices[i]);
 					}
 				}
 			indices = null;
 			addCount = 0;
 			addIndex = -1;
 			}
 
 		public boolean canImport(TransferHandler.TransferSupport info)
 			{
 			DataFlavor[] f = info.getDataFlavors();
 			boolean supported = false;
 			for (DataFlavor flav : f)
 				{
 				if (flav == ACTION_FLAVOR || flav == ACTION_ARRAY_FLAVOR || flav == LIB_ACTION_FLAVOR)
 					supported = true;
 				}
 			if (!supported) return false;
 			ActionList list = (ActionList) info.getComponent();
 			JList.DropLocation dl = (JList.DropLocation) info.getDropLocation();
 			if (list.actionContainer == null || dl.getIndex() == -1 || !info.isDrop()) return false;
 			return true;
 			}
 
 		public boolean importData(TransferHandler.TransferSupport info)
 			{
 			if (!canImport(info)) return false;
 			ActionList list = (ActionList) info.getComponent();
 			ActionListModel alm = (ActionListModel) list.getModel();
 			JList.DropLocation dl = (JList.DropLocation) info.getDropLocation();
 			Transferable t = info.getTransferable();
 			int index = dl.getIndex();
 			if (indices != null && index >= indices[0] && index <= indices[indices.length - 1])
 				{
 				indices = null;
 				return false;
 				}
 			if (info.isDataFlavorSupported(ACTION_FLAVOR))
 				{
 				Action a;
 				try
 					{
 					a = (Action) t.getTransferData(ACTION_FLAVOR);
 					}
 				catch (Exception e)
 					{
 					return false;
 					}
 				addIndex = index;
 				addCount = 1;
 				alm.add(index,a);
 				return true;
 				}
 			if (info.isDataFlavorSupported(ACTION_ARRAY_FLAVOR))
 				{
 				Action[] a;
 				try
 					{
 					a = ((List<?>) t.getTransferData(ACTION_ARRAY_FLAVOR)).toArray(new Action[0]);
 					}
 				catch (Exception e)
 					{
 					e.printStackTrace();
 					return false;
 					}
 				addIndex = index;
 				addCount = a.length;
 				alm.addAll(index,Arrays.asList(a));
 				return true;
 				}
 			if (info.isDataFlavorSupported(LIB_ACTION_FLAVOR))
 				{
 				LibAction la;
 				Action a;
 				try
 					{
 					la = (LibAction) t.getTransferData(LIB_ACTION_FLAVOR);
 					a = new Action(la);
 					ActionList.openActionFrame(a);
 					}
 				catch (Exception e)
 					{
 					return false;
 					}
 				addIndex = index;
 				addCount = 1;
 				alm.add(index,a);
 				return true;
 				}
 			return false;
 			}
 
 		public int getSourceActions(JComponent c)
 			{
 			return MOVE;
 			}
 
 		protected Transferable createTransferable(JComponent c)
 			{
 			JList list = (JList) c;
 			indices = list.getSelectedIndices();
 			Object[] o = list.getSelectedValues();
 			Action[] a = new Action[o.length];
 			a = Arrays.asList(o).toArray(a);
 			return new ActionTransferable(a);
 			}
 		}
 
 	private static class ActionRenderer implements ListCellRenderer
 		{
 		public String parse(String s, Action a)
 			{
 			String escape = "FrNw01234567"; //$NON-NLS-1$
 			String ret = ""; //$NON-NLS-1$
 			//s = s.replaceAll("\n","<br>");
 
 			int k = 0;
 			int p = s.indexOf("@"); //$NON-NLS-1$
 			while (p != -1)
 				{
 				ret += s.substring(k,p);
 				char c = s.charAt(p + 1);
 				if (!escape.contains(String.valueOf(c)))
 					{
 					ret += "@"; //$NON-NLS-1$
 					k = p + 1;
 					p = s.indexOf("@",k); //$NON-NLS-1$
 					continue;
 					}
 				if (c == 'F')
 					{
 					if (s.charAt(p + 2) == 'B' || s.charAt(p + 2) == 'I')
 						p += 2;
 					else
 						ret += "@"; //$NON-NLS-1$
 					k = p + 1;
 					p = s.indexOf("@",k); //$NON-NLS-1$
 					continue;
 					}
 				if (c == 'r' && a.relative) ret += Messages.getString("Action.RELATIVE"); //$NON-NLS-1$
 				if (c == 'N' && a.not) ret += Messages.getString("Action.NOT"); //$NON-NLS-1$
 				if (c == 'w' && !a.appliesTo.equals(GmObject.OBJECT_SELF))
 					{
 					if (a.appliesTo.equals(GmObject.OBJECT_OTHER))
 						ret += Messages.getString("Action.APPLIES_OTHER"); //$NON-NLS-1$
 					else
 						{
 						GmObject applies = deRef(a.appliesTo);
 						ret += String.format(Messages.getString("Action.APPLIES"), //$NON-NLS-1$
 								applies == null ? a.appliesTo.toString() : applies.getName());
 						}
 					}
 				if (c >= '0' && c < '8')
 					{
 					int arg = c - '0';
 					if (arg >= a.arguments.length)
 						ret += "0"; //$NON-NLS-1$
 					else
 						{
 						Argument aa = a.arguments[arg];
 						ret += aa.toString(a.libAction.libArguments[arg]);
 						}
 					}
 				k = p + 2;
 				p = s.indexOf("@",k); //$NON-NLS-1$
 				}
 
 			s = ret + s.substring(k);
 			s = s.replaceAll("&","&amp;"); //$NON-NLS-1$ //$NON-NLS-2$
 			s = s.replaceAll("<","&lt;"); //$NON-NLS-1$ //$NON-NLS-2$
 			s = s.replaceAll(">","&gt;"); //$NON-NLS-1$ //$NON-NLS-2$
 			s = s.replaceAll("\n","<br>"); //$NON-NLS-1$ //$NON-NLS-2$
 			s = s.replaceAll("\\\\#","\n"); //$NON-NLS-1$ //$NON-NLS-2$
 			s = s.replaceAll("#","<br>"); //$NON-NLS-1$ //$NON-NLS-2$
 			s = s.replaceAll("\n","&#35;"); //$NON-NLS-1$ //$NON-NLS-2$
 			s = s.replaceAll(" ","&nbsp;"); //$NON-NLS-1$ //$NON-NLS-2$
 
 			return s;
 			}
 
 		public Component getListCellRendererComponent(JList list, Object cell, int index,
 				boolean isSelected, boolean hasFocus)
 			{
 			final Action cellAction = (Action) cell;
 			LibAction la = cellAction.libAction;
 			JLabel l = new JLabel();
 			ListModel lm = list.getModel();
 			try
 				{
 				if (lm instanceof ActionListModel)
 					l.setBorder(new EmptyBorder(1,2 + 8 * ((ActionListModel) lm).indents.get(index),1,2));
 				}
 			catch (IndexOutOfBoundsException e)
 				{
 				}
 			if (isSelected)
 				{
 				l.setBackground(list.getSelectionBackground());
 				l.setForeground(list.getSelectionForeground());
 				}
 			else
 				{
 				l.setBackground(list.getBackground());
 				l.setForeground(list.getForeground());
 				}
 			l.setOpaque(true);
 			if (la.actImage == null)
 				{
 				l.setText(Messages.getString("Action.UNKNOWN")); //$NON-NLS-1$
 				return l;
 				}
 			l.setText(parse(la.listText,(Action) cell));
 			if (la.listText.contains("@FB")) //$NON-NLS-1$
 				l.setText("<b>" + l.getText()); //$NON-NLS-1$
 			if (la.listText.contains("@FI")) //$NON-NLS-1$
 				l.setText("<i>" + l.getText()); //$NON-NLS-1$
 			l.setText("<html>" + l.getText()); //$NON-NLS-1$
 			l.setIcon(new ImageIcon(Util.getTransparentIcon(la.actImage)));
 			l.setToolTipText("<html>" + parse(la.hintText,(Action) cell)); //$NON-NLS-1$
 			return l;
 			}
 		}
 
 	public void addEvent(Event e)
 		{
 		for (int i = 0; i < rootEvent.getChildCount(); i++)
 			{
 			DefaultMutableTreeNode n = (DefaultMutableTreeNode) rootEvent.getChildAt(i);
 			if (n instanceof EventGroupNode)
 				{
 				EventGroupNode group = (EventGroupNode) n;
 				if (group.mainId == e.mainId)
 					{
 					if (!group.contains(e))
 						{
 						group.add(new EventInstanceNode(e));
 						group.sortChildren();
 						}
 					group.select(e);
 					return;
 					}
 				}
 			else
 				{
 				EventInstanceNode ein = (EventInstanceNode) n;
 				if (ein.getUserObject().mainId == e.mainId)
 					{
 					if (!ein.getUserObject().matchesType(e))
 						{
 						EventGroupNode group = new EventGroupNode(e.mainId);
 						int ind = rootEvent.getIndex(ein);
 						rootEvent.remove(ind);
 						rootEvent.insert(group,ind);
 						group.add(ein);
 						group.add(new EventInstanceNode(e));
 						group.sortChildren();
 						group.select(e);
 						}
 					else
 						{
 						rootEvent.select(e);
 						}
 					return;
 					}
 				}
 			}
 		for (int i = 0; i < rootEvent.getChildCount(); i++)
 			{
 			int mid;
 			if (rootEvent.getChildAt(i) instanceof EventInstanceNode)
 				mid = ((EventInstanceNode) rootEvent.getChildAt(i)).getUserObject().mainId;
 			else
 				mid = ((EventGroupNode) rootEvent.getChildAt(i)).mainId;
 			if (mid > e.mainId)
 				{
 				EventInstanceNode ein = new EventInstanceNode(e);
 				rootEvent.insert(ein,i);
 				rootEvent.select(e);
 				return;
 				}
 			}
 		EventInstanceNode ein = new EventInstanceNode(e);
 		rootEvent.add(ein);
 		rootEvent.select(e);
 		}
 
 	public void removeEvent(Event e)
 		{
 		for (int i = 0; i < rootEvent.getChildCount(); i++)
 			{
 			if (rootEvent.getChildAt(i) instanceof EventInstanceNode)
 				{
 				if (((EventInstanceNode) rootEvent.getChildAt(i)).getUserObject().matchesType(e))
 					{
 					rootEvent.remove(i);
					if (rootEvent.getChildCount() == 0)
						{
						lastValidEventSelection = null;
						actions.setActionContainer(null);
						}
 					events.updateUI();
 					events.setSelectionRow(0);
 					return;
 					}
 				}
 			if (rootEvent.getChildAt(i) instanceof EventGroupNode)
 				{
 				EventGroupNode group = (EventGroupNode) rootEvent.getChildAt(i);
 				if (group.checkAndRemove(e))
 					{
 					if (group.getChildCount() == 1)
 						{
 						rootEvent.remove(i);
 						rootEvent.insert((EventInstanceNode) group.getChildAt(0),i);
 						events.updateUI();
 						events.setSelectionRow(0);
 						}
 					return;
 					}
 				}
 			}
 		}
 
 	public void makeEventTree(GmObject res)
 		{
 		rootEvent = new EventGroupNode(-1);
 		for (int m = 0; m < 11; m++)
 			{
 			MainEvent me = res.mainEvents[m];
 			ArrayList<Event> ale = me.events;
 			if (ale.size() == 1)
 				{
 				rootEvent.add(new EventInstanceNode(ale.get(0)));
 				}
 			if (ale.size() > 1)
 				{
 				EventGroupNode node = new EventGroupNode(m);
 				rootEvent.add(node);
 				for (Event e : ale)
 					node.add(new EventInstanceNode(e));
 				}
 			}
 		events = new EventTree(rootEvent);
 		events.setScrollsOnExpand(true);
 		DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer()
 			{
 				private static final long serialVersionUID = 1L;
 
 				public Component getTreeCellRendererComponent(JTree tree, Object val, boolean sel,
 						boolean exp, boolean leaf, int row, boolean focus)
 					{
 					super.getTreeCellRendererComponent(tree,val,sel,exp,leaf,row,focus);
 					return this;
 					}
 			};
 		events.setCellRenderer(renderer);
 		events.setRootVisible(false);
 		events.setShowsRootHandles(true);
 		events.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
 		events.addTreeSelectionListener(this);
 		if (LGM.javaVersion >= 10600)
 			{
 			events.setDragEnabled(true);
 			events.setDropMode(DropMode.ON);
 			events.setTransferHandler(new EventNodeTransferHandler());
 			}
 		}
 
 	public void saveEvents()
 		{
 		actions.save();
 		Enumeration<?> dfe = rootEvent.depthFirstEnumeration();
 		for (MainEvent me : res.mainEvents)
 			me.events.clear();
 		while (dfe.hasMoreElements())
 			{
 			Object o = dfe.nextElement();
 			if (o instanceof EventInstanceNode)
 				{
 				EventInstanceNode ein = (EventInstanceNode) o;
 				if (ein.getUserObject().actions.size() > 0)
 					{
 					Event e = ein.getUserObject();
 					res.mainEvents[e.mainId].events.add(e);
 					}
 				}
 			}
 		}
 
 	public static class ActionList extends JList
 		{
 		private static final long serialVersionUID = 1L;
 		private static final Hashtable<Action,MDIFrame> FRAMES = new Hashtable<Action,MDIFrame>();
 		private static final ActionListMouseListener ALML = new ActionListMouseListener();
 		private static final ActionListKeyListener ALKL = new ActionListKeyListener();
 		private ActionContainer actionContainer;
 		private ActionListModel model;
 
 		public ActionList()
 			{
 			setActionContainer(null);
 			if (LGM.javaVersion >= 10600)
 				{
 				setTransferHandler(new ActionTransferHandler());
 				setDragEnabled(true);
 				setDropMode(DropMode.ON_OR_INSERT);
 				}
 			addMouseListener(ALML);
 			addKeyListener(ALKL);
 			setCellRenderer(new ActionRenderer());
 			}
 
 		public void setActionContainer(ActionContainer ac)
 			{
 			save();
 			actionContainer = ac;
 			model = new ActionListModel();
 			setModel(model);
 			if (ac == null) return;
 			model.addAll(0,ac.actions);
 			}
 
 		public ActionContainer getActionContainer()
 			{
 			return actionContainer;
 			}
 
 		public void save()
 			{
 			if (actionContainer == null) return;
 			actionContainer.actions = model.list;
 			}
 
 		/**
 		 * Opens an ActionFrame representing a given action.
 		 * Actions like "else" etc. will not have a frame opened.
 		 * @param a The action to open a frame for
 		 * @return The frame opened or <code>null</code> if no
 		 * frame was opened.
 		 */
 		public static MDIFrame openActionFrame(Action a)
 			{
 			int k = a.libAction.actionKind;
 			if (k != Action.ACT_NORMAL && k != Action.ACT_REPEAT && k != Action.ACT_VARIABLE
 					&& k != Action.ACT_CODE) return null;
 			MDIFrame af = FRAMES.get(a);
 			if (af == null || af.isClosed())
 				{
 				af = new ActionFrame(a);
 				LGM.mdi.add(af);
 				FRAMES.put(a,af);
 				}
 			af.setVisible(true);
 			af.toFront();
 			try
 				{
 				af.setIcon(false);
 				af.setSelected(true);
 				}
 			catch (PropertyVetoException pve)
 				{
 				}
 			return af;
 			}
 
 		private static class ActionListMouseListener extends MouseAdapter
 			{
 			public void mouseClicked(MouseEvent e)
 				{
 				if (e.getClickCount() != 2) return;
 				JList l = (JList) e.getSource();
 				Object o = l.getSelectedValue();
 				if (o == null || !(o instanceof Action)) return;
 				openActionFrame((Action) o);
 				}
 			}
 
 		private static class ActionListKeyListener extends KeyAdapter
 			{
 			@Override
 			public void keyPressed(KeyEvent e)
 				{
 				JList l = (JList) e.getSource();
 				switch (e.getKeyCode())
 					{
 					case KeyEvent.VK_DELETE:
 						int[] indices = l.getSelectedIndices();
 						ActionListModel alm = (ActionListModel) l.getModel();
 						for (int i = indices.length - 1; i >= 0; i--)
 							alm.remove(indices[i]);
 						e.consume();
 						break;
 					}
 				}
 			}
 		}
 
 	public static ActionList addActionPane(JComponent container)
 		{
 		JPanel side3 = new JPanel(new BorderLayout());
 		side3.setPreferredSize(new Dimension(50,319));
 		JLabel lab = new JLabel(Messages.getString("GmObjectFrame.ACTIONS")); //$NON-NLS-1$
 		side3.add(lab,"North"); //$NON-NLS-1$
 		ActionList list = new ActionList();
 		JScrollPane scroll = new JScrollPane(list);
 		side3.add(scroll,"Center"); //$NON-NLS-1$
 
 		JTabbedPane side4 = GmObjectFrame.makeLibraryTabs(list);
 		side4.setPreferredSize(new Dimension(140,319));
 		container.add(side3);
 		container.add(side4);
 		return list;
 		}
 
 	private static JPanel makeLabelPane(String name)
 		{
 		JPanel lp = new JPanel(new GridLayout(0,3,0,0));
 		Border mb = BorderFactory.createMatteBorder(1,0,0,0,new Color(184,207,229));
 		Border tb = BorderFactory.createTitledBorder(mb,name);
 		lp.setBorder(tb);
 		return lp;
 		}
 
 	public static class LibActionButton extends JLabel
 		{
 		private static final long serialVersionUID = 1L;
 		private static LibActionTransferHandler transferHandler = new LibActionTransferHandler();
 		private LibAction libAction;
 		private ActionList list;
 
 		public LibActionButton(LibAction la, ActionList list)
 			{
 			super(new ImageIcon(la.actImage));
 			this.list = list;
 			setToolTipText(la.description);
 			libAction = la;
 			setTransferHandler(transferHandler);
 			}
 
 		public void processMouseEvent(MouseEvent e)
 			{
 			if (e.getID() == MouseEvent.MOUSE_PRESSED && e.getButton() == MouseEvent.BUTTON1)
 				{
 				JComponent c = (JComponent) e.getSource();
 				TransferHandler handler = c.getTransferHandler();
 				handler.exportAsDrag(c,e,TransferHandler.COPY);
 				}
 			else if (e.getID() == MouseEvent.MOUSE_PRESSED && e.getButton() == MouseEvent.BUTTON3
 					&& list.getActionContainer() != null)
 				{
 				Action act = new Action(libAction);
 				((ActionListModel) list.getModel()).add(act);
 				ActionList.openActionFrame(act);
 				}
 			super.processMouseEvent(e);
 			}
 
 		public LibAction getLibAction()
 			{
 			return libAction;
 			}
 		}
 
 	//XXX: possibly extract to some place like resources.library.LibManager
 	public static JTabbedPane makeLibraryTabs(ActionList actions)
 		{
 		JTabbedPane tp = new JTabbedPane(JTabbedPane.RIGHT);
 
 		tp.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
 		JPanel lp = null;
 		for (Library l : LibManager.libs)
 			{
 			JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
 			for (LibAction la : l.libActions)
 				{
 				if (la.hidden || la.actionKind == Action.ACT_SEPARATOR) continue;
 				if (la.advanced && !la.advanced) continue;
 				JLabel b;
 				if (la.actionKind == Action.ACT_LABEL)
 					{
 					lp = makeLabelPane(la.name);
 					p.add(lp);
 					continue;
 					}
 				if (la.actionKind == Action.ACT_PLACEHOLDER)
 					b = new JLabel();
 				else
 					b = new LibActionButton(la,actions);
 				b.setHorizontalAlignment(JLabel.LEFT);
 				b.setVerticalAlignment(JLabel.TOP);
 				b.setPreferredSize(new Dimension(30,30));
 				if (lp == null)
 					{
 					lp = makeLabelPane(null);
 					p.add(lp);
 					}
 				lp.add(b);
 				}
 			tp.addTab(l.tabCaption,p);
 			if (LGM.javaVersion >= 10600)
 				tp.setTabComponentAt(tp.getTabCount() - 1,new JLabel(new VTextIcon(tp,l.tabCaption)));
 			}
 		return tp;
 		}
 
 	@Override
 	public boolean resourceChanged()
 		{
 		commitChanges();
 		ResourceComparator c = new ResourceComparator();
 		return !c.areEqual(res,resOriginal);
 		}
 
 	@Override
 	public void revertResource()
 		{
 		LGM.currentFile.gmObjects.replace(res,resOriginal);
 		}
 
 	public void commitChanges()
 		{
 		saveEvents();
 		res.setName(name.getText());
 		res.sprite = sprite.getSelected();
 		res.visible = visible.isSelected();
 		res.solid = solid.isSelected();
 		res.depth = depth.getIntValue();
 		res.persistent = persistent.isSelected();
 		res.parent = parent.getSelected();
 		res.mask = mask.getSelected();
 		}
 
 	public void actionPerformed(ActionEvent e)
 		{
 		if (e.getSource() == newSprite)
 			{
 			ResNode n = Listener.getPrimaryParent(Resource.SPRITE);
 			Sprite spr = LGM.currentFile.sprites.add();
 			Listener.putNode(LGM.tree,n,n,Resource.SPRITE,n.getChildCount(),spr);
 			sprite.setSelected(new WeakReference<Sprite>(spr));
 			return;
 			}
 		if (e.getSource() == editSprite)
 			{
 			Sprite spr = deRef(sprite.getSelected());
 			if (spr == null) return;
 			spr.getNode().openFrame();
 			return;
 			}
 		if (e.getSource() == sprite)
 			{
 			preview.setIcon(GmTreeGraphics.getSpriteIcon(sprite.getSelected()));
 			return;
 			}
 		if (e.getSource() == parent)
 			{
 			WeakReference<GmObject> p = parent.getSelected();
 			res.parent = p;
 			if (deRef(p) != null) if (isCyclic(res))
 				{
 				String msg = Messages.getString("GmObjectFrame.LOOPING_PARENTS"); //$NON-NLS-1$
 				String ttl = Messages.getString("GmObjectFrame.ERROR"); //$NON-NLS-1$
 				JOptionPane.showMessageDialog(this,msg,ttl,JOptionPane.ERROR_MESSAGE);
 				parent.setSelected(null);
 				res.parent = null;
 				}
 			return;
 			}
 		if (e.getSource() == deleteEvent)
 			{
 			Object comp = events.getLastSelectedPathComponent();
 			if (!(comp instanceof EventInstanceNode)) return;
 			EventInstanceNode ein = (EventInstanceNode) comp;
 			removeEvent(ein.getUserObject());
 			return;
 			}
 		super.actionPerformed(e);
 		}
 
 	private boolean isCyclic(GmObject inheritor)
 		{
 		ArrayList<GmObject> traversed = new ArrayList<GmObject>();
 		traversed.add(inheritor);
 		while (deRef(inheritor.parent) != null)
 			{
 			GmObject p = deRef(inheritor.parent);
 			if (traversed.contains(p)) return true;
 			inheritor = p;
 			traversed.add(inheritor);
 			}
 		return false;
 		}
 
 	public void valueChanged(TreeSelectionEvent tse)
 		{
 		DefaultMutableTreeNode node = (DefaultMutableTreeNode) events.getLastSelectedPathComponent();
 		if (node == null || !node.isLeaf() || !(node.getUserObject() instanceof Event))
 			{
 			if (node != null && !node.isLeaf())
 				{
 				TreePath path = new TreePath(node.getPath());
 				if (events.isExpanded(path))
 					events.collapsePath(path);
 				else
 					events.expandPath(path);
 				}
 			if (lastValidEventSelection != null)
 				events.setSelectionPath(new TreePath(lastValidEventSelection.getPath()));
 			return;
 			}
 		lastValidEventSelection = node;
 		actions.setActionContainer((Event) node.getUserObject());
 		}
 	}
