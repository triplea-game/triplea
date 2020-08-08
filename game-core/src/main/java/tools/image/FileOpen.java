package tools.image;

import games.strategy.engine.framework.system.SystemProperties;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/** A file chooser for use by map making tools to prompt the user to select a file to open. */
public class FileOpen {
  private File file;

  public FileOpen(final String title, final File currentDirectory, final String... extensions) {
    this(title, currentDirectory, null, extensions);
  }

  FileOpen(
      final String title,
      final File currentDirectory,
      final File selectedFile,
      final String... extensions) {
    final JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle(title);
    if (selectedFile != null) {
      chooser.setSelectedFile(selectedFile);
    }
    chooser.setCurrentDirectory(
        ((currentDirectory == null || !currentDirectory.exists())
            ? new File(SystemProperties.getUserDir())
            : currentDirectory));
    /*
     * Show only text and gif files
     */
    chooser.setFileFilter(
        new javax.swing.filechooser.FileFilter() {
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
      file = chooser.getSelectedFile();
    } catch (final Exception ex) {
      JOptionPane.showMessageDialog(
          null, "Warning! Could not load the file!", "Warning!", JOptionPane.WARNING_MESSAGE);
      file = null;
    }
  } // constructor

  /** Returns the newly selected file. Will return null if no file is selected. */
  public File getFile() {
    return file;
  }

  /** Returns the newly selected file. Will return null if no file is selected. */
  public String getPathString() {
    return (file == null) ? null : file.getPath();
  }
}
