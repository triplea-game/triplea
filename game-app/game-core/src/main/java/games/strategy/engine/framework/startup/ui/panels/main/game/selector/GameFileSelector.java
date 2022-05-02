package games.strategy.engine.framework.startup.ui.panels.main.game.selector;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.GameDataFileUtils;
import games.strategy.triplea.settings.ClientSetting;
import java.awt.Frame;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
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
    return createFileChooser(frame)
        .mode(FileChooser.SAVE)
        .title("Save Game As")
        .fileName(getSaveGameName(gameData))
        .build()
        .chooseFile();
  }

  /**
   * Opens up a UI pop-up allowing user to select a game file. Returns nothing if user closes the
   * pop-up.
   */
  public Optional<Path> selectGameFile(final Frame frame) {
    Optional<Path> path =
        createFileChooser(frame)
            .mode(FileChooser.LOAD)
            .title("Open Saved Game")
            .build()
            .chooseFile();
    if (path.isPresent() && !Files.exists(path.get())) {
      fileDoesNotExistAction.accept(path.get());
      path = Optional.empty();
    }
    return path;
  }

  private static FileChooser.FileChooserBuilder createFileChooser(final Frame frame) {
    return FileChooser.builder()
        .parent(frame)
        .directory(ClientSetting.saveGamesFolderPath.getValueOrThrow())
        .filenameFilter((dir, name) -> GameDataFileUtils.isCandidateFileName(name))
        .fileExtension(GameDataFileUtils.getExtension());
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
}
