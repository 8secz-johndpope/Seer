 /**
  * Geotools - OpenSource mapping toolkit
  *            (C) 2002, Centre for Computational Geography
  *
  *    This library is free software; you can redistribute it and/or
  *    modify it under the terms of the GNU Lesser General Public
  *    License as published by the Free Software Foundation;
  *    version 2.1 of the License.
  *
  *    This library is distributed in the hope that it will be useful,
  *    but WITHOUT ANY WARRANTY; without even the implied warranty of
  *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  *    Lesser General Public License for more details.
  *
  *    You should have received a copy of the GNU Lesser General Public
  *    License along with this library; if not, write to the Free Software
  *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
  */
 
 package org.geotools.styling;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 /**
 * @version $Id: StyleImpl.java,v 1.4 2002/10/21 16:10:18 ianturton Exp $
  * @author James Macgill, CCG
  */
 public class StyleImpl implements org.geotools.styling.Style {
 
     ArrayList featureTypeStyleList = new ArrayList();
     String abstractText = "";
     String name = "Default Styler";
     String title = "Default Styler";
     boolean defaultB = true;
     
     /** Creates a new instance of DefaultStyle */
     protected StyleImpl() {
 //        featureTypeStyleList = new FeatureTypeStyleImpl[1];
 //        featureTypeStyleList[0] = new FeatureTypeStyleImpl();
 //        featureTypeStyleList[0].setFeatureTypeName("default");
 //        RuleImpl [] rules = new RuleImpl[1];
 //        rules[0] = new RuleImpl();
 //        Symbolizer[] symbolizers = new Symbolizer[3];
 //        symbolizers[0] = new PolygonSymbolizerImpl();
 //        symbolizers[1] = new LineSymbolizerImpl();
 //        symbolizers[2] = new PointSymbolizerImpl();
 //        rules[0].setSymbolizers(symbolizers);
 //        ((FeatureTypeStyleImpl)featureTypeStyleList[0]).setRules(rules);
     }
 
     public String getAbstract() {
         return abstractText;
     }
     
     public FeatureTypeStyle[] getFeatureTypeStyles() {
       if( featureTypeStyleList.size() == 0){
           return new FeatureTypeStyle[0];
        }
       
       return (FeatureTypeStyle[]) featureTypeStyleList.toArray(new FeatureTypeStyle[0]);
        
     }
     
     public void setFeatureTypeStyles(FeatureTypeStyle[] featureTypeStyles){
        featureTypeStyleList.add(java.util.Arrays.asList(featureTypeStyles));
     }
     
     public String getName() {
         return name;
     }
     
     public String getTitle() {
         return title;
     }
     
     public boolean isDefault() {
         return defaultB;
     }
     
     public void setAbstract(String abstractStr) {
         abstractText = abstractStr;
     }
     
     public void setIsDefault(boolean isDefault) {
         defaultB = isDefault;
     }
     
     public void setName(String name) {
         this.name = name;
     }
     
     public void setTitle(String title) {
         this.title = title;
     }
     
     public void addFeatureTypeStyle(FeatureTypeStyle type) {
         featureTypeStyleList.add(type);
     }
     
 }
