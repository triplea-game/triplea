package games.strategy.engine.framework.startup.launcher.local;

import games.strategy.engine.framework.startup.ui.PlayerTypes;

/**
 * Represents the data behind a player selected country, who is playing the country, which country,
 * etc.
 */
public interface PlayerCountrySelection {

  String getPlayerName();

  PlayerTypes.Type getPlayerType();

  boolean isPlayerEnabled();
}
