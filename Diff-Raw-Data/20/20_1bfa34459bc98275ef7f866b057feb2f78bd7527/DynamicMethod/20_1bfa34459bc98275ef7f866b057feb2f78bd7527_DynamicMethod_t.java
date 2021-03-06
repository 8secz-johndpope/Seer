 /***** BEGIN LICENSE BLOCK *****
  * Version: CPL 1.0/GPL 2.0/LGPL 2.1
  *
  * The contents of this file are subject to the Common Public
  * License Version 1.0 (the "License"); you may not use this file
  * except in compliance with the License. You may obtain a copy of
  * the License at http://www.eclipse.org/legal/cpl-v10.html
  *
  * Software distributed under the License is distributed on an "AS
  * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
  * implied. See the License for the specific language governing
  * rights and limitations under the License.
  *
  * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
  * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
  * Copyright (C) 2005 Thomas E Enebo <enebo@acm.org>
  * 
  * Alternatively, the contents of this file may be used under the terms of
  * either of the GNU General Public License Version 2 or later (the "GPL"),
  * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
  * in which case the provisions of the GPL or the LGPL are applicable instead
  * of those above. If you wish to allow use of your version of this file only
  * under the terms of either the GPL or the LGPL, and not to allow others to
  * use your version of this file under the terms of the CPL, indicate your
  * decision by deleting the provisions above and replace them with the notice
  * and other provisions required by the GPL or the LGPL. If you do not delete
  * the provisions above, a recipient may use your version of this file under
  * the terms of any one of the CPL, the GPL or the LGPL.
  ***** END LICENSE BLOCK *****/
 package org.jruby.internal.runtime.methods;
 
 import org.jruby.RubyModule;
 import org.jruby.runtime.Arity;
 import org.jruby.runtime.Block;
 import org.jruby.runtime.CallType;
 import org.jruby.runtime.ThreadContext;
 import org.jruby.runtime.Visibility;
 import org.jruby.runtime.builtin.IRubyObject;
 
 /**
  *
  * @author  jpetersen
  */
 public abstract class DynamicMethod {
     protected RubyModule implementationClass;
     protected Visibility visibility;
    private boolean needsImplementer;
     
     protected DynamicMethod(RubyModule implementationClass, Visibility visibility) {
         this.visibility = visibility;
         this.implementationClass = implementationClass;
         if (implementationClass != null) {
             boolean implIsClass = implementationClass.isClass();
             boolean implIsKernel = implementationClass == implementationClass.getRuntime().getKernel();
             needsImplementer = !(implIsClass || implIsKernel);
         }
     }
 
     /**
      * Call the method
      * @param context is the thread-specific information that this method is being invoked on
      * @param receiver 
      */
     public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, 
             String name, IRubyObject[] args, boolean noSuper, Block block) {
         // FIXME: this potentially should go in overridden call impls too, but so far it hasn't appeared to be necessary
         RubyModule implementer = null;
         if (needsImplementer) {
             // modules are included with a shim class; we must find that shim to handle super() appropriately
             implementer = clazz.findImplementer(getImplementationClass());
         } else {
             // classes are directly in the hierarchy, so no special logic is necessary for implementer
             implementer = getImplementationClass();
         }
         
         preMethod(context, implementer, self, name, args, noSuper, block);
 
         try {
             return internalCall(context, implementer, self, name, args, noSuper, block);
         } finally {
             postMethod(context);
         }
     }
     
     protected abstract void postMethod(ThreadContext context);
     protected abstract IRubyObject internalCall(ThreadContext context, RubyModule clazz, IRubyObject self, String name, IRubyObject[] args, boolean noSuper, Block block);
     protected abstract void preMethod(ThreadContext context, RubyModule clazz, IRubyObject self, String name, IRubyObject[] args, boolean noSuper, Block block);
     public abstract DynamicMethod dup();
 
     public boolean isCallableFrom(IRubyObject caller, CallType callType) {
         if (getVisibility().isPrivate() && (callType == CallType.NORMAL)) {
             return false;
         } else if (getVisibility().isProtected()) {
             RubyModule defined = getImplementationClass();
             while (defined.isIncluded()) {
                 defined = defined.getMetaClass();
             }
             if (!caller.isKindOf(defined)) {
                 return false;
             }
         }
         
         return true;
     }
     
     public RubyModule getImplementationClass() {
         return implementationClass;
     }
 
     public void setImplementationClass(RubyModule implClass) {
         implementationClass = implClass;
     }
 
     public Visibility getVisibility() {
         return visibility;
     }
 
     public void setVisibility(Visibility visibility) {
         this.visibility = visibility;
     }
 
     public boolean isUndefined() {
         return false;
     }
 
     public Arity getArity() {
         return Arity.optional();
     }
 }
