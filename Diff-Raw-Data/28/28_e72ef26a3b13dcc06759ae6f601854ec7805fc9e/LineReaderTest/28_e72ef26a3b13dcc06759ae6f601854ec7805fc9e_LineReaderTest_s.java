 /*
  * Copyright 1&1 Internet AG, http://www.1and1.org
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU Lesser General Public License as published by
  * the Free Software Foundation; either version 2 of the License,
  * or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  * See the GNU Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 
 package net.sf.beezle.sushi.fs;
 
 import org.junit.Test;
 
 import java.io.IOException;
 import java.util.Arrays;
 import java.util.List;
 
 import static org.junit.Assert.assertEquals;
 
 public class LineReaderTest {
     private final World world = new World();
 
     @Test
     public void zero() {
         check("");
     }
 
     @Test
     public void one() {
        check("abc\n", "abc");
     }
 
     @Test
     public void oneWithoutNewline() {
         check("ab", "ab");
     }
 
     @Test
     public void two() {
        check("abc\n\n123\n", "abc", "", "123");
     }
 
     @Test
     public void longline() {
         String ll = "1234567890abcdefghijklmnopqrstuvwxyz";
         
         ll = ll + ll + ll;
        check(ll + "\n" + ll, ll, ll);
     }
 
     @Test
     public void comment() {
        check("first\n // \n\n//comment\nlast",
                 new LineFormat(LineFormat.LF_SEPARATOR, LineFormat.Trim.ALL, LineFormat.excludes(true, "//")), 5,
                 "first", "last");
     }
 
     @Test
     public void excludeEmpty() {
        check("first\n\nthird\n  \nfifth",
                 new LineFormat(LineFormat.LF_SEPARATOR, LineFormat.Trim.ALL, LineFormat.excludes(true)), 5,
                 "first", "third", "fifth");
     }
 
     @Test
     public void trimNothing() {
        check("hello\nworld",
                 new LineFormat(LineFormat.LF_SEPARATOR, LineFormat.Trim.NOTHING, LineFormat.NO_EXCLUDES), 2,
                "hello\n", "world");
     }
 
     @Test
     public void separators() {
         check("a\nb\rc\r\nd\n\re", LineFormat.RAW_FORMAT,
                 5, "a\n", "b\r", "c\r\n", "d\n\r", "e");
     }
 
     //--
     
     private void check(String str, String ... expected) {
         check(str, world.getSettings().lineFormat, expected.length, expected);
     }
 
     private void check(String str, LineFormat format, int lastLine, String ... expected) {
         check(str, format, lastLine, 1024, expected);
         check(str, format, lastLine, 10, expected);
         check(str, format, lastLine, 7, expected);
         check(str, format, lastLine, 3, expected);
         check(str, format, lastLine, 1, expected);
     }
 
     private void check(String str, LineFormat format, int lastLine, int initialSize, String ... expected) {
         Node node;
         LineReader reader;
         List<String> result;
 
         try {
             node = world.memoryNode(str);
             reader = new LineReader(node.createReader(), format, initialSize);
             result = reader.collect();
         } catch (IOException e) {
             throw new IllegalStateException(e);
         }
         assertEquals(Arrays.asList(expected), result);
         assertEquals(lastLine, reader.getLine());
     }
 }
