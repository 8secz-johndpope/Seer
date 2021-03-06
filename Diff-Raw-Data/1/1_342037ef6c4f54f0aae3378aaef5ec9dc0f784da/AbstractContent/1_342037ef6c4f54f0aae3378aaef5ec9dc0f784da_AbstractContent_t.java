 /*******************************************************************************
  * Copyright (c) 2004 Actuate Corporation.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *  Actuate Corporation  - initial API and implementation
  *******************************************************************************/
 
 package org.eclipse.birt.report.engine.content.impl;
 
 import java.io.DataInputStream;
 import java.io.DataOutputStream;
 import java.io.IOException;
 
 import org.eclipse.birt.core.util.IOUtil;
 import org.eclipse.birt.report.engine.api.InstanceID;
 import org.eclipse.birt.report.engine.content.IContent;
 import org.eclipse.birt.report.engine.content.IContentVisitor;
 import org.eclipse.birt.report.engine.content.IHyperlinkAction;
 import org.eclipse.birt.report.engine.content.IReportContent;
 import org.eclipse.birt.report.engine.content.IStyle;
 import org.eclipse.birt.report.engine.css.dom.CompositeStyle;
 import org.eclipse.birt.report.engine.css.dom.ComputedStyle;
 import org.eclipse.birt.report.engine.css.dom.StyleDeclaration;
 import org.eclipse.birt.report.engine.css.engine.BIRTCSSEngine;
 import org.eclipse.birt.report.engine.css.engine.CSSEngine;
 import org.eclipse.birt.report.engine.ir.DimensionType;
 import org.eclipse.birt.report.engine.ir.ReportItemDesign;
 import org.eclipse.birt.report.engine.ir.StyledElementDesign;
 
 abstract public class AbstractContent extends AbstractElement
 		implements
 			IContent
 {
 
 	transient protected IReportContent report;
 
 	transient protected CSSEngine cssEngine;
 
 	protected String name;
 
 	protected DimensionType x;
 
 	protected DimensionType y;
 
 	protected DimensionType width;
 
 	protected DimensionType height;
 
 	protected IHyperlinkAction hyperlink;
 
 	protected String bookmark;
 
 	protected String helpText;
 
 	transient protected String styleClass;
 
 	protected IStyle inlineStyle;
 
 	transient protected IStyle style;
 
 	transient protected IStyle computedStyle;
 
 	transient protected Object generateBy;
 
 	protected InstanceID instanceId;
 
 	protected Object toc;
 
 	transient protected long offset = -1;
 	
 	transient protected IContent lastChild = null;
 	
 	transient protected int version = -1;
 
 	/**
 	 * Constructor of the AbstractContent
 	 * @param report report can't be null
 	 */
 	public AbstractContent( IReportContent report )
 	{
 		assert ( report != null && report instanceof ReportContent );
 		this.report = report;
 		this.parent = report.getRoot( );
 		this.cssEngine = ( (ReportContent) report ).getCSSEngine( );
 	}
 
 	/**
 	 * Set the report content, and the content can not be null
 	 * @param report report can't be null
 	 */
 	public void setReportContent( IReportContent report )
 	{
 		assert ( report != null && report instanceof ReportContent );
 		this.report = report;
 		this.cssEngine = ( (ReportContent) report ).getCSSEngine( );
 	}
 
 	/**
 	 * Constructor of the AbstractContent
 	 * @param content content can't be null
 	 */
 	public AbstractContent( IContent content )
 	{
 		this.report = content.getReportContent( );
 		this.parent = content.getReportContent( ).getRoot( );
 		this.cssEngine = ( (ReportContent) content.getReportContent( ) ).getCSSEngine( );
 		
 		this.name = content.getName( );
 		this.x = content.getX( );
 		this.y = content.getY( );
 		this.width = content.getWidth( );
 		this.height = content.getHeight( );
 		this.hyperlink = content.getHyperlinkAction( );
 		this.bookmark = content.getBookmark( );
 		this.helpText = content.getHelpText( );
 		this.inlineStyle = content.getInlineStyle( );
 		this.generateBy = content.getGenerateBy( );
 		this.styleClass = content.getStyleClass( );
 		this.instanceId = content.getInstanceID( );
 		this.toc = content.getTOC( );
 		Object ext = content.getExtension( IContent.DOCUMENT_EXTENSION );
 		if ( ext != null )
 		{
 			setExtension( IContent.DOCUMENT_EXTENSION, ext );
 		}
 	}
 
 	public IReportContent getReportContent( )
 	{
 		return this.report;
 	}
 
 	public CSSEngine getCSSEngine( )
 	{
 		return this.cssEngine;
 	}
 
 	public String getName( )
 	{
 		return name;
 	}
 
 	public Object accept( IContentVisitor visitor, Object value )
 	{
 		return visitor.visitContent( this, value );
 	}
 
 	/**
 	 * @return the bookmark value
 	 */
 	public String getBookmark( )
 	{
 		return bookmark;
 	}
 
 	/**
 	 * @return the actionString
 	 */
 	public IHyperlinkAction getHyperlinkAction( )
 	{
 		return hyperlink;
 	}
 
 	/**
 	 * @return the height of the report item
 	 */
 	public DimensionType getHeight( )
 	{
 		if ( height != null )
 		{
 			return height;
 		}
 		if ( generateBy instanceof ReportItemDesign )
 		{
 			return ( (ReportItemDesign) generateBy ).getHeight( );
 		}
 		return null;
 	}
 
 	/**
 	 * @return the width of the report item
 	 */
 	public DimensionType getWidth( )
 	{
 		if ( width != null )
 		{
 			return width;
 		}
 		if ( generateBy instanceof ReportItemDesign )
 		{
 			return ( (ReportItemDesign) generateBy ).getWidth( );
 		}
 		return null;
 
 	}
 
 	/**
 	 * @return the x position of the repor titem.
 	 */
 	public DimensionType getX( )
 	{
 		if ( x != null )
 		{
 			return x;
 		}
 		if ( generateBy instanceof ReportItemDesign )
 		{
 			return ( (ReportItemDesign) generateBy ).getX( );
 		}
 		return null;
 	}
 
 	/**
 	 * @return Returns the y position of the repor titem.
 	 */
 	public DimensionType getY( )
 	{
 		if ( y != null )
 		{
 			return y;
 		}
 		if ( generateBy instanceof ReportItemDesign )
 		{
 			return ( (ReportItemDesign) generateBy ).getY( );
 		}
 		return null;
 	}
 
 	/**
 	 * @return Returns the helpText.
 	 */
 	public String getHelpText( )
 	{
 		return helpText;
 	}
 
 	public IStyle getComputedStyle( )
 	{
 		if ( computedStyle == null )
 		{
 			CSSEngine cssEngine = null;
 			if ( report != null )
 			{
 				assert ( report instanceof ReportContent );
 				cssEngine = ( (ReportContent) report ).getCSSEngine( );
 			}
 			if ( cssEngine == null )
 			{
 				cssEngine = new BIRTCSSEngine( );
 			}
 			computedStyle = new ComputedStyle( this );
 		}
 		return computedStyle;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.birt.report.engine.content.IStyledContent#getStyle()
 	 */
 	public IStyle getStyle( )
 	{
 		if ( style == null )
 		{
 			if ( inlineStyle == null )
 			{
 				inlineStyle = report.createStyle( );
 			}
 			String styleClass = getStyleClass( );
 			IStyle classStyle = report.findStyle( styleClass );
 			style = new CompositeStyle( classStyle, inlineStyle );
 		}
 		return style;
 	}
 
 	public Object getGenerateBy( )
 	{
 		return generateBy;
 	}
 
 	/**
 	 * @param bookmark
 	 *            The bookmark to set.
 	 */
 	public void setBookmark( String bookmark )
 	{
 		this.bookmark = bookmark;
 	}
 
 	/**
 	 * @param generateBy
 	 *            The generateBy to set.
 	 */
 	public void setGenerateBy( Object generateBy )
 	{
 		this.generateBy = generateBy;
 	}
 
 	/**
 	 * @param height
 	 *            The height to set.
 	 */
 	public void setHeight( DimensionType height )
 	{
 		this.height = height;
 	}
 
 	/**
 	 * @param helpText
 	 *            The helpText to set.
 	 */
 	public void setHelpText( String helpText )
 	{
 		this.helpText = helpText;
 	}
 
 	/**
 	 * @param hyperlink
 	 *            The hyperlink to set.
 	 */
 	public void setHyperlinkAction( IHyperlinkAction hyperlink )
 	{
 		this.hyperlink = hyperlink;
 	}
 
 	/**
 	 * @param name
 	 *            The name to set.
 	 */
 	public void setName( String name )
 	{
 		this.name = name;
 	}
 
 	public void setStyleClass( String name )
 	{
 		this.styleClass = name;
 		this.style = null;
 		this.computedStyle = null;
 	}
 
 	public String getStyleClass( )
 	{
 		if ( styleClass != null )
 		{
 			return styleClass;
 		}
 		if ( generateBy instanceof StyledElementDesign )
 		{
 			return ( (StyledElementDesign) generateBy ).getStyleName( );
 		}
 		return null;
 	}
 
 	/**
 	 * @param style
 	 *            The style to set.
 	 */
 	public void setInlineStyle( IStyle style )
 	{
 
 		this.inlineStyle = style;
 		this.style = null;
 		this.computedStyle = null;
 	}
 
 	public IStyle getInlineStyle( )
 	{
 		return inlineStyle;
 	}
 
 	/**
 	 * @param width
 	 *            The width to set.
 	 */
 	public void setWidth( DimensionType width )
 	{
 		this.width = width;
 	}
 
 	/**
 	 * @param x
 	 *            The x to set.
 	 */
 	public void setX( DimensionType x )
 	{
 		this.x = x;
 	}
 
 	/**
 	 * @param y
 	 *            The y to set.
 	 */
 	public void setY( DimensionType y )
 	{
 		this.y = y;
 	}
 
 	public InstanceID getInstanceID( )
 	{
 		return instanceId;
 	}
 
 	public void setInstanceID( InstanceID id )
 	{
 		this.instanceId = id;
 	}
 
 	public void setTOC( Object toc )
 	{
 		this.toc = toc;
 	}
 
 	public Object getTOC( )
 	{
 		return toc;
 	}
 
 	protected static final int LAST_EXTENSION = 2;
 	protected Object[] extensions;
 	public Object getExtension(int extension)
 	{
 		if (extensions != null )
 		{
 			assert extension < LAST_EXTENSION;
 			return extensions[extension];
 		}
 		return null;
 	}
 	
 	public void setExtension(int extension, Object value)
 	{
 		if (extensions == null)
 		{
 			extensions = new Object[LAST_EXTENSION];
 		}
 		extensions[extension] = value;
 	}
 	/**
 	 * object document version
 	 */
 	static final protected int VERSION_0 = 0;
 	static final protected int VERSION_1 = 1;
 
 	final static short FIELD_NONE = -1;
 	final static short FIELD_NAME = 0;
 	final static short FIELD_X = 1;
 	final static short FIELD_Y = 2;
 	final static short FIELD_WIDTH = 3;
 	final static short FIELD_HEIGHT = 4;
 	final static short FIELD_HYPERLINK = 5;
 	final static short FIELD_BOOKMARK = 6;
 	final static short FIELD_HELPTEXT = 7;
 	final static short FIELD_INLINESTYLE = 8;
 	final static short FIELD_INSTANCE_ID = 9;
 	final static short FIELD_TOC = 10;
 
 	protected void writeFields( DataOutputStream out ) throws IOException
 	{
 		if ( name != null )
 		{
 			IOUtil.writeShort( out, FIELD_NAME );
 			IOUtil.writeString( out, name );
 		}
 		if ( x != null )
 		{
 			IOUtil.writeShort( out, FIELD_X );
 			x.writeObject( out );
 		}
 		if ( y != null )
 		{
 			IOUtil.writeShort( out, FIELD_Y );
 			y.writeObject( out );
 		}
 		if ( width != null )
 		{
 			IOUtil.writeShort( out, FIELD_WIDTH );
 			width.writeObject( out );
 		}
 		if ( height != null )
 		{
 			IOUtil.writeShort( out, FIELD_HEIGHT );
 			height.writeObject( out );
 		}
 		if ( hyperlink != null )
 		{
 			IOUtil.writeShort( out, FIELD_HYPERLINK );
 			( (ActionContent) hyperlink ).writeObject( out );
 		}
 		if ( bookmark != null )
 		{
 			IOUtil.writeShort( out, FIELD_BOOKMARK );
 			IOUtil.writeString( out, bookmark );
 		}
 		if ( helpText != null )
 		{
 			IOUtil.writeShort( out, FIELD_HELPTEXT );
 			IOUtil.writeString( out, helpText );
 		}
 		if ( inlineStyle != null )
 		{
 			String cssText = inlineStyle.getCssText( );
 			if ( cssText != null && cssText.length( ) != 0 )
 			{
 				IOUtil.writeShort( out, FIELD_INLINESTYLE );
 				IOUtil.writeString( out, cssText );
 			}
 		}
 		if ( instanceId != null )
 		{
 			IOUtil.writeShort( out, FIELD_INSTANCE_ID );
 			IOUtil.writeString( out, instanceId.toString( ) );
 		}
 		if ( toc != null )
 		{
 			IOUtil.writeShort( out, FIELD_TOC );
 			IOUtil.writeObject( out, toc );
 		}
 	}
 
 	protected void readField( int version, int filedId, DataInputStream in )
 			throws IOException
 	{
 		switch ( filedId )
 		{
 			case FIELD_NAME :
 				name = IOUtil.readString( in );
 				break;
 			case FIELD_X :
 				x = new DimensionType( );
 				x.readObject( in );
 				break;
 			case FIELD_Y :
 				y = new DimensionType( );
 				y.readObject( in );
 				break;
 			case FIELD_WIDTH :
 				width = new DimensionType( );
 				width.readObject( in );
 				break;
 			case FIELD_HEIGHT :
 				height = new DimensionType( );
 				height.readObject( in );
 				break;
 			case FIELD_HYPERLINK :
 				ActionContent action = new ActionContent( );
 				action.readObject( in );
 				hyperlink = action;
 				break;
 			case FIELD_BOOKMARK :
 				bookmark = IOUtil.readString( in );
 				break;
 			case FIELD_HELPTEXT :
 				helpText = IOUtil.readString( in );
 				break;
 			case FIELD_INLINESTYLE :
 				String style = IOUtil.readString( in );
 				if ( style != null && style.length( ) != 0 )
 				{
 					inlineStyle = new StyleDeclaration( cssEngine );
 					inlineStyle.setCssText( style );
 				}
 				break;
 			case FIELD_INSTANCE_ID :
 				String value = IOUtil.readString( in );
 				instanceId = InstanceID.parse( value );
 				break;
 			case FIELD_TOC :
 				toc = IOUtil.readObject( in );
 				break;
 		}
 	}
 
 	public void readContent( DataInputStream in ) throws IOException
 	{
 		if ( this.version == VERSION_1 )
 		{
 			readContentV1( in );
 		}
 		else if ( this.version == VERSION_0 )
 		{
 			readContentV0( in );
 		}
 		else
 		{
 			throw new IOException( "Unknown content version " + version );
 		}
 	}
 
 	protected void readContentV0( DataInputStream in ) throws IOException
 	{
 		//int version = IOUtil.readInt( in );
 		//read version
 		IOUtil.readInt( in );
 		
 		int filedId = IOUtil.readInt( in );
 		while ( filedId != FIELD_NONE )
 		{
 			readField( VERSION_0, filedId, in );
 			filedId = IOUtil.readInt( in );
 		}
 	}
 
 	protected void readContentV1( DataInputStream in ) throws IOException
 	{
 		while ( in.available( ) > 0 )
 		{
 			int filedId = IOUtil.readShort( in );
 			readField( VERSION_1, filedId, in );
 		}
 	}
 
 	public void writeContent( DataOutputStream out ) throws IOException
 	{
 		writeFields( out );
 	}
 	
 	/**
 	 * @param iVersion
 	 *            The version of the content.
 	 */
 	public void setVersion( int version )
 	{
 		this.version = version;
 	}
 
 	public boolean needSave( )
 	{
 		if ( name != null )
 		{
 			return true;
 		}
 		if ( x != null || y != null || width != null || height != null )
 		{
 			return true;
 		}
 		if ( hyperlink != null || bookmark != null || toc != null )
 		{
 			return true;
 		}
 		if ( helpText != null )
 		{
 			return true;
 		}
 		if ( inlineStyle != null && !inlineStyle.isEmpty( ))
 		{
 			return true;
 		}
 		return false;
 	}
 }
