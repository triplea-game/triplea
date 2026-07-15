package games.strategy.triplea.delegate.strategic.simulation;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.SupplyTerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.AirControlTracker;
import games.strategy.triplea.delegate.battle.BattleDelegate;
import games.strategy.triplea.delegate.battle.IBattle;
import games.strategy.triplea.delegate.battle.IBattle.BattleType;
import games.strategy.triplea.delegate.battle.simulation.BattleObservation;
import games.strategy.triplea.delegate.reinforcement.FixedReinforcementDelegate;
import games.strategy.triplea.delegate.reinforcement.FixedReinforcementObservation;
import games.strategy.triplea.delegate.reinforcement.FixedReinforcementObservationFactory;
import games.strategy.triplea.delegate.reinforcement.FixedReinforcementTracker;
import games.strategy.triplea.delegate.supply.SupplyNetworkResolver;
import games.strategy.triplea.delegate.visibility.VisibilityService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import javax.annotation.Nullable;

/** Builds deterministic local-front observations without exposing hidden territory state. */
public final class StrategicObservationFactory {
  private static final Comparator<IBattle> BATTLE_ORDER =
      Comparator.comparing((IBattle battle) -> battle.getTerritory().getName())
          .thenComparing(battle -> battle.getBattleType().name())
          .thenComparing(battle -> battle.getBattleId().toString());

  private StrategicObservationFactory() {}

  public static StrategicObservation create(
      final GameData data,
      final GamePlayer player,
      final long seed,
      final StrategicPhase phase,
      final @Nullable BattleObservation battle) {
    final Set<Territory> visible = VisibilityService.getVisibleTerritories(player, data);
    final List<Territory> territories = new ArrayList<>(data.getMap().getTerritories());
    territories.sort(Comparator.comparing(Territory::getName));
    final AirControlTracker airControl = AirControlTracker.get(data);
    final List<StrategicObservation.TerritoryState> territoryStates = new ArrayList<>();
    for (final Territory territory : territories) {
      final boolean territoryVisible = visible.contains(territory);
      final AirControlTracker.Status airControlStatus =
          territoryVisible
              ? airControl.getStatus(territory, data)
              : AirControlTracker.Status.UNCONTROLLED;
      territoryStates.add(
          new StrategicObservation.TerritoryState(
              territory.getName(),
              territory.isWater(),
              territoryVisible,
              territoryVisible ? territory.getOwner().getName() : null,
              territoryVisible ? SupplyNetworkResolver.isSupplied(territory, player, data) : null,
              territoryVisible
                  && SupplyTerritoryAttachment.get(territory)
                      .map(SupplyTerritoryAttachment::getSupplySource)
                      .orElse(false),
              territoryVisible && airControlStatus == AirControlTracker.Status.CONTROLLED
                  ? airControl.getController(territory, data).map(GamePlayer::getName).orElse(null)
                  : null,
              territoryVisible ? airControlStatus.name() : null,
              territoryVisible && airControlStatus != AirControlTracker.Status.UNCONTROLLED
                  ? AirControlTracker.isPersistent(data)
                  : null,
              data.getMap().getNeighbors(territory).stream()
                  .map(Territory::getName)
                  .sorted()
                  .toList(),
              SupplyNetworkResolver.isEnabled(data)
                  ? SupplyNetworkResolver.getRoadNeighbors(territory, data).stream()
                      .map(Territory::getName)
                      .sorted()
                      .toList()
                  : List.of(),
              territoryVisible ? groupUnits(territory, player, data) : List.of()));
    }

    final FixedReinforcementObservation reinforcements =
        FixedReinforcementObservationFactory.create(data, player, reinforcementTracker(data));
    final List<StrategicObservation.PendingBattle> pendingBattles =
        pendingBattles(data).stream()
            .filter(pending -> visible.contains(pending.getTerritory()))
            .map(
                pending ->
                    new StrategicObservation.PendingBattle(
                        pending.getBattleId().toString(),
                        pending.getTerritory().getName(),
                        pending.getBattleType().name()))
            .toList();
    final StrategicDecisionDomain domain =
        phase == StrategicPhase.COMPLETE
            ? StrategicDecisionDomain.COMPLETE
            : phase == StrategicPhase.BATTLE && battle != null && !battle.over()
                ? StrategicDecisionDomain.BATTLE
                : StrategicDecisionDomain.STRATEGIC;
    final String sequenceStep =
        data.getSequence().size() == 0
            ? ""
            : Optional.ofNullable(data.getSequence().getStep().getName()).orElse("");
    return new StrategicObservation(
        StrategicObservation.CURRENT_SCHEMA_VERSION,
        seed,
        data.getSequence().getRound(),
        player.getName(),
        sequenceStep,
        phase,
        domain,
        territoryStates,
        reinforcements,
        pendingBattles,
        battle,
        phase == StrategicPhase.COMPLETE);
  }

