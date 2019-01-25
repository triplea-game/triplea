package games.strategy.triplea.delegate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.net.GUID;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import games.strategy.util.CollectionUtils;
import games.strategy.util.Interruptibles;

/**
 * Maintains the state of a group of units firing during a {@link MustFightBattle}.
 */
public class Fire implements IExecutable {

  private static final long serialVersionUID = -3687054738070722403L;

  private final String stepName;
  private final Collection<Unit> firingUnits;
  private final Collection<Unit> attackableUnits;
  private final MustFightBattle.ReturnFire canReturnFire;
  private final String text;
  private final MustFightBattle battle;
  private final PlayerId firingPlayer;
  private final PlayerId hitPlayer;
  private final boolean defending;
  private final Map<Unit, Collection<Unit>> dependentUnits;
  private final GUID battleId;
  private DiceRoll dice;
  private Collection<Unit> killed;
  private Collection<Unit> damaged;
  private boolean confirmOwnCasualties = true;
  private final boolean isHeadless;
  private final Territory battleSite;
  private final Collection<TerritoryEffect> territoryEffects;
  private final List<Unit> allEnemyUnitsAliveOrWaitingToDie;
  private final Collection<Unit> allFriendlyUnitsNotIncludingWaitingToDie;
  private final Collection<Unit> allEnemyUnitsNotIncludingWaitingToDie;
  private final boolean isAmphibious;
  private final Collection<Unit> amphibiousLandAttackers;

  Fire(final Collection<Unit> attackableUnits, final MustFightBattle.ReturnFire canReturnFire,
      final PlayerId firingPlayer, final PlayerId hitPlayer, final Collection<Unit> firingUnits, final String stepName,
      final String text, final MustFightBattle battle, final boolean defending,
      final Map<Unit, Collection<Unit>> dependentUnits, final boolean headless,
      final Territory battleSite, final Collection<TerritoryEffect> territoryEffects,
      final List<Unit> allEnemyUnitsAliveOrWaitingToDie) {
    this.attackableUnits = CollectionUtils.getMatches(attackableUnits, Matches.unitIsNotInfrastructure());
    this.canReturnFire = canReturnFire;
    this.firingUnits = firingUnits;
    this.stepName = stepName;
    this.text = text;
    this.battle = battle;
    this.hitPlayer = hitPlayer;
    this.firingPlayer = firingPlayer;
    this.defending = defending;
    this.dependentUnits = dependentUnits;
    isHeadless = headless;
    battleId = battle.getBattleId();
    this.battleSite = battleSite;
    this.territoryEffects = territoryEffects;
    this.allEnemyUnitsAliveOrWaitingToDie = allEnemyUnitsAliveOrWaitingToDie;
    allFriendlyUnitsNotIncludingWaitingToDie =
        this.defending ? this.battle.getDefendingUnits() : this.battle.getAttackingUnits();
    allEnemyUnitsNotIncludingWaitingToDie =
        !this.defending ? this.battle.getDefendingUnits() : this.battle.getAttackingUnits();
    isAmphibious = this.battle.isAmphibious();
    amphibiousLandAttackers = this.battle.getAmphibiousLandAttackers();
  }

  private void rollDice(final IDelegateBridge bridge) {
    if (dice != null) {
      throw new IllegalStateException("Already rolled");
    }
    final List<Unit> units = new ArrayList<>(firingUnits);
    final String annotation;
    if (isHeadless) {
      annotation = "";
    } else {
      annotation = DiceRoll.getAnnotation(units, firingPlayer, battle);
    }
    dice = DiceRoll.rollDice(units, defending, firingPlayer, bridge, battle, annotation, territoryEffects,
        allEnemyUnitsAliveOrWaitingToDie);
  }

