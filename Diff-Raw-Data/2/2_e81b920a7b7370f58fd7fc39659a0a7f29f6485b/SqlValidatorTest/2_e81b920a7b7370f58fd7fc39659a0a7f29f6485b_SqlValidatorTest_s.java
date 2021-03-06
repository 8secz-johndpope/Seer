 /*
 // $Id$
 // Package org.eigenbase is a class library of data management components.
 // Copyright (C) 2005-2005 The Eigenbase Project
 // Copyright (C) 2002-2005 Disruptive Tech
 // Copyright (C) 2005-2005 LucidEra, Inc.
 // Portions Copyright (C) 2003-2005 John V. Sichi
 //
 // This program is free software; you can redistribute it and/or modify it
 // under the terms of the GNU General Public License as published by the Free
 // Software Foundation; either version 2 of the License, or (at your option)
 // any later version approved by The Eigenbase Project.
 //
 // This program is distributed in the hope that it will be useful,
 // but WITHOUT ANY WARRANTY; without even the implied warranty of
 // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 // GNU General Public License for more details.
 //
 // You should have received a copy of the GNU General Public License
 // along with this program; if not, write to the Free Software
 // Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
 
 package org.eigenbase.test;
 
 import junit.framework.TestCase;
 import org.eigenbase.sql.SqlCollation;
 import org.eigenbase.sql.SqlIntervalQualifier;
 
 import java.nio.charset.Charset;
 import java.util.logging.Logger;
 
 
 /**
  * Concrete child class of {@link SqlValidatorTestCase}, containing lots of
  * unit tests.
  *
  * <p>If you want to run these same tests in a different environment, create
  * a derived class whose {@link #getTester} returns a different implementation
  * of {@link Tester}.
  *
  * @author Wael Chatila
  * @since Jan 14, 2004
  * @version $Id$
  **/
 public class SqlValidatorTest extends SqlValidatorTestCase
 {
     /**
      * @deprecated Deprecated so that usages of this constant will show up in
      * yellow in Intellij and maybe someone will fix them.
      */
     protected static final boolean todo = false;
     private static final boolean bug269fixed = false;
     public static final boolean todoTypeInference = false;
 
     public final Logger logger = Logger.getLogger(getClass().getName());
 
     //~ Methods ---------------------------------------------------------------
 
     public void testMultipleSameAsPass() {
         checkExp("1 as again,2 as \"again\", 3 as AGAiN");
     }
 
     public void testMultipleDifferentAs() {
         check("select 1 as c1,2 as c2 from (values(true))");
     }
 
     public void testTypeOfAs() {
         checkExpType("1 as c1", "INTEGER");
         checkExpType("'hej' as c1", "CHAR(3)");
         checkExpType("x'deadbeef' as c1", "BINARY(4)");
     }
 
     public void testTypesLiterals() {
         checkExpType("'abc'", "CHAR(3)");
         checkExpType("n'abc'", "CHAR(3)");
         checkExpType("_iso_8859-2'abc'", "CHAR(3)");
         checkExpType("'ab '" + NL + "' cd'", "CHAR(6)");
         checkExpType("'ab'" + NL + "'cd'" + NL + "'ef'" + NL + "'gh'" + NL
             + "'ij'" + NL + "'kl'", "CHAR(12)");
         checkExpType("n'ab '" + NL + "' cd'", "CHAR(6)");
         checkExpType("_iso_8859-2'ab '" + NL + "' cd'", "CHAR(6)");
 
         checkExpFails("x'abc'",
             "Binary literal string must contain an even number of hexits");
         checkExpType("x'abcd'", "BINARY(2)");
         checkExpType("x'abcd'" + NL + "'ff001122aabb'", "BINARY(8)");
         checkExpType("x'aaaa'" + NL + "'bbbb'" + NL + "'0000'" + NL + "'1111'",
             "BINARY(8)");
 
         checkExpType("1234567890", "INTEGER");
         checkExpType("123456.7890", "DECIMAL(10, 4)");
         checkExpType("123456.7890e3", "DOUBLE");
         checkExpType("true", "BOOLEAN");
         checkExpType("false", "BOOLEAN");
         checkExpType("unknown", "BOOLEAN");
     }
 
     public void testBooleans() {
         check("select TRUE OR unknowN from (values(true))");
         check("select false AND unknown from (values(true))");
         check("select not UNKNOWn from (values(true))");
         check("select not true from (values(true))");
         check("select not false from (values(true))");
     }
 
     public void testAndOrIllegalTypesFails() {
         //TODO need col+line number
         assertExceptionIsThrown("select 'abc' AND FaLsE from (values(true))",
             "(?s).*'<CHAR.3.> AND <BOOLEAN>'.*");
 
         assertExceptionIsThrown("select TRUE OR 1 from (values(true))", "(?s).*");
 
         assertExceptionIsThrown("select unknown OR 1.0 from (values(true))",
             "(?s).*");
 
         assertExceptionIsThrown("select true OR 1.0e4 from (values(true))",
             "(?s).*");
 
         if (todo) {
             assertExceptionIsThrown("select TRUE OR (TIME '12:00' AT LOCAL) from (values(true))",
                 "some error msg with line + col");
         }
     }
 
     public void testNotIlleagalTypeFails() {
         //TODO need col+line number
         assertExceptionIsThrown("select NOT 3.141 from (values(true))",
             "(?s).*'NOT<DECIMAL.4, 3.>'.*");
 
         assertExceptionIsThrown("select NOT 'abc' from (values(true))", "(?s).*");
 
         assertExceptionIsThrown("select NOT 1 from (values(true))", "(?s).*");
     }
 
     public void testIs() {
         check("select TRUE IS FALSE FROM (values(true))");
         check("select false IS NULL FROM (values(true))");
         check("select UNKNOWN IS NULL FROM (values(true))");
         check("select FALSE IS UNKNOWN FROM (values(true))");
 
         check("select TRUE IS NOT FALSE FROM (values(true))");
         check("select TRUE IS NOT NULL FROM (values(true))");
         check("select false IS NOT NULL FROM (values(true))");
         check("select UNKNOWN IS NOT NULL FROM (values(true))");
         check("select FALSE IS NOT UNKNOWN FROM (values(true))");
 
         check("select 1 IS NULL FROM (values(true))");
         check("select 1.2 IS NULL FROM (values(true))");
         checkExpFails("'abc' IS NOT UNKNOWN", "(?s).*Cannot apply.*");
     }
 
     public void testIsFails() {
         assertExceptionIsThrown("select 1 IS TRUE FROM (values(true))",
             "(?s).*'<INTEGER> IS TRUE'.*");
 
         assertExceptionIsThrown("select 1.1 IS NOT FALSE FROM (values(true))",
             "(?s).*");
 
         assertExceptionIsThrown("select 1.1e1 IS NOT FALSE FROM (values(true))",
             "(?s).*Cannot apply 'IS NOT FALSE' to arguments of type '<DOUBLE> IS NOT FALSE'.*");
 
         assertExceptionIsThrown("select 'abc' IS NOT TRUE FROM (values(true))",
             "(?s).*");
     }
 
     public void testScalars() {
         check("select 1  + 1 from (values(true))");
         check("select 1  + 2.3 from (values(true))");
         check("select 1.2+3 from (values(true))");
         check("select 1.2+3.4 from (values(true))");
 
         check("select 1  - 1 from (values(true))");
         check("select 1  - 2.3 from (values(true))");
         check("select 1.2-3 from (values(true))");
         check("select 1.2-3.4 from (values(true))");
 
         check("select 1  * 2 from (values(true))");
         check("select 1.2* 3 from (values(true))");
         check("select 1  * 2.3 from (values(true))");
         check("select 1.2* 3.4 from (values(true))");
 
         check("select 1  / 2 from (values(true))");
         check("select 1  / 2.3 from (values(true))");
         check("select 1.2/ 3 from (values(true))");
         check("select 1.2/3.4 from (values(true))");
     }
 
     public void testScalarsFails() {
         //TODO need col+line number
         assertExceptionIsThrown("select 1+TRUE from (values(true))",
             "(?s).*Cannot apply '\\+' to arguments of type '<INTEGER> \\+ <BOOLEAN>'\\. Supported form\\(s\\):.*");
     }
 
     public void testNumbers() {
         check("select 1+-2.*-3.e-1/-4>+5 AND true from (values(true))");
     }
 
     public void testPrefix() {
         checkExpType("+interval '1' second","INTERVAL SECOND");
         checkExpType("-interval '1' month","INTERVAL MONTH");
         checkFails("SELECT -'abc' from (values(true))",
             "(?s).*Cannot apply '-' to arguments of type '-<CHAR.3.>'.*");
         checkFails("SELECT +'abc' from (values(true))",
             "(?s).*Cannot apply '\\+' to arguments of type '\\+<CHAR.3.>'.*");
     }
 
     public void testEqualNotEqual() {
         checkExp("''=''");
         checkExp("'abc'=n''");
         checkExp("''=_latin1''");
         checkExp("n''=''");
         checkExp("n'abc'=n''");
         checkExp("n''=_latin1''");
         checkExp("_latin1''=''");
         checkExp("_latin1''=n''");
         checkExp("_latin1''=_latin1''");
 
         checkExp("''<>''");
         checkExp("'abc'<>n''");
         checkExp("''<>_latin1''");
         checkExp("n''<>''");
         checkExp("n'abc'<>n''");
         checkExp("n''<>_latin1''");
         checkExp("_latin1''<>''");
         checkExp("_latin1'abc'<>n''");
         checkExp("_latin1''<>_latin1''");
 
         checkExp("true=false");
         checkExp("unknown<>true");
 
         checkExp("1=1");
         checkExp("1=.1");
         checkExp("1=1e-1");
         checkExp("0.1=1");
         checkExp("0.1=0.1");
         checkExp("0.1=1e1");
         checkExp("1.1e1=1");
         checkExp("1.1e1=1.1");
         checkExp("1.1e-1=1e1");
 
         checkExp("''<>''");
         checkExp("1<>1");
         checkExp("1<>.1");
         checkExp("1<>1e-1");
         checkExp("0.1<>1");
         checkExp("0.1<>0.1");
         checkExp("0.1<>1e1");
         checkExp("1.1e1<>1");
         checkExp("1.1e1<>1.1");
         checkExp("1.1e-1<>1e1");
     }
 
     public void testEqualNotEqualFails() {
         checkExpFails("''<>1",
             "(?s).*Cannot apply '<>' to arguments of type '<CHAR.0.> <> <INTEGER>'.*");
         checkExpFails("'1'>=1",
             "(?s).*Cannot apply '>=' to arguments of type '<CHAR.1.> >= <INTEGER>'.*");
         checkExpFails("1<>n'abc'",
             "(?s).*Cannot apply '<>' to arguments of type '<INTEGER> <> <CHAR.3.>'.*");
         checkExpFails("''=.1",
             "(?s).*Cannot apply '=' to arguments of type '<CHAR.0.> = <DECIMAL.1..1.>'.*");
         checkExpFails("true<>1e-1",
             "(?s).*Cannot apply '<>' to arguments of type '<BOOLEAN> <> <DOUBLE>'.*");
         checkExpFails("false=''",
             "(?s).*Cannot apply '=' to arguments of type '<BOOLEAN> = <CHAR.0.>'.*");
         checkExpFails("x'a4'=0.01",
             "(?s).*Cannot apply '=' to arguments of type '<BINARY.1.> = <DECIMAL.3, 2.>'.*");
         checkExpFails("x'a4'=1",
             "(?s).*Cannot apply '=' to arguments of type '<BINARY.1.> = <INTEGER>'.*");
         checkExpFails("x'13'<>0.01",
             "(?s).*Cannot apply '<>' to arguments of type '<BINARY.1.> <> <DECIMAL.3, 2.>'.*");
         checkExpFails("x'abcd'<>1",
             "(?s).*Cannot apply '<>' to arguments of type '<BINARY.2.> <> <INTEGER>'.*");
     }
 
     public void testBinaryString() {
         check("select x'face'=X'' from (values(true))");
         check("select x'ff'=X'' from (values(true))");
     }
 
     public void testBinaryStringFails() {
         assertExceptionIsThrown("select x'ffee'='abc' from (values(true))",
             "(?s).*Cannot apply '=' to arguments of type '<BINARY.2.> = <CHAR.3.>'.*");
         assertExceptionIsThrown("select x'ff'=88 from (values(true))",
             "(?s).*Cannot apply '=' to arguments of type '<BINARY.1.> = <INTEGER>'.*");
         assertExceptionIsThrown("select x''<>1.1e-1 from (values(true))",
             "(?s).*Cannot apply '<>' to arguments of type '<BINARY.0.> <> <DOUBLE>'.*");
         assertExceptionIsThrown("select x''<>1.1 from (values(true))",
             "(?s).*Cannot apply '<>' to arguments of type '<BINARY.0.> <> <DECIMAL.2, 1.>'.*");
     }
 
     public void testStringLiteral() {
         check("select n''=_iso_8859-1'abc' from (values(true))");
         check("select N'f'<>'''' from (values(true))");
     }
 
     public void testStringLiteralBroken() {
         check("select 'foo'" + NL + "'bar' from (values(true))");
         checkFails("select 'foo' 'bar' from (values(true))",
             "String literal continued on same line", 1, 14);
     }
 
     public void testArthimeticOperators() {
         checkExp("pow(2,3)");
         checkExp("aBs(-2.3e-2)");
         checkExp("MOD(5             ,\t\f\r\n2)");
         checkExp("ln(5.43  )");
         checkExp("log(- -.2  )");
 
         checkExpFails("mod(5.1, 3)", "(?s).*Cannot apply.*");
         checkExpFails("mod(2,5.1)", "(?s).*Cannot apply.*");
     }
 
     public void testArthimeticOperatorsTypes() {
         checkExpType("pow(2,3)", "DOUBLE");
         checkExpType("aBs(-2.3e-2)", "DOUBLE");
         checkExpType("aBs(5000000000)", "BIGINT");
         checkExpType("aBs(-interval '1-1' year to month)", "INTERVAL YEAR TO MONTH");
         checkExpType("aBs(+interval '1:1' hour to minute)", "INTERVAL HOUR TO MINUTE");
         checkExpType("MOD(5,2)", "INTEGER");
         checkExpType("ln(5.43  )", "DOUBLE");
         checkExpType("log(- -.2  )", "DOUBLE");
     }
 
     public void testArthimeticOperatorsFails() {
         checkExpFails("pow(2,'abc')",
             "(?s).*Cannot apply 'POW' to arguments of type 'POW.<INTEGER>, <CHAR.3.>.*");
         checkExpFails("pow(true,1)",
             "(?s).*Cannot apply 'POW' to arguments of type 'POW.<BOOLEAN>, <INTEGER>.*");
         checkExpFails("mod(x'1100',1)",
             "(?s).*Cannot apply 'MOD' to arguments of type 'MOD.<BINARY.2.>, <INTEGER>.*");
         checkExpFails("mod(1, x'1100')",
             "(?s).*Cannot apply 'MOD' to arguments of type 'MOD.<INTEGER>, <BINARY.2.>.*");
         checkExpFails("abs(x'')",
             "(?s).*Cannot apply 'ABS' to arguments of type 'ABS.<BINARY.0.>.*");
         checkExpFails("ln(x'face12')",
             "(?s).*Cannot apply 'LN' to arguments of type 'LN.<BINARY.3.>.*");
         checkExpFails("log(x'fa')",
             "(?s).*Cannot apply 'LOG' to arguments of type 'LOG.<BINARY.1.>.*");
     }
 
     public void testCaseExpression() {
         checkExp("case 1 when 1 then 'one' end");
         checkExp("case 1 when 1 then 'one' else null end");
         checkExp("case 1 when 1 then 'one' else 'more' end");
         checkExp("case 1 when 1 then 'one' when 2 then null else 'more' end");
         checkExp("case when TRUE then 'true' else 'false' end");
         check("values case when TRUE then 'true' else 'false' end");
         checkExp(
             "CASE 1 WHEN 1 THEN cast(null as integer) WHEN 2 THEN null END");
         checkExp(
             "CASE 1 WHEN 1 THEN cast(null as integer) WHEN 2 THEN cast(null as integer) END");
         checkExp(
             "CASE 1 WHEN 1 THEN null WHEN 2 THEN cast(null as integer) END");
         checkExp(
             "CASE 1 WHEN 1 THEN cast(null as integer) WHEN 2 THEN cast(cast(null as tinyint) as integer) END");
     }
 
     public void testCaseExpressionTypes() {
         checkExpType("case 1 when 1 then 'one' else 'not one' end",
             "CHAR(7)");
         checkExpType("case when 2<1 then 'impossible' end", "CHAR(10)");
         checkExpType("case 'one' when 'two' then 2.00 when 'one' then 1 else 3 end",
             "DECIMAL(3, 2)");
         checkExpType("case 'one' when 'two' then 2 when 'one' then 1.00 else 3 end",
             "DECIMAL(3, 2)");
         checkExpType("case 1 when 1 then 'one' when 2 then null else 'more' end",
             "CHAR(4)");
         checkExpType("case when TRUE then 'true' else 'false' end",
             "CHAR(5)");
         checkExpType("CASE 1 WHEN 1 THEN cast(null as integer) END", "INTEGER");
         checkExpType("CASE 1 WHEN 1 THEN NULL WHEN 2 THEN cast(cast(null as tinyint) as integer) END",
             "INTEGER");
         checkExpType("CASE 1 WHEN 1 THEN cast(null as integer) WHEN 2 THEN cast(null as integer) END",
             "INTEGER");
         checkExpType("CASE 1 WHEN 1 THEN cast(null as integer) WHEN 2 THEN cast(cast(null as tinyint) as integer) END",
             "INTEGER");
         ;
     }
 
     public void testCaseExpressionFails() {
         //varchar not comparable with bit string
         checkExpFails("case 'string' when x'01' then 'zero one' else 'something' end",
             "(?s).*Cannot apply '=' to arguments of type '<CHAR.6.> = <BINARY.1.>'.*");
 
         //all thens and else return null
         checkExpFails("case 1 when 1 then null else null end",
             "(?s).*ELSE clause or at least one THEN clause must be non-NULL.*");
 
         //all thens and else return null
         checkExpFails("case 1 when 1 then null end",
             "(?s).*ELSE clause or at least one THEN clause must be non-NULL.*");
         checkExpFails("case when true and true then 1 " + "when false then 2 "
             + "when false then true " + "else "
             + "case when true then 3 end end",
             "Illegal mixing of types in CASE or COALESCE statement", 1, 8);
     }
 
     public void testNullIf() {
         checkExp("nullif(1,2)");
         checkExpType("nullif(1,2)", "INTEGER");
         checkExpType("nullif('a','b')", "CHAR(1)");
     }
 
     public void testCoalesce() {
         checkExp("coalesce('a','b')");
         checkExpType("coalesce('a','b','c')", "CHAR(1)");
     }
 
     public void testCoalesceFails() {
         checkExpFails("coalesce('a',1)",
             "Illegal mixing of types in CASE or COALESCE statement", 1, 8);
         checkExpFails("coalesce('a','b',1)",
             "Illegal mixing of types in CASE or COALESCE statement", 1, 8);
     }
 
     public void testStringCompare() {
         checkExp("'a' = 'b'");
         checkExp("'a' <> 'b'");
         checkExp("'a' > 'b'");
         checkExp("'a' < 'b'");
         checkExp("'a' >= 'b'");
         checkExp("'a' <= 'b'");
 
         checkExp("cast('' as varchar(1))>cast('' as char(1))");
         checkExp("cast('' as varchar(1))<cast('' as char(1))");
         checkExp("cast('' as varchar(1))>=cast('' as char(1))");
         checkExp("cast('' as varchar(1))<=cast('' as char(1))");
         checkExp("cast('' as varchar(1))=cast('' as char(1))");
         checkExp("cast('' as varchar(1))<>cast('' as char(1))");
     }
 
     public void testStringCompareType() {
         checkExpType("'a' = 'b'", "BOOLEAN");
         checkExpType("'a' <> 'b'", "BOOLEAN");
         checkExpType("'a' > 'b'", "BOOLEAN");
         checkExpType("'a' < 'b'", "BOOLEAN");
         checkExpType("'a' >= 'b'", "BOOLEAN");
         checkExpType("'a' <= 'b'", "BOOLEAN");
     }
 
     public void testConcat() {
         checkExp("'a'||'b'");
         checkExp("x'12'||x'34'");
         checkExpType("'a'||'b'", "VARCHAR(2)");
         checkExpType("cast('a' as char(1))||cast('b' as char(2))", "VARCHAR(3)");
         checkExpType("'a'||'b'||'c'", "VARCHAR(3)");
         checkExpType("'a'||'b'||'cde'||'f'", "VARCHAR(6)");
         checkExp("_iso-8859-6'a'||_iso-8859-6'b'||_iso-8859-6'c'");
     }
 
     public void testConcatWithCharset() {
         checkCharset(
             "_iso-8859-6'a'||_iso-8859-6'b'||_iso-8859-6'c'",
             Charset.forName("ISO-8859-6"));
     }
 
     public void testConcatFails() {
         checkExpFails("'a'||x'ff'",
             "(?s).*Cannot apply '\\|\\|' to arguments of type '<CHAR.1.> \\|\\| <BINARY.1.>'"
             + ".*Supported form.s.: '<CHAR> \\|\\| <CHAR>'"
             + ".*'<VARCHAR> \\|\\| <VARCHAR>'"
             + ".*'<BINARY> \\|\\| <BINARY>'"
             + ".*'<VARBINARY> \\|\\| <VARBINARY>'.*");
     }
 
     public void testBetween() {
         checkExp("1 between 2 and 3");
         checkExp("'a' between 'b' and 'c'");
         checkExpFails("'' between 2 and 3", "(?s).*Cannot apply.*");
     }
 
     public void testCharsetMismatch() {
         checkExpFails("''=_shift_jis''",
             "(?s).*Cannot apply .* to the two different charsets.*");
         checkExpFails("''<>_shift_jis''",
             "(?s).*Cannot apply .* to the two different charsets.*");
         checkExpFails("''>_shift_jis''",
             "(?s).*Cannot apply .* to the two different charsets.*");
         checkExpFails("''<_shift_jis''",
             "(?s).*Cannot apply .* to the two different charsets.*");
         checkExpFails("''<=_shift_jis''",
             "(?s).*Cannot apply .* to the two different charsets.*");
         checkExpFails("''>=_shift_jis''",
             "(?s).*Cannot apply .* to the two different charsets.*");
         checkExpFails("''||_shift_jis''", "(?s).*");
         checkExpFails("'a'||'b'||_iso-8859-6'c'", "(?s).*");
     }
 
     // FIXME jvs 2-Feb-2005: all collation-related tests are disabled due to
     // dtbug 280
 
     public void _testSimpleCollate() {
         checkExp("'s' collate latin1$en$1");
         checkExpType("'s' collate latin1$en$1", "CHAR(1)");
         checkCollation("'s'", "ISO-8859-1$en_US$primary",
             SqlCollation.Coercibility.Coercible);
         checkCollation("'s' collate latin1$sv$3", "ISO-8859-1$sv$3",
             SqlCollation.Coercibility.Explicit);
     }
 
     public void _testCharsetAndCollateMismatch() {
         //todo
         checkExpFails("_shift_jis's' collate latin1$en$1", "?");
     }
 
     public void _testDyadicCollateCompare() {
         checkExp("'s' collate latin1$en$1 < 't'");
         checkExp("'t' > 's' collate latin1$en$1");
         checkExp("'s' collate latin1$en$1 <> 't' collate latin1$en$1");
     }
 
     public void _testDyadicCompareCollateFails() {
         //two different explicit collations. difference in strength
         checkExpFails("'s' collate latin1$en$1 <= 't' collate latin1$en$2",
             "(?s).*Two explicit different collations.*are illegal.*");
 
         //two different explicit collations. difference in language
         checkExpFails("'s' collate latin1$sv$1 >= 't' collate latin1$en$1",
             "(?s).*Two explicit different collations.*are illegal.*");
     }
 
     public void _testDyadicCollateOperator() {
         checkCollation("'a' || 'b'", "ISO-8859-1$en_US$primary",
             SqlCollation.Coercibility.Coercible);
         checkCollation("'a' collate latin1$sv$3 || 'b'", "ISO-8859-1$sv$3",
             SqlCollation.Coercibility.Explicit);
         checkCollation("'a' collate latin1$sv$3 || 'b' collate latin1$sv$3",
             "ISO-8859-1$sv$3", SqlCollation.Coercibility.Explicit);
     }
 
     public void testCharLength() {
         checkExp("char_length('string')");
         checkExp("char_length(_shift_jis'string')");
         checkExp("character_length('string')");
         checkExpType("char_length('string')", "INTEGER");
         checkExpType("character_length('string')", "INTEGER");
     }
 
     public void testUpperLower() {
         checkExp("upper(_shift_jis'sadf')");
         checkExp("lower(n'sadf')");
         checkExpType("lower('sadf')", "CHAR(4)");
         checkExpFails("upper(123)",
             "(?s).*Cannot apply 'UPPER' to arguments of type 'UPPER.<INTEGER>.'.*");
     }
 
     public void testPosition() {
         checkExp("position('mouse' in 'house')");
         checkExp("position(x'11' in x'100110')");
         checkExp("position(x'abcd' in x'')");
         checkExpType("position('mouse' in 'house')", "INTEGER");
         checkExpFails("position(x'1234' in '110')",
             "(?s).*Cannot apply 'POSITION' to arguments of type 'POSITION.<BINARY.2.> IN <CHAR.3.>.'.*");
     }
 
     public void testTrim() {
         checkExp("trim('mustache' FROM 'beard')");
         checkExp("trim(both 'mustache' FROM 'beard')");
         checkExp("trim(leading 'mustache' FROM 'beard')");
         checkExp("trim(trailing 'mustache' FROM 'beard')");
         checkExpType("trim('mustache' FROM 'beard')", "CHAR(5)");
 
         if (todo) {
             final SqlCollation.Coercibility expectedCoercibility = null;
             checkCollation("trim('mustache' FROM 'beard')", "CHAR(5)", expectedCoercibility);
         }
 
     }
 
     public void testTrimFails() {
         checkExpFails("trim(123 FROM 'beard')",
             "(?s).*Cannot apply 'TRIM' to arguments of type.*");
         checkExpFails("trim('a' FROM 123)",
             "(?s).*Cannot apply 'TRIM' to arguments of type.*");
         checkExpFails("trim('a' FROM _shift_jis'b')",
             "(?s).*not comparable to each other.*");
     }
 
     public void _testConvertAndTranslate() {
         checkExp("convert('abc' using conversion)");
         checkExp("translate('abc' using translation)");
     }
 
     public void testOverlay() {
         checkExp("overlay('ABCdef' placing 'abc' from 1)");
         checkExp("overlay('ABCdef' placing 'abc' from 1 for 3)");
         checkExpFails("overlay('ABCdef' placing 'abc' from '1' for 3)",
             "(?s).*OVERLAY\\(<CHAR> PLACING <CHAR> FROM <INTEGER>\\).*");
         checkExpType("overlay('ABCdef' placing 'abc' from 1 for 3)",
             "CHAR(9)");
 
         if (todo) {
             checkCollation("overlay('ABCdef' placing 'abc' collate latin1$sv from 1 for 3)",
                 "ISO-8859-1$sv", SqlCollation.Coercibility.Explicit);
         }
     }
 
     public void testSubstring() {
         checkExp("substring('a' FROM 1)");
         checkExp("substring('a' FROM 1 FOR 3)");
         checkExp("substring('a' FROM 'reg' FOR '\\')");
         checkExp("substring(x'ff' FROM 1  FOR 2)"); //binary string
 
         checkExpType("substring('10' FROM 1  FOR 2)", "VARCHAR(2)");
         checkExpType("substring('1000' FROM '1'  FOR 'w')", "VARCHAR(4)");
         checkExpType("substring(cast(' 100 ' as CHAR(99)) FROM '1'  FOR 'w')",
             "VARCHAR(99)");
         checkExpType("substring(x'10456b' FROM 1  FOR 2)", "VARBINARY(3)");
 
         checkCharset(
             "substring('10' FROM 1  FOR 2)",
             Charset.forName("latin1"));
         checkCharset(
             "substring(_shift_jis'10' FROM 1  FOR 2)",
             Charset.forName("SHIFT_JIS"));
     }
 
 
     public void testSubstringFails() {
         checkExpFails("substring('a' from 1 for 'b')",
             "(?s).*Cannot apply 'SUBSTRING' to arguments of type.*");
         checkExpFails("substring(_shift_jis'10' FROM '0' FOR '\\')",
             "(?s).* not comparable to each other.*");
         checkExpFails("substring('10' FROM _shift_jis'0' FOR '\\')",
             "(?s).* not comparable to each other.*");
         checkExpFails("substring('10' FROM '0' FOR _shift_jis'\\')",
             "(?s).* not comparable to each other.*");
     }
 
     public void testLikeAndSimilar() {
         checkExp("'a' like 'b'");
         checkExp("'a' like 'b'");
         checkExp("'a' similar to 'b'");
         checkExp("'a' similar to 'b' escape 'c'");
     }
 
     public void _testLikeAndSimilarFails() {
         checkExpFails("'a' like _shift_jis'b'  escape 'c'",
             "(?s).*Operands _ISO-8859-1.a. COLLATE ISO-8859-1.en_US.primary, _SHIFT_JIS.b..*");
         checkExpFails("'a' similar to _shift_jis'b'  escape 'c'",
             "(?s).*Operands _ISO-8859-1.a. COLLATE ISO-8859-1.en_US.primary, _SHIFT_JIS.b..*");
 
         checkExpFails("'a' similar to 'b' collate shift_jis$jp  escape 'c'",
             "(?s).*Operands _ISO-8859-1.a. COLLATE ISO-8859-1.en_US.primary, _ISO-8859-1.b. COLLATE SHIFT_JIS.jp.primary.*");
     }
 
     public void testNull() {
         checkFails("values 1.0 + NULL", "(?s).*Illegal use of .NULL.*");
         checkExpFails("1.0 + NULL", "(?s).*Illegal use of .NULL.*");
     }
 
     public void testNullCast() {
         checkExpType("cast(null as tinyint)", "TINYINT");
         checkExpType("cast(null as smallint)", "SMALLINT");
         checkExpType("cast(null as integer)", "INTEGER");
         checkExpType("cast(null as bigint)", "BIGINT");
         checkExpType("cast(null as float)", "FLOAT");
         checkExpType("cast(null as real)", "REAL");
         checkExpType("cast(null as double)", "DOUBLE");
         checkExpType("cast(null as boolean)", "BOOLEAN");
         checkExpType("cast(null as varchar(1))", "VARCHAR(1)");
         checkExpType("cast(null as char(1))", "CHAR(1)");
         checkExpType("cast(null as binary(1))", "BINARY(1)");
         checkExpType("cast(null as date)", "DATE");
         checkExpType("cast(null as time)", "TIME");
         checkExpType("cast(null as timestamp)", "TIMESTAMP");
         checkExpType("cast(null as decimal)", "DECIMAL");
         checkExpType("cast(null as varbinary(1))", "VARBINARY(1)");
 
         checkExp("cast(null as integer), cast(null as char(1))");
     }
 
     public void testCastTypeToType() {
         checkExpType("cast(123 as varchar(3))", "VARCHAR(3)");
         checkExpType("cast(123 as char(3))", "CHAR(3)");
         checkExpType("cast('123' as integer)", "INTEGER");
         checkExpType("cast('123' as double)", "DOUBLE");
         checkExpType("cast('1.0' as real)", "REAL");
         checkExpType("cast(1.0 as tinyint)", "TINYINT");
         checkExpType("cast(1 as tinyint)", "TINYINT");
         checkExpType("cast(1.0 as smallint)", "SMALLINT");
         checkExpType("cast(1 as integer)", "INTEGER");
         checkExpType("cast(1.0 as integer)", "INTEGER");
         checkExpType("cast(1.0 as bigint)", "BIGINT");
         checkExpType("cast(1 as bigint)", "BIGINT");
         checkExpType("cast(1.0 as float)", "FLOAT");
         checkExpType("cast(1 as float)", "FLOAT");
         checkExpType("cast(1.0 as real)", "REAL");
         checkExpType("cast(1 as real)", "REAL");
         checkExpType("cast(1.0 as double)", "DOUBLE");
         checkExpType("cast(1 as double)", "DOUBLE");
         checkExpType("cast(null as boolean)", "BOOLEAN");
         checkExpType("cast('abc' as varchar(1))", "VARCHAR(1)");
         checkExpType("cast('abc' as char(1))", "CHAR(1)");
         checkExpType("cast(x'ff' as binary(1))", "BINARY(1)");
         checkExpType("cast(multiset[1] as double multiset)", "DOUBLE MULTISET");
         checkExpType("cast(multiset['abc'] as integer multiset)", "INTEGER MULTISET");
     }
 
     public void testCastFails() {
         checkExpFails("cast('foo' as bar)",
             "(?s).*Unknown datatype name 'BAR'");
         checkExpFails("cast(multiset[1] as integer)",
             "(?s).*Cast function cannot convert value of type INTEGER MULTISET to type INTEGER");
     }
 
     public void testDateTime() {
         // LOCAL_TIME
         checkExp("LOCALTIME(3)");
         checkExp("LOCALTIME"); //    fix sqlcontext later.
         checkExpFails("LOCALTIME(1+2)",
             "Argument to function 'LOCALTIME' must be a positive integer literal");
         checkExpFails("LOCALTIME()",
             "No match found for function signature LOCALTIME..", 1, 8);
         checkExpType("LOCALTIME", "TIME(0)"); //  NOT NULL, with TZ ?
         checkExpFails("LOCALTIME(-1)",
             "Argument to function 'LOCALTIME' must be a positive integer literal"); // i guess -s1 is an expression?
         checkExpFails("LOCALTIME('foo')",
             "Argument to function 'LOCALTIME' must be a positive integer literal");
 
         // LOCALTIMESTAMP
         checkExp("LOCALTIMESTAMP(3)");
         checkExp("LOCALTIMESTAMP"); //    fix sqlcontext later.
         checkExpFails("LOCALTIMESTAMP(1+2)",
             "Argument to function 'LOCALTIMESTAMP' must be a positive integer literal");
         checkExpFails("LOCALTIMESTAMP()",
             "No match found for function signature LOCALTIMESTAMP..", 1, 8);
         checkExpType("LOCALTIMESTAMP", "TIMESTAMP(0)"); //  NOT NULL, with TZ ?
         checkExpFails("LOCALTIMESTAMP(-1)",
             "Argument to function 'LOCALTIMESTAMP' must be a positive integer literal"); // i guess -s1 is an expression?
         checkExpFails("LOCALTIMESTAMP('foo')",
             "Argument to function 'LOCALTIMESTAMP' must be a positive integer literal");
 
         // CURRENT_DATE
         checkExpFails("CURRENT_DATE(3)",
             "No match found for function signature CURRENT_DATE..NUMERIC..", 1, 8);
         checkExp("CURRENT_DATE"); //    fix sqlcontext later.
         checkExpFails("CURRENT_DATE(1+2)",
             "No match found for function signature CURRENT_DATE..NUMERIC..", 1, 8);
         checkExpFails("CURRENT_DATE()",
             "No match found for function signature CURRENT_DATE..", 1, 8);
         checkExpType("CURRENT_DATE", "DATE"); //  NOT NULL, with TZ?
         checkExpFails("CURRENT_DATE(-1)",
             "No match found for function signature CURRENT_DATE..NUMERIC..", 1, 8); // i guess -s1 is an expression?
         checkExpFails("CURRENT_DATE('foo')", "(?s).*");
 
         // current_time
         checkExp("current_time(3)");
         checkExp("current_time"); //    fix sqlcontext later.
         checkExpFails("current_time(1+2)",
             "Argument to function 'CURRENT_TIME' must be a positive integer literal");
         checkExpFails("current_time()",
             "No match found for function signature CURRENT_TIME..", 1, 8);
         checkExpType("current_time", "TIME(0)"); //  NOT NULL, with TZ ?
         checkExpFails("current_time(-1)",
             "Argument to function 'CURRENT_TIME' must be a positive integer literal");
         checkExpFails("current_time('foo')",
             "Argument to function 'CURRENT_TIME' must be a positive integer literal");
 
         // current_timestamp
         checkExp("CURRENT_TIMESTAMP(3)");
         checkExp("CURRENT_TIMESTAMP"); //    fix sqlcontext later.
         checkExpFails("CURRENT_TIMESTAMP(1+2)",
             "Argument to function 'CURRENT_TIMESTAMP' must be a positive integer literal");
         checkExpFails("CURRENT_TIMESTAMP()",
             "No match found for function signature CURRENT_TIMESTAMP..", 1, 8);
         checkExpType("CURRENT_TIMESTAMP", "TIMESTAMP(0)"); //  NOT NULL, with TZ ?
         checkExpType("CURRENT_TIMESTAMP(2)", "TIMESTAMP(2)"); //  NOT NULL, with TZ ?
         checkExpFails("CURRENT_TIMESTAMP(-1)",
             "Argument to function 'CURRENT_TIMESTAMP' must be a positive integer literal");
         checkExpFails("CURRENT_TIMESTAMP('foo')",
             "Argument to function 'CURRENT_TIMESTAMP' must be a positive integer literal");
 
         // Date literals
         checkExp("DATE '2004-12-01'");
         checkExp("TIME '12:01:01'");
         checkExp("TIME '11:59:59.99'");
         checkExp("TIME '12:01:01.001'");
         checkExp("TIMESTAMP '2004-12-01 12:01:01'");
         checkExp("TIMESTAMP '2004-12-01 12:01:01.001'");
 
         // REVIEW: Can't think of any date/time/ts literals that will parse, but not validate.
     }
 
     /**
      * Tests casting to/from date/time types.
      */
     public void testDateTimeCast() {
         checkExpFails("CAST(1 as DATE)",
             "Cast function cannot convert value of type INTEGER to type DATE");
         checkExp("CAST(DATE '2001-12-21' AS VARCHAR(10))");
         checkExp("CAST( '2001-12-21' AS DATE)");
         checkExp("CAST( TIMESTAMP '2001-12-21 10:12:21' AS VARCHAR(20))");
         checkExp("CAST( TIME '10:12:21' AS VARCHAR(20))");
         checkExp("CAST( '10:12:21' AS TIME)");
         checkExp("CAST( '2004-12-21 10:12:21' AS TIMESTAMP)");
     }
 
     public void testInvalidFunction() {
         checkExpFails("foo()",
             "No match found for function signature FOO..", 1, 8);
         checkExpFails("mod(123)",
             "Invalid number of arguments to function 'MOD'. Was expecting 2 arguments", 1, 8);
     }
 
     public void testJdbcFunctionCall() {
         checkExp("{fn log(1)}");
         checkExp("{fn locate('','')}");
         checkExp("{fn insert('',1,2,'')}");
         checkExpFails("{fn insert('','',1,2)}", "(?s).*.*");
         checkExpFails("{fn insert('','',1)}", "(?s).*4.*");
         checkExpFails("{fn locate('','',1)}", "(?s).*"); //todo this is legal jdbc syntax, just that currently the 3 ops call is not implemented in the system
         checkExpFails("{fn log('1')}",
             "(?s).*Cannot apply.*fn LOG..<CHAR.1.>.*");
         checkExpFails("{fn log(1,1)}",
             "(?s).*Encountered .fn LOG. with 2 parameter.s.; was expecting 1 parameter.s.*");
         checkExpFails("{fn fn(1)}",
             "(?s).*Function '.fn FN.' is not defined.*");
         checkExpFails("{fn hahaha(1)}",
             "(?s).*Function '.fn HAHAHA.' is not defined.*");
     }
 
     // REVIEW jvs 2-Feb-2005:  I am disabling this test because I removed
     // the corresponding support from the parser.  Where in the standard
     // does it state that you're supposed to be able to quote keywords
     // for builtin functions?
     public void _testQuotedFunction() {
         checkExp("\"CAST\"(1 as double)");
         checkExp("\"POSITION\"('b' in 'alphabet')");
 
         //convert and translate not yet implemented
         //        checkExp("\"CONVERT\"('b' using converstion)");
         //        checkExp("\"TRANSLATE\"('b' using translation)");
         checkExp("\"OVERLAY\"('a' PLAcing 'b' from 1)");
         checkExp("\"SUBSTRING\"('a' from 1)");
         checkExp("\"TRIM\"('b')");
     }
 
     public void testRowtype() {
         check("values (1),(2),(1)");
         check("values (1,'1'),(2,'2')");
         checkFails("values ('1'),(2)",
             "Values passed to VALUES operator must have compatible types", 1, 1);
         if (todo) {
             checkType("values (1),(2.0),(3)", "ROWTYPE(DOUBLE)");
         }
     }
 
     public void testMultiset() {
         checkExpType("multiset[1]","INTEGER MULTISET");
         checkExpType("multiset[1,2.3]","DECIMAL(2, 1) MULTISET");
         checkExpType("multiset[1,2.3, 4]","DECIMAL(2, 1) MULTISET");
         checkExpType("multiset['1','22', '333','22']","CHAR(3) MULTISET");
         checkExpFails("multiset[1, '2']", "Parameters must be of the same type", 1, 23);
         checkExpType("multiset[ROW(1,2)]","RecordType(INTEGER EXPR$0, INTEGER EXPR$1) MULTISET");
         checkExpType("multiset[ROW(1,2),ROW(2,5)]","RecordType(INTEGER EXPR$0, INTEGER EXPR$1) MULTISET");
         checkExpType("multiset[ROW(1,2),ROW(3.4,5.4)]","RecordType(DECIMAL(2, 1) EXPR$0, DECIMAL(2, 1) EXPR$1) MULTISET");
 
         checkExpType("multiset(select*from emp)",
                      "RecordType(INTEGER EMPNO, VARCHAR(20) ENAME, VARCHAR(10) JOB, INTEGER MGR, DATE HIREDATE, INTEGER SAL, INTEGER COMM, INTEGER DEPTNO) MULTISET");
     }
 
     public void testMultisetSetOperators() {
         checkExp("multiset[1] multiset union multiset[1,2.3]");
         checkExpType("multiset[1] multiset union multiset[1,2.3]", "DECIMAL(2, 1) MULTISET");
         checkExp("multiset[1] multiset union all multiset[1,2.3]");
         checkExp("multiset[1] multiset except multiset[1,2.3]");
         checkExp("multiset[1] multiset except all multiset[1,2.3]");
         checkExp("multiset[1] multiset intersect multiset[1,2.3]");
         checkExp("multiset[1] multiset intersect all multiset[1,2.3]");
 
         checkExpFails("multiset[1, '2'] multiset union multiset[1]", "Parameters must be of the same type", 1, 23);
         checkExp("multiset[ROW(1,2)] multiset intersect multiset[row(3,4)]");
         if (todo) {
             checkExpFails("multiset[ROW(1,'2')] multiset union multiset[ROW(1,2)]", "Parameters must be of the same type", 1, 23);
         }
     }
 
     public void testSubMultisetOf() {
         checkExpType("multiset[1] submultiset of multiset[1,2.3]", "BOOLEAN");
         checkExpType("multiset[1] submultiset of multiset[1]", "BOOLEAN");
 
         checkExpFails("multiset[1, '2'] submultiset of multiset[1]", "Parameters must be of the same type", 1, 23);
         checkExp("multiset[ROW(1,2)] submultiset of multiset[row(3,4)]");
     }
 
     public void testElement() {
         checkExpType("element(multiset[1])", "INTEGER");
         checkExpType("1.0+element(multiset[1])", "DECIMAL(2, 1)");
         checkExpType("element(multiset['1'])", "CHAR(1)");
         checkExpType("element(multiset[1e-2])", "DOUBLE");
         checkExpType("element(multiset[multiset[cast(null as tinyint)]])", "TINYINT MULTISET");
     }
 
     public void testMemberOf() {
         checkExpType("1 member of multiset[1]", "BOOLEAN");
         checkExpFails("1 member of multiset['1']", "Cannot compare values of types 'INTEGER', 'CHAR\\(1\\)'", 1, 32);
     }
 
     public void testIsASet() {
         checkExp("multiset[1] is a set");
         checkExp("multiset['1'] is a set");
         checkExpFails("'a' is a set", ".*Cannot apply 'IS A SET' to.*");
     }
 
     public void testCardinality() {
         checkExpType("cardinality(multiset[1])", "INTEGER");
         checkExpType("cardinality(multiset['1'])", "INTEGER");
         checkExpFails("cardinality('a')", "Cannot apply 'CARDINALITY' to arguments of type 'CARDINALITY.<CHAR.1.>.'. Supported form.s.: 'CARDINALITY.<MULTISET>.'", 1, 8);
     }
 
     public void testIntervalTimeUnitEnumeration() {
         // Since there is validation code relaying on the fact that the
         // enumerated time unit ordinals in SqlIntervalQualifier starts with 0
         // and ends with 5, this test is here to make sure that if someone
         // changes how the time untis are setup, an early feedback will be
         // generated by this test.
         assertEquals(0, SqlIntervalQualifier.TimeUnit.Year.getOrdinal());
         assertEquals(1, SqlIntervalQualifier.TimeUnit.Month.getOrdinal());
         assertEquals(2, SqlIntervalQualifier.TimeUnit.Day.getOrdinal());
         assertEquals(3, SqlIntervalQualifier.TimeUnit.Hour.getOrdinal());
         assertEquals(4, SqlIntervalQualifier.TimeUnit.Minute.getOrdinal());
         assertEquals(5, SqlIntervalQualifier.TimeUnit.Second.getOrdinal());
         boolean b =
             (SqlIntervalQualifier.TimeUnit.Year.getOrdinal() <
             SqlIntervalQualifier.TimeUnit.Month.getOrdinal())
             &&
             (SqlIntervalQualifier.TimeUnit.Month.getOrdinal() <
             SqlIntervalQualifier.TimeUnit.Day.getOrdinal())
             &&
             (SqlIntervalQualifier.TimeUnit.Day.getOrdinal() <
             SqlIntervalQualifier.TimeUnit.Hour.getOrdinal())
             &&
             (SqlIntervalQualifier.TimeUnit.Hour.getOrdinal() <
             SqlIntervalQualifier.TimeUnit.Minute.getOrdinal())
             &&
             (SqlIntervalQualifier.TimeUnit.Minute.getOrdinal() <
             SqlIntervalQualifier.TimeUnit.Second.getOrdinal());
         assertTrue(b);
     }
 
     public void testIntervalLiteral() {
         checkExpType("INTERVAL '1' DAY", "INTERVAL DAY");
         checkExpType("INTERVAL '1' DAY(4)", "INTERVAL DAY(4)");
         checkExpType("INTERVAL '1' HOUR", "INTERVAL HOUR");
         checkExpType("INTERVAL '1' MINUTE", "INTERVAL MINUTE");
         checkExpType("INTERVAL '1' SECOND", "INTERVAL SECOND");
         checkExpType("INTERVAL '1' SECOND(3)", "INTERVAL SECOND(3)");
         checkExpType("INTERVAL '1' SECOND(3, 4)", "INTERVAL SECOND(3, 4)");
         checkExpType("INTERVAL '1 2:3:4' DAY TO SECOND", "INTERVAL DAY TO SECOND");
         checkExpType("INTERVAL '1 2:3:4' DAY(4) TO SECOND(4)", "INTERVAL DAY(4) TO SECOND(4)");
 
         checkExpType("INTERVAL '1' YEAR", "INTERVAL YEAR");
         checkExpType("INTERVAL '1' MONTH", "INTERVAL MONTH");
         checkExpType("INTERVAL '1-2' YEAR TO MONTH", "INTERVAL YEAR TO MONTH");
     }
 
     public void testIntervalOperators() {
         checkExpType("interval '1' day + interval '1' DAY(4)", "INTERVAL DAY(4)");
         checkExpType("interval '1' day(5) + interval '1' DAY", "INTERVAL DAY(5)");
         checkExpType("interval '1' day + interval '1' HOUR(10)", "INTERVAL DAY TO HOUR");
         checkExpType("interval '1' day + interval '1' MINUTE", "INTERVAL DAY TO MINUTE");
         checkExpType("interval '1' day + interval '1' second", "INTERVAL DAY TO SECOND");
 
         checkExpType("interval '1:2' hour to minute + interval '1' second", "INTERVAL HOUR TO SECOND");
         checkExpType("interval '1:3' hour to minute + interval '1 1:2:3.4' day to second", "INTERVAL DAY TO SECOND");
         checkExpType("interval '1:2' hour to minute + interval '1 1' day to hour", "INTERVAL DAY TO MINUTE");
         checkExpType("interval '1:2' hour to minute + interval '1 1' day to hour", "INTERVAL DAY TO MINUTE");
         checkExpType("interval '1 2' day to hour + interval '1:1' minute to second", "INTERVAL DAY TO SECOND");
 
         checkExpType("interval '1' year + interval '1' month", "INTERVAL YEAR TO MONTH");
         checkExpType("interval '1' day - interval '1' hour", "INTERVAL DAY TO HOUR");
         checkExpType("interval '1' year - interval '1' month", "INTERVAL YEAR TO MONTH");
         checkExpType("interval '1' month - interval '1' year", "INTERVAL YEAR TO MONTH");
         checkExpFails("interval '1' year + interval '1' day");
         checkExpFails("interval '1' month + interval '1' second");
         checkExpFails("interval '1' year - interval '1' day");
         checkExpFails("interval '1' month - interval '1' second");
 
         // mixing between datetime and interval
 //todo        checkExpType("date '1234-12-12' + INTERVAL '1' month + interval '1' day","DATE");
 //todo        checkExpFails("date '1234-12-12' + (INTERVAL '1' month + interval '1' day)","?");
 
         // multiply operator
         checkExpType("interval '1' year * 2", "INTERVAL YEAR");
         checkExpType("1.234*interval '1 1:2:3' day to second ", "INTERVAL DAY TO SECOND");
 
         // division operator
         checkExpType("interval '1' month / 0.1", "INTERVAL MONTH");
         checkExpType("interval '1-2' year TO month / 0.1e-9", "INTERVAL YEAR TO MONTH");
         checkExpFails("1.234/interval '1 1:2:3' day to second ", "(?s).*Cannot apply '/' to arguments of type '<DECIMAL.4, 3.> / <INTERVAL DAY TO SECOND>'.*");
     }
 
 
     public void checkWinClauseExp(String sql, String expectedMsgPattern) {
         sql = "select * from emp " + sql;
         logger.info(sql);
         assertExceptionIsThrown(sql, expectedMsgPattern);
     }
 
     public void checkWindowExpFails(String sql, String expectedMsgPattern) {
         sql = "select * from emp " + sql;
         logger.info(sql);
         assertExceptionIsThrown(sql, expectedMsgPattern);
     }
 
      // test window partition clause. See SQL 2003 specification for detail
      public void _testWinPartClause()
      {
 
          // Test specified collation, window clause syntax rule 4,5.
 
          //:
 
      }
 
     public void _testWinFunc()
     {
         // Definition 6.10
 
         // Window functions may only appear in the <select list> of a <query specification> or <select statement: single row>, or the <order by clause> of a simple table query.  See 4.15.3 for detail
 
         // todo: test case for rule 1
 
         // rule 3, a)
         // todo:  window function in <order by clause> example
 
         // scope reference
 
         // rule 4,
 
 
         // valid window functions
         checkWinFuncExpWithWinClause("sum(sal)", null);
 
         // row_number function
         checkWinFuncExpWithWinClause("row_number()", null);
 
         // rank function type
         checkWinFuncExpWithWinClause("rank()", null);
         checkWinFuncExpWithWinClause("dense_rank()", null);
         checkWinFuncExpWithWinClause("percent_rank()", null);
         checkWinFuncExpWithWinClause("cume_dist()", null);
 
         // rule 6
         // a)
         // missing order by in window clause
 
         assertExceptionIsThrown("select rank() over w from emps window w as (partition by dept)", null);
         assertExceptionIsThrown("select dense_rank() over w from emps window w as (partition by dept)", null);
 
         // missing order by in-line window definition
         assertExceptionIsThrown("select rank() over (partition by dept) from emps", null);
         assertExceptionIsThrown("select dense_rank() over (partition by dept) from emps ", null);
 
         // b)
         // window framing defined in window clause
         assertExceptionIsThrown("select rank() over w from emps window w as (row 2 preceding )", null);
         assertExceptionIsThrown("select dense_rank() over w from emps window w as (row 2 preceding)", null);
         assertExceptionIsThrown("select prercent_rank() over w from emps window w as (row 2 preceding )", null);
         assertExceptionIsThrown("select cume_dist() over w from emps window w as (row 2 preceding)", null);
         // window framing defined in in-line window
         assertExceptionIsThrown("select rank() over (row 2 preceding ) from emps ", null);
         assertExceptionIsThrown("select dense_rank() over (row 2 preceding ) from emps ", null);
         assertExceptionIsThrown("select prercent_rank() over (row 2 preceding ) from emps", null);
         assertExceptionIsThrown("select cume_dist() over (row 2 preceding ) from emps ", null);
 
 
 
 
         // invalid column reference
         checkWinFuncExpWithWinClause("sum(invalidColumn)", null);
 
 
         // invalid window functions
         checkWinFuncExpWithWinClause("invalidFun(sal)", null);
 
         // rule 10. no distinct allow
         checkWinFuncExpWithWinClause("select dictinct sum(sal) over w from emps window as w (order by deptno)", null);
     }
 
     public void _testInlineWinDef()
     {
 
     }
 
     public void testWindowClause()
     {
         // -----------------------------------
         // --   positive testings           --
         // -----------------------------------
         // correct syntax:
         checkWinClauseExp("window w as (partition by sal order by deptno rows 2 preceding)", null);
         // define window on an existing window
         checkWinClauseExp("window w as (partition by sal), w1 as (w)", null);
 
         // -----------------------------------
         // --   negative testings           --
         // -----------------------------------
         // rule 11
         // a)
         // missing window order clause.
         checkWindowExpFails("window w as (range 100 preceding)", null);
         // order by number
         checkWindowExpFails("window w as (order by sal range 100 preceding)", null);
         // order by date
         checkWindowExpFails("window w as (order by hiredate range 100 preceding)", null);
         // order by string, should fail
         checkWindowExpFails("window w as (order by name range 100 preceding)", null);
         // todo: interval test ???
 
 
         // b)
         // valid
         //checkWindowExpFails("window w as (row 2 preceding)", null);
         // invalid tests
         // exact numeric for the unsigned value specification
         // error message: invalid preceding or following size in window function
 //        checkWindowExpFails("window w as (row 2.5 preceding)", null);
 //        checkWindowExpFails("window w as (row -2.5 preceding)", null);
 //        checkWindowExpFails("window w as (row -2 preceding)", null);
 
     }
 
     public void _testWindowClause() {
 
         // -----------------------------------
         // --   negative testings           --
         // -----------------------------------
         // reference undefined xyz column
         checkWinClauseExp("window w as (partition by xyz)", null);
 
         // window defintion is empty
         checkWindowExpFails("window w as ()", null);
 
 
         // syntax rule 2
         // REVIEW: klo 11-9-2005. What does the following rule mean?
         //<new window name> NWN1 shall not be contained in the scope of another <new window name> NWN2
         //such that NWN1 and NWN2 are equivalent.
         // duplidate window name
         checkWindowExpFails("window w as (partition by sal), window w as (partition by sal)", null);
 
 
         // syntax rule 6
 
         // syntax rule 7
 
 
         // ------------------------------------
         // ---- window frame between tests ----
         // ------------------------------------
         // bound 1 shall not specify UNBOUNDED FOLLOWING
         checkWindowExpFails("window w as (between unbounded following and bound2", null);
 
         // bound 2 shall not specify UNBOUNDED PRECEDING
         checkWindowExpFails("window w as (between bound1 and unbounded preceding", null);
         //
         checkWindowExpFails("window w as (between current row and window frame preceding", null);
         //
         checkWindowExpFails("window w as (between bound1 and current row", null);
         //
         checkWindowExpFails("window w as (between bound1 and window frame preceding", null);
 
         // todo: syntax rule 10. Do we understand this rule? Do the following test case cover this rule?
         // c)
         checkWindowExpFails("window w as (partition by sal order by deptno), window w as2 (w partition by sal", null);
         // d)
         // valid because existing window does not have a order by clause
         checkWindowExpFails("window w as (partition by sal ), window w as2 (w order by deptno)", null);
         // invalid
         checkWindowExpFails("window w as (partition by sal order by deptno), window w as2 (w order by deptno)", null);
         // e)
         checkWindowExpFails("window w as (partition by sal order by deptno), window w as2 (w range 100 preceding)", null);
 
 
         // rule 12, todo: test scope of window
 
         // rule 13, todo: to understand this rule
 
 
 //        checkWindowExpFails("window w as (partition by sal),
 //        checkWindowExpFails("window w as (partition by non_exist_col order by deptno)");
 //        checkWindowExpFails("window w as (partition by sal order by non_exist_col)");
         // unambiguously reference a column in the window clause
         // select * from emp, emp window w as (partition by sal);
         // select * from emp empalias window w as (partition by sal);
     }
 
     public void checkWinFuncExpWithWinClause(String sql, String expectedMsgPattern ) {
         sql = "select " + sql + " from emp window w as (partition by deptno)";
         assertExceptionIsThrown(sql, expectedMsgPattern);
     }
 
     public void checkWinFuncExp(String sql, String expectedMsgPattern ) {
         sql = "select " + sql + " from emp";
         assertExceptionIsThrown(sql, expectedMsgPattern);
     }
 
     public void testOneWinFunc() {
         checkWinClauseExp("window w as (partition by sal order by deptno rows 2 preceding)", null);
     }
 
     public void testNameResolutionInValuesClause() {
         final String emps = "(select 1 as empno, 'x' as name, 10 as deptno, 'M' as gender, 'San Francisco' as city, 30 as empid, 25 as age from (values (1)))";
         final String depts = "(select 10 as deptno, 'Sales' as name from (values (1)))";
 
         checkFails("select * from " + emps + " join " + depts + NL +
             " on emps.deptno = deptno",
             "Table 'EMPS' not found", 2, 5);
         // this is ok
         check("select * from " + emps + " as e" + NL +
             " join " + depts + " as d" + NL +
             " on e.deptno = d.deptno");
         // fail: ambiguous column in WHERE
         checkFails("select * from " + emps + " as emps," + NL +
             " " + depts + NL +
             "where deptno > 5",
             "Column 'DEPTNO' is ambiguous", 3, 7);
         // fail: ambiguous column reference in ON clause
         checkFails("select * from " + emps + " as e" + NL +
             " join " + depts + " as d" + NL +
             " on e.deptno = deptno",
             "Column 'DEPTNO' is ambiguous", 3, 16);
         // ok: column 'age' is unambiguous
         check("select * from " + emps + " as e" + NL +
             " join " + depts + " as d" + NL +
             " on e.deptno = age");
         // ok: reference to derived column
         check("select * from " + depts + NL +
             " join (select mod(age, 30) as agemod from " + emps + ") " + NL +
             "on deptno = agemod");
         // fail: deptno is ambiguous
         checkFails("select name from " + depts + " " + NL +
             "join (select mod(age, 30) as agemod, deptno from " + emps + ") " + NL +
             "on deptno = agemod",
             "Column 'DEPTNO' is ambiguous", 3, 4);
         // fail: lateral reference
         checkFails("select * from " + emps + " as e," + NL +
             " (select 1, e.deptno from (values(true))) as d",
             "Unknown identifier 'E'", 2, 13);
     }
 
     public void testNestedFrom() {
         checkType("values (true)", "BOOLEAN");
         checkType("select * from (values(true))", "BOOLEAN");
         checkType("select * from (select * from (values(true)))", "BOOLEAN");
         checkType("select * from (select * from (select * from (values(true))))", "BOOLEAN");
         checkType(
             "select * from (" +
             "  select * from (" +
             "    select * from (values(true))" +
             "    union" +
             "    select * from (values (false)))" +
             "  except" +
             "  select * from (values(true)))", "BOOLEAN");
     }
 
     public void testAmbiguousColumn() {
         checkFails("select * from emp join dept" + NL +
             " on emp.deptno = deptno",
             "Column 'DEPTNO' is ambiguous", 2, 18);
         // this is ok
         check("select * from emp as e" + NL +
             " join dept as d" + NL +
             " on e.deptno = d.deptno");
         // fail: ambiguous column in WHERE
         checkFails("select * from emp as emps, dept" + NL +
             "where deptno > 5",
             "Column 'DEPTNO' is ambiguous", 2, 7);
         // fail: alias 'd' obscures original table name 'dept'
         checkFails("select * from emp as emps, dept as d" + NL +
             "where dept.deptno > 5",
             "Unknown identifier 'DEPT'", 2, 7);
         // fail: ambiguous column reference in ON clause
         checkFails("select * from emp as e" + NL +
             " join dept as d" + NL +
             " on e.deptno = deptno",
             "Column 'DEPTNO' is ambiguous", 3, 16);
         // ok: column 'comm' is unambiguous
         check("select * from emp as e" + NL +
             " join dept as d" + NL +
             " on e.deptno = comm");
         // ok: reference to derived column
         check("select * from dept" + NL +
             " join (select mod(comm, 30) as commmod from emp) " + NL +
             "on deptno = commmod");
         // fail: deptno is ambiguous
         checkFails("select name from dept " + NL +
             "join (select mod(comm, 30) as commmod, deptno from emp) " + NL +
             "on deptno = commmod",
             "Column 'DEPTNO' is ambiguous", 3, 4);
         // fail: lateral reference
         checkFails("select * from emp as e," + NL +
             " (select 1, e.deptno from (values(true))) as d",
             "Unknown identifier 'E'", 2, 13);
     }
 
     public void testExpandStar() {
         // dtbug 282
         // "select r.* from sales.depts" gives NPE.
         checkFails("select r.* from dept",
             "Unknown identifier 'R'", 1, 10);
 
         check("select e.* from emp as e");
         check("select emp.* from emp");
 
         // Error message could be better (EMPNO does exist, but it's a column).
         checkFails("select empno.* from emp",
             "Unknown identifier 'EMPNO'", 1, 14);
     }
 
     // todo: implement IN
     public void _testAmbiguousColumnInIn() {
         // ok: cyclic reference
         check("select * from emp as e" + NL +
             "where e.deptno in (" + NL +
             "  select 1 from (values(true)) where e.empno > 10)");
         // ok: cyclic reference
         check("select * from emp as e" + NL +
             "where e.deptno in (" + NL +
             "  select e.deptno from (values(true)))");
     }
 
     public void testDoubleNoAlias() {
         check("select * from emp join dept on true");
         check("select * from emp, dept");
         check("select * from emp cross join dept");
     }
 
     // TODO: is this legal? check that standard
     public void _testDuplicateColumnAliasFails() {
         checkFails("select 1 as a, 2 as b, 3 as a from emp", "xyz");
     }
 
     // NOTE jvs 20-May-2003 -- this is just here as a reminder that GROUP BY
     // validation isn't implemented yet
     public void testInvalidGroupBy(TestCase test) {
         try {
             check("select empno, deptno from emp group by deptno");
         } catch (RuntimeException ex) {
             return;
         }
         test.fail("Expected validation error");
     }
 
     public void testSingleNoAlias() {
         check("select * from emp");
     }
 
     public void testObscuredAliasFails() {
         // It is an error to refer to a table which has been given another
         // alias.
         checkFails("select * from emp as e where exists ("
             + "  select 1 from dept where dept.deptno = emp.deptno)",
             "Table 'EMP' not found", 1, 79);
     }
 
     public void testFromReferenceFails() {
         // You cannot refer to a table ('e2') in the parent scope of a query in
         // the from clause.
         checkFails("select * from emp as e1 where exists (" + NL
             + "  select * from emp as e2, " + NL
             + "    (select * from dept where dept.deptno = e2.deptno))",
             "Table 'E2' not found", 3, 45);
     }
 
     public void testWhereReference() {
         // You can refer to a table ('e1') in the parent scope of a query in
         // the from clause.
         //
         // Note: Oracle10g does not allow this query.
         check("select * from emp as e1 where exists (" + NL
             + "  select * from emp as e2, " + NL
             + "    (select * from dept where dept.deptno = e1.deptno))");
     }
 
     public void testUnionNameResolution() {
         checkFails(
             "select * from emp as e1 where exists (" + NL +
             "  select * from emp as e2, " + NL +
             "  (select deptno from dept as d" + NL +
             "   union" + NL +
             "   select deptno from emp as e3 where deptno = e2.deptno))",
             "Table 'E2' not found", 5, 48);
 
         checkFails("select * from emp" + NL +
             "union" + NL +
             "select * from dept where empno < 10",
             "Unknown identifier 'EMPNO'", 3, 26);
     }
 
     public void testUnionCountMismatchFails() {
         checkFails("select 1,2 from emp" + NL +
             "union" + NL +
             "select 3 from dept",
             "Column count mismatch in UNION",
             3, 8);
     }
 
     public void testUnionTypeMismatchFails() {
         // error here         v
         checkFails("select 1, 2 from emp union select deptno, name from dept",
             "Type mismatch in column 2 of UNION", 1, 9);
     }
 
     public void testUnionTypeMismatchWithStarFails() {
         // error here      v
         checkFails("select * from dept union select 1, 2 from emp",
             "Type mismatch in column 2 of UNION", 1, 8);
     }
 
     public void testUnionTypeMismatchWithValuesFails() {
         // error here          v
         checkFails("values (1, 2, 3), (3, 4, 5), (6, 7, 8) union " + NL +
             "select deptno, name, deptno from dept",
             "Type mismatch in column 2 of UNION", 1, 10);
     }
 
     public void testUnionOfNonQueryFails() {
         checkFails("select 1 from emp union 2",
             "Non-query expression 2 encountered in illegal context near" +
             " line 1, column 25");
     }
 
     public void _testInTooManyColumnsFails() {
         checkFails("select * from emp where deptno in (select deptno,deptno from dept)",
             "xyz");
     }
 
     public void testNaturalCrossJoinFails() {
         checkFails("select * from emp natural cross join dept",
             "Cannot specify condition \\(NATURAL keyword, or ON or USING clause\\) following CROSS JOIN", 1, 33);
     }
 
     public void testCrossJoinUsingFails() {
         checkFails("select * from emp cross join dept using (deptno)",
             "Cannot specify condition \\(NATURAL keyword, or ON or USING clause\\) following CROSS JOIN", 1, 48);
     }
 
     public void testJoinUsing() {
         check("select * from emp join dept using (deptno)");
         // fail: comm exists on one side not the other
         // todo: The error message could be improved.
         checkFails("select * from emp join dept using (deptno, comm)",
             "Column 'COMM' not found in any table", 1, 44);
         // ok to repeat (ok in Oracle10g too)
         check("select * from emp join dept using (deptno, deptno)");
         // inherited column, not found in either side of the join, in the
         // USING clause
         checkFails("select * from dept where exists (" + NL +
             "select 1 from emp join bonus using (dname))",
             "Column 'DNAME' not found in any table", 2, 37);
         // inherited column, found in only one side of the join, in the
         // USING clause
         checkFails("select * from dept where exists (" + NL +
             "select 1 from emp join bonus using (deptno))",
             "Column 'DEPTNO' not found in any table", 2, 37);
     }
 
     public void testCrossJoinOnFails() {
         checkFails("select * from emp cross join dept" + NL +
             " on emp.deptno = dept.deptno",
             "Cannot specify condition \\(NATURAL keyword, or ON or USING clause\\) following CROSS JOIN", 2, 23);
     }
 
     public void testInnerJoinWithoutUsingOrOnFails() {
         checkFails("select * from emp inner join dept "
             + "where emp.deptno = dept.deptno",
             "INNER, LEFT, RIGHT or FULL join requires a condition \\(NATURAL keyword or ON or USING clause\\)", 1, 25);
     }
 
     public void testJoinUsingInvalidColsFails() {
         // todo: Improve error msg
         checkFails("select * from emp left join dept using (gender)",
             "Column 'GENDER' not found in any table", 1, 41);
     }
 
     // todo: Cannot handle '(a join b)' yet -- we see the '(' and expect to
     // see 'select'.
     public void _testJoinUsing() {
         check("select * from (emp join bonus using (job))" + NL +
             "join dept using (deptno)");
         // cannot alias a JOIN (actually this is a parser error, but who's
         // counting?)
         checkFails("select * from (emp join bonus using (job)) as x" + NL +
             "join dept using (deptno)",
             "as wrong here");
         checkFails("select * from (emp join bonus using (job))" + NL +
             "join dept using (dname)",
             "dname not found in lhs", 1, 41);
         checkFails("select * from (emp join bonus using (job))" + NL +
             "join (select 1 as job from (true)) using (job)",
             "ambig", 1, 1);
     }
 
     public void testWhere() {
         checkFails("select * from emp where sal",
             "WHERE clause must be a condition", 1, 25);
     }
 
     public void testHaving() {
         checkFails("select * from emp having sum(sal)",
             "HAVING clause must be a condition", 1, 26);
         checkFails("select * from emp having sum(sal) > 10",
             "Expression '\\*' is not being grouped", 1, 8);
         // agg in select and having, no group by
         check("select sum(sal + sal) from emp having sum(sal) > 10");
     }
 
     public void testOrder() {
         final Compatible compatible = getCompatible();
         final boolean sortByOrdinal =
             compatible == Compatible.Oracle10g ||
             compatible == Compatible.Strict92 ||
             compatible == Compatible.Pragmatic99;
         final boolean sortByAlias =
             compatible == Compatible.Default ||
             compatible == Compatible.Oracle10g ||
             compatible == Compatible.Strict92;
         final boolean sortByAliasObscures =
             compatible == Compatible.Strict92;
 
         check("select empno as x from emp order by empno");
 
         // In sql92, empno is obscured by the alias.
         // Otherwise valid.
         // Checked Oracle10G -- is it valid.
         checkFails("select empno as x from emp order by empno",
             // in sql92, empno is obscured by the alias
             sortByAliasObscures ? "unknown column empno" :
             // otherwise valid
             null);
         checkFails("select empno as x from emp order by x",
             // valid in oracle and pre-99 sql
             sortByAlias ? null :
             // invalid in sql:2003
             "column 'x' not found");
 
         checkFails("select empno as x from emp order by 10",
             // invalid in oracle and pre-99
             sortByOrdinal ? "offset out of range" :
             // valid from sql:99 onwards (but sorting by constant achieves
             // nothing!)
             null);
 
         // Has different meanings in different dialects (which makes it very
         // confusing!) but is always valid.
         check("select empno + 1 as empno from emp order by empno");
 
         // Always fails
         checkFails("select empno as x from emp, dept order by deptno",
             "Column 'DEPTNO' is ambiguous", 1, 43);
 
         checkFails("select empno as deptno from emp, dept order by deptno",
             // Alias 'deptno' is closer in scope than 'emp.deptno'
             // and 'dept.deptno', and is therefore not ambiguous.
             // Checked Oracle10G -- it is valid.
             sortByAlias ? null :
             // Ambiguous in SQL:2003
             "col ambig");
 
         check(
             "select deptno from dept" + NL +
             "union" + NL +
             "select empno from emp" + NL +
             "order by deptno");
 
         checkFails(
             "select deptno from dept" + NL +
             "union" + NL +
             "select empno from emp" + NL +
             "order by empno",
             "Column 'EMPNO' not found in any table", 4, 10);
 
         checkFails(
             "select deptno from dept" + NL +
             "union" + NL +
             "select empno from emp" + NL +
             "order by 10",
             // invalid in oracle and pre-99
             sortByOrdinal ? "offset out of range" :
             null);
 
         // Sort by scalar subquery
         check(
             "select * from emp " + NL +
             "order by (select name from dept where deptno = emp.deptno)");
         checkFails(
             "select * from emp " + NL +
             "order by (select name from dept where deptno = emp.foo)",
             "Column 'FOO' not found in table EMP");
 
         // Sort by aggregate. Oracle allows this.
         check("select 1 from emp order by sum(sal)");
     }
 
     public void testGroup() {
         checkFails("select empno from emp where sum(sal) > 50",
             "Aggregate expression is illegal in WHERE clause", 1, 29);
 
         checkFails("select empno from emp group by deptno",
             "Expression 'EMPNO' is not being grouped", 1, 8);
 
         checkFails("select * from emp group by deptno",
             "Expression '\\*' is not being grouped", 1, 8);
 
         // This query tries to reference an agg expression from within a
         // subquery as a correlating expression, but the SQL syntax rules say
         // that the agg function SUM always applies to the current scope.
         // As it happens, the query is valid.
         check("select deptno " + NL +
             "from emp " + NL +
             "group by deptno " + NL +
             "having exists (select sum(emp.sal) > 10 from (values(true)))");
 
         // if you reference a column from a subquery, it must be a group col
         check("select deptno " +
             "from emp " +
             "group by deptno " +
             "having exists (select 1 from (values(true)) where emp.deptno = 10)");
         if (todo) {
             checkFails("select deptno " +
                 "from emp " +
                 "group by deptno " +
                 "having exists (select 1 from (values(true)) where emp.empno = 10)",
                 "xx", 1, 1);
         }
         // constant expressions
         check("select cast(1 as integer) + 2 from emp group by deptno");
         check("select localtime, deptno + 3 from emp group by deptno");
     }
 
     public void testGroupExpressionEquivalence() {
         // operator equivalence
         check("select empno + 1 from emp group by empno + 1");
         checkFails("select 1 + empno from emp group by empno + 1",
             "Expression 'EMPNO' is not being grouped", 1, 12);
         // datatype equivalence
         check("select cast(empno as VARCHAR(10)) from emp group by cast(empno as VARCHAR(10))");
         checkFails("select cast(empno as VARCHAR(11)) from emp group by cast(empno as VARCHAR(10))",
             "Expression 'EMPNO' is not being grouped", 1, 13);
     }
 
     public void testGroupExpressionEquivalenceId() {
         // identifier equivalence
         check("select case empno when 10 then deptno else null end from emp " +
             "group by case empno when 10 then deptno else null end");
         // matches even when one column is qualified (checked on Oracle10.1)
         if (todo)
             check("select case empno when 10 then deptno else null end from emp " +
                 "group by case empno when 10 then emp.deptno else null end");
         // note that expression appears unchanged in error msg
         checkFails("select case emp.empno when 10 then deptno else null end from emp " +
             "group by case emp.empno when 10 then emp.deptno else null end",
             "Expression 'EMP.EMPNO' is not being grouped", 1, 13);
         checkFails("select case empno when 10 then deptno else null end from emp " +
             "group by case emp.empno when 10 then emp.deptno else null end",
             "Expression 'EMPNO' is not being grouped", 1, 13);
 
     }
 
     // todo: enable when correlating variables work
     public void _testGroupExpressionEquivalenceCorrelated() {
         // dname comes from dept, so it is constant within the subquery, and
         // is so is a valid expr in a group-by query
         check("select * from dept where exists (" +
             "select dname from emp group by empno)");
         check("select * from dept where exists (" +
             "select dname + empno + 1 from emp group by empno, dept.deptno)");
     }
 
     // todo: enable when params are implemented
     public void _testGroupExpressionEquivalenceParams() {
         check("select cast(? as integer) from emp group by cast(? as integer)");
     }
 
     public void testGroupExpressionEquivalenceLiteral() {
         // The purpose of this test is to see whether the validator
         // regards a pair of constants as equivalent. If we just used the raw
         // constants the validator wouldn't care ('SELECT 1 FROM emp GROUP BY
         // 2' is legal), so we combine a column and a constant into the same
         // CASE expression.
 
         // literal equivalence
         check("select case empno when 10 then date '1969-04-29' else null end from emp " +
             "group by case empno when 10 then date '1969-04-29' else null end");
         // this query succeeds in oracle 10.1 because 1 and 1.0 have the same type
         checkFails("select case empno when 10 then 1 else null end from emp " +
             "group by case empno when 10 then 1.0 else null end",
             "Expression 'EMPNO' is not being grouped", 1, 13);
         // 3.1415 and 3.14150 are different literals (I don't care either way)
         checkFails("select case empno when 10 then 3.1415 else null end from emp " +
             "group by case empno when 10 then 3.14150 else null end",
             "Expression 'EMPNO' is not being grouped", 1, 13);
         // 3 and 03 are the same literal (I don't care either way)
         check("select case empno when 10 then 03 else null end from emp " +
             "group by case empno when 10 then 3 else null end");
         checkFails("select case empno when 10 then 1 else null end from emp " +
             "group by case empno when 10 then 2 else null end",
             "Expression 'EMPNO' is not being grouped", 1, 13);
         check("select case empno when 10 then timestamp '1969-04-29 12:34:56.0' else null end from emp " +
             "group by case empno when 10 then timestamp '1969-04-29 12:34:56' else null end");
         if (bug269fixed) {
             check("select case empno when 10 then 'foo bar' else null end from emp " +
                 "group by case empno when 10 then 'foo bar' else null end");
         }
 
         if (!todo) {
             return;
         }
 
         check("select case empno when 10 then _iso-8859-1'foo bar' collate latin1$en$1 else null end from emp " +
             "group by case empno when 10 then _iso-8859-1'foo bar' collate latin1$en$1 else null end");
         checkFails("select case empno when 10 then _iso-8859-1'foo bar' else null end from emp " +
             "group by case empno when 10 then _iso-8859-2'foo bar' else null end",
             "Expression 'EMPNO' is not being grouped", 1, 13);
         checkFails("select case empno when 10 then 'foo bar' collate latin1$en$1 else null end from emp " +
             "group by case empno when 10 then 'foo bar' collate latin1$fr$1 else null end",
             "Expression 'EMPNO' is not being grouped", 1, 13);
     }
 
     public void testCorrelatingVariables() {
         // reference to unqualified correlating column
         checkFails("select * from emp where exists (" + NL +
             "select * from dept where deptno = sal)",
             "Unknown identifier 'SAL'");
 
         // reference to qualified correlating column
         check("select * from emp where exists (" + NL +
             "select * from dept where deptno = emp.sal)");
     }
 
     public void testIntervalCompare(){
         checkExpType("interval '1' hour = interval '1' day", "BOOLEAN");
         checkExpType("interval '1' hour <> interval '1' hour", "BOOLEAN");
         checkExpType("interval '1' hour < interval '1' second", "BOOLEAN");
         checkExpType("interval '1' hour <= interval '1' minute", "BOOLEAN");
         checkExpType("interval '1' minute > interval '1' second", "BOOLEAN");
         checkExpType("interval '1' second >= interval '1' day", "BOOLEAN");
         checkExpType("interval '1' year >= interval '1' year", "BOOLEAN");
         checkExpType("interval '1' month = interval '1' year", "BOOLEAN");
         checkExpType("interval '1' month <> interval '1' month", "BOOLEAN");
         checkExpType("interval '1' year >= interval '1' month", "BOOLEAN");
 
         checkExpFails("interval '1' second >= interval '1' year", "(?s).*Cannot apply '>=' to arguments of type '<INTERVAL SECOND> >= <INTERVAL YEAR>'.*");
         checkExpFails("interval '1' month = interval '1' day", "(?s).*Cannot apply '=' to arguments of type '<INTERVAL MONTH> = <INTERVAL DAY>'.*");
     }
 
     public void testOverlaps() {
         checkExpType("(date '1-2-3', date '1-2-3') overlaps (date '1-2-3', date '1-2-3')","BOOLEAN");
         checkExp("(date '1-2-3', date '1-2-3') overlaps (date '1-2-3', interval '1' year)");
         checkExp("(time '1:2:3', interval '1' second) overlaps (time '23:59:59', time '1:2:3')");
         checkExp("(timestamp '1-2-3 4:5:6', timestamp '1-2-3 4:5:6' ) overlaps (timestamp '1-2-3 4:5:6', interval '1 2:3:4.5' day to second)");
 
         checkExpFails("(timestamp '1-2-3 4:5:6', timestamp '1-2-3 4:5:6' ) overlaps (time '4:5:6', interval '1 2:3:4.5' day to second)","(?s).*Cannot apply 'OVERLAPS' to arguments of type '.<TIMESTAMP.0.>, <TIMESTAMP.0.>. OVERLAPS .<TIME.0.>, <INTERVAL DAY TO SECOND>.*");
         checkExpFails("(time '4:5:6', timestamp '1-2-3 4:5:6' ) overlaps (time '4:5:6', interval '1 2:3:4.5' day to second)");
         checkExpFails("(time '4:5:6', time '4:5:6' ) overlaps (time '4:5:6', date '1-2-3')");
     }
 
     public void testExtract() {
         // The reason why extract returns double is because we can have
         // seconds fractions
         checkExpType("extract(year from interval '1-2' year to month)","DOUBLE");
         checkExp("extract(minute from interval '1.1' second)");
 
         checkExpFails("extract(minute from interval '11' month)","(?s).*Cannot apply.*");
         checkExpFails("extract(year from interval '11' second)","(?s).*Cannot apply.*");
     }
 
     public void testCastToInterval() {
         checkExpType("cast(interval '1' month as interval year)", "INTERVAL YEAR");
         checkExpType("cast(interval '1-1' year to month as interval month)", "INTERVAL MONTH");
         checkExpType("cast(interval '1:1' hour to minute as interval day)", "INTERVAL DAY");
         checkExpType("cast(interval '1:1' hour to minute as interval minute to second)", "INTERVAL MINUTE TO SECOND");
 
         checkExpFails("cast(interval '1:1' hour to minute as interval month)", "Cast function cannot convert value of type INTERVAL HOUR TO MINUTE to type INTERVAL MONTH");
        checkExpFails("cast(interval '1:1' year to month as interval second)", "Cast function cannot convert value of type INTERVAL YEAR TO MONTH to type INTERVAL SECOND");
     }
 
     public void testMinusDateOperator() {
         checkExpType("(CURRENT_DATE - CURRENT_DATE) HOUR", "INTERVAL HOUR");
         checkExpType("(CURRENT_DATE - CURRENT_DATE) YEAR TO MONTH", "INTERVAL YEAR TO MONTH");
         checkExpFails("(CURRENT_DATE - LOCALTIME) YEAR TO MONTH", "(?s).*Parameters must be of the same type.*");
     }
 
     public void testBind() {
         check("select * from emp where deptno = ?");
         check("select * from emp where deptno = ? and sal < 100000");
         if (todoTypeInference)
         check("select case when deptno = ? then 1 else 2 end from emp");
         if (todoTypeInference)
         check("select deptno from emp group by substring(name from ? for ?)");
         if (todoTypeInference)
         check("select deptno from emp group by case when deptno = ? then 1 else 2 end");
         check("select 1 from emp having sum(sal) < ?");
     }
 
     public void testUnnest() {
         checkType("select*from unnest(multiset[1])","INTEGER");
         checkType("select*from unnest(multiset[1, 2])","INTEGER");
         checkType("select*from unnest(multiset[1, 2.3])","DECIMAL(2, 1)");
         checkType("select*from unnest(multiset[1, 2.3, 1])","DECIMAL(2, 1)");
         checkType("select*from unnest(multiset['1','22','333'])","CHAR(3)");
         checkType("select*from unnest(multiset['1','22','333','22'])","CHAR(3)");
         checkFails("select*from unnest(1)","(?s).*Cannot apply 'UNNEST' to arguments of type 'UNNEST.<INTEGER>.'.*");
 
         check("select*from unnest(multiset(select*from dept))");
     }
 
     public void testCorrelationJoin() {
         check("select *," +
             "         multiset(select * from emp where deptno=dept.deptno) " +
             "               as empset" +
             "      from dept");
 
         check("select*from unnest(select multiset[8] from dept)");
         check("select*from unnest(select multiset[deptno] from dept)");
     }
 
     public void testStructuredTypes()
     {
         checkType(
             "values new address()",
             "ObjectSqlType(ADDRESS)");
         checkType(
             "select home_address from emp_address",
             "ObjectSqlType(ADDRESS)");
         checkType(
             "select ea.home_address.zip from emp_address ea",
             "INTEGER");
         checkType(
             "select ea.mailing_address.city from emp_address ea",
             "VARCHAR(20)");
     }
 
     public void testLateral() {
         checkFails("select * from emp, (select * from dept where emp.deptno=dept.deptno)","(?s).*Unknown identifier 'EMP'.*");
 
         check("select * from emp, LATERAL (select * from dept where emp.deptno=dept.deptno)");
         check("select * from emp, LATERAL (select * from dept where emp.deptno=dept.deptno) as ldt");
         check("select * from emp, LATERAL (select * from dept where emp.deptno=dept.deptno) ldt");
 
     }
 
     public void testCollect() {
         check("select collect(deptno) from emp");
         check("select collect(multiset[3]) from emp");
         // todo. COLLECT is an aggregate function. test that validator only
         // can take set operators in its select list once aggregation support is
         // complete
     }
 
     public void testFusion() {
         checkFails("select fusion(deptno) from emp","(?s).*Cannot apply 'FUSION' to arguments of type 'FUSION.<INTEGER>.'.*");
         check("select fusion(multiset[3]) from emp");
         // todo. FUSION is an aggregate function. test that validator only
         // can take set operators in its select list once aggregation support is
         // complete
     }
 
     public void testNew() {
         // (To debug invidual statements, paste them into this method.)
     }
 }
 
 // End SqlValidatorTest.java
 
