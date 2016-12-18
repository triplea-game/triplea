package games.strategy.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import games.strategy.net.OpenFileUtility;
import games.strategy.triplea.UrlConstants;

public class SwingComponents {

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
    JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE);
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
}
