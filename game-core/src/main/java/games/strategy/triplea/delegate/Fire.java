package games.strategy.triplea.delegate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.net.GUID;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.util.CollectionUtils;
import games.strategy.util.Interruptibles;

public class Fire implements IExecutable {

  private static final long serialVersionUID = -3687054738070722403L;

  private final String m_stepName;
  private final Collection<Unit> m_firingUnits;
  private final Collection<Unit> m_attackableUnits;
  private final MustFightBattle.ReturnFire m_canReturnFire;
  private final String m_text;
  private final MustFightBattle m_battle;
  private final PlayerID m_firingPlayer;
  private final PlayerID m_hitPlayer;
  private final boolean m_defending;
  private final Map<Unit, Collection<Unit>> m_dependentUnits;
  private final GUID m_battleID;
  private DiceRoll m_dice;
  private Collection<Unit> m_killed;
  private Collection<Unit> m_damaged;
  private boolean m_confirmOwnCasualties = true;
  private final boolean m_isHeadless;
  private final Territory m_battleSite;
  private final Collection<TerritoryEffect> m_territoryEffects;
  private final List<Unit> m_allEnemyUnitsAliveOrWaitingToDie;
  private final Collection<Unit> m_allFriendlyUnitsNotIncludingWaitingToDie;
  private final Collection<Unit> m_allEnemyUnitsNotIncludingWaitingToDie;
  private final boolean m_isAmphibious;
  private final Collection<Unit> m_amphibiousLandAttackers;

  Fire(final Collection<Unit> attackableUnits, final MustFightBattle.ReturnFire canReturnFire,
      final PlayerID firingPlayer, final PlayerID hitPlayer, final Collection<Unit> firingUnits, final String stepName,
      final String text, final MustFightBattle battle, final boolean defending,
      final Map<Unit, Collection<Unit>> dependentUnits, final boolean headless,
      final Territory battleSite, final Collection<TerritoryEffect> territoryEffects,
      final List<Unit> allEnemyUnitsAliveOrWaitingToDie) {
    m_attackableUnits = CollectionUtils.getMatches(attackableUnits, Matches.unitIsNotInfrastructure());
    m_canReturnFire = canReturnFire;
    m_firingUnits = firingUnits;
    m_stepName = stepName;
    m_text = text;
    m_battle = battle;
    m_hitPlayer = hitPlayer;
    m_firingPlayer = firingPlayer;
    m_defending = defending;
    m_dependentUnits = dependentUnits;
    m_isHeadless = headless;
    m_battleID = battle.getBattleId();
    m_battleSite = battleSite;
    m_territoryEffects = territoryEffects;
    m_allEnemyUnitsAliveOrWaitingToDie = allEnemyUnitsAliveOrWaitingToDie;
    m_allFriendlyUnitsNotIncludingWaitingToDie =
        m_defending ? m_battle.getDefendingUnits() : m_battle.getAttackingUnits();
    m_allEnemyUnitsNotIncludingWaitingToDie =
        !m_defending ? m_battle.getDefendingUnits() : m_battle.getAttackingUnits();
    m_isAmphibious = m_battle.isAmphibious();
    m_amphibiousLandAttackers = m_battle.getAmphibiousLandAttackers();
  }

  private void rollDice(final IDelegateBridge bridge) {
    if (m_dice != null) {
      throw new IllegalStateException("Already rolled");
    }
    final List<Unit> units = new ArrayList<>(m_firingUnits);
    final String annotation;
    if (m_isHeadless) {
      annotation = "";
    } else {
      annotation = DiceRoll.getAnnotation(units, m_firingPlayer, m_battle);
    }
    m_dice = DiceRoll.rollDice(units, m_defending, m_firingPlayer, bridge, m_battle, annotation, m_territoryEffects,
        m_allEnemyUnitsAliveOrWaitingToDie);
  }

