package games.strategy.triplea.ai.tree;

import com.google.common.base.Preconditions;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.TechAbilityAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import games.strategy.triplea.delegate.battle.TargetGroup;
import games.strategy.triplea.delegate.battle.casualty.CasualtyUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import org.apache.commons.math3.distribution.BinomialDistribution;
import org.triplea.java.PredicateBuilder;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Triple;
import org.triplea.util.Tuple;

public class BattleStep {

  // enable this to keep children in memory for analysis
  // watch out for running out of memory, though
  private static final boolean DEBUG = false;

  private static final int MAX_ROUNDS = 16;
  private static final double IGNORE_BRANCH_PROBABILITY = 0.005;

  enum Type {
    AA_ATTACKER,
    AA_DEFENDER,
    SUB_ATTACKER,
    SUB_DEFENDER,
    ATTACKER,
    DEFENDER;

    // TODO - the order of subs is configurable (see MustFightBattle::addFightSteps)
    Type nextType() {
      switch (this) {
        case AA_ATTACKER:
          return AA_DEFENDER;
        case AA_DEFENDER:
          return SUB_ATTACKER;
        case SUB_ATTACKER:
          return SUB_DEFENDER;
        case SUB_DEFENDER:
          return ATTACKER;
        case ATTACKER:
          return DEFENDER;
        case DEFENDER:
          return AA_ATTACKER;
        default:
          Preconditions.checkArgument(false, "A type has been forgotten in swapSides.");
          return AA_ATTACKER;
      }
    }
  }

  private final List<BattleStep> children = new ArrayList<>();

  @Getter(AccessLevel.PACKAGE)
  private BattleStep parent;

  private double probability = 0.0;

  @Getter(AccessLevel.PACKAGE)
  private double winProbability = 0;

  @Getter(AccessLevel.PACKAGE)
  private double loseProbability = 0;

  @Getter(AccessLevel.PACKAGE)
  private double tieProbability = 0;

  @Getter(AccessLevel.PACKAGE)
  private double badProbability = 0;

  @Getter(AccessLevel.PACKAGE)
  private boolean hasResult = false;

  @Getter(AccessLevel.PACKAGE)
  private StepUnits averageUnits;

  @Getter(AccessLevel.PACKAGE)
  private double averageRounds;

  private final StepUnits units;
  private final GamePlayer player;
  private final int round;
  private final Parameters parameters;
  private final Map<StepUnits, BattleStep> nodeCache;
  private final Map<Integer, Map<Integer, double[]>> calculatedProbabilityCache;

  private List<StepUnits> cachedDefenderOutcomes = null;

  BattleStep(
      final StepUnits units,
      final GamePlayer player,
      final int round,
      final Parameters parameters) {
    this(units, player, round, parameters, new HashMap<>(), new HashMap<>());
  }

  private BattleStep(
      final StepUnits units,
      final GamePlayer player,
      final int round,
      final Parameters parameters,
      final Map<StepUnits, BattleStep> nodeCache,
      final Map<Integer, Map<Integer, double[]>> calculatedProbabilityCache) {
    this.units = units;
    this.player = player;
    this.round = round;
    this.parameters = parameters;
    this.nodeCache = nodeCache;
    this.calculatedProbabilityCache = calculatedProbabilityCache;
  }

  @Builder(toBuilder = true)
  @Value
  static class Parameters {
    GameData data;
    Territory location;
    Collection<TerritoryEffect> territoryEffects;
    // TODO: Determine amphibious battles
    boolean isAmphibious = false;
    Collection<Unit> amphibiousLandAttackers = List.of();
  }

  // Taken from MustFightBattle::updateOffensiveAaUnits
  private Tuple<List<Unit>, List<String>> getAaUnits(final StepUnits units) {
    final Map<String, Set<UnitType>> airborneTechTargetsAllowed;
    if (units.getType() == Type.AA_DEFENDER) {
      airborneTechTargetsAllowed =
          TechAbilityAttachment.getAirborneTargettedByAa(units.getEnemy(), parameters.data);
    } else {
      airborneTechTargetsAllowed = Map.of();
    }
    // no airborne targets for offensive aa
    final List<Unit> aaUnits =
        CollectionUtils.getMatches(
            units.getAliveOrWaitingToDieFriendly(),
            Matches.unitIsAaThatCanFire(
                units.getAliveOrWaitingToDieEnemy(),
                airborneTechTargetsAllowed,
                units.getEnemy(),
                Matches.unitIsAaForCombatOnly(),
                round,
                units.getType() == Type.AA_DEFENDER,
                parameters.data));
    // comes ordered alphabetically
    final List<String> aaUnitTypes = UnitAttachment.getAllOfTypeAas(aaUnits);
    // stacks are backwards
    Collections.reverse(aaUnitTypes);

    return Tuple.of(aaUnits, aaUnitTypes);
  }

  void addParent(final BattleStep parent) {
    this.parent = parent;
  }

  void addChild(final BattleStep child) {
    child.addParent(this);
    children.add(child);
  }

