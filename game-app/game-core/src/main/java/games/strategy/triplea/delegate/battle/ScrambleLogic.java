package games.strategy.triplea.delegate.battle;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitCollection;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.IBattle.BattleType;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import lombok.Getter;
import org.triplea.java.PredicateBuilder;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.util.Tuple;

/**
 * ScrambeLogic encapsulates the logic for finding possible units that can scramble to defend a
 * given territory.
 */
public class ScrambleLogic {
  private final GameState data;
  private final GamePlayer player;
  private final Set<Territory> territoriesWithBattles;
  private final BattleTracker battleTracker;
  @Getter private final Predicate<Unit> airbaseThatCanScramblePredicate;
  private final Predicate<Territory> canScrambleFromPredicate;
  private final int maxScrambleDistance;

  public ScrambleLogic(final GameState data, final GamePlayer player, final Territory territory) {
    this(data, player, Set.of(territory));
  }

  public ScrambleLogic(
      final GameState data, final GamePlayer player, final Set<Territory> territoriesWithBattles) {
    this(data, player, territoriesWithBattles, new BattleTracker());
  }

  public ScrambleLogic(
      final GameState data,
      final GamePlayer player,
      final Set<Territory> territoriesWithBattles,
      final BattleTracker battleTracker) {
    if (!Properties.getScrambleRulesInEffect(data.getProperties())) {
      throw new IllegalStateException("Scrambling not supported");
    }
    this.data = data;
    this.player = player;
    this.territoriesWithBattles = territoriesWithBattles;
    this.battleTracker = battleTracker;
    this.airbaseThatCanScramblePredicate =
        Matches.unitIsEnemyOf(player)
            .and(Matches.unitIsAirBase())
            .and(Matches.unitIsNotDisabled())
            .and(Matches.unitIsBeingTransported().negate());
    this.canScrambleFromPredicate =
        PredicateBuilder.of(Matches.territoryIsWater().or(Matches.isTerritoryEnemy(player)))
            .and(
                Matches.territoryHasUnitsThatMatch(
                    Matches.unitCanScramble()
                        .and(Matches.unitIsEnemyOf(player))
                        .and(Matches.unitIsNotDisabled())))
            .and(Matches.territoryHasUnitsThatMatch(airbaseThatCanScramblePredicate))
            .andIf(
                Properties.getScrambleFromIslandOnly(data.getProperties()),
                Matches.territoryIsIsland())
            .build();
    this.maxScrambleDistance = computeMaxScrambleDistance(data);
  }

  private static int computeMaxScrambleDistance(final GameState data) {
    int maxScrambleDistance = 0;
    for (final UnitType unitType : data.getUnitTypeList()) {
      final UnitAttachment ua = unitType.getUnitAttachment();
      if (ua.getCanScramble() && maxScrambleDistance < ua.getMaxScrambleDistance()) {
        maxScrambleDistance = ua.getMaxScrambleDistance();
      }
    }
    return maxScrambleDistance;
  }

  /**
   * Returns all units that can be scrambled for the given construction parameters.
   *
   * @return The units that can be scrambled.
   */
  public Collection<Unit> getUnitsThatCanScramble() {
    final var units = new HashSet<Unit>();
    for (final var entries : getUnitsThatCanScrambleByDestination().values()) {
      for (final var tuple : entries.values()) {
        units.addAll(tuple.getSecond());
      }
    }
    return units;
  }

