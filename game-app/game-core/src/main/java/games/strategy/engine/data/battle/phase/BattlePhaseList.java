package games.strategy.engine.data.battle.phase;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.unit.ability.ConvertUnitAbility;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;

/**
 * Stores all of the {@link BattlePhase}s that will be used during a battle
 *
 * <p>Also stores the {@link ConvertUnitAbility} that can be used during the battle.
 */
@Getter
public class BattlePhaseList {

  @NonNls public static final String DEFAULT_AA_PHASE = "AA";
  @NonNls public static final String DEFAULT_BOMBARD_PHASE = "Bombard";
  @NonNls public static final String DEFAULT_FIRST_STRIKE_PHASE = "First Strike";
  @NonNls public static final String DEFAULT_GENERAL_PHASE = "General";

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

  public void addAbility(final GamePlayer player, final ConvertUnitAbility convertUnitAbility) {
    convertAbilities.computeIfAbsent(player, p -> new ArrayList<>()).add(convertUnitAbility);
  }

  public void clearConvertAbilities() {
    convertAbilities.clear();
  }
}
