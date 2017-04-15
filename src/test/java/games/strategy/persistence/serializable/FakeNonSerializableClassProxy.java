package games.strategy.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;

/**
 * A serializable proxy for the {@code FakeNonSerializableClass} class.
 *
 * <p>
 * Instances of this class are immutable.
 * </p>
 */
public final class FakeNonSerializableClassProxy implements Serializable {
  private static final long serialVersionUID = 996961268400195642L;

  /**
   * @serial The integer field.
   */
  private final int intField;

  /**
   * @serial The string field.
   */
  private final String stringField;

  /**
   * Initializes a new instance of the {@code FakeNonSerializableClassProxy} class from the specified
   * {@code FakeNonSerializableClass} instance.
   *
   * @param subject The {@code FakeNonSerializableClass} instance; must not be {@code null}.
   */
  public FakeNonSerializableClassProxy(final FakeNonSerializableClass subject) {
    checkNotNull(subject);

    intField = subject.getIntField();
    stringField = subject.getStringField();
  }

  private Object readResolve() {
    return new FakeNonSerializableClass(intField, stringField);
  }
}
