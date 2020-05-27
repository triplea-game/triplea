package games.strategy.triplea.delegate.battle.units;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.TechAbilityAttachment;
import games.strategy.triplea.delegate.Matches;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.NonNull;
import org.triplea.java.collections.CollectionUtils;

@Builder
public class FindAaUnits {
  private @NonNull final Collection<Unit> firingUnits;
  private @NonNull final Collection<Unit> targetUnits;
  private @NonNull final GameData gameData;
  private @NonNull final GamePlayer hitPlayer;
  private @NonNull final Integer round;

  public List<Unit> offensive() {
    // no airborne targets for offensive aa
    final Map<String, Set<UnitType>> airborneTechTargetsAllowed = Map.of();
    return getUnits(hitPlayer, airborneTechTargetsAllowed, false);
  }

  public List<Unit> defensive() {
    final Map<String, Set<UnitType>> airborneTechTargetsAllowed =
        TechAbilityAttachment.getAirborneTargettedByAa(hitPlayer, gameData);
    return getUnits(hitPlayer, airborneTechTargetsAllowed, true);
  }

  private List<Unit> getUnits(
      final GamePlayer hitPlayer,
      final Map<String, Set<UnitType>> airborneTechTargetsAllowed,
      final boolean defending) {
    return CollectionUtils.getMatches(
        firingUnits,
        Matches.unitIsAaThatCanFire(
            targetUnits,
            airborneTechTargetsAllowed,
            hitPlayer,
            Matches.unitIsAaForCombatOnly(),
            round,
            defending,
            gameData));
  }
}
