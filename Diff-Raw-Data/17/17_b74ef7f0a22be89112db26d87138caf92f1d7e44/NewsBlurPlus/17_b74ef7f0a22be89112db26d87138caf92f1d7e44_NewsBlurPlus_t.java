 package com.asafge.newsblurplus;
 
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.List;
 
 import org.json.JSONArray;
 import org.json.JSONException;
 import org.json.JSONObject;
 
 import android.content.Context;
 import android.os.RemoteException;
 
 import com.noinnion.android.reader.api.ReaderException;
 import com.noinnion.android.reader.api.ReaderExtension;
 import com.noinnion.android.reader.api.internal.IItemIdListHandler;
 import com.noinnion.android.reader.api.internal.IItemListHandler;
 import com.noinnion.android.reader.api.internal.ISubscriptionListHandler;
 import com.noinnion.android.reader.api.internal.ITagListHandler;
 import com.noinnion.android.reader.api.provider.IItem;
 import com.noinnion.android.reader.api.provider.ISubscription;
 
 public class NewsBlurPlus extends ReaderExtension {
 	private Context c;
 	
 	/*
 	 * Constructor
 	 */
 	@Override
 	public void onCreate() {
 		super.onCreate();
 		c = getApplicationContext();
 	};
 	
 	/*
 	 * Main sync function to get folders, feeds, and counts.
 	 * 1. Get the folders (tags) and their feeds.
 	 * 2. Ask NewsBlur to Refresh feed counts + save to feeds.
 	 * 3. Send handler the tags and feeds.
 	 */
 	@Override
 	public void handleReaderList(ITagListHandler tagHandler, ISubscriptionListHandler subHandler, long syncTime) throws ReaderException {
 		try {
 			SubsStruct.InstanceRefresh(c);
 			APIHelper.updateFeedCounts(c, SubsStruct.Instance(c).Subs);
 			tagHandler.tags(SubsStruct.Instance(c).Tags);
 			subHandler.subscriptions(SubsStruct.Instance(c).Subs);
 		}
 		catch (JSONException e) {
 			throw new ReaderException("Data parse error", e);
 		}
 		catch (RemoteException e) {
 			throw new ReaderException("Remote connection error", e);
 		}
 	}
 
 	/* 
 	 * Get a list of unread story IDS (URLs), UI will mark all other as read.
 	 * This really speeds up the sync process. 
 	 */
 	@Override
 	public void handleItemIdList(IItemIdListHandler handler, long syncTime) throws IOException, ReaderException {
 		try {
 			String url;
 			if (handler.stream().startsWith(ReaderExtension.STATE_STARRED)) {
 				url = APICall.API_URL_STARRED_ITEMS;
 			}
 			else {
 				List<String> unread_hashes = APIHelper.getUnreadHashes(c);
 				url = APICall.API_URL_RIVER;
 				for (String h : unread_hashes)
 					url += "h=" + h + "&";
 				url += "read_filter=unread";
 			}
 			APICall ac = new APICall(url, c);
 			if (ac.sync())
 				handler.items(APIHelper.extractStoryIDs(ac.Json));
 		}
 		catch (JSONException e) {
 			throw new ReaderException("Data parse error", e);
 		}
 		catch (RemoteException e) {
 			throw new ReaderException("Remote connection error", e);
 		}
 	}
 	
 	/*
 	 * Handle a single item list (a feed or a folder).
 	 * This functions calls the parseItemList function.
 	 */
 	@Override
 	public void handleItemList(IItemListHandler handler, long syncTime) throws IOException, ReaderException {
 		try {
 			String uid = handler.stream();
 			if (uid.equals(ReaderExtension.STATE_READING_LIST)) {
 				for (ISubscription sub : SubsStruct.Instance(c).Subs)
 					if (sub.unreadCount > 0 && !handler.excludedStreams().contains(sub.uid))
 						parseItemList(sub.uid.replace("FEED:", ""), handler, sub.getCategories());
 			}
 			else if (uid.startsWith("FOL:")) {
 				for (ISubscription sub : SubsStruct.Instance(c).Subs)
 					if (sub.unreadCount > 0 && sub.getCategories().contains(uid) && !handler.excludedStreams().contains(sub.uid))
 						parseItemList(sub.uid.replace("FEED:", ""), handler, sub.getCategories());
 			}
 			else if (uid.startsWith("FEED:")) {
 				if (!handler.excludedStreams().contains(uid))
 					parseItemList(handler.stream().replace("FEED:", ""), handler, null);
 			}
 			else if (uid.startsWith(ReaderExtension.STATE_STARRED)) {
 				parseItemList(APICall.API_URL_STARRED_ITEMS, handler, null);
 			}
 			else
 				throw new ReaderException("Data parse error");
 		}
 		catch (JSONException e) {
 			throw new ReaderException("Data parse error", e);
 		}
 		catch (RemoteException e) {
 			throw new ReaderException("Remote connection error", e);
 		}
 	}
 	
 	/*
 	 * Get the content of a single feed 
 	 * 
 	 * API call: https://www.newsblur.com/reader/feeds
 	 * Result: 
 	 *   feeds/[ID]/feed_address (http://feeds.feedburner.com/codinghorror - rss file)
 	 *   feeds/[ID]/feed_title ("Coding Horror")
 	 *   feeds/[ID]/feed_link (http://www.codinghorror.com/blog/ - site's link)
 	 */
 	public void parseItemList(String url, IItemListHandler handler, List<String> categories) throws IOException, ReaderException {
 		Integer page = 1;
 		while (page > 0) {
 			APICall ac = new APICall(url + "&page=" + page.toString(), c);
 			if (ac.sync()) {			
 				try {
 					List<IItem> items = new ArrayList<IItem>();
 					JSONArray arr = ac.Json.getJSONArray("stories");
 					int length = 0;
 					for (int i=0; i<arr.length(); i++) {
 						JSONObject story = arr.getJSONObject(i);
 						IItem item = new IItem();
 						item.subUid = "FEED:" + APIHelper.getFeedUrlFromFeedId(story.getString("story_feed_id"));
 						item.title = story.getString("story_title");
 						item.link = story.getString("story_permalink");
 						item.uid = story.getString("id");
 						item.author = story.getString("story_authors");
 						item.updatedTime = story.getLong("story_timestamp");
 						item.publishedTime = story.getLong("story_timestamp");
 						item.read = (story.getInt("read_status") == 1);
 						item.content = story.getString("story_content");
 						if (story.has("starred") && story.getString("starred") == "true") {
 							item.starred = true;
 							item.addCategory(StarredTag.get().uid);
 						}
 						if (categories != null)
 							for (String cat : categories)
 								item.addCategory(cat);
 						items.add(item);
 						
 						// Handle TransactionTooLargeException, based on Noin's recommendation
 						length += item.getLength();
 						if (items.size() % 200 == 0 || length > 300000) {
 							handler.items(items);
 							items.clear();
 							length = 0;
 						}
 					}
 					page = (arr.length() > 0) ? (page + 1) : -1; 
 					handler.items(items);
 					items.clear();
 				}
 				catch (JSONException e) {
 					throw new ReaderException("Data parse error", e);
 				}
 				catch (RemoteException e) {
 					throw new ReaderException("Remote connection error", e);
 				}
 			}
 		}
 	}
 	
 	/*
 	 * Main function for marking stories (and their feeds) as read/unread.
 	 */
 	private boolean markAs(boolean read, String[]  itemUids, String[]  subUIds)	{	
 		APICall ac;
 		if (itemUids == null && subUIds == null) {
 			ac = new APICall(APICall.API_URL_MARK_ALL_AS_READ, c);
 		}
 		else {
 			if (itemUids == null) {
				String url = APICall.API_URL_MARK_FEED_AS_READ + "?";
 				for (String sub : subUIds)
					url += ("feed_id=" + APIHelper.getFeedIdFromFeedUrl(sub) + "&");
				ac = new APICall(url, c);
 			}
 			else {
 				String url = read ? APICall.API_URL_MARK_STORY_AS_READ : APICall.API_URL_MARK_STORY_AS_UNREAD;
 				ac = new APICall(url, c);
 				for (int i=0; i<itemUids.length; i++) {
 					ac.addParam("story_id", itemUids[i]);
 					ac.addParam("feed_id", APIHelper.getFeedIdFromFeedUrl(subUIds[i]));
 				}
 			}
 		}
 		return ac.syncGetBool();
 	}
 
 	/* 
 	 * Mark a list of stories (and their feeds) as read
 	 */
 	@Override
 	public boolean markAsRead(String[]  itemUids, String[]  subUIds) {
 		return this.markAs(true, itemUids, subUIds);
 	}
 
 	/* 
 	 * Mark a list of stories (and their feeds) as unread
 	 */
 	@Override
 	public boolean markAsUnread(String[]  itemUids, String[]  subUids, boolean keepUnread) {
 		return this.markAs(false, itemUids, subUids);
 	}
 
 	/*
 	 * Mark all stories on all feeds as read. Iterate all feeds in order to avoid marking excluded feeds as read. 
 	 * Note: S = subscription (feed), t = tag
 	 */
 	@Override
 	public boolean markAllAsRead(String s, String t, long syncTime) {
 		try {
 			boolean result = true;
 			if (s != null && s.startsWith("FEED:")) {
 				String[] feed = { APIHelper.getFeedIdFromFeedUrl(s) };
 				result = this.markAs(true, null, feed);
 			}
 			else if (((s == null && t == null) || s.startsWith("FOL:"))) {
 				List<String> subUIDs = new ArrayList<String>();
 				for (ISubscription sub : SubsStruct.Instance(c).Subs)
 					if (s == null || sub.getCategories().contains(s))
 						subUIDs.add(sub.uid);
				result = subUIDs.isEmpty() ? false : this.markAs(true, null, subUIDs.toArray(new String[0]));
 			}
 			else
 				result = false;
 			return result;
 		}
 		catch (JSONException e) {
 			return false;
 		}
 	}
 
 	/*
 	 * Edit an item's tag - currently supports only starring/unstarring items
 	 */
 	@Override
 	public boolean editItemTag(String[] itemUids, String[] subUids, String[] addTags, String[] removeTags) throws IOException, ReaderException {
 		boolean result = true;
 		for (int i=0; i<itemUids.length; i++) {
 			String url;
 			if ((addTags != null) && addTags[i].startsWith(StarredTag.get().uid)) {
 				url = APICall.API_URL_MARK_STORY_AS_STARRED;
 			}
 			else if ((removeTags != null) && removeTags[i].startsWith(StarredTag.get().uid)) {
 				url = APICall.API_URL_MARK_STORY_AS_UNSTARRED;
 			}
 			else {
 				result = false;
 				break;
 			}
 			APICall ac = new APICall(url, c);
 			ac.addParam("story_id", itemUids[i]);
 			ac.addParam("feed_id", APIHelper.getFeedIdFromFeedUrl(subUids[i]));
 			if (!ac.syncGetBool())
 				break;
 		}
 		return result;
 	}
 
 	/*
 	 * Rename a top level folder both in News+ and in NewsBlur server
 	 */
 	@Override
 	public boolean renameTag(String tagUid, String oldLabel, String newLabel) throws IOException, ReaderException {
 		if (!tagUid.startsWith("FOL:"))
 			return false;
 		else {
 			APICall ac = new APICall(APICall.API_URL_FOLDER_RENAME, c);
 			ac.addParam("folder_to_rename", oldLabel);
 			ac.addParam("new_folder_name", newLabel);
 			ac.addParam("in_folder", "");
 			return ac.syncGetBool();
 		}
 	}
 	
 	/*
 	 * Delete a top level folder both in News+ and in NewsBlur server
 	 * This just removes the folder, not the feeds in it
 	 */
 	@Override
 	public boolean disableTag(String tagUid, String label) throws IOException, ReaderException {
 		if (tagUid.startsWith("STAR:"))
 			return false;
 		else {
 			try {
 				for (ISubscription sub : SubsStruct.Instance(c).Subs) {
 					if (sub.getCategories().contains(label))
 						if (!APIHelper.moveFeedToFolder(c, APIHelper.getFeedIdFromFeedUrl(sub.uid), label, ""));
 							return false;
 				}
 				APICall ac = new APICall(APICall.API_URL_FOLDER_DEL, c);
 				ac.addParam("folder_to_delete", label);
 				return ac.syncGetBool();
 			}
 			catch (JSONException e) {
 				return false;
 			}
 		}
 	}
 	
 	/*
 	 * Main function for editing subscriptions - add/delete/rename/change-folder
 	 */	
 	@Override
 	public boolean editSubscription(String uid, String title, String feed_url, String[] tags, int action, long syncTime) throws IOException, ReaderException {
 		APICall ac = new APICall(c);
 		switch (action) {
 			// Feed - add/delete/rename
 			case ReaderExtension.SUBSCRIPTION_ACTION_SUBCRIBE:
 				ac.createCallback(APICall.API_URL_FEED_ADD, c);
 				ac.addParam("url", feed_url);
 				return ac.syncGetBool();
 			case ReaderExtension.SUBSCRIPTION_ACTION_UNSUBCRIBE:
 				ac.createCallback(APICall.API_URL_FEED_DEL, c);
 				ac.addParam("feed_id", APIHelper.getFeedIdFromFeedUrl(uid));
 				return ac.syncGetBool();
 			case ReaderExtension.SUBSCRIPTION_ACTION_EDIT:
 				ac.createCallback(APICall.API_URL_FEED_RENAME, c);
 				ac.addParam("feed_id", APIHelper.getFeedIdFromFeedUrl(uid));
 				ac.addParam("feed_title", title);
 				return ac.syncGetBool();
 
 			// Feed's parent folder - add/delete 
 			case ReaderExtension.SUBSCRIPTION_ACTION_ADD_LABEL:
 				// TODO: Always getting tags=[]
 				ac.createCallback(APICall.API_URL_FOLDER_ADD, c);
 				ac.addParam("folder", tags[0]);
 				return ac.syncGetBool() && APIHelper.moveFeedToFolder(c, APIHelper.getFeedIdFromFeedUrl(uid), "", tags[0]);
 			case ReaderExtension.SUBSCRIPTION_ACTION_REMOVE_LABEL:
 				return APIHelper.moveFeedToFolder(c, APIHelper.getFeedIdFromFeedUrl(uid), tags[0], "");
 				
 			default:
 				return false;
 		}
 	}
 }
