package games.strategy.triplea.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

import games.strategy.engine.ClientContext;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParser;
import games.strategy.engine.framework.GameRunner2;

public class LoadGameUtil {

  public static GameData loadGame(final String game) {
    return loadGame(game, new String[] {"maps"});
  }

  public static GameData loadTestGame(final String game) {
    return loadGame(game, new String[] {"test_data"});
  }

  /**
   * @deprecated drop the first parameter and call either loadGame(String game)
   *             or LoadTestGame(String game) instead
   */
  @Deprecated
  public static GameData loadGame(final String map, final String game) {
    return loadGame(game, new String[] {"maps", "test_data"});
  }

  private static GameData loadGame(final String game, final String[] possibleFolders) {

    try (final InputStream is = openInputStream(game, possibleFolders)) {
      if (is == null) {
        throw new IllegalStateException(game + " does not exist");
      }
      return (new GameParser()).parse(is, new AtomicReference<String>(), false);
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }

  /*
   * First try to load the game as a file on the classpath, if not found there
   * then try to load it from either the "maps" or "test_data" folders.
   */
  private static InputStream openInputStream(final String game, String[] possibleFolders) {
    InputStream is = LoadGameUtil.class.getResourceAsStream(game);
    if (is == null) {
      final File f = ClientContext.getFile(game, possibleFolders);
      if (f.exists()) {
        try {
          is = new FileInputStream(f);
        } catch (final FileNotFoundException e) {
          // ignore, we'll throw an exception anyways when the client sees we returned null
        }
      }
    }
    return is;
  }
}
