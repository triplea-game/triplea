package games.strategy.triplea.odds.calculator;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.framework.startup.ui.PlayerType;
import games.strategy.triplea.ai.AbstractAi;
import games.strategy.triplea.ai.AiUtils;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import games.strategy.triplea.delegate.data.CasualtyList;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.delegate.remote.ITechDelegate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import org.triplea.java.collections.CollectionUtils;

class DummyPlayer extends AbstractAi {
  private final boolean keepAtLeastOneLand;
  // negative = do not retreat
  private final int retreatAfterRound;
  // negative = do not retreat
  private final int retreatAfterXUnitsLeft;
  private final boolean retreatWhenOnlyAirLeft;
  private final DummyDelegateBridge bridge;
  private final boolean isAttacker;
  private final List<Unit> orderOfLosses;

  DummyPlayer(
      final DummyDelegateBridge dummyDelegateBridge,
      final boolean attacker,
      final String name,
      final List<Unit> orderOfLosses,
      final boolean keepAtLeastOneLand,
      final int retreatAfterRound,
      final int retreatAfterXUnitsLeft,
      final boolean retreatWhenOnlyAirLeft) {
    super(name);
    this.keepAtLeastOneLand = keepAtLeastOneLand;
    this.retreatAfterRound = retreatAfterRound;
    this.retreatAfterXUnitsLeft = retreatAfterXUnitsLeft;
    this.retreatWhenOnlyAirLeft = retreatWhenOnlyAirLeft;
    bridge = dummyDelegateBridge;
    isAttacker = attacker;
    this.orderOfLosses = orderOfLosses;
  }

  @Override
  public PlayerType getPlayerType() {
    return PlayerType.BATTLE_CALC_DUMMY;
  }

  private MustFightBattle getBattle() {
    return bridge.getBattle();
  }

  @VisibleForTesting
  List<Unit> getOurUnits() {
    final MustFightBattle battle = getBattle();
    if (battle == null) {
      return null;
    }
    return new ArrayList<>(isAttacker ? battle.getAttackingUnits() : battle.getDefendingUnits());
  }

  @VisibleForTesting
  List<Unit> getEnemyUnits() {
    final MustFightBattle battle = getBattle();
    if (battle == null) {
      return null;
    }
    return new ArrayList<>(isAttacker ? battle.getDefendingUnits() : battle.getAttackingUnits());
  }

  @Override
  protected void move(
      final boolean nonCombat,
      final IMoveDelegate moveDel,
      final GameData data,
      final PlayerId player) {}

  @Override
  protected void place(
      final boolean placeForBid,
      final IAbstractPlaceDelegate placeDelegate,
      final GameData data,
      final PlayerId player) {}

  @Override
  protected void purchase(
      final boolean purchaseForBid,
      final int pusToSpend,
      final IPurchaseDelegate purchaseDelegate,
      final GameData data,
      final PlayerId player) {}

  @Override
  protected void tech(
      final ITechDelegate techDelegate, final GameData data, final PlayerId player) {}

  @Override
  public boolean confirmMoveInFaceOfAa(final Collection<Territory> aaFiringTerritories) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<Unit> getNumberOfFightersToMoveToNewCarrier(
      final Collection<Unit> fightersThatCanBeMoved, final Territory from) {
    throw new UnsupportedOperationException();
  }

