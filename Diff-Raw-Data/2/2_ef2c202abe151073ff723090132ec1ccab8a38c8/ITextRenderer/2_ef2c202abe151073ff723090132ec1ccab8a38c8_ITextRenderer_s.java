 /*
  * {{{ header & license
  * Copyright (c) 2006 Wisconsin Court System
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
  * }}}
  */
 package org.xhtmlrenderer.pdf;
 
 import java.awt.Rectangle;
 import java.awt.Shape;
 import java.io.OutputStream;
 import java.util.List;
 
 import org.w3c.dom.Document;
 import org.w3c.dom.Element;
 import org.xhtmlrenderer.css.style.CalculatedStyle;
 import org.xhtmlrenderer.extend.NamespaceHandler;
 import org.xhtmlrenderer.extend.UserInterface;
 import org.xhtmlrenderer.layout.Boxing;
 import org.xhtmlrenderer.layout.Layer;
 import org.xhtmlrenderer.layout.LayoutContext;
 import org.xhtmlrenderer.layout.SharedContext;
 import org.xhtmlrenderer.layout.content.DomToplevelNode;
 import org.xhtmlrenderer.render.BlockBox;
 import org.xhtmlrenderer.render.Box;
 import org.xhtmlrenderer.render.PageBox;
 import org.xhtmlrenderer.render.RenderingContext;
 import org.xhtmlrenderer.simple.extend.XhtmlNamespaceHandler;
 import org.xhtmlrenderer.swing.NaiveUserAgent;
 import org.xhtmlrenderer.util.Configuration;
 
 import com.lowagie.text.DocumentException;
 import com.lowagie.text.pdf.PdfWriter;
 
 public class ITextRenderer {
     private static final int DEFAULT_PIXELS_PER_POINT = 5;
     
     private SharedContext _sharedContext;
     private ITextOutputDevice _outputDevice;
     
     private Document _doc;
     private Box _root;
     
     private int _pixelsPerPoint;
     
     public ITextRenderer() {
         this(DEFAULT_PIXELS_PER_POINT);
     }
     
     public ITextRenderer(int pixelsPerPoint) {
         _pixelsPerPoint = pixelsPerPoint;
         _sharedContext = new SharedContext(new NaiveUserAgent());
         _sharedContext.setFontResolver(new ITextFontResolver());
         _sharedContext.setTextRenderer(new ITextTextRenderer());
         _sharedContext.setDPI(72*_pixelsPerPoint);
         _sharedContext.setPrint(true);
         
         _outputDevice = new ITextOutputDevice(_pixelsPerPoint);
     }
     
     private Document loadDocument(final String uri) {
         return _sharedContext.getUac().getXMLResource(uri).getDocument();
     }
     
     public void setDocument(String uri) {
         setDocument(loadDocument(uri), uri);
     }
     
     public void setDocument(Document doc, String url) {
         setDocument(doc, url, new XhtmlNamespaceHandler());
     }
     
     public void setDocument(Document doc, String url, NamespaceHandler nsh) {
         _doc = doc;
         
         if (Configuration.isTrue("xr.cache.stylesheets", true)) {
             _sharedContext.getCss().flushStyleSheets();
         } else {
             _sharedContext.getCss().flushAllStyleSheets();
         }
         _sharedContext.setBaseURL(url);
         _sharedContext.setNamespaceHandler(nsh);
         _sharedContext.getCss().setDocumentContext(
                 _sharedContext, _sharedContext.getNamespaceHandler(), 
                 doc, new NullUserInterface());
     }
     
     public void layout() {
         LayoutContext c = newLayoutContext();
         BlockBox root = Boxing.constructBox(c, new DomToplevelNode(_doc));
         Boxing.layout(c, root, new DomToplevelNode(_doc));
         _root = root;
     }
     
     private RenderingContext newRenderingContext() {
         RenderingContext result = _sharedContext.newRenderingContextInstance();
         result.setFontContext(new ITextFontContext());
         
         
         result.setOutputDevice(_outputDevice);
         
         _sharedContext.getTextRenderer().setup(result.getFontContext());
 
         return result;
     }
 
     private LayoutContext newLayoutContext() {
 
         Rectangle extents = new Rectangle(0, 0, 1, 1);
 
         LayoutContext result = _sharedContext.newLayoutContextInstance(extents);
         result.setFontContext(new ITextFontContext());
         result.setReplacedElementFactory(new ITextReplacedElementFactory());
         
         _sharedContext.getTextRenderer().setup(result.getFontContext());
         
         if (result.isPrint()) {
             PageBox first = Layer.createPageBox(result, "first");
             extents = new Rectangle(0, 0, 
                     first.getContentWidth(result), first.getContentHeight(result));
             result.setExtents(extents);
         }
         
         return result;
     }
     
     /**
     * <B>NOTE:</B> Caller is responsible for cleaning up the OutputStream is something
      * goes wrong.
      */
     public void createPDF(OutputStream os) throws DocumentException {
         List pages = _root.getLayer().getPages();
         
         RenderingContext c = newRenderingContext();
         PageBox firstPage = (PageBox)pages.get(0);
         com.lowagie.text.Rectangle firstPageSize = new com.lowagie.text.Rectangle(
                 0, 0, 
                 firstPage.getWidth(c) / _pixelsPerPoint, 
                 firstPage.getHeight(c) / _pixelsPerPoint);
         
         com.lowagie.text.Document doc = 
             new com.lowagie.text.Document(firstPageSize, 0, 0, 0, 0);
         PdfWriter writer = PdfWriter.getInstance(doc, os);
         
         doc.open();
         
         _outputDevice.initializePage(writer.getDirectContent(), firstPageSize.height());
         
         _root.getLayer().assignPagePaintingPositions(c, Layer.PAGED_MODE_PRINT);
         
         int pageCount = _root.getLayer().getPages().size();
         
         for (int i = 0; i < pageCount; i++) {
             PageBox currentPage = (PageBox)pages.get(i);
             paintPage(c, currentPage);
             _outputDevice.finishPage();
             if (i != pageCount - 1) {
                 PageBox nextPage = (PageBox)pages.get(i+1);
                 com.lowagie.text.Rectangle nextPageSize = new com.lowagie.text.Rectangle(
                         0, 0, 
                         nextPage.getWidth(c) / _pixelsPerPoint, 
                         nextPage.getHeight(c) / _pixelsPerPoint);
                 doc.setPageSize(nextPageSize);
                 doc.newPage();
                 _outputDevice.initializePage(
                         writer.getDirectContent(), nextPageSize.height());
             }
         }
         doc.close();
     }
     
     private void paintPage(RenderingContext c, PageBox page) {
         Shape working = _outputDevice.getClip();
         
         Rectangle content = page.getPrintingClippingBounds(c);
         _outputDevice.clip(content);
         
         
         int top = -page.getPaintingTop() + 
             page.getStyle().getMarginBorderPadding(c, CalculatedStyle.TOP);
         
         int left = page.getStyle().getMarginBorderPadding(c, CalculatedStyle.LEFT);
         
         _outputDevice.translate(left, top);
         _root.getLayer().paint(c, 0, 0);
         _outputDevice.translate(-left, -top);
         
         _outputDevice.setClip(working);
         page.paintAlternateFlows(c, _root.getLayer(), Layer.PAGED_MODE_PRINT, 0);
         
         page.paintBorder(c, 0);
 
         _outputDevice.setClip(working);
     }    
     
     private static final class NullUserInterface implements UserInterface {
         public boolean isHover(Element e) {
             return false;
         }
 
         public boolean isActive(Element e) {
             return false;
         }
 
         public boolean isFocus(Element e) {
             return false;
         }
     }
 }
