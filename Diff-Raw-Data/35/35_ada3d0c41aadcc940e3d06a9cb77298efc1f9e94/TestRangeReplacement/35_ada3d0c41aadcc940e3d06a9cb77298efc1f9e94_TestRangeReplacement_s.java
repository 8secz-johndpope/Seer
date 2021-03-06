 
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
 
 package org.apache.poi.hwpf.usermodel;
 
 import java.io.ByteArrayOutputStream;
 import java.io.FileInputStream;
 import java.util.List;
 
 import org.apache.poi.hwpf.HWPFDocument;
 import org.apache.poi.hwpf.model.PicturesTable;
 import org.apache.poi.hwpf.usermodel.Picture;
 
 import junit.framework.TestCase;
 
 /**
  *	Test to see if Range.replaceText() works even if the Range contains a
  *	CharacterRun that uses Unicode characters.
  */
public class TestRangeReplacement extends TestCase {
 
 	// u201c and u201d are "smart-quotes"
 	private String originalText =
 		"It is used to confirm that text replacement works even if Unicode characters (such as \u201c\u2014\u201d (U+2014), \u201c\u2e8e\u201d (U+2E8E), or \u201c\u2714\u201d (U+2714)) are present.  Everybody should be thankful to the ${organization} and all the POI contributors for their assistance in this matter.\r";
 	private String searchText = "${organization}";
 	private String replacementText = "Apache Software Foundation";
 	private String expectedText2 =
 		"It is used to confirm that text replacement works even if Unicode characters (such as \u201c\u2014\u201d (U+2014), \u201c\u2e8e\u201d (U+2E8E), or \u201c\u2714\u201d (U+2714)) are present.  Everybody should be thankful to the Apache Software Foundation and all the POI contributors for their assistance in this matter.\r";
 	private String expectedText3 = "Thank you, Apache Software Foundation!\r";
 
 	private String illustrativeDocFile;
 
 	protected void setUp() throws Exception {
 
 		String dirname = System.getProperty("HWPF.testdata.path");
 
 		illustrativeDocFile = dirname + "/testRangeReplacement.doc";
 	}
 
 	/**
 	 * Test just opening the files
 	 */
 	public void testOpen() throws Exception {
 
 		HWPFDocument docA = new HWPFDocument(new FileInputStream(illustrativeDocFile));
 	}
 
 	/**
 	 * Test (more "confirm" than test) that we have the general structure that we expect to have.
 	 */
 	public void testDocStructure() throws Exception {
 
 		HWPFDocument daDoc = new HWPFDocument(new FileInputStream(illustrativeDocFile));
 
 		Range range = daDoc.getRange();
 
 		assertEquals(1, range.numSections());
 		Section section = range.getSection(0);
 
 		assertEquals(5, section.numParagraphs());
 		Paragraph para = section.getParagraph(2);
 
 		assertEquals(5, para.numCharacterRuns());
 		String text = para.getCharacterRun(0).text() + para.getCharacterRun(1).text() +
 			para.getCharacterRun(2).text() + para.getCharacterRun(3).text() + para.getCharacterRun(4).text();
 
 		assertEquals(originalText, text);
 	}
 
 	/**
 	 * Test that we can replace text in our Range with Unicode text.
 	 */
 	public void testRangeReplacementOne() throws Exception {
 
 		HWPFDocument daDoc = new HWPFDocument(new FileInputStream(illustrativeDocFile));
 
 		Range range = daDoc.getRange();
 		assertEquals(1, range.numSections());
 
 		Section section = range.getSection(0);
 		assertEquals(5, section.numParagraphs());
 
 		Paragraph para = section.getParagraph(2);
 
 		String text = para.text();
 		assertEquals(originalText, text);
 
 		int offset = text.indexOf(searchText);
 		assertEquals(181, offset);
 
 		para.replaceText(searchText, replacementText, offset);
 
 		assertEquals(1, range.numSections());
 		section = range.getSection(0);
 
 		assertEquals(4, section.numParagraphs());
 		para = section.getParagraph(2);
 
 		text = para.text();
 		assertEquals(expectedText2, text);
 	}
 
 	/**
 	 * Test that we can replace text in our Range with Unicode text.
 	 */
 	public void testRangeReplacementAll() throws Exception {
 
 		HWPFDocument daDoc = new HWPFDocument(new FileInputStream(illustrativeDocFile));
 
 		Range range = daDoc.getRange();
 		assertEquals(1, range.numSections());
 
 		Section section = range.getSection(0);
 		assertEquals(5, section.numParagraphs());
 
 		Paragraph para = section.getParagraph(2);
 
 		String text = para.text();
 		assertEquals(originalText, text);
 
 		range.replaceText(searchText, replacementText);
 
 		assertEquals(1, range.numSections());
 		section = range.getSection(0);
 		assertEquals(5, section.numParagraphs());
 
 		para = section.getParagraph(2);
 		text = para.text();
 		assertEquals(expectedText2, text);
 
 		para = section.getParagraph(3);
 		text = para.text();
 		assertEquals(expectedText3, text);
 	}
 }
