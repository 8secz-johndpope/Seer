 package com.nullprogram.chess;
 
 import com.nullprogram.chess.gui.ChessFrame;
 
 /**
  * Drives a game of chess, given players and a board.
  *
  * This will, in turn, inform players of their turn and wait for them
  * to respond with a move.
  */
 public class Game implements Runnable {
 
     /** Display frame. */
     private ChessFrame frame;
 
     /** The board being used for this game. */
     private Board board;
 
     /** The player playing white. */
     private Player white;
 
     /** The player playing black. */
     private Player black;
 
     /** Whose turn it is right now. */
     private Piece.Side turn;
 
     /** Set to true when the board is in a completed state. */
     private Boolean done;
 
     /**
      * Hidden constructor.
      */
     protected Game() {
     }
 
     /**
      * Create a new game with the given board and players.
      *
      * @param display     display for this game
      * @param gameBoard   the game board
      * @param whitePlayer the player playing white
      * @param blackPlayer the player playing black
      */
     public Game(final ChessFrame display,
                 final Board gameBoard,
                 final Player whitePlayer,
                 final Player blackPlayer) {
         done = false;
         frame = display;
         board = gameBoard;
         white = whitePlayer;
         black = blackPlayer;
         white.setGame(this);
         black.setGame(this);
         turn = Piece.Side.WHITE;
         turnStatus();
         white.setActive(turn);
     }
 
     /**
      * Perform a turn of the game.
      *
      * @param move the move action to take
      */
     public final void move(final Move move) {
         board.move(move);
         if (board.checkmate() || board.stalemate()) {
             if (board.checkmate(Piece.Side.BLACK)) {
                 frame.setStatus("White wins!");
             } else if (board.checkmate(Piece.Side.WHITE)) {
                 frame.setStatus("Black wins!");
             } else {
                 frame.setStatus("Stalemate!");
             }
             frame.endGame();
             done = true;
             return;
         }
         (new Thread(this)).start();
     }
 
     /**
      * The thread that fires off the next player.
      */
    public final void run() {
         switchTurns();
     }
 
     /**
      * Switch the turn variable and inform the player.
      */
     private void switchTurns() {
         if (turn == Piece.Side.WHITE) {
             turn = Piece.Side.BLACK;
             turnStatus();
             black.setActive(turn);
         } else {
             turn = Piece.Side.WHITE;
             turnStatus();
             white.setActive(turn);
         }
     }
 
     /**
      * Try to undo the last move in the game.
      */
     public final void undo() {
         board.undo();
         switchTurns();
         /* Still need to inform active player of this! */
     }
 
     /**
      * Display the current turn status.
      */
     private void turnStatus() {
         String status;
         if (turn == Piece.Side.WHITE) {
             status = "White's turn.";
         } else {
             status = "Black's turn.";
         }
         frame.getProgress().setValue(0);
         frame.setStatus(status);
     }
 }
