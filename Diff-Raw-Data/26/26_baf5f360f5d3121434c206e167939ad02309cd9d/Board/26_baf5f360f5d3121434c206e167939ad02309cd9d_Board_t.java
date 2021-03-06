 import java.util.ArrayList;
 import java.util.Arrays;
 
 public class Board {
 
 	private ArrayList<Piece> piecesOnBoard;
 
 	public static String color = "black";
 
 	public Board (Player[] players) {
 		piecesOnBoard = new ArrayList<Piece>();
 		for (int i=0; i<3; i++) {
 			for (int j=0; j<4; j++) {
 				piecesOnBoard.add(new Piece(new int[] {(2*j+(i%2)), i}, players[0]));
 				piecesOnBoard.add(new Piece(new int[] {7 - 2*j-(i%2), 7-i}, players[1]));
 			}
 		}
 	}
 
 	public Board (Board previousBoard, Move newMove) {
 		this.piecesOnBoard = ArraysHelper.asArrayList(ArraysHelper.deepCopy(previousBoard.getPiecesOnBoard()));
		Player.performMove(new Move(this.getPieceAtLocation(newMove.getSource()), newMove.getWaypoints()), this);
 	}
 
 	public Piece[] getPiecesOnBoard () {
 		return this.piecesOnBoard.toArray(new Piece[this.piecesOnBoard.size()]);
 	}
 
 	public Piece getPieceAtLocation(int[] location) {
 		for (Piece piece : this.piecesOnBoard) {
 			if (piece.getLocation()[0] == location[0] && piece.getLocation()[1] == location[1]) {
 				return piece;
 			}
 		}
 		return null;
 	}
 
 	public void removePiece (Piece pieceToRemove) {
 		piecesOnBoard.remove(pieceToRemove);
 	}
 
 	public int totalPiecesLeft(Player p) {
 		int count = 0;
 		for (Piece piece : piecesOnBoard) {
 			if (piece.getPlayer()==p) {
 				count++;
 			}
 		}
 		return count;
 	}
 
 	public int normalPiecesLeft(Player p) {
 		int count = 0;
 		for (Piece piece : piecesOnBoard) {
 			if (piece.getPlayer()==p && !piece.getIsKing()) {
 				count++;
 			}
 		}
 		return count;
 	}
 
 	public int kingsLeft(Player p) {
 		int count = 0;
 		for (Piece piece : piecesOnBoard) {
 			if (piece.getPlayer()==p && piece.getIsKing()) {
 				count++;
 			}
 		}
 		return count;
 	}
 
 	public static boolean locationIsInBounds (int[] testLocation) {
 		int[] boardValues = new int[] {0,1,2,3,4,5,6,7};
 		if (ArraysHelper.asArrayList(boardValues).contains(testLocation[0]) && ArraysHelper.asArrayList(boardValues).contains(testLocation[1])) {
 			return true;
 		}
 		else {
 			return false;
 		}
 	}
 
 	public double calculateValue(Player p) {
 		double p1Value = 0;
 		double p2Value = 0;
 
 		//iterates over every piece on the board
 		for (Piece piece : this.piecesOnBoard) {
 			//determines if the piece is owned by player 1
 			if (piece.getPlayer()==p) {
 				//determines if the piece is a king
 				if (piece.getIsKing()) {
 					//adds 3 to the player's total value for the board
 					p1Value += 3;
 				//the piece is not a king
 				} else {
 					//determines which side of the board the player is on
 					if (p.getIsOnZeroSide()) {
 						//adds value based on distance down the board
 						p1Value += 1 + 0.125*piece.getLocation()[1];
 					//the player is on the side of the board with index 7
 					} else {
 						//adds value based on distance down the board
 						p1Value += 1 + 0.125*(7 - piece.getLocation()[1]);
 					}
 				}
 			//the piece is owned by player 2
 			} else {
 				//determines if the piece is a king
 				if (piece.getIsKing()) {
 					//adds 3 to the player's total value for the board
 					p2Value += 3;
 				//the piece is not a king
 				} else {
 					//determines which side of the board the player is on
 					if (piece.getPlayer().getIsOnZeroSide()) {
 						//adds value based on distance down the board
 						p2Value += 1 + 0.125*piece.getLocation()[1];
 					//the player is on the side of the board with index 7
 					} else {
 						//adds value based on distance down the board
 						p2Value += 1 + 0.125*(7 - piece.getLocation()[1]);
 					}
 				}
 			}
 		}
 
 		return p1Value/p2Value;
 	}
 }
