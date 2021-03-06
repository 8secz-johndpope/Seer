 /**
  * 
  */
 package org.jpacman.test.framework.model;
 
 import org.jpacman.framework.factory.IGameFactory;
 import org.jpacman.framework.factory.UndoableGameFactory;
import org.jpacman.framework.ui.UndoablePacman;
 
 /**
  * @author Rick van Hattem <Rick.van.Hattem@Fawo.nl>
  * 
  */
 public class UndoableGameTest extends GameTest {
 	@Override
 	public IGameFactory makeFactory() {
		return new UndoableGameFactory(new UndoablePacman());
 	}
 }
