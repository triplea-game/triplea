package games.strategy.triplea.ai.pro.util;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.bomber;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.removeFrom;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.pro.ProData;
import games.strategy.triplea.ai.pro.data.ProTerritory;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProAirliftPlannerTest {
  private final GameData data = TestMapGameData.WW2V3_1941.getGameData();
  private final GamePlayer germans = germans(data);
  private final Territory germany = territory("Germany", data);
  private final Territory eastPoland = territory("East Poland", data);
  private final Territory poland = territory("Poland", data);
  private final Map<Territory, ProTerritory> attackMap = new HashMap<>();
  private Unit bomber;
  private List<Unit> infantryInGermany;

  @BeforeEach
  void setUp() {
    germans.getTechAttachment().setParatroopers("true");
    removeFrom(eastPoland, eastPoland.getUnits());
    bomber = germany.getUnitCollection().getMatches(Matches.unitIsAirTransport()).get(0);
    infantryInGermany = germany.getUnitCollection().getMatches(Matches.unitIsAirTransportable());
    attackMap.put(eastPoland, new ProTerritory(eastPoland, new ProData()));
  }

  private List<MoveDescription> plan() {
    return ProAirliftPlanner.planAirlifts(data, germans, attackMap, u -> germany);
  }

  @Test
  void bomberAttackingEnemyLandCarriesOneInfantry() {
    attackMap.get(eastPoland).addUnit(bomber);

    final var moves = plan();

    assertThat(moves).hasSize(1);
    final MoveDescription move = moves.get(0);
    assertThat(move.getRoute().getEnd()).isEqualTo(eastPoland);
    assertThat(move.getAirTransportsDependents()).containsOnlyKeys(bomber);
    assertThat(move.getAirTransportsDependents().get(bomber)).hasSize(1);
    assertThat(move.getUnits()).contains(bomber).hasSize(2);
  }

  @Test
  void bomberIsRemovedFromPlainAttackSoItIsNotMovedTwice() {
    attackMap.get(eastPoland).addUnit(bomber);

    plan();

    assertThat(attackMap.get(eastPoland).getUnits()).doesNotContain(bomber);
  }

  @Test
  void noAirliftWithoutParatroopersTech() {
    germans.getTechAttachment().setParatroopers("false");
    attackMap.get(eastPoland).addUnit(bomber);

    assertThat(plan()).isEmpty();
    assertThat(attackMap.get(eastPoland).getUnits()).contains(bomber);
  }

  @Test
  void noAirliftIntoFriendlyTerritory() {
    final var friendly = new ProTerritory(poland, new ProData());
    friendly.addUnit(bomber);
    attackMap.clear();
    attackMap.put(poland, friendly);

    assertThat(plan()).isEmpty();
  }

  @Test
  void infantryAssignedElsewhereIsNotAirlifted() {
    attackMap.get(eastPoland).addUnit(bomber);
    final var other = new ProTerritory(poland, new ProData());
    other.addUnits(infantryInGermany);
    attackMap.put(poland, other);

    assertThat(plan()).isEmpty();
  }

  @Test
  void movedInfantryIsNotAirlifted() {
    attackMap.get(eastPoland).addUnit(bomber);
    infantryInGermany.forEach(u -> u.setAlreadyMoved(java.math.BigDecimal.ONE));

    assertThat(plan()).isEmpty();
  }

  @Test
  void lastLandUnitIsNotStrippedFromTerritory() {
    attackMap.get(eastPoland).addUnit(bomber);
    removeFrom(germany, germany.getUnitCollection().getMatches(Matches.unitIsLand()));
    final Unit lone = infantry(data).create(germans);
    addTo(germany, List.of(lone));

    assertThat(plan()).isEmpty();
  }

  @Test
  void eachBomberCarriesItsOwnInfantry() {
    final Unit bomber2 = bomber(data).create(germans);
    addTo(germany, List.of(bomber2));
    attackMap.get(eastPoland).addUnit(bomber);
    attackMap.get(eastPoland).addUnit(bomber2);

    final var moves = plan();

    assertThat(moves).hasSize(2);
    final var carried =
        moves.stream().flatMap(m -> m.getAirTransportsDependents().values().stream()).toList();
    assertThat(carried).allSatisfy(c -> assertThat(c).hasSize(1));
    assertThat(carried.stream().flatMap(c -> c.stream()).distinct()).hasSize(2);
  }

  private void planDrops(final BiPredicate<Territory, List<Unit>> canHold) {
    ProAirliftPlanner.planDrops(
        data, germans, new ProData(), List.of(germany), attackMap, u -> germany, canHold);
  }

  @Test
  void idleBomberIsSentToUndefendedEnemyLand() {
    attackMap.clear();

    planDrops((t, paratroopers) -> true);

    assertThat(attackMap).containsOnlyKeys(eastPoland);
    assertThat(attackMap.get(eastPoland).getUnits()).containsExactly(bomber);
  }

  @Test
  void idleBomberIsSentToUndefendedEnemyCapitalWithNoProduction() throws Exception {
    attackMap.clear();
    final var attachment = TerritoryAttachment.getOrThrow(eastPoland);
    attachment.setCapital("Russians");
    attachment.getPropertyOrEmpty("production").orElseThrow().setValue(0);

    planDrops((t, paratroopers) -> true);

    assertThat(attackMap).containsOnlyKeys(eastPoland);
  }

  @Test
  void noParadropWhenParatroopersCannotHoldTheTerritory() {
    attackMap.clear();

    planDrops((t, paratroopers) -> false);

    assertThat(attackMap).isEmpty();
  }

  @Test
  void noParadropOnDefendedTerritory() {
    attackMap.clear();
    addTo(eastPoland, List.of(infantry(data).create(data.getPlayerList().getPlayerId("Russians"))));

    planDrops((t, paratroopers) -> true);

    assertThat(attackMap).isEmpty();
  }

  @Test
  void noParadropWithoutParatroopersTech() {
    germans.getTechAttachment().setParatroopers("false");
    attackMap.clear();

    planDrops((t, paratroopers) -> true);

    assertThat(attackMap).isEmpty();
  }

  @Test
  void plannedParadropRunsThroughAirliftPlanner() {
    attackMap.clear();
    planDrops((t, paratroopers) -> true);

    final var moves = plan();

    assertThat(moves).hasSize(1);
    assertThat(moves.get(0).getRoute().getEnd()).isEqualTo(eastPoland);
    assertThat(moves.get(0).getAirTransportsDependents().get(bomber)).hasSize(1);
  }

  @Test
  void lonelyBomberIsNotLeftToFlyIntoUndefendedLand() {
    attackMap.get(eastPoland).addUnit(bomber);
    infantryInGermany.forEach(u -> u.setAlreadyMoved(java.math.BigDecimal.ONE));

    assertThat(plan()).isEmpty();
    assertThat(attackMap.get(eastPoland).getUnits()).doesNotContain(bomber);
  }
}
