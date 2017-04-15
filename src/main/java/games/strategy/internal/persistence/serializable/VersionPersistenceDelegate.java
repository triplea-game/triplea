package games.strategy.internal.persistence.serializable;

import java.io.IOException;

import games.strategy.persistence.serializable.AbstractPersistenceDelegate;
import games.strategy.util.Version;

/**
 * A persistence delegate for the {@code Version} class.
 *
 * <p>
 * Instances of this class are immutable.
 * </p>
 */
public final class VersionPersistenceDelegate extends AbstractPersistenceDelegate {
  @Override
  public Object replaceObject(final Object obj) throws IOException {
    return (obj instanceof Version)
        ? new VersionProxy((Version) obj)
        : super.replaceObject(obj);
  }
}
