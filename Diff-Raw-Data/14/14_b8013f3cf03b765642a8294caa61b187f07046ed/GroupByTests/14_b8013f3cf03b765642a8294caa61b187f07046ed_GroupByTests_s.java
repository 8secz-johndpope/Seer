 /**
  * Copyright (c) 2009-2013, Lukas Eder, lukas.eder@gmail.com
  * All rights reserved.
  *
  * This software is licensed to you under the Apache License, Version 2.0
  * (the "License"); You may obtain a copy of the License at
  *
  *   http://www.apache.org/licenses/LICENSE-2.0
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions are met:
  *
  * . Redistributions of source code must retain the above copyright notice, this
  *   list of conditions and the following disclaimer.
  *
  * . Redistributions in binary form must reproduce the above copyright notice,
  *   this list of conditions and the following disclaimer in the documentation
  *   and/or other materials provided with the distribution.
  *
  * . Neither the name "jOOQ" nor the names of its contributors may be
  *   used to endorse or promote products derived from this software without
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
  */
 package org.jooq.test._.testcases;
 
 import static java.util.Arrays.asList;
 import static junit.framework.Assert.assertEquals;
 import static org.jooq.SQLDialect.DB2;
 import static org.jooq.SQLDialect.MARIADB;
 import static org.jooq.SQLDialect.MYSQL;
 import static org.jooq.SQLDialect.SYBASE;
 import static org.jooq.impl.DSL.count;
 import static org.jooq.impl.DSL.cube;
 import static org.jooq.impl.DSL.grouping;
 import static org.jooq.impl.DSL.groupingId;
 import static org.jooq.impl.DSL.groupingSets;
 import static org.jooq.impl.DSL.one;
 import static org.jooq.impl.DSL.rollup;
 import static org.jooq.impl.DSL.selectOne;
 
 import java.sql.Date;
 import java.util.Arrays;
 
 import org.jooq.Field;
 import org.jooq.Record1;
 import org.jooq.Record2;
 import org.jooq.Record3;
 import org.jooq.Record4;
 import org.jooq.Record6;
 import org.jooq.Result;
 import org.jooq.TableRecord;
 import org.jooq.UpdatableRecord;
 import org.jooq.exception.DataAccessException;
 import org.jooq.test.BaseTest;
 import org.jooq.test.jOOQAbstractTest;
 
 import org.junit.Test;
 
 public class GroupByTests<
     A    extends UpdatableRecord<A> & Record6<Integer, String, String, Date, Integer, ?>,
     AP,
     B    extends UpdatableRecord<B>,
     S    extends UpdatableRecord<S> & Record1<String>,
     B2S  extends UpdatableRecord<B2S> & Record3<String, Integer, Integer>,
     BS   extends UpdatableRecord<BS>,
     L    extends TableRecord<L> & Record2<String, String>,
     X    extends TableRecord<X>,
     DATE extends UpdatableRecord<DATE>,
     BOOL extends UpdatableRecord<BOOL>,
     D    extends UpdatableRecord<D>,
     T    extends UpdatableRecord<T>,
     U    extends TableRecord<U>,
     UU   extends UpdatableRecord<UU>,
     I    extends TableRecord<I>,
     IPK  extends UpdatableRecord<IPK>,
     T725 extends UpdatableRecord<T725>,
     T639 extends UpdatableRecord<T639>,
     T785 extends TableRecord<T785>>
 extends BaseTest<A, AP, B, S, B2S, BS, L, X, DATE, BOOL, D, T, U, UU, I, IPK, T725, T639, T785> {
 
     public GroupByTests(jOOQAbstractTest<A, AP, B, S, B2S, BS, L, X, DATE, BOOL, D, T, U, UU, I, IPK, T725, T639, T785> delegate) {
         super(delegate);
     }
 
     @Test
     public void testEmptyGrouping() throws Exception {
 
         // [#1665] Test the empty GROUP BY clause
         assertEquals(1, (int) create().selectOne()
             .from(TBook())
             .groupBy()
             .fetchOne(0, Integer.class));
 
         // [#1665] Test the empty GROUP BY clause
         assertEquals(1, (int) create().selectOne()
             .from(TBook())
             .groupBy()
             .having("1 = 1")
             .fetchOne(0, Integer.class));
     }
 
     @Test
     public void testGrouping() throws Exception {
 
         // Test a simple group by query
         Field<Integer> count = count().as("c");
         Result<Record2<Integer, Integer>> result = create()
             .select(TBook_AUTHOR_ID(), count)
             .from(TBook())
             .groupBy(TBook_AUTHOR_ID()).fetch();
 
         assertEquals(2, result.size());
         assertEquals(2, (int) result.get(0).getValue(count));
         assertEquals(2, (int) result.get(1).getValue(count));
 
         // Test a group by query with a single HAVING clause
         Result<Record2<String, Integer>> result2 = create()
             .select(TAuthor_LAST_NAME(), count)
             .from(TBook())
             .join(TAuthor()).on(TBook_AUTHOR_ID().equal(TAuthor_ID()))
             .where(TBook_TITLE().notEqual("1984"))
             .groupBy(TAuthor_LAST_NAME())
             .having(count().equal(2))
             .fetch();
 
         assertEquals(1, result2.size());
         assertEquals(2, (int) result2.getValue(0, count));
         assertEquals("Coelho", result2.getValue(0, TAuthor_LAST_NAME()));
 
         // Test a group by query with a combined HAVING clause
         Result<Record2<String, Integer>> result3 = create()
             .select(TAuthor_LAST_NAME(), count)
             .from(TBook())
             .join(TAuthor()).on(TBook_AUTHOR_ID().equal(TAuthor_ID()))
             .where(TBook_TITLE().notEqual("1984"))
             .groupBy(TAuthor_LAST_NAME())
             .having(count().equal(2))
             .or(count().greaterOrEqual(2))
             .andExists(selectOne())
             .fetch();
 
         assertEquals(1, result3.size());
         assertEquals(2, (int) result3.getValue(0, count));
         assertEquals("Coelho", result3.getValue(0, TAuthor_LAST_NAME()));
 
         // Test a group by query with a plain SQL having clause
         Result<Record2<String, Integer>> result4 = create()
             .select(VLibrary_AUTHOR(), count)
             .from(VLibrary())
             .where(VLibrary_TITLE().notEqual("1984"))
             .groupBy(VLibrary_AUTHOR())
 
             // MySQL seems to have a bug with fully qualified view names in the
             // having clause. TODO: Fully analyse this issue
             // https://sourceforge.net/apps/trac/jooq/ticket/277
             .having("v_library.author like ?", "Paulo%")
             .fetch();
 
         assertEquals(1, result4.size());
         assertEquals(2, (int) result4.getValue(0, count));
 
         // SQLite loses type information when views select functions.
         // In this case: concatenation. So as a workaround, SQLlite only selects
         // FIRST_NAME in the view
         assertEquals("Paulo", result4.getValue(0, VLibrary_AUTHOR()).substring(0, 5));
     }
 
     @Test
     public void testGroupByCubeRollup() throws Exception {
         switch (dialect()) {
             case ASE:
             case DERBY:
             case FIREBIRD:
             case H2:
             case HSQLDB:
             case INGRES:
             case POSTGRES:
             case SQLITE:
                 log.info("SKIPPING", "Group by CUBE / ROLLUP tests");
                 return;
         }
 
         // Simple ROLLUP clause
         // --------------------
         Result<Record2<Integer, Integer>> result = create()
                 .select(
                     TBook_ID(),
                     TBook_AUTHOR_ID())
                 .from(TBook())
                 .groupBy(rollup(
                     TBook_ID(),
                     TBook_AUTHOR_ID()))
                 .fetch();
 
        if (dialect() == DB2) {
            assertEquals(Arrays.asList(null, 1, 2, 3, 4, 1, 2, 3, 4), result.getValues(0));
            assertEquals(Arrays.asList(null, null, null, null, null, 1, 1, 2, 2), result.getValues(1));
        }
        else {
            assertEquals(Arrays.asList(1, 1, 2, 2, 3, 3, 4, 4, null), result.getValues(0));
            assertEquals(Arrays.asList(1, null, 1, null, 2, null, 2, null, null), result.getValues(1));
        }
 
         if (asList(MARIADB, MYSQL).contains(dialect())) {
             log.info("SKIPPING", "CUBE and GROUPING SETS tests");
             return;
         }
 
         // ROLLUP clause
         // -------------
         Field<Integer> groupingId = groupingId(TBook_ID(), TBook_AUTHOR_ID());
         if (asList(DB2, SYBASE).contains(dialect()))
             groupingId = one();
 
         Result<Record4<Integer, Integer, Integer, Integer>> result2 = create()
                 .select(
                     TBook_ID(),
                     TBook_AUTHOR_ID(),
                     grouping(TBook_ID()),
                     groupingId)
                 .from(TBook())
                 .groupBy(rollup(
                     TBook_ID(),
                     TBook_AUTHOR_ID()))
                 .orderBy(
                     TBook_ID().asc().nullsFirst(),
                     TBook_AUTHOR_ID().asc().nullsFirst()).fetch();
 
         assertEquals(9, result2.size());
         assertEquals(Arrays.asList(null, 1, 1, 2, 2, 3, 3, 4, 4), result2.getValues(0));
         assertEquals(Arrays.asList(null, null, 1, null, 1, null, 2, null, 2), result2.getValues(1));
         assertEquals(Arrays.asList(1, 0, 0, 0, 0, 0, 0, 0, 0), result2.getValues(2));
 
         if (!asList(DB2, SYBASE).contains(dialect()))
             assertEquals(Arrays.asList(3, 1, 0, 1, 0, 1, 0, 1, 0), result2.getValues(3));
 
         // CUBE clause
         // -----------
         Result<Record4<Integer, Integer, Integer, Integer>> result3 = create().select(
                     TBook_ID(),
                     TBook_AUTHOR_ID(),
                     grouping(TBook_ID()),
                     groupingId)
                 .from(TBook())
                 .groupBy(cube(
                     TBook_ID(),
                     TBook_AUTHOR_ID()))
                 .orderBy(
                     TBook_ID().asc().nullsFirst(),
                     TBook_AUTHOR_ID().asc().nullsFirst()).fetch();
 
         assertEquals(11, result3.size());
         assertEquals(Arrays.asList(null, null, null, 1, 1, 2, 2, 3, 3, 4, 4), result3.getValues(0));
         assertEquals(Arrays.asList(null, 1, 2, null, 1, null, 1, null, 2, null, 2), result3.getValues(1));
         assertEquals(Arrays.asList(1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0), result3.getValues(2));
 
         if (!asList(DB2, SYBASE).contains(dialect()))
             assertEquals(Arrays.asList(3, 2, 2, 1, 0, 1, 0, 1, 0, 1, 0), result3.getValues(3));
 
         // GROUPING SETS clause
         // --------------------
         Result<Record4<Integer, Integer, Integer, Integer>> result4 = create().select(
                     TBook_ID(),
                     TBook_AUTHOR_ID(),
                     grouping(TBook_ID()),
                     groupingId)
                 .from(TBook())
                 .groupBy(groupingSets(
                     new Field<?>[] { TBook_AUTHOR_ID(), TBook_ID() },
                     new Field<?>[] { TBook_AUTHOR_ID(), TBook_LANGUAGE_ID() },
                     new Field<?>[0],
                     new Field<?>[0]))
                 .orderBy(
                     TBook_ID().asc().nullsFirst(),
                     TBook_AUTHOR_ID().asc().nullsFirst()).fetch();
 
         assertEquals(9, result4.size());
         assertEquals(Arrays.asList(null, null, null, null, null, 1, 2, 3, 4), result4.getValues(0));
         assertEquals(Arrays.asList(null, null, 1, 2, 2, 1, 1, 2, 2), result4.getValues(1));
         assertEquals(Arrays.asList(1, 1, 1, 1, 1, 0, 0, 0, 0), result4.getValues(2));
 
         if (!asList(DB2, SYBASE).contains(dialect()))
             assertEquals(Arrays.asList(3, 3, 2, 2, 2, 0, 0, 0, 0), result4.getValues(3));
     }
 
     @Test
     public void testHavingWithoutGrouping() throws Exception {
         try {
             assertEquals(Integer.valueOf(1), create()
                 .selectOne()
                 .from(TBook())
                 .where(TBook_AUTHOR_ID().equal(1))
                 .having(count().greaterOrEqual(2))
                 .fetchOne(0));
             assertEquals(null, create()
                 .selectOne()
                 .from(TBook())
                 .where(TBook_AUTHOR_ID().equal(1))
                 .having(count().greaterOrEqual(3))
                 .fetchOne(0));
         }
         catch (DataAccessException e) {
 
             // HAVING without GROUP BY is not supported by some dialects,
             // So this exception is OK
             switch (dialect()) {
 
                 // [#1665] TODO: Add support for the empty GROUP BY () clause
                 case SQLITE:
                     log.info("SKIPPING", "HAVING without GROUP BY is not supported: " + e.getMessage());
                     break;
 
                 default:
                     throw e;
             }
         }
     }
 }
