 // ResumptionToken
 // (C) 2009 by Michael Peter Christen; mc@yacy.net, Frankfurt a. M., Germany
 // first published 31.10.2009 on http://yacy.net
 //
 // $LastChangedDate: 2006-04-02 22:40:07 +0200 (So, 02 Apr 2006) $
 // $LastChangedRevision: 1986 $
 // $LastChangedBy: orbiter $
 //
 // LICENSE
 // 
 // This program is free software; you can redistribute it and/or modify
 // it under the terms of the GNU General Public License as published by
 // the Free Software Foundation; either version 2 of the License, or
 // (at your option) any later version.
 //
 // This program is distributed in the hope that it will be useful,
 // but WITHOUT ANY WARRANTY; without even the implied warranty of
 // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 // GNU General Public License for more details.
 //
 // You should have received a copy of the GNU General Public License
 // along with this program; if not, write to the Free Software
 // Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 
 
 package net.yacy.document.importer;
 
 import java.io.IOException;
 import java.io.InputStream;
 import java.text.Collator;
 import java.text.ParseException;
 import java.util.Date;
 import java.util.Locale;
 import java.util.TreeMap;
 
 import javax.xml.parsers.ParserConfigurationException;
 import javax.xml.parsers.SAXParser;
 import javax.xml.parsers.SAXParserFactory;
 
 import org.xml.sax.Attributes;
 import org.xml.sax.SAXException;
 import org.xml.sax.helpers.DefaultHandler;
 
 import net.yacy.kelondro.data.meta.DigestURI;
 import net.yacy.kelondro.util.DateFormatter;
 
 public class ResumptionToken  extends TreeMap<String, String> {
     
     private static final long serialVersionUID = -8389462290545629792L;
 
     // use a collator to relax when distinguishing between lowercase und uppercase letters
     private static final Collator insensitiveCollator = Collator.getInstance(Locale.US);
     static {
         insensitiveCollator.setStrength(Collator.SECONDARY);
         insensitiveCollator.setDecomposition(Collator.NO_DECOMPOSITION);
     }
     
     int recordCounter;
     
     public ResumptionToken(final InputStream stream) throws IOException {
         super((Collator) insensitiveCollator.clone());
         this.recordCounter = 0;
         new Reader(stream);
     }
     
     public ResumptionToken(
             Date expirationDate,
             int completeListSize,
             int cursor,
             String token
             ) {
         super((Collator) insensitiveCollator.clone());
         this.recordCounter = 0;
         this.put("expirationDate", DateFormatter.formatISO8601(expirationDate));
         this.put("completeListSize", Integer.toString(completeListSize));
         this.put("cursor", Integer.toString(cursor));
         this.put("token", token);
     }
     
     public ResumptionToken(
             String expirationDate,
             int completeListSize,
             int cursor,
             String token
             ) {
         super((Collator) insensitiveCollator.clone());
         this.recordCounter = 0;
         this.put("expirationDate", expirationDate);
         this.put("completeListSize", Integer.toString(completeListSize));
         this.put("cursor", Integer.toString(cursor));
         this.put("token", token);
     }
     
     /**
      * truncate the given url at the '?'
      * @param url
      * @return a string containing the url up to and including the '?'
      */
     public static String truncatedURL(DigestURI url) {
         String u = url.toNormalform(true, true);
         int i = u.indexOf('?');
         if (i > 0) u = u.substring(0, i + 1);
         return u;
     }
     
     /**
      * while parsing the resumption token, also all records are counted
      * @return the result from counting the records
      */
     public int getRecordCounter() {
         return this.recordCounter;
     }
     
     /**
      * compute a url that can be used to resume the retrieval from the OAI-PMH resource
      * @param givenURL
      * @return
      * @throws IOException in case that no follow-up url can be generated; i.e. if the expiration date is exceeded
      */
     public DigestURI resumptionURL(DigestURI givenURL) throws IOException {
         // decide which kind of encoding stratgy was used to get a resumptionToken:
 
         String token = this.getToken();
         if (token == null || token.length() == 0) throw new IOException("end of resumption reached");
         String url = truncatedURL(givenURL);
         
         // encoded state
         if (token.indexOf("from=") >= 0) {
             return new DigestURI(url + "verb=ListRecords&" + token, null);
         }
         
         // cached result set
         // can be detected with given expiration date
         Date expiration = getExpirationDate();
         if (expiration != null) {
             if (expiration.before(new Date())) throw new IOException("the resumption is expired at " + DateFormatter.formatISO8601(expiration) + " (now: " + DateFormatter.formatISO8601(new Date()));
             // the resumption token is still fresh
         }

        return new DigestURI(url + "verb=ListRecords&resumptionToken=" + token, null);
     }
     
     /**
      * an expiration date of a resumption token that addresses how long a cached set will
      * stay in the cache of the oai-pmh server. See:
      * http://www.openarchives.org/OAI/2.0/guidelines-repository.htm#CachedResultSet
      * @return
      */
     public Date getExpirationDate() {
         String d = this.get("expirationDate");
         if (d == null) return null;
         try {
             return DateFormatter.parseISO8601(d);
         } catch (ParseException e) {
             e.printStackTrace();
             return new Date();
         }
     }
     
     /**
      * The completeListSize attribute provides a place where the estimated number of results
      * in the complete list response may be announced. This is likely to be used for
      * status monitoring by harvesting software and implementation is recommended especially in
      * repositories with large numbers of records. The value of completeListSize can be reliably
      * accurate only in the case of a system where the result set is cached.
      * In other cases, it is permissible for repositories to revise
      * the estimate during a list request sequence.
      * An attribute according to
      * http://www.openarchives.org/OAI/2.0/guidelines-repository.htm#completeListSize
      * @return
      */
     public int getCompleteListSize() {
         String t = this.get("completeListSize");
         if (t == null) return 0;
         return Integer.parseInt(t);
     }
     
     /**
      * The cursor attribute is the number of results returned so far in the complete list response,
      * thus it is always "0" in the first incomplete list response.
      * It should only be specified if it is consistently used in all responses.
      * An attribute according to
      * http://www.openarchives.org/OAI/2.0/guidelines-repository.htm#completeListSize
      * @return
      */
     public int getCursor() {
         String t = this.get("cursor");
         if (t == null) return 0;
         return Integer.parseInt(t);
     }
     
     /**
      * get a token of the stateless transfer in case that no expiration date is given
      * see:
      * http://www.openarchives.org/OAI/2.0/guidelines-repository.htm#StateInResumptionToken
      * @return
      */
     public String getToken() {
         return this.get("token");
     }
     
     public String toString() {
         return "expirationDate=" + DateFormatter.formatISO8601(this.getExpirationDate()) + ", completeListSize=" + getCompleteListSize() +
         ", cursor=" + this.getCursor() + ", token=" + this.getToken();
     }
     
     // get a resumption token using a SAX xml parser from am input stream
     private class Reader extends DefaultHandler {
 
         // class variables
         private final StringBuilder buffer;
         private boolean parsingValue;
         private SAXParser saxParser;
         private InputStream stream;
         private Attributes atts;
 
         public Reader(final InputStream stream) throws IOException {
             this.buffer = new StringBuilder();
             this.parsingValue = false;
             this.stream = stream;
             this.atts = null;
             final SAXParserFactory factory = SAXParserFactory.newInstance();
             try {
                 this.saxParser = factory.newSAXParser();
                 this.saxParser.parse(this.stream, this);
             } catch (SAXException e) {
                 e.printStackTrace();
             } catch (IOException e) {
                 e.printStackTrace();
             } catch (ParserConfigurationException e) {
                 e.printStackTrace();
                 throw new IOException(e.getMessage());
             } finally {
                 try {
                     this.stream.close();
                 } catch (IOException e) {
                     e.printStackTrace();
                 }
             }
         }
         
         /*
          <resumptionToken expirationDate="2009-10-31T22:52:14Z"
          completeListSize="226"
          cursor="0">688</resumptionToken>
          */
         
         public void startElement(final String uri, final String name, final String tag, final Attributes atts) throws SAXException {
             if ("record".equals(tag)) {
                 recordCounter++;
             }
             if ("resumptionToken".equals(tag)) {
                 this.parsingValue = true;
                 this.atts = atts;
             }
         }
 
         public void endElement(final String uri, final String name, final String tag) {
             if (tag == null) return;
             if ("resumptionToken".equals(tag)) {
                 put("expirationDate", atts.getValue("expirationDate"));
                 put("completeListSize", atts.getValue("completeListSize"));
                 put("cursor", atts.getValue("cursor"));
                 put("token", buffer.toString());
                 this.buffer.setLength(0);
                 this.parsingValue = false;
             }
         }
 
         public void characters(final char ch[], final int start, final int length) {
             if (parsingValue) {
                 buffer.append(ch, start, length);
             }
         }
 
     }
 
 }
