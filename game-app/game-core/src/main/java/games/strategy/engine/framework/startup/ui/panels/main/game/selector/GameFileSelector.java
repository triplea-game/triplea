package games.strategy.engine.framework.startup.ui.panels.main.game.selector;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.GameDataFileUtils;
import games.strategy.triplea.settings.ClientSetting;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Builder;
import org.triplea.swing.FileChooser;

/**
 * Utility class containing swing logic to show a file prompt to user. Used for selecting saved
 * games.
 */
@Builder
public final class GameFileSelector {
  @Nonnull private final Consumer<Path> fileDoesNotExistAction;

  /**
   * Displays a file chooser dialog for the user to select the file to which the current game should
   * be saved.
   *
   * @param frame The owner of the file chooser dialog; may be {@code null}.
   * @return The file to which the current game should be saved or {@code null} if the user
   *     cancelled the operation.
   */
  public static Optional<Path> getSaveGameLocation(final Frame frame, final GameData gameData) {
    final Path directory = ClientSetting.saveGamesFolderPath.getValueOrThrow();
    return new FileChooser()
        .parent(frame)
        .title("Save Game as")
        .directory(directory)
        .filenameFilter((dir, name) -> GameDataFileUtils.isCandidateFileName(name))
        .fileExtension(GameDataFileUtils.getExtension())
        .fileName(getSaveGameName(gameData))
        .chooseSave();
  }

  private static String getSaveGameName(final GameData gameData) {
    return gameData.getSaveGameFileName().orElse(formatGameName(gameData.getGameName()));
  }

  private static String formatGameName(final String gameName) {
    final ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
    return new StringJoiner("-")
        .add(String.valueOf(now.getYear()))
        .add(String.valueOf(now.getMonthValue()))
        .add(String.valueOf(now.getDayOfMonth()))
        .add(gameName.replace(" ", "-") + GameDataFileUtils.getExtension())
        .toString();
  }

  /**
   * Opens up a UI pop-up allowing user to select a game file. Returns nothing if user closes the
   * pop-up.
   */
  public Optional<Path> selectGameFile(final Frame owner) {
    final FileDialog fileDialog = new FileDialog(owner, "Open Saved Game");
    fileDialog.setMode(FileDialog.LOAD);
    fileDialog.setDirectory(ClientSetting.saveGamesFolderPath.getValueOrThrow().toString());
    fileDialog.setFilenameFilter((dir, name) -> GameDataFileUtils.isCandidateFileName(name));
    fileDialog.setVisible(true);

    // FileDialog.getFiles() always returns an array
    // of 1 or 0 items, because FileDialog.multipleMode is false by default
    return Arrays.stream(fileDialog.getFiles())
        .findAny()
        .map(File::toPath)
        .map(this::mapFileResult);
  }

  @Nullable
  private Path mapFileResult(final Path file) {
    if (Files.exists(file)) {
      return file;
    } else {
      fileDoesNotExistAction.accept(file);
      return null;
    }
  }
}
