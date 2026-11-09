package games.strategy.triplea.odds.calculator;

import static games.strategy.triplea.delegate.GameDataTestUtil.artillery;
import static games.strategy.triplea.delegate.GameDataTestUtil.fighter;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.odds.calculator.OrderOfLoss.splitOrderOfLoss;
import static games.strategy.triplea.odds.calculator.OrderOfLoss.splitOrderOfLossSection;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class OrderOfLossTest {
  @Nested
  final class SplitOrderOfLossTest {
    @Test
    void shouldReturnOneSectionWhenOrderOfLossDoesNotContainSeparator() {
      assertThat(splitOrderOfLoss("  *^infantry  ")).containsExactly("*^infantry");
    }

    @Test
    void shouldReturnOneSectionPerUnitTypeWhenOrderOfLossContainsSeparator() {
      assertThat(splitOrderOfLoss("  *^infantry;1^artillery;2^fighter  "))
          .containsExactly("*^infantry", "1^artillery", "2^fighter");
    }
  }

  @Nested
  final class SplitOrderOfLossSectionTest {
    @Test
    void shouldReturnAmountAndUnitType() {
      assertThat(splitOrderOfLossSection("*^infantry")).containsExactly("*", "infantry");
      assertThat(splitOrderOfLossSection("1^artillery")).containsExactly("1", "artillery");
      assertThat(splitOrderOfLossSection("99^fighter")).containsExactly("99", "fighter");
    }
  }

  @Nested
  final class IsValidOrderOfLossTest {
    private final GameData data = TestMapGameData.REVISED.getGameData();

    @Test
    void blankOrderIsValid() {
      assertThat(OrderOfLoss.isValidOrderOfLoss("  ", data)).isTrue();
    }

    @Test
    void knownUnitTypesWithAllOrPositiveAmountAreValid() {
      assertThat(OrderOfLoss.isValidOrderOfLoss("*^infantry;2^artillery", data)).isTrue();
    }

    @Test
    void unknownUnitTypeIsInvalid() {
      assertThat(OrderOfLoss.isValidOrderOfLoss("*^notAUnit", data)).isFalse();
    }

    @Test
    void zeroAmountIsInvalid() {
      assertThat(OrderOfLoss.isValidOrderOfLoss("0^infantry", data)).isFalse();
    }

    @Test
    void sectionWithoutAmountDescriptorIsInvalid() {
      assertThat(OrderOfLoss.isValidOrderOfLoss("infantry", data)).isFalse();
    }
  }

  @Nested
  final class GetUnitListByOrderOfLossTest {
    private final GameData data = TestMapGameData.REVISED.getGameData();

    @Test
    void returnsNullForBlankOrder() {
      final List<Unit> units = infantry(data).create(2, germans(data));

      assertThat(OrderOfLoss.getUnitListByOrderOfLoss(" ", units, data)).isNull();
    }

    @Test
    void ordersUnitsAsListedAndOmitsUnlistedOnes() {
      final List<Unit> units = new ArrayList<>();
      units.addAll(artillery(data).create(2, germans(data)));
      units.addAll(infantry(data).create(2, germans(data)));
      units.addAll(fighter(data).create(1, germans(data)));

      final List<Unit> ordered =
          OrderOfLoss.getUnitListByOrderOfLoss("*^infantry;1^artillery", units, data);

      assertThat(ordered)
          .extracting(Unit::getType)
          .containsExactly(infantry(data), infantry(data), artillery(data));
    }

    @Test
    void laterSectionsClaimUnitsBeforeEarlierAllSections() {
      final List<Unit> units = new ArrayList<>();
      units.addAll(infantry(data).create(3, germans(data)));
      units.addAll(fighter(data).create(1, germans(data)));

      final List<Unit> ordered =
          OrderOfLoss.getUnitListByOrderOfLoss("*^infantry;*^fighter;1^infantry", units, data);

      assertThat(ordered)
          .extracting(Unit::getType)
          .containsExactly(infantry(data), infantry(data), fighter(data), infantry(data));
    }
  }
}
