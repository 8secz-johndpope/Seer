 /*
  * Copyright 2005-2006 Noelios Consulting.
  *
  * The contents of this file are subject to the terms
  * of the Common Development and Distribution License
  * (the "License").  You may not use this file except
  * in compliance with the License.
  *
  * You can obtain a copy of the license at
  * http://www.opensource.org/licenses/cddl1.txt
  * See the License for the specific language governing
  * permissions and limitations under the License.
  *
  * When distributing Covered Code, include this CDDL
  * HEADER in each file and include the License file at
  * http://www.opensource.org/licenses/cddl1.txt
  * If applicable, add the following below this CDDL
  * HEADER, with the fields enclosed by brackets "[]"
  * replaced with your own identifying information:
  * Portions Copyright [yyyy] [name of copyright owner]
  */
 
 package org.restlet.data;
 
 import java.util.Map;
 import java.util.TreeMap;
 
 import org.restlet.spi.Factory;
 
 /**
  * Generic message exchanged between client and server connectors.
  * @author Jerome Louvel (contact@noelios.com) <a href="http://www.noelios.com/">Noelios Consulting</a>
  */
 public abstract class Message
 {
 	/** The modifiable attributes map. */
 	private Map<String, Object> attributes;
 
 	/** The payload of the message. */
 	private Representation entity;
 	
 	/** The wrapped message. */
 	private Message wrappedMessage;
 
 	/**
 	 * Constructor.
 	 */
 	public Message()
 	{
 		this((Representation)null);
 	}
 
 	/**
 	 * Constructor.
 	 * @param entity The payload of the message.
 	 */
 	public Message(Representation entity)
 	{
 		this.attributes = null;
 		this.entity = entity;
 		this.wrappedMessage = null;
 	}
 
 	/**
 	 * Wrapper constructor.
 	 * @param wrappedMessage The message to wrap.
 	 */
 	public Message(Message wrappedMessage)
 	{
 		this.wrappedMessage = wrappedMessage;
 	}
 
 	/**
 	 * Returns the wrapped message.
 	 * @return The wrapped message.
 	 */
 	public Message getWrappedMessage()
 	{
 		return this.wrappedMessage;
 	}
 
 	/**
 	 * Returns a modifiable attributes map that can be used by developers to save information relative
 	 * to the message. This is an easier alternative to the creation of a wrapper instance around the 
 	 * whole message.<br/>
 	 * <br/>
 	 * In addition, this map is a shared space between the developer and the connectors. In this case, 
 	 * it is used to exchange information that is not uniform across all protocols and couldn't therefore
 	 * be directly included in the API. For this purpose, all attribute names starting with "org.restlet"
 	 * are reserved. Currently the following attributes are used:
 	 * <table>
 	 * 	<tr>
 	 * 		<th>Attribute name</th>
 	 * 		<th>Class name</th>
 	 * 		<th>Description</th>
 	 * 	</tr>
 	 * 	<tr>
 	 * 		<td>org.restlet.http.headers</td>
 	 * 		<td>org.restlet.data.ParameterList</td>
	 * 		<td>Server HTTP connectors must provide all the request headers exactly as they were received
	 * from the client. When invoking client HTTP connectors, developers can also set this attribute to 
	 * specify <b>non-standard</b> HTTP headers that should be added to the request sent to a server.</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>org.restlet.http.headers</td>
	 * 		<td>org.restlet.data.ParameterList</td>
	 * 		<td>Client HTTP connectors must provide all the response headers exactly as they were received
	 * from the server. When replying to server HTTP connectors, developers can also set this attribute to 
	 * specify <b>non-standard</b> HTTP headers that should be added to the response sent to a client.</td>
 	 * 	</tr>
 	 *	</table>
 	 * Adding standard HTTP headers is forbidden because it could conflict with the connector's internal 
 	 * behavior, limit portability or prevent future optimizations.</td>
 	 * @return The modifiable attributes map.
 	 */
 	public Map<String, Object> getAttributes()
 	{
 		if(getWrappedMessage() != null)
 		{
 			return getWrappedMessage().getAttributes();
 		}
 		else
 		{
 			if (attributes == null)
 			{
 				attributes = new TreeMap<String, Object>();
 			}
 	
 			return attributes;
 		}
 	}
 
 	/**
 	 * Returns the entity representation.
 	 * @return The entity representation.
 	 */
 	public Representation getEntity()
 	{
 		if(getWrappedMessage() != null)
 		{
 			return getWrappedMessage().getEntity();
 		}
 		else
 		{
 			return this.entity;
 		}
 	}
 
 	/**
 	 * Returns the entity as a form.<br/>
 	 * Note that this triggers the parsing of the entity.<br/>
 	 * This method and the related getEntity*() methods can only be invoked once.
 	 * @return The entity as a form.
 	 */
 	public Form getEntityAsForm()
 	{
 		if(getWrappedMessage() != null)
 		{
 			return getWrappedMessage().getEntityAsForm();
 		}
 		else
 		{
 			return new Form(getEntity());
 		}
 	}
 
 	/**
 	 * Returns the entity as a string.<br/>
 	 * Note that this triggers the parsing of the entity.<br/>
 	 * This method and the related getEntity*() methods can only be invoked once.
 	 * @return The entity as a string.
 	 */
 	public String getEntityAsString()
 	{
 		if(getWrappedMessage() != null)
 		{
 			return getWrappedMessage().getEntityAsString();
 		}
 		else
 		{
 			return getEntity().toString();
 		}
 	}
 
 	/**
 	 * Indicates if a content is available and can be sent. Several conditions must be met: the content 
 	 * must exists and have some available data.
 	 * @return True if a content is available and can be sent.
 	 */
 	public boolean isEntityAvailable()
 	{
 		if(getWrappedMessage() != null)
 		{
 			return getWrappedMessage().isEntityAvailable();
 		}
 		else
 		{
 			return (getEntity() != null) && (getEntity().getSize() > 0) && getEntity().isAvailable();
 		}
 	}
 	
 	/**
 	 * Sets the entity representation.
 	 * @param entity The entity representation.
 	 */
 	public void setEntity(Representation entity)
 	{
 		if(getWrappedMessage() != null)
 		{
 			getWrappedMessage().setEntity(entity);
 		}
 		else
 		{
 			this.entity = entity;
 		}
 	}
 
 	/**
 	 * Sets a textual entity.
     * @param value The represented string.
     * @param mediaType The representation's media type.
 	 */
 	public void setEntity(String value, MediaType mediaType)
 	{
 		if(getWrappedMessage() != null)
 		{
 			getWrappedMessage().setEntity(value, mediaType);
 		}
 		else
 		{
 			setEntity(Factory.getInstance().createRepresentation(value, mediaType));
 		}
 	}
 
 }
