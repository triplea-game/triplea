package games.strategy.net;

import java.io.File;

import games.strategy.engine.framework.ui.SaveGameFileChooser;

public interface IClientMessenger extends IMessenger {
  void changeServerGameTo(final String gameName);

  void changeToLatestAutosave(final SaveGameFileChooser.AUTOSAVE_TYPE typeOfAutosave);

  void changeToGameSave(final byte[] bytes, final String fileName);

  void changeToGameSave(final File saveGame, final String fileName);
}
