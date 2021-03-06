 /*
  * Copyright (C) 2012 Daniel Thomas (drt24)
  * 
  * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
  * in compliance with the License. You may obtain a copy of the License at
  * 
  * http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software distributed under the License
  * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
  * or implied. See the License for the specific language governing permissions and limitations under
  * the License.
  */
 package com.google.nigori.client;
 
 import java.io.IOException;
 import java.security.MessageDigest;
 import java.security.NoSuchAlgorithmException;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.List;
 
 import com.google.nigori.client.DAG.Node;
 import com.google.nigori.common.Index;
 import com.google.nigori.common.RevValue;
 import com.google.nigori.common.Revision;
 
 /**
  * @author drt24
  * 
  */
 public class HashMigoriDatastore implements MigoriDatastore {
 
   private final NigoriDatastore store;
 
   public HashMigoriDatastore(NigoriDatastore store) {
     this.store = store;
   }
 
   @Override
   public boolean register() throws IOException, NigoriCryptographyException {
     return store.register();
   }
 
   @Override
   public boolean unregister() throws IOException, NigoriCryptographyException {
     return store.unregister();
   }
 
   @Override
   public boolean authenticate() throws IOException, NigoriCryptographyException {
     return store.authenticate();
   }
 
   @Override
   public List<Index> getIndices() throws NigoriCryptographyException, IOException {
     return store.getIndices();
   }
 
   @Override
   public RevValue deleteValue(Index index, RevValue... parents) {
     // TODO(drt24) implement
     throw new UnsupportedOperationException("Not yet implemented");
   }
 
   @Override
   public RevValue getHead(Index index, MigoriMerger merger) throws NigoriCryptographyException,
       IOException {
     List<RevValue> heads = get(index);
     if (heads.size() == 1) {
       for (RevValue head : heads) {
         return head;
       }
       throw new IllegalStateException("Can never happen as must be one head to return");
     } else {
       return put(index,merger.merge(index, heads),heads.toArray(new RevValue[0]));
     }
   }
 
   @Override
   public List<RevValue> get(Index index) throws NigoriCryptographyException, IOException {
     DAG<Revision> history = getHistory(index);
     Collection<Node<Revision>> heads = history.getHeads();
     List<RevValue> answer = new ArrayList<RevValue>();
     for (Node<Revision> rev : heads) {
       Revision revision = rev.getValue();
       byte[] value = store.getRevision(index, revision);
       // TODO(drt24) value might be null
       if (value != null) {
         answer.add(new RevValue(revision, value));
       }
     }
     return answer;
   }
 
   @Override
   public RevValue put(Index index, byte[] value, RevValue... parents) throws IOException,
       NigoriCryptographyException {
     byte[] toHash = new byte[value.length + parents.length * HashDAG.HASH_SIZE];
     Arrays.sort(parents);
     int insertPoint = 0;
     System.arraycopy(value, 0, toHash, insertPoint, value.length);
     insertPoint += value.length;
     for (RevValue rev : parents) {
       System.arraycopy(rev.getRevision().getBytes(), 0, toHash, insertPoint, HashDAG.HASH_SIZE);
       insertPoint += HashDAG.HASH_SIZE;
     }
     MessageDigest crypt;
     try {
       crypt = MessageDigest.getInstance("SHA-1");
 
       crypt.reset();
       crypt.update(toHash);
       Revision rev = new Revision(crypt.digest());
       RevValue rv = new RevValue(rev, value);
       boolean success = store.put(index, rev, value);
       if (!success) {
         throw new IOException("Could not put into the store");
       }
       return rv;
     } catch (NoSuchAlgorithmException e) {
       throw new NigoriCryptographyException(e);
     }
   }
 
   @Override
   public boolean deleteIndex(Index index, Revision position) throws NigoriCryptographyException,
       IOException {
     return store.delete(index, position.getBytes());
   }
 
   @Override
   public DAG<Revision> getHistory(Index index) throws NigoriCryptographyException, IOException {
     List<Revision> revisions = store.getRevisions(index);
     if (revisions == null) {
       return null;
     }
     return new HashDAG(revisions);
   }
 
 }
