 /*
  * PS3 Media Server, for streaming any medias to your PS3.
  * Copyright (C) 2008  A.Brochard
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU General Public License
  * as published by the Free Software Foundation; version 2
  * of the License only.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
  */
 package net.pms.io;
 
 import java.io.File;
 
 import net.pms.PMS;
 import net.pms.configuration.PmsConfiguration;
 
 public class OutputParams {
 	
 	public File outputFile;
 	public double minFileSize;
 	public double minBufferSize;
 	public double maxBufferSize;
 	public double timeseek;
 	public double timeend;
 	public int fromFrame;
 	public int toFrame;
 	public int waitbeforestart;
 	public PipeProcess input_pipes [] = new PipeProcess [2];
 	public PipeProcess output_pipes [] = new PipeProcess [2];
 	public int aid = -1;
 	public int sid = -1;
 	public int secondread_minsize;
 	public boolean noexitcheck;
 	public boolean log;
 	public boolean lossyaudio;
 	public boolean losslessaudio;
 	public boolean no_videoencode;
 	public String forceFps;
 	public String forceType;
 	public int mediaRenderer;
 	
 	public OutputParams(PmsConfiguration configuration) {
 		waitbeforestart = 6000;
 		fromFrame = -1;
 		toFrame = -1;
 		secondread_minsize = 1000000;
 		minFileSize = PMS.getConfiguration().getMinStreamBuffer();
 		minBufferSize = configuration.getMinMemoryBufferSize();
 		maxBufferSize = configuration.getMaxMemoryBufferSize();
 		timeseek = 0;
 		outputFile = null;
 	}
 
 }
