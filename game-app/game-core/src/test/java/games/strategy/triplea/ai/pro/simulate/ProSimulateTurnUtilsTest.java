package games.strategy.triplea.ai.pro.simulate;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.bomber;
import static games.strategy.triplea.delegate.GameDataTestUtil.british;
import static games.strategy.triplea.delegate.GameDataTestUtil.destroyer;
import static games.strategy.triplea.delegate.GameDataTestUtil.fighter;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.removeFrom;
import static games.strategy.triplea.delegate.GameDataTestUtil.russians;
import static games.strategy.triplea.delegate.GameDataTestUtil.submarine;
import static games.strategy.triplea.delegate.MockDelegateBridge.newDelegateBridge;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.ai.pro.ProData;
import games.strategy.triplea.ai.pro.data.ProBattleResult;
import games.strategy.triplea.ai.pro.util.ProOddsCalculator;
import games.strategy.triplea.delegate.battle.BattleTracker;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProSimulateTurnUtilsTest {
  private final GameData gameData = TestMapGameData.REVISED.getGameData();
  private final BattleTracker battleTracker = gameData.getBattleDelegate().getBattleTracker();
  private final ProOddsCalculator calc = mock(ProOddsCalculator.class);

  @Test
  void bombingRaidLeavesTargetWithItsOwner() {
    final GamePlayer british = british(gameData);
    final Territory unitedKingdom = gameData.getMap().getTerritoryOrThrow("United Kingdom");
    final Territory germany = gameData.getMap().getTerritoryOrThrow("Germany");
    final List<Unit> bombers = bomber(gameData).create(1, british);
    addTo(germany, bombers);
    final IDelegateBridge bridge = newDelegateBridge(british);
    battleTracker.addBattle(
        gameData.getMap().getRouteOrElseThrow(unitedKingdom, germany, it -> true),
        bombers,
        true,
        british,
        bridge,
        null,
        List.of(),
        Map.of(),
        false);
    when(calc.callBattleCalc(any(), any(), any(), any(), any()))
        .thenReturn(new ProBattleResult(100, 10, false, bombers, List.of(), 1));

    ProSimulateTurnUtils.simulateBattles(new ProData(), gameData, british, bridge, calc);

    assertThat(germany.getOwner()).isEqualTo(germans(gameData));
    assertThat(battleTracker.wasConquered(germany)).isFalse();
  }

  @Test
  void landBattleWonWithOnlyAirLeftLeavesTerritoryWithItsOwner() {
    final GamePlayer russians = russians(gameData);
    final Territory karelia = gameData.getMap().getTerritoryOrThrow("Karelia S.S.R.");
    final Territory belorussia = gameData.getMap().getTerritoryOrThrow("Belorussia");
    final List<Unit> fighters = fighter(gameData).create(1, russians);
    final List<Unit> infantry = infantry(gameData).create(1, russians);
    final List<Unit> attackers = List.of(fighters.get(0), infantry.get(0));
    addTo(belorussia, attackers);
    final IDelegateBridge bridge = newDelegateBridge(russians);
    battleTracker.addBattle(
        gameData.getMap().getRouteOrElseThrow(karelia, belorussia, it -> true),
        attackers,
        russians,
        bridge,
        null,
        List.of());
    when(calc.callBattleCalc(any(), any(), any(), any(), any()))
        .thenReturn(new ProBattleResult(100, 10, false, fighters, List.of(), 2));

    ProSimulateTurnUtils.simulateBattles(new ProData(), gameData, russians, bridge, calc);

    assertThat(belorussia.getOwner()).isEqualTo(germans(gameData));
    assertThat(battleTracker.wasConquered(belorussia)).isFalse();
  }

  @Test
  void undefendedLandingWaitingOnSeaBattleTakesTerritory() {
    final GamePlayer british = british(gameData);
    final Territory seaZone12 = gameData.getMap().getTerritoryOrThrow("12 Sea Zone");
    final Territory seaZone13 = gameData.getMap().getTerritoryOrThrow("13 Sea Zone");
    final Territory algeria = gameData.getMap().getTerritoryOrThrow("Algeria");
    removeFrom(algeria, List.copyOf(algeria.getUnits()));
    addTo(seaZone12, submarine(gameData).create(1, germans(gameData)));
    final List<Unit> destroyers = destroyer(gameData).create(1, british);
    addTo(seaZone12, destroyers);
    final List<Unit> infantry = infantry(gameData).create(1, british);
    addTo(algeria, infantry);
    final IDelegateBridge bridge = newDelegateBridge(british);
    battleTracker.addBattle(
        gameData.getMap().getRouteOrElseThrow(seaZone13, seaZone12, it -> true),
        destroyers,
        british,
        bridge,
        null,
        List.of());
    battleTracker.addBattle(
        gameData.getMap().getRouteOrElseThrow(seaZone12, algeria, it -> true),
        infantry,
        british,
        bridge,
        null,
        List.of());
    when(calc.callBattleCalc(any(), any(), any(), any(), any())).thenReturn(new ProBattleResult());

    ProSimulateTurnUtils.simulateBattles(new ProData(), gameData, british, bridge, calc);

    assertThat(algeria.getOwner()).isEqualTo(british);
    assertThat(battleTracker.wasConquered(algeria)).isTrue();
  }

  @Test
  void landBattleWonWithLandUnitLeftTakesTerritory() {
    final GamePlayer russians = russians(gameData);
    final Territory karelia = gameData.getMap().getTerritoryOrThrow("Karelia S.S.R.");
    final Territory belorussia = gameData.getMap().getTerritoryOrThrow("Belorussia");
    final List<Unit> fighters = fighter(gameData).create(1, russians);
    final List<Unit> infantry = infantry(gameData).create(1, russians);
    final List<Unit> attackers = List.of(fighters.get(0), infantry.get(0));
    addTo(belorussia, attackers);
    final IDelegateBridge bridge = newDelegateBridge(russians);
    battleTracker.addBattle(
        gameData.getMap().getRouteOrElseThrow(karelia, belorussia, it -> true),
        attackers,
        russians,
        bridge,
        null,
        List.of());
    when(calc.callBattleCalc(any(), any(), any(), any(), any()))
        .thenReturn(new ProBattleResult(100, 10, true, attackers, List.of(), 2));

    ProSimulateTurnUtils.simulateBattles(new ProData(), gameData, russians, bridge, calc);

    assertThat(belorussia.getOwner()).isEqualTo(russians);
    assertThat(battleTracker.wasConquered(belorussia)).isTrue();
  }
}
