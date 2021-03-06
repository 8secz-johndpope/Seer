 import java.io.BufferedReader;
 import java.io.File;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.io.OutputStream;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.net.URLConnection;
 import java.net.UnknownHostException;
 import java.util.HashMap;
 import java.util.Map;
 import javax.xml.stream.XMLEventReader;
 import javax.xml.stream.XMLInputFactory;
 import javax.xml.stream.XMLStreamException;
 import javax.xml.stream.events.XMLEvent;
 import org.bukkit.Bukkit;
 import org.bukkit.plugin.InvalidDescriptionException;
 import org.bukkit.plugin.InvalidPluginException;
 import org.bukkit.plugin.UnknownDependencyException;
 
 /**
  * This class is for integrating QuarterBukkit into a plugin.
  */
 public class QuarterBukkitIntegration {
 
     private static final String TITLE_TAG = "title";
     private static final String LINK_TAG  = "link";
     private static final String ITEM_TAG  = "item";
 
     private static URL          feedUrl;
 
     static {
         try {
             feedUrl = new URL("http://dev.bukkit.org/server-mods/QuarterCode");
         }
         catch (final MalformedURLException e) {
             e.printStackTrace();
         }
     }
 
     /**
      * Call this method in onLoad() for integrating QuarterBukkit into your plugin.
      * It simply installs QuarterBukkit if it isn't.
      */
     public static void integrate() {
 
         final File file = new File("plugins", "QuarterBukkit.jar");
 
         try {
             if (!Bukkit.getPluginManager().isPluginEnabled("QuarterBukkit")) {
                 install(file);
             }
         }
         catch (final UnknownHostException e) {
             Bukkit.getLogger().warning("Can't connect to dev.bukkit.org!");
         }
         catch (final Exception e) {
            Bukkit.getLogger().severe("An error occurred while updating QuarterBukkit: " + e.getClass() + ": " + e.getLocalizedMessage());
         }
     }
 
     private static void install(final File file) throws IOException, XMLStreamException, UnknownDependencyException, InvalidPluginException, InvalidDescriptionException {
 
         Bukkit.getLogger().info("Installing QuarterBukkit ...");
 
         final URL url = new URL(getFileURL(getFeedData().get("link")));
         final InputStream inputStream = url.openStream();
         final OutputStream outputStream = new FileOutputStream(file);
         outputStream.flush();
 
         final byte[] tempBuffer = new byte[4096];
         int counter;
         while ( (counter = inputStream.read(tempBuffer)) > 0) {
             outputStream.write(tempBuffer, 0, counter);
             outputStream.flush();
         }
 
         inputStream.close();
         outputStream.close();
 
         Bukkit.getPluginManager().enablePlugin(Bukkit.getPluginManager().loadPlugin(file));
     }
 
     private static String getFileURL(final String link) throws IOException {
 
         final URL url = new URL(link);
         URLConnection connection = url.openConnection();
         final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
 
         String line;
         while ( (line = reader.readLine()) != null) {
             if (line.contains("<li class=\"user-action user-action-download\">")) {
                 return line.split("<a href=\"")[1].split("\">Download</a>")[0];
             }
         }
         connection = null;
         reader.close();
 
         return null;
     }
 
     private static Map<String, String> getFeedData() throws IOException, XMLStreamException {
 
         final Map<String, String> returnMap = new HashMap<String, String>();
         String title = null;
         String link = null;
 
         final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
         final InputStream inputStream = feedUrl.openStream();
         final XMLEventReader eventReader = inputFactory.createXMLEventReader(inputStream);
 
         while (eventReader.hasNext()) {
             XMLEvent event = eventReader.nextEvent();
             if (event.isStartElement()) {
                 if (event.asStartElement().getName().getLocalPart().equals(TITLE_TAG)) {
                     event = eventReader.nextEvent();
                     title = event.asCharacters().getData();
                     continue;
                 }
                 if (event.asStartElement().getName().getLocalPart().equals(LINK_TAG)) {
                     event = eventReader.nextEvent();
                     link = event.asCharacters().getData();
                     continue;
                 }
             } else if (event.isEndElement()) {
                 if (event.asEndElement().getName().getLocalPart().equals(ITEM_TAG)) {
                     returnMap.put("title", title);
                     returnMap.put("link", link);
                     return returnMap;
                 }
             }
         }
 
         return returnMap;
     }
 
     private QuarterBukkitIntegration() {
 
     }
 
 }
