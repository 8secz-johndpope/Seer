 /*
   ItsNat Java Web Application Framework
   Copyright (C) 2007-2011 Jose Maria Arranz Santamaria, Spanish citizen
 
   This software is free software; you can redistribute it and/or modify it
   under the terms of the GNU Lesser General Public License as
   published by the Free Software Foundation; either version 3 of
   the License, or (at your option) any later version.
   This software is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
   Lesser General Public License for more details. You should have received
   a copy of the GNU Lesser General Public License along with this program.
   If not, see <http://www.gnu.org/licenses/>.
 */
 
 package org.itsnat.impl.core.jsren;
 
 import java.io.UnsupportedEncodingException;
 import org.itsnat.core.ItsNatException;
 import org.itsnat.core.script.ScriptExpr;
 import org.itsnat.impl.core.browser.Browser;
 import org.itsnat.impl.core.browser.BrowserMSIEOld;
 import org.itsnat.impl.core.clientdoc.ClientDocumentStfulImpl;
 import org.w3c.dom.Node;
 
 /**
  *
  * @author jmarranz
  */
 public abstract class JSRenderImpl
 {
 
     /** Creates a new instance of JSRenderImpl */
     public JSRenderImpl()
     {
     }
 
     public static String javaToJS(Object value,boolean cacheIfPossible,ClientDocumentStfulImpl clientDoc)
     {
         // Convierte value en el adecuado cdigo JavaScript.
         if (value == null) return "null";
 
         if (value instanceof Node)
             return clientDoc.getNodeReference((Node)value,cacheIfPossible,true);
         else if (value instanceof Boolean)
             return value.toString(); // Devuelve true o false en minsculas (sin comillas)
         else if (value instanceof Character)
             return getTransportableCharLiteral(((Character)value).charValue(),clientDoc.getBrowser());
         else if (value instanceof Number)
             return value.toString();
         else if (value instanceof ScriptExpr)
             return ((ScriptExpr)value).getCode();
         else if (value instanceof ScriptReference) // Por ahora no se usa salvo en pruebas
             return ((ScriptReference)value).getCode();
         else if (value instanceof String)
             return toTransportableStringLiteral((String)value,clientDoc.getBrowser());
         else
             return value.toString();
     }
 
     public static String toLiteralStringJS(String value)
     {
         if (value == null)
             value = "null";
         else
             value = "\"" + value + "\"";
         return value;
     }
 
     public static String toTransportableStringLiteral(String text,Browser browser)
     {
         return toTransportableStringLiteral(text,true,browser);
     }
 
     public static String toTransportableStringLiteral(String text,boolean addQuotation,Browser browser)
     {
         StringBuilder encoded = new StringBuilder(text);
         for (int i = 0; i < encoded.length(); i++)
         {
             char c = encoded.charAt(i);
             switch(c)
             {
                 case '\r':  encoded.deleteCharAt(i); // Caso Windows (CR), deber seguir un LF (\n). Lo eliminamos porque en navegadores no MSIE genera dos fines de lnea, en MSIE lo que haremos ser aadir un \r al procesar el \n
                             i--; // Pues el i++ aade otro ms y al eliminar uno no nos hemos movido
                             break;
                 case '\n':  encoded.deleteCharAt(i);
                             if (browser instanceof BrowserMSIEOld) // Importante por ejemplo cuando se aade dinmicamente el nodo de texto a un <textarea> o a un <pre> (no probado pero el problema parece que es el mismo)
                             {
                                 encoded.insert(i,"\\r");
                                 i += 2; // Pues hemos aadido dos caracteres
                             }
                             encoded.insert(i,"\\n");
                             i++; // Uno slo pues el i++ del for ya aade otro ms
                             break;
                 case '"':   encoded.deleteCharAt(i);
                             encoded.insert(i,"\\\"");
                             i++;
                             break;
                 case '\'':  if (!addQuotation) // Si la cadena se mete entre "" no hace falta "escapar" la ' 
                             {
                                 encoded.deleteCharAt(i);
                                 encoded.insert(i,"\\'");
                                 i++;
                             }
                             break;
                 case '\\':  encoded.deleteCharAt(i);
                             encoded.insert(i,"\\\\");
                             i++;
                             break;
                 case '\t':  encoded.deleteCharAt(i);
                             encoded.insert(i,"\\t");
                             i++;
                             break;
                 case '\f':  encoded.deleteCharAt(i); // FORM FEED
                             encoded.insert(i,"\\f");
                             i++;
                             break;
                 case '\b':  encoded.deleteCharAt(i); // BACK SPACE
                             encoded.insert(i,"\\b");
                             i++;
                             break;
             }
         }
 
         if (addQuotation)
         {
            if (encoded.indexOf("</script>") != -1) // Raro pero puede ocurrir por ejemplo si el texto es el contenido de un comentario y se procesa por JavaScript como en BlackBerry y S60WebKit en carga o est en el valor inicial en carga de un input o similar
             {
                String encoded2 = encoded.toString().replaceAll("</script>", "</\" + \"script>");
                //String encoded2 = encoded.toString().replaceAll("</script>", "<\\/script>"); NO VALE, genera un </script> normal
                 return "\"" + encoded2 + "\"";
             }
             else
                 return "\"" + encoded + "\"";
         }
         else
             return encoded.toString();
     }
 
     public static String getTransportableCharLiteral(char c,Browser browser)
     {
         // Permite meter el caracter en cdigo JavaScript
         if (c == '\r')  // Hay que tratarlo aparte porque toTransportableStringLiteral elimina el '\r' pues en cadenas al '\r' le sigue siempre (en Windows) el '\n' y si se envan los dos los browser no MSIE duplican los espacios pues con un '\n' ya le vale (a modo de Unix incluso en Windows). Para MSIE el proceso de un solo \n generar el \r correspondiente.
             return "'\r'";
         else
         {
             String encoded = toTransportableStringLiteral(Character.toString(c),browser);
             return "'" + encoded + "'";
         }
     }
 
     public static String encodeURIComponent(char c)
     {
         return encodeURIComponent(Character.toString(c));
     }
 
     public static String encodeURIComponent(String text)
     {
         return encodeURIComponent(text,true);
     }
 
     public static String encodeURIComponent(String text,boolean encodeSpaces)
     {
         // Sirve para codificar en el servidor preparado
         // para ser descodificado por decodeURIComponent()
         // con JavaScript, es decir emulando encodeURIComponent de JavaScript
         // Usamos URLEncoder.encode() que es lo ms parecido.
         // http://xkr.us/articles/javascript/encode-compare/
         // En el caso del caracter ' URLEncoder.encode() lo "escapea" con %
         // sin embargo encodeURIComponent de JavaScript no, pero de todas formas
         // funciona el decodeURIComponent en JavaScript.
 
 
         try
         {
             text = java.net.URLEncoder.encode(text,"UTF-8");
         }
         catch(UnsupportedEncodingException ex)
         {
             throw new ItsNatException(ex);
         }
 
         StringBuilder textBuff = new StringBuilder(text);
         for(int i = 0; i < textBuff.length(); i++)
         {
             char c = textBuff.charAt(i);
             if (c == '+')
             {
                 if (encodeSpaces)
                 {
                     textBuff.deleteCharAt(i);
                     textBuff.insert(i,"%20");
                     i += 2;
                 }
                 else
                     textBuff.setCharAt(i,' ');
             }
         }
         return textBuff.toString();
     }
 
     public static String encodeURI(String text)
     {
         // NO SE USA, se usa encodeURIComponent(String)
         // pero la conservamos porque funcionaba
 
         // Emulamos la funcionalidad de la funcin
         // JavaScript encodeURI() tal que un texto codificado en el servidor
         // y preparado para meterse transportarse como texto al cliente (en cadenas literales de JavaScript por ejemplo),
         // pueda volver al original usando decodeURI() en JavaScript.
         // Para ello usamos URLEncoder.encode() muy parecida pero no exactamente igual
         // URLEncoder.encode() convierte el ' ' en '+' sin embargo
         // decodeURI() en JavaScript espera %20
         // Por otra parte URLEncoder.encode() convierte '+' en %2B el
         // cual es ignorado por decodeURI(), por ello volvemos a
         // poner como '+' el %2B
         try
         {
             text = java.net.URLEncoder.encode(text,"UTF-8");
             for(int i = 0; i < text.length(); i++)
             {
                 char c = text.charAt(i);
                 if (c == '+')
                 {
                     text = text.substring(0,i) + "%20" + text.substring(i + 1,text.length());
                     i += 2;
                 }
                 else if ((c == '%') && (text.length() - i >= 3))
                 {
                     // Caracteres que han sido encoded por URLEncoder pero que no codifica encodeURI
                     String charCodeStr = text.substring(i + 1, i + 3);
                     char charCode = (char)Integer.parseInt(charCodeStr,16);
                     if ((charCode == 0x21) || //  !
                         (charCode == 0x23) || //  #
                         (charCode == 0x24) || //  $
                         ((0x26 <= charCode ) && (charCode <= 0x3B)) || // & ' ( ) + , / : ;
                         (charCode == 0x3D) || //  =
                         (charCode == 0x3F) || //  ?
                         (charCode == 0x40))   //  @
                     {
                        text = text.substring(0,i) + charCode + text.substring(i + 3,text.length());
                     }
                 }
             }
             return text;
         }
         catch(UnsupportedEncodingException ex)
         {
             throw new ItsNatException(ex);
         }
     }
 
     public static String getSetPropertyCode(Object object,String propertyName,Object value,boolean endSentence,boolean cacheIfPossible,ClientDocumentStfulImpl clientDoc)
     {
         StringBuilder code = new StringBuilder();
         code.append( javaToJS(object,cacheIfPossible,clientDoc) + "." + propertyName + "=" + javaToJS(value,cacheIfPossible,clientDoc) );
         if (endSentence)
             code.append( ";" );
         return code.toString();
     }
 
     public static String getGetPropertyCode(Object object,String propertyName,boolean endSentence,boolean cacheIfPossible,ClientDocumentStfulImpl clientDoc)
     {
         StringBuilder code = new StringBuilder();
         code.append( javaToJS(object,cacheIfPossible,clientDoc) + "." + propertyName );
         if (endSentence)
             code.append( ";" );
         return code.toString();
     }
 
 }
