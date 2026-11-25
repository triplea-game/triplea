package games.strategy.triplea.ai.pro.data;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.armour;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.makeGameLowLuck;
import static games.strategy.triplea.delegate.GameDataTestUtil.removeFrom;
import static games.strategy.triplea.delegate.GameDataTestUtil.russians;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.pro.ProData;
import games.strategy.triplea.ai.pro.util.ProCaptureChainUtils;
import games.strategy.triplea.ai.pro.util.ProCaptureChainUtils.Chain;
import games.strategy.triplea.ai.pro.util.ProOddsCalculator;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.odds.calculator.ConcurrentBattleCalculator;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sonatype.goodies.prefs.memory.MemoryPreferences;

class ProCaptureChainCacheTest {
  private final GameData gameData = TestMapGameData.LHTR.getGameData();
  private final ProData proData = new ProData(true);
  private final ProOddsCalculator calc;
  private final Territory germany = territory("Germany", gameData);
  private final ProCaptureChainUtils.Cache cache = new ProCaptureChainUtils.Cache();

  ProCaptureChainCacheTest() {
    ClientSetting.setPreferences(new MemoryPreferences());
    makeGameLowLuck(gameData);
    final ConcurrentBattleCalculator battleCalculator = new ConcurrentBattleCalculator();
    battleCalculator.setGameData(gameData).join();
    calc = new ProOddsCalculator(battleCalculator);
    proData.initializeSimulation(null, gameData, germans(gameData));
  }

  private List<Unit> garrison(final int infantry) {
    removeFrom(germany, germany.getMatches(Matches.unitIsNotInfrastructure()));
    final List<Unit> defenders = infantry(gameData).create(infantry, germans(gameData));
    addTo(germany, defenders);
    return defenders;
  }

  private ProTerritory armyOf(final int armourCount) {
    final ProTerritory army = new ProTerritory(germany, proData);
    army.addMaxUnits(armour(gameData).create(armourCount, russians(gameData)));
    return army;
  }

  private ProOtherMoveOptions twoArmies() {
    return new ProOtherMoveOptions(
        List.of(Map.of(germany, armyOf(5)), Map.of(germany, armyOf(4))), germans(gameData), true);
  }

  @Test
  void sameDefendersAgainstSameArmiesGiveTheChainWithoutRunningItAgain() {
    final List<Unit> defenders = garrison(8);
    final ProOtherMoveOptions enemies = twoArmies();

    final Chain first = cache.runAndLog(proData, calc, enemies, germany, defenders);
    final Chain second = cache.runAndLog(proData, calc, enemies, germany, defenders);

    assertThat(second).isSameAs(first);
  }

  @Test
  void differentDefendersAreRunAgain() {
    final List<Unit> defenders = garrison(8);
    final ProOtherMoveOptions enemies = twoArmies();
    final Chain first = cache.runAndLog(proData, calc, enemies, germany, defenders);

    final Chain fewer = cache.runAndLog(proData, calc, enemies, germany, defenders.subList(0, 7));

    assertThat(fewer).isNotSameAs(first);
  }

  @Test
  void clearedCacheRunsAgain() {
    final List<Unit> defenders = garrison(8);
    final ProOtherMoveOptions enemies = twoArmies();
    final Chain first = cache.runAndLog(proData, calc, enemies, germany, defenders);

    cache.clear();

    assertThat(cache.runAndLog(proData, calc, enemies, germany, defenders)).isNotSameAs(first);
  }

  @Test
  void armiesComeInTheOrderOfTheEnemyMoveMaps() {
    final ProOtherMoveOptions enemies = twoArmies();

    final List<ProCaptureChainUtils.Army> armies = ProCaptureChainUtils.armies(enemies, germany);

    assertThat(armies.stream().map(a -> a.units().size())).containsExactly(5, 4);
  }

  @Test
  void territoryNoEnemyCanReachHasNoChain() {
    final Chain chain =
        cache.runAndLog(proData, calc, new ProOtherMoveOptions(), germany, garrison(8));

    assertThat(chain).isNull();
  }
}
