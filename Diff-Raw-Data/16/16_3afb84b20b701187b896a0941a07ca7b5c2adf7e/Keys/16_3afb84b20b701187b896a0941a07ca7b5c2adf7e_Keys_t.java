 /*
  * Copyright (c) 2007-2008, Hazel Ltd. All Rights Reserved.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  *
  */
 
 package com.hazelcast.impl;
 
 import java.io.DataInput;
 import java.io.DataOutput;
 import java.io.IOException;
 import java.util.HashSet;
 import java.util.Set;
 
 import com.hazelcast.nio.Data;
 import com.hazelcast.nio.DataSerializable;
 
 public class Keys implements DataSerializable{

 	private Set<Data> keys = new HashSet<Data>();
 	
 	public Keys() {
 	}

 	public void readData(DataInput in) throws IOException {
 		int size = in.readInt();
 		keys = new HashSet<Data>();
 		for(int i=0;i<size;i++){
 			Data data = new Data();
 			data.readData(in);
 			keys.add(data);
 		}
 	}
 	
 	public void writeData(DataOutput out) throws IOException {
 		int size = (keys==null)?0:keys.size();
 		out.writeInt(size);
 		if(size>0){
 			for (Data key : keys) {
 				key.writeData(out);
 			}
 		}
 	}
 	
	public Set<Data> getKeys(){
 		return keys;
 	}
	public void setKeys(Set<Data> keys){
 		this.keys = keys;
 	}
 	public void addKey(Data obj) {
 		this.keys.add(obj);
 		
 	}
 }
