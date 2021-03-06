 package com.collabinate.server;
 
 import org.junit.After;
 import org.junit.Test;
 
 import com.tinkerpop.blueprints.GraphFactory;
 import com.tinkerpop.blueprints.KeyIndexableGraph;
 
 /**
  * Test class for the graph implementation of CollabinateReader.
  * 
  * @author mafuba
  *
  */
 public class GraphServerReaderTest extends CollabinateReaderTest
 {
 	private KeyIndexableGraph graph;
 	private GraphServer server;
 	
 	@Override
 	CollabinateReader getReader()
 	{
 		return getServer();
 	}
 	
 	@Override
 	CollabinateWriter getWriter()
 	{
 		return getServer();
 	}
 	
 	/**
 	 * Ensures the same server is used as both the reader and writer.
 	 * 
 	 * @return The server to use.
 	 */
 	private GraphServer getServer()
 	{
 		if (null == server)
 		{
 			graph = (KeyIndexableGraph)GraphFactory.open(
 					"src/test/resources/graph.properties");
 			server = new GraphServer(graph);
 		}
 		return server;
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
