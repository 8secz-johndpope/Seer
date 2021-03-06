 package org.javaan.graph;
 
 /*
  * #%L
  * Java Static Code Analysis
  * %%
  * Copyright (C) 2013 Andreas Behnke
  * %%
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  * 
  *      http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  * #L%
  */
 
 
 /**
 * Edge with reference to source and target vertex
  */
 public class VertexEdge<V> {
 
 	private final V source;
 	
 	private final V target;
 
 	public VertexEdge(V source, V target) {
 		this.source = source;
 		this.target = target;
 	}
 	
 	public V getSource() {
 		return source;
 	}
 
 	public V getTarget() {
 		return target;
 	}
 }
