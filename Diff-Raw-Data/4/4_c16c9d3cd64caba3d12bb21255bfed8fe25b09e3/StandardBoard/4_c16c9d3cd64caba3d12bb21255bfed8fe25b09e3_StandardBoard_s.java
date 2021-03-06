 package com.nullprogram.chess.boards;
 
 import com.nullprogram.chess.Board;
 import com.nullprogram.chess.Piece;
 import com.nullprogram.chess.pieces.*;
 
 public class StandardBoard extends Board {
     static final int WIDTH = 8;
     static final int HEIGHT = 8;
 
     public StandardBoard() {
        super();
         // Set up a standard board
         for (int x = 0; x < WIDTH; x++) {
             setPiece(x, 1, new Pawn(Piece.Side.WHITE));
             setPiece(x, 6, new Pawn(Piece.Side.BLACK));
         }
         setPiece(0, 0, new Rook(Piece.Side.WHITE));
         setPiece(7, 0, new Rook(Piece.Side.WHITE));
         setPiece(0, 7, new Rook(Piece.Side.BLACK));
         setPiece(7, 7, new Rook(Piece.Side.BLACK));
         setPiece(1, 0, new Knight(Piece.Side.WHITE));
         setPiece(6, 0, new Knight(Piece.Side.WHITE));
         setPiece(1, 7, new Knight(Piece.Side.BLACK));
         setPiece(6, 7, new Knight(Piece.Side.BLACK));
         setPiece(2, 0, new Bishop(Piece.Side.WHITE));
         setPiece(5, 0, new Bishop(Piece.Side.WHITE));
         setPiece(2, 7, new Bishop(Piece.Side.BLACK));
         setPiece(5, 7, new Bishop(Piece.Side.BLACK));
         setPiece(3, 0, new Queen(Piece.Side.WHITE));
         setPiece(3, 7, new Queen(Piece.Side.BLACK));
         setPiece(4, 0, new King(Piece.Side.WHITE));
         setPiece(4, 7, new King(Piece.Side.BLACK));
     }
 }
