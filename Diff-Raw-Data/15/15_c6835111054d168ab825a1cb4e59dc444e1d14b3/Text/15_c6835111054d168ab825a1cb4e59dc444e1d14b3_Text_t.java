 package haven;
 
 import java.awt.*;
 import java.awt.geom.Rectangle2D;
 import java.awt.image.BufferedImage;
 import java.util.*;
 
 public class Text {
     public static final Foundry std;
     public BufferedImage img;
     public final String text;
     private FontMetrics m;
     private Tex tex;
     public static final Color black = Color.BLACK;
     public static final Color white = Color.WHITE;
 	
     static {
 	std = new Foundry(new Font("SansSerif", Font.PLAIN, 10));
     }
 	
     public static int[] findspaces(String text) {
 	java.util.List<Integer> l = new ArrayList<Integer>();
 	for(int i = 0; i < text.length(); i++) {
 	    char c = text.charAt(i);
 	    if(Character.isWhitespace(c))
 		l.add(i);
 	}
 	int[] ret = new int[l.size()];
 	for(int i = 0; i < ret.length; i++)
 	    ret[i] = l.get(i);
 	return(ret);
     }
         
     public static class Foundry {
	private Graphics tmpl;
 	private FontMetrics m;
 	Font font;
 	Color defcol;
 	boolean aa = false;
 		
 	public Foundry(Font f, Color defcol) {
 	    font = f;
 	    this.defcol = defcol;
 	    BufferedImage junk = TexI.mkbuf(new Coord(10, 10));
	    tmpl = junk.getGraphics();
	    tmpl.setFont(f);
	    m = tmpl.getFontMetrics();
 	}
 		
 	public Foundry(Font f) {
 	    this(f, Color.WHITE);
 	}
 		
 	private Coord strsize(String text) {
	    Rectangle2D b = m.getStringBounds(text, tmpl);
 	    return(new Coord((int)b.getWidth(), (int)b.getHeight()));
 	}
                 
 	public Text renderwrap(String text, Color c, int width) {
 	    Text t = new Text(text);
 	    int y = 0;
 	    int[] sl = findspaces(text);
 	    int s = 0, e = 0, i = 0;
 	    java.util.List<String> lines = new LinkedList<String>();
 	    while(s < text.length()) {
 		do {
 		    int te;
 		    if(i < sl.length)
 			te = sl[i];
 		    else
 			te = text.length();
 		    Coord b = strsize(text.substring(s, te));
 		    if(b.x > width) {
 			break;
 		    } else {
 			e = te;
 			i++;
 		    }
 		} while(i <= sl.length);
 		String line = text.substring(s, e);
 		lines.add(line);
 		Coord b = strsize(line);
 		y += b.y;
 		s = e + 1;
 	    }
 	    t.img = TexI.mkbuf(new Coord(width, y));
 	    Graphics g = t.img.createGraphics();
 	    if(aa)
 		Utils.AA(g);
 	    g.setFont(font);
 	    g.setColor(c);
 	    t.m = g.getFontMetrics();
 	    y = 0;
 	    for(String line : lines) {
 		g.drawString(line, 0, y + t.m.getAscent());
 		Coord b = strsize(line);
 		y += b.y;
 	    }
 	    g.dispose();
 	    return(t);
 	}
                 
 	public Text renderwrap(String text, int width) {
 	    return(renderwrap(text, defcol, width));
 	}
                 
 	public Text render(String text, Color c) {
 	    Text t = new Text(text);
	    t.img = TexI.mkbuf(strsize(text));
 	    Graphics g = t.img.createGraphics();
 	    if(aa)
 		Utils.AA(g);
 	    g.setFont(font);
 	    g.setColor(c);
 	    t.m = g.getFontMetrics();
 	    g.drawString(text, 0, t.m.getAscent());
 	    g.dispose();
 	    return(t);
 	}
 		
 	public Text render(String text) {
 	    return(render(text, defcol));
 	}
                 
 	public Text renderf(String fmt, Object... args) {
 	    return(render(String.format(fmt, args)));
 	}
     }
 	
     private Text(String text) {
 	this.text = text;
     }
 	
     public Coord sz() {
 	return(Utils.imgsz(img));
     }
 	
     public Coord base() {
 	return(new Coord(0, m.getAscent()));
     }
 	
     public static Text render(String text, Color c) {
 	return(std.render(text, c));
     }
 	
     public static Text renderf(Color c, String text, Object... args) {
 	return(std.render(String.format(text, args), c));
     }
 	
     public static Text render(String text) {
 	return(render(text, Color.WHITE));
     }
 	
     public Tex tex() {
 	if(tex == null)
 	    tex = new TexI(img);
 	return(tex);
     }
 }
