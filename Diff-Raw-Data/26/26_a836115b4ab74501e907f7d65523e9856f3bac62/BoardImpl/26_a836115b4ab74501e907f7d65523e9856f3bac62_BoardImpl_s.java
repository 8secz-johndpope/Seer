 package org.kompiro.jamcircle.kanban.model;
 
 import java.beans.PropertyChangeEvent;
 import java.sql.SQLException;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.List;
 
 import org.kompiro.jamcircle.kanban.KanbanStatusHandler;
 import org.kompiro.jamcircle.kanban.model.mock.MockGraphicalEntity;
 
 
 public class BoardImpl extends EntityImpl{
 	
 	private Board board;
 	
 	private List<Card> mockCards = new ArrayList<Card>();
 
 	private List<Lane> mockLanes = new ArrayList<Lane>();
 
 	
 	public BoardImpl(Board board){
 		super(board);
 		this.board = board;
 	}
 	
 	public boolean addCard(Card card){
 		Board board2 = card.getBoard();
 		if(board2 == null || board.getID() != board2.getID()){
 			card.setBoard(board);
 		}
 		card.setLane(null);
 		card.setDeletedVisuals(false);
		if(card instanceof MockGraphicalEntity){
 			mockCards.add(card);
 		}else{
 			card.save();
 			board.getEntityManager().flush(card);
 			try {
 				board.getEntityManager().find(Card.class,Card.PROP_ID + " = ?", card.getID());
 			} catch (SQLException e) {
 				KanbanStatusHandler.fail(e, "SQLException has occured.");
 			}
 		}
 		PropertyChangeEvent event = new PropertyChangeEvent(board,Board.PROP_CARD,null,card);
 		fireEvent(event);
 		return true;
 	}
 
 	public boolean removeCard(Card card){
 		card.setBoard(null);
 		card.setDeletedVisuals(true);
		if(card instanceof MockGraphicalEntity){
 			mockCards.remove(card);
 		}else{
 			card.save();
 			board.getEntityManager().flush(card,board);
 		}
 		PropertyChangeEvent event = new PropertyChangeEvent(board,Board.PROP_CARD,card,null);
 		fireEvent(event);
 		return true;
 	}
 	
 	public boolean containCard(Card card){
 		return board.equals(card.getBoard()) || mockCards.contains(card);
 	}
 	
 	public Card[] getCards(){
 		Collection<Card> allCards = new ArrayList<Card>();
 		allCards.addAll(Arrays.asList(board.getCardsFromDB()));
 		allCards.addAll(mockCards);
 		return allCards.toArray(new Card[]{});
 	}
 	
 	public void clearMocks(){
 		mockCards.clear();
 		mockLanes.clear();
 	}
 	
 	public  boolean addLane(Lane lane){
 		lane.setBoard(board);
 		if(lane instanceof MockGraphicalEntity){
 			mockLanes.add(lane);
 		}else{
 			lane.save();
 			board.getEntityManager().flush(lane,board);
 		}
 		PropertyChangeEvent event = new PropertyChangeEvent(board,Board.PROP_LANE,null,lane);
 		fireEvent(event);
 		return true;
 	}
 
 	public  boolean removeLane(Lane lane){
 		lane.setBoard(null);
		if(lane instanceof MockGraphicalEntity){
 			mockLanes.remove(lane);
 		}else{
 			lane.save();
 			board.getEntityManager().flush(lane,board);
 		}
 		PropertyChangeEvent event = new PropertyChangeEvent(board,Board.PROP_LANE,lane,null);
 		fireEvent(event);
 		return true;		
 	}
 	
 	public Lane[] getLanes(){
 		Collection<Lane> allLanes = new ArrayList<Lane>();
 		allLanes.addAll(Arrays.asList(board.getLanesFromDB()));
 		allLanes.addAll(mockLanes);
 		return allLanes.toArray(new Lane[]{});
 	}
 	
 	@Override
 	public String toString() {
 		return String.format("['#%d':'%s' trashed:'%s']",board.getID(),board.getTitle(),board.isTrashed());
 	}
 	
 }
