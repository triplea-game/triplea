package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;

import games.strategy.util.Version;

/**
 * A serializable proxy for the {@code Version} class.
 *
 * <p>
 * Instances of this class are immutable.
 * </p>
 */
public final class VersionProxy implements Serializable {
  private static final long serialVersionUID = 6092507250760560736L;

  /**
   * @serial The major (first) component of the version.
   */
  private final int major;

  /**
   * @serial The minor (second) component of the version.
   */
  private final int minor;

  /**
   * @serial The point (third) component of the version.
   */
  private final int point;

  /**
   * @serial The micro (fourth) component of the version.
   */
  private final int micro;

  /**
   * Initializes a new instance of the {@code VersionProxy} class from the specified {@code Version} instance.
   *
   * @param version The {@code Version} instance; must not be {@code null}.
   */
  public VersionProxy(final Version version) {
    checkNotNull(version);

    major = version.getMajor();
    minor = version.getMinor();
    point = version.getPoint();
    micro = version.getMicro();
  }

  private Object readResolve() {
    return new Version(major, minor, point, micro);
  }
}
