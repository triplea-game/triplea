package org.triplea.util;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An unchecked exception indicating a {@link java.util.ServiceLoader} had no service instance
 * available that met some set of criteria.
 */
public final class ServiceNotAvailableException extends RuntimeException {
  private static final long serialVersionUID = 973569575256667767L;

  public ServiceNotAvailableException(final Class<?> serviceType) {
    super("No service available of type '" + checkNotNull(serviceType).getName() + "'");
  }
}
