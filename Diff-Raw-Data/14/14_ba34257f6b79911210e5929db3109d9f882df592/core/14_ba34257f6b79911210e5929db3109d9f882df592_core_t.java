 package skia.javacpp;
 
 import com.googlecode.javacpp.BytePointer;
 import com.googlecode.javacpp.FunctionPointer;
 import com.googlecode.javacpp.IntPointer;
 import com.googlecode.javacpp.Loader;
 import com.googlecode.javacpp.Pointer;
 import com.googlecode.javacpp.ShortPointer;
 import com.googlecode.javacpp.annotation.ByRef;
 import com.googlecode.javacpp.annotation.ByVal;
 import com.googlecode.javacpp.annotation.Cast;
 import com.googlecode.javacpp.annotation.Const;
 import com.googlecode.javacpp.annotation.MemberGetter;
 import com.googlecode.javacpp.annotation.MemberSetter;
 import com.googlecode.javacpp.annotation.Name;
 import com.googlecode.javacpp.annotation.NoDeallocator;
 import com.googlecode.javacpp.annotation.Platform;
 import com.googlecode.javacpp.annotation.Properties;
 
 import java.nio.charset.Charset;
 
 @Properties({
 	@Platform(include={
             "Sk64.h",
             "SkBitmap.h",
             "SkBounder.h",
             "SkCanvas.h",
             "SkClipStack.h",
             "SkColor.h",
             "SkColorFilter.h",
             "SkColorPriv.h",
             "SkColorShader.h",
             "SkData.h",
             "SkDevice.h",
             "SkDrawFilter.h",
             "SkDrawLooper.h",
             "SkImageFilter.h",
             "SkMaskFilter.h",
             "SkMath.h",
             "SkMatrix.h",
 			"SkPaint.h",
             "SkPath.h",
             "SkPathEffect.h",
             "SkPicture.h",
             "SkPoint.h",
             "SkRandom.h",
             "SkRegion.h",
             "SkScalar.h",
             "SkScalerContext.h",
 			"SkShader.h",
             "SkShape.h",
             "SkSize.h",
             "SkStream.h",
             "SkString.h",
             "SkTypeface.h",
 			"SkUnPreMultiply.h"})
 })
 public class core {
 	static { Loader.load(Skia.class); }
 
 	/*
 	 * Sk64.h
 	 */
 	
 	public static class Sk64 extends Pointer {
 		static { Loader.load(Skia.class); }
 		
 		public Sk64() { allocate(); }
 		private native void allocate();
 	}
 
     /*
      * SkBitmap.h
      */
     
 	public static class SkBitmap extends Pointer {
 		static { Loader.load(Skia.class); }
 		
 		//enum Config
         public static final int kNo_Config = 0,
         		kA1_Config = 1,
         		kA8_Config = 2,
         		kIndex8_Config = 3,
         		kRGB_565_Config = 4,
         		kARGB_4444_Config = 5,
         		kARGB_8888_Config = 6,
         		kRLE_Index8_Config = 7;
         
 		public SkBitmap() { allocate(); }
 		private native void allocate();
 
 	    public SkBitmap(SkBitmap src) { allocate(src); }
 	    private native void allocate(@Const @ByRef SkBitmap src);
 	        
 		@Name("operator=")
 		public native @ByRef SkBitmap copy(@Const @ByRef SkBitmap src);
 
 	    public native boolean empty();
 
 	    @Name("isNull")
 	    public native boolean isNullPixels();
 
 	    public native @Cast("SkBitmap::Config") int config();
 
 	    public native int width();
 
 	    public native int height();
 
 	    public native int rowBytes();
 
 	    public native int shiftPerPixel();
 	    public native int bytesPerPixel();
 	    public native int rowBytesAsPixels();
 	    public native @Cast("void*") Pointer getPixels();
 	    public native @Cast("size_t") int getSize();
 	    public native @Cast("size_t") int getSafeSize();
 	    public native @ByVal Sk64 getSize64();
 	    public native @ByVal Sk64 getSafeSize64();
 	    public native boolean isImmutable();
 	    public native void setImmutable();
 	    public native boolean isOpaque();
 	    public native void setIsOpaque(boolean isOpaque);
 	    public native boolean isVolatile();
 	    public native void setIsVolatile(boolean isVolatile);
 	    public native void reset();
 	    public native static int ComputeRowBytes(@Cast("SkBitmap::Config") int c, int width);
 	    public native static int ComputeBytesPerPixel(@Cast("SkBitmap::Config") int c);
 	    public native static int ComputeShiftPerPixel(@Cast("SkBitmap::Config") int c);
 	    public native static @ByVal Sk64 ComputeSize64(@Cast("SkBitmap::Config") int c, int width, int height);
 	    public native static @Cast("size_t") int ComputeSize(@Cast("SkBitmap::Config") int c, int width, int height);
 
         public native void getBounds(SkRect bounds);
         public native void getBounds(SkIRect bounds);
 
         public native void setConfig(@Cast("SkBitmap::Config")int config, int width, int height);
         public native void setConfig(@Cast("SkBitmap::Config")int config, int width, int height, int rowBytes/* = 0*/);
 
         public native void setPixels(Pointer p);
 	    public native void setPixels(Pointer p, SkColorTable ctable/* = NULL*/);
 
 	    public native boolean copyPixelsTo(Pointer dst, @Cast("size_t") int dstSize, int dstRowBytes/* = -1*/, boolean preserveDstPad/* = false*/);
         public native boolean allocPixels();
 	    public native boolean allocPixels(SkColorTable ctable/* = NULL*/);
         public native boolean allocPixels(Allocator allocator, SkColorTable ctable);
 
         public native SkPixelRef pixelRef();
         public native @Cast("size_t") int pixelRefOffset();
         public native SkPixelRef setPixelRef(SkPixelRef pr, @Cast("size_t") int offset/* = 0*/);
 
         public native void lockPixels();
 	    public native void unlockPixels();
 
         public native boolean lockPixelsAreWritable();
 
         public native boolean readyToDraw();
 
         //TODO: SkGpuTexture* getTexture() const;
 
         public native SkColorTable getColorTable();
 
         public native @Cast("uint32_t") int getGenerationID();
 
         public native void notifyPixelsChanged();
 
         public native void eraseARGB(@Cast("U8CPU") int a, @Cast("U8CPU") int r, @Cast("U8CPU") int g, @Cast("U8CPU") int b);
 	    public native void eraseRGB(@Cast("U8CPU") int r, @Cast("U8CPU") int g, @Cast("U8CPU") int b);
 	    public native void eraseColor(@Cast("SkColor") int c);
 
         public native boolean scrollRect(@Const SkIRect subset, int dx, int dy);
         public native boolean scrollRect(@Const SkIRect subset, int dx, int dy,
                         SkRegion inva/*l = NULL*/);
 
         public native @Cast("SkColor") int getColor(int x, int y);
 	    public native Pointer getAddr(int x, int y);
 	    public native @Cast("uint32_t*") IntPointer getAddr32(int x, int y);
 	    public native @Cast("uint16_t*") ShortPointer getAddr16(int x, int y);
 	    public native @Cast("uint8_t*") BytePointer getAddr8(int x, int y);
 	    public native @Cast("uint8_t*") BytePointer getAddr1(int x, int y);
 
         public native @Cast("SkPMColor") int getIndex8Color(int x, int y);
 
         public native boolean extractSubset(SkBitmap dst, @Const @ByRef SkIRect subset);
 
         public native boolean copyTo(SkBitmap dst, @Cast("SkBitmap::Config") int c);
         public native boolean copyTo(SkBitmap dst, @Cast("SkBitmap::Config") int c, Allocator allocator/* = NULL*/);
 
         public native boolean deepCopyTo(SkBitmap dst, @Cast("SkBitmap::Config") int c);
 
         public native boolean canCopyTo(@Cast("SkBitmap::Config") int newConfig);
 
         public native boolean hasMipMap();
         public native void buildMipMap(boolean forceRebuild/* = false*/);
         public native void freeMipMap();
 
         //TODO: int extractMipLevel(SkBitmap* dst, SkFixed sx, SkFixed sy);
 
         public native boolean extractAlpha(SkBitmap dst);
 
         public native boolean extractAlpha(SkBitmap dst, @Const SkPaint paint,
                           SkIPoint offset);
 
         public native boolean extractAlpha(SkBitmap dst, @Const SkPaint paint, Allocator allocator,
                           SkIPoint offset);
 
         public static class Allocator extends SkRefCnt {
             static { Loader.load(Skia.class); }
 
             protected Allocator() {}
             
             public native boolean allocPixelRef(SkBitmap bitmap, SkColorTable table);
         };
 	}
 
     public static class SkColorTable extends SkRefCnt {
         static { Loader.load(Skia.class); }
 
         public SkColorTable(SkColorTable src) {
             allocate(src);
             deallocator(new UnrefDeallocator(this));
         }
         @NoDeallocator
         private native void allocate(@Const @ByRef SkColorTable src);
 
         public SkColorTable(int count) {
             allocate(count);
             deallocator(new UnrefDeallocator(this));
         }
         @NoDeallocator
         private native void allocate(int count);
 
         public SkColorTable(int[] colors) {
             allocate(colors, colors.length);
             deallocator(new UnrefDeallocator(this));
         }
         @NoDeallocator
         private native void allocate(@Cast("const SkPMColor*") int[] colors, int count);
 
         public native @Cast("SkPMColor*") IntPointer lockColors();
         public native void unlockColors(boolean changed);
     }
 
     /*
      * SkBounder.h
      */
 
     public static class SkBounder extends SkRefCnt {
         static { Loader.load(Skia.class); }
 
         public SkBounder() {
             allocate();
             deallocator(new UnrefDeallocator(this));
         }
         @NoDeallocator
         private native void allocate();
 
         public native boolean doIRect(@Const @ByRef SkIRect rect);
         public native boolean doIRectGlyph(@Const @ByRef SkIRect rect, int x, int y, @Const @ByRef SkGlyph glyph);
     }
 
     /*
      * SkCanvas.h
      */
 	
 	public static class SkCanvas extends SkRefCnt {
 		static { Loader.load(Skia.class); }
 		
 		public SkCanvas() {
             allocate();
             deallocator(new UnrefDeallocator(this));
         }
         @NoDeallocator
         private native void allocate();
 
 		public SkCanvas(SkDevice device) {
             allocate(device);
             deallocator(new UnrefDeallocator(this));
         }
 		@NoDeallocator
 		private native void allocate(SkDevice device);
 
         public SkCanvas(SkBitmap bitmap) {
             allocate(bitmap);
             deallocator(new UnrefDeallocator(this));
         }
         @NoDeallocator
         private native void allocate(@Const @ByRef SkBitmap bitmap);
 
 	    public native void flush();
 
         public native @ByVal SkISize getDeviceSize();
         public native SkDevice getDevice();
         public native SkDevice setDevice(SkDevice device);
         public native SkDevice getTopDevice();
         public native SkDevice getTopDevice(boolean updateMatrixClip/* = false*/);
         public native SkDevice setBitmapDevice(@Const @ByRef SkBitmap bitmap);
         public native SkDevice createCompatibleDevice(@Cast("SkBitmap::Config") int config,
                                          int width, int height,
                                          boolean isOpaque);
 
         //enum Config8888
 	    public static final int kNative_Premul_Config8888 = 0,
 	    		kNative_Unpremul_Config8888 = 1,
 	    		kBGRA_Premul_Config8888 = 2,
 	    		kBGRA_Unpremul_Config8888 = 3,
 	    		kRGBA_Premul_Config8888 = 4,
 	    		kRGBA_Unpremul_Config8888 =5;
 	    public native boolean readPixels(SkBitmap bitmap, int x, int y, @Cast("SkCanvas::Config8888") int config8888/* = kNative_Premul_Config8888*/);
 	    public native void writePixels(@Const @ByRef SkBitmap bitmap, int x, int y, @Cast("SkCanvas::Config8888") int config8888/* = kNative_Premul_Config8888*/);
 
 	    //enum SaveFlags
 	    public static final int kMatrix_SaveFlag = 0x01,
 	    		kClip_SaveFlag = 0x02,
 	    		kHasAlphaLayer_SaveFlag = 0x04,
 	    		kFullColorLayer_SaveFlag = 0x08,
 	    		kClipToLayer_SaveFlag = 0x10,
 	    		kMatrixClip_SaveFlag = 0x03,
 	    		kARGB_NoClipLayer_SaveFlag = 0x0F,
 	    		kARGB_ClipLayer_SaveFlag = 0x1F;
 
         public native int save();
         public native int save(@Cast("SkCanvas::SaveFlags") int flags/* = kMatrixClip_SaveFlag*/);
 
         public native int saveLayer(@Const SkRect bounds, @Const SkPaint paint);
         public native int saveLayer(@Const SkRect bounds, @Const SkPaint paint, @Cast("SkCanvas::SaveFlags") int flags/* = kARGB_ClipLayer_SaveFlag*/);
 
         public native int saveLayerAlpha(@Const SkRect bounds, @Cast("U8CPU") int alpha);
         public native int saveLayerAlpha(@Const SkRect bounds, @Cast("U8CPU") int alpha, @Cast("SkCanvas::SaveFlags") int flags/* = kARGB_ClipLayer_SaveFlag*/);
 
 	    public native void restore();
 	    public native int getSaveCount();
 	    public native void restoreToCount(int saveCount);
 	    public native boolean isDrawingToLayer();
 
 	    public native boolean translate(@Cast("SkScalar") float dx, @Cast("SkScalar") float dy);
 	    public native boolean scale(@Cast("SkScalar") float sx, @Cast("SkScalar") float sy);
 	    public native boolean rotate(@Cast("SkScalar") float degrees);
 	    public native boolean skew(@Cast("SkScalar") float sx, @Cast("SkScalar") float sy);
 	    public native boolean concat(@Const @ByRef SkMatrix matrix);
 	    public native void setMatrix(@Const @ByRef SkMatrix matrix);
 	    public native void resetMatrix();
 
         public native boolean clipRect(@Const @ByRef SkRect rect);
         public native boolean clipRect(@Const @ByRef SkRect rect, @Cast("SkRegion::Op") int op);
         public native boolean clipRect(@Const @ByRef SkRect rect,
                               @Cast("SkRegion::Op") int op/* = SkRegion::kIntersect_Op*/,
                               boolean doAntiAlias/* = false*/);
 
         public native boolean clipPath(@Const @ByRef SkPath path);
         public native boolean clipPath(@Const @ByRef SkPath path, @Cast("SkRegion::Op") int op);
         public native boolean clipPath(@Const @ByRef SkPath path,
                               @Cast("SkRegion::Op") int op/* = SkRegion::kIntersect_Op*/,
                               boolean doAntiAlias/* = false*/);
 
         public native boolean clipRegion(@Const @ByRef SkRegion deviceRgn,
                                 @Cast("SkRegion::Op") int op/* = SkRegion::kIntersect_Op*/);
 
         public native boolean setClipRegion(@Const @ByRef SkRegion deviceRgn);
 
         //enum EdgeType
         public static final int kBW_EdgeType = 0,
             kAA_EdgeType = 1;
 
         public native boolean quickReject(@Const @ByRef SkRect rect, @Cast("SkCanvas::EdgeType") int et);
 
         public native boolean quickReject(@Const @ByRef SkPath path, @Cast("SkCanvas::EdgeType") int et);
 
         public native boolean quickRejectY(@Cast("SkScalar") float top, @Cast("SkScalar") float bottom, @Cast("SkCanvas::EdgeType") int et);
 
         public native boolean getClipBounds(SkRect bounds, @Cast("SkCanvas::EdgeType") int et/* = kAA_EdgeType*/);
 
         public native boolean getClipDeviceBounds(SkIRect bounds);
 
         public native void drawARGB(@Cast("U8CPU") int a, @Cast("U8CPU") int r, @Cast("U8CPU") int g, @Cast("U8CPU") int b,
                       @Cast("SkXfermode::Mode") int mode/* = SkXfermode::kSrcOver_Mode*/);
 
         public native void drawColor(@Cast("SkColor") int color);
 	    public native void drawColor(@Cast("SkColor") int color, @Cast("SkXfermode::Mode") int mode/* = SkXfermode::kSrcOver_Mode*/);
 		public native void clear(@Cast("SkColor") int color);
 	    public native void drawPaint(@Const @ByRef SkPaint paint);
 	    // enum PointMode
 	    public static final int kPoints_PointMode = 0,
 	    		kLines_PointMode = 1,
 	    		kPolygon_PointMode = 2;
 	    public void drawPoints(int mode, SkPoint[] pts, SkPaint paint) {
 	    	drawPoints(mode, pts.length, SkPoint.constArray(pts), paint);
 	    }
 	    public native void drawPoints(@Cast("SkCanvas::PointMode") int mode, @Cast("size_t") int count, @Const SkPoint pts, @Const @ByRef SkPaint paint);
 	    public native void drawPoint(@Cast("SkScalar") float x, @Cast("SkScalar") float y, @Const @ByRef SkPaint paint);
 	    public native void drawPoint(@Cast("SkScalar") float x, @Cast("SkScalar") float y, @Cast("SkColor") int color);
 	    public native void drawLine(@Cast("SkScalar") float x0, @Cast("SkScalar") float y0, @Cast("SkScalar") float x1, @Cast("SkScalar") float y1, @Const @ByRef SkPaint paint);
 		public native void drawRect(@Const @ByRef SkRect rect, @Const @ByRef SkPaint paint);
 	    public native void drawIRect(@Const @ByRef SkIRect rect, @Const @ByRef SkPaint paint);
 	    public native void drawRectCoords(@Cast("SkScalar") float left, @Cast("SkScalar") float top, @Cast("SkScalar") float right, @Cast("SkScalar") float bottom, @Const @ByRef SkPaint paint);
 	    public native void drawOval(@Const @ByRef SkRect oval, @Const @ByRef SkPaint paint);
 	    public native void drawCircle(@Cast("SkScalar") float cx, @Cast("SkScalar") float cy, @Cast("SkScalar") float radius, @Const @ByRef SkPaint paint);
 	    public native void drawArc(@Const @ByRef SkRect oval, @Cast("SkScalar") float startAngle, @Cast("SkScalar") float sweepAngle, boolean useCenter, @Const @ByRef SkPaint paint);
 	    public native void drawRoundRect(@Const @ByRef SkRect rect, @Cast("SkScalar") float rx, @Cast("SkScalar") float ry, @Const @ByRef SkPaint paint);
 	    public native void drawPath(@Const @ByRef SkPath path, @Const @ByRef SkPaint paint);
         public native void drawBitmap(@Const @ByRef SkBitmap bitmap, @Cast("SkScalar") float left, @Cast("SkScalar") float top);
         public native void drawBitmap(@Const @ByRef SkBitmap bitmap, @Cast("SkScalar") float left, @Cast("SkScalar") float top, @Const SkPaint paint/* = NULL*/);
         public native void drawBitmapRect(@Const @ByRef SkBitmap bitmap, @Const SkIRect src,
                                           @Const @ByRef SkRect dst);
         public native void drawBitmapRect(@Const @ByRef SkBitmap bitmap, @Const SkIRect src,
                                     @Const @ByRef SkRect dst, @Const SkPaint paint/* = NULL*/);
         public native void drawBitmapMatrix(@Const @ByRef SkBitmap bitmap, @Const @ByRef SkMatrix m,
                                       @Const SkPaint paint/* = NULL*/);
         public native void drawBitmapNine(@Const @ByRef SkBitmap bitmap, @Const @ByRef SkIRect center,
                                     @Const @ByRef SkRect dst, @Const SkPaint paint/* = NULL*/);
         public native void drawSprite(@Const @ByRef SkBitmap bitmap, int left, int top,
                                 @Const SkPaint paint/* = NULL*/);
 
 	    public void drawText(String text, float x,  float y, SkPaint paint) {
     		Pointer ptr = SkPaint.encodeText(text, paint.getTextEncoding());
 	    	drawText(ptr, ptr.capacity(), x, y, paint);
 	    }
 	    public native void drawText(@Const Pointer text, @Cast("size_t") int byteLength, @Cast("SkScalar") float x, @Cast("SkScalar") float y, @Const @ByRef SkPaint paint);
 
 	    public native void drawPosText(@Const Pointer text, @Cast("size_t") int byteLength, @Const SkPoint pos, @Const @ByRef SkPaint paint);
 
 	    public native void drawPosTextH(@Const Pointer text, @Cast("size_t") int byteLength, @Cast("const SkScalar*") float[] xpos, @Cast("SkScalar") float constY, @Const @ByRef SkPaint paint);
 
 	    public native void drawTextOnPathHV(@Const Pointer text, @Cast("size_t") int byteLength, @Const @ByRef SkPath path, @Cast("SkScalar") float hOffset, @Cast("SkScalar") float vOffset, @Const @ByRef SkPaint paint);
 
         public void drawTextOnPath(String text, SkPath path, SkMatrix matrix, SkPaint paint) {
             Pointer ptr = SkPaint.encodeText(text, paint.getTextEncoding());
             drawTextOnPath(ptr, ptr.capacity(), path, matrix, paint);
         }
 	    public native void drawTextOnPath(@Const Pointer text, @Cast("size_t") int byteLength, @Const @ByRef SkPath path, @Const SkMatrix matrix, @Const @ByRef SkPaint paint);
 
         public native void drawPicture(@ByRef SkPicture picture);
 
         //enum VertexMode
         public static final int kTriangles_VertexMode = 0,
             kTriangleStrip_VertexMode = 1,
             kTriangleFan_VertexMode = 2;
 
         public native void drawVertices(@Cast("SkCanvas::VertexMode") int vmode, int vertexCount,
                                   @Const SkPoint vertices, @Const SkPoint texs,
                                   @Cast("const SkColor*") int[] colors, SkXfermode xmode,
                                   @Cast("const uint16_t*") short[] indices, int indexCount,
                                   @Const @ByRef SkPaint paint);
 
         public native void drawData(@Const Pointer data, @Cast("size_t") int length);
 
         public native SkBounder  getBounder();
 
         public native SkBounder setBounder(SkBounder bounder);
 
         public native SkDrawFilter getDrawFilter();
 
         public native SkDrawFilter setDrawFilter(SkDrawFilter filter);
 
 	    public native @Const @ByRef SkMatrix getTotalMatrix();
 
         //enum ClipType
         public static final int kEmpty_ClipType = 0,
             kRect_ClipType = 1,
             kComplex_ClipType = 2;
 
         public native @Cast("SkCanvas::ClipType") int getClipType();
 
        @Deprecated
         public native @Const @ByRef SkRegion getTotalClip();
 
         public native @Const @ByRef SkClipStack getTotalClipStack();
 
         public native void setExternalMatrix(@Const SkMatrix mat/* = NULL*/);
     }
 
     /*
      * SkClipStack.h
      */
 
     public static class SkClipStack extends Pointer {
         static { Loader.load(Skia.class); }
 
         public SkClipStack() { allocate(); };
         private native void allocate();
 
         public SkClipStack(SkClipStack b) { allocate(b); };
         private native void allocate(@Const @ByRef SkClipStack b);
     }
 
     /*
      * SkColor.h
      */
 
 //    typedef uint8_t SkAlpha;
 //    typedef uint32_t SkColor;
 
     public static int SkColorSetARGB(int a, int r, int g, int b) {
 		return (0xFF & a) << 24 | (0xFF & r) << 16 | (0xFF  & g) << 8 | (0xFF & b);
 	}
 	
 	public static int SkColorSetRGB(int r, int g, int b) {
 		return SkColorSetARGB(0xFF, r, g, b);
 	}
 	
 	public static int SkColorGetA(int color) {
 		return (color >>> 24) & 0xFF;
 	}
 
 	public static int SkColorGetR(int color) {
 		return (color >>> 16) & 0xFF;
 	}
 	
 	public static int SkColorGetG(int color) {
 		return (color >>> 8) & 0xFF;
 	}
 	
 	public static int SkColorGetB(int color) {
 		return (color >>> 0) & 0xFF;
 	}
 	
 	public static int SkColorSetA(int c, int a) {
 		return c & 0x00FFFFFF | (a << 24);
 	}
 	
 	public static final int SK_ColorBLACK = 0xFF000000,
 			SK_ColorDKGRAY = 0xFF444444,
 			SK_ColorGRAY = 0xFF888888,
 			SK_ColorLTGRAY = 0xFFCCCCCC,
 			SK_ColorWHITE = 0xFFFFFFFF,
 			SK_ColorRED = 0xFFFF0000,
 			SK_ColorGREEN = 0xFF00FF00,
 			SK_ColorBLUE = 0xFF0000FF,
 			SK_ColorYELLOW = 0xFFFFFF00,
 			SK_ColorCYAN = 0xFF00FFFF,
 			SK_ColorMAGENTA = 0xFFFF00FF;
 
 	public native static void SkRGBToHSV(@Cast("U8CPU") int red, @Cast("U8CPU") int green, @Cast("U8CPU") int blue, @Cast("SkScalar*") float[] hsv);
 	public native static void SkColorToHSV(@Cast("SkColor") int color, @Cast("SkScalar*") float[] hsv);
 	public native static @Cast("SkColor") int SkHSVToColor(@Cast("U8CPU") int alpha, @Cast("SkScalar*") float[] hsv);
 	public native static @Cast("SkColor") int SkHSVToColor(@Cast("SkScalar*") float[] hsv);
 
 //    typedef uint32_t SkPMColor;
 
     public native static @Cast("SkPMColor") int SkPreMultiplyARGB(@Cast("U8CPU") int  a, @Cast("U8CPU") int r, @Cast("U8CPU") int g, @Cast("U8CPU") int b);
 	public native static @Cast("SkPMColor") int SkPreMultiplyColor(@Cast("SkColor") int c);
 
 	public static class SkXfermodeProc extends FunctionPointer {
 		static { Loader.load(Skia.class); }
 
 		protected SkXfermodeProc() { allocate(); }
 		protected final native void allocate();
 
 		public native @Cast("SkPMColor") int call(@Cast("SkPMColor") int src, @Cast("SkPMColor") int dst);
 	}
 	
 	public static class SkXfermodeProc16 extends FunctionPointer {
 		static { Loader.load(Skia.class); }
 
 		protected SkXfermodeProc16() { allocate(); }
 		protected final native void allocate();
 
 		public native @Cast("uint16_t") short call(@Cast("SkPMColor") int src, @Cast("uint16_t") short dst);
 	}
 	
 	/*
 	 * SkColorFilter.h
 	 */
 	
 	public static class SkColorFilter extends SkFlattenable {
 		static { Loader.load(Skia.class); }
 
         protected SkColorFilter() {}
 
         public native static SkColorFilter CreateModeFilter(@Cast("SkColor") int c, @Cast("SkXfermode::Mode") int mode);
 	}
 	
 	/*
 	 * SkColorPriv.h
 	 */
 	
 	public static final int SK_A32_BITS = 8,
 			SK_R32_BITS = 8,
 			SK_G32_BITS = 8,
 			SK_B32_BITS = 8;
 
 	public static final int SK_A32_MASK = ((1 << SK_A32_BITS) - 1),
 			SK_R32_MASK = ((1 << SK_R32_BITS) - 1),
 			SK_G32_MASK = ((1 << SK_G32_BITS) - 1),
 			SK_B32_MASK = ((1 << SK_B32_BITS) - 1);
 
     public static final int SkGetPackedA32(int packed) { return ((packed) << (24 - SK_A32_SHIFT)) >>> 24;}
     public static final int SkGetPackedR32(int packed) { return ((packed) << (24 - SK_R32_SHIFT)) >>> 24;}
     public static final int SkGetPackedG32(int packed) { return ((packed) << (24 - SK_G32_SHIFT)) >>> 24;}
     public static final int SkGetPackedB32(int packed) { return ((packed) << (24 - SK_B32_SHIFT)) >>> 24;}
 
     public native static @Cast("SkPMColor") int SkPackARGB32(@Cast("U8CPU") int a, @Cast("U8CPU") int r, @Cast("U8CPU") int g, @Cast("U8CPU") int b);
 
 	/*
 	 * SkColorShader.h
 	 */
 	
 	public static class SkColorShader extends SkShader {
 		static { Loader.load(Skia.class); }
 		
 		public SkColorShader() {
             allocate();
             deallocator(new UnrefDeallocator(this));
         }
         @NoDeallocator
         private native void allocate();
 
 		public SkColorShader(int c) {
             allocate(c);
             deallocator(new UnrefDeallocator(this));
         }
 		@NoDeallocator
 		private native void allocate(@Cast("SkColor") int c);
 	}
     
     /*
      * SkData.h
      */
     
     public static class SkData extends SkRefCnt {
         static { Loader.load(Skia.class); }
     }
 	
 	/*
 	 * SkDevice.h
 	 */
 	
 	public static class SkDevice extends SkRefCnt {
 		static { Loader.load(Skia.class); }
 		
 		public SkDevice(SkBitmap bitmap) {
             allocate(bitmap);
             deallocator(new UnrefDeallocator(this));
         }
         @NoDeallocator
         private native void allocate(@Const @ByRef SkBitmap bitmap);
 
         public SkDevice(int config, int width, int height, boolean isOpaque) {
             allocate(config, width, height, isOpaque);
             deallocator(new UnrefDeallocator(this));
         }
 		@NoDeallocator
 		private native void allocate(@Cast("SkBitmap::Config") int config, int width, int height, boolean isOpaque/* = false*/);
 
         public native @Const @ByRef SkBitmap accessBitmap(boolean changePixels);
     }
 
     /*
      * SkDrawFilter.h
      */
 
     public static class SkDrawFilter extends SkRefCnt {
         static { Loader.load(Skia.class); }
 
         protected SkDrawFilter() {}
 
         //enum Type
         public static final int kPaint_Type = 0,
             kPoint_Type = 1,
             kLine_Type = 2,
             kBitmap_Type = 3,
             kRect_Type = 4,
             kPath_Type = 5,
             kText_Type = 6;
 
         public native void filter(SkPaint paint, @Cast("SkDrawFilter::Type") int type);
     }
 
 	/*
 	 * SkDrawLooper.h
 	 */
 	
 	public static class SkDrawLooper extends SkFlattenable {
 		static { Loader.load(Skia.class); }
 
 		protected SkDrawLooper() {}
 	}
 	
 	/*
 	 * SkFlattenable.h
 	 */
 
 	public static class SkFlattenable extends SkRefCnt {
 		static { Loader.load(Skia.class); }
 		
 		public static class Factory extends FunctionPointer {
 			static { Loader.load(Skia.class); }
 
 			protected Factory() { allocate(); }
 			protected final native void allocate();
 
 			public native SkFlattenable call(@ByRef SkFlattenableReadBuffer buffer);
 		}
 		
 		protected SkFlattenable() {}
 		
 		public native Factory getFactory();

 	    public native static Factory NameToFactory(String name);
 	    public native static String FactoryToName(Factory factory);
 	    public native static void Register(String name, Factory factory);
 	}
 	
 	public static class SkFlattenableReadBuffer extends Pointer {
 		static { Loader.load(Skia.class); }
 		
		protected SkFlattenableReadBuffer() {}
 	}
 	
 	public static class SkFlattenableWriteBuffer extends Pointer {
 		static { Loader.load(Skia.class); }
 		
		protected SkFlattenableWriteBuffer() {}
 	}
 	
 	/*
 	 * SkImageFilter.h
 	 */
 	
 	public static class SkImageFilter extends SkFlattenable {
 		static { Loader.load(Skia.class); }
 		
 		protected SkImageFilter() {}
 	}
 
     /*
      * SkMask.h
      */
 
     public static class SkMask extends Pointer {
         static { Loader.load(Skia.class); }
 
         public SkMask() { allocate(); }
         private native void allocate();
 
         //enum Format
         public static final int kBW_Format = 0,
             kA8_Format = 1,
             k3D_Format = 2,
             kARGB32_Format = 3,
             kLCD16_Format = 4,
             kLCD32_Format = 5;
 
         public static final int kCountMaskFormats = kLCD32_Format + 1;
 
         //TODO:
 //        uint8_t*    fImage;
 //        SkIRect     fBounds;
 //        uint32_t    fRowBytes;
 //        Format      fFormat;
 
         public native boolean isEmpty();
 
         public native @Cast("size_t") int computeImageSize();
 
         public native @Cast("size_t") int computeTotalImageSize();
 
         public native @Cast("uint8_t*") BytePointer getAddr1(int x, int y);
 
         public native @Cast("uint8_t*") BytePointer getAddr8(int x, int y);
 
         public native @Cast("uint16_t*") ShortPointer getAddrLCD16(int x, int y);
 
         public native @Cast("uint32_t*") IntPointer getAddrLCD32(int x, int y);
 
         public native Pointer getAddr(int x, int y);
 
         public native static @Cast("uint8_t*") BytePointer AllocImage(@Cast("size_t") int bytes);
         public native static void FreeImage(Pointer image);
 
         //enum CreateMode
         public static final int kJustComputeBounds_CreateMode = 0,
             kJustRenderImage_CreateMode = 1,
             kComputeBoundsAndRenderImage_CreateMode = 2;
     };
 
     /*
     * SkMaskFilter.h
     */
 
     public static class SkMaskFilter extends SkFlattenable {
         static { Loader.load(Skia.class); }
 
         protected SkMaskFilter() {}
 
         public native @Cast("SkMask::Format") int getFormat();
 
         public native boolean filterMask(SkMask dst, @Const @ByRef SkMask src, @Const @ByRef SkMatrix mat,
                                 SkIPoint margin);
 
         //enum BlurType
         public static final int kNone_BlurType = 0,
             kNormal_BlurType = 1,
             kSolid_BlurType = 2,
             kOuter_BlurType = 3,
             kInner_BlurType = 4;
 
         public static class BlurInfo extends Pointer {
             static { Loader.load(Skia.class); }
 
             public BlurInfo() { allocate(); }
             private native void allocate();
 
             @MemberSetter
             public native void fRadius(@Cast("SkScalar") float radius);
             @MemberSetter
             public native void fIgnoreTransform(boolean ignoreTransform);
             @MemberSetter
             public native void fHighQuality(boolean quality);
         };
 
         public native @Cast("SkMaskFilter::BlurType") int asABlur(BlurInfo blurInfo);
 
         public native void computeFastBounds(@Const @ByRef SkRect src, SkRect dest);
     };
 
 /*
 * SkMath.h
 */
 
     public native static int SkNextPow2(int value);
 
     public native static int SkNextLog2(@Cast("uint32_t") int value);
 
     public native static boolean SkIsPow2(int value);
 
     /*
       * SkMatrix.h
       */
 
     //typedef SkScalar SkPersp;
     public static float SkScalarToPersp(float x) { return x; }
     public static float SkPerspToScalar(float x) { return x; }
 
     public static class SkMatrix extends Pointer {
 		static { Loader.load(Skia.class); }
 		
 		public SkMatrix() { allocate(); }
 		private native void allocate();
 
 	    //enum TypeMask
 		public static final int kIdentity_Mask = 0,
 				kTranslate_Mask = 0x01,
 				kScale_Mask = 0x02,
 				kAffine_Mask = 0x04,
 				kPerspective_Mask = 0x08;
 	    public native @Cast("SkMatrix::TypeMask") int getType();
 	    public native boolean isIdentity();
 	    public native boolean rectStaysRect();
 	    public native boolean preservesAxisAlignment();
 	    public native boolean hasPerspective();
 	    
 	    public static final int kMScaleX = 0,
 	    		kMSkewX = 1,
 		        kMTransX = 2,
 		        kMSkewY = 3,
 		        kMScaleY = 4,
 		        kMTransY = 5,
 		        kMPersp0 = 6,
 		        kMPersp1 = 7,
 		        kMPersp2 = 8;
 	    
 	    public static final int kAScaleX = 0,
 	    		kASkewY = 1,
 		        kASkewX = 2,
 		        kAScaleY = 3,
 		        kATransX = 4,
 		        kATransY = 5;
 	    
 	    public native @Cast("SkScalar") float get(int index);
 	    public native @Cast("SkScalar") float getScaleX();
 	    public native @Cast("SkScalar") float getScaleY();
 	    public native @Cast("SkScalar") float getSkewY();
 	    public native @Cast("SkScalar") float getSkewX();
 	    public native @Cast("SkScalar") float getTranslateX();
 	    public native @Cast("SkScalar") float getTranslateY();
 	    public native @Cast("SkPersp") float getPerspX();
 	    public native @Cast("SkPersp") float getPerspY();
 	    
 	    public native void set(int index, @Cast("SkScalar") float value);
 	    public native void setScaleX(@Cast("SkScalar") float v);
 	    public native void setScaleY(@Cast("SkScalar") float v);
 	    public native void setSkewY(@Cast("SkScalar") float v);
 	    public native void setSkewX(@Cast("SkScalar") float v);
 	    public native void setTranslateX(@Cast("SkScalar") float v);
 	    public native void setTranslateY(@Cast("SkScalar") float v);
 	    public native void setPerspX(@Cast("SkPersp") float v);
 	    public native void setPerspY(@Cast("SkPersp") float v);
 	    public native void setAll(@Cast("SkScalar") float scaleX, @Cast("SkScalar") float skewX, @Cast("SkScalar") float transX,
 	    		@Cast("SkScalar") float skewY, @Cast("SkScalar") float scaleY, @Cast("SkScalar") float transY,
 	    		@Cast("SkPersp") float persp0, @Cast("SkPersp") float persp1, @Cast("SkPersp") float persp2);
 	    
 	    public native void reset();
 	    public native void setIdentity();
 
 	    public native void setTranslate(@Cast("SkScalar") float dx, @Cast("SkScalar") float dy);
 	    public native void setScale(@Cast("SkScalar") float sx, @Cast("SkScalar") float sy, @Cast("SkScalar") float px, @Cast("SkScalar") float py);
 	    public native void setScale(@Cast("SkScalar") float sx, @Cast("SkScalar") float sy);
 	    public native boolean setIDiv(int divx, int divy);
 	    public native void setRotate(@Cast("SkScalar") float degrees, @Cast("SkScalar") float px, @Cast("SkScalar") float py);
 	    public native void setRotate(@Cast("SkScalar") float degrees);
 	    public native void setSinCos(@Cast("SkScalar") float sinValue, @Cast("SkScalar") float cosValue, @Cast("SkScalar") float px, @Cast("SkScalar") float py);
 	    public native void setSinCos(@Cast("SkScalar") float sinValue, @Cast("SkScalar") float cosValue);
 	    public native void setSkew(@Cast("SkScalar") float kx, @Cast("SkScalar") float ky, @Cast("SkScalar") float px, @Cast("SkScalar") float py);
 	    public native void setSkew(@Cast("SkScalar") float kx, @Cast("SkScalar") float ky);
 	    public native boolean setConcat(@Const @ByRef SkMatrix a, @Const @ByRef SkMatrix b);
 	    
 	    public native boolean preTranslate(@Cast("SkScalar") float dx, @Cast("SkScalar") float dy);
 	    public native boolean preScale(@Cast("SkScalar") float sx, @Cast("SkScalar") float sy, @Cast("SkScalar") float px, @Cast("SkScalar") float py);
 	    public native boolean preScale(@Cast("SkScalar") float sx, @Cast("SkScalar") float sy);
 	    public native boolean preRotate(@Cast("SkScalar") float degrees, @Cast("SkScalar") float px, @Cast("SkScalar") float py);
 	    public native boolean preRotate(@Cast("SkScalar") float degrees);
 	    public native boolean preSkew(@Cast("SkScalar") float kx, @Cast("SkScalar") float ky, @Cast("SkScalar") float px, @Cast("SkScalar") float py);
 	    public native boolean preSkew(@Cast("SkScalar") float kx, @Cast("SkScalar") float ky);
 	    public native boolean preConcat(@Const @ByRef SkMatrix other);
 	
 	    public native boolean postTranslate(@Cast("SkScalar") float dx, @Cast("SkScalar") float dy);
 	    public native boolean postScale(@Cast("SkScalar") float sx, @Cast("SkScalar") float sy, @Cast("SkScalar") float px, @Cast("SkScalar") float py);
 	    public native boolean postScale(@Cast("SkScalar") float sx, @Cast("SkScalar") float sy);
 	    public native boolean postIDiv(int divx, int divy);
 	    public native boolean postRotate(@Cast("SkScalar") float degrees, @Cast("SkScalar") float px, @Cast("SkScalar") float py);
 	    public native boolean postRotate(@Cast("SkScalar") float degrees);
 	    public native boolean postSkew(@Cast("SkScalar") float kx, @Cast("SkScalar") float ky, @Cast("SkScalar") float px, @Cast("SkScalar") float py);
 	    public native boolean postSkew(@Cast("SkScalar") float kx, @Cast("SkScalar") float ky);
 	    public native boolean postConcat(@Const @ByRef SkMatrix other);
 	    
 	    //enum ScaleToFit
 	    public static final int kFill_ScaleToFit = 0,
 	        kStart_ScaleToFit = 1,
 	        kCenter_ScaleToFit = 2;
 
 	    public native boolean setRectToRect(@Const @ByRef SkRect src, @Const @ByRef SkRect dst, @Cast("SkMatrix::ScaleToFit") int stf);
 	    public boolean setPolyToPoly(SkPoint[] src, SkPoint[] dst) {
 			return setPolyToPoly(SkPoint.constArray(src), SkPoint.constArray(dst), src.length);
 	    }
 	    public native boolean setPolyToPoly(@Const SkPoint src, @Const SkPoint dst, int count);
 	    public native boolean invert(SkMatrix inverse);
 
         public native static void SetAffineIdentity(@Cast("SkScalar*") float[] affine);
 
         public native boolean asAffine(@Cast("SkScalar*") float[] affine);
 
         public void mapPoints(SkPoint[] dst, SkPoint[] src) {
             mapPoints(SkPoint.array(dst), SkPoint.constArray(src), src.length);
         }
         public native void mapPoints(SkPoint dst, @Const SkPoint src, int count);
 
         public void mapPoints(SkPoint[] pts) {
             mapPoints(SkPoint.array(pts), pts.length);
         }
         public native void mapPoints(SkPoint pts, int count);
 
         public native static @Const @ByRef SkMatrix I();
 
         public native static @Const @ByRef SkMatrix InvalidMatrix();
 
         public native void dirtyMatrixTypeCache();
 	}
 
 	/*
 	 * SkPaint.h
 	 */
 
 	public static class SkPaint extends Pointer {
 		static { Loader.load(Skia.class); }
 		
 		public SkPaint() { allocate(); }
 		private native void allocate();
 
         public SkPaint(SkPaint paint) { allocate(paint); }
         private native void allocate(@Const @ByRef SkPaint paint);
 
 		@Name("operator=")
         public native @ByRef SkPaint copy(@Const @ByRef SkPaint paint);
 		//TODO: @Name("operator==")
 		//TODO: public native static boolean equal(@Const @ByRef SkPaint a, @Const @ByRef SkPaint b);
 		//TODO: @Name("operator!=")
 		//TODO: public native static boolean notEqual(@Const @ByRef SkPaint a, @Const @ByRef SkPaint b);
 	    //TODO: void flatten(SkFlattenableWriteBuffer&) const;
 	    //TODO: void unflatten(SkFlattenableReadBuffer&);
 	    public native void reset();
 	    //enum Hinting
 	    public static final int kNo_Hinting = 0,
 	    		kSlight_Hinting = 1,
 	    		kNormal_Hinting = 2,
 	    		kFull_Hinting = 3;
 	    public native @Cast("SkPaint::Hinting") int getHinting();
 	    public native void setHinting(@Cast("SkPaint::Hinting") int hintingLevel);
 	    //enum Flags
 	    public static final int kAntiAlias_Flag = 0x01,
 		        kFilterBitmap_Flag = 0x02,
 		        kDither_Flag = 0x04,
 		        kUnderlineText_Flag = 0x08,
 		        kStrikeThruText_Flag = 0x10,
 		        kFakeBoldText_Flag = 0x20,
 		        kLinearText_Flag = 0x40,
 		        kSubpixelText_Flag = 0x80,
 		        kDevKernText_Flag = 0x100,
 		        kLCDRenderText_Flag = 0x200,
 		        kEmbeddedBitmapText_Flag = 0x400,
 		        kAutoHinting_Flag = 0x800,
 		        kVerticalText_Flag = 0x1000,
 		        kGenA8FromLCD_Flag = 0x2000,
 		        kAllFlags = 0x3FFF;
 	    public native @Cast("uint32_t") int getFlags();
 	    public native void setFlags(@Cast("uint32_t") int flags);
 	    public native boolean isAntiAlias();
 	    public native void setAntiAlias(boolean aa);
 	    public native boolean isDither();
 	    public native void setDither(boolean dither);
 	    public native boolean isLinearText();
 	    public native void setLinearText(boolean linearText);
 	    public native boolean isSubpixelText();
 	    public native void setSubpixelText(boolean subpixelText);
 	    public native boolean isLCDRenderText();
 	    public native void setLCDRenderText(boolean lcdText);
 	    public native boolean isEmbeddedBitmapText();
 	    public native void setEmbeddedBitmapText(boolean useEmbeddedBitmapText);
 	    public native boolean isAutohinted();
 	    public native void setAutohinted(boolean useAutohinter);
 	    public native boolean isVerticalText();
 	    public native void setVerticalText(boolean verticalText);
 	    public native boolean isUnderlineText();
 	    public native void setUnderlineText(boolean underlineText);
 	    public native boolean isStrikeThruText();
 	    public native void setStrikeThruText(boolean strikeThruText);
 	    public native boolean isFakeBoldText();
 	    public native void setFakeBoldText(boolean fakeBoldText);
 	    public native boolean isDevKernText();
 	    public native void setDevKernText(boolean devKernText);
 	    public native boolean isFilterBitmap();
 	    public native void setFilterBitmap(boolean filterBitmap);
 	    //enum Style
 	    public static final int kFill_Style = 0,
 	    		kStroke_Style = 1,
 	    		kStrokeAndFill_Style = 2;
 	    public native @Cast("SkPaint::Style") int getStyle();
 	    public native void setStyle(@Cast("SkPaint::Style") int style);
 	    public native @Cast("SkColor") int getColor();
 	    public native void setColor(@Cast("SkColor") int color);
 	    public native @Cast("uint8_t") int getAlpha();
 	    public native void setAlpha(@Cast("U8CPU") int a);
 		public native void setARGB(@Cast("U8CPU") int a, @Cast("U8CPU") int r, @Cast("U8CPU") int g, @Cast("U8CPU") int b);
 		public native @Cast("SkScalar") float getStrokeWidth();
 	    public native void setStrokeWidth(@Cast("SkScalar") float width);
 	    public native @Cast("SkScalar") float getStrokeMiter();
 	    public native void setStrokeMiter(@Cast("SkScalar") float miter);
 	    //enum Cap
 	    public static final int kButt_Cap = 0,
 	    		kRound_Cap = 1,
 	    		kSquare_Cap = 2;
 	    public static final int kDefault_Cap = kButt_Cap;
 	    //enum Join
 	    public static final int kMiter_Join = 0,
 	    		kRound_Join = 1,
 	    		kBevel_Join =2;
 	    public static final int kDefault_Join = kMiter_Join;
 	    public native @Cast("SkPaint::Cap") int getStrokeCap();
 	    public native void setStrokeCap(@Cast("SkPaint::Cap") int cap);
 	    public native @Cast("SkPaint::Join") int getStrokeJoin();
 	    public native void setStrokeJoin(@Cast("SkPaint::Join") int join);
 	    public native boolean getFillPath(@Const @ByRef SkPath src, SkPath dst);
 	    public native boolean canComputeFastBounds();
 	    public native @Const @ByRef SkRect computeFastBounds(@Const @ByRef SkRect orig, SkRect storage);
 
 	    public native SkShader getShader();
 	    public native SkShader setShader(SkShader shader);
 
 	    public native SkColorFilter getColorFilter();
 	    public native SkColorFilter setColorFilter(SkColorFilter filter);
 
 	    public native SkXfermode getXfermode();
 	    public native SkXfermode setXfermode(SkXfermode xfermode);
 	    public native SkXfermode setXfermodeMode(@Cast("SkXfermode::Mode") int mode);
 
 	    public native SkPathEffect getPathEffect();
 	    public native SkPathEffect setPathEffect(SkPathEffect effect);
 
         public native SkMaskFilter getMaskFilter();
         public native SkMaskFilter setMaskFilter(SkMaskFilter maskfilter);
 
         public native SkTypeface getTypeface();
 	    public native SkTypeface setTypeface(SkTypeface typeface);
 
 	    public native SkImageFilter getImageFilter();
 	    public native SkImageFilter setImageFilter(SkImageFilter filter);
 
 	    public native SkDrawLooper getLooper();
 	    public native SkDrawLooper setLooper(SkDrawLooper looper);
 	    
 	    //enum Align
 	    public static final int kLeft_Align = 0,
 	    		kCenter_Align = 1,
 	    		kRight_Align =2;
 	    public native @Cast("SkPaint::Align") int getTextAlign();
 	    public native void setTextAlign(@Cast("SkPaint::Align") int align);
 	    public native @Cast("SkScalar") float getTextSize();
 	    public native void setTextSize(@Cast("SkScalar") float textSize);
 	    public native @Cast("SkScalar") float getTextScaleX();
 	    public native void setTextScaleX(@Cast("SkScalar") float scaleX);
 	    public native @Cast("SkScalar") float getTextSkewX();
 	    public native void setTextSkewX(@Cast("SkScalar") float skewX);
 	    //enum TextEncoding
 	    public static final int kUTF8_TextEncoding = 0,
 	    		kUTF16_TextEncoding = 1,
                 kUTF32_TextEncoding = 2,
 	    		kGlyphID_TextEncoding = 3;
 	    public native @Cast("SkPaint::TextEncoding") int getTextEncoding();
 	    public native void setTextEncoding(@Cast("SkPaint::TextEncoding") int encoding);
 	    
 	    public static class FontMetrics extends Pointer {
 			static { Loader.load(Skia.class); }
 			
 			public FontMetrics() { allocate(); }
 			private native void allocate();
 
 	    	@MemberGetter public native @Cast("SkScalar") float fTop();
 	    	@MemberGetter public native @Cast("SkScalar") float fAscent();
 	    	@MemberGetter public native @Cast("SkScalar") float fDescent();
 	    	@MemberGetter public native @Cast("SkScalar") float fBottom();
 	    	@MemberGetter public native @Cast("SkScalar") float fLeading();
 	    	@MemberGetter public native @Cast("SkScalar") float fAvgCharWidth();
 	    	@MemberGetter public native @Cast("SkScalar") float fXMin();
 	    	@MemberGetter public native @Cast("SkScalar") float fXMax();
 	    	@MemberGetter public native @Cast("SkScalar") float fXHeight();
 	    };
 
         public native @Cast("SkScalar") float getFontMetrics(FontMetrics metrics);
         public native @Cast("SkScalar") float getFontMetrics(FontMetrics metrics, @Cast("SkScalar") float scale/* = 0*/);
 	    public native @Cast("SkScalar") float getFontSpacing();
 	    static Charset toCharset(int encoding) {
 	    	switch (encoding) {
 	    	case SkPaint.kUTF8_TextEncoding:
 	    		return Skia.UTF_8;
 	    	case SkPaint.kUTF16_TextEncoding:
 	    		return Skia.UTF_16;
             case SkPaint.kUTF32_TextEncoding:
                 return Skia.UTF_32;
 	    	default:
 	    		throw new RuntimeException("Unknown text encoding " + encoding);
 	    	}
 	    }
 	    static Pointer encodeText(String text, int encoding) {
 	    	return Skia.toPointer(text, toCharset(encoding));
 	    }
 	    public int textToGlyphs(String text, short[] glyphs) {
 	    	Pointer ptr = encodeText(text, getTextEncoding());
 	    	return textToGlyphs(ptr, ptr.capacity(), glyphs);
 	    }
 	    public native int textToGlyphs(@Const Pointer text, @Cast("size_t") int byteLength, @Cast("uint16_t*") short[] glyphs);
 	    public native boolean containsText(@Const Pointer text, @Cast("size_t") int byteLength);
 	    public native void glyphsToUnichars(@Cast("const uint16_t*") short[] glyphs, int count, @Cast("SkUnichar*") int[] text);
 	    public native int countText(@Const Pointer text, @Cast("size_t") int byteLength);
 	    public native @Cast("SkScalar") float measureText(@Const Pointer text, @Cast("size_t") int length, SkRect bounds, @Cast("SkScalar") float scale/* = 0*/);
         public float measureText(String text) {
             Pointer ptr = encodeText(text, getTextEncoding());
             return measureText(ptr, ptr.capacity());
         }
 	    public native @Cast("SkScalar") float measureText(@Const Pointer text, @Cast("size_t") int length);
 	    
 	    //enum TextBufferDirection
 	    public static final int kForward_TextBufferDirection = 0,
 	        kBackward_TextBufferDirection = 1;
 	    public native @Cast("size_t") int breakText(@Const Pointer text, @Cast("size_t") int length, @Cast("SkScalar") float maxWidth, @Cast("SkScalar*") float[] measuredWidth/* = NULL*/, @Cast("SkPaint::TextBufferDirection") int tbd/* = kForward_TextBufferDirection*/);
         public int getTextWidths(String text, float[] widths, SkRect bounds/* = NULL*/) {
             Pointer ptr = encodeText(text, getTextEncoding());
             return getTextWidths(ptr, ptr.capacity(), widths, bounds);
         }
 	    public native int getTextWidths(@Const Pointer text, @Cast("size_t") int byteLength, @Cast("SkScalar*") float[] widths, SkRect bounds/* = NULL*/);
         
         public void getTextPath(String text, float x, float y, SkPath path) {
             Pointer ptr = encodeText(text, getTextEncoding());
             getTextPath(ptr, ptr.capacity(), x, y, path);
         }
 	    public native void getTextPath(@Const Pointer text, @Cast("size_t") int length, @Cast("SkScalar") float x, @Cast("SkScalar") float y, SkPath path);
 	    
 	    public native boolean nothingToDraw();
 	}
 
 	public static class SkStrokePathEffect extends SkPathEffect {
 		static { Loader.load(Skia.class); }
 
 		public SkStrokePathEffect(SkPaint paint) {
             allocate(paint);
             deallocator(new UnrefDeallocator(this));
         }
 		@NoDeallocator
 	    private native void allocate(@Const @ByRef SkPaint paint);
 
         public SkStrokePathEffect(float width, int style, int join, int cap) {
             allocate(width, style, join, cap);
             deallocator(new UnrefDeallocator(this));
         }
         @NoDeallocator
         private native void allocate(@Cast("SkScalar") float width, @Cast("SkPaint::Style") int style, @Cast("SkPaint::Join") int join, @Cast("SkPaint::Cap") int cap);
         public SkStrokePathEffect(float width, int style, int join, int cap, float miterLimit) {
             allocate(width, style, join, cap, miterLimit);
             deallocator(new UnrefDeallocator(this));
         }
         @NoDeallocator
 		private native void allocate(@Cast("SkScalar") float width, @Cast("SkPaint::Style") int style, @Cast("SkPaint::Join") int join, @Cast("SkPaint::Cap") int cap, @Cast("SkScalar") float miterLimit/* = -1*/);
 	}
 	
 	/*
 	 * SkPath.h
 	 */
 	
 	public static class SkPath extends Pointer {
 		static { Loader.load(Skia.class); }
 		
 		public SkPath() { allocate(); }
 		private native void allocate();
 
         public SkPath(SkPath src) { allocate(src); }
         private native void allocate(@Const @ByRef SkPath src);
 
         @Name("operator=")
         public native @ByRef SkPath copy(@Const @ByRef SkPath src);
 
         //enum FillType
 	    public static final int kWinding_FillType = 0,
 	        kEvenOdd_FillType = 1,
 	        kInverseWinding_FillType = 2,
 	        kInverseEvenOdd_FillType = 3;
 
 	    public native @Cast("SkPath::FillType") int getFillType();
 	    public native void setFillType(@Cast("SkPath::FillType") int ft);
 	    public native boolean isInverseFillType();
 	    public native void toggleInverseFillType();
 	    //enum Convexity
 	    public static final int kUnknown_Convexity = 0,
 	    		kConvex_Convexity = 1,
 	    		kConcave_Convexity = 2;
 
 	    public native @Cast("SkPath::Convexity") int getConvexity();
 	    public native @Cast("SkPath::Convexity") int getConvexityOrUnknown();
 	    public native void setConvexity(@Cast("SkPath::Convexity") int convexity);
 	    public native static @Cast("SkPath::Convexity") int ComputeConvexity(@Const @ByRef SkPath path);
 
         public native boolean isConvex();
 
         public native void setIsConvex(boolean isConvex);
 
         public native void reset();
 	    public native void rewind();
 	    public native boolean isEmpty();
 	    
 	    public native static boolean IsLineDegenerate(@Const @ByRef SkPoint p1, @Const @ByRef SkPoint p2);
 	    public native static boolean IsQuadDegenerate(@Const @ByRef SkPoint p1, @Const @ByRef SkPoint p2, @Const @ByRef SkPoint p3);
 	    public native static boolean IsCubicDegenerate(@Const @ByRef SkPoint p1, @Const @ByRef SkPoint p2, @Const @ByRef SkPoint p3, @Const @ByRef SkPoint p4);
 
 	    public native boolean isRect(SkRect rect);
 	    public native int countPoints();
 	    public native @ByVal SkPoint getPoint(int index);
 
 	    public native void moveTo(@Cast("SkScalar") float x, @Cast("SkScalar") float y);
 	    public native void moveTo(@Const @ByRef SkPoint p);
 	    public native void rMoveTo(@Cast("SkScalar") float dx, @Cast("SkScalar") float dy);
 	    public native void lineTo(@Cast("SkScalar") float x, @Cast("SkScalar") float y);
 	    public native void lineTo(@Const @ByRef SkPoint p);
 	    public native void rLineTo(@Cast("SkScalar") float dx, @Cast("SkScalar") float dy);
 	    public native void quadTo(@Cast("SkScalar") float x1, @Cast("SkScalar") float y1, @Cast("SkScalar") float x2, @Cast("SkScalar") float y2);
 	    public native void quadTo(@Const @ByRef SkPoint p1, @Const @ByRef SkPoint p2);
 	    public native void rQuadTo(@Cast("SkScalar") float dx1, @Cast("SkScalar") float dy1, @Cast("SkScalar") float dx2, @Cast("SkScalar") float dy2);
 	    public native void cubicTo(@Cast("SkScalar") float x1, @Cast("SkScalar") float y1, @Cast("SkScalar") float x2, @Cast("SkScalar") float y2, @Cast("SkScalar") float x3, @Cast("SkScalar") float y3);
 	    public native void cubicTo(@Const @ByRef SkPoint p1, @Const @ByRef SkPoint p2, @Const @ByRef SkPoint p3);
 	    public native void rCubicTo(@Cast("SkScalar") float x1, @Cast("SkScalar") float y1, @Cast("SkScalar") float x2, @Cast("SkScalar") float y2, @Cast("SkScalar") float x3, @Cast("SkScalar") float y3);
 	    public native void arcTo(@Const @ByRef SkRect oval, @Cast("SkScalar") float startAngle, @Cast("SkScalar") float sweepAngle, boolean forceMoveTo);
 	    public native void arcTo(@Cast("SkScalar") float x1, @Cast("SkScalar") float y1, @Cast("SkScalar") float x2, @Cast("SkScalar") float y2, @Cast("SkScalar") float radius);
 	    public native void arcTo(@Const @ByVal SkPoint p1, @Const @ByVal SkPoint p2, @Cast("SkScalar") float radius);
 	    public native void close();
 	    //enum Direction
 	    public static final int kCW_Direction = 0,
 	    		kCCW_Direction = 1;
 	    public native boolean cheapComputeDirection(@Cast("SkPath::Direction*") IntPointer dir);
 	    public native boolean cheapIsDirection(@Cast("SkPath::Direction") int dir);
 
         public native void addRect(@Const @ByRef SkRect rect);
 	    public native void addRect(@Const @ByRef SkRect rect, @Cast("SkPath::Direction") int dir/* = kCW_Direction*/);
 
 	    public native void addRect(@Cast("SkScalar") float left, @Cast("SkScalar") float top, @Cast("SkScalar") float right, @Cast("SkScalar") float bottom, @Cast("SkPath::Direction") int dir/* = kCW_Direction*/);
 
         public native void addOval(@Const @ByRef SkRect oval);
 	    public native void addOval(@Const @ByRef SkRect oval, @Cast("SkPath::Direction") int dir/* = kCW_Direction*/);
 
         public void addCircle(float x, float y, float radius) { addCircle(x, y, radius, kCW_Direction); }
 	    public native void addCircle(@Cast("SkScalar") float x, @Cast("SkScalar") float y, @Cast("SkScalar") float radius, @Cast("SkPath::Direction") int dir/* = kCW_Direction*/);
 
 	    public native void addArc(@Const @ByRef SkRect oval, @Cast("SkScalar") float startAngle, @Cast("SkScalar") float sweepAngle);
 
         public native void addRoundRect(@Const @ByRef SkRect rect, @Cast("SkScalar") float rx, @Cast("SkScalar") float ry);
 	    public native void addRoundRect(@Const @ByRef SkRect rect, @Cast("SkScalar") float rx, @Cast("SkScalar") float ry, @Cast("SkPath::Direction") int dir/* = kCW_Direction*/);
 
         public native void addRoundRect(@Const @ByRef SkRect rect, @Cast("const SkScalar*") float[] radii);
 	    public native void addRoundRect(@Const @ByRef SkRect rect, @Cast("const SkScalar*") float[] radii, @Cast("SkPath::Direction") int dir/* = kCW_Direction*/);
 
 	    public native void addPath(@Const @ByRef SkPath src, @Cast("SkScalar") float dx, @Cast("SkScalar") float dy);
 	    public native void addPath(@Const @ByRef SkPath src);
 	    public native void addPath(@Const @ByRef SkPath src, @Const @ByRef SkMatrix matrix);
 	    public native void reverseAddPath(@Const @ByRef SkPath src);
 	    public native void offset(@Cast("SkScalar") float dx, @Cast("SkScalar") float dy, SkPath dst);
 	    public native void offset(@Cast("SkScalar") float dx, @Cast("SkScalar") float dy);
 	    public native void transform(@Const @ByRef SkMatrix matrix, SkPath dst);
 	    public native void transform(@Const @ByRef SkMatrix matrix);
 	    public native boolean getLastPt(SkPoint lastPt);
 	    public native void setLastPt(@Cast("SkScalar") float x, @Cast("SkScalar") float y);
 	    public native void setLastPt(@Const @ByRef SkPoint p);
 
         //enum SegmentMask
         public static final int kLine_SegmentMask = 1 << 0,
             kQuad_SegmentMask   = 1 << 1,
             kCubic_SegmentMask  = 1 << 2;
 
         public native @Cast("uint32_t") int getSegmentMasks();
 
         //enum Verb
         public static final int kMove_Verb = 0,
             kLine_Verb = 1,
             kQuad_Verb = 2,
             kCubic_Verb = 3,
             kClose_Verb = 4,
             kDone_Verb = 5;
     }
 
     @Name("operator==")
     public native static boolean equal(@Const @ByRef SkPath a, @Const @ByRef SkPath b);
     @Name("operator!=")
     public native static boolean notEqual(@Const @ByRef SkPath a, @Const @ByRef SkPath b);
 
     /*
       * SkPathEffect.h
       */
 	
 	public static class SkPathEffect extends SkFlattenable {
 		static { Loader.load(Skia.class); }
 		
 		protected SkPathEffect() {}
 	}
 
 	public static class SkPairPathEffect extends SkPathEffect {
 		static { Loader.load(Skia.class); }
 		
 		protected SkPairPathEffect() {}
 	}
 
 	public static class SkComposePathEffect extends SkPairPathEffect {
 		static { Loader.load(Skia.class); }
 		
 		public SkComposePathEffect(SkPathEffect outer, SkPathEffect inner) {
             allocate(outer, inner);
             deallocator(new UnrefDeallocator(this));
         }
 		@NoDeallocator
 		private native void allocate(SkPathEffect outer, SkPathEffect inner);
 	}
 
 	public static class SkSumPathEffect extends SkPairPathEffect {
 		static { Loader.load(Skia.class); }
 		
 		public SkSumPathEffect(SkPathEffect first, SkPathEffect second) {
             allocate(first, second);
             deallocator(new UnrefDeallocator(this));
         }
 		@NoDeallocator
 		private native void allocate(SkPathEffect first, SkPathEffect second);
 	}
 
     /*
      * SkPicture.h
      */
 
     public static class SkPicture extends SkRefCnt {
         static { Loader.load(Skia.class); }
 
         public SkPicture() {
             allocate();
             deallocator(new UnrefDeallocator(this));
         }
         @NoDeallocator
         private native void allocate();
 
         public SkPicture(SkPicture src) {
             allocate(src);
             deallocator(new UnrefDeallocator(this));
         }
         @NoDeallocator
         private native void allocate(@Const @ByRef SkPicture src);
 
         public SkPicture(SkStream stream) {
             allocate(stream);
             deallocator(new UnrefDeallocator(this));
         }
         @NoDeallocator
         private native void allocate(SkStream stream);
 
         public native void swap(@ByRef SkPicture other);
 
         //enum RecordingFlags
         public static final int kUsePathBoundsForClip_RecordingFlag = 0x01;
 
         public native SkCanvas beginRecording(int width, int height);
         public native SkCanvas beginRecording(int width, int height, @Cast("uint32_t") int recordFlags/* = 0*/);
 
         public native SkCanvas getRecordingCanvas();
         public native void endRecording();
 
         public native void draw(SkCanvas surface);
 
         public native int width();
 
         public native int height();
 
         //public native void serialize(SkWStream stream);
 
         public native void abortPlayback();
     }
 
     /*
      * SkPixelRef.h
      */
 
     public static class SkPixelRef extends SkFlattenable {
         static { Loader.load(Skia.class); }
 
         protected SkPixelRef() {}
     }
 
 	/*
 	 * SkPoint.h
 	 */
 
     public static class SkIPoint extends Pointer {
         static { Loader.load(Skia.class); }
 
         @MemberGetter
         public native @Cast("int32_t") int fX();
         @MemberSetter
         public native void fX(@Cast("int32_t") int x);
         @MemberGetter
         public native @Cast("int32_t") int fY();
         @MemberSetter
         public native void fY(@Cast("int32_t") int y);
 
         public native static @ByVal SkIPoint Make(@Cast("int32_t") int x, @Cast("int32_t") int y);
 
         public native @Cast("int32_t") int x();
         public native @Cast("int32_t") int y();
         public native void setX(@Cast("int32_t") int x);
         public native void setY(@Cast("int32_t") int y);
 
         public native boolean isZero();
 
         public native void setZero();
 
         public native void set(@Cast("int32_t") int x, @Cast("int32_t") int y);
 
         public native void rotateCW(SkIPoint dst);
 
         public native void rotateCW();
 
         public native void rotateCCW(SkIPoint dst);
 
         public native void rotateCCW();
 
         public native void negate();
 
         @Name("operator-")
         public native @ByVal SkIPoint minus();
 
         @Name("operator+=")
         public native void plusAssign(@Const @ByRef SkIPoint v);
 
         @Name("operator-=")
         public native void minusAssign(@Const @ByRef SkIPoint v);
 
         public native boolean equals(@Cast("int32_t") int x, @Cast("int32_t") int y);
 
         public native static @Cast("int32_t") int DotProduct(@Const @ByRef SkIPoint a, @Const @ByRef SkIPoint b);
 
         public native static @Cast("int32_t") int CrossProduct(@Const @ByRef SkIPoint a, @Const @ByRef SkIPoint b);
     };
 
     @Name("operator==")
     public native static boolean equal(@Const @ByRef SkIPoint a, @Const @ByRef SkIPoint b);
 
     @Name("operator!=")
     public native static boolean notEqual(@Const @ByRef SkIPoint a, @Const @ByRef SkIPoint b);
 
     @Name("operator-")
     public native static @ByVal SkIPoint minus(@Const @ByRef SkIPoint a, @Const @ByRef SkIPoint b);
 
     @Name("operator+")
     public native static @ByVal SkIPoint plus(@Const @ByRef SkIPoint a, @Const @ByRef SkIPoint b);
 
     public static class SkPoint extends Pointer {
 		static { Loader.load(Skia.class); }
 
         public SkPoint(Pointer pointer) { super(pointer); }
 
 		public SkPoint() { allocate(); }
         private native void allocate();
 
         @Name("operator=")
         public native @ByRef SkPoint copy(@Const @ByRef SkPoint src);
 
         private SkPoint(int size) { allocateArray(size); }
         private native void allocateArray(int size);
         @Override public SkPoint position(int position) {
             return (SkPoint)super.position(position);
         }
         public static SkPoint constArray(SkPoint[] elements) {
             SkPoint ptr = new SkPoint(elements.length);
             for (int i = 0; i < elements.length; i++) {
                 ptr.position(i).copy(elements[i]);
             }
             return ptr.position(0);
         }
         public static SkPoint array(SkPoint[] elements) {
             SkPoint ptr = new SkPoint(elements.length);
             for (int i = 0; i < elements.length; i++) {
                 ptr.position(i).copy(elements[i]);
                 elements[i] = new SkPoint(ptr);
             }
             return ptr.position(0);
         }
 
         @MemberGetter
         public native @Cast("int32_t") int fX();
         @MemberSetter
         public native void fX(@Cast("int32_t") int x);
         @MemberGetter
         public native @Cast("int32_t") int fY();
         @MemberSetter
         public native void fY(@Cast("int32_t") int y);
 
 	    public native static @ByVal SkPoint Make(@Cast("SkScalar") float x, @Cast("SkScalar") float y);
 
 	    public native @Cast("SkScalar") float x();
 	    public native @Cast("SkScalar") float y();
 
         public native void set(@Cast("SkScalar") float x, @Cast("SkScalar") float y);
 
         public native void iset(@Cast("int32_t") int x, @Cast("int32_t") int y);
 
         public native void iset(@Const @ByRef SkIPoint p);
 
         public native void setAbs(@Const @ByRef SkPoint pt);
 
         public native void setIRectFan(int l, int t, int r, int b);
         public native void setIRectFan(int l, int t, int r, int b, @Cast("size_t") int stride);
 
         public native void setRectFan(@Cast("SkScalar") float l, @Cast("SkScalar") float t, @Cast("SkScalar") float r, @Cast("SkScalar") float b);
         public native void setRectFan(@Cast("SkScalar") float l, @Cast("SkScalar") float t, @Cast("SkScalar") float r, @Cast("SkScalar") float b, @Cast("size_t") int stride);
 
         public static void Offset(SkPoint[] points, SkPoint offset) {
             Offset(SkPoint.array(points), points.length, offset);
         }
         public native static void Offset(SkPoint points, int count, @Const @ByRef SkPoint offset);
 
         public static void Offset(SkPoint[] points, @Cast("SkScalar") float dx, @Cast("SkScalar") float dy) {
             Offset(SkPoint.array(points), points.length, dx, dy);
         }
         public native static void Offset(SkPoint points, int count, @Cast("SkScalar") float dx, @Cast("SkScalar") float dy);
 
         public native void offset(@Cast("SkScalar") float dx, @Cast("SkScalar") float dy);
 
         public native @Cast("SkScalar") float length();
         public native @Cast("SkScalar") float distanceToOrigin();
 
         public native static boolean CanNormalize(@Cast("SkScalar") float dx, @Cast("SkScalar") float dy);
 
         public native boolean canNormalize();
 
         public native boolean normalize();
 
         public native boolean setNormalize(@Cast("SkScalar") float x, @Cast("SkScalar") float y);
 
         public native boolean setLength(@Cast("SkScalar") float length);
 
         public native boolean setLength(@Cast("SkScalar") float x, @Cast("SkScalar") float y, @Cast("SkScalar") float length);
 
         public native void scale(@Cast("SkScalar") float scale, SkPoint dst);
 
         public native void scale(@Cast("SkScalar") float value);
 
         public native void rotateCW(SkPoint dst);
 
         public native void rotateCW();
 
         public native void rotateCCW(SkPoint dst);
 
         public native void rotateCCW();
 
         public native void negate();
 
         @Name("operator-")
         public native @ByVal SkPoint minus();
 
         @Name("operator+=")
         public native void plusAssign(@Const @ByRef SkPoint v);
 
         @Name("operator-=")
         public native void minusAssign(@Const @ByRef SkPoint v);
 
         public native boolean equals(@Cast("SkScalar") float x, @Cast("SkScalar") float y);
 
         public native boolean equalsWithinTolerance(@Const @ByRef SkPoint v, @Cast("SkScalar") float tol);
 
         public native static @Cast("SkScalar") float Length(@Cast("SkScalar") float x, @Cast("SkScalar") float y);
 
         public native static @Cast("SkScalar") float Normalize(SkPoint pt);
 
         public native static @Cast("SkScalar") float Distance(@Const @ByRef SkPoint a, @Const @ByRef SkPoint b);
 
         public native static @Cast("SkScalar") float DotProduct(@Const @ByRef SkPoint a, @Const @ByRef SkPoint b);
 
         public native static @Cast("SkScalar") float CrossProduct(@Const @ByRef SkPoint a, @Const @ByRef SkPoint b);
 
         public native @Cast("SkScalar") float cross(@Const @ByRef SkPoint vec);
 
         public native @Cast("SkScalar") float dot(@Const @ByRef SkPoint vec);
 
         public native @Cast("SkScalar") float lengthSqd();
 
         public native @Cast("SkScalar") float distanceToSqd(@Const @ByRef SkPoint pt);
 
         //enum Side
         public static final int kLeft_Side = -1,
             kOn_Side = 0,
             kRight_Side = 1;
 
         public native @Cast("SkScalar") float distanceToLineBetweenSqd(@Const @ByRef SkPoint a,
                                           @Const @ByRef SkPoint b,
                                           @Cast("SkPoint::Side*") IntPointer side/* = NULL*/);
 
         public native @Cast("SkScalar") float distanceToLineBetween(@Const @ByRef SkPoint a,
                                        @Const @ByRef SkPoint b,
                                        @Cast("SkPoint::Side*") IntPointer side/* = NULL*/);
 
         public native @Cast("SkScalar") float distanceToLineSegmentBetweenSqd(@Const @ByRef SkPoint a,
                                                  @Const @ByRef SkPoint b);
 
         public native @Cast("SkScalar") float distanceToLineSegmentBetween(@Const @ByRef SkPoint a,
                                               @Const @ByRef SkPoint b);
 
         public native void setOrthog(@Const @ByRef SkPoint vec, @Cast("SkPoint::Side") int side/* = kLeft_Side*/);
     }
 
     @Name("operator==")
     public native static boolean equal(@Const @ByRef SkPoint a, @Const @ByRef SkPoint b);
 
     @Name("operator!=")
     public native static boolean notEqual(@Const @ByRef SkPoint a, @Const @ByRef SkPoint b);
 
     @Name("operator-")
     public native static @ByVal SkPoint minus(@Const @ByRef SkPoint a, @Const @ByRef SkPoint b);
 
     @Name("operator+")
     public native static @ByVal SkPoint plus(@Const @ByRef SkPoint a, @Const @ByRef SkPoint b);
 
     /*
       * SkPostConfig.h
       */
 	
     public static final int SK_A32_SHIFT = 24,
     		SK_R32_SHIFT = 16,
     		SK_G32_SHIFT = 8,
     		SK_B32_SHIFT = 0;
 	
 	/*
 	 * SkRect.h
 	 */
 	
 	public static class SkIRect extends Pointer {
 		static { Loader.load(Skia.class); }
 		
 		public SkIRect() { allocate(); }
 		private native void allocate();
 		
 	    public native static @ByVal SkIRect MakeEmpty();
 	    public native static @ByVal SkIRect MakeWH(@Cast("int32_t") int w, @Cast("int32_t") int h);
 	    public native static @ByVal SkIRect MakeSize(@Const @ByRef SkISize size);
 	    public native static @ByVal SkIRect MakeLTRB(@Cast("int32_t") int l, @Cast("int32_t") int t, @Cast("int32_t") int r, @Cast("int32_t") int b);
 	    public native static @ByVal SkIRect MakeXYWH(@Cast("int32_t") int x, @Cast("int32_t") int y, @Cast("int32_t") int w, @Cast("int32_t") int h);
 
         public native void setXYWH(@Cast("int32_t") int x, @Cast("int32_t") int y, @Cast("int32_t") int width, @Cast("int32_t") int height);
 	}
 	
 	public static class SkRect extends Pointer {
 		static { Loader.load(Skia.class); }
 		
 		public SkRect() { allocate(); }
 		private native void allocate();
 
         @MemberGetter
         public native @Cast("SkScalar") float fLeft();
         @MemberGetter
         public native @Cast("SkScalar") float fTop();
         @MemberGetter
         public native @Cast("SkScalar") float fRight();
         @MemberGetter
         public native @Cast("SkScalar") float fBottom();
 
 	    public native static @ByVal SkRect MakeEmpty();
 	    public native static @ByVal SkRect MakeWH(@Cast("SkScalar") float w, @Cast("SkScalar") float h);
 	    public native static @ByVal SkRect MakeSize(@Const @ByRef SkSize size);
 	    public native static @ByVal SkRect MakeLTRB(@Cast("SkScalar") float l, @Cast("SkScalar") float t, @Cast("SkScalar") float r, @Cast("SkScalar") float b);
 	    public native static @ByVal SkRect MakeXYWH(@Cast("SkScalar") float x, @Cast("SkScalar") float y, @Cast("SkScalar") float w, @Cast("SkScalar") float h);
 
         public native @Cast("SkScalar") float left();
         public native @Cast("SkScalar") float top();
         public native @Cast("SkScalar") float right();
         public native @Cast("SkScalar") float bottom();
         public native @Cast("SkScalar") float width();
         public native @Cast("SkScalar") float height();
         public native @Cast("SkScalar") float centerX();
         public native @Cast("SkScalar") float centerY();
 
         public native void set(@Cast("SkScalar") float left, @Cast("SkScalar") float top, @Cast("SkScalar") float right, @Cast("SkScalar") float bottom);
         public native void setLTRB(@Cast("SkScalar") float left, @Cast("SkScalar") float top, @Cast("SkScalar") float right, @Cast("SkScalar") float bottom);
         public native void setXYWH(@Cast("SkScalar") float x, @Cast("SkScalar") float y, @Cast("SkScalar") float width, @Cast("SkScalar") float height);
 
         public native void offset(@Cast("SkScalar") float dx, @Cast("SkScalar") float dy);
         public native void inset(@Cast("SkScalar") float dx, @Cast("SkScalar") float dy);
 
 
 	}
 
     /*
      * SkRandom.h
      */
 
     public static class SkRandom extends Pointer {
         static { Loader.load(Skia.class); }
 
         public SkRandom() { allocate(); }
         private native void allocate();
         public SkRandom(int seed) { allocate(seed); }
         private native void allocate(@Cast("uint32_t") int seed);
 
         public native @Cast("uint32_t") int nextU();
 
         public native @Cast("int32_t") int nextS();
 
         public native @Cast("U16CPU") int nextU16();
 
         public native @Cast("S16CPU") int nextS16();
 
         public native @Cast("uint32_t") int nextBits(@Cast("unsigned") int bitCount);
 
         public native @Cast("uint32_t") int nextRangeU(@Cast("uint32_t") int min, @Cast("uint32_t") int max);
 
         public native @Cast("SkFixed") int nextUFixed1();
 
         public native @Cast("SkFixed") int nextSFixed1();
 
         public native @Cast("SkScalar") float nextUScalar1();
 
         public native @Cast("SkScalar") float nextSScalar1();
 
         public native void next64(Sk64 a);
 
         public native void setSeed(@Cast("uint32_t") int seed);
     }
 
 	/*
 	 * SkRefCnt.h
 	 */
 	
 	public static class SkRefCnt extends SkNoncopyable {
 		static { Loader.load(Skia.class); }
 		
 		protected SkRefCnt(Pointer pointer) { super(pointer); }
 
 		protected SkRefCnt() { }
 
 	    protected native @Cast("int32_t") int getRefCnt();
 
         protected native void ref();
 
         protected native void unref();
 
         protected native void validate();
 
         protected static class UnrefDeallocator extends SkRefCnt implements Deallocator {
             public UnrefDeallocator(SkRefCnt p) {
                 super(p);
             }
 
             @Override public void deallocate() {
                 unref();
             }
         }
 	}
 
     /*
      * SkRegion.h
      */
 
     public static class SkRegion extends Pointer {
         static { Loader.load(Skia.class); }
 
         //enum Op
         public static final int kDifference_Op = 0,
             kIntersect_Op = 1,
             kUnion_Op = 2,
             kXOR_Op = 3,
             kReverseDifference_Op = 4,
             kReplace_Op = 5;
     }
 
     /*
      * SkScalar.h
      */
 
 //    typedef float   SkScalar;
 //    extern const uint32_t gIEEENotANumber;
 //    extern const uint32_t gIEEEInfinity;
 
     public static final float SK_Scalar1 = 1.0f;
 
     public static final float SK_ScalarHalf = 0.5f;
 
     public static final float SK_ScalarInfinity = Float.POSITIVE_INFINITY;
 
     public static final float SK_ScalarMax = Float.MAX_VALUE;
 
     public static final float SK_ScalarMin = Float.MIN_VALUE;
 
     public static final float SK_ScalarNaN = Float.NaN;
 
     public native static boolean SkScalarIsNaN(float x);
     public native static boolean SkScalarIsFinite(float x);
 
     public static float SkIntToScalar(int n) { return (float) n; }
 
 //    #define SkFixedToScalar(x)      SkFixedToFloat(x)
 //    #define SkScalarToFixed(x)      SkFloatToFixed(x)
 
     public static float SkScalarToFloat( float n) { return n; };
     public static float SkFloatToScalar(float n) { return n; };
 
     public static double SkScalarToDouble(float n) { return (double)(n); }
     public static double SkDoubleToScalar(double n) { return (float)(n); }
 
     public native static float SkScalarFraction(float x);
 
     public native static float SkScalarFloorToScalar(float x);
     public native static float SkScalarCeilToScalar(float x);
     public native static float SkScalarRoundToScalar(float x);
 
     public native static int SkScalarFloorToInt(float x);
     public native static int SkScalarCeilToInt(float x);
     public native static int SkScalarRoundToInt(float x);
 
     public native static float SkScalarAbs(float x);
     public native static float SkScalarCopySign(float x, float y);
     public native static @Cast("SkScalar") float SkScalarClampMax(@Cast("SkScalar") float x, @Cast("SkScalar") float max);
     public native static @Cast("SkScalar") float SkScalarPin(@Cast("SkScalar") float x, @Cast("SkScalar") float min, @Cast("SkScalar") float max);
     public native static @Cast("SkScalar") float SkScalarSquare(@Cast("SkScalar") float x);
     public native static @Cast("SkScalar") float SkScalarMul(@Cast("SkScalar") float a, @Cast("SkScalar") float b);
     public native static @Cast("SkScalar") float SkScalarMulAdd(@Cast("SkScalar") float a, @Cast("SkScalar") float b, @Cast("SkScalar") float c);
     public native static @Cast("SkScalar") float SkScalarMulRound(@Cast("SkScalar") float a, @Cast("SkScalar") float b);
     public native static @Cast("SkScalar") float SkScalarMulCeil(@Cast("SkScalar") float a, @Cast("SkScalar") float b);
     public native static @Cast("SkScalar") float SkScalarMulFloor(@Cast("SkScalar") float a, @Cast("SkScalar") float b);
     public native static @Cast("SkScalar") float SkScalarDiv(@Cast("SkScalar") float a, @Cast("SkScalar") float b);
     public native static @Cast("SkScalar") float SkScalarMod(@Cast("SkScalar") float x,@Cast("SkScalar") float y);
     public native static @Cast("SkScalar") float SkScalarMulDiv(@Cast("SkScalar") float a, @Cast("SkScalar") float b, @Cast("SkScalar") float c);
     public native static @Cast("SkScalar") float SkScalarInvert(@Cast("SkScalar") float x);
     public native static @Cast("SkScalar") float SkScalarFastInvert(@Cast("SkScalar") float x);
     public native static @Cast("SkScalar") float SkScalarSqrt(@Cast("SkScalar") float x);
     public native static @Cast("SkScalar") float SkScalarAve(@Cast("SkScalar") float a, @Cast("SkScalar") float b);
     public native static @Cast("SkScalar") float SkScalarMean(@Cast("SkScalar") float a, @Cast("SkScalar") float b);
     public native static @Cast("SkScalar") float SkScalarHalf(@Cast("SkScalar") float a);
 
     public static final float SK_ScalarSqrt2 = 1.41421356f;
     public static final float SK_ScalarPI = 3.14159265f;
     public static final float SK_ScalarTanPIOver8 = 0.414213562f;
     public static final float SK_ScalarRoot2Over2 = 0.707106781f;
 
     public native static @Cast("SkScalar") float SkDegreesToRadians(@Cast("SkScalar") float degrees);
 //    float SkScalarSinCos(SkScalar radians, SkScalar* cosValue);
     public native static @Cast("SkScalar") float SkScalarSin(@Cast("SkScalar") float radians);
     public native static @Cast("SkScalar") float SkScalarCos(@Cast("SkScalar") float radians);
     public native static @Cast("SkScalar") float SkScalarTan(@Cast("SkScalar") float radians);
     public native static @Cast("SkScalar") float SkScalarASin(@Cast("SkScalar") float val);
     public native static @Cast("SkScalar") float SkScalarACos(@Cast("SkScalar") float val);
     public native static @Cast("SkScalar") float SkScalarATan2(@Cast("SkScalar") float y, @Cast("SkScalar") float x);
     public native static @Cast("SkScalar") float SkScalarExp(@Cast("SkScalar") float x);
     public native static @Cast("SkScalar") float SkScalarLog(@Cast("SkScalar") float x);
 
     public native static @Cast("SkScalar") float SkMaxScalar(@Cast("SkScalar") float a, @Cast("SkScalar") float b);
     public native static @Cast("SkScalar") float SkMinScalar(@Cast("SkScalar") float a, @Cast("SkScalar") float b);
 
     public native static boolean SkScalarIsInt(@Cast("SkScalar") float x);
 
     public native static @Cast("SkScalar") float SkScalarInterp(@Cast("SkScalar") float A, @Cast("SkScalar") float B, @Cast("SkScalar") float t);
 
     /*
      * SkScalerContext.h
      */
 
     public static class SkGlyph extends Pointer {
         static { Loader.load(Skia.class); }
 
         public SkGlyph() { allocate(); }
         private native void allocate();
     }
 
     /*
     * SkShader.h
     */
 	
 	public static class SkShader extends SkFlattenable {
 		static { Loader.load(Skia.class); }
 		
 		protected SkShader() { }
 
         public native boolean getLocalMatrix(SkMatrix localM);
         public native void setLocalMatrix(@Const @ByRef SkMatrix localM);
         public native void resetLocalMatrix();
 
 	    //enum TileMode
 	    public static final int kClamp_TileMode = 0,
 	        kRepeat_TileMode = 1,
 	        kMirror_TileMode = 2;
 	    
 	    public native static SkShader CreateBitmapShader(@Const @ByRef SkBitmap src, @Cast("SkShader::TileMode") int tmx, @Cast("SkShader::TileMode") int tmy);
 
 	}
 
     /*
      * SkShape.h
      */
 
     public static class SkShape extends SkFlattenable {
         static { Loader.load(Skia.class); }
 
         protected SkShape() {}
         
         public native void draw(SkCanvas canvas);
 
         public native void drawXY(SkCanvas canvas, @Cast("SkScalar") float dx, @Cast("SkScalar") float dy);
 
         public native void drawMatrix(SkCanvas canvas, @Const @ByRef SkMatrix matrix);
     }
 
 	/*
 	* SkSize.h
 	*/
 	
 	public static class SkISize extends Pointer {
 		static { Loader.load(Skia.class); }
 		
 		public SkISize() { allocate(); }
 		private native void allocate();
 		public native static @ByVal SkISize Make(int w, int h);
 		public native void set(int w, int h);
 		public native boolean isZero();
 		public native boolean isEmpty();
         public native void setEmpty();
 		public native int width();
 		public native int height();
 		public native void clampNegToZero();
 		public native boolean equals(int w, int h);
 	}
 	
 	public static class SkSize extends Pointer {
 		static { Loader.load(Skia.class); }
 
         public SkSize() { allocate(); }
         private native void allocate();
 		public native static @ByVal SkSize Make(@Cast("SkScalar") float w, @Cast("SkScalar") float h);
 		@Name("operator=")
 		public native @ByRef SkSize copy(@Const @ByRef SkSize src);
 		public native @ByVal SkISize toRound();
 		public native @ByVal SkISize toCeil();
 		public native @ByVal SkISize toFloor();
 	}
 
     /*
      * SkStream.h
      */
 
     public static class SkStream extends SkRefCnt {
         static { Loader.load(Skia.class); }
 
         protected SkStream() {}
         
         public native boolean rewind();
         public native String getFileName();
         public native @Cast("size_t") int read(Pointer buffer, @Cast("size_t") int size);
         public native @Cast("size_t") int getLength();
         public native @Cast("size_t") int skip(@Cast("size_t") int bytes);
         public native @Const Pointer getMemoryBase();
         public native @Cast("int8_t") byte readS8();
         public native @Cast("int16_t") short readS16();
         public native @Cast("int32_t") int readS32();
 
         public native @Cast("uint8_t") byte  readU8();
         public native @Cast("uint16_t") short readU16();
         public native @Cast("uint32_t") int readU32();
 
         public native boolean readBool();
         public native @Cast("SkScalar") float readScalar();
         public native @Cast("size_t") int readPackedUInt();
     };
 
     public static class SkWStream extends SkNoncopyable {
         static { Loader.load(Skia.class); }
 
         public native boolean write(@Const Pointer buffer, @Cast("size_t") int size);
         public native void newline();
         public native void flush();
 
         // helpers
 
         public native boolean write8(@Cast("U8CPU") int b);
         public native boolean write16(@Cast("U16CPU") int s);
         public native boolean write32(@Cast("uint32_t") int i);
 
         public native boolean writeText(String text);
         public native boolean writeDecAsText(@Cast("int32_t") int dec);
         public native boolean writeBigDecAsText(@Cast("int64_t") long bigDec, int minDigits/* = 0*/);
         public native boolean writeHexAsText(@Cast("uint32_t") int hex, int minDigits/* = 0*/);
         public native boolean writeScalarAsText(@Cast("SkScalar") float s);
 
         public native boolean writeBool(boolean v);
         public native boolean writeScalar(@Cast("SkScalar") float s);
         public native boolean writePackedUInt(@Cast("size_t") int s);
 
         public native boolean writeStream(SkStream input, @Cast("size_t") int length);
 
         public native boolean writeData(@Const SkData data);
     };
 
     public static class SkFILEStream extends SkStream {
         static { Loader.load(Skia.class); }
 
         public SkFILEStream(String path) {
             allocate(path);
             deallocator(new UnrefDeallocator(this));
         }
         @NoDeallocator
         private native void allocate(String path/* = NULL*/);
 
         public native boolean isValid();
         public native void setPath(String path);
     };
 
     public static class SkDynamicMemoryWStream extends SkWStream {
         static { Loader.load(Skia.class); }
 
         public SkDynamicMemoryWStream() { allocate(); }
         private native void allocate();
 
         public native boolean write(@Const Pointer buffer, @Cast("size_t") int size);
         public native boolean write(@Const Pointer buffer, @Cast("size_t") int offset, @Cast("size_t") int size);
         public native boolean read(Pointer buffer, @Cast("size_t") int offset, @Cast("size_t") int size);
         public native @Cast("size_t") int getOffset();
         public native @Cast("size_t") int bytesWritten();
 
         public native void copyTo(Pointer dst);
 
         public native SkData copyToData();
 
         public native void reset();
         public native void padToAlign4();
     };
 
 	/*
 	 * SkString.h 
 	 */
 	
 	public static class SkString extends Pointer {
 		static { Loader.load(Skia.class); }
 		
 		public SkString() { allocate(); }
 		private native void allocate();
 
         public SkString(int len) { allocate(len); }
         private native void allocate(@Cast("size_t") int len);
 
         public SkString(String text) { allocate(text); }
         private native void allocate(String text);
 
         public SkString(String text, int len) { allocate(text, len); };
         private native void allocate(String text, @Cast("size_t") int len);
 
         public SkString(SkString src) { allocate(src); };
         private native void allocate(@Const @ByRef SkString src);
 
         public native boolean isEmpty();
         public native @Cast("size_t") int size();
         public native String c_str();
         @Name("operator[]")
         public native byte get(@Cast("size_t") int n);
 
         public native boolean equals(@Const @ByRef SkString other);
         public native boolean equals(String text);
         public native boolean equals(String text, @Cast("size_t") int len);
 
         public native boolean startsWith(String prefix);
         public native boolean endsWith(String suffix);
 
 //        friend bool operator==(const SkString& a, const SkString& b) {
 //            return a.equals(b);
 //        }
 //        friend bool operator!=(const SkString& a, const SkString& b) {
 //            return !a.equals(b);
 //        }
 
         @Name("operator=")
         public native @ByRef SkString assign(@Const @ByRef SkString src);
         @Name("operator=")
         public native @ByRef SkString assign(String text);
 
 //        char* writable_str();
 //        char& operator[](size_t n) { return this->writable_str()[n]; }
 
         public native void reset();
         public native void resize(@Cast("size_t") int len);
         public native void set(@Const @ByRef SkString src);
         public native void set(String text);
         public native void set(String text, @Cast("size_t") int len);
         public native void setUTF16(@Cast("const uint16_t*") short[] text);
         public native void setUTF16(@Cast("const uint16_t*") short[] text, @Cast("size_t") int len);
 
         public native void insert(@Cast("size_t") int offset, @Const @ByRef SkString src);
         public native void insert(@Cast("size_t") int offset, String text);
         public native void insert(@Cast("size_t") int offset, String text, @Cast("size_t") int len);
         public native void insertUnichar(@Cast("size_t") int offset, @Cast("SkUnichar") int uni);
         public native void insertS32(@Cast("size_t") int offset, @Cast("int32_t") int value);
         public native void insertS64(@Cast("size_t") int offset, @Cast("int64_t") long value, int minDigits/* = 0*/);
         public native void insertHex(@Cast("size_t") int offset, @Cast("int32_t") int value, int minDigits/* = 0*/);
         public native void insertScalar(@Cast("size_t") int offset, @Cast("SkScalar") float vakye);
 
         public native void append(@Const @ByRef SkString str);
         public native void append(String text);
         public native void append(String text, @Cast("size_t") int len);
         public native void appendUnichar(@Cast("SkUnichar") int uni);
         public native void appendS32(@Cast("int32_t") int value);
         public native void appendS64(@Cast("int64_t") long value, int minDigits/* = 0*/);
         public native void appendHex(@Cast("int32_t") int value, int minDigits/* = 0*/);
         public native void appendScalar(@Cast("SkScalar") float value);
 
         public native void prepend(@Const @ByRef SkString str);
         public native void prepend(String text);
         public native void prepend(String text, @Cast("size_t") int len);
         public native void prependUnichar(@Cast("SkUnichar") int uni);
         public native void prependS32(@Cast("int32_t") int value);
         public native void prependS64(@Cast("int32_t") int value, int minDigits/* = 0*/);
         public native void prependHex(@Cast("int32_t") int value, int minDigits/* = 0*/);
         public native void prependScalar(@Cast("SkScalar") float value);
 
 //        void printf(const char format[], ...);
 //        void appendf(const char format[], ...);
 //        void prependf(const char format[], ...);
 
         public native void remove(@Cast("size_t") int offset, @Cast("size_t") int length);
 
 //        SkString& operator+=(const SkString& s) { this->append(s); return *this; }
 //        SkString& operator+=(const char text[]) { this->append(text); return *this; }
 //        SkString& operator+=(const char c) { this->append(&c, 1); return *this; }
 //
         public native void swap(@ByRef SkString other);
     }
 
 	/*
 	 * SkTypeface.h
 	 */
 	
 	public static class SkTypeface extends SkRefCnt {
 		static { Loader.load(Skia.class); }
 
         protected SkTypeface() {}
 
 	    //enum Style
 		public static final int kNormal = 0,
 				kBold = 0x01,
 				kItalic = 0x02,
 				kBoldItalic = 0x03;
 	    public native @Cast("SkTypeface::Style") int style();
 	    public native boolean isBold();
 	    public native boolean isItalic();
 	    public native boolean isFixedWidth();
 	    public native @Cast("SkFontID") int uniqueID();
 	    public native static boolean Equal(@Const SkTypeface facea, @Const SkTypeface faceb);
 	    public native static SkTypeface CreateFromName(String familyName, @Cast("SkTypeface::Style") int style);
 	    public native static SkTypeface CreateForChars(@Const Pointer data, @Cast("size_t") int bytelength, @Cast("SkTypeface::Style") int style);
 	    public native static SkTypeface CreateFromTypeface(@Const SkTypeface family, @Cast("SkTypeface::Style") int style);
 	    public native static SkTypeface CreateFromFile(String path);
 	}
 
     /*
      * SkTypes.h
      */
 
     public static void SkDebugf(String format, Object... args) {
         System.out.print(String.format(format, args));
     }
 
     public static void SkASSERT(boolean cond) { assert cond; }
 
 //    typedef int S8CPU;
 //    typedef unsigned U8CPU;
 //    typedef int S16CPU;
 //    typedef unsigned U16CPU;
 //    typedef int SkBool;
 //    typedef uint8_t SkBool8;
 
     public static boolean SkToBool(int cond) {  return (cond) != 0; }
 
     //typedef int32_t SkUnichar;
 
     public native static @Cast("int32_t") int SkAbs32(@Cast("int32_t") int value);
 
     public native static @Cast("int32_t") int SkMax32(@Cast("int32_t") int a, @Cast("int32_t") int b);
 
     public native static @Cast("int32_t") int SkMin32(@Cast("int32_t") int a, @Cast("int32_t") int b);
 
     public static class SkNoncopyable extends Pointer {
         static { Loader.load(Skia.class); }
 
         public SkNoncopyable(Pointer p) { super(p); }
         protected SkNoncopyable() {}
     };
 
 	/*
 	 * SkUnitMapper.h
 	 */
 	
 	public static class SkUnitMapper extends SkFlattenable {
 		static { Loader.load(Skia.class); }
 		
 		protected SkUnitMapper() {}
 	}
 	
 	/*
 	 * SkUnPreMultiply.h
 	 */
 	
 	public static class SkUnPreMultiply extends Pointer {
 		static { Loader.load(Skia.class); }
 		
 		public native static @Cast("const SkUnPreMultiply::Scale*") IntPointer GetScaleTable();
 		public native static @Cast("SkUnPreMultiply::Scale") int GetScale(@Cast("U8CPU") int alpha);
 		public native static @Cast("U8CPU") int ApplyScale(@Cast("SkUnPreMultiply::Scale") int scale, @Cast("U8CPU") int component);
 		public native static @Cast("SkColor") int PMColorToColor(@Cast("SkPMColor") int c);
 	}
 	
 	/*
 	 * SkXfermode.h
 	 */
 	
 	public static class SkXfermode extends SkFlattenable {
 		static { Loader.load(Skia.class); }
 
 		protected SkXfermode() {}
 		
 	    //enum Mode
 		public static final int kClear_Mode = 0,
 	        kSrc_Mode = 1,
 	        kDst_Mode = 2,
 	        kSrcOver_Mode = 3,
 	        kDstOver_Mode = 4,
 	        kSrcIn_Mode = 5,
 	        kDstIn_Mode = 6,
 	        kSrcOut_Mode = 7,
 	        kDstOut_Mode = 8,
 	        kSrcATop_Mode = 9,
 	        kDstATop_Mode = 10,
 	        kXor_Mode = 11;
 		public static final int kPlus_Mode = 12,
 	        kMultiply_Mode = 13;
 		public static final int kCoeffModesCnt = 14;
 		public static final int kScreen_Mode = kCoeffModesCnt,
 	        kOverlay_Mode = 15,
 	        kDarken_Mode = 16,
 	        kLighten_Mode= 17,
 	        kColorDodge_Mode = 18,
 	        kColorBurn_Mode = 19,
 	        kHardLight_Mode = 20,
 	        kSoftLight_Mode = 21,
 	        kDifference_Mode = 22,
 	        kExclusion_Mode = 23;
 
         public native static SkXfermode Create(@Cast("SkXfermode::Mode") int mode);
 	}
 }
