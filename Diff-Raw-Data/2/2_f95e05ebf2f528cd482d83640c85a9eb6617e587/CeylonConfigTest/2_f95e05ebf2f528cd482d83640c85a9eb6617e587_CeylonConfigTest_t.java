 package com.redhat.ceylon.common.test;
 
 import java.io.File;
 import java.io.IOException;
 import java.util.Arrays;
 
 import junit.framework.Assert;
 
 import org.junit.Before;
 import org.junit.Test;
 
 import com.redhat.ceylon.common.CeylonConfig;
 import com.redhat.ceylon.common.ConfigParser;
 
 public class CeylonConfigTest {
 
     CeylonConfig testConfig;
     CeylonConfig localConfig;
     CeylonConfig mergedConfig;
     
     @Before
     public void setup() throws IOException {
         testConfig = ConfigParser.loadConfigFromFile(new File("test/src/com/redhat/ceylon/common/test/test.config"));
         localConfig = ConfigParser.loadLocalConfig(new File("test/src/com/redhat/ceylon/common/test"));
         mergedConfig = ConfigParser.loadConfigFromFile(new File("test/src/com/redhat/ceylon/common/test/test.config"));
         CeylonConfig localConfig2 = ConfigParser.loadLocalConfig(new File("test/src/com/redhat/ceylon/common/test"));
         mergedConfig.merge(localConfig2);
     }
     
     @Test
     public void testIsOptionDefined() {
         Assert.assertFalse(testConfig.isOptionDefined("foo.bar"));
         Assert.assertTrue(testConfig.isOptionDefined("test.string-hello"));
     }
     
     @Test
     public void testStrings() {
         Assert.assertEquals(null, testConfig.getOption("foo.bar"));
         Assert.assertEquals("test", testConfig.getOption("foo.bar", "test"));
         Assert.assertEquals("hello", testConfig.getOption("test.string-hello"));
         Assert.assertEquals("world", testConfig.getOption("test.string-world"));
         Assert.assertEquals(" with spaces   ", testConfig.getOption("test.string-spaces"));
         Assert.assertEquals("\"\\", testConfig.getOption("test.string-escapes"));
         Assert.assertEquals("aap\nnoot\nmies", testConfig.getOption("test.string-multiline"));
         Assert.assertEquals("aap\nnoot\nmies", testConfig.getOption("test.string-multiline-with-spaces"));
         Assert.assertEquals("aap\nnoot\nmies", testConfig.getOption("test.string-quoted-multiline"));
     }
     
     @Test
     public void testBooleans() {
         Assert.assertEquals(null, testConfig.getBoolOption("foo.bar"));
         Assert.assertEquals(true, testConfig.getBoolOption("foo.bar", true));
         Assert.assertEquals(Boolean.TRUE, testConfig.getBoolOption("test.boolean-true"));
         Assert.assertEquals(Boolean.FALSE, testConfig.getBoolOption("test.boolean-false-with-spaces"));
         Assert.assertEquals(Boolean.TRUE, testConfig.getBoolOption("test.boolean-on"));
         Assert.assertEquals(Boolean.FALSE, testConfig.getBoolOption("test.boolean-off"));
         Assert.assertEquals(Boolean.TRUE, testConfig.getBoolOption("test.boolean-yes"));
         Assert.assertEquals(Boolean.FALSE, testConfig.getBoolOption("test.boolean-no"));
         Assert.assertEquals(Boolean.TRUE, testConfig.getBoolOption("test.boolean-1"));
         Assert.assertEquals(Boolean.FALSE, testConfig.getBoolOption("test.boolean-0"));
         Assert.assertEquals(Boolean.TRUE, testConfig.getBoolOption("test.boolean-implicit-true"));
     }
     
     @Test
     public void testNumbers() {
         Assert.assertEquals(null, testConfig.getNumberOption("foo.bar"));
         Assert.assertEquals(1, testConfig.getNumberOption("foo.bar", 1));
         Assert.assertEquals(Long.valueOf(42), testConfig.getNumberOption("test.number"));
         Assert.assertEquals(Long.valueOf(123), testConfig.getNumberOption("test.number-with-spaces"));
     }
     
     @Test
     public void testCurrentDirs() {
         Assert.assertTrue(compareCurrentDir("test/src/com/redhat/ceylon/common/test", testConfig.getOption("test.currentdir-simple")));
         Assert.assertTrue(compareCurrentDir("test/src/com/redhat/ceylon/common/test/with/extra/path", testConfig.getOption("test.currentdir-extra")));
         Assert.assertTrue(compareCurrentDir("before/${DIR}", testConfig.getOption("test.currentdir-fake")));
     }
     
     @Test
     public void testCommentedStrings() {
         Assert.assertEquals("hello", testConfig.getOption("test.commented.string-hello"));
         Assert.assertEquals("world", testConfig.getOption("test.commented.string-world"));
         Assert.assertEquals(" with spaces   ", testConfig.getOption("test.commented.string-spaces"));
         Assert.assertEquals("\"\\", testConfig.getOption("test.commented.string-escapes"));
         Assert.assertEquals("aap\nnoot\nmies", testConfig.getOption("test.commented.string-multiline"));
         Assert.assertEquals("aap\nnoot\nmies", testConfig.getOption("test.commented.string-multiline-with-spaces"));
         Assert.assertEquals("aap\nnoot\nmies", testConfig.getOption("test.commented.string-quoted-multiline"));
     }
     
     @Test
     public void testCommentedBooleans() {
         Assert.assertEquals(Boolean.TRUE, testConfig.getBoolOption("test.commented.boolean-true"));
         Assert.assertEquals(Boolean.FALSE, testConfig.getBoolOption("test.commented.boolean-false-with-spaces"));
         Assert.assertEquals(Boolean.TRUE, testConfig.getBoolOption("test.commented.boolean-on"));
         Assert.assertEquals(Boolean.FALSE, testConfig.getBoolOption("test.commented.boolean-off"));
         Assert.assertEquals(Boolean.TRUE, testConfig.getBoolOption("test.commented.boolean-yes"));
         Assert.assertEquals(Boolean.FALSE, testConfig.getBoolOption("test.commented.boolean-no"));
         Assert.assertEquals(Boolean.TRUE, testConfig.getBoolOption("test.commented.boolean-1"));
         Assert.assertEquals(Boolean.FALSE, testConfig.getBoolOption("test.commented.boolean-0"));
         Assert.assertEquals(Boolean.TRUE, testConfig.getBoolOption("test.commented.boolean-implicit-true"));
     }
     
     @Test
     public void testCommentedNumbers() {
         Assert.assertEquals(Long.valueOf(42), testConfig.getNumberOption("test.commented.number"));
         Assert.assertEquals(Long.valueOf(123), testConfig.getNumberOption("test.commented.number-with-spaces"));
     }
     
     @Test
     public void testMultiple() {
         Assert.assertEquals(null, testConfig.getOptionValues("foo.bar"));
         Assert.assertTrue(compareStringArrays(new String[]{"aap", "noot", "mies"}, testConfig.getOptionValues("test.multiple.strings")));
     }
     
     @Test
     public void testSectionNames() {
        Assert.assertTrue(compareStringArraysSorted(new String[]{"test", "test.commented", "test.multiple", "test.section", "test.section.Aap", "test.section.Mies", "test.section.Noot"}, testConfig.getSectionNames(null)));
         Assert.assertTrue(compareStringArraysSorted(new String[]{"test"}, testConfig.getSectionNames("")));
         Assert.assertTrue(compareStringArraysSorted(new String[]{"commented", "multiple", "section"}, testConfig.getSectionNames("test")));
         Assert.assertTrue(compareStringArraysSorted(new String[]{"Aap", "Noot", "Mies"}, testConfig.getSectionNames("test.section")));
         Assert.assertTrue(compareStringArraysSorted(new String[]{}, testConfig.getSectionNames("test.multiple")));
     }
     
     @Test
     public void testOptionNames() {
         Assert.assertTrue(compareStringArrays(new String[]{"foo", "fooz"}, testConfig.getOptionNames("test.section.Mies")));
         Assert.assertTrue(compareStringArrays(new String[]{"local.dir", "test.string-hello", "test.multiple.strings", "local.dir2", "local.foo"}, localConfig.getOptionNames(null)));
     }
     
     @Test
     public void testLocalConfig() {
         Assert.assertEquals("bar", localConfig.getOption("local.foo", "notbar"));
         Assert.assertTrue(compareCurrentDir("test/.ceylon", localConfig.getOption("local.dir")));
         Assert.assertTrue(compareCurrentDir("test", localConfig.getOption("local.dir2")));
     }
     
     @Test
     public void testMergedConfig() {
         Assert.assertEquals("bar", mergedConfig.getOption("local.foo", "notbar"));
         Assert.assertEquals("hallo", mergedConfig.getOption("test.string-hello"));
         Assert.assertEquals("world", mergedConfig.getOption("test.string-world"));
         Assert.assertTrue(compareStringArrays(new String[]{"one", "two"}, mergedConfig.getOptionValues("test.multiple.strings")));
     }
     
     private boolean compareStringArrays(String[] one, String[] two) {
         return Arrays.equals(one, two);
     }
     
     private boolean compareStringArraysSorted(String[] one, String[] two) {
         Arrays.sort(one);
         Arrays.sort(two);
         return Arrays.equals(one, two);
     }
     
     private boolean compareCurrentDir(String one, String two) {
         File f1 = new File(one);
         File f2 = new File(two);
         try {
             return f1.getCanonicalFile().equals(f2.getCanonicalFile());
         } catch (IOException e) {
             return false;
         }
     }
 }