  /**
   * The battle calc doesn't actually care if you have available territories to retreat to or not.
   * It will always let you retreat to the 'current' territory (the battle territory), even if that
   * is illegal. This is because the battle calc does not know where the attackers are actually
   * coming from.
   */
  @Override
  public Territory retreatQuery(
      final UUID battleId,
      final boolean submerge,
      final Territory battleSite,
      final Collection<Territory> possibleTerritories,
      final String message) {
    // null = do not retreat
    if (possibleTerritories.isEmpty()) {
      return null;
    }
    if (submerge) {
      // submerge if all air vs subs
      final Predicate<Unit> planeNotDestroyer =
          Matches.unitIsAir().and(Matches.unitIsDestroyer().negate());
      final List<Unit> ourUnits = getOurUnits();
      final List<Unit> enemyUnits = getEnemyUnits();
      if (ourUnits == null || enemyUnits == null) {
        return null;
      }
      if (!enemyUnits.isEmpty()
          && enemyUnits.stream().allMatch(planeNotDestroyer)
          && !ourUnits.isEmpty()
          && ourUnits.stream().allMatch(Matches.unitCanNotBeTargetedByAll())) {
        return possibleTerritories.iterator().next();
      }
      return null;
    }

    final MustFightBattle battle = getBattle();
    if (battle == null) {
      return null;
    }
    if (retreatAfterRound > -1 && battle.getBattleRound() >= retreatAfterRound) {
      return possibleTerritories.iterator().next();
    }
    if (!retreatWhenOnlyAirLeft && retreatAfterXUnitsLeft <= -1) {
      return null;
    }
    final Collection<Unit> unitsLeft =
        isAttacker ? battle.getAttackingUnits() : battle.getDefendingUnits();
    final Collection<Unit> airLeft = CollectionUtils.getMatches(unitsLeft, Matches.unitIsAir());
    if (retreatWhenOnlyAirLeft) {
      // lets say we have a bunch of 3 attack air unit, and a 4 attack non-air unit,
      // and we want to retreat when we have all air units left + that 4 attack non-air (cus it gets
      // taken
      // casualty last)
      // then we add the number of air, to the retreat after X left number (which we would set to
      // '1')
      int retreatNum = airLeft.size();
      if (retreatAfterXUnitsLeft > 0) {
        retreatNum += retreatAfterXUnitsLeft;
      }
      if (retreatNum >= unitsLeft.size()) {
        return possibleTerritories.iterator().next();
      }
    }
    if (retreatAfterXUnitsLeft > -1 && retreatAfterXUnitsLeft >= unitsLeft.size()) {
      return possibleTerritories.iterator().next();
    }
    return null;
  }

  // Added new collection autoKilled to handle killing units prior to casualty selection
  @Override
  public CasualtyDetails selectCasualties(
      final Collection<Unit> selectFrom,
      final Map<Unit, Collection<Unit>> dependents,
      final int count,
      final String message,
      final DiceRoll dice,
      final PlayerId hit,
      final Collection<Unit> friendlyUnits,
      final Collection<Unit> enemyUnits,
      final boolean amphibious,
      final Collection<Unit> amphibiousLandAttackers,
      final CasualtyList defaultCasualties,
      final UUID battleId,
      final Territory battleSite,
      final boolean allowMultipleHitsPerUnit) {
    final List<Unit> damagedUnits = new ArrayList<>(defaultCasualties.getDamaged());
    final List<Unit> killedUnits = new ArrayList<>(defaultCasualties.getKilled());
    if (keepAtLeastOneLand) {
      final List<Unit> notKilled = new ArrayList<>(selectFrom);
      notKilled.removeAll(killedUnits);
      // no land units left, but we have a non land unit to kill and land unit was killed
      if (notKilled.stream().noneMatch(Matches.unitIsLand())
          && notKilled.stream().anyMatch(Matches.unitIsNotLand())
          && killedUnits.stream().anyMatch(Matches.unitIsLand())) {
        final List<Unit> notKilledAndNotLand =
            CollectionUtils.getMatches(notKilled, Matches.unitIsNotLand());
        // sort according to cost
        notKilledAndNotLand.sort(AiUtils.getCostComparator());
        // remove the last killed unit, this should be the strongest
        killedUnits.remove(killedUnits.size() - 1);
        // add the cheapest unit
        killedUnits.add(notKilledAndNotLand.get(0));
      }
    }
    if (orderOfLosses != null && !orderOfLosses.isEmpty() && !killedUnits.isEmpty()) {
      final List<Unit> orderOfLosses = new ArrayList<>(this.orderOfLosses);
      orderOfLosses.retainAll(selectFrom);
      if (!orderOfLosses.isEmpty()) {
        int killedSize = killedUnits.size();
        killedUnits.clear();
        while (killedSize > 0 && !orderOfLosses.isEmpty()) {
          killedUnits.add(orderOfLosses.get(0));
          orderOfLosses.remove(0);
          killedSize--;
        }
        if (killedSize > 0) {
          final List<Unit> defaultKilled = new ArrayList<>(defaultCasualties.getKilled());
          defaultKilled.removeAll(killedUnits);
          while (killedSize > 0) {
            killedUnits.add(defaultKilled.get(0));
            defaultKilled.remove(0);
            killedSize--;
          }
        }
      }
    }
    return new CasualtyDetails(killedUnits, damagedUnits, false);
  }

  @Override
  public Territory selectTerritoryForAirToLand(
      final Collection<Territory> candidates,
      final Territory currentTerritory,
      final String unitMessage) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean shouldBomberBomb(final Territory territory) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Unit whatShouldBomberBomb(
      final Territory territory,
      final Collection<Unit> potentialTargets,
      final Collection<Unit> bombers) {
    throw new UnsupportedOperationException();
  }
}
