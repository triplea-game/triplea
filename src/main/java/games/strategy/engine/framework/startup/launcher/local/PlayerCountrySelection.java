package games.strategy.engine.framework.startup.launcher.local;

public interface PlayerCountrySelection {

  String getPlayerName();

  // TODO: convert return value of getPlayerType() to an enum
  String getPlayerType();


  boolean isPlayerEnabled();
}
