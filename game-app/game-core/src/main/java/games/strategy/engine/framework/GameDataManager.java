package games.strategy.engine.framework;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.engine.data.GameData;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.settings.ClientSetting;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.triplea.config.product.ProductVersionReader;
import org.triplea.util.Version;

/** Responsible for loading saved games, new games from xml, and saving games. */
@Slf4j
public final class GameDataManager {
  private static final String DELEGATE_START = "<DelegateStart>";
  private static final String DELEGATE_DATA_NEXT = "<DelegateData>";
  private static final String DELEGATE_LIST_END = "<EndDelegateList>";

  private GameDataManager() {}

  /**
   * Loads game data from the specified file.
   *
   * @param file The file from which the game data will be loaded.
   * @return The loaded game data or empty if there were problems.
   */
  public static Optional<GameData> loadGame(final Path file) {
    checkNotNull(file);
    checkArgument(Files.exists(file));

    try (InputStream fis = Files.newInputStream(file);
        InputStream is = new BufferedInputStream(fis)) {
      return loadGame(is);
    } catch (final IOException e) {
      log.error("Input stream error", e);
      return Optional.empty();
    }
  }

  public static Optional<GameData> loadGame(final InputStream is) {
    return loadGame(ProductVersionReader.getCurrentVersion(), is);
  }

  /**
   * Loads game data from the specified stream.
   *
   * @param ourVersion The version of the currently running game engine. Used to determine if the
   *     game read from input stream is compatible.
   * @param is The stream from which the game data will be loaded. The caller is responsible for
   *     closing this stream; it will not be closed when this method returns.
   * @return The loaded game data, or an empty optional if an error occurs.
   */
  public static Optional<GameData> loadGame(final Version ourVersion, final InputStream is) {
    try (GZIPInputStream input = new GZIPInputStream(is)) {
      return loadGameUncompressed(ourVersion, input);
    } catch (final Throwable e) {
      log.error("Error loading game data", e);
      return Optional.empty();
    }
  }

  public static Optional<GameData> loadGameUncompressed(
      final Version ourVersion, final InputStream is) {
    try (ObjectInputStream input = new ObjectInputStream(is)) {
      final Object version = input.readObject();
      final GameData data = (GameData) input.readObject();
      data.postDeSerialize();
      loadDelegates(input, data);
      return Optional.of(data);
    } catch (final Throwable e) {
      log.warn("Error loading game save, possible version incompatibility: " + e.getMessage(), e);
      return Optional.empty();
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
   * @param out The stream to which the game data will be saved. Note that this stream will be
   *     closed if this method returns successfully.
   * @param gameData The game data to save.
   * @throws IOException If an error occurs while saving the game.
   */
  public static void saveGame(
      final OutputStream out, final GameData gameData, final Version engineVersion)
      throws IOException {
    checkNotNull(out);
    checkNotNull(gameData);

    final Path tempFile =
        Files.createTempFile(
            GameDataManager.class.getSimpleName(), GameDataFileUtils.getExtension());
    try {
      // write to temporary file first in case of error
      try (OutputStream os = Files.newOutputStream(tempFile);
          OutputStream bufferedOutStream = new BufferedOutputStream(os);
          OutputStream zippedOutStream = new GZIPOutputStream(bufferedOutStream)) {
        saveGameUncompressed(zippedOutStream, gameData, Options.withEverything(), engineVersion);
      }

      // now write to sink (ensure sink is closed per method contract)
      try (InputStream is = Files.newInputStream(tempFile);
          OutputStream os = new BufferedOutputStream(out)) {
        IOUtils.copy(is, os);
      }
    } finally {
      Files.delete(tempFile);
    }
  }

  @Builder
  public static class Options {
    @Builder.Default boolean withDelegates = false;
    @Builder.Default boolean withHistory = false;
    @Builder.Default boolean withAttachmentXmlData = false;

    public static Options withEverything() {
      return builder().withDelegates(true).withHistory(true).withAttachmentXmlData(true).build();
    }

    public static Options forBattleCalculator() {
      return builder().build();
    }
  }

  public static void saveGameUncompressed(
      final OutputStream sink,
      final GameData data,
      final Options options,
      final Version engineVersion)
      throws IOException {
    // write to temporary file first in case of error
    try (ObjectOutputStream outStream = new ObjectOutputStream(sink)) {
      outStream.writeObject(engineVersion);
      try (GameData.Unlocker ignored = data.acquireWriteLock()) {
        final var history = data.getHistory();
        if (!options.withHistory) {
          data.resetHistory();
        }
        // TODO: Attachment order data is only used for XML export and takes up lots of memory.
        // Could we remove it and just get the info again from the XML when exporting?
        final var attachments = data.getAttachmentOrderAndValues();
        if (!options.withAttachmentXmlData) {
          data.setAttachmentOrderAndValues(null);
        }
        outStream.writeObject(data);
        if (!options.withAttachmentXmlData) {
          data.setAttachmentOrderAndValues(attachments);
        }
        if (!options.withHistory) {
          data.setHistory(history);
        }
        if (options.withDelegates) {
          writeDelegates(data, outStream);
        } else {
          outStream.writeObject(DELEGATE_LIST_END);
        }
      }
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
