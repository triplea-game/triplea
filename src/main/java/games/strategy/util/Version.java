package games.strategy.util;

import java.io.Serializable;
import java.util.StringTokenizer;

/**
 * Represents a version string.
 * versions are of the form major.minor.point.micro
 * note that when doing comparisons, if the micro for two
 * versions is the same, then the two versions are considered
 * equal
 */
public class Version implements Serializable, Comparable<Object> {
  static final long serialVersionUID = -4770210855326775333L;
  private final int m_major;
  private final int m_minor;
  private final int m_point;
  private final int m_micro;
  private final String exactVersion;

  public Version(final int major, final int minor) {
    this(major, minor, 0);
  }

  public Version(final int major, final int minor, final int point) {
    this(major, minor, point, 0);
  }

  public Version(final int major, final int minor, final int point, final int micro) {
    this.m_major = major;
    this.m_minor = minor;
    this.m_point = point;
    this.m_micro = micro;
    exactVersion = toString();
  }

  /**
   * version must be of the from xx.xx.xx.xx or xx.xx.xx or
   * xx.xx or xx where xx is a positive integer
   */
  public Version(final String version) {
    exactVersion = version;
    final StringTokenizer tokens = new StringTokenizer(version, ".", false);
    if (tokens.countTokens() < 1) {
      throw new IllegalArgumentException("invalid version string:" + version);
    }
    try {
      m_major = Integer.parseInt(tokens.nextToken());
      if (tokens.hasMoreTokens()) {
        m_minor = Integer.parseInt(tokens.nextToken());
      } else {
        m_minor = 0;
      }
      if (tokens.hasMoreTokens()) {
        m_point = Integer.parseInt(tokens.nextToken());
      } else {
        m_point = 0;
      }
      if (tokens.hasMoreTokens()) {
        m_micro = Integer.parseInt(tokens.nextToken());
      } else {
        m_micro = 0;
      }
    } catch (final NumberFormatException e) {
      throw new IllegalArgumentException("invalid version string:" + version);
    }
  }

  /**
   * Returns the exact and full version number.
   * For example, if we specify:
   * <code>
   * new Version(1.2.3.4.5).getMicro == 4; // true
   * new Version(1.2.3.4.5).toString().equals("1.2.3.4"); // true
   * new Version(1.2.3.4.5).getExactVersion.equals("1.2.3.4.5"); // true
   * </code>
   */
  public String getExactVersion() {
    // in case of deserialization, exactVersion may be null, in which case toString() it.
    return exactVersion != null ? exactVersion : toString();
  }

  public int getMajor() {
    return m_major;
  }

  public int getMinor() {
    return m_minor;
  }

  public int getPoint() {
    return m_point;
  }

  public int getMicro() {
    return m_micro;
  }

  @Override
  public boolean equals(final Object o) {
    return compareTo(o) == 0;
  }

  public boolean equals(final Object o, final boolean ignoreMicro) {
    return compareTo(o, ignoreMicro) == 0;
  }

  @Override
  public int hashCode() {
    return this.toString().hashCode();
  }

  @Override
  public int compareTo(final Object o) {
    return compareTo(o, false);
  }

  public int compareTo(final Version other) {
    return compareTo(other, false);
  }

  private int compareTo(final Object o, final boolean ignoreMicro) {
    if (o == null) {
      return -1;
    }
    if (!(o instanceof Version)) {
      return -1;
    }
    final Version other = (Version) o;
    return compareTo(other, ignoreMicro);
  }

  private int compareTo(final Version other, final boolean ignoreMicro) {
    if (other == null) {
      return -1;
    }
    if (other.m_major > m_major) {
      return 1;
    }
    if (other.m_major < m_major) {
      return -1;
    } else if (other.m_minor > m_minor) {
      return 1;
    } else if (other.m_minor < m_minor) {
      return -1;
    } else if (other.m_point > m_point) {
      return 1;
    } else if (other.m_point < m_point) {
      return -1;
    } else if (!ignoreMicro) {
      if (other.m_micro > m_micro) {
        return 1;
      } else if (other.m_micro < m_micro) {
        return -1;
      }
    }
    // if the only difference is m_micro, then ignore
    return 0;
  }

  public boolean isGreaterThan(final Version other) {
    return isGreaterThan(other, false);
  }

  public boolean isGreaterThan(final Version other, final boolean ignoreMicro) {
    return compareTo(other, ignoreMicro) < 0;
  }

  public boolean isLessThan(final Version other) {
    return compareTo(other, false) > 0;
  }

  public String toStringFull(final String separator) {
    return toStringFull(separator, false);
  }

  public String toStringFull(final String separator, final boolean noMicro) {
    return m_major + separator + m_minor + separator + m_point + (noMicro ? "" : (separator + m_micro));
  }

  @Override
  public String toString() {
    return m_major + "." + m_minor + ((m_point != 0 || m_micro != 0) ? "." + m_point : "")
        + (m_micro != 0 ? "." + m_micro : "");
  }
}
