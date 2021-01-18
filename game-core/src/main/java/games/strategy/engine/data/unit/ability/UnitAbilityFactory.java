package games.strategy.engine.data.unit.ability;

import static games.strategy.triplea.delegate.battle.BattleStepStrings.FIRST_STRIKE_UNITS;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.NAVAL_BOMBARD;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.UNITS;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.PlayerList;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.UnitTypeList;
import games.strategy.engine.data.battle.phase.BattlePhase;
import games.strategy.engine.data.battle.phase.BattlePhaseList;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

/**
 * Auto-generates unit abilities from deprecated unit options
 *
 * <p>If a map defines the unit abilities, then this should not be used.
 */
@UtilityClass
public class UnitAbilityFactory {

  private static final String WILL_NOT_FIRE_AA_ABILITY_PREFIX = "willNotFireAa";

  /**
   * Tracks the typeAa and their targetsAa
   *
   * <p>The typeAa and their targetsAa is copied on every single unitAttachment. This means that a
   * map maker might have different targetsAa in the same typeAa. But that is a typo since the
   * engine only uses the first targetsAa that it finds. This map keeps track of the typeAa and
   * targetsAa separately from the unitAttachment so that there is a central place to find them.
   */
  private static final Map<String, Collection<UnitType>> aaTargets = new HashMap<>();

  /**
   * Tracks what unit types a specific unit type can not target without an ally destroyer present
   *
   * <p>The deprecated property canNotBeTargetedBy was used to determine if a unit could be targeted
   * depending on the presence of an isDestroyer. This means that canNotBeTargetedBy is structured
   * as targeted unit -> firing units. But unitAbilities are structured as firing unit -> targeted
   * units.
   *
   * <p>This map is used to invert that data set at the beginning so that it can be easily looked up
   * later
   */
  private static final Map<UnitType, Collection<UnitType>> unitCanNotTargetWithoutDestroyer =
      new HashMap<>();

  /**
   * Create the default unit abilities at the beginning of the battle step
   *
   * <p>This needs to be called once at the beginning of each battle step. All the battles in the
   * battle step will use the same unit abilities but technology and triggers can change the
   * properties that this depends on so subsequent battle steps may have different unit abilities.
   */
  public static void generate(
      final PlayerList playerList,
      final UnitTypeList unitTypeList,
      final BattlePhaseList battlePhaseList,
      final GameProperties properties) {

    // clear out the existing unit abilities so that they can be rebuilt
    clearExistingUnitAbilities(battlePhaseList);

    // set up some helper maps
    initializeTargetsAa(unitTypeList);
    initializeCanNotTargetWithoutDestroyer(unitTypeList);

    // create unique unit abilities for each player
    playerList.forEach(
        player -> {
          generatePerPlayer(unitTypeList, battlePhaseList, properties, player);
        });
  }

  private static void clearExistingUnitAbilities(final BattlePhaseList battlePhaseList) {
    battlePhaseList.clearConvertAbilities();
    battlePhaseList.getPhases().forEach(BattlePhase::clearAbilities);
  }

  private static void initializeTargetsAa(final UnitTypeList unitTypeList) {
    aaTargets.clear();
    unitTypeList.stream()
        .map(UnitAttachment::get)
        .forEach(
            unitAttachment ->
                aaTargets.putIfAbsent(
                    unitAttachment.getTypeAa(), unitAttachment.getTargetsAa(unitTypeList)));
  }

  private static void initializeCanNotTargetWithoutDestroyer(final UnitTypeList unitTypeList) {
    unitCanNotTargetWithoutDestroyer.clear();
    unitTypeList.stream()
        .filter(
            Predicate.not(
                unitType -> UnitAttachment.get(unitType).getCanNotBeTargetedBy().isEmpty()))
        .forEach(
            unitType ->
                UnitAttachment.get(unitType)
                    .getCanNotBeTargetedBy()
                    .forEach(
                        firingUnitType ->
                            unitCanNotTargetWithoutDestroyer
                                .computeIfAbsent(firingUnitType, u -> new ArrayList<>())
                                .add(unitType)));
  }

  /**
   * Create the default unit abilities at the beginning of the battle step for each player
   *
   * <p>Since a player can have different tech advances, the unit abilities can be unique per player
   */
  private static void generatePerPlayer(
      final UnitTypeList unitTypeList,
      final BattlePhaseList battlePhaseList,
      final GameProperties properties,
      final GamePlayer player) {
    unitTypeList.stream()
        .forEach(
            unitType ->
                generatePerPlayerAndUnit(
                    unitTypeList, battlePhaseList, properties, player, unitType));

    createBombardUnitAbilities(unitTypeList, battlePhaseList, properties, player);
  }

