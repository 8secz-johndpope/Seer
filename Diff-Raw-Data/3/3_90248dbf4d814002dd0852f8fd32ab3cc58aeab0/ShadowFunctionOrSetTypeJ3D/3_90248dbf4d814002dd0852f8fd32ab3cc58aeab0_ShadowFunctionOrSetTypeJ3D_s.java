 //
 // ShadowFunctionOrSetTypeJ3D.java
 //
 
 /*
 VisAD system for interactive analysis and visualization of numerical
 data.  Copyright (C) 1996 - 1999 Bill Hibbard, Curtis Rueden, Tom
 Rink, Dave Glowacki, Steve Emmerson, Tom Whittaker, Don Murray, and
 Tommy Jasmin.
  
 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Library General Public
 License as published by the Free Software Foundation; either
 version 2 of the License, or (at your option) any later version.
 
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Library General Public License for more details.
 
 You should have received a copy of the GNU Library General Public
 License along with this library; if not, write to the Free
 Software Foundation, Inc., 59 Temple Place - Suite 330, Boston,
 MA 02111-1307, USA
 */
 
 package visad.java3d;
  
 import visad.*;
 
 import javax.media.j3d.*;
 import javax.vecmath.*;
 
 import java.util.Vector;
 import java.util.Enumeration;
 import java.rmi.*;
 
 import java.awt.*;
 import java.awt.image.*;
 
 /**
    The ShadowFunctionOrSetTypeJ3D is an abstract parent for
    ShadowFunctionTypeJ3D and ShadowSetTypeJ3D.<P>
 */
 public class ShadowFunctionOrSetTypeJ3D extends ShadowTypeJ3D {
 
   ShadowRealTupleTypeJ3D Domain;
   ShadowTypeJ3D Range; // null for ShadowSetTypeJ3D
 
   private Vector AccumulationVector = new Vector();
 
   public ShadowFunctionOrSetTypeJ3D(MathType t, DataDisplayLink link,
                                     ShadowType parent)
       throws VisADException, RemoteException {
     super(t, link, parent);
     if (this instanceof ShadowFunctionTypeJ3D) {
       Domain = (ShadowRealTupleTypeJ3D)
                ((FunctionType) Type).getDomain().buildShadowType(link, this);
       Range = (ShadowTypeJ3D)
               ((FunctionType) Type).getRange().buildShadowType(link, this);
       adaptedShadowType =
         new ShadowFunctionType(t, link, getAdaptedParent(parent),
                        (ShadowRealTupleType) Domain.getAdaptedShadowType(),
                        Range.getAdaptedShadowType());
     }
     else {
       Domain = (ShadowRealTupleTypeJ3D)
                ((SetType) Type).getDomain().buildShadowType(Link, this);
       Range = null;
       adaptedShadowType =
         new ShadowSetType(t, link, getAdaptedParent(parent),
                        (ShadowRealTupleType) Domain.getAdaptedShadowType());
     }
   }
 
   public ShadowRealTupleTypeJ3D getDomain() {
     return Domain;
   }
 
   public ShadowTypeJ3D getRange() {
     return Range;
   }
 
   /** clear AccumulationVector */
   public void preProcess() throws VisADException {
     AccumulationVector.removeAllElements();
     if (this instanceof ShadowFunctionTypeJ3D) {
       Range.preProcess();
     }
   }
 
   /** transform data into a Java3D scene graph;
       add generated scene graph components as children of group;
       value_array are inherited valueArray values;
       default_values are defaults for each display.DisplayRealTypeVector;
       return true if need post-process */
   public boolean doTransform(Group group, Data data, float[] value_array,
                              float[] default_values, DataRenderer renderer)
          throws VisADException, RemoteException {
 
     boolean post = ((ShadowFunctionOrSetType) adaptedShadowType).
                         doTransform(group, data, value_array,
                                     default_values, renderer, this);
     ensureNotEmpty(group);
     return post;
   }
 
   public int textureWidth(int data_width) {
     // must be a power of 2 in Java3D
     int texture_width = 1;
     while (texture_width < data_width) texture_width *= 2;
     return texture_width;
   }
  
   public int textureHeight(int data_height) {
     // must be a power of 2 in Java3D
     int texture_height = 1;
     while (texture_height < data_height) texture_height *= 2;
     return texture_height;
   }
 
   public int textureDepth(int data_depth) {
     // must be a power of 2 in Java3D
     int texture_depth = 1;
     while (texture_depth < data_depth) texture_depth *= 2;
     return texture_depth;
   }
 
   public void adjustZ(float[] coordinates) {
     if (display.getDisplayRenderer().getMode2D()) {
       for (int i=2; i<coordinates.length; i+=3) {
         coordinates[i] = DisplayImplJ3D.BACK2D;
       }
     }
   }
 
   public void setTexCoords(float[] texCoords, float ratiow, float ratioh) {
     // corner 0
     texCoords[0] = 0.0f;
     texCoords[1] = 1.0f;
     // corner 1
     texCoords[2] = ratiow;
     texCoords[3] = 1.0f;
     // corner 2
     texCoords[4] = ratiow;
     texCoords[5] = 1.0f - ratioh;
     // corner 3
     texCoords[6] = 0.0f;
     texCoords[7] = 1.0f - ratioh;
   }
 
   public float[] setTex3DCoords(int length, int axis, float ratiow,
                                 float ratioh, float ratiod) {
     // need to flip Y and Z in X and Y views?
     float[] texCoords = new float[12 * length];
     if (axis == 2) {
       for (int i=0; i<length; i++) {
         int i12 = i * 12;
         float depth = 0.0f + (ratiod - 0.0f) * i / (length - 1.0f);
         // corner 0
         texCoords[i12] = 0.0f;
         texCoords[i12 + 1] = 1.0f;
         texCoords[i12 + 2] = depth;
         // corner 1
         texCoords[i12 + 3] = ratiow;
         texCoords[i12 + 4] = 1.0f;
         texCoords[i12 + 5] = depth;
         // corner 2
         texCoords[i12 + 6] = ratiow;
         texCoords[i12 + 7] = 1.0f - ratioh;
         texCoords[i12 + 8] = depth;
         // corner 3
         texCoords[i12 + 9] = 0.0f;
         texCoords[i12 + 10] = 1.0f - ratioh;
         texCoords[i12 + 11] = depth;
       }
     }
     else if (axis == 1) {
       for (int i=0; i<length; i++) {
         int i12 = i * 12;
         float height = 1.0f - ratioh * i / (length - 1.0f);
         // corner 0
         texCoords[i12] = 0.0f;
         texCoords[i12 + 1] = height;
         texCoords[i12 + 2] = 0.0f;
         // corner 1
         texCoords[i12 + 3] = ratiow;
         texCoords[i12 + 4] = height;
         texCoords[i12 + 5] = 0.0f;
         // corner 2
         texCoords[i12 + 6] = ratiow;
         texCoords[i12 + 7] = height;
         texCoords[i12 + 8] = ratiod;
         // corner 3
         texCoords[i12 + 9] = 0.0f;
         texCoords[i12 + 10] = height;
         texCoords[i12 + 11] = ratiod;
       }
     }
     else if (axis == 0) {
       for (int i=0; i<length; i++) {
         int i12 = i * 12;
         float width = 0.0f + (ratiow - 0.0f) * i / (length - 1.0f);
         // corner 0
         texCoords[i12] = width;
         texCoords[i12 + 1] = 1.0f;
         texCoords[i12 + 2] = 0.0f;
         // corner 1
         texCoords[i12 + 3] = width;
         texCoords[i12 + 4] = 1.0f - ratioh;
         texCoords[i12 + 5] = 0.0f;
         // corner 2
         texCoords[i12 + 6] = width;
         texCoords[i12 + 7] = 1.0f - ratioh;
         texCoords[i12 + 8] = ratiod;
         // corner 3
         texCoords[i12 + 9] = width;
         texCoords[i12 + 10] = 1.0f;
         texCoords[i12 + 11] = ratiod;
       }
     }
     return texCoords;
   }
 
   // WLH 17 March 2000
   private static float EPS = 0.00f;
 
   public float[] setTexStackCoords(int length, int axis, float ratiow,
                                    float ratioh, float ratiod) {
     float[] texCoords = new float[8 * length];
     if (axis == 2) {
       for (int i=0; i<length; i++) {
         int i8 = i * 8;
         // corner 0
         texCoords[i8] = 0.0f + EPS;
         texCoords[i8 + 1] = 1.0f - EPS;
         // corner 1
         texCoords[i8 + 2] = ratiow - EPS;
         texCoords[i8 + 3] = 1.0f - EPS;
         // corner 2
         texCoords[i8 + 4] = ratiow - EPS;
         texCoords[i8 + 5] = 1.0f - ratioh + EPS;
         // corner 3
         texCoords[i8 + 6] = 0.0f + EPS;
         texCoords[i8 + 7] = 1.0f - ratioh + EPS;
       }
     }
     else if (axis == 1) {
       // WLH 23 Feb 2000 - flip Z
       for (int i=0; i<length; i++) {
         int i8 = i * 8;
         // corner 0
         texCoords[i8] = 0.0f + EPS;
         texCoords[i8 + 1] = 1.0f - EPS;
         // corner 1
         texCoords[i8 + 2] = ratiow - EPS;
         texCoords[i8 + 3] = 1.0f - EPS;
         // corner 2
         texCoords[i8 + 4] = ratiow - EPS;
         texCoords[i8 + 5] = 1.0f - ratiod + EPS;
         // corner 3
         texCoords[i8 + 6] = 0.0f + EPS;
         texCoords[i8 + 7] = 1.0f - ratiod + EPS;
       }
     }
     else if (axis == 0) {
       // WLH 23 Feb 2000 - flip Y and Z
       for (int i=0; i<length; i++) {
         int i8 = i * 8;
         // corner 0
         texCoords[i8] = 0.0f + EPS;
         texCoords[i8 + 1] = 1.0f - EPS;
         // corner 1
         texCoords[i8 + 2] = ratioh - EPS;
         texCoords[i8 + 3] = 1.0f - EPS;
         // corner 2
         texCoords[i8 + 4] = ratioh - EPS;
         texCoords[i8 + 5] = 1.0f - ratiod + EPS;
         // corner 3
         texCoords[i8 + 6] = 0.0f + EPS;
         texCoords[i8 + 7] = 1.0f - ratiod + EPS;
       }
     }
 /* WLH 17 March 2000
     if (axis == 2) {
       for (int i=0; i<length; i++) {
         int i8 = i * 8;
         // corner 0
         texCoords[i8] = 0.0f;
         texCoords[i8 + 1] = 1.0f;
         // corner 1
         texCoords[i8 + 2] = ratiow;
         texCoords[i8 + 3] = 1.0f;
         // corner 2
         texCoords[i8 + 4] = ratiow;
         texCoords[i8 + 5] = 1.0f - ratioh;
         // corner 3
         texCoords[i8 + 6] = 0.0f;
         texCoords[i8 + 7] = 1.0f - ratioh;
       }
     }
     else if (axis == 1) {
       // WLH 23 Feb 2000 - flip Z
       for (int i=0; i<length; i++) {
         int i8 = i * 8;
         // corner 0
         texCoords[i8] = 0.0f;
         texCoords[i8 + 1] = 1.0f;
         // corner 1
         texCoords[i8 + 2] = ratiow;
         texCoords[i8 + 3] = 1.0f;
         // corner 2
         texCoords[i8 + 4] = ratiow;
         texCoords[i8 + 5] = 1.0f - ratiod;
         // corner 3
         texCoords[i8 + 6] = 0.0f;
         texCoords[i8 + 7] = 1.0f - ratiod;
       }
     }
     else if (axis == 0) {
       // WLH 23 Feb 2000 - flip Y and Z
       for (int i=0; i<length; i++) {
         int i8 = i * 8;
         // corner 0
         texCoords[i8] = 0.0f;
         texCoords[i8 + 1] = 1.0f;
         // corner 1
         texCoords[i8 + 2] = ratioh;
         texCoords[i8 + 3] = 1.0f;
         // corner 2
         texCoords[i8 + 4] = ratioh;
         texCoords[i8 + 5] = 1.0f - ratiod;
         // corner 3
         texCoords[i8 + 6] = 0.0f;
         texCoords[i8 + 7] = 1.0f - ratiod;
       }
     }
 */
     return texCoords;
   }
 
   public Vector getTextMaps(int i, int[] textIndices) {
     if (i < 0) {
       return ((ShadowTextTypeJ3D) Range).getSelectedMapVector();
     }
     else {
       ShadowTextTypeJ3D text = (ShadowTextTypeJ3D)
         ((ShadowTupleTypeJ3D) Range).getComponent(textIndices[i]);
       return text.getSelectedMapVector();
     }
   }
 
   public void textureToGroup(Object group, VisADGeometryArray array,
                             BufferedImage image, GraphicsModeControl mode,
                             float constant_alpha, float[] constant_color,
                             int texture_width, int texture_height)
          throws VisADException {
     GeometryArray geometry = display.makeGeometry(array);
     // System.out.println("texture geometry");
     // create basic Appearance
     TransparencyAttributes c_alpha = null;
 
     if (constant_alpha == 1.0f) {
       // constant opaque alpha = NONE
       c_alpha = null;
     }
     else if (constant_alpha == constant_alpha) {
       // c_alpha = new TransparencyAttributes(mode.getTransparencyMode(),
       c_alpha = new TransparencyAttributes(TransparencyAttributes.BLENDED,
                                            constant_alpha);
     }
     else {
       c_alpha = new TransparencyAttributes();
       c_alpha.setTransparencyMode(TransparencyAttributes.BLENDED);
     }
     ColoringAttributes c_color = null;
     if (constant_color != null && constant_color.length == 3) {
       c_color = new ColoringAttributes();
       c_color.setColor(constant_color[0], constant_color[1], constant_color[2]);
     }
     Appearance appearance =
       makeAppearance(mode, c_alpha, null, geometry, false);
     // create TextureAttributes
     TextureAttributes texture_attributes = new TextureAttributes();
     // texture_attributes.setTextureMode(TextureAttributes.REPLACE);
     texture_attributes.setTextureMode(TextureAttributes.MODULATE);
     texture_attributes.setPerspectiveCorrectionMode(
                           TextureAttributes.NICEST);
     appearance.setTextureAttributes(texture_attributes);
     // create Texture2D
 // TextureLoader uses 1st argument = 1
 /*
 System.out.println("Texture.BASE_LEVEL = " + Texture.BASE_LEVEL); // 1
 System.out.println("Texture.RGBA = " + Texture.RGBA); // 6
 */
     Texture2D texture = new Texture2D(Texture.BASE_LEVEL, Texture.RGBA,
                                       texture_width, texture_height);
     texture.setCapability(Texture.ALLOW_IMAGE_READ);
     ImageComponent2D image2d =
       new ImageComponent2D(ImageComponent.FORMAT_RGBA, image);
     image2d.setCapability(ImageComponent.ALLOW_IMAGE_READ);
     texture.setImage(0, image2d);
     //
     // from TextureLoader
     // TextureLoader uses 3 for both setMinFilter and setMagFilter
 /*
 System.out.println("Texture.FASTEST = " + Texture.FASTEST); // 0
 System.out.println("Texture.NICEST = " + Texture.NICEST); // 1
 System.out.println("Texture.BASE_LEVEL_POINT = " + Texture.BASE_LEVEL_POINT); // 2
 System.out.println("Texture.BASE_LEVEL_LINEAR = " + Texture.BASE_LEVEL_LINEAR); // 3
 */
 /* for interpolation:
     texture.setMinFilter(Texture.BASE_LEVEL_LINEAR);
     texture.setMagFilter(Texture.BASE_LEVEL_LINEAR);
 */
     texture.setMinFilter(Texture.BASE_LEVEL_POINT);
     texture.setMagFilter(Texture.BASE_LEVEL_POINT);
     texture.setEnable(true);
     // end of from TextureLoader
     //
     Shape3D shape = new Shape3D(geometry, appearance);
     shape.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
     appearance.setTexture(texture);
     appearance.setCapability(Appearance.ALLOW_TEXTURE_READ);
 
     // WLH 6 April 2000
     // ((Group) group).addChild(shape);
     BranchGroup branch = new BranchGroup();
     branch.setCapability(BranchGroup.ALLOW_DETACH);
     branch.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
     branch.addChild(shape);
     if (((Group) group).numChildren() > 0) {
       ((Group) group).setChild(branch, 0);
     }
     else {
       ((Group) group).addChild(branch);
     }
   }
 
   public void texture3DToGroup(Object group, VisADGeometryArray arrayX,
                     VisADGeometryArray arrayY, VisADGeometryArray arrayZ,
                     VisADGeometryArray arrayXrev,
                     VisADGeometryArray arrayYrev,
                     VisADGeometryArray arrayZrev,
                     BufferedImage[] images, GraphicsModeControl mode,
                     float constant_alpha, float[] constant_color,
                     int texture_width, int texture_height,
                     int texture_depth, DataRenderer renderer)
          throws VisADException {
 
     GeometryArray geometryX = display.makeGeometry(arrayX);
     GeometryArray geometryY = display.makeGeometry(arrayY);
     GeometryArray geometryZ = display.makeGeometry(arrayZ);
     GeometryArray geometryXrev = display.makeGeometry(arrayXrev);
     GeometryArray geometryYrev = display.makeGeometry(arrayYrev);
     GeometryArray geometryZrev = display.makeGeometry(arrayZrev);
     // System.out.println("texture geometry");
     // create basic Appearance
     TransparencyAttributes c_alpha = null;
 
     if (constant_alpha == 1.0f) {
       // constant opaque alpha = NONE
       c_alpha = null;
     }
     else if (constant_alpha == constant_alpha) {
       // c_alpha = new TransparencyAttributes(mode.getTransparencyMode(),
       c_alpha = new TransparencyAttributes(TransparencyAttributes.BLENDED,
                                            constant_alpha);
     }
     else {
       c_alpha = new TransparencyAttributes();
       c_alpha.setTransparencyMode(TransparencyAttributes.BLENDED);
     }
     ColoringAttributes c_color = null;
     if (constant_color != null && constant_color.length == 3) {
       c_color = new ColoringAttributes();
       c_color.setColor(constant_color[0], constant_color[1], constant_color[2]);
     }
     Appearance appearance =
       makeAppearance(mode, c_alpha, null, geometryX, true);
     // create TextureAttributes
     TextureAttributes texture_attributes = new TextureAttributes();
     // texture_attributes.setTextureMode(TextureAttributes.REPLACE);
     texture_attributes.setTextureMode(TextureAttributes.MODULATE);
     texture_attributes.setPerspectiveCorrectionMode(
                           TextureAttributes.NICEST);
     appearance.setTextureAttributes(texture_attributes);
     // create Texture2D
 // TextureLoader uses 1st argument = 1
 /*
 System.out.println("Texture.BASE_LEVEL = " + Texture.BASE_LEVEL); // 1
 System.out.println("Texture.RGBA = " + Texture.RGBA); // 6
 */
     Texture3D texture = new Texture3D(Texture.BASE_LEVEL, Texture.RGBA,
                           texture_width, texture_height, texture_depth);
     texture.setCapability(Texture.ALLOW_IMAGE_READ);
     ImageComponent3D image3d =
       new ImageComponent3D(ImageComponent.FORMAT_RGBA, texture_width,
                            texture_height, texture_depth);
     image3d.setCapability(ImageComponent.ALLOW_IMAGE_READ);
     for (int i=0; i<texture_depth; i++) {
       image3d.set(i, images[i]);
       images[i] = null; // take out the garbage
     }
     texture.setImage(0, image3d);
     //
     // from TextureLoader
     // TextureLoader uses 3 for both setMinFilter and setMagFilter
 /*
 System.out.println("Texture.FASTEST = " + Texture.FASTEST); // 0
 System.out.println("Texture.NICEST = " + Texture.NICEST); // 1
 System.out.println("Texture.BASE_LEVEL_POINT = " + Texture.BASE_LEVEL_POINT); // 2
 System.out.println("Texture.BASE_LEVEL_LINEAR = " + Texture.BASE_LEVEL_LINEAR); // 3
 */
 /* for interpolation: */
     texture.setMinFilter(Texture.BASE_LEVEL_LINEAR);
     texture.setMagFilter(Texture.BASE_LEVEL_LINEAR);
 /* for sampling:
     texture.setMinFilter(Texture.BASE_LEVEL_POINT);
     texture.setMagFilter(Texture.BASE_LEVEL_POINT);
 */
     texture.setEnable(true);
     // end of from TextureLoader
 
     // OK to share appearance ??
     Shape3D shapeX = new Shape3D(geometryX, appearance);
     shapeX.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
     Shape3D shapeY = new Shape3D(geometryY, appearance);
     shapeY.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
     Shape3D shapeZ = new Shape3D(geometryZ, appearance);
     shapeZ.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
     Shape3D shapeXrev = new Shape3D(geometryXrev, appearance);
     Shape3D shapeYrev = new Shape3D(geometryYrev, appearance);
     Shape3D shapeZrev = new Shape3D(geometryZrev, appearance);
     appearance.setTexture(texture);
     appearance.setCapability(Appearance.ALLOW_TEXTURE_READ);
 
     Switch swit = (Switch) makeSwitch();
     swit.addChild(shapeX);
     swit.addChild(shapeY);
     swit.addChild(shapeZ);
     swit.addChild(shapeXrev);
     swit.addChild(shapeYrev);
     swit.addChild(shapeZrev);
 
     // WLH 6 April 2000
     // ((Group) group).addChild(swit);
     BranchGroup branch = new BranchGroup();
     branch.setCapability(BranchGroup.ALLOW_DETACH);
     branch.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
     branch.addChild(swit);
     if (((Group) group).numChildren() > 0) {
       ((Group) group).setChild(branch, 0);
     }
     else {
       ((Group) group).addChild(branch);
     }
    ((Group) group).addChild(branch);
 
     ProjectionControlJ3D control =
       (ProjectionControlJ3D) display.getProjectionControl();
     control.addPair(swit, renderer);
   }
 
   public void textureStackToGroup(Object group, VisADGeometryArray arrayX,
                     VisADGeometryArray arrayY, VisADGeometryArray arrayZ,
                     VisADGeometryArray arrayXrev,
                     VisADGeometryArray arrayYrev,
                     VisADGeometryArray arrayZrev,
                     BufferedImage[] imagesX,
                     BufferedImage[] imagesY,
                     BufferedImage[] imagesZ,
                     GraphicsModeControl mode,
                     float constant_alpha, float[] constant_color,
                     int texture_width, int texture_height,
                     int texture_depth, DataRenderer renderer)
          throws VisADException {
 
     GeometryArray[] geometryX = makeGeometrys(arrayX);
     GeometryArray[] geometryY = makeGeometrys(arrayY);
     GeometryArray[] geometryZ = makeGeometrys(arrayZ);
 /* not needed ??
     GeometryArray[] geometryXrev = makeGeometrys(arrayXrev);
     GeometryArray[] geometryYrev = makeGeometrys(arrayYrev);
     GeometryArray[] geometryZrev = makeGeometrys(arrayZrev);
 */
 
     // create Attributes for Appearances
     TransparencyAttributes c_alpha = null;
     if (constant_alpha == 1.0f) {
       // constant opaque alpha = NONE
       c_alpha = null;
     }
     else if (constant_alpha == constant_alpha) {
       // c_alpha = new TransparencyAttributes(mode.getTransparencyMode(),
       c_alpha = new TransparencyAttributes(TransparencyAttributes.BLENDED,
                                            constant_alpha);
     }
     else {
       c_alpha = new TransparencyAttributes();
       c_alpha.setTransparencyMode(TransparencyAttributes.BLENDED);
     }
     ColoringAttributes c_color = null;
     if (constant_color != null && constant_color.length == 3) {
       c_color = new ColoringAttributes();
       c_color.setColor(constant_color[0], constant_color[1], constant_color[2]);
     }
     TextureAttributes texture_attributes = new TextureAttributes();
 
     // WLH 17 March 2000
     // texture_attributes.setTextureMode(TextureAttributes.MODULATE);
     texture_attributes.setTextureMode(TextureAttributes.REPLACE);
 
     texture_attributes.setPerspectiveCorrectionMode(
                           TextureAttributes.NICEST);
 
     int transparencyMode = mode.getTransparencyMode();
 
     OrderedGroup branchX = new OrderedGroup();
     branchX.setCapability(Group.ALLOW_CHILDREN_READ);
     int data_depth = geometryX.length;
     Shape3D[] shapeX = new Shape3D[data_depth];
     for (int i=0; i<data_depth; i++) {
       int width = imagesX[i].getWidth();
       int height = imagesX[i].getHeight();
       Texture2D texture = new Texture2D(Texture.BASE_LEVEL, Texture.RGBA,
                                         width, height);
       texture.setCapability(Texture.ALLOW_IMAGE_READ);
       ImageComponent2D image2d =
         new ImageComponent2D(ImageComponent.FORMAT_RGBA, imagesX[i]);
       image2d.setCapability(ImageComponent.ALLOW_IMAGE_READ);
       texture.setImage(0, image2d);
       Appearance appearance =
         makeAppearance(mode, c_alpha, null, geometryX[i], true);
       appearance.setTextureAttributes(texture_attributes);
       // WLH 17 March 2000
       if (transparencyMode == TransparencyAttributes.FASTEST) {
         texture.setMinFilter(Texture.BASE_LEVEL_POINT);
         texture.setMagFilter(Texture.BASE_LEVEL_POINT);
       }
       else {
         texture.setBoundaryModeS(Texture.CLAMP);
         texture.setBoundaryModeT(Texture.CLAMP);
         texture.setMinFilter(Texture.BASE_LEVEL_LINEAR);
         texture.setMagFilter(Texture.BASE_LEVEL_LINEAR);
       }
       texture.setEnable(true);
       appearance.setTexture(texture);
       appearance.setCapability(Appearance.ALLOW_TEXTURE_READ);
       shapeX[i] = new Shape3D(geometryX[i], appearance);
       shapeX[i].setCapability(Shape3D.ALLOW_APPEARANCE_READ);
       branchX.addChild(shapeX[i]);
     }
     OrderedGroup branchXrev = new OrderedGroup();
     branchXrev.setCapability(Group.ALLOW_CHILDREN_READ);
     for (int i=data_depth-1; i>=0; i--) {
       int width = imagesX[i].getWidth();
       int height = imagesX[i].getHeight();
       Texture2D texture = new Texture2D(Texture.BASE_LEVEL, Texture.RGBA,
                                         width, height);
       texture.setCapability(Texture.ALLOW_IMAGE_READ);
       ImageComponent2D image2d =
         new ImageComponent2D(ImageComponent.FORMAT_RGBA, imagesX[i]);
       image2d.setCapability(ImageComponent.ALLOW_IMAGE_READ);
       texture.setImage(0, image2d);
       Appearance appearance =
         makeAppearance(mode, c_alpha, null, geometryX[i], true);
       appearance.setTextureAttributes(texture_attributes);
       // WLH 17 March 2000
       if (transparencyMode == TransparencyAttributes.FASTEST) {
         texture.setMinFilter(Texture.BASE_LEVEL_POINT);
         texture.setMagFilter(Texture.BASE_LEVEL_POINT);
       }
       else {
         texture.setBoundaryModeS(Texture.CLAMP);
         texture.setBoundaryModeT(Texture.CLAMP);
         texture.setMinFilter(Texture.BASE_LEVEL_LINEAR);
         texture.setMagFilter(Texture.BASE_LEVEL_LINEAR);
       }
       texture.setEnable(true);
       appearance.setTexture(texture);
       appearance.setCapability(Appearance.ALLOW_TEXTURE_READ);
       shapeX[i] = new Shape3D(geometryX[i], appearance);
       shapeX[i].setCapability(Shape3D.ALLOW_APPEARANCE_READ);
       branchXrev.addChild(shapeX[i]);
     }
     shapeX = null;
 
     OrderedGroup branchY = new OrderedGroup();
     branchY.setCapability(Group.ALLOW_CHILDREN_READ);
     int data_height = geometryY.length;
     Shape3D[] shapeY = new Shape3D[data_height];
     for (int i=0; i<data_height; i++) {
       int width = imagesY[i].getWidth();
       int height = imagesY[i].getHeight();
       Texture2D texture = new Texture2D(Texture.BASE_LEVEL, Texture.RGBA,
                                         width, height);
       texture.setCapability(Texture.ALLOW_IMAGE_READ);
       // flip texture on Y axis
       ImageComponent2D image2d =
         new ImageComponent2D(ImageComponent.FORMAT_RGBA, imagesY[i]);
       image2d.setCapability(ImageComponent.ALLOW_IMAGE_READ);
       texture.setImage(0, image2d);
       Appearance appearance =
         makeAppearance(mode, c_alpha, null, geometryY[i], true);
       appearance.setTextureAttributes(texture_attributes);
       // WLH 17 March 2000
       if (transparencyMode == TransparencyAttributes.FASTEST) {
         texture.setMinFilter(Texture.BASE_LEVEL_POINT);
         texture.setMagFilter(Texture.BASE_LEVEL_POINT);
       }
       else {
         texture.setBoundaryModeS(Texture.CLAMP);
         texture.setBoundaryModeT(Texture.CLAMP);
         texture.setMinFilter(Texture.BASE_LEVEL_LINEAR);
         texture.setMagFilter(Texture.BASE_LEVEL_LINEAR);
       }
       texture.setEnable(true);
       appearance.setTexture(texture);
       appearance.setCapability(Appearance.ALLOW_TEXTURE_READ);
       shapeY[i] = new Shape3D(geometryY[i], appearance);
       shapeY[i].setCapability(Shape3D.ALLOW_APPEARANCE_READ);
       branchY.addChild(shapeY[i]);
     }
     OrderedGroup branchYrev = new OrderedGroup();
     branchYrev.setCapability(Group.ALLOW_CHILDREN_READ);
     for (int i=data_height-1; i>=0; i--) {
       int width = imagesY[i].getWidth();
       int height = imagesY[i].getHeight();
       Texture2D texture = new Texture2D(Texture.BASE_LEVEL, Texture.RGBA,
                                         width, height);
       texture.setCapability(Texture.ALLOW_IMAGE_READ);
       // flip texture on Y axis
       ImageComponent2D image2d =
         new ImageComponent2D(ImageComponent.FORMAT_RGBA, imagesY[i]);
       image2d.setCapability(ImageComponent.ALLOW_IMAGE_READ);
       texture.setImage(0, image2d);
       Appearance appearance =
         makeAppearance(mode, c_alpha, null, geometryY[i], true);
       appearance.setTextureAttributes(texture_attributes);
       // WLH 17 March 2000
       if (transparencyMode == TransparencyAttributes.FASTEST) {
         texture.setMinFilter(Texture.BASE_LEVEL_POINT);
         texture.setMagFilter(Texture.BASE_LEVEL_POINT);
       }
       else {
         texture.setBoundaryModeS(Texture.CLAMP);
         texture.setBoundaryModeT(Texture.CLAMP);
         texture.setMinFilter(Texture.BASE_LEVEL_LINEAR);
         texture.setMagFilter(Texture.BASE_LEVEL_LINEAR);
       }
       texture.setEnable(true);
       appearance.setTexture(texture);
       appearance.setCapability(Appearance.ALLOW_TEXTURE_READ);
       shapeY[i] = new Shape3D(geometryY[i], appearance);
       shapeY[i].setCapability(Shape3D.ALLOW_APPEARANCE_READ);
       branchYrev.addChild(shapeY[i]);
     }
     shapeY = null;
 
     OrderedGroup branchZ = new OrderedGroup();
     branchZ.setCapability(Group.ALLOW_CHILDREN_READ);
     int data_width = geometryZ.length;
     Shape3D[] shapeZ = new Shape3D[data_width];
     for (int i=0; i<data_width; i++) {
       int width = imagesZ[i].getWidth();
       int height = imagesZ[i].getHeight();
       Texture2D texture = new Texture2D(Texture.BASE_LEVEL, Texture.RGBA,
                                         width, height);
       texture.setCapability(Texture.ALLOW_IMAGE_READ);
       ImageComponent2D image2d =
         new ImageComponent2D(ImageComponent.FORMAT_RGBA, imagesZ[i]);
       image2d.setCapability(ImageComponent.ALLOW_IMAGE_READ);
       texture.setImage(0, image2d);
       Appearance appearance =
         makeAppearance(mode, c_alpha, null, geometryZ[i], true);
       appearance.setTextureAttributes(texture_attributes);
       // WLH 17 March 2000
       if (transparencyMode == TransparencyAttributes.FASTEST) {
         texture.setMinFilter(Texture.BASE_LEVEL_POINT);
         texture.setMagFilter(Texture.BASE_LEVEL_POINT);
       }
       else {
         texture.setBoundaryModeS(Texture.CLAMP);
         texture.setBoundaryModeT(Texture.CLAMP);
         texture.setMinFilter(Texture.BASE_LEVEL_LINEAR);
         texture.setMagFilter(Texture.BASE_LEVEL_LINEAR);
       }
       texture.setEnable(true);
       appearance.setTexture(texture);
       appearance.setCapability(Appearance.ALLOW_TEXTURE_READ);
       shapeZ[i] = new Shape3D(geometryZ[i], appearance);
       shapeZ[i].setCapability(Shape3D.ALLOW_APPEARANCE_READ);
       branchZ.addChild(shapeZ[i]);
     }
     OrderedGroup branchZrev = new OrderedGroup();
     branchZrev.setCapability(Group.ALLOW_CHILDREN_READ);
     for (int i=data_width-1; i>=0; i--) {
       int width = imagesZ[i].getWidth();
       int height = imagesZ[i].getHeight();
       Texture2D texture = new Texture2D(Texture.BASE_LEVEL, Texture.RGBA,
                                         width, height);
       texture.setCapability(Texture.ALLOW_IMAGE_READ);
       ImageComponent2D image2d =
         new ImageComponent2D(ImageComponent.FORMAT_RGBA, imagesZ[i]);
       image2d.setCapability(ImageComponent.ALLOW_IMAGE_READ);
       texture.setImage(0, image2d);
       Appearance appearance =
         makeAppearance(mode, c_alpha, null, geometryZ[i], true);
       appearance.setTextureAttributes(texture_attributes);
       // WLH 17 March 2000
       if (transparencyMode == TransparencyAttributes.FASTEST) {
         texture.setMinFilter(Texture.BASE_LEVEL_POINT);
         texture.setMagFilter(Texture.BASE_LEVEL_POINT);
       }
       else {
         texture.setBoundaryModeS(Texture.CLAMP);
         texture.setBoundaryModeT(Texture.CLAMP);
         texture.setMinFilter(Texture.BASE_LEVEL_LINEAR);
         texture.setMagFilter(Texture.BASE_LEVEL_LINEAR);
       }
       texture.setEnable(true);
       appearance.setTexture(texture);
       appearance.setCapability(Appearance.ALLOW_TEXTURE_READ);
       shapeZ[i] = new Shape3D(geometryZ[i], appearance);
       shapeZ[i].setCapability(Shape3D.ALLOW_APPEARANCE_READ);
       branchZrev.addChild(shapeZ[i]);
     }
     shapeZ = null;
 
     Switch swit = (Switch) makeSwitch();
     swit.addChild(branchX);
     swit.addChild(branchY);
     swit.addChild(branchZ);
     swit.addChild(branchXrev);
     swit.addChild(branchYrev);
     swit.addChild(branchZrev);
 
     // WLH 6 April 2000
     // ((Group) group).addChild(swit);
     BranchGroup branch = new BranchGroup();
     branch.setCapability(BranchGroup.ALLOW_DETACH);
     branch.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
     branch.addChild(swit);
     if (((Group) group).numChildren() > 0) {
       ((Group) group).setChild(branch, 0);
     }
     else {
       ((Group) group).addChild(branch);
     }
    ((Group) group).addChild(branch);
 
     ProjectionControlJ3D control =
       (ProjectionControlJ3D) display.getProjectionControl();
     control.addPair(swit, renderer);
   }
 
   GeometryArray[] makeGeometrys(VisADGeometryArray array)
                   throws VisADException {
     int count = array.vertexCount;
     int depth = count / 4;
     int color_length = array.colors.length / count;
     int tex_length = array.texCoords.length / count;
 
     GeometryArray[] geometrys = new GeometryArray[depth];
     for (int d=0; d<depth; d++) {
       int i12 = d * 4 * 3;
       int i4c = d * 4 * color_length;
       int i4t = d * 4 * tex_length;
       VisADQuadArray qarray = new VisADQuadArray();
       qarray.vertexCount = 4;
       qarray.coordinates = new float[12];
       qarray.texCoords = new float[tex_length * 4];
       qarray.colors = new byte[color_length * 4];
       qarray.normals = new float[12];
       for (int i=0; i<12; i++) {
         qarray.coordinates[i] = array.coordinates[i12 + i];
         qarray.normals[i] = array.normals[i12 + i];
       }
       for (int i=0; i<4*color_length; i++) {
         qarray.colors[i] = array.colors[i4c + i];
       }
       for (int i=0; i<4*tex_length; i++) {
         qarray.texCoords[i] = array.texCoords[i4t + i];
       }
       geometrys[d] = display.makeGeometry(qarray);
     }
     return geometrys;
   }
 
   public Object makeSwitch() {
     Switch swit = new Switch();
     swit.setCapability(Switch.ALLOW_SWITCH_READ);
     swit.setCapability(Switch.ALLOW_SWITCH_WRITE);
     swit.setCapability(BranchGroup.ALLOW_DETACH);
     swit.setCapability(Group.ALLOW_CHILDREN_READ);
     swit.setCapability(Group.ALLOW_CHILDREN_WRITE);
     return swit;
   }
 
   public Object makeBranch() {
     BranchGroup branch = new BranchGroup();
     branch.setCapability(BranchGroup.ALLOW_DETACH);
     branch.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
     return branch;
   }
 
   public void addToSwitch(Object swit, Object branch)
          throws VisADException {
 /* WLH 18 Aug 98
    empty BranchGroup or Shape3D may cause NullPointerException
    from Shape3DRetained.setLive
 */
     ensureNotEmpty((BranchGroup) branch);
     ((Switch) swit).addChild((BranchGroup) branch);
   }
 
   public void addSwitch(Object group, Object swit, Control control,
                         Set domain_set, DataRenderer renderer)
          throws VisADException {
     ((AVControlJ3D) control).addPair((Switch) swit, domain_set, renderer);
     ((AVControlJ3D) control).init();
     ((Group) group).addChild((Switch) swit);
   }
 
   public boolean recurseRange(Object group, Data data, float[] value_array,
                              float[] default_values, DataRenderer renderer)
          throws VisADException, RemoteException {
     return Range.doTransform((Group) group, data, value_array,
                              default_values, renderer);
   }
 
   public boolean wantIndexed() {
 /* doesn't seem to matter to memory use
     return true;
 */
     return false;
   }
 
 
   /** render accumulated Vector of value_array-s to
       and add to group; then clear AccumulationVector */
   public void postProcess(Group group) throws VisADException {
     if (((ShadowFunctionOrSetType) adaptedShadowType).getFlat()) {
       int LevelOfDifficulty = getLevelOfDifficulty();
       if (LevelOfDifficulty == LEGAL) {
 /*
         Group data_group = null;
         // transform AccumulationVector
         group.addChild(data_group);
 */
         throw new UnimplementedException("terminal LEGAL unimplemented: " +
                                          "ShadowFunctionOrSetTypeJ3D.postProcess");
       }
       else {
         // includes !isTerminal
         // nothing to do
       }
     }
     else {
       if (this instanceof ShadowFunctionTypeJ3D) {
         Range.postProcess(group);
       }
     }
     AccumulationVector.removeAllElements();
   }
 
 }
 
