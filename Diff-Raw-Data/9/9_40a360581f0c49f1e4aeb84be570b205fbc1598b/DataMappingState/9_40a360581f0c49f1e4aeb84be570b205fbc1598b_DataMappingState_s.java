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
 package org.caleydo.view.entourage.datamapping;
 
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
 import java.util.Set;
 
 import org.caleydo.core.data.datadomain.ATableBasedDataDomain;
 import org.caleydo.core.data.datadomain.DataDomainManager;
 import org.caleydo.core.data.perspective.table.TablePerspective;
 import org.caleydo.core.data.perspective.variable.Perspective;
 import org.caleydo.core.data.virtualarray.events.ClearGroupSelectionEvent;
 import org.caleydo.core.event.EventPublisher;
 import org.caleydo.core.event.view.TablePerspectivesChangedEvent;
 import org.caleydo.core.id.IDCategory;
 import org.caleydo.core.view.listener.AddTablePerspectivesEvent;
 import org.caleydo.core.view.listener.RemoveTablePerspectiveEvent;
 import org.caleydo.datadomain.genetic.GeneticDataDomain;
 import org.caleydo.datadomain.pathway.listener.PathwayMappingEvent;
 import org.caleydo.view.entourage.GLEntourage;
 
 import com.google.common.collect.HashBasedTable;
 import com.google.common.collect.Table;
 import com.google.common.collect.Table.Cell;
 
 /**
  * Holds the currently selected data sets and stratifications.
  *
  * @author Marc Streit
  *
  */
 public class DataMappingState {
 
 	// /** All considered table perspectives */
 	// private List<TablePerspective> mappedTablePerspectives = new ArrayList<TablePerspective>();
 	//
 	// /**
 	// * Table perspectives that are non-genetic, but provide contextual information. Shown at top of Enroute.
 	// */
 	// private List<TablePerspective> contextualTablePerspectives = new ArrayList<TablePerspective>();
 
 	/** The the perspective that gets displayed. It represents a subsets of the groups in {@link #sourcePerspective}. */
 	private Perspective selectedPerspective;
 
 	/**
 	 * The perspective {@link #selectedPerspective} is based on.
 	 */
 	private Perspective sourcePerspective;
 
 	private TablePerspective pathwayMappedTablePerspective;
 
 	private Map<ATableBasedDataDomain, TablePerspective> hashDDToTablePerspective = new LinkedHashMap<ATableBasedDataDomain, TablePerspective>();
 
 	private Table<ATableBasedDataDomain, Perspective, TablePerspective> contextualTablePerspectivesTable = HashBasedTable
 			.create();
 
 	private final String eventSpace;
 
 	private final GLEntourage entourage;
 
 	/**
 	 * ID category that is used for the stratified experimental data.
 	 */
 	private IDCategory experimentalDataIDCategory;
 
 	public DataMappingState(GLEntourage entourage) {
 		this.entourage = entourage;
 		this.eventSpace = entourage.getPathEventSpace();
 		applyDefaultState();
 	}
 
 	private void applyDefaultState() {
 
 		List<ATableBasedDataDomain> dataDomains = DataDomainManager.get().getDataDomainsByType(
 				ATableBasedDataDomain.class);
 		if (dataDomains == null || dataDomains.isEmpty())
 			return;
 
 		Collections.sort(dataDomains, new Comparator<ATableBasedDataDomain>() {
 
 			@Override
 			public int compare(ATableBasedDataDomain dd1, ATableBasedDataDomain dd2) {
 				return dd1.getLabel().compareTo(dd2.getLabel());
 			}
 		});
 		GeneticDataDomain geneticDataDomain = null;
 		for (ATableBasedDataDomain dd : dataDomains) {
 			if (geneticDataDomain == null && dd instanceof GeneticDataDomain) {
 				geneticDataDomain = (GeneticDataDomain) dd;
 				break;
 			}
 		}
 
 		if (geneticDataDomain != null) {
 			IDCategory geneIDCategory = geneticDataDomain.getGeneIDType().getIDCategory();
 			if (geneticDataDomain.getRecordIDCategory() == geneIDCategory) {
 				sourcePerspective = geneticDataDomain.getTable().getDefaultDimensionPerspective(false);
 				setSelectedPerspective(sourcePerspective);
 				experimentalDataIDCategory = geneticDataDomain.getDimensionIDCategory();
 			} else {
 				sourcePerspective = geneticDataDomain.getTable().getDefaultRecordPerspective(false);
 				setSelectedPerspective(sourcePerspective);
 				experimentalDataIDCategory = geneticDataDomain.getRecordIDCategory();
 			}
 			addDataDomain(geneticDataDomain);
 		} else {
 			ATableBasedDataDomain dd = dataDomains.get(0);
 			sourcePerspective = dd.getTable().getDefaultRecordPerspective(false);
 			setSelectedPerspective(sourcePerspective);
 			experimentalDataIDCategory = dd.getRecordIDCategory();
 			addDataDomain(dd);
 		}
 	}
 
 	public List<TablePerspective> getTablePerspectives() {
 		return new ArrayList<>(hashDDToTablePerspective.values());
 	}
 
 	public void addDataDomain(ATableBasedDataDomain dd) {
 		if (hashDDToTablePerspective.containsKey(dd))
 			return;
 
 		addTablePerspective(dd);
 	}
 
 	private void addTablePerspective(ATableBasedDataDomain dd) {
 		TablePerspective tablePerspective = createTablePerspective(dd, selectedPerspective);
 		hashDDToTablePerspective.put(dd, tablePerspective);
 
 		triggerAddTablePerspectiveEvents(tablePerspective);
 	}
 
 	public void addContextualTablePerspective(ATableBasedDataDomain dd, Perspective dimensionPerspective) {
 
 		TablePerspective tablePerspective = createTablePerspective(dd, dimensionPerspective, selectedPerspective);
 		contextualTablePerspectivesTable.put(dd, dimensionPerspective, tablePerspective);
 
 		triggerAddTablePerspectiveEvents(tablePerspective);
 	}
 
 	private void triggerAddTablePerspectiveEvents(TablePerspective tablePerspective) {
 
 		AddTablePerspectivesEvent event = new AddTablePerspectivesEvent(tablePerspective);
 		event.setEventSpace(eventSpace);
 		event.setSender(this);
 		EventPublisher.trigger(event);
 		triggerTablePerspectivesChangedEvent();
 	}
 
 	private void triggerRemoveTablePerspectiveEvents(TablePerspective tablePerspective) {
 
 		RemoveTablePerspectiveEvent event = new RemoveTablePerspectiveEvent(tablePerspective);
 		event.setEventSpace(eventSpace);
 		event.setSender(this);
 		EventPublisher.trigger(event);
 		triggerTablePerspectivesChangedEvent();
 	}
 
 	private void triggerTablePerspectivesChangedEvent() {
 		TablePerspectivesChangedEvent e = new TablePerspectivesChangedEvent(entourage);
 		e.setSender(this);
 		EventPublisher.trigger(e);
 	}
 
 	// public void addTablePerspective(TablePerspective tablePerspective) {
 	// mappedTablePerspectives.add(tablePerspective);
 	// hashDDToTablePerspective.put(tablePerspective.getDataDomain(), tablePerspective);
 	//
 	// AddTablePerspectivesEvent event = new AddTablePerspectivesEvent(tablePerspective);
 	// event.setEventSpace(eventSpace);
 	// event.setSender(this);
 	// EventPublisher.trigger(event);
 	// TablePerspectivesChangedEvent e = new TablePerspectivesChangedEvent(entourage);
 	// e.setSender(this);
 	// EventPublisher.trigger(e);
 	// }
 
 	private TablePerspective createTablePerspective(ATableBasedDataDomain dd, Perspective foreignRecordPerspective) {
 		if (dd == null || foreignRecordPerspective == null)
 			return null;
 		return createTablePerspective(dd, dd.getTable().getDefaultDimensionPerspective(false), foreignRecordPerspective);
 	}
 
 	private TablePerspective createTablePerspective(ATableBasedDataDomain dd, Perspective ownDimensionPerspective,
 			Perspective foreignRecordPerspective) {
 		if (dd == null || ownDimensionPerspective == null || foreignRecordPerspective == null)
 			return null;
 		Perspective convertedPerspective = dd.convertForeignPerspective(foreignRecordPerspective);
 		TablePerspective tablePerspective = new TablePerspective(dd, convertedPerspective, ownDimensionPerspective);
 		tablePerspective.setLabel(dd.getLabel() + " - " + foreignRecordPerspective.getLabel());
 		return tablePerspective;
 	}
 
 	public void removeDataDomain(ATableBasedDataDomain dd) {
 
 		if (!hashDDToTablePerspective.containsKey(dd))
 			return;
 
 		triggerRemoveTablePerspectiveEvents(hashDDToTablePerspective.get(dd));
 		hashDDToTablePerspective.remove(dd);
 	}
 
 	public void removeContextualTablePerspective(ATableBasedDataDomain dd, Perspective dimensionPerspective) {
 
 		TablePerspective tablePerspective = contextualTablePerspectivesTable.remove(dd, dimensionPerspective);
		triggerAddTablePerspectiveEvents(tablePerspective);
 	}
 
 	/** Removes a previous and sets the new perspective on the event space */
 	public void setSelectedPerspective(Perspective perspective) {
 		if (this.selectedPerspective == perspective)
 			return;
 		selectedPerspective = perspective;
 
 		ClearGroupSelectionEvent clearEvent = new ClearGroupSelectionEvent();
 		EventPublisher.trigger(clearEvent);
 
 		for (TablePerspective tablePerspective : getTablePerspectives()) {
 			triggerRemoveTablePerspectiveEvents(tablePerspective);
 		}
 		for (TablePerspective tablePerspective : getContextualTablePerspectives()) {
 			triggerRemoveTablePerspectiveEvents(tablePerspective);
 		}
 
 		if (selectedPerspective != null) {
 			for (ATableBasedDataDomain dd : hashDDToTablePerspective.keySet()) {
 				addTablePerspective(dd);
 			}
 			for (ATableBasedDataDomain dd : contextualTablePerspectivesTable.rowKeySet()) {
 				for (Entry<Perspective, TablePerspective> entry : contextualTablePerspectivesTable.row(dd).entrySet()) {
 					if (entry.getValue() != null)
 						addContextualTablePerspective(dd, entry.getKey());
 				}
 			}
 		}
 
 		// also update pathway mapping
 		if (pathwayMappedTablePerspective != null) {
 			setPathwayMappedDataDomain(pathwayMappedTablePerspective.getDataDomain());
 		}
 	}
 
 	public List<TablePerspective> getContextualTablePerspectives() {
 		List<TablePerspective> contextualTablePerspectives = new ArrayList<>();
 		for (Cell<ATableBasedDataDomain, Perspective, TablePerspective> cell : contextualTablePerspectivesTable
 				.cellSet()) {
 			TablePerspective tablePerspective = cell.getValue();
 			if (tablePerspective != null)
 				contextualTablePerspectives.add(tablePerspective);
 		}
 		return contextualTablePerspectives;
 	}
 
 	/** Returns all data domains that are currently mapped */
 	public Set<ATableBasedDataDomain> getDataDomains() {
 		return hashDDToTablePerspective.keySet();
 	}
 
 	public TablePerspective getMatchingTablePerspective(ATableBasedDataDomain dd) {
 		return hashDDToTablePerspective.get(dd);
 	}
 
 	public TablePerspective getContextualTablePerspective(ATableBasedDataDomain dd, Perspective dimensionPerspective) {
 		return contextualTablePerspectivesTable.get(dd, dimensionPerspective);
 	}
 
 	/**
 	 * Creates and sets a table perspective based on the provided data domain.
 	 *
 	 * @param dd
 	 */
 	public void setPathwayMappedDataDomain(ATableBasedDataDomain dd) {
 		if (dd == null)
 			setPathwayMappedTablePerspective(null);
 		TablePerspective tablePerspective = createTablePerspective(dd, selectedPerspective);
 		setPathwayMappedTablePerspective(tablePerspective);
 		triggerTablePerspectivesChangedEvent();
 	}
 
 	/**
 	 * @param pathwayMappedTablePerspective
 	 *            setter, see {@link pathwayMappedTablePerspective}
 	 */
 	public void setPathwayMappedTablePerspective(TablePerspective pathwayMappedTablePerspective) {
 		this.pathwayMappedTablePerspective = pathwayMappedTablePerspective;
 		PathwayMappingEvent event = new PathwayMappingEvent(pathwayMappedTablePerspective);
 		event.setEventSpace(eventSpace);
 		EventPublisher.trigger(event);
 	}
 
 	/**
 	 * @return the pathwayMappedTablePerspective, see {@link #pathwayMappedTablePerspective}
 	 */
 	public TablePerspective getPathwayMappedTablePerspective() {
 		return pathwayMappedTablePerspective;
 	}
 
 	/**
 	 * @return the selectedPerspective, see {@link #selectedPerspective}
 	 */
 	public Perspective getSelectedPerspective() {
 		return selectedPerspective;
 	}
 
 	/**
 	 * @param sourcePerspective
 	 *            setter, see {@link sourcePerspective}
 	 */
 	public void setSourcePerspective(Perspective sourcePerspective) {
 		this.sourcePerspective = sourcePerspective;
 	}
 
 	/**
 	 * @return the sourcePerspective, see {@link #sourcePerspective}
 	 */
 	public Perspective getSourcePerspective() {
 		return sourcePerspective;
 	}
 
 	/**
 	 * @return the experimentalDataIDCategory, see {@link #experimentalDataIDCategory}
 	 */
 	public IDCategory getExperimentalDataIDCategory() {
 		return experimentalDataIDCategory;
 	}
 }
