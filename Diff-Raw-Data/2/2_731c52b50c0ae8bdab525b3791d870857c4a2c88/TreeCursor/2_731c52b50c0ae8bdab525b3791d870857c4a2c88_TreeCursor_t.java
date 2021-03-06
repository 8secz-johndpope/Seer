 /*
  *  Copyright 2011 Brian S O'Neill
  *
  *  Licensed under the Apache License, Version 2.0 (the "License");
  *  you may not use this file except in compliance with the License.
  *  You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  *  Unless required by applicable law or agreed to in writing, software
  *  distributed under the License is distributed on an "AS IS" BASIS,
  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *  See the License for the specific language governing permissions and
  *  limitations under the License.
  */
 
 package org.cojen.tupl;
 
 import java.io.IOException;
 
 import java.util.ArrayDeque;
 import java.util.Arrays;
 
 import java.util.concurrent.locks.Lock;
 
 /**
  * Internal cursor implementation, which can be used by one thread at a time.
  *
  * @author Brian S O'Neill
  */
 final class TreeCursor implements Cursor {
     final Tree mTree;
     private Transaction mTxn;
 
     // Top stack frame for cursor, always a leaf.
     private TreeCursorFrame mLeaf;
 
     byte[] mKey;
     byte[] mValue;
 
     // FIXME: Define mKeyHashCode, and reduce number of calls to LockManager.hashCode.
 
     TreeCursor(Tree tree, Transaction txn) {
         mTree = tree;
         mTxn = txn;
     }
 
     @Override
     public byte[] key() {
         return mKey;
     }
 
     @Override
     public byte[] value() {
         return mValue;
     }
 
     @Override
     public LockResult first() throws IOException {
         Node root = mTree.mRoot;
         TreeCursorFrame frame = reset(root);
         if (!root.hasKeys()) {
             root.releaseExclusive();
             mKey = null;
             mValue = null;
             return LockResult.UNOWNED;
         }
 
         Transaction txn = mTxn;
         LockResult result = toFirst(txn, root, frame);
         if (result != null || (result = lockCurrentIfExists(txn)) != null) {
             return result;
         }
 
         // If this point is reached, then entry was deleted after latch was
         // released. Move to next entry, which is consistent with findGe.
         // First means, "find greater than or equal to lowest possible key".
         return next();
     }
 
     /**
      * Moves the cursor to the first subtree entry.
      *
      * @param node latched node
      * @param frame frame to bind node to
      * @return null if lock was required but could not be obtained
      */
     private LockResult toFirst(Transaction txn, Node node, TreeCursorFrame frame)
         throws IOException
     {
         while (true) {
             frame.bind(node, 0);
 
             if (node.isLeaf()) {
                 try {
                     return tryCopyCurrent(txn, node, 0);
                 } finally {
                     node.releaseExclusive();
                     mLeaf = frame;
                 }
             }
 
             if (node.mSplit != null) {
                 node = node.mSplit.latchLeft(mTree.mDatabase, node);
             }
 
             node = latchChild(node, 0, true);
             frame = new TreeCursorFrame(frame);
         }
     }
 
     @Override
     public LockResult last() throws IOException {
         Node root = mTree.mRoot;
         TreeCursorFrame frame = reset(root);
         if (!root.hasKeys()) {
             root.releaseExclusive();
             mKey = null;
             mValue = null;
             return LockResult.UNOWNED;
         }
 
         Transaction txn = mTxn;
         LockResult result = toLast(txn, root, frame);
         if (result != null || (result = lockCurrentIfExists(txn)) != null) {
             return result;
         }
 
         // If this point is reached, then entry was deleted after latch was
         // released. Move to previous entry, which is consistent with findLe.
         // Last means, "find less than or equal to highest possible key".
         return previous();
     }
 
     /**
      * Moves the cursor to the last subtree entry.
      *
      * @param node latched node
      * @param frame frame to bind node to
      * @return null if lock was required but could not be obtained
      */
     private LockResult toLast(Transaction txn, Node node, TreeCursorFrame frame)
         throws IOException
     {
         while (true) {
             if (node.isLeaf()) {
                 try {
                     int pos;
                     if (node.mSplit == null) {
                         pos = node.highestLeafPos();
                     } else {
                         pos = node.mSplit.highestLeafPos(mTree.mDatabase, node);
                     }
                     frame.bind(node, pos);
                     return tryCopyCurrent(txn, node, pos);
                 } finally {
                     node.releaseExclusive();
                     mLeaf = frame;
                 }
             }
 
             Split split = node.mSplit;
             if (split == null) {
                 int childPos = node.highestInternalPos();
                 frame.bind(node, childPos);
                 node = latchChild(node, childPos, true);
             } else {
                 // Follow highest position of split, binding this frame to the
                 // unsplit node as if it had not split. The binding will be
                 // corrected when split is finished.
 
                 final Node sibling = split.latchSibling(mTree.mDatabase);
 
                 final Node left, right;
                 if (split.mSplitRight) {
                     left = node;
                     right = sibling;
                 } else {
                     left = sibling;
                     right = node;
                 }
 
                 int highestRightPos = right.highestInternalPos();
                 frame.bind(node, left.highestInternalPos() + 2 + highestRightPos);
                 left.releaseExclusive();
 
                 node = latchChild(right, highestRightPos, true);
             }
 
             frame = new TreeCursorFrame(frame);
         }
     }
 
     @Override
     public LockResult move(long amount) throws IOException {
         // TODO: optimize and also utilize counts embedded in the tree
 
         // FIXME: Skipped entries should be locked no higher than
         // READ_COMMITTED and released immediately.
 
         if (amount > 0) {
             do {
                 next();
             } while (--amount > 0);
         } else if (amount < 0) {
             do {
                 previous();
             } while (++amount < 0);
         }
 
         // FIXME: LockResult
         return LockResult.UNOWNED;
     }
 
     @Override
     public LockResult next() throws IOException {
         return next(mTxn, leafExclusiveNotSplit());
     }
 
     /**
      * @param frame leaf frame, not split, with exclusive latch
      */
     private LockResult next(Transaction txn, TreeCursorFrame frame) throws IOException {
         while (true) {
             LockResult result = toNext(txn, frame);
             if (result != null || (result = lockCurrentIfExists(txn)) != null) {
                 return result;
             }
         }
     }
 
     /**
      * @param frame leaf frame, not split, with exclusive latch
      * @return null if lock was required but could not be obtained
      */
     private LockResult toNext(Transaction txn, TreeCursorFrame frame) throws IOException {
         Node node = frame.mNode;
 
         quick: {
             int pos = frame.mNodePos;
 
             if (pos < 0) {
                 pos = ~2 - pos; // eq: (~pos) - 2;
                 if (pos >= node.highestLeafPos()) {
                     break quick;
                 }
                 frame.mNotFoundKey = null;
             } else if (pos >= node.highestLeafPos()) {
                 break quick;
             }
 
             frame.mNodePos = (pos += 2);
 
             try {
                 return tryCopyCurrent(txn, node, pos);
             } finally {
                 node.releaseExclusive();
             }
         }
 
         while (true) {
             TreeCursorFrame parentFrame = frame.peek();
 
             if (parentFrame == null) {
                 frame.popv();
                 node.releaseExclusive();
                 mLeaf = null;
                 mKey = null;
                 mValue = null;
                 return LockResult.UNOWNED;
             }
 
             Node parentNode;
             int parentPos;
 
             latchParent: {
                 splitCheck: {
                     // Latch coupling up the tree usually works, so give it a
                     // try. If it works, then there's no need to worry about a
                     // node merge.
                     parentNode = parentFrame.tryAcquireExclusive();
 
                     if (parentNode == null) {
                         // Latch coupling failed, and so acquire parent latch
                         // without holding child latch. The child might have
                         // changed, and so it must be checked again.
                         node.releaseExclusive();
                         parentNode = parentFrame.acquireExclusive();
                         if (parentNode.mSplit == null) {
                             break splitCheck;
                         }
                     } else {
                         if (parentNode.mSplit == null) {
                             frame.popv();
                             node.releaseExclusive();
                             parentPos = parentFrame.mNodePos;
                             break latchParent;
                         }
                         node.releaseExclusive();
                     }
 
                     // When this point is reached, parent node must be split.
                     // Parent latch is held, child latch is not held, but the
                     // frame is still valid.
 
                     parentNode = finishSplit(parentFrame, parentNode);
                 }
 
                 // When this point is reached, child must be relatched. Parent
                 // latch is held, and the child frame is still valid.
 
                 parentPos = parentFrame.mNodePos;
                 node = latchChild(parentNode, parentPos, false);
 
                 // Quick check again, in case node got bigger due to merging.
                 // Unlike the earlier quick check, this one must handle
                 // internal nodes too.
                 quick: {
                     int pos = frame.mNodePos;
 
                     if (pos < 0) {
                         pos = ~2 - pos; // eq: (~pos) - 2;
                         if (pos >= node.highestLeafPos()) {
                             break quick;
                         }
                         frame.mNotFoundKey = null;
                     } else if (pos >= node.highestPos()) {
                         break quick;
                     }
 
                     parentNode.releaseExclusive();
                     frame.mNodePos = (pos += 2);
 
                     if (frame == mLeaf) {
                         try {
                             return tryCopyCurrent(txn, node, pos);
                         } finally {
                             node.releaseExclusive();
                         }
                     } else {
                         return toFirst(txn,
                                        latchChild(node, pos, true),
                                        new TreeCursorFrame(frame));
                     }
                 }
 
                 frame.popv();
                 node.releaseExclusive();
             }
 
             // When this point is reached, only the parent latch is held. Child
             // frame is no longer valid.
 
             if (parentPos < parentNode.highestInternalPos()) {
                 parentFrame.mNodePos = (parentPos += 2);
                 return toFirst(txn,
                                latchChild(parentNode, parentPos, true),
                                new TreeCursorFrame(parentFrame));
             }
 
             frame = parentFrame;
             node = parentNode;
         }
     }
 
     @Override
     public LockResult previous() throws IOException {
         return previous(mTxn, leafExclusiveNotSplit());
     }
 
     /**
      * @param frame leaf frame, not split, with exclusive latch
      */
     private LockResult previous(Transaction txn, TreeCursorFrame frame) throws IOException {
         while (true) {
             LockResult result = toPrevious(txn, frame);
             if (result != null || (result = lockCurrentIfExists(txn)) != null) {
                 return result;
             }
         }
     }
 
     /**
      * @param frame leaf frame, not split, with exclusive latch
      * @return null if lock was required but could not be obtained
      */
     private LockResult toPrevious(Transaction txn, TreeCursorFrame frame) throws IOException {
         Node node = frame.mNode;
 
         quick: {
             int pos = frame.mNodePos;
 
             if (pos < 0) {
                 pos = ~pos;
                 if (pos == 0) {
                     break quick;
                 }
                 frame.mNotFoundKey = null;
             } else if (pos == 0) {
                 break quick;
             }
 
             frame.mNodePos = (pos -= 2);
 
             try {
                 return tryCopyCurrent(txn, node, pos);
             } finally {
                 node.releaseExclusive();
             }
         }
 
         while (true) {
             TreeCursorFrame parentFrame = frame.peek();
 
             if (parentFrame == null) {
                 frame.popv();
                 node.releaseExclusive();
                 mLeaf = null;
                 mKey = null;
                 mValue = null;
                 return LockResult.UNOWNED;
             }
 
             Node parentNode;
             int parentPos;
 
             latchParent: {
                 splitCheck: {
                     // Latch coupling up the tree usually works, so give it a
                     // try. If it works, then there's no need to worry about a
                     // node merge.
                     parentNode = parentFrame.tryAcquireExclusive();
 
                     if (parentNode == null) {
                         // Latch coupling failed, and so acquire parent latch
                         // without holding child latch. The child might have
                         // changed, and so it must be checked again.
                         node.releaseExclusive();
                         parentNode = parentFrame.acquireExclusive();
                         if (parentNode.mSplit == null) {
                             break splitCheck;
                         }
                     } else {
                         if (parentNode.mSplit == null) {
                             frame.popv();
                             node.releaseExclusive();
                             parentPos = parentFrame.mNodePos;
                             break latchParent;
                         }
                         node.releaseExclusive();
                     }
 
                     // When this point is reached, parent node must be split.
                     // Parent latch is held, child latch is not held, but the
                     // frame is still valid.
 
                     parentNode = finishSplit(parentFrame, parentNode);
                 }
 
                 // When this point is reached, child must be relatched. Parent
                 // latch is held, and the child frame is still valid.
 
                 parentPos = parentFrame.mNodePos;
                 node = latchChild(parentNode, parentPos, false);
 
                 // Quick check again, in case node got bigger due to merging.
                 // Unlike the earlier quick check, this one must handle
                 // internal nodes too.
                 quick: {
                     int pos = frame.mNodePos;
 
                     if (pos < 0) {
                         pos = ~pos;
                         if (pos == 0) {
                             break quick;
                         }
                         frame.mNotFoundKey = null;
                     } else if (pos == 0) {
                         break quick;
                     }
 
                     parentNode.releaseExclusive();
                     frame.mNodePos = (pos -= 2);
 
                     if (frame == mLeaf) {
                         try {
                             return tryCopyCurrent(txn, node, pos);
                         } finally {
                             node.releaseExclusive();
                         }
                     } else {
                         return toLast(txn,
                                       latchChild(node, pos, true),
                                       new TreeCursorFrame(frame));
                     }
                 }
 
                 frame.popv();
                 node.releaseExclusive();
             }
 
             // When this point is reached, only the parent latch is held. Child
             // frame is no longer valid.
 
             if (parentPos > 0) {
                 parentFrame.mNodePos = (parentPos -= 2);
                 return toLast(txn,
                               latchChild(parentNode, parentPos, true),
                               new TreeCursorFrame(parentFrame));
             }
 
             frame = parentFrame;
             node = parentNode;
         }
     }
 
     /**
      * Try to copy the current entry, locking it if required. Null is returned
      * if lock is not immediately available and only the key was copied.
      *
      * @return null, UNOWNED, INTERRUPTED, TIMED_OUT_LOCK, ACQUIRED,
      * OWNED_SHARED, OWNED_UPGRADABLE, or OWNED_EXCLUSIVE
      * @param txn optional
      * @param node latched node
      */
     private LockResult tryCopyCurrent(Transaction txn, Node node, int pos) {
         final LockMode mode;
         if (txn == null) {
             mode = LockMode.READ_COMMITTED;
         } else if ((mode = txn.lockMode()).noReadLock) {
             node.retrieveLeafEntry(pos, this);
             return LockResult.UNOWNED;
         }
 
         // Copy key for now, because lock might not be available. Value might
         // change after latch is released. Assign a value of null, in case lock
         // cannot be granted at all. This prevents uncommited value from being
         // exposed.
         mKey = node.retrieveLeafKey(pos);
         mValue = null;
 
         LockResult result;
 
         try {
             switch (mode) {
             default:
                 if (mTree.isLockAvailable(txn, mKey)) {
                     // No need to acquire full lock.
                     mValue = node.retrieveLeafValue(pos);
                     return LockResult.UNOWNED;
                 } else {
                     return null;
                 }
 
             case REPEATABLE_READ:
                 result = txn.tryLockShared(mTree.mId, mKey, 0);
                 break;
 
             case UPGRADABLE_READ:
                 result = txn.tryLockUpgradable(mTree.mId, mKey, 0);
                 break;
             }
 
             if (result.isGranted()) {
                 mValue = node.retrieveLeafValue(pos);
                 return result;
             } else {
                 return null;
             }
         } catch (DeadlockException e) {
             // Not expected with timeout of zero anyhow.
             return null;
         }
     }
 
     /**
      * With node latch not held, lock the current key. Returns the lock result
      * if entry exists, null otherwise.
      *
      * @param txn optional
      * @return null if current entry has been deleted
      */
     private LockResult lockCurrentIfExists(Transaction txn) throws IOException {
         if (txn == null) {
             Locker locker = mTree.lockSharedLocal(mKey);
             try {
                 if (copyValueIfExists()) {
                     return LockResult.UNOWNED;
                 }
             } finally {
                 locker.unlock();
             }
         } else {
             LockResult result;
 
             switch (txn.lockMode()) {
                 // Default case should only capture READ_COMMITTED, since
                 // no-lock modes were already handled.
             default:
                 if ((result = txn.lockShared(mTree.mId, mKey)) == LockResult.ACQUIRED) {
                     result = LockResult.UNOWNED;
                 }
                 break;
 
             case REPEATABLE_READ:
                 result = txn.lockShared(mTree.mId, mKey);
                 break;
 
             case UPGRADABLE_READ:
                 result = txn.lockUpgradable(mTree.mId, mKey);
                 break;
             }
 
             if (copyValueIfExists()) {
                 if (result == LockResult.UNOWNED) {
                     txn.unlock();
                 }
                 return result;
             }
 
             if (result == LockResult.UNOWNED || result == LockResult.ACQUIRED) {
                 txn.unlock();
             }
         }
 
         // Entry does not exist, and lock has been released if was just acquired.
         return null;
     }
 
     private boolean copyValueIfExists() throws IOException {
         TreeCursorFrame frame = leaf();
         Node node = frame.acquireShared();
         try {
             splitCheck: if (node.mSplit != null) {
                 if (!node.tryUpgrade()) {
                     node.releaseShared();
                     node = frame.acquireExclusive();
                     if (node.mSplit == null) {
                         break splitCheck;
                     }
                 }
                 node = finishSplit(frame, node);
             }
 
             int pos = frame.mNodePos;
 
             if (pos >= 0) {
                mValue = node.retrieveLeafValue(pos);
                 return true;
             }
         } finally {
             node.releaseShared();
         }
 
         return false;
     }
 
     private static final int
         VARIANT_REGULAR = 0,
         VARIANT_NEARBY  = 1,
         VARIANT_RETAIN  = 2, // retain node latch
         VARIANT_NO_LOCK = 3, // retain node latch, don't lock entry
         VARIANT_CHECK   = 4; // retain node latch, don't lock entry, don't copy entry
 
     @Override
     public LockResult find(byte[] key) throws IOException {
         return find(mTxn, key, VARIANT_REGULAR);
     }
 
     @Override
     public LockResult findGe(byte[] key) throws IOException {
         // If isolation level is read committed, then key must be
         // locked. Otherwise, an uncommitted delete could be observed.
         Transaction txn = mTxn;
         LockResult result = find(txn, key, VARIANT_RETAIN);
         if (mValue != null) {
             mLeaf.mNode.releaseExclusive();
             return result;
         } else {
             if (result == LockResult.ACQUIRED) {
                 txn.unlock();
             }
             return next(txn, mLeaf);
         }
     }
 
     @Override
     public LockResult findGt(byte[] key) throws IOException {
         // Never lock the requested key.
         Transaction txn = mTxn;
         find(txn, key, VARIANT_CHECK);
         return next(txn, mLeaf);
     }
 
     @Override
     public LockResult findLe(byte[] key) throws IOException {
         // If isolation level is read committed, then key must be
         // locked. Otherwise, an uncommitted delete could be observed.
         Transaction txn = mTxn;
         LockResult result = find(txn, key, VARIANT_RETAIN);
         if (mValue != null) {
             mLeaf.mNode.releaseExclusive();
             return result;
         } else {
             if (result == LockResult.ACQUIRED) {
                 txn.unlock();
             }
             return previous(txn, mLeaf);
         }
     }
 
     @Override
     public LockResult findLt(byte[] key) throws IOException {
         // Never lock the requested key.
         Transaction txn = mTxn;
         find(txn, key, VARIANT_CHECK);
         return previous(txn, mLeaf);
     }
 
     @Override
     public LockResult findNearby(byte[] key) throws IOException {
         return find(mTxn, key, VARIANT_NEARBY);
     }
 
     private LockResult find(Transaction txn, byte[] key, int variant) throws IOException {
         if (key == null) {
             throw new NullPointerException("Cannot find a null key");
         }
 
         LockResult result;
         Locker locker;
 
         if (variant == VARIANT_NO_LOCK) {
             result = LockResult.UNOWNED;
             locker = null;
         } else {
             if (txn == null) {
                 result = LockResult.UNOWNED;
                 locker = variant == VARIANT_CHECK ? null : mTree.lockSharedLocal(key);
             } else {
                 switch (txn.lockMode()) {
                 default: // no read lock requested by READ_UNCOMMITTED or UNSAFE
                     result = LockResult.UNOWNED;
                     locker = null;
                     break;
 
                 case READ_COMMITTED:
                     if ((result = txn.lockShared(mTree.mId, key)) == LockResult.ACQUIRED) {
                         result = LockResult.UNOWNED;
                         locker = txn;
                     } else {
                         locker = null;
                     }
                     break;
 
                 case REPEATABLE_READ:
                     result = txn.lockShared(mTree.mId, key);
                     locker = null;
                     break;
 
                 case UPGRADABLE_READ:
                     result = txn.lockUpgradable(mTree.mId, key);
                     locker = null;
                     break;
                 }
             }
         }
 
         try {
             mKey = key;
 
             Node node;
             TreeCursorFrame frame;
 
             nearby: if (variant == VARIANT_NEARBY) {
                 frame = mLeaf;
                 if (frame == null) {
                     node = mTree.mRoot;
                     node.acquireExclusive();
                     frame = new TreeCursorFrame();
                     break nearby;
                 }
 
                 node = frame.acquireExclusive();
                 if (node.mSplit != null) {
                     node = finishSplit(frame, node);
                 }
 
                 int startPos = frame.mNodePos;
                 if (startPos < 0) {
                     startPos = ~startPos;
                 }
 
                 int pos = node.binarySearchLeaf(key, startPos);
 
                 if (pos >= 0) {
                     frame.mNotFoundKey = null;
                     frame.mNodePos = pos;
                     mValue = node.retrieveLeafValue(pos);
                     node.releaseExclusive();
                     return result;
                 } else if (pos != ~0 && ~pos <= node.highestLeafPos()) {
                     // Not found, but insertion pos is in bounds.
                     frame.mNotFoundKey = key;
                     frame.mNodePos = pos;
                     mValue = null;
                     node.releaseExclusive();
                     return result;
                 }
 
                 // Cannot be certain if position is in leaf node, so pop up.
 
                 mLeaf = null;
 
                 while (true) {
                     TreeCursorFrame parent = frame.pop();
 
                     if (parent == null) {
                         // Usually the root frame refers to the root node, but
                         // it can be wrong if the tree height is changing.
                         Node root = mTree.mRoot;
                         if (node != root) {
                             node.releaseExclusive();
                             root.acquireExclusive();
                             node = root;
                         }
                         break;
                     }
 
                     node.releaseExclusive();
                     frame = parent;
                     node = frame.acquireExclusive();
 
                     // Only search inside non-split nodes. It's easier to just
                     // pop up rather than finish or search the split.
                     if (node.mSplit != null) {
                         continue;
                     }
 
                     pos = Node.internalPos(node.binarySearchInternal(key, frame.mNodePos));
 
                     if (pos == 0 || pos >= node.highestInternalPos()) {
                         // Cannot be certain if position is in this node, so pop up.
                         continue;
                     }
 
                     frame.mNodePos = pos;
                     node = latchChild(node, pos, true);
                     frame = new TreeCursorFrame(frame);
                     break;
                 }
             } else {
                 // Regular variant always discards existing frames.
                 node = mTree.mRoot;
                 frame = reset(node);
             }
 
             while (true) {
                 if (node.isLeaf()) {
                     int pos;
                     if (node.mSplit == null) {
                         pos = node.binarySearchLeaf(key);
                     } else {
                         pos = node.mSplit.binarySearchLeaf(mTree.mDatabase, node, key);
                     }
                     frame.bind(node, pos);
                     if (pos < 0) {
                         frame.mNotFoundKey = key;
                         mValue = null;
                     } else {
                         mValue = variant == VARIANT_CHECK ? null : node.retrieveLeafValue(pos);
                     }
                     mLeaf = frame;
                     if (variant < VARIANT_RETAIN) {
                         node.releaseExclusive();
                     }
                     return result;
                 }
 
                 Split split = node.mSplit;
                 if (split == null) {
                     int childPos = Node.internalPos(node.binarySearchInternal(key));
                     frame.bind(node, childPos);
                     node = latchChild(node, childPos, true);
                 } else {
                     // Follow search into split, binding this frame to the
                     // unsplit node as if it had not split. The binding will be
                     // corrected when split is finished.
 
                     final Node sibling = split.latchSibling(mTree.mDatabase);
 
                     final Node left, right;
                     if (split.mSplitRight) {
                         left = node;
                         right = sibling;
                     } else {
                         left = sibling;
                         right = node;
                     }
 
                     final Node selected;
                     final int selectedPos;
 
                     if (split.compare(key) < 0) {
                         selected = left;
                         selectedPos = Node.internalPos(left.binarySearchInternal(key));
                         frame.bind(node, selectedPos);
                         right.releaseExclusive();
                     } else {
                         selected = right;
                         selectedPos = Node.internalPos(right.binarySearchInternal(key));
                         frame.bind(node, left.highestInternalPos() + 2 + selectedPos);
                         left.releaseExclusive();
                     }
 
                     node = latchChild(selected, selectedPos, true);
                 }
 
                 frame = new TreeCursorFrame(frame);
             }
         } finally {
             if (locker != null) {
                 locker.unlock();
             }
         }
     }
 
     @Override
     public LockResult reload() throws IOException {
         byte[] key = mKey;
         if (key == null) {
             throw new IllegalStateException("Cursor position is undefined");
         }
 
         LockResult result;
         Locker locker;
 
         Transaction txn = mTxn;
         if (txn == null) {
             result = LockResult.UNOWNED;
             locker = mTree.lockSharedLocal(key);
         } else {
             switch (txn.lockMode()) {
             default: // no read lock requested by READ_UNCOMMITTED or UNSAFE
                 result = LockResult.UNOWNED;
                 locker = null;
                 break;
 
             case READ_COMMITTED:
                 if ((result = txn.lockShared(mTree.mId, key)) == LockResult.ACQUIRED) {
                     result = LockResult.UNOWNED;
                     locker = txn;
                 } else {
                     locker = null;
                 }
                 break;
 
             case REPEATABLE_READ:
                 result = txn.lockShared(mTree.mId, key);
                 locker = null;
                 break;
 
             case UPGRADABLE_READ:
                 result = txn.lockUpgradable(mTree.mId, key);
                 locker = null;
                 break;
             }
         }
 
         try {
             TreeCursorFrame frame = mLeaf;
             Node node = frame.acquireShared();
             if (node.mSplit == null) {
                 int pos = frame.mNodePos;
                 mValue = pos >= 0 ? node.retrieveLeafValue(pos) : null;
                 node.releaseShared();
             } else {
                 if (!node.tryUpgrade()) {
                     node.releaseShared();
                     node = frame.acquireExclusive();
                 }
                 if (node.mSplit != null) {
                     node = finishSplit(frame, node);
                 }
                 int pos = frame.mNodePos;
                 mValue = pos >= 0 ? node.retrieveLeafValue(pos) : null;
                 node.releaseExclusive();
             }
             return result;
         } finally {
             if (locker != null) {
                 locker.unlock();
             }
         }
     }
 
     @Override
     public void store(byte[] value) throws IOException {
         byte[] key = mKey;
         if (key == null) {
             throw new IllegalStateException("Cursor position is undefined");
         }
 
         Lock sharedCommitLock = mTree.mDatabase.sharedCommitLock();
         sharedCommitLock.lock();
         try {
             final Transaction txn = mTxn;
             final Locker locker = mTree.lockExclusive(txn, key);
             try {
                 store(txn, key, leafExclusive(), value);
             } finally {
                 if (locker != null) {
                     locker.unlock();
                 }
             }
         } catch (Throwable e) {
             throw handleStoreException(e);
         } finally {
             sharedCommitLock.unlock();
         }
     }
 
     /**
      * Atomic find and store operation.
      */
     void findAndStore(byte[] key, byte[] value) throws IOException {
         final Lock sharedCommitLock = mTree.mDatabase.sharedCommitLock();
         sharedCommitLock.lock();
         try {
             final Transaction txn = mTxn;
             final Locker locker = mTree.lockExclusive(txn, key);
             try {
                 // Find with no lock because it has already been acquired.
                 find(null, key, VARIANT_NO_LOCK);
                 store(txn, key, mLeaf, value);
             } finally {
                 if (locker != null) {
                     locker.unlock();
                 }
             }
         } catch (Throwable e) {
             throw handleStoreException(e);
         } finally {
             sharedCommitLock.unlock();
         }
     }
 
     static final byte[] MODIFY_INSERT = new byte[0], MODIFY_REPLACE = new byte[0];
 
     /**
      * Atomic find and modify operation.
      *
      * @param oldValue MODIFY_INSERT, MODIFY_REPLACE, else update mode
      */
     boolean findAndModify(byte[] key, byte[] oldValue, byte[] newValue) throws IOException {
         final Lock sharedCommitLock = mTree.mDatabase.sharedCommitLock();
         sharedCommitLock.lock();
         try {
             // Note: Acquire exclusive lock instead of performing upgrade
             // sequence. The upgrade would need to be performed with the node
             // latch held, which is deadlock prone.
 
             final Transaction txn = mTxn;
             if (txn == null) {
                 Locker locker = mTree.lockExclusiveLocal(key);
                 try {
                     return doFindAndModify(txn, key, oldValue, newValue);
                 } finally {
                     locker.unlock();
                 }
             }
 
             LockResult result;
 
             LockMode mode = txn.lockMode();
             if (mode == LockMode.UNSAFE) {
                 // Indicate that no unlock should be performed.
                 result = LockResult.OWNED_EXCLUSIVE;
             } else {
                 result = txn.lockExclusive(mTree.mId, key);
                 if (result == LockResult.ACQUIRED &&
                     (mode == LockMode.REPEATABLE_READ || mode == LockMode.UPGRADABLE_READ))
                 {
                     // Downgrade to upgradable when no modification is made, to
                     // preserve repeatable semantics and allow upgrade later.
                     result = LockResult.UPGRADED;
                 }
             }
 
             try {
                 if (doFindAndModify(txn, key, oldValue, newValue)) {
                     // Indicate that no unlock should be performed.
                     result = LockResult.OWNED_EXCLUSIVE;
                     return true;
                 }
                 return false;
             } finally {
                 if (result == LockResult.ACQUIRED) {
                     txn.unlock();
                 } else if (result == LockResult.UPGRADED) {
                     txn.unlockToUpgradable();
                 }
             }
         } catch (Throwable e) {
             throw handleStoreException(e);
         } finally {
             sharedCommitLock.unlock();
         }
     }
 
     private boolean doFindAndModify(Transaction txn, byte[] key, byte[] oldValue, byte[] newValue)
         throws IOException
     {
         // Find with no lock because caller must already acquire exclusive lock.
         find(null, key, VARIANT_NO_LOCK);
 
         if (oldValue == MODIFY_INSERT) {
             // insert mode
 
             if (mValue != null) {
                 mLeaf.mNode.releaseExclusive();
                 return false;
             }
 
             store(txn, key, mLeaf, newValue);
             return true;
         } else if (oldValue == MODIFY_REPLACE) {
             // replace mode
 
             if (mValue == null) {
                 mLeaf.mNode.releaseExclusive();
                 return false;
             }
 
             store(txn, key, mLeaf, newValue);
             return true;
         } else {
             // update mode
 
             if (mValue != null) {
                 if (Arrays.equals(oldValue, mValue)) {
                     store(txn, key, mLeaf, newValue);
                     return true;
                 } else {
                     mLeaf.mNode.releaseExclusive();
                     return false;
                 }
             } else if (oldValue == null) {
                 if (newValue == null) {
                     mLeaf.mNode.releaseExclusive();
                 } else {
                     store(txn, key, mLeaf, newValue);
                 }
                 return true;
             } else {
                 mLeaf.mNode.releaseExclusive();
                 return false;
             }
         }
     }
 
     /**
      * Note: caller must hold shared commit lock, to prevent deadlock.
      *
      * @param leaf leaf frame, latched exclusively, which is released by this method
      */
     private void store(Transaction txn, final byte[] key, final TreeCursorFrame leaf, byte[] value)
         throws IOException
     {
         if (value == null) {
             // Delete entry...
 
             if (leaf.mNodePos < 0) {
                 // Entry doesn't exist, so nothing to do.
                 leaf.mNode.releaseExclusive();
                 return;
             }
 
             Node node = notSplitDirty(leaf);
             final int pos = leaf.mNodePos;
 
             if (txn == null) {
                 mTree.redoStore(key, null);
             } else {
                 if (txn.lockMode() != LockMode.UNSAFE) {
                     node.undoPushLeafEntry(txn, mTree.mId, pos);
                 }
                 if (txn.mDurabilityMode != DurabilityMode.NO_LOG) {
                     txn.redoStore(mTree.mId, key, null);
                 }
             }
 
             node.deleteLeafEntry(pos);
             int newPos = ~pos;
 
             leaf.mNodePos = newPos;
             leaf.mNotFoundKey = key;
 
             // Fix all cursors bound to the node.
             TreeCursorFrame frame = node.mLastCursorFrame;
             do {
                 if (frame == leaf) {
                     // Don't need to fix self.
                     continue;
                 }
 
                 int framePos = frame.mNodePos;
 
                 if (framePos == pos) {
                     frame.mNodePos = newPos;
                     frame.mNotFoundKey = key;
                 } else if (framePos > pos) {
                     frame.mNodePos = framePos - 2;
                 } else if (framePos < newPos) {
                     // Position is a complement, so add instead of subtract.
                     frame.mNodePos = framePos + 2;
                 }
             } while ((frame = frame.mPrevCousin) != null);
 
             if (node.shouldLeafMerge()) {
                 mergeLeaf(leaf, node);
             } else {
                 node.releaseExclusive();
             }
 
             return;
         }
 
         // Update and insert always dirty the node.
         Node node = notSplitDirty(leaf);
         final int pos = leaf.mNodePos;
 
         if (pos >= 0) {
             // Update entry...
 
             if (txn == null) {
                 mTree.redoStore(key, value);
             } else {
                 if (txn.lockMode() != LockMode.UNSAFE) {
                     node.undoPushLeafEntry(txn, mTree.mId, pos);
                 }
                 if (txn.mDurabilityMode != DurabilityMode.NO_LOG) {
                     txn.redoStore(mTree.mId, key, value);
                 }
             }
 
             node.updateLeafValue(mTree.mDatabase, pos, value);
 
             if (node.shouldLeafMerge()) {
                 mergeLeaf(leaf, node);
             } else {
                 if (node.mSplit != null) {
                     node = finishSplit(leaf, node);
                 }
                 node.releaseExclusive();
             }
 
             return;
         }
 
         // Insert entry...
 
         if (txn == null) {
             mTree.redoStore(key, value);
         } else {
             if (txn.lockMode() != LockMode.UNSAFE) {
                 txn.undoDelete(mTree.mId, key);
             }
             if (txn.mDurabilityMode != DurabilityMode.NO_LOG) {
                 txn.redoStore(mTree.mId, key, value);
             }
         }
 
         int newPos = ~pos;
         node.insertLeafEntry(mTree.mDatabase, newPos, key, value);
 
         leaf.mNodePos = newPos;
         leaf.mNotFoundKey = null;
 
         // Fix all cursors bound to the node.
         TreeCursorFrame frame = node.mLastCursorFrame;
         do {
             if (frame == leaf) {
                 // Don't need to fix self.
                 continue;
             }
 
             int framePos = frame.mNodePos;
 
             if (framePos == pos) {
                 // Other cursor is at same not-found position as this one was.
                 // If keys are the same, then other cursor switches to a found
                 // state as well. If key is greater, then position needs to be
                 // updated.
 
                 byte[] frameKey = frame.mNotFoundKey;
                 int compare = Utils.compareKeys
                     (frameKey, 0, frameKey.length, key, 0, key.length);
                 if (compare > 0) {
                     // Position is a complement, so subtract instead of add.
                     frame.mNodePos = framePos - 2;
                 } else if (compare == 0) {
                     frame.mNodePos = newPos;
                     frame.mNotFoundKey = null;
                 }
             } else if (framePos >= newPos) {
                 frame.mNodePos = framePos + 2;
             } else if (framePos < pos) {
                 // Position is a complement, so subtract instead of add.
                 frame.mNodePos = framePos - 2;
             }
         } while ((frame = frame.mPrevCousin) != null);
 
         if (node.mSplit != null) {
             node = finishSplit(leaf, node);
         }
 
         node.releaseExclusive();
     }
 
     private IOException handleStoreException(Throwable e) throws IOException {
         // Any unexpected exception can corrupt the internal store state.
         // Closing down protects the persisted state.
         if (mLeaf == null && e instanceof IllegalStateException) {
             // Exception is caused by cursor state; store is safe.
             throw (IllegalStateException) e;
         }
         try {
             throw Utils.closeOnFailure(mTree.mDatabase, e);
         } finally {
             // FIXME: this can deadlock, because exception can be thrown at anytime
             //close();
         }
     }
 
     @Override
     public void link(Transaction txn) {
         mTxn = txn;
     }
 
     @Override
     public TreeCursor copy() {
         TreeCursor copy = new TreeCursor(mTree, mTxn);
         TreeCursorFrame frame = mLeaf;
         if (frame != null) {
             TreeCursorFrame frameCopy = new TreeCursorFrame();
             frame.copyInto(frameCopy);
             copy.mLeaf = frameCopy;
         }
         return copy;
     }
 
     @Override
     public void reset() {
         TreeCursorFrame frame = mLeaf;
         if (frame == null) {
             return;
         }
         mLeaf = null;
         mKey = null;
         mValue = null;
         TreeCursorFrame.popAll(frame);
     }
 
     /**
      * Resets all frames and latches root node, exclusively. Although the
      * normal reset could be called directly, this variant avoids unlatching
      * the root node, since a find operation would immediately relatch it.
      *
      * @return new or recycled frame
      */
     private TreeCursorFrame reset(Node root) {
         TreeCursorFrame frame = mLeaf;
         if (frame == null) {
             root.acquireExclusive();
             return new TreeCursorFrame();
         }
 
         mLeaf = null;
 
         while (true) {
             Node node = frame.acquireExclusive();
             TreeCursorFrame parent = frame.pop();
 
             if (parent == null) {
                 // Usually the root frame refers to the root node, but it
                 // can be wrong if the tree height is changing.
                 if (node != root) {
                     node.releaseExclusive();
                     root.acquireExclusive();
                 }
                 return frame;
             }
 
             node.releaseExclusive();
             frame = parent;
         }
     }
 
     /**
      * Verifies that cursor state is correct by performing a find operation.
      *
      * @return false if unable to verify completely at this time
      */
     boolean verify() throws IOException, IllegalStateException {
         return verify(mKey);
     }
 
     /**
      * Verifies that cursor state is correct by performing a find operation.
      *
      * @return false if unable to verify completely at this time
      * @throws NullPointerException if key is null
      */
     boolean verify(byte[] key) throws IllegalStateException {
         ArrayDeque<TreeCursorFrame> frames;
         {
             TreeCursorFrame frame = mLeaf;
             if (frame == null) {
                 return true;
             }
             frames = new ArrayDeque<TreeCursorFrame>(10);
             do {
                 frames.addFirst(frame);
                 frame = frame.mParentFrame;
             } while (frame != null);
         }
 
         TreeCursorFrame frame = frames.removeFirst();
         Node node = frame.acquireShared();
 
         if (node.mSplit != null) {
             // Cannot verify into split nodes.
             node.releaseShared();
             return false;
         }
 
         /* This check cannot be reliably performed, because the snapshot of
          * frames can be stale.
         if (node != mTree.mRoot) {
             node.releaseShared();
             throw new IllegalStateException("Bottom frame is not at root node");
         }
         */
 
         while (true) {
             if (node.isLeaf()) {
                 int pos = node.binarySearchLeaf(key);
 
                 try {
                     if (frame.mNodePos != pos) {
                         throw new IllegalStateException
                             ("Leaf frame position incorrect: " + frame.mNodePos + " != " + pos);
                     }
 
                     if (pos < 0) {
                         if (frame.mNotFoundKey == null) {
                             throw new IllegalStateException
                                 ("Leaf frame key is not set; pos=" + pos);
                         }
                     } else if (frame.mNotFoundKey != null) {
                         throw new IllegalStateException
                             ("Leaf frame key should not be set; pos=" + pos);
                     }
                 } finally {
                     node.releaseShared();
                 }
 
                 return true;
             }
 
             int childPos = Node.internalPos(node.binarySearchInternal(key));
 
             TreeCursorFrame next;
             try {
                 if (frame.mNodePos != childPos) {
                     throw new IllegalStateException
                         ("Internal frame position incorrect: " +
                          frame.mNodePos + " != " + childPos);
                 }
 
                 if (frame.mNotFoundKey != null) {
                     throw new IllegalStateException("Internal frame key should not be set");
                 }
 
                 next = frames.pollFirst();
 
                 if (next == null) {
                     throw new IllegalStateException("Top frame is not a leaf node");
                 }
 
                 next.acquireShared();
             } finally {
                 node.releaseShared();
             }
 
             frame = next;
             node = frame.mNode;
 
             if (node.mSplit != null) {
                 // Cannot verify into split nodes.
                 node.releaseShared();
                 return false;
             }
         }
     }
 
     /**
      * Checks that leaf is defined and returns it.
      */
     private TreeCursorFrame leaf() {
         TreeCursorFrame leaf = mLeaf;
         if (leaf == null) {
             throw new IllegalStateException("Cursor position is undefined");
         }
         return leaf;
     }
 
     /**
      * Latches and returns leaf frame, which might be split.
      */
     private TreeCursorFrame leafExclusive() {
         TreeCursorFrame leaf = leaf();
         leaf.acquireExclusive();
         return leaf;
     }
 
     /**
      * Latches and returns leaf frame, not split.
      */
     private TreeCursorFrame leafExclusiveNotSplit() throws IOException {
         TreeCursorFrame leaf = leafExclusive();
         if (leaf.mNode.mSplit != null) {
             finishSplit(leaf, leaf.mNode);
         }
         return leaf;
     }
 
     /**
      * Called with exclusive frame latch held, which is retained. Leaf frame is
      * dirtied, any split is finished, and the same applies to all parent
      * nodes. Caller must hold shared commit lock, to prevent deadlock.
      *
      * @return replacement node, still latched
      */
     private Node notSplitDirty(final TreeCursorFrame frame) throws IOException {
         Node node = frame.mNode;
 
         if (node.mSplit != null) {
             // Already dirty, but finish the split.
             return finishSplit(frame, node);
         }
 
         Database db = mTree.mDatabase;
         if (!db.shouldMarkDirty(node)) {
             return node;
         }
 
         TreeCursorFrame parentFrame = frame.mParentFrame;
         if (parentFrame == null) {
             db.doMarkDirty(mTree, node);
             return node;
         }
 
         // Make sure the parent is not split and dirty too.
         Node parentNode;
         doParent: {
             parentNode = parentFrame.tryAcquireExclusive();
             if (parentNode == null) {
                 node.releaseExclusive();
                 parentFrame.acquireExclusive();
             } else if (parentNode.mSplit != null || db.shouldMarkDirty(parentNode)) {
                 node.releaseExclusive();
             } else {
                 break doParent;
             }
             parentNode = notSplitDirty(parentFrame);
             node = frame.acquireExclusive();
         }
 
         while (node.mSplit != null) {
             // Already dirty now, but finish the split. Since parent latch is
             // already held, no need to call into the regular finishSplit
             // method. It would release latches and recheck everything.
             parentNode.insertSplitChildRef(db, parentFrame.mNodePos, node);
             if (parentNode.mSplit != null) {
                 parentNode = finishSplit(parentFrame, parentNode);
             }
             node = frame.acquireExclusive();
         }
         
         if (db.markDirty(mTree, node)) {
             parentNode.updateChildRefId(parentFrame.mNodePos, node.mId);
         }
 
         parentNode.releaseExclusive();
         return node;
     }
 
     /**
      * Caller must hold exclusive latch, which is released by this method.
      */
     private void mergeLeaf(final TreeCursorFrame leaf, Node node) throws IOException {
         TreeCursorFrame parentFrame = leaf.mParentFrame;
         node.releaseExclusive();
 
         if (parentFrame == null) {
             // Root node cannot merge into anything.
             return;
         }
 
         Node parentNode = parentFrame.acquireExclusive();
 
         Node leftNode, rightNode;
         int nodeAvail;
         while (true) {
             if (parentNode.mSplit != null) {
                 parentNode = finishSplit(parentFrame, parentNode);
             }
 
             if (parentNode.numKeys() <= 0) {
                 if (parentNode.mId != Node.STUB_ID) {
                     // FIXME: This shouldn't be a problem when internal nodes can be rebalanced.
                     //System.out.println("tiny internal node: " + (parentNode == mTree.mRoot));
                 }
                 parentNode.releaseExclusive();
                 return;
             }
 
             // Latch leaf and siblings in a strict left-to-right order to avoid deadlock.
             int pos = parentFrame.mNodePos;
             if (pos == 0) {
                 leftNode = null;
             } else {
                 leftNode = latchChild(parentNode, pos - 2, false);
                 if (leftNode.mSplit != null) {
                     // Finish sibling split.
                     parentNode.insertSplitChildRef(mTree.mDatabase, pos - 2, leftNode);
                     continue;
                 }
             }
 
             node = leaf.acquireExclusive();
 
             // Double check that node should still merge.
             if (!node.shouldMerge(nodeAvail = node.availableLeafBytes())) {
                 if (leftNode != null) {
                     leftNode.releaseExclusive();
                 }
                 node.releaseExclusive();
                 parentNode.releaseExclusive();
                 return;
             }
 
             if (pos >= parentNode.highestInternalPos()) {
                 rightNode = null;
             } else {
                 rightNode = latchChild(parentNode, pos + 2, false);
                 if (rightNode.mSplit != null) {
                     // Finish sibling split.
                     if (leftNode != null) {
                         leftNode.releaseExclusive();
                     }
                     node.releaseExclusive();
                     parentNode.insertSplitChildRef(mTree.mDatabase, pos + 2, rightNode);
                     continue;
                 }
             }
 
             break;
         }
 
         // Select a left and right pair, and then don't operate directly on the
         // original node and leaf parameters afterwards. The original node ends
         // up being referenced as a left or right member of the pair.
 
         int leftAvail = leftNode == null ? -1 : leftNode.availableLeafBytes();
         int rightAvail = rightNode == null ? -1 : rightNode.availableLeafBytes();
 
         // Choose adjacent node pair which has the most available space. If
         // only a rebalance can be performed on the pair, operating on
         // underutilized nodes continues them on a path to deletion.
 
         int leftPos;
         if (leftAvail < rightAvail) {
             if (leftNode != null) {
                 leftNode.releaseExclusive();
             }
             leftPos = parentFrame.mNodePos;
             leftNode = node;
             leftAvail = nodeAvail;
         } else {
             if (rightNode != null) {
                 rightNode.releaseExclusive();
             }
             leftPos = parentFrame.mNodePos - 2;
             rightNode = node;
             rightAvail = nodeAvail;
         }
 
         // Left node must always be marked dirty. Parent is already expected to be dirty.
         if (mTree.markDirty(leftNode)) {
             parentNode.updateChildRefId(leftPos, leftNode.mId);
         }
 
         // Determine if both nodes can fit in one node. If so, migrate and
         // delete the right node.
         int remaining = leftAvail + rightAvail - node.mPage.length + Node.TN_HEADER_SIZE;
 
         if (remaining >= 0) {
             // Migrate the entire contents of the right node into the left
             // node, and then delete the right node.
             rightNode.transferLeafToLeftAndDelete(mTree.mDatabase, leftNode);
             parentNode.deleteChildRef(leftPos + 2);
         } else if (false) { // FIXME: testing
             // Rebalance nodes, but don't delete anything. Right node must be dirtied too.
 
             if (mTree.markDirty(rightNode)) {
                 parentNode.updateChildRefId(leftPos + 2, rightNode.mId);
             }
 
             // FIXME: testing
             if (leftNode.numKeys() == 1 || rightNode.numKeys() == 1) {
                 System.out.println("left avail: " + leftAvail + ", right avail: " + rightAvail +
                                    ", left pos: " + leftPos);
                 throw new Error("MUST REBALANCE: " + leftNode.numKeys() + ", " + 
                                 rightNode.numKeys());
             }
 
             /*
             System.out.println("left avail: " + leftAvail + ", right avail: " + rightAvail +
                                ", left pos: " + leftPos + ", mode: " + migrateMode);
             */
 
             if (leftNode == node) {
                 // Rebalance towards left node, which is smaller.
                 // FIXME
             } else {
                 // Rebalance towards right node, which is smaller.
                 // FIXME
             }
         }
 
         mergeInternal(parentFrame, parentNode, leftNode, rightNode);
     }
 
     /**
      * Caller must hold exclusive latch, which is released by this method.
      */
     private void mergeInternal(TreeCursorFrame frame, Node node,
                                Node leftChildNode, Node rightChildNode)
         throws IOException
     {
         up: {
             if (node.shouldInternalMerge()) {
                 if (node.numKeys() > 0 || node != mTree.mRoot) {
                     // Continue merging up the tree.
                     break up;
                 }
 
                 // Delete the empty root node, eliminating a tree level.
 
                 // Note: By retaining child latches (although one has already
                 // been deleted), another thread is prevented from splitting
                 // the lone child. The lone child will become the new root.
                 // TODO: Investigate if this creates deadlocks.
                 node.rootDelete(mTree);
             }
 
             rightChildNode.releaseExclusive();
             leftChildNode.releaseExclusive();
             node.releaseExclusive();
             return;
         }
             
         rightChildNode.releaseExclusive();
         leftChildNode.releaseExclusive();
 
         // At this point, only one node latch is held, and it should merge with
         // a sibling node. Node is guaranteed to be a internal node.
 
         TreeCursorFrame parentFrame = frame.mParentFrame;
         node.releaseExclusive();
 
         if (parentFrame == null) {
             // Root node cannot merge into anything.
             return;
         }
 
         Node parentNode = parentFrame.acquireExclusive();
         if (parentNode.isLeaf()) {
             throw new AssertionError("Parent node is a leaf");
         }
 
         Node leftNode, rightNode;
         int nodeAvail;
         while (true) {
             if (parentNode.mSplit != null) {
                 parentNode = finishSplit(parentFrame, parentNode);
             }
 
             if (parentNode.numKeys() <= 0) {
                 if (parentNode.mId != Node.STUB_ID) {
                     // FIXME: This shouldn't be a problem when internal nodes can be rebalanced.
                     //System.out.println("tiny internal node (2): " + (parentNode == mTree.mRoot));
                 }
                 parentNode.releaseExclusive();
                 return;
             }
 
             // Latch node and siblings in a strict left-to-right order to avoid deadlock.
             int pos = parentFrame.mNodePos;
             if (pos == 0) {
                 leftNode = null;
             } else {
                 leftNode = latchChild(parentNode, pos - 2, false);
                 if (leftNode.mSplit != null) {
                     // Finish sibling split.
                     parentNode.insertSplitChildRef(mTree.mDatabase, pos - 2, leftNode);
                     continue;
                 }
             }
 
             node = frame.acquireExclusive();
 
             // Double check that node should still merge.
             if (!node.shouldMerge(nodeAvail = node.availableInternalBytes())) {
                 if (leftNode != null) {
                     leftNode.releaseExclusive();
                 }
                 node.releaseExclusive();
                 parentNode.releaseExclusive();
                 return;
             }
 
             if (pos >= parentNode.highestInternalPos()) {
                 rightNode = null;
             } else {
                 rightNode = latchChild(parentNode, pos + 2, false);
                 if (rightNode.mSplit != null) {
                     // Finish sibling split.
                     if (leftNode != null) {
                         leftNode.releaseExclusive();
                     }
                     node.releaseExclusive();
                     parentNode.insertSplitChildRef(mTree.mDatabase, pos + 2, rightNode);
                     continue;
                 }
             }
 
             break;
         }
 
         // Select a left and right pair, and then don't operate directly on the
         // original node and frame parameters afterwards. The original node
         // ends up being referenced as a left or right member of the pair.
 
         int leftAvail = leftNode == null ? -1 : leftNode.availableInternalBytes();
         int rightAvail = rightNode == null ? -1 : rightNode.availableInternalBytes();
 
         // Choose adjacent node pair which has the most available space. If
         // only a rebalance can be performed on the pair, operating on
         // underutilized nodes continues them on a path to deletion.
 
         int leftPos;
         if (leftAvail < rightAvail) {
             if (leftNode != null) {
                 leftNode.releaseExclusive();
             }
             leftPos = parentFrame.mNodePos;
             leftNode = node;
             leftAvail = nodeAvail;
         } else {
             if (rightNode != null) {
                 rightNode.releaseExclusive();
             }
             leftPos = parentFrame.mNodePos - 2;
             rightNode = node;
             rightAvail = nodeAvail;
         }
 
         if (leftNode == null || rightNode == null) {
             throw new AssertionError("No sibling node to merge into");
         }
 
         // Left node must always be marked dirty. Parent is already expected to be dirty.
         if (mTree.markDirty(leftNode)) {
             parentNode.updateChildRefId(leftPos, leftNode.mId);
         }
 
         // Determine if both nodes plus parent key can fit in one node. If so,
         // migrate and delete the right node.
         byte[] parentPage = parentNode.mPage;
         int parentEntryLoc = DataIO.readUnsignedShort
             (parentPage, parentNode.mSearchVecStart + leftPos);
         int parentEntryLen = Node.internalEntryLength(parentPage, parentEntryLoc);
         int remaining = leftAvail - parentEntryLen
             + rightAvail - parentPage.length + (Node.TN_HEADER_SIZE - 2);
 
         if (remaining >= 0) {
             // Migrate the entire contents of the right node into the left
             // node, and then delete the right node.
             rightNode.transferInternalToLeftAndDelete
                 (mTree.mDatabase, leftNode, parentPage, parentEntryLoc, parentEntryLen);
             parentNode.deleteChildRef(leftPos + 2);
         } else if (false) { // FIXME: testing
             // Rebalance nodes, but don't delete anything. Right node must be dirtied too.
 
             if (mTree.markDirty(rightNode)) {
                 parentNode.updateChildRefId(leftPos + 2, rightNode.mId);
             }
 
             // FIXME: testing
             if (leftNode.numKeys() == 1 || rightNode.numKeys() == 1) {
                 System.out.println("left avail: " + leftAvail + ", right avail: " + rightAvail +
                                    ", left pos: " + leftPos);
                 throw new Error("MUST REBALANCE: " + leftNode.numKeys() + ", " + 
                                 rightNode.numKeys());
             }
 
             /*
             System.out.println("left avail: " + leftAvail + ", right avail: " + rightAvail +
                                ", left pos: " + leftPos + ", mode: " + migrateMode);
             */
 
             if (leftNode == node) {
                 // Rebalance towards left node, which is smaller.
                 // FIXME
             } else {
                 // Rebalance towards right node, which is smaller.
                 // FIXME
             }
         }
 
         // Tail call. I could just loop here, but this is simpler.
         mergeInternal(parentFrame, parentNode, leftNode, rightNode);
     }
 
     /**
      * Caller must hold exclusive latch and it must verify that node has split.
      *
      * @return replacement node, still latched
      */
     private Node finishSplit(final TreeCursorFrame frame, Node node) throws IOException {
         Tree tree = mTree;
         Database db = tree.mDatabase;
 
         // FIXME: How to acquire shared commit lock without deadlock?
         if (node == tree.mRoot) {
             Node stub;
             if (tree.hasStub()) {
                 // FIXME: Use tryPopStub first, to avoid deadlock.
                 stub = tree.validateStub(tree.popStub());
             } else {
                 stub = null;
             }
             node.finishSplitRoot(db, stub);
             return node;
         }
 
         final TreeCursorFrame parentFrame = frame.mParentFrame;
         node.releaseExclusive();
 
         // To avoid deadlock, ensure shared commit lock is held. Not all
         // callers acquire the shared lock first, since they usually only read
         // from the tree. Node latch has now been released, which should have
         // been the only latch held, and so commit lock can be acquired without
         // deadlock.
 
         final Lock sharedCommitLock = db.sharedCommitLock();
         sharedCommitLock.lock();
         try {
             Node parentNode = parentFrame.acquireExclusive();
             while (true) {
                 if (parentNode.mSplit != null) {
                     parentNode = finishSplit(parentFrame, parentNode);
                 }
                 node = frame.acquireExclusive();
                 if (node.mSplit == null) {
                     parentNode.releaseExclusive();
                     return node;
                 }
                 parentNode.insertSplitChildRef(db, parentFrame.mNodePos, node);
             }
         } finally {
             sharedCommitLock.unlock();
         }
     }
 
     /**
      * With parent held exclusively, returns child with exclusive latch held.
      */
     private Node latchChild(Node parent, int childPos, boolean releaseParent)
         throws IOException
     {
         Node childNode = parent.mChildNodes[childPos >> 1];
         long childId = parent.retrieveChildRefId(childPos);
 
         check: if (childNode != null && childId == childNode.mId) {
             childNode.acquireExclusive();
 
             // Need to check again in case evict snuck in.
             if (childId != childNode.mId) {
                 childNode.releaseExclusive();
                 break check;
             }
 
             if (releaseParent) {
                 parent.releaseExclusive();
             }
 
             mTree.mDatabase.used(childNode);
             return childNode;
         }
 
         // If this point is reached, child needs to be loaded.
 
         childNode = mTree.mDatabase.allocLatchedNode();
         childNode.mId = childId;
         parent.mChildNodes[childPos >> 1] = childNode;
 
         // Release parent latch before child has been loaded. Any threads
         // which wish to access the same child will block until this thread
         // has finished loading the child and released its exclusive latch.
         if (releaseParent) {
             parent.releaseExclusive();
         }
 
         // FIXME: Don't hold latch during load. Instead, use an object for
         // holding state, and include a "loading" state. As other threads see
         // this state, they replace the state object with a linked stack of
         // parked threads. When the load is finished, all waiting threads are
         // unparked. Move some of this logic into a common Node.load method.
 
         try {
             childNode.read(mTree.mDatabase, childId);
         } catch (IOException e) {
             // Another thread might access child and see that it is invalid because
             // id is zero. It will assume it got evicted and will load child again.
             childNode.mId = 0;
             childNode.releaseExclusive();
             throw e;
         }
 
         mTree.mDatabase.used(childNode);
         return childNode;
     }
 }
 
