package games.strategy.ui;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
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
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import games.strategy.engine.framework.GameRunner;
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
   * Convenience method to create {@code AbstractAction} instances using lambdas.
   * Example usage:
   * <code><pre>
   *   AbstractAction action = SwingComponents.newAbstractionAction(() -> {
   *    if (value) {
   *      doSomething();
   *    }
   *   });
   * </pre></code>
   */
  public static AbstractAction newAbstractAction(final Runnable action) {
    return new AbstractAction() {
      private static final long serialVersionUID = -280371946771796597L;

      @Override
      public void actionPerformed(final ActionEvent e) {
        action.run();
      }
    };
  }



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

  public static void addEscapeKeyListener(
      final RootPaneContainer dialog,
      final Runnable keyDownAction) {
    if(dialog.getRootPane() != null) {
      addKeyListener(dialog.getRootPane(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), keyDownAction);
    }
  }
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

  public static void addKeyListener(
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


  public static JFrame newJFrame(final String title, final JComponent contents) {
    final JFrame frame = new JFrame(title);
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    frame.setIconImage(GameRunner.getGameIcon(frame));
    frame.getContentPane().add(contents, BorderLayout.CENTER);
    frame.pack();

    frame.setLocationRelativeTo(null);
    return frame;
  }

  public static JTabbedPane newJTabbedPane() {
    return newJTabbedPane(900, 600);
  }

  public static JTabbedPane newJTabbedPane(final int width, final int height) {
    final JTabbedPane tabbedPane = new JTabbedPaneWithFixedWidthTabs();
    tabbedPane.setPreferredSize(new Dimension(width, height));
    return tabbedPane;
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


  /**
   * Creates a JPanel with BorderLayout and adds a west component and an east component.
   */
  public static JPanel horizontalJPanel(final Component westComponent, final Component eastComponent) {
    final JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(westComponent, BorderLayout.WEST);
    panel.add(eastComponent, BorderLayout.EAST);
    return panel;
  }

  public static JPanel newJPanelWithGridLayout(final int rows, final int columns) {
    final JPanel panel = new JPanel();
    panel.setLayout(new GridLayout(rows, columns));
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
    SwingComponents.promptUser("Open external URL?", msg, () -> OpenFileUtility.openUrl(url));
  }

  public static void showDialog(final String title, final String message) {
    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, message, title,
        JOptionPane.INFORMATION_MESSAGE));
  }



  public static JMenu newJMenu(final String menuTitle, final KeyboardCode keyboardCode) {
    final JMenu menu = new JMenu(menuTitle);
    menu.setMnemonic(keyboardCode.getSwingKeyEventCode());
    return menu;
  }

  public enum FolderSelectionMode {
    FILES, DIRECTORIES
  }

  public static Optional<File> showJFileChooserForFiles() {
    return showJFileChooser(FolderSelectionMode.FILES);
  }

  public static Optional<File> showJFileChooserForFolders() {
    return showJFileChooser(FolderSelectionMode.DIRECTORIES);
  }

  /**
   * Shows a dialog the user can use to select a folder or file.
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

    if (result == JFileChooser.APPROVE_OPTION) {
      return Optional.of(fileChooser.getSelectedFile());
    } else {
      return Optional.empty();
    }
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

  /**
   * Helper class for gridbag layouts with a fixed number of columns.
   * Example usage:
   * <code><pre>
   * JPanel panelToHaveGridBag = new JPanel();
   * int columnCount = 2;
   * GridBagHelper helper = new GridBagHelper(panelToHaveGridBag, 2);
   * // adding 10 elements would create a 2x5 grid
   * for(int i = 0; i < 10; i ++ ) {
   *   helper.addComponents(childComponent);
   * }
   * </pre></code>
   */
  public static class GridBagHelper {
    private final JComponent parent;
    private final int columns;
    private final GridBagConstraints constraints;

    private int elementCount = 0;

    public GridBagHelper(final JComponent parent, final int columns) {
      this.parent = parent;
      this.parent.setLayout(new GridBagLayout());
      this.columns = columns;
      constraints = new GridBagConstraints();
    }

    /**
     * Adds components to the parent component used when constructing the {@code GridBagHelper}.
     * Components are added as rows in a grid bag layout and wrap to the next column when appropriate.
     * You can call this method multiple times and keep appending components.
     */
    public void addComponents(final JComponent ... children) {
      Preconditions.checkArgument(children.length > 0);
      for (final JComponent child : children) {

        final int x = elementCount % columns;
        final int y = elementCount / columns;

        constraints.gridx = x;
        constraints.gridy = y;

        constraints.ipadx = 3;
        constraints.ipady = 3;

        constraints.anchor = GridBagConstraints.WEST;
        parent.add(child, constraints);
        elementCount++;
      }
    }
  }
}
