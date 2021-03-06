 /*
  * Copyright 2011 MeMo News AG. All rights reserved.
  */
 package com.memonews.hbase.util;
 
 import java.io.IOException;
 import java.util.NavigableMap;
 import java.util.Set;
 
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.hbase.client.Delete;
 import org.apache.hadoop.hbase.client.Get;
 import org.apache.hadoop.hbase.client.HTable;
 import org.apache.hadoop.hbase.client.Put;
 import org.apache.hadoop.hbase.client.Result;
 import org.apache.hadoop.hbase.util.Bytes;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 /**
  * @author nkuebler, MeMo News AG
 * @since 2.0.0
 *
  */
 public class HBaseUtil {
 	
	
 	private static final Logger LOG = LoggerFactory.getLogger(HBaseUtil.class);
 	
 	public static void moveRow(final Configuration conf,
 			final String sourceTableName, final String sourceKey,
 			final String targetTableName, final String targetKey)
 			throws Exception {
 		copyRow(conf, sourceTableName, sourceKey, targetTableName, targetKey);
 		deleteRow(conf, sourceTableName, sourceKey);
 	}
 	
 	public static void deleteRow(final Configuration conf,
 			final String tableName, final String key) throws IOException {
 		final HTable table = new HTable(conf, tableName);
 		Delete delete = new Delete(Bytes.toBytes(key));
 		LOG.info("deleting row "+key+" in table " + tableName);
 		table.delete(delete);
 	}
 	
 	public static void copyRow(final Configuration conf,
 			final String sourceTableName, final String sourceKey,
 			final String targetTableName, final String targetKey)
 			throws Exception {
 		final HTable sourceTable = new HTable(conf, sourceTableName);
 		Get get = new Get(Bytes.toBytes(sourceKey));
 		get.setMaxVersions();
 		Result result = sourceTable.get(get);
 		Set<byte[]> allFamilies = result.getMap().keySet();
 		Put put = new Put(Bytes.toBytes(targetKey));
 		int familyCount = 0;
 		for(byte[] family : allFamilies){
 			resultToPut(result, put, family, family);
 			familyCount++;
 		}
 		LOG.info("creating row with "+familyCount+" families in table " + targetTableName);
 		final HTable targetTable = new HTable(conf, targetTableName);
 		targetTable.put(put);
 	}
 
 	public static Put resultToPut(final Result source, Put target,
 			final byte[] sourceFamily, final byte[] targetFamily) {
 		final NavigableMap<byte[], NavigableMap<Long, byte[]>> map = source
 				.getMap().get(sourceFamily);
 		for (final byte[] qualifier : map.keySet()) {
 			for (final Long ts : map.get(qualifier).keySet()) {
 				final byte[] value = map.get(qualifier).get(ts);
 				target.add(targetFamily, qualifier, ts, value);
 			}
 		}
 		return target;
 	}
 }
