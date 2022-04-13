package org.triplea.swing;

import com.google.common.annotations.VisibleForTesting;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import javax.swing.JOptionPane;

public class FileChooser {
  private static final String PERIOD = ".";

  private Frame parent;
  private String title = "";
  private Path directory;
  private FilenameFilter filenameFilter;
  private String fileName;
  private String fileExtension;

  public FileChooser parent(final Frame parent) {
    this.parent = parent;
    return this;
  }

  public FileChooser title(final String title) {
    this.title = title;
    return this;
  }

  public FileChooser directory(final Path directory) {
    this.directory = directory;
    return this;
  }

  public FileChooser filenameFilter(final FilenameFilter filenameFilter) {
    this.filenameFilter = filenameFilter;
    return this;
  }

  public FileChooser fileName(final String fileName) {
    this.fileName = fileName;
    return this;
  }

  public FileChooser fileExtension(final String fileExtension) {
    this.fileExtension = fileExtension;
    return this;
  }

  public Optional<Path> chooseSave() {
    // Use FileDialog rather than JFileChooser as the former results in a native dialog, which on
    // some platforms, like macOS provides a much better user experience than JFileChooser.
    final FileDialog fileDialog = new FileDialog(parent, "Save Game as");
    fileDialog.setMode(FileDialog.SAVE);
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
    // If the user selects a filename that already exists, the AWT Dialog will ask the user for
    // confirmation, but this fails if we append an extension afterwards, so show our own dialog.
    if (path == null || (Files.exists(path) && !shouldReplaceExistingFile(path))) {
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
    if (fileExtension != null) {
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
