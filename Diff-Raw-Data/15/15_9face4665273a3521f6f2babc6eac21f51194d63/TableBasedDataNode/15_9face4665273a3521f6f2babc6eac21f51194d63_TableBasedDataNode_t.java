 /*******************************************************************************
  * Caleydo - Visualization for Molecular Biology - http://caleydo.org
  * Copyright (c) The Caleydo Team. All rights reserved.
  * Licensed under the new BSD license, available at http://caleydo.org/license
  ******************************************************************************/
 package org.caleydo.view.dvi.node;
 
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 import javax.media.opengl.GL2;
 
 import org.caleydo.core.data.datadomain.ATableBasedDataDomain;
 import org.caleydo.core.data.datadomain.DataDomainActions;
 import org.caleydo.core.data.datadomain.IDataDomain;
 import org.caleydo.core.data.perspective.table.TablePerspective;
 import org.caleydo.core.data.perspective.variable.Perspective;
 import org.caleydo.core.data.virtualarray.group.Group;
 import org.caleydo.core.data.virtualarray.group.GroupList;
 import org.caleydo.core.util.collection.Pair;
 import org.caleydo.core.view.opengl.layout.Column;
 import org.caleydo.core.view.opengl.layout.ElementLayout;
 import org.caleydo.core.view.opengl.layout.Row;
 import org.caleydo.core.view.opengl.layout.util.ColorRenderer;
 import org.caleydo.core.view.opengl.picking.APickingListener;
 import org.caleydo.core.view.opengl.picking.Pick;
 import org.caleydo.core.view.opengl.util.button.Button;
 import org.caleydo.core.view.opengl.util.button.ButtonRenderer;
 import org.caleydo.core.view.opengl.util.draganddrop.DragAndDropController;
 import org.caleydo.core.view.opengl.util.draganddrop.IDraggable;
 import org.caleydo.core.view.opengl.util.draganddrop.IDropArea;
 import org.caleydo.core.view.opengl.util.texture.EIconTextures;
 import org.caleydo.view.dvi.GLDataViewIntegrator;
 import org.caleydo.view.dvi.layout.AGraphLayout;
 import org.caleydo.view.dvi.tableperspective.AMultiTablePerspectiveRenderer;
 import org.caleydo.view.dvi.tableperspective.PerspectiveRenderer;
 import org.caleydo.view.dvi.tableperspective.TablePerspectiveListRenderer;
 import org.caleydo.view.dvi.tableperspective.matrix.TablePerspectiveMatrixRenderer;
 
 public class TableBasedDataNode extends ADataNode implements IDropArea {
 
 	private final static String TOGGLE_TABLE_PERSPECTIVE_BUTTON_PICKING_TYPE = "org.caleydo.view.dvi.toggletableperspectivebutton";
 	private final static int TOGGLE_TABLE_PERSPECTIVE_BUTTON_PICKING_ID = 0;
 
 	private ATableBasedDataDomain dataDomain;
 
 	private ButtonRenderer toggleTablePerspectiveButtonRenderer;
 	private Button toggleTablePerspectiveButton;
 
 	private ALayoutState currentState;
 	private OverviewState overviewState;
 	private DetailState detailState;
 	private ElementLayout tablePerspectiveLayout;
 	private AMultiTablePerspectiveRenderer tablePerspectiveRenderer;
 	private Row bodyRow;
 	private List<TablePerspective> tablePerspectives;
 
 	private abstract class ALayoutState {
 		protected AMultiTablePerspectiveRenderer tablePerspectiveRenderer;
 		protected ButtonRenderer.ETextureRotation textureRotation;
 
 		public void apply() {
 			TableBasedDataNode.this.tablePerspectiveRenderer.unregisterPickingListeners();
 			TableBasedDataNode.this.tablePerspectiveRenderer = tablePerspectiveRenderer;
 			tablePerspectiveRenderer.setUpsideDown(isUpsideDown);
 			tablePerspectiveLayout.setRenderer(tablePerspectiveRenderer);
 			toggleTablePerspectiveButtonRenderer.setTextureRotation(currentState.textureRotation);
 			tablePerspectiveRenderer.setTablePerspectives(getTablePerspectives());
 			tablePerspectiveRenderer.unregisterPickingListeners();
 			tablePerspectiveRenderer.registerPickingListeners();
 			recalculateNodeSize();
 			graphLayout.fitNodesToDrawingArea(view.calculateGraphDrawingArea());
 		}
 
 		public abstract ALayoutState getNextState();
 	}
 
 	private class OverviewState extends ALayoutState {
 
 		public OverviewState() {
 			tablePerspectiveRenderer = new TablePerspectiveListRenderer(TableBasedDataNode.this, view,
 					dragAndDropController, getVisibleTablePerspectives());
 			List<Pair<String, Integer>> pickingIDsToBePushed = new ArrayList<Pair<String, Integer>>();
 			pickingIDsToBePushed.add(new Pair<String, Integer>(DATA_GRAPH_NODE_PENETRATING_PICKING_TYPE, id));
 
 			tablePerspectiveRenderer.setPickingIDsToBePushed(pickingIDsToBePushed);
 			textureRotation = ButtonRenderer.ETextureRotation.TEXTURE_ROTATION_270;
 		}
 
 		@Override
 		public ALayoutState getNextState() {
 			return detailState;
 		}
 
 		@Override
 		public void apply() {
 			super.apply();
 
 			List<TablePerspective> visibleTablePerspectives = getVisibleTablePerspectives();
 			tablePerspectiveRenderer.setTablePerspectives(getVisibleTablePerspectives());
 			bodyRow.clearBackgroundRenderers();
 			if (visibleTablePerspectives.size() > 0) {
 				bodyRow.addBackgroundRenderer(new ColorRenderer(new float[] { 1, 1, 1, 1 }));
 			}
 		}
 	}
 
 	private class DetailState extends ALayoutState {
 
 		public DetailState() {
 			tablePerspectiveRenderer = new TablePerspectiveMatrixRenderer(dataDomain, view, TableBasedDataNode.this,
 					dragAndDropController);
 			List<Pair<String, Integer>> pickingIDsToBePushed = new ArrayList<Pair<String, Integer>>();
 			pickingIDsToBePushed.add(new Pair<String, Integer>(DATA_GRAPH_NODE_PENETRATING_PICKING_TYPE, id));
 
 			tablePerspectiveRenderer.setPickingIDsToBePushed(pickingIDsToBePushed);
 			textureRotation = ButtonRenderer.ETextureRotation.TEXTURE_ROTATION_90;
 		}
 
 		@Override
 		public ALayoutState getNextState() {
 			return overviewState;
 		}
 
 		@Override
 		public void apply() {
 			super.apply();
 			bodyRow.clearBackgroundRenderers();
 			bodyRow.addBackgroundRenderer(new ColorRenderer(new float[] { 1, 1, 1, 1 }));
 		}
 	}
 
 	public TableBasedDataNode(AGraphLayout graphLayout, GLDataViewIntegrator view,
 			DragAndDropController dragAndDropController, Integer id, IDataDomain dataDomain) {
 		super(graphLayout, view, dragAndDropController, id, dataDomain);
 		this.dataDomain = (ATableBasedDataDomain) dataDomain;
 
 		overviewState = new OverviewState();
 		detailState = new DetailState();
 		currentState = overviewState;
 		tablePerspectiveRenderer = currentState.tablePerspectiveRenderer;
 	}
 
 	@Override
 	protected void registerPickingListeners() {
 		super.registerPickingListeners();
 
 		view.addIDPickingListener(new APickingListener() {
 
 			@Override
 			public void clicked(Pick pick) {
 				currentState = currentState.getNextState();
 				currentState.apply();
 				// recalculateNodeSize();
 				// graphLayout.updateNodePositions();
 
 				view.setDisplayListDirty();
 			}
 
 		}, TOGGLE_TABLE_PERSPECTIVE_BUTTON_PICKING_TYPE + getID(), TOGGLE_TABLE_PERSPECTIVE_BUTTON_PICKING_ID);
 
 		view.addIDPickingListener(new APickingListener() {
 
 			@Override
 			public void rightClicked(Pick pick) {
 				DataDomainActions.add(view.getContextMenuCreator(), dataDomain, this, true);
 			}
 
 		}, DATA_GRAPH_NODE_PICKING_TYPE, id);
 
 		view.addIDPickingListener(new APickingListener() {
 
 			@Override
 			public void dragged(Pick pick) {
 
 				// DragAndDropController dragAndDropController =
 				// dragAndDropController;
 				if (dragAndDropController.isDragging()
 						&& dragAndDropController.getDraggingMode().equals("PerspectiveDrag")) {
 					dragAndDropController.setDropArea(TableBasedDataNode.this);
 				}
 
 			}
 
 		}, DATA_GRAPH_NODE_PENETRATING_PICKING_TYPE, id);
 	}
 
 	@Override
 	protected ElementLayout setupLayout() {
 
 		Row baseRow = createDefaultBaseRow(dataDomain.getColor(), getID());
 		ElementLayout spacingLayoutX = createDefaultSpacingX();
 
 		baseColumn = new Column();
 
 		baseRow.append(spacingLayoutX);
 		baseRow.append(baseColumn);
 		baseRow.append(spacingLayoutX);
 
 		Row titleRow = new Row("titleRow");
 
 		ElementLayout captionLayout = createDefaultCaptionLayout(getID());
 
 		titleRow.append(captionLayout);
 		titleRow.setYDynamic(true);
 
 		ElementLayout lineSeparatorLayout = createDefaultLineSeparatorLayout();
 
 		ElementLayout toggleTablePerspectiveButtonLayout = new ElementLayout("toggleTablePerspectiveLayout");
 		toggleTablePerspectiveButtonLayout.setPixelSizeY(CAPTION_HEIGHT_PIXELS);
 		toggleTablePerspectiveButtonLayout.setPixelSizeX(CAPTION_HEIGHT_PIXELS);
 		toggleTablePerspectiveButton = new Button(TOGGLE_TABLE_PERSPECTIVE_BUTTON_PICKING_TYPE + getID(),
 				TOGGLE_TABLE_PERSPECTIVE_BUTTON_PICKING_ID, EIconTextures.CM_SELECTION_RIGHT_EXTENSIBLE_BLACK);
 
 		if (dataDomain.getRecordPerspectiveIDs().size() < 1 && dataDomain.getDimensionPerspectiveIDs().size() < 1) {
 			toggleTablePerspectiveButton.setVisible(false);
 		}
 		toggleTablePerspectiveButtonRenderer = new ButtonRenderer.Builder(view, toggleTablePerspectiveButton).build();
 		toggleTablePerspectiveButtonRenderer.addPickingID(DATA_GRAPH_NODE_PENETRATING_PICKING_TYPE, id);
 		toggleTablePerspectiveButtonRenderer.setZCoordinate(1);
 		toggleTablePerspectiveButtonLayout.setRenderer(toggleTablePerspectiveButtonRenderer);
 
 		// FIXME: Very bad hack
 		// if ((!dataDomain.getLabel().toLowerCase().contains("copy"))
 		// && (!dataDomain.getLabel().toLowerCase().contains("clinical"))
 		// && (!dataDomain.getLabel().toLowerCase().contains("mutation"))) {
 		titleRow.append(spacingLayoutX);
 		titleRow.append(toggleTablePerspectiveButtonLayout);
 		// }
 
 		bodyRow = new Row("bodyRow");
 
 		bodyColumn = new Column("bodyColumn");
 
 		tablePerspectiveLayout = new ElementLayout("tablePerspectiveLayout");
 		tablePerspectiveLayout.setRatioSizeY(1);
 		tablePerspectiveLayout.setRenderer(tablePerspectiveRenderer);
 
 		ElementLayout spacingLayoutY = createDefaultSpacingY();
 
 		bodyColumn.append(tablePerspectiveLayout);
 		bodyColumn.append(spacingLayoutY);
 
 		bodyRow.append(bodyColumn);
 
 		baseColumn.append(spacingLayoutY);
 		baseColumn.append(bodyRow);
 		baseColumn.append(spacingLayoutY);
 		baseColumn.append(lineSeparatorLayout);
 		baseColumn.append(titleRow);
 		baseColumn.append(spacingLayoutY);
 
 		setUpsideDown(isUpsideDown);
 
 		currentState.apply();
 		// recalculateNodeSize();
 		graphLayout.fitNodesToDrawingArea(view.calculateGraphDrawingArea());
 
 		return baseRow;
 	}
 
 	@Override
 	public void destroy() {
 		view.removeAllIDPickingListeners(TOGGLE_TABLE_PERSPECTIVE_BUTTON_PICKING_TYPE + getID(),
 				TOGGLE_TABLE_PERSPECTIVE_BUTTON_PICKING_ID);
 		view.removeAllIDPickingListeners(DATA_GRAPH_NODE_PENETRATING_PICKING_TYPE, id);
 		view.removeAllIDPickingListeners(DATA_GRAPH_NODE_PICKING_TYPE, id);
 		// tablePerspectiveRenderer.destroy();
 		overviewState.tablePerspectiveRenderer.destroy();
 		detailState.tablePerspectiveRenderer.destroy();
 	}
 
 	@Override
 	public void update() {
 
 		retrieveTablePerspectives();
 		if (dataDomain.getRecordPerspectiveIDs().size() > 0 || dataDomain.getDimensionPerspectiveIDs().size() > 0) {
 			toggleTablePerspectiveButton.setVisible(true);
 		}
 		currentState.apply();
 		if (currentState == overviewState) {
 			tablePerspectiveRenderer.setTablePerspectives(getVisibleTablePerspectives());
 		} else {
 			tablePerspectiveRenderer.setTablePerspectives(getTablePerspectives());
 		}
 	}
 
 	@Override
 	protected AMultiTablePerspectiveRenderer getTablePerspectiveRenderer() {
 		return tablePerspectiveRenderer;
 	}
 
 	protected void retrieveTablePerspectives() {
 		Collection<TablePerspective> containerCollection = dataDomain.getAllTablePerspectives();
 		if (containerCollection == null) {
 			tablePerspectives = new ArrayList<TablePerspective>();
 			return;
 		}
 		// List<Pair<String, TablePerspective>> sortedParentTablePerspectives =
 		// new
 		// ArrayList<Pair<String, TablePerspective>>();
 		// for (TablePerspective container : containerCollection) {
 		// sortedParentTablePerspectives.add(new Pair<String,
 		// TablePerspective>(container
 		// .getLabel(), container));
 		// }
 
 		Set<String> recordPerspectiveIDs = dataDomain.getRecordPerspectiveIDs();
 		if (recordPerspectiveIDs == null)
 			return;
 
 		List<Pair<String, Perspective>> parentRecordPerspectives = new ArrayList<Pair<String, Perspective>>();
 		Map<Perspective, List<Pair<String, Perspective>>> childRecordPerspectiveLists = new HashMap<Perspective, List<Pair<String, Perspective>>>();
 
 		for (String perspectiveID : recordPerspectiveIDs) {
 			Perspective perspective = dataDomain.getTable().getRecordPerspective(perspectiveID);
 
 			if (perspective.isPrivate()) {
 				continue;
 			}
 
 			parentRecordPerspectives.add(new Pair<String, Perspective>(perspective.getLabel(), perspective));
 
 			GroupList groupList = perspective.getVirtualArray().getGroupList();
 
 			if (groupList != null) {
 				List<Pair<String, Perspective>> childList = new ArrayList<Pair<String, Perspective>>(groupList.size());
 				for (int i = 0; i < groupList.size(); i++) {
 
 					Group group = groupList.get(i);
 					if (group.getPerspectiveID() != null) {
 
 						Perspective childPerspective = dataDomain.getTable().getRecordPerspective(
 								group.getPerspectiveID());
 						childList.add(new Pair<String, Perspective>(childPerspective.getLabel(), childPerspective));
 					}
 				}
 
 				// Collections.sort(childList);
 				childRecordPerspectiveLists.put(perspective, childList);
 			}
 
 		}
 
 		Collections.sort(parentRecordPerspectives, Pair.<String> compareFirst());
 
 		List<Perspective> sortedRecordPerspectives = new ArrayList<Perspective>();
 
 		for (Pair<String, Perspective> parentPair : parentRecordPerspectives) {
 			sortedRecordPerspectives.add(parentPair.getSecond());
 
 			List<Pair<String, Perspective>> childList = childRecordPerspectiveLists.get(parentPair.getSecond());
 
 			if (childList != null) {
 				for (Pair<String, Perspective> childPair : childList) {
 					sortedRecordPerspectives.add(childPair.getSecond());
 				}
 			}
 		}
 
 		Set<String> dimensionPerspectiveIDs = new HashSet<>(dataDomain.getDimensionPerspectiveIDs());
 
 		List<Pair<String, Perspective>> parentDimensionPerspectives = new ArrayList<Pair<String, Perspective>>();
 		Map<Perspective, List<Pair<String, Perspective>>> childDimensionPerspectiveLists = new HashMap<Perspective, List<Pair<String, Perspective>>>();
 
 		for (String perspectiveID : dimensionPerspectiveIDs) {
 			Perspective perspective = dataDomain.getTable().getDimensionPerspective(perspectiveID);
 
 			if (perspective.isPrivate()) {
 				continue;
 			}
 
 			parentDimensionPerspectives.add(new Pair<String, Perspective>(perspective.getLabel(), perspective));
 
 			GroupList groupList = perspective.getVirtualArray().getGroupList();
 
 			if (groupList != null) {
 				List<Pair<String, Perspective>> childList = new ArrayList<Pair<String, Perspective>>(groupList.size());
 				for (int i = 0; i < groupList.size(); i++) {
 
 					Group group = groupList.get(i);
 					if (group.getPerspectiveID() != null) {
 
 						Perspective childPerspective = dataDomain.getTable().getDimensionPerspective(
 								group.getPerspectiveID());
 						childList.add(new Pair<String, Perspective>(childPerspective.getLabel(), childPerspective));
 					}
 				}
 
 				// Collections.sort(childList);
 				childDimensionPerspectiveLists.put(perspective, childList);
 			}
 
 		}
 
 		Collections.sort(parentDimensionPerspectives, Pair.<String> compareFirst());
 
 		List<Perspective> sortedDimensionPerspectives = new ArrayList<Perspective>();
 
 		for (Pair<String, Perspective> parentPair : parentDimensionPerspectives) {
 			sortedDimensionPerspectives.add(parentPair.getSecond());
 
 			List<Pair<String, Perspective>> childList = childDimensionPerspectiveLists.get(parentPair.getSecond());
 
 			if (childList != null) {
 				for (Pair<String, Perspective> childPair : childList) {
 					sortedDimensionPerspectives.add(childPair.getSecond());
 				}
 			}
 		}
 
 		tablePerspectives = new ArrayList<TablePerspective>(containerCollection.size());
 
 		for (Perspective dimensionPerspective : sortedDimensionPerspectives) {
 			for (Perspective recordPerspective : sortedRecordPerspectives) {
 				if (dataDomain.hasTablePerspective(recordPerspective.getPerspectiveID(),
 						dimensionPerspective.getPerspectiveID())) {
 
 					TablePerspective tablePerspective = dataDomain.getTablePerspective(
 							recordPerspective.getPerspectiveID(), dimensionPerspective.getPerspectiveID());
 
 					if (!tablePerspective.isPrivate())
 						tablePerspectives.add(tablePerspective);
 				}
 			}
 		}
 
 	}
 
 	@Override
 	public List<TablePerspective> getTablePerspectives() {
 
 		if (tablePerspectives == null)
 			retrieveTablePerspectives();
 
 		return tablePerspectives;
 	}
 
 	@Override
 	public void handleDragOver(GL2 gl, Set<IDraggable> draggables, float mouseCoordinateX, float mouseCoordinateY) {
 		// TODO Auto-generated method stub
 
 	}
 
 	@Override
 	public void handleDrop(GL2 gl, Set<IDraggable> draggables, float mouseCoordinateX, float mouseCoordinateY,
 			DragAndDropController dragAndDropController) {
 
 		for (IDraggable draggable : draggables) {
 			if (draggable instanceof PerspectiveRenderer) {
 				PerspectiveRenderer perspectiveRenderer = (PerspectiveRenderer) draggable;
 				ATableBasedDataDomain foreignDataDomain = perspectiveRenderer.getDataDomain();
 				if (foreignDataDomain != this.dataDomain) {
 					if (perspectiveRenderer.isRecordPerspective()) {
						if (this.dataDomain.getRecordIDCategory() != foreignDataDomain.getRecordIDCategory())
							continue;
 						Perspective recordPerspective = foreignDataDomain.getTable().getRecordPerspective(
 								perspectiveRenderer.getPerspectiveID());
 

 						Perspective convertedPerspective = this.dataDomain.convertForeignPerspective(recordPerspective);
 						convertedPerspective.setDefault(false);
 						this.dataDomain.getTable().registerRecordPerspective(convertedPerspective);
					} else {
						if (this.dataDomain.getDimensionIDCategory() != foreignDataDomain.getDimensionIDCategory())
							continue;
						Perspective dimensionPerspective = foreignDataDomain.getTable().getDimensionPerspective(
								perspectiveRenderer.getPerspectiveID());

						Perspective convertedPerspective = this.dataDomain
								.convertForeignPerspective(dimensionPerspective);
						convertedPerspective.setDefault(false);
						this.dataDomain.getTable().registerDimensionPerspective(convertedPerspective);
 					}
 				}
 				// tablePerspectives.add(dimensionGroupRenderer.getTablePerspective());
 			}
 		}
 
 	}
 
 	@Override
 	protected int getMinTitleBarWidthPixels() {
 		float textWidth = view.getTextRenderer().getRequiredTextWidth(dataDomain.getLabel(),
 				pixelGLConverter.getGLHeightForPixelHeight(CAPTION_HEIGHT_PIXELS));
 
 		return pixelGLConverter.getPixelWidthForGLWidth(textWidth) + CAPTION_HEIGHT_PIXELS + SPACING_PIXELS;
 	}
 
 	@Override
 	public void handleDropAreaReplaced() {
 		// TODO Auto-generated method stub
 
 	}
 
 	@Override
 	public String getProviderName() {
 		return "Tabular Data Node";
 	}
 
 }
