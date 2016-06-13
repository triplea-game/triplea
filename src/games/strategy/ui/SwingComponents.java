package games.strategy.ui;

import games.strategy.net.DesktopUtilityBrowserLauncher;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.settings.SettingsWindow;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SwingComponents {

  public static void showJFrame(SettingsWindow settingsWindow) {
    settingsWindow.pack();
    settingsWindow.setLocationByPlatform(true);
//    settingsWindow.setLocation(500, 500);
    settingsWindow.setVisible(true);
  }

  public static JComponent newJTextField(int initialValue, int fieldSize) {
    JTextField textField = new JTextField(String.valueOf(initialValue), fieldSize);
    return textField;
  }

  public enum KeyboardCode {
    D(KeyEvent.VK_D),
    G(KeyEvent.VK_G);


    private final int keyEventCode;

    KeyboardCode(int keyEventCode) {
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
  public static JPanel horizontalJPanel(Component westComponent, Component eastComponent) {
    return horizontalJPanel(westComponent, Optional.empty(), eastComponent);
  }

  public static JPanel horizontalJPanel(Component westComponent, Component centerComponent, Component eastComponent) {
    return horizontalJPanel(westComponent, Optional.of(centerComponent), eastComponent);
  }

  private static JPanel horizontalJPanel(Component westComponent, Optional<Component> centerComponent,
      Component eastComponent) {
    JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(westComponent, BorderLayout.WEST);
    if (centerComponent.isPresent()) {
      panel.add(centerComponent.get(), BorderLayout.CENTER);
    }
    panel.add(eastComponent, BorderLayout.EAST);
    return panel;
  }

  public static JPanel gridPanel(int rows, int cols) {
    JPanel panel = new JPanel();
    panel.setLayout(new GridLayout(rows, cols));
    return panel;
  }

  public static JButton newJButton(String title, String toolTip, Runnable actionListener) {
    return newJButton(title, toolTip, SwingAction.of(e -> actionListener.run()));
  }

  public static JButton newJButton(String title, String toolTip, ActionListener actionListener) {
    JButton button = newJButton(title, actionListener);
    button.setToolTipText(toolTip);
    return button;
  }

  public static JButton newJButton(String title, ActionListener actionListener) {
    JButton button = new JButton(title);
    button.addActionListener(actionListener);
    return button;
  }


  public static JScrollPane newJScrollPane(Component contents) {
    final JScrollPane scroll = new JScrollPane();
    scroll.setViewportView(contents);
    scroll.setBorder(null);
    scroll.getViewport().setBorder(null);
    return scroll;
  }

  public static void promptUser(final String title, final String message, final Runnable confirmedAction) {
    promptUser(title, message, confirmedAction, () -> {
    });
  }

  public static void promptUser(final String title, final String message, final Runnable confirmedAction,
      final Runnable cancelAction) {

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
        int response = JOptionPane.showConfirmDialog(null, message, title,
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        // dialog is now closed
        visiblePrompts.remove(message);
        if (response == JOptionPane.YES_OPTION) {
          confirmedAction.run();
        } else {
          cancelAction.run();
        }
      });
    }

  }

  public static void newMessageDialog(String msg) {
    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, msg));
  }

  public static JFrame newJFrameWithCloseAction(final Runnable closeListener) {
    JFrame frame = new JFrame();
    addWindowCloseListener(frame, closeListener);
    return frame;
  }

  public static void addWindowCloseListener(Window window, Runnable closeAction) {
    window.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        closeAction.run();
      }
    });
  }

  public static <T> DefaultListModel<String> newJListModel(List<T> maps, Function<T, String> mapper) {
    List<String> mapList = maps.stream().map(mapper).collect(Collectors.toList());
    DefaultListModel<String> model = new DefaultListModel<>();
    mapList.forEach(e -> model.addElement(e));
    return model;
  }

  public static <T> JList<String> newJList(DefaultListModel<String> listModel) {
    return new JList<>(listModel);
  }

  public static JEditorPane newHtmlJEditorPane() {
    JEditorPane m_descriptionPane = new JEditorPane();
    m_descriptionPane.setEditable(false);
    m_descriptionPane.setContentType("text/html");
    m_descriptionPane.setBackground(new JLabel().getBackground());
    return m_descriptionPane;
  }

  public static JPanel newBorderedPanel(int borderWidth) {
    JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.setBorder(newEmptyBorder(borderWidth));
    return panel;
  }

  public static Border newEmptyBorder(int borderWidth) {
    int w = borderWidth;
    return new EmptyBorder(w, w, w, w);
  }

  public static void newOpenUrlConfirmationDialog(UrlConstants url) {
    newOpenUrlConfirmationDialog(url.toString());
  }

  public static void newOpenUrlConfirmationDialog(String url) {
    final String msg = "Okay to open URL in a web browser?\n" + url;
    SwingComponents.promptUser("Open external URL?", msg, () -> DesktopUtilityBrowserLauncher.openURL(url));
  }

  public static void showDialog(String message) {
    JOptionPane.showMessageDialog(null,message);
  }


  public static JDialog newJDialogModal(JFrame parent, String title, JPanel contents) {
    final JDialog dialog = new JDialog(parent, title, true);
    dialog.getContentPane().add(contents);
    final Action closeAction = SwingAction.of("", e -> dialog.setVisible(false));
    final KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    final String key = "dialog.close";
    dialog.getRootPane().getActionMap().put(key, closeAction);
    dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke, key);
    return dialog;
  }



  public static JMenu newJMenu(String menuTitle, KeyboardCode keyboardCode) {
    JMenu menu = new JMenu(menuTitle);
    menu.setMnemonic(keyboardCode.getSwingKeyEventCode());
    return menu;
  }
}
