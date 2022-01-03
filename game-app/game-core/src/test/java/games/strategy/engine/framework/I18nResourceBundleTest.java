package games.strategy.engine.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class I18nResourceBundleTest {
  private static final String resourcePath =
      "propertyTest"; // i18n.games.strategy.engine.framework.ui

  @Test
  void get() {
    final I18nResourceBundle i18nBundle = I18nTestBundle.get();
    assertNotNull(i18nBundle);
  }

  // @Test
  void isSupported() {
    assertTrue(I18nResourceBundle.isSupported(Locale.ENGLISH));
  }

  // @Test
  void getResourcePath() {
    final I18nResourceBundle i18nBundle = I18nTestBundle.get();
    assertEquals(resourcePath, i18nBundle.getResourcePath());
  }

  // @Test
  void getText() {
    final I18nResourceBundle i18nBundle = I18nTestBundle.get();
    assertEquals("Test Text: Happy Testing", i18nBundle.getText("test.Text"));
  }

  // @Test
  void testGetText() {}

  static class I18nTestBundle extends I18nResourceBundle {

    private static I18nResourceBundle instance;

    public static I18nResourceBundle get() {
      if (instance == null) {
        instance = new I18nEngineFramework();
      }
      return instance;
    }

    @Override
    public String getResourcePath() {
      return resourcePath;
    }
  }
}
