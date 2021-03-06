 /* ====================================================================
    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements.  See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to You under the Apache License, Version 2.0
    (the "License"); you may not use this file except in compliance with
    the License.  You may obtain a copy of the License at
 
        http://www.apache.org/licenses/LICENSE-2.0
 
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 ==================================================================== */
 
 package org.apache.poi.hssf.model;
 
 import junit.framework.TestCase;
 import org.apache.poi.ddf.*;
 import org.apache.poi.hssf.HSSFTestDataSamples;
 import org.apache.poi.hssf.record.CommonObjectDataSubRecord;
 import org.apache.poi.hssf.record.EscherAggregate;
 import org.apache.poi.hssf.record.ObjRecord;
 import org.apache.poi.hssf.usermodel.*;
 import org.apache.poi.ss.usermodel.Workbook;
 import org.apache.poi.util.HexDump;
 
 import java.io.IOException;
 import java.util.Arrays;
 
 import static junit.framework.Assert.assertEquals;
 
 /**
  * @author Evgeniy Berlog
  * date: 12.06.12
  */
 public class TestDrawingShapes extends TestCase {
 
     /**
      * HSSFShape tree bust be built correctly
      * Check file with such records structure:
      * -patriarch
      * --shape
      * --group
      * ---group
      * ----shape
      * ----shape
      * ---shape
      * ---group
      * ----shape
      * ----shape
      */
     public void testDrawingGroups() {
         HSSFWorkbook wb = HSSFTestDataSamples.openSampleWorkbook("drawings.xls");
         HSSFSheet sheet = wb.getSheet("groups");
         HSSFPatriarch patriarch = sheet.getDrawingPatriarch();
         assertEquals(patriarch.getChildren().size(), 2);
         HSSFShapeGroup group = (HSSFShapeGroup) patriarch.getChildren().get(1);
         assertEquals(3, group.getChildren().size());
         HSSFShapeGroup group1 = (HSSFShapeGroup) group.getChildren().get(0);
         assertEquals(2, group1.getChildren().size());
         group1 = (HSSFShapeGroup) group.getChildren().get(2);
         assertEquals(2, group1.getChildren().size());
     }
 
     public void testHSSFShapeCompatibility() {
         HSSFSimpleShape shape = new HSSFSimpleShape(null, new HSSFClientAnchor());
         shape.setShapeType(HSSFSimpleShape.OBJECT_TYPE_LINE);
         assertEquals(0x08000040, shape.getLineStyleColor());
         assertEquals(0x08000009, shape.getFillColor());
         assertEquals(HSSFShape.LINEWIDTH_DEFAULT, shape.getLineWidth());
         assertEquals(HSSFShape.LINESTYLE_SOLID, shape.getLineStyle());
         assertFalse(shape.isNoFill());
 
         AbstractShape sp = AbstractShape.createShape(shape, 1);
         EscherContainerRecord spContainer = sp.getSpContainer();
         EscherOptRecord opt =
                 spContainer.getChildById(EscherOptRecord.RECORD_ID);
 
         assertEquals(7, opt.getEscherProperties().size());
         assertEquals(true,
                 ((EscherBoolProperty) opt.lookup(EscherProperties.TEXT__SIZE_TEXT_TO_FIT_SHAPE)).isTrue());
         assertEquals(0x00000004,
                 ((EscherSimpleProperty) opt.lookup(EscherProperties.GEOMETRY__SHAPEPATH)).getPropertyValue());
         assertEquals(0x08000009,
                 ((EscherSimpleProperty) opt.lookup(EscherProperties.FILL__FILLCOLOR)).getPropertyValue());
         assertEquals(true,
                 ((EscherBoolProperty) opt.lookup(EscherProperties.FILL__NOFILLHITTEST)).isTrue());
         assertEquals(0x08000040,
                 ((EscherSimpleProperty) opt.lookup(EscherProperties.LINESTYLE__COLOR)).getPropertyValue());
         assertEquals(true,
                 ((EscherBoolProperty) opt.lookup(EscherProperties.LINESTYLE__NOLINEDRAWDASH)).isTrue());
         assertEquals(true,
                 ((EscherBoolProperty) opt.lookup(EscherProperties.GROUPSHAPE__PRINT)).isTrue());
     }
 
     public void testDefaultPictureSettings() {
         HSSFPicture picture = new HSSFPicture(null, new HSSFClientAnchor());
         assertEquals(picture.getLineWidth(), HSSFShape.LINEWIDTH_DEFAULT);
         assertEquals(picture.getFillColor(), HSSFShape.FILL__FILLCOLOR_DEFAULT);
         assertEquals(picture.getLineStyle(), HSSFShape.LINESTYLE_NONE);
         assertEquals(picture.getLineStyleColor(), HSSFShape.LINESTYLE__COLOR_DEFAULT);
         assertEquals(picture.isNoFill(), false);
         assertEquals(picture.getPictureIndex(), -1);//not set yet
     }
 
     /**
      * No NullPointerException should appear
      */
     public void testDefaultSettingsWithEmptyContainer() {
         EscherContainerRecord container = new EscherContainerRecord();
         EscherOptRecord opt = new EscherOptRecord();
         opt.setRecordId(EscherOptRecord.RECORD_ID);
         container.addChildRecord(opt);
         ObjRecord obj = new ObjRecord();
         CommonObjectDataSubRecord cod = new CommonObjectDataSubRecord();
         cod.setObjectType(HSSFSimpleShape.OBJECT_TYPE_PICTURE);
         obj.addSubRecord(cod);
         HSSFPicture picture = new HSSFPicture(container, obj);
 
         assertEquals(picture.getLineWidth(), HSSFShape.LINEWIDTH_DEFAULT);
         assertEquals(picture.getFillColor(), HSSFShape.FILL__FILLCOLOR_DEFAULT);
         assertEquals(picture.getLineStyle(), HSSFShape.LINESTYLE_DEFAULT);
         assertEquals(picture.getLineStyleColor(), HSSFShape.LINESTYLE__COLOR_DEFAULT);
         assertEquals(picture.isNoFill(), HSSFShape.NO_FILL_DEFAULT);
         assertEquals(picture.getPictureIndex(), -1);//not set yet
     }
 
     /**
      * create a rectangle, save the workbook, read back and verify that all shape properties are there
      */
     public void testReadWriteRectangle() throws IOException {
 
         HSSFWorkbook wb = new HSSFWorkbook();
         HSSFSheet sheet = wb.createSheet();
 
         HSSFPatriarch drawing = sheet.createDrawingPatriarch();
         HSSFClientAnchor anchor = new HSSFClientAnchor(10, 10, 50, 50, (short) 2, 2, (short) 4, 4);
         anchor.setAnchorType(2);
         assertEquals(anchor.getAnchorType(), 2);
 
         HSSFSimpleShape rectangle = drawing.createSimpleShape(anchor);
         rectangle.setShapeType(HSSFSimpleShape.OBJECT_TYPE_RECTANGLE);
         rectangle.setLineWidth(10000);
         rectangle.setFillColor(777);
         assertEquals(rectangle.getFillColor(), 777);
         assertEquals(10000, rectangle.getLineWidth());
         rectangle.setLineStyle(10);
         assertEquals(10, rectangle.getLineStyle());
         assertEquals(rectangle.getWrapText(), HSSFSimpleShape.WRAP_SQUARE);
         rectangle.setLineStyleColor(1111);
         rectangle.setNoFill(true);
         rectangle.setWrapText(HSSFSimpleShape.WRAP_NONE);
         rectangle.setString(new HSSFRichTextString("teeeest"));
         assertEquals(rectangle.getLineStyleColor(), 1111);
         assertEquals(((EscherSimpleProperty)((EscherOptRecord)HSSFTestHelper.getEscherContainer(rectangle).getChildById(EscherOptRecord.RECORD_ID))
                 .lookup(EscherProperties.TEXT__TEXTID)).getPropertyValue(), "teeeest".hashCode());
         assertEquals(rectangle.isNoFill(), true);
         assertEquals(rectangle.getWrapText(), HSSFSimpleShape.WRAP_NONE);
         assertEquals(rectangle.getString().getString(), "teeeest");
 
         wb = HSSFTestDataSamples.writeOutAndReadBack(wb);
         sheet = wb.getSheetAt(0);
         drawing = sheet.getDrawingPatriarch();
         assertEquals(1, drawing.getChildren().size());
 
         HSSFSimpleShape rectangle2 =
                 (HSSFSimpleShape) drawing.getChildren().get(0);
         assertEquals(HSSFSimpleShape.OBJECT_TYPE_RECTANGLE,
                 rectangle2.getShapeType());
         assertEquals(10000, rectangle2.getLineWidth());
         assertEquals(10, rectangle2.getLineStyle());
         assertEquals(anchor, rectangle2.getAnchor());
         assertEquals(rectangle2.getLineStyleColor(), 1111);
         assertEquals(rectangle2.getFillColor(), 777);
         assertEquals(rectangle2.isNoFill(), true);
         assertEquals(rectangle2.getString().getString(), "teeeest");
         assertEquals(rectangle.getWrapText(), HSSFSimpleShape.WRAP_NONE);
 
         rectangle2.setFillColor(3333);
         rectangle2.setLineStyle(9);
         rectangle2.setLineStyleColor(4444);
         rectangle2.setNoFill(false);
         rectangle2.setLineWidth(77);
         rectangle2.getAnchor().setDx1(2);
         rectangle2.getAnchor().setDx2(3);
         rectangle2.getAnchor().setDy1(4);
         rectangle2.getAnchor().setDy2(5);
         rectangle.setWrapText(HSSFSimpleShape.WRAP_BY_POINTS);
         rectangle2.setString(new HSSFRichTextString("test22"));
 
         wb = HSSFTestDataSamples.writeOutAndReadBack(wb);
         sheet = wb.getSheetAt(0);
         drawing = sheet.getDrawingPatriarch();
         assertEquals(1, drawing.getChildren().size());
         rectangle2 = (HSSFSimpleShape) drawing.getChildren().get(0);
         assertEquals(HSSFSimpleShape.OBJECT_TYPE_RECTANGLE, rectangle2.getShapeType());
         assertEquals(rectangle.getWrapText(), HSSFSimpleShape.WRAP_BY_POINTS);
         assertEquals(77, rectangle2.getLineWidth());
         assertEquals(9, rectangle2.getLineStyle());
         assertEquals(rectangle2.getLineStyleColor(), 4444);
         assertEquals(rectangle2.getFillColor(), 3333);
         assertEquals(rectangle2.getAnchor().getDx1(), 2);
         assertEquals(rectangle2.getAnchor().getDx2(), 3);
         assertEquals(rectangle2.getAnchor().getDy1(), 4);
         assertEquals(rectangle2.getAnchor().getDy2(), 5);
         assertEquals(rectangle2.isNoFill(), false);
         assertEquals(rectangle2.getString().getString(), "test22");
 
         HSSFSimpleShape rect3 = drawing.createSimpleShape(new HSSFClientAnchor());
         rect3.setShapeType(HSSFSimpleShape.OBJECT_TYPE_RECTANGLE);
         wb = HSSFTestDataSamples.writeOutAndReadBack(wb);
 
         drawing = wb.getSheetAt(0).getDrawingPatriarch();
         assertEquals(drawing.getChildren().size(), 2);
     }
 
     public void testReadExistingImage() {
         HSSFWorkbook wb = HSSFTestDataSamples.openSampleWorkbook("drawings.xls");
         HSSFSheet sheet = wb.getSheet("pictures");
         HSSFPatriarch drawing = sheet.getDrawingPatriarch();
         assertEquals(1, drawing.getChildren().size());
         HSSFPicture picture = (HSSFPicture) drawing.getChildren().get(0);
 
         assertEquals(picture.getPictureIndex(), 2);
         assertEquals(picture.getLineStyleColor(), HSSFShape.LINESTYLE__COLOR_DEFAULT);
         assertEquals(picture.getFillColor(), 0x5DC943);
         assertEquals(picture.getLineWidth(), HSSFShape.LINEWIDTH_DEFAULT);
         assertEquals(picture.getLineStyle(), HSSFShape.LINESTYLE_DEFAULT);
         assertEquals(picture.isNoFill(), false);
 
         picture.setPictureIndex(2);
         assertEquals(picture.getPictureIndex(), 2);
     }
 
 
     /* assert shape properties when reading shapes from a existing workbook */
     public void testReadExistingRectangle() {
         HSSFWorkbook wb = HSSFTestDataSamples.openSampleWorkbook("drawings.xls");
         HSSFSheet sheet = wb.getSheet("rectangles");
         HSSFPatriarch drawing = sheet.getDrawingPatriarch();
         assertEquals(1, drawing.getChildren().size());
 
         HSSFSimpleShape shape = (HSSFSimpleShape) drawing.getChildren().get(0);
         assertEquals(shape.isNoFill(), false);
         assertEquals(shape.getLineStyle(), HSSFShape.LINESTYLE_DASHDOTGEL);
         assertEquals(shape.getLineStyleColor(), 0x616161);
         assertEquals(HexDump.toHex(shape.getFillColor()), shape.getFillColor(), 0x2CE03D);
         assertEquals(shape.getLineWidth(), HSSFShape.LINEWIDTH_ONE_PT * 2);
         assertEquals(shape.getString().getString(), "POItest");
         assertEquals(shape.getRotationDegree(), 27);
     }
 
     public void testShapeIds() {
         HSSFWorkbook wb = new HSSFWorkbook();
         HSSFSheet sheet1 = wb.createSheet();
         HSSFPatriarch patriarch1 = sheet1.createDrawingPatriarch();
         for (int i = 0; i < 2; i++) {
             patriarch1.createSimpleShape(new HSSFClientAnchor());
         }
 
         wb = HSSFTestDataSamples.writeOutAndReadBack(wb);
         sheet1 = wb.getSheetAt(0);
         patriarch1 = sheet1.getDrawingPatriarch();
 
         EscherAggregate agg1 = HSSFTestHelper.getEscherAggregate(patriarch1);
         // last shape ID cached in EscherDgRecord
         EscherDgRecord dg1 =
                 agg1.getEscherContainer().getChildById(EscherDgRecord.RECORD_ID);
         assertEquals(1026, dg1.getLastMSOSPID());
 
         // iterate over shapes and check shapeId
         EscherContainerRecord spgrContainer =
                 agg1.getEscherContainer().getChildContainers().get(0);
         // root spContainer + 2 spContainers for shapes
         assertEquals(3, spgrContainer.getChildRecords().size());
 
         EscherSpRecord sp0 =
                 ((EscherContainerRecord) spgrContainer.getChild(0)).getChildById(EscherSpRecord.RECORD_ID);
         assertEquals(1024, sp0.getShapeId());
 
         EscherSpRecord sp1 =
                 ((EscherContainerRecord) spgrContainer.getChild(1)).getChildById(EscherSpRecord.RECORD_ID);
         assertEquals(1025, sp1.getShapeId());
 
         EscherSpRecord sp2 =
                 ((EscherContainerRecord) spgrContainer.getChild(2)).getChildById(EscherSpRecord.RECORD_ID);
         assertEquals(1026, sp2.getShapeId());
     }
 
     /**
      * Test get new id for shapes from existing file
      * File already have for 1 shape on each sheet, because document must contain EscherDgRecord for each sheet
      */
     public void testAllocateNewIds() {
         HSSFWorkbook wb = HSSFTestDataSamples.openSampleWorkbook("empty.xls");
         HSSFSheet sheet = wb.getSheetAt(0);
         HSSFPatriarch patriarch = sheet.getDrawingPatriarch();
 
         /**
          * 2048 - main SpContainer id
          * 2049 - existing shape id
          */
         assertEquals(HSSFTestHelper.allocateNewShapeId(patriarch), 2050);
         assertEquals(HSSFTestHelper.allocateNewShapeId(patriarch), 2051);
         assertEquals(HSSFTestHelper.allocateNewShapeId(patriarch), 2052);
 
         sheet = wb.getSheetAt(1);
         patriarch = sheet.getDrawingPatriarch();
 
         /**
          * 3072 - main SpContainer id
          * 3073 - existing shape id
          */
         assertEquals(HSSFTestHelper.allocateNewShapeId(patriarch), 3074);
         assertEquals(HSSFTestHelper.allocateNewShapeId(patriarch), 3075);
         assertEquals(HSSFTestHelper.allocateNewShapeId(patriarch), 3076);
 
 
         sheet = wb.getSheetAt(2);
         patriarch = sheet.getDrawingPatriarch();
 
         assertEquals(HSSFTestHelper.allocateNewShapeId(patriarch), 1026);
         assertEquals(HSSFTestHelper.allocateNewShapeId(patriarch), 1027);
         assertEquals(HSSFTestHelper.allocateNewShapeId(patriarch), 1028);
     }
 
     public void testOpt() throws Exception {
         HSSFWorkbook wb = new HSSFWorkbook();
 
         // create a sheet with a text box
         HSSFSheet sheet = wb.createSheet();
         HSSFPatriarch patriarch = sheet.createDrawingPatriarch();
 
         HSSFTextbox textbox = patriarch.createTextbox(new HSSFClientAnchor());
         EscherOptRecord opt1 = HSSFTestHelper.getOptRecord(textbox);
         EscherOptRecord opt2 = HSSFTestHelper.getEscherContainer(textbox).getChildById(EscherOptRecord.RECORD_ID);
         assertSame(opt1, opt2);
     }
     
     public void testCorrectOrderInOptRecord(){
         HSSFWorkbook wb = new HSSFWorkbook();
         HSSFSheet sheet = wb.createSheet();
         HSSFPatriarch patriarch = sheet.createDrawingPatriarch();
 
         HSSFTextbox textbox = patriarch.createTextbox(new HSSFClientAnchor());
         EscherOptRecord opt = HSSFTestHelper.getOptRecord(textbox);    
         
         String opt1Str = opt.toXml();
 
         textbox.setFillColor(textbox.getFillColor());
        assertEquals(opt1Str, HSSFTestHelper.getEscherContainer(textbox).getChildById(EscherOptRecord.RECORD_ID).toXml());
         textbox.setLineStyle(textbox.getLineStyle());
        assertEquals(opt1Str, HSSFTestHelper.getEscherContainer(textbox).getChildById(EscherOptRecord.RECORD_ID).toXml());
         textbox.setLineWidth(textbox.getLineWidth());
        assertEquals(opt1Str, HSSFTestHelper.getEscherContainer(textbox).getChildById(EscherOptRecord.RECORD_ID).toXml());
         textbox.setLineStyleColor(textbox.getLineStyleColor());
        assertEquals(opt1Str, HSSFTestHelper.getEscherContainer(textbox).getChildById(EscherOptRecord.RECORD_ID).toXml());
     }
 
     public void testDgRecordNumShapes(){
         HSSFWorkbook wb = new HSSFWorkbook();
         HSSFSheet sheet = wb.createSheet();
         HSSFPatriarch patriarch = sheet.createDrawingPatriarch();
 
         EscherAggregate aggregate = HSSFTestHelper.getEscherAggregate(patriarch);
         EscherDgRecord dgRecord = (EscherDgRecord) aggregate.getEscherRecord(0).getChild(0);
         assertEquals(dgRecord.getNumShapes(), 1);
     }
 
     public void testTextForSimpleShape(){
         HSSFWorkbook wb = new HSSFWorkbook();
         HSSFSheet sheet = wb.createSheet();
         HSSFPatriarch patriarch = sheet.createDrawingPatriarch();
 
         HSSFSimpleShape shape = patriarch.createSimpleShape(new HSSFClientAnchor());
         shape.setShapeType(HSSFSimpleShape.OBJECT_TYPE_RECTANGLE);
 
         EscherAggregate agg = HSSFTestHelper.getEscherAggregate(patriarch);
         assertEquals(agg.getShapeToObjMapping().size(), 2);
 
         wb = HSSFTestDataSamples.writeOutAndReadBack(wb);
         sheet = wb.getSheetAt(0);
         patriarch = sheet.getDrawingPatriarch();
 
         shape = (HSSFSimpleShape) patriarch.getChildren().get(0);
 
         agg = HSSFTestHelper.getEscherAggregate(patriarch);
         assertEquals(agg.getShapeToObjMapping().size(), 2);
 
         shape.setString(new HSSFRichTextString("string1"));
         assertEquals(shape.getString().getString(), "string1");
 
         assertNotNull(HSSFTestHelper.getEscherContainer(shape).getChildById(EscherTextboxRecord.RECORD_ID));
         assertEquals(agg.getShapeToObjMapping().size(), 2);
 
         wb = HSSFTestDataSamples.writeOutAndReadBack(wb);
 
         wb = HSSFTestDataSamples.writeOutAndReadBack(wb);
         sheet = wb.getSheetAt(0);
         patriarch = sheet.getDrawingPatriarch();
 
         shape = (HSSFSimpleShape) patriarch.getChildren().get(0);
 
         assertNotNull(HSSFTestHelper.getTextObjRecord(shape));
         assertEquals(shape.getString().getString(), "string1");
         assertNotNull(HSSFTestHelper.getEscherContainer(shape).getChildById(EscherTextboxRecord.RECORD_ID));
         assertEquals(agg.getShapeToObjMapping().size(), 2);
     }
 
     public void testComboboxRecords(){
         HSSFWorkbook wb = new HSSFWorkbook();
         HSSFSheet sheet = wb.createSheet();
         HSSFPatriarch patriarch = sheet.createDrawingPatriarch();
 
         HSSFCombobox combobox = new HSSFCombobox(null, new HSSFClientAnchor());
         HSSFTestHelper.setShapeId(combobox, 1024);
         ComboboxShape comboboxShape = new ComboboxShape(combobox, 1024);
 
         assertTrue(Arrays.equals(comboboxShape.getSpContainer().serialize(), HSSFTestHelper.getEscherContainer(combobox).serialize()));
         assertTrue(Arrays.equals(comboboxShape.getObjRecord().serialize(), HSSFTestHelper.getObjRecord(combobox).serialize()));
     }
 
     public void testRemoveShapes(){
         HSSFWorkbook wb = new HSSFWorkbook();
         HSSFSheet sheet = wb.createSheet();
         HSSFPatriarch patriarch = sheet.createDrawingPatriarch();
 
         HSSFSimpleShape rectangle = patriarch.createSimpleShape(new HSSFClientAnchor());
         rectangle.setShapeType(HSSFSimpleShape.OBJECT_TYPE_RECTANGLE);
 
         int idx = wb.addPicture(new byte[]{1,2,3}, Workbook.PICTURE_TYPE_JPEG);
         patriarch.createPicture(new HSSFClientAnchor(), idx);
 
         patriarch.createCellComment(new HSSFClientAnchor());
 
         HSSFPolygon polygon = patriarch.createPolygon(new HSSFClientAnchor());
         polygon.setPoints(new int[]{1,2}, new int[]{2,3});
 
         patriarch.createTextbox(new HSSFClientAnchor());
 
         HSSFShapeGroup group = patriarch.createGroup(new HSSFClientAnchor());
         group.createTextbox(new HSSFChildAnchor());
         group.createPicture(new HSSFChildAnchor(), idx);
 
         assertEquals(patriarch.getChildren().size(), 6);
         assertEquals(group.getChildren().size(), 2);
 
         assertEquals(HSSFTestHelper.getEscherAggregate(patriarch).getShapeToObjMapping().size(), 12);
         assertEquals(HSSFTestHelper.getEscherAggregate(patriarch).getTailRecords().size(), 1);
 
         wb = HSSFTestDataSamples.writeOutAndReadBack(wb);
         sheet = wb.getSheetAt(0);
         patriarch = sheet.getDrawingPatriarch();
 
         assertEquals(HSSFTestHelper.getEscherAggregate(patriarch).getShapeToObjMapping().size(), 12);
         assertEquals(HSSFTestHelper.getEscherAggregate(patriarch).getTailRecords().size(), 1);
 
         assertEquals(patriarch.getChildren().size(), 6);
 
         group = (HSSFShapeGroup) patriarch.getChildren().get(5);
         group.removeShape(group.getChildren().get(0));
 
         assertEquals(HSSFTestHelper.getEscherAggregate(patriarch).getShapeToObjMapping().size(), 10);
         assertEquals(HSSFTestHelper.getEscherAggregate(patriarch).getTailRecords().size(), 1);
 
         wb = HSSFTestDataSamples.writeOutAndReadBack(wb);
         sheet = wb.getSheetAt(0);
         patriarch = sheet.getDrawingPatriarch();
 
         assertEquals(HSSFTestHelper.getEscherAggregate(patriarch).getShapeToObjMapping().size(), 10);
         assertEquals(HSSFTestHelper.getEscherAggregate(patriarch).getTailRecords().size(), 1);
 
         group = (HSSFShapeGroup) patriarch.getChildren().get(5);
         patriarch.removeShape(group);
 
         assertEquals(HSSFTestHelper.getEscherAggregate(patriarch).getShapeToObjMapping().size(), 8);
         assertEquals(HSSFTestHelper.getEscherAggregate(patriarch).getTailRecords().size(), 1);
 
         wb = HSSFTestDataSamples.writeOutAndReadBack(wb);
         sheet = wb.getSheetAt(0);
         patriarch = sheet.getDrawingPatriarch();
 
         assertEquals(HSSFTestHelper.getEscherAggregate(patriarch).getShapeToObjMapping().size(), 8);
         assertEquals(HSSFTestHelper.getEscherAggregate(patriarch).getTailRecords().size(), 1);
         assertEquals(patriarch.getChildren().size(), 5);
 
         HSSFShape shape = patriarch.getChildren().get(0);
         patriarch.removeShape(shape);
 
         assertEquals(HSSFTestHelper.getEscherAggregate(patriarch).getShapeToObjMapping().size(), 6);
         assertEquals(HSSFTestHelper.getEscherAggregate(patriarch).getTailRecords().size(), 1);
         assertEquals(patriarch.getChildren().size(), 4);
 
         wb = HSSFTestDataSamples.writeOutAndReadBack(wb);
         sheet = wb.getSheetAt(0);
         patriarch = sheet.getDrawingPatriarch();
 
         assertEquals(HSSFTestHelper.getEscherAggregate(patriarch).getShapeToObjMapping().size(), 6);
         assertEquals(HSSFTestHelper.getEscherAggregate(patriarch).getTailRecords().size(), 1);
         assertEquals(patriarch.getChildren().size(), 4);
 
         HSSFPicture picture = (HSSFPicture) patriarch.getChildren().get(0);
         patriarch.removeShape(picture);
 
         assertEquals(HSSFTestHelper.getEscherAggregate(patriarch).getShapeToObjMapping().size(), 5);
         assertEquals(HSSFTestHelper.getEscherAggregate(patriarch).getTailRecords().size(), 1);
         assertEquals(patriarch.getChildren().size(), 3);
 
         wb = HSSFTestDataSamples.writeOutAndReadBack(wb);
         sheet = wb.getSheetAt(0);
         patriarch = sheet.getDrawingPatriarch();
 
         assertEquals(HSSFTestHelper.getEscherAggregate(patriarch).getShapeToObjMapping().size(), 5);
         assertEquals(HSSFTestHelper.getEscherAggregate(patriarch).getTailRecords().size(), 1);
         assertEquals(patriarch.getChildren().size(), 3);
 
         HSSFComment comment = (HSSFComment) patriarch.getChildren().get(0);
         patriarch.removeShape(comment);
 
         assertEquals(HSSFTestHelper.getEscherAggregate(patriarch).getShapeToObjMapping().size(), 3);
         assertEquals(HSSFTestHelper.getEscherAggregate(patriarch).getTailRecords().size(), 0);
         assertEquals(patriarch.getChildren().size(), 2);
 
         wb = HSSFTestDataSamples.writeOutAndReadBack(wb);
         sheet = wb.getSheetAt(0);
         patriarch = sheet.getDrawingPatriarch();
 
         assertEquals(HSSFTestHelper.getEscherAggregate(patriarch).getShapeToObjMapping().size(), 3);
         assertEquals(HSSFTestHelper.getEscherAggregate(patriarch).getTailRecords().size(), 0);
         assertEquals(patriarch.getChildren().size(), 2);
 
         polygon = (HSSFPolygon) patriarch.getChildren().get(0);
         patriarch.removeShape(polygon);
 
         assertEquals(HSSFTestHelper.getEscherAggregate(patriarch).getShapeToObjMapping().size(), 2);
         assertEquals(HSSFTestHelper.getEscherAggregate(patriarch).getTailRecords().size(), 0);
         assertEquals(patriarch.getChildren().size(), 1);
 
         wb = HSSFTestDataSamples.writeOutAndReadBack(wb);
         sheet = wb.getSheetAt(0);
         patriarch = sheet.getDrawingPatriarch();
 
         assertEquals(HSSFTestHelper.getEscherAggregate(patriarch).getShapeToObjMapping().size(), 2);
         assertEquals(HSSFTestHelper.getEscherAggregate(patriarch).getTailRecords().size(), 0);
         assertEquals(patriarch.getChildren().size(), 1);
 
         HSSFTextbox textbox = (HSSFTextbox) patriarch.getChildren().get(0);
         patriarch.removeShape(textbox);
 
         assertEquals(HSSFTestHelper.getEscherAggregate(patriarch).getShapeToObjMapping().size(), 0);
         assertEquals(HSSFTestHelper.getEscherAggregate(patriarch).getTailRecords().size(), 0);
         assertEquals(patriarch.getChildren().size(), 0);
 
         wb = HSSFTestDataSamples.writeOutAndReadBack(wb);
         sheet = wb.getSheetAt(0);
         patriarch = sheet.getDrawingPatriarch();
 
         assertEquals(HSSFTestHelper.getEscherAggregate(patriarch).getShapeToObjMapping().size(), 0);
         assertEquals(HSSFTestHelper.getEscherAggregate(patriarch).getTailRecords().size(), 0);
         assertEquals(patriarch.getChildren().size(), 0);
     }
 
     public void testShapeFlip(){
         HSSFWorkbook wb = new HSSFWorkbook();
         HSSFSheet sheet = wb.createSheet();
         HSSFPatriarch patriarch = sheet.createDrawingPatriarch();
 
         HSSFSimpleShape rectangle = patriarch.createSimpleShape(new HSSFClientAnchor());
         rectangle.setShapeType(HSSFSimpleShape.OBJECT_TYPE_RECTANGLE);
 
         assertEquals(rectangle.isFlipVertical(), false);
         assertEquals(rectangle.isFlipHorizontal(), false);
 
         rectangle.setFlipVertical(true);
         assertEquals(rectangle.isFlipVertical(), true);
         rectangle.setFlipHorizontal(true);
         assertEquals(rectangle.isFlipHorizontal(), true);
 
         wb = HSSFTestDataSamples.writeOutAndReadBack(wb);
         sheet = wb.getSheetAt(0);
         patriarch = sheet.getDrawingPatriarch();
 
         rectangle = (HSSFSimpleShape) patriarch.getChildren().get(0);
 
         assertEquals(rectangle.isFlipHorizontal(), true);
         rectangle.setFlipHorizontal(false);
         assertEquals(rectangle.isFlipHorizontal(), false);
 
         assertEquals(rectangle.isFlipVertical(), true);
         rectangle.setFlipVertical(false);
         assertEquals(rectangle.isFlipVertical(), false);
 
         wb = HSSFTestDataSamples.writeOutAndReadBack(wb);
         sheet = wb.getSheetAt(0);
         patriarch = sheet.getDrawingPatriarch();
 
         rectangle = (HSSFSimpleShape) patriarch.getChildren().get(0);
 
         assertEquals(rectangle.isFlipVertical(), false);
         assertEquals(rectangle.isFlipHorizontal(), false);
     }
 
     public void testRotation() {
         HSSFWorkbook wb = new HSSFWorkbook();
         HSSFSheet sheet = wb.createSheet();
         HSSFPatriarch patriarch = sheet.createDrawingPatriarch();
 
         HSSFSimpleShape rectangle = patriarch.createSimpleShape(new HSSFClientAnchor(0,0,100,100, (short) 0,0,(short)5,5));
         rectangle.setShapeType(HSSFSimpleShape.OBJECT_TYPE_RECTANGLE);
 
         assertEquals(rectangle.getRotationDegree(), 0);
         rectangle.setRotationDegree((short) 45);
         assertEquals(rectangle.getRotationDegree(), 45);
         rectangle.setFlipHorizontal(true);
 
         wb = HSSFTestDataSamples.writeOutAndReadBack(wb);
         sheet = wb.getSheetAt(0);
         patriarch = sheet.getDrawingPatriarch();
         rectangle = (HSSFSimpleShape) patriarch.getChildren().get(0);
         assertEquals(rectangle.getRotationDegree(), 45);
         rectangle.setRotationDegree((short) 30);
         assertEquals(rectangle.getRotationDegree(), 30);
 
         patriarch.setCoordinates(0, 0, 10, 10);
         rectangle.setString(new HSSFRichTextString("1234"));
     }
 
     public void testShapeContainerImplementsIterable(){
         HSSFWorkbook wb = new HSSFWorkbook();
         HSSFSheet sheet = wb.createSheet();
         HSSFPatriarch patriarch = sheet.createDrawingPatriarch();
 
         patriarch.createSimpleShape(new HSSFClientAnchor());
         patriarch.createSimpleShape(new HSSFClientAnchor());
 
         int i=2;
 
         for (HSSFShape shape: patriarch){
             i--;
         }
         assertEquals(i, 0);
     }
 
     public void testClearShapesForPatriarch(){
         HSSFWorkbook wb = new HSSFWorkbook();
         HSSFSheet sheet = wb.createSheet();
         HSSFPatriarch patriarch = sheet.createDrawingPatriarch();
 
         patriarch.createSimpleShape(new HSSFClientAnchor());
         patriarch.createSimpleShape(new HSSFClientAnchor());
         patriarch.createCellComment(new HSSFClientAnchor());
 
         EscherAggregate agg = HSSFTestHelper.getEscherAggregate(patriarch);
 
         assertEquals(agg.getShapeToObjMapping().size(), 6);
         assertEquals(agg.getTailRecords().size(), 1);
         assertEquals(patriarch.getChildren().size(), 3);
 
         patriarch.clear();
 
         assertEquals(agg.getShapeToObjMapping().size(), 0);
         assertEquals(agg.getTailRecords().size(), 0);
         assertEquals(patriarch.getChildren().size(), 0);
 
         wb = HSSFTestDataSamples.writeOutAndReadBack(wb);
         sheet = wb.getSheetAt(0);
         patriarch = sheet.getDrawingPatriarch();
 
         assertEquals(agg.getShapeToObjMapping().size(), 0);
         assertEquals(agg.getTailRecords().size(), 0);
         assertEquals(patriarch.getChildren().size(), 0);
     }
 }
