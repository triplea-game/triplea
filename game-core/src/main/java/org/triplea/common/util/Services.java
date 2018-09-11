package org.triplea.common.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * Provides convenience methods for loading services via {@link ServiceLoader}.
 */
public final class Services {
  private Services() {}

  /**
   * Returns any available provider of the service of the specified type.
   *
   * @throws ServiceNotAvailableException If there are no service providers of the specified type available.
   */
  public static <T> T loadAny(final Class<T> type) {
    checkNotNull(type);

    final Iterator<T> iterator = ServiceLoader.load(type).iterator();
    if (!iterator.hasNext()) {
      throw new ServiceNotAvailableException(type);
    }

    return iterator.next();
  }
}
