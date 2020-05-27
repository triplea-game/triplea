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

/** Detects any subs that are only opposed by air units */
@Builder
public class FindSubsVsOnlyAir {

  private @NonNull final Collection<Unit> friendlyUnits;
  private @NonNull final Collection<Unit> enemyUnits;

  public List<Unit> find() {
    // if ALL enemy units are AIR, return any friendly sub
    if (enemyUnits.stream().allMatch(Matches.unitIsAir())) {
      final Predicate<Unit> subMatch =
          Matches.unitCanEvade().and(Matches.unitCanNotBeTargetedByAll());
      return CollectionUtils.getMatches(friendlyUnits, subMatch);
    }

    return List.of();
  }
}
