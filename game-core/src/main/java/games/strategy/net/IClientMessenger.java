package games.strategy.net;

import java.io.File;

import games.strategy.engine.framework.HeadlessAutoSaveType;

/**
 * A client messenger. Additional methods for selecting the game on the server.
 */
public interface IClientMessenger extends IMessenger {
  void changeServerGameTo(String gameName);

  void changeToLatestAutosave(HeadlessAutoSaveType typeOfAutosave);

  void changeToGameSave(byte[] bytes, String fileName);

  void changeToGameSave(File saveGame, String fileName);
}
