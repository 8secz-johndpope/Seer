 /*
 
 @ShortLicense@
 
 Authors: @JS@
          @MJL@
 
 Released: @ReleaseDate@
 
  */
 
 package de.jskat.gui.table;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import javax.swing.table.AbstractTableModel;
 
 import de.jskat.util.JSkatResourceBundle;
 import de.jskat.util.Player;
 import de.jskat.util.SkatConstants;
 import de.jskat.util.SkatListMode;
 
 /**
  * Provides a model for the skat list table
  */
 class SkatListTableModel extends AbstractTableModel {
 
 	private static final long serialVersionUID = 1L;
 
 	private JSkatResourceBundle strings;
 
 	private SkatListMode mode = SkatListMode.NORMAL;
 
 	private int playerCount = 3;
 	private List<Player> declarers;
 	private List<List<Integer>> playerResults;
 	private List<Integer> gameResults;
 	private List<String> columns;
 	private List<List<Integer>> displayValues;
 
 	/**
 	 * Constructor
 	 */
 	public SkatListTableModel() {
 
 		strings = JSkatResourceBundle.instance();
 
 		declarers = new ArrayList<Player>();
 		playerResults = new ArrayList<List<Integer>>();
 		gameResults = new ArrayList<Integer>();
 		displayValues = new ArrayList<List<Integer>>();
 		columns = new ArrayList<String>();
 		setColumns();
 	}
 
 	/**
 	 * @see AbstractTableModel#getColumnCount()
 	 */
 	@Override
 	public int getColumnCount() {
 
 		return columns.size();
 	}
 
 	/**
 	 * @see AbstractTableModel#getRowCount()
 	 */
 	@Override
 	public int getRowCount() {
 
 		return declarers.size();
 	}
 
 	/**
 	 * @see AbstractTableModel#getValueAt(int, int)
 	 */
 	@Override
 	public Object getValueAt(int rowIndex, int columnIndex) {
 
 		Object result = null;
 
 		if (displayValues.get(rowIndex).get(columnIndex) != null) {
 			result = displayValues.get(rowIndex).get(columnIndex);
 		} else {
			result = "-"; //$NON-NLS-1$
 		}
 
 		return result;
 	}
 
 	Integer getPlayerValue(int playerColumn, int gameRow) {
 
 		Integer result = null;
 
 		return result;
 	}
 
 	/**
 	 * @see AbstractTableModel#getColumnName(int)
 	 */
 	@Override
 	public String getColumnName(int col) {
 
 		return columns.get(col);
 	}
 
 	/**
	 * @see AbstractTableModel#getColumnClass(int)
	 */
	@Override
	public Class<?> getColumnClass(int col) {

		return Integer.class;
	}

	/**
 	 * Sets the skat list mode
 	 * 
 	 * @param newMode
 	 */
 	void setSkatListMode(SkatListMode newMode) {
 
 		mode = newMode;
 
 		calculateDisplayValues();
 
 		fireTableDataChanged();
 	}
 
 	void calculateDisplayValues() {
 
 		int currResult = 0;
 		List<Integer> playerResultsSoFar = new ArrayList<Integer>();
 
 		displayValues.clear();
 
 		for (int game = 0; game < gameResults.size(); game++) {
 
 			displayValues.add(new ArrayList<Integer>());
 
 			// add player values
 			for (int player = 0; player < playerCount; player++) {
 
 				int previousResult = 0;
 				currResult = 0;
 
 				if (declarers.get(game) != null) {
 
 					if (game == 0) {
 						// set previous player result to 0
 						playerResultsSoFar.add(new Integer(0));
 					} else {
 						// get previous result for player values
 						// from second game on
 						previousResult = playerResultsSoFar.get(player)
 								.intValue();
 					}
 
 					// get player results from current game
 					switch (mode) {
 					case NORMAL:
 						currResult = playerResults.get(player).get(game);
 						break;
 					case TOURNAMENT:
 						boolean isDeclarer = (playerResults.get(player).get(
 								game) != 0);
 						currResult = SkatConstants.getTournamentGameValue(
 								isDeclarer, gameResults.get(game), playerCount);
 						break;
 					case BIERLACHS:
 						// FIXME jan 31.05.2010 add bierlachs value
 						break;
 					}
 				}
 
 				if (currResult != 0) {
 
 					Integer newResult = new Integer(previousResult + currResult);
 					displayValues.get(game).add(newResult);
 					playerResultsSoFar.set(player, newResult);
 
 				} else {
 
 					displayValues.get(game).add(null);
 				}
 			}
 
 			// get game result
 			switch (mode) {
 			case NORMAL:
 			case BIERLACHS:
 				currResult = gameResults.get(game);
 				break;
 			case TOURNAMENT:
 				currResult = SkatConstants.getTournamentGameValue(true,
 						gameResults.get(game), playerCount);
 				break;
 			}
 			displayValues.get(game).add(currResult);
 		}
 	}
 
 	/**
 	 * Adds a game result to the model
 	 * 
 	 * @param leftOpponent
 	 *            Position of the upper left opponent
 	 * @param rightOpponent
 	 *            Position of the upper right opponent
 	 * @param player
 	 *            Position of the player
 	 * @param declarer
 	 *            Position of the game declarer
 	 * @param gameResult
 	 *            Game result
 	 */
 	void addResult(Player leftOpponent, Player rightOpponent, Player player,
 			Player declarer, int gameResult) {
 
 		// FIXME works only on 3 player series
 		declarers.add(declarer);
 		gameResults.add(new Integer(gameResult));
 
 		int declarerColumn = getDeclarerColumn(leftOpponent, rightOpponent,
 				player, declarer);
 
 		if (declarer != null) {
 			playerResults.get(declarerColumn).add(new Integer(gameResult));
 			playerResults.get((declarerColumn + 1) % 3).add(new Integer(0));
 			playerResults.get((declarerColumn + 2) % 3).add(new Integer(0));
 		} else {
 			// game was passed in
 			for (int i = 0; i < playerCount; i++) {
 				playerResults.get(i).add(0);
 			}
 		}
 		calculateDisplayValues();
 
 		fireTableDataChanged();
 	}
 
 	int getDeclarerColumn(Player leftOpponent, Player rightOpponent,
 			Player player, Player declarer) {
 
 		int result = -1;
 
 		if (declarer == leftOpponent) {
 			result = 0;
 		} else if (declarer == rightOpponent) {
 			result = 1;
 		} else if (declarer == player) {
 			result = 2;
 		}
 
 		return result;
 	}
 
 	/**
 	 * Clears the complete list
 	 */
 	void clearList() {
 
 		declarers.clear();
 		for (List<Integer> currList : playerResults) {
 
 			currList.clear();
 		}
 		gameResults.clear();
 		displayValues.clear();
 
 		fireTableDataChanged();
 	}
 
 	public void setPlayerCount(int newPlayerCount) {
 
 		declarers.clear();
 		gameResults.clear();
 
 		playerCount = newPlayerCount;
 
 		setColumns();
 
 		fireTableStructureChanged();
 		fireTableDataChanged();
 	}
 
 	void setColumns() {
 
 		playerResults.clear();
 		displayValues.clear();
 		columns.clear();
 
 		for (int i = 0; i < playerCount; i++) {
 			// FIXME (jan 14.12.2010) get player names
 			columns.add("P" + i);
 			playerResults.add(new ArrayList<Integer>());
 			displayValues.add(new ArrayList<Integer>());
 		}
 		columns.add(strings.getString("games")); //$NON-NLS-1$
 		displayValues.add(new ArrayList<Integer>());
 	}
 }
