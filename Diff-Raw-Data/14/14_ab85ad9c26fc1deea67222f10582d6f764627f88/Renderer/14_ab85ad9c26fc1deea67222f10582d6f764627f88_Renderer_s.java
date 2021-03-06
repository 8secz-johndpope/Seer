 package net.georgewhiteside.android.abstractart;
 
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.nio.ByteBuffer;
 import java.nio.ByteOrder;
 import java.nio.FloatBuffer;
 import java.util.Random;
 
 import javax.microedition.khronos.egl.EGLConfig;
 import javax.microedition.khronos.opengles.GL10;
 
 import android.content.Context;
 import android.opengl.GLES20;
 import android.opengl.GLSurfaceView;
 import android.opengl.Matrix;
 import android.util.Log;
 
 // float refreshrate = getWindowManager().getDefaultDisplay().getRefreshRate();
 
 // "The PowerVR 530/535 is very slow. Andreno 200 and PowerVR 530/535 are first GPU generation
 // (OpenGL ES 2.x) for hdpi resolution. You can't redraw a full screen at 60FPS with a simple texture."
 
 public class Renderer implements GLSurfaceView.Renderer
 {
 	private static final String TAG = "Renderer";
 	private Context mContext;
 	
 	private FPSCounter mFPSCounter = new FPSCounter();
 	
 	private FloatBuffer quadVertexBuffer;
 	private FloatBuffer textureVertexBuffer;
 	
 	private int mProgram, hFXProgram;
 	private int mPositionHandle, hPosition;
 	private int mTextureHandle, hTexture;
 	private int mBaseMapTexId;
 	private int mBaseMapLoc, hBaseMap;
 	
 	private int mTick;
 	
 	private int mResolutionLoc;
 	private int mAmplitudeLoc, mFrequencyLoc, mCompressionLoc;
 	private int mAmplitudeDeltaLoc, mFrequencyDeltaLoc, mCompressionDeltaLoc;
 	private int mSpeedLoc;
 	private int mDistTypeLoc;
 	private int mTickLoc;
 	private int mOffsetLoc;
 	private int mDistortionDurationLoc;
 	
 	private int mSurfaceWidth;
 	private int mSurfaceHeight;
 	
 	private int hMVPMatrix;
 	private float[] mProjMatrix = new float[16];
 	
 	private int[] mFramebuffer = new int[1];
 	private int[] mRenderTexture = new int[1];
 	
 	private Boolean mHighRes = false;
 	private Boolean mFilterOutput = true;
 	
 	private BattleBackground bbg;
 	private int temp;
 	
 	private FloatBuffer textureVertexBufferUpsideDown;
 	
 	ByteBuffer byteBuffer;
 	
 	public void RandomBackground()
 	{
 		Random rand = new Random();
 		int number = rand.nextInt(bbg.getNumberOfBackgrounds());
 		//number = 133; // layer 95
 		loadBattleBackground(number);
 	}
 	
 	public Renderer(Context context)
 	{
 		//Random rand = new Random();
 		//temp = rand.nextInt(327);
 		
 		mContext = context;
 		mTick = 0;
 		bbg = new BattleBackground(mContext.getResources().openRawResource(R.raw.bgbank));
 		byteBuffer = ByteBuffer.allocateDirect(256 * 256 * 3);
 	}
 	
 	public void onDrawFrame(GL10 unused)
 	{
 		mFPSCounter.logFrame();
 		
 		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0); // target screen
 		GLES20.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);
 		
 		// used to update shader variables here; remove me
 		
 		
 		
 		if(bbg.layerA.distortion.hasCycled() == true)
 		{
 			mTick = 0;
 		}
 		
 		if(mHighRes)
 		{
 			renderBattleBackground();
 		}
 		else
 		{
 			renderToTexture();
 		}
 			
 		mFPSCounter.logEndFrame();
 		
 		bbg.layerA.distortion.doTick();
 		bbg.layerA.translation.doTick();	
 		
 		mTick += 1;
 	}
 
 	public void onSurfaceChanged(GL10 unused, int width, int height)
 	{
 		mSurfaceWidth = width;
 		mSurfaceHeight = height;
 		GLES20.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);
 		
 		float ratio = (float) mSurfaceWidth / mSurfaceHeight;	
 		Matrix.orthoM(mProjMatrix, 0, -ratio, ratio, -1.0f, 1.0f, 0.0f, 2.0f);	// configure projection matrix
 	}
 	
 	private void setupQuad()
 	{
 		float quadVertices[] =
 		{
 			-1.0f,	-1.0f,	 0.0f,
 			 1.0f,	-1.0f,	 0.0f,
 			-1.0f,	 1.0f,	 0.0f,
 			 1.0f,	 1.0f,	 0.0f			 
 		};
 		
 		float textureMap[] =
 		{
 				0.0f,	 1.0f,
 				 1.0f,	 1.0f,
 				 0.0f,	 0.0f,
 				 1.0f,	 0.0f 
 		};
 
 		quadVertexBuffer = ByteBuffer
 				.allocateDirect(quadVertices.length * 4) // float is 4 bytes
 				.order(ByteOrder.nativeOrder())
 				.asFloatBuffer(); 
 		quadVertexBuffer.put(quadVertices);
 		quadVertexBuffer.position(0);
 		
 		textureVertexBuffer = ByteBuffer
 				.allocateDirect(textureMap.length * 4) // float is 4 bytes
 				.order(ByteOrder.nativeOrder())
 				.asFloatBuffer(); 
 		textureVertexBuffer.put(textureMap);
 		textureVertexBuffer.position(0);
 		
 		if(mHighRes == false)
 		{
 			float textureMapUpsideDown[] =
 			{
 					0.0f,	 0.0f,
 					 1.0f,	 0.0f,
 					 0.0f,	 1.0f,
 					 1.0f,	 1.0f 
 			};
 			
 			textureVertexBufferUpsideDown = ByteBuffer
 					.allocateDirect(textureMapUpsideDown.length * 4) // float is 4 bytes
 					.order(ByteOrder.nativeOrder())
 					.asFloatBuffer(); 
 			textureVertexBufferUpsideDown.put(textureMapUpsideDown);
 			textureVertexBufferUpsideDown.position(0);
 		}
 	}
 
 	public void onSurfaceCreated( GL10 unused, EGLConfig config )
 	{
 		
 		setupQuad();
 		
 		GLES20.glClearColor( 0.5f, 0.5f, 0.5f, 0.0f );	// set surface background color
 		GLES20.glDisable(GLES20.GL_DITHER); // dithering causes really crappy/distracting visual artifacts when distorting the textures
 		
 		/* */
 		GLES20.glGenFramebuffers(1, mFramebuffer, 0);
 		GLES20.glGenTextures(1, mRenderTexture, 0);
 		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mRenderTexture[0]);
 		GLES20.glTexImage2D( GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, 256, 256, 0, GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, null );//GLES20.GL_UNSIGNED_SHORT_5_6_5, null ); //GLES20.GL_UNSIGNED_BYTE, null );
 		GLES20.glTexParameteri( GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR );
 		GLES20.glTexParameteri( GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR );
 		GLES20.glTexParameteri( GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE );
 		GLES20.glTexParameteri( GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE );
 		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebuffer[0]); // do I need to do this here?
 		GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, mRenderTexture[0], 0); // specify texture as color attachment
 		
 		/* shader for texture (the "low res") output */
 		
 		hFXProgram = createProgram(readTextFile(R.raw.passthrough_vert), readTextFile(R.raw.passthrough_frag));
 		if(hFXProgram == 0) { throw new RuntimeException("[...] shader compilation failed"); }
 		
 		hPosition = GLES20.glGetAttribLocation(hFXProgram, "a_position"); // a_position
 		hTexture = GLES20.glGetAttribLocation(hFXProgram, "a_texCoord"); // a_texCoord
 		hBaseMap = GLES20.glGetUniformLocation(hFXProgram, "s_texture"); // get sampler locations
 		/******** experimental FBO shit ********/
 		
 
 		/* shader for effects */
 		
 		mProgram = createProgram(readTextFile(R.raw.aspect_vert), readTextFile(R.raw.distortion_frag));
 		if(mProgram == 0) { throw new RuntimeException("[...] shader compilation failed"); }
 		
 		mPositionHandle = GLES20.glGetAttribLocation(mProgram, "a_position"); // a_position
 		mTextureHandle = GLES20.glGetAttribLocation(mProgram, "a_texCoord"); // a_texCoord
 		mBaseMapLoc = GLES20.glGetUniformLocation(mProgram, "s_texture"); // get sampler locations
 		
 		mResolutionLoc = GLES20.glGetUniformLocation(mProgram, "resolution");
 		mAmplitudeLoc = GLES20.glGetUniformLocation(mProgram, "u_ampl");
 		mFrequencyLoc = GLES20.glGetUniformLocation(mProgram, "u_freq");
 		mCompressionLoc = GLES20.glGetUniformLocation(mProgram, "u_comp");
 		mAmplitudeDeltaLoc = GLES20.glGetUniformLocation(mProgram, "u_ampl_delta");
 		mFrequencyDeltaLoc = GLES20.glGetUniformLocation(mProgram, "u_freq_delta");
 		mCompressionDeltaLoc = GLES20.glGetUniformLocation(mProgram, "u_comp_delta");
 		mSpeedLoc = GLES20.glGetUniformLocation(mProgram, "u_speed");
 		mDistTypeLoc = GLES20.glGetUniformLocation(mProgram, "u_dist_type");
 		mDistortionDurationLoc = GLES20.glGetUniformLocation(mProgram, "u_dist_duration");
 		mTickLoc = GLES20.glGetUniformLocation(mProgram, "u_tick");
 		mOffsetLoc = GLES20.glGetUniformLocation(mProgram, "scroll");
 		
 		Random rand = new Random();
 		temp = rand.nextInt(bbg.getNumberOfBackgrounds());
 		
 		// layer entries
 		//temp = 223; // giygas
 		//temp = 226; // giygas
 		//temp = 250; // giygas
 		//temp = 1; // spiteful crow
 		
 		//temp = 155;
 		//temp = 31;
 		//temp = 148;
 		
 		//temp = 175;	// 4-way linear translation
 		//temp = 75; // 0-duration linear translation
 		
 		temp = 220;
 		temp = 113;
 		temp = 206;
		temp = 223;
 		
 		loadBattleBackground(temp);
 		
 	}
 	
 	private void updateShaderVariables()
 	{
 		// glUniform* calls always act on the current program that is bound with glUseProgram
 		// have this method take an argument to determine which program to apply to
 		
 		Layer layerA = bbg.getLayerA();
 		
 		// update shader resolution
 		
		GLES20.glUniform2f(mResolutionLoc, (float)mSurfaceWidth, (float)mSurfaceHeight);
 		
 		// update distortion effect variables for the shader program
 		
 		GLES20.glUniform1f(mAmplitudeLoc, layerA.distortion.getAmplitude());
 		GLES20.glUniform1f(mFrequencyLoc, layerA.distortion.getFrequency());
 		GLES20.glUniform1f(mCompressionLoc, layerA.distortion.getCompression());
 		GLES20.glUniform1f(mAmplitudeDeltaLoc, layerA.distortion.getAmplitudeDelta());
 		GLES20.glUniform1f(mFrequencyDeltaLoc, layerA.distortion.getFrequencyDelta());
 		GLES20.glUniform1f(mCompressionDeltaLoc, layerA.distortion.getCompressionDelta());
 		GLES20.glUniform1f(mSpeedLoc, layerA.distortion.getSpeed());
 		GLES20.glUniform1i(mDistTypeLoc, layerA.distortion.getType() == Distortion.UNKNOWN ? Distortion.HORIZONTAL_INTERLACED : layerA.distortion.getType()); // TODO I'm currently treating distortion type 4 as 2 ... figure it must mean "horizontal interlaced + (something else)"
 		GLES20.glUniform1f(mDistortionDurationLoc, layerA.distortion.getDuration());
 		GLES20.glUniform1i(mTickLoc, mTick);
 		
 		// update translation effect variables for the shader program
 		
 		GLES20.glUniform2f(mOffsetLoc, layerA.translation.getHorizontalOffset(), layerA.translation.getVerticalOffset());
 	}
 	
 	public void loadBattleBackground(int index)
 	{	
 		//bbg.setLayers(296, 296);
 		bbg.setIndex(index);
 		byte[] data = bbg.getLayerA().getImage();
 		mTick = 0;
 		
 		bbg.layerA.distortion.dump(0);
 		bbg.layerA.translation.dump(0);
 
 		
 		int[] textureId = new int[1];
 		//ByteBuffer byteBuffer = ByteBuffer.allocateDirect(256 * 256 * 3);
         byteBuffer.put(data).position(0);
             
         GLES20.glGenTextures ( 1, textureId, 0 );
         GLES20.glBindTexture ( GLES20.GL_TEXTURE_2D, textureId[0] );
 
         GLES20.glTexImage2D ( GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, 256, 256, 0, GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, byteBuffer );
     
         int filter = mFilterOutput ? GLES20.GL_LINEAR : GLES20.GL_NEAREST;
         
         GLES20.glTexParameteri ( GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, filter );
         GLES20.glTexParameteri ( GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, filter );
         GLES20.glTexParameteri ( GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT );
         GLES20.glTexParameteri ( GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT );
         
         mBaseMapTexId = textureId[0];
 	}
 	
 	private void renderToTexture() // "low res" render
 	{
 		GLES20.glViewport(0, 0, 256, 256);	// render to native texture size, scale up later
 		
 		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
 		
 		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebuffer[0]);
 		
 		/* it may be prudent to check the framebuffer status here before continuing... */
 		
 		renderBattleBackground();
 		
 		/* now, try to render the texture? */
 		
 		GLES20.glUseProgram(hFXProgram);
 		
 		GLES20.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);		// now we're scaling the framebuffer up to size
 		
 		hMVPMatrix = GLES20.glGetUniformLocation(hFXProgram, "uMVPMatrix");/* projection and camera */
 		
 		/* load vertex positions */
 		
 		GLES20.glVertexAttribPointer(hPosition, 3, GLES20.GL_FLOAT, false, 12, quadVertexBuffer);
 		GLES20.glEnableVertexAttribArray(hPosition);
 		
 		/* load texture mapping */
 
 		
 		GLES20.glVertexAttribPointer(hTexture, 2, GLES20.GL_FLOAT, false, 8, textureVertexBufferUpsideDown);
 		GLES20.glEnableVertexAttribArray(hTexture);
 		
 		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
 		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mRenderTexture[0]);
 		
 		GLES20.glUniform1i(hBaseMap, 0);
 		
 		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
 		
 		GLES20.glUniformMatrix4fv(hMVPMatrix, 1, false, mProjMatrix, 0);	/* projection and camera */
 		
 		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
 	}
 	
 	private void renderBattleBackground() // "high res" render
 	{
 		
 		hMVPMatrix = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");/* projection and camera */
 		
 		//GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0); // render to screen buffer
 		
 		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
 		
 		GLES20.glUseProgram(mProgram);
 		
 		/* load vertex positions */
 		
 		GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 12, quadVertexBuffer);
 		GLES20.glEnableVertexAttribArray(mPositionHandle);
 		
 		/* load texture mapping */
 
 		
 		GLES20.glVertexAttribPointer(mTextureHandle, 2, GLES20.GL_FLOAT, false, 8, textureVertexBuffer);
 		GLES20.glEnableVertexAttribArray(mTextureHandle);
 		
 		//GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
 		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mBaseMapTexId);
 		GLES20.glUniform1i(mBaseMapLoc, 0);
 		
 		updateShaderVariables(); // be mindful of which active program this applies to!!
 		
 		/* apply model view projection transformation */
 		
 		//Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mVMatrix, 0);			/* projection and camera */
 		GLES20.glUniformMatrix4fv(hMVPMatrix, 1, false, mProjMatrix, 0);	/* projection and camera */
 		
 		/* draw the triangles */
 		
 		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
 	}
 	
 	private void checkGlError(String op)
 	{
 		/* from developer.android.com */
 		int error;
 		while((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
 			Log.e(TAG, op + ": glError " + error);
 			throw new RuntimeException(op + ": glError " + error);
 		}
 	}
 	
 	private int createProgram(String vertexSource, String fragmentSource)
 	{
 		// courtesy of android.developer.com
 		int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
 		if(vertexShader == 0) {
 			return 0;
 		}
 
 		int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
 		if(pixelShader == 0) {
 			return 0;
 		}
 
 		int program = GLES20.glCreateProgram();
 		if(program != 0) {
 			GLES20.glAttachShader(program, vertexShader);
 			checkGlError("glAttachShader");
 			GLES20.glAttachShader(program, pixelShader);
 			checkGlError("glAttachShader");
 			GLES20.glLinkProgram(program);
 			int[] linkStatus = new int[1];
 			GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
 			if(linkStatus[0] != GLES20.GL_TRUE) {
 				Log.e(TAG, "Could not link program: ");
 				Log.e(TAG, GLES20.glGetProgramInfoLog(program));
 				GLES20.glDeleteProgram(program);
 				program = 0;
 			}
 		}
 		return program;
 	}
 	
 	private int loadShader(int shaderType, String source)
 	{
 		
 //	int loadShader(int type, String code) {
 //		int shader = GLES20.glCreateShader(type);
 //		GLES20.glShaderSource(shader, code);
 //		GLES20.glCompileShader(shader);
 //		return shader;
 //	}
 		
 		int shader = GLES20.glCreateShader(shaderType);
 		if(shader != 0) {
 			GLES20.glShaderSource(shader, source);
 			GLES20.glCompileShader(shader);
 			int[] compiled = new int[1];
 			GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
 			if(compiled[0] == 0) {
 				Log.e(TAG, "Could not compile shader " + shaderType + ":");
 				Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
 				GLES20.glDeleteShader(shader);
 				shader = 0;
 			}
 		}
 		return shader;
 	}
 	
 	private String readTextFile(final int resourceId)
 	{
 		/* method lifted from learnopengles.com */
 		final InputStream inputStream = mContext.getResources().openRawResource(resourceId);
 		final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
 		final BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
  
 		String nextLine;
 		final StringBuilder body = new StringBuilder();
 		 
 		try
 		{
 			while ((nextLine = bufferedReader.readLine()) != null)
 			{
 				body.append(nextLine);
 				body.append('\n');
 			}
 		}
 		catch (IOException e)
 		{
 			return null;
 		}
  
 		return body.toString();
 	}
 }
