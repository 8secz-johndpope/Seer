 
 /* ====================================================================
    Copyright 2002-2004   Apache Software Foundation
 
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
 
        http://www.apache.org/licenses/LICENSE-2.0
 
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 ==================================================================== */
 
 package org.apache.poi.hwpf.usermodel;
 
 import java.util.ArrayList;
 
 public class Table
   extends Range
 {
   ArrayList _rows;
 
   Table(int startIdx, int endIdx, Range parent, int levelNum)
   {
     super(startIdx, endIdx, Range.TYPE_PARAGRAPH, parent);
     _rows = new ArrayList();
     int numParagraphs = numParagraphs();
 
     int rowStart = 0;
     int rowEnd = 0;
 
     while (rowEnd < numParagraphs)
     {
       Paragraph p = getParagraph(rowEnd);
       if (p.isTableRowEnd() && p.getTableLevel() == levelNum)
       {
        _rows.add(new TableRow(rowStart, rowEnd + 1, this, levelNum));
         rowStart = rowEnd;
       }
       rowEnd++;
     }
   }
 
   public int numRows()
   {
     return _rows.size();
   }
 
   public int type()
   {
     return TYPE_TABLE;
   }
 
   public TableRow getRow(int index)
   {
     return (TableRow)_rows.get(index);
   }
 }
