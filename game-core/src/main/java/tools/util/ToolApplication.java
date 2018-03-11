package tools.util;

import games.strategy.triplea.settings.ClientSetting;

/**
 * Provides methods for support tools when run as standalone applications.
 */
public final class ToolApplication {
  private ToolApplication() {}

  /**
   * Performs initialization required by all map making tool applications.
   */
  public static void initialize() {
    ClientSetting.initialize();
  }
}
