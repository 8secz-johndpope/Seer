 /**
  *  Copyright (C) 2002-2008  The FreeCol Team
  *
  *  This file is part of FreeCol.
  *
  *  FreeCol is free software: you can redistribute it and/or modify
  *  it under the terms of the GNU General Public License as published by
  *  the Free Software Foundation, either version 2 of the License, or
  *  (at your option) any later version.
  *
  *  FreeCol is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *  GNU General Public License for more details.
  *
  *  You should have received a copy of the GNU General Public License
  *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
  */
 
 package net.sf.freecol.server.model;
 
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 
 import net.sf.freecol.common.model.Ability;
 import net.sf.freecol.common.model.Building;
 import net.sf.freecol.common.model.BuildingType;
 import net.sf.freecol.common.model.Colony;
 import net.sf.freecol.common.model.Game;
 import net.sf.freecol.common.model.GameOptions;
 import net.sf.freecol.common.model.GoodsType;
 import net.sf.freecol.common.model.Player;
 import net.sf.freecol.common.model.Unit;
 import net.sf.freecol.common.model.UnitType;
 import net.sf.freecol.common.model.WorkLocation;
 import net.sf.freecol.server.FreeColServer;
 import net.sf.freecol.server.ServerTestHelper;
 import net.sf.freecol.server.model.ServerBuilding;
 import net.sf.freecol.util.test.FreeColTestCase;
 import net.sf.freecol.util.test.FreeColTestUtils;
 
 
 public class ServerBuildingTest extends FreeColTestCase {
 
     private static final BuildingType schoolType
         = spec().getBuildingType("model.building.schoolhouse");
     private static final BuildingType townHallType
         = spec().getBuildingType("model.building.townHall");
     private static final BuildingType universityType
         = spec().getBuildingType("model.building.university");
 
     private static final GoodsType foodType
         = spec().getGoodsType("model.goods.food");
 
    private static final GoodsType grainType
        = spec().getGoodsType("model.goods.grain");

     private static final UnitType freeColonistType
         = spec().getUnitType("model.unit.freeColonist");
     private static final UnitType indenturedServantType
         = spec().getUnitType("model.unit.indenturedServant");
     private static final UnitType pettyCriminalType
         = spec().getUnitType("model.unit.pettyCriminal");
     private static final UnitType expertOreMinerType
         = spec().getUnitType("model.unit.expertOreMiner");
     private static final UnitType expertLumberJackType
         = spec().getUnitType("model.unit.expertLumberJack");
     private static final UnitType masterCarpenterType
         = spec().getUnitType("model.unit.masterCarpenter");
     private static final UnitType masterBlacksmithType
         = spec().getUnitType("model.unit.masterBlacksmith");
     private static final UnitType veteranSoldierType
         = spec().getUnitType("model.unit.veteranSoldier");
     private static final UnitType elderStatesmanType
         = spec().getUnitType("model.unit.elderStatesman");
     private static final UnitType colonialRegularType
         = spec().getUnitType("model.unit.colonialRegular");
 
 
     private enum SchoolLevel { SCHOOLHOUSE, COLLEGE, UNIVERSITY };
 
     private Building addSchoolToColony(Game game, Colony colony,
                                        SchoolLevel level) {
         BuildingType schoolType = null;
         switch (level) {
         case SCHOOLHOUSE:
             schoolType = spec().getBuildingType("model.building.schoolhouse");
             break;
         case COLLEGE:
             schoolType = spec().getBuildingType("model.building.college");
             break;
         case UNIVERSITY:
             schoolType = spec().getBuildingType("model.building.university");
             break;
         default:
             fail("Setup error, cannot setup school");
         }
         Building school = new ServerBuilding(colony.getGame(), colony,
                                              schoolType);
         colony.addBuilding(school);
 
         Building townHall = colony.getBuilding(townHallType);
         for (Unit u : new ArrayList<Unit>(school.getUnitList())) {
             u.setLocation(townHall);
         }
 
         return school;
     }
 
     /**
      * Return a colony with a university and 10 elder statesmen
      * @return
      */
     private Colony getUniversityColony() {
         Colony colony = getStandardColony(10);
 
         for (Unit u : colony.getUnitList()) {
             u.setType(elderStatesmanType);
         }
 
         addSchoolToColony(colony.getGame(), colony, SchoolLevel.UNIVERSITY);
         return colony;
     }
 
     private void trainForTurns(Colony colony, int requiredTurns) {
         trainForTurns(colony, requiredTurns, freeColonistType);
     }
 
     private void trainForTurns(Colony colony, int requiredTurns, UnitType unitType) {
         for (int turn = 0; turn < requiredTurns; turn++) {
             ServerTestHelper.newTurn((ServerPlayer) colony.getOwner());
         }
     }
 
     /**
      * Returns a list of all units in this colony of the given type.
      *
      * @param type The type of the units to include in the list. For instance
      *            Unit.EXPERT_FARMER.
      * @return A list of all the units of the given type in this colony.
      */
     private List<Unit> getUnitList(Colony colony, UnitType type) {
         List<Unit> units = new ArrayList<Unit>() ;
         for (Unit unit : colony.getUnitList()) {
             if (type.equals(unit.getType())) {
                 units.add(unit);
             }
         }
         return units;
     }
 
     /**
      * Check that a free colonist can be taught something.
      *
      */
     public void testExpertTeaching() {
         Game game = ServerTestHelper.startServerGame(getTestMap(true));
 
         Colony colony = getStandardColony(4);
 
         Iterator<Unit> units = colony.getUnitIterator();
 
         Unit colonist = units.next();
         colonist.setType(freeColonistType);
 
         Unit lumber = units.next();
         lumber.setType(expertLumberJackType);
 
         Unit black = units.next();
         black.setType(masterBlacksmithType);
 
         Unit ore = units.next();
         ore.setType(expertOreMinerType);
 
         Building school
             = addSchoolToColony(game, colony, SchoolLevel.SCHOOLHOUSE);
         assertTrue(schoolType.hasAbility("model.ability.teach"));
         assertTrue(colony.canTrain(ore));
 
         ore.setLocation(school);
         trainForTurns(colony, ore.getNeededTurnsOfTraining());
         assertEquals(expertOreMinerType, colonist.getType());
     }
 
     public void testCollege() {
         Game game = ServerTestHelper.startServerGame(getTestMap(true));
 
         Colony colony = getStandardColony(8);
 
         Iterator<Unit> units = colony.getUnitIterator();
 
         Unit colonist = units.next();
         colonist.setType(freeColonistType);
 
         Unit blackSmith = units.next();
         blackSmith.setType(masterBlacksmithType);
 
         Building college = addSchoolToColony(game, colony, SchoolLevel.COLLEGE);
         blackSmith.setLocation(college);
         trainForTurns(colony, blackSmith.getNeededTurnsOfTraining());
 
         assertEquals(masterBlacksmithType, colonist.getType());
     }
 
     public void testUniversity() {
         Game game = ServerTestHelper.startServerGame(getTestMap(true));
 
         Colony colony = getStandardColony(10);
         assertEquals(10, colony.getUnitCount());
 
         Iterator<Unit> units = colony.getUnitIterator();
 
         Unit colonist = units.next();
         assertTrue("unit is not in a WorkLocation",
                    colonist.getLocation() instanceof WorkLocation);
         colonist.setType(freeColonistType);
 
         Unit elder = units.next();
         assertTrue("unit is not in a WorkLocation",
                    colonist.getLocation() instanceof WorkLocation);
         elder.setType(elderStatesmanType);
 
         Building university
             = addSchoolToColony(game, colony, SchoolLevel.UNIVERSITY);
         elder.setLocation(university);
         elder.setStudent(colonist);
         trainForTurns(colony, elder.getNeededTurnsOfTraining());
 
         assertEquals(elderStatesmanType, colonist.getType());
     }
 
     /**
      * [ 1616384 ] Teaching
      *
      * One LumberJack and one BlackSmith in a college. 4 Free Colonists, one as
      * LumberJack, one as BlackSmith two as Farmers.
      *
      * After some turns (2 or 3 I don't know) a new LumberJack is ready.
      * Removing the teacher LumberJack replaced by an Ore Miner.
      *
      * Next turn, a new BlackSmith id ready. Removing the teacher BlackSmith
      * replaced by a Veteran Soldier. There is still 2 Free Colonists as Farmers
      * in the Colony.
      *
      * Waiting during more than 8 turns. NOTHING happens.
      *
      * Changing the two Free Colonists by two other Free Colonists.
      *
      * After 2 or 3 turns, a new Ore Miner and a new Veteran Soldier are ready.
      *
      * http://sourceforge.net/tracker/index.php?func=detail&aid=1616384&group_id=43225&atid=435578
      *
      * CO: I think this is a special case of the testSingleGuyTwoTeachers. But
      * since already the TwoTeachersSimple case fails, I think that needs to be
      * sorted out first.
      */
     public void testTrackerBug1616384() {
         Game game = ServerTestHelper.startServerGame(getTestMap(true));
 
         Colony colony = getStandardColony(8);
 
         // Setting the stage...
         Iterator<Unit> units = colony.getUnitIterator();
 
         Unit colonist1 = units.next();
         colonist1.setType(freeColonistType);
 
         Unit colonist2 = units.next();
         colonist2.setType(freeColonistType);
 
         Unit colonist3 = units.next();
         colonist3.setType(freeColonistType);
 
         Unit colonist4 = units.next();
         colonist4.setType(freeColonistType);
 
         Unit lumberjack = units.next();
         lumberjack.setType(expertLumberJackType);
 
         Unit blacksmith = units.next();
         blacksmith.setType(masterBlacksmithType);
 
         Unit veteran = units.next();
         veteran.setType(veteranSoldierType);
 
         Unit ore = units.next();
         ore.setType(expertOreMinerType);
 
         // Build a college...
         Building college = addSchoolToColony(game, colony, SchoolLevel.COLLEGE);
         blacksmith.setLocation(college);
         lumberjack.setLocation(college);
 
         // It should not take more than 16 turns (my guess) to get the whole
         // story over with.
         int maxTurns = 16;
         while (4 == getUnitList(colony, freeColonistType).size()
                && maxTurns-- > 0) {
             ServerTestHelper.newTurn((ServerPlayer) college.getOwner());
         }
         assertEquals(3, getUnitList(colony, freeColonistType).size());
         assertEquals(1, getUnitList(colony, masterBlacksmithType).size());
         assertEquals(2, getUnitList(colony, expertLumberJackType).size());
 
         lumberjack.setLocation(colony.getVacantColonyTileFor(lumberjack, true, foodType));
         ore.setLocation(college);
 
         while (3 == getUnitList(colony, freeColonistType).size() && maxTurns-- > 0) {
             ServerTestHelper.newTurn((ServerPlayer) college.getOwner());
         }
         assertEquals(2, getUnitList(colony, freeColonistType).size());
         assertEquals(2, getUnitList(colony, masterBlacksmithType).size());
 
         blacksmith.setLocation(colony.getVacantColonyTileFor(blacksmith, true, foodType));
         veteran.setLocation(college);
 
         while (2 == getUnitList(colony, freeColonistType).size() && maxTurns-- > 0) {
             ServerTestHelper.newTurn((ServerPlayer) college.getOwner());
         }
         assertEquals(1, getUnitList(colony, freeColonistType).size());
         assertEquals(2, getUnitList(colony, expertOreMinerType).size());
 
         ore.setLocation(colony.getVacantColonyTileFor(ore, true, foodType));
 
         while (1 == getUnitList(colony, freeColonistType).size() && maxTurns-- > 0) {
             ServerTestHelper.newTurn((ServerPlayer) college.getOwner());
         }
         assertEquals(0, getUnitList(colony, freeColonistType).size());
         assertEquals(2, getUnitList(colony, veteranSoldierType).size());
     }
 
     public void testTwoTeachersSimple() {
         spec().getBooleanOption(GameOptions.ALLOW_STUDENT_SELECTION)
             .setValue(false);
         Game game = ServerTestHelper.startServerGame(getTestMap(true));
 
         Colony colony = getStandardColony(10);
         setProductionBonus(colony, 0);
 
         Iterator<Unit> units = colony.getUnitIterator();
 
         Unit colonist1 = units.next();
         colonist1.setType(freeColonistType);
 
         Unit colonist2 = units.next();
         colonist2.setType(freeColonistType);
 
         Unit colonist3 = units.next();
         colonist3.setType(freeColonistType);
 
         Unit colonist4 = units.next();
         colonist4.setType(freeColonistType);
 
         Unit lumber = units.next();
         lumber.setType(expertLumberJackType);
 
         Unit black = units.next();
         black.setType(masterBlacksmithType);
 
         Unit veteran = units.next();
         veteran.setType(veteranSoldierType);
 
         Unit ore = units.next();
         ore.setType(expertOreMinerType);
 
         Building university
             = addSchoolToColony(game, colony, SchoolLevel.UNIVERSITY);
         black.setLocation(university);
         ore.setLocation(university);
 
         assertEquals(6, getUnitList(colony, freeColonistType).size());
         assertEquals(1, getUnitList(colony, masterBlacksmithType).size());
         assertEquals(1, getUnitList(colony, expertOreMinerType).size());
 
         ServerTestHelper.newTurn((ServerPlayer) university.getOwner());
         assertEquals(6, getUnitList(colony, freeColonistType).size());
         assertEquals(1, getUnitList(colony, masterBlacksmithType).size());
         assertEquals(1, getUnitList(colony, expertOreMinerType).size());
 
         ServerTestHelper.newTurn((ServerPlayer) university.getOwner());
         assertEquals(6, getUnitList(colony, freeColonistType).size());
         assertEquals(1, getUnitList(colony, masterBlacksmithType).size());
         assertEquals(1, getUnitList(colony, expertOreMinerType).size());
 
         ServerTestHelper.newTurn((ServerPlayer) university.getOwner());
         assertEquals(6, getUnitList(colony, freeColonistType).size());
         assertEquals(1, getUnitList(colony, masterBlacksmithType).size());
         assertEquals(1, getUnitList(colony, expertOreMinerType).size());
 
         ServerTestHelper.newTurn((ServerPlayer) university.getOwner());
         assertEquals(5, getUnitList(colony, freeColonistType).size());
         assertEquals(1, getUnitList(colony, masterBlacksmithType).size());
         assertEquals(2, getUnitList(colony, expertOreMinerType).size());
 
         ServerTestHelper.newTurn((ServerPlayer) university.getOwner());
         assertEquals(5, getUnitList(colony, freeColonistType).size());
         assertEquals(1, getUnitList(colony, masterBlacksmithType).size());
         assertEquals(2, getUnitList(colony, expertOreMinerType).size());
 
         ServerTestHelper.newTurn((ServerPlayer) university.getOwner());
         assertEquals(4, getUnitList(colony, freeColonistType).size());
         assertEquals(2, getUnitList(colony, masterBlacksmithType).size());
         assertEquals(2, getUnitList(colony, expertOreMinerType).size());
     }
 
 
     /**
      * If there are two teachers, but just one colonist to be taught.
      */
     public void testSingleGuyTwoTeachers() {
         spec().getBooleanOption(GameOptions.ALLOW_STUDENT_SELECTION)
             .setValue(false);
         Game game = ServerTestHelper.startServerGame(getTestMap(true));
 
         Colony colony = getUniversityColony();
         Iterator<Unit> units = colony.getUnitIterator();
 
         Unit colonist = units.next();
         colonist.setType(freeColonistType);
 
         Unit lumberJack = units.next();
         lumberJack.setType(expertLumberJackType);
 
         Unit blackSmith = units.next();
         blackSmith.setType(masterBlacksmithType);
 
         // It should take 4 turns to train an expert lumber jack and 6 to train
         // a blacksmith
         // The lumber jack chould be finished teaching first.
         // But the school works for now as first come first serve
         Building school = colony.getBuilding(universityType);
         blackSmith.setLocation(school);
         lumberJack.setLocation(school);
         assertTrue(colonist.getTeacher() == blackSmith);
         trainForTurns(colony, blackSmith.getNeededTurnsOfTraining());
 
         assertEquals(0, getUnitList(colony, freeColonistType).size());
         assertEquals(1, getUnitList(colony, expertLumberJackType).size());
         assertEquals(2, getUnitList(colony, masterBlacksmithType).size());
     }
 
     /**
      * If there are two teachers of the same kind, but just one colonist to be
      * taught, this should not mean any speed up.
      */
     public void testTwoTeachersOfSameKind() {
         spec().getBooleanOption(GameOptions.ALLOW_STUDENT_SELECTION)
             .setValue(false);
         Game game = ServerTestHelper.startServerGame(getTestMap(true));
 
         Colony colony = getUniversityColony();
 
         Iterator<Unit> units = colony.getUnitIterator();
 
         Unit colonist1 = units.next();
         colonist1.setType(freeColonistType);
 
         Unit lumberjack1 = units.next();
         lumberjack1.setType(expertLumberJackType);
 
         Unit lumberjack2 = units.next();
         lumberjack2.setType(expertLumberJackType);
 
         Building school = colony.getBuilding(universityType);
         lumberjack1.setLocation(school);
         lumberjack2.setLocation(school);
         trainForTurns(colony, lumberjack1.getNeededTurnsOfTraining());
 
         assertEquals(0, getUnitList(colony, freeColonistType).size());
         assertEquals(3, getUnitList(colony, expertLumberJackType).size());
     }
 
     /**
      * If there are two teachers with the same skill level, the first to be put
      * in the school should be used for teaching.
      *
      */
     public void testSingleGuyTwoTeachers2() {
         spec().getBooleanOption(GameOptions.ALLOW_STUDENT_SELECTION)
             .setValue(false);
         Game game = ServerTestHelper.startServerGame(getTestMap(true));
 
         Colony colony = getUniversityColony();
         setProductionBonus(colony, 0);
 
         Iterator<Unit> units = colony.getUnitIterator();
 
         Unit colonist = units.next();
         colonist.setType(freeColonistType);
 
         Unit lumber = units.next();
         lumber.setType(expertLumberJackType);
 
         Unit ore = units.next();
         ore.setType(expertOreMinerType);
 
         // It should take 3 turns to train an expert lumber jack and also 3 to
         // train a ore miner
         // First come first serve, the lumber jack wins.
         Building school = colony.getBuilding(universityType);
         lumber.setLocation(school);
         ore.setLocation(school);
         assertTrue(colonist.getTeacher() == lumber);
 
         assertEquals(1, getUnitList(colony, freeColonistType).size());
         assertEquals(1, getUnitList(colony, expertLumberJackType).size());
         assertEquals(1, getUnitList(colony, expertOreMinerType).size());
 
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
         assertEquals(1, getUnitList(colony, freeColonistType).size());
         assertEquals(1, getUnitList(colony, expertLumberJackType).size());
         assertEquals(1, getUnitList(colony, expertOreMinerType).size());
 
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
         assertEquals(1, getUnitList(colony, freeColonistType).size());
         assertEquals(1, getUnitList(colony, expertLumberJackType).size());
         assertEquals(1, getUnitList(colony, expertOreMinerType).size());
 
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
         assertEquals(1, getUnitList(colony, freeColonistType).size());
         assertEquals(1, getUnitList(colony, expertLumberJackType).size());
         assertEquals(1, getUnitList(colony, expertOreMinerType).size());
 
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
         assertEquals(0, getUnitList(colony, freeColonistType).size());
         assertEquals(2, getUnitList(colony, expertLumberJackType).size());
         assertEquals(1, getUnitList(colony, expertOreMinerType).size());
     }
 
     /**
      * Test that an petty criminal becomes an indentured servant
      */
     public void testTeachPettyCriminals() {
         spec().getBooleanOption(GameOptions.ALLOW_STUDENT_SELECTION)
             .setValue(false);
         Game game = ServerTestHelper.startServerGame(getTestMap(true));
 
         Colony colony = getUniversityColony();
         Building school = colony.getBuilding(schoolType);
 
         Iterator<Unit> units = colony.getUnitIterator();
 
         Unit criminal = units.next();
         criminal.setType(pettyCriminalType);
 
         Unit teacher = units.next();
         teacher.setType(expertOreMinerType);
 
         teacher.setLocation(school);
         assertTrue(criminal.canBeStudent(teacher));
 
         // PETTY_CRIMINALS become INDENTURED_SERVANTS
         trainForTurns(colony, teacher.getNeededTurnsOfTraining(), pettyCriminalType);
         assertEquals(0, getUnitList(colony, pettyCriminalType).size());
         assertEquals(indenturedServantType, criminal.getType());
     }
 
     /**
      * The time to teach somebody does not depend on the one who is being
      * taught, but on the teacher.
      */
     public void testTeachPettyCriminalsByMaster() {
         spec().getBooleanOption(GameOptions.ALLOW_STUDENT_SELECTION)
             .setValue(false);
         Game game = ServerTestHelper.startServerGame(getTestMap(true));
 
         Colony colony = getUniversityColony();
         Building school = colony.getBuilding(schoolType);
         setProductionBonus(colony, 0);
 
         Iterator<Unit> units = colony.getUnitIterator();
 
         Unit criminal = units.next();
         criminal.setType(pettyCriminalType);
 
         Unit teacher = units.next();
         teacher.setType(masterBlacksmithType);
 
         teacher.setLocation(school);
 
         assertEquals(teacher.getNeededTurnsOfTraining(), 4);
         trainForTurns(colony, teacher.getNeededTurnsOfTraining(), pettyCriminalType);
         assertEquals(indenturedServantType, criminal.getType());
     }
 
     /**
      * Test that an indentured servant becomes a free colonist
      *
      */
     public void testTeachIndenturedServants() {
         spec().getBooleanOption(GameOptions.ALLOW_STUDENT_SELECTION)
             .setValue(false);
         Game game = ServerTestHelper.startServerGame(getTestMap(true));
 
         Colony colony = getUniversityColony();
         setProductionBonus(colony, 0);
         Building school = colony.getBuilding(schoolType);
 
         Iterator<Unit> units = colony.getUnitIterator();
 
         Unit indenturedServant = units.next();
         indenturedServant.setType(indenturedServantType);
 
         Unit teacher = units.next();
         teacher.setType(masterBlacksmithType);
 
         teacher.setLocation(school);
         assertEquals(teacher.getNeededTurnsOfTraining(), 4);
         trainForTurns(colony, teacher.getNeededTurnsOfTraining(),
                       indenturedServantType);
         // Train to become free colonist
         assertEquals(freeColonistType, indenturedServant.getType());
     }
 
     /**
      * Progress in teaching is bound to the teacher and not the learner.
      *
      * Moving students around does not slow education. This behavior is
      * there to simplify gameplay.
      */
     public void testTeacherStoresProgress() {
         spec().getBooleanOption(GameOptions.ALLOW_STUDENT_SELECTION)
             .setValue(false);
         Game game = ServerTestHelper.startServerGame(getTestMap(true));
 
         Colony outsideColony = getStandardColony(1, 10, 8);
         Iterator<Unit> outsideUnits = outsideColony.getUnitIterator();
         Unit outsider = outsideUnits.next();
         outsider.setType(freeColonistType);
 
         Colony colony = getUniversityColony();
         setProductionBonus(colony, 0);
         Building school = colony.getBuilding(universityType);
         Iterator<Unit> units = colony.getUnitIterator();
         Unit student = units.next();
         student.setType(freeColonistType);
         Unit teacher = units.next();
         teacher.setType(expertOreMinerType);
         teacher.setLocation(school);
 
         // Train to become free colonist
         trainForTurns(colony, teacher.getNeededTurnsOfTraining() - 1);
 
         // We swap the colonist with another one
         student.setLocation(outsideColony);
         outsider.setLocation(colony);
 
         assertEquals(1, getUnitList(colony, freeColonistType).size());
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
         assertEquals(0, getUnitList(colony, freeColonistType).size());
         assertEquals(expertOreMinerType, outsider.getType());
     }
 
     /**
      * Progress in teaching is bound to the teacher and not the learner.
      *
      * Moving a teacher inside the colony should not reset its training.
      */
     public void testMoveTeacherInside() {
         spec().getBooleanOption(GameOptions.ALLOW_STUDENT_SELECTION)
             .setValue(false);
         Game game = ServerTestHelper.startServerGame(getTestMap(true));
 
         Colony colony = getUniversityColony();
         setProductionBonus(colony, 0);
         Building school = colony.getBuilding(schoolType);
 
         Iterator<Unit> units = colony.getUnitIterator();
         Unit colonist = units.next();
         colonist.setType(freeColonistType);
         Unit criminal = units.next();
         criminal.setType(pettyCriminalType);
 
         Unit teacher1 = units.next();
         teacher1.setType(expertOreMinerType);
         Unit teacher2 = units.next();
         teacher2.setType(masterCarpenterType);
 
         // The carpenter is set in the school before the miner.
         // In this case, the colonist will become a miner (and the criminal
         // will become a servant).
         teacher2.setLocation(school);
         teacher1.setLocation(school);
         assertEquals(4, teacher1.getNeededTurnsOfTraining());
         assertEquals(4, teacher2.getNeededTurnsOfTraining());
 
         // wait a little
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
         assertEquals(2, teacher1.getTurnsOfTraining());
         assertEquals(2, teacher2.getTurnsOfTraining());
         assertEquals(1, getUnitList(colony, freeColonistType).size());
         assertEquals(1, getUnitList(colony, pettyCriminalType).size());
         assertEquals(1, getUnitList(colony, expertOreMinerType).size());
         assertEquals(1, getUnitList(colony, masterCarpenterType).size());
 
         // Now we want the colonist to be a carpenter. We just want to
         // shuffle the teachers.
         teacher2.setLocation(colony.getVacantColonyTileFor(teacher2, true, foodType));
         // outside the colony is still considered OK (same Tile)
         teacher1.putOutsideColony();
 
         assertNull(teacher1.getStudent());
         assertNull(teacher2.getStudent());
 
         // Passing a turn outside school does not reset training at this time
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
         assertEquals(2, teacher1.getTurnsOfTraining());
         assertEquals(2, teacher2.getTurnsOfTraining());
         assertEquals(1, getUnitList(colony, freeColonistType).size());
         assertEquals(1, getUnitList(colony, pettyCriminalType).size());
 
         // Move teachers back to school, miner first to pick up the criminal
         teacher1.setLocation(school);
         teacher2.setLocation(school);
 
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
         assertEquals(3, teacher1.getTurnsOfTraining());
         assertEquals(3, teacher2.getTurnsOfTraining());
         assertEquals(1, getUnitList(colony, freeColonistType).size());
         assertEquals(1, getUnitList(colony, pettyCriminalType).size());
 
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
         assertEquals(0, teacher1.getTurnsOfTraining());
         assertEquals(0, teacher2.getTurnsOfTraining());
 
         // Teacher1's student (criminal) should be a servant now
         // Teacher2's student (colonist) should be a carpenter now
         assertEquals(0, getUnitList(colony, freeColonistType).size());
         assertEquals(0, getUnitList(colony, pettyCriminalType).size());
         assertEquals(1, getUnitList(colony, indenturedServantType).size());
         assertEquals(1, getUnitList(colony, expertOreMinerType).size());
         assertEquals(2, getUnitList(colony, masterCarpenterType).size());
     }
 
     public void testCaseTwoTeachersWithDifferentExp() {
         spec().getBooleanOption(GameOptions.ALLOW_STUDENT_SELECTION)
             .setValue(false);
         Game game = ServerTestHelper.startServerGame(getTestMap(true));
 
         Colony colony = getUniversityColony();
         Building school = colony.getBuilding(schoolType);
         Iterator<Unit> units = colony.getUnitIterator();
         Unit colonist = units.next();
         colonist.setType(freeColonistType);
         Unit teacher1 = units.next();
         teacher1.setType(expertOreMinerType);
         Unit teacher2 = units.next();
         teacher2.setType(masterCarpenterType);
 
         // First we let the teacher1 train for 3 turns
         teacher1.setLocation(school);
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
         assertEquals(3, teacher1.getTurnsOfTraining());
 
         // Then teacher2 for 1 turn
         teacher1.setLocation(colony.getVacantColonyTileFor(teacher1, true, foodType));
         teacher2.setLocation(school);
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
         assertEquals(3, teacher1.getTurnsOfTraining());
         assertEquals(1, teacher2.getTurnsOfTraining());
 
         // If we now also add teacher2 to the school, then
         // Teacher1 will still be the teacher in charge
         teacher1.setLocation(school);
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
 
         assertEquals(3, teacher1.getTurnsOfTraining());
         assertEquals(2, teacher2.getTurnsOfTraining());
     }
 
     /**
      * Progress in teaching is bound to the teacher and not the learner.
      *
      * Moving a teacher outside the colony should reset its training.
      */
     public void testMoveTeacherOutside() {
         spec().getBooleanOption(GameOptions.ALLOW_STUDENT_SELECTION)
             .setValue(false);
         Game game = ServerTestHelper.startServerGame(getTestMap(true));
 
         Colony outsideColony = getStandardColony(1, 10, 8);
         Iterator<Unit> outsideUnits = outsideColony.getUnitIterator();
         Unit outsider = outsideUnits.next();
         outsider.setType(freeColonistType);
 
         Colony colony = getUniversityColony();
         setProductionBonus(colony, 0);
         Building school = colony.getBuilding(schoolType);
 
         Iterator<Unit> units = colony.getUnitIterator();
         Unit colonist = units.next();
         colonist.setType(freeColonistType);
         Unit criminal = units.next();
         criminal.setType(pettyCriminalType);
 
         Unit teacher1 = units.next();
         teacher1.setType(expertOreMinerType);
         Unit teacher2 = units.next();
         teacher2.setType(masterCarpenterType);
 
         // The carpenter is set in the school before the miner
         // In this case, the colonist will become a miner (and the criminal
         // will become a servant).
         teacher2.setLocation(school);
         teacher1.setLocation(school);
 
         // wait a little
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
         assertEquals(1, getUnitList(colony, freeColonistType).size());
         assertEquals(1, getUnitList(colony, pettyCriminalType).size());
         assertEquals(1, getUnitList(colony, expertOreMinerType).size());
         assertEquals(1, getUnitList(colony, masterCarpenterType).size());
         assertEquals(2, teacher1.getTurnsOfTraining());
         assertEquals(2, teacher2.getTurnsOfTraining());
 
         // Now we move the teachers somewhere else
         teacher1.setLocation(getGame().getMap().getTile(6, 8));
         teacher2.setLocation(outsideColony.getVacantColonyTileFor(teacher2, true, foodType));
         assertEquals(0, teacher1.getTurnsOfTraining());
         assertEquals(0, teacher2.getTurnsOfTraining());
         assertEquals(1, getUnitList(colony, freeColonistType).size());
         assertEquals(1, getUnitList(colony, pettyCriminalType).size());
         assertEquals(0, getUnitList(colony, expertOreMinerType).size());
         assertEquals(0, getUnitList(colony, masterCarpenterType).size());
 
         // Put them back here
         teacher2.setLocation(school);
         teacher1.setLocation(school);
         assertEquals(0, teacher1.getTurnsOfTraining());
         assertEquals(0, teacher2.getTurnsOfTraining());
         assertEquals(teacher1, colonist.getTeacher());
         assertEquals(teacher2, criminal.getTeacher());
         setProductionBonus(colony, 0);
 
         // Check that 2 new turns aren't enough for training
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
         assertEquals(1, getUnitList(colony, freeColonistType).size());
         assertEquals(1, getUnitList(colony, pettyCriminalType).size());
         assertEquals(1, getUnitList(colony, expertOreMinerType).size());
         assertEquals(1, getUnitList(colony, masterCarpenterType).size());
 
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
         assertEquals(1, getUnitList(colony, freeColonistType).size());
         assertEquals(1, getUnitList(colony, pettyCriminalType).size());
         assertEquals(1, getUnitList(colony, expertOreMinerType).size());
         assertEquals(1, getUnitList(colony, masterCarpenterType).size());
 
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
         assertEquals(1, getUnitList(colony, freeColonistType).size());
         assertEquals(1, getUnitList(colony, pettyCriminalType).size());
         assertEquals(1, getUnitList(colony, expertOreMinerType).size());
         assertEquals(1, getUnitList(colony, masterCarpenterType).size());
 
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
         assertEquals(0, getUnitList(colony, freeColonistType).size());
         assertEquals(0, getUnitList(colony, pettyCriminalType).size());
         assertEquals(1, getUnitList(colony, indenturedServantType).size());
         assertEquals(2, getUnitList(colony, expertOreMinerType).size());
         assertEquals(1, getUnitList(colony, masterCarpenterType).size());
     }
 
     /**
      * Sons of Liberty should not influence teaching.
      */
     public void testSonsOfLiberty() {
         spec().getBooleanOption(GameOptions.ALLOW_STUDENT_SELECTION)
             .setValue(false);
         Game game = ServerTestHelper.startServerGame(getTestMap(true));
 
         Colony colony = getUniversityColony();
         Building school = colony.getBuilding(schoolType);
         GoodsType bellsType = spec().getGoodsType("model.goods.bells");
         colony.addGoods(bellsType, 10000);
         ServerTestHelper.newTurn((ServerPlayer) colony.getOwner());
 
         Iterator<Unit> units = colony.getUnitIterator();
 
         Unit colonist1 = units.next();
         colonist1.setType(freeColonistType);
 
         Unit lumberjack = units.next();
         lumberjack.setType(expertLumberJackType);
         lumberjack.setLocation(school);
         trainForTurns(colony, lumberjack.getNeededTurnsOfTraining());
 
         assertEquals(0, getUnitList(colony, freeColonistType).size());
         assertEquals(2, getUnitList(colony, expertLumberJackType).size());
     }
 
     /**
      * Trains partly one colonist then put another teacher.
      *
      * Should not save progress but start all over.
      */
     public void testPartTraining() {
         spec().getBooleanOption(GameOptions.ALLOW_STUDENT_SELECTION)
             .setValue(false);
         Game game = ServerTestHelper.startServerGame(getTestMap(true));
 
         Colony colony = getUniversityColony();
         Building school = colony.getBuilding(schoolType);
 
         Iterator<Unit> units = colony.getUnitIterator();
 
         Unit colonist = units.next();
         colonist.setType(freeColonistType);
 
         Unit lumberjack = units.next();
         lumberjack.setType(expertLumberJackType);
 
         Unit miner = units.next();
         miner.setType(expertOreMinerType);
 
         // Put Lumberjack in School
         lumberjack.setLocation(school);
         ServerTestHelper.newTurn((ServerPlayer) colony.getOwner());
         assertTrue(lumberjack.getStudent() == colonist);
         assertTrue(colonist.getTeacher() == lumberjack);
         ServerTestHelper.newTurn((ServerPlayer) colony.getOwner());
 
         // After 2 turns replace by miner. Progress starts from scratch.
         lumberjack.setLocation(colony.getVacantColonyTileFor(lumberjack, true, foodType));
         assertTrue(lumberjack.getStudent() == null);
         assertTrue(colonist.getTeacher() == null);
 
         miner.setLocation(school);
         ServerTestHelper.newTurn((ServerPlayer) colony.getOwner());
         assertEquals(miner.getStudent(), colonist);
         assertEquals(colonist.getTeacher(), miner);
         trainForTurns(colony, miner.getNeededTurnsOfTraining());
 
         assertEquals(0, getUnitList(colony, freeColonistType).size());
         assertEquals(2, getUnitList(colony, expertOreMinerType).size());
     }
 
     /**
      * Test that free colonists are trained before indentured servants, which
      * are preferred to petty criminals.
      */
     public void testTeachingOrder() {
         spec().getBooleanOption(GameOptions.ALLOW_STUDENT_SELECTION)
             .setValue(false);
         Game game = ServerTestHelper.startServerGame(getTestMap(true));
 
         Colony colony = getUniversityColony();
         setProductionBonus(colony, 0);
         Building school = colony.getBuilding(schoolType);
 
         Iterator<Unit> units = colony.getUnitIterator();
 
         Unit colonist = units.next();
         colonist.setType(freeColonistType);
 
         Unit indenturedServant = units.next();
         indenturedServant.setType(indenturedServantType);
 
         Unit criminal = units.next();
         criminal.setType(pettyCriminalType);
 
         Unit teacher = units.next();
         teacher.setType(expertOreMinerType);
         teacher.setLocation(school);
 
         assertTrue(colonist.canBeStudent(teacher));
         assertTrue(indenturedServant.canBeStudent(teacher));
         assertTrue(criminal.canBeStudent(teacher));
 
         // Criminal training
         assertEquals(teacher, criminal.getTeacher());
         assertEquals(criminal, teacher.getStudent());
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
         assertEquals(0, getUnitList(colony, pettyCriminalType).size());
         assertEquals(indenturedServantType, criminal.getType());
         criminal.setLocation(getGame().getMap().getTile(10,8));
 
         // Servant training
         assertNull(teacher.getStudent());
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
         assertEquals(teacher, indenturedServant.getTeacher());
         assertEquals(indenturedServant, teacher.getStudent());
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
         assertEquals(0, getUnitList(colony, indenturedServantType).size());
         assertEquals(2, getUnitList(colony, freeColonistType).size());
         assertEquals(1, getUnitList(colony, expertOreMinerType).size());
         assertEquals(freeColonistType, indenturedServant.getType());
 
         // Colonist(former servant) training continues
         assertEquals(teacher, indenturedServant.getTeacher());
         assertEquals(indenturedServant, teacher.getStudent());
         assertEquals(colonist.getTeacher(), null);
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
         assertEquals(0, getUnitList(colony, indenturedServantType).size());
         assertEquals(1, getUnitList(colony, freeColonistType).size());
         assertEquals(2, getUnitList(colony, expertOreMinerType).size());
         assertEquals(expertOreMinerType, indenturedServant.getType());
         assertEquals(indenturedServant.getTeacher(), null);
     }
 
     /**
      * Test that an indentured servant cannot be promoted to free colonist and
      * learn a skill at the same time.
      */
     public void testTeachingDoublePromotion() {
         spec().getBooleanOption(GameOptions.ALLOW_STUDENT_SELECTION)
             .setValue(false);
         Game game = ServerTestHelper.startServerGame(getTestMap(true));
 
         Colony colony = getUniversityColony();
         setProductionBonus(colony, 0);
         Building school = colony.getBuilding(schoolType);
 
         Iterator<Unit> units = colony.getUnitIterator();
 
         Unit indenturedServant = units.next();
         indenturedServant.setType(indenturedServantType);
 
         Unit criminal = units.next();
         criminal.setType(pettyCriminalType);
 
         Unit teacher1 = units.next();
         teacher1.setType(expertOreMinerType);
 
         Unit teacher2 = units.next();
         teacher2.setType(expertLumberJackType);
 
         // set location only AFTER all types have been set!
         teacher1.setLocation(school);
         teacher2.setLocation(school);
         assertEquals(criminal, teacher1.getStudent());
         assertEquals(indenturedServant, teacher2.getStudent());
 
         // Training time
         trainForTurns(colony, teacher1.getNeededTurnsOfTraining(), pettyCriminalType);
 
         // indentured servant should have been promoted to free colonist
         // petty criminal should have been promoted to indentured servant
         assertEquals(freeColonistType, indenturedServant.getType());
         assertEquals(indenturedServantType, criminal.getType());
         assertEquals(1, getUnitList(colony, expertOreMinerType).size());
         assertEquals(1, getUnitList(colony, expertLumberJackType).size());
         assertEquals(1, getUnitList(colony, freeColonistType).size());
         assertEquals(1, getUnitList(colony, indenturedServantType).size());
         assertEquals(0, getUnitList(colony, pettyCriminalType).size());
         criminal.setLocation(getGame().getMap().getTile(10,8));
 
         // Train again
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
         assertEquals(1, getUnitList(colony, expertOreMinerType).size());
         assertEquals(2, getUnitList(colony, expertLumberJackType).size());
         assertEquals(0, getUnitList(colony, freeColonistType).size());
         assertEquals(0, getUnitList(colony, indenturedServantType).size());
         assertEquals(0, getUnitList(colony, pettyCriminalType).size());
         assertEquals(expertLumberJackType, indenturedServant.getType());
     }
 
     public void testColonialRegular() {
         spec().getBooleanOption(GameOptions.ALLOW_STUDENT_SELECTION)
             .setValue(false);
         Game game = ServerTestHelper.startServerGame(getTestMap(true));
 
         Colony colony = getStandardColony(10);
         setProductionBonus(colony, 0);
         Player owner = colony.getOwner();
         owner.getFeatureContainer().addAbility(new Ability("model.ability.independenceDeclared"));
 
         Iterator<Unit> units = colony.getUnitIterator();
 
         Unit regular = units.next();
         regular.setType(colonialRegularType);
 
         Building university
             = addSchoolToColony(game, colony, SchoolLevel.UNIVERSITY);
         regular.setLocation(university);
         Unit student = regular.getStudent();
         assertEquals(freeColonistType, student.getType());
         trainForTurns(colony, freeColonistType.getEducationTurns(veteranSoldierType));
 
         assertEquals(veteranSoldierType, student.getType());
     }
 
     public void testConcurrentUpgrade() {
         spec().getBooleanOption(GameOptions.ALLOW_STUDENT_SELECTION)
             .setValue(false);
         Game game = ServerTestHelper.startServerGame(getTestMap(true));
 
         Colony colony = getStandardColony(2);
 
         Iterator<Unit> units = colony.getUnitIterator();
 
         Unit lumber = units.next();
         lumber.setType(expertLumberJackType);
         Unit student = units.next();
         student.setType(pettyCriminalType);
 
         Building school
             = addSchoolToColony(game, colony, SchoolLevel.SCHOOLHOUSE);
         assertTrue(schoolType.hasAbility("model.ability.teach"));
         assertTrue(colony.canTrain(lumber));
         lumber.setLocation(school);
 
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
         assertEquals(student, lumber.getStudent());
 
         // lumber jack can teach indentured servant
         student.setType(indenturedServantType);
         assertEquals(student, lumber.getStudent());
 
         // lumber jack can teach free colonist
         student.setType(freeColonistType);
         assertEquals(student, lumber.getStudent());
 
         // lumber jack can not teach expert
         student.setType(masterCarpenterType);
         assertNull(lumber.getStudent());
         assertNull(student.getTeacher());
     }
 
     public void testProductionBonus() {
         spec().getBooleanOption(GameOptions.ALLOW_STUDENT_SELECTION)
             .setValue(false);
         Game game = ServerTestHelper.startServerGame(getTestMap(true));
 
         Colony colony = getUniversityColony();
         Building school = colony.getBuilding(schoolType);
 
         Iterator<Unit> units = colony.getUnitIterator();
 
         Unit carpenter = units.next();
         carpenter.setType(masterCarpenterType);
         carpenter.setLocation(school);
 
         Unit blacksmith = units.next();
         blacksmith.setType(masterBlacksmithType);
         blacksmith.setLocation(school);
 
         Unit statesman = units.next();
         statesman.setType(elderStatesmanType);
         statesman.setLocation(school);
 
         units.next().setType(freeColonistType);
         units.next().setType(freeColonistType);
         units.next().setType(freeColonistType);
 
         ServerTestHelper.newTurn((ServerPlayer) school.getOwner());
 
         for (int bonus = -2; bonus < 3; bonus++) {
             setProductionBonus(colony, bonus);
             assertEquals(4 - bonus, carpenter.getNeededTurnsOfTraining());
             assertEquals(6 - bonus, blacksmith.getNeededTurnsOfTraining());
             assertEquals(8 - bonus, statesman.getNeededTurnsOfTraining());
         }
     }
 }
