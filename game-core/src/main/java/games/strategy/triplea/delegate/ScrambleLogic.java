package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitCollection;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.IBattle.BattleType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.triplea.java.PredicateBuilder;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.util.Tuple;

public class ScrambleLogic {
  private final GameData data;
  private final PlayerId player;
  private final Set<Territory> territoriesWithBattles;
  private final Predicate<Unit> airbasesCanScramble;
  private @Nullable BattleTracker battleTracker;

  public ScrambleLogic(
      final GameData data, final PlayerId player, final Set<Territory> territoriesWithBattles) {
    this.data = data;
    this.player = player;
    this.territoriesWithBattles = territoriesWithBattles;
    this.airbasesCanScramble =
        Matches.unitIsEnemyOf(data, player)
            .and(Matches.unitIsAirBase())
            .and(Matches.unitIsNotDisabled())
            .and(Matches.unitIsBeingTransported().negate());
  }

  public void setBattleTracker(final BattleTracker battleTracker) {
    this.battleTracker = battleTracker;
  }

  public Predicate<Unit> getAirbasesCanScramble() {
    return airbasesCanScramble;
  }

  public Collection<Unit> getUnitsThatCanScramble() {
    final var units = new ArrayList<Unit>();
    for (final var entries : findPossibleScramblers().values()) {
      for (final var tuple : entries.values()) {
        units.addAll(tuple.getSecond());
      }
    }
    return units;
  }

