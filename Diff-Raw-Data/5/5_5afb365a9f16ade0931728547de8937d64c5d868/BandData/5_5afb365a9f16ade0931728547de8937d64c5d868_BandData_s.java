 /*
  * Copyright 2013 Haulmont
  *
  * Licensed under the Apache License, Version 2.0 (the "License"); you may not
  * use this file except in compliance with the License. You may obtain a copy of
  * the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  * License for the specific language governing permissions and limitations under
  * the License.
  */
 
 package com.haulmont.yarg.structure;
 
 import com.haulmont.yarg.structure.impl.BandOrientation;
 
 import java.util.*;
 
 /**
  * @author degtyarjov
  * @version $Id$
  */
 
 @SuppressWarnings({"UnusedDeclaration"})
 public class BandData {
     public static final String ROOT_BAND_NAME = "Root";
 
     protected Map<String, Object> data = new HashMap<String, Object>(10);
     protected BandData parentBand;
 
     protected Map<String, List<BandData>> childrenBands = new LinkedHashMap<String, List<BandData>>();
 
     protected final String name;
     protected final BandOrientation orientation;
     protected Set<String> firstLevelBandDefinitionNames = null;
     protected int level;
     protected Map<String, ReportFieldFormat> reportFieldFormats = new HashMap<String, ReportFieldFormat>();
 
 
     public BandData(String name) {
         this.name = name;
         this.parentBand = null;
         this.orientation = BandOrientation.HORIZONTAL;
     }
 
     public BandData(String name, BandData parentBand) {
         this.name = name;
         this.parentBand = parentBand;
         this.orientation = BandOrientation.HORIZONTAL;
     }
 
     public BandData(String name, BandData parentBand, BandOrientation orientation) {
         this.name = name;
         this.parentBand = parentBand;
         this.orientation = orientation;
 
         BandData currentBand = this;
         while (currentBand != null) {
             level++;
             currentBand = currentBand.parentBand;
         }
     }
 
     public Map<String, List<BandData>> getChildrenBands() {
         return childrenBands;
     }
 
     public Map<String, Object> getData() {
         return data;
     }
 
     public void setData(Map<String, Object> data) {
         this.data = data;
     }
 
     public void addData(String name, Object value) {
         data.put(name, value);
     }
 
     public Object getParameterValue(String name) {
         return data.get(name);
     }
 
     public void addAllParameters(Map<String, Object> parameters) {
         data.putAll(parameters);
     }
 
     public String getName() {
         return name;
     }
 
     public BandData getParentBand() {
         return parentBand;
     }
 
     public void setParentBand(BandData parentBand) {
         this.parentBand = parentBand;
     }
 
     public BandOrientation getOrientation() {
         return orientation;
     }
 
     public int getLevel() {
         return level;
     }
 
     public String getFullName() {
         String fullName = name;
         BandData upBand = parentBand;
         while ((upBand != null) && (upBand.level > 1)) {
             fullName = upBand.getName() + "." + fullName;
             upBand = upBand.parentBand;
         }
         return fullName;
     }
 
     public List<BandData> getChildrenList() {
         List<BandData> bandList = new ArrayList<BandData>();
         for (List<BandData> bands : childrenBands.values())
             bandList.addAll(bands);
         return bandList;
     }
 
     public void addChild(BandData band) {
         if (!childrenBands.containsKey(band.getName())) {
             childrenBands.put(band.getName(), new ArrayList<BandData>());
         }
         List<BandData> bands = childrenBands.get(band.getName());
         bands.add(band);
     }
 
     public void addChildren(List<BandData> bands) {
         for (BandData band : bands)
             addChild(band);
     }
 
     public BandData getChildByName(String bandName) {
         if (bandName == null) {
             throw new NullPointerException("Parameter bandName can not be null.");
         }
         if (getChildrenList() != null) {
             for (BandData child : getChildrenList()) {
                 if (bandName.equals(child.getName())) {
                     return child;
                 }
             }
         }
         return null;
     }
 
     public boolean visit(BandVisitor bandVisitor) {
         if (bandVisitor.visit(this)) {
             return true;
         }
 
         for (BandData child : getChildrenList()) {
             if (child.visit(bandVisitor)) {
                 return true;
             }
         }
 
         return false;
     }
 
     //todo use visitor
     public BandData findBandRecursively(String name) {
         if (getName().equals(name)) {
             return this;
         }
         for (BandData child : getChildrenList()) {
             BandData fromChild = child.findBandRecursively(name);
             if (fromChild != null) {
                 return fromChild;
             }
         }
         return null;
     }
 
     public List<BandData> findBandsRecursively(String name) {
         BandData firstBand = findBandRecursively(name);
         List<BandData> allBand = firstBand.getParentBand().getChildrenByName(name);
         return allBand;
     }
 
     public List<BandData> getChildrenByName(String bandName) {
         if (bandName == null) {
             throw new NullPointerException("Parameter bandName can not be null.");
         }
 
         List<BandData> children = new ArrayList<BandData>();
         for (BandData child : getChildrenList()) {
             if (bandName.equals(child.getName())) {
                 children.add(child);
             }
         }
         return children;
     }
 
     public Set<String> getFirstLevelBandDefinitionNames() {
         return firstLevelBandDefinitionNames;
     }
 
     public void setFirstLevelBandDefinitionNames(Set<String> firstLevelBandDefinitionNames) {
         this.firstLevelBandDefinitionNames = firstLevelBandDefinitionNames;
     }
 
     public void setReportFieldFormats(List<ReportFieldFormat> reportFieldFormats) {
         for (ReportFieldFormat reportFieldFormat : reportFieldFormats) {
             this.reportFieldFormats.put(reportFieldFormat.getName(), reportFieldFormat);
         }
     }
 
     public Map<String, ReportFieldFormat> getReportFieldFormats() {
         return reportFieldFormats;
     }
 
     @Override
     public String toString() {
         StringBuilder sbf = new StringBuilder();
         sbf.append(name).append(":").append(data.toString()).append("\n");
         for (BandData band : getChildrenList()) {
             for (int i = 0; i < level; i++)
                 sbf.append("\t");
             sbf.append(band.toString());
         }
         return sbf.toString();
     }
 }
 
