/**
 * Copyright (c) 2011-2013, Marc Röttig, Stephan Aiche.
  *
  * This file is part of GenericKnimeNodes.
  * 
  * GenericKnimeNodes is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Lesser General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 package com.genericworkflownodes.knime.nodes.io.listexporter;
 
 import org.knime.core.node.NodeDialogPane;
 import org.knime.core.node.NodeFactory;
 import org.knime.core.node.NodeView;
 
 /**
  * <code>NodeFactory</code> for the "MimeFileExporter" Node.
  * 
  * @author roettig
  */
 public class ListMimeFileExporterNodeFactory extends
 		NodeFactory<ListMimeFileExporterNodeModel> {
 
 	/**
 	 * {@inheritDoc}
 	 */
 	@Override
 	public ListMimeFileExporterNodeModel createNodeModel() {
 		return new ListMimeFileExporterNodeModel();
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	@Override
 	public int getNrNodeViews() {
 		return 0;
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	@Override
 	public NodeView<ListMimeFileExporterNodeModel> createNodeView(
 			final int viewIndex, final ListMimeFileExporterNodeModel nodeModel) {
 		return null;
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	@Override
 	public boolean hasDialog() {
 		return true;
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	@Override
 	public NodeDialogPane createNodeDialogPane() {
 		return new ListMimeFileExporterNodeDialog();
 	}
 
 }
