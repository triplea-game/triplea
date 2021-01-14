package games.strategy.engine.data.battle.phase;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.unit.ability.CombatUnitAbility;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;

@Value
public class BattlePhase {
  String name;
  int order;

  @Getter(AccessLevel.NONE)
  Map<GamePlayer, Collection<CombatUnitAbility>> abilities = new HashMap<>();

  public Collection<CombatUnitAbility> getAbilities(final GamePlayer player) {
    return abilities.getOrDefault(player, List.of());
  }

  public void clearAbilities() {
    abilities.clear();
  }

  /**
   * Adds the unit ability or finds an existing equal ability and merges the onUnitTypes
   *
   * @return The passed in unitAbility if it is new or the existing unitAbility
   */
  public CombatUnitAbility addAbilityOrMergeAttached(
      final GamePlayer player, final CombatUnitAbility unitAbility) {
    final Optional<CombatUnitAbility> duplicateAbility =
        abilities.computeIfAbsent(player, p -> new ArrayList<>()).stream()
            .filter(unitAbility::canMergeAttachedUnitTypes)
            .findFirst();
    if (duplicateAbility.isPresent()) {
      // this ability already exists so combine the unit types that have this ability
      duplicateAbility.get().mergeAttachedUnitTypes(unitAbility);
      return duplicateAbility.get();
    } else {
      abilities.get(player).add(unitAbility);
      return unitAbility;
    }
  }

  public void addAbility(final GamePlayer player, final CombatUnitAbility unitAbility) {
    abilities.computeIfAbsent(player, p -> new ArrayList<>()).add(unitAbility);
  }
}
