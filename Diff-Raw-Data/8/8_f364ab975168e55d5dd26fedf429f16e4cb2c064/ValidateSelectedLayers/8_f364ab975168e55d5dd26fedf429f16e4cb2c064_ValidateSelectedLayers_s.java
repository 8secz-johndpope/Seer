 /*
  * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
  * for visualizing and manipulating spatial features with geometry and attributes.
  *
  * Copyright (C) 2003 Vivid Solutions
  * 
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU General Public License
  * as published by the Free Software Foundation; either version 2
  * of the License, or (at your option) any later version.
  * 
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  * 
  * You should have received a copy of the GNU General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
  * 
  * For more information, contact:
  *
  * Vivid Solutions
  * Suite #1A
  * 2328 Government Street
  * Victoria BC  V8T 5G5
  * Canada
  *
  * (250)385-6040
  * www.vividsolutions.com
  */
 
 package org.contrib.ui.editorViews.toc.actions.geometry.qa;
 
 import java.awt.Color;
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 
 import org.contrib.images.IconLoader;
 import org.contrib.model.jump.adapter.FeatureCollectionAdapter;
 import org.contrib.model.jump.adapter.FeatureCollectionDatasourceAdapter;
 import org.contrib.model.jump.model.AttributeType;
 import org.contrib.model.jump.model.BasicFeature;
 import org.contrib.model.jump.model.CollectionMap;
 import org.contrib.model.jump.model.Feature;
 import org.contrib.model.jump.model.FeatureCollection;
 import org.contrib.model.jump.model.FeatureDataset;
 import org.contrib.model.jump.model.FeatureSchema;
 import org.contrib.model.jump.ui.MultiInputDialog;
 import org.gdms.data.DataSource;
 import org.gdms.data.DataSourceFactory;
 import org.gdms.data.SpatialDataSourceDecorator;
 import org.gdms.driver.DriverException;
 import org.gdms.driver.memory.ObjectMemoryDriver;
 import org.orbisgis.DataManager;
 import org.orbisgis.Services;
 import org.orbisgis.layerModel.ILayer;
 import org.orbisgis.layerModel.LayerException;
 import org.orbisgis.layerModel.MapContext;
 import org.orbisgis.outputManager.OutputManager;
 import org.orbisgis.pluginManager.background.BackgroundJob;
 import org.orbisgis.pluginManager.background.BackgroundManager;
 import org.orbisgis.progress.IProgressMonitor;
 
 import com.vividsolutions.jts.geom.Geometry;
 import com.vividsolutions.jts.geom.GeometryCollection;
 import com.vividsolutions.jts.geom.GeometryFactory;
 import com.vividsolutions.jts.geom.LineString;
 import com.vividsolutions.jts.geom.MultiLineString;
 import com.vividsolutions.jts.geom.MultiPoint;
 import com.vividsolutions.jts.geom.MultiPolygon;
 import com.vividsolutions.jts.geom.Point;
 import com.vividsolutions.jts.geom.Polygon;
 import com.vividsolutions.jump.qa.ValidationError;
 import com.vividsolutions.jump.qa.Validator;
 
 public class ValidateSelectedLayers {
 	private static String CHECK_BASIC_TOPOLOGY = "";
 
 	private final static String CHECK_POLYGON_ORIENTATION = "check polygon orientation";
 
 	private final static String CHECK_LINESTRINGS_SIMPLE = "check that linestrings are simple";
 
 	private final static String CHECK_POLYGONS_HAVE_NO_HOLES = "check polygons and multipolygons with holes";
 
 	private final static String CHECK_NO_REPEATED_CONSECUTIVE_POINTS = "check repeated consective points";
 
 	private final static String CHECK_MIN_SEGMENT_LENGTH = "check minimum segment length";
 
 	private final static String CHECK_MIN_ANGLE = "check minimum angle";
 
 	private final static String MIN_SEGMENT_LENGTH = "minimum segment length";
 
 	private final static String MIN_ANGLE = "minimum angle in degrees";
 
 	private final static String MIN_POLYGON_AREA = "minimum polygon area";
 
 	private final static String CHECK_MIN_POLYGON_AREA = "check minimum polygon area";
 
 	private final static String DISALLOW_POINTS = "check points";
 
 	private final static String DISALLOW_LINESTRINGS = "check linestrings";
 
 	private final static String DISALLOW_POLYGONS = "check polygons";
 
 	private final static String DISALLOW_MULTIPOINTS = "check multipoints";
 
 	private final static String DISALLOW_MULTILINESTRINGS = "check multilinestrings";
 
 	private final static String DISALLOW_MULTIPOLYGONS = "check multipolygons";
 
 	private final static String DISALLOW_GEOMETRYCOLLECTIONS = "check geometrycollections";
 
 	private static final String ERROR = "ERROR";
 
 	private static final String SOURCE_FID = "SOURCE_FID";
 
 	private static final String GEOMETRY = "GEOMETRY";
 
 	private MultiInputDialog dialog;
 
 	private FeatureSchema schema;
 
 	private GeometryFactory geometryFactory = new GeometryFactory();
 
 	private Validator validator;
 
 	private DataSource dataSourceToLocationFeatures;
 
 	private DataSource dataSourceToFeatures;
 
 	private boolean noErrors = true;
 
 	private IProgressMonitor pm;
 
 	public ValidateSelectedLayers() {
 		initFeatureSchema();
 	}
 
 	private void initFeatureSchema() {
 		schema = new FeatureSchema();
 		schema.addAttribute(ERROR, AttributeType.STRING);
 		schema.addAttribute(SOURCE_FID, AttributeType.INTEGER);
 		schema.addAttribute(GEOMETRY, AttributeType.GEOMETRY);
 	}
 
 	public void execute(MapContext mapContext, ILayer layer) {
 
 		if (dialog == null) {
 			initDialog();
 		}
 
 		dialog.setVisible(true);
 
 		validator = new Validator();
 		validator.setCheckingBasicTopology(dialog
 				.getBoolean(CHECK_BASIC_TOPOLOGY));
 		validator.setCheckingNoRepeatedConsecutivePoints(dialog
 				.getBoolean(CHECK_NO_REPEATED_CONSECUTIVE_POINTS));
 		validator.setCheckingLineStringsSimple(dialog
 				.getBoolean(CHECK_LINESTRINGS_SIMPLE));
 		validator.setCheckingPolygonOrientation(dialog
 				.getBoolean(CHECK_POLYGON_ORIENTATION));
 		validator.setCheckingNoHoles(dialog
 				.getBoolean(CHECK_POLYGONS_HAVE_NO_HOLES));
 		validator.setCheckingMinSegmentLength(dialog
 				.getBoolean(CHECK_MIN_SEGMENT_LENGTH));
 		validator.setCheckingMinAngle(dialog.getBoolean(CHECK_MIN_ANGLE));
 		validator.setCheckingMinPolygonArea(dialog
 				.getBoolean(CHECK_MIN_POLYGON_AREA));
 		validator.setMinSegmentLength(dialog.getDouble(MIN_SEGMENT_LENGTH));
 		validator.setMinAngle(dialog.getDouble(MIN_ANGLE));
 		validator.setMinPolygonArea(dialog.getDouble(MIN_POLYGON_AREA));
 
 		ArrayList disallowedGeometryClasses = new ArrayList();
 
 		if (dialog.getBoolean(DISALLOW_POINTS)) {
 			disallowedGeometryClasses.add(Point.class);
 		}
 
 		if (dialog.getBoolean(DISALLOW_LINESTRINGS)) {
 			disallowedGeometryClasses.add(LineString.class);
 		}
 
 		if (dialog.getBoolean(DISALLOW_POLYGONS)) {
 			disallowedGeometryClasses.add(Polygon.class);
 		}
 
 		if (dialog.getBoolean(DISALLOW_MULTIPOINTS)) {
 			disallowedGeometryClasses.add(MultiPoint.class);
 		}
 
 		if (dialog.getBoolean(DISALLOW_MULTILINESTRINGS)) {
 			disallowedGeometryClasses.add(MultiLineString.class);
 		}
 
 		if (dialog.getBoolean(DISALLOW_MULTIPOLYGONS)) {
 			disallowedGeometryClasses.add(MultiPolygon.class);
 		}
 
 		if (dialog.getBoolean(DISALLOW_GEOMETRYCOLLECTIONS)) {
 			disallowedGeometryClasses.add(GeometryCollection.class);
 		}
 
 		validator.setDisallowedGeometryClasses(disallowedGeometryClasses);
 
 		if (dialog.wasOKPressed()) {
 			BackgroundManager bm = (BackgroundManager) Services
 					.getService(BackgroundManager.class);
 			bm.backgroundOperation(new ExecuteValidateSelectedLayers(
 					mapContext, layer));
 
 		}
 
 	}
 
 	private class ExecuteValidateSelectedLayers implements BackgroundJob {
 
 		private ILayer layer;
 
 		private MapContext mapContext;
 
 		public ExecuteValidateSelectedLayers(MapContext mapContext, ILayer layer) {
 			this.mapContext = mapContext;
 			this.layer = layer;
 		}
 
 		public String getTaskName() {
 			return "Topological layer validation";
 		}
 
 		public void run(IProgressMonitor pm) {
 			validate(mapContext, layer, pm);
 
 		}
 	}
 
 	private void validate(MapContext mapContext, final ILayer layer,
 			IProgressMonitor pm) {
 
 		this.pm = pm;
 		DataSource ds = layer.getDataSource();
 		try {
 			SpatialDataSourceDecorator sds = new SpatialDataSourceDecorator(ds);
 			sds.open();
 			FeatureCollectionAdapter fc = new FeatureCollectionAdapter(sds);
 
 			List features = fc.getFeatures();
 			List validationErrors = validator.validate(features, pm);
 			sds.close();
 			if (!validationErrors.isEmpty()) {
 				noErrors = false;
 				DataManager dataManager = (DataManager) Services
 						.getService(DataManager.class);
 				DataSourceFactory dsf = dataManager.getDSF();
 
 				dataSourceToLocationFeatures = new FeatureCollectionDatasourceAdapter(
 						getFeatureCollection(toLocationFeatures(
 								validationErrors, layer)));
 
 				dataSourceToFeatures = new FeatureCollectionDatasourceAdapter(
 						getFeatureCollection(toFeatures(validationErrors, layer)));
 
 				ObjectMemoryDriver resultdriver = new ObjectMemoryDriver(
 						dataSourceToFeatures);
 				String resultlayer = dsf.getSourceManager().nameAndRegister(
 						resultdriver);
 				final ILayer rsLayer = dataManager.createLayer(resultlayer);
 				mapContext.getLayerModel().insertLayer(rsLayer, 0);
 
 			}
 
 			outputSummary(layer, validationErrors);
 		} catch (DriverException e) {
 			Services.getErrorManager().error(
 					"Cannot read the resulting datasource from the layer ", e);
 		} catch (LayerException e) {
 			Services.getErrorManager()
 					.error(
 							"Cannot insert resulting layer based on "
 									+ layer.getName(), e);
 		}
 
 	}
 
 	private void outputSummary(ILayer layer, List validationErrors) {
 
 		OutputManager om = (OutputManager) Services
 				.getService(OutputManager.class);
 		Color color = Color.black;
 		om.println("Report ----------------------------------", color);
 
 		color = Color.red;
 		om.println("Layer : " + layer.getName(), color);
 
 		if (validationErrors.isEmpty()) {
 
 			om.println("No validation errors ", color);
 			return;
 		}
 
 		CollectionMap descriptionToErrorMap = new CollectionMap();
 
 		for (Iterator i = validationErrors.iterator(); i.hasNext();) {
 			ValidationError error = (ValidationError) i.next();
 			descriptionToErrorMap.addItem(error.getMessage(), error);
 		}
 
 		for (Iterator i = descriptionToErrorMap.keySet().iterator(); i
 				.hasNext();) {
 			String message = (String) i.next();
 
 			color = Color.blue;
 			om.println(message + ":"
 					+ descriptionToErrorMap.getItems(message).size() + "", color);
 
 		}
 		color = Color.black;
 		om.println("----------------------------------", color);
		om.makeVisible();
 	}
 
 	public boolean isEmpty() {
 		return noErrors;
 
 	}
 
 	private List toFeatures(List validationErrors, ILayer sourceLayer) {
 
 		pm.startTask("Get geometries errors");
 		ArrayList features = new ArrayList();
 		int k = 0;
 		for (Iterator i = validationErrors.iterator(); i.hasNext();) {
 			pm.progressTo(k++);
 			ValidationError error = (ValidationError) i.next();
 			features.add(toFeature(error, sourceLayer, (Geometry) error
 					.getFeature().getGeometry().clone()));
 		}
 		pm.endTask();
 
 		return features;
 	}
 
 	private List toLocationFeatures(List validationErrors, ILayer sourceLayer) {
 		ArrayList features = new ArrayList();
 
 		for (Iterator i = validationErrors.iterator(); i.hasNext();) {
 			ValidationError error = (ValidationError) i.next();
 			Geometry geometry = geometryFactory
 					.createPoint(error.getLocation());
 			features.add(toFeature(error, sourceLayer, geometry));
 		}
 
 		return features;
 	}
 
 	private Feature toFeature(ValidationError error, ILayer sourceLayer,
 			Geometry geometry) {
 		Feature ringFeature = new BasicFeature(schema);
 		ringFeature.setAttribute(SOURCE_FID, new Integer(error.getFeature()
 				.getID()));
 		ringFeature.setAttribute(ERROR, error.getMessage());
 		ringFeature.setGeometry(geometry);
 
 		return ringFeature;
 	}
 
 	private FeatureCollection getFeatureCollection(List features) {
 
 		return new FeatureDataset(features, schema);
 
 	}
 
 	public DataSource getDataSourcetoLocationFeatures() {
 		return dataSourceToLocationFeatures;
 	}
 
 	public DataSource getDataSourcetoFeatures() {
 		return dataSourceToFeatures;
 	}
 
 	private void initDialog() {
 
 		CHECK_BASIC_TOPOLOGY = "check-basic-topology";
 		dialog = new MultiInputDialog(null, "Basic topological analysis", true);
 		dialog.setSideBarImage(IconLoader.getIcon("Validate.gif"));
 		dialog
 				.setSideBarDescription("Test layer against various topological criterias");
 		dialog.addLabel("<HTML><STRONG>" + "geometry metrics validation"
 				+ "</STRONG></HTML>");
 		dialog.addSeparator();
 		dialog.addCheckBox(CHECK_BASIC_TOPOLOGY, true);
 		dialog.addCheckBox(CHECK_NO_REPEATED_CONSECUTIVE_POINTS, false);
 		dialog
 				.addCheckBox(CHECK_POLYGON_ORIENTATION, false,
 						"check that polygon shells are oriented clockwise and holes counterclockwise");
 		dialog.addCheckBox(CHECK_MIN_SEGMENT_LENGTH, false);
 		dialog.addPositiveDoubleField(MIN_SEGMENT_LENGTH, 0.001, 5);
 		dialog.addCheckBox(CHECK_MIN_ANGLE, false);
 		dialog.addPositiveDoubleField(MIN_ANGLE, 1, 5);
 		dialog.addCheckBox(CHECK_MIN_POLYGON_AREA, false);
 		dialog.addPositiveDoubleField(MIN_POLYGON_AREA, 0.001, 5);
 		dialog.addCheckBox(CHECK_LINESTRINGS_SIMPLE, false,
 				"check that linestrings are simple");
 		dialog.startNewColumn();
 		dialog.addLabel("<HTML><STRONG>" + "geometry types validation"
 				+ "</STRONG></HTML>");
 		dialog.addSeparator();
 		dialog.addCheckBox(DISALLOW_POINTS, false);
 		dialog.addCheckBox(DISALLOW_LINESTRINGS, false);
 		dialog.addCheckBox(DISALLOW_POLYGONS, false);
 		dialog.addCheckBox(DISALLOW_MULTIPOINTS, false);
 		dialog.addCheckBox(DISALLOW_MULTILINESTRINGS, false);
 		dialog.addCheckBox(DISALLOW_MULTIPOLYGONS, false);
 		dialog.addCheckBox(CHECK_POLYGONS_HAVE_NO_HOLES, false);
 		dialog.addCheckBox(DISALLOW_GEOMETRYCOLLECTIONS, false,
 				"geometry collection subtypes are not disallowed");
 
 	}
 
 }
