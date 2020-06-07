package games.strategy.triplea.delegate.battle;

import games.strategy.engine.data.Unit;
import java.util.Collection;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;

/**
 * Simple implementation of BattleState for tests to use
 *
 * {@link #givenBattleState()} will return a builder with everything defaulted and the test
 * can override the specific items needed.
 */
@Builder
public class MockBattleState implements BattleState {

  public static MockBattleState.MockBattleStateBuilder givenBattleState() {
    return MockBattleState.builder().attackingUnits(List.of()).defendingUnits(List.of());
  }

  final @NonNull Collection<Unit> attackingUnits;
  final @NonNull Collection<Unit> defendingUnits;

  @Override
  public Collection<Unit> getAttackingUnits() {
    return attackingUnits;
  }

  @Override
  public Collection<Unit> getDefendingUnits() {
    return defendingUnits;
    }
}
