 package org.bladerunnerjs.model.utility;
 
 import java.io.IOException;
 import java.io.Reader;
 import java.util.LinkedList;
 import java.util.List;
 
 import com.google.common.base.CharMatcher;
 import com.google.common.base.Predicate;
 
 public class Trie<T>
 {
 	private Predicate<Character> charMatcher = CharMatcher.JAVA_LETTER_OR_DIGIT.or(CharMatcher.is('.'));
 	private TrieNode<T> root = new TrieNode<T>();
 	
 	
 	public void add(String key, T value) throws EmptyTrieKeyException, TrieKeyAlreadyExistsException {
 		if (key.length() < 1)
 		{
 			throw new EmptyTrieKeyException();
 		}
 		
 		TrieNode<T> node = root;
 		
 		for( char character : key.toCharArray() )
 		{
 			node = node.getOrCreateNextNode( character );
 		}
 		
 		if (node.getValue() != null)
 		{
 			throw new TrieKeyAlreadyExistsException(key);
 		}
 		node.setValue(value);
 	}
 	
 	public boolean containsKey(String key) {
 		return (get(key) == null) ? false : true;
 	}
 	
 	public Object get(String key)
 	{
 		TrieNode<T> node = root;
 		
 		for( char character : key.toCharArray() )
 		{
 			node = node.getNextNode( character );
			
			if (node == null)
			{
				return null;
			}
 		}
 		
 		return node.getValue();
 	}
 	
 	public List<T> getMatches(Reader reader) throws IOException
 	{
 		List<T> matches = new LinkedList<T>();
 		
 		TrieMatcher matcher = new TrieMatcher();
 		
 		int latestCharVal;
 		while ((latestCharVal = reader.read()) != -1)
 		{
 			char latestChar = (char) latestCharVal;
 			processChar( matches, latestChar, matcher);
 		}
 		processChar( matches, '\n', matcher);
 		
 		return matches;	
 	}
 	
 	public void setCharMatcher(Predicate<Character> matcher)
 	{
 		charMatcher = matcher;
 	}
 	
 	private void processChar(List<T> matches, char nextChar, TrieMatcher matcher)
 	{
 		TrieNode<T> nextNode = matcher.next(nextChar);
 		
 		if (nextNode == null)
 		{
 			T matcherValue = matcher.previousNode.getValue();
 			if (matcherValue != null && !charMatcher.apply(nextChar))
 			{
 				matches.add(matcherValue);
 			}
 			matcher.reset();
 		}
 		
 		if (!CharMatcher.forPredicate(charMatcher).apply( matcher.currentNode.getNodeChar() ))
 		{
 			matcher.reset();
 		}
 		
 		return;
 	}
 
 
 	private class TrieMatcher {
 		TrieNode<T> currentNode;
 		TrieNode<T> previousNode;
 		
 		TrieMatcher()
 		{
 			reset();
 		}
 		
 		TrieNode<T> next(char nextChar)
 		{
 			previousNode = currentNode;
 			currentNode = currentNode.getNextNode(nextChar);
 			return currentNode;
 		}
 		
 		void reset()
 		{
 			currentNode = root;
 			previousNode = null;
 		}
 	}
 }
