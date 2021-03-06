 // PathVisio,
 // a tool for data visualization and analysis using Biological Pathways
 // Copyright 2006-2007 BiGCaT Bioinformatics
 //
 // Licensed under the Apache License, Version 2.0 (the "License"); 
 // you may not use this file except in compliance with the License. 
 // You may obtain a copy of the License at 
 // 
 // http://www.apache.org/licenses/LICENSE-2.0 
 //  
 // Unless required by applicable law or agreed to in writing, software 
 // distributed under the License is distributed on an "AS IS" BASIS, 
 // WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 // See the License for the specific language governing permissions and 
 // limitations under the License.
 //
 package org.pathvisio.gui;
 
 import java.io.BufferedReader;
 import java.io.InputStreamReader;
 import java.util.List;
 
 import org.eclipse.swt.browser.Browser;
 import org.eclipse.swt.layout.FillLayout;
 import org.eclipse.swt.widgets.Composite;
 import org.pathvisio.data.DataSources;
 import org.pathvisio.data.Gdb;
 import org.pathvisio.data.Gex;
 import org.pathvisio.data.Gdb.IdCodePair;
import org.pathvisio.util.Utils;
import org.pathvisio.view.GeneProduct;
import org.pathvisio.view.SelectionBox;
import org.pathvisio.view.VPathwayElement;
import org.pathvisio.view.SelectionBox.SelectionEvent;
import org.pathvisio.view.SelectionBox.SelectionListener;
 
 /**
  * Backpage browser - side panel that shows the backpage information when a GeneProduct is double-clicked
  */
 public class BackpagePanel extends Composite implements SelectionListener {
 	/**
 	 * Directory containing HTML files needed to display the backpage information
 	 */
 	final static String BPDIR = "backpage";
 	/**
 	 * Header file, containing style information
 	 */
 	final static String HEADERFILE = "header.html";
 	
 	/**
 	 * Header for the gene information in HTML format
 	 */
 	final static String bpHeader = "<H1>Gene information</H1><P>";
 	/**
 	 * Header for the expression information in HTML format
 	 */
 	final static String gexHeader = "<H1>Expression data</H1><P>";
 	
 	private String bpText;
 	private String gexText;
 	private String header;
 	
 	private Browser bpBrowser;
 	
 	private GeneProduct geneProduct;
 	
 	/**
 	 * Constructor for this class
 	 * @param parent	Parent {@link Composite} for the Browser widget
 	 * @param style		Style for the Browser widget
 	 */
 	public BackpagePanel(Composite parent, int style) {
 		super(parent, style);
 		
 		initializeHeader(); //Load the header including style information
 		setLayout(new FillLayout());
 		bpBrowser = new Browser(this, style); //Set the Browser widget
 		setGeneText(null);
 		setGexText(null);
 		
 		SelectionBox.addListener(this);
 	}
 	
 	public void setGeneProduct(final GeneProduct gp) 
 	{ 
 		if(geneProduct == gp) return;
 		
 		Thread fetchThread = new Thread() {
 			public void run() {
 				geneProduct = gp;
 				if(gp == null) {
 					setGeneText(null);
 					setGexText(null);
 					return;
 				}
 				// Get the backpage text
 				String geneHeader = geneProduct.getGmmlData().getBackpageHead();
 				if (geneHeader == null) geneHeader = "";
 				String geneId = geneProduct.getGmmlData().getGeneID();
 				String systemCode = geneProduct.getGmmlData().getSystemCode();
 				String bpText = geneHeader.equals("") ? geneHeader : "<H2>" + geneHeader + "</H2><P>";
 				String bpInfo = Gdb.getBpInfo(geneId, systemCode);
 				bpText += bpInfo == null ? "<I>No gene information found</I>" : bpInfo;
 				String crossRefText = getCrossRefText(geneId, systemCode);
 				String gexText = Gex.getDataString(new IdCodePair(geneId, systemCode));
 				if (bpText != null) 	setGeneText(bpText);
 				if (gexText != null)	setGexText(gexText + crossRefText);
 				else 					setGexText("<I>No expression data found</I>");
 			}
 		};
 		
 		//Run in seperate thread so that this method can return
 		fetchThread.start();
 	}
 		
 	public String getCrossRefText(String id, String code) {
 		List<IdCodePair> crfs = Gdb.getCrossRefs(id, code);
 		if(crfs.size() == 0) return "";
 		StringBuilder crt = new StringBuilder("<H1>Cross references</H1><P>");
 		for(IdCodePair cr : crfs) {
 			String idtxt = cr.getId();
 			String url = getCrossRefLink(cr);
			if(url != null) {
				int os = Utils.getOS();
				if(os == Utils.OS_WINDOWS) {
					//In windows: open in new browser window
					idtxt = "<a href='" + url + "' target='_blank'>" + idtxt + "</a>";
				} else {
					//This doesn't work under ubuntu, so no new windoe there
					idtxt = "<a href='" + url + "'>" + idtxt + "</a>";
				}
				
			}
 			String dbName = DataSources.sysCode2Name.get(cr.getCode());
 			crt.append( idtxt + ", " + (dbName != null ? dbName : cr.getCode()) + "<br>");
 		}
 		return crt.toString();
 	}
 	
 	String getCrossRefLink(IdCodePair idc) {
 		String c = idc.getCode();
 		String id = idc.getId();
 		if(c.equalsIgnoreCase("En"))
 			return "http://www.ensembl.org/Homo_sapiens/searchview?species=all&idx=Gene&q=" + id;
 		if(c.equalsIgnoreCase("P"))
 			return "http://www.expasy.org/uniprot/" + id;
 		if(c.equalsIgnoreCase("Q")) {
 			String pre = "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?";
 			if(id.startsWith("NM")) {
 				return pre + "db=Nucleotide&cmd=Search&term=" + id;
 			} else {
 				return pre + "db=Protein&cmd=search&term=" + id;
 			}
 		}
 		if(c.equalsIgnoreCase("T"))
 			return "http://godatabase.org/cgi-bin/go.cgi?view=details&search_constraint=terms&depth=0&query=" + id;
 		if(c.equalsIgnoreCase("I"))
 			return "http://www.ebi.ac.uk/interpro/IEntry?ac=" + id;
 		if(c.equalsIgnoreCase("Pd"))
 			return "http://bip.weizmann.ac.il/oca-bin/ocashort?id=" + id;
 		if(c.equalsIgnoreCase("X"))
 			return "http://www.ensembl.org/Homo_sapiens/featureview?type=OligoProbe;id=" + id;
 		if(c.equalsIgnoreCase("Em"))
 			return "http://www.ebi.ac.uk/cgi-bin/emblfetch?style=html&id=" + id;
 		if(c.equalsIgnoreCase("L"))
 			return "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gene&cmd=Retrieve&dopt=full_report&list_uids=" + id;
 		if(c.equalsIgnoreCase("H"))
 			return "http://www.gene.ucl.ac.uk/cgi-bin/nomenclature/get_data.pl?hgnc_id=" + id;
 		if(c.equalsIgnoreCase("I"))
 			return "http://www.ebi.ac.uk/interpro/IEntry?ac=" + id;
 		if(c.equalsIgnoreCase("M"))
 			return "http://www.informatics.jax.org/searches/accession_report.cgi?id=" + id;
 		if(c.equalsIgnoreCase("Om"))
 			return "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=OMIM&cmd=Search&doptcmdl=Detailed&term=?" + id;
 		if(c.equalsIgnoreCase("Pf"))
 			return "http://www.sanger.ac.uk//cgi-bin/Pfam/getacc?" + id;
 		if(c.equalsIgnoreCase("R"))
 			return "http://rgd.mcw.edu/generalSearch/RgdSearch.jsp?quickSearch=1&searchKeyword=" + id;
 		if(c.equalsIgnoreCase("D"))
 			return "http://db.yeastgenome.org/cgi-bin/locus.pl?locus=" + id;
 		if(c.equalsIgnoreCase("S"))
 			return "http://www.expasy.org/uniprot/" + id;
 		if(c.equalsIgnoreCase("U")) {
 			String [] org_nr = id.split("\\.");
 			if(org_nr.length == 2) {
 				return "http://www.ncbi.nlm.nih.gov/UniGene/clust.cgi?ORG=" + 
 				org_nr[0] + "&CID=" + org_nr[1];
 			}
 			else {
 				return null;
 			}
 		}
 		if (c.equalsIgnoreCase("Nw"))
 		{
 			return "http://nugowiki.org/index.php/" + id;
 		}
 		if (c.equalsIgnoreCase("Ca"))
 		{
 			return "http://chem.sis.nlm.nih.gov/chemidplus/direct.jsp?regno=" + id;
 		}
 		if (c.equalsIgnoreCase("Cp"))
 		{
 			return "http://pubchem.ncbi.nlm.nih.gov/summary/summary.cgi?cid=" + id;
 		}
 		if (c.equalsIgnoreCase("Ce"))
 		{
 			return "http://www.ebi.ac.uk/chebi/searchId=CHEBI:" + id;
 		}
 		if (c.equalsIgnoreCase("Ch"))
 		{
 			return "http://www.hmdb.ca/scripts/show_card.cgi?METABOCARD=" + id + ".txt";
 		}
 		if (c.equalsIgnoreCase("Ck"))
 		{
 			return "http://www.genome.jp/dbget-bin/www_bget?cpd:" + id;
 		}
 		return null;
 	}
 	
 	/**
 	 * Sets the text for the Gene information part of the browser. Will be prepended by a paragraph
 	 * header as defined in bpHeader
 	 * @param bpText	Text to display in HTML format
 	 */
 	public void setGeneText(String bpText) {
 		if(bpText == null) { //In case no information has to be displayed
 			this.bpText = bpHeader + "<I>No gene selected</I>";
 		} else {
 			this.bpText = bpHeader + bpText;
 		}
 		refresh();
 	}
 	
 	/**
 	 * Sets the text for the expression part of the browser. Will be prepended by a paragraph
 	 * header as defined in gexHeader
 	 * @param gexText	Text to display in HTML format
 	 */
 	public void setGexText(String gexText) {
 		if(gexText != null) { //In case no information has to be displayed
 			this.gexText = gexHeader + gexText;
 		} else {
 			this.gexText = "";
 		}
 		refresh();
 	}
 	
 	/**
 	 * Refreshes the text displayed in the browser
 	 */
 	public void refresh() {
 		getDisplay().asyncExec(new Runnable() {
 			public void run() {
 				bpBrowser.setText(header + bpText + gexText);	
 			}
 		});
 	}
 	
 	/**
 	 * Reads the header of the HTML content displayed in the browser. This header is displayed in the
 	 * file specified in the {@link HEADERFILE} field
 	 */
 	private void initializeHeader() {
 		try {
 			BufferedReader input = new BufferedReader(new InputStreamReader(
 						Engine.getResourceURL(BPDIR + "/" + HEADERFILE).openStream()));
 			String line;
 			header = "";
 			while((line = input.readLine()) != null) {
 				header += line.trim();
 			}
 		} catch (Exception e) {
 			Engine.log.error("Unable to read header file for backpage browser: " + e.getMessage(), e);
 		}
 	}
 
 	public void drawingEvent(SelectionEvent e) {
 		switch(e.type) {
 		case SelectionEvent.OBJECT_ADDED:
 			//Just take the first GeneProduct in the selection
 			for(VPathwayElement o : e.selection) {
 				if(o instanceof GeneProduct) {
 					if(geneProduct != o) setGeneProduct((GeneProduct)o);
 					break; //Selects the first, TODO: use setGmmlDataObjects
 				}
 			}
 			break;
 		case SelectionEvent.OBJECT_REMOVED:
 			if(e.selection.size() != 0) break;
 		case SelectionEvent.SELECTION_CLEARED:
 			setGeneProduct(null);
 			break;
 		}
 	}
 }
