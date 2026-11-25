package games.strategy.triplea.ai.pro.util;

import static games.strategy.triplea.delegate.GameDataTestUtil.armour;
import static games.strategy.triplea.delegate.GameDataTestUtil.bomber;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameState;
import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProMoveUtilsTest {
  private final GameState data = TestMapGameData.WW2V3_1941.getGameData();
  private final Route route = new Route(territory("Germany", data), territory("Poland", data));
  private final Unit bomber = bomber(data).create(germans(data));
  private final Unit inf = infantry(data).create(germans(data));
  private final Unit inf2 = infantry(data).create(germans(data));
  private final Unit tank = armour(data).create(germans(data));

  @BeforeEach
  void enableParatroopers() {
    germans(data).getTechAttachment().setParatroopers("true");
  }

  private List<MoveDescription> moves(final MoveDescription... moves) {
    return new ArrayList<>(List.of(moves));
  }

  @Test
  void mergeKeepsBomberAndInfantryMovesSeparate() {
    final var moves =
        moves(
            new MoveDescription(List.of(bomber), route), new MoveDescription(List.of(inf), route));

    ProMoveUtils.mergeSameRouteMoves(moves);

    assertThat(moves).hasSize(2);
  }

  @Test
  void mergeKeepsAirTransportApartFromInfantryWhenOtherUnitsAlsoMove() {
    final var moves =
        moves(
            new MoveDescription(List.of(bomber, tank), route),
            new MoveDescription(List.of(inf), route));

    ProMoveUtils.mergeSameRouteMoves(moves);

    assertThat(moves).hasSize(2);
  }

  @Test
  void mergeStillMergesInfantryAndArmourOnSameRoute() {
    final var moves =
        moves(new MoveDescription(List.of(inf), route), new MoveDescription(List.of(tank), route));

    ProMoveUtils.mergeSameRouteMoves(moves);

    assertThat(moves).hasSize(1);
    assertThat(moves.get(0).getUnits()).containsExactlyInAnyOrder(inf, tank);
  }

  @Test
  void mergeStillMergesTwoBombers() {
    final Unit bomber2 = bomber(data).create(germans(data));
    final var moves =
        moves(
            new MoveDescription(List.of(bomber), route),
            new MoveDescription(List.of(bomber2), route));

    ProMoveUtils.mergeSameRouteMoves(moves);

    assertThat(moves).hasSize(1);
  }

  @Test
  void mergeLeavesIntentionalAirliftMoveAlone() {
    final var airlift =
        new MoveDescription(List.of(bomber, inf), route, Map.of(), Map.of(bomber, List.of(inf)));
    final var moves = moves(airlift, new MoveDescription(List.of(inf2), route));

    ProMoveUtils.mergeSameRouteMoves(moves);

    assertThat(moves).hasSize(2).contains(airlift);
  }
}
