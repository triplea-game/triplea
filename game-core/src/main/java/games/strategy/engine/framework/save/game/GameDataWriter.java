package games.strategy.engine.framework.save.game;

import games.strategy.engine.data.GameData;
import games.strategy.engine.delegate.DelegateExecutionManager;
import games.strategy.engine.framework.GameDataManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;
import org.triplea.injection.Injections;
import org.triplea.io.IoUtils;

/** Responsible to write game data to a file or output bytes. */
@Log
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

  public static void writeToFile(
      final GameData gameData,
      final DelegateExecutionManager delegateExecutionManager,
      final File file) {

    try (OutputStream fout = new FileOutputStream(file)) {
      GameDataWriter.writeToOutputStream(gameData, fout, delegateExecutionManager);
    } catch (final IOException e) {
      log.log(Level.SEVERE, "Failed to save game to file: " + file.getAbsolutePath(), e);
    }
  }

  // error prone is detecting the identical boolean condition as an error, when it's
  // intentional and is actually a retry.
  @SuppressWarnings("IdentityBinaryExpression")
  private static void writeToOutputStream(
      final GameData gameData,
      final OutputStream out,
      final DelegateExecutionManager delegateExecutionManager)
      throws IOException {
    final String errorMessage = "Error saving game.. ";

    try {
      // TODO: is this necessary to save a game?
      // try twice
      if (!delegateExecutionManager.blockDelegateExecution(6000)
          && !delegateExecutionManager.blockDelegateExecution(6000)) {
        log.severe(errorMessage + " could not lock delegate execution");
        return;
      }
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      return;
    }

    try {
      GameDataManager.saveGame(out, gameData, Injections.getInstance().getEngineVersion());
    } finally {
      delegateExecutionManager.resumeDelegateExecution();
    }
  }
}
