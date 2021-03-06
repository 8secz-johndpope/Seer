 /*
  * {{{ header & license
  * BackgroundPropertyDeclarationFactory.java
  * Copyright (c) 2004, 2005 Patrick Wright
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public License
  * as published by the Free Software Foundation; either version 2.1
  * of the License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
  * GNU Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
  * }}}
  */
 package org.xhtmlrenderer.css.sheet.factory;
 
 import org.w3c.dom.css.CSSPrimitiveValue;
 import org.xhtmlrenderer.css.constants.CSSName;
 import org.xhtmlrenderer.css.constants.Idents;
 import org.xhtmlrenderer.css.value.FSCssValue;
 
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 
 
 /**
  * A PropertyDeclarationFactory for CSS 2 "background" shorthand property,
  * instantiating PropertyDeclarations; Singleton, use {@link #instance()}.
  *
  * @author Patrick Wright
  */
 public class BackgroundPropertyDeclarationFactory extends AbstractPropertyDeclarationFactory {
     /**
      * Singleton instance.
      */
     private static BackgroundPropertyDeclarationFactory _instance;
 
     /**
      * Constructor for the BackgroundPropertyDeclarationFactory object
      */
     private BackgroundPropertyDeclarationFactory() {
     }
 
     /**
      * Subclassed implementation of redirected buildDeclarations() from abstract
      * superclass.
      *
      * @param primVals  PARAM
      * @param important True if author-marked important!
      * @param cssName   property name
      * @param origin    The origin of the stylesheet; constant from {@link
      *                  org.xhtmlrenderer.css.sheet.Stylesheet}, e.g. Stylesheet.AUTHOR
      * @return Iterator of PropertyDeclarations for the shorthand
      *         margin property.
      */
     protected Iterator doBuildDeclarations(CSSPrimitiveValue[] primVals,
                                            boolean important,
                                            CSSName cssName,
                                            int origin) {
 
         List declarations = new ArrayList();
         CSSPrimitiveValue primitive = null;
         CSSPrimitiveValue primitives[] = new CSSPrimitiveValue[1];
         CSSName[] names = new CSSName[1];
         CSSPrimitiveValue bgPosPrimitive = null;
         StringBuffer bgPos = null;
         String val = null;
         FSCssValue fsCssValue = null;
 
         for (int i = 0; i < primVals.length; i++) {
             primitive = primVals[i];
 
             val = primitive.getCssText().trim();
 
             // sniff this one value out. using an array to return
             // multiple pieces of info; see parseSingle() method
             Object[] ret = parseSingle(val, primitive, bgPos);
 
             // handle background-position. the issue here is that
             // the position will have either one or two assignments
             // (e.g. top, top left), both of (as a pair) are the actual assigned
             // CSS value (for the background-position property), as there
             // is no background-position-x/y. so while we call parseSingle()
             // once for each element in the property assignment, we need
             // to keep track of a bg position across multiple invocations.
             //
             // to make this worse, if both values for the position are given
             // they must appear as a pair (top left) without intervening properties,
             // whereas all other properties can be given in any order.
             if (((Boolean) ret[4]).booleanValue()) {
                 if (bgPos == null) {
                     bgPos = (StringBuffer) ret[2];
                 }
                 if (bgPosPrimitive == null) {
                     bgPosPrimitive = (CSSPrimitiveValue) ret[3];
                 }
                 continue;
             }
            if (ret[0] == null) continue;
            names[0] = (CSSName) ret[0];
            primitives[0] = new FSCssValue((CSSPrimitiveValue) ret[1]);
             addProperties(declarations, primitives, names, origin, important);
 
         }
         if (bgPos != null) {
             val = bgPos.toString().trim();
 
             val = BackgroundPositionPropertyDeclarationFactory.canonicalizeValue(val);
 
             bgPosPrimitive.setCssText(val);
             names[0] = CSSName.BACKGROUND_POSITION;
             primitives[0] = new FSCssValue(bgPosPrimitive, val);
             addProperties(declarations, primitives, names, origin, important);
         }
 
         return declarations.iterator();
     }
 
     /**
      * @param val
      * @param primitive
      * @param bgPos
      * @return
      */
     private Object[] parseSingle(String val, CSSPrimitiveValue primitive, StringBuffer bgPos) {
         Boolean wasBGP = Boolean.FALSE;
         CSSName expPropName = null;
         CSSPrimitiveValue bgPosPrimitive = null;
 
         if (Idents.looksLikeAColor(val)) {
             expPropName = CSSName.BACKGROUND_COLOR;
             primitive = new FSCssValue(primitive);
         } else if (Idents.looksLikeAURI(val) || "none".equals(val)) {
             expPropName = CSSName.BACKGROUND_IMAGE;
         } else if (Idents.looksLikeABGRepeat(val)) {
             expPropName = CSSName.BACKGROUND_REPEAT;
         } else if (Idents.looksLikeABGAttachment(val)) {
             expPropName = CSSName.BACKGROUND_ATTACHMENT;
         } else if (Idents.looksLikeABGPosition(val)) {
             if (bgPos == null) {
                 bgPos = new StringBuffer(val);
                 bgPosPrimitive = primitive;
             } else {
                 bgPos.append(" " + val);
             }
             wasBGP = Boolean.TRUE;
         } else {
             expPropName = null;
         }
         return new Object[]{expPropName, primitive, bgPos, bgPosPrimitive, wasBGP};
     }
 
     /**
      * Returns the singleton instance.
      *
      * @return Returns
      */
     public static synchronized PropertyDeclarationFactory instance() {
         if (_instance == null) {
             _instance = new BackgroundPropertyDeclarationFactory();
         }
         return _instance;
     }
 }// end class
 
 /*
  * $Id$
  *
  * $Log$
 * Revision 1.9  2005/07/22 23:48:29  tobega
 * fixed background shorthand factory bug (hope no new ones introduced)
 *
  * Revision 1.8  2005/07/02 09:40:23  tobega
  * More robust parsing
  *
  * Revision 1.7  2005/06/02 23:38:29  tobega
  * Now handles background-position idents
  *
  * Revision 1.6  2005/05/08 13:02:37  tobega
  * Fixed a bug whereby styles could get lost for inline elements, notably if root element was inline. Did a few other things which probably has no importance at this moment, e.g. refactored out some unused stuff.
  *
  * Revision 1.5  2005/02/02 12:11:25  pdoubleya
  * Instantiate properties with FSCSSValue to handle URIs correctly; remove printlns.
  *
  * Revision 1.4  2005/01/29 20:24:25  pdoubleya
  * Clean/reformat code. Removed commented blocks, checked copyright.
  *
  * Revision 1.3  2005/01/29 12:14:20  pdoubleya
  * Removed priority as a parameter, added alternate build when only CSSValue is available; could be used in a SAC DocumentHandler after the CSSValue is initialized from a property.
  *
  * Revision 1.2  2005/01/24 19:00:57  pdoubleya
  * Mass checkin. Changed to use references to CSSName, which now has a Singleton instance for each property, everywhere property names were being used before. Removed commented code. Cascaded and Calculated style now store properties in arrays rather than maps, for optimization.
  *
  * Revision 1.1  2005/01/24 14:25:33  pdoubleya
  * Added to CVS.
  *
  *
  */
 
