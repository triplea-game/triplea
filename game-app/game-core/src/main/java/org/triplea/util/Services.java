package org.triplea.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.Optional;
import java.util.ServiceLoader;

/** Provides convenience methods for loading services via {@link ServiceLoader}. */
public final class Services {
  private Services() {}

  /**
   * Returns any available provider of the service of the specified type.
   *
   * @throws ServiceNotAvailableException If there are no service providers of the specified type
   *     available.
   */
  public static <T> T loadAny(final Class<T> type) {
    checkNotNull(type);

    return tryLoadAny(type).orElseThrow(() -> new ServiceNotAvailableException(type));
  }

  /**
   * Returns any available provider of the service of the specified type or empty if there are no
   * service providers of the specified type available.
   */
  public static <T> Optional<T> tryLoadAny(final Class<T> type) {
    checkNotNull(type);

    final Iterator<T> iterator = ServiceLoader.load(type).iterator();
    return iterator.hasNext() ? Optional.of(iterator.next()) : Optional.empty();
  }
}
