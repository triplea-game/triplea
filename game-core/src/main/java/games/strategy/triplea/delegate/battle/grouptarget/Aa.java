package games.strategy.triplea.delegate.battle.grouptarget;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.TechAbilityAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
      getGroupsAndTargets(aaGroupsAndTargets, aaType, aaTypeUnits);
    }
    return aaGroupsAndTargets;
  }

  private void getGroupsAndTargets(
      final List<FiringGroup> aaGroupsAndTargets,
      final String aaType,
      final Collection<Unit> aaTypeUnits) {
    final List<Collection<Unit>> firingGroups = FiringGroup.newFiringUnitGroups(aaTypeUnits);
    for (final Collection<Unit> firingGroup : firingGroups) {
      final Collection<Unit> validTargets = getValidTargets(aaType, firingGroup);

      if (firingGroup.isEmpty() || validTargets.isEmpty()) {
        continue;
      }
      final boolean isSuicideOnHit = firingGroup.stream().anyMatch(Matches.unitIsSuicideOnHit());

      aaGroupsAndTargets.add(FiringGroup.of(firingGroup, validTargets, isSuicideOnHit, aaType));
    }
  }

  private Collection<Unit> getValidTargets(
      final String aaType, final Collection<Unit> firingGroup) {
    final Set<UnitType> validTargetTypes =
        UnitAttachment.get(firingGroup.iterator().next().getType()).getTargetsAa(gameData);
    final Set<UnitType> airborneTypesTargeted =
        defending
            ? TechAbilityAttachment.getAirborneTargettedByAa(hitPlayer, gameData).get(aaType)
            : new HashSet<>();
    return CollectionUtils.getMatches(
        attackableUnits,
        Matches.unitIsOfTypes(validTargetTypes)
            .or(Matches.unitIsAirborne().and(Matches.unitIsOfTypes(airborneTypesTargeted))));
  }
}
