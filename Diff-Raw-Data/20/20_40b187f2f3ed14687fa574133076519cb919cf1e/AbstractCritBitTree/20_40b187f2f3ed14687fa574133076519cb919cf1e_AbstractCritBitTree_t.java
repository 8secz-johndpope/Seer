 package io.prelink.critbit;
 
 
 import java.io.Serializable;
 import java.util.Map;
 
 import org.ardverk.collection.Cursor;
 import org.ardverk.collection.Cursor.Decision;
 import org.ardverk.collection.KeyAnalyzer;
 
 
 /**
  * An OO/Functional Java crit-bit tree, inspired by
  * djb (http://cr.yp.to/critbit.html),
  * Adam Langley (https://github.com/agl/critbit),
  * and Okasaki (http://www.eecs.usma.edu/webs/people/okasaki/pubs.html)
  */
 abstract class AbstractCritBitTree<K,V> implements Serializable {
 
     static final long serialVersionUID = 20110212L;
 
     static interface NodeFactory<K,V> extends Serializable {
         Node<K,V> mkShortBoth(int diffBit, K lk, V lv, K rk, V rv);
         Node<K,V> mkShortRight(int diffBit, Node<K,V> left, K k, V v);
         Node<K,V> mkShortLeft(int diffBit, K k, V v, Node<K,V> right);
         Node<K,V> mkTall(int diffBit, Node<K,V> left, Node<K,V> right);
         Node<K,V> mkLeaf(K key, V val);
     }
 
     static class Context<K,V> implements Serializable {
         private static final long serialVersionUID = 20110212L;
         final KeyAnalyzer<K> chk;
         final NodeFactory<K,V> nf;
         Context(KeyAnalyzer<K> chk, NodeFactory<K,V> nf) {
             this.chk = chk;
             this.nf = nf;
         }
     }
 
     static interface Node<K,V> extends Serializable {
         //Everybody implements these.
         Node<K,V> insert(int diffBit, K key, V val, Context<K,V> ctx);
         Node<K,V> remove(K key, Context<K,V> ctx, boolean force);
         boolean isInternal();
 
         //Should only be called for internal nodes.
         int bit();
         Direction next(K key, Context<K,V> ctx);
         Node<K,V> nextNode(K key, Context<K,V> ctx);
         Node<K,V> left(Context<K,V> ctx);
         Node<K,V> right(Context<K,V> ctx);
         Node<K,V> setLeft(int diffBit, K key, V val, Context<K,V> ctx);
         Node<K,V> setRight(int diffBit, K key, V val, Context<K,V> ctx);
         boolean hasExternalLeft();
         boolean hasExternalRight();
 
         //Should only be called for external nodes.
         K getKey();
         V getValue();
     }
 
     static enum Direction {
         LEFT, RIGHT
     }
 
     static abstract class BaseNode<K,V> implements Node<K,V> {
         private static final long serialVersionUID = 20110212L;
         public Node<K, V> insert(int diffBit, K key, V val, Context<K, V> ctx) {
             throw new UnsupportedOperationException();
         }
         public int bit() {
             throw new UnsupportedOperationException();
         }
         public Direction next(K key, Context<K, V> ctx) {
             throw new UnsupportedOperationException();
         }
         public Node<K,V> nextNode(K key, Context<K, V> ctx) {
             throw new UnsupportedOperationException();
         }
         public Node<K,V> left(Context<K, V> ctx) {
             throw new UnsupportedOperationException();
         }
         public Node<K,V> right(Context<K, V> ctx) {
             throw new UnsupportedOperationException();
         }
         public Node<K,V> setLeft(int diffBit, K key, V val, Context<K, V> ctx) {
             throw new UnsupportedOperationException();
         }
         public Node<K,V> setRight(int diffBit, K key, V val, Context<K, V> ctx) {
             throw new UnsupportedOperationException();
         }
         public boolean hasExternalLeft() {
             throw new UnsupportedOperationException();
         }
         public boolean hasExternalRight() {
             throw new UnsupportedOperationException();
         }
         public K getKey() {
             throw new UnsupportedOperationException();
         }
         public V getValue() {
             throw new UnsupportedOperationException();
         }
     }
 
     static abstract class AbstractInternal<K,V> extends BaseNode<K,V> {
         private static final long serialVersionUID = 20110212L;
 
         private final int bit;
         AbstractInternal(int bit) {
             this.bit = bit;
         }
 
         public final int bit() { return bit; }
 
         public final Node<K,V> insert(int diffBit, K k, V v, Context<K,V> ctx) {
             if(diffBit >= 0 && diffBit < bit()) {
                 if(ctx.chk.isBitSet(k, diffBit)) {
                     return ctx.nf.mkShortRight(diffBit, this, k, v);
                 } else {
                     return ctx.nf.mkShortLeft(diffBit, k, v, this);
                 }
             } else {
                 if(ctx.chk.isBitSet(k, bit())) {
                     return setRight(diffBit, k, v, ctx);
                 } else {
                     return setLeft(diffBit, k, v, ctx);
                 }
             }
         }
 
         public Node<K,V> remove(K key, Context<K,V> ctx, boolean force) {
             switch(next(key, ctx)) {
             case LEFT:
                 return removeLeft(key, ctx, force);
             default:
                 return removeRight(key, ctx, force);
             }
         }
 
         public final Direction next(K key, Context<K,V> ctx) {
             return ctx.chk.isBitSet(key, bit()) ? Direction.RIGHT
                                                 : Direction.LEFT;
         }
 
         public final Node<K,V> nextNode(K key, Context<K,V> ctx) {
             switch(next(key, ctx)) {
             case LEFT: return left(ctx);
             default: return right(ctx);
             }
         }
 
         public final boolean isInternal() { return true; }
 
         protected abstract Node<K,V> removeLeft(K key, Context<K,V> ctx, boolean force);
         protected abstract Node<K,V> removeRight(K key, Context<K,V> ctx, boolean force);
 
         protected final Node<K,V> mkShortBothChild(int diffBit,
                                                    K newKey, V newVal,
                                                    K oldKey, V oldVal,
                                                    Context<K,V> ctx) {
             boolean newGoesRight = ctx.chk.isBitSet(newKey, diffBit);
             K rKey = newGoesRight ? newKey : oldKey;
             V rVal = newGoesRight ? newVal : oldVal;
             K lKey = newGoesRight ? oldKey : newKey;
             V lVal = newGoesRight ? oldVal : newVal;
             return ctx.nf.mkShortBoth(diffBit, lKey, lVal, rKey, rVal);
         }
     }
 
     static final class LeafNode<K,V>
         extends BaseNode<K,V>
         implements Map.Entry<K,V>
     {
         private static final long serialVersionUID = 20110212L;
         private final K key;
         private final V value;
         public LeafNode(K key, V value) {
             this.key = key;
             this.value = value;
         }
         public K getKey() { return this.key; }
         public V getValue() { return this.value; }
         public V setValue(V arg0) {
             throw new UnsupportedOperationException();
         }
         public Node<K,V> insert(int diffBit, K key, V val, Context<K,V> ctx) {
             if(diffBit < 0) {
                 return ctx.nf.mkLeaf(key, val);
             }
             else if(ctx.chk.isBitSet(key, diffBit)) { //new key goes right
                 return ctx.nf.mkShortBoth(diffBit, this.key, this.value, key, val);
             } else { //new key goes left
                 return ctx.nf.mkShortBoth(diffBit, key, val, this.key, this.value);
             }
         }
         public boolean isInternal() { return false; }
         public Node<K,V> remove(K key, Context<K,V> ctx, boolean force) {
             if(force || ctx.chk.bitIndex(key, this.key) < 0) {
                 return null;
             } else {
                 return this;
             }
         }
 
     }
 
     static final class ShortBothNode<K,V> extends AbstractInternal<K,V> {
         private static final long serialVersionUID = 20110212L;
         private final K leftKey;
         private final V leftVal;
         private final K rightKey;
         private final V rightVal;
         public ShortBothNode(int bit, K leftKey, V leftVal, K rightKey, V rightVal) {
             super(bit);
             this.leftKey = leftKey;
             this.leftVal = leftVal;
             this.rightKey = rightKey;
             this.rightVal = rightVal;
         }
         public Node<K,V> left(Context<K,V> ctx) { return ctx.nf.mkLeaf(leftKey, leftVal); }
         public Node<K,V> right(Context<K,V> ctx) { return ctx.nf.mkLeaf(rightKey, rightVal); }
         public Node<K,V> setLeft(int diffBit, K key, V val, Context<K,V> ctx) {
             if(diffBit < 0) {
                 return ctx.nf.mkShortBoth(bit(), key, val, rightKey, rightVal);
             }
             Node<K,V> newLeft = mkShortBothChild(diffBit, key, val, leftKey, leftVal, ctx);
             return ctx.nf.mkShortRight(bit(), newLeft, rightKey, rightVal);
         }
         public Node<K,V> setRight(int diffBit, K key, V val, Context<K,V> ctx) {
             if(diffBit < 0) {
                 return ctx.nf.mkShortBoth(bit(), leftKey, leftVal, key, val);
             }
             Node<K,V> newRight = mkShortBothChild(diffBit, key, val, rightKey, rightVal, ctx);
             return ctx.nf.mkShortLeft(bit(), leftKey, leftVal, newRight);
         }
         protected Node<K,V> removeLeft(K key, Context<K,V> ctx, boolean force) {
             if(force || ctx.chk.bitIndex(key, this.leftKey) < 0) {
                 return ctx.nf.mkLeaf(rightKey, rightVal);
             } else {
                 return this;
             }
         }
         protected Node<K,V> removeRight(K key, Context<K,V> ctx, boolean force) {
             if(force || ctx.chk.bitIndex(key, this.rightKey) < 0) {
                 return ctx.nf.mkLeaf(leftKey, leftVal);
             } else {
                 return this;
             }
         }
         public boolean hasExternalLeft() { return true; }
         public boolean hasExternalRight() { return true; }
     }
 
     private final Context<K,V> ctx;
 
     AbstractCritBitTree(Context<K,V> context) {
         this.ctx = context;
     }
 
     abstract Node<K,V> root();
 
     Context<K,V> ctx() {
         return ctx;
     }
 
     static final class SearchResult<K,V> {
         final Node<K,V> parent;
         final Direction pDirection;
         final Node<K,V> result;
         final Direction rDirection;
         public SearchResult(Node<K,V> parent,
                             Direction pDirection,
                             Node<K,V> result,
                             Direction rDirection) {
             this.parent = parent;
             this.pDirection = pDirection;
             this.result = result;
             this.rDirection = rDirection;
         }
         K key(Context<K,V> ctx) {
             switch(rDirection) {
             case LEFT:
                 return result.left(ctx).getKey();
             default: //case RIGHT:
                 return result.right(ctx).getKey();
             }
         }
         V value(Context<K,V> ctx) {
             switch(rDirection) {
             case LEFT:
                 return result.left(ctx).getValue();
             default: //case RIGHT:
                 return result.right(ctx).getValue();
             }
         }
     }
 
     final SearchResult<K,V> search(final Node<K,V> start, final K key) {
         Node<K,V> par = null;
         Direction parDirection = null;
         Node<K,V> cur = start;
         for(;;) {
             switch(cur.next(key, ctx)) {
             case LEFT:
                 if(cur.hasExternalLeft()) {
                     return new SearchResult<K,V>(par, parDirection, cur, Direction.LEFT);
                 }
                 par = cur;
                 parDirection = Direction.LEFT;
                 cur = cur.left(ctx);
                 break;
             case RIGHT:
                 if(cur.hasExternalRight()) {
                     return new SearchResult<K,V>(par, parDirection, cur, Direction.RIGHT);
                 }
                 par = cur;
                 parDirection = Direction.RIGHT;
                 cur = cur.right(ctx);
                 break;
             }
         }
     }
 
     public final V get(K key) {
         if(root() == null) {
             return null;
         }
         if(!root().isInternal()) {
             return root().getValue();
         }
         SearchResult<K,V> sr = search(root(), key);
         switch(sr.rDirection) {
         case LEFT:
             return sr.result.left(ctx).getValue();
         default: //case RIGHT:, but we need to convince compiler we return.
             return sr.result.right(ctx).getValue();
         }
     }
 
     public final Map.Entry<K,V> min() {
         if(root() == null) {
             return null;
         }
         Node<K,V> current = root();
         while(current.isInternal()) {
             current = current.left(ctx());
         }
         return cast(current);
     }
 
     public final Map.Entry<K,V> max() {
         if(root() == null) {
             return null;
         }
         Node<K,V> current = root();
         while(current.isInternal()) {
             current = current.right(ctx());
         }
         return cast(current);
     }
 
     public final void traverse(Cursor<? super K, ? super V> cursor) {
         if(root() == null) {
             return;
         }
         doTraverse(root(), cursor);
     }
 
     public final void traverseWithPrefix(K key,
                                          Cursor<? super K, ? super V> cursor) {
         if(root() == null) {
             return;
         }
         if(!root().isInternal()) {
             Map.Entry<K,V> e = cast(root());
             cursor.select(e);
             return;
         }
 
         int keyLen = ctx.chk.lengthInBits(key);
         Node<K,V> current = root();
         Node<K,V> top = current;
         while(current.isInternal()) {
             switch(current.next(key,ctx)) {
             case LEFT:
                 current = current.left(ctx);
                 break;
             case RIGHT:
                 current = current.right(ctx);
                 break;
             }
             if(current.bit() < keyLen) {
                 top = current;
             }
         }
         if(!ctx.chk.isPrefix(current.getKey(), key)) {
             return;
         }
 
         doTraverse(top, cursor);
     }
 
     protected abstract Decision doTraverse(Node<K,V> top,
                                            Cursor<? super K, ? super V> cursor);
 
     public abstract int size();
     public boolean isEmpty() { return size() == 0; }
 
     @SuppressWarnings("unchecked")
    static <T> T cast(Object obj) {
        return (T)obj;
     }
 }
