 package net.sf.anathema.character.sidereal.reporting;
 
 import static net.sf.anathema.character.reporting.sheet.pageformat.IVoidStateFormatConstants.PADDING;
 import net.sf.anathema.character.generic.character.IGenericCharacter;
 import net.sf.anathema.character.generic.character.IGenericDescription;
 import net.sf.anathema.character.reporting.sheet.common.IPdfContentBoxEncoder;
 import net.sf.anathema.character.reporting.sheet.common.PdfHorizontalLineContentEncoder;
 import net.sf.anathema.character.reporting.sheet.page.IPdfPageEncoder;
 import net.sf.anathema.character.reporting.sheet.pageformat.PdfPageConfiguration;
 import net.sf.anathema.character.reporting.sheet.util.PdfBoxEncoder;
 import net.sf.anathema.character.reporting.util.Bounds;
 import net.sf.anathema.lib.resources.IResources;
 
 import com.lowagie.text.Document;
 import com.lowagie.text.DocumentException;
 import com.lowagie.text.pdf.BaseFont;
 import com.lowagie.text.pdf.PdfContentByte;
 
 public class SiderealDetailsPageEncoder implements IPdfPageEncoder {
 
   private final static int COLLEGE_HEIGHT = 312;
   private final static int DESTINY_HEIGHT = (COLLEGE_HEIGHT - PADDING) / 2;
   private final static int THIRD_BLOCK_HEIGHT = 145;
   private final static int STANDING_HEIGHT = 45;
   private final int essenceMax;
   private final IResources resources;
   private final BaseFont baseFont;
   private final BaseFont symbolBaseFont;
   private final PdfBoxEncoder boxEncoder;
   private final PdfPageConfiguration configuration;
   private final int fontSize;
 
   public SiderealDetailsPageEncoder(
       IResources resources,
       int essenceMax,
       BaseFont baseFont,
       BaseFont symbolBaseFont,
       int fontSize,
       PdfPageConfiguration configuration) {
     this.resources = resources;
     this.essenceMax = essenceMax;
     this.baseFont = baseFont;
     this.symbolBaseFont = symbolBaseFont;
     this.fontSize = fontSize;
     this.configuration = configuration;
     this.boxEncoder = new PdfBoxEncoder(resources, baseFont);
   }
 
   public void encode(
       Document document,
       PdfContentByte directContent,
       IGenericCharacter character,
       IGenericDescription description) throws DocumentException {
     int distanceFromTop = 0;
     distanceFromTop += encodeColleges(directContent, character, distanceFromTop);
     distanceFromTop += PADDING;
     distanceFromTop += encodeAstrology(directContent, character, distanceFromTop);
     distanceFromTop += PADDING;
     distanceFromTop += encodeArcaneFate(directContent, character, distanceFromTop);
     distanceFromTop += PADDING;
    distanceFromTop += encodeConnections(directContent, character, distanceFromTop);
 
     int centerDistanceFromTop = 0;
     centerDistanceFromTop += encodeResplendentDestiny(
         directContent,
         getCenterDestinyBounds(centerDistanceFromTop),
         character);
     centerDistanceFromTop += PADDING;
     centerDistanceFromTop += encodeResplendentDestiny(
         directContent,
         getCenterDestinyBounds(centerDistanceFromTop),
         character);
     centerDistanceFromTop += PADDING;
     centerDistanceFromTop += encodeResplendentDestiny(
         directContent,
         getCenterDestinyBounds(centerDistanceFromTop),
         character);
     centerDistanceFromTop += PADDING;
     centerDistanceFromTop += encodeAcquaintances(directContent, character, centerDistanceFromTop);
 
     int rightDistanceFromTop = 0;
     rightDistanceFromTop += encodeResplendentDestiny(
         directContent,
         getRightDestinyBounds(rightDistanceFromTop),
         character);
     rightDistanceFromTop += PADDING;
     rightDistanceFromTop += encodeResplendentDestiny(
         directContent,
         getRightDestinyBounds(rightDistanceFromTop),
         character);
     rightDistanceFromTop += PADDING;
     rightDistanceFromTop += encodeParadox(directContent, character, rightDistanceFromTop);
     rightDistanceFromTop += PADDING;
     rightDistanceFromTop += encodeStanding(directContent, character, rightDistanceFromTop);
     rightDistanceFromTop += PADDING;
     rightDistanceFromTop += encodeConventions(directContent, character, rightDistanceFromTop);
     rightDistanceFromTop += PADDING;
 
   }
 
  private int encodeConnections(PdfContentByte directContent, IGenericCharacter character, int distanceFromTop)
      throws DocumentException {
    int height = 145;
     Bounds boxBounds = configuration.getFirstColumnRectangle(distanceFromTop, height, 3);
     IPdfContentBoxEncoder encoder = new PdfHorizontalLineContentEncoder(4, "Sidereal.Connections"); //$NON-NLS-1$
     boxEncoder.encodeBox(directContent, encoder, character, boxBounds);
    return height;
   }
 
   private int encodeAcquaintances(PdfContentByte directContent, IGenericCharacter character, int distanceFromTop)
       throws DocumentException {
     int height = 145;
     Bounds boxBounds = configuration.getSecondColumnRectangle(distanceFromTop, height, 1);
     IPdfContentBoxEncoder encoder = new PdfHorizontalLineContentEncoder(1, "Sidereal.Acquaintances"); //$NON-NLS-1$
     boxEncoder.encodeBox(directContent, encoder, character, boxBounds);
     return height;
   }
 
   private int encodeConventions(PdfContentByte directContent, IGenericCharacter character, int distanceFromTop)
       throws DocumentException {
     int height = THIRD_BLOCK_HEIGHT - STANDING_HEIGHT - PADDING;
     Bounds boxBounds = configuration.getThirdColumnRectangle(distanceFromTop, height);
     IPdfContentBoxEncoder encoder = new PdfHorizontalLineContentEncoder(2, "Sidereal.Conventions"); //$NON-NLS-1$
     boxEncoder.encodeBox(directContent, encoder, character, boxBounds);
     return height;
   }
 
   private int encodeStanding(PdfContentByte directContent, IGenericCharacter character, int distanceFromTop)
       throws DocumentException {
     int height = STANDING_HEIGHT;
     Bounds boxBounds = configuration.getThirdColumnRectangle(distanceFromTop, height);
     IPdfContentBoxEncoder encoder = new StandingEncoder(baseFont, fontSize, resources);
     boxEncoder.encodeBox(directContent, encoder, character, boxBounds);
     return height;
   }
 
   private int encodeAstrology(PdfContentByte directContent, IGenericCharacter character, int distanceFromTop)
       throws DocumentException {
     int height = DESTINY_HEIGHT;
     Bounds boxBounds = configuration.getFirstColumnRectangle(distanceFromTop, height, 1);
     IPdfContentBoxEncoder encoder = new AstrologyInfoEncoder(baseFont, resources);
     boxEncoder.encodeBox(directContent, encoder, character, boxBounds);
     return height;
   }
 
   private int encodeResplendentDestiny(PdfContentByte directContent, Bounds boxBounds, IGenericCharacter character)
       throws DocumentException {
     IPdfContentBoxEncoder encoder = new ResplendentDestinyEncoder(baseFont, fontSize, resources);
     boxEncoder.encodeBox(directContent, encoder, character, boxBounds);
     return (int) boxBounds.height;
   }
 
   private Bounds getRightDestinyBounds(int distanceFromTop) {
     return configuration.getThirdColumnRectangle(distanceFromTop, DESTINY_HEIGHT);
   }
 
   private Bounds getCenterDestinyBounds(int distanceFromTop) {
     return configuration.getSecondColumnRectangle(distanceFromTop, DESTINY_HEIGHT, 1);
   }
 
   private int encodeParadox(PdfContentByte directContent, IGenericCharacter character, int distanceFromTop)
       throws DocumentException {
     int height = DESTINY_HEIGHT;
     Bounds boxBounds = configuration.getThirdColumnRectangle(distanceFromTop, height);
     IPdfContentBoxEncoder encoder = new ParadoxInfoEncoder(baseFont, symbolBaseFont, fontSize, resources);
     boxEncoder.encodeBox(directContent, encoder, character, boxBounds);
     return height;
   }
 
   private int encodeArcaneFate(PdfContentByte directContent, IGenericCharacter character, int distanceFromTop)
       throws DocumentException {
     int height = THIRD_BLOCK_HEIGHT;
     Bounds boxBounds = configuration.getFirstColumnRectangle(distanceFromTop, height, 1);
     IPdfContentBoxEncoder encoder = new ArcaneFateInfoEncoder(baseFont, symbolBaseFont, fontSize, resources);
     boxEncoder.encodeBox(directContent, encoder, character, boxBounds);
     return height;
   }
 
   private int encodeColleges(PdfContentByte directContent, IGenericCharacter character, int distanceFromTop)
       throws DocumentException {
     int height = COLLEGE_HEIGHT;
     Bounds boxBounds = configuration.getFirstColumnRectangle(distanceFromTop, height, 1);
     IPdfContentBoxEncoder encoder = new SiderealCollegeEncoder(baseFont, resources, essenceMax);
     boxEncoder.encodeBox(directContent, encoder, character, boxBounds);
     return height;
   }
 }
