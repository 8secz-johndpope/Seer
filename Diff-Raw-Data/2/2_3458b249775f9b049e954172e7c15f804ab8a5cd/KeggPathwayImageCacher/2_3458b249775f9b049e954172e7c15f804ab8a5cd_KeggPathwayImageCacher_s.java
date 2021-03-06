 package org.caleydo.core.application.helper.cacher.kegg;
 
 import java.io.File;
 import java.io.FileNotFoundException;
 import java.util.ArrayList;
 import org.apache.log4j.Level;
 import org.apache.log4j.Logger;
 import org.caleydo.core.application.helper.PathwayListGenerator;
 import org.caleydo.core.command.system.CmdFetchPathwayData;
 import org.caleydo.core.util.exception.CaleydoRuntimeException;
 import org.caleydo.core.util.exception.CaleydoRuntimeExceptionType;
 import org.eclipse.swt.widgets.Display;
 import org.eclipse.swt.widgets.ProgressBar;
 import com.enterprisedt.net.ftp.FTPConnectMode;
 import com.enterprisedt.net.ftp.FTPFile;
 import com.enterprisedt.net.ftp.FileTransferClient;
 
 /**
  * Fetch tool for KEGG image files.
  * 
  * @author Marc Streit
  * 
  */
 public class KeggPathwayImageCacher
 	extends Thread
 {
 	private static final int EXPECTED_DOWNLOADS = 214;
	private static final int MAX_FTP_CONNECTIONS = 5;
 	
 	/**
 	 * Needed for async access to set progress bar state
 	 */
 	private Display display;
 
 	private ProgressBar progressBar;
 
 	private CmdFetchPathwayData triggeringCommand;
 
 	private Integer iConcurrentFtpConnections = 0;
 
 	private int iDownloadCount = 0;
 
 	/**
 	 * Constructor.
 	 */
 	public KeggPathwayImageCacher(final Display display, final ProgressBar progressBar,
 			final CmdFetchPathwayData triggeringCommand)
 	{
 		this.display = display;
 		this.progressBar = progressBar;
 		this.triggeringCommand = triggeringCommand;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * @see java.lang.Thread#run()
 	 */
 	@Override
 	public void run()
 	{
 		String sServerName = "ftp.genome.ad.jp";
 		String sDirName = "/pub/kegg/pathway/organisms/hsa/";
 
 		// set up logger so that we get some output
 		Logger log = Logger.getLogger(KeggPathwayImageCacher.class);
 		log.setLevel(Level.INFO);
 
 		// Create KEGG folder in .caleydo
 		new File(System.getProperty("user.home") + "/.caleydo/kegg").mkdir();
 
 		FileTransferClient ftp = null;
 
 		try
 		{
 			// create client
 			log.info("Creating FTP client");
 			ftp = new FileTransferClient();
 			ftp.getAdvancedFTPSettings().setConnectMode(FTPConnectMode.PASV);
 
 			// set remote host
 			ftp.setRemoteHost(sServerName);
 			ftp.setUserName("anonymous");
 			ftp.setPassword("");
 
 			// connect to the server
 			log.info("Connecting to server " + sServerName);
 			ftp.connect();
 			log.info("Connected and logged in to server " + sServerName);
 
 			// ftp.changeDirectory(sDirName);
 
 			log.info("Getting current directory listing");
 			FTPFile[] files = ftp.directoryList(sDirName);
 			ftp.disconnect();
 
 			ArrayList<KeggSinglePathwayImageCacherThread> threadContainer = new ArrayList<KeggSinglePathwayImageCacherThread>();
 
 			String sTmpFileName = "";
 			int iPatternIndex = 0;
 
 			final int iFilesToDownload = files.length;
 
 			for (int iFileCount = 0; iFileCount < iFilesToDownload; iFileCount++)
 			{
 				if (iConcurrentFtpConnections > MAX_FTP_CONNECTIONS)
 				{
 					Thread.sleep(500);
 					iFileCount--;
 					continue;
 				}
 
 				sTmpFileName = files[iFileCount].toString();
 
 				// Download only image files
 				if (sTmpFileName.contains(".gif"))
 				{
 					iPatternIndex = sTmpFileName.indexOf(".gif");
 					sTmpFileName = sTmpFileName
 							.substring(iPatternIndex - 8, iPatternIndex + 4);
 
 					threadContainer.add(new KeggSinglePathwayImageCacherThread(this,
 							sTmpFileName, sDirName));
 					
 					iConcurrentFtpConnections++;
 
 					iDownloadCount++;
 
 					display.asyncExec(new Runnable()
 					{
 						public void run()
 						{
 							if (progressBar.isDisposed())
 								return;
 							
 							progressBar.setSelection((iDownloadCount * 100 / EXPECTED_DOWNLOADS));
 						}
 					});
 				}
 			}
 
 		}
 		catch (Exception e)
 		{
 			e.printStackTrace();
 		}
 
 		if (triggeringCommand != null)
 			triggeringCommand.setFinishedKeggImageCacher();
 
 		triggerPathwayListGeneration();
 	}
 
 	public void threadFinishNotification()
 	{
 		iConcurrentFtpConnections--;
 	}
 	
 	private void triggerPathwayListGeneration()
 	{
 		// Trigger pathway list generation
 		PathwayListGenerator pathwayListLoader = new PathwayListGenerator();
 
 		try
 		{
 			pathwayListLoader.run(PathwayListGenerator.INPUT_FOLDER_PATH_KEGG,
 					PathwayListGenerator.INPUT_IMAGE_PATH_KEGG,
 					PathwayListGenerator.OUTPUT_FILE_NAME_KEGG);
 		}
 		catch (FileNotFoundException fnfe)
 		{
 			throw new CaleydoRuntimeException("Cannot generate pathway list.",
 					CaleydoRuntimeExceptionType.DATAHANDLING);
 		}
 	}
 }
