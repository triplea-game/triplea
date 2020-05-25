package games.strategy.triplea.delegate.battle.firinggroups;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.TechAbilityAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import lombok.Builder;
import lombok.NonNull;
import org.triplea.java.collections.CollectionUtils;

@Builder
public class Aa {

  private @NonNull final Collection<Unit> aaUnits;
  private @NonNull final List<String> aaTypes;
  private @NonNull final GamePlayer hitPlayer;
  private @NonNull final Collection<Unit> attackableUnits;
  private final boolean defending;
  private @NonNull final GameData gameData;

  public List<FiringGroup> getFiringGroups() {
    final List<FiringGroup> aaGroupsAndTargets = new ArrayList<>();

    for (final String aaType : aaTypes) {
      final Collection<Unit> aaTypeUnits =
          CollectionUtils.getMatches(aaUnits, Matches.unitIsAaOfTypeAa(aaType));
      aaGroupsAndTargets.addAll(getGroupsAndTargets(aaType, aaTypeUnits));
    }
    return aaGroupsAndTargets;
  }

  private List<FiringGroup> getGroupsAndTargets(
      final String aaType, final Collection<Unit> aaTypeUnits) {

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
