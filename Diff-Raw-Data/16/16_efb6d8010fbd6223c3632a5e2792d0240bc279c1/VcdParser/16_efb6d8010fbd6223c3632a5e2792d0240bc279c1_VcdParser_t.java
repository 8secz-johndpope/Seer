 package hr.fer.zemris.vhdllab.vhdl.simulations;
 
 
 import java.io.BufferedReader;
 import java.io.FileNotFoundException;
 import java.io.FileReader;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.LinkedHashMap;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 
 /**
  * Klasa cita linije VCD-datoteke i sprema ih polje stringova
  * @author Boris Ozegovic
  */
 class VcdFileReader
 {
     /** Ime datoteke koju treba parsirati */
 	private String fileName;
 
 
     /** Constructor
      * 
      * @param fileName VCD file
      */
 	VcdFileReader (String fileName)
 	{
 		this.fileName = fileName;
 	}
 
 
 	/** 
      * Metoda koja cita VCD-datoteku
      * 
      * @return Polje stringova; string predstavlja pojedinu liniju datoteke
      */
 	public String[] toArrayString ()
 	{
 		String string;
 		List<String> list = new ArrayList<String>();
 
 		try
 		{
 			BufferedReader in = new BufferedReader(new FileReader (fileName));
 			while ((string = in.readLine()) != null) 
             {
 				list.add(string);
             }
             in.close();
 		}
 		catch (FileNotFoundException e)
 		{
 			System.out.println ("File open error");
 		}
 		catch (IOException e)
 		{
 			System.out.println ("File input/output error");
 		}
 		return ((String[])list.toArray(new String[list.size()]));
 	}
 }
 
 
 /**
  * Klasa parsira VCD-datoteku koju generira GHDL (GHDL je GNU VHDL simulator)
  * VCD-datoteka ima sljedeci format:  9 prvih linija predstavljaju osnovne
  * informacije koje su konstantne za svaku datoteku.  Nakon njih dolazi popis 
  * svih signala predstavljen u obliku ASCII simbola, npr:
  *          $var reg 1 ! tb_a $end
  * Svaka linija koja pocinje s "$var" predstavlja jedan signal.  U produzetku se
  * nalazi tip signala (vektor, bit) predstavljen brojem iza podstringa "reg", 
  * i sam ASCII simbol signala (u ovom slucaju je to "!").  Na kraju se nalazi
  * ime tog signala.  Svaki se signal nalazi unutar svog scopea.
  * S podstringom "#0" pocinje samo simulacija: svaka se tocka promjene oznacuje
  * s '#' iza kojih slijedi vrijednost promjene.  Vazno je napomenuti da vektori
  * imaju poseban nacin oznacavanja: svaki vektor pocinje sa slovom 'b', a prije 
  * ASCII vrijednosti signala mora biti praznina.
  *
  * @author Boris Ozegovic
  */
 public class VcdParser
 {
     /* Parser pocinje parsirati nakon osnovnih informacija */
 	private int index = 9;
 	
     /** Sadrzi sve linije datoteke */
     private String[] vcdLines;
 
     /** 
      * Mapa ciji su kljucevi kompletna imena signala (scope ukljuciv), 
      * te cije su vrijednosti poznate u svakoj od tocaka tranzicije 
      */
 	private Map<String, List<String>> signalValues = new LinkedHashMap<String, List<String>>();
 
     /** Sadrzi vrijednosti tocaka tranzicije (npr. 750-toj ns) */
 	private List<Long> transitionPoints = new ArrayList<Long>();
 
     /** Sadrzi upotrebljene ASCII simbole, svaki od njih predstavlja signal */
     private List<Character> asciiSignalSimbols = new ArrayList<Character>();
 
     /** Predstavlja najduze ime od svih signala, potreban zbog duljine panela */
     private int maximumSignalNameLength;
 
     /** Predstavlja string koji se prenosi klijentu */
     private StringBuffer resultInString = new StringBuffer("");
 
     /** 
      * Upotrebljava se za interni format prilikom razdvajanja imena signala i
      * razdvajanja vrijednosti za svaki od signala prilikom transformacije us
      * mape u string
      */
     private final String LIMITER = "###";
 
     /** 
      * Limiter koji razdvaja s jedne strane sva imena signala, njihove
      * vrijednosti, tocke u kojima se dogada promjena vrijednosti i konacno
      * duljinu u znakovima najduzeg imena signala
      */
     private final String HEAD_LIMITER = "%%%";
 
     /** Limiter koji razdvaju svaku od trenutnih vrijednosti (0 */
     private final String VALUE_LIMITER = "&&&";
 
 
     /** 
      * Constructor
      * 
      * @param fileName ime datoteke
      */
 	public VcdParser (String fileName)
 	{
 		VcdFileReader reader = new VcdFileReader(fileName);
 		vcdLines = reader.toArrayString();
 	}
     
 
 	/** 
      * Metoda izdvaja ASCII reprezentante odgovarajucih signala i trazi
      * puna imena svih signala.  Mapa imena signala/njihove vrijednosti poslije
      * koristenja ove metode sadrzavat ce samo imena signala kao kljuceve, bez
      * vrijednosti.  Koristi je metoda parse();
      */
 	public void parseSignals()
 	{
         /* Pretrazuje sve linije dok ne nade kraj definiranja simbola.  Ako linija
          * pocinje s $var znaci da predstavlja signal, pa se pronalazi ASCII simbol
          * i puno ime.  Ako linija pocinje s $scope znaci da ulazimo u novi scope te
          * se u listu (koja se ponasao kao stog) dodaje novi scope na prethodni.
          * Inace brise zadnji dodan scope.
          */
 		LinkedList<String> scopeName = new LinkedList<String>();
 		scopeName.addFirst("/");
 		int endSignIndex; /* indeks na kojem pocinje $end */  
 		String signalName; /* ime bez scopea */
 		StringBuffer completeSignalName = new StringBuffer(""); /* ime sa scopeom */
 		while (!vcdLines[index].equals("$enddefinitions $end"))
 		{
 			endSignIndex = vcdLines[index].indexOf("$end");
 			if (vcdLines[index].indexOf("$var") == 0)
 			{	
 				asciiSignalSimbols.add(vcdLines[index].charAt(11));
 				signalName = vcdLines[index].substring(13, endSignIndex - 1);
 				completeSignalName.setLength(0);
 				completeSignalName.append(scopeName.getFirst()).append(signalName);
                 if (vcdLines[index].charAt(9) != '1')
                 {
                     completeSignalName.insert(0, "+ ");
                 }
 				signalValues.put(completeSignalName.toString(), new ArrayList<String>());
                 if (completeSignalName.length() > maximumSignalNameLength)
                 {
                     maximumSignalNameLength = completeSignalName.length();
                 }
 			}
 			else if (vcdLines[index].indexOf ("$scope") == 0)
 			{
 				scopeName.addFirst(new StringBuffer(scopeName.getFirst())
                         .append(vcdLines[index].substring(14, endSignIndex - 1)).append("/").toString());
 			}
 			else 
 			{	
 				scopeName.removeFirst ();
 			}
 			index++;
 		}
 	}
 
     
     /**
      * Metoda koja parsira cijelu VHDL-simulaciju.  Nakon sto se upotrijebi ova
      * metoda dobije se rezultat u obliku stringa kojeg vraca
      * getResultInString() metoda.  Za potpuno parsiranje dovoljna je upotreba
      * samo ove metode.
      */ 
 	public void parse()
 	{
         /* Pretrazuje sve do kraja datoteke, tj. polja Stringova.  Izmedu svake
          * tocke u kojima se dogada promjena (tocke tranzicije) cita red po red i
          * provjerava vrijednosti signala.  Ako vrijednost pocinje s slovom 'b'
          * znaci da slijedi vrijednost vektora iza koje se nalazi znak praznine i
          * tek onda ASCII simbol, inace obraduje normalni signal.  Pronalazi ASCII
          * simbol za svkai od signala i usporeduje koji je to signal u polju po redu
          * tako da automatski moze dodati pod tim indeksom novu vrijednost u
          * privremenom polju values.  Nakon svake iteracije pune se mapa s
          * vrijednostima polje values, s tim da oni signali koji nisu mijenjali
          * vrijednost imaju vrijednost od prethodne iteracije.
          */
 		
         parseSignals();
 		
         /* vrijednosti pojedinih signala.  Mogu biti 0, 1, U, X, Z... */
 		String[] values = new String[asciiSignalSimbols.size()];
 		char asciiSimbol;
 		int position;
 		int positionOfWhiteSpaceChar;
 
         /* odmah dodaje nulu.  Nula predstavlja pocetak simulacije */
 		transitionPoints.add(Long.valueOf(vcdLines[++index].substring(1)));
		boolean isSharpFound = true;
 		for (++index; index < vcdLines.length; index++)
 		{
            while (vcdLines[index].charAt(0) != '#')
             {
                 if (vcdLines[index].charAt(0) != 'b')
                 {
                     asciiSimbol = vcdLines[index].charAt(1);
                     position = asciiSignalSimbols.indexOf(asciiSimbol);
                     values[position] = new Character(vcdLines[index].charAt(0)).toString();
                 }
                 else
                 {
                     positionOfWhiteSpaceChar = vcdLines[index].indexOf(' ');
                     asciiSimbol = vcdLines[index].charAt(positionOfWhiteSpaceChar + 1);
                     position = asciiSignalSimbols.indexOf(asciiSimbol);
                     values[position] = vcdLines[index].substring (1, positionOfWhiteSpaceChar);
                 }
                 index++;
				if (index == vcdLines.length) {
					isSharpFound = false;
					break;
				}
             }
			if (isSharpFound) {
				transitionPoints.add (Long.valueOf(vcdLines[index].substring(1)));
			} else {
				transitionPoints.add(transitionPoints.get(transitionPoints.size() - 1));
			}
             Iterator<List<String>> e = signalValues.values().iterator();
             int i = 0;
             while (e.hasNext())
             {
                 ((List<String>)e.next()).add(values[i++]);
             }
 		}
         this.resultToString();
 	}
 
 
     /**
      * Getter koji vraca rezultat simulacije kao jedan string
      *
      * @return Rezultat simulacije predstavljen kao String
      */
     public String getResultInString ()
     {
         return resultInString.toString();
     }
         
 
     /**
      * Getter koji vraca mapu imena signala i njihovih vrijednosti
      */
 	public Map<String, List<String>> getSignalValues ()
 	{
 		return signalValues;
 	}
 
     
     /**
      * Getter koji vraca tocke promjene vrijednosti 
      */
 	public List<Long> getTransitionPoints ()
 	{
 		return transitionPoints;
 	}
 
 
     /**
      * Getter koji vraca najduze ime signala
      */
     public int getMaximumSignalNameLength ()
     {
         return maximumSignalNameLength;
     }
 
 
     /**
      * Test metoda
      */
 	public static void main(String[] args)
 	{
 		VcdParser parser = new VcdParser("adder2.vcd");
 		parser.parse();
         parser.resultToString();
         System.out.println(parser.getResultInString());
 	}
 
 
     /**
      * Metoda transformira mapu sa imenima signala i njihovim vrijednostima, te
      * listu sa svim tockama u kojima se dogada promjena vrijednosti signala u
      * jedan string pomocu internog formata, npr: A###B%%%0&&&1&&&Z###1&&&Z&&&1
      * Razlog je tome sto se pomocu HTTP-a ne mogu prenositi objekti
      */
     private void resultToString ()
     {
         for (String key : signalValues.keySet())
         {
             resultInString.append(key).append(LIMITER);
         }
 
         /* ukloni zadnji limiter */
         resultInString.delete(resultInString.length() - 3, resultInString.length());
         
         /* stavi limiter izmedu imena signala i vrijednosti */
         resultInString.append(HEAD_LIMITER);
 
         for (List<String> values : signalValues.values())
         {
             for (String string : values)
             {
                 resultInString.append(string).append(VALUE_LIMITER);
             }
             resultInString.delete(resultInString.length() - 3, resultInString.length());
             resultInString.append(LIMITER);
         }
         resultInString.delete(resultInString.length() - 3, resultInString.length());
         /* 
          * stavli limiter izmedu vrijednosti signala i tocaka u kojima se dogada
          * promjena vrijednosti 
          */
         resultInString.append(HEAD_LIMITER);
 
         for (Long point : transitionPoints)
         {
             resultInString.append(point).append(LIMITER);
         }
 
         /* ukloni zadnji limiter */
         resultInString.delete(resultInString.length() - 3, resultInString.length());
 
         /* broj znakova najduljeg imena signala */
         resultInString.append(HEAD_LIMITER).append(maximumSignalNameLength);
     }
 }
 
 
