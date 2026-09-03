package games.strategy.engine.data.serializer;

import com.google.gson.JsonObject;
import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.Named;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.serializer.EntityRef.RefKind;
import games.strategy.triplea.delegate.TechAdvance;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * Resolves game entities to and from {@link EntityRef}s against a specific {@link GameData}. This
 * is the text-save generalization of {@link games.strategy.engine.data.GameObjectStreamData}: the
 * write side ({@link #refFor}) mirrors that class's constructor if/else chain, and the read side
 * ({@link #resolve}) mirrors its {@code getReference} switch — both extended to cover {@code Unit}
 * (by UUID) and the entity kinds the network handle omitted.
 *
 * <p>The existing {@code GameObjectStreamData}/{@code GameObjectOutputStream} network classes are
 * intentionally left untouched; this is a parallel, text-oriented reimplementation of the same
 * idea.
 */
public final class GameRefResolver {

  private final GameData data;

  public GameRefResolver(final GameData data) {
    this.data = data;
  }

  /** Writes a reference to {@code entity} as JSON. */
  public JsonObject writeRef(final Object entity) {
    return refFor(entity).toJson();
  }

  /** Reads a reference written by {@link #writeRef} and resolves it against this game data. */
  public Object readRef(final JsonObject json) {
    return resolve(EntityRef.fromJson(json));
  }

  /** The write-side generalization of the {@code GameObjectStreamData} constructor. */
  public EntityRef refFor(final Object entity) {
    return switch (entity) {
      case Unit unit -> new EntityRef(RefKind.UNIT, unit.getId().toString());
      case GamePlayer player -> new EntityRef(RefKind.PLAYER, player.getName());
      case Territory territory -> new EntityRef(RefKind.TERRITORY, territory.getName());
      case UnitType unitType -> new EntityRef(RefKind.UNIT_TYPE, unitType.getName());
      case ProductionRule rule -> new EntityRef(RefKind.PRODUCTION_RULE, rule.getName());
      case ProductionFrontier frontier ->
          new EntityRef(RefKind.PRODUCTION_FRONTIER, frontier.getName());
      case Resource resource -> new EntityRef(RefKind.RESOURCE, resource.getName());
      case RelationshipType relationship ->
          new EntityRef(RefKind.RELATIONSHIP_TYPE, relationship.getName());
      case TerritoryEffect effect -> new EntityRef(RefKind.TERRITORY_EFFECT, effect.getName());
      case TechAdvance tech -> new EntityRef(RefKind.TECH_ADVANCE, tech.getName());
      case IAttachment attachment ->
          new EntityRef(
              RefKind.ATTACHMENT, attachment.getName(), refFor(attachment.getAttachedTo()));
      default ->
          throw new IllegalArgumentException(
              "No EntityRef for type: " + entity.getClass().getName());
    };
  }

  /** The read-side generalization of {@code GameObjectStreamData.getReference}. */
  public @Nullable Object resolve(final EntityRef ref) {
    return switch (ref.kind()) {
      case UNIT -> data.getUnits().get(UUID.fromString(ref.id()));
      case PLAYER -> data.getPlayerList().getPlayerId(ref.id());
      case TERRITORY -> data.getMap().getTerritoryOrNull(ref.id());
      case UNIT_TYPE -> data.getUnitTypeList().getUnitTypeOrThrow(ref.id());
      case PRODUCTION_RULE -> data.getProductionRuleList().getProductionRule(ref.id());
      case PRODUCTION_FRONTIER -> data.getProductionFrontierList().getProductionFrontier(ref.id());
      case RESOURCE -> data.getResourceList().getResourceOrThrow(ref.id());
      case RELATIONSHIP_TYPE -> data.getRelationshipTypeList().getRelationshipType(ref.id());
      case TERRITORY_EFFECT -> data.getTerritoryEffectList().get(ref.id());
      case TECH_ADVANCE -> resolveTech(ref.id());
      case ATTACHMENT -> resolveAttachment(ref);
    };
  }

  private @Nullable Object resolveTech(final String name) {
    for (final TechAdvance tech : data.getTechnologyFrontier().getTechs()) {
      if (tech.getName().equals(name)) {
        return tech;
      }
    }
    return null;
  }

  private @Nullable Object resolveAttachment(final EntityRef ref) {
    final Object owner = resolve(ref.owner());
    if (owner instanceof Attachable attachable) {
      return attachable.getAttachment(ref.id());
    }
    return null;
  }

  /** Convenience for the common single-name case. */
  public String nameOf(final Named named) {
    return named.getName();
  }
}
