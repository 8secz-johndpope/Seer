 /*
  * JavaCL - Java API and utilities for OpenCL
  * http://javacl.googlecode.com/
  *
  * Copyright (c) 2009-2010, Olivier Chafik (http://ochafik.free.fr/)
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions are met:
  * 
  *     * Redistributions of source code must retain the above copyright
  *       notice, this list of conditions and the following disclaimer.
  *     * Redistributions in binary form must reproduce the above copyright
  *       notice, this list of conditions and the following disclaimer in the
  *       documentation and/or other materials provided with the distribution.
  *     * Neither the name of Olivier Chafik nor the
  *       names of its contributors may be used to endorse or promote products
  *       derived from this software without specific prior written permission.
  * 
  * THIS SOFTWARE IS PROVIDED BY OLIVIER CHAFIK AND CONTRIBUTORS ``AS IS'' AND ANY
  * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
  * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
  * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
  * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
  * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
  * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  */
 package com.nativelibs4java.opencl;
 
 import static com.nativelibs4java.opencl.CLException.error;
 import static com.nativelibs4java.opencl.CLException.failedForLackOfMemory;
 import static com.nativelibs4java.opencl.JavaCL.CL;
 import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
 import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_event;
 import static com.nativelibs4java.util.ImageUtils.getImageIntPixels;
 import static com.nativelibs4java.util.JNAUtils.toNS;
 import static com.nativelibs4java.util.JNAUtils.toNSArray;
 import static com.nativelibs4java.util.NIOUtils.getSizeInBytes;
 
 import java.awt.Image;
 import java.io.IOException;
 import java.nio.Buffer;
 import java.nio.ByteBuffer;
 import java.nio.ByteOrder;
 import java.nio.CharBuffer;
 import java.nio.DoubleBuffer;
 import java.nio.FloatBuffer;
 import java.nio.IntBuffer;
 import java.nio.LongBuffer;
 import java.nio.ShortBuffer;
 import java.util.ArrayList;
 import java.util.List;
 
 import com.nativelibs4java.opencl.CLDevice.QueueProperties;
 import com.nativelibs4java.opencl.CLSampler.AddressingMode;
 import com.nativelibs4java.opencl.CLSampler.FilterMode;
 import com.nativelibs4java.opencl.library.OpenGLContextUtils;
 import com.nativelibs4java.opencl.library.cl_image_format;
 import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_context;
 import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_device_id;
 import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_mem;
 import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_sampler;
 import com.nativelibs4java.util.EnumValue;
 import com.nativelibs4java.util.EnumValues;
 import com.nativelibs4java.util.NIOUtils;
 import com.nativelibs4java.util.ValuedEnum;
 import com.ochafik.lang.jnaerator.runtime.NativeSize;
 import com.ochafik.lang.jnaerator.runtime.NativeSizeByReference;
 import com.sun.jna.Memory;
 import com.sun.jna.Native;
 import com.sun.jna.Platform;
 import com.sun.jna.Pointer;
 import com.sun.jna.ptr.IntByReference;
 import java.io.InputStream;
 import java.util.Arrays;
 import java.util.Map;
 
 /**
  * OpenCL context.<br/>
  * An OpenCL context is created with one or more devices.<br/>
  * Contexts are used by the OpenCL runtime for managing objects such as command-queues, memory, program and kernel objects and for executing kernels on one or more devices specified in the context.
  * @author Olivier Chafik
  */
 public class CLContext extends CLAbstractEntity<cl_context> {
 
 	private static CLInfoGetter<cl_context> infos = new CLInfoGetter<cl_context>() {
 
 		@Override
 		protected int getInfo(cl_context entity, int infoTypeEnum, NativeSize size, Pointer out, NativeSizeByReference sizeOut) {
 			return CL.clGetContextInfo(entity, infoTypeEnum, size, out, sizeOut);
 		}
 	};
 	CLPlatform platform;
 	protected cl_device_id[] deviceIds;
 
 	CLContext(CLPlatform platform, cl_device_id[] deviceIds, cl_context context) {
 		super(context);
 		this.platform = platform;
 		this.deviceIds = deviceIds;
 	}
 	
 	/**
 	 * Creates a user event object. <br/>
 	 * User events allow applications to enqueue commands that wait on a user event to finish before the command is executed by the device.
 	 * @since OpenCL 1.1
 	 */
 	public CLEvent createUserEvent() {
 		try {
 			IntByReference pErr = new IntByReference();
 			cl_event evt = CL.clCreateUserEvent(getEntity(), pErr);
 			error(pErr.getValue());
			return CLEvent.createEvent(null, evt);
 		} catch (Throwable th) {
 			// TODO throw if supposed to handle OpenCL 1.1
     		return null;
 		}
 	}
 
 	/**
 	 * Create an OpenCL queue on the first device of this context.<br/>
 	 * Equivalent to calling <code>getDevices()[0].createQueue(context)</code>
 	 * @return new OpenCL queue
 	 */
 	public CLQueue createDefaultQueue(QueueProperties... queueProperties) {
 		return new CLDevice(platform, deviceIds[0]).createQueue(this, queueProperties);
 	}
 
 	/**
 	 * Create an out-of-order OpenCL queue on the first device of this context.<br/>
 	 * Equivalent to calling <code>getDevices()[0].createOutOfOrderQueue(context)</code>
 	 * @return new out-of-order OpenCL queue
 	 */
 	public CLQueue createDefaultOutOfOrderQueue() {
 		return new CLDevice(platform, deviceIds[0]).createOutOfOrderQueue(this);
 	}
 
 	/**
 	 * Create an profiling-enabled OpenCL queue on the first device of this context.<br/>
 	 * Equivalent to calling <code>getDevices()[0].createProfilingQueue(context)</code>
 	 * @return new profiling-enabled OpenCL queue
 	 */
 	public CLQueue createDefaultProfilingQueue() {
 		return new CLDevice(platform, deviceIds[0]).createProfilingQueue(this);
 	}
 
 	@SuppressWarnings("deprecation")
 	public CLImageFormat[] getSupportedImageFormats(CLBuffer.Flags flags, CLBuffer.ObjectType imageType) {
 		IntByReference pCount = new IntByReference();
 		int memFlags = (int) flags.value();
 		int imTyp = (int) imageType.value();
 		Memory memCount = new Memory(16);
 		pCount.setPointer(memCount);
 		CL.clGetSupportedImageFormats(getEntity(), memFlags, imTyp, 0, null, pCount);
 		cl_image_format ft = new cl_image_format();
 		int sz = ft.size();
 		int n = pCount.getValue();
 		if (n == 0) {
 			n = 30; // There HAS to be at least one format. the spec even says even more, but in fact on Mac OS X / CPU there's only one...
 		}
 		Memory mem = new Memory(n * sz);
 		ft.use(mem);
 		CL.clGetSupportedImageFormats(getEntity(), memFlags, imTyp, n, ft, (IntByReference) null);
 		List<CLImageFormat> ret = new ArrayList<CLImageFormat>(n);
 		for (int i = 0; i < n; i++) {
 			ft.use(mem, i * sz);
 			ft.read();
 			if (ft.image_channel_data_type == 0 && ft.image_channel_order == 0)
 				break;
 
 			ret.add(new CLImageFormat(ft));
 		}
 		return ret.toArray(new CLImageFormat[ret.size()]);
 	}
 
 	@SuppressWarnings("deprecation")
 	public CLSampler createSampler(boolean normalized_coords, AddressingMode addressing_mode, FilterMode filter_mode) {
 		IntByReference pErr = new IntByReference();
 		cl_sampler sampler = CL.clCreateSampler(getEntity(), normalized_coords ? CL_TRUE : CL_FALSE, (int) addressing_mode.value(), (int) filter_mode.value(), pErr);
 		error(pErr.getValue());
 		return new CLSampler(sampler);
 	}
 
 	public int getDeviceCount() {
 		return infos.getOptionalFeatureInt(getEntity(), CL.CL_CONTEXT_NUM_DEVICES);
 	}
 	
 	/**
 	 * Lists the devices of this context
 	 * @return array of the devices that form this context
 	 */
 	public synchronized CLDevice[] getDevices() {
 		if (deviceIds == null) {
 			Memory ptrs = infos.getMemory(getEntity(), CL_CONTEXT_DEVICES);
 			int n = (int) (ptrs.getSize() / Native.POINTER_SIZE);
 			deviceIds = new cl_device_id[n];
 			for (int i = 0; i < n; i++) {
 				deviceIds[i] = new cl_device_id(ptrs.getPointer(i * Native.POINTER_SIZE));
 			}
 		}
 		CLDevice[] devices = new CLDevice[deviceIds.length];
 		for (int i = devices.length; i-- != 0;) {
 			devices[i] = new CLDevice(platform, deviceIds[i]);
 		}
 		return devices;
 	}
 
 	/**
 	 * Create a program with all the C source code content provided as argument.
 	 * @param srcs list of the content of source code for the program
 	 * @return a program that needs to be built
 	 */
 	public CLProgram createProgram(String... srcs) {
         return createProgram(null, srcs);
 	}
 
     public CLProgram createProgram(CLDevice[] devices, String... srcs) {
         CLProgram program = new CLProgram(this, devices);
         for (String src : srcs)
             program.addSource(src);
         return program;
     }
 
     /**
      * Restore a program previously saved with {@link CLProgram#store(java.io.OutputStream) }
      * @param in will be closed
      * @return
      * @throws IOException
      */
     public CLProgram loadProgram(InputStream in) throws IOException {
         Map<CLDevice, byte[]> binaries = CLProgram.readBinaries(Arrays.asList(getDevices()), in);
         return createProgram(binaries);
     }
 
 	public CLProgram createProgram(Map<CLDevice, byte[]> binaries) {
 		return new CLProgram(this, binaries);
 	}
 
 	//cl_queue queue;
 	@Override
 	protected void clear() {
 		error(CL.clReleaseContext(getEntity()));
 	}
 
 
 
     @Deprecated
     public CLDevice guessCurrentGLDevice() {
         long[] props = CLPlatform.getContextProps(CLPlatform.getGLContextProperties(getPlatform()));
         Memory propsMem = toNSArray(props);
         NativeSizeByReference propsRef = new NativeSizeByReference();
         propsRef.setPointer(propsMem);
 
         NativeSizeByReference pCount = new NativeSizeByReference();
         Memory mem = new Memory(Pointer.SIZE);
         if (Platform.isMac())
         	error(CL.clGetGLContextInfoAPPLE(getEntity(), OpenGLContextUtils.INSTANCE.CGLGetCurrentContext(), CL_CURRENT_DEVICE_FOR_GL_CONTEXT_KHR, toNS(Pointer.SIZE), mem, pCount));
         else
             error(CL.clGetGLContextInfoKHR(propsRef, CL_CURRENT_DEVICE_FOR_GL_CONTEXT_KHR, toNS(Pointer.SIZE), mem, pCount));
 
         if (pCount.getValue().intValue() != Pointer.SIZE)
             throw new RuntimeException("Not a device : len = " + pCount.getValue().intValue());
 
         Pointer p = mem.getPointer(0);
         if (p.equals(Pointer.NULL))
             return null;
         return new CLDevice(null, new cl_device_id(p));
     }
 
     private static <T extends CLMem> T markAsGL(T mem) {
         mem.isGL = true;
         return mem;
     }
 
     /**
      * Makes an OpenGL Vertex Buffer Object (VBO) visible to OpenCL as a buffer object.<br/>
      * Note that memory objects shared with OpenGL must be acquired / released before / after use from OpenCL.
      * @see CLMem#acquireGLObject(com.nativelibs4java.opencl.CLQueue, com.nativelibs4java.opencl.CLEvent[])
      * @see CLMem#releaseGLObject(com.nativelibs4java.opencl.CLQueue, com.nativelibs4java.opencl.CLEvent[]) 
      * @param openGLBufferObject Identifier of a VBO, as generated by glGenBuffers
      */
 	@SuppressWarnings("deprecation")
 	public CLByteBuffer createBufferFromGLBuffer(CLMem.Usage usage, int openGLBufferObject) {
 		IntByReference pErr = new IntByReference();
 		cl_mem mem;
 		int previousAttempts = 0;
 		do {
 			mem = CL.clCreateFromGLBuffer(getEntity(), usage.getIntFlags(), openGLBufferObject, pErr);
 		} while (failedForLackOfMemory(pErr.getValue(), previousAttempts++));
         return markAsGL(new CLByteBuffer(this, -1, mem, null));
 	}
 
     /**
      * Makes an OpenGL Render Buffer visible to OpenCL as a 2D image.<br/>
      * Note that memory objects shared with OpenGL must be acquired / released before / after use from OpenCL.
      * @see CLMem#acquireGLObject(com.nativelibs4java.opencl.CLQueue, com.nativelibs4java.opencl.CLEvent[])
      * @see CLMem#releaseGLObject(com.nativelibs4java.opencl.CLQueue, com.nativelibs4java.opencl.CLEvent[])
      * @param openGLBufferObject Identifier of an OpenGL render buffer
      */
 	@SuppressWarnings("deprecation")
 	public CLImage2D createImage2DFromGLRenderBuffer(CLMem.Usage usage, int openGLRenderBuffer) {
 		IntByReference pErr = new IntByReference();
 		cl_mem mem;
 		int previousAttempts = 0;
 		do {
 			mem = CL.clCreateFromGLRenderbuffer(getEntity(), usage.getIntFlags(), openGLRenderBuffer, pErr);
 		} while (failedForLackOfMemory(pErr.getValue(), previousAttempts++));
 		return markAsGL(new CLImage2D(this, mem, null));
 	}
 	
 	/**
 	 * Creates an OpenCL 2D image object from an OpenGL 2D texture object, or a single face of an OpenGL cubemap texture object.<br/>
 	 * Note that memory objects shared with OpenGL must be acquired / released before / after use from OpenCL.
      * @param usage
 	 * @param textureTarget Must be one of GL_TEXTURE_2D, GL_TEXTURE_CUBE_MAP_POSITIVE_X, GL_TEXTURE_CUBE_MAP_POSITIVE_Y, GL_TEXTURE_CUBE_MAP_POSITIVE_Z, GL_TEXTURE_CUBE_MAP_NEGATIVE_X, GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, or GL_TEXTURE_RECTANGLE47. texture_target is used only to define the image type of texture. No reference to a bound GL texture object is made or implied by this parameter.
 	 * @param mipLevel Mipmap level to be used (Implementations may return CL_INVALID_OPERATION for miplevel values > 0)
 	 * @param texture Name of a GL 2D, cubemap or rectangle texture object. The texture object must be a complete texture as per OpenGL rules on texture completeness. The texture format and dimensions defined by OpenGL for the specified miplevel of the texture will be used to create the 2D image object. Only GL texture objects with an internal format that maps to appropriate image channel order and data type specified in tables 5.4 and 5.5 may be used to create a 2D image object.
 	 * @return valid OpenCL image object if the image object is created successfully
 	 * @throws CLException.InvalidMipLevel if miplevel is less than the value of levelbase (for OpenGL implementations) or zero (for OpenGL ES implementations); or greater than the value of q (for both OpenGL and OpenGL ES). levelbase and q are defined for the texture in section 3.8.10 (Texture Completeness) of the OpenGL 2.1 specification and section 3.7.10 of the OpenGL ES 2.0, or if miplevel is greather than zero and the OpenGL implementation does not support creating from non-zero mipmap levels.
 	 * @throws CLException.InvalidGLObject if texture is not a GL texture object whose type matches texture_target, if the specified miplevel of texture is not defined, or if the width or height of the specified miplevel is zero.
 	 * @throws CLException.InvalidImageFormatDescriptor if the OpenGL texture internal format does not map to a supported OpenCL image format.
 	 */
 	@SuppressWarnings("deprecation")
 	public CLImage2D createImage2DFromGLTexture2D(CLMem.Usage usage, GLTextureTarget textureTarget, int texture, int mipLevel) {
 		IntByReference pErr = new IntByReference();
 		cl_mem mem;
 		int previousAttempts = 0;
 		do {
 			mem = CL.clCreateFromGLTexture2D(getEntity(), usage.getIntFlags(), (int)textureTarget.value(), mipLevel, texture, pErr);
 		} while (failedForLackOfMemory(pErr.getValue(), previousAttempts++));
 		return markAsGL(new CLImage2D(this, mem, null));
 	}
 
 	private static final int 
 		GL_TEXTURE_2D = 0x0DE1,
 		GL_TEXTURE_3D = 0x806F,
 		GL_TEXTURE_CUBE_MAP_POSITIVE_X = 0x8515,
 		GL_TEXTURE_CUBE_MAP_NEGATIVE_X = 0x8516,
 		GL_TEXTURE_CUBE_MAP_POSITIVE_Y = 0x8517,
 		GL_TEXTURE_CUBE_MAP_NEGATIVE_Y = 0x8518,
 		GL_TEXTURE_CUBE_MAP_POSITIVE_Z = 0x8519,
 		GL_TEXTURE_CUBE_MAP_NEGATIVE_Z = 0x851A,
 		GL_TEXTURE_RECTANGLE = 0x84F5;
 
 	public CLPlatform getPlatform() {
 		return platform;
 	}
 		
 
 	public enum GLTextureTarget implements ValuedEnum {
 		
 		Texture2D(GL_TEXTURE_2D),
 		//Texture3D(GL_TEXTURE_3D),
 		CubeMapPositiveX(GL_TEXTURE_CUBE_MAP_POSITIVE_X),
 		CubeMapNegativeX(GL_TEXTURE_CUBE_MAP_NEGATIVE_X),
 		CubeMapPositiveY(GL_TEXTURE_CUBE_MAP_POSITIVE_Y),
 		CubeMapNegativeY(GL_TEXTURE_CUBE_MAP_NEGATIVE_Y),
 		CubeMapPositiveZ(GL_TEXTURE_CUBE_MAP_POSITIVE_Z),
 		CubeMapNegativeZ(GL_TEXTURE_CUBE_MAP_NEGATIVE_Z),
 		Rectangle(GL_TEXTURE_RECTANGLE);
 
 		GLTextureTarget(long value) { this.value = value; }
 		long value;
 		@Override
 		public long value() { return value; }
 		public static GLTextureTarget getEnum(int v) { return EnumValues.getEnum(v, GLTextureTarget.class); }
 	}
 	
 	/**
 	 * Creates an OpenCL 3D image object from an OpenGL 3D texture object<br/>
 	 * Note that memory objects shared with OpenGL must be acquired / released before / after use from OpenCL.
 	 * @param usage
 	 * @param mipLevel Mipmap level to be used (Implementations may return CL_INVALID_OPERATION for miplevel values > 0)
 	 * @param texture Name of a GL 3D texture object. The texture object must be a complete texture as per OpenGL rules on texture completeness. The texture format and dimensions defined by OpenGL for the specified miplevel of the texture will be used to create the 3D image object. Only GL texture objects with an internal format that maps to appropriate image channel order and data type specified in tables 5.4 and 5.5 can be used to create the 3D image object.
 	 * @return valid OpenCL image object if the image object is created successfully
 	 * @throws CLException.InvalidMipLevel if miplevel is less than the value of levelbase (for OpenGL implementations) or zero (for OpenGL ES implementations); or greater than the value of q (for both OpenGL and OpenGL ES). levelbase and q are defined for the texture in section 3.8.10 (Texture Completeness) of the OpenGL 2.1 specification and section 3.7.10 of the OpenGL ES 2.0, or if miplevel is greather than zero and the OpenGL implementation does not support creating from non-zero mipmap levels.
 	 * @throws CLException.InvalidGLObject if texture is not a GL texture object whose type matches texture_target, if the specified miplevel of texture is not defined, or if the width or height of the specified miplevel is zero.
 	 * @throws CLException.InvalidImageFormatDescriptor if the OpenGL texture internal format does not map to a supported OpenCL image format.
 	 */
 	@SuppressWarnings("deprecation")
 	public CLImage3D createImage3DFromGLTexture3D(CLMem.Usage usage, int texture, int mipLevel) {
 		IntByReference pErr = new IntByReference();
 		cl_mem mem;
 		int previousAttempts = 0;
 		do {
 			mem = CL.clCreateFromGLTexture3D(getEntity(), usage.getIntFlags(), GL_TEXTURE_3D, mipLevel, texture, pErr);
 		} while (failedForLackOfMemory(pErr.getValue(), previousAttempts++));
 		return markAsGL(new CLImage3D(this, mem, null));
 	}
 	
 	/**
 	 * Create an ARGB input 2D image with the content provided
 	 * @param allowUnoptimizingDirectRead Some images expose their internal data for direct read, leading to performance increase during the creation of the OpenCL image. However, direct access to the image data disables some Java2D optimizations for this image, leading to degraded performance in subsequent uses with AWT/Swing.
 	 */
 	public CLImage2D createImage2D(CLMem.Usage usage, Image image, boolean allowUnoptimizingDirectRead) {
 		int width = image.getWidth(null), height = image.getHeight(null);
 		int[] data = getImageIntPixels(image, 0, 0, width, height, allowUnoptimizingDirectRead);
 		IntBuffer directData = NIOUtils.directInts(data.length, getKernelsDefaultByteOrder());
 		directData.put(IntBuffer.wrap(data));
 		directData.rewind();
 
 		return createImage2D(
 				usage,
 				new CLImageFormat(CLImageFormat.ChannelOrder.RGBA, CLImageFormat.ChannelDataType.UnsignedInt8),
 				width, height,
 				0,
 				directData,
 				true);
 	}
 
 	@SuppressWarnings("deprecation")
 	public CLImage2D createImage2D(CLMem.Usage usage, CLImageFormat format, long width, long height, long rowPitch, Buffer buffer, boolean copy) {
 		long memFlags = usage.getIntFlags();
 		if (buffer != null) {
 			memFlags |= copy ? CL_MEM_COPY_HOST_PTR : CL_MEM_USE_HOST_PTR;
 		}
 
 		IntByReference pErr = new IntByReference();
 		cl_mem mem;
 		int previousAttempts = 0;
 		do {
 			mem = CL.clCreateImage2D(
 				getEntity(),
 				memFlags,
 				format.to_cl_image_format(),
 				toNS(width),
 				toNS(height),
 				toNS(rowPitch),
 				buffer == null ? null : Native.getDirectBufferPointer(buffer),
 				pErr);
 		} while (failedForLackOfMemory(pErr.getValue(), previousAttempts++));
 		return new CLImage2D(this, mem, format);
 	}
 
 	public CLImage2D createImage2D(CLMem.Usage usage, CLImageFormat format, long width, long height, long rowPitch) {
 		return createImage2D(usage, format, width, height, rowPitch, null, false);
 	}
 
 	public CLImage2D createImage2D(CLMem.Usage usage, CLImageFormat format, long width, long height) {
 		return createImage2D(usage, format, width, height, 0, null, false);
 	}
 
 	@SuppressWarnings("deprecation")
 	public CLImage3D createImage3D(CLMem.Usage usage, CLImageFormat format, long width, long height, long depth, long rowPitch, long slicePitch, Buffer buffer, boolean copy) {
 		long memFlags = usage.getIntFlags();
 		if (buffer != null) {
 			memFlags |= copy ? CL_MEM_COPY_HOST_PTR : CL_MEM_USE_HOST_PTR;
 		}
 
 		IntByReference pErr = new IntByReference();
 		cl_mem mem;
 		int previousAttempts = 0;
 		do {
 			mem = CL.clCreateImage3D(
 				getEntity(),
 				memFlags,
 				format.to_cl_image_format(),
 				toNS(width),
 				toNS(height),
 				toNS(depth),
 				toNS(rowPitch),
 				toNS(slicePitch),
 				buffer == null ? null : Native.getDirectBufferPointer(buffer),
 				pErr);
 		} while (failedForLackOfMemory(pErr.getValue(), previousAttempts++));
 		
 		return new CLImage3D(this, mem, format);
 	}
 
 	public CLImage3D createImage3D(CLMem.Usage usage, CLImageFormat format, long width, long height, long depth, long rowPitch, long slicePitch) {
 		return createImage3D(usage, format, width, height, depth, rowPitch, slicePitch, null, false);
 	}
 
 	public CLImage3D createImage3D(CLMem.Usage usage, CLImageFormat format, long width, long height, long depth) {
 		return createImage3D(usage, format, width, height, depth, 0, 0, null, false);
 	}
 
 	public CLIntBuffer createIntBuffer(CLMem.Usage kind, IntBuffer buffer, boolean copy) {
 		return createByteBuffer(kind, buffer, copy).asCLIntBuffer();
 	}
 
 	public CLIntBuffer createIntBuffer(CLMem.Usage kind, long count) {
 		return createByteBuffer(kind, count * 4).asCLIntBuffer();
 	}
 
 	public CLLongBuffer createLongBuffer(CLMem.Usage kind, LongBuffer buffer, boolean copy) {
 		return createByteBuffer(kind, buffer, copy).asCLLongBuffer();
 	}
 
 	public CLLongBuffer createLongBuffer(CLMem.Usage kind, long count) {
 		return createByteBuffer(kind, count * 8).asCLLongBuffer();
 	}
 
 	public CLShortBuffer createShortBuffer(CLMem.Usage kind, ShortBuffer buffer, boolean copy) {
 		return createByteBuffer(kind, buffer, copy).asCLShortBuffer();
 	}
 
 	public CLShortBuffer createShortBuffer(CLMem.Usage kind, long count) {
 		return createByteBuffer(kind, count * 2).asCLShortBuffer();
 	}
 
 	public CLCharBuffer createCharBuffer(CLMem.Usage kind, CharBuffer buffer, boolean copy) {
 		return createByteBuffer(kind, buffer, copy).asCLCharBuffer();
 	}
 
 	public CLCharBuffer createCharBuffer(CLMem.Usage kind, long count) {
 		return createByteBuffer(kind, count * 2).asCLCharBuffer();
 	}
 
 	public CLFloatBuffer createFloatBuffer(CLMem.Usage kind, FloatBuffer buffer, boolean copy) {
 		return createByteBuffer(kind, buffer, copy).asCLFloatBuffer();
 	}
 
 	public CLFloatBuffer createFloatBuffer(CLMem.Usage kind, long count) {
 		return createByteBuffer(kind, count * 4).asCLFloatBuffer();
 	}
 
 	public CLDoubleBuffer createDoubleBuffer(CLMem.Usage kind, DoubleBuffer buffer, boolean copy) {
 		return createByteBuffer(kind, buffer, copy).asCLDoubleBuffer();
 	}
 
 	public CLDoubleBuffer createDoubleBuffer(CLMem.Usage kind, long count) {
 		return createByteBuffer(kind, count * 8).asCLDoubleBuffer();
 	}
 
 	public CLByteBuffer createByteBuffer(CLMem.Usage kind, long count) {
 		return createBuffer(null, count, kind.getIntFlags(), false);
 	}
 
     @SuppressWarnings("unchecked")
 	public <B extends Buffer> CLBuffer<B> createBuffer(CLMem.Usage kind, long count, Class<B> bufferClass) {
         if (IntBuffer.class.isAssignableFrom(bufferClass))
             return (CLBuffer<B>)createIntBuffer(kind, count);
 		if (LongBuffer.class.isAssignableFrom(bufferClass))
             return (CLBuffer<B>)createLongBuffer(kind, count);
 		if (ShortBuffer.class.isAssignableFrom(bufferClass))
             return (CLBuffer<B>)createShortBuffer(kind, count);
 		if (ByteBuffer.class.isAssignableFrom(bufferClass))
             return (CLBuffer<B>)createByteBuffer(kind, count);
 		if (CharBuffer.class.isAssignableFrom(bufferClass))
             return (CLBuffer<B>)createCharBuffer(kind, count);
 		if (DoubleBuffer.class.isAssignableFrom(bufferClass))
             return (CLBuffer<B>)createDoubleBuffer(kind, count);
 		if (FloatBuffer.class.isAssignableFrom(bufferClass))
             return (CLBuffer<B>)createFloatBuffer(kind, count);
 
         throw new UnsupportedOperationException("Cannot create OpenCL buffers of Java type " + bufferClass.getName());
 	}
 
     @SuppressWarnings("unchecked")
 	public <B extends Buffer> CLBuffer<B> createBuffer(CLMem.Usage kind, B buffer, boolean copy) {
         Class<?> bufferClass = buffer.getClass();
         if (IntBuffer.class.isAssignableFrom(bufferClass))
             return (CLBuffer<B>)createIntBuffer(kind, (IntBuffer)buffer, copy);
 		if (LongBuffer.class.isAssignableFrom(bufferClass))
             return (CLBuffer<B>)createLongBuffer(kind, (LongBuffer)buffer, copy);
 		if (ShortBuffer.class.isAssignableFrom(bufferClass))
             return (CLBuffer<B>)createShortBuffer(kind, (ShortBuffer)buffer, copy);
 		if (ByteBuffer.class.isAssignableFrom(bufferClass))
             return (CLBuffer<B>)createByteBuffer(kind, (ByteBuffer)buffer, copy);
 		if (CharBuffer.class.isAssignableFrom(bufferClass))
             return (CLBuffer<B>)createCharBuffer(kind, (CharBuffer)buffer, copy);
 		if (DoubleBuffer.class.isAssignableFrom(bufferClass))
             return (CLBuffer<B>)createDoubleBuffer(kind, (DoubleBuffer)buffer, copy);
 		if (FloatBuffer.class.isAssignableFrom(bufferClass))
             return (CLBuffer<B>)createFloatBuffer(kind, (FloatBuffer)buffer, copy);
 
         throw new UnsupportedOperationException("Cannot create OpenCL buffers of Java type " + bufferClass.getName());
 	}
 
     /**
      * @param kind
      * @param buffer input/output buffer
      * @param copy If false, the buffer must be direct and might be used directly as the primary storage of the buffer data by OpenCL, or might be cached. Calling map/unmap is then necessary to make sure the cache is consistent with the buffer value.
      * @return
      */
 	public CLByteBuffer createByteBuffer(CLMem.Usage kind, Buffer buffer, boolean copy) {
             if (!buffer.isDirect()) {
                 if (!copy)
                     throw new IllegalArgumentException("Cannot create an OpenCL buffer object out of a non-direct NIO buffer without copy.");
                 if (kind == CLMem.Usage.Output)
                     throw new IllegalArgumentException("Output NIO buffers must be direct.");
                 buffer = NIOUtils.directCopy(buffer, getKernelsDefaultByteOrder());
             }
             return createBuffer(buffer, -1, kind.getIntFlags() | (copy ? CL_MEM_COPY_HOST_PTR : CL_MEM_USE_HOST_PTR), copy);
 	}
 
 	@SuppressWarnings("deprecation")
 	private CLByteBuffer createBuffer(final Buffer buffer, long byteCount, final int CLBufferFlags, final boolean retainBufferReference) {
 		if (buffer != null) {
 			byteCount = getSizeInBytes(buffer);
 		} else if (retainBufferReference) {
 			throw new IllegalArgumentException("Cannot retain reference to null pointer !");
 		}
 
 		if (byteCount <= 0) {
 			throw new IllegalArgumentException("Buffer size must be greater than zero (asked for size " + byteCount + ")");
 		}
 
 		IntByReference pErr = new IntByReference();
 		//IntBuffer errBuff = IntBuffer.wrap(new int[1]);
 		cl_mem mem;
 		int previousAttempts = 0;
 		do {
 			mem = CL.clCreateBuffer(
 				getEntity(),
 				CLBufferFlags,
 				toNS(byteCount),
 				buffer == null ? null : Native.getDirectBufferPointer(buffer),
 				pErr);
 		} while (failedForLackOfMemory(pErr.getValue(), previousAttempts++));
 
 		return new CLByteBuffer(this, byteCount, mem, buffer);
 	}
 
     /**
      * The OpenCL specification states that the default endianness of kernel arguments is that of the device.<br/>
      * However, there are implementations where this does not appear to be respected, so it is compulsory to perform a runtime test on those platforms.
      * @return byte order needed for pointer kernel arguments that do not have any <code>__attribute__ ((endian(device))</code> or <code>__attribute__ ((endian(host))</code> attribute.
      */
     public ByteOrder getKernelsDefaultByteOrder() {
         ByteOrder order = null;
         for (CLDevice device : getDevices()) {
             ByteOrder devOrder = device.getKernelsDefaultByteOrder();
             if (order != null && devOrder != order)
                 return null;
             order = devOrder;
         }
         return order;
     }
 
     public ByteOrder getByteOrder() {
         ByteOrder order = null;
         for (CLDevice device : getDevices()) {
             ByteOrder devOrder = device.getByteOrder();
             if (order != null && devOrder != order)
                 return null;
             order = devOrder;
         }
         return order;
     }
 
     private volatile int addressBits = -2;
     
     /**
      * Return the number of bits used to represent a pointer on all of the context's devices, or -1 if not all devices use the same number of bits.<br>
      * Size of size_t type in OpenCL kernels can be obtained with getAddressBits() / 8.
      * @return -1 if the address bits of the context's devices do not match, common address bits otherwise
      */
     public int getAddressBits() {
         if (addressBits == -2) {
             synchronized (this) {
                 if (addressBits == -2) {
                     for (CLDevice device : getDevices()) {
                         int bits = device.getAddressBits();
                         if (addressBits != -2 && bits != addressBits) {
                             addressBits = -1;
                             break;
                         }
                         addressBits = bits;
                     }
                 }
             }
         }
         return addressBits;
     }
 
 	public boolean isDoubleSupported() {
 		for (CLDevice device : getDevices())
 			if (!device.isDoubleSupported())
 				return false;
 		return true;
 	}
 	public boolean isHalfSupported() {
 		for (CLDevice device : getDevices())
 			if (!device.isHalfSupported())
 				return false;
 		return true;
 	}
 	public boolean isByteAddressableStoreSupported() {
 		for (CLDevice device : getDevices())
 			if (!device.isByteAddressableStoreSupported())
 				return false;
 		return true;
 	}
 
 
 }
