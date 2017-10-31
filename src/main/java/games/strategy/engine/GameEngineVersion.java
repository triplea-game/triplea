package games.strategy.engine;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.util.Version;

/**
 * Wraps a {@link Version} and provides operations specific to game engine versions.
 */
public final class GameEngineVersion {
  private final Version version;

  private GameEngineVersion(final Version version) {
    this.version = version;
  }

  /**
   * Creates a new {@link GameEngineVersion} from the specified {@link Version}.
   */
  public static GameEngineVersion of(final Version version) {
    checkNotNull(version);

    return new GameEngineVersion(version);
  }

  /**
   * Indicates this engine version is compatible with the specified engine version.
   *
   * @param engineVersion The engine version to check for compatibility.
   *
   * @return {@code true} if this engine version is compatible with the specified engine version; otherwise
   *         {@code false}.
   */
  public boolean isCompatibleWithEngineVersion(final Version engineVersion) {
    checkNotNull(engineVersion);

    return version.withMicro(0).equals(engineVersion.withMicro(0));
  }

  /**
   * Indicates this engine version is compatible with the specified map minimum engine version.
   *
   * @param mapMinimumEngineVersion The minimum engine version required by the map.
   *
   * @return {@code true} if this engine version is compatible with the specified map minimum engine version; otherwise
   *         {@code false}.
   */
  public boolean isCompatibleWithMapMinimumEngineVersion(final Version mapMinimumEngineVersion) {
    checkNotNull(mapMinimumEngineVersion);

    return version.withMicro(0).isGreaterThanOrEqualTo(mapMinimumEngineVersion.withMicro(0));
  }
}
