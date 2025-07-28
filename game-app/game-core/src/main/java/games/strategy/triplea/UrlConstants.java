package games.strategy.triplea;

import java.net.URI;
import lombok.experimental.UtilityClass;

/** Grouping of hardcoded URL constants. */
@UtilityClass
public final class UrlConstants {
  public static final String AXIS_AND_ALLIES_FORUM = "https://www.axisandallies.org/forums";
  public static final String DOWNLOAD_WEBSITE = "https://triplea-game.org/download/";
  public static final String GITHUB_ISSUES = "https://github.com/triplea-game/triplea/issues/new";
  public static final String LICENSE_NOTICE =
      "https://github.com/triplea-game/triplea/blob/master/README.md#license";
  public static final URI LIVE_SERVERS_URI =
      URI.create("https://raw.githubusercontent.com/triplea-game/triplea/master/servers.yml");
  public static final String MAP_MAKER_HELP =
      "https://github.com/triplea-game/triplea/wiki/How-to-create-custom-maps";
  public static final String PAYPAL_DONATE =
      "https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=ASJLHWGTA94MW";
  public static final String RELEASE_NOTES = "https://triplea-game.org/release_notes/";
  public static final String TRIPLEA_FORUM = "https://forums.triplea-game.org/";
  public static final String USER_GUIDE = "https://triplea-game.org/user-guide/";
  public static final String MARTI_REGISTRATION = "https://dice.marti.triplea-game.org/";
  public static final String PROD_LOBBY = "https://prod.triplea-game.org";
}