  private static void generatePerPlayerAndUnit(
      final UnitTypeList unitTypeList,
      final BattlePhaseList battlePhaseList,
      final GameProperties properties,
      final GamePlayer player,
      final UnitType unitType) {
    final UnitAttachment unitAttachment = UnitAttachment.get(unitType);
    if (unitAttachment.getIsAaForCombatOnly()) {
      createAaUnitAbilities(battlePhaseList, player, unitType);
    }
    createUnitAbilities(unitTypeList, battlePhaseList, properties, player, unitType);
  }

  private static void createAaUnitAbilities(
      final BattlePhaseList battlePhaseList, final GamePlayer player, final UnitType unitType) {
    final UnitAttachment unitAttachment = UnitAttachment.get(unitType);
    final Collection<BattleState.Side> sides = new ArrayList<>();
    if (unitAttachment.getOffensiveAttackAa(player) > 0 && unitAttachment.getMaxAaAttacks() != 0) {
      sides.add(BattleState.Side.OFFENSE);
    }
    if (unitAttachment.getAttackAa(player) > 0 && unitAttachment.getMaxAaAttacks() != 0) {
      sides.add(BattleState.Side.DEFENSE);
    }
    if (sides.isEmpty()) {
      return;
    }

    final CombatUnitAbility ability =
        addAbility(
            battlePhaseList,
            CombatUnitAbility.builder()
                .name(unitAttachment.getTypeAa())
                .attachedUnitTypes(List.of(unitType))
                .diceType(CombatUnitAbility.DiceType.AA)
                .sides(sides)
                .round(
                    unitAttachment.getMaxRoundsAa() == -1
                        ? Integer.MAX_VALUE
                        : unitAttachment.getMaxRoundsAa())
                .targets(aaTargets.get(unitAttachment.getTypeAa()))
                .returnFire(false)
                .commitSuicideAfterSuccessfulHit(getCommitSuicideOnHitSides(unitType))
                .commitSuicide(getCommitSuicideSides(unitType))
                .build(),
            player,
            BattlePhaseList.DEFAULT_AA_PHASE);

    unitAttachment.getWillNotFireIfPresent().stream()
        .map(
            unitTypeThatPreventsFiring ->
                createWillNotFireIfPresentAbilities(unitTypeThatPreventsFiring, ability))
        .forEach(antiAbility -> addConvertAbility(battlePhaseList, antiAbility, player));
  }

  private static CombatUnitAbility addAbility(
      final BattlePhaseList battlePhaseList,
      final CombatUnitAbility combatUnitAbility,
      final GamePlayer player,
      final String phaseName) {
    final BattlePhase battlePhase =
        battlePhaseList
            .getPhase(phaseName)
            .orElseThrow(
                () -> new IllegalArgumentException("The phase " + phaseName + " doesn't exist"));
    return battlePhase.addAbilityOrMergeAttached(player, combatUnitAbility);
  }

  private static Collection<BattleState.Side> getCommitSuicideOnHitSides(final UnitType unitType) {
    return UnitAttachment.get(unitType).getIsSuicideOnHit()
        ? List.of(BattleState.Side.OFFENSE, BattleState.Side.DEFENSE)
        : List.of();
  }

  private static Collection<BattleState.Side> getCommitSuicideSides(final UnitType unitType) {
    final Collection<BattleState.Side> commitSuicideSides = new ArrayList<>();
    final UnitAttachment unitAttachment = UnitAttachment.get(unitType);
    if (unitAttachment.getIsSuicideOnAttack()) {
      commitSuicideSides.add(BattleState.Side.OFFENSE);
    }
    if (unitAttachment.getIsSuicideOnDefense()) {
      commitSuicideSides.add(BattleState.Side.DEFENSE);
    }
    return commitSuicideSides;
  }

  private static ConvertUnitAbility createWillNotFireIfPresentAbilities(
      final UnitType unitTypeThatPreventsFiring, final CombatUnitAbility ability) {
    return ConvertUnitAbility.builder()
        .name(WILL_NOT_FIRE_AA_ABILITY_PREFIX + " " + ability.getName())
        .attachedUnitTypes(List.of(unitTypeThatPreventsFiring))
        .factions(List.of(ConvertUnitAbility.Faction.ENEMY))
        .from(ability)
        .build();
  }

