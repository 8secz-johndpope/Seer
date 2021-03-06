 /*
  * Copyright (c) 2004 IBM Corporation and others.
  * All rights reserved.   This program and the accompanying materials
  * are made available under the terms of the Common Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/cpl-v10.html
  *
  * Contributors:
  *   IBM - Initial API and implementation
  *
 * $Id: Ecore2EcoreMappingRootItemProvider.java,v 1.3 2005/04/20 03:20:29 davidms Exp $
  */
 package org.eclipse.emf.mapping.ecore2ecore.provider;
 
 
 import java.util.List;
 
 import org.eclipse.emf.common.notify.AdapterFactory;
 import org.eclipse.emf.common.notify.Notification;
 
 import org.eclipse.emf.common.util.ResourceLocator;
 
 import org.eclipse.emf.edit.provider.IEditingDomainItemProvider;
 import org.eclipse.emf.edit.provider.IItemLabelProvider;
 import org.eclipse.emf.edit.provider.IItemPropertySource;
 import org.eclipse.emf.edit.provider.IStructuredItemContentProvider;
 import org.eclipse.emf.edit.provider.ITreeItemContentProvider;
 
 //import org.eclipse.emf.mapping.ecore2ecore.Ecore2EcoreMappingRoot;
 import org.eclipse.emf.mapping.ecore2ecore.Ecore2EcorePlugin;
 
 import org.eclipse.emf.mapping.provider.MappingRootItemProvider;
 
 
 /**
 * This is the item provider adapter for a {@link org.eclipse.emf.mapping.ecore2ecore.Ecore2EcoreMappingRoot} object.
  * <!-- begin-user-doc -->
  * <!-- end-user-doc -->
  * @generated
  */
 public class Ecore2EcoreMappingRootItemProvider extends MappingRootItemProvider
   implements
     IEditingDomainItemProvider,
     IStructuredItemContentProvider,
     ITreeItemContentProvider,
     IItemLabelProvider,
     IItemPropertySource
 {
   /**
    * This constructs an instance from a factory and a notifier.
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * @generated
    */
   public Ecore2EcoreMappingRootItemProvider(AdapterFactory adapterFactory)
   {
     super(adapterFactory);
   }
 
   /**
    * This returns the property descriptors for the adapted class.
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * @generated
    */
   public List getPropertyDescriptors(Object object)
   {
     if (itemPropertyDescriptors == null)
     {
       super.getPropertyDescriptors(object);
 
     }
     return itemPropertyDescriptors;
   }
 
   /**
    * This returns Ecore2EcoreMappingRoot.gif.
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * @generated NOT
    */
   public Object getImage(Object object)
   {
     return super.getImage(object);
   }
 
   /**
    * This returns the label text for the adapted class.
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * @generated NOT
    */
   public String getText(Object object)
   {
     return super.getText(object);
   }
 
   /**
    * This handles model notifications by calling {@link #updateChildren} to update any cached
    * children and by creating a viewer notification, which it passes to {@link #fireNotifyChanged}.
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * @generated
    */
   public void notifyChanged(Notification notification)
   {
     updateChildren(notification);
     super.notifyChanged(notification);
   }
 
   /**
    * Return the resource locator for this item provider's resources.
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * @generated
    */
   public ResourceLocator getResourceLocator()
   {
     return Ecore2EcorePlugin.INSTANCE;
   }
 
 }
