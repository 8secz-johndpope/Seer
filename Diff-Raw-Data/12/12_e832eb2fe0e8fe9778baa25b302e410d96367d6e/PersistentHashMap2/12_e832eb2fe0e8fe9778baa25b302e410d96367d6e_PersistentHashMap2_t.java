 /**
  Copyright (c) 2007-2008, Rich Hickey
  All rights reserved.
 
  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions
  are met:
 
  * Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
 
  * Redistributions in binary form must reproduce the above
    copyright notice, this list of conditions and the following
    disclaimer in the documentation and/or other materials provided
    with the distribution.
 
  * Neither the name of Clojure nor the names of its contributors
    may be used to endorse or promote products derived from this
    software without specific prior written permission.
 
  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
  COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
  ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
  POSSIBILITY OF SUCH DAMAGE.
  **/
 
 /* rich Jun 18, 2007 */
 
 package clojure.lang;
 
 import java.util.*;
 //this stuff is just for the test main()
 import java.util.concurrent.atomic.AtomicReference;
 
 import clojure.lang.PersistentVector.Node;
 
 /*
  A persistent rendition of Phil Bagwell's Hash Array Mapped Trie
 
  Uses path copying for persistence
  HashCollision leaves vs. extended hashing
  Node polymorphism vs. conditionals
  No sub-tree pools or root-resizing
  Any errors are my own
  */
 
 public class PersistentHashMap2 extends APersistentMap /* implements IEditableCollection */ {
 
 final int count;
 final INode root;
 final boolean hasNull;
 final Object nullValue;
 
 final public static PersistentHashMap2 EMPTY = new PersistentHashMap2(0, null, false, null);
 final private static Object NOT_FOUND = new Object();
 
 static public IPersistentMap create(Map other){
 	IPersistentMap ret = EMPTY;
 	for(Object o : other.entrySet())
 		{
 		Map.Entry e = (Entry) o;
 		ret = ret.assoc(e.getKey(), e.getValue());
 		}
 	return ret;
 }
 
 /*
  * @param init {key1,val1,key2,val2,...}
  */
 public static PersistentHashMap2 create(Object... init){
 	IPersistentMap ret = EMPTY;
 	for(int i = 0; i < init.length; i += 2)
 		{
 		ret = ret.assoc(init[i], init[i + 1]);
 		}
 	return (PersistentHashMap2) ret;
 }
 
 public static PersistentHashMap2 create(List init){
 	IPersistentMap ret = EMPTY;
 	for(Iterator i = init.iterator(); i.hasNext();)
 		{
 		Object key = i.next();
 		if(!i.hasNext())
 			throw new IllegalArgumentException(String.format("No value supplied for key: %s", key));
 		Object val = i.next();
 		ret = ret.assoc(key, val);
 		}
 	return (PersistentHashMap2) ret;
 }
 
 static public PersistentHashMap2 create(ISeq items){
 	IPersistentMap ret = EMPTY;
 	for(; items != null; items = items.next().next())
 		{
 		if(items.next() == null)
 			throw new IllegalArgumentException(String.format("No value supplied for key: %s", items.first()));
 		ret = ret.assoc(items.first(), RT.second(items));
 		}
 	return (PersistentHashMap2) ret;
 }
 
 /*
  * @param init {key1,val1,key2,val2,...}
  */
 public static PersistentHashMap2 create(IPersistentMap meta, Object... init){
 	IPersistentMap ret = EMPTY.withMeta(meta);
 	for(int i = 0; i < init.length; i += 2)
 		{
 		ret = ret.assoc(init[i], init[i + 1]);
 		}
 	return (PersistentHashMap2) ret;
 }
 
 PersistentHashMap2(int count, INode root, boolean hasNull, Object nullValue){
 	this.count = count;
 	this.root = root;
 	this.hasNull = hasNull;
 	this.nullValue = nullValue;
 }
 
 public PersistentHashMap2(IPersistentMap meta, int count, INode root, boolean hasNull, Object nullValue){
 	super(meta);
 	this.count = count;
 	this.root = root;
 	this.hasNull = hasNull;
 	this.nullValue = nullValue;
 }
 
 public boolean containsKey(Object key){
 	if(key == null)
 		return hasNull;
 	return (root != null) ? root.find(0, Util.hash(key), key, NOT_FOUND) != NOT_FOUND : false;
 }
 
 public IMapEntry entryAt(Object key){
 	if(key == null)
 		return hasNull ? new MapEntry(null, nullValue) : null;
 	return (root != null) ? root.find(0, Util.hash(key), key) : null;
 }
 
 public IPersistentMap assoc(Object key, Object val){
 	if(key == null) {
 		if(hasNull && val == nullValue)
 			return this;
 		return new PersistentHashMap2(meta(), hasNull ? count : count + 1, root, true, val);
 	}
 	Box addedLeaf = new Box(null);
 	INode newroot = (root == null ? BitmapIndexedNode.EMPTY : root) 
 			.assoc(0, Util.hash(key), key, val, addedLeaf);
 	if(newroot == root)
 		return this;
 	return new PersistentHashMap2(meta(), addedLeaf.val == null ? count : count + 1, newroot, hasNull, nullValue);
 }
 
 public Object valAt(Object key, Object notFound){
 	if(key == null)
 		return hasNull ? nullValue : notFound;
 	return root != null ? root.find(0, Util.hash(key), key, notFound) : notFound;
 }
 
 public Object valAt(Object key){
 	return valAt(key, null);
 }
 
 public IPersistentMap assocEx(Object key, Object val) throws Exception{
 	if(containsKey(key))
 		throw new Exception("Key already present");
 	return assoc(key, val);
 }
 
 public IPersistentMap without(Object key){
 	if(key == null)
 		return hasNull ? new PersistentHashMap2(meta(), count - 1, root, false, null) : this;
 	if(root == null)
 		return this;
 	INode newroot = root.without(0, Util.hash(key), key);
 	if(newroot == root)
 		return this;
 	return new PersistentHashMap2(meta(), count - 1, newroot, hasNull, nullValue); 
 }
 
 public Iterator iterator(){
 	return new SeqIterator(seq());
 }
 
 public int count(){
 	return count;
 }
 
 public ISeq seq(){
 	ISeq s = root != null ? root.nodeSeq() : null; 
 	return hasNull ? new Cons(new MapEntry(null, nullValue), s) : s;
 }
 
 public IPersistentCollection empty(){
 	return EMPTY.withMeta(meta());	
 }
 
 static int mask(int hash, int shift){
 	//return ((hash << shift) >>> 27);// & 0x01f;
 	return (hash >>> shift) & 0x01f;
 }
 
 public PersistentHashMap2 withMeta(IPersistentMap meta){
 	return new PersistentHashMap2(meta, count, root, hasNull, nullValue);
 }
 
 public TransientHashMap asTransient() {
 	return new TransientHashMap(this);
 }
 
 static final class TransientHashMap extends ATransientMap {
 	AtomicReference<Thread> edit;
 	INode root;
 	int count;
 	
 	TransientHashMap(PersistentHashMap2 m) {
 		this(new AtomicReference<Thread>(Thread.currentThread()), m.root, m.count);
 	}
 	
 	TransientHashMap(AtomicReference<Thread> edit, INode root, int count) {
 		this.edit = edit;
 		this.root = root; 
 		this.count = count; 
 	}
 
 	ITransientMap doAssoc(Object key, Object val) {
 		Box addedLeaf = new Box(null);
 		this.root = root.assoc(edit, 0, Util.hash(key), key, val, addedLeaf);
 		if(addedLeaf.val != null) this.count++;
 		return this;
 	}
 
 	ITransientMap doWithout(Object key) {
 		// TODO
 //		Box removedLeaf = new Box(null);
 //		INode newroot = root.without(edit, null, Util.hash(key), key, removedLeaf);
 //		this.root = newroot == null ? EMPTY.root : newroot;
 //		if(removedLeaf.val != null) this.count--;
 		return this;
 	}
 
 	IPersistentMap doPersistent() {
 		edit.set(null);
 		return null; // TODO new PersistentHashMap(count, root);
 	}
 
 	private IMapEntry entryAt(Object key){
 		// TODO
 		// return root.find(null, null, Util.hash(key), key);
 		return null;
 	}
 
 	Object doValAt(Object key, Object notFound) {
 		IMapEntry e = entryAt(key);
 		if(e != null)
 			return e.val();
 		return notFound;
 	}
 
 	int doCount() {
 		return count;
 	}
 	
 	void ensureEditable(){
 		Thread owner = edit.get();
 		if(owner == Thread.currentThread())
 			return;
 		if(owner != null)
 			throw new IllegalAccessError("Mutable used by non-owner thread");
 		throw new IllegalAccessError("Mutable used after immutable call");
 	}
 }
 
 /*
 final static int[] pathmasks = new int[32];
 static{
 	pathmasks[0] = 0;
 	for(int i=1;i<32;i++)
 		pathmasks[i] = 0x80000000 >> (i - 1);
 }
 //final static int pathmask(int hash, int shift){
 //	//return hash & (0x80000000 >> (shift - 1));
 //	return hash & pathmasks[shift];
 //	}
 
 static boolean diffPath(int shift, int hash1, int hash2){
 	return shift > 0 && ((hash1^hash2) & pathmasks[shift]) != 0;
 	//return shift > 0 && pathmask(hash1^hash2,shift) != 0;
 }
 */
 static interface INode{
 	INode assoc(int shift, int hash, Object key, Object val, Box addedLeaf);
 
 	INode without(int shift, int hash, Object key);
 
 	IMapEntry find(int shift, int hash, Object key);
 
 	Object find(int shift, int hash, Object key, Object notFound);
 
 	ISeq nodeSeq();
 
 	INode assoc(AtomicReference<Thread> edit, int shift, int hash, Object key, Object val, Box addedLeaf);
 
 	INode without(AtomicReference<Thread> edit, int shift, int hash, Object key, Box removedLeaf);
 }
 
 final static class ArrayNode implements INode{
 	final Object[] array;
 	final AtomicReference<Thread> edit;
 
 	ArrayNode(AtomicReference<Thread> edit, Object[] array){
 		this.array = array;
 		this.edit = edit;
 	}
 
 	public INode assoc(int shift, int hash, Object key, Object val, Box addedLeaf){
 		int idx = mask(hash, shift);
 
 		Object keyOrNode = array[2*idx];
		if(keyOrNode == null) {
			addedLeaf.val = val;
			return new ArrayNode(null, cloneAndSet(array, 2*idx, key, 2*idx+1, val));			
		}
 		if(keyOrNode instanceof INode) {
 			INode n = ((INode) keyOrNode).assoc(shift + 5, hash, key, val, addedLeaf);
 			if(n == keyOrNode)
 				return this;
 			return new ArrayNode(null, cloneAndSet(array, 2*idx, n));
 		}
 		if(Util.equals(key, keyOrNode)) {
 			if(val == array[2*idx+1])
 				return this;
 			return new ArrayNode(null, cloneAndSet(array, 2*idx+1, val));
 		}
		addedLeaf.val = val;
 		return new ArrayNode(null, cloneAndSet(array, 
 				2*idx, createNode(shift + 5, keyOrNode, array[2*idx+1], hash, key, val), 
 				2*idx+1, null));
 	}
 
 	public INode without(int shift, int hash, Object key){
 		int idx = mask(hash, shift);
 		Object keyOrNode = array[2*idx];
 		if(keyOrNode == null)
 			return this;
 		if(keyOrNode instanceof INode) {
 			INode n = ((INode) keyOrNode).without(shift + 5, hash, key);
 			if(n == keyOrNode)
 				return this;
 			// TODO: shrink if null
 			return new ArrayNode(null, cloneAndSet(array, 2*idx, n));
 		}
 		if(Util.equals(key, keyOrNode))
 			// TODO: shrink
 			return new ArrayNode(null, cloneAndSet(array, 2*idx, null, 2*idx+1, null));
 		return this;
 	}
 
 	public IMapEntry find(int shift, int hash, Object key){
 		int idx = mask(hash, shift);
 		Object keyOrNode = array[2*idx];
 		if(keyOrNode == null)
 			return null;
 		return deepFind(shift, hash, keyOrNode, array[2*idx+1], key);
 	}
 
 	public Object find(int shift, int hash, Object key, Object notFound){
 		int idx = mask(hash, shift);
 		Object keyOrNode = array[2*idx];
 		if(keyOrNode == null)
 			return notFound;
 		return deepFind(shift, hash, keyOrNode, array[2*idx+1], key, notFound);
 	}
 	
 	public ISeq nodeSeq(){
 		return NodeSeq.create(array);
 	}
 
 	ArrayNode ensureEditable(AtomicReference<Thread> edit){
 //		TODO
 //		if(this.edit == edit)
 			return this;
 //		return new ArrayNode(edit, this.nodes.clone());
 	}
 
 	public INode assoc(AtomicReference<Thread> edit, int shift, int hash, Object key, Object val, Box addedLeaf){
 		return null;
 		// TODO
 //		int idx = mask(hash, shift);
 //
 //		INode n = nodes[idx].assoc(edit, shift + 5, hash, key, val, addedLeaf);
 //		if(n == nodes[idx])
 //			return this;
 //		else
 //			{
 //			ArrayNode node = this.ensureEditable(edit);
 //			node.nodes[idx] = n;
 //			return node;
 //			}
 	}	
 
 	public INode without(AtomicReference<Thread> edit, int shift, int hash, Object key, Box removedLeaf){
 		return null;
 //		int idx = mask(hash, shift);
 //		INode n = nodes[idx].without(edit, null, hash, key, removedLeaf);
 //		if(n != nodes[idx])
 //			{
 //			if(n == null)
 //				{
 //				INode[] newnodes = new INode[nodes.length - 1];
 //				System.arraycopy(nodes, 0, newnodes, 0, idx);
 //				System.arraycopy(nodes, idx + 1, newnodes, idx, nodes.length - (idx + 1));
 //				return new BitmapIndexedNode(edit, ~bitpos(hash, shift), newnodes, shift);
 //				}
 //			ArrayNode node = ensureEditable(edit);
 //			node.nodes[idx] = n;
 //			return node;
 //			}
 //		return this;
 	}
 }
 
 final static class BitmapIndexedNode implements INode{
 	static final BitmapIndexedNode EMPTY = new BitmapIndexedNode(null, 0, new Object[0]);
 	
 	final int bitmap;
 	final Object[] array;
 	final AtomicReference<Thread> edit;
 
 	final int index(int bit){
 		return Integer.bitCount(bitmap & (bit - 1));
 	}
 
 	BitmapIndexedNode(AtomicReference<Thread> edit, int bitmap, Object[] array){
 		this.bitmap = bitmap;
 		this.array = array;
 		this.edit = edit;
 	}
 
 	public INode assoc(int shift, int hash, Object key, Object val, Box addedLeaf){
 		int bit = bitpos(hash, shift);
 		int idx = index(bit);
 		if((bitmap & bit) != 0) {
 			Object keyOrNode = array[2*idx];
 			if(keyOrNode instanceof INode) {
 				INode n = ((INode) keyOrNode).assoc(shift + 5, hash, key, val, addedLeaf);
 				if(n == keyOrNode)
 					return this;
 				return new BitmapIndexedNode(null, bitmap, cloneAndSet(array, 2*idx, n));
 			} 
 			if(Util.equals(key, keyOrNode)) {
 				if(val == array[2*idx+1])
 					return this;
 				return new BitmapIndexedNode(null, bitmap, cloneAndSet(array, 2*idx+1, val));
 			} 
			addedLeaf.val = val;
 			return new BitmapIndexedNode(null, bitmap, 
 					cloneAndSet(array, 
 							2*idx, createNode(shift + 5, keyOrNode, array[2*idx+1], hash, key, val), 
 							2*idx+1, null));
 		} else {
			addedLeaf.val = val;
 			int n = Integer.bitCount(bitmap);
 			if(n >= 16) {
 				Object[] newArray = new Object[64];
 				int jdx = mask(hash, shift);
 				newArray[2*jdx] = key;  
 				newArray[2*jdx+1] = val;
 				int j = 0;
 				for(int i = 0; i < 32; i++)
 					if(((bitmap >>> i) & 1) != 0) {
 						newArray[2*i] = array[j];
 						newArray[2*i+1] = array[j+1];
 						j += 2;
 					}
 				return new ArrayNode(null, newArray);
 			} else {
 				int len = Integer.bitCount(bitmap);
 				Object[] newArray = new Object[2*(len+1)];
 				System.arraycopy(array, 0, newArray, 0, 2*idx);
 				newArray[2*idx] = key;
 				addedLeaf.val = newArray[2*idx+1] = val;
 				System.arraycopy(array, 2*idx, newArray, 2*(idx+1), 2*(len-idx));
 				return new BitmapIndexedNode(null, bitmap | bit, newArray);
 			}
 		}
 	}
 
 	public INode without(int shift, int hash, Object key){
 		int bit = bitpos(hash, shift);
 		if((bitmap & bit) == 0)
 			return this;
 		int idx = index(bit);
 		Object keyOrNode = array[2*idx];
 		if(keyOrNode == null)
 			return this;
 		if(keyOrNode instanceof INode) {
 			INode n = ((INode) keyOrNode).without(shift + 5, hash, key);
 			if(n == keyOrNode)
 				return this;
 			if(n == null) {
 				if(array.length == 2)
 					return null;
 				// TODO: collapse
 				return new BitmapIndexedNode(null, bitmap ^ bit, removePair(array, idx));
 			}
 			return new BitmapIndexedNode(null, bitmap, cloneAndSet(array, 2*idx, n));
 		}
 		if(Util.equals(key, keyOrNode))
 			// TODO: collapse
 			return new BitmapIndexedNode(null, bitmap ^ bit, removePair(array, idx));
 		return this;
 	}
 	
 	public IMapEntry find(int shift, int hash, Object key){
 		int bit = bitpos(hash, shift);
 		if((bitmap & bit) == 0)
 			return null;
 		int idx = index(bit);
 		return deepFind(shift, hash, array[2*idx], array[2*idx+1], key);
 	}
 
 	public Object find(int shift, int hash, Object key, Object notFound){
 		int bit = bitpos(hash, shift);
 		if((bitmap & bit) == 0)
 			return notFound;
 		int idx = index(bit);
 		return deepFind(shift, hash, array[2*idx], array[2*idx+1], key, notFound);
 	}
 
 	public ISeq nodeSeq(){
 		return NodeSeq.create(array);
 	}
 
 	BitmapIndexedNode ensureEditable(AtomicReference<Thread> edit){
 		return null;
 		// TODO
 //		if(this.edit == edit)
 //			return this;
 //		return new BitmapIndexedNode(edit, bitmap, nodes.clone(), shift);
 	}
 
 	public INode assoc(AtomicReference<Thread> edit, int shift, int hash, Object key, Object val, Box addedLeaf){
 		return null;
 		// TODO
 //		int bit = bitpos(hash, shift);
 //		int idx = index(bit);
 //		if((bitmap & bit) != 0)
 //			{
 //			INode n = nodes[idx].assoc(shift + 5, hash, key, val, addedLeaf);
 //			if(n == nodes[idx])
 //				return this;
 //			else
 //				{
 //				BitmapIndexedNode node = ensureEditable(edit);
 //				node.nodes[idx] = n;
 //				return node;
 //				}
 //			}
 //		else
 //			{
 //			// TODO can do better
 //			INode[] newnodes = new INode[nodes.length + 1];
 //			System.arraycopy(nodes, 0, newnodes, 0, idx);
 //			addedLeaf.val = newnodes[idx] = new IMapEntry(null, hash, key, val);
 //			System.arraycopy(nodes, idx, newnodes, idx + 1, nodes.length - idx);
 //			return create(edit, bitmap | bit, newnodes, shift);
 //			}
 	}
 
 	public INode without(AtomicReference<Thread> edit, int shift, int hash, Object key, Box removedLeaf){
 		return null;
 		// TODO
 //		int bit = bitpos(hash, shift);
 //		if((bitmap & bit) != 0)
 //			{
 //			int idx = index(bit);
 //			INode n = nodes[idx].without(edit, null, hash, key, removedLeaf);
 //			if(n != nodes[idx])
 //				{
 //				if(n == null)
 //					{
 //					if(bitmap == bit)
 //						return null;
 //					INode[] newnodes = new INode[nodes.length - 1];
 //					System.arraycopy(nodes, 0, newnodes, 0, idx);
 //					System.arraycopy(nodes, idx + 1, newnodes, idx, nodes.length - (idx + 1));
 //					return new BitmapIndexedNode(edit, bitmap & ~bit, newnodes, shift);
 //					}
 //				BitmapIndexedNode node = ensureEditable(edit);
 //				node.nodes[idx] = n;
 //				return node;
 //				}
 //			}
 //		return this;
 	}
 }
 
 final static class HashCollisionNode implements INode{
 
 	final int hash;
 	final Object[] array;
 	final AtomicReference<Thread> edit;
 
 	HashCollisionNode(AtomicReference<Thread> edit, int hash, Object... array){
 		this.edit = edit;
 		this.hash = hash;
 		this.array = array;
 	}
 
 	public INode assoc(int shift, int hash, Object key, Object val, Box addedLeaf){
 		if(hash == this.hash) {
 			int idx = findIndex(key);
 			if(idx != -1) {
 				if(array[idx + 1] == val)
 					return this;
 				return new HashCollisionNode(null, hash, cloneAndSet(array, idx + 1, val));
 			}
 			Object[] newArray = new Object[array.length + 2];
 			System.arraycopy(array, 0, newArray, 0, array.length);
 			newArray[array.length] = key;
 			newArray[array.length + 1] = val;
 			return new HashCollisionNode(null, hash, newArray);
 		}
 		// nest it in a bitmap node
 		return new BitmapIndexedNode(null, bitpos(this.hash, shift), new Object[] {this})
 			.assoc(shift, hash, key, val, addedLeaf);
 	}
 
 	public INode without(int shift, int hash, Object key){
 		int idx = findIndex(key);
 		if(idx == -1)
 			return this;
 		if(array.length == 2)
 			return null;
 		return new HashCollisionNode(null, hash, removePair(array, idx));
 	}
 
 	public IMapEntry find(int shift, int hash, Object key){
 		int idx = findIndex(key);
 		if(idx < 0)
 			return null;
 		if(Util.equals(key, array[idx]))
 			return new MapEntry(array[idx], array[idx+1]);
 		return null;
 	}
 
 	public Object find(int shift, int hash, Object key, Object notFound){
 		int idx = findIndex(key);
 		if(idx < 0)
 			return notFound;
 		if(Util.equals(key, array[idx]))
 			return array[idx+1];
 		return notFound;
 	}
 
 	public ISeq nodeSeq(){
 		return NodeSeq.create(array);
 	}
 
 	public int findIndex(Object key){
 		for(int i = 0; i < array.length; i+=2)
 			{
 			if(Util.equals(key, array[i]))
 				return i;
 			}
 		return -1;
 	}
 
 	HashCollisionNode ensureEditable(AtomicReference<Thread> edit){
 		return null;
 		// TODO
 //		if(this.edit == edit)
 //			return this;
 //		return new HashCollisionNode(edit, hash, leaves);
 	}
 
 	public INode assoc(AtomicReference<Thread> edit, int shift, int hash, Object key, Object val, Box addedLeaf){
 		return null;
 		// TODO
 //		if(hash == this.hash)
 //			{
 //			int idx = findIndex(hash, key);
 //			if(idx != -1)
 //				{
 //				if(leaves[idx].val == val)
 //					return this;
 //				IMapEntry leaf = leaves[idx].ensureEditable(edit);
 //				leaf.val = val;
 //				if(leaves[idx] == leaf)
 //					return this;
 //				HashCollisionNode node = ensureEditable(edit);
 //				node.leaves[idx] = leaf;
 //				return node;
 //				}
 //			IMapEntry[] newLeaves = new IMapEntry[leaves.length + 1];
 //			System.arraycopy(leaves, 0, newLeaves, 0, leaves.length);
 //			addedLeaf.val = newLeaves[leaves.length] = new IMapEntry(null, hash, key, val);
 //			return new HashCollisionNode(edit, hash, newLeaves);
 //			}
 //		return BitmapIndexedNode.create(edit, shift, this, hash, key, val, addedLeaf);
 	}	
 
 	public INode without(AtomicReference<Thread> edit, int shift, int hash, Object key, Box removedLeaf){
 		return null;
 		// TODO
 //		int idx = findIndex(hash, key);
 //		if(idx == -1)
 //			return this;
 //		removedLeaf.val = leaves[idx];
 //		if(leaves.length == 2)
 //			return idx == 0 ? leaves[1] : leaves[0];
 //		IMapEntry[] newLeaves = new IMapEntry[leaves.length - 1];
 //		System.arraycopy(leaves, 0, newLeaves, 0, idx);
 //		System.arraycopy(leaves, idx + 1, newLeaves, idx, leaves.length - (idx + 1));
 //		return new HashCollisionNode(edit, hash, newLeaves);
 	}
 }
 
 /*
 public static void main(String[] args){
 	try
 		{
 		ArrayList words = new ArrayList();
 		Scanner s = new Scanner(new File(args[0]));
 		s.useDelimiter(Pattern.compile("\\W"));
 		while(s.hasNext())
 			{
 			String word = s.next();
 			words.add(word);
 			}
 		System.out.println("words: " + words.size());
 		IPersistentMap map = PersistentHashMap.EMPTY;
 		//IPersistentMap map = new PersistentTreeMap();
 		//Map ht = new Hashtable();
 		Map ht = new HashMap();
 		Random rand;
 
 		System.out.println("Building map");
 		long startTime = System.nanoTime();
 		for(Object word5 : words)
 			{
 			map = map.assoc(word5, word5);
 			}
 		rand = new Random(42);
 		IPersistentMap snapshotMap = map;
 		for(int i = 0; i < words.size() / 200; i++)
 			{
 			map = map.without(words.get(rand.nextInt(words.size() / 2)));
 			}
 		long estimatedTime = System.nanoTime() - startTime;
 		System.out.println("count = " + map.count() + ", time: " + estimatedTime / 1000000);
 
 		System.out.println("Building ht");
 		startTime = System.nanoTime();
 		for(Object word1 : words)
 			{
 			ht.put(word1, word1);
 			}
 		rand = new Random(42);
 		for(int i = 0; i < words.size() / 200; i++)
 			{
 			ht.remove(words.get(rand.nextInt(words.size() / 2)));
 			}
 		estimatedTime = System.nanoTime() - startTime;
 		System.out.println("count = " + ht.size() + ", time: " + estimatedTime / 1000000);
 
 		System.out.println("map lookup");
 		startTime = System.nanoTime();
 		int c = 0;
 		for(Object word2 : words)
 			{
 			if(!map.contains(word2))
 				++c;
 			}
 		estimatedTime = System.nanoTime() - startTime;
 		System.out.println("notfound = " + c + ", time: " + estimatedTime / 1000000);
 		System.out.println("ht lookup");
 		startTime = System.nanoTime();
 		c = 0;
 		for(Object word3 : words)
 			{
 			if(!ht.containsKey(word3))
 				++c;
 			}
 		estimatedTime = System.nanoTime() - startTime;
 		System.out.println("notfound = " + c + ", time: " + estimatedTime / 1000000);
 		System.out.println("snapshotMap lookup");
 		startTime = System.nanoTime();
 		c = 0;
 		for(Object word4 : words)
 			{
 			if(!snapshotMap.contains(word4))
 				++c;
 			}
 		estimatedTime = System.nanoTime() - startTime;
 		System.out.println("notfound = " + c + ", time: " + estimatedTime / 1000000);
 		}
 	catch(FileNotFoundException e)
 		{
 		e.printStackTrace();
 		}
 
 }
 */
 
 private static Object[] cloneAndSet(Object[] array, int i, Object a) {
 	Object[] clone = array.clone();
 	clone[i] = a;
 	return clone;
 }
 
 private static Object[] cloneAndSet(Object[] array, int i, Object a, int j, Object b) {
 	Object[] clone = array.clone();
 	clone[i] = a;
 	clone[j] = b;
 	return clone;
 }
 
 private static Object[] removePair(Object[] array, int i) {
 	Object[] newArray = new Object[array.length - 2];
 	System.arraycopy(array, 0, newArray, 0, 2*i);
 	System.arraycopy(array, 2*(i+1), newArray, 2*i, newArray.length - 2*i);
 	return newArray;
 }
 
 private static INode createNode(int shift, Object key1, Object val1, int key2hash, Object key2, Object val2) {
 	int key1hash = Util.hash(key1);
 	if(key1hash == key2hash)
 		return new HashCollisionNode(null, key1hash, new Object[] {key1, val1, key2, val2});
 	// TODO: optimize;
 	Box _ = new Box(null);
 	return BitmapIndexedNode.EMPTY
 		.assoc(shift, key1hash, key1, val1, _)
 		.assoc(shift, key2hash, key2, val2, _);
 }
 
 private static Object deepFind(int shift, int hash, Object keyOrNode, Object maybeVal, Object key, Object notFound) {
 	if(keyOrNode == null)
 		return notFound;
 	if(keyOrNode instanceof INode)
 		return ((INode) keyOrNode).find(shift + 5, hash, key, notFound);
 	if(Util.equals(key, keyOrNode))
 		return maybeVal;
 	return notFound;
 }
 
 private static IMapEntry deepFind(int shift, int hash, Object keyOrNode, Object maybeVal, Object key) {
 	if(keyOrNode == null)
 		return null;
 	if(keyOrNode instanceof INode)
 		return ((INode) keyOrNode).find(shift + 5, hash, key);
 	if(Util.equals(key, keyOrNode))
 		return new MapEntry(keyOrNode, maybeVal);
 	return null;
 }
 
 private static int bitpos(int hash, int shift){
 	return 1 << mask(hash, shift);
 }
 
 static final class NodeSeq extends ASeq {
 	final Object[] array;
 	final int i;
 	final ISeq s;
 	
 	NodeSeq(Object[] array, int i) {
 		this(null, array, i, null);
 	}
 
 	static ISeq create(Object[] array) {
 		return create(array, 0, null);
 	}
 
 	private static ISeq create(Object[] array, int i, ISeq s) {
 		if(s != null)
 			return new NodeSeq(null, array, i, s);
 		for(int j = i; j < array.length; j+=2) {
 			Object keyOrNode = array[j];
 			if(keyOrNode instanceof INode) {
 				ISeq nodeSeq = ((INode) keyOrNode).nodeSeq();
 				if(nodeSeq != null)
 					return new NodeSeq(null, array, j + 2, nodeSeq);
 			} else if(keyOrNode != null) 
 				return new NodeSeq(null, array, j, null);
 		}
 		return null;
 	}
 	
 	NodeSeq(IPersistentMap meta, Object[] array, int i, ISeq s) {
 		super(meta);
 		this.array = array;
 		this.i = i;
 		this.s = s;
 	}
 
 	public Obj withMeta(IPersistentMap meta) {
 		return new NodeSeq(meta, array, i, s);
 	}
 
 	public Object first() {
 		if(s != null)
 			return s.first();
 		return new MapEntry(array[i], array[i+1]);
 	}
 
 	public ISeq next() {
 		if(s != null)
 			return create(array, i, s.next());
 		return create(array, i + 2, null);
 	}
 }
 
 }
