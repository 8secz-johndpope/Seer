 package de.htwg.sudoku.controller.impl;
 
 import static org.junit.Assert.assertEquals;
 import static org.junit.Assert.assertTrue;
 
 import org.junit.After;
 import org.junit.Before;
 import org.junit.Test;
 
 import de.htwg.sudoku.model.IGrid;
 import de.htwg.sudoku.model.impl.GridFactory;
 import de.htwg.sudoku.persistence.db4o.GridDb4oDAO;
 
 public class ControllerTest {
 	String newLine = System.getProperty("line.separator");
 
 	private IGrid grid1,grid3;
 	SudokuController controller1,controller3;
 	GridFactory gridFactory;
 	private GridDb4oDAO db4oDao;
 	
 	@Before
 	public void setUp() throws Exception {
 	    gridFactory = new GridFactory();
 	    db4oDao = new GridDb4oDAO();
 		controller1 = new SudokuController(gridFactory,  db4oDao);
 		controller1.resetSize(1);
 		controller3 = new SudokuController(gridFactory,  db4oDao);
 		controller3.resetSize(3);
 		grid3= controller3.getGrid();
 	}
 	
 	@After
 	public void after() {
 		db4oDao.closeDb();
 	}
 
 	@Test
 	public void testSetCell() {
 		
 		controller1.setValue(0, 0, 1);
 		assertEquals(1, grid1.getICell(0, 0).getValue());
 		assertEquals("The cell (0,0) = 1 was successfully set", controller1.getStatus());
 		controller1.setValue(0, 0, 2);
 		assertEquals(1, grid1.getICell(0, 0).getValue());
 		assertEquals("The cell (0,0) = 1 is already set", controller1.getStatus());	
 	}
 	
 	@Test
 	public void testGetGridString() {
 		assertEquals("+---+"+newLine+"|   |"+newLine+"+---+"+newLine,controller1.getGridString());
 	}
 	
 	@Test
 	public void testStatus() {
 		controller1.solve();
 		assertEquals("The Sudoku was solved successfully", controller1.getStatus());
 		controller1.setValue(0, 0, 1);
 		controller1.copy();
 		assertEquals("Copied Sudoku", controller1.getStatus());
 		controller1.paste();
 		assertEquals("Pasted Sudoku", controller1.getStatus());
 	}
 	
 	@Test
 	public void testUndoRedo() {
 		controller1.setValue(0, 0, 1);
 		controller1.undo();
 		assertEquals(0, controller1.getValue(0,0));
 		controller1.redo();
 		assertEquals(1, controller1.getValue(0,0));
 	}
 	
 	@Test
 	public void testX() {
		
 	}
 	@Test
 	public void testGetCellsPerEdge() {
 		assertEquals(1,controller1.getCellsPerRow());
 	}
 	@Test
 	public void testGetBlockSize() {
 		assertEquals(1,controller1.getBlockSize());
 	}
 	@Test
 	public void testHighlight() {
 		controller1.highlight(1);
 		assertTrue(controller1.isHighlighted(0,0));
 	}
 	@Test
 	public void testBlockAt() {
 		assertEquals(0,controller1.blockAt(0, 0));
 	}
 	@Test
 	public void testShowCandidates() {
 		controller1.showCandidates(0, 0);
 		assertTrue(grid1.getICell(0, 0).isShowCandidates());
 	}
 	@Test
 	public void testShowAllCandidates() {
 		controller1.showAllCandidates();
 		assertTrue(grid1.getICell(0, 0).isShowCandidates());
 		assertTrue(controller1.isShowCandidates(0, 0));
 	}
 	@Test
 	public void testIsCandidate() {
 		assertTrue(controller1.isCandidate(0, 0, 1));	
 	}
 	@Test
 	public void testIsSet() {
 		controller1.setValue(0, 0, 1);
 		assertTrue(controller1.isSet(0, 0));	
 	}
 	@Test
 	public void testIsGiven() {
 		controller1.create();
 		assertTrue(controller1.isGiven(0, 0));	
 	}
 	@Test
 	public void testParseStringToGrid() {
 		controller1.parseStringToGrid("1");
 		assertTrue(controller1.isGiven(0, 0));	
 		assertEquals(1,controller1.getValue(0, 0));
 	}
 	
 	@Test
 	public void testCopyPaste() {
 		controller3.create();
 		String orig = controller3.getGrid().toString();
 		controller3.copy();
 		controller3.create();
 		controller3.paste();
 		String copy = controller3.getGrid().toString();
 		assertEquals(orig,copy);
 		
 		
 		
 	}
 
 }