  private static void addConvertAbility(
      final BattlePhaseList battlePhaseList,
      final ConvertUnitAbility unitAbility,
      final GamePlayer player) {
    battlePhaseList.addAbilityOrMergeAttached(player, unitAbility);
  }

  private static void createUnitAbilities(
      final UnitTypeList unitTypeList,
      final BattlePhaseList battlePhaseList,
      final GameProperties properties,
      final GamePlayer player,
      final UnitType unitType) {
    final UnitAttachment unitAttachment = UnitAttachment.get(unitType);
    final Collection<BattleState.Side> sides = new ArrayList<>();
    if (unitAttachment.getAttack(player) > 0) {
      sides.add(BattleState.Side.OFFENSE);
    }
    if (unitAttachment.getDefense(player) > 0) {
      if (unitAttachment.getIsFirstStrike()
          && !Properties.getDefendingSubsSneakAttack(properties)) {
        // if the defending sub doesn't have sneak attack, then it needs to have a defensive general
        // ability instead
        createUnitAbilities(
            unitTypeList,
            battlePhaseList,
            properties,
            player,
            unitType,
            List.of(BattleState.Side.DEFENSE),
            false);
      } else {
        sides.add(BattleState.Side.DEFENSE);
      }
    }
    if (sides.isEmpty()) {
      return;
    }

    createUnitAbilities(
        unitTypeList,
        battlePhaseList,
        properties,
        player,
        unitType,
        sides,
        unitAttachment.getIsFirstStrike());
  }

  private static void createUnitAbilities(
      final UnitTypeList unitTypeList,
      final BattlePhaseList battlePhaseList,
      final GameProperties properties,
      final GamePlayer player,
      final UnitType unitType,
      final Collection<BattleState.Side> sides,
      final boolean isFirstStrike) {
    // if the unit needs a destroyer present to target things, then two abilities will be created.
    // one ability will target everything but will not be attached to this unit and the other
    // ability will only target the units that don't need a destroyer present. The destroyer will
    // gain a convertUnitAbility so that this unit can have its ability converted when the destroyer
    // is present
    final boolean needsDestroyerToTarget =
        !unitCanNotTargetWithoutDestroyer.getOrDefault(unitType, List.of()).isEmpty();

    final CombatUnitAbility ability =
        addAbility(
            battlePhaseList,
            CombatUnitAbility.builder()
                .name((isFirstStrike ? FIRST_STRIKE_UNITS : UNITS))
                .attachedUnitTypes(needsDestroyerToTarget ? List.of() : List.of(unitType))
                .diceType(CombatUnitAbility.DiceType.NORMAL)
                .sides(sides)
                .targets(getTargetsWithDestroyer(unitTypeList, unitType))
                .returnFire(!isFirstStrike)
                .commitSuicideAfterSuccessfulHit(getCommitSuicideOnHitSides(unitType))
                .commitSuicide(getCommitSuicideSides(unitType))
                .build(),
            player,
            isFirstStrike
                ? BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE
                : BattlePhaseList.DEFAULT_GENERAL_PHASE);

    if (isFirstStrike) {
      createAntiFirstStrikeAbility(unitTypeList, battlePhaseList, properties, ability, player);
    }

    if (needsDestroyerToTarget) {
      final CombatUnitAbility abilityWithoutDestroyer =
          addAbility(
              battlePhaseList,
              CombatUnitAbility.builder()
                  .name((isFirstStrike ? FIRST_STRIKE_UNITS : UNITS) + " without destroyer")
                  .attachedUnitTypes(List.of(unitType))
                  .diceType(CombatUnitAbility.DiceType.NORMAL)
                  .sides(sides)
                  .targets(getTargetsWithoutDestroyer(unitTypeList, unitType))
                  .returnFire(!isFirstStrike)
                  .commitSuicideAfterSuccessfulHit(getCommitSuicideOnHitSides(unitType))
                  .commitSuicide(getCommitSuicideSides(unitType))
                  .build(),
              player,
              isFirstStrike
                  ? BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE
                  : BattlePhaseList.DEFAULT_GENERAL_PHASE);

      battlePhaseList.addAbilityOrMergeAttached(
          player,
          ConvertUnitAbility.builder()
              .name("allow " + unitType.getName() + " to hit more units")
              .attachedUnitTypes(getIsDestroyerUnitTypes(unitTypeList))
              .factions(List.of(ConvertUnitAbility.Faction.ALLIED))
              .from(abilityWithoutDestroyer)
              .to(ability)
              .build());

      if (isFirstStrike) {
        createAntiFirstStrikeAbility(
            unitTypeList, battlePhaseList, properties, abilityWithoutDestroyer, player);
      }
    }
  }

