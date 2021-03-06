 package dk.itu.big_red.model;
 
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Map;
 
 import org.eclipse.draw2d.geometry.Dimension;
 import org.eclipse.draw2d.geometry.PointList;
 
 import dk.itu.big_red.model.changes.Change;
 import dk.itu.big_red.model.changes.ChangeGroup;
 import dk.itu.big_red.model.interfaces.IControl;
 import dk.itu.big_red.model.names.INamePolicy;
 
 /**
  * A Control is the bigraphical analogue of a <i>class</i> - a template from
  * which instances ({@link Node}s) should be constructed. Controls are
  * registered with a {@link Bigraph} as part of its {@link Signature}.
  * 
  * <p>In the formal bigraph model, controls define labels and numbered ports;
  * this model differs slightly by defining <i>named</i> ports and certain
  * graphical properties (chiefly shapes and default port offsets).
  * @author alec
  * @see IControl
  */
 public class Control extends Colourable implements IControl {
 	abstract class ControlChange extends ModelObjectChange {
 		@Override
 		public Control getCreator() {
 			return Control.this;
 		}
 	}
 	
 	public class ChangeName extends ControlChange {
 		public String name;
 		
 		public ChangeName(String name) {
 			this.name = name;
 		}
 		
 		private String oldName;
 		@Override
 		public void beforeApply() {
 			oldName = getCreator().getName();
 		}
 		
 		@Override
 		public boolean canInvert() {
 			return (oldName != null);
 		}
 		
 		@Override
 		public ChangeName inverse() {
 			return new ChangeName(oldName);
 		}
 		
 		@Override
 		public boolean isReady() {
 			return (name != null);
 		}
 		
 		@Override
 		public String toString() {
 			return "Change(set name of " + getCreator() + " to " + name + ")";
 		}
 	}
 	
 	public class ChangeShape extends ControlChange {
 		public Shape shape;
 		
 		public ChangeShape(Shape shape) {
 			this.shape = shape;
 		}
 		
 		private Shape oldShape;
 		@Override
 		public void beforeApply() {
 			oldShape = getCreator().getShape();
 		}
 		
 		@Override
 		public ChangeShape inverse() {
 			return new ChangeShape(oldShape);
 		}
 		
 		@Override
 		public boolean isReady() {
 			return (shape != null);
 		}
 		
 		@Override
 		public String toString() {
 			return "Change(set shape of " + getCreator() + " to " +
 					shape.toString() + ")";
 		}
 	}
 	
 	public class ChangeLabel extends ControlChange {
 		public String label;
 		
 		public ChangeLabel(String label) {
 			this.label = label;
 		}
 		
 		private String oldLabel;
 		@Override
 		public void beforeApply() {
 			oldLabel = getCreator().getLabel();
 		}
 		
 		@Override
 		public ChangeLabel inverse() {
 			return new ChangeLabel(oldLabel);
 		}
 		
 		@Override
 		public boolean canInvert() {
 			return (oldLabel != null);
 		}
 		
 		@Override
 		public boolean isReady() {
 			return (label != null);
 		}
 		
 		@Override
 		public String toString() {
 			return "Change(set label of " + getCreator() + " to " + label + ")";
 		}
 	}
 	
 	public class ChangeDefaultSize extends ControlChange {
 		public Dimension defaultSize;
 		public ChangeDefaultSize(Dimension defaultSize) {
 			this.defaultSize = defaultSize;
 		}
 		
 		private Dimension oldDefaultSize;
 		@Override
 		public void beforeApply() {
 			oldDefaultSize = getCreator().getDefaultSize();
 		}
 		
 		@Override
 		public boolean canInvert() {
 			return (oldDefaultSize != null);
 		}
 		
 		@Override
 		public ChangeDefaultSize inverse() {
 			return new ChangeDefaultSize(oldDefaultSize);
 		}
 		
 		@Override
 		public boolean isReady() {
 			return (defaultSize != null);
 		}
 		
 		@Override
 		public String toString() {
 			return "Change(set default size of " + getCreator() + " to " +
 					defaultSize + ")";
 		}
 	}
 	
 	public class ChangeResizable extends ControlChange {
 		public boolean resizable;
 		public ChangeResizable(boolean resizable) {
 			this.resizable = resizable;
 		}
 		
 		private Boolean oldResizable;
 		@Override
 		public void beforeApply() {
 			oldResizable = getCreator().isResizable();
 		}
 		
 		@Override
 		public boolean canInvert() {
 			return (oldResizable != null);
 		}
 		
 		@Override
 		public ChangeResizable inverse() {
 			return new ChangeResizable(oldResizable);
 		}
 		
 		@Override
 		public String toString() {
 			return "Change(set resizability of " + getCreator() + " to " +
 					resizable + ")";
 		}
 	}
 	
 	public class ChangeKind extends ControlChange {
 		public Kind kind;
 		public ChangeKind(Kind kind) {
 			this.kind = kind;
 		}
 		
 		private Kind oldKind;
 		@Override
 		public void beforeApply() {
 			oldKind = getCreator().getKind();
 		}
 		
 		@Override
 		public boolean canInvert() {
 			return (oldKind != null);
 		}
 		
 		@Override
 		public boolean isReady() {
 			return (kind != null);
 		}
 		
 		@Override
 		public ChangeKind inverse() {
 			return new ChangeKind(oldKind);
 		}
 		
 		@Override
 		public String toString() {
 			return "Change(set kind of " + getCreator() + " to " + kind + ")";
 		}
 	}
 	
 	private abstract class PortChange extends ControlChange {
 		public PortSpec port;
 		
 		@Override
 		public boolean isReady() {
 			return (port != null);
 		}
 	}
 	
 	public class ChangeAddPort extends PortChange {
 		public ChangeAddPort(PortSpec port) {
 			this.port = port;
 		}
 		
 		@Override
 		public ChangeRemovePort inverse() {
 			return new ChangeRemovePort(port);
 		}
 		
 		@Override
 		public String toString() {
 			return "Change(add port " + port + " to " + getCreator() + ")";
 		}
 	}
 	
 	public class ChangeRemovePort extends PortChange {
 		public ChangeRemovePort(PortSpec port) {
 			this.port = port;
 		}
 		
 		@Override
 		public ChangeAddPort inverse() {
 			return new ChangeAddPort(port);
 		}
 		
 		@Override
 		public String toString() {
 			return "Change(remove port " + port + " from " + getCreator() + ")";
 		}
 	}
 	
 	public class ChangePoints extends ControlChange {
 		public PointList points;
 		public ChangePoints(PointList points) {
 			this.points = points;
 		}
 		
 		private PointList oldPoints;
 		@Override
 		public void beforeApply() {
 			oldPoints = getCreator().getPoints();
 		}
 		
 		@Override
 		public boolean canInvert() {
 			return (oldPoints != null);
 		}
 		
 		@Override
 		public boolean isReady() {
 			return (points != null);
 		}
 		
 		@Override
 		public Change inverse() {
 			return new ChangePoints(oldPoints);
 		}
 	}
 	
 	public static enum Shape {
 		/**
 		 * An oval.
 		 */
 		OVAL,
 		/**
 		 * A polygon.
 		 */
 		POLYGON
 	}
 
 	public static enum Kind {
 		ATOMIC {
 			@Override
 			public String toString() {
 				return "atomic";
 			}
 		},
 		ACTIVE {
 			@Override
 			public String toString() {
 				return "active";
 			}
 		},
 		PASSIVE {
 			@Override
 			public String toString() {
 				return "passive";
 			}
 		};
 	}
 	
 	/**
 	 * The property name fired when the label (the one- or two-character
 	 * caption that appears next to {@link Node}s on the bigraph) changes.
 	 * The property values are {@link String}s.
 	 */
 	public static final String PROPERTY_LABEL = "ControlLabel";
 	
 	/**
 	 * The property name fired when the name changes. The property values are
 	 * {@link String}s.
 	 */
 	public static final String PROPERTY_NAME = "ControlName";
 	
 	/**
 	 * The property name fired when the shape changes. The property values are
 	 * {@link Control.Shape}s.
 	 */
 	public static final String PROPERTY_SHAPE = "ControlShape";
 	
 	/**
 	 * The property name fired when the set of points defining this control's
 	 * polygon changes. The property values are {@link PointList}s.
 	 */
 	public static final String PROPERTY_POINTS = "ControlPoints";
 	
 	/**
 	 * The property name fired when the default size changes. (This only
 	 * really matters for existing {@link Node}s if they aren't resizable.)
 	 * The property values are {@link Dimension}s.
 	 */
 	public static final String PROPERTY_DEFAULT_SIZE = "ControlDefaultSize";
 	
 	/**
 	 * The property name fired when the resizability changes. If this changes
 	 * from <code>true</code> to <code>false</code>, listeners should make sure
 	 * that any {@link Node}s with this Control are resized to the default
 	 * size. The property values are {@link Boolean}s.
 	 * @see Control#getDefaultSize
 	 */
 	public static final String PROPERTY_RESIZABLE = "ControlResizable";
 	
 	/**
 	 * The property name fired when the set of ports changes. If this changes
 	 * from <code>null</code> to a non-null value, then a port has been added;
 	 * if it changes from a non-null value to <code>null</code>, one has been
 	 * removed. The property values are {@link PortSpec}s.
 	 */
 	public static final String PROPERTY_PORT = "ControlPort";
 	
 	/**
 	 * The property name fired when the kind changes. The property values are
 	 * {@link Kind}s.
 	 */
 	public static final String PROPERTY_KIND = "ControlKind";
 	
 	public static final PointList POINTS_QUAD = new PointList(new int[] {
 			0, 0,
 			0, 40,
 			-40, 40,
 			-40, 0
 	});
 	
 	private ArrayList<PortSpec> ports = new ArrayList<PortSpec>();
 	private PointList points = new PointList();
 	
 	private Control.Shape shape;
 	private String name;
 	private String label;
 	private Dimension defaultSize;
 	private boolean resizable;
 	private Control.Kind kind;
 	
 	public Control() {
 		setName("Unknown");
 		setLabel("?");
 		setShape(Control.Shape.POLYGON);
 		setPoints(POINTS_QUAD);
 		setDefaultSize(new Dimension(50, 50));
 		setKind(Kind.ACTIVE);
 		setResizable(true);
 	}
 
 	@Override
 	public Control clone(Map<ModelObject,ModelObject> m) {
 		Control c = (Control)super.clone(m);
 		
 		c.setName(getName());
 		c.setLabel(getLabel());
 		c.setShape(getShape());
 		if (getShape() == Shape.POLYGON)
 			c.setPoints(getPoints().getCopy());
 		c.setDefaultSize(getDefaultSize().getCopy());
 		c.setKind(getKind());
 		c.setResizable(isResizable());
 		
 		for (PortSpec p : getPorts())
 			c.addPort(new PortSpec(p));
 		
 		return c;
 	}
 	
 	public String getLabel() {
 		return label;
 	}
 	
 	protected void setLabel(String label) {
 		String oldLabel = this.label;
 		this.label = label;
 		firePropertyChange(PROPERTY_LABEL, oldLabel, label);
 	}
 	
 	public Control.Shape getShape() {
 		return shape;
 	}
 	
 	protected void setShape(Control.Shape shape) {
 		Control.Shape oldShape = this.shape;
 		this.shape = shape;
 		firePropertyChange(PROPERTY_SHAPE, oldShape, shape);
 	}
 	
 	/**
 	 * Returns a copy of the list of points defining this Control's polygon.
 	 * @return a list of points defining a polygon
 	 * @see Control#getShape
 	 * @see Control#setShape
 	 */
 	public PointList getPoints() {
 		return points.getCopy();
 	}
 	
 	protected void setPoints(PointList points) {
 		if (points != null) {
 			PointList oldPoints = this.points;
 			this.points = points;
 			firePropertyChange(PROPERTY_POINTS, oldPoints, points);
 		}
 	}
 
 	protected void setName(String name) {
 		if (name != null) {
 			String oldName = this.name;
 			this.name = name;
 			firePropertyChange(PROPERTY_NAME, oldName, name);
 		}
 	}
 
 	@Override
 	public String getName() {
 		return name;
 	}
 	
 	public Dimension getDefaultSize() {
 		return defaultSize;
 	}
 	
 	protected void setDefaultSize(Dimension defaultSize) {
 		if (defaultSize != null) {
 			Dimension oldSize = this.defaultSize;
 			this.defaultSize = defaultSize;
 			firePropertyChange(PROPERTY_DEFAULT_SIZE, oldSize, defaultSize);
 		}
 	}
 	
 	public Kind getKind() {
 		return kind;
 	}
 	
 	protected void setKind(Kind kind) {
 		Kind oldKind = this.kind;
 		this.kind = kind;
 		firePropertyChange(PROPERTY_KIND, oldKind, kind);
 	}
 	
 	public boolean isResizable() {
 		return resizable;
 	}
 	
 	protected void setResizable(Boolean resizable) {
 		Boolean oldResizable = this.resizable;
 		this.resizable = resizable;
 		firePropertyChange(PROPERTY_RESIZABLE, oldResizable, resizable);
 	}
 	
 	protected void addPort(PortSpec p) {
 		if (p != null) {
 			PortSpec q = new PortSpec(p);
 			ports.add(q);
 			firePropertyChange(PROPERTY_PORT, null, q);
 		}
 	}
 	
 	protected void removePort(String port) {
 		PortSpec p = getPort(port);
 		if (p != null) {
 			ports.remove(p);
 			firePropertyChange(PROPERTY_PORT, p, null);
 		}
 	}
 	
 	public boolean hasPort(String port) {
 		return (getPort(port) != null);
 	}
 	
 	/**
 	 * Gets the {@link List} of this {@link Control}'s {@link PortSpec}s.
 	 * @return a reference to the internal list; modify it and <s>vampire
 	 * bats</s> undocumented behaviour will steal your blood while you sleep
 	 * @see #createPorts()
 	 */
 	@Override
 	public List<PortSpec> getPorts() {
 		return ports;
 	}
 	
 	/**
 	 * Produces a <i>new</i> array of {@link Port}s to give to a {@link Node}.
 	 * @return an array of Ports
 	 * @see #getPorts()
 	 */
 	public ArrayList<Port> createPorts() {
 		ArrayList<Port> r = new ArrayList<Port>();
 		for (PortSpec i : ports)
 			r.add(new Port(i));
 		return r;
 	}
 	
 	public PortSpec getPort(String name) {
 		for (PortSpec i : ports)
 			if (i.getName().equals(name))
 				return i;
 		return null;
 	}
 	
 	private INamePolicy parameterPolicy;
 	
 	public INamePolicy getParameterPolicy() {
 		return parameterPolicy;
 	}
 	
 	public void setParameterPolicy(INamePolicy parameterPolicy) {
 		this.parameterPolicy = parameterPolicy;
 	}
 	
 	/**
 	 * {@inheritDoc}
 	 * <p><strong>Special notes for {@link Control}:</strong>
 	 * <ul>
 	 * <li>Passing {@link #PROPERTY_PORT} will return a {@link List}&lt;{@link
 	 * PortSpec}&gt;, <strong>not</strong> a {@link PortSpec}.
 	 * </ul>
 	 */
 	@Override
 	public Object getProperty(String name) {
 		if (PROPERTY_DEFAULT_SIZE.equals(name)) {
 			return getDefaultSize();
 		} else if (PROPERTY_LABEL.equals(name)) {
 			return getLabel();
 		} else if (PROPERTY_NAME.equals(name)) {
 			return getName();
 		} else if (PROPERTY_POINTS.equals(name)) {
 			return getPoints();
 		} else if (PROPERTY_PORT.equals(name)) {
 			return getPorts();
 		} else if (PROPERTY_RESIZABLE.equals(name)) {
 			return isResizable();
 		} else if (PROPERTY_SHAPE.equals(name)) {
 			return getShape();
 		} else if (PROPERTY_KIND.equals(name)) {
 			return getKind();
 		} else return super.getProperty(name);
 	}
 	
 	@Override
 	public void dispose() {
 		defaultSize = null;
 		kind = null;
 		label = name = null;
 		
 		if (points != null) {
 			points.removeAllPoints();
 			points = null;
 		}
 		
 		super.dispose();
 	}
 	
 	public Change changeName(String name) {
 		ChangeGroup cg = new ChangeGroup();
 		cg.add(new ChangeName(name));
 		cg.add(new ChangeLabel(name.length() > 0 ? name.substring(0, 1) : name));
 		return cg;
 	}
 	
 	public ChangeShape changeShape(Shape shape) {
 		return new ChangeShape(shape);
 	}
 	
 	public ChangeLabel changeLabel(String label) {
 		return new ChangeLabel(label);
 	}
 	
 	public ChangeResizable changeResizable(boolean resizable) {
 		return new ChangeResizable(resizable);
 	}
 	
 	public ChangeDefaultSize changeDefaultSize(Dimension defaultSize) {
 		return new ChangeDefaultSize(defaultSize);
 	}
 	
 	public ChangeKind changeKind(Kind kind) {
 		return new ChangeKind(kind);
 	}
 	
 	public ChangeAddPort changeAddPort(PortSpec port) {
 		return new ChangeAddPort(port);
 	}
 	
 	public ChangeRemovePort changeRemovePort(PortSpec port) {
 		return new ChangeRemovePort(port);
 	}
 	
 	public ChangePoints changePoints(PointList pl) {
 		return new ChangePoints(pl);
 	}
 }
