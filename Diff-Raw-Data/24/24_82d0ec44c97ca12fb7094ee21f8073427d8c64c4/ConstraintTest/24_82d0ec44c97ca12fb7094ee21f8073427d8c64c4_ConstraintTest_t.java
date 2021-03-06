 package org.gdms.data.types;
 
 import org.gdms.TestBase;
 import org.junit.Test;
 import org.junit.Before;
 import java.sql.Time;
 import java.sql.Timestamp;
 import java.util.Date;
 
 
 import org.gdms.Geometries;
 import org.gdms.data.DataSource;
 import org.gdms.data.DataSourceFactory;
 import org.gdms.data.values.Value;
 import org.gdms.data.values.ValueCollection;
 import org.gdms.data.values.ValueFactory;
 import org.gdms.driver.DriverException;
 import org.gdms.driver.memory.MemoryDataSetDriver;
 
 import com.vividsolutions.jts.geom.Geometry;
 import com.vividsolutions.jts.geom.GeometryFactory;
 
 import static org.junit.Assert.*;
 
 public class ConstraintTest {
 
         private DataSourceFactory dsf;
         private Type type;
         private Value[] validValues = new Value[0];
         private Value[] invalidValues = new Value[0];
         private Value binaryValue = ValueFactory.createValue(new byte[]{2, 3, 4,
                         5});
         private Value booleanValue = ValueFactory.createValue(true);
         private Value byteValue = ValueFactory.createValue((byte) 3);
         private Value dateValue = ValueFactory.createValue(new Date());
         private Value doubleValue = ValueFactory.createValue(4.4d);
         private Value floatValue = ValueFactory.createValue(3.3f);
         private Value geomValue = ValueFactory.createValue(Geometries.getPoint());
         private Value intValue = ValueFactory.createValue(3);
         private Value longValue = ValueFactory.createValue(4L);
         private Value shortValue = ValueFactory.createValue((short) 3);
         private Value stringValue = ValueFactory.createValue("string");
         private Value timeValue = ValueFactory.createValue(new Time(2));
         private Value timestampValue = ValueFactory.createValue(new Timestamp(2));
         private ValueCollection collectionValue = ValueFactory.createValue(new Value[0]);
 
         @Before
         public void setUp() throws Exception {
                 dsf = new DataSourceFactory();
                 dsf.setTempDir(TestBase.backupDir.getAbsolutePath());
                 dsf.setResultDir(TestBase.backupDir);
         }
 
         @Test
         public void testLength() throws Exception {
                 setType(TypeFactory.createType(Type.STRING, new LengthConstraint(4)));
                 setValidValues(ValueFactory.createValue("1234"), ValueFactory.createValue(""), ValueFactory.createNullValue());
                 setInvalidValues(ValueFactory.createValue("12345"));
                 doEdition();
         }
 
         @Test
         public void testDefaultStringValue() throws Exception {
                 setType(TypeFactory.createType(Type.STRING,
                         new DefaultStringConstraint("erwan")));
                 setValidValues(ValueFactory.createValue("erwan"), ValueFactory.createValue("erwan"), ValueFactory.createValue("erwan"));
                 setInvalidValues(ValueFactory.createValue("bocher"));
                 doEdition();
         }
 
         @Test
         public void testMax() throws Exception {
                 setType(TypeFactory.createType(Type.INT, new MinConstraint(-10),
                         new MaxConstraint(10)));
                 setValidValues(ValueFactory.createValue(-10), ValueFactory.createValue(10), ValueFactory.createNullValue());
                 setInvalidValues(ValueFactory.createValue(-11), ValueFactory.createValue(11));
                 doEdition();
         }
 
         @Test
         public void testNotNull() throws Exception {
                 setType(TypeFactory.createType(Type.INT, new NotNullConstraint()));
                 setValidValues(ValueFactory.createValue(0));
                 setInvalidValues(ValueFactory.createNullValue());
                 doEdition();
         }
 
         @Test
         public void testAutoIncrement() throws Exception {
                 AutoIncrementConstraint constraint = new AutoIncrementConstraint();
                 checkOnlyCanSetAndAddNull(constraint);
         }
 
         @Test
         public void testReadOnly() throws Exception {
                 ReadOnlyConstraint constraint = new ReadOnlyConstraint();
                 checkOnlyCanSetAndAddNull(constraint);
         }
 
         /**
          * Cannot set a value Cannot insert a new row with values different than
          * null
          * 
          * @param constraint
          * @throws DriverException
          */
         private void checkOnlyCanSetAndAddNull(Constraint constraint)
                 throws DriverException {
                 Value three = ValueFactory.createValue(3);
                 Value nullV = ValueFactory.createNullValue();
                 setType(TypeFactory.createType(Type.INT, constraint));
                 DataSource ds = getDataSource();
                 ds.open();
                 ds.insertFilledRow(new Value[]{nullV});
                 assertNull(ds.check(0, nullV));
                 assertTrue(ds.check(0, three) != null);
                 try {
                         ds.insertFilledRow(new Value[]{three});
                         fail();
                 } catch (DriverException e) {
                 }
                 try {
                         ds.setFieldValue(0, 0, three);
                         fail();
                 } catch (DriverException e) {
                 }
                 ds.setFieldValue(0, 0, nullV);
                 ds.close();
         }
 
         @Test
         public void testGeometryType() throws Exception {
                 setType(TypeFactory.createType(Type.POINT));
                 setValidValues(ValueFactory.createValue(Geometries.getPoint()),
                         ValueFactory.createNullValue());
                 setInvalidValues(ValueFactory.createValue(new GeometryFactory().createGeometryCollection(new Geometry[0])),
                         ValueFactory.createValue(Geometries.getMultiPoint3D()));
                 doEdition();
         }
 
         @Test
         public void testDimension3DConstraint() throws Exception {
                 setType(TypeFactory.createType(Type.GEOMETRY,
                         new Dimension3DConstraint(3)));
                 setValidValues(ValueFactory.createValue(Geometries.getPoint3D()),
                         ValueFactory.createNullValue());
                 setInvalidValues(ValueFactory.createValue(Geometries.getMultiPolygon2D()));
                 doEdition();
         }
 
         @Test
         public void testDimension2DConstraint() throws Exception {
                 setType(TypeFactory.createType(Type.GEOMETRY,
                         new GeometryDimensionConstraint(0)));
                 setValidValues(ValueFactory.createValue(Geometries.getPoint3D()),
                         ValueFactory.createNullValue());
                 setInvalidValues(ValueFactory.createValue(Geometries.getMultiPolygon2D()));
                 doEdition();
         }
 
         @Test
         public void testPrecision() throws Exception {
                 setType(TypeFactory.createType(Type.DOUBLE, new PrecisionConstraint(3)));
                 setValidValues(ValueFactory.createValue(123), ValueFactory.createValue(12.3), ValueFactory.createValue(0.13),
                        ValueFactory.createNullValue(), ValueFactory.createValue(0.123), ValueFactory.createValue(12345));
                setInvalidValues(ValueFactory.createValue(0.1234), ValueFactory.createValue(123.4567));
                 doEdition();
         }
 
         @Test
         public void testScale() throws Exception {
                 setType(TypeFactory.createType(Type.DOUBLE, new ScaleConstraint(3)));
                 setValidValues(ValueFactory.createValue(123), ValueFactory.createValue(12.322), ValueFactory.createValue(0.133),
                         ValueFactory.createNullValue());
                 setInvalidValues(ValueFactory.createValue(0.1323), ValueFactory.createValue(1244.1235));
                 doEdition();
         }
 
         @Test
         public void testPattern() throws Exception {
                 setType(TypeFactory.createType(Type.STRING, new PatternConstraint(
                         "[hc]+at")));
                 setValidValues(ValueFactory.createValue("hat"), ValueFactory.createValue("cat"), ValueFactory.createNullValue());
                 setInvalidValues(ValueFactory.createValue("hate"), ValueFactory.createValue("at"));
                 doEdition();
         }
 
         @Test
         public void testUnique() throws Exception {
                 setType(TypeFactory.createType(Type.INT, new UniqueConstraint()));
                 checkUniqueness();
         }
 
         @Test
         public void testPK() throws Exception {
                 setType(TypeFactory.createType(Type.INT, new PrimaryKeyConstraint()));
                 checkUniqueness();
         }
 
         @Test
         public void testAddWrongTypeBinary() throws Exception {
                 setType(TypeFactory.createType(Type.BINARY));
                 setValidValues(binaryValue);
                 setInvalidValues(booleanValue, byteValue, dateValue, doubleValue,
                         floatValue, geomValue, intValue, longValue, shortValue,
                         stringValue, timeValue, timestampValue, collectionValue);
                 doEdition();
         }
 
         @Test
         public void testAddWrongTypeBoolean() throws Exception {
                 setType(TypeFactory.createType(Type.BOOLEAN));
                 setValidValues(booleanValue, stringValue);
                 setInvalidValues(binaryValue, byteValue, dateValue, doubleValue,
                         floatValue, geomValue, intValue, longValue, shortValue,
                         timeValue, timestampValue, collectionValue);
                 doEdition();
         }
 
         @Test
         public void testAddWrongTypeCollection() throws Exception {
                 setType(TypeFactory.createType(Type.COLLECTION));
                 setValidValues(collectionValue);
                 setInvalidValues(binaryValue, booleanValue, byteValue, dateValue,
                         doubleValue, floatValue, geomValue, intValue, longValue,
                         shortValue, stringValue, timeValue, timestampValue);
                 doEdition();
         }
 
         @Test
         public void testAddWrongTypeDate() throws Exception {
                 setType(TypeFactory.createType(Type.DATE));
                 setValidValues(timeValue, dateValue, timestampValue, ValueFactory.createValue("1980-09-05"), byteValue, intValue, longValue,
                         shortValue);
                 setInvalidValues(binaryValue, booleanValue, doubleValue, floatValue,
                         geomValue, stringValue, collectionValue);
                 doEdition();
         }
 
         @Test
         public void testAddWrongTypeGeometry() throws Exception {
                 setType(TypeFactory.createType(Type.GEOMETRY));
                 setValidValues(geomValue);
                 setInvalidValues(binaryValue, ValueFactory.createValue("POINT (0 0)"), booleanValue, byteValue, dateValue,
                         doubleValue, floatValue, intValue, longValue, shortValue,
                         stringValue, timeValue, timestampValue, collectionValue);
                 doEdition();
         }
 
         @Test
         public void testAddWrongTypeString() throws Exception {
                 setType(TypeFactory.createType(Type.STRING));
                 setValidValues(binaryValue, booleanValue, byteValue, dateValue,
                         doubleValue, floatValue, geomValue, intValue, longValue,
                         shortValue, stringValue, timeValue, timestampValue,
                         collectionValue);
                 doEdition();
         }
 
         @Test
         public void testAddWrongTypeTime() throws Exception {
                 setType(TypeFactory.createType(Type.TIME));
                 setValidValues(dateValue, ValueFactory.createValue("1980-09-05 12:00:20"), byteValue, intValue,
                         longValue, shortValue, timeValue, timestampValue);
                 setInvalidValues(binaryValue, booleanValue, doubleValue, floatValue,
                         geomValue, stringValue, collectionValue);
                 doEdition();
         }
 
         @Test
         public void testAddWrongTypeTimestamp() throws Exception {
                 setType(TypeFactory.createType(Type.TIMESTAMP));
                 setValidValues(dateValue, ValueFactory.createValue("1980-09-05 12:00:24.12132"), byteValue, intValue,
                         longValue, shortValue, timeValue, timestampValue);
                 setInvalidValues(binaryValue, booleanValue, doubleValue, floatValue,
                         geomValue, stringValue, collectionValue);
                 doEdition();
         }
 
         @Test
         public void testAddWrongTypeByte() throws Exception {
                 setType(TypeFactory.createType(Type.BYTE));
                 checkNumber();
         }
 
         private void checkNumber() throws Exception {
                 setValidValues(byteValue, intValue, longValue, shortValue, doubleValue, floatValue);
                 setInvalidValues(binaryValue, booleanValue, dateValue, 
                         geomValue, stringValue, timeValue, timestampValue,
                         collectionValue);
                 doEdition();
         }
 
         @Test
         public void testAddWrongTypeShort() throws Exception {
                 setType(TypeFactory.createType(Type.SHORT));
                 checkNumber();
         }
 
         @Test
         public void testAddWrongTypeInt() throws Exception {
                 setType(TypeFactory.createType(Type.INT));
                 checkNumber();
         }
 
         @Test
         public void testAddWrongTypeLong() throws Exception {
                 setType(TypeFactory.createType(Type.LONG));
                 checkNumber();
         }
 
         @Test
         public void testAddWrongTypeFloat() throws Exception {
                 setType(TypeFactory.createType(Type.FLOAT));
                 checkNumber();
         }
 
         @Test
         public void testAddWrongTypeDouble() throws Exception {
                 setType(TypeFactory.createType(Type.DOUBLE));
                 checkNumber();
         }
 
         @Test
         public void testTypeCasting() throws Exception {
                 assertTrue(TypeFactory.canBeCastTo(Type.BYTE, Type.SHORT));
                 assertTrue(TypeFactory.canBeCastTo(Type.SHORT, Type.INT));
                 assertTrue(TypeFactory.canBeCastTo(Type.INT, Type.LONG));
                 assertTrue(TypeFactory.canBeCastTo(Type.LONG, Type.FLOAT));
                 assertTrue(TypeFactory.canBeCastTo(Type.FLOAT, Type.DOUBLE));
                 assertTrue(TypeFactory.canBeCastTo(Type.INT, Type.DOUBLE));
                 assertTrue(TypeFactory.canBeCastTo(Type.INT, Type.LONG));
 
                 assertTrue(TypeFactory.canBeCastTo(Type.DATE, Type.TIME));
                 assertTrue(TypeFactory.canBeCastTo(Type.TIME, Type.TIMESTAMP));
         }
 
         private void checkUniqueness() throws DriverException {
                 DataSource ds = getDataSource();
                 ds.open();
                 ds.insertFilledRow(new Value[]{ValueFactory.createValue(2)});
                 try {
                         ds.insertFilledRow(new Value[]{ValueFactory.createValue(2)});
                         fail();
                 } catch (DriverException e) {
                 }
                 ds.insertFilledRow(new Value[]{ValueFactory.createValue(3)});
                 try {
                         ds.setFieldValue(ds.getRowCount() - 1, 0, ValueFactory.createValue(2));
                         fail();
                 } catch (DriverException e) {
                 }
         }
 
         private void setValidValues(Value... values) {
                 this.validValues = values;
         }
 
         private void setInvalidValues(Value... values) {
                 this.invalidValues = values;
         }
 
         private void setType(Type type) {
                 this.type = type;
         }
 
         /**
          * Given the set of valid and invalid values, this method checks that 
          * they can (or can not) be inserted in the datasource.
          * @throws Exception 
          */
         private void doEdition() throws Exception {
                 DataSource dataSource = getDataSource();
                 dataSource.open();
                 for (Value value : validValues) {
                         dataSource.insertFilledRow(new Value[]{value});
                         dataSource.setFieldValue(dataSource.getRowCount() - 1, 0, value);
                         assertNull(dataSource.check(0, value));
                 }
                 for (Value value : invalidValues) {
                         try {
                                 assertTrue(dataSource.check(0, value) != null);
                                 dataSource.insertFilledRow(new Value[]{value});
                                 fail();
                         } catch (DriverException e) {
                         } catch (IncompatibleTypesException e) {
                         }
                         try {
                                 assertTrue(dataSource.check(0, value) != null);
                                 dataSource.setFieldValue(0, 0, value);
                                 fail();
                         } catch (DriverException e) {
                         } catch (IncompatibleTypesException e) {
                         }
                 }
                 dataSource.commit();
                 dataSource.close();
         }
 
         private DataSource getDataSource() throws DriverException {
                 MemoryDataSetDriver omd = new MemoryDataSetDriver(
                         new String[]{"string"}, new Type[]{type});
                 DataSource dataSource = dsf.getDataSource(omd, "main");
                 return dataSource;
         }
 }
