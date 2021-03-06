 // htmlFilterAbstractScraper.java 
 // ---------------------------
 // (C) by Michael Peter Christen; mc@anomic.de
 // first published on http://www.anomic.de
 // Frankfurt, Germany, 2004
 // last major change: 18.02.2004
 //
 // You agree that the Author(s) is (are) not responsible for cost,
 // loss of data or any harm that may be caused by usage of this softare or
 // this documentation. The usage of this software is on your own risk. The
 // installation and usage (starting/running) of this software may allow other
 // people or application to access your computer and any attached devices and
 // is highly dependent on the configuration of the software which must be
 // done by the user of the software;the author(s) is (are) also
 // not responsible for proper configuration and usage of the software, even
 // if provoked by documentation provided together with the software.
 //
 // THE SOFTWARE THAT FOLLOWS AS ART OF PROGRAMMING BELOW THIS SECTION
 // IS PUBLISHED UNDER THE GPL AS DOCUMENTED IN THE FILE gpl.txt ASIDE THIS
 // FILE AND AS IN http://www.gnu.org/licenses/gpl.txt
 // ANY CHANGES TO THIS FILE ACCORDING TO THE GPL CAN BE DONE TO THE
 // LINES THAT FOLLOWS THIS COPYRIGHT NOTICE HERE, BUT CHANGES MUST NOT
 // BE DONE ABOVE OR INSIDE THE COPYRIGHT NOTICE. A RE-DISTRIBUTION
 // MUST CONTAIN THE INTACT AND UNCHANGED COPYRIGHT NOTICE.
 // CONTRIBUTIONS AND CHANGES TO THE PROGRAM CODE SHOULD BE MARKED AS SUCH.
 
 package de.anomic.htmlFilter;
 
 import java.util.HashMap;
 import java.util.Properties;
 import java.util.TreeSet;
 
 import de.anomic.server.serverCharBuffer;
 
 public abstract class htmlFilterAbstractScraper implements htmlFilterScraper {
 
     public static final char lb = '<';
     public static final char rb = '>';
     public static final char sl = '/';
  
     private TreeSet      tags0;
     private TreeSet      tags1;
 
     // define a translation table for html character codings
     private static HashMap trans = new HashMap(300);
     static {
         trans.put("&quot;", "\""); //Anf&uuml;hrungszeichen oben
         trans.put("&amp;", "&"); //Ampersand-Zeichen, kaufm&auml;nnisches Und
         trans.put("&lt;", "<"); //&ouml;ffnende spitze Klammer
         trans.put("&gt;", ">"); //schlie&szlig;ende spitze Klammer
         trans.put("&nbsp;", " "); //Erzwungenes Leerzeichen
         trans.put("&iexcl;", "!"); //umgekehrtes Ausrufezeichen
         trans.put("&cent;", " cent "); //Cent-Zeichen
         trans.put("&pound;", " pound "); //Pfund-Zeichen
         trans.put("&curren;", " currency "); //W&auml;hrungs-Zeichen
         trans.put("&yen;", " yen "); //Yen-Zeichen
         trans.put("&brvbar;", " "); //durchbrochener Strich
         trans.put("&sect;", " paragraph "); //Paragraph-Zeichen
         trans.put("&uml;", " "); //P&uuml;nktchen oben
         trans.put("&copy;", " copyright "); //Copyright-Zeichen
         trans.put("&ordf;", " "); //Ordinal-Zeichen weiblich
         trans.put("&laquo;", " "); //angewinkelte Anf&uuml;hrungszeichen links
         trans.put("&not;", " not "); //Verneinungs-Zeichen
         trans.put("&shy;", "-"); //kurzer Trennstrich
         trans.put("&reg;", " trademark "); //Registriermarke-Zeichen
         trans.put("&macr;", " "); //&Uuml;berstrich
         trans.put("&deg;", " degree "); //Grad-Zeichen
         trans.put("&plusmn;", " +/- "); //Plusminus-Zeichen
         trans.put("&sup2;", " square "); //Hoch-2-Zeichen
         trans.put("&sup3;", " 3 "); //Hoch-3-Zeichen
         trans.put("&acute;", " "); //Acute-Zeichen
         trans.put("&micro;", " micro "); //Mikro-Zeichen
         trans.put("&para;", " paragraph "); //Absatz-Zeichen
         trans.put("&middot;", " "); //Mittelpunkt
         trans.put("&cedil;", " "); //H&auml;kchen unten
         trans.put("&sup1;", " "); //Hoch-1-Zeichen
         trans.put("&ordm;", " degree "); //Ordinal-Zeichen m&auml;nnlich
         trans.put("&raquo;", " "); //angewinkelte Anf&uuml;hrungszeichen rechts
         trans.put("&frac14;", " quarter "); //ein Viertel
         trans.put("&frac12;", " half "); //ein Halb
         trans.put("&frac34;", " 3/4 "); //drei Viertel
         trans.put("&iquest;", "?"); //umgekehrtes Fragezeichen
         trans.put("&Agrave;", "A"); //A mit Accent grave
         trans.put("&Aacute;", "A"); //A mit Accent acute
         trans.put("&Acirc;", "A"); //A mit Circumflex
         trans.put("&Atilde;", "A"); //A mit Tilde
         trans.put("&Auml;", "Ae"); //A Umlaut
         trans.put("&Aring;", "A"); //A mit Ring
         trans.put("&AElig;", "A"); //A mit legiertem E
         trans.put("&Ccedil;", "C"); //C mit H&auml;kchen
         trans.put("&Egrave;", "E"); //E mit Accent grave
         trans.put("&Eacute;", "E"); //E mit Accent acute
         trans.put("&Ecirc;", "E"); //E mit Circumflex
         trans.put("&Euml;", "E"); //E Umlaut
         trans.put("&Igrave;", "I"); //I mit Accent grave
         trans.put("&Iacute;", "I"); //I mit Accent acute
         trans.put("&Icirc;", "I"); //I mit Circumflex
         trans.put("&Iuml;", "I"); //I Umlaut
         trans.put("&ETH;", "D"); //Eth (isl&auml;ndisch)
         trans.put("&Ntilde;", "N"); //N mit Tilde
         trans.put("&Ograve;", "O"); //O mit Accent grave
         trans.put("&Oacute;", "O"); //O mit Accent acute
         trans.put("&Ocirc;", "O"); //O mit Circumflex
         trans.put("&Otilde;", "O"); //O mit Tilde
         trans.put("&Ouml;", "Oe"); //O Umlaut
         trans.put("&times;", " times "); //Mal-Zeichen
         trans.put("&Oslash;", "O"); //O mit Schr&auml;gstrich
         trans.put("&Ugrave;", "U"); //U mit Accent grave
         trans.put("&Uacute;", "U"); //U mit Accent acute
         trans.put("&Ucirc;", "U"); //U mit Circumflex
         trans.put("&Uuml;", "Ue"); //U Umlaut
         trans.put("&Yacute;", "Y"); //Y mit Accent acute
         trans.put("&THORN;", "P"); //THORN (isl&auml;ndisch)
         trans.put("&szlig;", "ss"); //scharfes S
         trans.put("&agrave;", "a"); //a mit Accent grave
         trans.put("&aacute;", "a"); //a mit Accent acute
         trans.put("&acirc;", "a"); //a mit Circumflex
         trans.put("&atilde;", "a"); //a mit Tilde
         trans.put("&auml;", "ae"); //a Umlaut
         trans.put("&aring;", "a"); //a mit Ring
         trans.put("&aelig;", "a"); //a mit legiertem e
         trans.put("&ccedil;", "c"); //c mit H&auml;kchen
         trans.put("&egrave;", "e"); //e mit Accent grave
         trans.put("&eacute;", "e"); //e mit Accent acute
         trans.put("&ecirc;", "e"); //e mit Circumflex
         trans.put("&euml;", "e"); //e Umlaut
         trans.put("&igrave;", "i"); //i mit Accent grave
         trans.put("&iacute;", "i"); //i mit Accent acute
         trans.put("&icirc;", "i"); //i mit Circumflex
         trans.put("&iuml;", "i"); //i Umlaut
         trans.put("&eth;", "d"); //eth (isl&auml;ndisch)
         trans.put("&ntilde;", "n"); //n mit Tilde
         trans.put("&ograve;", "o"); //o mit Accent grave
         trans.put("&oacute;", "o"); //o mit Accent acute
         trans.put("&ocirc;", "o"); //o mit Circumflex
         trans.put("&otilde;", "o"); //o mit Tilde
         trans.put("&ouml;", "oe"); //o Umlaut
         trans.put("&divide;", "%"); //Divisions-Zeichen
         trans.put("&oslash;", "o"); //o mit Schr&auml;gstrich
         trans.put("&ugrave;", "u"); //u mit Accent grave
         trans.put("&uacute;", "u"); //u mit Accent acute
         trans.put("&ucirc;", "u"); //u mit Circumflex
         trans.put("&uuml;", "ue"); //u Umlaut
         trans.put("&yacute;", "y"); //y mit Accent acute
         trans.put("&thorn;", "p"); //thorn (isl&auml;ndisch)
         trans.put("&yuml;", "y"); //y Umlaut
         trans.put("&Alpha;", " Alpha "); //Alpha gro&szlig;
         trans.put("&alpha;", " alpha "); //alpha klein
         trans.put("&Beta;", " Beta "); //Beta gro&szlig;
         trans.put("&beta;", " beta "); //beta klein
         trans.put("&Gamma;", " Gamma "); //Gamma gro&szlig;
         trans.put("&gamma;", " gamma "); //gamma klein
         trans.put("&Delta;", " Delta "); //Delta gro&szlig;
         trans.put("&delta;", " delta "); //delta klein
         trans.put("&Epsilon;", " Epsilon "); //Epsilon gro&szlig;
         trans.put("&epsilon;", " epsilon "); //epsilon klein
         trans.put("&Zeta;", " Zeta "); //Zeta gro&szlig;
         trans.put("&zeta;", " zeta "); //zeta klein
         trans.put("&Eta;", " Eta "); //Eta gro&szlig;
         trans.put("&eta;", " eta "); //eta klein
         trans.put("&Theta;", " Theta "); //Theta gro&szlig;
         trans.put("&theta;", " theta "); //theta klein
         trans.put("&Iota;", " Iota "); //Iota gro&szlig;
         trans.put("&iota;", " iota "); //iota klein
         trans.put("&Kappa;", " Kappa "); //Kappa gro&szlig;
         trans.put("&kappa;", " kappa "); //kappa klein
         trans.put("&Lambda;", " Lambda "); //Lambda gro&szlig;
         trans.put("&lambda;", " lambda "); //lambda klein
         trans.put("&Mu;", " Mu "); //Mu gro&szlig;
         trans.put("&mu;", " mu "); //mu klein
         trans.put("&Nu;", " Nu "); //Nu gro&szlig;
         trans.put("&nu;", " nu "); //nu klein
         trans.put("&Xi;", " Xi "); //Xi gro&szlig;
         trans.put("&xi;", " xi "); //xi klein
         trans.put("&Omicron;", " Omicron "); //Omicron gro&szlig;
         trans.put("&omicron;", " omicron "); //omicron klein
         trans.put("&Pi;", " Pi "); //Pi gro&szlig;
         trans.put("&pi;", " pi "); //pi klein
         trans.put("&Rho;", " Rho "); //Rho gro&szlig;
         trans.put("&rho;", " rho "); //rho klein
         trans.put("&Sigma;", " Sigma "); //Sigma gro&szlig;
         trans.put("&sigmaf;", " sigma "); //sigmaf klein
         trans.put("&sigma;", " sigma "); //sigma klein
         trans.put("&Tau;", " Tau "); //Tau gro&szlig;
         trans.put("&tau;", " tau "); //tau klein
         trans.put("&Upsilon;", " Ypsilon "); //Upsilon gro&szlig;
         trans.put("&upsilon;", " ypsilon "); //upsilon klein
         trans.put("&Phi;", " Phi "); //Phi gro&szlig;
         trans.put("&phi;", " phi "); //phi klein
         trans.put("&Chi;", " Chi "); //Chi gro&szlig;
         trans.put("&chi;", " chi "); //chi klein
         trans.put("&Psi;", " Psi "); //Psi gro&szlig;
         trans.put("&psi;", " psi "); //psi klein
         trans.put("&Omega;", " Omega "); //Omega gro&szlig;
         trans.put("&omega;", " omega "); //omega klein
         trans.put("&thetasym;", " theta "); //theta Symbol
         trans.put("&upsih;", " ypsilon "); //upsilon mit Haken
         trans.put("&piv;", " pi "); //pi Symbol
         trans.put("&forall;", " for all "); //f&uuml;r alle
         trans.put("&part;", " part of "); //teilweise
         trans.put("&exist;", " exists "); //existiert
         trans.put("&empty;", " null "); //leer
         trans.put("&nabla;", " nabla "); //nabla
         trans.put("&isin;", " element of "); //Element von
         trans.put("&notin;", " not element of "); //kein Element von
         trans.put("&ni;", " contains "); //enth&auml;lt als Element
         trans.put("&prod;", " product "); //Produkt
         trans.put("&sum;", " sum "); //Summe
         trans.put("&minus;", " minus "); //minus
         trans.put("&lowast;", " times "); //Asterisk
         trans.put("&radic;", " sqare root "); //Quadratwurzel
         trans.put("&prop;", " proportional to "); //proportional zu
         trans.put("&infin;", " unlimited "); //unendlich
         trans.put("&ang;", " angle "); //Winkel
         trans.put("&and;", " and "); //und
         trans.put("&or;", " or "); //oder
         trans.put("&cap;", " "); //Schnittpunkt
         trans.put("&cup;", " unity "); //Einheit
         trans.put("&int;", " integral "); //Integral
         trans.put("&there4;", " cause "); //deshalb
         trans.put("&sim;", " similar to "); //&auml;hnlich wie
         trans.put("&cong;", " equal "); //ann&auml;hernd gleich
         trans.put("&asymp;", " equal "); //beinahe gleich
         trans.put("&ne;", " not equal "); //ungleich
         trans.put("&equiv;", " identical "); //identisch mit
         trans.put("&le;", " smaller or equal than "); //kleiner gleich
         trans.put("&ge;", " greater or equal than "); //gr&ouml;&szlig;er gleich
         trans.put("&sub;", " subset of "); //Untermenge von
         trans.put("&sup;", " superset of "); //Obermenge von
         trans.put("&nsub;", " not subset of "); //keine Untermenge von
         trans.put("&sube;", ""); //Untermenge von oder gleich mit
         trans.put("&supe;", ""); //Obermenge von oder gleich mit
         trans.put("&oplus;", ""); //Direktsumme
         trans.put("&otimes;", ""); //Vektorprodukt
         trans.put("&perp;", ""); //senkrecht zu
         trans.put("&sdot;", ""); //Punkt-Operator
         trans.put("&loz;", ""); //Raute
         trans.put("&lceil;", ""); //links oben
         trans.put("&rceil;", ""); //rechts oben
         trans.put("&lfloor;", ""); //links unten
         trans.put("&rfloor;", ""); //rechts unten
         trans.put("&lang;", ""); //spitze Klammer links
         trans.put("&rang;", ""); //spitze Klammer rechts
         trans.put("&larr;", ""); //Pfeil links
         trans.put("&uarr;", ""); //Pfeil oben
         trans.put("&rarr;", ""); //Pfeil rechts
         trans.put("&darr;", ""); //Pfeil unten
         trans.put("&harr;", ""); //Pfeil links/rechts
         trans.put("&crarr;", ""); //Pfeil unten-Knick-links
         trans.put("&lArr;", ""); //Doppelpfeil links
         trans.put("&uArr;", ""); //Doppelpfeil oben
         trans.put("&rArr;", ""); //Doppelpfeil rechts
         trans.put("&dArr;", ""); //Doppelpfeil unten
         trans.put("&hArr;", ""); //Doppelpfeil links/rechts
         trans.put("&bull;", ""); //Bullet-Zeichen
         trans.put("&hellip;", ""); //Horizontale Ellipse
         trans.put("&prime;", ""); //Minutenzeichen
         trans.put("&oline;", ""); //&Uuml;berstrich
         trans.put("&frasl;", ""); //Bruchstrich
         trans.put("&weierp;", ""); //Weierstrass p
         trans.put("&image;", ""); //Zeichen f&uuml;r &quot;imagin&auml;r&quot;
         trans.put("&real;", ""); //Zeichen f&uuml;r &quot;real&quot;
         trans.put("&trade;", ""); //Trademark-Zeichen
         trans.put("&euro;", ""); //Euro-Zeichen
         trans.put("&alefsym;", ""); //Alef-Symbol
         trans.put("&spades;", ""); //Pik-Zeichen
         trans.put("&clubs;", ""); //Kreuz-Zeichen
         trans.put("&hearts;", ""); //Herz-Zeichen
         trans.put("&diams;", ""); //Karo-Zeichen
         trans.put("&ensp;", ""); //Leerzeichen Breite n
         trans.put("&emsp;", ""); //Leerzeichen Breite m
         trans.put("&thinsp;", ""); //Schmales Leerzeichen
         trans.put("&zwnj;", ""); //null breiter Nichtverbinder
         trans.put("&zwj;", ""); //null breiter Verbinder
         trans.put("&lrm;", ""); //links-nach-rechts-Zeichen
         trans.put("&rlm;", ""); //rechts-nach-links-Zeichen
         trans.put("&ndash;", ""); //Gedankenstrich Breite n
         trans.put("&mdash;", ""); //Gedankenstrich Breite m
         trans.put("&lsquo;", ""); //einfaches Anf&uuml;hrungszeichen links
         trans.put("&rsquo;", ""); //einfaches Anf&uuml;hrungszeichen rechts
         trans.put("&sbquo;", ""); //einfaches low-9-Zeichen
         trans.put("&ldquo;", ""); //doppeltes Anf&uuml;hrungszeichen links
         trans.put("&rdquo;", ""); //doppeltes Anf&uuml;hrungszeichen rechts
         trans.put("&bdquo;", ""); //doppeltes low-9-Zeichen rechts
         trans.put("&dagger;", ""); //Kreuz
         trans.put("&Dagger;", ""); //Doppelkreuz
         trans.put("&permil;", ""); //zu tausend
         trans.put("&lsaquo;", ""); //angewinkeltes einzelnes Anf.zeichen links
         trans.put("&rsaquo;", ""); //angewinkeltes einzelnes Anf.zeichen rechts
     }
 
 
     public htmlFilterAbstractScraper(TreeSet tags0, TreeSet tags1) {
         this.tags0  = tags0;
         this.tags1  = tags1;
     }
 
     public boolean isTag0(String tag) {
         return (tags0 != null) && (tags0.contains(tag));
     }
 
     public boolean isTag1(String tag) {
         return (tags1 != null) && (tags1.contains(tag));
     }
 
     //the 'missing' method that shall be implemented:
    public abstract void scrapeText(char[] text, String insideTag);
 
     // the other methods must take into account to construct the return value correctly
     public abstract void scrapeTag0(String tagname, Properties tagopts);
 
     public abstract void scrapeTag1(String tagname, Properties tagopts, char[] text);
 
     // string conversions
     private static String code_iso8859s(char c) {
         switch (c) {
         
         // german umlaute and ligaturen
         case 0xc4: return "AE"; case 0xd6: return "OE"; case 0xdc: return "UE";
         case 0xe4: return "ae"; case 0xf6: return "oe"; case 0xfc: return "ue";
         case 0xdf: return "ss";
         
         // accent on letters; i.e. french characters
         case 0xc0: case 0xc1: case 0xc2: case 0xc3: case 0xc5: return  "A";
         case 0xc6: return  "AE";
         case 0xc7: return  "C";
         case 0xc8: case 0xc9: case 0xca: return  "E";
         case 0xcc: case 0xcd: case 0xce: case 0xcf: return  "I";
         case 0xd0: return  "D";
         case 0xd1: return  "N";
         case 0xd2: case 0xd3: case 0xd4: case 0xd5: case 0xd8: return  "O";
         case 0xd7: return  "x";
         case 0xd9: case 0xda: case 0xdb: return  "U";
         case 0xdd: return  "Y";
         case 0xde: return  "p";
         
         case 0xe0: case 0xe1: case 0xe2: case 0xe3: case 0xe5: return  "a";
         case 0xe6: return  "ae";
         case 0xe7: return  "c";
         case 0xe8: case 0xe9: case 0xea: return  "e";
         case 0xec: case 0xed: case 0xee: case 0xef: return  "i";
         case 0xf0: return  "d";
         case 0xf1: return  "n";
         case 0xf2: case 0xf3: case 0xf4: case 0xf5: case 0xf8: return  "o";
         case 0xf7: return  "%";
         case 0xf9: case 0xfa: case 0xfb: return  "u";
         case 0xfd: case 0xff: return  "y";
         case 0xfe: return  "p";
         
         // special characters
         case 0xa4: return " euro ";
         default: return null;
         }
     }
 
     public static serverCharBuffer convertUmlaute(serverCharBuffer bb) {
         if (bb.length() == 0) return bb;
 
             serverCharBuffer t = new serverCharBuffer(bb.length() + 20);
             char c;
             for  (int i = 0; i < bb.length(); i++) {
                 c = bb.charAt(i);
                 String z = code_iso8859s(c);
                 if (z == null) t.append((int)c); 
                 else t.append(z);
             }
             return t;
 
         
 //        serverByteBuffer t = new serverByteBuffer(bb.length() + 20);
 //        int b0, b1, b2;
 //        String z;
 //        int i = 0;
 //        while (i < bb.length()) {
 //            b0 = bb.byteAt(i) & 0xff;
 //            // check utf-8 encoding
 //            if ((b0 < 128) || (i + 1 == bb.length())) {
 //                t.append(b0);
 //                i++;
 //            } else {
 //                b1 = bb.byteAt(i + 1) & 0xff;
 //                if (b1 > 0x3f) {
 //                    z = code_iso8859s(b0);
 //                    i++;
 //                } else if ((b0 > 0xbf) && (b0 < 0xe0)) {
 //                    z = code_iso8859s(((b0 & 0x1f) << 0x6) | (b1 & 0x3f));
 //                    i += 2;
 //                } else {
 //                    if (i + 2 >= bb.length()) {
 //                        z = null;
 //                        i++;
 //                    } else {
 //                        b2 = bb.byteAt(i + 2) & 0xff;
 //                        if (b2 > 0x3f) {
 //                            z = code_iso8859s(b0);
 //                            i++;
 //                        } else {
 //                            z = code_iso8859s(((b0 & 0xf) << 0xc) | ((b1 & 0x3f) << 0x6) | (b2 & 0x3f));
 //                            i += 3;
 //                        }
 //                    }
 //                }
 //                if (z == null) t.append(b0); else t.append(z);
 //            }
 //        }
 //        return t;
     }
 
     private static char[] transscript(char[] code) {
         String t = (String) trans.get(new String(code)); 
         if (t == null) return new char[0];
         return t.toCharArray();
     }
 
     protected static serverCharBuffer transscriptAll(serverCharBuffer bb) {
         int p0 = 0, p1;
         char[] t;
         while ((p0 = bb.indexOf('&', p0)) >= 0) {
             p1 = bb.indexOf(';', p0);
             if (p1 >= 0) {
                 t = transscript(bb.getChars(p0, p1 + 1));
                 bb = new serverCharBuffer(bb.getChars(0, p0), bb.length() + p0 - p1 + t.length).append(t).append(bb.getChars(p1 + 1));
             } else {
                 bb = new serverCharBuffer(bb.getChars(0, p0), bb.length()).append(bb.getChars(p0 + 1));
             }
         }
         t = null;
         return bb;
     }
 
     protected static serverCharBuffer stripAllTags(serverCharBuffer bb) {
         int p0 = 0, p1;
         while ((p0 = bb.indexOf(lb, p0)) >= 0) {
             p1 = bb.indexOf(rb, p0);
             if (p1 >= 0) {
                 bb = ((serverCharBuffer)new serverCharBuffer(bb.getChars(0, p0), bb.length() + p0 - p1 + 1).trim().append(32)).append(new serverCharBuffer(bb.getChars(p1 + 1)).trim());
             } else {
                 bb = new serverCharBuffer(bb.getChars(0, p0), bb.length()).trim().append(new serverCharBuffer(bb.getChars(p0 + 1)).trim());
             }
         }
         return bb.trim(); 
     }
 
     public static serverCharBuffer stripAll(serverCharBuffer bb) {
         //return stripAllTags(s);
         return convertUmlaute(transscriptAll(stripAllTags(bb)));
     }
 
     public void close() {
         // free resources
         tags0 = null;
         tags1 = null;
     }
     
     public void finalize() {
         close();
     }
     
 }
 
 