  // TODO:
  //  handle bombardment
  //   - only first round and causalities can't fire back unless true ==
  // Properties.getNavalBombardCasualtiesReturnFire(gameData)
  //  handle subs first strike
  //  - handle destroyer being present or not to negate first strike
  void calculateBattle(final StepUnits units, final GamePlayer otherPlayer) {
    // AA attacks are the beginning of the round
    final int nextRound = Type.ATTACKER == units.getType() ? round + 1 : round;
    final StepUnits aliveOrInjuredUnits;
    if (units.getType() == Type.AA_ATTACKER) {
      // remove the units that were injured in the previous fight
      aliveOrInjuredUnits = units.removeWaitingToDie();
      if (checkEndOfBranch(aliveOrInjuredUnits)) {
        return;
      }
    } else if (units.getType() == Type.SUB_ATTACKER) {
      // remove the units that were injured in the AA rounds
      aliveOrInjuredUnits = units.removeWaitingToDie();
      submergeSubsVsOnlyAir(aliveOrInjuredUnits);
    } else if (units.getType() == Type.SUB_DEFENDER) {
      aliveOrInjuredUnits = units;
    } else if (units.getType() == Type.ATTACKER) {
      aliveOrInjuredUnits = units;

      if (Properties.getTransportCasualtiesRestricted(parameters.getData())) {
        checkUndefendedTransports(units.getEnemy(), aliveOrInjuredUnits);
        checkForUnitsThatCanRollLeft(true, aliveOrInjuredUnits);
        checkForUnitsThatCanRollLeft(false, aliveOrInjuredUnits);
      }
    } else {
      aliveOrInjuredUnits = units;
    }

    // for a single ATTACKER level, all of the DEFENDERs have the same outcome
    // since units are only damaged, not killed outright
    // this allows copying the first defender outcomes to all of the other defenders.
    List<StepUnits> newCachedDefenderOutcomes = null;
    final List<StepUnits> children;
    if (units.getType() == Type.DEFENDER) {
      if (this.cachedDefenderOutcomes == null) {
        children = getFightOutcomes(new StepUnits(aliveOrInjuredUnits));
        this.cachedDefenderOutcomes = children;
      } else {
        children =
            this.cachedDefenderOutcomes.stream()
                .map(childUnits -> childUnits.mergeParent(units))
                .collect(Collectors.toList());
      }
    } else {
      children = getFightOutcomes(new StepUnits(aliveOrInjuredUnits));
    }
    averageUnits = new StepUnits(units);
    for (final StepUnits childUnits : children) {
      final BattleStep child =
          new BattleStep(
              childUnits,
              otherPlayer,
              nextRound,
              parameters,
              nodeCache,
              calculatedProbabilityCache);
      child.probability = childUnits.getProbability();

      if (child.probability < IGNORE_BRANCH_PROBABILITY) {
        // skip these children completely
        child.badProbability = 1.0;
        child.hasResult = true;
        if (DEBUG) {
          addChild(child);
        }
      } else {
        addChild(child);
        if (units.getType() == Type.ATTACKER && newCachedDefenderOutcomes != null) {
          child.cachedDefenderOutcomes = newCachedDefenderOutcomes;
        }
        child.calculateBattle(childUnits, player);
        averageUnits.updateUnitChances(child.averageUnits, child.probability);
        if (units.getType() == Type.ATTACKER && newCachedDefenderOutcomes == null) {
          newCachedDefenderOutcomes = child.cachedDefenderOutcomes;
        }
      }
      winProbability += child.winProbability * child.probability;
      loseProbability += child.loseProbability * child.probability;
      tieProbability += child.tieProbability * child.probability;
      badProbability += child.badProbability * child.probability;
      averageRounds += child.averageRounds * child.probability;
    }
    if (children.isEmpty()) {
      tieProbability = 1;
    }

    hasResult = true;
    // the children are no longer needed so clear them out to reduce memory usage
    if (!DEBUG) {
      this.children.clear();
    }
  }

  private List<StepUnits> getFightOutcomes(final StepUnits aliveOrInjuredUnits) {
    if (aliveOrInjuredUnits.getType() == Type.AA_ATTACKER
        || aliveOrInjuredUnits.getType() == Type.AA_DEFENDER) {
      return getAaFightOutcomes(aliveOrInjuredUnits);
    } else if (aliveOrInjuredUnits.getType() == Type.SUB_ATTACKER) {
      final MustFightBattle.ReturnFire returnFire =
          returnFireAgainstAttackingSubs(
              units.getAliveOrWaitingToDieFriendly(), units.getAliveOrWaitingToDieEnemy());
      return getRegularFightOutcomes(aliveOrInjuredUnits, Matches.unitIsFirstStrike(), returnFire);
    } else if (aliveOrInjuredUnits.getType() == Type.SUB_DEFENDER) {
      final MustFightBattle.ReturnFire returnFire =
          returnFireAgainstAttackingSubs(
              units.getAliveOrWaitingToDieEnemy(), units.getAliveOrWaitingToDieFriendly());
      return getRegularFightOutcomes(aliveOrInjuredUnits, Matches.unitIsFirstStrike(), returnFire);
    } else {
      return getRegularFightOutcomes(
          aliveOrInjuredUnits,
          Matches.unitIsFirstStrike().negate(),
          MustFightBattle.ReturnFire.ALL);
    }
  }

  private List<StepUnits> getRegularFightOutcomes(
      final StepUnits aliveOrInjuredUnits,
      final Predicate<Unit> firingUnitPredicate,
      final MustFightBattle.ReturnFire returnFire) {
    final Collection<Unit> allFiringUnits =
        CollectionUtils.getMatches(
            aliveOrInjuredUnits.getAliveOrWaitingToDieFriendly(), firingUnitPredicate);
    final Collection<Unit> allEnemyUnits = aliveOrInjuredUnits.getAliveOrWaitingToDieEnemy();
    final List<Triple<Collection<Unit>, Collection<Unit>, Boolean>> groupsAndTargets =
        new ArrayList<>();
    final List<TargetGroup> targetGroups =
        TargetGroup.newTargetGroups(allFiringUnits, allEnemyUnits);
    for (final TargetGroup targetGroup : targetGroups) {
      final Collection<Unit> firingUnits = targetGroup.getFiringUnits(allFiringUnits);
      final Collection<Unit> attackableUnits = targetGroup.getTargetUnits(allEnemyUnits);
      final Collection<Unit> targetUnits =
          CollectionUtils.getMatches(
              attackableUnits,
              PredicateBuilder.of(Matches.unitIsNotInfrastructure())
                  .andIf(
                      aliveOrInjuredUnits.getType() == Type.DEFENDER,
                      Matches.unitIsSuicideOnAttack().negate())
                  .andIf(
                      aliveOrInjuredUnits.getType() == Type.ATTACKER,
                      Matches.unitIsSuicideOnDefense().negate())
                  .build());

      if (targetUnits.isEmpty() || firingUnits.isEmpty()) {
        continue;
      }
      final List<Collection<Unit>> firingGroups = MustFightBattle.newFiringUnitGroups(firingUnits);
      for (final Collection<Unit> firingGroup : firingGroups) {
        final Boolean isSuicideOnHit = firingGroup.stream().anyMatch(Matches.unitIsSuicideOnHit());
        groupsAndTargets.add(Triple.of(firingGroup, targetUnits, isSuicideOnHit));
      }
    }
    final Map<StepUnits, StepUnits> outcomes = new HashMap<>();

    if (!groupsAndTargets.isEmpty()) {
      getTargetGroupFightOutcomes(
          outcomes,
          groupsAndTargets,
          groupsAndTargets.size() - 1,
          aliveOrInjuredUnits.swapSides(),
          aliveOrInjuredUnits.getType(),
          returnFire,
          true,
          1.0);
    } else {
      return List.of(new StepUnits(aliveOrInjuredUnits.swapSides(), 1.0));
    }

    final List<StepUnits> units = new ArrayList<>(outcomes.values());

    units.sort(
        Comparator.comparingInt(StepUnits::countOfFriendlyDamagedOrDead)
            .reversed()
            .thenComparingInt(StepUnits::countOfFriendlyHitPoints));
    return units;
  }

