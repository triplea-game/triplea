package games.strategy.triplea;

public enum UrlConstants {
  GITHUB_ISSUES("http://github.com/triplea-game/triplea/issues/new"),
  RULE_BOOK("http://triplea-game.github.io/files/TripleA_RuleBook.pdf"),
  LOBBY_PROPS("https://raw.githubusercontent.com/triplea-game/triplea/master/lobby_server.yaml"),
  PAYPAL_DONATE("https://sourceforge.net/donate/index.php?group_id=44492"),
  GITHUB_HELP("http://www.triplea-game.org/help/"),
  GITHUB_HOSTING("http://tripleadev.1671093.n2.nabble.com/Download-Maps-Links-Hosting-Games-General-Information-td4074312.html#a4085700"),
  SF_PORT_FORWARDING(
      "http://tripleadev.1671093.n2.nabble.com/Download-Maps-Links-Hosting-Games-General-Information-tp4074312p4085700.html"),
  TRIPLEA_WAR_CLUB("http://www.tripleawarclub.org/"),
  TRIPLEA_WAR_CLUB_LOBBY_RULES("http://www.tripleawarclub.org/modules/newbb/viewtopic.php?topic_id=100&forum=1"),
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
