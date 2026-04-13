package games.strategy.engine.framework;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.engine.data.GameData;
import games.strategy.engine.delegate.IDelegate;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
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
import java.util.zip.ZipException;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NonNls;
import org.triplea.config.product.ProductVersionReader;
import org.triplea.debug.error.reporting.StackTraceReportModel;

/** Responsible for loading saved games, new games from xml, and saving games. */
@Slf4j
public final class GameDataManager {
  @NonNls private static final String DELEGATE_START = "<DelegateStart>";
  @NonNls private static final String DELEGATE_DATA_NEXT = "<DelegateData>";
  @NonNls private static final String DELEGATE_LIST_END = "<EndDelegateList>";

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

  /**
   * Loads game data from the specified stream.
   *
   * @param is The stream from which the game data will be loaded. The caller is responsible for
   *     closing this stream; it will not be closed when this method returns.
   * @return The loaded game data, or an empty optional if an error occurs.
   */
  public static Optional<GameData> loadGame(final InputStream is) {
    try (GZIPInputStream input = new GZIPInputStream(is)) {
      return loadGameUncompressed(input);
    } catch (final EOFException e) {
      log.error(
          "End of loading file has been reached unexpectedly.\n"
              + "When reporting to TripleA include steps to produce such a corrupted file.",
          e);
    } catch (final ZipException e) {
      log.error("Unzipping the file has failed. Check that the correct file was selected.", e);
    } catch (final Exception e) {
      log.error("Error loading game data", e);
    }
    return Optional.empty();
  }

  public static Optional<GameData> loadGameUncompressed(final InputStream is) {
    try (ObjectInputStream input = new ObjectInputStream(is)) {
      // read Version object (unused)
      input.readObject();
      final GameData data = (GameData) input.readObject();
      StackTraceReportModel.setCurrentMapNameFromGameData(data);
      data.postDeSerialize();
      loadDelegates(input, data);
      data.fixUpNullPlayersInDelegates();
      return Optional.of(data);
    } catch (final Exception e) {
      log.warn(
          "Error loading save game, saved version might not be compatible with current engine.", e);
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
  public static void saveGame(final OutputStream out, final GameData gameData) throws IOException {
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
        saveGameUncompressed(zippedOutStream, gameData, Options.forSaveGame());
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

    public static Options forSaveGame() {
      // Omit attachment data as it uses a lot of memory and is only needed for XML exports, which
      // now reads it from the original XML instead.
      return builder().withDelegates(true).withHistory(true).withAttachmentXmlData(false).build();
    }

    public static Options forBattleCalculator() {
      return builder().build();
    }
  }

  public static void saveGameUncompressed(
      final OutputStream sink, final GameData data, final Options options) throws IOException {
    // write to temporary file first in case of error
    try (ObjectOutputStream outStream = new ObjectOutputStream(sink)) {
      outStream.writeObject(ProductVersionReader.getCurrentVersion());
      try (GameData.Unlocker ignored = data.acquireWriteLock()) {
        final var history = data.getHistory();
        if (!options.withHistory) {
          data.resetHistory();
        }
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
