 package com.collabinate.server;
 
 import org.junit.After;
 import org.junit.Test;
 
 import com.tinkerpop.blueprints.GraphFactory;
 import com.tinkerpop.blueprints.KeyIndexableGraph;
 
 /**
  * Test class for the graph implementation of CollabinateWriter.
  * 
  * @author mafuba
  *
  */
 public class GraphServerWriterTest extends CollabinateWriterTest
 {
 	private KeyIndexableGraph graph;
 	
 	@Override
 	CollabinateWriter getWriter()
 	{
 		graph = (KeyIndexableGraph)GraphFactory.open(
 				"src/test/resources/graph.properties");
 		return new GraphServer(graph);
 	}
 	
 	@After
	public void shutdownGraph()
 	{
 		if (null != graph)
 			graph.shutdown();
 	}
 	
 	@Test
 	public void should_not_allow_null_graph()
 	{
 		exception.expect(IllegalArgumentException.class);
 		new GraphServer(null);
 	}
 }
