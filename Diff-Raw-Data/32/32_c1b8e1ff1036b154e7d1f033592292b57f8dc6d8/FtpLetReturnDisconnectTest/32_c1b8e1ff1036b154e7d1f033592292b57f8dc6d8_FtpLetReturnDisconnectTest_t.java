 /*
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  *  http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing,
  * software distributed under the License is distributed on an
  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  * KIND, either express or implied.  See the License for the
  * specific language governing permissions and limitations
  * under the License.
  */
 
 package org.apache.ftpserver.ftplet;
 
 import java.io.ByteArrayInputStream;
 import java.io.ByteArrayOutputStream;
 import java.io.File;
 import java.io.IOException;
 import java.net.SocketException;
 import java.util.Properties;
 
 import org.apache.commons.net.ftp.FTPConnectionClosedException;
 import org.apache.ftpserver.clienttests.ClientTestTemplate;
 import org.apache.ftpserver.test.TestUtil;
 
 public class FtpLetReturnDisconnectTest extends ClientTestTemplate {
     private static final byte[] TESTDATA = "TESTDATA".getBytes();
     private static final byte[] DOUBLE_TESTDATA = "TESTDATATESTDATA".getBytes();
     private static final File TEST_FILE1 = new File(ROOT_DIR, "test1.txt");
     private static final File TEST_FILE2 = new File(ROOT_DIR, "test2.txt");
     private static final File TEST_DIR1 = new File(ROOT_DIR, "dir1");
     
     protected FtpletEnum mockReturnValue = FtpletEnum.RET_DISCONNECT;
     
     /*
      * (non-Javadoc)
      * 
      * @see org.apache.ftpserver.clienttests.ClientTestTemplate#setUp()
      */
     protected void setUp() throws Exception {
         MockFtplet.callback = new MockFtpletCallback();
         
         initDirs();
 
         initServer();
 
         connectClient();
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see org.apache.ftpserver.clienttests.ClientTestTemplate#createConfig()
      */
     protected Properties createConfig() {
         Properties config = createDefaultConfig();
 
         config.setProperty("config.ftplets", "f1");
         config.setProperty("config.ftplet.f1.class", 
                 MockFtplet.class.getName());
         // #config.ftplet.f1.param=value1
         return config;
     }
 
 /*    public void testExceptionDuringInit() throws Exception {
         MockFtplet.callback = new MockFtpletCallback() {
             public void init(FtpletContext ftpConfig, Configuration config)
                     throws FtpException {
                 throw new RuntimeException();
             }
         };
 
         try {
             initServer();
             fail("Must throw FtpException");
         } catch (FtpException e) {
             // OK
         }
     }*/
 
     public void testExceptionDuringLogin() throws Exception {
         MockFtplet.callback = new MockFtpletCallback() {
             public FtpletEnum onLogin(FtpSession session, FtpRequest request, FtpReplyOutput response)
                     throws FtpException, IOException {
                 throwException();
                 return mockReturnValue;
             }
         };
 
         try {
             client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
             fail("Must throw FTPConnectionClosedException");
         } catch (FTPConnectionClosedException e) {
             // OK
         }
     }
 
     public void testExceptionDuringDeleteStart() throws Exception {
         MockFtplet.callback = new MockFtpletCallback() {
             public FtpletEnum onDeleteStart(FtpSession session, FtpRequest request, FtpReplyOutput response) throws FtpException, IOException {
                 throwException();
                 return mockReturnValue;
             }
         };
         
         TestUtil.writeDataToFile(TEST_FILE1, TESTDATA);
         
         client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
         try {
             client.deleteFile(TEST_FILE1.getName());
             fail("Must throw FTPConnectionClosedException");
         } catch (FTPConnectionClosedException e) {
             // OK
         }
         
         assertTrue(TEST_FILE1.exists());
     }
 
     public void testExceptionDuringDeleteEnd() throws Exception {
         MockFtplet.callback = new MockFtpletCallback() {
             public FtpletEnum onDeleteEnd(FtpSession session, FtpRequest request, FtpReplyOutput response) throws FtpException, IOException {
                 throwException();
                 return mockReturnValue;
             }
         };
         
         TestUtil.writeDataToFile(TEST_FILE1, TESTDATA);
         
         client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
         assertTrue(client.deleteFile(TEST_FILE1.getName()));
         try {
             client.noop();
             fail("Must throw FTPConnectionClosedException");
         } catch (FTPConnectionClosedException e) {
             // OK
         } catch (SocketException e) {
         	// OK
         }
         
         assertFalse(TEST_FILE1.exists());
     }
 
     public void testExceptionDuringMkdirStart() throws Exception {
         MockFtplet.callback = new MockFtpletCallback() {
             public FtpletEnum onMkdirStart(FtpSession session, FtpRequest request, FtpReplyOutput response) throws FtpException, IOException {
                 throwException();
                 return mockReturnValue;
             }
         };
         
         client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
         try {
             client.makeDirectory(TEST_DIR1.getName());
             fail("Must throw FTPConnectionClosedException");
         } catch (FTPConnectionClosedException e) {
             // OK
         }
         
         assertFalse(TEST_DIR1.exists());
     }
 
     public void testExceptionDuringMkdirEnd() throws Exception {
         MockFtplet.callback = new MockFtpletCallback() {
             public FtpletEnum onMkdirEnd(FtpSession session, FtpRequest request, FtpReplyOutput response) throws FtpException, IOException {
                 throwException();
                 return mockReturnValue;
             }
         };
         
         client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
         assertTrue(client.makeDirectory(TEST_DIR1.getName()));
         try {
             client.noop();
             fail("Must throw FTPConnectionClosedException");
         } catch (FTPConnectionClosedException e) {
             // OK
         } catch (SocketException e) {
         	// OK
         }
         
         assertTrue(TEST_DIR1.exists());
     }
 
     public void testExceptionDuringRmdirStart() throws Exception {
         MockFtplet.callback = new MockFtpletCallback() {
             public FtpletEnum onRmdirStart(FtpSession session, FtpRequest request, FtpReplyOutput response) throws FtpException, IOException {
                 throwException();
                 return mockReturnValue;
             }
         };
         
         TEST_DIR1.mkdirs();
         
         client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
         try {
             client.removeDirectory(TEST_DIR1.getName());
             fail("Must throw FTPConnectionClosedException");
         } catch (FTPConnectionClosedException e) {
             // OK
         }
         
         assertTrue(TEST_DIR1.exists());
     }
 
     public void testExceptionDuringRmdirEnd() throws Exception {
         MockFtplet.callback = new MockFtpletCallback() {
             public FtpletEnum onRmdirEnd(FtpSession session, FtpRequest request, FtpReplyOutput response) throws FtpException, IOException {
                 throwException();
                 return mockReturnValue;
             }
         };
         
         TEST_DIR1.mkdirs();
         
         client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
         assertTrue(client.removeDirectory(TEST_DIR1.getName()));
         try {
             client.noop();
             fail("Must throw FTPConnectionClosedException");
         } catch (FTPConnectionClosedException e) {
             // OK
         } catch (SocketException e) {
         	// OK
         }
         
         assertFalse(TEST_DIR1.exists());
     }
 
     public void testExceptionDuringSite() throws Exception {
         MockFtplet.callback = new MockFtpletCallback() {
             public FtpletEnum onSite(FtpSession session, FtpRequest request, FtpReplyOutput response) throws FtpException, IOException {
                 throwException();
                 return mockReturnValue;
             }
         };
         
         client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
         try {
             client.site("HELP");
             fail("Must throw FTPConnectionClosedException");
         } catch (FTPConnectionClosedException e) {
             // OK
         }
     }
 
     public void testExceptionDuringRenameStart() throws Exception {
         MockFtplet.callback = new MockFtpletCallback() {
             public FtpletEnum onRenameStart(FtpSession session, FtpRequest request, FtpReplyOutput response) throws FtpException, IOException {
                 throwException();
                 return mockReturnValue;
             }
         };
         
         TestUtil.writeDataToFile(TEST_FILE1, TESTDATA);
         
         client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
         try {
             client.rename(TEST_FILE1.getName(), TEST_FILE2.getName());
             fail("Must throw FTPConnectionClosedException");
         } catch (FTPConnectionClosedException e) {
             // OK
         }
         
         assertTrue(TEST_FILE1.exists());
         assertFalse(TEST_FILE2.exists());
     }
     
     public void testExceptionDuringRenameEnd() throws Exception {
         MockFtplet.callback = new MockFtpletCallback() {
             public FtpletEnum onRenameEnd(FtpSession session, FtpRequest request, FtpReplyOutput response) throws FtpException, IOException {
                 throwException();
                 return mockReturnValue;
             }
         };
         
         TestUtil.writeDataToFile(TEST_FILE1, TESTDATA);
         
         client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
         assertTrue(client.rename(TEST_FILE1.getName(), TEST_FILE2.getName()));
         try {
             client.noop();
             fail("Must throw FTPConnectionClosedException");
         } catch (FTPConnectionClosedException e) {
             // OK
         } catch (SocketException e) {
         	// OK
         }
 
         assertFalse(TEST_FILE1.exists());
         assertTrue(TEST_FILE2.exists());
 
     }
 
     public void testExceptionDuringDownloadStart() throws Exception {
         MockFtplet.callback = new MockFtpletCallback() {
             public FtpletEnum onDownloadStart(FtpSession session, FtpRequest request, FtpReplyOutput response) throws FtpException, IOException {
                 throwException();
                 return mockReturnValue;
             }
         };
         
         TestUtil.writeDataToFile(TEST_FILE1, TESTDATA);
         
         client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
         try {
             client.retrieveFileStream(TEST_FILE1.getName());
             fail("Must throw FTPConnectionClosedException");
         } catch (FTPConnectionClosedException e) {
             // OK
         }
     }
 
     public void testExceptionDuringDownloadEnd() throws Exception {
         MockFtplet.callback = new MockFtpletCallback() {
             public FtpletEnum onDownloadEnd(FtpSession session, FtpRequest request, FtpReplyOutput response) throws FtpException, IOException {
                 throwException();
                 return mockReturnValue;
             }
         };
         
         TestUtil.writeDataToFile(TEST_FILE1, TESTDATA);
         
         client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         assertTrue(client.retrieveFile(TEST_FILE1.getName(), baos));
         try {
             client.noop();
             fail("Must throw FTPConnectionClosedException");
         } catch (FTPConnectionClosedException e) {
             // OK
         }
     
         TestUtil.assertArraysEqual(TESTDATA, baos.toByteArray());
     }
 
     public void testExceptionDuringAppendStart() throws Exception {
         MockFtplet.callback = new MockFtpletCallback() {
             public FtpletEnum onAppendStart(FtpSession session, FtpRequest request, FtpReplyOutput response) throws FtpException, IOException {
                 throwException();
                 return mockReturnValue;
             }
         };
         
         TestUtil.writeDataToFile(TEST_FILE1, TESTDATA);
         
         client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
         try {
             client.appendFile(TEST_FILE1.getName(), new ByteArrayInputStream(TESTDATA));
             fail("Must throw FTPConnectionClosedException");
         } catch (FTPConnectionClosedException e) {
             // OK
         }
         
         TestUtil.assertFileEqual(TESTDATA, TEST_FILE1);
     }
     
     public void testExceptionDuringAppendEnd() throws Exception {
         MockFtplet.callback = new MockFtpletCallback() {
             public FtpletEnum onAppendEnd(FtpSession session, FtpRequest request, FtpReplyOutput response) throws FtpException, IOException {
                 throwException();
                 return mockReturnValue;
             }
         };
         
         TestUtil.writeDataToFile(TEST_FILE1, TESTDATA);
         
         client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
         assertTrue(client.appendFile(TEST_FILE1.getName(), new ByteArrayInputStream(TESTDATA)));
         try {
             client.noop();
             fail("Must throw FTPConnectionClosedException");
         } catch (FTPConnectionClosedException e) {
             // OK
         }
         
         TestUtil.assertFileEqual(DOUBLE_TESTDATA, TEST_FILE1);
     }
 
     public void testExceptionDuringUploadStart() throws Exception {
         MockFtplet.callback = new MockFtpletCallback() {
             public FtpletEnum onUploadStart(FtpSession session, FtpRequest request, FtpReplyOutput response) throws FtpException, IOException {
                 throwException();
                 return mockReturnValue;
             }
         };
         
         client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
         try {
             client.storeFile(TEST_FILE1.getName(), new ByteArrayInputStream(TESTDATA));
             fail("Must throw FTPConnectionClosedException");
         } catch (FTPConnectionClosedException e) {
             // OK
         }
         
         assertFalse(TEST_FILE1.exists());
     }
     
     public void testExceptionDuringUploadEnd() throws Exception {
         MockFtplet.callback = new MockFtpletCallback() {
             public FtpletEnum onUploadEnd(FtpSession session, FtpRequest request, FtpReplyOutput response) throws FtpException, IOException {
                 throwException();
                 return mockReturnValue;
             }
         };
         
         client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
         assertTrue(client.storeFile(TEST_FILE1.getName(), new ByteArrayInputStream(TESTDATA)));
         try {
             client.noop();
             fail("Must throw FTPConnectionClosedException");
         } catch (FTPConnectionClosedException e) {
             // OK
         }
         
         TestUtil.assertFileEqual(TESTDATA, TEST_FILE1);
     }
     
     public void testExceptionDuringUploadUniqueStart() throws Exception {
         MockFtplet.callback = new MockFtpletCallback() {
             public FtpletEnum onUploadUniqueStart(FtpSession session, FtpRequest request, FtpReplyOutput response) throws FtpException, IOException {
                 throwException();
                 return mockReturnValue;
             }
         };
         
         client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
         try {
             client.storeUniqueFile(TEST_FILE1.getName(), new ByteArrayInputStream(TESTDATA));
             fail("Must throw FTPConnectionClosedException");
         } catch (FTPConnectionClosedException e) {
             // OK
         }
         
         assertEquals(ROOT_DIR.listFiles().length, 0);
     }
     
     public void testExceptionDuringUploadUniqueEnd() throws Exception {
         MockFtplet.callback = new MockFtpletCallback() {
             public FtpletEnum onUploadUniqueEnd(FtpSession session, FtpRequest request, FtpReplyOutput response) throws FtpException, IOException {
                 throwException();
                 
                 return mockReturnValue;
             }
         };
         
         client.login(ADMIN_USERNAME, ADMIN_PASSWORD);
         assertTrue(client.storeUniqueFile(new ByteArrayInputStream(TESTDATA)));
         try {
             client.noop();
             fail("Must throw FTPConnectionClosedException");
         } catch (FTPConnectionClosedException e) {
             // OK
         }
         
         TestUtil.assertFileEqual(TESTDATA, ROOT_DIR.listFiles()[0]);
     }
 
     protected void throwException() throws FtpException, IOException {
         // do not throw, we want to check the result of return values
     }
 }
