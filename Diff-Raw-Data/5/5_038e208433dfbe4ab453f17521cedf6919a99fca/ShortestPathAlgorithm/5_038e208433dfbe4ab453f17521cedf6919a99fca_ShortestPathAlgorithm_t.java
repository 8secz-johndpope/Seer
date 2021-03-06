 /* This file is part of LiveCG.
  *
  * Copyright (C) 2013  Sebastian Kuerten
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 package de.topobyte.livecg.algorithms.polygon.shortestpath;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import de.topobyte.livecg.algorithms.polygon.monotonepieces.Diagonal;
 import de.topobyte.livecg.algorithms.polygon.monotonepieces.DiagonalUtil;
 import de.topobyte.livecg.algorithms.polygon.monotonepieces.SplitResult;
 import de.topobyte.livecg.algorithms.polygon.monotonepieces.TriangulationOperation;
 import de.topobyte.livecg.algorithms.polygon.shortestpath.steps.StepFinishAlgorithm;
 import de.topobyte.livecg.algorithms.polygon.shortestpath.steps.StepInitializeAlgorithm;
 import de.topobyte.livecg.core.algorithm.DefaultSceneAlgorithm;
 import de.topobyte.livecg.core.algorithm.Explainable;
 import de.topobyte.livecg.core.algorithm.steps.Step;
 import de.topobyte.livecg.core.algorithm.steps.StepUtil;
 import de.topobyte.livecg.core.geometry.geom.BoundingBoxes;
 import de.topobyte.livecg.core.geometry.geom.Chain;
 import de.topobyte.livecg.core.geometry.geom.CrossingsTest;
 import de.topobyte.livecg.core.geometry.geom.GeomMath;
 import de.topobyte.livecg.core.geometry.geom.Node;
 import de.topobyte.livecg.core.geometry.geom.Polygon;
 import de.topobyte.livecg.core.geometry.geom.Rectangle;
 import de.topobyte.livecg.core.geometry.geom.Rectangles;
 import de.topobyte.livecg.util.graph.Graph;
 
 public class ShortestPathAlgorithm extends DefaultSceneAlgorithm implements
 		Explainable
 {
 	final static Logger logger = LoggerFactory
 			.getLogger(ShortestPathAlgorithm.class);
 
 	private Polygon polygon;
 	private TriangulationOperation triangulationOperation;
 	private List<Diagonal> triangulationDiagonals;
 	private SplitResult splitResult;
 	private Graph<Polygon, Diagonal> graph;
 
 	private Node start;
 	private Node target;
 
 	private Polygon triangleStart;
 	private Polygon triangleTarget;
 
 	private boolean nodeHitStart = false;
 	private boolean nodeHitTarget = false;
 
 	private Sleeve sleeve;
 	private int numberOfSteps;
 
 	// First diagonal
 	private Node left;
 	private Node right;
 
 	private int status;
 	private int subStatus;
 	private Data data;
 
 	private List<Data> history = new ArrayList<Data>();
 
 	public ShortestPathAlgorithm(Polygon polygon, Node start, Node target)
 	{
 		this.polygon = polygon;
 		this.start = start;
 		this.target = target;
 		triangulationOperation = new TriangulationOperation(polygon);
 		triangulationDiagonals = triangulationOperation.getDiagonals();
 
 		splitResult = DiagonalUtil.split(polygon, triangulationDiagonals);
 		graph = splitResult.getGraph();
 
 		setup();
 	}
 
 	@Override
 	public Rectangle getScene()
 	{
 		Rectangle bbox = BoundingBoxes.get(polygon);
 		Rectangle scene = Rectangles.extend(bbox, 15);
 		return scene;
 	}
 
 	public void setStart(Node start)
 	{
 		this.start = start;
 		nodeHitStart = false;
 		triangleStart = null;
 		setup();
 		fireAlgorithmChanged();
 	}
 
 	public void setTarget(Node target)
 	{
 		this.target = target;
 		nodeHitTarget = false;
 		triangleTarget = null;
 		setup();
 		fireAlgorithmChanged();
 	}
 
 	public void setStartTarget(Node start, Node target)
 	{
 		this.start = start;
 		this.target = target;
 		nodeHitStart = false;
 		nodeHitTarget = false;
 		triangleStart = null;
 		triangleTarget = null;
 		setup();
 		fireAlgorithmChanged();
 	}
 
 	private void setup()
 	{
 		List<Polygon> triangulation = splitResult.getPolygons();
 		for (Polygon triangle : triangulation) {
 			Chain shell = triangle.getShell();
 			for (int i = 0; i < shell.getNumberOfNodes(); i++) {
 				if (shell.getNode(i) == start) {
 					nodeHitStart = true;
 					triangleStart = triangle;
 				}
 				if (shell.getNode(i) == target) {
 					nodeHitTarget = true;
 					triangleTarget = triangle;
 				}
 			}
 		}
 		if (triangleStart == null || triangleTarget == null) {
 			for (Polygon triangle : triangulation) {
 				CrossingsTest test = new CrossingsTest(triangle.getShell());
 				if (triangleStart == null && test.covers(start.getCoordinate())) {
 					triangleStart = triangle;
 				}
 				if (triangleTarget == null
 						&& test.covers(target.getCoordinate())) {
 					triangleTarget = triangle;
 				}
 			}
 		}
 
 		if (triangleStart == null || triangleTarget == null) {
 			if (triangleStart == null) {
 				logger.error("unable to locate start triangle");
 			}
 			if (triangleTarget == null) {
 				logger.error("unable to locate target triangle");
 			}
 			return;
 		}
 
 		sleeve = GraphFinder.find(graph, triangleStart, triangleTarget);
 		SleeveUtil.optimizePath(sleeve, start, target);
 
 		List<Polygon> triangles = sleeve.getPolygons();
 		triangleStart = triangles.get(0);
 		triangleTarget = triangles.get(triangles.size() - 1);
 
 		if (triangleStart == triangleTarget) {
 			numberOfSteps = 1;
 			return;
 		}
 
 		numberOfSteps = sleeve.getDiagonals().size() + 3;
 
 		// Get the first triangle
 		Polygon p0 = sleeve.getPolygons().get(0);
 		Node n0 = p0.getShell().getNode(0);
 		Node n1 = p0.getShell().getNode(1);
 		Node n2 = p0.getShell().getNode(2);
 		// Get the first diagonal
 		Diagonal d0 = sleeve.getDiagonals().get(0);
 		Node d0a = d0.getA();
 		Node d0b = d0.getB();
 		// Two of the first triangle's nodes must fit the triangle
 		if (n0 == d0a && n2 == d0b || n1 == d0a && n0 == d0b || n2 == d0a
 				&& n1 == d0b) {
 			left = d0a;
 			right = d0b;
 		} else if (n0 == d0b && n2 == d0a || n1 == d0b && n0 == d0a
 				|| n2 == d0b && n1 == d0a) {
 			left = d0b;
 			right = d0a;
 		} else {
 			logger.error("Could not match first triangle with first diagonal");
 		}
 
 		if (!nodeHitStart) {
 			Chain shell = new Chain();
 			shell.appendNode(right);
 			shell.appendNode(left);
 			shell.appendNode(start);
 			p0 = new Polygon(shell, null);
 			sleeve.getPolygons().set(0, p0);
 		}
 		if (!nodeHitTarget) {
 			Diagonal dN = sleeve.getDiagonals().get(
 					sleeve.getDiagonals().size() - 1);
 			Chain shell = new Chain();
 			if (GeomMath.isLeftOf(dN.getA().getCoordinate(), dN.getB()
 					.getCoordinate(), target.getCoordinate())) {
 				shell.appendNode(dN.getA());
 				shell.appendNode(dN.getB());
 				shell.appendNode(target);
 			} else {
 				shell.appendNode(dN.getB());
 				shell.appendNode(dN.getA());
 				shell.appendNode(target);
 			}
 			Polygon pN = new Polygon(shell, null);
 			sleeve.getPolygons().set(sleeve.getPolygons().size() - 1, pN);
 		}
 	}
 
 	public Polygon getPolygon()
 	{
 		return polygon;
 	}
 
 	public Node getNodeStart()
 	{
 		return start;
 	}
 
 	public Node getNodeTarget()
 	{
 		return target;
 	}
 
 	public Sleeve getSleeve()
 	{
 		return sleeve;
 	}
 
 	public Polygon getTriangleStart()
 	{
 		return triangleStart;
 	}
 
 	public Polygon getTriangleTarget()
 	{
 		return triangleTarget;
 	}
 
 	public Graph<Polygon, Diagonal> getGraph()
 	{
 		return graph;
 	}
 
 	public List<Diagonal> getTriangulationDiagonals()
 	{
 		return triangulationDiagonals;
 	}
 
 	public int getNumberOfSteps()
 	{
 		return numberOfSteps;
 	}
 
 	public int getStatus()
 	{
 		return status;
 	}
 
 	public int getSubStatus()
 	{
 		return subStatus;
 	}
 
 	public Data getData()
 	{
 		return data;
 	}
 
 	public void setStatus(int status, int subStatus)
 	{
 		if (this.status == status && this.subStatus == subStatus) {
 			return;
 		}
 		if (this.status != status) {
 			if (status == 0) {
 				data = null;
 				this.status = 0;
 				history.clear();
 			} else if (status > this.status) {
 				computeUpTo(status - 1);
 			} else if (status < this.status) {
 				if (status < 2) {
 					data = null;
 				} else {
 					data = history.get(status - 2).clone();
 				}
 				while (history.size() > status) {
 					history.remove(history.size() - 1);
 				}
 				this.status = status;
 			}
 		}
 		this.subStatus = subStatus;
 		fireAlgorithmStatusChanged();
 	}
 
 	public void setSubStatus(int subStatus)
 	{
 		this.subStatus = subStatus;
 		fireAlgorithmStatusChanged();
 	}
 
 	public Side getSideOfNextNode()
 	{
 		Diagonal next = nextDiagonal();
 		Side currentChain = sideOfNextNode(next);
 		return currentChain;
 	}
 
 	private void computeUpTo(int diagonal)
 	{
 		System.out.println("compute to " + diagonal);
 		if (status == 0) {
 			status = 1;
 		} else if (status == 1) {
 			status = 2;
 			// Initialize data structures
 			data = new Data(start, left, right);
 			history.add(data.clone());
 
 			// Handle the case with start and target lying in the same triangle
 			if (triangleStart == triangleTarget) {
 				data.appendCommon(target);
 				data.clear(Side.LEFT);
 				data.clear(Side.RIGHT);
 			}
 		}
 
 		// Main algorithm loop
 		List<Diagonal> diagonals = sleeve.getDiagonals();
 		while (status - 1 <= diagonals.size() && status - 1 < diagonal) {
 			logger.debug("Diagonal " + status);
 			Diagonal next = nextDiagonal();
 			Side currentChain = sideOfNextNode(next);
 			logger.debug("next node is on " + currentChain + " chain");
 			// Find node of diagonal that is not node of d_(i-1)
 			Node notYetOnChain = notYetOnChain(next);
 			FunnelUtil.updateFunnel(data, notYetOnChain, currentChain);
 			logger.debug("left path length: " + data.getFunnelLength(Side.LEFT));
 			logger.debug("right path length: "
 					+ data.getFunnelLength(Side.RIGHT));
 			history.add(data.clone());
 			status += 1;
 		}
 
 		// Make the current path the overall shortest path
 		if (diagonal >= diagonals.size() + 2) {
 			Side currentChain = Side.LEFT;
 			if (data.getLast(Side.RIGHT) == target) {
 				currentChain = Side.RIGHT;
 			}
 			for (int i = 0; i < data.getFunnelLength(currentChain);) {
 				data.appendCommon(data.removeFirst(currentChain));
 			}
 			data.clear(currentChain.other());
 			history.add(data.clone());
 			status = diagonal + 1;
 		}
 	}
 
 	public Data getNextFunnel()
 	{
 		if (data == null) {
 			return new Data(start, left, right);
 		}
 		Data copy = data.clone();
 		Diagonal next = nextDiagonal();
 		Side currentChain = sideOfNextNode(next);
 		// Find node of diagonal that is not node of d_(i-1)
 		Node notYetOnChain = notYetOnChain(next);
 		FunnelUtil.updateFunnel(copy, notYetOnChain, currentChain);
 		return copy;
 	}
 
 	private Diagonal nextDiagonal()
 	{
 		List<Diagonal> diagonals = sleeve.getDiagonals();
 		if (status - 1 < diagonals.size()) {
 			return diagonals.get(status - 1);
 		} else {
 			Diagonal last = diagonals.get(diagonals.size() - 1);
 			// Add a final diagonal that extends the right chain
 			if (last.getA() == data.getLast(Side.LEFT)) {
 				return new Diagonal(last.getA(), target);
 			} else {
 				return new Diagonal(last.getB(), target);
 			}
 		}
 	}
 
 	private Side sideOfNextNode(Diagonal d)
 	{
 		Node left = data.getLast(Side.LEFT);
 		Node right = data.getLast(Side.RIGHT);
 		if (d.getA() == left || d.getB() == left) {
 			return Side.RIGHT;
 		} else if (d.getA() == right || d.getB() == right) {
 			return Side.LEFT;
 		} else {
 			return null;
 		}
 	}
 
 	// Find node of diagonal that is not node of d_(i-1)
 	private Node notYetOnChain(Diagonal diagonal)
 	{
 		Node endOfLeftPath = data.getLast(Side.LEFT);
 		Node endOfRightPath = data.getLast(Side.RIGHT);
 		if (diagonal.getA() == endOfLeftPath
 				|| diagonal.getA() == endOfRightPath) {
 			return diagonal.getB();
 		}
 		return diagonal.getA();
 	}
 
 	public int numberOfStepsToNextDiagonal()
 	{
 		return StepUtil.totalNumberOfSteps(stepsToNextDiagonal());
 	}
 
 	public List<Step> stepsToNextDiagonal()
 	{
 		List<Step> steps = new ArrayList<Step>();
 
 		if (status == 0) {
 			return steps;
 		}
 
 		if (status == 1) {
 			steps.add(new StepInitializeAlgorithm());
 			return steps;
 		}
 
 		if (status == sleeve.getDiagonals().size() + 2) {
 			steps.add(new StepFinishAlgorithm());
 			return steps;
 		}
 
 		if (status == sleeve.getDiagonals().size() + 3) {
 			// No steps left here, we're finished
 			return steps;
 		}
 
 		Diagonal next = nextDiagonal();
 		Side on = sideOfNextNode(next);
 		Node notYetOnChain = notYetOnChain(next);
 
 		return FunnelUtil.stepsToUpdateFunnel(data, next, on, notYetOnChain);
 	}
 
 	public Node getNextNode()
 	{
 		Diagonal next = nextDiagonal();
 		return notYetOnChain(next);
 	}
 
 	public Node getNthNodeOfFunnelTraversal(int s)
 	{
 		Diagonal next = nextDiagonal();
 		Side on = sideOfNextNode(next);
 		return FunnelUtil.getNthNodeOfFunnelTraversal(data, next, on, s);
 	}
 
 	private List<String> messages = new ArrayList<String>();
 
 	private void addMessage(String text)
 	{
 		// String prefix = status + ", " + subStatus + ": ";
 		// messages.add(prefix + text);
 		messages.add(text);
 	}
 
 	@Override
 	public List<String> explain()
 	{
 		messages.clear();
 		if (status == 0) {
 			if (subStatus == 0) {
 				addMessage("The algorithm has just started.");
 			}
 		} else if (status == 1) {
 			if (subStatus == 0) {
 				addMessage("DIAGONAL: " + status);
 				addMessage("The funnel will be initialized with the first diagonal of the sleeve.");
 			} else if (subStatus == 1) {
 				addMessage("Funnel initialized.");
 			}
 		} else if (status <= sleeve.getDiagonals().size() + 1) {
 			List<Step> steps = stepsToNextDiagonal();
 			int nSteps = StepUtil.totalNumberOfSteps(steps);
 			if (subStatus == 0) {
 				addMessage("DIAGONAL: " + status);
 				if (status - 1 == sleeve.getDiagonals().size()) {
 					addMessage("The next diagonal is the last one.");
 				}
 			} else if (subStatus == 1) {
 				Side side = sideOfNextNode(nextDiagonal());
 				addMessage("The node of the next diagonal is on the "
 						+ name(side) + " path.");
 			} else {
				if (subStatus < nSteps) {
 					if (subStatus == 2) {
 						addMessage("We check the first segment for funnel concavity.");
 					} else {
 						addMessage("We check the next segment for funnel concavity.");
 					}
				}
				if (subStatus + 1 < nSteps) {
 					addMessage("When adding this segment, the funnel would not be concave anymore.");
 				} else if (subStatus + 1 == nSteps) {
 					addMessage("When adding this segment, the funnel will be concave.");
 					addMessage("We add this segment to the funnel.");
 				} else if (subStatus == nSteps) {
 					if (status < sleeve.getDiagonals().size()) {
 						addMessage("We continue with the next diagonal.");
 					} else {
 						addMessage("No diagonals left.");
 					}
 				}
 			}
 		} else if (status == sleeve.getDiagonals().size() + 2) {
 			Side currentChain = Side.LEFT;
 			if (data.getLast(Side.RIGHT) == target) {
 				currentChain = Side.RIGHT;
 			}
 			if (subStatus == 0) {
 				addMessage("We reached the target with the "
 						+ name(currentChain) + " path.");
 			} else if (subStatus == 1) {
 				addMessage("We set the result to the " + name(currentChain)
 						+ " path.");
 			}
 		} else if (status == sleeve.getDiagonals().size() + 3) {
 			addMessage("The algorithm is complete.");
 		}
 		return messages;
 	}
 
 	private String name(Side side)
 	{
 		return side.toString().toLowerCase();
 	}
 }
