 /***********************************************************************
  * Copyright (c) 2004 Actuate Corporation.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  * Actuate Corporation - initial API and implementation
  ***********************************************************************/
 
 package org.eclipse.birt.report.engine.layout.pdf;
 
 import java.io.IOException;
 import java.io.InputStream;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.util.ArrayList;
 import java.util.logging.Level;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 import org.eclipse.birt.report.engine.content.Dimension;
 import org.eclipse.birt.report.engine.content.IContainerContent;
 import org.eclipse.birt.report.engine.content.IContent;
 import org.eclipse.birt.report.engine.content.IHyperlinkAction;
 import org.eclipse.birt.report.engine.content.IImageContent;
 import org.eclipse.birt.report.engine.content.impl.ActionContent;
 import org.eclipse.birt.report.engine.content.impl.ReportContent;
 import org.eclipse.birt.report.engine.extension.IReportItemExecutor;
 import org.eclipse.birt.report.engine.ir.ExtendedItemDesign;
 import org.eclipse.birt.report.engine.layout.ILineStackingLayoutManager;
 import org.eclipse.birt.report.engine.layout.area.IImageArea;
 import org.eclipse.birt.report.engine.layout.area.impl.AreaFactory;
 import org.eclipse.birt.report.engine.layout.area.impl.BlockContainerArea;
 import org.eclipse.birt.report.engine.layout.area.impl.ContainerArea;
 import org.eclipse.birt.report.engine.layout.area.impl.ImageArea;
 import org.eclipse.birt.report.model.api.IResourceLocator;
 import org.eclipse.birt.report.model.api.ReportDesignHandle;
 
 import com.lowagie.text.BadElementException;
 import com.lowagie.text.Image;
 
 /**
  * 
  * This layout mananger implements formatting and locating of image content.
  * <p>
  * Image is an atomic conponent, so it can not be split. if the size exceeds the
  * boundry, user agent should overflow or clip it.
  * <p>
  * if layout manager can not retrieve the instrinsic dimension of image, layout
  * mangager set the instrinsic dimension to the default value (1,1). logger will
  * log this error, but this can not interrupt the layout process.
  * <p>
  * this layout manager genrate image area which perhaps has border, render
  * should take reponsibility to draw the image and its border
  * <p>
  * the dimension algorithm shows as following table:
  * <p>
  * <table>
  * <tr>
  * <td>scale</td>
  * <td>height</td>
  * <td>width</td>
  * <td>notes</td>
  * </tr>
  * <tr>
  * <td rowspan="3">true</td>
  * <td>X</td>
  * <td>X</td>
  * <td rowspan="3">Following the CSS defined algorithm.</td>
  * </tr>
  * <tr>
  * <td>X</td>
  * <td></td>
  * </tr>
  * <tr>
  * <td></td>
  * <td>X</td>
  * </tr>
  * <tr>
  * <td rowspan="4">false</td>
  * <td>X</td>
  * <td>X</td>
  * <td>Use the defined width and height</td>
  * </tr>
  * <tr>
  * <td>X</td>
  * <td></td>
  * <td>Use the defined height and intrinsic width</td>
  * </tr>
  * <tr>
  * <td></td>
  * <td>X</td>
  * <td>Use the intrinsic height, defined width</td>
  * </tr>
  * <tr>
  * <td></td>
  * <td></td>
  * <td>Use the intrinsic size</td>
  * </tr>
  * 
  * </table>
  * 
  */
 public class PDFImageLM extends PDFLeafItemLM
 {
 
 	protected final static int DEFAULT_WIDHT = 212000;
 
 	protected final static int DEFAULT_HEIGHT = 130000;
 
 	protected IImageContent image;
 
 	protected int maxWidth;
 
 	protected ContainerArea root;
 
	private Dimension instrinsic;
 
 	private static final String BOOKMARK_PREFIX = "javascript:catchBookmark('";
 
 	public PDFImageLM( PDFLayoutEngineContext context, PDFStackingLM parent,
 			IContent content, IReportItemExecutor executor )
 	{
 		super( context, parent, content, executor );
 		init( );
 
 	}
 
 	/**
 	 * get intrinsic dimension of image in pixels. Now only support png, bmp,
 	 * jpg, gif.
 	 * 
 	 * @param in
 	 * @return
 	 * @throws IOException
 	 * @throws MalformedURLException
 	 * @throws BadElementException
 	 */
 	protected Dimension getIntrinsicDimension( IImageContent content )
 			throws BadElementException, MalformedURLException, IOException
 	{
 		Image image = null;
 		switch ( content.getImageSource( ) )
 		{
 			case IImageContent.IMAGE_FILE :
 				ReportDesignHandle design = content.getReportContent( )
 						.getDesign( ).getReportDesign( );
 				URL url = design.findResource( content.getURI( ),
 						IResourceLocator.IMAGE );
 				InputStream in = url.openStream( );
 				try
 				{
 					byte[] buffer = new byte[in.available( )];
 					in.read( buffer );
 					image = Image.getInstance( buffer );
 				}
 				catch ( Exception ex )
 				{
 					logger.log( Level.WARNING, ex.getMessage( ), ex );
 				}
 				finally
 				{
 					in.close( );
 				}
 				break;
 			case IImageContent.IMAGE_NAME :
 			case IImageContent.IMAGE_EXPRESSION :
 				image = Image.getInstance( content.getData( ) );
 				break;
 
 			case IImageContent.IMAGE_URL :
 				image = Image.getInstance( new URL( content.getURI( ) ) );
 				break;
 			default :
 				assert ( false );
 		}
 		if ( image != null )
 		{
 			Object design = content.getGenerateBy( );
 			int resolution = 96;
 			if ( design instanceof ExtendedItemDesign )
 			{
 				resolution = 192;
 			}
 			return new Dimension( (int) ( image.plainWidth( ) * 1000
 					/ resolution * 72 ), (int) ( image.plainHeight( ) * 1000
 					/ resolution * 72 ) );
 		}
 		return null;
 	}
 
 	protected Dimension getSpecifiedDimension( IImageContent content )
 	{
 		Dimension dim = new Dimension( DEFAULT_WIDHT, DEFAULT_HEIGHT );
 		try
 		{
			instrinsic = getIntrinsicDimension( content );
 		}
 		catch ( Exception e )
 		{
 			logger.log( Level.SEVERE, e.getLocalizedMessage( ) );
 		}
 		boolean scale = false;
 		int specifiedWidth = getDimensionValue( content.getWidth( ) );
 		int specifiedHeight = getDimensionValue( content.getHeight( ) );
		if ( instrinsic == null )
 		{
 			dim.setDimension( specifiedWidth == 0
 					? DEFAULT_WIDHT
 					: specifiedWidth, specifiedHeight == 0
 					? DEFAULT_HEIGHT
 					: specifiedHeight );
 			return dim;
 		}
 		if ( scale )
 		{
			double ratio = instrinsic.getRatio( );
 
 			if ( specifiedWidth > 0 )
 			{
 				if ( specifiedHeight > 0 )
 				{
 					dim.setDimension( specifiedWidth, specifiedHeight );
 				}
 				else
 				{
 					dim.setDimension( specifiedWidth,
 							(int) ( specifiedWidth / ratio ) );
 				}
 			}
 			else
 			{
 				if ( specifiedHeight > 0 )
 				{
 					dim.setDimension( (int) ( specifiedHeight * ratio ),
 							specifiedHeight );
 				}
 				else
 				{
					dim.setDimension( instrinsic.getWidth( ), instrinsic
 							.getHeight( ) );
 				}
 			}
 		}
 		else
 		{
 			if ( specifiedWidth > 0 )
 			{
 				if ( specifiedHeight > 0 )
 				{
 					dim.setDimension( specifiedWidth, specifiedHeight );
 				}
 				else
 				{
					dim.setDimension( specifiedWidth, instrinsic.getHeight( ) );
 				}
 			}
 			else
 			{
 				if ( specifiedHeight > 0 )
 				{
					dim.setDimension( instrinsic.getWidth( ), specifiedHeight );
 				}
 				else
 				{
					dim.setDimension( instrinsic.getWidth( ), instrinsic
 							.getHeight( ) );
 				}
 			}
 		}
 		return dim;
 	}
 
 	public boolean layoutChildren( )
 	{
 		if ( root == null )
 		{
 			return false;
 		}
 		assert ( parent instanceof ILineStackingLayoutManager );
 		ILineStackingLayoutManager lineParent = (ILineStackingLayoutManager) parent;
 		// if height exceed current available value, must page break;
 		if ( root.getAllocatedHeight( ) > lineParent
 				.getCurrentMaxContentHeight( ) )
 		{
 			if ( !parent.isPageEmpty( ) )
 			{
 				return true;
 			}
 			else
 			{
 				// change the root height to make sure image can be put into
 				// parent.
 				root.setAllocatedHeight( lineParent
 						.getCurrentMaxContentHeight( ) );
 				parent.addArea( root, false, false );
 				return false;
 			}
 		}
 		else
 		{
 			if ( parent.getCurrentIP( ) + root.getAllocatedWidth( ) > maxWidth )
 			{
 				if ( !lineParent.isEmptyLine( ) )
 				{
 					boolean ret = lineParent.endLine( );
 					assert ( ret );
 					return layoutChildren( );
 				}
 				else
 				{
 					parent.addArea( root, false, false );
 					return false;
 				}
 			}
 			else
 			{
 				parent.addArea( root, false, false );
 				return false;
 			}
 		}
 	}
 
 	protected void init( )
 	{
 		assert ( content instanceof IImageContent );
 		image = (IImageContent) content;
 		maxWidth = parent.getCurrentMaxContentWidth( );
 
 		Dimension contentDimension = getSpecifiedDimension( image );
 		root = (ContainerArea) createInlineContainer( image, true, true );
 		validateBoxProperty( root.getStyle( ), maxWidth, context.getMaxHeight( ) );
 
 		// set max content width
 		root.setAllocatedWidth( maxWidth );
 		int maxContentWidth = root.getContentWidth( );
 		if ( contentDimension.getWidth( ) > maxContentWidth )
 		{
 			contentDimension.setDimension( maxContentWidth,
 					(int) ( maxContentWidth / contentDimension.getRatio( ) ) );
 		}
 
 		ImageArea imageArea = (ImageArea) AreaFactory.createImageArea( image );
 		imageArea.setWidth( contentDimension.getWidth( ) );
 		imageArea.setHeight( contentDimension.getHeight( ) );
 		root.addChild( imageArea );
 		imageArea.setPosition( root.getContentX( ), root.getContentY( ) );
 
 		processChartLegend( image, imageArea );
 		root.setContentWidth( contentDimension.getWidth( ) );
 		root.setContentHeight( Math.min( context.getMaxHeight( ),
 				contentDimension.getHeight( ) ) );
 	}
 
 	/**
 	 * Creates legend for chart.
 	 * 
 	 * @param imageContent
 	 *            the image content of the chart.
 	 * @param imageArea
 	 *            the imageArea of the chart.
 	 */
 	private void processChartLegend( IImageContent imageContent,
 			IImageArea imageArea )
 	{
 		Object imageMapObject = imageContent.getImageMap( );
 		boolean hasImageMap = ( imageMapObject != null )
 				&& ( imageMapObject instanceof String )
 				&& ( ( (String) imageMapObject ).length( ) > 0 );
 		if ( hasImageMap )
 		{
 			createImageMap( (String) imageMapObject, imageArea );
 		}
 	}
 
 	private void createImageMap( String imageMapObject, IImageArea imageArea )
 	{
 		Pattern pattern = Pattern
 				.compile( "<AREA[^<>]*coords=\"([\\d,]*)\" href=\"([^<>\"]*)\" target=\"([^<>\"]*)\"/>" );
 		Matcher matcher = pattern.matcher( imageMapObject );
 		String url = null;
 		String targetWindow = null;
 		ArrayList areas = new ArrayList( );
 		while ( matcher.find( ) )
 		{
 			try
 			{
 				areas.add( getArea( matcher.group( 1 ) ) );
 				if ( url == null )
 					url = matcher.group( 2 );
 				if ( targetWindow == null )
 					targetWindow = matcher.group( 3 );
 			}
 			catch ( NumberFormatException e )
 			{
 				logger.log( Level.WARNING, e.getMessage( ), e );
 			}
 		}
 		if ( url == null )
 		{
 			return;
 		}
 		ActionContent link = new ActionContent( );
 		if ( isBookmark( url ) )
 		{
 			String bookmark = getBookmark( url );
 			link.setBookmark( bookmark );
 		}
 		else
 		{
 			link.setHyperlink( url, targetWindow );
 		}
 		createImageMaps( areas, imageArea, link );
 	}
 
 	private void createImageMaps( ArrayList areas, IImageArea imageArea,
 			IHyperlinkAction link )
 	{
 		for ( int i = 0; i < areas.size( ); i++ )
 		{
 			int[] area = (int[]) areas.get( i );
 			area = getAbsoluteArea( area, imageArea );
 			createImageMapContainer( area[0], area[1], area[2], area[3], link );
 		}
 	}
 
 	/**
 	 * Creates an image map container, which is an empty container with an hyper
 	 * link.
 	 * 
 	 * @param x
 	 *            x cordinat of lower left corner of the container.
 	 * @param y
 	 *            x cordinat of lower left corner of the container.
 	 * @param width
 	 *            width of the container.
 	 * @param height
 	 *            height of the container.
 	 * @param link
 	 *            destination of the hyperlink.
 	 */
 	private void createImageMapContainer( int x, int y, int width, int height,
 			IHyperlinkAction link )
 	{
 		ReportContent reportContent = (ReportContent) image.getReportContent( );
 		IContainerContent mapContent = reportContent.createContainerContent( );
 		mapContent.setHyperlinkAction( link );
 		BlockContainerArea area = (BlockContainerArea) AreaFactory
 				.createBlockContainer( mapContent );
 		area.setPosition( x, y );
 		area.setWidth( width );
 		area.setHeight( height );
 		root.addChild( area );
 	}
 
 	/**
 	 * Caculates the absolute positions of image map when given the position of
 	 * image. The image map position is relative to the left up corner of the
 	 * image.
 	 * 
 	 * The argument and returned value are both 4 length integer area, the four
 	 * value of which are x, y of up left corner, width and height respectively.
 	 * 
 	 * @param area
 	 *            rectangle area of a image map.
 	 * @param imageArea
 	 *            image area of the image in which the image map is.
 	 * @return absolute postion of the image map.
 	 */
 	private int[] getAbsoluteArea( int[] area, IImageArea imageArea )
 	{
 		for ( int i = 0; i < 4; i++ )
 		{
 			area[i] = getTranslatedLength( area[i] );
 		}
 		int[] result = new int[4];
 		int imageX = imageArea.getX( );
 		int imageY = imageArea.getY( );
 		int imageHeight = imageArea.getHeight( );
 		int imageWidth = imageArea.getWidth( );
		int intrinsicWidth = instrinsic.getWidth( );
		int intrinsicHeight = instrinsic.getHeight( );
 		float ratio = (float) imageWidth / (float) intrinsicWidth;
 		result[0] = imageX + (int) ( area[0] * ratio );
 		result[2] = (int) ( area[2] * ratio );
 		ratio = (float) imageHeight / (float) intrinsicHeight;
 		result[1] = imageY + (int) ( area[1] * ratio );
 		result[3] = (int) ( area[3] * ratio );
 		return result;
 	}
 
 	private int getTranslatedLength( int length )
 	{
 		return length * 1000 / 192 * 72;
 	}
 
 	/**
 	 * Check if a url is of an internal bookmark.
 	 * 
 	 * @param url
 	 *            the url string.
 	 * @return true if and only if the url is of an internal bookmark.
 	 */
 	private boolean isBookmark( String url )
 	{
 		return url.startsWith( BOOKMARK_PREFIX ) && url.endsWith( "')" );
 	}
 
 	/**
 	 * Parses out bookmark name from a url for interanl bookmark.
 	 * 
 	 * @param url
 	 *            the url string
 	 * @return the bookmark name.
 	 */
 	private String getBookmark( String url )
 	{
 		int start = url.indexOf( BOOKMARK_PREFIX ) + BOOKMARK_PREFIX.length( );
 		int end = url.length( ) - 2;
 		return url.substring( start, end );
 	}
 
 	/**
 	 * Parse the image map postion from a string which is of format "x1, y1, x2,
 	 * y2".
 	 * 
 	 * @param string
 	 *            the postion string.
 	 * @return a array which contains the x, y cordinate of left up corner,
 	 *         width and hegiht in sequence.
 	 * 
 	 */
 	private int[] getArea( String string )
 	{
 		String[] rawDatas = string.split( "," );
 		int[] area = new int[4];
 		area[0] = Integer.parseInt( rawDatas[0] );
 		area[1] = Integer.parseInt( rawDatas[1] );
 		area[2] = Integer.parseInt( rawDatas[4] ) - area[0];
 		area[3] = Integer.parseInt( rawDatas[5] ) - area[1];
 		return area;
 	}
 
 }
