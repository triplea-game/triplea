package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.player.Player;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.battle.BattleTracker;
import games.strategy.triplea.formatter.MyFormatter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import org.triplea.java.PredicateBuilder;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.sound.SoundPath;

/** Logic to fire rockets. */
public class RocketsFireHelper implements Serializable {
  private static final long serialVersionUID = -3694482580987118444L;
  private final Set<Territory> attackingFromTerritories = new HashSet<>();
  private final Map<Territory, Territory> attackedTerritories = new LinkedHashMap<>();
  private final Map<Territory, Unit> attackedUnits = new LinkedHashMap<>();
  private boolean needToFindRocketTargets;

  private RocketsFireHelper() {}

  /** Rocket pseudo constructor for better than V1 rockets. */
  public static RocketsFireHelper setUpRockets(final IDelegateBridge bridge) {
    final GameState data = bridge.getData();
    final RocketsFireHelper helper = new RocketsFireHelper();
    // WW2V2/WW2V3, now fires at the start of the BattleDelegate
    // WW2V1, fires at end of non combat move so does not call here
    helper.needToFindRocketTargets = false;
    if ((Properties.getWW2V2(data.getProperties())
            || Properties.getAllRocketsAttack(data.getProperties()))
        && TechTracker.hasRocket(bridge.getGamePlayer())) {
      if (Properties.getSequentiallyTargetedRockets(data.getProperties())) {
        helper.needToFindRocketTargets = true;
      } else {
        helper.findRocketTargetsAndFireIfNeeded(bridge, false);
      }
    }
    return helper;
  }

  /**
   * Fire rockets if we are v1 In this rule set, each player only gets one rocket attack per turn.
   */
  public static void fireWW2V1IfNeeded(final IDelegateBridge bridge) {
    final GameData data = bridge.getData();
    final GamePlayer player = bridge.getGamePlayer();
    // If we don't have rockets or aren't V1 then do nothing.
    if (!TechTracker.hasRocket(player)
        || Properties.getWW2V2(data.getProperties())
        || Properties.getAllRocketsAttack(data.getProperties())) {
      return;
    }
    final Set<Territory> rocketTerritories = getTerritoriesWithRockets(data, player);
    final Set<Territory> targets = new HashSet<>();
    for (final Territory territory : rocketTerritories) {
      targets.addAll(getTargetsWithinRange(territory, data, player));
    }
    if (targets.isEmpty()) {
      bridge
          .getHistoryWriter()
          .startEvent(player.getName() + " has no targets to attack with rockets");
      return;
    }
    final Territory attacked = getTarget(targets, bridge, null);

    if (attacked != null) {
      new RocketsFireHelper().fireRocket(bridge, data, null, attacked);
    }
  }

