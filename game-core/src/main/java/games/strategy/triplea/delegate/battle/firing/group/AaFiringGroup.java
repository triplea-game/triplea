package games.strategy.triplea.delegate.battle.firing.group;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.TechAbilityAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.NonNull;
import org.triplea.java.collections.CollectionUtils;

@Builder
public class AaFiringGroup {

  private @NonNull final Collection<Unit> aaUnits;
  private @NonNull final GamePlayer hitPlayer;
  private @NonNull final Collection<Unit> attackableUnits;
  private final boolean defending;
  private @NonNull final GameData gameData;

  public List<FiringGroup> getFiringGroups() {
    final List<String> aaTypes = UnitAttachment.getAllOfTypeAas(aaUnits);
    return aaTypes.stream()
        // aaTypes come ordered alphabetically but stacks are backwards so reverse the order
        .sorted(Collections.reverseOrder())
        .map(this::mapAaTypesToFiringGroups)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  private List<FiringGroup> mapAaTypesToFiringGroups(final String aaType) {

    final Collection<Unit> aaTypeUnits =
        CollectionUtils.getMatches(aaUnits, Matches.unitIsAaOfTypeAa(aaType));
    return FiringGroup.newFiringUnitGroups(
        aaTypeUnits, firingGroup -> getValidTargets(aaType, firingGroup), aaType);
  }

  private Collection<Unit> getValidTargets(
      final String aaType, final Collection<Unit> firingGroup) {
    final Set<UnitType> validTargetTypes =
        UnitAttachment.get(firingGroup.iterator().next().getType()).getTargetsAa(gameData);

    Predicate<Unit> validTargetTypesOrAirborneTypes = Matches.unitIsOfTypes(validTargetTypes);
    if (defending) {
      final Set<UnitType> airborneTypesTargeted =
          TechAbilityAttachment.getAirborneTargettedByAa(hitPlayer, gameData).get(aaType);
      validTargetTypesOrAirborneTypes =
          validTargetTypesOrAirborneTypes.or(
              Matches.unitIsAirborne().and(Matches.unitIsOfTypes(airborneTypesTargeted)));
    }
    return CollectionUtils.getMatches(attackableUnits, validTargetTypesOrAirborneTypes);
  }
}
