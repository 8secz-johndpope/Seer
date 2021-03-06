 //
 // SewTiffs.java
 //
 
 import java.awt.image.BufferedImage;
 import java.io.File;
 import java.util.Hashtable;
 import loci.formats.FilePattern;
 import loci.formats.RandomAccessStream;
 import loci.formats.TiffTools;
 import loci.formats.in.TiffReader;
 import loci.formats.out.TiffWriter;
 
 /** Stitches the first plane from a collection of TIFFs into a single file. */
 public class SewTiffs {
 
   private static final int DOTS = 50;
 
   public static void main(String[] args) throws Exception {
     if (args.length < 2) {
       System.out.println(
         "Usage: java SewTiffs base_name channel_num [time_count]");
       System.exit(1);
     }
     String base = args[0];
     int c = Integer.parseInt(args[1]);
     int num;
     if (args.length < 3) {
       FilePattern fp = new FilePattern(new File(base + "_C" + c + "_TP1.tiff"));
       int[] count = fp.getCount();
       num = count[count.length - 1];
     }
     else num = Integer.parseInt(args[2]);
     System.out.println("Fixing " + base + "_C" + c + "_TP<1-" + num + ">.tiff");
     TiffReader in = new TiffReader();
     TiffWriter out = new TiffWriter();
     String outId = base + "_C" + c + ".tiff";
     System.out.println("Writing " + outId);
     System.out.print("   ");
     boolean comment = false;
     for (int t=0; t<num; t++) {
       String inId = base + "_C" + c + "_TP" + (t + 1) + ".tiff";
 
       // read first image plane
       BufferedImage image = in.openImage(inId, 0);
       in.close();
 
       if (t == 0) {
         // read first IFD
         RandomAccessStream ras = new RandomAccessStream(inId);
        Hashtable ifd = TiffTools.getFirstIFD(ras, 0);
         ras.close();
 
         // preserve TIFF comment
         Object descObj =
           TiffTools.getIFDValue(ifd, TiffTools.IMAGE_DESCRIPTION);
         String desc = null;
         if (descObj instanceof String) desc = (String) descObj;
         else if (descObj instanceof String[]) desc = ((String[]) descObj)[0];
 
         if (desc != null) {
           ifd = new Hashtable();
           TiffTools.putIFDValue(ifd, TiffTools.IMAGE_DESCRIPTION, desc);
           comment = true;
           out.saveImage(outId, image, ifd, t == num - 1);
           System.out.print(".");
           continue;
         }
       }
 
       // write image plane
       out.save(outId, image, t == num - 1);
 
       // update status
       System.out.print(".");
       if (t % DOTS == DOTS - 1) {
         System.out.println(" " + (t + 1));
         System.out.print("   ");
       }
     }
     System.out.println();
     if (comment) System.out.println("OME-TIFF comment saved.");
     else System.out.println("No OME-TIFF comment found.");
   }
 
 }
