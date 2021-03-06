 /*
  * Copyright 2000-2011 JetBrains s.r.o.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package jetbrains.buildServer.nuget.tests.integration.feed.server;
 
 import com.intellij.execution.configurations.GeneralCommandLine;
 import jetbrains.buildServer.ExecResult;
 import jetbrains.buildServer.SimpleCommandLineProcessRunner;
 import jetbrains.buildServer.nuget.server.feed.reader.FeedPackage;
 import jetbrains.buildServer.nuget.server.feed.reader.impl.Param;
 import jetbrains.buildServer.nuget.tests.integration.NuGet;
 import jetbrains.buildServer.nuget.tests.integration.Paths;
 import jetbrains.buildServer.nuget.tests.integration.http.SimpleHttpServer;
 import jetbrains.buildServer.nuget.tests.integration.http.SimpleHttpServerBase;
 import org.jetbrains.annotations.NotNull;
 import org.jetbrains.annotations.Nullable;
 import org.testng.Assert;
 import org.testng.annotations.Test;
 
 import java.io.File;
 import java.io.IOException;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.Collections;
 
 /**
  * @author Eugene Petrenko (eugene.petrenko@gmail.com)
  *         Date: 21.10.11 17:38
  */
 public class NuGetServerFeedIntegrationTest extends NuGetServerIntegrationTestBase {
 
   @Test
   public void testOnePackageFeed() throws Exception {
     enableDebug();
 
     final String packageId = "CommonServiceLocator";
     final File responseFile = createTempFile();
 
     final String name = packageId + ".1.0.nupkg";
     renderPackagesResponseFile(responseFile, Paths.getTestDataPath("/packages/" + name));
     registerHttpHandler(packagesFileHandler(responseFile));
 
     assertOwn().run();
 
     assert200("/Packages()").run();
     assert200("/////Packages()").run();
     assert200("/Packages()", new Param("$filter", "Id eq '" + packageId + "'")).run();
     assert200("////Packages()", new Param("$filter", "Id eq '" + packageId + "'")).run();
 
     final Collection<FeedPackage> packages = myFeedReader.queryPackageVersions(myNuGetServerUrl, packageId);
     Assert.assertTrue(packages.size() == 1);
     final FeedPackage pkg = packages.iterator().next();
 
    final String downloadUrl = pkg.getDownloadUrl();
    final String ending = myPaths.getArtifactsDownloadUrlWithTokenBase() + "42/some/package/download/" + name;
    Assert.assertTrue(downloadUrl.endsWith(ending), "actual url: " + downloadUrl + ", must end with " + ending);
   }
 
   @Test
   public void testConcurrency() throws Exception {
     enableDebug();
 
     final String packageId = "CommonServiceLocator";
     final File responseFile = createTempFile();
 
     renderPackagesResponseFile(responseFile, Paths.getTestDataPath("/packages/" + packageId + ".1.0.nupkg"));
     registerHttpHandler(packagesFileHandler(responseFile));
 
     runAsyncAndFailOnException(
             10,
             assert200("/Packages()"),
             assert200("/////Packages()"),
             assert200("/Packages()", new Param("$filter", "Id eq '" + packageId + "'")),
             assert200("////Packages()", new Param("$filter", "Id eq '" + packageId + "'"))
     );
   }
 
   @Test
   public void testTwoPackagesFeed() throws Exception {
     enableDebug();
 
     final String packageId_1 = "CommonServiceLocator";
     final String packageId_2 = "NuGet.Core";
 
     final File responseFile = createTempFile();
     renderPackagesResponseFile(
             responseFile,
             Paths.getTestDataPath("/packages/" + packageId_1 + ".1.0.nupkg"),
             Paths.getTestDataPath("/packages/" + packageId_2 + ".1.5.20902.9026.nupkg")
             );
 
     registerHttpHandler(packagesFileHandler(responseFile));
 
 
     assert200("/Packages()").run();
     assert200("/////Packages()").run();
     assert200("/Packages()", new Param("$filter", "Id eq '" + packageId_1 + "'")).run();
     assert200("////Packages()", new Param("$filter", "Id eq '" + packageId_1 + "'")).run();
     assert200("/Packages()", new Param("$filter", "Id eq '" + packageId_2 + "'")).run();
     assert200("////Packages()", new Param("$filter", "Id eq '" + packageId_2 + "'")).run();
 
     Assert.assertTrue(myFeedReader.queryPackageVersions(myNuGetServerUrl, packageId_1).size() > 0);
     Assert.assertTrue(myFeedReader.queryPackageVersions(myNuGetServerUrl, packageId_2).size() > 0);
   }
 
   @Test
   public void testNuGetClientReadsFeed_15() throws Exception{
     enableDebug();
 
     final String packageId_1 = "CommonServiceLocator";
     final String packageId_2 = "NuGet.Core";
 
     final File responseFile = createTempFile();
     renderPackagesResponseFile(
             responseFile,
             Paths.getTestDataPath("/packages/" + packageId_1 + ".1.0.nupkg"),
             Paths.getTestDataPath("/packages/" + packageId_2 + ".1.5.20902.9026.nupkg")
             );
 
     registerHttpHandler(packagesFileHandler(responseFile));
 
 
 
     GeneralCommandLine cmd = new GeneralCommandLine();
     cmd.setExePath(NuGet.NuGet_1_5.getPath().getPath());
     cmd.addParameter("list");
     cmd.addParameter("-Source");
     cmd.addParameter(myNuGetServerUrl);
 
     final ExecResult exec = SimpleCommandLineProcessRunner.runCommand(cmd, null);
     Assert.assertEquals(exec.getExitCode(), 0);
     final String stdout = exec.getStdout();
     System.out.println(stdout);
     Assert.assertTrue(stdout.contains(packageId_1), stdout);
     Assert.assertTrue(stdout.contains(packageId_2), stdout);
   }
 
   @Test(dependsOnGroups = "should create http server for artifacts downlaod")
   public void testNuGetClientInstall_15() throws Exception{
     enableDebug();
 
     final String packageId_1 = "CommonServiceLocator";
     final String packageId_2 = "NuGet.Core";
 
     final File responseFile = createTempFile();
     final File file1 = Paths.getTestDataPath("/packages/" + packageId_1 + ".1.0.nupkg");
     final File file2 = Paths.getTestDataPath("/packages/" + packageId_2 + ".1.5.20902.9026.nupkg");
 
     renderPackagesResponseFile(
             responseFile,
             file1,
             file2
             );
 
     registerHttpHandler(packagesFileHandler(responseFile));
 
     final File home = createTempDir();
 
     registerHttpHandler(new HttpServerHandler() {
       public SimpleHttpServerBase.Response processRequest(@NotNull String requestLine, @Nullable String path) {
         if ((myPaths.getArtifactsDownloadUrlWithTokenBase() + "/42/" + file1.getName()).equals(path)) {
           return SimpleHttpServer.getFileResponse(file1, Arrays.asList("Content-Type: "));
         }
         return null;
       }
     });
 
 
     GeneralCommandLine cmd = new GeneralCommandLine();
     cmd.setExePath(NuGet.NuGet_1_5.getPath().getPath());
     cmd.setWorkingDirectory(home);
     cmd.addParameter("install");
     cmd.addParameter(packageId_1);
     cmd.addParameter("-Source");
     cmd.addParameter(myNuGetServerUrl);
 
     final ExecResult exec = SimpleCommandLineProcessRunner.runCommand(cmd, null);
     System.out.println(exec.getStdout());
     System.out.println(exec.getStderr());
 
     Assert.assertEquals(exec.getExitCode(), 0);
 
     Assert.assertTrue(new File(home, packageId_1 + ".1.0").isDirectory());
     Assert.assertTrue(new File(home, packageId_1 + ".1.0/" + packageId_1 + ".1.0.nupkg").isFile());
   }
 
   @NotNull
   protected HttpServerHandler packagesFileHandler(@NotNull final File responseFile) throws IOException {
     return new HttpServerHandler() {
       public SimpleHttpServerBase.Response processRequest(@NotNull String requestLine, @Nullable String path) {
         if (!(myHttpContextUrl + "/packages-metadata.html").equals(path)) return null;
 
         if (checkContainsToken(requestLine)) {
           return SimpleHttpServer.getFileResponse(responseFile, Arrays.asList("Content-Type: text/plain; encoding=UTF-8"));
         } else {
           System.out.println("Failed to find authorization token in request!");
           return SimpleHttpServer.createStreamResponse(SimpleHttpServer.STATUS_LINE_500, Collections.<String>emptyList(), "invalid token".getBytes());
         }
       }
     };
   }
 
 }
