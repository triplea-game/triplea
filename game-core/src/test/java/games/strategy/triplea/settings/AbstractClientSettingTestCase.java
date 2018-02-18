package games.strategy.triplea.settings;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.sonatype.goodies.prefs.memory.MemoryPreferences;

/**
 * Superclass for test fixtures that directly or indirectly interact with instances of {@link ClientSetting}.
 *
 * <p>
 * This fixture ensures the {@link ClientSetting} preferences are properly initialized before each test and
 * uninitialized after each test.
 * </p>
 */
public abstract class AbstractClientSettingTestCase {
  protected AbstractClientSettingTestCase() {}

  @BeforeEach
  public final void initializeClientSettingPreferences() {
    ClientSetting.setPreferences(new MemoryPreferences());
  }

  @AfterEach
  public final void uninitializeClientSettingPreferences() {
    ClientSetting.resetPreferences();
  }
}
