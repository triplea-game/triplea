package games.strategy.triplea.delegate.data;

import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.Unit;
import java.util.HashMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MustMoveWithDetailsTest {

  @Mock private Unit unit;

  @Test
  @DisplayName(
      "Verify getMustMoveWithForUnit will convert null mapped values to an empty collection")
  void getMustMoveWithForUnitReturnsEmptyCollectionsWhenValueIsNull() {
    final MustMoveWithDetails mustMoveWithDetails = new MustMoveWithDetails(new HashMap<>());
    mustMoveWithDetails.getMustMoveWith().put(unit, null);
    assertThat(mustMoveWithDetails.getMustMoveWithForUnit(unit)).isEmpty();
  }
}
