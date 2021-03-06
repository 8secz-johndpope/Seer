 package differentiator;
 
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 /**
  * 
  * A lexer takes a string and splits it into tokens that are meaningful to a
  * parser.
  */
 public class Lexer {
     private final String lex;
 
     /**
      * Creates the lexer over the passed string. string can't be {@code null}
      * type
      * 
      * @param string
      *            The string to tokenize.
      */
     public Lexer(String string) {
         this.lex = string;
     }
 
     /**
      * Analyze the input string of the Lexer and return a list of Token No
      * parameter needed, this method uses the input string from Lexer
      * constructor and perform lexical analysis on this string if input string
      * is empty, output
      * 
      * @return ArrayList of type Token
      */
     private ArrayList<Token> lexAnalysis() {
         // Hold the tokens
         ArrayList<Token> tokens = new ArrayList<Token>();
         // appended all regex into one String
         StringBuffer regex = new StringBuffer();
         for (Token.Type type : Token.Type.values()) {
             regex.append("|" + type.getPattern());
         }
         System.out.println(regex.toString());
         // created pattern from regex and removing the first |
         Pattern pattern = Pattern.compile(new String(regex.substring(1)));
         Matcher matcher = pattern.matcher(this.lex);
         while (matcher.find()) {
             for (Token.Type j : Token.Type.values()) {
                 if (matcher.group().matches(j.getPattern())) {
                     tokens.add(new Token(j, matcher.group()));
                 }
             }
         }
         return tokens;
 
     }
 
     /**
      * Check basic invalid expression such as missing paren, unbalanced paren or
      * any invalid Token
      * */
     private ArrayList<Token> checkValidExpression(ArrayList<Token> tokens)
             throws IllegalArgumentException, RuntimeException {
         if (!tokens.get(0).getType().equals(Token.Type.LEFTPAREN))
             throw new RuntimeException("Missing outer most parenthesis");
         
         Iterator<Token> checkValid = tokens.iterator();
         // unbalance paren if the count of parenthesis goes to negative number
         int parenCount = 0;
         while (checkValid.hasNext()) {
             if (parenCount < 0)
                 throw new RuntimeException("Unbalanced Paren");
             Token currentToken = checkValid.next();
             if (currentToken.getType().equals(Token.Type.LEFTPAREN))
                 ++parenCount;
             else if (currentToken.getType().equals(Token.Type.RIGHTPAREN))
                 --parenCount;
             else if (currentToken.getType().equals(Token.Type.INVALID))
                 throw new IllegalArgumentException("INVALID ARGUMENT");
 
         }
         System.out.println(parenCount);
         if (parenCount != 0)
             throw new RuntimeException("Unbalanced Paren");
         return tokens;
 
     }
 
     /**
      * wrapper class to wrap the valid expression
      * 
      * @return ArrayList of type Token
      * */
 
     public ArrayList<Token> wrapperLexer() {
         return this.checkValidExpression(this.lexAnalysis());
     }
 
    public static void main(String[] args) {
        // Lexer lexer = new
        // Lexer("((3 * (x + 2.4) + (2*x)*x)) + ((1*x)*(x-/x)))) + 3x - 3!");
        Lexer lexer = new Lexer("(3.8 * 3 + 4 +  1000.1)");
        ArrayList<Token> tokens = lexer.wrapperLexer();

        System.out.println(tokens.toString());
        ArrayList<Token> expectedOutput = new ArrayList<Token>();
        expectedOutput.add(new Token(Token.Type.LEFTPAREN, "("));
        expectedOutput.add(new Token(Token.Type.NUMERIC, "3"));
        expectedOutput.add(new Token(Token.Type.PROD, ""));
        expectedOutput.add(new Token(Token.Type.NUMERIC, "4"));
        expectedOutput.add(new Token(Token.Type.RIGHTPAREN, ")"));
        System.out.println(tokens.size());
        System.out.println(expectedOutput.size());
        int i = 0;
        // for (Token token : tokens) {
        // System.out.println(token.equals(expectedOutput.get(i++)));
        //
        // }
    }

 }
