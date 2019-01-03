package games.strategy.triplea.help;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;

import com.google.common.html.HtmlEscapers;

import lombok.extern.java.Log;

/** A class for loading help files from the classpath. */
@Log
public final class HelpSupport {
  private HelpSupport() {}

  /**
   * Returns the HTML help text stored in the specified file. The file is assumed to be located on
   * the classpath within the {@code games.strategy.triplea.help} package.
   */
  public static String loadHelp(final String fileName) {
    checkNotNull(fileName);

    try (@Nullable InputStream is = HelpSupport.class.getResourceAsStream(fileName)) {
      if (is == null) {
        final String message = "Help file '" + fileName + "' does not exist";
        log.warning(message);
        return formatHtml(message);
      }

      return IOUtils.toString(is, StandardCharsets.UTF_8);
    } catch (final IOException e) {
      final String message = "Failed to load help file '" + fileName + "'";
      log.log(Level.WARNING, message, e);
      return formatHtml(message);
    }
  }

  private static String formatHtml(final String message) {
    return "<html><body>" + HtmlEscapers.htmlEscaper().escape(message) + "</body></html>";
  }
}
