 /*******************************************************************************
  * $URL$
  * $Id$
  * **********************************************************************************
  *
  * Copyright (c) 2006 The Sakai Foundation.
  *
  * Licensed under the Educational Community License, Version 1.0 (the
  * "License"); you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.opensource.org/licenses/ecl1.php
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  * License for the specific language governing permissions and limitations under
  * the License.
  *
  ******************************************************************************/
 
 package org.sakaiproject.citation.impl;
 
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
 import java.io.OutputStreamWriter;
 import java.io.UnsupportedEncodingException;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.HashSet;
 import java.util.Hashtable;
 import java.util.Iterator;
 import java.util.List;
 import java.util.ListIterator;
 import java.util.Map;
 import java.util.NoSuchElementException;
 import java.util.Set;
 import java.util.SortedSet;
 import java.util.Stack;
 import java.util.TreeSet;
 import java.util.Vector;
 import java.net.URLEncoder;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.osid.repository.Asset;
 import org.osid.repository.Part;
 import org.osid.repository.PartIterator;
 import org.osid.repository.Record;
 import org.osid.repository.RecordIterator;
 import org.osid.repository.RepositoryException;
 import org.sakaiproject.citation.api.ActiveSearch;
 import org.sakaiproject.citation.api.Citation;
 import org.sakaiproject.citation.api.CitationCollection;
 import org.sakaiproject.citation.api.CitationIterator;
 import org.sakaiproject.citation.api.CitationService;
 import org.sakaiproject.citation.api.ConfigurationService;
 import org.sakaiproject.citation.api.Schema;
 import org.sakaiproject.citation.api.Schema.Field;
 import org.sakaiproject.component.api.ServerConfigurationService;
 import org.sakaiproject.component.cover.ComponentManager;
 import org.sakaiproject.content.api.ContentEntity;
 import org.sakaiproject.content.api.ContentHostingService;
 import org.sakaiproject.content.api.ContentResource;
 import org.sakaiproject.content.api.InteractionAction;
 import org.sakaiproject.content.api.ResourceTypeRegistry;
 import org.sakaiproject.content.api.ResourceToolAction;
 import org.sakaiproject.content.api.ResourceToolAction.ActionType;
 import org.sakaiproject.content.util.BaseInteractionAction;
 import org.sakaiproject.content.util.BaseResourceAction;
 //import org.sakaiproject.content.util.BaseResourceAction.Localizer;
 import org.sakaiproject.content.util.BaseServiceLevelAction;
 import org.sakaiproject.content.util.BasicResourceType;
 //import org.sakaiproject.content.util.BasicResourceType.Localizer;
 
 import org.sakaiproject.entity.api.Entity;
 import org.sakaiproject.entity.api.EntityManager;
 import org.sakaiproject.entity.api.HttpAccess;
 import org.sakaiproject.entity.api.Reference;
 import org.sakaiproject.entity.api.ResourceProperties;
 import org.sakaiproject.exception.IdUnusedException;
 import org.sakaiproject.exception.PermissionException;
 import org.sakaiproject.exception.ServerOverloadException;
 import org.sakaiproject.exception.TypeException;
 import org.sakaiproject.id.cover.IdManager;
 import org.sakaiproject.javax.Filter;
 import org.sakaiproject.tool.api.SessionManager;
 import org.sakaiproject.user.api.User;
 import org.sakaiproject.user.api.UserDirectoryService;
 import org.sakaiproject.util.ResourceLoader;
 import org.sakaiproject.util.StringUtil;
 import org.w3c.dom.Document;
 import org.w3c.dom.Element;
 
 /**
  *
  *
  */
 public abstract class BaseCitationService implements CitationService
 {
 	protected boolean attemptToMatchSchema = false;
 	
 	protected static final List AUTHOR_AS_KEY = new Vector();
 	static 
 	{ 
 		AUTHOR_AS_KEY.add( CitationCollection.SORT_BY_AUTHOR ); 
 	};
 
 	protected static final List TITLE_AS_KEY = new Vector();
 
 	static 
 	{ 
 		TITLE_AS_KEY.add( CitationCollection.SORT_BY_TITLE ); 
 	};
 
 	public static final Map<String, String> GS_TAGS = new Hashtable<String, String>();
 	static
 	{
 		//GS_TAGS.put("rft_val_fmt", "genre");
 		GS_TAGS.put("rft.title", "title");
 		GS_TAGS.put("rft.atitle", "title");
 		GS_TAGS.put("rft.jtitle", "atitle");
 		GS_TAGS.put("rft.btitle", "atitle");
 		GS_TAGS.put("rft.aulast", "au");
 		GS_TAGS.put("rft.aufirst", "au");
 		GS_TAGS.put("rft.au", "au");
 		GS_TAGS.put("rft.pub", "publisher");
 		GS_TAGS.put("rft.volume", "volume");
 		GS_TAGS.put("rft.issue", "issue");
 		GS_TAGS.put("rft.pages", "pages");
 		GS_TAGS.put("rft.date", "date");
 		GS_TAGS.put("rft.issn", "id");
 		GS_TAGS.put("rft.isbn", "id");
 	}
 	
 	/**
 	 *
 	 */
 	public class BasicCitation implements Citation
 	{
 		/* for OpenUrl creation */
 		protected final static String OPENURL_VERSION = "Z39.88-2004";
 		protected final static String OPENURL_CONTEXT_FORMAT = "info:ofi/fmt:kev:mtx:ctx";
 		protected final static String OPENURL_JOURNAL_FORMAT = "info:ofi/fmt:kev:mtx:journal";
 		protected final static String OPENURL_BOOK_FORMAT = "info:ofi/fmt:kev:mtx:book";
 
 		protected Map m_citationProperties = null;
 		protected Map m_urls;
 		protected String m_citationUrl = null;
 		protected String m_displayName = null;
 		protected String m_fullTextUrl = null;
 		protected String m_id = null;
 		protected String m_imageUrl = null;
 		protected Schema m_schema;
 		protected String m_searchSourceUrl = null;
 		protected Integer m_serialNumber = null;
 		protected boolean m_temporary = false;
 		protected boolean m_isAdded = false;
 
 		/**
 		 * Constructs a temporary citation.
 		 */
 		protected BasicCitation()
 		{
 			m_serialNumber = nextSerialNumber();
 			m_temporary = true;
 			m_citationProperties = new Hashtable();
 			m_urls = new Hashtable();
 			setType(UNKNOWN_TYPE);
 		}
 
 		/**
 		 * Constructs a temporary citation based on an asset.
 		 *
 		 * @param asset
 		 */
 		protected BasicCitation(Asset asset)
 		{
 			m_serialNumber = nextSerialNumber();
 			m_temporary = true;
 			m_citationProperties = new Hashtable();
 			m_urls = new Hashtable();
 
 			boolean unknownSchema = true;
 
 			Set validProperties = getValidPropertyNames();
 			Set multivalued = getMultivalued();
 
 			String description;
 
 			// assetId = asset.getId().getIdString();
 			try
 			{
 				m_displayName = asset.getDisplayName();
 				if (this.m_displayName != null)
 				{
 					m_citationProperties.put(Schema.TITLE, this.m_displayName);
 				}
 
 				description = asset.getDescription();
 				if (description != null)
 				{
 					m_citationProperties.put("abstract", description);
 				}
 
 				RecordIterator rit = asset.getRecords();
 				try
 				{
 					while (rit.hasNextRecord())
 					{
 						Record record;
 						try
 						{
 							record = rit.nextRecord();
 
 							try
 							{
 								PartIterator pit = record.getParts();
 								try
 								{
 									while (pit.hasNextPart())
 									{
 										try
 										{
 											Part part = pit.nextPart();
 											String type = part.getPartStructure().getType()
 											        .getKeyword();
 											if (type == null)
 											{
 												// continue;
 											}
 											else if (validProperties.contains(type))
 											{
 
 												if (multivalued.contains(type))
 												{
 													List values = (List) m_citationProperties
 													        .get(type);
 													if (values == null)
 													{
 														values = new Vector();
 														m_citationProperties.put(type, values);
 													}
 													values.add(part.getValue());
 												}
 												else
 												{
 													m_citationProperties.put(type, part.getValue());
 												}
 											}
 											else if (type.equals("type"))
 											{
 												if (m_schema == null
 												        || m_schema.getIdentifier().equals(
 												                "unknown"))
 												{
 													if (getSynonyms("article").contains(
 													        part.getValue().toString()
 													                .toLowerCase()))
 													{
 														m_schema = BaseCitationService.this
 														        .getSchema("article");
 														unknownSchema = false;
 													}
 													else if (getSynonyms("book").contains(
 													        part.getValue().toString()
 													                .toLowerCase()))
 													{
 														m_schema = BaseCitationService.this
 														        .getSchema("book");
 														unknownSchema = false;
 													}
 													else if (getSynonyms("chapter").contains(
 													        part.getValue().toString()
 													                .toLowerCase()))
 													{
 														m_schema = BaseCitationService.this
 														        .getSchema("chapter");
 														unknownSchema = false;
 													}
 													else if (getSynonyms("report").contains(
 													        part.getValue().toString()
 													                .toLowerCase()))
 													{
 														m_schema = BaseCitationService.this
 														        .getSchema("report");
 														unknownSchema = false;
 													}
 													else
 													{
 														m_schema = BaseCitationService.this
 														        .getSchema("unknown");
 														unknownSchema = true;
 													}
 												}
 												List values = (List) m_citationProperties.get(type);
 												if (values == null)
 												{
 													values = new Vector();
 													m_citationProperties.put(type, values);
 												}
 												values.add(part.getValue());
 											}
 											else
 											{
 
 											}
 
 										}
 										catch (RepositoryException e)
 										{
 											M_log.warn("BasicCitation(" + asset + ") ", e);
 										}
 									}
 								}
 								catch (RepositoryException e)
 								{
 									M_log.warn("BasicCitation(" + asset + ") ", e);
 								}
 							}
 							catch (RepositoryException e1)
 							{
 								M_log.warn("BasicCitation(" + asset + ") ", e1);
 							}
 						}
 						catch (RepositoryException e2)
 						{
 							M_log.warn("BasicCitation(" + asset + ") ", e2);
 						}
 					}
 				}
 				catch (RepositoryException e)
 				{
 					M_log.warn("BasicCitation(" + asset + ") ", e);
 				}
 			}
 			catch (RepositoryException e)
 			{
 				M_log.warn("BasicCitation(" + asset + ") ", e);
 			}
 
 			if(unknownSchema && attemptToMatchSchema)
 			{
 				matchSchema();
 			}
 
 			setDefaults();
 		}
 
 		/**
          *
          */
         protected void matchSchema()
         {
         	Map pros = new Hashtable();
         	Map cons = new Hashtable();
 	        List schemas = getSchemas();
 	        Set fieldNames = this.m_citationProperties.keySet();
 	        Iterator schemaIt = schemas.iterator();
 	        while(schemaIt.hasNext())
 	        {
 	        	Schema schema = (Schema) schemaIt.next();
 	        	if(schema.getIdentifier().equals("unknown"))
 	        	{
 	        		continue;
 	        	}
 
 	        	pros.put(schema.getIdentifier(), new Counter());
 	        	cons.put(schema.getIdentifier(), new Counter());
 
 	        	Iterator fieldIt = fieldNames.iterator();
 	        	while(fieldIt.hasNext())
 	        	{
 	        		String fieldName = (String) fieldIt.next();
 	        		Field field = schema.getField(fieldName);
 	        		if(field == null)
 	        		{
 	        			// this indicates that data would be lost.
 	        			((Counter) cons.get(schema.getIdentifier())).increment();
 	        		}
 	        		else
 	        		{
 	        			// this is evidence that this schema might be best fit.
 	        			((Counter) pros.get(schema.getIdentifier())).increment();
 	        		}
 	        	}
 	        }
 
 	        // elminate schema that lose data
 	        Iterator consIt = cons.keySet().iterator();
 	        while(consIt.hasNext())
 	        {
 	        	String schemaId = (String) consIt.next();
 	        	boolean blocked = ((Counter) cons.get(schemaId)).intValue() > 0;
 	        	if(blocked)
 	        	{
 	        		pros.remove(schemaId);
 	        	}
 	        }
 	        Iterator prosIt = pros.keySet().iterator();
 	        int bestScore = 0;
 	        String bestMatch = null;
 	        // Nominate "article" as first candidate if it's not blocked
 	        Object article = pros.get("article");
 	        if(article != null)
 	        {
 	        	bestScore = ((Counter) article).intValue();
 	        	bestMatch = "article";
 	        }
 	        while(prosIt.hasNext())
 	        {
 	        	String schemaId = (String) prosIt.next();
 	        	int score = ((Counter) pros.get(schemaId)).intValue();
 	        	if(score > bestScore)
 	        	{
 	        		bestScore = score;
 	        		bestMatch = schemaId;
 	        	}
 	        }
 	        if(bestMatch != null)
 	        {
 	        	m_schema = BaseCitationService.this.getSchema(bestMatch);
 	        }
         }
 
 		/**
 		 * @param other
 		 */
 		public BasicCitation(BasicCitation other)
 		{
 			m_id = other.m_id;
 			m_serialNumber = other.m_serialNumber;
 			m_temporary = other.m_temporary;
 			m_citationProperties = new Hashtable();
 			m_urls = new Hashtable();
 			setSchema(other.m_schema);
 
 			copy(other);
 		}
 
 		/**
 		 * Construct a citation not marked as temporary of a particular type.
 		 *
 		 * @param mediatype
 		 */
 		public BasicCitation(String mediatype)
 		{
 			m_id = IdManager.createUuid();
 			m_citationProperties = new Hashtable();
 			m_urls = new Hashtable();
 			setType(mediatype);
 		}
 
 		public BasicCitation(String citationId, Schema schema)
 		{
 			m_id = citationId;
 			m_citationProperties = new Hashtable();
 			m_urls = new Hashtable();
 			setSchema(schema);
 		}
 
 		/**
 		 * Construct a citation not marked as temporary of a particular type
 		 * with a particular id.
 		 *
 		 * @param citationId
 		 * @param mediatype
 		 */
 		public BasicCitation(String citationId, String mediatype)
 		{
 			m_id = citationId;
 			m_citationProperties = new Hashtable();
 			m_urls = new Hashtable();
 			setType(mediatype);
 		}
 
 		/*
 		 * (non-Javadoc)
 		 *
 		 * @see org.sakaiproject.citation.api.Citation#addUrl(java.lang.String,
 		 *      java.net.URL)
 		 */
 		public void addCustomUrl(String label, String url)
 		{
 			UrlWrapper wrapper = new UrlWrapper(label, url);
 			String id = IdManager.createUuid();
 			m_urls.put(id, wrapper);
 		}
 
 		/*
 		 * (non-Javadoc)
 		 *
 		 * @see org.sakaiproject.citation.api.Citation#addPropertyValue(java.lang.String,
 		 *      java.lang.Object)
 		 */
 		public void addPropertyValue(String name, Object value)
 		{
 			if (this.m_citationProperties == null)
 			{
 				this.m_citationProperties = new Hashtable();
 			}
 			if (isMultivalued(name))
 			{
 				List list = (List) this.m_citationProperties.get(name);
 				if (list == null)
 				{
 					list = new Vector();
 					this.m_citationProperties.put(name, list);
 				}
 				list.add(value);
 			}
 			else
 			{
 				this.m_citationProperties.put(name, value);
 			}
 		}
 
 		/**
 		 *
 		 * @param citation
 		 */
 		public void copy(Citation citation)
 		{
 			BasicCitation other = (BasicCitation) citation;
 
 			m_citationUrl = other.m_citationUrl;
 			m_displayName = other.m_displayName;
 			m_fullTextUrl = other.m_fullTextUrl;
 			m_imageUrl = other.m_imageUrl;
 			m_searchSourceUrl = other.m_searchSourceUrl;
 
 			if (m_citationProperties == null)
 			{
 				m_citationProperties = new Hashtable();
 			}
 			m_citationProperties.clear();
 
 			if (other.m_citationProperties != null)
 			{
 				Iterator propIt = other.m_citationProperties.keySet().iterator();
 				while (propIt.hasNext())
 				{
 					String name = (String) propIt.next();
 					Object obj = other.m_citationProperties.get(name);
 					if (obj == null)
 					{
 
 					}
 					else if (obj instanceof List)
 					{
 						List list = (List) obj;
 						List copy = new Vector();
 						Iterator valueIt = list.iterator();
 						while (valueIt.hasNext())
 						{
 							Object val = valueIt.next();
 							copy.add(val);
 						}
 						this.m_citationProperties.put(name, copy);
 					}
 					else if (obj instanceof String)
 					{
 						this.m_citationProperties.put(name, obj);
 					}
 					else
 					{
 						M_log.debug("BasicCitation copy constructor: property is not String or List: "
 						                + name + " (" + obj.getClass().getName() + ") == " + obj);
 						this.m_citationProperties.put(name, obj);
 					}
 				}
 			}
 
 			if (m_urls == null)
 			{
 				m_urls = new Hashtable();
 			}
 			m_urls.clear();
 
 			if (other.m_urls != null)
 			{
 				Iterator urlIt = other.m_urls.keySet().iterator();
 				while (urlIt.hasNext())
 				{
 					String id = (String) urlIt.next();
 					UrlWrapper wrapper = (UrlWrapper) other.m_urls.get(id);
 					addCustomUrl(wrapper.getLabel(), wrapper.getUrl());
 				}
 			}
 
 		}
 
 		/*
 		 * Simple helpers to export RIS items
 		 * prefix will most often be empty, and is used to offer an "internal label"
 		 * for stuff that gets shoved into the Notes (N1) field because there isn't
 		 * a dedicated field (e.g., Rights)
 		 *
 		 * Outputs XX  - value
 		 *   or
 		 *         XX  - prefix: value
 		 */
 
         public void exportRisField(String rislabel,  String value, StringBuffer buffer, String prefix)
 		{
 			// Get rid of the newlines and spaces
 			value = value.replaceAll("\n", " ");
 			rislabel = rislabel.trim();
 
 			// Adjust the prefix to have a colon-space afterwards, if there *is* a prefix
 			if (prefix != null && !prefix.trim().equals(""))
 			{
 				prefix = prefix + ": ";
 			}
 
 			// Export it only if there's a value, or if it's an ER tag (which is by design empty)
 			if (value != null && !value.trim().equals("") || rislabel.equals("ER"))
 			{
 				buffer.append(rislabel + RIS_DELIM + prefix + value + "\n");
 			}
 
 		}
 
         /*
          * Again, without the prefix
          */
 
         public void exportRisField(String rislabel,  String value, StringBuffer buffer)
 		{
         	exportRisField(rislabel, value, buffer, "");
         }
 
         /*
 		 * If the value is a list, iterate over it and recursively call exportRISField
 		 *
 		 */
 
         public void exportRisField(String rislabel, List propvalues, StringBuffer buffer,  String prefix)
 		{
 			Iterator propvaliter = propvalues.iterator();
 			while (propvaliter.hasNext())
 			{
 				exportRisField(rislabel, propvaliter.next(), buffer, prefix);
 			}
 		}
 
         /*
          * And again, to do the dispatch
          */
 
         public void exportRisField(String rislabel, Object val, StringBuffer buffer, String prefix)
 		{
           if (val instanceof List)
           {
         	  exportRisField(rislabel, (List) val, buffer, prefix);
           } else
           {
         	  exportRisField(rislabel,  (String) val.toString(), buffer, prefix);
           }
         }
 
         /*
          * And, finally, a dispatcher to deal with items without a prefix
          */
         public void exportRisField(String rislabel, Object val, StringBuffer buffer)
 		{
         	exportRisField(rislabel, val, buffer, "");
 		}
 
 		/*
 		 *
 		 * (non-Javadoc)
 		 *
 		 * @see org.sakaiproject.citation.api.Citation#exportToRis(java.io.OutputStream)
 		 */
 		public void exportRis(StringBuffer buffer) throws IOException
 		{
 			// Get the RISType and write a blank line and the TY tag
 			String type = "article";
 			if (m_schema != null)
 			{
 				type = m_schema.getIdentifier();
 			}
 
 			String ristype = (String) m_RISType.get(type);
 			if (ristype == null)
 			{
 				ristype = (String) m_RISType.get("article");
 			}
 			exportRisField("TY", ristype, buffer);
 
 
 			// Cycle through all the properties except for those that need
 			// pre-processing (as listed in m_RISSpecialFields)
 
 			// Deal with the "normal" fields
 
 			List fields = m_schema.getFields();
 			Iterator iter = fields.iterator();
 			while (iter.hasNext())
 			{
 				Field field = (Field) iter.next();
 				String fieldname = field.getIdentifier();
 				if (m_RISSpecialFields.contains(fieldname))
 				{
 					continue; // Skip if this is a special field
 				}
 				String rislabel = field.getIdentifier(RIS_FORMAT);
 				if (rislabel != null)
 				{
 					exportRisField(rislabel, getCitationProperty(fieldname), buffer);
 				}
 			}
 
 			// Deal with the speical fields.
 
 			/**
 			 * Dates need to be of the formt YYYY/MM/DD/other, including the
 			 * slashes even if the data is empty. Hence, we'll mostly be
 			 * producing YYYY// for date formats
 			 */
 
 			// TODO: deal with real dates. Right now, just year
 
 			exportRisField("Y1", getCitationProperty(Schema.YEAR) + "//", buffer);
 
 			// Other stuff goes into the note field -- including the note
 			// itself of course.
 
 			Iterator specIter = m_RISNoteFields.entrySet().iterator();
 			while (specIter.hasNext())
 			{
 				Map.Entry entry = (Map.Entry) specIter.next();
 				String fieldname = (String) entry.getKey();
 				String prefix = (String) entry.getValue();
 				exportRisField("N1", getCitationProperty(fieldname), buffer, prefix);
 			}
 
 			/**
 			 * Deal with URLs.
 			 */
 
 			Iterator urlIDs = this.getCustomUrlIds().iterator();
 			while (urlIDs.hasNext())
 			{
 				String id = urlIDs.next().toString();
 				try
 				{
 					String url = this.getCustomUrl(id);
 					String urlLabel = this.getCustomUrlLabel(id);
 					exportRisField("UR", url, buffer); // URL
 					exportRisField("NT",  url, buffer, urlLabel); // Note
 
 				}
 				catch (IdUnusedException e)
 				{
 					// do nothing
 				}
 			}
 
 			// Write out the end-of-record identifier and an extra newline
 			exportRisField("ER", "", buffer);
 			buffer.append("\n");
 		}
 
 
 		/* (non-Javadoc)
 		 * @see org.sakaiproject.citation.api.Citation#getCitationProperties()
 		 */
 		public Map getCitationProperties()
 		{
 			if (m_citationProperties == null)
 			{
 				m_citationProperties = new Hashtable();
 			}
 
 			return m_citationProperties;
 
 		}
 
 		/* (non-Javadoc)
 		 * @see org.sakaiproject.citation.api.Citation#getCitationProperty(java.lang.String)
 		 */
 		public Object getCitationProperty(String name)
 		{
 			if (m_citationProperties == null)
 			{
 				m_citationProperties = new Hashtable();
 			}
 			Object value = m_citationProperties.get(name);
 			if (value == null)
 			{
 				if (isMultivalued(name))
 				{
 					value = new Vector();
 					((List) value).add("");
 				}
 				else
 				{
 					value = "";
 				}
 			}
 
 			return value;
 
 		}
 
 		/*
 		 * (non-Javadoc)
 		 *
 		 * @see org.sakaiproject.citation.api.Citation#getAuthor()
 		 */
 		public String getCreator()
 		{
 			List creatorList = null;
 
 			Object creatorObj = m_citationProperties.get(Schema.CREATOR);
 
 			if (creatorObj == null)
 			{
 				creatorList = new Vector();
 				m_citationProperties.put(Schema.CREATOR, creatorList);
 			}
 			else if (creatorObj instanceof List)
 			{
 				creatorList = (List) creatorObj;
 			}
 			else if (creatorObj instanceof String)
 			{
 				creatorList = new Vector();
 				creatorList.add(creatorObj);
 				m_citationProperties.put(Schema.CREATOR, creatorList);
 			}
 
 			String creators = "";
 			int count = 0;
 			Iterator it = creatorList.iterator();
 			while (it.hasNext())
 			{
 				String creator = (String) it.next();
 				if (it.hasNext() && count > 0)
 				{
 					creators += "; " + creator;
 				}
 				else if (it.hasNext())
 				{
 					creators += creator;
 				}
 				else if (count > 1)
 				{
 					creators += "; and " + creator;
 				}
 				else if (count > 0)
 				{
 					creators += " and " + creator;
 				}
 				else
 				{
 					creators += creator;
 				}
 				count++;
 			}
 			if (!creators.trim().equals("") && !creators.trim().endsWith("."))
 			{
 				creators = creators.trim() + ". ";
 			}
 			return creators;
 		}
 
 		/*
 		 * (non-Javadoc)
 		 *
 		 * @see org.sakaiproject.citation.api.Citation#getUrl(java.lang.String)
 		 */
 		public String getCustomUrl(String id) throws IdUnusedException
 		{
 			UrlWrapper wrapper = (UrlWrapper) m_urls.get(id);
 			if (wrapper == null)
 			{
 				throw new IdUnusedException(id);
 			}
 
 			return wrapper.getUrl();
 		}
 
 		/*
 		 * (non-Javadoc)
 		 *
 		 * @see org.sakaiproject.citation.api.Citation#getUrlIds()
 		 */
 		public List getCustomUrlIds()
 		{
 			List rv = new Vector();
 			if (!m_urls.isEmpty())
 			{
 				rv.addAll(m_urls.keySet());
 			}
 			return rv;
 		}
 
 		/*
 		 * (non-Javadoc)
 		 *
 		 * @see org.sakaiproject.citation.api.Citation#getUrlLabel(java.lang.String)
 		 */
 		public String getCustomUrlLabel(String id) throws IdUnusedException
 		{
 			UrlWrapper wrapper = (UrlWrapper) m_urls.get(id);
 			if (wrapper == null)
 			{
 				throw new IdUnusedException(id);
 			}
 
 			return wrapper.getLabel();
 		}
 
 		public String getDisplayName()
 		{
 			String displayName = m_displayName;
 			if (displayName == null || displayName.trim() == "")
 			{
 				displayName = (String) getCitationProperty(Schema.TITLE);
 			}
 			if (displayName == null)
 			{
 				displayName = "";
 			}
 			displayName = displayName.trim();
 			if (displayName.length() > 0 && !displayName.endsWith(".") && !displayName.endsWith("?") && !displayName.endsWith("!") && !displayName.endsWith(","))
 			{
 				displayName += ".";
 			}
 			return new String(displayName);
 
 		}
 
 		/*
 		 * (non-Javadoc)
 		 *
 		 * @see org.sakaiproject.citation.api.Citation#getFirstAuthor()
 		 */
 		public String getFirstAuthor()
 		{
 			String firstAuthor = null;
 			List authors = (List) this.m_citationProperties.get(Schema.CREATOR);
 			if (authors != null && !authors.isEmpty())
 			{
 				firstAuthor = (String) authors.get(0);
 			}
 			if (firstAuthor != null)
 			{
 				firstAuthor = firstAuthor.trim();
 			}
 			return firstAuthor;
 		}
 
 		public String getId()
 		{
 			if (isTemporary())
 			{
 				return m_serialNumber.toString();
 			}
 			return m_id;
 		}
 
 		/*
 		 * (non-Javadoc)
 		 *
 		 * @see org.sakaiproject.citation.api.Citation#getOpenurl()
 		 */
 		public String getOpenurl()
 		{
 			// check citationProperties
 			if (m_citationProperties == null)
 			{
 				// citation properties do not exist as yet - no OpenUrl
 				return null;
 			}
 
 			String openUrlParams = getOpenurlParameters();
 
 			// return the URL-encoded string
 			return m_configService.getSiteConfigOpenUrlResolverAddress() + openUrlParams;
 		}
 
 		/*
 		 * (non-Javadoc)
 		 *
 		 * @see org.sakaiproject.citation.api.Citation#getOpenurlParameters()
 		 */
 		public String getOpenurlParameters()
 		{
 			// check citationProperties
 			if (m_citationProperties == null)
 			{
 				// citation properties do not exist as yet - no OpenUrl
 				return "";
 			}
 
 			// default to journal type
 			boolean journalOpenUrlType = true;
 			String referentValueFormat = OPENURL_JOURNAL_FORMAT;
 			// check to see whether we should construct a journal OpenUrl
 			// (includes types: article, report, unknown)
 			// or a book OpenUrl (includes types: book, chapter)
 			// String type = (String) m_citationProperties.get("type");
 			String type = "article";
 			if (m_schema != null)
 			{
 				type = m_schema.getIdentifier();
 			}
 			if (type != null && !type.trim().equals(""))
 			{
 				if (type.equals("article") || type.equals("report") || type.equals("unknown"))
 				{
 					journalOpenUrlType = true;
 					referentValueFormat = OPENURL_JOURNAL_FORMAT;
 				}
 				else
 				{
 					journalOpenUrlType = false;
 					referentValueFormat = OPENURL_BOOK_FORMAT;
 				}
 			}
 
 			// start building the OpenUrl
 			StringBuffer openUrl = null;
 			try
 			{
 				openUrl = new StringBuffer();
 
 				openUrl.append("?url_ver=" + URLEncoder.encode(OPENURL_VERSION, "utf8")
 						+ "&url_ctx_fmt=" + URLEncoder.encode(OPENURL_CONTEXT_FORMAT, "utf8")
 						+ "&rft_val_fmt=" + URLEncoder.encode(referentValueFormat, "utf8"));
 
 				// get first author
 				String author = getFirstAuthor();
 
 				// get first author's last/first name
 				if (author != null)
 				{
 					String aulast;
 					StringBuffer aufirst = new StringBuffer();
 					String[] authorNames = author.split(",");
 					if (authorNames.length == 2)
 					{
 						aulast = authorNames[0].trim();
 						aufirst.append(authorNames[1].trim());
 					}
 					else
 					{
 						authorNames = author.split("\\s");
 						aulast = authorNames[authorNames.length - 1].trim();
 						for (int i = 0; i < authorNames.length - 1; i++)
 						{
 							aufirst.append(authorNames[i] + " ");
 						}
 
 						if (aufirst.length() > 0)
 						{
 							aufirst.deleteCharAt( aufirst.length() - 1 );
 						}
 					}
 					// append to the openUrl
 					openUrl.append("&rft.aulast=" + URLEncoder.encode(aulast,"utf8"));
 
 					if (!aufirst.toString().trim().equals(""))
 					{
 						openUrl.append("&rft.aufirst=" + URLEncoder.encode(aufirst.toString().
 								trim(), "utf8"));
 					}
 				}
 				// append any other authors to the openUrl
 				java.util.List authors = (java.util.List) m_citationProperties.get(Schema.CREATOR);
 				if (authors != null && !authors.isEmpty() && authors.size() > 1)
 				{
 					for (int i = 1; i < authors.size(); i++)
 					{
 						openUrl.append("&rft.au=" + URLEncoder.encode((String) authors.get(i),
 						"utf8"));
 					}
 				}
 				// atitle <journal:article title; book: chapter title>
 				if( m_displayName != null )
 				{
 					openUrl.append("&rft.atitle=" + URLEncoder.encode(m_displayName, "utf8"));
 				}
 				else
 				{
 					// want to 'borrow' a title from another field if possible
 					String sourceTitle = (String) m_citationProperties.get(Schema.SOURCE_TITLE);
 					if (sourceTitle != null && !sourceTitle.trim().equals(""))
 					{
 						m_displayName = sourceTitle;
 						openUrl.append("&rft.atitle=" + URLEncoder.encode(m_displayName, "utf8"));
 					}
 					// could add other else ifs for fields to borrow from...
 				}
 
 				// journal title or book title
 				String sourceTitle = (String) m_citationProperties.get(Schema.SOURCE_TITLE);
 				if (sourceTitle != null && !sourceTitle.trim().equals(""))
 				{
 					if (journalOpenUrlType)
 					{
 						openUrl.append("&rft.jtitle=" + URLEncoder.encode(sourceTitle, "utf8"));
 					}
 					else
 					{
 						openUrl.append("&rft.btitle=" + URLEncoder.encode(sourceTitle, "utf8"));
 					}
 				}
 
 				// date [ YYYY-MM-DD | YYYY-MM | YYYY ] - need filtering TODO
 				//  -- perhaps do another regular expressions scan...
 				// currently just using the year
 				String year = (String) m_citationProperties.get(Schema.YEAR);
 
 				if (year != null && !year.trim().equals(""))
 				{
 					openUrl.append("&rft.date=" + URLEncoder.encode(year, "utf8"));
 				}
 
 				// volume (edition)
 				if (journalOpenUrlType)
 				{
 					String volume = (String) m_citationProperties.get(Schema.VOLUME);
 					if (volume != null && !volume.trim().equals(""))
 					{
 						openUrl.append("&rft.volume=" + URLEncoder.encode(volume, "utf8"));
 					}
 				}
 				else
 				{
 					String edition = (String) m_citationProperties.get("edition");
 					if (edition != null && !edition.trim().equals(""))
 					{
 						openUrl.append("&rft.edition=" + URLEncoder.encode(edition, "utf8"));
 					}
 				}
 
 				// issue (place)
 				// (pub)
 				if (journalOpenUrlType)
 				{
 					String issue = (String) m_citationProperties.get(Schema.ISSUE);
 					if (issue != null && !issue.trim().equals(""))
 					{
 						openUrl.append("&rft.issue=" + URLEncoder.encode(issue, "utf8"));
 					}
 
 				}
 				else
 				{
 					String pub = (String) m_citationProperties.get("pub");
 
 					if (pub != null && !pub.trim().equals(""))
 					{
 						openUrl.append("&rft.pub=" + URLEncoder.encode(pub, "utf8"));
 					}
 					String place = (String) m_citationProperties.get("place");
 					if (place != null && !place.trim().equals(""))
 					{
 						openUrl.append("&rft.place=" + URLEncoder.encode(place, "utf8"));
 					}
 				}
 
 				// spage
 				String spage = (String) m_citationProperties.get("startPage");
 				if (spage != null && !spage.trim().equals(""))
 				{
 					openUrl.append("&rft.spage=" + URLEncoder.encode(spage, "utf8"));
 				}
 
 				// epage
 				String epage = (String) m_citationProperties.get("endPage");
 				if (epage != null && !epage.trim().equals(""))
 				{
 					openUrl.append("&rft.epage=" + URLEncoder.encode(epage, "utf8"));
 				}
 
 				// pages
 				String pages = (String) m_citationProperties.get(Schema.PAGES);
 				if (pages != null && !pages.trim().equals(""))
 				{
 					openUrl.append("&rft.pages=" + URLEncoder.encode(pages, "utf8"));
 				}
 
 				// issn (isbn)
 				String isn = (String) m_citationProperties.get(Schema.ISN);
 				if (isn != null && !isn.trim().equals(""))
 				{
 					if (journalOpenUrlType)
 					{
 						openUrl.append("&rft.issn=" + URLEncoder.encode(isn, "utf8"));
 					}
 					else
 					{
 						openUrl.append("&rft.isbn=" + URLEncoder.encode(isn, "utf8"));
 					}
 				}
 			}
 			catch( UnsupportedEncodingException uee )
 			{
 				M_log.warn( "getOpenurlParameters -- unsupported encoding argument: utf8" );
 			}
 
 			// genre needs some further work... TODO
 
 			return openUrl.toString();
 		}
 
 		public Schema getSchema()
 		{
 			return m_schema;
 		}
 
 		/*
 		 * (non-Javadoc)
 		 *
 		 * @see org.sakaiproject.citation.api.Citation#getSource()
 		 */
 		public String getSource()
 		{
 			String place = (String) getCitationProperty("publicationLocation");
 			String publisher = (String) getCitationProperty(Schema.PUBLISHER);
 			String sourceTitle = (String) getCitationProperty(Schema.SOURCE_TITLE);
 			String year = (String) getCitationProperty(Schema.YEAR);
 			String volume = (String) getCitationProperty(Schema.VOLUME);
 			String issue = (String) getCitationProperty(Schema.ISSUE);
 			String pages = (String) getCitationProperty(Schema.PAGES);
 			String startPage = (String) getCitationProperty("startPage");
 			String endPage = (String) getCitationProperty("endPage");
 			if (pages == null || pages.trim().equals(""))
 			{
 				pages = null;
 				if (startPage != null && ! startPage.trim().equals(""))
 				{
 					pages = startPage.trim();
 					if (endPage != null && ! endPage.trim().equals(""))
 					{
 						pages += "-" + endPage;
 					}
 				}
 			}
 
 			String source = "";
 			String schemaId = "unknown";
 			if (m_schema != null)
 			{
 				schemaId = m_schema.getIdentifier();
 			}
 			if ("book".equals(schemaId) || "report".equals(schemaId))
 			{
 				if (place != null)
 				{
 					source += place;
 				}
 				if (publisher != null)
 				{
 					if (source.length() > 0)
 					{
 						source = source.trim() + ": ";
 					}
 					source += publisher;
 				}
 				if (year != null && ! year.trim().equals(""))
 				{
 					if (source.length() > 0)
 					{
 						source = source.trim() + ", ";
 					}
 					source += year;
 				}
 			}
 			else if ("article".equals(schemaId))
 			{
 
 				if (sourceTitle != null)
 				{
 					source += sourceTitle;
 					if (volume != null)
 					{
 						source += ", " + volume;
 						if (issue != null && !issue.trim().equals(""))
 						{
 							source += "(" + issue + ") ";
 						}
 					}
 				}
 				if(year != null && ! year.trim().equals(""))
 				{
 					source += " " + year;
 				}
 				if(source != null && source.length() > 0)
 				{
 					source = source.trim();
 					if(!source.endsWith(".") && !source.endsWith("?") && !source.endsWith("!") && !source.endsWith(","))
 					{
 						source += ". ";
 					}
 				}
 				if (pages != null)
 				{
 					source += pages;
 				}
 				if(source != null && source.length() > 0)
 				{
 					source = source.trim();
 					if(!source.endsWith(".") && !source.endsWith("?") && !source.endsWith("!") && !source.endsWith(","))
 					{
 						source += ". ";
 					}
 				}
 			}
 			else if ("chapter".equals(schemaId))
 			{
 				if (sourceTitle != null)
 				{
 					source += "In " + sourceTitle;
 					if (pages == null)
 					{
 						if (startPage != null)
 						{
 							source = source.trim() + ", " + startPage;
 							if (endPage != null)
 							{
 								source = source.trim() + "-" + endPage;
 							}
 						}
 					}
 					else
 					{
 						source = source.trim() + ", " + pages;
 					}
 					if (publisher != null)
 					{
 						if (place != null)
 						{
 							source += place + ": ";
 						}
 						source += publisher;
 						if (year != null && ! year.trim().equals(""))
 						{
 							source += ", " + year;
 						}
 					}
 					else if (year != null && ! year.trim().equals(""))
 					{
 						source += " " + year;
 					}
 				}
 			}
 			else
 			{
 				if (sourceTitle != null && ! sourceTitle.trim().equals(""))
 				{
 					source += sourceTitle;
 					if (volume != null)
 					{
 						source += ", " + volume;
 						if (issue != null && !issue.trim().equals(""))
 						{
 							source += "(" + issue + ") ";
 						}
 					}
 					if (pages == null)
 					{
 						if (startPage != null)
 						{
 							source += startPage;
 							if (endPage != null)
 							{
 								source += "-" + endPage;
 							}
 						}
 					}
 					else
 					{
 						if (source.length() > 0)
 						{
 							source += ". ";
 						}
 						source += pages + ". ";
 					}
 				}
 				else if (publisher != null && ! publisher.trim().equals(""))
 				{
 					if (place != null)
 					{
 						source += place + ": ";
 					}
 					source += publisher;
 					if (year != null && ! year.trim().equals(""))
 					{
 						source += ", " + year;
 					}
 				}
 			}
 			if (!source.endsWith(".") && !source.endsWith("?") && !source.endsWith("!") && !source.endsWith(","))
 			{
 				source = source.trim() + ". ";
 			}
 
 			return source;
 		}
 
 		public String getAbstract() {
 			if ( m_citationProperties != null && m_citationProperties.get( "abstract" ) != null )
 			{
 				return m_citationProperties.get("abstract").toString().trim();
 			}
 			else
 			{
 				return null;
 			}
 		}
 		
 		public String getSubjectString() {
 			Object subjects = getCitationProperty( "subject" );
 			
 			if ( subjects instanceof List )
 			{
 				List subjectList = ( List ) subjects;
 				ListIterator subjectListIterator = subjectList.listIterator();
 				
 				StringBuffer subjectStringBuf = new StringBuffer();
 				
 				while ( subjectListIterator.hasNext() )
 				{
 					subjectStringBuf.append( ((String)subjectListIterator.next()).trim() + ", " );
 				}
 				
 				String subjectString = subjectStringBuf.substring( 0, subjectStringBuf.length() - 2 );
 				
 				if ( subjectString.equals("") )
 				{
 					return null;
 				}
 				else
 				{
 					return subjectString;
 				}
 			}
 			else
 			{
 				return null;
 			}			
 		}
 		
 		/*
 		 * (non-Javadoc)
 		 *
 		 * @see org.sakaiproject.citation.api.Citation#hasUrls()
 		 */
 		public boolean hasCustomUrls()
 		{
 			return m_urls != null && !m_urls.isEmpty();
 		}
 
 		public boolean hasPropertyValue(String fieldId)
 		{
 			boolean hasPropertyValue = m_citationProperties.containsKey(fieldId);
 			Object val = m_citationProperties.get(fieldId);
 			if (hasPropertyValue && val != null)
 			{
 				if (val instanceof List)
 				{
 					List list = (List) val;
 					hasPropertyValue = !list.isEmpty();
 				}
 			}
 
 			return hasPropertyValue;
 		}
 
 		/*
 		 * (non-Javadoc)
 		 *
 		 * @see org.sakaiproject.citation.api.Citation#importFromRis(java.io.InputStream)
 		 */
 		public void importFromRis(InputStream istream) throws IOException
 		{
 			// TODO Auto-generated method stub
 
 		}
 
 		/*
 		 * (non-Javadoc)
 		 *
 		 * @see org.sakaiproject.citation.api.Citation#isAdded()
 		 */
 		public boolean isAdded()
 		{
 			return this.m_isAdded;
 		}
 
 		/*
 		 * (non-Javadoc)
 		 *
 		 * @see org.sakaiproject.citation.api.Citation#isMultivalued(java.lang.String)
 		 */
 		public boolean isMultivalued(String fieldId)
 		{
 			boolean isMultivalued = false;
 			if (m_schema != null)
 			{
 				Field field = m_schema.getField(fieldId);
 				if (field != null)
 				{
 					isMultivalued = field.isMultivalued();
 				}
 			}
 			return isMultivalued;
 		}
 
 		/**
 		 * @return
 		 */
 		public boolean isTemporary()
 		{
 			return m_temporary;
 		}
 
 		public List listCitationProperties()
 		{
 			if (m_citationProperties == null)
 			{
 				m_citationProperties = new Hashtable();
 			}
 
 			return new Vector(m_citationProperties.keySet());
 
 		}
 
 		/*
 		 * (non-Javadoc)
 		 *
 		 * @see org.sakaiproject.citation.api.Citation#setAdded()
 		 */
 		public void setAdded(boolean added)
 		{
 			this.m_isAdded = added;
 		}
 
 		public void setCitationProperty(String name, Object value)
 		{
 			if (m_citationProperties == null)
 			{
 				m_citationProperties = new Hashtable();
 			}
 			if (isMultivalued(name))
 			{
 				List list = (List) m_citationProperties.get(name);
 				if (list == null)
 				{
 					list = new Vector();
 					m_citationProperties.put(name, list);
 				}
 				if (value != null)
 				{
 					list.add(value);
 				}
 			}
 			else
 			{
 				if (value == null)
 				{
 					m_citationProperties.remove(name);
 				}
 				else
 				{
 					m_citationProperties.put(name, value);
 				}
 			}
 
 		}
 
 		protected void setDefaults()
 		{
 			if (m_schema != null)
 			{
 				List fields = m_schema.getFields();
 				Iterator it = fields.iterator();
 				while (it.hasNext())
 				{
 					Field field = (Field) it.next();
 					if (field.isRequired())
 					{
 						Object value = field.getDefaultValue();
 						if (value == null)
 						{
 							// do nothing -- there's no value to set
 						}
 						else if (field.isMultivalued())
 						{
 							List current_values = (List) this.getCitationProperty(field
 							        .getIdentifier());
 							if (current_values.isEmpty())
 							{
 								this.addPropertyValue(field.getIdentifier(), value);
 							}
 						}
 						else if (this.getCitationProperty(field.getIdentifier()) == null)
 						{
 							setCitationProperty(field.getIdentifier(), value);
 						}
 					}
 				}
 			}
 		}
 
 		public void setDisplayName(String name)
 		{
 			String title = (String) this.m_citationProperties.get(Schema.TITLE);
 			if (title == null || title.trim().equals(""))
 			{
 				setCitationProperty(Schema.TITLE, name);
 			}
 			m_displayName = name;
 
 		}
 
 		/*
 		 * (non-Javadoc)
 		 *
 		 * @see org.sakaiproject.citation.api.Citation#setSchema(org.sakaiproject.citation.api.Schema)
 		 */
 		public void setSchema(Schema schema)
 		{
 			this.m_schema = schema;
 			setDefaults();
 
 		}
 
 		protected void setType(String mediatype)
 		{
 			Schema schema = m_storage.getSchema(mediatype);
 			if (schema == null)
 			{
 				schema = m_storage.getSchema(UNKNOWN_TYPE);
 			}
 			setSchema(schema);
 
 		}
 
 		public String toString()
 		{
 			return "BasicCitation: " + this.m_id;
 		}
 
 		public void updateCitationProperty(String name, List values)
 		{
 			// what if "name" is not a valid field in the schema??
 			if (m_citationProperties == null)
 			{
 				m_citationProperties = new Hashtable();
 			}
 			if (isMultivalued(name))
 			{
 				List list = (List) m_citationProperties.get(name);
 				if (list == null)
 				{
 					list = new Vector();
 					m_citationProperties.put(name, list);
 				}
 				list.clear();
 				if (values != null)
 				{
 					list.addAll(values);
 				}
 			}
 			else
 			{
 				if (values == null || values.isEmpty())
 				{
 					m_citationProperties.remove(name);
 				}
 				else
 				{
 					m_citationProperties.put(name, values.get(0));
 				}
 			}
 
 		}
 
 		/*
 		 * (non-Javadoc)
 		 *
 		 * @see org.sakaiproject.citation.api.Citation#updateUrl(java.lang.String,
 		 *      java.lang.String, java.net.URL)
 		 */
 		public void updateCustomUrl(String urlid, String label, String url)
 		{
 			UrlWrapper wrapper = new UrlWrapper(label, url);
 			m_urls.put(urlid, wrapper);
 		}
 
 		/* (non-Javadoc)
          * @see org.sakaiproject.citation.api.Citation#getSaveUrl()
          */
         public String getSaveUrl(String collectionId)
         {
      		SessionManager sessionManager = (SessionManager) ComponentManager.get("org.sakaiproject.tool.api.SessionManager");
     		String sessionId = sessionManager.getCurrentSession().getId();
  
 	        String url = m_serverConfigurationService.getServerUrl() + "/savecite/" + collectionId + "?sakai.session=" + sessionId;
 	        
 	        String genre = this.getSchema().getIdentifier();
 	        url += "&genre=" + genre;
 	        
 	        String openUrlParams = this.getOpenurlParameters();
 	        String[] params = openUrlParams.split("&");
 	        for(int i = 0; i < params.length; i++)
 	        {
         		String[] parts = params[i].split("=");
         		String key = GS_TAGS.get(parts[0]);
         		if(key != null)
         		{
         			url += "&" + key + "=" + parts[1];
         		}
 	        }
 	        
 	        return url;
         }
 
 	} // BaseCitationService.BasicCitation
 
 	/**
 	 *
 	 */
 	public class BasicCitationCollection implements CitationCollection
 	{
 		protected final Comparator DEFAULT_COMPARATOR = new BasicCitationCollection.TitleComparator(true);
 		
 		public class MultipleKeyComparator implements Comparator
 		{
 			protected List m_keys = new Vector();
 			
 			protected boolean m_ascending = true;
 
 			public MultipleKeyComparator(List keys, boolean ascending)
 			{
 				m_keys.addAll(keys);
 				
 			}
 			
 			/* (non-Javadoc)
              * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
              */
             public int compare(Object arg0, Object arg1)
             {
 	            int rv = 0;
 				if (!(arg0 instanceof String) || !(arg1 instanceof String))
 				{
 					throw new ClassCastException();
 				}
 
 				Object obj0 = m_citations.get(arg0);
 				Object obj1 = m_citations.get(arg1);
 
 				if (!(obj0 instanceof Citation) || !(obj1 instanceof Citation))
 				{
 					throw new ClassCastException();
 				}
 				Citation cit0 = (Citation) obj0;
 				Citation cit1 = (Citation) obj1;
 
 	            Iterator keyIt = m_keys.iterator();
 	            while(rv == 0 && keyIt.hasNext())
 	            {
 	            	String key = (String) keyIt.next();
 	            	String str0 = "";
 	            	String str1 = "";
 	            	if(CitationCollection.SORT_BY_TITLE.equalsIgnoreCase(key))
 	            	{
 	    				String title0 = cit0.getDisplayName();
 	    				String title1 = cit1.getDisplayName();
 
 	    				if (title0 == null)
 	    				{
 	    					title0 = "";
 	    				}
 
 	    				if (title1 == null)
 	    				{
 	    					title1 = "";
 	    				}
 	    				
 	    				rv = m_ascending ? title0.compareTo(title1) : title1.compareTo(title0); 
 	            	}
 	            	else if(CitationCollection.SORT_BY_AUTHOR.equalsIgnoreCase(key))
 	            	{
 				String author0 = cit0.getCreator();
 				String author1 = cit1.getCreator();
 
 				if (author0 == null)
 				{
 					author0 = "";
 				}
 
 				if (author1 == null)
 				{
 					author1 = "";
 				}
 	    				rv = m_ascending ? author0.compareTo(author1) : author1.compareTo(author0);
 	            	}
 	            }
 	            return rv;
             }
 			
             public void addKey(String key)
             {
             	m_keys.add(key);
             }
 		}
 		
 		public class AuthorComparator extends MultipleKeyComparator
 		{			
 			/**
 			 * @param ascending
 			 */
 			public AuthorComparator(boolean ascending)
 			{
 				super(AUTHOR_AS_KEY, ascending);
 			}
 
 		}
 
 		public class BasicIterator implements CitationIterator
 		{
 			protected List listOfKeys;
 
 			protected int nextItem;
 
 			protected int lastItem;
 
 			protected int startPage = 0;
 
 			public BasicIterator()
 			{
 				this.listOfKeys = new Vector(m_order);
 				setIndexes();
 			}
 
 			/*
 			 * (non-Javadoc)
 			 *
 			 * @see org.sakaiproject.citation.api.CitationIterator#getPage()
 			 */
 			public int getPage()
 			{
 				return this.startPage;
 			}
 
 			/*
 			 * (non-Javadoc)
 			 *
 			 * @see org.sakaiproject.citation.api.CitationIterator#getPageSize()
 			 */
 			public int getPageSize()
 			{
 				// TODO Auto-generated method stub
 				return m_pageSize;
 			}
 
 			/*
 			 * (non-Javadoc)
 			 *
 			 * @see org.sakaiproject.citation.api.CitationIterator#hasNext()
 			 */
 			public boolean hasNext()
 			{
 				boolean hasNext = false;
 				if (m_ascending)
 				{
 					hasNext = this.nextItem < this.lastItem
 					        && this.nextItem < this.listOfKeys.size();
 				}
 				else
 				{
 					hasNext = this.nextItem > this.lastItem && this.nextItem > 0;
 				}
 				return hasNext;
 			}
 
 			/*
 			 * (non-Javadoc)
 			 *
 			 * @see org.sakaiproject.citation.api.CitationIterator#hasNextPage()
 			 */
 			public boolean hasNextPage()
 			{
 				return m_pageSize * (startPage + 1) < this.listOfKeys.size();
 			}
 
 			/*
 			 * (non-Javadoc)
 			 *
 			 * @see org.sakaiproject.citation.api.CitationIterator#hasPreviousPage()
 			 */
 			public boolean hasPreviousPage()
 			{
 				return this.startPage > 0;
 			}
 
 			/*
 			 * (non-Javadoc)
 			 *
 			 * @see org.sakaiproject.citation.api.CitationIterator#next()
 			 */
 			public Object next()
 			{
 				Object item = null;
 				if (m_ascending)
 				{
 					if (this.nextItem >= this.lastItem || this.nextItem >= listOfKeys.size())
 					{
 						throw new NoSuchElementException();
 					}
 					item = m_citations.get(listOfKeys.get(this.nextItem++));
 				}
 				else
 				{
 					if (this.nextItem <= this.lastItem || this.nextItem <= 0)
 					{
 						throw new NoSuchElementException();
 					}
 					item = m_citations.get(listOfKeys.get(this.nextItem--));
 				}
 				return item;
 			}
 
 			/*
 			 * (non-Javadoc)
 			 *
 			 * @see org.sakaiproject.citation.api.CitationIterator#nextPage()
 			 */
 			public void nextPage()
 			{
 				this.startPage++;
 				setIndexes();
 			}
 
 			/*
 			 * (non-Javadoc)
 			 *
 			 * @see org.sakaiproject.citation.api.CitationIterator#previousPage()
 			 */
 			public void previousPage()
 			{
 				this.startPage--;
 				setIndexes();
 			}
 
 			/*
 			 * (non-Javadoc)
 			 *
 			 * @see java.util.Iterator#remove()
 			 */
 			public void remove()
 			{
 				throw new UnsupportedOperationException();
 
 			}
 
 			protected void setIndexes()
 			{
 				if (m_ascending)
 				{
 					this.nextItem = Math.min(this.listOfKeys.size(), this.startPage * m_pageSize);
 					this.lastItem = Math.min(this.listOfKeys.size(), this.nextItem + m_pageSize);
 				}
 				else
 				{
 					this.nextItem = Math.max(0, this.listOfKeys.size() - this.startPage
 					        * m_pageSize);
 					this.lastItem = Math.max(0, this.nextItem - m_pageSize);
 				}
 			}
 
 			/*
 			 * (non-Javadoc)
 			 *
 			 * @see org.sakaiproject.citation.api.CitationIterator#setSort(java.util.Comparator)
 			 */
 			public void setOrder(Comparator comparator)
 			{
 				m_comparator = comparator;
 				if (comparator == null)
 				{
 
 				}
 				else
 				{
 					Collections.sort(this.listOfKeys, m_comparator);
 				}
 				this.startPage = 0;
 				setIndexes();
 			}
 
 			/*
 			 * (non-Javadoc)
 			 *
 			 * @see org.sakaiproject.citation.api.CitationIterator#setPage(int)
 			 */
 			public void setPage(int page)
 			{
 				this.startPage = page;
 				setIndexes();
 			}
 
 			/*
 			 * (non-Javadoc)
 			 *
 			 * @see org.sakaiproject.citation.api.CitationIterator#setPageSize(int)
 			 */
 			public void setPageSize(int size)
 			{
 				m_pageSize = size;
 				this.startPage = 0;
 				setIndexes();
 			}
 
 		}
 
 		public class TitleComparator extends MultipleKeyComparator
 		{
 			/**
 			 * @param ascending
 			 */
 			public TitleComparator(boolean ascending)
 			{
 				super(TITLE_AS_KEY, ascending);
 			}
 
 		}
 
 		protected Map m_citations = new Hashtable();
 
 		protected Comparator m_comparator = DEFAULT_COMPARATOR;
 
 		protected SortedSet m_order;
 
 		protected int m_pageSize = DEFAULT_PAGE_SIZE;
 
 		protected String m_description;
 
 		protected String m_id;
 
 		protected String m_title;
 
 		protected boolean m_temporary = false;
 
 		protected Integer m_serialNumber;
 
 		protected ActiveSearch m_mySearch;
 
 		protected boolean m_ascending = true;
 
 		public BasicCitationCollection()
 		{
 			m_id = IdManager.createUuid();
 		}
 
 		/**
 		 * @param b
 		 */
 		public BasicCitationCollection(boolean temporary)
 		{
 			m_order = new TreeSet(m_comparator);
 			
 			m_temporary = temporary;
 			if (temporary)
 			{
 				m_serialNumber = nextSerialNumber();
 			}
 			else
 			{
 				m_id = IdManager.createUuid();
 			}
 		}
 
 		public BasicCitationCollection(Map attributes, List citations)
 		{
 			m_id = IdManager.createUuid();
 
 			m_order = new TreeSet(m_comparator);
 
 			if (citations != null)
 			{
 				Iterator citationIt = citations.iterator();
 				while (citationIt.hasNext())
 				{
 					Citation citation = (Citation) citationIt.next();
 					m_citations.put(citation.getId(), citation);
 					m_order.add(citation.getId());
 				}
 			}
 		}
 
 		/**
 		 * @param collectionId
 		 */
 		public BasicCitationCollection(String collectionId)
 		{
 			m_id = collectionId;
 			
 			m_order = new TreeSet(m_comparator);
 		}
 
 		public void add(Citation citation)
 		{
 			if (!this.m_citations.keySet().contains(citation.getId()))
 			{
 				// TODO: use comparator (if defined) when adding
 				this.m_citations.put(citation.getId(), citation);
 				this.m_order.add(citation.getId());
 			}
 		}
 
 		/*
 		 * (non-Javadoc)
 		 *
 		 * @see org.sakaiproject.citation.api.CitationCollection#addAll(org.sakaiproject.citation.api.CitationCollection)
 		 */
 		public void addAll(CitationCollection other)
 		{
 			Iterator keyIt = ((BasicCitationCollection) other).m_order.iterator();
 			while (keyIt.hasNext())
 			{
 				String key = (String) keyIt.next();
 				try
 				{
 					Citation citation = other.getCitation(key);
 					this.add(citation);
 				}
 				catch (IdUnusedException e)
 				{
 					M_log.debug("BasicCitationCollection.addAll citationId (" + key
 					                + ") in m_order but not in m_citations; collectionId: "
 					                + other.getId());
 				}
 			}
 		}
 
 		/*
 		 * (non-Javadoc)
 		 *
 		 * @see org.sakaiproject.citation.api.CitationCollection#clear()
 		 */
 		public void clear()
 		{
 			this.m_order.clear();
 			this.m_citations.clear();
 		}
 
 		public boolean contains(Citation citation)
 		{
 			return this.m_citations.containsKey(citation.getId());
 		}
 
 		public void exportRis(StringBuffer buffer, List citations) throws IOException
 		{
 			// output "header" info to buffer
 
 			// Iterate over citations and output to ostream
 			Iterator it = citations.iterator();
 			while (it.hasNext())
 			{
 				String citationId = (String) it.next();
 				Citation citation = (Citation) this.m_citations.get(citationId);
 				if (citation != null)
 				{
 					citation.exportRis(buffer);
 				}
 			}
 
 		}
 
 		/**
 		 * Compute an alternate root for a reference, based on the root
 		 * property.
 		 *
 		 * @param rootProperty
 		 *            The property name.
 		 * @return The alternate root, or "" if there is none.
 		 */
 		protected String getAlternateReferenceRoot(String rootProperty)
 		{
 			// null means don't do this
 			if (rootProperty == null || rootProperty.trim().equals(""))
 			{
 				return "";
 			}
 
 			// make sure it start with a separator and does not end with one
 			if (!rootProperty.startsWith(Entity.SEPARATOR))
 			{
 				rootProperty = Entity.SEPARATOR + rootProperty;
 			}
 
 			if (rootProperty.endsWith(Entity.SEPARATOR))
 			{
 				rootProperty = rootProperty
 				        .substring(0, rootProperty.length() - SEPARATOR.length());
 			}
 
 			return rootProperty;
 		}
 
 		public Citation getCitation(String citationId) throws IdUnusedException
 		{
 			Citation citation = (Citation) m_citations.get(citationId);
 			if (citation == null)
 			{
 				throw new IdUnusedException(citationId);
 			}
 			return citation;
 		}
 
 		/*
 		 * (non-Javadoc)
 		 *
 		 * @see org.sakaiproject.citation.api.CitationCollection#getCitations()
 		 */
 		public List getCitations()
 		{
 			List citations = new Vector();
 			if (m_citations == null)
 			{
 				m_citations = new Hashtable();
 			}
 
 			Iterator keyIt = this.m_order.iterator();
 			while (keyIt.hasNext())
 			{
 				String key = (String) keyIt.next();
 
 				Object citation = this.m_citations.get(key);
 				if (citation != null)
 				{
 					citations.add(citation);
 				}
 			}
 
 			return citations;
 		}
 
 		public CitationCollection getCitations(Comparator c)
 		{
 			// TODO Auto-generated method stub
 			return null;
 		}
 
 		public CitationCollection getCitations(Comparator c, Filter f)
 		{
 			// TODO Auto-generated method stub
 			return null;
 		}
 
 		public CitationCollection getCitations(Filter f)
 		{
 			// TODO Auto-generated method stub
 			return null;
 		}
 
 		public CitationCollection getCitations(Map properties)
 		{
 			// TODO Auto-generated method stub
 			return null;
 		}
 
 		// public Citation remove(int index)
 		// {
 		// // TODO
 		// return null;
 		// }
 		//
 		// public Citation remove(Map properties)
 		// {
 		// // TODO Auto-generated method stub
 		// return null;
 		// }
 
 		/*
 		 * (non-Javadoc)
 		 *
 		 * @see org.sakaiproject.citation.api.CitationCollection#getDescription()
 		 */
 		public String getDescription()
 		{
 			return m_description;
 		}
 
 		public String getId()
 		{
 			return this.m_id;
 		}
 
 		public ResourceProperties getProperties()
 		{
 			// TODO Auto-generated method stub
 			return null;
 		}
 
 		// public void sort(Comparator c)
 		// {
 		// // TODO Auto-generated method stub
 		//
 		// }
 
 		public String getReference()
 		{
 
 			return getReference(null);
 		}
 
 		public String getReference(String rootProperty)
 		{
 
 			return m_relativeAccessPoint + getAlternateReferenceRoot(rootProperty)
 			        + Entity.SEPARATOR + getId();
 		}
 
 		/* (non-Javadoc)
          * @see org.sakaiproject.citation.api.CitationCollection#getSaveUrl()
          */
         public String getSaveUrl()
         {
 	        String url = m_serverConfigurationService.getServerUrl() + "/savecite/" + this.getId() + "/";
 	        
 	        return url;
         }
 
 		/*
 		 * (non-Javadoc)
 		 *
 		 * @see org.sakaiproject.citation.api.CitationCollection#getTitle()
 		 */
 		public String getTitle()
 		{
 			return m_title;
 		}
 
 		public String getUrl()
 		{
 			return getUrl(null);
 		}
 
 		public String getUrl(String rootProperty)
 		{
 			return getAccessPoint(false) + getAlternateReferenceRoot(rootProperty)
 			        + Entity.SEPARATOR + getId();
 		}
 
 		public boolean isEmpty()
 		{
 			return this.m_citations.isEmpty();
 		}
 
 		/*
 		 * (non-Javadoc)
 		 *
 		 * @see org.sakaiproject.citation.api.CitationCollection#iterator()
 		 */
 		public CitationIterator iterator()
 		{
 			return new BasicIterator();
 		}
 
 		// public Iterator iterator()
 		// {
 		// // TODO Auto-generated method stub
 		// return null;
 		// }
 		//
 		//
 		// public int lastIndexOf(Citation item)
 		// {
 		// // TODO Auto-generated method stub
 		// return 0;
 		// }
 		//
 		// public boolean move(int from, int to)
 		// {
 		// // TODO Auto-generated method stub
 		// return false;
 		// }
 		//
 		// public boolean moveToBack(int index)
 		// {
 		// // TODO Auto-generated method stub
 		// return false;
 		// }
 		//
 		// public boolean moveToFront(int index)
 		// {
 		// // TODO Auto-generated method stub
 		// return false;
 		// }
 		//
 		public boolean remove(Citation item)
 		{
 			boolean success = true;
 			this.m_order.remove(item.getId());
 			Object obj = this.m_citations.remove(item.getId());
 			if (obj == null)
 			{
 				success = false;
 			}
 			return success;
 		}
 
 		/*
 		 * (non-Javadoc)
 		 *
 		 * @see org.sakaiproject.citation.api.CitationCollection#saveCitation(org.sakaiproject.citation.api.Citation)
 		 */
 		public void saveCitation(Citation citation)
 		{
 			// m_storage.saveCitation(citation);
 			save(citation);
 		}
 
 		/**
 		 *
 		 * @param comparator
 		 */
 		public void setSort(Comparator comparator)
 		{
 			this.m_comparator = comparator;
 		}
 
 		/**
 		 *
 		 * @param sortBy
 		 * @param ascending
 		 */
 		public void setSort(String sortBy, boolean ascending)
 		{
 			m_ascending = ascending;
 
 			if (sortBy == null || sortBy.equalsIgnoreCase(SORT_BY_DEFAULT_ORDER))
 			{
 				this.m_comparator = null;
 			}
 			else if (sortBy.equalsIgnoreCase(SORT_BY_AUTHOR))
 			{
 				this.m_comparator = new AuthorComparator(ascending);
 			}
 			else if (sortBy.equalsIgnoreCase(SORT_BY_TITLE))
 			{
 				this.m_comparator = new TitleComparator(ascending);
 			}
 
 		}
 
 		public int size()
 		{
 			return m_order.size();
 		}
 
 		public String toString()
 		{
 			return "BasicCitationCollection: " + this.m_id;
 		}
 
 		public Element toXml(Document doc, Stack stack)
 		{
 			// TODO Auto-generated method stub
 			return null;
 		}
 
 	} // BaseCitationService.BasicCitationCollection
 
 	/**
 	 *
 	 */
 	public class BasicField implements Field
 	{
 		protected Object defaultValue;
 
 		protected String description;
 
 		protected String identifier;
 
 		protected String label;
 
 		protected int maxCardinality;
 
 		protected int minCardinality;
 
 		protected String namespace;
 
 		protected int order;
 
 		protected boolean required;
 
 		protected String valueType;
 
 		protected Map identifiers;
 
 		protected boolean isEditable;
 
 		/**
 		 * @param field
 		 */
 		public BasicField(Field other)
 		{
 			this.identifier = other.getIdentifier();
 			this.valueType = other.getValueType();
 			this.required = other.isRequired();
 			this.minCardinality = other.getMinCardinality();
 			this.maxCardinality = other.getMaxCardinality();
 			this.namespace = other.getNamespaceAbbreviation();
 			this.description = other.getDescription();
 			this.identifiers = new Hashtable();
 			this.isEditable = other.isEditable();
 
 			if (other instanceof BasicField)
 			{
 				this.order = ((BasicField) other).getOrder();
 				Iterator it = ((BasicField) other).identifiers.keySet().iterator();
 				while (it.hasNext())
 				{
 					String format = (String) it.next();
 					this.identifiers.put(format, ((BasicField) other).identifiers.get(format));
 				}
 			}
 		}
 
 		public BasicField(String identifier, String valueType, boolean isEditable,
 		        boolean required, int minCardinality, int maxCardinality)
 		{
 			this.identifier = identifier;
 			this.valueType = valueType;
 			this.required = required;
 			this.minCardinality = minCardinality;
 			this.maxCardinality = maxCardinality;
 			this.namespace = "";
 			this.label = "";
 			this.description = "";
 			this.order = 0;
 			this.identifiers = new Hashtable();
 			this.isEditable = true;
 		}
 
 		public Object getDefaultValue()
 		{
 			return defaultValue;
 		}
 
 		public String getDescription()
 		{
 			return this.description;
 		}
 
 		public String getIdentifier()
 		{
 			return identifier;
 		}
 
 		public String getIdentifier(String format)
 		{
 			return (String) this.identifiers.get(format);
 		}
 
 		public String getLabel()
 		{
 			return this.label;
 		}
 
 		public int getMaxCardinality()
 		{
 			return maxCardinality;
 		}
 
 		public int getMinCardinality()
 		{
 			return minCardinality;
 		}
 
 		public String getNamespaceAbbreviation()
 		{
 			return this.namespace;
 		}
 
 		/*
 		 * (non-Javadoc)
 		 *
 		 * @see org.sakaiproject.citation.api.Schema.Field#getOrder()
 		 */
 		public int getOrder()
 		{
 			return order;
 		}
 
 		public String getValueType()
 		{
 			return valueType;
 		}
 
 		public boolean isEditable()
 		{
 			return isEditable;
 		}
 
 		/*
 		 * (non-Javadoc)
 		 *
 		 * @see org.sakaiproject.citation.api.Schema.Field#isMultivalued()
 		 */
 		public boolean isMultivalued()
 		{
 			return this.maxCardinality > 1;
 		}
 
 		public boolean isRequired()
 		{
 			return required;
 		}
 
 		public void setDefaultValue(Object value)
 		{
 			this.defaultValue = value;
 		}
 
 		/**
 		 * @param label
 		 */
 		public void setDescription(String description)
 		{
 			this.description = description;
 		}
 
 		public void setEditable(boolean isEditable)
 		{
 			this.isEditable = isEditable;
 		}
 
 		public void setIdentifier(String format, String identifier)
 		{
 			this.identifiers.put(format, identifier);
 
 		}
 
 		/**
 		 * @param label
 		 */
 		public void setLabel(String label)
 		{
 			this.label = label;
 		}
 
 		/**
 		 * @param maxCardinality
 		 *            The maxCardinality to set.
 		 */
 		public void setMaxCardinality(int maxCardinality)
 		{
 			this.maxCardinality = maxCardinality;
 		}
 
 		/**
 		 * @param minCardinality
 		 *            The minCardinality to set.
 		 */
 		public void setMinCardinality(int minCardinality)
 		{
 			this.minCardinality = minCardinality;
 		}
 
 		public void setNamespaceAbbreviation(String namespace)
 		{
 			this.namespace = namespace;
 		}
 
 		/*
 		 * (non-Javadoc)
 		 *
 		 * @see org.sakaiproject.citation.api.Schema.Field#setOrder(int)
 		 */
 		public void setOrder(int order)
 		{
 			this.order = order;
 
 		}
 
 		/**
 		 * @param required
 		 *            The required to set.
 		 */
 		public void setRequired(boolean required)
 		{
 			this.required = required;
 		}
 
 		/**
 		 * @param valueType
 		 *            The valueType to set.
 		 */
 		public void setValueType(String valueType)
 		{
 			this.valueType = valueType;
 		}
 
 		public String toString()
 		{
 			return "BasicField: " + this.identifier;
 		}
 
 	}
 
 	/**
 	 *
 	 */
 	protected class BasicSchema implements Schema
 	{
 		protected String defaultNamespace;
 
 		protected List fields;
 
 		protected String identifier;
 
 		protected Map index;
 
 		protected Map namespaces;
 
 		protected Map identifiers;
 
 		/**
 		 *
 		 */
 		public BasicSchema()
 		{
 			this.fields = new Vector();
 			this.index = new Hashtable();
 			this.identifiers = new Hashtable();
 		}
 
 		/**
 		 * @param schema
 		 */
 		public BasicSchema(Schema other)
 		{
 			this.identifier = other.getIdentifier();
 			this.defaultNamespace = other.getNamespaceAbbrev();
 			namespaces = new Hashtable();
 			List nsAbbrevs = other.getNamespaceAbbreviations();
 			if (nsAbbrevs != null)
 			{
 				Iterator nsIt = nsAbbrevs.iterator();
 				while (nsIt.hasNext())
 				{
 					String nsAbbrev = (String) nsIt.next();
 					String ns = other.getNamespaceUri(nsAbbrev);
 					namespaces.put(nsAbbrev, ns);
 				}
 			}
 			this.identifiers = new Hashtable();
 			if (other instanceof BasicSchema)
 			{
 				Iterator it = ((BasicSchema) other).identifiers.keySet().iterator();
 				while (it.hasNext())
 				{
 					String format = (String) it.next();
 					this.identifiers.put(format, ((BasicSchema) other).identifiers.get(format));
 				}
 			}
 
 			this.fields = new Vector();
 			this.index = new Hashtable();
 			List fields = other.getFields();
 			Iterator fieldIt = fields.iterator();
 			while (fieldIt.hasNext())
 			{
 				Field field = (Field) fieldIt.next();
 				this.fields.add(new BasicField(field));
 				index.put(field.getIdentifier(), field);
 			}
 		}
 
 		/**
 		 * @param schemaId
 		 */
 		public BasicSchema(String schemaId)
 		{
 			this.identifier = schemaId;
 			this.fields = new Vector();
 			this.index = new Hashtable();
 			this.identifiers = new Hashtable();
 		}
 
 		public void addAlternativeIdentifier(String fieldId, String altFormat, String altIdentifier)
 		{
 			BasicField field = (BasicField) this.index.get(fieldId);
 			if (field != null)
 			{
 				field.setIdentifier(altFormat, altIdentifier);
 			}
 		}
 
 		/*
 		 * (non-Javadoc)
 		 *
 		 * @see org.sakaiproject.citation.api.Schema#addField(org.sakaiproject.citation.api.Schema.Field)
 		 */
 		public void addField(Field field)
 		{
 			this.index.put(field.getIdentifier(), field);
 			this.fields.add(field);
 
 		}
 
 		/**
 		 * @param order
 		 * @param field
 		 */
 		public void addField(int order, Field field)
 		{
 			fields.add(order, field);
 			index.put(identifier, field);
 		}
 
 		public BasicField addField(String identifier, String valueType, boolean isEditable,
 		        boolean required, int minCardinality, int maxCardinality)
 		{
 			if (fields == null)
 			{
 				fields = new Vector();
 			}
 			if (index == null)
 			{
 				index = new Hashtable();
 			}
 			BasicField field = new BasicField(identifier, valueType, isEditable, required,
 			        minCardinality, maxCardinality);
 			fields.add(field);
 			index.put(identifier, field);
 			return field;
 		}
 
 		public BasicField addOptionalField(String identifier, String valueType, int minCardinality,
 		        int maxCardinality)
 		{
 			return addField(identifier, valueType, true, false, minCardinality, maxCardinality);
 		}
 
 		public BasicField addRequiredField(String identifier, String valueType, int minCardinality,
 		        int maxCardinality)
 		{
 			return addField(identifier, valueType, true, true, minCardinality, maxCardinality);
 		}
 
 		public Field getField(int index)
 		{
 			if (fields == null)
 			{
 				fields = new Vector();
 			}
 			return (Field) fields.get(index);
 		}
 
 		public Field getField(String name)
 		{
 			if (index == null)
 			{
 				index = new Hashtable();
 			}
 			return (Field) index.get(name);
 		}
 
 		public List getFields()
 		{
 			if (fields == null)
 			{
 				fields = new Vector();
 			}
 			return fields;
 		}
 
 		/*
 		 * (non-Javadoc)
 		 *
 		 * @see org.sakaiproject.citation.api.Schema#getIdentifier()
 		 */
 		public String getIdentifier()
 		{
 			return this.identifier;
 		}
 
 		public String getIdentifier(String format)
 		{
 			return (String) this.identifiers.get(format);
 		}
 
 		public String getNamespaceAbbrev()
 		{
 			return defaultNamespace;
 		}
 
 		public List getNamespaceAbbreviations()
 		{
 			if (namespaces == null)
 			{
 				namespaces = new Hashtable();
 			}
 			Collection keys = namespaces.keySet();
 			List rv = new Vector();
 			if (keys != null)
 			{
 				rv.addAll(keys);
 			}
 			return rv;
 		}
 
 		public String getNamespaceUri(String abbrev)
 		{
 			if (namespaces == null)
 			{
 				namespaces = new Hashtable();
 			}
 			return (String) namespaces.get(abbrev);
 		}
 
 		public List getRequiredFields()
 		{
 			if (fields == null)
 			{
 				fields = new Vector();
 			}
 			List required = new Vector();
 			Iterator it = fields.iterator();
 			while (it.hasNext())
 			{
 				Field field = (Field) it.next();
 				if (field.isRequired())
 				{
 					required.add(field);
 				}
 			}
 
 			return required;
 		}
 
 		/**
 		 * @param identifier
 		 */
 		public void setIdentifier(String identifier)
 		{
 			this.identifier = identifier;
 		}
 
 		public void setIdentifier(String format, String identifier)
 		{
 			this.identifiers.put(format, identifier);
 
 		}
 
 		/**
 		 *
 		 */
 		public void sortFields()
 		{
 			Collections.sort(fields, new Comparator()
 			{
 
 				public int compare(Object arg0, Object arg1)
 				{
 					if (arg0 instanceof BasicField && arg1 instanceof BasicField)
 					{
 						Integer int0 = new Integer(((BasicField) arg0).getOrder());
 						Integer int1 = new Integer(((BasicField) arg1).getOrder());
 						return int0.compareTo(int1);
 					}
 					else if (arg0 instanceof Field && arg1 instanceof Field)
 					{
 						String lbl0 = ((Field) arg0).getLabel();
 						String lbl1 = ((Field) arg1).getLabel();
 						return lbl0.compareTo(lbl1);
 					}
 					else
 					{
 						throw new ClassCastException(arg0.toString() + " " + arg1.toString());
 					}
 				}
 
 			});
 
 		}
 
 		public String toString()
 		{
 			return "BasicSchema: " + this.identifier;
 		}
 
 	}
 
 	/**
 	 *
 	 */
 	protected interface Storage
 	{
 		/**
 		 * @param mediatype
 		 * @return
 		 */
 		public Citation addCitation(String mediatype);
 
 		public CitationCollection addCollection(Map attributes, List citations);
 
 		public Schema addSchema(Schema schema);
 
 		public boolean checkCitation(String citationId);
 
 		public boolean checkCollection(String collectionId);
 
 		public boolean checkSchema(String schemaId);
 
 		/**
 		 * Close.
 		 */
 		public void close();
 
 		public CitationCollection copyAll(String collectionId);
 
 		public Citation getCitation(String citationId);
 
 		public CitationCollection getCollection(String collectionId);
 
 		public Schema getSchema(String schemaId);
 
 		public List getSchemas();
 
 		/**
 		 * @return
 		 */
 		public List listSchemas();
 
 		/**
 		 * Open and be ready to read / write.
 		 */
 		public void open();
 
 		public void putSchemas(Collection schemas);
 
 		public void removeCitation(Citation edit);
 
 		public void removeCollection(CitationCollection edit);
 
 		public void removeSchema(Schema schema);
 
 		public void saveCitation(Citation edit);
 
 		public void saveCollection(CitationCollection collection);
 
 		public void updateSchema(Schema schema);
 
 		public void updateSchemas(Collection schemas);
 
 	} // interface Storage
 
 	/**
 	 *
 	 */
 	public class UrlWrapper
 	{
 		protected String m_label;
 
 		protected String m_url;
 
 		/**
 		 * @param label
 		 * @param url
 		 */
 		public UrlWrapper(String label, String url)
 		{
 			m_label = label;
 			m_url = url;
 		}
 
 		/**
 		 * @return the label
 		 */
 		public String getLabel()
 		{
 			return m_label;
 		}
 
 		/**
 		 * @return the url
 		 */
 		public String getUrl()
 		{
 			return m_url;
 		}
 
 		/**
 		 * @param label
 		 *            the label to set
 		 */
 		public void setLabel(String label)
 		{
 			m_label = label;
 		}
 
 		/**
 		 * @param url
 		 *            the url to set
 		 */
 		public void setUrl(String url)
 		{
 			m_url = url;
 		}
 	}
 
 	public static ResourceLoader rb;
 
 	/** Our logger. */
 	private static Log M_log = LogFactory.getLog(BaseCitationService.class);
 
 	protected static final String PROPERTY_DEFAULTVALUE = "sakai:defaultValue";
 
 	protected static final String PROPERTY_DESCRIPTION = "sakai:description";
 
 	protected static final String PROPERTY_HAS_ABBREVIATION = "sakai:hasAbbreviation";
 
 	protected static final String PROPERTY_HAS_CITATION = "sakai:hasCitation";
 
 	protected static final String PROPERTY_HAS_FIELD = "sakai:hasField";
 
 	protected static final String PROPERTY_HAS_NAMESPACE = "sakai:hasNamespace";
 
 	protected static final String PROPERTY_HAS_ORDER = "sakai:hasOrder";
 
 	protected static final String PROPERTY_HAS_SCHEMA = "sakai:hasSchema";
 
 	protected static final String PROPERTY_LABEL = "sakai:label";
 
 	protected static final String PROPERTY_MAXCARDINALITY = "sakai:maxCardinality";
 
 	protected static final String PROPERTY_MINCARDINALITY = "sakai:minCardinality";
 
 	protected static final String PROPERTY_NAMESPACE = "sakai:namespace";
 
 	protected static final String PROPERTY_REQUIRED = "sakai:required";
 
 	protected static final String PROPERTY_VALUETYPE = "sakai:valueType";
 
 	public static final String SCHEMA_PREFIX = "schema.";
 
 	protected static final String UNKNOWN_TYPE = "unknown";
 
 	protected static Integer m_nextSerialNumber;
 
 	/*
 	 * RIS MAPPINGS below
 	 */
 
 	protected static final String RIS_DELIM = "  - ";
 
 	/**
 	 * Set up a mapping of our type to RIS 'TY - ' values
 	 */
 	protected static final Map m_RISType = new Hashtable();
 
 	/**
 	 * Which fields map onto the RIS Notes field? Include a prefix for the data,
 	 * if necessary.
 	 */
 	protected static final Map m_RISNoteFields = new Hashtable();
 
 	/**
 	 * Which fields need special processing for RIS export?
 	 */
 	protected static final Set m_RISSpecialFields = new java.util.HashSet();
 
 	static
 	{
 		m_RISType.put("unknown", "JOUR"); // Default to journal article
 		m_RISType.put("article", "JOUR");
 		m_RISType.put("book", "BOOK");
 		m_RISType.put("chapter", "CHAP");
 		m_RISType.put("report", "RPRT");
 	}
 
 	static
 	{
 		m_RISNoteFields.put("language", "Language: ");
 		m_RISNoteFields.put("doi", "DOI: ");
 		m_RISNoteFields.put("rights", "Rights: ");
 	}
 
 	static
 	{
 		m_RISSpecialFields.add("date");
 		m_RISSpecialFields.add("doi");
 	}
 
 	public static String escapeFieldName(String original)
 	{
 		if (original == null)
         {
 	        return "";
         }
 		original = original.trim();
 		try
 		{
 			// convert the string to bytes in UTF-8
 			byte[] bytes = original.getBytes("UTF-8");
 
 			StringBuffer buf = new StringBuffer();
 			for (int i = 0; i < bytes.length; i++)
 			{
 				byte b = bytes[i];
 				// escape ascii control characters, ascii high bits, specials
 				if (Schema.ESCAPE_FIELD_NAME.indexOf((char) b) != -1)
 				{
 					buf.append(Schema.ESCAPE_CHAR); // special funky way to
 					// encode bad URL characters
 					// - ParameterParser will
 					// decode it
 				}
 				else
 				{
 					buf.append((char) b);
 				}
 			}
 
 			String rv = buf.toString();
 			return rv;
 		}
 		catch (Exception e)
 		{
 			M_log.warn("BaseCitationService.escapeFieldName: ", e);
 			return original;
 		}
 
 	}
 	
 	/** Dependency: CitationsConfigurationService. */
 	protected ConfigurationService m_configService = null;
 	
 	/** Dependency: ServerConfigurationService. */
 	protected ServerConfigurationService m_serverConfigurationService = null;
 
 	/** Dependency: ContentHostingService. */
 	protected ContentHostingService m_contentHostingService = null;
 
 	/** Dependency: EntityManager. */
 	protected EntityManager m_entityManager = null;
 
 	protected String m_defaultSchema;
 
 	/** A Storage object for persistent storage. */
 	protected Storage m_storage = null;
 
 	protected String m_relativeAccessPoint;
 
 	/**
 	 * Dependency: the ResourceTypeRegistry
 	 */
 	protected ResourceTypeRegistry m_resourceTypeRegistry;
 	
 	/**
 	 * Dependency: inject the ResourceTypeRegistry
 	 * @param registry
 	 */
 	public void setResourceTypeRegistry(ResourceTypeRegistry registry)
 	{
 		m_resourceTypeRegistry = registry;
 	}
 	
 	/**
 	 * @return the ResourceTypeRegistry
 	 */
 	public ResourceTypeRegistry getResourceTypeRegistry()
 	{
 		return m_resourceTypeRegistry;
 	}
 	
 	public Citation addCitation(String mediatype)
 	{
 		Citation edit = m_storage.addCitation(mediatype);
 
 		return edit;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 *
 	 * @see org.sakaiproject.citation.api.CitationService#addCollection()
 	 */
 	public CitationCollection addCollection()
 	{
 		CitationCollection edit = m_storage.addCollection(null, null);
 		return edit;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 *
 	 * @see org.sakaiproject.entity.api.EntityProducer#archive(java.lang.String,
 	 *      org.w3c.dom.Document, java.util.Stack, java.lang.String,
 	 *      java.util.List)
 	 */
 	public String archive(String siteId, Document doc, Stack stack, String archivePath,
 	        List attachments)
 	{
 		// TODO Auto-generated method stub
 		return null;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 *
 	 * @see org.sakaiproject.citation.api.CitationService#copyAll(java.lang.String)
 	 */
 	public CitationCollection copyAll(String collectionId)
 	{
 		return m_storage.copyAll(collectionId);
 	}
 
 	/**
 	 * Returns to uninitialized state.
 	 */
 	public void destroy()
 	{
		if(m_storage != null)
		{
			m_storage.close();
			m_storage = null;
		}
 	}
 
 	/**
 	 * Access the partial URL that forms the root of calendar URLs.
 	 *
 	 * @param relative
 	 *            if true, form within the access path only (i.e. starting with
 	 *            /content)
 	 * @return the partial URL that forms the root of calendar URLs.
 	 */
 	protected String getAccessPoint(boolean relative)
 	{
 		return (relative ? "" : m_serverConfigurationService.getAccessUrl())
 		        + m_relativeAccessPoint;
 
 	} // getAccessPoint
 
 	/*
 	 * (non-Javadoc)
 	 *
 	 * @see org.sakaiproject.citation.api.CitationService#getCollection(java.lang.String)
 	 */
 	public CitationCollection getCollection(String collectionId) throws IdUnusedException
 	{
 		CitationCollection edit = m_storage.getCollection(collectionId);
 		if (edit == null)
 		{
 			throw new IdUnusedException(collectionId);
 		}
 		return edit;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 *
 	 * @see org.sakaiproject.citation.api.CitationService#getDefaultSchema()
 	 */
 	public Schema getDefaultSchema()
 	{
 		Schema rv = null;
 		if (m_defaultSchema != null)
 		{
 			rv = m_storage.getSchema(m_defaultSchema);
 		}
 		return rv;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 *
 	 * @see org.sakaiproject.entity.api.EntityProducer#getEntity(org.sakaiproject.entity.api.Reference)
 	 */
 	public Entity getEntity(Reference ref)
 	{
 		Entity entity = null;
 		if (APPLICATION_ID.equals(ref.getType()))
 		{
 			if (REF_TYPE_EXPORT_RIS.equals(ref.getSubType()))
 			{
 				// these entities are citation collections
 				String id = ref.getId();
 				if (id == null || id.trim().equals(""))
 				{
 					String reference = ref.getReference();
 					if (reference != null && reference.startsWith(REFERENCE_ROOT))
 					{
 						id = reference.substring(REFERENCE_ROOT.length(), reference.length());
 					}
 				}
 
 				if (id != null && !id.trim().equals(""))
 				{
 					entity = m_storage.getCollection(id);
 				}
 			}
 			else if (REF_TYPE_VIEW_LIST.equals(ref.getSubType()))
 			{
 				// these entities are actually in /content
 				String id = ref.getId();
 				if (id == null || id.trim().equals(""))
 				{
 					String reference = ref.getReference();
 					if (reference.startsWith(REFERENCE_ROOT))
 					{
 						reference = reference
 						        .substring(REFERENCE_ROOT.length(), reference.length());
 					}
 					if (reference.startsWith(m_contentHostingService.REFERENCE_ROOT))
 					{
 						id = reference.substring(m_contentHostingService.REFERENCE_ROOT.length(),
 						        reference.length());
 					}
 				}
 
 				if (id != null && !id.trim().equals(""))
 				{
 					try
 					{
 						entity = m_contentHostingService.getResource(id);
 					}
 					catch (PermissionException e)
 					{
 						M_log.warn("getEntity(" + id + ") ", e);
 					}
 					catch (IdUnusedException e)
 					{
 						M_log.warn("getEntity(" + id + ") ", e);
 					}
 					catch (TypeException e)
 					{
 						M_log.warn("getEntity(" + id + ") ", e);
 					}
 				}
 			}
 		}
 
 		// and maybe others are in /citation
 
 		return entity;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 *
 	 * @see org.sakaiproject.entity.api.EntityProducer#getEntityAuthzGroups(org.sakaiproject.entity.api.Reference,
 	 *      java.lang.String)
 	 */
 	public Collection getEntityAuthzGroups(Reference ref, String userId)
 	{
 		// entities that are actually in /content use the /content authz groups
 
 		// those in /citation are open?
 
 		return null;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 *
 	 * @see org.sakaiproject.entity.api.EntityProducer#getEntityDescription(org.sakaiproject.entity.api.Reference)
 	 */
 	public String getEntityDescription(Reference ref)
 	{
 		// TODO Auto-generated method stub
 		return null;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 *
 	 * @see org.sakaiproject.entity.api.EntityProducer#getEntityResourceProperties(org.sakaiproject.entity.api.Reference)
 	 */
 	public ResourceProperties getEntityResourceProperties(Reference ref)
 	{
 		// if it's a /content item, return its props
 
 		// otherwise return null
 
 		return null;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 *
 	 * @see org.sakaiproject.entity.api.EntityProducer#getEntityUrl(org.sakaiproject.entity.api.Reference)
 	 */
 	public String getEntityUrl(Reference ref)
 	{
 
 		return null;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 *
 	 * @see org.sakaiproject.entity.api.EntityProducer#getHttpAccess()
 	 */
 	public HttpAccess getHttpAccess()
 	{
 		// if it's a /content item, the access is via CitationListAccessServlet
 		return new CitationListAccessServlet();
 	}
 
 	/*
 	 * (non-Javadoc)
 	 *
 	 * @see org.sakaiproject.entity.api.EntityProducer#getLabel()
 	 */
 	public String getLabel()
 	{
 		// TODO Auto-generated method stub
 		return null;
 	}
 
 	/**
 	 * @return
 	 */
 	public Set getMultivalued()
 	{
 		Set multivalued = new TreeSet();
 		Iterator schemaIt = m_storage.getSchemas().iterator();
 		while (schemaIt.hasNext())
 		{
 			Schema schema = (Schema) schemaIt.next();
 			{
 				Iterator fieldIt = schema.getFields().iterator();
 				while (fieldIt.hasNext())
 				{
 					Field field = (Field) fieldIt.next();
 					if (field.getMaxCardinality() > 1)
 					{
 						multivalued.add(field.getIdentifier());
 					}
 				}
 			}
 		}
 
 		return multivalued;
 	}
 
 	/**
 	 * @return
 	 */
 	public Set getMultivalued(String type)
 	{
 		Set multivalued = new TreeSet();
 		Schema schema = m_storage.getSchema(type);
 		{
 			Iterator fieldIt = schema.getFields().iterator();
 			while (fieldIt.hasNext())
 			{
 				Field field = (Field) fieldIt.next();
 				if (field.getMaxCardinality() > 1)
 				{
 					multivalued.add(field.getIdentifier());
 				}
 			}
 		}
 
 		return multivalued;
 	}
 
 	public Schema getSchema(String name)
 	{
 		Schema schema = m_storage.getSchema(name);
 		return schema;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 *
 	 * @see org.sakaiproject.citation.api.CitationService#getSchemas()
 	 */
 	public List getSchemas()
 	{
 		List schemas = new Vector(m_storage.getSchemas());
 		return schemas;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 *
 	 * @see org.sakaiproject.citation.api.Schema#getSynonyms(java.lang.String)
 	 */
 	protected Set getSynonyms(String mediatype)
 	{
 		Set synonyms = new TreeSet();
 		if (mediatype.equalsIgnoreCase("article"))
 		{
 			synonyms.add("article");
 			synonyms.add("journal article");
 			synonyms.add("journal");
 			synonyms.add("periodical");
 			synonyms.add("newspaper article");
 			synonyms.add("magazine article");
 			synonyms.add("editorial");
 			synonyms.add("peer reviewed article");
 			synonyms.add("peer reviewed journal article");
 			synonyms.add("book review");
 			synonyms.add("review");
 			synonyms.add("meeting");
 			synonyms.add("wire feed");
 			synonyms.add("wire story");
       synonyms.add("journal article (cije)");
 		}
 		else if (mediatype.equalsIgnoreCase("book"))
 		{
 			synonyms.add("book");
 
 		}
 		else if (mediatype.equalsIgnoreCase("chapter"))
 		{
 			synonyms.add("chapter");
 			synonyms.add("book chapter");
 			synonyms.add("book section");
 		}
 		else if (mediatype.equalsIgnoreCase("report"))
 		{
 			synonyms.add("report");
 			synonyms.add("editorial material");
 			synonyms.add("technical report");
 			synonyms.add("se");
       synonyms.add("document (rie)");
 		}
 
 		return synonyms;
 	}
 
 	public Citation getTemporaryCitation()
 	{
 		return new BasicCitation();
 	}
 
 	public Citation getTemporaryCitation(Asset asset)
 	{
 		return new BasicCitation(asset);
 	}
 
 	public CitationCollection getTemporaryCollection()
 	{
 		return new BasicCitationCollection(true);
 	}
 
 	/*
 	 * (non-Javadoc)
 	 *
 	 * @see org.sakaiproject.citation.api.CitationService#getValidPropertyNames()
 	 */
 	public Set getValidPropertyNames()
 	{
 		Set names = new TreeSet();
 		Iterator schemaIt = m_storage.getSchemas().iterator();
 		while (schemaIt.hasNext())
 		{
 			Schema schema = (Schema) schemaIt.next();
 			{
 				Iterator fieldIt = schema.getFields().iterator();
 				while (fieldIt.hasNext())
 				{
 					Field field = (Field) fieldIt.next();
 					names.add(field.getIdentifier());
 				}
 			}
 		}
 
 		return names;
 
 	} // getValidPropertyNames
 	
 	public class CitationListCreateAction extends BaseInteractionAction
 	{
 
 		/**
          * @param id
          * @param actionType
          * @param typeId
          * @param helperId
          * @param requiredPropertyKeys
          */
         public CitationListCreateAction(String id, ActionType actionType, String typeId, String helperId, List requiredPropertyKeys)
         {
 	        super(id, actionType, typeId, helperId, requiredPropertyKeys);
         }
 
 		/* (non-Javadoc)
          * @see org.sakaiproject.content.util.BaseResourceAction#available(org.sakaiproject.content.api.ContentEntity)
          */
         @Override
         public boolean available(ContentEntity entity)
         {
 	        // TODO If allowSiteBySiteOverride, determine whether the context allows CitationLists
         	
         	
 	        return super.available(entity);
         }
 		
         
 	}
 
 	/**
 	 *
 	 *
 	 */
 	public void init()
 	{
 		if(m_configService.isCitationsEnabledByDefault() ||
 				m_configService.isAllowSiteBySiteOverride() )
 		{
 			m_storage = newStorage();
 			m_nextSerialNumber = new Integer(0);
 	
 			m_relativeAccessPoint = CitationService.REFERENCE_ROOT;
 	
 			// register as an entity producer
 			m_entityManager.registerEntityProducer(this, REFERENCE_ROOT);
 	
 			rb = new ResourceLoader("citation_mgr");
 	
 			BasicSchema unknown = new BasicSchema();
 			unknown.setIdentifier(UNKNOWN_TYPE);
 	
 			BasicSchema article = new BasicSchema();
 			article.setIdentifier("article");
 	
 			BasicSchema book = new BasicSchema();
 			book.setIdentifier("book");
 	
 			BasicSchema chapter = new BasicSchema();
 			chapter.setIdentifier("chapter");
 	
 			BasicSchema report = new BasicSchema();
 			report.setIdentifier("report");
 	
 			unknown.addField(Schema.TITLE, Schema.SHORTTEXT, true, true, 1, 1);
 			article.addField(Schema.TITLE, Schema.SHORTTEXT, true, true, 1, 1);
 			book.addField(Schema.TITLE, Schema.SHORTTEXT, true, true, 1, 1);
 			chapter.addField(Schema.TITLE, Schema.SHORTTEXT, true, true, 1, 1);
 			report.addField(Schema.TITLE, Schema.SHORTTEXT, true, true, 1, 1);
 	
 			unknown.addAlternativeIdentifier(Schema.TITLE, RIS_FORMAT, "T1");
 			article.addAlternativeIdentifier(Schema.TITLE, RIS_FORMAT, "T1");
 			book.addAlternativeIdentifier(Schema.TITLE, RIS_FORMAT, "BT");
 			chapter.addAlternativeIdentifier(Schema.TITLE, RIS_FORMAT, "CT");
 			report.addAlternativeIdentifier(Schema.TITLE, RIS_FORMAT, "T1");
 	
 			unknown.addField(Schema.CREATOR, Schema.SHORTTEXT, true, false, 0, Schema.UNLIMITED);
 			article.addField(Schema.CREATOR, Schema.SHORTTEXT, true, false, 0, Schema.UNLIMITED);
 			book.addField(Schema.CREATOR, Schema.SHORTTEXT, true, true, 1, Schema.UNLIMITED);
 			chapter.addField(Schema.CREATOR, Schema.SHORTTEXT, true, true, 1, Schema.UNLIMITED);
 			report.addField(Schema.CREATOR, Schema.SHORTTEXT, true, true, 1, Schema.UNLIMITED);
 	
 			unknown.addAlternativeIdentifier(Schema.CREATOR, RIS_FORMAT, "A1");
 			article.addAlternativeIdentifier(Schema.CREATOR, RIS_FORMAT, "A1");
 			book.addAlternativeIdentifier(Schema.CREATOR, RIS_FORMAT, "A1");
 			chapter.addAlternativeIdentifier(Schema.CREATOR, RIS_FORMAT, "A1");
 			report.addAlternativeIdentifier(Schema.CREATOR, RIS_FORMAT, "A1");
 	
 			unknown.addField("rights", Schema.SHORTTEXT, true, false, 0, Schema.UNLIMITED);
 			article.addField("rights", Schema.SHORTTEXT, true, false, 0, Schema.UNLIMITED);
 			book.addField("rights", Schema.SHORTTEXT, true, false, 0, Schema.UNLIMITED);
 			chapter.addField("rights", Schema.SHORTTEXT, true, false, 0, Schema.UNLIMITED);
 			report.addField("rights", Schema.SHORTTEXT, true, false, 0, Schema.UNLIMITED);
 	
 			unknown.addField(Schema.YEAR, Schema.NUMBER, true, false, 0, 1);
 			article.addField(Schema.YEAR, Schema.NUMBER, true, false, 0, 1);
 			book.addField(Schema.YEAR, Schema.NUMBER, true, false, 0, 1);
 			chapter.addField(Schema.YEAR, Schema.NUMBER, true, false, 0, 1);
 			report.addField(Schema.YEAR, Schema.NUMBER, true, false, 0, 1);
 	
 			unknown.addField("date", Schema.NUMBER, true, false, 0, 1);
 			article.addField("date", Schema.NUMBER, true, false, 0, 1);
 			book.addField("date", Schema.NUMBER, true, false, 0, 1);
 			chapter.addField("date", Schema.NUMBER, true, false, 0, 1);
 			report.addField("date", Schema.NUMBER, true, false, 0, 1);
 	
 			unknown.addAlternativeIdentifier("date", RIS_FORMAT, "Y1");
 			article.addAlternativeIdentifier("date", RIS_FORMAT, "Y1");
 			book.addAlternativeIdentifier("date", RIS_FORMAT, "Y1");
 			chapter.addAlternativeIdentifier("date", RIS_FORMAT, "Y1");
 			report.addAlternativeIdentifier("date", RIS_FORMAT, "Y1");
 	
 			unknown.addField(Schema.PUBLISHER, Schema.SHORTTEXT, true, false, 0, 1);
 			book.addField(Schema.PUBLISHER, Schema.SHORTTEXT, true, false, 0, 1);
 			chapter.addField(Schema.PUBLISHER, Schema.SHORTTEXT, true, false, 0, 1);
 			report.addField(Schema.PUBLISHER, Schema.SHORTTEXT, true, false, 0, 1);
 	
 			unknown.addAlternativeIdentifier(Schema.PUBLISHER, RIS_FORMAT, "PB");
 			book.addAlternativeIdentifier(Schema.PUBLISHER, RIS_FORMAT, "PB");
 			chapter.addAlternativeIdentifier(Schema.PUBLISHER, RIS_FORMAT, "PB");
 			report.addAlternativeIdentifier(Schema.PUBLISHER, RIS_FORMAT, "PB");
 	
 			unknown.addField("publicationLocation", Schema.SHORTTEXT, true, false, 0, 1);
 			book.addField("publicationLocation", Schema.SHORTTEXT, true, false, 0, 1);
 			chapter.addField("publicationLocation", Schema.SHORTTEXT, true, false, 0, 1);
 			report.addField("publicationLocation", Schema.SHORTTEXT, true, false, 0, 1);
 	
 			unknown.addAlternativeIdentifier("publicationLocation", RIS_FORMAT, "CY");
 			book.addAlternativeIdentifier("publicationLocation", RIS_FORMAT, "CY");
 			chapter.addAlternativeIdentifier("publicationLocation", RIS_FORMAT, "CY");
 			report.addAlternativeIdentifier("publicationLocation", RIS_FORMAT, "CY");
 	
 			unknown.addField("editor", Schema.SHORTTEXT, true, false, 0, Schema.UNLIMITED);
 			book.addField("editor", Schema.SHORTTEXT, true, false, 0, Schema.UNLIMITED);
 			chapter.addField("editor", Schema.SHORTTEXT, true, false, 0, Schema.UNLIMITED);
 			report.addField("editor", Schema.SHORTTEXT, true, false, 0, Schema.UNLIMITED);
 	
 			unknown.addAlternativeIdentifier("editor", RIS_FORMAT, "A3");
 			book.addAlternativeIdentifier("editor", RIS_FORMAT, "A3");
 			chapter.addAlternativeIdentifier("editor", RIS_FORMAT, "ED");
 			report.addAlternativeIdentifier("editor", RIS_FORMAT, "A3");
 	
 			unknown.addField("edition", Schema.NUMBER, true, false, 0, 1);
 			book.addField("edition", Schema.NUMBER, true, false, 0, 1);
 			chapter.addField("edition", Schema.NUMBER, true, false, 0, 1);
 			report.addField("edition", Schema.NUMBER, true, false, 0, 1);
 	
 			unknown.addAlternativeIdentifier("edition", RIS_FORMAT, "VL");
 			book.addAlternativeIdentifier("edition", RIS_FORMAT, "VL");
 			chapter.addAlternativeIdentifier("edition", RIS_FORMAT, "VL");
 			report.addAlternativeIdentifier("edition", RIS_FORMAT, "VL");
 	
 			unknown.addField(Schema.SOURCE_TITLE, Schema.SHORTTEXT, true, false, 0, 1);
 			article.addField(Schema.SOURCE_TITLE, Schema.SHORTTEXT, true, false, 0, 1);
 			book.addField(Schema.SOURCE_TITLE, Schema.SHORTTEXT, true, false, 0, 1);
 			chapter.addField(Schema.SOURCE_TITLE, Schema.SHORTTEXT, true, false, 0, 1);
 			report.addField(Schema.SOURCE_TITLE, Schema.SHORTTEXT, true, false, 0, 1);
 	
 			unknown.addAlternativeIdentifier(Schema.SOURCE_TITLE, RIS_FORMAT, "T3");
 			article.addAlternativeIdentifier(Schema.SOURCE_TITLE, RIS_FORMAT, "JF");
 			book.addAlternativeIdentifier(Schema.SOURCE_TITLE, RIS_FORMAT, "T3");
 			chapter.addAlternativeIdentifier(Schema.SOURCE_TITLE, RIS_FORMAT, "BT");
 			report.addAlternativeIdentifier(Schema.SOURCE_TITLE, RIS_FORMAT, "T3");
 	
 			unknown.addField(Schema.VOLUME, Schema.NUMBER, true, false, 0, 1);
 			article.addField(Schema.VOLUME, Schema.NUMBER, true, false, 0, 1);
 	
 			unknown.addAlternativeIdentifier(Schema.VOLUME, RIS_FORMAT, "VL");
 			article.addAlternativeIdentifier(Schema.VOLUME, RIS_FORMAT, "VL");
 	
 			unknown.addField(Schema.ISSUE, Schema.NUMBER, true, false, 0, 1);
 			article.addField(Schema.ISSUE, Schema.NUMBER, true, false, 0, 1);
 	
 			unknown.addAlternativeIdentifier(Schema.ISSUE, RIS_FORMAT, "IS");
 			article.addAlternativeIdentifier(Schema.ISSUE, RIS_FORMAT, "IS");
 	
 			unknown.addField(Schema.PAGES, Schema.NUMBER, true, false, 0, 1);
 			article.addField(Schema.PAGES, Schema.NUMBER, true, false, 0, 1);
 			chapter.addField(Schema.PAGES, Schema.NUMBER, true, false, 0, 1);
 			report.addField(Schema.PAGES, Schema.NUMBER, true, false, 0, 1);
 	
 			unknown.addAlternativeIdentifier(Schema.PAGES, RIS_FORMAT, "SP");
 			book.addAlternativeIdentifier(Schema.PAGES, RIS_FORMAT, "SP");
 			report.addAlternativeIdentifier(Schema.PAGES, RIS_FORMAT, "SP");
 	
 			unknown.addField("startPage", Schema.NUMBER, true, false, 0, 1);
 			article.addField("startPage", Schema.NUMBER, true, false, 0, 1);
 			chapter.addField("startPage", Schema.NUMBER, true, false, 0, 1);
 	
 			unknown.addAlternativeIdentifier("startPage", RIS_FORMAT, "SP");
 			article.addAlternativeIdentifier("startPage", RIS_FORMAT, "SP");
 			chapter.addAlternativeIdentifier("startPage", RIS_FORMAT, "SP");
 	
 			unknown.addField("endPage", Schema.NUMBER, true, false, 0, 1);
 			article.addField("endPage", Schema.NUMBER, true, false, 0, 1);
 			chapter.addField("endPage", Schema.NUMBER, true, false, 0, 1);
 	
 			unknown.addAlternativeIdentifier("endPage", RIS_FORMAT, "EP");
 			article.addAlternativeIdentifier("endPage", RIS_FORMAT, "EP");
 			chapter.addAlternativeIdentifier("endPage", RIS_FORMAT, "EP");
 	
 			unknown.addField("locIdentifier", Schema.SHORTTEXT, true, false, 0, 1);
 			article.addField("locIdentifier", Schema.SHORTTEXT, true, false, 0, 1);
 			book.addField("locIdentifier", Schema.SHORTTEXT, true, false, 0, 1);
 			chapter.addField("locIdentifier", Schema.SHORTTEXT, true, false, 0, 1);
 			report.addField("locIdentifier", Schema.SHORTTEXT, true, false, 0, 1);
 	
 			unknown.addAlternativeIdentifier("locIdentifier", RIS_FORMAT, "M1");
 			article.addAlternativeIdentifier("locIdentifier", RIS_FORMAT, "M1");
 			book.addAlternativeIdentifier("locIdentifier", RIS_FORMAT, "M1");
 			chapter.addAlternativeIdentifier("locIdentifier", RIS_FORMAT, "M1");
 			report.addAlternativeIdentifier("locIdentifier", RIS_FORMAT, "M1");
 	
 			unknown.addField(Schema.ISN, Schema.SHORTTEXT, true, false, 0, 1);
 			article.addField(Schema.ISN, Schema.SHORTTEXT, true, false, 0, 1);
 			book.addField(Schema.ISN, Schema.SHORTTEXT, true, false, 0, 1);
 			chapter.addField(Schema.ISN, Schema.SHORTTEXT, true, false, 0, 1);
 			report.addField(Schema.ISN, Schema.SHORTTEXT, true, false, 0, 1);
 	
 			unknown.addAlternativeIdentifier(Schema.ISN, RIS_FORMAT, "SN");
 			article.addAlternativeIdentifier(Schema.ISN, RIS_FORMAT, "SN");
 			book.addAlternativeIdentifier(Schema.ISN, RIS_FORMAT, "SN");
 			chapter.addAlternativeIdentifier(Schema.ISN, RIS_FORMAT, "SN");
 			report.addAlternativeIdentifier(Schema.ISN, RIS_FORMAT, "SN");
 	
 			unknown.addField("Language", Schema.NUMBER, true, false, 0, 1);
 			article.addField("Language", Schema.NUMBER, true, false, 0, 1);
 			book.addField("Language", Schema.NUMBER, true, false, 0, 1);
 			chapter.addField("Language", Schema.NUMBER, true, false, 0, 1);
 			report.addField("Language", Schema.NUMBER, true, false, 0, 1);
 	
 			unknown.addField("openURL", Schema.SHORTTEXT, false, false, 0, 1);
 			article.addField("openURL", Schema.SHORTTEXT, false, false, 0, 1);
 			book.addField("openURL", Schema.SHORTTEXT, false, false, 0, 1);
 			chapter.addField("openURL", Schema.SHORTTEXT, false, false, 0, 1);
 			report.addField("openURL", Schema.SHORTTEXT, false, false, 0, 1);
 	
 			unknown.addField("inlineCitation", Schema.SHORTTEXT, false, false, 0, Schema.UNLIMITED);
 			article.addField("inlineCitation", Schema.SHORTTEXT, false, false, 0, Schema.UNLIMITED);
 			book.addField("inlineCitation", Schema.SHORTTEXT, false, false, 0, Schema.UNLIMITED);
 			chapter.addField("inlineCitation", Schema.SHORTTEXT, false, false, 0, Schema.UNLIMITED);
 			report.addField("inlineCitation", Schema.SHORTTEXT, false, false, 0, Schema.UNLIMITED);
 	
 			unknown.addField("subject", Schema.SHORTTEXT, true, false, 0, Schema.UNLIMITED);
 			article.addField("subject", Schema.SHORTTEXT, true, false, 0, Schema.UNLIMITED);
 			book.addField("subject", Schema.SHORTTEXT, true, false, 0, Schema.UNLIMITED);
 			chapter.addField("subject", Schema.SHORTTEXT, true, false, 0, Schema.UNLIMITED);
 			report.addField("subject", Schema.SHORTTEXT, true, false, 0, Schema.UNLIMITED);
 	
 			unknown.addAlternativeIdentifier("subject", RIS_FORMAT, "KW");
 			article.addAlternativeIdentifier("subject", RIS_FORMAT, "KW");
 			book.addAlternativeIdentifier("subject", RIS_FORMAT, "KW");
 			chapter.addAlternativeIdentifier("subject", RIS_FORMAT, "KW");
 			report.addAlternativeIdentifier("subject", RIS_FORMAT, "KW");
 	
 			unknown.addField("abstract", Schema.LONGTEXT, true, false, 0, 1);
 			article.addField("abstract", Schema.LONGTEXT, true, false, 0, 1);
 			book.addField("abstract", Schema.LONGTEXT, true, false, 0, 1);
 			chapter.addField("abstract", Schema.LONGTEXT, true, false, 0, 1);
 			report.addField("abstract", Schema.LONGTEXT, true, false, 0, 1);
 	
 			unknown.addAlternativeIdentifier("abstract", RIS_FORMAT, "N2");
 			article.addAlternativeIdentifier("abstract", RIS_FORMAT, "N2");
 			book.addAlternativeIdentifier("abstract", RIS_FORMAT, "N2");
 			chapter.addAlternativeIdentifier("abstract", RIS_FORMAT, "N2");
 			report.addAlternativeIdentifier("abstract", RIS_FORMAT, "N2");
 	
 			unknown.addField("note", Schema.SHORTTEXT, true, false, 0, Schema.UNLIMITED);
 			article.addField("note", Schema.SHORTTEXT, true, false, 0, Schema.UNLIMITED);
 			book.addField("note", Schema.SHORTTEXT, true, false, 0, Schema.UNLIMITED);
 			chapter.addField("note", Schema.SHORTTEXT, true, false, 0, Schema.UNLIMITED);
 			report.addField("note", Schema.SHORTTEXT, true, false, 0, Schema.UNLIMITED);
 	
 			unknown.addAlternativeIdentifier("note", RIS_FORMAT, "N1");
 			article.addAlternativeIdentifier("note", RIS_FORMAT, "N1");
 			book.addAlternativeIdentifier("note", RIS_FORMAT, "N1");
 			chapter.addAlternativeIdentifier("note", RIS_FORMAT, "N1");
 			report.addAlternativeIdentifier("note", RIS_FORMAT, "N1");
 	
 			unknown.addField("doi", Schema.NUMBER, true, false, 0, 1);
 			article.addField("doi", Schema.NUMBER, true, false, 0, 1);
 			book.addField("doi", Schema.NUMBER, true, false, 0, 1);
 			chapter.addField("doi", Schema.NUMBER, true, false, 0, 1);
 			report.addField("doi", Schema.NUMBER, true, false, 0, 1);
 	
 			unknown.addField("dateRetrieved", Schema.DATE, false, false, 0, 1);
 			article.addField("dateRetrieved", Schema.DATE, false, false, 0, 1);
 			book.addField("dateRetrieved", Schema.DATE, false, false, 0, 1);
 			chapter.addField("dateRetrieved", Schema.DATE, false, false, 0, 1);
 			report.addField("dateRetrieved", Schema.DATE, false, false, 0, 1);
 	
 			if (m_storage.checkSchema(unknown.getIdentifier()))
 			{
 				m_storage.updateSchema(unknown);
 			}
 			else
 			{
 				m_storage.addSchema(unknown);
 			}
 	
 			if (m_storage.checkSchema(article.getIdentifier()))
 			{
 				m_storage.updateSchema(article);
 			}
 			else
 			{
 				m_storage.addSchema(article);
 			}
 	
 			if (m_storage.checkSchema(book.getIdentifier()))
 			{
 				m_storage.updateSchema(book);
 			}
 			else
 			{
 				m_storage.addSchema(book);
 			}
 	
 			if (m_storage.checkSchema(chapter.getIdentifier()))
 			{
 				m_storage.updateSchema(chapter);
 			}
 			else
 			{
 				m_storage.addSchema(chapter);
 			}
 	
 			if (m_storage.checkSchema(report.getIdentifier()))
 			{
 				m_storage.updateSchema(report);
 			}
 			else
 			{
 				m_storage.addSchema(report);
 			}
 	
 			m_defaultSchema = article.getIdentifier();
 			
 			ResourceTypeRegistry registry = getResourceTypeRegistry();
 			
 			BaseInteractionAction createAction = new CitationListCreateAction(ResourceToolAction.CREATE,  
 					ResourceToolAction.ActionType.CREATE, 
 					CitationService.CITATION_LIST_ID, 
 					CitationService.HELPER_ID, 
 					new Vector());
 			
 			createAction.setLocalizer(
 					new BaseResourceAction.Localizer()
 					{ 
 						public String getLabel()
 						{
 							return rb.getString("action.create");
 						} 
 					});
 	
 			BaseInteractionAction reviseAction = new BaseInteractionAction(ResourceToolAction.REVISE_CONTENT,  
 					ResourceToolAction.ActionType.REVISE_CONTENT, 
 					CitationService.CITATION_LIST_ID, 
 					CitationService.HELPER_ID, 
 					new Vector());
 			
 			reviseAction.setLocalizer(
 					new BaseResourceAction.Localizer()
 					{ 
 						public String getLabel()
 						{
 							return rb.getString("action.revise");
 						} 
 					});
 	
 			
 			BasicResourceType typedef = new BasicResourceType(CitationService.CITATION_LIST_ID);
 			typedef.setLocalizer(new CitationLocalizer());
 			typedef.addAction(createAction);
 			typedef.addAction(reviseAction);
 			typedef.addAction(new CitationListDeleteAction());
 			
 			registry.register(typedef);
 		}
 	}
 	
 	public class CitationListDeleteAction extends BaseServiceLevelAction
 	{
 		public CitationListDeleteAction()
 		{
 			super(ResourceToolAction.DELETE, ResourceToolAction.ActionType.DELETE, CitationService.CITATION_LIST_ID, false);
 		}
 		
 		public void finalizeAction(Reference reference)
 		{
 			try
 			{
 				ContentResource resource = (ContentResource) reference.getEntity();
 				String collectionId = new String(resource.getContent());
 				CitationCollection collection = getCollection(collectionId);
 				removeCollection(collection);
 			}
 			catch(IdUnusedException e)
 			{
 				M_log.warn("IdUnusedException ", e);
 			}
 			catch(ServerOverloadException e)
 			{
 				M_log.warn("ServerOverloadException ", e);
 			}
 		}
 	}
 	
 	public class CitationLocalizer implements BasicResourceType.Localizer
 	{
 		/**
 		 *
 		 * @return
 		 */
 		public String getLabel()
 		{
 			return rb.getString("list.title");
 		}
 		
 		/**
 		 *
 		 * @param member
 		 * @return
 		 */
 		public String getLocalizedHoverText(ContentEntity member)
 		{
 			
 			return rb.getString("list.title");
 		}
 		
 	}
 
 	/**
 	 * @param schemaId
 	 * @param fieldId
 	 * @return
 	 */
 	public boolean isMultivalued(String schemaId, String fieldId)
 	{
 		Schema schema = getSchema(schemaId.toLowerCase());
 		if (schema == null)
 		{
 			if (getSynonyms("article").contains(schemaId.toLowerCase()))
 			{
 				schema = getSchema("article");
 			}
 			else if (getSynonyms("book").contains(schemaId.toLowerCase()))
 			{
 				schema = getSchema("book");
 			}
 			else if (getSynonyms("chapter").contains(schemaId.toLowerCase()))
 			{
 				schema = getSchema("chapter");
 			}
 			else if (getSynonyms("report").contains(schemaId.toLowerCase()))
 			{
 				schema = getSchema("report");
 			}
 			else
 			{
 				schema = this.getSchema("unknown");
 			}
 		}
 		Field field = schema.getField(fieldId);
 		if (field == null)
 		{
 			return false;
 		}
 		return (field.isMultivalued());
 	}
 
 	/**
 	 * Access a list of all schemas that have been defined (other than the
 	 * "unknown" type).
 	 *
 	 * @return A list of Strings representing the identifiers for known schemas.
 	 */
 	public List listSchemas()
 	{
 		Set names = (Set) m_storage.listSchemas();
 		return new Vector(names);
 	}
 
 	/*
 	 * (non-Javadoc)
 	 *
 	 * @see org.sakaiproject.entity.api.EntityProducer#merge(java.lang.String,
 	 *      org.w3c.dom.Element, java.lang.String, java.lang.String,
 	 *      java.util.Map, java.util.Map, java.util.Set)
 	 */
 	public String merge(String siteId, Element root, String archivePath, String fromSiteId,
 	        Map attachmentNames, Map userIdTrans, Set userListAllowImport)
 	{
 		// TODO Auto-generated method stub
 		return null;
 	}
 
 	/**
 	 * Construct a Storage object.
 	 *
 	 * @return The new storage object.
 	 */
 	public abstract Storage newStorage();
 
 	/**
 	 * @return
 	 */
 	protected Integer nextSerialNumber()
 	{
 		Integer number;
 		synchronized (m_nextSerialNumber)
 		{
 			number = m_nextSerialNumber;
 			m_nextSerialNumber = new Integer(number.intValue() + 1);
 		}
 
 		return number;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 *
 	 * @see org.sakaiproject.entity.api.EntityProducer#parseEntityReference(java.lang.String,
 	 *      org.sakaiproject.entity.api.Reference)
 	 */
 	public boolean parseEntityReference(String reference, Reference ref)
 	{
 		boolean citationEntity = false;
 		if (reference.startsWith(CitationService.REFERENCE_ROOT))
 		{
 			citationEntity = true;
 			String[] parts = StringUtil.split(reference, Entity.SEPARATOR);
 
 			String subType = null;
 			String context = null;
 			String id = null;
 			String container = null;
 
 			// the first part will be null, then next the service, the third
 			// will be "export_ris", "content" or "list"
 			if (parts.length > 2)
 			{
 				subType = parts[2];
 				if (CitationService.REF_TYPE_EXPORT_RIS.equals(subType))
 				{
 					context = parts[3];
 					id = parts[3];
 					ref.set(APPLICATION_ID, subType, id, container, context);
 				}
 				else if ("content".equals(subType))
 				{
 					String wrappedRef = reference.substring(REFERENCE_ROOT.length(), reference
 					        .length());
 					Reference wrapped = m_entityManager.newReference(wrappedRef);
 					ref.set(APPLICATION_ID, REF_TYPE_VIEW_LIST, wrapped.getId(), wrapped
 					        .getContainer(), wrapped.getContext());
 				}
 				else
 				{
 					M_log.warn(".parseEntityReference(): unknown citation subtype: " + subType
 					        + " in ref: " + reference);
 					citationEntity = false;
 				}
 			}
 			else
 			{
 				citationEntity = false;
 			}
 		}
 
 		return citationEntity;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 *
 	 * @see org.sakaiproject.citation.api.CitationService#removeCollection(org.sakaiproject.citation.api.CitationCollection)
 	 */
 	public void removeCollection(CitationCollection edit)
 	{
 		// TODO Auto-generated method stub
 
 	}
 
 	/*
 	 * (non-Javadoc)
 	 *
 	 * @see org.sakaiproject.citation.api.CitationService#save(org.sakaiproject.citation.api.Citation)
 	 */
 	public void save(Citation citation)
 	{
 		if (citation instanceof BasicCitation && ((BasicCitation) citation).isTemporary())
 		{
 			((BasicCitation) citation).m_id = IdManager.createUuid();
 			((BasicCitation) citation).m_temporary = false;
 			((BasicCitation) citation).m_serialNumber = null;
 		}
 		this.m_storage.saveCitation(citation);
 	}
 
 	/*
 	 * (non-Javadoc)
 	 *
 	 * @see org.sakaiproject.citation.api.CitationService#save(org.sakaiproject.citation.api.CitationCollection)
 	 */
 	public void save(CitationCollection collection)
 	{
 		this.m_storage.saveCollection(collection);
 	}
 	/**
 	 * Dependency: ConfigurationService.
 	 *
 	 * @param service
 	 *            The ConfigurationService.
 	 */
 	public void setConfigurationService(ConfigurationService service)
 	{
 		m_configService = service;
 	}
 	
 	/**
 	 * Dependency: ContentHostingService.
 	 *
 	 * @param service
 	 *            The ContentHostingService.
 	 */
 	public void setContentHostingService(ContentHostingService service)
 	{
 		m_contentHostingService = service;
 	}
 
 	/**
 	 * Dependency: EntityManager.
 	 *
 	 * @param service
 	 *            The EntityManager.
 	 */
 	public void setEntityManager(EntityManager service)
 	{
 		m_entityManager = service;
 	}
 
 	/**
 	 * Dependency: ServerConfigurationService.
 	 *
 	 * @param service
 	 *            The ServerConfigurationService.
 	 */
 	public void setServerConfigurationService(ServerConfigurationService service)
 	{
 		m_serverConfigurationService = service;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 *
 	 * @see org.sakaiproject.entity.api.EntityProducer#willArchiveMerge()
 	 */
 	public boolean willArchiveMerge()
 	{
 		// TODO Auto-generated method stub
 		return false;
 	}
 
 	public final class Counter
 	{
 		private int value;
 		private Integer lock = new Integer(0);
 
 		public Counter()
 		{
 			value = 0;
 		}
 
 		public Counter(int val)
 		{
 			value = val;
 		}
 
 
 		public void increment()
 		{
 			synchronized(lock)
 			{
 				value++;
 			}
 		}
 
 		public void decrement()
 		{
 			synchronized(lock)
 			{
 				value--;
 			}
 		}
 
 		public int intValue()
 		{
 			return value;
 		}
 	}
 
 	/**
      * @return the attemptToMatchSchema
      */
     public boolean isAttemptToMatchSchema()
     {
     	return attemptToMatchSchema;
     }
 
 	/**
      * @param attemptToMatchSchema the attemptToMatchSchema to set
      */
     public void setAttemptToMatchSchema(boolean attemptToMatchSchema)
     {
     	this.attemptToMatchSchema = attemptToMatchSchema;
     }
 
 } // BaseCitationService
 
