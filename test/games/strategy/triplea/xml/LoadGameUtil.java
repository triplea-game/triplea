package games.strategy.triplea.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParser;
import games.strategy.engine.framework.GameRunner2;

public class LoadGameUtil {
  public static GameData loadGame(final String map, final String game) {
    final InputStream is = openInputStream(game);
    if (is == null) {
      throw new IllegalStateException(game + " does not exist");
    }
    try {
      try {
        return (new GameParser()).parse(is, new AtomicReference<String>(), false);
      } finally {
        is.close();
      }
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }

  /*
   * First try to load the game as a file on the classpath, if not found there
   * then try to load it from either the "maps" or "test_data" folders.
   */
  private static InputStream openInputStream(final String game) {
    InputStream is = LoadGameUtil.class.getResourceAsStream(game);
    if (is == null) {
      File f = new File(new File(GameRunner2.getRootFolder(), "maps"), game);
      if (!f.exists()) {
        f = new File(new File(GameRunner2.getRootFolder(), "test_data"), game);
      }
      if (f.exists()) {
        try {
          is = new FileInputStream(f);
        } catch (final FileNotFoundException e) {
          // ignore, can't happen because of the if check, and we'll throw
          // an exception anyways when the client sees we returned null
        }
      }
    }
    return is;
  }
}
