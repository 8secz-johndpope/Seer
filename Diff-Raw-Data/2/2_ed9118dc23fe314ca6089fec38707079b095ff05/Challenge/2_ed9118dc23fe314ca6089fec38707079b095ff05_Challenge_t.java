 import java.awt.Polygon;
 
 import java.awt.Rectangle;
 
 import java.lang.reflect.Method;
 
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.SortedSet;
 import java.util.TreeSet;
 
 public class Challenge
 {
     public Challenge(Board b) {
         this(b, "");
     }
     public Challenge(Board b, String s) {
 	board = b;
     }
 
     Board board;
 
     public static Set<Set<Point>> getEnclosures(Board b) {
 	SortedSet<Point> allWalls = getWallParticles(b);
	SortedSet<Point> walls = Collections.synchronizedSortedSet(new TreeSet<Point>(allWalls));
 	HashSet<Set<Point>> enclosures = new HashSet<Set<Point>>();
 
 	// Check if there are walls, then see if those walls make cages
 	if(walls.size() > 3)
 	{
 	    // Consider only walls can form a side of a cage
 	    SortedSet<Point> tempSet = new TreeSet<Point>(walls);
 	    List<Point> neighbors;
 	    {
 		Point p = null;
 		while(!tempSet.isEmpty()){
 		    if(p == null)
 			p = tempSet.first();
 
 		    neighbors = getNeighbors(walls, p);
 		    tempSet.remove(p);
 
 		    if(neighbors.size() < 2)
 		    {
 			walls.remove(p);
 			if(neighbors.size() == 1)
 			{
 			    p = neighbors.get(0);
 			    continue;
 			}
 		    }
 
 		    p = null;
 		}
 	    }
 
 	    // Remove walls that don't touch a non-wall cell
 	    Point[] tempArr = new Point[walls.size()];
 	    walls.toArray(tempArr);
 	    for(Point p : tempArr) {
 		if(getNeighbors(walls, p).size() + getNeighbors(tempSet, p).size() + getDiagNeighbors(walls, p).size() + getDiagNeighbors(tempSet, p).size() == 8)
 		{
 		    walls.remove(p);
 		    tempSet.add(p);
 		}
 	    }
 
 	    Point start;
 	    while(!walls.isEmpty()) {
 		// Make a polygon
 		start = walls.first();
 		tempSet.clear();
 
 		Polygon poly = tracePolygon(walls, tempSet, new HashSet<Point>(), start, start, null);
 		if(poly == null) {
 		    walls.remove(start);
 		}
 		else {
 		    walls.removeAll(tempSet);
 
 		    TreeSet<Point> tempSet2 = new TreeSet<Point>();
 		    Rectangle bounds = poly.getBounds();
 		    for(int x = (int)bounds.getX(); x < bounds.getX() + bounds.getWidth(); ++x) {
 			for(int y = (int)bounds.getY(); y < bounds.getY() + bounds.getHeight(); ++y) {
 			    Point q = new Point(x, y);
 			    if(!allWalls.contains(q) && poly.contains(q))
 				tempSet2.add(new Point(x, y));
 			}
 		    }
 		    enclosures.add(tempSet2);
 		}
 	    }
 	}
 	return enclosures;
     }
     private static Polygon tracePolygon(Set<Point> walls, Set<Point> added, Set<Point> eliminated, Point start, Point current, Point last) {
 	boolean finished = false;
 	added.add(current);
 	List<Point> neighbors = getNeighbors(walls, current);
 	if(neighbors.size() < 2)
 	    return null;
 
 	int i;
 	int temp;
 	if(last != null) {
 	    temp = neighbors.indexOf(last) % neighbors.size();
 	    i = (temp + 1) % neighbors.size();
 	}
 	else {
 	    temp = neighbors.size() - 1;
 	    i = 0;
 	}
 	for(; i != temp; i = (i + 1) % neighbors.size()) {
 	    Point p = neighbors.get(i % neighbors.size());
 	    if(!(added.contains(p) || eliminated.contains(current))) {
 		Polygon poly = tracePolygon(walls, added, eliminated, start, p, current);
 		if(poly != null) {
 		    poly.addPoint(current.x, current.y);
 		    return poly;
 		}
 	    }
 	    else if(p.equals(start))
 		finished = true;
 	}
 	if(!finished)
 	{
 	    added.remove(current);
 	    eliminated.add(current);
 	    return null;
 	}
 	else {
 	    Polygon poly = new Polygon();
 	    poly.addPoint(current.x, current.y);
 	    return poly;
 	}
     }
     public static TreeSet<Point> getWallParticles(Board b) {
 	TreeSet<Point> walls = new TreeSet<Point>();
 
 	// TODO: use a more fixed indication of what a wall is other than name
 	for(int i = 1; i <= 5; ++i) {
 	    Particle p = b.getParticleByName("wall/" + i);
 	    if(p != null){
 		walls.addAll(p.getOccupiedPoints());
 	    }
 	}
 
 	return walls;
     }
     // Adds all neighboring walls inside the set to a SortedSet
     // Neighbors MUST be added in order: left, up, right, then down
     public static List<Point> getNeighbors(Set<Point> walls, Point p) {
 	ArrayList<Point> neighbors = new ArrayList<Point>();
 	Point q;
 	for(int dir = 0; dir < 4; ++dir) {
 	    q = new Point(p);
 	    switch(dir)
 	    {
 		case 0:
 		    q.x--;
 		    break;
 		case 1:
 		    q.y--;
 		    break;
 		case 2:
 		    q.x++;
 		    break;
 		case 3:
 		    q.y++;
 		    break;
 	    }
 	    if(walls.contains(q))
 		neighbors.add(q);
 	}
 
 	return neighbors;
     }
     public static Set<Point> getDiagNeighbors(Set<Point> walls, Point p) {
 	HashSet<Point> neighbors = new HashSet<Point>();
 	Point q;
 	for(int dir = 0; dir < 4; ++dir) {
 	    q = new Point(p);
 	    switch(dir)
 	    {
 		case 0:
 		    q.x--;
 		    q.y--;
 		    break;
 		case 1:
 		    q.x++;
 		    q.y--;
 		    break;
 		case 2:
 		    q.x--;
 		    q.y++;
 		    break;
 		case 3:
 		    q.x++;
 		    q.y++;
 		    break;
 	    }
 	    if(walls.contains(q))
 		neighbors.add(q);
 	}
 
 	return neighbors;
     }
 
     public boolean check() {
 	int score = 0;
 
 	Set<Point> particlePoints = board.getParticleByName("zoo_guest").getOccupiedPoints();
 	for(Set<Point> enclosure : getEnclosures(board)) {
 	    enclosure.retainAll(particlePoints);
 	    if(enclosure.size() >= 5) {
 		++score;
 	    }
 	}
 	if(score >= 3)
 	    return true;
 
 	//if(getEnclosures(board).size() > 10)
 	//    return true;
 
 	return false;
     }
 
     private abstract class Condition {
 	public Condition(){
 
 	}
 	public Condition(int s){
 	    this();
 	    score = s;
 	}
 
 	protected int score = 1;
 
 	public boolean check() {
 	    return false;
 	}
     }
 
     private class ForEachArea extends Condition {
 	public boolean check() {
 	    return true;
 	}
     }
 }
