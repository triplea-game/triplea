package games.strategy.util;

import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.ui.AbstractUiContext;
import lombok.extern.java.Log;

/**
 * Provides methods that convert relative links within a game description into absolute links that will work on the
 * local system.
 */
@Log
public final class LocalizeHtml {
  private static final String ASSET_IMAGE_FOLDER = "doc/images/";
  private static final String ASSET_IMAGE_NOT_FOUND = "notFound.png";
  /*
   * You would think that there would be a single standardized REGEX for pulling html links out of <img> tags and <a>
   * tags.
   * But there isn't, and the internet seems to give million different answers, none of which work perfectly.
   * So here are the best one I could find.
   * Regex's found at http://www.mkyong.com/
   */

  /* Match the <img /> tag */
  public static final String PATTERN_HTML_IMG_TAG = "(?i)<img([^>]+)/>";
  /* Match the src attribute */
  private static final String PATTERN_HTML_IMG_SRC_TAG = "src\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')";

  private LocalizeHtml() {}

  /**
   * This is only useful once we are IN a game. Before we go into the game, resource loader will either be null, or be
   * the last game's resource loader.
   */
  public static String localizeImgLinksInHtml(final String htmlText) {
    return localizeImgLinksInHtml(htmlText, AbstractUiContext.getResourceLoader(), null);
  }

  /**
   * Replaces relative image links within the HTML document {@code htmlText} with absolute links that point to the
   * correct location on the local file system.
   */
  public static String localizeImgLinksInHtml(final String htmlText, final ResourceLoader resourceLoader,
      final String mapNameDir) {
    if (htmlText == null || (resourceLoader == null && (mapNameDir == null || mapNameDir.trim().length() == 0))) {
      return htmlText;
    }
    final Pattern patternTag = Pattern.compile(PATTERN_HTML_IMG_TAG);
    final Pattern patternLink = Pattern.compile(PATTERN_HTML_IMG_SRC_TAG, Pattern.CASE_INSENSITIVE);
    final Matcher matcherTag = patternTag.matcher(htmlText);
    final Set<String> linksToProcess = new HashSet<>();
    while (matcherTag.find()) {
      // img tag
      final String href = matcherTag.group(1);
      if (href == null) {
        continue;
      }

      final Matcher matcherLink = patternLink.matcher(href);
      if (matcherLink.find()) {
        // src link
        final String link = Optional.ofNullable(matcherLink.group(1)).orElseGet(() -> matcherLink.group(2));
        if (link != null && !link.isEmpty()) {
          linksToProcess.add(link);
        }
      }
    }

    return processLinks(htmlText,
        resourceLoader == null ? ResourceLoader.getMapResourceLoader(mapNameDir) : resourceLoader,
        mapNameDir,
        linksToProcess);
  }

  private static String processLinks(
      final String htmlText,
      final ResourceLoader resourceLoader,
      final String mapNameDir,
      final Set<String> linksToProcess) {

    final Map<String, String> mapping = linksToProcess.stream()
        .collect(Collectors.toMap(Function.identity(), link -> {
      // remove full parent path
      final String imageFileName = link.substring(Math.max(link.lastIndexOf("/") + 1, 0));

      // replace when testing with: "REPLACEMENTPATH/" + imageFileName;
      final String firstOption = ASSET_IMAGE_FOLDER + imageFileName;
      URL replacementUrl = resourceLoader.getResource(firstOption);

      if (replacementUrl == null || replacementUrl.toString().isEmpty()) {
        log.severe(String.format("Could not find: %s/%s", mapNameDir, firstOption));
        final String secondFallback = ASSET_IMAGE_FOLDER + ASSET_IMAGE_NOT_FOUND;
        replacementUrl = resourceLoader.getResource(secondFallback);
        if (replacementUrl == null || replacementUrl.toString().isEmpty()) {
          log.severe(String.format("Could not find: %s", secondFallback));
          return link;
        }
      }
      return replacementUrl.toString();
    }));
    return replaceEach(mapping, htmlText);
  }

  private static String replaceEach(final Map<String, String> mapping, final String input) {
    // StringBuffer is required here, starting with Java 9 StringBuilder can be used.
    final StringBuffer result = new StringBuffer();
    final String regex = String.join("|", mapping.keySet());
    final Matcher matcher = Pattern.compile(regex).matcher(input);
    while (matcher.find()) {
      final String match = matcher.group();
      matcher.appendReplacement(result, mapping.getOrDefault(match, match));
    }
    matcher.appendTail(result);
    return result.toString();
  }
}
