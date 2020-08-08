package games.strategy.triplea.delegate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.Constants;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.triplea.java.collections.IntegerMap;

final class AbstractEndTurnDelegateTest {
  @Nested
  final class FindEstimatedIncomeTest extends AbstractDelegateTestCase {
    @Test
    void testFindEstimatedIncome() throws Exception {
      final GameData global40Data = TestMapGameData.GLOBAL1940.getGameData();
      final GamePlayer germans = GameDataTestUtil.germans(global40Data);
      final IntegerMap<Resource> results =
          AbstractEndTurnDelegate.findEstimatedIncome(germans, global40Data);
      final int pus = results.getInt(new Resource(Constants.PUS, global40Data));
      assertEquals(40, pus);
    }
  }

  @Nested
  final class GetSingleNeighborBlockadesThenHighestToLowestProductionTest {
    private final GameData gameData = new GameData();
    private final Comparator<Territory> comparator =
        AbstractEndTurnDelegate.getSingleNeighborBlockadesThenHighestToLowestProduction(
            List.of(), gameData.getMap());
    private final Territory territory = new Territory("territoryName", gameData);

    @Test
    void shouldReturnZeroWhenBothTerritoriesAreNull() {
      assertThat(comparator.compare(null, null), is(0));
    }

    @Test
    void shouldReturnZeroWhenBothTerritoriesAreSame() {
      assertThat(comparator.compare(territory, territory), is(0));
    }

    @Test
    void shouldReturnZeroWhenBothTerritoriesAreEqual() {
      assertThat(
          comparator.compare(territory, new Territory(territory.getName(), gameData)), is(0));
    }

    @Test
    void shouldReturnLessThanZeroWhenFirstTerritoryIsNonNullAndSecondTerritoryIsNull() {
      assertThat(comparator.compare(territory, null), is(lessThan(0)));
    }

    @Test
    void shouldReturnGreaterThanZeroWhenFirstTerritoryIsNullAndSecondTerritoryIsNonNull() {
      assertThat(comparator.compare(null, territory), is(greaterThan(0)));
    }
  }

  @Nested
  final class GetSingleBlockadeThenHighestToLowestBlockadeDamageTest {
    private final GameData gameData = new GameData();
    private final Comparator<Territory> comparator =
        AbstractEndTurnDelegate.getSingleBlockadeThenHighestToLowestBlockadeDamage(Map.of());
    private final Territory territory = new Territory("territoryName", gameData);

    @Test
    void shouldReturnZeroWhenBothTerritoriesAreNull() {
      assertThat(comparator.compare(null, null), is(0));
    }

    @Test
    void shouldReturnZeroWhenBothTerritoriesAreSame() {
      assertThat(comparator.compare(territory, territory), is(0));
    }

    @Test
    void shouldReturnZeroWhenBothTerritoriesAreEqual() {
      assertThat(
          comparator.compare(territory, new Territory(territory.getName(), gameData)), is(0));
    }

    @Test
    void shouldReturnLessThanZeroWhenFirstTerritoryIsNonNullAndSecondTerritoryIsNull() {
      assertThat(comparator.compare(territory, null), is(lessThan(0)));
    }

    @Test
    void shouldReturnGreaterThanZeroWhenFirstTerritoryIsNullAndSecondTerritoryIsNonNull() {
      assertThat(comparator.compare(null, territory), is(greaterThan(0)));
    }
  }
}
