 package org.werti.uima.enhancer;
 
 import java.util.Arrays;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Stack;
 
 import org.apache.uima.UimaContext;
 import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
 import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
 import org.apache.uima.cas.FSIterator;
 import org.apache.uima.jcas.JCas;
 import org.apache.uima.resource.ResourceInitializationException;
 import org.werti.uima.types.Enhancement;
 import org.werti.uima.types.annot.CGReading;
 import org.werti.uima.types.annot.CGToken;
 import org.werti.util.EnhancerUtils;
 import org.werti.util.StringListIterable;
 
 public class Vislcg3Enhancer extends JCasAnnotator_ImplBase {
 
 	private List<String> chunkTags;
 	private static String CHUNK_BEGIN_SUFFIX = "-B";
 	private static String CHUNK_INSIDE_SUFFIX = "-I";
 	
 	@Override
 	public void initialize(UimaContext context)
 			throws ResourceInitializationException {
 		super.initialize(context);
 		chunkTags = Arrays.asList(((String)context.getConfigParameterValue("chunkTags")).split(","));
 	}
 
 	@Override
 	public void process(JCas cas) throws AnalysisEngineProcessException {
 		FSIterator cgTokenIter = cas.getAnnotationIndex(CGToken.type).iterator();
 		// stack for started enhancements
 		Stack<Enhancement> enhancements = new Stack<Enhancement>();
 		// keep track of ids for each annotation class
 		HashMap<String, Integer> classCounts = new HashMap<String, Integer>();
 		for (String chunkT : chunkTags) {
 			classCounts.put(chunkT, 0);
 		}
 		
 		// go through tokens
 		while (cgTokenIter.hasNext()) {
 			CGToken cgt = (CGToken) cgTokenIter.next();
 			// more than one reading? don't markup!
 			if (!is_Safe(cgt)) {
 				continue;
 			}
 			
 			// analyze reading
 			CGReading reading = cgt.getReadings(0);
 			for (String chunkT : classCounts.keySet()) {
 				// case 1: chunk start tag
 				if (containsTag(reading, chunkT + CHUNK_BEGIN_SUFFIX)) {
 					// make new enhancement
 					Enhancement e = new Enhancement(cas);
 					e.setBegin(cgt.getBegin());
 					// increment id
 					int newId = classCounts.get(chunkT) + 1;
 					e.setEnhanceStart("<span id=\"" + EnhancerUtils.get_id("WERTi-span" + chunkT, newId) + "\">");
 					classCounts.put(chunkT, newId);
 					// push onto stack
 					enhancements.push(e);
 				// case 2: started enhancement but current reading doesn't have a chunk inside tag
 				} else if (!enhancements.empty() && enhancements.peek().getEnhanceStart().contains(chunkT)
 						&& !containsTag(reading, chunkT + CHUNK_INSIDE_SUFFIX)) {
 					// finish enhancement
 					Enhancement e = enhancements.pop();
					e.setEnd(cgt.getEnd());
 					e.setEnhanceEnd("</span>");
 					// update CAS
 					cas.addFsToIndexes(e);
 				}
 			}
 		}
 	}
 	
 	/*
 	 * Determines whether the given token is safe, i.e. unambiguous
 	 */
 	private boolean is_Safe(CGToken t) {
 		return t.getReadings() != null && t.getReadings().size() == 1;
 	}
 	
 	/*
 	 * Determines whether the given reading contains the given tag
 	 */
 	private boolean containsTag(CGReading cgr, String tag) {
 		StringListIterable reading = new StringListIterable(cgr);
 		for (String rtag : reading) {
 			if (tag.equals(rtag)) {
 				return true;
 			}
 		}
 		return false;
 	}
 
 }
