 //----------------------------------------------------------------------------
 // $Id$
 //----------------------------------------------------------------------------
 
 package net.sf.gogui.go;
 
 import java.util.ArrayList;
 
 public final class BoardTest
     extends junit.framework.TestCase
 {
     public static void main(String args[])
     {
         junit.textui.TestRunner.run(suite());
     }
 
     public static junit.framework.Test suite()
     {
         return new junit.framework.TestSuite(BoardTest.class);
     }
 
     public void testBothPassed()
     {
         Board board = new Board(19);
         assertFalse(board.bothPassed());
         board.play(GoPoint.get(0, 0), GoColor.BLACK);
         assertFalse(board.bothPassed());
         board.play(null, GoColor.WHITE);
         assertFalse(board.bothPassed());
         board.play(null, GoColor.BLACK);
         assertTrue(board.bothPassed());
         board.play(null, GoColor.WHITE);
         assertTrue(board.bothPassed());
     }
 
     public void testCapture()
     {
         Board board = new Board(19);
         board.play(GoPoint.get(0, 0), GoColor.BLACK);
         board.play(GoPoint.get(1, 0), GoColor.BLACK);
         board.play(GoPoint.get(0, 1), GoColor.WHITE);
         board.play(GoPoint.get(1, 1), GoColor.WHITE);
         board.play(GoPoint.get(2, 0), GoColor.WHITE);
         assertEquals(GoColor.EMPTY, board.getColor(GoPoint.get(0, 0)));
         assertEquals(GoColor.EMPTY, board.getColor(GoPoint.get(1, 0)));
         assertEquals(GoColor.WHITE, board.getColor(GoPoint.get(0, 1)));
         assertEquals(GoColor.WHITE, board.getColor(GoPoint.get(1, 1)));
         assertEquals(GoColor.WHITE, board.getColor(GoPoint.get(2, 0)));
         assertEquals(2, board.getCapturedBlack());
         assertEquals(0, board.getCapturedWhite());
     }
 
     public void testContains()
     {
         Board board = new Board(19);
         assertTrue(board.contains(GoPoint.get(0, 0)));
         assertTrue(board.contains(GoPoint.get(0, 18)));
         assertTrue(board.contains(GoPoint.get(18, 0)));
         assertTrue(board.contains(GoPoint.get(18, 18)));
         assertFalse(board.contains(GoPoint.get(0, 19)));
         assertFalse(board.contains(GoPoint.get(19, 0)));
         assertFalse(board.contains(GoPoint.get(19, 19)));
         assertFalse(board.contains(GoPoint.get(20, 20)));
     }
 
     /** Test Board.getKilled() */
     public void testGetKilled()
     {
         Board board = new Board(19);
         // 4 . . . . .
         // 3 . . O . .
         // 2 O O @ O .
         // 1 @ @ . . .
         //   A B C D E
         PointList black = new PointList();
         PointList white = new PointList();
         black.add(GoPoint.get(0, 0));
         black.add(GoPoint.get(1, 0));
         black.add(GoPoint.get(2, 1));
         white.add(GoPoint.get(0, 1));
         white.add(GoPoint.get(1, 1));
         white.add(GoPoint.get(2, 2));
         white.add(GoPoint.get(3, 1));
         board.setup(black, white);
         board.play(GoPoint.get(2, 0), GoColor.WHITE);
         ConstPointList killed = board.getKilled();
         assertEquals(3, killed.size());
         assertTrue(killed.contains(GoPoint.get(0, 0)));
         assertTrue(killed.contains(GoPoint.get(1, 0)));
         assertTrue(killed.contains(GoPoint.get(2, 1)));
         board.undo();
         board.setup(null, new PointList(GoPoint.get(3, 0)));
         killed = board.getKilled();
         assertEquals(0, killed.size());
         board.undo();
         board.play(GoPoint.get(3, 0), GoColor.WHITE);
         assertTrue(board.getKilled().isEmpty());
     }
 
     /** Test Board.getSuicide() */
     public void testGetSuicide()
     {
         Board board = new Board(19);
         // 4 . . . .
         // 3 O . . .
         // 2 @ O . .
         // 1 . @ O .
         //   A B C D
         PointList black = new PointList();
         PointList white = new PointList();
         black.add(GoPoint.get(0, 1));
         black.add(GoPoint.get(1, 0));
         white.add(GoPoint.get(0, 2));
         white.add(GoPoint.get(1, 1));
         white.add(GoPoint.get(2, 0));
         board.setup(black, white);
         board.play(GoPoint.get(0, 0), GoColor.BLACK);
         ConstPointList suicide = board.getSuicide();
         assertEquals(3, suicide.size());
         assertTrue(suicide.contains(GoPoint.get(0, 0)));
         assertTrue(suicide.contains(GoPoint.get(0, 1)));
         assertTrue(suicide.contains(GoPoint.get(1, 0)));
         board.undo();
         board.setup(new PointList(GoPoint.get(0, 0)), null);
         assertTrue(board.getSuicide().isEmpty());
     }
 
     public void testGetPositionHashCode1()
     {
         Board board = new Board(19);
         long hashCode = board.getPositionHashCode();
         // Hash code should depend on size
         assertTrue(hashCode != (new Board(9)).getPositionHashCode());
         board.play(GoPoint.get(0, 1), GoColor.BLACK);
         assertTrue(hashCode != board.getPositionHashCode());
         board.undo();
         assertEquals(hashCode, board.getPositionHashCode());
     }
 
     public void testGetPositionHashCode2()
     {
         Board board = new Board(19);
         // 3 . . . .
         // 2 @ O . .
         // 1 . @ O .
         //   A B C D
         PointList black = new PointList();
         PointList white = new PointList();
         black.add(GoPoint.get(0, 1));
         black.add(GoPoint.get(1, 0));
         white.add(GoPoint.get(1, 1));
         white.add(GoPoint.get(2, 0));
         board.setup(black, white);
         long hashCode = board.getPositionHashCode();
         board.play(GoPoint.get(0, 0), GoColor.WHITE);
         assertTrue(hashCode != board.getPositionHashCode());
         board.play(GoPoint.get(1, 0), GoColor.BLACK);
         assertEquals(hashCode, board.getPositionHashCode());
         board.play(Move.getPass(GoColor.BLACK));
         // Hash code should not depend on color to move
         assertEquals(hashCode, board.getPositionHashCode());
     }
 
    /** Test Board.isKo() */
    public void testIsKo()
    {
        Board board = new Board(19);
        // 3 . . . .
        // 2 @ O . .
        // 1 . @ O .
        //   A B C D
        PointList black = new PointList();
        PointList white = new PointList();
        black.add(GoPoint.get(0, 1));
        black.add(GoPoint.get(1, 0));
        white.add(GoPoint.get(1, 1));
        white.add(GoPoint.get(2, 0));
        board.setup(black, white);
        assertFalse(board.isKo(GoPoint.get(0, 0)));
        board.play(GoPoint.get(0, 0), GoColor.WHITE);
        assertTrue(board.isKo(GoPoint.get(1, 0)));
        board.play(GoPoint.get(5, 5), GoColor.BLACK);
        assertFalse(board.isKo(GoPoint.get(1, 0)));
        board.undo();
        assertTrue(board.isKo(GoPoint.get(1, 0)));
    }

     public void testIsSuicide()
     {
         Board board = new Board(19);
         assertFalse(board.isSuicide(GoPoint.get(0, 0), GoColor.WHITE));
         board.play(GoPoint.get(0, 1), GoColor.BLACK);
         assertFalse(board.isSuicide(GoPoint.get(0, 0), GoColor.WHITE));
         board.play(GoPoint.get(1, 1), GoColor.BLACK);
         assertFalse(board.isSuicide(GoPoint.get(0, 0), GoColor.WHITE));
         board.play(GoPoint.get(2, 0), GoColor.BLACK);
         assertFalse(board.isSuicide(GoPoint.get(0, 0), GoColor.WHITE));
         board.play(GoPoint.get(1, 0), GoColor.WHITE);
         assertTrue(board.isSuicide(GoPoint.get(0, 0), GoColor.WHITE));
         board.play(GoPoint.get(0, 0), GoColor.BLACK);
         assertTrue(board.isSuicide(GoPoint.get(1, 0), GoColor.WHITE));
     }
 
     public void testGetLastMove()
     {
         Board board = new Board(19);
         assertNull(board.getLastMove());
         board.play(GoPoint.get(0, 0), GoColor.BLACK);
         assertEquals(Move.get(0, 0, GoColor.BLACK), board.getLastMove());
         board.setup(new PointList(GoPoint.get(1, 1)), null);        
         assertNull(board.getLastMove());
     }
 
     /** Test that playing on a occupied field does not fail.
         Board.play spciefies that a play never fails.
         Also tests that the old stone is correctly restored.
     */
     public void testPlayOnOccupied()
     {
         Board board = new Board(19);
         GoPoint point = GoPoint.get(0, 0);
         board.play(point, GoColor.WHITE);
         board.play(point, GoColor.BLACK);
         board.undo();
         assertEquals(GoColor.WHITE, board.getColor(point));
     }
 
     /** Test that setup does not cause suicide. */
     public void testSetupSuicide()
     {
         Board board = new Board(19);
         board.play(GoPoint.get(1, 0), GoColor.BLACK);
         board.play(GoPoint.get(0, 1), GoColor.BLACK);
         board.setup(null, new PointList(GoPoint.get(0, 0)));
         assertEquals(GoColor.WHITE, board.getColor(GoPoint.get(0, 0)));
     }
 
     /** Test that setup does not capture anything. */
     public void testSetupCapture()
     {
         Board board = new Board(19);
         board.play(GoPoint.get(1, 0), GoColor.BLACK);
         board.play(GoPoint.get(2, 0), GoColor.WHITE);
         board.play(GoPoint.get(0, 1), GoColor.BLACK);
         board.play(GoPoint.get(1, 1), GoColor.WHITE);
         board.setup(null, new PointList(GoPoint.get(0, 0)));
         assertEquals(GoColor.WHITE, board.getColor(GoPoint.get(0, 0)));
     }
 
     /** Test removing a stone. */
     public void testSetupEmpty()
     {
         Board board = new Board(19);
         board.play(GoPoint.get(0, 0), GoColor.WHITE);
         board.setup(null, null, new PointList(GoPoint.get(0, 0)));
         assertEquals(GoColor.EMPTY, board.getColor(GoPoint.get(0, 0)));
         board.undo();
         assertEquals(GoColor.WHITE, board.getColor(GoPoint.get(0, 0)));
     }
 
     public void testSetupHandicap()
     {
         Board board = new Board(19);
         PointList stones = new PointList();
         stones.add(GoPoint.get(4, 4));
         stones.add(GoPoint.get(5, 5));
         board.setupHandicap(stones);
         assertEquals(GoColor.WHITE, board.getToMove());
         assertEquals(GoColor.BLACK, board.getColor(GoPoint.get(4, 4)));
         assertEquals(GoColor.BLACK, board.getColor(GoPoint.get(5, 5)));
         board.undo();
         assertEquals(GoColor.BLACK, board.getToMove());
         assertEquals(GoColor.EMPTY, board.getColor(GoPoint.get(4, 4)));
         assertEquals(GoColor.EMPTY, board.getColor(GoPoint.get(5, 5)));
     }
 
     public void testToMove()
     {
         Board board = new Board(19);
         assertEquals(GoColor.BLACK, board.getToMove());
         board.play(GoPoint.get(0, 0), GoColor.BLACK);
         assertEquals(GoColor.WHITE, board.getToMove());
         // Setup should not change to move
         board.setup(new PointList(GoPoint.get(1, 1)), null);        
         assertEquals(GoColor.WHITE, board.getToMove());
     }
 
     /** Test that a placement from one board can be executed on another board.
         This happens should Board.Placement contain data for restoring
         the state specific to one board.
     */
     public void testTransferPlacement()
     {
         Board board1 = new Board(19);
         Board board2 = new Board(19);
         GoPoint p = GoPoint.get(0, 0);
         PointList list = new PointList(p);
         board1.setup(list, null);
         board1.setup(null, null, list);
         board2.setup(null, list);
         board2.doPlacement(board1.getPlacement(1));
         board1.undo();
         assertEquals(GoColor.BLACK, board1.getColor(p));
     }
 
     public void testUndo()
     {
         Board board = new Board(19);
         board.play(GoPoint.get(0, 0), GoColor.BLACK);
         board.undo();
         assertEquals(GoColor.EMPTY, board.getColor(GoPoint.get(0, 0)));
         assertEquals(GoColor.BLACK, board.getToMove());
     }
 }
 
