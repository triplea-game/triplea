package games.strategy.persistence.serializable;

import java.util.Objects;

/**
 * A fake non-serializable class used for testing the object serialization streams.
 *
 * <p>
 * This class is non-serializable because a) it does not implement {@code Serializable}, b) it is immutable, and c) it
 * does not define a default constructor.
 * </p>
 *
 * <p>
 * Instances of this class are immutable.
 * </p>
 */
public final class FakeNonSerializableClass {
  private final int intField;

  private final String stringField;

  /**
   * Initializes a new instance of the {@code FakeNonSerializableClass} class.
   *
   * @param intField The integer field.
   * @param stringField The string field; may be {@code null}.
   */
  public FakeNonSerializableClass(final int intField, final String stringField) {
    this.intField = intField;
    this.stringField = stringField;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    }

    if (!(obj instanceof FakeNonSerializableClass)) {
      return false;
    }

    final FakeNonSerializableClass other = (FakeNonSerializableClass) obj;
    return (intField == other.intField) && Objects.equals(stringField, other.stringField);
  }

  public int getIntField() {
    return intField;
  }

  public String getStringField() {
    return stringField;
  }

  @Override
  public int hashCode() {
    return Objects.hash(intField, stringField);
  }

  @Override
  public String toString() {
    return String.format("FakeNonSerializableClass[intField=%d, stringField=%s]", intField, stringField);
  }
}
