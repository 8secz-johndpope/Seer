/**
 * Copyright 1&1 Internet AG, https://github.com/1and1/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 package net.oneandone.sushi.fs.ssh;
 
 import com.jcraft.jsch.HostKey;
 import com.jcraft.jsch.HostKeyRepository;
 import com.jcraft.jsch.UserInfo;
 
 public class AcceptAllHostKeyRepository implements HostKeyRepository {
     @Override
     public int check(String host, byte[] key) {
         return HostKeyRepository.OK;
     }
 
     @Override
     public void add(HostKey hostkey, UserInfo ui) {
         throw new IllegalStateException();
     }
 
     @Override
     public void remove(String host, String type) {
         throw new IllegalStateException();
     }
 
     @Override
     public void remove(String host, String type, byte[] key) {
         throw new IllegalStateException();
     }
 
     @Override
     public String getKnownHostsRepositoryID() {
         throw new IllegalStateException();
     }
 
     @Override
     public HostKey[] getHostKey() {
         throw new IllegalStateException();
     }
 
     @Override
     public HostKey[] getHostKey(String host, String type) {
         throw new IllegalStateException();
     }
 }
