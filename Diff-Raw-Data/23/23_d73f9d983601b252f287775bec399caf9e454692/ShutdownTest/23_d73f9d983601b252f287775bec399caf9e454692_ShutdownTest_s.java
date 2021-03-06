 /*
    Copyright 2008-2009 Christian Vest Hansen
 
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
 
        http://www.apache.org/licenses/LICENSE-2.0
 
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
  */
 package net.nanopool;
 
 import java.sql.Connection;
 import static org.junit.Assert.*;
 
 import java.sql.SQLException;
 import java.util.List;
 import org.junit.Test;
 
 /**
  *
  * @author cvh
  */
 public class ShutdownTest extends NanoPoolTestBase {
     @Test
     public void shutDownPoolsMustRefuseToConnect() throws SQLException {
         pool = npds();
         Connection con = pool.getConnection();
         assertNotNull(con);
         con.close();
        List sqles = pool.close();
         assertTrue("Got exceptions from shutdown.", sqles.isEmpty());
         try {
             pool.getConnection();
             fail("getConnection did not throw.");
         } catch (IllegalStateException ile) {
             assertEquals(FsmMixin.MSG_SHUT_DOWN, ile.getMessage());
         }
     }
 }
