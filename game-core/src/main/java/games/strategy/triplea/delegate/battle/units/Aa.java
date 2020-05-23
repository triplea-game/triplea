package games.strategy.triplea.delegate.battle.units;

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
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.triplea.java.collections.CollectionUtils;

@Builder
public class Aa {
  private @NonNull final Collection<Unit> firingUnits;
  private @NonNull final Collection<Unit> targetUnits;
  private @NonNull final GameData gameData;
  private @NonNull final GamePlayer hitPlayer;
  private final int round;

  @Value(staticConstructor = "of")
  public static class Result {
    List<Unit> units;
    List<String> types;
  }

  public Result offensiveUnits() {
    // no airborne targets for offensive aa
    final Map<String, Set<UnitType>> airborneTechTargetsAllowed = Map.of();
    return getUnits(hitPlayer, airborneTechTargetsAllowed, false);
  }

  public Result defensiveUnits() {
    final Map<String, Set<UnitType>> airborneTechTargetsAllowed =
        TechAbilityAttachment.getAirborneTargettedByAa(hitPlayer, gameData);
    return getUnits(hitPlayer, airborneTechTargetsAllowed, true);
  }

  private Result getUnits(
      final GamePlayer hitPlayer,
      final Map<String, Set<UnitType>> airborneTechTargetsAllowed,
      final boolean defending) {
    final List<Unit> aaUnits =
        CollectionUtils.getMatches(
            firingUnits,
            Matches.unitIsAaThatCanFire(
                targetUnits,
                airborneTechTargetsAllowed,
                hitPlayer,
                Matches.unitIsAaForCombatOnly(),
                round,
                defending,
                gameData));
    // comes ordered alphabetically
    final List<String> aaTypes = UnitAttachment.getAllOfTypeAas(aaUnits);
    // stacks are backwards
    Collections.reverse(aaTypes);
    return Result.of(aaUnits, aaTypes);
  }
}
