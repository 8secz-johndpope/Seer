 /*
   Copyright 2008 Google Inc.
   
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
   
        http://www.apache.org/licenses/LICENSE-2.0
   
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */ 
 package com.kmagic.solitaire;
 
 import android.content.Context;
 import android.content.SharedPreferences;
 import android.content.res.Resources;
 import android.graphics.Canvas;
 import android.graphics.PointF;
 import android.os.Bundle;
 import android.os.SystemClock;
 import android.util.AttributeSet;
 import android.util.Log;
 import android.view.KeyEvent;
 import android.view.View;
 import android.view.MotionEvent;
 import android.view.Gravity;
 import android.widget.TextView;
 
 import java.lang.Math;
 import java.lang.Runnable;
 import java.util.Stack;
 
 // The brains of the operation
 public class SolitaireView extends View {
 
   private static final int MODE_NORMAL      = 1;
   private static final int MODE_MOVE_CARD   = 2;
   private static final int MODE_CARD_SELECT = 3;
   private static final int MODE_TEXT        = 4;
   private static final int MODE_ANIMATE     = 5;
   private static final int MODE_WIN         = 6;
   private static final int MODE_WIN_STOP    = 7;
 
   private CharSequence mHelpText;
   private CharSequence mWinText;
 
   private CardAnchor[] mCardAnchor;
   private DrawMaster mDrawMaster;
   private Rules mRules;
   private TextView mTextView;
   private AnimateCard mAnimateCard;
 
   private MoveCard mMoveCard;
   private SelectCard mSelectCard;
   private int mViewMode;
   private boolean mTextViewDown;
 
   private PointF mLastPoint;
   private PointF mDownPoint;
   private RefreshHandler mRefreshHandler;
   private Thread mRefreshThread;
   private Stack<Move> mMoveHistory;
   private Replay mReplay;
   private Context mContext;
   private boolean mHasMoved;
   private Speed mSpeed;
 
   private Card[] mUndoStorage;
 
   private int mElapsed;
   private long mStartTime;
   private boolean mTimePaused;
 
   private boolean mGameStarted;
 
   public SolitaireView(Context context, AttributeSet attrs) {
     super(context, attrs);
     setFocusable(true);
     setFocusableInTouchMode(true);
 
     mDrawMaster = new DrawMaster(context);
     mMoveCard = new MoveCard();
     mSelectCard = new SelectCard();
     mViewMode = MODE_NORMAL;
     mLastPoint = new PointF();
     mDownPoint = new PointF();
     mRefreshHandler = new RefreshHandler(this);
     mRefreshThread = new Thread(mRefreshHandler);
     mMoveHistory = new Stack<Move>();
     mUndoStorage = new Card[CardAnchor.MAX_CARDS];
     mAnimateCard = new AnimateCard(this);
     mSpeed = new Speed();
     mReplay = new Replay(this, mAnimateCard);
 
     Resources res = context.getResources();
     mHelpText = context.getResources().getText(R.string.help_text);
     mWinText = context.getResources().getText(R.string.win_text);
     mContext = context;
     mTextViewDown = false;
     mRefreshThread.start();
   }
 
   public void InitGame(int gameType) {
     int oldScore = 0;
     String oldGameType = "None";
 
     // We really really want focus :)
     setFocusable(true);
     setFocusableInTouchMode(true);
     requestFocus();
 
     SharedPreferences.Editor editor = GetSettings().edit();
     if (mRules != null) {
       if (mRules.HasScore()) {
         oldScore = mRules.GetScore();
         oldGameType = mRules.GetGameTypeString();
       }
       if (mRules.HasScore()) {
         if (mRules.GetScore() > GetSettings().getInt(mRules.GetGameTypeString() + "Score", -52)) {
           editor.putInt(mRules.GetGameTypeString() + "Score", mRules.GetScore());
         }
       }
     }
     ChangeViewMode(MODE_NORMAL);
     mTextView.setVisibility(View.INVISIBLE);
     mMoveHistory.clear();
     mAnimateCard.SetAnimate(true);
     mRules = Rules.CreateRules(gameType, null, this, mMoveHistory, mAnimateCard);
     if (oldGameType == mRules.GetGameTypeString()) {
       mRules.SetCarryOverScore(oldScore);
     }
     Card.SetSize(gameType);
     mDrawMaster.DrawCards(GetSettings().getBoolean("DisplayBigCards", false));
     mCardAnchor = mRules.GetAnchorArray();
     if (mDrawMaster.GetWidth() > 1) {
       mRules.Resize(mDrawMaster.GetWidth(), mDrawMaster.GetHeight());
       Refresh();
     }
     editor.putInt("LastType", gameType);
     editor.commit();
     mStartTime = SystemClock.uptimeMillis();
     mElapsed = 0;
     mTimePaused = false;
     mGameStarted = false;
   }
 
   public SharedPreferences GetSettings() { return ((Solitaire)mContext).GetSettings(); }
   public DrawMaster GetDrawMaster() { return mDrawMaster; }
   public Rules GetRules() { return mRules; }
   public void ClearGameStarted() { mGameStarted = false; }
 
   public void SetTimePassing(boolean timePassing) {
     if (timePassing == true && (mViewMode == MODE_WIN || mViewMode == MODE_WIN_STOP)) {
       return;
     }
     if (timePassing == true && mTimePaused == true) {
       mStartTime = SystemClock.uptimeMillis() - mElapsed;
       mTimePaused = false;
     } else if (timePassing == false) {
       mTimePaused = true;
     }
   }
 
   public void UpdateTime() {
     if (!mTimePaused) {
       int elapsed = (int)(SystemClock.uptimeMillis() - mStartTime);
       if (elapsed / 1000 > mElapsed / 1000) {
         Refresh();
       }
       mElapsed = elapsed;
     }
   }
 
   private void ChangeViewMode(int newMode) {
     switch (mViewMode) {
       case MODE_NORMAL:
         if (newMode != MODE_NORMAL) {
           DrawBoard();
         }
         break;
       case MODE_MOVE_CARD:
         mMoveCard.Release();
         DrawBoard();
         break;
       case MODE_CARD_SELECT:
         mSelectCard.Release();
         DrawBoard();
         break;
       case MODE_TEXT:
         mTextView.setVisibility(View.INVISIBLE);
         break;
       case MODE_ANIMATE:
         mRefreshHandler.SetRefresh(RefreshHandler.SINGLE_REFRESH);
         break;
       case MODE_WIN:
       case MODE_WIN_STOP:
         if (newMode != MODE_WIN_STOP) {
           mTextView.setVisibility(View.INVISIBLE);
         }
         DrawBoard();
         mReplay.StopPlaying();
         break;
     }
     mViewMode = newMode;
     switch (newMode) {
       case MODE_WIN:
         SetTimePassing(false);
       case MODE_MOVE_CARD:
       case MODE_CARD_SELECT:
       case MODE_ANIMATE:
         mRefreshHandler.SetRefresh(RefreshHandler.LOCK_REFRESH);
         break;
 
       case MODE_NORMAL:
       case MODE_TEXT:
       case MODE_WIN_STOP:
         mRefreshHandler.SetRefresh(RefreshHandler.SINGLE_REFRESH);
         break;
     }
   }
 
   public void ToSteadyState() {
     ChangeViewMode(MODE_NORMAL);
   }
 
   public void onPause() {
     mAnimateCard.SetAnimate(false);
     ToSteadyState();
     mRefreshHandler.SetRunning(false);
     try {
       mRefreshThread.join(100);
     } catch (InterruptedException e) {
     }
 
     if (mRules != null && mRules.GetScore() > GetSettings().getInt(mRules.GetGameTypeString() + "Score", -52)) {
       SharedPreferences.Editor editor = GetSettings().edit();
       editor.putInt(mRules.GetGameTypeString() + "Score", mRules.GetScore());
       editor.commit();
     }
   }
 
   public void onResume() {
     mStartTime = SystemClock.uptimeMillis() - mElapsed;
     mAnimateCard.SetAnimate(true);
     mRefreshHandler.SetRunning(true);
     mRefreshThread = new Thread(mRefreshHandler);
     mRefreshThread.start();
   }
 
   public void Refresh() {
     mRefreshHandler.SingleRefresh();
   }
 
   public void SetTextView(TextView textView) {
     mTextView = textView;
   }
 
   protected void onSizeChanged(int w, int h, int oldw, int oldh) {
     mDrawMaster.SetScreenSize(w, h);
     mRules.Resize(w, h);
     mSelectCard.SetHeight(h);
   }
 
   public void DisplayHelp() {
     mTextView.setTextSize(15);
     mTextView.setGravity(Gravity.LEFT);
     DisplayText(mHelpText);
   }
 
   public void DisplayWin() {
     MarkWin();
     mTextView.setTextSize(24);
     mTextView.setGravity(Gravity.CENTER_HORIZONTAL);
     DisplayText(mWinText);
     ChangeViewMode(MODE_WIN);
     mTextView.setVisibility(View.VISIBLE);
     mRules.SetIgnoreEvents(true);
     mReplay.StartReplay(mMoveHistory, mCardAnchor);
   }
 
   public void RestartGame() {
     mRules.SetIgnoreEvents(true);
     while (!mMoveHistory.empty()) {
       Undo();
     }
     mRules.SetIgnoreEvents(false);
     Refresh();
   }
 
   public void DisplayText(CharSequence text) {
     ChangeViewMode(MODE_TEXT);
     mTextView.setVisibility(View.VISIBLE);
     mTextView.setText(text);
     Refresh();
   }
 
   public void DrawBoard() {
     Canvas boardCanvas = mDrawMaster.GetBoardCanvas();
     mDrawMaster.DrawBackground(boardCanvas);
     for (int i = 0; i < mCardAnchor.length; i++) {
       mCardAnchor[i].Draw(mDrawMaster, boardCanvas);
     }
   }
 
   @Override
   public void onDraw(Canvas canvas) {
 
     // Only draw the stagnant stuff if it may have changed
     if (mViewMode == MODE_NORMAL) {
       // SanityCheck is for debug use only.
       //SanityCheck();
       DrawBoard();
     }
     mDrawMaster.DrawLastBoard(canvas);
     mDrawMaster.DrawTime(canvas, mElapsed);
     if (mRules.HasScore()) {
       mDrawMaster.DrawScore(canvas, mRules.GetScoreString());
     }
 
     switch (mViewMode) {
       case MODE_MOVE_CARD:
         mMoveCard.Draw(mDrawMaster, canvas);
         break;
       case MODE_CARD_SELECT:
         mSelectCard.Draw(mDrawMaster, canvas);
         break;
       case MODE_WIN:
         if (mReplay.IsPlaying()) {
           mAnimateCard.Draw(mDrawMaster, canvas);
         }
       case MODE_WIN_STOP:
       case MODE_TEXT:
         mDrawMaster.DrawShade(canvas);
         break;
       case MODE_ANIMATE:
         mAnimateCard.Draw(mDrawMaster, canvas);
     }
   }
 
   @Override
   public boolean onKeyDown(int keyCode, KeyEvent msg) {
     switch (keyCode) {
     case KeyEvent.KEYCODE_DPAD_CENTER:
       if (mViewMode == MODE_TEXT) {
         ChangeViewMode(MODE_NORMAL);
       } else if (mViewMode == MODE_NORMAL) {
         mRules.EventAlert(Rules.EVENT_DEAL, mCardAnchor[0]);
         Refresh();
       }
       return true;
       case KeyEvent.KEYCODE_BACK:
         Undo();
         return true;
       }
     return super.onKeyDown(keyCode, msg);
   }
 
   @Override
   public boolean onTouchEvent(MotionEvent event) {
     boolean ret = false;
 
     // Text mode only handles clickys
     if (mViewMode == MODE_TEXT) {
       if (event.getAction() == MotionEvent.ACTION_UP && mTextViewDown) {
         SharedPreferences.Editor editor = mContext.getSharedPreferences("SolitairePreferences", 0).edit();
         editor.putBoolean("PlayedBefore", true);
         editor.commit();
         mTextViewDown = false;
         ChangeViewMode(MODE_NORMAL);
       } if (event.getAction() == MotionEvent.ACTION_DOWN) {
         mTextViewDown = true;
       }
       return true;
     }
 
     switch (event.getAction()) {
       case MotionEvent.ACTION_DOWN:
         mHasMoved = false;
         mSpeed.Reset();
         ret = onDown(event.getX(), event.getY());
         mDownPoint.set(event.getX(), event.getY());
         break;
       case MotionEvent.ACTION_UP:
       case MotionEvent.ACTION_CANCEL:
         ret = onRelease(event.getX(), event.getY());
         break;
       case MotionEvent.ACTION_MOVE:
         if (!mHasMoved) {
           CheckMoved(event.getX(), event.getY());
         }
         ret = onMove(mLastPoint.x - event.getX(), mLastPoint.y - event.getY(),
                      event.getX(), event.getY());
         break;
     }
     mLastPoint.set(event.getX(), event.getY());
 
     if (!mGameStarted && !mMoveHistory.empty()) {
       mGameStarted = true;
       MarkAttempt();
     }
 
     return ret;
   }
 
   private boolean onRelease(float x, float y) {
     switch (mViewMode) {
       case MODE_NORMAL:
         if (!mHasMoved) {
           for (int i = 0; i < mCardAnchor.length; i++) {
             if (mCardAnchor[i].ExpandStack(x, y)) {
               mSelectCard.InitFromAnchor(mCardAnchor[i]);
               ChangeViewMode(MODE_CARD_SELECT);
               return true;
             } else if (mCardAnchor[i].TapCard(x, y)) {
               Refresh();
               return true;
             }
           }
         }
         break;
       case MODE_MOVE_CARD:
         for (int close = 0; close < 2; close++) {
           CardAnchor prevAnchor = mMoveCard.GetAnchor();
           boolean unhide = (prevAnchor.GetVisibleCount() == 0 &&
                             prevAnchor.GetCount() > 0);
           int count = mMoveCard.GetCount();
 
           for (int i = 0; i < mCardAnchor.length; i++) {
             if (mCardAnchor[i] != prevAnchor) {
               if (mCardAnchor[i].CanDropCard(mMoveCard, close)) {
                 mMoveHistory.push(new Move(prevAnchor.GetNumber(), i, count, false, unhide));
                 mCardAnchor[i].AddMoveCard(mMoveCard);
                 if (mViewMode == MODE_MOVE_CARD) {
                   ChangeViewMode(MODE_NORMAL);
                 }
                 return true;
               }
             }
           }
         }
         if (!mMoveCard.HasMoved()) {
           CardAnchor anchor = mMoveCard.GetAnchor();
           mMoveCard.Release();
           if (anchor.ExpandStack(x, y)) {
             mSelectCard.InitFromAnchor(anchor);
             ChangeViewMode(MODE_CARD_SELECT);
           } else {
             ChangeViewMode(MODE_NORMAL);
           }
         } else if (mSpeed.IsFast() && mMoveCard.GetCount() == 1) {
           if (!mRules.Fling(mMoveCard)) {
             ChangeViewMode(MODE_NORMAL);
           }
         } else {
           mMoveCard.Release();
           ChangeViewMode(MODE_NORMAL);
         }
         return true;
       case MODE_CARD_SELECT:
         if (!mSelectCard.IsOnCard() && !mHasMoved) {
           mSelectCard.Release();
           ChangeViewMode(MODE_NORMAL);
           return true;
         }
         break;
     }
 
     return false;
   }
 
   public boolean onDown(float x, float y) {
     switch (mViewMode) {
       case MODE_NORMAL:
         Card card = null;
         for (int i = 0; i < mCardAnchor.length; i++) {
           card = mCardAnchor[i].GrabCard(x, y);
           if (card != null) {
             if (y < card.GetY() + Card.HEIGHT/4) {
               mCardAnchor[i].AddCard(card);
               if (mCardAnchor[i].ExpandStack(x, y)) {
                 mMoveCard.InitFromAnchor(mCardAnchor[i], x-Card.WIDTH/2, y-Card.HEIGHT/2);
                 ChangeViewMode(MODE_MOVE_CARD);
                 break;
               } else {
                 card = mCardAnchor[i].PopCard();
               }
             }
             mMoveCard.SetAnchor(mCardAnchor[i]);
             mMoveCard.AddCard(card);
             ChangeViewMode(MODE_MOVE_CARD);
             break;
           }
         }
         break;
       case MODE_CARD_SELECT:
         mSelectCard.Tap(x, y);
         break;
     }
     return true;
   }
 
   public boolean onMove(float dx, float dy, float x, float y) {
     mSpeed.AddSpeed(dx, dy);
     switch (mViewMode) {
       case MODE_NORMAL:
         if (Math.abs(mDownPoint.x - x) > 15 || Math.abs(mDownPoint.y - y) > 15) {
           for (int i = 0; i < mCardAnchor.length; i++) {
             if (mCardAnchor[i].CanMoveStack(mDownPoint.x, mDownPoint.y)) {
               mMoveCard.InitFromAnchor(mCardAnchor[i], x-Card.WIDTH/2, y-Card.HEIGHT/2);
               ChangeViewMode(MODE_MOVE_CARD);
               return true;
             }
           }
         }
         break;
       case MODE_MOVE_CARD:
         mMoveCard.MovePosition(dx, dy);
         return true;
       case MODE_CARD_SELECT:
         if (mSelectCard.IsOnCard() && Math.abs(mDownPoint.x - x) > 30) {
           mMoveCard.InitFromSelectCard(mSelectCard, x, y);
           ChangeViewMode(MODE_MOVE_CARD);
         } else {
           mSelectCard.Scroll(dy);
           if (!mSelectCard.IsOnCard()) {
             mSelectCard.Tap(x, y);
           }
         }
         return true;
     }
 
     return false;
   }
 
   private void CheckMoved(float x, float y) {
     if (x >= mDownPoint.x - 30 && x <= mDownPoint.x + 30 &&
         y >= mDownPoint.y - 30 && y <= mDownPoint.y + 30) {
       mHasMoved = false;
     } else {
       mHasMoved = true;
     }    
   }
 
   public Bundle SaveState() {
     mMoveCard.Release();
     mSelectCard.Release();
 
     Bundle map = new Bundle();
 
     int cardCount = mRules.GetCardCount();
     int[] value = new int[cardCount];
     int[] suit = new int[cardCount];
     int[] anchorCardCount = new int[mCardAnchor.length];
     int[] anchorHiddenCount = new int[mCardAnchor.length];
     Card[] card;
 
     cardCount = 0;
     for (int i = 0; i < mCardAnchor.length; i++) {
       anchorCardCount[i] = mCardAnchor[i].GetCount();
       anchorHiddenCount[i] = mCardAnchor[i].GetHiddenCount();
       card = mCardAnchor[i].GetCards();
       for (int j = 0; j < anchorCardCount[i]; j++, cardCount++) {
         value[cardCount] = card[j].GetValue();
         suit[cardCount] = card[j].GetSuit();
       }
     }
 
     map.putInt("cardAnchorCount", mCardAnchor.length);
     map.putInt("cardCount", cardCount);
     map.putInt("type", mRules.GetType());
     map.putIntArray("anchorCardCount", anchorCardCount);
     map.putIntArray("anchorHiddenCount", anchorHiddenCount);
     map.putIntArray("value", value);
     map.putIntArray("suit", suit);
     map.putInt("rulesExtra", mRules.GetRulesExtra());
     map.putInt("score", mRules.GetScore());
     map.putInt("elapsedMillis", mElapsed);
 
     return map;
   }
 
   public void RestoreState(Bundle map) {
     mDrawMaster.DrawCards(GetSettings().getBoolean("DisplayBigCards", false));
     mRules = Rules.CreateRules(map.getInt("type"), map, this, mMoveHistory, mAnimateCard);
     mCardAnchor = mRules.GetAnchorArray();
     mElapsed = map.getInt("elapsedMillis");
     mStartTime = SystemClock.uptimeMillis() - mElapsed;
   }
 
   public void StartAnimating() {
     if (mViewMode != MODE_WIN && mViewMode != MODE_ANIMATE) {
       ChangeViewMode(MODE_ANIMATE);
     }
   }
 
   public void StopAnimating() {
     if (mViewMode == MODE_ANIMATE) {
       ChangeViewMode(MODE_NORMAL);
     } else if (mViewMode == MODE_WIN) {
       ChangeViewMode(MODE_WIN_STOP);
     }
   }
 
   public void Undo() {
    if (mViewMode != MODE_NORMAL) {
      return;
    }
     boolean oldIgnore = mRules.GetIgnoreEvents();
     mRules.SetIgnoreEvents(true);
 
     mMoveCard.Release();
     mSelectCard.Release();
 
     if (!mMoveHistory.empty()) {
       Move move = mMoveHistory.pop();
       int count = 0;
       int from = move.GetFrom();
       if (move.GetToBegin() != move.GetToEnd()) {
         for (int i = move.GetToBegin(); i <= move.GetToEnd(); i++) {
           for (int j = 0; j < move.GetCount(); j++) {
             mUndoStorage[count++] = mCardAnchor[i].PopCard();
           }
         }
       } else {
         for (int i = 0; i < move.GetCount(); i++) {
           mUndoStorage[count++] = mCardAnchor[move.GetToBegin()].PopCard();
         }
       }
       if (move.GetUnhide()) {
         mCardAnchor[from].SetHiddenCount(mCardAnchor[from].GetHiddenCount() + 1);
       }
       if (move.GetInvert()) {
         for (int i = 0; i < count; i++) {
           mCardAnchor[from].AddCard(mUndoStorage[i]);
         }
       } else {
         for (int i = count-1; i >= 0; i--) {
           mCardAnchor[from].AddCard(mUndoStorage[i]);
         }
       }
       if (mUndoStorage[0].GetValue() == 1) {
         for (int i = 0; i < mCardAnchor[from].GetCount(); i++) {
           Card card = mCardAnchor[from].GetCards()[i];
         }
       }
       Refresh();
     }
     mRules.SetIgnoreEvents(oldIgnore);
   }
 
   private void MarkAttempt() {
     String gameAttemptString = mRules.GetGameTypeString() + "Attempts";
     int attempts = GetSettings().getInt(gameAttemptString, 0);
     SharedPreferences.Editor editor = GetSettings().edit();
     editor.putInt(gameAttemptString, attempts + 1);
     editor.commit();
   }      
 
   private void MarkWin() {
     String gameWinString = mRules.GetGameTypeString() + "Wins";
     String gameTimeString = mRules.GetGameTypeString() + "Time";
     int wins = GetSettings().getInt(gameWinString, 0);
     int bestTime = GetSettings().getInt(gameTimeString, -1);
     SharedPreferences.Editor editor = GetSettings().edit();
 
     if (bestTime == -1 || mElapsed < bestTime) {
       editor.putInt(gameTimeString, mElapsed);
     }
 
     editor.putInt(gameWinString, wins + 1);
     editor.commit();
   }      
 
   // Simple function to check for a consistent state in Solitaire.
   private void SanityCheck() {
     int cardCount;
     int diffCardCount;
     int matchCount;
     String type = mRules.GetGameTypeString();
     if (type == "Spider1Suit") {
       cardCount = 13;
       matchCount = 8;
     } else if (type == "Spider2Suit") {
       cardCount = 26;
       matchCount = 4;
     } else if (type == "Spider4Suit") {
       cardCount = 52;
       matchCount = 2;
     } else {
       cardCount = 52;
       matchCount = 1;
     }
     
     int[] cards = new int[cardCount];
     for (int i = 0; i < cardCount; i++) {
       cards[i] = 0;
     }
     for (int i = 0; i < mCardAnchor.length; i++) {
       for (int j = 0; j < mCardAnchor[i].GetCount(); j++) {
         Card card = mCardAnchor[i].GetCards()[j];
         int idx = card.GetSuit() * 13 + card.GetValue() - 1;
         if (cards[idx] >= matchCount) {
           mTextView.setTextSize(20);
           mTextView.setGravity(Gravity.CENTER);
           DisplayText("Sanity Check Failed\nExtra: " + card.GetValue() + " " +card.GetSuit());
           return;
         }
         cards[idx]++;
       }
     }
     for (int i = 0; i < cardCount; i++) {
       if (cards[i] != matchCount) {
         mTextView.setTextSize(20);
         mTextView.setGravity(Gravity.CENTER);
         DisplayText("Sanity Check Failed\nMissing: " + (i %13 + 1) + " " + i / 13);
         return;
       }
     }
   }
 }
 
 class RefreshHandler implements Runnable {
   public static final int NO_REFRESH = 1;
   public static final int SINGLE_REFRESH = 2;
   public static final int LOCK_REFRESH = 3;
 
   private static final int FPS = 30;
 
   private boolean mRun;
   private int mRefresh;
   private SolitaireView mView;
 
   public RefreshHandler(SolitaireView solitaireView) {
     mView = solitaireView;
     mRun = true;
     mRefresh = NO_REFRESH;
   }
 
   public void SetRefresh(int refresh) {
     synchronized (this) {
       mRefresh = refresh;
     }
   }
 
   public void SingleRefresh() {
     synchronized (this) {
       if (mRefresh == NO_REFRESH) {
         mRefresh = SINGLE_REFRESH;
       }
     }
   }
 
   public void SetRunning(boolean run) {
     mRun = run;
   }
 
   public void run() {
     while (mRun) {
       try {
         Thread.sleep(1000 / FPS);
       } catch (InterruptedException e) {
       }
       mView.UpdateTime();
       if (mRefresh != NO_REFRESH) {
         mView.postInvalidate();
         if (mRefresh == SINGLE_REFRESH) {
           SetRefresh(NO_REFRESH);
         }
       }
     }
   }
 }
 
 class Speed {
   private static final int SPEED_COUNT = 4;
   private static final float SPEED_THRESHOLD = 10*10;
   private float[] mSpeed;
   private int mIdx;
 
   public Speed() {
     mSpeed = new float[SPEED_COUNT];
     Reset();
   }
   public void Reset() {
     mIdx = 0;
     for (int i = 0; i < SPEED_COUNT; i++) {
       mSpeed[i] = 0;
     }
   }
   public void AddSpeed(float dx, float dy) {
     mSpeed[mIdx] = dx*dx + dy*dy;
     mIdx = (mIdx + 1) % SPEED_COUNT;
   }
   public boolean IsFast() {
     for (int i = 0; i < SPEED_COUNT; i++) {
       if (mSpeed[i] > SPEED_THRESHOLD) {
         return true;
       }
     }
     return false;
   }
 }
 
