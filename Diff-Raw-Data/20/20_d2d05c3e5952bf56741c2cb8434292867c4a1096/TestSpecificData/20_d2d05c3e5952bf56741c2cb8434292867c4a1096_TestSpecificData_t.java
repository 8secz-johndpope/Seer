 /**
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package org.apache.avro.specific;
 
 import java.util.List;
import java.util.Map;
 import java.util.ArrayList;
 
 import org.junit.Test;
 
 import org.apache.avro.Schema;
 import org.apache.avro.generic.GenericData;
 import org.apache.avro.util.Utf8;
 
 import org.apache.avro.TestSchema;
 import org.apache.avro.test.TestRecord;
 import org.apache.avro.test.MD5;
 import org.apache.avro.test.Kind;
 
 public class TestSpecificData {
   
   @Test
   /** Make sure that even with nulls, hashCode() doesn't throw NPE. */
   public void testHashCode() {
     new TestRecord().hashCode();
     SpecificData.get().hashCode(null, TestRecord.SCHEMA$);
   }
 
   @Test
   /** Make sure that even with nulls, toString() doesn't throw NPE. */
   public void testToString() {
     new TestRecord().toString();
   }
 
  private static class X {
    public Map<String,String> map;
  }

  @Test
  public void testGetMapSchema() throws Exception {
    SpecificData.get().getSchema(X.class.getField("map").getGenericType());
  }

   @Test
   /** Test nesting of specific data within generic. */
   public void testSpecificWithinGeneric() throws Exception {
     // define a record with a field that's a generated TestRecord
     Schema schema = Schema.createRecord("Foo", "", "x.y.z", false);
     List<Schema.Field> fields = new ArrayList<Schema.Field>();
     fields.add(new Schema.Field("f", TestRecord.SCHEMA$, "", null));
     schema.setFields(fields);
 
     // create a generic instance of this record
     TestRecord nested = new TestRecord();
     nested.setName("foo");
     nested.setKind(Kind.BAR);
     nested.setHash(new MD5(new byte[]{0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5}));
     GenericData.Record record = new GenericData.Record(schema);
     record.put("f", nested);
 
     // test that this instance can be written & re-read
     TestSchema.checkBinary(schema, record,
                            new SpecificDatumWriter<Object>(),
                            new SpecificDatumReader<Object>());
 
     TestSchema.checkDirectBinary(schema, record,
         new SpecificDatumWriter<Object>(),
         new SpecificDatumReader<Object>());
 
     TestSchema.checkBlockingBinary(schema, record,
         new SpecificDatumWriter<Object>(),
         new SpecificDatumReader<Object>());
 }
 
 
 
 }
