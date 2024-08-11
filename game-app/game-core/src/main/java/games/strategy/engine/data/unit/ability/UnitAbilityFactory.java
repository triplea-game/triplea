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
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NonNls;

/**
 * Auto-generates unit abilities from deprecated unit options
 *
 * <p>If a map defines the unit abilities, then this should not be used.
 */
@UtilityClass
public class UnitAbilityFactory {

  @NonNls private static final String WILL_NOT_FIRE_AA_ABILITY_PREFIX = "willNotFireAa";

  @RequiredArgsConstructor
  @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
  private static class Parameters {

    UnitTypeList unitTypeList;
    BattlePhaseList battlePhaseList;
    GameProperties properties;

    /**
     * Tracks the typeAa and their targetsAa
     *
     * <p>The typeAa and their targetsAa is copied on every single unitAttachment. This means that a
     * map maker might have different targetsAa in the same typeAa. But that is a typo since the
     * engine only uses the first targetsAa that it finds. This map keeps track of the typeAa and
     * targetsAa separately from the unitAttachment so that there is a central place to find them.
     */
    Map<String, Collection<UnitType>> aaTargets = new HashMap<>();

    /**
     * Tracks what unit types a specific unit type can not target without an ally destroyer present
     *
     * <p>The deprecated property canNotBeTargetedBy was used to determine if a unit could be
     * targeted depending on the presence of an isDestroyer. This means that canNotBeTargetedBy is
     * structured as targeted unit -> firing units. But unitAbilities are structured as firing unit
     * -> targeted units.
     *
     * <p>This map is used to invert that data set at the beginning so that it can be easily looked
     * up later
     */
    Map<UnitType, Collection<UnitType>> unitCanNotTargetWithoutDestroyer = new HashMap<>();

    private void initialize() {
      initializeTargetsAa();
      initializeCanNotTargetWithoutDestroyer();
    }

    private void initializeTargetsAa() {
      unitTypeList.stream()
          .map(UnitType::getUnitAttachment)
          .forEach(
              unitAttachment ->
                  aaTargets.putIfAbsent(
                      unitAttachment.getTypeAa(), unitAttachment.getTargetsAa(unitTypeList)));
    }

    private void initializeCanNotTargetWithoutDestroyer() {
      unitTypeList.stream()
          .filter(
              Predicate.not(
                  unitType -> unitType.getUnitAttachment().getCanNotBeTargetedBy().isEmpty()))
          .forEach(
              unitType ->
                  unitType
                      .getUnitAttachment()
                      .getCanNotBeTargetedBy()
                      .forEach(
                          firingUnitType ->
                              unitCanNotTargetWithoutDestroyer
                                  .computeIfAbsent(firingUnitType, u -> new ArrayList<>())
                                  .add(unitType)));
    }
  }

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

    final Parameters parameters = new Parameters(unitTypeList, battlePhaseList, properties);
    // set up some helper maps
    parameters.initialize();

