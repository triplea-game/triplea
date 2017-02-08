package games.strategy.ui;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.engine.framework.startup.ui.MainFrame;
import games.strategy.net.OpenFileUtility;
import games.strategy.triplea.UrlConstants;

public class SwingComponents {
  private static final String PERIOD = ".";

  public static JTabbedPane newJTabbedPane() {
    return new JTabbedPaneWithFixedWidthTabs();
  }

  public static JPanel newJPanelWithVerticalBoxLayout() {
    return newJPanelWithBoxLayout(BoxLayout.Y_AXIS);
  }

  private static JPanel newJPanelWithBoxLayout(final int layout) {
    final JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, layout));
    return panel;
  }

  public static JPanel newJPanelWithHorizontalBoxLayout() {
    return newJPanelWithBoxLayout(BoxLayout.X_AXIS);
  }

  /**
   * Returns a row that has some padding at the top of it, and bottom.
   */
  public static JPanel createRowWithTopAndBottomPadding(final JPanel contentRow, final int topPadding,
      final int bottomPadding) {
    final JPanel rowContents = new JPanel();
    rowContents.setLayout(new BoxLayout(rowContents, BoxLayout.Y_AXIS));
    rowContents.add(Box.createVerticalStrut(topPadding));
    rowContents.add(contentRow);
    rowContents.add(Box.createVerticalStrut(bottomPadding));
    return rowContents;
  }

  public static ButtonGroup createButtonGroup(final JRadioButton... radioButtons) {
    final ButtonGroup group = new ButtonGroup();
    for (final JRadioButton radioButton : Arrays.asList(radioButtons)) {
      group.add(radioButton);
    }
    return group;
  }

  public static JDialog newJDialog(String title) {
    JDialog dialog = new JDialog(MainFrame.getInstance(), title);
    dialog.setModal(false);
    return dialog;
  }

  public static class ModalJDialog extends JDialog {
    private static final long serialVersionUID = -3953716954531215173L;

    protected ModalJDialog() {
      super((Frame) null, true);
      setLocationByPlatform(true);
    }
  }

  public static void showWindow(final Window window) {
    window.pack();
    window.setLocationByPlatform(true);
    window.setVisible(true);
  }

  public static JPanel newJPanelWithGridLayout(final int rows, final int columns) {
    final JPanel panel = new JPanel();
    panel.setLayout(new GridLayout(rows, columns));
    return panel;
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


  private static final Set<String> visiblePrompts = new HashSet<>();

  /**
   * Creates a JPanel with BorderLayout and adds a west component and an east component
   */
  public static JPanel horizontalJPanel(final Component westComponent, final Component eastComponent) {
    final JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(westComponent, BorderLayout.WEST);
    panel.add(eastComponent, BorderLayout.EAST);
    return panel;
  }

  public static JPanel gridPanel(final int rows, final int cols) {
    final JPanel panel = new JPanel();
    panel.setLayout(new GridLayout(rows, cols));
    return panel;
  }

  public static JButton newJButton(final String title, final String toolTip, final Runnable actionListener) {
    return newJButton(title, toolTip, SwingAction.of(e -> actionListener.run()));
  }

  public static JButton newJButton(final String title, final String toolTip, final ActionListener actionListener) {
    final JButton button = newJButton(title, actionListener);
    button.setToolTipText(toolTip);
    return button;
  }

  public static JButton newJButton(final String title, final ActionListener actionListener) {
    final JButton button = new JButton(title);
    button.addActionListener(actionListener);
    return button;
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

  public static JFrame newJFrameWithCloseAction(final Runnable closeListener) {
    final JFrame frame = new JFrame();
    addWindowCloseListener(frame, closeListener);
    return frame;
  }

  public static void addWindowCloseListener(final Window window, final Runnable closeAction) {
    window.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        closeAction.run();
      }
    });
  }

  public static <T> DefaultListModel<String> newJListModel(final List<T> maps, final Function<T, String> mapper) {
    final List<String> mapList = maps.stream().map(mapper).collect(Collectors.toList());
    final DefaultListModel<String> model = new DefaultListModel<>();
    mapList.forEach(model::addElement);
    return model;
  }

  public static JList<String> newJList(final DefaultListModel<String> listModel) {
    return new JList<>(listModel);
  }

  public static JEditorPane newHtmlJEditorPane() {
    final JEditorPane m_descriptionPane = new JEditorPane();
    m_descriptionPane.setEditable(false);
    m_descriptionPane.setContentType("text/html");
    m_descriptionPane.setBackground(new JLabel().getBackground());
    return m_descriptionPane;
  }

  public static JPanel newBorderedPanel(final int borderWidth) {
    final JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.setBorder(newEmptyBorder(borderWidth));
    return panel;
  }

  public static Border newEmptyBorder(final int borderWidth) {
    return new EmptyBorder(borderWidth, borderWidth, borderWidth, borderWidth);
  }

  public static void newOpenUrlConfirmationDialog(final UrlConstants url) {
    newOpenUrlConfirmationDialog(url.toString());
  }

  public static void newOpenUrlConfirmationDialog(final String url) {
    final String msg = "Okay to open URL in a web browser?\n" + url;
    SwingComponents.promptUser("Open external URL?", msg, () -> OpenFileUtility.openURL(url));
  }

  public static void showDialog(final String title, final String message) {
    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, message, title,
        JOptionPane.INFORMATION_MESSAGE));
  }


  public static JDialog newJDialogModal(final JFrame parent, final String title, final JPanel contents) {
    final JDialog dialog = new JDialog(parent, title, true);
    dialog.getContentPane().add(contents);
    final Action closeAction = SwingAction.of("", e -> dialog.setVisible(false));
    final KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    final String key = "dialog.close";
    dialog.getRootPane().getActionMap().put(key, closeAction);
    dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke, key);
    return dialog;
  }



  public static JMenu newJMenu(final String menuTitle, final KeyboardCode keyboardCode) {
    final JMenu menu = new JMenu(menuTitle);
    menu.setMnemonic(keyboardCode.getSwingKeyEventCode());
    return menu;
  }

  /**
   * Creates a new component that emulates a multiline label.
   *
   * <p>
   * The multiline label will properly wrap text that has embedded newlines ({@code \n}).
   * </p>
   *
   * @param text The text to be displayed; may be {@code null}.
   * @param rows The number of rows; must be greater than or equal to zero.
   * @param cols The number of columns; must be greater than or equal to zero.
   *
   * @return The new multiline label; never {@code null}.
   *
   * @throws IllegalArgumentException If {@code rows} or {@code cols} is negative.
   */
  public static JTextArea newMultilineLabel(final String text, final int rows, final int cols) {
    checkArgument(rows >= 0, "rows must not be negative");
    checkArgument(cols >= 0, "cols must not be negative");

    final JTextArea textArea = new JTextArea(text, rows, cols);
    textArea.setCursor(null);
    textArea.setEditable(false);
    textArea.setFocusable(false);
    textArea.setFont(UIManager.getFont("Label.font"));
    textArea.setLineWrap(true);
    textArea.setOpaque(false);
    textArea.setWrapStyleWord(true);
    return textArea;
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
    return extension.isEmpty() || extension.startsWith(PERIOD) ? extension : PERIOD + extension;
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
          promise.completeExceptionally(e);
          Thread.currentThread().interrupt();
        }
      }
    };
    final ProgressDialog progressDialog = new ProgressDialog(frame, message);
    worker.addPropertyChangeListener(new SwingWorkerCompletionWaiter(progressDialog));
    worker.execute();
    return promise;
  }
}
