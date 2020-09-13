package games.strategy.triplea.delegate.battle;

import static org.mockito.Mockito.mock;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
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
  final int battleRound;

  @Getter(onMethod = @__({@Override}))
  final UUID battleId;

  @Getter(onMethod = @__({@Override}))
  final @NonNull Territory battleSite;

  @Getter(onMethod = @__({@Override}))
  final @NonNull GamePlayer attacker;

  @Getter(onMethod = @__({@Override}))
  final @NonNull GamePlayer defender;

  final @NonNull Collection<Unit> attackingUnits;

  final @NonNull Collection<Unit> attackingWaitingToDie;

  final @NonNull Collection<Unit> defendingUnits;

  final @NonNull Collection<Unit> defendingWaitingToDie;

  @Getter(onMethod = @__({@Override}))
  final @NonNull Collection<Unit> offensiveAa;

  @Getter(onMethod = @__({@Override}))
  final @NonNull Collection<Unit> defendingAa;

  @Getter(onMethod = @__({@Override}))
  final @NonNull GameData gameData;

  @Getter(onMethod = @__({@Override}))
  final boolean amphibious;

  @Getter(onMethod = @__({@Override}))
  final boolean over;

  @Getter(onMethod = @__({@Override}))
  final Collection<Territory> attackerRetreatTerritories;

  final Collection<Territory> emptyOrFriendlySeaNeighbors;

  final Collection<Unit> dependentUnits;

  @Getter(onMethod = @__({@Override}))
  final @NonNull Collection<Unit> bombardingUnits;

  @Override
  public Collection<Territory> getEmptyOrFriendlySeaNeighbors(final Collection<Unit> units) {
    return emptyOrFriendlySeaNeighbors;
  }

  @Override
  public Collection<Unit> getDependentUnits(final Collection<Unit> units) {
    return dependentUnits;
  }

  @Override
  public Collection<Unit> getUnits(final Side... sides) {
    final Collection<Unit> units = new ArrayList<>();
    for (final Side side : sides) {
      switch (side) {
        case OFFENSE:
          units.addAll(attackingUnits);
          break;
        case DEFENSE:
          units.addAll(defendingUnits);
          break;
        default:
          break;
      }
    }
    return units;
  }

  @Override
  public Collection<Unit> getWaitingToDie(final Side... sides) {
    final Collection<Unit> waitingToDie = new ArrayList<>();
    for (final Side side : sides) {
      switch (side) {
        case OFFENSE:
          waitingToDie.addAll(attackingWaitingToDie);
          break;
        case DEFENSE:
          waitingToDie.addAll(defendingWaitingToDie);
          break;
        default:
          break;
      }
    }
    return waitingToDie;
  }

  @Override
  public void clearWaitingToDie(final Side... sides) {
    for (final Side side : sides) {
      switch (side) {
        case OFFENSE:
          attackingWaitingToDie.clear();
          break;
        case DEFENSE:
          defendingWaitingToDie.clear();
          break;
        default:
          break;
      }
    }
  }

  public static FakeBattleState.FakeBattleStateBuilder givenBattleStateBuilder() {
    return FakeBattleState.builder()
        .battleRound(2)
        .battleSite(mock(Territory.class))
        .attackingUnits(List.of())
        .defendingUnits(List.of())
        .attackingWaitingToDie(List.of())
        .defendingWaitingToDie(List.of())
        .attacker(mock(GamePlayer.class))
        .defender(mock(GamePlayer.class))
        .offensiveAa(List.of())
        .defendingAa(List.of())
        .bombardingUnits(List.of())
        .dependentUnits(List.of())
        .gameData(mock(GameData.class))
        .amphibious(false)
        .over(false)
        .attackerRetreatTerritories(List.of())
        .emptyOrFriendlySeaNeighbors(List.of());
  }
}
