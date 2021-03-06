 /*******************************************************************************
  * <copyright>
  *
 * Copyright (c) 2005, 2010 SAP AG.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *    SAP AG - initial API, implementation and documentation
  *
  * </copyright>
  *
  *******************************************************************************/
 package org.eclipse.graphiti.ui.internal.action;
 
 import org.eclipse.gef.GraphicalViewer;
 import org.eclipse.gef.ui.actions.PrintAction;
 import org.eclipse.graphiti.features.IPrintFeature;
 import org.eclipse.graphiti.features.context.IPrintContext;
 import org.eclipse.graphiti.features.context.impl.PrintContext;
 import org.eclipse.graphiti.ui.internal.services.GraphitiUiInternal;
 import org.eclipse.graphiti.ui.internal.util.ui.print.PrintFigureDialog;
 import org.eclipse.graphiti.ui.internal.util.ui.print.PrintFigureScaleableOperation;
 import org.eclipse.jface.action.IAction;
 import org.eclipse.jface.window.Window;
 import org.eclipse.swt.printing.Printer;
 import org.eclipse.swt.printing.PrinterData;
 import org.eclipse.swt.widgets.Shell;
 import org.eclipse.ui.IWorkbenchPart;
 import org.eclipse.ui.PlatformUI;
 import org.eclipse.ui.actions.ActionFactory;
 
 /**
  * An Action, which is used to print the contents of the GraphicalViewer.
  * <p>
  * Additional to the usual printing behaviour (the dialog, where the printer can
  * be selected), this Action also adds another dialog, where the size and
  * position of the graphics on the paper can be specified.
  * 
  * @noinstantiate This class is not intended to be instantiated by clients.
  * @noextend This class is not intended to be subclassed by clients.
  */
 public class PrintGraphicalViewerAction extends PrintAction {
 
 	/**
 	 * This static Action is just used as a template, to initialize the ID,
 	 * label and image of instances of this class accordingly.
 	 */
 	private static IAction TEMPLATE_ACTION = ActionFactory.PRINT.create(PlatformUI.getWorkbench()
 			.getActiveWorkbenchWindow());
 
 	private IPrintFeature printFeature;
 
 	// last time when we checked whether a printer is available with
 	// super.calculateEnabled()
 	private long lastPrinterCheckTime = 0;
 
 	private boolean cachedEnabled = true;
 
 	/**
 	 * Creates a new PrintGraphicalViewerAction. It initializes it with the
 	 * proper ID, label and image.
 	 * 
 	 * @param configurationProvider
 	 *            The IConfigurationProvider.
 	 * @param part
 	 *            The WorkbenchPart (e.g. the editor), to which this Action
 	 *            belongs. From the WorkbenchPart the GraphicalViewer will be
 	 *            determined.
 	 */
 	public PrintGraphicalViewerAction(IWorkbenchPart part,
 			IPrintFeature printFeature) {
 		super(part);
 		this.printFeature = printFeature;
 
 		// set all values of the TEMPLATE_ACTION for this Action.
 		setId(TEMPLATE_ACTION.getId());
 		setText(TEMPLATE_ACTION.getText());
 		setToolTipText(TEMPLATE_ACTION.getToolTipText());
 		setDescription(TEMPLATE_ACTION.getDescription());
 		setAccelerator(TEMPLATE_ACTION.getAccelerator());
 		setHelpListener(TEMPLATE_ACTION.getHelpListener());
 		setImageDescriptor(TEMPLATE_ACTION.getImageDescriptor());
 		setHoverImageDescriptor(TEMPLATE_ACTION.getHoverImageDescriptor());
 		setDisabledImageDescriptor(TEMPLATE_ACTION.getDisabledImageDescriptor());
 	}
 
 	/**
 	 * Same as super.calculateEnabled(), except that it also checks, if the
 	 * current WorkbenchPart has a GraphicalViewer.
 	 */
 	@Override
 	protected boolean calculateEnabled() {
 		if (getWorkbenchPart().getAdapter(GraphicalViewer.class) == null)
 			return false;
 
 		long currentTime = System.currentTimeMillis();
 		long diffTime = (currentTime - lastPrinterCheckTime) / 1000;
 
		// super.calculateEnabled() only checks whether a printer is available.
		// But calculateEnabled() is called very often and in some environments
		// this can lead to performance issues. See also bugzilla 355401.
		// Therefore we cache the result and call the super method earliest
		// after 5 minutes.
 		if (diffTime > 300) {
 			lastPrinterCheckTime = currentTime;
			cachedEnabled = super.calculateEnabled();
 		}
 		return cachedEnabled;
 		// TODO ask also feature for canPrint() ?
 	}
 
 	/**
 	 * Prints the GraphicalViewer of the WorkbenchPart. It is the same as
 	 * super.run(), except that it opens the PrintModeDialog before printing. In
 	 * this dialog the size of the graphics (fit-width, fit-height, ...) can be
 	 * specified.
 	 */
 	@Override
 	public void run() {
 		IPrintContext printContext = new PrintContext();
 		printFeature.prePrint(printContext);
 
 		Shell shell = GraphitiUiInternal.getWorkbenchService().getShell();
 
 		// get viewer
 		GraphicalViewer viewer = (GraphicalViewer) getWorkbenchPart().getAdapter(GraphicalViewer.class);
 
 		// create default PrinterData
 		PrinterData printerData = Printer.getDefaultPrinterData();
 		if (printerData == null || (printerData.name == null && printerData.driver == null)) {
 			printerData = Printer.getPrinterList()[0];
 		}
 
 		// open PrintFigureDialog
 		PrintFigureDialog printImageDialog = new PrintFigureDialog(shell, viewer, new Printer(printerData));
 		printImageDialog.open();
 		if (printImageDialog.getReturnCode() != Window.CANCEL) {
 
 			// start the printing
 			PrintFigureScaleableOperation op = new PrintFigureScaleableOperation(printImageDialog.getPrinter(),
 					printImageDialog.getFigure(), printImageDialog.getScaledImage(), printImageDialog.getPreferences());
 			op.run(getWorkbenchPart().getTitle());
 			printImageDialog.cleanUp();
 		}
 
 		printFeature.postPrint(printContext);
 	}
 }
