 package org.caleydo.core.view.opengl.canvas.pathway;
 
 import gleem.linalg.Vec3f;
 import java.awt.Font;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.logging.Level;
 import javax.media.opengl.GL;
 import org.caleydo.core.data.IUniqueObject;
 import org.caleydo.core.data.collection.ISet;
 import org.caleydo.core.data.graph.pathway.core.PathwayGraph;
 import org.caleydo.core.data.graph.pathway.item.vertex.EPathwayVertexType;
 import org.caleydo.core.data.graph.pathway.item.vertex.PathwayVertexGraphItem;
 import org.caleydo.core.data.graph.pathway.item.vertex.PathwayVertexGraphItemRep;
 import org.caleydo.core.data.mapping.EIDType;
 import org.caleydo.core.data.selection.ESelectionCommandType;
 import org.caleydo.core.data.selection.ESelectionType;
 import org.caleydo.core.data.selection.GenericSelectionManager;
 import org.caleydo.core.data.selection.ISelectionDelta;
 import org.caleydo.core.data.selection.SelectedElementRep;
 import org.caleydo.core.data.selection.SelectionCommand;
 import org.caleydo.core.data.selection.SelectionDelta;
 import org.caleydo.core.data.selection.SelectionItem;
 import org.caleydo.core.manager.event.mediator.EMediatorType;
 import org.caleydo.core.manager.event.mediator.IMediatorReceiver;
 import org.caleydo.core.manager.event.mediator.IMediatorSender;
 import org.caleydo.core.manager.id.EManagedObjectType;
 import org.caleydo.core.manager.picking.EPickingMode;
 import org.caleydo.core.manager.picking.EPickingType;
 import org.caleydo.core.manager.picking.Pick;
 import org.caleydo.core.manager.specialized.genome.IPathwayItemManager;
 import org.caleydo.core.manager.specialized.genome.IPathwayManager;
 import org.caleydo.core.manager.specialized.genome.pathway.EPathwayDatabaseType;
 import org.caleydo.core.manager.view.ConnectedElementRepresentationManager;
 import org.caleydo.core.view.opengl.camera.IViewFrustum;
 import org.caleydo.core.view.opengl.canvas.AGLEventListener;
 import org.caleydo.core.view.opengl.canvas.EDetailLevel;
 import org.caleydo.core.view.opengl.canvas.remote.IGLCanvasRemoteRendering;
 import org.caleydo.core.view.opengl.mouse.PickingJoglMouseListener;
 import org.caleydo.util.graph.EGraphItemKind;
 import org.caleydo.util.graph.EGraphItemProperty;
 import org.caleydo.util.graph.IGraphItem;
 import org.eclipse.swt.SWT;
 import com.sun.opengl.util.j2d.TextRenderer;
 
 /**
  * Single OpenGL pathway view
  * 
  * @author Marc Streit
  * @author Alexander Lex
  */
 public class GLPathway
 	extends AGLEventListener
 	implements IMediatorReceiver, IMediatorSender
 {
 	private int iPathwayID = -1;
 
 	private boolean bEnablePathwayTexture = true;
 
 	private IPathwayManager pathwayManager;
 
 	private GLPathwayContentCreator gLPathwayContentCreator;
 
 	private ConnectedElementRepresentationManager connectedElementRepresentationManager;
 
 	private GenericSelectionManager selectionManager;
 
 	/**
 	 * Own texture manager is needed for each GL context, because textures
 	 * cannot be bound to multiple GL contexts.
 	 */
 	private HashMap<GL, GLPathwayTextureManager> hashGLcontext2TextureManager;
 
 	private Vec3f vecScaling;
 	private Vec3f vecTranslation;
 
 	private TextRenderer textRenderer;
 	private boolean bEnableTitleRendering = true;
 	private int iHorizontalTextAlignment = SWT.CENTER;
 	private int iVerticalTextAlignment = SWT.BOTTOM;
 
 	/**
 	 * Constructor.
 	 */
 	public GLPathway(final int iGLCanvasID, final String sLabel, final IViewFrustum viewFrustum)
 	{
 		super(iGLCanvasID, sLabel, viewFrustum, false);
 		viewType = EManagedObjectType.GL_PATHWAY;
 		pathwayManager = generalManager.getPathwayManager();
 
 		gLPathwayContentCreator = new GLPathwayContentCreator(viewFrustum);
 		hashGLcontext2TextureManager = new HashMap<GL, GLPathwayTextureManager>();
 		// hashPathwayContainingSelectedVertex2VertexCount = new
 		// HashMap<Integer, Integer>();
 
 		connectedElementRepresentationManager = generalManager.getViewGLCanvasManager()
 				.getConnectedElementRepresentationManager();
 
 		vecScaling = new Vec3f(1, 1, 1);
 		vecTranslation = new Vec3f(0, 0, 0);
 
 		// initialize internal gene selection manager
 		ArrayList<ESelectionType> alSelectionType = new ArrayList<ESelectionType>();
 		for (ESelectionType selectionType : ESelectionType.values())
 		{
 			alSelectionType.add(selectionType);
 		}
 
 		selectionManager = new GenericSelectionManager.Builder(EIDType.PATHWAY_VERTEX).build();
 
 		textRenderer = new TextRenderer(new Font("Arial", Font.BOLD, 24), false);
 	}
 
 	public synchronized void setPathwayID(final int iPathwayID)
 	{
 		// Unregister former pathway in visibility list
 		if (iPathwayID != -1)
 			generalManager.getPathwayManager().setPathwayVisibilityStateByID(this.iPathwayID,
 					false);
 
 		this.iPathwayID = iPathwayID;
 	}
 
 	public int getPathwayID()
 	{
 
 		return iPathwayID;
 	}
 
 	@Override
 	public void initLocal(final GL gl)
 	{
 		iGLDisplayListIndexLocal = gl.glGenLists(1);
 		iGLDisplayListToCall = iGLDisplayListIndexLocal;
 		init(gl);
 		pickingTriggerMouseAdapter.resetEvents();
 		// TODO: individual toolboxrenderer
 	}
 
 	@Override
 	public void initRemote(final GL gl, final int iRemoteViewID, final PickingJoglMouseListener pickingTriggerMouseAdapter,
 			final IGLCanvasRemoteRendering remoteRenderingGLCanvas)
 	{
 		this.remoteRenderingGLCanvas = remoteRenderingGLCanvas;
 		this.pickingTriggerMouseAdapter = pickingTriggerMouseAdapter;
 
 		iGLDisplayListIndexRemote = gl.glGenLists(1);
 		iGLDisplayListToCall = iGLDisplayListIndexRemote;
 		init(gl);
 	}
 
 	@Override
 	public void init(final GL gl)
 	{
 		// Check if pathway exists or if it's already loaded
 		if (!generalManager.getPathwayManager().hasItem(iPathwayID))
 			return;
 
 		initPathwayData(gl);
 	}
 
 	@Override
 	public synchronized void displayLocal(final GL gl)
 	{
 		// Check if pathway exists or if it's already loaded
 		// FIXME: not good because check in every rendered frame
 		if (!generalManager.getPathwayManager().hasItem(iPathwayID))
 			return;
 
 		pickingManager.handlePicking(iUniqueID, gl, false);
 		if (bIsDisplayListDirtyLocal)
 		{
 			rebuildPathwayDisplayList(gl, iGLDisplayListIndexLocal);
 			bIsDisplayListDirtyLocal = false;
 		}
 		iGLDisplayListToCall = iGLDisplayListIndexLocal;
 		display(gl);
 		pickingTriggerMouseAdapter.resetEvents();
 	}
 
 	@Override
 	public synchronized void displayRemote(final GL gl)
 	{
 		// Check if pathway exists or if it is already loaded
 		// FIXME: not good because check in every rendered frame
 		if (!generalManager.getPathwayManager().hasItem(iPathwayID))
 			return;
 
 		if (bIsDisplayListDirtyRemote)
 		{
 			rebuildPathwayDisplayList(gl, iGLDisplayListIndexRemote);
 			bIsDisplayListDirtyRemote = false;
 		}
 		iGLDisplayListToCall = iGLDisplayListIndexRemote;
 		display(gl);
 	}
 
 	@Override
 	public synchronized void display(final GL gl)
 	{
 		checkForHits(gl);
 
 		// TODO: also put this in global DL
 		renderPathwayById(gl, iPathwayID);
 
 		gl.glCallList(iGLDisplayListToCall);
 	}
 
 	protected void initPathwayData(final GL gl)
 	{
 		// Initialize all elements in selection manager
 		Iterator<IGraphItem> iterPathwayVertexGraphItem = (generalManager.getPathwayManager()
 				.getItem(iPathwayID)).getAllItemsByKind(EGraphItemKind.NODE).iterator();
 		PathwayVertexGraphItemRep tmpPathwayVertexGraphItemRep = null;
 		while (iterPathwayVertexGraphItem.hasNext())
 		{
 			tmpPathwayVertexGraphItemRep = ((PathwayVertexGraphItemRep) iterPathwayVertexGraphItem
 					.next());
 
 			selectionManager.initialAdd(tmpPathwayVertexGraphItemRep.getId());
 		}
 
 		gLPathwayContentCreator.init(gl, alSets, selectionManager);
 
 		// Create new pathway manager for GL context
 		if (!hashGLcontext2TextureManager.containsKey(gl))
 		{
 			hashGLcontext2TextureManager.put(gl, new GLPathwayTextureManager());
 		}
 
 		calculatePathwayScaling(gl, iPathwayID);
 		pathwayManager.setPathwayVisibilityStateByID(iPathwayID, true);
 
 		// gLPathwayContentCreator.buildPathwayDisplayList(gl, this,
 		// iPathwayID);
 	}
 
 	private void renderPathwayById(final GL gl, final int iPathwayId)
 	{
 		gl.glPushMatrix();
 		gl.glTranslatef(vecTranslation.x(), vecTranslation.y(), vecTranslation.z());
 		gl.glScalef(vecScaling.x(), vecScaling.y(), vecScaling.z());
 
 		if (bEnablePathwayTexture)
 		{
 			float fPathwayTransparency = 1.0f;
 
 			hashGLcontext2TextureManager.get(gl).renderPathway(gl, this, iPathwayId,
 					fPathwayTransparency, false);
 		}
 
 		float tmp = PathwayRenderStyle.SCALING_FACTOR_Y
 				* (pathwayManager.getItem(iPathwayId)).getHeight();
 
 		// Pathway texture height is subtracted from Y to align pathways to
 		// front level
 		gl.glTranslatef(0, tmp, 0);
 
 		if (remoteRenderingGLCanvas.getBucketMouseWheelListener() != null)
 		{
 			// if
 			// (remoteRenderingGLCanvas.getHierarchyLayerByGLEventListenerId(iUniqueID)
 			// .getLevel().equals(EHierarchyLevel.UNDER_INTERACTION)
 			// &&
 			// remoteRenderingGLCanvas.getBucketMouseWheelListener().isZoomedIn())
 			if (detailLevel == EDetailLevel.HIGH)
 			{
 				gLPathwayContentCreator.renderPathway(gl, iPathwayId, true);
 			}
 			else
 			{
 				gLPathwayContentCreator.renderPathway(gl, iPathwayId, false);
 			}
 		}
 		else
 		{
 			gLPathwayContentCreator.renderPathway(gl, iPathwayId, false);
 		}
 
 		gl.glTranslatef(0, -tmp, 0);
 
 		gl.glScalef(1 / vecScaling.x(), 1 / vecScaling.y(), 1 / vecScaling.z());
 		gl.glTranslatef(-vecTranslation.x(), -vecTranslation.y(), -vecTranslation.z());
 
 		gl.glPopMatrix();
 	}
 
 	private void rebuildPathwayDisplayList(final GL gl, int iGLDisplayListIndex)
 	{
 		gLPathwayContentCreator.buildPathwayDisplayList(gl, this, iPathwayID);
 
 		gl.glNewList(iGLDisplayListIndex, GL.GL_COMPILE);
 		renderPathwayName(gl);
 		gl.glEndList();
 	}
 
 	@Override
 	public synchronized void handleUpdate(IUniqueObject eventTrigger,
 			ISelectionDelta selectionDelta, Collection<SelectionCommand> colSelectionCommand,
 			EMediatorType eMediatorType)
 	{
 		generalManager.getLogger().log(
 				Level.INFO,
 				"Update called by " + eventTrigger.getClass().getSimpleName()
 						+ ", received in: " + this.getClass().getSimpleName());
 
 		if (selectionDelta.getIDType() != EIDType.DAVID)
 			return;
 
 		selectionManager.executeSelectionCommands(colSelectionCommand);
 
 		for (SelectionItem item : selectionDelta)
 		{
 			// Ignore ADD and REMOVE in pathway
 			if (item.getSelectionType() == ESelectionType.ADD
 					|| item.getSelectionType() == ESelectionType.REMOVE)
 			{
 				return;
 			}
 		}
 
 		ISelectionDelta resolvedDelta = resolveExternalSelectionDelta(selectionDelta);
 		selectionManager.setDelta(resolvedDelta);
 
 		setDisplayListDirty();
 
 		int iPathwayHeight = (generalManager.getPathwayManager().getItem(iPathwayID))
 				.getHeight();
 		for (SelectionItem item : resolvedDelta)
 		{
 			if (item.getSelectionType() != ESelectionType.MOUSE_OVER)
 				continue;
 
 			PathwayVertexGraphItemRep vertexRep = (PathwayVertexGraphItemRep) generalManager
 					.getPathwayItemManager().getItem(item.getSelectionID());
 
 			SelectedElementRep elementRep = new SelectedElementRep(
 					EIDType.EXPRESSION_INDEX,
 					iUniqueID,
 					(vertexRep.getXOrigin() * PathwayRenderStyle.SCALING_FACTOR_X)
 							* vecScaling.x() + vecTranslation.x(),
 					((iPathwayHeight - vertexRep.getYOrigin()) * PathwayRenderStyle.SCALING_FACTOR_Y)
 							* vecScaling.y() + vecTranslation.y(), 0);
 
 			for (Integer iConnectionID : item.getConnectionID())
 			{
 				connectedElementRepresentationManager.addSelection(iConnectionID, elementRep);
 			}
 		}
 	}
 
 	private ISelectionDelta createExternalSelectionDelta(ISelectionDelta selectionDelta)
 	{
 		ISelectionDelta newSelectionDelta = new SelectionDelta(EIDType.DAVID);
 
 		IPathwayItemManager pathwayItemManager = generalManager.getPathwayItemManager();
 		int iDavidID = 0;
 		for (SelectionItem item : selectionDelta)
 		{
 			for (IGraphItem pathwayVertexGraphItem : pathwayItemManager.getItem(
 					item.getSelectionID()).getAllItemsByProp(EGraphItemProperty.ALIAS_PARENT))
 			{
 				iDavidID = generalManager.getPathwayItemManager()
 						.getDavidIdByPathwayVertexGraphItemId(pathwayVertexGraphItem.getId());
 
 				if (iDavidID == -1)
 					continue;
 
 				// // Ignore multiple nodes with same DAVID ID
 				// if (iLastDavidID == iDavidID)
 				// continue;
 
 				newSelectionDelta.addSelection(iDavidID, item.getSelectionType(), item
 						.getSelectionID());
 				for (Integer iConnectionID : item.getConnectionID())
 				{
 					newSelectionDelta.addConnectionID(iDavidID, iConnectionID);
 				}
 				// System.out.println("ExternalID: " + iDavidID + ", Internal: "
 				// + item.getSelectionID() + ", State: " +
 				// item.getSelectionType());
 
 			}
 		}
 
 		return newSelectionDelta;
 
 	}
 
 	private ISelectionDelta resolveExternalSelectionDelta(ISelectionDelta selectionDelta)
 	{
 		ISelectionDelta newSelectionDelta = new SelectionDelta(EIDType.PATHWAY_VERTEX,
 				EIDType.DAVID);
 
 		int iDavidID = 0;
 		int iPathwayVertexGraphItemID = 0;
 
 		for (SelectionItem item : selectionDelta)
 		{
 			iDavidID = item.getSelectionID();
 
 			iPathwayVertexGraphItemID = generalManager.getPathwayItemManager()
 					.getPathwayVertexGraphItemIdByDavidId(iDavidID);
 
 			// Ignore David IDs that do not exist in any pathway
 			if (iPathwayVertexGraphItemID == -1)
 			{
 				continue;
 			}
 
 			// Convert DAVID ID to pathway graph item representation ID
 			for (IGraphItem tmpGraphItemRep : generalManager.getPathwayItemManager().getItem(
 					iPathwayVertexGraphItemID).getAllItemsByProp(
 					EGraphItemProperty.ALIAS_CHILD))
 			{
 				if (!pathwayManager.getItem(iPathwayID).containsItem(tmpGraphItemRep))
 					continue;
 
 				SelectionItem newItem = newSelectionDelta.addSelection(
 						tmpGraphItemRep.getId(), item.getSelectionType(), iDavidID);
 				for (int iConnectionID : item.getConnectionID())
 				{
 					newItem.setConnectionID(iConnectionID);
 				}
 			}
 		}
 
 		return newSelectionDelta;
 	}
 
 	private void calculatePathwayScaling(final GL gl, final int iPathwayId)
 	{
 
 		if (hashGLcontext2TextureManager.get(gl) == null)
 			return;
 
 		// // Missing power of two texture GL extension workaround
 		// PathwayGraph tmpPathwayGraph =
 		// (PathwayGraph)generalManager.getPathwayManager().getItem(iPathwayId);
 		// ImageIcon img = new ImageIcon(generalManager.getPathwayManager()
 		// .getPathwayDatabaseByType(tmpPathwayGraph.getType()).getImagePath()
 		// + tmpPathwayGraph.getImageLink());
 		// int iImageWidth = img.getIconWidth();
 		// int iImageHeight = img.getIconHeight();
 		// tmpPathwayGraph.setWidth(iImageWidth);
 		// tmpPathwayGraph.setHeight(iImageHeight);
 		// img = null;
 
 		float fPathwayScalingFactor = 0;
 		float fPadding = 0.98f;
 
 		if ((generalManager.getPathwayManager().getItem(iPathwayId)).getType().equals(
 				EPathwayDatabaseType.BIOCARTA))
 		{
 			fPathwayScalingFactor = 5;
 		}
 		else
 		{
 			fPathwayScalingFactor = 3.2f;
 		}
 
 		PathwayGraph tmpPathwayGraph = generalManager.getPathwayManager().getItem(iPathwayId);
 
 		int iImageWidth = tmpPathwayGraph.getWidth();
 		int iImageHeight = tmpPathwayGraph.getHeight();
 
 		generalManager.getLogger().log(Level.FINE,
 				"Pathway texture width=" + iImageWidth + " / height=" + iImageHeight);
 
 		if (iImageWidth == -1 || iImageHeight == -1)
 		{
 			generalManager.getLogger().log(Level.SEVERE,
 					"Problem because pathway texture width or height is invalid!");
 		}
 
 		float fTmpPathwayWidth = iImageWidth * PathwayRenderStyle.SCALING_FACTOR_X
 				* fPathwayScalingFactor;
 		float fTmpPathwayHeight = iImageHeight * PathwayRenderStyle.SCALING_FACTOR_Y
 				* fPathwayScalingFactor;
 
 		if (fTmpPathwayWidth > (viewFrustum.getRight() - viewFrustum.getLeft())
 				&& fTmpPathwayWidth > fTmpPathwayHeight)
 		{
 			vecScaling.setX((viewFrustum.getRight() - viewFrustum.getLeft())
 					/ (iImageWidth * PathwayRenderStyle.SCALING_FACTOR_X) * fPadding);
 			vecScaling.setY(vecScaling.x());
 
 			vecTranslation.set((viewFrustum.getRight() - viewFrustum.getLeft() - iImageWidth
 					* PathwayRenderStyle.SCALING_FACTOR_X * vecScaling.x()) / 2.0f,
 					(viewFrustum.getTop() - viewFrustum.getBottom() - iImageHeight
 							* PathwayRenderStyle.SCALING_FACTOR_Y * vecScaling.y()) / 2.0f, 0);
 		}
 		else if (fTmpPathwayHeight > (viewFrustum.getTop() - viewFrustum.getBottom()))
 		{
 			vecScaling.setY((viewFrustum.getTop() - viewFrustum.getBottom())
 					/ (iImageHeight * PathwayRenderStyle.SCALING_FACTOR_Y) * fPadding);
 			vecScaling.setX(vecScaling.y());
 
 			vecTranslation.set((viewFrustum.getRight() - viewFrustum.getLeft() - iImageWidth
 					* PathwayRenderStyle.SCALING_FACTOR_X * vecScaling.x()) / 2.0f,
 					(viewFrustum.getTop() - viewFrustum.getBottom() - iImageHeight
 							* PathwayRenderStyle.SCALING_FACTOR_Y * vecScaling.y()) / 2.0f, 0);
 		}
 		else
 		{
 			vecScaling.set(fPathwayScalingFactor, fPathwayScalingFactor, 1f);
 
 			vecTranslation.set((viewFrustum.getRight() - viewFrustum.getLeft()) / 2.0f
 					- fTmpPathwayWidth / 2.0f,
 					(viewFrustum.getTop() - viewFrustum.getBottom()) / 2.0f
 							- fTmpPathwayHeight / 2.0f, 0);
 		}
 	}
 
 	public synchronized void setMappingRowCount(final int iMappingRowCount)
 	{
 		gLPathwayContentCreator.setMappingRowCount(iMappingRowCount);
 	}
 
 	public synchronized void enableGeneMapping(final boolean bEnableMapping)
 	{
 		gLPathwayContentCreator.enableGeneMapping(bEnableMapping);
 		setDisplayListDirty();
 	}
 
 	public synchronized void enablePathwayTextures(final boolean bEnablePathwayTexture)
 	{
 		gLPathwayContentCreator.enableEdgeRendering(!bEnablePathwayTexture);
 		setDisplayListDirty();
 
 		this.bEnablePathwayTexture = bEnablePathwayTexture;
 	}
 
 	public synchronized void enableNeighborhood(final boolean bEnableNeighborhood)
 	{
 		setDisplayListDirty();
 
 		gLPathwayContentCreator.enableNeighborhood(bEnableNeighborhood);
 	}
 
 	public synchronized void enableIdenticalNodeHighlighting(
 			final boolean bEnableIdenticalNodeHighlighting)
 	{
 		setDisplayListDirty();
 
 		gLPathwayContentCreator
 				.enableIdenticalNodeHighlighting(bEnableIdenticalNodeHighlighting);
 	}
 
 	public synchronized void enableAnnotation(final boolean bEnableAnnotation)
 	{
 		gLPathwayContentCreator.enableAnnotation(bEnableAnnotation);
 	}
 
 	@Override
 	protected void handleEvents(EPickingType ePickingType, EPickingMode pickingMode,
 			int iExternalID, Pick pick)
 	{
 		if (detailLevel == EDetailLevel.VERY_LOW)
 		{
 			pickingManager.flushHits(iUniqueID, ePickingType);
 			return;
 		}
 
 		switch (ePickingType)
 		{
 			case PATHWAY_ELEMENT_SELECTION:
 
 				PathwayVertexGraphItemRep tmpVertexGraphItemRep = (PathwayVertexGraphItemRep) generalManager
 						.getPathwayItemManager().getItem(iExternalID);
 
 				// Do nothing if new selection is the same as previous selection
 				if (selectionManager.checkStatus(ESelectionType.MOUSE_OVER,
 						tmpVertexGraphItemRep.getId())
 						&& !pickingMode.equals(EPickingMode.CLICKED)
 						&& !pickingMode.equals(EPickingMode.DOUBLE_CLICKED))
 				{
 					pickingManager.flushHits(iUniqueID, ePickingType);
 					return;
 				}
 
 				setDisplayListDirty();
 
 				selectionManager.clearSelection(ESelectionType.NEIGHBORHOOD_1);
 				selectionManager.clearSelection(ESelectionType.NEIGHBORHOOD_2);
 				selectionManager.clearSelection(ESelectionType.NEIGHBORHOOD_3);
 
 				if (pickingMode == EPickingMode.DOUBLE_CLICKED)
 				{
 					// Load embedded pathway
 					if (tmpVertexGraphItemRep.getType() == EPathwayVertexType.map)
 					{
 						int iPathwayID = 
 							generalManager.getPathwayManager().searchPathwayIdByName(
 									tmpVertexGraphItemRep.getName(), EPathwayDatabaseType.KEGG);
 						
 						if(iPathwayID != -1)
 						{
 							ISelectionDelta selectionDelta = new SelectionDelta(EIDType.PATHWAY);
 							selectionDelta.addSelection(iPathwayID, ESelectionType.SELECTION);
 							triggerUpdate(EMediatorType.SELECTION_MEDIATOR, selectionDelta, null);
 						}
 					}
 					
 					selectionManager.clearSelection(ESelectionType.SELECTION);
 					
 					// Add new vertex to internal selection manager
 					selectionManager.addToType(ESelectionType.SELECTION, tmpVertexGraphItemRep
 							.getId());
 				}
 				else if (pickingMode == EPickingMode.MOUSE_OVER
 						|| pickingMode == EPickingMode.CLICKED)
 				{
 					selectionManager.clearSelection(ESelectionType.MOUSE_OVER);
 					
 					// Add new vertex to internal selection manager
 					selectionManager.addToType(ESelectionType.MOUSE_OVER,
 							tmpVertexGraphItemRep.getId());
 				}
 
 				int iConnectionID = generalManager.getIDManager().createID(
 						EManagedObjectType.CONNECTION);
 				selectionManager.addConnectionID(iConnectionID, tmpVertexGraphItemRep.getId());
 				connectedElementRepresentationManager.clear(EIDType.EXPRESSION_INDEX);
 				gLPathwayContentCreator.performIdenticalNodeHighlighting();
 
 				createConnectionLines(tmpVertexGraphItemRep, iConnectionID);
 
 				Collection<SelectionCommand> colSelectionCommand = new ArrayList<SelectionCommand>();
 				colSelectionCommand.add(new SelectionCommand(ESelectionCommandType.CLEAR,
 						ESelectionType.MOUSE_OVER));
 
 				ISelectionDelta delta = selectionManager.getDelta();
 				triggerUpdate(EMediatorType.SELECTION_MEDIATOR,// BUCKET_INTERNAL_INCOMING_MEDIATOR,
 						createExternalSelectionDelta(delta), colSelectionCommand);
 
				pickingManager.flushHits(iUniqueID, EPickingType.PATHWAY_ELEMENT_SELECTION);
 				break;
 			// case PATHWAY_TEXTURE_SELECTION:
 			//				
 			// // Trigger empty update
 			// // TODO: think about better solution
 			// triggerUpdate(EMediatorType.SELECTION_MEDIATOR, new
 			// SelectionDelta(EIDType.DAVID));
 			// pickingManager.flushHits(iUniqueID, ePickingType);
 			//				
 			// break;
 		}
 	}
 
 	private void createConnectionLines(PathwayVertexGraphItemRep vertexGraphItemRep,
 			int iConnectionID)
 	{
 //		// PathwayVertexGraphItem tmpVertexGraphItem = null;
 //		for (IGraphItem tmpGraphItem : vertexGraphItemRep
 //				.getAllItemsByProp(EGraphItemProperty.ALIAS_PARENT))
 //		{
 			// else if (pickingMode == EPickingMode.CLICKED)
 			// {
 			// selectionManager.clearSelection(ESelectionType.SELECTION);
 			//				
 			// // Add new vertex to internal selection manager
 			// selectionManager.addToType(ESelectionType.SELECTION,
 			// tmpVertexGraphItemRep.getId());
 			// }
 			// else
 			// {
 			// return;
 			// }
 			//		
 			// tmpVertexGraphItem = (PathwayVertexGraphItem) tmpGraphItem;
 			//
 			// int iDavidId = generalManager.getPathwayItemManager()
 			// .getDavidIdByPathwayVertexGraphItemId(tmpVertexGraphItem.getId());
 			//
 			// if (iDavidId == -1 || iDavidId == 0)
 			// {
 			// generalManager.getLogger().log(Level.WARNING,
 			// "Invalid David Gene ID.");
 			// // pickingManager.flushHits(iUniqueID,
 			// // EPickingType.PATHWAY_ELEMENT_SELECTION);
 			// // pickingManager.flushHits(iUniqueID,
 			// // EPickingType.PATHWAY_TEXTURE_SELECTION);
 			//
 			// // connectedElementRepresentationManager.clear();
 			// // gLPathwayContentCreator.performIdenticalNodeHighlighting();
 			// continue;
 			// }
 
 			PathwayVertexGraphItemRep tmpPathwayVertexGraphItemRep;
 			int iPathwayHeight = (generalManager.getPathwayManager().getItem(iPathwayID))
 					.getHeight();
 
 			for (int iVertexRepID : selectionManager.getElements(ESelectionType.MOUSE_OVER))
 			{
 				tmpPathwayVertexGraphItemRep = generalManager.getPathwayItemManager()
 						.getPathwayVertexRep(iVertexRepID);
 
 				SelectedElementRep elementRep = new SelectedElementRep(
 						EIDType.EXPRESSION_INDEX,
 						this.getID(),
 						(tmpPathwayVertexGraphItemRep.getXOrigin() * PathwayRenderStyle.SCALING_FACTOR_X)
 								* vecScaling.x() + vecTranslation.x(),
 						((iPathwayHeight - tmpPathwayVertexGraphItemRep.getYOrigin()) * PathwayRenderStyle.SCALING_FACTOR_Y)
 								* vecScaling.y() + vecTranslation.y(), 0);
 
 				// for (Integer iConnectionID : selectionManager
 				// .getConnectionForElementID(iVertexRepID))
 				// {
 				connectedElementRepresentationManager.addSelection(iConnectionID, elementRep);
 				// }
 			}
 //		}
 	}
 
 	@Override
 	public synchronized void broadcastElements(ESelectionType type)
 	{
 		ISelectionDelta selectionDelta = new SelectionDelta(EIDType.DAVID);
 
 		// TODO: Move to own method (outside this class)
 		Iterator<IGraphItem> iterPathwayVertexGraphItemRep = (generalManager
 				.getPathwayManager().getItem(iPathwayID)).getAllItemsByKind(
 				EGraphItemKind.NODE).iterator();
 		Iterator<IGraphItem> iterPathwayVertexGraphItem;
 		PathwayVertexGraphItemRep tmpPathwayVertexGraphItemRep = null;
 		PathwayVertexGraphItem tmpPathwayVertexGraphItem = null;
 		while (iterPathwayVertexGraphItemRep.hasNext())
 		{
 			tmpPathwayVertexGraphItemRep = ((PathwayVertexGraphItemRep) iterPathwayVertexGraphItemRep
 					.next());
 
 			iterPathwayVertexGraphItem = tmpPathwayVertexGraphItemRep.getAllItemsByProp(
 					EGraphItemProperty.ALIAS_PARENT).iterator();
 
 			while (iterPathwayVertexGraphItem.hasNext())
 			{
 				tmpPathwayVertexGraphItem = (PathwayVertexGraphItem) iterPathwayVertexGraphItem
 						.next();
 
 				int iDavidId = generalManager.getPathwayItemManager()
 						.getDavidIdByPathwayVertexGraphItemId(
 								tmpPathwayVertexGraphItem.getId());
 
 				if (iDavidId == -1 || iDavidId == 0)
 				{
 					generalManager.getLogger().log(Level.WARNING, "Invalid David Gene ID.");
 					continue;
 				}
 
 				selectionDelta.addSelection(iDavidId, type);
 			}
 		}
 
 		triggerUpdate(EMediatorType.SELECTION_MEDIATOR, selectionDelta, null);
 	}
 
 	@Override
 	public synchronized String getShortInfo()
 	{
 		PathwayGraph pathway = (generalManager.getPathwayManager().getItem(iPathwayID));
 
 		return pathway.getTitle() + " (" + pathway.getType().getName() + ")";
 	}
 
 	@Override
 	public synchronized String getDetailedInfo()
 	{
 		StringBuffer sInfoText = new StringBuffer();
 		PathwayGraph pathway = (generalManager.getPathwayManager().getItem(iPathwayID));
 
 		sInfoText.append("<b>Pathway</b>\n\n<b>Name:</b> " + pathway.getTitle()
 				+ "\n<b>Type:</b> " + pathway.getType().getName());
 
 		// generalManager.getSWTGUIManager().setExternalRCPStatusLineMessage(
 		// pathway.getType().getName() + " Pathway: " + sPathwayTitle);
 
 		return sInfoText.toString();
 	}
 
 	@Override
 	public synchronized void triggerUpdate(EMediatorType eMediatorType,
 			ISelectionDelta selectionDelta, Collection<SelectionCommand> colSelectionCommand)
 	{
 		generalManager.getEventPublisher().triggerUpdate(eMediatorType, this, selectionDelta,
 				colSelectionCommand);
 	}
 
 	@Override
 	public synchronized void addSet(int setID)
 	{
 		super.addSet(setID);
 		connectedElementRepresentationManager.clear(EIDType.EXPRESSION_INDEX);
 	}
 
 	@Override
 	public synchronized void addSet(ISet set)
 	{
 		super.addSet(set);
 		connectedElementRepresentationManager.clear(EIDType.EXPRESSION_INDEX);
 	}
 
 	public void renderPathwayName(final GL gl)
 	{
 		if (!bEnableTitleRendering)
 			return;
 
 		float fHorizontalPosition = 0;
 		float fVerticalPosition = 0;
 
 		if (iHorizontalTextAlignment == SWT.LEFT)
 			fHorizontalPosition = 0.2f;
 		else if (iHorizontalTextAlignment == SWT.RIGHT)
 			fHorizontalPosition = 3.5f;
 		else if (iHorizontalTextAlignment == SWT.CENTER)
 			fHorizontalPosition = 1.8f;
 
 		if (iVerticalTextAlignment == SWT.TOP)
 			fVerticalPosition = 7.8f;
 		else if (iVerticalTextAlignment == SWT.BOTTOM)
 			fVerticalPosition = 0.2f;
 		else if (iVerticalTextAlignment == SWT.CENTER)
 			fVerticalPosition = 1;
 
 		String sPathwayName = generalManager.getPathwayManager().getItem(iPathwayID)
 				.getTitle();
 
 		int iMaxChars = 40;
 		if (iHorizontalTextAlignment == SWT.RIGHT)
 			iMaxChars = 30;
 
 		if (sPathwayName.length() > iMaxChars)
 			sPathwayName = sPathwayName.subSequence(0, iMaxChars - 3) + "...";
 
 		textRenderer.begin3DRendering();
 		textRenderer.setColor(0.2f, 0.2f, 0.2f, 1.0f);
 		textRenderer.draw3D(sPathwayName, fHorizontalPosition, fVerticalPosition, 0.05f,
 				0.011f);
 		textRenderer.end3DRendering();
 	}
 
 	public void enableTitleRendering(boolean bEnable)
 	{
 		bEnableTitleRendering = bEnable;
 	}
 
 	public void setAlignment(int iHorizontalAlignment, int iVerticalAlignment)
 	{
 		this.iHorizontalTextAlignment = iHorizontalAlignment;
 		this.iVerticalTextAlignment = iVerticalAlignment;
 	}
 
 	@Override
 	public int getNumberOfSelections(ESelectionType eSelectionType)
 	{
 		return selectionManager.getElements(eSelectionType).size();
 	}
 }
