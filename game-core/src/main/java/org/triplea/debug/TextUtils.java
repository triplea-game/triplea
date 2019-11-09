package org.triplea.debug;

import com.google.common.html.HtmlEscapers;

final class TextUtils {
  private TextUtils() {}

  static String textToHtml(final String text) {
    return "<html>"
        + HtmlEscapers.htmlEscaper().escape(text).replaceAll(System.lineSeparator(), "<br/>")
        + "</html>";
  }
}
