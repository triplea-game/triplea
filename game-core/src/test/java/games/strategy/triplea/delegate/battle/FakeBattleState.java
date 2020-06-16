package games.strategy.triplea.delegate.battle;

import static org.mockito.Mockito.mock;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import java.util.Collection;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/**
 * Fake implementation of BattleState for tests to use
 *
 * <p>{@link #givenBattleStateBuilder()} will return a builder with everything defaulted and the
 * test can override the specific items needed.
 */
@Builder
public class FakeBattleState implements BattleState {

  @Getter(onMethod = @__({@Override}))
  final @NonNull GamePlayer attacker;

  @Getter(onMethod = @__({@Override}))
  final @NonNull GamePlayer defender;

  @Getter(onMethod = @__({@Override}))
  final @NonNull Collection<Unit> attackingUnits;

  @Getter(onMethod = @__({@Override}))
  final @NonNull Collection<Unit> defendingUnits;

  @Getter(onMethod = @__({@Override}))
  final @NonNull Collection<Unit> offensiveAa;

  @Getter(onMethod = @__({@Override}))
  final @NonNull Collection<Unit> defendingAa;

  public static FakeBattleState.FakeBattleStateBuilder givenBattleStateBuilder() {
    return FakeBattleState.builder()
        .attackingUnits(List.of())
        .defendingUnits(List.of())
        .attacker(mock(GamePlayer.class))
        .defender(mock(GamePlayer.class))
        .offensiveAa(List.of())
        .defendingAa(List.of());
  }
}
