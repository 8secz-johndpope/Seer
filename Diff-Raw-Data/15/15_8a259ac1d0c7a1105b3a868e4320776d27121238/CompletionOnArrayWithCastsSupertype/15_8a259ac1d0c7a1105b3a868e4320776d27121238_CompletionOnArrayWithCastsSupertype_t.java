 /**
  * Copyright (c) 2010, 2011 Darmstadt University of Technology.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  */
 package data;
 
 import java.util.concurrent.atomic.AtomicInteger;
 
 //call chain 1 ok
 public class CompletionOnArrayWithCastsSupertype {
     public Integer[][][] findme;
     public int i;
 
     public static void method1() {
 		//@start
 	    final CompletionOnArrayWithCastsSupertype obj = new CompletionOnArrayWithCastsSupertype();
         final Number c = <^Space|obj.findme.*2 elements.*>
         //@end
         //final CompletionOnArrayWithCastsSupertype obj = new CompletionOnArrayWithCastsSupertype();
        //final Number c = obj.findme[j][k][l]
 	}
 
     public static void method2() {
         // @start
         final CompletionOnArrayWithCastsSupertype obj = new CompletionOnArrayWithCastsSupertype();
         final Number[] c = <^Space|obj.findme.*2 elements.*>
         // @end
         // final CompletionOnArrayWithCastsSupertype obj = new CompletionOnArrayWithCastsSupertype();
        //final Number[] c = obj.findme[j][k]
     }
 
     public static void method3() {
         //@start
 		final CompletionOnArrayWithCastsSupertype obj = new CompletionOnArrayWithCastsSupertype();
         final Number[][] c = <^Space|obj.findme.*2 elements.*>
         //@end
         //final CompletionOnArrayWithCastsSupertype obj = new CompletionOnArrayWithCastsSupertype();
        //final Number[][] c = obj.findme[j]
 	}
 
     public static void method4() {
         //@start
 		final CompletionOnArrayWithCastsSupertype obj = new CompletionOnArrayWithCastsSupertype();
         final Number[][][] c = <^Space|obj.findme.*>
         //@end
         //final CompletionOnArrayWithCastsSupertype obj = new CompletionOnArrayWithCastsSupertype();
         //final Number[][][] c = obj.findme
 	}
 }
