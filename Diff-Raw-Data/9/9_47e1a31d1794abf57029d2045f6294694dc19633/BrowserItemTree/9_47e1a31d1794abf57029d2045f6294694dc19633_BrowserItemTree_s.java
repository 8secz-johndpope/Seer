 /*
  * Copyright (C) 2010 Geometer Plus <contact@geometerplus.com>
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
  * 02110-1301, USA.
  */
 
 package org.geometerplus.android.fbreader.network;
 
import org.geometerplus.zlibrary.core.filesystem.ZLResourceFile;
import org.geometerplus.zlibrary.core.image.ZLFileImage;
 import org.geometerplus.zlibrary.core.image.ZLImage;
 import org.geometerplus.zlibrary.core.resources.ZLResource;
 
 import org.geometerplus.fbreader.network.*;
 
 public class BrowserItemTree extends NetworkTree {
 
 	public BrowserItemTree() {
 		super(1);
 	}
 
 	@Override
 	public String getName() {
 		return ZLResource.resource("networkView").getResource("browser").getValue();
 	}
 
 	@Override
 	public String getSummary() {
 		return ZLResource.resource("networkView").getResource("browserSummary").getValue();
 	}
 
 	@Override
 	protected ZLImage createCover() {
		ZLResourceFile file = ZLResourceFile.createResourceFile("data/browser.png");
		return new ZLFileImage("image/png", file);
 	}
 
 	@Override
 	public NetworkLibraryItem getHoldedItem() {
 		return null;
 	}
 }
