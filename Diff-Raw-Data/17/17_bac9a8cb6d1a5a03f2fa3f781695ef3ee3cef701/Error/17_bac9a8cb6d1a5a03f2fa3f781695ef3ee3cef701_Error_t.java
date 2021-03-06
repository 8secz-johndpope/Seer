 package demo.element;
 
 import wcs.core.Log;
 import wcs.java.AssetSetup;
 import wcs.java.CSElement;
 import wcs.java.Element;
 import wcs.java.Env;
 import wcs.java.Picker;
 
 public class Error extends Element {
 
 	final static Log log = Log.getLog(Error.class);
 	
 	public static AssetSetup setup() {
 		return new CSElement("DmError", demo.element.Error.class);
 	}
 
 	@Override
 	public String apply(Env e) {
 		log.trace("Demo Error");
 		
		Picker html = Picker.load("/demo/page.html", "#header");
 		html.replace("#title", "Error");
 		html.replace("#message", e.getString("error"));
		return html/*.dump(log)*/.outerHtml();
 	}
 }
