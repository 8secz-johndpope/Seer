 package com.philihp.weblabora.model;
 
 import static com.philihp.weblabora.model.TerrainTypeEnum.*;
 
 import java.util.List;
 import java.util.Set;
 
 import com.google.common.collect.ArrayTable;
 import com.google.common.collect.DiscreteDomains;
 import com.google.common.collect.Ranges;
 import com.google.common.collect.Table;
 
 public class CommandBuyPlot implements MoveCommand {
 	
 	/*
 	 * You're right. This would be a lot prettier with closures.
 	 */
 	
 	public static enum Side {
 		COAST(-2, WATER, TerrainTypeEnum.COAST),
 		MOUNTAIN(5, HILLSIDE, TerrainTypeEnum.MOUNTAIN);
 		
 		private int originColumn;
 		private TerrainTypeEnum[] terrains;
 		private Side(int originColumn, TerrainTypeEnum... terrains) {
 			this.originColumn = originColumn;
 			this.terrains = terrains;
 		}
 		public int getOriginColumn() {
 			return originColumn;
 		}
 		public boolean hasType(int y, int row, int column) {
 			return (column == originColumn || column == originColumn+1)
 					&&
 			       (row    == y            || row == y+1);
 		}
 		
 		public TerrainTypeEnum getType(int column) {
 			switch (this) {
 			case COAST:
 				switch (column) {
 				case -2:
 					return terrains[0];
 				case -1:
 					return terrains[1];
 				default:
 					throw new RuntimeException(this
 							+ " does not have a TerrainType for column "
 							+ column);
 				}
 			case MOUNTAIN:
 				switch (column) {
 				case 5:
 					return terrains[0];
 				case 6:
 					return terrains[1];
 				default:
 					throw new RuntimeException(this
 							+ " does not have a TerrainType for column "
 							+ column);
 				}
 			default:
 				throw new RuntimeException("Unknown Plot Side " + this);
 			}
 		}
 	};
 
 	@Override
 	public void execute(Board board, CommandParameters params)
 			throws WeblaboraException {
 		
 		execute(board,
 				Integer.parseInt(params.get(0)),
 				Side.valueOf(params.get(1))
 				);
 	}
 
 	public static void execute(Board board, int y, Side side)
 			throws WeblaboraException {
 		Player player = board.getPlayer(board.getActivePlayer());
 		
 		int cost = board.purchasePlot();
 		if(player.getCoins() < cost)
 			throw new WeblaboraException("Purchase price for a plot is "+cost+", but player "+player.getColor()+" only has "+player.getCoins()+".");
 		player.subtractCoins(cost);
 		
 		Landscape landscape = player.getLandscape();
 		
 		Table<Integer, Integer, Terrain> oldTerrain = landscape.getTerrain();
 
 		checkForOverlap(oldTerrain, y, y, side.getOriginColumn());
 		checkForOverlap(oldTerrain, y, y+1, side.getOriginColumn());
 		checkForConnection(oldTerrain, y, side.getOriginColumn());
 		
 		Set<Integer> oldRows = oldTerrain.rowKeySet();
 		Set<Integer> oldColumns = oldTerrain.columnKeySet();
 		
 		int minRow = y;
 		int maxRow = y+1; //+1 because the Plot is 2 rows tall
 		for(int i : oldRows) {
 			if(i < minRow) minRow = i;
 			if(i > maxRow) maxRow = i;
 		}
 		
 		Set<Integer> newRows = Ranges.closed(minRow, maxRow).asSet(DiscreteDomains.integers());
 		
 		int minColumn = (side==Side.COAST) ? -2 : 0;
 		int maxColumn = (side==Side.COAST) ?  0 : 6;
 		for(int i : oldColumns) {
 			if(i < minColumn) minColumn = i;
 			if(i > maxColumn) maxColumn = i;
 		}
 		
 		Set<Integer> newColumns = Ranges.closed(minColumn, maxColumn).asSet(DiscreteDomains.integers()); 
 		
 		ArrayTable<Integer, Integer, Terrain> newTerrain = ArrayTable.create(newRows, newColumns);
 		for(Integer rowKey : newRows) {
 			for(Integer columnKey : newColumns) {
 				if(oldTerrain.contains(rowKey, columnKey)) {
 					newTerrain.put(rowKey, columnKey, oldTerrain.get(rowKey, columnKey));
 				}
				else if(side.hasType(y, rowKey, columnKey)) {
					newTerrain.put(rowKey, columnKey, new Terrain(landscape, side.getType(columnKey), null));
				}
 			}
 		}
 		
 		landscape.setTerrain(newTerrain);
 		
 	}
 
 	private static void checkForConnection(
 			Table<Integer, Integer, Terrain> oldTerrain, Integer rowKey, Integer columnKey) throws WeblaboraException {
 		if(oldTerrain.contains(rowKey-1, columnKey)==false &&
 		   oldTerrain.contains(rowKey, columnKey+2)==false &&
 		   oldTerrain.contains(rowKey+1, columnKey+2)==false &&
 		   oldTerrain.contains(rowKey+2, columnKey)==false) {
 			throw new WeblaboraException("Cannot put a plot at "+rowKey+", as it does not connect to the rest of the landscape");
 		}
 	}
 
 	private static void checkForOverlap(
 			Table<Integer, Integer, Terrain> oldTerrain, int y, Integer rowKey, Integer columnKey) throws WeblaboraException {
 		if(oldTerrain.contains(rowKey, columnKey)) {
 			throw new WeblaboraException("Cannot put a plot at "+y+", as it would overlap on row "+rowKey);
 		}
 		
 	}
 }
