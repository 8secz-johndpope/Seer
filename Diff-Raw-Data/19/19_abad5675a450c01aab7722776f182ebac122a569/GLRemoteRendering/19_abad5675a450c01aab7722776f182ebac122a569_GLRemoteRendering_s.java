 package org.caleydo.core.view.opengl.canvas.remote;
 
 import gleem.linalg.Rotf;
 import gleem.linalg.Vec3f;
 import gleem.linalg.Vec4f;
 import gleem.linalg.open.Transform;
 
 import java.awt.Dimension;
 import java.awt.Font;
 import java.util.ArrayList;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Set;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 import javax.media.opengl.GL;
 import javax.media.opengl.GLAutoDrawable;
 
 import org.caleydo.core.command.ECommandType;
 import org.caleydo.core.command.view.opengl.CmdCreateGLEventListener;
 import org.caleydo.core.data.collection.ESetType;
 import org.caleydo.core.data.collection.ISet;
 import org.caleydo.core.data.graph.ICaleydoGraphItem;
 import org.caleydo.core.data.graph.pathway.core.PathwayGraph;
 import org.caleydo.core.data.selection.DeltaEventContainer;
 import org.caleydo.core.data.selection.ESelectionType;
 import org.caleydo.core.data.selection.EVAOperation;
 import org.caleydo.core.data.selection.ISelectionDelta;
 import org.caleydo.core.manager.ICommandManager;
 import org.caleydo.core.manager.IEventPublisher;
 import org.caleydo.core.manager.IViewManager;
 import org.caleydo.core.manager.event.EMediatorType;
 import org.caleydo.core.manager.event.IDListEventContainer;
 import org.caleydo.core.manager.event.IEventContainer;
 import org.caleydo.core.manager.event.IMediatorReceiver;
 import org.caleydo.core.manager.event.IMediatorSender;
 import org.caleydo.core.manager.event.view.ViewActivationEvent;
 import org.caleydo.core.manager.event.view.pathway.DisableGeneMappingEvent;
 import org.caleydo.core.manager.event.view.pathway.DisableNeighborhoodEvent;
 import org.caleydo.core.manager.event.view.pathway.DisableTexturesEvent;
 import org.caleydo.core.manager.event.view.pathway.EnableGeneMappingEvent;
 import org.caleydo.core.manager.event.view.pathway.EnableNeighborhoodEvent;
 import org.caleydo.core.manager.event.view.pathway.EnableTexturesEvent;
 import org.caleydo.core.manager.event.view.remote.CloseOrResetViewsEvent;
 import org.caleydo.core.manager.event.view.remote.DisableConnectionLinesEvent;
 import org.caleydo.core.manager.event.view.remote.EnableConnectionLinesEvent;
 import org.caleydo.core.manager.event.view.remote.LoadPathwayEvent;
 import org.caleydo.core.manager.event.view.remote.LoadPathwaysByGeneEvent;
 import org.caleydo.core.manager.general.GeneralManager;
 import org.caleydo.core.manager.id.EManagedObjectType;
 import org.caleydo.core.manager.picking.EPickingMode;
 import org.caleydo.core.manager.picking.EPickingType;
 import org.caleydo.core.manager.picking.Pick;
 import org.caleydo.core.util.system.SystemTime;
 import org.caleydo.core.util.system.Time;
 import org.caleydo.core.view.IView;
 import org.caleydo.core.view.opengl.camera.EProjectionMode;
 import org.caleydo.core.view.opengl.camera.IViewFrustum;
 import org.caleydo.core.view.opengl.camera.ViewFrustum;
 import org.caleydo.core.view.opengl.canvas.AGLEventListener;
 import org.caleydo.core.view.opengl.canvas.EDetailLevel;
 import org.caleydo.core.view.opengl.canvas.cell.GLCell;
 import org.caleydo.core.view.opengl.canvas.glyph.gridview.GLGlyph;
 import org.caleydo.core.view.opengl.canvas.pathway.GLPathway;
 import org.caleydo.core.view.opengl.canvas.remote.bucket.BucketMouseWheelListener;
 import org.caleydo.core.view.opengl.canvas.remote.bucket.GLConnectionLineRendererBucket;
 import org.caleydo.core.view.opengl.canvas.remote.jukebox.GLConnectionLineRendererJukebox;
 import org.caleydo.core.view.opengl.canvas.remote.listener.AddPathwayListener;
 import org.caleydo.core.view.opengl.canvas.remote.listener.CloseOrResetViewsListener;
 import org.caleydo.core.view.opengl.canvas.remote.listener.DisableConnectionLinesListener;
 import org.caleydo.core.view.opengl.canvas.remote.listener.DisableGeneMappingListener;
 import org.caleydo.core.view.opengl.canvas.remote.listener.DisableNeighborhoodListener;
 import org.caleydo.core.view.opengl.canvas.remote.listener.DisableTexturesListener;
 import org.caleydo.core.view.opengl.canvas.remote.listener.EnableConnectionLinesListener;
 import org.caleydo.core.view.opengl.canvas.remote.listener.EnableGeneMappingListener;
 import org.caleydo.core.view.opengl.canvas.remote.listener.EnableNeighborhoodListener;
 import org.caleydo.core.view.opengl.canvas.remote.listener.EnableTexturesListener;
 import org.caleydo.core.view.opengl.canvas.remote.listener.LoadPathwaysByGeneListener;
 import org.caleydo.core.view.opengl.canvas.storagebased.AStorageBasedView;
 import org.caleydo.core.view.opengl.canvas.storagebased.GLHeatMap;
 import org.caleydo.core.view.opengl.canvas.storagebased.GLParallelCoordinates;
 import org.caleydo.core.view.opengl.mouse.PickingMouseListener;
 import org.caleydo.core.view.opengl.renderstyle.GeneralRenderStyle;
 import org.caleydo.core.view.opengl.renderstyle.layout.ARemoteViewLayoutRenderStyle;
 import org.caleydo.core.view.opengl.renderstyle.layout.BucketLayoutRenderStyle;
 import org.caleydo.core.view.opengl.renderstyle.layout.JukeboxLayoutRenderStyle;
 import org.caleydo.core.view.opengl.renderstyle.layout.ListLayoutRenderStyle;
 import org.caleydo.core.view.opengl.renderstyle.layout.ARemoteViewLayoutRenderStyle.LayoutMode;
 import org.caleydo.core.view.opengl.util.GLHelperFunctions;
 import org.caleydo.core.view.opengl.util.drag.GLDragAndDrop;
 import org.caleydo.core.view.opengl.util.hierarchy.RemoteElementManager;
 import org.caleydo.core.view.opengl.util.hierarchy.RemoteLevel;
 import org.caleydo.core.view.opengl.util.hierarchy.RemoteLevelElement;
 import org.caleydo.core.view.opengl.util.infoarea.GLInfoAreaManager;
 import org.caleydo.core.view.opengl.util.slerp.SlerpAction;
 import org.caleydo.core.view.opengl.util.slerp.SlerpMod;
 import org.caleydo.core.view.opengl.util.texture.EIconTextures;
 import org.caleydo.core.view.opengl.util.texture.GLOffScreenTextureRenderer;
 import org.caleydo.core.view.serialize.ASerializedView;
 import org.caleydo.core.view.serialize.SerializedHeatMapView;
 import org.caleydo.core.view.serialize.SerializedParallelCoordinatesView;
 import org.caleydo.core.view.serialize.SerializedPathwayView;
 import org.caleydo.core.view.serialize.SerializedRemoteRenderingView;
 import org.caleydo.util.graph.EGraphItemHierarchy;
 import org.caleydo.util.graph.EGraphItemProperty;
 import org.caleydo.util.graph.IGraphItem;
 
 import com.sun.opengl.util.j2d.TextRenderer;
 import com.sun.opengl.util.texture.Texture;
 import com.sun.opengl.util.texture.TextureCoords;
 
 /**
  * Abstract class that is able to remotely rendering views. Subclasses implement the positioning of the views
  * (bucket, jukebox, etc.).
  * 
  * @author Marc Streit
  * @author Alexander Lex
  * @author Werner Puff
  */
 public class GLRemoteRendering
 	extends AGLEventListener
 	implements IMediatorReceiver, IMediatorSender, IGLCanvasRemoteRendering {
 
 	Logger log = Logger.getLogger(GLRemoteRendering.class.getName());
 
 	private ARemoteViewLayoutRenderStyle.LayoutMode layoutMode;
 
 	private static final int SLERP_RANGE = 1000;
 	private static final int SLERP_SPEED = 1400;
 
 	// private GenericSelectionManager selectionManager;
 
 	private int iMouseOverObjectID = -1;
 
 	private RemoteLevel focusLevel;
 	private RemoteLevel stackLevel;
 	private RemoteLevel poolLevel;
 	private RemoteLevel transitionLevel;
 	private RemoteLevel spawnLevel;
 	private RemoteLevel externalSelectionLevel;
 
 	private ArrayList<SlerpAction> arSlerpActions;
 
 	private GLHeatMap glSelectionHeatMap;
 
 	private Time time;
 
 	/**
 	 * Slerp factor: 0 = source; 1 = destination
 	 */
 	private int iSlerpFactor = 0;
 
 	protected AGLConnectionLineRenderer glConnectionLineRenderer;
 
 	private int iNavigationMouseOverViewID_left = -1;
 	private int iNavigationMouseOverViewID_right = -1;
 	private int iNavigationMouseOverViewID_out = -1;
 	private int iNavigationMouseOverViewID_in = -1;
 	private int iNavigationMouseOverViewID_lock = -1;
 
 	private boolean bEnableNavigationOverlay = false;
 
 	// Werner Puff, 2009-03-26
 	// replaced initNewPathway by several more generic methods, that work with different kind of views
 	// private ArrayList<Integer> iAlUninitializedPathwayIDs;
 
 	private TextRenderer textRenderer;
 
 	private GLDragAndDrop dragAndDrop;
 
 	private ARemoteViewLayoutRenderStyle layoutRenderStyle;
 
 	private BucketMouseWheelListener bucketMouseWheelListener;
 
 	// private GLColorMappingBarMiniView colorMappingBarMiniView;
 
 	private ArrayList<Integer> containedViewIDs;
 
 	/**
 	 * The current view in which the user is performing actions.
 	 */
 	private int iActiveViewID = -1;
 
 	// private int iGLDisplayList;
 
 	private ISelectionDelta lastSelectionDelta;
 
 	/**
 	 * Used for dragging views to the pool area.
 	 */
 	private int iPoolLevelCommonID = -1;
 
 	private GLOffScreenTextureRenderer glOffScreenRenderer;
 
 	private boolean bUpdateOffScreenTextures = true;
 
 	private boolean connectionLinesEnabled = true;
 
 	private boolean bRightMouseClickEventInvalid = false;
 
 	/** stores if the gene-mapping should be enabled */
 	private boolean geneMappingEnabled = true;
 
 	/** stores if the pathway textures are enabled */
 	private boolean pathwayTexturesEnabled = true;
 
 	/** stores if the "neighborhood" ist enabled */
 	private boolean neighborhoodEnabled = false;
 
 	private ArrayList<ASerializedView> newViews;
 
 	AddPathwayListener addPathwayListener = null;
 	LoadPathwaysByGeneListener loadPathwaysByGeneListener = null;
 
 	EnableGeneMappingListener enableGeneMappingListener = null;
 	DisableGeneMappingListener disableGeneMappingListener = null;
 
 	EnableTexturesListener enableTexturesListener = null;
 	DisableTexturesListener disableTexturesListener = null;
 
 	EnableNeighborhoodListener enableNeighborhoodListener = null;
 	DisableNeighborhoodListener disableNeighborhoodListener = null;
 	private GLInfoAreaManager infoAreaManager;
 
 	EnableConnectionLinesListener enableConnectionLinesListener = null;
 	DisableConnectionLinesListener disableConnectionLinesListener = null;
 
 	CloseOrResetViewsListener closeOrResetViewsListener = null;
 
 	/**
 	 * Constructor.
 	 */
 	public GLRemoteRendering(final int iGLCanvasID, final String sLabel, final IViewFrustum viewFrustum,
 		final ARemoteViewLayoutRenderStyle.LayoutMode layoutMode) {
 		super(iGLCanvasID, sLabel, viewFrustum, true);
 		viewType = EManagedObjectType.GL_REMOTE_RENDERING;
 		this.layoutMode = layoutMode;
 
 		if (generalManager.isWiiModeActive()) {
 			glOffScreenRenderer = new GLOffScreenTextureRenderer();
 		}
 
 		if (layoutMode.equals(ARemoteViewLayoutRenderStyle.LayoutMode.BUCKET)) {
 			layoutRenderStyle = new BucketLayoutRenderStyle(viewFrustum);
 			super.renderStyle = layoutRenderStyle;
 
 			bucketMouseWheelListener =
 				new BucketMouseWheelListener(this, (BucketLayoutRenderStyle) layoutRenderStyle);
 
 			// Unregister standard mouse wheel listener
 			parentGLCanvas.removeMouseWheelListener(pickingTriggerMouseAdapter);
 			// Register specialized bucket mouse wheel listener
 			parentGLCanvas.addMouseWheelListener(bucketMouseWheelListener);
 			// parentGLCanvas.addMouseListener(bucketMouseWheelListener);
 
 		}
 		else if (layoutMode.equals(ARemoteViewLayoutRenderStyle.LayoutMode.JUKEBOX)) {
 			layoutRenderStyle = new JukeboxLayoutRenderStyle(viewFrustum);
 		}
 		else if (layoutMode.equals(ARemoteViewLayoutRenderStyle.LayoutMode.LIST)) {
 			layoutRenderStyle = new ListLayoutRenderStyle(viewFrustum);
 		}
 
 		focusLevel = layoutRenderStyle.initFocusLevel();
 
 		if (GeneralManager.get().isWiiModeActive()
 			&& layoutMode.equals(ARemoteViewLayoutRenderStyle.LayoutMode.BUCKET)) {
 			stackLevel = ((BucketLayoutRenderStyle) layoutRenderStyle).initStackLevelWii();
 		}
 		else {
 			stackLevel = layoutRenderStyle.initStackLevel(bucketMouseWheelListener.isZoomedIn());
 		}
 
 		poolLevel = layoutRenderStyle.initPoolLevel(bucketMouseWheelListener.isZoomedIn(), -1);
 		externalSelectionLevel = layoutRenderStyle.initMemoLevel();
 		transitionLevel = layoutRenderStyle.initTransitionLevel();
 		spawnLevel = layoutRenderStyle.initSpawnLevel();
 
 		if (layoutMode.equals(ARemoteViewLayoutRenderStyle.LayoutMode.BUCKET)) {
 			glConnectionLineRenderer = new GLConnectionLineRendererBucket(focusLevel, stackLevel, poolLevel);
 		}
 		else if (layoutMode.equals(ARemoteViewLayoutRenderStyle.LayoutMode.JUKEBOX)) {
 			glConnectionLineRenderer = new GLConnectionLineRendererJukebox(focusLevel, stackLevel, poolLevel);
 		}
 		else if (layoutMode.equals(ARemoteViewLayoutRenderStyle.LayoutMode.LIST)) {
 			glConnectionLineRenderer = null;
 		}
 
 		pickingTriggerMouseAdapter.addGLCanvas(this);
 
 		arSlerpActions = new ArrayList<SlerpAction>();
 
 		// iAlUninitializedPathwayIDs = new ArrayList<Integer>();
 		newViews = new ArrayList<ASerializedView>();
 
 		createEventMediator();
 
 		dragAndDrop = new GLDragAndDrop();
 
 		textRenderer = new TextRenderer(new Font("Arial", Font.PLAIN, 24), false);
 
 		// TODO: the genome mapper should be stored centralized instead of newly
 		// created
 		// colorMappingBarMiniView = new GLColorMappingBarMiniView(viewFrustum);
 
 		createSelectionHeatMap();
 		// Registration to event system
 		IEventPublisher eventPublisher = generalManager.getEventPublisher();
 		eventPublisher.addSender(EMediatorType.SELECTION_MEDIATOR, this);
 		eventPublisher.addReceiver(EMediatorType.SELECTION_MEDIATOR, this);
 		eventPublisher.addSender(EMediatorType.VIEW_SELECTION, this);
 		registerEventListeners();
 
 		iPoolLevelCommonID = generalManager.getIDManager().createID(EManagedObjectType.REMOTE_LEVEL_ELEMENT);
 	}
 
 	@Override
 	public void initLocal(final GL gl) {
 		// iGLDisplayList = gl.glGenLists(1);
 
 		init(gl);
 	}
 
 	@Override
 	public void initRemote(final GL gl, final int iRemoteViewID,
 		final PickingMouseListener pickingTriggerMouseAdapter,
 		final IGLCanvasRemoteRendering remoteRenderingGLCanvas, GLInfoAreaManager infoAreaManager) {
 
 		throw new IllegalStateException("Not implemented to be rendered remote");
 	}
 
 	@Override
 	public void init(final GL gl) {
 		gl.glClearColor(0.5f, 0.5f, 0.5f, 1f);
 
 		if (glConnectionLineRenderer != null) {
 			glConnectionLineRenderer.init(gl);
 		}
 
 		// iconTextureManager = new GLIconTextureManager(gl);
 
 		time = new SystemTime();
 		((SystemTime) time).rebase();
 
 		infoAreaManager = new GLInfoAreaManager();
 		infoAreaManager.initInfoInPlace(viewFrustum);
 
 		initializeContainedViews(gl);
 
 		externalSelectionLevel.getElementByPositionIndex(0).setContainedElementID(glSelectionHeatMap.getID());
 
 		glSelectionHeatMap.addSets(alSets);
 		glSelectionHeatMap.initRemote(gl, getID(), pickingTriggerMouseAdapter, remoteRenderingGLCanvas, null);
 
 		if (generalManager.isWiiModeActive())
 			glOffScreenRenderer.init(gl);
 	}
 
 	private void createSelectionHeatMap() {
 		// Create selection panel
 		CmdCreateGLEventListener cmdCreateGLView =
 			(CmdCreateGLEventListener) generalManager.getCommandManager().createCommandByType(
 				ECommandType.CREATE_GL_HEAT_MAP_3D);
 		cmdCreateGLView.setAttributes(EProjectionMode.ORTHOGRAPHIC, 0, 0.8f, 0.1f, 4.1f, -20, 20, null, -1);
 		cmdCreateGLView.doCommand();
 		glSelectionHeatMap = (GLHeatMap) cmdCreateGLView.getCreatedObject();
 		glSelectionHeatMap.setToListMode(true);
 
 		generalManager.getEventPublisher()
 			.addReceiver(EMediatorType.PROPAGATION_MEDIATOR, glSelectionHeatMap);
 		generalManager.getEventPublisher().addReceiver(EMediatorType.SELECTION_MEDIATOR, glSelectionHeatMap);
 		generalManager.getEventPublisher().addSender(EMediatorType.SELECTION_MEDIATOR, glSelectionHeatMap);
 	}
 
 	@Override
 	public synchronized void displayLocal(final GL gl) {
 		if (pickingTriggerMouseAdapter.wasRightMouseButtonPressed() && !bucketMouseWheelListener.isZoomedIn()
 			&& !bRightMouseClickEventInvalid && !(layoutRenderStyle instanceof ListLayoutRenderStyle)) {
 			bEnableNavigationOverlay = !bEnableNavigationOverlay;
 
 			bRightMouseClickEventInvalid = true;
 
 			if (glConnectionLineRenderer != null) {
 				glConnectionLineRenderer.enableRendering(!bEnableNavigationOverlay);
 			}
 		}
 		else if (pickingTriggerMouseAdapter.wasMouseReleased()) {
 			bRightMouseClickEventInvalid = false;
 		}
 
 		pickingManager.handlePicking(iUniqueID, gl);
 
 		// if (bIsDisplayListDirtyLocal)
 		// {
 		// buildDisplayList(gl);
 		// bIsDisplayListDirtyLocal = false;
 		// }
 
 		display(gl);
 
 		if (eBusyModeState != EBusyModeState.OFF) {
 			renderBusyMode(gl);
 		}
 
 		if (pickingTriggerMouseAdapter.getPickedPoint() != null) {
 			dragAndDrop.setCurrentMousePos(gl, pickingTriggerMouseAdapter.getPickedPoint());
 		}
 
 		if (dragAndDrop.isDragActionRunning()) {
 			dragAndDrop.renderDragThumbnailTexture(gl);
 		}
 
 		if (pickingTriggerMouseAdapter.wasMouseReleased() && dragAndDrop.isDragActionRunning()) {
 			int iDraggedObjectId = dragAndDrop.getDraggedObjectedId();
 
 			// System.out.println("over: " +iExternalID);
 			// System.out.println("dragged: " +iDraggedObjectId);
 
 			// Prevent user from dragging element onto selection level
 			if (!RemoteElementManager.get().hasItem(iMouseOverObjectID)
 				|| !externalSelectionLevel.containsElement(RemoteElementManager.get().getItem(
 					iMouseOverObjectID))) {
 				RemoteLevelElement mouseOverElement = null;
 
 				// Check if a drag and drop action is performed onto the pool
 				// level
 				if (iMouseOverObjectID == iPoolLevelCommonID) {
 					mouseOverElement = poolLevel.getNextFree();
 				}
 				else if (mouseOverElement == null && iMouseOverObjectID != iDraggedObjectId) {
 					mouseOverElement = RemoteElementManager.get().getItem(iMouseOverObjectID);
 				}
 
 				if (mouseOverElement != null) {
 					RemoteLevelElement originElement = RemoteElementManager.get().getItem(iDraggedObjectId);
 
 					int iMouseOverElementID = mouseOverElement.getContainedElementID();
 					int iOriginElementID = originElement.getContainedElementID();
 
 					mouseOverElement.setContainedElementID(iOriginElementID);
 					originElement.setContainedElementID(iMouseOverElementID);
 
 					IViewManager viewGLCanvasManager = generalManager.getViewGLCanvasManager();
 
 					AGLEventListener originView = viewGLCanvasManager.getGLEventListener(iOriginElementID);
 					if (originView != null) {
 						originView.setRemoteLevelElement(mouseOverElement);
 					}
 
 					AGLEventListener mouseOverView =
 						viewGLCanvasManager.getGLEventListener(iMouseOverElementID);
 					if (mouseOverView != null) {
 						mouseOverView.setRemoteLevelElement(originElement);
 					}
 
 					updateViewDetailLevels(originElement);
 					updateViewDetailLevels(mouseOverElement);
 
 					if (mouseOverElement.getContainedElementID() != -1) {
 						if (poolLevel.containsElement(originElement)
 							&& (stackLevel.containsElement(mouseOverElement) || focusLevel
 								.containsElement(mouseOverElement))) {
 							generalManager.getViewGLCanvasManager().getGLEventListener(
 								mouseOverElement.getContainedElementID()).broadcastElements(
 								EVAOperation.APPEND_UNIQUE);
 						}
 
 						if (poolLevel.containsElement(mouseOverElement)
 							&& (stackLevel.containsElement(originElement) || focusLevel
 								.containsElement(originElement))) {
 							generalManager.getViewGLCanvasManager().getGLEventListener(
 								mouseOverElement.getContainedElementID()).broadcastElements(
 								EVAOperation.REMOVE_ELEMENT);
 						}
 					}
 				}
 			}
 
 			dragAndDrop.stopDragAction();
 			bUpdateOffScreenTextures = true;
 		}
 
 		checkForHits(gl);
 
 		// gl.glCallList(iGLDisplayListIndexLocal);
 	}
 
 	@Override
 	public synchronized void displayRemote(final GL gl) {
 		display(gl);
 	}
 
 	@Override
 	public synchronized void display(final GL gl) {
 		time.update();
 		// Update the pool transformations according to the current mouse over object
 		layoutRenderStyle.initPoolLevel(false, iMouseOverObjectID);
 		layoutRenderStyle.initFocusLevel();
 
 		// Just for layout testing during runtime
 		// layoutRenderStyle.initStackLevel(false);
 		// layoutRenderStyle.initMemoLevel();
 
 		if (GeneralManager.get().isWiiModeActive()
 			&& layoutMode.equals(ARemoteViewLayoutRenderStyle.LayoutMode.BUCKET)) {
 			((BucketLayoutRenderStyle) layoutRenderStyle).initFocusLevelWii();
 
 			((BucketLayoutRenderStyle) layoutRenderStyle).initStackLevelWii();
 		}
 
 		doSlerpActions(gl);
 		initNewView(gl);
 
 		if (!generalManager.isWiiModeActive()) {
 			renderRemoteLevel(gl, focusLevel);
 			renderRemoteLevel(gl, stackLevel);
 		}
 		else {
 			if (bUpdateOffScreenTextures) {
 				updateOffScreenTextures(gl);
 			}
 
 			renderRemoteLevel(gl, focusLevel);
 
 			glOffScreenRenderer.renderRubberBucket(gl, stackLevel,
 				(BucketLayoutRenderStyle) layoutRenderStyle, this);
 		}
 
 		// If user zooms to the bucket bottom all but the under
 		// focus layer is _not_ rendered.
 		if (bucketMouseWheelListener == null || !bucketMouseWheelListener.isZoomedIn()) {
 
 			renderPoolAndMemoLayerBackground(gl);
 
 			renderRemoteLevel(gl, transitionLevel);
 			renderRemoteLevel(gl, spawnLevel);
 			renderRemoteLevel(gl, poolLevel);
 			renderRemoteLevel(gl, externalSelectionLevel);
 		}
 
 		if (layoutMode.equals(ARemoteViewLayoutRenderStyle.LayoutMode.BUCKET)) {
 			bucketMouseWheelListener.render();
 		}
 
 		// colorMappingBarMiniView.render(gl,
 		// layoutRenderStyle.getColorBarXPos(),
 		// layoutRenderStyle.getColorBarYPos(), 4);
 
 		renderHandles(gl);
 
 		// gl.glCallList(iGLDisplayList);
 
 		// comment here for connection lines
 		gl.glDisable(GL.GL_DEPTH_TEST);
 		if (glConnectionLineRenderer != null && connectionLinesEnabled) {
 			glConnectionLineRenderer.render(gl);
 		}
 		gl.glEnable(GL.GL_DEPTH_TEST);
 
 		// System.out.println(size.height + " - " + size.width);
 		// GLHelperFunctions.drawViewFrustum(gl, viewFrustum);
 
 		// GLHelperFunctions.drawPointAt(gl, new Vec3f(0,0,0));
		// infoAreaManager.renderRemoteInPlaceInfo(gl, size.width, size.height, left, right, bottom, top);
 		// infoAreaManager.renderInPlaceInfo(gl);
 		// viewFrustum.setBottom(-4);
 		// viewFrustum.setTop(+4);
 		// viewFrustum.setLeft(-4);
 		// viewFrustum.setRight(4);
 		// GLHelperFunctions.drawPointAt(gl, new Vec3f(0, 0, 4));
 
		// Dimension size = getParentGLCanvas().getSize();
		// infoAreaManager.renderRemoteInPlaceInfo(gl, size.width, size.height, viewFrustum);
 		//		
 		// GLHelperFunctions.drawPointAt(gl, new Vec3f(1, 1, 4));
 		// GLHelperFunctions.drawPointAt(gl, new Vec3f(1, -1, 4));
 		// GLHelperFunctions.drawPointAt(gl, new Vec3f(-1, -1, 4));
 		// GLHelperFunctions.drawPointAt(gl, new Vec3f(-1, 1, 4));
 
 	}
 
 	public synchronized void setInitialContainedViews(ArrayList<Integer> iAlInitialContainedViewIDs) {
 		containedViewIDs = iAlInitialContainedViewIDs;
 	}
 
 	private void initializeContainedViews(final GL gl) {
 		if (containedViewIDs == null)
 			return;
 
 		for (int iContainedViewID : containedViewIDs) {
 			AGLEventListener tmpGLEventListener =
 				generalManager.getViewGLCanvasManager().getGLEventListener(iContainedViewID);
 
 			// Ignore pathway views upon startup
 			// because they will be activated when pathway loader thread has
 			// finished
 			if (tmpGLEventListener == this || tmpGLEventListener instanceof GLPathway) {
 				continue;
 			}
 
 			int iViewID = tmpGLEventListener.getID();
 
 			if (focusLevel.hasFreePosition()) {
 				RemoteLevelElement element = focusLevel.getNextFree();
 				element.setContainedElementID(iViewID);
 
 				tmpGLEventListener.initRemote(gl, iUniqueID, pickingTriggerMouseAdapter, this,
 					infoAreaManager);
 
 				tmpGLEventListener.broadcastElements(EVAOperation.APPEND_UNIQUE);
 				tmpGLEventListener.setDetailLevel(EDetailLevel.MEDIUM);
 				tmpGLEventListener.setRemoteLevelElement(element);
 
 				// generalManager.getGUIBridge().setActiveGLSubView(this,
 				// tmpGLEventListener);
 
 			}
 			else if (stackLevel.hasFreePosition() && !(layoutRenderStyle instanceof ListLayoutRenderStyle)) {
 				RemoteLevelElement element = stackLevel.getNextFree();
 				element.setContainedElementID(iViewID);
 
 				tmpGLEventListener.initRemote(gl, iUniqueID, pickingTriggerMouseAdapter, this,
 					infoAreaManager);
 
 				tmpGLEventListener.broadcastElements(EVAOperation.APPEND_UNIQUE);
 				tmpGLEventListener.setDetailLevel(EDetailLevel.LOW);
 				tmpGLEventListener.setRemoteLevelElement(element);
 			}
 			else if (poolLevel.hasFreePosition()) {
 				RemoteLevelElement element = poolLevel.getNextFree();
 				element.setContainedElementID(iViewID);
 
 				tmpGLEventListener.initRemote(gl, iUniqueID, pickingTriggerMouseAdapter, this,
 					infoAreaManager);
 				tmpGLEventListener.setDetailLevel(EDetailLevel.VERY_LOW);
 				tmpGLEventListener.setRemoteLevelElement(element);
 			}
 
 			// pickingTriggerMouseAdapter.addGLCanvas(tmpGLEventListener);
 			pickingManager.getPickingID(iUniqueID, EPickingType.VIEW_SELECTION, iViewID);
 
 			generalManager.getEventPublisher().addSender(EMediatorType.SELECTION_MEDIATOR,
 				(IMediatorSender) tmpGLEventListener);
 			generalManager.getEventPublisher().addReceiver(EMediatorType.SELECTION_MEDIATOR,
 				(IMediatorReceiver) tmpGLEventListener);
 		}
 	}
 
 	public void renderBucketWall(final GL gl, boolean bRenderBorder, RemoteLevelElement element) {
 		// Highlight potential view drop destination
 		if (dragAndDrop.isDragActionRunning() && element.getID() == iMouseOverObjectID) {
 			gl.glLineWidth(5);
 			gl.glColor4f(0.2f, 0.2f, 0.2f, 1);
 			gl.glBegin(GL.GL_LINE_LOOP);
 			gl.glVertex3f(0, 0, 0.01f);
 			gl.glVertex3f(0, 8, 0.01f);
 			gl.glVertex3f(8, 8, 0.01f);
 			gl.glVertex3f(8, 0, 0.01f);
 			gl.glEnd();
 		}
 
 		if (arSlerpActions.isEmpty()) {
 			gl.glColor4f(1f, 1f, 1f, 1.0f); // normal mode
 		}
 		else {
 			gl.glColor4f(1f, 1f, 1f, 0.3f);
 		}
 
 		if (!newViews.isEmpty()) {
 			gl.glColor4f(1f, 1f, 1f, 0.3f);
 		}
 
 		gl.glBegin(GL.GL_POLYGON);
 		gl.glVertex3f(0, 0, -0.03f);
 		gl.glVertex3f(0, 8, -0.03f);
 		gl.glVertex3f(8, 8, -0.03f);
 		gl.glVertex3f(8, 0, -0.03f);
 		gl.glEnd();
 
 		if (!bRenderBorder)
 			return;
 
 		gl.glColor4f(0.4f, 0.4f, 0.4f, 1f);
 		gl.glLineWidth(1f);
 
 		// gl.glBegin(GL.GL_LINES);
 		// gl.glVertex3f(0, 0, -0.02f);
 		// gl.glVertex3f(0, 8, -0.02f);
 		// gl.glVertex3f(8, 8, -0.02f);
 		// gl.glVertex3f(8, 0, -0.02f);
 		// gl.glEnd();
 	}
 
 	private void renderRemoteLevel(final GL gl, final RemoteLevel level) {
 		for (RemoteLevelElement element : level.getAllElements()) {
 			renderRemoteLevelElement(gl, element, level);
 
 			if (!(layoutRenderStyle instanceof ListLayoutRenderStyle)) {
 				renderEmptyBucketWall(gl, element, level);
 			}
 		}
 	}
 
 	private void renderRemoteLevelElement(final GL gl, RemoteLevelElement element, RemoteLevel level) {
 		// // Check if view is visible
 		// if (!level.getElementVisibilityById(iViewID))
 		// return;
 
 		if (element.getContainedElementID() == -1)
 			return;
 
 		int iViewID = element.getContainedElementID();
 
 		gl.glPushName(pickingManager.getPickingID(iUniqueID, EPickingType.REMOTE_LEVEL_ELEMENT, element
 			.getID()));
 		gl.glPushName(pickingManager.getPickingID(iUniqueID, EPickingType.VIEW_SELECTION, iViewID));
 
 		AGLEventListener glEventListener =
 			generalManager.getViewGLCanvasManager().getGLEventListener(iViewID);
 
 		if (glEventListener == null)
 			throw new IllegalStateException("Cannot render canvas object which is null!");
 
 		gl.glPushMatrix();
 
 		Transform transform = element.getTransform();
 		Vec3f translation = transform.getTranslation();
 		Rotf rot = transform.getRotation();
 		Vec3f scale = transform.getScale();
 		Vec3f axis = new Vec3f();
 		float fAngle = rot.get(axis);
 
 		gl.glTranslatef(translation.x(), translation.y(), translation.z());
 		gl.glRotatef(Vec3f.convertRadiant2Grad(fAngle), axis.x(), axis.y(), axis.z());
 		gl.glScalef(scale.x(), scale.y(), scale.z());
 
 		if (level == poolLevel) {
 			String sRenderText = glEventListener.getShortInfo();
 
 			// Limit pathway name in length
 			int iMaxChars;
 			if (layoutRenderStyle instanceof ListLayoutRenderStyle) {
 				iMaxChars = 80;
 			}
 			else {
 				iMaxChars = 20;
 			}
 
 			if (sRenderText.length() > iMaxChars && scale.x() < 0.03f) {
 				sRenderText = sRenderText.subSequence(0, iMaxChars - 3) + "...";
 			}
 
 			float fTextScalingFactor = 0.09f;
 			float fTextXPosition = 0f;
 
 			if (element.getID() == iMouseOverObjectID) {
 				renderPoolSelection(gl, translation.x() - 0.4f / fAspectRatio, translation.y() * scale.y()
 					+ 5.2f,
 
 				(float) textRenderer.getBounds(sRenderText).getWidth() * 0.06f + 23, 6f, element); // 1.8f
 				// ->
 				// pool
 				// focus
 				// scaling
 
 				gl.glTranslatef(0.8f, 1.3f, 0);
 
 				fTextScalingFactor = 0.075f;
 				fTextXPosition = 12f;
 			}
 			else {
 				// Render view background frame
 				Texture tempTexture =
 					iconTextureManager.getIconTexture(gl, EIconTextures.POOL_VIEW_BACKGROUND);
 				tempTexture.enable();
 				tempTexture.bind();
 
 				float fFrameWidth = 9.5f;
 				TextureCoords texCoords = tempTexture.getImageTexCoords();
 
 				gl.glColor4f(1, 1, 1, 0.75f);
 
 				gl.glBegin(GL.GL_POLYGON);
 				gl.glTexCoord2f(texCoords.left(), texCoords.bottom());
 				gl.glVertex3f(-0.7f, -0.6f + fFrameWidth, -0.01f);
 				gl.glTexCoord2f(texCoords.left(), texCoords.top());
 				gl.glVertex3f(-0.7f + fFrameWidth, -0.6f + fFrameWidth, -0.01f);
 				gl.glTexCoord2f(texCoords.right(), texCoords.top());
 				gl.glVertex3f(-0.7f + fFrameWidth, -0.6f, -0.01f);
 				gl.glTexCoord2f(texCoords.right(), texCoords.bottom());
 				gl.glVertex3f(-0.7f, -0.6f, -0.01f);
 				gl.glEnd();
 
 				tempTexture.disable();
 
 				fTextXPosition = 9.5f;
 			}
 
 			int iNumberOfGenesSelected = glEventListener.getNumberOfSelections(ESelectionType.SELECTION);
 			int iNumberOfGenesMouseOver = glEventListener.getNumberOfSelections(ESelectionType.MOUSE_OVER);
 
 			textRenderer.begin3DRendering();
 
 			if (element.getID() == iMouseOverObjectID) {
 				textRenderer.setColor(1, 1, 1, 1);
 			}
 			else {
 				textRenderer.setColor(0, 0, 0, 1);
 			}
 
 			if (iNumberOfGenesMouseOver == 0 && iNumberOfGenesSelected == 0) {
 				textRenderer.draw3D(sRenderText, fTextXPosition, 3f, 0, fTextScalingFactor);
 			}
 			else {
 				textRenderer.draw3D(sRenderText, fTextXPosition, 4.5f, 0, fTextScalingFactor);
 			}
 
 			textRenderer.end3DRendering();
 
 			gl.glLineWidth(4);
 
 			if (element.getID() == iMouseOverObjectID) {
 				gl.glTranslatef(2.2f, 0.5f, 0);
 			}
 
 			if (iNumberOfGenesMouseOver > 0) {
 				if (element.getID() == iMouseOverObjectID) {
 					gl.glTranslatef(-2.5f, 0, 0);
 				}
 
 				textRenderer.begin3DRendering();
 				textRenderer.draw3D(Integer.toString(iNumberOfGenesMouseOver), fTextXPosition + 9, 2.4f, 0,
 					fTextScalingFactor);
 				textRenderer.end3DRendering();
 
 				if (element.getID() == iMouseOverObjectID) {
 					gl.glTranslatef(2.5f, 0, 0);
 				}
 
 				gl.glColor4fv(GeneralRenderStyle.MOUSE_OVER_COLOR, 0);
 				gl.glBegin(GL.GL_LINES);
 				gl.glVertex3f(10, 2.7f, 0f);
 				gl.glVertex3f(18, 2.7f, 0f);
 				gl.glVertex3f(20, 2.7f, 0f);
 				gl.glVertex3f(29, 2.7f, 0f);
 				gl.glEnd();
 			}
 
 			if (iNumberOfGenesSelected > 0) {
 				if (iNumberOfGenesMouseOver > 0) {
 					gl.glTranslatef(0, -1.8f, 0);
 				}
 
 				if (element.getID() == iMouseOverObjectID) {
 					gl.glTranslatef(-2.5f, 0, 0);
 				}
 
 				textRenderer.begin3DRendering();
 				textRenderer.draw3D(Integer.toString(iNumberOfGenesSelected), fTextXPosition + 9, 2.5f, 0,
 					fTextScalingFactor);
 				textRenderer.end3DRendering();
 
 				if (element.getID() == iMouseOverObjectID) {
 					gl.glTranslatef(2.5f, 0, 0);
 				}
 
 				gl.glColor4fv(GeneralRenderStyle.SELECTED_COLOR, 0);
 				gl.glBegin(GL.GL_LINES);
 				gl.glVertex3f(10, 2.9f, 0f);
 				gl.glVertex3f(18, 2.9f, 0f);
 				gl.glVertex3f(20, 2.9f, 0f);
 				gl.glVertex3f(29, 2.9f, 0f);
 				gl.glEnd();
 
 				if (iNumberOfGenesMouseOver > 0) {
 					gl.glTranslatef(0, 1.8f, 0);
 				}
 			}
 
 			if (element.getID() == iMouseOverObjectID) {
 				gl.glTranslatef(-2.2f, -0.5f, 0);
 			}
 		}
 
 		// Prevent rendering of view textures when simple list view
 		// if ((layoutRenderStyle instanceof ListLayoutRenderStyle
 		// && (layer == poolLayer || layer == stackLayer)))
 		// {
 		// gl.glPopMatrix();
 		// return;
 		// }
 
 		if (level != externalSelectionLevel && level != poolLevel) {
 			if (level.equals(focusLevel)) {
 				renderBucketWall(gl, false, element);
 			}
 			else {
 				renderBucketWall(gl, true, element);
 			}
 		}
 
 		if (!bEnableNavigationOverlay || !level.equals(stackLevel)) {
 			glEventListener.displayRemote(gl);
 		}
 		else {
 			renderNavigationOverlay(gl, element.getID());
 		}
 
 		gl.glPopMatrix();
 
 		gl.glPopName();
 		gl.glPopName();
 	}
 
 	private void renderEmptyBucketWall(final GL gl, RemoteLevelElement element, RemoteLevel level) {
 		gl.glPushMatrix();
 
 		gl.glPushName(pickingManager.getPickingID(iUniqueID, EPickingType.REMOTE_LEVEL_ELEMENT, element
 			.getID()));
 
 		Transform transform = element.getTransform();
 		Vec3f translation = transform.getTranslation();
 		Rotf rot = transform.getRotation();
 		Vec3f scale = transform.getScale();
 		Vec3f axis = new Vec3f();
 		float fAngle = rot.get(axis);
 
 		gl.glTranslatef(translation.x(), translation.y(), translation.z());
 		gl.glScalef(scale.x(), scale.y(), scale.z());
 		gl.glRotatef(Vec3f.convertRadiant2Grad(fAngle), axis.x(), axis.y(), axis.z());
 
 		if (!level.equals(transitionLevel) && !level.equals(spawnLevel) && !level.equals(poolLevel)
 			&& !level.equals(externalSelectionLevel)) {
 			renderBucketWall(gl, true, element);
 		}
 
 		gl.glPopName();
 
 		gl.glPopMatrix();
 	}
 
 	private void renderHandles(final GL gl) {
 		float fZoomedInScalingFactor = 0.4f;
 
 		// Bucket stack top
 		RemoteLevelElement element = stackLevel.getElementByPositionIndex(0);
 		if (element.getContainedElementID() != -1) {
 			if (!bucketMouseWheelListener.isZoomedIn()) {
 				gl.glTranslatef(-2, 0, 4.02f);
 				renderNavigationHandleBar(gl, element, 4, 0.075f, false, 1);
 				gl.glTranslatef(2, 0, -4.02f);
 			}
 			else {
 				gl.glTranslatef(-2 - 4 * fZoomedInScalingFactor, 0, 0.02f);
 				renderNavigationHandleBar(gl, element, 4 * fZoomedInScalingFactor, 0.075f, false,
 					1 / fZoomedInScalingFactor);
 				gl.glTranslatef(2 + 4 * fZoomedInScalingFactor, 0, -0.02f);
 			}
 		}
 
 		// Bucket stack bottom
 		element = stackLevel.getElementByPositionIndex(2);
 		if (element.getContainedElementID() != -1) {
 			if (!bucketMouseWheelListener.isZoomedIn()) {
 				gl.glTranslatef(-2, 0, 4.02f);
 				gl.glRotatef(180, 1, 0, 0);
 				renderNavigationHandleBar(gl, element, 4, 0.075f, true, 1);
 				gl.glRotatef(-180, 1, 0, 0);
 				gl.glTranslatef(2, 0, -4.02f);
 			}
 			else {
 				gl.glTranslatef(-2 - 4 * fZoomedInScalingFactor, -4 + 4 * fZoomedInScalingFactor, 0.02f);
 				renderNavigationHandleBar(gl, element, 4 * fZoomedInScalingFactor, 0.075f, false,
 					1 / fZoomedInScalingFactor);
 				gl.glTranslatef(2 + 4 * fZoomedInScalingFactor, +4 - 4 * fZoomedInScalingFactor, -0.02f);
 			}
 		}
 
 		// Bucket stack left
 		element = stackLevel.getElementByPositionIndex(1);
 		if (element.getContainedElementID() != -1) {
 			if (!bucketMouseWheelListener.isZoomedIn()) {
 				gl.glTranslatef(-2f / fAspectRatio + 2 + 0.8f, -2, 4.02f);
 				gl.glRotatef(90, 0, 0, 1);
 				renderNavigationHandleBar(gl, element, 4, 0.075f, false, 1);
 				gl.glRotatef(-90, 0, 0, 1);
 				gl.glTranslatef(2f / fAspectRatio - 2 - 0.8f, 2, -4.02f);
 			}
 			else {
 				gl.glTranslatef(2, 0, 0.02f);
 				renderNavigationHandleBar(gl, element, 4 * fZoomedInScalingFactor, 0.075f, false,
 					1 / fZoomedInScalingFactor);
 				gl.glTranslatef(-2, 0, -0.02f);
 			}
 		}
 
 		// Bucket stack right
 		element = stackLevel.getElementByPositionIndex(3);
 		if (element.getContainedElementID() != -1) {
 			if (!bucketMouseWheelListener.isZoomedIn()) {
 				gl.glTranslatef(2f / fAspectRatio - 0.8f - 2, 2, 4.02f);
 				gl.glRotatef(-90, 0, 0, 1);
 				renderNavigationHandleBar(gl, element, 4, 0.075f, false, 1);
 				gl.glRotatef(90, 0, 0, 1);
 				gl.glTranslatef(-2f / fAspectRatio + 0.8f + 2, -2, -4.02f);
 			}
 			else {
 				gl.glTranslatef(2, -4 + 4 * fZoomedInScalingFactor, 0.02f);
 				renderNavigationHandleBar(gl, element, 4 * fZoomedInScalingFactor, 0.075f, false,
 					1 / fZoomedInScalingFactor);
 				gl.glTranslatef(-2, +4 - 4 * fZoomedInScalingFactor, -0.02f);
 			}
 		}
 
 		// Bucket center
 		element = focusLevel.getElementByPositionIndex(0);
 		if (element.getContainedElementID() != -1) {
 			float fYCorrection = 0f;
 
 			if (!bucketMouseWheelListener.isZoomedIn()) {
 				fYCorrection = 0f;
 			}
 			else {
 				fYCorrection = 0.1f;
 			}
 
 			Transform transform = element.getTransform();
 			Vec3f translation = transform.getTranslation();
 
 			gl.glTranslatef(translation.x(), translation.y() - 2 * 0.075f + fYCorrection,
 				translation.z() + 0.001f);
 
 			gl.glScalef(2, 2, 2);
 			renderNavigationHandleBar(gl, element, 2, 0.075f, false, 2);
 			gl.glScalef(1 / 2f, 1 / 2f, 1 / 2f);
 
 			gl.glTranslatef(-translation.x(), -translation.y() + 2 * 0.075f - fYCorrection,
 				-translation.z() - 0.001f);
 		}
 	}
 
 	private void renderNavigationHandleBar(final GL gl, RemoteLevelElement element, float fHandleWidth,
 		float fHandleHeight, boolean bUpsideDown, float fScalingFactor) {
 		// Render icons
 		gl.glTranslatef(0, 2 + fHandleHeight, 0);
 		renderSingleHandle(gl, element.getID(), EPickingType.BUCKET_DRAG_ICON_SELECTION,
 			EIconTextures.NAVIGATION_DRAG_VIEW, fHandleHeight, fHandleHeight);
 		gl.glTranslatef(fHandleWidth - 2 * fHandleHeight, 0, 0);
 		if (bUpsideDown) {
 			gl.glRotatef(180, 1, 0, 0);
 			gl.glTranslatef(0, fHandleHeight, 0);
 		}
 		renderSingleHandle(gl, element.getID(), EPickingType.BUCKET_LOCK_ICON_SELECTION,
 			EIconTextures.NAVIGATION_LOCK_VIEW, fHandleHeight, fHandleHeight);
 		if (bUpsideDown) {
 			gl.glTranslatef(0, -fHandleHeight, 0);
 			gl.glRotatef(-180, 1, 0, 0);
 		}
 		gl.glTranslatef(fHandleHeight, 0, 0);
 		renderSingleHandle(gl, element.getID(), EPickingType.BUCKET_REMOVE_ICON_SELECTION,
 			EIconTextures.NAVIGATION_REMOVE_VIEW, fHandleHeight, fHandleHeight);
 		gl.glTranslatef(-fHandleWidth + fHandleHeight, -2 - fHandleHeight, 0);
 
 		// Render background (also draggable)
 		gl.glPushName(pickingManager.getPickingID(iUniqueID, EPickingType.BUCKET_DRAG_ICON_SELECTION, element
 			.getID()));
 		gl.glColor3f(0.25f, 0.25f, 0.25f);
 		gl.glBegin(GL.GL_POLYGON);
 		gl.glVertex3f(0 + fHandleHeight, 2 + fHandleHeight, 0);
 		gl.glVertex3f(fHandleWidth - 2 * fHandleHeight, 2 + fHandleHeight, 0);
 		gl.glVertex3f(fHandleWidth - 2 * fHandleHeight, 2, 0);
 		gl.glVertex3f(0 + fHandleHeight, 2, 0);
 		gl.glEnd();
 
 		gl.glPopName();
 
 		// Render view information
 		String sText =
 			generalManager.getViewGLCanvasManager().getGLEventListener(element.getContainedElementID())
 				.getShortInfo();
 
 		int iMaxChars = 50;
 		if (sText.length() > iMaxChars) {
 			sText = sText.subSequence(0, iMaxChars - 3) + "...";
 		}
 
 		float fTextScalingFactor = 0.0027f;
 
 		if (bUpsideDown) {
 			gl.glRotatef(180, 1, 0, 0);
 			gl.glTranslatef(0, -4 - fHandleHeight, 0);
 		}
 
 		textRenderer.setColor(0.7f, 0.7f, 0.7f, 1);
 		textRenderer.begin3DRendering();
 		textRenderer.draw3D(sText, 2 / fScalingFactor - (float) textRenderer.getBounds(sText).getWidth() / 2f
 			* fTextScalingFactor, 2.02f, 0f, fTextScalingFactor);
 		textRenderer.end3DRendering();
 
 		if (bUpsideDown) {
 			gl.glTranslatef(0, 4 + fHandleHeight, 0);
 			gl.glRotatef(-180, 1, 0, 0);
 		}
 	}
 
 	private void renderSingleHandle(final GL gl, int iRemoteLevelElementID, EPickingType ePickingType,
 		EIconTextures eIconTexture, float fWidth, float fHeight) {
 		gl.glPushName(pickingManager.getPickingID(iUniqueID, ePickingType, iRemoteLevelElementID));
 
 		Texture tempTexture = iconTextureManager.getIconTexture(gl, eIconTexture);
 		tempTexture.enable();
 		tempTexture.bind();
 
 		TextureCoords texCoords = tempTexture.getImageTexCoords();
 		gl.glColor3f(1, 1, 1);
 		gl.glBegin(GL.GL_POLYGON);
 		gl.glTexCoord2f(texCoords.left(), texCoords.bottom());
 		gl.glVertex3f(0, -fHeight, 0f);
 		gl.glTexCoord2f(texCoords.left(), texCoords.top());
 		gl.glVertex3f(0, 0, 0f);
 		gl.glTexCoord2f(texCoords.right(), texCoords.top());
 		gl.glVertex3f(fWidth, 0, 0f);
 		gl.glTexCoord2f(texCoords.right(), texCoords.bottom());
 		gl.glVertex3f(fWidth, -fHeight, 0f);
 		gl.glEnd();
 
 		tempTexture.disable();
 
 		gl.glPopName();
 	}
 
 	private void renderNavigationOverlay(final GL gl, final int iRemoteLevelElementID) {
 		if (glConnectionLineRenderer != null) {
 			glConnectionLineRenderer.enableRendering(false);
 		}
 
 		RemoteLevelElement remoteLevelElement = RemoteElementManager.get().getItem(iRemoteLevelElementID);
 
 		EPickingType leftWallPickingType = null;
 		EPickingType rightWallPickingType = null;
 		EPickingType topWallPickingType = null;
 		EPickingType bottomWallPickingType = null;
 
 		Vec4f tmpColor_out =
 			new Vec4f(0.9f, 0.9f, 0.9f, ARemoteViewLayoutRenderStyle.NAVIGATION_OVERLAY_TRANSPARENCY);
 		Vec4f tmpColor_in =
 			new Vec4f(0.9f, 0.9f, 0.9f, ARemoteViewLayoutRenderStyle.NAVIGATION_OVERLAY_TRANSPARENCY);
 		Vec4f tmpColor_left =
 			new Vec4f(0.9f, 0.9f, 0.9f, ARemoteViewLayoutRenderStyle.NAVIGATION_OVERLAY_TRANSPARENCY);
 		Vec4f tmpColor_right =
 			new Vec4f(0.9f, 0.9f, 0.9f, ARemoteViewLayoutRenderStyle.NAVIGATION_OVERLAY_TRANSPARENCY);
 		Vec4f tmpColor_lock =
 			new Vec4f(0.9f, 0.9f, 0.9f, ARemoteViewLayoutRenderStyle.NAVIGATION_OVERLAY_TRANSPARENCY);
 
 		// Assign view symbol
 		Texture textureViewSymbol;
 		AGLEventListener view =
 			generalManager.getViewGLCanvasManager().getGLEventListener(
 				remoteLevelElement.getContainedElementID());
 		if (view instanceof GLHeatMap) {
 			textureViewSymbol = iconTextureManager.getIconTexture(gl, EIconTextures.HEAT_MAP_SYMBOL);
 		}
 		else if (view instanceof GLParallelCoordinates) {
 			textureViewSymbol = iconTextureManager.getIconTexture(gl, EIconTextures.PAR_COORDS_SYMBOL);
 		}
 		else if (view instanceof GLPathway) {
 			textureViewSymbol = iconTextureManager.getIconTexture(gl, EIconTextures.PATHWAY_SYMBOL);
 		}
 		else if (view instanceof GLGlyph) {
 			textureViewSymbol = iconTextureManager.getIconTexture(gl, EIconTextures.GLYPH_SYMBOL);
 		}
 		else if (view instanceof GLCell) {
 			textureViewSymbol = iconTextureManager.getIconTexture(gl, EIconTextures.GLYPH_SYMBOL);
 		}
 		else
 			throw new IllegalStateException("Unknown view that has no symbol assigned.");
 
 		Texture textureMoveLeft = null;
 		Texture textureMoveRight = null;
 		Texture textureMoveOut = null;
 		Texture textureMoveIn = null;
 
 		TextureCoords texCoords = textureViewSymbol.getImageTexCoords();
 
 		if (iNavigationMouseOverViewID_lock == iRemoteLevelElementID) {
 			tmpColor_lock.set(1, 0.3f, 0.3f, ARemoteViewLayoutRenderStyle.NAVIGATION_OVERLAY_TRANSPARENCY);
 		}
 
 		if (layoutMode.equals(LayoutMode.JUKEBOX)) {
 			topWallPickingType = EPickingType.BUCKET_MOVE_RIGHT_ICON_SELECTION;
 			bottomWallPickingType = EPickingType.BUCKET_MOVE_LEFT_ICON_SELECTION;
 			leftWallPickingType = EPickingType.BUCKET_MOVE_OUT_ICON_SELECTION;
 			rightWallPickingType = EPickingType.BUCKET_MOVE_IN_ICON_SELECTION;
 
 			if (iNavigationMouseOverViewID_out == iRemoteLevelElementID) {
 				tmpColor_left
 					.set(1, 0.3f, 0.3f, ARemoteViewLayoutRenderStyle.NAVIGATION_OVERLAY_TRANSPARENCY);
 			}
 			else if (iNavigationMouseOverViewID_in == iRemoteLevelElementID) {
 				tmpColor_right.set(1, 0.3f, 0.3f,
 					ARemoteViewLayoutRenderStyle.NAVIGATION_OVERLAY_TRANSPARENCY);
 			}
 			else if (iNavigationMouseOverViewID_left == iRemoteLevelElementID) {
 				tmpColor_in.set(1, 0.3f, 0.3f, ARemoteViewLayoutRenderStyle.NAVIGATION_OVERLAY_TRANSPARENCY);
 			}
 			else if (iNavigationMouseOverViewID_right == iRemoteLevelElementID) {
 				tmpColor_out.set(1, 0.3f, 0.3f, ARemoteViewLayoutRenderStyle.NAVIGATION_OVERLAY_TRANSPARENCY);
 			}
 
 			textureMoveIn = iconTextureManager.getIconTexture(gl, EIconTextures.ARROW_LEFT);
 			textureMoveOut = iconTextureManager.getIconTexture(gl, EIconTextures.ARROW_DOWN);
 			textureMoveLeft = iconTextureManager.getIconTexture(gl, EIconTextures.ARROW_DOWN);
 			textureMoveRight = iconTextureManager.getIconTexture(gl, EIconTextures.ARROW_LEFT);
 		}
 		else {
 			if (stackLevel.getPositionIndexByElementID(remoteLevelElement) == 0) // top
 			{
 				topWallPickingType = EPickingType.BUCKET_MOVE_OUT_ICON_SELECTION;
 				bottomWallPickingType = EPickingType.BUCKET_MOVE_IN_ICON_SELECTION;
 				leftWallPickingType = EPickingType.BUCKET_MOVE_LEFT_ICON_SELECTION;
 				rightWallPickingType = EPickingType.BUCKET_MOVE_RIGHT_ICON_SELECTION;
 
 				if (iNavigationMouseOverViewID_out == iRemoteLevelElementID) {
 					tmpColor_out.set(1, 0.3f, 0.3f,
 						ARemoteViewLayoutRenderStyle.NAVIGATION_OVERLAY_TRANSPARENCY);
 				}
 				else if (iNavigationMouseOverViewID_in == iRemoteLevelElementID) {
 					tmpColor_in.set(1, 0.3f, 0.3f,
 						ARemoteViewLayoutRenderStyle.NAVIGATION_OVERLAY_TRANSPARENCY);
 				}
 				else if (iNavigationMouseOverViewID_left == iRemoteLevelElementID) {
 					tmpColor_left.set(1, 0.3f, 0.3f,
 						ARemoteViewLayoutRenderStyle.NAVIGATION_OVERLAY_TRANSPARENCY);
 				}
 				else if (iNavigationMouseOverViewID_right == iRemoteLevelElementID) {
 					tmpColor_right.set(1, 0.3f, 0.3f,
 						ARemoteViewLayoutRenderStyle.NAVIGATION_OVERLAY_TRANSPARENCY);
 				}
 
 				textureMoveIn = iconTextureManager.getIconTexture(gl, EIconTextures.ARROW_LEFT);
 				textureMoveOut = iconTextureManager.getIconTexture(gl, EIconTextures.ARROW_DOWN);
 				textureMoveLeft = iconTextureManager.getIconTexture(gl, EIconTextures.ARROW_DOWN);
 				textureMoveRight = iconTextureManager.getIconTexture(gl, EIconTextures.ARROW_LEFT);
 			}
 			else if (stackLevel.getPositionIndexByElementID(remoteLevelElement) == 2) // bottom
 			{
 				topWallPickingType = EPickingType.BUCKET_MOVE_IN_ICON_SELECTION;
 				bottomWallPickingType = EPickingType.BUCKET_MOVE_OUT_ICON_SELECTION;
 				leftWallPickingType = EPickingType.BUCKET_MOVE_RIGHT_ICON_SELECTION;
 				rightWallPickingType = EPickingType.BUCKET_MOVE_LEFT_ICON_SELECTION;
 
 				if (iNavigationMouseOverViewID_out == iRemoteLevelElementID) {
 					tmpColor_in.set(1, 0.3f, 0.3f,
 						ARemoteViewLayoutRenderStyle.NAVIGATION_OVERLAY_TRANSPARENCY);
 				}
 				else if (iNavigationMouseOverViewID_in == iRemoteLevelElementID) {
 					tmpColor_out.set(1, 0.3f, 0.3f,
 						ARemoteViewLayoutRenderStyle.NAVIGATION_OVERLAY_TRANSPARENCY);
 				}
 				else if (iNavigationMouseOverViewID_left == iRemoteLevelElementID) {
 					tmpColor_right.set(1, 0.3f, 0.3f,
 						ARemoteViewLayoutRenderStyle.NAVIGATION_OVERLAY_TRANSPARENCY);
 				}
 				else if (iNavigationMouseOverViewID_right == iRemoteLevelElementID) {
 					tmpColor_left.set(1, 0.3f, 0.3f,
 						ARemoteViewLayoutRenderStyle.NAVIGATION_OVERLAY_TRANSPARENCY);
 				}
 
 				textureMoveIn = iconTextureManager.getIconTexture(gl, EIconTextures.ARROW_LEFT);
 				textureMoveOut = iconTextureManager.getIconTexture(gl, EIconTextures.ARROW_DOWN);
 				textureMoveLeft = iconTextureManager.getIconTexture(gl, EIconTextures.ARROW_DOWN);
 				textureMoveRight = iconTextureManager.getIconTexture(gl, EIconTextures.ARROW_LEFT);
 			}
 			else if (stackLevel.getPositionIndexByElementID(remoteLevelElement) == 1) // left
 			{
 				topWallPickingType = EPickingType.BUCKET_MOVE_RIGHT_ICON_SELECTION;
 				bottomWallPickingType = EPickingType.BUCKET_MOVE_LEFT_ICON_SELECTION;
 				leftWallPickingType = EPickingType.BUCKET_MOVE_OUT_ICON_SELECTION;
 				rightWallPickingType = EPickingType.BUCKET_MOVE_IN_ICON_SELECTION;
 
 				if (iNavigationMouseOverViewID_out == iRemoteLevelElementID) {
 					tmpColor_left.set(1, 0.3f, 0.3f,
 						ARemoteViewLayoutRenderStyle.NAVIGATION_OVERLAY_TRANSPARENCY);
 				}
 				else if (iNavigationMouseOverViewID_in == iRemoteLevelElementID) {
 					tmpColor_right.set(1, 0.3f, 0.3f,
 						ARemoteViewLayoutRenderStyle.NAVIGATION_OVERLAY_TRANSPARENCY);
 				}
 				else if (iNavigationMouseOverViewID_left == iRemoteLevelElementID) {
 					tmpColor_in.set(1, 0.3f, 0.3f,
 						ARemoteViewLayoutRenderStyle.NAVIGATION_OVERLAY_TRANSPARENCY);
 				}
 				else if (iNavigationMouseOverViewID_right == iRemoteLevelElementID) {
 					tmpColor_out.set(1, 0.3f, 0.3f,
 						ARemoteViewLayoutRenderStyle.NAVIGATION_OVERLAY_TRANSPARENCY);
 				}
 
 				textureMoveIn = iconTextureManager.getIconTexture(gl, EIconTextures.ARROW_LEFT);
 				textureMoveOut = iconTextureManager.getIconTexture(gl, EIconTextures.ARROW_DOWN);
 				textureMoveLeft = iconTextureManager.getIconTexture(gl, EIconTextures.ARROW_DOWN);
 				textureMoveRight = iconTextureManager.getIconTexture(gl, EIconTextures.ARROW_LEFT);
 			}
 			else if (stackLevel.getPositionIndexByElementID(remoteLevelElement) == 3) // right
 			{
 				topWallPickingType = EPickingType.BUCKET_MOVE_LEFT_ICON_SELECTION;
 				bottomWallPickingType = EPickingType.BUCKET_MOVE_RIGHT_ICON_SELECTION;
 				leftWallPickingType = EPickingType.BUCKET_MOVE_IN_ICON_SELECTION;
 				rightWallPickingType = EPickingType.BUCKET_MOVE_OUT_ICON_SELECTION;
 
 				if (iNavigationMouseOverViewID_out == iRemoteLevelElementID) {
 					tmpColor_right.set(1, 0.3f, 0.3f,
 						ARemoteViewLayoutRenderStyle.NAVIGATION_OVERLAY_TRANSPARENCY);
 				}
 				else if (iNavigationMouseOverViewID_in == iRemoteLevelElementID) {
 					tmpColor_left.set(1, 0.3f, 0.3f,
 						ARemoteViewLayoutRenderStyle.NAVIGATION_OVERLAY_TRANSPARENCY);
 				}
 				else if (iNavigationMouseOverViewID_left == iRemoteLevelElementID) {
 					tmpColor_out.set(1, 0.3f, 0.3f,
 						ARemoteViewLayoutRenderStyle.NAVIGATION_OVERLAY_TRANSPARENCY);
 				}
 				else if (iNavigationMouseOverViewID_right == iRemoteLevelElementID) {
 					tmpColor_in.set(1, 0.3f, 0.3f,
 						ARemoteViewLayoutRenderStyle.NAVIGATION_OVERLAY_TRANSPARENCY);
 				}
 
 				textureMoveIn = iconTextureManager.getIconTexture(gl, EIconTextures.ARROW_LEFT);
 				textureMoveOut = iconTextureManager.getIconTexture(gl, EIconTextures.ARROW_DOWN);
 				textureMoveLeft = iconTextureManager.getIconTexture(gl, EIconTextures.ARROW_DOWN);
 				textureMoveRight = iconTextureManager.getIconTexture(gl, EIconTextures.ARROW_LEFT);
 			}
 			// else if
 			// (underInteractionLayer.getPositionIndexByElementID(iViewID) == 0)
 			// // center
 			// {
 			// topWallPickingType = EPickingType.BUCKET_MOVE_OUT_ICON_SELECTION;
 			// bottomWallPickingType =
 			// EPickingType.BUCKET_MOVE_IN_ICON_SELECTION;
 			// leftWallPickingType =
 			// EPickingType.BUCKET_MOVE_LEFT_ICON_SELECTION;
 			// rightWallPickingType =
 			// EPickingType.BUCKET_MOVE_RIGHT_ICON_SELECTION;
 			//
 			// if (iNavigationMouseOverViewID_out == iViewID)
 			// tmpColor_out.set(1, 0.3f, 0.3f, 0.9f);
 			// else if (iNavigationMouseOverViewID_in == iViewID)
 			// tmpColor_in.set(1, 0.3f, 0.3f, 0.9f);
 			// else if (iNavigationMouseOverViewID_left == iViewID)
 			// tmpColor_left.set(1, 0.3f, 0.3f, 0.9f);
 			// else if (iNavigationMouseOverViewID_right == iViewID)
 			// tmpColor_right.set(1, 0.3f, 0.3f, 0.9f);
 			//
 			// textureMoveIn =
 			// iconTextureManager.getIconTexture(EIconTextures.ARROW_LEFT);
 			// textureMoveOut =
 			// iconTextureManager.getIconTexture(EIconTextures.ARROW_DOWN);
 			// textureMoveLeft = iconTextureManager
 			// .getIconTexture(EIconTextures.ARROW_DOWN);
 			// textureMoveRight = iconTextureManager
 			// .getIconTexture(EIconTextures.ARROW_LEFT);
 			// }
 		}
 		// else if (underInteractionLayer.containsElement(iViewID))
 		// {
 		// topWallPickingType = EPickingType.BUCKET_MOVE_OUT_ICON_SELECTION;
 		// bottomWallPickingType =
 		// EPickingType.BUCKET_MOVE_RIGHT_ICON_SELECTION;
 		// leftWallPickingType = EPickingType.BUCKET_MOVE_IN_ICON_SELECTION;
 		// rightWallPickingType = EPickingType.BUCKET_MOVE_OUT_ICON_SELECTION;
 		// }
 
 		gl.glLineWidth(1);
 
 		float fNavigationZValue = 0f;
 
 		// CENTER - NAVIGATION: VIEW IDENTIFICATION ICON
 		// gl.glPushName(pickingManager.getPickingID(iUniqueID,
 		// EPickingType.BUCKET_LOCK_ICON_SELECTION, iViewID));
 
 		gl.glColor4f(0.5f, 0.5f, 0.5f, 1);
 		gl.glBegin(GL.GL_LINE_LOOP);
 		gl.glVertex3f(2.66f, 2.66f, fNavigationZValue);
 		gl.glVertex3f(2.66f, 5.33f, fNavigationZValue);
 		gl.glVertex3f(5.33f, 5.33f, fNavigationZValue);
 		gl.glVertex3f(5.33f, 2.66f, fNavigationZValue);
 		gl.glEnd();
 
 		gl.glColor4f(tmpColor_lock.x(), tmpColor_lock.y(), tmpColor_lock.z(),
 			ARemoteViewLayoutRenderStyle.NAVIGATION_OVERLAY_TRANSPARENCY);
 
 		// gl.glBegin(GL.GL_POLYGON);
 		// gl.glVertex3f(2.66f, 2.66f, 0.02f);
 		// gl.glVertex3f(2.66f, 5.33f, 0.02f);
 		// gl.glVertex3f(5.33f, 5.33f, 0.02f);
 		// gl.glVertex3f(5.33f, 2.66f, 0.02f);
 		// gl.glEnd();
 
 		textureViewSymbol.enable();
 		textureViewSymbol.bind();
 
 		gl.glBegin(GL.GL_POLYGON);
 		gl.glTexCoord2f(texCoords.left(), texCoords.bottom());
 		gl.glVertex3f(2.66f, 2.66f, fNavigationZValue);
 		gl.glTexCoord2f(texCoords.left(), texCoords.top());
 		gl.glVertex3f(2.66f, 5.33f, fNavigationZValue);
 		gl.glTexCoord2f(texCoords.right(), texCoords.top());
 		gl.glVertex3f(5.33f, 5.33f, fNavigationZValue);
 		gl.glTexCoord2f(texCoords.right(), texCoords.bottom());
 		gl.glVertex3f(5.33f, 2.66f, fNavigationZValue);
 		gl.glEnd();
 
 		textureViewSymbol.disable();
 
 		// gl.glPopName();
 
 		// BOTTOM - NAVIGATION: MOVE IN
 		gl.glPushName(pickingManager.getPickingID(iUniqueID, bottomWallPickingType, iRemoteLevelElementID));
 
 		gl.glColor4f(0.5f, 0.5f, 0.5f, 1);
 		gl.glBegin(GL.GL_LINE_LOOP);
 		gl.glVertex3f(0, 0, fNavigationZValue);
 		gl.glVertex3f(2.66f, 2.66f, fNavigationZValue);
 		gl.glVertex3f(5.33f, 2.66f, fNavigationZValue);
 		gl.glVertex3f(8, 0, fNavigationZValue);
 		gl.glEnd();
 
 		gl.glColor4f(tmpColor_in.x(), tmpColor_in.y(), tmpColor_in.z(),
 			ARemoteViewLayoutRenderStyle.NAVIGATION_OVERLAY_TRANSPARENCY);
 
 		// gl.glBegin(GL.GL_POLYGON);
 		// gl.glVertex3f(0.05f, 0.05f, 0.02f);
 		// gl.glVertex3f(2.66f, 2.66f, 0.02f);
 		// gl.glVertex3f(5.33f, 2.66f, 0.02f);
 		// gl.glVertex3f(7.95f, 0.02f, 0.02f);
 		// gl.glEnd();
 
 		textureMoveIn.enable();
 		textureMoveIn.bind();
 		// texCoords = textureMoveIn.getImageTexCoords();
 		// gl.glColor4f(1,0.3f,0.3f,0.9f);
 		gl.glBegin(GL.GL_POLYGON);
 		gl.glTexCoord2f(texCoords.left(), texCoords.bottom());
 		gl.glVertex3f(2.66f, 0.05f, fNavigationZValue);
 		gl.glTexCoord2f(texCoords.right(), texCoords.bottom());
 		gl.glVertex3f(2.66f, 2.66f, fNavigationZValue);
 		gl.glTexCoord2f(texCoords.right(), texCoords.top());
 		gl.glVertex3f(5.33f, 2.66f, fNavigationZValue);
 		gl.glTexCoord2f(texCoords.left(), texCoords.top());
 		gl.glVertex3f(5.33f, 0.05f, fNavigationZValue);
 		gl.glEnd();
 
 		textureMoveIn.disable();
 
 		gl.glPopName();
 
 		// RIGHT - NAVIGATION: MOVE RIGHT
 		gl.glPushName(pickingManager.getPickingID(iUniqueID, rightWallPickingType, iRemoteLevelElementID));
 
 		gl.glColor4f(0.5f, 0.5f, 0.5f, 1);
 		gl.glBegin(GL.GL_LINE_LOOP);
 		gl.glVertex3f(8, 0, fNavigationZValue);
 		gl.glVertex3f(5.33f, 2.66f, fNavigationZValue);
 		gl.glVertex3f(5.33f, 5.33f, fNavigationZValue);
 		gl.glVertex3f(8, 8, fNavigationZValue);
 		gl.glEnd();
 
 		gl.glColor4f(tmpColor_right.x(), tmpColor_right.y(), tmpColor_right.z(),
 			ARemoteViewLayoutRenderStyle.NAVIGATION_OVERLAY_TRANSPARENCY);
 
 		// gl.glBegin(GL.GL_POLYGON);
 		// gl.glVertex3f(7.95f, 0.05f, 0.02f);
 		// gl.glVertex3f(5.33f, 2.66f, 0.02f);
 		// gl.glVertex3f(5.33f, 5.33f, 0.02f);
 		// gl.glVertex3f(7.95f, 7.95f, 0.02f);
 		// gl.glEnd();
 
 		textureMoveRight.enable();
 		textureMoveRight.bind();
 
 		// gl.glColor4f(0,1,0,1);
 		gl.glBegin(GL.GL_POLYGON);
 		gl.glTexCoord2f(texCoords.left(), texCoords.bottom());
 		gl.glVertex3f(7.95f, 2.66f, fNavigationZValue);
 		gl.glTexCoord2f(texCoords.right(), texCoords.bottom());
 		gl.glVertex3f(5.33f, 2.66f, fNavigationZValue);
 		gl.glTexCoord2f(texCoords.right(), texCoords.top());
 		gl.glVertex3f(5.33f, 5.33f, fNavigationZValue);
 		gl.glTexCoord2f(texCoords.left(), texCoords.top());
 		gl.glVertex3f(7.95f, 5.33f, fNavigationZValue);
 		gl.glEnd();
 
 		textureMoveRight.disable();
 
 		gl.glPopName();
 
 		// LEFT - NAVIGATION: MOVE LEFT
 		gl.glPushName(pickingManager.getPickingID(iUniqueID, leftWallPickingType, iRemoteLevelElementID));
 
 		gl.glColor4f(0.5f, 0.5f, 0.5f, 1);
 		gl.glBegin(GL.GL_LINE_LOOP);
 		gl.glVertex3f(0, 0, fNavigationZValue);
 		gl.glVertex3f(0, 8, fNavigationZValue);
 		gl.glVertex3f(2.66f, 5.33f, fNavigationZValue);
 		gl.glVertex3f(2.66f, 2.66f, fNavigationZValue);
 		gl.glEnd();
 
 		gl.glColor4f(tmpColor_left.x(), tmpColor_left.y(), tmpColor_left.z(),
 			ARemoteViewLayoutRenderStyle.NAVIGATION_OVERLAY_TRANSPARENCY);
 
 		// gl.glBegin(GL.GL_POLYGON);
 		// gl.glVertex3f(0.05f, 0.05f, fNavigationZValue);
 		// gl.glVertex3f(0.05f, 7.95f, fNavigationZValue);
 		// gl.glVertex3f(2.66f, 5.33f, fNavigationZValue);
 		// gl.glVertex3f(2.66f, 2.66f, fNavigationZValue);
 		// gl.glEnd();
 
 		textureMoveLeft.enable();
 		textureMoveLeft.bind();
 
 		// gl.glColor4f(0,1,0,1);
 		gl.glBegin(GL.GL_POLYGON);
 		gl.glTexCoord2f(texCoords.left(), texCoords.bottom());
 		gl.glVertex3f(0.05f, 2.66f, fNavigationZValue);
 		gl.glTexCoord2f(texCoords.right(), texCoords.bottom());
 		gl.glVertex3f(0.05f, 5.33f, fNavigationZValue);
 		gl.glTexCoord2f(texCoords.right(), texCoords.top());
 		gl.glVertex3f(2.66f, 5.33f, fNavigationZValue);
 		gl.glTexCoord2f(texCoords.left(), texCoords.top());
 		gl.glVertex3f(2.66f, 2.66f, fNavigationZValue);
 		gl.glEnd();
 
 		textureMoveLeft.disable();
 
 		gl.glPopName();
 
 		// TOP - NAVIGATION: MOVE OUT
 		gl.glPushName(pickingManager.getPickingID(iUniqueID, topWallPickingType, iRemoteLevelElementID));
 
 		gl.glColor4f(0.5f, 0.5f, 0.5f, 1);
 		gl.glBegin(GL.GL_LINE_LOOP);
 		gl.glVertex3f(0, 8, fNavigationZValue);
 		gl.glVertex3f(8, 8, fNavigationZValue);
 		gl.glVertex3f(5.33f, 5.33f, fNavigationZValue);
 		gl.glVertex3f(2.66f, 5.33f, fNavigationZValue);
 		gl.glEnd();
 
 		gl.glColor4f(tmpColor_out.x(), tmpColor_out.y(), tmpColor_out.z(),
 			ARemoteViewLayoutRenderStyle.NAVIGATION_OVERLAY_TRANSPARENCY);
 
 		// gl.glBegin(GL.GL_POLYGON);
 		// gl.glVertex3f(0.05f, 7.95f, 0.02f);
 		// gl.glVertex3f(7.95f, 7.95f, 0.02f);
 		// gl.glVertex3f(5.33f, 5.33f, 0.02f);
 		// gl.glVertex3f(2.66f, 5.33f, 0.02f);
 		// gl.glEnd();
 
 		textureMoveOut.enable();
 		textureMoveOut.bind();
 
 		// gl.glColor4f(0,1,0,1);
 		gl.glBegin(GL.GL_POLYGON);
 		gl.glTexCoord2f(texCoords.left(), texCoords.bottom());
 		gl.glVertex3f(2.66f, 7.95f, fNavigationZValue);
 		gl.glTexCoord2f(texCoords.right(), texCoords.bottom());
 		gl.glVertex3f(5.33f, 7.95f, fNavigationZValue);
 		gl.glTexCoord2f(texCoords.right(), texCoords.top());
 		gl.glVertex3f(5.33f, 5.33f, fNavigationZValue);
 		gl.glTexCoord2f(texCoords.left(), texCoords.top());
 		gl.glVertex3f(2.66f, 5.33f, fNavigationZValue);
 		gl.glEnd();
 
 		textureMoveOut.disable();
 
 		gl.glPopName();
 	}
 
 	private void renderPoolSelection(final GL gl, float fXOrigin, float fYOrigin, float fWidth,
 		float fHeight, RemoteLevelElement element) {
 		float fPanelSideWidth = 11f;
 
 		gl.glColor3f(0.25f, 0.25f, 0.25f);
 		gl.glBegin(GL.GL_POLYGON);
 
 		gl.glVertex3f(fXOrigin + 1.65f / fAspectRatio + fPanelSideWidth, fYOrigin - fHeight / 2f + fHeight,
 			0f);
 		gl.glVertex3f(fXOrigin + 1.65f / fAspectRatio + fPanelSideWidth + fWidth, fYOrigin - fHeight / 2f
 			+ fHeight, 0f);
 		gl
 			.glVertex3f(fXOrigin + 1.65f / fAspectRatio + fPanelSideWidth + fWidth, fYOrigin - fHeight / 2f,
 				0f);
 		gl.glVertex3f(fXOrigin + 1.65f / fAspectRatio + fPanelSideWidth, fYOrigin - fHeight / 2f, 0f);
 
 		gl.glEnd();
 
 		Texture tempTexture =
 			iconTextureManager.getIconTexture(gl, EIconTextures.POOL_VIEW_BACKGROUND_SELECTION);
 		tempTexture.enable();
 		tempTexture.bind();
 
 		TextureCoords texCoords = tempTexture.getImageTexCoords();
 
 		gl.glColor4f(1, 1, 1, 0.75f);
 
 		gl.glBegin(GL.GL_POLYGON);
 		gl.glTexCoord2f(texCoords.left(), texCoords.bottom());
 		gl.glVertex3f(fXOrigin + 2 / fAspectRatio + fPanelSideWidth, fYOrigin - fHeight, -0.01f);
 		gl.glTexCoord2f(texCoords.left(), texCoords.top());
 		gl.glVertex3f(fXOrigin + 2 / fAspectRatio + fPanelSideWidth, fYOrigin + fHeight, -0.01f);
 		gl.glTexCoord2f(texCoords.right(), texCoords.top());
 		gl.glVertex3f(fXOrigin + 2f / fAspectRatio, fYOrigin + fHeight, -0.01f);
 		gl.glTexCoord2f(texCoords.right(), texCoords.bottom());
 		gl.glVertex3f(fXOrigin + 2f / fAspectRatio, fYOrigin - fHeight, -0.01f);
 		gl.glEnd();
 
 		tempTexture.disable();
 
 		gl.glPopName();
 		gl.glPopName();
 
 		int fHandleScaleFactor = 18;
 		gl.glTranslatef(fXOrigin + 2.5f / fAspectRatio, fYOrigin - fHeight / 2f + fHeight - 1f, 1.8f);
 		gl.glScalef(fHandleScaleFactor, fHandleScaleFactor, fHandleScaleFactor);
 		renderSingleHandle(gl, element.getID(), EPickingType.BUCKET_DRAG_ICON_SELECTION,
 			EIconTextures.POOL_DRAG_VIEW, 0.1f, 0.1f);
 		gl.glTranslatef(0, -0.2f, 0);
 		renderSingleHandle(gl, element.getID(), EPickingType.BUCKET_REMOVE_ICON_SELECTION,
 			EIconTextures.POOL_REMOVE_VIEW, 0.1f, 0.1f);
 		gl.glTranslatef(0, 0.2f, 0);
 		gl.glScalef(1f / fHandleScaleFactor, 1f / fHandleScaleFactor, 1f / fHandleScaleFactor);
 		gl.glTranslatef(-fXOrigin - 2.5f / fAspectRatio, -fYOrigin + fHeight / 2f - fHeight + 1f, -1.8f);
 
 		// gl.glColor3f(0.25f, 0.25f, 0.25f);
 		// gl.glBegin(GL.GL_POLYGON);
 		// gl.glVertex3f(fXOrigin + 3f, fYOrigin - fHeight / 2f + fHeight -
 		// 2.5f, 0f);
 		// gl.glVertex3f(fXOrigin + 5.1f, fYOrigin - fHeight / 2f + fHeight -
 		// 2.5f, 0f);
 		// gl.glVertex3f(fXOrigin + 5.1f, fYOrigin- fHeight / 2f + 1.5f, 0f);
 		// gl.glVertex3f(fXOrigin + 3f, fYOrigin- fHeight / 2f + 1.5f , 0f);
 		// gl.glEnd();
 
 		gl.glPushName(pickingManager.getPickingID(iUniqueID, EPickingType.REMOTE_LEVEL_ELEMENT, element
 			.getID()));
 		gl.glPushName(pickingManager.getPickingID(iUniqueID, EPickingType.VIEW_SELECTION, element.getID()));
 	}
 
 	private void doSlerpActions(final GL gl) {
 		if (arSlerpActions.isEmpty())
 			return;
 
 		SlerpAction tmpSlerpAction = arSlerpActions.get(0);
 
 		if (iSlerpFactor == 0) {
 			tmpSlerpAction.start();
 
 			// System.out.println("Start slerp action " +tmpSlerpAction);
 		}
 
 		if (iSlerpFactor < SLERP_RANGE) {
 			// Makes animation rendering speed independent
 			iSlerpFactor += SLERP_SPEED * time.deltaT();
 
 			if (iSlerpFactor > SLERP_RANGE) {
 				iSlerpFactor = SLERP_RANGE;
 			}
 		}
 
 		slerpView(gl, tmpSlerpAction);
 	}
 
 	private void slerpView(final GL gl, SlerpAction slerpAction) {
 		int iViewID = slerpAction.getElementId();
 
 		SlerpMod slerpMod = new SlerpMod();
 
 		if (iSlerpFactor == 0) {
 			slerpMod.playSlerpSound();
 		}
 
 		Transform transform =
 			slerpMod.interpolate(slerpAction.getOriginRemoteLevelElement().getTransform(), slerpAction
 				.getDestinationRemoteLevelElement().getTransform(), (float) iSlerpFactor / SLERP_RANGE);
 
 		gl.glPushMatrix();
 
 		slerpMod.applySlerp(gl, transform);
 
 		generalManager.getViewGLCanvasManager().getGLEventListener(iViewID).displayRemote(gl);
 
 		gl.glPopMatrix();
 
 		// Check if slerp action is finished
 		if (iSlerpFactor >= SLERP_RANGE) {
 			arSlerpActions.remove(slerpAction);
 
 			iSlerpFactor = 0;
 
 			slerpAction.finished();
 
 			RemoteLevelElement destinationElement = slerpAction.getDestinationRemoteLevelElement();
 
 			updateViewDetailLevels(destinationElement);
 		}
 
 		// After last slerp action is done the line connections are turned on
 		// again
 		if (arSlerpActions.isEmpty()) {
 			if (glConnectionLineRenderer != null) {
 				glConnectionLineRenderer.enableRendering(true);
 			}
 
 			generalManager.getViewGLCanvasManager().getInfoAreaManager().enable(!bEnableNavigationOverlay);
 		}
 	}
 
 	private void updateViewDetailLevels(RemoteLevelElement element) {
 		RemoteLevel destinationLevel = element.getRemoteLevel();
 
 		if (element.getContainedElementID() == -1)
 			return;
 
 		AGLEventListener glActiveSubView =
 			GeneralManager.get().getViewGLCanvasManager().getGLEventListener(element.getContainedElementID());
 
 		glActiveSubView.setRemoteLevelElement(element);
 
 		// Update detail level of moved view when slerp action is finished;
 		if (destinationLevel == focusLevel) {
 			if (bucketMouseWheelListener.isZoomedIn() || layoutRenderStyle instanceof ListLayoutRenderStyle) {
 				glActiveSubView.setDetailLevel(EDetailLevel.HIGH);
 			}
 			else {
 				glActiveSubView.setDetailLevel(EDetailLevel.MEDIUM);
 			}
 
 			// if (glActiveSubView instanceof GLPathway)
 			// {
 			// ((GLPathway) glActiveSubView).enableTitleRendering(true);
 			// ((GLPathway) glActiveSubView).setAlignment(SWT.CENTER,
 			// SWT.BOTTOM);
 			// }
 
 			// generalManager.getGUIBridge().setActiveGLSubView(this,
 			// glActiveSubView);
 		}
 		else if (destinationLevel == stackLevel) {
 			glActiveSubView.setDetailLevel(EDetailLevel.LOW);
 
 			// if (glActiveSubView instanceof GLPathway)
 			// {
 			// ((GLPathway) glActiveSubView).enableTitleRendering(true);
 			//
 			// int iStackPos = stackLevel.getPositionIndexByElementID(element);
 			// switch (iStackPos)
 			// {
 			// case 0:
 			// ((GLPathway) glActiveSubView).setAlignment(SWT.CENTER, SWT.TOP);
 			// break;
 			// case 1:
 			// ((GLPathway) glActiveSubView).setAlignment(SWT.LEFT, SWT.BOTTOM);
 			// break;
 			// case 2:
 			// ((GLPathway) glActiveSubView).setAlignment(SWT.CENTER,
 			// SWT.BOTTOM);
 			// break;
 			// case 3:
 			// ((GLPathway) glActiveSubView).setAlignment(SWT.RIGHT,
 			// SWT.BOTTOM);
 			// break;
 			// default:
 			// break;
 			// }
 			// }
 		}
 		else if (destinationLevel == poolLevel || destinationLevel == externalSelectionLevel) {
 			glActiveSubView.setDetailLevel(EDetailLevel.VERY_LOW);
 
 			// if (glActiveSubView instanceof GLPathway)
 			// {
 			// ((GLPathway) glActiveSubView).enableTitleRendering(false);
 			// }
 		}
 
 		compactPoolLevel();
 	}
 
 	private void loadViewToFocusLevel(final int iRemoteLevelElementID) {
 		RemoteLevelElement element = RemoteElementManager.get().getItem(iRemoteLevelElementID);
 
 		// Check if other slerp action is currently running
 		if (iSlerpFactor > 0 && iSlerpFactor < SLERP_RANGE)
 			return;
 
 		arSlerpActions.clear();
 
 		int iViewID = element.getContainedElementID();
 
 		if (iViewID == -1)
 			return;
 
 		// Only broadcast elements if view is moved from pool to bucket
 		if (poolLevel.containsElement(element)) {
 			generalManager.getViewGLCanvasManager().getGLEventListener(iViewID).broadcastElements(
 				EVAOperation.APPEND_UNIQUE);
 		}
 
 		// if (layoutRenderStyle instanceof ListLayoutRenderStyle)
 		// {
 		// // Slerp selected view to under interaction transition position
 		// SlerpAction slerpActionTransition = new
 		// SlerpAction(iRemoteLevelElementID, poolLevel,
 		// transitionLevel);
 		// arSlerpActions.add(slerpActionTransition);
 		//
 		// // Check if focus has a free spot
 		// if (focusLevel.getElementByPositionIndex(0).getContainedElementID()
 		// != -1)
 		// {
 		// // Slerp under interaction view to free spot in pool
 		// SlerpAction reverseSlerpAction = new SlerpAction(focusLevel
 		// .getElementIDByPositionIndex(0), focusLevel, poolLevel);
 		// arSlerpActions.add(reverseSlerpAction);
 		// }
 		//
 		// // Slerp selected view from transition position to under interaction
 		// // position
 		// SlerpAction slerpAction = new SlerpAction(iViewID, transitionLevel,
 		// focusLevel);
 		// arSlerpActions.add(slerpAction);
 		// }
 		// else
 		{
 			// Check if view is already loaded in the stack layer
 			if (stackLevel.containsElement(element)) {
 
 				// Slerp selected view to transition position
 				SlerpAction slerpActionTransition =
 					new SlerpAction(element, transitionLevel.getElementByPositionIndex(0));
 				arSlerpActions.add(slerpActionTransition);
 
 				// Check if focus level is free
 				if (!focusLevel.hasFreePosition()) {
 					// Slerp focus view to free spot in stack
 					SlerpAction reverseSlerpAction =
 						new SlerpAction(focusLevel.getElementByPositionIndex(0).getContainedElementID(),
 							focusLevel.getElementByPositionIndex(0), element);
 					arSlerpActions.add(reverseSlerpAction);
 				}
 
 				// Slerp selected view from transition position to focus
 				// position
 				SlerpAction slerpAction =
 					new SlerpAction(element.getContainedElementID(), transitionLevel
 						.getElementByPositionIndex(0), focusLevel.getElementByPositionIndex(0));
 				arSlerpActions.add(slerpAction);
 			}
 			// Check if focus position is free
 			else if (focusLevel.hasFreePosition()) {
 
 				// Slerp selected view to focus position
 				SlerpAction slerpActionTransition =
 					new SlerpAction(element, focusLevel.getElementByPositionIndex(0));
 				arSlerpActions.add(slerpActionTransition);
 
 			}
 			else {
 				// Slerp selected view to transition position
 				SlerpAction slerpActionTransition =
 					new SlerpAction(element, transitionLevel.getElementByPositionIndex(0));
 				arSlerpActions.add(slerpActionTransition);
 
 				RemoteLevelElement freeStackElement = null;
 				if (!stackLevel.hasFreePosition()) {
 					int iReplacePosition = 1;
 
 					// // Determine non locked stack position for view movement
 					// to pool
 					// for (int iTmpReplacePosition = 0; iTmpReplacePosition <
 					// stackLevel.getCapacity(); iTmpReplacePosition++)
 					// {
 					// if
 					// (stackLevel.getElementByPositionIndex(iTmpReplacePosition).isLocked())
 					// continue;
 					//						
 					// iReplacePosition = iTmpReplacePosition + 1; // +1 to
 					// start with left view for outsourcing
 					//						
 					// if (iReplacePosition == 4)
 					// iReplacePosition = 0;
 					//						
 					// break;
 					// }
 					//					
 					// if (iReplacePosition == -1)
 					// throw new
 					// IllegalStateException("All views in stack are locked!");
 
 					freeStackElement = stackLevel.getElementByPositionIndex(iReplacePosition);
 
 					// Slerp view from stack to pool
 					SlerpAction reverseSlerpAction =
 						new SlerpAction(freeStackElement, poolLevel.getNextFree());
 					arSlerpActions.add(reverseSlerpAction);
 
 					// Unregister all elements of the view that is moved out
 					generalManager.getViewGLCanvasManager().getGLEventListener(
 						freeStackElement.getContainedElementID()).broadcastElements(
 						EVAOperation.REMOVE_ELEMENT);
 				}
 				else {
 					freeStackElement = stackLevel.getNextFree();
 				}
 
 				if (!focusLevel.hasFreePosition()) {
 					// Slerp focus view to free spot in stack
 					SlerpAction reverseSlerpAction2 =
 						new SlerpAction(focusLevel.getElementByPositionIndex(0), freeStackElement);
 					arSlerpActions.add(reverseSlerpAction2);
 				}
 
 				// Slerp selected view from transition position to focus
 				// position
 				SlerpAction slerpAction =
 					new SlerpAction(iViewID, transitionLevel.getElementByPositionIndex(0), focusLevel
 						.getElementByPositionIndex(0));
 				arSlerpActions.add(slerpAction);
 			}
 		}
 
 		iSlerpFactor = 0;
 	}
 
 	@SuppressWarnings("unchecked")
 	@Override
 	public void handleExternalEvent(IMediatorSender eventTrigger, IEventContainer eventContainer,
 		EMediatorType eMediatorType) {
 
 		switch (eventContainer.getEventType()) {
 			// pathway loading based on gene id
 			// case LOAD_PATHWAY_BY_GENE:
 			//
 			// // take care here, if we ever use non integer ids this has to be
 			// // cast to raw type first to determine the actual id data types
 			// IDListEventContainer<Integer> idContainer = (IDListEventContainer<Integer>) eventContainer;
 			// if (idContainer.getIDType() == EIDType.REFSEQ_MRNA_INT) {
 			// int iGraphItemID = 0;
 			// Integer iDavidID = -1;
 			// ArrayList<ICaleydoGraphItem> alPathwayVertexGraphItem =
 			// new ArrayList<ICaleydoGraphItem>();
 			//
 			// for (Integer iRefSeqID : idContainer.getIDs()) {
 			// iDavidID = idMappingManager.getID(EMappingType.REFSEQ_MRNA_INT_2_DAVID, iRefSeqID);
 			//
 			// if (iDavidID == null || iDavidID == -1)
 			// throw new IllegalStateException("Cannot resolve RefSeq ID to David ID.");
 			//
 			// iGraphItemID =
 			// generalManager.getPathwayItemManager().getPathwayVertexGraphItemIdByDavidId(
 			// iDavidID);
 			//
 			// if (iGraphItemID == -1) {
 			// continue;
 			// }
 			//
 			// PathwayVertexGraphItem tmpPathwayVertexGraphItem =
 			// (PathwayVertexGraphItem) generalManager.getPathwayItemManager().getItem(
 			// iGraphItemID);
 			//
 			// if (tmpPathwayVertexGraphItem == null) {
 			// continue;
 			// }
 			//
 			// alPathwayVertexGraphItem.add(tmpPathwayVertexGraphItem);
 			// }
 			//
 			// if (!alPathwayVertexGraphItem.isEmpty()) {
 			// loadDependentPathways(alPathwayVertexGraphItem);
 			// }
 			// }
 			// else
 			// throw new IllegalStateException("Not Implemented");
 			// break;
 			// Handle incoming pathways
 			case LOAD_PATHWAY_BY_PATHWAY_ID:
 				IDListEventContainer<Integer> pathwayIDContainer =
 					(IDListEventContainer<Integer>) eventContainer;
 
 				for (Integer iPathwayID : pathwayIDContainer.getIDs()) {
 					addPathwayView(iPathwayID);
 				}
 
 				break;
 			case SELECTION_UPDATE:
 				lastSelectionDelta =
 					((DeltaEventContainer<ISelectionDelta>) eventContainer).getSelectionDelta();
 		}
 
 		bUpdateOffScreenTextures = true;
 	}
 
 	/**
 	 * 
 	 */
 	@Override
 	public synchronized void addInitialRemoteView(ASerializedView serView) {
 		newViews.add(serView);
 	}
 
 	/**
 	 * Add pathway view. Also used when serialized pathways are loaded.
 	 * 
 	 * @param iPathwayIDToLoad
 	 */
 	public synchronized void addPathwayView(final int iPathwayID) {
 
 		if (!generalManager.getPathwayManager().isPathwayVisible(
 			generalManager.getPathwayManager().getItem(iPathwayID))) {
 			SerializedPathwayView serPathway = new SerializedPathwayView();
 			serPathway.setPathwayID(iPathwayID);
 			newViews.add(serPathway);
 		}
 	}
 
 	@Override
 	protected void handleEvents(EPickingType pickingType, EPickingMode pickingMode, int iExternalID, Pick pick) {
 		switch (pickingType) {
 			case BUCKET_DRAG_ICON_SELECTION:
 
 				switch (pickingMode) {
 					case CLICKED:
 
 						if (!dragAndDrop.isDragActionRunning()) {
 							// System.out.println("Start drag!");
 							dragAndDrop.startDragAction(iExternalID);
 						}
 
 						iMouseOverObjectID = iExternalID;
 
 						compactPoolLevel();
 
 						break;
 				}
 
 				pickingManager.flushHits(iUniqueID, EPickingType.BUCKET_DRAG_ICON_SELECTION);
 				pickingManager.flushHits(iUniqueID, EPickingType.REMOTE_LEVEL_ELEMENT);
 
 				break;
 
 			case BUCKET_REMOVE_ICON_SELECTION:
 
 				switch (pickingMode) {
 					case CLICKED:
 
 						RemoteLevelElement element = RemoteElementManager.get().getItem(iExternalID);
 
 						AGLEventListener glEventListener =
 							(AGLEventListener) generalManager.getViewGLCanvasManager().getGLEventListener(
 								element.getContainedElementID());
 
 						// // Unregister all elements of the view that is
 						// removed
 						// glEventListener.broadcastElements(EVAOperation.REMOVE_ELEMENT);
 
 						removeView(glEventListener);
 						element.setContainedElementID(-1);
 						containedViewIDs.remove(new Integer(glEventListener.getID()));
 
 						if (element.getRemoteLevel() == poolLevel) {
 							compactPoolLevel();
 						}
 
 						break;
 				}
 
 				pickingManager.flushHits(iUniqueID, EPickingType.BUCKET_REMOVE_ICON_SELECTION);
 
 				break;
 
 			case BUCKET_LOCK_ICON_SELECTION:
 
 				switch (pickingMode) {
 					case CLICKED:
 
 						RemoteLevelElement element = RemoteElementManager.get().getItem(iExternalID);
 
 						// Toggle lock flag
 						element.lock(!element.isLocked());
 
 						break;
 				}
 
 				pickingManager.flushHits(iUniqueID, EPickingType.BUCKET_LOCK_ICON_SELECTION);
 
 				break;
 
 			case REMOTE_LEVEL_ELEMENT:
 				switch (pickingMode) {
 					case MOUSE_OVER:
 					case DRAGGED:
 						iMouseOverObjectID = iExternalID;
 						break;
 					case CLICKED:
 
 						// Do not handle click if element is dragged
 						if (dragAndDrop.isDragActionRunning()) {
 							break;
 						}
 
 						// Check if view is contained in pool level
 						for (RemoteLevelElement element : poolLevel.getAllElements()) {
 							if (element.getID() == iExternalID) {
 								loadViewToFocusLevel(iExternalID);
 								break;
 							}
 						}
 						break;
 				}
 
 				pickingManager.flushHits(iUniqueID, pickingType);
 
 				break;
 
 			case VIEW_SELECTION:
 				switch (pickingMode) {
 					case MOUSE_OVER:
 
 						// generalManager.getViewGLCanvasManager().getInfoAreaManager()
 						// .setDataAboutView(iExternalID);
 
 						// Prevent update flood when moving mouse over view
 						if (iActiveViewID == iExternalID) {
 							break;
 						}
 
 						iActiveViewID = iExternalID;
 
 						setDisplayListDirty();
 
 						// TODO
 						// generalManager.getEventPublisher().triggerEvent(
 						// EMediatorType.VIEW_SELECTION,
 						// generalManager.getViewGLCanvasManager().getGLEventListener(
 						// iExternalID), );
 
 						break;
 
 					case CLICKED:
 
 						// generalManager.getViewGLCanvasManager().getInfoAreaManager()
 						// .setDataAboutView(iExternalID);
 
 						break;
 				}
 
 				pickingManager.flushHits(iUniqueID, EPickingType.VIEW_SELECTION);
 
 				break;
 
 			case BUCKET_MOVE_IN_ICON_SELECTION:
 				switch (pickingMode) {
 					case CLICKED:
 						loadViewToFocusLevel(iExternalID);
 						bEnableNavigationOverlay = false;
 						// glConnectionLineRenderer.enableRendering(true);
 						break;
 
 					case MOUSE_OVER:
 
 						iNavigationMouseOverViewID_left = -1;
 						iNavigationMouseOverViewID_right = -1;
 						iNavigationMouseOverViewID_out = -1;
 						iNavigationMouseOverViewID_in = iExternalID;
 						iNavigationMouseOverViewID_lock = -1;
 
 						break;
 				}
 
 				pickingManager.flushHits(iUniqueID, EPickingType.BUCKET_MOVE_IN_ICON_SELECTION);
 
 				break;
 
 			case BUCKET_MOVE_OUT_ICON_SELECTION:
 				switch (pickingMode) {
 					case CLICKED:
 
 						// Check if other slerp action is currently running
 						if (iSlerpFactor > 0 && iSlerpFactor < SLERP_RANGE) {
 							break;
 						}
 
 						// glConnectionLineRenderer.enableRendering(true);
 
 						arSlerpActions.clear();
 
 						RemoteLevelElement element = RemoteElementManager.get().getItem(iExternalID);
 						SlerpAction slerpActionTransition = new SlerpAction(element, poolLevel.getNextFree());
 						arSlerpActions.add(slerpActionTransition);
 
 						bEnableNavigationOverlay = false;
 
 						// Unregister all elements of the view that is moved out
 						generalManager.getViewGLCanvasManager().getGLEventListener(
 							element.getContainedElementID()).broadcastElements(EVAOperation.REMOVE_ELEMENT);
 
 						break;
 
 					case MOUSE_OVER:
 
 						iNavigationMouseOverViewID_left = -1;
 						iNavigationMouseOverViewID_right = -1;
 						iNavigationMouseOverViewID_out = iExternalID;
 						iNavigationMouseOverViewID_in = -1;
 						iNavigationMouseOverViewID_lock = -1;
 
 						break;
 				}
 
 				pickingManager.flushHits(iUniqueID, EPickingType.BUCKET_MOVE_OUT_ICON_SELECTION);
 
 				break;
 
 			case BUCKET_MOVE_LEFT_ICON_SELECTION:
 				switch (pickingMode) {
 					case CLICKED:
 						// Check if other slerp action is currently running
 						if (iSlerpFactor > 0 && iSlerpFactor < SLERP_RANGE) {
 							break;
 						}
 
 						// glConnectionLineRenderer.enableRendering(true);
 
 						arSlerpActions.clear();
 
 						RemoteLevelElement selectedElement = RemoteElementManager.get().getItem(iExternalID);
 
 						int iDestinationPosIndex = stackLevel.getPositionIndexByElementID(selectedElement);
 
 						if (iDestinationPosIndex == 3) {
 							iDestinationPosIndex = 0;
 						}
 						else {
 							iDestinationPosIndex++;
 						}
 
 						// Check if destination position in stack is free
 						if (stackLevel.getElementByPositionIndex(iDestinationPosIndex)
 							.getContainedElementID() == -1) {
 							SlerpAction slerpAction =
 								new SlerpAction(selectedElement, stackLevel
 									.getElementByPositionIndex(iDestinationPosIndex));
 							arSlerpActions.add(slerpAction);
 						}
 						else {
 							SlerpAction slerpActionTransition =
 								new SlerpAction(selectedElement, transitionLevel.getElementByPositionIndex(0));
 							arSlerpActions.add(slerpActionTransition);
 
 							SlerpAction slerpAction =
 								new SlerpAction(stackLevel.getElementByPositionIndex(iDestinationPosIndex),
 									selectedElement);
 							arSlerpActions.add(slerpAction);
 
 							SlerpAction slerpActionTransitionReverse =
 								new SlerpAction(selectedElement.getContainedElementID(), transitionLevel
 									.getElementByPositionIndex(0), stackLevel
 									.getElementByPositionIndex(iDestinationPosIndex));
 							arSlerpActions.add(slerpActionTransitionReverse);
 						}
 
 						bEnableNavigationOverlay = false;
 
 						break;
 
 					case MOUSE_OVER:
 
 						iNavigationMouseOverViewID_left = iExternalID;
 						iNavigationMouseOverViewID_right = -1;
 						iNavigationMouseOverViewID_out = -1;
 						iNavigationMouseOverViewID_in = -1;
 						iNavigationMouseOverViewID_lock = -1;
 
 						break;
 				}
 
 				pickingManager.flushHits(iUniqueID, EPickingType.BUCKET_MOVE_LEFT_ICON_SELECTION);
 
 				break;
 
 			case BUCKET_MOVE_RIGHT_ICON_SELECTION:
 				switch (pickingMode) {
 					case CLICKED:
 						// Check if other slerp action is currently running
 						if (iSlerpFactor > 0 && iSlerpFactor < SLERP_RANGE) {
 							break;
 						}
 
 						// glConnectionLineRenderer.enableRendering(true);
 
 						arSlerpActions.clear();
 
 						RemoteLevelElement selectedElement = RemoteElementManager.get().getItem(iExternalID);
 
 						int iDestinationPosIndex = stackLevel.getPositionIndexByElementID(selectedElement);
 
 						if (iDestinationPosIndex == 0) {
 							iDestinationPosIndex = 3;
 						}
 						else {
 							iDestinationPosIndex--;
 						}
 
 						// Check if destination position in stack is free
 						if (stackLevel.getElementByPositionIndex(iDestinationPosIndex)
 							.getContainedElementID() == -1) {
 							SlerpAction slerpAction =
 								new SlerpAction(selectedElement, stackLevel
 									.getElementByPositionIndex(iDestinationPosIndex));
 							arSlerpActions.add(slerpAction);
 						}
 						else {
 							SlerpAction slerpActionTransition =
 								new SlerpAction(selectedElement, transitionLevel.getElementByPositionIndex(0));
 							arSlerpActions.add(slerpActionTransition);
 
 							SlerpAction slerpAction =
 								new SlerpAction(stackLevel.getElementByPositionIndex(iDestinationPosIndex),
 									selectedElement);
 							arSlerpActions.add(slerpAction);
 
 							SlerpAction slerpActionTransitionReverse =
 								new SlerpAction(selectedElement.getContainedElementID(), transitionLevel
 									.getElementByPositionIndex(0), stackLevel
 									.getElementByPositionIndex(iDestinationPosIndex));
 							arSlerpActions.add(slerpActionTransitionReverse);
 						}
 
 						bEnableNavigationOverlay = false;
 
 						break;
 
 					case MOUSE_OVER:
 
 						iNavigationMouseOverViewID_left = -1;
 						iNavigationMouseOverViewID_right = iExternalID;
 						iNavigationMouseOverViewID_out = -1;
 						iNavigationMouseOverViewID_in = -1;
 						iNavigationMouseOverViewID_lock = -1;
 
 						break;
 				}
 
 				pickingManager.flushHits(iUniqueID, EPickingType.BUCKET_MOVE_RIGHT_ICON_SELECTION);
 
 				break;
 		}
 	}
 
 	@Override
 	public String getShortInfo() {
 		return "Bucket / Jukebox";
 	}
 
 	@Override
 	public String getDetailedInfo() {
 		StringBuffer sInfoText = new StringBuffer();
 		sInfoText.append("Bucket / Jukebox");
 		return sInfoText.toString();
 	}
 
 	private void createEventMediator() {
 		generalManager.getEventPublisher()
 			.addSender(EMediatorType.SELECTION_MEDIATOR, (IMediatorSender) this);
 
 		generalManager.getEventPublisher().addReceiver(EMediatorType.SELECTION_MEDIATOR,
 			(IMediatorReceiver) this);
 	}
 
 	public synchronized void toggleLayoutMode() {
 		if (layoutMode.equals(ARemoteViewLayoutRenderStyle.LayoutMode.BUCKET)) {
 			// layoutMode = ARemoteViewLayoutRenderStyle.LayoutMode.LIST;
 			layoutMode = ARemoteViewLayoutRenderStyle.LayoutMode.JUKEBOX;
 		}
 		else {
 			layoutMode = ARemoteViewLayoutRenderStyle.LayoutMode.BUCKET;
 		}
 
 		if (layoutMode.equals(ARemoteViewLayoutRenderStyle.LayoutMode.BUCKET)) {
 			layoutRenderStyle = new BucketLayoutRenderStyle(viewFrustum, layoutRenderStyle);
 
 			bucketMouseWheelListener =
 				new BucketMouseWheelListener(this, (BucketLayoutRenderStyle) layoutRenderStyle);
 
 			// Unregister standard mouse wheel listener
 			parentGLCanvas.removeMouseWheelListener(pickingTriggerMouseAdapter);
 			parentGLCanvas.removeMouseListener(pickingTriggerMouseAdapter);
 			// Register specialized bucket mouse wheel listener
 			parentGLCanvas.addMouseWheelListener(bucketMouseWheelListener);
 			parentGLCanvas.addMouseListener(bucketMouseWheelListener);
 
 			glConnectionLineRenderer = new GLConnectionLineRendererBucket(focusLevel, stackLevel, poolLevel);
 		}
 		else if (layoutMode.equals(ARemoteViewLayoutRenderStyle.LayoutMode.JUKEBOX)) {
 			layoutRenderStyle = new JukeboxLayoutRenderStyle(viewFrustum, layoutRenderStyle);
 
 			// Unregister bucket wheel listener
 			parentGLCanvas.removeMouseWheelListener(bucketMouseWheelListener);
 			// Register standard mouse wheel listener
 			parentGLCanvas.addMouseWheelListener(pickingTriggerMouseAdapter);
 
 			glConnectionLineRenderer = new GLConnectionLineRendererJukebox(focusLevel, stackLevel, poolLevel);
 		}
 		else if (layoutMode.equals(ARemoteViewLayoutRenderStyle.LayoutMode.LIST)) {
 			layoutRenderStyle = new ListLayoutRenderStyle(viewFrustum, layoutRenderStyle);
 			glConnectionLineRenderer = null;
 
 			// // Copy views from stack to pool
 			// for (Integer iElementID : stackLevel.getElementList())
 			// {
 			// if (iElementID == -1)
 			// continue;
 			//				
 			// poolLevel.addElement(iElementID);
 			// // poolLevel.setElementVisibilityById(true, iElementID);
 			// }
 			// stackLevel.clear();
 		}
 
 		focusLevel = layoutRenderStyle.initFocusLevel();
 		stackLevel = layoutRenderStyle.initStackLevel(bucketMouseWheelListener.isZoomedIn());
 		poolLevel = layoutRenderStyle.initPoolLevel(bucketMouseWheelListener.isZoomedIn(), -1);
 		externalSelectionLevel = layoutRenderStyle.initMemoLevel();
 		transitionLevel = layoutRenderStyle.initTransitionLevel();
 		spawnLevel = layoutRenderStyle.initSpawnLevel();
 
 		viewFrustum.setProjectionMode(layoutRenderStyle.getProjectionMode());
 
 		// Trigger reshape to apply new projection mode
 		// Is there a better way to achieve this? :)
 		parentGLCanvas.setSize(parentGLCanvas.getWidth(), parentGLCanvas.getHeight());
 	}
 
 	/**
 	 * Unregister view from event system. Remove view from GL render loop.
 	 */
 	public void removeView(AGLEventListener glEventListener) {
 		if (glEventListener != null) {
 			glEventListener.destroy();
 		}
 	}
 
 	public synchronized void clearAll() {
 		enableBusyMode(false);
 		pickingManager.enablePicking(true);
 
 		ArrayList<ASerializedView> removeNewViews = new ArrayList<ASerializedView>();
 		for (ASerializedView view : newViews) {
 			if (!(view instanceof SerializedParallelCoordinatesView || view instanceof SerializedHeatMapView)) {
 				removeNewViews.add(view);
 			}
 		}
 		newViews.removeAll(removeNewViews);
 
 		ArrayList<Integer> removeViewIDs = new ArrayList<Integer>();
 		IViewManager viewManager = generalManager.getViewGLCanvasManager();
 		for (int viewID : containedViewIDs) {
 			AGLEventListener view = viewManager.getGLEventListener(viewID);
 			if (!(view instanceof GLParallelCoordinates || view instanceof GLHeatMap)) {
 				removeViewIDs.add(viewID);
 			}
 		}
 		containedViewIDs.removeAll(removeViewIDs);
 
 		generalManager.getPathwayManager().resetPathwayVisiblityState();
 
 		arSlerpActions.clear();
 		clearRemoteLevel(focusLevel);
 		clearRemoteLevel(stackLevel);
 		clearRemoteLevel(poolLevel);
 
 		generalManager.getViewGLCanvasManager().getConnectedElementRepresentationManager().clearAll();
 	}
 
 	private void clearRemoteLevel(RemoteLevel remoteLevel) {
 		int iViewID;
 		IViewManager viewManager = generalManager.getViewGLCanvasManager();
 		AGLEventListener glEventListener = null;
 
 		for (RemoteLevelElement element : remoteLevel.getAllElements()) {
 			iViewID = element.getContainedElementID();
 
 			if (iViewID == -1) {
 				continue;
 			}
 
 			glEventListener = viewManager.getGLEventListener(iViewID);
 
 			if (glEventListener instanceof GLHeatMap || glEventListener instanceof GLParallelCoordinates) {
 				// Remove all elements from heatmap and parallel coordinates
 				((AStorageBasedView) glEventListener).resetView();
 
 				if (!glEventListener.isRenderedRemote()) {
 					glEventListener.enableBusyMode(false);
 				}
 			}
 			else {
 				removeView(glEventListener);
 				element.setContainedElementID(-1);
 			}
 		}
 	}
 
 	// @Override
 	// public synchronized RemoteLevel getHierarchyLayerByGLEventListenerId(
 	// final int iGLEventListenerId)
 	// {
 	// if (focusLevel.containsElement(iGLEventListenerId))
 	// return focusLevel;
 	// else if (stackLevel.containsElement(iGLEventListenerId))
 	// return stackLevel;
 	// else if (poolLevel.containsElement(iGLEventListenerId))
 	// return poolLevel;
 	// else if (transitionLevel.containsElement(iGLEventListenerId))
 	// return transitionLevel;
 	// else if (spawnLevel.containsElement(iGLEventListenerId))
 	// return spawnLevel;
 	// else if (selectionLevel.containsElement(iGLEventListenerId))
 	// return selectionLevel;
 	//
 	// generalManager.getLogger().log(Level.WARNING,
 	// "GL Event Listener " + iGLEventListenerId +
 	// " is not contained in any layer!");
 	//
 	// return null;
 	// }
 
 	@Override
 	public RemoteLevel getFocusLevel() {
 		return focusLevel;
 	}
 
 	@Override
 	public BucketMouseWheelListener getBucketMouseWheelListener() {
 		return bucketMouseWheelListener;
 	}
 
 	@Override
 	public synchronized void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
 		super.reshape(drawable, x, y, width, height);
 
 		// Update aspect ratio and reinitialize stack and focus layer
 		layoutRenderStyle.setAspectRatio(fAspectRatio);
 
 		layoutRenderStyle.initFocusLevel();
 		layoutRenderStyle.initStackLevel(bucketMouseWheelListener.isZoomedIn());
 		layoutRenderStyle.initPoolLevel(bucketMouseWheelListener.isZoomedIn(), iMouseOverObjectID);
 		layoutRenderStyle.initMemoLevel();
 	}
 
 	protected void renderPoolAndMemoLayerBackground(final GL gl) {
 		// Pool layer background
 
 		float fWidth = 0.8f;
 		float fXCorrection = 0.07f; // Detach pool level from stack
 
 		if (layoutMode.equals(LayoutMode.BUCKET)) {
 			gl.glPushName(pickingManager.getPickingID(iUniqueID, EPickingType.REMOTE_LEVEL_ELEMENT,
 				iPoolLevelCommonID));
 
 			gl.glColor4fv(GeneralRenderStyle.PANEL_BACKGROUN_COLOR, 0);
 			gl.glLineWidth(1);
 			gl.glBegin(GL.GL_POLYGON);
 			gl.glVertex3f((-2 - fXCorrection) / fAspectRatio, -2, 4);
 			gl.glVertex3f((-2 - fXCorrection) / fAspectRatio, 2, 4);
 			gl.glVertex3f((-2 - fXCorrection) / fAspectRatio + fWidth, 2, 4);
 			gl.glVertex3f((-2 - fXCorrection) / fAspectRatio + fWidth, -2, 4);
 			gl.glEnd();
 
 			if (dragAndDrop.isDragActionRunning() && iMouseOverObjectID == iPoolLevelCommonID) {
 				gl.glLineWidth(5);
 				gl.glColor4f(0.2f, 0.2f, 0.2f, 1);
 			}
 			else {
 				gl.glLineWidth(1);
 				gl.glColor4f(0.4f, 0.4f, 0.4f, 1);
 			}
 
 			gl.glBegin(GL.GL_LINE_LOOP);
 			gl.glVertex3f((-2 - fXCorrection) / fAspectRatio, -2, 4);
 			gl.glVertex3f((-2 - fXCorrection) / fAspectRatio, 2, 4);
 			gl.glVertex3f((-2 - fXCorrection) / fAspectRatio + fWidth, 2, 4);
 			gl.glVertex3f((-2 - fXCorrection) / fAspectRatio + fWidth, -2, 4);
 			gl.glEnd();
 
 			gl.glPopName();
 
 			// Render memo pad background
 			gl.glColor4fv(GeneralRenderStyle.PANEL_BACKGROUN_COLOR, 0);
 			gl.glLineWidth(1);
 			gl.glBegin(GL.GL_POLYGON);
 			gl.glVertex3f((2 + fXCorrection) / fAspectRatio, -2, 4);
 			gl.glVertex3f((2 + fXCorrection) / fAspectRatio, 2, 4);
 			gl.glVertex3f((2 + fXCorrection) / fAspectRatio - fWidth, 2, 4);
 			gl.glVertex3f((2 + fXCorrection) / fAspectRatio - fWidth, -2, 4);
 			gl.glEnd();
 
 			gl.glColor4f(0.4f, 0.4f, 0.4f, 1);
 			gl.glLineWidth(1);
 			gl.glBegin(GL.GL_LINE_LOOP);
 			gl.glVertex3f((2 + fXCorrection) / fAspectRatio, -2, 4);
 			gl.glVertex3f((2 + fXCorrection) / fAspectRatio, 2, 4);
 			gl.glVertex3f((2 + fXCorrection) / fAspectRatio - fWidth, 2, 4);
 			gl.glVertex3f((2 + fXCorrection) / fAspectRatio - fWidth, -2, 4);
 			gl.glEnd();
 		}
 
 		// Render caption
 		if (textRenderer == null)
 			return;
 
 		String sTmp = "POOL AREA";
 		textRenderer.begin3DRendering();
 		textRenderer.setColor(0.6f, 0.6f, 0.6f, 1.0f);
 		textRenderer.draw3D(sTmp, (-1.9f - fXCorrection) / fAspectRatio, -1.97f, 4.001f, 0.003f);
 		textRenderer.end3DRendering();
 	}
 
 	@Override
 	public void triggerEvent(EMediatorType eMediatorType, IEventContainer eventContainer) {
 		generalManager.getEventPublisher().triggerEvent(eMediatorType, this, eventContainer);
 
 	}
 
 	@Override
 	public synchronized void broadcastElements(EVAOperation type) {
 		// do nothing
 	}
 
 	/**
 	 * Adds new remote-rendered-views that have been queued for displaying to this view. Only one view is
 	 * taken from the list and added for remote rendering per call to this method.
 	 * 
 	 * @param GL
 	 */
 	private synchronized void initNewView(GL gl) {
 		if (arSlerpActions.isEmpty()) {
 			if (!newViews.isEmpty()) {
 				ASerializedView serView = newViews.remove(0);
 				AGLEventListener view = createView(gl, serView);
 				if (hasFreeViewPosition()) {
 					addSlerpActionForView(gl, view);
 					containedViewIDs.add(view.getID());
 				}
 				else {
 					newViews.clear();
 				}
 				if (newViews.isEmpty()) {
 					triggerToolBarUpdate();
 					enableUserInteraction();
 				}
 			}
 		}
 	}
 
 	/**
 	 * Triggers a toolbar update by sending an event similar to the view activation
 	 */
 	private void triggerToolBarUpdate() {
 		log.info("triggerToolBarUpdate() called");
 
 		ViewActivationEvent viewActivationEvent = new ViewActivationEvent();
 		List<Integer> viewIDs = this.getAllViewIDs();
 		viewActivationEvent.setViewIDs(viewIDs);
 
 		IEventPublisher eventPublisher = GeneralManager.get().getEventPublisher();
 		eventPublisher.triggerEvent(viewActivationEvent);
 	}
 
 	/**
 	 * Checks if this view has some space left to add at least 1 view
 	 * 
 	 * @return <code>true</code> if some space is left, <code>false</code> otherwise
 	 */
 	public boolean hasFreeViewPosition() {
 		return focusLevel.hasFreePosition()
 			|| (stackLevel.hasFreePosition() && !(layoutRenderStyle instanceof ListLayoutRenderStyle))
 			|| poolLevel.hasFreePosition();
 	}
 
 	/**
 	 * Adds a Slerp-Transition for a view. Usually this is used when a new view is added to the bucket or 2
 	 * views change its position in the bucket. The operation does not always succeed. A reason for this is
 	 * when no more space is left to slerp the given view to.
 	 * 
 	 * @param gl
 	 * @param view
 	 *            the view for which the slerp transition should be added
 	 * @return <code>true</code> if adding the slerp action was successfull, <code>false</code> otherwise
 	 */
 	private boolean addSlerpActionForView(GL gl, AGLEventListener view) {
 
 		RemoteLevelElement origin = spawnLevel.getElementByPositionIndex(0);
 		RemoteLevelElement destination = null;
 
 		if (focusLevel.hasFreePosition()) {
 			destination = focusLevel.getNextFree();
 			view.broadcastElements(EVAOperation.APPEND_UNIQUE);
 		}
 		else if (stackLevel.hasFreePosition() && !(layoutRenderStyle instanceof ListLayoutRenderStyle)) {
 			destination = stackLevel.getNextFree();
 			view.broadcastElements(EVAOperation.APPEND_UNIQUE);
 		}
 		else if (poolLevel.hasFreePosition()) {
 			destination = poolLevel.getNextFree();
 		}
 		else {
 			log.log(Level.SEVERE, "No empty space left to add new pathway!");
 			newViews.clear();
 			return false;
 		}
 
 		origin.setContainedElementID(view.getID());
 		SlerpAction slerpActionTransition = new SlerpAction(origin, destination);
 		arSlerpActions.add(slerpActionTransition);
 
 		view.initRemote(gl, iUniqueID, pickingTriggerMouseAdapter, this, infoAreaManager);
 		view.setDetailLevel(EDetailLevel.MEDIUM);
 
 		return true;
 	}
 
 	/**
 	 * Creates and initializes a new view based on its serialized form. The view is already added to the list
 	 * of event receivers and senders.
 	 * 
 	 * @param gl
 	 * @param serView
 	 *            serialized form of the view to create
 	 * @return the created view ready to be used within the application
 	 */
 	private AGLEventListener createView(GL gl, ASerializedView serView) {
 
 		ArrayList<Integer> iAlSetIDs = new ArrayList<Integer>();
 		for (ISet tmpSet : alSets) {
 			if (tmpSet.getSetType() != ESetType.GENE_EXPRESSION_DATA)
 				continue;
 			iAlSetIDs.add(tmpSet.getID());
 		}
 
 		ICommandManager cm = generalManager.getCommandManager();
 		ECommandType cmdType = serView.getCreationCommandType();
 		CmdCreateGLEventListener cmdView = (CmdCreateGLEventListener) cm.createCommandByType(cmdType);
 		cmdView.setAttributesFromSerializedForm(serView);
 		cmdView.setSetIDs(iAlSetIDs);
 		cmdView.doCommand();
 		AGLEventListener glView = cmdView.getCreatedObject();
 
 		if (glView instanceof GLPathway) {
 			initializePathwayView((GLPathway) glView);
 		}
 
 		IEventPublisher eventPublisher = generalManager.getEventPublisher();
 		eventPublisher.addSender(EMediatorType.SELECTION_MEDIATOR, (IMediatorSender) glView);
 		eventPublisher.addReceiver(EMediatorType.SELECTION_MEDIATOR, (IMediatorReceiver) glView);
 
 		triggerMostRecentDelta();
 
 		return glView;
 	}
 
 	/**
 	 * initializes the configuration of a pathway to the configuraion currently stored in this
 	 * remote-renderin-view.
 	 * 
 	 * @param pathway
 	 *            pathway to set the configuration
 	 */
 	private void initializePathwayView(GLPathway pathway) {
 		pathway.enablePathwayTextures(pathwayTexturesEnabled);
 		pathway.enableNeighborhood(neighborhoodEnabled);
 		pathway.enableGeneMapping(geneMappingEnabled);
 	}
 
 	/**
 	 * Triggers the most recent user selection to the views. This is especially needed to initialize new added
 	 * views with the current selection information.
 	 */
 	private void triggerMostRecentDelta() {
 		// Trigger last delta to new views
 		if (lastSelectionDelta != null) {
 			triggerEvent(EMediatorType.SELECTION_MEDIATOR, new DeltaEventContainer<ISelectionDelta>(
 				lastSelectionDelta));
 		}
 	}
 
 	/**
 	 * Disables picking and enables busy mode
 	 */
 	public void disableUserInteraction() {
 		IViewManager canvasManager = generalManager.getViewGLCanvasManager();
 		canvasManager.getPickingManager().enablePicking(false);
 		canvasManager.requestBusyMode(this);
 	}
 
 	/**
 	 * Enables picking and disables busy mode
 	 */
 	public void enableUserInteraction() {
 		IViewManager canvasManager = generalManager.getViewGLCanvasManager();
 		canvasManager.getPickingManager().enablePicking(true);
 		canvasManager.releaseBusyMode(this);
 	}
 
 	public synchronized void loadDependentPathways(final List<ICaleydoGraphItem> alVertex) {
 		// Remove pathways from stacked layer view
 		// poolLayer.removeAllElements();
 
 		Iterator<ICaleydoGraphItem> iterPathwayGraphItem = alVertex.iterator();
 		// Iterator<IGraphItem> iterIdenticalPathwayGraphItemRep = null;
 
 		// set to avoid duplicate pathways
 		Set<PathwayGraph> newPathways = new HashSet<PathwayGraph>();
 
 		while (iterPathwayGraphItem.hasNext()) {
 			IGraphItem pathwayGraphItem = iterPathwayGraphItem.next();
 
 			if (pathwayGraphItem == null) {
 				// generalManager.logMsg(
 				// this.getClass().getSimpleName() + " (" + iUniqueID
 				// + "): pathway graph item is null.  ",
 				// LoggerType.VERBOSE);
 				continue;
 			}
 
 			List<IGraphItem> pathwayItems =
 				pathwayGraphItem.getAllItemsByProp(EGraphItemProperty.ALIAS_CHILD);
 			for (IGraphItem pathwayItem : pathwayItems) {
 				PathwayGraph pathwayGraph =
 					(PathwayGraph) pathwayItem.getAllGraphByType(EGraphItemHierarchy.GRAPH_PARENT).get(0);
 				newPathways.add(pathwayGraph);
 			}
 
 			// iterIdenticalPathwayGraphItemRep =
 			// pathwayGraphItem.getAllItemsByProp(EGraphItemProperty.ALIAS_CHILD).iterator();
 			//
 			// while (iterIdenticalPathwayGraphItemRep.hasNext()) {
 			// int pathwayID =
 			// ((PathwayGraph) iterIdenticalPathwayGraphItemRep.next().getAllGraphByType(
 			// EGraphItemHierarchy.GRAPH_PARENT).toArray()[0]).getId();
 			// newPathwayIDs.add(pathwayID);
 			// }
 
 		}
 
 		// add new pathways to bucket
 		for (PathwayGraph pathway : newPathways) {
 			addPathwayView(pathway.getID());
 		}
 
 		if (!newViews.isEmpty()) {
 			// Zoom out of the bucket when loading pathways
 			if (bucketMouseWheelListener.isZoomedIn()) {
 				bucketMouseWheelListener.triggerZoom(false);
 			}
 			disableUserInteraction();
 		}
 
 	}
 
 	/*
 	 * private synchronized void initNewPathway(final GL gl, SerializedPathwayView pathway) { int
 	 * iTmpPathwayID = pathway.getPathwayID(); // Check if pathway is already loaded in bucket if
 	 * (!generalManager.getPathwayManager().isPathwayVisible(iTmpPathwayID)) { ArrayList<Integer> iAlSetIDs =
 	 * new ArrayList<Integer>(); for (ISet tmpSet : alSets) { if (tmpSet.getSetType() !=
 	 * ESetType.GENE_EXPRESSION_DATA) { continue; } iAlSetIDs.add(tmpSet.getID()); } // Create Pathway3D view
 	 * CmdCreateGLPathway cmdPathway = (CmdCreateGLPathway)
 	 * generalManager.getCommandManager().createCommandByType( ECommandType.CREATE_GL_PATHWAY_3D); //
 	 * cmdPathway.setAttributes(iTmpPathwayID, iAlSetIDs, EProjectionMode.ORTHOGRAPHIC, -4, 4, 4, -4, -20,
 	 * 20); cmdPathway.setPathwayID(iTmpPathwayID); cmdPathway.setViewFrustum(pathway.getViewFrustum());
 	 * cmdPathway.setSetIDs(iAlSetIDs); cmdPathway.doCommand(); GLPathway glPathway = (GLPathway)
 	 * cmdPathway.getCreatedObject(); int iGeneratedViewID = glPathway.getID();
 	 * GeneralManager.get().getEventPublisher().addSender(EMediatorType.SELECTION_MEDIATOR, (IMediatorSender)
 	 * glPathway); GeneralManager.get().getEventPublisher().addReceiver(EMediatorType.SELECTION_MEDIATOR,
 	 * (IMediatorReceiver) glPathway); iAlContainedViewIDs.add(iGeneratedViewID); // Trigger last delta to new
 	 * views if (lastSelectionDelta != null) { triggerEvent(EMediatorType.SELECTION_MEDIATOR, new
 	 * DeltaEventContainer<ISelectionDelta>( lastSelectionDelta)); } if (focusLevel.hasFreePosition()) {
 	 * spawnLevel.getElementByPositionIndex(0).setContainedElementID(iGeneratedViewID); SlerpAction
 	 * slerpActionTransition = new SlerpAction(spawnLevel.getElementByPositionIndex(0),
 	 * focusLevel.getNextFree()); arSlerpActions.add(slerpActionTransition); glPathway.initRemote(gl,
 	 * iUniqueID, pickingTriggerMouseAdapter, this); glPathway.setDetailLevel(EDetailLevel.MEDIUM); // Trigger
 	 * initial gene propagation glPathway.broadcastElements(EVAOperation.APPEND_UNIQUE); } else if
 	 * (stackLevel.hasFreePosition() && !(layoutRenderStyle instanceof ListLayoutRenderStyle)) {
 	 * spawnLevel.getElementByPositionIndex(0).setContainedElementID(iGeneratedViewID); SlerpAction
 	 * slerpActionTransition = new SlerpAction(spawnLevel.getElementByPositionIndex(0),
 	 * stackLevel.getNextFree()); arSlerpActions.add(slerpActionTransition); glPathway.initRemote(gl,
 	 * iUniqueID, pickingTriggerMouseAdapter, this); glPathway.setDetailLevel(EDetailLevel.LOW); // Trigger
 	 * initial gene propagation glPathway.broadcastElements(EVAOperation.APPEND_UNIQUE); } else if
 	 * (poolLevel.hasFreePosition()) {
 	 * spawnLevel.getElementByPositionIndex(0).setContainedElementID(iGeneratedViewID); SlerpAction
 	 * slerpActionTransition = new SlerpAction(spawnLevel.getElementByPositionIndex(0),
 	 * poolLevel.getNextFree()); arSlerpActions.add(slerpActionTransition); glPathway.initRemote(gl,
 	 * iUniqueID, pickingTriggerMouseAdapter, this); glPathway.setDetailLevel(EDetailLevel.VERY_LOW); } else {
 	 * generalManager.getLogger().log(Level.SEVERE, "No empty space left to add new pathway!");
 	 * iAlUninitializedPathwayIDs.clear(); for (AGLEventListener eventListener :
 	 * generalManager.getViewGLCanvasManager() .getAllGLEventListeners()) { if
 	 * (!eventListener.isRenderedRemote()) { eventListener.enableBusyMode(false); } } // Enable picking after
 	 * all pathways are loaded
 	 * generalManager.getViewGLCanvasManager().getPickingManager().enablePicking(true); return; } } else {
 	 * generalManager.getLogger().log(Level.WARNING, "Pathway with ID: " + iTmpPathwayID +
 	 * " is already loaded in Bucket."); } iAlUninitializedPathwayIDs.remove(0); if
 	 * (iAlUninitializedPathwayIDs.isEmpty()) { // Enable picking after all pathways are loaded
 	 * generalManager.getViewGLCanvasManager().getPickingManager().enablePicking(true); for (AGLEventListener
 	 * eventListener : generalManager.getViewGLCanvasManager() .getAllGLEventListeners()) { if
 	 * (!eventListener.isRenderedRemote()) { eventListener.enableBusyMode(false); } } } }
 	 */
 
 	@Override
 	public void enableBusyMode(boolean busyMode) {
 		super.enableBusyMode(busyMode);
 
 		if (eBusyModeState == EBusyModeState.ON) {
 			parentGLCanvas.removeMouseListener(bucketMouseWheelListener);
 			parentGLCanvas.removeMouseWheelListener(bucketMouseWheelListener);
 		}
 		else {
 			parentGLCanvas.addMouseListener(bucketMouseWheelListener);
 			parentGLCanvas.addMouseWheelListener(bucketMouseWheelListener);
 		}
 	}
 
 	@Override
 	public int getNumberOfSelections(ESelectionType eSelectionType) {
 		return 0;
 	}
 
 	private void compactPoolLevel() {
 		RemoteLevelElement element;
 		RemoteLevelElement elementInner;
 		for (int iIndex = 0; iIndex < poolLevel.getCapacity(); iIndex++) {
 			element = poolLevel.getElementByPositionIndex(iIndex);
 			if (element.isFree()) {
 				// Search for next element to put it in the free position
 				for (int iInnerIndex = iIndex + 1; iInnerIndex < poolLevel.getCapacity(); iInnerIndex++) {
 					elementInner = poolLevel.getElementByPositionIndex(iInnerIndex);
 
 					if (elementInner.isFree()) {
 						continue;
 					}
 
 					element.setContainedElementID(elementInner.getContainedElementID());
 					elementInner.setContainedElementID(-1);
 
 					break;
 				}
 			}
 		}
 	}
 
 	public ArrayList<Integer> getRemoteRenderedViews() {
 		return containedViewIDs;
 	}
 
 	private void updateOffScreenTextures(final GL gl) {
 		bUpdateOffScreenTextures = false;
 
 		gl.glPushMatrix();
 
 		int iViewWidth = parentGLCanvas.getWidth();
 		int iViewHeight = parentGLCanvas.getHeight();
 
 		if (stackLevel.getElementByPositionIndex(0).getContainedElementID() != -1) {
 			glOffScreenRenderer.renderToTexture(gl, stackLevel.getElementByPositionIndex(0)
 				.getContainedElementID(), 0, iViewWidth, iViewHeight);
 		}
 
 		if (stackLevel.getElementByPositionIndex(1).getContainedElementID() != -1) {
 			glOffScreenRenderer.renderToTexture(gl, stackLevel.getElementByPositionIndex(1)
 				.getContainedElementID(), 1, iViewWidth, iViewHeight);
 		}
 
 		if (stackLevel.getElementByPositionIndex(2).getContainedElementID() != -1) {
 			glOffScreenRenderer.renderToTexture(gl, stackLevel.getElementByPositionIndex(2)
 				.getContainedElementID(), 2, iViewWidth, iViewHeight);
 		}
 
 		if (stackLevel.getElementByPositionIndex(3).getContainedElementID() != -1) {
 			glOffScreenRenderer.renderToTexture(gl, stackLevel.getElementByPositionIndex(3)
 				.getContainedElementID(), 3, iViewWidth, iViewHeight);
 		}
 
 		gl.glPopMatrix();
 	}
 
 	@Override
 	public void clearAllSelections() {
 		for (Integer iViewID : containedViewIDs) {
 			generalManager.getViewGLCanvasManager().getGLEventListener(iViewID).clearAllSelections();
 		}
 
 	}
 
 	@Override
 	public void destroy() {
 		super.destroy();
 		unregisterEventListeners();
 	}
 
 	/**
 	 * FIXME: should be moved to a bucket-mediator registers the event-listeners to the event framework
 	 */
 	public void registerEventListeners() {
 		IEventPublisher eventPublisher = generalManager.getEventPublisher();
 
 		addPathwayListener = new AddPathwayListener();
 		addPathwayListener.setBucket(this);
 		eventPublisher.addListener(LoadPathwayEvent.class, addPathwayListener);
 
 		loadPathwaysByGeneListener = new LoadPathwaysByGeneListener();
 		loadPathwaysByGeneListener.setBucket(this);
 		eventPublisher.addListener(LoadPathwaysByGeneEvent.class, loadPathwaysByGeneListener);
 
 		enableTexturesListener = new EnableTexturesListener();
 		enableTexturesListener.setBucket(this);
 		eventPublisher.addListener(EnableTexturesEvent.class, enableTexturesListener);
 
 		disableTexturesListener = new DisableTexturesListener();
 		disableTexturesListener.setBucket(this);
 		eventPublisher.addListener(DisableTexturesEvent.class, disableTexturesListener);
 
 		enableGeneMappingListener = new EnableGeneMappingListener();
 		enableGeneMappingListener.setBucket(this);
 		eventPublisher.addListener(EnableGeneMappingEvent.class, enableGeneMappingListener);
 
 		disableGeneMappingListener = new DisableGeneMappingListener();
 		disableGeneMappingListener.setBucket(this);
 		eventPublisher.addListener(DisableGeneMappingEvent.class, disableGeneMappingListener);
 
 		enableNeighborhoodListener = new EnableNeighborhoodListener();
 		enableNeighborhoodListener.setBucket(this);
 		eventPublisher.addListener(EnableNeighborhoodEvent.class, enableNeighborhoodListener);
 
 		disableNeighborhoodListener = new DisableNeighborhoodListener();
 		disableNeighborhoodListener.setBucket(this);
 		eventPublisher.addListener(DisableNeighborhoodEvent.class, disableNeighborhoodListener);
 
 		enableConnectionLinesListener = new EnableConnectionLinesListener();
 		enableConnectionLinesListener.setBucket(this);
 		eventPublisher.addListener(EnableConnectionLinesEvent.class, enableConnectionLinesListener);
 
 		disableConnectionLinesListener = new DisableConnectionLinesListener();
 		disableConnectionLinesListener.setBucket(this);
 		eventPublisher.addListener(DisableConnectionLinesEvent.class, disableConnectionLinesListener);
 
 		closeOrResetViewsListener = new CloseOrResetViewsListener();
 		closeOrResetViewsListener.setBucket(this);
 		eventPublisher.addListener(CloseOrResetViewsEvent.class, closeOrResetViewsListener);
 
 	}
 
 	/**
 	 * FIXME: should be moved to a bucket-mediator registers the event-listeners to the event framework
 	 */
 	public void unregisterEventListeners() {
 		IEventPublisher eventPublisher = generalManager.getEventPublisher();
 
 		if (addPathwayListener != null) {
 			eventPublisher.removeListener(addPathwayListener);
 			addPathwayListener = null;
 		}
 
 		if (loadPathwaysByGeneListener != null) {
 			eventPublisher.removeListener(loadPathwaysByGeneListener);
 			loadPathwaysByGeneListener = null;
 		}
 		if (enableTexturesListener != null) {
 			eventPublisher.removeListener(enableTexturesListener);
 			enableTexturesListener = null;
 		}
 		if (disableTexturesListener != null) {
 			eventPublisher.removeListener(disableTexturesListener);
 			disableTexturesListener = null;
 		}
 		if (enableGeneMappingListener != null) {
 			eventPublisher.removeListener(enableGeneMappingListener);
 			enableGeneMappingListener = null;
 		}
 		if (disableGeneMappingListener != null) {
 			eventPublisher.removeListener(disableGeneMappingListener);
 			disableGeneMappingListener = null;
 		}
 		if (enableNeighborhoodListener != null) {
 			eventPublisher.removeListener(enableNeighborhoodListener);
 			enableNeighborhoodListener = null;
 		}
 		if (disableNeighborhoodListener != null) {
 			eventPublisher.removeListener(disableNeighborhoodListener);
 			disableNeighborhoodListener = null;
 		}
 		if (closeOrResetViewsListener != null) {
 			eventPublisher.removeListener(closeOrResetViewsListener);
 			closeOrResetViewsListener = null;
 		}
 
 	}
 
 	@Override
 	public ASerializedView getSerializableRepresentation() {
 		SerializedRemoteRenderingView serializedForm = new SerializedRemoteRenderingView();
 		serializedForm.setViewID(this.getID());
 		serializedForm.setPathwayTexturesEnabled(pathwayTexturesEnabled);
 		serializedForm.setNeighborhoodEnabled(neighborhoodEnabled);
 		serializedForm.setGeneMappingEnabled(geneMappingEnabled);
 		serializedForm.setConnectionLinesEnabled(connectionLinesEnabled);
 		return serializedForm; 
 	}
 
 	public boolean isGeneMappingEnabled() {
 		return geneMappingEnabled;
 	}
 
 	public void setGeneMappingEnabled(boolean geneMappingEnabled) {
 		this.geneMappingEnabled = geneMappingEnabled;
 	}
 
 	public boolean isPathwayTexturesEnabled() {
 		return pathwayTexturesEnabled;
 	}
 
 	public void setPathwayTexturesEnabled(boolean pathwayTexturesEnabled) {
 		this.pathwayTexturesEnabled = pathwayTexturesEnabled;
 	}
 
 	public boolean isNeighborhoodEnabled() {
 		return neighborhoodEnabled;
 	}
 
 	public void setNeighborhoodEnabled(boolean neighborhoodEnabled) {
 		this.neighborhoodEnabled = neighborhoodEnabled;
 	}
 
 	public boolean isConnectionLinesEnabled() {
 		return connectionLinesEnabled;
 	}
 
 	public void setConnectionLinesEnabled(boolean connectionLinesEnabled) {
 		this.connectionLinesEnabled = connectionLinesEnabled;
 	}
 
 }
