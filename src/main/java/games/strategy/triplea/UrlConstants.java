package games.strategy.triplea;

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
  TRIPLEA_WEBSITE("http://www.triplea-game.org/");


  private final String urlString;

  UrlConstants(final String value) {
    this.urlString = value;
  }

  @Override
  public String toString() {
    return urlString;
  }

}
