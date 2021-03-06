 package org.xhtmlrenderer.layout.inline;
 
 import org.xhtmlrenderer.layout.Context;
 import org.xhtmlrenderer.layout.FontUtil;
 import org.xhtmlrenderer.render.InlineBox;
 import org.xhtmlrenderer.util.Uu;
 
 import java.awt.Font;
 
 public class Breaker {
     public static void breakText(Context c, InlineBox inline, InlineBox prev_align, int avail, int max, Font font) {
         boolean db = false;
         if (db) {
             Uu.p("=========================");
             Uu.p("breaking: " + inline);
             //Uu.p("breaking : '" + inline.getSubstring() + "'");
             //Uu.p("avail = " + avail);
             //Uu.p("max = " + max);
             //Uu.p("prev align = " + prev_align);
         }
 
         inline.setFont(font);
         
         // ====== handle nowrap
         if (inline.whitespace.equals("nowrap")) {//we can't touch it
             return;
         }
 
         //check if we should break on the next newline
         if (inline.whitespace.equals("pre") ||
                 inline.whitespace.equals("pre-wrap") ||
                 inline.whitespace.equals("pre-line")) {
             // Uu.p("doing a pre line");
             int n = inline.getSubstring().indexOf(WhitespaceStripper.EOL);
             // Uu.p("got eol at: " + n);
             if (n > -1) {
                 inline.setSubstringLength(n + 1);
                 inline.break_after = true;
             }
         }
 
         //check if we may wrap
         if (inline.whitespace.equals("pre")) {//we can't do anymore
             return;
         }
 
         //check if it all fits on this line
         if (FontUtil.len(c, inline.getSubstring(), font) < avail) {
             return;
         }
         
         //all newlines are already taken care of
         //text too long to fit on this line, we may wrap
         //just find a space that works
         String currentString = inline.getSubstring();
         int n = currentString.length();
        int pn;
         do {
            pn = n;
            n = currentString.lastIndexOf(WhitespaceStripper.SPACE, pn - 1);
         } while (n >= 0 && FontUtil.len(c, currentString.substring(0, n), font) >= avail);
 
        if (n < 0) {//unbreakable string
            if (prev_align != null) {
                 inline.break_before = true;
             }
            return;
         } else {//found a place to wrap
             inline.setSubstring(inline.start_index, inline.start_index + n);
            inline.break_after = true;
            return;
         }
 
     }
 
 }
