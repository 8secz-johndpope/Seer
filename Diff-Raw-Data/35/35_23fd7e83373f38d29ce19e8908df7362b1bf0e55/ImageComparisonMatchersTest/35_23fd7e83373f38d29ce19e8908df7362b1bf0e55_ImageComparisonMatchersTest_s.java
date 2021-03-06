 package net.avh4.util.imagecomparison;
 
 import org.hamcrest.Matcher;
 import org.junit.AfterClass;
 import org.junit.Test;
 import testpackage.HelperClass;
 
 import javax.imageio.ImageIO;
 import java.awt.image.BufferedImage;
 import java.io.File;
 import java.io.IOException;
 
 import static java.awt.image.BufferedImage.TYPE_INT_RGB;
 import static net.avh4.util.MatcherMatcher.doesNotMatch;
 import static org.hamcrest.MatcherAssert.assertThat;
 import static org.hamcrest.Matchers.not;
 
 public class ImageComparisonMatchersTest {
 
     private Matcher<?> matcher;
 
     @AfterClass
     public static void tearDown() {
         //noinspection ResultOfMethodCallIgnored
         new File("ImageComparisonMatchersTest.isApproved_whenApprovalImageDoesNotExist.png")
                 .delete();
     }
 
     @Test
     public void isApproved_withNullActual() throws Exception {
         matcher = ImageComparisonMatchers.isApproved();
        assertThat(matcher, doesNotMatch(null)
                .withDescription("don't know how to make an image of null"));
     }
 
     @Test
     public void isApproved_withActualThatCantBeMadeIntoAnImage() throws Exception {
         matcher = ImageComparisonMatchers.isApproved();
        assertThat(matcher, doesNotMatch(new SomeObject())
                .withDescription("don't know how to make an image of <{{ some object }}>"));
     }
 
     @Test
     public void isApproved_whenApprovalImageDoesNotExist() throws Exception {
         matcher = ImageComparisonMatchers.isApproved();
         final BufferedImage image = new BufferedImage(1, 1, TYPE_INT_RGB);
         assertThat(matcher, doesNotMatch(image).withDescription("approval image " +
                 "ImageComparisonMatchersTest.isApproved_whenApprovalImageDoesNotExist.png doesn't exist " +
                 "-- expected to find it in net.avh4.util.imagecomparison"));
     }
 
     @Test
     public void isApproved_withMatchingImage() throws Exception {
         matcher = ImageComparisonMatchers.isApproved();
         final BufferedImage image = ImageIO.read(getClass().getResource("/net/avh4/util/imagecomparison/ImageComparisonMatchersTest.isApproved_withMatchingImage.png"));
         assertThat(matcher, not(doesNotMatch(image)));
     }
 
     @Test
     public void isApproved_whenCalledThroughAHelperMethod() throws Exception {
         matcher = helperMethod();
         final BufferedImage image = ImageIO.read(getClass().getResource("/net/avh4/util/imagecomparison/ImageComparisonMatchersTest.isApproved_whenCalledThroughAHelperMethod.png"));
         assertThat(matcher, not(doesNotMatch(image)));
     }
 
     @Test
     public void isApproved_whenCalledThroughAHelperMethodInAClassInAnotherPackage() throws Exception {
         matcher = HelperClass.helperMethod();
         final BufferedImage image = ImageIO.read(getClass().getResource("/net/avh4/util/imagecomparison/ImageComparisonMatchersTest.isApproved_whenCalledThroughAHelperMethodInAClassInAnotherPackage.png"));
         assertThat(matcher, not(doesNotMatch(image)));
     }
 
     private Matcher<Object> helperMethod() throws IOException {
         return ImageComparisonMatchers.isApproved();
     }
 
     private static class SomeObject {
         @Override
         public String toString() {
             return "{{ some object }}";
         }
     }
 }