  // Most of this logic comes from FireAa::execute
  private List<StepUnits> getAaFightOutcomes(final StepUnits aliveOrInjuredUnits) {
    final Tuple<List<Unit>, List<String>> aaUnits = getAaUnits(aliveOrInjuredUnits);
    if (aaUnits.getFirst().isEmpty()) {
      // no aa units
      return List.of(new StepUnits(aliveOrInjuredUnits.swapSides(), 1.0));
    }
    final boolean allowMultipleHitsPerUnit = false;

    final List<Triple<Collection<Unit>, Collection<Unit>, Boolean>> aaGroupsAndTargets =
        new ArrayList<>();

    for (final String aaType : aaUnits.getSecond()) {
      final Collection<Unit> aaTypeUnits =
          CollectionUtils.getMatches(aaUnits.getFirst(), Matches.unitIsAaOfTypeAa(aaType));
      final List<Collection<Unit>> firingGroups = MustFightBattle.newFiringUnitGroups(aaTypeUnits);
      for (final Collection<Unit> firingGroup : firingGroups) {
        final Set<UnitType> validTargetTypes =
            UnitAttachment.get(firingGroup.iterator().next().getType())
                .getTargetsAa(parameters.getData());
        final Set<UnitType> airborneTypesTargeted =
            aliveOrInjuredUnits.getType() == Type.AA_DEFENDER
                ? TechAbilityAttachment.getAirborneTargettedByAa(
                        aliveOrInjuredUnits.getEnemy(), parameters.getData())
                    .get(aaType)
                : new HashSet<>();
        final Collection<Unit> validTargets =
            CollectionUtils.getMatches(
                aliveOrInjuredUnits.getAliveOrWaitingToDieEnemy(),
                Matches.unitIsOfTypes(validTargetTypes)
                    .or(
                        Matches.unitIsAirborne()
                            .and(Matches.unitIsOfTypes(airborneTypesTargeted))));

        if (firingGroup.isEmpty() || validTargets.isEmpty()) {
          continue;
        }
        final boolean isSuicideOnHit = firingGroup.stream().anyMatch(Matches.unitIsSuicideOnHit());

        aaGroupsAndTargets.add(Triple.of(firingGroup, validTargets, isSuicideOnHit));
      }
    }

    final Map<StepUnits, StepUnits> outcomes = new HashMap<>();
    if (!aaGroupsAndTargets.isEmpty()) {
      getTargetGroupFightOutcomes(
          outcomes,
          aaGroupsAndTargets,
          aaGroupsAndTargets.size() - 1,
          aliveOrInjuredUnits.swapSides(),
          aliveOrInjuredUnits.getType(),
          MustFightBattle.ReturnFire.ALL,
          allowMultipleHitsPerUnit,
          1.0);
    } else {
      return List.of(new StepUnits(aliveOrInjuredUnits.swapSides(), 1.0));
    }

    final List<StepUnits> units = new ArrayList<>(outcomes.values());

    units.sort(
        Comparator.comparingInt(StepUnits::countOfFriendlyDamagedOrDead)
            .thenComparingInt(StepUnits::countOfFriendlyHitPoints)
            .reversed());
    return units;
  }

