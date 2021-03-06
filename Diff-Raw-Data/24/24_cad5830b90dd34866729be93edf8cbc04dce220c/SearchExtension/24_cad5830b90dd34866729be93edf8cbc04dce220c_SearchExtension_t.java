 package com.whysearchtwice.rexster.extension;
 
 import java.util.Calendar;
 import java.util.HashMap;
 import java.util.Map;
 
 import com.tinkerpop.blueprints.Graph;
 import com.tinkerpop.blueprints.Vertex;
 import com.tinkerpop.gremlin.groovy.Gremlin;
 import com.tinkerpop.gremlin.java.GremlinPipeline;
 import com.tinkerpop.pipes.Pipe;
 import com.tinkerpop.pipes.util.iterators.SingleIterator;
 import com.tinkerpop.rexster.RexsterResourceContext;
 import com.tinkerpop.rexster.extension.ExtensionDefinition;
 import com.tinkerpop.rexster.extension.ExtensionDescriptor;
 import com.tinkerpop.rexster.extension.ExtensionNaming;
 import com.tinkerpop.rexster.extension.ExtensionPoint;
 import com.tinkerpop.rexster.extension.ExtensionRequestParameter;
 import com.tinkerpop.rexster.extension.ExtensionResponse;
 import com.tinkerpop.rexster.extension.HttpMethod;
 import com.tinkerpop.rexster.extension.RexsterContext;
 
 @ExtensionNaming(name = SearchExtension.NAME, namespace = AbstractParsleyExtension.NAMESPACE)
 public class SearchExtension extends AbstractParsleyExtension {
 	public static final String NAME = "search";
 
 	@ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, method = HttpMethod.GET)
 	@ExtensionDescriptor(description = "Get the results of a search")
	public ExtensionResponse searchVertices(
 			@RexsterContext RexsterResourceContext context,
 			@RexsterContext Graph graph,
 			@ExtensionRequestParameter(name = "userGuid", defaultValue = "", description = "The user to retrieve information for") String userGuid,
 			@ExtensionRequestParameter(name = "domain", defaultValue = "", description = "Retrieve pages with this domain") String domain,
 			@ExtensionRequestParameter(name = "openTime", defaultValue = "", description = "The middle of a time based query") String openTime,
 			@ExtensionRequestParameter(name = "timeRange", defaultValue = "30", description = "The range of time to search around openTime (openTime +- timeRange/2)") Integer timeRange,
 			@ExtensionRequestParameter(name = "timeRangeUnits", defaultValue = "minutes", description = "hours, minutes, seconds") String units) {
 
 		if (openTime.equals("")) {
 			return ExtensionResponse.error("You should specify an openTime");
 		} else if (userGuid.equals("")) {
 			return ExtensionResponse.error("You should specify a userGuid");
 		}
 
 		// Manipulate parameters
 		Calendar pageOpenTime = Calendar.getInstance();
 		pageOpenTime.setTimeInMillis(Long.parseLong(openTime));
 		timeRange = adjustTimeRange(timeRange, units);
 		
 		Vertex user = graph.getVertex(userGuid);
		if(user == null) {
		    return ExtensionResponse.error("Invalid userGuid");
		}
 		
 		// Perform search
		Pipe pipe = Gremlin.compile("_().out('owns').id");
 		pipe.setStarts(new SingleIterator<Vertex>(user));
 		for(Object id : pipe) {
			System.out.println("Class: " + id.getClass());
			System.out.println("Object: " + id.toString());
 		}
 
 		// Map to store the results
 		Map<String, String> map = new HashMap<String, String>();
 		map.put("message", "Done!");
 
 		return ExtensionResponse.ok(map);
 	}
 
 	private int adjustTimeRange(int timeRange, String units) {
 		if (units.equals("seconds")) {
 			return timeRange * 1;
 		} else if (units.equals("minutes")) {
 			return timeRange * 1 * 60;
 		} else if (units.equals("hours")) {
 			return timeRange * 1 * 60 * 60;
 		} else {
 			return timeRange;
 		}
 	}
 }
