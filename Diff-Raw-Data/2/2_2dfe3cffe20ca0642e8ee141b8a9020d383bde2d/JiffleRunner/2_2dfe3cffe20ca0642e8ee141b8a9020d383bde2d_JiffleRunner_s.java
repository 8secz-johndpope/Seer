 /*
  * To change this template, choose Tools | Templates
  * and open the template in the editor.
  */
 package jaitools.jiffle.interpreter;
 
 import jaitools.jiffle.util.CollectionFactory;
 import jaitools.jiffle.parser.ImageCalculator;
 import java.awt.Rectangle;
 import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
 import javax.media.jai.TiledImage;
 import javax.media.jai.iterator.RandomIter;
 import javax.media.jai.iterator.RandomIterFactory;
 import javax.media.jai.iterator.WritableRandomIter;
 import org.antlr.runtime.RecognitionException;
 
 /**
  *
  * @author Michael Bedward
  */
 public class JiffleRunner {
     
     private static class ImageFnProxy {
         enum Type {POS, INFO};
         
         String varName;
         Type type;
         
         ImageFnProxy(Type type, String varName) {
             this.type = type;  this.varName = varName;
         }
     }
     
     private static Map<String, ImageFnProxy> proxyTable;
     static {
         proxyTable = CollectionFactory.newMap();
         proxyTable.put("width", new ImageFnProxy(ImageFnProxy.Type.INFO, "_width"));
         proxyTable.put("height", new ImageFnProxy(ImageFnProxy.Type.INFO, "_height"));
         proxyTable.put("x", new ImageFnProxy(ImageFnProxy.Type.POS, "_x"));
         proxyTable.put("y", new ImageFnProxy(ImageFnProxy.Type.POS, "_y"));
         proxyTable.put("row", new ImageFnProxy(ImageFnProxy.Type.POS, "_row"));
         proxyTable.put("col", new ImageFnProxy(ImageFnProxy.Type.POS, "_col"));
     }
     
     public static final String FIXED_EXPR_PREFIX = "@";
 
     private Jiffle jiffle;
     private Metadata metadata;
     private VarTable vars;
     private FunctionTable funcs;
 
     private static class ImageHandler {
         int x;
         int y;
         int xmin;
         int xmax;
         int ymax;
         int band;
         boolean isOutput;
         RandomIter iter;
     }
 
     private Map<String, ImageHandler> handlerTable;
 
     private boolean finished;
     
     public static boolean isPositionalFunction(String name) {
         ImageFnProxy proxy = proxyTable.get(name);
         if (proxy != null  &&  proxy.type == ImageFnProxy.Type.POS) {
             return true;
         }
         
         return false;
     }
     
     public static boolean isInfoFunction(String name) {
         ImageFnProxy proxy = proxyTable.get(name);
         if (proxy != null  &&  proxy.type == ImageFnProxy.Type.INFO) {
             return true;
         }
         
         return false;
     }
     
     public static String getImageFunctionProxyVar(String funcName) {
         return proxyTable.get(funcName).varName;
     }
     
     /**
      * Constructor.
      * @param jiffle
      */
     public JiffleRunner(Jiffle jiffle) throws JiffleInterpreterException {
         if (!jiffle.isCompiled()) {
             throw new JiffleInterpreterException("The jiffle is not compiled");
         }
         
         this.jiffle = jiffle;
         this.metadata = jiffle.getMetadata();
         
         vars = new VarTable();
         funcs = new FunctionTable();
 
         setHandlers();
         setSpecialVars();
         finished = false;
     }
 
     /**
      * Get the value of an image at the current location
      * @param imgName the image variable name
      * @return value as a double
      * @throws RuntimeException if the image variable has not been registered
      */
     public double getImageValue(String imgName) {
         ImageHandler h = handlerTable.get(imgName);
         if (h == null) {
             throw new RuntimeException("unknown image var name: " + imgName);
         }
 
         return h.iter.getSampleDouble(h.x, h.y, h.band);
     }
 
     /**
      * Get the current value of a variable
      * @param varName the variable name
      * @return value as a double
      * @throws a RuntimeException if the variable has not been assigned
      */
     public double getVar(String varName) {
         return vars.get(varName);
     }
 
     /**
      * Invoke a general function
      * @param name the function name
      * @param args list of argument values (may be empty but not null)
      * @return the result of the function call as a double
      */
     public double invokeFunction(String name, List<Double> args) {
         return funcs.invoke(name, args);
     }
     
     /**
      * Check if a variable has been assigned
      * @param varName the variable name
      */
     public boolean isVarDefined(String varName) {
         return vars.contains(varName);
     }
 
     /**
      * Set the value of a variable
      * @param varName variable name
      * @param value the value to assign
      */
     public void setVar(String varName, double value) {
         vars.set(varName, value);
     }
     
     /**
      * Set the value of a variable
      * @param varName variable name
      * @param op assignment operator symbol (e.g. "=", "*=")
      * @param value the value to assign
      */
     public void setVar(String varName, String op, double value) {
         vars.assign(varName, op, value);
     }
 
     /**
      * Write a value to the current image pixel location
      * @param imgName image variable name
      * @param value value to write
      */
     public void writeToImage(String imgName, double value) {
         ImageHandler h = handlerTable.get(imgName);
         if (h == null) {
             throw new RuntimeException("unknown image var name: " + imgName);
         }
 
         ((WritableRandomIter) h.iter).setSample(h.x, h.y, h.band, value);
     }
     
     /**
      * Package private method called by {@link JiffleRunnable}. Causes
      * an {@link ImageCalculator} to be created to run the compiled
      * jiffle program.
      * 
      * @return success (true) or failure (false)
      */
     boolean run() throws JiffleInterpreterException {
         if (finished) {
             throw new JiffleInterpreterException("JiffleRunner.run() can only be called once");
         }
         
         ImageCalculator calc = new ImageCalculator(jiffle.getRuntimeAST());
         calc.setRunner(this);
         
         /* 
          * Evalute the AST at each pixel position
          */
         while (!finished) {
             try {
                 calc.start();
             } catch (RecognitionException rex) {
                 throw new JiffleInterpreterException("ImageCalculator failed: " + rex.getMessage());
             }
             calc.reset();
             nextPixel();
         }
         
         return true;
     }
 
     /**
      * Set handlers for each input and output image. A handler keeps track of
      * current image position and owns an iterator to read or write pixel
      * values
      */
     private void setHandlers() {
         handlerTable = CollectionFactory.newMap();
 
         for (Entry<String, TiledImage> e : metadata.getImageParams().entrySet()) {
             ImageHandler h = new ImageHandler();
             TiledImage img = e.getValue();
 
             h.x = h.xmin = img.getMinX();
             h.y = img.getMinY();
             h.xmax = img.getMaxX() - 1;
             h.ymax = img.getMaxY() - 1;
             h.band = 0;
             h.isOutput = metadata.getOutputImageVars().contains(e.getKey());
 
             if (h.isOutput) {
                 h.iter = RandomIterFactory.createWritable(img, img.getBounds());
             } else {
                 h.iter = RandomIterFactory.create(img, img.getBounds());
             }
 
             handlerTable.put(e.getKey(), h);
         }
     }
     
     /**
      * Set up the special variables that are proxies for jiffle image
      * info and positional functions
      */
     private void setSpecialVars() {
         // TODO: as a hack for development we use first output
         // image var as a reference - change this later when
         // allowing images with different bounds
         
         List<String> outVars = CollectionFactory.newList();
         outVars.addAll(metadata.getOutputImageVars());
         TiledImage refImg = metadata.getImageParams().get(outVars.get(0));
         
         Rectangle bounds = refImg.getBounds();
         
         vars.set(proxyTable.get("x").varName, bounds.x);
         vars.set(proxyTable.get("y").varName, bounds.y);
         vars.set(proxyTable.get("width").varName, bounds.width);
         vars.set(proxyTable.get("height").varName, bounds.height);
     }
 
     /**
      * Advance to the next pixel position. If all pixels have been
      * processed set the finished flag.
      */
     private void nextPixel() {
         if (!finished) {
             boolean firstImg = true;  // @todo remove this hack
             
             for (ImageHandler h : handlerTable.values()) {
                 h.x++;
                 if (h.x > h.xmax) {
                     h.x = h.xmin;
                     h.y++;
 
                     if (h.y > h.ymax) {
                         finished = true;
                     }
                 }
                 
                 // @todo remove this hack
                 if (firstImg) {
                     vars.set("_x", h.x);
                     vars.set("_y", h.y);
                     firstImg = false;
                 }
             }
         }
     }
 }
