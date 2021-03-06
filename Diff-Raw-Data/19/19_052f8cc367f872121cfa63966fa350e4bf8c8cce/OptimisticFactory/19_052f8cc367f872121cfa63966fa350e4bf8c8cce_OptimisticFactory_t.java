 /*
  * Copyright (c) 2009, James Leigh All rights reserved.
  * 
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions are met:
  * 
  * - Redistributions of source code must retain the above copyright notice, this
  *   list of conditions and the following disclaimer.
  * - Redistributions in binary form must reproduce the above copyright notice,
  *   this list of conditions and the following disclaimer in the documentation
  *   and/or other materials provided with the distribution. 
  * - Neither the name of the openrdf.org nor the names of its contributors may
  *   be used to endorse or promote products derived from this software without
  *   specific prior written permission.
  * 
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
  * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
  * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
  * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
  * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
  * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
  * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
  * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
  * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
  * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
  * POSSIBILITY OF SUCH DAMAGE.
  * 
  */
 package org.openrdf.sail.optimistic.config;
 
 import org.openrdf.repository.Repository;
 import org.openrdf.repository.config.RepositoryConfigException;
 import org.openrdf.repository.config.RepositoryImplConfig;
import org.openrdf.repository.config.RepositoryImplConfigBase;
 import org.openrdf.repository.sail.SailRepository;
 import org.openrdf.repository.sail.config.SailRepositoryFactory;
 import org.openrdf.sail.optimistic.OptimisticRepository;
 
 /**
  * Creates {@link OptimisticRepository} from configurations.
  * 
  * @author James Leigh
  */
 public class OptimisticFactory extends SailRepositoryFactory {
 
 	public static final String REPOSITORY_TYPE = "openrdf:OptimisticRepository";
 
 	public String getRepositoryType() {
 		return REPOSITORY_TYPE;
 	}
 
 	@Override
 	public Repository getRepository(RepositoryImplConfig config)
 			throws RepositoryConfigException {
 		SailRepository repository = (SailRepository) super.getRepository(config);
 		return new OptimisticRepository(repository.getSail());
 	}

	@Override
	public RepositoryImplConfig getConfig() {
		RepositoryImplConfig config = super.getConfig();
		((RepositoryImplConfigBase)config).setType(getRepositoryType());
		return config;
	}
 }
