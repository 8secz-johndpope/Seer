 package compiler.language.parser;
 
 import compiler.language.ast.ParseInfo;
 import compiler.language.ast.terminal.IntegerLiteral;
 import compiler.language.ast.terminal.Name;
 import compiler.language.ast.terminal.StringLiteral;
 import compiler.parser.BadTokenException;
 import compiler.parser.ParseException;
 import compiler.parser.Parser;
 import compiler.parser.Token;
 import compiler.parser.Tokenizer;
 import compiler.parser.lalr.LALRParserGenerator;
 import compiler.parser.lalr.LALRRuleSet;
 
 /*
  * Created on 30 Jun 2010
  */
 
 /**
  * @author Anthony Bryant
  */
 public class LanguageParser
 {
 
   public static void main(String[] args)
   {
     LALRRuleSet rules = LanguageRules.getRuleSet();
 
     LALRParserGenerator generator = new LALRParserGenerator(rules);
     generator.generate();
 
     Tokenizer tokenizer = new LanguageTokenizer();
     Parser parser = new Parser(generator.getStartState(), tokenizer);
     try
     {
       Token result = parser.parse();
       System.out.println("Success!");
       System.out.println(result.getValue());
     }
     catch (LanguageParseException e)
     {
       printParseError(e.getMessage(), e.getParseInfo());
     }
     catch (ParseException e)
     {
       e.printStackTrace();
     }
     catch (BadTokenException e)
     {
       // TODO: should the error message show a list of expected token types here? they seem to not correspond to what should actually be expected
       Token token = e.getBadToken();
       String message;
       ParseInfo parseInfo;
       if (token == null)
       {
         message = "Unexpected end of input, expected one of: " + buildStringList(e.getExpectedTokenTypes());
         parseInfo = null;
       }
       else
       {
         message = "Unexpected " + token.getType() + ", expected one of: " + buildStringList(e.getExpectedTokenTypes());
         // extract the ParseInfo from the token's value
         // this is simply a matter of casting in most cases, but for literals it must be extracted differently
         if (token.getType() == ParseType.NAME)
         {
           parseInfo = ((Name) token.getValue()).getParseInfo();
         }
         else if (token.getType() == ParseType.INTEGER_LITERAL)
         {
           parseInfo = ((IntegerLiteral) token.getValue()).getParseInfo();
         }
         else if (token.getType() == ParseType.STRING_LITERAL)
         {
           parseInfo = ((StringLiteral) token.getValue()).getParseInfo();
         }
        else
         {
           parseInfo = (ParseInfo) token.getValue();
         }
       }
       printParseError(message, parseInfo);
     }
 
   }
 
   /**
    * Builds a string representing a list of the specified objects, separated by commas.
    * @param objects - the objects to convert to Strings and add to the list
    * @return the String representation of the list
    */
   private static String buildStringList(Object[] objects)
   {
     StringBuffer buffer = new StringBuffer();
     for (int i = 0; i < objects.length; i++)
     {
       buffer.append(objects[i]);
       if (i != objects.length - 1)
       {
         buffer.append(", ");
       }
     }
     return buffer.toString();
   }
 
   /**
    * Prints a parse error with the specified message and representing the location that the ParseInfo stores.
    * @param message - the message to print
    * @param parseInfo - the ParseInfo representing the location in the input where the error occurred, or null if the location is the end of input
    */
   private static void printParseError(String message, ParseInfo parseInfo)
   {
     // make a String representation of the ParseInfo's character range
     String characterRange;
     int startLine = parseInfo.getStartLine();
     int endLine = parseInfo.getEndLine();
     if (startLine == endLine)
     {
       // line:start-end if it is all on one line
       characterRange = startLine + ":";
       int startPos = parseInfo.getStartPos();
       int endPos = parseInfo.getEndPos();
       characterRange += startPos;
       if (startPos < endPos - 1)
       {
         characterRange += "-" + (endPos - 1);
       }
     }
     else
     {
       // startLine-endLine if it spans multiple lines
       characterRange = startLine + "-" + endLine;
     }

     System.err.println(characterRange + ": " + message);
   }
 
 }
