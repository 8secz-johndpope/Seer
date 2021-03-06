 /*
  * Copyright (C) 2012 CyberAgent
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package jp.co.cyberagent.android.gpuimage;
 
 import android.graphics.Bitmap;
 import android.opengl.GLES20;
 
 import java.nio.ByteBuffer;
 import java.nio.ByteOrder;
 import java.nio.FloatBuffer;
 
 public class GPUImageTwoInputFilter extends GPUImageFilter {
     private static final String VERTEX_SHADER = "attribute vec4 position;\n" +
             "attribute vec4 inputTextureCoordinate;\n" +
             "attribute vec4 inputTextureCoordinate2;\n" +
             " \n" +
             "varying vec2 textureCoordinate;\n" +
             "varying vec2 textureCoordinate2;\n" +
             " \n" +
             "void main()\n" +
             "{\n" +
             "    gl_Position = position;\n" +
             "    textureCoordinate = inputTextureCoordinate.xy;\n" +
             "    textureCoordinate2 = inputTextureCoordinate2.xy;\n" +
             "}";
 
     public int filterSecondTextureCoordinateAttribute;
     public int filterInputTextureUniform2;
     public int filterSourceTexture2 = OpenGlUtils.NO_TEXTURE;
     private ByteBuffer mTexture2CoordinatesBuffer;
 
     public GPUImageTwoInputFilter(String fragmentShader) {
         this(VERTEX_SHADER, fragmentShader);
     }
 
     public GPUImageTwoInputFilter(String vertexShader, String fragmentShader) {
         super(vertexShader, fragmentShader);
         setRotation(ROTATION_NONE);
     }
 
     @Override
     public void onInit() {
         super.onInit();
 
         filterSecondTextureCoordinateAttribute = GLES20.glGetAttribLocation(getProgram(), "inputTextureCoordinate2");
         filterInputTextureUniform2 = GLES20.glGetUniformLocation(getProgram(), "inputImageTexture2"); // This does assume a name of "inputImageTexture2" for second input texture in the fragment shader
         GLES20.glEnableVertexAttribArray(filterSecondTextureCoordinateAttribute);
     }
 
    public void setBitmap(final Bitmap bm) {
         runOnDraw(new Runnable() {
             public void run() {
                 if (filterSourceTexture2 == OpenGlUtils.NO_TEXTURE) {
                     GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
                    filterSourceTexture2 = OpenGlUtils.loadTexture(bm, OpenGlUtils.NO_TEXTURE);
                 }
             }
         });
     }
 
     public void onDestroy() {
         super.onDestroy();
     }
 
     @Override
     protected void onDrawArraysPre() {
         GLES20.glEnableVertexAttribArray(filterSecondTextureCoordinateAttribute);
         GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
         GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, filterSourceTexture2);
         GLES20.glUniform1i(filterInputTextureUniform2, 3);
 
         mTexture2CoordinatesBuffer.position(0);
         GLES20.glVertexAttribPointer(filterSecondTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, mTexture2CoordinatesBuffer);
     }
 
     public static float[] noRotationTextureCoordinates = {
             0.0f, 0.0f,
             1.0f, 0.0f,
             0.0f, 1.0f,
             1.0f, 1.0f,
     };
 
     static float[] rotateLeftTextureCoordinates = {
             1.0f, 0.0f,
             1.0f, 1.0f,
             0.0f, 0.0f,
             0.0f, 1.0f,
     };
 
     static float[] rotateRightTextureCoordinates = {
             0.0f, 1.0f,
             0.0f, 0.0f,
             1.0f, 1.0f,
             1.0f, 0.0f,
     };
 
     static float[] verticalFlipTextureCoordinates = {
             0.0f, 1.0f,
             1.0f, 1.0f,
             0.0f, 0.0f,
             1.0f, 0.0f,
     };
 
     static float[] horizontalFlipTextureCoordinates = {
             1.0f, 0.0f,
             0.0f, 0.0f,
             1.0f, 1.0f,
             0.0f, 1.0f,
     };
 
     static float[] rotateRightVerticalFlipTextureCoordinates = {
             0.0f, 0.0f,
             0.0f, 1.0f,
             1.0f, 0.0f,
             1.0f, 1.0f,
     };
 
     static float[] rotate180TextureCoordinates = {
             1.0f, 1.0f,
             0.0f, 1.0f,
             1.0f, 0.0f,
             0.0f, 0.0f,
     };
 
     public static final int ROTATION_NONE = 1;
     public static final int ROTATION_LEFT = 2;
     public static final int ROTATION_RIGHT = 3;
     public static final int ROTATION_FLIP_VERTICAL = 4;
     public static final int ROTATION_FLIP_HORIZONTAL = 5;
     public static final int ROTATION_RIGHT_FLIP_VERTICAL = 6;
     public static final int ROTATION_180 = 7;
 
     public void setRotation(int rotationMode) {
         float[] buffer;
         switch (rotationMode) {
             case ROTATION_NONE:
                 buffer = noRotationTextureCoordinates;
                 break;
             case ROTATION_LEFT:
                 buffer = rotateLeftTextureCoordinates;
                 break;
             case ROTATION_RIGHT:
                 buffer = rotateRightTextureCoordinates;
                 break;
             case ROTATION_FLIP_VERTICAL:
                 buffer = verticalFlipTextureCoordinates;
                 break;
             case ROTATION_FLIP_HORIZONTAL:
                 buffer = horizontalFlipTextureCoordinates;
                 break;
             case ROTATION_RIGHT_FLIP_VERTICAL:
                 buffer = rotateRightVerticalFlipTextureCoordinates;
                 break;
             case ROTATION_180:
                 buffer = rotate180TextureCoordinates;
                 break;
             default:
                 buffer = noRotationTextureCoordinates;
                 break;
         }
 
         ByteBuffer bBuffer = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder());
         FloatBuffer fBuffer = bBuffer.asFloatBuffer();
         fBuffer.put(buffer);
         fBuffer.flip();
 
         mTexture2CoordinatesBuffer = bBuffer;
     }
 }
