package games.strategy.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/**
 * Represents a version string.
 * versions are of the form major.minor.point.micro
 */
public final class Version implements Serializable, Comparable<Version> {
  private static final long serialVersionUID = -4770210855326775333L;

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

    final Matcher matcher = Pattern.compile("^(\\d+)(?:\\.(\\d+)(?:\\.(\\d+)(?:\\.((?:\\d+|dev)[^.]*))?)?)?")
        .matcher(version);

    if (matcher.find()) {
      m_major = Integer.parseInt(matcher.group(1));
      m_minor = Optional.ofNullable(matcher.group(2)).map(Integer::valueOf).orElse(0);
      m_point = Optional.ofNullable(matcher.group(3)).map(Integer::valueOf).orElse(0);
      final String microString = matcher.group(4);
      m_micro = "dev".equals(microString)
          ? Integer.MAX_VALUE
          : Optional.ofNullable(microString).map(Integer::valueOf).orElse(0);
      return;
    }
    throw new IllegalArgumentException("Invalid version String: " + version);
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
    return o instanceof Version && compareTo((Version) o) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(m_major, m_minor, m_point, m_micro);
  }

  @Override
  public int compareTo(final Version other) {
    checkNotNull(other);

    return Comparator.comparingInt(Version::getMajor)
        .thenComparingInt(Version::getMinor)
        .thenComparingInt(Version::getPoint)
        .thenComparingInt(Version::getMicro)
        .compare(this, other);
  }

  /**
   * Indicates this version is greater than the specified version.
   *
   * @param other The version to compare.
   *
   * @return {@code true} if this version is greater than the specified version; otherwise {@code false}.
   */
  public boolean isGreaterThan(final Version other) {
    checkNotNull(other);

    return compareTo(other) > 0;
  }

  /**
   * Indicates this version is greater than or equal to the specified version.
   *
   * @param other The version to compare.
   *
   * @return {@code true} if this version is greater than or equal to the specified version; otherwise {@code false}.
   */
  public boolean isGreaterThanOrEqualTo(final Version other) {
    checkNotNull(other);

    return compareTo(other) >= 0;
  }

  /**
   * Indicates this version is less than the specified version.
   *
   * @param other The version to compare.
   *
   * @return {@code true} if this version is less than the specified version; otherwise {@code false}.
   */
  public boolean isLessThan(final Version other) {
    checkNotNull(other);

    return compareTo(other) < 0;
  }

  /**
   * Returns a new version with the major, minor, and point versions from this instance and the specified micro version.
   */
  public Version withMicro(final int micro) {
    return new Version(m_major, m_minor, m_point, micro);
  }

  /**
   * Creates a complete version string with '.' as separator, even if some version numbers are 0.
   */
  public String toStringFull() {
    return String.join(
        ".",
        String.valueOf(m_major),
        String.valueOf(m_minor),
        String.valueOf(m_point),
        (m_micro == Integer.MAX_VALUE) ? "dev" : String.valueOf(m_micro));
  }

  @Override
  public String toString() {
    return m_micro != 0 ? toStringFull() : m_major + "." + m_minor + (m_point != 0 ? "." + m_point : "");
  }
}
