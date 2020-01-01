package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import games.strategy.triplea.attachments.UnitAttachment;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

/**
 * Remember to always use a {@code ChangeFactory} change over an {@code IDelegateBridge} for any
 * changes to game data, or any change that should go over the network.
 */
@Log
@Getter
@EqualsAndHashCode(of = "id", callSuper = false)
public class Unit extends GameDataComponent implements DynamicallyModifiable {
  private static final long serialVersionUID = -7906193079642776282L;

  private GamePlayer owner;
  private final UUID id;
  @Setter private int hits = 0;
  private final UnitType type;

  /** Creates new Unit. Owner can be null. */
  public Unit(final UnitType type, final GamePlayer owner, final GameData data) {
    this(type, owner, data, UUID.randomUUID());
  }

  public Unit(final UnitType type, final GamePlayer owner, final GameData data, final UUID id) {
    super(data);

    checkNotNull(type);
    checkNotNull(id);

    this.type = type;
    this.id = id;

    setOwner(owner);
  }

  public UnitAttachment getUnitAttachment() {
    return (UnitAttachment) type.getAttachment("unitAttachment");
  }

  public void setOwner(final @Nullable GamePlayer player) {
    owner = Optional.ofNullable(player).orElse(GamePlayer.NULL_PLAYERID);
  }

  public boolean isEquivalent(final Unit unit) {
    return type != null
        && type.equals(unit.getType())
        && owner != null
        && owner.equals(unit.getOwner())
        && hits == unit.getHits();
  }

  @Override
  public String toString() {
    // TODO: none of these should happen,... except that they did a couple times.
    if (type == null || owner == null || id == null || this.getData() == null) {
      final String text =
          "Unit.toString() -> Possible java de-serialization error: "
              + (type == null ? "Unit of UNKNOWN TYPE" : type.getName())
              + " owned by "
              + (owner == null ? "UNKNOWN OWNER" : owner.getName())
              + " with id: "
              + getId();
      UnitDeserializationErrorLazyMessage.printError(text);
      return text;
    }
    return type.getName() + " owned by " + owner.getName();
  }

  public String toStringNoOwner() {
    return type.getName();
  }

  /**
   * Until this error gets fixed, lets not scare the crap out of our users, as the problem doesn't
   * seem to be causing any serious issues. TODO: fix the root cause of this deserialization issue
   * (probably a circular dependency somewhere)
   */
  public static final class UnitDeserializationErrorLazyMessage {
    private static boolean shownError = false;

    private UnitDeserializationErrorLazyMessage() {}

    private static void printError(final String errorMessage) {
      if (!shownError) {
        shownError = true;
        log.severe(errorMessage);
      }
    }
  }

  @Override
  public Map<String, MutableProperty<?>> getPropertyMap() {
    return ImmutableMap.<String, MutableProperty<?>>builder()
        .put("owner", MutableProperty.ofSimple(this::setOwner, this::getOwner))
        .put("uid", MutableProperty.ofReadOnlySimple(this::getId))
        .put("hits", MutableProperty.ofSimple(this::setHits, this::getHits))
        .put("type", MutableProperty.ofReadOnlySimple(this::getType))
        .build();
  }
}
