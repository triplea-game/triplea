package games.strategy.engine.data.serializer;

import com.google.gson.JsonObject;
import javax.annotation.Nullable;

/**
 * A text-format reference to a game entity: the identity of the thing pointed at, without its
 * state. This is the text-save analogue of {@link games.strategy.engine.data.GameObjectStreamData}
 * (the network path's name-only handle), generalized to also cover {@code Unit} (by UUID) and the
 * entity kinds that handle omitted.
 *
 * <p>For every kind except {@link RefKind#UNIT} and {@link RefKind#ATTACHMENT}, {@code id} is the
 * entity's {@link games.strategy.engine.data.Named#getName() name}. For {@code UNIT} it is the
 * unit's UUID string. For {@code ATTACHMENT} it is the attachment name and {@code
 * attachmentOwnerId} carries the owning entity's reference (encoded as a nested ref).
 */
public record EntityRef(RefKind kind, String id, @Nullable EntityRef owner) {

  public EntityRef(final RefKind kind, final String id) {
    this(kind, id, null);
  }

  /** The distinct entity kinds a reference can point at. */
  public enum RefKind {
    PLAYER,
    TERRITORY,
    UNIT_TYPE,
    PRODUCTION_RULE,
    PRODUCTION_FRONTIER,
    RESOURCE,
    RELATIONSHIP_TYPE,
    TERRITORY_EFFECT,
    TECH_ADVANCE,
    ATTACHMENT,
    UNIT
  }

  JsonObject toJson() {
    final JsonObject json = new JsonObject();
    json.addProperty("kind", kind.name());
    json.addProperty("id", id);
    if (owner != null) {
      json.add("owner", owner.toJson());
    }
    return json;
  }

  static EntityRef fromJson(final JsonObject json) {
    final RefKind kind = RefKind.valueOf(json.get("kind").getAsString());
    final String id = json.get("id").getAsString();
    final EntityRef owner = json.has("owner") ? fromJson(json.getAsJsonObject("owner")) : null;
    return new EntityRef(kind, id, owner);
  }
}