  public Map<Territory, Map<Territory, Tuple<Collection<Unit>, Collection<Unit>>>>
      findPossibleScramblers() {
    // first, figure out all the territories where scrambling units could scramble to
    // then ask the defending player if they wish to scramble units there, and actually move the
    // units there
    if (!Properties.getScrambleRulesInEffect(data)) {
      return Map.of();
    }
    final boolean fromIslandOnly = Properties.getScrambleFromIslandOnly(data);
    final boolean toSeaOnly = Properties.getScrambleToSeaOnly(data);
    final boolean toAnyAmphibious = Properties.getScrambleToAnyAmphibiousAssault(data);
    int maxScrambleDistance = 0;
    for (final UnitType unitType : data.getUnitTypeList()) {
      final UnitAttachment ua = UnitAttachment.get(unitType);
      if (ua.getCanScramble() && maxScrambleDistance < ua.getMaxScrambleDistance()) {
        maxScrambleDistance = ua.getMaxScrambleDistance();
      }
    }
    final Predicate<Territory> canScramble =
        PredicateBuilder.of(Matches.territoryIsWater().or(Matches.isTerritoryEnemy(player, data)))
            .and(
                Matches.territoryHasUnitsThatMatch(
                    Matches.unitCanScramble()
                        .and(Matches.unitIsEnemyOf(data, player))
                        .and(Matches.unitIsNotDisabled())))
            .and(Matches.territoryHasUnitsThatMatch(airbasesCanScramble))
            .andIf(fromIslandOnly, Matches.territoryIsIsland())
            .build();

    final Set<Territory> territoriesWithBattlesWater =
        new HashSet<>(
            CollectionUtils.getMatches(territoriesWithBattles, Matches.territoryIsWater()));
    final Set<Territory> territoriesWithBattlesLand =
        new HashSet<>(
            CollectionUtils.getMatches(territoriesWithBattles, Matches.territoryIsLand()));
    final Map<Territory, Set<Territory>> scrambleTerrs = new HashMap<>();
    for (final Territory battleTerr : territoriesWithBattlesWater) {
      final Collection<Territory> canScrambleFrom =
          CollectionUtils.getMatches(
              data.getMap().getNeighbors(battleTerr, maxScrambleDistance), canScramble);
      if (!canScrambleFrom.isEmpty()) {
        scrambleTerrs.put(battleTerr, new HashSet<>(canScrambleFrom));
      }
    }
    for (final Territory battleTerr : territoriesWithBattlesLand) {
      if (!toSeaOnly) {
        final Collection<Territory> canScrambleFrom =
            CollectionUtils.getMatches(
                data.getMap().getNeighbors(battleTerr, maxScrambleDistance), canScramble);
        if (!canScrambleFrom.isEmpty()) {
          scrambleTerrs.put(battleTerr, new HashSet<>(canScrambleFrom));
        }
      }
      final IBattle battle = battleTracker.getPendingBattle(battleTerr, false, BattleType.NORMAL);
      // do not forget we may already have the territory in the list, so we need to add to the
      // collection, not overwrite it.
      if (battle != null && battle.isAmphibious() && battle instanceof DependentBattle) {
        final Collection<Territory> amphibFromTerrs =
            ((DependentBattle) battle).getAmphibiousAttackTerritories();
        amphibFromTerrs.removeAll(territoriesWithBattlesWater);
        for (final Territory amphibFrom : amphibFromTerrs) {
          final Set<Territory> canScrambleFrom =
              scrambleTerrs.getOrDefault(amphibFrom, new HashSet<>());
          if (toAnyAmphibious) {
            canScrambleFrom.addAll(
                CollectionUtils.getMatches(
                    data.getMap().getNeighbors(amphibFrom, maxScrambleDistance), canScramble));
          } else if (canScramble.test(battleTerr)) {
            canScrambleFrom.add(battleTerr);
          }
          if (!canScrambleFrom.isEmpty()) {
            scrambleTerrs.put(amphibFrom, canScrambleFrom);
          }
        }
      }
    }
    // now scrambleTerrs is a list of places we can scramble from
    if (scrambleTerrs.isEmpty()) {
      return Map.of();
    }
    final Predicate<Unit> unitCanScramble =
        Matches.unitIsEnemyOf(data, player)
            .and(Matches.unitCanScramble())
            .and(Matches.unitIsNotDisabled())
            .and(Matches.unitWasScrambled().negate());

    final Map<Territory, Map<Territory, Tuple<Collection<Unit>, Collection<Unit>>>>
        scramblersByTerritoryPlayer = new HashMap<>();
    for (final Territory to : scrambleTerrs.keySet()) {
      final Map<Territory, Tuple<Collection<Unit>, Collection<Unit>>> scramblers = new HashMap<>();
      for (final Territory from : scrambleTerrs.get(to)) {
        // find how many is the max this territory can scramble
        final UnitCollection fromUnits = from.getUnitCollection();
        final Collection<Unit> airbases = fromUnits.getMatches(airbasesCanScramble);
        if (getMaxScrambleCount(airbases) == 0) {
          continue;
        }
        // TODO: consider movement cost and canals by checking each air unit separately
        final Route toBattleRoute =
            data.getMap().getRoute(from, to, Matches.territoryIsNotImpassable());
        final Collection<Unit> canScrambleAir =
            fromUnits.getMatches(
                unitCanScramble.and(Matches.unitCanScrambleOnRouteDistance(toBattleRoute)));
        if (!canScrambleAir.isEmpty()) {
          scramblers.put(from, Tuple.of(airbases, canScrambleAir));
        }
      }
      if (scramblers.isEmpty()) {
        continue;
      }
      scramblersByTerritoryPlayer.put(to, scramblers);
    }
    return scramblersByTerritoryPlayer;
  }

  /**
   * Returns the maximum number of units that can scramble from the specified air bases.
   *
   * @return {@link Integer#MAX_VALUE} if any air base can scramble an infinite number of units.
   */
  public static int getMaxScrambleCount(final Collection<Unit> airbases) {
    if (airbases.isEmpty()
        || !airbases.stream().allMatch(Matches.unitIsAirBase().and(Matches.unitIsNotDisabled()))) {
      throw new IllegalStateException("All units must be viable airbases");
    }
    // find how many is the max this territory can scramble
    int maxScrambled = 0;
    for (final Unit base : airbases) {
      final int baseMax = ((TripleAUnit) base).getMaxScrambleCount();
      if (baseMax == -1) {
        return Integer.MAX_VALUE;
      }
      maxScrambled += baseMax;
    }
    return maxScrambled;
  }
}
