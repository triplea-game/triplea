package games.strategy.triplea.delegate;

import java.io.Serializable;
import java.util.Objects;

/** A single roll of a die. */
public class Die implements Serializable {
  private static final long serialVersionUID = 8766753280669636980L;

  /** The type of the die roll. */
  public enum DieType {
    MISS,
    HIT,
    IGNORED
  }

  private final DieType type;
  // the value of the dice, 0 based
  private final int value;
  // this value is 1 based
  private final int rolledAt;

  public Die(final int value) {
    this(value, -1, DieType.MISS);
  }

  Die(final int value, final int rolledAt, final DieType type) {
    this.type = type;
    this.value = value;
    this.rolledAt = rolledAt;
  }

  public Die.DieType getType() {
    return type;
  }

  public int getValue() {
    return value;
  }

  int getRolledAt() {
    return rolledAt;
  }

  // compress to an int
  // we write a lot of dice over the network and to the saved game, so we want to make this fairly
  // efficient
  int getCompressedValue() {
    if (value > 255 || rolledAt > 255) {
      throw new IllegalStateException("too big to serialize");
    }
    return (rolledAt << 8) + (value << 16) + type.ordinal();
  }

  // read from an int
  static Die getFromWriteValue(final int value) {
    final int rolledAt = (value & 0x0FF00) >> 8;
    final int roll = (value & 0x0FF0000) >> 16;
    final DieType type = DieType.values()[(value & 0x0F)];
    return new Die(roll, rolledAt, type);
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof Die)) {
      return false;
    }
    final Die other = (Die) o;
    return other.type == this.type && other.value == this.value && other.rolledAt == this.rolledAt;
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, rolledAt);
  }

  @Override
  public String toString() {
    if (rolledAt < 0) {
      return "Die roll:" + value + (type == DieType.IGNORED ? " type:" + type : "");
    }
    return "Die roll:" + value + " rolled at:" + rolledAt + " type:" + type;
  }
}
