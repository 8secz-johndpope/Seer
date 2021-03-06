 /*
  *    GeoTools - OpenSource mapping toolkit
  *    http://geotools.org
  *    (C) 2008, Geotools Project Managment Committee (PMC)
  *    (C) 2008, Geomatys
  *
  *    This library is free software; you can redistribute it and/or
  *    modify it under the terms of the GNU Lesser General Public
  *    License as published by the Free Software Foundation; either
  *    version 2.1 of the License, or (at your option) any later version.
  *
  *    This library is distributed in the hope that it will be useful,
  *    but WITHOUT ANY WARRANTY; without even the implied warranty of
  *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  *    Lesser General Public License for more details.
  */
 package org.geotools.image.io.mosaic;
 
 import org.junit.*;
 import static org.junit.Assert.*;
 
 
 /**
  * Tests {@link FilenameFormatter}.
  *
  * @source $URL$
  * @version $Id$
  * @author Martin Desruisseaux
  */
 public class FilenameFormatterTest {
     /**
      * Tests the creation of a pattern.
      */
     @Test
     public void testGuessPattern() {
         final FilenameFormatter formatter = new FilenameFormatter();
        assertNull(formatter.guessPattern(0, 0, 0, "L1_B1.png"));
         assertEquals("L{level:1}_{column:1}{row:1}.png", formatter.guessPattern(0,  0,  0, "L1_A1.png"));
         assertEquals("L{level:2}_{column:1}{row:1}.png", formatter.guessPattern(0,  0,  0, "L01_A1.png"));
         assertEquals("L{level:1}_{column:2}{row:1}.png", formatter.guessPattern(0,  0,  0, "L1_AA1.png"));
         assertEquals("L{level:1}_{column:1}{row:2}.png", formatter.guessPattern(0,  0,  0, "L1_A01.png"));
         assertEquals("L{level:2}_{column:2}{row:2}.png", formatter.guessPattern(0,  0,  0, "L01_AA01.png"));
         assertEquals("L{level:2}_{column:2}{row:2}.png", formatter.guessPattern(12, 3, 14, "L13_AD15.png"));
     }
 
     /**
      * Tests the application of a pattern.
      */
     @Test
     public void testApplyPattern() {
         final FilenameFormatter formatter = new FilenameFormatter();
         formatter.applyPattern("L{level:2}_{column:2}{row:2}.png");
         assertEquals("L{level:2}_{column:2}{row:2}.png", formatter.toString());
         assertEquals("L01_AA01.png", formatter.generateFilename(0,  0,  0));
         assertEquals("L13_AD15.png", formatter.generateFilename(12, 3, 14));
 
         formatter.applyPattern("L{level:1}_{column:2}{row:3}.png");
         assertEquals("L{level:1}_{column:2}{row:3}.png", formatter.toString());
         assertEquals("L1_AA001.png", formatter.generateFilename(0, 0,  0));
         assertEquals("L3_AD015.png", formatter.generateFilename(2, 3, 14));
     }
 }
