 /*
  * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
  * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
  * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
  *
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or
  * without modification, are permitted provided that the following
  * conditions are met:
  *
  * - Redistributions of source code must retain the above copyright
  *   notice, this list of conditions and the following disclaimer.
  *
  * - Redistributions in binary form must reproduce the above
  *   copyright notice, this list of conditions and the following
  *   disclaimer in the documentation and/or other materials provided
  *   with the distribution.
  *
  * - Neither the name of the Git Development Community nor the
  *   names of its contributors may be used to endorse or promote
  *   products derived from this software without specific prior
  *   written permission.
  *
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
  * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
  * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
  * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
  * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
  * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
  * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
  * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
  * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
  * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
  * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
  * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
  * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  */
 
 package org.spearce.jgit.lib;
 
 import java.io.BufferedReader;
 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.FileReader;
 import java.io.FilenameFilter;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 import java.util.Vector;
 
 import org.spearce.jgit.errors.IncorrectObjectTypeException;
 import org.spearce.jgit.errors.RevisionSyntaxException;
 import org.spearce.jgit.stgit.StGitPatch;
 import org.spearce.jgit.util.FS;
 
 /**
  * Represents a Git repository. A repository holds all objects and refs used for
  * managing source code (could by any type of file, but source code is what
  * SCM's are typically used for).
  *
  * In Git terms all data is stored in GIT_DIR, typically a directory called
  * .git. A work tree is maintained unless the repository is a bare repository.
  * Typically the .git directory is located at the root of the work dir.
  *
  * <ul>
  * <li>GIT_DIR
  * 	<ul>
  * 		<li>objects/ - objects</li>
  * 		<li>refs/ - tags and heads</li>
  * 		<li>config - configuration</li>
  * 		<li>info/ - more configurations</li>
  * 	</ul>
  * </li>
  * </ul>
  *
  * This implementation only handles a subtly undocumented subset of git features.
  *
  */
 public class Repository {
 	private final File gitDir;
 
 	private final File[] objectsDirs;
 
 	private final RepositoryConfig config;
 
 	private final RefDatabase refs;
 
 	private PackFile[] packs;
 
 	private GitIndex index;
 
 	private List<RepositoryListener> listeners = new Vector<RepositoryListener>(); // thread safe
 	static private List<RepositoryListener> allListeners = new Vector<RepositoryListener>(); // thread safe
 
 	/**
 	 * Construct a representation of a Git repository.
 	 * 
 	 * @param d
 	 *            GIT_DIR (the location of the repository metadata).
 	 * @throws IOException
 	 *             the repository appears to already exist but cannot be
 	 *             accessed.
 	 */
 	public Repository(final File d) throws IOException {
 		gitDir = d.getAbsoluteFile();
 		try {
 			objectsDirs = readObjectsDirs(FS.resolve(gitDir, "objects"),
 					new ArrayList<File>()).toArray(new File[0]);
 		} catch (IOException e) {
 			IOException ex = new IOException("Cannot find all object dirs for " + gitDir);
 			ex.initCause(e);
 			throw ex;
 		}
 		refs = new RefDatabase(this);
 		packs = new PackFile[0];
 		config = new RepositoryConfig(this);
 
 		final boolean isExisting = objectsDirs[0].exists();
 		if (isExisting) {
 			getConfig().load();
 			final String repositoryFormatVersion = getConfig().getString(
 					"core", null, "repositoryFormatVersion");
 			if (!"0".equals(repositoryFormatVersion)) {
 				throw new IOException("Unknown repository format \""
 						+ repositoryFormatVersion + "\"; expected \"0\".");
 			}
 		} else {
 			getConfig().create();
 		}
 		if (isExisting)
 			scanForPacks();
 	}
 
 	private Collection<File> readObjectsDirs(File objectsDir, Collection<File> ret) throws IOException {
 		ret.add(objectsDir);
 		final File altFile = FS.resolve(objectsDir, "info/alternates");
 		if (altFile.exists()) {
 			BufferedReader ar = new BufferedReader(new FileReader(altFile));
 			for (String alt=ar.readLine(); alt!=null; alt=ar.readLine()) {
 				readObjectsDirs(FS.resolve(objectsDir, alt), ret);
 			}
 			ar.close();
 		}
 		return ret;
 	}
 
 	/**
 	 * Create a new Git repository initializing the necessary files and
 	 * directories.
 	 *
 	 * @throws IOException
 	 */
 	public void create() throws IOException {
 		if (gitDir.exists()) {
 			throw new IllegalStateException("Repository already exists: "
 					+ gitDir);
 		}
 
 		gitDir.mkdirs();
 		refs.create();
 
 		objectsDirs[0].mkdirs();
 		new File(objectsDirs[0], "pack").mkdir();
 		new File(objectsDirs[0], "info").mkdir();
 
 		new File(gitDir, "branches").mkdir();
 		new File(gitDir, "remotes").mkdir();
 		final String master = Constants.R_HEADS + Constants.MASTER;
 		refs.link(Constants.HEAD, master);
 
 		getConfig().create();
 		getConfig().save();
 	}
 
 	/**
 	 * @return GIT_DIR
 	 */
 	public File getDirectory() {
 		return gitDir;
 	}
 
 	/**
 	 * @return the directory containing the objects owned by this repository.
 	 */
 	public File getObjectsDirectory() {
 		return objectsDirs[0];
 	}
 
 	/**
 	 * @return the configuration of this repository
 	 */
 	public RepositoryConfig getConfig() {
 		return config;
 	}
 
 	/**
 	 * Construct a filename where the loose object having a specified SHA-1
 	 * should be stored. If the object is stored in a shared repository the path
 	 * to the alternative repo will be returned. If the object is not yet store
 	 * a usable path in this repo will be returned. It is assumed that callers
 	 * will look for objects in a pack first.
 	 *
 	 * @param objectId
 	 * @return suggested file name
 	 */
 	public File toFile(final AnyObjectId objectId) {
 		final String n = objectId.name();
 		String d=n.substring(0, 2);
 		String f=n.substring(2);
 		for (int i=0; i<objectsDirs.length; ++i) {
 			File ret = new File(new File(objectsDirs[i], d), f);
 			if (ret.exists())
 				return ret;
 		}
 		return new File(new File(objectsDirs[0], d), f);
 	}
 
 	/**
 	 * @param objectId
 	 * @return true if the specified object is stored in this repo or any of the
 	 *         known shared repositories.
 	 */
 	public boolean hasObject(final AnyObjectId objectId) {
 		int k = packs.length;
 		if (k > 0) {
 			do {
 				if (packs[--k].hasObject(objectId))
 					return true;
 			} while (k > 0);
 		}
 		return toFile(objectId).isFile();
 	}
 
 	/**
 	 * @param id
 	 *            SHA-1 of an object.
 	 * 
 	 * @return a {@link ObjectLoader} for accessing the data of the named
 	 *         object, or null if the object does not exist.
 	 * @throws IOException
 	 */
 	public ObjectLoader openObject(final AnyObjectId id)
 			throws IOException {
 		return openObject(new WindowCursor(),id);
 	}
 
 	/**
 	 * @param curs
 	 *            temporary working space associated with the calling thread.
 	 * @param id
 	 *            SHA-1 of an object.
 	 * 
 	 * @return a {@link ObjectLoader} for accessing the data of the named
 	 *         object, or null if the object does not exist.
 	 * @throws IOException
 	 */
 	public ObjectLoader openObject(final WindowCursor curs, final AnyObjectId id)
 			throws IOException {
 		int k = packs.length;
 		if (k > 0) {
 			do {
 				try {
 					final ObjectLoader ol = packs[--k].get(curs, id);
 					if (ol != null)
 						return ol;
 				} catch (IOException ioe) {
 					// This shouldn't happen unless the pack was corrupted
 					// after we opened it or the VM runs out of memory. This is
 					// a know problem with memory mapped I/O in java and have
 					// been noticed with JDK < 1.6. Tell the gc that now is a good
 					// time to collect and try once more.
 					try {
 						curs.release();
 						System.gc();
 						final ObjectLoader ol = packs[k].get(curs, id);
 						if (ol != null)
 							return ol;
 					} catch (IOException ioe2) {
 						ioe2.printStackTrace();
 						ioe.printStackTrace();
 						// Still fails.. that's BAD, maybe the pack has
 						// been corrupted after all, or the gc didn't manage
 						// to release enough previously mmaped areas.
 					}
 				}
 			} while (k > 0);
 		}
 		try {
 			return new UnpackedObjectLoader(this, id.toObjectId());
 		} catch (FileNotFoundException fnfe) {
 			return null;
 		}
 	}
 
 	/**
 	 * Open object in all packs containing specified object.
 	 *
 	 * @param objectId
 	 *            id of object to search for
 	 * @param curs
 	 *            temporary working space associated with the calling thread.
 	 * @return collection of loaders for this object, from all packs containing
 	 *         this object
 	 * @throws IOException
 	 */
 	public Collection<PackedObjectLoader> openObjectInAllPacks(
 			final AnyObjectId objectId, final WindowCursor curs)
 			throws IOException {
 		Collection<PackedObjectLoader> result = new LinkedList<PackedObjectLoader>();
 		openObjectInAllPacks(objectId, result, curs);
 		return result;
 	}
 
 	/**
 	 * Open object in all packs containing specified object.
 	 *
 	 * @param objectId
 	 *            id of object to search for
 	 * @param resultLoaders
 	 *            result collection of loaders for this object, filled with
 	 *            loaders from all packs containing specified object
 	 * @param curs
 	 *            temporary working space associated with the calling thread.
 	 * @throws IOException
 	 */
 	void openObjectInAllPacks(final AnyObjectId objectId,
 			final Collection<PackedObjectLoader> resultLoaders,
 			final WindowCursor curs) throws IOException {
 		for (PackFile pack : packs) {
 			final PackedObjectLoader loader = pack.get(curs, objectId);
 			if (loader != null)
 				resultLoaders.add(loader);
 		}
 	}
 
 	/**
 	 * @param id
 	 *            SHA'1 of a blob
 	 * @return an {@link ObjectLoader} for accessing the data of a named blob
 	 * @throws IOException
 	 */
 	public ObjectLoader openBlob(final ObjectId id) throws IOException {
 		return openObject(id);
 	}
 
 	/**
 	 * @param id
 	 *            SHA'1 of a tree
 	 * @return an {@link ObjectLoader} for accessing the data of a named tree
 	 * @throws IOException
 	 */
 	public ObjectLoader openTree(final ObjectId id) throws IOException {
 		return openObject(id);
 	}
 
 	/**
 	 * Access a Commit object using a symbolic reference. This reference may
 	 * be a SHA-1 or ref in combination with a number of symbols translating
 	 * from one ref or SHA1-1 to another, such as HEAD^ etc.
 	 *
 	 * @param revstr a reference to a git commit object
 	 * @return a Commit named by the specified string
 	 * @throws IOException for I/O error or unexpected object type.
 	 *
 	 * @see #resolve(String)
 	 */
 	public Commit mapCommit(final String revstr) throws IOException {
 		final ObjectId id = resolve(revstr);
 		return id != null ? mapCommit(id) : null;
 	}
 
 	/**
 	 * Access any type of Git object by id and
 	 *
 	 * @param id
 	 *            SHA-1 of object to read
 	 * @param refName optional, only relevant for simple tags
 	 * @return The Git object if found or null
 	 * @throws IOException
 	 */
 	public Object mapObject(final ObjectId id, final String refName) throws IOException {
 		final ObjectLoader or = openObject(id);
 		if (or == null)
 			return null;
 		final byte[] raw = or.getBytes();
 		if (or.getType() == Constants.OBJ_TREE)
 			return makeTree(id, raw);
 		if (or.getType() == Constants.OBJ_COMMIT)
 			return makeCommit(id, raw);
 		if (or.getType() == Constants.OBJ_TAG)
 			return makeTag(id, refName, raw);
 		if (or.getType() == Constants.OBJ_BLOB)
 			return raw;
 		throw new IncorrectObjectTypeException(id,
 				"COMMIT nor TREE nor BLOB nor TAG");
 	}
 
 	/**
 	 * Access a Commit by SHA'1 id.
 	 * @param id
 	 * @return Commit or null
 	 * @throws IOException for I/O error or unexpected object type.
 	 */
 	public Commit mapCommit(final ObjectId id) throws IOException {
 		final ObjectLoader or = openObject(id);
 		if (or == null)
 			return null;
 		final byte[] raw = or.getBytes();
 		if (Constants.OBJ_COMMIT == or.getType())
 			return new Commit(this, id, raw);
 		throw new IncorrectObjectTypeException(id, Constants.TYPE_COMMIT);
 	}
 
 	private Commit makeCommit(final ObjectId id, final byte[] raw) {
 		Commit ret = new Commit(this, id, raw);
 		return ret;
 	}
 
 	/**
 	 * Access a Tree object using a symbolic reference. This reference may
 	 * be a SHA-1 or ref in combination with a number of symbols translating
 	 * from one ref or SHA1-1 to another, such as HEAD^{tree} etc.
 	 *
 	 * @param revstr a reference to a git commit object
 	 * @return a Tree named by the specified string
 	 * @throws IOException
 	 *
 	 * @see #resolve(String)
 	 */
 	public Tree mapTree(final String revstr) throws IOException {
 		final ObjectId id = resolve(revstr);
 		return id != null ? mapTree(id) : null;
 	}
 
 	/**
 	 * Access a Tree by SHA'1 id.
 	 * @param id
 	 * @return Tree or null
 	 * @throws IOException for I/O error or unexpected object type.
 	 */
 	public Tree mapTree(final ObjectId id) throws IOException {
 		final ObjectLoader or = openObject(id);
 		if (or == null)
 			return null;
 		final byte[] raw = or.getBytes();
 		if (Constants.OBJ_TREE == or.getType()) {
 			return new Tree(this, id, raw);
 		}
 		if (Constants.OBJ_COMMIT == or.getType())
 			return mapTree(ObjectId.fromString(raw, 5));
 		throw new IncorrectObjectTypeException(id, Constants.TYPE_TREE);
 	}
 
 	private Tree makeTree(final ObjectId id, final byte[] raw) throws IOException {
 		Tree ret = new Tree(this, id, raw);
 		return ret;
 	}
 
 	private Tag makeTag(final ObjectId id, final String refName, final byte[] raw) {
 		Tag ret = new Tag(this, id, refName, raw);
 		return ret;
 	}
 
 	/**
 	 * Access a tag by symbolic name.
 	 *
 	 * @param revstr
 	 * @return a Tag or null
 	 * @throws IOException on I/O error or unexpected type
 	 */
 	public Tag mapTag(String revstr) throws IOException {
 		final ObjectId id = resolve(revstr);
 		return id != null ? mapTag(revstr, id) : null;
 	}
 
 	/**
 	 * Access a Tag by SHA'1 id
 	 * @param refName
 	 * @param id
 	 * @return Commit or null
 	 * @throws IOException for I/O error or unexpected object type.
 	 */
 	public Tag mapTag(final String refName, final ObjectId id) throws IOException {
 		final ObjectLoader or = openObject(id);
 		if (or == null)
 			return null;
 		final byte[] raw = or.getBytes();
 		if (Constants.OBJ_TAG == or.getType())
 			return new Tag(this, id, refName, raw);
 		return new Tag(this, id, refName, null);
 	}
 
 	/**
 	 * Create a command to update, create or delete a ref in this repository.
 	 * 
 	 * @param ref
 	 *            name of the ref the caller wants to modify.
 	 * @return an update command. The caller must finish populating this command
 	 *         and then invoke one of the update methods to actually make a
 	 *         change.
 	 * @throws IOException
 	 *             a symbolic ref was passed in and could not be resolved back
 	 *             to the base ref, as the symbolic ref could not be read.
 	 */
 	public RefUpdate updateRef(final String ref) throws IOException {
 		return refs.newUpdate(ref);
 	}
 
 	/**
 	 * Parse a git revision string and return an object id.
 	 *
 	 * Currently supported is combinations of these.
 	 * <ul>
 	 *  <li>SHA-1 - a SHA-1</li>
 	 *  <li>refs/... - a ref name</li>
 	 *  <li>ref^n - nth parent reference</li>
 	 *  <li>ref~n - distance via parent reference</li>
 	 *  <li>ref@{n} - nth version of ref</li>
 	 *  <li>ref^{tree} - tree references by ref</li>
 	 *  <li>ref^{commit} - commit references by ref</li>
 	 * </ul>
 	 *
 	 * Not supported is
 	 * <ul>
 	 * <li>timestamps in reflogs, ref@{full or relative timestamp}</li>
 	 * <li>abbreviated SHA-1's</li>
 	 * </ul>
 	 *
 	 * @param revstr A git object references expression
 	 * @return an ObjectId or null if revstr can't be resolved to any ObjectId
 	 * @throws IOException on serious errors
 	 */
 	public ObjectId resolve(final String revstr) throws IOException {
 		char[] rev = revstr.toCharArray();
 		Object ref = null;
 		ObjectId refId = null;
 		for (int i = 0; i < rev.length; ++i) {
 			switch (rev[i]) {
 			case '^':
 				if (refId == null) {
 					String refstr = new String(rev,0,i);
 					refId = resolveSimple(refstr);
 					if (refId == null)
 						return null;
 				}
 				if (i + 1 < rev.length) {
 					switch (rev[i + 1]) {
 					case '0':
 					case '1':
 					case '2':
 					case '3':
 					case '4':
 					case '5':
 					case '6':
 					case '7':
 					case '8':
 					case '9':
 						int j;
 						ref = mapObject(refId, null);
 						if (!(ref instanceof Commit))
 							throw new IncorrectObjectTypeException(refId, Constants.TYPE_COMMIT);
 						for (j=i+1; j<rev.length; ++j) {
 							if (!Character.isDigit(rev[j]))
 								break;
 						}
 						String parentnum = new String(rev, i+1, j-i-1);
 						int pnum;
 						try {
 							pnum = Integer.parseInt(parentnum);
 						} catch (NumberFormatException e) {
 							throw new RevisionSyntaxException(
 									"Invalid commit parent number",
 									revstr);
 						}
 						if (pnum != 0) {
 							final ObjectId parents[] = ((Commit) ref)
 									.getParentIds();
 							if (pnum > parents.length)
 								refId = null;
 							else
 								refId = parents[pnum - 1];
 						}
 						i = j - 1;
 						break;
 					case '{':
 						int k;
 						String item = null;
 						for (k=i+2; k<rev.length; ++k) {
 							if (rev[k] == '}') {
 								item = new String(rev, i+2, k-i-2);
 								break;
 							}
 						}
 						i = k;
 						if (item != null)
 							if (item.equals("tree")) {
 								ref = mapObject(refId, null);
 								while (ref instanceof Tag) {
 									Tag t = (Tag)ref;
 									refId = t.getObjId();
 									ref = mapObject(refId, null);
 								}
 								if (ref instanceof Treeish)
 									refId = ((Treeish)ref).getTreeId();
 								else
 									throw new IncorrectObjectTypeException(refId,  Constants.TYPE_TREE);
 							}
 							else if (item.equals("commit")) {
 								ref = mapObject(refId, null);
 								while (ref instanceof Tag) {
 									Tag t = (Tag)ref;
 									refId = t.getObjId();
 									ref = mapObject(refId, null);
 								}
 								if (!(ref instanceof Commit))
 									throw new IncorrectObjectTypeException(refId,  Constants.TYPE_COMMIT);
 							}
 							else if (item.equals("blob")) {
 								ref = mapObject(refId, null);
 								while (ref instanceof Tag) {
 									Tag t = (Tag)ref;
 									refId = t.getObjId();
 									ref = mapObject(refId, null);
 								}
 								if (!(ref instanceof byte[]))
 									throw new IncorrectObjectTypeException(refId,  Constants.TYPE_BLOB);
 							}
 							else if (item.equals("")) {
 								ref = mapObject(refId, null);
 								if (ref instanceof Tag)
 									refId = ((Tag)ref).getObjId();
 								else {
 									// self
 								}
 							}
 							else
 								throw new RevisionSyntaxException(revstr);
 						else
 							throw new RevisionSyntaxException(revstr);
 						break;
 					default:
 						ref = mapObject(refId, null);
 						if (ref instanceof Commit) {
 							final ObjectId parents[] = ((Commit) ref)
 									.getParentIds();
 							if (parents.length == 0)
 								refId = null;
 							else
 								refId = parents[0];
 						} else
 							throw new IncorrectObjectTypeException(refId,  Constants.TYPE_COMMIT);
 						
 					}
 				} else {
 					ref = mapObject(refId, null);
 					if (ref instanceof Commit) {
 						final ObjectId parents[] = ((Commit) ref)
 								.getParentIds();
 						if (parents.length == 0)
 							refId = null;
 						else
 							refId = parents[0];
 					} else
 						throw new IncorrectObjectTypeException(refId,  Constants.TYPE_COMMIT);
 				}
 				break;
 			case '~':
 				if (ref == null) {
 					String refstr = new String(rev,0,i);
 					refId = resolveSimple(refstr);
 					ref = mapCommit(refId);
 				}
 				int l;
 				for (l = i + 1; l < rev.length; ++l) {
 					if (!Character.isDigit(rev[l]))
 						break;
 				}
 				String distnum = new String(rev, i+1, l-i-1);
 				int dist;
 				try {
 					dist = Integer.parseInt(distnum);
 				} catch (NumberFormatException e) {
 					throw new RevisionSyntaxException(
 							"Invalid ancestry length", revstr);
 				}
				while (dist >= 0) {
 					final ObjectId[] parents = ((Commit) ref).getParentIds();
 					if (parents.length == 0) {
 						refId = null;
 						break;
 					}
 					refId = parents[0];
 					ref = mapCommit(refId);
 					--dist;
 				}
 				i = l - 1;
 				break;
 			case '@':
 				int m;
 				String time = null;
 				for (m=i+2; m<rev.length; ++m) {
 					if (rev[m] == '}') {
 						time = new String(rev, i+2, m-i-2);
 						break;
 					}
 				}
 				if (time != null)
 					throw new RevisionSyntaxException("reflogs not yet supported by revision parser yet", revstr);
 				i = m - 1;
 				break;
 			default:
 				if (refId != null)
 					throw new RevisionSyntaxException(revstr);
 			}
 		}
 		if (refId == null)
 			refId = resolveSimple(revstr);
 		return refId;
 	}
 
 	private ObjectId resolveSimple(final String revstr) throws IOException {
 		if (ObjectId.isId(revstr))
 			return ObjectId.fromString(revstr);
 		final Ref r = refs.readRef(revstr);
 		return r != null ? r.getObjectId() : null;
 	}
 
 	/**
 	 * Close all resources used by this repository
 	 */
 	public void close() {
 		closePacks();
 	}
 
 	void closePacks() {
 		for (int k = packs.length - 1; k >= 0; k--) {
 			packs[k].close();
 		}
 		packs = new PackFile[0];
 	}
 
 	/**
 	 * Add a single existing pack to the list of available pack files.
 	 * 
 	 * @param pack
 	 *            path of the pack file to open.
 	 * @param idx
 	 *            path of the corresponding index file.
 	 * @throws IOException
 	 *             index file could not be opened, read, or is not recognized as
 	 *             a Git pack file index.
 	 */
 	public void openPack(final File pack, final File idx) throws IOException {
 		final String p = pack.getName();
 		final String i = idx.getName();
 		if (p.length() != 50 || !p.startsWith("pack-") || !p.endsWith(".pack"))
 		    throw new IllegalArgumentException("Not a valid pack " + pack);
 		if (i.length() != 49 || !i.startsWith("pack-") || !i.endsWith(".idx"))
 		    throw new IllegalArgumentException("Not a valid pack " + idx);
 		if (!p.substring(0,45).equals(i.substring(0,45)))
 			throw new IllegalArgumentException("Pack " + pack
 					+ "does not match index " + idx);
 
 		final PackFile[] cur = packs;
 		final PackFile[] arr = new PackFile[cur.length + 1];
 		System.arraycopy(cur, 0, arr, 1, cur.length);
 		arr[0] = new PackFile(this, idx, pack);
 		packs = arr;
 	}
 
 	/**
 	 * Scan the object dirs, including alternates for packs
 	 * to use.
 	 */
 	public void scanForPacks() {
 		final ArrayList<PackFile> p = new ArrayList<PackFile>();
 		for (int i=0; i<objectsDirs.length; ++i)
 			scanForPacks(new File(objectsDirs[i], "pack"), p);
 		final PackFile[] arr = new PackFile[p.size()];
 		p.toArray(arr);
 		packs = arr;
 	}
 
 	private void scanForPacks(final File packDir, Collection<PackFile> packList) {
 		final String[] idxList = packDir.list(new FilenameFilter() {
 			public boolean accept(final File baseDir, final String n) {
 				// Must match "pack-[0-9a-f]{40}.idx" to be an index.
 				return n.length() == 49 && n.endsWith(".idx")
 						&& n.startsWith("pack-");
 			}
 		});
 		if (idxList != null) {
 			for (final String indexName : idxList) {
 				final String n = indexName.substring(0, indexName.length() - 4);
 				final File idxFile = new File(packDir, n + ".idx");
 				final File packFile = new File(packDir, n + ".pack");
 
 				if (!packFile.isFile()) {
 					// Sometimes C Git's http fetch transport leaves a
 					// .idx file behind and does not download the .pack.
 					// We have to skip over such useless indexes.
 					//
 					continue;
 				}
 
 				try {
 					packList.add(new PackFile(this, idxFile, packFile));
 				} catch (IOException ioe) {
 					// Whoops. That's not a pack!
 					//
 					ioe.printStackTrace();
 				}
 			}
 		}
 	}
 
     /**
      * Writes a symref (e.g. HEAD) to disk
      *
      * @param name symref name
      * @param target pointed to ref
      * @throws IOException
      */
     public void writeSymref(final String name, final String target)
 			throws IOException {
 		refs.link(name, target);
 	}
 
 	public String toString() {
 		return "Repository[" + getDirectory() + "]";
 	}
 
 	/**
 	 * @return name of topmost Stacked Git patch.
 	 * @throws IOException
 	 */
 	public String getPatch() throws IOException {
 		final File ptr = new File(getDirectory(),"patches/"+getBranch()+"/applied");
 		final BufferedReader br = new BufferedReader(new FileReader(ptr));
 		String last=null;
 		try {
 			String line;
 			while ((line=br.readLine())!=null) {
 				last = line;
 			}
 		} finally {
 			br.close();
 		}
 		return last;
 	}
 
 	/**
 	 * @return name of current branch
 	 * @throws IOException
 	 */
 	public String getFullBranch() throws IOException {
 		final File ptr = new File(getDirectory(),"HEAD");
 		final BufferedReader br = new BufferedReader(new FileReader(ptr));
 		String ref;
 		try {
 			ref = br.readLine();
 		} finally {
 			br.close();
 		}
 		if (ref.startsWith("ref: "))
 			ref = ref.substring(5);
 		return ref;
 	}
 	
 	/**
 	 * @return name of current branch.
 	 * @throws IOException
 	 */
 	public String getBranch() throws IOException {
 		try {
 			final File ptr = new File(getDirectory(), Constants.HEAD);
 			final BufferedReader br = new BufferedReader(new FileReader(ptr));
 			String ref;
 			try {
 				ref = br.readLine();
 			} finally {
 				br.close();
 			}
 			if (ref.startsWith("ref: "))
 				ref = ref.substring(5);
 			if (ref.startsWith("refs/heads/"))
 				ref = ref.substring(11);
 			return ref;
 		} catch (FileNotFoundException e) {
 			final File ptr = new File(getDirectory(),"head-name");
 			final BufferedReader br = new BufferedReader(new FileReader(ptr));
 			String ref;
 			try {
 				ref = br.readLine();
 			} finally {
 				br.close();
 			}
 			return ref;
 		}
 	}
 	
 	/**
 	 * @return all known refs (heads, tags, remotes).
 	 */
 	public Map<String, Ref> getAllRefs() {
 		return refs.getAllRefs();
 	}
 
 	/**
 	 * @return all tags; key is short tag name ("v1.0") and value of the entry
 	 *         contains the ref with the full tag name ("refs/tags/v1.0").
 	 */
 	public Map<String, Ref> getTags() {
 		return refs.getTags();
 	}
 
 	/**
 	 * @return true if HEAD points to a StGit patch.
 	 */
 	public boolean isStGitMode() {
 		try {
 			File file = new File(getDirectory(), "HEAD");
 			BufferedReader reader = new BufferedReader(new FileReader(file));
 			String string = reader.readLine();
 			if (!string.startsWith("ref: refs/heads/"))
 				return false;
 			String branch = string.substring("ref: refs/heads/".length());
 			File currentPatches = new File(new File(new File(getDirectory(),
 					"patches"), branch), "applied");
 			if (!currentPatches.exists())
 				return false;
 			if (currentPatches.length() == 0)
 				return false;
 			return true;
 
 		} catch (IOException e) {
 			e.printStackTrace();
 			return false;
 		}
 	}
 
 	/**
 	 * @return applied patches in a map indexed on current commit id
 	 * @throws IOException
 	 */
 	public Map<ObjectId,StGitPatch> getAppliedPatches() throws IOException {
 		Map<ObjectId,StGitPatch> ret = new HashMap<ObjectId,StGitPatch>();
 		if (isStGitMode()) {
 			File patchDir = new File(new File(getDirectory(),"patches"),getBranch());
 			BufferedReader apr = new BufferedReader(new FileReader(new File(patchDir,"applied")));
 			for (String patchName=apr.readLine(); patchName!=null; patchName=apr.readLine()) {
 				File topFile = new File(new File(new File(patchDir,"patches"), patchName), "top");
 				BufferedReader tfr = new BufferedReader(new FileReader(topFile));
 				String objectId = tfr.readLine();
 				ObjectId id = ObjectId.fromString(objectId);
 				ret.put(id, new StGitPatch(patchName, id));
 				tfr.close();
 			}
 			apr.close();
 		}
 		return ret;
 	}
 	
 	/** Clean up stale caches */
 	public void refreshFromDisk() {
 		refs.clearCache();
 	}
 
 	/**
 	 * @return a representation of the index associated with this repo
 	 * @throws IOException
 	 */
 	public GitIndex getIndex() throws IOException {
 		if (index == null) {
 			index = new GitIndex(this);
 			index.read();
 		} else {
 			index.rereadIfNecessary();
 		}
 		return index;
 	}
 
 	static byte[] gitInternalSlash(byte[] bytes) {
 		if (File.separatorChar == '/')
 			return bytes;
 		for (int i=0; i<bytes.length; ++i)
 			if (bytes[i] == File.separatorChar)
 				bytes[i] = '/';
 		return bytes;
 	}
 
 	/**
 	 * @return an important state
 	 */
 	public RepositoryState getRepositoryState() {
 		if (new File(getWorkDir(), ".dotest").exists())
 			return RepositoryState.REBASING;
 		if (new File(gitDir,".dotest-merge").exists())
 			return RepositoryState.REBASING_INTERACTIVE;
 		if (new File(gitDir,"MERGE_HEAD").exists())
 			return RepositoryState.MERGING;
 		if (new File(gitDir,"BISECT_LOG").exists())
 			return RepositoryState.BISECTING;
 		return RepositoryState.SAFE;
 	}
 
 	/**
 	 * Check validity of a ref name. It must not contain character that has
 	 * a special meaning in a Git object reference expression. Some other
 	 * dangerous characters are also excluded.
 	 *
 	 * @param refName
 	 *
 	 * @return true if refName is a valid ref name
 	 */
 	public static boolean isValidRefName(final String refName) {
 		final int len = refName.length();
 		if (len == 0)
 			return false;
 
 		char p = '\0';
 		for (int i=0; i<len; ++i) {
 			char c = refName.charAt(i);
 			if (c <= ' ')
 				return false;
 			switch(c) {
 			case '.':
 				if (i == 0)
 					return false;
 				if (p == '/')
 					return false;
 				if (p == '.')
 					return false;
 				break;
 			case '/':
 				if (i == 0)
 					return false;
 				if (i == len -1)
 					return false;
 				break;
 			case '~': case '^': case ':':
 			case '?': case '[':
 				return false;
 			case '*':
 				return false;
 			}
 			p = c;
 		}
 		return true;
 	}
 
 	/**
 	 * Strip work dir and return normalized repository path
 	 *
 	 * @param wd Work dir
 	 * @param f File whose path shall be stripped of its workdir
 	 * @return normalized repository relative path
 	 */
 	public static String stripWorkDir(File wd, File f) {
 		String relName = f.getPath().substring(wd.getPath().length() + 1);
 		relName = relName.replace(File.separatorChar, '/');
 		return relName;
 	}
 
 	/**
 	 * @return the workdir file, i.e. where the files are checked out
 	 */
 	public File getWorkDir() {
 		return getDirectory().getParentFile();
 	}
 
 	/**
 	 * Register a {@link RepositoryListener} which will be notified
 	 * when ref changes are detected.
 	 *
 	 * @param l
 	 */
 	public void addRepositoryChangedListener(final RepositoryListener l) {
 		listeners.add(l);
 	}
 
 	/**
 	 * Remove a registered {@link RepositoryListener}
 	 * @param l
 	 */
 	public void removeRepositoryChangedListener(final RepositoryListener l) {
 		listeners.remove(l);
 	}
 
 	/**
 	 * Register a global {@link RepositoryListener} which will be notified
 	 * when a ref changes in any repository are detected.
 	 *
 	 * @param l
 	 */
 	public static void addAnyRepositoryChangedListener(final RepositoryListener l) {
 		allListeners.add(l);
 	}
 
 	/**
 	 * Remove a globally registered {@link RepositoryListener}
 	 * @param l
 	 */
 	public static void removeAnyRepositoryChangedListener(final RepositoryListener l) {
 		allListeners.remove(l);
 	}
 
 	void fireRefsMaybeChanged() {
 		if (refs.lastRefModification != refs.lastNotifiedRefModification) {
 			refs.lastNotifiedRefModification = refs.lastRefModification;
 			final RefsChangedEvent event = new RefsChangedEvent(this);
 			List<RepositoryListener> all;
 			synchronized (listeners) {
 				all = new ArrayList<RepositoryListener>(listeners);
 			}
 			synchronized (allListeners) {
 				all.addAll(allListeners);
 			}
 			for (final RepositoryListener l : all) {
 				l.refsChanged(event);
 			}
 		}
 	}
 
 	void fireIndexChanged() {
 		final IndexChangedEvent event = new IndexChangedEvent(this);
 		List<RepositoryListener> all;
 		synchronized (listeners) {
 			all = new ArrayList<RepositoryListener>(listeners);
 		}
 		synchronized (allListeners) {
 			all.addAll(allListeners);
 		}
 		for (final RepositoryListener l : all) {
 			l.indexChanged(event);
 		}
 	}
 
 	/**
 	 * Force a scan for changed refs.
 	 *
 	 * @throws IOException
 	 */
 	public void scanForRepoChanges() throws IOException {
 		getAllRefs(); // This will look for changes to refs
 		getIndex(); // This will detect changes in the index
 	}
 }
