package games.strategy.triplea.delegate.battle.steps.fire;

import static games.strategy.triplea.delegate.battle.BattleStepStrings.CASUALTIES_SUFFIX;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.CASUALTIES_WITHOUT_SPACE_SUFFIX;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.NOTIFY_PREFIX;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.SELECT_CASUALTIES;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.SELECT_FIRST_STRIKE_CASUALTIES;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.SELECT_NAVAL_BOMBARDMENT_CASUALTIES;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.UNITS;

import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.triplea.java.ChangeOnNextMajorRelease;
import org.triplea.java.Interruptibles;
import org.triplea.java.RemoveOnNextMajorRelease;

/** Step where the casualties are moved from ALIVE to CASUALTY */
@RequiredArgsConstructor
public class MarkCasualties implements BattleStep {

  private static final long serialVersionUID = -3823676731273987167L;

  private final BattleState battleState;

  private final BattleActions battleActions;

  /** The side of the firing player */
  private final BattleState.Side side;

  private final FiringGroup firingGroup;

  private final FireRoundState fireRoundState;

  @ChangeOnNextMajorRelease(
      "returnFire is ALL for everything except NavalBombardment and old saves."
          + "Rework so that returnFire isn't needed at all.")
  private final MustFightBattle.ReturnFire returnFire;

  @Override
  public List<String> getNames() {
    return List.of(getName());
  }

  private String getName() {
    return battleState.getPlayer(side.getOpposite()).getName()
        + NOTIFY_PREFIX
        // displaying UNITS makes the text feel redundant so hide it if that is the group name
        + (firingGroup.getDisplayName().equals(UNITS)
            ? CASUALTIES_WITHOUT_SPACE_SUFFIX
            : firingGroup.getDisplayName() + CASUALTIES_SUFFIX);
  }

  @Override
  public Order getOrder() {
    return Order.FIRE_ROUND_REMOVE_CASUALTIES;
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    if (!battleState.getStatus().isHeadless()) {
      notifyCasualties(bridge);
    }

    battleActions.removeCasualties(
        fireRoundState.getCasualties().getKilled(), returnFire, side.getOpposite(), bridge);

    if (firingGroup.isSuicideOnHit()) {
      removeSuicideOnHitUnits(bridge);
    }
  }

  private void notifyCasualties(final IDelegateBridge bridge) {
    final Map<Unit, Collection<Unit>> dependentUnits = new HashMap<>();
    for (final Unit unit : fireRoundState.getCasualties().getKilled()) {
      dependentUnits.put(unit, battleState.getDependentUnits(List.of(unit)));
    }
    for (final Unit unit : fireRoundState.getCasualties().getDamaged()) {
      dependentUnits.put(unit, battleState.getDependentUnits(List.of(unit)));
    }

    bridge
        .getDisplayChannelBroadcaster()
        .casualtyNotification(
            battleState.getBattleId(),
            getPossibleOldNameForNotifyingBattleDisplay(battleState, firingGroup, side, getName()),
            fireRoundState.getDice(),
            battleState.getPlayer(side.getOpposite()),
            new ArrayList<>(fireRoundState.getCasualties().getKilled()),
            new ArrayList<>(fireRoundState.getCasualties().getDamaged()),
            dependentUnits);

    // Allow players to confirm if the casualties were auto calculated
    // Always confirm casualties for AI to give them a chance to pause.
    if (fireRoundState.getCasualties().getAutoCalculated()
        || battleState.getPlayer(side.getOpposite()).isAi()) {
      battleActions
          .getRemotePlayer(battleState.getPlayer(side.getOpposite()), bridge)
          .confirmOwnCasualties(battleState.getBattleId(), "Press space to continue");
    }

    // execute in a separate thread to allow either player to click continue first.
    final Thread t =
        new Thread(
            () -> {
              try {
                battleActions
                    .getRemotePlayer(battleState.getPlayer(side), bridge)
                    .confirmEnemyCasualties(
                        battleState.getBattleId(),
                        "Press space to continue",
                        battleState.getPlayer(side.getOpposite()));
              } catch (final Exception e) {
                // ignore
              }
            },
            "click to continue waiter");
    t.start();
    bridge.leaveDelegateExecution();
    Interruptibles.join(t);
    bridge.enterDelegateExecution();
  }

  /**
   * If the game is loaded from an earlier version, a different step name is shown in the UI. This
   * needs to return that older name so that the UI updates correctly.
   */
  @RemoveOnNextMajorRelease
  static String getPossibleOldNameForNotifyingBattleDisplay(
      final BattleState battleState,
      final FiringGroup firingGroup,
      final BattleState.Side side,
      final String name) {
    if (battleState.getStepStrings().contains(name)) {
      return name;
    }

    // this is from a save game and so must use the old step name
    if (firingGroup.getFiringUnits().stream().anyMatch(Matches.unitIsFirstStrike())) {
      return battleState.getPlayer(side.getOpposite()).getName() + SELECT_FIRST_STRIKE_CASUALTIES;
    } else if (firingGroup.getFiringUnits().stream().anyMatch(Matches.unitIsSea())
        && !battleState.getBattleSite().isWater()) {
      return SELECT_NAVAL_BOMBARDMENT_CASUALTIES;
    } else {
      return battleState.getPlayer(side.getOpposite()).getName() + SELECT_CASUALTIES;
    }
  }

  /**
   * Remove all suicideOnHit units that had a hit
   *
   * <p>All units in a suicideOnHit firing group are expected to be of the same unit type (see
   * {@link FiringGroup#groupBySuicideOnHit}), so just remove units equal to the number of hits.
   *
   * @param bridge the bridge
   */
  private void removeSuicideOnHitUnits(final IDelegateBridge bridge) {
    final List<Unit> suicidedUnits =
        firingGroup.getFiringUnits().stream()
            .limit(fireRoundState.getDice().getHits())
            .collect(Collectors.toList());
    final Map<Unit, Collection<Unit>> dependentUnits = new HashMap<>();
    for (final Unit unit : suicidedUnits) {
      dependentUnits.put(unit, battleState.getDependentUnits(List.of(unit)));
    }

    bridge
        .getDisplayChannelBroadcaster()
        .deadUnitNotification(
            battleState.getBattleId(), battleState.getPlayer(side), suicidedUnits, dependentUnits);

    battleActions.remove(suicidedUnits, bridge, battleState.getBattleSite(), side);
  }
}
