  /**********************************************************************
  **                                                                   **
  **               This code belongs to the KETTLE project.            **
  **                                                                   **
  ** Kettle, from version 2.2 on, is released into the public domain   **
  ** under the Lesser GNU Public License (LGPL).                       **
  **                                                                   **
  ** For more details, please read the document LICENSE.txt, included  **
  ** in this project                                                   **
  **                                                                   **
  ** http://www.kettle.be                                              **
  ** info@kettle.be                                                    **
  **                                                                   **
  **********************************************************************/
 
  
 /*
  * Created on 18-mei-2003
  *
  */
 
 package be.ibridge.kettle.trans.step.textfileoutput;
 
 import java.nio.charset.Charset;
 import java.util.ArrayList;
 
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.custom.CCombo;
 import org.eclipse.swt.custom.CTabFolder;
 import org.eclipse.swt.custom.CTabItem;
 import org.eclipse.swt.events.FocusListener;
 import org.eclipse.swt.events.ModifyEvent;
 import org.eclipse.swt.events.ModifyListener;
 import org.eclipse.swt.events.SelectionAdapter;
 import org.eclipse.swt.events.SelectionEvent;
 import org.eclipse.swt.events.ShellAdapter;
 import org.eclipse.swt.events.ShellEvent;
 import org.eclipse.swt.graphics.Cursor;
 import org.eclipse.swt.graphics.Point;
 import org.eclipse.swt.layout.FormAttachment;
 import org.eclipse.swt.layout.FormData;
 import org.eclipse.swt.layout.FormLayout;
 import org.eclipse.swt.widgets.Button;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Display;
 import org.eclipse.swt.widgets.Event;
 import org.eclipse.swt.widgets.FileDialog;
 import org.eclipse.swt.widgets.Label;
 import org.eclipse.swt.widgets.Listener;
 import org.eclipse.swt.widgets.MessageBox;
 import org.eclipse.swt.widgets.Shell;
 import org.eclipse.swt.widgets.Table;
 import org.eclipse.swt.widgets.TableItem;
 import org.eclipse.swt.widgets.Text;
 
 import be.ibridge.kettle.core.ColumnInfo;
 import be.ibridge.kettle.core.Const;
 import be.ibridge.kettle.core.Props;
 import be.ibridge.kettle.core.Row;
 import be.ibridge.kettle.core.dialog.EnterSelectionDialog;
 import be.ibridge.kettle.core.dialog.ErrorDialog;
 import be.ibridge.kettle.core.exception.KettleException;
 import be.ibridge.kettle.core.util.StringUtil;
 import be.ibridge.kettle.core.value.Value;
 import be.ibridge.kettle.core.widget.TableView;
 import be.ibridge.kettle.trans.TransMeta;
 import be.ibridge.kettle.trans.step.BaseStepDialog;
 import be.ibridge.kettle.trans.step.BaseStepMeta;
 import be.ibridge.kettle.trans.step.StepDialogInterface;
 import be.ibridge.kettle.trans.step.textfileinput.VariableButtonListenerFactory;
 
 
 public class TextFileOutputDialog extends BaseStepDialog implements StepDialogInterface
 {
 	private CTabFolder   wTabFolder;
 	private FormData     fdTabFolder;
 	
 	private CTabItem     wFileTab, wContentTab, wFieldsTab;
 
 	private FormData     fdFileComp, fdContentComp, fdFieldsComp;
 
 	private Label        wlFilename;
 	private Button       wbFilename;
 	private Button       wbcFilename;
 	private Text         wFilename;
 	private FormData     fdlFilename, fdbFilename, fdbcFilename, fdFilename;
 
 	private Label        wlExtension;
 	private Text         wExtension;
 	private FormData     fdlExtension, fdExtension;
 
 	private Label        wlAddStepnr;
 	private Button       wAddStepnr;
 	private FormData     fdlAddStepnr, fdAddStepnr;
 
 	private Label        wlAddDate;
 	private Button       wAddDate;
 	private FormData     fdlAddDate, fdAddDate;
 
 	private Label        wlAddTime;
 	private Button       wAddTime;
 	private FormData     fdlAddTime, fdAddTime;
 
 	private Button       wbShowFiles;
 	private FormData     fdbShowFiles;
 
 	private Label        wlAppend;
 	private Button       wAppend;
 	private FormData     fdlAppend, fdAppend;
 
 	private Label        wlSeparator;
 	private Button       wbSeparator;
 	private Text         wSeparator;
 	private FormData     fdlSeparator, fdbSeparator, fdSeparator;
 
 	private Label        wlEnclosure;
 	private Text         wEnclosure;
 	private FormData     fdlEnclosure, fdEnclosure;
 
 	private Label        wlEndedLine;
 	private Text         wEndedLine;
 	private FormData     fdlEndedLine, fdEndedLine;
 	
     private Label        wlEnclForced;
     private Button       wEnclForced;
     private FormData     fdlEnclForced, fdEnclForced;
 
 	private Label        wlHeader;
 	private Button       wHeader;
 	private FormData     fdlHeader, fdHeader;
 	
 	private Label        wlFooter;
 	private Button       wFooter;
 	private FormData     fdlFooter, fdFooter;
 
 	private Label        wlFormat;
 	private CCombo       wFormat;
 	private FormData     fdlFormat, fdFormat;
 
 	private Label        wlCompression;
 	private CCombo       wCompression;
 	private FormData     fdlCompression, fdCompression;
 
     private Label        wlEncoding;
     private CCombo       wEncoding;
     private FormData     fdlEncoding, fdEncoding;
 
 	private Label        wlPad;
 	private Button       wPad;
 	private FormData     fdlPad, fdPad;
 
 	private Label        wlSplitEvery;
 	private Text         wSplitEvery;
 	private FormData     fdlSplitEvery, fdSplitEvery;
 
 	private TableView    wFields;
 	private FormData     fdFields;
 
 	private TextFileOutputMeta input;
 	
     private Button       wMinWidth;
     private Listener     lsMinWidth;
     private boolean      gotEncodings = false; 
     
 	public TextFileOutputDialog(Shell parent, Object in, TransMeta transMeta, String sname)
 	{
 		super(parent, (BaseStepMeta)in, transMeta, sname);
 		input=(TextFileOutputMeta)in;
 	}
 
 	public String open()
 	{
         Shell parent = getParent();
 		Display display = parent.getDisplay();
 
         shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN);
  		props.setLook(shell);
 
 		ModifyListener lsMod = new ModifyListener() 
 		{
 			public void modifyText(ModifyEvent e) 
 			{
 				input.setChanged();
 			}
 		};
 		changed = input.hasChanged();
 		
 		FormLayout formLayout = new FormLayout ();
 		formLayout.marginWidth  = Const.FORM_MARGIN;
 		formLayout.marginHeight = Const.FORM_MARGIN;
 
 		shell.setLayout(formLayout);
 		shell.setText(Messages.getString("TextFileOutputDialog.DialogTitle"));
 		
 		int middle = props.getMiddlePct();
 		int margin = Const.MARGIN;
 
 		// Stepname line
 		wlStepname=new Label(shell, SWT.RIGHT);
 		wlStepname.setText(Messages.getString("System.Label.StepName"));
  		props.setLook(wlStepname);
 		fdlStepname=new FormData();
 		fdlStepname.left  = new FormAttachment(0, 0);
 		fdlStepname.top   = new FormAttachment(0, margin);
 		fdlStepname.right = new FormAttachment(middle, -margin);
 		wlStepname.setLayoutData(fdlStepname);
 		wStepname=new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
 		wStepname.setText(stepname);
  		props.setLook(wStepname);
 		wStepname.addModifyListener(lsMod);
 		fdStepname=new FormData();
 		fdStepname.left = new FormAttachment(middle, 0);
 		fdStepname.top  = new FormAttachment(0, margin);
 		fdStepname.right= new FormAttachment(100, 0);
 		wStepname.setLayoutData(fdStepname);
 
 		wTabFolder = new CTabFolder(shell, SWT.BORDER);
  		props.setLook(wTabFolder, Props.WIDGET_STYLE_TAB);
 		
 		//////////////////////////
 		// START OF FILE TAB///
 		///
 		wFileTab=new CTabItem(wTabFolder, SWT.NONE);
 		wFileTab.setText(Messages.getString("TextFileOutputDialog.FileTab.TabTitle"));
 		
 		Composite wFileComp = new Composite(wTabFolder, SWT.NONE);
  		props.setLook(wFileComp);
 
 		FormLayout fileLayout = new FormLayout();
 		fileLayout.marginWidth  = 3;
 		fileLayout.marginHeight = 3;
 		wFileComp.setLayout(fileLayout);
 
 		// Filename line
 		wlFilename=new Label(wFileComp, SWT.RIGHT);
 		wlFilename.setText(Messages.getString("TextFileOutputDialog.Filename.Label"));
  		props.setLook(wlFilename);
 		fdlFilename=new FormData();
 		fdlFilename.left = new FormAttachment(0, 0);
 		fdlFilename.top  = new FormAttachment(0, margin);
 		fdlFilename.right= new FormAttachment(middle, -margin);
 		wlFilename.setLayoutData(fdlFilename);
 
 		wbFilename=new Button(wFileComp, SWT.PUSH| SWT.CENTER);
  		props.setLook(wbFilename);
 		wbFilename.setText(Messages.getString("System.Button.Browse"));
 		fdbFilename=new FormData();
 		fdbFilename.right= new FormAttachment(100, 0);
 		fdbFilename.top  = new FormAttachment(0, 0);
 		wbFilename.setLayoutData(fdbFilename);
 
 		wbcFilename=new Button(wFileComp, SWT.PUSH| SWT.CENTER);
  		props.setLook(wbcFilename);
 		wbcFilename.setText(Messages.getString("System.Button.Variable"));
 		fdbcFilename=new FormData();
 		fdbcFilename.right= new FormAttachment(wbFilename, -margin);
 		fdbcFilename.top  = new FormAttachment(0, 0);
 		wbcFilename.setLayoutData(fdbcFilename);
 
 		wFilename=new Text(wFileComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
  		props.setLook(wFilename);
 		wFilename.addModifyListener(lsMod);
 		fdFilename=new FormData();
 		fdFilename.left = new FormAttachment(middle, 0);
 		fdFilename.top  = new FormAttachment(0, margin);
 		fdFilename.right= new FormAttachment(wbcFilename, -margin);
 		wFilename.setLayoutData(fdFilename);
 		
 		// Whenever something changes, set the tooltip to the expanded version:
 		wFilename.addModifyListener(new ModifyListener()
 			{
 				public void modifyText(ModifyEvent e)
 				{
 					wFilename.setToolTipText(StringUtil.environmentSubstitute( wFilename.getText() ) );
 				}
 			}
 		);
 
 		// Extension line
 		wlExtension=new Label(wFileComp, SWT.RIGHT);
 		wlExtension.setText(Messages.getString("System.Label.Extension"));
  		props.setLook(wlExtension);
 		fdlExtension=new FormData();
 		fdlExtension.left = new FormAttachment(0, 0);
 		fdlExtension.top  = new FormAttachment(wFilename, margin);
 		fdlExtension.right= new FormAttachment(middle, -margin);
 		wlExtension.setLayoutData(fdlExtension);
 		wExtension=new Text(wFileComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
 		wExtension.setText("");
  		props.setLook(wExtension);
 		wExtension.addModifyListener(lsMod);
 		fdExtension=new FormData();
 		fdExtension.left = new FormAttachment(middle, 0);
 		fdExtension.top  = new FormAttachment(wFilename, margin);
 		fdExtension.right= new FormAttachment(wbcFilename, -margin);
 		wExtension.setLayoutData(fdExtension);
 
 		// Create multi-part file?
 		wlAddStepnr=new Label(wFileComp, SWT.RIGHT);
 		wlAddStepnr.setText(Messages.getString("TextFileOutputDialog.AddStepnr.Label"));
  		props.setLook(wlAddStepnr);
 		fdlAddStepnr=new FormData();
 		fdlAddStepnr.left = new FormAttachment(0, 0);
 		fdlAddStepnr.top  = new FormAttachment(wExtension, margin);
 		fdlAddStepnr.right= new FormAttachment(middle, -margin);
 		wlAddStepnr.setLayoutData(fdlAddStepnr);
 		wAddStepnr=new Button(wFileComp, SWT.CHECK);
  		props.setLook(wAddStepnr);
 		fdAddStepnr=new FormData();
 		fdAddStepnr.left = new FormAttachment(middle, 0);
 		fdAddStepnr.top  = new FormAttachment(wExtension, margin);
 		fdAddStepnr.right= new FormAttachment(100, 0);
 		wAddStepnr.setLayoutData(fdAddStepnr);
 		wAddStepnr.addSelectionListener(new SelectionAdapter() 
 			{
 				public void widgetSelected(SelectionEvent e) 
 				{
 					input.setChanged();
 				}
 			}
 		);
 
 		// Create multi-part file?
 		wlAddDate=new Label(wFileComp, SWT.RIGHT);
 		wlAddDate.setText(Messages.getString("TextFileOutputDialog.AddDate.Label"));
  		props.setLook(wlAddDate);
 		fdlAddDate=new FormData();
 		fdlAddDate.left = new FormAttachment(0, 0);
 		fdlAddDate.top  = new FormAttachment(wAddStepnr, margin);
 		fdlAddDate.right= new FormAttachment(middle, -margin);
 		wlAddDate.setLayoutData(fdlAddDate);
 		wAddDate=new Button(wFileComp, SWT.CHECK);
  		props.setLook(wAddDate);
 		fdAddDate=new FormData();
 		fdAddDate.left = new FormAttachment(middle, 0);
 		fdAddDate.top  = new FormAttachment(wAddStepnr, margin);
 		fdAddDate.right= new FormAttachment(100, 0);
 		wAddDate.setLayoutData(fdAddDate);
 		wAddDate.addSelectionListener(new SelectionAdapter() 
 			{
 				public void widgetSelected(SelectionEvent e) 
 				{
 					input.setChanged();
 					// System.out.println("wAddDate.getSelection()="+wAddDate.getSelection());
 				}
 			}
 		);
 		// Create multi-part file?
 		wlAddTime=new Label(wFileComp, SWT.RIGHT);
 		wlAddTime.setText(Messages.getString("TextFileOutputDialog.AddTime.Label"));
  		props.setLook(wlAddTime);
 		fdlAddTime=new FormData();
 		fdlAddTime.left = new FormAttachment(0, 0);
 		fdlAddTime.top  = new FormAttachment(wAddDate, margin);
 		fdlAddTime.right= new FormAttachment(middle, -margin);
 		wlAddTime.setLayoutData(fdlAddTime);
 		wAddTime=new Button(wFileComp, SWT.CHECK);
  		props.setLook(wAddTime);
 		fdAddTime=new FormData();
 		fdAddTime.left = new FormAttachment(middle, 0);
 		fdAddTime.top  = new FormAttachment(wAddDate, margin);
 		fdAddTime.right= new FormAttachment(100, 0);
 		wAddTime.setLayoutData(fdAddTime);
 		wAddTime.addSelectionListener(new SelectionAdapter() 
 			{
 				public void widgetSelected(SelectionEvent e) 
 				{
 					input.setChanged();
 				}
 			}
 		);
 
 		wbShowFiles=new Button(wFileComp, SWT.PUSH| SWT.CENTER);
  		props.setLook(wbShowFiles);
 		wbShowFiles.setText(Messages.getString("TextFileOutputDialog.ShowFiles.Button"));
 		fdbShowFiles=new FormData();
 		fdbShowFiles.left = new FormAttachment(middle, 0);
 		fdbShowFiles.top  = new FormAttachment(wAddTime, margin*2);
 		wbShowFiles.setLayoutData(fdbShowFiles);
 		wbShowFiles.addSelectionListener(new SelectionAdapter() 
 			{
 				public void widgetSelected(SelectionEvent e) 
 				{
 					TextFileOutputMeta tfoi = new TextFileOutputMeta();
 					getInfo(tfoi);
 					String files[] = tfoi.getFiles();
 					if (files!=null && files.length>0)
 					{
 						EnterSelectionDialog esd = new EnterSelectionDialog(shell, props, files, Messages.getString("TextFileOutputDialog.SelectOutputFiles.DialogTitle"), Messages.getString("TextFileOutputDialog.SelectOutputFiles.DialogMessage"));
 						esd.setViewOnly();
 						esd.open();
 					}
 					else
 					{
 						MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR );
 						mb.setMessage(Messages.getString("TextFileOutputDialog.NoFilesFound.DialogMessage"));
 						mb.setText(Messages.getString("System.DialogTitle.Error"));
 						mb.open(); 
 					}
 				}
 			}
 		);
 
 
 		
 		fdFileComp=new FormData();
 		fdFileComp.left  = new FormAttachment(0, 0);
 		fdFileComp.top   = new FormAttachment(0, 0);
 		fdFileComp.right = new FormAttachment(100, 0);
 		fdFileComp.bottom= new FormAttachment(100, 0);
 		wFileComp.setLayoutData(fdFileComp);
 	
 		wFileComp.layout();
 		wFileTab.setControl(wFileComp);
 
 		/////////////////////////////////////////////////////////////
 		/// END OF FILE TAB
 		/////////////////////////////////////////////////////////////
 
 		//////////////////////////
 		// START OF CONTENT TAB///
 		///
 		wContentTab=new CTabItem(wTabFolder, SWT.NONE);
 		wContentTab.setText(Messages.getString("TextFileOutputDialog.ContentTab.TabTitle"));
 
 		FormLayout contentLayout = new FormLayout ();
 		contentLayout.marginWidth  = 3;
 		contentLayout.marginHeight = 3;
 		
 		Composite wContentComp = new Composite(wTabFolder, SWT.NONE);
  		props.setLook(wContentComp);
 		wContentComp.setLayout(contentLayout);
 
 
 		// Append to end of file?
 		wlAppend=new Label(wContentComp, SWT.RIGHT);
 		wlAppend.setText(Messages.getString("TextFileOutputDialog.Append.Label"));
  		props.setLook(wlAppend);
 		fdlAppend=new FormData();
 		fdlAppend.left = new FormAttachment(0, 0);
 		fdlAppend.top  = new FormAttachment(0, 0);
 		fdlAppend.right= new FormAttachment(middle, -margin);
 		wlAppend.setLayoutData(fdlAppend);
 		wAppend=new Button(wContentComp, SWT.CHECK);
  		props.setLook(wAppend);
 		fdAppend=new FormData();
 		fdAppend.left = new FormAttachment(middle, 0);
 		fdAppend.top  = new FormAttachment(0, 0);
 		fdAppend.right= new FormAttachment(100, 0);
 		wAppend.setLayoutData(fdAppend);
 		wAppend.addSelectionListener(new SelectionAdapter() 
 			{
 				public void widgetSelected(SelectionEvent e) 
 				{
 					input.setChanged();
 				}
 			}
 		);
 		
 		wlSeparator=new Label(wContentComp, SWT.RIGHT);
 		wlSeparator.setText(Messages.getString("TextFileOutputDialog.Separator.Label"));
  		props.setLook(wlSeparator);
 		fdlSeparator=new FormData();
 		fdlSeparator.left = new FormAttachment(0, 0);
 		fdlSeparator.top  = new FormAttachment(wAppend, margin);
 		fdlSeparator.right= new FormAttachment(middle, -margin);
 		wlSeparator.setLayoutData(fdlSeparator);
 
 		wbSeparator=new Button(wContentComp, SWT.PUSH| SWT.CENTER);
  		props.setLook(wbSeparator);
 		wbSeparator.setText(Messages.getString("TextFileOutputDialog.Separator.Button"));
 		fdbSeparator=new FormData();
 		fdbSeparator.right= new FormAttachment(100, 0);
 		fdbSeparator.top  = new FormAttachment(wAppend, 0);
 		wbSeparator.setLayoutData(fdbSeparator);
 		wbSeparator.addSelectionListener(new SelectionAdapter() 
 			{
 				public void widgetSelected(SelectionEvent se) 
 				{
 					wSeparator.insert("\t");
 				}
 			}
 		);
 
 		wSeparator=new Text(wContentComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
  		props.setLook(wSeparator);
 		wSeparator.addModifyListener(lsMod);
 		fdSeparator=new FormData();
 		fdSeparator.left = new FormAttachment(middle, 0);
 		fdSeparator.top  = new FormAttachment(wAppend, margin);
 		fdSeparator.right= new FormAttachment(wbSeparator, -margin);
 		wSeparator.setLayoutData(fdSeparator);
 
 		// Enclosure line...
 		wlEnclosure=new Label(wContentComp, SWT.RIGHT);
 		wlEnclosure.setText(Messages.getString("TextFileOutputDialog.Enclosure.Label"));
  		props.setLook(wlEnclosure);
 		fdlEnclosure=new FormData();
 		fdlEnclosure.left = new FormAttachment(0, 0);
 		fdlEnclosure.top  = new FormAttachment(wSeparator, margin);
 		fdlEnclosure.right= new FormAttachment(middle, -margin);
 		wlEnclosure.setLayoutData(fdlEnclosure);
 		wEnclosure=new Text(wContentComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
  		props.setLook(wEnclosure);
 		wEnclosure.addModifyListener(lsMod);
 		fdEnclosure=new FormData();
 		fdEnclosure.left = new FormAttachment(middle, 0);
 		fdEnclosure.top  = new FormAttachment(wSeparator, margin);
 		fdEnclosure.right= new FormAttachment(100, 0);
 		wEnclosure.setLayoutData(fdEnclosure);
 
         wlEnclForced=new Label(wContentComp, SWT.RIGHT);
         wlEnclForced.setText(Messages.getString("TextFileOutputDialog.EnclForced.Label"));
         props.setLook(wlEnclForced);
         fdlEnclForced=new FormData();
         fdlEnclForced.left = new FormAttachment(0, 0);
         fdlEnclForced.top  = new FormAttachment(wEnclosure, margin);
         fdlEnclForced.right= new FormAttachment(middle, -margin);
         wlEnclForced.setLayoutData(fdlEnclForced);
         wEnclForced=new Button(wContentComp, SWT.CHECK );
         props.setLook(wEnclForced);
         fdEnclForced=new FormData();
         fdEnclForced.left = new FormAttachment(middle, 0);
         fdEnclForced.top  = new FormAttachment(wEnclosure, margin);
         fdEnclForced.right= new FormAttachment(100, 0);
         wEnclForced.setLayoutData(fdEnclForced);
         wEnclForced.addSelectionListener(new SelectionAdapter() 
             {
                 public void widgetSelected(SelectionEvent e) 
                 {
                     input.setChanged();
                 }
             }
         );
 
 		wlHeader=new Label(wContentComp, SWT.RIGHT);
 		wlHeader.setText(Messages.getString("TextFileOutputDialog.Header.Label"));
  		props.setLook(wlHeader);
 		fdlHeader=new FormData();
 		fdlHeader.left = new FormAttachment(0, 0);
 		fdlHeader.top  = new FormAttachment(wEnclForced, margin);
 		fdlHeader.right= new FormAttachment(middle, -margin);
 		wlHeader.setLayoutData(fdlHeader);
 		wHeader=new Button(wContentComp, SWT.CHECK );
  		props.setLook(wHeader);
 		fdHeader=new FormData();
 		fdHeader.left = new FormAttachment(middle, 0);
 		fdHeader.top  = new FormAttachment(wEnclForced, margin);
 		fdHeader.right= new FormAttachment(100, 0);
 		wHeader.setLayoutData(fdHeader);
 		wHeader.addSelectionListener(new SelectionAdapter() 
 			{
 				public void widgetSelected(SelectionEvent e) 
 				{
 					input.setChanged();
 				}
 			}
 		);
 
 		wlFooter=new Label(wContentComp, SWT.RIGHT);
 		wlFooter.setText(Messages.getString("TextFileOutputDialog.Footer.Label"));
  		props.setLook(wlFooter);
 		fdlFooter=new FormData();
 		fdlFooter.left = new FormAttachment(0, 0);
 		fdlFooter.top  = new FormAttachment(wHeader, margin);
 		fdlFooter.right= new FormAttachment(middle, -margin);
 		wlFooter.setLayoutData(fdlFooter);
 		wFooter=new Button(wContentComp, SWT.CHECK );
  		props.setLook(wFooter);
 		fdFooter=new FormData();
 		fdFooter.left = new FormAttachment(middle, 0);
 		fdFooter.top  = new FormAttachment(wHeader, margin);
 		fdFooter.right= new FormAttachment(100, 0);
 		wFooter.setLayoutData(fdFooter);
 		wFooter.addSelectionListener(new SelectionAdapter() 
 			{
 				public void widgetSelected(SelectionEvent e) 
 				{
 					input.setChanged();
 				}
 			}
 		);
 
 		wlFormat=new Label(wContentComp, SWT.RIGHT);
 		wlFormat.setText(Messages.getString("TextFileOutputDialog.Format.Label"));
  		props.setLook(wlFormat);
 		fdlFormat=new FormData();
 		fdlFormat.left = new FormAttachment(0, 0);
 		fdlFormat.top  = new FormAttachment(wFooter, margin);
 		fdlFormat.right= new FormAttachment(middle, -margin);
 		wlFormat.setLayoutData(fdlFormat);
 		wFormat=new CCombo(wContentComp, SWT.BORDER | SWT.READ_ONLY);
 		wFormat.setText(Messages.getString("TextFileOutputDialog.Format.Label"));
  		props.setLook(wFormat);
 
 		wFormat.add("DOS");
 		wFormat.add("Unix");
 		wFormat.select(0);
 		wFormat.addModifyListener(lsMod);
 		fdFormat=new FormData();
 		fdFormat.left = new FormAttachment(middle, 0);
 		fdFormat.top  = new FormAttachment(wFooter, margin);
 		fdFormat.right= new FormAttachment(100, 0);
 		wFormat.setLayoutData(fdFormat);
 
 		wlCompression=new Label(wContentComp, SWT.RIGHT);
 		wlCompression.setText(Messages.getString("TextFileOutputDialog.Compression.Label"));
  		props.setLook(wlCompression);
 		fdlCompression=new FormData();
 		fdlCompression.left = new FormAttachment(0, 0);
 		fdlCompression.top  = new FormAttachment(wFormat, margin);
 		fdlCompression.right= new FormAttachment(middle, -margin);
 		wlCompression.setLayoutData(fdlCompression);
 		wCompression=new CCombo(wContentComp, SWT.BORDER | SWT.READ_ONLY);
 		wCompression.setText(Messages.getString("TextFileOutputDialog.Compression.Label"));
  		props.setLook(wCompression);
 
 		wCompression.add("None");
 		wCompression.add("Zip");
 		wCompression.add("GZip");
 		wCompression.select(0);
 		wCompression.addModifyListener(lsMod);
 		fdCompression=new FormData();
 		fdCompression.left = new FormAttachment(middle, 0);
 		fdCompression.top  = new FormAttachment(wFormat, margin);
 		fdCompression.right= new FormAttachment(100, 0);
 		wCompression.setLayoutData(fdCompression);
 
         wlEncoding=new Label(wContentComp, SWT.RIGHT);
         wlEncoding.setText(Messages.getString("TextFileOutputDialog.Encoding.Label"));
         props.setLook(wlEncoding);
         fdlEncoding=new FormData();
         fdlEncoding.left = new FormAttachment(0, 0);
        fdlEncoding.top  = new FormAttachment(wCompression, margin);
         fdlEncoding.right= new FormAttachment(middle, -margin);
         wlEncoding.setLayoutData(fdlEncoding);
         wEncoding=new CCombo(wContentComp, SWT.BORDER | SWT.READ_ONLY);
         wEncoding.setEditable(true);
         props.setLook(wEncoding);
         wEncoding.addModifyListener(lsMod);
         fdEncoding=new FormData();
         fdEncoding.left = new FormAttachment(middle, 0);
         fdEncoding.top  = new FormAttachment(wCompression, margin);
         fdEncoding.right= new FormAttachment(100, 0);
         wEncoding.setLayoutData(fdEncoding);
         wEncoding.addFocusListener(new FocusListener()
             {
                 public void focusLost(org.eclipse.swt.events.FocusEvent e)
                 {
                 }
             
                 public void focusGained(org.eclipse.swt.events.FocusEvent e)
                 {
                     Cursor busy = new Cursor(shell.getDisplay(), SWT.CURSOR_WAIT);
                     shell.setCursor(busy);
                     setEncodings();
                     shell.setCursor(null);
                     busy.dispose();
                 }
             }
         );
 
         
 		wlPad=new Label(wContentComp, SWT.RIGHT);
 		wlPad.setText(Messages.getString("TextFileOutputDialog.Pad.Label"));
  		props.setLook(wlPad);
 		fdlPad=new FormData();
 		fdlPad.left = new FormAttachment(0, 0);
 		fdlPad.top  = new FormAttachment(wEncoding, margin);
 		fdlPad.right= new FormAttachment(middle, -margin);
 		wlPad.setLayoutData(fdlPad);
 		wPad=new Button(wContentComp, SWT.CHECK );
  		props.setLook(wPad);
 		fdPad=new FormData();
 		fdPad.left = new FormAttachment(middle, 0);
 		fdPad.top  = new FormAttachment(wEncoding, margin);
 		fdPad.right= new FormAttachment(100, 0);
 		wPad.setLayoutData(fdPad);
 		wPad.addSelectionListener(new SelectionAdapter() 
 			{
 				public void widgetSelected(SelectionEvent e) 
 				{
 					input.setChanged();
 				}
 			}
 		);
 
 
 		wlSplitEvery=new Label(wContentComp, SWT.RIGHT);
 		wlSplitEvery.setText(Messages.getString("TextFileOutputDialog.SplitEvery.Label"));
  		props.setLook(wlSplitEvery);
 		fdlSplitEvery=new FormData();
 		fdlSplitEvery.left = new FormAttachment(0, 0);
 		fdlSplitEvery.top  = new FormAttachment(wPad, margin);
 		fdlSplitEvery.right= new FormAttachment(middle, -margin);
 		wlSplitEvery.setLayoutData(fdlSplitEvery);
 		wSplitEvery=new Text(wContentComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
  		props.setLook(wSplitEvery);
 		wSplitEvery.addModifyListener(lsMod);
 		fdSplitEvery=new FormData();
 		fdSplitEvery.left = new FormAttachment(middle, 0);
 		fdSplitEvery.top  = new FormAttachment(wPad, margin);
 		fdSplitEvery.right= new FormAttachment(100, 0);
 		wSplitEvery.setLayoutData(fdSplitEvery);
 
 		//Bruise:
 		wlEndedLine=new Label(wContentComp, SWT.RIGHT);
 		wlEndedLine.setText(Messages.getString("TextFileOutputDialog.EndedLine.Label"));
  		props.setLook(wlEndedLine);
 		fdlEndedLine=new FormData();
 		fdlEndedLine.left = new FormAttachment(0, 0);
 		fdlEndedLine.top  = new FormAttachment(wSplitEvery, margin);
 		fdlEndedLine.right= new FormAttachment(middle, -margin);
 		wlEndedLine.setLayoutData(fdlEndedLine);
 		wEndedLine=new Text(wContentComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
  		props.setLook(wEndedLine);
  		wEndedLine.addModifyListener(lsMod);
 		fdEndedLine=new FormData();
 		fdEndedLine.left = new FormAttachment(middle, 0);
 		fdEndedLine.top  = new FormAttachment(wSplitEvery, margin);
 		fdEndedLine.right= new FormAttachment(100, 0);
 		wEndedLine.setLayoutData(fdEndedLine);
 		
 		fdContentComp = new FormData();
 		fdContentComp.left  = new FormAttachment(0, 0);
 		fdContentComp.top   = new FormAttachment(0, 0);
 		fdContentComp.right = new FormAttachment(100, 0);
 		fdContentComp.bottom= new FormAttachment(100, 0);
 		wContentComp.setLayoutData(fdContentComp);
 
 		wContentComp.layout();
 		wContentTab.setControl(wContentComp);
 
 		/////////////////////////////////////////////////////////////
 		/// END OF CONTENT TAB
 		/////////////////////////////////////////////////////////////
 
 		// Fields tab...
 		//
 		wFieldsTab = new CTabItem(wTabFolder, SWT.NONE);
 		wFieldsTab.setText(Messages.getString("TextFileOutputDialog.FieldsTab.TabTitle"));
 		
 		FormLayout fieldsLayout = new FormLayout ();
 		fieldsLayout.marginWidth  = Const.FORM_MARGIN;
 		fieldsLayout.marginHeight = Const.FORM_MARGIN;
 		
 		Composite wFieldsComp = new Composite(wTabFolder, SWT.NONE);
 		wFieldsComp.setLayout(fieldsLayout);
  		props.setLook(wFieldsComp);
 
 		wGet=new Button(wFieldsComp, SWT.PUSH);
 		wGet.setText(Messages.getString("System.Button.GetFields"));
 		wGet.setToolTipText(Messages.getString("System.Tooltip.GetFields"));
 
 		wMinWidth =new Button(wFieldsComp, SWT.PUSH);
 		wMinWidth.setText(Messages.getString("TextFileOutputDialog.MinWidth.Button"));
 		wMinWidth.setToolTipText(Messages.getString("TextFileOutputDialog.MinWidth.Tooltip"));
 
 		setButtonPositions(new Button[] { wGet, wMinWidth}, margin, null);
 
 		final int FieldsCols=9;
 		final int FieldsRows=input.getOutputFields().length;
 		
 		// Prepare a list of possible formats...
 		String dats[] = Const.dateFormats;
 		String nums[] = Const.numberFormats;
 		int totsize = dats.length + nums.length;
 		String formats[] = new String[totsize];
 		for (int x=0;x<dats.length;x++) formats[x] = dats[x];
 		for (int x=0;x<nums.length;x++) formats[dats.length+x] = nums[x];
 		
 		ColumnInfo[] colinf=new ColumnInfo[FieldsCols];
 		colinf[0]=new ColumnInfo(Messages.getString("TextFileOutputDialog.NameColumn.Column"),       ColumnInfo.COLUMN_TYPE_TEXT,   false);
 		colinf[1]=new ColumnInfo(Messages.getString("TextFileOutputDialog.TypeColumn.Column"),       ColumnInfo.COLUMN_TYPE_CCOMBO, Value.getTypes() );
 		colinf[2]=new ColumnInfo(Messages.getString("TextFileOutputDialog.FormatColumn.Column"),     ColumnInfo.COLUMN_TYPE_CCOMBO, formats);
 		colinf[3]=new ColumnInfo(Messages.getString("TextFileOutputDialog.LengthColumn.Column"),     ColumnInfo.COLUMN_TYPE_TEXT,   false);
 		colinf[4]=new ColumnInfo(Messages.getString("TextFileOutputDialog.PrecisionColumn.Column"),  ColumnInfo.COLUMN_TYPE_TEXT,   false);
 		colinf[5]=new ColumnInfo(Messages.getString("TextFileOutputDialog.CurrencyColumn.Column"),   ColumnInfo.COLUMN_TYPE_TEXT,   false);
 		colinf[6]=new ColumnInfo(Messages.getString("TextFileOutputDialog.DecimalColumn.Column"),    ColumnInfo.COLUMN_TYPE_TEXT,   false);
 		colinf[7]=new ColumnInfo(Messages.getString("TextFileOutputDialog.GroupColumn.Column"),      ColumnInfo.COLUMN_TYPE_TEXT,   false);
 		colinf[8]=new ColumnInfo(Messages.getString("TextFileOutputDialog.NullColumn.Column"),       ColumnInfo.COLUMN_TYPE_TEXT,   false);
 		
 		wFields=new TableView(wFieldsComp, 
 						      SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI, 
 						      colinf, 
 						      FieldsRows,  
 						      lsMod,
 							  props
 						      );
 
 		fdFields=new FormData();
 		fdFields.left  = new FormAttachment(0, 0);
 		fdFields.top   = new FormAttachment(0, 0);
 		fdFields.right = new FormAttachment(100, 0);
 		fdFields.bottom= new FormAttachment(wGet, -margin);
 		wFields.setLayoutData(fdFields);
 
 		fdFieldsComp=new FormData();
 		fdFieldsComp.left  = new FormAttachment(0, 0);
 		fdFieldsComp.top   = new FormAttachment(0, 0);
 		fdFieldsComp.right = new FormAttachment(100, 0);
 		fdFieldsComp.bottom= new FormAttachment(100, 0);
 		wFieldsComp.setLayoutData(fdFieldsComp);
 
 		wFieldsComp.layout();
 		wFieldsTab.setControl(wFieldsComp);
 
 		fdTabFolder = new FormData();
 		fdTabFolder.left  = new FormAttachment(0, 0);
 		fdTabFolder.top   = new FormAttachment(wStepname, margin);
 		fdTabFolder.right = new FormAttachment(100, 0);
 		fdTabFolder.bottom= new FormAttachment(100, -50);
 		wTabFolder.setLayoutData(fdTabFolder);
 		
 		wOK=new Button(shell, SWT.PUSH);
 		wOK.setText(Messages.getString("System.Button.OK"));
 
 		wCancel=new Button(shell, SWT.PUSH);
 		wCancel.setText(Messages.getString("System.Button.Cancel"));
 
 		setButtonPositions(new Button[] { wOK, wCancel }, margin, wTabFolder);
 
 		// Add listeners
 		lsOK       = new Listener() { public void handleEvent(Event e) { ok();       } };
 		lsGet      = new Listener() { public void handleEvent(Event e) { get();      } };
 		lsMinWidth    = new Listener() { public void handleEvent(Event e) { setMinimalWidth(); } };
 		lsCancel   = new Listener() { public void handleEvent(Event e) { cancel();   } };
 		
 		wOK.addListener    (SWT.Selection, lsOK    );
 		wGet.addListener   (SWT.Selection, lsGet   );
 		wMinWidth.addListener (SWT.Selection, lsMinWidth );
 		wCancel.addListener(SWT.Selection, lsCancel);
 
 		lsDef=new SelectionAdapter() { public void widgetDefaultSelected(SelectionEvent e) { ok(); } };
 
 		wStepname.addSelectionListener( lsDef );
 		wFilename.addSelectionListener( lsDef );
 		wSeparator.addSelectionListener( lsDef );
 
 		// Whenever something changes, set the tooltip to the expanded version:
 		wFilename.addModifyListener(new ModifyListener()
 			{
 				public void modifyText(ModifyEvent e)
 				{
 					wFilename.setToolTipText(StringUtil.environmentSubstitute( wFilename.getText() ) );
 				}
 			}
 		);
 		
 
 		// Listen to the Variable... button
 		wbcFilename.addSelectionListener(VariableButtonListenerFactory.getSelectionAdapter(shell, wFilename));
 
 		wbFilename.addSelectionListener
 		(
 			new SelectionAdapter()
 			{
 				public void widgetSelected(SelectionEvent e) 
 				{
 					FileDialog dialog = new FileDialog(shell, SWT.OPEN);
 					dialog.setFilterExtensions(new String[] {"*.txt", "*.csv", "*"});
 					if (wFilename.getText()!=null)
 					{
 						dialog.setFileName(StringUtil.environmentSubstitute(wFilename.getText()));
 					}
 					dialog.setFilterNames(new String[] {Messages.getString("System.FileType.TextFiles"), Messages.getString("System.FileType.CSVFiles"), Messages.getString("System.FileType.AllFiles")});
 					if (dialog.open()!=null)
 					{
 						wFilename.setText(dialog.getFilterPath()+System.getProperty("file.separator")+dialog.getFileName());
 					}
 				}
 			}
 		);
 		
 		// Detect X or ALT-F4 or something that kills this window...
 		shell.addShellListener(	new ShellAdapter() { public void shellClosed(ShellEvent e) { cancel(); } } );
 
 		lsResize = new Listener() 
 		{
 			public void handleEvent(Event event) 
 			{
 				Point size = shell.getSize();
 				wFields.setSize(size.x-10, size.y-50);
 				wFields.table.setSize(size.x-10, size.y-50);
 				wFields.redraw();
 			}
 		};
 		shell.addListener(SWT.Resize, lsResize);
 
 		wTabFolder.setSelection(0);
 		
 		// Set the shell size, based upon previous time...
 		setSize();
 		
 		getData();
 		input.setChanged(changed);
 		
 		shell.open();
 		while (!shell.isDisposed())
 		{
 				if (!display.readAndDispatch()) display.sleep();
 		}
 		return stepname;
 	}
 	
     private void setEncodings()
     {
         // Encoding of the text file:
         if (!gotEncodings)
         {
             gotEncodings = true;
             
             wEncoding.removeAll();
             ArrayList values = new ArrayList(Charset.availableCharsets().values());
             for (int i=0;i<values.size();i++)
             {
                 Charset charSet = (Charset)values.get(i);
                 wEncoding.add( charSet.displayName() );
             }
             
             // Now select the default!
             String defEncoding = Const.getEnvironmentVariable("file.encoding", "UTF-8");
             int idx = Const.indexOfString(defEncoding, wEncoding.getItems() );
             if (idx>=0) wEncoding.select( idx );
         }
     }
 
 
     /**
 	 * Copy information from the meta-data input to the dialog fields.
 	 */ 
 	public void getData()
 	{
 		if (input.getFileName()  != null) wFilename.setText(input.getFileName());
 		if (input.getExtension() != null) wExtension.setText(input.getExtension());
 		if (input.getSeparator() !=null) wSeparator.setText(input.getSeparator());
 		if (input.getEnclosure() !=null) wEnclosure.setText(input.getEnclosure());
 		if (input.getFileFormat()!=null) wFormat.setText(input.getFileFormat());
 		if (input.getFileCompression()!=null) wCompression.setText(input.getFileCompression());
         if (input.getEncoding()  !=null) wEncoding.setText(input.getEncoding());
         if (input.getEndedLine() !=null) wEndedLine.setText(input.getEndedLine());
         
 		wSplitEvery.setText(""+input.getSplitEvery());
 
         wEnclForced.setSelection(input.isEnclosureForced());
 		wHeader.setSelection(input.isHeaderEnabled());
 		wFooter.setSelection(input.isFooterEnabled());
 		wAddDate.setSelection(input.isDateInFilename());
 		wAddTime.setSelection(input.isTimeInFilename());
 		wAppend.setSelection(input.isFileAppended());
 		wAddStepnr.setSelection(input.isStepNrInFilename());
 		wPad.setSelection(input.isPadded());
 		
 		log.logDebug(toString(), "getting fields info...");
 		
 		for (int i=0;i<input.getOutputFields().length;i++)
 		{
 		    TextFileField field = input.getOutputFields()[i];
 		    
 			TableItem item = wFields.table.getItem(i);
 			if (field.getName()!=null) item.setText(1, field.getName());
 			item.setText(2, field.getTypeDesc());
 			if (field.getFormat()!=null) item.setText(3, field.getFormat());
 			if (field.getLength()!=-1) item.setText(4, ""+field.getLength());
 			if (field.getPrecision()!=-1) item.setText(5, ""+field.getPrecision());
 			if (field.getCurrencySymbol()!=null) item.setText(6, field.getCurrencySymbol());
 			if (field.getDecimalSymbol()!=null) item.setText(7, field.getDecimalSymbol());
 			if (field.getGroupingSymbol()!=null) item.setText(8, field.getGroupingSymbol());
 			if (field.getNullString()!=null) item.setText(9, field.getNullString());
 		}
 		
 		wFields.optWidth(true);
 		wStepname.selectAll();
 	}
 	
 	private void cancel()
 	{
 		stepname=null;
 		
 		input.setChanged(backupChanged);
 
 		dispose();
 	}
 	
 	private void getInfo(TextFileOutputMeta tfoi)
 	{
 		tfoi.setFileName(   wFilename.getText() );
 		tfoi.setFileFormat( wFormat.getText() );
 		tfoi.setFileCompression( wCompression.getText() );
         tfoi.setEncoding( wEncoding.getText() );
 		tfoi.setSeparator(  wSeparator.getText() );
 		tfoi.setEnclosure(  wEnclosure.getText() );
 		tfoi.setExtension(  wExtension.getText() );
 		tfoi.setSplitEvery( Const.toInt(wSplitEvery.getText(), 0) );
 		tfoi.setEndedLine( wEndedLine.getText() );
 
         tfoi.setEnclosureForced( wEnclForced.getSelection() ); 
 		tfoi.setHeaderEnabled( wHeader.getSelection() ); 
 		tfoi.setFooterEnabled( wFooter.getSelection() );
 		tfoi.setFileAppended( wAppend.getSelection() );
 		tfoi.setStepNrInFilename( wAddStepnr.getSelection() );
 		tfoi.setDateInFilename( wAddDate.getSelection() );
 		tfoi.setTimeInFilename( wAddTime.getSelection() );
 		tfoi.setPadded( wPad.getSelection() );
 
 		int i;
 		//Table table = wFields.table;
 		
 		int nrfields = wFields.nrNonEmpty();
 
 		tfoi.allocate(nrfields);
 		
 		for (i=0;i<nrfields;i++)
 		{
 		    TextFileField field = new TextFileField();
 		    
 			TableItem item = wFields.getNonEmpty(i);
 			field.setName( item.getText(1) );
 			field.setType( item.getText(2) );
 			field.setFormat( item.getText(3) );
 			field.setLength( Const.toInt(item.getText(4), -1) );
 			field.setPrecision( Const.toInt(item.getText(5), -1) );
 			field.setCurrencySymbol( item.getText(6) );
 			field.setDecimalSymbol( item.getText(7) );
 			field.setGroupingSymbol( item.getText(8) );
 			field.setNullString( item.getText(9) );
 			
 			tfoi.getOutputFields()[i]  = field;
 		}
 	}
 	
 	private void ok()
 	{
 		stepname = wStepname.getText(); // return value
 		
 		getInfo(input);
 		
 		dispose();
 	}
 	
 	private void get()
 	{
 		try
 		{
 			Row r = transMeta.getPrevStepFields(stepname);
 			if (r!=null)
 			{
 				Table table=wFields.table;
 				int count=table.getItemCount();
 				
 				for (int i=0;i<r.size();i++)
 				{
 					Value v = r.getValue(i);
 					TableItem ti = new TableItem(table, SWT.NONE);
 					ti.setText(0, ""+(count+i+1));
 					ti.setText(1, v.getName());
 					ti.setText(2, v.getTypeDesc());
 					if (v.isNumber())
 					{
 						if (v.getLength()>0)
 						{
 							int le=v.getLength();
 							int pr=v.getPrecision();
 							
 							if (v.getPrecision()<=0)
 							{
 								pr=0;
 							}
 							
 							String mask="";
 							for (int m=0;m<le-pr;m++)
 							{
 								mask+="0";
 							}
 							if (pr>0) mask+=".";
 							for (int m=0;m<pr;m++)
 							{
 								mask+="0";
 							}
 							ti.setText(3, mask);
 						}
 					}
 					ti.setText(4, ""+v.getLength());
 					ti.setText(5, ""+v.getPrecision());
 				}
 				wFields.removeEmptyRows();
 				wFields.setRowNums();
 				wFields.optWidth(true);
 			}
 		}
 		catch(KettleException ke)
 		{
 			new ErrorDialog(shell, Messages.getString("System.Dialog.GetFieldsFailed.Title"), Messages.getString("System.Dialog.GetFieldsFailed.Message"), ke);
 		}
 
 	}
 	
 	/**
 	 * Sets the output width to minimal width...
 	 *
 	 */
 	public void setMinimalWidth()
 	{
 		
 		for (int i=0;i<wFields.nrNonEmpty();i++)
 		{
 			TableItem item = wFields.getNonEmpty(i);
 			
 			item.setText(4, "");
 			item.setText(5, "");
 			
 			int type = Value.getType(item.getText(2));
 			switch(type)
 			{
 			case Value.VALUE_TYPE_STRING:  item.setText(3, ""); break;
 			case Value.VALUE_TYPE_INTEGER: item.setText(3, "0"); break;
 			case Value.VALUE_TYPE_NUMBER: item.setText(3, "0.#####"); break;
 			case Value.VALUE_TYPE_DATE: break;
 			default: break;
 			}
 		}
 		wFields.optWidth(true);
 	}
 
 	public String toString()
 	{
 		return this.getClass().getName();
 	}
 }
