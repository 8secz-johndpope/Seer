 package templates.util;
 
 import java.io.UnsupportedEncodingException;
 import java.net.URLEncoder;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Set;
 
 import org.eclipse.xtext.xdoc.xdoc.Code;
 import org.eclipse.xtext.xdoc.xdoc.CodeBlock;
 import org.eclipse.xtext.xdoc.xdoc.LangDef;
 import org.eclipse.xtext.xdoc.xdoc.Link;
 
 public class StringFormatter {
 	
 	@SuppressWarnings("unused")
 	static private HashMap<String, Set<String>> languages = new HashMap<String, Set<String>>();
 	
 	static private Set<String> links = new HashSet<String>();
 	
 	static public final CodeBlock removeIndent(CodeBlock cb) {
 		if(cb.getContents().get(0) instanceof Code){
 			String code = ((Code)cb.getContents().get(0)).getContents();
 			int indent = code.length();
 			indent -= code.replaceAll("^(\n*)\\s*", "$1").length();
 			String string = "\n\\s{"+indent+"}";
 			for(int i = 0; i < cb.getContents().size(); i++) {
 				if (cb.getContents().get(i) instanceof Code) {
 					code = ((Code) cb.getContents().get(i)).getContents();
 					code = code.replaceAll(string, "\n");
 					if(i == 0) {
 						code = code.replaceAll("^\n*", "");
 					} else if(i == cb.getContents().size() - 1){
 						code = code.replaceAll("\n*$", "");
 					}
 					((Code)cb.getContents().get(i)).setContents(code);
 				}
 			}
 		}
 		return cb;
 	}
 
 	static public String encode(String s){
 		try {
 			return URLEncoder.encode(s, "ISO-8859-1");
 		} catch (UnsupportedEncodingException e) {
 			e.printStackTrace();
 			return "";
 		}
 	}
 	
 	static public void storeLink(Link link){
 		links.add(link.getUrl());
 	}
 	
 	static public Set<String> getStoredLinks(){
 		return links;
 	}
 
 	/**
 	 * Highlight keywords in a text.
 	 * 
 	 * @param text a piece of source code
 	 * @param dirName a directory to read a language file from
 	 * @param langName the name of the language
 	 * @return the string with keywords highlighted
 	 */
 	static public String highlightKeywords(String text, final LangDef lang){
 		if(lang != null){
 			List<String> keywords = lang.getKeywords();
 			for (String keyword : keywords) {
 				if(keyword.trim().equals("class")){
 					text = text.replaceAll("(?<!<span )" + makePattern(keyword.trim()) + "(?!\\=\"keyword\">)",
 							"<span class=\"keyword\">" + keyword.trim() + "</span>");
 				} else if(keyword.trim().equals("span")){
 					text = text.replaceAll("((?<!<)" + makePattern(keyword.trim()) + "(?!class\\=\"keyword\">)|(?<!</)span(?!>))",
 							"<span class=\"keyword\">" + keyword.trim() + "</span>");
 				} else if(keyword.trim().equals("keyword")){
 					text = text.replaceAll("(?<!<span class=\")" + makePattern(keyword.trim()) + "(?!\">)",
 							"<span class=\"keyword\">" + keyword.trim() + "</span>");
 				} else {
 					text = text.replaceAll(makePattern(keyword.trim()), "<span class=\"keyword\">" + keyword.trim() + "</span>");
 				}
 			}
 		}
 		return text;
 	}
 
 	public static String percentToFloat(String percent){
 		return (Float.parseFloat(percent.replaceAll("%", ""))/100) +"";
 	}
 
 	private static String makePattern(String keyword) {
		return "(?<![\\w\\^])"+keyword+"(?!\\w)";
 	}
 }
