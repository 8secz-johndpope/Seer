 /*
  * Copyright (c) 2004-2009 XMLVM --- An XML-based Programming Language
  * 
  * This program is free software; you can redistribute it and/or modify it under
  * the terms of the GNU General Public License as published by the Free Software
  * Foundation; either version 2 of the License, or (at your option) any later
  * version.
  * 
  * This program is distributed in the hope that it will be useful, but WITHOUT
  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
  * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
  * details.
  * 
  * You should have received a copy of the GNU General Public License along with
  * this program; if not, write to the Free Software Foundation, Inc., 675 Mass
  * Ave, Cambridge, MA 02139, USA.
  * 
  * For more information, visit the XMLVM Home Page at http://www.xmlvm.org
  */
 
 
 package android.view;
 
 import android.content.Context;
 import android.internal.LayoutManager;
 
 
 /**
  * @author arno
  *
  */
 public class LayoutInflater {
     
     private Context context;
 
     public LayoutInflater(Context context) {
         this.context = context;
     }
     
     /**
      * @param legionGameOver
      * @param legionGameOverLayout
      * @param b
      */
     public View inflate(int resource, ViewGroup root, boolean attachToRoot)
     throws InflateException {
         View v = LayoutManager.getLayout(context, resource);
         if (v == null) {
             throw new InflateException("Unable to inflate layout: " + resource);
         }
         
         if (root != null) {
            v.setLayoutParams(root.getLayoutParams());
             if (attachToRoot) {
                 root.addView(v);
             }
         }
         
         return root != null && attachToRoot ? root : v;
     }
 
 }
