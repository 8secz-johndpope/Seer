 /* This file is part of the Joshua Machine Translation System.
  * 
  * Joshua is free software; you can redistribute it and/or modify
  * it under the terms of the GNU Lesser General Public License as
  * published by the Free Software Foundation; either version 2.1
  * of the License, or (at your option) any later version.
  *
  * This library is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this library; if not, write to the Free
  * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
  * MA 02111-1307 USA
  */
 package joshua.corpus;
 
 /**
  * Partial basic implementation of a symbol table.
  * 
  * @author Lane Schwartz
  * @author Zhifei Li
  * @version $LastChangedDate$
  */
 public abstract class AbstractSymbolTable implements SymbolTable {
 
 	
 	final public int[] addTerminals(String sentence){
 		return addTerminals(sentence.split("\\s+"));
 	}	
 	
     final public int[] addTerminals(String[] words){
 		int[] res =new int[words.length];
 		for(int t=0; t<words.length; t++)
 			res[t]=addTerminal(words[t]);
 		return res;
 	}	
     
 	final public int getTargetNonterminalIndex(int id) {
 		if (! isNonterminal(id)) {
 			return -1;
 		} else {
 			// TODO: get rid of this expensive interim object
 			String symbol = getWord(id);
 			
			return getTargetNonterminalIndex(symbol);
 		}
 	}
 	
	final public int getTargetNonterminalIndex(String wrd) {
 		// Assumes the last character is a digit
 		// and extracts it, starting from one.
 		// Assumes the whole prefix is the
 		// nonterminal-ID portion of the string
 		return Integer.parseInt( wrd.substring(wrd.length() - 2,	wrd.length() - 1) ) - 1;
 	}
 
 }
