package games.strategy.engine.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.junit.jupiter.api.Test;

class I18nResourceBundleTest {
  private static final String resourcePath =
      "propertyTest"; // i18n.games.strategy.engine.framework.ui
  private static final int maxTextMatchLengthWithEn = 10;

  @Test
  void get() {
    final I18nResourceBundle i18nBundle = I18nTestBundle.get();
    assertNotNull(i18nBundle);
  }

  private static String errorTextNotTranslated(
      final ResourceBundle bundle, final String lang, final String key) {
    return "i18n error: Key '"
        + key
        + "' not translated into language '"
        + lang
        + "'; Check bundle '"
        + bundle.getBaseBundleName()
        + "'";
  }

  @Test
  void isSupported() {
    assertTrue(I18nResourceBundle.isSupported(Locale.ENGLISH));
  }

  @Test
  void getResourcePath() {
    final I18nResourceBundle i18nBundle = I18nTestBundle.get();
    assertEquals(resourcePath, i18nBundle.getResourcePath());
  }

  @Test
  void getText() {
    final I18nResourceBundle i18nBundle = I18nTestBundle.get();
    assertEquals("Test Text: Happy Testing", i18nBundle.getText("test.Text"));
  }

  /**
   * Add each subclass of I18nResourceBundle!
   *
   * <p>Tests for each listed subclass of I18nResourceBundle whether the properties files are having
   * a key maintained as in the onw for English (default). For string values longer than
   * maxTextMatchLengthWithEn the text must be different, as otherwise a missing translation is
   * assumed.
   */
  @Test
  void testGetText() {
    // Add subclasses of I18nResourceBundle below
    List<Class<? extends I18nResourceBundle>> subclasses = List.of(I18nEngineFramework.class);
    subclasses.forEach(
        (subclass) -> {
          // instantiate i18nBundle
          I18nResourceBundle i18nBundle = null;
          try {
            i18nBundle = subclass.getDeclaredConstructor().newInstance();
          } catch (NoSuchMethodException
              | InvocationTargetException
              | InstantiationException
              | IllegalAccessException e) {
            assertNotNull(e);
          }
          assertNotNull(i18nBundle);
          // Check each language bundle for each key against default language (English)
          final String baseBundleName = i18nBundle.getBaseBundleName();
          final ResourceBundle bundleEn = ResourceBundle.getBundle(baseBundleName, Locale.ENGLISH);
          for (String lang : I18nResourceBundle.getSupportedLanguages()) {
            if (lang.equals(Locale.ENGLISH.getLanguage())) {
              continue; // do not check en <-> en
            }
            final ResourceBundle bundle =
                ResourceBundle.getBundle(baseBundleName, Locale.forLanguageTag(lang));
            for (String key : bundleEn.keySet()) {
              try {
                final String langString = bundle.getString(key);
                final String enString = bundleEn.getString(key);
                if (enString.length() >= maxTextMatchLengthWithEn) {
                  continue; // too short to assume translation is missing from matching strings
                } else {
                  assert !enString.equals(langString) : errorTextNotTranslated(bundle, lang, key);
                }
                assert 0 != langString.length() : errorTextNotTranslated(bundle, lang, key);
              } catch (MissingResourceException e) {
                assertNotNull(e);
              }
            }
          }
        });
  }

  static class I18nTestBundle extends I18nResourceBundle {

    private static I18nResourceBundle instance;

    public static I18nResourceBundle get() {
      if (instance == null) {
        instance = new I18nTestBundle();
      }
      return instance;
    }

    @Override
    public String getResourcePath() {
      return resourcePath;
    }
  }
}
