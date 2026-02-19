package org.triplea.swing;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.UtilityClass;
import org.triplea.awt.OpenFileUtility;
import org.triplea.swing.jpanel.JPanelBuilder;

/**
 * Wrapper/utility class to give Swing components a nicer API. This class is to help extract pure UI
 * code out of the rest of the code base. This also gives us a cleaner interface between UI and the
 * rest of the code.
 */
@UtilityClass
public final class SwingComponents {
  private static final Collection<String> visiblePrompts = new HashSet<>();

  /**
   * Colors a label text to a highlight color if not valid, otherwise returns the label text to a
   * default color.
   */
  public static void highlightLabelIfNotValid(final boolean valid, final JLabel label) {
    SwingUtilities.invokeLater(
        () -> label.setForeground(valid ? SwingComponents.getDefaultLabelColor() : Color.RED));
  }

  /**
   * Returns the default text color for labels. Note, this is a dynamic value in case the look and
   * feel is changed which could potentially change the foreground color of labels.
   *
   * @throws IllegalStateException Thrown if current thread is not EDT. We need EDT thread to allow
   *     creation of a JLabel where we then check the current foreground color.
   */
  private static Color getDefaultLabelColor() {
    Preconditions.checkState(SwingUtilities.isEventDispatchThread());
    return new JLabel().getForeground();
  }

  public static JTabbedPane newJTabbedPane(final int width, final int height) {
    final JTabbedPane tabbedPane = new JTabbedPane();
    tabbedPane.setPreferredSize(new Dimension(width, height));
    return tabbedPane;
  }

  public static void assignToButtonGroup(final JRadioButton... radioButtons) {
    final ButtonGroup group = new ButtonGroup();
    for (final JRadioButton radioButton : radioButtons) {
      group.add(radioButton);
    }
  }

  public static JScrollPane newJScrollPane(final Component contents) {
    final JScrollPane scroll = new JScrollPane();
    scroll.setViewportView(contents);
    scroll.setBorder(null);
    scroll.getViewport().setBorder(null);
    return scroll;
  }

  /**
   * Displays a dialog that prompts the user for a yes/no response. If the user answers yes, {@code
   * confirmedAction} is run; otherwise no action is performed.
   *
   * @param title The title of the dialog.
   * @param message The message displayed in the dialog; should be phrased as a question.
   * @param confirmedAction The action that is run if the user answers yes.
   */
  public static void promptUser(
      final String title, final String message, final Runnable confirmedAction) {
    if (!SwingUtilities.isEventDispatchThread()) {
      SwingUtilities.invokeLater(() -> promptUser(title, message, confirmedAction));
      return;
    }

    boolean showMessage = false;
    synchronized (visiblePrompts) {
      if (!visiblePrompts.contains(message)) {
        visiblePrompts.add(message);
        showMessage = true;
      }
    }

    if (showMessage) {
      // If message is long, place it in a scroll pane with a max size set.
      // The max size is required to prevent the confirmation window from expanding
      // to fit the full message (which might extend beyond the height of the screen).
      final Component messageComponent =
          message.length() < 1000
              ? new JLabel(message)
              : new JScrollPaneBuilder(new JLabel(message))
                  .preferredSize(600, 600)
                  .maxSize(600, 600)
                  .border(BorderBuilder.EMPTY_BORDER)
                  .build();

      // blocks until the user responds to the modal dialog
      final int response =
          JOptionPane.showConfirmDialog(
              null,
              messageComponent,
              title,
              JOptionPane.YES_NO_OPTION,
              JOptionPane.QUESTION_MESSAGE);

      // dialog is now closed
      visiblePrompts.remove(message);
      if (response == JOptionPane.YES_OPTION) {
        confirmedAction.run();
      }
    }
  }

