 /**
  * <copyright>
  *
  * Copyright (c) 2002-2004 IBM Corporation and others.
  * All rights reserved.   This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *   IBM - Initial API and implementation
  *
  * </copyright>
  *
 * $Id: EOperationItemProvider.java,v 1.12 2006/12/05 20:26:51 emerks Exp $
  */
 package org.eclipse.emf.ecore.provider;
 
 
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Iterator;
 import java.util.List;
 
 import org.eclipse.emf.common.notify.AdapterFactory;
 import org.eclipse.emf.common.notify.Notification;
 import org.eclipse.emf.common.util.ResourceLocator;
 import org.eclipse.emf.ecore.EClass;
 import org.eclipse.emf.ecore.EClassifier;
 import org.eclipse.emf.ecore.EGenericType;
 import org.eclipse.emf.ecore.EObject;
 import org.eclipse.emf.ecore.EOperation;
 import org.eclipse.emf.ecore.EStructuralFeature;
 import org.eclipse.emf.ecore.EParameter;
 import org.eclipse.emf.ecore.ETypeParameter;
 import org.eclipse.emf.ecore.EcoreFactory;
 import org.eclipse.emf.ecore.EcorePackage;
 import org.eclipse.emf.edit.command.SetCommand;
 import org.eclipse.emf.edit.domain.EditingDomain;
 import org.eclipse.emf.edit.provider.ComposeableAdapterFactory;
 import org.eclipse.emf.edit.provider.IEditingDomainItemProvider;
 import org.eclipse.emf.edit.provider.IItemLabelProvider;
 import org.eclipse.emf.edit.provider.IItemPropertySource;
 import org.eclipse.emf.edit.provider.IStructuredItemContentProvider;
 import org.eclipse.emf.edit.provider.ITreeItemContentProvider;
 import org.eclipse.emf.edit.provider.ItemPropertyDescriptor;
 import org.eclipse.emf.edit.provider.ViewerNotification;
 
 
 /**
  * This is the item provider adapter for a {@link org.eclipse.emf.ecore.EOperation} object.
  * <!-- begin-user-doc -->
  * <!-- end-user-doc -->
  * @generated
  */
 public class EOperationItemProvider
   extends ETypedElementItemProvider
   implements
     IEditingDomainItemProvider, IStructuredItemContentProvider, ITreeItemContentProvider, IItemLabelProvider, IItemPropertySource
 {
   /**
    * This constructs an instance from a factory and a notifier.
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * @generated
    */
   public EOperationItemProvider(AdapterFactory adapterFactory)
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
 
       addEExceptionsPropertyDescriptor(object);
     }
     return itemPropertyDescriptors;
   }
 
   /**
    * This adds a property descriptor for the EExceptions feature.
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * @generated NOT
    */
   protected void addEExceptionsPropertyDescriptor(Object object)
   {
     itemPropertyDescriptors.add
       (new ItemPropertyDescriptor
         (((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
          getResourceLocator(),
          getString("_UI_EOperation_eExceptions_feature"),
          getString("_UI_PropertyDescriptor_description", "_UI_EOperation_eExceptions_feature", "_UI_EOperation_type"),
          EcorePackage.Literals.EOPERATION__EEXCEPTIONS,
          true,
          false,
          true,
          null,
          null,
          null)
        {
          @Override
          public void setPropertyValue(Object object, Object value)
          {
            EditingDomain editingDomain = getEditingDomain(object);
            if (editingDomain == null)
            {
              super.setPropertyValue(object, value);
            }
            else 
            {
              EOperation eOperation = (EOperation)object;
              List eGenericTypes = new ArrayList();
              LOOP:
              for (EClassifier eException : (List<EClassifier>)value)
              {
                for (EGenericType eGenericException : eOperation.getEGenericExceptions())
                {
                  if (eGenericException.getEClassifier() == eException)
                  {
                    eGenericTypes.add(eGenericException);
                    continue LOOP;
                  }
                }
                EGenericType eGenericType = EcoreFactory.eINSTANCE.createEGenericType();
                eGenericType.setEClassifier(eException);
                for (int i = 0, size = eException.getETypeParameters().size(); i < size; ++i)
                {
                  eGenericType.getETypeArguments().add(EcoreFactory.eINSTANCE.createEGenericType());
                }
                eGenericTypes.add(eGenericType);
              }
              editingDomain.getCommandStack().execute
                (SetCommand.create(editingDomain, object, EcorePackage.Literals.EOPERATION__EGENERIC_EXCEPTIONS, eGenericTypes));
            }
          }
        });
   }
 
   /**
    * This specifies how to implement {@link #getChildren} and is used to deduce an appropriate feature for an
    * {@link org.eclipse.emf.edit.command.AddCommand}, {@link org.eclipse.emf.edit.command.RemoveCommand} or
    * {@link org.eclipse.emf.edit.command.MoveCommand} in {@link #createCommand}.
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * @generated NOT
    */
   public Collection getChildrenFeatures(Object object)
   {
     if (childrenFeatures == null)
     {
       super.getChildrenFeatures(object);
       childrenFeatures.add(1, EcorePackage.Literals.EOPERATION__ETYPE_PARAMETERS);
       childrenFeatures.add(2, EcorePackage.Literals.EOPERATION__EPARAMETERS);
       childrenFeatures.add(4, EcorePackage.Literals.EOPERATION__EGENERIC_EXCEPTIONS);
     }
     return childrenFeatures;
   }
 
   /**
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * @generated NOT
    */
   protected EStructuralFeature getChildFeature(Object object, Object child)
   {
     // Check the type of the specified child object and return the proper feature to use for
     // adding (see {@link AddCommand}) it as a child.
 
     EOperation eOperation = (EOperation)object;
     if (child instanceof EObject)
     {
       EObject eObject = (EObject)child;
       if (eObject.eContainer() == eOperation)
       {
         // If it's really a contained child, return the feature for it.
         //
         return eObject.eContainingFeature();
       }
     }
    if (eOperation.getEGenericType() == null)
     {
      return EcorePackage.Literals.ETYPED_ELEMENT__EGENERIC_TYPE;
     }
     else
     {
      return EcorePackage.Literals.EOPERATION__EGENERIC_EXCEPTIONS;
     }
   }
 
   /**
    * This returns EOperation.gif.
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * @generated
    */
   public Object getImage(Object object)
   {
     return overlayImage(object, getResourceLocator().getImage("full/obj16/EOperation"));
   }
 
   /**
    * This returns the label text for the adapted class.
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * @generated NOT
    */
   public String getText(Object object)
   {
     EOperation eOperation = (EOperation)object;
     StringBuffer result = new StringBuffer();
     result.append(eOperation.getName());
     if (!eOperation.getETypeParameters().isEmpty())
     {
       result.append("<");
       for (Iterator<ETypeParameter> i = eOperation.getETypeParameters().iterator(); i.hasNext(); )
       {
         result.append(ETypeParameterItemProvider.getText(i.next()));
         if (i.hasNext())
         {
           result.append(", ");
         }
       }
       result.append(">");
       
     }
     result.append("("); //)
     for (Iterator i = eOperation.getEParameters().iterator(); i.hasNext(); )
     {
       EParameter eParameter = (EParameter)i.next();
       if (eParameter.getEGenericType() != null)
       {
         result.append(EGenericTypeItemProvider.getText(eParameter.getEGenericType()));
         if (i.hasNext())
         {
           result.append(", ");
         }
       }
     }
     // (
     result.append(")");
     if (eOperation.getEGenericType() != null)
     {
       result.append(" : ");
       result.append(EGenericTypeItemProvider.getText(eOperation.getEGenericType()));
     }
 
     if (!eOperation.getEGenericExceptions().isEmpty())
     {
       result.append(" throws ");
       for (Iterator i = eOperation.getEGenericExceptions().iterator(); i.hasNext(); )
       {
         EGenericType eException = (EGenericType)i.next();
         result.append(EGenericTypeItemProvider.getText(eException));
         if (i.hasNext())
         {
           result.append(", ");
         }
       }
     }
     return result.toString();
   }
 
   /**
    * This handles model notifications by calling {@link #updateChildren} to update any cached
    * children and by creating a viewer notification, which it passes to {@link #fireNotifyChanged}.
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * @generated NOT
    */
   public void notifyChanged(Notification notification)
   {
     updateChildren(notification);
 
     switch (notification.getFeatureID(EOperation.class))
     {
       // Changes to the parameters should also update the label.
       //
       case EcorePackage.EOPERATION__EGENERIC_TYPE:
       case EcorePackage.EOPERATION__EGENERIC_EXCEPTIONS:
       case EcorePackage.EOPERATION__EPARAMETERS:
       case EcorePackage.EOPERATION__ETYPE_PARAMETERS:
         fireNotifyChanged(new ViewerNotification(notification, notification.getNotifier(), true, true));
         return;
     }
     super.notifyChanged(notification);
   }
 
   /**
    * This adds to the collection of {@link org.eclipse.emf.edit.command.CommandParameter}s
    * describing all of the children that can be created under this object.
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * @generated
    */
   protected void collectNewChildDescriptors(Collection newChildDescriptors, Object object)
   {
     super.collectNewChildDescriptors(newChildDescriptors, object);
 
     newChildDescriptors.add
       (createChildParameter
         (EcorePackage.Literals.EOPERATION__ETYPE_PARAMETERS,
          EcoreFactory.eINSTANCE.createETypeParameter()));
 
     newChildDescriptors.add
       (createChildParameter
         (EcorePackage.Literals.EOPERATION__EPARAMETERS,
          EcoreFactory.eINSTANCE.createEParameter()));
 
     newChildDescriptors.add
       (createChildParameter
         (EcorePackage.Literals.EOPERATION__EGENERIC_EXCEPTIONS,
          EcoreFactory.eINSTANCE.createEGenericType()));
   }
 
   /**
    * This returns the label text for {@link org.eclipse.emf.edit.command.CreateChildCommand}.
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * @generated NOT
    */
   public String getCreateChildText(Object owner, Object feature, Object child, Collection selection)
   {
     return
       feature == EcorePackage.Literals.ETYPED_ELEMENT__EGENERIC_TYPE ?
         getString("_UI_EGenericReturnType_label") :
         feature == EcorePackage.Literals.EOPERATION__EGENERIC_EXCEPTIONS ?
           getString("_UI_EGenericExceptionType_label") :
           super.getCreateChildText(owner, feature, child, selection);
   }
 
   /**
    * Return the resource locator for this item provider's resources.
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * @generated
    */
   public ResourceLocator getResourceLocator()
   {
     return EcoreEditPlugin.INSTANCE;
   }
 
 }
