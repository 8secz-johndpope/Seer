 /*
  * This work is licensed under a Creative Commons Attribution-NonCommercial 3.0 Unported License:
  * http://creativecommons.org/licenses/by-nc/3.0/
  * For alternative conditions contact the author.
  *
  * Copyright (c) 2010 "Robin Wenglewski <robin@wenglewski.de>"
  */
 package com.freshbourne.io;
 
 import com.google.inject.Inject;
 import com.google.inject.Singleton;
 import com.google.inject.assistedinject.Assisted;
 import com.google.inject.assistedinject.AssistedInject;
 import com.google.inject.name.Named;
 
 import java.io.File;
 import java.io.IOException;
 import java.io.RandomAccessFile;
 import java.nio.ByteBuffer;
 import java.nio.channels.FileChannel;
 import java.nio.channels.FileLock;
 import java.nio.channels.OverlappingFileLockException;
 import java.util.Map;
 import java.util.Random;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 
 
 @Singleton
 public class FileResourceManager implements ResourceManager {
 	private RandomAccessFile handle;
 	private final File file;
 	private final int pageSize;
 	private FileLock fileLock;
 	private FileChannel ioChannel;
 	private ResourceHeader header;
 	private Map<Integer, RawPage> cache;
 	private boolean doLock;
 	
 	private static Log LOG = LogFactory.getLog(FileResourceManager.class);
 	
 	
     @Inject
 	FileResourceManager(@Assisted File f, @PageSize int pageSize, @Named("doLock") boolean doLock){
 		this.file = f;
 		this.pageSize = pageSize;
 		this.doLock = doLock;
 	}
 	
 	/* (non-Javadoc)
 	 * @see com.freshbourne.io.ResourceManager#open()
 	 */
 	@Override
 	public void open() throws IOException {
 		if(isOpen())
 			throw new IllegalStateException("Resource already open");
 		
 		// if the file does not exist already
 		if(!getFile().exists()){
 			getFile().createNewFile();
 		}
 		
 		initIOChannel(getFile());
 		this.header = new ResourceHeader(ioChannel, pageSize);
 		
 		
 		if(handle.length() == 0){
 			header.initialize();
 		} else {
 			// load header if file existed
 			header.load();
 		}
 		
 		this.cache = new SoftReferenceCacheMap<Integer, RawPage>();
 	}
 	
 	@Override
 	public void writePage(RawPage page) {
 		LOG.debug("writing page to disk: "+ page.id());
 		ensureOpen();
         ensurePageExists(page.id());
         
         ByteBuffer buffer = page.bufferForReading(0);
 
 		try{
 			long offset = header.getPageOffset(page.id());
 			ioChannel.write(buffer, offset);
 		} catch(IOException e){
 			throw new RuntimeException(e);
 		}
 		LOG.debug("page written");
 	}
 
 	/* (non-Javadoc)
 	 * @see com.freshbourne.io.PageManager#getPage(long)
 	 */
 	@Override
 	public RawPage getPage(int pageId) {
 		
 		ensureOpen();
 		ensurePageExists(pageId);
 		
 		RawPage result;
 		
 		if(cache.containsKey(pageId)){
 			result = cache.get(pageId);
 			if(result != null)
 				return result;
 		}
 
 		ByteBuffer buf = ByteBuffer.allocate(pageSize);
 		
 		try{
 			ioChannel.read(buf, header.getPageOffset(pageId));
 		} catch(IOException e){
 			e.printStackTrace();
 			System.exit(1);
 		}
 		
 		result = new RawPage(buf, pageId, this);
 		cache.put(pageId, result);
 		
 		return result;
 	}
 
 	/**
 	 * @param pageId
 	 * @throws PageNotFoundException 
 	 */
 	private void ensurePageExists(int pageId) {
 		if(!header.contains(pageId))
             throw new PageNotFoundException(this, pageId);
     }
 
 	/* (non-Javadoc)
 	 * @see com.freshbourne.io.ResourceManager#close()
 	 */
 	@Override
 	public void close() throws IOException {
 		if(header != null){
 			for(RawPage r : cache.values()){
 				r.sync();
 			}
 			
 			header = null;
 		}
 		
 		try{
 			if (fileLock != null && fileLock.isValid()) {
 				fileLock.release();
 				fileLock = null;
 			}
 			
 			if(ioChannel != null){
 				ioChannel.close();
 				ioChannel = null;
 			}
 
 			if (handle != null) {
 				handle.close();
 				handle = null;
 			}
 		} catch (Exception ignored) {
 		}
 	}
 
 	/* (non-Javadoc)
 	 * @see com.freshbourne.io.ResourceManager#getPageSize()
 	 */
 	@Override
 	public int pageSize() {
 		return pageSize;
 	}
 
 	/**
 	 * Generic private initializer that takes the random access file and initializes
 	 * the I/O channel and locks it for exclusive use by this instance.
 	 * 
 	 * from minidb
 	 * 
 	 * @param file The random access file representing the index.
 	 * @throws IOException Thrown, when the I/O channel could not be opened.
 	 */
 	private void initIOChannel(File file)
 	throws IOException {
 		handle = new RandomAccessFile(file, "rw");
 		
 		// Open the channel. If anything fails, make sure we close it again
 		try {
 			ioChannel = handle.getChannel();
 			try {
 				if(doLock)
 					fileLock = ioChannel.tryLock();
 			}
 			catch (OverlappingFileLockException oflex) {
 				throw new IOException("Index file locked by other consumer.");
 			}
 		}
 		catch (Throwable t) {
 			// something failed.
 			close();
 			
 			// propagate the exception
 			if (t instanceof IOException) {
 				throw (IOException) t;
 			}
 			else {
 				throw new IOException("An error occured while opening the index: " + t.getMessage());
 			}
 		}
 	}
 	
 	@Override
 	public boolean isOpen() {
 		return !(ioChannel == null || !ioChannel.isOpen());
 	}
 	
 	/*
 	 * (non-Javadoc)
 	 * @see java.lang.Object#toString()
 	 */
 	@Override
 	public String toString(){
 		return "Resource: " + getFile().getAbsolutePath();
 	}
 
 	/* (non-Javadoc)
 	 * @see com.freshbourne.io.ResourceManager#addPage(com.freshbourne.io.HashPage)
 	 */
 	@Override
 	public RawPage addPage(RawPage page) {
 		ensureOpen();
 		ensureCorrectPageSize(page);
 		
 		RawPage result = new RawPage(page.bufferForWriting(0), header.generateId(), this);
 		
 		try {
 			ioChannel.write(page.bufferForReading(0), ioChannel.size());
 		} catch (DuplicatePageIdException e) {
 			e.printStackTrace();
 			System.exit(1);
 		} catch (IOException e) {
 			e.printStackTrace();
 			System.exit(1);
 		}
 		
 		cache.put(result.id(), result);
 		return result;
 	}
 	
 	/**
 	 * @param page
 	 * @throws WrongPageSizeException 
 	 */
 	private void ensureCorrectPageSize(RawPage page) {
 		if(page.bufferForReading(0).limit() != pageSize)
 			throw new WrongPageSizeException(page, pageSize);
 	}
 
 	private void ensureOpen() {
 		if(!isOpen())
 			throw new IllegalStateException("Resource is not open: " + toString());
 	}
 
 	/* (non-Javadoc)
 	 * @see com.freshbourne.io.ResourceManager#numberOfPages()
 	 */
 	@Override
 	public int numberOfPages() {
 		return header.getNumberOfPages();
 	}
 
 	/* (non-Javadoc)
 	 * @see com.freshbourne.io.ResourceManager#createPage()
 	 */
 	@Override
 	public RawPage createPage() {
 		ensureOpen();
 		
 		ByteBuffer buf = ByteBuffer.allocate(pageSize);
 		RawPage result = new RawPage(buf, header.generateId(), this);
 		
 		cache.put(result.id(), result);
 		return result;
 	}
 
 	/* (non-Javadoc)
 	 * @see com.freshbourne.io.ResourceManager#removePage(long)
 	 */
 	@Override
 	public void removePage(int pageId) {
 		
 		cache.remove(pageId);
 	}
 	
 	@Override
 	protected void finalize() throws Throwable{
 		try{
 			close();
 		} catch (Exception e) {
 			super.finalize();
 		}
 	}
 
 	/* (non-Javadoc)
 	 * @see com.freshbourne.io.PageManager#hasPage(long)
 	 */
 	@Override
 	public boolean hasPage(int id) {
 		return header.contains(id);
 	}
 
 	/* (non-Javadoc)
 	 * @see com.freshbourne.io.PageManager#sync()
 	 */
 	@Override
 	public void sync() {
 		LOG.debug("Syncing pages to disk");
 		for(RawPage p : cache.values()){
 			LOG.debug("trying to sync page " + p.id());
 			p.sync();
 		}
 	}
 
 	/**
 	 * @return the handle
 	 */
 	public RandomAccessFile getHandle() {
 		return handle;
 	}
 
 	/**
 	 * @return the file
 	 */
 	public File getFile() {
 		return file;
 	}
 }
