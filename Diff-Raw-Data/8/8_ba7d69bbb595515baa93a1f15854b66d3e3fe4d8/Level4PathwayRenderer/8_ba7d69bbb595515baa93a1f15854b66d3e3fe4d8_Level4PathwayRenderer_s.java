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
 package org.caleydo.view.subgraph;
 
 import java.awt.geom.Rectangle2D;
 import java.util.ArrayList;
 import java.util.List;
 
 import javax.media.opengl.GL2;
 
 import org.caleydo.core.view.opengl.layout.ALayoutRenderer;
 import org.caleydo.datadomain.pathway.IPathwayRepresentation;
 import org.caleydo.datadomain.pathway.VertexRepBasedContextMenuItem;
 import org.caleydo.datadomain.pathway.graph.PathwayGraph;
 import org.caleydo.datadomain.pathway.graph.item.vertex.PathwayVertexRep;
 
 /**
  * @author Christian
  *
  */
 public class Level4PathwayRenderer extends ALayoutRenderer implements IPathwayRepresentation {
 
 	protected final PathwayGraph pathway;
 
 	public Level4PathwayRenderer(PathwayGraph pathway) {
 		this.pathway = pathway;
 	}
 
 	@Override
 	protected void renderContent(GL2 gl) {
 		// render nothing
 	}
 
 	@Override
 	protected boolean permitsWrappingDisplayLists() {
 		// it is actually better not to create a display list if there is nothing to render
 		return false;
 	}
 
 	@Override
 	public int getMinHeightPixels() {
 		return 0;
 	}
 
 	@Override
 	public int getMinWidthPixels() {
 		return 0;
 	}
 
 	@Override
 	public PathwayGraph getPathway() {
 		return pathway;
 	}
 
 	@Override
 	public List<PathwayGraph> getPathways() {
 		List<PathwayGraph> pathways = new ArrayList<>(1);
 		pathways.add(pathway);
 		return pathways;
 	}
 
 	@Override
 	public Rectangle2D getVertexRepBounds(PathwayVertexRep vertexRep) {
 		return null;
 	}
 
 	@Override
 	public List<Rectangle2D> getVertexRepsBounds(PathwayVertexRep vertexRep) {
 		return null;
 	}
 
 	@Override
 	public void addVertexRepBasedContextMenuItem(VertexRepBasedContextMenuItem item) {
 	}
 
 }
