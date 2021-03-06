 package gravityTest;
 
 import gameObjects.Game;
 import terminalGame.TerminalGame;
 import utilities.PublicFunctions;
 
 public class GravityTerminal extends TerminalGame
 {
 	public GravityTerminal()
 	{
 		super();
 	}
 	
 	@Override
 	public void menu()
 	{
		int random = PublicFunctions.getRandomNumberPiece();
 
 		while(true)
 		{			
 			builder.setLength(0);
			
			//Print out the status
			if(random == 8)
			{
				builder.append("The new piece is mystery piece").append("\n");
			}
			else
			{
				builder.append("The new piece is ").append(random).append("\n");
			}
			builder.append(board.toString());
			System.out.println(builder);
			
                        
			//Prompt starts here
 			System.out.println("Where do you want to place the piece?");
 			
 			String position = scanner.nextLine();
 			
 			if(position.equalsIgnoreCase("exit"))
 			{
 				System.out.println("Exiting...");
				//dump stats here
 				System.exit(0);
 			}
 			else if(position.equalsIgnoreCase("help"))
 			{
 				System.out.println("Help here!");
 			}
 			else if(PublicFunctions.isValidPosition(position))
 			{
 				if(!board.insert(Integer.parseInt(position), random))
 				{
 					System.out.println("You lose");
					//dump statistics here
 					System.exit(0);
 				}
 				System.out.println("Inserting piece...");

				//Generate random number for next interation
				random = PublicFunctions.getRandomNumberPiece();
 			}
 			else
 			{
 				System.out.println("Invalid Entry");
 			}
 		}
 	}
 	
 	public static void main(String[] args)
 	{
		Game g = new TerminalGame();
 		g.menu();
 	}
 }
