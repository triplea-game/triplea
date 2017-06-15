package games.strategy.util;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.ui.AbstractUIContext;

public class LocalizeHTML {
  public static final String ASSET_IMAGE_FOLDER = "doc/images/";
  public static final String ASSET_IMAGE_NOT_FOUND = "notFound.png";
  /*
   * You would think that there would be a single standardized REGEX for pulling html links out of <img> tags and <a>
   * tags.
   * But there isn't, and the internet seems to give million different answers, none of which work perfectly.
   * So here are the best one I could find.
   * Regex's found at http://www.mkyong.com/
   */

  /*
   * Match an <a></a> tag.
   * ( # start of group #1
   * ?i # all checking are case insensive
   * ) # end of group #1
   * <a # start with "<a"
   * ( # start of group #2
   * [^>]+ # anything except (">"), at least one character
   * ) # end of group #2
   * > # follow by ">"
   * (.*?) # match anything
   * </a> # end with "</a>
   */
  public static final String PATTERN_HTML_A_TAG = "(?i)<a([^>]+)>(.*?)</a>";
  /*
   * Match the href attribute.
   * \s* # can start with whitespace
   * (?i) # all checking are case insensive
   * href # follow by "href" word
   * \s*=\s* # allows spaces on either side of the equal sign,
   * ( # start of group #1
   * "([^"]*") # allow string with double quotes enclosed - "string"
   * | # ..or
   * '[^']*' # allow string with single quotes enclosed - 'string'
   * | # ..or
   * ([^'">]+) # can't contains one single quotes, double quotes ">"
   * ) # end of group #1
   */
  public static final String PATTERN_HTML_A_HREF_TAG = "\\s*(?i)href\\s*=\\s*(\"([^\"]*\")|'[^']*'|([^'\">\\s]+))";
  /* Match the <img /> tag */
  public static final String PATTERN_HTML_IMG_TAG = "(?i)<img([^>]+)/>";
  /* Match the src attribute */
  public static final String PATTERN_HTML_IMG_SRC_TAG = "\\s*(?i)src\\s*=\\s*(\"([^\"]*\")|'[^']*'|([^'\">\\s]+))";

  /**
   * This is only useful once we are IN a game. Before we go into the game, resource loader will either be null, or be
   * the last game's
   * resource loader.
   */
  public static String localizeImgLinksInHTML(final String htmlText) {
    return localizeImgLinksInHTML(htmlText, AbstractUIContext.getResourceLoader(), null);
  }

  public static String localizeImgLinksInHTML(final String htmlText, final ResourceLoader resourceLoader,
      final String mapNameDir) {
    if (htmlText == null || (resourceLoader == null && (mapNameDir == null || mapNameDir.trim().length() == 0))) {
      return htmlText;
    }
    ResourceLoader ourResourceLoader = resourceLoader;
    String rVal = htmlText;
    final Pattern patternTag = Pattern.compile(PATTERN_HTML_IMG_TAG);
    final Pattern patternLink = Pattern.compile(PATTERN_HTML_IMG_SRC_TAG);
    final Matcher matcherTag = patternTag.matcher(htmlText);
    Matcher matcherLink;
    while (matcherTag.find()) {
      // img tag
      final String href = matcherTag.group(1);
      if (href == null) {
        continue;
      }
      matcherLink = patternLink.matcher(href);
      while (matcherLink.find()) {
        // src link
        final String fullLink = matcherLink.group(1);
        if (fullLink != null && fullLink.length() > 2) {
          if (ourResourceLoader == null) {
            ourResourceLoader = ResourceLoader.getMapResourceLoader(mapNameDir);
          }
          // remove quotes
          final String link = fullLink.substring(1, fullLink.length() - 1);

          // remove full parent path
          final String imageFileName = link.substring(Math.max((link.lastIndexOf("/") + 1), 0));
          // replace when testing with: "REPLACEMENTPATH/" + imageFileName;
          URL replacementUrl = ourResourceLoader.getResource(ASSET_IMAGE_FOLDER + imageFileName);

          if (replacementUrl == null || replacementUrl.toString().length() == 0) {
            System.out.println("Could not find: " + mapNameDir + "/" + ASSET_IMAGE_FOLDER + imageFileName);
            replacementUrl = ourResourceLoader.getResource(ASSET_IMAGE_FOLDER + ASSET_IMAGE_NOT_FOUND);
          }
          if (replacementUrl == null || replacementUrl.toString().length() == 0) {
            System.err.println("Could not find: " + ASSET_IMAGE_FOLDER + ASSET_IMAGE_NOT_FOUND);
            continue;
          }

          rVal = rVal.replaceAll(link, replacementUrl.toString());
        }
      }
    }

    return rVal;
  }
}
