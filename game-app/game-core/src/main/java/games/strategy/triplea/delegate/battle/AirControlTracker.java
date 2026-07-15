package games.strategy.triplea.delegate.battle;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.changefactory.ChangeFactory;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import javax.annotation.Nullable;

/** Stores territory air control independently from ground ownership. */
public final class AirControlTracker implements Serializable {
  private static final long serialVersionUID = 7442198467916127089L;

  public static final String AIR_CONTROL_ENABLED = "Air Control Enabled";
  public static final String AIR_CONTROL_PERSISTENT = "Air Control Persistent";
  public static final String AIR_CONTROL_GROUND_ATTACK_BONUS = "Air Control Ground Attack Bonus";

  static final String STATE_PROPERTY = "__smallFrontAirControlTracker";
  private static final AirControlTracker EMPTY = new AirControlTracker();

  private final Map<String, ControlEntry> controlByTerritory = new HashMap<>();

  public static boolean isEnabled(final GameState data) {
    return data.getProperties().get(AIR_CONTROL_ENABLED, false);
  }

  public static boolean isPersistent(final GameState data) {
    return data.getProperties().get(AIR_CONTROL_PERSISTENT, false);
  }

  public static int configuredGroundAttackBonus(final GameState data) {
    return Math.max(0, data.getProperties().get(AIR_CONTROL_GROUND_ATTACK_BONUS, 1));
  }

  public static AirControlTracker get(final GameState data) {
    final Serializable value = data.getProperties().get(STATE_PROPERTY);
    return value instanceof AirControlTracker tracker ? tracker : EMPTY;
  }

  static AirControlTracker getOrCreate(final GameState data) {
    final Serializable value = data.getProperties().get(STATE_PROPERTY);
    if (value instanceof AirControlTracker tracker) {
      return tracker;
    }
    final AirControlTracker tracker = new AirControlTracker();
    data.getProperties().set(STATE_PROPERTY, tracker);
    return tracker;
  }

  public Optional<GamePlayer> getController(final Territory territory, final GameState data) {
    if (!isEnabled(data)) {
      return Optional.empty();
    }
    return activeEntry(territory, data)
        .map(ControlEntry::playerName)
        .map(data.getPlayerList()::getPlayerId);
  }

  public int getGroundAttackBonus(
      final Territory territory, final GamePlayer player, final GameState data) {
    if (territory.isWater()) {
      return 0;
    }
    return getController(territory, data)
        .filter(
            controller ->
                controller.equals(player)
                    || data.getRelationshipTracker().isAllied(controller, player))
        .map(controller -> configuredGroundAttackBonus(data))
        .orElse(0);
  }

  public Map<String, String> snapshot(final GameState data) {
    if (!isEnabled(data)) {
      return Map.of();
    }
    final Map<String, String> result = new TreeMap<>();
    data.getMap()
        .getTerritories()
        .forEach(
            territory ->
                activeEntry(territory, data)
                    .ifPresent(entry -> result.put(territory.getName(), entry.playerName())));
    return Map.copyOf(result);
  }

  public static Change changeControl(
      final Territory territory, final @Nullable GamePlayer newController, final GameState data) {
    final AirControlTracker tracker = get(data);
    final ControlEntry oldEntry = tracker.controlByTerritory.get(territory.getName());
    if (isPersistent(data)
        && oldEntry != null
        && newController != null
        && oldEntry.playerName().equals(newController.getName())) {
      return ChangeFactory.EMPTY_CHANGE;
    }
    final ControlEntry newEntry =
        newController == null
            ? null
            : new ControlEntry(newController.getName(), data.getSequence().getRound());
    if (Objects.equals(oldEntry, newEntry)) {
      return ChangeFactory.EMPTY_CHANGE;
    }
    return new AirControlChange(territory.getName(), oldEntry, newEntry);
  }

  private Optional<ControlEntry> activeEntry(final Territory territory, final GameState data) {
    final ControlEntry entry = controlByTerritory.get(territory.getName());
    if (entry == null) {
      return Optional.empty();
    }
    if (isPersistent(data) || entry.establishedRound() == data.getSequence().getRound()) {
      return Optional.of(entry);
    }
    return Optional.empty();
  }

  void setEntry(final String territoryName, final @Nullable ControlEntry entry) {
    if (entry == null) {
      controlByTerritory.remove(territoryName);
    } else {
      controlByTerritory.put(territoryName, entry);
    }
  }

  record ControlEntry(String playerName, int establishedRound) implements Serializable {
    private static final long serialVersionUID = 6848528345141320284L;

    ControlEntry {
      Objects.requireNonNull(playerName);
    }
  }
}
