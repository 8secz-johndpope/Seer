 /*
  * Created on Apr 20, 2005
  * 
  * This software is licensed under the terms of the GNU GENERAL PUBLIC LICENSE
  * Version 2, which can be found at http://www.gnu.org/copyleft/gpl.html
 */
 package org.cubictest.model;
 
 import java.util.ArrayList;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Set;
 
 import org.cubictest.common.utils.Logger;
 import org.cubictest.ui.gef.interfaces.exported.ITestEditor;
 import org.cubictest.utils.CubicCloner;
 import org.eclipse.draw2d.geometry.Dimension;
 import org.eclipse.draw2d.geometry.Point;
 
 /**
  * Base class for all nodes in a test.
  *
  * @author trond
  */
 public abstract class TransitionNode extends PropertyAwareObject{
 	private Transition inTransition = null;
 	private List<Transition> outTransitions = new ArrayList<Transition>();
 	private String id = "";
 	private Point position;
 	private Dimension dimension;
 	private String name = "";
 	
 	public TransitionNode() {
 		this.setId(getNewGeneratedId());
 	}
 
 	/**
 	 * @return Returns the name.
 	 */
 	public String getName() {
 		return name;
 	}
 	
 	public void setName(String name) {
 		String oldName = this.name;
 		this.name = name;
 		firePropertyChange(PropertyAwareObject.NAME, oldName, name);
 	}
 
 	public Dimension getDefaultDimension() {
 		return new Dimension(ITestEditor.INITIAL_PAGE_WIDTH, ITestEditor.INITIAL_PAGE_HEIGHT);
 	}
 	
 	/**
 	 * @return Returns the postition.
 	 */
 	public Point getPosition() {
 		if(position == null) {
 			position = new Point(0,0);
 		}
 		return position;
 	}
 	/**
 	 * @param postition The postition to set.
 	 */
 	public void setPosition(Point position) {
 		Point oldPos = this.position;
 		this.position = position;
 		firePropertyChange(PropertyAwareObject.LAYOUT,oldPos,position);
 	}
 	/**
 	 * @return
 	 */
 	public Transition getInTransition() {
 		return inTransition;
 	}
 	/**
 	 * @param inTransition The inTransition to set.
 	 */
 	public void setInTransition(Transition inTransition) {
 		Transition oldTransition = this.inTransition;
 		this.inTransition = inTransition;
 		if (inTransition != null)
 			firePropertyChange(PropertyAwareObject.INPUT,oldTransition,inTransition);
 	}
 	/**
 	 * @return Returns the outTransitions.
 	 */
 	public List<Transition> getOutTransitions() {
 		Set<Transition> set = new HashSet<Transition>();
 		set.addAll(outTransitions);
 		if (set.size() != outTransitions.size()) {
 			Logger.error("Duplicate out-transitions detected from " + this.toString() + "! Removing them.");
 			outTransitions = new ArrayList<Transition>();
 			outTransitions.addAll(set);
 		}
 		return outTransitions;
 	}
 	/**
 	 * @param outTransitions The outTransitions to set.
 	 */
 	public void setOutTransitions(List<Transition> outTransitions) {
 		this.outTransitions = outTransitions;
 	}
 	/**
 	 * @param transition
 	 */
 	public void addOutTransition(Transition transition){
 		if (outTransitions.contains(transition)) {
 			outTransitions.remove(transition);
 			Logger.warn("Removing duplicate out-transition from " + this.toString() + ".");
 		}
 		outTransitions.add(transition);
 		firePropertyChange(PropertyAwareObject.OUTPUT,null,transition);
 	}
 	/**
 	 * @return Returns the id.
 	 */
 	public String getId() {
 		return id;
 	}
 	/**
 	 * @param id The id to set.
 	 */
 	public void setId(String id) {
 		this.id = id;
 	}
 	/**
 	 * @param linkTransition
 	 */
 	public void removeOutTransition(Transition transition) {
 		outTransitions.remove(transition);
 		firePropertyChange(PropertyAwareObject.OUTPUT,transition,null);
 	}
 	
 	public void removeOutTransitions() {
 		List<Transition> transList = new ArrayList<Transition>();
 		transList.addAll(outTransitions);
 		for (Transition transition : transList) {
 			removeOutTransition(transition);
 		}
 	}
 	
 	/**
 	 * @param linkTransition
 	 */
 	public void removeInTransition() {
 		Transition oldTrans = inTransition;
 		inTransition = null;
 		firePropertyChange(PropertyAwareObject.INPUT,oldTrans,null);
 		
 	}
 	
 	public Dimension getDimension() {
 		if(dimension == null) {
 			dimension = getDefaultDimension();
 		}
 		return dimension;
 	}
 	
 	public void setDimension(Dimension dimension) {
 		Dimension oldDim = this.dimension;
 		this.dimension = dimension;
 		firePropertyChange(PropertyAwareObject.LAYOUT, oldDim, dimension);
 	}
 
 	private String getNewGeneratedId() {
 		String name = getClass().getSimpleName().toLowerCase();
 		return name + this.hashCode() + System.currentTimeMillis();
 	}
 	
 	public boolean hasInTransition() {
 		return getInTransition() != null;
 	}
 
 	@Override
 	public TransitionNode clone() throws CloneNotSupportedException {
 		TransitionNode node = (TransitionNode) CubicCloner.deepCopy(this);
 		node.removeInTransition();
 		node.removeOutTransitions();
 		node.setId(getNewGeneratedId());
 		return node;
 	}
 }