    // create unique unit abilities for each player
    playerList.forEach(player -> generatePerPlayer(parameters, player));
  }

  private static void clearExistingUnitAbilities(final BattlePhaseList battlePhaseList) {
    battlePhaseList.clearConvertAbilities();
    battlePhaseList.getPhases().forEach(BattlePhase::clearAbilities);
  }

  /**
   * Create the default unit abilities at the beginning of the battle step for each player
   *
   * <p>Since a player can have different tech advances, the unit abilities can be unique per player
   */
  private static void generatePerPlayer(final Parameters parameters, final GamePlayer player) {
    parameters.unitTypeList.stream()
        .forEach(unitType -> generatePerPlayerAndUnit(parameters, player, unitType));

    createBombardUnitAbilities(parameters, player);
  }

  private static void generatePerPlayerAndUnit(
      final Parameters parameters, final GamePlayer player, final UnitType unitType) {
    final UnitAttachment unitAttachment = unitType.getUnitAttachment();
    if (unitAttachment.isAaForCombatOnly()) {
      createAaUnitAbilities(parameters, player, unitType);
    }
    createUnitAbilities(parameters, player, unitType);
  }

  private static void createAaUnitAbilities(
      final Parameters parameters, final GamePlayer player, final UnitType unitType) {
    final UnitAttachment unitAttachment = unitType.getUnitAttachment();
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
            parameters.battlePhaseList,
            CombatUnitAbility.builder()
                .name(unitAttachment.getTypeAa())
                .attachedUnitTypes(List.of(unitType))
                .combatValueType(CombatUnitAbility.CombatValueType.AA)
                .sides(sides)
                .round(
                    unitAttachment.getMaxRoundsAa() == -1
                        ? Integer.MAX_VALUE
                        : unitAttachment.getMaxRoundsAa())
                .targets(parameters.aaTargets.get(unitAttachment.getTypeAa()))
                .returnFire(false)
                .suicideOnOffense(calculateSuicideType(unitType, BattleState.Side.OFFENSE))
                .suicideOnDefense(calculateSuicideType(unitType, BattleState.Side.DEFENSE))
                .build(),
            player,
            BattlePhaseList.DEFAULT_AA_PHASE);

    unitAttachment.getWillNotFireIfPresent().stream()
        .map(
            unitTypeThatPreventsFiring ->
                createWillNotFireIfPresentAbilities(unitTypeThatPreventsFiring, ability))
        .forEach(antiAbility -> addConvertAbility(parameters.battlePhaseList, antiAbility, player));
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
                () ->
                    new IllegalArgumentException(
                        "The phase "
                            + phaseName
                            + " doesn't exist. Existing phases are: "
                            + battlePhaseList.getPhases().stream()
                                .map(BattlePhase::getName)
                                .collect(Collectors.joining(", "))));
    return battlePhase.addAbilityOrMergeAttached(player, combatUnitAbility);
  }

  private static CombatUnitAbility.Suicide calculateSuicideType(
      final UnitType unitType, final BattleState.Side side) {
    final UnitAttachment unitAttachment = unitType.getUnitAttachment();
    if (side == BattleState.Side.OFFENSE
        ? unitAttachment.getIsSuicideOnAttack()
        : unitAttachment.getIsSuicideOnDefense()) {
      return CombatUnitAbility.Suicide.AFTER_FIRE;
    } else if (unitAttachment.isSuicideOnHit()) {
      return CombatUnitAbility.Suicide.AFTER_HIT;
    } else {
      return CombatUnitAbility.Suicide.NONE;
    }
  }

  private static ConvertUnitAbility createWillNotFireIfPresentAbilities(
      final UnitType unitTypeThatPreventsFiring, final CombatUnitAbility ability) {
    return ConvertUnitAbility.builder()
        .name(WILL_NOT_FIRE_AA_ABILITY_PREFIX + " " + ability.getName())
        .attachedUnitTypes(List.of(unitTypeThatPreventsFiring))
        .teams(List.of(ConvertUnitAbility.Team.FOE))
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
      final Parameters parameters, final GamePlayer player, final UnitType unitType) {
    final UnitAttachment unitAttachment = unitType.getUnitAttachment();
    final Collection<BattleState.Side> sides = new ArrayList<>();
    if (unitAttachment.getAttack(player) > 0) {
      sides.add(BattleState.Side.OFFENSE);
    }
    if (unitAttachment.getDefense(player) > 0) {
      if (unitAttachment.getIsFirstStrike()
          && !Properties.getDefendingSubsSneakAttack(parameters.properties)) {
        // if the defending sub doesn't have sneak attack, then it needs to have a defensive general
        // ability instead
        createUnitAbilities(
            parameters,
            player,
            unitType,
            List.of(BattleState.Side.DEFENSE),
            BattlePhaseList.DEFAULT_GENERAL_PHASE);
      } else {
        sides.add(BattleState.Side.DEFENSE);
      }
    }
    if (sides.isEmpty()) {
      return;
    }

    createUnitAbilities(
        parameters,
        player,
        unitType,
        sides,
        unitAttachment.getIsFirstStrike()
            ? BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE
            : BattlePhaseList.DEFAULT_GENERAL_PHASE);
  }

  private static void createUnitAbilities(
      final Parameters parameters,
      final GamePlayer player,
      final UnitType unitType,
      final Collection<BattleState.Side> sides,
      final String phase) {
    // if the unit needs a destroyer present to target things, then two abilities will be created.
    // one ability will target everything but will not be attached to this unit and the other
    // ability will only target the units that don't need a destroyer present. The destroyer will
    // gain a convertUnitAbility so that this unit can have its ability converted when the destroyer
    // is present
    final boolean needsDestroyerToTarget =
        !parameters.unitCanNotTargetWithoutDestroyer.getOrDefault(unitType, List.of()).isEmpty();
    final boolean isFirstStrike = phase.equals(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE);

    final CombatUnitAbility ability =
        addAbility(
            parameters.battlePhaseList,
            CombatUnitAbility.builder()
                .name((isFirstStrike ? FIRST_STRIKE_UNITS : UNITS))
                .attachedUnitTypes(needsDestroyerToTarget ? List.of() : List.of(unitType))
                .combatValueType(CombatUnitAbility.CombatValueType.NORMAL)
                .sides(sides)
                .targets(getTargetsWithDestroyer(parameters.unitTypeList, unitType))
                .returnFire(!isFirstStrike)
                .suicideOnOffense(calculateSuicideType(unitType, BattleState.Side.OFFENSE))
                .suicideOnDefense(calculateSuicideType(unitType, BattleState.Side.DEFENSE))
                .build(),
            player,
            phase);

    if (isFirstStrike) {
      createAntiFirstStrikeAbility(parameters, ability, player);
    }

    if (needsDestroyerToTarget) {
      final CombatUnitAbility abilityWithoutDestroyer =
          addAbility(
              parameters.battlePhaseList,
              CombatUnitAbility.builder()
                  .name((isFirstStrike ? FIRST_STRIKE_UNITS : UNITS) + " without destroyer")
                  .attachedUnitTypes(List.of(unitType))
                  .combatValueType(CombatUnitAbility.CombatValueType.NORMAL)
                  .sides(sides)
                  .targets(getTargetsWithoutDestroyer(parameters, unitType))
                  .returnFire(!isFirstStrike)
                  .suicideOnOffense(calculateSuicideType(unitType, BattleState.Side.OFFENSE))
                  .suicideOnDefense(calculateSuicideType(unitType, BattleState.Side.DEFENSE))
                  .build(),
              player,
              phase);

      if (isFirstStrike) {
        createAntiFirstStrikeAbility(parameters, abilityWithoutDestroyer, player);
      }

      parameters.battlePhaseList.addAbilityOrMergeAttached(
          player,
          ConvertUnitAbility.builder()
              .name("allow " + unitType.getName() + " to hit more units")
              .attachedUnitTypes(getIsDestroyerUnitTypes(parameters.unitTypeList))
              .teams(List.of(ConvertUnitAbility.Team.FRIENDLY))
              .from(abilityWithoutDestroyer)
              .to(ability)
              .build());
    }
  }

  private static List<UnitType> getTargetsWithDestroyer(
      final UnitTypeList unitTypeList, final UnitType unitType) {
    return unitTypeList.stream()
        .filter(
            Predicate.not(
                possibleTarget ->
                    unitType.getUnitAttachment().getCanNotTarget().contains(possibleTarget)))
        .filter(isNotInfrastructure())
        .collect(Collectors.toList());
  }

  private static List<UnitType> getTargetsWithoutDestroyer(
      final Parameters parameters, final UnitType unitType) {
    return getTargetsWithDestroyer(parameters.unitTypeList, unitType).stream()
        .filter(
            Predicate.not(
                possibleTarget ->
                    parameters
                        .unitCanNotTargetWithoutDestroyer
                        .computeIfAbsent(unitType, k -> List.of())
                        .contains(possibleTarget)))
        .collect(Collectors.toList());
  }

  private static Predicate<UnitType> isNotInfrastructure() {
    return Predicate.not(possibleTarget -> possibleTarget.getUnitAttachment().isInfrastructure());
  }

  private static void createAntiFirstStrikeAbility(
      final Parameters parameters, final CombatUnitAbility unitAbility, final GamePlayer player) {
    if (getIsDestroyerUnitTypes(parameters.unitTypeList).isEmpty()) {
      return;
    }

    final CombatUnitAbility unitAbilityWithReturnFire =
        addAbility(
            parameters.battlePhaseList,
            unitAbility.toBuilder().attachedUnitTypes(List.of()).returnFire(true).build(),
            player,
            Properties.getWW2V2(parameters.properties)
                ? BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE
                : BattlePhaseList.DEFAULT_GENERAL_PHASE);

    parameters.battlePhaseList.addAbilityOrMergeAttached(
        player,
        ConvertUnitAbility.builder()
            .name("neutralize first strike ability")
            .attachedUnitTypes(getIsDestroyerUnitTypes(parameters.unitTypeList))
            .teams(List.of(ConvertUnitAbility.Team.FOE))
            .from(unitAbility)
            .to(unitAbilityWithReturnFire)
            .build());
  }

  private static Collection<UnitType> getIsDestroyerUnitTypes(final UnitTypeList unitTypeList) {
    return unitTypeList.stream()
        .filter(unitType -> unitType.getUnitAttachment().isDestroyer())
        .collect(Collectors.toList());
  }

  private static void createBombardUnitAbilities(
      final Parameters parameters, final GamePlayer player) {

    if (getCanBombardUnitTypes(parameters.unitTypeList, player).isEmpty()) {
      return;
    }

    parameters
        .battlePhaseList
        .getPhase(BattlePhaseList.DEFAULT_BOMBARD_PHASE)
        .ifPresent(
            phase ->
                phase.addAbilityOrMergeAttached(
                    player,
                    CombatUnitAbility.builder()
                        .name(NAVAL_BOMBARD)
                        .attachedUnitTypes(getCanBombardUnitTypes(parameters.unitTypeList, player))
                        .combatValueType(CombatUnitAbility.CombatValueType.BOMBARD)
                        .round(1)
                        .returnFire(
                            Properties.getNavalBombardCasualtiesReturnFire(parameters.properties))
                        .targets(getBombardTargetUnitTypes(parameters.unitTypeList))
                        .build()));
  }

  private static List<UnitType> getCanBombardUnitTypes(
      final UnitTypeList unitTypeList, final GamePlayer player) {
    return unitTypeList.stream()
        .filter(unitType -> unitType.getUnitAttachment().getCanBombard(player))
        .collect(Collectors.toList());
  }

  private static Collection<UnitType> getBombardTargetUnitTypes(final UnitTypeList unitTypeList) {
    return unitTypeList.stream().filter(isNotInfrastructure()).collect(Collectors.toList());
  }
}
