package tools.image;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

public class FileOpen {
  private final String ERR_MSG_1 = "Warning! Could not load the file!";
  private File file = null;

  /**
   * Default Constructor.
   *
   * @param java
   *        .lang.String title the title of the JFileChooser
   * @exception java.lang.Exception
   *            ex
   *            Creates a file selection dialog starting at the current
   *            working directory. Filters out all non-txt files and
   *            handles possible file load errors.
   */
  public FileOpen(final String title) {
    this(title, ".txt", ".gif", ".png");
  }

  public FileOpen(final String title, final String... extensions) {
    this(title, new File(System.getProperties().getProperty("user.dir")), extensions);
  }

  public FileOpen(final String title, final File currentDirectory) {
    this(title, currentDirectory, ".txt", ".gif", ".png");
  }

  public FileOpen(final String title, final File currentDirectory, final String... extensions) {
    this(title, currentDirectory, null, extensions);
  }

  public FileOpen(final String title, final File currentDirectory, final File selectedFile,
      final String... extensions) {
    final JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle(title);
    if (selectedFile != null) {
      chooser.setSelectedFile(selectedFile);
    }
    chooser.setCurrentDirectory(((currentDirectory == null || !currentDirectory.exists())
        ? new File(System.getProperties().getProperty("user.dir")) : currentDirectory));
    /*
     * Show only text and gif files
     */
    chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
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
        final StringBuffer buf = new StringBuffer();
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
      JOptionPane.showMessageDialog(null, ERR_MSG_1, "Warning!", JOptionPane.WARNING_MESSAGE);
      file = null;
    }
  }// constructor

  /**
   * Returns the newly selected file.
   * Will return null if no file is selected.
   *
   * @return java.io.File
   */
  public File getFile() {
    return file;
  }

  /**
   * Returns the newly selected file.
   * Will return null if no file is selected.
   *
   * @return java.lang.String
   */
  public String getPathString() {
    if (file == null) {
      return null;
    } else {
      return file.getPath();
    }
  }
}
