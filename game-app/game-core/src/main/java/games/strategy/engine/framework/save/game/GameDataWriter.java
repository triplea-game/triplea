package games.strategy.engine.framework.save.game;

import games.strategy.engine.data.GameData;
import games.strategy.engine.delegate.DelegateExecutionManager;
import games.strategy.engine.framework.GameDataManager;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.triplea.io.IoUtils;

/** Responsible to write game data to a file or output bytes. */
@Slf4j
@UtilityClass
public class GameDataWriter {

  public static byte[] writeToBytes(
      final GameData gameData, final DelegateExecutionManager delegateExecutionManager) {
    try {
      return IoUtils.writeToMemory(
          outputStream ->
              GameDataWriter.writeToOutputStream(gameData, outputStream, delegateExecutionManager));
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Saves the game to {@code file}. Writes to a sibling temp file first and renames on success, so
   * the existing file at {@code file} is preserved if the delegate lock cannot be acquired or the
   * write fails partway through.
   */
  public static void writeToFile(
      final GameData gameData,
      final DelegateExecutionManager delegateExecutionManager,
      final Path file) {

    if (!acquireDelegateLock(delegateExecutionManager)) {
      log.error("Error saving game.. could not lock delegate execution: {}", file.toAbsolutePath());
      return;
    }

    try {
      Path tempFile = null;
      try {
        tempFile = Files.createTempFile(file.getParent(), file.getFileName().toString(), ".tmp");
        try (OutputStream out = Files.newOutputStream(tempFile)) {
          GameDataManager.saveGame(out, gameData);
        }
        Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING);
        tempFile = null;
      } catch (final IOException e) {
        log.error("Failed to save game to file: " + file.toAbsolutePath(), e);
      } finally {
        if (tempFile != null) {
          try {
            Files.deleteIfExists(tempFile);
          } catch (final IOException e) {
            log.warn("Failed to delete temp save file: " + tempFile.toAbsolutePath(), e);
          }
        }
      }
    } finally {
      delegateExecutionManager.resumeDelegateExecution();
    }
  }

  private static void writeToOutputStream(
      final GameData gameData,
      final OutputStream out,
      final DelegateExecutionManager delegateExecutionManager)
      throws IOException {
    if (!acquireDelegateLock(delegateExecutionManager)) {
      log.error("Error saving game..  could not lock delegate execution");
      return;
    }

    try {
      GameDataManager.saveGame(out, gameData);
    } finally {
      delegateExecutionManager.resumeDelegateExecution();
    }
  }

  // error prone is detecting the identical boolean condition as an error, when it's
  // intentional and is actually a retry.
  @SuppressWarnings("IdentityBinaryExpression")
  private static boolean acquireDelegateLock(
      final DelegateExecutionManager delegateExecutionManager) {
    try {
      // try twice
      return delegateExecutionManager.blockDelegateExecution(6000)
          || delegateExecutionManager.blockDelegateExecution(6000);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }
}
