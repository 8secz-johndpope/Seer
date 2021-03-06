 /* FormulaEditor.java
 
 {{IS_NOTE
 	Purpose:
 		
 	Description:
 		
 	History:
 		Nov 15, 2010 4:28:32 PM , Created by Sam
 }}IS_NOTE
 
 Copyright (C) 2009 Potix Corporation. All Rights Reserved.
 
 */
 package org.zkoss.zss.app.zul;
 
 import org.zkoss.poi.ss.usermodel.Cell;
 import org.zkoss.poi.ss.util.CellRangeAddress;
 import org.zkoss.zk.ui.event.Event;
 import org.zkoss.zk.ui.event.EventListener;
 import org.zkoss.zk.ui.event.InputEvent;
 import org.zkoss.zss.app.cell.EditHelper;
 import org.zkoss.zss.app.zul.ctrl.DesktopWorkbenchContext;
 import org.zkoss.zss.model.Ranges;
 import org.zkoss.zss.model.Worksheet;
 import org.zkoss.zss.model.impl.SheetCtrl;
 import org.zkoss.zss.ui.Position;
 import org.zkoss.zss.ui.Spreadsheet;
 import org.zkoss.zss.ui.event.CellEvent;
 import org.zkoss.zss.ui.event.EditboxEditingEvent;
 import org.zkoss.zss.ui.event.Events;
 import org.zkoss.zss.ui.event.StopEditingEvent;
 import org.zkoss.zss.ui.impl.Utils;
 import org.zkoss.zul.Textbox;
 
 /**
  * @author Sam
  *
  */
 public class FormulaEditor extends Textbox implements ZssappComponent {
 
 	private Spreadsheet ss;
 	
 	private String oldEdit;
 	private String oldText;
 	private String newEdit;
 	
 	private boolean everFocusCell = false;
 	private boolean focusOut = false;
 	private Cell currentEditcell;
 	
 	public FormulaEditor() {
 		setCols(100);
 	}
 	
 	public void onChanging(InputEvent event) {
 		if (currentEditcell == null) {
 			final int left = ss.getSelection().getLeft();
 			final int top = ss.getSelection().getTop();
 			final Worksheet sheet = ss.getSelectedSheet();
 			currentEditcell = Utils.getOrCreateCell(sheet, top, left);
 		}
 		ss.updateText(currentEditcell, ((InputEvent) event).getValue());
 	}
 
 	public void onCancel() {
 		focusOut = true;
 		recoverEditorText();
 		recoverCellText();
 		int row = ss.getSelection().getTop();
 		int col = ss.getSelection().getLeft();
 		ss.focusTo(row, col);
 	}
 	
 	public void onFocus() {
		Worksheet sheet = ss.getSelectedSheet();
		if (sheet == null) { //no sheet, no operation
			return;
		}
 		newEdit = null;
 		everFocusCell = false;
 		focusOut = false;
 		int left = ss.getSelection().getLeft();
 		int top = ss.getSelection().getTop();
 		currentEditcell = Utils.getCell(sheet, top, left);
 		
 		if (currentEditcell != null) {
 			oldEdit = Ranges.range(sheet, top, left).getEditText();
 			oldText = Utils.getCellText(sheet, currentEditcell); //escaped HTML to show cell value
 			ss.updateText(currentEditcell, oldEdit);
 		}
 		
 		EditHelper.clearCutOrCopy(ss);
 	}
 	
 	public void onChange(Event event) {
 		newEdit = ((InputEvent)event).getValue(); //remember the changed value
 	}
 	
 	private void handleCellText() {
 		if (newEdit == null) { //no change
 			recoverCellText(); //recover cell text
 		} else if (currentEditcell != null){
 			Utils.setEditText(ss.getSelectedSheet(), 
 					currentEditcell.getRowIndex(), currentEditcell.getColumnIndex(), newEdit);
 		}
 	}
 	
 	public void onBlur() {
 		if (focusOut) { //onChange, onOK, or onCancel already done everything!
 			return;
 		}
 		focusOut = true;
 		
 		handleCellText();
 		
 		Position pos = ss.getCellFocus();
 		int row = pos.getRow();
 		int col = pos.getColumn();
 		int oldrow = currentEditcell == null ? -1 : currentEditcell.getRowIndex();
 		int oldcol = currentEditcell == null ? -1 : currentEditcell.getColumnIndex();
 		final boolean focusChanged = (row != oldrow || col != oldcol); //user click directly to a different cell
 		if (!focusChanged) {
 			if (!everFocusCell) { //Tab key
 				final Worksheet sheet = ss.getSelectedSheet();
 				final CellRangeAddress merged = sheet != null ? ((SheetCtrl)sheet).getMerged(row, col) : null;
 				col = merged == null ? col + 1 : merged.getLastColumn() + 1;
 				if (ss.getMaxcolumns() <= col) {
 					col = ss.getMaxcolumns() - 1;
 				}
 				ss.focusTo(row, col);
 				org.zkoss.zk.ui.event.Events.sendEvent(new CellEvent(Events.ON_CELL_FOUCSED, ss, sheet, row, col));
 				getDesktopWorkbenchContext().getWorkbookCtrl().reGainFocus();
 			} else { //click on the same cell, shall enter edit mode, something like press F2
 				//TODO click on the same cell, shall enter edit mode, something like press F2
 			}
 		}
 	}
 
 	private void recoverEditorText() {
 		setText(oldEdit);
 	}
 	private void recoverCellText() {
 		if (oldText != null && currentEditcell != null) {
 			ss.updateText(currentEditcell, oldText);
 		}
 	}
 	public void onOK() {
 		focusOut = true;
 		
 		handleCellText();
 		
 		//move cell focus
 		Position pos = ss.getCellFocus();
 		int row = pos.getRow() + 1;
 		int col = pos.getColumn();
 		if (ss.getMaxrows() <= row) {
 			row = ss.getMaxrows() - 1;
 		}
 		ss.focusTo(row, col);
 		final Worksheet sheet = ss.getSelectedSheet();
 		org.zkoss.zk.ui.event.Events.sendEvent(new CellEvent(Events.ON_CELL_FOUCSED, ss, sheet, row, col));
 		getDesktopWorkbenchContext().getWorkbookCtrl().reGainFocus();
 	}
 
 	@Override
 	public void bindSpreadsheet(Spreadsheet spreadsheet) {
 		ss = spreadsheet;
 		
 		setWidgetListener("onFocus", "this.$f('" + ss.getId() + "', true).focus(false);");
 		
 		ss.addEventListener(Events.ON_CELL_FOUCSED, new EventListener() {
 
 			@Override
 			public void onEvent(Event event) throws Exception {
 				everFocusCell = true;
 				// TODO Auto-generated method stub
 				CellEvent evt = (CellEvent)event;
 				int row = evt.getRow();
 				int col = evt.getColumn();
 				Cell cell = Utils.getCell(ss.getSelectedSheet(), row, col);
 				String editText = Utils.getEditText(cell);
 				setText(cell == null ? "" : (editText == null ? "" : editText));
 			}
 			
 		});
 		ss.addEventListener(Events.ON_STOP_EDITING,
 				new EventListener() {
 					public void onEvent(Event event) throws Exception {
 						StopEditingEvent evt = (StopEditingEvent)event;
 						
 						setText((String) evt.getEditingValue());
 
 						//chart not implement yet
 //						// to notify all widgets there is a cell changed
 //						for (int i = 0; i < chartKey; i++) {
 //							try {
 //								Window win = (Window) mainWin.getFellow("chartWin" + i);
 //								if (win != null) {
 //									Chart myChart = (Chart) win.getFellow("myChart");
 //									CellEvent event = new StopEditingEvent(
 //											org.zkoss.zss.ui.event.Events.ON_STOP_EDITING,
 //											myChart, evt.getSheet(), evt.getRow(), evt
 //													.getColumn(), (String) evt.getData());
 //									org.zkoss.zk.ui.event.Events.postEvent(event);
 //								}
 //							} catch (Exception e) {
 //								// the chart may be deleted
 //							}
 //						}
 					}
 
 				});
 		ss.addEventListener(Events.ON_EDITBOX_EDITING,
 				new EventListener() {
 			public void onEvent(Event event) throws Exception {
 				EditboxEditingEvent evt = (EditboxEditingEvent)event;
 				setText((String) evt.getEditingValue());
 			}
 		});
 	}
 
 	@Override
 	public void unbindSpreadsheet() {
 		// TODO Auto-generated method stub
 		
 	}
 
 	private DesktopWorkbenchContext getDesktopWorkbenchContext() {
 		return Zssapp.getDesktopWorkbenchContext(this);
 	}
 }
