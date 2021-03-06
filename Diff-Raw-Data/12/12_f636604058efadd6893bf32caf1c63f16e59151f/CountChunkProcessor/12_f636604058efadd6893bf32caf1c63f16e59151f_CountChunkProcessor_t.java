 package semanticMarkup.ling.extract.lib;
 
 import java.util.HashMap;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Set;
 
 import semanticMarkup.core.description.DescriptionTreatmentElement;
 import semanticMarkup.core.description.DescriptionTreatmentElementType;
 import semanticMarkup.know.ICharacterKnowledgeBase;
 import semanticMarkup.know.IGlossary;
 import semanticMarkup.know.IPOSKnowledgeBase;
 import semanticMarkup.ling.chunk.Chunk;
 import semanticMarkup.ling.extract.AbstractChunkProcessor;
 import semanticMarkup.ling.extract.ProcessingContext;
 import semanticMarkup.ling.extract.ProcessingContextState;
 import semanticMarkup.ling.learn.ITerminologyLearner;
 import semanticMarkup.ling.transform.IInflector;
 
 import com.google.inject.Inject;
 import com.google.inject.name.Named;
 
 /**
  * CountChunkProcessor processes chunks of ChunkType.COUNT
  * @author rodenhausen
  */
 public class CountChunkProcessor extends AbstractChunkProcessor {
 
 	/**
 	 * @param inflector
 	 * @param glossary
 	 * @param terminologyLearner
 	 * @param characterKnowledgeBase
 	 * @param posKnowledgeBase
 	 * @param baseCountWords
 	 * @param locationPrepositions
 	 * @param clusters
 	 * @param units
 	 * @param equalCharacters
 	 * @param numberPattern
 	 * @param attachToLast
 	 * @param times
 	 */
 	@Inject
 	public CountChunkProcessor(IInflector inflector, IGlossary glossary, ITerminologyLearner terminologyLearner, 
 			ICharacterKnowledgeBase characterKnowledgeBase, @Named("LearnedPOSKnowledgeBase") IPOSKnowledgeBase posKnowledgeBase,
 			@Named("BaseCountWords")Set<String> baseCountWords, @Named("LocationPrepositionWords")Set<String> locationPrepositions, 
 			@Named("Clusters")Set<String> clusters, @Named("Units")String units, @Named("EqualCharacters")HashMap<String, String> equalCharacters, 
 			@Named("NumberPattern")String numberPattern, @Named("AttachToLast")boolean attachToLast, @Named("TimesWords")String times) {
 		super(inflector, glossary, terminologyLearner, characterKnowledgeBase, posKnowledgeBase, baseCountWords, locationPrepositions, clusters, units, equalCharacters, 
 				numberPattern, attachToLast, times);
 	}
 
 	@Override
 	protected List<DescriptionTreatmentElement> processChunk(Chunk chunk, ProcessingContext processingContext) {
 		LinkedList<DescriptionTreatmentElement> result = new LinkedList<DescriptionTreatmentElement>();
 		ProcessingContextState processingContextState = processingContext.getCurrentState();
 		List<Chunk> modifiers = processingContextState.getUnassignedModifiers();
 		
		LinkedList<DescriptionTreatmentElement> lastElements = processingContextState.getLastElements();
		LinkedList<DescriptionTreatmentElement> subjects = processingContextState.getSubjects();
		LinkedList<DescriptionTreatmentElement> parents = new LinkedList<DescriptionTreatmentElement>();
		if(!lastElements.getLast().isOfDescriptionType(DescriptionTreatmentElementType.STRUCTURE)) {
			if(!subjects.isEmpty())
				parents.add(subjects.getLast());
		} else {
			parents.add(lastElements.getLast()); 
		}
		
 		if(!parents.isEmpty() && parents.getLast().isOfDescriptionType(DescriptionTreatmentElementType.STRUCTURE)) {
 			List<DescriptionTreatmentElement> characterElement = 
 					this.annotateNumericals(chunk.getTerminalsText(), "count", modifiers, parents, false, processingContextState);
 			//DescriptionTreatmentElement characterElement = createCharacterElement(parents, modifiers, chunk.getTerminalsText(), 
 			//		"count", "", processingContextState);
 			if(characterElement!=null)
 				result.addAll(characterElement);
 			processingContextState.clearUnassignedModifiers();
 		} else {
 			List<DescriptionTreatmentElement> characterElement = 
 					this.annotateNumericals(chunk.getTerminalsText(), "count", modifiers, parents, false, processingContextState);
 			//DescriptionTreatmentElement characterElement = createCharacterElement(parents, modifiers, chunk.getTerminalsText(), 
 			//		"count", "", processingContextState);
 			processingContextState.getUnassignedCharacters().addAll(characterElement);
 		}
 		
 		processingContextState.setLastElements(result);
 		processingContextState.setCommaAndOrEosEolAfterLastElements(false);
 		return result;
 	}
 
 }
