package games.strategy.triplea.delegate.battle;

import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;
import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.mockito.Mockito.lenient;
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
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Getter;

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
  final @Nonnull Territory battleSite;

  @Getter(onMethod = @__({@Override}))
  final @Nonnull Collection<TerritoryEffect> territoryEffects;

  final @Nonnull GamePlayer attacker;

  final @Nonnull GamePlayer defender;

  final @Nonnull Collection<Unit> attackingUnits;

  final @Nonnull Collection<Unit> attackingWaitingToDie;

  final @Nonnull Collection<Unit> defendingUnits;

  final @Nonnull Collection<Unit> defendingWaitingToDie;

  final @Nonnull Collection<Unit> killed;

  final @Nonnull Collection<Unit> retreatUnits;

  @Getter(onMethod = @__({@Override}))
  final @Nonnull GameData gameData;

  final boolean amphibious;

  final boolean over;

  final boolean headless;

  @Getter(onMethod = @__({@Override}))
  final Collection<Territory> attackerRetreatTerritories;

  final Collection<Unit> dependentUnits;

  @Getter(onMethod = @__({@Override}))
  final @Nonnull Collection<Unit> bombardingUnits;

  public FakeBattleState init() {
    lenient().when(attacker.getData()).thenReturn(gameData);
    lenient().when(defender.getData()).thenReturn(gameData);
    return this;
  }

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
  public Collection<Unit> filterUnits(final UnitBattleFilter filter, final Side... sides) {
    return filter.getFilter().stream()
        .flatMap(status -> getUnits(status, sides).stream())
        .collect(Collectors.toList());
  }

  private Collection<Unit> getUnits(final UnitBattleStatus status, final Side... sides) {
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
  public void retreatUnits(final Side side, final Collection<Unit> units) {
    retreatUnits.addAll(units);
  }

  @Override
  public Collection<Unit> removeNonCombatants(final Side side) {
    return List.of();
  }

  @Override
  public void markCasualties(final Collection<Unit> casualties, final Side side) {}

  @Override
  public List<String> getStepStrings() {
    return List.of();
  }

  @Override
  public Optional<String> findStepNameForFiringUnits(final Collection<Unit> firingUnits) {
    return Optional.empty();
  }

  public static FakeBattleState.FakeBattleStateBuilder givenBattleStateBuilder(
      final GamePlayer attacker, final GamePlayer defender) {
    return FakeBattleState.builder()
        .battleRound(2)
        .maxBattleRounds(-1)
        .battleSite(mock(Territory.class))
        .territoryEffects(List.of())
        .attackingUnits(List.of())
        .defendingUnits(List.of())
        .attackingWaitingToDie(List.of())
        .defendingWaitingToDie(List.of())
        .attacker(attacker)
        .defender(defender)
        .bombardingUnits(List.of())
        .dependentUnits(List.of())
        .killed(List.of())
        .retreatUnits(new ArrayList<>())
        .gameData(attacker.getData())
        .amphibious(false)
        .over(false)
        .attackerRetreatTerritories(List.of());
  }

  public static FakeBattleState.FakeBattleStateBuilder givenBattleStateBuilder() {
    final GameData gameData = givenGameData().build();
    final GamePlayer attacker = mock(GamePlayer.class);
    lenient().when(attacker.getData()).thenReturn(gameData);
    final GamePlayer defender = mock(GamePlayer.class);
    lenient().when(defender.getData()).thenReturn(gameData);
    return givenBattleStateBuilder(attacker, defender);
  }
}
