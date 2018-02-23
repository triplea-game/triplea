package games.strategy.engine.data.properties;

import java.awt.FileDialog;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.Arrays;
import java.util.Optional;

import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.system.SystemProperties;

/**
 * User editable property representing a file.
 *
 * <p>
 * Presents a clickable label with the currently selected file name, through which a file dialog panel is accessible to
 * change the file.
 * </p>
 */
public class FileProperty extends AEditableProperty {
  private static final long serialVersionUID = 6826763550643504789L;
  private static final String[] defaultImageSuffixes = {"png", "jpg", "jpeg", "gif"};

  private final String[] m_acceptableSuffixes;
  private File m_file;

  /**
   * Construct a new file property.
   *
   * @param name - The name of the property
   * @param fileName - The name of the file to be associated with this property
   */
  public FileProperty(final String name, final String description, final String fileName) {
    this(name, description, getFileIfExists(new File(fileName)));
  }

  private static File getFileIfExists(final File file) {
    if (file.exists()) {
      return file;
    }
    return null;
  }


  public FileProperty(final String name, final String description, final File file) {
    this(name, description, file, defaultImageSuffixes);
  }

  public FileProperty(final String name, final String description, final File file, final String[] acceptableSuffixes) {
    super(name, description);
    m_file = getFileIfExists(file);
    m_acceptableSuffixes = acceptableSuffixes;
  }

  /**
   * Gets the file associated with this property.
   *
   * @return The file associated with this property
   */
  @Override
  public Object getValue() {
    return m_file;
  }

  @Override
  public void setValue(final Object value) throws ClassCastException {
    m_file = (File) value;
  }

  /**
   * Gets a Swing component to display this property.
   *
   * @return a non-editable JTextField
   */
  @Override
  public JComponent getEditorComponent() {
    final JTextField label;
    if (m_file == null) {
      label = new JTextField();
    } else {
      label = new JTextField(m_file.getAbsolutePath());
    }
    label.setEditable(false);
    label.addMouseListener(new MouseListener() {
      @Override
      public void mouseClicked(final MouseEvent e) {
        final File selection = getFileUsingDialog(m_acceptableSuffixes);
        if (selection != null) {
          m_file = selection;
          label.setText(m_file.getAbsolutePath());
          // Ask Swing to repaint this label when it's convenient
          SwingUtilities.invokeLater(() -> label.repaint());
        }
      }

      @Override
      public void mouseEntered(final MouseEvent e) {}

      @Override
      public void mouseExited(final MouseEvent e) {}

      @Override
      public void mousePressed(final MouseEvent e) {}

      @Override
      public void mouseReleased(final MouseEvent e) {}
    });
    return label;
  }

  /**
   * Prompts the user to select a file.
   */
  private static File getFileUsingDialog(final String... acceptableSuffixes) {
    // For some strange reason,
    // the only way to get a Mac OS X native-style file dialog
    // is to use an AWT FileDialog instead of a Swing JDialog
    if (SystemProperties.isMac()) {
      final FileDialog fileDialog = GameRunner.newFileDialog();
      fileDialog.setMode(FileDialog.LOAD);
      fileDialog.setFilenameFilter((dir, name) -> {
        if ((acceptableSuffixes == null) || (acceptableSuffixes.length == 0)) {
          return true;
        }
        for (final String suffix : acceptableSuffixes) {
          if (name.toLowerCase().endsWith(suffix)) {
            return true;
          }
        }
        return false;
      });
      fileDialog.setVisible(true);
      final String fileName = fileDialog.getFile();
      final String dirName = fileDialog.getDirectory();
      if (fileName == null) {
        return null;
      }
      return new File(dirName, fileName);
    }
    final Optional<File> selectedFile = GameRunner.showFileChooser(new FileFilter() {
      @Override
      public boolean accept(final File file) {
        if (file == null) {
          return false;
        } else if (file.isDirectory()) {
          return true;
        } else {
          final String name = file.getAbsolutePath().toLowerCase();
          for (final String suffix : acceptableSuffixes) {
            if (name.endsWith(suffix)) {
              return true;
            }
          }
          return false;
        }
      }

      @Override
      public String getDescription() {
        return Arrays.toString(acceptableSuffixes);
      }
    });
    return selectedFile.orElse(null);
  }

  @Override
  public boolean validate(final Object value) {
    if (value == null) {
      return true;
    }
    if (value instanceof File) {
      final File file = (File) value;
      for (final String suff : m_acceptableSuffixes) {
        if ((file.getName() != null) && file.getName().endsWith(suff)) {
          return true;
        }
      }
    }
    return false;
  }
}
