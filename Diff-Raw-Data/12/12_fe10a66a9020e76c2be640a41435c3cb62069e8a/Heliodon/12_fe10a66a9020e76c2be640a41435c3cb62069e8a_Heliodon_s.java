 package org.concord.energy3d.shapes;
 
 import java.nio.FloatBuffer;
 import java.util.Calendar;
 import java.util.Date;
 
 import org.concord.energy3d.gui.MainFrame;
 import org.concord.energy3d.scene.Scene;
 import org.concord.energy3d.scene.SceneManager;
 import org.concord.energy3d.util.FontManager;
 import org.concord.energy3d.util.Util;
 
 import com.ardor3d.bounding.BoundingVolume;
 import com.ardor3d.extension.effect.bloom.BloomRenderPass;
 import com.ardor3d.framework.Canvas;
 import com.ardor3d.input.MouseButton;
 import com.ardor3d.input.MouseState;
 import com.ardor3d.input.logical.InputTrigger;
 import com.ardor3d.input.logical.LogicalLayer;
 import com.ardor3d.input.logical.MouseButtonPressedCondition;
 import com.ardor3d.input.logical.MouseButtonReleasedCondition;
 import com.ardor3d.input.logical.MouseMovedCondition;
 import com.ardor3d.input.logical.TriggerAction;
 import com.ardor3d.input.logical.TwoInputStates;
 import com.ardor3d.intersection.IntersectionRecord;
 import com.ardor3d.intersection.PickResults;
 import com.ardor3d.intersection.PickingUtil;
 import com.ardor3d.intersection.PrimitivePickResults;
 import com.ardor3d.light.DirectionalLight;
 import com.ardor3d.math.ColorRGBA;
 import com.ardor3d.math.MathUtils;
 import com.ardor3d.math.Matrix3;
 import com.ardor3d.math.Ray3;
 import com.ardor3d.math.Vector2;
 import com.ardor3d.math.Vector3;
 import com.ardor3d.math.type.ReadOnlyTransform;
 import com.ardor3d.math.type.ReadOnlyVector3;
 import com.ardor3d.renderer.IndexMode;
 import com.ardor3d.renderer.pass.BasicPassManager;
 import com.ardor3d.renderer.pass.RenderPass;
 import com.ardor3d.renderer.queue.RenderBucketType;
 import com.ardor3d.renderer.state.BlendState;
 import com.ardor3d.renderer.state.ClipState;
 import com.ardor3d.renderer.state.MaterialState;
 import com.ardor3d.renderer.state.OffsetState;
 import com.ardor3d.renderer.state.OffsetState.OffsetType;
 import com.ardor3d.renderer.state.ShadingState;
 import com.ardor3d.renderer.state.ShadingState.ShadingMode;
 import com.ardor3d.renderer.state.WireframeState;
 import com.ardor3d.scenegraph.Line;
 import com.ardor3d.scenegraph.Mesh;
 import com.ardor3d.scenegraph.Node;
 import com.ardor3d.scenegraph.Spatial;
 import com.ardor3d.scenegraph.hint.CullHint;
 import com.ardor3d.scenegraph.hint.LightCombineMode;
 import com.ardor3d.scenegraph.hint.TransparencyType;
 import com.ardor3d.scenegraph.shape.Sphere;
 import com.ardor3d.ui.text.BMText;
 import com.ardor3d.ui.text.BMText.Align;
 import com.ardor3d.ui.text.BMText.AutoFade;
 import com.ardor3d.ui.text.BMText.AutoScale;
 import com.ardor3d.ui.text.BMText.Justify;
 import com.ardor3d.util.geom.BufferUtils;
 
 public class Heliodon {
 	private static final double BASE_DIVISIONS = 72.0;
 	private static final double DECLINATION_DIVISIONS = 12.0;
 	private static final double HOUR_DIVISIONS = 96.0;
 	private static final int BASE_VERTICES = 72 * 2;
 	private static final int SUN_REGION_VERTICES = 8064 / 3;
 	private static final int SUN_PATH_VERTICES = 291 / 3;
 	private static final double TILT_ANGLE = 23.45 / 180.0 * Math.PI;
 	private final Node root = new Node("Heliodon Root");
 	private final Spatial sun = new Sphere("Sun", 20, 20, 0.3);
 	private final DirectionalLight light;
 	private final BloomRenderPass bloomRenderPass;
 	private final Line sunPath;
 	private final Mesh sunRegion;
 	private final PickResults pickResults;
 	private final Mesh base;
 	private final Mesh baseTicks;
 	private final Calendar calendar = Calendar.getInstance();
 	private double baseAngle = 0;
 	private double hourAngle;
 	private double declinationAngle;
 	private double observerLatitude;
 	private boolean sunGrabbed = false;
 	private boolean selectDifferentDeclinationWithMouse = false;
 	private boolean lock = false;
 
 	public Heliodon(final Node scene, final DirectionalLight light, final BasicPassManager passManager, final LogicalLayer logicalLayer) {
 		this.light = light;
 		this.pickResults = new PrimitivePickResults();
 		this.pickResults.setCheckDistance(true);
 		
 		// Sun
 		this.bloomRenderPass = new BloomRenderPass(SceneManager.getInstance().getCanvas().getCanvasRenderer().getCamera(), 4);
 		passManager.add(bloomRenderPass);
 		bloomRenderPass.add(sun);
 		final MaterialState material = new MaterialState();
 		material.setEmissive(ColorRGBA.WHITE);
 		sun.setRenderState(material);		
 		sun.setTranslation(0, 0, 5);
 		root.attachChild(sun);
 
 		// Sun Path
 		sunPath = new Line("Sun Path", BufferUtils.createVector3Buffer(SUN_PATH_VERTICES), null, null, null);
 		sunPath.setDefaultColor(ColorRGBA.YELLOW);
 		sunPath.setLineWidth(3);
 		sunPath.getMeshData().setIndexMode(IndexMode.LineStrip);
 		sunPath.getSceneHints().setLightCombineMode(LightCombineMode.Off);
 		root.attachChild(sunPath);
 
 		// Sun Region
 		sunRegion = new Mesh("Sun Region");
		sunRegion.getSceneHints().setCullHint(CullHint.Always);
 		sunRegion.getMeshData().setVertexBuffer(BufferUtils.createVector3Buffer(SUN_REGION_VERTICES));
 		sunRegion.getMeshData().setIndexMode(IndexMode.Quads);
 		sunRegion.setDefaultColor(new ColorRGBA(1f, 1f, 0f, 0.5f));
 		final BlendState blendState = new BlendState();
 		blendState.setBlendEnabled(true);
 		sunRegion.setRenderState(blendState);
 		sunRegion.getSceneHints().setTransparencyType(TransparencyType.TwoPass);
 		sunRegion.getSceneHints().setRenderBucketType(RenderBucketType.Transparent);
 		sunRegion.getSceneHints().setLightCombineMode(LightCombineMode.Off);
 		root.attachChild(sunRegion);
 
 		// Sun Region Wireframe
 		final RenderPass wireframePass = new RenderPass();
 		wireframePass.setPassState(new WireframeState());
 		wireframePass.add(sunRegion);
 		passManager.add(wireframePass);
 
 		// Base
         base = new Mesh("Base");
 		base.getMeshData().setVertexBuffer(BufferUtils.createVector3Buffer(BASE_VERTICES + 2));
 		base.getMeshData().setColorBuffer(BufferUtils.createColorBuffer(BASE_VERTICES + 2));
 		base.getMeshData().setIndexMode(IndexMode.QuadStrip);
 		final OffsetState offsetState = new OffsetState();
 		offsetState.setTypeEnabled(OffsetType.Fill, true);
 		offsetState.setFactor(0.2f);
 		offsetState.setUnits(3f);		
 		base.setRenderState(offsetState);
 		final ShadingState shadingState = new ShadingState();
 		shadingState.setShadingMode(ShadingMode.Flat);
 		base.setRenderState(shadingState);
 		base.getSceneHints().setLightCombineMode(LightCombineMode.Off);
 		root.attachChild(base);
 		
 		// Base Ticks
 		baseTicks = new Mesh("Base Ticks");
 		baseTicks.getMeshData().setVertexBuffer(BufferUtils.createVector3Buffer(BASE_VERTICES));
 		baseTicks.getMeshData().setIndexMode(IndexMode.Lines);
 		baseTicks.setDefaultColor(ColorRGBA.BLACK);
 		baseTicks.getSceneHints().setLightCombineMode(LightCombineMode.Off);
 		root.attachChild(baseTicks);
 		
 		// Compass Labels N S E W
 		final BMText northLabel = new BMText("North", "N", FontManager.getInstance().getAnnotationFont(), Align.Center, Justify.Left, false);
 		northLabel.setAutoRotate(false);
 		northLabel.setAutoScale(AutoScale.Off);
 		northLabel.setAutoFade(AutoFade.Off);
 		northLabel.setRotation(new Matrix3().fromAngles(-MathUtils.HALF_PI, 0, 0));
 		northLabel.setTranslation(0, 6, 0);
 		root.attachChild(northLabel);
 		final BMText southLabel = new BMText("South", "S", FontManager.getInstance().getAnnotationFont(), Align.Center);
 		southLabel.setAutoRotate(false);
 		southLabel.setAutoScale(AutoScale.Off);
 		southLabel.setRotation(new Matrix3().fromAngles(-MathUtils.HALF_PI, 0, Math.PI));
 		southLabel.setTranslation(0, -6, 0);
 		root.attachChild(southLabel);
 		final BMText eastLabel = new BMText("East", "E", FontManager.getInstance().getAnnotationFont(), Align.Center);
 		eastLabel.setAutoRotate(false);
 		eastLabel.setAutoScale(AutoScale.Off);
 		eastLabel.setRotation(new Matrix3().fromAngles(-MathUtils.HALF_PI, 0, -MathUtils.HALF_PI));
 		eastLabel.setTranslation(6, 0, 0);
 		root.attachChild(eastLabel);
 		final BMText westLabel = new BMText("West", "W", FontManager.getInstance().getAnnotationFont(), Align.Center);
 		westLabel.setAutoRotate(false);
 		westLabel.setAutoScale(AutoScale.Off);
 		westLabel.setRotation(new Matrix3().fromAngles(-MathUtils.HALF_PI, 0, MathUtils.HALF_PI));
 		westLabel.setTranslation(-6, 0, 0);
 		root.attachChild(westLabel);
 		
 		// Clip
 		final ClipState cs = new ClipState();
 		cs.setEnableClipPlane(0, true);
 		cs.setClipPlaneEquation(0, 0, 0, 1, 0);
 		sunPath.setRenderState(cs);
 		sunRegion.setRenderState(cs);
 		
 		initMouse(logicalLayer);
 		
 		root.getSceneHints().setCullHint(CullHint.Always);
 		scene.attachChild(root);
 		draw();
 	}
 
 	private void initMouse(final LogicalLayer logicalLayer) {
 		logicalLayer.registerTrigger(new InputTrigger(new MouseButtonPressedCondition(MouseButton.LEFT), new TriggerAction() {
 			public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
 				lock = true;
 				final int x = inputStates.getCurrent().getMouseState().getX();
 				final int y = inputStates.getCurrent().getMouseState().getY();
 				final Ray3 pickRay = SceneManager.getInstance().getCanvas().getCanvasRenderer().getCamera().getPickRay(new Vector2(x, y), false, null);
 				pickResults.clear();
 				PickingUtil.findPick(sun, pickRay, pickResults);
 				if (pickResults.getNumber() != 0)
 					sunGrabbed = true;
 				else
 					sunGrabbed = false;
 				selectDifferentDeclinationWithMouse = false;
 				SceneManager.getInstance().setMouseControlEnabled(!sunGrabbed);
 			}
 		}));
 
 		logicalLayer.registerTrigger(new InputTrigger(new MouseButtonReleasedCondition(MouseButton.LEFT), new TriggerAction() {
 			public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {				
 				sunGrabbed = false;
				sunRegion.getSceneHints().setCullHint(CullHint.Always);
 				SceneManager.getInstance().setMouseControlEnabled(true);
 				lock = false;
 			}
 		}));
 
 		logicalLayer.registerTrigger(new InputTrigger(new MouseMovedCondition(), new TriggerAction() {
 			public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
 				if (!sunGrabbed)
 					return;
 				final MouseState mouse = inputStates.getCurrent().getMouseState();
 				final Ray3 pickRay = SceneManager.getInstance().getCanvas().getCanvasRenderer().getCamera().getPickRay(new Vector2(mouse.getX(), mouse.getY()), false, null);
 
 				pickResults.clear();
 				PickingUtil.findPick(sunRegion, pickRay, pickResults);
 				final Vector3 intersectionPoint;
 				if (pickResults.getNumber() > 0) {
 					final IntersectionRecord intersectionRecord = pickResults.getPickData(0).getIntersectionRecord();
 					intersectionPoint = intersectionRecord.getIntersectionPoint(intersectionRecord.getClosestIntersection());
 				} else
 					intersectionPoint = null;
 
 				double smallestDistance = Double.MAX_VALUE;
 				int hourVertex = -1;
 				int totalHourVertices = 0;
 				final Vector3 newSunLocation = new Vector3();
 				final Vector3 p = new Vector3();
 				final Vector3 p_abs = new Vector3();
 				final ReadOnlyTransform rootTansform = root.getTransform();
 				if (!selectDifferentDeclinationWithMouse) {
 					final FloatBuffer buf = sunPath.getMeshData().getVertexBuffer();
 					buf.rewind();					
 					while (buf.hasRemaining()) {
 						p.set(buf.get(), buf.get(), buf.get());
 						rootTansform.applyForward(p, p_abs);
 						final double d;
 						if (intersectionPoint != null)
 							d = intersectionPoint.distanceSquared(p_abs);
 						else
 							d = pickRay.distanceSquared(p_abs, null);
 						if (d < smallestDistance) {
 							smallestDistance = d;
 							hourVertex = buf.position() / 3 - 1;
 							newSunLocation.set(p);
 						}
 					}
 					totalHourVertices = buf.limit() / 3;
 				}
 				if (smallestDistance > 5.0 * root.getTransform().getScale().getX() * root.getTransform().getScale().getX())
 					selectDifferentDeclinationWithMouse = true;
 				
 				boolean declinationChanged = false;
 
 				if (selectDifferentDeclinationWithMouse) {
 					sunRegion.getSceneHints().setCullHint(CullHint.Inherit);
 					int rowCounter = 0;
 					int resultRow = -1;
 					final FloatBuffer buf = sunRegion.getMeshData().getVertexBuffer();
 					buf.rewind();
 					final double r = 5.0 / 2.0;
 					final Vector3 prev = new Vector3();
 					int quadVertexCounter = 0;
 					final double maxVertexInRow = HOUR_DIVISIONS * 4.0;
 					int rowVertexCounter = 0;
 					boolean foundInThisRow = false;
 					while (buf.hasRemaining()) {
 						p.set(buf.get(), buf.get(), buf.get());
 						rootTansform.applyForward(p, p_abs);
 						final double d;
 						if (intersectionPoint != null)
 							d = intersectionPoint.distanceSquared(p_abs);
 						else
 							d = pickRay.distanceSquared(p_abs, null);
 						if (d < smallestDistance && p.getZ() >= -MathUtils.ZERO_TOLERANCE) { 
 							smallestDistance = d;
 							newSunLocation.set(p);
 							resultRow = rowCounter + (quadVertexCounter >= 2 ? 1 : 0);
 							hourVertex = rowVertexCounter / 4 + (quadVertexCounter == 1 || quadVertexCounter == 2 ? 1 : 0);
 							foundInThisRow = true;
 						}
 						if (prev.lengthSquared() != 0 && (prev.distance(p) > r || rowVertexCounter >= maxVertexInRow)) {
 							rowCounter++;
 							if (foundInThisRow)
 								totalHourVertices = rowVertexCounter / 4;
 							foundInThisRow = false;
 							rowVertexCounter = 0;
 						}
 						prev.set(p);
 						quadVertexCounter = (quadVertexCounter + 1) % 4;						
 						rowVertexCounter++;
 					}
 					rowCounter++;
 					if (resultRow != -1) {
 						if (rowCounter < 10 && observerLatitude > 0)
 							resultRow += 10 - rowCounter;
 						double newDeclinationAngle = -TILT_ANGLE + (2.0 * TILT_ANGLE * resultRow / DECLINATION_DIVISIONS);
 						declinationChanged = Math.abs(newDeclinationAngle - declinationAngle) > MathUtils.EPSILON;
 						if (declinationChanged) {							
 							setDeclinationAngle(newDeclinationAngle, false, true);							
 							drawSunPath();
 						}
 					}					
 				}				
 				final double newHourAngle = ((double)hourVertex - Math.floor(totalHourVertices / 2.0)) * Math.PI / 48.0;
 				final boolean hourAngleChanged = Math.abs(newHourAngle - hourAngle) > MathUtils.EPSILON;
 				if (hourAngleChanged) {
 					System.out.println("hourVertex = " + hourVertex + " / " + totalHourVertices);
 					setHourAngle(newHourAngle, false, true);					
 				}
 				if (declinationChanged || hourAngleChanged)
 					setSunLocation(newSunLocation);
 			}
 		}));
 	}
 
 	public Node getRoot() {
 		return root;
 	}
 
 	public double getBaseAngle() {
 		return baseAngle;
 	}
 
 	public void setBaseAngle(final double baseAngle) {
 		this.baseAngle = baseAngle % 360;
 		root.setRotation(new Matrix3().fromAngleAxis(baseAngle * Math.PI / 180, Vector3.UNIT_Z));
 		drawSun();
 	}
 
 	public double getHourAngle() {
 		return hourAngle;
 	}
 
 	public void setHourAngle(final double hourAngle, final boolean redrawHeliodon, final boolean updateGUI) {
 		this.hourAngle = toPlusMinusPIRange(hourAngle, -Math.PI, Math.PI);
 		final int minutes = (int)Math.round(this.hourAngle / Math.PI * 12 * 60 + 12 * 60);
 		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, minutes);
		
		light.setEnabled(this.hourAngle >= -MathUtils.HALF_PI && this.hourAngle <= MathUtils.HALF_PI);
 		
 		if (updateGUI)
 			MainFrame.getInstance().getTimeSpinner().setValue(calendar.getTime());
 		
 		if (redrawHeliodon)
 			drawSun();
 	}
 
 	public double getDeclinationAngle() {
 		return declinationAngle;
 	}
 
 	public void setDeclinationAngle(final double declinationAngle, final boolean redrawHeliodon, final boolean updateGUI) {
 		this.declinationAngle = toPlusMinusPIRange(declinationAngle, -TILT_ANGLE, TILT_ANGLE);
 		final double days = MathUtils.asin(this.declinationAngle / TILT_ANGLE) / MathUtils.TWO_PI * 365.25 - 284.0;
 		calendar.set(calendar.get(Calendar.YEAR), 0, (int)Math.round(days));
 		
 		if (updateGUI)
 			MainFrame.getInstance().getDateSpinner().setValue(calendar.getTime());
 		
 		if (redrawHeliodon) {
 			drawSunPath();
 			drawSun();
 		}
 	}
 
 	public double getObserverLatitude() {
 		return observerLatitude;
 	}
 
 	public void setObserverLatitude(double observerLatitude) {
 		this.observerLatitude = toPlusMinusPIRange(observerLatitude, -MathUtils.HALF_PI, MathUtils.HALF_PI);
 		drawSunRegion();
 		drawSunPath();
 		drawSun();		
 	}
 
 	public void setVisible(final boolean visible) {
 		root.getSceneHints().setCullHint(visible ? CullHint.Inherit : CullHint.Always);
 		bloomRenderPass.setEnabled(visible);
 		if (visible)
 			updateSize();
 	}
 
 	public void updateSize() {
 		Scene.getRoot().updateWorldBound(true);
 		final BoundingVolume bounds = Scene.getRoot().getWorldBound();
 		if (bounds == null)
 			root.setScale(1);
 		else {
 			final double scale = (Util.findBoundLength(bounds) / 2.0 + bounds.getCenter().length()) / 5.0;
 			System.out.println("Heliodon scale = " + scale);
 			root.setScale(scale);
 		}
 	}
 
 	private Vector3 computeSunLocation(final double hourAngle, final double declinationAngle, final double observerLatitude) {
 		final double altitudeAngle = MathUtils.asin(MathUtils.sin(declinationAngle) * MathUtils.sin(observerLatitude) + MathUtils.cos(declinationAngle) * MathUtils.cos(hourAngle) * MathUtils.cos(observerLatitude));
 		final double x_azm = MathUtils.sin(hourAngle) * MathUtils.cos(declinationAngle);
 		final double y_azm = (-(MathUtils.cos(hourAngle)) * MathUtils.cos(declinationAngle) * MathUtils.sin(observerLatitude)) + (MathUtils.cos(observerLatitude) * MathUtils.sin(declinationAngle));
 		final double azimuthAngle = Math.atan2(y_azm, x_azm);
 
 		final double r = 5;
 		final Vector3 coords = new Vector3(r, azimuthAngle, altitudeAngle);
 		return MathUtils.sphericalToCartesianZ(coords, coords);
 	}
 
 	private double toPlusMinusPIRange(final double radian, double min, double max) {
 		double result = radian - (int) (radian / MathUtils.TWO_PI) * MathUtils.TWO_PI;
 		if (Math.abs(result) > Math.PI)
 			result = -Math.signum(result) * (MathUtils.TWO_PI - Math.abs(result));
 		if (result < min)
 			result = min;
 		else if (result > max)
 			result = max;
 		return result;
 	}
 
 	private void drawSunRegion() {
 		final FloatBuffer buf = sunRegion.getMeshData().getVertexBuffer();
 		buf.limit(buf.capacity());
 		buf.rewind();
 		final double declinationStep = 2.0 * TILT_ANGLE / DECLINATION_DIVISIONS;
 		final double hourStep = MathUtils.TWO_PI / HOUR_DIVISIONS;
 		int limit = 0;
 		for (double declinationAngle = -TILT_ANGLE; declinationAngle < TILT_ANGLE - declinationStep / 2.0; declinationAngle += declinationStep) {
 			for (double hourAngle = -Math.PI; hourAngle < Math.PI - hourStep / 2.0; hourAngle += hourStep) {
 				double hourAngle2 = hourAngle + hourStep;
 				double declinationAngle2 = declinationAngle + declinationStep;
 				if (hourAngle2 > Math.PI)
 					hourAngle2 = Math.PI;
 				if (declinationAngle2 > TILT_ANGLE)
 					declinationAngle2 = TILT_ANGLE;
 				final Vector3 v1 = computeSunLocation(hourAngle, declinationAngle, observerLatitude);
 				final Vector3 v2 = computeSunLocation(hourAngle2, declinationAngle, observerLatitude);
 				final Vector3 v3 = computeSunLocation(hourAngle2, declinationAngle2, observerLatitude);
 				final Vector3 v4 = computeSunLocation(hourAngle, declinationAngle2, observerLatitude);
 				if (v1.getZ() >= 0 || v2.getZ() >= 0 || v3.getZ() >= 0 || v4.getZ() >= 0) {
 					buf.put(v1.getXf()).put(v1.getYf()).put(v1.getZf()).put(v2.getXf()).put(v2.getYf()).put(v2.getZf()).put(v3.getXf()).put(v3.getYf()).put(v3.getZf()).put(v4.getXf()).put(v4.getYf()).put(v4.getZf());
 					limit += 12;
 				}
 			}
 		}
 		buf.limit(limit);
 		sunRegion.getMeshData().updateVertexCount();
 		sunRegion.updateModelBound();
 		System.out.println("limit = " + limit + " / " + buf.capacity());
 	}
 
 	private void drawSunPath() {
 		final FloatBuffer buf = sunPath.getMeshData().getVertexBuffer();
 		buf.limit(buf.capacity());
 		buf.rewind();
 		final double step = MathUtils.TWO_PI / HOUR_DIVISIONS;
 		int limit = 0;
 		for (double hourAngle = -Math.PI; hourAngle < Math.PI + step / 2.0; hourAngle += step) {
 			final Vector3 v = computeSunLocation(hourAngle, declinationAngle, observerLatitude);
 			if (v.getZ() > -MathUtils.ZERO_TOLERANCE) {
 				buf.put(v.getXf()).put(v.getYf()).put(v.getZf());
 				limit += 3;
 			}
 		}		
 		buf.limit(limit);
 		System.out.println("sunpath limit = " + limit + " / " + buf.capacity());
 		sunPath.updateModelBound();
 		sunPath.getSceneHints().setCullHint(limit == 0 ? CullHint.Always : CullHint.Inherit);
 	}
 
 	private void drawSun() {
 		final Vector3 sunLocation = computeSunLocation(hourAngle, declinationAngle, observerLatitude);
 		setSunLocation(sunLocation);
 	}
 
 	private void setSunLocation(final ReadOnlyVector3 sunLocation) {
 		sun.setTranslation(sunLocation);
 		light.setDirection(sunLocation.negate(null));
 	}
 
 	private void draw() {
 		drawBase();
 		drawSunRegion();
 		drawSunPath();
 		drawSun();
 	}
 
 	public void updateBloom() {
 		bloomRenderPass.markNeedsRefresh();
 	}
 	
 	private void drawBase() {
 		final FloatBuffer buf = base.getMeshData().getVertexBuffer();
 		buf.rewind();
 		final FloatBuffer cbuf = base.getMeshData().getColorBuffer();
 		cbuf.rewind();
 		final FloatBuffer lbuf = baseTicks.getMeshData().getVertexBuffer();
 		lbuf.rewind();
 		final Vector3 p = new Vector3();
 		final double r = 5.0;
 		final double step = MathUtils.TWO_PI / BASE_DIVISIONS;
 		int counter = 0;
 		for (double angle = 0 ; angle < MathUtils.TWO_PI + step / 2.0; angle += step) {
 			final double trimedAngle;
 			if (angle > MathUtils.TWO_PI)
 				trimedAngle = MathUtils.TWO_PI;
 			else
 				trimedAngle = angle;
 			float width = 0.3f;
 			float zoffset = 0f;
 
 			MathUtils.sphericalToCartesianZ(p.set(r, trimedAngle, 0), p);
 			buf.put(p.getXf()).put(p.getYf()).put(p.getZf() + zoffset);			
 			MathUtils.sphericalToCartesianZ(p.set(r + width, trimedAngle, 0), p);
 			buf.put(p.getXf()).put(p.getYf()).put(p.getZf() + zoffset);
 			
 			final float c = ((counter - 1) / 3) % 2 == 0 ? 0.5f : 1.0f;
 			cbuf.put(c).put(c).put(c).put(c);
 			cbuf.put(c).put(c).put(c).put(c);
 			
 			if (MathUtils.TWO_PI - trimedAngle > MathUtils.ZERO_TOLERANCE) {
 				width = counter % 3 == 0 ? 0.5f : 0.3f;
 				MathUtils.sphericalToCartesianZ(p.set(r, trimedAngle, 0), p);
 				lbuf.put(p.getXf()).put(p.getYf()).put(p.getZf() + zoffset);
 				MathUtils.sphericalToCartesianZ(p.set(r + width, trimedAngle, 0), p);
 				lbuf.put(p.getXf()).put(p.getYf()).put(p.getZf() + zoffset);
 			}
 			counter++;
 		}
 		System.out.println("\nbase limit = " + buf.position() + " / " + buf.capacity());
 		base.updateModelBound();
 		baseTicks.updateModelBound();
 	}
 
 	public void setTime(final Date time) {
 		if (lock )
 			return;		
 		final Calendar timeCalendar = Calendar.getInstance();
 		timeCalendar.setTime(time);
 		calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
 		calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));
 		
 		final int minutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE) - 12 * 60;
 		setHourAngle(minutes / (12.0 * 60.0) * Math.PI, true, false);		
 	}
 
 	public void setDate(final Date date) {
 		if (lock)
 			return;
 		final Calendar dateCalendar = Calendar.getInstance();
 		dateCalendar.setTime(date);
 		calendar.set(dateCalendar.get(Calendar.YEAR), dateCalendar.get(Calendar.MONTH), dateCalendar.get(Calendar.DAY_OF_MONTH));		
 		
 		final int days = calendar.get(Calendar.DAY_OF_YEAR);
 		setDeclinationAngle(TILT_ANGLE * MathUtils.sin(MathUtils.TWO_PI*(284+days)/365.25), true, false);
 	}
 	
 }
