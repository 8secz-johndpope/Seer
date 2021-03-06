 /*******************************************************************************
  * Caleydo - visualization for molecular biology - http://caleydo.org
  * 
  * Copyright(C) 2005, 2012 Graz University of Technology, Marc Streit, Alexander
  * Lex, Christian Partl, Johannes Kepler University Linz </p>
  * 
  * This program is free software: you can redistribute it and/or modify it under
  * the terms of the GNU General Public License as published by the Free Software
  * Foundation, either version 3 of the License, or (at your option) any later
  * version.
  * 
  * This program is distributed in the hope that it will be useful, but WITHOUT
  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
  * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
  * details.
  * 
  * You should have received a copy of the GNU General Public License along with
  * this program. If not, see <http://www.gnu.org/licenses/>
  *******************************************************************************/
 package org.caleydo.datadomain.pathway.graph.item.vertex;
 
 import java.io.Serializable;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.StringTokenizer;
 
 import org.caleydo.core.data.IUniqueObject;
 import org.caleydo.core.data.id.ManagedObjectType;
 import org.caleydo.core.manager.GeneralManager;
 import org.caleydo.core.util.collection.Pair;
 import org.caleydo.datadomain.pathway.graph.PathwayGraph;
 import org.caleydo.datadomain.pathway.manager.PathwayItemManager;
 
 /**
  * <p>
  * A <code>PathwayVertexRep</code> is a visible representation of a node (a
  * {@link PathwayVertex}) in a pathway texture. It may contain 1-n
  * {@link PathwayVertex} objects.
  * </p>
  * <p>
  * The representation contains information on the type of shape, the position
  * and the size of the representation.
  * </p>
  * 
  * @author Marc Streit
  * @author Alexander Lex
  */
 public class PathwayVertexRep implements Serializable, IUniqueObject {
 
 	private static final long serialVersionUID = 1L;
 
 	/** A unique id of the vertex rep */
 	private int id;
 
 	private String name;
 
 	/** The type of shape that this vertex rep uses */
 	private EPathwayVertexShape shape;
 
 	private ArrayList<Pair<Short, Short>> coords;
 
 	private short width = 20;
 
 	private short height = 20;
 
 	/**
 	 * The {@link PathwayVertex} objects that map to this representation there
 	 * might be several
 	 */
 	private List<PathwayVertex> pathwayVertices = new ArrayList<PathwayVertex>();
 
 	private PathwayGraph pathway;
 
 	/**
 	 * Constructor.
 	 * 
 	 * @param name
 	 * @param shapeType
 	 * @param sCoords
 	 */
 	public PathwayVertexRep(final String name, final String shapeType,
 			final String sCoords) {
 
 		id = GeneralManager.get().getIDCreator()
 				.createID(ManagedObjectType.PATHWAY_VERTEX_REP);
 
 		shape = EPathwayVertexShape.valueOf(shapeType);
 		this.name = name;
 
 		setCoordsByCommaSeparatedString(sCoords);
 	}
 
 	/**
 	 * Constructor.
 	 * 
 	 * @param name
 	 * @param shapeType
 	 * @param x
 	 * @param y
 	 * @param width
 	 * @param height
 	 */
 	public PathwayVertexRep(final String name, final String shapeType, final short x,
 			final short y, final short width, final short height) {
 
		id = GeneralManager.get().getIDCreator()
				.createID(ManagedObjectType.PATHWAY_VERTEX_REP);
		
 		if (shapeType == null || shapeType.isEmpty())
 			shape = EPathwayVertexShape.rect;
 		else
 			shape = EPathwayVertexShape.valueOf(shapeType);
 
 		this.name = name;
 		this.width = width;
 		this.height = height;
 
 		setRectangularCoords(x, y, width, height);
 	}
 
 	/**
 	 * Example: 213,521,202,515,248,440,261,447,213,521 Currently used for
 	 * BioCarta input.
 	 */
 	private void setCoordsByCommaSeparatedString(final String sCoords) {
 
 		String[] stringCoordinates = sCoords.split(",");
 		coords = new ArrayList<Pair<Short, Short>>();
 
 		for (int coordinateCount = 0; coordinateCount < stringCoordinates.length;) {
 			// Filter white spaces
 			String xString = stringCoordinates[coordinateCount++].replace(" ", "");
 			Short xCoord = Short.valueOf(xString);
 
 			if(coordinateCount >= stringCoordinates.length)
 				return;
 			
 			String yString = stringCoordinates[coordinateCount++].replace(" ", "");
 			Short yCoord = Short.valueOf(yString);
 
 			Pair<Short, Short> coordinates = new Pair<Short, Short>(xCoord, yCoord);
 			coords.add(coordinates);
 		}
 	}
 
 	private void setRectangularCoords(final short x, final short y, final short width,
 			final short height) {
 
 		coords = new ArrayList<Pair<Short, Short>>(4);
 
 		coords.add(new Pair<Short, Short>(x, y));
 		coords.add(new Pair<Short, Short>((short) (x + width), y));
 		coords.add(new Pair<Short, Short>((short) (x + width), (short) (y + height)));
 		coords.add(new Pair<Short, Short>(x, (short) (y + height)));
 	}
 
 	/**
 	 * @return the id, see {@link #id}
 	 */
 	public int getID() {
 		return id;
 	}
 
 	public String getName() {
 
 		return name;
 	}
 
 	public EPathwayVertexShape getShapeType() {
 
 		return shape;
 	}
 
 	public ArrayList<Pair<Short, Short>> getCoords() {
 
 		return coords;
 	}
 
 	public short getXOrigin() {
 
 		return coords.get(0).getFirst();
 	}
 
 	public short getYOrigin() {
 
 		return coords.get(0).getSecond();
 	}
 
 	public short getWidth() {
 
 		return width;
 	}
 
 	public short getHeight() {
 
 		return height;
 	}
 
 	@Override
 	public String toString() {
 
 		return name;
 	}
 
 	/** Adds a vertex to {@link #pathwayVertices} */
 	public void addPathwayVertex(PathwayVertex vertex) {
 		pathwayVertices.add(vertex);
 	}
 
 	/**
 	 * @return The pathwayVertices, see {@link #pathwayVertices}, or an empty
 	 *         list if no vertices can be resolved.
 	 */
 	public List<PathwayVertex> getPathwayVertices() {
 		return pathwayVertices;
 	}
 
 	/**
 	 * @param pathway
 	 *            setter, see {@link #pathway}
 	 */
 	public void setPathway(PathwayGraph pathway) {
 		this.pathway = pathway;
 	}
 
 	/**
 	 * @return the pathway, see {@link #pathway}
 	 */
 	public PathwayGraph getPathway() {
 		return pathway;
 	}
 
 	/**
 	 * Returns the type of the pathway vertex underneath, assuming that alle
 	 * vertex reps are of the same type
 	 */
 	public EPathwayVertexType getType() {
 		return pathwayVertices.get(0).getType();
 	}
 
 	/**
 	 * Returns all david IDs of all vertices stored in this
 	 * <code>PathwayVertexRep</code>, or an empty list if no IDs can be mapped.
 	 * 
 	 * @see PathwayItemManager#getDavidIDsByPathwayVertexRep(PathwayVertexRep)
 	 */
 	public ArrayList<Integer> getDavidIDs() {
 		return PathwayItemManager.get().getDavidIDsByPathwayVertexRep(this);
 	}
 }
