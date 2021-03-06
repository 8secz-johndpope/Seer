 /*******************************************************************************
  * Caleydo - visualization for molecular biology - http://caleydo.org
  *
  * Copyright(C) 2005, 2012 Graz University of Technology, Marc Streit, Alexander Lex, Christian Partl, Johannes Kepler
  * University Linz </p>
  *
  * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
  * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
  * version.
  *
  * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
  * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License along with this program. If not, see
  * <http://www.gnu.org/licenses/>
  *******************************************************************************/
 package org.caleydo.view.info.dataset;
 
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.text.DateFormat;
 import java.util.Locale;
 
 import javax.xml.bind.JAXBContext;
 import javax.xml.bind.JAXBException;
 
 import org.caleydo.core.data.datadomain.ATableBasedDataDomain;
 import org.caleydo.core.data.datadomain.DataDomainManager;
 import org.caleydo.core.data.datadomain.IDataDomain;
 import org.caleydo.core.event.EventListenerManager;
 import org.caleydo.core.event.EventListenerManager.ListenTo;
 import org.caleydo.core.event.EventListenerManagers;
 import org.caleydo.core.event.data.DataDomainUpdateEvent;
 import org.caleydo.core.manager.GeneralManager;
 import org.caleydo.core.serialize.ASerializedSingleTablePerspectiveBasedView;
 import org.caleydo.core.serialize.ProjectMetaData;
 import org.caleydo.core.util.system.BrowserUtils;
 import org.caleydo.core.view.CaleydoRCPViewPart;
 import org.caleydo.core.view.IDataDomainBasedView;
 import org.caleydo.view.histogram.GLHistogram;
 import org.caleydo.view.histogram.RcpGLColorMapperHistogramView;
 import org.caleydo.view.histogram.SerializedHistogramView;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.layout.FillLayout;
 import org.eclipse.swt.layout.GridData;
 import org.eclipse.swt.layout.GridLayout;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.ExpandBar;
 import org.eclipse.swt.widgets.ExpandItem;
 import org.eclipse.swt.widgets.Label;
 import org.eclipse.swt.widgets.Link;
 
 /**
  * Data meta view showing details about a data table.
  *
  * @author Marc Streit
  */
 public class RcpDatasetInfoView extends CaleydoRCPViewPart implements IDataDomainBasedView<IDataDomain> {
 
 	public static final String VIEW_TYPE = "org.caleydo.view.info.dataset";
 
 	private IDataDomain dataDomain;
 
 	private ExpandItem dataSetItem;
 	private Label recordLabel;
 	private Label recordCount;
 	private Label dimensionLabel;
 	private Label dimensionCount;
 
 	private ExpandItem histogramItem;
 	private RcpGLColorMapperHistogramView histogramView;
 
 	private final EventListenerManager listeners = EventListenerManagers.wrap(this);
 
 
 	/**
 	 * Constructor.
 	 */
 	public RcpDatasetInfoView() {
 		super();
 
 		eventPublisher = GeneralManager.get().getEventPublisher();
 		isSupportView = true;
 
 		try {
 			viewContext = JAXBContext.newInstance(SerializedDatasetInfoView.class);
 		} catch (JAXBException ex) {
 			throw new RuntimeException("Could not create JAXBContext", ex);
 		}
 	}
 
 	@Override
 	public void createPartControl(Composite parent) {
 		ExpandBar expandBar = new ExpandBar(parent, SWT.V_SCROLL | SWT.NO_BACKGROUND);
 		expandBar.setSpacing(1);
 		expandBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
 
 		parentComposite = expandBar;
 
 		createProjectInfos(expandBar);
 		createDataSetInfos(expandBar);
 		createHistogramInfos(expandBar);
 
 		if (dataDomain == null) {
 			setDataDomain(DataDomainManager.get().getDataDomainByID(
 					((ASerializedSingleTablePerspectiveBasedView) serializedView).getDataDomainID()));
 		}
 
 		parent.layout();
 	}
 
 
 	private void createDataSetInfos(ExpandBar expandBar) {
 		this.dataSetItem = new ExpandItem(expandBar, SWT.NONE);
 		dataSetItem.setText("Data Set: <no selection>");
 		Composite c = new Composite(expandBar, SWT.NONE);
 		c.setLayout(new GridLayout(2, false));
 
 		recordLabel = new Label(c, SWT.NONE);
 		recordLabel.setText("");
 		recordLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
 		recordCount = new Label(c, SWT.NONE);
 		recordCount.setText("");
 		recordCount.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
 
 		dimensionLabel = new Label(c, SWT.NONE);
 		dimensionLabel.setText("");
 		dimensionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
 		dimensionCount = new Label(c, SWT.NONE);
 		dimensionCount.setText("");
 		dimensionCount.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
 
 		dataSetItem.setControl(c);
 		dataSetItem.setHeight(c.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
 		dataSetItem.setExpanded(false);
 	}
 
 	private void createProjectInfos(ExpandBar expandBar) {
 		ProjectMetaData metaData = GeneralManager.get().getMetaData();
 		if (metaData.keys().isEmpty())
 			return;
 		ExpandItem expandItem = new ExpandItem(expandBar, SWT.NONE);
 		expandItem.setText("Project: " + metaData.getName());
 		Composite g = new Composite(expandBar, SWT.NONE);
 		g.setLayout(new GridLayout(2, false));
 		createLine(
 				g,
 				"Creation Date",
 				DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.ENGLISH).format(
 						metaData.getCreationDate()));
 		for (String key : metaData.keys()) {
 			createLine(g, key, metaData.get(key));
 		}
 
 		expandItem.setControl(g);
 		expandItem.setHeight(g.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
 		expandItem.setExpanded(true);
 	}
 
 	private void createHistogramInfos(ExpandBar expandBar) {
 		histogramItem = new ExpandItem(expandBar, SWT.NONE);
 		histogramItem.setText("Histogram");
 		histogramItem.setHeight(200);
 		Composite wrapper = new Composite(expandBar, SWT.NONE);
 		wrapper.setLayout(new FillLayout());
 		histogramItem.setControl(wrapper);
 
 		histogramItem.setExpanded(false);
 		histogramItem.getControl().setEnabled(false);
 	}
 
 	private void createLine(Composite parent, String label, String value) {
 		if (label == null || label.trim().isEmpty() || value == null || value.trim().isEmpty())
 			return;
 		Label l = new Label(parent, SWT.NO_BACKGROUND);
 		l.setText(label + ":");
 		l.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
 
 		try {
 			final URL url = new URL(value);
 			Link v = new Link(parent, SWT.NO_BACKGROUND);
 
 			value = url.toExternalForm();
 			if (value.length() > 20)
				value = value.substring(0, 20 - 3) + "...";
 			v.setText("<a href=\"" + url.toExternalForm() + "\">" + value + "</a>");
 			v.setToolTipText(url.toExternalForm());
 			v.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
 			v.addSelectionListener(BrowserUtils.LINK_LISTENER);
 		} catch (MalformedURLException e) {
 			Label v = new Label(parent, SWT.NO_BACKGROUND);
 			v.setText(value);
 			v.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
 		}
 
 	}
 
 	@Override
 	public void setDataDomain(IDataDomain dataDomain) {
 		// Do nothing if new datadomain is the same as the current one, or if dd
 		// is null
 		if (dataDomain == this.dataDomain || dataDomain == null)
 			return;
 
 		this.dataDomain = dataDomain;
 
 		updateDataSetInfo();
 
 	}
 
 	private void updateDataSetInfo() {
 		dataSetItem.setText("Data Set: " + dataDomain.getLabel());
 
 		if (dataDomain instanceof ATableBasedDataDomain) {
 			ATableBasedDataDomain tableBasedDD = (ATableBasedDataDomain) dataDomain;
 
 			dataSetItem.setExpanded(true);
 
 			recordLabel.setText(tableBasedDD.getRecordDenomination(true, true) + ":");
 			recordCount.setText("" + tableBasedDD.getTable().depth());
 
 			dimensionLabel.setText(tableBasedDD.getDimensionDenomination(true, true) + ":");
 			dimensionCount.setText("" + tableBasedDD.getTable().size());
 
 			((Composite) dataSetItem.getControl()).layout();
 
 			if (!tableBasedDD.getTable().isDataHomogeneous()) {
 				histogramItem.getControl().setEnabled(false);
 				histogramItem.setExpanded(false);
 				return;
 			}
 
 			histogramItem.getControl().setEnabled(true);
 			histogramItem.setExpanded(true);
 
 			if (histogramView == null) {
 				histogramView = new RcpGLColorMapperHistogramView();
 				histogramView.setDataDomain(tableBasedDD);
 				SerializedHistogramView serializedHistogramView = new SerializedHistogramView();
 				serializedHistogramView.setDataDomainID(dataDomain.getDataDomainID());
 				serializedHistogramView
 						.setTablePerspectiveKey(((ASerializedSingleTablePerspectiveBasedView) serializedView)
 								.getTablePerspectiveKey());
 
 				histogramView.setExternalSerializedView(serializedHistogramView);
 				histogramView.createPartControl((Composite) histogramItem.getControl());
 				// Usually the canvas is registered to the GL2 animator in the
 				// PartListener. Because the GL2 histogram is no usual RCP view
 				// we
 				// have to do it on our own
 				GeneralManager.get().getViewManager().registerGLCanvasToAnimator(histogramView.getGLCanvas());
 				((Composite) histogramItem.getControl()).layout();
 			}
 			// else {
 
 			// If the default table perspective does not exist yet, we
 			// create it and set it to private so that it does not show up
 			// in the DVI
 			if (!tableBasedDD.hasTablePerspective(tableBasedDD.getTable().getDefaultRecordPerspective()
 					.getPerspectiveID(), tableBasedDD.getTable().getDefaultDimensionPerspective().getPerspectiveID())) {
 				tableBasedDD.getDefaultTablePerspective().setPrivate(true);
 			}
 			histogramView.setDataDomain(tableBasedDD);
 			((GLHistogram) histogramView.getGLView()).setDataDomain(tableBasedDD);
 			((GLHistogram) histogramView.getGLView()).setDisplayListDirty();
 			// }
 		} else {
 			dataSetItem.setExpanded(true);
 			histogramItem.setExpanded(false);
 			histogramItem.getControl().setEnabled(false);
 		}
 	}
 
 	@Override
 	public IDataDomain getDataDomain() {
 		return dataDomain;
 	}
 
 	@Override
 	public void createDefaultSerializedView() {
 		serializedView = new SerializedDatasetInfoView();
 		determineDataConfiguration(serializedView, false);
 	}
 
 	@Override
 	public void registerEventListeners() {
 		super.registerEventListeners();
 		listeners.register(this);
 	}
 
 	@Override
 	public void unregisterEventListeners() {
 		super.unregisterEventListeners();
 		listeners.unregisterAll();
 	}
 
 	@ListenTo
 	private void onDataDomainUpdate(DataDomainUpdateEvent event) {
 		setDataDomain(event.getDataDomain());
 	}
 
 }