  private void selectCasualties(final IDelegateBridge bridge) {
    final int hitCount = dice.getHits();
    AbstractBattle.getDisplay(bridge).notifyDice(dice, stepName);
    final int countTransports =
        CollectionUtils.countMatches(attackableUnits, Matches.unitIsTransport().and(Matches.unitIsSea()));
    if (countTransports > 0 && isTransportCasualtiesRestricted(bridge.getData())) {
      final CasualtyDetails message;
      final Collection<Unit> nonTransports = CollectionUtils.getMatches(attackableUnits,
          Matches.unitIsNotTransportButCouldBeCombatTransport().or(Matches.unitIsNotSea()));
      final Collection<Unit> transportsOnly = CollectionUtils.getMatches(attackableUnits,
          Matches.unitIsTransportButNotCombatTransport().and(Matches.unitIsSea()));
      final int numPossibleHits = AbstractBattle.getMaxHits(nonTransports);
      // more hits than combat units
      if (hitCount > numPossibleHits) {
        int extraHits = hitCount - numPossibleHits;
        final Collection<PlayerId> alliedHitPlayer = new ArrayList<>();
        // find the players who have transports in the attackable pile
        for (final Unit unit : transportsOnly) {
          if (!alliedHitPlayer.contains(unit.getOwner())) {
            alliedHitPlayer.add(unit.getOwner());
          }
        }
        // Leave enough transports for each defender for overflows so they can select who loses them.
        for (final PlayerId player : alliedHitPlayer) {
          final Predicate<Unit> match = Matches.unitIsTransportButNotCombatTransport()
              .and(Matches.unitIsOwnedBy(player));
          final Collection<Unit> playerTransports = CollectionUtils.getMatches(transportsOnly, match);
          final int transportsToRemove = Math.max(0, playerTransports.size() - extraHits);
          transportsOnly.removeAll(
              CollectionUtils.getNMatches(playerTransports, transportsToRemove,
                  Matches.unitIsTransportButNotCombatTransport()));
        }
        killed = nonTransports;
        damaged = Collections.emptyList();
        if (extraHits > transportsOnly.size()) {
          extraHits = transportsOnly.size();
        }
        message = BattleCalculator.selectCasualties(hitPlayer, transportsOnly,
            allEnemyUnitsNotIncludingWaitingToDie, allFriendlyUnitsNotIncludingWaitingToDie,
            isAmphibious, amphibiousLandAttackers, battleSite, territoryEffects, bridge, text, dice,
            !defending, battleId, isHeadless, extraHits, true);
        killed.addAll(message.getKilled());
        confirmOwnCasualties = true;
      } else if (hitCount == numPossibleHits) { // exact number of combat units
        killed = nonTransports;
        damaged = Collections.emptyList();
        confirmOwnCasualties = true;
      } else { // less than possible number
        message = BattleCalculator.selectCasualties(hitPlayer, nonTransports,
            allEnemyUnitsNotIncludingWaitingToDie, allFriendlyUnitsNotIncludingWaitingToDie,
            isAmphibious, amphibiousLandAttackers, battleSite, territoryEffects, bridge, text, dice,
            !defending, battleId, isHeadless, dice.getHits(), true);
        killed = message.getKilled();
        damaged = message.getDamaged();
        confirmOwnCasualties = message.getAutoCalculated();
      }
    } else { // not isTransportCasualtiesRestricted
      // they all die
      if (hitCount >= AbstractBattle.getMaxHits(attackableUnits)) {
        killed = attackableUnits;
        damaged = Collections.emptyList();
        // everything died, so we need to confirm
        confirmOwnCasualties = true;
      } else { // Choose casualties
        final CasualtyDetails message;
        message = BattleCalculator.selectCasualties(hitPlayer, attackableUnits,
            allEnemyUnitsNotIncludingWaitingToDie, allFriendlyUnitsNotIncludingWaitingToDie,
            isAmphibious, amphibiousLandAttackers, battleSite, territoryEffects, bridge, text, dice,
            !defending, battleId, isHeadless, dice.getHits(), true);
        killed = message.getKilled();
        damaged = message.getDamaged();
        confirmOwnCasualties = message.getAutoCalculated();
      }
    }
  }

  private void notifyCasualties(final IDelegateBridge bridge) {
    if (isHeadless) {
      return;
    }
    AbstractBattle.getDisplay(bridge).casualtyNotification(battleId, stepName, dice, hitPlayer,
        new ArrayList<>(killed), new ArrayList<>(damaged), dependentUnits);
    // execute in a separate thread to allow either player to click continue first.
    final Thread t = new Thread(() -> {
      try {
        AbstractBattle.getRemote(firingPlayer, bridge).confirmEnemyCasualties(battleId, "Press space to continue",
            hitPlayer);
      } catch (final Exception e) {
        // someone else will deal with this, ignore
      }
    }, "Click to continue waiter");
    t.start();
    if (confirmOwnCasualties) {
      AbstractBattle.getRemote(hitPlayer, bridge).confirmOwnCasualties(battleId, "Press space to continue");
    }
    bridge.leaveDelegateExecution();
    Interruptibles.join(t);
    bridge.enterDelegateExecution();
  }

  /**
   * We must execute in atomic steps, push these steps onto the stack, and let them execute.
   */
  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    // add to the stack so we will execute, we want to roll dice, select casualties, then notify in that order, so push
    // onto the stack in reverse order
    final IExecutable rollDice = new IExecutable() {
      private static final long serialVersionUID = 7578210876028725797L;

      @Override
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        rollDice(bridge);
      }
    };
    final IExecutable selectCasualties = new IExecutable() {
      private static final long serialVersionUID = -7687053541570519623L;

      @Override
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        selectCasualties(bridge);
      }
    };
    final IExecutable notifyCasualties = new IExecutable() {
      private static final long serialVersionUID = -9173385989239225660L;

      @Override
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        notifyCasualties(bridge);
        if (damaged != null) {
          battle.markDamaged(damaged, bridge);
        }
        battle.removeCasualties(killed, canReturnFire, !defending, bridge);
        battle.removeSuicideOnHitCasualties(firingUnits, dice.getHits(), defending, bridge);
      }
    };
    stack.push(notifyCasualties);
    stack.push(selectCasualties);
    stack.push(rollDice);
  }

  private static boolean isTransportCasualtiesRestricted(final GameData data) {
    return Properties.getTransportCasualtiesRestricted(data);
  }

}
