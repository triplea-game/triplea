package org.triplea.swing;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.framework.system.SystemProperties;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import lombok.Builder;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NonNls;

@Builder
public class FileChooser {
  @NonNls private static final String PERIOD = ".";

  @Nullable private Frame parent;
  @Nullable private String title;
  @Builder.Default private int mode = FileDialog.SAVE;
  @Nullable private Path directory;
  @Nullable private FilenameFilter filenameFilter;
  @Nullable private String fileName;
  @Nullable private String fileExtension;

  /**
   * Wraps the file chooser for a given {@code defaultFileName}, infers its file extension and uses
   * as starting directory the user directory.
   *
   * @param frame parent frame for the file chooser
   * @param defaultFileName default name of the file
   * @return path to the chosen file
   */
  public static Optional<Path> chooseExportFileWithDefaultName(
      JFrame frame, String defaultFileName) {
    return builder()
        .parent(frame)
        .directory(Path.of(SystemProperties.getUserDir()))
        .fileName(defaultFileName)
        .fileExtension("." + FilenameUtils.getExtension(defaultFileName))
        .build()
        .chooseFile();
  }

  public Optional<Path> chooseFile() {
    // Use FileDialog rather than JFileChooser as the former results in a native dialog, which on
    // some platforms, like macOS provides a much better user experience than JFileChooser.
    final FileDialog fileDialog = new FileDialog(parent);
    fileDialog.setMode(mode);
    if (title != null) {
      fileDialog.setTitle(title);
    }
    if (directory != null) {
      fileDialog.setDirectory(directory.toString());
    }
    if (filenameFilter != null) {
      fileDialog.setFilenameFilter(filenameFilter);
    }
    if (fileName != null) {
      fileDialog.setFile(fileName);
    }

    fileDialog.setVisible(true);
    return getSelectedPath(fileDialog);
  }

  private Optional<Path> getSelectedPath(final FileDialog fileDialog) {
    final String fileName = fileDialog.getFile();
    if (fileName == null) {
      return Optional.empty();
    }
    Path path = Path.of(fileDialog.getDirectory(), fileName);
    if (mode == FileDialog.SAVE && fileExtension != null) {
      final Path newPath = appendExtensionIfAbsent(path, fileExtension);
      // If the user selects a file that already exists, the FileDialog will ask the user for
      // confirmation, but not if we appended an extension above, so show our own dialog.
      if (!newPath.equals(path) && Files.exists(newPath) && !shouldReplaceExistingFile(newPath)) {
        return Optional.empty();
      }
      path = newPath;
    }
    return Optional.of(path);
  }

  private boolean shouldReplaceExistingFile(final Path path) {
    final int result =
        JOptionPane.showConfirmDialog(
            parent,
            String.format(
                "A file named \"%s\" already exists. Do you want to replace it?",
                path.getFileName()),
            "Confirm Save",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
    return result == JOptionPane.YES_OPTION;
  }

  @VisibleForTesting
  public static Path appendExtensionIfAbsent(final Path file, final String extension) {
    final String extensionWithLeadingPeriod = extensionWithLeadingPeriod(extension);
    if (file.getFileName()
        .toString()
        .toLowerCase(Locale.ROOT)
        .endsWith(extensionWithLeadingPeriod.toLowerCase(Locale.ROOT))) {
      return file;
    }

    return file.resolveSibling(file.getFileName() + extensionWithLeadingPeriod);
  }

  @VisibleForTesting
  static String extensionWithLeadingPeriod(final String extension) {
    return extension.isEmpty() || extension.startsWith(PERIOD) ? extension : PERIOD + extension;
  }

  @VisibleForTesting
  static String extensionWithoutLeadingPeriod(final String extension) {
    return extension.startsWith(PERIOD) ? extension.substring(PERIOD.length()) : extension;
  }
}
