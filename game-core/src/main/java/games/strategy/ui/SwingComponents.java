package games.strategy.ui;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.net.OpenFileUtility;
import games.strategy.triplea.UrlConstants;

/**
 * Wrapper/utility class to give Swing components a nicer API. This class is to help extract pure UI code out of
 * the rest of the code base. This also gives us a cleaner interface between UI and the rest of the code.
 */
public class SwingComponents {
  private static final String PERIOD = ".";
  private static final Collection<String> visiblePrompts = new HashSet<>();

  /**
   * Enum for swing codes that represent key events. In this case holding control or the
   * meta keys.
   */
  public enum KeyDownMask {
    META_DOWN(InputEvent.META_DOWN_MASK),

    CTRL_DOWN(InputEvent.CTRL_DOWN_MASK);

    private final int code;

    KeyDownMask(final int code) {
      this.code = code;
    }
  }

  public static void addSpaceKeyListener(final JComponent component, final Runnable runnable) {
    addKeyListener(component, KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), runnable);
  }

  public static void addEnterKeyListener(final JComponent component, final Runnable runnable) {
    addKeyListener(component, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), runnable);
  }

  /**
   * To a given dialog, adds a key listener that is fired if a key is pressed.
   */
  public static void addEscapeKeyListener(
      final RootPaneContainer dialog,
      final Runnable keyDownAction) {
    if (dialog.getRootPane() != null) {
      addKeyListener(dialog.getRootPane(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), keyDownAction);
    }
  }

  /**
   * To a given component, adds a key listener that is fired if a key is pressed.
   */
  public static void addEscapeKeyListener(
      final JComponent component,
      final Runnable keyDownAction) {

    // TODO: null checks are bit questionable, have them here because they were here before...
    if (component.getRootPane() != null) {
      addKeyListener(component.getRootPane(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), keyDownAction);
    }
  }

  public static void addKeyListenerWithMetaAndCtrlMasks(
      final JFrame component, final char key, final Runnable action) {
    addKeyListener((JComponent) component.getContentPane(), key, KeyDownMask.CTRL_DOWN, action);
    addKeyListener((JComponent) component.getContentPane(), key, KeyDownMask.META_DOWN, action);
  }

  private static void addKeyListener(
      final JComponent component,
      final char key,
      final KeyDownMask keyDownMask,
      final Runnable keyDownAction) {
    addKeyListener(component, KeyStroke.getKeyStroke(key, keyDownMask.code), keyDownAction);
  }



  private static void addKeyListener(
      final JComponent component,
      final KeyStroke keyStroke,
      final Runnable keyDownAction) {

    // We are using the object address here of our action.
    // It is okay since we only need it to be the same value when we store it in the
    // input and action maps below. Having the address be logged could be useful
    // for debugging, otherwise no particular reason to use this exact value.
    final String actionKey = keyDownAction.toString();

    component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(keyStroke, actionKey);

    component.getActionMap().put(actionKey, new AbstractAction() {
      private static final long serialVersionUID = -280371946771796597L;

      @Override
      public void actionPerformed(final ActionEvent e) {
        keyDownAction.run();
      }
    });
  }

  public static JTabbedPane newJTabbedPane() {
    return newJTabbedPane(900, 600);
  }

  public static JTabbedPane newJTabbedPane(final int width, final int height) {
    final JTabbedPane tabbedPane = new JTabbedPaneWithFixedWidthTabs();
    tabbedPane.setPreferredSize(new Dimension(width, height));
    return tabbedPane;
  }

  public static ButtonGroup createButtonGroup(final JRadioButton... radioButtons) {
    final ButtonGroup group = new ButtonGroup();
    for (final JRadioButton radioButton : Arrays.asList(radioButtons)) {
      group.add(radioButton);
    }
    return group;
  }


  /**
   * Adds a focus listener to a given component and executes a given action when focus is lost.
   */
  public static void addTextFieldFocusLostListener(final JTextField component, final Runnable focusLostListener) {
    addFocusLostListener(component, focusLostListener);
    component.addActionListener(e -> focusLostListener.run());
  }

  private static void addFocusLostListener(final JComponent component, final Runnable focusLostListener) {
    component.addFocusListener(new FocusListener() {
      @Override
      public void focusGained(final FocusEvent e) {

      }

      @Override
      public void focusLost(final FocusEvent e) {
        focusLostListener.run();
      }
    });
  }

  public enum KeyboardCode {
    D(KeyEvent.VK_D), G(KeyEvent.VK_G);


    private final int keyEventCode;

    KeyboardCode(final int keyEventCode) {
      this.keyEventCode = keyEventCode;
    }

    int getSwingKeyEventCode() {
      return keyEventCode;
    }
  }


  public static JScrollPane newJScrollPane(final Component contents) {
    final JScrollPane scroll = new JScrollPane();
    scroll.setViewportView(contents);
    scroll.setBorder(null);
    scroll.getViewport().setBorder(null);
    return scroll;
  }

  public static void promptUser(final String title, final String message, final Runnable confirmedAction) {
    boolean showMessage = false;
    synchronized (visiblePrompts) {
      if (!visiblePrompts.contains(message)) {
        visiblePrompts.add(message);
        showMessage = true;
      }
    }

    if (showMessage) {
      SwingUtilities.invokeLater(() -> {
        // blocks until the user responds to the modal dialog
        final int response = JOptionPane.showConfirmDialog(null, message, title,
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        // dialog is now closed
        visiblePrompts.remove(message);
        if (response == JOptionPane.YES_OPTION) {
          confirmedAction.run();
        }
      });
    }

  }

  public static void newMessageDialog(final String msg) {
    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, msg));
  }

  /**
   * Executes the specified action when the specified window is in the process of being closed.
   *
   * @param window The window to which the action is attached; must not be {@code null}.
   * @param action The action to execute; must not be {@code null}.
   */
  public static void addWindowClosingListener(final Window window, final Runnable action) {
    checkNotNull(window);
    checkNotNull(action);

    window.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        action.run();
      }
    });
  }

  /**
   * Executes the specified action when the specified window has been closed.
   *
   * @param window The window to which the action is attached; must not be {@code null}.
   * @param action The action to execute; must not be {@code null}.
   */
  public static void addWindowClosedListener(final Window window, final Runnable action) {
    checkNotNull(window);
    checkNotNull(action);

    window.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosed(final WindowEvent e) {
        action.run();
      }
    });
  }

  /**
   * Creates a new combo box model containing a copy of the specified collection of values.
   *
   * @param values The values used to populate the combo box model.
   *
   * @return A new combo box model.
   */
  public static <T> ComboBoxModel<T> newComboBoxModel(final Collection<T> values) {
    checkNotNull(values);

    final DefaultComboBoxModel<T> comboBoxModel = new DefaultComboBoxModel<>();
    values.forEach(comboBoxModel::addElement);
    return comboBoxModel;
  }

  /**
   * Creates a new list model containing a copy of the specified collection of values.
   *
   * @param values The values used to populate the list model.
   *
   * @return A new list model.
   */
  public static <T> ListModel<T> newListModel(final Collection<T> values) {
    checkNotNull(values);

    final DefaultListModel<T> listModel = new DefaultListModel<>();
    values.forEach(listModel::addElement);
    return listModel;
  }

  public static JEditorPane newHtmlJEditorPane() {
    final JEditorPane descriptionPane = new JEditorPane();
    descriptionPane.setEditable(false);
    descriptionPane.setContentType("text/html");
    return descriptionPane;
  }

  public static void newOpenUrlConfirmationDialog(final UrlConstants url) {
    newOpenUrlConfirmationDialog(url.toString());
  }

  public static void newOpenUrlConfirmationDialog(final String url) {
    final String msg = "Okay to open URL in a web browser?\n" + url;
    SwingComponents.promptUser("Open external URL?", msg, () -> OpenFileUtility.openUrl(url));
  }

  public static void showDialog(final String title, final String message) {
    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, message, title,
        JOptionPane.INFORMATION_MESSAGE));
  }

  /**
   * Builds a new menu item with title and keyboard shortcut key.
   */
  public static JMenu newJMenu(final String menuTitle, final KeyboardCode keyboardCode) {
    final JMenu menu = new JMenu(menuTitle);
    menu.setMnemonic(keyboardCode.getSwingKeyEventCode());
    return menu;
  }

  /**
   * Flag for file selection component, whether to allow selection of files only or folders only.
   */
  public enum FolderSelectionMode {
    FILES, DIRECTORIES
  }

  /**
   * Shows a dialog the user can use to select a folder or file.
   *
   * @param folderSelectionMode Flag controlling whether files or folders are available for selection.
   * @return Empty if the user selects nothing, otherwise the users selection.
   */
  public static Optional<File> showJFileChooser(final FolderSelectionMode folderSelectionMode) {
    final JFileChooser fileChooser = new JFileChooser();
    if (folderSelectionMode == FolderSelectionMode.DIRECTORIES) {
      fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    } else {
      fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    }

    final int result = fileChooser.showOpenDialog(null);
    return (result == JFileChooser.APPROVE_OPTION) ? Optional.of(fileChooser.getSelectedFile()) : Optional.empty();
  }

  /**
   * Displays a file chooser from which the user can select a file to save.
   *
   * <p>
   * The user will be asked to confirm the save if the selected file already exists.
   * </p>
   *
   * @param parent Determines the {@code Frame} in which the dialog is displayed; if {@code null}, or if {@code parent}
   *        has no {@code Frame}, a default {@code Frame} is used.
   * @param fileExtension The extension of the file to save, with or without a leading period; must not be {@code null}.
   *        This extension will be automatically appended to the file name if not present.
   * @param fileExtensionDescription The description of the file extension to be displayed in the file chooser; must not
   *        be {@code null}.
   *
   * @return The file selected by the user or empty if the user aborted the save; never {@code null}.
   */
  public static Optional<File> promptSaveFile(final Component parent, final String fileExtension,
      final String fileExtensionDescription) {
    checkNotNull(fileExtension);
    checkNotNull(fileExtensionDescription);

    final JFileChooser fileChooser = new JFileChooser() {
      private static final long serialVersionUID = -136588718021703367L;

      @Override
      public void approveSelection() {
        final File file = appendExtensionIfAbsent(getSelectedFile(), fileExtension);
        setSelectedFile(file);
        if (file.exists()) {
          final int result = JOptionPane.showConfirmDialog(
              parent,
              String.format("A file named \"%s\" already exists. Do you want to replace it?", file.getName()),
              "Confirm Save",
              JOptionPane.YES_NO_OPTION,
              JOptionPane.WARNING_MESSAGE);
          if (result != JOptionPane.YES_OPTION) {
            return;
          }
        }

        super.approveSelection();
      }
    };

    final String fileExtensionWithoutLeadingPeriod = extensionWithoutLeadingPeriod(fileExtension);
    final FileFilter fileFilter = new FileNameExtensionFilter(
        String.format("%s, *.%s", fileExtensionDescription, fileExtensionWithoutLeadingPeriod),
        fileExtensionWithoutLeadingPeriod);
    fileChooser.setFileFilter(fileFilter);

    final int result = fileChooser.showSaveDialog(parent);
    return (result == JFileChooser.APPROVE_OPTION) ? Optional.of(fileChooser.getSelectedFile()) : Optional.empty();
  }

  @VisibleForTesting
  static File appendExtensionIfAbsent(final File file, final String extension) {
    final String extensionWithLeadingPeriod = extensionWithLeadingPeriod(extension);
    if (file.getName().toLowerCase().endsWith(extensionWithLeadingPeriod.toLowerCase())) {
      return file;
    }

    return new File(file.getParentFile(), file.getName() + extensionWithLeadingPeriod);
  }

  @VisibleForTesting
  static String extensionWithLeadingPeriod(final String extension) {
    return (extension.isEmpty() || extension.startsWith(PERIOD)) ? extension : (PERIOD + extension);
  }

  @VisibleForTesting
  static String extensionWithoutLeadingPeriod(final String extension) {
    return extension.startsWith(PERIOD) ? extension.substring(PERIOD.length()) : extension;
  }

  /**
   * Runs the specified task on a background thread while displaying a progress dialog.
   *
   * @param<T> The type of the task result.
   *
   * @param frame The {@code Frame} from which the progress dialog is displayed or {@code null} to use a shared, hidden
   *        frame as the owner of the progress dialog.
   * @param message The message to display in the progress dialog; must not be {@code null}.
   * @param task The task to be executed; must not be {@code null}.
   *
   * @return A promise that resolves to the result of the task; never {@code null}.
   */
  public static <T> CompletableFuture<T> runWithProgressBar(
      final Frame frame,
      final String message,
      final Callable<T> task) {
    checkNotNull(message);
    checkNotNull(task);

    final CompletableFuture<T> promise = new CompletableFuture<>();
    final SwingWorker<T, ?> worker = new SwingWorker<T, Void>() {
      @Override
      protected T doInBackground() throws Exception {
        return task.call();
      }

      @Override
      protected void done() {
        try {
          promise.complete(get());
        } catch (final ExecutionException e) {
          promise.completeExceptionally(e.getCause());
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
          promise.completeExceptionally(e);
        }
      }
    };
    final ProgressDialog progressDialog = new ProgressDialog(frame, message);
    worker.addPropertyChangeListener(new SwingWorkerCompletionWaiter(progressDialog));
    worker.execute();
    return promise;
  }
}
