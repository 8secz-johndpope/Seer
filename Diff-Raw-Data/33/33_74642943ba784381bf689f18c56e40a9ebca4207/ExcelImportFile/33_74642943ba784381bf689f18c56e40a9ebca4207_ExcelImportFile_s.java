 package com.idega.block.importer.data;
 
 import java.io.FileInputStream;
 import java.io.FileNotFoundException;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Iterator;
 
 import org.apache.poi.hssf.usermodel.HSSFCell;
 import org.apache.poi.hssf.usermodel.HSSFRow;
 import org.apache.poi.hssf.usermodel.HSSFSheet;
 import org.apache.poi.hssf.usermodel.HSSFWorkbook;
 
 import com.idega.block.importer.business.NoRecordsException;
 import com.idega.util.CoreConstants;
 import com.idega.util.Timer;
 
 public class ExcelImportFile extends GenericImportFile {
 	
 	private Iterator iter;
 	
 	@Override
 	public Object getNextRecord() {
 		if (iter == null) {
 			Collection records = getAllRecords();
 			if (records != null) {
 				iter = records.iterator();
 			}
 		}
 		
 		if (iter != null) {
 			while (iter.hasNext()) {
 				return iter.next();
 			}
 		}
 		
 		return "";
 	}
 	
 	public Collection getAllRecords() throws NoRecordsException {
 		try {
			FileInputStream input = new FileInputStream(getFile());
 			HSSFWorkbook wb = new HSSFWorkbook(input);
 			HSSFSheet sheet = wb.getSheetAt(0);
 			
 			int records = 0;
 	
 			Timer clock = new Timer();
 			clock.start();
 	
 			StringBuffer buffer = new StringBuffer();
 			ArrayList list = new ArrayList();
 			for (int i = sheet.getFirstRowNum(); i <= sheet.getLastRowNum(); i++) {
 				HSSFRow row = sheet.getRow(i);
 				if (buffer == null) {
 					buffer = new StringBuffer();
 				}
 				
 				if (row != null) {
 					for (short j = row.getFirstCellNum(); j < row.getLastCellNum(); j++) {
 						HSSFCell cell = row.getCell(j);
 						if (cell != null) {
 							if (cell.getCellType() == HSSFCell.CELL_TYPE_STRING) {
 								buffer.append(cell.getStringCellValue());
 							} else if (cell.getCellType() == HSSFCell.CELL_TYPE_NUMERIC) {
 								buffer.append(cell.getNumericCellValue());
 							} else if (cell.getCellType() == HSSFCell.CELL_TYPE_FORMULA) {
 								switch (cell.getCachedFormulaResultType()) {
 									case HSSFCell.CELL_TYPE_NUMERIC: {
 										buffer.append(cell.getNumericCellValue());
 										break;
 									}
 									case HSSFCell.CELL_TYPE_STRING: {
 										buffer.append(cell.getStringCellValue());
 										break;
 									}
 									case HSSFCell.CELL_TYPE_BLANK: {
 										buffer.append(CoreConstants.EMPTY);
 										break;
 									}
 									case HSSFCell.CELL_TYPE_BOOLEAN: {
 										buffer.append(cell.getBooleanCellValue());
 										break;
 									}
 								}
 							} else {
 								buffer.append(cell.getStringCellValue());
 							}
 						}
 						buffer.append(getValueSeparator());
 					}
 					
 					records++;
 					if ((records % 1000) == 0) {
 						System.out.println("Importer: Reading record nr.: " + records + " from file " + getFile().getName());
 					}
 
 					list.add(buffer.toString());
 					buffer = null;
 				}
 			}
 			
 			if (records == 0) {
 				throw new NoRecordsException("No records where found in the selected file" + getFile().getAbsolutePath());
 			}
 	
 			return list;
 		}
 		catch (FileNotFoundException ex) {
 			ex.printStackTrace(System.err);
 			return null;
 		}
 		catch (IOException ex) {
 			ex.printStackTrace(System.err);
 			return null;
 		}
 	}
 }
