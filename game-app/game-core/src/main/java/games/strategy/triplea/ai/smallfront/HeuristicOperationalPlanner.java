package games.strategy.triplea.ai.smallfront;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.attachments.SupplyTerritoryAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.AirControlTracker;
import games.strategy.triplea.delegate.supply.SupplyNetworkResolver;
import games.strategy.triplea.delegate.visibility.VisibilityService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Builds a deterministic turn plan from the same fog-safe state available to strategic agents. */
public final class HeuristicOperationalPlanner implements OperationalTurnPlanner {
  private static final int NO_PATH = 10_000;

  @Override
  public OperationalTurnPlan plan(final GameData data, final GamePlayer player) {
    final Set<Territory> visible = VisibilityService.getVisibleTerritories(player, data);
    final List<Territory> friendlyOrigins = friendlyGroundOrigins(data, player);
    final List<Territory> targets = candidateTargets(data, player, visible, friendlyOrigins);
    final Set<String> protectedTerritories = protectedTerritories(data, player, visible);
    final List<OperationalTurnPlan.Objective> objectives = new ArrayList<>();

    final Optional<Territory> primaryTarget = targets.stream().findFirst();
    primaryTarget.ifPresent(
        target -> {
          final String airObjectiveId = "gain-air-" + slug(target.getName());
          final boolean needsAirObjective =
              AirControlTracker.isEnabled(data)
                  && visible.contains(target)
                  && hasVisibleEnemyGround(target, player, data)
                  && hasFriendlyAir(data, player);
          if (needsAirObjective) {
            objectives.add(
                new OperationalTurnPlan.Objective(
                    airObjectiveId,
                    OperationalObjectiveType.GAIN_AIR_SUPERIORITY,
                    target.getName(),
                    100,
                    Set.of()));
          }
          objectives.add(
              new OperationalTurnPlan.Objective(
                  "capture-" + slug(target.getName()),
                  OperationalObjectiveType.CAPTURE,
                  target.getName(),
                  95,
                  needsAirObjective ? Set.of(airObjectiveId) : Set.of()));
        });

    targets.stream()
        .skip(1)
        .limit(2)
        .forEach(
            target ->
                objectives.add(
                    new OperationalTurnPlan.Objective(
                        "capture-" + slug(target.getName()),
                        OperationalObjectiveType.CAPTURE,
                        target.getName(),
                        60,
                        Set.of())));

    protectedTerritories.stream()
        .map(data.getMap()::getTerritoryOrThrow)
        .filter(HeuristicOperationalPlanner::isSupplySource)
        .forEach(
            territory ->
                objectives.add(
                    new OperationalTurnPlan.Objective(
                        "protect-supply-" + slug(territory.getName()),
                        OperationalObjectiveType.PROTECT_SUPPLY,
                        territory.getName(),
                        85,
                        Set.of())));

    visible.stream()
        .filter(territory -> isFriendly(territory.getOwner(), player, data))
        .filter(HeuristicOperationalPlanner::isObjective)
        .filter(territory -> hasVisibleEnemyNeighbor(territory, visible, player, data))
        .sorted(Comparator.comparing(Territory::getName))
        .forEach(
            territory ->
                objectives.add(
                    new OperationalTurnPlan.Objective(
                        "hold-" + slug(territory.getName()),
                        OperationalObjectiveType.HOLD,
                        territory.getName(),
                        80,
                        Set.of())));

    final String focus = primaryTarget.map(Territory::getName).orElse("the current front");
    final String commanderIntent =
        "Concentrate on "
            + focus
            + ", preserve supply continuity, and retain a local reserve before expanding "
            + "the attack.";
    return new OperationalTurnPlan(
        OperationalTurnPlan.SCHEMA_VERSION,
        slug(player.getName()) + "-r" + data.getSequence().getRound() + "-" + slug(focus),
        player.getName(),
        data.getSequence().getRound(),
        commanderIntent,
        objectives,
        protectedTerritories,
        1);
  }

  private static List<Territory> friendlyGroundOrigins(
      final GameData data, final GamePlayer player) {
    return data.getMap().getTerritories().stream()
        .filter(territory -> !territory.isWater())
        .filter(
            territory ->
                territory.getUnitCollection().getUnits().stream()
                    .filter(Matches.unitIsLand())
                    .anyMatch(unit -> unit.isOwnedBy(player)))
        .sorted(Comparator.comparing(Territory::getName))
        .toList();
  }

