package tools.util;

import games.strategy.engine.framework.system.SystemProperties;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nullable;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import lombok.Getter;

/** A file chooser for use by map making tools to prompt the user to select a file to open. */
@Getter
public class FileOpen {
  /** -- GETTER -- Returns the newly selected file. Will return null if no file is selected. */
  private @Nullable Path file;

  public FileOpen(final String title, final Path currentDirectory, final String... extensions) {
    this(title, currentDirectory, null, extensions);
  }

  public FileOpen(
      final String title,
      final Path currentDirectory,
      final Path selectedFile,
      final String... extensions) {
    final JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle(title);
    if (selectedFile != null) {
      chooser.setSelectedFile(selectedFile.toFile());
    }
    final Path currentDirectoryFallback =
        currentDirectory == null || !Files.exists(currentDirectory)
            ? Path.of(SystemProperties.getUserDir())
            : currentDirectory;

    chooser.setCurrentDirectory(currentDirectoryFallback.toFile());
    /*
     * Show only text and gif files
     */
    chooser.setFileFilter(
        new FileFilter() {
          @Override
          public boolean accept(final File f) {
            if (f.isDirectory()) {
              return true;
            }
            for (final String ex : extensions) {
              if (f.getName().endsWith(ex)) {
                return true;
              }
            }
            return false;
          }

          @Override
          public String getDescription() {
            final StringBuilder buf = new StringBuilder();
            for (final String ex : extensions) {
              buf.append("*").append(ex).append(" ");
            }
            return buf.toString();
          }
        });
    final int result = chooser.showOpenDialog(null);
    if (result == JFileChooser.CANCEL_OPTION) {
      return;
    }
    try {
      // get the file
      file = chooser.getSelectedFile().toPath();
    } catch (final Exception ex) {
      JOptionPane.showMessageDialog(
          null, "Warning! Could not load the file!", "Warning!", JOptionPane.WARNING_MESSAGE);
      file = null;
    }
  }
}