  /**
   * Find Rocket Targets and load up fire rockets for later execution if necessary. Directly fired
   * with Sequentially Targeted rockets.
   */
  private void findRocketTargetsAndFireIfNeeded(
      final IDelegateBridge bridge, final boolean fireRocketsImmediately) {
    final GameData data = bridge.getData();
    final GamePlayer player = bridge.getGamePlayer();
    final Map<Territory, Integer> previouslyAttackedTerritories = new LinkedHashMap<>();
    final int maxAttacks = data.getTechTracker().getRocketNumberPerTerritory(player);
    for (final Territory attackFrom : getTerritoriesWithRockets(data, player)) {
      final Set<Territory> targets = getTargetsWithinRange(attackFrom, data, player);
      for (final Territory t : previouslyAttackedTerritories.keySet()) {
        // negative Rocket Number per Territory == unlimited
        if (maxAttacks >= 0 && maxAttacks <= previouslyAttackedTerritories.get(t)) {
          targets.remove(t);
        }
      }
      if (targets.isEmpty()) {
        continue;
      }
      // Ask the user where each rocket launcher should target.
      Territory targetTerritory;
      while (true) {
        targetTerritory = getTarget(targets, bridge, attackFrom);
        if (targetTerritory == null) {
          break;
        }
        final Collection<Unit> enemyUnits =
            targetTerritory.getMatches(
                Matches.enemyUnit(player).and(Matches.unitIsBeingTransported().negate()));
        final Collection<Unit> enemyTargetsTotal =
            CollectionUtils.getMatches(
                enemyUnits, Matches.unitIsAtMaxDamageOrNotCanBeDamaged(targetTerritory).negate());
        Unit unitTarget = null;
        if (Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data.getProperties())) {
          final Collection<Unit> rocketTargets =
              CollectionUtils.getMatches(attackFrom.getUnits(), rocketMatch(player));
          final HashSet<UnitType> legalTargetsForTheseRockets = new HashSet<>();
          // a hack for now, we let the rockets fire at anyone who could be targeted by any rocket
          // Not sure if that comment is still current
          for (final Unit r : rocketTargets) {
            legalTargetsForTheseRockets.addAll(
                r.getUnitAttachment().getBombingTargets(data.getUnitTypeList()));
          }
          final Collection<Unit> enemyTargets =
              CollectionUtils.getMatches(
                  enemyTargetsTotal, Matches.unitIsOfTypes(legalTargetsForTheseRockets));
          if (enemyTargets.isEmpty()) {
            // TODO: this sucks
            continue;
          }
          if (enemyTargets.size() == 1) {
            unitTarget = CollectionUtils.getAny(enemyTargets);
          } else {
            final Player remotePlayer = bridge.getRemotePlayer(player);
            unitTarget =
                remotePlayer.whatShouldBomberBomb(targetTerritory, enemyTargets, rocketTargets);
          }
          if (unitTarget == null) {
            continue;
            // Ask them if they now want to attack a different territory
          }
        }
        attackedTerritories.put(attackFrom, targetTerritory);
        attackedUnits.put(attackFrom, unitTarget);
        // Sequentially Targeted Rockets are target, fire, target, fire ...
        // Sensible (non-sequential) Rockets are target, target, target, fire, fire, fire.
        if (fireRocketsImmediately) {
          fireRocket(bridge, data, attackFrom, targetTerritory);
          break;
        }
        // Can't add this value above because it would cause the rocket to fire twice in a
        // Sequentially Targeted rocket
        // scenario
        attackingFromTerritories.add(attackFrom);
        break;
      } // while (true)
      if (targetTerritory != null) {
        final int numAttacks = previouslyAttackedTerritories.getOrDefault(targetTerritory, 0);
        previouslyAttackedTerritories.put(targetTerritory, numAttacks + 1);
      }
    }
  }

  /**
   * Fire rockets which have been previously targeted (if any), or for Sequentially Targeted rockets
   * target them too.
   */
  public void fireRockets(final IDelegateBridge bridge) {
    if (needToFindRocketTargets) {
      findRocketTargetsAndFireIfNeeded(bridge, true);
    } else {
      for (final Territory attackingFrom : attackingFromTerritories) {
        // Roll dice for the rocket attack damage and apply it
        fireRocket(bridge, bridge.getData(), attackingFrom, attackedTerritories.get(attackingFrom));
      }
    }
  }

  static Set<Territory> getTerritoriesWithRockets(final GameData data, final GamePlayer player) {
    final Set<Territory> territories = new HashSet<>();
    final Predicate<Unit> ownedRockets = rocketMatch(player);
    final BattleTracker tracker = AbstractMoveDelegate.getBattleTracker(data);
    for (final Territory current : data.getMap()) {
      if (tracker.wasConquered(current)) {
        continue;
      }
      if (current.anyUnitsMatch(ownedRockets)) {
        territories.add(current);
      }
    }
    return territories;
  }

  private static Predicate<Unit> rocketMatch(final GamePlayer player) {
    return Matches.unitIsRocket()
        .and(Matches.unitIsOwnedBy(player))
        .and(Matches.unitIsNotDisabled())
        .and(Matches.unitIsBeingTransported().negate())
        .and(Matches.unitIsSubmerged().negate())
        .and(Matches.unitHasNotMoved());
  }

  private static Set<Territory> getTargetsWithinRange(
      final Territory territory, final GameState data, final GamePlayer player) {
    final int maxDistance = data.getTechTracker().getRocketDistance(player);
    final Set<Territory> hasFactory = new HashSet<>();
    final Predicate<Territory> allowed =
        PredicateBuilder.of(Matches.territoryAllowsRocketsCanFlyOver(player))
            .andIf(
                !Properties.getRocketsCanFlyOverImpassables(data.getProperties()),
                Matches.territoryIsNotImpassable())
            .build();
    final Collection<Territory> possible =
        data.getMap().getNeighbors(territory, maxDistance, allowed);
    final Predicate<Unit> attackableUnits =
        Matches.enemyUnit(player).and(Matches.unitIsBeingTransported().negate());
    for (final Territory current : possible) {
      final Optional<Route> optionalRoute = data.getMap().getRoute(territory, current, allowed);
      if (optionalRoute.isPresent()
          && optionalRoute.get().numberOfSteps() <= maxDistance
          && current.anyUnitsMatch(
              attackableUnits.and(Matches.unitIsAtMaxDamageOrNotCanBeDamaged(current).negate()))) {
        hasFactory.add(current);
      }
    }
    return hasFactory;
  }

  private static Territory getTarget(
      final Collection<Territory> targets, final IDelegateBridge bridge, final Territory from) {
    // ask even if there is only once choice, that will allow the user to not attack if he doesn't
    // want to
    return bridge.getRemotePlayer().whereShouldRocketsAttack(targets, from);
  }

  private void fireRocket(
      final IDelegateBridge bridge,
      final GameData data,
      final Territory attackFrom,
      final Territory attackedTerritory) {
    final GamePlayer player = bridge.getGamePlayer();
    final GamePlayer attacked = attackedTerritory.getOwner();
    final Resource pus = data.getResourceList().getResource(Constants.PUS);
    final boolean damageFromBombingDoneToUnits =
        Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data.getProperties());
    // unit damage vs territory damage
    final Collection<Unit> enemyUnits =
        attackedTerritory.getMatches(
            Matches.enemyUnit(player).and(Matches.unitIsBeingTransported().negate()));
    final Collection<Unit> enemyTargetsTotal =
        CollectionUtils.getMatches(
            enemyUnits, Matches.unitIsAtMaxDamageOrNotCanBeDamaged(attackedTerritory).negate());
    final Collection<Unit> rockets;
    final int numberOfAttacks;
    // attackFrom could be null if WW2V1
    if (attackFrom == null) {
      rockets = List.of();
      numberOfAttacks = 1;
    } else {
      rockets = CollectionUtils.getMatches(attackFrom.getUnits(), rocketMatch(player));
      int rocketDiceNumber = 0;
      for (final Unit u : rockets) {
        rocketDiceNumber += data.getTechTracker().getRocketDiceNumber(u.getOwner(), u.getType());
      }
      numberOfAttacks =
          Math.min(rocketDiceNumber, data.getTechTracker().getRocketNumberPerTerritory(player));
    }
    if (numberOfAttacks <= 0) {
      return;
    }
    final String transcript;
    final boolean doNotUseBombingBonus =
        !Properties.getUseBombingMaxDiceSidesAndBonus(data.getProperties()) || attackFrom == null;
    int cost = 0;
    if (!Properties.getLowLuckDamageOnly(data.getProperties())) {
      if (doNotUseBombingBonus) {
        // no low luck, and no bonus, so just roll based on the map's dice sides
        final int[] rolls =
            bridge.getRandom(
                data.getDiceSides(),
                numberOfAttacks,
                player,
                DiceType.BOMBING,
                "Rocket fired by " + player.getName() + " at " + attacked.getName());
        for (final int r : rolls) {
          // we are zero based
          cost += r + 1;
        }
        transcript =
            "Rockets "
                + (attackFrom == null ? "" : "in " + attackFrom.getName())
                + " roll: "
                + MyFormatter.asDice(rolls);
      } else {
        // we must use bombing bonus
        int highestMaxDice = 0;
        int highestBonus = 0;
        final int diceSides = data.getDiceSides();
        for (final Unit u : rockets) {
          final UnitAttachment ua = u.getUnitAttachment();
          int maxDice = ua.getBombingMaxDieSides();
          final int bonus = ua.getBombingBonus();
          // both could be -1, meaning they were not set. if they were not set, then we use default
          // dice sides for the
          // map, and zero for the bonus.
          if (maxDice < 0) {
            maxDice = diceSides;
          }
          // we only roll once for rockets, so if there are other rockets here we just roll for the
          // best rocket
          if ((bonus + ((maxDice + 1) / 2)) > (highestBonus + ((highestMaxDice + 1) / 2))) {
            highestMaxDice = maxDice;
            highestBonus = bonus;
          }
        }
        // now we roll, or don't if there is nothing to roll.
        if (highestMaxDice > 0) {
          final int[] rolls =
              bridge.getRandom(
                  highestMaxDice,
                  numberOfAttacks,
                  player,
                  DiceType.BOMBING,
                  "Rocket fired by " + player.getName() + " at " + attacked.getName());
          for (int i = 0; i < rolls.length; i++) {
            final int r = Math.max(-1, rolls[i] + highestBonus);
            rolls[i] = r;
            // we are zero based
            cost += r + 1;
          }
          transcript = "Rockets in " + attackFrom.getName() + " roll: " + MyFormatter.asDice(rolls);
        } else {
          cost = highestBonus * numberOfAttacks;
          transcript =
              "Rockets in "
                  + attackFrom.getName()
                  + " do "
                  + highestBonus
                  + " damage for each rocket";
        }
      }
    } else { // Low luck
      if (doNotUseBombingBonus) {
        // no bonus, so just roll based on the map's dice sides, but modify for LL
        final int maxDice = (data.getDiceSides() + 1) / 3;
        final int bonus = (data.getDiceSides() + 1) / 3;
        final int[] rolls =
            bridge.getRandom(
                maxDice,
                numberOfAttacks,
                player,
                DiceType.BOMBING,
                "Rocket fired by " + player.getName() + " at " + attacked.getName());
        for (int i = 0; i < rolls.length; i++) {
          final int r = rolls[i] + bonus;
          rolls[i] = r;
          // we are zero based
          cost += r + 1;
        }
        transcript =
            "Rockets "
                + (attackFrom == null ? "" : "in " + attackFrom.getName())
                + " roll: "
                + MyFormatter.asDice(rolls);
      } else {
        int highestMaxDice = 0;
        int highestBonus = 0;
        final int diceSides = data.getDiceSides();
        for (final Unit rocket : rockets) {
          final UnitAttachment ua = rocket.getUnitAttachment();
          int maxDice = ua.getBombingMaxDieSides();
          int bonus = ua.getBombingBonus();
          // both could be -1, meaning they were not set. if they were not set, then we use default
          // dice sides for the
          // map, and zero for the bonus.
          if (maxDice < 0) {
            maxDice = diceSides;
          }
          // now, regardless of whether they were set or not, we have to apply "low luck" to them,
          // meaning in this case
          // that we reduce the luck by 2/3.
          if (maxDice >= 5) {
            bonus += (maxDice + 1) / 3;
            maxDice = (maxDice + 1) / 3;
          }
          // we only roll once for rockets, so if there are other rockets here we just roll for the
          // best rocket
          if ((bonus + ((maxDice + 1) / 2)) > (highestBonus + ((highestMaxDice + 1) / 2))) {
            highestMaxDice = maxDice;
            highestBonus = bonus;
          }
        }
        // now we roll, or don't if there is nothing to roll.
        if (highestMaxDice > 0) {
          final int[] rolls =
              bridge.getRandom(
                  highestMaxDice,
                  numberOfAttacks,
                  player,
                  DiceType.BOMBING,
                  "Rocket fired by " + player.getName() + " at " + attacked.getName());
          for (int i = 0; i < rolls.length; i++) {
            final int r = Math.max(-1, rolls[i] + highestBonus);
            rolls[i] = r;
            // we are zero based
            cost += r + 1;
          }
          transcript = "Rockets in " + attackFrom.getName() + " roll: " + MyFormatter.asDice(rolls);
        } else {
          cost = highestBonus * numberOfAttacks;
          transcript =
              "Rockets in "
                  + attackFrom.getName()
                  + " do "
                  + highestBonus
                  + " damage for each rocket";
        }
      }
    }
    int territoryProduction = TerritoryAttachment.getProduction(attackedTerritory);
    final Unit unit = attackFrom == null ? null : attackedUnits.get(attackFrom);
    if (damageFromBombingDoneToUnits && attackFrom != null) {
      final int damageLimit = unit.getHowMuchMoreDamageCanThisUnitTake(attackedTerritory);
      cost = Math.max(0, Math.min(cost, damageLimit));
      final int totalDamage = unit.getUnitDamage() + cost;
      // Record production lost
      // DelegateFinder.moveDelegate(data).PUsLost(attackedTerritory, cost);
      // apply the hits to the targets
      final IntegerMap<Unit> damageMap = new IntegerMap<>();
      damageMap.put(unit, totalDamage);
      bridge.addChange(ChangeFactory.bombingUnitDamage(damageMap, List.of(attackedTerritory)));
      // attackedTerritory.notifyChanged();
      // in WW2V2, limit rocket attack cost to production value of factory.
    } else if (Properties.getWW2V2(data.getProperties())
        || Properties.getLimitRocketAndSbrDamageToProduction(data.getProperties())) {
      // If we are limiting total PUs lost then take that into account
      if (Properties.getPuCap(data.getProperties())
          || Properties.getLimitRocketDamagePerTurn(data.getProperties())) {
        final int alreadyLost = data.getMoveDelegate().pusAlreadyLost(attackedTerritory);
        territoryProduction -= alreadyLost;
        territoryProduction = Math.max(0, territoryProduction);
      }
      if (cost > territoryProduction) {
        cost = territoryProduction;
      }
    }
    // Record the PUs lost
    data.getMoveDelegate().pusLost(attackedTerritory, cost);
    if (damageFromBombingDoneToUnits && unit != null) {
      getRemote(bridge)
          .reportMessage(
              "Rocket attack in "
                  + attackedTerritory.getName()
                  + " does "
                  + cost
                  + " damage to "
                  + unit,
              "Rocket attack in "
                  + attackedTerritory.getName()
                  + " does "
                  + cost
                  + " damage to "
                  + unit);
      bridge
          .getHistoryWriter()
          .startEvent(
              "Rocket attack in "
                  + attackedTerritory.getName()
                  + " does "
                  + cost
                  + " damage to "
                  + unit);
    } else {
      cost *= Properties.getPuMultiplier(data.getProperties());
      getRemote(bridge)
          .reportMessage(
              "Rocket attack in " + attackedTerritory.getName() + " costs: " + cost,
              "Rocket attack in " + attackedTerritory.getName() + " costs: " + cost);
      // Trying to remove more PUs than the victim has is A Bad Thing[tm]
      final int availForRemoval = attacked.getResources().getQuantity(pus);
      if (cost > availForRemoval) {
        cost = availForRemoval;
      }
      final String transcriptText =
          attacked.getName() + " lost " + cost + " PUs to rocket attack by " + player.getName();
      bridge.getHistoryWriter().startEvent(transcriptText);
      final Change rocketCharge = ChangeFactory.changeResourcesChange(attacked, pus, -cost);
      bridge.addChange(rocketCharge);
    }
    bridge
        .getHistoryWriter()
        .addChildToEvent(transcript, attackFrom == null ? null : new ArrayList<>(rockets));
    // this is null in WW2V1
    if (attackFrom != null) {
      if (!rockets.isEmpty()) {
        // TODO: only a certain number fired...
        final Change change =
            ChangeFactory.markNoMovementChange(Set.of(CollectionUtils.getAny(rockets)));
        bridge.addChange(change);
      } else {
        throw new IllegalStateException("No rockets?" + attackFrom.getUnits());
      }
    }
    // kill any units that can die if they have reached max damage (veqryn)
    final Collection<Unit> targetUnitCol = unit == null ? enemyTargetsTotal : Set.of(unit);
    if (targetUnitCol.stream().anyMatch(Matches.unitCanDieFromReachingMaxDamage())) {
      final List<Unit> unitsCanDie =
          CollectionUtils.getMatches(targetUnitCol, Matches.unitCanDieFromReachingMaxDamage());
      unitsCanDie.retainAll(
          CollectionUtils.getMatches(
              unitsCanDie, Matches.unitIsAtMaxDamageOrNotCanBeDamaged(attackedTerritory)));
      if (!unitsCanDie.isEmpty()) {
        final Change removeDead = ChangeFactory.removeUnits(attackedTerritory, unitsCanDie);
        final String transcriptText =
            MyFormatter.unitsToText(unitsCanDie) + " lost in " + attackedTerritory.getName();
        bridge.getHistoryWriter().addChildToEvent(transcriptText, unitsCanDie);
        bridge.addChange(removeDead);
      }
    }
    // play a sound
    if (cost > 0) {
      bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BOMBING_ROCKET, player);
    }
  }

  private static Player getRemote(final IDelegateBridge bridge) {
    return bridge.getRemotePlayer();
  }
}
