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
 
 package com.oneandone.sushi.fs.webdav;
 
 import com.oneandone.sushi.fs.Node;
 import com.oneandone.sushi.fs.NodeReadOnlyTest;
 import org.junit.Test;
 
 import java.io.IOException;
 import java.net.URI;
 
 import static org.junit.Assert.assertEquals;
 import static org.junit.Assert.assertTrue;
 
 /** Accesses external hosts and might need proxy configuration => Full test */
 public class WebdavReadOnlyFullTest extends NodeReadOnlyTest {
     @Test
     public void normal() throws Exception {
         WebdavNode node;
 
         node = (WebdavNode) IO.node("http://englishediting.de/index.html");
         assertTrue(node.isFile());
         assertTrue(node.exists());
         assertTrue(node.readString().length() > 1);
         assertEquals("//englishediting.de/", node.getRoot().getId());
         assertEquals("index.html", node.getPath());
         assertEquals("", node.getParent().getPath());
     }
 
     @Test
     public void query() throws Exception {
         URI uri;
         WebdavNode node;
         String str;
 
         uri = URI.create("http://dict.tu-chemnitz.de:80/dings.cgi?lang=en&noframes=1&service=&query=foobarbaz&optword=1&optcase=1&opterrors=0&optpro=0&style=&dlink=self");
         node = (WebdavNode) IO.node(uri);
         assertEquals(uri, node.getURI());
        assertTrue(node.isFile());
         str = node.readString();
         assertTrue(str.contains("foobarbaz"));
     }
 
     @Test
     public void node() throws Exception {
         URI uri;
         WebdavNode node;
 
         uri = new URI("http://englishediting.de:80/index.html");
         node = (WebdavNode) IO.node(uri);
         assertEquals("index.html", node.getPath());
         assertEquals(uri, node.getURI());
     }
 
     @Override
     protected Node createWork() throws IOException {
         return IO.validNode("http://englishediting.de/foo");
     }
 }
 