  private void getTargetGroupFightOutcomes(
      final Map<StepUnits, StepUnits> outcomes,
      final List<Triple<Collection<Unit>, Collection<Unit>, Boolean>> groupsAndTargets,
      final int groupsAndTargetsIndex,
      final StepUnits currentUnits,
      final Type type,
      final MustFightBattle.ReturnFire returnFire,
      final boolean allowMultipleHitsPerUnit,
      final double probability) {
    final List<Unit> firingGroup =
        new ArrayList<>(groupsAndTargets.get(groupsAndTargetsIndex).getFirst());
    final Collection<Unit> aliveUnits = currentUnits.getAliveFriendly();
    final Collection<Unit> validAliveOrInjuredTargets =
        groupsAndTargets.get(groupsAndTargetsIndex).getSecond();
    final List<Unit> validAliveTargets =
        groupsAndTargets.get(groupsAndTargetsIndex).getSecond().stream()
            .filter(aliveUnits::contains)
            .collect(Collectors.toList());

    final boolean isSuicideOnHit = groupsAndTargets.get(groupsAndTargetsIndex).getThird();

    final boolean defending = type == Type.DEFENDER || type == Type.AA_DEFENDER;
    final RollData diceTuple;

    if (type == Type.AA_ATTACKER || type == Type.AA_DEFENDER) {
      diceTuple =
          getAaRollData(
              currentUnits,
              defending,
              allowMultipleHitsPerUnit,
              firingGroup,
              validAliveOrInjuredTargets);
    } else {
      diceTuple = getRegularRollData(defending, firingGroup, validAliveOrInjuredTargets);
    }
    final List<Double> hitProbabilities = calculateHitProbabilities(diceTuple);

    int totalHits = 0;
    double totalProbability = 0.0;
    for (final Unit targetUnit : currentUnits.addFriendlyMultiHitTargets(validAliveTargets)) {

      if (totalHits >= diceTuple.totalRolls) {
        // no more rolls available
        break;
      }

      final double hitProbability = hitProbabilities.get(totalHits);
      totalProbability += hitProbability;
      if (groupsAndTargetsIndex > 0) {
        getTargetGroupFightOutcomes(
            outcomes,
            groupsAndTargets,
            groupsAndTargetsIndex - 1,
            new StepUnits(currentUnits),
            type,
            returnFire,
            allowMultipleHitsPerUnit,
            hitProbability * probability);
      } else {
        outcomes.compute(
            new StepUnits(currentUnits, hitProbability * probability),
            (key, value) -> {
              if (value == null) {
                return key;
              } else {
                value.addProbability(hitProbability * probability);
              }
              return value;
            });
      }

      totalHits += 1;
      if (MustFightBattle.ReturnFire.ALL == returnFire) {
        currentUnits.hitFriendly(targetUnit);
      } else {
        currentUnits.killFriendly(targetUnit);
      }
      if (isSuicideOnHit) {
        currentUnits.hitEnemy(firingGroup.get(totalHits - 1));
      }
    }

    if (groupsAndTargetsIndex > 0) {
      getTargetGroupFightOutcomes(
          outcomes,
          groupsAndTargets,
          groupsAndTargetsIndex - 1,
          new StepUnits(currentUnits),
          type,
          returnFire,
          allowMultipleHitsPerUnit,
          (1.0 - totalProbability) * probability);
    } else {
      final double childProbability = 1.0 - totalProbability;
      outcomes.compute(
          new StepUnits(currentUnits, childProbability * probability),
          (key, value) -> {
            if (value == null) {
              return key;
            } else {
              value.addProbability(childProbability * probability);
            }
            return value;
          });
    }
  }

  @Value(staticConstructor = "of")
  static class RollData {
    int totalRolls;
    Map<Integer, Integer> rollsByDicePower;
    int maxDiceSides;
  }

  private RollData getAaRollData(
      final StepUnits childUnits,
      final boolean defending,
      final boolean allowMultipleHitsPerUnit,
      final Collection<Unit> firingGroup,
      final Collection<Unit> validAliveOrInjuredTargets) {
    final Map<Unit, DiceRoll.TotalPowerAndTotalRolls> unitPowerAndRollsMap =
        DiceRoll.getAaUnitPowerAndRollsForNormalBattles(
            firingGroup,
            childUnits.getAliveOrWaitingToDieFriendly(),
            childUnits.getAliveOrWaitingToDieEnemy(),
            defending,
            parameters.data);
    final Tuple<Integer, Integer> maxAttackAndDiceSides =
        DiceRoll.getMaxAaAttackAndDiceSides(
            firingGroup, parameters.getData(), defending, unitPowerAndRollsMap);
    final int maxRollsAvailable =
        DiceRoll.getTotalAaAttacks(unitPowerAndRollsMap, validAliveOrInjuredTargets);
    final int planeHitPoints =
        (allowMultipleHitsPerUnit
            ? CasualtyUtil.getTotalHitpointsLeft(validAliveOrInjuredTargets)
            : validAliveOrInjuredTargets.size());
    final int totalRolls = Math.min(planeHitPoints, maxRollsAvailable);
    final Map<Integer, Integer> rollsByDicePower =
        Map.of(maxAttackAndDiceSides.getFirst(), totalRolls);
    return RollData.of(totalRolls, rollsByDicePower, maxAttackAndDiceSides.getSecond());
  }

  private RollData getRegularRollData(
      final boolean defending,
      final Collection<Unit> firingGroup,
      final Collection<Unit> validAliveOrInjuredTargets) {
    final Map<Integer, Integer> rollsByDicePower =
        getRegularDiceGrouped(parameters, defending, firingGroup, validAliveOrInjuredTargets);
    final int totalRolls = rollsByDicePower.values().stream().reduce(0, Integer::sum);
    return RollData.of(totalRolls, rollsByDicePower, parameters.data.getDiceSides());
  }

  Map<Integer, Integer> getRegularDiceGrouped(
      final Parameters parameters,
      final boolean defending,
      final Collection<Unit> friendlyUnits,
      final Collection<Unit> enemyUnits) {

    final Map<Unit, IntegerMap<Unit>> unitSupportPowerMap = new HashMap<>();
    final Map<Unit, IntegerMap<Unit>> unitSupportRollsMap = new HashMap<>();
    final Map<Unit, DiceRoll.TotalPowerAndTotalRolls> unitPowerAndRollsMap =
        DiceRoll.getUnitPowerAndRollsForNormalBattles(
            friendlyUnits,
            new ArrayList<>(enemyUnits),
            friendlyUnits,
            defending,
            parameters.data,
            parameters.location,
            parameters.territoryEffects,
            parameters.isAmphibious,
            parameters.amphibiousLandAttackers,
            unitSupportPowerMap,
            unitSupportRollsMap);

    final Map<Integer, Integer> rollsByDicePower = new HashMap<>();
    for (final Unit unit : friendlyUnits) {
      final DiceRoll.TotalPowerAndTotalRolls totalPowerAndTotalRolls =
          unitPowerAndRollsMap.get(unit);
      final int power = totalPowerAndTotalRolls.getTotalPower();
      final int rolls = totalPowerAndTotalRolls.getTotalRolls();
      if (power == 0) {
        continue;
      }

      rollsByDicePower.compute(
          power,
          (key, prevRolls) -> {
            if (prevRolls == null) {
              return rolls;
            } else {
              return prevRolls + rolls;
            }
          });
    }
    return rollsByDicePower;
  }

