 /*
 // $Id$
 // Package org.eigenbase is a class library of data management components.
 // Copyright (C) 2006-2006 The Eigenbase Project
 // Copyright (C) 2006-2006 Disruptive Tech
 // Copyright (C) 2006-2006 LucidEra, Inc.
 // Portions Copyright (C) 2006-2006 John V. Sichi
 //
 // This program is free software; you can redistribute it and/or modify it
 // under the terms of the GNU General Public License as published by the Free
 // Software Foundation; either version 2 of the License, or (at your option)
 // any later version approved by The Eigenbase Project.
 //
 // This program is distributed in the hope that it will be useful,
 // but WITHOUT ANY WARRANTY; without even the implied warranty of
 // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 // GNU General Public License for more details.
 //
 // You should have received a copy of the GNU General Public License
 // along with this program; if not, write to the Free Software
 // Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
 
 package org.eigenbase.util;
 
 /**
  * Holder for a list of constants describing which bugs which have not been
  * fixed.
  *
  * <p>You can use these constants to control the flow of your code. For
  * example, suppose that bug FNL-123 causes the "INSERT" statement to return
  * an incorrect row-count, and you want to disable unit tests.
  * You might use the constant in your code as follows:
  *
  * <blockquote><pre>Statement stmt = connection.createStatement();
  * int rowCount = stmt.execute(
  *     "INSERT INTO FemaleEmps SELECT * FROM Emps WHERE gender = 'F'");
  * if (Bug.Fnl123Fixed) {
  *    assertEquals(rowCount, 5);
  * }</pre></blockquote>
  *
  * <p>The usage of the constant is a convenient way to identify the impact of
  * the bug. When someone fixes the bug, they will remove the constant and all
  * usages of it. Also, the constant helps track the propagation of the fix: as
  * the fix is integrated into other branches, the constant will be removed from
  * those branches.</p>
  *
  * @author jhyde
  * @since 2006/3/2
  * @version $Id$
  */
 public abstract class Bug
 {
     //~ Static fields/initializers --------------------------------------------
 
 
     /**
      * Whether <a href="http://issues.eigenbase.org/browse/FRG-26">
      * issue FRG-26</a> is fixed.
      */
     public static final boolean Frg26Fixed = false;
 
     /** Also filed as dtbug324 */
     public static final boolean Frg65Fixed = false;
 
     /**
      * Whether <a href="http://issues.eigenbase.org/browse/FNL-3">issue
      * Fnl-3</a> is fixed.
      */
     public static final boolean Fnl3Fixed = false;
 
     /**
      * Whether <a href="http://jirahost.eigenbase.org:8080/browse/FRG-27">issue
      * Frg-27</a> is fixed.  (also filed as dtbug 153)
      */
     public static final boolean Fn25Fixed = false;
 
     /**
      * Whether <a href="http://jirahost.eigenbase.org:8080/browse/FRG-72">issue
      * Frg-72</a> is fixed.
      */
     public static final boolean Frg72Fixed = false;
 
 
     public static final boolean Dt269Fixed = false;
 
     public static final boolean Dt315Fixed = false;
 
     public static final boolean Dt317Fixed = false;
 
     public static final boolean Dt464Fixed = false;
 
     public static final boolean Dt292Fixed = false;
 
     public static final boolean Dt294Fixed = false;
 
     public static final boolean Dt466Fixed = false;
 
     public static final boolean Dt471Fixed = false;
 
    public static final boolean Dt591Fixed = false;

     // -----------------------------------------------------------------------
     // Developers should create new fields here, in their own section. This
     // will make merge conflicts much less likely than if everyone is
     // appending.
 
     // angel
 
     // elin
 
     // fliang
 
     // fzhang
 
     // hersker
 
     // jack
 
     // jhyde
 
     public static final boolean Dt213Fixed = false;
 
     public static final boolean Dt536Fixed = false;
 
     public static final boolean Dt561Fixed = false;
 
     public static final boolean Dt562Fixed = false;
 
     public static final boolean Dt563Fixed = false;
 
     /**
      * Whether <a href="http://issues.eigenbase.org/browse/FRG-26">
      * issue FRG-103: validator allows duplicate target columns in insert</a>
      * is fixed.
      */
     public static final boolean Frg103Fixed = false;
 
     /**
      * Whether <a href="http://issues.eigenbase.org/browse/FRG-115">
      * issue FRG-115: having clause with between not working</a>
      * is fixed.
      */
     public static final boolean Frg115Fixed = false;
 
     // johnk
 
     // jouellette
 
     // jpham
 
     // jvs
 
     // kkrueger
 
     // mberkowitz
 
     // murali
 
     // rchen
 
     // schoi
 
     // stephan
 
     // tleung
 
     // xluo
 
     // zfong    
 }
 
 // End Bug.java
