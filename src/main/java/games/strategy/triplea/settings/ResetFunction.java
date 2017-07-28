package games.strategy.triplea.settings;

/**
 * Function to reset a set of given settings back to their default values.
 */
interface ResetFunction {
  /**
   * @param settings The set of settings to be restored back to default.
   * @param settingsFlusher Runnable that will persist any setting changes to disk.
   */
  static void resetSettings(final Iterable<? extends UiBinding> settings, final Runnable settingsFlusher) {
    settings.forEach(UiBinding::resetToDefault);
    settingsFlusher.run();
  }
}
