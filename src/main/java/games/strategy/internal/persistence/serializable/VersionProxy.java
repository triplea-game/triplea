package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;

import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.util.Version;

/**
 * A serializable proxy for the {@link Version} class.
 *
 * <p>
 * Instances of this class are immutable.
 * </p>
 */
public final class VersionProxy implements Serializable {
  private static final long serialVersionUID = 6092507250760560736L;

  public static final ProxyFactory FACTORY = ProxyFactory.newInstance(Version.class, VersionProxy::new);

  private final int major;
  private final int minor;
  private final int point;
  private final int micro;

  /**
   * @param version The {@link Version} from which the proxy will be initialized.
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
