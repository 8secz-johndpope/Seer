 package org.cytoscape.io.write;
 
 
 import org.cytoscape.work.AbstractTask;
 import org.cytoscape.work.TaskMonitor;
 import org.cytoscape.work.Tunable;
 import org.cytoscape.work.util.ListSingleSelection;
 import org.cytoscape.io.CyFileFilter;
 import org.cytoscape.io.write.CyWriter;
 import org.cytoscape.io.write.CyWriterManager;
 
 import java.io.File;
 
 import java.util.Map;
 import java.util.TreeMap;
 import java.util.ArrayList;
 
 
 /**
  * An abstract utility implementation of a Task that writes a user defined 
  * file to a file type determined by a provided writer manager.  This class
  * is meant to be extended for specific file types such that the appropriate
  * {@link org.cytoscape.io.write.CyWriter} can be identified.
  * @param <T> Generic type that extends CyWriterManager.
  * @CyAPI.Abstract.Class
  */
 public abstract class AbstractCyWriter<T extends CyWriterManager> extends AbstractTask
 	implements CyWriter
 {
 	/** The file to be written. */
 	protected File outputFile;
 
 	/**
 	 * The method sets the file to be written.  This field should not
 	 * be called directly, but rather handled by the {@link org.cytoscape.work.Tunable}
 	 * processing. This method is the "setter" portion of a
 	 * getter/setter tunable method pair.
 	 * @param f The file to be written.
 	 */
 	public final void setOutputFile(File f) {
 		if ( f != null )
 			outputFile = f;
 	}
 
 	/**
 	 * This method gets the file to be written.  This method should not
 	 * be called directly, but rather handled by the {@link org.cytoscape.work.Tunable}
 	 * processing. This method is the "getter" portion of a
 	 * getter/setter tunable method pair.
 	 * @return The file to be written.
 	 */
 	public File getOutputFile() {
 		return outputFile;
 	}
 
 	/** An implementation of this method should return a file format description
 	 * from {@link CyFileFilter}, such that the string can be found in the descriptionFilterMap.
 	 * @return a file format description from {@link CyFileFilter}.
 	 */
 	abstract protected String getExportFileFormat();
 	/** A Map that maps description strings to {@link CyFileFilter}s*/
 	protected final Map<String,CyFileFilter> descriptionFilterMap;
 
 	/**
 	 * The CyWriterManager specified in the constructor.
 	 */
 	protected final T writerManager;
 
 	/**
 	 * Constructor.
 	 * @param writerManager The CyWriterManager to be used to determine which
 	 * {@link org.cytoscape.io.write.CyWriter} to be used to write the file chosen by the user. 
 	 */
 	public AbstractCyWriter(T writerManager) {
 		if (writerManager == null)
 			throw new NullPointerException("CyWriterManager is null");
 		this.writerManager = writerManager;
 
 		descriptionFilterMap = new TreeMap<String,CyFileFilter>();
 		for (CyFileFilter f : writerManager.getAvailableWriterFilters())
 			descriptionFilterMap.put(f.getDescription(), f);
 	}
 
 	/**
 	 * This method processes the chosen input file and output type and attempts
 	 * to write the file.
 	 * @param tm The {@link org.cytoscape.work.TaskMonitor} provided by the TaskManager execution environment.
 	 */
 	public final void run(final TaskMonitor tm) throws Exception {
 		if (outputFile == null)
			throw new NullPointerException("Output file has not ben specified!");
 
 		final String desc = getExportFileFormat();
 		if (desc == null)
 			throw new NullPointerException("No file type has been specified!");
 
 		final CyFileFilter filter = descriptionFilterMap.get(desc);
 		if (filter == null)
 			throw new NullPointerException("No file filter found for specified file type!");
 		
 		final CyWriter writer = getWriter(filter, outputFile); 
 		if (writer == null)
 			throw new NullPointerException("No CyWriter found for specified file type!");
 
 		insertTasksAfterCurrentTask(writer);
 	}
 
 	/**
 	 * Should return a {@link org.cytoscape.io.write.CyWriter} object for writing the specified file of the specified type.
 	 * @param filter The specific type of file to be written.
 	 * @param out The file that will be written.
 	 * @return a {@link org.cytoscape.io.write.CyWriter} object for writing the specified file of the specified type.
 	 * @throws Exception 
 	 */
 	protected abstract CyWriter getWriter(CyFileFilter filter, File out) throws Exception;
 
 }