  private void selectCasualties(final IDelegateBridge bridge) {
    final int hitCount = m_dice.getHits();
    AbstractBattle.getDisplay(bridge).notifyDice(m_dice, m_stepName);
    final int countTransports =
        CollectionUtils.countMatches(m_attackableUnits, Matches.unitIsTransport().and(Matches.unitIsSea()));
    if ((countTransports > 0) && isTransportCasualtiesRestricted(bridge.getData())) {
      final CasualtyDetails message;
      final Collection<Unit> nonTransports = CollectionUtils.getMatches(m_attackableUnits,
          Matches.unitIsNotTransportButCouldBeCombatTransport().or(Matches.unitIsNotSea()));
      final Collection<Unit> transportsOnly = CollectionUtils.getMatches(m_attackableUnits,
          Matches.unitIsTransportButNotCombatTransport().and(Matches.unitIsSea()));
      final int numPossibleHits = AbstractBattle.getMaxHits(nonTransports);
      // more hits than combat units
      if (hitCount > numPossibleHits) {
        int extraHits = hitCount - numPossibleHits;
        final Collection<PlayerID> alliedHitPlayer = new ArrayList<>();
        // find the players who have transports in the attackable pile
        for (final Unit unit : transportsOnly) {
          if (!alliedHitPlayer.contains(unit.getOwner())) {
            alliedHitPlayer.add(unit.getOwner());
          }
        }
        // Leave enough transports for each defender for overflows so they can select who loses them.
        for (final PlayerID player : alliedHitPlayer) {
          final Predicate<Unit> match = Matches.unitIsTransportButNotCombatTransport()
              .and(Matches.unitIsOwnedBy(player));
          final Collection<Unit> playerTransports = CollectionUtils.getMatches(transportsOnly, match);
          final int transportsToRemove = Math.max(0, playerTransports.size() - extraHits);
          transportsOnly.removeAll(
              CollectionUtils.getNMatches(playerTransports, transportsToRemove,
                  Matches.unitIsTransportButNotCombatTransport()));
        }
        m_killed = nonTransports;
        m_damaged = Collections.emptyList();
        if (extraHits > transportsOnly.size()) {
          extraHits = transportsOnly.size();
        }
        message = BattleCalculator.selectCasualties(m_stepName, m_hitPlayer, transportsOnly,
            m_allEnemyUnitsNotIncludingWaitingToDie, m_firingPlayer, m_allFriendlyUnitsNotIncludingWaitingToDie,
            m_isAmphibious, m_amphibiousLandAttackers, m_battleSite, m_territoryEffects, bridge, m_text, m_dice,
            !m_defending, m_battleID, m_isHeadless, extraHits, true);
        m_killed.addAll(message.getKilled());
        m_confirmOwnCasualties = true;
      } else if (hitCount == numPossibleHits) { // exact number of combat units
        m_killed = nonTransports;
        m_damaged = Collections.emptyList();
        m_confirmOwnCasualties = true;
      } else { // less than possible number
        message = BattleCalculator.selectCasualties(m_stepName, m_hitPlayer, nonTransports,
            m_allEnemyUnitsNotIncludingWaitingToDie, m_firingPlayer, m_allFriendlyUnitsNotIncludingWaitingToDie,
            m_isAmphibious, m_amphibiousLandAttackers, m_battleSite, m_territoryEffects, bridge, m_text, m_dice,
            !m_defending, m_battleID, m_isHeadless, m_dice.getHits(), true);
        m_killed = message.getKilled();
        m_damaged = message.getDamaged();
        m_confirmOwnCasualties = message.getAutoCalculated();
      }
    } else { // not isTransportCasualtiesRestricted
      // they all die
      if (hitCount >= AbstractBattle.getMaxHits(m_attackableUnits)) {
        m_killed = m_attackableUnits;
        m_damaged = Collections.emptyList();
        // everything died, so we need to confirm
        m_confirmOwnCasualties = true;
      } else { // Choose casualties
        final CasualtyDetails message;
        message = BattleCalculator.selectCasualties(m_stepName, m_hitPlayer, m_attackableUnits,
            m_allEnemyUnitsNotIncludingWaitingToDie, m_firingPlayer, m_allFriendlyUnitsNotIncludingWaitingToDie,
            m_isAmphibious, m_amphibiousLandAttackers, m_battleSite, m_territoryEffects, bridge, m_text, m_dice,
            !m_defending, m_battleID, m_isHeadless, m_dice.getHits(), true);
        m_killed = message.getKilled();
        m_damaged = message.getDamaged();
        m_confirmOwnCasualties = message.getAutoCalculated();
      }
    }
  }

  private void notifyCasualties(final IDelegateBridge bridge) {
    if (m_isHeadless) {
      return;
    }
    AbstractBattle.getDisplay(bridge).casualtyNotification(m_battleID, m_stepName, m_dice, m_hitPlayer,
        new ArrayList<>(m_killed), new ArrayList<>(m_damaged), m_dependentUnits);
    // execute in a separate thread to allow either player to click continue first.
    final Thread t = new Thread(() -> {
      try {
        AbstractBattle.getRemote(m_firingPlayer, bridge).confirmEnemyCasualties(m_battleID, "Press space to continue",
            m_hitPlayer);
      } catch (final Exception e) {
        // someone else will deal with this, ignore
      }
    }, "Click to continue waiter");
    t.start();
    if (m_confirmOwnCasualties) {
      AbstractBattle.getRemote(m_hitPlayer, bridge).confirmOwnCasualties(m_battleID, "Press space to continue");
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
        if (m_damaged != null) {
          m_battle.markDamaged(m_damaged, bridge);
        }
        m_battle.removeCasualties(m_killed, m_canReturnFire, !m_defending, bridge);
        m_battle.removeSuicideOnHitCasualties(m_firingUnits, m_dice.getHits(), m_defending, bridge);
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
