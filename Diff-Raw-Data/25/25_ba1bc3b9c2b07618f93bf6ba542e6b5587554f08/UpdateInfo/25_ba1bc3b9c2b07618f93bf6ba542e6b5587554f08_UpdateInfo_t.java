 /*
  * JF Updater: Auto-updater for modified Android OS
  *
  * Copyright (c) 2009 Sergi Vélez
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful, but
  * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
  * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License along
  * with this program; if not, write to the Free Software Foundation, Inc.,
  * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
  */
 
 package cmupdater.service;
 
 import java.io.Serializable;
 import java.net.URI;
 import java.util.List;
 
 public class UpdateInfo implements Serializable {
 
 	private static final long serialVersionUID = 8671456102755862106L;
 	
 	public boolean needsWipe;
 	public String mod;
 	public String name;
 	public String displayVersion;
 	public String type;
 	public String branchCode;
 	public String description;
 	public String md5;
	public String fileName;
 	
 	public List<URI> updateFileUris;
 	@Override
 	public String toString() {
 		return name;
 	}
 }
