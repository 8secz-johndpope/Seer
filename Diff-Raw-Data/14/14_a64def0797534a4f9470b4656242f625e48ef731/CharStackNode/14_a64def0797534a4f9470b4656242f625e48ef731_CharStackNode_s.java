 package org.rascalmpl.parser.sgll.stack;
 
 import org.rascalmpl.parser.sgll.result.AbstractNode;
 import org.rascalmpl.parser.sgll.result.CharNode;
 import org.rascalmpl.parser.sgll.result.ContainerNode;
 import org.rascalmpl.parser.sgll.result.struct.Link;
 import org.rascalmpl.parser.sgll.util.ArrayList;
 
 public final class CharStackNode extends AbstractStackNode implements IReducableStackNode{
 	private final char[][] ranges;
 	
 	private AbstractNode result;
 	
 	public CharStackNode(int id, char[][] ranges){
 		super(id);
 
 		this.ranges = ranges;
 	}
 	
 	private CharStackNode(CharStackNode original){
 		super(original);
 		
 		ranges = original.ranges;
 	}
 	
 	private CharStackNode(CharStackNode original, ArrayList<Link>[] prefixes){
 		super(original, prefixes);
 		
 		ranges = original.ranges;
 	}
 	
 	public String getName(){
 		throw new UnsupportedOperationException();
 	}
 	
 	public boolean reduce(char[] input){
 		char next = input[startLocation];
 		for(int i = ranges.length - 1; i >= 0; --i){
 			char[] range = ranges[i];
 			if(next >= range[0] && next <= range[1]){
 				result = new CharNode(next);
 				return true;
 			}
 		}
 		
 		return false;
 	}
 	
 	public boolean reduceWithoutResult(char[] input, int location){
 		char next = input[location];
 		for(int i = ranges.length - 1; i >= 0; --i){
 			char[] range = ranges[i];
 			if(next >= range[0] && next <= range[1]){
 				return true;
 			}
 		}
 		
 		return false;
 	}
 	
 	public boolean isClean(){
 		return true;
 	}
 	
 	public AbstractStackNode getCleanCopy(){
 		return new CharStackNode(this);
 	}
 
 	public AbstractStackNode getCleanCopyWithPrefix(){
 		return new CharStackNode(this, prefixesMap);
 	}
 	
 	public void setResultStore(ContainerNode resultStore){
 		throw new UnsupportedOperationException();
 	}
 	
 	public ContainerNode getResultStore(){
 		throw new UnsupportedOperationException();
 	}
 	
 	public int getLength(){
 		return 1;
 	}
 	
 	public AbstractStackNode[] getChildren(){
 		throw new UnsupportedOperationException();
 	}
 	
 	public AbstractNode getResult(){
 		return result;
 	}
 	
 	public String toString(){
 		StringBuilder sb = new StringBuilder();
 		
 		sb.append('[');
		for(int i = ranges.length - 1; i >= 0; --i){
			char[] range = ranges[i];
			sb.append(range[0]);
 			sb.append('-');
			sb.append(range[1]);
 		}
 		sb.append(']');
 		
 		sb.append(getId());
 		sb.append('(');
 		sb.append(startLocation);
 		sb.append(',');
 		sb.append(startLocation + getLength());
 		sb.append(')');
 		
 		return sb.toString();
 	}
 }
