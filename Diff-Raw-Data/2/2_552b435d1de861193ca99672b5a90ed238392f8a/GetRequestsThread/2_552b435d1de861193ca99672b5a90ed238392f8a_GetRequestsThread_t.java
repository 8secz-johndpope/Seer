 /*
   GetRequestsThread.java / Frost
   Copyright (C) 2001  Jan-Thomas Czornack <jantho@users.sourceforge.net>
 
   This program is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public License as
   published by the Free Software Foundation; either version 2 of
   the License, or (at your option) any later version.
 
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
   General Public License for more details.
 
   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
 package frost.threads;
 
 import java.io.File;
 import java.util.*;
 
 import javax.swing.JTable;
 
 import frost.*;
 import frost.gui.model.UploadTableModel;
 import frost.gui.objects.FrostUploadItemObject;
 import frost.FcpTools.*;
 /**
  * Downloads file requests
  */
  
  
 public class GetRequestsThread extends Thread
 {
     static java.util.ResourceBundle LangRes = java.util.ResourceBundle.getBundle("res.LangRes");
 
     //public FrostBoardObject board;
     private int downloadHtl;
     private String keypool;
     private String destination;
     private String fileSeparator = System.getProperty("file.separator");
     private JTable uploadTable;
 
     /*public int getThreadType()
     {
         return BoardUpdateThread.BOARD_FILE_UPLOAD;
     }*/
 
     public void run()
     {
        // notifyThreadStarted(this);
         try {
 
         // Wait some random time to speed up the update of the TOF table
         // ... and to not to flood the node
         int waitTime = (int)(Math.random() * 5000); // wait a max. of 5 seconds between start of threads
         mixed.wait(waitTime);
 
         GregorianCalendar cal= new GregorianCalendar();
         cal.setTimeZone(TimeZone.getTimeZone("GMT"));
 
         String dirdate = DateFun.getDate();
 
         /*destination = new StringBuffer().append(keypool)
                         .append(board.getBoardFilename()).append(fileSeparator)
                         .append(dirdate).append(fileSeparator).toString();*/
 	
 	//yes mister spammer, this is a special for you!
 	destination = new StringBuffer().append("requests")
 			.append(fileSeparator)
 			.append(mixed.makeFilename(frame1.getMyId().getUniqueName())).toString();
 
         File makedir = new File(destination);
         if( !makedir.exists() )
         {
             Core.getOut().println(Thread.currentThread().getName()+ ": Creating directory: " + destination);
             makedir.mkdirs();
         }
 
         if( isInterrupted() )
         {
             //notifyThreadFinished(this);
             return;
         }
 	
 	//start the request loop
 	while (true) {
 	dirdate = DateFun.getDate();
 	mixed.wait(15*60*1000);
 	if (Core.getMyBatches().isEmpty()) continue; //do not start requesting until the user has shared something
 	try{
 	Iterator it = frame1.getMyBatches().keySet().iterator();
 	while (it.hasNext()) {
 	String currentBatch = (String)it.next();
         int index = 0;
         int failures = 0;
         int maxFailures = 3; // increased, skips now up to 3 request indicies (in case if gaps occured)
         while( failures < maxFailures )
         {
             String val = new StringBuffer().append(destination)
 	    				   .append(fileSeparator)
 					   .append(currentBatch)
                                            .append("-")
                                            .append(dirdate)
                                            .append("-")
                                            .append(index)
                                            .append(".req.sha").toString();
             File testMe = new File(val);
             boolean justDownloaded = false;
 
             // already downloaded ?
             if( testMe.length() > 0 )
             {
                 index++;
                 failures = 0;
 		continue;
             }
             else
             {
                 String tmp = new StringBuffer().append("GetRequestsThread.run, file = ")
                                         .append(testMe.getName())
                                         .append(", failures = ")
                                         .append(failures).toString();
                 Core.getOut().println( tmp );
 
                 FcpRequest.getFile("KSK@frost/request/" +
                                    frame1.frostSettings.getValue("messageBase") + "/" +
 				   frame1.getMyId().getUniqueName() + "-"+
 				   testMe.getName(),
                                    null,
                                    testMe,
                                    downloadHtl,
                                    false);
                 justDownloaded = true;
             }
 
             // Download successful?
             if( testMe.length() > 0 /* && justDownloaded */ )
             {
                 Core.getOut().println(Thread.currentThread().getName()+" Received request " + testMe.getName());
 
                 String content = (FileAccess.readFileRaw(testMe)).trim();
                 Core.getOut().println("Request content is " + content);
                 UploadTableModel tableModel = (UploadTableModel)uploadTable.getModel();
                 int rowCount = tableModel.getRowCount();
 
                 for( int i = 0; i < rowCount; i++ )
                 {
                     FrostUploadItemObject ulItem = (FrostUploadItemObject)tableModel.getRow(i);
                     String SHA1 = ulItem.getSHA1();
 		    if (SHA1==null) continue;
 		    else SHA1=SHA1.trim();
                     if( SHA1.equals(content) )
                     {
                         // FIXED: 1st: there is never such a file written (using SHA1 in name)
                         // are you sure?  I've seen it written several times --zab
                         //        2nd: is'nt it possible to use uploadItem.getLastUploadData for this?
                         // probably, this .lck thing is jantho's style 
                         File requestLock = new File(destination + SHA1 + ".lck");
                         if( !requestLock.exists() )
                         {
                            // FIXME: (IMPORTANT, ideas needed!) what to do if state is ENCODING ? In this case actually encoding runs,
                             // and the next state should be REQUESTED.
                             // maybe we need to set the REQUESTED as next state in insertThread ...
                             
                             // changing state ENCODING_REQUESTED to REQUESTED is ok!
                             if( ulItem.getState() != FrostUploadItemObject.STATE_UPLOADING &&
                                 ulItem.getState() != FrostUploadItemObject.STATE_PROGRESS &&
 				                ulItem.getBatch().equals(currentBatch)) //TOTHINK: this is optional
                             {
                                 Core.getOut().println("Request matches row " + i);
                                 ulItem.setState( FrostUploadItemObject.STATE_REQUESTED );
                                 tableModel.updateRow( ulItem );
                             }
                         }
                         else
                         {
                             Core.getOut().println("File with hash " + SHA1 + " was requested, but already uploaded today");
                         }
                     }
                 }
                 index++;
                 failures = 0;
             }
             else
             {
                 index++; // this now skips gaps in requests, but gives each download only 1 try
                 failures++;
             }
             if( isInterrupted() )
             {
                 break;
             }
         }
 
         }
 	
 	
 	}catch(ConcurrentModificationException e) {
 		continue;
 	}
 	}
 	} //people with nice ides can refactor :-P
         catch(Throwable t)
         {
             Core.getOut().println(Thread.currentThread().getName()+": Oo. EXCEPTION in GetRequestsThread:");
             t.printStackTrace(Core.getOut());
         }
 	
 	
         //notifyThreadFinished(this);
     }
 
     /**Constructor*/
     public GetRequestsThread(int dlHtl, String kpool, JTable uploadTable)
     {
         //super(boa);
         //this.board = boa;
         this.downloadHtl = dlHtl;
         this.keypool = kpool;
         this.uploadTable = uploadTable;
     }
 }
