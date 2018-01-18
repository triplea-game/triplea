package games.strategy.engine.framework;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.swing.JOptionPane;

import games.strategy.engine.ClientContext;
import games.strategy.engine.GameEngineVersion;
import games.strategy.engine.data.GameData;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.framework.headlessGameServer.HeadlessGameServer;
import games.strategy.io.IoUtils;
import games.strategy.triplea.UrlConstants;
import games.strategy.util.Version;

/**
 * Responsible for loading saved games, new games from xml, and saving games.
 */
public final class GameDataManager {
  private static final String DELEGATE_START = "<DelegateStart>";
  private static final String DELEGATE_DATA_NEXT = "<DelegateData>";
  private static final String DELEGATE_LIST_END = "<EndDelegateList>";

  private GameDataManager() {}

  /**
   * Loads game data from the specified file.
   *
   * @param file The file from which the game data will be loaded.
   *
   * @return The loaded game data.
   *
   * @throws IOException If an error occurs while loading the game.
   */
  public static GameData loadGame(final File file) throws IOException {
    checkNotNull(file);

    try (InputStream fis = new FileInputStream(file);
        InputStream is = new BufferedInputStream(fis)) {
      return loadGame(is);
    }
  }

  /**
   * Loads game data from the specified stream.
   *
   * @param is The stream from which the game data will be loaded. The caller is responsible for closing this stream; it
   *        will not be closed when this method returns.
   *
   * @return The loaded game data.
   *
   * @throws IOException If an error occurs while loading the game.
   */
  public static GameData loadGame(final InputStream is) throws IOException {
    checkNotNull(is);

    final ObjectInputStream input = new ObjectInputStream(new GZIPInputStream(is));
    try {
      final Version readVersion = (Version) input.readObject();
      final boolean headless = HeadlessGameServer.headless();
      if (!GameEngineVersion.of(ClientContext.engineVersion()).isCompatibleWithEngineVersion(readVersion)) {
        // a hack for now, but a headless server should not try to open any savegame that is not its version
        if (headless) {
          final String message = "Incompatible game save, we are: " + ClientContext.engineVersion()
              + "  Trying to load game created with: " + readVersion;
          HeadlessGameServer.sendChat(message);
          System.out.println(message);
          return null;
        }
        final String error = "Incompatible engine versions. We are: "
            + ClientContext.engineVersion() + " . Trying to load game created with: " + readVersion
            + "\nTo download the latest version of TripleA, Please visit "
            + UrlConstants.LATEST_GAME_DOWNLOAD_WEBSITE;
        throw new IOException(error);
      } else if (!headless && readVersion.isGreaterThan(ClientContext.engineVersion())) {
        // we can still load it because our engine is compatible, however this save was made by a
        // newer engine, so prompt the user to upgrade
        final String messageString =
            "Your TripleA engine is OUT OF DATE.  This save was made by a newer version of TripleA."
                + "\nHowever, because the first 3 version numbers are the same as your current version, we can "
                + "still open the savegame."
                + "\n\nThis TripleA engine is version "
                + ClientContext.engineVersion().toStringFull()
                + " and you are trying to open a savegame made with version " + readVersion.toStringFull()
                + "\n\nTo download the latest version of TripleA, Please visit "
                + UrlConstants.LATEST_GAME_DOWNLOAD_WEBSITE
                + "\n\nIt is recommended that you upgrade to the latest version of TripleA before playing this "
                + "savegame."
                + "\n\nDo you wish to continue and open this save with your current 'old' version?";
        final int answer =
            JOptionPane.showConfirmDialog(null, messageString, "Open Newer Save Game?", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
          return null;
        }
      }
      final GameData data = (GameData) input.readObject();
      loadDelegates(input, data);
      data.postDeSerialize();
      return data;
    } catch (final ClassNotFoundException cnfe) {
      throw new IOException(cnfe.getMessage());
    }
  }

  private static void loadDelegates(final ObjectInputStream input, final GameData data)
      throws ClassNotFoundException, IOException {
    for (Object endMarker = input.readObject(); !endMarker.equals(DELEGATE_LIST_END); endMarker = input.readObject()) {
      final String name = (String) input.readObject();
      final String displayName = (String) input.readObject();
      final String className = (String) input.readObject();
      final IDelegate instance;
      try {
        instance = (IDelegate) Class.forName(className).getDeclaredConstructor().newInstance();
        instance.initialize(name, displayName);
        data.getDelegateList().addDelegate(instance);
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
   * @param os The stream to which the game data will be saved. Note that this stream will be closed if this method
   *        returns successfully.
   * @param gameData The game data to save.
   *
   * @throws IOException If an error occurs while saving the game.
   */
  public static void saveGame(final OutputStream os, final GameData gameData) throws IOException {
    checkNotNull(os);
    checkNotNull(gameData);

    saveGame(os, gameData, true);
  }

  static void saveGame(
      final OutputStream sink,
      final GameData data,
      final boolean saveDelegateInfo)
      throws IOException {
    // write internally first in case of error
    final byte[] bytes = IoUtils.writeToMemory(os -> {
      try (ObjectOutputStream outStream = new ObjectOutputStream(os)) {
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
    });

    // now write to file
    try (OutputStream zippedOut = new GZIPOutputStream(sink)) {
      zippedOut.write(bytes);
    }
  }

  private static void writeDelegates(final GameData data, final ObjectOutputStream out) throws IOException {
    for (final IDelegate delegate : data.getDelegateList()) {
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
