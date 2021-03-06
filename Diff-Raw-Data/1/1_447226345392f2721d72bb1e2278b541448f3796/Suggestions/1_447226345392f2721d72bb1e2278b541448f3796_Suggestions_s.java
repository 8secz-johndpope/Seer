 /*
  * Copyright (C) 2009 The Android Open Source Project
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package com.android.quicksearchbox;
 
 import android.content.ComponentName;
 import android.database.DataSetObservable;
 import android.database.DataSetObserver;
 import android.util.Log;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 
 /**
  * Contains all {@link SuggestionCursor} objects that have been reported.
  */
 public class Suggestions {
 
     private static final boolean DBG = false;
     private static final String TAG = "QSB.Suggestions";
 
     private final int mMaxPromoted;
 
     private final String mQuery;
 
     /** The number of sources that are expected to report. */
     private final int mExpectedSourceCount;
 
     /**
      * The observers that want notifications of changes to the published suggestions.
      * This object may be accessed on any thread.
      */
     private final DataSetObservable mDataSetObservable = new DataSetObservable();
 
     /**
      * All {@link SuggestionCursor} objects that have been published so far,
      * in the order that they were published.
      * This object may only be accessed on the UI thread.
      * */
     private final ArrayList<SuggestionCursor> mSourceResults;
 
     /**
      * All {@link SuggestionCursor} objects that have been published so far.
      * This object may only be accessed on the UI thread.
      * */
     private final HashMap<ComponentName,SuggestionCursor> mSourceResultsBySource;
 
     private SuggestionCursor mShortcuts;
 
     private MyShortcutsObserver mShortcutsObserver = new MyShortcutsObserver();
 
     /** True if {@link Suggestions#close} has been called. */
     private boolean mClosed = false;
 
     private final Promoter mPromoter;
 
     private ListSuggestionCursor mPromoted;
 
     /**
      * Creates a new empty Suggestions.
      *
      * @param expectedSourceCount The number of sources that are expected to report.
      */
     public Suggestions(Promoter promoter, int maxPromoted,
             String query, int expectedSourceCount) {
         mPromoter = promoter;
         mMaxPromoted = maxPromoted;
         mQuery = query;
         mExpectedSourceCount = expectedSourceCount;
         mSourceResults = new ArrayList<SuggestionCursor>(mExpectedSourceCount);
         mSourceResultsBySource = new HashMap<ComponentName,SuggestionCursor>(mExpectedSourceCount);
         mPromoted = null;  // will be set by updatePromoted()
     }
 
     public String getQuery() {
         return mQuery;
     }
 
     /**
      * Gets the number of sources that are expected to report.
      */
     public int getExpectedSourceCount() {
         return mExpectedSourceCount;
     }
 
     /**
      * Registers an observer that will be notified when the reported results or
      * the done status changes.
      */
     public void registerDataSetObserver(DataSetObserver observer) {
         if (mClosed) {
             throw new IllegalStateException("registerDataSetObserver() when closed");
         }
         mDataSetObservable.registerObserver(observer);
     }
 
     /**
      * Unregisters an observer.
      */
     public void unregisterDataSetObserver(DataSetObserver observer) {
         mDataSetObservable.unregisterObserver(observer);
     }
 
     public SuggestionCursor getPromoted() {
         if (mPromoted == null) {
             updatePromoted();
         }
         return mPromoted;
     }
 
     /**
      * Calls {@link DataSetObserver#onChanged()} on all observers.
      */
     private void notifyDataSetChanged() {
         if (DBG) Log.d(TAG, "notifyDataSetChanged()");
         mDataSetObservable.notifyChanged();
     }
 
     /**
      * Closes all the source results and unregisters all observers.
      */
     public void close() {
         if (DBG) Log.d(TAG, "close()");
         if (mClosed) {
             throw new IllegalStateException("Double close()");
         }
         mDataSetObservable.unregisterAll();
         mClosed = true;
         if (mShortcuts != null) {
             mShortcuts.close();
             mShortcuts = null;
         }
         for (SuggestionCursor result : mSourceResults) {
             result.close();
         }
         mSourceResults.clear();
         mSourceResultsBySource.clear();
     }
 
     @Override
     protected void finalize() {
         if (!mClosed) {
             Log.e(TAG, "LEAK! Finalized without being closed: Suggestions[" + mQuery + "]");
             close();
         }
     }
 
     /**
      * Checks whether all sources have reported.
      * Must be called on the UI thread, or before this object is seen by the UI thread.
      */
     public boolean isDone() {
         return mSourceResults.size() >= mExpectedSourceCount;
     }
 
     /**
      * Sets the shortcut suggestions.
      * Must be called on the UI thread, or before this object is seen by the UI thread.
      *
      * @param shortcuts The shortcuts.
      */
     public void setShortcuts(SuggestionCursor shortcuts) {
         if (DBG) Log.d(TAG, "setShortcuts(" + shortcuts + ")");
         mShortcuts = shortcuts;
         if (shortcuts != null) {
             mShortcuts.registerDataSetObserver(mShortcutsObserver);
         }
     }
 
     /**
      * Adds a source result. Must be called on the UI thread, or before this
      * object is seen by the UI thread.
      *
      * @param sourceResult The source result.
      */
     public void addSourceResult(SuggestionCursor sourceResult) {
         if (mClosed) {
             sourceResult.close();
             return;
         }
         if (!mQuery.equals(sourceResult.getUserQuery())) {
           throw new IllegalArgumentException("Got result for wrong query: "
                 + mQuery + " != " + sourceResult.getUserQuery());
         }
         mSourceResults.add(sourceResult);
         mSourceResultsBySource.put(sourceResult.getSourceComponentName(), sourceResult);
         mPromoted = null;
         notifyDataSetChanged();
     }
 
     private void updatePromoted() {
         mPromoted = new ListSuggestionCursorNoDuplicates(mQuery);
         if (mPromoter == null) {
             return;
         }
         mPromoter.pickPromoted(mShortcuts, mSourceResults, mMaxPromoted, mPromoted);
     }
 
     /**
      * Gets a given source result.
      * Must be called on the UI thread, or before this object is seen by the UI thread.
      *
      * @param sourcePos 
      * @return The source result at the given position.
      * @throws IndexOutOfBoundsException If {@code sourcePos < 0} or
      *         {@code sourcePos >= getSourceCount()}.
      */
     public SuggestionCursor getSourceResult(int sourcePos) {
         if (mClosed) {
             throw new IllegalStateException("Called getSourceResult(" + sourcePos
                 + ") when closed.");
         }
         return mSourceResults.get(sourcePos);
     }
 
     /**
      * Gets a given source result.
      * Must be called on the UI thread, or before this object is seen by the UI thread.
      *
      * @param source Source name.
      * @return The source result for the given source. {@code null} if the source has not
      *         yet returned.
      */
     public SuggestionCursor getSourceResult(ComponentName source) {
         if (mClosed) {
             throw new IllegalStateException("Called getSourceResult(" + source
                 + ") when closed.");
         }
         return mSourceResultsBySource.get(source);
     }
 
     /**
      * Gets the number of source results.
      * Must be called on the UI thread, or before this object is seen by the UI thread.
      */
     public int getSourceCount() {
         if (mClosed) {
             throw new IllegalStateException("Called getSourceCount() when closed.");
         }
         return mSourceResults == null ? 0 : mSourceResults.size();
     }
 
     private class MyShortcutsObserver extends DataSetObserver {
         @Override
         public void onChanged() {
             notifyDataSetChanged();
         }
     }
 
 }
