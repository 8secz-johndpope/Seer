 /*
  * Copyright Red Hat Inc. and/or its affiliates and other contributors
  * as indicated by the authors tag. All rights reserved.
  *
  * This copyrighted material is made available to anyone wishing to use,
  * modify, copy, or redistribute it subject to the terms and conditions
  * of the GNU General Public License version 2.
  * 
  * This particular file is subject to the "Classpath" exception as provided in the 
  * LICENSE file that accompanied this code.
  * 
  * This program is distributed in the hope that it will be useful, but WITHOUT A
  * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  * PARTICULAR PURPOSE.  See the GNU General Public License for more details.
  * You should have received a copy of the GNU General Public License,
  * along with this distribution; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
  * MA  02110-1301, USA.
  */
 
 package com.redhat.ceylon.compiler.java.loader;
 
 import java.util.IdentityHashMap;
 import java.util.Map;
 
 import com.redhat.ceylon.compiler.typechecker.model.ClassOrInterface;
 import com.redhat.ceylon.compiler.typechecker.model.Declaration;
 import com.redhat.ceylon.compiler.typechecker.model.Functional;
 import com.redhat.ceylon.compiler.typechecker.model.IntersectionType;
 import com.redhat.ceylon.compiler.typechecker.model.NothingType;
 import com.redhat.ceylon.compiler.typechecker.model.Parameter;
 import com.redhat.ceylon.compiler.typechecker.model.ParameterList;
 import com.redhat.ceylon.compiler.typechecker.model.ProducedType;
 import com.redhat.ceylon.compiler.typechecker.model.TypeDeclaration;
 import com.redhat.ceylon.compiler.typechecker.model.TypeParameter;
 import com.redhat.ceylon.compiler.typechecker.model.UnionType;
 import com.redhat.ceylon.compiler.typechecker.model.UnknownType;
 import com.redhat.ceylon.compiler.typechecker.model.Value;
 import com.redhat.ceylon.compiler.typechecker.tree.Tree;
 import com.redhat.ceylon.compiler.typechecker.tree.Visitor;
 
 /**
  *
  * @author Stéphane Épardaud <stef@epardaud.fr>
  */
 public class UnknownTypeCollector extends Visitor {
     public void visit(Tree.BaseMemberOrTypeExpression that) {
         super.visit(that);
         Declaration declaration = that.getDeclaration();
         if(declaration == null)
             return;
         if(declaration instanceof Functional){
             Functional m = (Functional)declaration;
             collectUnknownTypes(m.getType());
             for(ParameterList pl : m.getParameterLists()){
                 for(Parameter p : pl.getParameters()){
                     collectUnknownTypes(p.getType());
                 }
             }
         }else if(declaration instanceof Value){
             Value v = (Value)declaration;
             collectUnknownTypes(v.getType());
         }
     }
 
     private void collectUnknownTypes(ProducedType type) {
         Map<Declaration, Declaration> visited = new IdentityHashMap<Declaration, Declaration>();
         collectUnknownTypes(type, visited);
     }
 
     private void collectUnknownTypes(ProducedType type, Map<Declaration, Declaration> visited) {
        collectUnknownTypesResolved(type.resolveAliases(), visited);
     }
 
     private void collectUnknownTypesResolved(ProducedType type, Map<Declaration, Declaration> visited) {
         TypeDeclaration declaration = type.getDeclaration();
         if(declaration != null)
             collectUnknownTypes(declaration, visited);
         for(ProducedType tl : type.getTypeArgumentList()){
             collectUnknownTypesResolved(tl, visited);
         }
     }
 
     private void collectUnknownTypes(TypeDeclaration declaration, Map<Declaration, Declaration> visited) {
         if(visited.put(declaration, declaration) != null)
             return;
         if(declaration instanceof UnknownType){
             ((UnknownType) declaration).reportErrors();
         }else if(declaration instanceof UnionType){
             for(ProducedType t : declaration.getCaseTypes()){
                 collectUnknownTypesResolved(t, visited);
             }
         }else if(declaration instanceof IntersectionType){
             for(ProducedType t : declaration.getSatisfiedTypes()){
                 collectUnknownTypesResolved(t, visited);
             }
         }else if(declaration instanceof ClassOrInterface){
             // these are not resolved
             if(declaration.getExtendedType() != null)
                 collectUnknownTypes(declaration.getExtendedType(), visited);
             for(ProducedType t : declaration.getSatisfiedTypes())
                 collectUnknownTypes(t, visited);
         }else if(declaration instanceof NothingType
                 || declaration instanceof TypeParameter){
             // do nothing
         }
     }
 }
