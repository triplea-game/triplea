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
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.swing.JOptionPane;

import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.commons.io.output.CloseShieldOutputStream;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientContext;
import games.strategy.engine.GameEngineUtils;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameDataMemento;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.framework.headlessGameServer.HeadlessGameServer;
import games.strategy.io.IoUtils;
import games.strategy.persistence.serializable.ProxyRegistry;
import games.strategy.persistence.serializable.ProxyableObjectOutputStream;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.util.Version;
import games.strategy.util.memento.Memento;
import games.strategy.util.memento.MementoExportException;
import games.strategy.util.memento.MementoExporter;
import games.strategy.util.memento.MementoImportException;
import games.strategy.util.memento.MementoImporter;

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

    return ClientSetting.TEST_USE_PROXY_SERIALIZATION.booleanValue()
        ? loadGameInProxySerializationFormat(is)
        : loadGameInSerializationFormat(is);
  }

  @VisibleForTesting
  static GameData loadGameInProxySerializationFormat(final InputStream is) throws IOException {
    return fromMemento(loadMemento(new CloseShieldInputStream(is)));
  }

  private static Memento loadMemento(final InputStream is) throws IOException {
    try (InputStream gzipis = new GZIPInputStream(is);
        ObjectInputStream ois = new ObjectInputStream(gzipis)) {
      return (Memento) ois.readObject();
    } catch (final ClassNotFoundException e) {
      throw new IOException(e);
    }
  }

  private static GameData fromMemento(final Memento memento) throws IOException {
    try {
      final MementoImporter<GameData> mementoImporter = GameDataMemento.newImporter();
      return mementoImporter.importMemento(memento);
    } catch (final MementoImportException e) {
      throw new IOException(e);
    }
  }

  private static GameData loadGameInSerializationFormat(final InputStream inputStream) throws IOException {
    final ObjectInputStream input = new ObjectInputStream(new GZIPInputStream(inputStream));
    try {
      final Version readVersion = (Version) input.readObject();
      final boolean headless = HeadlessGameServer.headless();
      if (!GameEngineUtils.isEngineCompatibleWithEngine(ClientContext.engineVersion(), readVersion)) {
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
      } else if (!headless && readVersion.compareTo(ClientContext.engineVersion()) > 0) {
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
        ClientLogger.logQuietly(e);
        throw new IOException(e.getMessage());
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
   * @param os The stream to which the game data will be saved. The caller is responsible for closing this stream; it
   *        will not be closed when this method returns.
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
      final OutputStream os,
      final GameData gameData,
      final boolean includeDelegates)
      throws IOException {
    if (ClientSetting.TEST_USE_PROXY_SERIALIZATION.booleanValue()) {
      saveGameInProxySerializationFormat(
          os,
          gameData,
          Collections.singletonMap(GameDataMemento.ExportOptionName.EXCLUDE_DELEGATES, !includeDelegates));
    } else {
      saveGameInSerializationFormat(os, gameData, includeDelegates);
    }
  }

  private static void saveGameInProxySerializationFormat(
      final OutputStream os,
      final GameData gameData,
      final Map<GameDataMemento.ExportOptionName, Object> optionsByName)
      throws IOException {
    saveMemento(new CloseShieldOutputStream(os), toMemento(gameData, optionsByName), ProxyRegistries.GAME_DATA_MEMENTO);
  }

  @VisibleForTesting
  static void saveGameInProxySerializationFormat(
      final OutputStream os,
      final GameData gameData,
      final Map<GameDataMemento.ExportOptionName, Object> optionsByName,
      final ProxyRegistry proxyRegistry)
      throws IOException {
    saveMemento(new CloseShieldOutputStream(os), toMemento(gameData, optionsByName), proxyRegistry);
  }

  private static Memento toMemento(
      final GameData gameData,
      final Map<GameDataMemento.ExportOptionName, Object> optionsByName)
      throws IOException {
    try {
      final MementoExporter<GameData> mementoExporter = GameDataMemento.newExporter(optionsByName);
      return mementoExporter.exportMemento(gameData);
    } catch (final MementoExportException e) {
      throw new IOException(e);
    }
  }

  private static void saveMemento(
      final OutputStream os,
      final Memento memento,
      final ProxyRegistry proxyRegistry)
      throws IOException {
    try (OutputStream gzipos = new GZIPOutputStream(os);
        ObjectOutputStream oos = new ProxyableObjectOutputStream(gzipos, proxyRegistry)) {
      oos.writeObject(memento);
    }
  }

  private static void saveGameInSerializationFormat(
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
    final Iterator<IDelegate> iter = data.getDelegateList().iterator();
    while (iter.hasNext()) {
      out.writeObject(DELEGATE_START);
      final IDelegate delegate = iter.next();
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
