 /*
  * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
  * 			Copyright (c) 2008 Kai Krueger apmonkey at users dot sourceforge dot net 
  * See COPYING
  */
 
 package de.ueller.midlet.gps;
 
 import java.util.Timer;
 import java.util.TimerTask;
 import java.util.Vector;
 import java.lang.Integer;
 //#if polish.api.bigsearch
 import java.util.Hashtable;
 //import java.util.Enumeration;
 //#endif
 
 import javax.microedition.lcdui.Canvas;
 import javax.microedition.lcdui.ChoiceGroup;
 import javax.microedition.lcdui.Command;
 import javax.microedition.lcdui.CommandListener;
 import javax.microedition.lcdui.Display;
 import javax.microedition.lcdui.Displayable;
 import javax.microedition.lcdui.Form;
 import javax.microedition.lcdui.Graphics;
 import javax.microedition.lcdui.Image;
 import javax.microedition.lcdui.List;
 import javax.microedition.lcdui.TextField;
 
 import de.ueller.gps.data.Configuration;
 import de.ueller.gps.data.Legend;
 import de.ueller.gps.data.SearchResult;
 import de.ueller.gps.tools.HelperRoutines;
 import de.ueller.gps.tools.LayoutElement;
 import de.ueller.gpsMid.CancelMonitorInterface;
 import de.ueller.midlet.gps.GuiPOItypeSelectMenu.POItypeSelectMenuItem;
 import de.ueller.midlet.gps.data.KeySelectMenuItem;
 import de.ueller.midlet.gps.data.MoreMath;
 import de.ueller.midlet.gps.data.PositionMark;
 import de.ueller.midlet.gps.data.ProjMath;
 import de.ueller.midlet.gps.data.RoutePositionMark;
 import de.ueller.midlet.gps.names.NumberCanon;
 import de.ueller.midlet.gps.tile.SearchNames;
 import de.ueller.midlet.screens.InputListener;
 
 import de.enough.polish.util.Locale;
 
 public class GuiSearch extends Canvas implements CommandListener,
 		      GpsMidDisplayable, InputListener, KeySelectMenuReducedListener, CancelMonitorInterface {
 
 	protected static final int VIRTUALKEY_PRESSED = 1;
 
 	private final static Logger logger = Logger.getInstance(GuiSearch.class,Logger.DEBUG);
 
 	/** OK_CMD for Nearest POI / Fulltext Search */
 	private final Command OK_CMD = new Command(Locale.get("generic.OK")/*Ok*/, Command.OK, 1);
 	/** OK1_CMD is used when GUI is not optimised for routing */
 	private final Command OK1_CMD = new Command(Locale.get("generic.OK")/*Ok*/, Command.OK, 1);
 	/** ROUTE2_CMD is used when GUI is not optimised for routing */
 	private final Command ROUTE2_CMD = new Command(Locale.get("guisearch.Route1")/*Route*/, Command.ITEM, 3);
 	/** ROUTE1_CMD is used when GUI is optimised for routing */
 	private final Command ROUTE1_CMD = new Command(Locale.get("guisearch.Route1")/*Route*/, Command.OK, 1);
 	/** OK2_CMD is used when GUI is optimised for routing */
 	private final Command OK2_CMD = new Command(Locale.get("guisearch.Asdest")/*As destination*/, Command.ITEM, 3);
 	private final Command DISP_CMD = new Command(Locale.get("guisearch.Disp")/*Display*/, Command.ITEM, 2);
 	private final Command DEL_CMD = new Command(Locale.get("generic.Delete")/*Delete*/, Command.ITEM, 4);
 	private final Command CLEAR_CMD = new Command(Locale.get("guisearch.Clear")/*Clear*/, Command.ITEM, 5);
 	private final Command BOOKMARK_CMD = new Command(Locale.get("guisearch.Bookmark")/*Add to way points*/, Command.ITEM, 6);
 	private final Command BACK_CMD = new Command(Locale.get("generic.Back")/*Back*/, Command.BACK, 7);
 	private final Command OVERVIEW_MAP_CMD = new Command(Locale.get("guisearch.OverviewMap")/*Overview/Filter map*/, Command.ITEM, 8);
 	private final Command POI_CMD = new Command(Locale.get("guisearch.NearestPoi")/*Nearest POI*/, Command.ITEM, 9);
 	public final Command POI_URL_SEARCH_CMD = new Command(Locale.get("guiwebinfo.Websites")/*Nearby POIs with websites*/, Command.ITEM, 9);
 	private final Command POI_PHONE_SEARCH_CMD = new Command(Locale.get("guiwebinfo.Phones")/*Nearby POIs with phone numbers*/, Command.ITEM, 9);
 	private final Command SORT_CMD = new Command(Locale.get("guisearch.Sort")/*Toggle sort order (exper.)*/, Command.ITEM, 10);
 	private final Command FULLT_CMD = new Command(Locale.get("guisearch.Fulltext")/*Fulltext search*/, Command.ITEM, 10);
 	private final Command URL_CMD = new Command(Locale.get("guisearch.OpenURL")/*Open URL*/, Command.ITEM, 11);
 	private final Command PHONE_CMD = new Command(Locale.get("guisearch.Phone")/*Call Phone*/, Command.ITEM, 12);
 
 	//private Form form;
 
 	private final Image waypointIcon = Image.createImage("/waypoint.png");
 
 	private final Trace parent;
 
 	private Vector result = new Vector();
 	
 	// this array is used to get a copy of the waypoints for the favorites
 	public PositionMark[] wayPts = null;
 	public boolean showAllWayPts = false;
 	public boolean sortByDist = false;
 	
 	public static GuiSearchLayout gsl = null;
 
 	/**
 	 * This vector is used to buffer writes,
 	 * so that we only have to synchronize threads
 	 * at the end of painting
 	 */
 	private Vector result2 = new Vector();
 
 	private int carret=0;
 
 	private int cursor=0;
 	
 	private int scrollOffset = 0;
 
 	private StringBuffer searchCanon = new StringBuffer();
 
 	private boolean searchAlpha = false;
 
 	private SearchNames searchThread;
 
 	//private boolean abortPaint = false;
 	private volatile boolean needsPainting;
 	
 	public int displayReductionLevel = 0;
 	
 	private volatile TimerTask timerT;
 	private volatile TimerTask housenumberTimerTask = null;
 	private volatile Timer timer;
 	
 	private boolean hideKeypad = false;
 
 	private int width = 0;
 	private	int height = 0;
 
 	private ChoiceGroup poiSelectionCG;
 	private TextField poiSelectionMaxDistance;
 	private TextField fulltextSearchField;
 	
 	
 	public volatile byte state;
 	
 	public volatile int filter;
 	public final static byte FILTER_BIT_URLS = 1;
 	public final static byte FILTER_BIT_PHONES = 2;
 	
 	public final static byte STATE_MAIN = 0;
 	public final static byte STATE_POI = 1;
 	public final static byte STATE_FULLTEXT = 2;
 	public final static byte STATE_FAVORITES = 3;
 	public final static byte STATE_SEARCH_PROGRESS = 4;
 	public final static byte STATE_POI_URLS = 5;
 	public final static byte STATE_POI_PHONES = 6;
 	
 	private volatile int fontSize;
 	
 	private volatile boolean isSearchCanceled;
 	
 	/**
 	 * Record the time at which a pointer press was recorded to determine
 	 * a double click
 	 */
 	private long pressedPointerTime;
 	/**
 	 * Stores if there was already a click that might be the first click in a double click
 	 */
 	private boolean potentialDoubleClick;
 	/**
 	 * Indicates that there was a drag event since the last pointerPressed
 	 */
 	private boolean pointerDragged;
 	/**
 	 * Indicates that there was a rather far drag event since the last pointerPressed
 	 */
 	private static volatile boolean pointerDraggedMuch = false;
 	/**
 	 * Stores the position of the X coordinate at which the pointer started dragging since the last update 
 	 */
 	private int pointerXDragged;
 	/**
 	 * Stores the position of the Y coordinate at which the pointer started dragging since the last update 
 	 */
 	private int pointerYDragged;
 	/**
 	 * Stores the position of the initial pointerPress to identify dragging
 	 */
 	private int pointerXPressed;
 	private int pointerYPressed;
 	private int clickIdxAtSlideStart;
 	/**
 	 * indicates if the next release event is valid or the corresponding pointer pressing has already been handled
 	 */
 	private volatile boolean pointerActionDone;
 	
 	/** timer checking for long tap */
 	private volatile TimerTask tapAutoReleaseTimerTask = null;
 	private final int TAP_AUTORELEASE_DELAY = 500;
 
 	private KeySelectMenu poiTypeForm;
 
 	//#if polish.api.bigsearch
 	private Hashtable matchSources = null;
 	private Hashtable matchLats = null;
 	private Hashtable matchLons = null;
 	//#endif
 
 	private boolean spacePressed = false;
 	public volatile String words = "";
 
 	public GuiSearch(Trace parent) throws Exception {
 		super();
 		this.parent = parent;
 		setCommandListener(this);
 		
 		searchThread = new SearchNames(this);
 		if (Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_ROUTING_OPTIMIZED)) {
 			addCommand(ROUTE1_CMD);			
 			addCommand(OK2_CMD);
 		} else {
 			addCommand(OK1_CMD);
 			addCommand(ROUTE2_CMD);			
 		}
 		addCommand(DISP_CMD);
 		addCommand(DEL_CMD);
 		addCommand(CLEAR_CMD);
 		addCommand(BOOKMARK_CMD);
 		addCommand(BACK_CMD);
 		addCommand(OVERVIEW_MAP_CMD);
 		addCommand(POI_CMD);
                 if (Legend.enableUrlTags && Configuration.getCfgBitSavedState(Configuration.CFGBIT_ONLINE_WEBSITE)) {
 			addCommand(POI_URL_SEARCH_CMD);
 		}
                 if (Legend.enablePhoneTags && Configuration.getCfgBitSavedState(Configuration.CFGBIT_ONLINE_PHONE)) {
 			addCommand(POI_PHONE_SEARCH_CMD);
 		}
 		addCommand(FULLT_CMD);
 		addCommand(SORT_CMD);
 		if (Legend.enableUrlTags && Configuration.getCfgBitSavedState(Configuration.CFGBIT_ONLINE_WEBSITE)) {
 			addCommand(URL_CMD);
 		}
 		if (Legend.enablePhoneTags && Configuration.getCfgBitSavedState(Configuration.CFGBIT_ONLINE_PHONE)) {
 			addCommand(PHONE_CMD);
 		}
 		
 		timerT = new TimerTask() {
 			public void run() {
 				if (needsPainting) {
 					repaint();
 				}
 			}			
 		};
 		
 		needsPainting = false;
 		try {
 			GpsMid.getTimer().schedule(timerT, 500, 500);
 		} catch (Exception e) {
 			logger.exception("Failed to initialize GuiSearch repaint timer", e);
 		}
 		
 		reSearch();
 
 		//#debug
 		logger.debug("GuiSearch initialisied");
 		
 	}
 
 	private boolean isCursorValid() {
 		return (cursor < result.size() && cursor >= 0 && result.size() != 0);
 	}
 	
 	public void commandAction(Command c, Displayable d) {
 //		System.out.println("got Command " + c);
 		if (state == STATE_MAIN || state == STATE_FAVORITES) {
 			if (c == URL_CMD || c == PHONE_CMD) {
 				if (!isCursorValid()) {
 					return;
 				}
 				SearchResult sr = (SearchResult) result.elementAt(cursor);
 				String url = null;
 				String phone = null;
 				if (c == PHONE_CMD) {
 					if (sr.phoneIdx != -1) {
 						phone = parent.getUrl(sr.phoneIdx);
 						if (phone != null) {
 							url = "tel:" + phone;
 						}
 					}
 				}
 				if (c == URL_CMD) {
 					if (sr.phoneIdx != -1) {
 						url = parent.getUrl(sr.urlIdx);
 					}
 				}
 				try {
 					if (url != null) {
 						//#if polish.api.online
 						GpsMid.getInstance().platformRequest(url);
 						//#else
 						// FIXME: show user the url or phone number
 						//#endif
 					}
 				} catch (Exception e) {
 					//mLogger.exception("Could not open url " + url, e);
 				}
 			}
 			if (c == OK1_CMD || c == OK2_CMD || c == ROUTE1_CMD || c == ROUTE2_CMD) {			
 				if (!isCursorValid()) {
 					return;
 				}
 				SearchResult sr = (SearchResult) result.elementAt(cursor);
 				//			System.out.println("select " + sr);
 				RoutePositionMark positionMark = new RoutePositionMark(sr.lat,sr.lon);
 				//#if polish.api.bigsearch
 				if (state == STATE_FAVORITES && sr.source == SearchNames.INDEX_WAYPOINTS) {
 					positionMark.displayName = wayPts[sr.nameIdx].displayName;
 				} else {
 					positionMark.nameIdx=sr.nameIdx;
 					positionMark.displayName=parent.getName(sr.nameIdx);
 				}
 				//#else
 				if (state == STATE_FAVORITES) {
 					positionMark.displayName = wayPts[sr.nameIdx].displayName;
 				} else {
 					positionMark.nameIdx=sr.nameIdx;
 					positionMark.displayName=parent.getName(sr.nameIdx);
 				}
 				//#endif
 				parent.setDestination(positionMark);
 				//#debug info
 				logger.info("Search selected: " + positionMark);
 				destroy();
 				parent.show();				
 				if (c == ROUTE1_CMD || c == ROUTE2_CMD) {
 					parent.performIconAction(Trace.ROUTING_START_WITH_MODE_SELECT_CMD);
 				}
 				return;
 			}
 			if (c == DISP_CMD) {			
 				if (!isCursorValid()) {
 					return;
 				}
 				SearchResult sr = (SearchResult) result.elementAt(cursor);				
 				parent.receivePosition(sr.lat, sr.lon, Configuration.getRealBaseScale());				
 				parent.show();				
 				destroy();
 				return;
 			}
 			if (c == BACK_CMD) {
 				destroy();
 				parent.show();
 				return;
 			}
 		} else if (state == STATE_FULLTEXT) {
 			if (c == BACK_CMD) {
 				state = STATE_MAIN;
 				show();
 				return;
 			}
 			if (c == OK_CMD) {
 				clearList();
 				setTitle();
 				searchCanon.setLength(0);
 				final CancelMonitorInterface cmi = this;
 				isSearchCanceled = false;
 				Thread t = new Thread(new Runnable() {
 					public void run() {
 						setTitle(Locale.get("guisearch.searchingdots")/*searching...*/);
 						show();
 						Vector names = parent.fulltextSearch(fulltextSearchField.getString().toLowerCase(), cmi);
 						for (int i = 0; i < names.size(); i++) {
 							if (cmi.monitorIsCanceled()) {
 									break;
 							}
 							searchCanon.setLength(0);
 							String name = (String)names.elementAt(i);
 							//#debug debug
 							logger.debug("Retrieving entries for " + name);							
 
 							// FIXME: Workaround for the full text search sometimes failing for substrings included at the end of names
 							// This change from "[ gpsmid-Patches-3002028 ] Improving full text search" reduces the number of failures
 							// see also: http://sourceforge.net/projects/gpsmid/forums/forum/677687/topic/3708460
 							// old code: searchThread.appendSearchBlocking(NumberCanon.canonial(name));							
 							if (name.length() < 21) {						
 								searchThread.appendSearchBlocking(NumberCanon.canonial(name));
 							} else {
 								searchThread.appendSearchBlocking(NumberCanon.canonial(name.substring(0,20)));
 							}
 						}
 						setTitle(Locale.get("guisearch.SearchResults")/*Search results:*/);
 						state = STATE_MAIN;
 						show();
 						triggerRepaint();
 					}
 				}, "fulltextSearch");
 				state = STATE_SEARCH_PROGRESS;
 				show();
 				t.start();
 			}
 		} else if (state == STATE_SEARCH_PROGRESS) {
 			if (c == BACK_CMD) {
 				state = STATE_MAIN;
 				isSearchCanceled = true;
 				show();
 				return;
 			}
 		}
 		if (c == DEL_CMD) {
 			if (carret > 0){
 				searchCanon.deleteCharAt(--carret);
 				reSearch();
 			}
 			return;
 		}
 		if (c == CLEAR_CMD) {
 			result.removeAllElements();
 			searchCanon.setLength(0);
 			searchAlpha = false;
 			hideKeypad = false;
 			//matchSources = new Hashtable();
 			//matchLats = new Hashtable();
 			//matchLons = new Hashtable();
 			//#if polish.api.bigsearch
 			matchSources = null;
 			matchLats = null;
 			matchLons = null;
 			//#endif
 			words = "";
 			spacePressed = false;
 			setTitle();
 			carret=0;
 			repaint();
 			return;
 		}
 		if (c == BOOKMARK_CMD) {
 			if (cursor >= result.size()) return;
 			SearchResult sr = (SearchResult) result.elementAt(cursor);
 			PositionMark positionMark = new PositionMark(sr.lat,sr.lon);
 			positionMark.displayName=parent.getName(sr.nameIdx);
 			parent.gpx.addWayPt(positionMark);
 			parent.show();
 			return;
 		}
 		
 		if (c == OVERVIEW_MAP_CMD) {
 			GuiOverviewElements ovEl = new GuiOverviewElements(parent);
 			ovEl.show();
 		}
 		
 		if (c == POI_CMD) {
 			state = STATE_POI;
 			filter = 0;
 			try{
 				poiTypeForm = new GuiPOItypeSelectMenu(this, this);
 				poiTypeForm.show();
 			} catch (Exception e) {
 				logger.exception(Locale.get("guisearch.FailedToSelectPOIType")/*Failed to select POI type*/, e);
 				state = STATE_MAIN;
 				show();
 			}
 			
 		}
 		if (c == POI_URL_SEARCH_CMD) {
 			filter = 1 << FILTER_BIT_URLS;
 			state = STATE_POI_URLS;
 			try{
 				poiTypeForm = new GuiPOItypeSelectMenu(this, this);
 				poiTypeForm.show();
 			} catch (Exception e) {
 				logger.exception(Locale.get("guisearch.FailedToSelectPOIType")/*Failed to select POI type*/, e);
 				state = STATE_MAIN;
 				show();
 			}
 		}
 		if (c == POI_PHONE_SEARCH_CMD) {
 			filter = 1 << FILTER_BIT_PHONES;
 			state = STATE_POI_URLS;
 			try{
 				poiTypeForm = new GuiPOItypeSelectMenu(this, this);
 				poiTypeForm.show();
 			} catch (Exception e) {
 				logger.exception(Locale.get("guisearch.FailedToSelectPOIType")/*Failed to select POI type*/, e);
 				state = STATE_MAIN;
 				show();
 			}
 		}
 		if (c == SORT_CMD) {
 				sortByDist = !sortByDist;
 				reSearch();
 				return;
 		}
 		if (c == FULLT_CMD) {
 			Form fulltextForm = new Form(Locale.get("guisearch.Fulltext")/*Fulltext search*/);
 			String match = "";
 			String name = null;
 			if (isCursorValid()) {
 				SearchResult sr = (SearchResult) result.elementAt(cursor);
 				if (state == STATE_FAVORITES) {
 					name = wayPts[sr.nameIdx].displayName;
 				} else {
 					name = parent.getName(sr.nameIdx);
 				}
 				int imatch=searchCanon.length(); 
 				if (name != null && name.length()<imatch) { 
 					imatch=name.length(); 
 				}
 				if (name != null) {
 					match = name.substring(0,imatch);
 				}
 			}
 			fulltextSearchField = new TextField(Locale.get("guisearch.Find")/*Find: */, 
 							    (state == STATE_FAVORITES && name != null ) ?
 							    name :
 							    (searchAlpha ? searchCanon.toString() : match), 40, TextField.ANY);
 			state = STATE_FULLTEXT;
 			fulltextForm.append(fulltextSearchField);
 			fulltextForm.addCommand(BACK_CMD);
 			fulltextForm.addCommand(OK_CMD);
 			fulltextForm.setCommandListener(this);
 			GpsMid.getInstance().show(fulltextForm);			
 		}
 
 	}
 
 	private void destroy() {
 		searchThread.shutdown();
 		searchThread=null;
 	}
 
 	public void show() {
 		hideKeypad = false;
 		height = getHeight();
 		width = getWidth();
 		gsl = new GuiSearchLayout(0, 0, width, height);
 		potentialDoubleClick = false;
 		pointerDragged = false;
 		if (state == STATE_SEARCH_PROGRESS) {
 			Form f = new Form(Locale.get("guisearch.SearchingdotsForm")/*Searching...*/);
 			f.addCommand(BACK_CMD);
 			f.setCommandListener(this);
 			GpsMid.getInstance().show(f);
 		} else {
 			GpsMid.getInstance().show(this);
 			//Display.getDisplay(parent.getParent()).setCurrent(this);
 		}
 		repaint();
 	}
 
 	public void sizeChanged(int w, int h) {
 		width = w;
 		height = h;
 		gsl = new GuiSearchLayout(0, 0, w, h);
 		repaint();
 	}
 
 	//#if polish.api.bigsearch
 	private void nextWord() {
 		words = words + searchCanon.toString() + " ";
 		searchCanon.setLength(0);
 		carret = 0;
 		spacePressed = false;
 	}
 	//#endif
 
 	// insert new results from search thread 
 	private void insertResults() {
 		if (result2.size() > 0) {
 			synchronized(this) {				
 				for (int i = 0; i < result2.size(); i++ ) {
 					SearchResult res = (SearchResult) result2.elementAt(i);
 					String name = null;
 					//#if polish.api.bigsearch
 					Long id = new Long(res.resultid);
 					if (state == STATE_FAVORITES && res.source == SearchNames.INDEX_WAYPOINTS) {
 						name = wayPts[res.nameIdx].displayName;
 					} else {
 						name = parent.getName(res.nameIdx);
 					}
 					//#else
 					if (state == STATE_FAVORITES) {
 						name = wayPts[res.nameIdx].displayName;
 					} else {
 						name = parent.getName(res.nameIdx);
 					}
 					//#endif
 					//System.out.println ("MatchMode: " + matchMode());
 					//System.out.println ("insertresults name: " + name);
 					//System.out.println ("parent.getName: " + parent.getName(res.nameIdx));
 					// FIXME repeating code, combine
 					// avoid string index out of bound
 					int len = searchCanon.length();
 					if (name != null && name.length() < len) {
 						len = name.length();
 					}
 					if (Configuration.getCfgBitState(Configuration.CFGBIT_WORD_ISEARCH) ||
 					    matchMode() ||
 					    !searchAlpha || name == null ||
 					    searchCanon.toString().equalsIgnoreCase(name.substring(0, len))) {
 						//#if polish.api.bigsearch
 						// match multiword search or housenumber search
 						//System.out.println ("MatchMode: " + matchMode() + " matchSources: " + matchSources);
 						if (matchMode() && matchSources != null) {
 							if (matchSources.get(id) != null) {
 								result.addElement(res);
 							}
 						} else {
 							result.addElement(res);
 						}
 						//#else
 						result.addElement(res);
 						//#endif
 					}
 				}
 				result2.removeAllElements();
 			}
 		}
 	}
 
 	protected void paint(Graphics gc) {
 		//#debug debug
 		logger.debug("Painting search screen with offset: " + scrollOffset);
 		if (fontSize == 0)
 			fontSize = gc.getFont().getHeight();		
 		int yc=scrollOffset;
 		int reducedName=0;
 		gc.setColor(Legend.COLORS[Legend.COLOR_SEARCH_BACKGROUND]);
 		gc.fillRect(0, 0, getWidth(), getHeight());
 		if (Configuration.getCfgBitSavedState(Configuration.CFGBIT_SEARCH_TOUCH_NUMBERKEYPAD)) {
 			gc.setColor(Legend.COLORS[Legend.COLOR_SEARCH_BUTTON_TEXT]);
 			if (hasPointerEvents() && ! hideKeypad) {
 				if (gsl == null) {
 					gsl = new GuiSearchLayout(0, 0, width, height);
 				}
 			
 				String letters[] = {  Locale.get("guisearch.sort")/*sort*/, "  X  ", "  <- ", 
 						      Configuration.getCfgBitState(Configuration.CFGBIT_WORD_ISEARCH) ?
 						      Locale.get("guisearch.label1wordSearch")/* 1*- */ :
 						      Locale.get("guisearch.label1")/*_1*- */,
 						      Locale.get("guisearch.label2")/* abc2*/,
 						      Locale.get("guisearch.label3")/* def3*/, Locale.get("guisearch.label4")/* ghi4*/,
 						      Locale.get("guisearch.label5")/* jkl5*/, Locale.get("guisearch.label6")/* mno6*/,
 						      Locale.get("guisearch.label7")/*pqrs7*/, Locale.get("guisearch.label8")/* tuv8*/,
 						      Locale.get("guisearch.label9")/*wxyz9*/, 
 						      Locale.get("guisearch.more")/*more*/, "  0  ", 
 						      Configuration.getCfgBitState(Configuration.CFGBIT_WORD_ISEARCH) ?
 						      Locale.get("guisearch.pound")/*_#end*/ :
 						      Locale.get("guisearch.poundNameSearch")/*#end*/};
 				for (int i = 0; i < 15 ; i++) {
 					// hide sort when more than 2 chars typed
 					if (i == 0 /* sort */ && carret > 2) {
 						gsl.ele[i].setText(" ");
 					} else {
 						gsl.ele[i].setText(letters[i]);
 					}
 				}
 				gsl.paint(gc);
 			}
 		}
 		if (yc < 0) {
 			gc.setColor(Legend.COLORS[Legend.COLOR_SEARCH_ARROWS]);
 			gc.drawString("^", getWidth(), 0, Graphics.TOP | Graphics.RIGHT);
 		}
 		// insert new results from search thread 
 		insertResults();
 
 	    needsPainting = false;
 	    
 	    // keep cursor within bounds
 	    if (cursor!=0 && cursor >= result.size()) {
 		    cursor = result.size() - 1;
 	    }
 	    StringBuffer nameb=new StringBuffer();
 	    StringBuffer nearNameb=new StringBuffer();
 
 	    for (int i=0;i<result.size();i++){	    	
 			if (yc < 0) {
 				yc += fontSize;
 				continue;
 			}
 			if (yc > getHeight()) {
 				gc.setColor(Legend.COLORS[Legend.COLOR_SEARCH_ARROWS]);
 				gc.drawString("v", getWidth(), getHeight() - 7,
 						Graphics.BOTTOM | Graphics.RIGHT);				
 				return;
 			}
 
 			if (i == cursor){
 				gc.setColor(Legend.COLORS[Legend.COLOR_SEARCH_SELECTED_TYPED]);
 			} else {
 				gc.setColor(Legend.COLORS[Legend.COLOR_SEARCH_NONSELECTED_TYPED]);
 			}
 			SearchResult sr=(SearchResult) result.elementAt(i);
 			String flags="";
 			if (sr.urlIdx != -1) {
 				flags = flags + "W";
 			}
 			if (sr.phoneIdx != -1) {
 				flags = flags + "P";
 			}
 			if (sr.urlIdx != -1 || sr.phoneIdx != -1) {
 				flags = flags + " ";
 			}
 			Image img = null;
 			if (sr.type < 0) {
 				img = Legend.getNodeSearchImage((byte)(sr.type*-1));
 			} else if (sr.type == 0 ) {
 				img = waypointIcon;
 			} else { // type > 0
 				img = Legend.getWayOrAreaSearchImage(sr.type);
 			}
 			if (img != null)
 				gc.drawImage(img, 8, yc + fontSize / 2 - 1, Graphics.VCENTER | Graphics.HCENTER);
 			String name = null;
 			//#if polish.api.bigsearch
 			if (state != STATE_FAVORITES || sr.source != SearchNames.INDEX_WAYPOINTS) {
 			//#else
 			if (state != STATE_FAVORITES) {
 			//#endif
 				name = flags + getName(sr.nameIdx);
 			} else {
 				if (wayPts.length > sr.nameIdx) {
 					name = wayPts[sr.nameIdx].displayName;
 				}
 			}
 			nameb.setLength(0);
 			if (name != null){
 				if (displayReductionLevel < 1) {
 					nameb.append(name);
 					reducedName=0;
 				} else {
 					reducedName=1;
 					nameb.append(name.charAt(0));
 					nameb.append('.');
 				}
 			}
 
 			if (sr.nearBy != null){
 				for (int ib=sr.nearBy.length; ib-- != 0;){
 					nameb.append(" / ");
 					nearNameb.setLength(0);
 					nearNameb.append(getName(sr.nearBy[ib]));
 					if (displayReductionLevel < (sr.nearBy.length - ib + 1)) {
 						nameb.append(nearNameb);
 						reducedName=0;
 					} else {
 						reducedName=2;
 						nameb.append(nearNameb.charAt(0));
 						nameb.append('.');
 					}					
 				}
 			}
 			// always show last name part unreduced
 			if(reducedName!=0 && nameb.length()>=2) {
 				// only if the result is more than once reduced (for POIs) or the result has a nearby entry
 				if (displayReductionLevel > 1 || sr.nearBy != null) {
 					nameb.setLength(nameb.length()-2);
 					if(reducedName==1) {
 						nameb.append(name);
 					}
 					else {
 						nameb.append(nearNameb.toString());					
 					}
 				}
 			}
 			appendCompassDirection(nameb, sr);
 			name=nameb.toString();
 			if (name != null) {
 				// avoid index out of bounds 
 				int imatch=searchCanon.length(); 
 				if (name.length()<imatch) { 
 					imatch=name.length(); 
 				} 
 				// when display is reduced only 1st char matches 
 				if (displayReductionLevel > 0) { 
 					imatch=1; 
 				} 
 
 				// name part identical to search string 
 				if (Configuration.getCfgBitState(Configuration.CFGBIT_WORD_ISEARCH) || matchMode()) {
 					// FIXME could use improvement, maybe put match in the middle of screen
 					if (i == cursor){ 
 						gc.setColor(Legend.COLORS[Legend.COLOR_SEARCH_SELECTED_TYPED]);
 					} else { 
 						gc.setColor(Legend.COLORS[Legend.COLOR_SEARCH_NONSELECTED_TYPED]);
 					}
 					gc.drawString(name, 17, yc, Graphics.TOP | Graphics.LEFT); 
 				} else {	
 					// name part identical to search string 
 					if (i == cursor){ 
 						gc.setColor(Legend.COLORS[Legend.COLOR_SEARCH_SELECTED_TYPED]);
 					} else { 
 						gc.setColor(Legend.COLORS[Legend.COLOR_SEARCH_NONSELECTED_TYPED]);
 					}
 					gc.drawString(name.substring(0,imatch+flags.length()), 17, yc, Graphics.TOP | Graphics.LEFT); 
 					// remaining name part 
 					if (i == cursor){ 
 						gc.setColor(Legend.COLORS[Legend.COLOR_SEARCH_SELECTED_REST]);
 					} else { 
 						gc.setColor(Legend.COLORS[Legend.COLOR_SEARCH_NONSELECTED_REST]);
 					} 
 					gc.drawString(name.substring(imatch+flags.length()), 17 + gc.getFont().stringWidth(name.substring(0,imatch+flags.length())) , yc, Graphics.TOP | Graphics.LEFT);
 				}
 				// carret 
 				if(!(Configuration.getCfgBitState(Configuration.CFGBIT_WORD_ISEARCH) || matchMode())
 				   && carret<=imatch && displayReductionLevel<1) { 
 					int cx=17 + gc.getFont().stringWidth(name.substring(0,carret+flags.length())); 
 					gc.setColor(Legend.COLORS[Legend.COLOR_SEARCH_SELECTED_TYPED]);
 					gc.drawLine(cx-1,yc+fontSize,cx+1,yc+fontSize); 
 				}
 			}
 			else 
 				gc.drawString("..." + sr.nameIdx,17, yc, Graphics.TOP | Graphics.LEFT);
 			yc+=fontSize;
 		}
 	}
 	
 	protected void keyRepeated(int keyCode) {
 		//Moving the cursor should work with repeated keys the same
 		//as pressing the key multiple times
 		int action = this.getGameAction(keyCode);
 		// System.out.println("repeat key " + keyCode + " " + action);
 		if ((action == UP) || (action == DOWN) ||
 			(action == LEFT) || (action == RIGHT) ||
 			(keyCode == -8) ) {
 			keyPressed(keyCode);
 			return;
 		}
 	}
 
 	protected void keyPressed(int keyCode) {
 		int action = getGameAction(keyCode);
 		/** Ignore gameActions from unicode character keys (space char and above).
 		 *  By not treating those keys as game actions they get added to the search canon
 		 *  by default if not handled otherwise */
 		if (keyCode >= 32) {
 			action = 0;
 		}
 		if (spacePressed) {
 			//#if polish.api.bigsearch
 			filterMatches();
 			nextWord();
 			//#endif
 		}
 		logger.info("Search dialog: got key " + keyCode + " " + action);
 		if (keyCode >= KEY_NUM0 && keyCode <= KEY_NUM9) {
 			/*  KEY_NUM constants match "(char) keyCode" */
 			searchCanon.insert(carret++, (char) keyCode);
 		//#if polish.api.bigsearch
 		} else if (keyCode == KEY_POUND || (keyCode == 32 && Configuration.getCfgBitState(Configuration.CFGBIT_WORD_ISEARCH))) {
 			// switch to another word, start searching in AND mode
 			// collect a list of long entity id's, mark
 
 			// first wait for all results
 			// then do a search for housenumbers and whole words
 
 			if (!spacePressed) {
 				// first time here, search for whole words
 				//System.out.println("space pressed, searching whole word index");
 				String searchString = NumberCanon.canonial(searchCanon.toString());
 				if (Configuration.getCfgBitState(Configuration.CFGBIT_WORD_ISEARCH)) {
 					searchThread.appendSearchBlocking(searchString, SearchNames.INDEX_WORD);
 				} else {
 				//#if polish.api.bigsearch
 					searchThread.appendSearchBlocking(searchString, SearchNames.INDEX_BIGNAME);
 				//#else
 					searchThread.appendSearchBlocking(searchString, SearchNames.INDEX_NAME);
 				//#endif
 				}
 				searchThread.appendSearchBlocking(searchString, SearchNames.INDEX_HOUSENUMBER);
 				//searchThread.appendSearchBlocking(searchString, SearchNames.INDEX_WHOLEWORD);
 				// insert new results from search thread 
 				insertResults();
 				if (matchMode()) {
 					//filterMatches();
 					//repaint(0, 0, getWidth(), getHeight());
 					spacePressed = true;
 					return;
 				} else {
 					//#if polish.api.bigsearch
 					storeMatches();
 					nextWord();
 					//#endif
 				}
 				if (keyCode == KEY_POUND && state == STATE_FAVORITES) {
 					// FIXME make this work
 					sortByDist = !sortByDist;
 					reSearch();
 					return;
 				}
 			}
 		//#else
 		} else if (keyCode == KEY_POUND && state != STATE_FAVORITES) {
 			searchCanon.insert(carret++,'1');				
 		//#endif
 		} else if (keyCode == KEY_STAR) {
 			if (state == STATE_FAVORITES || searchCanon.length() < 2 ) {
 				showAllWayPts = !showAllWayPts;
 				reSearch();
 				return;
 			} else {
 				displayReductionLevel++;
 				if (displayReductionLevel > 3)
 					displayReductionLevel = 0;
 				repaint(0, 0, getWidth(), getHeight());
 				return;
 			}
 			// Unicode character 10 is LF
 			// so 10 should correspond to Enter key on QWERT keyboards
 		} else if (keyCode == 10 || action == FIRE) {
 			if (Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_ROUTING_OPTIMIZED)) {
 				commandAction( ROUTE1_CMD, (Displayable) null);
 			} else {
 				commandAction( OK1_CMD, (Displayable) null);				
 			}
 			return;
 		} else if (action == UP) {
 			if (cursor > 0)
 				cursor--;			
 			if (cursor * fontSize + scrollOffset < 0) {
 				scrollOffset += 3*fontSize;
 			}
 			if (scrollOffset > 0)
 				scrollOffset = 0;
 			repaint(0, 0, getWidth(), getHeight());
 			return;
 		} else if (action == DOWN) {
 			if (cursor < result.size() - 1)
 				cursor++;			
 			if (((cursor + 1) * fontSize + scrollOffset) > getHeight()) {
 				scrollOffset -= 3*fontSize;
 			}
 
 			if (scrollOffset > 0)
 				scrollOffset = 0;
 
 			repaint(0, 0, getWidth(), getHeight());
 			return;
 		} else if (action == LEFT) {
 			if (carret > 0)
 				carret--;
 			repaint(0, 0, getWidth(), getHeight());
 			return;
 		} else if (action == RIGHT) {
 			if (carret < searchCanon.length()) {
 				carret++;
 			} else {
 				if (isCursorValid())   {
 					SearchResult sr = (SearchResult) result.elementAt(cursor);
 					String name = null;
 					if (state == STATE_FAVORITES) {
 						name = wayPts[sr.nameIdx].displayName;
 					} else {
 						name = parent.getName(sr.nameIdx);
 					}
 					if (carret < name.length()) {
 						searchAlpha = true;
 						char nameLetters[] = name.toCharArray();
 						searchCanon.insert(carret++,nameLetters[carret-1]);
 					}
 				}
 			}
 
 			repaint(0, 0, getWidth(), getHeight());
 			return;
 		} else if (keyCode == -8 || keyCode == 8 || keyCode == 127) { 
 			/** Non standard Key -8: hopefully is mapped to
 			 * the delete / clear key. According to 
 			 * www.j2meforums.com/wiki/index.php/Canvas_Keycodes
 			 * most major mobiles that have this key map to -8
 			 * 
 			 * Unicode Character Key: 8 is backspace so this should be standard
 			 * Keycode 127 is Clear-Key passed by MicroEmulator
 			 **/
 			
 			if (carret > 0){
 				searchCanon.deleteCharAt(--carret);
 			} else if (matchMode()) {
 				// FIXME in multi-word search should also handle situation with more than two words
 			        // by redoing the word searches
 				int slen = words.length()-1;
 				searchCanon.setLength(0);
 				for (int i = 0; i < slen ; i++) {
 					searchCanon.insert(carret++, (char) words.charAt(i));
 				}
				//#if polish.api.bigsearch
				matchSources = null;
				matchLats = null;
				matchLons = null;
				//#endif
 				//System.out.println("Searchcanon tostring: " + searchCanon.toString());
 				//System.out.println("Searchcanon length: " + searchCanon.length());
 				words = "";
 				carret = slen;
 				spacePressed = false;
 				result.removeAllElements();
 				reSearch();
 			}
 		} else if (keyCode == -111) {
 			/**
 			 * Do not reSearch() after Android MENU key is pressed, otherwise selected result looses focus
 			 **/
 			return;
 		} else {
 			// filter out special keys such as shift key (-50), volume keys, camera keys...
 			if (keyCode > 0) {
 				// this is an alpha key (letter), set flag for post-filtering of results
 				searchAlpha = true;
 				searchCanon.insert(carret++,(char)keyCode);
 			}
 		}
 		//#if polish.api.bigsearch
 		if (housenumberTimerTask != null) {
 			housenumberTimerTask.cancel();
 			housenumberTimerTask = null;
 		}
 		if (housenumberTimerTask == null) {
 			housenumberTimerTask = new TimerTask() {
 				public void run() {
 					if (matchMode()) {
 						//System.out.println("housenumber timer fired");
 						searchThread.appendSearchBlocking(NumberCanon.canonial(searchCanon.toString()),
 										  SearchNames.INDEX_HOUSENUMBER);
 						insertResults();
 						boolean matchall = true;
 						int limit = 0;
 						if (matchall && searchCanon.toString().length() == 1) {
 							limit = 10;
 							// uncomment the following two lines to test showing all numbers for streetname
 							// FIXME names will be missing however from the list, but will show an idea
 							// of the performance
 						//
 						} else if (matchall && searchCanon.toString().length() == 0) {
 							limit = 100;
 						}
 						for (int i = 0; i < limit; i++) {
 							String ss = searchCanon.toString();
 							if (limit > 1) {
 								ss = ss + Integer.toString(i);
 							}
 							//System.out.println("search for: " + ss);
 
 							searchThread.appendSearchBlocking(NumberCanon.canonial(ss),
 											  SearchNames.INDEX_HOUSENUMBER);
 							insertResults();
 						}
 						//searchThread.appendSearchBlocking(NumberCanon.canonial(ss),
 						// SearchNames.INDEX_WHOLEWORD);
 						// insert new results from search thread 
 					}
 				}
 			};
 		}
 		if (matchMode() && housenumberTimerTask != null) {
 			try {
 				//System.out.println("scheduling housenumber timer");
 				GpsMid.getTimer().schedule(housenumberTimerTask, 1500);
 			} catch (Exception e) {
 				logger.exception("Failed to initialize GuiSearch housenumber search timer", e);
 			}
 		}
 		
 		if (searchCanon.length() > 1 || matchMode()) {
 			state = STATE_MAIN;
 			reSearch();
 		}
 		if (spacePressed) {
 			repaint(0, 0, getWidth(), getHeight());
 		} else {
 			//System.out.println("zeroing spacePressed");
 			reSearch();
 		}
 		//#else
 		if (searchCanon.length() > 1) {
 			state = STATE_MAIN;
 		}
 		//#endif
 	}
 	private boolean matchMode() {
 		return (!words.equals(""));
 	}
 
 	//#if polish.api.bigsearch
 	private void storeMatches() {
 		SearchResult sr = null;
 		if (matchSources == null) {
 			matchSources = new Hashtable();
 		}
 		if (matchLats == null) {
 			matchLats = new Hashtable();
 		}
 		if (matchLons == null) {
 			matchLons = new Hashtable();
 		}
 
 		for (int i = 0; i < result.size(); i++) {
 			sr = (SearchResult) result.elementAt(i);
 			Long id = new Long(sr.resultid);
 			Float Lat = new Float(sr.lat);
 			Float Lon = new Float(sr.lon);
 			Integer source = new Integer(sr.source);
 			matchSources.put(id, source);
 			matchLats.put(id, Lat);
 			matchLons.put(id, Lon);
 			//System.out.println("Adding result: " + sr.resultid + " sr.source/housenum: " + sr.source + "/" + SearchNames.INDEX_HOUSENUMBER);
 			//System.out.println("Store match, adding, source = " + ((Integer) matchSources.get(id)).intValue());
 		}
 		//showMatchSources();
 	}
 	// filter matches for possible second space press
 	// also transfer node (housenumber) coordinates to ways when necessary
 	private void filterMatches() {
 		SearchResult sr = null;
 
 		Hashtable matchNewSources = new Hashtable();
 
 		for (int i = 0; i < result.size(); i++) {
 			sr = (SearchResult) result.elementAt(i);
 			Long id = new Long(sr.resultid);
 			// transfer house number coordinates to street
 			Integer sourceNew = new Integer(sr.source);
 			if (matchSources.get(id) != null) {
 				//System.out.println("found match from old results, id = "
 				//		   + id + "source = "
 				//		   + ((Integer) matchSources.get(id)).intValue());
 				//if (((Integer) matchSources.get(id)).intValue() == SearchNames.INDEX_HOUSENUMBER && matchLats.get(id) != null) {
 				// get more exact coordinates from old match if current match is a way
 				if (sr.type > 0 && matchLats != null && matchLats.get(id) != null) {
 					sr.lat = ((Float) matchLats.get(id)).floatValue();
 					sr.lon = ((Float) matchLons.get(id)).floatValue();
 					sourceNew = (Integer) matchSources.get(id);
 				}
 				//	result.removeElementAt(i);
 				//	result.addElement(sr);
 			}
 			if (sr.type < 0 && matchLats.get(id) != null) {
 				// if new match is a node, save coordinates
 				Float Lat = new Float(sr.lat);
 				Float Lon = new Float(sr.lon);
 				matchLats.put(id, Lat);
 				matchLons.put(id, Lon);
 			}
 			matchNewSources.put(id, sourceNew);
 		}
 		matchSources = matchNewSources;
 		//showMatchSources();
  	}
 	//private void showMatchSources() {
 	//	Enumeration e = matchSources.keys();
 	//	while(e.hasMoreElements()) {
 	//		Long key = ((Long) e.nextElement());
 	//		int value =((Integer) matchSources.get(key)).intValue();
 	//		System.out.println ("key: " + key + " value: " + value);
 	//	}
 	//}
 	//#endif
 
 	public void pointerPressed(int x, int y) {
 		//#debug debug
 		logger.debug("PointerPressed: " + x + "," + y);
 		pointerActionDone = false;
 		pointerDraggedMuch = false;
 		long currTime = System.currentTimeMillis();
 		if (potentialDoubleClick) {
 			if ((currTime - pressedPointerTime > 400)) {
 				potentialDoubleClick = false;
 				pressedPointerTime = currTime;
 			}
 		} else {
 			pressedPointerTime = currTime;
 		}
 		pointerXDragged = x;
 		pointerYDragged = y;
 
 		// when the display is pressed again before the auto release,
 		// cancel any outstanding auto release timer and perform the corresponding action immediately instead 
 		if (tapAutoReleaseTimerTask != null) {
 			tapAutoReleaseTimerTask.cancel();
 			if (!pointerDraggedMuch) {
 				autoPointerRelease(pointerXPressed, pointerYPressed);
 			}	
 		}
 
 		pointerXPressed = x;
 		pointerYPressed = y;
 
 		/** if clicking above or below the search results show a text field to enter the search string */
 		int clickIdx = (y - scrollOffset)/fontSize;
 		if ( (state == STATE_MAIN || state == STATE_FAVORITES)
 			&& (clickIdx < 0 || clickIdx >= result.size() || ((clickIdx + 1) * fontSize + scrollOffset) > getHeight())
 		     && (hideKeypad || !Configuration.getCfgBitSavedState(Configuration.CFGBIT_SEARCH_TOUCH_NUMBERKEYPAD))
 
 		) {
 			GuiNameEnter gne = new GuiNameEnter(this, null, Locale.get("guisearch.SearchForNamesStarting")/*Search for names starting with:*/, searchCanon.toString(), 20);
 			gne.show();
 		} else {
 			if (Configuration.getCfgBitSavedState(Configuration.CFGBIT_SEARCH_TOUCH_NUMBERKEYPAD) && !hideKeypad
 			    && gsl.getElementIdAtPointer(x, y) >= 0 && gsl.isAnyActionIdAtPointer(x, y)) {
 				int touchedElementId = gsl.getElementIdAtPointer(x, y);
 				if (touchedElementId >= 0
 				    &&
 				    gsl.isAnyActionIdAtPointer(x, y)
 					) {
 					//System.out.println("setTouchedElement: " + touchedElementId);
 					gsl.setTouchedElement((LayoutElement) gsl.elementAt(touchedElementId));
 					repaint();
 				}
 			}
 			tapAutoReleaseTimerTask = new TimerTask() {
 				public void run() {
 					tapAutoReleaseTimerTask = null;
 					// if no action (e.g. from double tap) is already done
 					// and the pointer did not move or if it was pressed on a control and not moved much
 					if (!pointerDraggedMuch) {
 						if (System.currentTimeMillis() - pressedPointerTime >= TAP_AUTORELEASE_DELAY){
 							/* automatically release the pointer as a workaround for S60V5 devices
 							 * which start drawing blue circles but give no pointerReleased() event
 							 * when holding down the pointer just a few ms with the finger
 							 */
 							autoPointerRelease(pointerXPressed, pointerYPressed);
 						}
 					}
 				}
 			};
 			try {
 				// set timer to continue check if this is a long tap
 				GpsMid.getTimer().schedule(tapAutoReleaseTimerTask, TAP_AUTORELEASE_DELAY);
 			} catch (Exception e) {
 				logger.error(Locale.get("trace.NoLongTapTimerTask")/*No LongTap TimerTask: */ + e.toString());
 			}
 		}
 		clickIdxAtSlideStart = clickIdx;
 	}
 
 	public void autoPointerRelease(int x, int y) {
 		int clickIdx = (y - scrollOffset)/fontSize;
 		long currTime = System.currentTimeMillis();
 		if (Configuration.getCfgBitSavedState(Configuration.CFGBIT_SEARCH_TOUCH_NUMBERKEYPAD) && !hideKeypad
 		    && gsl.getElementIdAtPointer(x, y) >= 0 && gsl.isAnyActionIdAtPointer(x, y)) {
 			int touchedElementId = gsl.getElementIdAtPointer(x, y);
 			if (touchedElementId >= 0
 			    &&
 			    gsl.isAnyActionIdAtPointer(x, y)
 				) {
 				//gsl.setTouchedElement((LayoutElement) gsl.elementAt(touchedElementId));
 				//repaint();
 				if (touchedElementId == GuiSearchLayout.KEY_1) {
 					keyPressed('1');
 				} else if (touchedElementId == GuiSearchLayout.KEY_2) {
 					keyPressed('2');
 				} else if (touchedElementId == GuiSearchLayout.KEY_3) {
 					keyPressed('3');
 				} else if (touchedElementId == GuiSearchLayout.KEY_4) {
 					keyPressed('4');
 				} else if (touchedElementId == GuiSearchLayout.KEY_5) {
 					keyPressed('5');
 				} else if (touchedElementId == GuiSearchLayout.KEY_6) {
 					keyPressed('6');
 				} else if (touchedElementId == GuiSearchLayout.KEY_7) {
 					keyPressed('7');
 				} else if (touchedElementId == GuiSearchLayout.KEY_8) {
 					keyPressed('8');
 				} else if (touchedElementId == GuiSearchLayout.KEY_9) {
 					keyPressed('9');
 				} else if (touchedElementId == GuiSearchLayout.KEY_0) {
 					keyPressed('0');
 				} else if (touchedElementId == GuiSearchLayout.KEY_STAR) {
 					keyPressed(KEY_STAR);
 				} else if (touchedElementId == GuiSearchLayout.KEY_POUND || touchedElementId == GuiSearchLayout.KEY_SORT) {
 					keyPressed(KEY_POUND);
 				} else if (touchedElementId == GuiSearchLayout.KEY_BACKSPACE) {
 					keyPressed(8);
 				} else if (touchedElementId == GuiSearchLayout.KEY_CLOSE) {
 					// hide keypad
 					hideKeypad = true;
 				} else if (touchedElementId == GuiSearchLayout.KEY_POUND) {
 					keyPressed(KEY_POUND);
 				}
 			}
 		
 		} else {
 			// if touching the right side of the display (150% font height) this equals to the * key 
 			if (x > getWidth() - fontSize * 3 / 2) {
 				keyPressed(KEY_STAR);
 			} else {
 				// else position the cursor
 				potentialDoubleClick = true;			
 				cursor = clickIdx;
 			}		
 		}
 		if (gsl != null) {
 		    gsl.clearTouchedElement();
 		}
 		repaint();
 	}
 
 	public void pointerReleased(int x, int y) {
 		// avoid division by zero when releasing pointer before screen is drawn
 		if (fontSize == 0) {
 			return;
 		}
 		long currTime = System.currentTimeMillis();
 		int clickIdx = (y - scrollOffset)/fontSize;
 		//#debug debug
 		logger.debug("PointerReleased: " + x + "," + y);
 		pointerActionDone = true;
 
 		// If this could be a double click
 		if (potentialDoubleClick
 				&&
 			(
 				// but never in the virtual keypad
 				!Configuration.getCfgBitSavedState(Configuration.CFGBIT_SEARCH_TOUCH_NUMBERKEYPAD) || hideKeypad || !gsl.isAnyActionIdAtPointer(x, y)
 			)
 		) {
 			if ((currTime - pressedPointerTime < 1500) && (clickIdx == cursor)) {
 				//#debug debug
 				logger.debug("PointerDoublePressed");
 				keyPressed(10);
 				potentialDoubleClick = false;
 				return;
 			}
 		}
 		if (pointerDragged) {
 			 // Gestures: sliding horizontally with almost no vertical movement
 			if ( Math.abs(y - pointerYPressed) < fontSize ) {
 				int xDist = x - pointerXPressed; 
 				logger.debug("Slide right " + xDist);
 				// Sort mode Slide: Sliding right at least half the screen width is the same as the # key
 				if (xDist > getWidth() / 2 ) {
 					//#debug debug
 					logger.debug("Sort mode slide");
 					keyPressed(KEY_POUND);
 				// Route Slide: sliding right at least the fontHeight
 				} else if (xDist > fontSize ) {
 					logger.debug("Route slide");
 					cursor = clickIdxAtSlideStart;
 					repaint();
 					commandAction( ROUTE1_CMD, (Displayable) null);					
 				// Search field slide: sliding left at least the fontHeight
 				} else if (xDist < -getWidth()/2 ) {
 					logger.debug("Search field slide");
 					GuiNameEnter gne = new GuiNameEnter(this, null, Locale.get("guisearch.SearchForNamesStarting")/*Search for names starting with:*/, searchCanon.toString(), 20);
 					gne.show();
 				// Select entry slide: sliding left at least the fontHeight
 				} else if (xDist < -fontSize ) {
 					logger.debug("Select entry slide");
 					cursor = clickIdxAtSlideStart;					
 					repaint();
 				}
 			}
 			pointerDragged = false;
 			pointerDraggedMuch = false;
 			potentialDoubleClick = false;
 			return;
 		}
 		if (gsl != null) {
 		    gsl.clearTouchedElement();
 		}		
 		repaint();
 	}
 	
 	public void pointerDragged(int x, int y) {
 		//#debug debug
 		logger.debug("Pointer dragged: " + x + " " + y);
 		if ((Math.abs(x - pointerXPressed) < fontSize / 2) && (Math.abs(y - pointerYPressed) < fontSize / 2)) {
 			/**
 			 * On some devices, such as PhoneME, every pointerPressed event also causes
 			 * a pointerDragged event. We therefore need to filter out those pointerDragged
 			 * events that haven't actually moved the pointer.
 			 * 
 			 * Also for devices like Nokia 5800 we need some not too small threshold,
 			 * thus use half of the fontSize as threshold
 			 */
 			//#debug debug
 			logger.debug("No real dragging, as pointer hasn't moved");
 			return;
 
 		}
 		pointerDragged = true;
 		// check if there's been much movement, do this before the slide lock/unlock
 		// to avoid a single tap action when not sliding enough
 		if (Math.abs(x - pointerXPressed) > 8
 				|| 
 			Math.abs(y - pointerYPressed) > 8
 		) {
 			pointerDraggedMuch = true;
 			// avoid double tap triggering on fast consecutive drag actions starting at almost the same position
 			pressedPointerTime = 0; 
 		}
 		
 		// only scroll if drag wasn't started at the first entry to avoid scrolling it out accidently during slide gestures
 		if (pointerYPressed > fontSize) {		
 			scrollOffset += (y - pointerYDragged);
 			
 			if (scrollOffset > 0) {
 				scrollOffset = 0;
 			}
 			if (scrollOffset < -1*(result.size() - 2) *fontSize) {
 				scrollOffset = -1*(result.size() - 2) *fontSize;
 			}
 			pointerXDragged = x;
 			pointerYDragged = y;
 			repaint();
 		}
 	}
 
 	private void reSearch() {
 		//#debug info
 		logger.info("researching");
 		scrollOffset = 0;
 		//FIXME: in rare cases there occurs an NPE in the following line
 		searchThread.search(NumberCanon.canonial(searchCanon.toString()),
 				    Configuration.getCfgBitState(Configuration.CFGBIT_WORD_ISEARCH) ? 
 				    SearchNames.INDEX_WORD : SearchNames.INDEX_BIGNAME);
 		repaint(0, 0, getWidth(), getHeight());
 		// title will be set by SearchName.doSearch when we need to determine first if we have favorites
 		//#if polish.api.bigsearch
 		if (searchCanon.length() > 0 || matchMode()) { 
  			setTitle();
  		}
 		//#else
 		if (searchCanon.length() > 0) { 
 			setTitle();
 		}
 		//#endif
 	}
 
 	private void appendCompassDirection(StringBuffer sb, SearchResult sr) {
 		if (sr.dist >= 0) {
 			int courseToGo;
 			courseToGo = (int) (MoreMath.bearing_int(
 					parent.center.radlat,
 					parent.center.radlon,
 					sr.lat,
 					sr.lon
 			)  * MoreMath.FAC_RADTODEC);
 			courseToGo %= 360;
 			if (courseToGo < 0) {
 				courseToGo += 360;
 			}
 			sb.append("  (").append(HelperRoutines.formatDistance(sr.dist)).append(" ").append(Configuration.getCompassDirection(courseToGo)).append(")");
 		}
 	}
 	
 	public void addDistanceToSearchResult(SearchResult sr) {
 		sr.dist=ProjMath.getDistance(sr.lat, sr.lon, parent.center.radlat, parent.center.radlon);
 	}
 
 	public void setTitle() {
 		StringBuffer sb = new StringBuffer();
 		switch (state) {
 			case STATE_MAIN:
 				//#if polish.api.bigsearch
 				if (searchCanon.length() == 0 && !matchMode()) {
 					sb.append(Locale.get("guisearch.Searchforname")/*Search for name*/);
 				} else {
 					sb.append((words + searchCanon.toString() + " " + carret));
 				}
 				//#else
 				if (searchCanon.length() == 0) {
 					sb.append(Locale.get("guisearch.Searchforname")/*Search for name*/);
 				} else {
 					sb.append((searchCanon.toString() + " " + carret));
 				}
 				//#endif
 				if (searchCanon.length() > 0) {
 					sb.append(" (" + Locale.get("guisearch.key")/*key*/ + " " + searchCanon.toString() + ")");
 				} else {
 					if (sortByDist) {
 						sb.append(" (" + Locale.get("guisearch.distance")/*distance*/ + ")");
 					} else {
 						sb.append(" (" + Locale.get("guisearch.name")/*name*/ + ")");
 					}
 				}
 				break;
 			case STATE_FAVORITES:
 				if (showAllWayPts) {
 					sb.append(Locale.get("guisearch.Waypoints")/*Waypoints*/);
 				} else {
 					sb.append(Locale.get("guisearch.Favorites")/*Favorites*/);					
 				}
 				if (searchCanon.length() > 0) {
 					sb.append(" (key " + searchCanon.toString() + ")");
 				} else {
 					if (sortByDist) {
 						sb.append(Locale.get("guisearch.bydistance")/* by distance*/);
 					} else {
 						sb.append(Locale.get("guisearch.byname")/* by name*/);
 					}
 				}
 				break;
 			case STATE_POI:
 				sb.append(Locale.get("guisearch.nearestpois")/*Nearest POIs*/); break;			
 			case STATE_POI_URLS:
 				sb.append(Locale.get("guisearch.nearestpoiswithurls")/*Nearest POIs with URLs*/); break;
 			case STATE_POI_PHONES:
 				sb.append(Locale.get("guisearch.nearestpoiswithphones")/*Nearest POIs with phone #s*/); break;
 		        case STATE_FULLTEXT:
 				sb.append(Locale.get("guisearch.fulltextresults")/*Fulltext Results*/); break;			
 		}
 		setTitle(sb.toString());
 	}
 	
 	private String getName(int idx) {
 		String name = parent.getName(idx);
 		if (name == null) {			
 			needsPainting = true;
 		}
 		return name;
 	}
 	
 	// TODO: optimize sort-in algorithm, e.g. by bisectioning
 	/**
 	 * @param srNew
 	 * @return if SearchResult srNew was actually added (true = added, false = skipped)
 	 */
 	public synchronized boolean addResult(SearchResult srNew){		
 		//#if polish.api.bigsearch
 		if (matchMode() && state!= STATE_FAVORITES) {
 			Long id = new Long(srNew.resultid);
 			// if match is a way, try to get more exact coords from previous match
 			if (srNew.type > 0) {
 				// transfer house number coordinates to street
 				if (matchLats != null && matchLats.get(id) != null) {
 					srNew.lat = ((Float) matchLats.get(id)).floatValue();
 					srNew.lon = ((Float) matchLons.get(id)).floatValue();
 				}
 			}
 		}
                 //#endif
 		addDistanceToSearchResult(srNew);
 		String name = null;
 		//#if polish.api.bigsearch
 		if (state == STATE_FAVORITES && srNew.source == SearchNames.INDEX_WAYPOINTS) {
 			name = wayPts[srNew.nameIdx].displayName;
 		} else {
 			name = parent.getName(srNew.nameIdx);
 		}
 		//#else
 		if (state == STATE_FAVORITES) {
 			name = wayPts[srNew.nameIdx].displayName;
 		} else {
 			name = parent.getName(srNew.nameIdx);
 		}
 		//#endif
 		//#debug debug
 		logger.debug(Locale.get("guisearch.matchingnamefound")/*Found matching name: */ + srNew);
 
 		//System.out.println ("addResult: resultid = " + srNew.resultid + " source: " + srNew.source + " name: " + name);
 		// FIXME repeating code
 		// avoid index out of bounds 
 		int len = searchCanon.length();
 		//System.out.println ("name: " + name);
 		//System.out.println ("parent.getName: " + parent.getName(srNew.nameIdx));
 		if (name != null && name.length() < len) {
 			len = name.length();
 		}
 		if (Configuration.getCfgBitState(Configuration.CFGBIT_WORD_ISEARCH) ||
 		    matchMode() ||
 		    !searchAlpha || name == null ||
 		    searchCanon.toString().equalsIgnoreCase(name.substring(0, len))) {
 			if (!sortByDist) {
 				result2.addElement(srNew);
 			} else {
 				SearchResult sr = null;
 				int i = 0;
 				for (i=0; i<result2.size(); i++) {
 					sr = (SearchResult) result2.elementAt(i);
 					if (srNew.dist < sr.dist) {
 						break;
 					}
 				}
 				result2.insertElementAt(srNew, i);
 			}
 			needsPainting = true;
 			return true;
 		}
 		return false;
 	}
 
 	
 	// TODO: optimize sort-in algorithm, e.g. by bisectioning
 	public synchronized void  insertWptSearchResultSortedByNameOrDist(PositionMark[] wpts, SearchResult srNew) {
 		addDistanceToSearchResult(srNew);
 		SearchResult sr = null;
 		int i = 0;
 		for (i=0; i<result2.size(); i++) {
 			sr = (SearchResult) result2.elementAt(i);
 			if (
 				!sortByDist && wpts[srNew.nameIdx].displayName.compareTo(wpts[sr.nameIdx].displayName) < 0
 				||				
 				sortByDist && srNew.dist < sr.dist
 			) {
 				break;
 			}
 		}
 		result2.insertElementAt(srNew, i);
 	}
 	
 	public void triggerRepaint(){
 		repaint(0, 0, getWidth(), getHeight());
 	}
 
 	public synchronized void clearList() {
 		result.removeAllElements();
 		result2.removeAllElements();
 		scrollOffset = 0;
 	}
 
 	public void inputCompleted(String strResult) {
 		if (strResult != null) {		
 			searchCanon.setLength(0);
 			searchCanon.append(strResult);
 			carret = searchCanon.length();
 			if (carret > 1) {
 				state = STATE_MAIN;
 			}
 			reSearch();
 		}
 		show();
 	}
 
 	public void keySelectMenuCancel() {
 		state = STATE_MAIN;
 		show();
 	}
 
 	public void keySelectMenuItemSelected(final KeySelectMenuItem item) {
 		setTitle();
 		
 		clearList();
 		searchCanon.setLength(0);
 		searchAlpha = false;
 		final byte poiType = ((POItypeSelectMenuItem)item).getIdx();
 		final CancelMonitorInterface cmi = this;
 		isSearchCanceled = false;
 		Thread t = new Thread(new Runnable() {
 			public void run() {
 				try {
 					int maxScale = Legend.getNodeMaxScale(poiType);
 					// index 0 is all POI types
 					Vector res = parent.tiles[Legend.scaleToTile(maxScale)].getNearestPoi(
 						(((POItypeSelectMenuItem)item).getIdx() == 0) ? true : false, poiType, 
 						parent.center.radlat, parent.center.radlon, 10.0f*1000.0f, cmi);
 					for (int i = 0; i < res.size(); i++) {
 						SearchResult sr=(SearchResult) res.elementAt(i);
 						boolean match = false;
 						// don't filter
 						if (filter == 0) {
 							match = true;
 						}
 						// show urls
 						if ((filter & (1 << FILTER_BIT_URLS)) == (1 << FILTER_BIT_URLS) && sr.urlIdx != -1) {
 							match = true;
 						}
 						// show phones
 						if ((filter & (1 << FILTER_BIT_PHONES)) == (1 << FILTER_BIT_PHONES) && sr.phoneIdx != -1) {
 							match = true;
 						}
 						if (match) {
 							addResult(sr);
 						}
 					}
 					state = STATE_MAIN;
 					show();
 					synchronized(this) {
 						try {
 							//Wait for the Names to be resolved
 							//This is an arbitrary value, but hopefully
 							//a reasonable compromise.
 							wait(500);
 							repaint();
 						} catch (InterruptedException e) {
 							//Nothing to do
 						}
 					}
 				} catch (Exception e) {
 					logger.exception(Locale.get("guisearch.NearestPOISearchThreadCrashed")/*Nearest POI search thread crashed */, e);
 				} catch (OutOfMemoryError oome) {
 					logger.error(Locale.get("guisearch.NearestPOISearchOOM")/*Nearest POI search thread ran out of memory */);
 				}
 			}
 		}, "nearestPOI");
 		state = STATE_SEARCH_PROGRESS;
 		t.start();
 	}
 	public boolean monitorIsCanceled() {
 		return isSearchCanceled;
 	}
 }
