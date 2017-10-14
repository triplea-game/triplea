package games.strategy.triplea.settings;

/**
 * Basic save/read API for a 'game setting'. Generally these will be values the user can modify over the
 * course of the game, either directly or indirectly. For example, default window size may saved here,
 * which could be set from a UI control or it could be based on the last window size used.
 */
public interface GameSetting {
  /**
   * Return true if the setting has been specified by the user or updated from default.
   */
  boolean isSet();

  /**
   * Queues a new value for a setting, may not persist right away.
   */
  void save(String newValue);

  default void save(final int newValue) {
    save(String.valueOf(newValue));
  }

  default void save(final boolean newValue) {
    save(String.valueOf(newValue));
  }

  /**
   * Returns the current persisted value of the setting.
   */
  String value();

  default int intValue() {
    return Integer.valueOf(value());
  }

  default boolean booleanValue() {
    return Boolean.valueOf(value());
  }

  void resetAndFlush();
}