  // Taken from one of the steps in MustFightBattle::addCheckEndBattleAndRetreatingSteps
  private void checkEndOfBattle(final StepUnits units) {
    final Collection<Unit> attackingUnits = units.getAliveOrWaitingToDieFriendly();
    final Collection<Unit> defendingUnits = units.getAliveOrWaitingToDieEnemy();
    final GamePlayer attacker = units.getPlayer();
    final Territory battleSite = parameters.getLocation();
    if (CollectionUtils.getMatches(attackingUnits, Matches.unitIsNotInfrastructure()).size() == 0) {
      if (!Properties.getTransportCasualtiesRestricted(parameters.getData())) {
        loseProbability = 1;
        hasResult = true;
      } else {
        // Get all allied transports in the territory
        final Predicate<Unit> matchAllied =
            Matches.unitIsTransport()
                .and(Matches.unitIsNotCombatTransport())
                .and(Matches.isUnitAllied(attacker, parameters.getData()));
        final List<Unit> alliedTransports =
            CollectionUtils.getMatches(battleSite.getUnits(), matchAllied);
        // If no transports, just end the battle
        if (alliedTransports.isEmpty()) {
          loseProbability = 1;
          hasResult = true;
        } else if (round <= 1) {
          /*
          TODO: update the attacking units in units
          attackingUnits =
              CollectionUtils.getMatches(
                  battleSite.getUnits(), Matches.unitIsOwnedBy(attacker));
           */
        } else {
          loseProbability = 1;
          hasResult = true;
        }
      }
    } else if (CollectionUtils.getMatches(defendingUnits, Matches.unitIsNotInfrastructure()).size()
        == 0) {
      if (Properties.getTransportCasualtiesRestricted(parameters.getData())) {
        // If there are undefended attacking transports, determine if they automatically die
        checkUndefendedTransports(units.getEnemy(), units);
      }
      checkForUnitsThatCanRollLeft(false, units);
      winProbability = 1;
      hasResult = true;
    } else if (MAX_ROUNDS > 0 && MAX_ROUNDS <= round) {
      tieProbability = 1;
      hasResult = true;
      /*
         * Removed the else because this is slow for every iteration and
         * is already taken care of by getFightOutcomes
      } else {
        final Collection<TerritoryEffect> territoryEffects = parameters.getTerritoryEffects();
        final int attackPower =
            DiceRoll.getTotalPower(
                DiceRoll.getUnitPowerAndRollsForNormalBattles(
                    attackingUnits,
                    defendingUnits,
                    attackingUnits,
                    false,
                    parameters.getData(),
                    battleSite,
                    territoryEffects,
                    parameters.isAmphibious,
                    parameters.amphibiousLandAttackers),
                parameters.getData());
        final int defensePower =
            DiceRoll.getTotalPower(
                DiceRoll.getUnitPowerAndRollsForNormalBattles(
                    defendingUnits,
                    attackingUnits,
                    defendingUnits,
                    true,
                    parameters.getData(),
                    battleSite,
                    territoryEffects,
                    parameters.isAmphibious,
                    parameters.amphibiousLandAttackers),
                parameters.getData());
        if (attackPower == 0 && defensePower == 0) {
          tieProbability = 1;
          hasResult = true;
        }
         */
    }
    // if a result was found, but no one is still alive, treat it as a draw
    // see BattleResults::draw
    if (hasResult && units.noMoreEnemies() && units.noMoreFriendlies()) {
      loseProbability = 0;
      winProbability = 0;
      badProbability = 0;
      tieProbability = 1;
    }
  }

  /**
   * Determines the result of the battle if it is finished
   *
   * @param units The two sides of the battle
   * @return did the battle end
   */
  private boolean checkEndOfBranch(final StepUnits units) {
    checkEndOfBattle(units);
    if (hasResult) {
      averageUnits = units;
      averageUnits.recordChances();
      averageRounds = round + 1;
      return true;
    }

    // see if we've seen this situation and if so, grab the results
    // if we've seen the situation but it doesn't yet have results
    // then we are recursing.
    final BattleStep cachedBattleStep = nodeCache.get(units);
    if (cachedBattleStep != null) {
      if (cachedBattleStep.hasResult) {
        this.winProbability = cachedBattleStep.winProbability;
        this.loseProbability = cachedBattleStep.loseProbability;
        this.tieProbability = cachedBattleStep.tieProbability;
        this.badProbability = cachedBattleStep.badProbability;
        this.averageUnits = cachedBattleStep.averageUnits;
        this.averageRounds = round + 1;
        // System.out.println("Cached: " + this);
      } else {
        // this node reaches itself
        // an example scenario would be two units fighting
        // each round, there is a possibility that no one will hit
        // so the units never change

        final double[] siblingResults = new double[5];
        siblingResults[0] = cachedBattleStep.winProbability;
        siblingResults[1] = cachedBattleStep.loseProbability;
        siblingResults[2] = cachedBattleStep.tieProbability;
        siblingResults[3] = cachedBattleStep.averageRounds;
        siblingResults[4] = 1.0;
        final StepUnits siblingUnits = cachedBattleStep.averageUnits.swapSides();

        calculateSiblingResultsForRecursiveEnd(
            cachedBattleStep.children, siblingResults, siblingUnits);

        this.averageUnits = units;

        // add 5 levels worth of the sibling probabilities
        // example on how this works:
        // three children, with probabilities of .2, .3, and .5
        // The .5 child is recursive and the .2 has a result of 1 and the .3 has a result of -1
        // Inside of the .5, it will also go to the .2 and .3 nodes and itself.
        // So, its score is basically .5 * (.2 * 1 + .3 * -1) + .5^2 * (.2 * 1 + .3 * -1) + ...
        for (int level = 0; level < 6; level++) {
          this.winProbability += siblingResults[0] * Math.pow(siblingResults[4], level);
          this.loseProbability += siblingResults[1] * Math.pow(siblingResults[4], level);
          this.tieProbability += siblingResults[2] * Math.pow(siblingResults[4], level);
          this.averageRounds += siblingResults[3] * Math.pow(siblingResults[4], level);
          this.averageUnits.updateUnitChances(siblingUnits, Math.pow(siblingResults[4], level));
        }
        // put the rest in "bad"
        this.badProbability =
            1.0 - (this.winProbability + this.loseProbability + this.tieProbability);
        // overwrite the cached one with this that has a result
        nodeCache.put(units, this);
        // System.out.println("Recurse: " + this);
      }
      this.hasResult = true;
      return true;
    }

    if (round >= MAX_ROUNDS) {
      // some branches go for ever
      // an example is where no one hits each other
      // treat any of these branches as a tie
      System.out.println("Reached max rounds");
      hasResult = true;
      badProbability = 1.0;
      // System.out.println("Max: " + this);
      return true;
    }

    nodeCache.put(units, this);
    return false;
  }

