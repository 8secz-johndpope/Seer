 /*******************************************************************************
  * <copyright>
  *
  * Copyright (c) 2005, 2010 SAP AG.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *    SAP AG - initial API, implementation and documentation
  *
  * </copyright>
  *
  *******************************************************************************/
 package org.eclipse.graphiti.ui.internal.figures;
 
 import java.util.List;
 
 import org.eclipse.draw2d.Figure;
 import org.eclipse.draw2d.Graphics;
 import org.eclipse.draw2d.IFigure;
 import org.eclipse.draw2d.Shape;
 import org.eclipse.draw2d.geometry.Rectangle;
 import org.eclipse.gef.handles.HandleBounds;
 import org.eclipse.graphiti.internal.pref.GFPreferences;
 import org.eclipse.graphiti.internal.services.GraphitiInternal;
 import org.eclipse.graphiti.mm.pictograms.GraphicsAlgorithm;
 import org.eclipse.graphiti.mm.pictograms.PictogramElement;
 import org.eclipse.graphiti.mm.pictograms.RenderingStyle;
 import org.eclipse.graphiti.platform.ga.IVisualState;
 import org.eclipse.graphiti.platform.ga.IVisualStateChangeListener;
 import org.eclipse.graphiti.platform.ga.IVisualStateHolder;
 import org.eclipse.graphiti.services.Graphiti;
 import org.eclipse.graphiti.tb.ISelectionInfo;
 import org.eclipse.graphiti.tb.IToolBehaviorProvider;
 import org.eclipse.graphiti.ui.internal.config.IConfigurationProvider;
 import org.eclipse.graphiti.ui.internal.parts.IPictogramElementDelegate;
 import org.eclipse.graphiti.ui.internal.util.DataTypeTransformation;
 import org.eclipse.graphiti.ui.internal.util.draw2d.GFColorConstants;
 import org.eclipse.graphiti.util.IColorConstant;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.graphics.Color;
 import org.eclipse.swt.graphics.Path;
 
 /**
  * This class is an abstract super-class for all Shapes in the GFW. The main
  * idea is, that the outline and fill-area of a Shape is defined by a Path.
  * Sub-classes usually only have to implement the abstract method
  * {@link #createPath(Rectangle, Graphics, boolean)}
  * 
  * @noinstantiate This class is not intended to be instantiated by clients.
  * @noextend This class is not intended to be subclassed by clients.
  */
 public abstract class GFAbstractShape extends Shape implements HandleBounds, IVisualStateHolder, IVisualStateChangeListener {
 
 	/**
 	 * The {@link IPictogramElementDelegate} given in the constructor.
 	 */
 	private final IPictogramElementDelegate pictogramElementDelegate;
 
 	/**
 	 * The {@link GraphicsAlgorithm} given in the constructor.
 	 */
 	private final GraphicsAlgorithm graphicsAlgorithm;
 
 	/**
 	 * The selection GraphicsAlgorithm. See
 	 * {@link #getSelectionGraphicsAlgorithm()}.
 	 */
 	private GraphicsAlgorithm selectionGraphicsAlgorithm;
 
 	/**
 	 * The selection-area GraphicsAlgorithms. See {@link #getSelectionArea()}.
 	 */
 	private GraphicsAlgorithm selectionArea[];
 
 	// ============================ constructors ==============================
 
 	/**
 	 * Creates a new GFAbstractShape.
 	 * 
 	 * @param pictogramElementDelegate
 	 *            The PictogramElementDelegate which provides the
 	 *            GraphicsAlgorithm.
 	 * @param graphicsAlgorithm
 	 *            The GraphicsAlgorithm which provides the values to paint this
 	 *            Shape. It is either the immediate GraphicsAlgorithm of the
 	 *            PictogramElementDelegate or a child of that immediate
 	 *            GraphicsAlgorithm. It must not be null.
 	 */
 	public GFAbstractShape(IPictogramElementDelegate pictogramElementDelegate, GraphicsAlgorithm graphicsAlgorithm) {
 		this.pictogramElementDelegate = pictogramElementDelegate;
 		this.graphicsAlgorithm = graphicsAlgorithm;
 
 		getVisualState().addChangeListener(this);
 	}
 
 	// ======================== new abstract methods ==========================
 
 	/**
 	 * Returns the Path which shall be painted in
 	 * {@link #paintShape(Graphics, boolean)}.
 	 * 
 	 * @param outerBounds
 	 *            The outer bounds which shall contain the Path. They are
 	 *            calculated from the bounds of this figure by
 	 *            {@link GFFigureUtil#getAdjustedRectangle(Rectangle, double, int)}
 	 *            . Note, that those outline-bounds are just a suggestion which
 	 *            works fine for many cases.
 	 * @param graphics
 	 *            The Graphics on which the outline Path shall be painted. It
 	 *            can be used to react on Graphics specific values, like the
 	 *            zoom-level of the Graphics.
 	 * @param isFill
 	 *            if true, the Path is used for filling the Shape, otherwise for
 	 *            outlining the Shape.
 	 * @return The Path which shall be painted in
 	 *         {@link #paintShape(Graphics, boolean)}.
 	 */
 	abstract protected Path createPath(Rectangle outerBounds, Graphics graphics, boolean isFill);
 
 	// ========================= new public methods ===========================
 
 	/**
 	 * Returns the PictogramElementDelegate, which was given in the constructor.
 	 * 
 	 * @return The PictogramElementDelegate, which was given in the constructor.
 	 */
 	protected IPictogramElementDelegate getPictogramElementDelegate() {
 		return pictogramElementDelegate;
 	}
 
 	/**
 	 * Returns the GraphicsAlgorithm, which was given in the constructor.
 	 * 
 	 * @return The GraphicsAlgorithm, which was given in the constructor.
 	 */
 	protected GraphicsAlgorithm getGraphicsAlgorithm() {
 		return graphicsAlgorithm;
 	}
 
 	/**
 	 * Returns the IConfigurationProvider, which can be used to access the GFW
 	 * configuration. This is just a convenience for
 	 * <code>getPictogramElementDelegate().getConfigurationProvider()</code>.
 	 * 
 	 * @return The IConfigurationProvider, which can be used to access the GFW
 	 *         configuration.
 	 */
 	protected IConfigurationProvider getConfigurationProvider() {
 		return getPictogramElementDelegate().getConfigurationProvider();
 	}
 
 	/**
 	 * Returns the zoom-level of the given Graphics.
 	 * 
 	 * @param graphics
 	 *            The Graphics for which to return the zoom-level.
 	 * @return The zoom-level of the given Graphics.
 	 */
 	protected double getZoomLevel(Graphics graphics) {
 		return graphics.getAbsoluteScale();
 	}
 
 	/**
 	 * Returns the line-width of this figure adjusted according to the given
 	 * Graphics. This means especially, that the line-width is multiplied with
 	 * the zoom-level of the given Graphics.
 	 * 
 	 * @param graphics
 	 *            The Graphics used to adjust the line-width.
 	 * @return The line-width of this figure adjusted according to the given
 	 *         Graphics.
 	 */
 	protected int getLineWidth(Graphics graphics) {
 		return Math.max(1, (int) (getZoomLevel(graphics) * getLineWidth()));
 	}
 
 	/**
 	 * Changes the given outline-bounds which should be calculated by
 	 * {@link #getSingletonOutlineBounds(Graphics)}) to the fill-bounds. In this
 	 * default implementation the fill-bounds are calculated from the
 	 * outline-bounds by
 	 * <ul>
 	 * <li>Shrinking by the half line-width, so that the fill-bounds fit exactly
 	 * inside the outline painted using the line-width</li>
 	 * </ul>
 	 * 
 	 * @param outlineBounds
 	 *            The outline-bounds to transform. They should be calculated by
 	 *            {@link #getSingletonOutlineBounds(Graphics)}.
 	 * @param graphics
 	 *            The Graphics used to calculate the bounds.
 	 */
 	protected void transformToFillBounds(Rectangle outlineBounds, Graphics graphics) {
 		// shrink the bounds by half line-width because there the outline is
 		// painted
 		int lineWidth = getLineWidth(graphics);
 		outlineBounds.x += (lineWidth + 1) / 2;
 		outlineBounds.y += (lineWidth + 1) / 2;
 		outlineBounds.height -= lineWidth - 1;
 		outlineBounds.width -= lineWidth - 1;
 	}
 
 	/**
 	 * Returns the selection-area GraphicsAlgorithms of this Shape. By default
 	 * the bounds of a Shape are used as selection-area to calculate if a Point
 	 * is inside the Shape (see {@link #containsPoint(int, int)}). With this
 	 * method the selection area can be changed, so that the selection-area are
 	 * all bounds of the returned selection-area GraphicsAlgorithm. Can be null,
 	 * if no special selection-area GraphicsAlgorithms exists.
 	 * <p>
 	 * By default this method returns
 	 * {@link IToolBehaviorProvider#getSelectionArea(PictogramElement)}
 	 * 
 	 * @see #getSelectionGraphicsAlgorithm()
 	 * @return The selection-area GraphicsAlgorithms of this Shape. Can be null.
 	 */
 	protected GraphicsAlgorithm[] getSelectionArea() {
 		return selectionArea;
 	}
 
 	/**
 	 * Returns the selection GraphicsAlgorithm of this Shape. By default the
 	 * bounds of a Shape are used to give selection feedback, especially the
 	 * selection handles. With this method the selection feedback can be
 	 * changed, so that the handle-bounds are the bounds of the returned
 	 * selection GraphicsAlgorithm. Can be null, if no special selection
 	 * GraphicsAlgorithm exists.
 	 * <p>
 	 * By default this method returns
 	 * {@link IToolBehaviorProvider#getSelectionGraphicsAlgorithm(PictogramElement)}
 	 * 
 	 * @see #getSelectionArea()
 	 * @return The selection GraphicsAlgorithm of this Shape. Can be null.
 	 */
 	protected GraphicsAlgorithm getSelectionGraphicsAlgorithm() {
 		return selectionGraphicsAlgorithm;
 	}
 
 	/**
 	 * Returns true, if the given point is contained inside one of the
 	 * selection-area GraphicsAlgorithms defined in {@link #getSelectionArea()}.
 	 * 
 	 * @param x
 	 *            The x-coordinate of the point to check.
 	 * @param y
 	 *            The y-coordinate of the point to check.
 	 * @see #containsPoint(int, int)
 	 * @return true, if the given point is contained inside one of the
 	 *         selection-area GraphicsAlgorithms.
 	 */
 	protected Boolean containsPointInArea(int x, int y) {
 		List<Figure> children2 = getChildren();
 		for (Figure figure : children2) {
 			if (figure instanceof RenderingImageFigure) {
 				if (figure.containsPoint(x, y)) {
 					return Boolean.TRUE;
 				}
 			}
 		}
 
 		GraphicsAlgorithm[] gas = getSelectionArea();
 		if (gas != null) {
 			for (int i = 0; i < gas.length; i++) {
 				IFigure figure = getPictogramElementDelegate().getFigureForGraphicsAlgorithm(gas[i]);
 				if (figure != null && !this.equals(figure)) { // don't check the figure
 					if (figure.containsPoint(x, y)) {
 						return Boolean.TRUE;
 					}
 				} else {
 					return null;
 				}
 			}
 			return Boolean.FALSE;
 		}
 		return null;
 	}
 
 	/**
 	 * Returns true, if the given point is contained inside this Shape. This
 	 * implementation just forwards to <code>super.contains(x, y)</code>.
 	 * 
 	 * @param x
 	 *            The x-coordinate of the point to check.
 	 * @param y
 	 *            The y-coordinate of the point to check.
 	 * @see #containsPoint(int, int)
 	 * @return true, if the given point is contained inside this Shape.
 	 */
 	protected boolean containsPointInFigure(int x, int y) {
 		return super.containsPoint(x, y);
 	}
 
 	/**
 	 * A helper method, which fills the Path according to the fill-colors of
 	 * this figures GraphicsAlgorithm. This can be a single-color filling or a
 	 * gradient filling.
 	 * 
 	 * @param graphics
 	 *            The graphics on which to fill the Path.
 	 * @param path
 	 *            The Path which to fill.
 	 */
 	protected void fillPath(Graphics graphics, Path path) {
 		RenderingStyle renderingStyle = Graphiti.getGaService().getRenderingStyle(graphicsAlgorithm, true);
 
 		if (adaptBackgroundToHover(graphics)) {
 			// fill area
 			graphics.fillPath(path);
 		} else {
 			if (renderingStyle != null) {
 				graphics.pushState();
 				try {
					graphics.clipPath(path);
 					int styleAdaptation = getStyleAdaptation();
 					String coloredAreaId = renderingStyle.getPredefinedStyleId();
 					ColoredArea coloredArea[] = PredefinedColoredAreas.getColoredAreas(coloredAreaId, styleAdaptation);
 					for (int i = 0; i < coloredArea.length; i++) {
 						GFFigureUtil.paintColorFlow(getBounds(), graphics, coloredArea[i], getZoomLevel(graphics), true);
 					}
 				} finally {
 					graphics.popState();
 				}
 
 			} else {
 				setBackgroundWithoutStyle(graphics, path);
 			}
 		}
 	}
 
 	private void setBackgroundWithoutStyle(Graphics graphics, Path path) {
 		// set background color depending on visual state
 		Color oldBackground = graphics.getBackgroundColor();
 		if (getPreferences().getVisualStateRendering() == 1) {
 			int selectionFeedback = getVisualState().getSelectionFeedback();
 			if (selectionFeedback == IVisualState.SELECTION_PRIMARY)
 				graphics.setBackgroundColor(GFColorConstants.MEDIUM_BLUE);
 			else if (selectionFeedback == IVisualState.SELECTION_SECONDARY)
 				graphics.setBackgroundColor(GFColorConstants.LIGHT_BLUE);
 			else
 				graphics.setBackgroundColor(GFColorConstants.VERY_LIGHT_BLUE);
 
 			int actionTargetFeedback = getVisualState().getActionTargetFeedback();
 			if (actionTargetFeedback == IVisualState.ACTION_TARGET_ALLOWED)
 				graphics.setBackgroundColor(GFColorConstants.LIGHT_GREEN);
 			else if (actionTargetFeedback == IVisualState.ACTION_TARGET_FORBIDDEN)
 				graphics.setBackgroundColor(GFColorConstants.LIGHT_ORANGE);
 		} else {
 			// usually not supported for simple BackgroundColor
 			// customers must use RenderingStyle instead
 		}
 		// fill area
 		graphics.fillPath(path);
 
 		// revert to old background color
 		graphics.setBackgroundColor(oldBackground);
 	}
 
 	private int getStyleAdaptation() {
 		int styleAdaptation = PredefinedColoredAreas.STYLE_ADAPTATION_DEFAULT;
 		if (getPreferences().getVisualStateRendering() == 1) {
 			styleAdaptation = PredefinedColoredAreas.STYLE_ADAPTATION_DEFAULT;
 			int selectionFeedback = getVisualState().getSelectionFeedback();
 			if (selectionFeedback == IVisualState.SELECTION_PRIMARY)
 				styleAdaptation = PredefinedColoredAreas.STYLE_ADAPTATION_PRIMARY_SELECTED;
 			else if (selectionFeedback == IVisualState.SELECTION_SECONDARY)
 				styleAdaptation = PredefinedColoredAreas.STYLE_ADAPTATION_SECONDARY_SELECTED;
 
 			int actionTargetFeedback = getVisualState().getActionTargetFeedback();
 			if (actionTargetFeedback == IVisualState.ACTION_TARGET_ALLOWED)
 				styleAdaptation = PredefinedColoredAreas.STYLE_ADAPTATION_ACTION_ALLOWED;
 			else if (actionTargetFeedback == IVisualState.ACTION_TARGET_FORBIDDEN)
 				styleAdaptation = PredefinedColoredAreas.STYLE_ADAPTATION_ACTION_FORBIDDEN;
 		} else {
 			styleAdaptation = PredefinedColoredAreas.STYLE_ADAPTATION_DEFAULT;
 			int selectionFeedback = getVisualState().getSelectionFeedback();
 			if (selectionFeedback == IVisualState.SELECTION_PRIMARY)
 				styleAdaptation = PredefinedColoredAreas.STYLE_ADAPTATION_PRIMARY_SELECTED;
 			else if (selectionFeedback == IVisualState.SELECTION_SECONDARY)
 				styleAdaptation = PredefinedColoredAreas.STYLE_ADAPTATION_SECONDARY_SELECTED;
 
 			int actionTargetFeedback = getVisualState().getActionTargetFeedback();
 			if (actionTargetFeedback == IVisualState.ACTION_TARGET_ALLOWED)
 				styleAdaptation = PredefinedColoredAreas.STYLE_ADAPTATION_SECONDARY_SELECTED;
 		}
 		return styleAdaptation;
 	}
 
 	private boolean adaptBackgroundToHover(Graphics graphics) {
 		// Compute hover state and adapt background color accordingly
 		// We distinguish the state where we hover over a figure
 		// without the parent being slected and with the parent being selected.
 		if (getVisualState().getHoverFeedback() == IVisualState.HOVER_ON) {
 			IToolBehaviorProvider tbp = getConfigurationProvider().getDiagramTypeProvider().getCurrentToolBehaviorProvider();
 			IFigure parent = getParent();
 			boolean parentSelected = false;
 			if (parent instanceof GFAbstractShape) {
 				GFAbstractShape gfa = (GFAbstractShape) parent;
 				IVisualState visualState = gfa.getVisualState();
 				parentSelected = visualState.getSelectionFeedback() == IVisualState.SELECTION_PRIMARY;
 			}
 			PictogramElement pe = getPictogramElementDelegate().getPictogramElement();
 			if (!(pe instanceof org.eclipse.graphiti.mm.pictograms.Shape))
 				return false;
 			org.eclipse.graphiti.mm.pictograms.Shape s = (org.eclipse.graphiti.mm.pictograms.Shape) pe;
 			ISelectionInfo selectionInfo = tbp.getShapeSelectionInfo(s);
 			IColorConstant hoverColor = null;
 			hoverColor = selectionInfo.getHoverColor();
 			if (parentSelected)
 				hoverColor = selectionInfo.getHoverColorParentSelected();
 			if (hoverColor != null) {
 				Color hoverColorSwt = DataTypeTransformation.toSwtColor(getConfigurationProvider(), hoverColor);
 				graphics.setBackgroundColor(hoverColorSwt);
 				return true;
 			}
 
 		}
 		return false;
 	}
 
 	/**
 	 * Outlines or fills this Shape on the given Graphics. First the outline
 	 * Path is be determined by calling
 	 * {@link #createPath(Rectangle, Graphics, boolean)}. Afterwards this Path
 	 * is either outlined on the Graphics using the correct line-width or filled
 	 * using a single color or color-gradients.
 	 * 
 	 * @param graphics
 	 *            The Graphics on which to outline or fill this Shape.
 	 * @param isFill
 	 *            if true, fills this Shape, otherwise outlines this Shape.
 	 */
 	protected void paintShape(Graphics graphics, boolean isFill) {
 		// initialize Graphics
 		int oldLineWidth = graphics.getLineWidth();
 		graphics.setLineWidth(getLineWidth(graphics));
 
 		// get Path
 		double zoom = getZoomLevel(graphics);
 		int lw = getLineWidth(graphics);
 		Rectangle pathbounds = GFFigureUtil.getAdjustedRectangle(getBounds(), zoom, lw);
 		if (isFill) {
 			transformToFillBounds(pathbounds, graphics);
 		}
 		Path path = createPath(pathbounds, graphics, isFill);
 
 		// outline or fill Path
 		if (isFill) {
 			fillPath(graphics, path);
 		} else {
 			graphics.drawPath(path);
 		}
 
 		// reset Graphics
 		path.dispose();
 		graphics.setLineWidth(oldLineWidth);
 	}
 
 	// ======================== overwritten methods ===========================
 
 	/**
 	 * First initializes the given Graphics with settings like alpha-value,
 	 * antialias-value, ... Afterwards calls
 	 * <code>super.paintFigure(graphics)</code> to continue with the default
 	 * painting mechanisms.
 	 * 
 	 * @param graphics
 	 *            The Graphics on which to paint.
 	 */
 	@Override
 	public void paintFigure(Graphics graphics) {
 		if (GraphitiInternal.getEmfService().isObjectAlive(graphicsAlgorithm)) {
 			double transparency = Graphiti.getGaService().getTransparency(graphicsAlgorithm, true);
 			int alpha = (int) ((1.0 - transparency) * 255.0);
 			graphics.setAlpha(alpha);
 
 			graphics.setAntialias(SWT.ON);
 
 			super.paintFigure(graphics);
 		}
 	}
 
 	/**
 	 * Fills this Shape on the given Graphics. This implementation just forwards
 	 * to {@link #paintShape(Graphics, boolean)}.
 	 * 
 	 * @param graphics
 	 *            The Graphics on which to fill this Shape.
 	 */
 	@Override
 	protected void fillShape(Graphics graphics) {
 		paintShape(graphics, true);
 	}
 
 	/**
 	 * Outlines this Shape on the given Graphics. This implementation just
 	 * forwards to {@link #paintShape(Graphics, boolean)}.
 	 * 
 	 * @param graphics
 	 *            The Graphics on which to outline this Shape.
 	 */
 	@Override
 	protected void outlineShape(Graphics graphics) {
 		paintShape(graphics, false);
 	}
 
 	/**
 	 * Returns true, if the given point is contained inside this Shape. It first
 	 * calls {@link #containsPointInArea(int, int)} to check if there is a
 	 * special selection-area defined for this Shape. If not, it returns
 	 * {@link #containsPointInFigure(int, int)}.
 	 * <p>
 	 * This method is final. Override {@link #containsPointInFigure(int, int)}
 	 * if needed.
 	 * 
 	 * @param x
 	 *            The x-coordinate of the point to check.
 	 * @param y
 	 *            The y-coordinate of the point to check.
 	 * @return true, if the given point is contained inside this Shape.
 	 */
 	@Override
 	public final boolean containsPoint(int x, int y) {
 		Boolean ret = containsPointInArea(x, y);
 		if (ret != null) {
 			// If a selection area is available, but the mouse is not inside this selection area
 			// and mouse is inside this figure (e.g. the ghost figure) then check the main figures 
 			// of all child edit parts.
 			// It could be possible that one of these child figures is rendered outside and the mouse is inside this 
 			// child figure. In this case return true, otherwise the child edit part will never be selectable (the contains
 			// method of child edit part figure will never be called).
 			if (ret.booleanValue() == false && getSelectionArea() != null && containsPointInFigure(x, y)) {
 				List<IFigure> fList = getPictogramElementDelegate().getMainFiguresFromChildEditparts();
 				for (IFigure figure : fList) {
 					if (figure.containsPoint(x, y)) {
 						return true;
 					}
 				}
 			}
 			return ret.booleanValue();
 		}
 		return containsPointInFigure(x, y);
 	}
 
 	// ========================== interface IHandleInsets =====================
 
 	/**
 	 * Returns the selection handle bounds of this Shape. First it checks, if a
 	 * special selection GraphicsAlgorithm is defined for this Shape (see
 	 * {@link #getSelectionGraphicsAlgorithm()}. Otherwise it just returns the
 	 * bounds of this Shape.
 	 * 
 	 * @return The selection handle bounds of this Shape.
 	 */
 	public Rectangle getHandleBounds() {
 		Rectangle ret = null;
 		final GraphicsAlgorithm selectionGa = getSelectionGraphicsAlgorithm();
 		if (selectionGa != null) {
 			IFigure selectionFigure = getPictogramElementDelegate().getFigureForGraphicsAlgorithm(selectionGa);
 			if (selectionFigure != null) {
 				ret = selectionFigure.getBounds();
 			}
 		}
 
 		if (ret == null) {
 			ret = getBounds();
 		}
 
 		return ret;
 	}
 
 	// ====================== interface IVisualStateHolder ====================
 
 	/**
 	 * Returns the visual state of this shape.
 	 * 
 	 * @return The visual state of this shape.
 	 */
 	public IVisualState getVisualState() {
 		return getPictogramElementDelegate().getVisualState();
 	}
 
 	/**
 	 * Is called after the visual state changed.
 	 */
 	public void visualStateChanged() {
 		// The colors might have changed, so force a repaint()
 		repaint();
 	}
 
 	// ===================== support of selection-behavior ====================
 
 	/**
 	 * @param selectionGraphicsAlgorithm
 	 *            the selectionGraphicsAlgorithm to set
 	 */
 	public void setSelectionGraphicsAlgorithm(GraphicsAlgorithm selectionGraphicsAlgorithm) {
 		this.selectionGraphicsAlgorithm = selectionGraphicsAlgorithm;
 	}
 
 	/**
 	 * @param selectionArea
 	 *            the selectionArea to set
 	 */
 	public void setSelectionArea(GraphicsAlgorithm[] selectionArea) {
 		this.selectionArea = selectionArea;
 	}
 
 	protected GFPreferences getPreferences() {
 		return GFPreferences.getInstance();
 	}
 }
