 /*
   JWildfire - an image and animation processor written in Java 
   Copyright (C) 1995-2011 Andreas Maschke
 
   This is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser 
   General Public License as published by the Free Software Foundation; either version 2.1 of the 
   License, or (at your option) any later version.
  
   This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without 
   even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
   Lesser General Public License for more details.
 
   You should have received a copy of the GNU Lesser General Public License along with this software; 
   if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
   02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
 package org.jwildfire.create.tina.variation;
 
 import org.jwildfire.create.tina.base.XForm;
 import org.jwildfire.create.tina.base.XYZPoint;
 
 public class Variation {
   private double amount;
   private VariationFunc func;
 
   public double getAmount() {
     return amount;
   }
 
   public void setAmount(double pAmount) {
     this.amount = pAmount;
   }
 
   public VariationFunc getFunc() {
     return func;
   }
 
   public void setFunc(VariationFunc func) {
     this.func = func;
   }
 
   public void transform(XFormTransformationContext pContext, XForm pXForm, XYZPoint pAffineTP, XYZPoint pVarTP) {
     func.transform(pContext, pXForm, pAffineTP, pVarTP, amount);
   }
 
   @Override
   public String toString() {
     return func.getName() + "(" + amount + ")";
   }
 
   public void assign(Variation var) {
     amount = var.amount;
     func = VariationFuncList.getVariationFuncInstance(var.func.getName());
    for (int i = 0; i < var.func.getParameterNames().length; i++) {
      Object val = var.func.getParameterValues()[i];
      if (val instanceof Double) {
        func.setParameter(var.func.getParameterNames()[i], (Double) val);
      }
      else if (val instanceof Integer) {
        func.setParameter(var.func.getParameterNames()[i], Double.valueOf(((Integer) val)));
      }
      else {
        throw new IllegalStateException();
      }
    }
   }
 }
