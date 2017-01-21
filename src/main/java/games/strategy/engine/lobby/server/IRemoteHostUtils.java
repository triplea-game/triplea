package games.strategy.engine.lobby.server;

import games.strategy.engine.message.IRemote;

public interface IRemoteHostUtils extends IRemote {
  String getConnections();

  String getChatLogHeadlessHostBot(String hashedPassword, String salt);

  String mutePlayerHeadlessHostBot(String playerNameToBeMuted, int minutes, String hashedPassword, String salt);

  String bootPlayerHeadlessHostBot(String playerNameToBeBooted, String hashedPassword, String salt);

  String banPlayerHeadlessHostBot(String playerNameToBeBanned, int hours, String hashedPassword, String salt);

  String stopGameHeadlessHostBot(String hashedPassword, String salt);

  String shutDownHeadlessHostBot(String hashedPassword, String salt);

  String getSalt();
}
