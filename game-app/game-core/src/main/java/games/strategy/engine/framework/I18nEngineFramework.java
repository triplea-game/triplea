package games.strategy.engine.framework;

import org.jetbrains.annotations.NonNls;

public class I18nEngineFramework extends I18nResourceBundle {
  private static I18nResourceBundle instance;

  public static I18nResourceBundle get() {
    if (instance == null) {
      instance = new I18nEngineFramework();
    }
    return instance;
  }

  @Override
  public @NonNls String getResourcePath() {
    return "i18n.games.strategy.engine.framework.ui";
  }
}
