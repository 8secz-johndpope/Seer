 /*
  * Copyright (C) 2012 The Android Open Source Project
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package android.support.v8.renderscript;
 
 
 /**
  * Utility class for packing arguments and structures from Android system objects to
  * Renderscript objects.
  *
  **/
 public class FieldPacker {
     private static int thunk = 0;
 
     private android.renderscript.FieldPacker mN;
 
     private boolean shouldThunk() {
         if (thunk == 0) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                 thunk = 1;
             } else {
                 thunk = -1;
             }
         }
         if (thunk == 1) {
             return true;
         }
         return false;
     }
 
     public FieldPacker(int len) {
         mPos = 0;
         mLen = len;
         mData = new byte[len];
         if (shouldThunk()) {
             mN = new android.renderscript.FieldPacker(len);
         }
     }
 
     public void align(int v) {
         if (shouldThunk()) {
             mN.align(v);
             return;
         }
         if ((v <= 0) || ((v & (v - 1)) != 0)) {
             throw new RSIllegalArgumentException("argument must be a non-negative non-zero power of 2: " + v);
         }
 
         while ((mPos & (v - 1)) != 0) {
             mData[mPos++] = 0;
         }
     }
 
     public void reset() {
         if (shouldThunk()) {
             mN.reset();
             return;
         }
         mPos = 0;
     }
     public void reset(int i) {
         if (shouldThunk()) {
             mN.reset(i);
             return;
         }
         if ((i < 0) || (i >= mLen)) {
             throw new RSIllegalArgumentException("out of range argument: " + i);
         }
         mPos = i;
     }
 
     public void skip(int i) {
         if (shouldThunk()) {
             mN.skip(i);
             return;
         }
         int res = mPos + i;
         if ((res < 0) || (res > mLen)) {
             throw new RSIllegalArgumentException("out of range argument: " + i);
         }
         mPos = res;
     }
 
     public void addI8(byte v) {
         if (shouldThunk()) {
             mN.addI8(v);
             return;
         }
         mData[mPos++] = v;
     }
 
     public void addI16(short v) {
         if (shouldThunk()) {
             mN.addI16(v);
             return;
         }
         align(2);
         mData[mPos++] = (byte)(v & 0xff);
         mData[mPos++] = (byte)(v >> 8);
     }
 
     public void addI32(int v) {
         if (shouldThunk()) {
             mN.addI32(v);
             return;
         }
         align(4);
         mData[mPos++] = (byte)(v & 0xff);
         mData[mPos++] = (byte)((v >> 8) & 0xff);
         mData[mPos++] = (byte)((v >> 16) & 0xff);
         mData[mPos++] = (byte)((v >> 24) & 0xff);
     }
 
     public void addI64(long v) {
         if (shouldThunk()) {
             mN.addI64(v);
             return;
         }
         align(8);
         mData[mPos++] = (byte)(v & 0xff);
         mData[mPos++] = (byte)((v >> 8) & 0xff);
         mData[mPos++] = (byte)((v >> 16) & 0xff);
         mData[mPos++] = (byte)((v >> 24) & 0xff);
         mData[mPos++] = (byte)((v >> 32) & 0xff);
         mData[mPos++] = (byte)((v >> 40) & 0xff);
         mData[mPos++] = (byte)((v >> 48) & 0xff);
         mData[mPos++] = (byte)((v >> 56) & 0xff);
     }
 
     public void addU8(short v) {
         if (shouldThunk()) {
             mN.addU8(v);
             return;
         }
         if ((v < 0) || (v > 0xff)) {
             throw new IllegalArgumentException("Saving value out of range for type");
         }
         mData[mPos++] = (byte)v;
     }
 
     public void addU16(int v) {
         if (shouldThunk()) {
             mN.addU16(v);
             return;
         }
         if ((v < 0) || (v > 0xffff)) {
             android.util.Log.e("rs", "FieldPacker.addU16( " + v + " )");
             throw new IllegalArgumentException("Saving value out of range for type");
         }
         align(2);
         mData[mPos++] = (byte)(v & 0xff);
         mData[mPos++] = (byte)(v >> 8);
     }
 
     public void addU32(long v) {
         if (shouldThunk()) {
             mN.addU32(v);
             return;
         }
         if ((v < 0) || (v > 0xffffffffL)) {
             android.util.Log.e("rs", "FieldPacker.addU32( " + v + " )");
             throw new IllegalArgumentException("Saving value out of range for type");
         }
         align(4);
         mData[mPos++] = (byte)(v & 0xff);
         mData[mPos++] = (byte)((v >> 8) & 0xff);
         mData[mPos++] = (byte)((v >> 16) & 0xff);
         mData[mPos++] = (byte)((v >> 24) & 0xff);
     }
 
     public void addU64(long v) {
         if (shouldThunk()) {
             mN.addU64(v);
             return;
         }
         if (v < 0) {
             android.util.Log.e("rs", "FieldPacker.addU64( " + v + " )");
             throw new IllegalArgumentException("Saving value out of range for type");
         }
         align(8);
         mData[mPos++] = (byte)(v & 0xff);
         mData[mPos++] = (byte)((v >> 8) & 0xff);
         mData[mPos++] = (byte)((v >> 16) & 0xff);
         mData[mPos++] = (byte)((v >> 24) & 0xff);
         mData[mPos++] = (byte)((v >> 32) & 0xff);
         mData[mPos++] = (byte)((v >> 40) & 0xff);
         mData[mPos++] = (byte)((v >> 48) & 0xff);
         mData[mPos++] = (byte)((v >> 56) & 0xff);
     }
 
     public void addF32(float v) {
         if (shouldThunk()) {
             mN.addF32(v);
             return;
         }
         addI32(Float.floatToRawIntBits(v));
     }
 
     public void addF64(double v) {
         if (shouldThunk()) {
             mN.addF64(v);
             return;
         }
         addI64(Double.doubleToRawLongBits(v));
     }
 
     public void addObj(BaseObj obj) {
         if (shouldThunk()) {
             mN.addObj(obj.getNObj());
             return;
         }
         if (obj != null) {
             addI32(obj.getID(null));
         } else {
             addI32(0);
         }
     }
 
     public void addF32(Float2 v) {
         if (shouldThunk()) {
             mN.addF32(new android.renderscript.Float2(v.x, v.y));
             return;
         }
         addF32(v.x);
         addF32(v.y);
     }
     public void addF32(Float3 v) {
         if (shouldThunk()) {
             mN.addF32(new android.renderscript.Float3(v.x, v.y, v.z));
             return;
         }
         addF32(v.x);
         addF32(v.y);
         addF32(v.z);
     }
     public void addF32(Float4 v) {
         if (shouldThunk()) {
             mN.addF32(new android.renderscript.Float4(v.x, v.y, v.z, v.w));
             return;
         }
         addF32(v.x);
         addF32(v.y);
         addF32(v.z);
         addF32(v.w);
     }
 
     public void addF64(Double2 v) {
         if (shouldThunk()) {
             mN.addF64(new android.renderscript.Double2(v.x, v.y));
             return;
         }
         addF64(v.x);
         addF64(v.y);
     }
     public void addF64(Double3 v) {
         if (shouldThunk()) {
             mN.addF64(new android.renderscript.Double3(v.x, v.y, v.z));
             return;
         }
         addF64(v.x);
         addF64(v.y);
         addF64(v.z);
     }
     public void addF64(Double4 v) {
         if (shouldThunk()) {
             mN.addF64(new android.renderscript.Double4(v.x, v.y, v.z, v.w));
             return;
         }
         addF64(v.x);
         addF64(v.y);
         addF64(v.z);
         addF64(v.w);
     }
 
     public void addI8(Byte2 v) {
         if (shouldThunk()) {
             mN.addI8(new android.renderscript.Byte2(v.x, v.y));
             return;
         }
         addI8(v.x);
         addI8(v.y);
     }
     public void addI8(Byte3 v) {
         if (shouldThunk()) {
             mN.addI8(new android.renderscript.Byte3(v.x, v.y, v.z));
             return;
         }
         addI8(v.x);
         addI8(v.y);
         addI8(v.z);
     }
     public void addI8(Byte4 v) {
         if (shouldThunk()) {
             mN.addI8(new android.renderscript.Byte4(v.x, v.y, v.z, v.w));
             return;
         }
         addI8(v.x);
         addI8(v.y);
         addI8(v.z);
         addI8(v.w);
     }
 
     public void addU8(Short2 v) {
         if (shouldThunk()) {
             mN.addU8(new android.renderscript.Short2(v.x, v.y));
             return;
         }
         addU8(v.x);
         addU8(v.y);
     }
     public void addU8(Short3 v) {
         if (shouldThunk()) {
             mN.addU8(new android.renderscript.Short3(v.x, v.y, v.z));
             return;
         }
         addU8(v.x);
         addU8(v.y);
         addU8(v.z);
     }
     public void addU8(Short4 v) {
         if (shouldThunk()) {
             mN.addU8(new android.renderscript.Short4(v.x, v.y, v.z, v.w));
             return;
         }
         addU8(v.x);
         addU8(v.y);
         addU8(v.z);
         addU8(v.w);
     }
 
     public void addI16(Short2 v) {
         if (shouldThunk()) {
             mN.addI16(new android.renderscript.Short2(v.x, v.y));
             return;
         }
         addI16(v.x);
         addI16(v.y);
     }
     public void addI16(Short3 v) {
         if (shouldThunk()) {
             mN.addI16(new android.renderscript.Short3(v.x, v.y, v.z));
             return;
         }
         addI16(v.x);
         addI16(v.y);
         addI16(v.z);
     }
     public void addI16(Short4 v) {
         if (shouldThunk()) {
             mN.addI16(new android.renderscript.Short4(v.x, v.y, v.z, v.w));
             return;
         }
         addI16(v.x);
         addI16(v.y);
         addI16(v.z);
         addI16(v.w);
     }
 
     public void addU16(Int2 v) {
         if (shouldThunk()) {
             mN.addU16(new android.renderscript.Int2(v.x, v.y));
             return;
         }
         addU16(v.x);
         addU16(v.y);
     }
     public void addU16(Int3 v) {
         if (shouldThunk()) {
             mN.addU16(new android.renderscript.Int3(v.x, v.y, v.z));
             return;
         }
         addU16(v.x);
         addU16(v.y);
         addU16(v.z);
     }
     public void addU16(Int4 v) {
         if (shouldThunk()) {
             mN.addU16(new android.renderscript.Int4(v.x, v.y, v.z, v.w));
             return;
         }
         addU16(v.x);
         addU16(v.y);
         addU16(v.z);
         addU16(v.w);
     }
 
     public void addI32(Int2 v) {
         if (shouldThunk()) {
             mN.addI32(new android.renderscript.Int2(v.x, v.y));
             return;
         }
         addI32(v.x);
         addI32(v.y);
     }
     public void addI32(Int3 v) {
         if (shouldThunk()) {
             mN.addI32(new android.renderscript.Int3(v.x, v.y, v.z));
             return;
         }
         addI32(v.x);
         addI32(v.y);
         addI32(v.z);
     }
     public void addI32(Int4 v) {
         if (shouldThunk()) {
             mN.addI32(new android.renderscript.Int4(v.x, v.y, v.z, v.w));
             return;
         }
         addI32(v.x);
         addI32(v.y);
         addI32(v.z);
         addI32(v.w);
     }
 
     public void addU32(Long2 v) {
         if (shouldThunk()) {
             mN.addU32(new android.renderscript.Long2(v.x, v.y));
             return;
         }
         addU32(v.x);
         addU32(v.y);
     }
     public void addU32(Long3 v) {
         if (shouldThunk()) {
             mN.addU32(new android.renderscript.Long3(v.x, v.y, v.z));
             return;
         }
         addU32(v.x);
         addU32(v.y);
         addU32(v.z);
     }
     public void addU32(Long4 v) {
         if (shouldThunk()) {
             mN.addU32(new android.renderscript.Long4(v.x, v.y, v.z, v.w));
             return;
         }
         addU32(v.x);
         addU32(v.y);
         addU32(v.z);
         addU32(v.w);
     }
 
     public void addI64(Long2 v) {
         if (shouldThunk()) {
             mN.addI64(new android.renderscript.Long2(v.x, v.y));
             return;
         }
         addI64(v.x);
         addI64(v.y);
     }
     public void addI64(Long3 v) {
         if (shouldThunk()) {
             mN.addI64(new android.renderscript.Long3(v.x, v.y, v.z));
             return;
         }
         addI64(v.x);
         addI64(v.y);
         addI64(v.z);
     }
     public void addI64(Long4 v) {
         if (shouldThunk()) {
             mN.addI64(new android.renderscript.Long4(v.x, v.y, v.z, v.w));
             return;
         }
         addI64(v.x);
         addI64(v.y);
         addI64(v.z);
         addI64(v.w);
     }
 
     public void addU64(Long2 v) {
         if (shouldThunk()) {
             mN.addU64(new android.renderscript.Long2(v.x, v.y));
             return;
         }
         addU64(v.x);
         addU64(v.y);
     }
     public void addU64(Long3 v) {
         if (shouldThunk()) {
             mN.addU64(new android.renderscript.Long3(v.x, v.y, v.z));
             return;
         }
         addU64(v.x);
         addU64(v.y);
         addU64(v.z);
     }
     public void addU64(Long4 v) {
         if (shouldThunk()) {
             mN.addU64(new android.renderscript.Long4(v.x, v.y, v.z, v.w));
             return;
         }
         addU64(v.x);
         addU64(v.y);
         addU64(v.z);
         addU64(v.w);
     }
 
     public void addMatrix(Matrix4f v) {
         if (shouldThunk()) {
             mN.addMatrix(new android.renderscript.Matrix4f(v.getArray()));
             return;
         }
         for (int i=0; i < v.mMat.length; i++) {
             addF32(v.mMat[i]);
         }
     }
 
     public void addMatrix(Matrix3f v) {
         if (shouldThunk()) {
             mN.addMatrix(new android.renderscript.Matrix3f(v.getArray()));
             return;
         }
         for (int i=0; i < v.mMat.length; i++) {
             addF32(v.mMat[i]);
         }
     }
 
     public void addMatrix(Matrix2f v) {
         if (shouldThunk()) {
             mN.addMatrix(new android.renderscript.Matrix2f(v.getArray()));
             return;
         }
         for (int i=0; i < v.mMat.length; i++) {
             addF32(v.mMat[i]);
         }
     }
 
     public void addBoolean(boolean v) {
         if (shouldThunk()) {
             mN.addBoolean(v);
             return;
         }
         addI8((byte)(v ? 1 : 0));
     }
 
     public final byte[] getData() {
         if (shouldThunk()) {
             return mN.getData();
         }
         return mData;
     }
 
     private final byte mData[];
     private int mPos;
     private int mLen;
 
 }
 
 
