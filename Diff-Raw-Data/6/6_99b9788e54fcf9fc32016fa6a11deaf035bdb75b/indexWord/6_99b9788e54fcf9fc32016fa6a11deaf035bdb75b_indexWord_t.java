 // indexWord.java
 // (C) 2008 by Michael Peter Christen; mc@yacy.net, Frankfurt a. M., Germany
 // first published 26.03.2008 on http://yacy.net
 //
 // This is a part of YaCy, a peer-to-peer based web search engine
 //
 // $LastChangedDate: 2006-04-02 22:40:07 +0200 (So, 02 Apr 2006) $
 // $LastChangedRevision: 1986 $
 // $LastChangedBy: orbiter $
 //
 // LICENSE
 // 
 // This program is free software; you can redistribute it and/or modify
 // it under the terms of the GNU General Public License as published by
 // the Free Software Foundation; either version 2 of the License, or
 // (at your option) any later version.
 //
 // This program is distributed in the hope that it will be useful,
 // but WITHOUT ANY WARRANTY; without even the implied warranty of
 // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 // GNU General Public License for more details.
 //
 // You should have received a copy of the GNU General Public License
 // along with this program; if not, write to the Free Software
 // Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 
 package de.anomic.index;
 
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.Locale;
 import java.util.Set;
 import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
 
 import de.anomic.kelondro.order.Base64Order;
 import de.anomic.kelondro.order.Bitfield;
 import de.anomic.kelondro.order.Digest;
 import de.anomic.yacy.yacySeedDB;
 
 public class indexWord {
 
    private static final ConcurrentHashMap<String, String> hashCache = new ConcurrentHashMap<String, String>(1000);
     
     // object carries statistics for words and sentences
     public  int      count;       // number of occurrences
     public  int      posInText;   // unique handle, is initialized with word position (excluding double occurring words)
     public  int      posInPhrase; // position of word in phrase
     public  int      numOfPhrase; // number of phrase. 'normal' phrases begin with number 100
     HashSet<Integer> phrases;     // a set of handles to all phrases where this word appears
     public  Bitfield flags;       // the flag bits for each word
 
     public indexWord(final int handle, final int pip, final int nop) {
         this.count = 1;
         this.posInText = handle;
         this.posInPhrase = pip;
         this.numOfPhrase = nop;
         this.phrases = new HashSet<Integer>();
         this.flags = null;
     }
 
     public void inc() {
         count++;
     }
     
     public int occurrences() {
         return count;
     }
 
     public void check(final int i) {
         phrases.add(Integer.valueOf(i));
     }
 
     public Iterator<Integer> phrases() {
         // returns an iterator to handles of all phrases where the word appears
         return phrases.iterator();
     }
     
     // static methods
 
     // create a word hash
     public static final String word2hash(final String word) {
         String h = hashCache.get(word);
         if (h != null) return h;
         h = Base64Order.enhancedCoder.encode(Digest.encodeMD5Raw(word.toLowerCase(Locale.ENGLISH))).substring(0, yacySeedDB.commonHashLength);
         hashCache.put(word, h); // prevent expensive MD5 computation and encoding
         if (hashCache.size() > 20000) {
             // prevent memory leak
             hashCache.clear();
         }
         return h;
     }
     
     public static final Set<String> words2hashSet(final String[] words) {
         final TreeSet<String> hashes = new TreeSet<String>(Base64Order.enhancedComparator);
         for (int i = 0; i < words.length; i++) hashes.add(word2hash(words[i]));
         return hashes;
     }
 
     public static final String words2hashString(final String[] words) {
         final StringBuilder sb = new StringBuilder();
         for (int i = 0; i < words.length; i++) sb.append(word2hash(words[i]));
         return new String(sb);
     }
 
     public static final TreeSet<String> words2hashes(final Set<String> words) {
         final Iterator<String> i = words.iterator();
         final TreeSet<String> hashes = new TreeSet<String>(Base64Order.enhancedComparator);
         while (i.hasNext()) hashes.add(word2hash(i.next()));
         return hashes;
     }
 }
