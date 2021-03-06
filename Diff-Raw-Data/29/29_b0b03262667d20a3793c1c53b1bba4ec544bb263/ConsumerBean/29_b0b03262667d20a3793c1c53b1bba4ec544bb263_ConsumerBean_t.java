 /*
  * JBoss, a division of Red Hat
  * Copyright 2010, Red Hat Middleware, LLC, and individual
  * contributors as indicated by the @authors tag. See the
  * copyright.txt in the distribution for a full listing of
  * individual contributors.
  *
  * This is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as
  * published by the Free Software Foundation; either version 2.1 of
  * the License, or (at your option) any later version.
  *
  * This software is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this software; if not, write to the Free
  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
  */
 
 package org.gatein.wsrp.admin.ui;
 
 import com.google.common.base.Function;
 import org.gatein.common.util.ParameterValidation;
 import org.gatein.pc.api.Portlet;
 import org.gatein.pc.api.PortletContext;
 import org.gatein.pc.api.PortletInvokerException;
 import org.gatein.wsrp.WSRPConsumer;
 import org.gatein.wsrp.WSRPUtils;
 import org.gatein.wsrp.api.PortalStructureProvider;
 import org.gatein.wsrp.consumer.EndpointConfigurationInfo;
 import org.gatein.wsrp.consumer.ProducerInfo;
 import org.gatein.wsrp.consumer.RegistrationInfo;
 import org.gatein.wsrp.consumer.RegistrationProperty;
 import org.gatein.wsrp.consumer.migration.ExportInfo;
 import org.gatein.wsrp.consumer.migration.ImportInfo;
 import org.gatein.wsrp.consumer.migration.MigrationService;
 import org.gatein.wsrp.consumer.registry.ConsumerRegistry;
 
 import javax.faces.context.FacesContext;
 import javax.faces.event.ActionEvent;
 import javax.faces.event.ValueChangeEvent;
 import javax.faces.model.DataModel;
 import javax.faces.model.ListDataModel;
 import javax.faces.model.SelectItem;
 import javax.xml.namespace.QName;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Locale;
 import java.util.Map;
 import java.util.SortedMap;
 
 /**
  * @author <a href="mailto:chris.laprun@jboss.com">Chris Laprun</a>
  * @version $Revision: 12865 $
  * @since 2.6
  */
 public class ConsumerBean extends ManagedBean
 {
    public static final SelectablePortletToHandleFunction SELECTABLE_TO_HANDLE = new SelectablePortletToHandleFunction();
    private WSRPConsumer consumer;
    private ConsumerRegistry registry;
    private ConsumerManagerBean manager;
    private boolean modified;
    private String wsdl;
 
    private static final String CANNOT_FIND_CONSUMER = "bean_consumer_cannot_find_consumer";
    private static final String CANNOT_UPDATE_CONSUMER = "bean_consumer_cannot_update_consumer";
    private static final String CANNOT_REFRESH_CONSUMER = "bean_consumer_cannot_refresh_consumer";
    private static final String MODIFY_REG_SUCCESS = "bean_consumer_modify_reg_success";
    private static final String INVALID_MODIFY = "bean_consumer_invalid_modify";
    private static final String CANNOT_MODIFY_REG = "bean_consumer_cannot_modify_reg";
    private static final String CANNOT_ERASE_REG = "bean_consumer_cannot_erase_reg";
    private static final String MALFORMED_URL = "bean_consumer_malformed_url";
    private static final String UPDATE_SUCCESS = "bean_consumer_update_success";
    private static final String CANNOT_EXPORT = "bean_consumer_cannot_export";
    private static final String IMPORT_SUCCESS = "bean_consumer_import_success";
    private static final String FAILED_PORTLETS = "bean_consumer_import_failed_portlets";
    private static final String CONSUMER_TYPE = "CONSUMER_TYPE";
 
    private DataModel portletHandles;
    private DataModel existingExports;
    private ExportInfoDisplay currentExport;
 
    public void setRegistry(ConsumerRegistry registry)
    {
       this.registry = registry;
    }
 
    public void setManager(ConsumerManagerBean manager)
    {
       this.manager = manager;
    }
 
    public boolean isModified()
    {
       return modified || getProducerInfo().isModifyRegistrationRequired() || isRegistrationLocallyModified();
    }
 
    public boolean isRefreshNeeded()
    {
       return consumer.isRefreshNeeded();
    }
 
    public String getId()
    {
       return consumer.getProducerId();
    }
 
    public void setId(String id)
    {
       if (consumer != null)
       {
          // renaming scenario
          ProducerInfo info = getProducerInfo();
          String oldId = info.getId();
 
          // need to check that the new id is valid
          if (isOldAndNewDifferent(oldId, id))
          {
             id = checkNameValidity(id, "edit-cons-form:id");
             if (id != null)
             {
                info.setId(id);
 
                // properly update the registry after change of id
                getRegistry().updateProducerInfo(info);
 
                // we're not using modifyIfNeeded here to avoid double equality check, so we need to set modified manually
                modified = true;
             }
          }
       }
       else
       {
          // initialization scenario
          consumer = getRegistry().getConsumer(id);
          if (consumer != null)
          {
             EndpointConfigurationInfo endpoint = getProducerInfo().getEndpointConfigurationInfo();
             wsdl = endpoint.getWsdlDefinitionURL();
          }
          else
          {
             beanContext.createErrorMessage(CANNOT_FIND_CONSUMER, id);
          }
       }
    }
 
    public Integer getCache()
    {
       return getProducerInfo().getExpirationCacheSeconds();
    }
 
    public void setCache(Integer cache)
    {
       getProducerInfo().setExpirationCacheSeconds((Integer)modifyIfNeeded(getCache(), cache, "cache", false));
    }
 
    public Integer getTimeout()
    {
       return getProducerInfo().getEndpointConfigurationInfo().getWSOperationTimeOut();
    }
 
    public void setTimeout(Integer timeout)
    {
       getProducerInfo().getEndpointConfigurationInfo().setWSOperationTimeOut((Integer)modifyIfNeeded(getTimeout(), timeout, "timeout", false));
    }
 
    public String getWsdl()
    {
       return wsdl;
    }
 
    public void setWsdl(String wsdlURL)
    {
       wsdl = (String)modifyIfNeeded(wsdl, wsdlURL, "wsdl", true);
    }
 
    private void internalSetWsdl(String wsdlURL)
    {
       try
       {
          getProducerInfo().getEndpointConfigurationInfo().setWsdlDefinitionURL(wsdlURL);
       }
       catch (Exception e)
       {
          getRegistry().deactivateConsumerWith(getId());
          beanContext.createErrorMessageFrom("wsdl", e);
       }
    }
 
    public boolean isActive()
    {
       return consumer.isActive();
    }
 
    public boolean isRegistered()
    {
       return getProducerInfo().isRegistered();
    }
 
    public boolean isRegistrationRequired()
    {
       return getProducerInfo().isRegistrationRequired();
    }
 
    public boolean isRegistrationCheckNeeded()
    {
       ProducerInfo info = getProducerInfo();
       if (info.isRefreshNeeded(true))
       {
          RegistrationInfo regInfo = info.getRegistrationInfo();
          if (regInfo == null)
          {
             return true;
          }
          else
          {
             Boolean consistent = regInfo.isConsistentWithProducerExpectations();
             return consistent == null || !consistent.booleanValue();
          }
       }
       else
       {
          return false;
       }
    }
 
    public boolean isDisplayExpectedNeeded()
    {
       ProducerInfo producerInfo = getProducerInfo();
 
       // only show expected registration info if it is different from the one we currently have
       return producerInfo.isModifyRegistrationRequired() && producerInfo.getRegistrationInfo() != producerInfo.getExpectedRegistrationInfo();
    }
 
    public boolean isRegistrationLocallyModified()
    {
       return isRegistered() && getProducerInfo().getRegistrationInfo().isModifiedSinceLastRefresh();
    }
 
    public boolean isRegistrationChecked()
    {
       return getProducerInfo().isRegistrationChecked();
    }
 
    public boolean isRegistrationValid()
    {
       if (isRegistrationChecked())
       {
          return getProducerInfo().getRegistrationInfo().isRegistrationValid().booleanValue();
       }
       throw new IllegalStateException("Need to check the registration before determining if it's valid!");
    }
 
    public ProducerInfo getProducerInfo()
    {
       return consumer.getProducerInfo();
    }
 
    public boolean isLocalInfoPresent()
    {
       return getProducerInfo().hasLocalRegistrationInfo();
    }
 
    public boolean isRegistrationPropertiesExisting()
    {
       RegistrationInfo regInfo = getProducerInfo().getRegistrationInfo();
       return regInfo == null || regInfo.isRegistrationPropertiesExisting();
    }
 
    public boolean isExpectedRegistrationPropertiesExisting()
    {
       RegistrationInfo info = getExpectedRegistrationInfo();
       return info != null && info.isRegistrationPropertiesExisting();
    }
 
    private RegistrationInfo getExpectedRegistrationInfo()
    {
       return getProducerInfo().getExpectedRegistrationInfo();
    }
 
    public List<RegistrationProperty> getRegistrationProperties()
    {
       return getSortedProperties(getProducerInfo().getRegistrationInfo());
    }
 
    public List<RegistrationProperty> getExpectedRegistrationProperties()
    {
       return getSortedProperties(getExpectedRegistrationInfo());
    }
 
    private List<RegistrationProperty> getSortedProperties(RegistrationInfo registrationInfo)
    {
       if (registrationInfo != null)
       {
          LinkedList<RegistrationProperty> list = new LinkedList<RegistrationProperty>(registrationInfo.getRegistrationProperties().values());
          Collections.sort(list);
          return list;
       }
       else
       {
          return Collections.emptyList();
       }
    }
 
    // Actions
 
    public String update()
    {
       return internalUpdate(true);
    }
 
    public String confirmEraseRegistration()
    {
       return "confirmEraseRegistration";
    }
 
    private String internalUpdate(boolean showMessage)
    {
       if (consumer != null)
       {
          if (isModified())
          {
             try
             {
                // update values
                ProducerInfo prodInfo = getProducerInfo();
                EndpointConfigurationInfo endpointInfo = prodInfo.getEndpointConfigurationInfo();
                internalSetWsdl(wsdl);
 
                saveToRegistry(prodInfo);
             }
             catch (Exception e)
             {
                beanContext.createErrorMessageFrom(e);
                return null;
             }
          }
 
          if (showMessage)
          {
             beanContext.createInfoMessage(UPDATE_SUCCESS);
          }
          return ConsumerManagerBean.CONFIGURE_CONSUMER;
       }
 
       beanContext.createErrorMessage(CANNOT_UPDATE_CONSUMER);
       return null;
    }
 
    private void saveToRegistry(ProducerInfo prodInfo)
    {
       getRegistry().updateProducerInfo(prodInfo);
       modified = false;
    }
 
    public String refreshConsumer()
    {
       if (consumer != null)
       {
          if (isModified())
          {
             String updateResult = internalUpdate(false);
             if (updateResult == null)
             {
                return null;
             }
          }
 
          // if the registration is locally modified, bypass the refresh as it will not yield a proper result
          if (!isRegistrationLocallyModified())
          {
             manager.refresh(consumer);
          }
          else
          {
             beanContext.createInfoMessage(ConsumerManagerBean.REFRESH_MODIFY);
          }
 
          return ConsumerManagerBean.CONFIGURE_CONSUMER;
       }
 
       beanContext.createErrorMessage(CANNOT_REFRESH_CONSUMER);
       return null;
    }
 
    public String modifyRegistration()
    {
       if (consumer != null)
       {
          ProducerInfo info = getProducerInfo();
          if (isModified())
          {
             // get updated registration info
             RegistrationInfo newReg = getExpectedRegistrationInfo();
 
             // make sure we save any modified registration properties
             saveToRegistry(info);
 
             // save old info in case something goes wrong
             RegistrationInfo oldReg = getProducerInfo().getRegistrationInfo();
 
             // check that we have the proper state
             if (newReg == null)
             {
                // if we want to change an existing registration property (for example, to upgrade service) then there are
                // no expected information, we're just using the modified local version
                newReg = new RegistrationInfo(oldReg);
 
                if (!isRegistrationLocallyModified())
                {
                   IllegalStateException e =
                      new IllegalStateException("Registration not locally modified: there should be expected registration from producer!");
                   log.debug("Couldn't modify registration", e);
                   throw e;
                }
             }
 
             try
             {
                // todo: this should be done better cf regPropListener
                newReg.setModifiedSinceLastRefresh(true); // mark as modified to force refresh of RegistrationData
                // attempt to modify the registration using new registration info
                info.setRegistrationInfo(newReg);
                info.modifyRegistration();
                newReg.setModifiedSinceLastRefresh(false);
 
                beanContext.createInfoMessage(MODIFY_REG_SUCCESS);
             }
             catch (Exception e)
             {
                // restore old info
                info.setRegistrationInfo(oldReg);
 
                beanContext.createErrorMessageFrom(e);
                return null;
             }
 
             refreshConsumer();
             return null;
          }
          else
          {
             beanContext.createErrorMessage(INVALID_MODIFY);
          }
       }
 
       beanContext.createErrorMessage(CANNOT_MODIFY_REG);
       return null;
    }
 
    public String eraseLocalRegistration()
    {
       if (consumer != null)
       {
          getProducerInfo().eraseRegistrationInfo();
          return ConsumerManagerBean.CONFIGURE_CONSUMER;
       }
 
       beanContext.createErrorMessage(CANNOT_ERASE_REG);
       return null;
    }
 
    private Object modifyIfNeeded(Object oldValue, Object newValue, String target, boolean checkURL)
    {
       if (isOldAndNewDifferent(oldValue, newValue))
       {
          if (checkURL)
          {
             try
             {
                // check that the new value is a valid URL
                new URL(newValue.toString());
             }
             catch (MalformedURLException e)
             {
                beanContext.createTargetedErrorMessage(target, MALFORMED_URL, newValue, e.getLocalizedMessage());
             }
          }
 
          oldValue = newValue;
          modified = true;
       }
 
       return oldValue;
    }
 
    protected String getObjectTypeName()
    {
       return CONSUMER_TYPE;
    }
 
    public boolean isAlreadyExisting(String objectName)
    {
       return getRegistry().getConsumer(objectName) != null;
    }
 
    public ConsumerRegistry getRegistry()
    {
       return registry;
    }
 
    public DataModel getPortlets()
    {
       try
       {
          if (portletHandles == null)
          {
             Collection<Portlet> portlets = consumer.getProducerInfo().getPortletMap().values();
             List<SelectablePortletHandle> selectableHandles = Collections.emptyList();
             if (ParameterValidation.existsAndIsNotEmpty(portlets))
             {
                selectableHandles = new ArrayList<SelectablePortletHandle>(portlets.size());
                for (Portlet portlet : portlets)
                {
                   selectableHandles.add(new SelectablePortletHandle(portlet.getContext().getId(), consumer.getMigrationService().getStructureProvider()));
                }
             }
            Collections.sort(selectableHandles);
             portletHandles = new ListDataModel(selectableHandles);
          }
 
          return portletHandles;
       }
       catch (PortletInvokerException e)
       {
          beanContext.createErrorMessageFrom(e);
          return null;
       }
    }
 
    public String exportPortlets()
    {
       if (consumer != null)
       {
          List<SelectablePortletHandle> handles = (List<SelectablePortletHandle>)portletHandles.getWrappedData();
          List<String> selectedHandles = new ArrayList<String>(handles.size());
          for (SelectablePortletHandle selectablePortletHandle : handles)
          {
             if (selectablePortletHandle.isSelected())
             {
                selectedHandles.add(selectablePortletHandle.getHandle());
             }
          }
 
          try
          {
             currentExport = new ExportInfoDisplay(consumer.exportPortlets(selectedHandles), beanContext.getLocale(), consumer.getMigrationService().getStructureProvider());
          }
          catch (Exception e)
          {
             beanContext.createErrorMessageFrom(e);
             return null;
          }
          return ConsumerManagerBean.EXPORT_DETAIL;
       }
 
       beanContext.createErrorMessage(CANNOT_EXPORT);
       return null;
    }
 
    public ExportInfoDisplay getCurrentExport()
    {
       return currentExport;
    }
 
    public DataModel getExistingExports()
    {
       if (existingExports == null)
       {
          Locale locale = beanContext.getLocale();
          MigrationService migrationService = consumer.getMigrationService();
          List<ExportInfo> availableExportInfos = migrationService.getAvailableExportInfos();
          List<ExportInfoDisplay> exportDisplays = new ArrayList<ExportInfoDisplay>(availableExportInfos.size());
          for (ExportInfo exportInfo : availableExportInfos)
          {
             exportDisplays.add(new ExportInfoDisplay(exportInfo, locale, migrationService.getStructureProvider()));
          }
          existingExports = new ListDataModel(exportDisplays);
       }
 
       return existingExports;
    }
 
    public String viewExport()
    {
       selectExport();
 
       return ConsumerManagerBean.EXPORT_DETAIL;
    }
 
    public String importPortlets()
    {
       List<SelectablePortletHandle> exportedPortlets = currentExport.getExportedPortlets();
 
       try
       {
          List<SelectablePortletHandle> portletsToImport = new ArrayList<SelectablePortletHandle>(exportedPortlets.size());
          for (SelectablePortletHandle exportedPortlet : exportedPortlets)
          {
             if (exportedPortlet.isSelected())
             {
                portletsToImport.add(exportedPortlet);
             }
          }
          ImportInfo info = consumer.importPortlets(currentExport.getExport(), WSRPUtils.transform(portletsToImport, SELECTABLE_TO_HANDLE));
 
          PortalStructureProvider structureProvider = consumer.getMigrationService().getStructureProvider();
          int importCount = 0;
          for (SelectablePortletHandle importedPortlet : portletsToImport)
          {
             PortletContext portletContext = info.getPortletContextFor(importedPortlet.getHandle());
             if (portletContext != null)
             {
                structureProvider.assignPortletToWindow(portletContext, importedPortlet.getWindow(), importedPortlet.getPage());
                importCount++;
             }
          }
 
          beanContext.createLocalizedMessage(BeanContext.STATUS, IMPORT_SUCCESS, beanContext.getInfoSeverity(), importCount);
         SortedMap<QName, List<String>> errorCodesToFailedPortletHandlesMapping = info.getErrorCodesToFailedPortletHandlesMapping();
         if (!errorCodesToFailedPortletHandlesMapping.isEmpty())
         {
            beanContext.createErrorMessage(FAILED_PORTLETS, errorCodesToFailedPortletHandlesMapping);
         }
 
          return ConsumerManagerBean.CONSUMERS;
       }
       catch (PortletInvokerException e)
       {
          beanContext.createErrorMessageFrom(e);
          return null;
       }
 
    }
 
    public String deleteExport()
    {
       ExportInfo export = currentExport.getExport();
       if (consumer.getMigrationService().remove(export) == export)
       {
          existingExports = null; // force rebuild of export list
          currentExport = null;
       }
 
       return ConsumerManagerBean.EXPORTS;
    }
 
    public void selectExport(ActionEvent actionEvent)
    {
       selectExport();
    }
 
    public void selectExport()
    {
       currentExport = (ExportInfoDisplay)existingExports.getRowData();
    }
 
    public boolean isSupportsExport()
    {
       return consumer.isSupportsExport();
    }
 
    public boolean isAvailableExportInfosEmpty()
    {
       return consumer.getMigrationService().isAvailableExportInfosEmpty();
    }
 
   public static class SelectablePortletHandle implements Comparable<SelectablePortletHandle>
    {
       private String handle;
       private boolean selected;
       private String page;
       private String window;
       private PortalStructureProvider provider;
 
       public SelectablePortletHandle(String handle, PortalStructureProvider provider)
       {
          this.handle = handle;
          this.provider = provider;
       }
 
       public String getHandle()
       {
          return handle;
       }
 
       public boolean isSelected()
       {
          return selected;
       }
 
       public void setSelected(boolean selected)
       {
          this.selected = selected;
       }
 
       public void setPage(String page)
       {
          this.page = page;
       }
 
       public String getPage()
       {
          return page;
       }
 
       public void setWindow(String window)
       {
          this.window = window;
       }
 
       public String getWindow()
       {
          return window;
       }
 
       public void selectCurrentPage(ValueChangeEvent event)
       {
          page = (String)event.getNewValue();
 
          // bypass the rest of the life cycle and re-display page
          FacesContext.getCurrentInstance().renderResponse();
       }
 
       public List<SelectItem> getPages()
       {
          List<String> pageIdentifiers = provider.getPageIdentifiers();
          return getSelectItemsFrom(pageIdentifiers);
       }
 
       private List<SelectItem> getSelectItemsFrom(List<String> identifiers)
       {
          List<SelectItem> result = new ArrayList<SelectItem>(identifiers.size());
          for (String pageIdentifier : identifiers)
          {
             result.add(new SelectItem(pageIdentifier));
          }
          return result;
       }
 
       public List<SelectItem> getWindows()
       {
          return getSelectItemsFrom(provider.getWindowIdentifiersFor(page));
       }
 
       public void select(ValueChangeEvent event)
       {
          selected = (Boolean)event.getNewValue();
       }

      public int compareTo(SelectablePortletHandle o)
      {
         return handle.compareTo(o.handle);
      }
    }
 
    public static class ExportInfoDisplay
    {
       private ExportInfo export;
       private Locale locale;
       private List<FailedPortletsDisplay> failedPortlets;
       private List<SelectablePortletHandle> exportedPortlets;
 
       public ExportInfoDisplay(ExportInfo export, Locale locale, PortalStructureProvider provider)
       {
          this.export = export;
          this.locale = locale;
 
          List<String> exportedPortletHandles = export.getExportedPortletHandles();
          if (ParameterValidation.existsAndIsNotEmpty(exportedPortletHandles))
          {
             exportedPortlets = new ArrayList<SelectablePortletHandle>(exportedPortletHandles.size());
             for (String handle : exportedPortletHandles)
             {
                exportedPortlets.add(new SelectablePortletHandle(handle, provider));
             }
          }
          else
          {
             exportedPortlets = Collections.emptyList();
          }
 
          SortedMap<QName, List<String>> errorCodesToFailedPortletHandlesMapping = export.getErrorCodesToFailedPortletHandlesMapping();
          if (ParameterValidation.existsAndIsNotEmpty(errorCodesToFailedPortletHandlesMapping))
          {
             failedPortlets = new ArrayList<FailedPortletsDisplay>(errorCodesToFailedPortletHandlesMapping.size());
             for (Map.Entry<QName, List<String>> entry : errorCodesToFailedPortletHandlesMapping.entrySet())
             {
                failedPortlets.add(new FailedPortletsDisplay(entry.getKey(), entry.getValue()));
             }
          }
          else
          {
             failedPortlets = Collections.emptyList();
          }
       }
 
       public String getExportTime()
       {
          return export.getHumanReadableExportTime(locale);
       }
 
       public String getExpirationTime()
       {
          return export.getHumanReadableExpirationTime(locale);
       }
 
       public boolean isHasFailedPortlets()
       {
          return !failedPortlets.isEmpty();
       }
 
       public List<SelectablePortletHandle> getExportedPortlets()
       {
          return exportedPortlets;
       }
 
       public List<FailedPortletsDisplay> getFailedPortlets()
       {
          return failedPortlets;
       }
 
       public ExportInfo getExport()
       {
          return export;
       }
    }
 
    public static class FailedPortletsDisplay
    {
       private QName errorCode;
       private List<String> faiedPortlets;
 
       public FailedPortletsDisplay(QName errorCode, List<String> failedPortlets)
       {
          this.errorCode = errorCode;
          this.faiedPortlets = failedPortlets;
       }
 
       public QName getErrorCode()
       {
          return errorCode;
       }
 
       public List<String> getFailedPortlets()
       {
          return faiedPortlets;
       }
    }
 
    private static class SelectablePortletToHandleFunction implements Function<SelectablePortletHandle, String>
    {
       public String apply(SelectablePortletHandle from)
       {
          return from.getHandle();
       }
    }
 }
