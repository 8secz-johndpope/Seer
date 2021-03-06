 /**
  * Copyright (C) 2012 BonitaSoft S.A.
  * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 2.0 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 package org.bonitasoft.studio.expression.editor.widget;
 
 import org.bonitasoft.studio.common.jface.SWTBotConstants;
 import org.bonitasoft.studio.expression.editor.autocompletion.AutoCompletionField;
 import org.bonitasoft.studio.pics.Pics;
 import org.eclipse.jface.fieldassist.TextContentAdapter;
 import org.eclipse.jface.layout.GridDataFactory;
 import org.eclipse.jface.layout.GridLayoutFactory;
 import org.eclipse.jface.viewers.ILabelProvider;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.events.FocusEvent;
 import org.eclipse.swt.events.FocusListener;
 import org.eclipse.swt.events.PaintEvent;
 import org.eclipse.swt.events.PaintListener;
 import org.eclipse.swt.events.SelectionAdapter;
 import org.eclipse.swt.events.SelectionEvent;
 import org.eclipse.swt.graphics.GC;
 import org.eclipse.swt.graphics.Path;
 import org.eclipse.swt.graphics.Point;
 import org.eclipse.swt.graphics.Rectangle;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Control;
 import org.eclipse.swt.widgets.Display;
 import org.eclipse.swt.widgets.Text;
 import org.eclipse.swt.widgets.ToolBar;
 import org.eclipse.swt.widgets.ToolItem;
 
 /**
  * @author Romain Bioteau
  *
  */
 public class ContentAssistText extends Composite implements SWTBotConstants {
 
 
 	private Text textControl;
 	private AutoCompletionField autoCompletion;
 	private boolean drawBorder = true;
 	private ToolBar tb;
 
 	public ContentAssistText(Composite parent, ILabelProvider contentProposalLabelProvider, int style) {
 		super(parent, SWT.NONE);
 		Point margins = new Point(3, 3);
 		if ((style & SWT.BORDER) == 0){
 			drawBorder = false;
 			margins = new Point(0, 0);
 		}else{
 			style = style ^ SWT.BORDER;
 		}
 		setLayout(GridLayoutFactory.fillDefaults().numColumns(2).margins(margins).spacing(32, 0).create());
 		setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
 
 
 		textControl = new Text(this,style | SWT.SINGLE);
 
 		textControl.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
 		textControl.addFocusListener(new FocusListener() {
 			
 			@Override
 			public void focusLost(FocusEvent e) {
 				if(textControl.equals(e.widget)){
					ContentAssistText.this.redraw();
 				}
 			}
 			
 			@Override
 			public void focusGained(FocusEvent e) {
 				if(textControl.equals(e.widget)){
					ContentAssistText.this.redraw();
 				}
 			}
 		});
 		/*Data for test purpose*/
 		textControl.setData(SWTBOT_WIDGET_ID_KEY, SWTBOT_ID_EXPRESSIONVIEWER_TEXT);
 		tb = new ToolBar(this, SWT.FLAT | SWT.NO_FOCUS);
 		tb.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
 		tb.setLayoutData(GridDataFactory.swtDefaults().create());
 		final ToolItem ti = new ToolItem(tb, SWT.FLAT | SWT.NO_FOCUS);
 		ti.setData(SWTBOT_WIDGET_ID_KEY, SWTBOT_ID_EXPRESSIONVIEWER_DROPDOWN);
 		ti.setImage(Pics.getImage("resize_S.gif"));
 		ti.addSelectionListener(new SelectionAdapter() {
 
 			@Override
 			public void widgetSelected(SelectionEvent e) {
 				if(autoCompletion.getContentProposalAdapter().isProposalPopupOpen()){
 					autoCompletion.getContentProposalAdapter().closeProposalPopup();
 				}else{
 					autoCompletion.getContentProposalAdapter().showProposalPopup();
 				}
 			}
 		});
 	
 		addPaintListener(new PaintListener() {
 
 			@Override
 			public void paintControl(PaintEvent e) {
 				if(drawBorder){
 					paintControlBorder(e);
 				}
 			}
 		});
 		autoCompletion = new AutoCompletionField(textControl, new TextContentAdapter(), contentProposalLabelProvider) ;
 	}
 
 
 	protected void paintControlBorder(PaintEvent e) {
 		//if(ContentAssistText.this.equals(e.widget)){
 			GC gc = e.gc;
 			Display display = e.display ;
 			if(display!= null && gc != null && !gc.isDisposed()){
 				Control focused = display.getFocusControl() ;
 				GC parentGC  = gc;
 				parentGC.setAdvanced(true);
 				Rectangle r = ContentAssistText.this.getBounds();
 
 				//parentGC.setClipping(getBorderPath(r, display));
 				if(focused == null || !focused.getParent().equals(ContentAssistText.this)){
 					parentGC.setForeground(display.getSystemColor(SWT.COLOR_GRAY));
 				}else{
 					parentGC.setForeground(display.getSystemColor(SWT.COLOR_WIDGET_BORDER));
 				}
 				parentGC.setLineWidth(1);
 				parentGC.drawRectangle(0, 0, r.width-1, r.height-1);
 				
 			}
 		//}
 	}
 
 	private Path getBorderPath(Rectangle widgetBounds, Display display) {
 		final Path path = new Path(display);
 		path.addRectangle(0,0,1,widgetBounds.height);//Left border
 		path.addRectangle(1,0,widgetBounds.width-2,1);//Top border
 		path.addRectangle(1, widgetBounds.height-1, widgetBounds.width-2,1);//Bottom border
 		path.addRectangle(widgetBounds.width-1, 0, 1,widgetBounds.height);//Right border
 		return path;
 	}
 
 	public Text getTextControl() {
 		return textControl;
 	}
 
 	public AutoCompletionField getAutocompletion() {
 		return autoCompletion;
 	}
 
 	public ToolBar getToolbar() {
 		return tb;
 	}
 
 }
