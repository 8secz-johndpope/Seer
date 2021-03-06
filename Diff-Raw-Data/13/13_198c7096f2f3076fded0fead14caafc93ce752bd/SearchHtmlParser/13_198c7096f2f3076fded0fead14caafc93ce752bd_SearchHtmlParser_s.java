 package at.yawk.fimfiction.core;
 
 import at.yawk.fimfiction.data.*;
 import at.yawk.fimfiction.data.Optional;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.text.ParseException;
 import java.text.SimpleDateFormat;
 import java.util.*;
 import javax.annotation.Nonnull;
 import javax.annotation.Nullable;
 import org.xml.sax.Attributes;
 import org.xml.sax.SAXException;
 
 import static at.yawk.fimfiction.data.Story.StoryKey.*;
 
 /**
  * Class for parsing pretty much any data in search HTML.
  *
  * @author Yawkat
  */
 class SearchHtmlParser extends SearchParser {
     /*
      * Do not waste your time trying to understand if you don't need to.
      * class is highly complex and messy but that is hardly avoidable with
      * complex lexer parser classes.
      */
 
     final SimpleDateFormat fimfictionDateFormat = new SimpleDateFormat("d MMM yyyy", Locale.ENGLISH);
 
     boolean idOnly;
 
     int stage;
 
     @Nullable Story story;
 
     StringBuilder title = new StringBuilder();
     @Nullable User author;
     @Nullable StringBuilder authorName;
     @Nullable Set<Category> categories;
     @Nullable FormattedStringParser.FormattedStringHandler description;
     @Nullable Chapter chapter;
     @Nullable StringBuilder chapterTitle;
     @Nullable Set<FimCharacter> characters;
     @Nullable Set<Shelf> shelves;
     @Nullable Set<Shelf> shelvesNotAdded;
     @Nullable List<Chapter> chapters;
     int characterId;
     @Nullable Optional<User> loggedIn;
     @Nullable String nonce;
     @Nullable Set<Shelf> globalShelves;
 
     @Override
     public void startElement(@Nonnull String uri,
                              @Nonnull String localName,
                              @Nonnull String qName,
                              @Nonnull Attributes attributes) throws SAXException {
         switch (stage) {
         case 0:
             if (qName.equals("div")) {
                 String clazz = attributes.getValue("class");
 
                 if ("story_container".equals(clazz)) { // story container
                     stage = 1;
                     story = Story.createMutable();
                 } else if ("menu_list bookshelves".equals(clazz)) { // bookshelf list
                     globalShelves = new HashSet<Shelf>();
                     stage = 500;
                 }
             }
             break;
         case 991:
             if (qName.equals("a")) { // like button (no onclick when logged out)
                 String onclick = attributes.getValue("onclick");
                 if (onclick != null) {
                     assert story != null;
                     story.set(Story.StoryKey.RATING_TOKEN,
                               onclick.substring(onclick.indexOf('\'') + 1, onclick.lastIndexOf('\'')));
                 }
                 stage = 992;
             }
             break;
         case 992:
         case 994:
             if (qName.equals("span")) { // (dis-)like span
                 stage++;
             }
             break;
         case 996:
             if (qName.equals("div") && "story_content_box".equals(attributes.getValue("class"))) { // story id
                 assert story != null;
                 story.set(Story.StoryKey.ID, toIntLenient(attributes.getValue("id")));
                 stage = 883;
             }
             break;
         case 997:
             if (qName.equals("a")) { // content rating
                 String contentRating = attributes.getValue("class").substring(15);
                 assert story != null;
                 ContentRating rating = ContentRating.forId(contentRating);
                 assert rating != null;
                 story.set(Story.StoryKey.CONTENT_RATING, rating);
                 stage = 998;
             }
             break;
         case 998:
             if (qName.equals("a")) { // title tag
                 try {
                     assert story != null;
                     story.set(Story.StoryKey.URL, new URL(Constants.BASE_URL + attributes.getValue("href")));
                 } catch (MalformedURLException e) {
                     throw new SAXException(e);
                 }
                 title.setLength(0);
                 stage = 999;
             }
             break;
         case 880:
             if (qName.equals("span")) {
                 assert story != null;
                 story.set(COMMENT_COUNT, toIntLenient(attributes.getValue("title")));
                 stage = 881;
             }
             break;
         case 881:
             if (qName.equals("span")) {
                 assert story != null;
                 story.set(VIEW_COUNT_TOTAL, toIntLenient(attributes.getValue("title")));
                 shelves = new HashSet<Shelf>();
                 shelvesNotAdded = new HashSet<Shelf>();
                 stage = 770;
             }
             break;
         case 883:
             if (qName.equals("span")) {
                 assert story != null;
                StoryStatus status = StoryStatus.forId(attributes.getValue("class").substring(17));
                assert status != null;
                 story.set(STATUS, status);
                 stage = 997;
             }
             break;
         case 770:
             if (qName.equals("li")) {
                 String idString = attributes.getValue("data-bookshelf");
                 if (idString != null) {
                     int id = Integer.parseInt(idString);
                     String title = attributes.getValue("title");
                     boolean selected = "1".equals(attributes.getValue("data-added"));
                     Shelf shelf = Shelf.createMutable();
                     shelf.set(Shelf.ShelfKey.ID, id);
                     shelf.set(Shelf.ShelfKey.NAME, title);
                     if (selected) {
                         assert shelves != null;
                         shelves.add(shelf);
                     } else {
                         assert shelvesNotAdded != null;
                         shelvesNotAdded.add(shelf);
                     }
                 }
             }
             break;
         case 500:
             if ("li".equals(qName)) {
                 String idString = attributes.getValue("data-id");
                 if (idString != null) {
                     int id = Integer.parseInt(idString);
                     String name = attributes.getValue("data-name");
                     assert globalShelves != null;
                     globalShelves.add(Shelf.createMutable().set(Shelf.ShelfKey.ID, id).set(Shelf.ShelfKey.NAME, name));
                 }
             }
             break;
         case 1:
             String src = attributes.getValue("src");
             author = User.createMutable();
             if (!"//www.fimfiction-static.net/images/avatars/none_64.png".equals(src)) {
                 try {
                     author.set(User.UserKey.URL_PROFILE_IMAGE, Optional.existing(new URL("http:" + src)));
                 } catch (MalformedURLException e) {
                     throw new SAXException(e);
                 }
             } else {
                 author.set(User.UserKey.URL_PROFILE_IMAGE, Optional.missing(URL.class));
             }
             stage = 2;
             break;
         case 2:
             if ("div".equals(qName) && "right".equals(attributes.getValue("class"))) {
                 stage = 991;
             }
             break;
         case 14:
             if ("a".equals(qName)) {
                 try {
                     assert story != null;
                     story.set(URL, new URL("http://www.fimfiction.net" + attributes.getValue("href")));
                 } catch (MalformedURLException e) {
                     throw new SAXException(e);
                 }
                 title = new StringBuilder();
                 stage = 15;
             }
             break;
         case 16:
             if ("a".equals(qName)) {
                 authorName = new StringBuilder();
                 stage = 17;
             }
             break;
         case 18:
             if ("img".equals(qName) &&
                 "//www.fimfiction-static.net/images/icons/views.png".equals(attributes.getValue("src"))) {
                 stage = 19;
             }
             break;
         case 20:
             if ("div".equals(qName) && "description".equals(attributes.getValue("class"))) {
                 stage = 21;
             }
             break;
         case 21:
             if ("story_image".equals(attributes.getValue("class"))) { stage = 22; } else {
                 assert story != null;
                 Optional<URL> optional = Optional.missing(URL.class);
                 story.set(URL_COVER, optional);
                 story.set(URL_THUMBNAIL, optional);
                 stage = 25;
             }
             break;
         case 22:
             try {
                 assert story != null;
                 story.set(URL_COVER, Optional.existing(new URL("http:" + attributes.getValue("href"))));
             } catch (MalformedURLException e) {
                 throw new SAXException(e);
             }
             stage = 23;
             break;
         case 23:
             try {
                 assert story != null;
                 story.set(URL_THUMBNAIL, Optional.existing(new URL("http:" + attributes.getValue("src"))));
             } catch (MalformedURLException e) {
                 throw new SAXException(e);
             }
             stage = 24;
             break;
         case 24:
             if ("a".equals(qName)) {
                 stage = 25;
             } else {
                 if (categories != null) {
                     assert story != null;
                     story.set(CATEGORIES, categories);
                     categories = null;
                 }
                 description = new FormattedStringParser.FormattedStringHandler();
                 stage = 26;
             }
             break;
         case 27:
             assert description != null;
             description.startElement(uri, localName, qName, attributes);
             break;
         case 28:
             if ("ul".equals(qName)) {
                 chapters = new ArrayList<Chapter>();
                 stage = 29;
             }
             break;
         case 29:
             if ("div".equals(qName)) {
                 String clazz = attributes.getValue("class");
                 if (clazz != null && clazz.contains("chapter_container")) {
                     if (!clazz.contains("chapter_expander")) {
                         stage = 100;
                     }
                 }
             }
             if ("li".equals(qName) && "save_ordering".equals(attributes.getValue("class"))) {
                 assert story != null;
                 assert chapters != null;
                 story.set(CHAPTERS, chapters);
                 story.set(CHAPTER_COUNT, chapters.size());
                 chapters = null;
                 stage = 38;
             }
             break;
         case 100:
             stage = 101;
             break;
         case 101:
             stage = 30;
             break;
         case 30:
             chapter = Chapter.createMutable();
             if ("i".equals(qName)) {
                 if (attributes.getValue("class") != null) {
                     chapter.set(Chapter.ChapterKey.UNREAD, attributes.getValue("class").length() == 17);
                     stage = 31;
                 }
             } else if ("a".equals(qName)) {
                 try {
                     chapter.set(Chapter.ChapterKey.URL,
                                 new URL("http://www.fimfiction.net" + attributes.getValue("href")));
                 } catch (MalformedURLException e) {
                     throw new SAXException(e);
                 }
                 chapterTitle = new StringBuilder();
                 stage = 32;
             }
             break;
         case 31:
             if ("a".equals(qName)) {
                 try {
                     assert chapter != null;
                     chapter.set(Chapter.ChapterKey.URL,
                                 new URL("http://www.fimfiction.net" + attributes.getValue("href")));
                 } catch (MalformedURLException e) {
                     throw new SAXException(e);
                 }
                 chapterTitle = new StringBuilder();
                 stage = 32;
             }
             break;
         case 35:
             stage = 36;
             break;
         case 37:
             if ("a".equals(qName)) {
                 assert chapter != null;
                 chapter.set(Chapter.ChapterKey.ID, toIntLenient(attributes.getValue("href")));
                 assert chapters != null;
                 chapters.add(chapter);
                 chapter = null;
                 stage = 29;
             }
             break;
         case 203:
             if ("div".equals(qName)) {
                 assert story != null;
                 if (!story.has(SEX)) { story.set(SEX, false); }
                 if (!story.has(GORE)) { story.set(GORE, false); }
                 stage = 204;
             }
             break;
         case 205:
             if ("br".equals(qName)) {
                 stage = 39;
             }
             break;
         case 39:
             if ("span".equals(qName)) {
                 stage = 40;
             }
             break;
         case 41:
             if ("br".equals(qName)) {
                 stage = 42;
             }
             if ("a".equals(qName)) {
                 stage = 43;
             }
             break;
         case 42:
             if ("span".equals(qName)) {
                 stage = 43;
             }
             break;
         case 43:
         case 44:
             if ("img".equals(qName)) {
                 src = attributes.getValue("src");
                 try {
                     assert characters != null;
                     characters.add(FimCharacter.DefaultCharacter.getOrCreateCharacter(characterId,
                                                                                       new URL("http:" + src)));
                 } catch (MalformedURLException e) {
                     throw new SAXException(e);
                 }
             } else if ("i".equals(qName)) {
                 assert story != null;
                 assert characters != null;
                 story.set(CHARACTERS, characters);
                 characters = null;
                 finishedStories.add(story);
                 story = null;
                 stage = 0;
             } else if ("a".equals(qName)) {
                 characterId = toIntLenient(attributes.getValue("href"));
             }
             break;
         }
     }
 
     @Override
     public void endElement(@Nonnull String uri, @Nonnull String localName, @Nonnull String qName) throws SAXException {
         if (stage == 0) {
             return;
         }
         switch (stage) {
         case 999:
             assert story != null;
             story.set(Story.StoryKey.TITLE, title.toString());
             stage = 16;
             break;
         case 770:
             if (qName.equals("ul")) {
                 assert story != null;
                 assert shelves != null;
                 assert shelvesNotAdded != null;
                 story.set(SHELVES_ADDED, shelves);
                 story.set(SHELVES_NOT_ADDED, shelvesNotAdded);
 
                 LegacySupport.deriveFavoriteAndReadLaterFromShelves(story);
 
                 stage = 996;
             }
             break;
         case 500:
             if ("ul".equals(qName)) {
                 stage = 0;
             }
             break;
         case 11:
             if ("div".equals(qName)) {
                 stage = 12;
             }
             break;
         case 15:
             assert story != null;
             assert title != null;
             story.set(TITLE, title.toString());
             stage = 16;
             break;
         case 17:
             assert authorName != null;
             assert author != null;
             author.set(User.UserKey.NAME, authorName.toString());
             assert story != null;
             story.set(AUTHOR, author);
             authorName = null;
             stage = 18;
             break;
         case 26:
             stage = 27;
             break;
         case 27:
             assert description != null;
             if ("div".equals(qName)) {
                 assert story != null;
                 story.set(DESCRIPTION, description.build());
                 description = null;
                 stage = 28;
             } else {
                 description.endElement(uri, localName, qName);
             }
             break;
         case 32:
             assert chapterTitle != null;
             assert chapter != null;
             chapter.set(Chapter.ChapterKey.TITLE, chapterTitle.toString());
             chapterTitle = null;
             stage = 33;
             break;
         case 33:
             if ("b".equals(qName)) {
                 stage = 34;
             }
             break;
         case 36:
             stage = 37;
             break;
         case 38:
             if ("a".equals(qName)) {
                 stage = 200;
             }
             break;
         case 200:
             if ("a".equals(qName)) {
                 stage = 201;
             }
             break;
         case 201:
             if ("a".equals(qName)) {
                 stage = 202;
             }
             break;
         case 202:
             if ("a".equals(qName)) {
                 stage = 203;
             }
             break;
         }
     }
 
     @Override
     public void characters(@Nonnull char[] ch, int start, int length) throws SAXException {
         if (stage == 0) {
             if (loggedIn == null) {
                 String asString = new String(ch, start, length).trim();
                 if (asString.contains("var static_url = \"//www.fimfiction-static.net\";")) {
                     int i = asString.indexOf("logged_in_user.id = ");
                     if (i == -1) {
                         loggedIn = Optional.missing(User.class);
                     } else {
                         String sub1 = asString.substring(i + 20);
                         String sub2 = sub1.substring(0, sub1.indexOf(';'));
                         loggedIn = Optional.existing(User.createMutable().set(User.UserKey.ID, Integer.parseInt(sub2)));
                     }
                 }
             }
             return;
         }
         String asString = new String(ch, start, clipWhitespace(ch, start, length));
         if (asString.isEmpty() && stage != 27) {
             return;
         }
         switch (stage) {
         case 993:
             assert story != null;
             story.set(Story.StoryKey.LIKE_COUNT, toIntLenient(asString));
             stage = 994;
             break;
         case 995:
             assert story != null;
             story.set(Story.StoryKey.DISLIKE_COUNT, toIntLenient(asString));
             stage = 880;
             break;
         case 999:
             title.append(asString);
             break;
         case 17:
             assert authorName != null;
             authorName.append(asString);
             break;
         case 19:
             assert story != null;
             story.set(VIEW_COUNT_MAXIMUM_CHAPTER, toIntLenient(asString.substring(0, asString.indexOf('('))));
             story.set(VIEW_COUNT_TOTAL, toIntLenient(asString.substring(asString.indexOf('('))));
             stage = 20;
             break;
         case 25:
             if (categories == null) {
                 categories = EnumSet.noneOf(Category.class);
             }
             for (Category category : Category.values()) {
                 if (getDisplayName(category).equals(asString)) {
                     categories.add(category);
                     break;
                 }
             }
             stage = 24;
             break;
         case 27:
             assert description != null;
             description.characters(ch, start, length);
             break;
         case 32:
             assert chapterTitle != null;
             chapterTitle.append(asString);
             break;
         case 34:
             assert story != null;
             story.set(DATE_UPDATED, parseDate(asString));
             stage = 35;
             break;
         case 36:
             int c = toIntLenient(asString, -1);
             if (c >= -1) {
                 assert chapter != null;
                 chapter.set(Chapter.ChapterKey.WORD_COUNT, c);
             }
             break;
         case 203:
             assert story != null;
             asString = asString.trim();
             if ("Complete".equals(asString)) {
                 story.set(STATUS, StoryStatus.COMPLETED);
             } else if ("Incomplete".equals(asString)) {
                 story.set(STATUS, StoryStatus.INCOMPLETE);
             } else if ("Cancelled".equals(asString)) {
                 story.set(STATUS, StoryStatus.CANCELLED);
             } else if ("On Hiatus".equals(asString)) {
                 story.set(STATUS, StoryStatus.ON_HIATUS);
             } else if ("Everyone".equals(asString)) {
                 story.set(CONTENT_RATING, ContentRating.EVERYONE);
             } else if ("Teen".equals(asString)) {
                 story.set(CONTENT_RATING, ContentRating.TEEN);
             } else if ("Mature".equals(asString)) {
                 story.set(CONTENT_RATING, ContentRating.MATURE);
             } else if ("Sex".equals(asString)) {
                 story.set(SEX, true);
             } else if ("Gore".equals(asString)) {
                 story.set(GORE, true);
             }
             break;
         case 204:
             int words = toIntLenient(asString, -1);
             if (words >= 0) {
                 assert story != null;
                 story.set(WORD_COUNT, words);
                 stage = 205;
             }
             break;
         case 40:
             assert story != null;
             story.set(DATE_FIRST_POSTED, parseDate(asString));
             characters = new HashSet<FimCharacter>();
             stage = 41;
             break;
         case 43:
             assert story != null;
             story.set(DATE_UPDATED, parseDate(asString));
             stage = 44;
             break;
         }
     }
 
     @Nonnull
     private Date parseDate(@Nonnull String date) throws SAXException {
         try {
             return fimfictionDateFormat.parse(date.replaceAll("(st|nd|rd|th)", ""));
         } catch (ParseException e) {
             throw new SAXException(e);
         }
     }
 
     private static int toIntLenient(String toInt) {
         return toIntLenient(toInt, 0);
     }
 
     private static int toIntLenient(@Nullable String toInt, int defaultValue) {
         if (toInt != null) {
             int result = 0;
             boolean modified = false;
             for (int i = 0, l = toInt.length(); i < l; i++) {
                 char c = toInt.charAt(i);
                 int value = c - '0';
                 if (value >= 0 && value < 10) {
                     result *= 10;
                     result += value;
                     modified = true;
                 }
             }
             if (modified) {
                 return result;
             }
         }
         return defaultValue;
     }
 
     @Nonnull
     private static String getDisplayName(@Nonnull Category category) {
         switch (category) {
         case ADVENTURE:
             return "Adventure";
         case ALTERNATE_UNIVERSE:
             return "Alternate Universe";
         case ANTHRO:
             return "Anthro";
         case COMEDY:
             return "Comedy";
         case CROSSOVER:
             return "Crossover";
         case DARK:
             return "Dark";
         case HUMAN:
             return "Human";
         case RANDOM:
             return "Random";
         case ROMANCE:
             return "Romance";
         case SAD:
             return "Sad";
         case SLICE_OF_LIFE:
             return "Slice of Life";
         case TRAGEDY:
             return "Tragedy";
         default:
             throw new IllegalArgumentException(category.name());
         }
     }
 
     private static int clipWhitespace(char[] ch, int start, int length) {
         boolean white = false;
         int i = start;
         int j = i;
         for (; i < start + length; i++) {
             if (isWhitespace(ch[i])) {
                 if (!white) {
                     ch[j++] = ' ';
                     white = true;
                 }
             } else {
                 ch[j++] = ch[i];
                 white = false;
             }
         }
         return j;
     }
 
     static boolean isWhitespace(char c) { return c == ' ' || c == '\n' || c == '\r' || c == '\t'; }
 }
