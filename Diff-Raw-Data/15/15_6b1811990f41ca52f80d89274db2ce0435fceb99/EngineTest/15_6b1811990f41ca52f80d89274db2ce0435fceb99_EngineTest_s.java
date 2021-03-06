 package com.floreysoft.jmte;
 
 import static org.junit.Assert.*;
 import static org.junit.Assert.assertEquals;
 import static org.junit.Assert.assertNull;
 import static org.junit.Assert.assertTrue;
 
 import java.io.ByteArrayInputStream;
 import java.io.File;
 import java.io.StringReader;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 
 import org.junit.Before;
 import org.junit.Test;
 
 import com.floreysoft.jmte.Engine.StartEndPair;
 import com.floreysoft.jmte.token.StringToken;
 
 @SuppressWarnings("unchecked")
 public final class EngineTest {
 
 	private static class MyIterable implements Iterable {
 		List<Object> list = new ArrayList<Object>();
 
 		public MyIterable() {
 
 		}
 
 		public MyIterable(List<Object> list) {
 			this.list = list;
 		}
 
 		@Override
 		public Iterator<Object> iterator() {
 			return list.iterator();
 		}
 	}
 
 	public static class MyBean {
 
 		private Object property1 = "propertyValue1";
 		public Object property2 = "propertyValue2";
 		public boolean falseCond = false;
 
 		public MyBean(Object property1, Object property2) {
 			this.property1 = property1;
 			this.property2 = property2;
 		}
 
 		public MyBean() {
 		}
 
 		public List getList() {
 			return LIST;
 		}
 
 		public Map getMap() {
 			return MAP;
 		}
 
 		public Object getProperty1() {
 			return property1;
 		}
 
 		public boolean getTrueCond() {
 			return true;
 		}
 
 		@Override
 		public String toString() {
 			return property1.toString() + ", " + property2.toString();
 		}
 
 	}
 
 	private final static Map<String, Object> MAP = new HashMap<String, Object>();
 	static {
 		MAP.put("mapEntry1", "mapValue1");
 		MAP.put("mapEntry2", "mapValue2");
 	}
 
 	private final static List<MyBean> LIST = new ArrayList<MyBean>();
 	static {
 		LIST.add(new MyBean("1.1", "1.2"));
 		LIST.add(new MyBean("2.1", "2.2"));
 	}
 
 	private final static MyBean[] ARRAY = new MyBean[2];
 	static {
 		ARRAY[0] = new MyBean("1.1", "1.2");
 		ARRAY[1] = new MyBean("2.1", "2.2");
 	}
 
 	private final static int[] INT_ARRAY = { 1, 2 };
 
 	private final static Iterable ITERABLE;
 	static {
 		List<Object> list = new ArrayList<Object>();
 		list.add("iterableEntry1");
 		list.add("iterableEntry2");
 		ITERABLE = new MyIterable(list);
 	}
 
 	private final static MyBean BEAN = new MyBean();
 
 	private final static Map<String, Object> DEFAULT_MODEL = new HashMap<String, Object>();
 	static {
 		DEFAULT_MODEL.put("address", "Fillbert");
 		DEFAULT_MODEL.put("map", MAP);
 		DEFAULT_MODEL.put("list", LIST);
 		DEFAULT_MODEL.put("iterable", ITERABLE);
 		DEFAULT_MODEL.put("array", ARRAY);
 		DEFAULT_MODEL.put("intArray", INT_ARRAY);
 		DEFAULT_MODEL.put("bean", BEAN);
 		DEFAULT_MODEL.put("emptyMap", new HashMap());
 		DEFAULT_MODEL.put("emptyList", new ArrayList());
 		DEFAULT_MODEL.put("emptyArray", new Object[0]);
 		DEFAULT_MODEL.put("emptyIntArray", new int[0]);
 		DEFAULT_MODEL.put("emptyIterable", new MyIterable());
 		DEFAULT_MODEL.put("empty", "");
 	}
 
 	private Engine engine;
 
 	@Before
 	public void setUp() {
 		engine = new Engine();
 	}
 
 	@Test
 	public void extract() throws Exception {
 		String line = "${if adresse}Sie wohnen an ${adresse}";
 		List<StartEndPair> scan = engine.scan(line, true);
 		assertEquals(2, scan.size());
 
 		assertEquals(2, scan.get(0).start);
 		assertEquals(12, scan.get(0).end);
 
 		assertEquals(29, scan.get(1).start);
 		assertEquals(36, scan.get(1).end);
 
 	}
 
 	@Test
 	public void identityTransform() throws Exception {
 		engine.setLexer(new Lexer() {
 			public Token nextToken(char[] template, int start, int end,
 					Map<String, Object> model, boolean expand,
 					ErrorHandler errorHandler) {
 				String input = new String(template, start, end - start);
 				return new StringToken("${" + input + "}");
 			}
 
 		});
 
 		String line = "${if adresse}Sie wohnen an ${adresse}";
 		String output = engine.transform(line, null);
 		assertEquals(line, output);
 	}
 
 	@Test
 	public void simpleExpression() throws Exception {
 		String output = engine.transform("${address}", DEFAULT_MODEL);
 		assertEquals(DEFAULT_MODEL.get("address"), output);
 	}
 
 	@Test
 	public void empty() throws Exception {
 		String output = engine.transform("${\n}${ \n }${}", DEFAULT_MODEL);
 		assertEquals("", output);
 	}
 
 	@Test
 	public void suffixPrefix() throws Exception {
 		String output = engine.transform("PREFIX${address}SUFFIX",
 				DEFAULT_MODEL);
 		assertEquals("PREFIX" + DEFAULT_MODEL.get("address") + "SUFFIX", output);
 	}
 
 	@Test
 	public void noTransformation() throws Exception {
 		String output = engine.transform("No transformation required",
 				DEFAULT_MODEL);
 		assertEquals("No transformation required", output);
 	}
 
 	@Test
 	public void errorPosition() throws Exception {
 		boolean foundPosition = false;
 		try {
 			engine.transform("\n${address}\n     ${else}NIX${end}",
 					DEFAULT_MODEL);
 		} catch (Exception e) {
 			String message = e.getMessage();
 			foundPosition = message
					.equals("Error while parsing 'else' at (3:8): Can't use else outside of if block");
 
 		}
 		assertTrue(
 				"Position not found in exception message or exception not thrown",
 				foundPosition);
 	}
 
 	@Test
 	public void mapExpression() throws Exception {
 		String output = engine.transform("${map.mapEntry1}", DEFAULT_MODEL);
 		assertEquals(MAP.get("mapEntry1"), output);
 
 	}
 
 	@Test
 	public void propertyExpressionGetter() throws Exception {
 		String output = engine.transform("${bean.property1}", DEFAULT_MODEL);
 		assertEquals(BEAN.getProperty1().toString(), output);
 
 	}
 
 	@Test
 	public void propertyExpressionField() throws Exception {
 		String output = engine.transform("${bean.property2}", DEFAULT_MODEL);
 		assertEquals(BEAN.property2.toString(), output);
 
 	}
 
 	@Test
 	public void nullExpression() throws Exception {
 		String output = engine.transform("${undefined}", DEFAULT_MODEL);
 		assertEquals("", output);
 	}
 
 	@Test
 	public void directMap() throws Exception {
 		// if we try to directly output a map, we simply get the first value
 		String output = engine.transform("${map}", DEFAULT_MODEL);
 		assertEquals("mapValue1", output);
 	}
 
 	@Test
 	public void directEmptyMap() throws Exception {
 		// if we try to directly output an empty map, we simply get an empty string
 		String output = engine.transform("${emptyMap}", DEFAULT_MODEL);
 		assertEquals("", output);
 	}
 	
 	@Test
 	public void directList() throws Exception {
 		// if we try to directly output a list, we simply get the first value
 		String output = engine.transform("${list}", DEFAULT_MODEL);
 		assertEquals("1.1, 1.2", output);
 	}
 	
 	@Test
 	public void directArray() throws Exception {
 		// if we try to directly output an array, we simply get the first value
 		String output = engine.transform("${array}", DEFAULT_MODEL);
 		assertEquals("1.1, 1.2", output);
 	}
 	
 	@Test
 	public void directEmptyList() throws Exception {
 		// if we try to directly output an empty list, we simply get an empty string
 		String output = engine.transform("${emptyList}", DEFAULT_MODEL);
 		assertEquals("", output);
 	}
 	
 	@Test
 	public void directEmptyArray() throws Exception {
 		// if we try to directly output an empty array, we simply get an empty string
 		String output = engine.transform("${emptyArray}", DEFAULT_MODEL);
 		assertEquals("", output);
 	}
 	
 	@Test
 	public void ifEmptyFalseExpression() throws Exception {
 		String output = engine.transform(
 				"${if empty}${address}${else}NIX${end}", DEFAULT_MODEL);
 		assertEquals("NIX", output);
 	}
 
 	@Test
 	public void ifNullTrueExpression() throws Exception {
 		String output = engine.transform(
 				"${if address}${address}${else}NIX${end}", DEFAULT_MODEL);
 		assertEquals(DEFAULT_MODEL.get("address"), output);
 	}
 
 	@Test(expected = IllegalArgumentException.class)
 	public void elseWithoutIfError() throws Exception {
 		engine.transform("${address}${else}NIX${end}", DEFAULT_MODEL);
 	}
 
 	@Test(expected = IllegalArgumentException.class)
 	public void endWithoutBlockError() throws Exception {
 		engine.transform("${address}${end}", DEFAULT_MODEL);
 	}
 
 	@Test
 	public void ifNotExpression() throws Exception {
 		String output = engine.transform(
 				"${if !hugo}${address}${else}NIX${end}", DEFAULT_MODEL);
 		assertEquals(DEFAULT_MODEL.get("address"), output);
 	}
 
 	@Test
 	public void ifBooleanTrueExpression() throws Exception {
 		String output = engine.transform(
 				"${if bean.trueCond}${address}${else}NIX${end}", DEFAULT_MODEL);
 		assertEquals(DEFAULT_MODEL.get("address"), output);
 	}
 
 	@Test
 	public void ifBooleanFalseExpression() throws Exception {
 		String output = engine
 				.transform("${if bean.falseCond}${address}${else}NIX${end}",
 						DEFAULT_MODEL);
 		assertEquals("NIX", output);
 	}
 
 	@Test
 	public void ifMapTrueExpression() throws Exception {
 		String output = engine.transform("${if map}${address}${else}NIX${end}",
 				DEFAULT_MODEL);
 		assertEquals(DEFAULT_MODEL.get("address"), output);
 	}
 
 	@Test
 	public void ifMapFalseExpression() throws Exception {
 		String output = engine.transform(
 				"${if emptyMap}${address}${else}NIX${end}", DEFAULT_MODEL);
 		assertEquals("NIX", output);
 	}
 
 	@Test
 	public void ifCollectionTrueExpression() throws Exception {
 		String output = engine.transform(
 				"${if list}${address}${else}NIX${end}", DEFAULT_MODEL);
 		assertEquals(DEFAULT_MODEL.get("address"), output);
 	}
 
 	@Test
 	public void ifCollectionFalseExpression() throws Exception {
 		String output = engine.transform(
 				"${if emptyList}${address}${else}NIX${end}", DEFAULT_MODEL);
 		assertEquals("NIX", output);
 	}
 
 	@Test
 	public void ifIterableTrueExpression() throws Exception {
 		String output = engine.transform(
 				"${if iterable}${address}${else}NIX${end}", DEFAULT_MODEL);
 		assertEquals(DEFAULT_MODEL.get("address"), output);
 	}
 
 	@Test
 	public void ifIterableFalseExpression() throws Exception {
 		String output = engine.transform(
 				"${if emptyIterable}${address}${else}NIX${end}", DEFAULT_MODEL);
 		assertEquals("NIX", output);
 	}
 
 	@Test
 	public void ifArrayTrueExpression() throws Exception {
 		String output = engine.transform(
 				"${if array}${address}${else}NIX${end}", DEFAULT_MODEL);
 		assertEquals(DEFAULT_MODEL.get("address"), output);
 	}
 
 	@Test
 	public void ifArrayFalseExpression() throws Exception {
 		String output = engine.transform(
 				"${if emptyArray }${address}${else}NIX${end}", DEFAULT_MODEL);
 		assertEquals("NIX", output);
 	}
 
 	@Test
 	public void ifPrimitiveArrayTrueExpression() throws Exception {
 		String output = engine.transform(
 				"${if intArray}${address}${else}NIX${end}", DEFAULT_MODEL);
 		assertEquals(DEFAULT_MODEL.get("address"), output);
 	}
 
 	@Test
 	public void ifPrimitiveArrayFalseExpression() throws Exception {
 		String output = engine
 				.transform("${if emptyIntArray }${address}${else}NIX${end}",
 						DEFAULT_MODEL);
 		assertEquals("NIX", output);
 	}
 
 	@Test
 	public void nestedIfExpression() throws Exception {
 		String output = engine
 				.transform(
 						"${if hugo}${address}${else}${if address}${address}${else}NIX${end}${end}",
 						DEFAULT_MODEL);
 		assertEquals(DEFAULT_MODEL.get("address"), output);
 	}
 
 	@Test
 	public void simpleForeach() throws Exception {
 		String output = engine.transform("${foreach list item}${item}\n${end}",
 				DEFAULT_MODEL);
 		assertEquals("1.1, 1.2\n" + "2.1, 2.2\n", output);
 		assertNull(DEFAULT_MODEL.get("item"));
 	}
 
 	@Test
 	public void foreachSingletonAsList() throws Exception {
 		// if the variable we want to iterate over is atomic, we simply wrap
 		// it into a singleton list
 		String output = engine.transform("${foreach address item}${item}${end}",
 				DEFAULT_MODEL);
 		String expected = engine.transform("${address}",
 				DEFAULT_MODEL);
 		assertEquals(expected, output);
 	}
 
 	@Test
 	public void mergedForeach() throws Exception {
 		List amount = Arrays.asList(1, 2, 3);
 		List price = Arrays.asList(3.6, 2, 3.0);
 		List total = Arrays.asList("3.6", "4", "9");
 		
 		List<Map<String,Object>> mergedLists = Engine.mergeLists(new String[] {"amount", "price", "total"}, amount, price, total);
 		Map<String,Object> model = new HashMap<String, Object>();
 		model.put("mergedLists", mergedLists);
 		String output = engine.transform("${foreach mergedLists item}${item.amount} x ${item.price} = ${item.total}\n${end}",
 				model);
 		assertEquals("1 x 3.6 = 3.6\n" + "2 x 2 = 4\n"+ "3 x 3.0 = 9\n", output);
 	}
 
 	@Test
 	public void foreachArray() throws Exception {
 		String output = engine.transform(
 				"${foreach array item}${item}\n${end}", DEFAULT_MODEL);
 		assertEquals("1.1, 1.2\n" + "2.1, 2.2\n", output);
 		assertNull(DEFAULT_MODEL.get("item"));
 	}
 
 	@Test
 	public void foreachPrimitiveArray() throws Exception {
 		String output = engine.transform(
 				"${foreach intArray item}${item}\n${end}", DEFAULT_MODEL);
 		assertEquals("1\n2\n", output);
 		assertNull(DEFAULT_MODEL.get("item"));
 	}
 
 	@Test
 	public void foreachMap() throws Exception {
 		String output = engine.transform(
 				"${foreach map entry}${entry.key}=${entry.value}\n${end}",
 				DEFAULT_MODEL);
 		assertEquals("mapEntry1=mapValue1\n" + "mapEntry2=mapValue2\n", output);
 		assertNull(DEFAULT_MODEL.get("item"));
 
 	}
 
 	@Test
 	public void specialForeachVariables() throws Exception {
 		String output = engine
 				.transform(
 						"${foreach list item}${item}\n${if last_item}last${end}${if first_item}first${end}${if even_item} even${end}${if odd_item} odd${end}${end}",
 						DEFAULT_MODEL);
 		assertEquals("1.1, 1.2\nfirst even" + "2.1, 2.2\nlast odd", output);
 		assertNull(DEFAULT_MODEL.get("item"));
 	}
 
 	@Test
 	public void foreachSeparator() throws Exception {
 		String output = engine
 				.transform(
 						"${ \n foreach\n\t  list  \r  item   ,}${item.property1}${end}",
 						DEFAULT_MODEL);
 		assertEquals("1.1  ,2.1", output);
 		assertNull(DEFAULT_MODEL.get("item"));
 	}
 
 	@Test
 	public void crazyForeachSeparator() throws Exception {
 		String output = engine
 				.transform("${ foreach list item  }${item.property1}${end}",
 						DEFAULT_MODEL);
 		assertEquals("1.1 2.1", output);
 		assertNull(DEFAULT_MODEL.get("item"));
 	}
 
 	@Test
 	public void propertyForeach() throws Exception {
 		String output = engine.transform(
 				"${foreach list item}${item.property1}\n${end}", DEFAULT_MODEL);
 		assertEquals("1.1\n" + "2.1\n", output);
 		assertNull(DEFAULT_MODEL.get("item"));
 	}
 
 	@Test
 	public void nestedForeach() throws Exception {
 		String output = engine
 				.transform(
 						"${foreach list item}${foreach item.list item2}${item2.property1}${end}\n${end}",
 						DEFAULT_MODEL);
 		assertEquals("1.12.1\n" + "1.12.1\n", output);
 		assertNull(DEFAULT_MODEL.get("item"));
 		assertNull(DEFAULT_MODEL.get("item2"));
 	}
 
 	@Test
 	public void emptyForeach() throws Exception {
 		String output = engine.transform(
 				"${foreach emptyList item}${item.property1}\n${end}",
 				DEFAULT_MODEL);
 		assertEquals("", output);
 		assertNull(DEFAULT_MODEL.get("item"));
 	}
 
 	@Test
 	public void ifInForeach() throws Exception {
 		String output = engine
 				.transform(
 						"${foreach list item}${if item}${item}${if hugo}${item}${end}${end}\n${end}",
 						DEFAULT_MODEL);
 		assertEquals("1.1, 1.2\n" + "2.1, 2.2\n", output);
 		assertNull(DEFAULT_MODEL.get("item"));
 	}
 
 	@Test
 	public void foreachDoubleVarNameError() throws Exception {
 		// XXX strange way of checking for excepion as we need to make sure
 		// model is clean even after exception
 		boolean exceptionFound = false;
 		try {
 			engine
 					.transform(
 							"${foreach list item}${foreach list item}${item}\n${end}${end}",
 							DEFAULT_MODEL);
 		} catch (IllegalArgumentException e) {
 			exceptionFound = true;
 		}
 		assertTrue(exceptionFound);
 		assertNull(DEFAULT_MODEL.get("item"));
 	}
 
 	@Test
 	public void simpleEscaping() throws Exception {
 		String output = engine.transform("\\${\\}\n\\\\}", DEFAULT_MODEL);
 		assertEquals("${}\n\\}", output);
 	}
 
 	@Test
 	public void escapingDeactivated() throws Exception {
 		String output = engine.transform("\\${address}\n\\}", DEFAULT_MODEL,
 				false);
 		assertEquals("\\Fillbert\n\\}", output);
 	}
 
 	@Test
 	public void escapingKernel() throws Exception {
 		String output = engine.applyEscapes("\\${\\}\n\\\\}");
 		assertEquals("${}\n\\}", output);
 	}
 
 	@Test
 	public void complexEscaping() throws Exception {
 		String output = engine
 				.transform(
 						"${foreach list item \\${\n \\},}${item.property1}${end}\n\\\\",
 						DEFAULT_MODEL);
 		assertEquals("1.1${\n },2.1\n\\", output);
 	}
 
 	@Test
 	public void stream2String() throws Exception {
 		String charsetName = "ISO-8859-15";
 		String input = "stream content";
 		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
 				input.getBytes(charsetName));
 		String streamToString = Util.streamToString(byteArrayInputStream,
 				charsetName);
 		assertEquals(input, streamToString);
 	}
 
 	@Test
 	public void reader2String() throws Exception {
 		String input = "reader content";
 		StringReader stringReader = new StringReader(input);
 		String readerToString = Util.readerToString(stringReader);
 		assertEquals(input, readerToString);
 	}
 
 	@Test
 	public void file2String() throws Exception {
 		String charsetName = "ISO-8859-15";
 		File file = new File("example/basic.mte");
 		String fileToString = Util.fileToString(file, charsetName);
 		assertEquals("${if address}${address}${else}NIX${end}", fileToString);
 	}
 
 	@Test
 	public void format() throws Exception {
 		String output = Engine.format("${if 1}${1}${else}${2}${end}", "arg1",
 				"arg2");
 		assertEquals("arg1", output);
 		// just to prove that shortcut for false-branch works
 		Engine.format("${if 1}${1}${else}${2}${end}", "arg1");
 
 		// check for null values
 		output = Engine.format("${if 1}${1}${else}${2}${end}", null, "arg2");
 		assertEquals("arg2", output);
 
 		// check for boolean values
 		output = Engine.format("${if 1}${1}${else}${2}${end}", false, "arg2");
 		assertEquals("arg2", output);
 	}
 
 	// sandbox just for quick testing
 	public static void main(String[] args) {
 		String input = "${name}";
 		Map<String, Object> model = new HashMap<String, Object>();
 		model.put("name", "Minimal Template Engine");
 		Engine engine = new Engine();
 		String transformed = engine.transform(input, model);
 		System.out.println(transformed);
 		assert(transformed.equals("Minimal Template Engine"));
 	}
 }
