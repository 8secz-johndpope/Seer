 package org.gitools.model.decorator.impl;
 
 import java.awt.Color;
 import java.util.HashMap;
 import java.util.Map;
 
 import javax.xml.bind.annotation.XmlAccessType;
 import javax.xml.bind.annotation.XmlAccessorType;
 import javax.xml.bind.annotation.XmlTransient;
 
 import org.gitools.matrix.MatrixUtils;
 import org.gitools.model.decorator.ElementDecoration;
 import org.gitools.model.decorator.ElementDecorator;
 import org.gitools.matrix.model.element.IElementAdapter;
 
 import edu.upf.bg.formatter.GenericFormatter;
 import edu.upf.bg.colorscale.IColorScale;
 import edu.upf.bg.colorscale.impl.LinearTwoSidedColorScale;
 import edu.upf.bg.colorscale.util.ColorConstants;
 import javax.xml.bind.annotation.XmlRootElement;
 
 @XmlRootElement
 @XmlAccessorType(XmlAccessType.FIELD)
 public class LinearTwoSidedElementDecorator extends ElementDecorator {
 
 	private static final long serialVersionUID = -181427286948958314L;
 
 	private int valueIndex;
 	
 	private LinearTwoSidedColorScale scale;
 
 	@XmlTransient
 	private GenericFormatter fmt;
 	
 	
 	public LinearTwoSidedElementDecorator() {
 		
 		valueIndex = getPropertyIndex(new String[] {
 				"value", "log2ratio" });
 		
 		scale = new LinearTwoSidedColorScale();
 
 		fmt = new GenericFormatter("<");
 	}
 
 	public LinearTwoSidedElementDecorator(IElementAdapter adapter) {
 		super(adapter);
 		
 		valueIndex = getPropertyIndex(new String[] {
 				"value", "log2ratio" });
 		
 		scale = new LinearTwoSidedColorScale();
 
 		fmt = new GenericFormatter("<");
 	}
 
 	/*@Override
 	public Object clone() {
 		LinearTwoSidedElementDecorator obj = null;
 		try {
 			obj = (LinearTwoSidedElementDecorator) super.clone();
 			obj.scale = scale.clone();
 			obj.fmt = new GenericFormatter("<");
 		}
 		catch (CloneNotSupportedException ex) { }
 		return obj;
 	}*/
 
 	public final int getValueIndex() {
 		return valueIndex;
 	}
 
 	public final void setValueIndex(int valueIndex) {
 		int old = this.valueIndex;
 		this.valueIndex = valueIndex;
 		firePropertyChange(PROPERTY_CHANGED, old, valueIndex);
 	}
 
 	public final double getMinValue() {
 		return scale.getMin().getValue();
 	}
 
 	public final void setMinValue(double minValue) {
 		double old = scale.getMin().getValue();
 		scale.getMin().setValue(minValue);
 		firePropertyChange(PROPERTY_CHANGED, old, minValue);
 	}
 
 	public final double getMidValue() {
 		return scale.getMid().getValue();
 	}
 
 	public final void setMidValue(double midValue) {
 		double old = scale.getMid().getValue();
 		scale.getMid().setValue(midValue);
 		firePropertyChange(PROPERTY_CHANGED, old, midValue);
 	}
 
 	public final double getMaxValue() {
 		return scale.getMax().getValue();
 	}
 
 	public final void setMaxValue(double maxValue) {
 		double old = scale.getMax().getValue();
 		scale.getMax().setValue(maxValue);
 		firePropertyChange(PROPERTY_CHANGED, old, maxValue);
 	}
 
 	public final Color getMinColor() {
 		return scale.getMin().getColor();
 	}
 
 	public final void setMinColor(Color minColor) {
 		Color old = scale.getMin().getColor();
 		scale.getMin().setColor(minColor);
 		firePropertyChange(PROPERTY_CHANGED, old, minColor);
 	}
 
 	public final Color getMidColor() {
 		return scale.getMid().getColor();
 	}
 
 	public final void setMidColor(Color midColor) {
 		Color old = scale.getMid().getColor();
 		scale.getMid().setColor(midColor);
 		firePropertyChange(PROPERTY_CHANGED, old, midColor);
 	}
 
 	public final Color getMaxColor() {
 		return scale.getMax().getColor();
 	}
 
 	public final void setMaxColor(Color maxColor) {
 		Color old = scale.getMax().getColor();
 		scale.getMax().setColor(maxColor);
 		firePropertyChange(PROPERTY_CHANGED, old, maxColor);
 	}
 
 	public Color getEmptyColor() {
 		return scale.getEmptyColor();
 	}
 	
 	public void setEmptyColor(Color color) {
 		Color old = scale.getEmptyColor();
 		scale.setEmptyColor(color);
 		firePropertyChange(PROPERTY_CHANGED, old, color);
 	}
 
 	@Override
 	public void decorate(ElementDecoration decoration, Object element) {
 		decoration.reset();
 		
 		if (element == null) {
			decoration.setBgColor(ColorConstants.emptyColor);
 			decoration.setToolTip("Empty cell");
 			return;
 		}
 		
 		Object value = adapter.getValue(element, valueIndex);
 		
 		double v = MatrixUtils.doubleValue(value);
 
 		final Color color = scale.valueColor(v);
 		
 		decoration.setBgColor(color);
 		decoration.setToolTip(fmt.pvalue(v));
 	}
 
 	@Override
 	public IColorScale getScale() {
 		return scale;
 	}
 
 	/*@Deprecated
 	@Override
 	public Map<String, String> getConfiguration() {
 		
 		Map<String, String> configuration = new HashMap <String, String>();
 		configuration.put("valueIndex;", Integer.toString(valueIndex));
 		return configuration;
 	}
 
 	@Deprecated
 	@Override
 	public void setConfiguration(Map<String, String> configuration) {
 		this.valueIndex = Integer.parseInt((String) configuration.get("valueIndex"));	
 	}*/
 }
