 package org.micromanager.acquisition;
 
 import ij.process.LUT;
 import java.awt.event.AdjustmentEvent;
 import ij.CompositeImage;
 import ij.ImagePlus;
 import ij.gui.ImageWindow;
 import ij.gui.ScrollbarWithLabel;
 import ij.gui.StackWindow;
 import ij.plugin.Animator;
 import ij.process.ImageProcessor;
 import java.awt.Color;
 import java.awt.event.AdjustmentListener;
 import java.awt.event.WindowEvent;
 import java.io.File;
 import java.io.IOException;
 import java.util.ArrayList;
 import javax.swing.JFileChooser;
 import javax.swing.JOptionPane;
 import mmcorej.TaggedImage;
 import org.json.JSONArray;
 import org.json.JSONException;
 import org.json.JSONObject;
 import org.micromanager.api.AcquisitionEngine;
 import org.micromanager.api.MMWindow;
 import org.micromanager.utils.ImageUtils;
 import org.micromanager.utils.JavaUtils;
 import org.micromanager.utils.MDUtils;
 import org.micromanager.utils.MMScriptException;
 import org.micromanager.utils.ReportingUtils;
 
 /**
  *
  * @author arthur
  */
 public class MMVirtualAcquisitionDisplay{
 
    private String dir_;
    MMImageCache imageCache_;
    private int numChannels_;
    private int numFrames_;
    private int numSlices_;
    private int numPositions_ = 1;
    private int height_;
    private int width_;
    private int numComponents_ = 1;
    private ImagePlus hyperImage_;
    private ArrayList<AcquisitionVirtualStack> virtualStacks_;
    private JSONObject summaryMetadata_ = null;
    private boolean newData_;
    private boolean windowClosed_ = false;
    private int numGrayChannels_;
    private boolean diskCached_;
    private AcquisitionEngine eng_;
    private HyperstackControls hc_;
    private String status_ = "";
    private ScrollbarWithLabel pSelector_;
 
    private int curPosition_ = -1;
    private ChannelDisplaySettings[] channelSettings_;
 
    MMVirtualAcquisitionDisplay(String dir, boolean newData, boolean diskCached) {
       dir_ = dir;
       newData_ = newData;
       diskCached_ = diskCached;
       summaryMetadata_ = new JSONObject();
       try {
          summaryMetadata_.put("MetadataVersion", "10");
       } catch (Exception ex) {
          ReportingUtils.showError(ex);
       }
    }
 
    public void setCache(MMImageCache imageCache) {
       imageCache_ = imageCache;
       summaryMetadata_ = imageCache_.getSummaryMetadata();
    }
 
    public void initialize() throws MMScriptException {
       summaryMetadata_ = imageCache_.getSummaryMetadata();
       try {
          width_ = MDUtils.getWidth(summaryMetadata_);
          height_ = MDUtils.getHeight(summaryMetadata_);
          numSlices_ = Math.max(summaryMetadata_.getInt("Slices"), 1);
          numFrames_ = Math.max(summaryMetadata_.getInt("Frames"), 1);
          numChannels_ = Math.max(summaryMetadata_.getInt("Channels"), 1);
          numPositions_ = Math.max(summaryMetadata_.getInt("Positions"), 1);
          numComponents_ = MDUtils.getNumberOfComponents(summaryMetadata_);
       } catch (Exception e) {
          ReportingUtils.showError(e);
       }
 
       numGrayChannels_ = numComponents_ * numChannels_;
       AcquisitionVirtualStack virtualStack;
       virtualStacks_ = new ArrayList<AcquisitionVirtualStack>();
 
       for (int i = 0; i < numPositions_; ++i) {
          int pos = 0;
          if (numPositions_ == 1)
             try {
                pos = MDUtils.getPositionIndex(summaryMetadata_);
             } catch (Exception ex) { }
          else
             pos = i;
 
          virtualStack = new AcquisitionVirtualStack(width_, height_, null,
                  imageCache_, numGrayChannels_ * numSlices_ * numFrames_, pos, this);
          try {
             virtualStack.setType(MDUtils.getSingleChannelType(summaryMetadata_));
             virtualStacks_.add(virtualStack);
          } catch (Exception ex) {
             ReportingUtils.showError(ex);
          }
       }
 
       if (channelSettings_ == null) {
           channelSettings_ = new ChannelDisplaySettings[numChannels_];
           for (int i=0;i<numChannels_;++i) {
             channelSettings_[i] = new ChannelDisplaySettings();
           }
       }
       createImagePlus();
 
       readChannelSettingsFromCache(true);
    }
 
    private int getPositionIndex(TaggedImage taggedImg) throws Exception {
       int pos;
       pos = MDUtils.getPositionIndex(taggedImg.tags);
       return pos;
    }
 
    private void updateAndDraw() {
       if (numChannels_ > 1) {
             ((CompositeImage) hyperImage_).setChannelsUpdated();
       }
       hyperImage_.updateAndDraw();
    }
 
    public void updateWindow() {
       if (hc_ == null)
          return;
 
       if (newData_) {
          if (acquisitionIsRunning()) {
             if (!abortRequested()) {
                if (isPaused()) {
                   status_ = " (Paused)";
                } else {
                   status_ = " (Running)";
                }
             } else {
                status_ = " (Interrupted)";
                hc_.disableAcquisitionControls();
             }
          } else {
             if (!status_.contentEquals(" (Interrupted)")) {
                status_ = " (Finished)";
             }
             hc_.disableAcquisitionControls();
          }
       } else {
          if (diskCached_)
             status_ = " (On disk)";
          hc_.disableAcquisitionControls();
       }
       hc_.enableShowFolderButton(diskCached_);
       hyperImage_.getWindow().setTitle(new File(dir_).getName() +  status_);
    }
 
    public void insertImage(TaggedImage taggedImg) throws MMScriptException {
 
       try {
          int pos = getPositionIndex(taggedImg);
          virtualStacks_.get(pos).insertImage(taggedImg);
          if (hyperImage_ == null) {
             show(pos);
          }
         if (numPositions_ == 1 && numChannels_ == 1 && numSlices_ == 1 && numFrames_ == 1) {
            hyperImage_.setProcessor(virtualStacks_.get(0).getProcessor(1));
         }

          updateWindow();
          JSONObject md = taggedImg.tags;
 
          try {
             pSelector_.setValue(1 + pos);
             hyperImage_.setPosition(1 + MDUtils.getChannelIndex(md), 1 + MDUtils.getSliceIndex(md), 1 + MDUtils.getFrameIndex(md));
          } catch (Exception e) {
             ReportingUtils.logError(e);
          }
          if (hyperImage_.getFrame() == 1) {
             try {
                int pixelMin = ImageUtils.getMin(taggedImg.pix);
                int pixelMax = ImageUtils.getMax(taggedImg.pix);
                if (MDUtils.isRGB(taggedImg)) {
                   for (int i = 1; i <= numGrayChannels_; ++i) {
                      (hyperImage_).setPosition(i, MDUtils.getSliceIndex(taggedImg.tags), MDUtils.getFrameIndex(taggedImg.tags));
                      hyperImage_.setDisplayRange(pixelMin, pixelMax);
                      updateAndDraw();
                   }
                } else {
                   double min = hyperImage_.getDisplayRangeMin();
                   double max = hyperImage_.getDisplayRangeMax();
                   if (hyperImage_.getSlice() == 1) {
                      min = Double.MAX_VALUE;
                      max = Double.MIN_VALUE;
                   }
                   
                   int chan = MDUtils.getChannelIndex(md);
 
                   if (channelSettings_[chan] == null) {
                      channelSettings_[chan] = new ChannelDisplaySettings();
                   }
 
                   readChannelSettingsFromCache(false);
                   setChannelColor(chan, channelSettings_[chan].color.getRGB());
                   min = Math.min(min, pixelMin);
                   max = Math.max(max, pixelMax);
                   channelSettings_[chan].min = (int) min;
                   channelSettings_[chan].max = (int) max;
 
                   hyperImage_.setDisplayRange(min, max);
                   imageCache_
                     .getSummaryMetadata()
                     .getJSONArray("ChContrastMax")
                     .put(chan,max);
                   imageCache_
                     .getSummaryMetadata()
                     .getJSONArray("ChContrastMin")
                     .put(chan,min);
                   writeChannelSettingsToCache(chan);
                }
             } catch (Exception ex) {
                ReportingUtils.showError(ex);
             }
          }
          updateAndDraw();
       } catch (Exception ex) {
          ReportingUtils.logError(ex);
       }
    }
 
    private void updatePosition(int p) {
       if (curPosition_ != p) {
          double min = hyperImage_.getDisplayRangeMin();
          double max = hyperImage_.getDisplayRangeMax();
          // TODO: figure out why position is 1-based in the code
          // but 0-based for the end user????
          hyperImage_.setStack(virtualStacks_.get(p - 1));
          hyperImage_.setDisplayRange(min, max);
          virtualStacks_.get(p - 1).setImagePlus(hyperImage_);
          updateAndDraw();
          curPosition_ = p;
       }
    }
 
 
    public void setPosition(int p) {
       pSelector_.setValue(p);
    }
 
 
    boolean pause() {
       if (eng_ != null) {
          if (eng_.isPaused()) {
             eng_.setPause(false);
          } else {
             eng_.setPause(true);
          }
          updateWindow();
          return (eng_.isPaused());
       }
       return false;
    }
 
    boolean abort() {
       if (eng_ != null) {
          if (eng_.abortRequest()) {
             updateWindow();
             return true;
          }
       }
       return false;
    }
 
    public void setEngine(AcquisitionEngine eng) {
       eng_ = eng;
    }
 
    public boolean acquisitionIsRunning() {
       if (eng_ != null) {
          return eng_.isAcquisitionRunning();
       } else {
          return false;
       }
    }
 
    public boolean abortRequested() {
       if (eng_ != null) {
          return eng_.abortRequested();
       } else {
          return false;
       }
    }
 
    private boolean isPaused() {
       if (eng_ != null) {
          return eng_.isPaused();
       } else {
          return false;
       }
    }
 
    boolean saveAs() {
       String prefix;
       String root;
       for (;;) {
          final JFileChooser fc = new JFileChooser(new File(dir_).getParent());
          fc.setDialogTitle("Please choose a location for the data set.");
          fc.showSaveDialog(hyperImage_.getWindow());
          File f = fc.getSelectedFile();
          if (f == null) // Canceled.
          {
             return false;
          }
          prefix = f.getName();
          root = new File(f.getParent()).getAbsolutePath();
          if (f.exists()) {
             ReportingUtils.showMessage(prefix + " already exists! Please choose another name.");
          } else {
             break;
          }
       }
 
       TaggedImageStorageDiskDefault newFileManager = new TaggedImageStorageDiskDefault(root + "/" + prefix, true,
               summaryMetadata_);
       for (int i=0; i<numChannels_; i++) {
          writeChannelSettingsToCache(i);
       }
       imageCache_.saveAs(newFileManager);
       diskCached_ = true;
       dir_ = root + "/" + prefix;
       newData_ = false;
       updateWindow();
       return true;
    }
 
    public void createImagePlus() {
       ImagePlus imgp = new MMImagePlus(dir_, virtualStacks_.get(0));
       for (AcquisitionVirtualStack virtualStack : virtualStacks_) {
          virtualStack.setImagePlus(imgp);
       }
       imgp.setDimensions(numGrayChannels_, numSlices_, numFrames_);
       if (numGrayChannels_ > 1) {
          hyperImage_ = new CompositeImage(imgp, CompositeImage.COMPOSITE);
       } else {
          hyperImage_ = imgp;
          imgp.setOpenAsHyperStack(true);
       }
 
       final ImageWindow win = new StackWindow(hyperImage_) {
 
          @Override
          public void windowClosing(WindowEvent e) {
             if (eng_ != null && eng_.isAcquisitionRunning()) {
                if (!abort()) {
                   return;
                }
             }
 
             if (diskCached_ == false) {
                int result = JOptionPane.showConfirmDialog(this,
                        "This data set has not yet been saved.\n"
                        + "Do you want to save it?",
                        "Closing image...",
                        JOptionPane.YES_NO_CANCEL_OPTION);
                if (result != JOptionPane.NO_OPTION) {
                   return;
                } else if (result == JOptionPane.YES_OPTION) {
                   if (!saveAs()) {
                      return;
                   }
                }
             }
 
             // push current display settings to cache
             if (imageCache_ != null)
                imageCache_.close();
             setWindowClosed(true);
             imageCache_ = null;
             virtualStacks_ = null;
             close();
             hyperImage_ = null;
 
 
             super.windowClosing(e);
          }
 
          @Override
          public void windowActivated(WindowEvent e) {
             if (!isClosed()) {
                super.windowActivated(e);
             }
          }
       };
 
       ScrollbarWithLabel positionSelector = createPositionScrollbar(numPositions_);
       if (numPositions_ > 1) {
          win.add(positionSelector);
       }
 
       hc_ = new HyperstackControls(this, win);
       win.add(hc_);
       ImagePlus.addImageListener(hc_);
 
       win.pack();
 
       if (!newData_) {
          updateAndDraw();
       }
       updateWindow();
    }
 
    public void show(int position) {
       hyperImage_.show();
    }
 
    private ScrollbarWithLabel createPositionScrollbar(int nPositions) {
       pSelector_ = new ScrollbarWithLabel(null, 1, 1, 1, nPositions + 1, 'p') {
 
          @Override
          public void setValue(int v) {
             if (this.getValue() != v) {
                super.setValue(v);
                updatePosition(v);
             }
          }
       };
 
       pSelector_.setFocusable(false); // prevents scroll bar from blinking on Windows
       pSelector_.setUnitIncrement(1);
       pSelector_.setBlockIncrement(1);
       pSelector_.addAdjustmentListener(new AdjustmentListener() {
 
          public void adjustmentValueChanged(AdjustmentEvent e) {
             updatePosition(pSelector_.getValue());
             // ReportingUtils.logMessage("" + pSelector.getValue());
          }
       });
       return pSelector_;
    }
 
    public void setChannelColor(int channel, int rgb) throws MMScriptException {
       double gamma;
       if (channelSettings_ == null) {
          gamma = 1.0;
       } else {
          gamma = channelSettings_[channel].gamma;
       }
 
       setChannelLut(channel, new Color(rgb), gamma);
       writeChannelSettingsToCache(channel);
    }
 
    public void setChannelGamma(int channel, double gamma) {
       setChannelLut(channel, channelSettings_[channel].color, gamma);
       writeChannelSettingsToCache(channel);
    }
 
    public void setChannelDisplaySettings(int channel, ChannelDisplaySettings settings) {
       setChannelLut(channel, settings.color, settings.gamma);
       setChannelDisplayRange(channel, settings.min, settings.max);
       channelSettings_[channel] = settings;
    }
 
    public ChannelDisplaySettings getChannelDisplaySettings(int channel) {
       return channelSettings_[channel];
    }
 
    public void setChannelLut(int channel, Color color, double gamma) {
       // Note: both hyperImage_ and channelSettings_ can be null when this function is called
       // null pointer exception will ensue!
       if (hyperImage_ == null)
          return;
       LUT lut = ImageUtils.makeLUT(color, gamma, 8);
       if (hyperImage_.isComposite()) {
          CompositeImage ci = (CompositeImage) hyperImage_;
          setChannelWithoutUpdate(channel + 1);
          ci.setChannelColorModel(lut);
       } else {
          hyperImage_.getProcessor().setColorModel(lut);
       }
       updateAndDraw();
 
       channelSettings_[channel].color = color;
       channelSettings_[channel].gamma = gamma;
    }
 
    public void setChannelDisplayRange(int channel, int min, int max) {
       if (hyperImage_ == null)
          return;
       setChannelWithoutUpdate(channel + 1);
       hyperImage_.updateImage();
       hyperImage_.setDisplayRange(min, max);
       updateAndDraw();
       channelSettings_[channel].min = min;
       channelSettings_[channel].max = max;
 
       writeChannelSettingsToCache(channel);
    }
 
    public JSONObject getCurrentMetadata() {
       int index = getCurrentFlatIndex();
       int posIndex = pSelector_.getValue() - 1;
       try {
          TaggedImage image = virtualStacks_.get(posIndex).getTaggedImage(index);
          if (image != null) {
             return image.tags;
          } else {
             return null;
          }
       } catch (NullPointerException ex) {
          return null;
       }
    }
 
    private int getCurrentFlatIndex() {
       if (hyperImage_ != null)
          return hyperImage_.getCurrentSlice();
       return 0;
    }
 
    public int getNumChannels() {
       return numChannels_;
    }
 
    public int getNumPositions() {
       return numPositions_;
    }
 
    public ImagePlus getImagePlus() {
       return hyperImage_;
    }
 
    public ImagePlus getImagePlus(int position) {
       ImagePlus iP = new ImagePlus();
       iP.setStack(virtualStacks_.get(position - 1));
       iP.setDimensions(numChannels_, numSlices_, numFrames_);
       return iP;
    }
 
    public void setComment(String comment) throws MMScriptException {
       try {
          summaryMetadata_.put("Comment", comment);
       } catch (Exception ex) {
          ReportingUtils.logError(ex);
       }
    }
 
    public void close() {
       if (hyperImage_ != null)
          hyperImage_.close();
    }
 
    public synchronized void setWindowClosed(boolean state) {
       windowClosed_ = state;
    }
 
    public synchronized boolean windowClosed() {
       return windowClosed_;
    }
 
    void showFolder() {
       if (dir_.length() != 0) {
          try {
             if (JavaUtils.isWindows()) {
                Runtime.getRuntime().exec("Explorer /n,/select," + dir_);
             } else if (JavaUtils.isMac()) {
                Runtime.getRuntime().exec("open " + dir_);
             }
          } catch (IOException ex) {
             ReportingUtils.logError(ex);
          }
       }
    }
 
    private void setChannelWithoutUpdate(int channel) {
       if (hyperImage_ != null) {
          int z = hyperImage_.getSlice();
          int t = hyperImage_.getFrame();
 
          hyperImage_.setPositionWithoutUpdate(channel, z, t);
       }
 
    }
 
    public void setPlaybackFPS(double fps) {
       if (hyperImage_ != null) {
          try {
             JavaUtils.setRestrictedFieldValue(null, Animator.class, "animationRate", (double) fps);
          } catch (NoSuchFieldException ex) {
             ReportingUtils.showError(ex);
          }
       }
    }
 
    public double getPlaybackFPS() {
       return Animator.getFrameRate();
    }
 
    public int[] getCurrentSlices() {
       int frame = hyperImage_.getFrame();
       int slice = hyperImage_.getSlice();
       int nChannels = hyperImage_.getNChannels();
       int[] indices = new int[nChannels];
       for (int i = 0; i < nChannels; ++i) {
          indices[i] = hyperImage_.getStackIndex(i + 1, slice, frame);
       }
       return indices;
    }
 
    public String[] getChannelNames() {
       if (hyperImage_ instanceof CompositeImage) {
          AcquisitionVirtualStack stack = (AcquisitionVirtualStack) hyperImage_.getStack();
 
          int nChannels = hyperImage_.getNChannels();
          int[] indices = getCurrentSlices();
          String[] chanNames = new String[nChannels];
          for (int i = 0; i < nChannels; ++i) {
             try {
                chanNames[i] = imageCache_
                        .getDisplaySettings()
                        .getJSONArray("Channels")
                        .getJSONObject(i)
                        .getString("Name");
             } catch (Exception ex) {
                ReportingUtils.logError(ex);
             }
          }
          return chanNames;
       } else {
          return null;
       }
    }
 
    public void setChannelVisibility(int channelIndex, boolean visible) {
       if (!(hyperImage_ instanceof CompositeImage)) {
          return;
       }
       CompositeImage ci = (CompositeImage) hyperImage_;
       ci.getActiveChannels()[channelIndex] = visible;
       updateAndDraw();
    }
 
    public int[] getChannelHistogram(int channelIndex) {
       if (hyperImage_ == null) {
          return null;
       }
       if (hyperImage_.isComposite() && ((CompositeImage) hyperImage_).getMode() == CompositeImage.COMPOSITE) {
          ImageProcessor ip = ((CompositeImage) hyperImage_).getProcessor(channelIndex + 1);
          if (ip == null)
             return null;
          return ip.getHistogram();
       } else {
          if (hyperImage_.getChannel() == (channelIndex + 1))
             return hyperImage_.getProcessor().getHistogram();
          else
             return null;
       }
    }
 
    public int getChannelMax(int channelIndex) {
       return channelSettings_[channelIndex].max;
    }
 
    public int getChannelMin(int channelIndex) {
       return channelSettings_[channelIndex].min;
    }
 
    public double getChannelGamma(int channelIndex) {
       return channelSettings_[channelIndex].gamma;
    }
 
    public Color getChannelColor(int channelIndex) {
       return channelSettings_[channelIndex].color;
    }
 
    private void readChannelSettingsFromCache(boolean updateDisplay) {
       try {
          JSONArray channelsArray = imageCache_.getDisplaySettings().getJSONArray("Channels");
          for (int i = 0; i < channelSettings_.length; ++i) {
             try {
                JSONObject channel = channelsArray.getJSONObject(i);
                channelSettings_[i].color = new Color(channel.getInt("Color"));
                channelSettings_[i].min = channel.getInt("Min");
                channelSettings_[i].max = channel.getInt("Max");
                channelSettings_[i].gamma = channel.getDouble("Gamma");
                if (updateDisplay)
                   setChannelDisplaySettings(i, channelSettings_[i]);
             } catch (JSONException ex) {
                //ReportingUtils.logError(ex);
             }
          }
       } catch (JSONException ex) {
          ReportingUtils.logError(ex);
       }
    }
 
    private void writeChannelSettingsToCache(int channelIndex) {
       try {
          JSONObject jsonSetting = imageCache_.
                  getDisplaySettings().
                  getJSONArray("Channels").
                  getJSONObject(channelIndex);
          ChannelDisplaySettings setting = channelSettings_[channelIndex];
          jsonSetting.put("Color", setting.color.getRGB());
          jsonSetting.put("Gamma", setting.gamma);
          jsonSetting.put("Min", setting.min);
          jsonSetting.put("Max", setting.max);
       } catch (Exception ex) {
          ReportingUtils.logError(ex);
       }
 
    }
 
    public String getSummaryComment() {
       return imageCache_.getComment();
    }
 
    public void setSummaryComment(String comment) {
       imageCache_.setComment(comment);
    }
 
    void setImageComment(String comment) {
       imageCache_.setImageComment(comment, getCurrentMetadata());
    }
 
    String getImageComment() {
       try {
          return imageCache_.getImageComment(getCurrentMetadata());
       } catch (NullPointerException ex) {
          return "";
       }
    }
 
 }
