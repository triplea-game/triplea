package games.strategy.engine.framework;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * This is the main i18n (internationalization) class to retrieve langauge dependent outputs. The
 * subclasses of this class should implement a singleton pattern and define the path to the resource
 * property file(s).
 */
public abstract class I18nResourceBundle {

  private ResourceBundle bundle;

  protected I18nResourceBundle() {
    bundle = ResourceBundle.getBundle(this.getResourcePath());
  }

  /**
   * @param locale which should be checked whether it is supported
   * @return True value, if it is supported, false value otherwise
   */
  public static boolean isSupported(final Locale locale) {
    final Locale[] availableLocales = Locale.getAvailableLocales();
    return Arrays.asList(availableLocales).contains(locale);
  }

  /** @return List of supported languages */
  public static List<String> getSupportedLanguages() {
    return Arrays.asList(Locale.ENGLISH.getLanguage(), Locale.GERMAN.getLanguage());
  }

  private ResourceBundle getResourceBundle() {
    if (bundle == null) {
      bundle = ResourceBundle.getBundle(getResourcePath());
    }
    return bundle;
  }

  /** @return Path to resource bundle property file */
  protected abstract String getResourcePath();

  /**
   * @param key Bundle property key
   * @param arguments formatting arguments
   * @return Language dependent text
   */
  public String getText(final String key, final Object... arguments) {
    return MessageFormat.format(getText(key), arguments);
  }

  /**
   * @param key Bundle property key
   * @return Language dependent text
   */
  public String getText(final String key) {
    return getResourceBundle().getString(key);
  }
}