  public static void newMessageDialog(final Component parent, final String msg) {
    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent, msg));
  }

  /**
   * Executes the specified action when the specified window is in the process of being closed.
   *
   * @param window The window to which the action is attached.
   * @param action The action to execute.
   */
  public static void addWindowClosingListener(final Window window, final Runnable action) {
    checkNotNull(window);
    checkNotNull(action);

    window.addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowClosing(final WindowEvent e) {
            action.run();
          }
        });
  }

  /**
   * Executes the specified action when the specified window has been closed.
   *
   * @param window The window to which the action is attached.
   * @param action The action to execute.
   */
  public static void addWindowClosedListener(final Window window, final Runnable action) {
    checkNotNull(window);
    checkNotNull(action);

    window.addWindowListener(
        new WindowAdapter() {
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

  public static void newOpenUrlConfirmationDialog(
      final Component parentComponent, final String url) {
    final String msg = "Okay to open URL in a web browser?\n" + url;
    SwingComponents.promptUser(
        "Open external URL?", msg, () -> OpenFileUtility.openUrl(parentComponent, url));
  }

  public static void showDialog(final Component parent, final String title, final String message) {
    SwingUtilities.invokeLater(
        () ->
            JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE));
  }

  /**
   * Flag for file selection component, whether to allow selection of files only or folders only.
   */
  public enum FolderSelectionMode {
    FILES,
    DIRECTORIES
  }

  /**
   * Shows a dialog the user can use to select a folder or file.
   *
   * @param folderSelectionMode Flag controlling whether files or folders are available for
   *     selection.
   * @return Empty if the user selects nothing, otherwise the users selection.
   */
  public static Optional<Path> showJFileChooser(final FolderSelectionMode folderSelectionMode) {
    final JFileChooser fileChooser = new JFileChooser();
    if (folderSelectionMode == FolderSelectionMode.DIRECTORIES) {
      fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    } else {
      fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    }

    final int result = fileChooser.showOpenDialog(null);
    return (result == JFileChooser.APPROVE_OPTION)
        ? Optional.of(fileChooser.getSelectedFile().toPath())
        : Optional.empty();
  }

  /**
   * Runs the specified task on a background thread while displaying a progress dialog.
   *
   * @param <T> The type of the task result.
   * @param frame The {@code Frame} from which the progress dialog is displayed or {@code null} to
   *     use a shared, hidden frame as the owner of the progress dialog.
   * @param message The message to display in the progress dialog.
   * @param task The task to be executed.
   * @return A promise that resolves to the result of the task.
   */
  public static <T> CompletableFuture<T> runWithProgressBar(
      final Frame frame, final String message, final Callable<T> task) {
    checkNotNull(message);
    checkNotNull(task);

    final CompletableFuture<T> promise = new CompletableFuture<>();
    final SwingWorker<T, ?> worker =
        new SwingWorker<T, Void>() {
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

  public static JComponent leftBox(final JComponent c) {
    final Box b = new Box(BoxLayout.X_AXIS);
    b.add(c);
    b.add(Box.createHorizontalGlue());
    return b;
  }

  public static void showError(
      final Component parentWindow, final String title, final String message) {
    SwingUtilities.invokeLater(
        () ->
            JOptionPane.showMessageDialog(parentWindow, message, title, JOptionPane.ERROR_MESSAGE));
  }

  /** Displays a pop-up dialog with clickable HTML links. */
  public static void showDialogWithLinks(final DialogWithLinksParams params) {
    final JEditorPane editorPane = new JEditorPaneWithClickableLinks(params.dialogText);

    final JPanel messageToShow = new JPanelBuilder().border(10).add(editorPane).build();

    // parentComponent == null to avoid pop-up from appearing behind other windows
    final Component parentComponent = null;
    SwingUtilities.invokeLater(
        () ->
            JOptionPane.showMessageDialog(
                parentComponent,
                messageToShow,
                params.title,
                Optional.ofNullable(params.dialogType)
                    .orElse(DialogWithLinksTypes.INFO)
                    .optionPaneFlag));
  }

  @Builder
  public static class DialogWithLinksParams {
    @Nonnull private final String title;
    @Nonnull private final String dialogText;
    private DialogWithLinksTypes dialogType;
  }

  @AllArgsConstructor
  public enum DialogWithLinksTypes {
    INFO(JOptionPane.INFORMATION_MESSAGE),
    ERROR(JOptionPane.ERROR_MESSAGE);

    private final int optionPaneFlag;
  }

  /**
   * Checks if the current font can display a given character. Not all fonts can display all unicode
   * characters. EG: not all fonts can display '►'.
   */
  public static boolean canDisplayCharacter(final char character) {
    return new JLabel().getFont().canDisplay(character);
  }

  /** Whether the current look and feel is the platform "native" one. */
  public static boolean isUsingNativeLookAndFeel() {
    return UIManager.getSystemLookAndFeelClassName()
        .equals(UIManager.getLookAndFeel().getClass().getName());
  }

  /**
   * Creates a KeyListener that fires when the escape key is pressed.
   *
   * @param action The action to execute when escape key is pressed.
   */
  public static KeyListener escapeKeyListener(final Runnable action) {
    return new KeyAdapter() {
      @Override
      public void keyPressed(final KeyEvent keyEvent) {
        if (keyEvent.getKeyCode() == KeyEvent.VK_ESCAPE) {
          action.run();
        }
      }
    };
  }

  /** Redraws a component, useful if the contents of the component have been changed. */
  public static void redraw(final Component component) {
    component.revalidate();
    component.repaint();
  }

  /**
   * Creates a 'Help' button that opens a window displaying help text.
   *
   * @param helpWindowTitle The title displayed on the help window
   * @param helpWindowText The (HTML) text displayed in the help window.
   */
  public static JButton helpButton(final String helpWindowTitle, final String helpWindowText) {
    return new JButtonBuilder()
        .title("Help")
        .actionListener(
            () ->
                new JDialogBuilder()
                    .title(helpWindowTitle)
                    .add(
                        new JPanelBuilder()
                            .border(20)
                            .borderLayout()
                            .addCenter(new JLabel("<html>" + helpWindowText))
                            .build())
                    .escapeKeyCloses()
                    .alwaysOnTop()
                    .buildAndShow())
        .build();
  }
}
