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

  public static final ProxyFactory FACTORY =
      ProxyFactory.newInstance(FakeNonSerializableClass.class, FakeNonSerializableClassProxy::new);

  private final int intField;
  private final String stringField;

  public FakeNonSerializableClassProxy(final FakeNonSerializableClass subject) {
    checkNotNull(subject);

    intField = subject.getIntField();
    stringField = subject.getStringField();
  }

  private Object readResolve() {
    return new FakeNonSerializableClass(intField, stringField);
  }
}
