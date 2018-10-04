package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;

import games.strategy.net.GUID;
import games.strategy.triplea.attachments.UnitAttachment;
import lombok.extern.java.Log;

/**
 * Remember to always use a {@code ChangeFactory} change over an {@code IDelegateBridge} for any changes to game data,
 * or any change that should go over the network.
 */
@Log
public class Unit extends GameDataComponent implements DynamicallyModifiable {
  private static final long serialVersionUID = -7906193079642776282L;

  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private PlayerID m_owner;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private final GUID m_uid;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private int m_hits = 0;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private final UnitType m_type;

  /**
   * Creates new Unit. Owner can be null.
   */
  public Unit(final UnitType type, final PlayerID owner, final GameData data) {
    this(type, owner, data, new GUID());
  }

  public Unit(final UnitType type, final PlayerID owner, final GameData data, final GUID id) {
    super(data);

    checkNotNull(type);
    checkNotNull(id);

    m_type = type;
    m_uid = id;

    setOwner(owner);
  }

  public GUID getId() {
    return m_uid;
  }

  public UnitType getType() {
    return m_type;
  }

  public PlayerID getOwner() {
    return m_owner;
  }

  public UnitAttachment getUnitAttachment() {
    return (UnitAttachment) m_type.getAttachment("unitAttachment");
  }

  public int getHits() {
    return m_hits;
  }

  public void setHits(final int hits) {
    m_hits = hits;
  }

  public void setOwner(final @Nullable PlayerID player) {
    m_owner = Optional.ofNullable(player).orElse(PlayerID.NULL_PLAYERID);
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof Unit)) {
      return false;
    }
    final Unit other = (Unit) o;
    return this.m_uid.equals(other.m_uid);
  }

  public boolean isEquivalent(final Unit unit) {
    return m_type != null && m_type.equals(unit.getType())
        && m_owner != null && m_owner.equals(unit.getOwner())
        && m_hits == unit.getHits();
  }

  @Override
  public int hashCode() {
    if (m_type == null || m_owner == null || m_uid == null || this.getData() == null) {
      final String text =
          "Unit.toString() -> Possible java de-serialization error: "
              + (m_type == null ? "Unit of UNKNOWN TYPE" : m_type.getName()) + " owned by " + (m_owner == null
                  ? "UNKNOWN OWNER"
                  : m_owner.getName())
              + " with id: " + getId();
      UnitDeserializationErrorLazyMessage.printError(text);
      return 0;
    }
    return Objects.hashCode(m_uid);
  }

  @Override
  public String toString() {
    // TODO: none of these should happen,... except that they did a couple times.
    if (m_type == null || m_owner == null || m_uid == null || this.getData() == null) {
      final String text =
          "Unit.toString() -> Possible java de-serialization error: "
              + (m_type == null ? "Unit of UNKNOWN TYPE" : m_type.getName()) + " owned by " + (m_owner == null
                  ? "UNKNOWN OWNER"
                  : m_owner.getName())
              + " with id: " + getId();
      UnitDeserializationErrorLazyMessage.printError(text);
      return text;
    }
    return m_type.getName() + " owned by " + m_owner.getName();
  }

  public String toStringNoOwner() {
    return m_type.getName();
  }

  /**
   * Until this error gets fixed, lets not scare the crap out of our users, as the problem doesn't seem to be causing
   * any serious issues.
   * TODO: fix the root cause of this deserialization issue (probably a circular dependency somewhere)
   */
  public static class UnitDeserializationErrorLazyMessage {
    private static boolean shownError = false;

    private static void printError(final String errorMessage) {
      if (!shownError) {
        shownError = true;
        log.log(Level.SEVERE, errorMessage);
      }
    }
  }

  @Override
  public Map<String, MutableProperty<?>> getPropertyMap() {
    return ImmutableMap.<String, MutableProperty<?>>builder()
        .put("owner",
            MutableProperty.ofSimple(
                this::setOwner,
                this::getOwner))
        .put("uid", MutableProperty.ofReadOnlySimple(this::getId))
        .put("hits",
            MutableProperty.ofSimple(
                this::setHits,
                this::getHits))
        .put("type", MutableProperty.ofReadOnlySimple(this::getType))
        .build();
  }
}
