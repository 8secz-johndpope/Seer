 /**
  * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
  * http://fusesource.com
  *
  * The software in this package is published under the terms of the
  * CDDL license a copy of which has been included with this distribution
  * in the license.txt file.
  */
 package org.fusesource.fabric.internal;
 
 import org.apache.zookeeper.CreateMode;
 import org.apache.zookeeper.KeeperException;
 import org.apache.zookeeper.ZooDefs;
 import org.fusesource.fabric.api.FabricException;
 import org.fusesource.fabric.api.Profile;
 import org.fusesource.fabric.service.FabricServiceImpl;
 import org.fusesource.fabric.zookeeper.ZkPath;
 import org.linkedin.zookeeper.client.IZKClient;
 
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

 public class ProfileImpl implements Profile {
 
     private final static String AGENT_PID = "org.fusesource.fabric.agent";
 
     private final String id;
     private final String version;
     private final FabricServiceImpl service;
 
     public ProfileImpl(String id, String version, FabricServiceImpl service) {
         this.id = id;
         this.version = version;
         this.service = service;
     }
 
     public String getId() {
         return id;
     }
 
     public String getVersion() {
         return version;
     }
 
     public FabricServiceImpl getService() {
         return service;
     }
 
     public Profile[] getParents() {
         try {
             String node = ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, id);
             String str = service.getZooKeeper().getStringData(node);
             if (str == null) {
                 return new Profile[0];
             }
             List<Profile> profiles = new ArrayList<Profile>();
             for (String p : str.split(" ")) {
                 profiles.add(new ProfileImpl(p, version, service));
             }
             return profiles.toArray(new Profile[profiles.size()]);
         } catch (Exception e) {
             throw new FabricException(e);
         }
     }
 
     public void setParents(Profile[] parents) {
         try {
             String str = "";
             for (Profile parent : parents) {
                 if (!version.equals(parent.getVersion())) {
                     throw new IllegalArgumentException("Bad profile: " + parent);
                 }
                 if (!str.isEmpty()) {
                     str += " ";
                 }
                 str += parent.getId();
             }
             service.getZooKeeper().setData( ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, id), str );
         } catch (Exception e) {
             throw new FabricException(e);
         }
     }
 
     public boolean isOverlay() {
         return false;
     }
 
     public Profile getOverlay() {
         return new ProfileOverlayImpl(this);
     }
 
     @Override
     public Map<String, byte[]> getFileConfigurations() {
         try {
             Map<String, byte[]> configurations = new HashMap<String, byte[]>();
             String path = ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, id);
             List<String> pids = service.getZooKeeper().getChildren(path);
             for (String pid : pids) {
                 configurations.put(pid, getFileConfiguration(pid));
             }
             return configurations;
         } catch (Exception e) {
             throw new FabricException(e);
         }
     }
 
     private byte[] getFileConfiguration(String pid) throws InterruptedException, KeeperException {
         IZKClient zooKeeper = service.getZooKeeper();
         String path = ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, id) + "/" + pid;
         if (zooKeeper.exists(path) == null) {
             return null;
         }
         return zooKeeper.getData(path);
     }
 
     @Override
     public void setFileConfigurations(Map<String, byte[]> configurations) {
         try {
             IZKClient zooKeeper = service.getZooKeeper();
             Map<String, byte[]> oldCfgs = getFileConfigurations();
             // Store new configs
             String path = ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, id);
             for (String pid : configurations.keySet()) {
                 oldCfgs.remove(pid);
                 byte[] newCfg = configurations.get(pid);
                String configPath =  path + "/" + pid;
                if (zooKeeper.exists(configPath) == null) {
                    zooKeeper.createBytesNode(configPath, newCfg, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                 } else {
                    zooKeeper.setByteData(configPath, newCfg);
                 }
             }
             for (String pid : oldCfgs.keySet()) {
                 zooKeeper.deleteWithChildren(path + "/" + pid);
             }
         } catch (Exception e) {
             throw new FabricException(e);
         }
     }
 
     public Map<String, Map<String, String>> getConfigurations() {
         try {
             Map<String, Map<String, String>> configurations = new HashMap<String, Map<String, String>>();
             Map<String, byte[]> configs = getFileConfigurations();
             for (Map.Entry<String, byte[]> entry: configs.entrySet()){
                 if(entry.getKey().endsWith(".properties")) {
                     String pid = stripSuffix(entry.getKey(), ".properties");
                     configurations.put(pid, getConfiguration(pid));
                 }
             }
             return configurations;
         } catch (Exception e) {
             throw new FabricException(e);
         }
     }
 
     private Map<String, String> getConfiguration(String pid) throws InterruptedException, KeeperException, IOException {
         IZKClient zooKeeper = service.getZooKeeper();
         String path = ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, id) + "/" + pid +".properties";
         if (zooKeeper.exists(path) == null) {
             return null;
         }
         byte[] data = zooKeeper.getData(path);
         return toMap(toProperties(data));
     }
 
     static public byte[] toBytes(Properties source) throws IOException {
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         source.store(baos, null);
         return baos.toByteArray();
     }
 
     static public Properties toProperties(byte[] source) throws IOException {
         Properties rc = new Properties();
         rc.load(new ByteArrayInputStream(source));
         return rc;
     }
 
     static public Map<String, String> toMap(Properties source) {
         Map<String, String> rc = new HashMap<String, String>();
         for (Map.Entry<Object, Object> entry: source.entrySet()){
             rc.put((String) entry.getKey(), (String) entry.getValue());
         }
         return rc;
     }
 
     static public Properties toProperties(Map<String, String> source) {
         Properties rc = new Properties();
         for (Map.Entry<String, String> entry: source.entrySet()){
             rc.put(entry.getKey(), entry.getValue());
         }
         return rc;
     }

     static public String stripSuffix(String value, String suffix) throws IOException {
         if(value.endsWith(suffix)) {
             return value.substring(0, value.length() -suffix.length());
         } else {
             return value;
         }
     }

 
     public void setConfigurations(Map<String, Map<String, String>> configurations) {
         try {
             IZKClient zooKeeper = service.getZooKeeper();
             Map<String, Map<String, String>> oldCfgs = getConfigurations();
             // Store new configs
             String path = ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, id);
             for (String pid : configurations.keySet()) {
                 oldCfgs.remove(pid);
                 byte[] data = toBytes(toProperties(configurations.get(pid)));
                 String p =  path + "/" + pid + ".properties";
                 if (zooKeeper.exists(p) == null) {
                     zooKeeper.createBytesNodeWithParents(p, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                 } else {
                     zooKeeper.setByteData(p, data);
                 }
             }
             for (String key : oldCfgs.keySet()) {
                 zooKeeper.deleteWithChildren(path + "/" + key +".properties");
             }
         } catch (Exception e) {
             throw new FabricException(e);
         }
     }
 
     public void delete() {
         service.deleteProfile(this);
     }
 
     @Override
     public String toString() {
         return "ProfileImpl[" +
                 "id='" + id + '\'' +
                 ", version='" + version + '\'' +
                 ']';
     }
 
     @Override
     public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;
         ProfileImpl profile = (ProfileImpl) o;
         if (!id.equals(profile.id)) return false;
         if (!version.equals(profile.version)) return false;
         return true;
     }
 
     @Override
     public int hashCode() {
         int result = id.hashCode();
         result = 31 * result + version.hashCode();
         return result;
     }
 }
