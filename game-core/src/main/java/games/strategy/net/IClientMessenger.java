package games.strategy.net;

import java.io.File;

import games.strategy.engine.framework.HeadlessAutoSaveType;

/**
 * A client messenger. Additional methods for selecting the game on the server.
 */
public interface IClientMessenger extends IMessenger {
  void changeServerGameTo(final String gameName);

  void changeToLatestAutosave(final HeadlessAutoSaveType typeOfAutosave);

  void changeToGameSave(final byte[] bytes, final String fileName);

  void changeToGameSave(final File saveGame, final String fileName);
}
