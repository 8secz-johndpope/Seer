 /*
  * Copyright 2013 Hannes Janetzek
  *
  * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
  *
  * This program is free software: you can redistribute it and/or modify it under the
  * terms of the GNU Lesser General Public License as published by the Free Software
  * Foundation, either version 3 of the License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful, but WITHOUT ANY
  * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public License along with
  * this program. If not, see <http://www.gnu.org/licenses/>.
  */
 package org.oscim.gdx;
 
 import org.oscim.android.canvas.AndroidGraphics;
 import org.oscim.backend.GL20;
 import org.oscim.backend.GLAdapter;
 import org.oscim.core.Tile;
 import org.oscim.tiling.TileSource;
 import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;
 
 import android.os.Bundle;
 
 import com.badlogic.gdx.backends.android.AndroidApplication;
 import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
 import com.badlogic.gdx.backends.android.AndroidGL20;
 import com.badlogic.gdx.utils.SharedLibraryLoader;
 
 public class MainActivity extends AndroidApplication {
 
 	private final class AndroidGLAdapter extends AndroidGL20 implements GL20 {
 	}
 
 	@Override
 	public void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 
 		AndroidGraphics.init();
		GdxAssets.init("");
 		GLAdapter.init(new AndroidGLAdapter());
 		Tile.SIZE = 400;
 
 		AndroidApplicationConfiguration cfg = new AndroidApplicationConfiguration();
 
 		new SharedLibraryLoader().load("vtm-jni");
 
 		initialize(new GdxMapAndroid(), cfg);
 	}
 
 	class GdxMapAndroid extends GdxMap {
 		@Override
 		public void createLayers() {
 			TileSource ts = new OSciMap4TileSource();
 			initDefaultLayers(ts, true, true, true);
 		}
 	}
 }
