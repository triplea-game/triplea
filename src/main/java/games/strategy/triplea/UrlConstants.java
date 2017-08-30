package games.strategy.triplea;

/**
 * Grouping of hardcoded URL constants.

 * <p>
 * Typical usage:
 * </p>
 * <code><pre>
 *   String someUrl = UrlConstants.toString();
 * </pre></code>
 */
public enum UrlConstants {
  GITHUB_ISSUES("http://github.com/triplea-game/triplea/issues/new"),
  RULE_BOOK("http://triplea-game.github.io/files/TripleA_RuleBook.pdf"),
  LOBBY_PROPS("https://raw.githubusercontent.com/triplea-game/triplea/master/lobby_server.yaml"),
  PAYPAL_DONATE("https://sourceforge.net/donate/index.php?group_id=44492"),
  GITHUB_HELP("http://www.triplea-game.org/help/"),
  HOSTING_GUIDE("https://forums.triplea-game.org/topic/100"),
  TRIPLEA_FORUM("http://forums.triplea-game.org/"),
  TRIPLEA_LOBBY_RULES("https://forums.triplea-game.org/topic/4"),
  LATEST_GAME_DOWNLOAD_WEBSITE("http://www.triplea-game.org/download"),
  TRIPLEA_WEBSITE("http://www.triplea-game.org/"),
  MAP_DOWNLOAD_LIST("https://raw.githubusercontent.com/triplea-game/triplea/master/triplea_maps.yaml?raw=true"),
  LOBBY_PROPS_FILE_URL("https://raw.githubusercontent.com/triplea-game/triplea/master/lobby_server.yaml?raw=true"),
  MAP_MAKER_HELP("https://github.com/triplea-game/triplea/blob/master/docs/map_making/map_and_map_skin_making_overview.md");

  private final String urlString;

  UrlConstants(final String value) {
    this.urlString = value;
  }

  @Override
  public String toString() {
    return urlString;
  }
}
