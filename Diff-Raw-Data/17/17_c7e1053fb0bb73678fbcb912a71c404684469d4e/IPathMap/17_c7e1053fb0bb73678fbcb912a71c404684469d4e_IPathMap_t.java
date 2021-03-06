 package org.aimas.craftingquest.core.pathfinding;
 
 import org.aimas.craftingquest.state.Point2i;
 import org.aimas.craftingquest.state.UnitState;
 
 public interface IPathMap
 {
	static final float INF = 99999.0f;
	
 	public int getWidth();
 	public int getHeight();
 	
	//costs used in path finding algorithm.
	//inpassable cells should have a value higher than 90000.0f (ex. 99999.0f).
	//use INF for impassable cells.
 	//you can use this cost function to customize the importance of each cell !
 	public float getCellCost(Point2i cell, UnitState unit);
 }