  private void calculateSiblingResultsForRecursiveEnd(
      final List<BattleStep> steps, final double[] siblingResults, final StepUnits siblingUnits) {
    for (final BattleStep level : steps) {
      if (level.hasResult) {
        continue;
      }
      siblingResults[4] *= level.probability;
      siblingResults[0] += siblingResults[4] * level.winProbability;
      siblingResults[1] += siblingResults[4] * level.loseProbability;
      siblingResults[2] += siblingResults[4] * level.tieProbability;
      siblingResults[3] += siblingResults[4] * level.averageRounds;
      if (level.units.getType() != Type.AA_ATTACKER) {
        if (siblingUnits.getPlayer().equals(level.averageUnits.getPlayer())) {
          siblingUnits.updateUnitChances(level.averageUnits.swapSides(), siblingResults[4]);
        } else {
          siblingUnits.updateUnitChances(level.averageUnits, siblingResults[4]);
        }
        calculateSiblingResultsForRecursiveEnd(level.children, siblingResults, siblingUnits);
      }
    }
  }

  /**
   * Figures out all the probabilities of rolling dice with a specific set of powers and amounts.
   *
   * @param rollData The details about the dice available to roll
   * @return List of possible probabilities from rolling 0 dice to rolling rollData.totalRolls})
   */
  List<Double> calculateHitProbabilities(final RollData rollData) {
    if (rollData.totalRolls == 0) {
      return List.of();
    }
    final List<Integer> dicePowersList = new ArrayList<>(rollData.rollsByDicePower.keySet());
    Collections.sort(dicePowersList);
    final int maxPower = dicePowersList.get(dicePowersList.size() - 1);
    final int[] dicePowers = new int[dicePowersList.size()];

    final Map<Integer, double[]> calculatedProbabilityCache =
        this.calculatedProbabilityCache.computeIfAbsent(
            rollData.maxDiceSides,
            key -> new HashMap<>(rollData.maxDiceSides * rollData.totalRolls));

    final int[] diceAvailableGroupedByPower = new int[maxPower + 1];
    for (int dicePowerIndex = 0; dicePowerIndex < dicePowersList.size(); dicePowerIndex++) {
      final int dicePower = dicePowersList.get(dicePowerIndex);

      dicePowers[dicePowerIndex] = dicePower;
      diceAvailableGroupedByPower[dicePower] = rollData.rollsByDicePower.get(dicePower);
    }

    final double[] probabilities = new double[rollData.totalRolls + 1];
    calculateHitProbabilitiesWorker(
        0,
        dicePowersList.size() - 1,
        probabilities,
        1.0,
        rollData.maxDiceSides,
        dicePowers,
        diceAvailableGroupedByPower,
        calculatedProbabilityCache);

    final List<Double> probabilitiesList = new ArrayList<>();
    for (final double probability : probabilities) {
      probabilitiesList.add(probability);
    }

    return probabilitiesList;
  }

  /**
   * Recursive worker for {@link #calculateHitProbabilities}
   *
   * <p>This recursively goes through all of the dicePowers and calculates the probabilities for
   * every combination of dicePower and their amounts.
   *
   * <p>This is a hotspot and so should not do anything more than necessary.
   *
   * @param totalDiceHits The number of dice hit so far
   * @param dicePowerIndex The position in the dicePower
   * @param probabilities The resulting probabilities per dice hit
   * @param totalProbability The current total for this recursion
   * @param maxDiceSides The number of dice sides
   * @param dicePowers The list of possible powers
   * @param diceAvailableGroupedByPower The dice that can be rolled for each dicePower
   * @param calculatedProbabilityCache A cache of previously seen probabilities
   */
  void calculateHitProbabilitiesWorker(
      final int totalDiceHits,
      final int dicePowerIndex,
      final double[] probabilities,
      final double totalProbability,
      final int maxDiceSides,
      final int[] dicePowers,
      final int[] diceAvailableGroupedByPower,
      final Map<Integer, double[]> calculatedProbabilityCache) {
    final int dicePower = dicePowers[dicePowerIndex];

    for (int diceHits = diceAvailableGroupedByPower[dicePower]; diceHits >= 0; diceHits--) {

      final double newTotalProbability =
          totalProbability
              * calculateHitProbability(
                  calculatedProbabilityCache,
                  diceHits,
                  dicePower,
                  diceAvailableGroupedByPower[dicePower],
                  maxDiceSides);

      final int newTotalDiceHits = totalDiceHits + diceHits;
      if (dicePowerIndex == 0) {
        probabilities[newTotalDiceHits] += newTotalProbability;
      } else {
        calculateHitProbabilitiesWorker(
            newTotalDiceHits,
            dicePowerIndex - 1,
            probabilities,
            newTotalProbability,
            maxDiceSides,
            dicePowers,
            diceAvailableGroupedByPower,
            calculatedProbabilityCache);
      }
    }
  }

