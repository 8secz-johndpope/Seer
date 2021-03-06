 package com.github.sarxos.webcam;
 
 import java.awt.Dimension;
 import java.awt.image.BufferedImage;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collections;
 import java.util.List;
 
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import com.github.sarxos.webcam.ds.buildin.WebcamDefaultDriver;
 
 
 /**
  * Webcam class.
  * 
  * @author Bartosz Firyn (bfiryn)
  */
 public class Webcam {
 
 	private static final Logger LOG = LoggerFactory.getLogger(Webcam.class);
 
 	//@formatter:off
 	private static final String[] DRIVERS_DEFAULT = new String[] {
 		"com.github.sarxos.webcam.ds.openimaj.OpenImajDriver",
 		"com.github.sarxos.webcam.ds.civil.LtiCivilDriver",
 		"com.github.sarxos.webcam.ds.jmf.JmfDriver",
 	};
 	//@formatter:on
 
 	private static final List<String> DRIVERS_LIST = new ArrayList<String>(Arrays.asList(DRIVERS_DEFAULT));
 	private static final List<Class<?>> DRIVERS_CLASS_LIST = new ArrayList<Class<?>>();
 
 	private static class ShutdownHook extends Thread {
 
 		private Webcam webcam = null;
 
 		public ShutdownHook(Webcam webcam) {
 			this.webcam = webcam;
 		}
 
 		@Override
 		public void run() {
 			LOG.info("Automatic resource deallocation");
 			super.run();
 			webcam.close0();
 		}
 	}
 
 	private static WebcamDriver driver = null;
 	private static List<Webcam> webcams = null;
 
 	/**
 	 * Webcam listeners.
 	 */
 	private List<WebcamListener> listeners = new ArrayList<WebcamListener>();
 
 	private ShutdownHook hook = null;
 	private WebcamDevice device = null;
 	private boolean open = false;
 
 	/**
 	 * Webcam class.
 	 * 
 	 * @param device - device to be used as webcam
 	 */
 	protected Webcam(WebcamDevice device) {
 		if (device == null) {
 			throw new IllegalArgumentException("Webcam device cannot be null");
 		}
 		this.device = device;
 	}
 
 	/**
 	 * Check if size is set up.
 	 */
 	private void ensureSize() {
 
 		Dimension size = device.getSize();
 		if (size == null) {
 
 			Dimension[] sizes = device.getSizes();
 
 			if (sizes == null) {
 				throw new WebcamException("Sizes array from driver cannot be null!");
 			}
 			if (sizes.length == 0) {
 				throw new WebcamException("Sizes array from driver is empty, cannot choose image size");
 			}
 
 			device.setSize(sizes[0]);
 		}
 	}
 
 	/**
 	 * Open webcam.
 	 */
 	public synchronized void open() {
 
 		if (open) {
 			return;
 		}
 
 		if (LOG.isInfoEnabled()) {
 			LOG.info("Opening webcam " + getName());
 		}
 
 		ensureSize();
 
 		device.open();
 		open = true;
 
 		hook = new ShutdownHook(this);
 
 		Runtime.getRuntime().addShutdownHook(hook);
 
 		WebcamEvent we = new WebcamEvent(this);
 		for (WebcamListener l : listeners) {
 			try {
 				l.webcamOpen(we);
 			} catch (Exception e) {
				LOG.error("Notify webcam open, exception when calling {}", WebcamListener.class.getSimpleName(), e);
 			}
 		}
 	}
 
 	/**
 	 * Close webcam.
 	 */
 	public synchronized void close() {
 
 		if (!open) {
 			return;
 		}
 
 		Runtime.getRuntime().removeShutdownHook(hook);
 		close0();
 	}
 
 	/**
 	 * Close webcam.
 	 */
 	private void close0() {
 
 		if (!open) {
 			return;
 		}
 
 		if (LOG.isInfoEnabled()) {
 			LOG.info("Closing {}", getName());
 		}
 
 		device.close();
 		open = false;
 
 		WebcamEvent we = new WebcamEvent(this);
 		for (WebcamListener l : listeners) {
 			try {
 				l.webcamClosed(we);
 			} catch (Exception e) {
				LOG.error("Notify webcam closed, exception when calling {}", WebcamListener.class.getSimpleName(), e);
 			}
 		}
 	}
 
 	/**
 	 * Is webcam open?
 	 * 
 	 * @return true if open, false otherwise
 	 */
 	public synchronized boolean isOpen() {
 		return open;
 	}
 
 	/**
 	 * @return Webcam view size (picture size) in pixels.
 	 */
 	public Dimension getViewSize() {
 		return device.getSize();
 	}
 
 	/**
 	 * Return list of supported view sizes. It can differ between vary webcam
 	 * data sources.
 	 * 
 	 * @return
 	 */
 	public Dimension[] getViewSizes() {
 		return device.getSizes();
 	}
 
 	public void setViewSize(Dimension size) {
 
 		if (size == null) {
 			throw new IllegalArgumentException("View size cannot be null!");
 		}
 
 		// check if dimension is valid
 
 		boolean ok = false;
 		Dimension[] sizes = getViewSizes();
 		for (Dimension d : sizes) {
 			if (d.width == size.width && d.height == size.height) {
 				ok = true;
 				break;
 			}
 		}
 
 		if (!ok) {
 			StringBuilder sb = new StringBuilder("Incorrect dimension [");
 			sb.append(size.width).append("x").append(size.height).append("] ");
 			sb.append("possible ones are ");
 			for (Dimension d : sizes) {
 				sb.append("[").append(d.width).append("x").append(d.height).append("] ");
 			}
 			throw new IllegalArgumentException(sb.toString());
 		}
 
 		if (LOG.isDebugEnabled()) {
 			LOG.debug("Setting new view size {} x {}", size.width, size.height);
 		}
 
 		device.setSize(size);
 	}
 
 	/**
 	 * Capture image from webcam.
 	 * 
 	 * @return Captured image
 	 */
 	public synchronized BufferedImage getImage() {
 		if (!open) {
 			LOG.debug("Try to get image on closed webcam, opening it automatically");
 			open();
 		}
 		return device.getImage();
 	}
 
 	/**
 	 * Get list of webcams to use.
 	 * 
 	 * @return List of webcams
 	 */
 	public static List<Webcam> getWebcams() {
 		if (webcams == null) {
 
 			webcams = new ArrayList<Webcam>();
 
 			if (driver == null) {
 				driver = WebcamDriverUtils.findDriver(DRIVERS_LIST, DRIVERS_CLASS_LIST);
 			}
 			if (driver == null) {
 				LOG.info("Webcam driver has not been found, default one will be used!");
 				driver = new WebcamDefaultDriver();
 			}
 
 			for (WebcamDevice device : driver.getDevices()) {
 				webcams.add(new Webcam(device));
 			}
 
 			if (LOG.isInfoEnabled()) {
 				for (Webcam webcam : webcams) {
 					LOG.info("Webcam found " + webcam.getName());
 				}
 			}
 		}
 
 		return Collections.unmodifiableList(webcams);
 	}
 
 	protected static void clearWebcams() {
 		webcams = null;
 	}
 
 	/**
 	 * @return Default webcam (first from the list)
 	 */
 	public static Webcam getDefault() {
 		List<Webcam> webcams = getWebcams();
 		if (webcams.isEmpty()) {
 			throw new WebcamException("No webcam available in the system");
 		}
 		return webcams.get(0);
 	}
 
 	/**
 	 * Get webcam name (device name). The name of device depends on the value
 	 * returned by the underlying data source, so in some cases it can be
 	 * human-readable value and sometimes it can be some strange number.
 	 * 
 	 * @return Name
 	 */
 	public String getName() {
 		return device.getName();
 	}
 
 	@Override
 	public String toString() {
 		return String.format("Webcam %s", getName());
 	}
 
 	/**
 	 * Add webcam listener.
 	 * 
 	 * @param l the listener to be added
 	 */
 	public boolean addWebcamListener(WebcamListener l) {
 		if (l == null) {
 			throw new IllegalArgumentException("Webcam listener cannot be null!");
 		}
 		synchronized (listeners) {
 			return listeners.add(l);
 		}
 	}
 
 	/**
 	 * @return All webcam listeners
 	 */
 	public WebcamListener[] getWebcamListeners() {
 		synchronized (listeners) {
 			return listeners.toArray(new WebcamListener[listeners.size()]);
 		}
 	}
 
 	public boolean removeWebcamListener(WebcamListener l) {
 		synchronized (listeners) {
 			return listeners.remove(l);
 		}
 	}
 
 	/**
 	 * @return Data source currently used by webcam
 	 */
 	public static WebcamDriver getDriver() {
 		return driver;
 	}
 
 	/**
 	 * Set new video driver to be used by webcam.
 	 * 
 	 * @param driver new video driver to use (e.g. Civil, JFM, FMJ, QTJ, etc)
 	 */
 	public static void setDriver(WebcamDriver driver) {
 		if (driver == null) {
 			throw new IllegalArgumentException("Webcam driver cannot be null!");
 		}
 		resetDriver();
 		Webcam.driver = driver;
 	}
 
 	/**
 	 * Set new video driver class to be used by webcam. Class given in the
 	 * argument shall extend {@link WebcamDriver} interface and should have
 	 * public default constructor, so instance can be created by reflection.
 	 * 
 	 * @param driver new video driver class to use
 	 */
 	public static void setDriver(Class<? extends WebcamDriver> driverClass) {
 
 		resetDriver();
 
 		if (driverClass == null) {
 			throw new IllegalArgumentException("Webcam driver class cannot be null!");
 		}
 
 		try {
 			driver = driverClass.newInstance();
 		} catch (InstantiationException e) {
 			throw new WebcamException(e);
 		} catch (IllegalAccessException e) {
 			throw new WebcamException(e);
 		}
 
 	}
 
 	public static void resetDriver() {
 
 		DRIVERS_LIST.clear();
 		DRIVERS_LIST.addAll(Arrays.asList(DRIVERS_DEFAULT));
 
 		driver = null;
 
 		if (webcams != null && !webcams.isEmpty()) {
 			webcams.clear();
 		}
 
 		webcams = null;
 	}
 
 	/**
 	 * Register new webcam video driver.
 	 * 
 	 * @param clazz webcam video driver class
 	 */
 	public static void registerDriver(Class<? extends WebcamDriver> clazz) {
 		if (clazz == null) {
 			throw new IllegalArgumentException("Webcam driver class to register cannot be null!");
 		}
 		DRIVERS_CLASS_LIST.add(clazz);
 		registerDriver(clazz.getCanonicalName());
 	}
 
 	/**
 	 * Register new webcam video driver.
 	 * 
 	 * @param clazzName webcam video driver class name
 	 */
 	public static void registerDriver(String clazzName) {
 		if (clazzName == null) {
 			throw new IllegalArgumentException("Webcam driver class name to register cannot be null!");
 		}
 		DRIVERS_LIST.add(clazzName);
 	}
 
 	protected WebcamDevice getDevice() {
 		return device;
 	}
 }
