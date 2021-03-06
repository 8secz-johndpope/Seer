 package net.schattenkind.androidLove.luan;
 
 import java.io.FileNotFoundException;
 import java.io.InputStream;
 import java.io.IOException;
 import java.nio.ByteBuffer;
 import java.nio.ByteOrder;
 import java.nio.FloatBuffer;
 import java.util.HashMap;
 
 import javax.microedition.khronos.opengles.GL10;
 
 import net.schattenkind.androidLove.LoveVM;
 import net.schattenkind.androidLove.luan.LuanGraphics;
 import net.schattenkind.androidLove.luan.LuanGraphics.LuanDrawable;
 
 import org.luaj.vm2.LuaTable;
 import org.luaj.vm2.LuaValue;
 import org.luaj.vm2.Varargs;
 import org.luaj.vm2.lib.VarArgFunction;
 
 import android.graphics.Bitmap;
 import android.graphics.drawable.BitmapDrawable;
 import android.opengl.GLUtils;
 import android.util.Log;
 
 
 public class LuanImage extends LuanDrawable {
 	private LuanGraphics	g;
 	private int				miTextureID = 0;
 	public float			mWidth;
 	public float			mHeight;
 	public int				mFilterMin = GL10.GL_LINEAR;
 	public int				mFilterMag = GL10.GL_LINEAR;
 	public int				mWrapH = GL10.GL_REPEAT;
 	public int				mWrapV = GL10.GL_REPEAT;
 	public Bitmap			mBitmap;
 		
 	public static LuanImage self (Varargs args) { return (LuanImage)args.checkuserdata(1,LuanImage.class); }
 	
 	public static String	Filter2Str	(int	a) { return (a == GL10.GL_LINEAR)?"linear":"nearest"; }
 	public static int		Str2Filter	(String a) { return (a.equals("linear"))?GL10.GL_LINEAR:GL10.GL_NEAREST; }
 	
 	public static String	Wrap2Str	(int	a) { return (a == GL10.GL_CLAMP_TO_EDGE)?"clamp":"repeat"; }
 	public static int		Str2Wrap	(String a) { return (a.equals("clamp"))?GL10.GL_CLAMP_TO_EDGE:GL10.GL_REPEAT; }
 	
 	public int getColAtPos (int x,int y) { return (mBitmap != null) ? mBitmap.getPixel(x,y) : 0; }
 	
 	public static LuaTable CreateMetaTable (final LuanGraphics g) {
 		LuaTable mt = LuaValue.tableOf();
 		LuaTable t = LuaValue.tableOf();
 		mt.set("__index",t);
 		
 		/// min, mag = Image:getFilter( )	"linear" , "nearest"
 		t.set("getFilter", new VarArgFunction() { @Override public Varargs invoke(Varargs args) { 
 			return LuaValue.varargsOf(	
 				LuaValue.valueOf(Filter2Str(self(args).mFilterMin)) , 
 				LuaValue.valueOf(Filter2Str(self(args).mFilterMag)) ); } });
 		
 		/// Image:setFilter( min, mag )		"linear" , "nearest"
 		t.set("setFilter", new VarArgFunction() { @Override public Varargs invoke(Varargs args) {
 			self(args).setFilter(Str2Filter(args.checkjstring(2)),Str2Filter(args.checkjstring(3)));
 			return LuaValue.NONE;
 		} });
 		
 		/// horiz, vert = Image:getWrap( )	"repeat" , "clamp"
 		t.set("getWrap", new VarArgFunction() { @Override public Varargs invoke(Varargs args) {
 			return LuaValue.varargsOf(
 				LuaValue.valueOf(Wrap2Str(self(args).mWrapH)) , 
 				LuaValue.valueOf(Wrap2Str(self(args).mWrapV)) ); } });
 		
 		/// Image:setWrap( horiz, vert )	"repeat" , "clamp"
 		t.set("setWrap", new VarArgFunction() { @Override public Varargs invoke(Varargs args) {
 			self(args).setWrap(Str2Wrap(args.checkjstring(2)),Str2Wrap(args.checkjstring(3)));
 			return LuaValue.NONE;
 		} });
 		
 		/// w = Image:getWidth( )
 		t.set("getWidth", new VarArgFunction() { @Override public Varargs invoke(Varargs args) { return LuaValue.valueOf(self(args).mWidth); } });
 		
 		/// h = Image:getHeight( )
 		t.set("getHeight", new VarArgFunction() { @Override public Varargs invoke(Varargs args) { return LuaValue.valueOf(self(args).mWidth); } });
 		
 		/// type = Object:type()  , e.g. "Image" or audio:"Source"
 		t.set("type", new VarArgFunction() { @Override public Varargs invoke(Varargs args) { return LuaValue.valueOf("Image"); } });
 		
 		/// b = Object:typeOf( name )
 		t.set("typeOf", new VarArgFunction() { @Override public Varargs invoke(Varargs args) { 
 			String s = args.checkjstring(2); 
			return LuaValue.valueOf(s == "Object" || s == "Drawable" || s == "Image"); 
 		} });
 		
 		
 		return mt;
 	}
 			
 	public void setFilter (int min, int mag) {
 		mFilterMin = min;
 		mFilterMag = mag;
 		GL10 gl = g.getGL();
 		gl.glBindTexture( GL10.GL_TEXTURE_2D, GetTextureID() );
 		gl.glTexParameterf( GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, mFilterMin );
 		gl.glTexParameterf( GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, mFilterMag );
 	}
 	
 	public void setWrap (int h, int v) {
 		mWrapH = h;
 		mWrapV = v;
 		GL10 gl = g.getGL();
 		gl.glBindTexture( GL10.GL_TEXTURE_2D, GetTextureID() );
 		gl.glTexParameterf( GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, mWrapH );
 		gl.glTexParameterf( GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, mWrapV );
 	}
 	
 	public int GetTextureID () { 
 		// TODO: reload if contextswitch detected
 		return miTextureID;
 	}
 	
 	public void LoadFromBitmap (Bitmap bm) {
 		mBitmap = bm;
 		GL10 gl = g.getGL();
 		
 		// Generate one texture pointer
 		int[] textureIds = new int[1];
 		gl.glGenTextures( 1, textureIds, 0 );
 		miTextureID = textureIds[0];
 
 		// bind this texture
 		gl.glBindTexture( GL10.GL_TEXTURE_2D, GetTextureID() );
 
 		// Create Nearest Filtered Texture
 		gl.glTexParameterf( GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, mFilterMin );
 		gl.glTexParameterf( GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, mFilterMag );
 
 		gl.glTexParameterf( GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, mWrapH );
 		gl.glTexParameterf( GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, mWrapV );
 
 		GLUtils.texImage2D( GL10.GL_TEXTURE_2D, 0, bm, 0 ); // texImage2D(int target, int level, Bitmap bitmap, int border
 	}
 	
 	/// load image from resource id, e.g. R.raw.font
 	public LuanImage (LuanGraphics g,int iResID) throws IOException {
 		this(g,g.vm.getResourceInputStream(iResID)); 
 		// TODO: store origin for reload
 	}
 	
 	/// load image from file
 	public LuanImage (LuanGraphics g,String filepath) throws FileNotFoundException {
 		this(g,g.vm.getStorage().getFileStreamFromSdCard(filepath)); 
 		// TODO: store origin for reload
 		// TODO : throw lua error if file not found ?
 		//g.getActivity().openFileInput(filepath);
 		//~ Drawable d = Drawable.createFromStream(input,filepath);
 		//~ Log.i("LuanImage","InputStream ok");
 		//~ Log.i("LuanImage","constructor:"+filepath);
 	}
 	
 	/// don't allow public call, since we need to store the origin of the image stream somehow, so we can reload the file image after context-switch
 	private LuanImage (LuanGraphics g,InputStream input) {
 		this.g = g;
 		
 		// todo : remember filepath so textureid can be reconstructed if lost during context-switch ?
 		
 		//~ http://developer.android.com/resources/samples/ApiDemos/src/com/example/android/apis/graphics/CompressedTextureActivity.html
 		//~ von ressource : InputStream input = getResources().openRawResource(R.raw["bla.png"]);  // NICHT M�GLICH!!!! ->> sd card
 		//~ von ressource : InputStream input = getResources().openRawResource(R.raw.androids);
 		//~ von sd laden : InputStreamODERSO input = openFileInput("lala.lua");
 		//~ GLUtils.texImage2D : http://gamedev.stackexchange.com/questions/10829/loading-png-textures-for-use-in-android-opengl-es1		(see also comments/answers)
 		//~ static Drawable 	Drawable.createFromStream(InputStream is, String srcName)
 		
 		BitmapDrawable bmd = new BitmapDrawable(g.vm.getResources(),input); // ressources needed for "density" / dpi etc ?  no idea
 		Log.i("LuanImage","BitmapDrawable ok");
 		Bitmap bm = bmd.getBitmap();
 		Log.i("LuanImage","Bitmap ok w="+bm.getWidth()+",h="+bm.getHeight());
 		mWidth = bm.getWidth();
 		mHeight = bm.getHeight();
 		// TODO : auto-scale to 2^n resolution ? naaaah.
 		// bitmap loaded into ram
 		
 		// load into texture
 		LoadFromBitmap(bm);
 		Log.i("LuanImage","LoadFromBitmap done.");
 		
 		// release bitmap ram
 		//~ bm.recycle(); // MEMORY LEAK.. needed for font glyph stuff tho. or store path and re-load on demand
 		
 		Log.i("LuanImage","constructor done.");
 	}
 }
