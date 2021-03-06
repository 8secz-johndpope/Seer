 package npanday.its;
 
 /*
  * Copyright 2010
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 import java.io.File;
 
 import org.apache.maven.it.Verifier;
 import org.apache.maven.it.util.ResourceExtractor;
 
 public class NPandayIT0020Test
     extends AbstractNPandayIntegrationTestCase
 {
     public NPandayIT0020Test()
     {
         super( "(1.1,)" );
     }
 
     public void testEmbeddedResources()
         throws Exception
     {
         File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/NPandayIT0020" );
         Verifier verifier = getVerifier( testDir );
         verifier.executeGoal( "install" );
         verifier.assertFilePresent(
             new File( testDir, getAssemblyFile( "NPandayIT0020", "1.0.0.0", "dll" ) ).getAbsolutePath() );
         verifier.assertFilePresent(
            new File( testDir, getAssemblyResourceFile( "resgen/fix.gif" ) ).getAbsolutePath() );
         verifier.assertFilePresent(
             new File( testDir, getAssemblyResourceFile( "resgen/my-prop.x-properties" ) ).getAbsolutePath() );
 
         // TODO: test that DLL contains the embedded resource, probably by adding a NUnit test case that tries to load it
 
         verifier.verifyErrorFreeLog();
         verifier.resetStreams();
     }
 }
