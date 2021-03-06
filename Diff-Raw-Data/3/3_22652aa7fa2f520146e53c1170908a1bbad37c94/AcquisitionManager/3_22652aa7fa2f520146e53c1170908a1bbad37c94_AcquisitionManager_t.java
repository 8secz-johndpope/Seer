 package org.micromanager.acquisition;
 
 import java.util.Enumeration;
 import java.util.Hashtable;
 import java.util.Set;
 
 import mmcorej.TaggedImage;
 import org.json.JSONException;
 import org.json.JSONObject;
 import org.micromanager.utils.MDUtils;
 
 import org.micromanager.utils.MMScriptException;
 import org.micromanager.utils.ReportingUtils;
 
 public class AcquisitionManager {
    Hashtable<String, MMAcquisition> acqs_;
    private String album_ = null;
    
    public AcquisitionManager() {
       acqs_ = new Hashtable<String, MMAcquisition>();
    }
    
    public void openAcquisition(String name, String rootDir) throws MMScriptException {
       if (acquisitionExists(name))
          throw new MMScriptException("The name is in use");
       else {
          MMAcquisition acq = new MMAcquisition(name, rootDir);
          acqs_.put(name, acq);
       }
    }
    
    public void openAcquisition(String name, String rootDir, boolean show) throws MMScriptException {
       this.openAcquisition(name, rootDir, show, false);
    }
 
    public void openAcquisition(String name, String rootDir, boolean show, boolean diskCached) throws MMScriptException {
       this.openAcquisition(name, rootDir, show, diskCached, false);
    }
 
    public void openAcquisition(String name, String rootDir, boolean show,
            boolean diskCached, boolean existing) throws MMScriptException {
       if (acquisitionExists(name)) {
          throw new MMScriptException("The name is in use");
       } else {
          acqs_.put(name, new MMAcquisition(name, rootDir, show, diskCached, existing));
       }
    }
    
   
    public void closeAcquisition(String name) throws MMScriptException {
       if (!acqs_.containsKey(name))
          throw new MMScriptException("The name does not exist");
       else {
          acqs_.get(name).close();
          acqs_.remove(name);
       }
    }
    
    public void closeImage5D(String name) throws MMScriptException {
       if (!acquisitionExists(name))
          throw new MMScriptException("The name does not exist");
       else
          acqs_.get(name).closeImage5D();
    }
    
    public Boolean acquisitionExists(String name) {
       if (acqs_.containsKey(name)) {
          if (acqs_.get(name).windowClosed()) {
             acqs_.get(name).close();
             acqs_.remove(name);
             return false;
          }
          return true;
       }
       return false;
    }
    
    public boolean hasActiveImage5D(String name) throws MMScriptException {
       if (acquisitionExists(name)) {
          return ! acqs_.get(name).windowClosed();
       }
       return false;
    }
       
    public MMAcquisition getAcquisition(String name) throws MMScriptException {
       if (acquisitionExists(name))
          return acqs_.get(name);
       else
          throw new MMScriptException("Undefined acquisition name: " + name);
    }
 
    public void closeAll() {
       for (Enumeration<MMAcquisition> e=acqs_.elements(); e.hasMoreElements(); )
          e.nextElement().close();
       
       acqs_.clear();
    }
 
    public String getUniqueAcquisitionName(String name) {
       char separator = '_';
       while (acquisitionExists(name)) {
          int lastSeparator = name.lastIndexOf(separator);
          if (lastSeparator == -1)
             name += separator + "1";
          else {
             Integer i = Integer.parseInt(name.substring(lastSeparator + 1));
             i++;
             name = name.substring(0, lastSeparator) + separator + i;
          }
       }
       return name;
    }
 
    public String getCurrentAlbum() {
       if (album_ == null) {
          return createNewAlbum();
       } else {
          return album_;
       }
    }
 
    public String createNewAlbum() {
       album_ = getUniqueAcquisitionName("Album");
       return album_;
    }
 
    public String addToAlbum(TaggedImage image) throws MMScriptException {
       boolean newNeeded = true;
       MMAcquisition acq = null;
       String album = getCurrentAlbum();
       JSONObject tags = image.tags;
       int imageWidth, imageHeight, imageDepth;
 
       try {
          imageWidth = MDUtils.getWidth(tags);
          imageHeight = MDUtils.getHeight(tags);
          imageDepth = MDUtils.getDepth(tags);
       } catch (Exception e) {
          throw new MMScriptException("Something wrong with image tags.");
       }
 
       if (acquisitionExists(album)) {
          acq = acqs_.get(album);
          try {
          if (acq.getWidth() == imageWidth &&
              acq.getHeight() == imageHeight &&
             acq.getDepth() == imageDepth  &&
                ! acq.getImageCache().isFinished() )
              newNeeded = false;
          } catch (Exception e) {
          }
       }
 
       if (newNeeded) {
          album = createNewAlbum();
          openAcquisition(album, "", true, false);
          acq = getAcquisition(album);
          acq.setDimensions(2, 1, 1, 1);
          acq.setImagePhysicalDimensions(imageWidth, imageHeight, imageDepth);
 
          try {
             JSONObject summary = new JSONObject();
             summary.put("PixelType", tags.get("PixelType"));
             acq.setSummaryProperties(summary);
          } catch (JSONException ex) {
             ex.printStackTrace();
          }
          
          acq.initialize();
       }
 
       int f = 1 + acq.getLastAcquiredFrame();
       try {
          MDUtils.setFrameIndex(image.tags, f);
       } catch (JSONException ex) {
          ReportingUtils.showError(ex);
       }
       acq.insertImage(image);
 
       return album;
    }
 
 
    public String[] getAcqusitionNames() {
       Set<String> keySet = acqs_.keySet();
       String keys[] = new String[keySet.size()];
       return keySet.toArray(keys);
    }
 }
