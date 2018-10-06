package games.strategy.triplea.settings;

import java.io.File;

final class DefaultClientSetting extends ClientSetting {
  DefaultClientSetting(final String name) {
    super(name, "");
  }

  DefaultClientSetting(final String name, final String defaultValue) {
    super(name, defaultValue);
  }

  DefaultClientSetting(final String name, final int defaultValue) {
    super(name, String.valueOf(defaultValue));
  }

  DefaultClientSetting(final String name, final boolean defaultValue) {
    super(name, String.valueOf(defaultValue));
  }

  DefaultClientSetting(final String name, final File file) {
    super(name, file.getAbsolutePath());
  }
}
