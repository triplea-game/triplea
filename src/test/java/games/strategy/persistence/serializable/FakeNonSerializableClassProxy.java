package games.strategy.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import net.jcip.annotations.Immutable;

/**
 * A serializable proxy for the {@code FakeNonSerializableClass} class.
 */
@Immutable
public final class FakeNonSerializableClassProxy implements Proxy {
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

  @Override
  public Object readResolve() {
    return new FakeNonSerializableClass(intField, stringField);
  }
}
