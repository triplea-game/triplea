package games.strategy.triplea.delegate.battle;

import games.strategy.engine.data.Unit;
import java.util.Collection;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/**
 * Simple implementation of BattleState for tests to use
 *
 * <p>{@link #givenBattleState()} will return a builder with everything defaulted and the test can
 * override the specific items needed.
 */
@Builder
public class MockBattleState implements BattleState {

  @Getter(onMethod = @__({@Override}))
  final @NonNull Collection<Unit> attackingUnits;

  @Getter(onMethod = @__({@Override}))
  final @NonNull Collection<Unit> defendingUnits;

  public static MockBattleState.MockBattleStateBuilder givenBattleState() {
    return MockBattleState.builder().attackingUnits(List.of()).defendingUnits(List.of());
  }
}
