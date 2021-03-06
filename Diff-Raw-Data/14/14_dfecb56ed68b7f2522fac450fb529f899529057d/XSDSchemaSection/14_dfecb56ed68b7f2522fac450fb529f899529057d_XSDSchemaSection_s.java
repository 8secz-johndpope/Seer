 /*******************************************************************************
  * Copyright (c) 2001, 2006 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.wst.xsd.ui.internal.common.properties.sections;
 
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.custom.CLabel;
 import org.eclipse.swt.custom.StyleRange;
 import org.eclipse.swt.custom.StyledText;
 import org.eclipse.swt.events.SelectionEvent;
 import org.eclipse.swt.graphics.Color;
 import org.eclipse.swt.layout.GridData;
 import org.eclipse.swt.layout.GridLayout;
 import org.eclipse.swt.widgets.Button;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Event;
 import org.eclipse.swt.widgets.Text;
 import org.eclipse.ui.IWorkbenchPart;
 import org.eclipse.wst.xsd.ui.internal.actions.XSDEditNamespacesAction;
 import org.eclipse.wst.xsd.ui.internal.common.commands.UpdateNamespaceInformationCommand;
 import org.eclipse.wst.xsd.ui.internal.editor.XSDEditorPlugin;
 import org.eclipse.wst.xsd.ui.internal.util.TypesHelper;
 import org.eclipse.xsd.util.XSDConstants;
 import org.w3c.dom.Element;
 
 public class XSDSchemaSection extends AbstractSection
 {
   IWorkbenchPart part;
   Text prefixText;
   Text targetNamespaceText;
   Button editButton;
   StyledText errorText;
   Color red;
 
   /**
    * 
    */
   public XSDSchemaSection()
   {
     super();
   }
 
   /**
    * @see org.eclipse.wst.common.ui.properties.internal.provisional.ITabbedPropertySection#createControls(org.eclipse.swt.widgets.Composite,
    *      org.eclipse.wst.common.ui.properties.internal.provisional.TabbedPropertySheetWidgetFactory)
    */
   public void createContents(Composite parent)
   {
     composite = getWidgetFactory().createFlatFormComposite(parent);
 
     GridLayout gridLayout = new GridLayout();
     gridLayout.marginTop = 0;
     gridLayout.marginBottom = 0;
     gridLayout.numColumns = 2;
     composite.setLayout(gridLayout);
 
     GridData data = new GridData();
 
     // Create Prefix Label
     CLabel prefixLabel = getWidgetFactory().createCLabel(composite, XSDEditorPlugin.getXSDString("_UI_LABEL_SCHEMA_PREFIX")); //$NON-NLS-1$
     data.horizontalAlignment = GridData.HORIZONTAL_ALIGN_BEGINNING;
     data.grabExcessHorizontalSpace = false;
     prefixLabel.setLayoutData(data);
 
     // Create Prefix Text
     prefixText = getWidgetFactory().createText(composite, "", SWT.NONE); //$NON-NLS-1$
     data = new GridData();
     data.grabExcessHorizontalSpace = true;
     data.horizontalAlignment = GridData.FILL;
     prefixText.setLayoutData(data);
    prefixText.addListener(SWT.Modify, this);
 
     // Create TargetNamespace Label
     CLabel targetNamespaceLabel = getWidgetFactory().createCLabel(composite, XSDEditorPlugin.getXSDString("_UI_LABEL_TARGET_NAME_SPACE")); //$NON-NLS-1$
     data = new GridData();
     data.horizontalAlignment = GridData.HORIZONTAL_ALIGN_BEGINNING;
     data.grabExcessHorizontalSpace = false;
     targetNamespaceLabel.setLayoutData(data);
 
     // Create TargetNamespace Text
     targetNamespaceText = getWidgetFactory().createText(composite, "", SWT.NONE); //$NON-NLS-1$
     data = new GridData();
     data.grabExcessHorizontalSpace = true;
     data.horizontalAlignment = GridData.FILL;
     targetNamespaceText.setLayoutData(data);
    targetNamespaceText.addListener(SWT.Modify, this);
 
     // Advanced Button
     editButton = getWidgetFactory().createButton(composite, XSDEditorPlugin.getXSDString("_UI_SECTION_ADVANCED_ATTRIBUTES") + "...", SWT.PUSH); //$NON-NLS-1$ //$NON-NLS-2$
     data = new GridData(SWT.END, SWT.CENTER, true, false);
     data.horizontalSpan = 2;
     editButton.setLayoutData(data);
     editButton.addSelectionListener(this);
 
     // error text
     errorText = new StyledText(composite, SWT.FLAT);
     errorText.setEditable(false);
     errorText.setEnabled(false);
     errorText.setText(""); //$NON-NLS-1$
     data = new GridData();
     data.horizontalAlignment = GridData.FILL;
     data.horizontalSpan = 2;
     data.grabExcessHorizontalSpace = true;
     errorText.setLayoutData(data);
 
   }
 
   /*
    * @see org.eclipse.wst.common.ui.properties.internal.provisional.view.ITabbedPropertySection#refresh()
    */
   public void refresh()
   {
     setListenerEnabled(false);
 
     Element element = xsdSchema.getElement();
 
     if (element != null)
     {
       // Handle prefixText
       TypesHelper helper = new TypesHelper(xsdSchema);
       String aPrefix = helper.getPrefix(element.getAttribute(XSDConstants.TARGETNAMESPACE_ATTRIBUTE), false);
 
       if (aPrefix != null && aPrefix.length() > 0)
       {
         prefixText.setText(aPrefix);
       }
       else
       {
         prefixText.setText(""); //$NON-NLS-1$
       }
 
       // Handle TargetNamespaceText
       String tns = element.getAttribute(XSDConstants.TARGETNAMESPACE_ATTRIBUTE);
       if (tns != null && tns.length() > 0)
       {
         targetNamespaceText.setText(tns);
       }
       else
       {
         targetNamespaceText.setText(""); //$NON-NLS-1$
       }
       errorText.setText(""); //$NON-NLS-1$
     }
     setListenerEnabled(true);
   }
 
   public void doHandleEvent(Event event)
   {
     errorText.setText(""); //$NON-NLS-1$
     String prefixValue = prefixText.getText();
     String tnsValue = targetNamespaceText.getText();
     if (tnsValue.trim().length() == 0)
     {
       if (prefixValue.trim().length() > 0)
       {
         errorText.setText(XSDEditorPlugin.getXSDString("_ERROR_TARGET_NAMESPACE_AND_PREFIX")); //$NON-NLS-1$
         int length = errorText.getText().length();
         red = new Color(null, 255, 0, 0);
         StyleRange style = new StyleRange(0, length, red, targetNamespaceText.getBackground());
         errorText.setStyleRange(style);
         return;
       }
     }
 
     if (event.widget == prefixText)
     {
       updateNamespaceInfo(prefixValue, tnsValue);
     }
     else if (event.widget == targetNamespaceText)
     {
       updateNamespaceInfo(prefixValue, tnsValue);
     }
   }
 
   public void doWidgetSelected(SelectionEvent e)
   {
     if (e.widget == editButton)
     {
       XSDEditNamespacesAction nsAction = new XSDEditNamespacesAction(XSDEditorPlugin.getXSDString("_UI_ACTION_EDIT_NAMESPACES"), xsdSchema.getElement(), null, xsdSchema); //$NON-NLS-1$ 
       nsAction.run();
       refresh();
     }
   }
 
   /*
    * (non-Javadoc)
    * 
    * @see org.eclipse.wst.common.ui.properties.internal.provisional.ISection#shouldUseExtraSpace()
    */
   public boolean shouldUseExtraSpace()
   {
     return true;
   }
 
   private void updateNamespaceInfo(String newPrefix, String newTargetNamespace)
   {
     UpdateNamespaceInformationCommand command = new UpdateNamespaceInformationCommand("", xsdSchema, newPrefix, newTargetNamespace);
     command.execute();
   }
 
   public void dispose()
   {
    super.dispose();
     if (red != null)
     {
       red.dispose();
       red = null;
     }
   }
 
   /**
    * @deprecated
    */
   protected boolean validatePrefix(String prefix)
   {
     return true;
   }
 
 }
