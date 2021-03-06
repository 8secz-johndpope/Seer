 /*******************************************************************************
  * This files was developed for CS4233: Object-Oriented Analysis & Design.
  * The course was taken at Worcester Polytechnic Institute.
  *
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *******************************************************************************/
 package strategy.game.version.beta.move;
 
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.Map;
 
 import strategy.common.PlayerColor;
 import strategy.common.StrategyException;
 import strategy.game.common.Coordinate;
 import strategy.game.common.Location;
 import strategy.game.common.Location2D;
 import strategy.game.common.MoveResult;
 import strategy.game.common.MoveResultStatus;
 import strategy.game.common.Piece;
 import strategy.game.common.PieceLocationDescriptor;
 import strategy.game.common.PieceType;
 import strategy.game.version.board.BoardRuleSet;
 import strategy.game.version.combat.CombatResult;
 import strategy.game.version.combat.CombatRuleSet;
 import strategy.game.version.move.MoveRuleSet;
 
 /**
  * Constructor for BetaMoveRuleSet
  * @author rjsmieja, jrspicola
  * @version Sept 21, 2013
  */
 public class BetaMoveRuleSet implements MoveRuleSet {
 
 	private static final int MAX_TOTAL_MOVES = 12;
 	private static final int MAX_MOVE_DISTANCE = 1;
 
 	final private Collection<PieceType> unmovablePieces;
 	final private Collection<PieceType> validPieceTypes;
 
 	final private CombatRuleSet combatRules;
 	final private BoardRuleSet boardRules;
 
 	private int moveCount;
 	private PlayerColor turnPlayer;
 
 	/**
 	 * Constructor for BetaMoveRuleSet
 	 * @param combatRules ComatRuleSet to follow
 	 * @param boardRules BoardRuleSet to follow
 	 */
 	public BetaMoveRuleSet(CombatRuleSet combatRules, BoardRuleSet boardRules){
 		unmovablePieces = new ArrayList<PieceType>();
 		unmovablePieces.add(PieceType.FLAG);
 
 		validPieceTypes = new ArrayList<PieceType>();
 		validPieceTypes.add(PieceType.MARSHAL);
 		validPieceTypes.add(PieceType.COLONEL);
 		validPieceTypes.add(PieceType.CAPTAIN);
 		validPieceTypes.add(PieceType.LIEUTENANT);
 		validPieceTypes.add(PieceType.SERGEANT);
 		validPieceTypes.add(PieceType.FLAG);
 
 		this.combatRules = combatRules;
 		this.boardRules = boardRules;
 
 		moveCount = 0;
 		turnPlayer = PlayerColor.RED;
 	}
 
 	@Override
 	public void reset(){
 		moveCount = 0;
 		turnPlayer = PlayerColor.RED;
 	}
 
 	@Override
 	public MoveResult doMove(PieceType movingType, PieceLocationDescriptor movingPiece, PieceLocationDescriptor targetPiece, Collection<PieceLocationDescriptor> redConfiguration, Collection<PieceLocationDescriptor> blueConfiguration) throws StrategyException
 	{
 		if (!boardRules.validateLocation(movingPiece)){
 			throw new StrategyException("Error validating the location the piece is moving from");
 		}
 		if (!boardRules.validateLocation(targetPiece)){
 			throw new StrategyException("Error validating the location the piece is moving to");
 		}
 
 		if(movingPiece.getPiece() == null){
 			throw new StrategyException("No piece at location");
 		} 
 
 		if(unmovablePieces.contains(movingPiece.getPiece().getType())){
 			throw new StrategyException("Specified piece type of <" + movingPiece.getPiece().getType() + "> is not allowed to move");
 		}
 
 		if (movingPiece.getPiece().getType() != movingType){
 			throw new StrategyException("Piece type does not match!");
 		}
 
 		final Location from = movingPiece.getLocation();
 		final Location to = targetPiece.getLocation();
 		final int distanceToMove = to.distanceTo(from);
 		final PlayerColor pieceOwner = movingPiece.getPiece().getOwner();
 
 		if (pieceOwner != turnPlayer){
 			throw new StrategyException("Piece does not belong to the current player's turn!");
 		}
 
 		if (distanceToMove > MAX_MOVE_DISTANCE){
 			throw new StrategyException("Moving too many spaces");
 		}
 
 		if (distanceToMove == 0){
 			throw new StrategyException("Not moving");
 		}
 
 		moveCount++;
 
 		//check 'to' spot. if no piece there, make move. else, check piece color.
 		if (targetPiece.getPiece() != null){
 			if (movingPiece.getPiece().getOwner() == targetPiece.getPiece().getOwner()){
 				throw new StrategyException("Attempting to do combat on friendly piece!");
 			}
 			//if color is friend, throw exception. if color is enemy, battle
 			if(movingPiece.getPiece().getType().equals(targetPiece.getPiece().getType())) {
 				//remove both pieces
 				removePiece(movingPiece, redConfiguration, blueConfiguration);
 				removePiece(targetPiece, redConfiguration, blueConfiguration);
 			} else {
 				final PieceLocationDescriptor movedPiece = new PieceLocationDescriptor(movingPiece.getPiece(), targetPiece.getLocation());
 				final CombatResult combatResult = combatRules.doCombat(movedPiece, targetPiece);
 
 				if (combatResult.getLosingPiece().getPiece().getType() == PieceType.FLAG){
 					return new MoveResult(MoveResultStatus.valueOf(combatResult.getWinningPiece().getPiece().getOwner() + "_WINS"), combatResult.getWinningPiece());
 				} else if (moveCount < MAX_TOTAL_MOVES) {
 					return new MoveResult(MoveResultStatus.OK, combatResult.getWinningPiece());
 				}
 			}
 		} else {
 			movePiece(movingPiece, to, redConfiguration, blueConfiguration);
 		}
 
 		if (moveCount >= MAX_TOTAL_MOVES){
 			return new MoveResult(MoveResultStatus.DRAW, null);
 		}
 
 		//Next player's turn
 		if (turnPlayer == PlayerColor.RED){
 			turnPlayer = PlayerColor.BLUE;
 		} else {
 			turnPlayer = PlayerColor.RED;
 		}
 
 		return new MoveResult(MoveResultStatus.OK, null);
 	}
 
 	@Override
 	public boolean canMove(Collection<PieceLocationDescriptor> redConfiguration, Collection<PieceLocationDescriptor> blueConfiguration){

		//		boolean canMove = true;

 		final Map<Location, Piece> locationPieceMap = new HashMap<Location, Piece>();
 
 		for (PieceLocationDescriptor piece : redConfiguration){
 			locationPieceMap.put(piece.getLocation(), piece.getPiece());
 		}
 
 		for (PieceLocationDescriptor piece : blueConfiguration){
 			locationPieceMap.put(piece.getLocation(), piece.getPiece());
 		}
 
		final Map<Location, Piece> edgePieces = new HashMap<Location, Piece>();
 
 		//Get all moveable edge Pieces, aka those that have a space next to them, and that belong to the current turn player
 		for (Location location : locationPieceMap.keySet()){
 			Piece piece = locationPieceMap.get(location);
			if (isEdgePiece(locationPieceMap, location) && !(unmovablePieces.contains(piece)) && piece.getOwner() == turnPlayer){
				edgePieces.put(location, piece);
 			}
 		}
 
		if(edgePieces.isEmpty()){
			return false;
		}

 		//TODO: MOVE HISTORY HERE
		
		return true;
 	}
 
 	/**
 	 * Helper to actually move a piece to an empty location
 	 * @param piece Piece to move
 	 * @param from Location to move piece from
 	 * @param to Location to move piece to
 	 */
 	private void movePiece(PieceLocationDescriptor oldPiece, Location to, Collection<PieceLocationDescriptor> redConfiguration, Collection<PieceLocationDescriptor> blueConfiguration){
 		final PieceLocationDescriptor newPiece = new PieceLocationDescriptor(oldPiece.getPiece(), to);
 
 		if(oldPiece.getPiece().getOwner() == PlayerColor.RED){
 			redConfiguration.remove(oldPiece);
 			redConfiguration.add(newPiece);
 		}else if (oldPiece.getPiece().getOwner() == PlayerColor.BLUE){
 			blueConfiguration.remove(oldPiece);
 			blueConfiguration.add(newPiece);
 		}
 	}
 
 	/**
 	 * Helper to remove piece from the passed in configurations
 	 * @param piece the pieces to remove
 	 * @param configurations configurations remove the piece from
 	 */
 	@SafeVarargs
 	static private void removePiece(PieceLocationDescriptor piece, Collection<PieceLocationDescriptor>... configurations) {
 		for (Collection<PieceLocationDescriptor> configuration: configurations){
 			configuration.remove(piece);
 		}
 	}
 
 	/**
 	 * Helper to determine if a piece has an empty space next to it in +/- 1 in the X and Y directions
 	 * @param pieces HashMap of Location and Pieces to check
 	 * @param location Location to determine edge pieces for
 	 * @return true if an edge piece, false otherwise
 	 */
	private boolean isEdgePiece(Map<Location, Piece> pieces, Location location){
 
 		final Location xUp = new Location2D(location.getCoordinate(Coordinate.X_COORDINATE) + 1, location.getCoordinate(Coordinate.Y_COORDINATE));
 		final Location xDown = new Location2D(location.getCoordinate(Coordinate.X_COORDINATE) - 1, location.getCoordinate(Coordinate.Y_COORDINATE));
 		final Location yUp = new Location2D(location.getCoordinate(Coordinate.X_COORDINATE), location.getCoordinate(Coordinate.Y_COORDINATE) + 1);
 		final Location yDown= new Location2D(location.getCoordinate(Coordinate.X_COORDINATE), location.getCoordinate(Coordinate.Y_COORDINATE) - 1);
 
 		if (pieces.containsKey(xUp) && pieces.containsKey(xDown) && pieces.containsKey(yUp) && pieces.containsKey(yDown)){
 			return false;
 		}
 
 		return true;
 	}
 }
