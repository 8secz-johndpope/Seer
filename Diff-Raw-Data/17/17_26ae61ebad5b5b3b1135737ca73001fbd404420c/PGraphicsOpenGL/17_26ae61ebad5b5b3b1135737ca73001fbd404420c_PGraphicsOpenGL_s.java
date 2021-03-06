 /* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */
 
 /*
   Part of the Processing project - http://processing.org
 
   Copyright (c) 2004-12 Ben Fry and Casey Reas
 
   This library is free software; you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public
   License version 2.1 as published by the Free Software Foundation.
 
   This library is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   Lesser General Public License for more details.
 
   You should have received a copy of the GNU Lesser General
   Public License along with this library; if not, write to the
   Free Software Foundation, Inc., 59 Temple Place, Suite 330,
   Boston, MA  02111-1307  USA
  */
 
 package processing.opengl;
 
 import processing.core.PApplet;
 import processing.core.PFont;
 import processing.core.PGraphics;
 import processing.core.PImage;
 import processing.core.PMatrix;
 import processing.core.PMatrix2D;
 import processing.core.PMatrix3D;
 import processing.core.PShape;
 import processing.core.PVector;
 
 import java.net.URL;
 import java.nio.*;
 import java.util.EmptyStackException;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Set;
 import java.util.Stack;
 
 // TODO: 
 // 2) move bezier/curve vertex generation to InGeometry.   
 
 /**
  * OpenGL renderer.
  *
  */
 public class PGraphicsOpenGL extends PGraphics {  
   /** Interface between Processing and OpenGL */
   public PGL pgl;
 
   /** The PApplet renderer. For the primary surface, pg == this. */
   protected PGraphicsOpenGL pg;
 
   // ........................................................
 
   // Basic rendering parameters:
 
   /** Flush modes: continuously (geometry is flushed after each call to
    * endShape) when-full (geometry is accumulated until a maximum size is
    * reached.  */
   static protected final int FLUSH_CONTINUOUSLY = 0;
   static protected final int FLUSH_WHEN_FULL    = 1;     
   
   /** Type of geometry: immediate is that generated with beginShape/vertex/
    * endShape, retained is the result of creating a PShape3D object with
    * createShape. */
   static protected final int IMMEDIATE = 0;
   static protected final int RETAINED  = 1;
 
   /** Current flush mode. */
   protected int flushMode = FLUSH_WHEN_FULL;
   
   // ........................................................
 
   // VBOs for immediate rendering:
 
   public int glFillVertexBufferID;
   public int glFillColorBufferID;
   public int glFillNormalBufferID;
   public int glFillTexCoordBufferID;
   public int glFillAmbientBufferID;
   public int glFillSpecularBufferID;
   public int glFillEmissiveBufferID;
   public int glFillShininessBufferID;
   public int glFillIndexBufferID;
   protected boolean fillVBOsCreated = false;
 
   public int glLineVertexBufferID;
   public int glLineColorBufferID;
   public int glLineDirWidthBufferID;
   public int glLineIndexBufferID;
   protected boolean lineVBOsCreated = false;
 
   public int glPointVertexBufferID;
   public int glPointColorBufferID;
   public int glPointSizeBufferID;
   public int glPointIndexBufferID;
   protected boolean pointVBOsCreated = false;
 
   // ........................................................
 
   // GL parameters
 
   static protected boolean glParamsRead = false;
 
   /** Extensions used by Processing */
   static public boolean npotTexSupported;
   static public boolean mipmapGeneration;
   static public boolean fboMultisampleSupported;
   static public boolean packedDepthStencilSupported;
   static public boolean blendEqSupported;
 
   /** Some hardware limits */
   static public int maxTextureSize;
   static public int maxSamples;
   static public float maxPointSize;
   static public float maxLineWidth;
   static public int depthBits;
   static public int stencilBits;
 
   /** OpenGL information strings */
   static public String OPENGL_VENDOR;
   static public String OPENGL_RENDERER;
   static public String OPENGL_VERSION;
   static public String OPENGL_EXTENSIONS;
 
   // ........................................................
 
   // GL objects:
 
   static protected HashMap<Integer, Boolean> glTextureObjects    = new HashMap<Integer, Boolean>();
   static protected HashMap<Integer, Boolean> glVertexBuffers     = new HashMap<Integer, Boolean>();
   static protected HashMap<Integer, Boolean> glFrameBuffers      = new HashMap<Integer, Boolean>();
   static protected HashMap<Integer, Boolean> glRenderBuffers     = new HashMap<Integer, Boolean>();
   static protected HashMap<Integer, Boolean> glslPrograms        = new HashMap<Integer, Boolean>();
   static protected HashMap<Integer, Boolean> glslVertexShaders   = new HashMap<Integer, Boolean>();
   static protected HashMap<Integer, Boolean> glslFragmentShaders = new HashMap<Integer, Boolean>();
 
   // ........................................................
 
   // Shaders
 
   static protected URL defFillShaderVertSimpleURL = PGraphicsOpenGL.class.getResource("FillShaderVertSimple.glsl");
   static protected URL defFillShaderVertTexURL    = PGraphicsOpenGL.class.getResource("FillShaderVertTex.glsl");
   static protected URL defFillShaderVertLitURL    = PGraphicsOpenGL.class.getResource("FillShaderVertLit.glsl");
   static protected URL defFillShaderVertFullURL   = PGraphicsOpenGL.class.getResource("FillShaderVertFull.glsl");
   static protected URL defFillShaderFragNoTexURL  = PGraphicsOpenGL.class.getResource("FillShaderFragNoTex.glsl");
   static protected URL defFillShaderFragTexURL    = PGraphicsOpenGL.class.getResource("FillShaderFragTex.glsl");
   static protected URL defLineShaderVertURL       = PGraphicsOpenGL.class.getResource("LineShaderVert.glsl");
   static protected URL defLineShaderFragURL       = PGraphicsOpenGL.class.getResource("LineShaderFrag.glsl");
   static protected URL defPointShaderVertURL      = PGraphicsOpenGL.class.getResource("PointShaderVert.glsl");
   static protected URL defPointShaderFragURL      = PGraphicsOpenGL.class.getResource("PointShaderFrag.glsl");
 
   static protected FillShaderSimple defFillShaderSimple;
   static protected FillShaderTex defFillShaderTex;
   static protected FillShaderLit defFillShaderLit;
   static protected FillShaderFull defFillShaderFull;
   static protected LineShader defLineShader;
   static protected PointShader defPointShader;
 
   protected FillShaderSimple fillShaderSimple;
   protected FillShaderTex fillShaderTex;
   protected FillShaderLit fillShaderLit;
   protected FillShaderFull fillShaderFull;
   protected LineShader lineShader;
   protected PointShader pointShader;
 
   // ........................................................
 
   // Tessellator, geometry
 
   protected InGeometry inGeo;
   protected TessGeometry tessGeo;
   protected int firstTexIndex;
   protected TexCache texCache;
   protected Tessellator tessellator;
 
   // ........................................................
 
   // Camera:
 
   /** Camera field of view. */
   public float cameraFOV;
 
   /** Default position of the camera. */
   public float cameraX, cameraY, cameraZ;
   /** Distance of the near and far planes. */
   public float cameraNear, cameraFar;
   /** Aspect ratio of camera's view. */
   public float cameraAspect;
 
   /** Distance between the camera eye and aim point. */
   protected float cameraDepth;
 
   /** Actual position of the camera. */
   protected float cameraEyeX, cameraEyeY, cameraEyeZ;
 
   /** Flag to indicate that we are inside beginCamera/endCamera block. */
   protected boolean manipulatingCamera;
 
   // ........................................................
 
   // All the matrices required for camera and geometry transformations.
   public PMatrix3D projection;
   public PMatrix3D camera;
   public PMatrix3D cameraInv;
   public PMatrix3D modelview;
   public PMatrix3D modelviewInv;
   public PMatrix3D projmodelview;
 
   // To pass to shaders
   protected float[] glProjection;
   protected float[] glModelview;
   protected float[] glProjmodelview;
   protected float[] glNormal;
 
   protected boolean matricesAllocated = false;
 
   /**
    * Marks when changes to the size have occurred, so that the camera
    * will be reset in beginDraw().
    */
   protected boolean sizeChanged;
 
   /** Modelview matrix stack **/
   protected Stack<PMatrix3D> modelviewStack;
 
   /** Inverse modelview matrix stack **/
   protected Stack<PMatrix3D> modelviewInvStack;
 
   /** Projection matrix stack **/
   protected Stack<PMatrix3D> projectionStack;
 
   // ........................................................
 
   // Lights:
 
   public boolean lights;
   public int lightCount = 0;
 
   /** Light types */
   public int[] lightType;
 
   /** Light positions */
   public float[] lightPosition;
 
   /** Light direction (normalized vector) */
   public float[] lightNormal;
 
   /**
    * Ambient colors for lights.
    */
   public float[] lightAmbient;
 
   /**
    * Diffuse colors for lights.
    */
   public float[] lightDiffuse;
 
   /**
    * Specular colors for lights. Internally these are stored as numbers between
    * 0 and 1.
    */
   public float[] lightSpecular;
 
   /** Light falloff */
   public float[] lightFalloffCoefficients;
 
   /** Light spot parameters: Cosine of light spot angle
    * and concentration */
   public float[] lightSpotParameters;
 
   /** Current specular color for lighting */
   public float[] currentLightSpecular;
 
   /** Current light falloff */
   public float currentLightFalloffConstant;
   public float currentLightFalloffLinear;
   public float currentLightFalloffQuadratic;
 
   protected boolean lightsAllocated = false;
 
   // ........................................................
 
   // Blending:
 
   protected int blendMode;
 
   // ........................................................
 
   // Clipping
 
   protected boolean clip = false;
 
   // ........................................................
 
   // Text:
 
   /** Font texture of currently selected font. */
   PFontTexture textTex;
 
   // .......................................................
 
   // Framebuffer stack:
 
   static protected Stack<PFramebuffer> fbStack;
   static protected PFramebuffer screenFramebuffer;
   static protected PFramebuffer currentFramebuffer;
 
   // .......................................................
 
   // Offscreen rendering:
 
   protected PFramebuffer offscreenFramebuffer;
   protected PFramebuffer offscreenFramebufferMultisample;
   protected boolean offscreenMultisample;
 
   protected boolean offscreenNotCurrent;
 
   // ........................................................
 
   // Screen surface:
 
   /** A handy reference to the PTexture bound to the drawing surface
    * (off or on-screen) */
   protected PTexture texture;
 
   /** IntBuffer wrapping the pixels array. */
   protected IntBuffer pixelBuffer;
 
   /** Array to store pixels in OpenGL format. */
   protected int[] rgbaPixels;
 
   /** Flag to indicate if the user is manipulating the
    * pixels array through the set()/get() methods */
   protected boolean setgetPixels;
 
   // ........................................................
 
   // Bezier and Catmull-Rom curves
   
   protected boolean bezierInited = false;
   public int bezierDetail = 20;
   protected PMatrix3D bezierDrawMatrix;
 
   protected boolean curveInited = false;
   protected int curveDetail = 20;
   public float curveTightness = 0;
 
   // catmull-rom basis matrix, perhaps with optional s parameter
   protected PMatrix3D curveBasisMatrix;
   protected PMatrix3D curveDrawMatrix;
 
   protected PMatrix3D bezierBasisInverse;
   protected PMatrix3D curveToBezierMatrix;
 
   protected float curveVertices[][];
   protected int curveVertexCount;
 
   // used by both curve and bezier, so just init here
   protected PMatrix3D bezierBasisMatrix =
     new PMatrix3D(-1,  3, -3,  1,
                    3, -6,  3,  0,
                   -3,  3,  0,  0,
                    1,  0,  0,  0);
 
   // ........................................................
 
   // Utility variables:
 
   /** True if we are inside a beginDraw()/endDraw() block. */
   protected boolean drawing = false;
 
   /** Type of pixels operation. */
   static protected final int OP_NONE = 0;
   static protected final int OP_READ = 1;
   static protected final int OP_WRITE = 2;
   protected int pixelsOp = OP_NONE;
 
   /** Used to detect the occurrence of a frame resize event. */
   protected boolean resized = false;
 
   /** Viewport dimensions. */
   protected int[] viewport = {0, 0, 0, 0};
 
   /** Used to register calls to glClear. */
   protected boolean clearColorBuffer;
   protected boolean clearColorBuffer0;
 
   protected boolean openContour = false;
   protected boolean breakShape = false;
   protected boolean defaultEdges = false;
   protected PImage textureImage0;
 
   protected boolean perspectiveCorrectedLines = false;
 
   /** Used in point tessellation. */
   final static protected int MIN_POINT_ACCURACY = 6;
   final protected float[][] QUAD_POINT_SIGNS = { {-1, +1}, {-1, -1}, {+1, -1}, {+1, +1} };
   
 
   //////////////////////////////////////////////////////////////
 
   // INIT/ALLOCATE/FINISH
 
 
   public PGraphicsOpenGL() {
     pgl = new PGL(this);
     pg = null;
 
     tessellator = new Tessellator();
 
     inGeo = newInGeometry(IMMEDIATE);
     tessGeo = newTessGeometry(IMMEDIATE);
     texCache = newTexCache();
 
     glFillVertexBufferID = 0;
     glFillColorBufferID = 0;
     glFillNormalBufferID = 0;
     glFillTexCoordBufferID = 0;
     glFillAmbientBufferID = 0;
     glFillSpecularBufferID = 0;
     glFillEmissiveBufferID = 0;
     glFillShininessBufferID = 0;
     glFillIndexBufferID = 0;
 
     glLineVertexBufferID = 0;
     glLineColorBufferID = 0;
     glLineDirWidthBufferID = 0;
     glLineIndexBufferID = 0;
 
     glPointVertexBufferID = 0;
     glPointColorBufferID = 0;
     glPointSizeBufferID = 0;
     glPointIndexBufferID = 0;
   }
 
 
   //public void setParent(PApplet parent)  // PGraphics
 
 
   public void setPrimary(boolean primary) {
     super.setPrimary(primary);
     format = ARGB;
   }
 
 
   //public void setPath(String path)  // PGraphics
 
 
   //public void setAntiAlias(int samples)  // PGraphics
 
 
   public void setFrameRate(float framerate) {
     pgl.setFramerate(framerate);
   }
 
 
   public void setSize(int iwidth, int iheight) {
     resized = (0 < width && width != iwidth) || (0 < height && height != iwidth);
 
     width = iwidth;
     height = iheight;
 //    width1 = width - 1;
 //    height1 = height - 1;
 
     allocate();
     reapplySettings();
 
     // init perspective projection based on new dimensions
     cameraFOV = 60 * DEG_TO_RAD; // at least for now
     cameraX = width / 2.0f;
     cameraY = height / 2.0f;
     cameraZ = cameraY / ((float) Math.tan(cameraFOV / 2.0f));
     cameraNear = cameraZ / 10.0f;
     cameraFar = cameraZ * 10.0f;
     cameraAspect = (float) width / (float) height;
     cameraDepth = cameraZ; // eye is at (cameraX, cameraY, cameraZ), aiming at (cameraX, cameraY, 0)
 
     // set this flag so that beginDraw() will do an update to the camera.
     sizeChanged = true;
 
     // Forces a restart of OpenGL so the canvas has the right size.
     pgl.initialized = false;
   }
 
 
   /**
    * Called by resize(), this handles creating the actual GLCanvas the
    * first time around, or simply resizing it on subsequent calls.
    * There is no pixel array to allocate for an OpenGL canvas
    * because OpenGL's pixel buffer is all handled internally.
    */
   protected void allocate() {
     super.allocate();
 
     if (!matricesAllocated) {
       projection = new PMatrix3D();
       camera = new PMatrix3D();
       cameraInv = new PMatrix3D();
       modelview = new PMatrix3D();
       modelviewInv = new PMatrix3D();
       projmodelview = new PMatrix3D();
       matricesAllocated = true;
     }
 
     if (!lightsAllocated) {
       lightType = new int[PGL.MAX_LIGHTS];
       lightPosition = new float[4 * PGL.MAX_LIGHTS];
       lightNormal = new float[3 * PGL.MAX_LIGHTS];
       lightAmbient = new float[3 * PGL.MAX_LIGHTS];
       lightDiffuse = new float[3 * PGL.MAX_LIGHTS];
       lightSpecular = new float[3 * PGL.MAX_LIGHTS];
       lightFalloffCoefficients = new float[3 * PGL.MAX_LIGHTS];
       lightSpotParameters = new float[2 * PGL.MAX_LIGHTS];
       currentLightSpecular = new float[3];
       lightsAllocated = true;
     }
   }
 
 
   public void dispose() { // PGraphics
     super.dispose();
     deleteFinalizedGLResources();
   }
 
   
   protected void setFlushMode(int mode) {
     flushMode = mode;    
   }
   
 
   //////////////////////////////////////////////////////////////
 
   // RESOURCE HANDLING
 
 
   // Texture Objects -------------------------------------------
 
   protected int createTextureObject() {
     deleteFinalizedTextureObjects();
 
     int[] temp = new int[1];
     pgl.glGenTextures(1, temp, 0);
     int id = temp[0];
 
     if (glTextureObjects.containsKey(id)) {
       showWarning("Adding same texture twice");
     } else {
       glTextureObjects.put(id, false);
     }
 
     return id;
   }
 
   protected void deleteTextureObject(int id) {
     if (glTextureObjects.containsKey(id)) {
       int[] temp = { id };
       pgl.glDeleteTextures(1, temp, 0);
       glTextureObjects.remove(id);
     }
   }
 
   protected void deleteAllTextureObjects() {
     for (Integer id : glTextureObjects.keySet()) {
       int[] temp = { id.intValue() };
       pgl.glDeleteTextures(1, temp, 0);
     }
     glTextureObjects.clear();
   }
 
   // This is synchronized because it is called from the GC thread.
   synchronized protected void finalizeTextureObject(int id) {
     if (glTextureObjects.containsKey(id)) {
       glTextureObjects.put(id, true);
     } else {
       showWarning("Trying to finalize non-existing texture");
     }
   }
 
   protected void deleteFinalizedTextureObjects() {
     Set<Integer> finalized = new HashSet<Integer>();
 
     for (Integer id : glTextureObjects.keySet()) {
       if (glTextureObjects.get(id)) {
         finalized.add(id);
         int[] temp = { id.intValue() };
         pgl.glDeleteTextures(1, temp, 0);
       }
     }
 
     for (Integer id : finalized) {
       glTextureObjects.remove(id);
     }
   }
 
 
   // Vertex Buffer Objects ----------------------------------------------
 
   protected int createVertexBufferObject() {
     deleteFinalizedVertexBufferObjects();
 
     int[] temp = new int[1];
     pgl.glGenBuffers(1, temp, 0);
     int id = temp[0];
 
     if (glVertexBuffers.containsKey(id)) {
       showWarning("Adding same VBO twice");
     } else {
       glVertexBuffers.put(id, false);
     }
 
     return id;
   }
 
   protected void deleteVertexBufferObject(int id) {
     if (glVertexBuffers.containsKey(id)) {
       int[] temp = { id };
       pgl.glDeleteBuffers(1, temp, 0);
       glVertexBuffers.remove(id);
     }
   }
 
   protected void deleteAllVertexBufferObjects() {
     for (Integer id : glVertexBuffers.keySet()) {
       int[] temp = { id.intValue() };
       pgl.glDeleteBuffers(1, temp, 0);
     }
     glVertexBuffers.clear();
   }
 
   // This is synchronized because it is called from the GC thread.
   synchronized protected void finalizeVertexBufferObject(int id) {
     if (glVertexBuffers.containsKey(id)) {
       glVertexBuffers.put(id, true);
     } else {
       showWarning("Trying to finalize non-existing VBO");
     }
   }
 
   protected void deleteFinalizedVertexBufferObjects() {
     Set<Integer> finalized = new HashSet<Integer>();
 
     for (Integer id : glVertexBuffers.keySet()) {
       if (glVertexBuffers.get(id)) {
         finalized.add(id);
         int[] temp = { id.intValue() };
         pgl.glDeleteBuffers(1, temp, 0);
       }
     }
 
     for (Integer id : finalized) {
       glVertexBuffers.remove(id);
     }
   }
 
 
   // FrameBuffer Objects -----------------------------------------
 
   protected int createFrameBufferObject() {
     deleteFinalizedFrameBufferObjects();
 
     int[] temp = new int[1];
     pgl.glGenFramebuffers(1, temp, 0);
     int id = temp[0];
 
     if (glFrameBuffers.containsKey(id)) {
       showWarning("Adding same FBO twice");
     } else {
       glFrameBuffers.put(id, false);
     }
 
     return id;
   }
 
   protected void deleteFrameBufferObject(int id) {
     if (glFrameBuffers.containsKey(id)) {
       int[] temp = { id };
       pgl.glDeleteFramebuffers(1, temp, 0);
       glFrameBuffers.remove(id);
     }
   }
 
   protected void deleteAllFrameBufferObjects() {
     for (Integer id : glFrameBuffers.keySet()) {
       int[] temp = { id.intValue() };
       pgl.glDeleteFramebuffers(1, temp, 0);
     }
     glFrameBuffers.clear();
   }
 
   // This is synchronized because it is called from the GC thread.
   synchronized protected void finalizeFrameBufferObject(int id) {
     if (glFrameBuffers.containsKey(id)) {
       glFrameBuffers.put(id, true);
     } else {
       showWarning("Trying to finalize non-existing FBO");
     }
   }
 
   protected void deleteFinalizedFrameBufferObjects() {
     Set<Integer> finalized = new HashSet<Integer>();
 
     for (Integer id : glFrameBuffers.keySet()) {
       if (glFrameBuffers.get(id)) {
         finalized.add(id);
         int[] temp = { id.intValue() };
         pgl.glDeleteFramebuffers(1, temp, 0);
       }
     }
 
     for (Integer id : finalized) {
       glFrameBuffers.remove(id);
     }
   }
 
 
   // RenderBuffer Objects -----------------------------------------------
 
   protected int createRenderBufferObject() {
     deleteFinalizedRenderBufferObjects();
 
     int[] temp = new int[1];
     pgl.glGenRenderbuffers(1, temp, 0);
     int id = temp[0];
 
     if (glRenderBuffers.containsKey(id)) {
       showWarning("Adding same renderbuffer twice");
     } else {
       glRenderBuffers.put(id, false);
     }
 
     return id;
   }
 
   protected void deleteRenderBufferObject(int id) {
     if (glRenderBuffers.containsKey(id)) {
       int[] temp = { id };
       pgl.glDeleteRenderbuffers(1, temp, 0);
       glRenderBuffers.remove(id);
     }
   }
 
   protected void deleteAllRenderBufferObjects() {
     for (Integer id : glRenderBuffers.keySet()) {
       int[] temp = { id.intValue() };
       pgl.glDeleteRenderbuffers(1, temp, 0);
     }
     glRenderBuffers.clear();
   }
 
   // This is synchronized because it is called from the GC thread.
   synchronized protected void finalizeRenderBufferObject(int id) {
     if (glRenderBuffers.containsKey(id)) {
       glRenderBuffers.put(id, true);
     } else {
       showWarning("Trying to finalize non-existing renderbuffer");
     }
   }
 
   protected void deleteFinalizedRenderBufferObjects() {
     Set<Integer> finalized = new HashSet<Integer>();
 
     for (Integer id : glRenderBuffers.keySet()) {
       if (glRenderBuffers.get(id)) {
         finalized.add(id);
         int[] temp = { id.intValue() };
         pgl.glDeleteRenderbuffers(1, temp, 0);
       }
     }
 
     for (Integer id : finalized) {
       glRenderBuffers.remove(id);
     }
   }
 
 
   // GLSL Program Objects -----------------------------------------------
 
   protected int createGLSLProgramObject() {
     deleteFinalizedGLSLProgramObjects();
 
     int id = pgl.glCreateProgram();
 
     if (glslPrograms.containsKey(id)) {
       showWarning("Adding same glsl program twice");
     } else {
       glslPrograms.put(id, false);
     }
 
     return id;
   }
 
   protected void deleteGLSLProgramObject(int id) {
     if (glslPrograms.containsKey(id)) {
       pgl.glDeleteProgram(id);
       glslPrograms.remove(id);
     }
   }
 
   protected void deleteAllGLSLProgramObjects() {
     for (Integer id : glslPrograms.keySet()) {
       pgl.glDeleteProgram(id);
     }
     glslPrograms.clear();
   }
 
   // This is synchronized because it is called from the GC thread.
   synchronized protected void finalizeGLSLProgramObject(int id) {
     if (glslPrograms.containsKey(id)) {
       glslPrograms.put(id, true);
     } else {
       showWarning("Trying to finalize non-existing glsl program");
     }
   }
 
   protected void deleteFinalizedGLSLProgramObjects() {
     Set<Integer> finalized = new HashSet<Integer>();
 
     for (Integer id : glslPrograms.keySet()) {
       if (glslPrograms.get(id)) {
         finalized.add(id);
         pgl.glDeleteProgram(id);
       }
     }
 
     for (Integer id : finalized) {
       glslPrograms.remove(id);
     }
   }
 
 
   // GLSL Vertex Shader Objects -----------------------------------------------
 
   protected int createGLSLVertShaderObject() {
     deleteFinalizedGLSLVertShaderObjects();
 
     int id = pgl.glCreateShader(PGL.GL_VERTEX_SHADER);
 
     if (glslVertexShaders.containsKey(id)) {
       showWarning("Adding same glsl vertex shader twice");
     } else {
       glslVertexShaders.put(id, false);
     }
 
     return id;
   }
 
   protected void deleteGLSLVertShaderObject(int id) {
     if (glslVertexShaders.containsKey(id)) {
       pgl.glDeleteShader(id);
       glslVertexShaders.remove(id);
     }
   }
 
   protected void deleteAllGLSLVertShaderObjects() {
     for (Integer id : glslVertexShaders.keySet()) {
       pgl.glDeleteShader(id);
     }
     glslVertexShaders.clear();
   }
 
   // This is synchronized because it is called from the GC thread.
   synchronized protected void finalizeGLSLVertShaderObject(int id) {
     if (glslVertexShaders.containsKey(id)) {
       glslVertexShaders.put(id, true);
     } else {
       showWarning("Trying to finalize non-existing glsl vertex shader");
     }
   }
 
   protected void deleteFinalizedGLSLVertShaderObjects() {
     Set<Integer> finalized = new HashSet<Integer>();
 
     for (Integer id : glslVertexShaders.keySet()) {
       if (glslVertexShaders.get(id)) {
         finalized.add(id);
         pgl.glDeleteShader(id);
       }
     }
 
     for (Integer id : finalized) {
       glslVertexShaders.remove(id);
     }
   }
 
 
   // GLSL Fragment Shader Objects -----------------------------------------------
 
   protected int createGLSLFragShaderObject() {
     deleteFinalizedGLSLFragShaderObjects();
 
     int id = pgl.glCreateShader(PGL.GL_FRAGMENT_SHADER);
 
     if (glslFragmentShaders.containsKey(id)) {
       showWarning("Adding same glsl fragment shader twice");
     } else {
       glslFragmentShaders.put(id, false);
     }
 
     return id;
   }
 
   protected void deleteGLSLFragShaderObject(int id) {
     if (glslFragmentShaders.containsKey(id)) {
       pgl.glDeleteShader(id);
       glslFragmentShaders.remove(id);
     }
   }
 
   protected void deleteAllGLSLFragShaderObjects() {
     for (Integer id : glslFragmentShaders.keySet()) {
       pgl.glDeleteShader(id);
     }
     glslFragmentShaders.clear();
   }
 
   // This is synchronized because it is called from the GC thread.
   synchronized protected void finalizeGLSLFragShaderObject(int id) {
     if (glslFragmentShaders.containsKey(id)) {
       glslFragmentShaders.put(id, true);
     } else {
       showWarning("Trying to finalize non-existing glsl fragment shader");
     }
   }
 
   protected void deleteFinalizedGLSLFragShaderObjects() {
     Set<Integer> finalized = new HashSet<Integer>();
 
     for (Integer id : glslFragmentShaders.keySet()) {
       if (glslFragmentShaders.get(id)) {
         finalized.add(id);
         pgl.glDeleteShader(id);
       }
     }
 
     for (Integer id : finalized) {
       glslFragmentShaders.remove(id);
     }
   }
 
 
   protected void deleteFinalizedGLResources() {
     deleteFinalizedTextureObjects();
     deleteFinalizedVertexBufferObjects();
     deleteFinalizedFrameBufferObjects();
     deleteFinalizedRenderBufferObjects();
     deleteFinalizedGLSLProgramObjects();
     deleteFinalizedGLSLVertShaderObjects();
     deleteFinalizedGLSLFragShaderObjects();
   }
 
 
   protected void deleteAllGLResources() {
     deleteAllTextureObjects();
     deleteAllVertexBufferObjects();
     deleteAllFrameBufferObjects();
     deleteAllRenderBufferObjects();
     deleteAllGLSLProgramObjects();
     deleteAllGLSLVertShaderObjects();
     deleteAllGLSLFragShaderObjects();
   }
 
 
   //////////////////////////////////////////////////////////////
 
   // FRAMEBUFFERS
 
 
   public void pushFramebuffer() {
     fbStack.push(currentFramebuffer);
   }
 
 
   public void setFramebuffer(PFramebuffer fbo) {
     currentFramebuffer = fbo;
     currentFramebuffer.bind();
   }
 
 
   public void popFramebuffer() {
     try {
       currentFramebuffer.finish();
       currentFramebuffer = fbStack.pop();
       currentFramebuffer.bind();
     } catch (EmptyStackException e) {
       PGraphics.showWarning("P3D: Empty framebuffer stack");
     }
   }
 
 
   //////////////////////////////////////////////////////////////
 
   // FRAME RENDERING
 
 
   protected void releaseResources() {
     // First, releasing the resources used by
     // the renderer itself.
     if (texture != null) {
       texture.release();
       texture = null;
     }
 
     if (defFillShaderSimple != null) {
       defFillShaderSimple.release();
       defFillShaderSimple = null;
     }
 
     if (defFillShaderLit != null) {
       defFillShaderLit.release();
       defFillShaderLit = null;
     }
 
     if (defFillShaderTex != null) {
       defFillShaderTex.release();
       defFillShaderTex = null;
     }
 
     if (defFillShaderFull != null) {
       defFillShaderFull.release();
       defFillShaderFull = null;
     }
 
     if (defLineShader != null) {
       defLineShader.release();
       defLineShader = null;
     }
 
     if (defPointShader != null) {
       defPointShader.release();
       defPointShader = null;
     }
 
     if (fillShaderSimple != null) {
       fillShaderSimple.release();
       fillShaderSimple = null;
     }
 
     if (fillShaderTex != null) {
       fillShaderTex.release();
       fillShaderTex = null;
     }
 
     if (fillShaderLit != null) {
       fillShaderLit.release();
       fillShaderLit = null;
     }
 
     if (fillShaderFull != null) {
       fillShaderFull.release();
       fillShaderFull = null;
     }
 
     if (lineShader != null) {
       lineShader.release();
       lineShader = null;
     }
 
     if (pointShader != null) {
       pointShader.release();
       pointShader = null;
     }
 
     if (fillVBOsCreated) {
       releaseFillBuffers();
       fillVBOsCreated = false;
     }
 
     if (lineVBOsCreated) {
       releaseLineBuffers();
       lineVBOsCreated = false;
     }
 
     if (pointVBOsCreated) {
       releasePointBuffers();
       pointVBOsCreated = false;
     }
 
     // Now, releasing the remaining resources
     // (from user's objects).
     deleteAllGLResources();
   }
 
 
   protected void createFillBuffers() {
     int sizef = PGL.MAX_TESS_VERTICES * PGL.SIZEOF_FLOAT;
     int sizei = PGL.MAX_TESS_VERTICES * PGL.SIZEOF_INT;
     int sizex = PGL.MAX_TESS_INDICES * PGL.SIZEOF_INDEX;
 
     glFillVertexBufferID = createVertexBufferObject();
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillVertexBufferID);
     pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 3 * sizef, null, PGL.GL_STATIC_DRAW);
 
     glFillColorBufferID = createVertexBufferObject();
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillColorBufferID);
     pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, null, PGL.GL_STATIC_DRAW);
 
     glFillNormalBufferID = createVertexBufferObject();
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillNormalBufferID);
     pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 3 * sizef, null, PGL.GL_STATIC_DRAW);
 
     glFillTexCoordBufferID = createVertexBufferObject();
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillTexCoordBufferID);
     pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 2 * sizef, null, PGL.GL_STATIC_DRAW);
 
     glFillAmbientBufferID = pg.createVertexBufferObject();
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillAmbientBufferID);
     pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, null, PGL.GL_STATIC_DRAW);
 
     glFillSpecularBufferID = pg.createVertexBufferObject();
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillSpecularBufferID);
     pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, null, PGL.GL_STATIC_DRAW);
 
     glFillEmissiveBufferID = pg.createVertexBufferObject();
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillEmissiveBufferID);
     pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, null, PGL.GL_STATIC_DRAW);
 
     glFillShininessBufferID = pg.createVertexBufferObject();
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillShininessBufferID);
     pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizef, null, PGL.GL_STATIC_DRAW);
 
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
 
     glFillIndexBufferID = createVertexBufferObject();
     pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glFillIndexBufferID);
     pgl.glBufferData(PGL.GL_ELEMENT_ARRAY_BUFFER, sizex, null, PGL.GL_STATIC_DRAW);
 
     pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);
   }
 
 
   protected void updateFillBuffers(boolean lit, boolean tex) {
     int size = tessGeo.fillVertexCount;
     int sizef = size * PGL.SIZEOF_FLOAT;
     int sizei = size * PGL.SIZEOF_INT;
 
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillVertexBufferID);
     pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 3 * sizef, FloatBuffer.wrap(tessGeo.fillVertices, 0, 3 * size), PGL.GL_STATIC_DRAW);
 
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillColorBufferID);
     pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, IntBuffer.wrap(tessGeo.fillColors, 0, size), PGL.GL_STATIC_DRAW);
 
     if (lit) {
       pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillNormalBufferID);
       pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 3 * sizef, FloatBuffer.wrap(tessGeo.fillNormals, 0, 3 * size), PGL.GL_STATIC_DRAW);
 
       pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillAmbientBufferID);
       pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, IntBuffer.wrap(tessGeo.fillAmbient, 0, size), PGL.GL_STATIC_DRAW);
 
       pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillSpecularBufferID);
       pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, IntBuffer.wrap(tessGeo.fillSpecular, 0, size), PGL.GL_STATIC_DRAW);
 
       pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillEmissiveBufferID);
       pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, IntBuffer.wrap(tessGeo.fillEmissive, 0, size), PGL.GL_STATIC_DRAW);
 
 
       pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillShininessBufferID);
       pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizef, FloatBuffer.wrap(tessGeo.fillShininess, 0, size), PGL.GL_STATIC_DRAW);
     }
 
     if (tex) {
       pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillTexCoordBufferID);
       pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 2 * sizef, FloatBuffer.wrap(tessGeo.fillTexcoords, 0, 2 * size), PGL.GL_STATIC_DRAW);
     }
 
     pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glFillIndexBufferID);
     pgl.glBufferData(PGL.GL_ELEMENT_ARRAY_BUFFER, tessGeo.fillIndexCount * PGL.SIZEOF_INDEX,
                      ShortBuffer.wrap(tessGeo.fillIndices, 0, tessGeo.fillIndexCount), PGL.GL_STATIC_DRAW);
   }
 
 
   protected void unbindFillBuffers() {
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
     pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);
   }
 
 
   protected void releaseFillBuffers() {
     deleteVertexBufferObject(glFillVertexBufferID);
     glFillVertexBufferID = 0;
 
     deleteVertexBufferObject(glFillColorBufferID);
     glFillColorBufferID = 0;
 
     deleteVertexBufferObject(glFillNormalBufferID);
     glFillNormalBufferID = 0;
 
     deleteVertexBufferObject(glFillTexCoordBufferID);
     glFillTexCoordBufferID = 0;
 
     deleteVertexBufferObject(glFillAmbientBufferID);
     glFillAmbientBufferID = 0;
 
     deleteVertexBufferObject(glFillSpecularBufferID);
     glFillSpecularBufferID = 0;
 
     deleteVertexBufferObject(glFillEmissiveBufferID);
     glFillEmissiveBufferID = 0;
 
     deleteVertexBufferObject(glFillShininessBufferID);
     glFillShininessBufferID = 0;
 
     deleteVertexBufferObject(glFillIndexBufferID);
     glFillIndexBufferID = 0;
   }
 
 
   protected void createLineBuffers() {
     int sizef = PGL.MAX_TESS_VERTICES * PGL.SIZEOF_FLOAT;
     int sizex = PGL.MAX_TESS_INDICES * PGL.SIZEOF_INDEX;
     int sizei = PGL.MAX_TESS_INDICES * PGL.SIZEOF_INT;
 
     glLineVertexBufferID = createVertexBufferObject();
 
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineVertexBufferID);
     pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 3 * sizef, null, PGL.GL_STATIC_DRAW);
 
     glLineColorBufferID = createVertexBufferObject();
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineColorBufferID);
     pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, null, PGL.GL_STATIC_DRAW);
 
     glLineDirWidthBufferID = createVertexBufferObject();
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineDirWidthBufferID);
     pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 4 * sizef, null, PGL.GL_STATIC_DRAW);
 
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
 
     glLineIndexBufferID = createVertexBufferObject();
     pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glLineIndexBufferID);
     pgl.glBufferData(PGL.GL_ELEMENT_ARRAY_BUFFER, sizex, null, PGL.GL_STATIC_DRAW);
 
     pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);
   }
 
 
   protected void updateLineBuffers() {
     int size = tessGeo.lineVertexCount;
     int sizef = size * PGL.SIZEOF_FLOAT;
     int sizei = size * PGL.SIZEOF_INT;
 
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineVertexBufferID);
     pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 3 * sizef, FloatBuffer.wrap(tessGeo.lineVertices, 0, 3 * size), PGL.GL_STATIC_DRAW);
 
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineColorBufferID);
     pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, IntBuffer.wrap(tessGeo.lineColors, 0, size), PGL.GL_STATIC_DRAW);
 
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineDirWidthBufferID);
     pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 4 * sizef, FloatBuffer.wrap(tessGeo.lineDirWidths, 0, 4 * size), PGL.GL_STATIC_DRAW);
 
     pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glLineIndexBufferID);
     pgl.glBufferData(PGL.GL_ELEMENT_ARRAY_BUFFER, tessGeo.lineIndexCount * PGL.SIZEOF_INDEX,
                      ShortBuffer.wrap(tessGeo.lineIndices, 0, tessGeo.lineIndexCount), PGL.GL_STATIC_DRAW);
   }
 
 
   protected void unbindLineBuffers() {
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
     pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);
   }
 
 
   protected void releaseLineBuffers() {
     deleteVertexBufferObject(glLineVertexBufferID);
     glLineVertexBufferID = 0;
 
     deleteVertexBufferObject(glLineColorBufferID);
     glLineColorBufferID = 0;
 
     deleteVertexBufferObject(glLineDirWidthBufferID);
     glLineDirWidthBufferID = 0;
 
     deleteVertexBufferObject(glLineIndexBufferID);
     glLineIndexBufferID = 0;
   }
 
 
   protected void createPointBuffers() {
     int sizef = PGL.MAX_TESS_VERTICES * PGL.SIZEOF_FLOAT;
     int sizex = PGL.MAX_TESS_INDICES * PGL.SIZEOF_INDEX;
     int sizei = PGL.MAX_TESS_INDICES * PGL.SIZEOF_INT;
 
     glPointVertexBufferID = createVertexBufferObject();
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointVertexBufferID);
     pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 3 * sizef, null, PGL.GL_STATIC_DRAW);
 
     glPointColorBufferID = createVertexBufferObject();
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointColorBufferID);
     pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, null, PGL.GL_STATIC_DRAW);
 
     glPointSizeBufferID = createVertexBufferObject();
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointSizeBufferID);
     pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 2 * sizef, null, PGL.GL_STATIC_DRAW);
 
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
 
     glPointIndexBufferID = createVertexBufferObject();
     pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glPointIndexBufferID);
     pgl.glBufferData(PGL.GL_ELEMENT_ARRAY_BUFFER, sizex, null, PGL.GL_STATIC_DRAW);
 
     pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);
   }
 
 
   protected void updatePointBuffers() {
     int size = tessGeo.pointVertexCount;
     int sizef = size * PGL.SIZEOF_FLOAT;
     int sizei = size * PGL.SIZEOF_INT;
 
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointVertexBufferID);
     pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 3 * sizef, FloatBuffer.wrap(tessGeo.pointVertices, 0, 3 * size), PGL.GL_STATIC_DRAW);
 
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointColorBufferID);
     pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, IntBuffer.wrap(tessGeo.pointColors, 0, size), PGL.GL_STATIC_DRAW);
 
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointSizeBufferID);
     pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 2 * sizef, FloatBuffer.wrap(tessGeo.pointSizes, 0, 2 * size), PGL.GL_STATIC_DRAW);
 
     pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glPointIndexBufferID);
     pgl.glBufferData(PGL.GL_ELEMENT_ARRAY_BUFFER, tessGeo.pointIndexCount * PGL.SIZEOF_INDEX,
                      ShortBuffer.wrap(tessGeo.pointIndices, 0, tessGeo.pointIndexCount), PGL.GL_STATIC_DRAW);
   }
 
 
   protected void unbindPointBuffers() {
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
     pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);
   }
 
 
   protected void releasePointBuffers() {
     deleteVertexBufferObject(glPointVertexBufferID);
     glPointVertexBufferID = 0;
 
     deleteVertexBufferObject(glPointColorBufferID);
     glPointColorBufferID = 0;
 
     deleteVertexBufferObject(glPointSizeBufferID);
     glPointSizeBufferID = 0;
 
     deleteVertexBufferObject(glPointIndexBufferID);
     glPointIndexBufferID = 0;
   }
 
 
   /**
    * OpenGL cannot draw until a proper native peer is available, so this
    * returns the value of PApplet.isDisplayable() (inherited from Component).
    */
   public boolean canDraw() {
     return pgl.canDraw();
   }
 
 
   public void requestDraw() {
     if (primarySurface) {
       if (pgl.initialized) {
         pgl.requestDraw();
       } else {
         initPrimary();
       }
     }
   }
 
 
   public void beginDraw() {
     if (drawing) {
       showWarning("P3D: Already called beginDraw().");
       return;
     }
 
     if (!glParamsRead) {
       getGLParameters();
     }
 
     if (!settingsInited) {
       defaultSettings();
     }
 
     if (primarySurface) {
       pgl.updatePrimary();
     } else {
       if (!pgl.initialized) {
         initOffscreen();
       } else {
         boolean outdated = offscreenFramebuffer != null && offscreenFramebuffer.contextIsOutdated();
         boolean outdatedMulti = offscreenFramebufferMultisample != null && offscreenFramebufferMultisample.contextIsOutdated();
         if (outdated || outdatedMulti) {
           pgl.initialized = false;
           initOffscreen();
         }
       }
 
       pushFramebuffer();
       if (offscreenMultisample) {
         setFramebuffer(offscreenFramebufferMultisample);
       } else {
         setFramebuffer(offscreenFramebuffer);
       }
       pgl.glDrawBuffer(PGL.GL_COLOR_ATTACHMENT0);
       pgl.updateOffscreen(pg.pgl);
     }
 
     // We are ready to go!
 
     report("top beginDraw()");
 
     inGeo.clear();
     tessGeo.clear();
     texCache.clear();
 
     // Each frame starts with textures disabled.
     super.noTexture();
 
     // Screen blend is needed for alpha (i.e. fonts) to work.
     // Using setDefaultBlend() instead of blendMode() because
     // the latter will set the blend mode only if it is different
     // from current.
     setDefaultBlend();
 
     // this is necessary for 3D drawing
     if (hints[DISABLE_DEPTH_TEST]) {
       pgl.glDisable(PGL.GL_DEPTH_TEST);
     } else {
       pgl.glEnable(PGL.GL_DEPTH_TEST);
     }
     // use <= since that's what processing.core does
     pgl.glDepthFunc(PGL.GL_LEQUAL);
 
     if (hints[ENABLE_ACCURATE_2D]) {
       flushMode = FLUSH_CONTINUOUSLY;
     } else {
       flushMode = FLUSH_WHEN_FULL;
     }
 
     if (primarySurface) {
       int[] temp = { 0 };
       pgl.glGetIntegerv(PGL.GL_SAMPLES, temp, 0);
       if (antialias != temp[0] && 1 < temp[0] && 1 < antialias) {
         antialias = temp[0];
       }
     }
     if (antialias < 2) {
       pgl.glDisable(PGL.GL_MULTISAMPLE);
       pgl.glEnable(PGL.GL_POINT_SMOOTH);
       pgl.glEnable(PGL.GL_LINE_SMOOTH);
       pgl.glEnable(PGL.GL_POLYGON_SMOOTH);
     } else {
       pgl.glEnable(PGL.GL_MULTISAMPLE);
       pgl.glDisable(PGL.GL_POINT_SMOOTH);
       pgl.glDisable(PGL.GL_LINE_SMOOTH);
       pgl.glDisable(PGL.GL_POLYGON_SMOOTH);
     }
 
     // setup opengl viewport.
     viewport[0] = 0; viewport[1] = 0; viewport[2] = width; viewport[3] = height;
     pgl.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
     if (resized) {
       // To avoid having garbage in the screen after a resize,
       // in the case background is not called in draw().
       background(0);
       if (texture != null) {
         // The screen texture should be deleted because it
         // corresponds to the old window size.
         this.removeCache(pg);
         this.removeParams(pg);
         texture = null;
         loadTexture();
       }
       resized = false;
     }
 
     if (sizeChanged) {
       // defaults to perspective, if the user has setup up their
       // own projection, they'll need to fix it after resize anyway.
       // this helps the people who haven't set up their own projection.
       perspective();
 
       // set up the default camera and initializes modelview matrix.
       camera();
 
       // clear the flag
       sizeChanged = false;
     } else {
       // The camera and projection matrices, saved when calling camera() and frustrum()
       // are set as the current modelview and projection matrices. This is done to
       // remove any additional modelview transformation (and less likely, projection
       // transformations) applied by the user after setting the camera and/or projection
       modelview.set(camera);
       modelviewInv.set(cameraInv);
       calcProjmodelview();
     }
 
     noLights();
     lightFalloff(1, 0, 0);
     lightSpecular(0, 0, 0);
 
     // Because y is flipped, the vertices that should be specified by 
     // the user in CCW order to define a front-facing facet, end up being
     // CW.
     pgl.glFrontFace(PGL.GL_CW);
     pgl.glDisable(PGL.GL_CULL_FACE);
 
     // Processing uses only one texture unit.
     pgl.glActiveTexture(PGL.GL_TEXTURE0);
 
     // The current normal vector is set to be parallel to the Z axis.
     normalX = normalY = normalZ = 0;
 
     perspectiveCorrectedLines = hints[ENABLE_PERSPECTIVE_CORRECTED_LINES];
 
     // Clear depth and stencil buffers.
     pgl.glDepthMask(true);
     pgl.glClearColor(0, 0, 0, 0);
     pgl.glClear(PGL.GL_DEPTH_BUFFER_BIT | PGL.GL_STENCIL_BUFFER_BIT);
 
     if (primarySurface) {
       pgl.beginOnscreenDraw(clearColorBuffer);
     } else {
       pgl.beginOffscreenDraw(pg.clearColorBuffer);
 
       // Just in case the texture was recreated (in a resize event for example)
       offscreenFramebuffer.setColorBuffer(texture);
     }
 
     if (hints[DISABLE_DEPTH_MASK]) {
       pgl.glDepthMask(false);
     } else {
       pgl.glDepthMask(true);
     }
 
     drawing = true;
     pixelsOp = OP_NONE;
 
     modified = false;
     setgetPixels = false;
 
     clearColorBuffer0 = clearColorBuffer;
     clearColorBuffer = false;
 
     report("bot beginDraw()");
   }
 
 
   public void endDraw() {
     report("top endDraw()");
 
     if (flushMode == FLUSH_WHEN_FULL) {
       // Flushing any remaining geometry.
       flush();
 
       // TODO: Implement depth sorting (http://code.google.com/p/processing/issues/detail?id=51)
       //if (hints[ENABLE_DEPTH_SORT]) {
       //  flush();
       //}
     }
 
     if (!drawing) {
       showWarning("P3D: Cannot call endDraw() before beginDraw().");
       return;
     }
 
     if (primarySurface) {
       pgl.endOnscreenDraw(clearColorBuffer0);
       pgl.glFlush();
     } else {
       if (offscreenMultisample) {
         offscreenFramebufferMultisample.copy(offscreenFramebuffer);
       }
       popFramebuffer();
 
       pgl.endOffscreenDraw(pg.clearColorBuffer0);
 
       pg.restoreGL();
     }
 
     drawing = false;
 
     report("bot endDraw()");
   }
 
 
   public PGL beginPGL() {
     return pgl;
   }
 
 
   public void endPGL() {
     restoreGL();
   }
 
 
   public void restartPGL() {
     pgl.initialized = false;
   }
 
 
   protected void restoreGL() {
     blendMode(blendMode);
 
     if (hints[DISABLE_DEPTH_TEST]) {
       pgl.glDisable(PGL.GL_DEPTH_TEST);
     } else {
       pgl.glEnable(PGL.GL_DEPTH_TEST);
     }
     pgl.glDepthFunc(PGL.GL_LEQUAL);
 
     if (antialias < 2) {
       pgl.glDisable(PGL.GL_MULTISAMPLE);
       pgl.glEnable(PGL.GL_POINT_SMOOTH);
       pgl.glEnable(PGL.GL_LINE_SMOOTH);
       pgl.glEnable(PGL.GL_POLYGON_SMOOTH);
     } else {
       pgl.glEnable(PGL.GL_MULTISAMPLE);
       pgl.glDisable(PGL.GL_POINT_SMOOTH);
       pgl.glDisable(PGL.GL_LINE_SMOOTH);
       pgl.glDisable(PGL.GL_POLYGON_SMOOTH);
     }
 
     pgl.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
 
     pgl.glFrontFace(PGL.GL_CW);
     pgl.glDisable(PGL.GL_CULL_FACE);
 
     pgl.glActiveTexture(PGL.GL_TEXTURE0);
 
     if (hints[DISABLE_DEPTH_MASK]) {
       pgl.glDepthMask(false);
     } else {
       pgl.glDepthMask(true);
     }
   }
 
 
   protected void beginPixelsOp(int op) {
     if (primarySurface) {
       if (op == OP_READ) {
         pgl.glReadBuffer(PGL.GL_FRONT);
       } else {
         pgl.glDrawBuffer(PGL.GL_BACK);
       }
       offscreenNotCurrent = false;
     } else {
       // Making sure that the offscreen FBO is current. This allows to do calls
       // like loadPixels(), set() or get() without enclosing them between
       // beginDraw()/endDraw() when working with a PGraphics object. We don't
       // need the rest of the surface initialization/finalization, since only
       // the pixels are affected.
       if (op == OP_READ) {
         // We always read the screen pixels from the color FBO.
         offscreenNotCurrent = offscreenFramebuffer != currentFramebuffer;
         if (offscreenNotCurrent) {
           pushFramebuffer();
           setFramebuffer(offscreenFramebuffer);
           pgl.updateOffscreen(pg.pgl);
         }
         pgl.glReadBuffer(PGL.GL_COLOR_ATTACHMENT0);
       } else {
         // We can write directly to the color FBO, or to the multisample FBO
         // if multisampling is enabled.
         if (offscreenMultisample) {
           offscreenNotCurrent = offscreenFramebufferMultisample != currentFramebuffer;
         } else {
           offscreenNotCurrent = offscreenFramebuffer != currentFramebuffer;
         }
         if (offscreenNotCurrent) {
           pushFramebuffer();
           if (offscreenMultisample) {
             setFramebuffer(offscreenFramebufferMultisample);
           } else {
             setFramebuffer(offscreenFramebuffer);
           }
           pgl.updateOffscreen(pg.pgl);
         }
         pgl.glDrawBuffer(PGL.GL_COLOR_ATTACHMENT0);
       }
     }
     pixelsOp = op;
   }
 
 
   protected void endPixelsOp() {
     if (offscreenNotCurrent) {
       if (pixelsOp == OP_WRITE && offscreenMultisample) {
         // We were writing to the multisample FBO, so we need
         // to blit its contents to the color FBO.
         offscreenFramebufferMultisample.copy(offscreenFramebuffer);
       }
       popFramebuffer();
     }
     pixelsOp = OP_NONE;
   }
 
 
   protected void updateGLProjection() {
     if (glProjection == null) {
       glProjection = new float[16];
     }
 
     glProjection[0] = projection.m00;
     glProjection[1] = projection.m10;
     glProjection[2] = projection.m20;
     glProjection[3] = projection.m30;
 
     glProjection[4] = projection.m01;
     glProjection[5] = projection.m11;
     glProjection[6] = projection.m21;
     glProjection[7] = projection.m31;
 
     glProjection[8] = projection.m02;
     glProjection[9] = projection.m12;
     glProjection[10] = projection.m22;
     glProjection[11] = projection.m32;
 
     glProjection[12] = projection.m03;
     glProjection[13] = projection.m13;
     glProjection[14] = projection.m23;
     glProjection[15] = projection.m33;
   }
 
 
   protected void updateGLModelview() {
     if (glModelview == null) {
       glModelview = new float[16];
     }
 
     glModelview[0] = modelview.m00;
     glModelview[1] = modelview.m10;
     glModelview[2] = modelview.m20;
     glModelview[3] = modelview.m30;
 
     glModelview[4] = modelview.m01;
     glModelview[5] = modelview.m11;
     glModelview[6] = modelview.m21;
     glModelview[7] = modelview.m31;
 
     glModelview[8] = modelview.m02;
     glModelview[9] = modelview.m12;
     glModelview[10] = modelview.m22;
     glModelview[11] = modelview.m32;
 
     glModelview[12] = modelview.m03;
     glModelview[13] = modelview.m13;
     glModelview[14] = modelview.m23;
     glModelview[15] = modelview.m33;
   }
 
 
   protected void calcProjmodelview() {
     projmodelview.set(projection);
     projmodelview.apply(modelview);
   }
 
 
   protected void updateGLProjmodelview() {
     if (glProjmodelview == null) {
       glProjmodelview = new float[16];
     }
 
     glProjmodelview[0] = projmodelview.m00;
     glProjmodelview[1] = projmodelview.m10;
     glProjmodelview[2] = projmodelview.m20;
     glProjmodelview[3] = projmodelview.m30;
 
     glProjmodelview[4] = projmodelview.m01;
     glProjmodelview[5] = projmodelview.m11;
     glProjmodelview[6] = projmodelview.m21;
     glProjmodelview[7] = projmodelview.m31;
 
     glProjmodelview[8] = projmodelview.m02;
     glProjmodelview[9] = projmodelview.m12;
     glProjmodelview[10] = projmodelview.m22;
     glProjmodelview[11] = projmodelview.m32;
 
     glProjmodelview[12] = projmodelview.m03;
     glProjmodelview[13] = projmodelview.m13;
     glProjmodelview[14] = projmodelview.m23;
     glProjmodelview[15] = projmodelview.m33;
   }
 
 
   protected void updateGLNormal() {
     if (glNormal == null) {
       glNormal = new float[9];
     }
 
     // The normal matrix is the transpose of the inverse of the
     // modelview (remember that gl matrices are column-major,
     // meaning that elements 0, 1, 2 are the first column,
     // 3, 4, 5 the second, etc.:
     glNormal[0] = modelviewInv.m00;
     glNormal[1] = modelviewInv.m01;
     glNormal[2] = modelviewInv.m02;
 
     glNormal[3] = modelviewInv.m10;
     glNormal[4] = modelviewInv.m11;
     glNormal[5] = modelviewInv.m12;
 
     glNormal[6] = modelviewInv.m20;
     glNormal[7] = modelviewInv.m21;
     glNormal[8] = modelviewInv.m22;
   }
 
 
   //////////////////////////////////////////////////////////////
 
   // SETTINGS
 
   // protected void checkSettings()
 
 
   protected void defaultSettings() {
     super.defaultSettings();
 
     manipulatingCamera = false;
 
     clearColorBuffer = false;
 
     if (fbStack == null) {
       fbStack = new Stack<PFramebuffer>();
 
       screenFramebuffer = new PFramebuffer(parent, width, height, true);
       setFramebuffer(screenFramebuffer);
     }
 
     if (modelviewStack == null) {
       modelviewStack = new Stack<PMatrix3D>();
     }
     if (modelviewInvStack == null) {
       modelviewInvStack = new Stack<PMatrix3D>();
     }
     if (projectionStack == null) {
       projectionStack = new Stack<PMatrix3D>();
     }
 
     // easiest for beginners
     textureMode(IMAGE);
 
     // Default material properties
     ambient(80);
     specular(125);
     emissive(0);
     shininess(1);
     
     // To indicate that the user hasn't set ambient
     setAmbient = false;
   }
 
 
   // reapplySettings
 
   //////////////////////////////////////////////////////////////
 
   // HINTS
 
 
   public void hint(int which) {
     boolean oldValue = hints[PApplet.abs(which)];
     super.hint(which);
     boolean newValue = hints[PApplet.abs(which)];
 
     if (oldValue == newValue) {
       return;
     }
 
     if (which == DISABLE_DEPTH_TEST) {
       flush();
       pgl.glDisable(PGL.GL_DEPTH_TEST);
     } else if (which == ENABLE_DEPTH_TEST) {
       flush();
       pgl.glEnable(PGL.GL_DEPTH_TEST);
     } else if (which == DISABLE_DEPTH_MASK) {
       flush();
       pgl.glDepthMask(false);
     } else if (which == ENABLE_DEPTH_MASK) {
       flush();
       pgl.glDepthMask(true);
     } else if (which == DISABLE_ACCURATE_2D) {
       flush();
       setFlushMode(FLUSH_WHEN_FULL);
     } else if (which == ENABLE_ACCURATE_2D) {
       flush();
       setFlushMode(FLUSH_CONTINUOUSLY);
     } else if (which == DISABLE_TEXTURE_CACHE) {
       flush();
     } else if (which == DISABLE_PERSPECTIVE_CORRECTED_LINES) {
       if (0 < tessGeo.lineVertexCount && 0 < tessGeo.lineIndexCount) {
         // We flush the geometry using the previous line setting.
         flush();
       }
       perspectiveCorrectedLines = false;
     } else if (which == ENABLE_PERSPECTIVE_CORRECTED_LINES) {
       if (0 < tessGeo.lineVertexCount && 0 < tessGeo.lineIndexCount) {
         // We flush the geometry using the previous line setting.
         flush();
       }
       perspectiveCorrectedLines = true;
     }
   }
 
 
   //////////////////////////////////////////////////////////////
 
   // SHAPE CREATORS
 
 
   public PShape createShape() {
     return createShape(POLYGON);
   }
 
 
   public PShape createShape(int type) {
     PShape3D shape = null;
     if (type == PShape.GROUP) {
       shape = new PShape3D(parent, PShape.GROUP);
     } else if (type == POINTS) {
       shape = new PShape3D(parent, PShape.GEOMETRY);
       shape.setKind(POINTS);
     } else if (type == LINES) {
       shape = new PShape3D(parent, PShape.GEOMETRY);
       shape.setKind(LINES);
     } else if (type == TRIANGLE || type == TRIANGLES) {
       shape = new PShape3D(parent, PShape.GEOMETRY);
       shape.setKind(TRIANGLES);
     } else if (type == TRIANGLE_FAN) {
       shape = new PShape3D(parent, PShape.GEOMETRY);
       shape.setKind(TRIANGLE_FAN);
     } else if (type == TRIANGLE_STRIP) {
       shape = new PShape3D(parent, PShape.GEOMETRY);
       shape.setKind(TRIANGLE_STRIP);
     } else if (type == QUAD || type == QUADS) {
       shape = new PShape3D(parent, PShape.GEOMETRY);
       shape.setKind(QUADS);
     } else if (type == QUAD_STRIP) {
       shape = new PShape3D(parent, PShape.GEOMETRY);
       shape.setKind(QUAD_STRIP);
     } else if (type == POLYGON) {
       shape = new PShape3D(parent, PShape.GEOMETRY);
       shape.setKind(POLYGON);
     }
     return shape;
   }
 
 
   public PShape createShape(int kind, float... p) {
     PShape3D shape = null;
     int len = p.length;
 
     if (kind == POINT) {
       if (len != 2) {
         showWarning("Wrong number of parameters");
         return null;
       }
       shape = new PShape3D(parent, PShape.PRIMITIVE);
       shape.setKind(POINT);
     } else if (kind == LINE) {
       if (len != 4) {
         showWarning("Wrong number of parameters");
         return null;
       }
       shape = new PShape3D(parent, PShape.PRIMITIVE);
       shape.setKind(LINE);
     } else if (kind == TRIANGLE) {
       if (len != 6) {
         showWarning("Wrong number of parameters");
         return null;
       }
       shape = new PShape3D(parent, PShape.PRIMITIVE);
       shape.setKind(TRIANGLE);
     } else if (kind == QUAD) {
       if (len != 8) {
         showWarning("Wrong number of parameters");
         return null;
       }
       shape = new PShape3D(parent, PShape.PRIMITIVE);
       shape.setKind(QUAD);
     } else if (kind == RECT) {
       if (len != 4 && len != 5 && len != 8) {
         showWarning("Wrong number of parameters");
         return null;
       }
       shape = new PShape3D(parent, PShape.PRIMITIVE);
       shape.setKind(RECT);
     } else if (kind == ELLIPSE) {
       if (len != 4) {
         showWarning("Wrong number of parameters");
         return null;
       }
       shape = new PShape3D(parent, PShape.PRIMITIVE);
       shape.setKind(ELLIPSE);
     } else if (kind == ARC) {
       if (len != 6) {
         showWarning("Wrong number of parameters");
         return null;
       }
       shape = new PShape3D(parent, PShape.PRIMITIVE);
       shape.setKind(ARC);
     } else if (kind == BOX) {
       if (len != 1 && len != 3) {
         showWarning("Wrong number of parameters");
         return null;
       }
       shape = new PShape3D(parent, PShape.PRIMITIVE);
       shape.setKind(BOX);
     } else if (kind == SPHERE) {
       if (len != 1) {
         showWarning("Wrong number of parameters");
         return null;
       }
       shape = new PShape3D(parent, PShape.PRIMITIVE);
       shape.setKind(SPHERE);
     } else {
       showWarning("Unrecognized primitive type");
     }
 
     if (shape != null) {
       shape.setParams(p);
     }
 
     return shape;
   }
 
 
   //////////////////////////////////////////////////////////////
 
   // VERTEX SHAPES
 
 
   public void beginShape(int kind) {
     shape = kind;
 
     inGeo.clear();
 
     breakShape = false;
     defaultEdges = true;
 
     textureImage0 = textureImage;
     // The superclass method is called to avoid an early flush.
     super.noTexture();
 
     normalMode = NORMAL_MODE_AUTO;
   }
 
 
   public void endShape(int mode) {
     if (flushMode == FLUSH_WHEN_FULL && hints[DISABLE_TEXTURE_CACHE] &&
         textureImage0 != null && textureImage == null) {
       // The previous shape had a texture and this one doesn't. So we need to flush
       // the textured geometry.
       textureImage = textureImage0;
       flush();
       textureImage = null;
     }
 
     tessellate(mode);
 
     if (flushMode == FLUSH_CONTINUOUSLY ||
         (flushMode == FLUSH_WHEN_FULL && tessGeo.isFull())) {
 
      if (flushMode == FLUSH_WHEN_FULL && tessGeo.isOverflow()) {
        PGraphics.showWarning("P3D: tessellated arrays are overflowing");
      }

       flush();
     }
   }
 
   protected void endShape(int[] indices) {
     endShape(indices, null);
   }
 
   protected void endShape(int[] indices, int[] edges) {
     if (shape != TRIANGLE && shape != TRIANGLES) {
       throw new RuntimeException("Indices and edges can only be set for TRIANGLE shapes");
     }
     
     if (flushMode == FLUSH_WHEN_FULL && hints[DISABLE_TEXTURE_CACHE] &&
         textureImage0 != null && textureImage == null) {
       // The previous shape had a texture and this one doesn't. So we need to flush
       // the textured geometry.
       textureImage = textureImage0;
       flush();
       textureImage = null;
     }   
 
     tessellate(indices, edges);    
     
     if (flushMode == FLUSH_CONTINUOUSLY ||
         (flushMode == FLUSH_WHEN_FULL && tessGeo.isFull())) {
 
      if (flushMode == FLUSH_WHEN_FULL && tessGeo.isOverflow()) {
        PGraphics.showWarning("P3D: tessellated arrays are overflowing");
      }

       flush();
     }    
   }
 
   public void texture(PImage image) {
     if (flushMode == FLUSH_WHEN_FULL && hints[DISABLE_TEXTURE_CACHE] &&
         image != textureImage0) {
       // Changing the texture image, so we need to flush the
       // tessellated geometry accumulated until now, so that
       // textures are not mixed.
       textureImage = textureImage0;
       flush();
     }
     super.texture(image);
   }
 
 
   public void noTexture() {
     if (flushMode == FLUSH_WHEN_FULL && hints[DISABLE_TEXTURE_CACHE] &&
         null != textureImage0) {
       // Changing the texture image, so we need to flush the
       // tessellated geometry accumulated until now, so that
       // textures are not mixed.
       textureImage = textureImage0;
       flush();
     }
     super.noTexture();
   }
 
 
   public void beginContour() {
     if (openContour) {
       showWarning("P3D: Already called beginContour().");
       return;
     }
     openContour = true;
   }
 
 
   public void endContour() {
     if (!openContour) {
       showWarning("P3D: Need to call beginContour() first.");
       return;
     }
     openContour = false;
     breakShape = true;
   }
 
 
   public void vertex(float x, float y) {
     vertex(x, y, 0, 0, 0);
   }
 
 
   public void vertex(float x, float y, float u, float v) {
     vertex(x, y, 0, u, v);
   }
 
 
   public void vertex(float x, float y, float z) {
     vertex(x, y, z, 0, 0);
   }
 
 
   public void vertex(float x, float y, float z, float u, float v) {
     vertexImpl(x, y, z, u, v, VERTEX);
   }
 
 
   protected void vertexImpl(float x, float y, float z, float u, float v, int code) {
     if (inGeo.isFull()) {
       PGraphics.showWarning("P3D: Too many vertices, try creating smaller shapes");
       return;
     }
 
     boolean textured = textureImage != null;
     int fcolor = 0x00;
     if (fill || textured) {
       if (!textured) {
         fcolor = fillColor;
       } else {
         if (tint) {
           fcolor = tintColor;
         } else {
           fcolor = 0xffFFFFFF;
         }
       }
     }
 
     int scolor = 0x00;
     float sweight = 0;
     if (stroke) {
       scolor = strokeColor;
       sweight = strokeWeight;
     }
 
     if (breakShape) {
       code = PShape.BREAK;
       breakShape = false;
     }
 
     if (textured && textureMode == IMAGE) {
       u = PApplet.min(1, u / textureImage.width);
       v = PApplet.min(1, v / textureImage.height);
     }
 
     inGeo.addVertex(x, y, z,
                  fcolor,
                  normalX, normalY, normalZ,
                  u, v,
                  scolor, sweight,
                  ambientColor, specularColor, emissiveColor, shininess,
                  code);
   }
 
 
   public void clip(float a, float b, float c, float d) {
     if (imageMode == CORNER) {
       if (c < 0) {  // reset a negative width
         a += c; c = -c;
       }
       if (d < 0) {  // reset a negative height
         b += d; d = -d;
       }
 
       clipImpl(a, b, a + c, b + d);
 
     } else if (imageMode == CORNERS) {
       if (c < a) {  // reverse because x2 < x1
         float temp = a; a = c; c = temp;
       }
       if (d < b) {  // reverse because y2 < y1
         float temp = b; b = d; d = temp;
       }
 
       clipImpl(a, b, c, d);
 
     } else if (imageMode == CENTER) {
       // c and d are width/height
       if (c < 0) c = -c;
       if (d < 0) d = -d;
       float x1 = a - c/2;
       float y1 = b - d/2;
 
       clipImpl(x1, y1, x1 + c, y1 + d);
     }
   }
 
 
   protected void clipImpl(float x1, float y1, float x2, float y2) {
     flush();
     pgl.glEnable(PGL.GL_SCISSOR_TEST);
 
     float h = y2 - y1;
     pgl.glScissor((int)x1, (int)(height - y1 - h), (int)(x2 - x1), (int)h);
 
     clip = true;
   }
 
 
   public void noClip() {
     if (clip) {
       flush();
       pgl.glDisable(PGL.GL_SCISSOR_TEST);
       clip = false;
     }
   }
 
 
   //////////////////////////////////////////////////////////////
 
   // RENDERING
 
   // protected void render()
 
   // protected void sort()
 
 
   protected void tessellate(int mode) {
     tessellator.setInGeometry(inGeo);
     tessellator.setTessGeometry(tessGeo);
     tessellator.setFill(fill || textureImage != null);
     tessellator.setStroke(stroke);
     tessellator.setStrokeWeight(strokeWeight);
     tessellator.setStrokeCap(strokeCap);
     tessellator.setStrokeJoin(strokeJoin);
 
     setFirstTexIndex(tessGeo.fillIndexCount);
 
     if (shape == POINTS) {
       tessellator.tessellatePoints();
     } else if (shape == LINES) {
       tessellator.tessellateLines();
     } else if (shape == TRIANGLE || shape == TRIANGLES) {
       if (stroke && defaultEdges) inGeo.addTrianglesEdges();
       if (normalMode == NORMAL_MODE_AUTO) inGeo.calcTrianglesNormals();
       tessellator.tessellateTriangles();
     } else if (shape == TRIANGLE_FAN) {
       if (stroke && defaultEdges) inGeo.addTriangleFanEdges();
       if (normalMode == NORMAL_MODE_AUTO) inGeo.calcTriangleFanNormals();
       tessellator.tessellateTriangleFan();
     } else if (shape == TRIANGLE_STRIP) {
       if (stroke && defaultEdges) inGeo.addTriangleStripEdges();
       if (normalMode == NORMAL_MODE_AUTO) inGeo.calcTriangleStripNormals();
       tessellator.tessellateTriangleStrip();
     } else if (shape == QUAD || shape == QUADS) {
       if (stroke && defaultEdges) inGeo.addQuadsEdges();
       if (normalMode == NORMAL_MODE_AUTO) inGeo.calcQuadsNormals();
       tessellator.tessellateQuads();
     } else if (shape == QUAD_STRIP) {
       if (stroke && defaultEdges) inGeo.addQuadStripEdges();
       if (normalMode == NORMAL_MODE_AUTO) inGeo.calcQuadStripNormals();
       tessellator.tessellateQuadStrip();
     } else if (shape == POLYGON) {
       if (stroke && defaultEdges) inGeo.addPolygonEdges(mode == CLOSE);
       tessellator.tessellatePolygon(false, mode == CLOSE, normalMode == NORMAL_MODE_AUTO);
     }
 
     setLastTexIndex(tessGeo.lastFillIndex);
   }
 
   protected void tessellate(int[] indices, int[] edges) {
     if (edges != null) {
       int nedges = edges.length / 2;
       for (int n = 0; n < nedges; n++) {
         int i0 = edges[2 * n + 0];
         int i1 = edges[2 * n + 1];
         inGeo.addEdge(i0, i1, n == 0, n == nedges - 1);            
       }
     }
     
     tessellator.setInGeometry(inGeo);
     tessellator.setTessGeometry(tessGeo);
     tessellator.setFill(fill || textureImage != null);
     tessellator.setStroke(stroke);
     tessellator.setStrokeWeight(strokeWeight);
     tessellator.setStrokeCap(strokeCap);
     tessellator.setStrokeJoin(strokeJoin);
 
     setFirstTexIndex(tessGeo.fillIndexCount);
     
     if (stroke && defaultEdges && edges == null) inGeo.addTrianglesEdges();
     if (normalMode == NORMAL_MODE_AUTO) inGeo.calcTrianglesNormals();
     tessellator.tessellateTriangles(indices);    
     
     setLastTexIndex(tessGeo.lastFillIndex);
   }
   
 
   protected void setFirstTexIndex(int first) {
     firstTexIndex = first;
   }
 
 
   protected void setLastTexIndex(int last) {
     if (textureImage0 != textureImage || texCache.count == 0) {
       texCache.addTexture(textureImage, firstTexIndex, last);
     } else {
       texCache.setLastIndex(last);
     }
   }
 
 
   public void flush() {
     boolean hasPoints = 0 < tessGeo.pointVertexCount && 0 < tessGeo.pointIndexCount;
     boolean hasLines = 0 < tessGeo.lineVertexCount && 0 < tessGeo.lineIndexCount;
     boolean hasFill = 0 < tessGeo.fillVertexCount && 0 < tessGeo.fillIndexCount;
     boolean hasPixels = modified && pixels != null;
 
     if (hasPixels) {
       // If the user has been manipulating individual pixels,
       // the changes need to be copied to the screen before
       // drawing any new geometry.
       renderPixels();
       setgetPixels = false;
     }
 
     if (hasPoints || hasLines || hasFill) {
       if (flushMode == FLUSH_WHEN_FULL && !hints[DISABLE_TRANSFORM_CACHE]) {
         // The modelview transformation has been applied already to the
         // tessellated vertices, so we set the OpenGL modelview matrix as
         // the identity to avoid applying the model transformations twice.
         pushMatrix();
         resetMatrix();
       }
 
       if (hasFill) {
         renderFill();
       }
 
       if (hasPoints) {
         renderPoints();
       }
 
       if (hasLines) {
         renderLines();
       }
 
       if (flushMode == FLUSH_WHEN_FULL && !hints[DISABLE_TRANSFORM_CACHE]) {
         popMatrix();
       }
     }
 
     tessGeo.clear();
     texCache.clear();
   }
 
 
   protected void renderPixels() {
     int mi1 = my1 * width + mx1;
     int mi2 = my2 * width + mx2;
     int mw = mx2 - mx1 + 1;
     int mh = my2 - my1 + 1;
     int mlen = mi2 - mi1 + 1;
 
     if (rgbaPixels == null || rgbaPixels.length < mlen) {
       rgbaPixels = new int[mlen];
     }
 
     PApplet.arrayCopy(pixels, mi1, rgbaPixels, 0, mlen);
     PGL.javaToNativeARGB(rgbaPixels, mw, mh);
 
     //PApplet.arrayCopy(pixels, rgbaPixels);
     //PGL.javaToNativeARGB(rgbaPixels, width, height);
 
     // Copying pixel buffer to screen texture...
     pgl.copyToTexture(texture.glTarget, texture.glFormat, texture.glID,
                       mx1, my1, mw, mh, IntBuffer.wrap(rgbaPixels));
 
     if (primarySurface || offscreenMultisample) {
       // ...and drawing the texture to screen... but only
       // if we are on the primary surface or we have
       // multisampled FBO. Why? Because in the case of non-
       // multisampled FBO, texture is actually the color buffer
       // used by the color FBO, so with the copy operation we
       // should be done updating the (off)screen buffer.
       beginPixelsOp(OP_WRITE);
       drawTexture(mx1, my1, mw, mh);
       endPixelsOp();
     }
 
     modified = false;
   }
 
 
   protected void renderPoints() {
     if (!pointVBOsCreated) {
       createPointBuffers();
       pointVBOsCreated = true;
     }
     updatePointBuffers();
 
     PointShader shader = getPointShader();
     shader.start();
     shader.setVertexAttribute(glPointVertexBufferID, 3, PGL.GL_FLOAT, 0, 0);
     shader.setColorAttribute(glPointColorBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 0);
     shader.setSizeAttribute(glPointSizeBufferID, 2, PGL.GL_FLOAT, 0, 0);
 
     pgl.glDrawElements(PGL.GL_TRIANGLES, tessGeo.pointIndexCount, PGL.INDEX_TYPE, 0);
 
     shader.stop();
     unbindPointBuffers();
   }
 
 
   protected void renderLines() {
     if (!lineVBOsCreated) {
       createLineBuffers();
       lineVBOsCreated = true;
     }
     updateLineBuffers();
 
     LineShader shader = getLineShader();
     shader.start();
 
     shader.setVertexAttribute(glLineVertexBufferID, 3, PGL.GL_FLOAT, 0, 0);
     shader.setColorAttribute(glLineColorBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 0);
     shader.setDirWidthAttribute(glLineDirWidthBufferID, 4, PGL.GL_FLOAT, 0, 0);
 
     pgl.glDrawElements(PGL.GL_TRIANGLES, tessGeo.lineIndexCount, PGL.INDEX_TYPE, 0);
 
     shader.stop();
     unbindLineBuffers();
   }
 
 
   protected void renderFill() {
     if (!fillVBOsCreated) {
       createFillBuffers();
       fillVBOsCreated = true;
     }
     updateFillBuffers(lights, texCache.hasTexture);
 
     texCache.beginRender();
     for (int i = 0; i < texCache.count; i++) {
       PTexture tex = texCache.getTexture(i);
 
       FillShader shader = getFillShader(lights, tex != null);
       shader.start();
 
       shader.setVertexAttribute(glFillVertexBufferID, 3, PGL.GL_FLOAT, 0, 0);
       shader.setColorAttribute(glFillColorBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 0);
 
       if (lights) {
         shader.setNormalAttribute(glFillNormalBufferID, 3, PGL.GL_FLOAT, 0, 0);
         shader.setAmbientAttribute(glFillAmbientBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 0);
         shader.setSpecularAttribute(glFillSpecularBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 0);
         shader.setEmissiveAttribute(glFillEmissiveBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 0);
         shader.setShininessAttribute(glFillShininessBufferID, 1, PGL.GL_FLOAT, 0, 0);
       }
 
       if (tex != null) {
         shader.setTexCoordAttribute(glFillTexCoordBufferID, 2, PGL.GL_FLOAT, 0, 0);
         shader.setTexture(tex);
       }
 
       int offset = texCache.firstIndex[i];
       int size = texCache.lastIndex[i] - texCache.firstIndex[i] + 1;
       pgl.glDrawElements(PGL.GL_TRIANGLES, size, PGL.INDEX_TYPE, offset * PGL.SIZEOF_INDEX);
 
       shader.stop();
     }
     texCache.endRender();
     unbindFillBuffers();
   }
 
 
   // Utility function to render current tessellated geometry, under the assumption that
   // the texture is already bound.
   protected void renderTexFill(PTexture tex) {
     if (!fillVBOsCreated) {
       createFillBuffers();
       fillVBOsCreated = true;
     }
     updateFillBuffers(lights, true);
 
     FillShader shader = getFillShader(lights, true);
     shader.start();
 
     shader.setVertexAttribute(glFillVertexBufferID, 3, PGL.GL_FLOAT, 0, 0);
     shader.setColorAttribute(glFillColorBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 0);
     shader.setTexCoordAttribute(glFillTexCoordBufferID, 2, PGL.GL_FLOAT, 0, 0);
     shader.setTexture(tex);
 
     if (lights) {
       shader.setNormalAttribute(glFillNormalBufferID, 3, PGL.GL_FLOAT, 0, 0);
       shader.setAmbientAttribute(glFillAmbientBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 0);
       shader.setSpecularAttribute(glFillSpecularBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 0);
       shader.setEmissiveAttribute(glFillEmissiveBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 0);
       shader.setShininessAttribute(glFillShininessBufferID, 1, PGL.GL_FLOAT, 0, 0);
     }
 
     int size = tessGeo.fillIndexCount;
     int sizex = size * PGL.SIZEOF_INDEX;
     pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glFillIndexBufferID);
     pgl.glBufferData(PGL.GL_ELEMENT_ARRAY_BUFFER, sizex, ShortBuffer.wrap(tessGeo.fillIndices, 0, size), PGL.GL_STATIC_DRAW);
     pgl.glDrawElements(PGL.GL_TRIANGLES, size, PGL.INDEX_TYPE, 0);
     pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);
 
     shader.stop();
   }
 
 
   //////////////////////////////////////////////////////////////
 
   // BEZIER CURVE VERTICES
 
 
   public void bezierDetail(int detail) {
     bezierDetail = detail;
 
     if (bezierDrawMatrix == null) {
       bezierDrawMatrix = new PMatrix3D();
     }
 
     // setup matrix for forward differencing to speed up drawing
     pg.splineForward(detail, bezierDrawMatrix);
 
     // multiply the basis and forward diff matrices together
     // saves much time since this needn't be done for each curve
     bezierDrawMatrix.apply(pg.bezierBasisMatrix);
   }
 
 
   public void bezierVertex(float x2, float y2,
                            float x3, float y3,
                            float x4, float y4) {
     bezierVertex(x2, y2, 0,
                  x3, y3, 0,
                  x4, y4, 0);
   }
 
 
   public void bezierVertex(float x2, float y2, float z2,
                            float x3, float y3, float z3,
                            float x4, float y4, float z4) {
     bezierInitCheck();
     bezierVertexCheck();
     PMatrix3D draw = bezierDrawMatrix;
 
     float x1 = inGeo.getLastVertexX();
     float y1 = inGeo.getLastVertexY();
     float z1 = inGeo.getLastVertexZ();
 
     float xplot1 = draw.m10*x1 + draw.m11*x2 + draw.m12*x3 + draw.m13*x4;
     float xplot2 = draw.m20*x1 + draw.m21*x2 + draw.m22*x3 + draw.m23*x4;
     float xplot3 = draw.m30*x1 + draw.m31*x2 + draw.m32*x3 + draw.m33*x4;
 
     float yplot1 = draw.m10*y1 + draw.m11*y2 + draw.m12*y3 + draw.m13*y4;
     float yplot2 = draw.m20*y1 + draw.m21*y2 + draw.m22*y3 + draw.m23*y4;
     float yplot3 = draw.m30*y1 + draw.m31*y2 + draw.m32*y3 + draw.m33*y4;
 
     float zplot1 = draw.m10*z1 + draw.m11*z2 + draw.m12*z3 + draw.m13*z4;
     float zplot2 = draw.m20*z1 + draw.m21*z2 + draw.m22*z3 + draw.m23*z4;
     float zplot3 = draw.m30*z1 + draw.m31*z2 + draw.m32*z3 + draw.m33*z4;
 
     for (int j = 0; j < bezierDetail; j++) {
       x1 += xplot1; xplot1 += xplot2; xplot2 += xplot3;
       y1 += yplot1; yplot1 += yplot2; yplot2 += yplot3;
       z1 += zplot1; zplot1 += zplot2; zplot2 += zplot3;
       vertexImpl(x1, y1, z1, 0, 0, BEZIER_VERTEX);
     }
   }
 
 
   public void quadraticVertex(float cx, float cy,
                               float x3, float y3) {
     quadraticVertex(cx, cy, 0,
                     x3, y3, 0);
   }
 
 
   public void quadraticVertex(float cx, float cy, float cz,
                               float x3, float y3, float z3) {
     float x1 = inGeo.getLastVertexX();
     float y1 = inGeo.getLastVertexY();
     float z1 = inGeo.getLastVertexZ();
 
     bezierVertex(x1 + ((cx-x1)*2/3.0f), y1 + ((cy-y1)*2/3.0f), z1 + ((cz-z1)*2/3.0f),
                  x3 + ((cx-x3)*2/3.0f), y3 + ((cy-y3)*2/3.0f), z3 + ((cz-z3)*2/3.0f),
                  x3, y3, z3);
   }
 
 
   protected void bezierInitCheck() {
     if (!bezierInited) {
       bezierInit();
     }
   }
 
 
   protected void bezierInit() {
     // overkill to be broken out, but better parity with the curve stuff below
     bezierDetail(bezierDetail);
     bezierInited = true;
   }
 
 
   protected void bezierVertexCheck() {
     if (shape != POLYGON) {
       throw new RuntimeException("beginShape() or beginShape(POLYGON) " +
                                  "must be used before bezierVertex() or quadraticVertex()");
     }
     if (inGeo.vertexCount == 0) {
       throw new RuntimeException("vertex() must be used at least once" +
                                  "before bezierVertex() or quadraticVertex()");
     }
   }
 
 
   //////////////////////////////////////////////////////////////
 
   // CATMULL-ROM CURVE VERTICES
 
 
   public void curveDetail(int detail) {
     curveDetail = detail;
     curveInit();
   }
 
 
   public void curveTightness(float tightness) {
     curveTightness = tightness;
     curveInit();
   }
 
 
   public void curveVertex(float x, float y) {
     curveVertex(x, y, 0);
   }
 
 
   public void curveVertex(float x, float y, float z) {
     curveVertexCheck();
     float[] vertex = curveVertices[curveVertexCount];
     vertex[X] = x;
     vertex[Y] = y;
     vertex[Z] = z;
     curveVertexCount++;
 
     // draw a segment if there are enough points
     if (curveVertexCount > 3) {
       curveVertexSegment(curveVertices[curveVertexCount-4][X],
                          curveVertices[curveVertexCount-4][Y],
                          curveVertices[curveVertexCount-4][Z],
                          curveVertices[curveVertexCount-3][X],
                          curveVertices[curveVertexCount-3][Y],
                          curveVertices[curveVertexCount-3][Z],
                          curveVertices[curveVertexCount-2][X],
                          curveVertices[curveVertexCount-2][Y],
                          curveVertices[curveVertexCount-2][Z],
                          curveVertices[curveVertexCount-1][X],
                          curveVertices[curveVertexCount-1][Y],
                          curveVertices[curveVertexCount-1][Z]);
     }
 
   }
 
 
   protected void curveVertexCheck() {
     if (shape != POLYGON) {
       throw new RuntimeException("You must use createGeometry() or " +
                                  "createGeometry(POLYGON) before curveVertex()");
     }
 
     // to improve code init time, allocate on first use.
     if (curveVertices == null) {
       curveVertices = new float[128][3];
     }
 
     if (curveVertexCount == curveVertices.length) {
       // Can't use PApplet.expand() cuz it doesn't do the copy properly
       float[][] temp = new float[curveVertexCount << 1][3];
       System.arraycopy(curveVertices, 0, temp, 0, curveVertexCount);
       curveVertices = temp;
     }
     curveInitCheck();
   }
 
 
   protected void curveInitCheck() {
     if (!curveInited) {
       curveInit();
     }
   }
 
 
   protected void curveInit() {
     // allocate only if/when used to save startup time
     if (curveDrawMatrix == null) {
       curveBasisMatrix = new PMatrix3D();
       curveDrawMatrix = new PMatrix3D();
       curveInited = true;
     }
 
     float s = curveTightness;
     curveBasisMatrix.set((s-1)/2f, (s+3)/2f,  (-3-s)/2f, (1-s)/2f,
                          (1-s),    (-5-s)/2f, (s+2),     (s-1)/2f,
                          (s-1)/2f, 0,         (1-s)/2f,  0,
                          0,        1,         0,         0);
 
     pg.splineForward(curveDetail, curveDrawMatrix);
 
     if (bezierBasisInverse == null) {
       bezierBasisInverse = pg.bezierBasisMatrix.get();
       bezierBasisInverse.invert();
       curveToBezierMatrix = new PMatrix3D();
     }
 
     curveToBezierMatrix.set(curveBasisMatrix);
     curveToBezierMatrix.preApply(bezierBasisInverse);
 
     // multiply the basis and forward diff matrices together
     // saves much time since this needn't be done for each curve
     curveDrawMatrix.apply(curveBasisMatrix);
   }
 
 
   /**
    * Handle emitting a specific segment of Catmull-Rom curve. This can be
    * overridden by subclasses that need more efficient rendering options.
    */
   protected void curveVertexSegment(float x1, float y1, float z1,
                                     float x2, float y2, float z2,
                                     float x3, float y3, float z3,
                                     float x4, float y4, float z4) {
     float x0 = x2;
     float y0 = y2;
     float z0 = z2;
 
     PMatrix3D draw = curveDrawMatrix;
 
     float xplot1 = draw.m10*x1 + draw.m11*x2 + draw.m12*x3 + draw.m13*x4;
     float xplot2 = draw.m20*x1 + draw.m21*x2 + draw.m22*x3 + draw.m23*x4;
     float xplot3 = draw.m30*x1 + draw.m31*x2 + draw.m32*x3 + draw.m33*x4;
 
     float yplot1 = draw.m10*y1 + draw.m11*y2 + draw.m12*y3 + draw.m13*y4;
     float yplot2 = draw.m20*y1 + draw.m21*y2 + draw.m22*y3 + draw.m23*y4;
     float yplot3 = draw.m30*y1 + draw.m31*y2 + draw.m32*y3 + draw.m33*y4;
 
     float zplot1 = draw.m10*z1 + draw.m11*z2 + draw.m12*z3 + draw.m13*z4;
     float zplot2 = draw.m20*z1 + draw.m21*z2 + draw.m22*z3 + draw.m23*z4;
     float zplot3 = draw.m30*z1 + draw.m31*z2 + draw.m32*z3 + draw.m33*z4;
 
     vertexImpl(x0, y0, z0, 0, 0, CURVE_VERTEX);
     for (int j = 0; j < curveDetail; j++) {
       x0 += xplot1; xplot1 += xplot2; xplot2 += xplot3;
       y0 += yplot1; yplot1 += yplot2; yplot2 += yplot3;
       z0 += zplot1; zplot1 += zplot2; zplot2 += zplot3;
       vertexImpl(x0, y0, z0, 0, 0, CURVE_VERTEX);
     }
   }
 
 
   //////////////////////////////////////////////////////////////
 
   // SPLINE UTILITY FUNCTIONS (used by both Bezier and Catmull-Rom)
 
 
   /**
    * Setup forward-differencing matrix to be used for speedy
    * curve rendering. It's based on using a specific number
    * of curve segments and just doing incremental adds for each
    * vertex of the segment, rather than running the mathematically
    * expensive cubic equation.
    * @param segments number of curve segments to use when drawing
    * @param matrix target object for the new matrix
    */
   protected void splineForward(int segments, PMatrix3D matrix) {
     float f  = 1.0f / segments;
     float ff = f * f;
     float fff = ff * f;
 
     matrix.set(0,     0,    0, 1,
                fff,   ff,   f, 0,
                6*fff, 2*ff, 0, 0,
                6*fff, 0,    0, 0);
   }
 
 
   //////////////////////////////////////////////////////////////
 
   // POINT, LINE, TRIANGLE, QUAD
 
   // Because vertex(x, y) is mapped to vertex(x, y, 0), none of these commands
   // need to be overridden from their default implementation in PGraphics.
 
   // public void point(float x, float y)
 
   // public void point(float x, float y, float z)
 
   // public void line(float x1, float y1, float x2, float y2)
 
   // public void line(float x1, float y1, float z1,
   // float x2, float y2, float z2)
 
   // public void triangle(float x1, float y1, float x2, float y2,
   // float x3, float y3)
 
   // public void quad(float x1, float y1, float x2, float y2,
   // float x3, float y3, float x4, float y4)
 
   //////////////////////////////////////////////////////////////
 
   // RECT
 
   // public void rectMode(int mode)
 
   // public void rect(float a, float b, float c, float d)
 
   // protected void rectImpl(float x1, float y1, float x2, float y2)
 
   //////////////////////////////////////////////////////////////
 
   // ELLIPSE
 
   // public void ellipseMode(int mode)
 
 
   public void ellipse(float a, float b, float c, float d) {
      beginShape(TRIANGLE_FAN);
      defaultEdges = false;
      normalMode = NORMAL_MODE_SHAPE;
      inGeo.addEllipse(ellipseMode, a, b, c, d,
                       fill, fillColor,
                       stroke, strokeColor, strokeWeight,
                       ambientColor, specularColor, emissiveColor, shininess);
      endShape();
   }
 
 
   // public void ellipse(float a, float b, float c, float d)
 
   // public void arc(float a, float b, float c, float d,
   // float start, float stop)
 
   //////////////////////////////////////////////////////////////
 
   // ARC
 
 
   protected void arcImpl(float x, float y, float w, float h,
                          float start, float stop) {
     float hr = w / 2f;
     float vr = h / 2f;
 
     float centerX = x + hr;
     float centerY = y + vr;
 
     if (fill) {
       // shut off stroke for a minute
       boolean savedStroke = stroke;
       stroke = false;
 
       int startLUT = (int) (0.5f + (start / TWO_PI) * SINCOS_LENGTH);
       int stopLUT = (int) (0.5f + (stop / TWO_PI) * SINCOS_LENGTH);
 
       beginShape(TRIANGLE_FAN);
       vertex(centerX, centerY);
       int increment = 1; // what's a good algorithm? stopLUT - startLUT;
       for (int i = startLUT; i < stopLUT; i += increment) {
         int ii = i % SINCOS_LENGTH;
         // modulo won't make the value positive
         if (ii < 0) ii += SINCOS_LENGTH;
         vertex(centerX + cosLUT[ii] * hr,
                centerY + sinLUT[ii] * vr);
       }
       // draw last point explicitly for accuracy
       vertex(centerX + cosLUT[stopLUT % SINCOS_LENGTH] * hr,
              centerY + sinLUT[stopLUT % SINCOS_LENGTH] * vr);
       endShape();
 
       stroke = savedStroke;
     }
 
     if (stroke) {
       // Almost identical to above, but this uses a LINE_STRIP
       // and doesn't include the first (center) vertex.
 
       boolean savedFill = fill;
       fill = false;
 
       int startLUT = (int) (0.5f + (start / TWO_PI) * SINCOS_LENGTH);
       int stopLUT = (int) (0.5f + (stop / TWO_PI) * SINCOS_LENGTH);
 
       beginShape(); //LINE_STRIP);
       int increment = 1; // what's a good algorithm? stopLUT - startLUT;
       for (int i = startLUT; i < stopLUT; i += increment) {
         int ii = i % SINCOS_LENGTH;
         if (ii < 0) ii += SINCOS_LENGTH;
         vertex(centerX + cosLUT[ii] * hr,
                centerY + sinLUT[ii] * vr);
       }
       // draw last point explicitly for accuracy
       vertex(centerX + cosLUT[stopLUT % SINCOS_LENGTH] * hr,
              centerY + sinLUT[stopLUT % SINCOS_LENGTH] * vr);
       endShape();
 
       fill = savedFill;
     }
   }
 
 
   //////////////////////////////////////////////////////////////
 
   // BOX
 
   // public void box(float size)
   
   public void box(float w, float h, float d) {
     beginShape(QUADS);
     defaultEdges = false;
     normalMode = NORMAL_MODE_VERTEX;
     inGeo.addBox(w, h, d,
                  fill, fillColor, 
                  stroke, strokeColor, strokeWeight,
                  ambientColor, specularColor, emissiveColor, 
                  shininess);
     endShape();
   }
 
   //////////////////////////////////////////////////////////////
 
   // SPHERE
 
   // public void sphereDetail(int res)
 
   // public void sphereDetail(int ures, int vres)
   
   public void sphere(float r) {
     beginShape(TRIANGLES);
     defaultEdges = false;
     normalMode = NORMAL_MODE_VERTEX;
     int[] indices = inGeo.addSphere(r, sphereDetailU, sphereDetailV, 
                                     fill, fillColor, 
                                     stroke, strokeColor, strokeWeight,
                                     ambientColor, specularColor, emissiveColor, 
                                     shininess);    
     endShape(indices);
   }
 
   //////////////////////////////////////////////////////////////
 
   // BEZIER
 
   // public float bezierPoint(float a, float b, float c, float d, float t)
 
   // public float bezierTangent(float a, float b, float c, float d, float t)
 
   // public void bezierDetail(int detail)
 
   // public void bezier(float x1, float y1,
   // float x2, float y2,
   // float x3, float y3,
   // float x4, float y4)
 
   // public void bezier(float x1, float y1, float z1,
   // float x2, float y2, float z2,
   // float x3, float y3, float z3,
   // float x4, float y4, float z4)
 
   //////////////////////////////////////////////////////////////
 
   // CATMULL-ROM CURVES
 
   // public float curvePoint(float a, float b, float c, float d, float t)
 
   // public float curveTangent(float a, float b, float c, float d, float t)
 
   // public void curveDetail(int detail)
 
   // public void curveTightness(float tightness)
 
   // public void curve(float x1, float y1,
   // float x2, float y2,
   // float x3, float y3,
   // float x4, float y4)
 
   // public void curve(float x1, float y1, float z1,
   // float x2, float y2, float z2,
   // float x3, float y3, float z3,
   // float x4, float y4, float z4)
 
   //////////////////////////////////////////////////////////////
 
   // IMAGES
 
   // public void imageMode(int mode)
 
   // public void image(PImage image, float x, float y)
 
   // public void image(PImage image, float x, float y, float c, float d)
 
   // public void image(PImage image,
   // float a, float b, float c, float d,
   // int u1, int v1, int u2, int v2)
 
   // protected void imageImpl(PImage image,
   // float x1, float y1, float x2, float y2,
   // int u1, int v1, int u2, int v2)
 
   //////////////////////////////////////////////////////////////
 
   // SMOOTH
 
 
   public void smooth() {
     smooth(2);
   }
 
 
   public void smooth(int level) {
     smooth = true;
 
     if (maxSamples < level) {
       PGraphics.showWarning("Smooth level " + level + " is not supported by the hardware. Using " + maxSamples + " instead.");
       level = maxSamples;
     }
 
     if (antialias != level) {
       antialias = level;
       if (antialias == 1) {
         antialias = 0;
       }
       // This will trigger a surface restart next time
       // requestDraw() is called.
       pgl.initialized = false;
     }
   }
 
 
   public void noSmooth() {
     smooth = false;
 
     if (1 < antialias) {
       antialias = 0;
       // This will trigger a surface restart next time
       // requestDraw() is called.
       pgl.initialized = false;
     }
   }
 
 
   //////////////////////////////////////////////////////////////
 
   // SHAPE
 
   // public void shapeMode(int mode)
 
 
   public void shape(PShape shape, float x, float y, float z) {
     if (shape.isVisible()) { // don't do expensive matrix ops if invisible
       flush();
 
       pushMatrix();
 
       if (shapeMode == CENTER) {
         translate(x - shape.getWidth() / 2, y - shape.getHeight() / 2, z
             - shape.getDepth() / 2);
 
       } else if ((shapeMode == CORNER) || (shapeMode == CORNERS)) {
         translate(x, y, z);
       }
       shape.draw(this);
 
       popMatrix();
     }
   }
 
 
   public void shape(PShape shape, float x, float y, float z, float c, float d, float e) {
     if (shape.isVisible()) { // don't do expensive matrix ops if invisible
       flush();
 
       pushMatrix();
 
       if (shapeMode == CENTER) {
         // x, y and z are center, c, d and e refer to a diameter
         translate(x - c / 2f, y - d / 2f, z - e / 2f);
         scale(c / shape.getWidth(), d / shape.getHeight(), e / shape.getDepth());
 
       } else if (shapeMode == CORNER) {
         translate(x, y, z);
         scale(c / shape.getWidth(), d / shape.getHeight(), e / shape.getDepth());
 
       } else if (shapeMode == CORNERS) {
         // c, d, e are x2/y2/z2, make them into width/height/depth
         c -= x;
         d -= y;
         e -= z;
         // then same as above
         translate(x, y, z);
         scale(c / shape.getWidth(), d / shape.getHeight(), e / shape.getDepth());
       }
       shape.draw(this);
 
       popMatrix();
     }
   }
 
 
   //////////////////////////////////////////////////////////////
 
   // TEXT SETTINGS
 
   // public void textAlign(int align)
 
   // public void textAlign(int alignX, int alignY)
 
   // public float textAscent()
 
   // public float textDescent()
 
   // public void textFont(PFont which)
 
   // public void textFont(PFont which, float size)
 
   // public void textLeading(float leading)
 
   // public void textMode(int mode)
 
   protected boolean textModeCheck(int mode) {
     return mode == MODEL;
   }
 
   // public void textSize(float size)
 
   // public float textWidth(char c)
 
   // public float textWidth(String str)
 
   // protected float textWidthImpl(char buffer[], int start, int stop)
 
 
   //////////////////////////////////////////////////////////////
 
   // TEXT IMPL
 
 
   // protected void textLineAlignImpl(char buffer[], int start, int stop,
   // float x, float y)
 
   /**
    * Implementation of actual drawing for a line of text.
    */
   protected void textLineImpl(char buffer[], int start, int stop, float x, float y) {
     textTex = (PFontTexture)textFont.getCache(pg);
     if (textTex == null) {
       textTex = new PFontTexture(parent, textFont, maxTextureSize, maxTextureSize);
       textFont.setCache(this, textTex);
     } else {
       if (textTex.contextIsOutdated()) {
         textTex = new PFontTexture(parent, textFont, PApplet.min(PGL.MAX_FONT_TEX_SIZE, maxTextureSize),
                                                      PApplet.min(PGL.MAX_FONT_TEX_SIZE, maxTextureSize));
         textFont.setCache(this, textTex);
       }
     }
     textTex.setFirstTexture();
 
     // Saving style parameters modified by text rendering.
     int savedTextureMode = textureMode;
     boolean savedStroke = stroke;
     float savedNormalX = normalX;
     float savedNormalY = normalY;
     float savedNormalZ = normalZ;
     boolean savedTint = tint;
     int savedTintColor = tintColor;
     int savedBlendMode = blendMode;
 
     // Setting style used in text rendering.
     textureMode = NORMAL;
     stroke = false;
     normalX = 0;
     normalY = 0;
     normalZ = 1;
     tint = true;
     tintColor = fillColor;
 
     blendMode(BLEND);
 
     super.textLineImpl(buffer, start, stop, x, y);
 
     // Restoring original style.
     textureMode  = savedTextureMode;
     stroke = savedStroke;
     normalX = savedNormalX;
     normalY = savedNormalY;
     normalZ = savedNormalZ;
     tint = savedTint;
     tintColor = savedTintColor;
 
     // Note that if the user is using a blending mode different from
     // BLEND, and has a bunch of continuous text rendering, the performance
     // won't be optimal because at the end of each text() call the geometry
     // will be flushed when restoring the user's blend.
     blendMode(savedBlendMode);
   }
 
 
   protected void textCharImpl(char ch, float x, float y) {
     PFont.Glyph glyph = textFont.getGlyph(ch);
 
     if (glyph != null) {
       PFontTexture.TextureInfo tinfo = textTex.getTexInfo(glyph);
 
       if (tinfo == null) {
         // Adding new glyph to the font texture.
         tinfo = textTex.addToTexture(glyph);
       }
 
       if (textMode == MODEL) {
         float high = glyph.height / (float) textFont.getSize();
         float bwidth = glyph.width / (float) textFont.getSize();
         float lextent = glyph.leftExtent / (float) textFont.getSize();
         float textent = glyph.topExtent / (float) textFont.getSize();
 
         float x1 = x + lextent * textSize;
         float y1 = y - textent * textSize;
         float x2 = x1 + bwidth * textSize;
         float y2 = y1 + high * textSize;
 
         textCharModelImpl(tinfo, x1, y1, x2, y2);
       }
     }
   }
 
 
   protected void textCharModelImpl(PFontTexture.TextureInfo info, float x0, float y0,
                                                                   float x1, float y1) {
     if (textTex.currentTex != info.texIndex) {
       textTex.setTexture(info.texIndex);
     }
     PImage tex = textTex.getCurrentTexture();
 
     beginShape(QUADS);
     texture(tex);
     vertex(x0, y0, info.u0, info.v0);
     vertex(x1, y0, info.u1, info.v0);
     vertex(x1, y1, info.u1, info.v1);
     vertex(x0, y1, info.u0, info.v1);
     endShape();
   }
 
 
   //////////////////////////////////////////////////////////////
 
   // MATRIX STACK
 
 
   public void pushMatrix() {
     modelviewStack.push(new PMatrix3D(modelview));
     modelviewInvStack.push(new PMatrix3D(modelviewInv));
   }
 
 
   public void popMatrix() {
     if (hints[DISABLE_TRANSFORM_CACHE]) {
       flush();
     }
     PMatrix3D mat;
 
     mat = modelviewStack.pop();
     modelview.set(mat);
 
     mat = modelviewInvStack.pop();
     modelviewInv.set(mat);
 
     calcProjmodelview();
   }
 
 
   //////////////////////////////////////////////////////////////
 
   // MATRIX TRANSFORMATIONS
 
 
   public void translate(float tx, float ty) {
     translate(tx, ty, 0);
   }
 
 
   public void translate(float tx, float ty, float tz) {
     if (hints[DISABLE_TRANSFORM_CACHE]) {
       flush();
     }
 
     modelview.translate(tx, ty, tz);
     invTranslate(modelviewInv, tx, ty, tz);
     projmodelview.translate(tx, ty, tz);
   }
 
 
   static protected void invTranslate(PMatrix3D matrix, float tx, float ty, float tz) {
     matrix.preApply(1, 0, 0, -tx,
                     0, 1, 0, -ty,
                     0, 0, 1, -tz,
                     0, 0, 0, 1);
   }
 
 
   /**
    * Two dimensional rotation. Same as rotateZ (this is identical to a 3D
    * rotation along the z-axis) but included for clarity -- it'd be weird for
    * people drawing 2D graphics to be using rotateZ. And they might kick our a--
    * for the confusion.
    */
   public void rotate(float angle) {
     rotateZ(angle);
   }
 
 
   public void rotateX(float angle) {
     rotate(angle, 1, 0, 0);
   }
 
 
   public void rotateY(float angle) {
     rotate(angle, 0, 1, 0);
   }
 
 
   public void rotateZ(float angle) {
     rotate(angle, 0, 0, 1);
   }
 
 
   /**
    * Rotate around an arbitrary vector, similar to glRotate(), except that it
    * takes radians (instead of degrees).
    */
   public void rotate(float angle, float v0, float v1, float v2) {
     if (hints[DISABLE_TRANSFORM_CACHE]) {
       flush();
     }
 
     modelview.rotate(angle, v0, v1, v2);
     invRotate(modelviewInv, angle, v0, v1, v2);
     calcProjmodelview(); // Possibly cheaper than doing projmodelview.rotate()
   }
 
 
   static private void invRotate(PMatrix3D matrix, float angle, float v0, float v1, float v2) {
     float norm2 = v0 * v0 + v1 * v1 + v2 * v2;
     if (Math.abs(norm2 - 1) > EPSILON) {
       // The rotation vector is not normalized.
       float norm = PApplet.sqrt(norm2);
       v0 /= norm;
       v1 /= norm;
       v2 /= norm;
     }    
     
     float c = PApplet.cos(-angle);
     float s = PApplet.sin(-angle);
     float t = 1.0f - c;
 
     matrix.preApply((t*v0*v0) + c, (t*v0*v1) - (s*v2), (t*v0*v2) + (s*v1), 0,
                     (t*v0*v1) + (s*v2), (t*v1*v1) + c, (t*v1*v2) - (s*v0), 0,
                     (t*v0*v2) - (s*v1), (t*v1*v2) + (s*v0), (t*v2*v2) + c, 0,
                     0, 0, 0, 1);
   }
 
 
   /**
    * Same as scale(s, s, s).
    */
   public void scale(float s) {
     scale(s, s, s);
   }
 
 
   /**
    * Same as scale(sx, sy, 1).
    */
   public void scale(float sx, float sy) {
     scale(sx, sy, 1);
   }
 
 
   /**
    * Scale in three dimensions.
    */
   public void scale(float sx, float sy, float sz) {
     if (hints[DISABLE_TRANSFORM_CACHE]) {
       flush();
     }
 
     modelview.scale(sx, sy, sz);
     invScale(modelviewInv, sx, sy, sz);
     projmodelview.scale(sx, sy, sz);
   }
 
 
   static protected void invScale(PMatrix3D matrix, float x, float y, float z) {
     matrix.preApply(1/x, 0, 0, 0,  0, 1/y, 0, 0,  0, 0, 1/z, 0,  0, 0, 0, 1);
   }
 
 
   public void shearX(float angle) {
     float t = (float) Math.tan(angle);
     applyMatrix(1, t, 0, 0,
                 0, 1, 0, 0,
                 0, 0, 1, 0,
                 0, 0, 0, 1);
   }
 
 
   public void shearY(float angle) {
     float t = (float) Math.tan(angle);
     applyMatrix(1, 0, 0, 0,
                 t, 1, 0, 0,
                 0, 0, 1, 0,
                 0, 0, 0, 1);
   }
 
 
   //////////////////////////////////////////////////////////////
 
   // MATRIX MORE!
 
 
   public void resetMatrix() {
     modelview.reset();
     modelviewInv.reset();
     projmodelview.set(projection);
   }
 
 
   public void applyMatrix(PMatrix2D source) {
     applyMatrix(source.m00, source.m01, source.m02,
                 source.m10, source.m11, source.m12);
   }
 
 
   public void applyMatrix(float n00, float n01, float n02,
                           float n10, float n11, float n12) {
     applyMatrix(n00, n01, n02, 0,
                 n10, n11, n12, 0,
                 0,   0,   1,   0,
                 0,   0,   0,   1);
   }
 
 
   public void applyMatrix(PMatrix3D source) {
     applyMatrix(source.m00, source.m01, source.m02, source.m03,
                 source.m10, source.m11, source.m12, source.m13,
                 source.m20, source.m21, source.m22, source.m23,
                 source.m30, source.m31, source.m32, source.m33);
   }
 
 
   /**
    * Apply a 4x4 transformation matrix to the modelview stack.
    */
   public void applyMatrix(float n00, float n01, float n02, float n03,
                           float n10, float n11, float n12, float n13,
                           float n20, float n21, float n22, float n23,
                           float n30, float n31, float n32, float n33) {
     if (hints[DISABLE_TRANSFORM_CACHE]) {
       flush();
     }
     modelview.apply(n00, n01, n02, n03,
                     n10, n11, n12, n13,
                     n20, n21, n22, n23,
                     n30, n31, n32, n33);
     projmodelview.apply(n00, n01, n02, n03,
                         n10, n11, n12, n13,
                         n20, n21, n22, n23,
                         n30, n31, n32, n33);
   }
 
   /*
   protected void loadProjection() {
     pgl.setProjectionMode();
     loadMatrix(projection);
     pgl.setModelviewMode();
   }
 
 
   protected void loadCamera() {
     pgl.setModelviewMode();
     loadMatrix(camera);
   }
 
 
   protected void loadModelview() {
     pgl.setModelviewMode();
     loadMatrix(modelview);
   }
 
   protected void loadMatrix(PMatrix3D pMatrix) {
     modelview.set(pMatrix);
   }
   */
 
   //////////////////////////////////////////////////////////////
 
   // MATRIX GET/SET/PRINT
 
 
   public PMatrix getMatrix() {
     return modelview.get();
   }
 
 
   // public PMatrix2D getMatrix(PMatrix2D target)
 
 
   public PMatrix3D getMatrix(PMatrix3D target) {
     if (target == null) {
       target = new PMatrix3D();
     }
     target.set(modelview);
     return target;
   }
 
 
   // public void setMatrix(PMatrix source)
 
 
   public void setMatrix(PMatrix2D source) {
     resetMatrix();
     applyMatrix(source);
   }
 
 
   /**
    * Set the current transformation to the contents of the specified source.
    */
   public void setMatrix(PMatrix3D source) {
     resetMatrix();
     applyMatrix(source);
   }
 
 
   /**
    * Print the current model (or "transformation") matrix.
    */
   public void printMatrix() {
     modelview.print();
   }
 
 
   //////////////////////////////////////////////////////////////
 
   // PROJECTION
 
 
   public void pushProjection() {
     projectionStack.push(new PMatrix3D(projection));
   }
 
 
   public void popProjection() {
     PMatrix3D mat = projectionStack.pop();
     projection.set(mat);
   }
 
 
   public void applyProjection(PMatrix3D mat) {
     projection.apply(mat);
   }
 
 
   public void setProjection(PMatrix3D mat) {
     projection.set(mat);
   }
 
 
   //////////////////////////////////////////////////////////////
 
   // CAMERA
 
   /**
    * Set matrix mode to the camera matrix (instead of the current transformation
    * matrix). This means applyMatrix, resetMatrix, etc. will affect the camera.
    * <P>
    * Note that the camera matrix is *not* the perspective matrix, it contains
    * the values of the modelview matrix immediatly after the latter was
    * initialized with ortho() or camera(), or the modelview matrix as result of
    * the operations applied between beginCamera()/endCamera().
    * <P>
    * beginCamera() specifies that all coordinate transforms until endCamera()
    * should be pre-applied in inverse to the camera transform matrix. Note that
    * this is only challenging when a user specifies an arbitrary matrix with
    * applyMatrix(). Then that matrix will need to be inverted, which may not be
    * possible. But take heart, if a user is applying a non-invertible matrix to
    * the camera transform, then he is clearly up to no good, and we can wash our
    * hands of those bad intentions.
    * <P>
    * begin/endCamera clauses do not automatically reset the camera transform
    * matrix. That's because we set up a nice default camera transform in
    * setup(), and we expect it to hold through draw(). So we don't reset the
    * camera transform matrix at the top of draw(). That means that an
    * innocuous-looking clause like
    *
    * <PRE>
    * beginCamera();
    * translate(0, 0, 10);
    * endCamera();
    * </PRE>
    *
    * at the top of draw(), will result in a runaway camera that shoots
    * infinitely out of the screen over time. In order to prevent this, it is
    * necessary to call some function that does a hard reset of the camera
    * transform matrix inside of begin/endCamera. Two options are
    *
    * <PRE>
    * camera(); // sets up the nice default camera transform
    * resetMatrix(); // sets up the identity camera transform
    * </PRE>
    *
    * So to rotate a camera a constant amount, you might try
    *
    * <PRE>
    * beginCamera();
    * camera();
    * rotateY(PI / 8);
    * endCamera();
    * </PRE>
    */
   public void beginCamera() {
     if (manipulatingCamera) {
       throw new RuntimeException("beginCamera() cannot be called again "
           + "before endCamera()");
     } else {
       manipulatingCamera = true;
     }
   }
 
 
   /**
    * Record the current settings into the camera matrix, and set the matrix mode
    * back to the current transformation matrix.
    * <P>
    * Note that this will destroy any settings to scale(), translate(), or
    * whatever, because the final camera matrix will be copied (not multiplied)
    * into the modelview.
    */
   public void endCamera() {
     if (!manipulatingCamera) {
       throw new RuntimeException("Cannot call endCamera() "
           + "without first calling beginCamera()");
     }
 
     camera.set(modelview);
     cameraInv.set(modelviewInv);
 
     // all done
     manipulatingCamera = false;
   }
 
 
   /**
    * Set camera to the default settings.
    * <P>
    * Processing camera behavior:
    * <P>
    * Camera behavior can be split into two separate components, camera
    * transformation, and projection. The transformation corresponds to the
    * physical location, orientation, and scale of the camera. In a physical
    * camera metaphor, this is what can manipulated by handling the camera body
    * (with the exception of scale, which doesn't really have a physcial analog).
    * The projection corresponds to what can be changed by manipulating the lens.
    * <P>
    * We maintain separate matrices to represent the camera transform and
    * projection. An important distinction between the two is that the camera
    * transform should be invertible, where the projection matrix should not,
    * since it serves to map three dimensions to two. It is possible to bake the
    * two matrices into a single one just by multiplying them together, but it
    * isn't a good idea, since lighting, z-ordering, and z-buffering all demand a
    * true camera z coordinate after modelview and camera transforms have been
    * applied but before projection. If the camera transform and projection are
    * combined there is no way to recover a good camera-space z-coordinate from a
    * model coordinate.
    * <P>
    * Fortunately, there are no functions that manipulate both camera
    * transformation and projection.
    * <P>
    * camera() sets the camera position, orientation, and center of the scene. It
    * replaces the camera transform with a new one.
    * <P>
    * The transformation functions are the same ones used to manipulate the
    * modelview matrix (scale, translate, rotate, etc.). But they are bracketed
    * with beginCamera(), endCamera() to indicate that they should apply (in
    * inverse), to the camera transformation matrix.
    */
   public void camera() {
     camera(cameraX, cameraY, cameraZ, cameraX, cameraY, 0, 0, 1, 0);
   }
 
 
   /**
    * More flexible method for dealing with camera().
    * <P>
    * The actual call is like gluLookat. Here's the real skinny on what does
    * what:
    *
    * <PRE>
    * camera(); or
    * camera(ex, ey, ez, cx, cy, cz, ux, uy, uz);
    * </PRE>
    *
    * do not need to be called from with beginCamera();/endCamera(); That's
    * because they always apply to the camera transformation, and they always
    * totally replace it. That means that any coordinate transforms done before
    * camera(); in draw() will be wiped out. It also means that camera() always
    * operates in untransformed world coordinates. Therefore it is always
    * redundant to call resetMatrix(); before camera(); This isn't technically
    * true of gluLookat, but it's pretty much how it's used.
    * <P>
    * Now, beginCamera(); and endCamera(); are useful if you want to move the
    * camera around using transforms like translate(), etc. They will wipe out
    * any coordinate system transforms that occur before them in draw(), but they
    * will not automatically wipe out the camera transform. This means that they
    * should be at the top of draw(). It also means that the following:
    *
    * <PRE>
    * beginCamera();
    * rotateY(PI / 8);
    * endCamera();
    * </PRE>
    *
    * will result in a camera that spins without stopping. If you want to just
    * rotate a small constant amount, try this:
    *
    * <PRE>
    * beginCamera();
    * camera(); // sets up the default view
    * rotateY(PI / 8);
    * endCamera();
    * </PRE>
    *
    * That will rotate a little off of the default view. Note that this is
    * entirely equivalent to
    *
    * <PRE>
    * camera(); // sets up the default view
    * beginCamera();
    * rotateY(PI / 8);
    * endCamera();
    * </PRE>
    *
    * because camera() doesn't care whether or not it's inside a begin/end
    * clause. Basically it's safe to use camera() or camera(ex, ey, ez, cx, cy,
    * cz, ux, uy, uz) as naked calls because they do all the matrix resetting
    * automatically.
    */
   public void camera(float eyeX, float eyeY, float eyeZ,
                      float centerX, float centerY, float centerZ,
                      float upX, float upY, float upZ) {
     if (hints[DISABLE_TRANSFORM_CACHE]) {
       flush();
     }
 
     // Calculating Z vector
     float z0 = eyeX - centerX;
     float z1 = eyeY - centerY;
     float z2 = eyeZ - centerZ;
     float mag = PApplet.sqrt(z0 * z0 + z1 * z1 + z2 * z2);
     if (mag != 0) {
       z0 /= mag;
       z1 /= mag;
       z2 /= mag;
     }
     cameraEyeX = eyeX;
     cameraEyeY = eyeY;
     cameraEyeZ = eyeZ;
     cameraDepth = mag;
 
     // Calculating Y vector
     float y0 = upX;
     float y1 = upY;
     float y2 = upZ;
 
     // Computing X vector as Y cross Z
     float x0 = y1 * z2 - y2 * z1;
     float x1 = -y0 * z2 + y2 * z0;
     float x2 = y0 * z1 - y1 * z0;
 
     // Recompute Y = Z cross X
     y0 = z1 * x2 - z2 * x1;
     y1 = -z0 * x2 + z2 * x0;
     y2 = z0 * x1 - z1 * x0;
 
     // Cross product gives area of parallelogram, which is < 1.0 for
     // non-perpendicular unit-length vectors; so normalize x, y here:
     mag = PApplet.sqrt(x0 * x0 + x1 * x1 + x2 * x2);
     if (mag != 0) {
       x0 /= mag;
       x1 /= mag;
       x2 /= mag;
     }
 
     mag = PApplet.sqrt(y0 * y0 + y1 * y1 + y2 * y2);
     if (mag != 0) {
       y0 /= mag;
       y1 /= mag;
       y2 /= mag;
     }
 
     modelview.set(x0, x1, x2, 0,
                   y0, y1, y2, 0,
                   z0, z1, z2, 0,
                    0,  0,  0, 1);
 
     float tx = -eyeX;
     float ty = -eyeY;
     float tz = -eyeZ;
     modelview.translate(tx, ty, tz);
 
     modelviewInv.set(modelview);
     modelviewInv.invert();
 
     camera.set(modelview);
     cameraInv.set(modelviewInv);
 
     calcProjmodelview();
   }
 
 
   /**
    * Print the current camera matrix.
    */
   public void printCamera() {
     camera.print();
   }
 
 
   //////////////////////////////////////////////////////////////
 
   // PROJECTION
 
 
   /**
    * Calls ortho() with the proper parameters for Processing's standard
    * orthographic projection.
    */
   public void ortho() {
     ortho(0, width, 0, height, -500, 500);
   }
 
 
   /**
    * Calls ortho() with the specified size of the viewing volume along
    * the X and Z directions.
    */
   public void ortho(float left, float right,
                     float bottom, float top) {
     ortho(left, right, bottom, top, -500, 500);
   }
 
 
   /**
    * Sets orthographic projection. The left, right, bottom and top
    * values refer to the top left corner of the screen, not to the
    * center or eye of the camera. This is like this because making
    * it relative to the camera is not very intuitive if we think
    * of the perspective function, which is also independent of the
    * camera position.
    *
    */
   public void ortho(float left, float right,
                     float bottom, float top,
                     float near, float far) {
     // Flushing geometry with a different perspective configuration.
     flush();
 
     left -= width/2;
     right -= width/2;
 
     bottom -= height/2;
     top -= height/2;
 
     near += cameraDepth;
     far += cameraDepth;
 
     float x = 2.0f / (right - left);
     float y = 2.0f / (top - bottom);
     float z = -2.0f / (far - near);
 
     float tx = -(right + left) / (right - left);
     float ty = -(top + bottom) / (top - bottom);
     float tz = -(far + near) / (far - near);
 
     // The minus sign is needed to invert the Y axis.
     projection.set(x,  0, 0, tx,
                    0, -y, 0, ty,
                    0,  0, z, tz,
                    0,  0, 0,  1);
   }
 
 
   /**
    * Calls perspective() with Processing's standard coordinate projection.
    * <P>
    * Projection functions:
    * <UL>
    * <LI>frustrum()
    * <LI>ortho()
    * <LI>perspective()
    * </UL>
    * Each of these three functions completely replaces the projection matrix
    * with a new one. They can be called inside setup(), and their effects will
    * be felt inside draw(). At the top of draw(), the projection matrix is not
    * reset. Therefore the last projection function to be called always
    * dominates. On resize, the default projection is always established, which
    * has perspective.
    * <P>
    * This behavior is pretty much familiar from OpenGL, except where functions
    * replace matrices, rather than multiplying against the previous.
    * <P>
    */
   public void perspective() {
     perspective(cameraFOV, cameraAspect, cameraNear, cameraFar);
   }
 
 
   /**
    * Similar to gluPerspective(). Implementation based on Mesa's glu.c
    */
   public void perspective(float fov, float aspect, float zNear, float zFar) {
     float ymax = zNear * (float) Math.tan(fov / 2);
     float ymin = -ymax;
     float xmin = ymin * aspect;
     float xmax = ymax * aspect;
     frustum(xmin, xmax, ymin, ymax, zNear, zFar);
   }
 
 
   /**
    * Same as glFrustum(), except that it wipes out (rather than multiplies
    * against) the current perspective matrix.
    * <P>
    * Implementation based on the explanation in the OpenGL blue book.
    */
   public void frustum(float left, float right, float bottom, float top,
                       float znear, float zfar) {
     // Flushing geometry with a different perspective configuration.
     flush();
 
     float temp, temp2, temp3, temp4;
     temp = 2.0f * znear;
     temp2 = right - left;
     temp3 = top - bottom;
     temp4 = zfar - znear;
 
     // The minus sign in the temp / temp3 term is to invert the Y axis.
     projection.set(temp / temp2,              0,  (right + left) / temp2,                      0,
                               0,  -temp / temp3,  (top + bottom) / temp3,                      0,
                               0,              0, (-zfar - znear) / temp4, (-temp * zfar) / temp4,
                               0,              0,                      -1,                      1);
 
     calcProjmodelview();
   }
 
 
   /**
    * Print the current projection matrix.
    */
   public void printProjection() {
     projection.print();
   }
 
 
   //////////////////////////////////////////////////////////////
 
   // SCREEN AND MODEL COORDS
 
 
   public float screenX(float x, float y) {
     return screenX(x, y, 0);
   }
 
 
   public float screenY(float x, float y) {
     return screenY(x, y, 0);
   }
 
 
   public float screenX(float x, float y, float z) {
     float ax = modelview.m00 * x + modelview.m01 * y + modelview.m02 * z + modelview.m03;
     float ay = modelview.m10 * x + modelview.m11 * y + modelview.m12 * z + modelview.m13;
     float az = modelview.m20 * x + modelview.m21 * y + modelview.m22 * z + modelview.m23;
     float aw = modelview.m30 * x + modelview.m31 * y + modelview.m32 * z + modelview.m33;
 
     float ox = projection.m00 * ax + projection.m01 * ay + projection.m02 * az + projection.m03 * aw;
     float ow = projection.m30 * ax + projection.m31 * ay + projection.m32 * az + projection.m33 * aw;
 
     if (ow != 0) {
       ox /= ow;
     }
     float sx = width * (1 + ox) / 2.0f;
     return sx;
   }
 
 
   public float screenY(float x, float y, float z) {
     float ax = modelview.m00 * x + modelview.m01 * y + modelview.m02 * z + modelview.m03;
     float ay = modelview.m10 * x + modelview.m11 * y + modelview.m12 * z + modelview.m13;
     float az = modelview.m20 * x + modelview.m21 * y + modelview.m22 * z + modelview.m23;
     float aw = modelview.m30 * x + modelview.m31 * y + modelview.m32 * z + modelview.m33;
 
     float oy = projection.m10 * ax + projection.m11 * ay + projection.m12 * az + projection.m13 * aw;
     float ow = projection.m30 * ax + projection.m31 * ay + projection.m32 * az + projection.m33 * aw;
 
     if (ow != 0) {
       oy /= ow;
     }
     float sy = height * (1 + oy) / 2.0f;
     // Turning value upside down because of Processing's inverted Y axis.
     sy = height - sy;
     return sy;
   }
 
 
   public float screenZ(float x, float y, float z) {
     float ax = modelview.m00 * x + modelview.m01 * y + modelview.m02 * z + modelview.m03;
     float ay = modelview.m10 * x + modelview.m11 * y + modelview.m12 * z + modelview.m13;
     float az = modelview.m20 * x + modelview.m21 * y + modelview.m22 * z + modelview.m23;
     float aw = modelview.m30 * x + modelview.m31 * y + modelview.m32 * z + modelview.m33;
 
     float oz = projection.m20 * ax + projection.m21 * ay + projection.m22 * az + projection.m23 * aw;
     float ow = projection.m30 * ax + projection.m31 * ay + projection.m32 * az + projection.m33 * aw;
 
     if (ow != 0) {
       oz /= ow;
     }
     float sz = (oz + 1) / 2.0f;
     return sz;
   }
 
 
   public float modelX(float x, float y, float z) {
     float ax = modelview.m00 * x + modelview.m01 * y + modelview.m02 * z + modelview.m03;
     float ay = modelview.m10 * x + modelview.m11 * y + modelview.m12 * z + modelview.m13;
     float az = modelview.m20 * x + modelview.m21 * y + modelview.m22 * z + modelview.m23;
     float aw = modelview.m30 * x + modelview.m31 * y + modelview.m32 * z + modelview.m33;
 
     float ox = cameraInv.m00 * ax + cameraInv.m01 * ay + cameraInv.m02 * az + cameraInv.m03 * aw;
     float ow = cameraInv.m30 * ax + cameraInv.m31 * ay + cameraInv.m32 * az + cameraInv.m33 * aw;
 
     return (ow != 0) ? ox / ow : ox;
   }
 
 
   public float modelY(float x, float y, float z) {
     float ax = modelview.m00 * x + modelview.m01 * y + modelview.m02 * z + modelview.m03;
     float ay = modelview.m10 * x + modelview.m11 * y + modelview.m12 * z + modelview.m13;
     float az = modelview.m20 * x + modelview.m21 * y + modelview.m22 * z + modelview.m23;
     float aw = modelview.m30 * x + modelview.m31 * y + modelview.m32 * z + modelview.m33;
 
     float oy = cameraInv.m10 * ax + cameraInv.m11 * ay + cameraInv.m12 * az + cameraInv.m13 * aw;
     float ow = cameraInv.m30 * ax + cameraInv.m31 * ay + cameraInv.m32 * az + cameraInv.m33 * aw;
 
     return (ow != 0) ? oy / ow : oy;
   }
 
 
   public float modelZ(float x, float y, float z) {
     float ax = modelview.m00 * x + modelview.m01 * y + modelview.m02 * z + modelview.m03;
     float ay = modelview.m10 * x + modelview.m11 * y + modelview.m12 * z + modelview.m13;
     float az = modelview.m20 * x + modelview.m21 * y + modelview.m22 * z + modelview.m23;
     float aw = modelview.m30 * x + modelview.m31 * y + modelview.m32 * z + modelview.m33;
 
     float oz = cameraInv.m20 * ax + cameraInv.m21 * ay + cameraInv.m22 * az + cameraInv.m23 * aw;
     float ow = cameraInv.m30 * ax + cameraInv.m31 * ay + cameraInv.m32 * az + cameraInv.m33 * aw;
 
     return (ow != 0) ? oz / ow : oz;
   }
 
   // STYLES
 
   // public void pushStyle()
   // public void popStyle()
   // public void style(PStyle)
   // public PStyle getStyle()
   // public void getStyle(PStyle)
 
   //////////////////////////////////////////////////////////////
 
   // COLOR MODE
 
   // public void colorMode(int mode)
   // public void colorMode(int mode, float max)
   // public void colorMode(int mode, float mx, float my, float mz);
   // public void colorMode(int mode, float mx, float my, float mz, float ma);
 
   //////////////////////////////////////////////////////////////
 
   // COLOR CALC
 
   // protected void colorCalc(int rgb)
   // protected void colorCalc(int rgb, float alpha)
   // protected void colorCalc(float gray)
   // protected void colorCalc(float gray, float alpha)
   // protected void colorCalc(float x, float y, float z)
   // protected void colorCalc(float x, float y, float z, float a)
   // protected void colorCalcARGB(int argb, float alpha)
 
   //////////////////////////////////////////////////////////////
 
   // STROKE CAP/JOIN/WEIGHT
 
 
   public void strokeWeight(float weight) {
     this.strokeWeight = weight;
   }
 
 
   public void strokeJoin(int join) {
     this.strokeJoin = join;
   }
 
 
   public void strokeCap(int cap) {
     this.strokeCap = cap;
   }
 
   
   //////////////////////////////////////////////////////////////
 
   // FILL COLOR  
   
   
   protected void fillFromCalc() {
     super.fillFromCalc();
     
     if (!setAmbient) {
       // Setting the ambient color from the current fill
       // is what the old P3D did and allows to have an 
       // default ambient color when the user doesn't specify
       // it explicitly.
       ambientFromCalc();
       setAmbient = false;
     }
   }  
   
 
   //////////////////////////////////////////////////////////////
 
   // LIGHTING
 
   /**
    * Sets up an ambient and directional light using OpenGL. API taken from
    * PGraphics3D.
    *
    * <PRE>
    * The Lighting Skinny:
    * The way lighting works is complicated enough that it's worth
    * producing a document to describe it. Lighting calculations proceed
    * pretty much exactly as described in the OpenGL red book.
    * Light-affecting material properties:
    *   AMBIENT COLOR
    *   - multiplies by light's ambient component
    *   - for believability this should match diffuse color
    *   DIFFUSE COLOR
    *   - multiplies by light's diffuse component
    *   SPECULAR COLOR
    *   - multiplies by light's specular component
    *   - usually less colored than diffuse/ambient
    *   SHININESS
    *   - the concentration of specular effect
    *   - this should be set pretty high (20-50) to see really
    *     noticeable specularity
    *   EMISSIVE COLOR
    *   - constant additive color effect
    * Light types:
    *   AMBIENT
    *   - one color
    *   - no specular color
    *   - no direction
    *   - may have falloff (constant, linear, and quadratic)
    *   - may have position (which matters in non-constant falloff case)
    *   - multiplies by a material's ambient reflection
    *   DIRECTIONAL
    *   - has diffuse color
    *   - has specular color
    *   - has direction
    *   - no position
    *   - no falloff
    *   - multiplies by a material's diffuse and specular reflections
    *   POINT
    *   - has diffuse color
    *   - has specular color
    *   - has position
    *   - no direction
    *   - may have falloff (constant, linear, and quadratic)
    *   - multiplies by a material's diffuse and specular reflections
    *   SPOT
    *   - has diffuse color
    *   - has specular color
    *   - has position
    *   - has direction
    *   - has cone angle (set to half the total cone angle)
    *   - has concentration value
    *   - may have falloff (constant, linear, and quadratic)
    *   - multiplies by a material's diffuse and specular reflections
    * Normal modes:
    * All of the primitives (rect, box, sphere, etc.) have their normals
    * set nicely. During beginShape/endShape normals can be set by the user.
    *   AUTO-NORMAL
    *   - if no normal is set during the shape, we are in auto-normal mode
    *   - auto-normal calculates one normal per triangle (face-normal mode)
    *   SHAPE-NORMAL
    *   - if one normal is set during the shape, it will be used for
    *     all vertices
    *   VERTEX-NORMAL
    *   - if multiple normals are set, each normal applies to
    *     subsequent vertices
    *   - (except for the first one, which applies to previous
    *     and subsequent vertices)
    * Efficiency consequences:
    *   There is a major efficiency consequence of position-dependent
    *   lighting calculations per vertex. (See below for determining
    *   whether lighting is vertex position-dependent.) If there is no
    *   position dependency then the only factors that affect the lighting
    *   contribution per vertex are its colors and its normal.
    *   There is a major efficiency win if
    *   1) lighting is not position dependent
    *   2) we are in AUTO-NORMAL or SHAPE-NORMAL mode
    *   because then we can calculate one lighting contribution per shape
    *   (SHAPE-NORMAL) or per triangle (AUTO-NORMAL) and simply multiply it
    *   into the vertex colors. The converse is our worst-case performance when
    *   1) lighting is position dependent
    *   2) we are in AUTO-NORMAL mode
    *   because then we must calculate lighting per-face * per-vertex.
    *   Each vertex has a different lighting contribution per face in
    *   which it appears. Yuck.
    * Determining vertex position dependency:
    *   If any of the following factors are TRUE then lighting is
    *   vertex position dependent:
    *   1) Any lights uses non-constant falloff
    *   2) There are any point or spot lights
    *   3) There is a light with specular color AND there is a
    *      material with specular color
    * So worth noting is that default lighting (a no-falloff ambient
    * and a directional without specularity) is not position-dependent.
    * We should capitalize.
    * Simon Greenwold, April 2005
    * </PRE>
    */
   public void lights() {
     enableLighting();
 
     // need to make sure colorMode is RGB 255 here
     int colorModeSaved = colorMode;
     colorMode = RGB;
 
     lightFalloff(1, 0, 0);
     lightSpecular(0, 0, 0);
 
     ambientLight(colorModeX * 0.5f, colorModeY * 0.5f, colorModeZ * 0.5f);
     directionalLight(colorModeX * 0.5f, colorModeY * 0.5f, colorModeZ * 0.5f, 0, 0, -1);
 
     colorMode = colorModeSaved;
   }
 
 
   /**
    * Disables lighting.
    */
   public void noLights() {
     disableLighting();
     lightCount = 0;
   }
 
 
   /**
    * Add an ambient light based on the current color mode.
    */
   public void ambientLight(float r, float g, float b) {
     ambientLight(r, g, b, 0, 0, 0);
   }
 
 
   /**
    * Add an ambient light based on the current color mode. This version includes
    * an (x, y, z) position for situations where the falloff distance is used.
    */
   public void ambientLight(float r, float g, float b, float x, float y, float z) {
     enableLighting();
     if (lightCount == PGL.MAX_LIGHTS) {
       throw new RuntimeException("can only create " + PGL.MAX_LIGHTS + " lights");
     }
 
     lightType[lightCount] = AMBIENT;
 
     lightPosition(lightCount, x, y, z, false);
     lightNormal(lightCount, 0, 0, 0);
 
     lightAmbient(lightCount, r, g, b);
     noLightDiffuse(lightCount);
     noLightSpecular(lightCount);
     noLightSpot(lightCount);
     lightFalloff(lightCount, currentLightFalloffConstant,
                              currentLightFalloffLinear,
                              currentLightFalloffQuadratic);
 
     lightCount++;
   }
 
 
   public void directionalLight(float r, float g, float b,
                                float dx, float dy, float dz) {
     enableLighting();
     if (lightCount == PGL.MAX_LIGHTS) {
       throw new RuntimeException("can only create " + PGL.MAX_LIGHTS + " lights");
     }
 
     lightType[lightCount] = DIRECTIONAL;
 
     lightPosition(lightCount, 0, 0, 0, true);
     lightNormal(lightCount, dx, dy, dz);
 
     noLightAmbient(lightCount);
     lightDiffuse(lightCount, r, g, b);
     lightSpecular(lightCount, currentLightSpecular[0],
                               currentLightSpecular[1],
                               currentLightSpecular[2]);
     noLightSpot(lightCount);
     noLightFalloff(lightCount);
 
     lightCount++;
   }
 
 
   public void pointLight(float r, float g, float b,
                          float x, float y, float z) {
     enableLighting();
     if (lightCount == PGL.MAX_LIGHTS) {
       throw new RuntimeException("can only create " + PGL.MAX_LIGHTS + " lights");
     }
 
     lightType[lightCount] = POINT;
 
     lightPosition(lightCount, x, y, z, false);
     lightNormal(lightCount, 0, 0, 0);
 
     noLightAmbient(lightCount);
     lightDiffuse(lightCount, r, g, b);
     lightSpecular(lightCount, currentLightSpecular[0],
                               currentLightSpecular[1],
                               currentLightSpecular[2]);
     noLightSpot(lightCount);
     lightFalloff(lightCount, currentLightFalloffConstant,
                              currentLightFalloffLinear,
                              currentLightFalloffQuadratic);
 
     lightCount++;
   }
 
 
   public void spotLight(float r, float g, float b,
                         float x, float y, float z,
                         float dx, float dy, float dz,
                         float angle, float concentration) {
     enableLighting();
     if (lightCount == PGL.MAX_LIGHTS) {
       throw new RuntimeException("can only create " + PGL.MAX_LIGHTS + " lights");
     }
 
     lightType[lightCount] = SPOT;
 
     lightPosition(lightCount, x, y, z, false);
     lightNormal(lightCount, dx, dy, dz);
 
     noLightAmbient(lightCount);
     lightDiffuse(lightCount, r, g, b);
     lightSpecular(lightCount, currentLightSpecular[0],
                               currentLightSpecular[1],
                               currentLightSpecular[2]);
     lightSpot(lightCount, angle, concentration);
     lightFalloff(lightCount, currentLightFalloffConstant,
                              currentLightFalloffLinear,
                              currentLightFalloffQuadratic);
 
     lightCount++;
   }
 
 
   /**
    * Set the light falloff rates for the last light that was created. Default is
    * lightFalloff(1, 0, 0).
    */
   public void lightFalloff(float constant, float linear, float quadratic) {
     currentLightFalloffConstant = constant;
     currentLightFalloffLinear = linear;
     currentLightFalloffQuadratic = quadratic;
   }
 
 
   /**
    * Set the specular color of the last light created.
    */
   public void lightSpecular(float x, float y, float z) {
     colorCalc(x, y, z);
     currentLightSpecular[0] = calcR;
     currentLightSpecular[1] = calcG;
     currentLightSpecular[2] = calcB;
   }
 
 
   protected void enableLighting() {
     if (!lights) {
       flush(); // Flushing non-lit geometry.
       lights = true;
     }
   }
 
 
   protected void disableLighting() {
     if (lights) {
       flush(); // Flushing lit geometry.
       lights = false;
     }
   }
 
 
   protected void lightPosition(int num, float x, float y, float z, boolean dir) {
     lightPosition[4 * num + 0] = x * modelview.m00 + y * modelview.m01 + z * modelview.m02 + modelview.m03;
     lightPosition[4 * num + 1] = x * modelview.m10 + y * modelview.m11 + z * modelview.m12 + modelview.m13;
     lightPosition[4 * num + 2] = x * modelview.m20 + y * modelview.m21 + z * modelview.m22 + modelview.m23;
 
     // Used to inicate if the light is directional or not.
     lightPosition[4 * num + 3] = dir ? 1: 0;
   }
 
 
   protected void lightNormal(int num, float dx, float dy, float dz) {
     // Applying normal matrix to the light direction vector, which is the transpose of the inverse of the
     // modelview.
     float nx = dx * modelviewInv.m00 + dy * modelviewInv.m10 + dz * modelviewInv.m20;
     float ny = dx * modelviewInv.m01 + dy * modelviewInv.m11 + dz * modelviewInv.m21;
     float nz = dx * modelviewInv.m02 + dy * modelviewInv.m12 + dz * modelviewInv.m22;
 
     float invn = 1.0f / PApplet.dist(0, 0, 0, nx, ny, nz);
     lightNormal[3 * num + 0] = invn * nx;
     lightNormal[3 * num + 1] = invn * ny;
     lightNormal[3 * num + 2] = invn * nz;
   }
 
 
   protected void lightAmbient(int num, float r, float g, float b) {
     colorCalc(r, g, b);
     lightAmbient[3 * num + 0] = calcR;
     lightAmbient[3 * num + 1] = calcG;
     lightAmbient[3 * num + 2] = calcB;
   }
 
 
   protected void noLightAmbient(int num) {
     lightAmbient[3 * num + 0] = 0;
     lightAmbient[3 * num + 1] = 0;
     lightAmbient[3 * num + 2] = 0;
   }
 
 
   protected void lightDiffuse(int num, float r, float g, float b) {
     colorCalc(r, g, b);
     lightDiffuse[3 * num + 0] = calcR;
     lightDiffuse[3 * num + 1] = calcG;
     lightDiffuse[3 * num + 2] = calcB;
   }
 
 
   protected void noLightDiffuse(int num) {
     lightDiffuse[3 * num + 0] = 0;
     lightDiffuse[3 * num + 1] = 0;
     lightDiffuse[3 * num + 2] = 0;
   }
 
 
   protected void lightSpecular(int num, float r, float g, float b) {
     lightSpecular[3 * num + 0] = r;
     lightSpecular[3 * num + 1] = g;
     lightSpecular[3 * num + 2] = b;
   }
 
 
   protected void noLightSpecular(int num) {
     lightSpecular[3 * num + 0] = 0;
     lightSpecular[3 * num + 1] = 0;
     lightSpecular[3 * num + 2] = 0;
   }
 
 
   protected void lightFalloff(int num, float c0, float c1, float c2) {
     lightFalloffCoefficients[3 * num + 0] = c0;
     lightFalloffCoefficients[3 * num + 1] = c1;
     lightFalloffCoefficients[3 * num + 2] = c2;
   }
 
 
   protected void noLightFalloff(int num) {
     lightFalloffCoefficients[3 * num + 0] = 1;
     lightFalloffCoefficients[3 * num + 1] = 0;
     lightFalloffCoefficients[3 * num + 2] = 0;
   }
 
 
   protected void lightSpot(int num, float angle, float exponent) {
     lightSpotParameters[2 * num + 0] = Math.max(0, PApplet.cos(angle));
     lightSpotParameters[2 * num + 1] = exponent;
   }
 
 
   protected void noLightSpot(int num) {
     lightSpotParameters[2 * num + 0] = 0;
     lightSpotParameters[2 * num + 1] = 0;
   }
 
 
   //////////////////////////////////////////////////////////////
 
   // BACKGROUND
 
 
   protected void backgroundImpl(PImage image) {
     backgroundImpl();
     set(0, 0, image);
     if (0 < parent.frameCount) {
       clearColorBuffer = true;
     }
   }
 
 
   protected void backgroundImpl() {
     flush();
 
     pgl.glDepthMask(true);
     pgl.glClearColor(0, 0, 0, 0);
     pgl.glClear(PGL.GL_DEPTH_BUFFER_BIT);
     if (hints[DISABLE_DEPTH_MASK]) {
       pgl.glDepthMask(false);
     } else {
       pgl.glDepthMask(true);
     }
 
     pgl.glClearColor(backgroundR, backgroundG, backgroundB, backgroundA);
     pgl.glClear(PGL.GL_COLOR_BUFFER_BIT);
     if (0 < parent.frameCount) {
       clearColorBuffer = true;
     }
   }
 
 
   //////////////////////////////////////////////////////////////
 
   // COLOR MODE
 
   // colorMode() is inherited from PGraphics.
 
 
   //////////////////////////////////////////////////////////////
 
   // COLOR METHODS
 
   // public final int color(int gray)
   // public final int color(int gray, int alpha)
   // public final int color(int rgb, float alpha)
   // public final int color(int x, int y, int z)
 
   // public final float alpha(int what)
   // public final float red(int what)
   // public final float green(int what)
   // public final float blue(int what)
   // public final float hue(int what)
   // public final float saturation(int what)
   // public final float brightness(int what)
 
   // public int lerpColor(int c1, int c2, float amt)
   // static public int lerpColor(int c1, int c2, float amt, int mode)
 
   //////////////////////////////////////////////////////////////
 
   // BEGINRAW/ENDRAW
 
   // beginRaw, endRaw() both inherited.
 
   //////////////////////////////////////////////////////////////
 
   // WARNINGS and EXCEPTIONS
 
   // showWarning() and showException() available from PGraphics.
 
   /**
    * Report on anything from glError().
    * Don't use this inside glBegin/glEnd otherwise it'll
    * throw an GL_INVALID_OPERATION error.
    */
   public void report(String where) {
     if (!hints[DISABLE_OPENGL_ERROR_REPORT]) {
       int err = pgl.glGetError();
       if (err != 0) {
         String errString = pgl.glErrorString(err);
         String msg = "OpenGL error " + err + " at " + where + ": " + errString;
         PGraphics.showWarning(msg);
       }
     }
   }
 
 
   //////////////////////////////////////////////////////////////
 
   // RENDERER SUPPORT QUERIES
 
   // public boolean displayable()
 
   // public boolean dimensional() // from P3D
 
 
   /**
    * Return true if this renderer supports 2D drawing. Defaults to true.
    */
   public boolean is2D() {
     return true;
   }
 
 
   /**
    * Return true if this renderer supports 2D drawing. Defaults to false.
    */
   public boolean is3D() {
     return true;
   }
 
 
   public boolean isGL() {
     return true;
   }
 
 
   //////////////////////////////////////////////////////////////
 
   // PIMAGE METHODS
 
   // getImage
   // setCache, getCache, removeCache
   // isModified, setModified
 
 
   //////////////////////////////////////////////////////////////
 
   // LOAD/UPDATE PIXELS
 
 
   // Initializes the pixels array, copying the current contents of the
   // color buffer into it.
   public void loadPixels() {
     if (!setgetPixels) {
       // Draws any remaining geometry in case the user is still not
       // setting/getting new pixels.
       flush();
     }
 
     if ((pixels == null) || (pixels.length != width * height)) {
       pixels = new int[width * height];
       pixelBuffer = IntBuffer.wrap(pixels);
     }
 
     if (!setgetPixels) {
       beginPixelsOp(OP_READ);
       pixelBuffer.rewind();
       pgl.glReadPixels(0, 0, width, height, PGL.GL_RGBA, PGL.GL_UNSIGNED_BYTE, pixelBuffer);
       endPixelsOp();
 
       PGL.nativeToJavaARGB(pixels, width, height);
 
       if (primarySurface) {
         loadTextureImpl(POINT);
         pixelsToTexture();
       }
     }
   }
 
 
   //////////////////////////////////////////////////////////////
 
   // GET/SET PIXELS
 
 
   public int get(int x, int y) {
     loadPixels();
     setgetPixels = true;
     return super.get(x, y);
   }
 
 
   protected PImage getImpl(int x, int y, int w, int h) {
     loadPixels();
     setgetPixels = true;
     return super.getImpl(x, y, w, h);
   }
 
 
   public void set(int x, int y, int argb) {
     loadPixels();
     setgetPixels = true;
     super.set(x, y, argb);
   }
 
 
   protected void setImpl(int dx, int dy, int sx, int sy, int sw, int sh,
                          PImage src) {
     loadPixels();
     setgetPixels = true;
     super.setImpl(dx, dy, sx, sy, sw, sh, src);
   }
 
 
   //////////////////////////////////////////////////////////////
 
   // LOAD/UPDATE TEXTURE
 
 
   // Copies the contents of the color buffer into the pixels
   // array, and then the pixels array into the screen texture.
   public void loadTexture() {
     if (primarySurface) {
       loadTextureImpl(POINT);
       loadPixels();
       pixelsToTexture();
     }
   }
 
 
   // Draws wherever it is in the screen texture right now to the screen.
   public void updateTexture() {
     flush();
     beginPixelsOp(OP_WRITE);
     drawTexture();
     endPixelsOp();
   }
 
 
   protected void loadTextureImpl(int sampling) {
     if (width == 0 || height == 0) return;
     if (texture == null || texture.contextIsOutdated()) {
       PTexture.Parameters params = PTexture.newParameters(ARGB, sampling);
       texture = new PTexture(parent, width, height, params);
       texture.setFlippedY(true);
       this.setCache(pg, texture);
       this.setParams(pg, params);
     }
   }
 
 
   protected void drawTexture() {
     pgl.drawTexture(texture.glTarget, texture.glID,
                     texture.glWidth, texture.glHeight,
                     0, 0, width, height);
   }
 
 
   protected void drawTexture(int x, int y, int w, int h) {
     pgl.drawTexture(texture.glTarget, texture.glID,
                     texture.glWidth, texture.glHeight,
                     x, y, x + w, y + h);
   }
 
 
   protected void pixelsToTexture() {
     texture.set(pixels);
   }
 
 
   protected void textureToPixels() {
     texture.get(pixels);
   }
 
 
   //////////////////////////////////////////////////////////////
 
   // IMAGE CONVERSION
 
 
   static public void nativeToJavaRGB(PImage image) {
     if (image.pixels != null) {
       PGL.nativeToJavaRGB(image.pixels, image.width, image.height);
     }
   }
 
 
   static public void nativeToJavaARGB(PImage image) {
     if (image.pixels != null) {
       PGL.nativeToJavaARGB(image.pixels, image.width, image.height);
     }
   }
 
 
   static public void javaToNativeRGB(PImage image) {
     if (image.pixels != null) {
       PGL.javaToNativeRGB(image.pixels, image.width, image.height);
     }
   }
 
 
   static public void javaToNativeARGB(PImage image) {
     if (image.pixels != null) {
       PGL.javaToNativeARGB(image.pixels, image.width, image.height);
     }
   }
 
 
   //////////////////////////////////////////////////////////////
 
   // MASK
 
 
   public void mask(int alpha[]) {
     PGraphics.showMethodWarning("mask");
   }
 
 
   public void mask(PImage alpha) {
     PGraphics.showMethodWarning("mask");
   }
 
 
   //////////////////////////////////////////////////////////////
 
   // FILTER
 
 
   /**
    * This is really inefficient and not a good idea in OpenGL. Use get() and
    * set() with a smaller image area, or call the filter on an image instead,
    * and then draw that.
    */
   public void filter(int kind) {
     PImage temp = get();
     temp.filter(kind);
     set(0, 0, temp);
   }
 
 
   /**
    * This is really inefficient and not a good idea in OpenGL. Use get() and
    * set() with a smaller image area, or call the filter on an image instead,
    * and then draw that.
    */
   public void filter(int kind, float param) {
     PImage temp = get();
     temp.filter(kind, param);
     set(0, 0, temp);
   }
 
 
   //////////////////////////////////////////////////////////////
 
   /**
    * Extremely slow and not optimized, should use GL methods instead. Currently
    * calls a beginPixels() on the whole canvas, then does the copy, then it
    * calls endPixels().
    */
   // public void copy(int sx1, int sy1, int sx2, int sy2,
   // int dx1, int dy1, int dx2, int dy2)
 
   // public void copy(PImage src,
   // int sx1, int sy1, int sx2, int sy2,
   // int dx1, int dy1, int dx2, int dy2)
 
 
   //////////////////////////////////////////////////////////////
 
   // BLEND
 
   // static public int blendColor(int c1, int c2, int mode)
 
   // public void blend(PImage src,
   // int sx, int sy, int dx, int dy, int mode) {
   // set(dx, dy, PImage.blendColor(src.get(sx, sy), get(dx, dy), mode));
   // }
 
 
   /**
    * Extremely slow and not optimized, should use GL methods instead. Currently
    * calls a beginPixels() on the whole canvas, then does the copy, then it
    * calls endPixels(). Please help fix: <A
    * HREF="http://dev.processing.org/bugs/show_bug.cgi?id=941">Bug 941</A>, <A
    * HREF="http://dev.processing.org/bugs/show_bug.cgi?id=942">Bug 942</A>.
    */
   // public void blend(int sx1, int sy1, int sx2, int sy2,
   // int dx1, int dy1, int dx2, int dy2, int mode) {
   // loadPixels();
   // super.blend(sx1, sy1, sx2, sy2, dx1, dy1, dx2, dy2, mode);
   // updatePixels();
   // }
 
   // public void blend(PImage src,
   // int sx1, int sy1, int sx2, int sy2,
   // int dx1, int dy1, int dx2, int dy2, int mode) {
   // loadPixels();
   // super.blend(src, sx1, sy1, sx2, sy2, dx1, dy1, dx2, dy2, mode);
   // updatePixels();
   // }
 
 
   /**
    * Allows to set custom blend modes for the entire scene, using openGL.
    * Reference article about blending modes:
    * http://www.pegtop.net/delphi/articles/blendmodes/
    */
   public void blendMode(int mode) {
     if (blendMode != mode) {
       // Flushing any remaining geometry that uses a different blending
       // mode.
       flush();
 
       blendMode = mode;
       pgl.glEnable(PGL.GL_BLEND);
 
       if (mode == REPLACE) {
         if (blendEqSupported) {
           pgl.glBlendEquation(PGL.GL_FUNC_ADD);
         }
         pgl.glBlendFunc(PGL.GL_ONE, PGL.GL_ZERO);
       } else if (mode == BLEND) {
         if (blendEqSupported) {
           pgl.glBlendEquation(PGL.GL_FUNC_ADD);
         }
         pgl.glBlendFunc(PGL.GL_SRC_ALPHA, PGL.GL_ONE_MINUS_SRC_ALPHA);
       } else if (mode == ADD) {
         if (blendEqSupported) {
           pgl.glBlendEquation(PGL.GL_FUNC_ADD);
         }
         pgl.glBlendFunc(PGL.GL_SRC_ALPHA, PGL.GL_ONE);
       } else if (mode == SUBTRACT) {
         if (blendEqSupported) {
           pgl.glBlendEquation(PGL.GL_FUNC_ADD);
         }
         pgl.glBlendFunc(PGL.GL_ONE_MINUS_DST_COLOR, PGL.GL_ZERO);
       } else if (mode == LIGHTEST) {
         if (blendEqSupported) {
           pgl.glBlendEquation(PGL.GL_FUNC_MAX);
         } else {
           PGraphics.showWarning("This blend mode is not supported");
           return;
         }
         pgl.glBlendFunc(PGL.GL_SRC_ALPHA, PGL.GL_DST_ALPHA);
       } else if (mode == DARKEST) {
         if (blendEqSupported) {
           pgl.glBlendEquation(PGL.GL_FUNC_MIN);
         } else {
           PGraphics.showWarning("This blend mode is not supported");
           return;
         }
         pgl.glBlendFunc(PGL.GL_SRC_ALPHA, PGL.GL_DST_ALPHA);
       } else if (mode == DIFFERENCE) {
         if (blendEqSupported) {
           pgl.glBlendEquation(PGL.GL_FUNC_REVERSE_SUBTRACT);
         } else {
           PGraphics.showWarning("This blend mode is not supported");
           return;
         }
         pgl.glBlendFunc(PGL.GL_ONE, PGL.GL_ONE);
       } else if (mode == EXCLUSION) {
         if (blendEqSupported) {
           pgl.glBlendEquation(PGL.GL_FUNC_ADD);
         }
         pgl.glBlendFunc(PGL.GL_ONE_MINUS_DST_COLOR, PGL.GL_ONE_MINUS_SRC_COLOR);
       } else if (mode == MULTIPLY) {
         if (blendEqSupported) {
           pgl.glBlendEquation(PGL.GL_FUNC_ADD);
         }
         pgl.glBlendFunc(PGL.GL_DST_COLOR, PGL.GL_SRC_COLOR);
       } else if (mode == SCREEN) {
         if (blendEqSupported) {
           pgl.glBlendEquation(PGL.GL_FUNC_ADD);
         }
         pgl.glBlendFunc(PGL.GL_ONE_MINUS_DST_COLOR, PGL.GL_ONE);
       }
       // HARD_LIGHT, SOFT_LIGHT, OVERLAY, DODGE, BURN modes cannot be implemented
       // in fixed-function pipeline because they require conditional blending and
       // non-linear blending equations.
     }
   }
 
 
   protected void setDefaultBlend() {
     blendMode = BLEND;
     pgl.glEnable(PGL.GL_BLEND);
     if (blendEqSupported) {
       pgl.glBlendEquation(PGL.GL_FUNC_ADD);
     }
     pgl.glBlendFunc(PGL.GL_SRC_ALPHA, PGL.GL_ONE_MINUS_SRC_ALPHA);
   }
 
 
   //////////////////////////////////////////////////////////////
 
   // SAVE
 
   // public void save(String filename) // PImage calls loadPixels()
 
 
   //////////////////////////////////////////////////////////////
 
   // SHAPE I/O
 
 
   protected String[] getSupportedShapeFormats() {
     return new String[] { "obj" };
   }
 
 
   protected PShape loadShape(String filename, Object params) {
     return null;
     //return new PShape3D(parent, filename, (PShape3D.Parameters)params);
   }
 
 
   protected PShape createShape(int size, Object params) {
     return null;
     //return new PShape3D(parent, size, (PShape3D.Parameters)params);
   }
 
 
   //////////////////////////////////////////////////////////////
 
   // TEXTURE UTILS
 
 
   /**
    * This utility method returns the texture associated to the renderer's.
    * drawing surface, making sure is updated to reflect the current contents
    * off the screen (or offscreen drawing surface).
    */
   public PTexture getTexture() {
     loadTexture();
     return texture;
   }
 
 
   /**
    * This utility method returns the texture associated to the image.
    * creating and/or updating it if needed.
    *
    * @param img the image to have a texture metadata associated to it
    */
   public PTexture getTexture(PImage img) {
     PTexture tex = (PTexture)img.getCache(pg);
     if (tex == null) {
       tex = addTexture(img);
     } else {
       if (tex.contextIsOutdated()) {
         tex = addTexture(img);
       }
 
       if (img.isModified()) {
         if (img.width != tex.width || img.height != tex.height) {
           tex.init(img.width, img.height);
         }
         updateTexture(img, tex);
       }
 
       if (tex.hasBuffers()) {
         tex.bufferUpdate();
       }
     }
     return tex;
   }
 
 
   /**
    * This utility method creates a texture for the provided image, and adds it
    * to the metadata cache of the image.
    * @param img the image to have a texture metadata associated to it
    */
   protected PTexture addTexture(PImage img) {
     PTexture.Parameters params = (PTexture.Parameters)img.getParams(pg);
     if (params == null) {
       params = PTexture.newParameters();
       img.setParams(pg, params);
     }
     PTexture tex = new PTexture(img.parent, img.width, img.height, params);
     img.loadPixels();
     if (img.pixels != null) tex.set(img.pixels);
     img.setCache(pg, tex);
     return tex;
   }
 
 
   protected PImage wrapTexture(PTexture tex) {
     // We don't use the PImage(int width, int height, int mode) constructor to
     // avoid initializing the pixels array.
     PImage img = new PImage();
     img.parent = parent;
     img.width = tex.width;
     img.height = tex.height;
     img.format = ARGB;
     img.setCache(pg, tex);
     return img;
   }
 
 
   protected void updateTexture(PImage img, PTexture tex) {
     if (tex != null) {
       int x = img.getModifiedX1();
       int y = img.getModifiedY1();
       int w = img.getModifiedX2() - x + 1;
       int h = img.getModifiedY2() - y + 1;
       tex.set(img.pixels, x, y, w, h, img.format);
     }
     img.setModified(false);
   }
 
 
   //////////////////////////////////////////////////////////////
 
   // RESIZE
 
 
   public void resize(int wide, int high) {
     PGraphics.showMethodWarning("resize");
   }
 
 
   //////////////////////////////////////////////////////////////
 
   // INITIALIZATION ROUTINES
 
 
   protected void initPrimary() {
     if (pg != null) {
       releaseResources();
     }
 
     pgl.initPrimarySurface(antialias);
     pg = this;
   }
 
 
   protected void initOffscreen() {
     // Getting the context and capabilities from the main renderer.
     pg = (PGraphicsOpenGL)parent.g;
     pgl.initOffscreenSurface(pg.pgl);
 
     pgl.updateOffscreen(pg.pgl);
     loadTextureImpl(BILINEAR);
 
     // In case of reinitialization (for example, when the smooth level
     // is changed), we make sure that all the OpenGL resources associated
     // to the surface are released by calling delete().
     if (offscreenFramebuffer != null) {
       offscreenFramebuffer.release();
     }
     if (offscreenFramebufferMultisample != null) {
       offscreenFramebufferMultisample.release();
     }
 
     if (PGraphicsOpenGL.fboMultisampleSupported && 1 < antialias) {
       offscreenFramebufferMultisample = new PFramebuffer(parent, texture.glWidth, texture.glHeight, antialias, 0,
                                                          depthBits, stencilBits,
                                                          depthBits == 24 && stencilBits == 8 && packedDepthStencilSupported, false);
 
       offscreenFramebufferMultisample.clear();
       offscreenMultisample = true;
 
       // The offscreen framebuffer where the multisampled image is finally drawn to doesn't
       // need depth and stencil buffers since they are part of the multisampled framebuffer.
       offscreenFramebuffer = new PFramebuffer(parent, texture.glWidth, texture.glHeight, 1, 1,
                                               0, 0,
                                               false, false);
 
     } else {
       antialias = 0;
       offscreenFramebuffer = new PFramebuffer(parent, texture.glWidth, texture.glHeight, 1, 1,
                                               depthBits, stencilBits,
                                               depthBits == 24 && stencilBits == 8 && packedDepthStencilSupported, false);
       offscreenMultisample = false;
     }
 
     offscreenFramebuffer.setColorBuffer(texture);
     offscreenFramebuffer.clear();
   }
 
 
   protected void getGLParameters() {
     OPENGL_VENDOR     = pgl.glGetString(PGL.GL_VENDOR);
     OPENGL_RENDERER   = pgl.glGetString(PGL.GL_RENDERER);
     OPENGL_VERSION    = pgl.glGetString(PGL.GL_VERSION);
     OPENGL_EXTENSIONS = pgl.glGetString(PGL.GL_EXTENSIONS);
 
     npotTexSupported            = -1 < OPENGL_EXTENSIONS.indexOf("texture_non_power_of_two");
     mipmapGeneration            = -1 < OPENGL_EXTENSIONS.indexOf("generate_mipmap");
     fboMultisampleSupported     = -1 < OPENGL_EXTENSIONS.indexOf("framebuffer_multisample");
     packedDepthStencilSupported = -1 < OPENGL_EXTENSIONS.indexOf("packed_depth_stencil");
 
     try {
       pgl.glBlendEquation(PGL.GL_FUNC_ADD);
       blendEqSupported = true;
     } catch (UnsupportedOperationException e) {
       blendEqSupported = false;
     }
 
     int temp[] = new int[2];
 
     pgl.glGetIntegerv(PGL.GL_MAX_TEXTURE_SIZE, temp, 0);
     maxTextureSize = temp[0];
 
     pgl.glGetIntegerv(PGL.GL_MAX_SAMPLES, temp, 0);
     maxSamples = temp[0];
 
     pgl.glGetIntegerv(PGL.GL_ALIASED_LINE_WIDTH_RANGE, temp, 0);
     maxLineWidth = temp[1];
 
     pgl.glGetIntegerv(PGL.GL_ALIASED_POINT_SIZE_RANGE, temp, 0);
     maxPointSize = temp[1];
 
     pgl.glGetIntegerv(PGL.GL_DEPTH_BITS, temp, 0);
     depthBits = temp[0];
 
     pgl.glGetIntegerv(PGL.GL_STENCIL_BITS, temp, 0);
     stencilBits = temp[0];
 
     glParamsRead = true;
   }
 
 
   //////////////////////////////////////////////////////////////
 
   // SHADER HANDLING
 
 
   public PShader loadShader(String vertFilename, String fragFilename, int kind) {
     if (kind == FILL_SHADER_SIMPLE) {
       return new FillShaderSimple(parent, vertFilename, fragFilename);
     } else if (kind == FILL_SHADER_LIT) {
       return new FillShaderLit(parent, vertFilename, fragFilename);
     } else if (kind == FILL_SHADER_TEX) {
       return new FillShaderTex(parent, vertFilename, fragFilename);
     } else if (kind == FILL_SHADER_FULL) {
       return new FillShaderFull(parent, vertFilename, fragFilename);
     } else if (kind == LINE_SHADER) {
       return new LineShader(parent, vertFilename, fragFilename);
     } else if (kind == POINT_SHADER) {
       return new PointShader(parent, vertFilename, fragFilename);
     } else {
       PGraphics.showWarning("Wrong shader type");
       return null;
     }
   }
 
 
   public PShader loadShader(String fragFilename, int kind) {
     PShader shader;
     if (kind == FILL_SHADER_SIMPLE) {
       shader = new FillShaderSimple(parent);
       shader.setVertexShader(defFillShaderVertSimpleURL);
     } else if (kind == FILL_SHADER_LIT) {
       shader = new FillShaderLit(parent);
       shader.setVertexShader(defFillShaderVertLitURL);
     } else if (kind == FILL_SHADER_TEX) {
       shader = new FillShaderTex(parent);
       shader.setVertexShader(defFillShaderVertTexURL);
     } else if (kind == FILL_SHADER_FULL) {
       shader = new FillShaderFull(parent);
       shader.setVertexShader(defFillShaderVertFullURL);
     } else if (kind == LINE_SHADER) {
       shader = new LineShader(parent);
       shader.setVertexShader(defLineShaderVertURL);
     } else if (kind == POINT_SHADER) {
       shader = new PointShader(parent);
       shader.setVertexShader(defPointShaderVertURL);
     } else {
       PGraphics.showWarning("Wrong shader type");
       return null;
     }
     shader.setFragmentShader(fragFilename);
     return shader;
   }
 
 
   public void setShader(PShader shader, int kind) {
     if (kind == FILL_SHADER_SIMPLE) {
       fillShaderSimple = (FillShaderSimple) shader;
     } else if (kind == FILL_SHADER_LIT) {
       fillShaderLit = (FillShaderLit) shader;
     } else if (kind == FILL_SHADER_TEX) {
       fillShaderTex = (FillShaderTex) shader;
     } else if (kind == FILL_SHADER_FULL) {
       fillShaderFull = (FillShaderFull) shader;
     } else if (kind == LINE_SHADER) {
       lineShader = (LineShader) shader;
     } else if (kind == POINT_SHADER) {
       pointShader = (PointShader) shader;
     } else {
       PGraphics.showWarning("Wrong shader type");
     }
   }
 
 
   public void resetShader(int kind) {
     if (kind == FILL_SHADER_SIMPLE) {
       if (defFillShaderSimple == null) {
         defFillShaderSimple = new FillShaderSimple(parent, defFillShaderVertSimpleURL, defFillShaderFragNoTexURL);
       }
       fillShaderSimple = defFillShaderSimple;
     } else if (kind == FILL_SHADER_LIT) {
       if (defFillShaderLit == null) {
         defFillShaderLit = new FillShaderLit(parent, defFillShaderVertLitURL, defFillShaderFragNoTexURL);
       }
       fillShaderLit = defFillShaderLit;
     } else if (kind == FILL_SHADER_TEX) {
       if (defFillShaderTex == null) {
         defFillShaderTex = new FillShaderTex(parent, defFillShaderVertTexURL, defFillShaderFragTexURL);
       }
       fillShaderTex = defFillShaderTex;
     } else if (kind == FILL_SHADER_FULL) {
       if (defFillShaderFull == null) {
         defFillShaderFull = new FillShaderFull(parent, defFillShaderVertFullURL, defFillShaderFragTexURL);
       }
       fillShaderFull = defFillShaderFull;
     } else if (kind == LINE_SHADER) {
       if (defLineShader == null) {
         defLineShader = new LineShader(parent, defLineShaderVertURL, defLineShaderFragURL);
       }
       lineShader = defLineShader;
     } else if (kind == POINT_SHADER) {
       if (defPointShader == null) {
         defPointShader = new PointShader(parent, defPointShaderVertURL, defPointShaderFragURL);
       }
       pointShader = defPointShader;
     } else {
       PGraphics.showWarning("Wrong shader type");
     }
   }
 
 
   protected FillShader getFillShader(boolean lit, boolean tex) {
     FillShader shader;
     if (lit) {
       if (tex) {
         if (defFillShaderFull == null) {
           defFillShaderFull = new FillShaderFull(parent, defFillShaderVertFullURL, defFillShaderFragTexURL);
         }
         if (fillShaderFull == null) {
           fillShaderFull = defFillShaderFull;
         }
         shader = fillShaderFull;
       } else {
         if (defFillShaderLit == null) {
           defFillShaderLit = new FillShaderLit(parent, defFillShaderVertLitURL, defFillShaderFragNoTexURL);
         }
         if (fillShaderLit == null) {
           fillShaderLit = defFillShaderLit;
         }
         shader = fillShaderLit;
       }
     } else {
       if (tex) {
         if (defFillShaderTex == null) {
           defFillShaderTex = new FillShaderTex(parent, defFillShaderVertTexURL, defFillShaderFragTexURL);
         }
         if (fillShaderTex == null) {
           fillShaderTex = defFillShaderTex;
         }
         shader = fillShaderTex;
       } else {
         if (defFillShaderSimple == null) {
           defFillShaderSimple = new FillShaderSimple(parent, defFillShaderVertSimpleURL, defFillShaderFragNoTexURL);
         }
         if (fillShaderSimple == null) {
           fillShaderSimple = defFillShaderSimple;
         }
         shader = fillShaderSimple;
       }
     }
     shader.setRenderer(this);
     shader.loadAttributes();
     shader.loadUniforms();
     return shader;
   }
 
 
   protected LineShader getLineShader() {
     if (defLineShader == null) {
       defLineShader = new LineShader(parent, defLineShaderVertURL, defLineShaderFragURL);
     }
     if (lineShader == null) {
       lineShader = defLineShader;
     }
     lineShader.setRenderer(this);
     lineShader.loadAttributes();
     lineShader.loadUniforms();
     return lineShader;
   }
 
 
   protected PointShader getPointShader() {
     if (defPointShader == null) {
       defPointShader = new PointShader(parent, defPointShaderVertURL, defPointShaderFragURL);
     }
     if (pointShader == null) {
       pointShader = defPointShader;
     }
     pointShader.setRenderer(this);
     pointShader.loadAttributes();
     pointShader.loadUniforms();
     return pointShader;
   }
 
 
   protected class FillShader extends PShader {
     // We need a reference to the renderer since a shader might
     // be called by different renderers within a single application
     // (the one corresponding to the main surface, or other offscreen
     // renderers).
     protected PGraphicsOpenGL renderer;
 
     public FillShader(PApplet parent) {
       super(parent);
     }
 
     public FillShader(PApplet parent, String vertFilename, String fragFilename) {
       super(parent, vertFilename, fragFilename);
     }
 
     public FillShader(PApplet parent, URL vertURL, URL fragURL) {
       super(parent, vertURL, fragURL);
     }
 
     public void setRenderer(PGraphicsOpenGL pg) {
       this.renderer = pg;
     }
 
     public void loadAttributes() { }
     public void loadUniforms() { }
 
     public void setAttribute(int loc, int vboId, int size, int type, boolean normalized, int stride, int offset) {
       if (-1 < loc) {
         pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, vboId);
         pgl.glVertexAttribPointer(loc, size, type, normalized, stride, offset);
       }
     }
 
     public void setVertexAttribute(int vboId, int size, int type, int stride, int offset) { }
     public void setColorAttribute(int vboId, int size, int type, int stride, int offset) { }
     public void setNormalAttribute(int vboId, int size, int type, int stride, int offset) { }
     public void setAmbientAttribute(int vboId, int size, int type, int stride, int offset) { }
     public void setSpecularAttribute(int vboId, int size, int type, int stride, int offset) { }
     public void setEmissiveAttribute(int vboId, int size, int type, int stride, int offset) { }
     public void setShininessAttribute(int vboId, int size, int type, int stride, int offset) { }
     public void setTexCoordAttribute(int vboId, int size, int type, int stride, int offset) { }
     public void setTexture(PTexture tex) { }
   }
 
 
   protected class FillShaderSimple extends FillShader {
     protected int projmodelviewMatrixLoc;
 
     protected int inVertexLoc;
     protected int inColorLoc;
 
     public FillShaderSimple(PApplet parent) {
       super(parent);
     }
 
     public FillShaderSimple(PApplet parent, String vertFilename, String fragFilename) {
       super(parent, vertFilename, fragFilename);
     }
 
     public FillShaderSimple(PApplet parent, URL vertURL, URL fragURL) {
       super(parent, vertURL, fragURL);
     }
 
     public void loadAttributes() {
       inVertexLoc = getAttribLocation("inVertex");
       inColorLoc = getAttribLocation("inColor");
     }
 
     public void loadUniforms() {
       projmodelviewMatrixLoc = getUniformLocation("projmodelviewMatrix");
     }
 
     public void setVertexAttribute(int vboId, int size, int type, int stride, int offset) {
       setAttribute(inVertexLoc, vboId, size, type, false, stride, offset);
     }
 
     public void setColorAttribute(int vboId, int size, int type, int stride, int offset) {
       setAttribute(inColorLoc, vboId, size, type, true, stride, offset);
     }
 
     public void start() {
       super.start();
 
       if (-1 < inVertexLoc) pgl.glEnableVertexAttribArray(inVertexLoc);
       if (-1 < inColorLoc)  pgl.glEnableVertexAttribArray(inColorLoc);
 
       if (renderer != null) {
         renderer.updateGLProjmodelview();
         set4x4MatUniform(projmodelviewMatrixLoc, renderer.glProjmodelview);
       }
     }
 
     public void stop() {
       if (-1 < inVertexLoc) pgl.glDisableVertexAttribArray(inVertexLoc);
       if (-1 < inColorLoc)  pgl.glDisableVertexAttribArray(inColorLoc);
 
       pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
 
       super.stop();
     }
   }
 
 
   protected class FillShaderLit extends FillShader {
     protected int projmodelviewMatrixLoc;
     protected int modelviewMatrixLoc;
     protected int normalMatrixLoc;
 
     protected int lightCountLoc;
     protected int lightPositionLoc;
     protected int lightNormalLoc;
     protected int lightAmbientLoc;
     protected int lightDiffuseLoc;
     protected int lightSpecularLoc;
     protected int lightFalloffCoefficientsLoc;
     protected int lightSpotParametersLoc;
 
     protected int inVertexLoc;
     protected int inColorLoc;
     protected int inNormalLoc;
 
     protected int inAmbientLoc;
     protected int inSpecularLoc;
     protected int inEmissiveLoc;
     protected int inShineLoc;
 
     public FillShaderLit(PApplet parent) {
       super(parent);
     }
 
     public FillShaderLit(PApplet parent, String vertFilename, String fragFilename) {
       super(parent, vertFilename, fragFilename);
     }
 
     public FillShaderLit(PApplet parent, URL vertURL, URL fragURL) {
       super(parent, vertURL, fragURL);
     }
 
     public void loadAttributes() {
       inVertexLoc = getAttribLocation("inVertex");
       inColorLoc = getAttribLocation("inColor");
       inNormalLoc = getAttribLocation("inNormal");
 
       inAmbientLoc = getAttribLocation("inAmbient");
       inSpecularLoc = getAttribLocation("inSpecular");
       inEmissiveLoc = getAttribLocation("inEmissive");
       inShineLoc = getAttribLocation("inShine");
     }
 
     public void loadUniforms() {
       projmodelviewMatrixLoc = getUniformLocation("projmodelviewMatrix");
       modelviewMatrixLoc = getUniformLocation("modelviewMatrix");
       normalMatrixLoc = getUniformLocation("normalMatrix");
 
       lightCountLoc = getUniformLocation("lightCount");
       lightPositionLoc = getUniformLocation("lightPosition");
       lightNormalLoc = getUniformLocation("lightNormal");
       lightAmbientLoc = getUniformLocation("lightAmbient");
       lightDiffuseLoc = getUniformLocation("lightDiffuse");
       lightSpecularLoc = getUniformLocation("lightSpecular");
       lightFalloffCoefficientsLoc = getUniformLocation("lightFalloffCoefficients");
       lightSpotParametersLoc = getUniformLocation("lightSpotParameters");
     }
 
     public void setVertexAttribute(int vboId, int size, int type, int stride, int offset) {
       setAttribute(inVertexLoc, vboId, size, type, false, stride, offset);
     }
 
     public void setColorAttribute(int vboId, int size, int type, int stride, int offset) {
       setAttribute(inColorLoc, vboId, size, type, true, stride, offset);
     }
 
     public void setNormalAttribute(int vboId, int size, int type, int stride, int offset) {
       setAttribute(inNormalLoc, vboId, size, type, false, stride, offset);
     }
 
     public void setAmbientAttribute(int vboId, int size, int type, int stride, int offset) {
       setAttribute(inAmbientLoc, vboId, size, type, true, stride, offset);
     }
 
     public void setSpecularAttribute(int vboId, int size, int type, int stride, int offset) {
       setAttribute(inSpecularLoc, vboId, size, type, true, stride, offset);
     }
 
     public void setEmissiveAttribute(int vboId, int size, int type, int stride, int offset) {
       setAttribute(inEmissiveLoc, vboId, size, type, true, stride, offset);
     }
 
     public void setShininessAttribute(int vboId, int size, int type, int stride, int offset) {
       setAttribute(inShineLoc, vboId, size, type, false, stride, offset);
     }
 
     public void start() {
       super.start();
 
       if (-1 < inVertexLoc) pgl.glEnableVertexAttribArray(inVertexLoc);
       if (-1 < inColorLoc)  pgl.glEnableVertexAttribArray(inColorLoc);
       if (-1 < inNormalLoc) pgl.glEnableVertexAttribArray(inNormalLoc);
 
       if (-1 < inAmbientLoc)  pgl.glEnableVertexAttribArray(inAmbientLoc);
       if (-1 < inSpecularLoc) pgl.glEnableVertexAttribArray(inSpecularLoc);
       if (-1 < inEmissiveLoc) pgl.glEnableVertexAttribArray(inEmissiveLoc);
       if (-1 < inShineLoc)    pgl.glEnableVertexAttribArray(inShineLoc);
 
       if (renderer != null) {
         renderer.updateGLProjmodelview();
         set4x4MatUniform(projmodelviewMatrixLoc, renderer.glProjmodelview);
 
         renderer.updateGLModelview();
         set4x4MatUniform(modelviewMatrixLoc, renderer.glModelview);
 
         renderer.updateGLNormal();
         set3x3MatUniform(normalMatrixLoc, renderer.glNormal);
 
         setIntUniform(lightCountLoc, renderer.lightCount);
         set4FloatVecUniform(lightPositionLoc, renderer.lightPosition);
         set3FloatVecUniform(lightNormalLoc, renderer.lightNormal);
         set3FloatVecUniform(lightAmbientLoc, renderer.lightAmbient);
         set3FloatVecUniform(lightDiffuseLoc, renderer.lightDiffuse);
         set3FloatVecUniform(lightSpecularLoc, renderer.lightSpecular);
         set3FloatVecUniform(lightFalloffCoefficientsLoc, renderer.lightFalloffCoefficients);
         set2FloatVecUniform(lightSpotParametersLoc, renderer.lightSpotParameters);
       }
     }
 
     public void stop() {
       if (-1 < inVertexLoc) pgl.glDisableVertexAttribArray(inVertexLoc);
       if (-1 < inColorLoc)  pgl.glDisableVertexAttribArray(inColorLoc);
       if (-1 < inNormalLoc) pgl.glDisableVertexAttribArray(inNormalLoc);
 
       if (-1 < inAmbientLoc)  pgl.glDisableVertexAttribArray(inAmbientLoc);
       if (-1 < inSpecularLoc) pgl.glDisableVertexAttribArray(inSpecularLoc);
       if (-1 < inEmissiveLoc) pgl.glDisableVertexAttribArray(inEmissiveLoc);
       if (-1 < inShineLoc)    pgl.glDisableVertexAttribArray(inShineLoc);
 
       pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
 
       super.stop();
     }
   }
 
 
   protected class FillShaderTex extends FillShaderSimple {
     protected int inTexcoordLoc;
 
     protected int texcoordMatrixLoc;
     protected int texcoordOffsetLoc;
 
     protected float[] tcmat;
 
     public FillShaderTex(PApplet parent) {
       super(parent);
     }
 
     public FillShaderTex(PApplet parent, String vertFilename, String fragFilename) {
       super(parent, vertFilename, fragFilename);
     }
 
     public FillShaderTex(PApplet parent, URL vertURL, URL fragURL) {
       super(parent, vertURL, fragURL);
     }
 
     public void loadUniforms() {
       super.loadUniforms();
 
       texcoordMatrixLoc = getUniformLocation("texcoordMatrix");
       texcoordOffsetLoc = getUniformLocation("texcoordOffset");
     }
 
     public void loadAttributes() {
       super.loadAttributes();
 
       inTexcoordLoc = getAttribLocation("inTexcoord");
     }
 
     public void setTexCoordAttribute(int vboId, int size, int type, int stride, int offset) {
       setAttribute(inTexcoordLoc, vboId, size, type, false, stride, offset);
     }
 
     public void setTexture(PTexture tex) {
       float scaleu = 1;
       float scalev = 1;
       float dispu = 0;
       float dispv = 0;
 
       if (tex.isFlippedX()) {
         scaleu = -1;
         dispu = 1;
       }
 
       if (tex.isFlippedY()) {
         scalev = -1;
         dispv = 1;
       }
 
       scaleu *= tex.maxTexCoordU;
       dispu *= tex.maxTexCoordU;
       scalev *= tex.maxTexCoordV;
       dispv *= tex.maxTexCoordV;
 
       if (tcmat == null) {
         tcmat = new float[16];
       }
 
       tcmat[0] = scaleu; tcmat[4] = 0;      tcmat[ 8] = 0; tcmat[12] = dispu;
       tcmat[1] = 0;      tcmat[5] = scalev; tcmat[ 9] = 0; tcmat[13] = dispv;
       tcmat[2] = 0;      tcmat[6] = 0;      tcmat[10] = 0; tcmat[14] = 0;
       tcmat[3] = 0;      tcmat[7] = 0;      tcmat[11] = 0; tcmat[15] = 0;
       set4x4MatUniform(texcoordMatrixLoc, tcmat);
 
       set2FloatUniform(texcoordOffsetLoc, 1.0f / tex.width, 1.0f / tex.height);
     }
 
     public void start() {
       super.start();
 
       if (-1 < inTexcoordLoc) pgl.glEnableVertexAttribArray(inTexcoordLoc);
     }
 
     public void stop() {
       if (-1 < inTexcoordLoc) pgl.glDisableVertexAttribArray(inTexcoordLoc);
 
       super.stop();
     }
   }
 
 
   protected class FillShaderFull extends FillShaderLit {
     protected int inTexcoordLoc;
 
     protected int texcoordMatrixLoc;
     protected int texcoordOffsetLoc;
 
     protected float[] tcmat;
 
     public FillShaderFull(PApplet parent) {
       super(parent);
     }
 
     public FillShaderFull(PApplet parent, String vertFilename, String fragFilename) {
       super(parent, vertFilename, fragFilename);
     }
 
     public FillShaderFull(PApplet parent, URL vertURL, URL fragURL) {
       super(parent, vertURL, fragURL);
     }
 
     public void loadUniforms() {
       super.loadUniforms();
 
       texcoordMatrixLoc = getUniformLocation("texcoordMatrix");
       texcoordOffsetLoc = getUniformLocation("texcoordOffset");
     }
 
     public void loadAttributes() {
       super.loadAttributes();
 
       inTexcoordLoc = getAttribLocation("inTexcoord");
     }
 
     public void setTexCoordAttribute(int vboId, int size, int type, int stride, int offset) {
       setAttribute(inTexcoordLoc, vboId, size, type, false, stride, offset);
     }
 
     public void setTexture(PTexture tex) {
       float scaleu = 1;
       float scalev = 1;
       float dispu = 0;
       float dispv = 0;
 
       if (tex.isFlippedX()) {
         scaleu = -1;
         dispu = 1;
       }
 
       if (tex.isFlippedY()) {
         scalev = -1;
         dispv = 1;
       }
 
       scaleu *= tex.maxTexCoordU;
       dispu *= tex.maxTexCoordU;
       scalev *= tex.maxTexCoordV;
       dispv *= tex.maxTexCoordV;
 
       if (tcmat == null) {
         tcmat = new float[16];
       }
 
       tcmat[0] = scaleu; tcmat[4] = 0;      tcmat[ 8] = 0; tcmat[12] = dispu;
       tcmat[1] = 0;      tcmat[5] = scalev; tcmat[ 9] = 0; tcmat[13] = dispv;
       tcmat[2] = 0;      tcmat[6] = 0;      tcmat[10] = 0; tcmat[14] = 0;
       tcmat[3] = 0;      tcmat[7] = 0;      tcmat[11] = 0; tcmat[15] = 0;
       set4x4MatUniform(texcoordMatrixLoc, tcmat);
 
       set2FloatUniform(texcoordOffsetLoc, 1.0f / tex.width, 1.0f / tex.height);
     }
 
     public void start() {
       super.start();
 
       if (-1 < inTexcoordLoc) pgl.glEnableVertexAttribArray(inTexcoordLoc);
     }
 
     public void stop() {
       if (-1 < inTexcoordLoc) pgl.glDisableVertexAttribArray(inTexcoordLoc);
 
       super.stop();
     }
   }
 
 
   protected class LineShader extends PShader {
     protected PGraphicsOpenGL renderer;
 
     protected int projectionMatrixLoc;
     protected int modelviewMatrixLoc;
 
     protected int viewportLoc;
     protected int perspectiveLoc;
 
     protected int inVertexLoc;
     protected int inColorLoc;
     protected int inDirWidthLoc;
 
     public LineShader(PApplet parent) {
       super(parent);
     }
 
     public LineShader(PApplet parent, String vertFilename, String fragFilename) {
       super(parent, vertFilename, fragFilename);
     }
 
     public LineShader(PApplet parent, URL vertURL, URL fragURL) {
       super(parent, vertURL, fragURL);
     }
 
     public void setRenderer(PGraphicsOpenGL pg) {
       this.renderer = pg;
     }
 
     public void loadAttributes() {
       inVertexLoc = getAttribLocation("inVertex");
       inColorLoc = getAttribLocation("inColor");
       inDirWidthLoc = getAttribLocation("inDirWidth");
     }
 
     public void loadUniforms() {
       projectionMatrixLoc = getUniformLocation("projectionMatrix");
       modelviewMatrixLoc = getUniformLocation("modelviewMatrix");
 
       viewportLoc = getUniformLocation("viewport");
       perspectiveLoc = getUniformLocation("perspective");
     }
 
     public void setAttribute(int loc, int vboId, int size, int type, boolean normalized, int stride, int offset) {
       pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, vboId);
       pgl.glVertexAttribPointer(loc, size, type, normalized, stride, offset);
     }
 
     public void setVertexAttribute(int vboId, int size, int type, int stride, int offset) {
       setAttribute(inVertexLoc, vboId, size, type, false, stride, offset);
     }
 
     public void setColorAttribute(int vboId, int size, int type, int stride, int offset) {
       setAttribute(inColorLoc, vboId, size, type, true, stride, offset);
     }
 
     public void setDirWidthAttribute(int vboId, int size, int type, int stride, int offset) {
       setAttribute(inDirWidthLoc, vboId, size, type, false, stride, offset);
     }
 
     public void start() {
       super.start();
 
       if (-1 < inVertexLoc)   pgl.glEnableVertexAttribArray(inVertexLoc);
       if (-1 < inColorLoc)    pgl.glEnableVertexAttribArray(inColorLoc);
       if (-1 < inDirWidthLoc) pgl.glEnableVertexAttribArray(inDirWidthLoc);
 
       if (renderer != null) {
         renderer.updateGLProjection();
         set4x4MatUniform(projectionMatrixLoc, renderer.glProjection);
 
         renderer.updateGLModelview();
         set4x4MatUniform(modelviewMatrixLoc, renderer.glModelview);
 
         set4FloatUniform(viewportLoc, renderer.viewport[0], renderer.viewport[1], renderer.viewport[2], renderer.viewport[3]);
       }
 
       setIntUniform(perspectiveLoc, perspectiveCorrectedLines ? 1 : 0);
     }
 
     public void stop() {
       if (-1 < inVertexLoc)   pgl.glDisableVertexAttribArray(inVertexLoc);
       if (-1 < inColorLoc)    pgl.glDisableVertexAttribArray(inColorLoc);
       if (-1 < inDirWidthLoc) pgl.glDisableVertexAttribArray(inDirWidthLoc);
 
       pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
 
       super.stop();
     }
   }
 
 
   protected class PointShader extends PShader {
     protected PGraphicsOpenGL renderer;
 
     protected int projectionMatrixLoc;
     protected int modelviewMatrixLoc;
 
     protected int inVertexLoc;
     protected int inColorLoc;
     protected int inSizeLoc;
 
     public PointShader(PApplet parent) {
       super(parent);
     }
 
     public PointShader(PApplet parent, String vertFilename, String fragFilename) {
       super(parent, vertFilename, fragFilename);
     }
 
     public PointShader(PApplet parent, URL vertURL, URL fragURL) {
       super(parent, vertURL, fragURL);
     }
 
     public void setRenderer(PGraphicsOpenGL pg) {
       this.renderer = pg;
     }
 
     public void loadAttributes() {
       inVertexLoc = getAttribLocation("inVertex");
       inColorLoc = getAttribLocation("inColor");
       inSizeLoc = getAttribLocation("inSize");
     }
 
     public void loadUniforms() {
       projectionMatrixLoc = getUniformLocation("projectionMatrix");
       modelviewMatrixLoc = getUniformLocation("modelviewMatrix");
     }
 
     public void setAttribute(int loc, int vboId, int size, int type, boolean normalized, int stride, int offset) {
       pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, vboId);
       pgl.glVertexAttribPointer(loc, size, type, normalized, stride, offset);
     }
 
     public void setVertexAttribute(int vboId, int size, int type, int stride, int offset) {
       setAttribute(inVertexLoc, vboId, size, type, false, stride, offset);
     }
 
     public void setColorAttribute(int vboId, int size, int type, int stride, int offset) {
       setAttribute(inColorLoc, vboId, size, type, true, stride, offset);
     }
 
     public void setSizeAttribute(int vboId, int size, int type, int stride, int offset) {
       setAttribute(inSizeLoc, vboId, size, type, false, stride, offset);
     }
 
     public void start() {
       super.start();
 
       if (-1 < inVertexLoc) pgl.glEnableVertexAttribArray(inVertexLoc);
       if (-1 < inColorLoc)  pgl.glEnableVertexAttribArray(inColorLoc);
       if (-1 < inSizeLoc)   pgl.glEnableVertexAttribArray(inSizeLoc);
 
       if (renderer != null) {
         renderer.updateGLProjection();
         set4x4MatUniform(projectionMatrixLoc, renderer.glProjection);
 
         renderer.updateGLModelview();
         set4x4MatUniform(modelviewMatrixLoc, renderer.glModelview);
       }
     }
 
     public void stop() {
       if (-1 < inVertexLoc) pgl.glDisableVertexAttribArray(inVertexLoc);
       if (-1 < inColorLoc)  pgl.glDisableVertexAttribArray(inColorLoc);
       if (-1 < inSizeLoc)   pgl.glDisableVertexAttribArray(inSizeLoc);
 
       pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
 
       super.stop();
     }
   }
 
 
 
   //////////////////////////////////////////////////////////////
 
   // Input (raw) and Tessellated geometry, tessellator.
 
 
   public InGeometry newInGeometry(int mode) {
     return new InGeometry(mode);
   }
 
 
   protected TessGeometry newTessGeometry(int mode) {
     return new TessGeometry(mode);
   }
 
 
   protected TessGeometry newTessGeometry(int mode, boolean empty) {
     return new TessGeometry(mode, empty);
   }
   
   
   protected TexCache newTexCache() {
     return new TexCache();
   }
 
 
   // Holds an array of textures and the range of vertex
   // indices each texture applies to.
   public class TexCache {
     protected int count;
     protected PImage[] textures;
     protected int[] firstIndex;
     protected int[] lastIndex;
     protected boolean hasTexture;
     protected PTexture tex0;
 
     public TexCache() {
       allocate();
     }
 
     public void allocate() {
       textures = new PImage[PGL.DEFAULT_IN_TEXTURES];
       firstIndex = new int[PGL.DEFAULT_IN_TEXTURES];
       lastIndex = new int[PGL.DEFAULT_IN_TEXTURES];
       count = 0;
       hasTexture = false;
     }
 
     public void clear() {
       java.util.Arrays.fill(textures, 0, count, null);
       count = 0;
       hasTexture = false;
     }
 
     public void dispose() {
       textures = null;
       firstIndex = null;
       lastIndex = null;
     }
 
     public void beginRender() {
       tex0 = null;
     }
 
     public PTexture getTexture(int i) {
       PImage img = textures[i];
       PTexture tex = null;
 
       if (img != null) {
         tex = pg.getTexture(img);
         if (tex != null) {
           tex.bind();
           tex0 = tex;
         }
       }
       if (tex == null && tex0 != null) {
         tex0.unbind();
         pgl.disableTexturing(tex0.glTarget);
       }
 
       return tex;
     }
 
     public void endRender() {
       if (hasTexture) {
         // Unbinding all the textures in the cache.
         for (int i = 0; i < count; i++) {
           PImage img = textures[i];
           if (img != null) {
             PTexture tex = pg.getTexture(img);
             if (tex != null) {
               tex.unbind();
             }
           }
         }
         // Disabling texturing for each of the targets used
         // by textures in the cache.
         for (int i = 0; i < count; i++) {
           PImage img = textures[i];
           if (img != null) {
             PTexture tex = pg.getTexture(img);
             if (tex != null) {
               pgl.disableTexturing(tex.glTarget);
             }
           }
         }
       }
     }
 
     public void addTexture(PImage img, int first, int last) {
       textureCheck();
 
       textures[count] = img;
       firstIndex[count] = first;
       lastIndex[count] = last;
 
       // At least one non-null texture since last reset.
       hasTexture |= img != null;
 
       count++;
     }
 
     public void setLastIndex(int last) {
       lastIndex[count - 1] = last;
     }
 
     public void textureCheck() {
       if (count == textures.length) {
         int newSize = count << 1;
 
         expandTextures(newSize);
         expandFirstIndex(newSize);
         expandLastIndex(newSize);
       }
     }
 
     public void expandTextures(int n) {
       PImage[] temp = new PImage[n];
       PApplet.arrayCopy(textures, 0, temp, 0, count);
       textures = temp;
     }
 
     public void expandFirstIndex(int n) {
       int[] temp = new int[n];
       PApplet.arrayCopy(firstIndex, 0, temp, 0, count);
       firstIndex = temp;
     }
 
     public void expandLastIndex(int n) {
       int[] temp = new int[n];
       PApplet.arrayCopy(lastIndex, 0, temp, 0, count);
       lastIndex = temp;
     }
   }
 
   // Holds the input vertices: xyz coordinates, fill/tint color,
   // normal, texture coordinates and stroke color and weight.
   public class InGeometry {
     int renderMode;
     public int vertexCount;
     public int edgeCount;
 
     // Range of vertices that will be processed by the
     // tessellator. They can be used in combination with the
     // edges array to have the tessellator using only a specific
     // range of vertices to generate fill geometry, while the
     // line geometry will be read from the edge vertices, which
     // could be completely different.
     public int firstVertex;
     public int lastVertex;
 
     public int firstEdge;
     public int lastEdge;
 
     public int[] codes;
     public float[] vertices;
     public int[] colors;
     public float[] normals;
     public float[] texcoords;
     public int[] scolors;
     public float[] sweights;
 
     // Material properties
     public int[] ambient;
     public int[] specular;
     public int[] emissive;
     public float[] shininess;
     
     public int[][] edges;
     
     // Internally used by the addVertex() methods.
     protected int fillColor;
     protected int strokeColor; 
     protected float strokeWeight;
     protected int ambientColor;
     protected int specularColor;
     protected int emissiveColor;
     protected float shininessFactor; 
 
     // Indices to map input vertices to tessellated vertices.
     public int[][] pointIndices;
     public int firstPointIndex;
     public int[][] lineIndices;
     public int firstLineIndex;
     public int[][] fillIndices;
     public float[][] fillWeights;
     public int firstFillIndex;    
     
     public InGeometry(int mode) {
       renderMode = mode;
       allocate();
     }
 
     public void clear() {
       vertexCount = firstVertex = lastVertex = 0;
       edgeCount = firstEdge = lastEdge = 0;
     }
 
     public void clearEdges() {
       edgeCount = firstEdge = lastEdge = 0;
     }
 
     public void allocate() {
       codes = new int[PGL.DEFAULT_IN_VERTICES];
       vertices = new float[3 * PGL.DEFAULT_IN_VERTICES];
       colors = new int[PGL.DEFAULT_IN_VERTICES];
       normals = new float[3 * PGL.DEFAULT_IN_VERTICES];
       texcoords = new float[2 * PGL.DEFAULT_IN_VERTICES];
       scolors = new int[PGL.DEFAULT_IN_VERTICES];
       sweights = new float[PGL.DEFAULT_IN_VERTICES];
       ambient = new int[PGL.DEFAULT_IN_VERTICES];
       specular = new int[PGL.DEFAULT_IN_VERTICES];
       emissive = new int[PGL.DEFAULT_IN_VERTICES];
       shininess = new float[PGL.DEFAULT_IN_VERTICES];
       edges = new int[PGL.DEFAULT_IN_EDGES][3];
       clear();
     }
 
     public void trim() {
       if (0 < vertexCount && vertexCount < vertices.length / 3) {
         trimVertices();
         trimColors();
         trimNormals();
         trimTexcoords();
         trimStrokeColors();
         trimStrokeWeights();
         trimAmbient();
         trimSpecular();
         trimEmissive();
         trimShininess();
       }
 
       if (0 < edgeCount && edgeCount < edges.length) {
         trimEdges();
       }
     }
 
     public void dispose() {
       codes = null;
       vertices = null;
       colors = null;
       normals = null;
       texcoords = null;
       scolors = null;
       scolors = null;
       ambient = null;
       specular = null;
       emissive = null;
       shininess = null;
       edges = null;
     }
 
     public float getVertexX(int idx) {
       return vertices[3 * idx + 0];
     }
 
     public float getVertexY(int idx) {
       return vertices[3 * idx + 1];
     }
 
     public float getVertexZ(int idx) {
       return vertices[3 * idx + 2];
     }
 
     public float getLastVertexX() {
       return vertices[3 * (vertexCount - 1) + 0];
     }
 
     public float getLastVertexY() {
       return vertices[3 * (vertexCount - 1) + 1];
     }
 
     public float getLastVertexZ() {
       return vertices[3 * (vertexCount - 1) + 2];
     }
 
     public boolean isFull() {
       return PGL.MAX_TESS_VERTICES <= vertexCount;
     }
     
     
     public void getVertexMin(PVector v) {
       int index;
       for (int i = 0; i < vertexCount; i++) {
         index = 3 * i;
         v.x = PApplet.min(v.x, vertices[index++]);
         v.y = PApplet.min(v.y, vertices[index++]);
         v.z = PApplet.min(v.z, vertices[index  ]);
       }      
     }
     
 
     public void getVertexMax(PVector v) {
       int index;
       for (int i = 0; i < vertexCount; i++) {
         index = 3 * i;
         v.x = PApplet.max(v.x, vertices[index++]);
         v.y = PApplet.max(v.y, vertices[index++]);
         v.z = PApplet.max(v.z, vertices[index  ]);
       }      
     }    
     
     
     public int getVertexSum(PVector v) {
       int index;
       for (int i = 0; i < vertexCount; i++) {
         index = 3 * i;
         v.x += vertices[index++];
         v.y += vertices[index++];
         v.z += vertices[index  ];
       }
       return vertexCount;
     }
     
     
     public int addVertex(float x, float y,
                          int fcolor,
                          float u, float v,
                          int scolor, float sweight,
                          int am, int sp, int em, float shine,
                          int code) {
       return addVertex(x, y, 0,
                        fcolor,
                        0, 0, 1,
                        u, v,
                        scolor, sweight,
                        am, sp, em, shine,
                        code);
     }
 
     public int addVertex(float x, float y,
                          float u, float v,
                          int code) { 
       return addVertex(x, y, 
                        fillColor, 
                        u, v, 
                        strokeColor, strokeWeight,
                        ambientColor, specularColor, emissiveColor, shininessFactor,
                        code);
     }    
     
     public int addVertex(float x, float y,
                          int fcolor,
                          int scolor, float sweight,
                          int am, int sp, int em, float shine,
                          int code) {
       return addVertex(x, y, 0,
                        fcolor,
                        0, 0, 1,
                        0, 0,
                        scolor, sweight,
                        am, sp, em, shine,
                        code);
     }
     
     public int addVertex(float x, float y,
                          int code) { 
       return addVertex(x, y, 
                        fillColor,
                        strokeColor, strokeWeight,
                        ambientColor, specularColor, emissiveColor, shininessFactor,
                        code);
     }      
     
     public int addVertex(float x, float y, float z,
                          int fcolor,
                          float nx, float ny, float nz,
                          float u, float v,
                          int scolor, float sweight,
                          int am, int sp, int em, float shine,
                          int code) {
       vertexCheck();
       int index;
 
       codes[vertexCount] = code;
 
       index = 3 * vertexCount;
       vertices[index++] = x;
       vertices[index++] = y;
       vertices[index  ] = z;
 
       colors[vertexCount] = PGL.javaToNativeARGB(fcolor);
 
       index = 3 * vertexCount;
       normals[index++] = nx;
       normals[index++] = ny;
       normals[index  ] = nz;
 
       index = 2 * vertexCount;
       texcoords[index++] = u;
       texcoords[index  ] = v;
 
       scolors[vertexCount] = PGL.javaToNativeARGB(scolor);
       sweights[vertexCount] = sweight;
 
       ambient[vertexCount] = PGL.javaToNativeARGB(am);
       specular[vertexCount] = PGL.javaToNativeARGB(sp);
       emissive[vertexCount] = PGL.javaToNativeARGB(em);
       shininess[vertexCount] = shine;
 
       lastVertex = vertexCount;
       vertexCount++;
 
       return lastVertex;
     }
     
     public int addVertex(float x, float y, float z,
                          float nx, float ny, float nz,
                          float u, float v,
                          int code) {
       return addVertex(x, y, z,
                        fillColor,
                        nx, ny, nz,
                        u, v, 
                        strokeColor, strokeWeight,
                        ambientColor, specularColor, emissiveColor, shininessFactor,
                        code);           
     }
 
 
     public void vertexCheck() {
       if (vertexCount == vertices.length / 3) {
         int newSize = vertexCount << 1;
 
         expandCodes(newSize);
         expandVertices(newSize);
         expandColors(newSize);
         expandNormals(newSize);
         expandTexcoords(newSize);
         expandStrokeColors(newSize);
         expandStrokeWeights(newSize);
         expandAmbient(newSize);
         expandSpecular(newSize);
         expandEmissive(newSize);
         expandShininess(newSize);
       }
     }
 
     public void calcTriangleNormal(int i0, int i1, int i2) {
       int index;
 
       index = 3 * i0;
       float x0 = vertices[index++];
       float y0 = vertices[index++];
       float z0 = vertices[index  ];
 
       index = 3 * i1;
       float x1 = vertices[index++];
       float y1 = vertices[index++];
       float z1 = vertices[index  ];
 
       index = 3 * i2;
       float x2 = vertices[index++];
       float y2 = vertices[index++];
       float z2 = vertices[index  ];
 
       float v12x = x2 - x1;
       float v12y = y2 - y1;
       float v12z = z2 - z1;
 
       float v10x = x0 - x1;
       float v10y = y0 - y1;
       float v10z = z0 - z1;
 
       // The automatic normal calculation in Processing assumes
       // that vertices as given in CCW order so:
       // n = v12 x v10
       // so that the normal outwards.
       float nx = v12y * v10z - v10y * v12z;
       float ny = v12z * v10x - v10z * v12x;
       float nz = v12x * v10y - v10x * v12y;
       float d = PApplet.sqrt(nx * nx + ny * ny + nz * nz);
       nx /= d;
       ny /= d;
       nz /= d;
 
       index = 3 * i0;
       normals[index++] = nx;
       normals[index++] = ny;
       normals[index  ] = nz;
 
       index = 3 * i1;
       normals[index++] = nx;
       normals[index++] = ny;
       normals[index  ] = nz;
 
       index = 3 * i2;
       normals[index++] = nx;
       normals[index++] = ny;
       normals[index  ] = nz;
     }
 
     public int addEdge(int i, int j, boolean start, boolean end) {
       edgeCheck();
 
       int[] edge = edges[edgeCount];
       edge[0] = i;
       edge[1] = j;
 
       // Possible values for state:
       // 0 = middle edge (not start, not end)
       // 1 = start edge (start, not end)
       // 2 = end edge (not start, end)
       // 3 = isolated edge (start, end)
       edge[2] = (start ? 1 : 0) + 2 * (end ? 1 : 0);
 
       lastEdge = edgeCount;
       edgeCount++;
 
       return lastEdge;
     }
 
     public void edgeCheck() {
       if (edgeCount == edges.length) {
         int newLen = edgeCount << 1;
 
         int temp[][] = new int[newLen][3];
         PApplet.arrayCopy(edges, 0, temp, 0, edgeCount);
         edges = temp;
       }
     }
 
     protected void expandCodes(int n) {
       int temp[] = new int[n];
       PApplet.arrayCopy(codes, 0, temp, 0, vertexCount);
       codes = temp;
     }
 
     protected void expandVertices(int n) {
       float temp[] = new float[3 * n];
       PApplet.arrayCopy(vertices, 0, temp, 0, 3 * vertexCount);
       vertices = temp;
     }
 
     protected void expandColors(int n) {
       int temp[] = new int[n];
       PApplet.arrayCopy(colors, 0, temp, 0, vertexCount);
       colors = temp;
     }
 
     protected void expandNormals(int n) {
       float temp[] = new float[3 * n];
       PApplet.arrayCopy(normals, 0, temp, 0, 3 * vertexCount);
       normals = temp;
     }
 
     protected void expandTexcoords(int n) {
       float temp[] = new float[2 * n];
       PApplet.arrayCopy(texcoords, 0, temp, 0, 2 * vertexCount);
       texcoords = temp;
     }
 
     protected void expandStrokeColors(int n) {
       int temp[] = new int[n];
       PApplet.arrayCopy(scolors, 0, temp, 0, vertexCount);
       scolors = temp;
     }
 
     protected void expandStrokeWeights(int n) {
       float temp[] = new float[n];
       PApplet.arrayCopy(sweights, 0, temp, 0, vertexCount);
       sweights = temp;
     }
 
     protected void expandAmbient(int n) {
       int temp[] = new int[n];
       PApplet.arrayCopy(ambient, 0, temp, 0, vertexCount);
       ambient = temp;
     }
 
     protected void expandSpecular(int n) {
       int temp[] = new int[n];
       PApplet.arrayCopy(specular, 0, temp, 0, vertexCount);
       specular = temp;
     }
 
     protected void expandEmissive(int n) {
       int temp[] = new int[n];
       PApplet.arrayCopy(emissive, 0, temp, 0, vertexCount);
       emissive = temp;
     }
 
     protected void expandShininess(int n) {
       float temp[] = new float[n];
       PApplet.arrayCopy(shininess, 0, temp, 0, vertexCount);
       shininess = temp;
     }
 
     protected void trimVertices() {
       float temp[] = new float[3 * vertexCount];
       PApplet.arrayCopy(vertices, 0, temp, 0, 3 * vertexCount);
       vertices = temp;
     }
 
     protected void trimColors() {
       int temp[] = new int[vertexCount];
       PApplet.arrayCopy(colors, 0, temp, 0, vertexCount);
       colors = temp;
     }
 
     protected void trimNormals() {
       float temp[] = new float[3 * vertexCount];
       PApplet.arrayCopy(normals, 0, temp, 0, 3 * vertexCount);
       normals = temp;
     }
 
     protected void trimTexcoords() {
       float temp[] = new float[2 * vertexCount];
       PApplet.arrayCopy(texcoords, 0, temp, 0, 2 * vertexCount);
       texcoords = temp;
     }
 
     protected void trimStrokeColors() {
       int temp[] = new int[vertexCount];
       PApplet.arrayCopy(scolors, 0, temp, 0, vertexCount);
       scolors = temp;
     }
 
     protected void trimStrokeWeights() {
       float temp[] = new float[vertexCount];
       PApplet.arrayCopy(sweights, 0, temp, 0, vertexCount);
       sweights = temp;
     }
 
     protected void trimAmbient() {
       int temp[] = new int[vertexCount];
       PApplet.arrayCopy(ambient, 0, temp, 0, vertexCount);
       ambient = temp;
     }
 
     protected void trimSpecular() {
       int temp[] = new int[vertexCount];
       PApplet.arrayCopy(specular, 0, temp, 0, vertexCount);
       specular = temp;
     }
 
     protected void trimEmissive() {
       int temp[] = new int[vertexCount];
       PApplet.arrayCopy(emissive, 0, temp, 0, vertexCount);
       emissive = temp;
     }
 
     protected void trimShininess() {
       float temp[] = new float[vertexCount];
       PApplet.arrayCopy(shininess, 0, temp, 0, vertexCount);
       shininess = temp;
     }
 
     protected void trimEdges() {
       int temp[][] = new int[edgeCount][3];
       PApplet.arrayCopy(edges, 0, temp, 0, edgeCount);
       edges = temp;
     }
 
     public int getNumLineVertices() {
       return 4 * (lastEdge - firstEdge + 1);
     }
 
     public int getNumLineIndices() {
       return 6 * (lastEdge - firstEdge + 1);
     }
 
     public void calcTrianglesNormals() {
       for (int i = 0; i < (lastVertex - firstVertex + 1) / 3; i++) {
         int i0 = 3 * i + 0;
         int i1 = 3 * i + 1;
         int i2 = 3 * i + 2;
 
         calcTriangleNormal(i0, i1, i2);
       }
     }
 
     public void addTrianglesEdges() {
       for (int i = 0; i < (lastVertex - firstVertex + 1) / 3; i++) {
         int i0 = 3 * i + 0;
         int i1 = 3 * i + 1;
         int i2 = 3 * i + 2;
 
         addEdge(i0, i1,  true, false);
         addEdge(i1, i2, false, false);
         addEdge(i2, i0, false,  true);
       }
     }
 
     public void calcTriangleFanNormals() {
       for (int i = firstVertex + 1; i < lastVertex; i++) {
         int i0 = firstVertex;
         int i1 = i;
         int i2 = i + 1;
 
         calcTriangleNormal(i0, i1, i2);
       }
     }
 
     public void addTriangleFanEdges() {
       for (int i = firstVertex + 1; i < lastVertex; i++) {
         int i0 = firstVertex;
         int i1 = i;
         int i2 = i + 1;
 
         addEdge(i0, i1,  true, false);
         addEdge(i1, i2, false, false);
         addEdge(i2, i0, false,  true);
       }
     }
 
     public void calcTriangleStripNormals() {
       for (int i = firstVertex + 1; i < lastVertex; i++) {
         int i1 = i;
         int i0, i2;
         if (i % 2 == 0) {
           // The even triangles (0, 2, 4...) should be CW
           i0 = i + 1;
           i2 = i - 1;
         } else {
           // The even triangles (1, 3, 5...) should be CCW
           i0 = i - 1;
           i2 = i + 1;
         }
         calcTriangleNormal(i0, i1, i2);
       }
     }
 
     public void addTriangleStripEdges() {
       for (int i = firstVertex + 1; i < lastVertex; i++) {
         int i0 = i;
         int i1, i2;
         if (i % 2 == 0) {
           i1 = i - 1;
           i2 = i + 1;
         } else {
           i1 = i + 1;
           i2 = i - 1;
         }
 
         addEdge(i0, i1,  true, false);
         addEdge(i1, i2, false, false);
         addEdge(i2, i0, false,  true);
       }
     }
 
     public void calcQuadsNormals() {
       for (int i = 0; i < (lastVertex - firstVertex + 1) / 4; i++) {
         int i0 = 4 * i + 0;
         int i1 = 4 * i + 1;
         int i2 = 4 * i + 2;
         int i3 = 4 * i + 3;
 
         calcTriangleNormal(i0, i1, i2);
         calcTriangleNormal(i2, i3, i0);
       }
     }
 
     public void addQuadsEdges() {
       for (int i = 0; i < (lastVertex - firstVertex + 1) / 4; i++) {
         int i0 = 4 * i + 0;
         int i1 = 4 * i + 1;
         int i2 = 4 * i + 2;
         int i3 = 4 * i + 3;
 
         addEdge(i0, i1,  true, false);
         addEdge(i1, i2, false, false);
         addEdge(i2, i3, false,  false);
         addEdge(i3, i0, false,  true);
       }
     }
 
     public void calcQuadStripNormals() {
       for (int qd = 1; qd < (lastVertex - firstVertex + 1) / 2; qd++) {
         int i0 = firstVertex + 2 * (qd - 1);
         int i1 = firstVertex + 2 * (qd - 1) + 1;
         int i2 = firstVertex + 2 * qd;
         int i3 = firstVertex + 2 * qd + 1;
 
         calcTriangleNormal(i0, i3, i1);
         calcTriangleNormal(i0, i2, i3);
       }
     }
 
     public void addQuadStripEdges() {
       for (int qd = 1; qd < (lastVertex - firstVertex + 1) / 2; qd++) {
         int i0 = firstVertex + 2 * (qd - 1);
         int i1 = firstVertex + 2 * (qd - 1) + 1;
         int i2 = firstVertex + 2 * qd + 1;
         int i3 = firstVertex + 2 * qd;
 
         addEdge(i0, i1,  true, false);
         addEdge(i1, i2, false, false);
         addEdge(i2, i3, false,  false);
         addEdge(i3, i0, false,  true);
       }
     }
 
     public void addPolygonEdges(boolean closed) {
       // Count number of edge segments in the perimeter.
       int edgeCount = 0;
       int lnMax = lastVertex - firstVertex + 1;
       int first = firstVertex;
       int contour0 = first;
       if (!closed) lnMax--;
       for (int ln = 0; ln < lnMax; ln++) {
         int i = first + ln + 1;
         if ((i == lnMax || codes[i] == PShape.BREAK) && closed) {
           i = first + ln;
         }
         if (codes[i] != PShape.BREAK) {
           edgeCount++;
         }
       }
 
       if (0 < edgeCount) {
         boolean begin = true;
         contour0 = first;
         for (int ln = 0; ln < lnMax; ln++) {
           int i0 = first + ln;
           int i1 = first + ln + 1;
           if (codes[i0] == PShape.BREAK) contour0 = i0;
           if (i1 == lnMax || codes[i1] == PShape.BREAK) {
             // We are at the end of a contour.
             if (closed) {
               // Draw line to the first vertex of the current contour,
               // if the polygon is closed.
               i0 = first + ln;
               i1 = contour0;
               addEdge(i0, i1, begin, true);
             } else if (codes[i1] != PShape.BREAK) {
               addEdge(i0, i1, begin, false);
             }
             // We might start a new contour in the next iteration.
             begin = true;
           } else if (codes[i1] != PShape.BREAK) {
             addEdge(i0, i1, begin, false);
           }
         }
       }
     }
     
     public void initTessMaps() {
       pointIndices = new int[vertexCount][0];
       lineIndices = new int[vertexCount][0];
       fillIndices = new int[vertexCount][0];
       fillWeights = new float[vertexCount][0];
     }
     
     public void freeTessMaps() {
       pointIndices = null;
       lineIndices = null;
       fillIndices = null;
       fillWeights = null;
     }
     
     public void compactTessMaps() {
       firstPointIndex = -1;      
       firstLineIndex = -1;      
       
       boolean contiguous = true;
       for (int i = firstVertex; i <= lastVertex; i++) {
         int[] indices = fillIndices[i];
         float[] weigths = fillWeights[i];        
         if (indices.length == 1 && weigths[0] == 1) {
           if (i < lastVertex) {
             int[] indices1 = fillIndices[i + 1];
             if (indices[0] + 1 != indices1[0]) {
               contiguous = false;
               break;
             }            
           }
         } else {
           contiguous = false;
           break;
         }
       }
       if (contiguous) {
         firstFillIndex = 0 < fillIndices.length ? fillIndices[firstVertex][0] : 0;
         fillIndices = null;
         fillWeights = null;
       } else {
         firstFillIndex = -1;
       }
     }
     
     public void addPointIndex(int inIdx, int tessIdx) {
       if (renderMode == RETAINED) {
         int[] indices = pointIndices[inIdx];
         int pos;
         if (indices.length == 0) {
           indices = new int[1];
           pos = 0;
         } else {
           int len = indices.length;
           indices = new int[len + 1];
           PApplet.arrayCopy(pointIndices[inIdx], indices, len);      
           pos = len;
         }
         indices[pos] = tessIdx;
         pointIndices[inIdx] = indices;
       }     
     }
     
     public void addLineIndex(int inIdx, int tessIdx) {
       if (renderMode == RETAINED) {
         int[] indices = lineIndices[inIdx];
         int pos;
         if (indices.length == 0) {
           indices = new int[1];
           pos = 0;
         } else {
           int len = indices.length;
           indices = new int[len + 1];
           PApplet.arrayCopy(lineIndices[inIdx], indices, len);      
           pos = len;
         }
         indices[pos] = tessIdx;
         lineIndices[inIdx] = indices;
       }  
     }
         
     public void addFillIndex(int inIdx, int tessIdx, float weight) {
       if (renderMode == RETAINED) {
         int[] indices = fillIndices[inIdx];
         float[] weights = fillWeights[inIdx];
         int pos;
         if (indices.length == 0) {
           indices = new int[1];
           weights = new float[1];
           pos = 0;
         } else {
           int len = indices.length;
           indices = new int[len + 1];
           weights = new float[len + 1];
           PApplet.arrayCopy(fillIndices[inIdx], indices, len);
           PApplet.arrayCopy(fillWeights[inIdx], weights, len);          
           pos = len;
         }
         indices[pos] = tessIdx;
         weights[pos] = weight;
         fillIndices[inIdx] = indices;
         fillWeights[inIdx] = weights; 
       }
     }    
     
     public void addFillIndex(int inIdx, int tessIdx) {
       addFillIndex(inIdx, tessIdx, 1);
     }
     
     public void addFillIndices(int firstTessIdx) {
       if (renderMode == RETAINED) {
         int i0 = firstVertex;
         int i1 = lastVertex;        
         for (int i = i0; i <= i1; i++) {
           addFillIndex(i, firstTessIdx + i - i0);
         }                  
       }      
     }    
     
     public void addEllipse(int ellipseMode, float a, float b, float c, float d,
                            boolean fill, int fillColor,
                            boolean stroke, int strokeColor, float strokeWeight,
                            int ambientColor, int specularColor, int emissiveColor, 
                            float shininessFactor) {
       float x = a;
       float y = b;
       float w = c;
       float h = d;
 
       if (ellipseMode == CORNERS) {
         w = c - a;
         h = d - b;
 
       } else if (ellipseMode == RADIUS) {
         x = a - c;
         y = b - d;
         w = c * 2;
         h = d * 2;
 
       } else if (ellipseMode == DIAMETER) {
         x = a - c/2f;
         y = b - d/2f;
       }
 
       if (w < 0) {  // undo negative width
         x += w;
         w = -w;
       }
 
       if (h < 0) {  // undo negative height
         y += h;
         h = -h;
       }
       
       this.fillColor = fillColor;
       this.strokeColor = strokeColor; 
       this.strokeWeight = strokeWeight;
       this.ambientColor = ambientColor;
       this.specularColor = specularColor;
       this.emissiveColor = emissiveColor;
       this.shininessFactor = shininessFactor; 
       
       float radiusH = w / 2;
       float radiusV = h / 2;
 
       float centerX = x + radiusH;
       float centerY = y + radiusV;
 
       float sx1 = screenX(x, y);
       float sy1 = screenY(x, y);
       float sx2 = screenX(x + w, y + h);
       float sy2 = screenY(x + w, y + h);
 
       int accuracy = (int) (TWO_PI * PApplet.dist(sx1, sy1, sx2, sy2) / 20);
       if (accuracy < 6) {
         accuracy = 6;
       }
       float inc = (float) PGraphicsOpenGL.SINCOS_LENGTH / accuracy;
 
       if (fill) {
         addVertex(centerX, centerY, VERTEX);
       }
       int idx0, pidx, idx;
       idx0 = pidx = idx = 0;
       float val = 0;
       for (int i = 0; i < accuracy; i++) {
         idx = addVertex(centerX + PGraphicsOpenGL.cosLUT[(int) val] * radiusH,
                         centerY + PGraphicsOpenGL.sinLUT[(int) val] * radiusV,
                         VERTEX);
         val = (val + inc) % PGraphicsOpenGL.SINCOS_LENGTH;
 
         if (0 < i) {
           if (stroke) addEdge(pidx, idx, i == 1, false);
         } else {
           idx0 = idx;
         }
 
         pidx = idx;
       }
       // Back to the beginning
       addVertex(centerX + PGraphicsOpenGL.cosLUT[0] * radiusH,
                 centerY + PGraphicsOpenGL.sinLUT[0] * radiusV,
                 VERTEX);
       if (stroke) addEdge(idx, idx0, false, true);
     }
 
     public void addBox(float w, float h, float d,
                        boolean fill, int fillColor,
                        boolean stroke, int strokeColor, float strokeWeight,
                        int ambientColor, int specularColor, int emissiveColor, 
                        float shininessFactor) {
       float x1 = -w/2f; float x2 = w/2f;
       float y1 = -h/2f; float y2 = h/2f;
       float z1 = -d/2f; float z2 = d/2f;
 
       this.fillColor = fillColor;
       this.strokeColor = strokeColor; 
       this.strokeWeight = strokeWeight;
       this.ambientColor = ambientColor;
       this.specularColor = specularColor;
       this.emissiveColor = emissiveColor;
       this.shininessFactor = shininessFactor;     
 
       if (fill || stroke) {
         // front face
         addVertex(x1, y1, z1, 0, 0, 1, 0, 0, VERTEX);
         addVertex(x2, y1, z1, 0, 0, 1, 1, 0, VERTEX);
         addVertex(x2, y2, z1, 0, 0, 1, 1, 1, VERTEX);
         addVertex(x1, y2, z1, 0, 0, 1, 0, 1, VERTEX);
 
         // right face
         addVertex(x2, y1, z1, 1, 0, 0, 0, 0, VERTEX);
         addVertex(x2, y1, z2, 1, 0, 0, 1, 0, VERTEX);
         addVertex(x2, y2, z2, 1, 0, 0, 1, 1, VERTEX);
         addVertex(x2, y2, z1, 1, 0, 0, 0, 1, VERTEX);
 
         // back face
         addVertex(x2, y1, z2, 0, 0, -1, 0, 0, VERTEX);
         addVertex(x1, y1, z2, 0, 0, -1, 1, 0, VERTEX);
         addVertex(x1, y2, z2, 0, 0, -1, 1, 1, VERTEX);
         addVertex(x2, y2, z2, 0, 0, -1, 0, 1, VERTEX);
 
         // left face
         addVertex(x1, y1, z2, -1, 0, 0, 0, 0, VERTEX);
         addVertex(x1, y1, z1, -1, 0, 0, 1, 0, VERTEX);
         addVertex(x1, y2, z1, -1, 0, 0, 1, 1, VERTEX);
         addVertex(x1, y2, z2, -1, 0, 0, 0, 1, VERTEX);;
 
         // top face
         addVertex(x1, y1, z2, 0, 1, 0, 0, 0, VERTEX);
         addVertex(x2, y1, z2, 0, 1, 0, 1, 0, VERTEX);
         addVertex(x2, y1, z1, 0, 1, 0, 1, 1, VERTEX);
         addVertex(x1, y1, z1, 0, 1, 0, 0, 1, VERTEX);
 
         // bottom face
         addVertex(x1, y2, z1, 0, -1, 0, 0, 0, VERTEX);
         addVertex(x2, y2, z1, 0, -1, 0, 1, 0, VERTEX);
         addVertex(x2, y2, z2, 0, -1, 0, 1, 1, VERTEX);
         addVertex(x1, y2, z2, 0, -1, 0, 0, 1, VERTEX);        
       }
       
       if (stroke) {
         addEdge(0, 1, true, false);
         addEdge(1, 2, false, false);
         addEdge(2, 3, false, false);
         addEdge(3, 0, false, false);
                 
         addEdge(0,  9, false, false);
         addEdge(1,  8, false, false);
         addEdge(2, 11, false, false);
         addEdge(3, 10, false, false);
         
         addEdge( 8,  9, false, false);
         addEdge( 9, 10, false, false);
         addEdge(10, 11, false, false);
         addEdge(11,  8, false, true);        
       }
     }
     
     // Adds the vertices that define an sphere, without duplicating
     // any vertex or edge.
     public int[] addSphere(float r, int detU, int detV, 
                            boolean fill, int fillColor,
                            boolean stroke, int strokeColor, float strokeWeight,
                            int ambientColor, int specularColor, int emissiveColor, 
                            float shininessFactor) {
       if ((detU < 3) || (detV < 2)) {
         sphereDetail(30);
       }      
       
       this.fillColor = fillColor;
       this.strokeColor = strokeColor; 
       this.strokeWeight = strokeWeight;
       this.ambientColor = ambientColor;
       this.specularColor = specularColor;
       this.emissiveColor = emissiveColor;
       this.shininessFactor = shininessFactor;      
       
       int nind = 3 * sphereDetailU + (6 * sphereDetailU + 3) * (sphereDetailV - 2) + 3 * sphereDetailU;
       int[] indices = new int[nind];      
       
       int vertCount = 0;
       int indCount = 0;
       int vert0, vert1;      
       
       float u, v;
       float du = 1.0f / (sphereDetailU);
       float dv = 1.0f / (sphereDetailV);
 
       // Southern cap -------------------------------------------------------
       
       // Adding multiple copies of the south pole vertex, each one with a 
       // different u coordinate, so the texture mapping is correct when 
       // making the first strip of triangles.
       u = 1; v = 1;
       for (int i = 0; i < sphereDetailU; i++) {
         addVertex(0, r, 0, 0, 1, 0, u , v, VERTEX);
         u -= du;
       }      
       vertCount = sphereDetailU;
       vert0 = vertCount;
       u = 1; v -= dv;
       for (int i = 0; i < sphereDetailU; i++) {      
         addVertex(r * sphereX[i], r *sphereY[i], r * sphereZ[i], 
                   sphereX[i], sphereY[i], sphereZ[i], u , v, VERTEX);
         u -= du;
       }      
       vertCount += sphereDetailU;
       vert1 = vertCount;      
       addVertex(r * sphereX[0], r * sphereY[0], r * sphereZ[0], 
                 sphereX[0], sphereY[0], sphereZ[0], u, v, VERTEX);
       vertCount++;
       
       for (int i = 0; i < sphereDetailU; i++) {
         int i1 = vert0 + i;
         int i0 = vert0 + i - sphereDetailU;
         
         indices[3 * i + 0] = i1;
         indices[3 * i + 1] = i0;
         indices[3 * i + 2] = i1 + 1;
         
         addEdge(i0, i1, i == 0, false);
         addEdge(i1, i1 + 1, false, false);        
       }
       indCount += 3 * sphereDetailU;
       
       // Middle rings -------------------------------------------------------
             
       int offset = 0;
       for (int j = 2; j < sphereDetailV; j++) {      
         offset += sphereDetailU;
         vert0 = vertCount;
         u = 1; v -= dv;       
         for (int i = 0; i < sphereDetailU; i++) {
           int ioff = offset + i;
           addVertex(r * sphereX[ioff], r *sphereY[ioff], r * sphereZ[ioff], 
                     sphereX[ioff], sphereY[ioff], sphereZ[ioff], u , v, VERTEX);
           u -= du;
         }
         vertCount += sphereDetailU;
         vert1 = vertCount;
         addVertex(r * sphereX[offset], r * sphereY[offset], r * sphereZ[offset], 
                   sphereX[offset], sphereY[offset], sphereZ[offset], u, v, VERTEX);
         vertCount++;
         
         for (int i = 0; i < sphereDetailU; i++) {
           int i1 = vert0 + i;
           int i0 = vert0 + i - sphereDetailU - 1;
           
           indices[indCount + 6 * i + 0] = i1;
           indices[indCount + 6 * i + 1] = i0;
           indices[indCount + 6 * i + 2] = i0 + 1;
           
           indices[indCount + 6 * i + 3] = i1;
           indices[indCount + 6 * i + 4] = i0 + 1;
           indices[indCount + 6 * i + 5] = i1 + 1;
           
           addEdge(i0, i1, false, false);
           addEdge(i1, i1 + 1, false, false);
           addEdge(i0 + 1, i1, false, false);
         }
         indCount += 6 * sphereDetailU;
         indices[indCount + 0] = vert1;
         indices[indCount + 1] = vert1 - sphereDetailU;
         indices[indCount + 2] = vert1 - 1;
         indCount += 3;
         
         addEdge(vert1 - sphereDetailU, vert1 - 1, false, false);
         addEdge(vert1 - 1, vert1, false, false);        
       }
             
       // Northern cap -------------------------------------------------------
       
       // Adding multiple copies of the north pole vertex, each one with a 
       // different u coordinate, so the texture mapping is correct when 
       // making the last strip of triangles.      
       u = 1; v = 0;
       for (int i = 0; i < sphereDetailU; i++) {
         addVertex(0, -r, 0, 0, -1, 0, u , v, VERTEX);
         u -= du;
       }         
       vertCount += sphereDetailU;      
       
       for (int i = 0; i < sphereDetailU; i++) {
         int i0 = vert0 + i;
         int i1 = vert0 + i + sphereDetailU + 1;
         
         indices[indCount + 3 * i + 0] = i0;
         indices[indCount + 3 * i + 1] = i1;
         indices[indCount + 3 * i + 2] = i0 + 1;
         
         addEdge(i0, i0 + 1, false, false);
         addEdge(i0, i1, false, i == sphereDetailU - 1);        
       }
       indCount += 3 * sphereDetailU;      
       
       return indices;
     }
   }
 
   // Holds tessellated data for fill, line and point geometry.
   public class TessGeometry {
     int renderMode;
 
     // Tessellated fill data
     public int fillVertexCount;
     public int firstFillVertex;
     public int lastFillVertex;
     public float[] fillVertices;
     public int[] fillColors;
     public float[] fillNormals;
     public float[] fillTexcoords;
 
     // Fill material properties (fillColor is used
     // as the diffuse color when lighting is enabled)
     public int[] fillAmbient;
     public int[] fillSpecular;
     public int[] fillEmissive;
     public float[] fillShininess;
 
     public int fillIndexCount;
     public int firstFillIndex;
     public int lastFillIndex;
     public short[] fillIndices;
 
     // Tessellated line data
     public int lineVertexCount;
     public int firstLineVertex;
     public int lastLineVertex;
     public float[] lineVertices;
     public int[] lineColors;
     public float[] lineDirWidths;
 
     public int lineIndexCount;
     public int firstLineIndex;
     public int lastLineIndex;
     public short[] lineIndices;
 
     // Tessellated point data
     public int pointVertexCount;
     public int firstPointVertex;
     public int lastPointVertex;
     public float[] pointVertices;
     public int[] pointColors;
     public float[] pointSizes;
 
     public int pointIndexCount;
     public int firstPointIndex;
     public int lastPointIndex;
     public short[] pointIndices;
 
     public boolean isStroked;
 
     public TessGeometry(int mode) {
       renderMode = mode;
       allocate(false);
     }
 
     public TessGeometry(int mode, boolean empty) {
       renderMode = mode;
       allocate(empty);
     }    
     
     public void clear() {
       firstFillVertex = lastFillVertex = fillVertexCount = 0;
       firstFillIndex = lastFillIndex = fillIndexCount = 0;
 
       firstLineVertex = lastLineVertex = lineVertexCount = 0;
       firstLineIndex = lastLineIndex = lineIndexCount = 0;
 
       firstPointVertex = lastPointVertex = pointVertexCount = 0;
       firstPointIndex = lastPointIndex = pointIndexCount = 0;
 
       isStroked = false;
     }
 
     public void allocate(boolean empty) {
       if (!empty) {
         fillVertices = new float[3 * PGL.DEFAULT_TESS_VERTICES];
         fillColors = new int[PGL.DEFAULT_TESS_VERTICES];
         fillNormals = new float[3 * PGL.DEFAULT_TESS_VERTICES];
         fillTexcoords = new float[2 * PGL.DEFAULT_TESS_VERTICES];
         fillAmbient = new int[PGL.DEFAULT_TESS_VERTICES];
         fillSpecular = new int[PGL.DEFAULT_TESS_VERTICES];
         fillEmissive = new int[PGL.DEFAULT_TESS_VERTICES];
         fillShininess = new float[PGL.DEFAULT_TESS_VERTICES];
         fillIndices = new short[PGL.DEFAULT_TESS_VERTICES];
 
         lineVertices = new float[3 * PGL.DEFAULT_TESS_VERTICES];
         lineColors = new int[PGL.DEFAULT_TESS_VERTICES];
         lineDirWidths = new float[4 * PGL.DEFAULT_TESS_VERTICES];
         lineIndices = new short[PGL.DEFAULT_TESS_VERTICES];
 
         pointVertices = new float[3 * PGL.DEFAULT_TESS_VERTICES];
         pointColors = new int[PGL.DEFAULT_TESS_VERTICES];
         pointSizes = new float[2 * PGL.DEFAULT_TESS_VERTICES];
         pointIndices = new short[PGL.DEFAULT_TESS_VERTICES];
       } else {
         fillVertices = null;
         fillColors = null;
         fillNormals = null;
         fillTexcoords = null;
         fillAmbient = null;
         fillSpecular = null;
         fillEmissive = null;
         fillShininess = null;
         fillIndices = null;
 
         lineVertices = null;
         lineColors = null;
         lineDirWidths = null;
         lineIndices = null;
 
         pointVertices = null;
         pointColors = null;
         pointSizes = null;
         pointIndices = null;        
       }
       clear();
     }
 
     
     public void free() {
       
     }
     
     public void trim() {
       if (0 < fillVertexCount && fillVertexCount < fillVertices.length / 3) {
         trimFillVertices();
         trimFillColors();
         trimFillNormals();
         trimFillTexcoords();
         trimFillAmbient();
         trimFillSpecular();
         trimFillEmissive();
         trimFillShininess();
       }
 
       if (0 < fillIndexCount && fillIndexCount < fillIndices.length) {
         trimFillIndices();
       }
 
       if (0 < lineVertexCount && lineVertexCount < lineVertices.length / 3) {
         trimLineVertices();
         trimLineColors();
         trimLineAttributes();
       }
 
       if (0 < lineIndexCount && lineIndexCount < lineIndices.length) {
         trimLineIndices();
       }
 
       if (0 < pointVertexCount && pointVertexCount < pointVertices.length / 3) {
         trimPointVertices();
         trimPointColors();
         trimPointAttributes();
       }
 
       if (0 < pointIndexCount && pointIndexCount < pointIndices.length) {
         trimPointIndices();
       }
     }
 
     protected void trimFillVertices() {
       float temp[] = new float[3 * fillVertexCount];
       PApplet.arrayCopy(fillVertices, 0, temp, 0, 3 * fillVertexCount);
       fillVertices = temp;
     }
 
     protected void trimFillColors() {
       int temp[] = new int[fillVertexCount];
       PApplet.arrayCopy(fillColors, 0, temp, 0, fillVertexCount);
       fillColors = temp;
     }
 
     protected void trimFillNormals() {
       float temp[] = new float[3 * fillVertexCount];
       PApplet.arrayCopy(fillNormals, 0, temp, 0, 3 * fillVertexCount);
       fillNormals = temp;
     }
 
     protected void trimFillTexcoords() {
       float temp[] = new float[2 * fillVertexCount];
       PApplet.arrayCopy(fillTexcoords, 0, temp, 0, 2 * fillVertexCount);
       fillTexcoords = temp;
     }
 
     protected void trimFillAmbient() {
       int temp[] = new int[fillVertexCount];
       PApplet.arrayCopy(fillAmbient, 0, temp, 0, fillVertexCount);
       fillAmbient = temp;
     }
 
     protected void trimFillSpecular() {
       int temp[] = new int[fillVertexCount];
       PApplet.arrayCopy(fillSpecular, 0, temp, 0, fillVertexCount);
       fillSpecular = temp;
     }
 
     protected void trimFillEmissive() {
       int temp[] = new int[fillVertexCount];
       PApplet.arrayCopy(fillEmissive, 0, temp, 0, fillVertexCount);
       fillEmissive = temp;
     }
 
     protected void trimFillShininess() {
       float temp[] = new float[fillVertexCount];
       PApplet.arrayCopy(fillShininess, 0, temp, 0, fillVertexCount);
       fillShininess = temp;
     }
 
     public void trimFillIndices() {
       short temp[] = new short[fillIndexCount];
       PApplet.arrayCopy(fillIndices, 0, temp, 0, fillIndexCount);
       fillIndices = temp;
     }
 
     protected void trimLineVertices() {
       float temp[] = new float[3 * lineVertexCount];
       PApplet.arrayCopy(lineVertices, 0, temp, 0, 3 * lineVertexCount);
       lineVertices = temp;
     }
 
     protected void trimLineColors() {
       int temp[] = new int[lineVertexCount];
       PApplet.arrayCopy(lineColors, 0, temp, 0, lineVertexCount);
       lineColors = temp;
     }
 
     protected void trimLineAttributes() {
       float temp[] = new float[4 * lineVertexCount];
       PApplet.arrayCopy(lineDirWidths, 0, temp, 0, 4 * lineVertexCount);
       lineDirWidths = temp;
     }
 
     protected void trimLineIndices() {
       short temp[] = new short[lineIndexCount];
       PApplet.arrayCopy(lineIndices, 0, temp, 0, lineIndexCount);
       lineIndices = temp;
     }
 
     protected void trimPointVertices() {
       float temp[] = new float[3 * pointVertexCount];
       PApplet.arrayCopy(pointVertices, 0, temp, 0, 3 * pointVertexCount);
       pointVertices = temp;
     }
 
     protected void trimPointColors() {
       int temp[] = new int[pointVertexCount];
       PApplet.arrayCopy(pointColors, 0, temp, 0, pointVertexCount);
       pointColors = temp;
     }
 
     protected void trimPointAttributes() {
       float temp[] = new float[2 * pointVertexCount];
       PApplet.arrayCopy(pointSizes, 0, temp, 0, 2 * pointVertexCount);
       pointSizes = temp;
     }
 
     protected void trimPointIndices() {
       short temp[] = new short[pointIndexCount];
       PApplet.arrayCopy(pointIndices, 0, temp, 0, pointIndexCount);
       pointIndices = temp;
     }
 
     public void dipose() {
       fillVertices = null;
       fillColors = null;
       fillNormals = null;
       fillTexcoords = null;
       fillAmbient = null;
       fillSpecular = null;
       fillEmissive = null;
       fillShininess = null;
       fillIndices = null;
 
       lineVertices = null;
       lineColors = null;
       lineDirWidths = null;
       lineIndices = null;
 
       pointVertices = null;
       pointColors = null;
       pointSizes = null;
       pointIndices = null;
     }
 
     public boolean isFull() {
       return PGL.MAX_TESS_VERTICES <= fillVertexCount ||
              PGL.MAX_TESS_VERTICES <= lineVertexCount ||
              PGL.MAX_TESS_VERTICES <= pointVertexCount ||
              PGL.MAX_TESS_INDICES  <= fillIndexCount ||
              PGL.MAX_TESS_INDICES  <= fillIndexCount ||
              PGL.MAX_TESS_INDICES  <= fillIndexCount;
     }
 
     public boolean isOverflow() {
       return PGL.MAX_TESS_VERTICES < fillVertexCount ||
              PGL.MAX_TESS_VERTICES < lineVertexCount ||
              PGL.MAX_TESS_VERTICES < pointVertexCount ||
              PGL.MAX_TESS_INDICES  < fillIndexCount ||
              PGL.MAX_TESS_INDICES  < fillIndexCount ||
              PGL.MAX_TESS_INDICES  < fillIndexCount;
     }
 
     public void addCounts(TessGeometry other) {
       fillVertexCount += other.fillVertexCount;
       fillIndexCount += other.fillIndexCount;
 
       lineVertexCount += other.lineVertexCount;
       lineIndexCount += other.lineIndexCount;
 
       pointVertexCount += other.pointVertexCount;
       pointIndexCount += other.pointIndexCount;
     }
 
     public void setFirstFill(TessGeometry other) {
       firstFillVertex = other.firstFillVertex;
       firstFillIndex = other.firstFillIndex;
     }
 
     public void setLastFill(TessGeometry other) {
       lastFillVertex = other.lastFillVertex;
       lastFillIndex = other.lastFillIndex;
     }
 
     public void setFirstLine(TessGeometry other) {
       firstLineVertex = other.firstLineVertex;
       firstLineIndex = other.firstLineIndex;
     }
 
     public void setLastLine(TessGeometry other) {
       lastLineVertex = other.lastLineVertex;
       lastLineIndex = other.lastLineIndex;
     }
 
     public void setFirstPoint(TessGeometry other) {
       firstPointVertex = other.firstPointVertex;
       firstPointIndex = other.firstPointIndex;
     }
 
     public void setLastPoint(TessGeometry other) {
       lastPointVertex = other.lastPointVertex;
       lastPointIndex = other.lastPointIndex;
     }
 
     public int setFillVertex(int offset) {
       firstFillVertex = 0;
       if (0 < offset) {
         firstFillVertex = offset + 1;
       }
       lastFillVertex = firstFillVertex + fillVertexCount - 1;
       return lastFillVertex;
     }
 
     public int setFillIndex(int voffset, int ioffset) {
       firstFillIndex = 0;
       if (0 < ioffset) {
         firstFillIndex = ioffset + 1;
       }
 
       if (0 < voffset) {
         // The indices are update to take into account all the previous
         // shapes in the hierarchy, as the entire geometry will be stored
         // contiguously in a single VBO in the root node.
         for (int i = 0; i < fillIndexCount; i++) {
           fillIndices[i] += voffset;
         }
       }
 
       lastFillIndex = firstFillIndex + fillIndexCount - 1;
       return lastFillIndex;
     }
 
     public int setLineVertex(int offset) {
       firstLineVertex = 0;
       if (0 < offset) {
         firstLineVertex = offset + 1;
       }
       lastLineVertex = firstLineVertex + lineVertexCount - 1;
       return lastLineVertex;
     }
 
     public int setLineIndex(int voffset, int ioffset) {
       firstLineIndex = 0;
       if (0 < ioffset) {
         firstLineIndex = ioffset + 1;
       }
 
       if (0 < voffset) {
         // The indices are update to take into account all the previous
         // shapes in the hierarchy, as the entire geometry will be stored
         // contiguously in a single VBO in the root node.
         for (int i = 0; i < lineIndexCount; i++) {
           lineIndices[i] += voffset;
         }
       }
 
       lastLineIndex = firstLineIndex + lineIndexCount - 1;
       return lastLineIndex;
     }
 
     public int setPointVertex(int offset) {
       firstPointVertex = 0;
       if (0 < offset) {
         firstPointVertex = offset + 1;
       }
       lastPointVertex = firstPointVertex + pointVertexCount - 1;
       return lastPointVertex;
     }
 
     public int setPointIndex(int voffset, int ioffset) {
       firstPointIndex = 0;
       if (0 < ioffset) {
         firstPointIndex = ioffset + 1;
       }
 
       if (0 < voffset) {
         // The indices are update to take into account all the previous
         // shapes in the hierarchy, as the entire geometry will be stored
         // contiguously in a single VBO in the root node.
         for (int i = 0; i < pointIndexCount; i++) {
           pointIndices[i] += voffset;
         }
       }
 
       lastPointIndex = firstPointIndex + pointIndexCount - 1;
       return lastPointIndex;
     }
 
     public void fillIndexCheck() {
       if (fillIndexCount == fillIndices.length) {
         int newSize = fillIndexCount << 1;
         expandFillIndices(newSize);
       }
     }
 
     public void expandFillIndices(int n) {
       short temp[] = new short[n];
       PApplet.arrayCopy(fillIndices, 0, temp, 0, fillIndexCount);
       fillIndices = temp;
     }
 
     public void addFillIndex(int idx) {
       fillIndexCheck();
       fillIndices[fillIndexCount] = PGL.makeIndex(idx);
       fillIndexCount++;
       lastFillIndex = fillIndexCount - 1;
     }
 
     public void calcFillNormal(int i0, int i1, int i2) {
       int index;
 
       index = 3 * i0;
       float x0 = fillVertices[index++];
       float y0 = fillVertices[index++];
       float z0 = fillVertices[index  ];
 
       index = 3 * i1;
       float x1 = fillVertices[index++];
       float y1 = fillVertices[index++];
       float z1 = fillVertices[index  ];
 
       index = 3 * i2;
       float x2 = fillVertices[index++];
       float y2 = fillVertices[index++];
       float z2 = fillVertices[index  ];
 
       float v12x = x2 - x1;
       float v12y = y2 - y1;
       float v12z = z2 - z1;
 
       float v10x = x0 - x1;
       float v10y = y0 - y1;
       float v10z = z0 - z1;
 
       float nx = v12y * v10z - v10y * v12z;
       float ny = v12z * v10x - v10z * v12x;
       float nz = v12x * v10y - v10x * v12y;
       float d = PApplet.sqrt(nx * nx + ny * ny + nz * nz);
       nx /= d;
       ny /= d;
       nz /= d;
 
       index = 3 * i0;
       fillNormals[index++] = nx;
       fillNormals[index++] = ny;
       fillNormals[index  ] = nz;
 
       index = 3 * i1;
       fillNormals[index++] = nx;
       fillNormals[index++] = ny;
       fillNormals[index  ] = nz;
 
       index = 3 * i2;
       fillNormals[index++] = nx;
       fillNormals[index++] = ny;
       fillNormals[index  ] = nz;
 
     }
 
     public void fillVertexCheck() {
       if (fillVertexCount == fillVertices.length / 3) {
         int newSize = fillVertexCount << 1;
 
         expandFillVertices(newSize);
         expandFillColors(newSize);
         expandFillNormals(newSize);
         expandFillTexcoords(newSize);
         expandFillAmbient(newSize);
         expandFillSpecular(newSize);
         expandFillEmissive(newSize);
         expandFillShininess(newSize);
       }
     }
 
     public void addFillVertices(int count) {
       int oldSize = fillVertices.length / 3;
       if (fillVertexCount + count > oldSize) {
         int newSize = expandVertSize(oldSize, fillVertexCount + count);
 
         expandFillVertices(newSize);
         expandFillColors(newSize);
         expandFillNormals(newSize);
         expandFillTexcoords(newSize);
         expandFillAmbient(newSize);
         expandFillSpecular(newSize);
         expandFillEmissive(newSize);
         expandFillShininess(newSize);
       }
 
       firstFillVertex = fillVertexCount;
       fillVertexCount += count;
       lastFillVertex = fillVertexCount - 1;
     }
 
     public void addFillIndices(int count) {
       int oldSize = fillIndices.length;
       if (fillIndexCount + count > oldSize) {
         int newSize = expandIndSize(oldSize, fillIndexCount + count);
 
         expandFillIndices(newSize);
       }
 
       firstFillIndex = fillIndexCount;
       fillIndexCount += count;
       lastFillIndex = fillIndexCount - 1;
     }
 
     protected void expandFillVertices(int n) {
       float temp[] = new float[3 * n];
       PApplet.arrayCopy(fillVertices, 0, temp, 0, 3 * fillVertexCount);
       fillVertices = temp;
     }
 
     protected void expandFillColors(int n) {
       int temp[] = new int[n];
       PApplet.arrayCopy(fillColors, 0, temp, 0, fillVertexCount);
       fillColors = temp;
     }
 
     protected void expandFillNormals(int n) {
       float temp[] = new float[3 * n];
       PApplet.arrayCopy(fillNormals, 0, temp, 0, 3 * fillVertexCount);
       fillNormals = temp;
     }
 
     protected void expandFillTexcoords(int n) {
       float temp[] = new float[2 * n];
       PApplet.arrayCopy(fillTexcoords, 0, temp, 0, 2 * fillVertexCount);
       fillTexcoords = temp;
     }
 
     protected void expandFillAmbient(int n) {
       int temp[] = new int[n];
       PApplet.arrayCopy(fillAmbient, 0, temp, 0, fillVertexCount);
       fillAmbient = temp;
     }
 
     protected void expandFillSpecular(int n) {
       int temp[] = new int[n];
       PApplet.arrayCopy(fillSpecular, 0, temp, 0, fillVertexCount);
       fillSpecular = temp;
     }
 
     protected void expandFillEmissive(int n) {
       int temp[] = new int[n];
       PApplet.arrayCopy(fillEmissive, 0, temp, 0, fillVertexCount);
       fillEmissive = temp;
     }
 
     protected void expandFillShininess(int n) {
       float temp[] = new float[n];
       PApplet.arrayCopy(fillShininess, 0, temp, 0, fillVertexCount);
       fillShininess = temp;
     }
 
     public void addLineVertices(int count) {
       int oldSize = lineVertices.length / 3;
       if (lineVertexCount + count > oldSize) {
         int newSize = expandVertSize(oldSize, lineVertexCount + count);
 
         expandLineVertices(newSize);
         expandLineColors(newSize);
         expandLineAttributes(newSize);
       }
 
       firstLineVertex = lineVertexCount;
       lineVertexCount += count;
       lastLineVertex = lineVertexCount - 1;
     }
 
     protected void expandLineVertices(int n) {
       float temp[] = new float[3 * n];
       PApplet.arrayCopy(lineVertices, 0, temp, 0, 3 * lineVertexCount);
       lineVertices = temp;
     }
 
     protected void expandLineColors(int n) {
       int temp[] = new int[n];
       PApplet.arrayCopy(lineColors, 0, temp, 0, lineVertexCount);
       lineColors = temp;
     }
 
     protected void expandLineAttributes(int n) {
       float temp[] = new float[4 * n];
       PApplet.arrayCopy(lineDirWidths, 0, temp, 0, 4 * lineVertexCount);
       lineDirWidths = temp;
     }
 
     public void addLineIndices(int count) {
       int oldSize = lineIndices.length;
       if (lineIndexCount + count > oldSize) {
         int newSize = expandIndSize(oldSize, lineIndexCount + count);
 
         expandLineIndices(newSize);
       }
 
       firstLineIndex = lineIndexCount;
       lineIndexCount += count;
       lastLineIndex = lineIndexCount - 1;
     }
 
     protected void expandLineIndices(int n) {
       short temp[] = new short[n];
       PApplet.arrayCopy(lineIndices, 0, temp, 0, lineIndexCount);
       lineIndices = temp;
     }
 
     public void addPointVertices(int count) {
       int oldSize = pointVertices.length / 3;
       if (pointVertexCount + count > oldSize) {
         int newSize = expandVertSize(oldSize, pointVertexCount + count);
 
         expandPointVertices(newSize);
         expandPointColors(newSize);
         expandPointAttributes(newSize);
       }
 
       firstPointVertex = pointVertexCount;
       pointVertexCount += count;
       lastPointVertex = pointVertexCount - 1;
     }
 
     protected void expandPointVertices(int n) {
       float temp[] = new float[3 * n];
       PApplet.arrayCopy(pointVertices, 0, temp, 0, 3 * pointVertexCount);
       pointVertices = temp;
     }
 
     protected void expandPointColors(int n) {
       int temp[] = new int[n];
       PApplet.arrayCopy(pointColors, 0, temp, 0, pointVertexCount);
       pointColors = temp;
     }
 
     protected void expandPointAttributes(int n) {
       float temp[] = new float[2 * n];
       PApplet.arrayCopy(pointSizes, 0, temp, 0, 2 * pointVertexCount);
       pointSizes = temp;
     }
 
     public void addPointIndices(int count) {
       int oldSize = pointIndices.length;
       if (pointIndexCount + count > oldSize) {
         int newSize = expandIndSize(oldSize, pointIndexCount + count);
 
         expandPointIndices(newSize);
       }
 
       firstPointIndex = pointIndexCount;
       pointIndexCount += count;
       lastPointIndex = pointIndexCount - 1;
     }
 
     protected void expandPointIndices(int n) {
       short temp[] = new short[n];
       PApplet.arrayCopy(pointIndices, 0, temp, 0, pointIndexCount);
       pointIndices = temp;
     }
 
     public void addFillVertex(float x, float y, float z,
                               int rgba,
                               float nx, float ny, float nz,
                               float u, float v,
                               int am, int sp, int em, float shine) {
       fillVertexCheck();
       int index;
 
       if (renderMode == IMMEDIATE && flushMode == FLUSH_WHEN_FULL && !hints[DISABLE_TRANSFORM_CACHE]) {
         PMatrix3D mm = modelview;
         PMatrix3D nm = modelviewInv;
 
         index = 3 * fillVertexCount;
         fillVertices[index++] = x * mm.m00 + y * mm.m01 + z * mm.m02 + mm.m03;
         fillVertices[index++] = x * mm.m10 + y * mm.m11 + z * mm.m12 + mm.m13;
         fillVertices[index  ] = x * mm.m20 + y * mm.m21 + z * mm.m22 + mm.m23;
 
         index = 3 * fillVertexCount;
         fillNormals[index++] = nx * nm.m00 + ny * nm.m10 + nz * nm.m20;
         fillNormals[index++] = nx * nm.m01 + ny * nm.m11 + nz * nm.m21;
         fillNormals[index  ] = nx * nm.m02 + ny * nm.m12 + nz * nm.m22;
       } else {
         index = 3 * fillVertexCount;
         fillVertices[index++] = x;
         fillVertices[index++] = y;
         fillVertices[index  ] = z;
 
         index = 3 * fillVertexCount;
         fillNormals[index++] = nx;
         fillNormals[index++] = ny;
         fillNormals[index  ] = nz;
       }
 
       fillColors[fillVertexCount] = rgba;
 
       index = 2 * fillVertexCount;
       fillTexcoords[index++] = u;
       fillTexcoords[index  ] = v;
 
       fillAmbient[fillVertexCount] = am;
       fillSpecular[fillVertexCount] = sp;
       fillEmissive[fillVertexCount] = em;
       fillShininess[fillVertexCount] = shine;
 
       fillVertexCount++;     
     }
 
     public void addFillVertices(InGeometry in) {
       int index;
       int i0 = in.firstVertex;
       int i1 = in.lastVertex;
       int nvert = i1 - i0 + 1;
 
       addFillVertices(nvert);
 
       if (renderMode == IMMEDIATE && flushMode == FLUSH_WHEN_FULL && !hints[DISABLE_TRANSFORM_CACHE]) {
         PMatrix3D mm = modelview;
         PMatrix3D nm = modelviewInv;
 
         for (int i = 0; i < nvert; i++) {
           int inIdx = i0 + i;
           int tessIdx = firstFillVertex + i;
 
           index = 3 * inIdx;
           float x = in.vertices[index++];
           float y = in.vertices[index++];
           float z = in.vertices[index  ];
 
           index = 3 * inIdx;
           float nx = in.normals[index++];
           float ny = in.normals[index++];
           float nz = in.normals[index  ];
 
           index = 3 * tessIdx;
           fillVertices[index++] = x * mm.m00 + y * mm.m01 + z * mm.m02 + mm.m03;
           fillVertices[index++] = x * mm.m10 + y * mm.m11 + z * mm.m12 + mm.m13;
           fillVertices[index  ] = x * mm.m20 + y * mm.m21 + z * mm.m22 + mm.m23;
 
           index = 3 * tessIdx;
           fillNormals[index++] = nx * nm.m00 + ny * nm.m10 + nz * nm.m20;
           fillNormals[index++] = nx * nm.m01 + ny * nm.m11 + nz * nm.m21;
           fillNormals[index  ] = nx * nm.m02 + ny * nm.m12 + nz * nm.m22;
         }
       } else {
         if (nvert <= PGL.MIN_ARRAYCOPY_SIZE) {
           // Copying elements one by one instead of using arrayCopy is more efficient for
           // few vertices...
           for (int i = 0; i < nvert; i++) {
             int inIdx = i0 + i;
             int tessIdx = firstFillVertex + i;
 
             index = 3 * inIdx;
             float x = in.vertices[index++];
             float y = in.vertices[index++];
             float z = in.vertices[index  ];
 
             index = 3 * inIdx;
             float nx = in.normals[index++];
             float ny = in.normals[index++];
             float nz = in.normals[index  ];
 
             index = 3 * tessIdx;
             fillVertices[index++] = x;
             fillVertices[index++] = y;
             fillVertices[index  ] = z;
 
             index = 3 * tessIdx;
             fillNormals[index++] = nx;
             fillNormals[index++] = ny;
             fillNormals[index  ] = nz;
           }
         } else {
           PApplet.arrayCopy(in.vertices, 3 * i0, fillVertices, 3 * firstFillVertex, 3 * nvert);
           PApplet.arrayCopy(in.normals, 3 * i0, fillNormals, 3 * firstFillVertex, 3 * nvert);
         }
       }
 
       if (nvert <= PGL.MIN_ARRAYCOPY_SIZE) {
         for (int i = 0; i < nvert; i++) {
           int inIdx = i0 + i;
           int tessIdx = firstFillVertex + i;
 
           index = 2 * inIdx;
           float u = in.texcoords[index++];
           float v = in.texcoords[index  ];
 
           fillColors[tessIdx] = in.colors[inIdx];
 
           index = 2 * tessIdx;
           fillTexcoords[index++] = u;
           fillTexcoords[index  ] = v;
 
           fillAmbient[tessIdx] = in.ambient[inIdx];
           fillSpecular[tessIdx] = in.specular[inIdx];
           fillEmissive[tessIdx] = in.emissive[inIdx];
           fillShininess[tessIdx] = in.shininess[inIdx];
         }
       } else {
         PApplet.arrayCopy(in.colors, i0, fillColors, firstFillVertex, nvert);
         PApplet.arrayCopy(in.texcoords, 2 * i0, fillTexcoords, 2 * firstFillVertex, 2 * nvert);
         PApplet.arrayCopy(in.ambient, i0, fillAmbient, firstFillVertex, nvert);
         PApplet.arrayCopy(in.specular, i0, fillSpecular, firstFillVertex, nvert);
         PApplet.arrayCopy(in.emissive, i0, fillEmissive, firstFillVertex, nvert);
         PApplet.arrayCopy(in.shininess, i0, fillShininess, firstFillVertex, nvert);
       }
     }
 
     public void putLineVertex(InGeometry in, int inIdx0, int inIdx1, int tessIdx, int rgba) {
       int index;
 
       index = 3 * inIdx0;
       float x0 = in.vertices[index++];
       float y0 = in.vertices[index++];
       float z0 = in.vertices[index  ];
 
       index = 3 * inIdx1;
       float x1 = in.vertices[index++];
       float y1 = in.vertices[index++];
       float z1 = in.vertices[index  ];
 
       if (renderMode == IMMEDIATE && flushMode == FLUSH_WHEN_FULL && !hints[DISABLE_TRANSFORM_CACHE]) {
         PMatrix3D mm = modelview;
 
         index = 3 * tessIdx;
         lineVertices[index++] = x0 * mm.m00 + y0 * mm.m01 + z0 * mm.m02 + mm.m03;
         lineVertices[index++] = x0 * mm.m10 + y0 * mm.m11 + z0 * mm.m12 + mm.m13;
         lineVertices[index  ] = x0 * mm.m20 + y0 * mm.m21 + z0 * mm.m22 + mm.m23;
 
         index = 4 * tessIdx;
         lineDirWidths[index++] = x1 * mm.m00 + y1 * mm.m01 + z1 * mm.m02 + mm.m03;
         lineDirWidths[index++] = x1 * mm.m10 + y1 * mm.m11 + z1 * mm.m12 + mm.m13;
         lineDirWidths[index  ] = x1 * mm.m20 + y1 * mm.m21 + z1 * mm.m22 + mm.m23;
       } else {
         index = 3 * tessIdx;
         lineVertices[index++] = x0;
         lineVertices[index++] = y0;
         lineVertices[index  ] = z0;
 
         index = 4 * tessIdx;
         lineDirWidths[index++] = x1;
         lineDirWidths[index++] = y1;
         lineDirWidths[index  ] = z1;
       }
 
       lineColors[tessIdx] = rgba;
     }
 
     public void putLineVertex(InGeometry in, int inIdx0, int inIdx1, int tessIdx) {
       putLineVertex(in, inIdx0, inIdx1, tessIdx, in.scolors[inIdx0]);
     }
 
 
     public void putPointVertex(InGeometry in, int inIdx, int tessIdx) {
       int index;
 
       index = 3 * inIdx;
       float x = in.vertices[index++];
       float y = in.vertices[index++];
       float z = in.vertices[index ];
 
       if (renderMode == IMMEDIATE && flushMode == FLUSH_WHEN_FULL && !hints[DISABLE_TRANSFORM_CACHE]) {
         PMatrix3D mm = modelview;
 
         index = 3 * tessIdx;
         pointVertices[index++] = x * mm.m00 + y * mm.m01 + z * mm.m02 + mm.m03;
         pointVertices[index++] = x * mm.m10 + y * mm.m11 + z * mm.m12 + mm.m13;
         pointVertices[index  ] = x * mm.m20 + y * mm.m21 + z * mm.m22 + mm.m23;
       } else {
         index = 3 * tessIdx;
         pointVertices[index++] = x;
         pointVertices[index++] = y;
         pointVertices[index  ] = z;
       }
 
       pointColors[tessIdx] = in.scolors[inIdx];
     }
 
     public int expandVertSize(int currSize, int newMinSize) {
       int newSize = currSize;
       while (newSize < newMinSize) {
         newSize <<= 1;
       }
       return newSize;
     }
 
     public int expandIndSize(int currSize, int newMinSize) {
       int newSize = currSize;
       while (newSize < newMinSize) {
         newSize <<= 1;
       }
       return newSize;
     }
 
     public void getVertexMin(PVector v) {
       int index;
       for (int i = 0; i < fillVertexCount; i++) {
         index = 3 * i;
         v.x = PApplet.min(v.x, fillVertices[index++]);
         v.y = PApplet.min(v.y, fillVertices[index++]);
         v.z = PApplet.min(v.z, fillVertices[index  ]);
       }     
       for (int i = 0; i < lineVertexCount; i++) {
         index = 3 * i;
         v.x += PApplet.min(v.x, lineVertices[index++]);
         v.y += PApplet.min(v.y, lineVertices[index++]);
         v.z += PApplet.min(v.z, lineVertices[index  ]);
       }
       for (int i = 0; i < pointVertexCount; i++) {
         index = 3 * i;
         v.x += PApplet.min(v.x, pointVertices[index++]);
         v.y += PApplet.min(v.y, pointVertices[index++]);
         v.z += PApplet.min(v.z, pointVertices[index  ]);
       }      
     }
     
     public void getVertexMax(PVector v) {
       int index;
       for (int i = 0; i < fillVertexCount; i++) {
         index = 3 * i;
         v.x = PApplet.max(v.x, fillVertices[index++]);
         v.y = PApplet.max(v.y, fillVertices[index++]);
         v.z = PApplet.max(v.z, fillVertices[index  ]);
       }
       for (int i = 0; i < lineVertexCount; i++) {
         index = 3 * i;
         v.x += PApplet.max(v.x, lineVertices[index++]);
         v.y += PApplet.max(v.y, lineVertices[index++]);
         v.z += PApplet.max(v.z, lineVertices[index  ]);
       }
       for (int i = 0; i < pointVertexCount; i++) {
         index = 3 * i;
         v.x += PApplet.max(v.x, pointVertices[index++]);
         v.y += PApplet.max(v.y, pointVertices[index++]);
         v.z += PApplet.max(v.z, pointVertices[index  ]);
       }          
     }       
     
     public int getVertexSum(PVector v) {
       int index;
       for (int i = 0; i < fillVertexCount; i++) {
         index = 3 * i;
         v.x += fillVertices[index++];
         v.y += fillVertices[index++];
         v.z += fillVertices[index  ];
       }
       for (int i = 0; i < lineVertexCount; i++) {
         index = 3 * i;
         v.x += lineVertices[index++];
         v.y += lineVertices[index++];
         v.z += lineVertices[index  ];
       }
       for (int i = 0; i < pointVertexCount; i++) {
         index = 3 * i;
         v.x += pointVertices[index++];
         v.y += pointVertices[index++];
         v.z += pointVertices[index  ];
       }
       return fillVertexCount + lineVertexCount + pointVertexCount;
     }
 
     public void applyMatrix(PMatrix tr) {
       if (tr instanceof PMatrix2D) {
         applyMatrix((PMatrix2D) tr);
       } else if (tr instanceof PMatrix3D) {
         applyMatrix((PMatrix3D) tr);
       }      
     }
     
     public void applyMatrix(PMatrix2D tr) {
       if (0 < fillVertexCount) {
         int index;
 
         for (int i = 0; i < fillVertexCount; i++) {
           index = 3 * i;
           float x = fillVertices[index++];
           float y = fillVertices[index  ];
 
           index = 3 * i;
           float nx = fillNormals[index++];
           float ny = fillNormals[index  ];
 
           index = 3 * i;
           fillVertices[index++] = x * tr.m00 + y * tr.m01 + tr.m02;
           fillVertices[index  ] = x * tr.m10 + y * tr.m11 + tr.m12;
 
           index = 3 * i;
           fillNormals[index++] = nx * tr.m00 + ny * tr.m01;
           fillNormals[index  ] = nx * tr.m10 + ny * tr.m11;
         }
       }
 
       if (0 < lineVertexCount) {
         int index;
 
         for (int i = 0; i < lineVertexCount; i++) {
           index = 3 * i;
           float x = lineVertices[index++];
           float y = lineVertices[index  ];
 
           index = 4 * i;
           float xa = lineDirWidths[index++];
           float ya = lineDirWidths[index  ];
 
           index = 3 * i;
           lineVertices[index++] = x * tr.m00 + y * tr.m01 + tr.m02;
           lineVertices[index  ] = x * tr.m10 + y * tr.m11 + tr.m12;
 
           index = 4 * i;
           lineDirWidths[index++] = xa * tr.m00 + ya * tr.m01 + tr.m02;
           lineDirWidths[index  ] = xa * tr.m10 + ya * tr.m11 + tr.m12;
         }
       }
 
       if (0 < pointVertexCount) {
         int index;
 
         for (int i = 0; i < pointVertexCount; i++) {
           index = 3 * i;
           float x = pointVertices[index++];
           float y = pointVertices[index  ];
 
           index = 3 * i;
           pointVertices[index++] = x * tr.m00 + y * tr.m01 + tr.m02;
           pointVertices[index  ] = x * tr.m10 + y * tr.m11 + tr.m12;
         }
       }
     }
 
     public void applyMatrix(PMatrix3D tr) {
       if (0 < fillVertexCount) {
         int index;
 
         for (int i = 0; i < fillVertexCount; i++) {
           index = 3 * i;
           float x = fillVertices[index++];
           float y = fillVertices[index++];
           float z = fillVertices[index  ];
 
           index = 3 * i;
           float nx = fillNormals[index++];
           float ny = fillNormals[index++];
           float nz = fillNormals[index  ];
 
           index = 3 * i;
           fillVertices[index++] = x * tr.m00 + y * tr.m01 + z * tr.m02 + tr.m03;
           fillVertices[index++] = x * tr.m10 + y * tr.m11 + z * tr.m12 + tr.m13;
           fillVertices[index  ] = x * tr.m20 + y * tr.m21 + z * tr.m22 + tr.m23;
 
           index = 3 * i;
           fillNormals[index++] = nx * tr.m00 + ny * tr.m01 + nz * tr.m02;
           fillNormals[index++] = nx * tr.m10 + ny * tr.m11 + nz * tr.m12;
           fillNormals[index  ] = nx * tr.m20 + ny * tr.m21 + nz * tr.m22;
         }
       }
 
       if (0 < lineVertexCount) {
         int index;
 
         for (int i = 0; i < lineVertexCount; i++) {
           index = 3 * i;
           float x = lineVertices[index++];
           float y = lineVertices[index++];
           float z = lineVertices[index  ];
 
           index = 4 * i;
           float xa = lineDirWidths[index++];
           float ya = lineDirWidths[index++];
           float za = lineDirWidths[index  ];
 
           index = 3 * i;
           lineVertices[index++] = x * tr.m00 + y * tr.m01 + z * tr.m02 + tr.m03;
           lineVertices[index++] = x * tr.m10 + y * tr.m11 + z * tr.m12 + tr.m13;
           lineVertices[index  ] = x * tr.m20 + y * tr.m21 + z * tr.m22 + tr.m23;
 
           index = 4 * i;
           lineDirWidths[index++] = xa * tr.m00 + ya * tr.m01 + za * tr.m02 + tr.m03;
           lineDirWidths[index++] = xa * tr.m10 + ya * tr.m11 + za * tr.m12 + tr.m13;
           lineDirWidths[index  ] = xa * tr.m20 + ya * tr.m21 + za * tr.m22 + tr.m23;
         }
       }
 
       if (0 < pointVertexCount) {
         int index;
 
         for (int i = 0; i < pointVertexCount; i++) {
           index = 3 * i;
           float x = pointVertices[index++];
           float y = pointVertices[index++];
           float z = pointVertices[index  ];
 
           index = 3 * i;
           pointVertices[index++] = x * tr.m00 + y * tr.m01 + z * tr.m02 + tr.m03;
           pointVertices[index++] = x * tr.m10 + y * tr.m11 + z * tr.m12 + tr.m13;
           pointVertices[index  ] = x * tr.m20 + y * tr.m21 + z * tr.m22 + tr.m23;
         }
       }
     }
   }
 
   
 //  final static protected float sinLUT[];
 //  final static protected float cosLUT[];
 //  final static protected float SINCOS_PRECISION = 0.5f;
 //  final static protected int SINCOS_LENGTH = (int) (360f / SINCOS_PRECISION);
 //  static {
 //    sinLUT = new float[SINCOS_LENGTH];
 //    cosLUT = new float[SINCOS_LENGTH];
 //    for (int i = 0; i < SINCOS_LENGTH; i++) {
 //      sinLUT[i] = (float) Math.sin(i * DEG_TO_RAD * SINCOS_PRECISION);
 //      cosLUT[i] = (float) Math.cos(i * DEG_TO_RAD * SINCOS_PRECISION);
 //    }
 //  }
   
 
   // Generates tessellated geometry given a batch of input vertices.
   public class Tessellator {
     InGeometry in;
     TessGeometry tess;
     PGL.Tessellator gluTess;
     TessellatorCallback callback;
     
     boolean fill;
     boolean stroke;
     float strokeWeight;
     int strokeJoin;
     int strokeCap;
     int bezierDetil = 20;
 
     public Tessellator() {
       callback = new TessellatorCallback();
       gluTess = pgl.createTessellator(callback);
       bezierDetil = 20;
     }
     
     public void setInGeometry(InGeometry in) {
       this.in = in;
     }
 
     public void setTessGeometry(TessGeometry tess) {
       this.tess = tess;
     }
 
     public void setFill(boolean fill) {
       this.fill = fill;
     }
 
     public void setStroke(boolean stroke) {
       this.stroke = stroke;
     }
 
     public void setStrokeWeight(float weight) {
       this.strokeWeight = weight;
     }
 
     public void setStrokeJoin(int strokeJoin) {
       this.strokeJoin = strokeJoin;
     }
 
     public void setStrokeCap(int strokeCap) {
       this.strokeCap = strokeCap;
     }
 
     public void tessellatePoints() {
       if (strokeCap == ROUND) {
         tessellateRoundPoints();
       } else {
         tessellateSquarePoints();
       }
     }
 
     protected void tessellateRoundPoints() {
       int nInVert = in.lastVertex - in.firstVertex + 1;
 
       if (stroke && 1 <= nInVert) {
         tess.isStroked = true;
 
         // Each point generates a separate triangle fan.
         // The number of triangles of each fan depends on the
         // stroke weight of the point.
         int nvertTot = 0;
         int nindTot = 0;
         for (int i = in.firstVertex; i <= in.lastVertex; i++) {
           int perim = PApplet.max(MIN_POINT_ACCURACY, (int) (TWO_PI * strokeWeight / 20));
           // Number of points along the perimeter plus the center point.
           int nvert = perim + 1;
           nvertTot += nvert;
           nindTot += 3 * (nvert - 1);
         }
 
         checkForFlush(tess.lineVertexCount + nvertTot, tess.lineIndexCount + nindTot);
 
         tess.addPointVertices(nvertTot);
         tess.addPointIndices(nindTot);
         int vertIdx = tess.firstPointVertex;
         int attribIdx = tess.firstPointVertex;
         int indIdx = tess.firstPointIndex;
         int firstVert = tess.firstPointVertex;
         for (int i = in.firstVertex; i <= in.lastVertex; i++) {
           // Creating the triangle fan for each input vertex.
           int perim = PApplet.max(MIN_POINT_ACCURACY, (int) (TWO_PI * strokeWeight / 20));
           int nvert = perim + 1;
 
           // All the tessellated vertices are identical to the center point
           for (int k = 0; k < nvert; k++) {
             tess.putPointVertex(in, i, vertIdx);
             in.addPointIndex(i, vertIdx);
             vertIdx++;
           }
 
           // The attributes for each tessellated vertex are the displacement along
           // the circle perimeter. The point shader will read these attributes and
           // displace the vertices in screen coordinates so the circles are always
           // camera facing (bilboards)
           tess.pointSizes[2 * attribIdx + 0] = 0;
           tess.pointSizes[2 * attribIdx + 1] = 0;
           attribIdx++;
           float val = 0;
           float inc = (float) SINCOS_LENGTH / perim;
           for (int k = 0; k < perim; k++) {
             tess.pointSizes[2 * attribIdx + 0] = 0.5f * cosLUT[(int) val] * strokeWeight;
             tess.pointSizes[2 * attribIdx + 1] = 0.5f * sinLUT[(int) val] * strokeWeight;
             val = (val + inc) % SINCOS_LENGTH;
             attribIdx++;
           }
 
           // Adding vert0 to take into account the triangles of all
           // the preceding points.
           for (int k = 1; k < nvert - 1; k++) {
             tess.pointIndices[indIdx++] = PGL.makeIndex(firstVert + 0);
             tess.pointIndices[indIdx++] = PGL.makeIndex(firstVert + k);
             tess.pointIndices[indIdx++] = PGL.makeIndex(firstVert + k + 1);
           }
           // Final triangle between the last and first point:
           tess.pointIndices[indIdx++] = PGL.makeIndex(firstVert + 0);
           tess.pointIndices[indIdx++] = PGL.makeIndex(firstVert + 1);
           tess.pointIndices[indIdx++] = PGL.makeIndex(firstVert + nvert - 1);
 
           firstVert = vertIdx;
         }
       }
     }
 
     protected void tessellateSquarePoints() {
       int nInVert = in.lastVertex - in.firstVertex + 1;
 
       if (stroke && 1 <= nInVert) {
         tess.isStroked = true;
 
         // Each point generates a separate quad.
         int quadCount = nInVert;
 
         // Each quad is formed by 5 vertices, the center one
         // is the input vertex, and the other 4 define the
         // corners (so, a triangle fan again).
         int nvertTot = 5 * quadCount;
         // So the quad is formed by 4 triangles, each requires
         // 3 indices.
         int nindTot = 12 * quadCount;
 
         checkForFlush(tess.lineVertexCount + nvertTot, tess.lineIndexCount + nindTot);
 
         tess.addPointVertices(nvertTot);
         tess.addPointIndices(nindTot);
         int vertIdx = tess.firstPointVertex;
         int attribIdx = tess.firstPointVertex;
         int indIdx = tess.firstPointIndex;
         int firstVert = tess.firstPointVertex;
         for (int i = in.firstVertex; i <= in.lastVertex; i++) {
           int nvert = 5;
 
           for (int k = 0; k < nvert; k++) {
             tess.putPointVertex(in, i, vertIdx);
             in.addPointIndex(i, vertIdx);
             vertIdx++;
           }
 
           // The attributes for each tessellated vertex are the displacement along
           // the quad corners. The point shader will read these attributes and
           // displace the vertices in screen coordinates so the quads are always
           // camera facing (bilboards)
           tess.pointSizes[2 * attribIdx + 0] = 0;
           tess.pointSizes[2 * attribIdx + 1] = 0;
           attribIdx++;
           for (int k = 0; k < 4; k++) {
             tess.pointSizes[2 * attribIdx + 0] = 0.5f * QUAD_POINT_SIGNS[k][0] * strokeWeight;
             tess.pointSizes[2 * attribIdx + 1] = 0.5f * QUAD_POINT_SIGNS[k][1] * strokeWeight;
             attribIdx++;
           }
 
           // Adding firstVert to take into account the triangles of all
           // the preceding points.
           for (int k = 1; k < nvert - 1; k++) {
             tess.pointIndices[indIdx++] = PGL.makeIndex(firstVert + 0);
             tess.pointIndices[indIdx++] = PGL.makeIndex(firstVert + k);
             tess.pointIndices[indIdx++] = PGL.makeIndex(firstVert + k + 1);
           }
           // Final triangle between the last and first point:
           tess.pointIndices[indIdx++] = PGL.makeIndex(firstVert + 0);
           tess.pointIndices[indIdx++] = PGL.makeIndex(firstVert + 1);
           tess.pointIndices[indIdx++] = PGL.makeIndex(firstVert + nvert - 1);
 
           firstVert = vertIdx;
         }
       }
     }
 
     public void tessellateLines() {
       int nInVert = in.lastVertex - in.firstVertex + 1;
 
       if (stroke && 2 <= nInVert) {
         tess.isStroked = true;
 
         int lineCount = nInVert / 2;
         int first = in.firstVertex;
 
         // Lines are made up of 4 vertices defining the quad.
         // Each vertex has its own offset representing the stroke weight.
         int nvert = lineCount * 4;
         // Each stroke line has 4 vertices, defining 2 triangles, which
         // require 3 indices to specify their connectivities.
         int nind = lineCount * 2 * 3;
 
         checkForFlush(tess.lineVertexCount + nvert, tess.lineIndexCount + nvert);
 
         tess.addLineVertices(nvert);
         tess.addLineIndices(nind);
         int vcount = tess.firstLineVertex;
         int icount = tess.firstLineIndex;
         for (int ln = 0; ln < lineCount; ln++) {
           int i0 = first + 2 * ln + 0;
           int i1 = first + 2 * ln + 1;
           addLine(i0, i1, vcount, icount); vcount += 4; icount += 6;
         }
       }
     }
     
     public void tessellateTriangles() {
       int nInVert = in.lastVertex - in.firstVertex + 1;
 
       if (fill && 3 <= nInVert) {
         int nInInd = nInVert;
         checkForFlush(tess.fillVertexCount + nInVert, tess.fillIndexCount + nInInd);
 
         tess.addFillVertices(in);
         in.addFillIndices(tess.firstFillVertex);
 
         tess.addFillIndices(nInInd);
         int idx0 = tess.firstFillIndex;
         int offset = tess.firstFillVertex;
         for (int i = in.firstVertex; i <= in.lastVertex; i++) {
           tess.fillIndices[idx0 + i] = PGL.makeIndex(offset + i);
         }
       }
 
       if (stroke) {
         tess.isStroked = true;
         tessellateEdges();
       }
     }
 
     public void tessellateTriangles(int[] indices) {
       int nInVert = in.lastVertex - in.firstVertex + 1;
       
       if (fill && 3 <= nInVert) {
         int nInInd = indices.length;
         checkForFlush(tess.fillVertexCount + nInVert, tess.fillIndexCount + nInInd);
 
         tess.addFillVertices(in);
         in.addFillIndices(tess.firstFillVertex);
         
         tess.addFillIndices(nInInd);
         int idx0 = tess.firstFillIndex;
         int offset = tess.firstFillVertex;
         for (int i = 0; i < nInInd; i++) {
           tess.fillIndices[idx0 + i] = PGL.makeIndex(offset + indices[i]);
         }
       }
       
       if (stroke) {
         tess.isStroked = true;
         tessellateEdges();
       }      
     }
     
     public void tessellateTriangleFan() {
       int nInVert = in.lastVertex - in.firstVertex + 1;
 
       if (fill && 3 <= nInVert) {
         int nInInd = 3 * (nInVert - 2);
         checkForFlush(tess.fillVertexCount + nInVert, tess.fillIndexCount + nInInd);
 
         tess.addFillVertices(in);
         in.addFillIndices(tess.firstFillVertex);
 
         tess.addFillIndices(nInInd);
         int idx = tess.firstFillIndex;
         int offset = tess.firstFillVertex;
         for (int i = in.firstVertex + 1; i < in.lastVertex; i++) {
           tess.fillIndices[idx++] = PGL.makeIndex(offset + in.firstVertex);
           tess.fillIndices[idx++] = PGL.makeIndex(offset + i);
           tess.fillIndices[idx++] = PGL.makeIndex(offset + i + 1);
         }
       }
 
       if (stroke) {
         tess.isStroked = true;
         tessellateEdges();
       }
     }
 
     public void tessellateTriangleStrip() {
       int nInVert = in.lastVertex - in.firstVertex + 1;
 
       if (fill && 3 <= nInVert) {
         int triCount = nInVert - 2;
         int nInInd = 3 * triCount;
 
         checkForFlush(tess.fillVertexCount + nInVert, tess.fillIndexCount + nInInd);
 
         tess.addFillVertices(in);
         in.addFillIndices(tess.firstFillVertex);
 
         // Each vertex, except the first and last, defines a triangle.
         tess.addFillIndices(nInInd);
         int idx = tess.firstFillIndex;
         int offset = tess.firstFillVertex;
         for (int i = in.firstVertex + 1; i < in.lastVertex; i++) {
           tess.fillIndices[idx++] = PGL.makeIndex(offset + i);
           if (i % 2 == 0) {
             tess.fillIndices[idx++] = PGL.makeIndex(offset + i - 1);
             tess.fillIndices[idx++] = PGL.makeIndex(offset + i + 1);
           } else {
             tess.fillIndices[idx++] = PGL.makeIndex(offset + i + 1);
             tess.fillIndices[idx++] = PGL.makeIndex(offset + i - 1);
           }
         }
       }
 
       if (stroke) {
         tess.isStroked = true;
         tessellateEdges();
       }
     }
 
     public void tessellateQuads() {
       int nInVert = in.lastVertex - in.firstVertex + 1;
 
       if (fill && 4 <= nInVert) {
         int quadCount = nInVert / 4;
         int nInInd = 6 * quadCount;
 
         checkForFlush(tess.fillVertexCount + nInVert, tess.fillIndexCount + nInInd);
         tess.addFillVertices(in);
         in.addFillIndices(tess.firstFillVertex);
 
         tess.addFillIndices(nInInd);
         int idx = tess.firstFillIndex;
         int offset = tess.firstFillVertex;
         for (int qd = 0; qd < quadCount; qd++) {
           int i0 = offset + 4 * qd + 0;
           int i1 = offset + 4 * qd + 1;
           int i2 = offset + 4 * qd + 2;
           int i3 = offset + 4 * qd + 3;
 
           tess.fillIndices[idx++] = PGL.makeIndex(i0);
           tess.fillIndices[idx++] = PGL.makeIndex(i1);
           tess.fillIndices[idx++] = PGL.makeIndex(i3);
 
           tess.fillIndices[idx++] = PGL.makeIndex(i1);
           tess.fillIndices[idx++] = PGL.makeIndex(i2);
           tess.fillIndices[idx++] = PGL.makeIndex(i3);
         }
       }
 
       if (stroke) {
         tess.isStroked = true;
         tessellateEdges();
       }
     }
 
 
     public void tessellateQuadStrip() {
       int nInVert = in.lastVertex - in.firstVertex + 1;
 
       if (fill && 4 <= nInVert) {
         int quadCount = nInVert / 2 - 1;
         int nInInd = 6 * quadCount;
 
         checkForFlush(tess.fillVertexCount + nInVert, tess.fillIndexCount + nInInd);
         tess.addFillVertices(in);
         in.addFillIndices(tess.firstFillVertex);
 
         tess.addFillIndices(nInInd);
         int idx = tess.firstFillIndex;
         int offset = tess.firstFillVertex;
         for (int qd = 1; qd < nInVert / 2; qd++) {
           int i0 = offset + 2 * (qd - 1);
           int i1 = offset + 2 * (qd - 1) + 1;
           int i2 = offset + 2 * qd + 1;
           int i3 = offset + 2 * qd;
 
           tess.fillIndices[idx++] = PGL.makeIndex(i0);
           tess.fillIndices[idx++] = PGL.makeIndex(i1);
           tess.fillIndices[idx++] = PGL.makeIndex(i3);
 
           tess.fillIndices[idx++] = PGL.makeIndex(i1);
           tess.fillIndices[idx++] = PGL.makeIndex(i2);
           tess.fillIndices[idx++] = PGL.makeIndex(i3);
         }
       }
 
       if (stroke) {
         tess.isStroked = true;
         tessellateEdges();
       }
     }
 
     public void tessellatePolygon(boolean solid, boolean closed, boolean calcNormals) {
       int nInVert = in.lastVertex - in.firstVertex + 1;
 
       callback.calcNormals = calcNormals;
 
       if (fill && 3 <= nInVert) {
         checkForFlush(nInVert);
 
         gluTess.beginPolygon();
 
         if (solid) {
           // Using NONZERO winding rule for solid polygons.
           gluTess.setWindingRule(PGL.GLU_TESS_WINDING_NONZERO);
         } else {
           // Using ODD winding rule to generate polygon with holes.
           gluTess.setWindingRule(PGL.GLU_TESS_WINDING_ODD);
         }
 
         gluTess.beginContour();
 
         // Now, iterate over all input data and send to GLU tessellator..
         for (int i = in.firstVertex; i <= in.lastVertex; i++) {
           boolean breakPt = in.codes[i] == PShape.BREAK;
           if (breakPt) {
             gluTess.endContour();
             gluTess.beginContour();
           }
 
           // Separting colors into individual rgba components for interpolation.
           int fa = (in.colors[i] >> 24) & 0xFF;
           int fr = (in.colors[i] >> 16) & 0xFF;
           int fg = (in.colors[i] >>  8) & 0xFF;
           int fb = (in.colors[i] >>  0) & 0xFF;
 
           int aa = (in.ambient[i] >> 24) & 0xFF;
           int ar = (in.ambient[i] >> 16) & 0xFF;
           int ag = (in.ambient[i] >>  8) & 0xFF;
           int ab = (in.ambient[i] >>  0) & 0xFF;
 
           int sa = (in.specular[i] >> 24) & 0xFF;
           int sr = (in.specular[i] >> 16) & 0xFF;
           int sg = (in.specular[i] >>  8) & 0xFF;
           int sb = (in.specular[i] >>  0) & 0xFF;
 
           int ea = (in.emissive[i] >> 24) & 0xFF;
           int er = (in.emissive[i] >> 16) & 0xFF;
           int eg = (in.emissive[i] >>  8) & 0xFF;
           int eb = (in.emissive[i] >>  0) & 0xFF;
 
           // Vertex data includes coordinates, colors, normals, texture coordinates, and material properties.
           double[] vertex = new double[] { in.vertices [3 * i + 0], in.vertices [3 * i + 1], in.vertices[3 * i + 2],
                                            fa, fr, fg, fb,
                                            in.normals  [3 * i + 0], in.normals  [3 * i + 1], in.normals [3 * i + 2],
                                            in.texcoords[2 * i + 0], in.texcoords[2 * i + 1],
                                            aa, ar, ag, ab, sa, sr, sg, sb, ea, er, eg, eb,
                                            in.shininess[i], i, 1.0 };
 
           gluTess.addVertex(vertex);
         }
         gluTess.endContour();
 
         gluTess.endPolygon();
       }
 
       if (stroke) {
         tess.isStroked = true;
         tessellateEdges();
       }
     }
 
     // Adding the data that defines a quad starting at vertex i0 and
     // ending at i1.
     protected void addLine(int i0, int i1, int vcount, int icount) {
       tess.putLineVertex(in, i0, i1, vcount);
       tess.lineDirWidths[4 * vcount + 3] = +strokeWeight;
       tess.lineIndices[icount++] = PGL.makeIndex(vcount);
       in.addLineIndex(i0, vcount);
       
       vcount++;
       tess.putLineVertex(in, i0, i1, vcount);
       tess.lineDirWidths[4 * vcount + 3] = -strokeWeight;
       tess.lineIndices[icount++] = PGL.makeIndex(vcount);
       in.addLineIndex(i0, vcount);
       
       vcount++;
       tess.putLineVertex(in, i1, i0, vcount);
       tess.lineDirWidths[4 * vcount + 3] = -strokeWeight;
       tess.lineIndices[icount++] = PGL.makeIndex(vcount);
       in.addLineIndex(i1, vcount);
       
       // Starting a new triangle re-using prev vertices.
       tess.lineIndices[icount++] = PGL.makeIndex(vcount);      
       tess.lineIndices[icount++] = PGL.makeIndex(vcount - 1); 
 
       vcount++;
       tess.putLineVertex(in, i1, i0, vcount);
       tess.lineDirWidths[4 * vcount + 3] = +strokeWeight;
       tess.lineIndices[icount++] = PGL.makeIndex(vcount);
       in.addLineIndex(i1, vcount);
     }
 
     public void tessellateEdges() {
       int nInVert = in.getNumLineVertices();
       int nInInd = in.getNumLineIndices();
 
       checkForFlush(tess.lineVertexCount + nInVert, tess.lineIndexCount + nInInd);
 
       tess.addLineVertices(nInVert);
       tess.addLineIndices(nInInd);
       int vcount = tess.firstLineVertex;
       int icount = tess.firstLineIndex;
       for (int i = in.firstEdge; i <= in.lastEdge; i++) {
         int[] edge = in.edges[i];
         addLine(edge[0], edge[1], vcount, icount); vcount += 4; icount += 6;
       }
     }
 
     protected void checkForFlush(int vertCount) {
       if (tess.renderMode == IMMEDIATE && PGL.MAX_TESS_VERTICES < vertCount) {
         setLastTexIndex(tess.lastFillIndex);
         flush();
         setFirstTexIndex(0);
       }
     }
 
     protected void checkForFlush(int vertCount, int indCount) {
       if (tess.renderMode == IMMEDIATE && (PGL.MAX_TESS_VERTICES < vertCount ||
                                            PGL.MAX_TESS_INDICES  < indCount)) {
         setLastTexIndex(tess.lastFillIndex);
         flush();
         setFirstTexIndex(0);
       }
     }
 
     protected boolean startEdge(int edge) {
       return edge % 2 != 0;
     }
 
     protected boolean endEdge(int edge) {
       return 1 < edge;
     }
 
     protected class TessellatorCallback implements PGL.TessellatorCallback {
       public boolean calcNormals;
       protected int tessFirst;
       protected int tessCount;
       protected int tessType;
 
       public void begin(int type) {
         tessFirst = tess.fillVertexCount;
         tessCount = 0;
 
         switch (type) {
         case PGL.GL_TRIANGLE_FAN:
           tessType = TRIANGLE_FAN;
           break;
         case PGL.GL_TRIANGLE_STRIP:
           tessType = TRIANGLE_STRIP;
           break;
         case PGL.GL_TRIANGLES:
           tessType = TRIANGLES;
           break;
         }
       }
 
       public void end() {
         switch (tessType) {
         case TRIANGLE_FAN:
           for (int i = 1; i < tessCount - 1; i++) {
             addIndex(0);
             addIndex(i);
             addIndex(i + 1);
             if (calcNormals) calcTriNormal(0, i, i + 1);
           }
           break;
         case TRIANGLE_STRIP:
           for (int i = 1; i < tessCount - 1; i++) {            
             if (i % 2 == 0) {
               addIndex(i + 1);
               addIndex(i);
               addIndex(i - 1);              
               if (calcNormals) calcTriNormal(i + 1, i, i - 1);
             } else {
               addIndex(i - 1);
               addIndex(i);
               addIndex(i + 1);              
               if (calcNormals) calcTriNormal(i - 1, i, i + 1);
             }
           }
           break;
         case TRIANGLES:
           for (int i = 0; i < tessCount; i++) {
             addIndex(i);
           }
           if (calcNormals) {
             for (int tr = 0; tr < tessCount / 3; tr++) {
               int i0 = 3 * tr + 0;
               int i1 = 3 * tr + 1;
               int i2 = 3 * tr + 2;
               calcTriNormal(i0, i1, i2);
             }
           }
           break;
         }
       }
 
       protected void addIndex(int tessIdx) {
         if (tess.fillVertexCount < PGL.MAX_TESS_INDICES) {
           tess.addFillIndex(tessFirst + tessIdx);
         } else {
           throw new RuntimeException("P3D: the tessellator is generating too many indices, reduce complexity of shape.");
         }
       }
 
       protected void calcTriNormal(int tessIdx0, int tessIdx1, int tessIdx2) {
         tess.calcFillNormal(tessFirst + tessIdx0, tessFirst + tessIdx1, tessFirst + tessIdx2);
       }
 
       public void vertex(Object data) {
         if (data instanceof double[]) {
           double[] d = (double[]) data;
           int l = d.length;
           if (l < 25) {
             throw new RuntimeException("TessCallback vertex() data is not of length 25");
           }
 
           if (tess.fillVertexCount < PGL.MAX_TESS_VERTICES) {
 
             // Combining individual rgba components back into int color values
             int fcolor = ((int) d[ 3] << 24) | ((int) d[ 4] << 16) | ((int) d[ 5] << 8) | (int) d[ 6];
             int acolor = ((int) d[12] << 24) | ((int) d[13] << 16) | ((int) d[14] << 8) | (int) d[15];
             int scolor = ((int) d[16] << 24) | ((int) d[17] << 16) | ((int) d[18] << 8) | (int) d[19];
             int ecolor = ((int) d[20] << 24) | ((int) d[21] << 16) | ((int) d[22] << 8) | (int) d[23];
             
             tess.addFillVertex((float) d[ 0],  (float) d[ 1], (float) d[ 2],
                                fcolor,
                                (float) d[ 7],  (float) d[ 8], (float) d[ 9],
                                (float) d[10], (float) d[11],
                                acolor, scolor, ecolor,
                                (float) d[24]);
 
             int nvert = (l - 25) / 2;
             if (0 < nvert) {
               int tessIdx = tess.fillVertexCount - 1;
               for (int n = 0; n < nvert; n++) {
                 int inIdx = (int) d[25 + 2 * n + 0];
                 float weight = (float) d[25 + 2 * n + 1];
                 in.addFillIndex(inIdx, tessIdx, weight);
               }
             }
             
             tessCount++;
           } else {
             throw new RuntimeException("P3D: the tessellator is generating too many vertices, reduce complexity of shape.");
           }
 
         } else {
           throw new RuntimeException("TessCallback vertex() data not understood");
         }
       }
 
       public void error(int errnum) {
         String estring = pgl.gluErrorString(errnum);
         PGraphics.showWarning("Tessellation Error: " + estring);
       }
 
       /**
        * Implementation of the GLU_TESS_COMBINE callback.
        * @param coords is the 3-vector of the new vertex
        * @param data is the vertex data to be combined, up to four elements.
        * This is useful when mixing colors together or any other
        * user data that was passed in to gluTessVertex.
        * @param weight is an array of weights, one for each element of "data"
        * that should be linearly combined for new values.
        * @param outData is the set of new values of "data" after being
        * put back together based on the weights. it's passed back as a
        * single element Object[] array because that's the closest
        * that Java gets to a pointer.
        */
       public void combine(double[] coords, Object[] data,
                           float[] weight, Object[] outData) {
         double[] vertex = new double[25 + 8];
         vertex[0] = coords[0];
         vertex[1] = coords[1];
         vertex[2] = coords[2];
 
         // Calculating the rest of the vertex parameters (color,
         // normal, texcoords) as the linear combination of the
         // combined vertices.
         for (int i = 3; i < 25; i++) {
           vertex[i] = 0;
           for (int j = 0; j < 4; j++) {
             double[] vertData = (double[])data[j];
             if (vertData != null) {
               vertex[i] += weight[j] * vertData[i];
             }
           }
         }
         
         // Adding the indices and weights of the 4 input vertices
         // used to construct this combined vertex.
         for (int j = 0; j < 4; j++) {
           double[] vertData = (double[])data[j];
           if (vertData != null) {
             vertex[25 + 2 * j + 0] = vertData[25];
             vertex[25 + 2 * j + 1] = weight[j];            
           }
         }
         
         // Normalizing normal vector, since the weighted
         // combination of normal vectors is not necessarily
         // normal.
         double sum = vertex[7] * vertex[7] +
                      vertex[8] * vertex[8] +
                      vertex[9] * vertex[9];
         double len = Math.sqrt(sum);
         vertex[7] /= len;
         vertex[8] /= len;
         vertex[9] /= len;
         
         outData[0] = vertex;
       }
     }
   }
 }
