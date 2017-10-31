package games.strategy.engine;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Comparator;

import games.strategy.util.Version;

/**
 * A collection of methods for use with game engines.
 */
public final class GameEngineUtils {
  private GameEngineUtils() {}

  /**
   * Indicates the engines with the specified versions are compatible.
   *
   * @param engineVersion The first engine version.
   * @param otherEngineVersion The second engine version.
   *
   * @return {@code true} if the engines are compatible; otherwise {@code false}.
   */
  public static boolean isEngineCompatibleWithEngine(final Version engineVersion, final Version otherEngineVersion) {
    checkNotNull(engineVersion);
    checkNotNull(otherEngineVersion);

    return compareVersionsExcludingMicro(engineVersion, otherEngineVersion) == 0;
  }

  private static int compareVersionsExcludingMicro(final Version version, final Version otherVersion) {
    return Comparator.comparingInt(Version::getMajor)
        .thenComparingInt(Version::getMinor)
        .thenComparingInt(Version::getPoint)
        .compare(version, otherVersion);
  }

  /**
   * Indicates the engine with the specified version is compatible with a map having the specified minimum engine
   * version.
   *
   * @param engineVersion The engine version.
   * @param mapMinimumEngineVersion The minimum engine version required by the map.
   *
   * @return {@code true} if the engine is compatible with the map; otherwise {@code false}.
   */
  public static boolean isEngineCompatibleWithMap(final Version engineVersion, final Version mapMinimumEngineVersion) {
    checkNotNull(engineVersion);
    checkNotNull(mapMinimumEngineVersion);

    return compareVersionsExcludingMicro(engineVersion, mapMinimumEngineVersion) >= 0;
  }
}
