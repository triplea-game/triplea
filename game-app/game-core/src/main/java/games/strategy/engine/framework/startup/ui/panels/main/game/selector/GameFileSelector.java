package games.strategy.engine.framework.startup.ui.panels.main.game.selector;

import games.strategy.engine.framework.GameDataFileUtils;
import games.strategy.triplea.settings.ClientSetting;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Builder;

/**
 * Utility class containing swing logic to show a file prompt to user. Used for selecting saved
 * games.
 */
@Builder
public final class GameFileSelector {
  @Nonnull private final Consumer<Path> fileDoesNotExistAction;

  /**
   * Opens up a UI pop-up allowing user to select a game file. Returns nothing if user closes the
   * pop-up.
   */
  public Optional<Path> selectGameFile(final Frame owner) {
    final FileDialog fileDialog = new FileDialog(owner);
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
