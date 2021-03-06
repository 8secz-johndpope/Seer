 package org.osate.imv.aadldiagram.util;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import org.osate.aadl2.ComponentCategory;
 import org.osate.aadl2.Element;
 import org.osate.aadl2.instance.ComponentInstance;
 import org.osate.aadl2.instance.ConnectionInstance;
 import org.osate.aadl2.instance.ConnectionReference;
 import org.osate.aadl2.instance.FeatureInstance;
import org.osate.aadl2.util.OsateDebug;
 import org.osate.xtext.aadl2.errormodel.errorModel.ErrorPropagation;
 import org.osate.xtext.aadl2.errormodel.errorModel.ErrorType;
 import org.osate.xtext.aadl2.errormodel.errorModel.TypeSet;
 import org.osate.xtext.aadl2.errormodel.errorModel.TypeToken;
 import org.osate.xtext.aadl2.errormodel.util.EM2Util;
 import org.osate.xtext.aadl2.properties.util.GetProperties;
 
 public class ErrorUtil {
 	
 	public final static int INVALID_FACTOR 		= -1;
 	public final static int MIN_FACTOR     		=  1;
 	public final static int MAX_FACTOR     		=  99;
 	public final static int DEFAULT_FACTOR     	=  50;
 	
 	/*
 	 * returns a factor between 0 and 100 which indicates
 	 * the potential impact of a fault ocuring from the selected
 	 * component.
 	 */
 	public static int getFactor (ComponentInstance source, ComponentInstance dest)
 	{
 		ComponentInstance 		boundProcessor;
 		List<ComponentInstance> boundProcessors;
 		ComponentInstance 		sourceParent;
 		ComponentInstance		destinationParent;
 		Element 				tmpElement;
 		List<ConnectionInstance> connectionInstances;
 		ErrorPropagation  		sourcePropagation;
 		ErrorPropagation		destinationPropagation;
 		
 		if ( (source == null) || (dest == null))
 		{
 			return INVALID_FACTOR;
 		}
 		
 		/*
 		 * The component source and dest are the same. So, basically, this
 		 * is where we draw the component responsible to propagate an error.
 		 * When evaluating it, we just return the highest value, 99.
 		 */
 		if (source == dest)
 		{
 			return MAX_FACTOR;
 		}
 		
 		tmpElement = source.getOwner();
 		if (tmpElement instanceof ComponentInstance)
 		{
 			sourceParent = (ComponentInstance) tmpElement;
 		}
 		
 		tmpElement = dest.getOwner();
 		if (tmpElement instanceof ComponentInstance)
 		{
 			destinationParent = (ComponentInstance) tmpElement;
 		}
 		
 	
 		for (FeatureInstance fi : source.getFeatureInstances())
 		{
 			//OsateDebug.osateDebug("source feature=" + fi);
 			connectionInstances = new ArrayList<ConnectionInstance>();
 			connectionInstances.addAll(fi.getDstConnectionInstances());
 			connectionInstances.addAll(fi.getSrcConnectionInstances());
 
 			for (ConnectionInstance ci : connectionInstances)
 			{
 				//OsateDebug.osateDebug("ci=" + ci);
 
 				for (ConnectionReference cr : ci.getConnectionReferences())
 				{
 					//OsateDebug.osateDebug("dest=" + cr.getDestination());
 					if ((cr.getDestination() instanceof FeatureInstance) && 
 					    (cr.getDestination().getContainingComponentInstance() == dest))
 					{
						OsateDebug.osateDebug("same");
 						FeatureInstance destinationFeature = (FeatureInstance) cr.getDestination();
 						sourcePropagation = EM2Util.getOutgoingErrorPropagation (fi);
 						destinationPropagation = EM2Util.getOutgoingErrorPropagation (destinationFeature);
 						
						if (sourcePropagation == null)
						{
							continue;
						}
						
						if (destinationPropagation == null)
						{
							continue;
						}
						
 						TypeSet sourceTypes = sourcePropagation.getTypeSet();
 						TypeSet destinationTypes = sourcePropagation.getTypeSet();
 						for (TypeToken tt1 : sourceTypes.getElementType())
 						{
 							for (ErrorType type1 : tt1.getType())
 							{
 								for (TypeToken tt2 : destinationTypes.getElementType())
 								{
 									for (ErrorType type2 : tt2.getType())
 									{
 										if (type1.getName().equalsIgnoreCase(type2.getName()))
 										{
 											/*
 											 * Here, it means that we have the
 											 * same error propagation
 											 * between the source and the destination.
 											 */
											OsateDebug.osateDebug("same type");
 											return DEFAULT_FACTOR;
 										}
 									}
 								}
 
 							}
 						}
 
 					}
 					
 				}
 			}
 			
 			ErrorPropagation ep = EM2Util.getOutgoingErrorPropagation (fi);
 		}
 		
 		/*
 		 * Here, we check if the destination component is a device and process and the source a processor.
 		 * Then, it means that errors from the runtime processor may affect process or device bound
 		 * to this processor.
 		 */
 		if (( (dest.getCategory() == ComponentCategory.DEVICE) && (source.getCategory() == ComponentCategory.PROCESSOR))
 			||
 			( (dest.getCategory() == ComponentCategory.PROCESS) && (source.getCategory() == ComponentCategory.PROCESSOR)))
 		{
 			boundProcessors = GetProperties.getActualProcessorBinding(dest);
 			if (boundProcessors.size() > 0)
 			{
 				boundProcessor = boundProcessors.get(0);
 				if (boundProcessor == source)
 				{
 					return (MAX_FACTOR - MIN_FACTOR) / 2;
 				}
 			}
 		}
 		return INVALID_FACTOR;
 	}
 }
