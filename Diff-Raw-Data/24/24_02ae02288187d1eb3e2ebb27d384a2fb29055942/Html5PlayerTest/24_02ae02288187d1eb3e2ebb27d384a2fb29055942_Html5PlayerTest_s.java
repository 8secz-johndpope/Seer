 package de.agilecoders.wicket.extensions.markup.html.bootstrap.html5player;
 
 import com.google.common.collect.Lists;
 import de.agilecoders.wicket.extensions.markup.html.bootstrap.WicketApplicationTest;
 import org.apache.wicket.model.Model;
 import org.apache.wicket.util.string.Strings;
 import org.junit.Test;
 
 import java.util.List;
 
 /**
  * Tests the {@link Html5Player} class
  *
  * @author miha
  */
 public class Html5PlayerTest extends WicketApplicationTest {
 
    /**
     * TODO: ugly test...
     */
     @Test
     public void componentIsRendered() {
         List<Html5Player.IVideo> videoList = Lists.<Html5Player.IVideo>newArrayList(
                 new Video("url", "type")
         );
         Html5Player player = new Html5Player("video", Model.ofList(videoList));
 
         tester().startComponentInPage(player);
 
        String newLine = System.lineSeparator();
 
        tester().assertContains(Strings.replaceAll("<wicket:panel>\n"
                                + "            <video wicket:id=\"video\" width=\"370\" height=\"215\" id=\"video1\">\n"
                                + "                <wicket:fragment wicket:id=\"videos\"><source wicket:id=\"element\" src=\"url\" type=\"type\"/></wicket:fragment>\n"
                                + "                Your browser does not support the video tag.\n"
                                + "            </video>\n"
                                + "        </wicket:panel>", "\n", newLine).toString());
     }
 
 }