  static List<IBattle> pendingBattles(final GameData data) {
    return data.getDelegates().stream()
        .filter(BattleDelegate.class::isInstance)
        .map(BattleDelegate.class::cast)
        .findFirst()
        .map(BattleDelegate::getBattleTracker)
        .map(
            tracker ->
                Arrays.stream(BattleType.values())
                    .flatMap(type -> tracker.getPendingBattles(type).stream())
                    .distinct()
                    .sorted(BATTLE_ORDER)
                    .toList())
        .orElse(List.of());
  }

  private static FixedReinforcementTracker reinforcementTracker(final GameData data) {
    return data.getDelegates().stream()
        .filter(FixedReinforcementDelegate.class::isInstance)
        .map(FixedReinforcementDelegate.class::cast)
        .map(FixedReinforcementDelegate::getTracker)
        .findFirst()
        .orElseGet(FixedReinforcementTracker::new);
  }

  private static List<StrategicObservation.UnitGroup> groupUnits(
      final Territory territory, final GamePlayer perspective, final GameData data) {
    final Map<UnitKey, List<Unit>> groups = new TreeMap<>();
    final int removalTurns = SupplyNetworkResolver.getRemovalTurns(data);
    for (final Unit unit : territory.getUnitCollection().getUnits()) {
      final boolean land = Matches.unitIsLand().test(unit);
      final boolean friendly =
          unit.isOwnedBy(perspective)
              || data.getRelationshipTracker().isAllied(perspective, unit.getOwner());
      final boolean supplyRelevant =
          land && friendly && SupplyNetworkResolver.isEnabled(data) && !territory.isWater();
      final Boolean supplied =
          supplyRelevant
              ? SupplyNetworkResolver.isSupplied(territory, unit.getOwner(), data)
              : null;
      final Integer isolationTurns =
          supplyRelevant ? SupplyNetworkResolver.getOutOfSupplyTurns(unit, data) : null;
      final Integer turnsUntilRemoval =
          supplyRelevant && Boolean.FALSE.equals(supplied)
              ? Math.max(0, removalTurns - isolationTurns)
              : null;
      final UnitKey key =
          new UnitKey(
              unit.getOwner().getName(),
              unit.getType().getName(),
              land,
              Matches.unitIsAir().test(unit),
              Matches.unitIsSea().test(unit),
              supplied,
              isolationTurns,
              turnsUntilRemoval);
      groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(unit);
    }
    return groups.entrySet().stream()
        .map(
            entry ->
                new StrategicObservation.UnitGroup(
                    entry.getKey().owner(),
                    entry.getKey().unitType(),
                    entry.getValue().size(),
                    entry.getKey().land(),
                    entry.getKey().air(),
                    entry.getKey().sea(),
                    entry.getValue().stream()
                        .map(Unit::getMovementLeft)
                        .min(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO)
                        .toPlainString(),
                    entry.getKey().supplied(),
                    entry.getKey().outOfSupplyTurns(),
                    entry.getKey().turnsUntilRemoval()))
        .toList();
  }

  private record UnitKey(
      String owner,
      String unitType,
      boolean land,
      boolean air,
      boolean sea,
      @Nullable Boolean supplied,
      @Nullable Integer outOfSupplyTurns,
      @Nullable Integer turnsUntilRemoval)
      implements Comparable<UnitKey> {
    private UnitKey {
      Objects.requireNonNull(owner);
      Objects.requireNonNull(unitType);
    }

    @Override
    public int compareTo(final UnitKey other) {
      return Comparator.comparing(UnitKey::owner)
          .thenComparing(UnitKey::unitType)
          .thenComparing(UnitKey::land)
          .thenComparing(UnitKey::air)
          .thenComparing(UnitKey::sea)
          .thenComparing(UnitKey::supplied, Comparator.nullsFirst(Comparator.naturalOrder()))
          .thenComparing(
              UnitKey::outOfSupplyTurns, Comparator.nullsFirst(Comparator.naturalOrder()))
          .thenComparing(
              UnitKey::turnsUntilRemoval, Comparator.nullsFirst(Comparator.naturalOrder()))
          .compare(this, other);
    }
  }
}