  private static List<UnitType> getTargetsWithDestroyer(
      final UnitTypeList unitTypeList, final UnitType unitType) {
    return unitTypeList.stream()
        .filter(
            Predicate.not(
                possibleTarget ->
                    UnitAttachment.get(unitType).getCanNotTarget().contains(possibleTarget)))
        .filter(isNotInfrastructure())
        .collect(Collectors.toList());
  }

  private static List<UnitType> getTargetsWithoutDestroyer(
      final UnitTypeList unitTypeList, final UnitType unitType) {
    return getTargetsWithDestroyer(unitTypeList, unitType).stream()
        .filter(
            Predicate.not(
                possibleTarget ->
                    unitCanNotTargetWithoutDestroyer
                        .computeIfAbsent(unitType, k -> List.of())
                        .contains(possibleTarget)))
        .collect(Collectors.toList());
  }

  private static Predicate<UnitType> isNotInfrastructure() {
    return Predicate.not(
        possibleTarget -> UnitAttachment.get(possibleTarget).getIsInfrastructure());
  }

  private static void createAntiFirstStrikeAbility(
      final UnitTypeList unitTypeList,
      final BattlePhaseList battlePhaseList,
      final GameProperties properties,
      final CombatUnitAbility unitAbility,
      final GamePlayer player) {
    if (getIsDestroyerUnitTypes(unitTypeList).isEmpty()) {
      return;
    }

    final CombatUnitAbility unitAbilityWithReturnFire =
        addAbility(
            battlePhaseList,
            unitAbility.toBuilder().attachedUnitTypes(List.of()).returnFire(true).build(),
            player,
            Properties.getWW2V2(properties)
                ? BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE
                : BattlePhaseList.DEFAULT_GENERAL_PHASE);

    battlePhaseList.addAbilityOrMergeAttached(
        player,
        ConvertUnitAbility.builder()
            .name("neutralize first strike ability")
            .attachedUnitTypes(getIsDestroyerUnitTypes(unitTypeList))
            .factions(List.of(ConvertUnitAbility.Faction.ENEMY))
            .from(unitAbility)
            .to(unitAbilityWithReturnFire)
            .build());
  }

  private static Collection<UnitType> getIsDestroyerUnitTypes(final UnitTypeList unitTypeList) {
    return unitTypeList.stream()
        .filter(unitType -> UnitAttachment.get(unitType).getIsDestroyer())
        .collect(Collectors.toList());
  }

  private static void createBombardUnitAbilities(
      final UnitTypeList unitTypeList,
      final BattlePhaseList battlePhaseList,
      final GameProperties properties,
      final GamePlayer player) {

    if (getCanBombardUnitTypes(unitTypeList, player).isEmpty()) {
      return;
    }

    battlePhaseList
        .getPhase(BattlePhaseList.DEFAULT_BOMBARD_PHASE)
        .ifPresent(
            phase ->
                phase.addAbilityOrMergeAttached(
                    player,
                    CombatUnitAbility.builder()
                        .name(NAVAL_BOMBARD)
                        .attachedUnitTypes(getCanBombardUnitTypes(unitTypeList, player))
                        .diceType(CombatUnitAbility.DiceType.BOMBARD)
                        .round(1)
                        .returnFire(Properties.getNavalBombardCasualtiesReturnFire(properties))
                        .targets(getBombardTargetUnitTypes(unitTypeList))
                        .build()));
  }

  private static List<UnitType> getCanBombardUnitTypes(
      final UnitTypeList unitTypeList, final GamePlayer player) {
    return unitTypeList.stream()
        .filter(unitType -> UnitAttachment.get(unitType).getCanBombard(player))
        .collect(Collectors.toList());
  }

  private static Collection<UnitType> getBombardTargetUnitTypes(final UnitTypeList unitTypeList) {
    return unitTypeList.stream().filter(isNotInfrastructure()).collect(Collectors.toList());
  }
}
