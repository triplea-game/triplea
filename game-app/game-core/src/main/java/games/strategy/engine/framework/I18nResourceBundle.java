package games.strategy.engine.framework;

import java.text.Collator;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import org.jetbrains.annotations.NonNls;

/**
 * This is the main i18n (internationalization) class to retrieve language dependent outputs. The
 * subclasses of this class should implement a singleton pattern and define the path to the resource
 * property file(s).
 */
public abstract class I18nResourceBundle {

  private final ResourceBundle bundle;
  private static final HashMap<Locale, Double> mapSupportedLocales = getNewMapSupportedLocales();
  private static final Locale locale =
      Locale.lookup(getSupportedLanguageRange(), Arrays.asList(Collator.getAvailableLocales()));

  protected I18nResourceBundle() {
    bundle = ResourceBundle.getBundle(getResourcePath(), locale);
  }

  /**
   * Add new supported locales here!
   *
   * @return new map supported Locale->weight Double
   */
  private static HashMap<Locale, Double> getNewMapSupportedLocales() {
    final HashMap<Locale, Double> newMapSupportedLocales = new HashMap<>();
    newMapSupportedLocales.put(Locale.US, 1.0);
    newMapSupportedLocales.put(Locale.GERMANY, 0.5);
    return newMapSupportedLocales;
  }

  public static Set<Locale> getMapSupportedLocales() {
    return mapSupportedLocales.keySet();
  }

  public static List<Locale.LanguageRange> getSupportedLanguageRange() {
    @NonNls final StringBuilder sb = new StringBuilder();
    for (final var entry : mapSupportedLocales.entrySet()) {
      sb.append(",").append(entry.getKey().toLanguageTag()).append(";q=").append(entry.getValue());
    }
    return Locale.LanguageRange.parse(sb.substring(1));
  }

  /*
   * @param long number to be language-dependently converted to string
   * @return Language-dependent string for number
   */
  /*public static String convToText(final long number) {
    return NumberFormat.getInstance(locale).format(number);
  }*/

  /*
   * @param date to be language-dependently converted to string
   * @return Language-dependent string for date
   */
  /*public static String convToText(final Date date) {
    return convToText(date, DateFormat.SHORT);
  }*/
  /*
   * @param date to be language-dependently converted to string
   * @param style Date formatting style to be used
   * @return Language-dependent string for date
   */
  /*public static String convToText(final Date date, final int style) {
    return DateFormat.getDateInstance(style, locale).format(date);
  }*/

  /**
   * @return List of supported languages
   */
  public static List<Locale> getSupportedLanguages() {
    return Arrays.asList(Locale.ENGLISH, Locale.GERMAN);
  }

  public String getBaseBundleName() {
    return bundle.getBaseBundleName();
  }

  /**
   * @param locale which should be checked whether it is supported
   * @return True value, if it is supported, false value otherwise
   */
  public static boolean isSupported(final Locale locale) {
    final Locale[] availableLocales = Locale.getAvailableLocales();
    return Arrays.asList(availableLocales).contains(locale);
  }

  /**
   * @return Path to resource bundle property file
   */
  protected abstract String getResourcePath();

  /**
   * @param key Bundle property key
   * @param arguments formatting arguments
   * @return Language dependent text
   */
  public String getText(final String key, final Object... arguments) {
    return String.format(getText(key), arguments);
  }

  /**
   * @param key Bundle property key
   * @return Language dependent text
   */
  public String getText(final String key) {
    return bundle.getString(key);
  }
}
