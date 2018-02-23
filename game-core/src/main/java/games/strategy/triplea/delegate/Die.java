package games.strategy.triplea.delegate;

import java.io.Serializable;
import java.util.Objects;

/**
 * A single roll of a die.
 */
public class Die implements Serializable {
  private static final long serialVersionUID = 8766753280669636980L;

  public enum DieType {
    MISS, HIT, IGNORED
  }

  private final DieType m_type;
  // the value of the dice, 0 based
  private final int m_value;
  // this value is 1 based
  private final int m_rolledAt;

  public Die(final int value) {
    this(value, -1, DieType.MISS);
  }

  Die(final int value, final int rolledAt, final DieType type) {
    m_type = type;
    m_value = value;
    m_rolledAt = rolledAt;
  }

  public Die.DieType getType() {
    return m_type;
  }

  public int getValue() {
    return m_value;
  }

  int getRolledAt() {
    return m_rolledAt;
  }

  // compress to an int
  // we write a lot of dice over the network and to the saved
  // game, so we want to make this fairly efficient
  int getCompressedValue() {
    if ((m_value > 255) || (m_rolledAt > 255)) {
      throw new IllegalStateException("too big to serialize");
    }
    return (m_rolledAt << 8) + (m_value << 16) + (m_type.ordinal());
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
    return (other.m_type == this.m_type) && (other.m_value == this.m_value) && (other.m_rolledAt == this.m_rolledAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(m_value, m_rolledAt);
  }

  @Override
  public String toString() {
    if (m_rolledAt < 0) {
      return "Die roll:" + m_value + (m_type == DieType.IGNORED ? " type:" + m_type : "");
    }
    return "Die roll:" + m_value + " rolled at:" + m_rolledAt + " type:" + m_type;
  }
}
