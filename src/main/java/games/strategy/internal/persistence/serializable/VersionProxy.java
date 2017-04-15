package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import games.strategy.persistence.serializable.VersionedProxySupport;
import games.strategy.util.Version;

/**
 * A serializable proxy for the {@code Version} class.
 *
 * <p>
 * Instances of this class are not thread safe.
 * </p>
 */
public final class VersionProxy implements Externalizable {
  private static final long serialVersionUID = 6092507250760560736L;

  private static final long CURRENT_VERSION = 1L;

  private final VersionedProxySupport versionedProxySupport = new VersionedProxySupport(this);

  /**
   * @serial The major (first) component of the version.
   */
  private int major;

  /**
   * @serial The minor (second) component of the version.
   */
  private int minor;

  /**
   * @serial The point (third) component of the version.
   */
  private int point;

  /**
   * @serial The micro (fourth) component of the version.
   */
  private int micro;

  /**
   * Initializes a new instance of the {@code VersionProxy} class during deserialization.
   */
  public VersionProxy() {}

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

  @Override
  public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
    versionedProxySupport.read(in);
  }

  @SuppressWarnings("unused")
  private void readExternalV1(final ObjectInput in) throws IOException {
    major = in.readInt();
    minor = in.readInt();
    point = in.readInt();
    micro = in.readInt();
  }

  @Override
  public void writeExternal(final ObjectOutput out) throws IOException {
    versionedProxySupport.write(out, CURRENT_VERSION);
  }

  @SuppressWarnings("unused")
  private void writeExternalV1(final ObjectOutput out) throws IOException {
    out.writeInt(major);
    out.writeInt(minor);
    out.writeInt(point);
    out.writeInt(micro);
  }
}
