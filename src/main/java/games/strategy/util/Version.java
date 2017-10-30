package games.strategy.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.StringTokenizer;

import javax.annotation.Nullable;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

/**
 * Represents a version string.
 * versions are of the form major.minor.point.micro
 */
public final class Version implements Serializable, Comparable<Version> {
  private static final long serialVersionUID = -4770210855326775333L;
  private static final CompareOption[] NO_COMPARE_OPTIONS = new CompareOption[0];

  private final int m_major;
  private final int m_minor;
  private final int m_point;
  private final int m_micro;
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
        final String micro = tokens.nextToken();
        m_micro = micro.equals("dev") ? Integer.MAX_VALUE : Integer.parseInt(micro);
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
  public boolean equals(final @Nullable Object o) {
    return o instanceof Version ? compareTo((Version) o) == 0 : false;
  }

  @Override
  public int hashCode() {
    return this.toString().hashCode();
  }

  @Override
  public int compareTo(final Version other) {
    checkNotNull(other);

    return compareTo(other, NO_COMPARE_OPTIONS);
  }

  /**
   * Compares this version with the specified version for order while considering the specified options.
   *
   * @param other The version to be compared.
   * @param options The comparison options.
   *
   * @return A negative integer, zero, or a positive integer if this version is less than, equal, or greater than the
   *         specified version.
   */
  public int compareTo(final Version other, final CompareOption... options) {
    checkNotNull(other);
    checkNotNull(options);

    return compareTo(other, Sets.newEnumSet(Arrays.asList(options), CompareOption.class));
  }

  private int compareTo(final Version other, final Set<CompareOption> options) {
    return Comparator.comparingInt(Version::getMajor)
        .thenComparingInt(Version::getMinor)
        .thenComparingInt(Version::getPoint)
        .thenComparingInt(options.contains(CompareOption.IGNORE_MICRO) ? it -> 0 : Version::getMicro)
        .compare(this, other);
  }

  /**
   * Creates a complete version string with '.' as separator, even if some version numbers are 0.
   */
  public String toStringFull() {
    return toStringFull('.');
  }

  /**
   * Creates a complete version string with the given separator, even if some version numbers are 0.
   */
  public String toStringFull(final char separator) {
    return Joiner.on(separator).join(m_major, m_minor, m_point, m_micro == Integer.MAX_VALUE ? "dev" : m_micro);
  }

  @Override
  public String toString() {
    return m_micro != 0 ? toStringFull() : m_major + "." + m_minor + (m_point != 0 ? "." + m_point : "");
  }

  /**
   * The possible options when comparing versions.
   */
  public enum CompareOption {
    /** Indicates the micro version will be ignored during comparison. */
    IGNORE_MICRO;
  }
}
