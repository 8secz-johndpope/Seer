 /*
  * Sone - Core.java - Copyright © 2010 David Roden
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 
 package net.pterodactylus.sone.core;
 
 import java.net.MalformedURLException;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 import net.pterodactylus.sone.core.Options.DefaultOption;
 import net.pterodactylus.sone.core.Options.Option;
 import net.pterodactylus.sone.core.Options.OptionWatcher;
 import net.pterodactylus.sone.data.Post;
 import net.pterodactylus.sone.data.Profile;
 import net.pterodactylus.sone.data.Reply;
 import net.pterodactylus.sone.data.Sone;
 import net.pterodactylus.sone.freenet.wot.Identity;
 import net.pterodactylus.sone.freenet.wot.IdentityListener;
 import net.pterodactylus.sone.freenet.wot.IdentityManager;
 import net.pterodactylus.sone.freenet.wot.OwnIdentity;
 import net.pterodactylus.util.config.Configuration;
 import net.pterodactylus.util.config.ConfigurationException;
 import net.pterodactylus.util.logging.Logging;
 import net.pterodactylus.util.number.Numbers;
 import freenet.keys.FreenetURI;
 
 /**
  * The Sone core.
  *
  * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
  */
 public class Core implements IdentityListener {
 
 	/**
 	 * Enumeration for the possible states of a {@link Sone}.
 	 *
 	 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 	 */
 	public enum SoneStatus {
 
 		/** The Sone is unknown, i.e. not yet downloaded. */
 		unknown,
 
 		/** The Sone is idle, i.e. not being downloaded or inserted. */
 		idle,
 
 		/** The Sone is currently being inserted. */
 		inserting,
 
 		/** The Sone is currently being downloaded. */
 		downloading,
 	}
 
 	/** The logger. */
 	private static final Logger logger = Logging.getLogger(Core.class);
 
 	/** The options. */
 	private final Options options = new Options();
 
 	/** The configuration. */
 	private final Configuration configuration;
 
 	/** The identity manager. */
 	private final IdentityManager identityManager;
 
 	/** Interface to freenet. */
 	private final FreenetInterface freenetInterface;
 
 	/** The Sone downloader. */
 	private final SoneDownloader soneDownloader;
 
 	/** The Sones’ statuses. */
 	/* synchronize access on itself. */
 	private final Map<Sone, SoneStatus> soneStatuses = new HashMap<Sone, SoneStatus>();
 
 	/** Sone inserters. */
 	/* synchronize access on this on localSones. */
 	private final Map<Sone, SoneInserter> soneInserters = new HashMap<Sone, SoneInserter>();
 
 	/** All local Sones. */
 	/* synchronize access on this on itself. */
 	private Map<String, Sone> localSones = new HashMap<String, Sone>();
 
 	/** All remote Sones. */
 	/* synchronize access on this on itself. */
 	private Map<String, Sone> remoteSones = new HashMap<String, Sone>();
 
 	/** All posts. */
 	private Map<String, Post> posts = new HashMap<String, Post>();
 
 	/** All replies. */
 	private Map<String, Reply> replies = new HashMap<String, Reply>();
 
 	/**
 	 * Creates a new core.
 	 *
 	 * @param configuration
 	 *            The configuration of the core
 	 * @param freenetInterface
 	 *            The freenet interface
 	 * @param identityManager
 	 *            The identity manager
 	 */
 	public Core(Configuration configuration, FreenetInterface freenetInterface, IdentityManager identityManager) {
 		this.configuration = configuration;
 		this.freenetInterface = freenetInterface;
 		this.identityManager = identityManager;
 		this.soneDownloader = new SoneDownloader(this, freenetInterface);
 	}
 
 	//
 	// ACCESSORS
 	//
 
 	/**
 	 * Returns the options used by the core.
 	 *
 	 * @return The options of the core
 	 */
 	public Options getOptions() {
 		return options;
 	}
 
 	/**
 	 * Returns the identity manager used by the core.
 	 *
 	 * @return The identity manager
 	 */
 	public IdentityManager getIdentityManager() {
 		return identityManager;
 	}
 
 	/**
 	 * Returns the status of the given Sone.
 	 *
 	 * @param sone
 	 *            The Sone to get the status for
 	 * @return The status of the Sone
 	 */
 	public SoneStatus getSoneStatus(Sone sone) {
 		synchronized (soneStatuses) {
 			return soneStatuses.get(sone);
 		}
 	}
 
 	/**
 	 * Sets the status of the given Sone.
 	 *
 	 * @param sone
 	 *            The Sone to set the status of
 	 * @param soneStatus
 	 *            The status to set
 	 */
 	public void setSoneStatus(Sone sone, SoneStatus soneStatus) {
 		synchronized (soneStatuses) {
 			soneStatuses.put(sone, soneStatus);
 		}
 	}
 
 	/**
 	 * Returns all Sones, remote and local.
 	 *
 	 * @return All Sones
 	 */
 	public Set<Sone> getSones() {
 		Set<Sone> allSones = new HashSet<Sone>();
 		allSones.addAll(getLocalSones());
 		allSones.addAll(getRemoteSones());
 		return allSones;
 	}
 
 	/**
 	 * Returns the Sone with the given ID, regardless whether it’s local or
 	 * remote.
 	 *
 	 * @param id
 	 *            The ID of the Sone to get
 	 * @return The Sone with the given ID, or {@code null} if there is no such
 	 *         Sone
 	 */
 	public Sone getSone(String id) {
 		if (isLocalSone(id)) {
 			return getLocalSone(id);
 		}
 		return getRemoteSone(id);
 	}
 
 	/**
 	 * Returns whether the given Sone is a local Sone.
 	 *
 	 * @param sone
 	 *            The Sone to check for its locality
 	 * @return {@code true} if the given Sone is local, {@code false} otherwise
 	 */
 	public boolean isLocalSone(Sone sone) {
 		synchronized (localSones) {
 			return localSones.containsKey(sone.getId());
 		}
 	}
 
 	/**
 	 * Returns whether the given ID is the ID of a local Sone.
 	 *
 	 * @param id
 	 *            The Sone ID to check for its locality
 	 * @return {@code true} if the given ID is a local Sone, {@code false}
 	 *         otherwise
 	 */
 	public boolean isLocalSone(String id) {
 		synchronized (localSones) {
 			return localSones.containsKey(id);
 		}
 	}
 
 	/**
 	 * Returns all local Sones.
 	 *
 	 * @return All local Sones
 	 */
 	public Set<Sone> getLocalSones() {
 		synchronized (localSones) {
 			return new HashSet<Sone>(localSones.values());
 		}
 	}
 
 	/**
 	 * Returns the local Sone with the given ID.
 	 *
 	 * @param id
 	 *            The ID of the Sone to get
 	 * @return The Sone with the given ID
 	 */
 	public Sone getLocalSone(String id) {
 		synchronized (localSones) {
 			Sone sone = localSones.get(id);
 			if (sone == null) {
 				sone = new Sone(id);
 				localSones.put(id, sone);
 			}
 			return sone;
 		}
 	}
 
 	/**
 	 * Returns all remote Sones.
 	 *
 	 * @return All remote Sones
 	 */
 	public Set<Sone> getRemoteSones() {
 		synchronized (remoteSones) {
 			return new HashSet<Sone>(remoteSones.values());
 		}
 	}
 
 	/**
 	 * Returns the remote Sone with the given ID.
 	 *
 	 * @param id
 	 *            The ID of the remote Sone to get
 	 * @return The Sone with the given ID
 	 */
 	public Sone getRemoteSone(String id) {
 		synchronized (remoteSones) {
 			Sone sone = remoteSones.get(id);
 			if (sone == null) {
 				sone = new Sone(id);
 				remoteSones.put(id, sone);
 			}
 			return sone;
 		}
 	}
 
 	/**
 	 * Returns whether the given Sone is a remote Sone.
 	 *
 	 * @param sone
 	 *            The Sone to check
 	 * @return {@code true} if the given Sone is a remote Sone, {@code false}
 	 *         otherwise
 	 */
 	public boolean isRemoteSone(Sone sone) {
 		synchronized (remoteSones) {
 			return remoteSones.containsKey(sone.getId());
 		}
 	}
 
 	/**
 	 * Returns the post with the given ID.
 	 *
 	 * @param postId
 	 *            The ID of the post to get
 	 * @return The post, or {@code null} if there is no such post
 	 */
 	public Post getPost(String postId) {
 		synchronized (posts) {
 			Post post = posts.get(postId);
 			if (post == null) {
 				post = new Post(postId);
 				posts.put(postId, post);
 			}
 			return post;
 		}
 	}
 
 	/**
 	 * Returns the reply with the given ID.
 	 *
 	 * @param replyId
 	 *            The ID of the reply to get
 	 * @return The reply, or {@code null} if there is no such reply
 	 */
 	public Reply getReply(String replyId) {
 		synchronized (replies) {
 			Reply reply = replies.get(replyId);
 			if (reply == null) {
 				reply = new Reply(replyId);
 				replies.put(replyId, reply);
 			}
 			return reply;
 		}
 	}
 
 	/**
 	 * Returns all replies for the given post, order ascending by time.
 	 *
 	 * @param post
 	 *            The post to get all replies for
 	 * @return All replies for the given post
 	 */
 	public List<Reply> getReplies(Post post) {
 		Set<Sone> sones = getSones();
 		List<Reply> replies = new ArrayList<Reply>();
 		for (Sone sone : sones) {
 			for (Reply reply : sone.getReplies()) {
 				if (reply.getPost().equals(post)) {
 					replies.add(reply);
 				}
 			}
 		}
 		Collections.sort(replies, Reply.TIME_COMPARATOR);
 		return replies;
 	}
 
 	/**
 	 * Returns all Sones that have liked the given post.
 	 *
 	 * @param post
 	 *            The post to get the liking Sones for
 	 * @return The Sones that like the given post
 	 */
 	public Set<Sone> getLikes(Post post) {
 		Set<Sone> sones = new HashSet<Sone>();
 		for (Sone sone : getSones()) {
 			if (sone.getLikedPostIds().contains(post.getId())) {
 				sones.add(sone);
 			}
 		}
 		return sones;
 	}
 
 	/**
 	 * Returns all Sones that have liked the given reply.
 	 *
 	 * @param reply
 	 *            The reply to get the liking Sones for
 	 * @return The Sones that like the given reply
 	 */
 	public Set<Sone> getLikes(Reply reply) {
 		Set<Sone> sones = new HashSet<Sone>();
 		for (Sone sone : getSones()) {
 			if (sone.getLikedReplyIds().contains(reply.getId())) {
 				sones.add(sone);
 			}
 		}
 		return sones;
 	}
 
 	//
 	// ACTIONS
 	//
 
 	/**
 	 * Adds a local Sone from the given ID which has to be the ID of an own
 	 * identity.
 	 *
 	 * @param id
 	 *            The ID of an own identity to add a Sone for
 	 * @return The added (or already existing) Sone
 	 */
 	public Sone addLocalSone(String id) {
 		synchronized (localSones) {
 			if (localSones.containsKey(id)) {
 				logger.log(Level.FINE, "Tried to add known local Sone: %s", id);
 				return localSones.get(id);
 			}
 			OwnIdentity ownIdentity = identityManager.getOwnIdentity(id);
 			if (ownIdentity == null) {
 				logger.log(Level.INFO, "Invalid Sone ID: %s", id);
 				return null;
 			}
 			return addLocalSone(ownIdentity);
 		}
 	}
 
 	/**
 	 * Adds a local Sone from the given own identity.
 	 *
 	 * @param ownIdentity
 	 *            The own identity to create a Sone from
 	 * @return The added (or already existing) Sone
 	 */
 	public Sone addLocalSone(OwnIdentity ownIdentity) {
 		if (ownIdentity == null) {
 			logger.log(Level.WARNING, "Given OwnIdentity is null!");
 			return null;
 		}
 		synchronized (localSones) {
 			final Sone sone;
 			try {
 				sone = getLocalSone(ownIdentity.getId()).setIdentity(ownIdentity).setInsertUri(new FreenetURI(ownIdentity.getInsertUri())).setRequestUri(new FreenetURI(ownIdentity.getRequestUri()));
 				sone.setLatestEdition(Numbers.safeParseLong(ownIdentity.getProperty("Sone.LatestEdition"), (long) 0));
 			} catch (MalformedURLException mue1) {
 				logger.log(Level.SEVERE, "Could not convert the Identity’s URIs to Freenet URIs: " + ownIdentity.getInsertUri() + ", " + ownIdentity.getRequestUri(), mue1);
 				return null;
 			}
 			/* TODO - load posts ’n stuff */
 			localSones.put(ownIdentity.getId(), sone);
 			SoneInserter soneInserter = new SoneInserter(this, freenetInterface, sone);
 			soneInserters.put(sone, soneInserter);
 			soneInserter.start();
 			setSoneStatus(sone, SoneStatus.idle);
 			loadSone(sone);
 			return sone;
 		}
 	}
 
 	/**
 	 * Creates a new Sone for the given own identity.
 	 *
 	 * @param ownIdentity
 	 *            The own identity to create a Sone for
 	 * @return The created Sone
 	 */
 	public Sone createSone(OwnIdentity ownIdentity) {
 		identityManager.addContext(ownIdentity, "Sone");
 		Sone sone = addLocalSone(ownIdentity);
 		synchronized (sone) {
 			/* mark as modified so that it gets inserted immediately. */
 			sone.setModificationCounter(sone.getModificationCounter() + 1);
 		}
 		return sone;
 	}
 
 	/**
 	 * Adds the Sone of the given identity.
 	 *
 	 * @param identity
 	 *            The identity whose Sone to add
 	 * @return The added or already existing Sone
 	 */
 	public Sone addRemoteSone(Identity identity) {
 		if (identity == null) {
 			logger.log(Level.WARNING, "Given Identity is null!");
 			return null;
 		}
 		synchronized (remoteSones) {
 			final Sone sone = getRemoteSone(identity.getId()).setIdentity(identity);
 			sone.setRequestUri(getSoneUri(identity.getRequestUri(), identity.getProperty("Sone.LatestEdition")));
 			remoteSones.put(identity.getId(), sone);
 			soneDownloader.addSone(sone);
 			new Thread(new Runnable() {
 
 				@Override
 				@SuppressWarnings("synthetic-access")
 				public void run() {
 					soneDownloader.fetchSone(sone);
 				}
 
 			}, "Sone Downloader").start();
 			setSoneStatus(sone, SoneStatus.idle);
 			return sone;
 		}
 	}
 
 	/**
 	 * Updates the stores Sone with the given Sone.
 	 *
 	 * @param sone
 	 *            The updated Sone
 	 */
 	public void updateSone(Sone sone) {
 		if (isRemoteSone(sone)) {
 			Sone storedSone = getRemoteSone(sone.getId());
 			if (!(sone.getTime() > storedSone.getTime())) {
 				logger.log(Level.FINE, "Downloaded Sone %s is not newer than stored Sone %s.", new Object[] { sone, storedSone });
 				return;
 			}
 			synchronized (posts) {
 				for (Post post : storedSone.getPosts()) {
 					posts.remove(post.getId());
 				}
 				for (Post post : sone.getPosts()) {
 					posts.put(post.getId(), post);
 				}
 			}
 			synchronized (replies) {
 				for (Reply reply : storedSone.getReplies()) {
 					replies.remove(reply.getId());
 				}
 				for (Reply reply : sone.getReplies()) {
 					replies.put(reply.getId(), reply);
 				}
 			}
 			synchronized (storedSone) {
 				storedSone.setTime(sone.getTime());
 				storedSone.setProfile(sone.getProfile());
 				storedSone.setPosts(sone.getPosts());
 				storedSone.setReplies(sone.getReplies());
 				storedSone.setLikePostIds(sone.getLikedPostIds());
 				storedSone.setLikeReplyIds(sone.getLikedReplyIds());
 				storedSone.setLatestEdition(sone.getRequestUri().getEdition());
 			}
 			saveSone(storedSone);
 		}
 	}
 
 	/**
 	 * Deletes the given Sone. This will remove the Sone from the
 	 * {@link #getLocalSone(String) local Sones}, stops its {@link SoneInserter}
 	 * and remove the context from its identity.
 	 *
 	 * @param sone
 	 *            The Sone to delete
 	 */
 	public void deleteSone(Sone sone) {
 		if (!(sone.getIdentity() instanceof OwnIdentity)) {
 			logger.log(Level.WARNING, "Tried to delete Sone of non-own identity: %s", sone);
 			return;
 		}
 		synchronized (localSones) {
 			if (!localSones.containsKey(sone.getId())) {
 				logger.log(Level.WARNING, "Tried to delete non-local Sone: %s", sone);
 				return;
 			}
 			localSones.remove(sone.getId());
 			soneInserters.remove(sone).stop();
 		}
 		identityManager.removeContext((OwnIdentity) sone.getIdentity(), "Sone");
 		identityManager.removeProperty((OwnIdentity) sone.getIdentity(), "Sone.LatestEdition");
 		try {
 			configuration.getLongValue("Sone/" + sone.getId() + "/Time").setValue(null);
 		} catch (ConfigurationException ce1) {
 			logger.log(Level.WARNING, "Could not remove Sone from configuration!", ce1);
 		}
 	}
 
 	/**
 	 * Loads and updates the given Sone from the configuration. If any error is
 	 * encountered, loading is aborted and the given Sone is not changed.
 	 *
 	 * @param sone
 	 *            The Sone to load and update
 	 */
 	public void loadSone(Sone sone) {
 		if (!isLocalSone(sone)) {
 			logger.log(Level.FINE, "Tried to load non-local Sone: %s", sone);
 			return;
 		}
 
 		/* load Sone. */
 		String sonePrefix = "Sone/" + sone.getId();
 		Long soneTime = configuration.getLongValue(sonePrefix + "/Time").getValue(null);
 		if (soneTime == null) {
			logger.log(Level.INFO, "Could not load Sone because no Sone has been saved.");
 			return;
 		}
 		long soneModificationCounter = configuration.getLongValue(sonePrefix + "/ModificationCounter").getValue((long) 0);
 
 		/* load profile. */
 		Profile profile = new Profile();
 		profile.setFirstName(configuration.getStringValue(sonePrefix + "/Profile/FirstName").getValue(null));
 		profile.setMiddleName(configuration.getStringValue(sonePrefix + "/Profile/MiddleName").getValue(null));
 		profile.setLastName(configuration.getStringValue(sonePrefix + "/Profile/LastName").getValue(null));
 		profile.setBirthDay(configuration.getIntValue(sonePrefix + "/Profile/BirthDay").getValue(null));
 		profile.setBirthMonth(configuration.getIntValue(sonePrefix + "/Profile/BirthMonth").getValue(null));
 		profile.setBirthYear(configuration.getIntValue(sonePrefix + "/Profile/BirthYear").getValue(null));
 
 		/* load posts. */
 		Set<Post> posts = new HashSet<Post>();
 		while (true) {
 			String postPrefix = sonePrefix + "/Posts/" + posts.size();
 			String postId = configuration.getStringValue(postPrefix + "/ID").getValue(null);
 			if (postId == null) {
 				break;
 			}
 			long postTime = configuration.getLongValue(postPrefix + "/Time").getValue((long) 0);
 			String postText = configuration.getStringValue(postPrefix + "/Text").getValue(null);
 			if ((postTime == 0) || (postText == null)) {
 				logger.log(Level.WARNING, "Invalid post found, aborting load!");
 				return;
 			}
 			posts.add(getPost(postId).setSone(sone).setTime(postTime).setText(postText));
 		}
 
 		/* load replies. */
 		Set<Reply> replies = new HashSet<Reply>();
 		while (true) {
 			String replyPrefix = sonePrefix + "/Replies/" + replies.size();
 			String replyId = configuration.getStringValue(replyPrefix + "/ID").getValue(null);
 			if (replyId == null) {
 				break;
 			}
 			String postId = configuration.getStringValue(replyPrefix + "/Post/ID").getValue(null);
 			long replyTime = configuration.getLongValue(replyPrefix + "/Time").getValue((long) 0);
 			String replyText = configuration.getStringValue(replyPrefix + "/Text").getValue(null);
 			if ((postId == null) || (replyTime == 0) || (replyText == null)) {
 				logger.log(Level.WARNING, "Invalid reply found, aborting load!");
 				return;
 			}
 			replies.add(getReply(replyId).setSone(sone).setPost(getPost(postId)).setTime(replyTime).setText(replyText));
 		}
 
 		/* load post likes. */
 		Set<String> likedPostIds = new HashSet<String>();
 		while (true) {
 			String likedPostId = configuration.getStringValue(sonePrefix + "/Likes/Post/" + likedPostIds.size() + "/ID").getValue(null);
 			if (likedPostId == null) {
 				break;
 			}
 			likedPostIds.add(likedPostId);
 		}
 
 		/* load reply likes. */
 		Set<String> likedReplyIds = new HashSet<String>();
 		while (true) {
 			String likedReplyId = configuration.getStringValue(sonePrefix + "/Likes/Reply/" + likedReplyIds.size() + "/ID").getValue(null);
 			if (likedReplyId == null) {
 				break;
 			}
 			likedReplyIds.add(likedReplyId);
 		}
 
 		/* load friends. */
 		Set<Sone> friends = new HashSet<Sone>();
 		while (true) {
 			String friendId = configuration.getStringValue(sonePrefix + "/Friends/" + friends.size() + "/ID").getValue(null);
 			if (friendId == null) {
 				break;
 			}
 			Boolean friendLocal = configuration.getBooleanValue(sonePrefix + "/Friends/" + friends.size() + "/Local").getValue(null);
 			if (friendLocal == null) {
 				logger.log(Level.WARNING, "Invalid friend found, aborting load!");
 				return;
 			}
 			friends.add(friendLocal ? getLocalSone(friendId) : getRemoteSone(friendId));
 		}
 
 		/* if we’re still here, Sone was loaded successfully. */
 		synchronized (sone) {
 			sone.setTime(soneTime);
 			sone.setProfile(profile);
 			sone.setPosts(posts);
 			sone.setReplies(replies);
 			sone.setLikePostIds(likedPostIds);
 			sone.setLikeReplyIds(likedReplyIds);
 			sone.setFriends(friends);
 			sone.setModificationCounter(soneModificationCounter);
 		}
 	}
 
 	/**
 	 * Saves the given Sone. This will persist all local settings for the given
 	 * Sone, such as the friends list and similar, private options.
 	 *
 	 * @param sone
 	 *            The Sone to save
 	 */
 	public void saveSone(Sone sone) {
 		if (!isLocalSone(sone)) {
 			logger.log(Level.FINE, "Tried to save non-local Sone: %s", sone);
 			return;
 		}
 		if (!(sone.getIdentity() instanceof OwnIdentity)) {
 			logger.log(Level.WARNING, "Local Sone without OwnIdentity found, refusing to save: %s", sone);
 			return;
 		}
 
 		logger.log(Level.INFO, "Saving Sone: %s", sone);
 		identityManager.setProperty((OwnIdentity) sone.getIdentity(), "Sone.LatestEdition", String.valueOf(sone.getLatestEdition()));
 		try {
 			/* save Sone into configuration. */
 			String sonePrefix = "Sone/" + sone.getId();
 			configuration.getLongValue(sonePrefix + "/Time").setValue(sone.getTime());
 			configuration.getLongValue(sonePrefix + "/ModificationCounter").setValue(sone.getModificationCounter());
 
 			/* save profile. */
 			Profile profile = sone.getProfile();
 			configuration.getStringValue(sonePrefix + "/Profile/FirstName").setValue(profile.getFirstName());
 			configuration.getStringValue(sonePrefix + "/Profile/MiddleName").setValue(profile.getMiddleName());
 			configuration.getStringValue(sonePrefix + "/Profile/LastName").setValue(profile.getLastName());
 			configuration.getIntValue(sonePrefix + "/Profile/BirthDay").setValue(profile.getBirthDay());
 			configuration.getIntValue(sonePrefix + "/Profile/BirthMonth").setValue(profile.getBirthMonth());
 			configuration.getIntValue(sonePrefix + "/Profile/BirthYear").setValue(profile.getBirthYear());
 
 			/* save posts. */
 			int postCounter = 0;
 			for (Post post : sone.getPosts()) {
 				String postPrefix = sonePrefix + "/Posts/" + postCounter++;
 				configuration.getStringValue(postPrefix + "/ID").setValue(post.getId());
 				configuration.getLongValue(postPrefix + "/Time").setValue(post.getTime());
 				configuration.getStringValue(postPrefix + "/Text").setValue(post.getText());
 			}
 			configuration.getStringValue(sonePrefix + "/Posts/" + postCounter + "/ID").setValue(null);
 
 			/* save replies. */
 			int replyCounter = 0;
 			for (Reply reply : sone.getReplies()) {
 				String replyPrefix = sonePrefix + "/Replies/" + replyCounter++;
 				configuration.getStringValue(replyPrefix + "/ID").setValue(reply.getId());
 				configuration.getStringValue(replyPrefix + "/Post/ID").setValue(reply.getPost().getId());
 				configuration.getLongValue(replyPrefix + "/Time").setValue(reply.getTime());
 				configuration.getStringValue(replyPrefix + "/Text").setValue(reply.getText());
 			}
 			configuration.getStringValue(sonePrefix + "/Replies/" + replyCounter + "/ID").setValue(null);
 
 			/* save post likes. */
 			int postLikeCounter = 0;
 			for (String postId : sone.getLikedPostIds()) {
 				configuration.getStringValue(sonePrefix + "/Likes/Post/" + postLikeCounter++ + "/ID").setValue(postId);
 			}
 			configuration.getStringValue(sonePrefix + "/Likes/Post/" + postLikeCounter + "/ID").setValue(null);
 
 			/* save reply likes. */
 			int replyLikeCounter = 0;
 			for (String replyId : sone.getLikedReplyIds()) {
 				configuration.getStringValue(sonePrefix + "/Likes/Reply/" + replyLikeCounter++ + "/ID").setValue(replyId);
 			}
 			configuration.getStringValue(sonePrefix + "/Likes/Reply/" + replyLikeCounter + "/ID").setValue(null);
 
 			/* save friends. */
 			int friendCounter = 0;
 			for (Sone friend : sone.getFriends()) {
 				configuration.getStringValue(sonePrefix + "/Friends/" + friendCounter + "/ID").setValue(friend.getId());
 				configuration.getBooleanValue(sonePrefix + "/Friends/" + friendCounter++ + "/Local").setValue(friend.getInsertUri() != null);
 			}
 			configuration.getStringValue(sonePrefix + "/Friends/" + friendCounter + "/ID").setValue(null);
 
 			logger.log(Level.INFO, "Sone %s saved.", sone);
 		} catch (ConfigurationException ce1) {
 			logger.log(Level.WARNING, "Could not save Sone: " + sone, ce1);
 		}
 	}
 
 	/**
 	 * Creates a new post.
 	 *
 	 * @param sone
 	 *            The Sone that creates the post
 	 * @param text
 	 *            The text of the post
 	 */
 	public void createPost(Sone sone, String text) {
 		createPost(sone, System.currentTimeMillis(), text);
 	}
 
 	/**
 	 * Creates a new post.
 	 *
 	 * @param sone
 	 *            The Sone that creates the post
 	 * @param time
 	 *            The time of the post
 	 * @param text
 	 *            The text of the post
 	 */
 	public void createPost(Sone sone, long time, String text) {
 		if (!isLocalSone(sone)) {
 			logger.log(Level.FINE, "Tried to create post for non-local Sone: %s", sone);
 			return;
 		}
 		Post post = new Post(sone, time, text);
 		synchronized (posts) {
 			posts.put(post.getId(), post);
 		}
 		sone.addPost(post);
 		saveSone(sone);
 	}
 
 	/**
 	 * Deletes the given post.
 	 *
 	 * @param post
 	 *            The post to delete
 	 */
 	public void deletePost(Post post) {
 		if (!isLocalSone(post.getSone())) {
 			logger.log(Level.WARNING, "Tried to delete post of non-local Sone: %s", post.getSone());
 			return;
 		}
 		post.getSone().removePost(post);
 		synchronized (posts) {
 			posts.remove(post.getId());
 		}
 		saveSone(post.getSone());
 	}
 
 	/**
 	 * Creates a new reply.
 	 *
 	 * @param sone
 	 *            The Sone that creates the reply
 	 * @param post
 	 *            The post that this reply refers to
 	 * @param text
 	 *            The text of the reply
 	 */
 	public void createReply(Sone sone, Post post, String text) {
 		createReply(sone, post, System.currentTimeMillis(), text);
 	}
 
 	/**
 	 * Creates a new reply.
 	 *
 	 * @param sone
 	 *            The Sone that creates the reply
 	 * @param post
 	 *            The post that this reply refers to
 	 * @param time
 	 *            The time of the reply
 	 * @param text
 	 *            The text of the reply
 	 */
 	public void createReply(Sone sone, Post post, long time, String text) {
 		if (!isLocalSone(sone)) {
 			logger.log(Level.FINE, "Tried to create reply for non-local Sone: %s", sone);
 			return;
 		}
 		Reply reply = new Reply(sone, post, System.currentTimeMillis(), text);
 		synchronized (replies) {
 			replies.put(reply.getId(), reply);
 		}
 		sone.addReply(reply);
 		saveSone(sone);
 	}
 
 	/**
 	 * Deletes the given reply.
 	 *
 	 * @param reply
 	 *            The reply to delete
 	 */
 	public void deleteReply(Reply reply) {
 		Sone sone = reply.getSone();
 		if (!isLocalSone(sone)) {
 			logger.log(Level.FINE, "Tried to delete non-local reply: %s", reply);
 			return;
 		}
 		synchronized (replies) {
 			replies.remove(reply.getId());
 		}
 		sone.removeReply(reply);
 		saveSone(sone);
 	}
 
 	/**
 	 * Starts the core.
 	 */
 	public void start() {
 		loadConfiguration();
 	}
 
 	/**
 	 * Stops the core.
 	 */
 	public void stop() {
 		synchronized (localSones) {
 			for (SoneInserter soneInserter : soneInserters.values()) {
 				soneInserter.stop();
 			}
 		}
 		saveConfiguration();
 	}
 
 	//
 	// PRIVATE METHODS
 	//
 
 	/**
 	 * Loads the configuration.
 	 */
 	@SuppressWarnings("unchecked")
 	private void loadConfiguration() {
 		/* create options. */
 		options.addIntegerOption("InsertionDelay", new DefaultOption<Integer>(60, new OptionWatcher<Integer>() {
 
 			@Override
 			public void optionChanged(Option<Integer> option, Integer oldValue, Integer newValue) {
 				SoneInserter.setInsertionDelay(newValue);
 			}
 
 		}));
 		options.addBooleanOption("ClearOnNextRestart", new DefaultOption<Boolean>(false));
 		options.addBooleanOption("ReallyClearOnNextRestart", new DefaultOption<Boolean>(false));
 
 		/* read options from configuration. */
 		options.getBooleanOption("ClearOnNextRestart").set(configuration.getBooleanValue("Option/ClearOnNextRestart").getValue(null));
 		options.getBooleanOption("ReallyClearOnNextRestart").set(configuration.getBooleanValue("Option/ReallyClearOnNextRestart").getValue(null));
 		boolean clearConfiguration = options.getBooleanOption("ClearOnNextRestart").get() && options.getBooleanOption("ReallyClearOnNextRestart").get();
 		options.getBooleanOption("ClearOnNextRestart").set(null);
 		options.getBooleanOption("ReallyClearOnNextRestart").set(null);
 		if (clearConfiguration) {
 			/* stop loading the configuration. */
 			return;
 		}
 
 		options.getIntegerOption("InsertionDelay").set(configuration.getIntValue("Option/InsertionDelay").getValue(null));
 
 	}
 
 	/**
 	 * Saves the current options.
 	 */
 	private void saveConfiguration() {
 		/* store the options first. */
 		try {
 			configuration.getIntValue("Option/InsertionDelay").setValue(options.getIntegerOption("InsertionDelay").getReal());
 			configuration.getBooleanValue("Option/ClearOnNextRestart").setValue(options.getBooleanOption("ClearOnNextRestart").getReal());
 			configuration.getBooleanValue("Option/ReallyClearOnNextRestart").setValue(options.getBooleanOption("ReallyClearOnNextRestart").getReal());
 		} catch (ConfigurationException ce1) {
 			logger.log(Level.SEVERE, "Could not store configuration!", ce1);
 		}
 	}
 
 	/**
 	 * Generate a Sone URI from the given URI and latest edition.
 	 *
 	 * @param uriString
 	 *            The URI to derive the Sone URI from
 	 * @param latestEditionString
 	 *            The latest edition as a {@link String}, or {@code null}
 	 * @return The derived URI
 	 */
 	private FreenetURI getSoneUri(String uriString, String latestEditionString) {
 		try {
 			FreenetURI uri = new FreenetURI(uriString).setDocName("Sone").setMetaString(new String[0]).setSuggestedEdition(Numbers.safeParseLong(latestEditionString, (long) 0));
 			return uri;
 		} catch (MalformedURLException mue1) {
 			logger.log(Level.WARNING, "Could not create Sone URI from URI: " + uriString, mue1);
 			return null;
 		}
 	}
 
 	//
 	// INTERFACE IdentityListener
 	//
 
 	/**
 	 * {@inheritDoc}
 	 */
 	@Override
 	public void ownIdentityAdded(OwnIdentity ownIdentity) {
 		logger.log(Level.FINEST, "Adding OwnIdentity: " + ownIdentity);
 		if (ownIdentity.hasContext("Sone")) {
 			addLocalSone(ownIdentity);
 		}
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	@Override
 	public void ownIdentityRemoved(OwnIdentity ownIdentity) {
 		/* TODO */
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	@Override
 	public void identityAdded(Identity identity) {
 		logger.log(Level.FINEST, "Adding Identity: " + identity);
 		addRemoteSone(identity);
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	@Override
 	public void identityUpdated(Identity identity) {
 		/* TODO */
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	@Override
 	public void identityRemoved(Identity identity) {
 		/* TODO */
 	}
 
 }
