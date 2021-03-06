 /*
  * Copyright (C) 2011 United States Government as represented by the Administrator of the
  * National Aeronautics and Space Administration.
  * All Rights Reserved.
  */
 
 package gov.nasa.worldwind.symbology.milstd2525;
 
 import gov.nasa.worldwind.avlist.*;
 import gov.nasa.worldwind.symbology.AbstractIconRetriever;
 import gov.nasa.worldwind.util.Logging;
 
 import java.awt.*;
 import java.awt.image.*;
 import java.util.MissingResourceException;
 
 /**
  * @author ccrick
  * @version $Id: MilStd2525IconRetriever.java 90 2011-17-10 23:58:29Z ccrick $
  */
 public class MilStd2525IconRetriever extends AbstractIconRetriever
 {
     // TODO: add more error checking
 
     public BufferedImage createIcon(String symbolIdentifier)
     {
         AVListImpl params = new AVListImpl();
 
         return createIcon(symbolIdentifier, params);
     }
 
     public BufferedImage createIcon(String symbolIdentifier, String URL)
     {
         if (URL == null)
         {
             String msg = Logging.getMessage("Symbology.RepositoryURLIsNull");
             Logging.logger().severe(msg);
             throw new IllegalArgumentException(msg);
         }
 
         setRepository(URL);
 
         return createIcon(symbolIdentifier);
     }
 
     public BufferedImage createIcon(String symbolIdentifier, AVList params, String URL)
     {
         if (URL == null)
         {
             String msg = Logging.getMessage("Symbology.RepositoryURLIsNull");
             Logging.logger().severe(msg);
             throw new IllegalArgumentException(msg);
         }
 
         setRepository(URL);
 
         return createIcon(symbolIdentifier, params);
     }
 
     public BufferedImage createIcon(String symbolIdentifier, AVList params)
     {
         // retrieve desired symbol and convert to bufferedImage
         SymbolCode symbolCode = new SymbolCode(symbolIdentifier);
 
         BufferedImage img = null;
         String filename = getFilename(symbolCode);
 
         img = retrieveImageFromURL(filename, img);
 
         if (img == null)
         {
             String msg = Logging.getMessage("Symbology.SymbolIconNotFound", symbolCode);
             Logging.logger().severe(msg);
             throw new MissingResourceException(msg, BufferedImage.class.getName(), filename);
         }
 
         // apply dotted border where required by Standard Identity (cases P, A, S, G, M)
         String stdid = (String) symbolCode.getValue(SymbolCode.STANDARD_IDENTITY);
         if ("PASGMpasgm".indexOf(stdid.charAt(0)) > -1)
         {
             BufferedImage dest = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
             Graphics2D g = dest.createGraphics();
             g.setComposite(AlphaComposite.SrcOver);
             g.setBackground(new Color(0, 0, 0, 0));
             g.clearRect(0, 0, dest.getWidth(), dest.getHeight());
             g.drawImage(img, 0, 0, null);
 
             // now overlay dotted line
             BufferedImage overlay = retrieveOverlay(symbolCode, params);
             if (overlay == null)
             {
                 String msg = Logging.getMessage("Symbology.SymbolIconOverlayNotFound", symbolCode);
                 Logging.logger().severe(msg);
                 throw new MissingResourceException(msg, BufferedImage.class.getName(), filename);
             }
             g.drawImage(overlay, 0, 0, null);
             g.dispose();
 
             img = dest;
         }
 
         // handle Joker and Faker, which should have a Friend frame, but with fill color red
         if ("JKjk".indexOf(stdid.charAt(0)) > -1)
         {
            img = changeFillColor(img, SymbolCode.COLOR_HOSTILE);
         }
 
         // TODO: add exercise overlay
 
         return img;
     }
 
     protected BufferedImage retrieveOverlay(SymbolCode symbolCode, AVList params)
     {
         BufferedImage img = null;
         String filename = null;
         String stdID = (String) symbolCode.getValue(SymbolCode.STANDARD_IDENTITY);
         String battleDim = (String) symbolCode.getValue(SymbolCode.BATTLE_DIMENSION);
         String functionID = (String) symbolCode.getValue(SymbolCode.FUNCTION_ID);
 
         // TODO: handle special case of installations with overlays
 
         if (stdID.equalsIgnoreCase(SymbolCode.IDENTITY_PENDING) ||
             stdID.equalsIgnoreCase(SymbolCode.IDENTITY_EXERCISE_PENDING))
         {
             if (battleDim.equalsIgnoreCase(SymbolCode.BATTLE_DIMENSION_UNKNOWN) ||
                 battleDim.equalsIgnoreCase(SymbolCode.BATTLE_DIMENSION_GROUND) ||
                 battleDim.equalsIgnoreCase(SymbolCode.BATTLE_DIMENSION_SEA_SURFACE) ||
                 battleDim.equalsIgnoreCase(SymbolCode.BATTLE_DIMENSION_SOF))
             {
                 // 1. clover
                 filename = "clover_overlay.png";
             }
             else if (battleDim.equalsIgnoreCase(SymbolCode.BATTLE_DIMENSION_SPACE) ||
                 battleDim.equalsIgnoreCase(SymbolCode.BATTLE_DIMENSION_AIR))
             {
                 // 2. cloverTop
                 filename = "clovertop_overlay.png";
             }
             else if (battleDim.equalsIgnoreCase(SymbolCode.BATTLE_DIMENSION_SUBSURFACE))
             {
                 // 3. cloverBottom
                 filename = "cloverbottom_overlay.png";
             }
         }
         else if
             (stdID.equalsIgnoreCase(SymbolCode.IDENTITY_ASSUMED_FRIEND) ||
                 stdID.equalsIgnoreCase(SymbolCode.IDENTITY_EXERCISE_ASSUMED_FRIEND))
         {
             if (battleDim.equalsIgnoreCase(SymbolCode.BATTLE_DIMENSION_UNKNOWN) ||
                 battleDim.equalsIgnoreCase(SymbolCode.BATTLE_DIMENSION_SEA_SURFACE))
             {
                 // 4. circle
                 filename = "circle_overlay.png";
             }
             else if
                 (battleDim.equalsIgnoreCase(SymbolCode.BATTLE_DIMENSION_SPACE) ||
                     battleDim.equalsIgnoreCase(SymbolCode.BATTLE_DIMENSION_AIR))
             {
                 // 5. arch
                 filename = "arch_overlay.png";
             }
             else if (battleDim.equalsIgnoreCase(SymbolCode.BATTLE_DIMENSION_SUBSURFACE))
             {
                 // 6. smile
                 filename = "smile_overlay.png";
             }
             else if
                 (battleDim.equalsIgnoreCase(SymbolCode.BATTLE_DIMENSION_GROUND))
             {
                 if ("E".equalsIgnoreCase(functionID.substring(0, 1)))   // special case of Ground Equipment
                 {
                     // 4. circle
                     filename = "circle_overlay.png";
                 }
                 else        // Units and Installations
                 {
                     // 7. rectangle
                     filename = "rectangle_overlay.png";
                 }
             }
             else if
                 (battleDim.equalsIgnoreCase(SymbolCode.BATTLE_DIMENSION_SOF))
             {
                 // 7. rectangle
                 filename = "rectangle_overlay.png";
             }
         }
         else if (stdID.equalsIgnoreCase(SymbolCode.IDENTITY_SUSPECT))
         {
             if (battleDim.equalsIgnoreCase(SymbolCode.BATTLE_DIMENSION_UNKNOWN) ||
                 battleDim.equalsIgnoreCase(SymbolCode.BATTLE_DIMENSION_GROUND) ||
                 battleDim.equalsIgnoreCase(SymbolCode.BATTLE_DIMENSION_SEA_SURFACE) ||
                 battleDim.equalsIgnoreCase(SymbolCode.BATTLE_DIMENSION_SOF))
             {
                 // 8. diamond
                 filename = "diamond_overlay.png";
             }
             else if (battleDim.equalsIgnoreCase(SymbolCode.BATTLE_DIMENSION_SPACE) ||
                 battleDim.equalsIgnoreCase(SymbolCode.BATTLE_DIMENSION_AIR))
             {
                 // 9. tent
                 filename = "tent_overlay.png";
             }
             else if (battleDim.equalsIgnoreCase(SymbolCode.BATTLE_DIMENSION_SUBSURFACE))
             {
                 // 10. top
                 filename = "top_overlay.png";
             }
         }
 
         // handle the special case of overlays for installations (Ground battle dimension only)
         if (battleDim.equalsIgnoreCase(SymbolCode.BATTLE_DIMENSION_GROUND)
             && "I".equalsIgnoreCase(functionID.substring(0, 1)))
         {
             filename = "installation_" + filename;
         }
 
         img = retrieveImageFromURL(filename, img);
 
         return img;
     }
 
     protected static String getFilename(SymbolCode code)
     {
 
         String standardID = (String) code.getValue(SymbolCode.STANDARD_IDENTITY);
         standardID = standardID.toLowerCase();
 
         int prefix = 0;
         char stdid = 'u';
         // See Table I and TABLE II, p.15 of MIL-STD-2525C for standard identities that use similar shapes
         switch (standardID.charAt(0))
         {
             case 'p':      // PENDING
             case 'u':      // UNKNOWN
             case 'g':      // EXERCISE PENDING
             case 'w':      // EXERCISE UNKNOWN
                 prefix = 0;
                 stdid = 'u';
                 break;
             case 'f':      // FRIEND
             case 'a':      // ASSUMED FRIEND
             case 'd':      // EXERCISE FRIEND
             case 'm':      // EXERCISE ASSUMED FRIEND
             case 'j':      // JOKER
             case 'k':      // FAKER
                 prefix = 1;
                 stdid = 'f';
                 break;
             case 'n':      // NEUTRAL
             case 'l':      // EXERCISE NEUTRAL
                 prefix = 2;
                 stdid = 'n';
                 break;
             case 'h':      // HOSTILE
             case 's':      // SUSPECT
                 prefix = 3;
                 stdid = 'h';
                 break;
             default:
                 String msg = Logging.getMessage("Symbology.InvalidSymbolCode", standardID);
                 Logging.logger().severe(msg);
                 throw new IllegalArgumentException(msg);
         }
 
         String padding = "-----";
 
         // handle special case of installations, as indicated by a 'H' in position 11
         if ("Hh".indexOf(((String) code.getValue(SymbolCode.SYMBOL_MODIFIER)).charAt(0)) > -1)
             padding = "h----";
 
         String result = Integer.toString(prefix) + '.' + code.getValue(SymbolCode.SCHEME).toString().toLowerCase()
             + stdid + code.getValue(SymbolCode.BATTLE_DIMENSION).toString().toLowerCase()
             + 'p' + code.getValue(SymbolCode.FUNCTION_ID).toString().toLowerCase()
             //+ code.getValue(SymbolCode.SYMBOL_MODIFIER).toString().toLowerCase()
             + padding + ".png";
         return result;
     }
 }
 
