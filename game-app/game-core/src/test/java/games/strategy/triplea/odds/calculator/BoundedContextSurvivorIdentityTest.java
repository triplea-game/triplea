package games.strategy.triplea.odds.calculator;

import static games.strategy.triplea.delegate.GameDataTestUtil.armour;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.russians;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pins the survivor-identity contract (T4): the "average units remaining" the bounded-context calc
 * returns must be the caller's own {@link Unit} instances, not rehydrated copies — AI consumers
 * remove them from their own collections by identity. Each case also checks <em>completeness</em>:
 * as many units come back as the count-backed average says survived, so the mapper returns the
 * whole survivor set, not a subset.
 *
 * <p>Both cases run {@code alwaysHits} over several runs (every run is identical, so the average
 * and the representative run coincide) with two unit types per side, and confirm the losing side's
 * remaining collection is empty but still mutable (a downstream {@code addAll} must not throw).
 */
class BoundedContextSurvivorIdentityTest {

  @Test
  void attackerSurvivorsAreOriginalInstancesAndComplete() {
    final GameData gameData = TestMapGameData.REVISED.getGameData();
    final GamePlayer russians = russians(gameData);
    final GamePlayer germans = germans(gameData);
    final Territory germany = territory("Germany", gameData);
    final List<Unit> attacking = new ArrayList<>(infantry(gameData).create(10, russians));
    attacking.addAll(armour(gameData).create(5, russians));
    final List<Unit> defending = infantry(gameData).create(1, germans);

    final AggregateResults results =
        calculate(gameData, russians, germans, germany, attacking, defending);

    final Collection<Unit> remaining = results.getAverageAttackingUnitsRemaining();
    assertAllOriginals(remaining, attacking);
    assertEquals(
        Math.round(results.getAverageAttackingUnitsLeft()),
        remaining.size(),
        "every surviving attacker the counts report must be mapped back to an original unit");
    assertTrue(
        remaining.stream().anyMatch(u -> u.getType().getName().equals("armour")),
        "the surviving mix must include both attacker types");
    assertMutableAndEmpty(results.getAverageDefendingUnitsRemaining());
  }

  @Test
  void defenderSurvivorsAreOriginalInstancesAndComplete() {
    final GameData gameData = TestMapGameData.REVISED.getGameData();
    final GamePlayer russians = russians(gameData);
    final GamePlayer germans = germans(gameData);
    final Territory germany = territory("Germany", gameData);
    final List<Unit> attacking = infantry(gameData).create(1, russians);
    final List<Unit> defending = new ArrayList<>(infantry(gameData).create(10, germans));
    defending.addAll(armour(gameData).create(5, germans));

    final AggregateResults results =
        calculate(gameData, russians, germans, germany, attacking, defending);

    final Collection<Unit> remaining = results.getAverageDefendingUnitsRemaining();
    assertAllOriginals(remaining, defending);
    assertEquals(
        Math.round(results.getAverageDefendingUnitsLeft()),
        remaining.size(),
        "every surviving defender the counts report must be mapped back to an original unit");
    assertTrue(
        remaining.stream().anyMatch(u -> u.getType().getName().equals("armour")),
        "the surviving mix must include both defender types");
    assertMutableAndEmpty(results.getAverageAttackingUnitsRemaining());
  }

  private static AggregateResults calculate(
      final GameData gameData,
      final GamePlayer attacker,
      final GamePlayer defender,
      final Territory location,
      final List<Unit> attacking,
      final List<Unit> defending) {
    final BoundedContextBattleCalculator calculator = new BoundedContextBattleCalculator();
    calculator.setGameData(gameData);
    calculator.setRandomSource(ScriptedRandomSource.alwaysHits());
    return calculator.calculate(
        attacker,
        defender,
        location,
        attacking,
        defending,
        List.of(),
        TerritoryEffectHelper.getEffects(location),
        false,
        5);
  }

  private static void assertAllOriginals(
      final Collection<Unit> remaining, final List<Unit> originals) {
    assertFalse(remaining.isEmpty(), "the winning side must leave survivors");
    for (final Unit survivor : remaining) {
      assertTrue(
          originals.stream().anyMatch(original -> original == survivor),
          "each survivor must be an original caller instance, not a rehydrated copy");
    }
  }

  private static void assertMutableAndEmpty(final Collection<Unit> remaining) {
    assertTrue(remaining.isEmpty(), "the annihilated side leaves no survivors");
    assertDoesNotThrow(
        () -> remaining.addAll(List.of()), "the empty remaining collection must stay mutable");
  }
}
