 /**
  * ***************************************************************************
  * Copyright (c) 2010 Qcadoo Limited
  * Project: Qcadoo Framework
  * Version: 1.2.0
  *
  * This file is part of Qcadoo.
  *
  * Qcadoo is free software; you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as published
  * by the Free Software Foundation; either version 3 of the License,
  * or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty
  * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  * See the GNU Affero General Public License for more details.
  *
  * You should have received a copy of the GNU Affero General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
  * ***************************************************************************
  */
 package com.qcadoo.report.internal;
 
 import static org.springframework.context.i18n.LocaleContextHolder.getLocale;
 
 import java.io.IOException;
 import java.net.MalformedURLException;
 import java.text.SimpleDateFormat;
 import java.util.Arrays;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Locale;
 import java.util.Map;
 
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.stereotype.Component;
 
 import com.lowagie.text.BadElementException;
 import com.lowagie.text.Chunk;
 import com.lowagie.text.Document;
 import com.lowagie.text.DocumentException;
 import com.lowagie.text.Element;
 import com.lowagie.text.Font;
 import com.lowagie.text.Image;
 import com.lowagie.text.Paragraph;
 import com.lowagie.text.Phrase;
 import com.lowagie.text.Rectangle;
 import com.lowagie.text.pdf.PdfPTable;
 import com.lowagie.text.pdf.draw.LineSeparator;
 import com.qcadoo.localization.api.TranslationService;
 import com.qcadoo.localization.api.utils.DateUtils;
 import com.qcadoo.report.api.ColorUtils;
 import com.qcadoo.report.api.FontUtils;
 import com.qcadoo.report.api.pdf.HeaderAlignment;
 import com.qcadoo.report.api.pdf.PdfHelper;
 import com.qcadoo.report.api.pdf.TableBorderEvent;
 
 @Component
 public final class PdfHelperImpl implements PdfHelper {
 
     private static final Logger LOG = LoggerFactory.getLogger(PdfHelperImpl.class);
 
     private static final Integer MINIMUM_ALLOWABLE_SIZE_COLUMN_IN_PIXEL = 63;
 
     @Autowired
     private TranslationService translationService;
 
     @Override
     public void addDocumentHeader(final Document document, final String name, final String documenTitle,
             final String documentAuthor, final Date date, final String username) throws DocumentException {
         SimpleDateFormat df = new SimpleDateFormat(DateUtils.L_DATE_TIME_FORMAT, getLocale());
         LineSeparator line = new LineSeparator(3, 100f, ColorUtils.getLineDarkColor(), Element.ALIGN_LEFT, 0);
         document.add(Chunk.NEWLINE);
         Paragraph title = new Paragraph(new Phrase(documenTitle, FontUtils.getDejavuBold19Light()));
         title.add(new Phrase(" " + name, FontUtils.getDejavuBold19Dark()));
         title.setSpacingAfter(7f);
         document.add(title);
         document.add(line);
         PdfPTable userAndDate = new PdfPTable(2);
         userAndDate.setWidthPercentage(100f);
         userAndDate.setHorizontalAlignment(Element.ALIGN_LEFT);
         userAndDate.getDefaultCell().setBorderWidth(0);
         Paragraph userParagraph = new Paragraph(new Phrase(documentAuthor, FontUtils.getDejavuRegular9Light()));
         userParagraph.add(new Phrase(" " + username, FontUtils.getDejavuRegular9Dark()));
         Paragraph dateParagraph = new Paragraph(df.format(date), FontUtils.getDejavuRegular9Light());
         userAndDate.addCell(userParagraph);
         userAndDate.getDefaultCell().setHorizontalAlignment(Element.ALIGN_RIGHT);
         userAndDate.addCell(dateParagraph);
         userAndDate.setSpacingAfter(14f);
         document.add(userAndDate);
     }
 
     @Override
     public void addMetaData(final Document document) {
         document.addSubject("Using iText");
         document.addKeywords("Java, PDF, iText");
         document.addAuthor("QCADOO");
         document.addCreator("QCADOO");
     }
 
     @Override
     public PdfPTable createPanelTable(final int column) {
         PdfPTable mainData = new PdfPTable(column);
         mainData.setWidthPercentage(100f);
         mainData.getDefaultCell().setBackgroundColor(ColorUtils.getBackgroundColor());
         mainData.getDefaultCell().setBorder(Rectangle.NO_BORDER);
         mainData.getDefaultCell().setVerticalAlignment(Element.ALIGN_MIDDLE);
         mainData.getDefaultCell().setPadding(8.0f);
         mainData.setTableEvent(new TableBorderEvent());
         return mainData;
     }
 
     @Override
     public void addTableCellAsTable(final PdfPTable table, final String label, final Object fieldValue, final Font headerFont,
             final Font valueFont, final int columns) {
         addTableCellAsTable(table, label, fieldValue, "-", headerFont, valueFont, columns);
     }
 
     @Override
     public void addTableCellAsTable(final PdfPTable table, final String label, final Object fieldValue, final String nullValue,
             final Font headerFont, final Font valueFont, final int columns) {
         PdfPTable cellTable = new PdfPTable(columns);
         cellTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);
         cellTable.addCell(new Phrase(label, headerFont));
         if (fieldValue == null) {
             cellTable.addCell(new Phrase(nullValue, valueFont));
         } else {
             cellTable.addCell(new Phrase(fieldValue.toString(), valueFont));
         }
         table.addCell(cellTable);
     }
 
     @Override
     public void addTableCellAsTwoColumnsTable(final PdfPTable table, final String label, final Object fieldValue) {
         addTableCellAsTable(table, label, fieldValue, FontUtils.getDejavuBold9Dark(), FontUtils.getDejavuBold9Dark(), 2);
     }
 
     @Override
     public void addTableCellAsOneColumnTable(final PdfPTable table, final String label, final Object fieldValue) {
         addTableCellAsTable(table, label, fieldValue, FontUtils.getDejavuBold10Dark(), FontUtils.getDejavuRegular10Dark(), 1);
     }
 
     @Override
     public void addImage(final Document document, final String fileName) {
         try {
             Image img = Image.getInstance(fileName);
             if (img.getWidth() > 515 || img.getHeight() > 370) {
                 img.scaleToFit(515, 370);
             }
 
             document.add(img);
             document.add(Chunk.NEWLINE);
         } catch (BadElementException e) {
             LOG.error(e.getMessage(), e);
         } catch (MalformedURLException e) {
             LOG.error(e.getMessage(), e);
         } catch (IOException e) {
             LOG.error(e.getMessage(), e);
         } catch (DocumentException e) {
             LOG.error(e.getMessage(), e);
         }
     }
 
     @Override
     public PdfPTable createTableWithHeader(final int numOfColumns, final List<String> header,
             final boolean lastColumnAligmentToLeft, final int[] columnWidths, final HeaderAlignment headerAlignment) {
         PdfPTable table = new PdfPTable(numOfColumns);
         try {
             table.setWidths(columnWidths);
         } catch (DocumentException e) {
             LOG.error(e.getMessage(), e);
         }
         return setTableProperties(header, lastColumnAligmentToLeft, table, headerAlignment);
     }
 
     @Override
     public PdfPTable createTableWithHeader(final int numOfColumns, final List<String> header,
             final boolean lastColumnAligmentToLeft, final HeaderAlignment headerAlignment) {
         PdfPTable table = new PdfPTable(numOfColumns);
         return setTableProperties(header, lastColumnAligmentToLeft, table, headerAlignment);
     }
 
     @Override
     public PdfPTable createTableWithHeader(final int numOfColumns, final List<String> header,
             final boolean lastColumnAligmentToLeft, final int[] columnWidths) {
         PdfPTable table = new PdfPTable(numOfColumns);
         try {
             table.setWidths(columnWidths);
         } catch (DocumentException e) {
             LOG.error(e.getMessage(), e);
         }
         return setTableProperties(header, lastColumnAligmentToLeft, table, HeaderAlignment.CENTER);
     }
 
     @Override
     public PdfPTable createTableWithHeader(final int numOfColumns, final List<String> header,
             final boolean lastColumnAligmentToLeft) {
         PdfPTable table = new PdfPTable(numOfColumns);
         return setTableProperties(header, lastColumnAligmentToLeft, table, HeaderAlignment.CENTER);
     }
 
     @Override
     public int getMaxSizeOfColumnsRows(final List<Integer> columnsListSize) {
         int size = 0;
         for (int columnSize : columnsListSize) {
             if (columnSize > size) {
                 size = columnSize;
             }
         }
         return size;
     }
 
     @Override
     public PdfPTable addDynamicHeaderTableCell(final PdfPTable headerTable, final Map<String, Object> column, final Locale locale) {
         if (column.keySet().size() == 0) {
             addTableCellAsOneColumnTable(headerTable, "", "");
         } else {
             Object key = column.keySet().iterator().next();
             addTableCellAsOneColumnTable(headerTable, translationService.translate(key.toString(), locale), column.get(key));
             column.remove(key);
         }
         return headerTable;
     }
 
     private PdfPTable setTableProperties(final List<String> header, final boolean lastColumnAligmentToLeft,
             final PdfPTable table, final HeaderAlignment aligment) {
         table.setWidthPercentage(100f);
         table.setHorizontalAlignment(Element.ALIGN_LEFT);
         table.setSpacingBefore(7.0f);
         table.getDefaultCell().setBackgroundColor(ColorUtils.getBackgroundColor());
         table.getDefaultCell().setBorderColor(ColorUtils.getLineDarkColor());
         table.getDefaultCell().setVerticalAlignment(Element.ALIGN_MIDDLE);
         table.getDefaultCell().setPadding(5.0f);
         table.getDefaultCell().disableBorderSide(Rectangle.RIGHT);
 
         if (HeaderAlignment.LEFT.equals(aligment)) {
             table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
         } else if (HeaderAlignment.CENTER.equals(aligment)) {
             table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
         } else if (HeaderAlignment.RIGHT.equals(aligment)) {
             table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_RIGHT);
         }
 
         int i = 0;
         for (String element : header) {
             i++;
             if (i == header.size() && lastColumnAligmentToLeft) {
                 table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_RIGHT);
             }
             if (i == header.size()) {
                 table.getDefaultCell().enableBorderSide(Rectangle.RIGHT);
             }
             table.addCell(new Phrase(element, FontUtils.getDejavuRegular9Dark()));
             if (i == 1) {
                 table.getDefaultCell().disableBorderSide(Rectangle.LEFT);
             }
             if (i == header.size() && lastColumnAligmentToLeft) {
                 table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
             }
         }
         table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
         table.getDefaultCell().setBackgroundColor(null);
         table.getDefaultCell().disableBorderSide(Rectangle.RIGHT);
         table.getDefaultCell().setBorderColor(ColorUtils.getLineLightColor());
         return table;
     }
 
     @Override
     public int[] getReportColumnWidths(Integer availableWidth, Map<String, Integer> fixedColumns, List<String> allColumns) {
         int[] reportColumnWidths = new int[allColumns.size()];
         Integer remainedAvailableWidth = availableWidth;
         Map<Integer, Integer> columnWithFixedWidth = new HashMap<Integer, Integer>();
         int i = 0;
         for (String entryColumn : allColumns) {
             for (Map.Entry<String, Integer> entryFixedColumn : fixedColumns.entrySet()) {
                 if (entryColumn.toLowerCase().contains(entryFixedColumn.getKey().toLowerCase())) {
                     remainedAvailableWidth = remainedAvailableWidth - entryFixedColumn.getValue();
                     columnWithFixedWidth.put(i, entryFixedColumn.getValue());
                 }
             }
             i++;
         }

         Integer columnWithoutFixedWidth = allColumns.size() - columnWithFixedWidth.size();
        if (allColumns.size() == columnWithFixedWidth.size() && remainedAvailableWidth >= 0) {
            for (Map.Entry<Integer, Integer> entry : columnWithFixedWidth.entrySet()) {
                reportColumnWidths[entry.getKey()] = entry.getValue();
            }
        } else if (remainedAvailableWidth >= 0
                && remainedAvailableWidth > columnWithoutFixedWidth * MINIMUM_ALLOWABLE_SIZE_COLUMN_IN_PIXEL) {
             Integer columnSize = remainedAvailableWidth / columnWithoutFixedWidth;
             Arrays.fill(reportColumnWidths, columnSize);
             for (Map.Entry<Integer, Integer> entry : columnWithFixedWidth.entrySet()) {
                 reportColumnWidths[entry.getKey()] = entry.getValue();
             }
         } else {
             Integer columnSize = availableWidth / allColumns.size();
             Arrays.fill(reportColumnWidths, columnSize);
         }
         return reportColumnWidths;
     }
 }
