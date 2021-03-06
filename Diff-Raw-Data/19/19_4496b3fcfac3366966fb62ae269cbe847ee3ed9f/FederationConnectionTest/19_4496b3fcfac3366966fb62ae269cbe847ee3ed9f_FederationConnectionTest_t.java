 /*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2008-2009.
  *
 * Licensed under the Aduna BSD-style license.
  */
 package org.openrdf.sail.federation;
 
 import java.io.IOException;
 
 import org.openrdf.repository.Repository;
 import org.openrdf.repository.RepositoryConnectionTest;
 import org.openrdf.repository.sail.SailRepository;
 import org.openrdf.sail.federation.Federation;
 import org.openrdf.sail.memory.MemoryStore;
 
 public class FederationConnectionTest extends RepositoryConnectionTest {
 
 	public FederationConnectionTest(String name) {
 		super(name);
 	}
 
 	@Override
 	protected Repository createRepository()
 		throws IOException
 	{
 		Federation sail = new Federation();
 		sail.addMember(new SailRepository(new MemoryStore()));
 		sail.addMember(new SailRepository(new MemoryStore()));
 		sail.addMember(new SailRepository(new MemoryStore()));
 		return new SailRepository(sail);
 	}
 }