  private static List<Territory> candidateTargets(
      final GameData data,
      final GamePlayer player,
      final Set<Territory> visible,
      final List<Territory> friendlyOrigins) {
    final List<Territory> objectives =
        data.getMap().getTerritories().stream()
            .filter(HeuristicOperationalPlanner::isObjective)
            .filter(
                territory ->
                    !visible.contains(territory) || !isFriendly(territory.getOwner(), player, data))
            .toList();
    final List<Territory> contacts =
        visible.stream()
            .filter(territory -> isEnemy(territory.getOwner(), player, data))
            .filter(territory -> !territory.isWater())
            .filter(
                territory ->
                    data.getMap().getNeighbors(territory).stream()
                        .anyMatch(
                            neighbor ->
                                isFriendly(neighbor.getOwner(), player, data)
                                    || neighbor.getUnitCollection().getUnits().stream()
                                        .anyMatch(unit -> unit.isOwnedBy(player))))
            .toList();
    return java.util.stream.Stream.concat(objectives.stream(), contacts.stream())
        .distinct()
        .sorted(
            Comparator.<Territory>comparingInt(
                    territory -> minimumDistance(data, friendlyOrigins, territory))
                .thenComparing(Territory::getName))
        .toList();
  }

  private static Set<String> protectedTerritories(
      final GameData data, final GamePlayer player, final Set<Territory> visible) {
    final Set<String> protectedNames = new LinkedHashSet<>();
    data.getMap().getTerritories().stream()
        .filter(
            territory ->
                visible.contains(territory)
                    || territory.getUnitCollection().getUnits().stream()
                        .anyMatch(unit -> unit.isOwnedBy(player)))
        .filter(territory -> isFriendly(territory.getOwner(), player, data))
        .filter(
            territory ->
                isSupplySource(territory)
                    || isObjective(territory)
                    || hasVisibleEnemyNeighbor(territory, visible, player, data))
        .filter(
            territory ->
                territory.getUnitCollection().getUnits().stream()
                    .filter(Matches.unitIsLand())
                    .anyMatch(unit -> unit.isOwnedBy(player)))
        .sorted(Comparator.comparing(Territory::getName))
        .map(Territory::getName)
        .forEach(protectedNames::add);
    if (SupplyNetworkResolver.isEnabled(data)) {
      SupplyNetworkResolver.getSupplySources(player, data).stream()
          .filter(
              source ->
                  visible.contains(source)
                      || source.getUnitCollection().getUnits().stream()
                          .anyMatch(unit -> unit.isOwnedBy(player)))
          .filter(source -> isFriendly(source.getOwner(), player, data))
          .sorted(Comparator.comparing(Territory::getName))
          .map(Territory::getName)
          .forEach(protectedNames::add);
    }
    return Set.copyOf(protectedNames);
  }

  private static int minimumDistance(
      final GameData data, final List<Territory> origins, final Territory target) {
    return origins.stream()
        .mapToInt(
            origin -> {
              final int distance =
                  data.getMap().getDistance(origin, target, Matches.territoryIsLand());
              return distance < 0 ? NO_PATH : distance;
            })
        .min()
        .orElse(NO_PATH);
  }

  private static boolean hasFriendlyAir(final GameData data, final GamePlayer player) {
    return data.getMap().getTerritories().stream()
        .flatMap(territory -> territory.getUnitCollection().getUnits().stream())
        .filter(Matches.unitIsAir())
        .anyMatch(unit -> unit.isOwnedBy(player));
  }

  private static boolean hasVisibleEnemyGround(
      final Territory territory, final GamePlayer player, final GameData data) {
    return territory.getUnitCollection().getUnits().stream()
        .filter(Matches.unitIsLand())
        .anyMatch(unit -> isEnemy(unit.getOwner(), player, data));
  }

  private static boolean hasVisibleEnemyNeighbor(
      final Territory territory,
      final Set<Territory> visible,
      final GamePlayer player,
      final GameData data) {
    return data.getMap().getNeighbors(territory).stream()
        .filter(visible::contains)
        .map(Territory::getOwner)
        .anyMatch(owner -> isEnemy(owner, player, data));
  }

  private static boolean isObjective(final Territory territory) {
    return TerritoryAttachment.get(territory).map(TerritoryAttachment::getVictoryCity).orElse(0)
        > 0;
  }

  private static boolean isSupplySource(final Territory territory) {
    return SupplyTerritoryAttachment.get(territory)
        .map(SupplyTerritoryAttachment::getSupplySource)
        .orElse(false);
  }

  private static boolean isEnemy(
      final GamePlayer candidate, final GamePlayer player, final GameData data) {
    return !candidate.isNull()
        && !candidate.equals(player)
        && data.getRelationshipTracker().getRelationship(player, candidate) != null
        && data.getRelationshipTracker().isAtWar(player, candidate);
  }

  private static boolean isFriendly(
      final GamePlayer candidate, final GamePlayer player, final GameData data) {
    return !candidate.isNull()
        && (candidate.equals(player)
            || (data.getRelationshipTracker().getRelationship(player, candidate) != null
                && data.getRelationshipTracker().isAllied(player, candidate)));
  }

  private static String slug(final String value) {
    final String slug = value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
    return slug.replaceAll("^-|-$", "");
  }
}
