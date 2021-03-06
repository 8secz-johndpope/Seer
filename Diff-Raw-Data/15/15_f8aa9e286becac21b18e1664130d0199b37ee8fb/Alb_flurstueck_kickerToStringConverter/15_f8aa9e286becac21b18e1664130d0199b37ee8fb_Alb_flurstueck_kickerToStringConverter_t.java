 /*
  * To change this template, choose Tools | Templates
  * and open the template in the editor.
  */
 package de.cismet.cids.custom.tostringconverter.wunda_blau;
 
import de.cismet.cids.dynamics.CidsBean;
 import de.cismet.cids.tools.CustomToStringConverter;
 
 /**
  *
  * @author srichter
  */
 public class Alb_flurstueck_kickerToStringConverter extends CustomToStringConverter {
 
    public static final String HISTORISCH = " (hist.)";

     @Override
     public String createString() {
         final StringBuilder result = new StringBuilder();
         result.append(cidsBean.getProperty("gemarkung"));
         result.append("-");
         result.append(cidsBean.getProperty("flur"));
         result.append("-");
         result.append(cidsBean.getProperty("zaehler"));
         Object nenner = cidsBean.getProperty("nenner");
         result.append("/");
         if (nenner != null) {
             result.append(nenner);
         } else {
             result.append("0");
         }
         Object real_flurstueck = cidsBean.getProperty("fs_referenz");
        if (real_flurstueck instanceof CidsBean) {
            CidsBean fsBean = (CidsBean) real_flurstueck;
            Object hist_date = fsBean.getProperty("historisch");
            if (hist_date != null) {
                result.append(HISTORISCH);
            }
        } else {
            result.append(HISTORISCH);
         }
         return result.toString();
     }
 }
