 /*
  * Copyright 2011 Mozilla Foundation
  *
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  *   http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package com.mozilla.bagheera.dao;
 
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Map;
 
 import org.apache.hadoop.hbase.client.Get;
 import org.apache.hadoop.hbase.client.HTable;
 import org.apache.hadoop.hbase.client.HTableInterface;
 import org.apache.hadoop.hbase.client.HTablePool;
 import org.apache.hadoop.hbase.client.Put;
 import org.apache.hadoop.hbase.client.Result;
 import org.apache.hadoop.hbase.util.Bytes;
 
 import com.mozilla.bagheera.util.IdUtil;
 
 /**
  * HBaseTableDao is designed to be a thin class that encapsulates some of most
  * commonly used byte conversion and try/finally logic associated with doing
  * puts into HBase.
  */
 public class HBaseTableDao {
 
 	private final HTablePool pool;
 	private final byte[] tableName;
 	private final byte[] family;
 	private final byte[] qualifier;
 
 	public HBaseTableDao(HTablePool pool, String tableName, String family, String qualifier) {
 		this.pool = pool;
 		this.tableName = Bytes.toBytes(tableName);
 		this.family = Bytes.toBytes(family);
 		this.qualifier = Bytes.toBytes(qualifier);
 	}
 
 	/**
 	 * @return
 	 */
 	public byte[] getTableName() {
 		return tableName;
 	}
 
 	/**
 	 * @return
 	 */
 	public byte[] getColumnFamily() {
 		return family;
 	}
 
 	/**
 	 * @return
 	 */
 	public byte[] getColumnQualifier() {
 		return qualifier;
 	}
 
 	/**
 	 * @param value
 	 * @throws IOException
 	 */
 	public void put(String value) throws IOException {
 		put(IdUtil.generateBucketizedId(), Bytes.toBytes(value));
 	}
 
 	/**
 	 * @param key
 	 * @param value
 	 * @throws IOException
 	 */
 	public void put(String key, String value) throws IOException {
 		put(Bytes.toBytes(key), Bytes.toBytes(value));
 	}
 
 	/**
 	 * @param key
 	 * @param value
 	 * @throws IOException
 	 */
 	public void put(byte[] key, byte[] value) throws IOException {
 		HTableInterface table = null;
 		try {
 			table = pool.getTable(tableName);
 			Put p = new Put(key);
 			p.add(family, qualifier, value);
 			table.put(p);
 		} finally {
 			if (table != null) {
 				pool.putTable(table);
 			}
 		}
 	}
 
 	/**
 	 * @param values
 	 * @throws IOException
 	 */
 	public void putStringList(List<String> values) throws IOException {
 		List<Put> puts = new ArrayList<Put>();
 		for (String value : values) {
 			byte[] id = IdUtil.generateBucketizedId();
 			Put p = new Put(id);
 			p.add(family, qualifier, Bytes.toBytes(value));
 			puts.add(p);
 		}
 		putList(puts);
 	}
 
 	/**
 	 * @param values
 	 * @throws IOException
 	 */
 	public void putStringMap(Map<String, String> values) throws IOException {
 		List<Put> puts = new ArrayList<Put>();
 		for (Map.Entry<String, String> entry : values.entrySet()) {
 			Put p = new Put(Bytes.toBytes(entry.getKey()));
 			p.add(family, qualifier, Bytes.toBytes(entry.getValue()));
 			puts.add(p);
 		}
 		putList(puts);
 	}
 
 	/**
 	 * @param values
 	 * @throws IOException
 	 */
 	public void putByteMap(Map<byte[], byte[]> values) throws IOException {
 		List<Put> puts = new ArrayList<Put>();
 		for (Map.Entry<byte[], byte[]> entry : values.entrySet()) {
 			Put p = new Put(entry.getKey());
 			p.add(family, qualifier, entry.getValue());
 			puts.add(p);
 		}
 		putList(puts);
 	}
 
 	/**
 	 * @param puts
 	 * @throws IOException
 	 */
 	public void putList(List<Put> puts) throws IOException {
 		HTable table = null;
 		try {
 			table = (HTable) pool.getTable(tableName);
 			table.setAutoFlush(false);
 			table.put(puts);
 			table.flushCommits();
 		} finally {
 			if (table != null) {
 				pool.putTable(table);
 			}
 		}
 	}
 
 	/**
 	 * Example Row: 0110216000605a4-6640-4576-be23-b76e32110216
 	 * @param row
 	 * @return
 	 */
 	public String get(String row) {
 		HTableInterface table = null;
		table = pool.getTable(tableName);
		Get g = new Get(Bytes.toBytes(row));
		Result r;
 		try {
			r = table.get(g);
 			byte[] value = r.getValue(family, qualifier);
			if (value == null) {
				return null;
 			}
			return new String(value);
 		} catch (IOException e) {
			e.printStackTrace();

 		}
 
		return null;
 
 	}
 
 }
