 package au.gov.ga.worldwind.animator.animation.parameter;
 
 
 /**
  * A basic implementation of the {@link BezierParameterValue} interface.
  * 
  * @author Michael de Hoog (michael.deHoog@ga.gov.au)
  * @author James Navin (james.navin@ga.gov.au)
  */
 public class BasicBezierParameterValue extends BasicParameterValue implements BezierParameterValue
 {
 	private static final long serialVersionUID = 20100819L;
 
 	/** The default control point percentage to use */
 	private static final double DEFAULT_CONTROL_POINT_PERCENTAGE = 0.4;
 	
 	/** 
 	 * Whether or not this parameter is locked.
 	 * 
 	 * @see #isLocked()
 	 */
 	private boolean locked;
 	
 	/** The '<code>in</code>' control point value */
 	private BezierContolPoint in = new BezierContolPoint();
 	
 	/** The '<code>out</code>' control point value */
 	private BezierContolPoint out = new BezierContolPoint();
 	
 	/**
 	 * Construct a new bezier parameter value.
 	 * <p/>
 	 * Uses the {@link #smooth()} method to infer values for <code>in</code> and <code>out</code>.
 	 * <p/>
 	 * The bezier value will be locked.
 	 * 
 	 * @param value The value to store on this {@link ParameterValue}
 	 * @param frame The frame at which this value applies
 	 * @param owner The {@link Parameter} that 'owns' this parameter value
 	 */
 	public BasicBezierParameterValue(double value, int frame, Parameter owner)
 	{
 		super(value, frame, owner);
		this.in.setValue(value);
		this.in.setPercent(DEFAULT_CONTROL_POINT_PERCENTAGE);
		this.out.setValue(value);
		this.out.setPercent(DEFAULT_CONTROL_POINT_PERCENTAGE);
		this.locked = true;
 	}
 	
 	/**
 	 * Construct a new bezier parameter value.
 	 * <p/>
 	 * Allows <code>in</code> and <code>out</code> values to be specified.
 	 * <p/>
 	 * The resuling bezier value will be unlocked.
 	 * 
 	 * @param value The value to store on this {@link ParameterValue}
 	 * @param frame The frame at which this value applies
 	 * @param owner The {@link Parameter} that 'owns' this parameter value
 	 * @param inValue The value to use for the <code>in</code> control point
 	 * @param inPercent The relative time percentage to use for the <code>in</code> control point
 	 * @param outValue The value to use for the <code>out</code> control point
 	 * @param outPercent The relative time percentage to use for the <code>out</code> control point
 	 * 
 	 */
 	public BasicBezierParameterValue(double value, int frame, Parameter owner, double inValue, double inPercent, double outValue, double outPercent)
 	{
 		super(value, frame, owner);
 		this.locked = false;
 		this.in.setValue(inValue);
 		this.in.setPercent(inPercent);
 		this.out.setValue(outValue);
 		this.out.setPercent(outPercent);
 	}
 	
 	/**
 	 * Construct a new bezier parameter value.
 	 * <p/>
 	 * Allows <code>in</code> value to be specified, with the <code>out</code> value locked to the <code>in</code> value
 	 * <p/>
 	 * The resuling bezier value will be locked.
 	 * 
 	 * @param value The value to store on this {@link ParameterValue}
 	 * @param frame The frame at which this value applies
 	 * @param owner The {@link Parameter} that 'owns' this parameter value
 	 * @param inValue The value to use for the <code>in</code> control point
 	 * @param inPercent The relative time percentage to use for the <code>in</code> control point
 	 * 
 	 */
 	public BasicBezierParameterValue(double value, int frame, Parameter owner, double inValue, double inPercent)
 	{
 		super(value, frame, owner);
 		this.locked = true;
 		this.in.setValue(inValue);
 		this.in.setPercent(inPercent);
 		lockOut();
 	}
 	
 	
 	@Override
 	public ParameterValueType getType()
 	{
 		return ParameterValueType.BEZIER;
 	}
 	
 	@Override
 	public void setInValue(double value)
 	{
 		this.in.setValue(value);
 		if (isLocked() && in.hasValue()) 
 		{
 			lockOut();
 		}
 	}
 
 	@Override
 	public double getInValue()
 	{
 		return in.getValue();
 	}
 	
 	@Override
 	public void setInPercent(double percent)
 	{
 		this.in.setPercent(percent);
 	}
 	
 	@Override
 	public double getInPercent()
 	{
 		return this.in.getPercent();
 	}
 
 	@Override
 	public void setOutValue(double value)
 	{
 		this.out.setValue(value);
 		if (isLocked() && out.hasValue())
 		{
 			lockIn();
 		}
 	}
 
 	@Override
 	public double getOutValue()
 	{
 		return out.getValue();
 	}
 
 	@Override
 	public void setOutPercent(double percent)
 	{
 		this.out.setPercent(percent);
 	}
 	
 	@Override
 	public double getOutPercent()
 	{
 		return this.out.getPercent();
 	}
 	
 	@Override
 	public boolean isLocked()
 	{
 		return locked;
 	}
 
 	@Override
 	public void setLocked(boolean locked)
 	{
 		this.locked = locked;
 	}
 
 	/**
 	 * Lock the '<code>in</code>' value to the '<code>out</code>' value.
 	 * <p/>
 	 * This will:
 	 * <ul>
 	 * 	<li>Adjust <code>in</code> so that <code>in</code>, <code>value</code> and <code>out</code> are colinear
 	 *  <li>Lock the <code>in</code> percentage to the same as the <code>out</code> percentage
 	 * </ul>
 	 */
 	private void lockIn()
 	{
 		if (!out.hasValue()) 
 		{
 			return;
 		}
 		
 		double outValueDelta = out.getValue() - getValue();
 		in.setValue(getValue() - outValueDelta);
 		in.setPercent(out.getPercent());
 	}
 	
 	/**
 	 * Lock the '<code>out</code>' value to the '<code>in</code>' value.
 	 * <p/>
 	 * This will:
 	 * <ul>
 	 * 	<li>Adjust <code>out</code> so that <code>in</code>, <code>value</code> and <code>out</code> are colinear
 	 *  <li>Lock the <code>out</code> percentage to the same as the <code>in</code> percentage
 	 * </ul>
 	 */
 	private void lockOut()
 	{
 		if (!in.hasValue()) 
 		{
 			return;
 		}
 		double inValueDelta = in.getValue() - getValue();
 		out.setValue(getValue() - inValueDelta);
 		out.setPercent(in.getPercent());
 	}
 	
 	/**
 	 * Use a 3-point window to 'smooth' the curve at this point.
 	 * <p/>
 	 * This examines the 'previous' and 'next' points, and sets the control points for this value
 	 * such that a smooth transition is obtained.
 	 * <p/>
 	 * If there are no 'previous' or 'next' points, this method will reset the control points to their default values.
 	 */
 	@Override
 	public void smooth()
 	{
 		// Default 'in' and 'out' value to the same as the 'value'. This will result in a horizontal 'in-value-out' control line
 		double inValue = getValue();
 		double outValue = getValue();
 		
 		// If previous and next points exist, and they exist on different sides of 'value' vertically,
 		// use them to choose a better value for 'in' based on the line joining 'previous' and 'next'
 		ParameterValue previousValue = getOwner().getValueAtKeyFrameBeforeFrame(getFrame());
 		ParameterValue nextValue = getOwner().getValueAtKeyFrameAfterFrame(getFrame());
 		if (previousValue != null && nextValue != null && 
 				Math.signum(getValue() - previousValue.getValue()) != Math.signum(getValue() - nextValue.getValue()))
 		{
 			// Compute the gradient of the line joining the previous and next points
 			double m = (nextValue.getValue() - previousValue.getValue()) / (nextValue.getFrame() - previousValue.getFrame());
 			double xIn = (getFrame() - previousValue.getFrame()) * getInPercent();
 			double xOut = (nextValue.getFrame() - getFrame()) * getOutPercent();
 			
 			// Compute the value to use for the in control point such that it lies on the line joining the previous and next lines
 			inValue = getValue() - m * xIn;
 			outValue = getValue() + m * xOut;
 		}
 		
 		// Set the values (bypass the locking as we have calculated what we need here)
 		boolean wasLocked = isLocked();
 		setLocked(false);
 		
 		setInValue(inValue);
 		setOutValue(outValue);
 		
 		setLocked(wasLocked);
 	}
 	
 	/**
 	 * A simple container class that holds a value and time percent
 	 */
 	private static class BezierContolPoint
 	{
 		private Double value;
 		private double percent = DEFAULT_CONTROL_POINT_PERCENTAGE;
 		
 		private boolean hasValue()
 		{
 			return value != null;
 		}
 		
 		public void setValue(Double value)
 		{
 			this.value = value;
 		}
 		
 		public Double getValue()
 		{
 			return value;
 		}
 		
 		public void setPercent(double percent)
 		{
 			this.percent = percent;
 		}
 		
 		public double getPercent()
 		{
 			return percent;
 		}
 	}
 
 }
