 // PathVisio,
 // a tool for data visualization and analysis using Biological Pathways
 // Copyright 2006-2007 BiGCaT Bioinformatics
 //
 // Licensed under the Apache License, Version 2.0 (the "License"); 
 // you may not use this file except in compliance with the License. 
 // You may obtain a copy of the License at 
 // 
 // http://www.apache.org/licenses/LICENSE-2.0 
 //  
 // Unless required by applicable law or agreed to in writing, software 
 // distributed under the License is distributed on an "AS IS" BASIS, 
 // WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 // See the License for the specific language governing permissions and 
 // limitations under the License.
 //
 package org.pathvisio.data;
 
 import java.awt.Color;
 import java.io.BufferedReader;
 import java.io.File;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Map;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 import org.eclipse.jface.dialogs.MessageDialog;
 import org.eclipse.jface.wizard.IWizardPage;
 import org.eclipse.jface.wizard.Wizard;
 import org.eclipse.jface.wizard.WizardPage;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.custom.ScrolledComposite;
 import org.eclipse.swt.events.KeyAdapter;
 import org.eclipse.swt.events.KeyEvent;
 import org.eclipse.swt.events.ModifyEvent;
 import org.eclipse.swt.events.ModifyListener;
 import org.eclipse.swt.events.SelectionAdapter;
 import org.eclipse.swt.events.SelectionEvent;
 import org.eclipse.swt.layout.FillLayout;
 import org.eclipse.swt.layout.GridData;
 import org.eclipse.swt.layout.GridLayout;
 import org.eclipse.swt.widgets.Button;
 import org.eclipse.swt.widgets.Combo;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.FileDialog;
 import org.eclipse.swt.widgets.Group;
 import org.eclipse.swt.widgets.Label;
 import org.eclipse.swt.widgets.List;
 import org.eclipse.swt.widgets.Spinner;
 import org.eclipse.swt.widgets.Table;
 import org.eclipse.swt.widgets.TableColumn;
 import org.eclipse.swt.widgets.TableItem;
 import org.eclipse.swt.widgets.Text;
 import org.pathvisio.Globals;
 import org.pathvisio.debug.Logger;
 import org.pathvisio.gui.swt.SwtEngine;
 import org.pathvisio.model.DataSource;
 import org.pathvisio.preferences.swt.SwtPreferences.SwtPreference;
 import org.pathvisio.util.swt.TableColumnResizer;
 import org.pathvisio.visualization.colorset.ColorSet;
 import org.pathvisio.visualization.colorset.ColorGradient;
 import org.pathvisio.visualization.colorset.ColorGradient.ColorValuePair;
 import org.pathvisio.visualization.colorset.ColorSetManager;
 
 /**
  * This class is a {@link Wizard} that guides the user trough the process to
  * create an expression dataset from a delimited text file
  */
 public class GexImportWizard extends Wizard 
 {
 	private ImportInformation importInformation;
 	private ImportPage importPage = new ImportPage();
 	private java.util.List<DataSource> dataSourceList = 
 		new ArrayList<DataSource>();
 
 	// File name from which data will be imported as 
 	// chosen by the user at the file page
 	private String file;
 	// String array with all the available names of 
 	// datasources and systemcodes 
 	private String[] sysCode;
 	private boolean importFinished;
 	
 	public GexImportWizard() 
 	{
 		importInformation = new ImportInformation();
 				
 		setWindowTitle("Create an expression dataset");
 		setNeedsProgressMonitor(true);
 	}
 
 	public void addPages() 
 	{
 		addPage(new FilePage());
 		addPage(new HeaderPage());
 		addPage(new ColumnPage());
 		addPage(importPage);
 	}
 
 	public boolean performFinish() 
 	{
 		//if true is returned: wizard closes
 		if (!importFinished) 
 		{			
 			try 
 			{
 				// Start import process
 				getContainer().run(true, true,
 					new GexSwt.ImportProgressKeeper(
 						(ImportPage) getPage("ImportPage"), importInformation));
 			} 
 			catch (Exception e) 
 			{
 				MessageDialog.openError(getShell(), "Error", "Exception while importing data.\n" + e.getMessage() + "\nSee log for details.");
 				Logger.log.error("while running expression data import process: " + e.getMessage(), e);
 			} 
 
 			
 			importFinished = true;
 			// Show different text on import page when the data has been imported
 			importPage.refreshProgress();
 			return false;
 		}
 		if (importFinished && 
 			getContainer().getCurrentPage().getName().equals("ImportPage")) 
 		{
 			return true;
 		}
 		return false;
 	}
 
 	public boolean performCancel() 
 	{
 		return true; // Do nothing, just close wizard
 	}
 
 	/**
 	 * This is the wizard page to specify the location of the text file
 	 * containing expression data and the location to store the new expression
 	 * dataset
 	 */
 	private class FilePage extends WizardPage 
 	{
 		private boolean txtFileComplete;
 		private boolean gexFileComplete;
 
 		public FilePage() 
 		{
 			super("FilePage");
 			setTitle("File locations");
 			setDescription("Specify the locations of the file containing the expression data"
 					+ ", where to store the expression dataset\n" 
 					+ " and change the gene database if required.");
 			setPageComplete(false);
 		}
 
 		public void createControl(Composite parent) 
 		{
 			final FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN);
 
 			Composite composite = new Composite(parent, SWT.NULL);
 			composite.setLayout(new GridLayout(2, false));
 
 			GridData labelGrid = new GridData(GridData.FILL_HORIZONTAL);
 			labelGrid.horizontalSpan = 2;
 
 			Label txtLabel = new Label(composite, SWT.FLAT);
 			txtLabel.setText("Specify location of text file containing expression data");
 			txtLabel.setLayoutData(labelGrid);
 
 			final Text txtText = new Text(composite, SWT.SINGLE | SWT.BORDER);
 			txtText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
 			Button txtButton = new Button(composite, SWT.PUSH);
 			txtButton.setText("Browse");
 
 			Label gexLabel = new Label(composite, SWT.FLAT);
 			gexLabel.setText("Specify location to save the expression dataset");
 			gexLabel.setLayoutData(labelGrid);
 
 			final Text gexText = new Text(composite, SWT.SINGLE | SWT.BORDER);
 			gexText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
 			Button gexButton = new Button(composite, SWT.PUSH);
 			gexButton.setText("Browse");
 
 			//Add widget and listener for selecting the Gene Database
 			
 			Label gdbLabel = new Label(composite, SWT.FLAT);
 			gdbLabel.setText("Specify location of the gene database");
 			gdbLabel.setLayoutData(labelGrid);
 
 			final Text gdbText = new Text(composite, SWT.SINGLE | SWT.BORDER);
 			gdbText.setText(GdbManager.getCurrentGdb().getDbName());
 			gdbText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
 			Button gdbButton = new Button(composite, SWT.PUSH);
 			gdbButton.setText("Browse");
 			
 			gdbButton.addSelectionListener(new SelectionAdapter() 
 			{
 				public void widgetSelected(SelectionEvent e) 
 				{
 					fileDialog.setText("Select gene database");
 					fileDialog.setFilterExtensions(
 						new String[] { "*.pgdb", "*.*" });
 					fileDialog.setFilterNames(new String[]
 					{ 
 							"Gene Database",
 							"All files" 
 					});
 					fileDialog.setFilterPath(SwtPreference.SWT_DIR_GDB.getValue());
 					String file = fileDialog.open();
 					if (file != null)
 					{
 						gdbText.setText(file);
 					}
 					
 					try
 					{
 						//Connect to the new database
 						GdbManager.setGeneDb(file);
 					}
 					catch(DataException ex)
 					{
 						MessageDialog.openError(getShell(), "Error", "Unable to open gene database");
 						Logger.log.error ("Unable to open gene database", ex);
 					}
 				}
 			});
 			
 			//End gene database widget
 			
 			txtButton.addSelectionListener(new SelectionAdapter() 
 			{
 				public void widgetSelected(SelectionEvent e) 
 				{
 					fileDialog.setText("Select text file containing expression data");
 					fileDialog.setFilterExtensions(
 							new String[] { "*.txt",	"*.*" });
 					fileDialog.setFilterNames(
 							new String[] { "Text file",	"All files" });
 					fileDialog.setFilterPath(SwtPreference.SWT_DIR_EXPR.getValue());
 					file = fileDialog.open();
 					if (file != null) 
 					{
 						txtText.setText(file);
 						gexText.setText(file.replace(file.substring(
 								file.lastIndexOf(".")), ""));
 					}
 				}
 			});
 
 			gexButton.addSelectionListener(new SelectionAdapter() 
 			{
 				public void widgetSelected(SelectionEvent e) 
 				{
 					try 
 					{
 						DBConnectorSwt dbcon = SwtEngine.getCurrent().getSwtDbConnector(DBConnector.TYPE_GEX);
 						String dbName = dbcon.openNewDbDialog(getShell(), gexText.getText());
 						if(dbName != null) gexText.setText(dbName);
 					} 
 					catch(Exception ex) 
 					{
 						MessageDialog.openError(getShell(), "Error", "Unable to open connection dialog");
 						Logger.log.error("", ex);
 					}
 				}
 			});
 
 			txtText.addModifyListener(new ModifyListener() 
 			{
 				public void modifyText(ModifyEvent e) 
 				{
 					setTxtFile(new File(txtText.getText()));
 					setPageComplete(txtFileComplete && gexFileComplete);
 				}
 			});
 
 			gexText.addModifyListener(new ModifyListener() 
 			{
 				public void modifyText(ModifyEvent e) 
 				{
 					setDbName(gexText.getText());
 					setPageComplete(txtFileComplete && gexFileComplete);
 				}
 			});
 
 			composite.pack();
 			setControl(composite);
 		}
 
 		/**
 		 * Stores the given {@link File} pointing to the file containing the expresssion
 		 * data in text form to the {@link ImportInformation} object
 		 * @param file
 		 */
 		private void setTxtFile(File file) 
 		{
 			if (!file.exists()) 
 			{
 				setErrorMessage("Specified file to import does not exist");
 				txtFileComplete = false;
 				return;
 			}
 			if (!file.canRead()) 
 			{
 				setErrorMessage("Can't access specified file containing expression data");
 				txtFileComplete = false;
 				return;
 			}
 			importInformation.setTxtFile(file);
 			setErrorMessage(null);
 			txtFileComplete = true;
 		}
 
 		/**
 		 * Sets the name of the database to save the
 		 * expression database to the {@link ImportInformation} object
 		 * @param file
 		 */
 		private void setDbName(String name) 
 		{
 			importInformation.setDbName (name);
 			setMessage("Expression dataset location: " + name + ".");
 			gexFileComplete = true;
 		}
 
 		public IWizardPage getNextPage() 
 		{
 			setPreviewTableContent(previewTable); //Content of previewtable depends on file locations
 			return super.getNextPage();
 		}
 	}
 
 	Table previewTable;
 	
 	/**
 	 * This {@link WizardPage} is used to ask the user information about on which line the
 	 * column headers are and on which line the data starts
 	 */
 	public class HeaderPage extends WizardPage 
 	{
 		public HeaderPage() 
 		{
 			super("HeaderPage");
 			setTitle("Header information and delimiter");
 			setDescription("Specify the line with the column headers and from where the data starts, "+"\n"
 					+"then select the column delimiter used in your dataset.");
 			setPageComplete(true);
 		}
 		
 		private Spinner startSpinner;
 		private Spinner headerSpinner;
 		private Button checkOther;
 		private Button headerButton;
 		private Text otherText;
 		
 		public void createControl(Composite parent) 
 		{
 			Composite composite = new Composite(parent, SWT.NULL);
 			composite.setLayout(new GridLayout(2, false));
 
 			Label headerLabel = new Label(composite, SWT.FLAT);
 			headerLabel.setText("Column headers at line: ");
 			headerSpinner = new Spinner(composite, SWT.BORDER);
 			headerSpinner.setMinimum(1);
 			headerSpinner.setSelection(importInformation.headerRow);
 
 			// button to check when there is no header in the data
 			Label noheaderLabel = new Label(composite, SWT.FLAT);
 			noheaderLabel.setText("Or choose 'no header': ");
 			headerButton = new Button(composite, SWT.CHECK);
 			headerButton.addSelectionListener(new SelectionAdapter() 
 			{
 				public void widgetSelected(SelectionEvent e) 
 				{
 					if(headerButton.getSelection() == true) headerSpinner.setEnabled(false);
 					if(headerButton.getSelection() == false) headerSpinner.setEnabled(true);
 				}
 			});
 
 			Label startLabel = new Label(composite, SWT.FLAT);
 			startLabel.setText("Data starts at line: ");
 			startSpinner = new Spinner(composite, SWT.BORDER);
 			startSpinner.setMinimum(1);
			startSpinner.setSelection(importInformation.firstDataRow);
 
 			//Widgets to give control over the delimiter
 			
 			Label delimiterLabel = new Label(composite,SWT.FLAT);
 			delimiterLabel.setText("Select the delimiter: \n");
 			
 			Label legeLabel = new Label(composite,SWT.FLAT);
 			legeLabel.setText(" ");
 						
 			Button checkTabs = new Button(composite,SWT.RADIO);
 			checkTabs.setText("Tabs");
 			checkTabs.setLocation(0,0);
 			checkTabs.setSize(100,20);
 			checkTabs.setSelection(true);
 						
 			Button checkComma = new Button(composite,SWT.RADIO);
 			checkComma.setText("Commas");
 			checkComma.setLocation(0,0);
 			checkComma.setSize(100,20);
 			
 			Button checkSemicolon = new Button(composite,SWT.RADIO);
 			checkSemicolon.setText("Semicolons");
 			checkSemicolon.setLocation(0,0);
 			checkSemicolon.setSize(100,20);
 			
 			Button checkSpaces = new Button(composite,SWT.RADIO);
 			checkSpaces.setText("Spaces");
 			checkSpaces.setLocation(0,0);
 			checkSpaces.setSize(100,20);
 			
 			checkOther = new Button(composite,SWT.RADIO);
 			checkOther.setText("Other:");
 			checkOther.setLocation(0,0);
 			checkOther.setSize(100,20);
 			otherText = new Text(composite, SWT.SINGLE | SWT.BORDER);
 			otherText.setLayoutData(new GridData(GridData.FILL));
 			otherText.setEditable(false);
 			
 			//Listeners for the 'select delimiter' buttons 		
 			checkTabs.addSelectionListener(new SelectionAdapter() 
 			{
 				public void widgetSelected(SelectionEvent e) 
 				{
 					importInformation.setDelimiter("\t");
 					otherText.setEditable(false);
 					setPageComplete(true);
 				
 				}
 			});
 			
 			checkComma.addSelectionListener(new SelectionAdapter() 
 			{
 				public void widgetSelected(SelectionEvent e) 
 				{
 					importInformation.setDelimiter(",");
 					otherText.setEditable(false);
 					setPageComplete(true);
 				}
 			});
 			
 			checkSemicolon.addSelectionListener(new SelectionAdapter() 
 			{
 				public void widgetSelected(SelectionEvent e) 
 				{
 					importInformation.setDelimiter(";");
 					otherText.setEditable(false);
 					setPageComplete(true);
 				}
 			});
 			
 			checkSpaces.addSelectionListener(new SelectionAdapter() 
 			{
 				public void widgetSelected(SelectionEvent e) 
 				{
 					importInformation.setDelimiter(" ");
 					otherText.setEditable(false);
 					setPageComplete(true);
 				}
 			});
 
 			checkOther.addSelectionListener(new SelectionAdapter() 
 			{
 				public void widgetSelected(SelectionEvent e) 
 				{
 					otherText.setEditable(true);
 					setPageComplete(false);
 				}
 			});
 			
 			//Listener to check if the 'other' text box is empty or not
 			otherText.addKeyListener(new KeyAdapter() 
 			{
 				public void keyReleased(KeyEvent e)
 				{
 					if (otherText.getText()!="")
 					{
 						setPageComplete(true);
 					}
 					else 
 					{
 						setPageComplete(false);
 					}
 				}
 			});
 						
 			Group tableGroup = new Group(composite, SWT.SHADOW_ETCHED_IN);
 			GridData groupGrid = new GridData(GridData.FILL_BOTH);
 			groupGrid.horizontalSpan = 2;
 			groupGrid.widthHint = 300;
 			tableGroup.setLayoutData(groupGrid);
 			tableGroup.setLayout(new FillLayout());
 			tableGroup.setText("Preview of file to import");
 			
 			previewTable = new Table(tableGroup, SWT.SINGLE | SWT.BORDER);
 			previewTable.setLinesVisible(true);
 			previewTable.setHeaderVisible(true);
 			TableColumn nrCol = new TableColumn(previewTable, SWT.LEFT);
 			nrCol.setText("line");
 			TableColumn txtCol = new TableColumn(previewTable, SWT.LEFT);
 			txtCol.setText("data");
 			nrCol.setWidth(40);
 			nrCol.setResizable(false);
 			previewTable.addControlListener(new TableColumnResizer(previewTable, tableGroup, new int[] {0, 100}));
 
 			composite.pack();
 			setControl(composite);
 		}
 
 		public IWizardPage getNextPage() 
 		{			
 			//If 'other' is selected change the delimiter
 			if ((otherText.getText() != "") && (checkOther.getSelection()))
 			{
 				String other = otherText.getText();
 				importInformation.setDelimiter(other);
 			}
 			
 			importInformation.headerRow = headerSpinner.getSelection();
			importInformation.firstDataRow = startSpinner.getSelection();
 			importInformation.setNoHeader(headerButton.getSelection());
 			setColumnTableContent(columnTable);
 			setColumnControlsContent();
 			return super.getNextPage();
 		}
 	}
 
 	private Table columnTable;
 	private List columnList;
 	private Combo codeCombo;
 	private Button codeRadio;
 	private Combo idCombo;
 	private Button syscodeRadio;
 	private Combo syscodeCombo;
 
 	/**
 	 * This is the wizard page to specify column information, e.g. which
 	 * are the gene id and systemcode columns
 	 */
 	public class ColumnPage extends WizardPage 
 	{		
 		public ColumnPage() 
 		{
 			super("ColumnPage");
 			setTitle("Column information");
 			setDescription("Specify which columns contain the gene information and "
 					+ "which columns should not be treated as numeric data.");
 			setPageComplete(true);
 		}
 
 		public void createControl(Composite parent) 
 		{
 			Composite composite = new Composite(parent, SWT.NULL);
 			composite.setLayout(new GridLayout(1, false));
 			
 			Label idLabel = new Label(composite, SWT.FLAT);
 			idLabel.setText("Select column with gene identifiers");
 			idCombo = new Combo(composite, SWT.READ_ONLY);
 			idCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
 			
 			//Radio button to select that a system code column is present.
 			codeRadio = new Button(composite,SWT.RADIO);
 			codeRadio.setSelection(true);
 			codeRadio.setText("Select column with System Code");
 			//Drop down menu to select in which column the system code is present in the data file. 
 			codeCombo = new Combo(composite, SWT.READ_ONLY);
 			codeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
 						
 			/*Create a new String array with the full names of the dataSources
 			 * and add the system codes between parentheses for use in the syscodeCombo.
 			 * The last element of the string with system codes is an empty string,
 			 * therefore length-1 is used*/
 			
 			dataSourceList.addAll(DataSource.getDataSources());
 			sysCode = new String[dataSourceList.size()];
 			for (int i=0; i<(dataSourceList.size()); i++) 
 			{
 				sysCode[i] = 
 					dataSourceList.get(i).getFullName() + 
 					" (" + dataSourceList.get(i).getSystemCode() + ")";
 			}
 			
 			//Radio button to select that no system code column is present.
 			syscodeRadio = new Button(composite,SWT.RADIO);
 			syscodeRadio.setText("Select System Code for whole dataset if no System Code colomn is availabe in the dataset");
 			//Drop down menu to select the data source.
 			syscodeCombo = new Combo(composite, SWT.READ_ONLY);
 			syscodeCombo.setItems(sysCode);
 			syscodeCombo.setEnabled(false);
 			
 			Label columnLabel = new Label(composite, SWT.FLAT | SWT.WRAP);
 			columnLabel.setText(
 					"Select the columns containing data that should NOT be treated" + 
 					" as NUMERIC from the list below");
 
 			columnList = new List(composite, SWT.BORDER | SWT.MULTI
 					| SWT.V_SCROLL);
 			GridData listGrid = new GridData(GridData.FILL_HORIZONTAL);
 			listGrid.heightHint = 150;
 			columnList.setLayoutData(listGrid);
 			columnList.addSelectionListener(new SelectionAdapter() 
 			{
 				public void widgetSelected(SelectionEvent e) 
 				{
 					importInformation.setStringCols(
 						columnList.getSelectionIndices());
 				}
 			});
 			
 			
 			idCombo.addSelectionListener(new SelectionAdapter() 
 			{
 				public void widgetSelected(SelectionEvent e) 
 				{
					importInformation.idColumn = idCombo.getSelectionIndex();
 				}
 			});
 			
 			codeRadio.addSelectionListener(new SelectionAdapter() 
 			{
 				public void widgetSelected(SelectionEvent e) 
 				{
 					codeCombo.setEnabled(true);
 					syscodeCombo.setEnabled(false);
 					importInformation.setSyscodeColumn(true);				
 				}
 			});
 			
 			codeCombo.addSelectionListener(new SelectionAdapter() 
 			{
 				public void widgetSelected(SelectionEvent e) 
 				{
 					if (codeRadio.getSelection()){
						importInformation.codeColumn = 
							codeCombo.getSelectionIndex();
 					}
 				}
 			});
 			
 			syscodeRadio.addSelectionListener(new SelectionAdapter() 
 			{
 				public void widgetSelected(SelectionEvent e) 
 				{
 					syscodeCombo.setEnabled(true);
 					codeCombo.setEnabled(false);
 					importInformation.setSyscodeColumn(false);				
 				}
 			});
 			
 			syscodeCombo.addSelectionListener(new SelectionAdapter() 
 			{
 				public void widgetSelected(SelectionEvent e) 
 				{
 					if (syscodeRadio.getSelection())
 					{
 						importInformation.setDataSource (
 							dataSourceList.get(syscodeCombo.getSelectionIndex()));
 					}
 				}
 			});
 			
 
 			Group tableGroup = new Group(composite, SWT.SHADOW_ETCHED_IN);
 			GridData tableGrid = new GridData(GridData.FILL_BOTH);
 			tableGrid.heightHint = 100;
 			tableGroup.setLayoutData(tableGrid);
 			tableGroup.setLayout(new FillLayout());
 			tableGroup.setText("Preview of file to import");
 
 			ScrolledComposite sc = new ScrolledComposite (tableGroup, SWT.H_SCROLL | SWT.V_SCROLL);
 			columnTable = new Table(sc, SWT.SINGLE | SWT.BORDER);
 			sc.setContent (columnTable);
 			columnTable.setLinesVisible(true);
 			columnTable.setHeaderVisible(true);
 			// columnTable.addControlListener(new TableColumnResizer(columnTable, tableGroup));
 
 			composite.pack();
 			setControl(composite);
 		}
 	}
 
 	/**
 	 * Sets the content of the Controls on the {@link ColumnPage}
 	 */
 	public void setColumnControlsContent() 
 	{
 		String[] colNames = importInformation.getColNames();
 
 		columnList.setItems(colNames);
 		columnList.setSelection(importInformation.getStringCols());
 		idCombo.setItems(colNames);
		idCombo.select(importInformation.idColumn);
 		codeCombo.setItems(colNames);
		codeCombo.select(importInformation.codeColumn);
 	}
 
 	/**
 	 * Sets the content of the given preview table (containing 2 columns: linenumber and textdata)
 	 * @param previewTable
 	 */
 	public void setPreviewTableContent(Table previewTable) 
 	{
 		previewTable.removeAll();
 		try 
 		{
 			int n = 50; // nr of lines to include in the preview
 			BufferedReader in = importInformation.getBufferedReader();
 			String line;
 			int i = 1;
 			while ((line = in.readLine()) != null && i <= n) 
 			{
 				TableItem ti = new TableItem(previewTable, SWT.NULL);
 				ti.setText(0, Integer.toString(i++));
 				ti.setText(1, line);
 			}
 		} 
 		catch (IOException e) 
 		{ 		
 			MessageDialog.openError(getShell(), "Error", "while generating preview for importing expression data: " + e.getMessage());
 			Logger.log.error("while generating preview for importing expression data: " + e.getMessage(), e);
 		}
 		previewTable.pack();
 	}
 
 	/**
 	 * Sets the content of the given columnTable (previews how the data will be divided in columns)
 	 * @param columnTable
 	 */
 	public void setColumnTableContent(Table columnTable) 
 	{
 		columnTable.removeAll();
 		for (TableColumn col : columnTable.getColumns())
 			col.dispose();
 		for (String colName : importInformation.getColNames()) 
 		{
 			TableColumn tc = new TableColumn(columnTable, SWT.NONE);
 			tc.setText(colName);
 			tc.pack();
 		}
 		try 
 		{
 			int n = 50; // nr of lines to include in the preview
 			BufferedReader in = importInformation.getBufferedReader();
 			String line;
			for (int i = 0; i < importInformation.firstDataRow - 1; i++)
 				in.readLine(); // Go to line where data starts
 			int j = 1;
 			
 			//"Guess" the system code based on the first 50 lines.
 			
 			//Make regular expressions patterns for the gene ID's.
 			Map<DataSource, Pattern> patterns = DataSourcePatterns.getPatterns();
 			
 			//Make regular expressions pattern for the system code. 
 			Pattern syscodepattern;
 			syscodepattern = Pattern.compile("[A-Z][a-z]?");
 			
 			//Make count variables.
 			Map<DataSource, Integer> counts = new HashMap<DataSource, Integer>();
 
 			for (DataSource ds : patterns.keySet())
 			{
 				counts.put (ds, 0);
 			}
 			int syscodecount = 0;
 			
 			while ((line = in.readLine()) != null && j++ < n) 
 			{
 				TableItem ti = new TableItem(columnTable, SWT.NULL);
 				String[] elements = line.split(importInformation.getDelimiter());	//Get the elements of a row
 				ti.setText(elements);	//Sets the elements in the table
 				
 				for (int i=0; i < elements.length; i++)
 				{
 					Matcher	syscodematcher;			
 					//Count all the times that an element matches a gene identifier.
 					syscodematcher = syscodepattern.matcher(elements[i]);
 					if (syscodematcher.matches()) 
 					{
 						syscodecount++;
 					}
 					
 					for (DataSource ds : patterns.keySet())
 					{
 						//Setup the matcher
 						Matcher m = patterns.get(ds).matcher(elements[i]);					
 					
 						if (m.matches())
 						{
 							//Check if it matches, and count how many times it does.
 							counts.put(ds, counts.get(ds) + 1);	
 						}
 					}
 				}				
 			}
 			
 			/*Calculate percentage of rows where a system code is found and
 			 * compare with a given percentage*/
 			double checkpercentage = 0.9;
 			double syscodepercentage = (double)syscodecount / (double)j;
 			
 			/*Set the selection to the codeRadio button if a system code is found
 			 * in more than rows than the given percentage, otherwise set the 
 			 * selection to the syscodeRadio button*/
 			if (syscodepercentage >= checkpercentage) 
 			{
 				codeRadio.setSelection(true);
 				syscodeRadio.setSelection(false);
 				codeCombo.setEnabled(true);
 				syscodeCombo.setEnabled(false);				
 				importInformation.setSyscodeColumn(true);
 			}
 			else 
 			{
 				syscodeRadio.setSelection(true);
 				codeRadio.setSelection(false);
 				syscodeCombo.setEnabled(true);
 				codeCombo.setEnabled(false);				
 				importInformation.setSyscodeColumn(false);
 			}
 			
 			//Calculate percentages from the counts (length isn't always 50)		
 			double percentage; 
 			
 			//Look for maximum.
 			double max = 0;
 			DataSource maxds = null;
 			for (DataSource ds : patterns.keySet())
 			{
 				//Determine the maximum of the percentages (most hits). 
 				//Sometimes, normal data can match a gene identifier, in which case percentages[i]>1. 
 				//Ignores these gene identifiers.
 				percentage = (double)counts.get(ds)/(double)j;
 				if (percentage > max && percentage <= 1)
 				{
 					max = percentage;
 					maxds = ds;
 				}
 			}
 			
 			//Select the right entry in the drop down menu and change the system code in importInformation
 			syscodeCombo.select(dataSourceList.indexOf(maxds));
 			importInformation.setDataSource(maxds);
 		} 
 		catch (IOException e) 
 		{ 
 			MessageDialog.openError(getShell(), "Error", "while generating preview for importing expression data: " + e.getMessage());			
 			Logger.log.error("while generating preview for importing expression data: " + e.getMessage(), e);
 		}
 		columnTable.pack();
 	}
 	
 
 	/**
 	 * This page shows the progress and status of the import process
 	 */
 	public class ImportPage extends WizardPage 
 	{
 		Text progressText;
 
 		public ImportPage() 
 		{
 			super("ImportPage");
 			setTitle("Create expression dataset");
 			setDescription("Press finish button to create the expression dataset.");
 			setPageComplete(true);
 		}
 
 		public void createControl(Composite parent) 
 		{
 			Composite composite = new Composite(parent, SWT.NULL);
 			composite.setLayout(new FillLayout());
 
 			progressText = new Text(composite, SWT.READ_ONLY | SWT.BORDER
 					| SWT.WRAP);
 			refreshProgress();
 			setControl(composite);
 		}
 
 		public void refreshProgress()
 		{
 			if (!importFinished) { // New data can be read
 				importPage.setTitle("Create expression dataset");
 				importPage.setDescription("Press finish button to create the expression dataset.");				
 				
 				if(file != null)
 				{
 					progressText.setText(
 						"Ready to import data from file: '" + 
 						file + "'\n" +
 						"\nImported data will be saved at: '" + 
 						file.replace(
 								file.substring (file.lastIndexOf(".")), "") + 
 						"'\n" +	"\nUsed gene database: '" + 
 						GdbManager.getCurrentGdb().getDbName() + "'\n\n");
 					if(importInformation.getSyscodeColumn()) 
 					{ // System code can be read from data
 						println("System code will be read from data");
 					}
 					else
 					{ // System code is chosen by the user
 						println("Chosen System Code: " + sysCode[syscodeCombo.getSelectionIndex()]);
 					}
 				}
 			}
 
 			if(importFinished) 
 			{ // Data has been read
 
 				//Creating a default color set for the visualizations.
 				//this has to be done after re-connecting to the pgex.
 				createDefaultColorSet(importInformation.getMinimum(), importInformation.getMaximum());
 				importPage.setTitle("Import finished");
 				importPage.setDescription("Press finish to return to " + Globals.APPLICATION_VERSION_NAME);
 				progressText.setText("Data has been imported. \n\n");
 				
 				// Show a list of errors
 				int show = 20; // number of errors that will be showed
 				int nrErrors = importInformation.getNrErrors(); 
 				if (nrErrors < 20) { show = nrErrors; }
 				println("Errors (" + show + " of " + nrErrors + " items)");				
 				java.util.List<String> errorList = importInformation.getErrorList();
 				int nrLines = 0; 
 				for (String error : errorList)
 				{
 					if (nrLines == 20) break; // Maximum number of errors to be showed
 					println(error);
 					nrLines++;
 				}
 				if (nrErrors > 0) 
 				{
 					println("\nSee errorfile '" + importInformation.getDbName() + ".ex.txt" + "' for details");
 				}
 			}
 		}			
 		
 		public void println(String text) 
 		{
 			appendProgressText(text, true);
 		}
 
 		public void print(String text) 
 		{
 			appendProgressText(text, false);
 		}
 		
 		public void appendProgressText (final String updateText,
 				final boolean newLine) 
 		{
 			if (progressText != null && !progressText.isDisposed())
 			{
 				progressText.getDisplay().asyncExec(new Runnable() 
 				{
 					public void run() 
 					{
 						progressText.append(updateText
 								+ (newLine ? progressText.getLineDelimiter()
 										: ""));
 					}
 				});
 			}
 		}
 
 		public IWizardPage getPreviousPage() 
 		{
 			// User pressed back, probably to change settings and redo the
 			// importing, so set importFinished to false
 			importFinished = false;
 			// Refresh the text on the last page in case someone already loaded data.
 			importPage.refreshProgress();
 			return super.getPreviousPage();
 		}
 	}
 
 	public static void createDefaultColorSet(double minimum, double maximum)
 	{
 		Color green= new Color(0,255,0);
 		Color red = new Color(255,0,0);
 		Color yellow = new Color(255,255,0);
 		ColorSet colorSet = new ColorSet("Default");
 		ColorGradient gradient = new ColorGradient(colorSet);
 		
 		ColorValuePair low = gradient.new ColorValuePair(green,minimum);
 		ColorValuePair middle = gradient.new ColorValuePair(red,(minimum+maximum)/2);
 		ColorValuePair high = gradient.new ColorValuePair(yellow,maximum);
 		
 		gradient.addColorValuePair(low);
 		gradient.addColorValuePair(middle);
 		gradient.addColorValuePair(high);
 		
 		colorSet.addObject(gradient);
 		
 		ColorSetManager.addColorSet(colorSet);
 		
 	}
 
 }
