package games.strategy.persistence.serializable;

import java.io.IOException;

/**
 * A persistence delegate for the {@code FakeNonSerializableClass} class.
 *
 * <p>
 * Instances of this class are immutable.
 * </p>
 */
public final class FakeNonSerializableClassPersistenceDelegate extends AbstractPersistenceDelegate {
  @Override
  public Object replaceObject(final Object obj) throws IOException {
    return (obj instanceof FakeNonSerializableClass)
        ? new FakeNonSerializableClassProxy((FakeNonSerializableClass) obj)
        : super.replaceObject(obj);
  }
}
