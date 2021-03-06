 package hudson.model;
 
 import hudson.Util;
 import hudson.util.KeyedDataStorage;
 
 import java.io.File;
 import java.io.IOException;
 
 /**
  * Cache of {@link Fingerprint}s.
  *
  * <p>
  * This implementation makes sure that no two {@link Fingerprint} objects
  * lie around for the same hash code, and that unused {@link Fingerprint}
  * will be adequately GC-ed to prevent memory leak.
  *
  * @author Kohsuke Kawaguchi
  */
 public final class FingerprintMap extends KeyedDataStorage<Fingerprint,FingerprintParams> {
 
     /**
      * Returns true if there's some data in the fingerprint database.
      */
     public boolean isReady() {
         return new File( Hudson.getInstance().getRootDir(),"fingerprints").exists();
     }
 
     /**
      * @param build
      *      set to non-null if {@link Fingerprint} to be created (if so)
      *      will have this build as the owner. Otherwise null, to indicate
      *      an owner-less build.
      */
     public Fingerprint getOrCreate(AbstractBuild build, String fileName, byte[] md5sum) throws IOException {
         return getOrCreate(build,fileName, Util.toHexString(md5sum));
     }
 
     public Fingerprint getOrCreate(AbstractBuild build, String fileName, String md5sum) throws IOException {
         return super.getOrCreate(md5sum, new FingerprintParams(build,fileName));
     }
 
     protected Fingerprint get(String md5sum, boolean createIfNotExist, FingerprintParams createParams) throws IOException {
         // sanity check
         if(md5sum.length()!=32)
             return null;    // illegal input
         md5sum = md5sum.toLowerCase();
 
         return super.get(md5sum,createIfNotExist,createParams);
     }
 
     private byte[] toByteArray(String md5sum) {
         byte[] data = new byte[16];
         for( int i=0; i<md5sum.length(); i+=2 )
             data[i/2] = (byte)Integer.parseInt(md5sum.substring(i,i+2),16);
         return data;
     }
 
     protected Fingerprint create(String md5sum, FingerprintParams createParams) throws IOException {
         return new Fingerprint(createParams.build, createParams.fileName, toByteArray(md5sum));
     }
 
     protected Fingerprint load(String key) throws IOException {
         return Fingerprint.load(toByteArray(key));
     }
 }
 
 class FingerprintParams {
     final AbstractBuild build;
     final String fileName;
 
     public FingerprintParams(AbstractBuild build, String fileName) {
         this.build = build;
         this.fileName = fileName;
 
         assert build!=null;
         assert fileName!=null;
     }
 }
