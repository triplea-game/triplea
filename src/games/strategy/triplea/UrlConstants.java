package games.strategy.triplea;

public enum UrlConstants {
  GITHUB_ISSUES("http://github.com/triplea-game/triplea/issues/new"), WEBSITE_HELP(
      "http://triplea-game.github.io/docs/"), RULE_BOOK(
          "http://triplea-game.github.io/files/TripleA_RuleBook.pdf"), LOBBY_PROPS(
              "https://raw.githubusercontent.com/triplea-game/triplea/master/lobby_server.yaml"), PAYPAL_DONATE(
                  "https://sourceforge.net/donate/index.php?group_id=44492"), SOURCE_FORGE(
                      "http://triplea.sourceforge.net/"), SF_TICKET_LIST(
                          "https://sourceforge.net/p/triplea/_list/tickets"), SF_HOSTING_MAPS(
                              "http://tripleadev.1671093.n2.nabble.com/Download-Maps-Links-Hosting-Games-General-Information-tp4074312.html"), SF_PORT_FORWARDING(
                                  "http://tripleadev.1671093.n2.nabble.com/Download-Maps-Links-Hosting-Games-General-Information-tp4074312p4085700.html"), SF_FORUM(
                                      "http://triplea.sourceforge.net/mywiki/Forum"), TRIPLEA_WAR_CLUB(
                                          "http://www.tripleawarclub.org/"), TRIPLEA_WAR_CLUB_LOBBY_RULES(
                                              "http://www.tripleawarclub.org/modules/newbb/viewtopic.php?topic_id=100&forum=1");


  private final String urlString;

  UrlConstants(final String value) {
    this.urlString = value;
  }

  @Override
  public String toString() {
    return urlString;
  }

}
