package games.strategy.triplea.ai.tree;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.jupiter.api.Assertions.*;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import org.junit.jupiter.api.Test;

class StepUnitsTest {
  private static final GameData data = TestMapGameData.GLOBAL1940.getGameData();
  private static final GamePlayer BRITISH =
      checkNotNull(data.getPlayerList().getPlayerId("British"));
  private static final GamePlayer GERMAN =
      checkNotNull(data.getPlayerList().getPlayerId("Germans"));
  private static final UnitType INFANTRY =
      checkNotNull(data.getUnitTypeList().getUnitType("infantry"));
  private static final UnitType ARMOUR = checkNotNull(data.getUnitTypeList().getUnitType("armour"));
  private static final UnitType ARTILLERY =
      checkNotNull(data.getUnitTypeList().getUnitType("artillery"));
  private static final UnitType BATTLESHIP =
      checkNotNull(data.getUnitTypeList().getUnitType("battleship"));

  @Test
  void testFilterWithNoHits() {
    final List<Unit> attackers = List.of(INFANTRY.create(BRITISH), ARMOUR.create(BRITISH));
    final List<Unit> defenders =
        List.of(INFANTRY.create(GERMAN), ARTILLERY.create(GERMAN), ARMOUR.create(GERMAN));
    final StepUnits units = new StepUnits(attackers, BRITISH, defenders, GERMAN);

    assertEquals(attackers, units.getAliveOrWaitingToDieFriendly());
    assertEquals(defenders, units.getAliveOrWaitingToDieEnemy());
  }

  @Test
  void testFilterWithHits() {
    final List<Unit> attackers = List.of(INFANTRY.create(BRITISH), ARMOUR.create(BRITISH));
    final List<Unit> defenders =
        List.of(INFANTRY.create(GERMAN), ARTILLERY.create(GERMAN), ARMOUR.create(GERMAN));
    final StepUnits units = new StepUnits(attackers, BRITISH, defenders, GERMAN);
    units.hitFriendly(attackers.get(0));
    units.hitEnemy(defenders.get(1));
    final StepUnits actual = units.removeWaitingToDie();

    assertEquals(List.of(attackers.get(1)), actual.getAliveOrWaitingToDieFriendly());
    assertEquals(List.of(defenders.get(0), defenders.get(2)), actual.getAliveOrWaitingToDieEnemy());
  }

  @Test
  void testNoMoreFriendliesWithExistingFriendlies() {
    final List<Unit> attackers = List.of(INFANTRY.create(BRITISH), ARMOUR.create(BRITISH));
    final List<Unit> defenders = List.of(INFANTRY.create(GERMAN), ARMOUR.create(GERMAN));
    final StepUnits units = new StepUnits(attackers, BRITISH, defenders, GERMAN);

    assertFalse(units.noMoreFriendlies());
    assertFalse(units.noMoreEnemies());
  }

  @Test
  void testNoMoreFriendliesWithNoFriendlies() {
    final List<Unit> attackers = List.of(INFANTRY.create(BRITISH), ARMOUR.create(BRITISH));
    final List<Unit> defenders = List.of(INFANTRY.create(GERMAN), ARMOUR.create(GERMAN));
    final StepUnits units = new StepUnits(attackers, BRITISH, defenders, GERMAN);
    units.hitFriendly(attackers.get(0));
    units.hitFriendly(attackers.get(1));

    assertTrue(units.noMoreFriendlies());
    assertFalse(units.noMoreEnemies());
  }

  @Test
  void testNoMoreEnemiesWithNoEnemies() {
    final List<Unit> attackers = List.of(INFANTRY.create(BRITISH), ARMOUR.create(BRITISH));
    final List<Unit> defenders = List.of(INFANTRY.create(GERMAN), ARMOUR.create(GERMAN));
    final StepUnits units = new StepUnits(attackers, BRITISH, defenders, GERMAN);
    units.hitEnemy(defenders.get(0));
    units.hitEnemy(defenders.get(1));

    assertFalse(units.noMoreFriendlies());
    assertTrue(units.noMoreEnemies());
  }

  @Test
  void testCountWithExisting() {
    final List<Unit> attackers = List.of(INFANTRY.create(BRITISH), ARMOUR.create(BRITISH));
    final List<Unit> defenders = List.of(INFANTRY.create(GERMAN), ARMOUR.create(GERMAN));
    final StepUnits units = new StepUnits(attackers, BRITISH, defenders, GERMAN);

    assertEquals(2, units.countOfFriendliesNotDamagedOrDead());
    assertEquals(2, units.countOfEnemiesNotDamagedOrDead());
  }

  @Test
  void testCountWithNoFriendlies() {
    final List<Unit> attackers = List.of(INFANTRY.create(BRITISH), ARMOUR.create(BRITISH));
    final List<Unit> defenders = List.of(INFANTRY.create(GERMAN), ARMOUR.create(GERMAN));
    final StepUnits units = new StepUnits(attackers, BRITISH, defenders, GERMAN);
    units.hitFriendly(attackers.get(0));
    units.hitFriendly(attackers.get(1));

    assertEquals(0, units.countOfFriendliesNotDamagedOrDead());
    assertEquals(2, units.countOfEnemiesNotDamagedOrDead());
  }

