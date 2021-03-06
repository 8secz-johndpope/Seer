 /**
  * Copyright (C) 2011 The XDocReport Team <xdocreport@googlegroups.com>
  *
  * All rights reserved.
  *
  * Permission is hereby granted, free  of charge, to any person obtaining
  * a  copy  of this  software  and  associated  documentation files  (the
  * "Software"), to  deal in  the Software without  restriction, including
  * without limitation  the rights to  use, copy, modify,  merge, publish,
  * distribute,  sublicense, and/or sell  copies of  the Software,  and to
  * permit persons to whom the Software  is furnished to do so, subject to
  * the following conditions:
  *
  * The  above  copyright  notice  and  this permission  notice  shall  be
  * included in all copies or substantial portions of the Software.
  *
  * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
  * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
  * MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
  * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
  * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
  * OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
  * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
  */
 package org.odftoolkit.odfdom.converter.pdf.internal;
 
 import java.awt.Color;
 import java.io.IOException;
 import java.io.OutputStream;
 import java.io.Writer;
 import java.util.ArrayList;
 import java.util.List;
 
 import org.odftoolkit.odfdom.converter.core.ElementVisitorConverter;
 import org.odftoolkit.odfdom.converter.core.ODFConverterException;
 import org.odftoolkit.odfdom.converter.core.utils.ODFUtils;
 import org.odftoolkit.odfdom.converter.pdf.PdfOptions;
 import org.odftoolkit.odfdom.converter.pdf.internal.stylable.IStylableContainer;
 import org.odftoolkit.odfdom.converter.pdf.internal.stylable.IStylableElement;
 import org.odftoolkit.odfdom.converter.pdf.internal.stylable.StylableAnchor;
 import org.odftoolkit.odfdom.converter.pdf.internal.stylable.StylableChunk;
 import org.odftoolkit.odfdom.converter.pdf.internal.stylable.StylableDocument;
 import org.odftoolkit.odfdom.converter.pdf.internal.stylable.StylableDocumentSection;
 import org.odftoolkit.odfdom.converter.pdf.internal.stylable.StylableHeaderFooter;
 import org.odftoolkit.odfdom.converter.pdf.internal.stylable.StylableHeading;
 import org.odftoolkit.odfdom.converter.pdf.internal.stylable.StylableImage;
 import org.odftoolkit.odfdom.converter.pdf.internal.stylable.StylableList;
 import org.odftoolkit.odfdom.converter.pdf.internal.stylable.StylableListItem;
 import org.odftoolkit.odfdom.converter.pdf.internal.stylable.StylableMasterPage;
 import org.odftoolkit.odfdom.converter.pdf.internal.stylable.StylableParagraph;
 import org.odftoolkit.odfdom.converter.pdf.internal.stylable.StylablePhrase;
 import org.odftoolkit.odfdom.converter.pdf.internal.stylable.StylableTab;
 import org.odftoolkit.odfdom.converter.pdf.internal.stylable.StylableTable;
 import org.odftoolkit.odfdom.converter.pdf.internal.stylable.StylableTableCell;
 import org.odftoolkit.odfdom.converter.pdf.internal.styles.Style;
 import org.odftoolkit.odfdom.converter.pdf.internal.styles.StyleTextProperties;
 import org.odftoolkit.odfdom.doc.OdfDocument;
 import org.odftoolkit.odfdom.dom.element.OdfStylableElement;
 import org.odftoolkit.odfdom.dom.element.draw.DrawFrameElement;
 import org.odftoolkit.odfdom.dom.element.draw.DrawImageElement;
 import org.odftoolkit.odfdom.dom.element.office.OfficeTextElement;
 import org.odftoolkit.odfdom.dom.element.style.StyleFooterElement;
 import org.odftoolkit.odfdom.dom.element.style.StyleFooterLeftElement;
 import org.odftoolkit.odfdom.dom.element.style.StyleHeaderElement;
 import org.odftoolkit.odfdom.dom.element.style.StyleHeaderLeftElement;
 import org.odftoolkit.odfdom.dom.element.style.StyleMasterPageElement;
