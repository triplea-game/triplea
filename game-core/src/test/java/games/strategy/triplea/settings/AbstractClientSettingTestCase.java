package games.strategy.triplea.settings;

import java.util.prefs.Preferences;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.sonatype.goodies.prefs.memory.MemoryPreferences;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * Superclass for test fixtures that directly or indirectly interact with instances of {@link ClientSetting}.
 *
 * <p>
 * This fixture ensures the {@link ClientSetting} preferences are properly initialized before each test and
 * uninitialized after each test.
 * </p>
 */
public abstract class AbstractClientSettingTestCase {
  @Getter(AccessLevel.PROTECTED)
  private final Preferences preferences = new MemoryPreferences();

  protected AbstractClientSettingTestCase() {}

  @BeforeEach
  public final void initializeClientSettingPreferences() {
    ClientSetting.setPreferences(preferences);
  }

  @AfterEach
  @SuppressWarnings("static-method")
  public final void uninitializeClientSettingPreferences() {
    ClientSetting.resetPreferences();
  }
}
