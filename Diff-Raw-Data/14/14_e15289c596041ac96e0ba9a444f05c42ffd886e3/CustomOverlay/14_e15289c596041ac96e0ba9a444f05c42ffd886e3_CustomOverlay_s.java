 /*
  * Copyright 2013 OpenScienceMap.org
  *
  * This program is free software: you can redistribute it and/or modify it under the
  * terms of the GNU Lesser General Public License as published by the Free Software
  * Foundation, either version 3 of the License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful, but WITHOUT ANY
  * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public License along with
  * this program. If not, see <http://www.gnu.org/licenses/>.
  */
 package org.oscim.renderer.overlays;
 
 import java.nio.ByteBuffer;
 import java.nio.ByteOrder;
 import java.nio.FloatBuffer;
 
 import org.oscim.core.MapPosition;
 import org.oscim.renderer.GLState;
 import org.oscim.utils.GlUtils;
 import org.oscim.view.MapView;
 
 import android.opengl.GLES20;
 import android.opengl.Matrix;
 
 // http://safari.informit.com/9780321563835
 // http://www.opengles-book.com
 
 public class CustomOverlay extends RenderOverlay {
 
 	private int mProgramObject;
 	private int hVertexPosition;
 	private int hMatrixPosition;
 
 	private FloatBuffer mVertices;
 	private final float[] mVerticesData = {
 			0, 200, 0,
 			-200, -200, 0,
 			200, -200, 0
 	};
 	private boolean mInitialized;
 
 	public CustomOverlay(MapView mapView) {
 		super(mapView);
 	}
 
 	// ---------- everything below runs in GLRender Thread ----------
 	@Override
 	public void update(MapPosition curPos, boolean positionChanged, boolean tilesChanged) {
 		if (!mInitialized) {
 			if (!init())
 				return;
 
 			// tell GLRender to call 'compile' when data has changed
 			newData = true;
 
 			mInitialized = true;
 		}
 	}
 
 	@Override
 	public void compile() {
 		// modify mVerticesData and put in FloatBuffer
 
 		mVertices.clear();
 		mVertices.put(mVerticesData);
 		mVertices.flip();
 
 		newData = false;
 
 		// tell GLRender to call 'render'
 		isReady = true;
 	}
 
 	@Override
 	public void render(MapPosition pos, float[] tmp, float[] proj) {
 
 		// Use the program object
 		GLState.useProgram(mProgramObject);
 
 		GLState.blend(true);
 		GLState.test(false, false);
 
 		// unbind previously bound VBOs
 		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
 
 		// Load the vertex data
 		mVertices.position(0);
 		GLES20.glVertexAttribPointer(hVertexPosition, 3, GLES20.GL_FLOAT, false, 0, mVertices);
 		GLState.enableVertexArrays(hVertexPosition, -1);
 
		// apply view and projection matrices
		Matrix.multiplyMM(tmp, 0, proj, 0, pos.viewMatrix, 0);
 		GLES20.glUniformMatrix4fv(hMatrixPosition, 1, false, tmp, 0);
 
 		// Draw the triangle
 		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
 
 		GlUtils.checkGlError("...");
 	}
 
 	private boolean init() {
 		// Load the vertex/fragment shaders
 		int programObject = GlUtils.createProgram(vShaderStr, fShaderStr);
 
 		if (programObject == 0)
 			return false;
 
 		// Handle for vertex position in shader
 		hVertexPosition = GLES20.glGetAttribLocation(programObject, "a_pos");
 		hMatrixPosition = GLES20.glGetUniformLocation(programObject, "u_mvp");
 
 		// Store the program object
 		mProgramObject = programObject;
 
 		mVertices = ByteBuffer.allocateDirect(mVerticesData.length * 4)
 				.order(ByteOrder.nativeOrder()).asFloatBuffer();
 
 		return true;
 	}
 
 	private final static String vShaderStr =
 			"precision mediump float;"
 					+ "uniform mat4 u_mvp;"
 					+ "attribute vec4 a_pos;"
 					+ "void main()"
 					+ "{"
 					+ "   gl_Position = u_mvp * a_pos;"
 					+ "}";
 
 	private final static String fShaderStr =
 			"precision mediump float;"
 					+ "void main()"
 					+ "{"
 					+ "  gl_FragColor = vec4 ( 0.0, 0.0, 0.0, 0.7 );"
 					+ "}";
 
 }
