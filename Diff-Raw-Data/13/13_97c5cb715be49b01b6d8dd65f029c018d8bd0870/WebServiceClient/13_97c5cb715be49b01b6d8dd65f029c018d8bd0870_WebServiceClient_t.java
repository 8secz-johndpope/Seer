 /*
  Copyright (c) 2006, 2007, The Cytoscape Consortium (www.cytoscape.org)
 
  The Cytoscape Consortium is:
  - Institute for Systems Biology
  - University of California San Diego
  - Memorial Sloan-Kettering Cancer Center
  - Institut Pasteur
  - Agilent Technologies
 
  This library is free software; you can redistribute it and/or modify it
  under the terms of the GNU Lesser General Public License as published
  by the Free Software Foundation; either version 2.1 of the License, or
  any later version.
 
  This library is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
  MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
  documentation provided hereunder is on an "as is" basis, and the
  Institute for Systems Biology and the Whitehead Institute
  have no obligations to provide maintenance, support,
  updates, enhancements or modifications.  In no event shall the
  Institute for Systems Biology and the Whitehead Institute
  be liable to any party for direct, indirect, special,
  incidental or consequential damages, including lost profits, arising
  out of the use of this software and its documentation, even if the
  Institute for Systems Biology and the Whitehead Institute
  have been advised of the possibility of such damage.  See
  the GNU Lesser General Public License for more details.
 
  You should have received a copy of the GNU Lesser General Public License
  along with this library; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
  */
 package org.cytoscape.io.webservice;
 
 import java.awt.Container;
 import java.net.URI;
 
 import org.cytoscape.work.ProvidesGUI;
 import org.cytoscape.work.TaskFactory;
 
 /**
  * Thin wrapper for SOAP/REST web service clients.
  * 
  * All web service clients <strong>must</strong> implement this method.
  * 
  * 
  */
 public interface WebServiceClient extends TaskFactory {
 	
 	/**
 	 * Returns resource location of this service, i.e., service URL.
 	 * This is guaranteed to be globally unique and can be used as identifier.
 	 * 
 	 * @return URI of the service.
 	 */
 	URI getServiceLocation();
 
 	
 	/**
 	 * Returns display name of this client. This is more human readable name for
 	 * this client.  This may not be unique.
 	 * 
 	 * @return display name for this client.
 	 */
 	String getDisplayName();
 	
 	
 	/**
 	 * Get human-readable description of this client.
 	 * 
 	 * @return Description as a string. Users should write parser for this
 	 *         return value.
 	 */
 	String getDescription();
 
 	
 	/**
 	 * Returns query builder UI.  Since this is a TaskFactory, 
 	 * getTaskIterator() method should use parameters from this GUI.
 	 * 
 	 * @return query builder UI.
 	 */
 	@ProvidesGUI Container getQueryBuilderGUI();
 	
 	
 	/**
 	 * Set query for the tasks to be executed.
 	 * 
 	 * @param query query object.  This is client-dependent.
 	 */
 	void setQuery(Object query);
 	
 }