  /**
   * Figures out the probability of getting diceAmount hits out of diceRolls where the dice hits if
   * dicePower or lower is rolled.
   *
   * @param calculatedProbabilityCache A cache of previously seen probabilities
   * @param diceAmount Number of hits wanted
   * @param dicePower The power required for a hit
   * @param diceRolls The number of rolls being made
   * @param maxDiceSides The number of sides on the dice
   * @return The probability of getting a hit
   */
  private double calculateHitProbability(
      final Map<Integer, double[]> calculatedProbabilityCache,
      final int diceAmount,
      final int dicePower,
      final int diceRolls,
      final int maxDiceSides) {
    final Integer cacheKey = (diceRolls * 31) + dicePower;
    final double[] probabilities;
    final double[] result = calculatedProbabilityCache.get(cacheKey);
    if (result != null) {
      probabilities = result;
    } else {
      final double[] innerProbabilities = new double[diceRolls + 1];
      final double hitProbability = (double) dicePower / maxDiceSides;
      final double missProbability = 1.0 - hitProbability;
      final BinomialDistribution bin = new BinomialDistribution(null, diceRolls, hitProbability);

      innerProbabilities[0] = bin.probability(0);
      double previousValue = innerProbabilities[0];
      for (int hitsNeeded = 1; hitsNeeded <= diceRolls; hitsNeeded++) {
        previousValue =
            previousValue
                * hitProbability
                * (diceRolls - hitsNeeded + 1)
                / (missProbability * hitsNeeded);
        innerProbabilities[hitsNeeded] = previousValue;
      }
      probabilities = innerProbabilities;
      calculatedProbabilityCache.put(cacheKey, probabilities);
    }

    return probabilities[diceAmount];
  }

  // Taken from MustFightBattle::checkForUnitsThatCanRollLeft
  void checkForUnitsThatCanRollLeft(final boolean attacker, final StepUnits units) {
    if (units.getAliveOrWaitingToDieFriendly().isEmpty()
        || units.getAliveOrWaitingToDieEnemy().isEmpty()) {
      return;
    }
    final Predicate<Unit> notSubmergedAndType =
        Matches.unitIsSubmerged()
            .negate()
            .and(
                Matches.territoryIsLand().test(parameters.location)
                    ? Matches.unitIsSea().negate()
                    : Matches.unitIsLand().negate());
    final Collection<Unit> unitsToKill;
    final boolean hasUnitsThatCanRollLeft;
    if (attacker) {
      hasUnitsThatCanRollLeft =
          units.getAliveOrWaitingToDieFriendly().stream()
              .anyMatch(
                  notSubmergedAndType.and(Matches.unitIsSupporterOrHasCombatAbility(attacker)));
      unitsToKill =
          CollectionUtils.getMatches(
              units.getAliveOrWaitingToDieFriendly(),
              notSubmergedAndType.and(Matches.unitIsNotInfrastructure()));
    } else {
      hasUnitsThatCanRollLeft =
          units.getAliveOrWaitingToDieEnemy().stream()
              .anyMatch(
                  notSubmergedAndType.and(Matches.unitIsSupporterOrHasCombatAbility(attacker)));
      unitsToKill =
          CollectionUtils.getMatches(
              units.getAliveOrWaitingToDieEnemy(),
              notSubmergedAndType.and(Matches.unitIsNotInfrastructure()));
    }
    final boolean enemy = !attacker;
    final boolean enemyHasUnitsThatCanRollLeft;
    if (enemy) {
      enemyHasUnitsThatCanRollLeft =
          units.getAliveOrWaitingToDieFriendly().stream()
              .anyMatch(notSubmergedAndType.and(Matches.unitIsSupporterOrHasCombatAbility(enemy)));
    } else {
      enemyHasUnitsThatCanRollLeft =
          units.getAliveOrWaitingToDieEnemy().stream()
              .anyMatch(notSubmergedAndType.and(Matches.unitIsSupporterOrHasCombatAbility(enemy)));
    }
    if (!hasUnitsThatCanRollLeft && enemyHasUnitsThatCanRollLeft) {
      for (final Unit unit : unitsToKill) {
        if (attacker) {
          units.hitFriendly(unit);
        } else {
          units.hitEnemy(unit);
        }
      }
    }
  }

  // Taken from MustFightBattle::checkUndefendedTransports
  private void checkUndefendedTransports(final GamePlayer player, final StepUnits units) {
    final GamePlayer attacker = units.getPlayer();
    final Territory battleSite = parameters.getLocation();
    // if we are the attacker, we can retreat instead of dying
    if (player.equals(attacker)) {
      return;
    }
    // Get all allied transports in the territory
    final Predicate<Unit> matchAllied =
        Matches.unitIsTransport()
            .and(Matches.unitIsNotCombatTransport())
            .and(Matches.isUnitAllied(player, parameters.getData()))
            .and(Matches.unitIsSea());
    final List<Unit> alliedTransports =
        CollectionUtils.getMatches(battleSite.getUnits(), matchAllied);
    // If no transports, just return
    if (alliedTransports.isEmpty()) {
      return;
    }
    // Get all ALLIED, sea & air units in the territory (that are NOT submerged)
    final Predicate<Unit> alliedUnitsMatch =
        Matches.isUnitAllied(player, parameters.getData())
            .and(Matches.unitIsNotLand())
            .and(Matches.unitIsSubmerged().negate());
    final Collection<Unit> alliedUnits =
        CollectionUtils.getMatches(battleSite.getUnits(), alliedUnitsMatch);
    // If transports are unescorted, check opposing forces to see if the Trns die automatically
    if (alliedTransports.size() == alliedUnits.size()) {
      // Get all the ENEMY sea and air units (that can attack) in the territory
      final Predicate<Unit> enemyUnitsMatch =
          Matches.unitIsNotLand()
              .and(Matches.unitIsSubmerged().negate())
              .and(Matches.unitCanAttack(player));
      final Collection<Unit> enemyUnits =
          CollectionUtils.getMatches(battleSite.getUnits(), enemyUnitsMatch);
      // If there are attackers set their movement to 0 and kill the transports
      if (!enemyUnits.isEmpty()) {
        alliedTransports.forEach(units::hitEnemy);
      }
    }
  }

