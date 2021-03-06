 package de.ueller.midlet.gps.tile;
 /*
  * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
  * 			Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
  * See Copying
  */
 
 import java.io.DataInputStream;
 import java.io.EOFException;
 import java.io.IOException;
 import java.io.InputStream;
 
 import de.ueller.gps.data.SearchResult;
 import de.ueller.gpsMid.mapData.QueueReader;
 import de.ueller.midlet.gps.GpsMid;
 import de.ueller.midlet.gps.GuiSearch;
 import de.ueller.midlet.gps.Logger;
 import de.ueller.midlet.gps.Trace;
 import de.ueller.midlet.gps.names.Names;
 
 public class SearchNames implements Runnable{
 
 	private Thread processorThread;
 	private int foundEntries=0;
 	private boolean stopSearch=false;
 	private String search;
 	private final GuiSearch gui;
 	private boolean newSearch=false;
 	protected static final Logger logger = Logger.getInstance(SearchNames.class,Logger.TRACE);
 
 	public SearchNames(GuiSearch gui) {
 		super();
 		this.gui = gui;
 	}
 
 	public void run() {
 	
 	    try {
 		while (newSearch) {
 		    doSearch(search);
 		    // refresch display to give change to fetch the names
 		    for (int i=8;i!=0;i--){
 			try {
 			    synchronized (this) {
 				wait(300);						
 			    }
 			    if (stopSearch){
 					break;
 			    } else {
 					gui.triggerRepaint();
 			    }
 			} catch (InterruptedException e) {
 			}
 		    }
 		}
 	    } catch (OutOfMemoryError oome ) {
 		logger.fatal("SearchNames thread crashed as out of memory: " + oome.getMessage());
 		oome.printStackTrace();
 	    } catch (Exception e) {
 		logger.fatal("SearchNames thread crashed unexpectadly with error " +  e.getMessage());
 		e.printStackTrace();
 	    }		
 	}
 	
 	private void doSearch(String search) throws IOException {
 		try {
 			//#debug
 			System.out.println("search");
 			String fn=search.substring(0,2);
 			String compare=search.substring(2);
 			StringBuffer current=new StringBuffer();
 			synchronized(this) {
 				stopSearch=false;
 				if (newSearch){
 					gui.clearList();
 					newSearch=false;
 				}
 			}
 			String fileName = "/s"+fn+".d";
 //			System.out.println("open " +fileName);
			InputStream stream = GpsMid.getInstance().getConfig().getMapResource(fileName);
			if (stream == null){
				System.out.println("file not Found");
				return;
			}
 			DataInputStream ds=new DataInputStream(stream);
 			int pos=0;
 			int type = 0;
 			/**
 			 * InputStream.available doesn't seem to reliably give a correct value
 			 * depending on what type of InputStream we actually get.
 			 * Instead we will continue reading until we receive a EOFexpetion.
 			 */
 			while (true){
 				
 				if (stopSearch){
 					ds.close();					
 					return;
 				}
 				try {
 					type = ds.readByte();
 				} catch (EOFException eof) {
 					//Normal way of detecting the end of a file
 					ds.close();
 					return;
 				}
 				int sign=1;
 				if (type < 0){
 					type += 128;
 					sign=-1;
 				}
 //				System.out.println("type = " + type);
 				int entryType=(type & 0x60);
 				int delta=type & 0x9f;
 				delta *=sign;
 //				System.out.println("pos=" + pos + "  delta="+delta);
 				if (delta > Byte.MAX_VALUE)
 					delta -= Byte.MAX_VALUE;
 				pos+=delta;
 				current.setLength(pos);
 //				System.out.println("pos=" + pos + "  delta="+delta);
 				long value=0;
 				switch (entryType){
 				case 0:
 					value=ds.readByte();
 //					System.out.println("read byte");
 					break;
 				case 0x20:
 					value=ds.readShort();
 //					System.out.println("read short");
 					break;
 				case 0x40:
 					value=ds.readInt();
 //					System.out.println("read int");
 					break;
 				case 0x60:
 					value=ds.readLong();
 //					System.out.println("read long");
 					break;
 				}
 				current.append(""+value);
 //				System.out.println("test " + current);
 				int idx = Names.readNameIdx(ds);
 				if (!current.toString().startsWith(compare)){
 					idx=-1;
 				}
 				type=ds.readByte();
 				while (type != 0){					
 					if (stopSearch){
 						ds.close();						
 						return;
 					}
 					byte isInCount=ds.readByte();
 					int[] isInArray=null;
 					if (isInCount > 0 ){
 						isInArray=new int[isInCount];
 						for (int i=isInCount;i--!=0;){
 							isInArray[i]= Names.readNameIdx(ds);							
 						}
 					}
 					float lat=ds.readFloat();
 					float lon=ds.readFloat();
 					if (idx != -1){
 						SearchResult sr=new SearchResult();
 						sr.nameIdx=idx;
 						sr.type=(byte) type;
 						sr.lat=lat;
 						sr.lon=lon;
 						sr.nearBy=isInArray;
 						if (newSearch){
 							gui.clearList();
 							newSearch=false;
 						}
 						gui.addResult(sr);
 						foundEntries++;
 						if (foundEntries > 50)
 							return;			
 //						System.out.println("found " + current +"(" + shortIdx + ") type=" + type);
 					}
 					type=ds.readByte();
 				}
 			}			
 		} catch (NullPointerException e) {
 			logger.exception("Null pointer exception in SearchNames: ", e);			
 		}
 	}
 	
 	public void shutdown(){
 		stopSearch=true;
 	}
 
 	public synchronized void search(String search){
 		//#debug
 		logger.info("search for  " + search);
 		stopSearch=true;
 		newSearch=true;
 		foundEntries=0;
 		this.search=search;
 		if (processorThread == null || !processorThread.isAlive()) {
 			processorThread = new Thread(this);
 			processorThread.setPriority(Thread.MIN_PRIORITY+1);
 			processorThread.start();
 			logger.info("started search thread");
 		}		
 	}
 
 
 }
 
