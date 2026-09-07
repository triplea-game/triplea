package games.strategy.triplea.odds.calculator.adapter;

import static games.strategy.triplea.delegate.GameDataTestUtil.americans;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.submarine;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.GameDataTestUtil.transport;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.odds.calculator.context.model.BattleOptions;
import games.strategy.triplea.odds.calculator.context.model.BattleScenario;
import games.strategy.triplea.odds.calculator.context.model.CargoRule;
import games.strategy.triplea.odds.calculator.context.model.CombatFlag;
import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.Force;
import games.strategy.triplea.odds.calculator.context.model.Key;
import games.strategy.triplea.odds.calculator.context.model.UnitTypeId;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Adapter-side coverage of the transport cargo cascade (the {@code IS_DEPENDENT} half of the
 * feature): the model-level tests prove the cascade fires given a {@code Dependents} map, and this
 * pins that {@link GameDataBattleAdapter} actually bakes one — marking a side's land units in a sea
 * battle as non-combatant cargo and keying a compact {@link CargoRule} on the transport that
 * carries them.
 */
class GameDataBattleAdapterCargoTest {

  @Test
  void seaBattleCargoIsFlaggedDependentAndBakedIntoDependents() {
    final GameData gameData = TestMapGameData.REVISED.getGameData();
    final Territory seaZone = territory("1 Sea Zone", gameData);
    final List<Unit> transportUnit = transport(gameData).create(1, germans(gameData));
    final Collection<Unit> cargo = infantry(gameData).create(2, germans(gameData));
    final Collection<Unit> defending = new ArrayList<>(transportUnit);
    defending.addAll(cargo);

    final BattleScenario scenario =
        new GameDataBattleAdapter()
            .toScenario(
                americans(gameData),
                germans(gameData),
                seaZone,
                submarine(gameData).create(2, americans(gameData)),
                defending,
                List.of(),
                TerritoryEffectHelper.getEffects(seaZone),
                new BattleOptions(false, List.of(), List.of()));

    final String infantryName = infantry(gameData).getName();
    final String transportName = transport(gameData).getName();
    final CombatProfile cargoProfile = profileOf(scenario.defenders(), infantryName);
    final CombatProfile transportProfile = profileOf(scenario.defenders(), transportName);

    // The infantry ride as non-combatant cargo; the transport stays an ordinary combatant.
    assertThat(cargoProfile.flags()).contains(CombatFlag.IS_DEPENDENT);
    assertThat(transportProfile.flags()).doesNotContain(CombatFlag.IS_DEPENDENT);

    // Compact loading: the transport's full two-infantry load is tied to its profile.
    final CargoRule rule = scenario.dependents().rules().get(transportProfile);
    assertThat(rule).isNotNull();
    assertThat(rule.cargoType()).isEqualTo(new UnitTypeId(infantryName));
    assertThat(rule.capacity()).isEqualTo(2);
  }

  private static CombatProfile profileOf(final Force force, final String typeName) {
    return force.counts().keySet().stream()
        .map(Key::profile)
        .filter(profile -> profile.type().name().equals(typeName))
        .findFirst()
        .orElseThrow(() -> new AssertionError("no " + typeName + " profile in force"));
  }
}
