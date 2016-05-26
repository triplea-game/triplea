package games.strategy.engine.framework;

import java.io.File;

import javax.swing.JFileChooser;

import games.strategy.debug.ClientLogger;
/**
 * <p>
 * Title: TripleA
 * </p>
 * <p>
 * </p>
 * <p>
 * Copyright (c) 2002
 * </p>
 * <p>
 * </p>
 */
import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.ui.SaveGameFileChooser;

public class SavedGamedDataLoader implements IGameDataLoader {
  @Override
  public GameData loadData() {
    final SaveGameFileChooser fileChooser = SaveGameFileChooser.getInstance();
    final int rVal = fileChooser.showOpenDialog(null);
    if (rVal == JFileChooser.APPROVE_OPTION) {
      final File f = fileChooser.getSelectedFile();
      try {
        return new GameDataManager().loadGame(f);
      } catch (final Exception e) {
        ClientLogger.logQuietly(e);
        System.exit(0);
        return null;
      }
    } else {
      System.exit(0);
      return null;
    }
  }
}
