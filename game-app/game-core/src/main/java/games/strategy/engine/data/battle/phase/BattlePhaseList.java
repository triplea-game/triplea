package games.strategy.engine.data.battle.phase;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.unit.ability.CombatUnitAbility;
import games.strategy.engine.data.unit.ability.ConvertUnitAbility;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.BattlePhaseStep;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.Value;

/**
 * Stores all of the {@link BattlePhase}s that will be used during a battle
 *
 * <p>Also stores the {@link ConvertUnitAbility} that can be used during the battle.
 */
@Getter
public class BattlePhaseList {

  public static final String DEFAULT_AA_PHASE = "AA";
  public static final String DEFAULT_BOMBARD_PHASE = "Bombard";
  public static final String DEFAULT_FIRST_STRIKE_PHASE = "First Strike";
  public static final String DEFAULT_GENERAL_PHASE = "General";

  private final Collection<BattlePhase> phases = new ArrayList<>();

  private final Map<GamePlayer, Collection<ConvertUnitAbility>> convertAbilities = new HashMap<>();

  public BattlePhaseList() {
    phases.add(new BattlePhase(DEFAULT_AA_PHASE, 100));
    phases.add(new BattlePhase(DEFAULT_BOMBARD_PHASE, 200));
    phases.add(new BattlePhase(DEFAULT_FIRST_STRIKE_PHASE, 300));
    phases.add(new BattlePhase(DEFAULT_GENERAL_PHASE, 400));
  }

  public Optional<BattlePhase> getPhase(final String name) {
    return phases.stream().filter(phase -> phase.getName().equals(name)).findFirst();
  }

  public ConvertUnitAbility addAbilityOrMergeAttached(
      final GamePlayer player, final ConvertUnitAbility convertUnitAbility) {
    final Optional<ConvertUnitAbility> duplicateAbility =
        convertAbilities.computeIfAbsent(player, p -> new ArrayList<>()).stream()
            .filter(convertUnitAbility::canMergeAttachedUnitTypes)
            .findFirst();
    if (duplicateAbility.isPresent()) {
      // this ability already exists so combine the unit types that have this ability
      duplicateAbility.get().mergeAttachedUnitTypes(convertUnitAbility);
      return duplicateAbility.get();
    } else {
      convertAbilities.get(player).add(convertUnitAbility);
      return convertUnitAbility;
    }
  }

  public void clearConvertAbilities() {
    convertAbilities.clear();
  }

  /** Gathers the battle steps that the units will perform in all the phases */
  public Collection<BattleStep> getBattleSteps(final BattleState battleState) {
    final List<BattleStep> battleSteps = new ArrayList<>();
    battleSteps.addAll(getBattleStepsForSide(battleState, BattleState.Side.OFFENSE));
    battleSteps.addAll(getBattleStepsForSide(battleState, BattleState.Side.DEFENSE));
    return battleSteps;
  }

  private List<BattleStep> getBattleStepsForSide(
      final BattleState battleState, final BattleState.Side side) {
    final Collection<UnitTypeAndOwner> units =
        battleState.filterUnits(BattleState.UnitBattleFilter.ACTIVE, side).stream()
            .map(unit -> new UnitTypeAndOwner(unit.getType(), unit.getOwner()))
            .collect(Collectors.toSet());
    final Collection<UnitTypeAndOwner> oppositeUnits =
        battleState.filterUnits(BattleState.UnitBattleFilter.ACTIVE, side.getOpposite()).stream()
            .map(unit -> new UnitTypeAndOwner(unit.getType(), unit.getOwner()))
            .collect(Collectors.toSet());

    final Collection<ConvertUnitAbility> convertAbilities =
        units.stream()
            .map(unit -> getConvertUnitAbilities(unit, ConvertUnitAbility.Team.FRIENDLY))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

    convertAbilities.addAll(
        oppositeUnits.stream()
            .map(unit -> getConvertUnitAbilities(unit, ConvertUnitAbility.Team.FOE))
            .flatMap(Collection::stream)
            .collect(Collectors.toList()));

    final Map<CombatUnitAbility, CombatUnitAbility> fromToAbilities = new HashMap<>();
    final Map<CombatUnitAbility, CombatUnitAbility> toFromAbilities = new HashMap<>();
    for (final ConvertUnitAbility convertAbility : convertAbilities) {
      fromToAbilities.put(convertAbility.getFrom(), convertAbility.getTo());
      toFromAbilities.put(convertAbility.getTo(), convertAbility.getFrom());
    }

    return phases.stream()
        .sorted(Comparator.comparingInt(BattlePhase::getOrder))
        .map(
            phase -> {
              final Collection<CombatUnitAbility> abilities = getPhaseAbilities(units, phase);
              final Collection<UnitAbilityAndUnits> unitAbilityAndUnits =
                  getActiveAbilitiesAndUnits(fromToAbilities, toFromAbilities, abilities);
              if (unitAbilityAndUnits.isEmpty()) {
                return null;
              } else {
                return new BattlePhaseStep(
                    phase.getName(), phase.getOrder(), unitAbilityAndUnits, side);
              }
            })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  @Value
  private static class UnitTypeAndOwner {
    UnitType type;
    GamePlayer owner;
  }

  @Value
  public static class UnitAbilityAndUnits {
    CombatUnitAbility unitAbility;
    Collection<UnitType> unitTypes;
  }

  private Collection<ConvertUnitAbility> getConvertUnitAbilities(
      final UnitTypeAndOwner unitTypeAndOwner, final ConvertUnitAbility.Team team) {

    return filterConvertAbilitiesForUnits(unitTypeAndOwner.getOwner(), unitTypeAndOwner.getType())
        .filter(convertUnitAbility -> convertUnitAbility.getTeams().contains(team))
        .collect(Collectors.toList());
  }

  private Stream<ConvertUnitAbility> filterConvertAbilitiesForUnits(
      final GamePlayer player, final UnitType unitType) {
    return convertAbilities.getOrDefault(player, List.of()).stream()
        .filter(ability -> ability.getAttachedUnitTypes().contains(unitType));
  }

  private Collection<CombatUnitAbility> getPhaseAbilities(
      final Collection<UnitTypeAndOwner> units, final BattlePhase phase) {
    return units.stream()
        .map(unitTypeAndOwner -> phase.getAbilities(unitTypeAndOwner.getOwner()))
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  private Collection<UnitAbilityAndUnits> getActiveAbilitiesAndUnits(
      final Map<CombatUnitAbility, CombatUnitAbility> fromToAbilities,
      final Map<CombatUnitAbility, CombatUnitAbility> toFromAbilities,
      final Collection<CombatUnitAbility> abilities) {
    return abilities.stream()
        .map(
            ability -> {
              // this ability will be converted to another ability so don't add it
              if (fromToAbilities.containsKey(ability)) {
                return null;
              }
              final Collection<UnitType> unitTypes =
                  toFromAbilities.getOrDefault(ability, ability).getAttachedUnitTypes();
              // this ability has no active units in the battle so don't add it
              if (unitTypes.isEmpty()) {
                return null;
              }
              return new UnitAbilityAndUnits(
                  ability, toFromAbilities.getOrDefault(ability, ability).getAttachedUnitTypes());
            })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }
}
