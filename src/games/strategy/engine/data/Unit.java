package games.strategy.engine.data;

import java.io.Serializable;
import java.util.Optional;

import com.google.common.base.Preconditions;

import games.strategy.engine.data.annotations.GameProperty;
import games.strategy.net.GUID;

public class Unit extends GameDataComponent implements Serializable {
  private static final long serialVersionUID = -7906193079642776282L;
  private PlayerID m_owner;
  private final GUID m_uid;
  private int m_hits = 0;
  private final UnitType m_type;

  /**
   * Creates new Unit. Should use a call to UnitType.create(). Owner can be null
   */
  protected Unit(final UnitType type, final PlayerID owner, final GameData data) {
    super(data);
    m_type = Preconditions.checkNotNull(type);
    m_uid = new GUID();
    setOwner(owner);
  }

  public GUID getID() {
    return m_uid;
  }

  public UnitType getType() {
    return m_type;
  }

  public UnitType getUnitType() {
    return m_type;
  }

  public PlayerID getOwner() {
    return m_owner;
  }

  /**
   * DO NOT USE THIS METHOD if at all possible. It is very slow.
   * This can return null if the unit is not in any territories.
   * A unit just created, or held by a player after purchasing may not be in a territory.
   * A unit can be in exactly 2 territories, if the unit is in the process of moving from one territory to another. This
   * method will just
   * return the first territory found.
   * A unit should never be in more than 2 territories.
   */
  @Deprecated
  protected Territory getTerritoryUnitIsIn() {
    // TODO: Add Multithreading
    for (final Territory t : this.getData().getMap().getTerritories()) {
      if (t.getUnits().getUnits().contains(this)) {
        return t;
      }
    }
    return null;
  }

  public int getHits() {
    return m_hits;
  }

  /**
   * Remember to always use a ChangeFactory change over an IDelegate Bridge for any changes to game data, or any change
   * that should go over
   * the network.
   *
   * @param hits
   */
  @GameProperty(xmlProperty = false, gameProperty = true, adds = false)
  public void setHits(final int hits) {
    m_hits = hits;
  }

  /**
   * can be null.
   */
  @GameProperty(xmlProperty = false, gameProperty = true, adds = false)
  void setOwner(PlayerID player) {
    if (player == null) {
      player = PlayerID.NULL_PLAYERID;
    }
    m_owner = player;
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof Unit)) {
      return false;
    }
    final Unit other = (Unit) o;
    return this.m_uid.equals(other.m_uid);
  }

  @Override
  public int hashCode() {
    final Optional<String> errorText = checkDeserializationErrorAndGetText();
    if (errorText.isPresent()) {
      return 0;
    } else {
      return m_uid.hashCode();
    }
  }

  @Override
  public String toString() {
    final Optional<String> errorText = checkDeserializationErrorAndGetText();
    if (errorText.isPresent()) {
      return errorText.get();
    } else {
      return m_type.getName() + " owned by " + m_owner.getName();
    }
  }


  private Optional<String> checkDeserializationErrorAndGetText() {
    // TODO: none of these should happen,... except that they did a couple times.
    if (m_type == null || m_owner == null || m_uid == null || this.getData() == null) {
      final GameData gameData = this.getData();
      final StringBuilder sb = new StringBuilder();
      sb.append("Unit.toString() -> Possible java de-serialization error: "
          + "Unit of " + (m_type == null ? "UNKNOWN TYPE" : m_type.getName()));
      sb.append(" owned by " + (m_owner == null
          ? "UNKNOWN OWNER" : m_owner.getName()));
      sb.append(" in territory: " + ((gameData != null && gameData.getMap() != null)
          ? getTerritoryUnitIsIn() : "UNKNOWN TERRITORY"));
      sb.append((gameData == null ? " no GameData reference"
          : " of game " + gameData.getGameName() + " version " + gameData.getGameVersion()));
      final String errorText = sb.toString();
      UnitDeserializationErrorLazyMessage.printError(errorText);
      return Optional.of(errorText);
    }
    return Optional.empty();
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
    private static transient boolean s_shownError = false;

    private static void printError(final String errorMessage) {
      if (s_shownError == false) {
        s_shownError = true;
        System.err.println(errorMessage);
      }
    }
  }
}
