package games.strategy.triplea.ai;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.british;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.GameDataTestUtil.unitType;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Constants;
import games.strategy.triplea.ai.pro.ProAi;
import games.strategy.triplea.ai.weak.WeakAi;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AbstractAiBombingTargetTest {
  private final GameData gameData = TestMapGameData.GLOBAL1940.getGameData();
  private final AbstractAi beta = new ProAi("beta", "beta", true);
  private final AbstractAi hard = new ProAi("hard", "hard");
  private final AbstractAi weak = new WeakAi("weak");

  @Test
  void bomberHitsTheFactoryWithTheMostProductionLeft() {
    final Territory germany = territory("Germany", gameData);
    final Unit minor = unitType("factory_minor", gameData).create(germans(gameData));
    final Unit major = unitType("factory_major", gameData).create(germans(gameData));

    assertThat(beta.whatShouldBomberBomb(germany, List.of(minor, major), List.of()))
        .isSameAs(major);
    assertThat(beta.whatShouldBomberBomb(germany, List.of(major, minor), List.of()))
        .isSameAs(major);
  }

  @Test
  void bomberSkipsAFactoryThatIsMostlyDestroyed() {
    final Territory germany = territory("Germany", gameData);
    final Unit minor = unitType("factory_minor", gameData).create(germans(gameData));
    final Unit wreck = unitType("factory_major", gameData).create(germans(gameData));
    wreck.setUnitDamage(9);

    assertThat(beta.whatShouldBomberBomb(germany, List.of(wreck, minor), List.of()))
        .isSameAs(minor);
  }

  @Test
  void bombardmentGoesWhereDefendersAreStillUnhit() {
    final Territory a = territory("Germany", gameData);
    final Territory b = territory("France", gameData);

    assertThat(AbstractAi.chooseBombardTerritory(List.of(a, b), t -> Map.of(a, 1, b, 3).get(t)))
        .isSameAs(b);
    assertThat(AbstractAi.chooseBombardTerritory(List.of(b, a), t -> Map.of(a, 3, b, 1).get(t)))
        .isSameAs(a);
  }

  @Test
  void bombardmentBreaksTiesByName() {
    final Territory a = territory("Germany", gameData);
    final Territory b = territory("France", gameData);

    assertThat(AbstractAi.chooseBombardTerritory(List.of(a, b), t -> 2)).isSameAs(b);
  }

  @Test
  void shipBombardsTheTerritoryWithMoreDefenders() {
    final Territory germany = territory("Germany", gameData);
    final Territory france = territory("France", gameData);
    final GamePlayer british = british(gameData);
    final int germanDefenders =
        germany.getUnitCollection().countMatches(Matches.enemyUnit(british));
    final int frenchDefenders = france.getUnitCollection().countMatches(Matches.enemyUnit(british));
    assertThat(germanDefenders).isNotEqualTo(frenchDefenders);
    final Territory expected = germanDefenders > frenchDefenders ? germany : france;
    final Territory seaZone = territory("42 Sea Zone", gameData);
    final Unit ship = unitType("battleship", gameData).create(british);

    assertThat(beta.selectBombardingTerritory(ship, seaZone, List.of(germany, france), true))
        .isSameAs(expected);
    assertThat(beta.selectBombardingTerritory(ship, seaZone, List.of(france, germany), true))
        .isSameAs(expected);
  }

  @Test
  void rocketsAvoidATerritoryWhoseProductionIsAlreadyLostThisTurn() {
    final GameData revised = TestMapGameData.REVISED.getGameData();
    revised.getProperties().set(Constants.LIMIT_ROCKET_DAMAGE_PER_TURN, true);
    final Territory germany = territory("Germany", revised);
    final Territory westernEurope = territory("Western Europe", revised);
    assertThat(TerritoryAttachment.getProduction(germany))
        .isGreaterThan(TerritoryAttachment.getProduction(westernEurope));
    revised.getMoveDelegate().pusLost(germany, TerritoryAttachment.getProduction(germany));
    final AbstractAi betaBritish = new ProAi("beta", "beta", true);
    betaBritish.initialize(null, british(revised));
    final AbstractAi weakBritish = new WeakAi("weak");
    weakBritish.initialize(null, british(revised));
    final AbstractAi hardBritish = new ProAi("hard", "hard");
    hardBritish.initialize(null, british(revised));

    assertThat(betaBritish.whereShouldRocketsAttack(List.of(germany, westernEurope), null))
        .isSameAs(westernEurope);
    assertThat(weakBritish.whereShouldRocketsAttack(List.of(germany, westernEurope), null))
        .isSameAs(germany);
    assertThat(hardBritish.whereShouldRocketsAttack(List.of(germany, westernEurope), null))
        .isSameAs(germany);
  }

  @Test
  void rocketsAvoidATerritoryWhoseFactoriesAreAtMaxDamage() {
    final Territory germany = territory("Germany", gameData);
    final Territory france = territory("France", gameData);
    assertThat(TerritoryAttachment.getProduction(germany))
        .isGreaterThan(TerritoryAttachment.getProduction(france));
    germany
        .getUnits()
        .forEach(
            u -> {
              if (u.getHowMuchDamageCanThisUnitTakeTotal(germany) > 0) {
                u.setUnitDamage(u.getHowMuchDamageCanThisUnitTakeTotal(germany));
              }
            });
    addTo(france, List.of(unitType("factory_minor", gameData).create(germans(gameData))));
    final AbstractAi betaBritish = new ProAi("beta", "beta", true);
    betaBritish.initialize(null, british(gameData));
    final AbstractAi weakBritish = new WeakAi("weak");
    weakBritish.initialize(null, british(gameData));

    assertThat(betaBritish.whereShouldRocketsAttack(List.of(germany, france), null))
        .isSameAs(france);
    assertThat(weakBritish.whereShouldRocketsAttack(List.of(germany, france), null))
        .isSameAs(germany);
  }

  @Test
  void stockAisKeepTheFirstBombingTarget() {
    final Territory germany = territory("Germany", gameData);
    final Unit minor = unitType("factory_minor", gameData).create(germans(gameData));
    final Unit major = unitType("factory_major", gameData).create(germans(gameData));

    for (final AbstractAi stock : List.of(weak, hard)) {
      assertThat(stock.whatShouldBomberBomb(germany, List.of(minor, major), List.of()))
          .isSameAs(minor);
      assertThat(stock.whatShouldBomberBomb(germany, List.of(major, minor), List.of()))
          .isSameAs(major);
    }
  }

  @Test
  void stockAisKeepTheFirstBombardTerritory() {
    final Territory germany = territory("Germany", gameData);
    final Territory france = territory("France", gameData);
    final Territory seaZone = territory("42 Sea Zone", gameData);
    final Unit ship = unitType("battleship", gameData).create(british(gameData));

    for (final AbstractAi stock : List.of(weak, hard)) {
      assertThat(stock.selectBombardingTerritory(ship, seaZone, List.of(germany, france), true))
          .isSameAs(germany);
      assertThat(stock.selectBombardingTerritory(ship, seaZone, List.of(france, germany), true))
          .isSameAs(france);
    }
  }
}
