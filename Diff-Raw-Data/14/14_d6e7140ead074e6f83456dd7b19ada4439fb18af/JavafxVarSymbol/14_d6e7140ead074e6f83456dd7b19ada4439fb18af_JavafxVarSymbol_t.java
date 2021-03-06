 /*
  * Copyright 1999-2007 Sun Microsystems, Inc.  All Rights Reserved.
  * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
  *
  * This code is free software; you can redistribute it and/or modify it
  * under the terms of the GNU General Public License version 2 only, as
  * published by the Free Software Foundation.  Sun designates this
  * particular file as subject to the "Classpath" exception as provided
  * by Sun in the LICENSE file that accompanied this code.
  *
  * This code is distributed in the hope that it will be useful, but WITHOUT
  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
  * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
  * version 2 for more details (a copy is included in the LICENSE file that
  * accompanied this code).
  *
  * You should have received a copy of the GNU General Public License version
  * 2 along with this work; if not, write to the Free Software Foundation,
  * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
  *
  * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
  * CA 95054 USA or visit www.sun.com if you need additional information or
  * have any questions.
  */
 
 package com.sun.tools.javafx.code;
 
 import com.sun.tools.javac.code.Symbol;
 import com.sun.tools.javac.code.Symbol.VarSymbol;
 import com.sun.tools.javac.code.Type;
 import com.sun.tools.javac.util.Name;
 
 /**
  * Marker wrapper on class: this is a JavaFX var
  * 
  * @author llitchev
  */
 public class JavafxVarSymbol extends VarSymbol {
     public static final int TYPE_KIND_OBJECT = 0;
     public static final int TYPE_KIND_DOUBLE = 1;
     public static final int TYPE_KIND_BOOLEAN = 2;
     public static final int TYPE_KIND_INT = 3;
     public static final int TYPE_KIND_SEQUENCE = 4;
     public static final int TYPE_KIND_COUNT = 5;
     
    public static final String[] typePrefixes = new String[] { "Object", "Double", "Boolean", "Int", "Sequence" };
     public static String getTypePrefix(int index) { return typePrefixes[index]; }
     
     private static final String[] accessorSuffixes = new String[] { "", "AsDouble", "AsBoolean", "AsInt", "AsSequence" };
     public static String getAccessorSuffix(int index) { return accessorSuffixes[index]; }
 
     /** Creates a new instance of JavafxVarSymbol */
     public JavafxVarSymbol(long flags,
             Name name,
             Type type,
             Symbol owner) {
         super(flags, name, type, owner);
     }
 }