  /**
   * Returns the possible scramblers keyed by territory to scramble to. The value is a map from
   * territory to scramble from to a Tuple of unit collections, with the first being the airbases
   * used for scrambling and the second being the units to scramble.
   *
   * <p>Note: Since the same unit may be able scramble to different territories, the same unit may
   * appear in the list of scramblers for different 'to' territories.
   *
   * <p>TODO: Simplify by getting rid of the Tuple - make a dedicated class or don't return
   * airbases.
   *
   * @return The units that can be scrambled keyed by territory to scramble to.
   */
  public Map<Territory, Map<Territory, Tuple<Collection<Unit>, Collection<Unit>>>>
      getUnitsThatCanScrambleByDestination() {
    // first, figure out all the territories where scrambling units could scramble to
    // then ask the defending player if they wish to scramble units there, and actually move the
    // units there
    final boolean toSeaOnly = Properties.getScrambleToSeaOnly(data.getProperties());
    final boolean toAnyAmphibious =
        Properties.getScrambleToAnyAmphibiousAssault(data.getProperties());

    final Collection<Territory> territoriesWithBattlesWater =
        CollectionUtils.getMatches(territoriesWithBattles, Matches.territoryIsWater());
    final Collection<Territory> territoriesWithBattlesLand =
        CollectionUtils.getMatches(territoriesWithBattles, Matches.territoryIsLand());
    final Map<Territory, Set<Territory>> scrambleTerrs = new HashMap<>();
    for (final Territory battleTerr : territoriesWithBattlesWater) {
      final Collection<Territory> canScrambleFrom = getCanScrambleFromTerritories(battleTerr);
      if (!canScrambleFrom.isEmpty()) {
        scrambleTerrs.put(battleTerr, new HashSet<>(canScrambleFrom));
      }
    }
    for (final Territory battleTerr : territoriesWithBattlesLand) {
      if (!toSeaOnly) {
        final Collection<Territory> canScrambleFrom = getCanScrambleFromTerritories(battleTerr);
        if (!canScrambleFrom.isEmpty()) {
          scrambleTerrs.put(battleTerr, new HashSet<>(canScrambleFrom));
        }
      }
      final IBattle battle = battleTracker.getPendingBattle(battleTerr, BattleType.NORMAL);
      // do not forget we may already have the territory in the list, so we need to add to the
      // collection, not overwrite it.
      if (battle != null && battle.isAmphibious() && battle instanceof DependentBattle) {
        final Collection<Territory> amphibFromTerrs =
            ((DependentBattle) battle).getAmphibiousAttackTerritories();
        amphibFromTerrs.removeAll(territoriesWithBattlesWater);
        for (final Territory amphibFrom : amphibFromTerrs) {
          if (toAnyAmphibious) {
            final Collection<Territory> territories = getCanScrambleFromTerritories(amphibFrom);
            scrambleTerrs.computeIfAbsent(amphibFrom, key -> new HashSet<>()).addAll(territories);
          } else if (canScrambleFromPredicate.test(battleTerr)) {
            scrambleTerrs.computeIfAbsent(amphibFrom, key -> new HashSet<>()).add(battleTerr);
          }
        }
      }
    }
    // now scrambleTerrs is a list of places we can scramble from
    if (scrambleTerrs.isEmpty()) {
      return Map.of();
    }
    final Predicate<Unit> unitCanScramble =
        Matches.unitIsEnemyOf(player)
            .and(Matches.unitCanScramble())
            .and(Matches.unitIsNotDisabled())
            .and(Matches.unitWasScrambled().negate());

    final Map<Territory, Map<Territory, Tuple<Collection<Unit>, Collection<Unit>>>>
        scramblersByTerritory = new HashMap<>();
    for (final Territory to : scrambleTerrs.keySet()) {
      final Map<Territory, Tuple<Collection<Unit>, Collection<Unit>>> scramblers = new HashMap<>();
      for (final Territory from : scrambleTerrs.get(to)) {
        // find how many is the max this territory can scramble
        final UnitCollection fromUnits = from.getUnitCollection();
        final Collection<Unit> airbases = fromUnits.getMatches(airbaseThatCanScramblePredicate);
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
      scramblersByTerritory.put(to, scramblers);
    }
    return scramblersByTerritory;
  }

  private Collection<Territory> getCanScrambleFromTerritories(final Territory battleTerr) {
    return CollectionUtils.getMatches(
        data.getMap().getNeighbors(battleTerr, maxScrambleDistance), canScrambleFromPredicate);
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
    for (final Unit airbase : airbases) {
      final int baseMax = airbase.getMaxScrambleCount();
      if (baseMax == -1) {
        return Integer.MAX_VALUE;
      }
      maxScrambled += baseMax;
    }
    return maxScrambled;
  }
}
