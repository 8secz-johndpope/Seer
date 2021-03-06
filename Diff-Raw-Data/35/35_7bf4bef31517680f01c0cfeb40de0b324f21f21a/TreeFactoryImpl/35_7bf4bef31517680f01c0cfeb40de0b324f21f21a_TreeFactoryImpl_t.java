 /**
  * <copyright>
  *
  * Copyright (c) 2002-2004 IBM Corporation and others.
  * All rights reserved.   This program and the accompanying materials
  * are made available under the terms of the Common Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/cpl-v10.html
  * 
  * Contributors: 
  *   IBM - Initial API and implementation
  *
  * </copyright>
  *
 * $Id: TreeFactoryImpl.java,v 1.4 2005/04/20 03:20:26 davidms Exp $
  */
 package org.eclipse.emf.edit.tree.impl;
 
 
 import org.eclipse.emf.ecore.EClass;
 import org.eclipse.emf.ecore.EObject;
 import org.eclipse.emf.ecore.impl.EFactoryImpl;
 import org.eclipse.emf.edit.tree.*;
 
 
 /**
  * <!-- begin-user-doc -->
  * An implementation of the model <b>Factory</b>.
  * <!-- end-user-doc -->
  * @generated
  */
 public class TreeFactoryImpl extends EFactoryImpl implements TreeFactory
 {
   /**
   * Creates an instance of the factory.
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * @generated
    */
   public TreeFactoryImpl()
   {
     super();
   }
 
   /**
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * @generated
    */
   public EObject create(EClass eClass)
   {
     switch (eClass.getClassifierID())
     {
       case TreePackage.TREE_NODE: return createTreeNode();
       default:
         throw new IllegalArgumentException("The class '" + eClass.getName() + "' is not a valid classifier");
     }
   }
 
   /**
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * @generated
    */
   public TreeNode createTreeNode()
   {
     TreeNodeImpl treeNode = new TreeNodeImpl();
     return treeNode;
   }
 
   /**
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * @generated
    */
   public TreePackage getTreePackage()
   {
     return (TreePackage)getEPackage();
   }
 
   /**
    * <!-- begin-user-doc -->
    * <!-- end-user-doc -->
    * @deprecated
    * @generated
    */
   public static TreePackage getPackage()
   {
     return TreePackage.eINSTANCE;
   }
 
 } //TreeFactoryImpl
