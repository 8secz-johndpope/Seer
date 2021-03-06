 package org.optaplanner.core.impl.constructionheuristic.placer.entity;
 
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.Iterator;
 import java.util.List;
 
 import org.junit.Test;
 import org.optaplanner.core.impl.heuristic.selector.SelectorTestUtils;
 import org.optaplanner.core.impl.heuristic.selector.entity.EntitySelector;
 import org.optaplanner.core.impl.heuristic.selector.entity.mimic.MimicRecordingEntitySelector;
 import org.optaplanner.core.impl.heuristic.selector.entity.mimic.MimicReplayingEntitySelector;
 import org.optaplanner.core.impl.heuristic.selector.move.MoveSelector;
 import org.optaplanner.core.impl.heuristic.selector.move.generic.ChangeMove;
 import org.optaplanner.core.impl.heuristic.selector.move.generic.ChangeMoveSelector;
 import org.optaplanner.core.impl.heuristic.selector.value.ValueSelector;
 import org.optaplanner.core.impl.move.Move;
 import org.optaplanner.core.impl.phase.AbstractSolverPhaseScope;
 import org.optaplanner.core.impl.phase.step.AbstractStepScope;
 import org.optaplanner.core.impl.solver.scope.DefaultSolverScope;
 import org.optaplanner.core.impl.testdata.domain.TestdataEntity;
 import org.optaplanner.core.impl.testdata.domain.TestdataValue;
 
 import static org.junit.Assert.*;
 import static org.mockito.Mockito.*;
 import static org.optaplanner.core.impl.testdata.util.PlannerAssert.*;
 
 public class QueuedEntityPlacerTest {
 
     @Test
     public void oneMoveSelector() {
        EntitySelector entitySelector = SelectorTestUtils.mockEntitySelector(TestdataEntity.class,
                new TestdataEntity("a"), new TestdataEntity("b"), new TestdataEntity("c"));
        MimicRecordingEntitySelector recordingEntitySelector = new MimicRecordingEntitySelector(
                entitySelector);
         ValueSelector valueSelector = SelectorTestUtils.mockValueSelector(TestdataEntity.class, "value",
                 new TestdataValue("1"), new TestdataValue("2"));
 
         MoveSelector moveSelector = new ChangeMoveSelector(
                new MimicReplayingEntitySelector(recordingEntitySelector),
                 valueSelector,
                 false);
        QueuedEntityPlacer placer = new QueuedEntityPlacer(recordingEntitySelector, Collections.singletonList(moveSelector));
 
         DefaultSolverScope solverScope = mock(DefaultSolverScope.class);
         placer.solvingStarted(solverScope);
 
         AbstractSolverPhaseScope phaseScopeA = mock(AbstractSolverPhaseScope.class);
         when(phaseScopeA.getSolverScope()).thenReturn(solverScope);
         placer.phaseStarted(phaseScopeA);
         Iterator<Placement> placementIterator = placer.iterator();
 
         assertTrue(placementIterator.hasNext());
         AbstractStepScope stepScopeA1 = mock(AbstractStepScope.class);
         when(stepScopeA1.getPhaseScope()).thenReturn(phaseScopeA);
         placer.stepStarted(stepScopeA1);
         assertPlacement(placementIterator.next(), "a", "1", "2");
         placer.stepEnded(stepScopeA1);
 
         assertTrue(placementIterator.hasNext());
         AbstractStepScope stepScopeA2 = mock(AbstractStepScope.class);
         when(stepScopeA2.getPhaseScope()).thenReturn(phaseScopeA);
         placer.stepStarted(stepScopeA2);
         assertPlacement(placementIterator.next(), "b", "1", "2");
         placer.stepEnded(stepScopeA2);
 
         assertTrue(placementIterator.hasNext());
         AbstractStepScope stepScopeA3 = mock(AbstractStepScope.class);
         when(stepScopeA3.getPhaseScope()).thenReturn(phaseScopeA);
         placer.stepStarted(stepScopeA3);
         assertPlacement(placementIterator.next(), "c", "1", "2");
         placer.stepEnded(stepScopeA3);
 
         assertFalse(placementIterator.hasNext());
         placer.phaseEnded(phaseScopeA);
 
         AbstractSolverPhaseScope phaseScopeB = mock(AbstractSolverPhaseScope.class);
         when(phaseScopeB.getSolverScope()).thenReturn(solverScope);
         placer.phaseStarted(phaseScopeB);
         placementIterator = placer.iterator();
 
         assertTrue(placementIterator.hasNext());
         AbstractStepScope stepScopeB1 = mock(AbstractStepScope.class);
         when(stepScopeB1.getPhaseScope()).thenReturn(phaseScopeB);
         placer.stepStarted(stepScopeB1);
         assertPlacement(placementIterator.next(), "a", "1", "2");
         placer.stepEnded(stepScopeB1);
 
         placer.phaseEnded(phaseScopeB);
 
         placer.solvingEnded(solverScope);
 
         verifySolverPhaseLifecycle(entitySelector, 1, 2, 4);
         verifySolverPhaseLifecycle(valueSelector, 1, 2, 4);
     }
 
     private void assertPlacement(Placement placement, String entityCode, String... valueCodes) {
         Iterator<Move> iterator = placement.iterator();
         assertNotNull(iterator);
         for (String valueCode : valueCodes) {
             assertTrue(iterator.hasNext());
             ChangeMove move = (ChangeMove) iterator.next();
             assertCode(entityCode, move.getEntity());
             assertCode(valueCode, move.getToPlanningValue());
         }
         assertFalse(iterator.hasNext());
     }
 
 }
