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
 package org.pathvisio.util;
 
 import java.io.File;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.List;
 
 import org.pathvisio.debug.Logger;
 import org.pathvisio.model.DataSource;
 import org.pathvisio.model.XrefWithSymbol;
 import org.xml.sax.Attributes;
 import org.xml.sax.SAXException;
 import org.xml.sax.SAXParseException;
 import org.xml.sax.XMLReader;
 import org.xml.sax.helpers.DefaultHandler;
 
 /**
  * This sax handler can be used to quickly parse pathway information from
  * a gpml file
  */
 public class PathwayParser extends DefaultHandler 
 {
 	public static class ParseException extends Exception
 	{
 		private static final long serialVersionUID = 1L;
 		
 		public ParseException (Exception e)
 		{
 			super (e);
 		}
 	}
 	
 	String name;
 	private ArrayList<XrefWithSymbol> genes;
 	
 	public PathwayParser() 
 	{
 		name = "";
 		genes = new ArrayList<XrefWithSymbol>();
 	}
 	
 	public PathwayParser(File f, XMLReader xmlReader) throws ParseException
 	{
 		this();
 		xmlReader.setContentHandler(this);
 		xmlReader.setEntityResolver(this);
 		
 		try
 		{
			xmlReader.parse(f.getAbsolutePath());
 		}
 		catch (IOException e) 
 		{ 
 			throw new ParseException (e);
 		}
 		catch (SAXException e)
 		{
 			throw new ParseException (e);
 			// ignore pathways that generate an exception (return empty list)
 		}
 	}
 		
 	public List<XrefWithSymbol> getGenes() { return genes; }
 	
 	public String getName() { return name; }
 	
 	XrefWithSymbol currentGene = null;
 	
 	public void startElement(String uri, String localName, String qName, Attributes attributes)
 			throws SAXException 
 	{
 		if(localName.equals("DataNode")) 
 		{ 
 			// the only way this can be not null
 			// is when two consecutive DataNode opening tags don't have an Xref in between
 			assert (currentGene != null);				
 			currentGene = new XrefWithSymbol(null, null, null);
 
 			String symbol = attributes.getValue("TextLabel");
 			currentGene.setSymbol(symbol);		
 		}
 		else if(localName.equals("Pathway")) 
 		{
 			name = attributes.getValue("Name");
 		}
 		else if(localName.equals("Xref"))
 		{
 			String sysName = attributes.getValue("Database");
 			assert (sysName != null);
 			DataSource ds = DataSource.getByFullName (sysName);
 			String geneId = attributes.getValue("ID");
 			assert (geneId != null);
 			
 			currentGene.setDataSource(ds);
 			currentGene.setId(geneId);
 			
 			if(!genes.contains(currentGene)) //Don't add duplicate genes
 				genes.add(currentGene);
 			currentGene = null;
 		}
 	}
 	
 	public void error(SAXParseException e) 
 	{ 
 		Logger.log.error("Error while parsing xml document", e);
 	}
 	
 	public void fatalError(SAXParseException e) throws SAXParseException 
 	{ 
 		Logger.log.error("Fatal error while parsing xml document", e);
 		throw new SAXParseException("Fatal error, parsing of this document aborted", null);
 	}
 	
 	public void warning(SAXParseException e) 
 	{ 
 		Logger.log.error("Warning while parsing xml document", e);
 	}	
 }
 
