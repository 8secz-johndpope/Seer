 package com.asafge.pocketplus;
 
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 
 import org.json.JSONArray;
 import org.json.JSONException;
 import org.json.JSONObject;
 
 import android.content.Context;
 import android.os.RemoteException;
 import android.util.Log;
 
 import com.asafge.pocketplus.APICall;
 import com.asafge.pocketplus.StarredTag;
 import com.noinnion.android.reader.api.ReaderException;
 import com.noinnion.android.reader.api.ReaderExtension;
 import com.noinnion.android.reader.api.internal.IItemIdListHandler;
 import com.noinnion.android.reader.api.internal.IItemListHandler;
 import com.noinnion.android.reader.api.internal.ISubscriptionListHandler;
 import com.noinnion.android.reader.api.internal.ITagListHandler;
 import com.noinnion.android.reader.api.provider.IItem;
 import com.noinnion.android.reader.api.provider.ISubscription;
 import com.noinnion.android.reader.api.provider.ITag;
 
 public class PocketPlus extends ReaderExtension {
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
 	 * Designed for getting feed/folder structure, but Pocket API doesn't have an endpoint for that.
 	 * This function will just initialize the constant tags, user tags are added while parsing stories.
 	 */
 	@Override
 	public void handleReaderList(ITagListHandler tagHandler, ISubscriptionListHandler subHandler, long syncTime) throws ReaderException {
 		try {
 			List<ISubscription> subs = new ArrayList<ISubscription>();
 			List<ITag> tags = new ArrayList<ITag>();
 
 			// Untagged "feed"
 			ISubscription untagged = new ISubscription();
 			untagged.title = APICall.POCKET_UNTAGGED_TITLE;
 			untagged.uid = APICall.POCKET_UNTAGGED_TITLE;
			untagged.htmlUrl = APICall.POCKET_HOME_URL;
 			subs.add(untagged);
 			
 			// Tagged "feed"
 			ISubscription tagged = new ISubscription();
			tagged.title = APICall.POCKET_TAGGED_TITLE;
			tagged.uid = APICall.POCKET_TAGGED_TITLE;
 			tagged.htmlUrl = APICall.POCKET_HOME_URL;
 			subs.add(tagged);			
 			
 			subHandler.subscriptions(subs);
 			
 			tags.add(StarredTag.get());			
 			tagHandler.tags(tags);
 		}
 		catch (RemoteException e) {
 			throw new ReaderException("Sub/tag handler error", e);
 		}
 	}
 	
 	/* 
 	 * Gets the list of unread item IDs, used for 2-way sync.
 	 * This function calls the parseIDList function. 
 	 */
 	@Override
 	public void handleItemIdList(IItemIdListHandler handler, long syncTime) throws ReaderException {
 		try {
 			APICall ac = new APICall(APICall.API_URL_GET, c);
 			String uid = handler.stream();
 			
 			if (uid.startsWith(ReaderExtension.STATE_STARRED)) {
 				ac.addPostParam("state", "all");
 				ac.addPostParam("favorite", "1");
 			}
 			else {
 				ac.addPostParam("state", "unread");
 			}
 			ac.addPostParam("sort", "newest");
 			ac.addPostParam("detailType", "simple");
 			//ac.addPostParam("since", String.valueOf(syncTime));
 			ac.addPostParam("count", String.valueOf(handler.limit()));
 			
 			ac.makeAuthenticated().sync();
 			parseIDList(ac.Json, handler);
 		}
 		catch (RemoteException e) {
 			throw new ReaderException("ItemList handler error", e);
 		}
 	}
 	
 	/*
 	 * Parse an array of items to get the IDs, from the Pocket JSON object.
 	 */
 	public void parseIDList(JSONObject json, IItemIdListHandler handler) throws ReaderException {
 		try {
 			List<String> items = new ArrayList<String>();
 			JSONObject item_list = json.optJSONObject("list");
 			if (item_list != null) {
 				Iterator<?> keys = item_list.keys();
 				while (keys.hasNext()) {
 					items.add((String)keys.next());
 				}
 				handler.items(items);
 			}
 		}
 		catch (RemoteException e) {
 			throw new ReaderException("SingleItem handler error", e);
 		}
 	}
 	
 	/*
 	 * Get the list of all stories (starred or regular).
 	 * This functions calls the parseItemList function.
 	 */
 	@Override
 	public void handleItemList(IItemListHandler handler, long syncTime) throws ReaderException {
 		try {
 			APICall ac = new APICall(APICall.API_URL_GET, c);
 			String uid = handler.stream();
 			
 			if (uid.startsWith(ReaderExtension.STATE_STARRED)) {
 				ac.addPostParam("state", "all");
 				ac.addPostParam("favorite", "1");
 			}
 			else {
 				ac.addPostParam("state", "unread");
 			}
 			ac.addPostParam("sort", "newest");
 			ac.addPostParam("detailType", "complete");
 			//ac.addPostParam("since", String.valueOf(syncTime));
 			ac.addPostParam("count", String.valueOf(handler.limit()));
 			
 			ac.makeAuthenticated().sync();
 			parseItemList(ac.Json, handler);
 		}
 		catch (RemoteException e) {
 			throw new ReaderException("ItemList handler error", e);
 		}
 	}
 	
 	/*
 	 * Parse an array of items that are in the Pocket JSON format.
 	 */
 	public void parseItemList(JSONObject json, IItemListHandler handler) throws ReaderException {
 		try {
 			int length = 0;
 			List<IItem> items = new ArrayList<IItem>();
 			JSONObject item_list = json.optJSONObject("list");
 			if (item_list != null) {
 				Iterator<?> keys = item_list.keys();
 				while (keys.hasNext()) {
 					String uid = (String)keys.next();
 					JSONObject story = item_list.getJSONObject(uid);
 					IItem item = new IItem();
 					item.uid = uid;
 					item.title = story.getString("resolved_title");
 					item.link = story.getString("resolved_url");
 					item.id = Long.parseLong(uid);
 					item.read = (story.getInt("status") != 0);
 					item.starred = (story.getString("favorite").startsWith("1"));
 					item.content = story.getString("excerpt");
 					if (item.starred)
 						item.addTag(StarredTag.get().uid);
 					
 					JSONObject tags = story.optJSONObject("tags");
 					if (tags != null) {
 						Iterator<?> tag_keys = tags.keys();
 						while (tag_keys.hasNext()) {
 							ITag tag = createTag((String)tag_keys.next(), false);
 							item.addTag(tag.uid, tag.label);
 						}
						item.subUid = APICall.POCKET_TAGGED_TITLE;
 					}
 					else {
 						item.subUid = APICall.POCKET_UNTAGGED_TITLE;
 					}
 					
 					item.updatedTime = story.getLong("time_updated");
 					item.publishedTime = story.getLong("time_added");
 					items.add(item);
 					
 					length += item.getLength();
 					if (items.size() % 200 == 0 || length > 300000) {
 						handler.items(items, 0);
 						items.clear();
 						length = 0;
 					}
 				}
 				handler.items(items, 0);
 			}
 		}
 		catch (JSONException e) {
 			Log.e("Pocket+ Debug", "JSONExceotion: " + e.getMessage());
 			Log.e("Pocket+ Debug", "JSON: " + json.toString());
 			throw new ReaderException("ParseItemList parse error", e);
 		}
 		catch (RemoteException e) {
 			throw new ReaderException("SingleItem handler error", e);
 		}
 	}
 	
 	/*
 	 * Main function for marking stories (and their feeds) as read/unread.
 	 */
 	private boolean markAs(boolean read, String[]  itemUids, String[]  subUIds) throws ReaderException	{	
 		APICall ac = new APICall(APICall.API_URL_SEND, c);
 		try {
 			if (itemUids == null && subUIds == null) {
 				throw new ReaderException("Mark all as read not supported in Pocket");
 			}
 			else if (itemUids != null) {
 				JSONArray list = new JSONArray();
 				for (String itemUid : itemUids) {
 					JSONObject action = new JSONObject();
 					action.put("action", read ? "archive" : "readd");
 					action.put("item_id", itemUid);
 					list.put(action);
 				}
 				ac.addPostParam("actions", list.toString());
 				return ac.makeAuthenticated().syncGetResultOk();
 			}
 			else {
 				// TODO: Mark tag as read (using markAllAsRead function)
 				return false;
 			}
 		}
 		catch (JSONException e) {
 			return false;	
 		}
 	}
 
 	/* 
 	 * Mark a list of stories (and their feeds) as read
 	 */
 	@Override
 	public boolean markAsRead(String[]  itemUids, String[]  subUIds) throws ReaderException {
 		return this.markAs(true, itemUids, subUIds);
 	}
 
 	/* 
 	 * Mark a list of stories (and their feeds) as unread
 	 */
 	@Override
 	public boolean markAsUnread(String[]  itemUids, String[]  subUids, boolean keepUnread) throws ReaderException {
 		return this.markAs(false, itemUids, subUids);
 	}
 
 	/*
 	 * Mark all stories on all feeds as read. Iterate all feeds in order to avoid marking excluded feeds as read. 
 	 * Note: S = subscription (feed), t = tag
 	 */
 	@Override
 	public boolean markAllAsRead(String arg0, String arg1, String[] arg2, long arg3) throws IOException, ReaderException {
 		// TODO: mark tag as read
 		return false;
 	}
 
 	/*
 	 * Create a new tag object 
 	 */
 	public static ITag createTag(String name, boolean isStar) {
 		ITag tag = new ITag();
 		tag.label = name;
 		String prefix = isStar ? "STAR" : "TAG";
 		tag.uid = name = (prefix + ":" + name);
 		tag.type = isStar ? ITag.TYPE_TAG_STARRED : ITag.TYPE_FOLDER;
 		return tag;
 	}
 	
 	/*
 	 * Edit an item's tag - starring/unstarring items, or changing string-tags
 	 */
 	@Override
 	public boolean editItemTag(String[] itemUids, String[] subUids, String[] addTags, String[] removeTags) throws IOException, ReaderException {
 		try {
 			APICall ac = new APICall(APICall.API_URL_SEND, c);
 			JSONArray list = new JSONArray();
 			for (int i=0; i<itemUids.length; i++) {
 				JSONObject action = new JSONObject();
 				action.put("item_id", itemUids[i]);
 
 				if (addTags != null) {
 					if (addTags[i].startsWith(StarredTag.get().uid)) { 
 						action.put("action", "favorite");
 					}
 					else {
 						action.put("action", "tags_add");
 						action.put("tags", new JSONArray().put(addTags[i].replace("TAG:", "")));
 					}
 				}
 				if (removeTags != null) {
 					if (removeTags[i].startsWith(StarredTag.get().uid)) { 
 						action.put("action", "unfavorite");
 					}
 					else {
 						action.put("action", "tags_remove");
 						action.put("tags", new JSONArray().put(removeTags[i].replace("TAG:", "")));
 					}
 				}
 				list.put(action);
 			}
 			ac.addPostParam("actions", list.toString());
 			return ac.makeAuthenticated().syncGetResultOk();
 		}
 		catch (JSONException e) {
 			Log.e("Pocket+ Debug", "JSONExceotion: " + e.getMessage());
 			return false;
 		}
 	}
 
 	/*
 	 * Rename a top level tag both in News+ and in Pocket server
 	 */
 	@Override
 	public boolean renameTag(String tagUid, String oldLabel, String newLabel) throws IOException, ReaderException {
 		// TODO
 		return false;
 	}
 	
 	/*
 	 * Delete a top level tag both in News+ and in Pocket server
 	 * This just removes the tag, not the feeds in it
 	 */
 	@Override
 	public boolean disableTag(String tagUid, String label) throws IOException, ReaderException {
 		if (tagUid.startsWith("STAR:"))
 			return false;
 		return false;
 	}
 	
 
 	/* 
 	 * Not implemented in Pocket+ - editing subscriptions (add/delete/rename/change-folder)
 	 */
 	@Override
 	public boolean editSubscription(String arg0, String arg1, String arg2,
 			String[] arg3, int arg4, long arg5) throws IOException,
 			ReaderException {
 		return false;
 	}
 }
