 /*
  *  This file is part of the Haven & Hearth game client.
  *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
  *                     Björn Johannessen <johannessen.bjorn@gmail.com>
  *
  *  Redistribution and/or modification of this file is subject to the
  *  terms of the GNU Lesser General Public License, version 3, as
  *  published by the Free Software Foundation.
  *
  *  This program is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *  GNU General Public License for more details.
  *
  *  Other parts of this source tree adhere to other copying
  *  rights. Please see the file `COPYING' in the root directory of the
  *  source tree for details.
  *
  *  A copy the GNU Lesser General Public License is distributed along
  *  with the source tree of which this file is a part in the file
  *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
  *  Software Foundation's website at <http://www.fsf.org/>, or write
  *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  *  Boston, MA 02111-1307 USA
  */
 
 package haven;
 
 import java.util.*;
 import java.awt.font.TextAttribute;
 
 public class OptWnd extends Window {
     public final Panel main;
     public Panel current;
 
     public void chpanel(Panel p) {
 	if(current != null)
 	    current.hide();
 	(current = p).show();
 	pack();
     }
 
     public class PButton extends Button {
 	public final Panel tgt;
 	public final int key;
 
 	public PButton(Coord c, int w, Widget parent, String title, int key, Panel tgt) {
 	    super(c, w, parent, title);
 	    this.tgt = tgt;
 	    this.key = key;
 	}
 
 	public void click() {
 	    chpanel(tgt);
 	}
 
 	public boolean type(char key, java.awt.event.KeyEvent ev) {
 	    if((this.key != -1) && (key == this.key)) {
 		click();
 		return(true);
 	    }
 	    return(false);
 	}
     }
 
     public class Panel extends Widget {
 	public Panel(Coord sz) {
 	    super(Coord.z, sz, OptWnd.this);
 	    visible = false;
 	}
     }
 
     public class VideoPanel extends Panel {
 	public VideoPanel(Panel back) {
 	    super(new Coord(200, 200));
 	    new PButton(new Coord(0, 180), 200, this, "Back", 27, back);
 	}
 
 	public class CPanel extends Widget {
 	    public final GLConfig cf;
 
 	    public CPanel(GLConfig gcf) {
 		super(Coord.z, new Coord(200, 175), VideoPanel.this);
 		this.cf = gcf;
 		int y = 0;
 		new CheckBox(new Coord(0, y), this, "Render shadows") {
 		    {a = (cf.deflight == Light.pslights);}
 
 		    public void changed(boolean val) {
 			if(val) {
 			    if(cf.shuse) {
 				cf.deflight = Light.pslights;
 			    } else {
 				getparent(GameUI.class).error("Shadow rendering requires a shader compatible video card.");
 				a = false;
 			    }
 			} else {
 			    cf.deflight = Light.vlights;
 			}
 		    }
 		};
 		y += 20;
 		new CheckBox(new Coord(0, y), this, "Antialiasing") {
 		    {a = cf.fsaa;}
 
 		    public void changed(boolean val) {
 			if(val && !cf.havefsaa()) {
 			    getparent(GameUI.class).error("Your video card does not support antialiasing.");
 			    a = false;
 			    return;
 			}
 			cf.fsaa = val;
 		    }
 		};
 	    }
 	}
 
 	private CPanel curcf = null;
 	public void draw(GOut g) {
 	    if((curcf == null) || (g.gc != curcf.cf)) {
 		if(curcf != null)
 		    curcf.destroy();
 		curcf = new CPanel(g.gc);
 	    }
 	    super.draw(g);
 	}
     }
 
     public OptWnd(Coord c, Widget parent) {
 	super(c, Coord.z, parent, "Options");
 	main = new Panel(new Coord(200, 200));
 	Panel video = new VideoPanel(main);
 	Panel audio = new Panel(new Coord(200, 200));
 	int y;
 
 	new PButton(new Coord(0, 0), 200, main, "Video settings", 'v', video);
 	new PButton(new Coord(0, 30), 200, main, "Audio settings", 'a', audio);
 	new Button(new Coord(0, 180), 200, main, "Log out") {
 	    public void click() {
 		getparent(GameUI.class).act("lo");
 	    }
 	};
 
 	y = 0;
 	new Label(new Coord(0, y), audio, "Audio volume");
 	y += 20;
 	new HSlider(new Coord(0, y), 200, audio, 0, 1000, (int)(Audio.volume * 1000)) {
 	    public void changed() {
 		Audio.setvolume(val / 1000.0);
 	    }
 	};
 	new PButton(new Coord(0, 180), 200, audio, "Back", 27, main);
 
 	chpanel(main);
     }
 
     public void wdgmsg(Widget sender, String msg, Object... args) {
	if((sender == this) && (msg == "close")) {
 	    hide();
	} else {
	    super.wdgmsg(sender, msg, args);
	}
     }
 
     public void show() {
 	chpanel(main);
 	super.show();
     }
 }
