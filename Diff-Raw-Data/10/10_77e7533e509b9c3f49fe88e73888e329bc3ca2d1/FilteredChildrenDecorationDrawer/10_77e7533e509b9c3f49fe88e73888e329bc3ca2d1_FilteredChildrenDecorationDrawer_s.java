 package org.eclipse.mylyn.internal.context.ui;
 
 import org.eclipse.jface.viewers.StructuredSelection;
 import org.eclipse.jface.viewers.TreeViewer;
 import org.eclipse.mylyn.internal.provisional.commons.ui.CommonImages;
 import org.eclipse.mylyn.internal.provisional.commons.ui.PlatformUiUtil;
 import org.eclipse.mylyn.internal.provisional.commons.ui.WorkbenchUtil;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.events.MouseEvent;
 import org.eclipse.swt.events.MouseListener;
 import org.eclipse.swt.events.MouseMoveListener;
 import org.eclipse.swt.events.MouseTrackListener;
 import org.eclipse.swt.graphics.Image;
 import org.eclipse.swt.graphics.Point;
 import org.eclipse.swt.graphics.Rectangle;
 import org.eclipse.swt.widgets.Event;
 import org.eclipse.swt.widgets.Listener;
 import org.eclipse.swt.widgets.ToolTip;
 import org.eclipse.swt.widgets.Tree;
 import org.eclipse.swt.widgets.TreeItem;
 import org.eclipse.ui.PlatformUI;
 
 /**
  * @author Mik Kersten
  */
 
 public class FilteredChildrenDecorationDrawer implements Listener {
 
 	private final class MoveListener implements MouseMoveListener, MouseListener, MouseTrackListener {
 
 		private TreeItem lastItem;
 
 		private TreeItem currentItem;
 
 		private long lastMoveTime;
 
 		private long startMoveTime;
 
 		private final TreeViewer viewer;
 
 		private final BrowseFilteredListener browseFilteredListener;
 
 		private ToolTip toolTip;
 
 		public MoveListener(TreeViewer viewer, BrowseFilteredListener browseFilteredListener) {
 			this.viewer = viewer;
 			this.browseFilteredListener = browseFilteredListener;
 		}
 
 		public void mouseEnter(MouseEvent e) {
 			mouseMove(e);
 		}
 
 		public void mouseExit(MouseEvent e) {
 			if (lastItem != null && !lastItem.isDisposed()) {
 				lastItem.setData(ID_HOVER, NodeState.LESS);
 			}
 			if (toolTip != null && !toolTip.isDisposed()) {
 				toolTip.setVisible(false);
 				toolTip.dispose();
 				toolTip = null;
 			}
 
 			redrawTree(lastItem);
 			lastItem = null;
 			currentItem = null;
 		}
 
 		private void redrawTree(TreeItem item) {
 			if (viewer.getTree() != null && !viewer.getTree().isDisposed()) {
 				if (item != null && !item.isDisposed()) {
 					Rectangle bounds = item.getBounds();
					viewer.getTree().redraw(bounds.x, bounds.y, viewer.getTree().getSize().x, bounds.height, true);
 				} else {
 					viewer.getTree().redraw();
 				}
 			}
 		}
 
 		public void mouseHover(MouseEvent e) {
 
 			if (toolTip == null || toolTip.isDisposed()) {
 				toolTip = new ToolTip(WorkbenchUtil.getShell(), PlatformUiUtil.getSwtTooltipStyle());
 			}
 
 			if (toolTip != null && !toolTip.isDisposed()) {
 				Tree tree = (Tree) e.widget;
 				TreeItem item = findItem(tree, e.y);
 				if (item != null && !item.isDisposed()) {
 					Object data = item.getData(ID_HOVER);
 					if (data == NodeState.MORE) {
 						toolTip.setMessage(Messages.FilteredChildrenDecorationDrawer_Show_Filtered_Children);
 					} else {
 						toolTip.setMessage(Messages.FilteredChildrenDecorationDrawer_No_Filtered_Children);
 					}
 					if (inImageBounds(tree, item, e)) {
 						toolTip.setVisible(true);
 					}
 				}
 			}
 		}
 
 		private boolean inImageBounds(Tree tree, TreeItem item, MouseEvent e) {
 			int selectedX = e.x;
 
 			int imageStartX = getImageStartX(item.getBounds().x, item.getBounds().width, tree);
 
 			int imageEndX = imageStartX + moreImage.getBounds().width;
 
 			return selectedX > imageStartX && selectedX < imageEndX;
 		}
 
 		public void mouseMove(MouseEvent e) {
 			if (toolTip != null && !toolTip.isDisposed()) {
 				toolTip.setVisible(false);
 				toolTip.dispose();
 				toolTip = null;
 			}
 
 			if (!(e.widget instanceof Tree) || e.widget.isDisposed()) {
 				return;
 			}
 			Tree tree = (Tree) e.widget;
 			final TreeItem item = findItem(tree, e.y);
 			if (item != null && !item.isDisposed()) {
 				if (lastItem != null && !lastItem.isDisposed() && !lastItem.equals(item)) {
 					boolean redraw = lastItem.getData(ID_HOVER) != NodeState.LESS;
 					lastItem.setData(ID_HOVER, NodeState.LESS);
 					if (redraw) {// hide the + immediately
 						redrawTree(lastItem);
 					}
 				}
 
 				currentItem = item;
 				long currentTime = System.currentTimeMillis();
 				if (currentTime - lastMoveTime > 250) {// user paused movement
 					startMoveTime = currentTime;
 				}
 
 				lastMoveTime = currentTime;
 				// be responsive for small moves but delay more for bigger ones
 				int delay = Math.min(100, (int) ((currentTime - startMoveTime) / 4.0));
 				PlatformUI.getWorkbench().getDisplay().timerExec(delay, new Runnable() {
 					public void run() {// do nothing if we aren't using the most recent item
 						if (currentItem == item && !item.isDisposed()) {
 							if (item.getData(ID_HOVER) != NodeState.MORE_ERROR) {
 								item.setData(ID_HOVER, NodeState.MORE);
 							}
 							if (lastItem == null || (!lastItem.isDisposed() && !lastItem.equals(item))) {
 								redrawTree(lastItem);
 								redrawTree(item);
 							}
 							lastItem = item;
 						}
 					}
 				});
 			} else {
 				if (lastItem != null && !lastItem.isDisposed() && !lastItem.equals(item)) {
 					lastItem.setData(ID_HOVER, NodeState.LESS);
 					redrawTree(lastItem);
 				}
 				lastItem = item;
 			}
 		}
 
 		public void dispose() {
 			if (toolTip != null && !toolTip.isDisposed()) {
 				toolTip.dispose();
 			}
 		}
 
 		public void mouseDoubleClick(MouseEvent e) {
 			// ignore
 
 		}
 
 		public void mouseDown(MouseEvent e) {
 			if (toolTip != null && !toolTip.isDisposed()) {
 				toolTip.setVisible(false);
 				toolTip.dispose();
 				toolTip = null;
 			}
 
 			if (!(e.widget instanceof Tree) || e.widget.isDisposed()) {
 				// we only handle tree's
 				return;
 			}
 
 			Tree tree = (Tree) e.widget;
 			TreeItem item = findItem(tree, e.y);
 
 			if (item == null || item.isDisposed()) {
 				// we can't do anything if we cant find the tree items
 				return;
 			}
 
 			int prevNumberItems = item.getItemCount();
 			boolean prevHasData = true;
 			if (prevNumberItems > 0) {
 				prevHasData = item.getItem(0).getData() != null;
 			}
 			if (inImageBounds(tree, item, e)) {
 				browseFilteredListener.setWasExternalClick(true);
 				browseFilteredListener.unfilterSelection(viewer, new StructuredSelection(item.getData()));
 
 				int newNumItems = item.getItemCount();
 				if (newNumItems == prevNumberItems && prevHasData) {
 					item.setData(ID_HOVER, NodeState.MORE_ERROR);
 					redrawTree(item);
 				}
 			}
 		}
 
 		public void mouseUp(MouseEvent e) {
 			// ignore
 
 		}
 	}
 
 	private static final int IMAGE_PADDING = 5;
 
 	// XXX Update Images
 	private final Image moreImage = CommonImages.getImage(CommonImages.EXPAND);
 
 	private final Image moreErrorImage = CommonImages.getImage(CommonImages.REMOVE);
 
 	enum NodeState {
 		MORE, LESS, MORE_ERROR
 	};
 
 	private static final String ID_HOVER = "mylyn-context-hover"; //$NON-NLS-1$
 
 	private final TreeViewer treeViewer;
 
 	private MoveListener listener;
 
 	private final BrowseFilteredListener browseFilteredListener;
 
 	public FilteredChildrenDecorationDrawer(TreeViewer treeViewer, BrowseFilteredListener browseFilteredListener) {
 		this.treeViewer = treeViewer;
 		this.browseFilteredListener = browseFilteredListener;
 	}
 
 	public void applyToTreeViewer() {
 		if (treeViewer.getTree() != null && !treeViewer.getTree().isDisposed()) {
 			treeViewer.getTree().addListener(SWT.PaintItem, this);
 
 			listener = new MoveListener(treeViewer, browseFilteredListener);
 			treeViewer.getTree().addMouseMoveListener(listener);
 			treeViewer.getTree().addMouseListener(listener);
 			treeViewer.getTree().addMouseTrackListener(listener);
 		}
 	}
 
 	public void dispose() {
 		if (treeViewer.getTree() == null || treeViewer.getTree().isDisposed()) {
 			return;
 		}
 		treeViewer.getTree().removeListener(SWT.PaintItem, this);
 
 		if (listener != null) {
 			treeViewer.getTree().removeMouseMoveListener(listener);
 			treeViewer.getTree().removeMouseListener(listener);
 			treeViewer.getTree().removeMouseTrackListener(listener);
 			listener.dispose();
 		}
 	}
 
 	/*
 	 * NOTE: MeasureItem, PaintItem and EraseItem are called repeatedly.
 	 * Therefore, it is critical for performance that these methods be as
 	 * efficient as possible.
 	 */
 	public void handleEvent(Event event) {
 
 		if (!(event.widget instanceof Tree)) {
 			// we only handle tree's
 			return;
 		}
 
 		switch (event.type) {
 		case SWT.PaintItem: {
 			Tree tree = (Tree) event.widget;
 			if (tree.isDisposed() || event.index != 0) {
 				return;
 			}
 			TreeItem item = findItem(tree, event.y);
 			if (item == null || item.isDisposed()) {
 				return;
 			}
 
 			int imageStartX = getImageStartX(event.x, event.width, tree);
 
 			NodeState value = (NodeState) item.getData(ID_HOVER);
 
 			int imageStartY = event.y;
 			int imageHeight = moreImage.getBounds().height;
 			if (value != null && value.equals(NodeState.MORE_ERROR)) {
 				imageHeight = moreErrorImage.getBounds().height;
 			}
 
 			int offset = Math.round(((float) event.height) / 2 - ((float) imageHeight) / 2);
 			imageStartY += offset;

 			if (value != null && value.equals(NodeState.MORE)) {
 				event.gc.drawImage(moreImage, imageStartX, imageStartY);
 			} else if (value != null && value.equals(NodeState.MORE_ERROR)) {
 				event.gc.drawImage(moreErrorImage, imageStartX, imageStartY);
 			}
 			break;
 		}
 		}
 	}
 
 	private int getImageStartX(int x, int width, Tree tree) {
 		int imageStartX = x + width + IMAGE_PADDING;
 
 		int imageEndX = imageStartX + moreImage.getBounds().width;
 
 		Rectangle clientArea = tree.getClientArea();
 		int currentTreeBounds = clientArea.x + clientArea.width;
 		if (imageStartX > currentTreeBounds) {
 			imageStartX = currentTreeBounds - moreImage.getBounds().width;
 		}
 
 		float tolerance = 0;//moreImage.getBounds().width - (((float) moreImage.getBounds().width) / 3 * 2); // draw over item if more than 33% of the image is hidden 
 		if (imageEndX > currentTreeBounds && imageEndX - currentTreeBounds > tolerance) {
 			imageStartX = currentTreeBounds - moreImage.getBounds().width;
 		}
 		return imageStartX;
 	}
 
 	private TreeItem findItem(Tree tree, int y) {
 		TreeItem item = null;
 		Point size = tree.getSize();
 		final int RATE = 17;
 		for (int i = 0; i <= RATE && item == null; i++) {
 			int position = size.x / RATE + (i * size.x / RATE);
 			item = tree.getItem(new Point(position, y));
 		}
 		return item;
 	}
 }
