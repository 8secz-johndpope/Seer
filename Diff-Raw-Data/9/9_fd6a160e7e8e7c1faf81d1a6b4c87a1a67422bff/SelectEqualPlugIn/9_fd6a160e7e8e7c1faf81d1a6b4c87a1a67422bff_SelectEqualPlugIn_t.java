 /*
  * OrbisGIS is a GIS application dedicated to scientific spatial simulation.
  * This cross-platform GIS is developed at French IRSTV institute and is able to
  * manipulate and create vector and raster spatial information. OrbisGIS is
  * distributed under GPL 3 license. It is produced by the "Atelier SIG" team of
  * the IRSTV Institute <http://www.irstv.cnrs.fr/> CNRS FR 2488.
  *
  *
  *  Team leader Erwan BOCHER, scientific researcher,
  *
  *  User support leader : Gwendall Petit, geomatic engineer.
  *
  *
  * Copyright (C) 2007 Erwan BOCHER, Fernando GONZALEZ CORTES, Thomas LEDUC
  *
  * Copyright (C) 2010 Erwan BOCHER, Pierre-Yves FADET, Alexis GUEGANNO, Maxence LAURENT
  *
  * This file is part of OrbisGIS.
  *
  * OrbisGIS is free software: you can redistribute it and/or modify it under the
  * terms of the GNU General Public License as published by the Free Software
  * Foundation, either version 3 of the License, or (at your option) any later
  * version.
  *
  * OrbisGIS is distributed in the hope that it will be useful, but WITHOUT ANY
  * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
  * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License along with
  * OrbisGIS. If not, see <http://www.gnu.org/licenses/>.
  *
  * For more information, please consult: <http://www.orbisgis.org/>
  *
  * or contact directly:
  * erwan.bocher _at_ ec-nantes.fr
  * gwendall.petit _at_ ec-nantes.fr
  */
 package org.orbisgis.core.ui.plugins.editors.tableEditor;
 
 import java.util.ArrayList;
 
 import org.gdms.data.DataSource;
 import org.gdms.data.values.Value;
 import org.gdms.driver.DriverException;
 import org.orbisgis.core.Services;
 import org.orbisgis.core.background.BackgroundJob;
 import org.orbisgis.core.background.BackgroundManager;
 import org.orbisgis.core.ui.editor.IEditor;
 import org.orbisgis.core.ui.editors.table.TableComponent;
 import org.orbisgis.core.ui.editors.table.TableEditableElement;
 import org.orbisgis.core.ui.pluginSystem.AbstractPlugIn;
 import org.orbisgis.core.ui.pluginSystem.PlugInContext;
 import org.orbisgis.core.ui.pluginSystem.message.ErrorMessages;
 import org.orbisgis.core.ui.pluginSystem.workbench.Names;
 import org.orbisgis.core.ui.pluginSystem.workbench.WorkbenchContext;
 import org.orbisgis.core.ui.pluginSystem.workbench.WorkbenchFrame;
 import org.orbisgis.core.ui.plugins.views.tableEditor.TableEditorPlugIn;
 import org.orbisgis.core.ui.preferences.lookandfeel.OrbisGISIcon;
 import org.orbisgis.progress.ProgressMonitor;
 import org.orbisgis.utils.I18N;
 
 public class SelectEqualPlugIn extends AbstractPlugIn {
 
         @Override
         public boolean execute(PlugInContext context) throws Exception {
                 IEditor editor = context.getActiveEditor();
                 final TableEditableElement element = (TableEditableElement) editor.getElement();
                 // We must retrieve the indices of the element we have selected.
                 // We must be careful, as the rowIndex we retrieve is no the one
                 // we want to use to retrieve the data in the data source.
                 TableComponent currentTable = (TableComponent) editor.getView().getComponent();
                 final int rowIndex = currentTable.getTable().rowAtPoint(
                         getEvent().getPoint());
                 final int dbRowIndex = currentTable.getRowIndex(rowIndex);
                 final int columnIndex = currentTable.getTable().columnAtPoint(
                         getEvent().getPoint());
                 BackgroundManager bm = Services.getService(BackgroundManager.class);
                 bm.backgroundOperation(new BackgroundJob() {
 
                         @Override
                         public void run(ProgressMonitor pm) {
 
                                 try {
                                         DataSource dataSource = element.getDataSource();
                                         ArrayList<Integer> newSel = new ArrayList<Integer>();
                                         Value ref = dataSource.getFieldValue(dbRowIndex,
                                                 columnIndex);
                                         for (int i = 0; i < dataSource.getRowCount(); i++) {
                                                 Value value = dataSource.getFieldValue(i, columnIndex);
                                                Value comp = value.equals(ref);
                                                if (!comp.isNull()) {
                                                        if (comp.getAsBoolean()) {
                                                                 newSel.add(i);
                                                         }
                                                 }
                                         }
                                         int[] sel = new int[newSel.size()];
                                         for (int i = 0; i < sel.length; i++) {
                                                 sel[i] = newSel.get(i);
                                         }
 
                                         element.getSelection().setSelectedRows(sel);
                                 } catch (DriverException e) {
                                         ErrorMessages.error(ErrorMessages.CannotReadSource, e);
                                 }
                         }
 
                         @Override
                         public String getTaskName() {
                                 return I18N.getString("orbisgis.org.orbisgis.core.ui.plugins.editors.tableEditor.findMatch");
                         }
                 });
                 return true;
         }
 
         @Override
         public void initialize(PlugInContext context) throws Exception {
                 WorkbenchContext wbContext = context.getWorkbenchContext();
                 WorkbenchFrame frame = wbContext.getWorkbench().getFrame().getTableEditor();
                 context.getFeatureInstaller().addPopupMenuItem(frame, this,
                         new String[]{Names.POPUP_TABLE_EQUALS_PATH1},
                         Names.POPUP_TABLE_EQUALS_GROUP, false,
                         OrbisGISIcon.TABLE_SELECT_SAME_VALUE, wbContext);
         }
 
         @Override
         public boolean isEnabled() {
 
                 TableEditorPlugIn table = getPlugInContext().getTableEditor();
                 if (table != null && getSelectedColumn() == -1 && getEvent() != null) {
                         int rowCountSelected = ((TableComponent) table.getView().getComponent()).getTable().getSelectedRowCount();
                         if (rowCountSelected == 1) {
                                 return true;
                         }
 
                         return false;
                 }
                 return false;
         }
 }
