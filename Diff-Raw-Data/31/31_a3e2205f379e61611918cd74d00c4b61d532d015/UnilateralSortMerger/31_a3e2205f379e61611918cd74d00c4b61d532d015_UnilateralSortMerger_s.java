 /***********************************************************************************************************************
  *
  * Copyright (C) 2010 by the Stratosphere project (http://stratosphere.eu)
  *
  * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
  * the License. You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
  * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
  * specific language governing permissions and limitations under the License.
  *
  **********************************************************************************************************************/
 
 package eu.stratosphere.pact.runtime.sort;
 
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Comparator;
 import java.util.Iterator;
 import java.util.List;
 import java.util.NoSuchElementException;
 import java.util.concurrent.BlockingQueue;
 import java.util.concurrent.LinkedBlockingQueue;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 
 import eu.stratosphere.nephele.io.Reader;
 import eu.stratosphere.nephele.services.iomanager.Channel;
 import eu.stratosphere.nephele.services.iomanager.ChannelReader;
 import eu.stratosphere.nephele.services.iomanager.ChannelWriter;
 import eu.stratosphere.nephele.services.iomanager.Deserializer;
 import eu.stratosphere.nephele.services.iomanager.IOManager;
 import eu.stratosphere.nephele.services.iomanager.RawComparator;
 import eu.stratosphere.nephele.services.iomanager.SerializationFactory;
 import eu.stratosphere.nephele.services.memorymanager.MemoryAllocationException;
 import eu.stratosphere.nephele.services.memorymanager.MemoryManager;
 import eu.stratosphere.nephele.services.memorymanager.MemorySegment;
 import eu.stratosphere.nephele.template.AbstractTask;
 import eu.stratosphere.pact.common.type.Key;
 import eu.stratosphere.pact.common.type.KeyValuePair;
 import eu.stratosphere.pact.common.type.Value;
 import eu.stratosphere.pact.runtime.task.ReduceTask;
 
 /**
  * The {@link UnilateralSortMerger} is part of a merge-sort implementation.
  * The {@link ReduceTask} requires a grouping of the incoming key-value pairs by key. Typically grouping is achieved by
  * determining a total order for the given set of pairs (sorting). Thereafter an iteration over the ordered set is
  * performed and each time the key changes the consecutive objects are united into a new group.
  * Conceptually, a merge sort works as follows:
  * (1) Divide the unsorted list into n sublists of about 1/n the size. (2) Sort each sublist recursively by re-applying
  * merge sort. (3) Merge the two sublists back into one sorted list.
  * Internally, the {@link UnilateralSortMerger} logic is factored into three threads (read, sort, spill) which
  * communicate through a set of blocking queues (forming a closed loop).
  * Memory is allocated using the {@link MemoryManager} interface. Thus the component will most likely not exceed the
  * user-provided memory limits.
  * 
  * @author Erik Nijkamp
  * @author Stephan Ewen
  * 
  * @param <K>
  *        The key class
  * @param <V>
  *        The value class
  */
 public class UnilateralSortMerger<K extends Key, V extends Value> implements SortMerger<K, V>
 {
 	// ------------------------------------------------------------------------
 	//                              Constants
 	// ------------------------------------------------------------------------
 
 	/**
 	 * Logging.
 	 */
 	private static final Log LOG = LogFactory.getLog(UnilateralSortMerger.class);
 
 	// ------------------------------------------------------------------------
 	//                               Fields
 	// ------------------------------------------------------------------------
 
 	/**
 	 * This list contains all segments of allocated memory. They will be freed the latest in the
 	 * shutdown method. If some segments have been freed before, they will not be freed again.
 	 */
 	private final List<MemorySegment> allocatedMemory;
 
 	/**
 	 * The memory manager through which memory is allocated and released.
 	 */
 	protected final MemoryManager memoryManager;
 
 	/**
 	 * The I/O manager through which file reads and writes are performed.
 	 */
 	protected final IOManager ioManager;
 
 	/**
 	 * The comparator through which an order over the keys is established.
 	 */
 	protected final Comparator<K> keyComparator;
 
 	/**
 	 * Factory used to deserialize the keys.
 	 */
 	protected final SerializationFactory<K> keySerialization;
 
 	/**
 	 * Factory used to deserialize the values.
 	 */
 	protected final SerializationFactory<V> valueSerialization;
 	
 	/**
 	 * The parent task that owns this sorter.
 	 */
 	protected final AbstractTask parent;
 	
 	/**
 	 * The monitor which guards the iterator field.
 	 */
 	protected final Object iteratorLock = new Object();
 	
 	/**
 	 * The iterator to be returned by the sort-merger. This variable is zero, while receiving and merging is still in
 	 * progress and it will be set once we have &lt; merge factor sorted sub-streams that will then be streamed sorted.
 	 */
 	protected Iterator<KeyValuePair<K, V>> iterator;
 	
 	/**
 	 * The exception that is set, if the iterator cannot be created.
 	 */
 	protected IOException iteratorException;
 
 	/**
 	 * The maximum number of file handles
 	 */
 	protected final int maxNumFileHandles;
 	
 	// ------------------------------------------------------------------------
 	// Threads
 	// ------------------------------------------------------------------------
 
 	/**
 	 * The thread that reads the input channels into buffers and passes them on to the merger.
 	 */
 	private final ThreadBase readThread;
 
 	/**
 	 * The thread that merges the buffer handed from the reading thread.
 	 */
 	private final ThreadBase sortThread;
 
 	/**
 	 * The thread that handles spilling to secondary storage.
 	 */
 	private final ThreadBase spillThread;
 
 	// ------------------------------------------------------------------------
 	// Constructor & Shutdown
 	// ------------------------------------------------------------------------
 
 	/**
 	 * @param memoryManager
 	 * @param ioManager
 	 * @param numSortBuffers
 	 * @param sizeSortBuffer
 	 * @param ioMemorySize
 	 * @param maxNumFileHandles
 	 * @param keySerialization
 	 * @param valueSerialization
 	 * @param keyComparator
 	 * @param reader
 	 * @param offsetArrayPerc
 	 * @param parentTask
 	 * @throws IOException
 	 * @throws MemoryAllocationException
 	 */
 	public UnilateralSortMerger(MemoryManager memoryManager, IOManager ioManager, int numSortBuffers,
 			int sizeSortBuffer, int ioMemorySize, int maxNumFileHandles, SerializationFactory<K> keySerialization,
 			SerializationFactory<V> valueSerialization, Comparator<K> keyComparator, Reader<KeyValuePair<K, V>> reader,
 			AbstractTask parentTask)
 	throws IOException, MemoryAllocationException
 	{
 		// sanity checks
 		if (memoryManager == null) {
 			throw new NullPointerException("Memory manager must not be null.");
 		}
 		if (ioManager == null) {
 			throw new NullPointerException("IO-Manager must not be null.");
 		}
 		if (parentTask == null) {
 			throw new NullPointerException("Parent Task must not be null.");
 		}
 		
 		
 		this.maxNumFileHandles = maxNumFileHandles;
 		this.memoryManager = memoryManager;
 		this.ioManager = ioManager;
 		this.keyComparator = keyComparator;
 		this.keySerialization = keySerialization;
 		this.valueSerialization = valueSerialization;
 		this.parent = parentTask;
 
 		this.allocatedMemory = new ArrayList<MemorySegment>();
 
 		// circular queues
 		CircularQueues circularQueues = new CircularQueues();
 
 		// fill empty queue with buffers
 		for (int i = 0; i < numSortBuffers; i++) {
 			// serialization
 			Deserializer<K> keyDeserializer = keySerialization.getDeserializer();
 
 			// comparator
 			RawComparator comparator = new DeserializerComparator<K>(keyDeserializer, keyComparator);
 
 			// get memory for sorting
 			MemorySegment seg = memoryManager.allocate(parentTask, sizeSortBuffer);
 			freeSegmentAtShutdown(seg);
 
 			// sort-buffer
 			BufferSortableGuaranteed<K, V> buffer = new BufferSortableGuaranteed<K, V>(seg, comparator, keySerialization,
 				valueSerialization);
 
 			// add to empty queue
 			CircularElement element = new CircularElement(i, buffer);
 			circularQueues.empty.add(element);
 		}
 
 		// exception handling
 		ExceptionHandler<IOException> exceptionHandler = new ExceptionHandler<IOException>() {
 			public void handleException(IOException exception) {
 				// forward exception
 				setResultIteratorException(exception);
 				close();
 			}
 		};
 
 		// start the thread that reads the input channels
 		readThread = getReadingThread(exceptionHandler, reader, circularQueues, parentTask);
 
 		// start the thread that sorts the buffers
 		sortThread = getSortingThread(exceptionHandler, circularQueues, parentTask);
 
 		// start the thread that handles spilling to secondary storage
 		spillThread = getSpillingThread(exceptionHandler, circularQueues, memoryManager, ioManager, ioMemorySize,
 			parentTask, numSortBuffers >= 3 ? numSortBuffers - 2 : 0);
 		
 		startThreads();
 	}
 	
 	/**
 	 * Starts all the threads that are used by this sort-merger.
 	 */
 	protected void startThreads()
 	{
 		// start threads
 		readThread.start();
 		sortThread.start();
 		spillThread.start();
 	}
 
 	/**
 	 * Shuts down all the threads initiated by this sort/merger. Also releases all previously allocated
 	 * memory, if it has not yet been released by the threads.
 	 * <p>
 	 * The threads are set to exit directly, but depending on their operation, it may take a while to actually happen.
 	 * The sorting thread will for example not finish before the current batch is sorted. This method attempts to wait
 	 * for the working thread to exit. If it is however interrupted, the method exits immediately and is not guaranteed
 	 * how long the threads continue to exist and occupy resources afterwards.
 	 *
 	 * @see java.io.Closeable#close()
 	 */
 	@Override
 	public void close() {
 		try {
 			if (readThread != null) {
 				try {
 					readThread.shutdown();
 				} catch (Throwable t) {
 					LOG.error("Error shutting down reader thread: " + t.getMessage(), t);
 				}
 			}
 			if (sortThread != null) {
 				try {
 					sortThread.shutdown();
 				} catch (Throwable t) {
 					LOG.error("Error shutting down sorter thread: " + t.getMessage(), t);
 				}
 			}
 			if (spillThread != null) {
 				try {
 					spillThread.shutdown();
 				} catch (Throwable t) {
 					LOG.error("Error shutting down spilling thread: " + t.getMessage(), t);
 				}
 			}
 
 			try {
 				if (readThread != null) {
 					readThread.join();
 				}
 				
 				if (sortThread != null) {
 					sortThread.join();
 				}
 				
 				if (spillThread != null) {
 					spillThread.join();
 				}
 			}
 			catch (InterruptedException iex) {
 				LOG.debug("Closing of sort/merger was interrupted. " +
 						"The reading/sorting/spilling threads may still be working.", iex);
 			}
 		} finally {
 			// release all memory
 			memoryManager.release(this.allocatedMemory);
 			this.allocatedMemory.clear();
 		}
 	}
 
 	/**
 	 * Adds a given memory segment to the list of segments that are to be released at shutdown.
 	 * 
 	 * @param s The memory segment to add to the list.
 	 */
 	public void freeSegmentAtShutdown(MemorySegment s) {
 		this.allocatedMemory.add(s);
 	}
 
 	/**
 	 * Adds a given collection of memory segments to the list of segments that are to be released at shutdown.
 	 * 
 	 * @param s The collection of memory segments.
 	 */
 	public void freeSegmentsAtShutdown(Collection<MemorySegment> s) {
 		this.allocatedMemory.addAll(s);
 	}
 
 	// ------------------------------------------------------------------------
 	//                           Factory Methods
 	// ------------------------------------------------------------------------
 
 	/**
 	 * Creates the reading thread. The reading thread simply reads the data off the input and puts it
 	 * into the buffer where it will be sorted.
 	 * <p>
 	 * The returned thread is not yet started.
 	 * 
 	 * @param exceptionHandler
 	 *        The handler for exceptions in the thread.
 	 * @param reader
 	 *        The reader from which the thread reads.
 	 * @param queues
 	 *        The queues through which the thread communicates with the other threads.
 	 * @param parentTask
 	 *        The task at which the thread registers itself (for profiling purposes).
 	 * @return The thread that reads data from a Nephele reader and puts it into a queue.
 	 */
 	protected ThreadBase getReadingThread(ExceptionHandler<IOException> exceptionHandler,
 			eu.stratosphere.nephele.io.Reader<KeyValuePair<K, V>> reader, CircularQueues queues, AbstractTask parentTask) {
 		return new ReadingThread(exceptionHandler, reader, queues, parentTask);
 	}
 
 	/**
 	 * Creates the sorting thread. This thread takes the buffers from the sort queue, sorts them and
 	 * puts them into the spill queue.
 	 * <p>
 	 * The returned thread is not yet started.
 	 * 
 	 * @param exceptionHandler
 	 *        The handler for exceptions in the thread.
 	 * @param queues
 	 *        The queues through which the thread communicates with the other threads.
 	 * @param parentTask
 	 *        The task at which the thread registers itself (for profiling purposes).
 	 * @return The sorting thread.
 	 */
 	protected ThreadBase getSortingThread(ExceptionHandler<IOException> exceptionHandler, CircularQueues queues,
 			AbstractTask parentTask) {
 		return new SortingThread(exceptionHandler, queues, parentTask);
 	}
 
 	/**
 	 * Creates the spilling thread. This thread also merges the number of sorted streams until a sufficiently
 	 * small number of streams is produced that can be merged on the fly while returning the results.
 	 * 
 	 * @param exceptionHandler
 	 *        The handler for exceptions in the thread.
 	 * @param queues
 	 *        The queues through which the thread communicates with the other threads.
 	 * @param memoryManager
 	 *        The memory manager from which the memory is allocated.
 	 * @param ioManager
 	 *        The I/O manager
 	 * @param ioMemorySize
 	 *        The amount of memory that is dedicated to reading and writing.
 	 * @param parentTask
 	 *        The task at which the thread registers itself (for profiling purposes).
 	 * @return The thread that does the spilling and pre-merging.
 	 */
 	protected ThreadBase getSpillingThread(ExceptionHandler<IOException> exceptionHandler, CircularQueues queues,
 			MemoryManager memoryManager, IOManager ioManager, int ioMemorySize, AbstractTask parentTask,
 			int buffersToKeepBeforeSpilling)
 	{
 		return new SpillingThread(exceptionHandler, queues, memoryManager, ioManager, ioMemorySize,
 			parentTask, buffersToKeepBeforeSpilling);
 	}
 
 	// ------------------------------------------------------------------------
 	//                           Result Iterator
 	// ------------------------------------------------------------------------
 
 	/*
 	 * (non-Javadoc)
 	 * @see eu.stratosphere.pact.runtime.sort.SortMerger#getIterator()
 	 */
 	@Override
 	public Iterator<KeyValuePair<K, V>> getIterator() {
 		synchronized (this.iteratorLock) {
 			// wait while both the iterator and the exception are not set
 			while (this.iterator == null && this.iteratorException == null) {
 				try {
 					this.iteratorLock.wait();
 				}
 				catch (InterruptedException iex) {
 					LOG.error("SHOULD NOT BE", iex);
 				}
 			}
 			
 			if (this.iteratorException != null) {
 				throw new RuntimeException("Error obtaining the sorted input: " + this.iteratorException.getMessage(),
 					this.iteratorException);
 			}
 			else {
 				return this.iterator;
 			}
 		}
 	}
 	
 	/**
 	 * Sets the result iterator. By setting the result iterator, all threads that are waiting for the result
 	 * iterator are notified and will obtain it.
 	 * 
 	 * @param iterator The result iterator to set.
 	 */
 	protected void setResultIterator(Iterator<KeyValuePair<K, V>> iterator) {
 		synchronized (this.iteratorLock) {
 			this.iterator = iterator;
 			this.iteratorLock.notifyAll();
 		}
 	}
 	
 	/**
 	 * Reports an exception to all threads that are waiting for the result iterator.
 	 * 
 	 * @param ioex The exception to be reported to the threads that wait for the result iterator.
 	 */
 	protected void setResultIteratorException(IOException ioex) {
 		synchronized (this.iteratorLock) {
 			this.iteratorException = ioex;
 			this.iteratorLock.notifyAll();
 		}
 	}
 
 	// ------------------------------------------------------------------------
 	// Result Merging
 	// ------------------------------------------------------------------------
 
 	/**
 	 * Returns an iterator that iterates over the merged result from all given channels.
 	 * 
 	 * @param channelIDs
 	 *        The channels that are to be merged and returned.
 	 * @param ioMemorySize
 	 *        The size of I/O memory that can be used for reading.
 	 * @return An iterator over the merged KeyValuePairs of the input channels.
 	 * @throws MemoryAllocationException
 	 * @throws IOException
 	 *         Thrown, if the readers
 	 */
 	protected final Iterator<KeyValuePair<K, V>> getMergingIterator(final List<Channel.ID> channelIDs,
 			final int ioMemorySize)
 	throws MemoryAllocationException, IOException
 	{
 		// check if we do not have a channel at all. This happens if the input was empty
 		if (channelIDs.isEmpty()) {
 			// no data
 			return new EmptyKeyValueIterator<K, V>();
 		}
 
 		// create one iterator per channel id
 		LOG.debug("Initiating final merge. Opening " + channelIDs.size() + " ChannelReaders.");
 
 		List<Iterator<KeyValuePair<K, V>>> iterators = new ArrayList<Iterator<KeyValuePair<K, V>>>();
 		final int ioMemoryPerChannel = ioMemorySize / channelIDs.size();
 
 		for (Channel.ID id : channelIDs) {
 			final Collection<MemorySegment> inputSegments = memoryManager.allocate(this.parent, 1, ioMemoryPerChannel);
 			freeSegmentsAtShutdown(inputSegments);
 
 			ChannelReader reader = ioManager.createChannelReader(id, inputSegments, true);
 
 			// wrap channel reader as iterator
 			final Iterator<KeyValuePair<K, V>> iterator = new KVReaderIterator<K, V>(reader, keySerialization,
 				valueSerialization, memoryManager, true);
 			iterators.add(iterator);
 		}
 
 		return new MergeIterator<K, V>(iterators, keyComparator);
 	}
 
 	/**
 	 * @param channelIDs
 	 * @param ioMemorySize
 	 * @return A list of channels that can be merged in one turn.
 	 * @throws Exception
 	 */
 	protected List<Channel.ID> mergeChannelList(List<Channel.ID> channelIDs, int ioMemorySize) throws Exception {
 
 		int channelsToMergePerStep = ((channelIDs.size() / (((int) Math.floor(((double) channelIDs.size())
 			/ ((double) maxNumFileHandles))) + 1)) + 1);
 		ArrayList<Channel.ID> mergedChannelIDs = new ArrayList<Channel.ID>();
 
 		ArrayList<Channel.ID> channelsToMerge;
 		while (!channelIDs.isEmpty()) {
 			channelsToMerge = new ArrayList<Channel.ID>();
 
 			for (int i = 0; (i < channelsToMergePerStep && i < channelIDs.size()); i++) {
 				channelsToMerge.add(channelIDs.get(i));
 			}
 			mergedChannelIDs.add(mergeChannels(channelsToMerge, ioMemorySize));
 			channelIDs.removeAll(channelsToMerge);
 		}
 
 		return mergedChannelIDs;
 
 	}
 
 	/**
 	 * @param channelIDs
 	 * @param ioMemorySize
 	 * @return The ID of the channel that hold the merged data of the input channels.
 	 */
 	protected Channel.ID mergeChannels(List<Channel.ID> channelIDs, int ioMemorySize)
 	throws IOException, MemoryAllocationException
 	{
 		List<Iterator<KeyValuePair<K, V>>> iterators = new ArrayList<Iterator<KeyValuePair<K, V>>>();
 		final int ioMemoryPerChannel = ioMemorySize / (channelIDs.size() + 2);
 
 		for (Channel.ID id : channelIDs) {
 
 			Collection<MemorySegment> inputSegments;
 			
 			inputSegments = memoryManager.allocate(this.parent, 1, ioMemoryPerChannel);
 			freeSegmentsAtShutdown(inputSegments);
 
 			final ChannelReader reader = ioManager.createChannelReader(id, inputSegments, true);
 
 			// wrap channel reader as iterator
 			final Iterator<KeyValuePair<K, V>> iterator = new KVReaderIterator<K, V>(reader, keySerialization,
 				valueSerialization, memoryManager, true);
 			iterators.add(iterator);
 		}
 
 		MergeIterator<K, V> mi = new MergeIterator<K, V>(iterators, keyComparator);
 
 		// create a new channel writer
 		final Channel.Enumerator enumerator = ioManager.createChannelEnumerator();
 		final Channel.ID mergedChannelID = enumerator.next();
 
 		Collection<MemorySegment> outputSegments = memoryManager.allocate(this.parent, 2, ioMemoryPerChannel);
 		freeSegmentsAtShutdown(outputSegments);
 
 		ChannelWriter writer = ioManager.createChannelWriter(mergedChannelID, outputSegments);
 
 		while (mi.hasNext()) {
 
 			// read sorted pairs into memory buffer
 			KeyValuePair<K, V> pair = mi.next();
 			if (!writer.write(pair)) {
 				throw new RuntimeException("Writing of pair during merging failed");
 			}
 		}
 
 		// close channel writer
 		outputSegments = writer.close();
 		memoryManager.release(outputSegments);
 
 		return mergedChannelID;
 	}
 
 	// ------------------------------------------------------------------------
 	// Inter-Thread Communication
 	// ------------------------------------------------------------------------
 
 	/**
 	 * The element that is passed as marker for the end of data.
 	 */
 	protected final CircularElement SENTINEL = new CircularElement();
 
 	/**
 	 * Class representing buffers that circulate between the reading, sorting and spilling thead.
 	 */
 	protected final class CircularElement {
 		final int id;
 
 		final BufferSortableGuaranteed<K, V> buffer;
 
 		public CircularElement() {
 			this.buffer = null;
 			this.id = -1;
 		}
 
 		public CircularElement(int id, BufferSortableGuaranteed<K, V> buffer) {
 			this.id = id;
 			this.buffer = buffer;
 		}
 	}
 
 	/**
 	 * Collection of queues that are used for the communication between the threads.
 	 */
 	protected final class CircularQueues {
 		final BlockingQueue<CircularElement> empty;
 
 		final BlockingQueue<CircularElement> sort;
 
 		final BlockingQueue<CircularElement> spill;
 
 		public CircularQueues() {
 			this.empty = new LinkedBlockingQueue<CircularElement>();
 			this.sort = new LinkedBlockingQueue<CircularElement>();
 			this.spill = new LinkedBlockingQueue<CircularElement>();
 		}
 	}
 
 	// ------------------------------------------------------------------------
 	// Threads
 	// ------------------------------------------------------------------------
 
 	/**
 	 * Base class for all working threads in this sort-merger. The specific threads for reading, sorting, spilling,
 	 * merging, etc... extend this subclass.
 	 * <p>
 	 * The threads are designed to terminate themselves when the task they are set up to do is completed. Further more,
 	 * they terminate immediately when the <code>shutdown()</code> method is called.
 	 */
 	protected abstract class ThreadBase extends Thread implements Thread.UncaughtExceptionHandler {
 		/**
 		 * The queue of empty buffer that can be used for reading;
 		 */
 		protected final CircularQueues queues;
 
 		/**
 		 * The exception handler for any problems.
 		 */
 		private final ExceptionHandler<IOException> exceptionHandler;
 
 		/**
 		 * The parent task at whom the thread needs to register.
 		 */
 		private final AbstractTask parentTask;
 
 		/**
 		 * The flag marking this thread as alive.
 		 */
 		private volatile boolean alive;
 
 		/**
 		 * Creates a new thread.
 		 * 
 		 * @param exceptionHandler
 		 *        The exception handler to call for all exceptions.
 		 * @param name
 		 *        The name of the thread.
 		 * @param queues
 		 *        The queues used to pass buffers between the threads.
 		 */
 		protected ThreadBase(ExceptionHandler<IOException> exceptionHandler, String name, CircularQueues queues,
 				AbstractTask parentTask)
 		{
 			// thread setup
 			super(name);
 			this.setDaemon(true);
 
 			// exception handling
 			this.exceptionHandler = exceptionHandler;
 			this.setUncaughtExceptionHandler(this);
 
 			// queues
 			this.queues = queues;
 
 			this.parentTask = parentTask;
 
 			this.alive = true;
 		}
 
 		/**
 		 * Implements exception handling and delegates to go().
 		 */
 		public void run() {
 			try {
 				if (this.parentTask != null) {
 					this.parentTask.userThreadStarted(this);
 				}
 				go();
 			} catch (Throwable t) {
 				internalHandleException(new IOException("Thread '" + getName() + "' terminated due to an exception: "
 					+ t.getMessage(), t));
 			} finally {
 				if (this.parentTask != null) {
 					this.parentTask.userThreadFinished(this);
 				}
 			}
 		}
 
 		/**
 		 * Equivalent to the run() method.
 		 * 
 		 * @throws Exception
 		 *         Exceptions that prohibit correct completion of the work may be thrown by the thread.
 		 */
 		protected abstract void go() throws Exception;
 
 		/**
 		 * Checks whether this thread is still alive.
 		 * 
 		 * @return true, if the thread is alive, false otherwise.
 		 */
 		public boolean isRunning() {
 			return this.alive;
 		}
 
 		/**
 		 * Forces an immediate shutdown of the thread. Looses any state and all buffers that the thread is currently
 		 * working on. This terminates cleanly for the JVM, but looses intermediate results.
 		 */
 		public void shutdown() {
 			this.alive = false;
 			this.interrupt();
 		}
 
 		/**
 		 * Internally handles an exception and makes sure that this method returns without a problem.
 		 * 
 		 * @param ioex
 		 *        The exception to handle.
 		 */
 		protected final void internalHandleException(IOException ioex) {
 			if (exceptionHandler != null) {
 				try {
 					exceptionHandler.handleException(ioex);
 				} catch (Throwable t) {
 				}
 			}
 		}
 
 		/**
 		 * {@inheritDoc}
 		 */
 		@Override
 		public void uncaughtException(Thread t, Throwable e) {
 			internalHandleException(new IOException("Thread '" + t.getName()
 				+ "' terminated due to an uncaught exception: " + e.getMessage(), e));
 		}
 	}
 
 	/**
 	 * The thread that consumes the input data and puts it into a buffer that will be sorted.
 	 */
 	private class ReadingThread extends ThreadBase
 	{
 		/**
 		 * The input channels to read from.
 		 */
 		private final eu.stratosphere.nephele.io.Reader<KeyValuePair<K, V>> reader;
 
 		/**
 		 * Creates a new reading thread.
 		 * 
 		 * @param exceptionHandler
 		 *        The exception handler to call for all exceptions.
 		 * @param reader
 		 *        The reader to pull the data from.
 		 * @param queues
 		 *        The queues used to pass buffers between the threads.
 		 */
 		public ReadingThread(ExceptionHandler<IOException> exceptionHandler,
 				eu.stratosphere.nephele.io.Reader<KeyValuePair<K, V>> reader, CircularQueues queues,
 				AbstractTask parentTask) {
 			super(exceptionHandler, "SortMerger Reading Thread", queues, parentTask);
 
 			// members
 			this.reader = reader;
 		}
 
 		/**
 		 * The entry point for the thread. Gets a buffer for all threads and then loops as long as there is input
 		 * available.
 		 */
 		public void go() throws Exception {
 			// initially, grab a buffer
 			CircularElement element = null;
 			while (element == null) {
 				try {
 					element = queues.empty.take();
 				} catch (InterruptedException iex) {
 					if (!isRunning())
 						return;
 				}
 			}
 
 			// we have two loops here, one with debug statements and one without
 			// the reason is that the string construction always takes place, even
 			// when the debug logging is later discarded because of a more coarse log level
 
 			if (LOG.isDebugEnabled()) {
 				// now loop until all channels have no more input data
 				while (isRunning() && reader.hasNext()) {
 					KeyValuePair<K, V> pair = reader.next();
 					if (!element.buffer.write(pair)) {
 						LOG.debug("Emitting full read buffer " + element.id + ".");
 
 						queues.sort.put(element);
 						element = null;
 
 						do {
 							try {
 								element = queues.empty.take();
 							} catch (InterruptedException iex) {
 								if (!isRunning()) {
 									return;
 								}
 							}
 						} while (element == null);
 
 						if (!element.buffer.isEmpty()) {
 							LOG.error("New buffer is not empty.");
 						}
 						element.buffer.write(pair);
 
 						LOG.debug("Retrieved empty read buffer " + element.id + ".");
 					}
 				}
 			} else {
 				// now loop until all channels have no more input data
 				while (isRunning() && reader.hasNext()) {
 					KeyValuePair<K, V> pair = reader.next();
 
 					if (!element.buffer.write(pair)) {
 						queues.sort.put(element);
 						element = null;
 
 						do {
 							try {
 								element = queues.empty.take();
 							} catch (InterruptedException iex) {
 								if (!isRunning()) {
 									return;
 								}
 							}
 						} while (element == null);
 
 						if (!element.buffer.isEmpty()) {
 							LOG.error("New buffer is not empty.");
 						}
 						element.buffer.write(pair);
 					}
 				}
 			}
 
 			if (!isRunning()) {
 				return;
 			}
 
 			// final buffer
 			if (!element.buffer.isEmpty()) {
 				LOG.debug("Emitting last read buffer " + element.id + ".");
 				queues.sort.put(element);
 			}
 			queues.sort.put(SENTINEL);
 
 			LOG.debug("Reading thread done.");
 		}
 	}
 
 	/**
 	 * The thread that sorts filled buffers.
 	 */
 	private class SortingThread extends ThreadBase {
 		/**
 		 * The sorter.
 		 */
 		private final IndexedSorter sorter;
 
 		/**
 		 * Creates a new sorting thread.
 		 * 
 		 * @param exceptionHandler
 		 *        The exception handler to call for all exceptions.
 		 * @param queues
 		 *        The queues used to pass buffers between the threads.
 		 */
 		public SortingThread(ExceptionHandler<IOException> exceptionHandler, CircularQueues queues,
 				AbstractTask parentTask) {
 			super(exceptionHandler, "SortMerger sorting thread", queues, parentTask);
 
 			// members
 			this.sorter = new QuickSort();
 		}
 
 		/**
 		 * Entry point of the thread.
 		 */
 		public void go() throws Exception {
 			boolean alive = true;
 
 			// loop as long as the thread is marked alive
 			while (isRunning() && alive) {
 				CircularElement element = null;
 				try {
 					element = queues.sort.take();
 				} catch (InterruptedException iex) {
 					if (!isRunning())
 						return;
 					else
 						continue;
 				}
 
 				if (element != SENTINEL) {
 					LOG.debug("Sorting buffer " + element.id + ".");
 
 					sorter.sort(element.buffer);
 
 					LOG.debug("Sorted buffer " + element.id + ".");
 				} else {
 					LOG.debug("Sorting thread done.");
 
 					alive = false;
 				}
 
 				queues.spill.put(element);
 			}
 		}
 	}
 
 	private class SpillingThread extends ThreadBase {
 		private final MemoryManager memoryManager;
 
 		private final IOManager ioManager;
 
 		private final int ioMemorySize;
 		
 		private Collection<MemorySegment> outputSegments;
 		
 		private final int buffersToKeepBeforeSpilling;
 
 		public SpillingThread(ExceptionHandler<IOException> exceptionHandler, CircularQueues queues,
 				MemoryManager memoryManager, IOManager ioManager, int ioMemorySize, AbstractTask parentTask,
 				int buffersToKeepBeforeSpilling)
 		{
 			super(exceptionHandler, "SortMerger spilling thread", queues, parentTask);
 
 			// members
 			this.memoryManager = memoryManager;
 			this.ioManager = ioManager;
 			this.ioMemorySize = ioMemorySize;
 			this.buffersToKeepBeforeSpilling = buffersToKeepBeforeSpilling;
 		}
 
 		/**
 		 * Entry point of the thread.
 		 */
 		public void go() throws Exception {
			final Channel.Enumerator enumerator = ioManager.createChannelEnumerator();
			List<Channel.ID> channelIDs = new ArrayList<Channel.ID>();

			// allocate memory segments for channel writer
			outputSegments = memoryManager.allocate(UnilateralSortMerger.this.parent, 2, ioMemorySize / 2);
			freeSegmentsAtShutdown(outputSegments);
 
 			/* ## 1. cache segments ## */
 			List<CircularElement> cache = new ArrayList<CircularElement>(buffersToKeepBeforeSpilling);
 			CircularElement element = null;
 			boolean cacheOnly = false;
 			
 			// see whether we should keep some buffers
 			if(buffersToKeepBeforeSpilling > 0) {
 				// fill cache
 				while (isRunning()) {					
 					// is cache exhausted?
 					if(cache.size() >= buffersToKeepBeforeSpilling) {
 						cacheOnly = false;
 						break;
 					}
 					
 					// take next element from queue
 					element = queues.spill.take();
 					cache.add(element);
 					if(element == SENTINEL) {
 						cacheOnly = true;
 						break;
 					}
 				}
 			}
 			
 			/* ## 2. merge segments ## */
 			if(cacheOnly) {
 				
 				/* # case 1: operates on in-memory segments only # */
 				LOG.debug("Initiating merge-iterator (in-memory segments).");
 				
 				List<Iterator<KeyValuePair<K, V>>> iterators = new ArrayList<Iterator<KeyValuePair<K, V>>>();
 				
 				// iterate buffers and collect a set of iterators
				for(CircularElement cached : cache)
 				{
					// note: the yielded iterator only operates on the buffer heap (and disregards the stack)
					iterators.add(cached.buffer.getIterator());
 				}
 				
 				// release sort-buffers
 				LOG.debug("Releasing sort-buffer memory.");
 				while (!queues.empty.isEmpty()) {
 					memoryManager.release(queues.empty.take().buffer.unbind());
 				}
 
 				// set lazy iterator
 				setResultIterator(new MergeIterator<K, V>(iterators, keyComparator));
 			
 			} else {
 				
 				/* # case 2: operates on materialized segments only # */
 				LOG.debug("Initiating merge-iterator (materialized segments).");
 				
 				// loop as long as the thread is marked alive and we do not see the final
 				// element
 				while (isRunning() && (element = takeNext(queues.spill, cache)) != SENTINEL) {
 					// open next channel
 					Channel.ID channel = enumerator.next();
 					channelIDs.add(channel);
 	
 					// create writer
 					ChannelWriter writer = ioManager.createChannelWriter(channel, outputSegments);
 	
 					// write sort-buffer to channel
 					LOG.debug("Spilling buffer " + element.id + ".");
 					element.buffer.writeToChannel(writer);
 					LOG.debug("Spilled buffer " + element.id + ".");
 	
 					// free buffers, store id
 					outputSegments = writer.close();
 	
 					// pass empty sort-buffer to reading thread
 					element.buffer.reset();
 					queues.empty.put(element);
 				}
 	
 				// done with the spilling
 				LOG.debug("Spilling done.");
 	
 				// free output buffers
 				LOG.debug("Releasing output-buffer memory.");
 				memoryManager.release(outputSegments);
 	
 				// release sort-buffers
 				LOG.debug("Releasing sort-buffer memory.");
 				while (!queues.empty.isEmpty()) {
 					memoryManager.release(queues.empty.take().buffer.unbind());
 				}
 	
 				// merge channels until sufficient file handles are available
 				while (channelIDs.size() > maxNumFileHandles) {
 					channelIDs = mergeChannelList(channelIDs, ioMemorySize);
 				}
 	
 				// set lazy iterator
 				setResultIterator(getMergingIterator(channelIDs, ioMemorySize));
 			}			
 
 			// done
 			LOG.debug("Spilling thread done.");
 		}
 		
 		private CircularElement takeNext(BlockingQueue<CircularElement> queue, List<CircularElement> cache) throws InterruptedException {
 			return cache.isEmpty() ? queue.take() : cache.remove(0);
 		}
 		
 		@Override
 		public void shutdown() {
 			this.memoryManager.release(outputSegments);
 			super.shutdown();
 		}
 	}
 
 	/**
 	 * This class represents an iterator over a key/value stream that is obtained from a reader.
 	 */
 	protected static final class KVReaderIterator<K extends Key, V extends Value> implements
 			Iterator<KeyValuePair<K, V>>
 	{
 		private final ChannelReader reader; // the reader from which to get the input
 
 		private final SerializationFactory<K> keySerialization; // deserializer for keys
 
 		private final SerializationFactory<V> valueSerialization; // deserializer for values
 
 		private final MemoryManager toRelease; // memory manager at which memory is released
 
 		private KeyValuePair<K, V> next; // the next pair to be returned
 
 		private final boolean deleteWhenDone; // flag describing whether to delete the channel once it has been read
 
 		/**
 		 * Creates a new reader iterator.
 		 * 
 		 * @param reader
 		 *        The reader from which to read the keys and values.
 		 * @param keySerialization
 		 *        The factory to instantiate keys.
 		 * @param valueSerialization
 		 *        The factory to instantiate values.
 		 * @param memManager
 		 *        The memory manager that is used to release the memory segments used by the reader.
 		 */
 		protected KVReaderIterator(ChannelReader reader, SerializationFactory<K> keySerialization,
 				SerializationFactory<V> valueSerialization, MemoryManager memManager, boolean deleteWhenDone) {
 			this.reader = reader;
 			this.keySerialization = keySerialization;
 			this.valueSerialization = valueSerialization;
 			this.toRelease = memManager;
 			this.deleteWhenDone = deleteWhenDone;
 		}
 
 		/*
 		 * (non-Javadoc)
 		 * @see java.util.Iterator#hasNext()
 		 */
 		@Override
 		public boolean hasNext() {
 			if (next != null) {
 				return true;
 			}
 
 			// immutable deserialization
 			final K key = keySerialization.newInstance();
 			final V value = valueSerialization.newInstance();
 
 			next = new KeyValuePair<K, V>(key, value);
 
 			try {
 				if (!reader.read(next)) {
 					next = null;
 					toRelease.release(reader.close());
 	
 					if (this.deleteWhenDone) {
 						reader.deleteChannel();
 					}
 	
 					return false;
 				} else {
 					return true;
 				}
 			}
 			catch (IOException ioex) {
 				throw new RuntimeException(ioex);
 			}
 		}
 
 		/*
 		 * (non-Javadoc)
 		 * @see java.util.Iterator#next()
 		 */
 		@Override
 		public KeyValuePair<K, V> next() {
 			if (!hasNext()) {
 				throw new NoSuchElementException();
 			}
 
 			KeyValuePair<K, V> p = next;
 			next = null;
 
 			return p;
 		}
 
 		/*
 		 * (non-Javadoc)
 		 * @see java.util.Iterator#remove()
 		 */
 		@Override
 		public void remove() {
 			throw new UnsupportedOperationException();
 		}
 	}
 
 	protected static final class EmptyKeyValueIterator<K extends Key, V extends Value> implements
 			Iterator<KeyValuePair<K, V>> {
 		/*
 		 * (non-Javadoc)
 		 * @see java.util.Iterator#hasNext()
 		 */
 		@Override
 		public boolean hasNext() {
 			return false;
 		}
 
 		/*
 		 * (non-Javadoc)
 		 * @see java.util.Iterator#next()
 		 */
 		@Override
 		public KeyValuePair<K, V> next() {
 			throw new NoSuchElementException();
 		}
 
 		/*
 		 * (non-Javadoc)
 		 * @see java.util.Iterator#remove()
 		 */
 		@Override
 		public void remove() {
 			throw new UnsupportedOperationException();
 		}
 	}
 
 }
