 package jp.gauzau.MikuMikuDroid;
 
 import java.util.ArrayList;
 
 import javax.microedition.khronos.egl.EGLConfig;
 import javax.microedition.khronos.opengles.GL10;
 
 import android.opengl.GLES20;
 import android.opengl.GLUtils;
 import android.util.Log;
 
 public class MikuRendererGLES20 extends MikuRendererBase {
 
 	private String TAG = "MikuRendarGLES20";
 	
 	class GLSL {
 		public int mProgram;
 		public int maPositionHandle;
 		public int maBlendHandle;
 		public int msTextureSampler;
 		public int msToonSampler;
 		public int msSphereSampler;
 		public int muTexEn;
 		public int muSpaEn;
 		public int muSphEn;
 		public int muDif;
 		public int muSpec;
 		public int muAmb;
 		public int muPow;
 		public int maNormalHandle;
 		public int muMBone;
 
 		public int muPMatrix;
 		public int muLightDir;
 		
 		public GLSL(String v, String f) {
 			mProgram = createProgram(v, f);
 			if (mProgram == 0) {
 				return;
 			}
 
 			GLES20.glUseProgram(mProgram);
 			checkGlError("glUseProgram");
 
 			// attribute & uniform handles
 			maPositionHandle	= GLES20.glGetAttribLocation(mProgram, "aPosition");
 			maNormalHandle		= GLES20.glGetAttribLocation(mProgram, "aNormal");
 			maBlendHandle		= GLES20.glGetAttribLocation(mProgram, "aBlend");
 
 			muPMatrix			= GLES20.glGetUniformLocation(mProgram, "uPMatrix");
 			msTextureSampler	= GLES20.glGetUniformLocation(mProgram, "sTex");
 			msToonSampler		= GLES20.glGetUniformLocation(mProgram, "sToon");
 			msSphereSampler		= GLES20.glGetUniformLocation(mProgram, "sSphere");
 			muTexEn				= GLES20.glGetUniformLocation(mProgram, "bTexEn");
 			muSpaEn				= GLES20.glGetUniformLocation(mProgram, "bSpaEn");
 			muSphEn				= GLES20.glGetUniformLocation(mProgram, "bSphEn");
 			muDif				= GLES20.glGetUniformLocation(mProgram, "uDif");
 			muSpec				= GLES20.glGetUniformLocation(mProgram, "uSpec");
 			muAmb				= GLES20.glGetUniformLocation(mProgram, "uAmb");
 			muPow				= GLES20.glGetUniformLocation(mProgram, "uPow");
 			muMBone				= GLES20.glGetUniformLocation(mProgram, "uMBone");
 			muLightDir			= GLES20.glGetUniformLocation(mProgram, "uLightDir");
 		}
 		
 		private int loadShader(int shaderType, String source) {
 			int shader = GLES20.glCreateShader(shaderType);
 			if (shader != 0) {
 				GLES20.glShaderSource(shader, source);
 				GLES20.glCompileShader(shader);
 				int[] compiled = new int[1];
 				GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
 				if (compiled[0] == 0) {
 					Log.e(TAG, "Could not compile shader " + shaderType + ":");
 					Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
 					Log.e(TAG, "message ends.");
 					GLES20.glDeleteShader(shader);
 					shader = 0;
 				}
 			}
 			return shader;
 		}
 
 		private int createProgram(String vertexSource, String fragmentSource) {
 			int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
 			if (vertexShader == 0) {
 				return 0;
 			}
 
 			int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
 			if (pixelShader == 0) {
 				return 0;
 			}
 
 			int program = GLES20.glCreateProgram();
 			if (program != 0) {
 				GLES20.glAttachShader(program, vertexShader);
 				checkGlError("glAttachShader");
 				GLES20.glAttachShader(program, pixelShader);
 				checkGlError("glAttachShader");
 				GLES20.glLinkProgram(program);
 				int[] linkStatus = new int[1];
 				GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
 				if (linkStatus[0] != GLES20.GL_TRUE) {
 					Log.e(TAG, "Could not link program: ");
 					Log.e(TAG, GLES20.glGetProgramInfoLog(program));
 					GLES20.glDeleteProgram(program);
 					program = 0;
 				}
 			}
 			return program;
 		}
 		
 		
 	};
 
 	private GLSL mGLSL;
 	private GLSL mGLSLBG;
 	
 	public float mBoneMatrix[];
 	private float[] mLightDir = new float[3];
 
 	public MikuRendererGLES20(CoreLogic cl) {
 		super(cl);
 		mBoneMatrix		= new float[16 * 256];	// ad-hock number: will be fixed to mBoneNum
 		clear();
 	}
 
 	@Override
 	public void onDrawFrame(GL10 gl) {
 		mLightDir[0] = -0.5f; mLightDir[1] = -1.0f; mLightDir[2] = -0.5f;	// in left-handed region
 		Vector.normalize(mLightDir);
 		
 		GLES20.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
 
 		int pos = mCoreLogic.applyCurrentMotion();
 
 		GLES20.glUseProgram(mGLSL.mProgram);
 
 		// Projection Matrix
 		GLES20.glUniformMatrix4fv(mGLSL.muPMatrix, 1, false, mCoreLogic.getProjectionMatrix(), 0);
 
 		// LightPosition
		GLES20.glUniform3fv(mGLSL.muLightDir, 1, mLightDir, 0);
 
 		GLES20.glUniform1i(mGLSL.msToonSampler, 0);
 		GLES20.glUniform1i(mGLSL.msTextureSampler, 1);
 		GLES20.glUniform1i(mGLSL.msSphereSampler, 2);
 		checkGlError("on onDrawFrame");
 
 		if (mCoreLogic.getMiku() != null) {
 			for (Miku miku : mCoreLogic.getMiku()) {
 				if(miku.mModel.mIsTextureLoaded == false) {
 					initializeTextures(miku);
 					miku.mModel.mIsTextureLoaded = true;
 				}
 				bindBufferGLES20(miku.mModel, mGLSL);
 				drawGLES20(miku.mModel, mGLSL);
 			}
 		}
 
 		if (mCoreLogic.getMikuStage() != null) {
 			GLES20.glUseProgram(mGLSLBG.mProgram);
 
 			// Projection, Model, View Matrix
 			GLES20.glUniformMatrix4fv(mGLSLBG.muPMatrix, 1, false, mCoreLogic.getProjectionMatrix(), 0);
 
 			// LightPosition
 			GLES20.glUniform3fv(mGLSLBG.muLightDir, 1, mLightDir, 0);		
 
 			GLES20.glUniform1i(mGLSLBG.msToonSampler, 0);
 			GLES20.glUniform1i(mGLSLBG.msTextureSampler, 1);
 			checkGlError("on onDrawFrame");
 
 			if(mCoreLogic.getMikuStage().mModel.mIsTextureLoaded == false) {
 				initializeTextures(mCoreLogic.getMikuStage());
 				mCoreLogic.getMikuStage().mModel.mIsTextureLoaded = true;
 			}				
 			bindBufferGLES20(mCoreLogic.getMikuStage().mModel, mGLSLBG);
 			drawGLES20(mCoreLogic.getMikuStage().mModel, mGLSLBG);
 		}
 
 		GLES20.glFlush();
 		mCoreLogic.onDraw(pos);
 	}
 
 	@Override
 	public void onSurfaceChanged(GL10 gl, int width, int height) {
 		super.onSurfaceChanged(gl, width, height);
 		GLES20.glViewport(0, 0, width, height);
 	}
 
 	@Override
 	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
 		GLES20.glClearColor(1, 1, 1, 1);
 
 		GLES20.glEnable(GLES20.GL_CULL_FACE);
 		GLES20.glFrontFace(GLES20.GL_CW);
 		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
 
 		// GLUtils.texImage2D generates premultiplied-alpha texture. so we use GL_ONE instead of GL_ALPHA
 		GLES20.glEnable(GLES20.GL_BLEND);
 		GLES20.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);
 //		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
 
 		GLES20.glDepthFunc(GLES20.GL_LEQUAL);
 //		GLES20.glEnable(GLES20.GL_POLYGON_OFFSET_FILL);
 //		GLES20.glPolygonOffset(-1.0f, -2.0f);
 		
 		// sharder program
 		int bonenum = 48;
 		mCoreLogic.setGLConfig(bonenum);
 		mGLSL = new GLSL(String.format(mCoreLogic.getRawResourceString(R.raw.vs), bonenum), mCoreLogic.getRawResourceString(R.raw.fs));
 //		mGLSL = new GLSL(String.format(mCoreLogic.getRawResourceString(R.raw.vs_simple), bonenum), mCoreLogic.getRawResourceString(R.raw.fs_simple));
 //		mGLSL = new GLSL(String.format(mCoreLogic.getRawResourceString(R.raw.vs_sph), bonenum), mCoreLogic.getRawResourceString(R.raw.fs_sph));
 		if (mCoreLogic.getMiku() != null) {
 			for (Miku miku : mCoreLogic.getMiku()) {
 				initializeTextures(miku);
 			}
 		}
 
 		// sharder program in no animation
 		mGLSLBG = new GLSL(mCoreLogic.getRawResourceString(R.raw.vs_nm), mCoreLogic.getRawResourceString(R.raw.fs_nm));
 		if (mCoreLogic.getMikuStage() != null) {
 			initializeTextures(mCoreLogic.getMikuStage());
 		}
 	}
 	
 	
 	// for GLES20
 	private void drawGLES20(MikuModel miku, GLSL glsl) {
 		ArrayList<Material> rendar = miku.mAnimation ? miku.mRendarList : miku.mMaterial;
 		ArrayList<Bone> bs = miku.mBone;
 	
 		int max = rendar.size();
 		for (int r = 0; r < max; r++) {
 			Material mat = rendar.get(r);
 			if (miku.mAnimation) {
 				for (int j = 0; j < miku.mRenameBone; j++) {
 					int inv = mat.rename_inv_map[j];
 					if (inv >= 0) {
 						Bone b = bs.get(inv);
 						System.arraycopy(b.matrix, 0, mBoneMatrix, j * 16, 16);
 					}
 				}
 				GLES20.glUniformMatrix4fv(glsl.muMBone, mat.rename_hash_size, false, mBoneMatrix, 0);
 	
 				GLES20.glEnableVertexAttribArray(glsl.maBlendHandle);
 				GLES20.glVertexAttribPointer(glsl.maBlendHandle, 3, GLES20.GL_UNSIGNED_BYTE, false, 0, mat.rename_index);
 				checkGlError("drawGLES20 VertexAttribPointer blend");
 			}
 	
 			// don't cull face that has alpha value 0.99
 			if(mat.diffuse_color[3] == 0.99f) {
 				GLES20.glDisable(GLES20.GL_CULL_FACE);
 			} else {
 				GLES20.glEnable(GLES20.GL_CULL_FACE);
 			}
 	
 			// Toon texture
 			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
 			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, miku.mToon.get(mat.toon_index).tex);
 
 			if (mat.texture != null) {
 				TexBitmap tb = miku.mTexture.get(mat.texture);
 				if(tb != null) {
 					GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
 					GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, miku.mTexture.get(mat.texture).tex);
 					GLES20.glUniform1i(glsl.muTexEn, 1);					
 				} else {	// avoid crash
 					GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
 					GLES20.glUniform1i(glsl.muTexEn, 0);					
 				}
 			} else {
 				GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
 				GLES20.glUniform1i(glsl.muTexEn, 0);
 			}				
 			if(glsl.muSphEn >= 0) {
 				if (mat.sphere != null) {
 					GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
 					GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, miku.mTexture.get(mat.sphere).tex);
 					if(mat.sphere.endsWith(".spa")) {
 						GLES20.glUniform1i(glsl.muSpaEn, 1);					
 						GLES20.glUniform1i(glsl.muSphEn, 0);
 					} else {
 						GLES20.glUniform1i(glsl.muSpaEn, 0);					
 						GLES20.glUniform1i(glsl.muSphEn, 1);					
 					}
 				} else {
 					GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
 					GLES20.glUniform1i(glsl.muSpaEn, 0);
 					GLES20.glUniform1i(glsl.muSphEn, 0);
 				}
 			}
 
 			checkGlError("on DrawGLES20");
 			
 			float wi = 0.6f;	// light color = (0.6, 0.6, 0.6)
 			if(glsl.muAmb >= 0) {
 				GLES20.glUniform4f(glsl.muDif, mat.diffuse_color[0] * wi, mat.diffuse_color[1] * wi, mat.diffuse_color[2] * wi, mat.diffuse_color[3]);
 				GLES20.glUniform4f(glsl.muAmb, mat.emmisive_color[0], mat.emmisive_color[1], mat.emmisive_color[2], 0);				
 			} else {
 				GLES20.glUniform4f(glsl.muDif, 
 						 (mat.diffuse_color[0] * wi + mat.emmisive_color[0]),
 					     (mat.diffuse_color[1] * wi + mat.emmisive_color[1]),
 					     (mat.diffuse_color[2] * wi + mat.emmisive_color[2]), mat.diffuse_color[3]);
 			}
 			if (glsl.muPow >= 0) {
 				GLES20.glUniform4f(glsl.muSpec, mat.specular_color[0], mat.specular_color[1], mat.specular_color[2], 0);
 				GLES20.glUniform1f(glsl.muPow, mat.power);
 			}
 			miku.mIndexBuffer.position(mat.face_vart_offset);
 			GLES20.glDrawElements(GLES20.GL_TRIANGLES, mat.face_vert_count, GLES20.GL_UNSIGNED_SHORT, miku.mIndexBuffer);
 			checkGlError("glDrawElements");
 		}
 		miku.mIndexBuffer.position(0);
 	}
 
 	private void initializeTextures(Miku miku) {
 		// toon shading
 		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
 		miku.mModel.readToonTexture();
 		bindToonTextureGLES20(miku.mModel);
 
 		// Texture
 		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
 		miku.mModel.readAndBindTextureGLES20();
 		
 		// Sphere
 		GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
 		miku.mModel.readAndBindSphereTextureGLES20();
 	}
 
 	private void bindBufferGLES20(MikuModel miku, GLSL glsl) {
 		GLES20.glEnableVertexAttribArray(glsl.maPositionHandle);
 		miku.mAllBuffer.position(0);
 		GLES20.glVertexAttribPointer(glsl.maPositionHandle, 4, GLES20.GL_FLOAT, false, 8 * 4, miku.mAllBuffer);
 		checkGlError("drawGLES20 VertexAttribPointer vertex");
 
 		GLES20.glEnableVertexAttribArray(glsl.maNormalHandle);
 		miku.mAllBuffer.position(4);
 		GLES20.glVertexAttribPointer(glsl.maNormalHandle, 4, GLES20.GL_FLOAT, false, 8 * 4, miku.mAllBuffer);
 		checkGlError("drawGLES20 VertexAttribPointer normal");
 		miku.mAllBuffer.position(0);
 	}
 	
 	private void bindToonTextureGLES20(MikuModel miku) {
 		int tex[] = new int[11];
 		GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
 		GLES20.glGenTextures(11, tex, 0);
 	
 		for (int i = 0; i < 11; i++) {
 			TexBitmap tb = miku.mToon.get(i);
 			tb.tex = tex[i];
 	
 			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tb.tex);
 			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
 			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
 			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
 			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
 			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, tb.bmp, 0);
 			tb.bmp.recycle();
 		}
 	}
 	
 	private void checkGlError(String op) {
 		int error;
 		while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
 			Log.e(TAG, op + ": glError " + error);
 //			throw new RuntimeException(op + ": glError " + error);
 		}
 	}
 }
