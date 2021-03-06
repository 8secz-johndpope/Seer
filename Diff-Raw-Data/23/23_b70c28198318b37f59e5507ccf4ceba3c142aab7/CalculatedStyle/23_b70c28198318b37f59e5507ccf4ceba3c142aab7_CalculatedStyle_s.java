 /*
  * CalculatedStyle.java
  * Copyright (c) 2004, 2005 Patrick Wright, Torbj�rn Gannholm
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
  *
  */
 package org.xhtmlrenderer.css.style;
 
 import org.w3c.dom.css.CSSPrimitiveValue;
 import org.w3c.dom.css.RGBColor;
 import org.xhtmlrenderer.css.constants.CSSName;
 import org.xhtmlrenderer.css.constants.IdentValue;
 import org.xhtmlrenderer.css.constants.Idents;
 import org.xhtmlrenderer.css.constants.ValueConstants;
 import org.xhtmlrenderer.css.newmatch.CascadedStyle;
 import org.xhtmlrenderer.css.sheet.PropertyDeclaration;
 import org.xhtmlrenderer.css.style.derived.BorderPropertySet;
 import org.xhtmlrenderer.css.style.derived.DerivedValueFactory;
 import org.xhtmlrenderer.css.style.derived.LengthValue;
 import org.xhtmlrenderer.css.style.derived.RectPropertySet;
 import org.xhtmlrenderer.css.value.FontSpecification;
 import org.xhtmlrenderer.util.XRRuntimeException;
 
 import java.awt.Color;
 import java.awt.Point;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.Map;
 
 
 /**
  * A set of properties that apply to a single Element, derived from all matched
  * properties following the rules for CSS cascade, inheritance, importance,
  * specificity and sequence. A derived style is just like a style but
  * (presumably) has additional information that allows relative properties to be
  * assigned values, e.g. font attributes. Property values are fully resolved
  * when this style is created. A property retrieved by name should always have
  * only one value in this class (e.g. one-one map). Any methods to retrieve
  * property values from an instance of this class require a valid {@link
  * org.xhtmlrenderer.layout.Context} be given to it, for some cases of property
  * resolution. Generally, a programmer will not use this class directly, but
  * will retrieve properties using a {@link org.xhtmlrenderer.context.StyleReference}
  * implementation.
  *
  * @author Torbjrn Gannholm
  * @author Patrick Wright
  */
 public class CalculatedStyle {
     /**
      * Map of the RectPropertySets we have cached; keys are generated by the instances themselves.
      * These include margins, borders, and paddings.
      */
     private static final Map _cachedRects = new HashMap(500);
 
     /**
      * The parent-style we inherit from
      */
     private CalculatedStyle _parent;
 
     /**
      * A key for this style; names each of the properties defined in the style,
      * so is a "unique" identifier for the style, matching any other style
      * which have the same properties defined.
      */
     private String _styleKey;
 
     private BorderPropertySet _border;
     private RectPropertySet _margin;
     private RectPropertySet _padding;
 
     /**
      * Cache child styles of this style that have the same cascaded properties
      */
     private java.util.HashMap _childCache = new java.util.HashMap();
     /*private java.util.HashMap _childCache = new java.util.LinkedHashMap(5, 0.75f, true) {
         private static final int MAX_ENTRIES = 10;
 
         protected boolean removeEldestEntry(Map.Entry eldest) {
             return size() > MAX_ENTRIES;
         }
     };*/
 
     /**
      * Our main array of property values defined in this style, keyed
      * by the CSSName assigned ID.
      */
     private FSDerivedValue[] _derivedValuesById;
 
     /**
      * The derived Font for this style
      */
     private FontSpecification _font;
 
 
     /**
      * Default constructor; as the instance is immutable after use, don't use
      * this for class instantiation externally.
      */
     protected CalculatedStyle() {
         _derivedValuesById = new FSDerivedValue[CSSName.countCSSPrimitiveNames()];
     }
 
 
     /**
      * Constructor for the CalculatedStyle object. To get a derived style, use
      * the Styler objects getDerivedStyle which will cache styles
      *
      * @param parent  PARAM
      * @param matched PARAM
      */
     CalculatedStyle(CalculatedStyle parent, CascadedStyle matched) {
         this();
         _parent = parent;
 
         derive(matched);
         this._styleKey = genStyleKey();
     }
 
     /**
      * derives a child style from this style.
      * <p/>
      * depends on the ability to return the identical CascadedStyle each time a child style is needed
      *
      * @param matched the CascadedStyle to apply
      * @return The derived child style
      */
     public synchronized CalculatedStyle deriveStyle(CascadedStyle matched) {
         String fingerprint = matched.getFingerprint();
         CalculatedStyle cs = (CalculatedStyle) _childCache.get(fingerprint);
 
         if (cs == null) {
             cs = new CalculatedStyle(this, matched);
             _childCache.put(fingerprint, cs);
         }
         return cs;
     }
 
     public synchronized CalculatedStyle deriveNewStyle(CascadedStyle matched) {
         return new CalculatedStyle(this, matched);
     }
 
     public int countAssigned() {
         int c = 0;
         for (int i = 0; i < _derivedValuesById.length; i++) {
             if (_derivedValuesById[i] != null) c++;
         }
         return c;
     }
 
     /**
      * Returns the parent style.
      *
      * @return Returns the parent style
      */
     public CalculatedStyle getParent() {
         return _parent;
     }
 
     /**
      * Converts to a String representation of the object.
      *
      * @return The borderWidth value
      */
     public String toString() {
         return _styleKey;
     }
 
     public Color asColor(CSSName cssName) {
         return valueByName(cssName).asColor();
     }
 
     public float asFloat(CSSName cssName) {
         return valueByName(cssName).asFloat();
     }
 
     public String[] asStringArray(CSSName cssName) {
         return valueByName(cssName).asStringArray();
     }
 
     // TODO: doc
     public boolean hasAbsoluteUnit(CSSName cssName) {
         return valueByName(cssName).hasAbsoluteUnit();
     }
 
     /**
      * Gets the ident attribute of the CalculatedStyle object
      *
      * @param cssName PARAM
      * @param val     PARAM
      * @return The ident value
      */
     public boolean isIdent(CSSName cssName, IdentValue val) {
         return valueByName(cssName) == val;
     }
 
     /**
      * Gets the ident attribute of the CalculatedStyle object
      *
      * @param cssName PARAM
      * @return The ident value
      */
     public IdentValue getIdent(CSSName cssName) {
         return valueByName(cssName).asIdentValue();
     }
 
     /**
      * Convenience property accessor; returns a Color initialized with the
      * foreground color Uses the actual value (computed actual value) for this
      * element.
      *
      * @return The color value
      */
     public Color getColor() {
         return valueByName(CSSName.COLOR).asColor();
     }
 
     /**
      * Convenience property accessor; returns a Color initialized with the
      * background color value; Uses the actual value (computed actual value) for
      * this element.
      *
      * @return The backgroundColor value
      */
     public Color getBackgroundColor() {
         return asColor(CSSName.BACKGROUND_COLOR);
     }
 
     /**
      * @param parentWidth
      * @param parentHeight
      * @param ctx
      * @return The "background-position" property as a Point
      */
     public Point getBackgroundPosition(float parentWidth, float parentHeight, CssContext ctx) {
         return valueByName(CSSName.BACKGROUND_POSITION).asPoint(CSSName.BACKGROUND_POSITION, parentWidth, parentHeight, ctx);
     }
 
     public BorderPropertySet getBorder(CssContext ctx) {
         BorderPropertySet b = getBorderProperty(this, ctx);
         return b;
     }
 
     public FontSpecification getFont(CssContext ctx) {
         if (_font == null) {
             _font = new FontSpecification();
             _font.size = getFloatPropertyProportionalTo(CSSName.FONT_SIZE, 0, ctx);
 
             _font.fontWeight = getIdent(CSSName.FONT_WEIGHT);
             _font.families = valueByName(CSSName.FONT_FAMILY).asStringArray();
 
             _font.fontStyle = getIdent(CSSName.FONT_STYLE);
             _font.variant = getIdent(CSSName.FONT_VARIANT);
         }
         return _font;
     }
 
     public float getFloatPropertyProportionalTo(CSSName cssName, float baseValue, CssContext ctx) {
         return valueByName(cssName).getFloatProportionalTo(cssName, baseValue, ctx);
     }
 
     /**
      * @param cssName
      * @param parentWidth
      * @param ctx
      * @return TODO
      */
     public float getFloatPropertyProportionalWidth(CSSName cssName, float parentWidth, CssContext ctx) {
         return valueByName(cssName).getFloatProportionalTo(cssName, parentWidth, ctx);
     }
 
     /**
      * @param cssName
      * @param parentHeight
      * @param ctx
      * @return TODO
      */
     public float getFloatPropertyProportionalHeight(CSSName cssName, float parentHeight, CssContext ctx) {
         return valueByName(cssName).getFloatProportionalTo(cssName, parentHeight, ctx);
     }
 
     public float getLineHeight(CssContext ctx) {
         if (isIdent(CSSName.LINE_HEIGHT, IdentValue.NORMAL)) {
             return getFont(ctx).size * 1.1f;//css recommends something between 1.0 and 1.2
         } else if (isLengthValue(CSSName.LINE_HEIGHT)) {
             //could be more elegant, I suppose
             return getFloatPropertyProportionalHeight(CSSName.LINE_HEIGHT, 0, ctx);
         } else {
             //must be a number
             return getFont(ctx).size * valueByName(CSSName.LINE_HEIGHT).asFloat();
         }
     }
 
     /**
      * Convenience property accessor; returns a Border initialized with the
      * four-sided margin width. Uses the actual value (computed actual value)
      * for this element.
      *
      * @param parentWidth
      * @param parentHeight
      * @param ctx
      * @return The marginWidth value
      */
     public RectPropertySet getMarginRect(float parentWidth, float parentHeight, CssContext ctx) {
         return getMarginProperty(this, CSSName.MARGIN_SHORTHAND, CSSName.MARGIN_SIDE_PROPERTIES, parentWidth, parentHeight, ctx);
     }
 
     /**
      * Convenience property accessor; returns a Border initialized with the
      * four-sided padding width. Uses the actual value (computed actual value)
      * for this element.
      *
      * @param parentWidth
      * @param parentHeight
      * @param ctx
      * @return The paddingWidth value
      */
     public RectPropertySet getPaddingRect(float parentWidth, float parentHeight, CssContext ctx) {
         return getPaddingProperty(this, CSSName.PADDING_SHORTHAND, CSSName.PADDING_SIDE_PROPERTIES, parentWidth, parentHeight, ctx);
     }
 
     /**
      * @param cssName
      * @return TODO
      */
     public String getStringProperty(CSSName cssName) {
         return valueByName(cssName).asString();
     }
 
     /**
      * TODO: doc
      */
     public boolean isLengthValue(CSSName cssName) {
         FSDerivedValue val = valueByName(cssName);
         return val instanceof LengthValue;
     }
 
     public FSDerivedValue copyOf(CSSName cssName) {
         return valueByName(cssName).copyOf(cssName);
     }
 
     /**
      * Returns a {@link FSDerivedValue} by name. Because we are a derived
      * style, the property will already be resolved at this point.
      *
      * @param cssName The CSS property name, e.g. "font-family"
      * @return See desc.
      */
     public FSDerivedValue valueByName(CSSName cssName) {
         FSDerivedValue val = _derivedValuesById[cssName.FS_ID];
 
         // but the property may not be defined for this Element
         if (val == null) {
             // if it is inheritable (like color) and we are not root, ask our parent
             // for the value
             if (CSSName.propertyInherits(cssName)
                     && _parent != null
                     //
                     && (val = _parent.valueByName(cssName)) != null) {
 
                 val = val.copyOf(cssName);
             } else {
                 // otherwise, use the initial value (defined by the CSS2 Spec)
                 String initialValue = CSSName.initialValue(cssName);
                 if (initialValue == null) {
                     throw new XRRuntimeException("Property '" + cssName + "' has no initial values assigned. " +
                             "Check CSSName declarations.");
                 }
                 if (initialValue.startsWith("=")) {
                     CSSName ref = CSSName.getByPropertyName(initialValue.substring(1));
                     val = valueByName(ref);
                 } else {
                     initialValue = Idents.convertIdent(cssName, initialValue);
 
                     short type = ValueConstants.guessType(initialValue);
 
                     val = DerivedValueFactory.newDerivedValue(this,
                             cssName,
                             type,
                             initialValue,
                             initialValue,
                             null);
                 }
             }
             _derivedValuesById[cssName.FS_ID] = val;
         }
         return val;
     }
 
     /**
      * <p/>
      * <p/>
      * <p/>
      * <p/>
      * Implements cascade/inherit/important logic. This should result in the
      * element for this style having a value for *each and every* (visual)
      * property in the CSS2 spec. The implementation is based on the notion that
      * the matched styles are given to us in a perfectly sorted order, such that
      * properties appearing later in the rule-set always override properties
      * appearing earlier. It also assumes that all properties in the CSS2 spec
      * are defined somewhere across all the matched styles; for example, that
      * the full-property set is given in the user-agent CSS that is always
      * loaded with styles. The current implementation makes no attempt to check
      * either of these assumptions. When this method exits, the derived property
      * list for this class will be populated with the properties defined for
      * this element, properly cascaded.</p>
      *
      * @param matched PARAM
      */
     private void derive(CascadedStyle matched) {
         if (matched == null) {
             return;
         }//nothing to derive
 
         Iterator mProps = matched.getCascadedPropertyDeclarations();
         while (mProps.hasNext()) {
             PropertyDeclaration pd = (PropertyDeclaration) mProps.next();
             FSDerivedValue val = deriveValue(pd.getCSSName(), pd.getValue());
             _derivedValuesById[pd.getCSSName().FS_ID] = val;
         }
     }
 
     private FSDerivedValue deriveValue(CSSName cssName, org.w3c.dom.css.CSSPrimitiveValue value) {
         // Start assuming our computed value is the same as the specified value
         RGBColor rgb = (value.getPrimitiveType() == CSSPrimitiveValue.CSS_RGBCOLOR ? value.getRGBColorValue() : null);
         String s = (value.getPrimitiveType() == CSSPrimitiveValue.CSS_STRING ? value.getStringValue() : null);
 
         // derive the value, will also handle "inherit"
         FSDerivedValue dval = DerivedValueFactory.newDerivedValue(this,
                 cssName,
                 value.getPrimitiveType(),
                value.getCssText(),
                 s,
                 rgb);
         return dval;
     }
 
     private String genStyleKey() {
         StringBuffer sb = new StringBuffer();
         for (int i = 0; i < _derivedValuesById.length; i++) {
             CSSName name = CSSName.getByID(i);
             FSDerivedValue val = _derivedValuesById[i];
             if (val != null) {
                 sb.append(name.toString());
             } else {
                 sb.append("(no prop assigned in this pos)");
             }
             sb.append("|\n");
         }
         return sb.toString();
 
     }
 
     public RectPropertySet getCachedPadding() {
         if (_padding == null) {
             throw new XRRuntimeException("No padding property cached yet; should have called getPropertyRect() at least once before.");
         } else {
             return _padding;
         }
     }
 
     public RectPropertySet getCachedMargin() {
         if (_margin == null) {
             throw new XRRuntimeException("No margin property cached yet; should have called getMarginRect() at least once before.");
         } else {
             return _margin;
         }
     }
 
     private static RectPropertySet getPaddingProperty(CalculatedStyle style,
                                                       CSSName shorthandProp,
                                                       CSSName[] sides,
                                                       float parentWidth,
                                                       float parentHeight,
                                                       CssContext ctx) {
         String key = null;
 
         if (style._padding == null) {
             key = RectPropertySet.deriveKey(style, sides);
             if (key == null) {
                 style._padding = newRectInstance(style, shorthandProp, sides, parentHeight, parentWidth, ctx);
                 return style._padding;
             } else {
                 style._padding = (RectPropertySet) _cachedRects.get(key);
                 if (style._padding == null) {
                     style._padding = newRectInstance(style, shorthandProp, sides, parentHeight, parentWidth, ctx);
                     _cachedRects.put(key, style._padding);
                 }
             }
         }
 
         return style._padding;
     }
 
     private static RectPropertySet getMarginProperty(CalculatedStyle style,
                                                      CSSName shorthandProp,
                                                      CSSName[] sides,
                                                      float parentWidth,
                                                      float parentHeight,
                                                      CssContext ctx) {
         String key = null;
 
         if (style._margin == null) {
             key = RectPropertySet.deriveKey(style, sides);
             if (key == null) {
                 style._margin = newRectInstance(style, shorthandProp, sides, parentHeight, parentWidth, ctx);
                 return style._margin;
             } else {
                 style._margin = (RectPropertySet) _cachedRects.get(key);
                 if (style._margin == null) {
                     style._margin = newRectInstance(style, shorthandProp, sides, parentHeight, parentWidth, ctx);
                     _cachedRects.put(key, style._margin);
                 }
             }
         }
 
         return style._margin;
     }
 
     private static RectPropertySet newRectInstance(CalculatedStyle style,
                                                    CSSName shorthand,
                                                    CSSName[] sides,
                                                    float parentHeight,
                                                    float parentWidth,
                                                    CssContext ctx) {
         RectPropertySet rect;
         rect = RectPropertySet.newInstance(style,
                 shorthand,
                 sides,
                 parentHeight,
                 parentWidth,
                 ctx);
         return rect;
     }
 
     private static BorderPropertySet getBorderProperty(CalculatedStyle style,
                                                        CssContext ctx) {
         if (style._border == null) {
             String key = BorderPropertySet.deriveKey(style);
             style._border = (BorderPropertySet) _cachedRects.get(key);
             if (style._border == null) {
                 style._border = BorderPropertySet.newInstance(style, ctx);
                 _cachedRects.put(key, style._border);
             }
         }
         return style._border;
     }
 }// end class
 
 /*
  * $Id$
  *
  * $Log$
  * Revision 1.53  2005/11/08 22:53:44  tobega
  * added getLineHeight method to CalculatedStyle and hacked in some list-item support
  *
  * Revision 1.52  2005/10/31 22:43:15  tobega
  * Some memory optimization of the Matcher. Probably cleaner code, too.
  *
  * Revision 1.51  2005/10/31 19:02:12  pdoubleya
  * support for inherited padding and margins.
  *
  * Revision 1.50  2005/10/31 18:01:44  pdoubleya
  * InheritedLength is created per-length, to accomodate calls that need to defer to a specific parent.
  *
  * Revision 1.49  2005/10/31 12:38:14  pdoubleya
  * Additional inheritance fixes.
  *
  * Revision 1.48  2005/10/31 10:16:08  pdoubleya
  * Preliminary support for inherited lengths.
  *
  * Revision 1.47  2005/10/25 15:38:28  pdoubleya
  * Moved guessType() to ValueConstants, applied fix to method suggested by Chris Oliver, to avoid exception-based catch.
  *
  * Revision 1.46  2005/10/25 00:38:47  tobega
  * Reduced memory footprint of Matcher and stopped trying to cache the possibly uncache-able CascadedStyles, the fingerprint works just as well or better as a key in CalculatedStyle!
  *
  * Revision 1.45  2005/10/24 15:37:35  pdoubleya
  * Caching border, margin and property instances directly.
  *
  * Revision 1.44  2005/10/24 10:19:40  pdoubleya
  * CSSName FS_ID is now public and final, allowing direct access to the id, bypassing getAssignedID(); micro-optimization :); getAssignedID() and setAssignedID() have been removed. IdentValue string property is also final (as should have been).
  *
  * Revision 1.43  2005/10/22 22:58:15  peterbrant
  * Box level restyle works again (really this time!)
  *
  * Revision 1.42  2005/10/21 23:51:48  peterbrant
  * Rollback ill-advised change in revision 1.40
  *
  * Revision 1.41  2005/10/21 23:11:26  pdoubleya
  * Store key for margin, border and padding in each style instance, was re-creating on each call.
  *
  * Revision 1.40  2005/10/21 23:04:02  peterbrant
  * Make box level restyle work again
  *
  * Revision 1.39  2005/10/21 18:49:46  pdoubleya
  * Fixed border painting bug.
  *
  * Revision 1.38  2005/10/21 18:14:59  pdoubleya
  * set  initial capacity for cached rects.
  *
  * Revision 1.37  2005/10/21 18:10:50  pdoubleya
  * Support for cachable borders. Still buggy on some pages, but getting there.
  *
  * Revision 1.36  2005/10/21 13:02:20  pdoubleya
  * Changed to cache padding in RectPropertySet.
  *
  * Revision 1.35  2005/10/21 12:20:04  pdoubleya
  * Added array for margin side props.
  *
  * Revision 1.34  2005/10/21 12:16:18  pdoubleya
  * Removed use of MarginPropertySet; using RectPS  now.
  *
  * Revision 1.33  2005/10/21 12:01:13  pdoubleya
  * Added cachable rect property for margin, cleanup minor in styling.
  *
  * Revision 1.32  2005/10/21 10:02:54  pdoubleya
  * Cleanup, removed unneeded vars, reorg code in CS.
  *
  * Revision 1.31  2005/10/20 20:48:01  pdoubleya
  * Updates for refactoring to style classes. CalculatedStyle now has lookup methods to cover all general cases, so propertyByName() is private, which means the backing classes for styling were able to be replaced.
  *
  * Revision 1.30  2005/10/03 23:44:43  tobega
  * thread-safer css code and improved style caching
  *
  * Revision 1.29  2005/09/11 20:43:15  tobega
  * Fixed table-css interaction bug, colspan now works again
  *
  * Revision 1.28  2005/07/20 22:47:33  joshy
  * fix for 94, percentage for top absolute position
  *
  * Revision 1.27  2005/06/22 23:48:41  tobega
  * Refactored the css package to allow a clean separation from the core.
  *
  * Revision 1.26  2005/06/21 08:23:13  pdoubleya
  * Added specific list and count of primitive, non shorthand properties, and CalculatedStyle now sizes array to this size.
  *
  * Revision 1.25  2005/06/16 07:24:46  tobega
  * Fixed background image bug.
  * Caching images in browser.
  * Enhanced LinkListener.
  * Some house-cleaning, playing with Idea's code inspection utility.
  *
  * Revision 1.24  2005/06/03 23:06:21  tobega
  * Now uses value of "color" as initial value for "border-color" and rgb-triples are supported
  *
  * Revision 1.23  2005/06/01 00:47:02  tobega
  * Partly confused hack trying to get width and height working properly for replaced elements.
  *
  * Revision 1.22  2005/05/29 16:38:58  tobega
  * Handling of ex values should now be working well. Handling of em values improved. Is it correct?
  * Also started defining dividing responsibilities between Context and RenderingContext.
  *
  * Revision 1.21  2005/05/13 11:49:57  tobega
  * Started to fix up borders on inlines. Got caught up in refactoring.
  * Boxes shouldn't cache borders and stuff unless necessary. Started to remove unnecessary references.
  * Hover is not working completely well now, might get better when I'm done.
  *
  * Revision 1.20  2005/05/09 20:35:38  tobega
  * Caching fonts in CalculatedStyle
  *
  * Revision 1.19  2005/05/08 15:37:28  tobega
  * Fixed up style caching so it really works (internalize CascadedStyles and let each CalculatedStyle keep track of its derived children)
  *
  * Revision 1.18  2005/05/08 14:51:21  tobega
  * Removed the need for the Styler
  *
  * Revision 1.17  2005/05/08 14:36:54  tobega
  * Refactored away the need for having a context in a CalculatedStyle
  *
  * Revision 1.16  2005/04/07 16:33:34  pdoubleya
  * Fix border width if set to "none" in CSS (Kevin).
  *
  * Revision 1.15  2005/03/24 23:16:33  pdoubleya
  * Added use of SharedContext (Kevin).
  *
  * Revision 1.14  2005/02/03 23:15:50  pdoubleya
  * .
  *
  * Revision 1.13  2005/01/29 20:22:20  pdoubleya
  * Clean/reformat code. Removed commented blocks, checked copyright.
  *
  * Revision 1.12  2005/01/25 12:46:12  pdoubleya
  * Refactored duplicate code into separate method.
  *
  * Revision 1.11  2005/01/24 22:46:43  pdoubleya
  * Added support for ident-checks using IdentValue instead of string comparisons.
  *
  * Revision 1.10  2005/01/24 19:01:05  pdoubleya
  * Mass checkin. Changed to use references to CSSName, which now has a Singleton instance for each property, everywhere property names were being used before. Removed commented code. Cascaded and Calculated style now store properties in arrays rather than maps, for optimization.
  *
  * Revision 1.9  2005/01/24 14:36:31  pdoubleya
  * Mass commit, includes: updated for changes to property declaration instantiation, and new use of DerivedValue. Removed any references to older XR... classes (e.g. XRProperty). Cleaned imports.
  *
  * Revision 1.8  2004/12/05 18:11:36  tobega
  * Now uses style cache for pseudo-element styles. Also started preparing to replace inline node handling with inline content handling.
  *
  * Revision 1.7  2004/12/05 00:48:54  tobega
  * Cleaned up so that now all property-lookups use the CalculatedStyle. Also added support for relative values of top, left, width, etc.
  *
  * Revision 1.6  2004/11/15 12:42:23  pdoubleya
  * Across this checkin (all may not apply to this particular file)
  * Changed default/package-access members to private.
  * Changed to use XRRuntimeException where appropriate.
  * Began move from System.err.println to std logging.
  * Standard code reformat.
  * Removed some unnecessary SAC member variables that were only used in initialization.
  * CVS log section.
  *
  *
  */
 
