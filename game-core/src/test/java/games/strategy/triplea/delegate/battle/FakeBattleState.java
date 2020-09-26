package games.strategy.triplea.delegate.battle;

import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;
import static org.mockito.Mockito.mock;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

  final int battleRound;

  final int maxBattleRounds;

  @Getter(onMethod = @__({@Override}))
  final UUID battleId;

  @Getter(onMethod = @__({@Override}))
  final @NonNull Territory battleSite;

  @Getter(onMethod = @__({@Override}))
  final @NonNull Collection<TerritoryEffect> territoryEffects;

  final @NonNull GamePlayer attacker;

  final @NonNull GamePlayer defender;

  final @NonNull Collection<Unit> attackingUnits;

  final @NonNull Collection<Unit> attackingWaitingToDie;

  final @NonNull Collection<Unit> defendingUnits;

  final @NonNull Collection<Unit> defendingWaitingToDie;

  final @NonNull Collection<Unit> offensiveAa;

  final @NonNull Collection<Unit> defendingAa;

  final @NonNull Collection<Unit> killed;

  final @NonNull Collection<Unit> retreatUnits;

  @Getter(onMethod = @__({@Override}))
  final @NonNull GameData gameData;

  final boolean amphibious;

  final boolean over;

  final boolean headless;

  @Getter(onMethod = @__({@Override}))
  final Collection<Territory> attackerRetreatTerritories;

  final Collection<Unit> dependentUnits;

  @Getter(onMethod = @__({@Override}))
  final @NonNull Collection<Unit> bombardingUnits;

  @Override
  public Collection<Unit> getDependentUnits(final Collection<Unit> units) {
    return dependentUnits;
  }

  @Override
  public Collection<Unit> getTransportDependents(final Collection<Unit> units) {
    return new ArrayList<>();
  }

  @Override
  public Collection<IBattle> getDependentBattles() {
    return new ArrayList<>();
  }

  @Override
  public BattleStatus getStatus() {
    return BattleStatus.of(battleRound, maxBattleRounds, over, amphibious, headless);
  }

  @Override
  public GamePlayer getPlayer(final Side side) {
    return side == OFFENSE ? attacker : defender;
  }

  @Override
  public Collection<Unit> getUnits(final UnitBattleStatus status, final Side... sides) {
    switch (status) {
      case ALIVE:
        return Collections.unmodifiableCollection(getUnits(sides));
      case CASUALTY:
        return Collections.unmodifiableCollection(getWaitingToDie(sides));
      case REMOVED_CASUALTY:
        return Collections.unmodifiableCollection(killed);
      default:
        return List.of();
    }
  }

  @Override
  public Collection<Unit> getUnits(
      final UnitBattleStatus status1, final UnitBattleStatus status2, final Side... sides) {
    return Stream.concat(getUnits(status1, sides).stream(), getUnits(status2, sides).stream())
        .collect(Collectors.toUnmodifiableList());
  }

  private Collection<Unit> getUnits(final Side... sides) {
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

  private Collection<Unit> getWaitingToDie(final Side... sides) {
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

  @Override
  public void retreatUnits(final Side side, final Collection<Unit> units) {
    retreatUnits.addAll(units);
  }

  @Override
  public Collection<Unit> getAa(final Side... sides) {
    final Collection<Unit> units = new ArrayList<>();
    for (final Side side : sides) {
      switch (side) {
        case OFFENSE:
          units.addAll(offensiveAa);
          break;
        case DEFENSE:
          units.addAll(defendingAa);
          break;
        default:
          break;
      }
    }
    return units;
  }

  public static FakeBattleState.FakeBattleStateBuilder givenBattleStateBuilder() {
    return FakeBattleState.builder()
        .battleRound(2)
        .maxBattleRounds(-1)
        .battleSite(mock(Territory.class))
        .territoryEffects(List.of())
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
        .killed(List.of())
        .retreatUnits(new ArrayList<>())
        .gameData(mock(GameData.class))
        .amphibious(false)
        .over(false)
        .attackerRetreatTerritories(List.of());
  }
}
