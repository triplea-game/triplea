package games.strategy.triplea.settings;

import javax.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.sonatype.goodies.prefs.memory.MemoryPreferences;
import org.triplea.test.common.ExtendedUnitTest;

@ExtendedUnitTest
@Execution(ExecutionMode.SAME_THREAD)
final class ClientSettingAsGameSettingTest extends AbstractGameSettingTestCase {
  @Override
  protected GameSetting<Integer> newGameSetting(
      final @Nullable Integer value, final @Nullable Integer defaultValue) {
    final TestClientSetting clientSetting = new TestClientSetting(defaultValue);
    if (value != null) {
      clientSetting.setValue(value);
    }
    return clientSetting;
  }

  @BeforeEach
  @SuppressWarnings("static-method")
  void initializeClientSettingPreferences() {
    ClientSetting.setPreferences(new MemoryPreferences());
  }

  @AfterEach
  @SuppressWarnings("static-method")
  void uninitializeClientSettingPreferences() {
    ClientSetting.resetPreferences();
  }

  private static final class TestClientSetting extends ClientSetting<Integer> {
    TestClientSetting(final @Nullable Integer defaultValue) {
      super(Integer.class, "name", defaultValue);
    }

    @Override
    protected String encodeValue(final Integer value) {
      return value.toString();
    }

    @Override
    protected Integer decodeValue(final String encodedValue) {
      return Integer.valueOf(encodedValue);
    }
  }
}
