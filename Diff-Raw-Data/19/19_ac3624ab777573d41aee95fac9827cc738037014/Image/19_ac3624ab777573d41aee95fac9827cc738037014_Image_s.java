 /*
 *Copyright 2001 idega.is All Rights Reserved.
 */
 package com.idega.presentation;
 import java.rmi.RemoteException;
 import java.sql.SQLException;
 import java.util.HashMap;
 import java.util.Locale;
 import java.util.Map;
 
 import com.idega.core.builder.business.BuilderService;
 import com.idega.core.builder.data.ICDomain;
 import com.idega.core.file.business.FileSystemConstants;
 import com.idega.core.localisation.business.ICLocaleBusiness;
 import com.idega.idegaweb.IWConstants;
 import com.idega.presentation.text.Link;
 import com.idega.presentation.text.Text;
 import com.idega.presentation.ui.Window;
 import com.idega.util.text.StyleConstants;
 import com.idega.util.text.TextSoap;
 /**
 *@author <a href="mailto:tryggvi@idega.is">Tryggvi Larusson</a>
 *@version 1.5
 *@modified <a href="mailto:eiki@idega.is">Eirikur Hrafnson</a>
 */
 public class Image extends PresentationObject
 {
 	public static final String ALIGNMENT_BOTTOM = "bottom";
 	public static final String ALIGNMENT_MIDDLE = "middle";
 	public static final String ALIGNMENT_LEFT = "left";
 	public static final String ALIGNMENT_RIGHT = "right";
 	public static final String ALIGNMENT_TOP = "top";
 	public static final String ALIGNMENT_ABSOLUTE_MIDDLE = "absmiddle";
 	public static final String ALIGNMENT_ABSOLUTE_BOTTOM = "absbottom";
 	public static final String ALIGNMENT_BASELINE = "baseline";
 	public static final String ALIGNMENT_TEXT_TOP = "texttop";
 	private Script theAssociatedScript;
 	private String overImageUrl;
 	private String downImageUrl;
 	private Map _ImageLocalizationMap;
 	private String textBgColor = "#CCCCCC";
 	private boolean limitImageWidth = false;
 	private boolean zoomView = false;
 	private boolean linkOnImage = true;
 	private boolean useCaching = true;
 	private String align;
 	private Link zoomLink;
 	private String zoomImageID;
 	private String zoomPageID;
 	private String zoomImageWidth;
 	private String zoomImageHeight;
 	private int imageId = -1;
 	//private ImageEntity image;
 	//**@todo: remove this when no longer needed
 	//private com.idega.jmodule.image.data.ImageEntity image2;
 	private int maxImageWidth = 140;
 	//private boolean usesOldImageTables = false;
 	private static String PARAM_IMAGE_ID = FileSystemConstants.PARAM_FILE_ID;
 	
 	private static final String BORDER_COLOR_DEFAULT = "#000000";
 	private static final String BORDER_STYLE_DEFAULT = StyleConstants.BORDER_SOLID;
 	
 	public Image()
 	{
 		this("");
		setBorder(0, BORDER_COLOR_DEFAULT, BORDER_STYLE_DEFAULT);
 	}
 	public Image(String url)
 	{
 		this(url, "");
		setBorder(0, BORDER_COLOR_DEFAULT, BORDER_STYLE_DEFAULT);
 	}
 	public Image(String url, String name)
 	{
 		super();
 		if ("".equalsIgnoreCase(name))
 			name = this.generateID();
 		setName(name);
 		setAlt(name);
 		setURL(url);
		setBorder(1, BORDER_COLOR_DEFAULT, BORDER_STYLE_DEFAULT);
 	}
 	public Image(String name, String url, String overImageUrl)
 	{
 		super();
 		setName(name);
 		setAlt(name);
 		setURL(url);
		setBorder(0, BORDER_COLOR_DEFAULT, BORDER_STYLE_DEFAULT);
 		this.overImageUrl = overImageUrl;
 		setOnMouseOut("swapImgRestore()");
 		setOnMouseOver("swapImage('" + getName() + "','','" + overImageUrl + "',1)");
 	}
 	public Image(String name, String url, String overImageUrl, String downImageUrl)
 	{
 		this(name, url, overImageUrl);
 		this.downImageUrl = downImageUrl;
 		setOnMouseDown("swapImage('" + getName() + "','','" + downImageUrl + "',1)");
 		//setAttribute("onMouseUp","swapImage('"+getName()+"','','"+overImageUrl+"',1)");
 	}
 	public Image(String url, String name, int width, int height)
 	{
 		super();
 		setName(name);
 		setAlt(name);
 		setURL(url);
 		setWidth(width);
 		setHeight(height);
		setBorder(0, BORDER_COLOR_DEFAULT, BORDER_STYLE_DEFAULT);
 	}
 	/**
 	*Fetches an image from the database through the imageservlet or blobcache
 	*/
 	public Image(int imageId) throws SQLException
 	{
 		super();
 		this.imageId = imageId;
		setBorder(0, BORDER_COLOR_DEFAULT, BORDER_STYLE_DEFAULT);
 		setName(this.generateID());
 	}
 	public Image(int imageId, String name) throws SQLException
 	{
 		this(imageId);
 		setName(name);
 		setAlt(name);
 	}
 	public Image(int imageId, int width, int height) throws SQLException
 	{
 		this(imageId);
 		setWidth(width);
 		setHeight(height);
 	}
 	public Image(int imageId, String name, int width, int height) throws SQLException
 	{
 		this(imageId, name);
 		setWidth(width);
 		setHeight(height);
 	}
 	private void setImageURL(IWContext iwc) throws Exception
 	{
 		//IWMainApplication iwma = iwc.getApplication();
 		//**@todo: remove this when no longer needed
 		/*String mmProp = iwma.getSettings().getProperty(MediaServlet.USES_OLD_TABLES);
 		if (mmProp != null)
 		{
 			usesOldImageTables = true;
 		}*/
 		//if (useCaching)
 		//{
 			//Cache cachedImage;
 			/*if (usesOldImageTables)
 			{ //eiki: could this clash with other cached files and overwrite them?
 				cachedImage =
 					(Cache) IWCacheManager.getInstance(iwma).getCachedBlobObject("com.idega.jmodule.image.data.ImageEntity", imageId, iwma);
 			}
 			else
 			{*/
 				//cachedImage =
 				//	(Cache) IWCacheManager.getInstance(iwma).getCachedBlobObject("com.idega.block.image.data.ImageEntity", imageId, iwma);
 			//}
 			/*if (cachedImage != null)
 			{
 				//**@todo: remove this when no longer needed
 				if (usesOldImageTables)
 				{
 					image2 = (com.idega.jmodule.image.data.ImageEntity) cachedImage.getEntity();
 				}
 				else
 				{
 					image = (ImageEntity) cachedImage.getEntity();
 				}
 				setURL(cachedImage.getVirtualPathToFile());
 			}*/
 
 				String url;
 				url = getICFileSystem(iwc).getFileURI(imageId);
 				setURL(url);
 			
 		//}
 		//**@todo: remove this when no longer needed
 		//if(image==null){//if something went wrong or we are not using caching
 		// if( (image==null) ){//&& (image2==null) ){//if something went wrong or we are not using caching
 		
 		/*if ((image == null) && (image2 == null))
 		{ //if something went wrong or we are not using caching
 			if (usesOldImageTables)
 			{
 				image2 = new com.idega.jmodule.image.data.ImageEntity(imageId);
 			}
 			else
 			{
 				image =
 					(
 						(com.idega.block.image.data.ImageEntityHome) com.idega.data.IDOLookup.getHomeLegacy(
 							ImageEntity.class)).findByPrimaryKeyLegacy(
 						imageId);
 			}
 			StringBuffer URIBuffer;
 			URIBuffer = new StringBuffer(iwma.getMediaServletURI());
 			URIBuffer.append(imageId);
 			URIBuffer.append("image?");
 			URIBuffer.append(idName);
 			URIBuffer.append("=");
 			URIBuffer.append(imageId);
 			setURL(URIBuffer.toString());
 		}*/
 	}
 	public void setLocalizedImage(String localeString, int imageID)
 	{
 		setLocalizedImage(ICLocaleBusiness.getLocaleFromLocaleString(localeString), imageID);
 	}
 	public void setLocalizedImage(Locale locale, int imageID)
 	{
 		getImageLocalizationMap().put(locale, new Integer(imageID));
 	}
 	private Map getImageLocalizationMap()
 	{
 		if (_ImageLocalizationMap == null)
 		{
 			_ImageLocalizationMap = new HashMap();
 		}
 		return _ImageLocalizationMap;
 	}
 	public void setProperty(String key, String values[])
 	{
 		if (key.equalsIgnoreCase("url"))
 			setURL(values[0]);
 		else if (key.equalsIgnoreCase("width"))
 		{
 			setWidth(values[0]);
 		}
 		else if (key.equalsIgnoreCase("height"))
 		{
 			setHeight(values[0]);
 		}
 		else if (key.equalsIgnoreCase("border"))
 		{
 			setHeight(Integer.parseInt(values[0]));
 		}
 		else if (key.equalsIgnoreCase("image_id"))
 		{
 			this.setImageID(Integer.parseInt(values[0]));
 		}
 	}
 	public void setBorder(int size, String color, String style) {
 		setBorder(Integer.toString(size), color, style);
 	}
 	public void setBorder(String size, String color, String style) {
 		setBorder(size);
 		setBorderColor(color);
 		setBorderStyle(style);
 	}
 	public void setBorder(String size)
 	{
 		setStyleAttribute("border-width", size+"px");
 	}
 	public void setBorderColor(String color)
 	{
 		setStyleAttribute("border-color", color);
 	}
 	public void setBorderStyle(String style) {
 		setStyleAttribute("border-style", style);
 	}
 	public void setBorder(int i)
 	{
 		setBorder(Integer.toString(i));
 	}
 	public void setURL(String url)
 	{
 		setSrc(url);
 	}
 	public void setSrc(String src)
 	{
 		setMarkupAttribute("src", src);
 	}
 	public void setWidth(int width)
 	{
 		setWidth(Integer.toString(width));
 	}
 	public void setWidth(String width)
 	{
 		setMarkupAttribute("width", width);
 	}
 	public void setHeight(int height)
 	{
 		setHeight(Integer.toString(height));
 	}
 	public void setHeight(String height)
 	{
 		setMarkupAttribute("height", height);
 	}
 	public void setImageID(int imageID)
 	{
 		this.imageId = imageID;
 	}
 	public int getDefaultImageID()
 	{
 		return this.imageId;
 	}
 	public int getImageID(IWContext iwc)
 	{
 		try
 		{
 			Integer localizedID = getTheCorrectDefaultImageID(iwc);
 			if (localizedID == null)
 			{
 				return this.imageId;
 			}
 			else
 			{
 				return localizedID.intValue();
 			}
 		}
 		catch (Exception e)
 		{
 			return this.imageId;
 		}
 	}
 	/**
 	 * Returns the correct Image, localized or not depending on what has been set.
 	 */
 	private Integer getTheCorrectDefaultImageID(IWContext iwc) throws Exception
 	{
 		if (this._ImageLocalizationMap != null)
 		{
 			Locale currLocale = iwc.getCurrentLocale();
 			Integer localizedImageID = (Integer) this.getImageLocalizationMap().get(currLocale);
 			if (localizedImageID != null)
 			{
 				return localizedImageID;
 			}
 			else
 			{
 				Integer defImageID = (Integer) this.getImageLocalizationMap().get(iwc.getApplication().getSettings().getDefaultLocale());
 				if (defImageID != null)
 				{
 					return defImageID;
 				}
 			}
 		}
 		return null;
 	}
 	public void setVerticalSpacing(int spacing)
 	{
 		setMarkupAttribute("vspace", Integer.toString(spacing));
 	}
 	public void setHorizontalSpacing(int spacing)
 	{
 		setMarkupAttribute("hspace", Integer.toString(spacing));
 	}
 	public void setTextBackgroundColor(String color)
 	{
 		this.textBgColor = color;
 	}
 	public String getHeight()
 	{
 		return getMarkupAttribute("height");
 	}
 	public String getWidth()
 	{
 		return getMarkupAttribute("width");
 	}
 	public String getURL()
 	{
 		return this.getMarkupAttribute("src");
 	}
 	/**
 	 * Returns true if the image has been set to a source, else false
 	 */
 	public boolean hasSource()
 	{
 		return ((getURL() != null) || this.imageId != -1);
 	}
 	public void setOnClick(String action)
 	{
 		setMarkupAttribute("onClick", action);
 	}
 	public void setOnMouseOver(String action)
 	{
 		setMarkupAttribute("onMouseOver", action);
 	}
 	public void setOnMouseOut(String action)
 	{
 		setMarkupAttribute("onMouseOut", action);
 	}
 	public void setOnMouseDown(String action)
 	{
 		setMarkupAttribute("onMouseDown", action);
 	}
 	public void setOnClickImageURL(String clickImageURL)
 	{
 		overImageUrl = clickImageURL;
 		setOnClick("swapImgRestore(); swapImage('" + getName() + "','','" + clickImageURL + "',1)");
 	}
 	public void setOnClickImage(Image image)
 	{
 		setOnClickImageURL(image.getMediaURL());
 	}
 	public void setOverImageURL(String overImageURL)
 	{
 		this.overImageUrl = overImageURL;
 	}
 	public void setOverImage(Image image)
 	{
 		this.overImageUrl = image.getMediaURL();
 		setOnMouseOut("swapImgRestore()");
 		setOnMouseOver("swapImage('" + getName() + "','','" + overImageUrl + "',1)");
 	}
 	public void setImageToOpenInPopUp(Image image)
 	{
 		this.setOnClick(
 			"img_wnd=window.open('','','width=100,height=100,left='+((screen.width/2)-50)+',top='+((screen.height/2)-50)+',resizable=yes,scrollbars=no'); doopen('"
 				+ image.getMediaURL()
 				+ "'); return true;");
 	}
 	public String getOverImageURL()
 	{
 		return this.overImageUrl;
 	}
 	public void addMarkupAttributes(Map attributeMap)
 	{
 		if (attributeMap.containsKey(FileSystemConstants.ZOOMIMAGE) && attributeMap.containsKey(FileSystemConstants.ZOOMPAGE))
 		{
 			zoomImageID =
 				attributeMap.containsKey(FileSystemConstants.ZOOMIMAGE) ? (String) attributeMap.get(FileSystemConstants.ZOOMIMAGE) : null;
 			zoomPageID =
 				attributeMap.containsKey(FileSystemConstants.ZOOMPAGE) ? (String) attributeMap.get(FileSystemConstants.ZOOMPAGE) : null;
 			zoomImageWidth =
 				attributeMap.containsKey(FileSystemConstants.ZOOMWIDTH)
 					? (String) attributeMap.get(FileSystemConstants.ZOOMWIDTH)
 					: "400";
 			zoomImageHeight =
 				attributeMap.containsKey(FileSystemConstants.ZOOMHEIGHT)
 					? (String) attributeMap.get(FileSystemConstants.ZOOMHEIGHT)
 					: "400";
 			attributeMap.remove(FileSystemConstants.ZOOMIMAGE);
 			attributeMap.remove(FileSystemConstants.ZOOMPAGE);
 			attributeMap.remove(FileSystemConstants.ZOOMWIDTH);
 			attributeMap.remove(FileSystemConstants.ZOOMHEIGHT);
 		}
 		super.addMarkupAttributes(attributeMap);
 	}
 	public void setAssociatedScript(Script myScript)
 	{
 		theAssociatedScript = myScript;
 	}
 	public void setAlignment(String alignment)
 	{
 		align = alignment;
 	}
 	public Script getAssociatedScript()
 	{
 		return theAssociatedScript;
 	}
 	public void setImageLinkZoomView()
 	{
 		this.zoomView = true;
 	}
 	public void setImageZoomLink(Link link)
 	{
 		this.zoomLink = link;
 	}
 	public void setNoImageLink()
 	{
 		this.linkOnImage = false;
 	}
 	public void setAlt(String alt)
 	{
 		setMarkupAttribute("alt", alt);
 	}
 	public String getAlt()
 	{
 		return getMarkupAttribute("alt");
 	}
 	private String getHTMLString(IWContext iwc) throws RemoteException
 	{
 		StringBuffer sPrint = new StringBuffer();
 		sPrint.append("<img ");
 		//alt always added for standards compliancy
 		sPrint.append("alt=\"");
 		/** @todo Fix this shitty mix!!! */
 		if (getAlt() != null && (getAlt().length() > 2 && !getAlt().substring(0, 2).equals("id")))
 		{
 			sPrint.append(getAlt());
 		}
 		sPrint.append("\" ");
 		removeMarkupAttribute("alt");
 		/*sPrint.append("name=\"");
 		sPrint.append(getName());
 		sPrint.append("\"");*/
 		if (iwc != null)
 		{
 			BuilderService bs = getBuilderService(iwc);
 			ICDomain d = bs.getCurrentDomain();
 			if (d.getURL() != null)
 			{
 				String src = getMarkupAttribute("src");
 				if (src.startsWith("/"))
 				{
 					String protocol;
 					//@todo this is case sensitive and could break! move to IWContext. Also done in Link, SubmitButton, Image and PageIncluder
 					if (iwc.getRequest().isSecure())
 					{
 						protocol = "https://";
 					}
 					else
 					{
 						protocol = "http://";
 					}
 					setMarkupAttribute("src", protocol + d.getURL() + src);
 				}
 			}
 		}
 		sPrint.append(getMarkupAttributesString());
 		if (align != null)
 		{
 			sPrint.append(" align=\"" + align + "\" ");
 		}
 		String markup = iwc.getApplicationSettings().getProperty(Page.MARKUP_LANGUAGE, Page.HTML);
 		sPrint.append(" "+(!markup.equals(Page.HTML) ? "/" : "")+">");
 		return sPrint.toString();
 	}
 	public static String getNoImageSource()
 	{
 		return "/idegaweb/bundles/core.bundle/resources/noimage.gif";
 	}
 	private void getHTMLImage(IWContext iwc)
 	{ //optimize by writing in pure html
 		try
 		{
 			setImageURL(iwc);
 			//if( (image!=null) && (image.getID()!=-1) ){//begin debug
 			/**@todo : remove temporary backward compatability when no longer needed
 			 *
 			 */
 			// if( ((image!=null) && (image.getID()!=-1)) ){//(|| ((image2!=null) && (image2.getID()!=-1)) ){//begin debug
 			//if (((image != null) && (image.getPrimaryKey() != null)) || ((image2 != null) && (image2.getID() != -1)))
 			//{ //begin debug
 				String texti = null;
 				String link = null;
 				String width = null;
 				String height = null;
 			/*	if (usesOldImageTables)
 				{
 					texti = image2.getText();
 					link = image2.getLink();
 					width = image2.getWidth();
 					height = image2.getHeight();
 				}
 				else
 				{
 					//texti = image.getDescription();
 					//link = image.getLink();
 					// width = image.getWidth();
 					// height = image.getHeight();
 				}*/
 				// if( getName() != null && name != null ) setName(name);
 				if (!limitImageWidth)
 				{
 					if ((width != null) && (!width.equalsIgnoreCase("")) && (!width.equalsIgnoreCase("-1")))
 					{
 						setWidth(width);
 					}
 					if ((height != null) && (!height.equalsIgnoreCase("")) && (!height.equalsIgnoreCase("-1")))
 					{
 						setHeight(height);
 					}
 				}
 				else
 				{
 					if ((width != null) && (!width.equalsIgnoreCase("")) && (!width.equalsIgnoreCase("-1")))
 					{
 						if (Integer.parseInt(width) > maxImageWidth)
 						{
 							setWidth(maxImageWidth);
 						}
 					}
 					else
 					{
 						setWidth(maxImageWidth);
 					}
 				}
 				if ((texti != null) && (!"".equalsIgnoreCase(texti)))
 				{
 					Table imageTable = new Table(1, 2);
 					imageTable.setAlignment(1, 1, "center");
 					imageTable.setAlignment(1, 2, "left");
 					//imageTable.setCellpadding(0);
 					//imageTable.setCellspacing(0);
 					imageTable.setColor(1, 2, textBgColor);
 					String sWidth = getWidth();
 					if ((sWidth != null) && (!sWidth.equalsIgnoreCase("")) && (!limitImageWidth))
 					{
 						imageTable.setWidth(sWidth);
 					}
 					else if (limitImageWidth)
 					{
 						imageTable.setWidth(maxImageWidth);
 					}
 					Text imageText = new Text(texti);
 					imageText.setFontSize(1);
 					if ((link != null) && (!"".equalsIgnoreCase(link)))
 					{ //has a link
 						Link textLink = new Link(imageText, link);
 						textLink.setTarget("_new");
 						textLink.setFontSize(1);
 						imageTable.add(textLink, 1, 2); //add the text with the link on it
 						//should we add the image with a link? or just the image
 						if (zoomView)
 						{
 							if (zoomLink != null)
 							{
 								zoomLink.setText(getHTMLString(iwc));
 								imageTable.add(zoomLink, 1, 1);
 							}
 							else
 							{
 								Link imageLink = new Link(getHTMLString(iwc));
 								imageLink.addParameter("image_id", imageId);
 								imageTable.add(imageLink, 1, 1);
 							}
 						}
 						else if ((!zoomView) && (linkOnImage))
 						{
 							Link imageLink = new Link(getHTMLString(iwc), link);
 							imageLink.setTarget("_new");
 							imageTable.add(imageLink, 1, 1);
 						}
 						else
 							imageTable.add(getHTMLString(iwc), 1, 1);
 					}
 					else
 					{ //or no link
 						if (zoomView)
 						{
 							if (zoomLink != null)
 							{
 								zoomLink.setText(getHTMLString(iwc));
 								imageTable.add(zoomLink, 1, 1);
 							}
 							else
 							{
 								Link imageLink = new Link(getHTMLString(iwc));
 								imageLink.addParameter("image_id", imageId);
 								imageTable.add(imageLink, 1, 1);
 							}
 						}
 						else
 							imageTable.add(getHTMLString(iwc), 1, 1);
 						imageTable.add(imageText, 1, 2);
 					}
 					imageTable._print(iwc);
 				}
 				else
 				{
 					if (zoomView)
 					{
 						if (zoomLink != null)
 						{
 							zoomLink.setText(getHTMLString(iwc));
 							zoomLink._print(iwc);
 						}
 						else
 						{
 							Link imageLink = new Link(getHTMLString(iwc));
 							imageLink.addParameter("image_id", imageId);
 							imageLink._print(iwc);
 						}
 					}
 					else
 						print(getHTMLString(iwc));
 				}
 			//} //end debug
 		}
 		catch (Exception e)
 		{
 			e.printStackTrace(System.err);
 			System.out.println(e.getMessage());
 		}
 	}
 	public void setMaxImageWidth(int maxImageWidth)
 	{
 		this.limitImageWidth = true;
 		this.maxImageWidth = maxImageWidth;
 	}
 	public void limitImageWidth(boolean limitImageWidth)
 	{
 		this.limitImageWidth = limitImageWidth;
 	}
 	/*
 	* this uses an undocumented access method to the IWContext which is fetched from the current thread
 	* it could brake! Use getMediaURL(IWContext) if possible.
 	* @todo implement in the main method if possible
 	*/
 	public String getMediaURL()
 	{
 		return getMediaURL(IWContext.getInstance());
 	}
 	/**
 	 * Use this method for getting an images (stored in the database) url
 	 * @todo check if fails with usesOldMedia tables
 	 * @param iwc The IWContext
 	 * @return
 	 */
 	public String getMediaURL(IWContext iwc)
 	{
 		if (imageId != -1){
 			String theReturn = "";
 			//return MediaBusiness.getMediaURL(imageId, ImageEntity.class, iwc.getApplication());
 			try
 			{
 				theReturn = getICFileSystem(iwc).getFileURI(imageId);
 			}
 			catch (RemoteException e)
 			{
 				// TODO Auto-generated catch block
 				e.printStackTrace();
 			}
 			return theReturn;
 		}
 		else{
 			return getURL();
 		}
 	}
 	/**
 	 *  return a call to image preloadscript
 	 *
 	 */
 	public static String getPreloadScript(String url)
 	{
 		return "preLoadImages('" + url + "')";
 	}
 	public Object clone()
 	{
 		Image obj = null;
 		try
 		{
 			obj = (Image) super.clone();
 			if (theAssociatedScript != null)
 			{
 				obj.theAssociatedScript = (Script) this.theAssociatedScript.clone();
 			}
 			obj.overImageUrl = this.overImageUrl;
 			obj.textBgColor = this.textBgColor;
 			obj.limitImageWidth = this.limitImageWidth;
 			obj.zoomView = this.zoomView;
 			obj.linkOnImage = this.linkOnImage;
 			obj.imageId = this.imageId;
 			obj.maxImageWidth = this.maxImageWidth;
 		}
 		catch (Exception ex)
 		{
 			ex.printStackTrace(System.err);
 		}
 		return obj;
 	}
 	public void main(IWContext iwc)
 	{
 		if (iwc.isParameterSet(PARAM_IMAGE_ID))
 		{
 			this.imageId = Integer.parseInt(iwc.getParameter(PARAM_IMAGE_ID));
 		}
 		if (this.overImageUrl != null)
 		{
 			Page parent = getParentPage();
 			if (parent != null)
 			{
 				Script rollOverScript = parent.getAssociatedScript();
 				rollOverScript.addFunction(
 					"swapImgRestore()",
 					"function swapImgRestore() {var i,x,a=document.sr; for(i=0;a&&i<a.length&&(x=a[i])&&x.oSrc;i++) x.src=x.oSrc;}");
 				rollOverScript.addFunction(
 					"preLoadImages()",
 					"function preLoadImages(){var d=document; if(d.images){ if(!d.p) d.p=new Array(); var i,j=d.p.length,a=preLoadImages.arguments; for(i=0; i<a.length; i++)  if (a[i].indexOf(\"#\")!=0){ d.p[j]=new Image; d.p[j++].src=a[i];}}}");
 				rollOverScript.addFunction(
 					"findObj(n, d)",
 					"function findObj(n, d){var p,i,x;  if(!d) d=document; if((p=n.indexOf(\"?\"))>0&&parent.frames.length) {  d=parent.frames[n.substring(p+1)].document; n=n.substring(0,p);}  if(!(x=d[n])&&d.all) x=d.all[n]; for (i=0;!x&&i<d.forms.length;i++) x=d.forms[i][n];  for(i=0;!x&&d.layers&&i<d.layers.length;i++) x=findObj(n,d.layers[i].document); return x;}");
 				rollOverScript.addFunction(
 					"swapImage()",
 					"function swapImage(){ var i,j=0,x,a=swapImage.arguments; document.sr=new Array; for(i=0;i<(a.length-2);i+=3) if ((x=findObj(a[i]))!=null){document.sr[j++]=x; if(!x.oSrc) x.oSrc=x.src; x.src=a[i+2];}}");
 				parent.setAssociatedScript(rollOverScript);
 				parent.setOnLoad(getPreloadScript(overImageUrl));
 			}
 		}
 	}
 	public void print(IWContext iwc) throws Exception
 	{
 		if (zoomImageID != null)
 		{
 			Link link = new Link();
 			link.addParameter(PARAM_IMAGE_ID, zoomImageID);
 			link.setPage(Integer.parseInt(zoomPageID));
 			link.setURL("/index.jsp");
 			link.setWindowToOpenScript(
 				Window.getWindowCallingScript(
 					link.getURL(iwc),
 					"",
 					false,
 					false,
 					false,
 					false,
 					false,
 					false,
 					false,
 					true,
 					false,
 					Integer.parseInt(zoomImageWidth),
 					Integer.parseInt(zoomImageHeight)));
 			setImageZoomLink(link);
 			setImageLinkZoomView();
 		}
 		if (getLanguage().equals(IWConstants.MARKUP_LANGUAGE_HTML))
 		{
 			//added by eiki
 			//Change the imageId so that it is localized
 			imageId = this.getImageID(iwc);
 			if (imageId == -1)
 			{ //from an url
 				print(getHTMLString(iwc));
 			}
 			else
 			{ //from the database
 				getHTMLImage(iwc);
 			}
 		}
 		else if (getLanguage().equals(IWConstants.MARKUP_LANGUAGE_PDF_XML))
 		{
 			setURL(iwc.getServerURL() + getMediaURL(iwc));
 			String markup = getHTMLString(iwc);
 			markup = TextSoap.findAndReplace(markup, "img", "image");
 			markup = TextSoap.findAndReplace(markup, "src", "url");
 			markup = TextSoap.findAndReplace(markup, "width", "plainwidth");
 			markup = TextSoap.findAndReplace(markup, "height", "plainheight");
 			print(markup);
     }
     
   }
 
 	public void setPadding(int padding) {
 		setStyleAttribute("padding: "+padding+"px;");
 	}
 	
 	public void setPaddingLeft(int padding) {
 		setStyleAttribute("padding-left: "+padding+"px;");
 	}
 	
 	public void setPaddingRight(int padding) {
 		setStyleAttribute("padding-right: "+padding+"px;");
 	}
 	
 	public void setPaddingTop(int padding) {
 		setStyleAttribute("padding-top: "+padding+"px;");
 	}
 	
 	public void setPaddingBottom(int padding) {
 		setStyleAttribute("padding-bottom: "+padding+"px;");
 	}
 
 }
