 /*
  * Copyright (C) 2012 Brian Muramatsu
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
 
 package com.btmura.android.reddit.widget;
 
 import android.content.Context;
 import android.content.CursorLoader;
 import android.content.Loader;
 import android.database.Cursor;
 import android.net.Uri;
 import android.os.Bundle;
 import android.text.TextUtils;
 import android.view.View;
 
 import com.btmura.android.reddit.database.Messages;
 import com.btmura.android.reddit.net.Urls;
 import com.btmura.android.reddit.provider.MessageProvider;
 import com.btmura.android.reddit.util.Array;
 import com.btmura.android.reddit.util.Objects;
 import com.btmura.android.reddit.widget.ThingAdapter.ProviderAdapter;
 
 class MessageProviderAdapter extends ProviderAdapter {
 
     private static final String[] PROJECTION = {
             Messages._ID,
             Messages.COLUMN_AUTHOR,
             Messages.COLUMN_BODY,
             Messages.COLUMN_CONTEXT,
             Messages.COLUMN_CREATED_UTC,
             Messages.COLUMN_KIND,
             Messages.COLUMN_SUBREDDIT,
             Messages.COLUMN_THING_ID,
             Messages.COLUMN_WAS_COMMENT,
            Messages.COLUMN_VOTE,
     };
 
     private static final int INDEX_AUTHOR = 1;
     private static final int INDEX_BODY = 2;
     private static final int INDEX_CONTEXT = 3;
     private static final int INDEX_CREATED_UTC = 4;
     private static final int INDEX_KIND = 5;
     private static final int INDEX_SUBREDDIT = 6;
     private static final int INDEX_THING_ID = 7;
     private static final int INDEX_WAS_COMMENT = 8;
    private static final int INDEX_VOTE = 9;
 
     @Override
     Uri getLoaderUri(Bundle args) {
         return MessageProvider.MESSAGES_URI;
     }
 
     @Override
     Loader<Cursor> getLoader(Context context, Uri uri, Bundle args) {
         String selection;
         switch (getFilter(args)) {
             case FilterAdapter.MESSAGE_UNREAD:
                 selection = Messages.SELECT_NEW_BY_ACCOUNT_FROM_INBOX;
                 break;
 
             case FilterAdapter.MESSAGE_SENT:
                 selection = Messages.SELECT_BY_ACCOUNT_FROM_SENT;
                 break;
 
             default:
                 selection = Messages.SELECT_BY_ACCOUNT_FROM_INBOX;
                 break;
         }
         return new CursorLoader(context, uri, PROJECTION, selection,
                 Array.of(getAccountName(args)), null);
     }
 
     @Override
     boolean isLoadable(Bundle args) {
         return getAccountName(args) != null && getMessageUser(args) != null;
     }
 
     @Override
     String createSessionId(Bundle args) {
         return null; // Messages doesn't use the session API.
     }
 
     @Override
     void deleteSessionData(Context context, Bundle args) {
         // Messages doesn't use the session API.
     }
 
     @Override
     String getThingId(ThingAdapter adapter, int position) {
         return adapter.getString(position, INDEX_THING_ID);
     }
 
     @Override
     String getLinkId(ThingAdapter adapter, int position) {
         return null; // Messages don't have link ids.
     }
 
     @Override
     String getAuthor(ThingAdapter adapter, int position) {
         return adapter.getString(position, INDEX_AUTHOR);
     }
 
     @Override
     String getTitle(ThingAdapter adapter, int position) {
         return adapter.getString(position, INDEX_BODY);
     }
 
     @Override
     String getUrl(ThingAdapter adapter, int position) {
         // Comment reply messages have a context url we can use.
         String context = adapter.getString(position, INDEX_CONTEXT);
         if (!TextUtils.isEmpty(context)) {
             return Urls.permaUrl(context, null).toExternalForm();
         }
 
         // Assume this is a raw message.
         return Urls.messageMessagesUrl(getThingId(adapter, position)).toExternalForm();
     }
 
     @Override
     int getKind(ThingAdapter adapter, int position) {
         return adapter.getInt(position, INDEX_KIND);
     }
 
     @Override
     String getMoreThingId(ThingAdapter adapter) {
         return null; // Pagination not supported in messages.
     }
 
     @Override
     void bindThingView(ThingAdapter adapter, View view, Context context, Cursor cursor) {
         String author = cursor.getString(INDEX_AUTHOR);
         String body = cursor.getString(INDEX_BODY);
         long createdUtc = cursor.getLong(INDEX_CREATED_UTC);
        int likes = !cursor.isNull(INDEX_VOTE) ? cursor.getInt(INDEX_VOTE) : 0;
         int kind = cursor.getInt(INDEX_KIND);
         String subreddit = cursor.getString(INDEX_SUBREDDIT);
         String thingId = cursor.getString(INDEX_THING_ID);
 
         ThingView tv = (ThingView) view;
        tv.setData(adapter.accountName, author, body, createdUtc, null, 0, true, kind, likes,
                 null, 0, adapter.nowTimeMs, 0, false, adapter.parentSubreddit, 0, subreddit,
                 adapter.thingBodyWidth, thingId, null, null, 0);
         tv.setChosen(adapter.singleChoice && Objects.equals(adapter.selectedThingId, thingId));
         tv.setOnVoteListener(adapter.listener);
     }
 
     @Override
     Bundle makeThingBundle(Context context, Cursor cursor) {
         Bundle b = new Bundle(5);
         ThingBundle.putSubreddit(b, cursor.getString(INDEX_SUBREDDIT));
         ThingBundle.putKind(b, cursor.getInt(INDEX_KIND));
 
         // Messages don't have titles so use the body for both.
         String body = cursor.getString(INDEX_BODY);
         ThingBundle.putTitle(b, body);
 
         ThingBundle.putThingId(b, cursor.getString(INDEX_THING_ID));
 
         String contextUrl = cursor.getString(INDEX_CONTEXT);
         if (!TextUtils.isEmpty(contextUrl)) {
             // If there is a context url, then we have to parse that url to grab
             // the link id embedded inside of it like:
             //
             // /r/rbb/comments/13ejyf/testing_from_laptop/c738opg?context=3
             String[] parts = contextUrl.split("/");
             if (parts != null && parts.length >= 5) {
                 ThingBundle.putLinkId(b, parts[4]);
             }
         }
 
         // If this message isn't a comment, then it's simply a message with no
         // comments or links.
         ThingBundle.putNoComments(b, cursor.getInt(INDEX_WAS_COMMENT) == 0);
 
         return b;
     }
 }
