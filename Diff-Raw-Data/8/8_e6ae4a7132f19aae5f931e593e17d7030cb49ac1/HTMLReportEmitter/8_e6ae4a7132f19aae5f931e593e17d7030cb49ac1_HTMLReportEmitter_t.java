 /*******************************************************************************
  * Copyright (c) 2004 Actuate Corporation.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  * Contributors:
  *  Actuate Corporation  - initial API and implementation
  *******************************************************************************/
 
 package org.eclipse.birt.report.engine.emitter.html;
 
 import java.io.BufferedOutputStream;
 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.OutputStream;
 import java.io.PrintWriter;
 import java.io.StringWriter;
 import java.net.URL;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Stack;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 import org.eclipse.birt.core.exception.BirtException;
 import org.eclipse.birt.report.engine.api.EngineConstants;
 import org.eclipse.birt.report.engine.api.HTMLEmitterConfig;
 import org.eclipse.birt.report.engine.api.HTMLRenderOption;
 import org.eclipse.birt.report.engine.api.IHTMLActionHandler;
 import org.eclipse.birt.report.engine.api.IHTMLImageHandler;
 import org.eclipse.birt.report.engine.api.IImage;
 import org.eclipse.birt.report.engine.api.IRenderOption;
 import org.eclipse.birt.report.engine.api.IReportRunnable;
 import org.eclipse.birt.report.engine.api.InstanceID;
 import org.eclipse.birt.report.engine.api.RenderOptionBase;
 import org.eclipse.birt.report.engine.api.impl.Action;
 import org.eclipse.birt.report.engine.api.impl.Image;
 import org.eclipse.birt.report.engine.content.ICellContent;
 import org.eclipse.birt.report.engine.content.IColumn;
 import org.eclipse.birt.report.engine.content.IContainerContent;
 import org.eclipse.birt.report.engine.content.IContent;
 import org.eclipse.birt.report.engine.content.IDataContent;
 import org.eclipse.birt.report.engine.content.IElement;
 import org.eclipse.birt.report.engine.content.IForeignContent;
 import org.eclipse.birt.report.engine.content.IHyperlinkAction;
 import org.eclipse.birt.report.engine.content.IImageContent;
 import org.eclipse.birt.report.engine.content.ILabelContent;
 import org.eclipse.birt.report.engine.content.IPageContent;
 import org.eclipse.birt.report.engine.content.IReportContent;
 import org.eclipse.birt.report.engine.content.IRowContent;
 import org.eclipse.birt.report.engine.content.IStyle;
 import org.eclipse.birt.report.engine.content.ITableBandContent;
 import org.eclipse.birt.report.engine.content.ITableContent;
 import org.eclipse.birt.report.engine.content.ITextContent;
 import org.eclipse.birt.report.engine.css.engine.value.birt.BIRTConstants;
 import org.eclipse.birt.report.engine.emitter.ContentEmitterAdapter;
 import org.eclipse.birt.report.engine.emitter.IEmitterServices;
 import org.eclipse.birt.report.engine.executor.ExecutionContext.ElementExceptionInfo;
 import org.eclipse.birt.report.engine.executor.css.HTMLProcessor;
 import org.eclipse.birt.report.engine.i18n.EngineResourceHandle;
 import org.eclipse.birt.report.engine.i18n.MessageConstants;
 import org.eclipse.birt.report.engine.ir.DimensionType;
 import org.eclipse.birt.report.engine.ir.EngineIRConstants;
 import org.eclipse.birt.report.engine.ir.ExtendedItemDesign;
 import org.eclipse.birt.report.engine.ir.LabelItemDesign;
 import org.eclipse.birt.report.engine.ir.ListItemDesign;
 import org.eclipse.birt.report.engine.ir.MasterPageDesign;
 import org.eclipse.birt.report.engine.ir.Report;
 import org.eclipse.birt.report.engine.ir.TableBandDesign;
 import org.eclipse.birt.report.engine.ir.TableItemDesign;
 import org.eclipse.birt.report.engine.ir.TemplateDesign;
 import org.eclipse.birt.report.engine.parser.TextParser;
 import org.eclipse.birt.report.engine.presentation.ContentEmitterVisitor;
 import org.eclipse.birt.report.model.api.IResourceLocator;
 import org.eclipse.birt.report.model.api.ReportDesignHandle;
 import org.eclipse.birt.report.model.api.core.IModuleModel;
 import org.w3c.dom.Document;
 import org.w3c.dom.Element;
 import org.w3c.dom.NamedNodeMap;
 import org.w3c.dom.Node;
 import org.w3c.dom.NodeList;
 
 /**
  * <code>HTMLReportEmitter</code> is a subclass of
  * <code>ContentEmitterAdapter</code> that implements IContentEmitter
  * interface to output IARD Report ojbects to HTML file.
  * 
 * @version $Revision: 1.105 $ $Date: 2006/05/19 07:19:17 $
  */
 public class HTMLReportEmitter extends ContentEmitterAdapter
 {
 
 	/**
 	 * the output format
 	 */
 	public static final String OUTPUT_FORMAT_HTML = "HTML"; //$NON-NLS-1$
 
 	/**
 	 * the default target report file name
 	 */
 	public static final String REPORT_FILE = "report.html"; //$NON-NLS-1$
 
 	/**
 	 * the default image folder
 	 */
 	public static final String IMAGE_FOLDER = "image"; //$NON-NLS-1$
 
 	/**
 	 * output stream
 	 */
 	protected OutputStream out = null;
 
 	/**
 	 * the report content
 	 */
 	protected IReportContent report;
 
 	/**
 	 * the report runnable instance
 	 */
 	protected IReportRunnable runnable;
 
 	/**
 	 * the render options
 	 */
 	protected IRenderOption renderOption;
 
 	/**
 	 * should output the page header & footer
 	 */
 	protected boolean outputMasterPageContent = true;
 
 	/**
 	 * specifies if the HTML output is embeddable
 	 */
 	protected boolean isEmbeddable = false;
 
 	/**
 	 * the url encoding
 	 */
 	protected String urlEncoding = null;
 
 	/**
 	 * specified the current page number, starting from 0
 	 */
 	protected int pageNo = 0;
 
 	/**
 	 * the <code>HTMLWriter<code> object that is used to output HTML content
 	 */
 	protected HTMLWriter writer;
 
 	/**
 	 * the image reder context
 	 */
 	protected Object renderContext;
 
 	/**
 	 * indicates that the styled element is hidden or not
 	 */
 	protected Stack stack = new Stack( );
 
 	/**
 	 * An Log object that <code>HTMLReportEmitter</code> use to log the error,
 	 * debug, information messages.
 	 */
 	protected static Logger logger = Logger.getLogger( HTMLReportEmitter.class
 			.getName( ) );
 
 	/**
 	 * html image handler
 	 */
 	protected IHTMLImageHandler imageHandler;
 
 	/**
 	 * html action handler
 	 */
 	protected IHTMLActionHandler actionHandler;
 
 	/**
 	 * emitter services
 	 */
 	protected IEmitterServices services;
 
 	/**
 	 * The <code>tagStack</code> that stores the tag names to be closed in
 	 * <code>endContainer()</code>.
 	 */
 	private Stack tagStack = new Stack( );
 
 	/**
 	 * display type of Block
 	 */
 	protected static final int DISPLAY_BLOCK = 1;
 
 	/**
 	 * display type of Inline
 	 */
 	protected static final int DISPLAY_INLINE = 2;
 
 	/**
 	 * display type of Inline-Block
 	 */
 	protected static final int DISPLAY_INLINE_BLOCK = 4;
 
 	/**
 	 * display type of none
 	 */
 	protected static final int DISPLAY_NONE = 8;
 
 	/**
 	 * display flag which contains all display types
 	 */
 	protected static final int DISPLAY_FLAG_ALL = 0xffff;
 
 	/**
 	 * content visitor that is used to handle page header/footer
 	 */
 	protected ContentEmitterVisitor contentVisitor;
 
 	/**
 	 * the constructor
 	 */
 	public HTMLReportEmitter( )
 	{
 		contentVisitor = new ContentEmitterVisitor( this );
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#initialize(org.eclipse.birt.report.engine.emitter.IEmitterServices)
 	 */
 	public void initialize( IEmitterServices services )
 	{
 		this.services = services;
 
 		Object fd = services.getOption( RenderOptionBase.OUTPUT_FILE_NAME );
 		File file = null;
 		try
 		{
 			if ( fd != null )
 			{
 				file = new File( fd.toString( ) );
 				File parent = file.getParentFile( );
 				if ( parent != null && !parent.exists( ) )
 				{
 					parent.mkdirs( );
 				}
 				out = new BufferedOutputStream( new FileOutputStream( file ) );
 			}
 		}
 		catch ( FileNotFoundException e )
 		{
 			logger.log( Level.WARNING, e.getMessage( ), e );
 		}
 
 		if ( out == null )
 		{
 			Object value = services.getOption( RenderOptionBase.OUTPUT_STREAM );
 			if ( value != null && value instanceof OutputStream )
 			{
 				out = (OutputStream) value;
 			}
 			else
 			{
 				try
 				{
 					// FIXME
 					file = new File( REPORT_FILE );
 					out = new BufferedOutputStream( new FileOutputStream( file ) );
 				}
 				catch ( FileNotFoundException e )
 				{
 					// FIXME
 					logger.log( Level.SEVERE, e.getMessage( ), e );
 				}
 			}
 		}
 
 		Object emitterConfig = services.getEmitterConfig( ).get( "html" ); //$NON-NLS-1$
 		if ( emitterConfig != null
 				&& emitterConfig instanceof HTMLEmitterConfig )
 		{
 			imageHandler = ( (HTMLEmitterConfig) emitterConfig )
 					.getImageHandler( );
 			actionHandler = ( (HTMLEmitterConfig) emitterConfig )
 					.getActionHandler( );
 		}
 
 		Object im = services.getOption( HTMLRenderOption.IMAGE_HANDLER );
 		if ( im != null && im instanceof IHTMLImageHandler )
 		{
 			imageHandler = (IHTMLImageHandler) im;
 		}
 
 		Object ac = services.getOption( HTMLRenderOption.ACTION_HANDLER );
 		if ( ac != null && ac instanceof IHTMLActionHandler )
 		{
 			actionHandler = (IHTMLActionHandler) ac;
 		}
 
 		if ( services.getRenderContext( ) instanceof Map )
 		{
 			renderContext = ( (Map) services.getRenderContext( ) )
 					.get( EngineConstants.APPCONTEXT_HTML_RENDER_CONTEXT );
 		}
 		else
 		{
 			renderContext = services.getRenderContext( ); // Handle the
 			// old-style render
 			// context, follow
 			// the same code
 			// path as before.
 		}
 
 		renderOption = services.getRenderOption( );
 		runnable = services.getReportRunnable( );
 		if ( renderOption != null && renderOption instanceof HTMLRenderOption )
 		{
 			HTMLRenderOption htmlOption = (HTMLRenderOption) renderOption;
 			isEmbeddable = htmlOption.getEmbeddable( );
 			HashMap options = renderOption.getOutputSetting( );
 			if ( options != null )
 			{
 				urlEncoding = (String) options
 						.get( HTMLRenderOption.URL_ENCODING );
 			}
 			outputMasterPageContent = htmlOption.getMasterPageContent( );
 			IHTMLActionHandler actHandler = htmlOption.getActionHandle( );
 			if ( ac != null )
 			{
 				actionHandler = actHandler;
 			}
 		}
 
 		writer = new HTMLWriter( );
 	}
 
 	/**
 	 * @return the <code>Report</code> object.
 	 */
 	public IReportContent getReport( )
 	{
 		return report;
 	}
 
 	/**
 	 * Pushes the Boolean indicating whether or not the item is hidden according
 	 * to the style
 	 * 
 	 * @param style
 	 */
 	public void push( IStyle style )
 	{
 		stack.push( new Boolean( peek( style ) ) );
 	}
 
 	/**
 	 * Pops the element of the stack
 	 * 
 	 * @return the boolean indicating whether or not the item is hidden
 	 */
 	public boolean pop( )
 	{
 		return ( (Boolean) stack.pop( ) ).booleanValue( );
 	}
 
 	/**
 	 * Peeks the element of stack
 	 * 
 	 * @param style
 	 * @return the boolean indicating whether or not the item is hidden
 	 */
 	public boolean peek( IStyle style )
 	{
 		boolean isHidden = false;
 		if ( !stack.empty( ) )
 		{
 			isHidden = ( (Boolean) stack.peek( ) ).booleanValue( );
 		}
 		if ( !isHidden )
 		{
 			String formats = style.getVisibleFormat( );
 			if ( formats != null
 					&& ( formats.indexOf( EngineIRConstants.FORMAT_TYPE_VIEWER ) >= 0 || formats
 							.indexOf( BIRTConstants.BIRT_ALL_VALUE ) >= 0 ) )
 			{
 				isHidden = true;
 			}
 		}
 		return isHidden;
 	}
 
 	/**
 	 * Checks if the current item is hidden
 	 * 
 	 * @return a boolean value
 	 */
 	public boolean isHidden( )
 	{
 		return ( (Boolean) stack.peek( ) ).booleanValue( );
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#getOutputFormat()
 	 */
 	public String getOutputFormat( )
 	{
 		return OUTPUT_FORMAT_HTML;
 	}
 
 	/**
 	 * Fixes a PNG problem related to transparency. See
 	 * http://homepage.ntlworld.com/bobosola/ for detail.
 	 */
 	protected void fixTransparentPNG( )
 	{
 		writer.writeCode( "<!--[if gte IE 5.5000]>" ); //$NON-NLS-1$
 		writer
 				.writeCode( "   <script language=\"JavaScript\"> var ie55up = true </script>" ); //$NON-NLS-1$
 		writer.writeCode( "<![endif]-->" ); //$NON-NLS-1$
 		writer.writeCode( "<script language=\"JavaScript\">" ); //$NON-NLS-1$
 		writer
 				.writeCode( "   function fixPNG(myImage) // correctly handle PNG transparency in Win IE 5.5 or higher." ); //$NON-NLS-1$
 		writer.writeCode( "      {" ); //$NON-NLS-1$
 		writer.writeCode( "      if (window.ie55up)" ); //$NON-NLS-1$
 		writer.writeCode( "         {" ); //$NON-NLS-1$
 		writer
 				.writeCode( "         var imgID = (myImage.id) ? \"id='\" + myImage.id + \"' \" : \"\"" ); //$NON-NLS-1$
 		writer
 				.writeCode( "         var imgClass = (myImage.className) ? \"class='\" + myImage.className + \"' \" : \"\"" ); //$NON-NLS-1$
 		writer
 				.writeCode( "         var imgTitle = (myImage.title) ? \"title='\" + myImage.title + \"' \" : \"title='\" + myImage.alt + \"' \"" ); //$NON-NLS-1$
 		writer
 				.writeCode( "         var imgStyle = \"display:inline-block;\" + myImage.style.cssText" ); //$NON-NLS-1$
 		writer
 				.writeCode( "         var strNewHTML = \"<span \" + imgID + imgClass + imgTitle" ); //$NON-NLS-1$
 		writer
 				.writeCode( "         strNewHTML += \" style=\\\"\" + \"width:\" + myImage.width + \"px; height:\" + myImage.height + \"px;\" + imgStyle + \";\"" ); //$NON-NLS-1$
 		writer
 				.writeCode( "         strNewHTML += \"filter:progid:DXImageTransform.Microsoft.AlphaImageLoader\"" ); //$NON-NLS-1$
 		writer
 				.writeCode( "         strNewHTML += \"(src=\\'\" + myImage.src + \"\\', sizingMethod='scale');\\\"></span>\"" ); //$NON-NLS-1$
 		writer.writeCode( "         myImage.outerHTML = strNewHTML" ); //$NON-NLS-1$
 		writer.writeCode( "         }" ); //$NON-NLS-1$
 		writer.writeCode( "      }" ); //$NON-NLS-1$
 		writer.writeCode( "</script>" ); //$NON-NLS-1$
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#start(org.eclipse.birt.report.engine.content.IReportContent)
 	 */
 	public void start( IReportContent report )
 	{
 		logger.log( Level.FINE, "[HTMLReportEmitter] Start emitter." ); //$NON-NLS-1$
 
 		this.report = report;
 		writer.open( out, "UTF-8" ); //$NON-NLS-1$
 		
 		// If it is the body style and htmlRtLFlag has been set true, 
 		// remove the text-align included in the style.
 		if ( renderOption != null && renderOption instanceof HTMLRenderOption )
 		{
 			HTMLRenderOption htmlOption = (HTMLRenderOption) renderOption;
 			boolean htmlRtLFlag = htmlOption.getHtmlRtLFlag( );
 			if ( htmlRtLFlag )
 			{
 				String reportStyleName = report == null ? null : report.getDesign( )
 						.getRootStyleName( );
 				if ( reportStyleName != null )
 				{
 					IStyle style =  report.findStyle( reportStyleName );
 					style.removeProperty( "text-align" );
 				}
 			}
 		}
 		
 		if ( isEmbeddable )
 		{
 			fixTransparentPNG( );
 
 			writer.openTag( HTMLTags.TAG_DIV );
 
 			String reportStyleName = report == null ? null : report.getDesign( )
 					.getRootStyleName( );
 			if ( reportStyleName != null )
 			{
 				IStyle style =  report.findStyle( reportStyleName );
 				StringBuffer styleBuffer = new StringBuffer( );
 				AttributeBuilder.buildStyle( styleBuffer, style, this, false );
 				writer.attribute( HTMLTags.ATTR_STYLE, styleBuffer.toString( ) );
 			}
 
 			return;
 		}
 
 		writer.startWriter( );
 		writer.openTag( HTMLTags.TAG_HTML );
 		writer.openTag( HTMLTags.TAG_HEAD );
 		
 		// write the title of the report in html.
 		Report reportDesign = null;	
 		if ( report != null )
 		{
 			reportDesign = report.getDesign( );	
 			ReportDesignHandle designHandle = reportDesign.getReportDesign( );
 			String title = designHandle.getStringProperty(  IModuleModel.TITLE_PROP );
 			if ( title == null )
 			{
 				// set the default title
 				if ( renderOption != null && renderOption instanceof HTMLRenderOption )
 				{
 					HTMLRenderOption htmlOption = (HTMLRenderOption) renderOption;
 					title = htmlOption.getHtmlTitle( );					
 				}
 			}
 			if ( title != null )
 			{
 				writer.openTag( HTMLTags.TAG_TITLE );
 				writer.text( title );
 				writer.closeTag( HTMLTags.TAG_TITLE );
 			}
 		}
 				
 		writer.openTag( HTMLTags.TAG_META );
 		writer.attribute( HTMLTags.ATTR_HTTP_EQUIV, "Content-Type" ); //$NON-NLS-1$ 
 		writer.attribute( HTMLTags.ATTR_CONTENT, "text/html; charset=UTF-8" ); //$NON-NLS-1$ 
 		writer.closeNoEndTag( );
 
 		writer.openTag( HTMLTags.TAG_STYLE );
 		writer.attribute( HTMLTags.ATTR_TYPE, "text/css" ); //$NON-NLS-1$
 
 		// output general styles
 		writer.style( "*", "vertical-align: baseline;", true ); //$NON-NLS-1$ //$NON-NLS-2$ 
 
 		IStyle style;
 		StringBuffer styleBuffer = new StringBuffer( );
 		if ( report == null )
 		{
 			logger.log( Level.WARNING,
 					"[HTMLReportEmitter] Report object is null." ); //$NON-NLS-1$
 		}
 		else
 		{
 			for ( int n = 0; n < reportDesign.getStyleCount( ); n++ )
 			{
 				styleBuffer.delete( 0, styleBuffer.capacity( ) );
 				style = (IStyle) reportDesign.getStyle( n );	
 				AttributeBuilder.buildStyle( styleBuffer, style, this, true );
 				writer.style( Report.PREFIX_STYLE_NAME + n, styleBuffer
 						.toString( ), false );
 			}
 		}
 
 		writer.closeTag( HTMLTags.TAG_STYLE );
 		fixTransparentPNG( );
 		writer.closeTag( HTMLTags.TAG_HEAD );
 
 		String reportStyleName = report == null ? null : report.getDesign( )
 				.getRootStyleName( );
 		writer.openTag( HTMLTags.TAG_BODY );
 		if ( reportStyleName != null )
 		{
 			writer.attribute( HTMLTags.ATTR_CLASS, reportStyleName );
 		}
 	}
 
 	private void appendErrorMessage( int index, ElementExceptionInfo info )
 	{
 		String errorId = "document.getElementById('error_detail" + index + "')";
 		String errorIcon = "document.getElementById('error_icon" + index + "')";
 		String onClick = "if (" + errorId + ".style.display == 'none') { "
 				+ errorIcon + ".innerHTML = '-'; " + errorId
 				+ ".style.display = 'block'; }" + "else { " + errorIcon
 				+ ".innerHTML = '+'; " + errorId + ".style.display = 'none'; }";
 
 		EngineResourceHandle rc = EngineResourceHandle.getInstance( );
 		writer.writeCode( "			<div>" );
 		writer.writeCode( "				<span id=\"error_icon" + index
 				+ "\"  style=\"cursor:pointer\" onclick=\"" + onClick
 				+ "\" > + </span>" );
 		writer.writeCode( "				<span  id=\"error_title\">" );
 		writer.text( rc.getMessage( MessageConstants.REPORT_ERROR_MESSAGE,
 				new Object[]{info.getType( ), info.getElementInfo( )} ), false );
 		writer.writeCode( "</span>" );
 		writer.writeCode( "				<pre id=\"error_detail" + index
 				+ "\" style=\"display:none;\" >" );
 		ArrayList errorList = info.getErrorList( );
 		ArrayList countList = info.getCountList( );
 		for ( int i = 0; i < errorList.size( ); i++ )
 		{
 			BirtException ex = (BirtException) errorList.get( i );
 
 			String messageTitle = rc.getMessage(
 					MessageConstants.REPORT_ERROR_ID, new Object[]{
 							new Integer( i ), ex.getErrorCode( ),
 							countList.get( i )} );
 			String detailTag = rc
 					.getMessage( MessageConstants.REPORT_ERROR_DETAIL );
 			String messageBody = getDetailMessage( ex );
 			boolean indent = writer.isIndent( );
 			writer.setIndent( false );
 			writer.text( messageTitle, false );
 			writer.writeCode( "\r\n" );
 			writer.text( detailTag, false );
 			writer.text( messageBody, false );
 			writer.setIndent( indent );
 		}
 		writer.writeCode( "				</pre>" );
 		writer.writeCode( "		</div>" ); //$NON-NLS-1$
 	}
 
 	private String getDetailMessage( Throwable t )
 	{
 		StringWriter out = new StringWriter( );
 		PrintWriter print = new PrintWriter( out );
 		try
 		{
 			t.printStackTrace( print );
 		}
 		catch ( Throwable ex )
 		{
 		}
 		print.flush( );
 		return out.getBuffer( ).toString( );
 	}
 
 	protected boolean outputErrors( List errors )
 	{
 		// Outputs the error message at the end of the report
 		if ( errors != null && !errors.isEmpty( ) )
 		{
 			writer.writeCode( "	<hr style=\"color:red\"/>" );
 			writer.writeCode( "	<div style=\"color:red\">" );
 			writer.writeCode( "		<div>" );
 			writer.text( EngineResourceHandle.getInstance( ).getMessage(
 					MessageConstants.ERRORS_ON_REPORT_PAGE ), false );
 			writer.writeCode( "</div>" );//$NON-NLS-1$
 
 			Iterator it = errors.iterator( );
 			int index = 0;
 			while ( it.hasNext( ) )
 			{
 				appendErrorMessage( index++, (ElementExceptionInfo) it.next( ) );
 			}
 			writer.writeCode( "</div>" );
 			return true;
 		}
 		return false;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#end(org.eclipse.birt.report.engine.content.IReportContent)
 	 */
 	public void end( IReportContent report )
 	{
 		logger.log( Level.FINE, "[HTMLReportEmitter] End body." ); //$NON-NLS-1$
 		if ( report != null )
 		{
 			List errors = report.getErrors( );
 			if ( errors != null && !errors.isEmpty( ) )
 			{
 				outputErrors( errors );
 			}
 		}
 		if ( !isEmbeddable )
 		{
 			writer.closeTag( HTMLTags.TAG_BODY );
 			writer.closeTag( HTMLTags.TAG_HTML );
 		}
 		else
 		{
 			writer.closeTag( HTMLTags.TAG_DIV );
 		}
 
 		writer.endWriter( );
 		writer.close( );
 		if ( out != null )
 		{
 			try
 			{
 				out.close( );
 			}
 			catch ( IOException e )
 			{
 				logger.log( Level.WARNING, e.getMessage( ), e );
 			}
 		}
 	}
 	/*** 
      * output the style of page header/footer/body.
 	 * In embedded HTML, we output the style as inline style, otherwise
      * use the style name as the "class"
 	 * @param styleName name of the style
 	 * @param style style object
 	 */
 	public void handlePageStyle( String styleName, IStyle style )
 	{
 		if ( isEmbeddable )
 		{
 			StringBuffer styleBuffer = new StringBuffer( );
 			AttributeBuilder.buildStyle( styleBuffer, style, this );
 			writer.attribute( HTMLTags.ATTR_STYLE, styleBuffer.toString( ) );
 		}
 		else
 		{
 			setStyleName( styleName );					
 		}	
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#startPage(org.eclipse.birt.report.engine.content.IPageContent)
 	 */
 	public void startPage( IPageContent page )
 	{
 		pageNo++;
 		
 		if ( pageNo > 1 && outputMasterPageContent == false )
 		{
 			writer.openTag( "hr" );
 			writer.closeTag( "hr" );
 		}
 
 		// out put the page tag
 		writer.openTag( HTMLTags.TAG_DIV );
 		if ( renderOption != null && renderOption instanceof HTMLRenderOption )
 		{
 			HTMLRenderOption htmlOption = (HTMLRenderOption) renderOption;
 			boolean htmlRtLFlag = htmlOption.getHtmlRtLFlag( );
 			if ( htmlRtLFlag )
 			{
 				writer.attribute( HTMLTags.ATTR_HTML_DIR, "RTL" );
 			}
 		}
 		if ( pageNo > 1 )
 		{
 			writer.attribute( HTMLTags.ATTR_STYLE, "page-break-before: always;" );
 		}
 
 		//output page header
 		if ( page != null )
 		{
 			if ( outputMasterPageContent )
 			{
 				//output DIV for page header
 				writer.openTag( HTMLTags.TAG_DIV );		
 				handlePageStyle( page.getPageHeader().getStyleClass( ), page.getPageHeader().getStyle( ) );
 				
 				contentVisitor.visitList( page.getHeader( ), null );
 				
 				//close the page header
 				writer.closeTag( HTMLTags.TAG_DIV );
 			}
 			}
 		
 		// start output the page body , with the body style 
 		writer.openTag( HTMLTags.TAG_DIV );
 
 		if ( page != null )
 		{
 			IContent pageBody = page.getPageBody( );
 			IStyle bodyStyle = pageBody.getStyle( );
 			String bodyStyleName = pageBody.getStyleClass( );
 			
 			Object genBy = page.getGenerateBy( );
 			if ( genBy instanceof MasterPageDesign )
 			{
 				MasterPageDesign masterPage = (MasterPageDesign) genBy;
 				StringBuffer styleBuffer = new StringBuffer( );
 				if ( isEmbeddable )
 				{
 					AttributeBuilder.buildStyle( styleBuffer, bodyStyle, this );
 				}
 				else
 				{
 					setStyleName( bodyStyleName );
 				}
 				AttributeBuilder.buildSize( styleBuffer,
 						HTMLTags.ATTR_MIN_HEIGHT,
 						masterPage.getPageHeight( ) );
 				writer.attribute( HTMLTags.ATTR_STYLE, styleBuffer.toString( ) );
 			}
 		}
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#endPage(org.eclipse.birt.report.engine.content.IPageContent)
 	 */
 	public void endPage( IPageContent page )
 	{
 		
 		logger.log( Level.FINE, "[HTMLReportEmitter] End page." ); //$NON-NLS-1$
 
 		//close the page body (DIV)		
 		writer.closeTag( HTMLTags.TAG_DIV );
 		
 		//output page footer
 		if ( page != null )
 		{
 			if ( outputMasterPageContent )
 			{
 				//start output the page footer
 				writer.openTag( HTMLTags.TAG_DIV );				
 				handlePageStyle( page.getPageFooter().getStyleClass( ), page.getPageFooter().getStyle( ) );			
 				
 				contentVisitor.visitList( page.getFooter( ), null );
 								
 				//close the page footer
 				writer.closeTag( HTMLTags.TAG_DIV );
 			}
 		}
 		
 		// close the page tag ( DIV )
 		writer.closeTag( HTMLTags.TAG_DIV );
 	}	
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#startTable(org.eclipse.birt.report.engine.content.ITableContent)
 	 */
 	public void startTable( ITableContent table )
 	{
 		assert table != null;
 
 		IStyle mergedStyle = table.getStyle( );
 		push( mergedStyle );
 		if ( isHidden( ) )
 		{
 			return;
 		}
 		logger.log( Level.FINE, "[HTMLTableEmitter] Start table" ); //$NON-NLS-1$
 
 		DimensionType x = table.getX( );
 		DimensionType y = table.getY( );
 		StringBuffer styleBuffer = new StringBuffer( );
 
 		addDefaultTableStyles( styleBuffer );
 
 		writer.openTag( HTMLTags.TAG_TABLE );
 
 		// style string
 		setStyleName( table.getStyleClass( ) );
 		int display = checkElementType( x, y, mergedStyle, styleBuffer );
 		setDisplayProperty( display, DISPLAY_INLINE, styleBuffer );
 
 		handleShrink( DISPLAY_BLOCK, mergedStyle, table.getHeight( ), table
 				.getWidth( ), styleBuffer );
 		handleStyle( table, styleBuffer );
 
 		// bookmark
 		String bookmark = table.getBookmark( );
 		if ( bookmark == null )
 		{
 			bookmark = generateUniqueID( );
 			table.setBookmark( bookmark );
 		}
 		setBookmark( null, bookmark );
 
 		// Add it to active id list, and output type ��iid to html
 		setActiveIDTypeIID(table);
 
 		// table caption
 		String caption = table.getCaption( );
 		if ( caption != null && caption.length( ) > 0 )
 		{
 			writer.openTag( HTMLTags.TAG_CAPTION );
 			writer.text( caption );
 			writer.closeTag( HTMLTags.TAG_CAPTION );
 		}
 
 		// include select handle table
 		if ( renderOption != null && renderOption instanceof HTMLRenderOption )
 		{
 			HTMLRenderOption htmlOption = (HTMLRenderOption) renderOption;
 		
 			Object generateBy = table.getGenerateBy( );
 			if ( htmlOption.getIncludeSelectionHandle( )
 					&& generateBy instanceof TableItemDesign )
 			{
 				startSelectHandleTable( );
 			}
 		}
 
 		writeColumns( table );
 	}
 
 	/**
 	 * bug128480
 	 * render table with a highlight handle
 	 */	
 	protected void startSelectHandleTable( )
 	{
 		writer.openTag( HTMLTags.TAG_COL );
		writer.attribute( HTMLTags.ATTR_STYLE, "width:18px;background-color:#cccccc" );
 		writer.closeTag( HTMLTags.TAG_COL );
 		writer.openTag( HTMLTags.TAG_COL );
 		writer.attribute( HTMLTags.ATTR_STYLE, "width:95%" );
 		writer.closeTag( HTMLTags.TAG_COL );
 		writer.openTag( HTMLTags.TAG_TR );
 		writer.openTag( HTMLTags.TAG_TD );
 		writer.closeTag( HTMLTags.TAG_TD );
 		writer.openTag( HTMLTags.TAG_TD );
 		writer.openTag( HTMLTags.TAG_TABLE );
 		writer.attribute( HTMLTags.ATTR_STYLE, "width:100%" );
 	}
 	
 	protected void endSelectHandleTable( )
 	{
 		writer.closeTag( HTMLTags.TAG_TABLE );
 		writer.closeTag( HTMLTags.TAG_TD );
 		writer.closeTag( HTMLTags.TAG_TR );
 	}
 
 	protected void writeColumns( ITableContent table )
 	{
 		for ( int i = 0; i < table.getColumnCount( ); i++ )
 		{
 			IColumn column = table.getColumn( i );
 			
 			if ( isColumnHidden( column ) )
 			{
 				continue;
 			}
 
 			writer.openTag( HTMLTags.TAG_COL );
 
 			setStyleName( column.getStyleClass( ) );
 
 			// width
 			StringBuffer styleBuffer = new StringBuffer( );
 			AttributeBuilder.buildSize( styleBuffer, HTMLTags.ATTR_WIDTH,
 					column.getWidth( ) );
 			if ( isEmbeddable )
 			{
 				// output in-line style
 				String styleName = column.getStyleClass( );
 				if ( styleName != null )
 				{
 					IStyle style = report.findStyle( styleName );
 					if ( style != null )
 					{
 						AttributeBuilder.buildStyle( styleBuffer, style, this );
 					}
 				}
 			}
 			writer.attribute( HTMLTags.ATTR_STYLE, styleBuffer.toString( ) );
 			
 			// Instance ID
 			InstanceID iid = column.getInstanceID( );			
 			if ( iid != null )
 			{
 				writer.attribute( "iid", iid.toString( ) );
 			}
 			
 			writer.closeNoEndTag( );
 		}
 	}
 	
 	/**
 	 * Pushes the Boolean indicating whether or not the item is hidden according	 
 	 * @param hidden
 	 */
 	public void push( boolean hidden )
 	{		
 		boolean isHidden = false;
 		if ( !stack.empty( ) )
 		{
 			isHidden = ( (Boolean) stack.peek( ) ).booleanValue( );
 		}
 		if ( !isHidden )
 		{
 			isHidden = hidden;
 		}
 		stack.push( new Boolean( isHidden ) );		
 	}
 	
 	/**
 	 * check whether to hide the column.
 	 * @param column
 	 * @return
 	 */
 	private boolean isColumnHidden( IColumn column )
 	{
 		String formats = column.getVisibleFormat( );
 		if ( formats != null
 				&& ( formats.indexOf( EngineIRConstants.FORMAT_TYPE_VIEWER ) >= 0 || formats
 						.indexOf( BIRTConstants.BIRT_ALL_VALUE ) >= 0 ) )
 		{
 			return true;
 		}
 		return false;
 	}
 	
 	/**
 	 * get the new colspan of the cell 
 	 * by checking the relational columns.
 	 * @param column
 	 * @return
 	 */
 	private int getNewColSpan( ICellContent cell )
 	{
 		int columnNumber = cell.getColumn( );
 		int colSpan = cell.getColSpan( );
 		ITableContent table = getTableOfCell(cell);
 		if (table == null)
 		{
 			return colSpan;
 		}
 		int newColSpan = colSpan;
 		for ( int i = 0; i < colSpan; i++ )
 		{
 			IColumn column = table.getColumn( columnNumber + i );
 			if ( isColumnHidden( column ) )
 			{
 				newColSpan--;
 			}
 		}
 		return newColSpan;
 	}
 	
 	private ITableContent getTableOfCell( ICellContent cell )
 	{
 		IElement parent = cell.getParent( );
 		while ( parent != null )
 		{
 			if ( parent instanceof ITableContent )
 			{
 				return (ITableContent) parent;
 			}
 			parent = parent.getParent( );
 		}
 		return null;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#endTable(org.eclipse.birt.report.engine.content.ITableContent)
 	 */
 	public void endTable( ITableContent table )
 	{
 		if ( pop( ) )
 		{
 			return;
 		}
 
 		//	include select handle table
 		if ( renderOption != null && renderOption instanceof HTMLRenderOption )
 		{
 			HTMLRenderOption htmlOption = (HTMLRenderOption) renderOption;
 			Object generateBy = table.getGenerateBy();
 			
 			if ( htmlOption.getIncludeSelectionHandle( )
 					&& generateBy instanceof TableItemDesign )
 			{
 				endSelectHandleTable( );
 			}
 		}
 				
 		writer.closeTag( HTMLTags.TAG_TABLE );
 
 		logger.log( Level.FINE, "[HTMLTableEmitter] End table" ); //$NON-NLS-1$
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#startTableHeader(org.eclipse.birt.report.engine.content.ITableBandContent)
 	 */
 	public void startTableHeader( ITableBandContent band )
 	{
 		if ( isHidden( ) )
 		{
 			return;
 		}
 		writer.openTag( HTMLTags.TAG_THEAD );
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#endTableHeader(org.eclipse.birt.report.engine.content.ITableBandContent)
 	 */
 	public void endTableHeader( ITableBandContent band )
 	{
 		if ( isHidden( ) )
 		{
 			return;
 		}
 		writer.closeTag( HTMLTags.TAG_THEAD );
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#startTableBody(org.eclipse.birt.report.engine.content.ITableBandContent)
 	 */
 	public void startTableBody( ITableBandContent band )
 	{
 		if ( isHidden( ) )
 		{
 			return;
 		}
 		writer.openTag( HTMLTags.TAG_TBODY );
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#endTableBody(org.eclipse.birt.report.engine.content.ITableBandContent)
 	 */
 	public void endTableBody( ITableBandContent band )
 	{
 		if ( isHidden( ) )
 		{
 			return;
 		}
 		writer.closeTag( HTMLTags.TAG_TBODY );
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#startTableFooter(org.eclipse.birt.report.engine.content.ITableBandContent)
 	 */
 	public void startTableFooter( ITableBandContent band )
 	{
 		if ( isHidden( ) )
 		{
 			return;
 		}
 		writer.openTag( HTMLTags.TAG_TFOOT );
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#endTableFooter(org.eclipse.birt.report.engine.content.ITableBandContent)
 	 */
 	public void endTableFooter( ITableBandContent band )
 	{
 		if ( isHidden( ) )
 		{
 			return;
 		}
 		writer.closeTag( HTMLTags.TAG_TFOOT );
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#startRow(org.eclipse.birt.report.engine.content.IRowContent)
 	 */
 	public void startRow( IRowContent row )
 	{
 		assert row != null;
 		IStyle mergedStyle = row.getStyle( );
 		push( mergedStyle );
 		if ( isHidden( ) )
 		{
 			return;
 		}
 		writer.openTag( HTMLTags.TAG_TR );
 
 		setStyleName( row.getStyleClass( ) );
 
 		// bookmark
 		setBookmark( null, row.getBookmark( ) );
 
 		StringBuffer styleBuffer = new StringBuffer( );
 
 		AttributeBuilder.buildSize( styleBuffer, HTMLTags.ATTR_HEIGHT, row
 				.getHeight( ) ); //$NON-NLS-1$
 		
 		setRowType( HTMLTags.ATTR_TYPE, row );
 		writer.attribute( HTMLTags.ATTR_GOURP_ID, row.getGroupId( ) );
 		writer.attribute( HTMLTags.ATTR_ROW_TYPE, row.getRowType( ) );
 		handleStyle( row, styleBuffer );
 	}
 	
 	protected void setRowType( String tagName, IRowContent rowContent )
 	{
 		if ( tagName != null && rowContent != null )
 		{
 			int bandType = rowContent.getRowType( );
 			if ( bandType == TableBandDesign.TABLE_HEADER )
 			{
 				writer.attribute( tagName, "wrth" );
 			}
 			else if ( bandType == TableBandDesign.TABLE_FOOTER )
 			{
 				writer.attribute( tagName, "wrtf" );
 			}
 			else if ( bandType == TableBandDesign.GROUP_HEADER )
 			{
 				writer.attribute( tagName, "wrgh" + rowContent.getGroupLevel( ) );
 			}
 			else if ( bandType == TableBandDesign.GROUP_FOOTER )
 			{
 				writer.attribute( tagName, "wrgf" + rowContent.getGroupLevel( ) );
 			}
 		}
 	}
 	
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#endRow(org.eclipse.birt.report.engine.content.IRowContent)
 	 */
 	public void endRow( IRowContent row )
 	{
 		if ( pop( ) )
 		{
 			return;
 		}
 
 		// assert currentData != null;
 		//
 		// currentData.adjustCols( );
 		writer.closeTag( HTMLTags.TAG_TR );
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#startCell(org.eclipse.birt.report.engine.content.ICellContent)
 	 */
 	public void startCell( ICellContent cell )
 	{				
 		int colSpan = getNewColSpan( cell );
 		if ( colSpan < 1 )
 		{
 			push( true );
 			return;
 		}
 		push( false );
 		
 		if ( isHidden( ) )
 		{
 			return;
 		}			
 		
 		logger.log( Level.FINE, "[HTMLTableEmitter] Start cell." ); //$NON-NLS-1$
 			
 		// output 'td' tag
 		writer.openTag( HTMLTags.TAG_TD ); //$NON-NLS-1$
 
 		// set the 'name' property
 		setStyleName( cell.getStyleClass( ) );
 
 		// colspan
 		if ( colSpan > 1 )
 		{
 			writer.attribute( HTMLTags.ATTR_COLSPAN, colSpan );
 		}
 
 		// rowspan
 		if ( ( cell.getRowSpan( ) ) > 1 )
 		{
 			writer.attribute( HTMLTags.ATTR_ROWSPAN, cell.getRowSpan( ) );
 		}
 
 		// vertical align can only be used with tabelCell/Inline Element
 		StringBuffer styleBuffer = new StringBuffer( );
 		IStyle mergedStyle = cell.getStyle( );
 		String vAlign = null;
 		String textAlign = null;
 		if ( mergedStyle != null )
 		{
 			vAlign = mergedStyle.getVerticalAlign( );
 			textAlign = mergedStyle.getTextAlign( );
 		}
 		if ( vAlign == null )
 		{
 			IStyle cs = cell.getComputedStyle( );
 			vAlign = cs.getVerticalAlign( );
 			styleBuffer.append( "vertical-align: " );
 			styleBuffer.append( vAlign );
 			styleBuffer.append( ";" );
 		}
 		if ( textAlign == null )
 		{
 			IStyle cs = cell.getComputedStyle( );
 			textAlign = cs.getTextAlign( );
 			styleBuffer.append( "text-align: " );
 			styleBuffer.append( textAlign );
 			styleBuffer.append( ";" );
 		}
 
 		handleStyle( cell, styleBuffer );
 		if ( cell.isStartOfGroup( ) )
 		{
 			//	include select handle table
 			if ( renderOption != null && renderOption instanceof HTMLRenderOption )
 			{
 				HTMLRenderOption htmlOption = (HTMLRenderOption) renderOption;
 				if ( htmlOption.getIncludeSelectionHandle( ) )
 				{
 					//TODO: change the output tag
 					writer.openTag( HTMLTags.TAG_DIV );
 					//TODO: change the javascript name
 					writer.attribute( HTMLTags.ATTR_ONCLICK, "_collasp_group()" );
 					writer.text( " + " );
 					writer.closeTag( HTMLTags.TAG_DIV );
 				}
 			}
 		}
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#endCell(org.eclipse.birt.report.engine.content.ICellContent)
 	 */
 	public void endCell( ICellContent cell )
 	{
 		if ( pop( ) )
 		{
 			return;
 		}
 		logger.log( Level.FINE, "[HTMLReportEmitter] End cell." ); //$NON-NLS-1$
 		
 		writer.closeTag( HTMLTags.TAG_TD );
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#startContainer(org.eclipse.birt.report.engine.content.IContainerContent)
 	 */
 	public void startContainer( IContainerContent container )
 	{
 		IStyle mergedStyle = container.getStyle( );
 		push( mergedStyle );
 		if ( isHidden( ) )
 		{
 			return;
 		}
 		logger.log( Level.FINE, "[HTMLReportEmitter] Start container" ); //$NON-NLS-1$
 
 		String tagName;
 		StringBuffer styleBuffer = new StringBuffer( );
 		DimensionType x = container.getX( );
 		DimensionType y = container.getY( );
 		DimensionType width = container.getWidth( );
 		DimensionType height = container.getHeight( );
 
 		int display = checkElementType( x, y, width, height, mergedStyle,
 				styleBuffer );
 		tagName = openTagByType( display, DISPLAY_FLAG_ALL );
 		tagStack.push( tagName );
 
 		// class
 		setStyleName( container.getStyleClass( ) );
 
 		// bookmark
 		String bookmark = container.getBookmark( );
 
 		if ( bookmark == null )
 		{
 			bookmark = generateUniqueID( );
 			container.setBookmark( bookmark );
 		}
 
 		setBookmark( tagName, bookmark );
 		
 		setActiveIDTypeIID( container );
 
 		// output style
 		// if ( x == null && y == null )
 		// {
 		// styleBuffer.append( "position: relative;" ); //$NON-NLS-1$
 		// }
 
 		setDisplayProperty( display, DISPLAY_INLINE_BLOCK, styleBuffer );
 
 		handleShrink( display, mergedStyle, height, width, styleBuffer );
 
 		handleStyle( container, styleBuffer );
 
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#endContainer(org.eclipse.birt.report.engine.content.IContainerContent)
 	 */
 	public void endContainer( IContainerContent container )
 	{
 		if ( pop( ) )
 		{
 			return;
 		}
 
 		writer.closeTag( (String) tagStack.pop( ) );
 
 		logger.log( Level.FINE, "[HTMLContainerEmitter] End container" ); //$NON-NLS-1$
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#startText(org.eclipse.birt.report.engine.content.ITextContent)
 	 */
 	public void startText( ITextContent text )
 	{
 		IStyle mergedStyle = text.getStyle( );
 		if ( peek( mergedStyle ) )
 		{
 			return;
 		}
 
 		logger.log( Level.FINE, "[HTMLReportEmitter] Start text" ); //$NON-NLS-1$
 
 		//Resize if the text generated by TemplateDesign
 		resizeTemplateElement( text);
 
 		StringBuffer styleBuffer = new StringBuffer( );
 		DimensionType x = text.getX( );
 		DimensionType y = text.getY( );
 		DimensionType width = text.getWidth( );
 		DimensionType height = text.getHeight( );
 		String textValue = text.getText( );
 		if (textValue == null || textValue == "" ) //$NON-NLS-1$
 		{
 			textValue = " "; //$NON-NLS-1$
 		}
 		
 		int display;
 		// If the item is multi-line, we should check if it can be inline-block
 		if ( textValue != null && textValue.indexOf( '\n' ) >= 0 )
 		{
 			display = checkElementType( x, y, width, height, mergedStyle,
 					styleBuffer );
 		}
 		else
 		{
 			display = checkElementType( x, y, mergedStyle, styleBuffer );
 		}
 
 		// action
 		String tagName;
 		if ( handleAction( text.getHyperlinkAction( ) ) )
 		{
 			tagName = HTMLTags.TAG_A;
 			setDisplayProperty( display, DISPLAY_BLOCK | DISPLAY_INLINE_BLOCK,
 					styleBuffer );
 			AttributeBuilder.checkHyperlinkTextDecoration( mergedStyle,
 					styleBuffer );
 		}
 		else
 		{
 			tagName = openTagByType( display, DISPLAY_FLAG_ALL );
 			setDisplayProperty( display, DISPLAY_INLINE_BLOCK, styleBuffer );
 		}
 
 		setStyleName( text.getStyleClass( ) );
 
 		// bookmark
 		setBookmark( tagName, text.getBookmark( ) );
 		
 		// If text is get from a label or template, add it to active id list, and output type & iid to html
 		setActiveIDTypeIID( text );
 		
 		// title
 		writer.attribute( HTMLTags.ATTR_TITLE, text.getHelpText( ) ); //$NON-NLS-1$
 
 		// check 'can-shrink' property
 		handleShrink( display, mergedStyle, height, width, styleBuffer );
 		handleStyle( text, styleBuffer, false );
 
 		
 		String verticalAlign = mergedStyle.getVerticalAlign( );
 		if ( !"baseline".equals( verticalAlign ) && height != null )
 		{
 			// implement vertical align.
 			writer.openTag( HTMLTags.TAG_TABLE );
 			writer.attribute( HTMLTags.ATTR_STYLE, " width:100%; height:100%;" );
 			writer.openTag( HTMLTags.TAG_TR );
 			writer.openTag( HTMLTags.TAG_TD );
 
 			StringBuffer textStyleBuffer = new StringBuffer( );
 			textStyleBuffer.append( " vertical-align:" );
 			textStyleBuffer.append( verticalAlign );
 			textStyleBuffer.append( ";" );
 			String textAlign = mergedStyle.getTextAlign( );
 			if ( textAlign != null )
 			{
 				textStyleBuffer.append( " text-align:" );
 				textStyleBuffer.append( textAlign );
 				textStyleBuffer.append( ";" );
 			}
 			writer.attribute( HTMLTags.ATTR_STYLE, textStyleBuffer );
 
 			writer.text( textValue );
 
 			writer.closeTag( HTMLTags.TAG_TD );
 			writer.closeTag( HTMLTags.TAG_TR );
 			writer.closeTag( HTMLTags.TAG_TABLE );
 		}
 		else
 		{
 			writer.text( textValue );
 		}
 		
 		writer.closeTag( tagName );
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#startForeign(org.eclipse.birt.report.engine.content.IForeignContent)
 	 */
 	public void startForeign( IForeignContent foreign )
 	{
 		IStyle mergedStyle = foreign.getStyle( );
 
 		if ( peek( mergedStyle ) )
 		{
 			return;
 		}
 
 		logger.log( Level.FINE, "[HTMLReportEmitter] Start foreign" ); //$NON-NLS-1$
 
 		StringBuffer styleBuffer = new StringBuffer( );
 		DimensionType x = foreign.getX( );
 		DimensionType y = foreign.getY( );
 		DimensionType width = foreign.getWidth( );
 		DimensionType height = foreign.getHeight( );
 
 		int display;
 		display = checkElementType( x, y, width, height, mergedStyle,
 				styleBuffer );
 
 		// action
 		String tagName;
 		if ( handleAction( foreign.getHyperlinkAction( ) ) )
 		{
 			tagName = HTMLTags.TAG_A;
 			setDisplayProperty( display, DISPLAY_BLOCK | DISPLAY_INLINE_BLOCK,
 					styleBuffer );
 			AttributeBuilder.checkHyperlinkTextDecoration( mergedStyle,
 					styleBuffer );
 		}
 		else
 		{
 			tagName = openTagByType( display, DISPLAY_FLAG_ALL );
 			setDisplayProperty( display, DISPLAY_INLINE_BLOCK, styleBuffer );
 		}
 
 		setStyleName( foreign.getStyleClass( ) );
 
 		// bookmark
 		setBookmark( tagName, foreign.getBookmark( ) );
 
 		// title
 		writer.attribute( HTMLTags.ATTR_TITLE, foreign.getHelpText( ) );
 
 		// check 'can-shrink' property
 		handleShrink( display, mergedStyle, height, width, styleBuffer );
 		handleStyle( foreign, styleBuffer, false );
 
 		Object rawValue = foreign.getRawValue( );
 		String rawType = foreign.getRawType( );
 		boolean isHtml = IForeignContent.HTML_TYPE.equalsIgnoreCase( rawType );
 		String text = rawValue == null ? null : rawValue.toString( );
 
 		if ( isHtml )
 		{
 			Document doc = new TextParser( ).parse( text,
 					TextParser.TEXT_TYPE_HTML );
 			ReportDesignHandle design = (ReportDesignHandle) runnable
 					.getDesignHandle( );
 			HTMLProcessor htmlProcessor = new HTMLProcessor( design );
 
 			HashMap styleMap = new HashMap( );
 
 			Element body = null;
 			if ( doc != null )
 			{
 				NodeList bodys = doc.getElementsByTagName( "body" );
 				if ( bodys.getLength( ) > 0 )
 				{
 					body = (Element) bodys.item( 0 );
 				}
 			}
 			if ( body != null )
 			{
 				htmlProcessor.execute( body, styleMap );
 				processNodes( body, styleMap );
 			}
 		}
 
 		// writer.text( text, !isHtml, !isHtml );
 
 		writer.closeTag( tagName );
 
 	}
 
 	/**
 	 * Visits the children nodes of the specific node
 	 * 
 	 * @param visitor
 	 *            the ITextNodeVisitor instance
 	 * @param ele
 	 *            the specific node
 	 */
 	private void processNodes( Element ele, HashMap cssStyles )
 	{
 		for ( Node node = ele.getFirstChild( ); node != null; node = node
 				.getNextSibling( ) )
 		{
 
 			// At present we only deal with the text and element nodes
 			if ( node.getNodeType( ) == Node.TEXT_NODE
 					|| node.getNodeType( ) == Node.ELEMENT_NODE )
 			{
 				if ( !node.getNodeName( ).equals( "#text" ) )
 				{
 					startNode( node, cssStyles );
 				}
 				if ( node.getNodeType( ) == Node.TEXT_NODE )
 				{
 				//	bug132213 in text item should only deal with the escape special characters: < > &
 				//  old code:	writer.text( node.getNodeValue( ), false, true );
 					writer.textForHtmlItem( node.getNodeValue( ) );
 				}
 				else
 				{
 					processNodes( (Element) node, cssStyles );
 				}
 				if ( !node.getNodeName( ).equals( "#text" ) )
 				{
 					endNode( node );
 				}
 			}
 		}
 
 	}
 
 	public void startNode( Node node, HashMap cssStyles )
 	{
 		String nodeName = node.getNodeName( );
 		HashMap cssStyle = (HashMap) cssStyles.get( node );
 		writer.openTag( nodeName );
 		NamedNodeMap attributes = node.getAttributes( );
 		if ( attributes != null )
 		{
 			for ( int i = 0; i < attributes.getLength( ); i++ )
 			{
 				Node attribute = attributes.item( i );
 				String attrName = attribute.getNodeName( );
 				String attrValue = attribute.getNodeValue( );
 
 				if ( attrValue != null )
 				{
 					if ( "img".equalsIgnoreCase( nodeName )
 							&& "src".equalsIgnoreCase( attrName ) )
 					{
 						String attrValueTrue = handleStyleImage( attrValue );
 						if ( attrValueTrue != null )
 						{
 							attrValue = attrValueTrue;
 						}
 					}
 					writer.attribute( attrName, attrValue );
 				}
 			}
 		}
 		if ( cssStyle != null )
 		{
 			StringBuffer buffer = new StringBuffer( );
 			Iterator ite = cssStyle.entrySet( ).iterator( );
 			while ( ite.hasNext( ) )
 			{
 				Map.Entry entry = (Map.Entry) ite.next( );
 				Object keyObj = entry.getKey( );
 				Object valueObj = entry.getValue( );
 				if ( keyObj == null || valueObj == null )
 				{
 					continue;
 				}
 				String key = keyObj.toString( );
 				String value = valueObj.toString( );
 				buffer.append( key );
 				buffer.append( ":" );
 				if ( "background-image".equalsIgnoreCase( key ) )
 				{
 					String valueTrue = handleStyleImage( value );
 					if ( valueTrue != null )
 					{
 						value = valueTrue;
 					}
 					buffer.append( "url(" );
 					buffer.append( value );
 					buffer.append( ")" );
 				}
 				else
 				{
 					buffer.append( value.replaceAll( " ", "" ) );
 				}
 				buffer.append( ";" );
 			}
 			if ( buffer.length( ) != 0 )
 			{
 				writer.attribute( "style", buffer.toString( ) );
 			}
 		}
 	}
 
 	public void endNode( Node node )
 	{
 		writer.closeTag( node.getNodeName( ) );
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#startLabel(org.eclipse.birt.report.engine.content.ILabelContent)
 	 */
 	public void startLabel( ILabelContent label )
 	{
 		String bookmark = label.getBookmark( );
 		if ( bookmark == null ) 
 		{
 			bookmark = generateUniqueID( );
 			label.setBookmark( bookmark );
 		}
 		startText( label );
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#startData(org.eclipse.birt.report.engine.content.IDataContent)
 	 */
 	public void startData( IDataContent data )
 	{
 		startText( data );
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#startImage(org.eclipse.birt.report.engine.content.IImageContent)
 	 */
 	public void startImage( IImageContent image )
 	{
 		assert image != null;
 		IStyle mergedStyle = image.getStyle( );
 
 		if ( peek( mergedStyle ) )
 		{
 			return;
 		}
 
 		logger.log( Level.FINE, "[HTMLImageEmitter] Start image" ); //$NON-NLS-1$ 
 
 		Object generateBy = image.getGenerateBy( );
 		
 		// include select handle chart
 		boolean isSelectHandleTableChart = false;
 		if ( renderOption != null && renderOption instanceof HTMLRenderOption )
 		{
 			HTMLRenderOption htmlOption = (HTMLRenderOption) renderOption;
 			
 			if ( htmlOption.getIncludeSelectionHandle( )
 					&& generateBy instanceof ExtendedItemDesign )
 			{
 				startSelectHandleTableChart( image );
 				isSelectHandleTableChart = true;
 			}
 		}		
 
 		StringBuffer styleBuffer = new StringBuffer( );
 		int display = checkElementType( image.getX( ), image.getY( ),
 				mergedStyle, styleBuffer );
 		String tag = openTagByType( display, DISPLAY_BLOCK );
 
 		// action
 		boolean hasAction = handleAction( image.getHyperlinkAction( ) );
 
 		String imgUri = getImageURI( image );
 		boolean useSVG = ( image.getMIMEType( ) != null )
 				&& image.getMIMEType( ).equalsIgnoreCase( "image/svg+xml" ); //$NON-NLS-1$
 		if ( useSVG )
 		{ // use svg
 			writer.openTag( HTMLTags.TAG_EMBED );
 
 			// bookmark
 			String bookmark = image.getBookmark( );				
 			
 			if ( !isSelectHandleTableChart )
 			{
 				if ( bookmark == null )
 				{
 					bookmark = generateUniqueID( );
 					image.setBookmark( bookmark );
 				}
 				setBookmark( HTMLTags.ATTR_IMAGE, bookmark ); //$NON-NLS-1$
 				// If the image is a chart, add it to active id list, and output type ��iid to html
 				setActiveIDTypeIID(image);
 				
 			}
 			else
 			{
 				bookmark = generateUniqueID( );
 				setBookmark( HTMLTags.ATTR_IMAGE, bookmark ); //$NON-NLS-1$
 			}
 			
 			//	onresize gives the SVG a change to change its content
 			writer.attribute( "onresize", bookmark+".reload()"); //$NON-NLS-1$
 			
 			writer.attribute( HTMLTags.ATTR_TYPE, "image/svg+xml" ); //$NON-NLS-1$
 			writer.attribute( HTMLTags.ATTR_SRC, imgUri );
 			setStyleName( image.getStyleClass( ) );
 			StringBuffer buffer = new StringBuffer( );
 			// build size
 			AttributeBuilder.buildSize( buffer, HTMLTags.ATTR_WIDTH, image
 					.getWidth( ) ); //$NON-NLS-1$
 			AttributeBuilder.buildSize( buffer, HTMLTags.ATTR_HEIGHT, image
 					.getHeight( ) ); //$NON-NLS-1$
 			// handle inline style
 			handleStyle( image, buffer, false );
 			writer.closeNoEndTag( );
 		}
 		else
 		{ // use img
 
 			// write image map if necessary
 			Object imageMapObject = image.getImageMap( );
 			// use imgUri as the image ID. As we know only the CHART can have
 			// image maps and each chart
 			// will have differnt URI, so it is safe for CHART. (If the named
 			// image also support image
 			// map, then we must use another way to get the image ID.
 			String imageMapId = imgUri;
 			boolean hasImageMap = ( imageMapObject != null )
 					&& ( imageMapObject instanceof String )
 					&& ( ( (String) imageMapObject ).length( ) > 0 );
 			if ( hasImageMap )
 			{
 				writer.openTag( HTMLTags.TAG_MAP );
 				writer.attribute( HTMLTags.ATTR_NAME, imageMapId );
 				writer.text( (String) imageMapObject, true, false );
 				writer.closeTag( HTMLTags.TAG_MAP );
 			}
 
 			writer.openTag( HTMLTags.TAG_IMAGE ); //$NON-NLS-1$
 			setStyleName( image.getStyleClass( ) );
 			setDisplayProperty( display, 0, styleBuffer );
 			
 			// bookmark
 			String bookmark = image.getBookmark( );				
 			if ( !isSelectHandleTableChart )
 			{
 				if ( bookmark == null )
 				{
 					bookmark = generateUniqueID( );
 					image.setBookmark( bookmark );
 				}
 				setBookmark( HTMLTags.ATTR_IMAGE, bookmark ); //$NON-NLS-1$
 				
 				// if the image is a chart, add it to active id list, and output type ��iid to html
 				setActiveIDTypeIID( image );
 			}
 			else
 			{
 				bookmark = generateUniqueID( );
 				setBookmark( HTMLTags.ATTR_IMAGE, bookmark ); //$NON-NLS-1$
 			}
 			
 			String ext = image.getExtension( );
 			// FIXME special process, such as encoding etc
 			writer.attribute( HTMLTags.ATTR_SRC, imgUri );
 
 			if ( hasImageMap )
 			{
 				// BUGZILLA 119245 request chart (without hyperlink) can't have
 				// borders, the BROWSER add blue-solid border to the image with
 				// maps.
 				if ( !hasAction )
 				{
 					// disable the border, if the user defines border with the
 					// image, it will be overided by the following style setting
 					IStyle style = image.getStyle( );
 					if ( style.getBorderTopStyle( ) == null )
 					{
 						// user doesn't define the border, remove it.
 						styleBuffer.append( "border-top-style:none;" );
 					}
 					else
 					{
 						// use define the border-style, but not define the
 						// border color, use the default
 						// color.
 						if ( style.getBorderTopColor( ) == null )
 						{
 							styleBuffer.append( "border-top-color:black" );
 						}
 					}
 					if ( style.getBorderBottomStyle( ) == null )
 					{
 						styleBuffer.append( "border-bottom-style:none;" );
 					}
 					else
 					{
 						if ( style.getBorderBottomColor( ) == null )
 						{
 							styleBuffer.append( "border-bottom-color:black" );
 						}
 					}
 					if ( style.getBorderLeftStyle( ) == null )
 					{
 						styleBuffer.append( "border-left-style:none;" );
 					}
 					else
 					{
 						if ( style.getBorderLeftColor( ) == null )
 						{
 							styleBuffer.append( "border-left-color:black" );
 						}
 					}
 					if ( style.getBorderRightStyle( ) == null )
 					{
 						styleBuffer.append( "border-right-style:none;" );
 					}
 					else
 					{
 						if ( style.getBorderRightColor( ) == null )
 						{
 							styleBuffer.append( "border-right-color:black" );
 						}
 					}
 				}
 				writer.attribute( HTMLTags.ATTR_USEMAP, "#" + imageMapId ); //$NON-NLS-1$ //$NON-NLS-2$
 			}
 
 			// alternative text
 			writer.attribute( HTMLTags.ATTR_ALT, image.getAltText( ) );
 
 			// help text
 			writer.attribute( HTMLTags.ATTR_TITLE, image.getHelpText( ) );
 
 			// image size
 			AttributeBuilder.buildSize( styleBuffer, HTMLTags.ATTR_WIDTH, image
 					.getWidth( ) ); //$NON-NLS-1$
 			AttributeBuilder.buildSize( styleBuffer, HTMLTags.ATTR_HEIGHT,
 					image.getHeight( ) ); //$NON-NLS-1$
 			// handle style
 			handleStyle( image, styleBuffer, false );
 
 			if ( ".PNG".equalsIgnoreCase( ext ) && imageHandler != null ) //$NON-NLS-1$
 			{
 				writer.attribute( HTMLTags.ATTR_ONLOAD, "fixPNG(this)" ); //$NON-NLS-1$
 			}
 
 			writer.closeNoEndTag( );
 		}
 
 		if ( hasAction )
 		{
 			writer.closeTag( HTMLTags.TAG_A );
 		}
 
 		writer.closeTag( tag );
 		
 		// include	select handle chart
 		if ( renderOption != null && renderOption instanceof HTMLRenderOption )
 		{
 			HTMLRenderOption htmlOption = (HTMLRenderOption) renderOption;
 			
 			if ( htmlOption.getIncludeSelectionHandle( )
 					&& generateBy instanceof ExtendedItemDesign )
 			{
 				endSelectHandleTableChart( );
 			}
 		}		
 
 	}
 	
 	/**
 	 * bug128480
 	 * render chard with a highlight handle
 	 */
 	protected void startSelectHandleTableChart( IContent content )
 	{
 		writer.openTag( HTMLTags.TAG_TABLE );
 		
 		// set the id and iid to the out table
 		// bookmark
 		String bookmark = content.getBookmark( );
 		if ( bookmark == null )
 		{
 			bookmark = generateUniqueID( );
 			content.setBookmark( bookmark );
 		}
 		setBookmark( HTMLTags.ATTR_IMAGE, bookmark ); //$NON-NLS-1$
 		// If the image is a chart, add it to active id list, and output type ��iid to html
 		setActiveIDTypeIID( content );
 		
 		writer.attribute( HTMLTags.ATTR_STYLE, "width:100%" );
 		writer.openTag( HTMLTags.TAG_COL );
		writer.attribute( HTMLTags.ATTR_STYLE, "width:18px;background-color:#cccccc" );
 		writer.closeTag( HTMLTags.TAG_COL );
 		writer.openTag( HTMLTags.TAG_COL );
 		writer.attribute( HTMLTags.ATTR_STYLE, "width:95%" );
 		writer.closeTag( HTMLTags.TAG_COL );
 		writer.openTag( HTMLTags.TAG_TR );
 		writer.openTag( HTMLTags.TAG_TD );
 		writer.closeTag( HTMLTags.TAG_TD );
 		writer.openTag( HTMLTags.TAG_TD );
 	}
 	
 	protected void endSelectHandleTableChart( )
 	{
 		writer.closeTag( HTMLTags.TAG_TD );
 		writer.closeTag( HTMLTags.TAG_TR );
 		writer.closeTag( HTMLTags.TAG_TABLE );		
 	}
 
 	/**
 	 * gets the image's URI
 	 * 
 	 * @param image
 	 *            the image content
 	 * @return image's URI
 	 */
 	protected String getImageURI( IImageContent image )
 	{
 		String imgUri = null;
 		if ( imageHandler != null )
 		{
 
 			Image img = new Image( image );
 			img.setRenderOption( renderOption );
 			img.setReportRunnable( runnable );
 			switch ( img.getSource( ) )
 			{
 				case IImage.DESIGN_IMAGE :
 					imgUri = imageHandler.onDesignImage( img, renderContext );
 					break;
 				case IImage.URL_IMAGE :
 					imgUri = imageHandler.onURLImage( img, renderContext );
 					break;
 				case IImage.REPORTDOC_IMAGE :
 					imgUri = imageHandler.onDocImage( img, renderContext );
 					break;
 				case IImage.CUSTOM_IMAGE :
 					imgUri = imageHandler.onCustomImage( img, renderContext );
 					break;
 				case IImage.FILE_IMAGE :
 					imgUri = imageHandler.onFileImage( img, renderContext );
 					break;
 				case IImage.INVALID_IMAGE :
 					break;
 			}
 		}
 
 		return imgUri;
 	}
 
 	/**
 	 * Sets the <code>'class'</code> property and stores the style to styleMap
 	 * object.
 	 * 
 	 * @param styleName
 	 *            the style name
 	 */
 	protected void setStyleName( String styleName )
 	{
 		if ( isEmbeddable )
 		{
 			return;
 		}
 
 		if ( styleName != null )
 		{
 			writer.attribute( HTMLTags.ATTR_CLASS, styleName );
 		}
 	}
 
 	/**
 	 * Checks the 'CanShrink' property and sets the width and height according
 	 * to the table below:
 	 * <p>
 	 * <table border=0 cellspacing=3 cellpadding=0 summary="Chart showing
 	 * symbol, location, localized, and meaning.">
 	 * <tr bgcolor="#ccccff">
 	 * <th align=left>CanShrink</th>
 	 * <th align=left>Element Type</th>
 	 * <th align=left>Width</th>
 	 * <th align=left>Height</th>
 	 * </tr>
 	 * <tr valign=middle>
 	 * <td rowspan="2"><code>true(by default)</code></td>
 	 * <td>in-line</td>
 	 * <td>ignor</td>
 	 * <td>set</td>
 	 * </tr>
 	 * <tr valign=top bgcolor="#eeeeff">
 	 * <td><code>block</code></td>
 	 * <td>set</td>
 	 * <td>ignor</td>
 	 * </tr>
 	 * <tr valign=middle>
 	 * <td rowspan="2" bgcolor="#eeeeff"><code>false</code></td>
 	 * <td>in-line</td>
 	 * <td>replaced by 'min-width' property</td>
 	 * <td>set</td>
 	 * </tr>
 	 * <tr valign=top bgcolor="#eeeeff">
 	 * <td><code>block</code></td>
 	 * <td>set</td>
 	 * <td>replaced by 'min-height' property</td>
 	 * </tr>
 	 * </table>
 	 * 
 	 * @param type
 	 *            The display type of the element.
 	 * @param style
 	 *            The style of an element.
 	 * @param height
 	 *            The height property.
 	 * @param width
 	 *            The width property.
 	 * @param styleBuffer
 	 *            The <code>StringBuffer</code> object that returns 'style'
 	 *            content.
 	 * @return A <code>boolean</code> value indicating 'Can-Shrink' property
 	 *         is set to <code>true</code> or not.
 	 */
 	protected boolean handleShrink( int type, IStyle style,
 			DimensionType height, DimensionType width, StringBuffer styleBuffer )
 	{
 		boolean canShrink = style == null
 				|| !"false".equalsIgnoreCase( style.getCanShrink( ) ); //$NON-NLS-1$
 
 		if ( ( type & DISPLAY_BLOCK ) > 0 )
 		{
 			AttributeBuilder
 					.buildSize( styleBuffer, HTMLTags.ATTR_WIDTH, width );
 			if ( !canShrink )
 			{
 				AttributeBuilder.buildSize( styleBuffer,
 						HTMLTags.ATTR_MIN_HEIGHT, height );
 			}
 		}
 		else if ( ( type & DISPLAY_INLINE ) > 0 )
 		{
 			AttributeBuilder.buildSize( styleBuffer, HTMLTags.ATTR_HEIGHT,
 					height );
 			if ( !canShrink )
 			{
 				AttributeBuilder.buildSize( styleBuffer,
 						HTMLTags.ATTR_MIN_WIDTH, width );
 			}
 
 		}
 		else
 		{
 			assert false;
 		}
 		return canShrink;
 	}
 
 	/**
 	 * Outputs the 'bookmark' property. Destination anchors in HTML documents
 	 * may be specified either by the A element (naming it with the 'name'
 	 * attribute), or by any other elements (naming with the 'id' attribute).
 	 * 
 	 * @param tagName
 	 *            The tag's name.
 	 * @param bookmark
 	 *            The bookmark value.
 	 */
 	protected void setBookmark( String tagName, String bookmark )
 	{
 		if ( tagName == null || !HTMLTags.TAG_A.equalsIgnoreCase( tagName ) )
 		{
 			writer.attribute( HTMLTags.ATTR_ID, bookmark );
 		}
 		else
 		{
 			writer.attribute( HTMLTags.ATTR_ID, bookmark );
 			writer.attribute( HTMLTags.ATTR_NAME, bookmark );
 		}
 	}
 
 	/**
 	 * Checks whether the element is block, inline or inline-block level. In
 	 * BIRT, the absolute positioning model is used and a box is explicitly
 	 * offset with respect to its containing block. When an element's x or y is
 	 * set, it will be treated as a block level element regardless of the
 	 * 'Display' property set in style. When designating width or height value
 	 * to an inline element, it will be treated as inline-block.
 	 * 
 	 * @param x
 	 *            Specifies how far a box's left margin edge is offset to the
 	 *            right of the left edge of the box's containing block.
 	 * @param y
 	 *            Specifies how far an absolutely positioned box's top margin
 	 *            edge is offset below the top edge of the box's containing
 	 *            block.
 	 * @param width
 	 *            The width of the element.
 	 * @param height
 	 *            The height of the element.
 	 * @param style
 	 *            The <code>IStyle</code> object.
 	 * @param styleBuffer
 	 *            The <code>StringBuffer</code> object that returns 'style'
 	 *            content.
 	 * @return The display type of the element.
 	 */
 	protected int checkElementType( DimensionType x, DimensionType y,
 			IStyle style, StringBuffer styleBuffer )
 	{
 		return checkElementType( x, y, null, null, style, styleBuffer );
 	}
 
 	protected int checkElementType( DimensionType x, DimensionType y,
 			DimensionType width, DimensionType height, IStyle style,
 			StringBuffer styleBuffer )
 	{
 		int type = 0;
 		String display = null;
 		if ( style != null )
 		{
 			display = style.getDisplay( );
 		}
 
 		if ( EngineIRConstants.DISPLAY_NONE.equalsIgnoreCase( display ) )
 		{
 			type |= DISPLAY_NONE;
 		}
 
 		if ( x != null || y != null )
 		{
 			styleBuffer.append( "position: absolute;" ); //$NON-NLS-1$
 			AttributeBuilder.buildSize( styleBuffer, HTMLTags.ATTR_LEFT, x );
 			AttributeBuilder.buildSize( styleBuffer, HTMLTags.ATTR_LEFT, y );
 			return type | DISPLAY_BLOCK;
 		}
 		else if ( EngineIRConstants.DISPLAY_INLINE.equalsIgnoreCase( display ) )
 		{
 			type |= DISPLAY_INLINE;
 			if ( width != null || height != null )
 			{
 				type |= DISPLAY_INLINE_BLOCK;
 			}
 			return type;
 		}
 
 		return type | DISPLAY_BLOCK;
 	}
 
 	/**
 	 * Open a tag according to the display type of the element. Here is the
 	 * mapping table:
 	 * <p>
 	 * <table border=0 cellspacing=3 cellpadding=0 summary="Chart showing
 	 * symbol, location, localized, and meaning.">
 	 * <tr bgcolor="#ccccff">
 	 * <th align=left>Display Type</th>
 	 * <th align=left>Tag name</th>
 	 * </tr>
 	 * <tr valign=middle>
 	 * <td>DISPLAY_BLOCK</td>
 	 * <td>DIV</td>
 	 * </tr>
 	 * <tr valign=top bgcolor="#eeeeff">
 	 * <td>DISPLAY_INLINE</td>
 	 * <td>SPAN</td>
 	 * </tr>
 	 * </table>
 	 * 
 	 * @param display
 	 *            The display type.
 	 * @param mask
 	 *            The mask value.
 	 * @return Tag name.
 	 */
 	protected String openTagByType( int display, int mask )
 	{
 		int flag = display & mask;
 		if ( ( flag & DISPLAY_BLOCK ) > 0 )
 		{
 			writer.openTag( HTMLTags.TAG_DIV );
 			return HTMLTags.TAG_DIV;
 		}
 
 		if ( ( flag & DISPLAY_INLINE ) > 0 )
 		{
 			writer.openTag( HTMLTags.TAG_SPAN );
 			return HTMLTags.TAG_SPAN;
 		}
 
 		return null;
 	}
 
 	/**
 	 * Set the display property to style.
 	 * 
 	 * @param display
 	 *            The display type.
 	 * @param mask
 	 *            The mask.
 	 * @param styleBuffer
 	 *            The <code>StringBuffer</code> object that returns 'style'
 	 *            content.
 	 */
 	protected void setDisplayProperty( int display, int mask,
 			StringBuffer styleBuffer )
 	{
 		int flag = display & mask;
 		if ( ( display & DISPLAY_NONE ) > 0 )
 		{
 			styleBuffer.append( "display: none;" ); //$NON-NLS-1$
 		}
 		else if ( flag > 0 )
 		{
 			if ( ( flag & DISPLAY_BLOCK ) > 0 )
 			{
 				styleBuffer.append( "display: block;" ); //$NON-NLS-1$
 			}
 			else if ( ( flag & DISPLAY_INLINE_BLOCK ) > 0 )
 			{
 				styleBuffer.append( "display: inline-block;" ); //$NON-NLS-1$
 			}
 			else if ( ( flag & DISPLAY_INLINE ) > 0 )
 			{
 				styleBuffer.append( "display: inline;" ); //$NON-NLS-1$
 			}
 		}
 	}
 
 	/**
 	 * Checks the Action object and then output corresponding tag and property.
 	 * 
 	 * @param action
 	 *            The <code>IHyperlinkAction</code> object.
 	 * @return A <code>boolean</code> value indicating whether the Action
 	 *         object is valid or not.
 	 */
 	protected boolean handleAction( IHyperlinkAction action )
 	{
 		if ( action == null )
 		{
 			return false;
 		}
 		Action act = new Action( action );
 
 		if ( actionHandler == null )
 		{
 			return false;
 		}
 
 		String link = actionHandler.getURL( act, renderContext );
 		boolean ret = ( link != null && !link.equals( "" ) ); //$NON-NLS-1$
 
 		if ( ret )
 		{
 
 			writer.openTag( HTMLTags.TAG_A );
 
 			writer.attribute( HTMLTags.ATTR_HREF, link );
 
 			writer.attribute( HTMLTags.ATTR_TARGET, action.getTargetWindow( ) );
 		}
 
 		return ret;
 	}
 
 	/**
 	 * handle style image
 	 * 
 	 * @param uri
 	 *            uri in style image
 	 * @return
 	 */
 	public String handleStyleImage( String uri )
 	{
 		ReportDesignHandle design = (ReportDesignHandle) runnable
 				.getDesignHandle( );
 		URL url = design.findResource( uri, IResourceLocator.IMAGE );
 		if ( url == null )
 		{
 			return uri;
 		}
 		uri = url.toExternalForm( );
 		Image image = new Image( uri );
 		image.setReportRunnable( runnable );
 		image.setRenderOption( renderOption );
 		String imgUri = null;
 		if ( imageHandler != null )
 		{
 			switch ( image.getSource( ) )
 			{
 
 				case IImage.URL_IMAGE :
 					imgUri = imageHandler.onURLImage( image, renderContext );
 					break;
 
 				case IImage.FILE_IMAGE :
 					imgUri = imageHandler.onFileImage( image, renderContext );
 					break;
 
 				case IImage.INVALID_IMAGE :
 					break;
 
 				default :
 					assert ( false );
 			}
 			// imgUri = imgUri.replace( File.separatorChar, '/' );
 		}
 		return imgUri;
 	}
 
 	/**
 	 * Handles the style of the styled element content
 	 * 
 	 * @param element
 	 *            the styled element content
 	 * @param styleBuffer
 	 *            the StringBuffer instance
 	 */
 	protected void handleStyle( IContent element, StringBuffer styleBuffer,
 			boolean bContainer )
 	{
 		IStyle style;
 		if ( isEmbeddable )
 		{
 			style = element.getStyle( );
 		}
 		else
 		{
 			style = element.getInlineStyle( );
 		}
 		AttributeBuilder.buildStyle( styleBuffer, style, this, bContainer );
 		if ( !bContainer )
 		{
 			AttributeBuilder.buildComputedTextStyle( styleBuffer, element.getComputedStyle( ),this , bContainer );
 		}
 
 		// output in-line style
 		writer.attribute( HTMLTags.ATTR_STYLE, styleBuffer.toString( ) );
 	}
 
 	protected void handleStyle( IContent element, StringBuffer styleBuffer )
 	{
 		IStyle style;
 		if ( isEmbeddable )
 		{
 			style = element.getStyle( );
 		}
 		else
 		{
 			style = element.getInlineStyle( );
 		}
 		AttributeBuilder.buildStyle( styleBuffer, style, this, true );
 
 		// output in-line style
 		writer.attribute( HTMLTags.ATTR_STYLE, styleBuffer.toString( ) );
 	}
 
 	/**
 	 * adds the default table styles
 	 * 
 	 * @param styleBuffer
 	 */
 	protected void addDefaultTableStyles( StringBuffer styleBuffer )
 	{
 			styleBuffer
 					.append( "border-collapse: collapse; empty-cells: show;" ); //$NON-NLS-1$
 	}
 
 	protected void exportElementID( IContent content, String bookmark,
 			String type, long componentID )
 	{
 		Object generateBy = content.getGenerateBy( );
 		if ( generateBy instanceof TableItemDesign
 				|| generateBy instanceof ListItemDesign
 				|| generateBy instanceof ExtendedItemDesign
 				|| generateBy instanceof LabelItemDesign
 				|| generateBy instanceof TemplateDesign )
 		{
 			if ( renderOption instanceof HTMLRenderOption )
 			{
 				List htmlIds = ( (HTMLRenderOption) renderOption )
 						.getInstanceIDs( );
 				if ( htmlIds != null && bookmark != null )
 				{
 					assert type != null;
 					String newBookmark = bookmark + "," + type 
 							+ "," + new Long( componentID ).toString();
 					htmlIds.add( newBookmark );
 				}
 			}
 		}
 	}
 
 	protected int bookmarkId = 0;
 	protected String generateUniqueID( )
 	{
 		bookmarkId ++;
 		return "AUTOGENBOOKMARK_" + bookmarkId;
 	}
 	
 	/**
 	 * Add element to active ID list. Output type & iid to html.
 	 * 
 	 * @param content
 	 *            the styled element content
 	 */
 	private void setActiveIDTypeIID( IContent content)
 	{
 		// If content is generated by LabelItemDesign or TemplateDesign,
 		// ExtendedItemDesign, TableItemDesign
 		// add it to active id list, and output type & iid to html
 		Object genBy = content.getGenerateBy( );
 		String type = null;
 		if (genBy instanceof LabelItemDesign)
 		{
 			type = "LABEL";
 		}
 		else if (genBy instanceof TemplateDesign)
 		{
 			type = "TEMPLATE";
 		}
 		
 		else if( genBy instanceof ExtendedItemDesign )
 		{
 			type = "EXTENDED" ;
 		}
 		else if ( genBy instanceof TableItemDesign )
 		{
 			type = "TABLE";
 		}
 		else if (genBy instanceof ListItemDesign)
 		{
 			type = "LIST";
 		}
 		if (type != null)
 		{
 			// Instance ID
 			InstanceID iid = content.getInstanceID( );
 			// active id
 			long componentID = ( iid != null ) ? iid.getComponentID() : 0;
 			exportElementID( content, content.getBookmark( ), type, componentID );
 			// type
 			writer.attribute( "element_type", type );
 			if ( iid != null )
 			{
 				writer.attribute( "iid", iid.toString( ) );
 			}
 		}
 	}
 	
 	/**
 	 * Resize chart template and table template element.
 	 * 
 	 * @param content
 	 *            the styled element content
 	 */
 	private void resizeTemplateElement( IContent content )
 	{
 
 			Object genBy = content.getGenerateBy( );
 			if( genBy instanceof TemplateDesign )
 			{
 				TemplateDesign template = (TemplateDesign) genBy;
 				
 				String allowedType = template.getAllowedType( );
 				if ( "ExtendedItem".equals(allowedType ) )
 				{
 					// Resize chart template element
 					IStyle style = content.getStyle( );
 					style.setProperty( IStyle.STYLE_CAN_SHRINK, IStyle.FALSE_VALUE );
 					content.setWidth( new DimensionType( 3, DimensionType.UNITS_IN ) );
 					content.setHeight( new DimensionType( 3, DimensionType.UNITS_IN ) );
 				}
 				else if ( "Table".equals(allowedType) )
 				{
 					// Resize table template element
 					IStyle style = content.getStyle( );
 					style.setProperty( IStyle.STYLE_CAN_SHRINK, IStyle.FALSE_VALUE );
 					content.setWidth( new DimensionType( 3, DimensionType.UNITS_IN ) );
 					content.setHeight( new DimensionType( 5, DimensionType.UNITS_IN ) );
 				}
 		}
 	}
 }
