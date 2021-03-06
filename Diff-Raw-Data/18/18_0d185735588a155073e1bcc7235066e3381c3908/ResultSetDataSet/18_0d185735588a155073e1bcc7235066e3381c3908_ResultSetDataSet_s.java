 package net.sourceforge.squirrel_sql.fw.datasetviewer;
 /*
  * Copyright (C) 2001-2003 Colin Bell
  * colbell@users.sourceforge.net
  * Copyright (C) 2001-2003 Johan Compagner
  * jcompagner@j-com.nl
  *
  * This library is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public
  * License as published by the Free Software Foundation; either
  * version 2.1 of the License, or (at your option) any later version.
  *
  * This library is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this library; if not, write to the Free Software
  * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
  */
 import java.sql.ResultSet;
 import java.sql.ResultSetMetaData;
 import java.sql.SQLException;
 import java.util.ArrayList;
 import java.util.List;
 
 import net.sourceforge.squirrel_sql.fw.sql.ResultSetReader;
 import net.sourceforge.squirrel_sql.fw.util.IMessageHandler;
 import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
 import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;
 
 public class ResultSetDataSet implements IDataSet
 {
 	private final static ILogger s_log =
 		LoggerController.createLogger(ResultSetDataSet.class);
 
 	// TODO: These 2 should be handled with an Iterator.
 	private int _iCurrent = -1;
 	private Object[] _currentRow;
 
 	private int _columnCount;
 	private DataSetDefinition _dataSetDefinition;
 	private List _alData;
 
 	/** If <TT>true</TT> cancel has been requested. */
 	private boolean _cancel = false;
 
 	public ResultSetDataSet()
 	{
 		super();
 	}
 
 
 	/**
 	 * Form used by Tabs other than ContentsTab
 	 */
 	public void setResultSet(ResultSet rs)
 		throws DataSetException
 	{
  		setResultSet(rs, null, false);
 	}
 	
 	/**
	 * Form used by ContentsTab
 	 */
 	public void setContentsTabResultSet(ResultSet rs, 
 		String fullTableName)
 		throws DataSetException
 	{
 			setResultSet(rs, fullTableName, null, false, true);
 	}
 
 	public void setResultSet(ResultSet rs, int[] columnIndices)
 		throws DataSetException
 	{
  		setResultSet(rs, columnIndices, false);
 	}
 
 
 	/**
 	 * External method to read the contents of a ResultSet that is used by
 	 * all Tab classes except ContentsTab.  This tunrs all the data into strings
 	 * for simplicity of operation.
 	 */
 	public void setResultSet(ResultSet rs,
  				 int[] columnIndices, boolean computeWidths)
  			throws DataSetException
 	{
 		setResultSet(rs, null, columnIndices, computeWidths, false);
 	}
 
 
 	/**
 	 * Internal method to read the contents of a ResultSet that is used by
 	 * all Tab classes
 	 */
 	private void setResultSet(ResultSet rs, String fullTableName, 
 				 int[] columnIndices, boolean computeWidths, boolean isContentsTab)
  			throws DataSetException
 	{
 		reset();
 
 		if (columnIndices != null && columnIndices.length == 0)
 		{
 			columnIndices = null;
 		}
 		_iCurrent = -1;
 		_alData = new ArrayList();
 
 		if (rs != null)
 		{
 			try
 			{
 				ResultSetMetaData md = rs.getMetaData();
  				_columnCount = columnIndices != null ? columnIndices.length : md.getColumnCount();
 
 				// Done before actually reading the data from the ResultSet. If done after
 				// reading the data from the ResultSet Oracle throws a NullPointerException
 				// when processing ResultSetMetaData methods for the ResultSet returned for
 				// DatabasemetaData.getExportedKeys.
 				ColumnDisplayDefinition[] colDefs =
 					createColumnDefinitions(md, fullTableName, columnIndices, computeWidths);
 				_dataSetDefinition = new DataSetDefinition(colDefs);
 
  				// Read the entire row, since some drivers complain if columns are read out of sequence
  				ResultSetReader rdr = new ResultSetReader(rs, null);
 				Object[] row = null;
 
 				while (true) {
					if (isContentsTab)
 						row = rdr.readRow(colDefs);
 					else
 						row = rdr.readRow();
 		
 					if (row == null)
 						break;
 		
 					if (_cancel)
 					{
 						return;
 					}
 
  					// SS: now select/reorder columns
  					if (columnIndices != null)
 					{
  						Object[] newRow = new Object[_columnCount];
  						for (int i = 0; i < _columnCount; i++)
 						{
 							if (columnIndices[i] - 1 < row.length)
 							{
  								newRow[i] = row[columnIndices[i] - 1];
 							}
 							else
 							{
 								newRow[i] = "Unknown";
 							}
  						}
  						row = newRow;
  					}
 					_alData.add(row);
 				}
  
 // 				ColumnDisplayDefinition[] colDefs = createColumnDefinitions(md, columnIndices, computeWidths);
 // 				_dataSetDefinition = new DataSetDefinition(colDefs);
 			}
 			catch (SQLException ex)
 			{
 				s_log.error("Error reading ResultSet", ex);
 				throw new DataSetException(ex);
 			}
 		}
 	}
 
 	public final int getColumnCount()
 	{
 		return _columnCount;
 	}
 
 	public DataSetDefinition getDataSetDefinition()
 	{
 		return _dataSetDefinition;
 	}
 
 	public synchronized boolean next(IMessageHandler msgHandler)
 		throws DataSetException
 	{
 		// TODO: This should be handled with an Iterator
 		if (++_iCurrent < _alData.size())
 		{
 			_currentRow = (Object[])_alData.get(_iCurrent);
 			return true;
 		}
 		return false;
 	}
 
 	public Object get(int columnIndex)
 	{
 		return _currentRow[columnIndex];
 	}
 
 	public void cancelProcessing()
 	{
 		_cancel = true;
 	}
 
  	// SS: Modified to auto-compute column widths if <computeWidths> is true
 	private ColumnDisplayDefinition[] createColumnDefinitions(ResultSetMetaData md,
  			String fullTableName, int[] columnIndices, boolean computeWidths) throws SQLException
 	{
 		// TODO?? ColumnDisplayDefinition should also have the Type (String, Date, Double,Integer,Boolean)
  		int[] colWidths = null;
  
  		// SS: update dynamic column widths
  		if (computeWidths) {
  			colWidths = new int[_columnCount];
  			for (int i = 0; i < _alData.size(); i++) {
  				Object[] row = (Object[])_alData.get(i);
  				for (int col = 0; i < _columnCount; i++) {
  					if (row[col] != null) {
  						int colWidth = row[col].toString().length();
  						if (colWidth > colWidths[col]) {
  							colWidths[col] = colWidth + 2;
  						}
  					}
  				}
  			}
  		}
 
 		ColumnDisplayDefinition[] columnDefs =
 			new ColumnDisplayDefinition[_columnCount];
 		for (int i = 0; i < _columnCount; ++i)
 		{
 			int idx = columnIndices != null ? columnIndices[i] : i + 1;
 
 			// save various info about the column for use in user input validation
 			// when editing table contents.
 			// Note that the columnDisplaySize is included two times, where the first
 			// entry may be adjusted for actual display while the second entry is the
 			// size expected by the DB.
 			// The isNullable() method returns three values that we convert into two
 			// by saying that if it is not known whether or not a column allows nulls,
 			// we will allow the user to enter nulls and any problems will be caught
 			// when they try to save the data to the DB
 			boolean isNullable = true;
 			if (md.isNullable(idx) == ResultSetMetaData.columnNoNulls)
 				isNullable = false;
 			
 			int precis;
 			try {
 				precis = md.getPrecision(idx);
 			}
 			catch (NumberFormatException ignore) {
 				precis = Integer.MAX_VALUE;	// Oracle throws this ex on BLOB data types
 			}
 
 			boolean isSigned = true;
 			try
 			{
 				isSigned = md.isSigned(idx); // HSQLDB 1.7.1 throws error.
 			}
 			catch (SQLException ignore)
 			{
 				// Empty block
 			}
 
 			columnDefs[i] =
  					new ColumnDisplayDefinition(
  					computeWidths ? colWidths[i] : md.getColumnDisplaySize(idx),
 					fullTableName+":"+md.getColumnLabel(idx),
  					md.getColumnLabel(idx),
  					md.getColumnType(idx),
  					md.getColumnTypeName(idx),
  					isNullable,
  					md.getColumnDisplaySize(idx),
  					precis,
  					md.getScale(idx),
  					isSigned,
  					md.isCurrency(idx));
 		}
 		return columnDefs;
 	}
 
 	private void reset()
 	{
 		_iCurrent = -1;
 		_currentRow = null;
 		_columnCount = 0;
 		_dataSetDefinition = null;
 		_alData = null;
 	}
 }
