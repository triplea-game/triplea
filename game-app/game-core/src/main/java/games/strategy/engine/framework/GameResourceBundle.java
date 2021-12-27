package games.strategy.engine.framework;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;

public class GameResourceBundle {
  private static Locale currentLocale;
  private static NumberFormat numberFormat;
  private static DateFormat dateFormat;

  public static String contToString(final String text) {
    return ""; // ResourceBundle.getBundle("MessagesBundle", currentLocale);
  }

  public static String convToString(final long number) {
    return numberFormat.format(number);
  }

  public static String convToString(final Date date) {
    return dateFormat.format(date);
  }

  public static void setLocale(final String language, final String country) {
    currentLocale = new Locale(language, country);
    numberFormat = NumberFormat.getInstance(currentLocale);
    // dateFormat = DateFormat.getInstance(currentLocale);
  }
}
