package org.triplea.util;

import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import lombok.experimental.UtilityClass;
import org.triplea.io.FileUtils;

/**
 * Provides methods that convert relative links within a game description into absolute links that
 * will work on the local system. Links that are already absolute (those beginning with 'http') will
 * not be changed.
 */
@UtilityClass
public final class LocalizeHtml {
  // Match the <img> src
  private static final Pattern PATTERN_HTML_IMG_SRC_TAG =
      Pattern.compile(
          "(<img[^>]*src\\s*=\\s*)(?:\"([^\"]+)\"|'([^']+)')([^>]*/?>)", Pattern.CASE_INSENSITIVE);

  /**
   * Replaces relative image links within a given {@code htmlText} with absolute links that point to
   * the correct location on the local file system.
   */
  public static @Nullable String localizeImgLinksInHtml(
      final String htmlText, final Path mapContentFolder) {
    if (htmlText == null) {
      return null;
    }
    final StringBuilder result = new StringBuilder();
    final Matcher matcher = PATTERN_HTML_IMG_SRC_TAG.matcher(htmlText);
    while (matcher.find()) {
      final String link = Optional.ofNullable(matcher.group(2)).orElseGet(() -> matcher.group(3));
      assert link != null && !link.isEmpty() : "RegEx is broken";

      final boolean linkIsAbsolute = link.startsWith("http");
      final String localized = linkIsAbsolute ? link : getLocalizedLink(link, mapContentFolder);

      final char quote = matcher.group(2) != null ? '"' : '\'';
      matcher.appendReplacement(
          result, matcher.group(1) + quote + localized + quote + matcher.group(4));
    }
    matcher.appendTail(result);

    return result.toString();
  }

  private static String getLocalizedLink(final String link, final Path mapContentFolder) {
    final Path imagesPath = mapContentFolder.resolve("doc").resolve("images").resolve(link);
    return FileUtils.toUrl(imagesPath).toString();
  }
}
