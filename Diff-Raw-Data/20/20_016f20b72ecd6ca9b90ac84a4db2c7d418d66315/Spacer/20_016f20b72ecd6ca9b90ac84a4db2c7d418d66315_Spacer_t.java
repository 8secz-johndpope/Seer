 /*
  * Scriptographer
  * 
  * This file is part of Scriptographer, a Plugin for Adobe Illustrator.
  * 
  * Copyright (c) 2002-2006 Juerg Lehni, http://www.scratchdisk.com.
  * All rights reserved.
  *
  * Please visit http://scriptographer.com/ for updates and contact.
  * 
  * -- GPL LICENSE NOTICE --
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU General Public License
  * as published by the Free Software Foundation; either version 2
  * of the License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
  * -- GPL LICENSE NOTICE --
  * 
  * File created on 06.03.2005.
  * 
  * $RCSfile: Spacer.java,v $
  * $Author: lehni $
 * $Revision: 1.7 $
 * $Date: 2006/12/28 21:05:26 $
  */
 
 package com.scriptographer.adm;
 
 import java.awt.*;
 
 import com.scriptographer.js.Unsealed;
 
 public class Spacer extends Item implements Unsealed {
 
	private boolean visible;

 	public Spacer(int width, int height) {
 		bounds = new Rectangle(0, 0, width, height);
		visible = true;
 	}
 
 	public Spacer(Dimension size) {
 		this(size.width, size.height);
 	}
 
 	public Dimension getPreferredSize() {
 		return bounds.getSize();
 	}
 	
 	public void setBounds(int x, int y, int width, int height) {
 		bounds.setBounds(x, y, width, height);
 	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	
	
 }
