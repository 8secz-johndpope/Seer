 package com.ardverk.dht.net;
 
 import java.net.InetAddress;
 import java.nio.ByteBuffer;
 
 import com.ardverk.collection.FixedSizeHashSet;
 import com.ardverk.net.NetworkMask;
 
 /**
  * 
  */
 public class AddressVerifier {
 
     private final NetworkMask mask;
     
     private final FixedSizeHashSet<ByteBuffer> history;
     
     private InetAddress current = null;
     
     private InetAddress temporary = null;
     
     public AddressVerifier(NetworkMask mask, int count) {        
         if (mask == null) {
             throw new NullPointerException("mask");
         }
         
         if (count < 0) {
            throw new IllegalArgumentException("maxSize=" + count);
         }
         
         this.mask = mask;
         this.history = new FixedSizeHashSet<ByteBuffer>(count);
     }
     
     /**
      * 
      */
     public synchronized boolean set(InetAddress src, InetAddress address) {
         if (src == null) {
             throw new NullPointerException("src");
         }
         
         if (address == null) {
             throw new NullPointerException("address");
         }
         
         // Do nothing if both addresses are equal
         if (current != null && current.equals(address)) {
             return true;
         }
         
         // Initialize the temporary address with the given value if 
         // it's null
         if (temporary == null) {
             temporary = address;
             
         // Reset the temporary address if it doesn't match with the 
         // given address. In other words, we continue working with 
         // the current address.
         } else if (!temporary.equals(address)) {
             temporary = null;
             history.clear();
             return false;
         }
         
         ByteBuffer network = ByteBuffer.wrap(mask.mask(src));
         
         // Make sure we're not accepting proposals more than once
         // from the same Network during the discovery process.
         if (!history.contains(network)) {
             history.add(network);
             if (history.isFull()) {
                 current = temporary;
                 temporary = null;
                 history.clear();
             }
             
             return true;
         } 
         
         return false;
     }
     
     /**
      * 
      */
     public synchronized InetAddress get() {
         return current;
     }
 }
