 /* -*- compile-command: "cd ../../../../../; ant install"; -*- */
 /*
  * Copyright 2009-2010 by Eric House (xwords@eehouse.org).  All
  * rights reserved.
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU General Public License as
  * published by the Free Software Foundation; either version 2 of the
  * License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful, but
  * WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  * General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
  */
 
 package org.eehouse.android.xw4;
 
 import android.view.View;
 import android.graphics.Canvas;
 import android.graphics.Paint;
 import android.graphics.Rect;
 import android.graphics.RectF;
 import android.graphics.Bitmap;
 import android.content.Context;
 import android.util.AttributeSet;
 import org.eehouse.android.xw4.jni.*;
 import android.view.MotionEvent;
 import android.graphics.drawable.Drawable;
 import android.content.res.Resources;
 import android.graphics.Paint.FontMetricsInt;
 import android.widget.ZoomButtonsController;
 import android.os.Handler;
 import java.util.HashMap;
 import java.nio.IntBuffer;
 
 import junit.framework.Assert;
 
 public class BoardView extends View implements DrawCtx, BoardHandler,
                                                SyncedDraw {
     private static final int k_miniTextSize = 24;
     private static final int k_miniPaddingH = 2;
     private static final int k_miniPaddingV = 2;
     private static final float MIN_FONT_DIPS = 14.0f;
 
     private Paint m_drawPaint;
     private Paint m_fillPaint;
     private Paint m_strokePaint;
     private int m_defaultFontHt;
     private int m_mediumFontHt;
     private Paint m_tileStrokePaint;
     private int m_jniGamePtr;
     private CurGameInfo m_gi;
     private int m_layoutWidth;
     private int m_layoutHeight;
     private Bitmap m_bitmap;    // the board
     private Canvas m_canvas;    // owns the bitmap
     private int m_trayOwner;
     private Rect m_valRect;
     private Rect m_letterRect;
     private Drawable m_rightArrow;
     private Drawable m_downArrow;
     private Drawable m_origin;
     private int m_left, m_top;
     private JNIThread m_jniThread;
     private String[][] m_scores;
     private String[] m_dictChars;
     private Rect m_boundsScratch;
     private String m_remText;
     private int m_dictPtr = 0;
     private int m_lastSecsLeft;
     private Handler m_viewHandler;
 
     // FontDims: exists to translate space available to the largest
     // font we can draw within that space taking advantage of our use
     // being limited to a known small subset of glyphs.  We need two
     // numbers from this: the textHeight to pass to Paint.setTextSize,
     // and the descent to use when drawing.  Both can be calculated
     // proportionally.  We know the ht we passed to Paint to get the
     // height we've now measured; that gives a percent to multiply any
     // future wantHt by.  Ditto for the descent
     private class FontDims {
         FontDims( float askedHt, int topRow, int bottomRow, float width ) {
             // Utils.logf( "FontDims(): askedHt=" + askedHt );
             // Utils.logf( "FontDims(): topRow=" + topRow );
             // Utils.logf( "FontDims(): bottomRow=" + bottomRow );
             // Utils.logf( "FontDims(): width=" + width );
             float gotHt = bottomRow - topRow + 1;
             m_htProportion = gotHt / askedHt;
             Assert.assertTrue( (bottomRow+1) >= askedHt );
             float descent = (bottomRow+1) - askedHt;
             Utils.logf( "descent: " + descent );
             m_descentProportion = descent / askedHt;
             Assert.assertTrue( m_descentProportion >= 0 );
             m_widthProportion = width / askedHt;
             // Utils.logf( "m_htProportion: " + m_htProportion );
             // Utils.logf( "m_descentProportion: " + m_descentProportion );
         }
         private float m_htProportion;
         private float m_descentProportion;
         private float m_widthProportion;
         int heightFor( int ht ) { return (int)(ht / m_htProportion); }
         int descentFor( int ht ) { return (int)(ht * m_descentProportion); }
         int widthFor( int width ) { return (int)(width / m_widthProportion); }
     }
     FontDims m_fontDims;
 
     private static final int BLACK = 0xFF000000;
     private static final int WHITE = 0xFFFFFFFF;
     private static final int GREY = 0xFF7F7F7F;
     private int[] m_bonusColors;
     private int[] m_playerColors;
     private int[] m_otherColors;
     private String[] m_bonusSummaries;
     private ZoomButtonsController m_zoomButtons;
     private boolean m_useZoomControl;
     private boolean m_canZoom;
 
     // called when inflating xml
     public BoardView( Context context, AttributeSet attrs ) 
     {
         super( context, attrs );
         init( context );
     }
 
     public boolean onTouchEvent( MotionEvent event ) 
     {
         int action = event.getAction();
         int xx = (int)event.getX() - m_left;
         int yy = (int)event.getY() - getCurTop();
         
         switch ( action ) {
         case MotionEvent.ACTION_DOWN:
             enableZoomControlsIf();
             m_jniThread.handle( JNIThread.JNICmd.CMD_PEN_DOWN, xx, yy );
             break;
         case MotionEvent.ACTION_MOVE:
             enableZoomControlsIf();
             m_jniThread.handle( JNIThread.JNICmd.CMD_PEN_MOVE, xx, yy );
             break;
         case MotionEvent.ACTION_UP:
             m_jniThread.handle( JNIThread.JNICmd.CMD_PEN_UP, xx, yy );
             break;
         default:
             Utils.logf( "unknown action: " + action );
             Utils.logf( event.toString() );
         }
 
         return true;             // required to get subsequent events
     }
 
     // This will be called from the UI thread
     @Override
     protected void onDraw( Canvas canvas ) 
     {
         synchronized( this ) {
             if ( layoutBoardOnce() ) {
                 canvas.drawBitmap( m_bitmap, m_left, getCurTop(), m_drawPaint );
             }
         }
     }
 
     @Override
     protected void onDetachedFromWindow() 
     {
         m_zoomButtons.setVisible( false );
         super.onDetachedFromWindow();
     }
 
     private void init( Context context )
     {
         final float scale = getResources().getDisplayMetrics().density;
         m_defaultFontHt = (int)(MIN_FONT_DIPS * scale + 0.5f);
         m_mediumFontHt = m_defaultFontHt * 3 / 2;
 
         m_drawPaint = new Paint();
         m_fillPaint = new Paint( Paint.ANTI_ALIAS_FLAG );
         m_strokePaint = new Paint();
         m_strokePaint.setStyle( Paint.Style.STROKE );
         m_tileStrokePaint = new Paint();
         m_tileStrokePaint.setStyle( Paint.Style.STROKE );
         Utils.logf( "stroke starts at " + m_tileStrokePaint.getStrokeWidth() );
         float curWidth = m_tileStrokePaint.getStrokeWidth();
         curWidth *= 2;
         if ( curWidth < 2 ) {
             curWidth = 2;
         }
         m_tileStrokePaint.setStrokeWidth( curWidth );
 
         Resources res = getResources();
         m_rightArrow = res.getDrawable( R.drawable.rightarrow );
         m_downArrow = res.getDrawable( R.drawable.downarrow );
         m_origin = res.getDrawable( R.drawable.origin );
 
         m_boundsScratch = new Rect();
 
         CommonPrefs prefs = CommonPrefs.get(context);
         m_playerColors = prefs.playerColors;
         m_bonusColors = prefs.bonusColors;
         m_otherColors = prefs.otherColors;
 
         m_bonusSummaries = new String[5];
         int[] ids = { R.string.bonus_l2x_summary,
                       R.string.bonus_w2x_summary ,
                       R.string.bonus_l3x_summary,
                       R.string.bonus_w3x_summary };
         for ( int ii = 0; ii < ids.length; ++ii ) {
             m_bonusSummaries[ ii+1 ] = getResources().getString( ids[ii] );
         }
 
         m_viewHandler = new Handler();
         m_zoomButtons = new ZoomButtonsController( this );
         ZoomButtonsController.OnZoomListener lstnr =
             new ZoomButtonsController.OnZoomListener(){
                 public void onVisibilityChanged( boolean visible ){}
                 public void onZoom( boolean zoomIn )
                 {
                     if ( null != m_jniThread ) {
                         int zoomBy = zoomIn ? 1 : -1;
                         m_jniThread.handle( JNIThread.JNICmd.CMD_ZOOM, zoomBy );
                     }
                 }
             };
         m_zoomButtons.setOnZoomListener( lstnr );
         m_zoomButtons.setZoomSpeed( 100 ); // milliseconds
     }
 
     protected void setUseZoomControl( boolean useZoomControl )
     {
         m_useZoomControl = useZoomControl;
         if ( !useZoomControl ) {
             m_zoomButtons.setVisible( false );
         }
     }
 
     private int getCurTop() 
     {
         return m_useZoomControl ? 0 : m_top;
     }
 
     private BoardDims figureBoardDims( int width, int height,
                                        CurGameInfo gi )
     {
         BoardDims result = new BoardDims();
         result.width = width;
         result.left = 0;
 
         int nCells = gi.boardSize;
         int cellSize = width / nCells;
         int maxCellSize = 3 * m_defaultFontHt;
         if ( cellSize > maxCellSize ) {
             cellSize = maxCellSize;
 
             int boardWidth = nCells * cellSize;
             result.left = (width - boardWidth) / 2;
             result.width = boardWidth;
         }
         result.maxCellSize = maxCellSize;
 
         result.trayHt = cellSize * 3;
         result.scoreHt = 2 * m_defaultFontHt;
         int wantHt = result.trayHt + result.scoreHt + (cellSize * nCells);
         int nToScroll = 0;
         if ( wantHt <= height ) {
             result.top = (height - wantHt) / 2;
         } else {
             int minTray = 3 * m_defaultFontHt;
             nToScroll = 
                 nCells - ((height - minTray - result.scoreHt) / cellSize);
             result.trayHt = 
                 height - result.scoreHt - (cellSize * (nCells-nToScroll));
             result.top = 0;
         }
 
         result.boardHt = cellSize * (nCells-nToScroll);
         result.trayTop = result.scoreHt + result.boardHt;
         result.height = result.scoreHt + result.boardHt + result.trayHt;
         result.cellSize = cellSize;
 
         if ( gi.timerEnabled ) {
             Paint paint = new Paint();
             paint.setTextSize( m_mediumFontHt );
             paint.getTextBounds( "-00:00", 0, 6, m_boundsScratch );
             result.timerWidth = m_boundsScratch.width();
         }
 
         return result;
     } // figureBoardDims
 
     private boolean layoutBoardOnce() 
     {
         final int width = getWidth();
         final int height = getHeight();
         boolean layoutDone = width == m_layoutWidth && height == m_layoutHeight;
         if ( layoutDone ) {
             // nothing to do
         } else if ( null == m_gi ) {
             // nothing to do either
         } else {
             m_layoutWidth = width;
             m_layoutHeight = height;
             m_fontDims = null; // force recalc of font
             m_letterRect = null;
             m_valRect = null;
 
             // We hide zoom on change in orientation
             m_zoomButtons.setVisible( false );
 
             BoardDims dims = figureBoardDims( width, height, m_gi );
             m_left = dims.left;
             m_top = dims.top;
             
             m_bitmap = Bitmap.createBitmap( 1 + dims.width,
                                             1 + dims.height,
                                             Bitmap.Config.ARGB_8888 );
             m_canvas = new Canvas( m_bitmap );
 
             // need to synchronize??
             m_jniThread.handle( JNIThread.JNICmd.CMD_LAYOUT, dims );
             m_jniThread.handle( JNIThread.JNICmd.CMD_DRAW );
             layoutDone = true;
         }
         return layoutDone;
     } // layoutBoardOnce
 
     private void enableZoomControlsIf()
     {
         if ( m_useZoomControl && m_canZoom ) {
             if ( m_layoutWidth <= m_layoutHeight ) {
                 m_zoomButtons.setVisible( true );
             }
         }
     }
 
     // BoardHandler interface implementation
     public void startHandling( JNIThread thread, int gamePtr, CurGameInfo gi ) 
     {
         m_jniThread = thread;
         m_jniGamePtr = gamePtr;
         m_gi = gi;
         m_layoutWidth = 0;
         m_layoutHeight = 0;
     }
 
     // SyncedDraw interface implementation
     public void doJNIDraw()
     {
         boolean drew;
         synchronized( this ) {
             drew = XwJNI.board_draw( m_jniGamePtr );
         }
         if ( !drew ) {
             Utils.logf( "draw not complete" );
         }
     }
 
     public void doIconDraw( int resID, final Rect rect )
     {
         synchronized( this ) {
             if ( null != m_canvas ) {
                 if ( 0 == resID ) {
                     clearToBack( rect );
                 } else {
                     Drawable icon = getResources().getDrawable( resID );
                     icon.setBounds( rect );
                     icon.draw( m_canvas );
                 }
             }
         }
     }
 
     public void zoomChanged( final boolean[] canZoom )
     {
         m_viewHandler.post( new Runnable() {
                 public void run() {
                     m_zoomButtons.setZoomInEnabled( canZoom[0] );
                     m_zoomButtons.setZoomOutEnabled( canZoom[1] );
                     m_canZoom = canZoom[0] || canZoom[1];
                 }
             } );
     }
 
     // DrawCtxt interface implementation
     public boolean scoreBegin( Rect rect, int numPlayers, int[] scores, 
                                int remCount, int dfs )
     {
         clearToBack( rect );
         m_canvas.save( Canvas.CLIP_SAVE_FLAG );
         m_canvas.clipRect(rect);
         m_scores = new String[numPlayers][];
         return true;
     }
 
     public void measureRemText( Rect r, int nTilesLeft, int[] width, 
                                 int[] height ) 
     {
         if ( nTilesLeft > 0 ) {
             // should cache a formatter
             m_remText = String.format( "%d", nTilesLeft );
             m_fillPaint.setTextSize( m_mediumFontHt );
             m_fillPaint.getTextBounds( m_remText, 0, m_remText.length(), 
                                        m_boundsScratch );
 
             int minWidth = m_boundsScratch.width();
             if ( minWidth < 20 ) {
                 minWidth = 20; // it's a button; make it bigger
             }
             width[0] = minWidth;
             height[0] = m_boundsScratch.height();
         } else {
             width[0] = height[0] = 0;
         }
     }
 
     public void drawRemText( Rect rInner, Rect rOuter, int nTilesLeft, 
                              boolean focussed )
     {
         int indx = focussed ? CommonPrefs.COLOR_FOCUS
             : CommonPrefs.COLOR_TILE_BACK;
         fillRect( rOuter, m_otherColors[indx] );
 
         m_fillPaint.setColor( BLACK );
         drawCentered( m_remText, rInner, null );
     }
 
     public void measureScoreText( Rect r, DrawScoreInfo dsi, 
                                   int[] width, int[] height )
     {
         String[] scoreInfo = new String[dsi.isTurn?1:2];
         int indx = 0;
         StringBuffer sb = new StringBuffer();
 
         // If it's my turn I get one line.  Otherwise squeeze into
         // two.
 
         if ( dsi.isTurn ) {
             sb.append( dsi.name );
             sb.append( ":" );
         } else {
             scoreInfo[indx++] = dsi.name;
         }
         sb.append( dsi.totalScore );
         if ( dsi.nTilesLeft >= 0 ) {
             sb.append( ":" );
             sb.append( dsi.nTilesLeft );
         }
         scoreInfo[indx] = sb.toString();
         m_scores[dsi.playerNum] = scoreInfo;
 
         m_fillPaint.setTextSize( dsi.isTurn? r.height() : m_defaultFontHt );
 
         int needWidth = 0;
         for ( int ii = 0; ii < scoreInfo.length; ++ii ) {
             m_fillPaint.getTextBounds( scoreInfo[ii], 0, scoreInfo[ii].length(), 
                                        m_boundsScratch );
             if ( needWidth < m_boundsScratch.width() ) {
                 needWidth = m_boundsScratch.width();
             }
         }
         if ( needWidth > r.width() ) {
             needWidth = r.width();
         }
         width[0] = needWidth;
 
         height[0] = r.height();
     }
 
     public void score_drawPlayer( Rect rInner, Rect rOuter, DrawScoreInfo dsi )
     {
         if ( 0 != (dsi.flags & CELL_ISCURSOR) ) {
             fillRect( rOuter, m_otherColors[CommonPrefs.COLOR_FOCUS] );
         }
         String[] texts = m_scores[dsi.playerNum];
         m_fillPaint.setColor( m_playerColors[dsi.playerNum] );
 
         Rect rect = new Rect( rOuter );
         int height = rect.height() / texts.length;
         rect.bottom = rect.top + height;
         for ( String text : texts ) {
             drawCentered( text, rect, null );
             rect.offset( 0, height );
         }
     }
 
     public void drawTimer( Rect rect, int player, int secondsLeft )
     {
         if ( null != m_canvas && m_lastSecsLeft != secondsLeft ) {
             m_lastSecsLeft = secondsLeft;
 
             String negSign = secondsLeft < 0? "-":"";
             secondsLeft = Math.abs( secondsLeft );
             String time = String.format( "%s%d:%02d", negSign, secondsLeft/60, 
                                          secondsLeft%60 );
 
             clearToBack( rect );
             m_fillPaint.setColor( m_playerColors[player] );
 
             Rect shorter = new Rect( rect );
             shorter.inset( 0, shorter.height() / 5 );
             drawCentered( time, shorter, null );
 
             m_jniThread.handle( JNIThread.JNICmd.CMD_DRAW );
         }
     }
 
     public boolean drawCell( Rect rect, String text, int tile, int owner, 
                              int bonus, int hintAtts, int flags ) 
     {
         int backColor;
         boolean empty = 0 != (flags & (CELL_DRAGSRC|CELL_ISEMPTY));
         boolean pending = 0 != (flags & CELL_HIGHLIGHT);
         String bonusStr = null;
 
         figureFontDims();
 
         if ( owner < 0 ) {
             owner = 0;
         }
         int foreColor = m_playerColors[owner];
 
         if ( 0 != (flags & CELL_ISCURSOR) ) {
             backColor = m_otherColors[CommonPrefs.COLOR_FOCUS];
         } else if ( empty ) {
             if ( 0 == bonus ) {
                 backColor = m_otherColors[CommonPrefs.COLOR_BKGND];
             } else {
                 backColor = m_bonusColors[bonus];
                 bonusStr = m_bonusSummaries[bonus];
             }
         } else if ( pending ) {
             backColor = BLACK;
             foreColor = WHITE;
         } else {
             backColor = m_otherColors[CommonPrefs.COLOR_TILE_BACK];
         }
 
         fillRect( rect, backColor );
 
         if ( empty ) {
             if ( (CELL_ISSTAR & flags) != 0 ) {
                 m_origin.setBounds( rect );
                 m_origin.draw( m_canvas );
             } else if ( null != bonusStr ) {
                 m_fillPaint.setColor( GREY );
                 drawCentered( bonusStr, rect, m_fontDims );
             }
         } else {
             m_fillPaint.setColor( foreColor );
             drawCentered( text, rect, m_fontDims );
         }
 
         if ( (CELL_ISBLANK & flags) != 0 ) {
             markBlank( rect, pending );
         }
         // frame the cell
         m_canvas.drawRect( rect, m_strokePaint );
         
         return true;
     } // drawCell
 
     public void drawBoardArrow ( Rect rect, int bonus, boolean vert, 
                                  int hintAtts, int flags )
     {
         rect.inset( 2, 2 );
         Drawable arrow = vert? m_downArrow : m_rightArrow;
         arrow.setBounds( rect );
         arrow.draw( m_canvas );
     }
 
     public boolean trayBegin ( Rect rect, int owner, int dfs ) 
     {
         m_trayOwner = owner;
         return true;
     }
 
     public void drawTile( Rect rect, String text, int val, int flags ) 
     {
         drawTileImpl( rect, text, val, flags, true );
     }
 
     public void drawTileMidDrag( Rect rect, String text, int val, int owner, 
                                  int flags ) 
     {
         drawTileImpl( rect, text, val, flags, false );
     }
 
     public void drawTileBack( Rect rect, int flags ) 
     {
         drawTileImpl( rect, "?", -1, flags, true );
     }
 
     public void drawTrayDivider( Rect rect, int flags ) 
     {
         boolean isCursor = 0 != (flags & CELL_ISCURSOR);
         boolean selected = 0 != (flags & CELL_HIGHLIGHT);
 
         int backColor = isCursor? m_otherColors[CommonPrefs.COLOR_FOCUS]:WHITE;
         rect.inset( 0, 1 );
         fillRect( rect, backColor );
 
         rect.inset( rect.width()/4, 0 );
         if ( selected ) {
             m_canvas.drawRect( rect, m_strokePaint );
         } else {
             fillRect( rect, BLACK );
         }
     }
 
     public void score_pendingScore( Rect rect, int score, int playerNum, 
                                     int flags ) 
     {
         String text = score >= 0? String.format( "%d", score ) : "??";
         ++rect.top;
         fillRect( rect, (0 == (flags & CELL_ISCURSOR)) 
                   ? WHITE : m_otherColors[CommonPrefs.COLOR_FOCUS] );
         m_fillPaint.setColor( m_playerColors[playerNum] );
         rect.inset( 0, rect.height() / 4 );
         drawCentered( text, rect, null );
     }
 
     public String getMiniWText ( int textHint )
     {
         int id = 0;
         switch( textHint ) {
         case BONUS_DOUBLE_LETTER:
             id = R.string.bonus_l2x;
             break;
         case BONUS_DOUBLE_WORD:
             id = R.string.bonus_w2x;
             break;
         case BONUS_TRIPLE_LETTER:
             id = R.string.bonus_l3x;
             break;
         case BONUS_TRIPLE_WORD:
             id = R.string.bonus_w3x;
             break;
         case INTRADE_MW_TEXT:
             id = R.string.trading_text;
             break;
         default:
             Assert.fail();
         }
         return getResources().getString( id );
     }
 
     public void measureMiniWText( String str, int[] width, int[] height )
     {
         m_fillPaint.setTextSize( k_miniTextSize );
         FontMetricsInt fmi = m_fillPaint.getFontMetricsInt();
         int lineHeight = -fmi.top + fmi.leading;
         
         String[] lines = str.split("\n");
         height[0] = (lines.length * lineHeight) + (2 * k_miniPaddingV);
 
         int maxWidth = 0;
         for ( String line : lines ) {
             m_fillPaint.getTextBounds( line, 0, line.length(), m_boundsScratch );
             int thisWidth = m_boundsScratch.width();
             if ( maxWidth < thisWidth ) {
                 maxWidth = thisWidth;
             }
         }
         width[0] = maxWidth + (k_miniPaddingH * 2);
     }
 
     public void drawMiniWindow( String text, Rect rect )
     {
         clearToBack( rect );
 
         m_fillPaint.setTextSize( k_miniTextSize );
         m_fillPaint.setTextAlign( Paint.Align.CENTER );
         m_fillPaint.setColor( BLACK );
 
         String[] lines = text.split("\n");
         int lineHt = rect.height() / lines.length;
         int bottom = rect.top + lineHt
             - m_fillPaint.getFontMetricsInt().descent;
         int center = rect.left + (rect.width() / 2);
 
         for ( String line : lines ) {
             m_canvas.drawText( line, center, bottom, m_fillPaint );
             bottom += lineHt;
         }
 
         m_canvas.drawRect( rect, m_strokePaint );
        m_jniThread.handle( JNIThread.JNICmd.CMD_DRAW );
     }
 
     public void objFinished( /*BoardObjectType*/int typ, Rect rect, int dfs )
     {
         if ( DrawCtx.OBJ_SCORE == typ ) {
             m_canvas.restoreToCount(1); // in case new canvas...
         }
     }
 
     public void dictChanged( int dictPtr )
     {
         if ( m_dictPtr != dictPtr ) {
             if ( m_dictPtr == 0 || 
                  !XwJNI.dict_tilesAreSame( m_dictPtr, dictPtr ) ) {
                 m_fontDims = null;
                 m_dictChars = XwJNI.dict_getChars( dictPtr );
             }
             m_dictPtr = dictPtr;
         }
     }
 
     private void drawTileImpl( Rect rect, String text, int val, 
                                int flags, boolean clearBack )
     {
         // boolean valHidden = (flags & CELL_VALHIDDEN) != 0;
         boolean notEmpty = (flags & CELL_ISEMPTY) == 0;
         boolean isCursor = (flags & CELL_ISCURSOR) != 0;
 
         m_canvas.save( Canvas.CLIP_SAVE_FLAG );
         rect.top += 1;
         m_canvas.clipRect( rect );
 
         if ( clearBack ) {
             clearToBack( rect );
         }
 
         if ( isCursor || notEmpty ) {
 
             if ( clearBack ) {
                 int indx = isCursor? CommonPrefs.COLOR_FOCUS 
                     : CommonPrefs.COLOR_TILE_BACK;
                 fillRect( rect, m_otherColors[indx] );
             }
 
             m_fillPaint.setColor( m_playerColors[m_trayOwner] );
 
             if ( notEmpty ) {
                 positionDrawTile( rect, text, val );
 
                 m_canvas.drawRect( rect, m_tileStrokePaint); // frame
                 if ( 0 != (flags & CELL_HIGHLIGHT) ) {
                     rect.inset( 2, 2 );
                     m_canvas.drawRect( rect, m_tileStrokePaint ); // frame
                 }
             }
         }
         m_canvas.restoreToCount(1); // in case new canvas....
     } // drawTileImpl
 
     private void drawCentered( String text, Rect rect, FontDims fontDims ) 
     {
         int descent = -1;
         int textSize;
         if ( null == fontDims ) {
             textSize = rect.height() - 2;
         } else {
             int height = rect.height() - 4; // borders and padding, 2 each 
             descent = fontDims.descentFor( height );
             textSize = fontDims.heightFor( height );
             // Utils.logf( "using descent: " + descent + " and textSize: " 
             //             + textSize + " in height " + height );
         }
         m_fillPaint.setTextSize( textSize );
         if ( descent == -1 ) {
             descent = m_fillPaint.getFontMetricsInt().descent;
         }
         descent += 2;
 
         m_fillPaint.getTextBounds( text, 0, text.length(), m_boundsScratch );
         if ( m_boundsScratch.width() > rect.width() ) {
             m_fillPaint.setTextAlign( Paint.Align.LEFT );
             drawScaled( text, rect, descent );
         } else {
             int bottom = rect.bottom - descent;
             int center = rect.left + ( rect.width() / 2 );
             m_fillPaint.setTextAlign( Paint.Align.CENTER );
             m_canvas.drawText( text, center, bottom, m_fillPaint );
         }
     }
 
     private void drawScaled( String text, final Rect rect, int descent )
     {
         Rect local = new Rect();
         m_fillPaint.getTextBounds( text, 0, text.length(), local );
         local.bottom = rect.height();
 
         Bitmap bitmap = Bitmap.createBitmap( local.width(),
                                              rect.height(), 
                                              Bitmap.Config.ARGB_8888 );
 
         Canvas canvas = new Canvas( bitmap );
         int bottom = local.bottom - descent;
         canvas.drawText( text, 0, bottom, m_fillPaint );
 
         m_canvas.drawBitmap( bitmap, local, rect, m_drawPaint );
     }
 
     private void positionDrawTile( final Rect rect, String text, int val )
     {
         figureFontDims();
 
         if ( null != text ) {
             if ( null == m_letterRect ) {
                 m_letterRect = new Rect( 0, 0, rect.width() * 3 / 4, 
                                          rect.height() * 3 / 4 );
             }
             m_letterRect.offsetTo( rect.left+2, rect.top+2 );
             drawCentered( text, m_letterRect, m_fontDims );
         }
 
         if ( val >= 0 ) {
             if ( null == m_valRect ) {
                 m_valRect = new Rect( 0, 0, rect.width() / 4, rect.height() / 4 );
                 m_valRect.inset( 2, 2 );
             }
             m_valRect.offsetTo( rect.right - (rect.width() / 4),
                                 rect.bottom - (rect.height() / 4) );
             text = String.format( "%d", val );
             m_fillPaint.setTextSize( m_valRect.height() );
             m_fillPaint.setTextAlign( Paint.Align.RIGHT );
             m_canvas.drawText( text, m_valRect.right, m_valRect.bottom, 
                                m_fillPaint );
         }
     }
 
     private void fillRect( Rect rect, int color )
     {
         m_fillPaint.setColor( color );
         m_canvas.drawRect( rect, m_fillPaint );
     }
 
     private void clearToBack( Rect rect ) 
     {
         fillRect( rect, m_otherColors[CommonPrefs.COLOR_BKGND] );
     }
 
     private void figureFontDims()
     {
         if ( null == m_fontDims ) {
             final int ht = 24;
             final int width = 20;
 
             Paint paint = new Paint(); // CommonPrefs.getFontFlags()??
             paint.setStyle( Paint.Style.STROKE );
             paint.setTextAlign( Paint.Align.LEFT );
             paint.setTextSize( ht );
 
             Bitmap bitmap = Bitmap.createBitmap( width, (ht*3)/2, 
                                                  Bitmap.Config.ARGB_8888 );
             Canvas canvas = new Canvas( bitmap );
 
             // FontMetrics fmi = paint.getFontMetrics();
             // Utils.logf( "ascent: " + fmi.ascent );
             // Utils.logf( "bottom: " + fmi.bottom );
             // Utils.logf( "descent: " + fmi.descent );
             // Utils.logf( "leading: " + fmi.leading );
             // Utils.logf( "top : " + fmi.top );
 
             // Utils.logf( "using as baseline: " + ht );
 
             Rect bounds = new Rect();
             int maxWidth = 0;
             for ( String str : m_dictChars ) {
                 if ( str.length() == 1 && str.charAt(0) >= 32 ) {
                     canvas.drawText( str, 0, ht, paint );
                     paint.getTextBounds( str, 0, 1, bounds );
                     if ( maxWidth < bounds.right ) {
                         maxWidth = bounds.right;
                     }
                 }
             }
 
             // for ( int row = 0; row < bitmap.getHeight(); ++row ) {
             //     StringBuffer sb = new StringBuffer( bitmap.getWidth() );
             //     for ( int col = 0; col < bitmap.getWidth(); ++col ) {
             //         int pixel = bitmap.getPixel( col, row );
             //         sb.append( pixel==0? "." : "X" );
             //     }
             //     Utils.logf( sb.append(row).toString() );
             // }
 
             int topRow = 0;
             findTop:
             for ( int row = 0; row < bitmap.getHeight(); ++row ) {
                 for ( int col = 0; col < bitmap.getWidth(); ++col ) {
                     if ( 0 != bitmap.getPixel( col, row ) ){
                         topRow = row;
                         break findTop;
                     }
                 }
             }
 
             int bottomRow = 0;
             findBottom:
             for ( int row = bitmap.getHeight() - 1; row > topRow; --row ) {
                 for ( int col = 0; col < bitmap.getWidth(); ++col ) {
                     if ( 0 != bitmap.getPixel( col, row ) ){
                         bottomRow = row;
                         break findBottom;
                     }
                 }
             }
         
             m_fontDims = new FontDims( ht, topRow, bottomRow, maxWidth );
         }
     } // figureFontDims
 
     private void markBlank( final Rect rect, boolean whiteOnBlack )
     {
         RectF oval = new RectF( rect.left, rect.top, rect.right, rect.bottom );
         int curColor = 0;
         if ( whiteOnBlack ) {
             curColor = m_strokePaint.getColor();
             m_strokePaint.setColor( WHITE );
         }
         m_canvas.drawArc( oval, 0, 360, false, m_strokePaint );
         if ( whiteOnBlack ) {
             m_strokePaint.setColor( curColor );
         }
     }
 }
