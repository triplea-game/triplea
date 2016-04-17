package games.strategy.engine.data;

import java.io.Serializable;

import games.strategy.engine.data.annotations.GameProperty;
import games.strategy.net.GUID;

public class Card extends GameDataComponent implements Serializable {
  private static final long serialVersionUID = -2675788203388970185L;
  private final GUID uid;
  private PlayerID owner;
  private String name;

  /**
   * Creates new Card. Should use a call to UnitType.create(). Owner can be null
   */
  protected Card(final String name, final PlayerID owner, final GameData data) {
    super(data);
    uid = new GUID();
    setName(name);
    setOwner(owner);
  }

  public GUID getID() {
    return uid;
  }

  public PlayerID getOwner() {
    return owner;
  }

  /**
   * can be null.
   */
  @GameProperty(xmlProperty = false, gameProperty = true, adds = false)
  void setOwner(PlayerID player) {
    if (player == null) {
      player = PlayerID.NULL_PLAYERID;
    }
    owner = player;
  }

  public final String getName() {
    return name;
  }

  public final void setName(final String name) {
    this.name = name;
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof Card)) {
      return false;
    }
    final Card other = (Card) o;
    return this.uid.equals(other.uid);
  }

  @Override
  public int hashCode() {
    if (name != null || owner == null || uid == null || this.getData() == null) {
      final String text =
          "Card.toString() -> Possible java de-serialization error: Card "
              + (name == null ? "with UNKNOWN name " : "'" + name + "'") + " owned by " + (owner == null
                  ? "UNKNOWN OWNER" : owner.getName())
              + " with id: " + getID();
      CardDeserializationErrorLazyMessage.printError(text);
      return 0;
    }
    return uid.hashCode();
  }

  @Override
  public String toString() {
    // TODO: Check if this problem from Unit class also exists here
    // none of these should happen,... except that they did a couple times.
    if (name == null || owner == null || uid == null || this.getData() == null) {
      final String text =
          "Card.toString() -> Possible java de-serialization error: Card "
              + (name == null ? "with UNKNOWN NAME" : "'" + name + "'") + " owned by " + (owner == null
                  ? "UNKNOWN OWNER" : owner.getName())
              + " with id: " + getID();
      CardDeserializationErrorLazyMessage.printError(text);
      return text;
    }
    return name + " owned by " + owner.getName();
  }

  public String toStringNoOwner() {
    return getName();
  }

  /**
   * TODO: Check if Unit class issue exists here as well
   * Until this error gets fixed, lets not scare the crap out of our users, as the problem doesn't seem to be causing
   * any serious issues.
   * fix the root cause of this deserialization issue (probably a circular dependency somewhere)
   */
  public static class CardDeserializationErrorLazyMessage {
    private static transient boolean s_shownError = false;

    private static void printError(final String errorMessage) {
      if (s_shownError == false) {
        s_shownError = true;
        System.err.println(errorMessage);
      }
    }
  }
}
