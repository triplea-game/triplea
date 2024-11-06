package games.strategy.triplea.delegate;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** A single roll of a die. */
@Builder
@AllArgsConstructor
@EqualsAndHashCode
@Getter
public class Die implements Serializable {
  private static final long serialVersionUID = 8766753280669636980L;

  /** The type of the die roll. */
  public enum DieType {
    MISS,
    HIT,
    IGNORED
  }

  /** The value of the dice, 0 based. */
  private final int value;

  /** This value is 1 based. */
  private final int rolledAt;

  private final DieType type;

  public Die(final int value) {
    this(value, -1, DieType.MISS);
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
  public String toString() {
    if (rolledAt < 0) {
      return "Die roll: " + value + (type == DieType.IGNORED ? " type: " + type : "");
    }
    return "Die roll: " + value + " rolled at: " + rolledAt + " type: " + type;
  }
}
