package org.triplea.swing;

import com.google.common.annotations.VisibleForTesting;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.swing.JOptionPane;
import lombok.Builder;

@Builder
public class FileChooser {
  private static final String PERIOD = ".";

  @Nullable private Frame parent;
  @Nullable private String title;
  @Builder.Default private int mode = FileDialog.SAVE;
  @Nullable private Path directory;
  @Nullable private FilenameFilter filenameFilter;
  @Nullable private String fileName;
  @Nullable private String fileExtension;

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
    final Path path = getSelectedPath(fileDialog);
    if (path == null) {
      return Optional.empty();
    }

    // If the user selects a filename that already exists, the AWT Dialog will ask the user for
    // confirmation, but this fails if we append an extension afterwards, so show our own dialog.
    if (mode == FileDialog.SAVE && Files.exists(path) && !shouldReplaceExistingFile(path)) {
      return Optional.empty();
    }

    return Optional.of(path);
  }

  private Path getSelectedPath(final FileDialog fileDialog) {
    final String fileName = fileDialog.getFile();
    if (fileName == null) {
      return null;
    }
    Path path = Path.of(fileDialog.getDirectory(), fileName);
    if (mode == FileDialog.SAVE && fileExtension != null) {
      path = appendExtensionIfAbsent(path, fileExtension);
    }
    return path;
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
        .toLowerCase()
        .endsWith(extensionWithLeadingPeriod.toLowerCase())) {
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
