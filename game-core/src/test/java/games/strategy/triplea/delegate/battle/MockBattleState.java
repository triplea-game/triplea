package games.strategy.triplea.delegate.battle;

import games.strategy.engine.data.Unit;
import java.util.Collection;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class MockBattleState {

  @Builder
  public static class TestBattleState implements BattleState {

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

  public static TestBattleState.TestBattleStateBuilder givenBattleState() {
    return TestBattleState.builder().attackingUnits(List.of()).defendingUnits(List.of());
  }
}