  // Taken from MustFightBattle::submergeSubsVsOnlyAir
  private void submergeSubsVsOnlyAir(final StepUnits units) {
    final Collection<Unit> attackingUnits = units.getAliveOrWaitingToDieFriendly();
    final Collection<Unit> defendingUnits = units.getAliveOrWaitingToDieEnemy();
    // if All attackers are AIR, submerge any defending subs
    final Predicate<Unit> subMatch =
        Matches.unitCanEvade().and(Matches.unitCanNotBeTargetedByAll());
    if (!attackingUnits.isEmpty()
        && attackingUnits.stream().allMatch(Matches.unitIsAir())
        && defendingUnits.stream().anyMatch(subMatch)) {
      // Get all defending subs (including allies) in the territory
      final List<Unit> defendingSubs = CollectionUtils.getMatches(defendingUnits, subMatch);
      // submerge defending subs
      defendingSubs.forEach(units::retreatEnemy);
      // checking defending air on attacking subs
    } else if (!defendingUnits.isEmpty()
        && defendingUnits.stream().allMatch(Matches.unitIsAir())
        && attackingUnits.stream().anyMatch(subMatch)) {
      // Get all attacking subs in the territory
      final List<Unit> attackingSubs = CollectionUtils.getMatches(attackingUnits, subMatch);
      // submerge attacking subs
      attackingSubs.forEach(units::retreatFriendly);
    }
  }

  // Taken from MustFightBattle::returnFireAgainstAttackingSubs
  private MustFightBattle.ReturnFire returnFireAgainstAttackingSubs(
      final Collection<Unit> attackingUnits, final Collection<Unit> defendingUnits) {
    final boolean attackingSubsSneakAttack =
        defendingUnits.stream().noneMatch(Matches.unitIsDestroyer());
    final boolean defendingSubsSneakAttack =
        defendingSubsSneakAttackAndNoAttackingDestroyers(attackingUnits);
    final MustFightBattle.ReturnFire returnFireAgainstAttackingSubs;
    if (!attackingSubsSneakAttack) {
      returnFireAgainstAttackingSubs = MustFightBattle.ReturnFire.ALL;
    } else if (defendingSubsSneakAttack || Properties.getWW2V2(parameters.getData())) {
      returnFireAgainstAttackingSubs = MustFightBattle.ReturnFire.SUBS;
    } else {
      returnFireAgainstAttackingSubs = MustFightBattle.ReturnFire.NONE;
    }
    return returnFireAgainstAttackingSubs;
  }

  /*
  // Taken from MustFightBattle::returnFireAgainstDefendingSubs
  private MustFightBattle.ReturnFire returnFireAgainstDefendingSubs(
  final Collection<Unit> attackingUnits, final Collection<Unit> defendingUnits) {
    final boolean attackingSubsSneakAttack =
        defendingUnits.stream().noneMatch(Matches.unitIsDestroyer());
    final boolean defendingSubsSneakAttack =
    defendingSubsSneakAttackAndNoAttackingDestroyers(attackingUnits);
    final MustFightBattle.ReturnFire returnFireAgainstDefendingSubs;
    if (!defendingSubsSneakAttack) {
      returnFireAgainstDefendingSubs = MustFightBattle.ReturnFire.ALL;
    } else if (attackingSubsSneakAttack || Properties.getWW2V2(parameters.getData())) {
      returnFireAgainstDefendingSubs = MustFightBattle.ReturnFire.SUBS;
    } else {
      returnFireAgainstDefendingSubs = MustFightBattle.ReturnFire.NONE;
    }
    return returnFireAgainstDefendingSubs;
  }
   */

  // Taken from MustFightBattle::defendingSubsSneakAttackAndNoAttackingDestroyers
  private boolean defendingSubsSneakAttackAndNoAttackingDestroyers(
      final Collection<Unit> attackingUnits) {
    return attackingUnits.stream().noneMatch(Matches.unitIsDestroyer())
        && defendingSubsSneakAttack();
  }

  // Taken from MustFightBattle::defendingSubsSneakAttack
  private boolean defendingSubsSneakAttack() {
    return Properties.getWW2V2(parameters.getData())
        || Properties.getDefendingSubsSneakAttack(parameters.getData());
  }

  @Override
  public String toString() {
    final String type;
    switch (units.getType()) {
      case AA_ATTACKER:
        type = "AA";
        break;
      case AA_DEFENDER:
        type = "AD";
        break;
      case SUB_ATTACKER:
        type = "SA";
        break;
      case SUB_DEFENDER:
        type = "SD";
        break;
      case ATTACKER:
        type = "AT";
        break;
      case DEFENDER:
        type = "DE";
        break;
      default:
        type = "??";
        break;
    }
    final String out =
        ""
            + player.getName()
            + " ("
            + type
            + ") "
            + units.getAliveOrWaitingToDieFriendly()
            + "("
            + units.getAliveFriendly()
            + ")"
            + " vs "
            + units.getAliveOrWaitingToDieEnemy()
            + "("
            + units.getAliveEnemy()
            + ")"
            + " has "
            + String.format("%.4f", probability)
            + "% chance"
            + " w:"
            + String.format("%.3f", winProbability)
            + " l:"
            + String.format("%.3f", loseProbability)
            + " t:"
            + String.format("%.3f", tieProbability)
            + " b:"
            + String.format("%.3f", badProbability)
            + " [r:"
            + round
            + "]";

    return out;
  }
}
