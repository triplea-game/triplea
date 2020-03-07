package games.strategy.engine.framework;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.engine.ClientContext;
import games.strategy.engine.data.GameData;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.triplea.UrlConstants;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.io.IOUtils;
import org.triplea.util.Version;

/** Responsible for loading saved games, new games from xml, and saving games. */
public final class GameDataManager {
  private static final String DELEGATE_START = "<DelegateStart>";
  private static final String DELEGATE_DATA_NEXT = "<DelegateData>";
  private static final String DELEGATE_LIST_END = "<EndDelegateList>";

  private GameDataManager() {}

  /**
   * Loads game data from the specified file.
   *
   * @param file The file from which the game data will be loaded.
   * @return The loaded game data.
   * @throws IOException If an error occurs while loading the game.
   */
  public static GameData loadGame(final File file) throws IOException {
    checkNotNull(file);
    checkArgument(file.exists());

    try (InputStream fis = new FileInputStream(file);
        InputStream is = new BufferedInputStream(fis)) {
      return loadGame(is);
    }
  }

  /**
   * Loads game data from the specified stream.
   *
   * @param is The stream from which the game data will be loaded. The caller is responsible for
   *     closing this stream; it will not be closed when this method returns.
   * @return The loaded game data.
   * @throws IOException If an error occurs while loading the game.
   */
  public static GameData loadGame(final InputStream is) throws IOException {
    checkNotNull(is);

    final ObjectInputStream input = new ObjectInputStream(new GZIPInputStream(is));
    try {
      final Object version = input.readObject();

      if (version instanceof games.strategy.util.Version) {
        throw new IOException(
            String.format(
                "Incompatible engine versions. We are: %s<br>"
                    + "Trying to load incompatible save game version: %s<br>"
                    + "To download an older version of TripleA,<br>"
                    + "please visit: <a href=\"%s\">%s</a>",
                ClientContext.engineVersion(),
                ((games.strategy.util.Version) version).getExactVersion(),
                UrlConstants.OLD_DOWNLOADS_WEBSITE,
                UrlConstants.OLD_DOWNLOADS_WEBSITE));

      } else if (!(version instanceof Version)) {
        throw new IOException(
            "Incompatible engine version with save game, "
                + "unable to determine version of the save game");
      } else if (!ClientContext.engineVersion().isCompatibleWithEngineVersion((Version) version)) {
        throw new IOException(
            String.format(
                "Incompatible engine versions. We are: %s<br>"
                    + "Trying to load game created with: %s<br>"
                    + "To download the latest version of TripleA,<br>"
                    + "please visit: <a href=\"%s\">%s</a>",
                ClientContext.engineVersion(),
                version,
                UrlConstants.DOWNLOAD_WEBSITE,
                UrlConstants.DOWNLOAD_WEBSITE));
      }

      final GameData data = (GameData) input.readObject();
      data.postDeSerialize();
      loadDelegates(input, data);
      return data;
    } catch (final ClassNotFoundException cnfe) {
      throw new IOException(cnfe.getMessage());
    }
  }

  private static void loadDelegates(final ObjectInputStream input, final GameData data)
      throws ClassNotFoundException, IOException {
    for (Object endMarker = input.readObject();
        !endMarker.equals(DELEGATE_LIST_END);
        endMarker = input.readObject()) {
      final String name = (String) input.readObject();
      final String displayName = (String) input.readObject();
      final String className = (String) input.readObject();
      final IDelegate instance;
      try {
        instance =
            Class.forName(className)
                .asSubclass(IDelegate.class)
                .getDeclaredConstructor()
                .newInstance();
        instance.initialize(name, displayName);
        data.addDelegate(instance);
      } catch (final Exception e) {
        throw new IOException(e);
      }
      final String next = (String) input.readObject();
      if (next.equals(DELEGATE_DATA_NEXT)) {
        instance.loadState((Serializable) input.readObject());
      }
    }
  }

  /**
   * Saves the specified game data to the specified stream.
   *
   * @param os The stream to which the game data will be saved. Note that this stream will be closed
   *     if this method returns successfully.
   * @param gameData The game data to save.
   * @throws IOException If an error occurs while saving the game.
   */
  public static void saveGame(final OutputStream os, final GameData gameData) throws IOException {
    checkNotNull(os);
    checkNotNull(gameData);

    saveGame(os, gameData, true);
  }

  static void saveGame(final OutputStream sink, final GameData data, final boolean saveDelegateInfo)
      throws IOException {
    final File tempFile =
        File.createTempFile(
            GameDataManager.class.getSimpleName(), GameDataFileUtils.getExtension());
    try {
      // write to temporary file first in case of error
      try (OutputStream os = new FileOutputStream(tempFile);
          OutputStream bufferedOutStream = new BufferedOutputStream(os);
          OutputStream zippedOutStream = new GZIPOutputStream(bufferedOutStream);
          ObjectOutputStream outStream = new ObjectOutputStream(zippedOutStream)) {
        outStream.writeObject(ClientContext.engineVersion());
        data.acquireReadLock();
        try {
          outStream.writeObject(data);
          if (saveDelegateInfo) {
            writeDelegates(data, outStream);
          } else {
            outStream.writeObject(DELEGATE_LIST_END);
          }
        } finally {
          data.releaseReadLock();
        }
      }

      // now write to sink (ensure sink is closed per method contract)
      try (InputStream is = new FileInputStream(tempFile);
          OutputStream os = new BufferedOutputStream(sink)) {
        IOUtils.copy(is, os);
      }
    } finally {
      tempFile.delete();
    }
  }

  private static void writeDelegates(final GameData data, final ObjectOutputStream out)
      throws IOException {
    for (final IDelegate delegate : data.getDelegates()) {
      out.writeObject(DELEGATE_START);
      // write out the delegate info
      out.writeObject(delegate.getName());
      out.writeObject(delegate.getDisplayName());
      out.writeObject(delegate.getClass().getName());
      out.writeObject(DELEGATE_DATA_NEXT);
      out.writeObject(delegate.saveState());
    }
    // mark end of delegate section
    out.writeObject(DELEGATE_LIST_END);
  }
}
