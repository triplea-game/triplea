package games.strategy.triplea;

/**
 * Grouping of hardcoded URL constants.
 *
 * <p>
 * Typical usage:
 * </p>
 * <code><pre>
 *   String someUrl = UrlConstants.toString();
 * </pre></code>
 */
public enum UrlConstants {
  GITHUB_ISSUES("https://github.com/triplea-game/triplea/issues/new"),

  RULE_BOOK("http://www.triplea-game.org/files/TripleA_RuleBook.pdf"),

  LOBBY_PROPS("https://raw.githubusercontent.com/triplea-game/triplea/master/lobby_server.yaml"),

  PAYPAL_DONATE("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=GKZL7598EDZLN"),

  GITHUB_HELP("http://www.triplea-game.org/help/"),

  HOSTING_GUIDE("https://forums.triplea-game.org/topic/100"),

  TRIPLEA_FORUM("https://forums.triplea-game.org/"),

  AXIS_AND_ALLIES_FORUM("https://www.axisandallies.org/forums/index.php"),

  TRIPLEA_LOBBY_RULES("https://forums.triplea-game.org/topic/4"),

  LATEST_GAME_DOWNLOAD_WEBSITE("http://www.triplea-game.org/download/"),

  TRIPLEA_WEBSITE("http://www.triplea-game.org/"),

  DOWNLOAD_WEBSITE("http://www.triplea-game.org/download/"),

  RELEASE_NOTES("http://www.triplea-game.org/release_notes/"),

  MAP_DOWNLOAD_LIST("https://raw.githubusercontent.com/triplea-game/triplea/master/triplea_maps.yaml"),

  MAP_MAKER_HELP(
      "https://github.com/triplea-game/triplea/blob/master/docs/map_making/map_and_map_skin_making_overview.md");

  private final String urlString;

  UrlConstants(final String value) {
    this.urlString = value;
  }

  @Override
  public String toString() {
    return urlString;
  }
}
