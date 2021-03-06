 /*
  * HUMBOLDT: A Framework for Data Harmonisation and Service Integration.
  * EU Integrated Project #030962                 01.10.2006 - 30.09.2010
  * 
  * For more information on the project, please refer to the this web site:
  * http://www.esdi-humboldt.eu
  * 
  * LICENSE: For information on the license under which this program is 
  * available, please refer to http:/www.esdi-humboldt.eu/license.html#core
  * (c) the HUMBOLDT Consortium, 2007 to 2010.
  */
 
 package eu.esdihumboldt.cst.corefunctions;
 
 
 import java.util.Map;
 import org.geotools.feature.FeatureCollection;
 import org.geotools.feature.PropertyImpl;
 import org.opengis.feature.Feature;
 import org.opengis.feature.simple.SimpleFeature;
 import org.opengis.feature.type.FeatureType;
 import org.opengis.feature.type.PropertyDescriptor;
 import eu.esdihumboldt.cst.align.ICell;
 import eu.esdihumboldt.cst.align.ext.IParameter;
 import eu.esdihumboldt.cst.transformer.AbstractCstFunction;
 import eu.esdihumboldt.goml.align.Cell;
 import eu.esdihumboldt.goml.omwg.Property;
 import eu.esdihumboldt.goml.rdf.About;
 
 /**
  * CST Function to set default
  * attribute target values.
  *
  * @author Ulrich Schaeffler, Anna Pitaev
  * @partner 14 / TUM, 04 / Logica
  * @version $Id$ 
  */
 public class ConstantValueFunction extends AbstractCstFunction {
 	
 	public static final String DEFAULT_VALUE_PARAMETER_NAME = "defaultValue";
 	private Object defaultValue = null;
 	private Property targetProperty = null;
 
 	/**
 	 * @see eu.esdihumboldt.cst.transformer.AbstractCstFunction#setParametersTypes(java.util.Map)
 	 */
 	@Override
 	protected void setParametersTypes(Map<String, Class<?>> parametersTypes) {
 		parameterTypes.put(ConstantValueFunction.DEFAULT_VALUE_PARAMETER_NAME, Object.class);
 
 	}
 
 	/**
 	 * @see eu.esdihumboldt.cst.transformer.CstFunction#configure(eu.esdihumboldt.cst.align.ICell)
 	 */
 	public boolean configure(ICell cell) {
 		for (IParameter ip : cell.getEntity2().getTransformation().getParameters()) {
 			if (ip.getName().equals(ConstantValueFunction.DEFAULT_VALUE_PARAMETER_NAME)) {
 				this.defaultValue = ip.getValue();
 			}
 		}
 		this.targetProperty = (Property) cell.getEntity2();
 		return true;
 	}
 
 	/**
 	 * @see eu.esdihumboldt.cst.transformer.CstFunction#transform(org.geotools.feature.FeatureCollection)
 	 */
 	public FeatureCollection<? extends FeatureType, ? extends Feature> transform(
 			FeatureCollection<? extends FeatureType, ? extends Feature> fc) {
 		// TODO Auto-generated method stub
 		return null;
 	}
 
 	/**
 	 * @see eu.esdihumboldt.cst.transformer.CstFunction#transform(org.opengis.feature.Feature, org.opengis.feature.Feature)
 	 */
 	public Feature transform(Feature source, Feature target) {
 		PropertyDescriptor pd = target.getProperty(
 				this.targetProperty.getLocalname()).getDescriptor();
 		
 		
 		PropertyImpl p = null;
 		if (pd.getType().getBinding().isPrimitive()) {
 			
 			if (pd.getType().getBinding().equals(Integer.class)){
 				((SimpleFeature)target).setAttribute(this.targetProperty.getLocalname(),(Integer)this.defaultValue);
 			}
 			else if (pd.getType().getBinding().equals(Short.class)){
 				((SimpleFeature)target).setAttribute(this.targetProperty.getLocalname(),(Short)this.defaultValue);
 			}
 			else if (pd.getType().getBinding().equals(Double.class)){
 				((SimpleFeature)target).setAttribute(this.targetProperty.getLocalname(),(Double)this.defaultValue);
 			}
 			else if (pd.getType().getBinding().equals(Long.class)){
 				((SimpleFeature)target).setAttribute(this.targetProperty.getLocalname(),(Long)this.defaultValue);
 			}
 			else if (pd.getType().getBinding().equals(Float.class)){
 				((SimpleFeature)target).setAttribute(this.targetProperty.getLocalname(),(Float)this.defaultValue);
 			}
 			else if (pd.getType().getBinding().equals(Boolean.class)){
 				((SimpleFeature)target).setAttribute(this.targetProperty.getLocalname(),(Boolean)this.defaultValue);
 			}
 			else if (pd.getType().getBinding().equals(Byte.class)){
 				((SimpleFeature)target).setAttribute(this.targetProperty.getLocalname(),(Byte)this.defaultValue);
 			}
 			else {
 				((SimpleFeature)target).setAttribute(this.targetProperty.getLocalname(),(Character)this.defaultValue);
 			}
 
 		}
 		else if (pd.getType().getBinding().equals(String.class)){
 			
 			((SimpleFeature)target).setAttribute(this.targetProperty.getLocalname(),this.defaultValue.toString());
 
 		}
 		
 		return target;
 	}
 
	public Cell getParameters() {
		Cell parameterCell = new Cell();
		Property entity1 = new Property(new About(""));
		Property entity2 = new Property(new About(""));
	
		parameterCell.setEntity1(entity1);
		parameterCell.setEntity2(entity2);
		return parameterCell;
	}
 }
