package games.strategy.util;

import java.io.Serializable;
import java.util.StringTokenizer;

import com.google.common.base.Joiner;

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
  private final boolean microBlank;
  private final String exactVersion;

  /**
   * Constructs a Version object without the point and micro version, defaults those to 0.
   */
  public Version(final int major, final int minor) {
    this(major, minor, 0);
  }

  /**
   * Constructs a Version object without the micro version, defaults to 0.
   */
  public Version(final int major, final int minor, final int point) {
    this(major, minor, point, 0);
  }

  /**
   * Constructs a Version object with all version values set.
   */
  public Version(final int major, final int minor, final int point, final int micro) {
    this.m_major = major;
    this.m_minor = minor;
    this.m_point = point;
    this.m_micro = micro;
    this.microBlank = false;
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
        String micro = tokens.nextToken();
        if (micro.equals("dev")) {
          m_micro = Integer.MAX_VALUE;
        } else {
          m_micro = Integer.parseInt(micro);
        }
        microBlank = false;
      } else {
        m_micro = 0;
        microBlank = true;
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

  /**
   * Returns the major version number.
   */
  public int getMajor() {
    return m_major;
  }

  /**
   * Returns the minor version number.
   */
  public int getMinor() {
    return m_minor;
  }

  /**
   * Returns the point version number.
   */
  public int getPoint() {
    return m_point;
  }

  /**
   * Returns the micro version number.
   */
  public int getMicro() {
    return m_micro;
  }

  @Override
  public boolean equals(final Object o) {
    return compareTo(o) == 0;
  }

  @Override
  public int hashCode() {
    return this.toString().hashCode();
  }

  @Override
  public int compareTo(final Object o) {
    if (!(o instanceof Version)) {
      return -1;
    }
    return compareTo((Version) o, false);
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
    } else if (!ignoreMicro && !microBlank && !other.microBlank) {
      if (other.m_micro > m_micro) {
        return 1;
      } else if (other.m_micro < m_micro) {
        return -1;
      }
    }
    // if the only difference is m_micro, then ignore
    return 0;
  }

  /**
   * Returns true, if the provided Version object is greater than this object.
   */
  public boolean isGreaterThan(final Version other) {
    return isGreaterThan(other, false);
  }

  /**
   * Returns true, if the provided Version object is greater than this object.
   * If ignoreMicro is set to true, the micro version number is ignored.
   */
  public boolean isGreaterThan(final Version other, final boolean ignoreMicro) {
    return compareTo(other, ignoreMicro) < 0;
  }

  /**
   * Returns true, if the provided Version object is less than this object.
   */
  public boolean isLessThan(final Version other) {
    return compareTo(other) > 0;
  }

  /**
   * Creates a complete version string, even if some version numbers are 0.
   */
  public String toStringFull() {
    return Joiner.on('.').join(m_major, m_minor, m_point, m_micro == Integer.MAX_VALUE ? "dev" : m_micro);
  }

  @Override
  public String toString() {
    return m_micro != 0 ? toStringFull() : m_major + "." + m_minor + (m_point != 0 ? "." + m_point : "");
  }

  /**
   * Checks if The major and minor version numbers are equal.
   * Returns true if they are.
   */
  public boolean isCompatible(final Version otherVersion) {
    return otherVersion != null && otherVersion.m_major == m_major && otherVersion.m_minor == m_minor;
  }
}