  @Test
  void testCountWithNoEnemies() {
    final List<Unit> attackers = List.of(INFANTRY.create(BRITISH), ARMOUR.create(BRITISH));
    final List<Unit> defenders = List.of(INFANTRY.create(GERMAN), ARMOUR.create(GERMAN));
    final StepUnits units = new StepUnits(attackers, BRITISH, defenders, GERMAN);
    units.hitEnemy(defenders.get(0));
    units.hitEnemy(defenders.get(1));

    assertEquals(2, units.countOfFriendliesNotDamagedOrDead());
    assertEquals(0, units.countOfEnemiesNotDamagedOrDead());
  }

  @Test
  void addMultiHitTargetsWithNoMultiHitTargets() {
    final List<Unit> attackers = List.of(INFANTRY.create(BRITISH));
    final List<Unit> defenders = List.of();
    final StepUnits units = new StepUnits(attackers, BRITISH, defenders, GERMAN);

    assertEquals(List.of(attackers.get(0)), units.addFriendlyMultiHitTargets(attackers));
  }

  @Test
  void addMultiHitTargetsWithOneMultiHitWithFullHitPoints() {
    final List<Unit> attackers = List.of(BATTLESHIP.create(BRITISH));
    final List<Unit> defenders = List.of();
    final StepUnits units = new StepUnits(attackers, BRITISH, defenders, GERMAN);

    assertEquals(
        List.of(attackers.get(0), attackers.get(0)), units.addFriendlyMultiHitTargets(attackers));
  }

  @Test
  void addMultiHitTargetsWithOneMultiHitWithOneHit() {
    final List<Unit> attackers = List.of(BATTLESHIP.create(BRITISH));
    final List<Unit> defenders = List.of();
    final StepUnits units = new StepUnits(attackers, BRITISH, defenders, GERMAN);

    units.hitFriendly(attackers.get(0));

    assertEquals(List.of(attackers.get(0)), units.addFriendlyMultiHitTargets(attackers));
  }

  @Test
  void addMultiHitTargetsWithOneMultiHitAndOneNonMultiHit() {
    final List<Unit> attackers = List.of(INFANTRY.create(BRITISH), BATTLESHIP.create(BRITISH));
    final List<Unit> defenders = List.of();
    final StepUnits units = new StepUnits(attackers, BRITISH, defenders, GERMAN);

    assertEquals(
        List.of(attackers.get(1), attackers.get(0), attackers.get(1)),
        units.addFriendlyMultiHitTargets(attackers));
  }

  @Test
  void multiHitTargetIsNotKilledOffWithPartialDamage() {
    final List<Unit> attackers = List.of(BATTLESHIP.create(BRITISH));
    final List<Unit> defenders = List.of();
    final StepUnits units = new StepUnits(attackers, BRITISH, defenders, GERMAN);

    units.hitFriendly(attackers.get(0));

    final StepUnits aliveUnits = units.removeWaitingToDie();
    assertFalse(aliveUnits.noMoreFriendlies());
  }

  @Test
  void multiHitTargetIsKilledOffWithFullDamage() {
    final List<Unit> attackers = List.of(BATTLESHIP.create(BRITISH));
    final List<Unit> defenders = List.of();
    final StepUnits units = new StepUnits(attackers, BRITISH, defenders, GERMAN);

    units.hitFriendly(attackers.get(0));
    units.hitFriendly(attackers.get(0));

    final StepUnits aliveUnits = units.removeWaitingToDie();
    assertTrue(aliveUnits.noMoreFriendlies());
  }

  @Test
  void countOfHitPointsOneUnitNoDamage() {
    final List<Unit> attackers = List.of(BATTLESHIP.create(BRITISH));
    final List<Unit> defenders = List.of();
    final StepUnits units = new StepUnits(attackers, BRITISH, defenders, GERMAN);

    assertEquals(2, units.countOfFriendlyHitPoints());
  }

  @Test
  void countOfHitPointsOneUnitOneDamage() {
    final List<Unit> attackers = List.of(BATTLESHIP.create(BRITISH));
    final List<Unit> defenders = List.of();
    final StepUnits units = new StepUnits(attackers, BRITISH, defenders, GERMAN);

    units.hitFriendly(attackers.get(0));

    assertEquals(1, units.countOfFriendlyHitPoints());
  }

  @Test
  void countOfHitPointsOneUnitTwoDamage() {
    final List<Unit> attackers = List.of(BATTLESHIP.create(BRITISH));
    final List<Unit> defenders = List.of();
    final StepUnits units = new StepUnits(attackers, BRITISH, defenders, GERMAN);

    units.hitFriendly(attackers.get(0));
    units.hitFriendly(attackers.get(0));

    assertEquals(0, units.countOfFriendlyHitPoints());
  }

  @Test
  void countOfHitPointsTwoUnits() {
    final List<Unit> attackers = List.of(BATTLESHIP.create(BRITISH), BATTLESHIP.create(BRITISH));
    final List<Unit> defenders = List.of();
    final StepUnits units = new StepUnits(attackers, BRITISH, defenders, GERMAN);

    assertEquals(4, units.countOfFriendlyHitPoints());
  }
}
