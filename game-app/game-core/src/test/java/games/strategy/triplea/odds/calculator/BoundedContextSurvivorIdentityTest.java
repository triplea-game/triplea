package games.strategy.triplea.odds.calculator;

import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.russians;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pins the survivor-identity contract (T4): the "average units remaining" the bounded-context calc
 * returns must be the caller's own {@link Unit} instances, not rehydrated copies — AI consumers
 * remove them from their own collections by identity.
 */
class BoundedContextSurvivorIdentityTest {

  @Test
  void survivorsAreTheCallersOriginalInstances() {
    final GameData gameData = TestMapGameData.REVISED.getGameData();
    final GamePlayer russians = russians(gameData);
    final GamePlayer germans = germans(gameData);
    final Territory germany = territory("Germany", gameData);
    final List<Unit> attacking = infantry(gameData).create(10, russians);
    final List<Unit> defending = infantry(gameData).create(1, germans);

    final BoundedContextBattleCalculator calculator = new BoundedContextBattleCalculator();
    calculator.setGameData(gameData);
    calculator.setRandomSource(ScriptedRandomSource.alwaysHits());

    final AggregateResults results =
        calculator.calculate(
            russians,
            germans,
            germany,
            attacking,
            defending,
            List.of(),
            TerritoryEffectHelper.getEffects(germany),
            false,
            1);

    final Collection<Unit> remaining = results.getAverageAttackingUnitsRemaining();
    assertFalse(remaining.isEmpty(), "an attacker-favored brawl must leave survivors");
    for (final Unit survivor : remaining) {
      assertTrue(
          attacking.stream().anyMatch(original -> original == survivor),
          "each survivor must be an original attacking instance, not a rehydrated copy");
    }
  }
}
