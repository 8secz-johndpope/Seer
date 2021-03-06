 package org.basex.data;
 
 import static org.basex.util.Token.*;
 import java.io.IOException;
 import java.util.Arrays;
 import org.basex.core.proc.InfoTable;
 import org.basex.index.Index;
 import org.basex.index.IndexIterator;
 import org.basex.index.IndexToken;
 import org.basex.index.Names;
 import org.basex.io.IO;
 import org.basex.io.TableAccess;
 import org.basex.util.Atts;
 import org.basex.util.IntList;
 import org.basex.util.TokenBuilder;
 import org.deepfs.fs.DeepFS;
 
 /**
  * This class provides access to the database storage.
  * Note that the methods of this class are optimized for performance.
  * They will not check if correct data is requested, i.e. if a text is
  * requested, a pre value must points to a text node.
  *
  * All nodes in the table are accessed by their
  * implicit pre value. The following restrictions are imposed on the data:
  * <ul>
  * <li>The table is limited to 2^31 entries (pre values are signed int's)</li>
  * <li>A maximum of 2^15 different tag and attribute names is allowed</li>
  * <li>A maximum of 2^8 different namespaces is allowed</li>
  * <li>A tag can have a maximum of 32 attributes</li>
  * </ul>
  * Each node occupies 128 bits. The current storage layout looks as follows:
  *
  * <pre>
  * COMMON ATTRIBUTES:
  * - Byte     0:  KIND: Node kind (2-0)
  * ELEMENT NODES:
  * - Byte     0:  ATTS: Number of attributes (7-3)
  * - Byte   1-2:  NAME: Namespace Flag (15), Name (14-0)
  * - Byte     3:  NURI: Namespace URI
  * - Byte  4- 7:  DIST: Distance to parent node
  * - Byte  8-11:  SIZE: Number of descendants
  * - Byte 12-15:  UNID: Unique Node ID
  * DOCUMENT NODES:
  * - Byte  3- 7:  TEXT: Text reference
  * - Byte  8-11:  SIZE: Number of descendants
  * - Byte 12-15:  UNID: Unique Node ID
  * TEXT, COMMENT, PI NODES:
  * - Byte  3- 7:  TEXT: Text reference
  * - Byte  8-11:  DIST: Distance to parent node
  * - Byte 12-15:  UNID: Unique Node ID
  * ATTRIBUTE NODES:
  * - Byte     0:  DIST: Distance to parent node (7-3)
  * - Byte   1-2:  NAME: Namespace Flag (15), Name (14-0)
  * - Byte  3- 7:  TEXT: Attribute value reference
  * - Byte    11:  NURI: Namespace (7-3)
  * - Byte 12-15:  UNID: Unique Node ID
  * </pre>
  *
  * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
  * @author Christian Gruen
  */
 public abstract class Data {
   /** Node kind: Document. */
   public static final byte DOC = 0x00;
   /** Node kind: Element. */
   public static final byte ELEM = 0x01;
   /** Node kind: Text. */
   public static final byte TEXT = 0x02;
   /** Node kind: Attribute. */
   public static final byte ATTR = 0x03;
   /** Node kind: Comment. */
   public static final byte COMM = 0x04;
   /** Node kind: Processing Instruction. */
   public static final byte PI = 0x05;
 
   /** Index types. */
   public enum IndexType {
     /** Attribute index. */ ATN,
     /** Tag index.       */ TAG,
     /** Text index.      */ TXT,
     /** Attribute index. */ ATV,
     /** Full-text index. */ FTX,
     /** Path index. */      PTH
   };
 
   /** Meta data. */
   public MetaData meta;
   /** Tag index. */
   public Names tags;
   /** Attribute name index. */
   public Names atts;
   /** Namespace index. */
   public Namespaces ns;
   /** Path Summary. */
   public PathSummary path;
 
   /** File system reference. */
   public DeepFS fs;
   /** Index Reference for name tag. */
   public int nameID;
   /** Index References. */
   public int sizeID;
 
   /** Table access file. */
   protected TableAccess table;
   /** Text index. */
   protected Index txtindex;
   /** Attribute value index. */
   protected Index atvindex;
   /** Full-text index instance. */
   protected Index ftxindex;
 
   /**
    * Dissolves the references to often used tag names and attributes.
    * @throws IOException I/O exception
    */
   @SuppressWarnings("unused")
   public void init() throws IOException {
     if(meta.deepfs) fs = new DeepFS(this);
     nameID = atts.id(DataText.NAME);
     sizeID = atts.id(DataText.SIZE);
   }
 
   /**
    * Closes the current database.
    * @throws IOException I/O exception
    */
   public final void close() throws IOException {
     if(fs != null) fs.close();
     cls();
   }
 
   /**
    * Internal method to close the database.
    * @throws IOException I/O exception
    */
   protected abstract void cls() throws IOException;
 
   /**
    * Flushes the table data.
    */
   public abstract void flush();
 
   /**
    * Closes the specified index.
    * @param index index to be closed
    * @throws IOException I/O exception
    */
   public abstract void closeIndex(IndexType index) throws IOException;
 
   /**
    * Assigns the specified index.
    * @param type index to be opened
    * @param ind index instance
    */
   public abstract void setIndex(IndexType type, Index ind);
 
   /**
    * Returns the indexed id references for the specified token.
    * @param token index token reference
    * @return id array
    */
   public final IndexIterator ids(final IndexToken token) {
     switch(token.type()) {
       case TXT: return txtindex.ids(token);
       case ATV: return atvindex.ids(token);
       case FTX: return ftxindex.ids(token);
       default:  return null;
     }
   }
 
   /**
    * Returns the number of indexed id references for the specified token.
    * @param token text to be found
    * @return id array
    */
   public final int nrIDs(final IndexToken token) {
     // token too long.. no results can be expected
     if(token.get().length > MAXLEN) return Integer.MAX_VALUE;
     switch(token.type()) {
       case TXT: return txtindex.nrIDs(token);
       case ATV: return atvindex.nrIDs(token);
       case FTX: return ftxindex.nrIDs(token);
       default:  return Integer.MAX_VALUE;
     }
   }
 
   /**
    * Returns the document nodes.
    * @return root nodes
    */
   public final int[] doc() {
     final IntList il = new IntList();
     for(int i = 0; i < meta.size; i += size(i, Data.DOC)) il.add(i);
     return il.finish();
   }
 
   /**
    * Returns info on the specified index structure.
    * @param type index type
    * @return info
    */
   public final byte[] info(final IndexType type) {
     switch(type) {
       case TAG: return tags.info();
       case ATN: return atts.info();
       case TXT: return txtindex.info();
       case ATV: return atvindex.info();
       case FTX: return ftxindex.info();
       case PTH: return path.info(this);
       default: return EMPTY;
     }
   }
 
   /**
    * Returns an atomized content for any node kind.
    * The atomized value can be an attribute value or XML content.
    * @param pre pre value
    * @return atomized value
    */
   public final byte[] atom(final int pre) {
     switch(kind(pre)) {
       case TEXT: case COMM:
         return text(pre, true);
       case ATTR:
         return text(pre, false);
       case PI:
         final byte[] txt = text(pre, true);
         final int i = indexOf(txt, ' ');
         return i == -1 ? EMPTY : substring(txt, i + 1);
       default:
         // create atomized text node
         final TokenBuilder tb = new TokenBuilder();
         int p = pre;
         final int s = p + size(p, kind(p));
         while(p != s) {
           final int k = kind(p);
           if(k == TEXT) tb.add(text(p, true));
           p += attSize(p, k);
         }
         return tb.finish();
     }
   }
 
   // RETRIEVING VALUES ========================================================
 
   /**
    * Returns a pre value.
    * @param id unique node id
    * @return pre value or -1 if id was not found
    */
   public final int pre(final int id) {
     // find pre value in table
     for(int p = id; p < meta.size; p++) if(id == id(p)) return p;
     for(int p = 0; p < id; p++) if(id == id(p)) return p;
     // id not found
     return -1;
   }
 
   /**
    * Returns a unique node id.
    * @param pre pre value
    * @return node id
    */
   public final int id(final int pre) {
     return table.read4(pre, 12);
   }
 
   /**
    * Returns a node kind.
    * @param pre pre value
    * @return node kind
    */
   public final int kind(final int pre) {
     return table.read1(pre, 0) & 0x07;
   }
 
   /**
    * Returns a pre value of the parent node.
    * @param pre pre value
    * @param k node kind
    * @return pre value of the parent node
    */
   public final int parent(final int pre, final int k) {
     return pre - dist(pre, k);
   }
 
   /**
    * Returns the distance of the specified node.
    * @param pre pre value
    * @param k node kind
    * @return distance
    */
   private int dist(final int pre, final int k) {
     switch(k) {
       case ELEM: return table.read4(pre, 4);
       case TEXT:
       case COMM:
       case PI:   return table.read4(pre, 8);
       case ATTR: return table.read1(pre, 0) >> 3 & 0x1F;
       default:   return pre + 1;
     }
   }
 
   /**
    * Returns a size value (number of descendant table entries).
    * @param pre pre value
    * @param k node kind
    * @return size value
    */
   public final int size(final int pre, final int k) {
     return k == ELEM || k == DOC ? table.read4(pre, 8) : 1;
   }
 
   /**
    * Returns a number of attributes.
    * @param pre pre value
    * @param k node kind
    * @return number of attributes
    */
   public final int attSize(final int pre, final int k) {
     return k == ELEM ? table.read1(pre, 0) >> 3 & 0x1F : 1;
   }
 
   /**
    * Returns a reference to the tag or attribute name id.
    * @param pre pre value
    * @return token reference
    */
   public final int name(final int pre) {
     return table.read2(pre, 1) & 0x7FFF;
   }
 
   /**
    * Returns a tag, attribute or pi name.
    * @param pre pre value
    * @param k node kind
    * @return name reference
    */
   public final byte[] name(final int pre, final int k) {
     if(k == Data.PI) {
       final byte[] name = text(pre, true);
       final int i = indexOf(name, ' ');
       return i == -1 ? name : substring(name, 0, i);
     }
     return (k == Data.ELEM ? tags : atts).key(name(pre));
   }
 
   /**
    * Returns a reference to the tag or attribute namespace URI.
    * @param pre pre value
    * @return token reference
    * @param k node kind
    */
   public final int uri(final int pre, final int k) {
     return k == ELEM || k == ATTR ?
         table.read1(pre, k == ELEM ? 3 : 11) & 0xFF : 0;
   }
 
   /**
    * Returns a namespace flag.
    * Should be only called for element nodes.
    * @param pre pre value
    * @return namespace flag
    */
   public final boolean nsFlag(final int pre) {
     return (table.read1(pre, 1) & 0x80) != 0;
   }
 
   /**
    * Returns namespace key and value ids.
    * Should be only called for element nodes.
    * @param pre pre value
    * @return key and value ids
    */
   public final Atts ns(final int pre) {
     final Atts as = new Atts();
     if(nsFlag(pre)) {
       final int[] nsp = ns.get(pre);
       for(int n = 0; n < nsp.length; n += 2)
         as.add(ns.pref(nsp[n]), ns.uri(nsp[n + 1]));
     }
     return as;
   }
 
   /**
    * Returns the disk offset of a text (text, comment, pi) or attribute value.
    * @param pre pre value
    * @return disk offset
    */
   protected final long textOff(final int pre) {
     return table.read5(pre, 3);
   }
 
   /**
    * Returns a text (text, comment, pi) or attribute value.
    * @param pre pre value
    * @param text text/attribute flag
    * @return atomized value
    */
   public abstract byte[] text(int pre, boolean text);
 
   /**
    * Returns a text (text, comment, pi) as double value.
    * @param pre pre value
    * @param text text/attribute flag
    * @return numeric value
    */
   public abstract double textNum(int pre, boolean text);
 
   /**
    * Returns a text (text, comment, pi) length.
    * @param pre pre value
    * @param text text/attribute flag
    * @return length
    */
   public abstract int textLen(int pre, boolean text);
 
   // UPDATE OPERATIONS ========================================================
 
   /**
    * Renames (updates) an element, attribute or pi name.
    * @param pre pre value
    * @param k node kind
    * @param nm new tag, attribute or pi name
    * @param uri uri
    */
   public final void rename(final int pre, final int k, final byte[] nm,
       final byte[] uri) {
     meta.update();
     if(k == Data.PI) {
       text(pre, trim(concat(nm, SPACE, atom(pre))), true);
     } else {
       // update/set namespace reference
       final int ou = ns.uri(nm, pre);
       final boolean ne = ou == 0 && uri.length != 0;
       final int p = k == Data.ATTR ? parent(pre, k) : pre;
       final int u = ne ? ns.add(p, p, pref(nm), uri) :
         ou != 0 && eq(ns.uri(ou), uri) ? ou : 0;
 
       // write namespace uri reference
       table.write1(pre, k == ELEM ? 3 : 11, u);
       // write name reference
       table.write2(pre, 1, (nsFlag(pre) ? 1 << 15 : 0) |
         (k == ELEM ? tags : atts).index(nm, null, false));
       // write namespace flag
       table.write2(p, 1, (ne || nsFlag(p) ? 1 << 15 : 0) | name(p));
     }
   }
 
   /**
    * Replaces (updates) a text, comment, pi or attribute value.
    * @param pre pre value
    * @param k node kind
    * @param val value to be updated (tag name, text, comment, pi)
    */
   public final void replace(final int pre, final int k, final byte[] val) {
     meta.update();
     final byte[] v = k == PI ? trim(concat(name(pre, k), SPACE, val)) : val;
     text(pre, v, k != ATTR);
   }
 
   /**
    * Deletes a node and its descendants.
    * @param pre pre value of the node to delete
    */
   public final void delete(final int pre) {
     meta.update();
 
     // size of the subtree to delete
     int k = kind(pre);
     int s = size(pre, k);
     ns.delete(pre, s);
 
     // reduce size of ancestors
     int par = pre;
     // check if we are an attribute (different size counters)
     if(k == ATTR) {
       par = parent(par, ATTR);
       attSize(par, ELEM, attSize(par, ELEM) - 1);
       size(par, ELEM, size(par, ELEM) - 1);
       k = kind(par);
     }
 
     // reduce size of ancestors
     while(par > 0 && k != DOC) {
       par = parent(par, k);
       k = kind(par);
       size(par, k, size(par, k) - s);
     }
 
     // preserve empty root node
     int p = pre;
     final boolean empty = p == 0 && s == meta.size;
     if(empty) {
       p++;
       s--;
     } else {
       if(kind(p) == DOC) meta.ndocs--;
     }
 
     // delete node from table structure and reduce document size
     table.delete(pre, s);
     updateDist(p, -s);
 
     // restore empty document node
     if(empty) {
       doc(0, 1, EMPTY);
       table.set(0, f());
     }
   }
 
   /**
    * Inserts attributes.
    * @param pre pre value
    * @param par parent of node
    * @param dt data instance to copy from
    */
   public final void insertAttr(final int pre, final int par, final MemData dt) {
     meta.update();
     insert(pre, par, dt);
     attSize(par, ELEM, attSize(par, ELEM) + dt.meta.size);
   }
 
   /**
    * Inserts a data instance at the specified pre value.
    * Note that the specified data instance must differ from this instance.
    * @param ipre value at which to insert new data
    * @param ipar parent pre value of node
    * @param md data instance to copy from
    */
   public final void insert(final int ipre, final int ipar, final MemData md) {
     meta.update();
 
     final int[] preStack = new int[IO.MAXHEIGHT];
     int l = 0;
 
     final int ms = md.meta.size;
     final int buf = Math.min(ms, IO.BLOCKSIZE >> IO.NODEPOWER);
     // resize buffer to cache more entries
     buffer(buf);
 
     // loop through all entries
     int mpre = -1;
     while(++mpre != ms) {
       if(mpre != 0 && mpre % buf == 0) insert(ipre + mpre - buf);
 
       final int mk = md.kind(mpre);
       final int mpar = md.parent(mpre, mk);
       final int pre = ipre + mpre;
       final int dis = mpar >= 0 ? mpre - mpar : pre - ipar;
       final int par = pre - dis;
       while(l > 0 && preStack[l - 1] > par) ns.close(preStack[--l]);
 
       switch(mk) {
         case DOC:
           // add document
           doc(pre, md.size(mpre, mk), md.text(mpre, true));
           meta.ndocs++;
           ns.open();
           preStack[l++] = pre;
           break;
         case ELEM:
           // add element
           final boolean ne = md.nsFlag(mpre);
           if(ne) {
             final Atts at = md.ns(mpre);
             for(int a = 0; a < at.size; a++) ns.add(at.key[a], at.val[a], pre);
           }
           ns.open();
           byte[] nm = md.name(mpre, mk);
           elem(dis, tags.index(nm, null, false), md.attSize(mpre, mk),
               md.size(mpre, mk), ns.uri(nm, true), ne);
           preStack[l++] = pre;
           break;
         case TEXT:
         case COMM:
         case PI:
           // add text
           text(pre, dis, md.text(mpre, true), mk);
           break;
         case ATTR:
           // add attribute
           nm = md.name(mpre, mk);
           if(md.nsFlag(mpre)) {
             ns.add(par, l == 0 ? ipar : preStack[l - 1], pref(nm),
                 md.ns.uri(md.uri(mpre, mk)));
             table.write2(ipar, 1, 1 << 15 | name(ipar));
           }
           attr(pre, dis, atts.index(nm, null, false), md.text(mpre, false),
               ns.uri(nm, false), false);
           break;
       }
     }
     
     while(l > 0) ns.close(preStack[--l]);
 
     if(bp != 0) insert(ipre + (mpre - 1) - (mpre - 1) % buf);
     // reset buffer to old size
     buffer(1);
 
     // increase size of ancestors
     int p = ipar;
     while(p >= 0) {
       final int k = kind(p);
       size(p, k, size(p, k) + ms);
       p = parent(p, k);
     }
     updateDist(ipre + ms, ms);
 
     // delete old empty root node
     if(size(0, DOC) == 1) delete(0);
   }
 
   /**
    * This method updates the distance values of the specified pre value
    * and the following siblings.
    * @param pre root node
    * @param s size to be added/removed
    */
   private void updateDist(final int pre, final int s) {
     int p = pre;
     while(p < meta.size) {
       final int k = kind(p);
       dist(p, k, dist(p, k) + s);
       p += size(p, kind(p));
     }
   }
 
   /**
    * Sets the size value.
    * @param pre pre reference
    * @param k node kind
    * @param v value to be stored
    */
   public final void size(final int pre, final int k, final int v) {
     if(k == ELEM || k == DOC) table.write4(pre, 8, v);
   }
 
   /**
    * Sets the disk offset of a text/attribute value.
    * @param pre pre value
    * @param off offset
    */
   protected final void textOff(final int pre, final long off) {
     table.write5(pre, 3, off);
   }
 
   /**
    * Updates the specified text or attribute value.
    * @param pre pre value
    * @param val content
    * @param txt text (text, comment or pi) or attribute flag
    */
   protected abstract void text(final int pre, final byte[] val,
       final boolean txt);
 
   /**
    * Sets the distance.
    * @param pre pre value
    * @param k node kind
    * @param v value
    */
   private void dist(final int pre, final int k, final int v) {
     if(k == ATTR) table.write1(pre, 0, v << 3 | ATTR);
     else if(k != DOC) table.write4(pre, k == ELEM ? 4 : 8, v);
   }
 
   /**
    * Sets the attribute size.
    * @param pre pre value
    * @param k node kind
    * @param v value
    */
   private void attSize(final int pre, final int k, final int v) {
     if(k == ELEM) table.write1(pre, 0, v << 3 | ELEM);
   }
 
   // INSERTS WITHOUT TABLE UPDATES ============================================
 
   /** Buffer for caching new table entries. */
   private byte[] b = new byte[1 << IO.NODEPOWER];
   /** Buffer position. */
   private int bp;
 
   /**
    * Sets the update buffer to a new size.
    * @param s number of table entries
    */
   public final void buffer(final int s) {
     final int bs = s << IO.NODEPOWER;
     if(b.length != bs) b = new byte[bs];
   }
 
   /**
    * Inserts the internal buffer to the storage
    * without updating the table structure.
    * @param pre insert position
    */
   public final void insert(final int pre) {
     table.insert(pre, f());
   }
 
   /**
    * Adds a document entry to the internal update buffer.
    * @param pre pre value
    * @param s node size
    * @param vl document name
    */
   public void doc(final int pre, final int s, final byte[] vl) {
     final long i = ++meta.lastid;
     final long v = index(vl, pre, true);
     s(DOC); s(0); s(0); s(v >> 32);
     s(v >> 24); s(v >> 16); s(v >> 8); s(v);
     s(s >> 24); s(s >> 16); s(s >> 8); s(s);
     s(i >> 24); s(i >> 16); s(i >> 8); s(i);
   }
 
   /**
    * Adds an element entry to the internal update buffer.
    * @param d parent distance
    * @param tn tag name index
    * @param as number of attributes
    * @param s node size
    * @param u namespace uri reference
    * @param ne namespace flag
    */
   public final void elem(final int d, final int tn, final int as, final int s,
       final int u, final boolean ne) {
 
     // build and insert new entry
     final long i = ++meta.lastid;
     final int n = ne ? 1 << 7 : 0;
     s(as << 3 | ELEM); s(n | (byte) (tn >> 8)); s(tn); s(u);
     s(d >> 24); s(d >> 16); s(d >> 8); s(d);
     s(s >> 24); s(s >> 16); s(s >> 8); s(s);
     s(i >> 24); s(i >> 16); s(i >> 8); s(i);
   }
 
   /**
    * Adds a text entry to the internal update buffer.
    * @param pre insert position
    * @param d parent distance
    * @param vl tag name or text node
    * @param k node kind
    */
   public final void text(final int pre, final int d, final byte[] vl,
       final int k) {
 
     // build and insert new entry
     final long i = ++meta.lastid;
     final long v = index(vl, pre, true);
     s(k); s(0); s(0); s(v >> 32);
     s(v >> 24); s(v >> 16); s(v >> 8); s(v);
     s(d >> 24); s(d >> 16); s(d >> 8); s(d);
     s(i >> 24); s(i >> 16); s(i >> 8); s(i);
   }
 
   /**
    * Adds an attribute entry to the internal update buffer.
    * @param pre pre value
    * @param d parent distance
    * @param tn attribute name
    * @param vl attribute value
    * @param u namespace uri reference
    * @param ne namespace flag
    */
   public final void attr(final int pre, final int d, final int tn,
       final byte[] vl, final int u, final boolean ne) {
 
     // add attribute to text storage
     final long v = index(vl, pre, false);
     final long i = ++meta.lastid;
     final int n = ne ? 1 << 7 : 0;
     s(d << 3 | ATTR); s(n | (byte) (tn >> 8)); s(tn); s(v >> 32);
     s(v >> 24); s(v >> 16); s(v >> 8); s(v);
     s(0); s(0); s(0); s(u);
     s(i >> 24); s(i >> 16); s(i >> 8); s(i);
   }
 
   /**
    * Stores the specified value in the update buffer.
    * @param v value to be stored
    */
   private void s(final int v) {
     b[bp++] = (byte) v;
   }
 
   /**
    * Stores the specified value in the update buffer.
    * @param v value to be stored
    */
   private void s(final long v) {
     b[bp++] = (byte) v;
   }
 
   /**
    * Returns the byte buffer.
    * @return byte buffer
    */
   private byte[] f() {
     final byte[] bb = bp == b.length ? b : Arrays.copyOf(b, bp);
     bp = 0;
     return bb;
   }
 
   /**
    * Indexes a text and returns the reference.
    * @param txt text to be indexed
    * @param pre pre value
    * @param text text/attribute flag
    * @return reference
    */
   protected abstract long index(final byte[] txt, final int pre,
       final boolean text);
 
   /**
    * Returns a string representation of the specified table range.
    * @param s start pre value
    * @param e end pre value
    * @return table
    */
   public String toString(final int s, final int e) {
     return string(InfoTable.table(this, s, e));
   }
 
   @Override
   public String toString() {
     return toString(0, meta.size);
   }
 }
