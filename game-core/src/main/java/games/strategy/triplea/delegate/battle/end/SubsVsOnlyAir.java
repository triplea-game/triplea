package games.strategy.triplea.delegate.battle.end;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.Matches;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.triplea.java.collections.CollectionUtils;

@Builder
public class SubsVsOnlyAir {

  private @NonNull final Collection<Unit> attackingUnits;
  private @NonNull final Collection<Unit> defendingUnits;

  @Value(staticConstructor = "of")
  public static class Result {
    Collection<Unit> subs;
    boolean isAttacker;
  }

  private final Result emptyResult = Result.of(List.of(), false);

  /** Submerge attacking/defending subs if they're alone OR with transports against only air. */
  public Result check() {
    // if All attackers are AIR, submerge any defending subs
    final Predicate<Unit> subMatch =
        Matches.unitCanEvade().and(Matches.unitCanNotBeTargetedByAll());
    if (!attackingUnits.isEmpty()
        && attackingUnits.stream().allMatch(Matches.unitIsAir())
        && defendingUnits.stream().anyMatch(subMatch)) {
      // Get all defending subs (including allies) in the territory
      final List<Unit> defendingSubs = CollectionUtils.getMatches(defendingUnits, subMatch);
      return Result.of(defendingSubs, false);
      // checking defending air on attacking subs
    } else if (!defendingUnits.isEmpty()
        && defendingUnits.stream().allMatch(Matches.unitIsAir())
        && attackingUnits.stream().anyMatch(subMatch)) {
      // Get all attacking subs in the territory
      final List<Unit> attackingSubs = CollectionUtils.getMatches(attackingUnits, subMatch);
      return Result.of(attackingSubs, true);
    }

    return emptyResult;
  }
}
