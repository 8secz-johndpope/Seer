 /*
  * Copyright (c) 2006-2011 Philipp Meinen <philipp@bind.ch>
  * 
  * Permission is hereby granted, free of charge, to any person obtaining a copy
  * of this software and associated documentation files (the "Software"),
  * to deal in the Software without restriction, including without limitation
  * the rights to use, copy, modify, merge, publish, distribute, sublicense,
  * and/or sell copies of the Software, and to permit persons to whom the Software
  * is furnished to do so, subject to the following conditions:
  * 
  * The above copyright notice and this permission notice shall be included
  * in all copies or substantial portions of the Software.
  * 
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
  * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
  * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
  * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
  * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
  * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
  * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
  */
 package ch.bind.philib.lang;
 
 import static org.testng.Assert.assertEquals;
 import static org.testng.Assert.assertFalse;
 import static org.testng.Assert.assertNotNull;
 import static org.testng.Assert.assertTrue;
 
 import java.util.Arrays;
 
 import org.testng.annotations.Test;
 import static ch.bind.philib.lang.ArrayUtil.*;
 
 public class ArrayUtilTest {
 
 	@Test(expectedExceptions = NullPointerException.class)
 	public void sourceNull() {
 		Object[] arr = new Object[1];
 
 		pickRandom(null, arr);
 	}
 
 	@Test(expectedExceptions = NullPointerException.class)
 	public void destinationNull() {
 		Object[] arr = new Object[1];
 		pickRandom(arr, null);
 	}
 
 	@Test(expectedExceptions = IllegalArgumentException.class)
 	public void sourceSmallerThenDestination() {
 		Object[] src = new Object[1];
 		Object[] dst = new Object[2];
 		pickRandom(src, dst);
 	}
 
 	@Test
 	public void equalSize() {
 		final int N = 4096;
 		Integer[] src = new Integer[N];
 		Integer[] dst = new Integer[N];
 		boolean[] found = new boolean[N];
 		for (int i = 0; i < N; i++) {
 			src[i] = i;
 		}
 		pickRandom(src, dst);
 		for (int i = 0; i < N; i++) {
 			int v = dst[i].intValue();
 			assertTrue(v >= 0);
 			assertTrue(v < N);
 			assertFalse(found[v]);
 			found[v] = true;
 		}
 	}
 
 	@Test
 	public void concatNullNull() {
 		byte[] r = concat(null, null);
 		assertNotNull(r);
 		assertEquals(0, r.length);
 	}
 
 	@Test
 	public void concatNullEmpty() {
 		byte[] r = concat(null, EMPTY_BYTE_ARRAY);
 		assertNotNull(r);
 		assertEquals(0, r.length);
 	}
 
 	@Test
 	public void concatEmptyNull() {
 		byte[] r = concat(EMPTY_BYTE_ARRAY, null);
 		assertNotNull(r);
 		assertEquals(0, r.length);
 	}
 
 	@Test
 	public void concatEmptyEmpty() {
 		byte[] r = concat(EMPTY_BYTE_ARRAY, EMPTY_BYTE_ARRAY);
 		assertNotNull(r);
 		assertEquals(0, r.length);
 	}
 
 	@Test
 	public void concatNormalNull() {
 		byte[] a = "123".getBytes();
 		byte[] b = null;
 		byte[] c = concat(a, b);
 		assertNotNull(c);
 		assertEquals(3, c.length);
 		assertTrue(Arrays.equals(a, c));
 	}
 
 	@Test
 	public void concatNullNormal() {
 		byte[] a = null;
 		byte[] b = "123".getBytes();
 		byte[] c = concat(a, b);
 		assertNotNull(c);
 		assertEquals(3, c.length);
 		assertTrue(Arrays.equals(b, c));
 	}
 
 	@Test
 	public void concatNormalNormal() {
 		byte[] a = "123".getBytes();
 		byte[] b = "abc".getBytes();
 		byte[] c = concat(a, b);
 		byte[] ce = "123abc".getBytes();
 		assertNotNull(c);
 		assertEquals(6, c.length);
 		assertTrue(Arrays.equals(ce, c));
 	}
 
 	@Test
 	public void testExtractBack() {
 		byte[] a = "abcd".getBytes();
 		assertEquals(extractBack(a, 0), EMPTY_BYTE_ARRAY);
 		assertEquals(extractBack(a, 1), "d".getBytes());
 		assertEquals(extractBack(a, 2), "cd".getBytes());
 		assertEquals(extractBack(a, 3), "bcd".getBytes());
 		assertEquals(extractBack(a, 4), "abcd".getBytes());
 	}
	
 	@Test
 	public void testExtractFront() {
 		byte[] a = "abcd".getBytes();
 		assertEquals(extractFront(a, 0), EMPTY_BYTE_ARRAY);
		assertEquals(extractFront(a, 1), "ad".getBytes());
 		assertEquals(extractFront(a, 2), "ab".getBytes());
 		assertEquals(extractFront(a, 3), "abc".getBytes());
 		assertEquals(extractFront(a, 4), "abcd".getBytes());
 	}
	
 	@Test
 	public void testFormatShortHex() {
 		byte[] a = "abcd".getBytes();
		assertEquals(formatShortHex(EMPTY_BYTE_ARRAY),"");
		assertEquals(formatShortHex(a),"");
		assertEquals(extractFront(a, 2), "ab".getBytes());
		assertEquals(extractFront(a, 3), "abc".getBytes());
		assertEquals(extractFront(a, 4), "abcd".getBytes());
 	}
 }
