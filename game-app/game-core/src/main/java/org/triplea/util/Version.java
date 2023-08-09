package org.triplea.util;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Optional;
import javax.annotation.Nonnull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.triplea.java.RemoveOnNextMajorRelease;
import org.triplea.java.StringUtils;

/** Represents a version string. versions are of the form major.minor.point */
@Getter
@EqualsAndHashCode
public final class Version implements Serializable, Comparable<Version> {
  private static final long serialVersionUID = -4770210855326775333L;

  private final String versionString;

  /** Indicates engine incompatible releases. */
  private final int major;

  /** Indicates engine compatible releases. */
  private final int minor;

  /**
   * Point (build number), unused, kept for serialization compatibility.
   *
   * @deprecated Do not use
   */
  @RemoveOnNextMajorRelease @Deprecated private int point;

  @RemoveOnNextMajorRelease @Deprecated private String buildNumber;

  /** version must be of the from xx.xx.xx or xx.xx or xx where xx is a positive integer */
  public Version(final String version) {
    Preconditions.checkNotNull(version);
    this.versionString = version;

    final String[] parts = StringUtils.truncateFrom(version, "+").split("\\.", -1);
    if (parts.length == 0) {
      throw new IllegalArgumentException("Invalid version String: " + version);
    }

    major = Integer.parseInt(parts[0]);
    minor = parts.length <= 1 ? 0 : Integer.parseInt(parts[1]);
  }

  @Override
  public int compareTo(@Nonnull final Version other) {
    checkNotNull(other);

    return Comparator.comparingInt(Version::getMajor)
        .thenComparingInt(Version::getMinor)
        .compare(this, other);
  }

  /**
   * Indicates this version (major.minor) is greater than the specified version. Ignores build
   * number in comparison.
   *
   * @param other The version to compare.
   * @return {@code true} if this version is greater than the specified version; otherwise {@code
   *     false}.
   */
  public boolean isGreaterThan(final Version other) {
    checkNotNull(other);

    return compareTo(other) > 0;
  }

  /** Creates a complete version string with '.' as separator. */
  @Override
  public String toString() {
    // if we saved a serialized version of 'Version.java' some time in the past, versionString
    // might be null and 'major' and 'minor' are not.
    return Optional.ofNullable(versionString)
        .orElseGet(() -> String.join(".", String.valueOf(major), String.valueOf(minor)));
  }

  /**
   * Indicates this engine version is compatible with the specified map minimum engine version.
   *
   * @param mapMinimumEngineVersion The minimum engine version required by the map.
   * @return {@code true} if this engine version is compatible with the specified map minimum engine
   *     version; otherwise {@code false}.
   */
  public boolean isCompatibleWithMapMinimumEngineVersion(final Version mapMinimumEngineVersion) {
    checkNotNull(mapMinimumEngineVersion);

    return major > mapMinimumEngineVersion.major
        || (major == mapMinimumEngineVersion.major && minor >= mapMinimumEngineVersion.minor);
  }
}
