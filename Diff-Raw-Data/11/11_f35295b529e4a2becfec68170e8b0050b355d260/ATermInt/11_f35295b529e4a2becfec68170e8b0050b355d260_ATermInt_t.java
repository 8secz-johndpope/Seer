 package org.spoofax.jsglr.shared.terms;
 
 import java.io.IOException;
 
 public class ATermInt extends ATerm {
 
 	private static final long serialVersionUID = 1L;
 	
 	private int value;
 	
 	ATermInt() {}
 
 	ATermInt(ATermFactory factory, int value) {
 		super(factory);
 		this.value = value;
 	}
 
 	@Override
 	public int getChildCount() {
 		return 0;
 	}
 
 	public int getInt() {
 		return value;
 	}
 
 	@Override
 	public ATerm getChildAt(int i) {
 		return null;
 	}
 
 	@Override
 	public int getType() {
 		return ATerm.INT;
 	}
 
 	@Override
 	public void writeTo(Appendable sb, int depth) throws IOException {
 		if(depth == 0) {
 			sb.append("...");
 		}
 		else {
 			sb.append(Integer.toString(value));
 		}
 	}
 
 	@Override
 	public boolean simpleMatch(ATerm t) {
 		if(!(t instanceof ATermInt))
 			return false;
 		ATermInt a = (ATermInt)t;
 		return a.value == value;
 	}
 
 	@Override
 	public int hashCode() {
 		return value;
 	}
 }
