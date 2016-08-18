package games.strategy.util;

import java.io.Serializable;
import java.util.Comparator;
import java.util.StringTokenizer;

/**
 * Represents a version string.
 * versions are of the form major.minor.point.micro
 * note that when doing comparisons, if the micro for two
 * versions is the same, then the two versions are considered
 * equal
 */
public class Version implements Serializable, Comparable<Object> {
  // maintain compatability with old versions
  static final long serialVersionUID = -4770210855326775333L;
  private final int major;
  private final int minor;
  private final int point;
  private final int micro;

  public Version(final int major, final int minor) {
    this(major, minor, 0);
  }

  public Version(final int major, final int minor, final int point) {
    this(major, minor, point, 0);
  }

  public Version(final int major, final int minor, final int point, final int micro) {
    this.major = major;
    this.minor = minor;
    this.point = point;
    this.micro = micro;
  }

  /**
   * version must be of the from xx.xx.xx.xx or xx.xx.xx or
   * xx.xx or xx where xx is a positive integer
   */
  public Version(final String version) {
    final StringTokenizer tokens = new StringTokenizer(version, ".", false);
    if (tokens.countTokens() < 1) {
      throw new IllegalArgumentException("invalid version string:" + version);
    }
    try {
      major = Integer.parseInt(tokens.nextToken());
      if (tokens.hasMoreTokens()) {
        minor = Integer.parseInt(tokens.nextToken());
      } else {
        minor = 0;
      }
      if (tokens.hasMoreTokens()) {
        point = Integer.parseInt(tokens.nextToken());
      } else {
        point = 0;
      }
      if (tokens.hasMoreTokens()) {
        micro = Integer.parseInt(tokens.nextToken());
      } else {
        micro = 0;
      }
    } catch (final NumberFormatException e) {
      throw new IllegalArgumentException("invalid version string:" + version);
    }
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

  public int compareTo(final Object o, final boolean ignoreMicro) {
    if (o == null) {
      return -1;
    }
    if (!(o instanceof Version)) {
      return -1;
    }
    final Version other = (Version) o;
    return compareTo(other, ignoreMicro);
  }

  public int compareTo(final Version other, final boolean ignoreMicro) {
    if (other == null) {
      return -1;
    }
    if (other.major > major) {
      return 1;
    }
    if (other.major < major) {
      return -1;
    } else if (other.minor > minor) {
      return 1;
    } else if (other.minor < minor) {
      return -1;
    } else if (other.point > point) {
      return 1;
    } else if (other.point < point) {
      return -1;
    } else if (!ignoreMicro) {
      if (other.micro > micro) {
        return 1;
      } else if (other.micro < micro) {
        return -1;
      }
    }
    // if the only difference is micro, then ignore
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

  public static Comparator<Version> getHighestToLowestComparator() {
    return (v1, v2) -> {
      if (v1 == null && v2 == null) {
        return 0;
      } else if (v1 == null) {
        return 1;
      } else if (v2 == null) {
        return -1;
      }
      if (v1.equals(v2, false)) {
        return 0;
      } else if (v1.isGreaterThan(v2, false)) {
        return -1;
      } else {
        return 1;
      }
    };
  }

  public String toStringFull(final String separator) {
    return toStringFull(separator, false);
  }

  public String toStringFull(final String separator, final boolean noMicro) {
    return major + separator + minor + separator + point + (noMicro ? "" : (separator + micro));
  }

  @Override
  public String toString() {
    return major + "." + minor + ((point != 0 || micro != 0) ? "." + point : "")
        + (micro != 0 ? "." + micro : "");
  }
}