import org.odftoolkit.odfdom.dom.element.svg.SvgDescElement;
 import org.odftoolkit.odfdom.dom.element.table.TableTableCellElement;
 import org.odftoolkit.odfdom.dom.element.table.TableTableElement;
 import org.odftoolkit.odfdom.dom.element.table.TableTableHeaderRowsElement;
 import org.odftoolkit.odfdom.dom.element.table.TableTableRowElement;
 import org.odftoolkit.odfdom.dom.element.text.TextAElement;
 import org.odftoolkit.odfdom.dom.element.text.TextBookmarkElement;
 import org.odftoolkit.odfdom.dom.element.text.TextBookmarkStartElement;
 import org.odftoolkit.odfdom.dom.element.text.TextHElement;
 import org.odftoolkit.odfdom.dom.element.text.TextIndexBodyElement;
 import org.odftoolkit.odfdom.dom.element.text.TextLineBreakElement;
 import org.odftoolkit.odfdom.dom.element.text.TextListElement;
 import org.odftoolkit.odfdom.dom.element.text.TextListItemElement;
 import org.odftoolkit.odfdom.dom.element.text.TextPElement;
 import org.odftoolkit.odfdom.dom.element.text.TextPageNumberElement;
 import org.odftoolkit.odfdom.dom.element.text.TextSElement;
 import org.odftoolkit.odfdom.dom.element.text.TextSectionElement;
 import org.odftoolkit.odfdom.dom.element.text.TextSoftPageBreakElement;
 import org.odftoolkit.odfdom.dom.element.text.TextSpanElement;
 import org.odftoolkit.odfdom.dom.element.text.TextTabElement;
 import org.odftoolkit.odfdom.dom.element.text.TextTableOfContentElement;
 import org.odftoolkit.odfdom.dom.element.text.TextTableOfContentSourceElement;
 import org.odftoolkit.odfdom.dom.style.OdfStyleFamily;
 import org.odftoolkit.odfdom.pkg.OdfElement;
 import org.w3c.dom.Node;
 import org.w3c.dom.Text;
 
 import com.lowagie.text.Chunk;
 import com.lowagie.text.DocumentException;
 import com.lowagie.text.Font;
 import com.lowagie.text.Image;
 
 import fr.opensagres.xdocreport.utils.StringUtils;
 
 /**
  * fixes for pdf conversion by Leszek Piotrowicz <leszekp@safe-mail.net>
  */
 public class ElementVisitorForIText
     extends ElementVisitorConverter
 {
     private final StyleEngineForIText styleEngine;
 
     // private final PDFViaITextOptions options;
 
     private IStylableContainer currentContainer;
 
     private StylableMasterPage currentMasterPage;
 
     private StylableDocument document;
 
     private boolean parseOfficeTextElement = false;
 
     private boolean inTableOfContent; // tabs processing
 
     private List<Integer> currentHeadingNumbering; // heading processing
 
     private StylableTable currentTable; // table processing
 
     private int currentListLevel; // list processing
 
     private StylableList previousList; // list processing
 
     public ElementVisitorForIText( OdfDocument odfDocument, OutputStream out, Writer writer,
                                    StyleEngineForIText styleEngine, PdfOptions options )
     {
         super( odfDocument, options.getExtractor(), out, writer );
         this.styleEngine = styleEngine;
         // this.options = options != null ? options : PDFViaITextOptions.create();
         // Create document
         try
         {
             document = new StylableDocument( out, styleEngine );
         }
         catch ( DocumentException e )
         {
             throw new ODFConverterException( e );
         }
     }
 
     // ---------------------- visit root
     // styles.xml//office:document-styles/office:master-styles/style:master-page
 
     /**
      * Generate XHTML page footer + header
      */
     @Override
     public void visit( StyleMasterPageElement ele )
     {
         String name = ele.getStyleNameAttribute();
         String pageLayoutName = ele.getStylePageLayoutNameAttribute();
         String nextStyleName = ele.getStyleNextStyleNameAttribute();
         currentMasterPage = new StylableMasterPage( name, pageLayoutName, nextStyleName );
         document.addMasterPage( currentMasterPage );
         super.visit( ele );
         currentMasterPage = null;
     }
 
     // ---------------------- visit
     // styles.xml//office:document-styles/office:master-styles/style:master-page/style-header
 
     @Override
     public void visit( StyleHeaderElement ele )
     {
         StylableHeaderFooter header = document.createHeaderFooter( true );
         Style style = document.getStyleMasterPage( currentMasterPage );
         if ( style != null )
         {
             document.applyStyles( style );
             header.applyStyles( style );
         }
         currentMasterPage.setHeader( header );
         StylableTableCell tableCell = header.getTableCell();
         currentContainer = tableCell;
         super.visit( ele );
         header.flush();
         currentContainer = null;
     }
 
     @Override
     public void visit( StyleHeaderLeftElement ele )
     {
         // TODO : implement it.
     }
 
     // ---------------------- visit
     // styles.xml//office:document-styles/office:master-styles/style:master-page/style-footer
 
     @Override
     public void visit( StyleFooterElement ele )
     {
         StylableHeaderFooter footer = document.createHeaderFooter( false );
         Style style = document.getStyleMasterPage( currentMasterPage );
         if ( style != null )
         {
             document.applyStyles( style );
             footer.applyStyles( style );
         }
         currentMasterPage.setFooter( footer );
         StylableTableCell tableCell = footer.getTableCell();
         currentContainer = tableCell;
         super.visit( ele );
         footer.flush();
         currentContainer = null;
 
     }
 
     @Override
     public void visit( StyleFooterLeftElement ele )
     {
         // TODO : implement it.
     }
 
     // ---------------------- visit root //office-body/office-text
 
     @Override
     public void visit( OfficeTextElement ele )
     {
         this.parseOfficeTextElement = true;
         currentContainer = document;
         super.visit( ele );
         this.parseOfficeTextElement = false;
     }
 
     @Override
     public void visit( TextTableOfContentElement ele )
     {
         inTableOfContent = true;
         super.visit( ele );
         inTableOfContent = false;
     }
 
     @Override
     public void visit( TextTableOfContentSourceElement ele )
     {
         // do not visit child nodes
         // they may contain unnecessary text
     }
 
     @Override
     public void visit( TextIndexBodyElement ele )
     {
         super.visit( ele );
     }
 
     @Override
     public void visit( TextSectionElement ele )
     {
         StylableDocumentSection documentSection =
             document.createDocumentSection( currentContainer, !parseOfficeTextElement );
         applyStyles( ele, documentSection );
         addITextContainer( ele, documentSection );
     }
 
     // ---------------------- visit //text:h
 
     @Override
     public void visit( TextHElement ele )
     {
         // compute heading numbering (ie 1.3.1)
         int outlineLevel = ele.getTextOutlineLevelAttribute() != null ? ele.getTextOutlineLevelAttribute() : 1;
         if ( currentHeadingNumbering == null )
         {
             currentHeadingNumbering = new ArrayList<Integer>();
         }
         while ( currentHeadingNumbering.size() > outlineLevel )
         {
             currentHeadingNumbering.remove( currentHeadingNumbering.size() - 1 );
         }
         if ( currentHeadingNumbering.size() == outlineLevel )
         {
             currentHeadingNumbering.set( outlineLevel - 1, currentHeadingNumbering.get( outlineLevel - 1 ) + 1 );
         }
         while ( currentHeadingNumbering.size() < outlineLevel )
         {
             currentHeadingNumbering.add( StylableHeading.getFirst( currentContainer.getLastStyleApplied(),
                                                                    currentHeadingNumbering.size() + 1 ) );
         }
 
         StylableHeading heading =
             document.createHeading( currentContainer, new ArrayList<Integer>( currentHeadingNumbering ) );
         applyStyles( ele, heading );
         addITextContainer( ele, heading );
     }
 
     // ---------------------- visit //text:p
 
     @Override
     public void visit( TextPElement ele )
     {
         StylableParagraph paragraph = document.createParagraph( currentContainer );
         applyStyles( ele, paragraph );
         addITextContainer( ele, paragraph );
 
     }
 
     // ---------------------- visit //text:span
 
     @Override
     public void visit( TextSpanElement ele )
     {
         StylablePhrase phrase = document.createPhrase( currentContainer );
         applyStyles( ele, phrase );
         addITextContainer( ele, phrase );
     }
 
     // ---------------------- visit //text:a
 
     @Override
     public void visit( TextAElement ele )
     {
         StylableAnchor anchor = document.createAnchor( currentContainer );
         String reference = ele.getXlinkHrefAttribute();
         applyStyles( ele, anchor );
 
         if ( anchor.getFont().getColor() == null )
         {
             // if no color was applied to the link get the font of the paragraph and set blue color.
             Font linkFont = anchor.getFont();
             Style style = currentContainer.getLastStyleApplied();
             if ( style != null )
             {
                 StyleTextProperties textProperties = style.getTextProperties();
                 if ( textProperties != null )
                 {
                     Font font = textProperties.getFont();
                     if ( font != null )
                     {
                         linkFont = new Font( font );
                         anchor.setFont( linkFont );
                     }
                 }
             }
             linkFont.setColor( Color.BLUE );
         }
 
         // set the link
         if ( reference.endsWith( StylableHeading.IMPLICIT_REFERENCE_SUFFIX ) )
         {
             reference = "#" + StylableHeading.generateImplicitDestination( reference );
         }
         anchor.setReference( reference );
         // Add to current container.
         addITextContainer( ele, anchor );
     }
 
     @Override
     public void visit( TextBookmarkElement ele )
     {
         // destination for a local anchor
         // chunk with empty text does not work as local anchor
         // so we create chunk with invisible but not empty text content
         // if bookmark is the last chunk in a paragraph something must be added after or it does not work
         createAndAddChunk( ODFUtils.TAB_STR, ele.getTextNameAttribute(), false );
     }
 
     @Override
     public void visit( TextBookmarkStartElement ele )
     {
         createAndAddChunk( ODFUtils.TAB_STR, ele.getTextNameAttribute(), false );
         super.visit( ele );
     }
 
     // ---------------------- visit table:table (ex : <table:table
     // table:name="Tableau1" table:style-name="Tableau1">)
 
     @Override
     public void visit( TableTableElement ele )
     {
         float[] columnWidth = ODFUtils.getColumnWidths( ele, odfDocument );
         StylableTable table = document.createTable( currentContainer, columnWidth.length );
         try
         {
             table.setTotalWidth( columnWidth );
         }
         catch ( DocumentException e )
         {
             // Do nothing
         }
         applyStyles( ele, table );
         StylableTable oldTable = currentTable;
         currentTable = table;
         addITextContainer( ele, table );
         currentTable = oldTable;
     }
 
     // ---------------------- visit table:table-header-rows
 
     @Override
     public void visit( TableTableHeaderRowsElement ele )
     {
         // we want to count table rows nested inside table header rows element
         // to determine how many header rows we have in current table
         currentTable.beginTableHeaderRows();
         super.visit( ele );
         currentTable.endTableHeaderRows();
     }
 
     // ---------------------- visit table:table-row
 
     @Override
     public void visit( TableTableRowElement ele )
     {
         Style currentRowStyle = getStyle( ele, null );
         currentTable.beginTableRow( currentRowStyle );
         super.visit( ele );
         currentTable.endTableRow();
     }
 
     // ---------------------- visit table:table-cell
 
     @Override
     public void visit( TableTableCellElement ele )
     {
         StylableTableCell tableCell = document.createTableCell( currentContainer );
 
         // table:number-columns-spanned
         Integer colSpan = ele.getTableNumberColumnsSpannedAttribute();
         if ( colSpan != null )
         {
             tableCell.setColspan( colSpan );
         }
         // table:number-rows-spanned
         Integer rowSpan = ele.getTableNumberRowsSpannedAttribute();
         if ( rowSpan != null )
         {
             tableCell.setRowspan( rowSpan );
 
         }
         // Apply styles coming from table-row
         if ( currentTable.getCurrentRowStyle() != null )
         {
             tableCell.applyStyles( currentTable.getCurrentRowStyle() );
         }
         // Apply styles coming from table-cell
         applyStyles( ele, tableCell );
         addITextContainer( ele, tableCell );
     }
 
     // ---------------------- visit text:list
 
     @Override
     public void visit( TextListElement ele )
     {
         currentListLevel++;
         StylableList list = document.createList( currentContainer, currentListLevel );
         applyStyles( ele, list );
         Boolean continueNumbering = ele.getTextContinueNumberingAttribute();
         if ( Boolean.TRUE.equals( continueNumbering ) && previousList != null
             && previousList.getLastStyleApplied() != null && list.getLastStyleApplied() != null
             && previousList.getLastStyleApplied().getStyleName() != null
             && previousList.getLastStyleApplied().getStyleName().equals( list.getLastStyleApplied().getStyleName() ) )
         {
             list.setFirst( previousList.getIndex() );
         }
         addITextContainer( ele, list );
         currentListLevel--;
         previousList = list;
     }
 
     // ---------------------- visit text:listitem
 
     @Override
     public void visit( TextListItemElement ele )
     {
         StylableListItem listItem = document.createListItem( currentContainer );
         addITextContainer( ele, listItem );
     }
 
     // ---------------------- visit draw:image
 
     @Override
     protected void visitImage( DrawImageElement ele, String href, byte[] imageStream )
     {
         // add image in the pdf.
         Image imageObj = StylableImage.getImage( imageStream );
         if ( imageObj != null )
         {
             DrawFrameElement frame = null;
             Float x = null;
             Float y = null;
             Float width = null;
             Float height = null;
             // set width, height....image
             Node parentNode = ele.getParentNode();
             if ( parentNode instanceof DrawFrameElement )
             {
                 frame = (DrawFrameElement) parentNode;
                 String svgX = frame.getSvgXAttribute();
                 if ( StringUtils.isNotEmpty( svgX ) )
                 {
                     x = ODFUtils.getDimensionAsPoint( svgX );
                 }
                 String svgY = frame.getSvgYAttribute();
                 if ( StringUtils.isNotEmpty( svgY ) )
                 {
                     y = ODFUtils.getDimensionAsPoint( svgY );
                 }
                 String svgWidth = frame.getSvgWidthAttribute();
                 if ( StringUtils.isNotEmpty( svgWidth ) )
                 {
                     width = ODFUtils.getDimensionAsPoint( svgWidth );
                 }
                 String svgHeight = frame.getSvgHeightAttribute();
                 if ( StringUtils.isNotEmpty( svgHeight ) )
                 {
                     height = ODFUtils.getDimensionAsPoint( svgHeight );
                 }
             }
             StylableImage image = document.createImage( currentContainer, imageObj, x, y, width, height );
             if ( frame != null )
             {
                 applyStyles( frame, image );
             }
             addITextElement( image );
         }
     }
 
     @Override
     protected boolean isNeedImageStream()
     {
         return true;
     }
 
    @Override
    public void visit( SvgDescElement ele )
    {
        // do not visit child nodes
        // they may contain unnecessary text
    }

     // ---------------------- visit //text:soft-page-break
 
     @Override
     public void visit( TextSoftPageBreakElement ele )
     {
     }
 
     // ---------------------- visit //text:tab
 
     @Override
     public void visit( TextTabElement ele )
     {
         StylableTab tab = document.createTab( currentContainer, inTableOfContent );
         Style style = currentContainer.getLastStyleApplied();
         if ( style != null )
         {
             tab.applyStyles( style );
         }
         addITextElement( tab );
     }
 
     // ---------------------- visit text:line-break
 
     @Override
     public void visit( TextLineBreakElement ele )
     {
         createAndAddChunk( "\n", null, false );
     }
 
     // ---------------------- visit text:s
 
     @Override
     public void visit( TextSElement ele )
     {
         String spaceStr = " ";
         Integer spaceCount = ele.getTextCAttribute();
         if ( spaceCount != null && spaceCount > 1 )
         {
             for ( int i = 1; i < spaceCount; i++ )
             {
                 spaceStr += " ";
             }
         }
         createAndAddChunk( spaceStr, null, false );
     }
 
     @Override
     public void visit( TextPageNumberElement ele )
     {
         createAndAddChunk( "#", null, true );
     }
 
     @Override
     protected void processTextNode( Text node )
     {
         createAndAddChunk( node.getTextContent(), null, false );
     }
 
     private Chunk createAndAddChunk( String textContent, String localDestinationName, boolean pageNumberChunk )
     {
         // StylableChunk can replace several ODT elements
         // plain text
         // text:bookmark
         // text:line-break
         // text:s
         // text:page-number
         StylableChunk chunk = document.createChunk( currentContainer, textContent );
         Style style = currentContainer.getLastStyleApplied();
         if ( style != null )
         {
             chunk.applyStyles( style );
         }
         if ( localDestinationName != null )
         {
             chunk.setLocalDestination( localDestinationName );
         }
         if ( pageNumberChunk )
         {
             chunk.setPageNumberChunk( pageNumberChunk );
         }
         addITextElement( chunk );
         return chunk;
     }
 
     @Override
     public void save()
         throws IOException
     {
         if ( document != null )
         {
             document.close();
         }
         super.save();
     }
 
     private void addITextContainer( OdfElement ele, IStylableContainer newContainer )
     {
         addITextContainer( ele, newContainer, true );
     }
 
     private void addITextContainer( OdfElement ele, IStylableContainer newContainer, boolean add )
     {
         IStylableContainer oldContainer = currentContainer;
         try
         {
             currentContainer = newContainer;
             super.visit( ele );
             if ( add )
             {
                 oldContainer.addElement( newContainer.getElement() );
             }
         }
         finally
         {
             currentContainer = oldContainer;
         }
     }
 
     private void addITextElement( IStylableElement element )
     {
         currentContainer.addElement( element.getElement() );
     }
 
     private void applyStyles( OdfElement ele, IStylableElement element )
     {
         Style style = getStyle( ele, element );
         if ( style != null )
         {
             if ( parseOfficeTextElement )
             {
                 String masterPageName = style.getMasterPageName();
                 if ( StringUtils.isNotEmpty( masterPageName ) )
                 {
                     // explicit master page activation
                     StylableMasterPage masterPage = document.getMasterPage( masterPageName );
                     if ( masterPage != null && masterPage != document.getActiveMasterPage() )
                     {
                         document.setActiveMasterPage( masterPage );
                     }
                 }
                 else if ( document.getActiveMasterPage() == null )
                 {
                     // no master page was activated yet
                     // activate default
                     document.setActiveMasterPage( document.getDefaultMasterPage() );
                 }
             }
             element.applyStyles( style );
         }
     }
 
     private Style getStyle( OdfElement e, IStylableElement element )
     {
         Style style = null;
         Style parentElementStyle = element != null ? getParentElementStyle( element ) : null;
         if ( e instanceof OdfStylableElement )
         {
             OdfStylableElement ele = (OdfStylableElement) e;
 
             String styleName = ele.getStyleName();
             String familyName = ele.getStyleFamily() != null ? ele.getStyleFamily().getName() : null;
 
             style = styleEngine.getStyle( familyName, styleName, parentElementStyle );
         }
         else if ( e instanceof TextListElement )
         {
             TextListElement ele = (TextListElement) e;
 
             String styleName = ele.getTextStyleNameAttribute();
 
             style = styleEngine.getStyle( OdfStyleFamily.List.getName(), styleName, parentElementStyle );
         }
         return style;
     }
 
     private Style getParentElementStyle( IStylableElement element )
     {
         for ( IStylableContainer c = element.getParent(); c != null; c = c.getParent() )
         {
             Style style = c.getLastStyleApplied();
             if ( style != null )
             {
                 return style;
             }
         }
         return null;
     }
 }
