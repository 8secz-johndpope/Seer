 package cz.incad.kramerius.pdf.impl;
 
 import static cz.incad.kramerius.FedoraNamespaces.RDF_NAMESPACE_URI;
 import static cz.incad.kramerius.utils.BiblioModsUtils.getPageNumber;
 import static cz.incad.kramerius.utils.BiblioModsUtils.getTitle;
 import static cz.incad.kramerius.utils.imgs.KrameriusImageSupport.readImage;
 import static cz.incad.kramerius.utils.imgs.KrameriusImageSupport.writeImageToStream;
 
 import java.awt.Image;
 import java.io.BufferedReader;
 import java.io.ByteArrayOutputStream;
 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
 import java.io.StringReader;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.nio.charset.Charset;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Locale;
 import java.util.Map;
 import java.util.ResourceBundle;
 import java.util.logging.Level;
 
 import javax.xml.transform.Transformer;
 import javax.xml.transform.TransformerException;
 import javax.xml.transform.TransformerFactory;
 import javax.xml.transform.dom.DOMSource;
 import javax.xml.transform.stream.StreamResult;
 import javax.xml.transform.stream.StreamSource;
 import javax.xml.xpath.XPathExpressionException;
 
 import org.w3c.dom.DOMException;
 import org.w3c.dom.Element;
 import org.w3c.dom.Node;
 import org.w3c.dom.NodeList;
 
 import com.google.inject.Inject;
 import com.google.inject.Provider;
 import com.google.inject.name.Named;
 import com.lowagie.text.BadElementException;
 import com.lowagie.text.Chunk;
 import com.lowagie.text.Document;
 import com.lowagie.text.DocumentException;
 import com.lowagie.text.Font;
 import com.lowagie.text.Paragraph;
 import com.lowagie.text.Rectangle;
 import com.lowagie.text.pdf.BaseFont;
 import com.lowagie.text.pdf.PdfAction;
 import com.lowagie.text.pdf.PdfContentByte;
 import com.lowagie.text.pdf.PdfDestination;
 import com.lowagie.text.pdf.PdfOutline;
 import com.lowagie.text.pdf.PdfPCell;
 import com.lowagie.text.pdf.PdfPTable;
 import com.lowagie.text.pdf.PdfPTableEvent;
 import com.lowagie.text.pdf.PdfWriter;
 import com.lowagie.text.pdf.draw.LineSeparator;
 import com.lowagie.text.pdf.draw.VerticalPositionMark;
 
 import cz.incad.kramerius.Constants;
 import cz.incad.kramerius.FedoraAccess;
 import cz.incad.kramerius.FedoraNamespaces;
 import cz.incad.kramerius.FedoraRelationship;
 import cz.incad.kramerius.KrameriusModels;
 import cz.incad.kramerius.RelsExtHandler;
 import cz.incad.kramerius.pdf.Break;
 import cz.incad.kramerius.pdf.GeneratePDFService;
 import cz.incad.kramerius.pdf.pdfpages.AbstractPage;
 import cz.incad.kramerius.pdf.pdfpages.AbstractRenderedDocument;
 import cz.incad.kramerius.pdf.pdfpages.ImagePage;
 import cz.incad.kramerius.pdf.pdfpages.OutlineItem;
 import cz.incad.kramerius.pdf.pdfpages.RenderedDocument;
 import cz.incad.kramerius.pdf.pdfpages.TextPage;
 import cz.incad.kramerius.service.ResourceBundleService;
 import cz.incad.kramerius.service.TextsService;
 import cz.incad.kramerius.utils.BiblioModsUtils;
 import cz.incad.kramerius.utils.DCUtils;
 import cz.incad.kramerius.utils.IOUtils;
 import cz.incad.kramerius.utils.XMLUtils;
 import cz.incad.kramerius.utils.conf.KConfiguration;
 import cz.incad.kramerius.utils.imgs.ImageMimeType;
 import cz.incad.kramerius.utils.pid.LexerException;
 import cz.incad.kramerius.utils.pid.PIDParser;
 
 public class GeneratePDFServiceImpl implements GeneratePDFService {
 	
 	public static final java.util.logging.Logger LOGGER = java.util.logging.Logger
 			.getLogger(GeneratePDFServiceImpl.class.getName());
 
     public static final int DEFAULT_WIDTH = 595;
     public static final int DEFAULT_HEIGHT = 842;
 
 	public static com.lowagie.text.Image DEFAULT_LOGO_IMAGE;
 	static {
 		try {
 			DEFAULT_LOGO_IMAGE = com.lowagie.text.Image.getInstance(GeneratePDFServiceImpl.class.getResource("res/kramerius_logo.png"));
 		} catch (BadElementException e) {
 			LOGGER.log(Level.SEVERE, e.getMessage(), e);
 		} catch (MalformedURLException e) {
 			LOGGER.log(Level.SEVERE, e.getMessage(), e);
 		} catch (IOException e) {
 			LOGGER.log(Level.SEVERE, e.getMessage(), e);
 		}
 	}
 
 	private FedoraAccess fedoraAccess;
 	private KConfiguration configuration;
 	private Provider<Locale> localeProvider;
 	private TextsService textsService;
 	private ResourceBundleService resourceBundleService;
 	
 	@Inject
 	public GeneratePDFServiceImpl(@Named("securedFedoraAccess") FedoraAccess fedoraAccess, KConfiguration configuration, Provider<Locale> localeProvider, TextsService textsService, ResourceBundleService resourceBundleService) {
 		super();
 		this.fedoraAccess = fedoraAccess;
 		this.configuration = configuration;
 		this.localeProvider = localeProvider;
 		this.textsService = textsService;
 		this.configuration = configuration;
 		this.resourceBundleService = resourceBundleService;
 		try {
 			this.init();
 		} catch (IOException e) {
 			LOGGER.log(Level.SEVERE, e.getMessage(), e);
 		}
 	}
 	
 	private void init() throws IOException {
 		String[] texts = 
 		{"first_page",
 		"first_page_CZ_cs",
 		"security_fail",
 		"security_fail_CZ_cs"};
 		copyFiles(texts,"res/", this.textsService.textsFolder());
 		String[] xlsts = 
 		{"template.xslt"};
 		copyFiles(xlsts,"templates/", this.templatesFolder());
 	}
 
 	private void copyFiles(String[] texts, String prefix, File folder) throws FileNotFoundException,
 			IOException {
 		for (String def : texts) {
 			InputStream is = null;
 			FileOutputStream os = null;
 			try {
 				File file = new File(folder, def);
 				if (!file.exists()) {
 					is= this.getClass().getResourceAsStream(prefix+def);
 					os = new FileOutputStream(file);
 					IOUtils.copyStreams(is, os);
 				}
 			} finally {
 				if (os != null) {
 					try {
 						os.close();
 					} catch (Exception e) {
 						LOGGER.log(Level.SEVERE, e.getMessage(), e);
 					}
 				}
 				if (is != null) {
 					try {
 						is.close();
 					} catch(Exception e) {
 						LOGGER.log(Level.SEVERE, e.getMessage(), e);
 					}
 				}
 			}
 		}
 	}
 
 	@Override
 	public AbstractRenderedDocument generateCustomPDF(AbstractRenderedDocument rdoc, String parentUUID, OutputStream os, Break brk, String djvUrl, String i18nUrl) throws IOException {
 		try {
 			String brokenPage = null;
 			Document doc = createDocument();
 			PdfWriter writer = PdfWriter.getInstance(doc, os);
 			doc.open();
 			insertFirstPage(rdoc, parentUUID, rdoc.getUuidTitlePage(), writer, doc, djvUrl);
 			doc.newPage();
 			int pocetStranek = 0;
 			List<AbstractPage> pages = new ArrayList<AbstractPage>(rdoc.getPages());
 			while(!pages.isEmpty()) {
 				pocetStranek += 1;
 				AbstractPage page = pages.remove(0);
 				doc.newPage();
 				if (page instanceof ImagePage) {
 					ImagePage iPage = (ImagePage) page;
 					insertOutlinedImagePage(iPage, writer, doc, djvUrl);
 				} else {
 					TextPage tPage = (TextPage) page;
 					insertOutlinedTextPage(tPage, writer, doc, rdoc.getDocumentTitle(), i18nUrl);
 				}
 				os.flush();
 				if (brk.broken(page.getUuid())) {
 					brokenPage = page.getUuid();
 					rdoc.removePagesTill(page.getUuid());
 					break;
 				}
 			}
 			
 			if (brokenPage == null) {
 				rdoc.removePages();
 			}
 
 			
 			OutlineItem root = rdoc.getOutlineItemRoot();
 			if (brokenPage != null) {
 				OutlineItem right = rdoc.getOutlineItemRoot().copy();
 				OutlineItem left = rdoc.getOutlineItemRoot().copy();
 				rdoc.divide(left, right, brokenPage);
 				root = left;
 				rdoc.setOutlineItemRoot(right);
 			}
 			
 			PdfContentByte cb = writer.getDirectContent();
 			PdfOutline pdfRoot = cb.getRootOutline();
 			StringBuffer buffer = new StringBuffer();
 			root.debugInformations(buffer, 1);
 			fillOutline(pdfRoot, root);
 			doc.close();
 			doc.close();
 			os.flush();
 
 		} catch (DocumentException e) {
 			LOGGER.log(Level.SEVERE, e.getMessage(), e);
 		} catch (XPathExpressionException e) {
 			LOGGER.log(Level.SEVERE, e.getMessage(), e);
 		} catch (TransformerException e) {
 			LOGGER.log(Level.SEVERE, e.getMessage(), e);
 		}
 		return rdoc;
 	}
 
 
 	@Override
 	public void generateCustomPDF(AbstractRenderedDocument rdoc, String parentUUID, OutputStream os, String djvuUrl, String i18nUrl) throws IOException {
 		try {
 			Document doc = createDocument();
 			PdfWriter writer = PdfWriter.getInstance(doc, os);
 			doc.open();
 			
 			insertFirstPage(rdoc, parentUUID, rdoc.getUuidTitlePage(), writer, doc, djvuUrl);
 
 			doc.newPage();
 			for (AbstractPage page : rdoc.getPages()) {
 				doc.newPage();
 				if (page instanceof ImagePage) {
 					ImagePage iPage = (ImagePage) page;
 					insertOutlinedImagePage(iPage, writer, doc, djvuUrl);
 				} else {
 					TextPage tPage = (TextPage) page;
 					if (tPage.getOutlineTitle().trim().equals("")) throw new IllegalArgumentException(page.getUuid());
 					insertOutlinedTextPage(tPage, writer, doc, rdoc.getDocumentTitle(), i18nUrl);
 				}
 			}
 
 			PdfContentByte cb = writer.getDirectContent();
 			PdfOutline pdfRoot = cb.getRootOutline();
 			OutlineItem rDocRoot = rdoc.getOutlineItemRoot();
 			StringBuffer buffer = new StringBuffer();
 			rDocRoot.debugInformations(buffer, 1);
 			fillOutline(pdfRoot, rDocRoot);
 
 			doc.close();
 			doc.close();
 			os.flush();
 
 		} catch (DocumentException e) {
 			LOGGER.log(Level.SEVERE, e.getMessage(), e);
 		} catch (XPathExpressionException e) {
 			LOGGER.log(Level.SEVERE, e.getMessage(), e);
 		} catch (TransformerException e) {
 			LOGGER.log(Level.SEVERE, e.getMessage(), e);
 		}
 		
 	}
 
 
 	private void fillOutline(PdfOutline pdfRoot, OutlineItem rDocRoot) {
 		OutlineItem[] children = rDocRoot.getChildren();
 		for (OutlineItem outlineItem : children) {
 			PdfOutline pdfOutline = new PdfOutline(pdfRoot, PdfAction.gotoLocalPage(outlineItem.getDestination(), false),outlineItem.getTitle());
 			fillOutline(pdfOutline, outlineItem);
 		}
 	}
 
 
 
 	
 
 	@Override
 	public void dynamicPDFExport(List<String> path, String uuidFrom, String uuidTo, String titlePage, OutputStream os, String djvuUrl, String i18nUrl) throws IOException {
 		LOGGER.info("current locale is "+localeProvider.get());
 		if (!path.isEmpty()) {
 			String lastUuid = path.get(path.size() -1);
 
 			org.w3c.dom.Document relsExt = this.fedoraAccess.getRelsExt(lastUuid);
 			KrameriusModels model = this.fedoraAccess.getKrameriusModel(relsExt);
 			
 			final AbstractRenderedDocument renderedDocument = new RenderedDocument(model, lastUuid);
 			StringBuffer bufferTitle = new StringBuffer();
 			for (int i = 0,ll=path.size(); i < ll; i++) {
 				bufferTitle.append(DCUtils.titleFromDC(this.fedoraAccess.getDC(path.get(i))));
 				if (i<ll-1) {
 					bufferTitle.append(" ");
 				}
 			}
 			//renderedDocument.setDocumentTitle(DCUtils.titleFromDC(this.fedoraAccess.getDC(lastUuid)));
 			renderedDocument.setDocumentTitle(bufferTitle.toString());
 			renderedDocument.setUuidTitlePage(titlePage);
 			renderedDocument.setUuidMainTitle(path.get(0));
 			
 			buildRenderingDocumentAsFlat(relsExt, renderedDocument, uuidFrom, uuidTo);
 			generateCustomPDF(renderedDocument, lastUuid,os, djvuUrl,i18nUrl);
 		}
 	}
 
 
 	@Override
 	public void fullPDFExport(String parentUUID, OutputStreams streams, Break brk, String djvuUrl, String i18nUrl) throws IOException {
 		org.w3c.dom.Document relsExt = this.fedoraAccess.getRelsExt(parentUUID);
 		KrameriusModels model = this.fedoraAccess.getKrameriusModel(relsExt);
 		
 		final AbstractRenderedDocument renderedDocument = new RenderedDocument(model, parentUUID);
 		renderedDocument.setDocumentTitle(getTitle(this.fedoraAccess.getBiblioMods(parentUUID), model));
 		renderedDocument.setUuidMainTitle(parentUUID);
 		
 		TextPage dpage = new TextPage(model, parentUUID);
 		dpage.setOutlineDestination("desc");
 		dpage.setOutlineTitle("Popis");
 		renderedDocument.addPage(dpage);
 		OutlineItem item = new OutlineItem();
 		item.setLevel(1); item.setParent(renderedDocument.getOutlineItemRoot()); 
 		item.setTitle("Popis"); item.setDestination("desc");
 		renderedDocument.getOutlineItemRoot().addChild(item);
 		
 		buildRenderingDocumentAsTree(relsExt, renderedDocument);
 		
 		AbstractRenderedDocument restOfDoc = renderedDocument;
 		OutputStream os = null;
 		boolean konec = false;
 		while(!konec) {
 			if (!restOfDoc.getPages().isEmpty()) {
 				os = streams.newOutputStream();
 				restOfDoc = generateCustomPDF(restOfDoc, parentUUID, os, brk, djvuUrl,i18nUrl);
 				
 				StringBuffer buffer = new StringBuffer();
 				restOfDoc.getOutlineItemRoot().debugInformations(buffer, 1);
 				os.close();
 			} else {
 				konec = true;
 				break;
 			}
 		}
 	}
 
 
 	
 	private void buildRenderingDocumentAsFlat(org.w3c.dom.Document relsExt, final AbstractRenderedDocument renderedDocument, final String uuidFrom, final String uuidTo ) throws IOException {
 		KrameriusModels krameriusModel = fedoraAccess.getKrameriusModel(relsExt);
 		if (krameriusModel.equals(KrameriusModels.PAGE)) {
 			Element documentElement = relsExt.getDocumentElement();
 			NodeList childNodes = documentElement.getChildNodes();
 			for (int i = 0,ll=childNodes.getLength(); i < ll; i++) {
 				Node node = childNodes.item(i);
 				if (node.getNodeType() ==  Node.ELEMENT_NODE) {
 					if ((node.getLocalName().equals("Description")) && (node.getNamespaceURI().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#"))) {
 						String attrAbout = ((Element)node).getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "about");
 						try {
 							renderedDocument.addPage(createPage(renderedDocument, attrAbout, FedoraRelationship.hasPage));
 						} catch (LexerException e) {
 							LOGGER.log(Level.SEVERE, e.getMessage(), e);
 						}
 					}
 				}
 			}
 		} else {
 			fedoraAccess.processRelsExt(relsExt, new RelsExtHandler() {
 				
 				private boolean acceptingState = false;
 				
 				@Override
 				public boolean accept(FedoraRelationship relation) {
 					return relation == FedoraRelationship.hasPage;
 				}
 
 				@Override
 				public void handle(Element elm, FedoraRelationship relation, int level) {
 					if (relation == FedoraRelationship.hasPage) {
 						try {
 							String pid = elm.getAttributeNS(RDF_NAMESPACE_URI, "resource");
 							PIDParser pidParse = new PIDParser(pid);
 							pidParse.disseminationURI();
 							String objectId = pidParse.getObjectId();
 							if (!acceptingState) {
 								if (objectId.equals(uuidFrom)) {
 									acceptingState = true;
 									String pidAttribute = elm.getAttributeNS(RDF_NAMESPACE_URI, "resource");
 									renderedDocument.addPage(createPage(renderedDocument, pidAttribute, relation));
 								}
 							} else {
 								if (objectId.equals(uuidTo)) {
 									acceptingState = false;
 								}
 								String pidAttribute = elm.getAttributeNS(RDF_NAMESPACE_URI, "resource");
 								renderedDocument.addPage(createPage(renderedDocument, pidAttribute, relation));
 							}
 							
 						} catch (LexerException e) {
 							LOGGER.log(Level.SEVERE, e.getMessage(), e);
 						} catch (IOException e) {
 							LOGGER.log(Level.SEVERE, e.getMessage(), e);
 						}
 					}
 				}
 
 				@Override
 				public boolean breakProcess() {
 					// TODO Auto-generated method stub
 					return false;
 				}
 			});
 		}
 	}
 	
 	private void buildRenderingDocumentAsTree(org.w3c.dom.Document relsExt, final AbstractRenderedDocument renderedDocument ) throws IOException {
 		fedoraAccess.processRelsExt(relsExt, new RelsExtHandler() {
 			
 				private int previousLevel = -1;
 				private OutlineItem currOutline = null;
 				
 				@Override
 				public void handle(Element elm, FedoraRelationship relation, int level) {
 					try {
 						String pidAttribute = elm.getAttributeNS(RDF_NAMESPACE_URI, "resource");
 						AbstractPage page = createPage(renderedDocument, pidAttribute, relation);
 						renderedDocument.addPage(page);
 						if (previousLevel == -1) {
 							// first
 							this.currOutline = createOutlineItem(renderedDocument.getOutlineItemRoot(), page.getOutlineDestination(), page.getOutlineTitle(), level);
 							StringBuffer buffer = new StringBuffer();
 							this.currOutline.debugInformations(buffer, 0);
 						} else if (previousLevel == level) {
 							this.currOutline = this.currOutline.getParent();
 							this.currOutline = createOutlineItem(this.currOutline, page.getOutlineDestination(), page.getOutlineTitle(), level);
 
 							StringBuffer buffer = new StringBuffer();
 							this.currOutline.debugInformations(buffer, 0);
 
 						} else if (previousLevel < level) {
 							// dolu
 							this.currOutline = createOutlineItem(this.currOutline, page.getOutlineDestination(), page.getOutlineTitle(), level);
 
 							StringBuffer buffer = new StringBuffer();
 							this.currOutline.debugInformations(buffer, 0);
 
 						} else if (previousLevel > level) {
 							// nahoru // za poslednim smerem nahoru
 							this.currOutline = this.currOutline.getParent();
 							
 							StringBuffer buffer = new StringBuffer();
 							this.currOutline.debugInformations(buffer, 0);
 							
 							this.currOutline = this.currOutline.getParent();
 							this.currOutline = createOutlineItem(this.currOutline, page.getOutlineDestination(), page.getOutlineTitle(), level);
 							
 						}
 
 						previousLevel = level;
 					} catch (DOMException e) {
 						LOGGER.log(Level.SEVERE, e.getMessage(), e);
 						throw new RuntimeException(e);
 					} catch (LexerException e) {
 						LOGGER.log(Level.SEVERE, e.getMessage(), e);
 						throw new RuntimeException(e);
 					} catch (IOException e) {
 						LOGGER.log(Level.SEVERE, e.getMessage(), e);
 						throw new RuntimeException(e);
 					}
 				}
 
 
 				private OutlineItem createOutlineItem(OutlineItem parent, String objectId, String biblioModsTitle, int level) {
 					OutlineItem item = new OutlineItem();
 					item.setDestination(objectId);
 
 					
 					item.setTitle(biblioModsTitle);
 					
 					parent.addChild(item);
 					item.setParent(parent);
 					item.setLevel(level);
 					return item;
 				}
 				
 				@Override
 				public boolean accept(FedoraRelationship relation) {
 					return relation.name().startsWith("has");
 				}
 
 
 				@Override
 				public boolean breakProcess() {
 					return false;
 				}
 			});
 	}
 	
 
 	protected AbstractPage createPage( final AbstractRenderedDocument renderedDocument,
 			String pid, FedoraRelationship relation)
 			throws LexerException, IOException {
 		//String pid = elm.getAttributeNS(RDF_NAMESPACE_URI, "resource");
 		PIDParser pidParse = new PIDParser(pid);
 		pidParse.disseminationURI();
 		String objectId = pidParse.getObjectId();
 		
 		org.w3c.dom.Document biblioMods = fedoraAccess.getBiblioMods(objectId);
 		org.w3c.dom.Document dc = fedoraAccess.getDC(objectId);
 		
 		AbstractPage page = null;
 		if (relation.equals(FedoraRelationship.hasPage)) {
 			
 			page = new ImagePage(KrameriusModels.PAGE, objectId);
 			page.setOutlineDestination(objectId);
 			String pageNumber = getPageNumber(biblioMods);
 			if (pageNumber.trim().equals("")) {
 				throw new IllegalStateException(objectId);
 			}
 			page.setPageNumber(pageNumber);
 			//renderedDocument.addPage(page);
 			Element part = XMLUtils.findElement(biblioMods.getDocumentElement(), "part", FedoraNamespaces.BIBILO_MODS_URI);
 			String attribute = part.getAttribute("type");
 			if (attribute != null) {
 				ResourceBundle resourceBundle = resourceBundleService.getResourceBundle("base", localeProvider.get());
 				String key = "pdf."+attribute;
 				if (resourceBundle.containsKey(key)) {
 					page.setOutlineTitle(page.getPageNumber()+" "+resourceBundle.getString(key));
 				} else {
 					page.setOutlineTitle(page.getPageNumber());
 					//throw new RuntimeException("");
 				}
 			}
 			if ((renderedDocument.getUuidTitlePage() == null) && ("TitlePage".equals(attribute))) {
 				renderedDocument.setUuidTitlePage(objectId);
 			}
 
 			if ((renderedDocument.getUuidFrontCover() == null) && ("FrontCover".equals(attribute))) {
 				renderedDocument.setUuidFrontCover(objectId);
 			}
 
 			if ((renderedDocument.getUuidBackCover() == null) && ("BackCover".equals(attribute))) {
 				renderedDocument.setUuidBackCover(objectId);
 			}
 
 			if (renderedDocument.getFirstPage() == null)  {
 				renderedDocument.setFirstPage(objectId);
 			}
 
 		} else {
 			page = new TextPage(relation.getPointingModel(), objectId);
 			page.setOutlineDestination(objectId);
 			String title = DCUtils.titleFromDC(dc);
 			if ((title == null) || title.equals("")) {
 				title = BiblioModsUtils.getTitle(biblioMods, relation.getPointingModel());
 			}
 			if (title.trim().equals("")) throw new IllegalArgumentException(objectId+" has no title ");
 			page.setOutlineTitle(title);
 		}
 		return page;
 	}
 
 
 	private static Document createDocument() {
 		Document doc = new Document(new Rectangle(DEFAULT_WIDTH, DEFAULT_HEIGHT));
 		return doc;
 	}
 
 	public PdfPTable insertTitleAndAuthors(AbstractRenderedDocument document) throws DocumentException, IOException {
 		PdfPTable pdfPTable = new PdfPTable(new float[] {1.0f});
 		
 		pdfPTable.setSpacingBefore(3f);
 		pdfPTable.getDefaultCell().disableBorderSide(PdfPCell.TOP);
 		pdfPTable.getDefaultCell().disableBorderSide(PdfPCell.LEFT);
 		pdfPTable.getDefaultCell().disableBorderSide(PdfPCell.RIGHT);
 		pdfPTable.getDefaultCell().disableBorderSide(PdfPCell.BOTTOM);
 		pdfPTable.getDefaultCell().setBorderWidth(15f);
 
 		Font bigFont = getFont();
 		bigFont.setSize(20f);
 		Chunk titleChunk = new Chunk(document.getDocumentTitle(), bigFont);
 
 
 		Font smallFont = getFont();
 		smallFont.setSize(12f);
 		StringBuffer buffer = new StringBuffer();
 		String[] creatorsFromDC = DCUtils.creatorsFromDC(fedoraAccess.getDC(document.getUuidMainTitle()));
 		for (String string : creatorsFromDC) {
 			buffer.append(string).append('\n');
 		}
 		Chunk creatorsChunk = new Chunk(buffer.toString(), smallFont);
 		pdfPTable.addCell(new Paragraph(titleChunk));
 		pdfPTable.addCell(new Paragraph(creatorsChunk));
 		
 		return pdfPTable;
 	}
 
 	public void insertFirstPage(AbstractRenderedDocument model, String parentUuid, String titlePageUuid , PdfWriter pdfWriter, Document pdfDoc, String djvuUrl) throws IOException, DocumentException {
 		try {
 			Paragraph paragraph = new Paragraph();
 			paragraph.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
 			
 			paragraph.add(DEFAULT_LOGO_IMAGE);
 			pdfDoc.add(paragraph);
 			pdfDoc.add(new Paragraph(" "));
 			
 			PdfPTable pdfPTable = new PdfPTable(new float[] {0.2f, 0.8f});
 			pdfPTable.setSpacingBefore(3f);
 
 			pdfPTable.getDefaultCell().disableBorderSide(PdfPCell.TOP);
 			pdfPTable.getDefaultCell().disableBorderSide(PdfPCell.LEFT);
 			pdfPTable.getDefaultCell().disableBorderSide(PdfPCell.RIGHT);
 			pdfPTable.getDefaultCell().disableBorderSide(PdfPCell.BOTTOM);
 			pdfPTable.getDefaultCell().setBorderWidth(15f);
 
 			
 			insertTitleImage(pdfPTable,model, djvuUrl);
 			pdfPTable.addCell(insertTitleAndAuthors(model));
 			
 			final float[] mheights = new float[2];
 			pdfPTable.setTableEvent(new PdfPTableEvent() {
 				@Override
 				public void tableLayout(PdfPTable arg0, float[][] widths, float[] heights, int arg3, int rowStart, PdfContentByte[] arg5) {
 					mheights[0] = heights[0];
 					mheights[1] = heights[1];
 				}
 			});
 			pdfDoc.add(pdfPTable);
 			
 			lineInFirstPage(pdfWriter, pdfDoc, mheights[1]);
 
 			pdfDoc.add(new Paragraph(" "));
 			pdfDoc.add(new Paragraph(" "));
 			
 			Paragraph parDesc = new Paragraph(this.textsService.getText("first_page", localeProvider.get()), getFont());		
 			pdfDoc.add(parDesc);
 		} catch (XPathExpressionException e) {
 			LOGGER.log(Level.SEVERE, e.getMessage(), e);
 		}
 	}
 	
 
 	private  void lineInFirstPage(PdfWriter pdfWriter, Document document,
 			float y) {
 		PdfContentByte cb = pdfWriter.getDirectContent();
 
 		cb.moveTo(5f, y-10);
 		cb.lineTo(document.getPageSize().getWidth()-10, y - 10);
 		cb.stroke();
 	}
 
 
 	private File prepareXSLStyleSheet(Locale locale, String i18nUrl, String title, KrameriusModels model) throws IOException {
 		File tmpFile = File.createTempFile("temporary", "stylesheet");
 		tmpFile.deleteOnExit();
 		FileOutputStream fos = null;
 		try {
 			String localizedXslt = STUtils.localizedXslt(locale, i18nUrl, templatesFolder(), title, model);
 			fos = new FileOutputStream(tmpFile);
 			fos.write(localizedXslt.getBytes(Charset.forName("UTF-8")));
 			fos.close();
 		} finally {
 			if (fos != null) fos.close();
 		}
 		return tmpFile;
 	}
 	
 	public String xslt(FedoraAccess fa, File styleSheet, String uuid) throws IOException, TransformerException {
 		TransformerFactory tf = TransformerFactory.newInstance();
 		Transformer transformer = tf.newTransformer(new StreamSource(styleSheet));
 		org.w3c.dom.Document biblioMods = fa.getBiblioMods(uuid);
 		ByteArrayOutputStream bos = new ByteArrayOutputStream();
 		transformer.transform(new DOMSource(biblioMods), new StreamResult(bos));
 		return new String(bos.toByteArray(), Charset.forName("UTF-8"));
 	}
 	
 	
 	
 	public void insertOutlinedTextPage(TextPage page, PdfWriter pdfWriter, Document document, String title, String i18nUrl) throws XPathExpressionException, IOException, DocumentException, TransformerException {
 		File styleSheet = prepareXSLStyleSheet(localeProvider.get(), i18nUrl, title, page.getModel());
 		String text = xslt(this.fedoraAccess, styleSheet, page.getUuid());
 		System.out.println("Styled text '"+text+"'");
 		
 		
 		BufferedReader strReader = new BufferedReader(new StringReader(text));
 		StringBuffer oneChunkBuffer = new StringBuffer();
 		String line = null;
 		while((line = strReader.readLine()) != null) {
 			SimpleTextCommands command = SimpleTextCommands.findCommand(line);
 			if (command == SimpleTextCommands.LINE) {
 				insertPara(page, pdfWriter, document, oneChunkBuffer.toString(), getFont());
 				document.add(new Paragraph(" "));
 				VerticalPositionMark vsep = new LineSeparator();
 				document.add(vsep);
 				document.add(new Paragraph(" "));
 				
 				
 				oneChunkBuffer = new StringBuffer();
 				
 			} else if (command == SimpleTextCommands.PARA) {
 				insertPara(page, pdfWriter, document, oneChunkBuffer.toString(),getFont());
 				oneChunkBuffer = new StringBuffer();
 			} else if (command == SimpleTextCommands.FONT) {
 				oneChunkBuffer.append(line);
 				Font font = getFont();
 				String oneChunkText = oneChunkBuffer.toString();
 				Map<String, String> parameters = command.parameters(oneChunkText);
 				if (parameters.containsKey("size")) {
 					font.setSize(Integer.parseInt(parameters.get("size")));
 				}
 				insertPara(page, pdfWriter, document, oneChunkText.substring(0, command.indexStart(oneChunkText)), font);
 				oneChunkBuffer = new StringBuffer();
 			} else {
 				oneChunkBuffer.append(line).append("\n");
 			}
 		}
 		
 		if (oneChunkBuffer.length() > 0 ) {
 			insertPara(page, pdfWriter, document, oneChunkBuffer.toString(), getFont());
 		}
 	}
 
 
 	private void insertPara(TextPage page, PdfWriter pdfWriter,
 			Document document, String text, Font font)
 			throws DocumentException, IOException {
 
 		Chunk chunk = new Chunk(text, font);
 		chunk.setLocalDestination(page.getOutlineDestination());
 		pdfWriter.setOpenAction(page.getOutlineDestination());
 
 		Paragraph para = new Paragraph(chunk);
 		document.add(para);
 	}
 
 	public void insertOutlinedImagePage(ImagePage page, PdfWriter pdfWriter, Document document, String djvuUrl) throws XPathExpressionException, IOException, DocumentException {
 		String pageNumber = page.getPageNumber();
 		insertImage(page.getUuid(), pdfWriter, document, 0.7f,djvuUrl);
 		
 		Font font = getFont();
 		Chunk chunk = new Chunk(pageNumber);
 		chunk.setLocalDestination(page.getOutlineDestination());
 		float fontSize = chunk.getFont().getCalculatedSize();
 		float chwidth = chunk.getWidthPoint();
 		int choffsetx = (int) ((document.getPageSize().getWidth() - chwidth) / 2);
 		int choffsety = (int) ( 10 - fontSize);
 
 		PdfContentByte cb = pdfWriter.getDirectContent();
 		cb.saveState();
 		cb.beginText();
 		cb.localDestination(page.getOutlineDestination(), new PdfDestination(PdfDestination.FIT));
 		pdfWriter.setOpenAction(page.getOutlineDestination());
 		
 		cb.setFontAndSize(font.getBaseFont(), 14f);
 		cb.showTextAligned(com.lowagie.text.Element.ALIGN_LEFT, pageNumber,choffsetx, choffsety + 10, 0);
 		cb.endText();
 		cb.restoreState();
 	}
 
 	private Font getFont() throws DocumentException, IOException {
		BaseFont bf = BaseFont.createFont("Helvetica", BaseFont.CP1250,BaseFont.EMBEDDED);
 		return new Font(bf);
 	}
 
 	
 //	public void insertImagePage(String uuid, PdfWriter pdfWriter, Document document) throws XPathExpressionException, IOException, DocumentException { 
 //		insertImage(uuid, pdfWriter, document, 1.0f);
 //	}
 	
 	public void insertTitleImage(PdfPTable pdfPTable, AbstractRenderedDocument model, String djvuUrl) throws IOException, BadElementException, XPathExpressionException {
 		String imgUrl = createIMGFULL(model.getUuidTitlePage(), djvuUrl);
 		try {
 			String uuidToFirstPage = null;
 			if (fedoraAccess.isImageFULLAvailable(model.getUuidTitlePage())) {
 				uuidToFirstPage = model.getUuidTitlePage();
 			}
 			if ((uuidToFirstPage == null) && (fedoraAccess.isImageFULLAvailable(model.getUuidFrontCover()))) {
 				uuidToFirstPage = model.getUuidFrontCover();
 				
 			}
 			if ((uuidToFirstPage == null) && (fedoraAccess.isImageFULLAvailable(model.getFirstPage()))) {
 				uuidToFirstPage = model.getFirstPage();
 				
 			}
 			if (uuidToFirstPage != null) {
 				String mimetypeString = fedoraAccess.getImageFULLMimeType(uuidToFirstPage);
 				ImageMimeType mimetype = ImageMimeType.loadFromMimeType(mimetypeString);
 				if (mimetype != null) {
 					float smallImage = 0.2f;
 					Image javaImg = readImage(new URL(imgUrl), mimetype);
 					ByteArrayOutputStream bos = new ByteArrayOutputStream();
 					writeImageToStream(javaImg, "jpeg", bos);
 
 					com.lowagie.text.Image img = com.lowagie.text.Image.getInstance(bos.toByteArray());
 					
 					img.scaleAbsoluteHeight(smallImage * img.getHeight());
 					img.scaleAbsoluteWidth(smallImage * img.getWidth());
 					pdfPTable.addCell(img);
 				}
 			} else {
 				pdfPTable.addCell(" - ");
 			}
 		} catch (cz.incad.kramerius.security.SecurityException e) {
 			LOGGER.log(Level.SEVERE, e.getMessage(), e);
 			pdfPTable.addCell(" - ");
 		}
 	}
 	
 	public void insertImage(String uuid, PdfWriter pdfWriter , Document document, float percentage, String djvuUrl) throws XPathExpressionException, IOException, DocumentException {
 		try {
 			if (fedoraAccess.isImageFULLAvailable(uuid)) {
 				//bypass 
 				String imgUrl = createIMGFULL(uuid, djvuUrl);
 				String mimetypeString = fedoraAccess.getImageFULLMimeType(uuid);
 				ImageMimeType mimetype = ImageMimeType.loadFromMimeType(mimetypeString);
 				if (mimetype != null) {
 					Image javaImg = readImage(new URL(imgUrl), mimetype);
 					ByteArrayOutputStream bos = new ByteArrayOutputStream();
 					writeImageToStream(javaImg, "jpeg", bos);
 
 					com.lowagie.text.Image img = com.lowagie.text.Image.getInstance(bos.toByteArray());
 
 					Float wratio = document.getPageSize().getWidth()/ javaImg.getWidth(null);
 					Float hratio = document.getPageSize().getHeight()/ javaImg.getHeight(null);
 					Float ratio = Math.min(wratio, hratio);
 					if (percentage != 1.0) { ratio = ratio * percentage; }
 					
 					int fitToPageWidth = (int) (javaImg.getWidth(null) * ratio);
 					int fitToPageHeight = (int) (javaImg.getHeight(null) * ratio);
 					
 					int offsetX = ((int)document.getPageSize().getWidth() - fitToPageWidth) / 2;
 					int offsetY = ((int)document.getPageSize().getHeight() - fitToPageHeight) / 2;
 
 					img.scaleAbsoluteHeight(ratio * img.getHeight());
 					
 					img.scaleAbsoluteWidth(ratio * img.getWidth());
 					img.setAbsolutePosition((offsetX), document.getPageSize().getHeight() - offsetY - (ratio * img.getHeight()));
 					document.add(img);
 				}
 			} else {
 				Paragraph na = new Paragraph(textsService.getText("image_not_available", localeProvider.get()));
 				document.add(na);
 			}
 		} catch (cz.incad.kramerius.security.SecurityException e) {
 			LOGGER.log(Level.SEVERE, e.getMessage(), e);
 			Paragraph na = new Paragraph(textsService.getText("security_fail", localeProvider.get()));
 			document.add(na);
 		}
 	}
 	
 
 
 	
 	/**
 	 * Bypass url
 	 * @param objectId
 	 * @return
 	 */
 	private String createIMGFULL(String objectId, String djvuUrl) {
 		String imgUrl = djvuUrl +"?uuid="+objectId+"&outputFormat=RAW";
 		return imgUrl;
 	}
 
 	@Override
 	public File templatesFolder() {
 		String dirName = Constants.WORKING_DIR + File.separator + "templates";
 		File dir = new File(dirName);
 		if (!dir.exists()) { 
 			boolean mkdirs = dir.mkdirs();
 			if (!mkdirs) throw new RuntimeException("cannot create folder '"+dir.getAbsolutePath()+"'");
 		}
 		return dir;
 	}
 
 	
 }
