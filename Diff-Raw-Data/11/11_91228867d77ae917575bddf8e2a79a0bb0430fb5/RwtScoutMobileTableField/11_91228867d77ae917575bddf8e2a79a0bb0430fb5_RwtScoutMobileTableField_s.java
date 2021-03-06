 /*******************************************************************************
  * Copyright (c) 2010 BSI Business Systems Integration AG.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     BSI Business Systems Integration AG - initial API and implementation
  ******************************************************************************/
 package org.eclipse.scout.rt.ui.rap.mobile.form.fields;
 
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.scout.commons.StringUtility;
 import org.eclipse.scout.commons.holders.Holder;
 import org.eclipse.scout.commons.logger.IScoutLogger;
 import org.eclipse.scout.commons.logger.ScoutLogManager;
 import org.eclipse.scout.rt.client.ClientSyncJob;
 import org.eclipse.scout.rt.client.mobile.ui.basic.table.MobileTable;
 import org.eclipse.scout.rt.client.mobile.ui.forms.OutlineChooserForm;
 import org.eclipse.scout.rt.client.ui.basic.table.ITable;
 import org.eclipse.scout.rt.client.ui.desktop.outline.IOutline;
 import org.eclipse.scout.rt.client.ui.desktop.outline.IOutlineTableForm;
 import org.eclipse.scout.rt.client.ui.form.IForm;
 import org.eclipse.scout.rt.shared.TEXTS;
 import org.eclipse.scout.rt.ui.rap.basic.table.IRwtScoutTable;
 import org.eclipse.scout.rt.ui.rap.form.fields.tablefield.RwtScoutTableField;
 import org.eclipse.scout.rt.ui.rap.window.desktop.IRwtScoutActionBar;
 
 /**
  * @since 3.8.0
  */
 public class RwtScoutMobileTableField extends RwtScoutTableField {
   private static final IScoutLogger LOG = ScoutLogManager.getLogger(RwtScoutMobileTableField.class);
 
   private MobileTable m_mobileTable;
   private boolean m_columnTransformationEnabled;
 
   public RwtScoutMobileTableField() {
     this(true);
   }
 
   public RwtScoutMobileTableField(boolean columnTransformationEnabled) {
     m_columnTransformationEnabled = columnTransformationEnabled;
   }
 
   @Override
   protected IRwtScoutTable createRwtScoutTable() {
     return new RwtScoutList();
   }
 
   @Override
   protected void setTableFromScout(ITable table) {
     if (!m_columnTransformationEnabled) {
       super.setTableFromScout(table);
       return;
     }
 
    disposeSingleColumnTable();
 
     if (table == null) {
       super.setTableFromScout(table);
       return;
     }
     else {
       m_mobileTable = wrapTable(table);
       super.setTableFromScout(m_mobileTable);
     }
   }
 
   private MobileTable wrapTable(final ITable table) {
     final Holder<MobileTable> holder = new Holder<MobileTable>(MobileTable.class);
 
     ClientSyncJob job = new ClientSyncJob("", getUiEnvironment().getClientSession()) {
       @Override
       protected void runVoid(IProgressMonitor monitor) throws Throwable {
         MobileTable wrapperTable = new MobileTable();
         String headerName = createColumnHeaderName(table);
         wrapperTable.setHeaderName(headerName);
         wrapperTable.setDrillDownPossible(computeDrillDownColumnVisibility());
         wrapperTable.installWrappedTable(table);
 
         holder.setValue(wrapperTable);
       }
     };
     job.schedule();
 
     try {
       job.join();
     }
     catch (InterruptedException e) {
       LOG.error("Table wrapping interrupted. ", e);
     }
 
     return holder.getValue();
   }
 
   private String createColumnHeaderName(ITable table) {
     String headerName = getScoutObject().getLabel();
     if (StringUtility.hasText(headerName)) {
       return headerName;
     }
     if (table.getVisibleColumnCount() == 1) {
       headerName = table.getColumnSet().getVisibleColumn(0).getHeaderCell().getText();
     }
     if (StringUtility.hasText(headerName)) {
       return headerName;
     }
 
    return TEXTS.get("SingleColumnTableDefaultHeader");
   }
 
   private boolean computeDrillDownColumnVisibility() {
     if (getScoutObject().getForm() instanceof OutlineChooserForm) {
       return true;
     }
 
     if (getScoutObject().getForm() instanceof IOutlineTableForm) {
       IOutline outline = getUiEnvironment().getClientSession().getDesktop().getOutline();
       if (outline.getActivePage() != null) {
         return !outline.getActivePage().isLeaf();
       }
     }
 
     return false;
   }
 
   @Override
   protected IRwtScoutActionBar createRwtScoutActionBar() {
     RwtScoutTableActionBar actionBar = new RwtScoutTableActionBar();
     actionBar.createUiField(getUiContainer(), getScoutObject(), getUiEnvironment());
     return actionBar;
   }
 
   @Override
   protected void detachScout() {
    disposeSingleColumnTable();
 
     super.detachScout();
   }
 
  private void disposeSingleColumnTable() {
     if (m_mobileTable == null) {
       return;
     }
 
     Runnable job = new Runnable() {
 
       @Override
       public void run() {
         if (m_mobileTable == null) {
           return;
         }
 
         m_mobileTable.dispose();
         m_mobileTable = null;
       }
 
     };
 
     getUiEnvironment().invokeScoutLater(job, 0);
   }
 
   /**
    * Returns true if the form is a {@link OutlineChooserForm} to avoid a border around the table. Otherwise calls super.
    */
   @Override
   protected boolean dontCreateTableContainer() {
     IForm form = null;
     if (getScoutObject() != null) {
       form = getScoutObject().getForm();
     }
 
     if (form instanceof OutlineChooserForm) {
       return true;
     }
 
     return super.dontCreateTableContainer();
   }
 }
