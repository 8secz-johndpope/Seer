 package unigene;
 
 import java.io.*;
 import java.net.*;
 import org.biojava.bio.*;
 import org.biojava.bio.program.unigene.*;
 
 public class CreateUnigeneFlat {
   public static void main(String[] args)
   throws Exception {
     URL url = new URL(new URL("file:"), args[0]);
    UnigeneDB unigene = UnigeneTools.createUnigene(url);
   }
 }
