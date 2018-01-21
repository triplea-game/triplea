package games.strategy.engine.framework.startup.launcher.local;

/**
 * Represents the data behind a player selected country, who is playing the country, which country, etc.
 */
public interface PlayerCountrySelection {

  String getPlayerName();

  // TODO: convert return value of getPlayerType() to an enum
  String getPlayerType();


  boolean isPlayerEnabled();
}
