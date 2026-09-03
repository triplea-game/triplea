package games.strategy.engine.message.wire;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import java.util.UUID;
import javax.annotation.Nonnull;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Wire-only reference to a game entity. Only an identifying key crosses the wire; the receiver
 * re-resolves the live object against its own {@link GameData}. Keeping references symbolic avoids
 * shipping entity graphs and lets each node keep ownership of its own object identities.
 *
 * <p>Units are keyed by their stable per-unit id; territories, players, and resources are keyed by
 * name, which is unique within a game.
 */
@EqualsAndHashCode
@ToString
public final class EntityRef {
  private static final String UNIT = "unit";
  private static final String TERRITORY = "territory";
  private static final String PLAYER = "player";
  private static final String RESOURCE = "resource";

  @Nonnull private final String kind;
  @Nonnull private final String key;

  private EntityRef(final String kind, final String key) {
    this.kind = kind;
    this.key = key;
  }

  public static EntityRef of(final Unit unit) {
    return new EntityRef(UNIT, unit.getId().toString());
  }

  public static EntityRef of(final Territory territory) {
    return new EntityRef(TERRITORY, territory.getName());
  }

  public static EntityRef of(final GamePlayer player) {
    return new EntityRef(PLAYER, player.getName());
  }

  public static EntityRef of(final Resource resource) {
    return new EntityRef(RESOURCE, resource.getName());
  }

  public Unit resolveUnit(final GameData gameData) {
    expectKind(UNIT);
    return gameData.getUnits().get(UUID.fromString(key));
  }

  public Territory resolveTerritory(final GameData gameData) {
    expectKind(TERRITORY);
    return gameData.getMap().getTerritoryOrThrow(key);
  }

  public GamePlayer resolvePlayer(final GameData gameData) {
    expectKind(PLAYER);
    return gameData.getPlayerList().getPlayerId(key);
  }

  public Resource resolveResource(final GameData gameData) {
    expectKind(RESOURCE);
    return gameData.getResourceList().getResourceOrThrow(key);
  }

  private void expectKind(final String expected) {
    if (!expected.equals(kind)) {
      throw new IllegalStateException(
          "Entity reference of kind '" + kind + "' cannot be resolved as '" + expected + "'");
    }
  }
}
