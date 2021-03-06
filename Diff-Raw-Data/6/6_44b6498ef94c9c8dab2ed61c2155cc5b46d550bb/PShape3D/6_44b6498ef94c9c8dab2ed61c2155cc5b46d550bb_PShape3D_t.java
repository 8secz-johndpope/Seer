 /* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */
 
 /*
   Part of the Processing project - http://processing.org
 
   Copyright (c) 2011 Andres Colubri
   Copyright (c) 2010 Ben Fry and Casey Reas
 
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
 import processing.core.PGraphics;
 import processing.core.PImage;
 import processing.core.PMatrix;
 import processing.core.PMatrix2D;
 import processing.core.PMatrix3D;
 import processing.core.PShape;
 import processing.core.PVector;
 import processing.opengl.PGraphicsOpenGL.FillShader;
 import processing.opengl.PGraphicsOpenGL.InGeometry;
 import processing.opengl.PGraphicsOpenGL.IndexCache;
 import processing.opengl.PGraphicsOpenGL.LineShader;
 import processing.opengl.PGraphicsOpenGL.PointShader;
 import processing.opengl.PGraphicsOpenGL.TessGeometry;
 import processing.opengl.PGraphicsOpenGL.Tessellator;
 import java.io.BufferedReader;
 import java.nio.FloatBuffer;
 import java.nio.IntBuffer;
 import java.nio.ShortBuffer;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.HashSet;
 import java.util.Hashtable;
 
 // TODO:
 // 1) Complete the get/set API (getTextureU/V setTextureUV), set/get for fill, stroke, tint, ambient, etc.
 
 /**
  * This class holds a 3D model composed of vertices, normals, colors (per vertex) and 
  * texture coordinates (also per vertex). All this data is stored in Vertex Buffer Objects
  * (VBO) in GPU memory for very fast access.
  * OBJ loading implemented using code from Saito's OBJLoader library (http://code.google.com/p/saitoobjloader/)
  * and OBJReader from Ahmet Kizilay (http://www.openprocessing.org/visuals/?visualID=191). 
  * By Andres Colubri
  * 
  * 
  * Other formats to consider:
  * AMF: http://en.wikipedia.org/wiki/Additive_Manufacturing_File_Format
  * STL: http://en.wikipedia.org/wiki/STL_(file_format)
  * OFF: http://people.sc.fsu.edu/~jburkardt/data/off/off.html(file_format)
  * DXF: http://en.wikipedia.org/wiki/AutoCAD_DXF
  */
 public class PShape3D extends PShape {
   static protected final int TRANSLATE = 0;
   static protected final int ROTATE    = 1;
   static protected final int SCALE     = 2;
   static protected final int MATRIX    = 3;
   
   protected PGraphicsOpenGL pg;
   protected PGL pgl;
   protected PGL.Context context;      // The context that created this shape.
 
   protected PShape3D root;  
 
   // ........................................................
   
   // Input, tessellated geometry    
   
   protected InGeometry in;
   protected TessGeometry tess;
   protected Tessellator tessellator;
 
   // ........................................................
   
   // Texturing  
   
   protected HashSet<PImage> textures;
   protected PImage texture; 
   
   // ........................................................
   
   // OpenGL buffers  
   
   public int glFillVertexBufferID;
   public int glFillColorBufferID;
   public int glFillNormalBufferID;
   public int glFillTexCoordBufferID;
   public int glFillAmbientBufferID;
   public int glFillSpecularBufferID;
   public int glFillEmissiveBufferID;
   public int glFillShininessBufferID;
   public int glFillIndexBufferID;
   
   public int glLineVertexBufferID;
   public int glLineColorBufferID;
   public int glLineDirWidthBufferID;
   public int glLineIndexBufferID;  
   
   public int glPointVertexBufferID;
   public int glPointColorBufferID;
   public int glPointSizeBufferID;
   public int glPointIndexBufferID;  
 
   // ........................................................
   
   // Offsets for geometry aggregation and update.
   
   protected int fillVertCopyOffset;
   protected int fillIndCopyOffset;
   protected int lineVertCopyOffset;
   protected int lineIndCopyOffset;
   protected int pointVertCopyOffset;
   protected int pointIndCopyOffset;
 
   protected int fillIndexOffset;
   protected int fillVertexOffset;
   protected int fillVertexAbs;
   protected int fillVertexRel;  
   
   protected int lineIndexOffset;
   protected int lineVertexOffset;
   protected int lineVertexAbs;
   protected int lineVertexRel;
   
   protected int pointIndexOffset;
   protected int pointVertexOffset;
   protected int pointVertexAbs;
   protected int pointVertexRel;
     
   // ........................................................
   
   // State/rendering flags  
   
   protected boolean tessellated;
   protected boolean needBufferInit;
   
   protected boolean isSolid;
   protected boolean isClosed;
   
   protected boolean openContour = false;
   protected boolean breakShape = false;
   protected boolean shapeEnded = false;
 
   protected boolean hasFill;
   protected boolean hasLines;
   protected boolean hasPoints;
   
   protected int prevMode;
   
   // ........................................................
   
   // Modes inherited from renderer  
   
   protected int textureMode;
   protected int rectMode;
   protected int ellipseMode;
   protected int shapeMode;
   protected int imageMode;
 
   // ........................................................
   
   // Geometric transformations  
   
   protected PMatrix transform;
   protected boolean cacheTransformations;
   protected boolean childHasMatrix;    
   
   // ........................................................
   
   // Normals
   
   protected float normalX, normalY, normalZ;
 
   // normal calculated per triangle
   static protected final int NORMAL_MODE_AUTO = 0;
   // one normal manually specified per shape
   static protected final int NORMAL_MODE_SHAPE = 1;
   // normals specified for each shape vertex
   static protected final int NORMAL_MODE_VERTEX = 2;
 
   // Current mode for normals, one of AUTO, SHAPE, or VERTEX
   protected int normalMode;  
     
   // ........................................................
   
   // Modification caches  
   
   // The caches are used to copy the data from each modified child shape 
   // to a contiguous array that will be then copied to the VBO in the root shape.
   protected VertexCache fillVerticesCache;
   protected VertexCache fillColorsCache;
   protected VertexCache fillNormalsCache;
   protected VertexCache fillTexCoordsCache;
   protected VertexCache fillAmbientCache;
   protected VertexCache fillSpecularCache;
   protected VertexCache fillEmissiveCache;
   protected VertexCache fillShininessCache;
   
   protected VertexCache lineVerticesCache;
   protected VertexCache lineColorsCache;
   protected VertexCache lineAttributesCache;  
 
   protected VertexCache pointVerticesCache;
   protected VertexCache pointColorsCache;
   protected VertexCache pointAttributesCache;
   
   protected boolean modifiedFillVertices;
   protected boolean modifiedFillColors;
   protected boolean modifiedFillNormals;
   protected boolean modifiedFillTexCoords;  
   protected boolean modifiedFillAmbient;
   protected boolean modifiedFillSpecular;
   protected boolean modifiedFillEmissive;
   protected boolean modifiedFillShininess;
   
   protected boolean modifiedLineVertices;
   protected boolean modifiedLineColors;
   protected boolean modifiedLineAttributes;  
 
   protected boolean modifiedPointVertices;
   protected boolean modifiedPointColors;
   protected boolean modifiedPointAttributes;    
     
   // ........................................................
   
   // Bezier and Catmull-Rom curves
   
   protected int bezierDetail = 20;
   protected int curveDetail = 20;
   protected float curveTightness = 0;  
   
   
   public PShape3D(PApplet parent, int family) {
     pg = (PGraphicsOpenGL)parent.g;
     pgl = pg.pgl;
     context = pgl.createEmptyContext();
     
     prevMode = mode = DYNAMIC;
     
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
     
     this.tessellator = pg.tessellator;
     this.family = family;    
     this.root = this;
     this.parent = null;
     this.tessellated = false;
     
     if (family == GEOMETRY || family == PRIMITIVE || family == PATH) {
       in = pg.newInGeometry(PGraphicsOpenGL.RETAINED);      
     }    
     
     // Modes are retrieved from the current values in the renderer.
     textureMode = pg.textureMode;    
     rectMode = pg.rectMode;
     ellipseMode = pg.ellipseMode;
     shapeMode = pg.shapeMode;
     imageMode = pg.imageMode;
             
     colorMode(pg.colorMode, pg.colorModeX, pg.colorModeY, pg.colorModeZ, pg.colorModeA);
     
     // Initial values for fill, stroke and tint colors are also imported from the renderer.
     // This is particular relevant for primitive shapes, since is not possible to set 
     // their color separately when creating them, and their input vertices are actually
     // generated at rendering time, by which the color configuration of the renderer might
     // have changed.
     fill = pg.fill;
     fillColor = pg.fillColor;
     
     stroke = pg.stroke;      
     strokeColor = pg.strokeColor;     
     strokeWeight = pg.strokeWeight;    
     
     tint = pg.tint;  
     tintColor = pg.tintColor;
 
     ambientColor = pg.ambientColor;  
     specularColor = pg.specularColor;  
     emissiveColor = pg.emissiveColor;
     shininess = pg.shininess;
     
     normalX = normalY = 0; 
     normalZ = 1;
     
     normalMode = NORMAL_MODE_AUTO;
     
     cacheTransformations = pg.hintEnabled(ENABLE_TRANSFORM_CACHE);
     
     if (family == GROUP) {
       // GROUP shapes are always marked as ended.
       shapeEnded = true;
     }
   }
 
   
   public void setMode(int mode) {
     if (this.mode == STATIC && mode == DYNAMIC) {
       // Marking the shape as not tessellated, this
       // will trigger a tessellation next time the
       // shape is drawn or modified, which will bring
       // back all the tess objects.      
       tessellated = false;
     }
     super.setMode(mode);
   }
   
   
   public void addChild(PShape child) {
     if (child instanceof PShape3D) {
       if (family == GROUP) {
         PShape3D c3d = (PShape3D)child;
         
         super.addChild(c3d);
         c3d.updateRoot(root);
         root.tessellated = false;
         tessellated = false;
         
         if (!cacheTransformations && c3d.hasMatrix()) {
           setChildHasMatrix(true);
         }        
         
         if (c3d.texture != null) {
           addTexture(c3d.texture);
         }        
       } else {
         PGraphics.showWarning("Cannot add child shape to non-group shape.");
       }
     } else {
       PGraphics.showWarning("Shape must be 3D to be added to the group.");
     }
   }
   
   
   public void updateRoot(PShape root) {
     this.root = (PShape3D) root;
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D)children[i];
         child.updateRoot(root);
       }
     }
   }      
   
   
   protected void finalize() throws Throwable {
     try {
       finalizeFillBuffers();  
       finalizeLineBuffers();
       finalizePointBuffers();
     } finally {
       super.finalize();
     }
   }
   
   
   protected void finalizeFillBuffers() {
     if (glFillVertexBufferID != 0) {    
       pg.finalizeVertexBufferObject(glFillVertexBufferID, context.code());   
     }    
     
     if (glFillColorBufferID != 0) {    
       pg.finalizeVertexBufferObject(glFillColorBufferID, context.code());   
     }    
 
     if (glFillNormalBufferID != 0) {    
       pg.finalizeVertexBufferObject(glFillNormalBufferID, context.code());   
     }     
 
     if (glFillTexCoordBufferID != 0) {    
       pg.finalizeVertexBufferObject(glFillTexCoordBufferID, context.code());   
     }    
 
     if (glFillAmbientBufferID != 0) {    
       pg.finalizeVertexBufferObject(glFillAmbientBufferID, context.code());   
     }    
     
     if (glFillSpecularBufferID != 0) {    
       pg.finalizeVertexBufferObject(glFillSpecularBufferID, context.code());   
     }    
 
     if (glFillEmissiveBufferID != 0) {    
       pg.finalizeVertexBufferObject(glFillEmissiveBufferID, context.code());   
     }     
 
     if (glFillShininessBufferID != 0) {    
       pg.finalizeVertexBufferObject(glFillShininessBufferID, context.code());   
     }    
     
     if (glFillIndexBufferID != 0) {    
       pg.finalizeVertexBufferObject(glFillIndexBufferID, context.code());   
     }   
   }
   
   
   protected void finalizeLineBuffers() {
     if (glLineVertexBufferID != 0) {    
       pg.finalizeVertexBufferObject(glLineVertexBufferID, context.code());   
     }    
     
     if (glLineColorBufferID != 0) {    
       pg.finalizeVertexBufferObject(glLineColorBufferID, context.code());   
     }    
 
     if (glLineDirWidthBufferID != 0) {    
       pg.finalizeVertexBufferObject(glLineDirWidthBufferID, context.code());   
     }    
     
     if (glLineIndexBufferID != 0) {    
       pg.finalizeVertexBufferObject(glLineIndexBufferID, context.code());   
     }  
   }  
   
   
   protected void finalizePointBuffers() {
     if (glPointVertexBufferID != 0) {    
       pg.finalizeVertexBufferObject(glPointVertexBufferID, context.code());   
     }    
     
     if (glPointColorBufferID != 0) {    
       pg.finalizeVertexBufferObject(glPointColorBufferID, context.code());   
     }    
 
     if (glPointSizeBufferID != 0) {    
       pg.finalizeVertexBufferObject(glPointSizeBufferID, context.code());   
     }    
     
     if (glPointIndexBufferID != 0) {    
       pg.finalizeVertexBufferObject(glPointIndexBufferID, context.code());   
     }  
   }
 
   
   ///////////////////////////////////////////////////////////  
   
   //
   
   // Query methods  
   
   
   public float getWidth() {
     PVector min = new PVector(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY); 
     PVector max = new PVector(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
     if (shapeEnded) {
       getVertexMin(min);
       getVertexMin(max);
     }    
     width = max.x - min.x;
     return width;
   }
 
   
   public float getHeight() {
     PVector min = new PVector(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY); 
     PVector max = new PVector(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
     if (shapeEnded) {
       getVertexMin(min);
       getVertexMin(max);
     }    
     width = max.y - min.y;
     return height;
   }
 
   
   public float getDepth() {
     PVector min = new PVector(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY); 
     PVector max = new PVector(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
     if (shapeEnded) {
       getVertexMin(min);
       getVertexMin(max);
     }    
     width = max.z - min.z;    
     return depth;
   }  
 
   
   public PVector getCenter() {
     PVector center = new PVector();
     int count = 0;
     if (shapeEnded) {
       count = getVertexSum(center, count);
       if (0 < count) {
         center.x /= count;
         center.y /= count;
         center.z /= count;
       }    
     }
     return center;
   }  
   
   
   protected void getVertexMin(PVector min) {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];
         child.getVertexMin(min);
       }
     } else {      
       if (tessellated) {
         tess.getVertexMin(min);
       } else {
         in.getVertexMin(min);
       }
     }
   }
 
   
   protected void getVertexMax(PVector max) {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];
         child.getVertexMax(max);
       }
     } else {      
       if (tessellated) {
         tess.getVertexMax(max);
       } else {
         in.getVertexMax(max);
       }
     }
   }  
   
   
   protected int getVertexSum(PVector sum, int count) {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];
         count += child.getVertexSum(sum, count);
       }
     } else {      
       if (tessellated) {
         count += tess.getVertexSum(sum);
       } else {
         count += in.getVertexSum(sum);
       }
     }
     return count;
   }
   
   
   ///////////////////////////////////////////////////////////  
   
   //
   
   // Drawing methods  
   
   
   public void textureMode(int mode) {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.textureMode(mode);        
       }         
     } else {    
       textureMode = mode;
     }
   }
 
   
   public void texture(PImage tex) {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.texture(tex);        
       }      
     } else {
       PImage tex0 = texture;
       texture = tex;
       if (tex0 != tex && parent != null) {
         ((PShape3D)parent).removeTexture(tex);
       }      
       if (parent != null) {
         ((PShape3D)parent).addTexture(texture);
       }
     }        
   }
 
   
   public void noTexture() {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.noTexture();        
       }
     } else {
       PImage tex0 = texture;
       texture = null;
       if (tex0 != null && parent != null) {
         ((PShape3D)parent).removeTexture(tex0);
       }      
     }
   }  
 
   
   protected void addTexture(PImage tex) {
     if (textures == null) {
       textures = new HashSet<PImage>();      
     }
     textures.add(tex);
     if (parent != null) {
       ((PShape3D)parent).addTexture(tex);
     }   
   }
   
   
   protected void removeTexture(PImage tex) {
     if (textures != null) {
       boolean childHasIt = false;
       
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         if (child.hasTexture(tex)) {
           childHasIt = true;
           break;
         }
       }
       
       if (!childHasIt) {
         textures.remove(tex);
         if (textures.size() == 0) {
           textures = null;
         }
       }
     }
   }
   
   
   protected boolean hasTexture(PImage tex) {
     if (family == GROUP) {
       return textures != null && textures.contains(tex);  
     } else {
       return texture == tex;
     }
   }
   
   
   public void solid(boolean solid) {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.solid(solid);
       }
     } else {
       isSolid = solid;  
     }    
   }
   
   
   public void beginContour() {
     if (family == GROUP) {      
       PGraphics.showWarning("Cannot begin contour in GROUP shapes");
       return;
     }
     
     if (openContour) {
       PGraphics.showWarning("P3D: Already called beginContour().");
       return;
     }    
     openContour = true;    
   }
   
   
   public void endContour() {
     if (family == GROUP) {      
       PGraphics.showWarning("Cannot end contour in GROUP shapes");
       return;
     }
     
     if (!openContour) {
       PGraphics.showWarning("P3D: Need to call beginContour() first.");
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
     vertexImpl(x, y, z, u, v);  
   }  
   
   
   protected void vertexImpl(float x, float y, float z, float u, float v) {
     if (family == GROUP) {      
       PGraphics.showWarning("Cannot add vertices to GROUP shape");
       return;
     }
 
     boolean textured = texture != null;
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
     
     if (texture != null && textureMode == IMAGE) {
       u = PApplet.min(1, u / texture.width);
       v = PApplet.min(1, v / texture.height);
     }
         
     int scolor = 0x00;
     float sweight = 0;
     if (stroke) {
       scolor = strokeColor;
       sweight = strokeWeight;
     }    
     
     in.addVertex(x, y, z, 
                  fcolor, 
                  normalX, normalY, normalZ,
                  u, v, 
                  scolor, sweight,
                  ambientColor, specularColor, emissiveColor, shininess,
                  vertexCode());    
     
     root.tessellated = false;
     tessellated = false;  
   }
   
   
   protected int vertexCode() {
     int code = VERTEX;
     if (breakShape) {
       code = BREAK;
       breakShape = false;
     }    
     return code;
   }
   
   
   public void normal(float nx, float ny, float nz) {
     if (family == GROUP) {      
       PGraphics.showWarning("Cannot set normal in GROUP shape");
       return;
     }
     
     normalX = nx;
     normalY = ny;
     normalZ = nz;
     
     // if drawing a shape and the normal hasn't been set yet,
     // then we need to set the normals for each vertex so far
     if (normalMode == NORMAL_MODE_AUTO) {
       // One normal per begin/end shape
       normalMode = NORMAL_MODE_SHAPE;
     } else if (normalMode == NORMAL_MODE_SHAPE) {
       // a separate normal for each vertex
       normalMode = NORMAL_MODE_VERTEX;
     } 
   }
 
   
   public void end() {
     end(OPEN);
   }  
 
   
   public void end(int mode) { 
     if (family == GROUP) {      
       PGraphics.showWarning("Cannot end GROUP shape");
       return;
     }
     
     // Input arrays are trimmed since they are expanded by doubling their old size,
     // which might lead to arrays larger than the vertex counts.
     in.trim();
     
     isClosed = mode == CLOSE;    
     root.tessellated = false;
     tessellated = false;
     shapeEnded = true;
   }  
   
   
   public void setParams(float[] source) {
     if (family != PRIMITIVE) {      
       PGraphics.showWarning("Parameters can only be set to PRIMITIVE shapes");
       return;
     }
     
     super.setParams(source);
     root.tessellated = false;
     tessellated = false;
     shapeEnded = true;
   }
   
   
   public void setPath(float[][] coords) {
     setPath(coords, null, OPEN);
   }
 
   
   public void setPath(float[][] coords, int mode) {
     setPath(coords, null, mode);
   }  
   
   
   public void setPath(float[][] coords, int[] codes) {
     setPath(coords, codes, OPEN);
   }
   
   
   public void setPath(float[][] coords, int[] codes, int mode) {
     if (family != PATH) {      
       PGraphics.showWarning("Vertex coordinates and codes can only be set to PATH shapes");
       return;
     }
     
     super.setPath(coords, codes);
     isClosed = mode == CLOSE;
     root.tessellated = false;
     tessellated = false;
     shapeEnded = true;    
   }
 
   
   //////////////////////////////////////////////////////////////
 
   // STROKE CAP/JOIN/WEIGHT
 
   
   public void strokeWeight(float weight) {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.strokeWeight(weight);
       }
     } else {
       float prevStrokeWeight = strokeWeight; 
       strokeWeight = weight;
       updateStrokeWeight(prevStrokeWeight);
     }    
   }
   
   protected void updateStrokeWeight(float prevStrokeWeight) {
     if (shapeEnded && tessellated && (0 < tess.lineVertexCount || 0 < tess.pointVertexCount)) {      
       float resizeFactor = strokeWeight / prevStrokeWeight;
       Arrays.fill(in.sweights, 0, in.vertexCount, strokeWeight);         
       if (0 < tess.lineVertexCount) {
         for (int i = 0; i < tess.lineVertexCount; i++) {
           tess.lineDirWidths[4 * i + 3] *= resizeFactor;
         }
         modifiedLineAttributes = true;
         modified();   
       }      
       if (0 < tess.pointVertexCount) {
         for (int i = 0; i < tess.pointVertexCount; i++) {
           tess.pointSizes[2 * i + 0] *= resizeFactor;
           tess.pointSizes[2 * i + 1] *= resizeFactor;
         }
         modifiedPointAttributes = true;
         modified();            
       }            
     }    
   }
 
 
   public void strokeJoin(int join) {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.strokeJoin(join);
       }
     } else {
       strokeJoin = join;
     }        
   }
 
 
   public void strokeCap(int cap) {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.strokeCap(cap);
       }
     } else {
       strokeCap = cap;
     }    
   }
     
   
   //////////////////////////////////////////////////////////////
 
   // FILL COLOR
 
   
   public void noFill() {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.noFill();        
       }      
     } else {
       fill = false;
       fillColor = 0x0;
       updateFillColor();      
     }
   }
 
   
   public void fill(int rgb) {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.fill(rgb);        
       }      
     } else {
       colorCalc(rgb);
       fillFromCalc();        
     }
   }
 
   
   public void fill(int rgb, float alpha) {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.fill(rgb, alpha);        
       }      
     } else {
       colorCalc(rgb, alpha);
       fillFromCalc();
     }    
   }
 
   
   public void fill(float gray) {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.fill(gray);        
       }      
     } else {
       colorCalc(gray);
       fillFromCalc();      
     }
   }
 
   
   public void fill(float gray, float alpha) {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.fill(gray, alpha);        
       }      
     } else {
       colorCalc(gray, alpha);
       fillFromCalc();
     }    
   }
 
   
   public void fill(float x, float y, float z) {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.fill(x, y, z);        
       }      
     } else {
       colorCalc(x, y, z);
       fillFromCalc();
     }    
   }
 
   
   public void fill(float x, float y, float z, float a) {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.fill(x, y, z, a);        
       }      
     } else {
       colorCalc(x, y, z, a);
       fillFromCalc();
     }    
   }
   
 
   protected void fillFromCalc() {
     fill = true;
     fillColor = calcColor;
     updateFillColor();  
   }
 
   
   protected void updateFillColor() {
     if (shapeEnded && tessellated && 0 < tess.fillVertexCount && texture == null) {
       Arrays.fill(in.colors, 0, in.vertexCount, PGL.javaToNativeARGB(fillColor));
       Arrays.fill(tess.fillColors, 0, tess.fillVertexCount, PGL.javaToNativeARGB(fillColor));
       modifiedFillColors = true;
       modified();  
     }
   }
   
     
   //////////////////////////////////////////////////////////////
 
   // STROKE COLOR 
   
   
   public void noStroke() {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.noStroke();        
       }      
     } else {
       stroke = false;
       strokeColor = 0x0;
       updateStrokeColor();      
     }  
   }
   
   
   public void stroke(int rgb) {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.stroke(rgb);        
       }      
     } else {
       colorCalc(rgb);
       strokeFromCalc();
     }    
   }
   
   
   public void stroke(int rgb, float alpha) {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.stroke(rgb, alpha);        
       }      
     } else {
       colorCalc(rgb, alpha);
       strokeFromCalc();      
     }
   }
 
   
   public void stroke(float gray) {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.stroke(gray);        
       }      
     } else {
       colorCalc(gray);
       strokeFromCalc();
     }    
   }
 
   
   public void stroke(float gray, float alpha) {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.stroke(gray, alpha);        
       }      
     } else {
       colorCalc(gray, alpha);
       strokeFromCalc();      
     }
   }
 
   
   public void stroke(float x, float y, float z) {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.stroke(x, y, z);        
       }      
     } else {
       colorCalc(x, y, z);
       strokeFromCalc();      
     }
   }
 
   
   public void stroke(float x, float y, float z, float alpha) {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.stroke(x, y, z, alpha);        
       }      
     } else {
       colorCalc(x, y, z, alpha);
       strokeFromCalc();      
     }
   }
   
   
   protected void strokeFromCalc() {
     stroke = true;
     strokeColor = calcColor;
     updateStrokeColor();  
   }
 
   
   protected void updateStrokeColor() {
     if (shapeEnded && tessellated && (0 < tess.lineVertexCount || 0 < tess.pointVertexCount)) {
       Arrays.fill(in.scolors, 0, in.vertexCount, PGL.javaToNativeARGB(strokeColor));      
       if (0 < tess.lineVertexCount) {
         Arrays.fill(tess.lineColors, 0, tess.lineVertexCount, PGL.javaToNativeARGB(strokeColor));
         modifiedLineColors = true;
         modified();         
       }      
       if (0 < tess.pointVertexCount) {
         Arrays.fill(tess.pointColors, 0, tess.pointVertexCount, PGL.javaToNativeARGB(strokeColor));
         modifiedPointColors = true;
         modified();            
       }            
     }    
   }  
 
  
   //////////////////////////////////////////////////////////////
 
   // TINT COLOR 
   
   
   public void noTint() {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.noTint();        
       }      
     } else {
       tint = false;
       tintColor = 0x0;
       updateTintColor();      
     }   
   }  
   
   
   public void tint(int rgb) {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.tint(rgb);        
       }      
     } else {
       colorCalc(rgb);
       tintFromCalc();      
     }
   }  
   
   
   public void tint(int rgb, float alpha) {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.tint(rgb, alpha);        
       }      
     } else {
       colorCalc(rgb, alpha);
       tintFromCalc();      
     }
   }
   
   
   public void tint(float gray) {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.tint(gray);        
       }      
     } else {
       colorCalc(gray);
       tintFromCalc();      
     }    
   }
   
   
   public void tint(float gray, float alpha) {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.tint(gray, alpha);        
       }      
     } else {
       colorCalc(gray, alpha);
       tintFromCalc();      
     }    
   }
   
 
   public void tint(float x, float y, float z) {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.tint(x, y, z);        
       }      
     } else {
       colorCalc(x, y, z);
       tintFromCalc();      
     }    
   }
   
   
   public void tint(float x, float y, float z, float alpha) {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.tint(x, y, z, alpha);        
       }      
     } else {
       colorCalc(x, y, z, alpha);
       tintFromCalc();      
     }        
   }  
   
   
   protected void tintFromCalc() {
     tint = true;
     tintColor = calcColor;
     updateTintColor();  
   }  
   
   
   protected void updateTintColor() {    
     if (shapeEnded && tessellated && 0 < tess.fillVertexCount && texture != null) {
       Arrays.fill(in.colors, 0, in.vertexCount, PGL.javaToNativeARGB(tintColor));
       Arrays.fill(tess.fillColors, 0, tess.fillVertexCount, PGL.javaToNativeARGB(tintColor));
       modifiedFillColors = true;
       modified();  
     }
   }
   
   
   //////////////////////////////////////////////////////////////
 
   // AMBIENT COLOR
   
   
   public void ambient(int rgb) {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.ambient(rgb);        
       }      
     } else {
       colorCalc(rgb);
       ambientFromCalc();      
     }    
   }
 
 
   public void ambient(float gray) {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.ambient(gray);        
       }      
     } else {
       colorCalc(gray);
       ambientFromCalc();      
     }     
   }
 
 
   public void ambient(float x, float y, float z) {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.ambient(x, y, z);        
       }      
     } else {
       colorCalc(x, y, z);
       ambientFromCalc();      
     }      
   }
   
   
   protected void ambientFromCalc() {
     ambientColor = calcColor;
     updateAmbientColor();      
   }
   
 
   protected void updateAmbientColor() {    
     if (shapeEnded && tessellated && 0 < tess.fillVertexCount) {
       Arrays.fill(in.ambient, 0, in.vertexCount, PGL.javaToNativeARGB(ambientColor));
       Arrays.fill(tess.fillAmbient, 0, tess.fillVertexCount, PGL.javaToNativeARGB(ambientColor));      
       modifiedFillAmbient = true;
       modified();  
     }      
   }
   
   
   //////////////////////////////////////////////////////////////
 
   // SPECULAR COLOR  
   
 
   public void specular(int rgb) {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.specular(rgb);        
       }      
     } else {
       colorCalc(rgb);
       specularFromCalc();      
     }      
   }
 
 
   public void specular(float gray) {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.specular(gray);        
       }      
     } else {
       colorCalc(gray);
       specularFromCalc();      
     }     
   }
 
 
   public void specular(float x, float y, float z) {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.specular(x, y, z);        
       }      
     } else {
       colorCalc(x, y, z);
       specularFromCalc();      
     }      
   }
   
   
   protected void specularFromCalc() {
     specularColor = calcColor;
     updateSpecularColor();    
   }
 
   
   protected void updateSpecularColor() {
     if (shapeEnded && tessellated && 0 < tess.fillVertexCount) {
       Arrays.fill(in.specular, 0, in.vertexCount, PGL.javaToNativeARGB(specularColor));
       Arrays.fill(tess.fillSpecular, 0, tess.fillVertexCount, PGL.javaToNativeARGB(specularColor));    
       modifiedFillSpecular = true;
       modified();
     }
   }
   
   
   //////////////////////////////////////////////////////////////
 
   // EMISSIVE COLOR
   
   
   public void emissive(int rgb) {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.emissive(rgb);        
       }      
     } else {
       colorCalc(rgb);
       emissiveFromCalc();      
     }      
   }
 
 
   public void emissive(float gray) {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.emissive(gray);        
       }      
     } else {
       colorCalc(gray);
       emissiveFromCalc();      
     }     
   }
 
 
   public void emissive(float x, float y, float z) {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.emissive(x, y, z);        
       }      
     } else {
       colorCalc(x, y, z);
       emissiveFromCalc();      
     }      
   }
   
   
   protected void emissiveFromCalc() {
     emissiveColor = calcColor;
     updateEmissiveColor();     
   }
 
   
   protected void updateEmissiveColor() {   
     if (shapeEnded && tessellated && 0 < tess.fillVertexCount) {
       Arrays.fill(in.emissive, 0, in.vertexCount, PGL.javaToNativeARGB(emissiveColor));
       Arrays.fill(tess.fillEmissive, 0, tess.fillVertexCount, PGL.javaToNativeARGB(emissiveColor));    
       modifiedFillEmissive = true;
       modified();
     }    
   }
   
   
   //////////////////////////////////////////////////////////////
 
   // SHININESS  
   
   
   public void shininess(float shine) {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.shininess(shine);        
       }      
     } else {
       shininess = shine;
       updateShininessFactor();    
     }       
   }
   
   
   protected void updateShininessFactor() {
     if (shapeEnded && tessellated && 0 < tess.fillVertexCount) {
       Arrays.fill(in.shininess, 0, in.vertexCount, shininess);
       Arrays.fill(tess.fillShininess, 0, tess.fillVertexCount, shininess);    
       modifiedFillShininess = true;
       modified();    
     }
   }
   
   
   ///////////////////////////////////////////////////////////  
   
   //
   
   // Geometric transformations
   
   
   public void translate(float tx, float ty) {
     transform(TRANSLATE, tx, ty);
   }
   
   
   public void translate(float tx, float ty, float tz) {
     transform(TRANSLATE, tx, ty, tz);
   }
   
   
   public void rotate(float angle) {
     transform(ROTATE, angle);
   }
   
   
   public void rotate(float angle, float v0, float v1, float v2) {
     transform(ROTATE, v0, v1, v2);
   }
   
   
   public void scale(float s) {
     transform(SCALE, s, s);
   }
 
 
   public void scale(float x, float y) {
     transform(SCALE, x, y);
   }
 
 
   public void scale(float x, float y, float z) {
     transform(SCALE, x, y, z);
   }  
 
 
   public void applyMatrix(float n00, float n01, float n02,
                           float n10, float n11, float n12) {
     transform(MATRIX, n00, n01, n02,
                       n10, n11, n12);
   }
 
 
   public void applyMatrix(float n00, float n01, float n02, float n03,
                           float n10, float n11, float n12, float n13,
                           float n20, float n21, float n22, float n23,
                           float n30, float n31, float n32, float n33) {
     transform(MATRIX, n00, n01, n02, n03,
                       n10, n11, n12, n13,
                       n20, n21, n22, n23,
                       n30, n31, n32, n33);
   }
   
   
   public void resetMatrix() {
     if (!cacheTransformations && parent != null && hasMatrix()) {
       ((PShape3D)parent).setChildHasMatrix(false);
     }    
     
     if (shapeEnded) {
       if (family == GROUP) {
         updateTessellation();
         
         for (int i = 0; i < childCount; i++) {
           PShape3D child = (PShape3D) children[i];
           child.resetMatrix();
         }
         if (matrix != null) {
           matrix.reset();
         }
       } else {
         if (cacheTransformations) {
           boolean res = matrix.invert();
           if (res) {
             if (tessellated) {
               // This will be ultimately handled by transformImpl(),
               // which will take care of setting the modified flags, etc.
               applyMatrix(matrix);
             }
             matrix.reset();
           } else {
             PGraphics.showWarning("The transformation matrix cannot be inverted");
           }
         } else {
           if (hasMatrix()) {
             matrix.reset();
           }          
         }
       }      
     }
   }
   
   
   protected void transform(int type, float... args) {
     int dimensions;
     if (type == ROTATE) {
       dimensions = args.length == 1 ? 2 : 3;
     } else if (type == MATRIX) {
       dimensions = args.length == 6 ? 2 : 3;
     } else {
       dimensions = args.length;
     }    
     transformImpl(type, dimensions, args);
   }
   
   
   protected void transformImpl(int type, int ncoords, float... args) {
     if (shapeEnded) {
 
       if (family == GROUP) {
         updateTessellation();
         
         if (cacheTransformations) {
           // The transformation will be passed down to the child shapes
           // which will in turn apply it to the tessellated vertices,
           // so effectively becoming "cached" into the geometry itself.
           for (int i = 0; i < childCount; i++) {
             PShape3D child = (PShape3D) children[i];
             child.transformImpl(type, ncoords, args);
           }
         } else {
           checkMatrix(ncoords);
           calcTransform(type, ncoords, args);
         }
       } else {
         // The tessellation is not updated for geometry/primitive shapes
         // because a common situation is shapes not still tessellated
         // but being transformed before adding them to the parent group
         // shape. If each shape is tessellated individually, then the process
         // is significantly slower than tessellating all the geometry in a single 
         // batch when calling tessellate() on the root shape.
         
         checkMatrix(ncoords);
         if (cacheTransformations) {
           calcTransform(type, ncoords, args);
           if (tessellated) {
             applyTransform(ncoords);
             modified();
             if (0 < tess.fillVertexCount) {
               modifiedFillVertices = true;  
               modifiedFillNormals = true; 
             }        
             if (0 < tess.lineVertexCount) {
               modifiedLineVertices = true;
               modifiedLineAttributes = true;
             }
             if (0 < tess.pointVertexCount) {
               modifiedPointVertices = true;        
             }            
           }
         } else {
           // The transformation will be applied in draw().
           calcTransform(type, ncoords, args);          
         }
       }
       
       if (!cacheTransformations && parent != null && hasMatrix()) {
         // Making sure that the parent shapes (if any) know that
         // this shape has a transformation matrix to be applied
         // in draw().
         ((PShape3D)parent).setChildHasMatrix(true);
       }
     }        
   }
   
   
   protected void calcTransform(int type, int dimensions, float... args) {
     if (transform == null) {
       if (dimensions == 2) {
         transform = new PMatrix2D();
       } else {
         transform = new PMatrix3D();
       }
     } else {
       transform.reset();
     }
       
     switch (type) {
     case TRANSLATE:
       if (dimensions == 3) {
         transform.translate(args[0], args[1], args[2]);
       } else {
         transform.translate(args[0], args[1]);
       }
       break;
     case ROTATE:
       if (dimensions == 3) {
         transform.rotate(args[0], args[1], args[2], args[3]);
       } else {
         transform.rotate(args[0]);
       }
       break;
     case SCALE:
       if (dimensions == 3) {
         transform.scale(args[0], args[1], args[2]);
       } else {
         transform.scale(args[0], args[1]);
       }
       break;
     case MATRIX:
       if (dimensions == 3) {
         transform.set(args[ 0], args[ 1], args[ 2], args[ 3],
                       args[ 4], args[ 5], args[ 6], args[ 7],
                       args[ 8], args[ 9], args[10], args[11],
                       args[12], args[13], args[14], args[15]);
       } else {
         transform.set(args[0], args[1], args[2],
                       args[3], args[4], args[5]);
       }
       break;      
     }   
     matrix.apply(transform);
   }
   
   
   protected void applyTransform(int dimensions) {
     if (dimensions == 3) {
       tess.applyMatrix((PMatrix3D) transform);
     } else {
       tess.applyMatrix((PMatrix2D) transform);
     }
   }
   
   
   protected void setChildHasMatrix(boolean value) {
     childHasMatrix = value;
     if (parent != null) {
       ((PShape3D)parent).setChildHasMatrix(value);
     }
   }
   
   
   protected boolean hasMatrix() {
     return matrix != null || childHasMatrix; 
   }  
   
   
   ///////////////////////////////////////////////////////////  
   
   //
   
   // Bezier curves 
   
   
   public void bezierDetail(int detail) {
     bezierDetail = detail;
     pg.bezierDetail(detail);
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
     in.setColors(fillColor, strokeColor, strokeWeight,
                  ambientColor, specularColor, emissiveColor, shininess);
     in.setNormal(normalX, normalY, normalZ);
     in.addBezierVertex(x2, y2, z2,
                        x3, y3, z3,
                        x4, y4, z4,
                        fill, stroke, bezierDetail, vertexCode(), kind);     
   }
   
   
   public void quadraticVertex(float cx, float cy,
                               float x3, float y3) {
     quadraticVertex(cx, cy, 0,
                     x3, y3, 0);
   }  
   
   
   public void quadraticVertex(float cx, float cy, float cz,
                               float x3, float y3, float z3) {
     in.setColors(fillColor, strokeColor, strokeWeight,
                  ambientColor, specularColor, emissiveColor, shininess);
     in.setNormal(normalX, normalY, normalZ);    
     in.addQuadraticVertex(cx, cy, cz,
                           x3, y3, z3,
                           fill, stroke, bezierDetail, vertexCode(), kind); 
   }
   
   
   ///////////////////////////////////////////////////////////  
   
   //
   
   // Catmull-Rom curves
 
   
   public void curveDetail(int detail) {
     curveDetail = detail;
     pg.curveDetail(detail);
   }
   
   
   public void curveTightness(float tightness) {
     curveTightness = tightness;
     pg.curveTightness(tightness);
   }  
   
   
   public void curveVertex(float x, float y) {
     curveVertex(x, y, 0);
   }  
 
   
   public void curveVertex(float x, float y, float z) {
     in.setColors(fillColor, strokeColor, strokeWeight,
                  ambientColor, specularColor, emissiveColor, shininess);
     in.setNormal(normalX, normalY, normalZ);
     in.addCurveVertex(x, y, z,
                       fill, stroke, curveDetail, vertexCode(), kind); 
   }
   
   
   ///////////////////////////////////////////////////////////  
   
   //
   
   // Setters/getters of individual vertices
   
 
   public int getVertexCount() {
     if (family == GROUP) {
       PGraphics.showWarning("GROUP shapes don't have any vertices");
       return 0;
     }
     
     return in.vertexCount;  
   }
   
   
   public float[] getVertex(int index) {
     if (family == GROUP) {
       PGraphics.showWarning("GROUP shapes don't have any vertices");
       return null;
     }
     
     float[] data = new float[VERTEX_FIELD_COUNT];
         
     
   data[X] = in.vertices[3 * index + 0];
   data[Y] = in.vertices[3 * index + 1];
   data[Z] = in.vertices[3 * index + 2];
 
   
 //  int fa = (in.colors[i] >> 24) & 0xFF;
 //  int fr = (in.colors[i] >> 16) & 0xFF;
 //  int fg = (in.colors[i] >>  8) & 0xFF;
 //  int fb = (in.colors[i] >>  0) & 0xFF;
   
   
 //  in.colors[3 * index + 2];
   
   /*
   data[R] = 3;  // actual rgb, after lighting
   data[G] = 4;  // fill stored here, transform in place
   data[B] = 5;  // TODO don't do that anymore (?)
   data[A] = 6;
 
   data[U] = 7; // texture
   data[V] = 8;
 
   data[NX] = 9; // normal
   data[NY] = 10;
   data[NZ] = 11;
 
   // stroke
 
   data[SR] = 13;
   data[SG] = 14;
   data[SB] = 15;
   data[SA] = 16;
 
   data[SW = 17;
 
   // material properties
   data[AR] = 25;
   data[AG] = 26;
   data[AB] = 27;
 
   data[SPR] = 28;
   data[SPG] = 29;
   data[SPB] = 30;
 
   data[SHINE] = 31;
 
   data[ER] = 32;
   data[EG] = 33;
   data[EB] = 34;
 
   data[HAS_NORMAL] = 36;
      */
     
     
     return data;
   }
   
   
 //  public PVector getVertex(int index) {
 //    return getVertex(index, null);
 //  }  
   
   
   public PVector getVertex(int index, PVector vec) {
     if (family == GROUP) {
       PGraphics.showWarning("GROUP shapes don't have any vertices");
       return null;
     }
     updateTessellation();
     
     if (vec == null) {
       vec = new PVector();
     }
     vec.x = in.vertices[3 * index + 0];
     vec.y = in.vertices[3 * index + 1];
     vec.z = in.vertices[3 * index + 2];
     return vec;
   }
   
   
   public float getVertexX(int index) {
     if (family == GROUP) {
       PGraphics.showWarning("GROUP shapes don't have any vertices");
       return 0;
     }
     updateTessellation();
     
     return in.vertices[3 * index + 0];
   }
   
   
   public float getVertexY(int index) {
     if (family == GROUP) {
       PGraphics.showWarning("GROUP shapes don't have any vertices");
       return 0;
     }
     updateTessellation();
     
     return in.vertices[3 * index + 1];
   }
   
   
   public float getVertexZ(int index) {
     if (family == GROUP) {
       PGraphics.showWarning("GROUP shapes don't have any vertices");
       return 0;
     }
     updateTessellation();
     
     return in.vertices[3 * index + 2];
   }  
   
   
   public void setVertex(int index, float x, float y) {
     setVertex(index, x, y, 0);
   }
   
   
   public void setVertex(int index, float x, float y, float z) {
     if (family == GROUP) {
       PGraphics.showWarning("GROUP shapes don't have any vertices");
       return;
     }
     updateTessellation();
     
     if (hasPoints) {
       int[] indices = in.pointIndices[index];
       for (int i = 0; i < indices.length; i++) {
         int tessIdx = indices[i];
         tess.pointVertices[3 * tessIdx + 0] = x;
         tess.pointVertices[3 * tessIdx + 1] = y;
         tess.pointVertices[3 * tessIdx + 2] = z;        
       } 
       modifiedPointVertices = true;
     }
 
     if (hasLines) {
       int[] indices = in.lineIndices[index];
       for (int i = 0; i < indices.length; i++) {
         int tessIdx = indices[i];
         tess.lineVertices[3 * tessIdx + 0] = x;
         tess.lineVertices[3 * tessIdx + 1] = y;
         tess.lineVertices[3 * tessIdx + 2] = z;        
       }     
       modifiedLineVertices = true;
     }    
     
     if (hasFill) {
       if (-1 < in.firstFillIndex) {
         int tessIdx = in.firstFillIndex + index;
         tess.fillVertices[3 * tessIdx + 0] = x;
         tess.fillVertices[3 * tessIdx + 1] = y;
         tess.fillVertices[3 * tessIdx + 2] = z;
       } else {
         float x0 = in.vertices[3 * index + 0];
         float y0 = in.vertices[3 * index + 1];
         float z0 = in.vertices[3 * index + 2];        
         int[] indices = in.fillIndices[index];
         float[] weigths = in.fillWeights[index];
         for (int i = 0; i < indices.length; i++) {
           int tessIdx = indices[i];
           float weight = weigths[i];
           float tx0 = tess.fillVertices[3 * tessIdx + 0];
           float ty0 = tess.fillVertices[3 * tessIdx + 1];
           float tz0 = tess.fillVertices[3 * tessIdx + 2];        
           tess.fillVertices[3 * tessIdx + 0] = tx0 + weight * (x - x0);
           tess.fillVertices[3 * tessIdx + 1] = ty0 + weight * (y - y0);
           tess.fillVertices[3 * tessIdx + 2] = tz0 + weight * (z - z0);        
         }    
       }
       modifiedFillVertices = true;      
     }
     in.vertices[3 * index + 0] = x;
     in.vertices[3 * index + 1] = y;
     in.vertices[3 * index + 2] = z;    
     modified();
   }
   
   
   public PVector getNormal(int index, PVector vec) {
     if (family == GROUP) {
       PGraphics.showWarning("GROUP shapes don't have any vertices");
       return null;
     }
     updateTessellation();
     
     if (vec == null) {
       vec = new PVector();
     }
     vec.x = in.normals[3 * index + 0];
     vec.y = in.normals[3 * index + 1];
     vec.z = in.normals[3 * index + 2];
     return vec;
   }
   
   
   public float getNormalX(int index) {
     if (family == GROUP) {
       PGraphics.showWarning("GROUP shapes don't have any vertices");
       return 0;
     }
     updateTessellation();
     
     return in.normals[3 * index + 0];
   }
 
   
   public float getNormalY(int index) {
     if (family == GROUP) {
       PGraphics.showWarning("GROUP shapes don't have any vertices");
       return 0;
     }
     updateTessellation();
     
     return in.normals[3 * index + 1];
   }  
 
   
   public float getNormalZ(int index) {
     if (family == GROUP) {
       PGraphics.showWarning("GROUP shapes don't have any vertices");
       return 0;
     }
     updateTessellation();
     
     return in.normals[3 * index + 2];
   }    
   
   
   public void setNormal(int index, float nx, float ny, float nz) {
     if (family == GROUP) {
       PGraphics.showWarning("GROUP shapes don't have any vertices");
       return;
     }
     updateTessellation();
     
     if (hasFill) {
       if (-1 < in.firstFillIndex) {
         int tessIdx = in.firstFillIndex + index;
         tess.fillNormals[3 * tessIdx + 0] = nx;
         tess.fillNormals[3 * tessIdx + 1] = ny;
         tess.fillNormals[3 * tessIdx + 2] = nz;
       } else {
         float nx0 = in.normals[3 * index + 0];
         float ny0 = in.normals[3 * index + 1];
         float nz0 = in.normals[3 * index + 2];        
         int[] indices = in.fillIndices[index];
         float[] weigths = in.fillWeights[index];
         for (int i = 0; i < indices.length; i++) {
           int tessIdx = indices[i];
           float weight = weigths[i];
           float tnx0 = tess.fillNormals[3 * tessIdx + 0];
           float tny0 = tess.fillNormals[3 * tessIdx + 1];
           float tnz0 = tess.fillNormals[3 * tessIdx + 2];        
           float tnx = tnx0 + weight * (nx - nx0);
           float tny = tny0 + weight * (ny - ny0);
           float tnz = tnz0 + weight * (nz - nz0);
           
           // Making sure that the new normal vector is indeed
           // normalized.
           float sum = tnx * tnx + tny * tny + tnz * tnz;
           float len = PApplet.sqrt(sum);
           tnx /= len;
           tny /= len;
           tnz /= len;
            
           tess.fillNormals[3 * tessIdx + 0] = tnx;
           tess.fillNormals[3 * tessIdx + 1] = tny;
           tess.fillNormals[3 * tessIdx + 2] = tnz;
         }    
       }
       modifiedFillNormals = true;      
     }    
     in.normals[3 * index + 0] = nx;
     in.normals[3 * index + 1] = ny;
     in.normals[3 * index + 2] = nz;      
     modified(); 
   }
   
   
   public PVector getVertexUV(int index, PVector vec) {
     if (family == GROUP) {
       PGraphics.showWarning("GROUP shapes don't have any vertices");
       return null;
     }
     updateTessellation();
     
     if (vec == null) {
       vec = new PVector();
     }
     vec.x = in.texcoords[2 * index + 0];
     vec.y = in.texcoords[2 * index + 1];
     return vec;
   }
   
   
   public float getVertexU(int index) {
     if (family == GROUP) {
       PGraphics.showWarning("GROUP shapes don't have any vertices");
       return 0;
     }
     updateTessellation();
     
     return in.texcoords[2 * index + 0];
   }
 
   
   public float getVertexV(int index) {
     if (family == GROUP) {
       PGraphics.showWarning("GROUP shapes don't have any vertices");
       return 0;
     }
     updateTessellation();
     
     return in.texcoords[2 * index + 1];
   }  
   
   
   public void setVertexUV(int index, float u, float v) {
     if (family == GROUP) {
       PGraphics.showWarning("GROUP shapes don't have any vertices");
       return;
     }
     updateTessellation();
     
     if (hasFill) {
       if (-1 < in.firstFillIndex) {
         int tessIdx = in.firstFillIndex + index;
         tess.fillTexcoords[2 * tessIdx + 0] = u;
         tess.fillTexcoords[2 * tessIdx + 1] = v;
       } else {       
         float u0 = in.normals[2 * index + 0];
         float v0 = in.normals[2 * index + 1];
         int[] indices = in.fillIndices[index];
         float[] weigths = in.fillWeights[index];
         for (int i = 0; i < indices.length; i++) {
           int tessIdx = indices[i];
           float weight = weigths[i];
           float tu0 = tess.fillTexcoords[2 * tessIdx + 0];
           float tv0 = tess.fillTexcoords[2 * tessIdx + 1];        
           float tu = tu0 + weight * (u - u0);
           float tv = tv0 + weight * (v - v0);           
           tess.fillTexcoords[2 * tessIdx + 0] = tu;
           tess.fillTexcoords[2 * tessIdx + 1] = tv;
         }        
       }
       modifiedFillTexCoords = true;
     }
     
     in.texcoords[3 * index + 0] = u;
     in.texcoords[3 * index + 1] = v;    
   }
 
   
   /*
   public float[] getVertex(int index) {
   }    
   public int[] getVertexCodes() {    
   }
   public int getVertexCodeCount() {    
   }
   public int getVertexCode(int index) {    
   }
   */
   
   
   ///////////////////////////////////////////////////////////  
   
   //
   
   // Getters of tessellated data.  
   
   
   public int fillVertexCount() {
     return tess.fillVertices.length;
   }
 
   
   public int fillIndexCount() {
     return tess.fillIndices.length;
   }  
   
   
   public float[] fillVertices(float[] vertices) {
     if (family == GROUP) {
       PGraphics.showWarning("GROUP shapes don't have any vertices");
       return vertices;
     }    
     updateTessellation();
     
     if (vertices == null || vertices.length != tess.fillVertices.length) {
       vertices = new float[tess.fillVertices.length];
     }
     PApplet.arrayCopy(tess.fillVertices, vertices);
     return vertices;
   }
   
   
   public int[] fillColors(int[] colors) {
     if (family == GROUP) {
       PGraphics.showWarning("GROUP shapes don't have any vertices");
       return colors;
     }    
     updateTessellation();
     
     if (colors == null || colors.length != tess.fillColors.length) {
       colors = new int[tess.fillColors.length];  
     }
     PApplet.arrayCopy(tess.fillColors, colors);
     return colors;
   }  
   
   
   public float[] fillNormals(float[] normals) {
     if (family == GROUP) {
       PGraphics.showWarning("GROUP shapes don't have any vertices");
       return normals;
     }    
     updateTessellation();
     
     if (normals == null || normals.length != tess.fillNormals.length) {
       normals = new float[tess.fillNormals.length];
     }
     PApplet.arrayCopy(tess.fillNormals, normals);
     return normals;
   }  
   
   
   public float[] fillTexcoords(float[] texcoords) {
     if (family == GROUP) {
       PGraphics.showWarning("GROUP shapes don't have any vertices");
       return texcoords;
     }    
     updateTessellation();
     
     if (texcoords == null || texcoords.length != tess.fillTexcoords.length) {
       texcoords = new float[tess.fillTexcoords.length];
     }
     PApplet.arrayCopy(tess.fillTexcoords, texcoords);
     return texcoords;
   }  
 
   
   public int[] fillAmbient(int[] ambient) {
     if (family == GROUP) {
       PGraphics.showWarning("GROUP shapes don't have any vertices");
       return ambient;
     }    
     updateTessellation();
     
     if (ambient == null || ambient.length != tess.fillAmbient.length) {
       ambient = new int[tess.fillAmbient.length];
     }
     PApplet.arrayCopy(tess.fillAmbient, ambient);
     return ambient;
   }  
 
   
   public int[] fillSpecular(int[] specular) {
     if (family == GROUP) {
       PGraphics.showWarning("GROUP shapes don't have any vertices");
       return specular;
     }    
     updateTessellation();
     
     if (specular == null || specular.length != tess.fillSpecular.length) {
       specular = new int[tess.fillSpecular.length];  
     }
     PApplet.arrayCopy(tess.fillSpecular, specular);
     return specular;
   }
   
 
   public int[] fillEmissive(int[] emissive) {
     if (family == GROUP) {
       PGraphics.showWarning("GROUP shapes don't have any vertices");
       return emissive;
     }    
     updateTessellation();
     
     if (emissive == null || emissive.length != tess.fillEmissive.length) {
       emissive = new int[tess.fillEmissive.length];
     }
     PApplet.arrayCopy(tess.fillEmissive, emissive);
     return emissive;
   }
   
 
   public float[] fillShininess(float[] shininess) {
     if (family == GROUP) {
       PGraphics.showWarning("GROUP shapes don't have any vertices");
       return shininess;
     }    
     updateTessellation();
     
     if (shininess == null || shininess.length != tess.fillShininess.length) {
       shininess = new float[tess.fillShininess.length];
     }
     PApplet.arrayCopy(tess.fillShininess, shininess);
     return shininess;
   }  
   
   
   public int[] fillIndices(int[] indices) {
     if (family == GROUP) {
       PGraphics.showWarning("GROUP shapes don't have any vertices");
       return indices;
     }    
     updateTessellation();
     
     if (indices == null || indices.length != tess.fillIndices.length) {
       indices = new int[tess.fillIndices.length];      
     }
     //PApplet.arrayCopy(tess.fillIndices, indices);
     for (int i = 0; i < tess.fillIndices.length; i++) {
       indices[i] = tess.fillIndices[i];
     }    
     removeIndexOffset(indices);
     return indices;
   }   
   
 
   public int lineVertexCount() {
     return tess.lineVertices.length;
   }
 
   
   public int lineIndexCount() {
     return tess.lineIndices.length;
   }   
   
   
   public float[] lineVertices(float[] vertices) {
     if (family == GROUP) {
       PGraphics.showWarning("GROUP shapes don't have any vertices");
       return vertices;
     }    
     updateTessellation();
     
     if (vertices == null || vertices.length != tess.lineVertices.length) {
       vertices = new float[tess.lineVertices.length];
     }
     PApplet.arrayCopy(tess.lineVertices, vertices);
     return vertices;
   }
   
   
   public int[] lineColors(int[] colors) {
     if (family == GROUP) {
       PGraphics.showWarning("GROUP shapes don't have any vertices");
       return colors;
     }    
     updateTessellation();
     
     if (colors == null || colors.length != tess.lineColors.length) {
       colors = new int[tess.lineColors.length];
     }
     PApplet.arrayCopy(tess.lineColors, colors);
     return colors;
   }  
   
   
   public float[] lineAttributes(float[] attribs) {
     if (family == GROUP) {
       PGraphics.showWarning("GROUP shapes don't have any vertices");
       return attribs;
     }    
     updateTessellation();
     
     if (attribs == null || attribs.length != tess.lineDirWidths.length) {
       attribs = new float[tess.lineDirWidths.length];
     }
     PApplet.arrayCopy(tess.lineDirWidths, attribs);
     return attribs;
   }  
   
   
   public int[] lineIndices(int[] indices) {
     if (family == GROUP) {
       PGraphics.showWarning("GROUP shapes don't have any vertices");
       return indices;
     }    
     updateTessellation();
     
     if (indices == null || indices.length != tess.lineIndices.length) {
       indices = new int[tess.lineIndices.length];
     }
     //PApplet.arrayCopy(tess.lineIndices, indices);
     for (int i = 0; i < tess.lineIndices.length; i++) {
       indices[i] = tess.lineIndices[i];
     }    
     removeIndexOffset(indices);
     return indices;
   }  
   
   
   public int pointVertexCount() {
     return tess.pointVertices.length;
   }
 
   
   public int pointIndexCount() {
     return tess.pointIndices.length;
   }    
   
     
   public float[] pointVertices(float[] vertices) {
     if (family == GROUP) {
       PGraphics.showWarning("GROUP shapes don't have any vertices");
       return vertices;
     }    
     updateTessellation();
     
     if (vertices == null || vertices.length != tess.pointVertices.length) {
       vertices = new float[tess.pointVertices.length];
     }
     PApplet.arrayCopy(tess.pointVertices, vertices);
     return vertices;
   }
   
   
   public int[] pointColors(int[] colors) {
     if (family == GROUP) {
       PGraphics.showWarning("GROUP shapes don't have any vertices");
       return colors;
     }    
     updateTessellation();
     
     if (colors == null || colors.length != tess.pointColors.length) {
       colors = new int[tess.pointColors.length];
     }
     PApplet.arrayCopy(tess.pointColors, colors);
     return colors;
   }  
   
   
   public float[] pointAttributes(float[] attribs) {
     if (family == GROUP) {
       PGraphics.showWarning("GROUP shapes don't have any vertices");
       return attribs;
     }    
     updateTessellation();
     
     if (attribs == null || attribs.length != tess.pointSizes.length) {
       attribs = new float[tess.pointSizes.length];
     }
     PApplet.arrayCopy(tess.pointSizes, attribs);
     return attribs;
   }  
   
   
   public int[] pointIndices(int[] indices) {
     if (family == GROUP) {
       PGraphics.showWarning("GROUP shapes don't have any vertices");
       return indices;
     }    
     updateTessellation();
     
     if (indices == null || indices.length != tess.pointIndices.length) {
       indices = new int[tess.pointIndices.length];
     }
 //    PApplet.arrayCopy(tess.pointIndices, indices);
     for (int i = 0; i < tess.pointIndices.length; i++) {
       indices[i] = tess.pointIndices[i];
     }    
     removeIndexOffset(indices);
     return indices;
   }   
   
   
   protected void removeIndexOffset(int[] indices) {
     if (0 < indices.length && 0 < indices[0]) {
       // Removing any offset added in the aggregation step.
       int i0 = indices[0];
       for (int i = 0; i < indices.length; i++) {
         indices[i] -= i0;
       }
     }    
   }
   
     
   ///////////////////////////////////////////////////////////  
   
   //
   
   // Construction methods  
   
   
   protected void updateTessellation() {
     if (!root.tessellated || root.contextIsOutdated()) {
       root.tessellate();
       root.aggregate();
     }
   }
  
   
   protected void tessellate() {
     if (tess == null) {
       tess = pg.newTessGeometry(PGraphicsOpenGL.RETAINED, family == GROUP);
     }
     tess.clear();
     
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];
         child.tessellate();
       }      
     } else {   
       if (shapeEnded) {
         // If the geometry was tessellated previously, then
         // the edges information will still be stored in the
         // input object, so it needs to be removed to avoid
         // duplication.
         in.clearEdges();
         
         tessellator.setInGeometry(in);
         tessellator.setTessGeometry(tess);
         tessellator.setFill(fill || texture != null);
         tessellator.setStroke(stroke);
         tessellator.setStrokeWeight(strokeWeight);
         tessellator.setStrokeCap(strokeCap);
         tessellator.setStrokeJoin(strokeJoin);       
         
         if (family == GEOMETRY) {
           // The tessellation maps are used to associate input
           // vertices with the corresponding tessellated vertices.
           // This correspondence might not be one-to-one, in the
           // case of lines and polygon shapes for example.
           in.initTessMaps();
           
           if (kind == POINTS) {
             tessellator.tessellatePoints();    
           } else if (kind == LINES) {
             tessellator.tessellateLines();    
           } else if (kind == TRIANGLE || kind == TRIANGLES) {
             if (stroke) in.addTrianglesEdges();
             if (normalMode == NORMAL_MODE_AUTO) in.calcTrianglesNormals();
             tessellator.tessellateTriangles();
           } else if (kind == TRIANGLE_FAN) {
             if (stroke) in.addTriangleFanEdges();
             if (normalMode == NORMAL_MODE_AUTO) in.calcTriangleFanNormals();
             tessellator.tessellateTriangleFan();
           } else if (kind == TRIANGLE_STRIP) {            
             if (stroke) in.addTriangleStripEdges();
             if (normalMode == NORMAL_MODE_AUTO) in.calcTriangleStripNormals();
             tessellator.tessellateTriangleStrip();
           } else if (kind == QUAD || kind == QUADS) {            
             if (stroke) in.addQuadsEdges();
             if (normalMode == NORMAL_MODE_AUTO) in.calcQuadsNormals();
             tessellator.tessellateQuads();
           } else if (kind == QUAD_STRIP) {
             if (stroke) in.addQuadStripEdges();
             if (normalMode == NORMAL_MODE_AUTO) in.calcQuadStripNormals();
             tessellator.tessellateQuadStrip();
           } else if (kind == POLYGON) {
             if (stroke) in.addPolygonEdges(isClosed);
             tessellator.tessellatePolygon(isSolid, isClosed, normalMode == NORMAL_MODE_AUTO);
           }
         } else if (family == PRIMITIVE) {          
           // The input geometry needs to be cleared because the geometry
           // generation methods in InGeometry add the vertices of the
           // new primitive to what is already stored.
           in.clear();
           
           if (kind == POINT) {
             tessellatePoint();
           } else if (kind == LINE) {
             tessellateLine(); 
           } else if (kind == TRIANGLE) {
             tessellateTriangle();            
           } else if (kind == QUAD) {
             tessellateQuad();            
           } else if (kind == RECT) {
             tessellateRect();
           } else if (kind == ELLIPSE) {
             tessellateEllipse();
           } else if (kind == ARC) {
             tessellateArc();
           } else if (kind == BOX) {
             tessellateBox();            
           } else if (kind == SPHERE) {
             tessellateSphere();
           }
         } else if (family == PATH) {
           tessellatePath();
         }
         
         // Tessellated arrays are trimmed since they are expanded 
         // by doubling their old size, which might lead to arrays 
         // larger than the vertex counts.        
         tess.trim();
         
         if (texture != null && parent != null) {
           ((PShape3D)parent).addTexture(texture);
         }
         
         if (cacheTransformations && matrix != null) {
           // Some geometric transformations were applied on
           // this shape before tessellation, so they are applied now.
           tess.applyMatrix(matrix);
         }
         
         in.compactTessMaps();
       }
     }
     
     tessellated = true;
     modified = false;
   }
 
   
   protected void tessellatePoint() {
     float x = 0, y = 0, z = 0;
     if (params.length == 2) {
       x = params[0];
       y = params[1];
       z = 0;
     } else if (params.length == 3) {
       x = params[0];
       y = params[1];
       z = params[2];
     }
     
     in.setColors(fillColor, strokeColor, strokeWeight,
                  ambientColor, specularColor, emissiveColor, shininess);
     in.setNormal(normalX, normalY, normalZ);
     in.addPoint(x, y, z, fill, stroke);    
     in.initTessMaps();    
     tessellator.tessellatePoints();   
   }
   
   
   protected void tessellateLine() {
     float x1 = 0, y1 = 0, z1 = 0;
     float x2 = 0, y2 = 0, z2 = 0;
     if (params.length == 4) {
       x1 = params[0];
       y1 = params[1];
       x2 = params[2];
       y2 = params[3];     
     } else if (params.length == 6) {
       x1 = params[0];
       y1 = params[1];
       z1 = params[2];
       x2 = params[3];
       y2 = params[4];
       z2 = params[5];      
     }
     
     in.setColors(fillColor, strokeColor, strokeWeight,
                  ambientColor, specularColor, emissiveColor, shininess);
     in.setNormal(normalX, normalY, normalZ);
     in.addLine(x1, y1, z1,
                x2, y2, z2,
                fill, stroke);    
     in.initTessMaps();    
     tessellator.tessellateLines();  
   }
   
   
   protected void tessellateTriangle() {
     float x1 = 0, y1 = 0;
     float x2 = 0, y2 = 0;
     float x3 = 0, y3 = 0;
     if (params.length == 6) {
       x1 = params[0];
       y1 = params[1];
       x2 = params[2];
       y2 = params[3];
       x3 = params[4];
       y3 = params[5];       
     }
 
     in.setColors(fillColor, strokeColor, strokeWeight,
                  ambientColor, specularColor, emissiveColor, shininess);  
     in.setNormal(normalX, normalY, normalZ);
     in.addTriangle(x1, y1, 0,
                    x2, y2, 0,
                    x3, y3, 0,
                    fill, stroke);    
     in.initTessMaps();    
     tessellator.tessellateTriangles();    
   }
   
   
   protected void tessellateQuad() {
     float x1 = 0, y1 = 0;
     float x2 = 0, y2 = 0;
     float x3 = 0, y3 = 0;
     float x4 = 0, y4 = 0;    
     if (params.length == 8) {
       x1 = params[0];
       y1 = params[1];
       x2 = params[2];
       y2 = params[3];
       x3 = params[4];
       y3 = params[5];      
       x4 = params[6];
       y4 = params[7];            
     }
 
     in.setColors(fillColor, strokeColor, strokeWeight,
                  ambientColor, specularColor, emissiveColor, shininess);
     in.setNormal(normalX, normalY, normalZ);
     in.addQuad(x1, y1, 0,
                x2, y2, 0,
                x3, y3, 0,
                x4, y4, 0,
                fill, stroke);    
     in.initTessMaps();    
     tessellator.tessellateQuads();     
   }  
   
   
   protected void tessellateRect() {
     float a = 0, b = 0, c = 0, d = 0;
     float tl = 0, tr = 0, br = 0, bl = 0;
     boolean rounded = false;
     if (params.length == 4) {
       rounded = false;
       a = params[0];
       b = params[1];
       c = params[2];
       d = params[3];
     } else if (params.length == 5) {
       a = params[0];
       b = params[1];
       c = params[2];
       d = params[3];
       tl = tr = br = bl = params[4]; 
       rounded = true;
     } else if (params.length == 8) {
       a = params[0];
       b = params[1];
       c = params[2];
       d = params[3];
       tl = params[4];
       tr = params[5];
       br = params[6];
       bl = params[7]; 
       rounded = true;
     }
 
     in.setColors(fillColor, strokeColor, strokeWeight,
                  ambientColor, specularColor, emissiveColor, shininess);
     in.setNormal(normalX, normalY, normalZ);
     if (rounded) {
       in.addRect(a, b, c, d,
                  tl, tr, br, bl,
                  fill, stroke, bezierDetail, rectMode);       
       in.initTessMaps();      
       tessellator.tessellatePolygon(false, true, true);      
     } else {
       in.addRect(a, b, c, d,
                  fill, stroke, rectMode);    
       in.initTessMaps();
       tessellator.tessellateQuads();      
     }   
   }
   
   
   protected void tessellateEllipse() {
     float a = 0, b = 0, c = 0, d = 0;
     if (params.length == 4) {
       a = params[0];
       b = params[1];
       c = params[2];
       d = params[3];      
     }
 
     in.setColors(fillColor, strokeColor, strokeWeight,
                  ambientColor, specularColor, emissiveColor, shininess);
     in.setNormal(normalX, normalY, normalZ);    
     in.addEllipse(a, b, c, d, fill, stroke, ellipseMode);
     in.initTessMaps();
     tessellator.tessellateTriangleFan(); 
   }
   
   
   protected void tessellateArc() {
     float a = 0, b = 0, c = 0, d = 0;
     float start = 0, stop = 0;
     if (params.length == 6) {
       a = params[0];
       b = params[1];
       c = params[2];
       d = params[3];      
       start = params[4];
       stop = params[5];      
     }    
     
     in.setColors(fillColor, strokeColor, strokeWeight,
                  ambientColor, specularColor, emissiveColor, shininess);
     in.setNormal(normalX, normalY, normalZ);
     in.addArc(a, b, c, d, start, stop, fill, stroke, ellipseMode);
     in.initTessMaps();
     tessellator.tessellateTriangleFan();    
   }
   
   
   protected void tessellateBox() {
     float w = 0, h = 0, d = 0;
     if (params.length == 1) {
       w = h = d = params[0];  
     } else if (params.length == 3) {
       w = params[0];
       h = params[1];
       d = params[2];
     }
         
     in.setColors(fillColor, strokeColor, strokeWeight,
                  ambientColor, specularColor, emissiveColor, shininess);    
     in.addBox(w, h, d, fill, stroke);   
     in.initTessMaps();    
     tessellator.tessellateQuads();     
   }
   
   
   protected void tessellateSphere() {
     // Getting sphere detail from renderer. Is this correct?
     int nu = pg.sphereDetailU;
     int nv = pg.sphereDetailV;
     float r = 0;
     if (params.length == 1) {
       r = params[0];
     }
     
     in.setColors(fillColor, strokeColor, strokeWeight,
                  ambientColor, specularColor, emissiveColor, shininess); 
     int[] indices = in.addSphere(r, nu, nv, fill, stroke);   
     in.initTessMaps();
     tessellator.tessellateTriangles(indices);               
   }
   
   
   protected void tessellatePath() {
     if (vertices == null) return;
 
     in.setColors(fillColor, strokeColor, strokeWeight,
                  ambientColor, specularColor, emissiveColor, shininess);
     
     boolean insideContour = false;
 
     if (vertexCodeCount == 0) {  // each point is a simple vertex
       if (vertices[0].length == 2) {  // tesellating 2D vertices
         for (int i = 0; i < vertexCount; i++) {
           in.addVertex(vertices[i][X], vertices[i][Y], VERTEX);
         }
       } else {  // drawing 3D vertices
         for (int i = 0; i < vertexCount; i++) {
           in.addVertex(vertices[i][X], vertices[i][Y], vertices[i][Z], VERTEX);
         }
       }
     } else {  // coded set of vertices
       int index = 0;
       int code = VERTEX;
       
       if (vertices[0].length == 2) {  // tessellating a 2D path
         for (int j = 0; j < vertexCodeCount; j++) {
           switch (vertexCodes[j]) {
 
           case VERTEX:
             in.addVertex(vertices[index][X], vertices[index][Y], code);
             index++;
             break;
 
           case QUAD_BEZIER_VERTEX:
             in.addQuadraticVertex(vertices[index+0][X], vertices[index+0][Y], 0, 
                                   vertices[index+1][X], vertices[index+1][Y], 0,
                                   fill, stroke, bezierDetail, code);
             index += 2;
             break;
 
           case BEZIER_VERTEX:
             in.addBezierVertex(vertices[index+0][X], vertices[index+0][Y], 0,
                                vertices[index+1][X], vertices[index+1][Y], 0,
                                vertices[index+2][X], vertices[index+2][Y], 0,
                                fill, stroke, bezierDetail, code);
             index += 3;
             break;
 
           case CURVE_VERTEX:
             in.addCurveVertex(vertices[index][X], vertices[index][Y], 0,
                               fill, stroke, curveDetail, code);
             index++;
 
           case BREAK:
             if (insideContour) {
               code = VERTEX;
             }
             code = BREAK;
             insideContour = true;
           }
         }
       } else {  // tessellating a 3D path
         for (int j = 0; j < vertexCodeCount; j++) {
           switch (vertexCodes[j]) {
 
           case VERTEX:
             in.addVertex(vertices[index][X], vertices[index][Y], vertices[index][Z], code);
             index++;
             break;
 
           case QUAD_BEZIER_VERTEX:
             in.addQuadraticVertex(vertices[index+0][X], vertices[index+0][Y], vertices[index+0][Z],
                                   vertices[index+1][X], vertices[index+1][Y], vertices[index+0][Z],
                                   fill, stroke, bezierDetail, code);
             index += 2;
             break;
 
 
           case BEZIER_VERTEX:
             in.addBezierVertex(vertices[index+0][X], vertices[index+0][Y], vertices[index+0][Z],
                                vertices[index+1][X], vertices[index+1][Y], vertices[index+1][Z],
                                vertices[index+2][X], vertices[index+2][Y], vertices[index+2][Z],
                                fill, stroke, bezierDetail, code);
             index += 3;
             break;
 
           case CURVE_VERTEX:
             in.addCurveVertex(vertices[index][X], vertices[index][Y], vertices[index][Z],
                               fill, stroke, curveDetail, code);
             index++;
 
           case BREAK:
             if (insideContour) {
               code = VERTEX;
             }
             code = BREAK;
             insideContour = true;
           }
         }
       }
     }
     
     if (stroke) in.addPolygonEdges(isClosed);
     in.initTessMaps();  
     tessellator.tessellatePolygon(false, isClosed, true);    
   }  
   
   
   protected void updateGeometry() {
     if (root == this && parent == null && modified) {
       // Initializing offsets
       fillVertCopyOffset = 0;
       lineVertCopyOffset = 0;
       pointVertCopyOffset = 0;
       
       updateRootGeometry();
       
       // Copying any data remaining in the caches
       if (root.fillVerticesCache != null && root.fillVerticesCache.hasData()) {
         root.copyFillVertices(root.fillVerticesCache.offset, root.fillVerticesCache.size, root.fillVerticesCache.floatData);
         root.fillVerticesCache.clear();
       }
       
       if (root.fillColorsCache != null && root.fillColorsCache.hasData()) {
         root.copyFillColors(root.fillColorsCache.offset, root.fillColorsCache.size, root.fillColorsCache.intData);
         root.fillColorsCache.clear();
       }
       
       if (root.fillNormalsCache != null && root.fillNormalsCache.hasData()) {
         root.copyFillNormals(root.fillNormalsCache.offset, root.fillNormalsCache.size, root.fillNormalsCache.floatData);
         root.fillNormalsCache.clear();
       }
       
       if (root.fillTexCoordsCache != null && root.fillTexCoordsCache.hasData()) {
         root.copyFillTexCoords(root.fillTexCoordsCache.offset, root.fillTexCoordsCache.size, root.fillTexCoordsCache.floatData);
         root.fillTexCoordsCache.clear();
       }
       
       if (root.fillAmbientCache != null && root.fillAmbientCache.hasData()) {
         root.copyFillAmbient(root.fillAmbientCache.offset, root.fillAmbientCache.size, root.fillAmbientCache.intData);
         root.fillAmbientCache.clear();
       }
 
       if (root.fillSpecularCache != null && root.fillSpecularCache.hasData()) {
         root.copyFillSpecular(root.fillSpecularCache.offset, root.fillSpecularCache.size, root.fillSpecularCache.intData);
         root.fillSpecularCache.clear();
       }      
       
       if (root.fillEmissiveCache != null && root.fillEmissiveCache.hasData()) {
         root.copyFillEmissive(root.fillEmissiveCache.offset, root.fillEmissiveCache.size, root.fillEmissiveCache.intData);
         root.fillEmissiveCache.clear();
       }      
       
       if (root.fillShininessCache != null && root.fillShininessCache.hasData()) {
         root.copyFillShininess(root.fillShininessCache.offset, root.fillShininessCache.size, root.fillShininessCache.floatData);
         root.fillShininessCache.clear();
       }            
       
       if (root.lineVerticesCache != null && root.lineVerticesCache.hasData()) {
         root.copyLineVertices(root.lineVerticesCache.offset, root.lineVerticesCache.size, root.lineVerticesCache.floatData);
         root.lineVerticesCache.clear();
       }
       
       if (root.lineColorsCache != null && root.lineColorsCache.hasData()) {
         root.copyLineColors(root.lineColorsCache.offset, root.lineColorsCache.size, root.lineColorsCache.intData);
         root.lineColorsCache.clear();
       }
       
       if (root.lineAttributesCache != null && root.lineAttributesCache.hasData()) {
         root.copyLineAttributes(root.lineAttributesCache.offset, root.lineAttributesCache.size, root.lineAttributesCache.floatData);
         root.lineAttributesCache.clear();
       }      
     
      if (root.pointVerticesCache != null && root.pointVerticesCache.hasData()) {
         root.copyPointVertices(root.pointVerticesCache.offset, root.pointVerticesCache.size, root.pointVerticesCache.floatData);
         root.pointVerticesCache.clear();
       }
       
       if (root.pointColorsCache != null && root.pointColorsCache.hasData()) {
         root.copyPointColors(root.pointColorsCache.offset, root.pointColorsCache.size, root.pointColorsCache.intData);
         root.pointColorsCache.clear();
       }
       
       if (root.pointAttributesCache != null && root.pointAttributesCache.hasData()) {
         root.copyPointAttributes(root.pointAttributesCache.offset, root.pointAttributesCache.size, root.pointAttributesCache.floatData);
         root.pointAttributesCache.clear();
       }        
     }
   }
   
   
   protected void aggregate() {
     if (root == this && parent == null) {
       fillIndexOffset = 0;
       fillVertexOffset = 0;
       fillVertexAbs = 0;      
       fillVertexRel = 0;      
       
       lineIndexOffset = 0;
       lineVertexOffset = 0;
       lineVertexAbs = 0;      
       lineVertexRel = 0;  
       
       pointIndexOffset = 0;
       pointVertexOffset = 0;
       pointVertexAbs = 0;      
       pointVertexRel = 0;
       
       // We recursively calculate the total number of vertices and indices.
       aggregateImpl();
       
       needBufferInit = true;      
     }
   }
   
   
   // This method is very important, as it is responsible of generating the correct 
   // vertex and index offsets for each level of the shape hierarchy.
   // This is the core of the recursive algorithm that calculates the indices
   // for the vertices accumulated in a single VBO.
   // Basically, the algorithm traverses all the shapes in the hierarchy and 
   // updates the index cache for each child shape holding geometry (those being 
   // the leaf nodes in the hierarchy tree), and creates index caches for the 
   // group shapes so that the draw() method can be called from any shape in the
   // hierarchy and the correct piece of geometry will be rendered.  
   //
   // For example, in the following hierarchy:
   //
   //                     ROOT GROUP                       
   //                         |
   //       /-----------------0-----------------\ 
   //       |                                   |
   //  CHILD GROUP 0                       CHILD GROUP 1                     
   //       |                                   |
   //       |                   /---------------0-----------------\              
   //       |                   |               |                 |
   //   GEO SHAPE 0         GEO SHAPE 0     GEO SHAPE 1       GEO SHAPE 2
   //   4 vertices          5 vertices      6 vertices        3 vertices
   //
   // calling draw() from the root group should result in all the 
   // vertices (4 + 5 + 6 + 3 = 18) being rendered, while calling
   // draw() from either child groups 0 or 1 should result in the first
   // 4 vertices or the last 14 vertices being rendered, respectively.
   protected void aggregateImpl() {
     if (family == GROUP) {
       boolean firstGeom = true;
       boolean firstStroke = true;
       boolean firstPoint = true;
       for (int i = 0; i < childCount; i++) {        
         PShape3D child = (PShape3D) children[i];
         child.aggregateImpl();
 
         tess.addCounts(child.tess);
         
         if (0 < child.tess.fillVertexCount) {
           if (firstGeom) {
             tess.setFirstFill(child.tess);
             firstGeom = false;
           }
           tess.setLastFill(child.tess);
         }  
 
         if (0 < child.tess.lineVertexCount) {
           if (firstStroke) {
             tess.setFirstLine(child.tess);
             firstStroke = false;
           }          
           tess.setLastLine(child.tess);
         }
         
         if (0 < child.tess.pointVertexCount) {
           if (firstPoint) {
             tess.setFirstPoint(child.tess);
             firstPoint = false;
           }
           tess.setLastPoint(child.tess);
         }           
       }
       
       buildFillIndexCache();
       buildLineIndexCache();
       buildPointIndexCache();
     } else { // LEAF SHAPE (family either GEOMETRY, PATH or PRIMITIVE)
       
       // The index caches for fill, line and point geometry are updated
       // in order to reflect the fact that all the vertices will be stored
       // in a single VBO in the root shape.
       // This update works as follows (the methodology is the same for
       // fill, line and point): the VertexAbs variable in the root shape 
       // stores the index of the last vertex up to this shape (plus one)
       // without taking into consideration the MAX_VERTEX_INDEX limit, so
       // it effectively runs over the entire range.
       // VertexRel, on the other hand, is reset every time the limit is
       // exceeded, therefore creating the start of a new index group in the
       // root shape. When this happens, the indices in the child shape need
       // to be restarted as well to reflect the new index offset.
       
       if (0 < tess.fillVertexCount && 0 < tess.fillIndexCount) {
         IndexCache cache = tess.fillIndexCache;
         for (int n = 0; n < cache.count; n++) {
           int ioffset = cache.indexOffset[n];
           int icount = cache.indexCount[n];
           int vcount = cache.vertexCount[n];
 
           if (PGL.MAX_VERTEX_INDEX1 <= root.fillVertexRel + vcount) {            
             root.fillVertexRel = 0;
             root.fillVertexOffset = root.fillVertexAbs;
             cache.indexOffset[n] = root.fillIndexOffset;
           } else tess.incFillIndices(ioffset, ioffset + icount - 1, root.fillVertexRel); 
           cache.vertexOffset[n] = root.fillVertexOffset;
                     
           root.fillIndexOffset += icount;          
           root.fillVertexAbs += vcount;
           root.fillVertexRel += vcount;
         }
         tess.updateFillFromCache();
       }
             
       if (0 < tess.lineVertexCount && 0 < tess.lineIndexCount) {
         IndexCache cache = tess.lineIndexCache;
         for (int n = 0; n < cache.count; n++) {
           int ioffset = cache.indexOffset[n];
           int icount = cache.indexCount[n];
           int vcount = cache.vertexCount[n];
          
           if (PGL.MAX_VERTEX_INDEX1 <= root.lineVertexRel + vcount) {            
             root.lineVertexRel = 0;
             root.lineVertexOffset = root.lineVertexAbs;
             cache.indexOffset[n] = root.lineIndexOffset;
           } else tess.incLineIndices(ioffset, ioffset + icount - 1, root.lineVertexRel); 
           cache.vertexOffset[n] = root.lineVertexOffset;          
           
           root.lineIndexOffset += icount;          
           root.lineVertexAbs += vcount;
           root.lineVertexRel += vcount;          
         }
         tess.updateLineFromCache();
       }
             
       if (0 < tess.pointVertexCount && 0 < tess.pointIndexCount) {
         IndexCache cache = tess.pointIndexCache;
         for (int n = 0; n < cache.count; n++) {
           int ioffset = cache.indexOffset[n];
           int icount = cache.indexCount[n];
           int vcount = cache.vertexCount[n];
           
           if (PGL.MAX_VERTEX_INDEX1 <= root.pointVertexRel + vcount) {            
             root.pointVertexRel = 0;
             root.pointVertexOffset = root.pointVertexAbs;
             cache.indexOffset[n] = root.pointIndexOffset;
           } else tess.incPointIndices(ioffset, ioffset + icount - 1, root.pointVertexRel); 
           cache.vertexOffset[n] = root.pointVertexOffset;  
           
           root.pointIndexOffset += icount;          
           root.pointVertexAbs += vcount;
           root.pointVertexRel += vcount;                    
         }
         tess.updatePointFromCache();
       }      
     }
     
     hasFill = 0 < tess.fillVertexCount && 0 < tess.fillIndexCount;
     hasLines = 0 < tess.lineVertexCount && 0 < tess.lineIndexCount; 
     hasPoints = 0 < tess.pointVertexCount && 0 < tess.pointIndexCount;            
   }
   
   
   // Builds the index cache for a group shape, using the caches of the child
   // shapes. The index ranges of the child shapes that share the vertex offset
   // are unified into a single range in the parent level.
   protected void buildFillIndexCache() {
     IndexCache gcache = tess.fillIndexCache;    
     int gindex = -1;
     gcache.clear(); 
     
     for (int i = 0; i < childCount; i++) {        
       PShape3D child = (PShape3D) children[i];
       IndexCache ccache = child.tess.fillIndexCache;
       
       for (int n = 0; n < ccache.count; n++) {        
         if (gindex == -1) {
           gindex = gcache.addNew(ccache, n);
         } else {
           if (gcache.vertexOffset[gindex] == ccache.vertexOffset[n]) {
             // When the vertex offsets are the same, this means that the 
             // current index range in the group shape can be extended to 
             // include either the index range in the current child shape.
             // This is a result of how the indices are updated for the
             // leaf shapes in aggregateImpl().
             gcache.incCounts(gindex, ccache.indexCount[n], ccache.vertexCount[n]);
           } else {
             gindex = gcache.addNew(ccache, n);
           }
         }
       }
     }    
   }
 
   
   protected void buildLineIndexCache() {
     IndexCache gcache = tess.lineIndexCache;    
     int gindex = -1;
     gcache.clear(); 
     
     for (int i = 0; i < childCount; i++) {        
       PShape3D child = (PShape3D) children[i];
       IndexCache ccache = child.tess.lineIndexCache;
       
       for (int n = 0; n < ccache.count; n++) {        
         if (gindex == -1) {
           gindex = gcache.addNew(ccache, n);
         } else {
           if (gcache.vertexOffset[gindex] == ccache.vertexOffset[n]) {
             gcache.incCounts(gindex, ccache.indexCount[n], ccache.vertexCount[n]);
           } else {
             gindex = gcache.addNew(ccache, n);
           }
         }
       }
     }        
   }  
 
   
   protected void buildPointIndexCache() {
     IndexCache gcache = tess.pointIndexCache;    
     int gindex = -1;
     gcache.clear(); 
     
     for (int i = 0; i < childCount; i++) {        
       PShape3D child = (PShape3D) children[i];
       IndexCache ccache = child.tess.pointIndexCache;
       
       for (int n = 0; n < ccache.count; n++) {        
         if (gindex == -1) {
           gindex = gcache.addNew(ccache, n);
         } else {
           if (gcache.vertexOffset[gindex] == ccache.vertexOffset[n]) {
             gcache.incCounts(gindex, ccache.indexCount[n], ccache.vertexCount[n]);
           } else {
             gindex = gcache.addNew(ccache, n);
           }
         }
       }
     }   
   }
   
   
   protected void initBuffers() {
     if (root.needBufferInit) {
       root.copyGeometryToRoot();
     }
   }
   
   
   protected void copyGeometryToRoot() {
     if (root == this && parent == null) {
       context = pgl.getCurrentContext();
       
       // Now that we know, we can initialize the buffers with the correct size.
       if (0 < tess.fillVertexCount && 0 < tess.fillIndexCount) {   
         initFillBuffers(tess.fillVertexCount, tess.fillIndexCount);          
         fillVertCopyOffset = 0;
         fillIndCopyOffset = 0;
         copyFillGeometryToRoot();
       }
       
       if (0 < tess.lineVertexCount && 0 < tess.lineIndexCount) {   
         initLineBuffers(tess.lineVertexCount, tess.lineIndexCount);
         lineVertCopyOffset = 0;
         lineIndCopyOffset = 0;
         copyLineGeometryToRoot();
       }
       
       if (0 < tess.pointVertexCount && 0 < tess.pointIndexCount) {   
         initPointBuffers(tess.pointVertexCount, tess.pointIndexCount);
         pointVertCopyOffset = 0;
         pointIndCopyOffset = 0;
         copyPointGeometryToRoot();
       }
       
       needBufferInit = false;
       
       // Since all the tessellated geometry has just been copied to the
       // root VBOs, we can mark all the shapes as not modified.
       notModified();
     }
   }
   
   
   protected void modeCheck() {
     if (root.mode == STATIC && root.prevMode == DYNAMIC) {
       root.freeCaches();
       root.freeTessData();
       root.freeTessMaps();
     }
     root.prevMode = root.mode;   
   }
   
   
   protected boolean contextIsOutdated() {
     boolean outdated = !pgl.contextIsCurrent(context);
     if (outdated) {
       // Removing the VBOs from the renderer's list so they
       // doesn't get deleted by OpenGL. The VBOs were already 
       // automatically disposed when the old context was 
       // destroyed.
       pg.removeVertexBufferObject(glFillVertexBufferID, context.code());
       pg.removeVertexBufferObject(glFillColorBufferID, context.code());
       pg.removeVertexBufferObject(glFillNormalBufferID, context.code());
       pg.removeVertexBufferObject(glFillTexCoordBufferID, context.code());
       pg.removeVertexBufferObject(glFillAmbientBufferID, context.code());
       pg.removeVertexBufferObject(glFillSpecularBufferID, context.code());
       pg.removeVertexBufferObject(glFillEmissiveBufferID, context.code());
       pg.removeVertexBufferObject(glFillShininessBufferID, context.code());     
       pg.removeVertexBufferObject(glFillIndexBufferID, context.code());
       
       pg.removeVertexBufferObject(glLineVertexBufferID, context.code());
       pg.removeVertexBufferObject(glLineColorBufferID, context.code());
       pg.removeVertexBufferObject(glLineDirWidthBufferID, context.code());
       pg.removeVertexBufferObject(glLineIndexBufferID, context.code());
       
       pg.removeVertexBufferObject(glPointVertexBufferID, context.code());
       pg.removeVertexBufferObject(glPointColorBufferID, context.code());
       pg.removeVertexBufferObject(glPointSizeBufferID, context.code());
       pg.removeVertexBufferObject(glPointIndexBufferID, context.code());
       
       // The OpenGL resources have been already deleted
       // when the context changed. We only need to zero 
       // them to avoid deleting them again when the GC
       // runs the finalizers of the disposed object.
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
     return outdated;
   }
   
   
   protected void initFillBuffers(int nvert, int nind) {
     int sizef = nvert * PGL.SIZEOF_FLOAT;
     int sizei = nvert * PGL.SIZEOF_INT;
     int sizex = nind * PGL.SIZEOF_INDEX;
     
     glFillVertexBufferID = pg.createVertexBufferObject(context.code());  
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillVertexBufferID);
     pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 3 * sizef, null, PGL.GL_STATIC_DRAW);
     
     glFillColorBufferID = pg.createVertexBufferObject(context.code());
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillColorBufferID);
     pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, null, PGL.GL_STATIC_DRAW);    
     
     glFillNormalBufferID = pg.createVertexBufferObject(context.code());
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillNormalBufferID);
     pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 3 * sizef, null, PGL.GL_STATIC_DRAW);     
     
     glFillTexCoordBufferID = pg.createVertexBufferObject(context.code());
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillTexCoordBufferID);
     pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 2 * sizef, null, PGL.GL_STATIC_DRAW);  
     
     glFillAmbientBufferID = pg.createVertexBufferObject(context.code());  
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillAmbientBufferID);
     pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, null, PGL.GL_STATIC_DRAW);
     
     glFillSpecularBufferID = pg.createVertexBufferObject(context.code());
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillSpecularBufferID);
     pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, null, PGL.GL_STATIC_DRAW);    
     
     glFillEmissiveBufferID = pg.createVertexBufferObject(context.code());
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillEmissiveBufferID);
     pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, null, PGL.GL_STATIC_DRAW);
     
     glFillShininessBufferID = pg.createVertexBufferObject(context.code());
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillShininessBufferID);
     pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizef, null, PGL.GL_STATIC_DRAW);
         
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
         
     glFillIndexBufferID = pg.createVertexBufferObject(context.code());  
     pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glFillIndexBufferID);
     pgl.glBufferData(PGL.GL_ELEMENT_ARRAY_BUFFER, sizex, null, PGL.GL_STATIC_DRAW);
     
     pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);  
   }  
   
   
   protected void copyFillGeometryToRoot() {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.copyFillGeometryToRoot();
       }    
     } else {
       if (0 < tess.fillVertexCount && 0 < tess.fillIndexCount) {        
         root.copyFillGeometry(root.fillVertCopyOffset, tess.fillVertexCount, 
                               tess.fillVertices, tess.fillColors, tess.fillNormals, tess.fillTexcoords,
                               tess.fillAmbient, tess.fillSpecular, tess.fillEmissive, tess.fillShininess);
         root.fillVertCopyOffset += tess.fillVertexCount;
       
         root.copyFillIndices(root.fillIndCopyOffset, tess.fillIndexCount, tess.fillIndices);
         root.fillIndCopyOffset += tess.fillIndexCount;
       }
     }
   }
   
   
   protected void updateRootGeometry() {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];        
         child.updateRootGeometry();        
       } 
     } else {      
       if (0 < tess.fillVertexCount) {    
         if (modifiedFillVertices) {
           if (root.fillVerticesCache == null) { 
             root.fillVerticesCache = new VertexCache(3, true);
           }            
           
           root.fillVerticesCache.add(root.fillVertCopyOffset, tess.fillVertexCount, tess.fillVertices);
           modifiedFillVertices = false;
         } else if (root.fillVerticesCache != null && root.fillVerticesCache.hasData()) {
           root.copyFillVertices(root.fillVerticesCache.offset, root.fillVerticesCache.size, root.fillVerticesCache.floatData);
           root.fillVerticesCache.clear();
         }
         
         if (modifiedFillColors) {
           if (root.fillColorsCache == null) { 
             root.fillColorsCache = new VertexCache(1, false);
           }            
           root.fillColorsCache.add(root.fillVertCopyOffset, tess.fillVertexCount, tess.fillColors);
           modifiedFillColors = false;            
         } else if (root.fillColorsCache != null && root.fillColorsCache.hasData()) {
           root.copyFillColors(root.fillColorsCache.offset, root.fillColorsCache.size, root.fillColorsCache.intData);
           root.fillColorsCache.clear();
         }
         
         if (modifiedFillNormals) {
           if (root.fillNormalsCache == null) { 
             root.fillNormalsCache = new VertexCache(3, true);
           }            
           root.fillNormalsCache.add(root.fillVertCopyOffset, tess.fillVertexCount, tess.fillNormals);            
           modifiedFillNormals = false;            
         } else if (root.fillNormalsCache != null && root.fillNormalsCache.hasData()) {
           root.copyFillNormals(root.fillNormalsCache.offset, root.fillNormalsCache.size, root.fillNormalsCache.floatData);
           root.fillNormalsCache.clear();
         }
         
         if (modifiedFillTexCoords) {
           if (root.fillTexCoordsCache == null) { 
             root.fillTexCoordsCache = new VertexCache(2, true);
           }            
           root.fillTexCoordsCache.add(root.fillVertCopyOffset, tess.fillVertexCount, tess.fillTexcoords);            
           modifiedFillTexCoords = false;
         } else if (root.fillTexCoordsCache != null && root.fillTexCoordsCache.hasData()) {
           root.copyFillTexCoords(root.fillTexCoordsCache.offset, root.fillTexCoordsCache.size, root.fillTexCoordsCache.floatData);
           root.fillTexCoordsCache.clear();
         }
         
         if (modifiedFillAmbient) {
           if (root.fillAmbientCache == null) { 
             root.fillAmbientCache = new VertexCache(1, false);
           }            
           root.fillAmbientCache.add(root.fillVertCopyOffset, tess.fillVertexCount, tess.fillAmbient);            
           modifiedFillAmbient = false;
         } else if (root.fillAmbientCache != null && root.fillAmbientCache.hasData()) {
           root.copyFillAmbient(root.fillAmbientCache.offset, root.fillAmbientCache.size, root.fillAmbientCache.intData);
           root.fillAmbientCache.clear();
         }
 
         if (modifiedFillSpecular) {
           if (root.fillSpecularCache == null) { 
             root.fillSpecularCache = new VertexCache(1, false);
           }            
           root.fillSpecularCache.add(root.fillVertCopyOffset, tess.fillVertexCount, tess.fillSpecular);            
           modifiedFillSpecular = false;
         } else if (root.fillSpecularCache != null && root.fillSpecularCache.hasData()) {
           root.copyFillSpecular(root.fillSpecularCache.offset, root.fillSpecularCache.size, root.fillSpecularCache.intData);
           root.fillSpecularCache.clear();
         }        
         
         if (modifiedFillEmissive) {
           if (root.fillEmissiveCache == null) { 
             root.fillEmissiveCache = new VertexCache(1, false);
           }            
           root.fillEmissiveCache.add(root.fillVertCopyOffset, tess.fillVertexCount, tess.fillEmissive);            
           modifiedFillEmissive = false;
         } else if (root.fillEmissiveCache != null && root.fillEmissiveCache.hasData()) {
           root.copyFillEmissive(root.fillEmissiveCache.offset, root.fillEmissiveCache.size, root.fillEmissiveCache.intData);
           root.fillEmissiveCache.clear();
         }          
         
         if (modifiedFillShininess) {
           if (root.fillShininessCache == null) { 
             root.fillShininessCache = new VertexCache(1, true);
           }            
           root.fillShininessCache.add(root.fillVertCopyOffset, tess.fillVertexCount, tess.fillShininess);            
           modifiedFillShininess = false;
         } else if (root.fillShininessCache != null && root.fillShininessCache.hasData()) {
           root.copyFillShininess(root.fillShininessCache.offset, root.fillShininessCache.size, root.fillShininessCache.floatData);
           root.fillShininessCache.clear();
         }          
       } 
       
       if (0 < tess.lineVertexCount) {
         if (modifiedLineVertices) {
           if (root.lineVerticesCache == null) { 
             root.lineVerticesCache = new VertexCache(3, true);
           }            
           root.lineVerticesCache.add(root.lineVertCopyOffset, tess.lineVertexCount, tess.lineVertices);
           modifiedLineVertices = false;
         } else if (root.lineVerticesCache != null && root.lineVerticesCache.hasData()) {
           root.copyLineVertices(root.lineVerticesCache.offset, root.lineVerticesCache.size, root.lineVerticesCache.floatData);
           root.lineVerticesCache.clear();
         }
         
         if (modifiedLineColors) {
           if (root.lineColorsCache == null) { 
             root.lineColorsCache = new VertexCache(1, false);
           }            
           root.lineColorsCache.add(root.lineVertCopyOffset, tess.lineVertexCount, tess.lineColors);
           modifiedLineColors = false;            
         } else if (root.lineColorsCache != null && root.lineColorsCache.hasData()) {
           root.copyLineColors(root.lineColorsCache.offset, root.lineColorsCache.size, root.lineColorsCache.intData);
           root.lineColorsCache.clear();
         }
         
         if (modifiedLineAttributes) {
           if (root.lineAttributesCache == null) { 
             root.lineAttributesCache = new VertexCache(4, true);
           }            
           root.lineAttributesCache.add(root.lineVertCopyOffset, tess.lineVertexCount, tess.lineDirWidths);            
           modifiedLineAttributes = false;
         } else if (root.lineAttributesCache != null && root.lineAttributesCache.hasData()) {
           root.copyLineAttributes(root.lineAttributesCache.offset, root.lineAttributesCache.size, root.lineAttributesCache.floatData);
           root.lineAttributesCache.clear();
         }      
       }
 
       if (0 < tess.pointVertexCount) {
         if (modifiedPointVertices) {
           if (root.pointVerticesCache == null) { 
             root.pointVerticesCache = new VertexCache(3, true);
           }            
           root.pointVerticesCache.add(root.pointVertCopyOffset, tess.pointVertexCount, tess.pointVertices);
           modifiedPointVertices = false;
         } else if (root.pointVerticesCache != null && root.pointVerticesCache.hasData()) {
           root.copyPointVertices(root.pointVerticesCache.offset, root.pointVerticesCache.size, root.pointVerticesCache.floatData);
           root.pointVerticesCache.clear();
         }
         
         if (modifiedPointColors) {
           if (root.pointColorsCache == null) { 
             root.pointColorsCache = new VertexCache(1, false);
           }            
           root.pointColorsCache.add(root.pointVertCopyOffset, tess.pointVertexCount, tess.pointColors);
           modifiedPointColors = false;            
         } else if (root.pointColorsCache != null && root.pointColorsCache.hasData()) {
           root.copyPointColors(root.pointColorsCache.offset, root.pointColorsCache.size, root.pointColorsCache.intData);
           root.pointColorsCache.clear();
         }
         
         if (modifiedPointAttributes) {
           if (root.pointAttributesCache == null) { 
             root.pointAttributesCache = new VertexCache(2, true);
           }            
           root.pointAttributesCache.add(root.pointVertCopyOffset, tess.pointVertexCount, tess.pointSizes);            
           modifiedPointAttributes = false;
         } else if (root.pointAttributesCache != null && root.pointAttributesCache.hasData()) {
           root.copyPointAttributes(root.pointAttributesCache.offset, root.pointAttributesCache.size, root.pointAttributesCache.floatData);
           root.pointAttributesCache.clear();
         }        
       }
       
       root.fillVertCopyOffset += tess.fillVertexCount;
       root.lineVertCopyOffset += tess.lineVertexCount;
       root.pointVertCopyOffset += tess.pointVertexCount;
     }
     
     modified = false;
   }
     
   
   protected void copyFillGeometry(int offset, int size, 
                                   float[] vertices, int[] colors, 
                                   float[] normals, float[] texcoords,
                                   int[] ambient, int[] specular, int[] emissive, float[] shininess) {
     int offsetf = offset * PGL.SIZEOF_FLOAT;
     int offseti = offset * PGL.SIZEOF_INT;
     int sizef = size * PGL.SIZEOF_FLOAT;
     int sizei = size * PGL.SIZEOF_INT;
     
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillVertexBufferID);
     pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, 3 * offsetf, 3 * sizef, FloatBuffer.wrap(vertices, 0, 3 * size));
     
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillColorBufferID);
     pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, offseti, sizei, IntBuffer.wrap(colors, 0, size));
     
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillNormalBufferID);
     pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, 3 * offsetf, 3 * sizef, FloatBuffer.wrap(normals, 0, 3 * size));
     
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillTexCoordBufferID);
     pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, 2 * offsetf, 2 * sizef, FloatBuffer.wrap(texcoords, 0, 2 * size));
     
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillAmbientBufferID);
     pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, offseti, sizei, IntBuffer.wrap(ambient, 0, size));
     
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillSpecularBufferID);
     pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, offseti, sizei, IntBuffer.wrap(specular, 0, size));    
     
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillEmissiveBufferID);
     pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, offseti, sizei, IntBuffer.wrap(emissive, 0, size));   
     
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillShininessBufferID);
     pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, offsetf, sizef, FloatBuffer.wrap(shininess, 0, size));
         
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);    
   }
 
   
   protected void copyFillVertices(int offset, int size, float[] vertices) {    
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillVertexBufferID);
     pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, 3 * offset * PGL.SIZEOF_FLOAT, 3 * size * PGL.SIZEOF_FLOAT, FloatBuffer.wrap(vertices, 0, 3 * size));
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
   }
   
   
   protected void copyFillColors(int offset, int size, int[] colors) {    
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillColorBufferID);
     pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, offset * PGL.SIZEOF_INT, size * PGL.SIZEOF_INT, IntBuffer.wrap(colors, 0, size));     
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
   }  
   
   
   protected void copyFillNormals(int offset, int size, float[] normals) {
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillNormalBufferID);
     pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, 3 * offset * PGL.SIZEOF_FLOAT, 3 * size * PGL.SIZEOF_FLOAT, FloatBuffer.wrap(normals, 0, 3 * size));    
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
   }  
 
   
   protected void copyFillTexCoords(int offset, int size, float[] texcoords) {
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillTexCoordBufferID);
     pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, 2 * offset * PGL.SIZEOF_FLOAT, 2 * size * PGL.SIZEOF_FLOAT, FloatBuffer.wrap(texcoords, 0, 2 * size));
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
   }   
 
   
   protected void copyFillAmbient(int offset, int size, int[] ambient) {
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillAmbientBufferID);
     pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, offset * PGL.SIZEOF_INT, size * PGL.SIZEOF_INT, IntBuffer.wrap(ambient, 0, size));
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);    
   }
   
   
   protected void copyFillSpecular(int offset, int size, int[] specular) {
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillSpecularBufferID);
     pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, offset * PGL.SIZEOF_INT, size * PGL.SIZEOF_INT, IntBuffer.wrap(specular, 0, size));     
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);       
   }
 
   
   protected void copyFillEmissive(int offset, int size, int[] emissive) {
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillEmissiveBufferID);
     pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, offset * PGL.SIZEOF_INT, size * PGL.SIZEOF_INT, IntBuffer.wrap(emissive, 0, size));      
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);    
   }  
 
   
   protected void copyFillShininess(int offset, int size, float[] shininess) {
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glFillShininessBufferID);
     pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, offset * PGL.SIZEOF_FLOAT, size * PGL.SIZEOF_FLOAT, FloatBuffer.wrap(shininess, 0, size));
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);     
   }    
   
   
   protected void copyFillIndices(int offset, int size, short[] indices) {
     pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glFillIndexBufferID);
     pgl.glBufferSubData(PGL.GL_ELEMENT_ARRAY_BUFFER, offset * PGL.SIZEOF_INDEX, size * PGL.SIZEOF_INDEX, ShortBuffer.wrap(indices, 0, size));
     pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);
   }
   
   
   protected void initLineBuffers(int nvert, int nind) {
     int sizef = nvert * PGL.SIZEOF_FLOAT;
     int sizei = nvert * PGL.SIZEOF_INT;
     int sizex = nind * PGL.SIZEOF_INDEX;
     
     glLineVertexBufferID = pg.createVertexBufferObject(context.code());    
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineVertexBufferID);      
     pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 3 * sizef, null, PGL.GL_STATIC_DRAW);
     
     glLineColorBufferID = pg.createVertexBufferObject(context.code());
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineColorBufferID);
     pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, null, PGL.GL_STATIC_DRAW);       
 
     glLineDirWidthBufferID = pg.createVertexBufferObject(context.code());
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineDirWidthBufferID);
     pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 4 * sizef, null, PGL.GL_STATIC_DRAW);    
     
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);    
     
     glLineIndexBufferID = pg.createVertexBufferObject(context.code());    
     pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glLineIndexBufferID);
     pgl.glBufferData(PGL.GL_ELEMENT_ARRAY_BUFFER, sizex, null, PGL.GL_STATIC_DRAW);
 
     pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);
   }
   
   
   protected void copyLineGeometryToRoot() {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];
         child.copyLineGeometryToRoot();
       }    
     } else {
       if (0 < tess.lineVertexCount && 0 < tess.lineIndexCount) {
         root.copyLineGeometry(root.lineVertCopyOffset, tess.lineVertexCount, 
                               tess.lineVertices, tess.lineColors, tess.lineDirWidths);        
         root.lineVertCopyOffset += tess.lineVertexCount;
         
         root.copyLineIndices(root.lineIndCopyOffset, tess.lineIndexCount, tess.lineIndices);
         root.lineIndCopyOffset += tess.lineIndexCount;        
       }
     }    
   }
 
   
   protected void copyLineGeometry(int offset, int size, 
                                   float[] vertices, int[] colors, float[] attribs) {
     int offsetf = offset * PGL.SIZEOF_FLOAT;
     int sizef = size * PGL.SIZEOF_FLOAT;
     int offseti = offset * PGL.SIZEOF_INT;
     int sizei = size * PGL.SIZEOF_INT;
     
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineVertexBufferID);
     pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, 3 * offsetf, 3 * sizef, FloatBuffer.wrap(vertices, 0, 3 * size));
 
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineColorBufferID);
     pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, offseti, sizei, IntBuffer.wrap(colors, 0, size));
     
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineDirWidthBufferID);
     pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, 4 * offsetf, 4 * sizef, FloatBuffer.wrap(attribs, 0, 4 * size));
     
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
   }    
   
   
   protected void copyLineVertices(int offset, int size, float[] vertices) {    
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineVertexBufferID);
     pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, 3 * offset * PGL.SIZEOF_FLOAT, 3 * size * PGL.SIZEOF_FLOAT, FloatBuffer.wrap(vertices, 0, 3 * size));
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
   }     
   
   
   protected void copyLineColors(int offset, int size, int[] colors) {
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineColorBufferID);
     pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, offset * PGL.SIZEOF_INT, size * PGL.SIZEOF_INT, IntBuffer.wrap(colors, 0, size));             
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
   }
   
   
   protected void copyLineAttributes(int offset, int size, float[] attribs) {
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glLineDirWidthBufferID);
     pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, 4 * offset * PGL.SIZEOF_FLOAT, 4 * size * PGL.SIZEOF_FLOAT, FloatBuffer.wrap(attribs, 0, 4 * size));
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
   }
   
   
   protected void copyLineIndices(int offset, int size, short[] indices) {
     pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glLineIndexBufferID);
     pgl.glBufferSubData(PGL.GL_ELEMENT_ARRAY_BUFFER, offset * PGL.SIZEOF_INDEX, size * PGL.SIZEOF_INDEX, ShortBuffer.wrap(indices, 0, size));
     pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);
   }  
   
 
   protected void initPointBuffers(int nvert, int nind) {
     int sizef = nvert * PGL.SIZEOF_FLOAT;
     int sizei = nvert * PGL.SIZEOF_INT;
     int sizex = nind * PGL.SIZEOF_INDEX;
     
     glPointVertexBufferID = pg.createVertexBufferObject(context.code());
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointVertexBufferID);
     pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 3 * sizef, null, PGL.GL_STATIC_DRAW);   
 
     glPointColorBufferID = pg.createVertexBufferObject(context.code());
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointColorBufferID);
     pgl.glBufferData(PGL.GL_ARRAY_BUFFER, sizei, null, PGL.GL_STATIC_DRAW);     
     
     glPointSizeBufferID = pg.createVertexBufferObject(context.code());
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointSizeBufferID);
     pgl.glBufferData(PGL.GL_ARRAY_BUFFER, 2 * sizef, null, PGL.GL_STATIC_DRAW);
       
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);     
         
     glPointIndexBufferID = pg.createVertexBufferObject(context.code());
     pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glPointIndexBufferID);
     pgl.glBufferData(PGL.GL_ELEMENT_ARRAY_BUFFER, sizex, null, PGL.GL_STATIC_DRAW);
     
     pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);
   }  
   
   
   protected void copyPointGeometryToRoot() {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D) children[i];
         child.copyPointGeometryToRoot();
       }    
     } else {
       if (0 < tess.pointVertexCount && 0 < tess.pointIndexCount) {
         root.copyPointGeometry(root.pointVertCopyOffset, tess.pointVertexCount, 
                                tess.pointVertices, tess.pointColors, tess.pointSizes);        
         root.pointVertCopyOffset += tess.pointVertexCount;
         
         root.copyPointIndices(root.pointIndCopyOffset, tess.pointIndexCount, tess.pointIndices);
         root.pointIndCopyOffset += tess.pointIndexCount;        
       }
     }
   }
   
   
   protected void copyPointGeometry(int offset, int size, 
                                    float[] vertices, int[] colors, float[] attribs) {
     int offsetf = offset * PGL.SIZEOF_FLOAT;
     int sizef = size * PGL.SIZEOF_FLOAT;
     int offseti = offset * PGL.SIZEOF_INT;
     int sizei = size * PGL.SIZEOF_INT;    
 
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointVertexBufferID);
     pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, 3 * offsetf, 3 * sizef, FloatBuffer.wrap(vertices, 0, 3 * size));
 
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointColorBufferID);
     pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, offseti, sizei, IntBuffer.wrap(colors, 0, size));
     
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointSizeBufferID);
     pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, 2 * offsetf, 2 * sizef, FloatBuffer.wrap(attribs, 0, 2 * size));
     
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);    
   }  
 
 
   protected void copyPointVertices(int offset, int size, float[] vertices) {    
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointVertexBufferID);
     pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, 3 * offset * PGL.SIZEOF_FLOAT, 3 * size * PGL.SIZEOF_FLOAT, FloatBuffer.wrap(vertices, 0, 3 * size));
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
   }
     
     
   protected void copyPointColors(int offset, int size, int[] colors) {
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointColorBufferID);
     pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, offset * PGL.SIZEOF_INT, size * PGL.SIZEOF_INT, IntBuffer.wrap(colors, 0, size));
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
   }
     
   
   protected void copyPointAttributes(int offset, int size, float[] attribs) {
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, glPointSizeBufferID);
     pgl.glBufferSubData(PGL.GL_ARRAY_BUFFER, 2 * offset * PGL.SIZEOF_FLOAT, 2 * size * PGL.SIZEOF_FLOAT, FloatBuffer.wrap(attribs, 0, 2 * size));
     pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, 0);
   }
   
   
   protected void copyPointIndices(int offset, int size, short[] indices) {
     pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, glPointIndexBufferID);
     pgl.glBufferSubData(PGL.GL_ELEMENT_ARRAY_BUFFER, offset * PGL.SIZEOF_INDEX, size * PGL.SIZEOF_INDEX, ShortBuffer.wrap(indices, 0, size));
     pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);    
   }    
 
   
   ///////////////////////////////////////////////////////////  
   
   //
   
   // Deletion methods
 
   
   protected void release() {
     deleteFillBuffers();
     deleteLineBuffers();
     deletePointBuffers();
   }  
   
   
   protected void deleteFillBuffers() {
     if (glFillVertexBufferID != 0) {    
       pg.deleteVertexBufferObject(glFillVertexBufferID, context.code());   
       glFillVertexBufferID = 0;
     }    
     
     if (glFillColorBufferID != 0) {    
       pg.deleteVertexBufferObject(glFillColorBufferID, context.code());   
       glFillColorBufferID = 0;
     }    
 
     if (glFillNormalBufferID != 0) {    
       pg.deleteVertexBufferObject(glFillNormalBufferID, context.code());   
       glFillNormalBufferID = 0;
     }     
 
     if (glFillTexCoordBufferID != 0) {    
       pg.deleteVertexBufferObject(glFillTexCoordBufferID, context.code());  
       glFillTexCoordBufferID = 0;
     }    
 
     if (glFillAmbientBufferID != 0) {    
       pg.deleteVertexBufferObject(glFillAmbientBufferID, context.code());   
       glFillAmbientBufferID = 0;
     }    
     
     if (glFillSpecularBufferID != 0) {    
       pg.deleteVertexBufferObject(glFillSpecularBufferID, context.code());   
       glFillSpecularBufferID = 0;
     }    
 
     if (glFillEmissiveBufferID != 0) {    
       pg.deleteVertexBufferObject(glFillEmissiveBufferID, context.code());   
       glFillEmissiveBufferID = 0;
     }     
 
     if (glFillShininessBufferID != 0) {    
       pg.deleteVertexBufferObject(glFillShininessBufferID, context.code());   
       glFillShininessBufferID = 0;
     }        
     
     if (glFillIndexBufferID != 0) {    
       pg.deleteVertexBufferObject(glFillIndexBufferID, context.code());  
       glFillIndexBufferID = 0;
     }   
   }
   
   
   protected void deleteLineBuffers() {
     if (glLineVertexBufferID != 0) {    
       pg.deleteVertexBufferObject(glLineVertexBufferID, context.code());   
       glLineVertexBufferID = 0;
     }    
     
     if (glLineColorBufferID != 0) {    
       pg.deleteVertexBufferObject(glLineColorBufferID, context.code());   
       glLineColorBufferID = 0;
     }    
 
     if (glLineDirWidthBufferID != 0) {    
       pg.deleteVertexBufferObject(glLineDirWidthBufferID, context.code());  
       glLineDirWidthBufferID = 0;
     }    
     
     if (glLineIndexBufferID != 0) {    
       pg.deleteVertexBufferObject(glLineIndexBufferID, context.code());   
       glLineIndexBufferID = 0;
     }  
   }  
   
   
   protected void deletePointBuffers() {
     if (glPointVertexBufferID != 0) {    
       pg.deleteVertexBufferObject(glPointVertexBufferID, context.code());   
       glPointVertexBufferID = 0;
     }    
     
     if (glPointColorBufferID != 0) {    
       pg.deleteVertexBufferObject(glPointColorBufferID, context.code());   
       glPointColorBufferID = 0;
     }    
 
     if (glPointSizeBufferID != 0) {    
       pg.deleteVertexBufferObject(glPointSizeBufferID, context.code());   
       glPointSizeBufferID = 0;
     }    
     
     if (glPointIndexBufferID != 0) {    
       pg.deleteVertexBufferObject(glPointIndexBufferID, context.code());   
       glPointIndexBufferID = 0;
     }  
   }
   
   
   protected void freeCaches() {
     fillVerticesCache = null;
     fillColorsCache = null;
     fillNormalsCache = null;
     fillTexCoordsCache = null;
     fillAmbientCache = null;
     fillSpecularCache = null;
     fillEmissiveCache = null;
     fillShininessCache = null;
     
     lineVerticesCache = null;
     lineColorsCache = null;
     lineAttributesCache = null;
 
     pointVerticesCache = null;
     pointColorsCache = null;
     pointAttributesCache = null;  
   }
   
   
   protected void freeTessData() {
    // The dispose() call will destroy all the geometry
    // arrays but will keep the index caches that are
    // needed by the render methods.
    tess.dipose();    
    
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D)children[i];
         child.freeTessData();
       }
     }
   }
   
   
   protected void freeTessMaps() {
     if (family == GROUP) {
       for (int i = 0; i < childCount; i++) {
         PShape3D child = (PShape3D)children[i];
         child.freeTessMaps();
       }
     } else {
       if (in != null) {
         in.freeTessMaps();
       }
     }
   }
   
   
   ///////////////////////////////////////////////////////////  
   
   //
   
   // Rendering methods
   
   
   public void draw() {
     draw(pg);
   }
   
   
   public void draw(PGraphics g) {
     if (visible) {      
       updateTessellation();
       initBuffers();
       updateGeometry();
       
       if (!cacheTransformations && matrix != null) {
         g.pushMatrix();
         g.applyMatrix(matrix);
       }
     
       if (family == GROUP) {        
         boolean matrixBelow = childHasMatrix;
         boolean diffTexBelow = textures != null && 1 < textures.size();
         
         if (matrixBelow || diffTexBelow) {
           // Some child shape below this group has a non-null matrix
           // transformation assigned to it, so the group cannot
           // be drawn in a single render call.
           // Or, some child shapes below this group use different
           // texture maps, so they cannot rendered in a single call
           // either.
           
           for (int i = 0; i < childCount; i++) {
             ((PShape3D) children[i]).draw(g);
           }        
         } else {
           // None of the child shapes below this group has a matrix
           // transformation applied to them, so we can render everything
           // in a single block.
           // And all have the same texture applied to them.          
           PImage tex = null;
           if (textures != null && textures.size() == 1) {
             tex = (PImage)textures.toArray()[0];
           }
           render(tex);
         }
               
       } else {
         render(texture);
       }
     
       if (!cacheTransformations && matrix != null) {
         g.popMatrix();
       } 
       
       modeCheck();
     }
   }
 
   
   // Render the geometry stored in the root shape as VBOs, for the vertices 
   // corresponding to this shape. Sometimes we can have root == this.
   protected void render(PImage texture) {
     if (root == null) {
       // Some error. Root should never be null. At least it should be this.
       return; 
     }
     
     if (hasPoints) {
       renderPoints();
     }
     
     if (hasLines) {    
       renderLines();    
     }    
     
     if (hasFill) { 
       renderFill(texture);
     }
   }
 
 
   protected void renderPoints() {
     PointShader shader = pg.getPointShader();
     shader.start(); 
     
     IndexCache cache = tess.pointIndexCache;    
     for (int n = 0; n < cache.count; n++) {     
       int ioffset = cache.indexOffset[n];
       int icount = cache.indexCount[n];
       int voffset = cache.vertexOffset[n];
       
       shader.setVertexAttribute(root.glPointVertexBufferID, 3, PGL.GL_FLOAT, 0, 3 * voffset * PGL.SIZEOF_FLOAT);        
       shader.setColorAttribute(root.glPointColorBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 4 * voffset * PGL.SIZEOF_BYTE);    
       shader.setSizeAttribute(root.glPointSizeBufferID, 2, PGL.GL_FLOAT, 0, 2 * voffset * PGL.SIZEOF_FLOAT);      
       
       pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, root.glPointIndexBufferID);      
       pgl.glDrawElements(PGL.GL_TRIANGLES, icount, PGL.INDEX_TYPE, ioffset * PGL.SIZEOF_INDEX);    
       pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);      
     }
     
     shader.stop();
   }  
 
 
   protected void renderLines() {
     LineShader shader = pg.getLineShader();
     shader.start(); 
     
     IndexCache cache = tess.lineIndexCache;
     for (int n = 0; n < cache.count; n++) {     
       int ioffset = cache.indexOffset[n];
       int icount = cache.indexCount[n];
       int voffset = cache.vertexOffset[n];
     
       shader.setVertexAttribute(root.glLineVertexBufferID, 3, PGL.GL_FLOAT, 0, 3 * voffset * PGL.SIZEOF_FLOAT);        
       shader.setColorAttribute(root.glLineColorBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 4 * voffset * PGL.SIZEOF_BYTE);    
       shader.setDirWidthAttribute(root.glLineDirWidthBufferID, 4, PGL.GL_FLOAT, 0, 4 * voffset * PGL.SIZEOF_FLOAT);
       
       pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, root.glLineIndexBufferID);
       pgl.glDrawElements(PGL.GL_TRIANGLES, icount, PGL.INDEX_TYPE, ioffset * PGL.SIZEOF_INDEX);      
       pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);      
     }
     
     shader.stop();
   }  
 
   
   protected void renderFill(PImage textureImage) {
     PTexture tex = null;
     if (textureImage != null) {
       tex = pg.getTexture(textureImage);
       if (tex != null) {
         pgl.enableTexturing(tex.glTarget);          
         pgl.glBindTexture(tex.glTarget, tex.glID);        
       }
     }    
     
     FillShader shader = pg.getFillShader(pg.lights, tex != null);
     shader.start();
     
     IndexCache cache = tess.fillIndexCache;
     for (int n = 0; n < cache.count; n++) {     
       int ioffset = cache.indexOffset[n];
       int icount = cache.indexCount[n];
       int voffset = cache.vertexOffset[n];
       
       shader.setVertexAttribute(root.glFillVertexBufferID, 3, PGL.GL_FLOAT, 0, 3 * voffset * PGL.SIZEOF_FLOAT);        
       shader.setColorAttribute(root.glFillColorBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 4 * voffset * PGL.SIZEOF_BYTE);    
       
       if (pg.lights) {
         shader.setNormalAttribute(root.glFillNormalBufferID, 3, PGL.GL_FLOAT, 0, 3 * voffset * PGL.SIZEOF_FLOAT);
         shader.setAmbientAttribute(root.glFillAmbientBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 4 * voffset * PGL.SIZEOF_BYTE);
         shader.setSpecularAttribute(root.glFillSpecularBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 4 * voffset * PGL.SIZEOF_BYTE);
         shader.setEmissiveAttribute(root.glFillEmissiveBufferID, 4, PGL.GL_UNSIGNED_BYTE, 0, 4 * voffset * PGL.SIZEOF_BYTE);
         shader.setShininessAttribute(root.glFillShininessBufferID, 1, PGL.GL_FLOAT, 0, voffset * PGL.SIZEOF_FLOAT);
       }
       
       if (tex != null) {        
         shader.setTexCoordAttribute(root.glFillTexCoordBufferID, 2, PGL.GL_FLOAT, 0, 2 * voffset * PGL.SIZEOF_FLOAT);
         shader.setTexture(tex);
       }      
       
       pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, root.glFillIndexBufferID);
       pgl.glDrawElements(PGL.GL_TRIANGLES, icount, PGL.INDEX_TYPE, ioffset * PGL.SIZEOF_INDEX);
       pgl.glBindBuffer(PGL.GL_ELEMENT_ARRAY_BUFFER, 0);
     }
     
     shader.stop();
     
     if (tex != null) {
       pgl.glBindTexture(tex.glTarget, 0); 
       pgl.disableTexturing(tex.glTarget);
     }    
   }  
 
   
   ///////////////////////////////////////////////////////////  
   
   // 
   
   // Internal class to store a cache of vertex data used to copy data
   // to the VBOs with fewer calls.
   protected class VertexCache {
     boolean isFloat;
     int ncoords;
     int offset;
     int size;    
     float[] floatData;
     int[] intData;
     
     VertexCache(int ncoords, boolean isFloat) {
       this.ncoords = ncoords;
       this.isFloat = isFloat;
       if (isFloat) {
         this.floatData = new float[ncoords * PGL.DEFAULT_VERTEX_CACHE_SIZE];        
       } else {
         this.intData = new int[ncoords * PGL.DEFAULT_VERTEX_CACHE_SIZE];
       }
       this.offset = 0;
       this.size = 0;      
     }
     
     void clear() {
       offset = 0;
       size = 0;
     }    
     
     void add(int dataOffset, int dataSize, float[] newData) {
       if (size == 0) {
         offset = dataOffset;
       }
       
       int oldSize = floatData.length / ncoords;
       if (size + dataSize >= oldSize) {
         int newSize = expandSize(oldSize, size + dataSize);        
         expand(newSize);
       }
       
       if (dataSize <= PGL.MIN_ARRAYCOPY_SIZE) {
         // Copying elements one by one instead of using arrayCopy is more efficient for
         // few vertices...
         for (int i = 0; i < dataSize; i++) {
           int srcIndex = ncoords * i;
           int destIndex = ncoords * (size + i);
           
           if (ncoords == 2) {
             floatData[destIndex++] = newData[srcIndex++];
             floatData[destIndex  ] = newData[srcIndex  ];
           } else if (ncoords == 3) {
             floatData[destIndex++] = newData[srcIndex++];
             floatData[destIndex++] = newData[srcIndex++];
             floatData[destIndex  ] = newData[srcIndex  ];
           } else if (ncoords == 4) {
             floatData[destIndex++] = newData[srcIndex++];
             floatData[destIndex++] = newData[srcIndex++];
             floatData[destIndex++] = newData[srcIndex++];
             floatData[destIndex  ] = newData[srcIndex  ];            
           } else {
             for (int j = 0; j < ncoords; j++) {
               floatData[destIndex++] = newData[srcIndex++];
             }            
           }
         }
       } else {
         PApplet.arrayCopy(newData, 0, floatData, ncoords * size, ncoords * dataSize);
       }
       
       size += dataSize;
     } 
     
     void add(int dataOffset, int dataSize, int[] newData) {
       if (size == 0) {
         offset = dataOffset;
       }
       
       int oldSize = intData.length / ncoords;
       if (size + dataSize >= oldSize) {
         int newSize = expandSize(oldSize, size + dataSize);        
         expand(newSize);
       }
       
       if (dataSize <= PGL.MIN_ARRAYCOPY_SIZE) {
         // Copying elements one by one instead of using arrayCopy is more efficient for
         // few vertices...
         for (int i = 0; i < dataSize; i++) {
           int srcIndex = ncoords * i;
           int destIndex = ncoords * (size + i);
           
           if (ncoords == 2) {
             intData[destIndex++] = newData[srcIndex++];
             intData[destIndex  ] = newData[srcIndex  ];
           } else if (ncoords == 3) {
             intData[destIndex++] = newData[srcIndex++];
             intData[destIndex++] = newData[srcIndex++];
             intData[destIndex  ] = newData[srcIndex  ];
           } else if (ncoords == 4) {
             intData[destIndex++] = newData[srcIndex++];
             intData[destIndex++] = newData[srcIndex++];
             intData[destIndex++] = newData[srcIndex++];
             intData[destIndex  ] = newData[srcIndex  ];            
           } else {
             for (int j = 0; j < ncoords; j++) {
               intData[destIndex++] = newData[srcIndex++];
             }            
           }
         }
       } else {
         PApplet.arrayCopy(newData, 0, intData, ncoords * size, ncoords * dataSize);
       }
       
       size += dataSize;
     } 
     
     void expand(int n) {
       if (isFloat) {
         expandFloat(n);
       } else {
         expandInt(n);
       }
     }
 
     void expandFloat(int n) {
       float temp[] = new float[ncoords * n];      
       PApplet.arrayCopy(floatData, 0, temp, 0, ncoords * size);
       floatData = temp;      
     }
     
     void expandInt(int n) {
       int temp[] = new int[ncoords * n];      
       PApplet.arrayCopy(intData, 0, temp, 0, ncoords * size);
       intData = temp;      
     }
     
     int expandSize(int currSize, int newMinSize) {
       int newSize = currSize; 
       while (newSize < newMinSize) {
         newSize = newSize << 1;
       }
       return newSize;
     }
     
     boolean hasData() {
       return 0 < size;
     }    
   }  
   
   
   ///////////////////////////////////////////////////////////////////////////   
   
   // OBJ loading
   
   
   protected BufferedReader getBufferedReader(String filename) {
     //BufferedReader retval = papplet.createReader(filename);
 //    BufferedReader retval = null;
 //    if (retval != null) {
 //      return retval;
 //    } else {
 //      PApplet.println("Could not find this file " + filename);
 //      return null;
 //    }
     return null;
   }
   
   
   protected void parseOBJ(BufferedReader reader, ArrayList<PVector> vertices, ArrayList<PVector> normals, ArrayList<PVector> textures, ArrayList<OBJFace> faces, ArrayList<OBJMaterial> materials) {
     Hashtable<String, Integer> mtlTable  = new Hashtable<String, Integer>();
     int mtlIdxCur = -1;
     boolean readv, readvn, readvt;
     try {
       // Parse the line.
       
       readv = readvn = readvt = false;
       String line;
       String gname = "object";
       while ((line = reader.readLine()) != null) {
         
         // The below patch/hack comes from Carlos Tomas Marti and is a
         // fix for single backslashes in Rhino obj files
         
         // BEGINNING OF RHINO OBJ FILES HACK
         // Statements can be broken in multiple lines using '\' at the
         // end of a line.
         // In regular expressions, the backslash is also an escape
         // character.
         // The regular expression \\ matches a single backslash. This
         // regular expression as a Java string, becomes "\\\\".
         // That's right: 4 backslashes to match a single one.
         while (line.contains("\\")) {
           line = line.split("\\\\")[0];
           final String s = reader.readLine();
           if (s != null)
             line += s;
         }
         // END OF RHINO OBJ FILES HACK
         
         String[] elements = line.split("\\s+");        
         // if not a blank line, process the line.
         if (elements.length > 0) {
           if (elements[0].equals("v")) {
             // vertex
             PVector tempv = new PVector(Float.valueOf(elements[1]).floatValue(), Float.valueOf(elements[2]).floatValue(), Float.valueOf(elements[3]).floatValue());
             vertices.add(tempv);
             readv = true;
           } else if (elements[0].equals("vn")) {
             // normal
             PVector tempn = new PVector(Float.valueOf(elements[1]).floatValue(), Float.valueOf(elements[2]).floatValue(), Float.valueOf(elements[3]).floatValue());
             normals.add(tempn);
             readvn = true;
           } else if (elements[0].equals("vt")) {
             // uv
             PVector tempv = new PVector(Float.valueOf(elements[1]).floatValue(), Float.valueOf(elements[2]).floatValue());
             textures.add(tempv);
             readvt = true;
           } else if (elements[0].equals("o")) {
             // Object name is ignored, for now.
           } else if (elements[0].equals("mtllib")) {
             if (elements[1] != null) {
               parseMTL(getBufferedReader(elements[1]), materials, mtlTable); 
             }
           } else if (elements[0].equals("g")) {            
             gname = elements[1];
           } else if (elements[0].equals("usemtl")) {
             // Getting index of current active material (will be applied on all subsequent faces)..
             if (elements[1] != null) {
               String mtlname = elements[1];
               if (mtlTable.containsKey(mtlname)) {
                 Integer tempInt = mtlTable.get(mtlname);
                 mtlIdxCur = tempInt.intValue();
               } else {
                 mtlIdxCur = -1;                
               }
             }
           } else if (elements[0].equals("f")) {
             // Face setting
             OBJFace face = new OBJFace();
             face.matIdx = mtlIdxCur; 
             face.name = gname;
             
             for (int i = 1; i < elements.length; i++) {
               String seg = elements[i];
 
               if (seg.indexOf("/") > 0) {
                 String[] forder = seg.split("/");
 
                 if (forder.length > 2) {
                   // Getting vertex and texture and normal indexes.
                   if (forder[0].length() > 0 && readv) {
                     face.vertIdx.add(Integer.valueOf(forder[0]));
                   }
 
                   if (forder[1].length() > 0 && readvt) {
                     face.texIdx.add(Integer.valueOf(forder[1]));
                   }
 
                   if (forder[2].length() > 0 && readvn) {
                     face.normIdx.add(Integer.valueOf(forder[2]));
                   }
                 } else if (forder.length > 1) {
                   // Getting vertex and texture/normal indexes.
                   if (forder[0].length() > 0 && readv) {
                     face.vertIdx.add(Integer.valueOf(forder[0]));
                   }
  
                   if (forder[1].length() > 0) {
                     if (readvt) {
                       face.texIdx.add(Integer.valueOf(forder[1]));  
                     } else  if (readvn) {
                       face.normIdx.add(Integer.valueOf(forder[1]));
                     }
                     
                   }
                   
                 } else if (forder.length > 0) {
                   // Getting vertex index only.
                   if (forder[0].length() > 0 && readv) {
                     face.vertIdx.add(Integer.valueOf(forder[0]));
                   }
                 }
               } else {
                 // Getting vertex index only.
                 if (seg.length() > 0 && readv) {
                   face.vertIdx.add(Integer.valueOf(seg));
                 }
               }
             }
            
             faces.add(face);
             
           }
         }
       }
 
       if (materials.size() == 0) {
         // No materials definition so far. Adding one default material.
         OBJMaterial defMtl = new OBJMaterial(); 
         materials.add(defMtl);
       }      
       
     } catch (Exception e) {
       e.printStackTrace();
     }
   }
 
   
   protected void parseMTL(BufferedReader reader, ArrayList<OBJMaterial> materials, Hashtable<String, Integer> materialsHash) {
     try {
       String line;
       OBJMaterial currentMtl = null;
       while ((line = reader.readLine()) != null) {
         // Parse the line
         line = line.trim();
 
         String elements[] = line.split("\\s+");
 
         if (elements.length > 0) {
           // Extract the material data.
 
           if (elements[0].equals("newmtl")) {
             // Starting new material.
             String mtlname = elements[1];
             currentMtl = new OBJMaterial(mtlname);
             materialsHash.put(mtlname, new Integer(materials.size()));
             materials.add(currentMtl);
           } else if (elements[0].equals("map_Kd") && elements.length > 1) {
             // Loading texture map.
 //            String texname = elements[1];
             //currentMtl.kdMap = papplet.loadImage(texname);
             currentMtl.kdMap = null;
           } else if (elements[0].equals("Ka") && elements.length > 3) {
             // The ambient color of the material
             currentMtl.ka.x = Float.valueOf(elements[1]).floatValue();
             currentMtl.ka.y = Float.valueOf(elements[2]).floatValue();
             currentMtl.ka.z = Float.valueOf(elements[3]).floatValue();
           } else if (elements[0].equals("Kd") && elements.length > 3) {
             // The diffuse color of the material
             currentMtl.kd.x = Float.valueOf(elements[1]).floatValue();
             currentMtl.kd.y = Float.valueOf(elements[2]).floatValue();
             currentMtl.kd.z = Float.valueOf(elements[3]).floatValue();
           } else if (elements[0].equals("Ks") && elements.length > 3) {
             // The specular color weighted by the specular coefficient
             currentMtl.ks.x = Float.valueOf(elements[1]).floatValue();
             currentMtl.ks.y = Float.valueOf(elements[2]).floatValue();
             currentMtl.ks.z = Float.valueOf(elements[3]).floatValue();
           } else if ((elements[0].equals("d") || elements[0].equals("Tr")) && elements.length > 1) {
             // Reading the alpha transparency.
             currentMtl.d = Float.valueOf(elements[1]).floatValue();
           } else if (elements[0].equals("Ns") && elements.length > 1) {
             // The specular component of the Phong shading model
             currentMtl.ns = Float.valueOf(elements[1]).floatValue();
           } 
           
         }
       }
     } catch (Exception e) {
       e.printStackTrace();
     }    
   }
   
   protected void recordOBJ() {
 //    recordOBJ(objVertices, objNormal, objTexCoords, objFaces, objMaterials);
 //    objVertices = null; 
 //    objNormal = null; 
 //    objTexCoords = null;    
 //    objFaces = null;
 //    objMaterials = null;    
 //    
 //    readFromOBJ = false;
   }
   
   protected void recordOBJ(ArrayList<PVector> vertices, ArrayList<PVector> normals, ArrayList<PVector> textures, ArrayList<OBJFace> faces, ArrayList<OBJMaterial> materials) {
     int mtlIdxCur = -1;
     OBJMaterial mtl = null;
     
     //pg.saveDrawingState();
     
     // The recorded shapes are not merged, they are grouped
     // according to the group names found in the OBJ file.    
     //ogl.mergeRecShapes = false;
     
     // Using RGB mode for coloring.
     pg.colorMode = RGB;
     
     // Strokes are not used to draw the model.
     pg.stroke = false;    
     
     // Normals are automatically computed if not specified in the OBJ file.
     //renderer.autoNormal(true);
     
     // Using normal mode for texture coordinates (i.e.: normalized between 0 and 1).
     pg.textureMode = NORMAL;    
     
     //ogl.beginShapeRecorderImpl();    
     for (int i = 0; i < faces.size(); i++) {
       OBJFace face = faces.get(i);
       
       // Getting current material.
       if (mtlIdxCur != face.matIdx) {
         mtlIdxCur = PApplet.max(0, face.matIdx); // To make sure that at least we get the default material.
         
         mtl = materials.get(mtlIdxCur);
 
         // Setting colors.
         pg.specular(mtl.ks.x * 255.0f, mtl.ks.y * 255.0f, mtl.ks.z * 255.0f);
         pg.ambient(mtl.ka.x * 255.0f, mtl.ka.y * 255.0f, mtl.ka.z * 255.0f);
         if (pg.fill) {
           pg.fill(mtl.kd.x * 255.0f, mtl.kd.y * 255.0f, mtl.kd.z * 255.0f, mtl.d * 255.0f);  
         }        
         pg.shininess(mtl.ns);
         
         if (pg.tint && mtl.kdMap != null) {
           // If current material is textured, then tinting the texture using the diffuse color.
           pg.tint(mtl.kd.x * 255.0f, mtl.kd.y * 255.0f, mtl.kd.z * 255.0f, mtl.d * 255.0f);
         }
       }
 
       // Recording current face.
       if (face.vertIdx.size() == 3) {
         pg.beginShape(TRIANGLES); // Face is a triangle, so using appropriate shape kind.
       } else if (face.vertIdx.size() == 4) {
         pg.beginShape(QUADS);        // Face is a quad, so using appropriate shape kind.
       } else {
         pg.beginShape();  
       }      
       
       //renderer.shapeName(face.name);
       
       for (int j = 0; j < face.vertIdx.size(); j++){
         int vertIdx, normIdx;
         PVector vert, norms;
 
         vert = norms = null;
         
         vertIdx = face.vertIdx.get(j).intValue() - 1;
         vert = vertices.get(vertIdx);
         
         if (j < face.normIdx.size()) {
           normIdx = face.normIdx.get(j).intValue() - 1;
           if (-1 < normIdx) {
             norms = normals.get(normIdx);  
           }
         }
         
         if (mtl != null && mtl.kdMap != null) {
           // This face is textured.
           int texIdx;
           PVector tex = null; 
           
           if (j < face.texIdx.size()) {
             texIdx = face.texIdx.get(j).intValue() - 1;
             if (-1 < texIdx) {
               tex = textures.get(texIdx);  
             }
           }
           
           PTexture texMtl = (PTexture)mtl.kdMap.getCache(pg);
           if (texMtl != null) {     
             // Texture orientation in Processing is inverted.
             texMtl.setFlippedY(true);          
           }
           pg.texture(mtl.kdMap);
           if (norms != null) {
             pg.normal(norms.x, norms.y, norms.z);
           }
           if (tex != null) {
             pg.vertex(vert.x, vert.y, vert.z, tex.x, tex.y);  
           } else {
             pg.vertex(vert.x, vert.y, vert.z);
           }
         } else {
           // This face is not textured.
           if (norms != null) {
             pg.normal(norms.x, norms.y, norms.z);
           }
           pg.vertex(vert.x, vert.y, vert.z);          
         }
       } 
       pg.endShape(CLOSE);
     }
     
     // Allocate space for the geometry that the triangulator has generated from the OBJ model.
     //setSize(ogl.recordedVertices.size());
 //    allocate();
 //    initChildrenData();
 //    updateElement = -1;
     
     width = height = depth = 0;
 //    xmin = ymin = zmin = 10000;
 //    xmax = ymax = zmax = -10000;
     
     //ogl.endShapeRecorderImpl(this);
     //ogl.endShapeRecorderImpl(null);
     
     //pg.restoreDrawingState();    
   }
   
 
   protected class OBJFace {
     ArrayList<Integer> vertIdx;
     ArrayList<Integer> texIdx;
     ArrayList<Integer> normIdx;
     int matIdx;
     String name;
     
     OBJFace() {
       vertIdx = new ArrayList<Integer>();
       texIdx = new ArrayList<Integer>();
       normIdx = new ArrayList<Integer>();
       matIdx = -1;
       name = "";
     }
   }
 
   protected class OBJMaterial {
     String name;
     PVector ka;
     PVector kd;
     PVector ks;
     float d;
     float ns;
     PImage kdMap;
     
     OBJMaterial() {
       this("default");
     }
     
     OBJMaterial(String name) {
       this.name = name;
       ka = new PVector(0.5f, 0.5f, 0.5f);
       kd = new PVector(0.5f, 0.5f, 0.5f);
       ks = new PVector(0.5f, 0.5f, 0.5f);
       d = 1.0f;
       ns = 0.0f;
       kdMap = null;
     }    
   }
 
   
   
 }
