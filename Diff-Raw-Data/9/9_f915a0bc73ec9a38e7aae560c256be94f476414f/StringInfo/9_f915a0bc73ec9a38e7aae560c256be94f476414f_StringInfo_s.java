 /*
  * Created on Dec 15, 2004
  *
  * TODO To change the template for this generated file go to
  * Window - Preferences - Java - Code Style - Code Templates
  */
 
 package com.jopdesign.build;
 import java.io.PrintWriter;
 import java.util.*;
 
 /**
  * @author Falvius
  *
  */
 public class StringInfo {
 	
 	// two handles a two word plus reference to char[]
 	final static int STR_OBJ_LEN = 2+2+1;
 
 	public static HashMap usedStrings = new HashMap();
 	public static List list = new LinkedList();
 	public static ClassInfo cli;
 	public static int stringTableAddress = -1;
 	public static int length = 0;
 
 	/**
 	 * relative address to start of the String table
 	 */
 	public int startAddress;
 	public String string;
 	
 	public static void addString(String s) {
 		if(usedStrings.containsKey(s)) return;
 		// System.err.println("// Constant String: " + s);
 		StringInfo si = new StringInfo(s, length);
 		usedStrings.put(s, si);
 		list.add(si);
 		
 		length += STR_OBJ_LEN+s.length();
 	}
 	public static StringInfo getStringInfo(String s) {
 		StringInfo si = (StringInfo) usedStrings.get(s);
 		return si;
 	}
 	
 	/**
 	 * Get the address of the String object.
 	 * Internal startAddress points to the the object (= first field).
 	 * @return
 	 */
 	public int getAddress() {
 		return startAddress;
 	}
 
 	
 	public StringInfo(String s, int addr) {
 		string = s;
 		startAddress = addr;
 	}
 	
 	
 	private final static int maxCom = 20;
 	
 	public String getSaveString() {
 		
 		StringBuffer sb = new StringBuffer("\"");
 		for (int i=0; i<string.length() && i<maxCom; ++i) {
 			char ch = string.charAt(i);
 			if (ch<' ' || ch>'~') {
 				sb.append('\\');
 				if (ch=='\r') sb.append("r");
 				else if (ch=='\n') sb.append("n");
 				else {
 					sb.append('0');
 					sb.append(Integer.toOctalString((int) ch));
 				}			
 			} else {
 				sb.append(ch);
 			}
 		}
 		if (string.length() > maxCom) { 
 			sb.append("...");
 		}
 		sb.append("\"");
 		
 		return sb.toString();
 	}
 
 //	private void commentary(String s, int addrCnt, CCodeWriter out ) {
 	private void commentary(String s, int addrCnt, PrintWriter out) {
 		out.println("\t//\t"+addrCnt+"\t"+getSaveString());
 	}
 	
 	public void dump(PrintWriter out, ClassInfo strcli, int arrygcinfo) {
 
 		int addr = stringTableAddress+startAddress;
 		commentary(string, addr, out);
 		out.println("\t"+(addr+4)+",\t//\tString handle points to the first field");
 		out.println("\t"+strcli.methodsAddress+",\t//\t pointer to String mtab ");
 		out.println("\t"+(addr+5)+",\t//\tchar[] handle points to the first element");
 		out.println("\t"+string.length()+",\t// array length in the handle");
 		out.println("\t"+(addr+2)+",\t//\tchar ref. points to char[] handle");
 			
		byte chrsp[] = string.getBytes();
 		out.print("\t");
 		for(int i=0;i<chrsp.length;i++) {
 			out.print(chrsp[i]+", ");
 			if ((i&0x07)==7) {
 				out.println();
 				out.print("\t");
 			}
 		}
 		out.println();
 
 	}
 }
