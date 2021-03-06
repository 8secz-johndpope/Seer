 package graphs.ops;
 
import java.util.Map;

 import com.fasterxml.jackson.databind.JsonNode;
 import com.tinkerpop.blueprints.Direction;

 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
import degraphmalizr.ID;
 
 public class LoggingSubgraph implements Subgraph
 {
 	Logger log = LoggerFactory.getLogger("subgraph");
 	
 	final String prefix;
 	
 	public LoggingSubgraph(String prefix)
 	{
 		this.prefix = prefix;
 	}
 
     @Override
     public final void addEdge(String label, ID other, Direction direction, Map<String, JsonNode> properties)
     {
         final String d = direction == Direction.IN ? "to" : "from";
        log.info(prefix + " edge " + d + " self to " + other + " with label " + label + " created");
     }
 
     @Override
     public final void setProperty(String key, JsonNode value)
     {
         log.info(prefix + " property " + key + " set to " + value.toString());
     }
 }
