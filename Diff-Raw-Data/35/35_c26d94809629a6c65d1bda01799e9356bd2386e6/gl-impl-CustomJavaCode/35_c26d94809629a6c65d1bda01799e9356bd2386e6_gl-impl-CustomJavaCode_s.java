 // Attempt to return the same ByteBuffer object from glMapBufferARB if
 // the vertex buffer object's base address and size haven't changed
 private static class ARBVBOKey {
   private long addr;
   private int  capacity;
 
   ARBVBOKey(long addr, int capacity) {
     this.addr = addr;
     this.capacity = capacity;
   }
 }
 
 private Map/*<ARBVBOKey, ByteBuffer>*/ arbVBOCache = new HashMap();
 
 /** Entry point to C language function: <br> <code> LPVOID glMapBufferARB(GLenum target, GLenum access); </code>    */
 public java.nio.ByteBuffer glMapBufferARB(int target, int access) {
   final long __addr_ = _context.getGLProcAddressTable()._addressof_glMapBufferARB;
   if (__addr_ == 0) {
     throw new GLException("Method \"glMapBufferARB\" not available");
   }
   int[] sz = new int[1];
   glGetBufferParameterivARB(target, GL_BUFFER_SIZE_ARB, sz);
   long addr;
   addr = dispatch_glMapBufferARB(target, access, __addr_);
   if (addr == 0 || sz[0] == 0) {
     return null;
   }
   ARBVBOKey key = new ARBVBOKey(addr, sz[0]);
   ByteBuffer _res = (ByteBuffer) arbVBOCache.get(key);
   if (_res == null) {
     _res = InternalBufferUtils.newDirectByteBuffer(addr, sz[0]);
     _res.order(ByteOrder.nativeOrder());
     arbVBOCache.put(key, _res);
   }
   return _res;
 }
 
 /** Encapsulates function pointer for OpenGL function <br>: <code> LPVOID glMapBufferARB(GLenum target, GLenum access); </code>    */
 native private long dispatch_glMapBufferARB(int target, int access, long glProcAddress);
