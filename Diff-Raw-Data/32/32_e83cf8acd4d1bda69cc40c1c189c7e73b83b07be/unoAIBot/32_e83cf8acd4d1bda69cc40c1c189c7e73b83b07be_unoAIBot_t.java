 package uno2;
 
 import org.jibble.pircbot.*;
 /*
  * To change this template, choose Tools | Templates
  * and open the template in the editor.
  */
 
 /**
  *
  * @author roofis0
  */
 public class unoAIBot extends PircBot {
    private String[] botOps;
    //private String master = "roofis0";         
     
     public unoAIBot(){
         this.setName("unoAI");
     }  
     
    public void setBotOps(String[] botOps) {
        this.botOps = botOps;
    }
    
    private boolean isBotOp(String nick) {
        for (String i : botOps) {
            if (i.equalsIgnoreCase(nick)) {
                return true;
            }
        }
        return false;
    }
    
     public void playAI(String channel,Player me,Deck deck){
         Card card = null;
         boolean passed = false;
         if(UnoAI.hasPlayable(me, deck)){
             card = UnoAI.getPlayable(me, deck);
         } else {
             sendMessage(channel, "!draw");
             if (UnoAI.hasPlayable(me, deck)) {
                 card = UnoAI.getPlayable(me, deck);
             }else {
                 sendMessage(channel,"!pass");
                 passed = true;
             }
         }
         
         if(card.color.equals(Card.Color.WILD) && !passed){
             sendMessage(channel,"!play " + card.face.toString() + " " + UnoAI.colorMostOf(me, deck).toString());
         }else if (!passed){
             sendMessage(channel,"!play " + card.color.toString() + " " + card.face.toString());
         }
         
     }
     
     
     @Override
     public void onMessage(String channel, String sender, String login, String hostname, String message) {
         String[] Tokens = message.split(" ");        
         //NICK
        if ( Tokens[0].equalsIgnoreCase("!nickai") && this.isBotOp(sender) ) {
             changeNick(Tokens[1]);            
         }
         //HELP
         
         //JOINC
        else if ( Tokens[0].equalsIgnoreCase("!joincai") && this.isBotOp(sender) ) {
             joinChannel( Tokens[1] );
         }
         //QUIT
        else if ( Tokens[0].equalsIgnoreCase("!quit") && this.isBotOp(sender) ) {
             quitServer();
             System.exit(0);
         }
         //UNO
         else if ( Tokens[0].equalsIgnoreCase("!uno")){
                 sendMessage(channel, "!join");            
         }
 }
     
     @Override
     public void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname, String recipientNick, String reason){
         if(recipientNick.equals(this.getNick())){
             this.joinChannel(channel);
   
         }
     }
 
     
 
     
 
     
 
     
 }
