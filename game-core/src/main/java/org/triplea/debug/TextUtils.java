package org.triplea.debug;

import com.google.common.html.HtmlEscapers;
import lombok.experimental.UtilityClass;

@UtilityClass
final class TextUtils {

  static String textToHtml(final String text) {
    return HtmlEscapers.htmlEscaper().escape(text).replaceAll("\r?\n", "<br/>");
  }
}
