package games.strategy.triplea.delegate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.TechAbilityAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleAPlayer;
import games.strategy.util.CollectionUtils;
import games.strategy.util.IntegerMap;
import games.strategy.util.PredicateBuilder;

/**
 * Logic to fire rockets.
 */
public class RocketsFireHelper {
  private static boolean isWW2V2(final GameData data) {
    return Properties.getWW2V2(data);
  }

  private static boolean isAllRocketsAttack(final GameData data) {
    return Properties.getAllRocketsAttack(data);
  }

  private static boolean isRocketsCanFlyOverImpassables(final GameData data) {
    return Properties.getRocketsCanFlyOverImpassables(data);
  }

  private static boolean isDamageFromBombingDoneToUnitsInsteadOfTerritories(final GameData data) {
    return Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data);
  }

  private static boolean isRocketAttacksPerFactoryInfinite(final GameData data) {
    return Properties.getRocketAttacksPerFactoryInfinite(data);
  }

  private static boolean isPuCap(final GameData data) {
    return Properties.getPuCap(data);
  }

  private static boolean isLimitRocketDamagePerTurn(final GameData data) {
    return Properties.getLimitRocketDamagePerTurn(data);
  }

  private static boolean isLimitRocketDamageToProduction(final GameData data) {
    return Properties.getLimitRocketAndSbrDamageToProduction(data);
  }

  static void fireRockets(final IDelegateBridge bridge, final PlayerID player) {
    final GameData data = bridge.getData();
    final Set<Territory> rocketTerritories = getTerritoriesWithRockets(data, player);
    if (rocketTerritories.isEmpty()) {
      bridge.getHistoryWriter().startEvent(player.getName() + " has no rockets to fire");
      return;
    }
    if (isWW2V2(data) || isAllRocketsAttack(data)) {
      fireWW2V2(bridge, player, rocketTerritories);
    } else {
      fireWW2V1(bridge, player, rocketTerritories);
    }
  }

  private static void fireWW2V2(
      final IDelegateBridge bridge,
      final PlayerID player,
      final Set<Territory> rocketTerritories) {
    final GameData data = bridge.getData();
    final Set<Territory> attackedTerritories = new HashSet<>();
    final Map<Territory, Territory> attackingFromTerritories = new LinkedHashMap<>();
    final boolean oneAttackPerTerritory = !isRocketAttacksPerFactoryInfinite(data);
    for (final Territory territory : rocketTerritories) {
      final Set<Territory> targets = getTargetsWithinRange(territory, data, player);
      if (oneAttackPerTerritory) {
        targets.removeAll(attackedTerritories);
      }
      if (targets.isEmpty()) {
        continue;
      }
      // Ask the user where each rocket launcher should target.
      final Territory target = getTarget(targets, bridge, territory);
      if (target != null) {
        attackedTerritories.add(target);
        attackingFromTerritories.put(target, territory);
      }
    }
    for (final Territory target : attackedTerritories) {
      // Roll dice for the rocket attack damage and apply it
      fireRocket(player, target, bridge, attackingFromTerritories.get(target));
    }
  }

  /** In this rule set, each player only gets one rocket attack per turn. */
  private static void fireWW2V1(
      final IDelegateBridge bridge,
      final PlayerID player,
      final Set<Territory> rocketTerritories) {
    final GameData data = bridge.getData();
    final Set<Territory> targets = new HashSet<>();
    for (final Territory territory : rocketTerritories) {
      targets.addAll(getTargetsWithinRange(territory, data, player));
    }
    if (targets.isEmpty()) {
      bridge.getHistoryWriter().startEvent(player.getName() + " has no targets to attack with rockets");
      return;
    }
    final Territory attacked = getTarget(targets, bridge, null);

    if (attacked != null) {
      fireRocket(player, attacked, bridge, null);
    }
  }

  static Set<Territory> getTerritoriesWithRockets(final GameData data, final PlayerID player) {
    final Set<Territory> territories = new HashSet<>();
    final Predicate<Unit> ownedRockets = rocketMatch(player);
    final BattleTracker tracker = AbstractMoveDelegate.getBattleTracker(data);
    for (final Territory current : data.getMap()) {
      if (tracker.wasConquered(current)) {
        continue;
      }
      if (current.getUnits().anyMatch(ownedRockets)) {
        territories.add(current);
      }
    }
    return territories;
  }

  private static Predicate<Unit> rocketMatch(final PlayerID player) {
    return Matches.unitIsRocket()
        .and(Matches.unitIsOwnedBy(player))
        .and(Matches.unitIsNotDisabled())
        .and(Matches.unitIsBeingTransported().negate())
        .and(Matches.unitIsSubmerged().negate())
        .and(Matches.unitHasNotMoved());
  }

  private static Set<Territory> getTargetsWithinRange(final Territory territory, final GameData data,
      final PlayerID player) {
    final int maxDistance = TechAbilityAttachment.getRocketDistance(player, data);
    final Collection<Territory> possible = data.getMap().getNeighbors(territory, maxDistance);
    final Set<Territory> hasFactory = new HashSet<>();
    final Predicate<Territory> allowed = PredicateBuilder
        .of(Matches.territoryAllowsRocketsCanFlyOver(player, data))
        .andIf(!isRocketsCanFlyOverImpassables(data), Matches.territoryIsNotImpassable())
        .build();
    final Predicate<Unit> attackableUnits = Matches.enemyUnit(player, data)
        .and(Matches.unitIsBeingTransported().negate());
    for (final Territory current : possible) {
      final Route route = data.getMap().getRoute(territory, current, allowed);
      if ((route != null) && (route.numberOfSteps() <= maxDistance)) {
        if (current.getUnits().anyMatch(attackableUnits
            .and(Matches.unitIsAtMaxDamageOrNotCanBeDamaged(current).negate()))) {
          hasFactory.add(current);
        }
      }
    }
    return hasFactory;
  }

  private static Territory getTarget(final Collection<Territory> targets, final IDelegateBridge bridge,
      final Territory from) {
    // ask even if there is only once choice, that will allow the user to not attack if he doesn't want to
    return ((ITripleAPlayer) bridge.getRemotePlayer()).whereShouldRocketsAttack(targets, from);
  }

  private static void fireRocket(final PlayerID player, final Territory attackedTerritory, final IDelegateBridge bridge,
      final Territory attackFrom) {
    final GameData data = bridge.getData();
    final PlayerID attacked = attackedTerritory.getOwner();
    final Resource pus = data.getResourceList().getResource(Constants.PUS);
    final boolean damageFromBombingDoneToUnits = isDamageFromBombingDoneToUnitsInsteadOfTerritories(data);
    // unit damage vs territory damage
    final Collection<Unit> enemyUnits = attackedTerritory.getUnits().getMatches(
        Matches.enemyUnit(player, data).and(Matches.unitIsBeingTransported().negate()));
    final Collection<Unit> enemyTargetsTotal =
        CollectionUtils.getMatches(enemyUnits, Matches.unitIsAtMaxDamageOrNotCanBeDamaged(attackedTerritory).negate());
    final Collection<Unit> rockets;
    // attackFrom could be null if WW2V1
    if (attackFrom == null) {
      rockets = null;
    } else {
      rockets = new ArrayList<>(CollectionUtils.getMatches(attackFrom.getUnits().getUnits(), rocketMatch(player)));
    }
    final int numberOfAttacks = ((rockets == null) ? 1
        : Math.min(TechAbilityAttachment.getRocketNumberPerTerritory(player, data),
        TechAbilityAttachment.getRocketDiceNumber(rockets, data)));
    if (numberOfAttacks <= 0) {
      return;
    }
    final Collection<Unit> targets = new ArrayList<>();
    if (damageFromBombingDoneToUnits) {
      // TODO: rockets needs to be completely redone to allow for multiple rockets to fire at different targets, etc
      // etc.
      final HashSet<UnitType> legalTargetsForTheseRockets = new HashSet<>();
      if (rockets == null) {
        legalTargetsForTheseRockets.addAll(data.getUnitTypeList().getAllUnitTypes());
      } else {
        // a hack for now, we let the rockets fire at anyone who could be targetted by any rocket
        for (final Unit r : rockets) {
          legalTargetsForTheseRockets.addAll(UnitAttachment.get(r.getType()).getBombingTargets(data));
        }
      }
      final Collection<Unit> enemyTargets =
          CollectionUtils.getMatches(enemyTargetsTotal, Matches.unitIsOfTypes(legalTargetsForTheseRockets));
      if (enemyTargets.isEmpty()) {
        // TODO: this sucks
        return;
      }
      Unit target = null;
      if (enemyTargets.size() == 1) {
        target = enemyTargets.iterator().next();
      } else {
        while (target == null) {
          final ITripleAPlayer iplayer = (ITripleAPlayer) bridge.getRemotePlayer(player);
          target = iplayer.whatShouldBomberBomb(attackedTerritory, enemyTargets, rockets);
        }
      }
      if (target == null) {
        throw new IllegalStateException("No Targets in " + attackedTerritory.getName());
      }
      targets.add(target);
    }
    final boolean doNotUseBombingBonus =
        !Properties.getUseBombingMaxDiceSidesAndBonus(data) || (rockets == null);
    int cost = 0;
    final String transcript;
    if (!Properties.getLowLuckDamageOnly(data)) {
      if (doNotUseBombingBonus || (rockets == null)) {
        // no low luck, and no bonus, so just roll based on the map's dice sides
        final int[] rolls = bridge.getRandom(data.getDiceSides(), numberOfAttacks, player, DiceType.BOMBING,
            "Rocket fired by " + player.getName() + " at " + attacked.getName());
        for (final int r : rolls) {
          // we are zero based
          cost += r + 1;
        }
        transcript = "Rockets " + (attackFrom == null ? "" : "in " + attackFrom.getName()) + " roll: "
            + MyFormatter.asDice(rolls);
      } else {
        // we must use bombing bonus
        int highestMaxDice = 0;
        int highestBonus = 0;
        final int diceSides = data.getDiceSides();
        for (final Unit u : rockets) {
          final UnitAttachment ua = UnitAttachment.get(u.getType());
          int maxDice = ua.getBombingMaxDieSides();
          final int bonus = ua.getBombingBonus();
          // both could be -1, meaning they were not set. if they were not set, then we use default dice sides for the
          // map, and zero for the bonus.
          if (maxDice < 0) {
            maxDice = diceSides;
          }
          // we only roll once for rockets, so if there are other rockets here we just roll for the best rocket
          if ((bonus + ((maxDice + 1) / 2)) > (highestBonus + ((highestMaxDice + 1) / 2))) {
            highestMaxDice = maxDice;
            highestBonus = bonus;
          }
        }
        // now we roll, or don't if there is nothing to roll.
        if (highestMaxDice > 0) {
          final int[] rolls = bridge.getRandom(highestMaxDice, numberOfAttacks, player, DiceType.BOMBING,
              "Rocket fired by " + player.getName() + " at " + attacked.getName());
          for (int i = 0; i < rolls.length; i++) {
            final int r = Math.max(-1, rolls[i] + highestBonus);
            rolls[i] = r;
            // we are zero based
            cost += r + 1;
          }
          transcript = "Rockets " + (attackFrom == null ? "" : "in " + attackFrom.getName()) + " roll: "
              + MyFormatter.asDice(rolls);
        } else {
          cost = highestBonus * numberOfAttacks;
          transcript = "Rockets " + (attackFrom == null ? "" : "in " + attackFrom.getName()) + " do " + highestBonus
              + " damage for each rocket";
        }
      }
    } else {
      if (doNotUseBombingBonus || (rockets == null)) {
        // no bonus, so just roll based on the map's dice sides, but modify for LL
        final int maxDice = (data.getDiceSides() + 1) / 3;
        final int bonus = (data.getDiceSides() + 1) / 3;
        final int[] rolls = bridge.getRandom(maxDice, numberOfAttacks, player, DiceType.BOMBING,
            "Rocket fired by " + player.getName() + " at " + attacked.getName());
        for (int i = 0; i < rolls.length; i++) {
          final int r = rolls[i] + bonus;
          rolls[i] = r;
          // we are zero based
          cost += r + 1;
        }
        transcript = "Rockets " + (attackFrom == null ? "" : "in " + attackFrom.getName()) + " roll: "
            + MyFormatter.asDice(rolls);
      } else {
        int highestMaxDice = 0;
        int highestBonus = 0;
        final int diceSides = data.getDiceSides();
        for (final Unit u : rockets) {
          final UnitAttachment ua = UnitAttachment.get(u.getType());
          int maxDice = ua.getBombingMaxDieSides();
          int bonus = ua.getBombingBonus();
          // both could be -1, meaning they were not set. if they were not set, then we use default dice sides for the
          // map, and zero for the bonus.
          if ((maxDice < 0) || doNotUseBombingBonus) {
            maxDice = diceSides;
          }
          if (doNotUseBombingBonus) {
            bonus = 0;
          }
          // now, regardless of whether they were set or not, we have to apply "low luck" to them, meaning in this case
          // that we reduce the
          // luck by 2/3.
          if (maxDice >= 5) {
            bonus += (maxDice + 1) / 3;
            maxDice = (maxDice + 1) / 3;
          }
          // we only roll once for rockets, so if there are other rockets here we just roll for the best rocket
          if ((bonus + ((maxDice + 1) / 2)) > (highestBonus + ((highestMaxDice + 1) / 2))) {
            highestMaxDice = maxDice;
            highestBonus = bonus;
          }
        }
        // now we roll, or don't if there is nothing to roll.
        if (highestMaxDice > 0) {
          final int[] rolls = bridge.getRandom(highestMaxDice, numberOfAttacks, player, DiceType.BOMBING,
              "Rocket fired by " + player.getName() + " at " + attacked.getName());
          for (int i = 0; i < rolls.length; i++) {
            final int r = Math.max(-1, rolls[i] + highestBonus);
            rolls[i] = r;
            // we are zero based
            cost += r + 1;
          }
          transcript = "Rockets " + (attackFrom == null ? "" : "in " + attackFrom.getName()) + " roll: "
              + MyFormatter.asDice(rolls);
        } else {
          cost = highestBonus * numberOfAttacks;
          transcript = "Rockets " + (attackFrom == null ? "" : "in " + attackFrom.getName()) + " do " + highestBonus
              + " damage for each rocket";
        }
      }
    }
    int territoryProduction = TerritoryAttachment.getProduction(attackedTerritory);
    if (damageFromBombingDoneToUnits && !targets.isEmpty()) {
      // we are doing damage to 'target', not to the territory
      final Unit target = targets.iterator().next();
      // UnitAttachment ua = UnitAttachment.get(target.getType());
      final TripleAUnit taUnit = (TripleAUnit) target;
      final int damageLimit = taUnit.getHowMuchMoreDamageCanThisUnitTake(target, attackedTerritory);
      cost = Math.max(0, Math.min(cost, damageLimit));
      final int totalDamage = taUnit.getUnitDamage() + cost;
      // Record production lost
      // DelegateFinder.moveDelegate(data).PUsLost(attackedTerritory, cost);
      // apply the hits to the targets
      final IntegerMap<Unit> damageMap = new IntegerMap<>();
      damageMap.put(target, totalDamage);
      bridge.addChange(ChangeFactory.bombingUnitDamage(damageMap));
      // attackedTerritory.notifyChanged();
      // in WW2V2, limit rocket attack cost to production value of factory.
    } else if (isWW2V2(data) || isLimitRocketDamageToProduction(data)) {
      // If we are limiting total PUs lost then take that into account
      if (isPuCap(data) || isLimitRocketDamagePerTurn(data)) {
        final int alreadyLost = DelegateFinder.moveDelegate(data).pusAlreadyLost(attackedTerritory);
        territoryProduction -= alreadyLost;
        territoryProduction = Math.max(0, territoryProduction);
      }
      if (cost > territoryProduction) {
        cost = territoryProduction;
      }
    }
    // Record the PUs lost
    DelegateFinder.moveDelegate(data).pusLost(attackedTerritory, cost);
    if (damageFromBombingDoneToUnits && !targets.isEmpty()) {
      getRemote(bridge).reportMessage(
          "Rocket attack in " + attackedTerritory.getName() + " does " + cost + " damage to "
              + targets.iterator().next(),
          "Rocket attack in " + attackedTerritory.getName() + " does " + cost + " damage to "
              + targets.iterator().next());
      bridge.getHistoryWriter().startEvent("Rocket attack in " + attackedTerritory.getName() + " does " + cost
          + " damage to " + targets.iterator().next());
    } else {
      cost *= Properties.getPuMultiplier(data);
      getRemote(bridge).reportMessage("Rocket attack in " + attackedTerritory.getName() + " costs:" + cost,
          "Rocket attack in " + attackedTerritory.getName() + " costs:" + cost);
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
    bridge.getHistoryWriter().addChildToEvent(transcript, (rockets == null) ? null : new ArrayList<>(rockets));
    // this is null in WW2V1
    if (attackFrom != null) {
      if ((rockets != null) && !rockets.isEmpty()) {
        // TODO: only a certain number fired...
        final Change change = ChangeFactory.markNoMovementChange(Collections.singleton(rockets.iterator().next()));
        bridge.addChange(change);
      } else {
        throw new IllegalStateException("No rockets?" + attackFrom.getUnits().getUnits());
      }
    }
    // kill any units that can die if they have reached max damage (veqryn)
    if (targets.stream().anyMatch(Matches.unitCanDieFromReachingMaxDamage())) {
      final List<Unit> unitsCanDie = CollectionUtils.getMatches(targets, Matches.unitCanDieFromReachingMaxDamage());
      unitsCanDie.retainAll(
          CollectionUtils.getMatches(unitsCanDie, Matches.unitIsAtMaxDamageOrNotCanBeDamaged(attackedTerritory)));
      if (!unitsCanDie.isEmpty()) {
        final Change removeDead = ChangeFactory.removeUnits(attackedTerritory, unitsCanDie);
        final String transcriptText = MyFormatter.unitsToText(unitsCanDie) + " lost in " + attackedTerritory.getName();
        bridge.getHistoryWriter().addChildToEvent(transcriptText, unitsCanDie);
        bridge.addChange(removeDead);
      }
    }
    // play a sound
    if (cost > 0) {
      bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BOMBING_ROCKET, player);
    }
  }

  private static ITripleAPlayer getRemote(final IDelegateBridge bridge) {
    return (ITripleAPlayer) bridge.getRemotePlayer();
  }
}
