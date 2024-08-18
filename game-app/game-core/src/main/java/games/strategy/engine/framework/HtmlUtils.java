package games.strategy.engine.framework;

import org.jetbrains.annotations.NonNls;

public class HtmlUtils {
  private HtmlUtils() {}

  /**
   * @param text Text to be wrapped with html-tags
   * @return Wrapped text
   */
  @NonNls
  public static String getHtml(final String text) {
    return "<html>" + text + "</html>";
  }

  public static HtmlBuilder getHtml() {
    return new HtmlBuilder();
  }
}
