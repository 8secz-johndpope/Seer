 /**
  * Copyright 2013 ArcBees Inc.
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
 
 package com.jci.client.resource.program;
 
 import com.google.gwt.resources.client.ClientBundle;
 import com.google.gwt.resources.client.CssResource;
 import com.google.gwt.resources.client.ImageResource;
 
 public interface ProgramResource extends ClientBundle {
     public static int COL_WIDTH = 145;
     public static int COL_HEIGTH = 80;
     public static int BEFORE_LEFT = 0;
     public static int BEFORE_TOP= 30;
 
     public interface Style extends CssResource {
         String hoursTable();
 
         String carouselInner();
 
         String carouselInnerDiv();
 
         String rooms();
 
         String visioProgram();
 
         String odd();
 
         String l1();
 
         String l2();
 
         String event();
 
         String t800();
 
         String h400();
 
         String h200();
 
         String w1();
 
         String w2();
 
         String h500();
 
         String t1400();
 
         String t1800();
 
         String t2300();
 
         String l5();
 
         String t700();
 
         String t900();
 
         String t1000();
 
         String t1100();
 
         String t1200();
 
         String t1300();
 
         String h100();
 
         String h275();
 
         String t1375();
 
         String l3();
 
         String h125();
 
         String t1050();
 
         String t1550();
 
         String t1350();
 
         String l4();
 
         String t1525();
 
         String h150();
 
         String t1175();
 
         String h025();
 
         String t1275();
 
         String h300();
 
         String t1700();
 
         String t1475();
 
         String t1325();
 
         String h425();
 
         String t2025();
 
         String h225();
 
         String h250();
 
         String t2050();
 
         String l6();
 
         String t750();
     }
 
     @ImageResource.ImageOptions(repeatStyle = ImageResource.RepeatStyle.Both)
     ImageResource tuile();
 
     @CssResource.NotStrict
     Style style();
 }
