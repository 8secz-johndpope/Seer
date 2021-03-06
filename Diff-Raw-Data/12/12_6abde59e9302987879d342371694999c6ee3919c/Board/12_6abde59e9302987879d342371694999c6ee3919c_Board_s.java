 import java.util.Arrays;
 
 /**
  * The board to hold the ships
  * The default size is 11x11 which represents the 1:10 and A:J
  * and leaves column zero and row zero blank
  *
  * S represents a ship
  * X represents a hit on a ship
  * O represents a miss
  * * represents an unused space
  */
 public class Board {
     private char[][] board;
     private int boardWidth;
     private int boardHeight;
     private int numShips;
 
     public Board() {
        this(11, 11);
     }
 
     public Board(int boardHeight, int boardWidth) {
        this.boardWidth = boardHeight;
        this.boardHeight = boardWidth;
        this.board = new char[boardHeight][boardWidth];
         boardInit();
     }
 
     /* This assumes an NxM board (i.e. rows are equal lengths)
      * */
     public Board(char[][] board) {
         this.boardWidth = board.length;
         this.boardHeight = board[0].length;
         this.board = board;
         boardInit();
     }
 
     private void boardInit() {
         for(int i = 1; i < this.boardHeight; i++) {
             for(int j = 1; j < this.boardWidth; j++) {
                 board[i][j] = '*';
             }
         }
     }
 
     /**
      * Attempts to place a ship on the board.
      * @param row starting row for the ship
      * @param col starting column for the ship
      * @param shipSize the amount of spaces the ship will occupy
      * @param orientation whether it is horizontal or vertical
      * @return Returns true if the ship was successfully placed, or false
      * if there was an error of some nature. An error message will be printed.
      */
 
     public boolean placeShip(int row, int col, int shipSize, char orientation) {
         /* validate the input values before adding the ship */
         if(!checkAddShipParams(row, col, shipSize, orientation))
             return false;
         /* if someone is in the spot already we can't put another ship there */
         else if(spotTaken(row, col, shipSize, orientation))
             return false;
 
         /* We made it this far so everything must be ok, now place the ship */
         if(orientation == 'v' || orientation == 'V') {
             for(int i = 0; i < shipSize; i++) {
                 this.board[row+i][col] = 'S';
             }
 
             numShips++;
             return true;
         }
         else { // don't need to check if it equals H because it has to if it passed the param check
             for(int i = 0; i < shipSize; i++) {
                 this.board[row][col+i] = 'S';
             }
 
             numShips++;
             return true;
         }
     }
 
     private boolean spotTaken(int row, int col, int shipSize, char orientation) {
         /* if you wanted this could easily be extended to return or print the
          * first index X,Y where there was a conflict */
         boolean spotTaken = false;
         for(int i = 0; i < shipSize; i++) {
             if(this.board[row][col+i] != '*') {
                 System.err.println(String.format("Error: There is already part of a ship at ship at %d, %d", row, col+i));
                 return true;
             }
         }
 
         return false;
     }
 
     /* returns true if the ship fits and everything looks ok
      * returns false and prints an error message otherwise */
     private boolean checkAddShipParams(int row, int col, int shipSize, char orientation) {
         /* check if the coordinates initially make sense */
         if(row < 1 || row > this.boardHeight) {
             System.err.println(String.format("Error: The row must be between 1 and %d", this.boardHeight-1));
             return false;
         }
         else if(col < 1 || col > this.boardWidth) {
             System.err.println(String.format("Error: The column must be between 1 and %d", this.boardWidth-1));
             return false;
         }
         /* is the orientation one we know? */
         else if(orientation != 'h' && orientation != 'H' && orientation != 'v' && orientation != 'V') {
             System.err.println(String.format("Error: Unrecognized orientation '%c'", orientation));
             return false;
         }
         /* will the ship fit on the board with that size and orientation? */
         else if((orientation == 'h' || orientation == 'H') && (col + (shipSize-1) > this.boardWidth)) {
             System.err.println("Error: The ship does not fit on the board there");
             return false;
         }
         else if((orientation == 'v' || orientation == 'V') && (row + (shipSize-1) > this.boardHeight)) {
             System.err.println("Error: The ship does not fit on the board there");
             return false;
         }
 
         /* Everything looks good! */
         return true;
     }
 
     /**
      * Checks if a given shot is a hit or miss.
      * @param row The target row for the shot.
      * @param col The target column for the shot.
      * @return Returns true if the shot was a hit. Returns false if the shot
      * was a miss or if the spot was already targetted */
     public boolean checkShot(int row, int col) {
         /* invalid board position */
         if(row < 1 || col < 1 || row > (this.boardHeight-1) || col > (this.boardWidth-1))
             return false;
 
         /* We have a hit! */
         if(this.board[row][col] == 'S') {
             this.board[row][col] = 'X';
 
             /* check if this was the last part of a ship, if so decrement the
              * number of ships left */
             if(this.board[row+1][col] != 'S' && this.board[row][col+1] != 'S' &&
                this.board[row-1][col] != 'S' && this.board[row][col-1] != 'S') {
                numShips--;
             }
 
             return true;
         }
         /* Did they really shoot at the same spot again? */
         else if(this.board[row][col] == 'X' || this.board[row][col] == 'O') {
             /* do nothing, penalize them for their foolish double shot! */
             return false;
         }
         /* The must have missed then */
         else {
             this.board[row][col] = 'O';
             return false;
         }
     }
 
     public boolean hasShipsLeft() {
         return this.numShips > 0;
     }
 
     public String toString() {
         StringBuilder s = new StringBuilder();
         for(int i = 0; i < this.boardHeight; i++) {
             s.append(Arrays.toString(this.board[i]));
             s.append("\n");
         }
 
         return s.toString();
     }
 }
