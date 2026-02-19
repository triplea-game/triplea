package tools.util;

import com.google.common.base.Strings;
import games.strategy.engine.framework.system.SystemProperties;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import lombok.Getter;

/** A file chooser for use by map making tools to prompt the user to select a file to save. */
@Getter
public class FileSave {
  /** -- GETTER -- Returns the directory path as a File object. */
  private final Path file;

  public FileSave(final String title, final String name, final Path currentDirectory) {
    this(
        title,
        name,
        currentDirectory,
        JFileChooser.DIRECTORIES_ONLY,
        null,
        new FileFilter() {
          @Override
          public boolean accept(final File f) {
            return f.isDirectory();
          }

          @Override
          public String getDescription() {
            return "Folder To Save In";
          }
        });
  }

  /**
   * Initializes a new instance of the FileSave class with the specified file selection mode and
   * selected file.
   *
   * @param fileSelectionMode The type of files to be displayed. Must be one of {@link
   *     JFileChooser#FILES_ONLY}, {@link JFileChooser#DIRECTORIES_ONLY}, or {@link
   *     JFileChooser#FILES_AND_DIRECTORIES}.
   */
  public FileSave(
      final String title,
      final int fileSelectionMode,
      final Path selectedFile,
      final Path currentDirectory) {
    this(title, null, currentDirectory, fileSelectionMode, selectedFile, null);
  }

  /**
   * Initializes a new instance of the FileSave class with the specified file selection mode,
   * selected file, and file filter.
   *
   * @param fileSelectionMode The type of files to be displayed. Must be one of {@link
   *     JFileChooser#FILES_ONLY}, {@link JFileChooser#DIRECTORIES_ONLY}, or {@link
   *     JFileChooser#FILES_AND_DIRECTORIES}.
   */
  public FileSave(
      final String title,
      final String name,
      final Path currentDirectory,
      final int fileSelectionMode,
      final Path selectedFile,
      final FileFilter fileFilter) {
    final JFileChooser chooser = new JFileChooser();
    chooser.setFileSelectionMode(fileSelectionMode);
    chooser.setDialogTitle(title);
    if (selectedFile != null) {
      chooser.setSelectedFile(selectedFile.toFile());
    }
    final Path currentDirectoryFallback =
        currentDirectory == null || !Files.exists(currentDirectory)
            ? Path.of(SystemProperties.getUserDir())
            : currentDirectory;

    chooser.setCurrentDirectory(currentDirectoryFallback.toFile());
    if (fileFilter != null) {
      chooser.setFileFilter(fileFilter);
    }
    file =
        chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION
            ? chooser.getSelectedFile().toPath().resolve(Strings.nullToEmpty(name))
            : null;
  }
}
